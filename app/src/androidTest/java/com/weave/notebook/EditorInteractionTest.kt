package com.weave.notebook

import android.graphics.Color
import android.os.SystemClock
import android.view.MotionEvent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.weave.notebook.ink.InkCanvasView
import com.weave.notebook.model.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class EditorInteractionTest {
    private val inst=InstrumentationRegistry.getInstrumentation()
    private fun event(v:InkCanvasView,action:Int,vararg points:Pair<Float,Float>) {
        val properties=Array(points.size) {i->MotionEvent.PointerProperties().apply {id=i;toolType=MotionEvent.TOOL_TYPE_FINGER}}
        val coords=Array(points.size) {i->MotionEvent.PointerCoords().apply {x=points[i].first;y=points[i].second;pressure=1f;size=0.1f}}
        val now=SystemClock.uptimeMillis()
        val e=MotionEvent.obtain(now,now,action,points.size,properties,coords,0,0,1f,1f,0,0,android.view.InputDevice.SOURCE_TOUCHSCREEN,0)
        v.onTouchEvent(e);e.recycle()
    }
    @Test fun twoFingersPanAndPinchWithoutCommittingOrResumingInk() {inst.runOnMainSync {
        val v=InkCanvasView(inst.targetContext);v.layout(0,0,800,1000);v.viewport.setZoom(2f,400f,500f)
        var commits=0;v.onStroke={_,_->commits++}
        event(v,MotionEvent.ACTION_DOWN,300f to 400f)
        event(v,MotionEvent.ACTION_POINTER_DOWN or (1 shl 8),300f to 400f,500f to 400f)
        val before=v.viewport.screenToPage(400f,400f)
        event(v,MotionEvent.ACTION_MOVE,340f to 450f,540f to 450f)
        val moved=v.viewport.pageToScreen(before[0],before[1])
        assertEquals(440f,moved[0],0.5f);assertEquals(450f,moved[1],0.5f)
        event(v,MotionEvent.ACTION_MOVE,290f to 450f,590f to 450f)
        assertEquals(3f,v.viewport.zoom,0.01f)
        val anchored=v.viewport.pageToScreen(before[0],before[1]);assertEquals(440f,anchored[0],0.5f);assertEquals(450f,anchored[1],0.5f)
        event(v,MotionEvent.ACTION_POINTER_UP or (1 shl 8),290f to 450f,590f to 450f)
        event(v,MotionEvent.ACTION_MOVE,320f to 500f);event(v,MotionEvent.ACTION_UP,320f to 500f)
        assertEquals(0,commits)
        event(v,MotionEvent.ACTION_DOWN,400f to 500f);event(v,MotionEvent.ACTION_UP,400f to 500f);assertEquals(1,commits)
    }}
    @Test fun eraserPreviewsBeforeLiftCommitsOnceAndCancelRestores() {inst.runOnMainSync {
        val v=InkCanvasView(inst.targetContext);v.layout(0,0,800,1000)
        val stroke=StrokeRecord(UUID.randomUUID().toString(),v.page.pageId,listOf(StrokeSample(80f,100f,0),StrokeSample(120f,100f,1)),BrushKind.PEN,Color.BLACK,0.45f)
        v.strokes=listOf(stroke);v.brush=BrushKind.ERASER;v.widthMm=4f
        var commits=0;var removed=setOf<String>();v.onErase={commits++;removed=it}
        fun page(action:Int,x:Float,y:Float) {val p=v.viewport.pageToScreen(x,y);event(v,action,p[0] to p[1])}
        page(MotionEvent.ACTION_DOWN,100f,80f);assertTrue(v.erasedPreview.isEmpty())
        page(MotionEvent.ACTION_MOVE,100f,105f)
        assertTrue(stroke.strokeId in v.erasedPreview);assertEquals(0,commits);assertEquals(1,v.strokes.size)
        page(MotionEvent.ACTION_CANCEL,100f,105f);assertTrue(v.erasedPreview.isEmpty());assertEquals(0,commits)
        page(MotionEvent.ACTION_DOWN,100f,80f);page(MotionEvent.ACTION_MOVE,100f,105f)
        page(MotionEvent.ACTION_UP,100f,110f);assertEquals(1,commits);assertEquals(setOf(stroke.strokeId),removed)
        page(MotionEvent.ACTION_DOWN,100f,100f)
        val p=v.viewport.pageToScreen(100f,100f)
        event(v,MotionEvent.ACTION_POINTER_DOWN or (1 shl 8),p[0] to p[1],(p[0]+100f) to p[1])
        assertTrue(v.erasedPreview.isEmpty());event(v,MotionEvent.ACTION_CANCEL,p[0] to p[1]);assertEquals(1,commits)
    }}
}
