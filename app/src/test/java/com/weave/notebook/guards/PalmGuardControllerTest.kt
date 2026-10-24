package com.weave.notebook.guards

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PalmGuardControllerTest {

    @Test
    fun touchInsideBottomGuard_blocksInkEvenIfLaterOutside() {
        val c = PalmGuardController(guard = PalmGuardState(edge = GuardEdge.BOTTOM, sizePx = 100f))
        // Origin inside guard
        assertThat(c.blocksInkOrigin(50f, 950f, 800f, 1000f)).isTrue()
        // Outside guard but would still be blocked if origin was guarded — origin test only
        assertThat(c.blocksInkOrigin(50f, 100f, 800f, 1000f)).isFalse()
    }

    @Test
    fun leftRightHandedness() {
        val c = PalmGuardController()
        c.setHandedness(GuardEdge.LEFT)
        assertThat(c.pointInGuard(10f, 100f, 800f, 1000f)).isTrue()
        c.setHandedness(GuardEdge.RIGHT)
        assertThat(c.pointInGuard(790f, 100f, 800f, 1000f)).isTrue()
    }

    @Test
    fun locked_preventsResizeAndEdgeChange() {
        val c = PalmGuardController(guard = PalmGuardState(sizePx = 120f, locked = true))
        c.resize(50f)
        c.setHandedness(GuardEdge.LEFT)
        assertThat(c.guard.sizePx).isEqualTo(120f)
        assertThat(c.guard.edge).isEqualTo(GuardEdge.BOTTOM)
    }

    @Test
    fun writingWindow_blocksOutsideRegion() {
        val c = PalmGuardController(
            guard = PalmGuardState(enabled = false),
            writingWindow = WritingWindowState(true, 100f, 100f, 400f, 400f)
        )
        assertThat(c.blocksInkOrigin(200f, 200f, 800f, 1000f)).isFalse()
        assertThat(c.blocksInkOrigin(10f, 10f, 800f, 1000f)).isTrue()
    }

    @Test
    fun experimentalSizeFilter_defaultsOff() {
        val c = PalmGuardController()
        assertThat(c.experimentalRejectBySize(0.9f)).isFalse()
        c.experimentalSizeFilter = true
        assertThat(c.experimentalRejectBySize(0.9f)).isTrue()
    }
}
