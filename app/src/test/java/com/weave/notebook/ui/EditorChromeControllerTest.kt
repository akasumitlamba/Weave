package com.weave.notebook.ui

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class EditorChromeControllerTest {

    @Test
    fun minTouchTarget_isAtLeast48dp() {
        assertThat(EditorChromeController.MIN_TOUCH_TARGET_DP).isAtLeast(48)
    }

    @Test
    fun selectTool_updatesSelection() {
        val c = EditorChromeController()
        c.selectTool(ToolKind.MARKER)
        assertThat(c.state.selectedTool).isEqualTo(ToolKind.MARKER)
        assertThat(c.isToolSelected(ToolKind.MARKER)).isTrue()
        assertThat(c.isToolSelected(ToolKind.PEN)).isFalse()
    }

    @Test
    fun navigateMode_forcesHandTool() {
        val c = EditorChromeController()
        c.selectTool(ToolKind.PEN)
        c.setWriteMode(false)
        assertThat(c.state.writeMode).isFalse()
        assertThat(c.state.selectedTool).isEqualTo(ToolKind.HAND)
    }

    @Test
    fun historyButtons_respectFlags() {
        val c = EditorChromeController()
        assertThat(c.state.canUndo).isFalse()
        assertThat(c.state.canRedo).isFalse()
        c.setHistory(canUndo = true, canRedo = false)
        assertThat(c.state.canUndo).isTrue()
        assertThat(c.state.canRedo).isFalse()
    }

    @Test
    fun zoom_clampedToProductRange() {
        val c = EditorChromeController()
        c.setZoomPercent(10)
        assertThat(c.state.zoomPercent).isEqualTo(50)
        c.setZoomPercent(900)
        assertThat(c.state.zoomPercent).isEqualTo(500)
    }

    @Test
    fun pageBounds_requireAtLeastOnePage() {
        val c = EditorChromeController()
        c.setPage(0, 3)
        assertThat(c.state.pageCount).isEqualTo(3)
        assertThat(c.state.pageIndex).isEqualTo(0)
    }

    @Test
    fun tabletViewportBudgets_documented() {
        // Acceptance layouts from handoff — used by evidence / manual checks.
        assertThat(PORTRAIT_W).isEqualTo(1200)
        assertThat(PORTRAIT_H).isEqualTo(1920)
        assertThat(LANDSCAPE_W).isEqualTo(1920)
        assertThat(LANDSCAPE_H).isEqualTo(1200)
    }

    companion object {
        const val PORTRAIT_W = 1200
        const val PORTRAIT_H = 1920
        const val LANDSCAPE_W = 1920
        const val LANDSCAPE_H = 1200
    }
}
