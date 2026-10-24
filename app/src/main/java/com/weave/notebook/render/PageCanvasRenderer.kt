package com.weave.notebook.render

import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import com.weave.notebook.geom.ViewportTransform
import com.weave.notebook.model.BrushKind
import com.weave.notebook.model.PageGeometry
import com.weave.notebook.model.PageSpec
import com.weave.notebook.model.PaperType
import com.weave.notebook.model.StrokeRecord

/**
 * Draws a page template and strokes in page millimeters via the viewport matrix.
 */
class PageCanvasRenderer {

    private val paperPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = 0xFFFFFFFF.toInt()
    }
    private val rulePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 0f // hairline in page space scaled by matrix... use mm width
        color = 0xFFB0B6B8.toInt()
    }
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = 0xFF252A2D.toInt()
        strokeWidth = 0.2f
    }
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val path = Path()

    fun drawPage(
        canvas: Canvas,
        viewport: ViewportTransform,
        page: PageSpec,
        strokes: List<StrokeRecord>
    ) {
        val matrix = viewport.copyPageToScreenMatrix()
        canvas.save()
        canvas.concat(matrix)

        canvas.drawRect(0f, 0f, page.widthMm, page.heightMm, paperPaint)
        if (page.paperType == PaperType.RULED) {
            drawRules(canvas, page)
        }
        canvas.drawRect(0f, 0f, page.widthMm, page.heightMm, borderPaint)

        val highlighters = strokes.filter { it.brush == BrushKind.HIGHLIGHTER }
        val opaque = strokes.filter { it.brush != BrushKind.HIGHLIGHTER && it.brush != BrushKind.ERASER }
        for (s in highlighters) drawStroke(canvas, s)
        for (s in opaque) drawStroke(canvas, s)

        canvas.restore()
    }

    private fun drawRules(canvas: Canvas, page: PageSpec) {
        val spacing = page.ruleSpacingMm
        val margin = PageGeometry.RULE_MARGIN_MM
        rulePaint.strokeWidth = 0.15f
        var y = margin
        while (y < page.heightMm - margin * 0.5f) {
            canvas.drawLine(0f, y, page.widthMm, y, rulePaint)
            y += spacing
        }
    }

    private fun drawStroke(canvas: Canvas, stroke: StrokeRecord) {
        if (stroke.samples.isEmpty()) return
        strokePaint.color = stroke.colorArgb
        strokePaint.strokeWidth = stroke.widthMm
        strokePaint.alpha = (stroke.colorArgb ushr 24) and 0xFF
        path.reset()
        val first = stroke.samples.first()
        path.moveTo(first.xMm, first.yMm)
        if (stroke.samples.size == 1) {
            path.lineTo(first.xMm + 0.01f, first.yMm)
        } else {
            for (i in 1 until stroke.samples.size) {
                val p = stroke.samples[i]
                path.lineTo(p.xMm, p.yMm)
            }
        }
        canvas.drawPath(path, strokePaint)
    }

    /** Identity helper for tests that need a matrix without a view. */
    fun identityMatrix(): Matrix = Matrix()
}
