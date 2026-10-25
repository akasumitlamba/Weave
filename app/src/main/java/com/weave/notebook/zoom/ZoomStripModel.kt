package com.weave.notebook.zoom

import com.weave.notebook.model.PageGeometry

/**
 * Magnified writing strip over a selected page rectangle.
 * Strip and main page share the same stroke model / undo history (IDs only).
 */
data class ZoomStripState(
    val enabled: Boolean = false,
    val magnification: Float = 2.5f,
    /** Source rect in page mm. */
    val leftMm: Float = 15f,
    val topMm: Float = 40f,
    val widthMm: Float = 180f,
    val heightMm: Float = 24f
) {
    val rightMm: Float get() = leftMm + widthMm
    val bottomMm: Float get() = topMm + heightMm
}

class ZoomStripController(
    var state: ZoomStripState = ZoomStripState()
) {
    fun setEnabled(enabled: Boolean) {
        state = state.copy(enabled = enabled)
    }

    fun setMagnification(mag: Float) {
        state = state.copy(magnification = mag.coerceIn(1f, 5f))
    }

    fun moveSource(dxMm: Float, dyMm: Float) {
        val left = (state.leftMm + dxMm).coerceIn(0f, PageGeometry.WIDTH_MM - state.widthMm)
        val top = (state.topMm + dyMm).coerceIn(0f, PageGeometry.HEIGHT_MM - state.heightMm)
        state = state.copy(leftMm = left, topMm = top)
    }

    fun nextLine(ruleSpacingMm: Float = 8f) {
        moveSource(0f, ruleSpacingMm)
    }

    fun mapStripToPage(xInStripMm: Float, yInStripMm: Float): Pair<Float, Float> {
        // Strip local mm (0..width/height of source) maps into source rect.
        val x = state.leftMm + xInStripMm.coerceIn(0f, state.widthMm)
        val y = state.topMm + yInStripMm.coerceIn(0f, state.heightMm)
        return x to y
    }

    fun mapPageToStrip(xMm: Float, yMm: Float): Pair<Float, Float>? {
        if (xMm < state.leftMm || xMm > state.rightMm || yMm < state.topMm || yMm > state.bottomMm) return null
        return (xMm - state.leftMm) to (yMm - state.topMm)
    }
}
