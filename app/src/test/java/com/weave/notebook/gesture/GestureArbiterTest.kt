package com.weave.notebook.gesture

import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test

class GestureArbiterTest {

    private lateinit var arbiter: GestureArbiter

    @Before
    fun setUp() {
        arbiter = GestureArbiter()
    }

    @Test
    fun navigate_neverStartsStroke() {
        arbiter.setMode(GestureArbiter.Mode.NAVIGATE)
        assertThat(arbiter.onDown(t(1, 0f, 0f))).isEqualTo(GestureArbiter.Action.NONE)
        assertThat(arbiter.onMove(listOf(t(1, 1f, 0f)))).isEqualTo(GestureArbiter.Action.PAN)
    }

    @Test
    fun secondTouch_cancelsProvisionalInk() {
        arbiter.setMode(GestureArbiter.Mode.WRITE)
        assertThat(arbiter.onDown(t(1, 0f, 0f))).isEqualTo(GestureArbiter.Action.START_STROKE)
        assertThat(arbiter.onDown(t(2, 10f, 10f))).isEqualTo(GestureArbiter.Action.CANCEL_STROKE)
        assertThat(arbiter.activeStrokePointerId()).isNull()
    }

    @Test
    fun fullLift_resetsSuppressGate() {
        arbiter.setMode(GestureArbiter.Mode.WRITE)
        arbiter.onDown(t(1, 0f, 0f))
        arbiter.onDown(t(2, 10f, 10f))
        arbiter.onUp(1)
        arbiter.onUp(2)
        assertThat(arbiter.onDown(t(3, 0f, 0f))).isEqualTo(GestureArbiter.Action.START_STROKE)
    }

    @Test
    fun pointerReindex_usesIdsNotIndices() {
        arbiter.setMode(GestureArbiter.Mode.WRITE)
        arbiter.onDown(t(5, 0f, 0f))
        assertThat(arbiter.onMove(listOf(t(9, 1f, 1f), t(5, 2f, 2f))))
            .isEqualTo(GestureArbiter.Action.APPEND_STROKE)
        assertThat(arbiter.activeStrokePointerId()).isEqualTo(5)
    }

    @Test
    fun cancel_hardResets() {
        arbiter.setMode(GestureArbiter.Mode.WRITE)
        arbiter.onDown(t(1, 0f, 0f))
        assertThat(arbiter.onCancel()).isEqualTo(GestureArbiter.Action.CANCEL_STROKE)
        assertThat(arbiter.activeStrokePointerId()).isNull()
    }

    private fun t(id: Int, x: Float, y: Float) =
        GestureArbiter.Touch(id, x, y, 0L)
}
