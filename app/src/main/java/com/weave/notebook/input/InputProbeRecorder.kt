package com.weave.notebook.input

import android.view.MotionEvent

/**
 * Captures MotionEvent metadata for passive-stylus investigation.
 * Keeps an in-memory ring buffer only; never writes ink or personal traces to storage.
 */
class InputProbeRecorder(
    private val capacity: Int = 512
) {
    private val samples = ArrayDeque<InputSample>(capacity)
    private var sawFinger = false
    private var sawStylus = false
    private var sawEraser = false
    private var sawMouse = false
    private var minPressure = Float.POSITIVE_INFINITY
    private var maxPressure = Float.NEGATIVE_INFINITY

    fun clear() {
        samples.clear()
        sawFinger = false
        sawStylus = false
        sawEraser = false
        sawMouse = false
        minPressure = Float.POSITIVE_INFINITY
        maxPressure = Float.NEGATIVE_INFINITY
    }

    fun record(event: MotionEvent): List<InputSample> {
        val added = mutableListOf<InputSample>()
        val actionMasked = event.actionMasked
        val actionIndex = event.actionIndex
        val canceled = actionMasked == MotionEvent.ACTION_CANCEL ||
            (event.flags and MotionEvent.FLAG_CANCELED) != 0

        // Historical points for the active pointer (when MOVE).
        if (actionMasked == MotionEvent.ACTION_MOVE) {
            for (h in 0 until event.historySize) {
                for (p in 0 until event.pointerCount) {
                    val sample = sampleFrom(event, p, h, historical = true, canceled = false)
                    add(sample)
                    added += sample
                }
            }
        }

        when (actionMasked) {
            MotionEvent.ACTION_DOWN,
            MotionEvent.ACTION_POINTER_DOWN,
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_POINTER_UP,
            MotionEvent.ACTION_CANCEL -> {
                val sample = sampleFrom(event, actionIndex, historyPos = -1, historical = false, canceled = canceled)
                add(sample)
                added += sample
            }
            MotionEvent.ACTION_MOVE -> {
                for (p in 0 until event.pointerCount) {
                    val sample = sampleFrom(event, p, historyPos = -1, historical = false, canceled = canceled)
                    add(sample)
                    added += sample
                }
            }
        }
        return added
    }

    /** Test / replay entry that does not require a live MotionEvent. */
    fun recordSynthetic(sample: InputSample) {
        add(sample)
    }

    fun snapshot(): List<InputSample> = samples.toList()

    fun buildReport(deviceVerified: Boolean): DeviceInputReport {
        val pressureRange = if (minPressure.isFinite() && maxPressure.isFinite()) {
            minPressure..maxPressure
        } else {
            null
        }
        val identity = when {
            !deviceVerified -> StylusIdentityFinding.UNVERIFIED
            sawStylus && sawFinger -> StylusIdentityFinding.MIXED_OR_UNKNOWN
            sawStylus -> StylusIdentityFinding.STYLUS_DISTINCT
            sawFinger && !sawStylus -> StylusIdentityFinding.FINGER_ONLY
            else -> StylusIdentityFinding.MIXED_OR_UNKNOWN
        }
        val notes = buildString {
            append("Passive stylus may report as TOOL_TYPE_FINGER. ")
            append("Do not treat pressure as genuine without device verification. ")
            if (!deviceVerified) {
                append("Device findings UNVERIFIED — run capture steps on the TCL TAB 10s.")
            }
        }
        return DeviceInputReport(
            stylusIdentity = identity,
            sawFinger = sawFinger,
            sawStylus = sawStylus,
            sawEraser = sawEraser,
            sawMouse = sawMouse,
            pressureRangeObserved = pressureRange,
            sampleCount = samples.size,
            notes = notes
        )
    }

    private fun add(sample: InputSample) {
        while (samples.size >= capacity) {
            samples.removeFirst()
        }
        samples.addLast(sample)
        when (sample.toolType) {
            MotionEvent.TOOL_TYPE_FINGER -> sawFinger = true
            MotionEvent.TOOL_TYPE_STYLUS -> sawStylus = true
            MotionEvent.TOOL_TYPE_ERASER -> sawEraser = true
            MotionEvent.TOOL_TYPE_MOUSE -> sawMouse = true
        }
        if (sample.pressure.isFinite()) {
            minPressure = minOf(minPressure, sample.pressure)
            maxPressure = maxOf(maxPressure, sample.pressure)
        }
    }

    private fun sampleFrom(
        event: MotionEvent,
        pointerIndex: Int,
        historyPos: Int,
        historical: Boolean,
        canceled: Boolean
    ): InputSample {
        val toolType = event.getToolType(pointerIndex)
        val x: Float
        val y: Float
        val size: Float
        val pressure: Float
        val orientation: Float
        val time: Long
        if (historical && historyPos >= 0) {
            x = event.getHistoricalX(pointerIndex, historyPos)
            y = event.getHistoricalY(pointerIndex, historyPos)
            size = event.getHistoricalSize(pointerIndex, historyPos)
            pressure = event.getHistoricalPressure(pointerIndex, historyPos)
            orientation = event.getHistoricalOrientation(pointerIndex, historyPos)
            time = event.getHistoricalEventTime(historyPos)
        } else {
            x = event.getX(pointerIndex)
            y = event.getY(pointerIndex)
            size = event.getSize(pointerIndex)
            pressure = event.getPressure(pointerIndex)
            orientation = event.getOrientation(pointerIndex)
            time = event.eventTime
        }
        return InputSample(
            eventAction = actionLabel(event.actionMasked),
            pointerId = event.getPointerId(pointerIndex),
            pointerIndex = pointerIndex,
            toolType = toolType,
            toolTypeLabel = toolTypeLabel(toolType),
            x = x,
            y = y,
            size = size,
            pressure = pressure,
            orientation = orientation,
            eventTimeMs = time,
            isHistorical = historical,
            canceled = canceled
        )
    }

    companion object {
        fun actionLabel(actionMasked: Int): String = when (actionMasked) {
            MotionEvent.ACTION_DOWN -> "DOWN"
            MotionEvent.ACTION_UP -> "UP"
            MotionEvent.ACTION_MOVE -> "MOVE"
            MotionEvent.ACTION_CANCEL -> "CANCEL"
            MotionEvent.ACTION_POINTER_DOWN -> "POINTER_DOWN"
            MotionEvent.ACTION_POINTER_UP -> "POINTER_UP"
            else -> "OTHER($actionMasked)"
        }

        fun toolTypeLabel(toolType: Int): String = when (toolType) {
            MotionEvent.TOOL_TYPE_FINGER -> "FINGER"
            MotionEvent.TOOL_TYPE_STYLUS -> "STYLUS"
            MotionEvent.TOOL_TYPE_ERASER -> "ERASER"
            MotionEvent.TOOL_TYPE_MOUSE -> "MOUSE"
            MotionEvent.TOOL_TYPE_UNKNOWN -> "UNKNOWN"
            else -> "OTHER($toolType)"
        }
    }
}
