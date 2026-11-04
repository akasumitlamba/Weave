package com.weave.notebook.ink

import android.content.Context
import android.graphics.*
import android.os.SystemClock
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.weave.notebook.geom.ViewportTransform
import com.weave.notebook.guards.*
import com.weave.notebook.model.*
import com.weave.notebook.render.PageCanvasRenderer
import com.weave.notebook.shape.*
import kotlin.math.*
import java.util.UUID

/** Pointer ownership stays in screen space; every ink point is transformed exactly once. */
class InkCanvasView @JvmOverloads constructor(context: Context, attrs: AttributeSet?=null): View(context,attrs) {
    val viewport=ViewportTransform()
    val pipeline=RawInkPipeline()
    val palmGuards=PalmGuardController()
    var page=PageSpec(UUID.randomUUID().toString())
    var strokes: List<StrokeRecord> = emptyList()
    var brush=BrushKind.PEN
    var colorArgb=Color.BLACK
    var widthMm=0.45f
    var inkEnabled=true
    var holdShapes=false
    var onStroke: ((StrokeRecord,StrokeRecord?)->Unit)?=null
    var onErase: ((Set<String>)->Unit)?=null
    var onErasePreview: ((Set<String>)->Unit)?=null
    var erasedPreview:Set<String> = emptySet()
    private var eraseIndex:com.weave.notebook.command.EraserPath.Index?=null
    private var eraseLast:StrokeSample?=null
    private fun eraseAt(points:List<RawInkPipeline.PointerPoint>) {
        val path=buildList {eraseLast?.let {add(it)};points.forEach {add(StrokeSample(it.xMm,it.yMm,it.tMs))}}
        if(path.isEmpty())return
        eraseLast=path.last()
        val hits=eraseIndex?.hitIds(path,widthMm/2,erasedPreview)?:emptySet()
        if(hits!=erasedPreview) {erasedPreview=hits;onErasePreview?.invoke(hits);invalidate()}
    }
    private fun clearErase() {eraseIndex=null;eraseLast=null;erasedPreview=emptySet();onErasePreview?.invoke(emptySet())}
    var onViewport: (()->Unit)?=null
    var onShapeStatus: ((Boolean)->Unit)?=null
    var stripRegion: RectF?=null
    var sourceIndicator: RectF?=null
    private data class Scene(val page:PageSpec,val strokes:List<StrokeRecord>,val base:Picture,val opaque:Picture)
    private var scene:Scene?=null
    private val sceneGeneration=java.util.concurrent.atomic.AtomicInteger()
    private var pendingStrokes:List<StrokeRecord>?=null
    private var pendingPage:PageSpec?=null
    private fun prepareScene() {
        val model=strokes;val spec=page
        if(pendingStrokes===model && pendingPage==spec)return
        pendingStrokes=model;pendingPage=spec
        val generation=sceneGeneration.incrementAndGet()
        // Conservative command-size budget, shared as 16 MiB per viewport.
        if(model.sumOf {it.samples.size.toLong()*128+512}>16L*1024*1024) {scene=null;return}
        renderWorker.execute {
            if(sceneGeneration.get()!=generation)return@execute
            val draw=PageCanvasRenderer();val transform=ViewportTransform().apply {setViewSize(210f,297f)}
            val base=Picture();val bottom=base.beginRecording(210,297)
            draw.drawPage(bottom,transform,spec,model.filter {it.brush==BrushKind.HIGHLIGHTER});base.endRecording()
            val opaque=Picture();val top=opaque.beginRecording(210,297)
            top.clipRect(0f,0f,210f,297f)
            model.forEach {if(it.brush!=BrushKind.HIGHLIGHTER && it.brush!=BrushKind.ERASER)draw.drawStroke(top,it)}
            opaque.endRecording()
            post {
                if(strokes===model && page==spec && sceneGeneration.get()==generation) {
                    scene=Scene(spec,model,base,opaque);invalidate()
                }
            }
        }
    }
    private val renderer=PageCanvasRenderer()
    private val overlay=Paint(Paint.ANTI_ALIAS_FLAG)
    private var owner=-1
    private var suppressed=false
    private val blocked=HashSet<Int>()
    private var panX=0f; private var panY=0f; private var distance=0f
    private var guardDrag=false; private var guardLast=0f
    private var holdX=0f; private var holdY=0f
    private var preview: ShapeCandidate?=null
    private var disarmed=false
    private var eraserX=0f; private var eraserY=0f
    private val recognizer=ShapeRecognizer()
    private val dwell=Runnable {
        if(owner>=0 && !disarmed && pipeline.phase==RawInkPipeline.Phase.DRAWING) {
            preview=recognizer.classify(pipeline.provisionalSamples)
            onShapeStatus?.invoke(preview!=null); invalidate()
        }
    }
    init { isFocusable=true; contentDescription="A4 handwriting page"; palmGuards.guard=palmGuards.guard.copy(enabled=false) }
    override fun onSizeChanged(w:Int,h:Int,oldw:Int,oldh:Int) {
        if(w>0 && h>0) { viewport.setViewSize(w.toFloat(),h.toFloat()); stripRegion?.let { region(it) }; invalidate() }
    }
    fun region(r:RectF) { stripRegion=r; if(width>0 && height>0) viewport.showRegion(r.left,r.top,r.width(),r.height()); invalidate() }
    fun cancelWork() {
        removeCallbacks(dwell); preview=null; onShapeStatus?.invoke(false)
        pipeline.onCancel();clearErase(); owner=-1; suppressed=false; distance=0f; blocked.clear(); guardDrag=false; invalidate()
    }
    private fun point(event:MotionEvent,index:Int,h:Int?=null): RawInkPipeline.PointerPoint {
        val x=if(h==null) event.getX(index) else event.getHistoricalX(index,h)
        val y=if(h==null) event.getY(index) else event.getHistoricalY(index,h)
        val p=viewport.screenToPage(x,y)
        return RawInkPipeline.PointerPoint(event.getPointerId(index),p[0].coerceIn(0f,210f),p[1].coerceIn(0f,297f),
            if(h==null) event.eventTime else event.getHistoricalEventTime(h))
    }
    private fun scheduleHold(p:RawInkPipeline.PointerPoint) {
        if(!holdShapes || disarmed || brush !in listOf(BrushKind.PEN,BrushKind.MARKER)) return
        if(hypot(p.xMm-holdX,p.yMm-holdY)>2f) {
            removeCallbacks(dwell)
            if(preview!=null) { disarmed=true; preview=null; onShapeStatus?.invoke(false) }
            holdX=p.xMm; holdY=p.yMm
            if(!disarmed) postDelayed(dwell,650)
        }
    }
    override fun onTouchEvent(e:MotionEvent):Boolean {
        parent?.requestDisallowInterceptTouchEvent(true)
        when(e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                cancelWork()
                val x=e.x; val y=e.y
                val g=palmGuards.guard
                val boundary=when(g.edge) { GuardEdge.BOTTOM -> height-g.sizePx; GuardEdge.LEFT -> g.sizePx; GuardEdge.RIGHT -> width-g.sizePx }
                val pos=if(g.edge==GuardEdge.BOTTOM) y else x
                if(g.enabled && !g.locked && abs(pos-boundary)<22*resources.displayMetrics.density) {
                    guardDrag=true; guardLast=pos; return true
                }
                panX=x; panY=y
                val p=viewport.screenToPage(x,y)
                if(palmGuards.blocksInkOrigin(x,y,width.toFloat(),height.toFloat())) {
                    blocked.add(e.getPointerId(0)); return true
                }
                if(inkEnabled && p[0] in 0f..210f && p[1] in 0f..297f) {
                    owner=e.getPointerId(0); disarmed=false
                    pipeline.configureStroke(page.pageId,brush,colorArgb,widthMm)
                    val pt=point(e,0); pipeline.onDown(pt); holdX=pt.xMm; holdY=pt.yMm
                    eraserX=x; eraserY=y
                    if(brush==BrushKind.ERASER) {eraseIndex=com.weave.notebook.command.EraserPath.Index(strokes);eraseAt(listOf(pt))}
                    if(holdShapes && brush in listOf(BrushKind.PEN,BrushKind.MARKER)) postDelayed(dwell,650)
                }
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                if(palmGuards.blocksInkOrigin(e.getX(e.actionIndex),e.getY(e.actionIndex),width.toFloat(),height.toFloat())) {
                    blocked.add(e.getPointerId(e.actionIndex)); return true
                }
                if(owner>=0) { pipeline.onCancel();clearErase(); removeCallbacks(dwell); owner=-1; preview=null; onShapeStatus?.invoke(false) }
                val eligible=(0 until e.pointerCount).filter { e.getPointerId(it) !in blocked }
                if(eligible.size==1 && inkEnabled && !suppressed && owner<0) {
                    val i=eligible.first();val pt=point(e,i)
                    owner=pt.pointerId;disarmed=false;pipeline.configureStroke(page.pageId,brush,colorArgb,widthMm);pipeline.onDown(pt)
                    holdX=pt.xMm;holdY=pt.yMm
                    if(brush==BrushKind.ERASER) {eraseIndex=com.weave.notebook.command.EraserPath.Index(strokes);eraseAt(listOf(pt))}
                    if(holdShapes && brush in listOf(BrushKind.PEN,BrushKind.MARKER))postDelayed(dwell,650)
                    invalidate();return true
                }
                suppressed=true
                if(eligible.size>=2) {
                    guardDrag=false
                    val a=eligible[0];val b=eligible[1]
                    distance=hypot(e.getX(a)-e.getX(b),e.getY(a)-e.getY(b))
                    panX=(e.getX(a)+e.getX(b))/2;panY=(e.getY(a)+e.getY(b))/2
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if(guardDrag) {
                    val g=palmGuards.guard; val pos=if(g.edge==GuardEdge.BOTTOM) e.y else e.x
                    palmGuards.resize((pos-guardLast)*if(g.edge==GuardEdge.LEFT) 1 else -1); guardLast=pos; invalidate(); return true
                }
                val eligible=(0 until e.pointerCount).filter { e.getPointerId(it) !in blocked }
                if(eligible.size>=2) {
                    val a=eligible[0]; val b=eligible[1]
                    val next=hypot(e.getX(a)-e.getX(b),e.getY(a)-e.getY(b))
                    val cx=(e.getX(a)+e.getX(b))/2;val cy=(e.getY(a)+e.getY(b))/2
                    if(distance>0 && stripRegion==null) {
                        viewport.setZoom(viewport.zoom*next/distance,panX,panY)
                        viewport.panBy(cx-panX,cy-panY);onViewport?.invoke()
                    }
                    distance=next;panX=cx;panY=cy
                } else if(owner>=0 && !suppressed) {
                    val i=e.findPointerIndex(owner)
                    if(i>=0) {
                        if(palmGuards.writingWindow.enabled && !palmGuards.pointInWindow(e.getX(i),e.getY(i))) {
                            pipeline.onCancel();clearErase();removeCallbacks(dwell);owner=-1;suppressed=true;preview=null;onShapeStatus?.invoke(false);invalidate();return true
                        }
                        val points=ArrayList<RawInkPipeline.PointerPoint>(e.historySize+1)
                        for(h in 0 until e.historySize) points.add(point(e,i,h))
                        val p=point(e,i);points.add(p);pipeline.onMove(points);if(brush==BrushKind.ERASER)eraseAt(points) else scheduleHold(p)
                        eraserX=e.getX(i);eraserY=e.getY(i)
                    }
                }
            }
            MotionEvent.ACTION_UP,MotionEvent.ACTION_POINTER_UP -> {
                val id=e.getPointerId(e.actionIndex)
                if(blocked.remove(id)) {if(e.actionMasked==MotionEvent.ACTION_UP)cancelWork();return true}
                if(e.flags and 0x20 != 0) {cancelWork();return true}
                if(id==owner && !suppressed) {
                    val fitted=preview
                    val raw=pipeline.onUp(point(e,e.actionIndex))
                    if(raw!=null) {
                        if(brush==BrushKind.ERASER) {eraseAt(listOf(point(e,e.actionIndex)));onErase?.invoke(erasedPreview.toSet())}
                        else onStroke?.invoke(raw, fitted?.let { ShapeCommit.replaceWithShape(raw,it).first })
                    }
                }
                blocked.remove(id)
                if(e.actionMasked==MotionEvent.ACTION_UP) cancelWork() else { owner=-1; suppressed=true; distance=0f }
            }
            MotionEvent.ACTION_CANCEL -> cancelWork()
        }
        invalidate();return true
    }
    companion object { private val renderWorker=java.util.concurrent.Executors.newSingleThreadExecutor() }
    override fun onDetachedFromWindow() { cancelWork();sceneGeneration.incrementAndGet();scene=null;pendingStrokes=null;super.onDetachedFromWindow() }
    override fun onDraw(c:Canvas) {
        c.drawColor(com.weave.notebook.ui.NativeUi.SURFACE)
        val live=if(owner>=0 && brush==BrushKind.HIGHLIGHTER) StrokeRecord("live-highlighter",page.pageId,pipeline.provisionalSamples,brush,colorArgb,widthMm,
            when(pipeline.smoother.level) {com.weave.notebook.smooth.SmoothingLevel.OFF->0;com.weave.notebook.smooth.SmoothingLevel.LOW->1;else->2}) else null
        prepareScene()
        val visible=if(erasedPreview.isEmpty())strokes else strokes.filter {it.strokeId !in erasedPreview}
        val cached=scene?.takeIf {erasedPreview.isEmpty() && it.page==page && it.strokes===strokes}
        if(cached!=null) {
            c.save();c.concat(viewport.copyPageToScreenMatrix());c.clipRect(0f,0f,210f,297f)
            c.drawPicture(cached.base);live?.let {renderer.drawStroke(c,it)};c.drawPicture(cached.opaque);c.restore()
        } else renderer.drawPage(c,viewport,page,visible,live)
        if(owner>=0 && brush!=BrushKind.ERASER && brush!=BrushKind.HIGHLIGHTER) {
            val samples=pipeline.liveSamples
            c.save();c.concat(viewport.copyPageToScreenMatrix());c.clipRect(0f,0f,210f,297f)
            val candidate=preview
            if(candidate!=null) {
                val raw=StrokeRecord("preview",page.pageId,samples,brush,colorArgb,widthMm)
                renderer.drawStroke(c,ShapeCommit.replaceWithShape(raw,candidate).first)
            } else if(samples.isNotEmpty()) {
                overlay.color=colorArgb;overlay.style=Paint.Style.STROKE;overlay.strokeWidth=widthMm
                overlay.strokeCap=Paint.Cap.ROUND;overlay.strokeJoin=Paint.Join.ROUND
                val path=PageCanvasRenderer.strokePath(samples,pipeline.smoother.level!=com.weave.notebook.smooth.SmoothingLevel.OFF)
                if(samples.size<2) c.drawCircle(samples[0].xMm,samples[0].yMm,widthMm/2,overlay.apply { style=Paint.Style.FILL })
                else c.drawPath(path,overlay)
            }
            c.restore()
        }
        if(owner>=0 && brush==BrushKind.ERASER) {
            overlay.color=com.weave.notebook.ui.NativeUi.TEAL;overlay.style=Paint.Style.STROKE;overlay.strokeWidth=2f
            c.drawCircle(eraserX,eraserY,widthMm/2/viewport.mmPerPixel(),overlay)
        }
        sourceIndicator?.let { r ->
            val a=viewport.pageToScreen(r.left,r.top);val b=viewport.pageToScreen(r.right,r.bottom)
            overlay.color=com.weave.notebook.ui.NativeUi.TEAL;overlay.style=Paint.Style.STROKE;overlay.strokeWidth=3f
            c.drawRect(a[0],a[1],b[0],b[1],overlay)
        }
        if(palmGuards.guard.enabled) {
            val g=palmGuards.guard;val w=width.toFloat();val h=height.toFloat()
            overlay.style=Paint.Style.FILL;overlay.color=0x4471767A
            val rect=when(g.edge) { GuardEdge.BOTTOM->RectF(0f,h-g.sizePx,w,h);GuardEdge.LEFT->RectF(0f,0f,g.sizePx,h);GuardEdge.RIGHT->RectF(w-g.sizePx,0f,w,h) }
            c.drawRect(rect,overlay)
            overlay.color=0xFF465357.toInt();overlay.textSize=14*resources.displayMetrics.scaledDensity
            c.drawText(if(g.locked) "Palm guard · locked" else "Palm guard · drag edge",rect.left+16,rect.top+30,overlay)
        }
        val win=palmGuards.writingWindow
        if(win.enabled) { overlay.style=Paint.Style.STROKE;overlay.color=com.weave.notebook.ui.NativeUi.TEAL;overlay.strokeWidth=3f;c.drawRect(win.left,win.top,win.right,win.bottom,overlay) }
    }
}
