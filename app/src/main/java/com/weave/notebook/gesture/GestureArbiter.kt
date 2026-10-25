package com.weave.notebook.gesture

import kotlin.math.hypot

/**
 * Arbitrates Write vs Navigate gestures.
 * Second contact cancels provisional ink before pinch; navigation never creates ink.
 */
class GestureArbiter(
    private val pinchSlopMm: Float = 1.5f
) {
    enum class Mode { WRITE, NAVIGATE }
    enum class Action {
        NONE,
        START_STROKE,
        APPEND_STROKE,
        END_STROKE,
        CANCEL_STROKE,
        PAN,
        PINCH,
        SUPPRESS_INK
    }

    data class Touch(
        val pointerId: Int,
        val xMm: Float,
        val yMm: Float,
        val tMs: Long
    )

    var mode: Mode = Mode.WRITE
        private set

    private var strokeOwnerId: Int? = null
    private var contacts = linkedMapOf<Int, Touch>()
    private var awaitingFullLiftAfterMulti = false
    private var pinchActive = false
    private var pinchStartDistance = 0f
    private var lastPanX = 0f
    private var lastPanY = 0f

    fun setMode(mode: Mode) {
        if (this.mode != mode) {
            this.mode = mode
            hardReset()
        }
    }

    fun hardReset() {
        strokeOwnerId = null
        contacts.clear()
        awaitingFullLiftAfterMulti = false
        pinchActive = false
        pinchStartDistance = 0f
    }

    fun onDown(touch: Touch): Action {
        contacts[touch.pointerId] = touch
        if (awaitingFullLiftAfterMulti) {
            return Action.SUPPRESS_INK
        }
        return when (mode) {
            Mode.NAVIGATE -> {
                if (contacts.size == 1) {
                    lastPanX = touch.xMm
                    lastPanY = touch.yMm
                    Action.NONE
                } else {
                    beginPinchTracking()
                    Action.NONE
                }
            }
            Mode.WRITE -> {
                when (contacts.size) {
                    1 -> {
                        strokeOwnerId = touch.pointerId
                        Action.START_STROKE
                    }
                    else -> {
                        // Second contact: cancel provisional ink before pinch.
                        strokeOwnerId = null
                        awaitingFullLiftAfterMulti = true
                        beginPinchTracking()
                        Action.CANCEL_STROKE
                    }
                }
            }
        }
    }

    fun onMove(touches: List<Touch>): Action {
        for (t in touches) {
            if (contacts.containsKey(t.pointerId)) contacts[t.pointerId] = t
        }
        if (awaitingFullLiftAfterMulti && mode == Mode.WRITE) {
            return if (contacts.size >= 2 && pinchMoved()) Action.PINCH else Action.SUPPRESS_INK
        }
        return when (mode) {
            Mode.NAVIGATE -> navigateMove()
            Mode.WRITE -> {
                val owner = strokeOwnerId
                if (owner != null && contacts.size == 1) Action.APPEND_STROKE
                else if (contacts.size >= 2) {
                    strokeOwnerId = null
                    awaitingFullLiftAfterMulti = true
                    beginPinchTracking()
                    Action.CANCEL_STROKE
                } else Action.NONE
            }
        }
    }

    fun onUp(pointerId: Int): Action {
        contacts.remove(pointerId)
        val action = when {
            mode == Mode.WRITE && pointerId == strokeOwnerId && !awaitingFullLiftAfterMulti -> {
                strokeOwnerId = null
                Action.END_STROKE
            }
            contacts.isEmpty() && awaitingFullLiftAfterMulti -> {
                awaitingFullLiftAfterMulti = false
                pinchActive = false
                Action.SUPPRESS_INK
            }
            else -> Action.NONE
        }
        if (contacts.isEmpty()) {
            strokeOwnerId = null
            pinchActive = false
        }
        return action
    }

    fun onCancel(): Action {
        hardReset()
        return Action.CANCEL_STROKE
    }

    fun activeStrokePointerId(): Int? = strokeOwnerId

    fun panDeltaMm(): Pair<Float, Float> {
        if (contacts.size != 1) return 0f to 0f
        val t = contacts.values.first()
        val dx = t.xMm - lastPanX
        val dy = t.yMm - lastPanY
        lastPanX = t.xMm
        lastPanY = t.yMm
        return dx to dy
    }

    fun pinchScaleFactor(): Float {
        if (contacts.size < 2 || pinchStartDistance <= 0f) return 1f
        val dist = currentPinchDistance()
        return (dist / pinchStartDistance).coerceIn(0.5f, 2f)
    }

    private fun navigateMove(): Action {
        return when {
            contacts.size >= 2 -> {
                if (!pinchActive) beginPinchTracking()
                if (pinchMoved()) Action.PINCH else Action.NONE
            }
            contacts.size == 1 -> Action.PAN
            else -> Action.NONE
        }
    }

    private fun beginPinchTracking() {
        pinchActive = true
        pinchStartDistance = currentPinchDistance()
    }

    private fun currentPinchDistance(): Float {
        if (contacts.size < 2) return 0f
        val pts = contacts.values.toList()
        return hypot(pts[0].xMm - pts[1].xMm, pts[0].yMm - pts[1].yMm)
    }

    private fun pinchMoved(): Boolean {
        if (pinchStartDistance <= 0f) return false
        return kotlin.math.abs(currentPinchDistance() - pinchStartDistance) >= pinchSlopMm
    }
}
