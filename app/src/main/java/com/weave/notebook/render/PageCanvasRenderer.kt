package com.weave.notebook.render

import android.graphics.*
import com.weave.notebook.geom.ViewportTransform
import com.weave.notebook.model.*
import com.weave.notebook.smooth.*

/** Same clipped vector scene for the editor, thumbnails, zoom strip and PDF. */
class PageCanvasRenderer {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeCap=Paint.Cap.ROUND; strokeJoin=Paint.Join.ROUND }
    private data class Cached(val stroke: StrokeRecord, val path: Path, val bytes: Int)
    private val paths = LinkedHashMap<String,Cached>(32,0.75f,true)
    private var bytes = 0
    private fun geometry(s: StrokeRecord): Path {
        paths[s.strokeId]?.let { if(it.stroke === s) return it.path }
        val level = when(s.smoothingVersion) { 0 -> SmoothingLevel.OFF; 2 -> SmoothingLevel.MEDIUM; else -> SmoothingLevel.LOW }
        val smoother = StrokeSmoother(level)
        val samples = if(s.fittedShape != null) s.samples else s.samples.map { smoother.filterLive(it) }
        val path = strokePath(samples, s.fittedShape == null && level != SmoothingLevel.OFF)
        paint.style=Paint.Style.STROKE; paint.strokeWidth=s.widthMm
        val outline = Path()
        paint.getFillPath(path,outline)
        if(samples.size == 1 || samples.all { it.xMm==samples[0].xMm && it.yMm==samples[0].yMm }) {
            outline.addCircle(samples.first().xMm,samples.first().yMm,s.widthMm/2,Path.Direction.CW)
        }
        val cost = samples.size * 80 + 256
        paths.remove(s.strokeId)?.let { bytes-=it.bytes }
        if(cost <= 32*1024*1024) { paths[s.strokeId]=Cached(s,outline,cost); bytes+=cost }
        val iterator=paths.entries.iterator()
        while(bytes>32*1024*1024 && iterator.hasNext()) { bytes-=iterator.next().value.bytes; iterator.remove() }
        return outline
    }
    fun drawPage(canvas: Canvas, viewport: ViewportTransform, page: PageSpec, strokes: List<StrokeRecord>, liveHighlighter: StrokeRecord? = null, minimumRulePx:Float=0f) {
        canvas.save(); canvas.concat(viewport.copyPageToScreenMatrix())
        canvas.clipRect(0f,0f,page.widthMm,page.heightMm)
        paint.style=Paint.Style.FILL; paint.color=Color.WHITE
        canvas.drawRect(0f,0f,page.widthMm,page.heightMm,paint)
        if(page.paperType==PaperType.RULED) {
            paint.style=Paint.Style.STROKE; paint.strokeWidth=maxOf(0.13f,minimumRulePx/(viewport.pageToScreen(1f,0f)[0]-viewport.pageToScreen(0f,0f)[0]).coerceAtLeast(0.001f)); paint.color=0xFFD7E1E6.toInt()
            var y=15f
            while(y<page.heightMm-7.5f) { canvas.drawLine(0f,y,page.widthMm,y,paint); y+=page.ruleSpacingMm }
            paint.color=0xFFEBC6C6.toInt(); canvas.drawLine(15f,0f,15f,page.heightMm,paint)
        }
        strokes.forEach { if(it.brush==BrushKind.HIGHLIGHTER) drawStroke(canvas,it) }
        liveHighlighter?.let {drawStroke(canvas,it)}
        strokes.forEach { if(it.brush!=BrushKind.HIGHLIGHTER && it.brush!=BrushKind.ERASER) drawStroke(canvas,it) }
        canvas.restore()
    }
    fun drawStroke(canvas: Canvas, stroke: StrokeRecord) {
        if(stroke.samples.isEmpty()) return
        val path=geometry(stroke)
        paint.style=Paint.Style.FILL; paint.color=stroke.colorArgb
        canvas.drawPath(path,paint)
    }
    fun identityMatrix(): Matrix = Matrix()
    companion object {
        fun strokePath(samples: List<StrokeSample>, smooth: Boolean): Path {
            val p=Path(); if(samples.isEmpty()) return p
            p.moveTo(samples[0].xMm,samples[0].yMm)
            if(smooth && samples.size>2) {
                for(i in 1 until samples.size) {
                    val a=samples[i-1]; val b=samples[i]
                    p.quadTo(a.xMm,a.yMm,(a.xMm+b.xMm)/2,(a.yMm+b.yMm)/2)
                }
                p.lineTo(samples.last().xMm,samples.last().yMm)
            } else samples.drop(1).forEach { p.lineTo(it.xMm,it.yMm) }
            return p
        }
    }
}
