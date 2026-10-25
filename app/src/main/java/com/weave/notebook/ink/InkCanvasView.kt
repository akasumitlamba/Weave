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
import com.weave.notebook.gesture.GestureArbiter
import com.weave.notebook.model.BrushKind
import com.weave.notebook.model.PageSpec
import com.weave.notebook.model.PaperType
import com.weave.notebook.model.StrokeRecord
import com.weave.notebook.render.PageCanvasRenderer
import java.util.UUID

/**
 * Custom Canvas drawing View with Write/Navigate gesture arbitration.
 */
class InkCanvasView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val renderer = PageCanvasRenderer()
    val viewport = ViewportTransform()
    val pipeline = RawInkPipeline()
    val arbiter = GestureArbiter()

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

    var inkEnabled: Boolean = true
        set(value) {
            field = value
            arbiter.setMode(
                if (value) GestureArbiter.Mode.WRITE else GestureArbiter.Mode.NAVIGATE
            )
        }

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
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val touch = touchFrom(event, event.actionIndex)
                when (arbiter.onDown(touch)) {
                    GestureArbiter.Action.START_STROKE -> {
                        pipeline.onDown(
                            RawInkPipeline.PointerPoint(touch.pointerId, touch.xMm, touch.yMm, touch.tMs)
                        )
                    }
                    GestureArbiter.Action.CANCEL_STROKE -> pipeline.onCancel()
                    else -> Unit
                }
                invalidate()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val touches = buildList {
                    for (h in 0 until event.historySize) {
                        for (i in 0 until event.pointerCount) {
                            add(historicalTouch(event, i, h))
                        }
                    }
                    for (i in 0 until event.pointerCount) add(touchFrom(event, i))
                }
                when (arbiter.onMove(touches)) {
                    GestureArbiter.Action.APPEND_STROKE -> {
                        val owner = arbiter.activeStrokePointerId()
                        pipeline.onMove(
                            touches.filter { it.pointerId == owner }.map {
                                RawInkPipeline.PointerPoint(it.pointerId, it.xMm, it.yMm, it.tMs)
                            }
                        )
                    }
                    GestureArbiter.Action.CANCEL_STROKE -> pipeline.onCancel()
                    GestureArbiter.Action.PAN -> {
                        val (dxMm, dyMm) = arbiter.panDeltaMm()
                        val dxPx = dxMm / viewport.mmPerPixel()
                        val dyPx = dyMm / viewport.mmPerPixel()
                        viewport.panBy(dxPx, dyPx)
                    }
                    GestureArbiter.Action.PINCH -> {
                        val factor = arbiter.pinchScaleFactor()
                        val newZoom = (viewport.zoom * factor).coerceIn(
                            ViewportTransform.MIN_ZOOM,
                            ViewportTransform.MAX_ZOOM
                        )
                        viewport.setZoom(newZoom, width / 2f, height / 2f)
                    }
                    else -> Unit
                }
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                val id = event.getPointerId(event.actionIndex)
                val touch = touchFrom(event, event.actionIndex)
                when (arbiter.onUp(id)) {
                    GestureArbiter.Action.END_STROKE -> {
                        val committed = pipeline.onUp(
                            RawInkPipeline.PointerPoint(touch.pointerId, touch.xMm, touch.yMm, touch.tMs)
                        )
                        if (committed != null) {
                            strokes.add(committed)
                            onStrokeCommitted?.invoke(committed)
                        }
                    }
                    GestureArbiter.Action.CANCEL_STROKE -> pipeline.onCancel()
                    else -> Unit
                }
                invalidate()
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                arbiter.onCancel()
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

    private fun touchFrom(event: MotionEvent, index: Int): GestureArbiter.Touch {
        viewport.screenToPage(event.getX(index), event.getY(index), pagePoint)
        return GestureArbiter.Touch(
            pointerId = event.getPointerId(index),
            xMm = pagePoint[0],
            yMm = pagePoint[1],
            tMs = event.eventTime
        )
    }

    private fun historicalTouch(event: MotionEvent, index: Int, historyPos: Int): GestureArbiter.Touch {
        viewport.screenToPage(
            event.getHistoricalX(index, historyPos),
            event.getHistoricalY(index, historyPos),
            pagePoint
        )
        return GestureArbiter.Touch(
            pointerId = event.getPointerId(index),
            xMm = pagePoint[0],
            yMm = pagePoint[1],
            tMs = event.getHistoricalEventTime(historyPos)
        )
    }
}
