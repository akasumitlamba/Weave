package com.weave.notebook.input

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.weave.notebook.R

/**
 * Debug-only capture surface. Draws transient contact markers; does not retain writing.
 */
class InputProbeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    var recorder: InputProbeRecorder = InputProbeRecorder()
    var onSamplesChanged: ((List<InputSample>) -> Unit)? = null

    private val markerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        color = ContextCompat.getColor(context, R.color.teal_selection)
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.charcoal)
        textSize = 28f
    }

    private var lastMarkers: List<Pair<Float, Float>> = emptyList()

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val added = recorder.record(event)
        lastMarkers = added
            .filter { !it.isHistorical }
            .map { it.x to it.y }
        onSamplesChanged?.invoke(recorder.snapshot())
        invalidate()
        return true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(ContextCompat.getColor(context, R.color.paper_white))
        for ((x, y) in lastMarkers) {
            canvas.drawCircle(x, y, 24f, markerPaint)
        }
        canvas.drawText("Input probe — samples not saved", 24f, 48f, textPaint)
    }
}
