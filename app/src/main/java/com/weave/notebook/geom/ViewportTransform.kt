package com.weave.notebook.geom

import android.graphics.Matrix
import com.weave.notebook.model.PageGeometry
import kotlin.math.max
import kotlin.math.min

/**
 * One invertible page(mm) ↔ screen(px) transform.
 * Rotation changes the viewport mapping, never stored A4 coordinates.
 *
 * Core math is pure floats so JVM unit tests do not require Robolectric.
 * [copyPageToScreenMatrix] is an Android bridge for Canvas rendering.
 */
class ViewportTransform(
    pageWidthMm: Float = PageGeometry.WIDTH_MM,
    pageHeightMm: Float = PageGeometry.HEIGHT_MM
) {
    var pageWidthMm: Float = pageWidthMm
        private set
    var pageHeightMm: Float = pageHeightMm
        private set

    var zoom: Float = 1f
        private set

    var panXPx: Float = 0f
        private set
    var panYPx: Float = 0f
        private set

    var viewWidthPx: Float = 1f
        private set
    var viewHeightPx: Float = 1f
        private set

    /** Scale: px per mm. */
    private var scale: Float = 1f
    private var originX: Float = 0f
    private var originY: Float = 0f

    fun setViewSize(widthPx: Float, heightPx: Float) {
        require(widthPx > 0f && heightPx > 0f)
        viewWidthPx = widthPx
        viewHeightPx = heightPx
        rebuild()
    }

    fun setZoom(zoomFactor: Float, anchorScreenX: Float, anchorScreenY: Float) {
        val before = FloatArray(2)
        screenToPage(anchorScreenX, anchorScreenY, before)
        zoom = zoomFactor.coerceIn(MIN_ZOOM, MAX_ZOOM)
        rebuild()
        val after = FloatArray(2)
        pageToScreen(before[0], before[1], after)
        panXPx += anchorScreenX - after[0]
        panYPx += anchorScreenY - after[1]
        clampPan()
        rebuild()
    }

    fun panBy(dxPx: Float, dyPx: Float) {
        panXPx += dxPx
        panYPx += dyPx
        clampPan()
        rebuild()
    }

    fun restore(zoom: Float, panX: Float, panY: Float) {
        this.zoom = zoom.coerceIn(MIN_ZOOM, MAX_ZOOM)
        panXPx = panX; panYPx = panY
        clampPan(); rebuild()
    }

    fun showRegion(leftMm: Float, topMm: Float, widthMm: Float, heightMm: Float) {
        val desired = min(viewWidthPx / widthMm, viewHeightPx / heightMm)
        zoom = desired / fitScale()
        panXPx = (pageWidthMm/2 - leftMm - widthMm/2) * desired
        panYPx = (pageHeightMm/2 - topMm - heightMm/2) * desired
        rebuild()
    }

    fun fitPage() {
        zoom = 1f
        panXPx = 0f
        panYPx = 0f
        rebuild()
    }

    fun fitWidth() {
        val fit = fitScale()
        val widthScale = viewWidthPx / pageWidthMm
        zoom = (widthScale / fit).coerceIn(MIN_ZOOM, MAX_ZOOM)
        panXPx = 0f
        panYPx = 0f
        rebuild()
    }

    fun pageToScreen(xMm: Float, yMm: Float, out: FloatArray = FloatArray(2)): FloatArray {
        out[0] = xMm * scale + originX
        out[1] = yMm * scale + originY
        return out
    }

    fun screenToPage(xPx: Float, yPx: Float, out: FloatArray = FloatArray(2)): FloatArray {
        out[0] = (xPx - originX) / scale
        out[1] = (yPx - originY) / scale
        return out
    }

    fun copyPageToScreenMatrix(): Matrix {
        val m = Matrix()
        m.setScale(scale, scale)
        m.postTranslate(originX, originY)
        return m
    }

    fun mmPerPixel(): Float = 1f / scale

    private fun fitScale(): Float {
        val sx = viewWidthPx / pageWidthMm
        val sy = viewHeightPx / pageHeightMm
        return min(sx, sy)
    }

    private fun rebuild() {
        scale = fitScale() * zoom
        val contentW = pageWidthMm * scale
        val contentH = pageHeightMm * scale
        originX = (viewWidthPx - contentW) * 0.5f + panXPx
        originY = (viewHeightPx - contentH) * 0.5f + panYPx
    }

    private fun clampPan() {
        val s = fitScale() * zoom
        val contentW = pageWidthMm * s
        val contentH = pageHeightMm * s
        val over = OVERSCROLL_PX
        val maxPanX = max(0f, (contentW - viewWidthPx) * 0.5f) + over
        val maxPanY = max(0f, (contentH - viewHeightPx) * 0.5f) + over
        panXPx = panXPx.coerceIn(-maxPanX, maxPanX)
        panYPx = panYPx.coerceIn(-maxPanY, maxPanY)
    }

    companion object {
        const val MIN_ZOOM = 0.5f
        const val MAX_ZOOM = 5.0f
        const val OVERSCROLL_PX = 48f
    }
}
