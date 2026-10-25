package com.weave.notebook.lifecycle

import com.google.common.truth.Truth.assertThat
import com.weave.notebook.ui.ToolKind
import com.weave.notebook.zoom.ZoomStripState
import org.junit.Test

class InterruptedWorkGuardTest {
    @Test
    fun lifecycleInterrupt_clearsStrokeAndHold() {
        val g = InterruptedWorkGuard()
        g.onStrokeBegin()
        assertThat(g.onLifecycleInterrupt()).isTrue()
        assertThat(g.strokeActive).isFalse()
        assertThat(g.holdArmed).isFalse()
    }

    @Test
    fun sessionState_roundTripWithoutAndroidBundle() {
        // Pure data equality for fields used across recreation.
        val s = EditorSessionState(
            noteId = "n1",
            noteTitle = "Physics",
            tool = ToolKind.MARKER,
            writeMode = false,
            zoomPercent = 250,
            pageIndex = 1,
            pageCount = 3,
            panXPx = 12f,
            panYPx = -4f,
            zoomStrip = ZoomStripState(enabled = true, magnification = 2f, leftMm = 10f, topMm = 20f)
        )
        assertThat(s.copy().noteTitle).isEqualTo("Physics")
        assertThat(s.zoomStrip.enabled).isTrue()
        assertThat(s.tool).isEqualTo(ToolKind.MARKER)
    }
}
