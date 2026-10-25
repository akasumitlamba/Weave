package com.weave.notebook.ink

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.weave.notebook.R
import com.weave.notebook.geom.ViewportTransform
import com.weave.notebook.model.BrushKind
import com.weave.notebook.model.PageSpec
import com.weave.notebook.model.PaperType
import com.weave.notebook.model.StrokeRecord
import com.weave.notebook.render.PageCanvasRenderer
import java.util.UUID

/**
 * Custom Canvas drawing View: cached page + current stroke overlay.
 * Historical MotionEvent samples are consumed in order; CANCEL clears provisional ink.
 */
class InkCanvasView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val renderer = PageCanvasRenderer()
    val viewport = ViewportTransform()
    val pipeline = RawInkPipeline()

    var page: PageSpec = PageSpec(pageId = UUID.randomUUID().toString(), paperType = PaperType.RULED)
        set(value) {
            field = value
            pipeline.configureStroke(value.pageId, brush, colorArgb, widthMm)
            invalidate()
        }

    var strokes: MutableList<StrokeRecord> = mutableListOf()
        set(value) {
            field = value
            invalidate()
        }

    var brush: BrushKind = BrushKind.PEN
        set(value) {
            field = value
            pipeline.configureStroke(page.pageId, value, colorArgb, widthMm)
        }

    var colorArgb: Int = 0xFF000000.toInt()
        set(value) {
            field = value
            pipeline.configureStroke(page.pageId, brush, value, widthMm)
        }

    var widthMm: Float = 0.45f
        set(value) {
            field = value
            pipeline.configureStroke(page.pageId, brush, colorArgb, value)
        }

    /** When false, touches are ignored for ink (Navigate mode / Hand tool). */
    var inkEnabled: Boolean = true

    var onStrokeCommitted: ((StrokeRecord) -> Unit)? = null

    private val livePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val livePath = Path()
    private val pagePoint = FloatArray(2)

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 0 && h > 0) {
            viewport.setViewSize(w.toFloat(), h.toFloat())
            viewport.fitPage()
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!inkEnabled) return false
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val p = pointFrom(event, event.actionIndex, historical = false)
                pipeline.onDown(p)
                invalidate()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val batch = ArrayList<RawInkPipeline.PointerPoint>()
                for (h in 0 until event.historySize) {
                    for (i in 0 until event.pointerCount) {
                        batch += pointFromHistorical(event, i, h)
                    }
                }
                for (i in 0 until event.pointerCount) {
                    batch += pointFrom(event, i, historical = false)
                }
                pipeline.onMove(batch)
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                if (event.actionMasked == MotionEvent.ACTION_POINTER_UP) {
                    // Non-owner lift ignored for now; PR 06 handles multi-touch cancel.
                    val id = event.getPointerId(event.actionIndex)
                    if (id != pipeline.activePointerId) return true
                }
                val p = pointFrom(event, event.actionIndex, historical = false)
                val committed = pipeline.onUp(p)
                if (committed != null) {
                    strokes.add(committed)
                    onStrokeCommitted?.invoke(committed)
                }
                invalidate()
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                pipeline.onCancel()
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(ContextCompat.getColor(context, R.color.workspace_bg))
        renderer.drawPage(canvas, viewport, page, strokes)
        drawProvisional(canvas)
    }

    private fun drawProvisional(canvas: Canvas) {
        val samples = pipeline.provisionalSamples
        if (samples.isEmpty()) return
        livePaint.color = colorArgb
        livePaint.strokeWidth = widthMm * (1f / viewport.mmPerPixel())
        livePath.reset()
        viewport.pageToScreen(samples[0].xMm, samples[0].yMm, pagePoint)
        livePath.moveTo(pagePoint[0], pagePoint[1])
        if (samples.size == 1) {
            livePath.lineTo(pagePoint[0] + 0.5f, pagePoint[1])
        } else {
            for (i in 1 until samples.size) {
                viewport.pageToScreen(samples[i].xMm, samples[i].yMm, pagePoint)
                livePath.lineTo(pagePoint[0], pagePoint[1])
            }
        }
        canvas.drawPath(livePath, livePaint)
    }

    private fun pointFrom(event: MotionEvent, index: Int, historical: Boolean): RawInkPipeline.PointerPoint {
        viewport.screenToPage(event.getX(index), event.getY(index), pagePoint)
        return RawInkPipeline.PointerPoint(
            pointerId = event.getPointerId(index),
            xMm = pagePoint[0],
            yMm = pagePoint[1],
            tMs = event.eventTime,
            historical = historical
        )
    }

    private fun pointFromHistorical(event: MotionEvent, index: Int, historyPos: Int): RawInkPipeline.PointerPoint {
        viewport.screenToPage(
            event.getHistoricalX(index, historyPos),
            event.getHistoricalY(index, historyPos),
            pagePoint
        )
        return RawInkPipeline.PointerPoint(
            pointerId = event.getPointerId(index),
            xMm = pagePoint[0],
            yMm = pagePoint[1],
            tMs = event.getHistoricalEventTime(historyPos),
            historical = true
        )
    }
}
