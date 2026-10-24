package com.weave.notebook.guards

/**
 * Explicit touch-blocking palm guard and optional writing window.
 * Not automatic palm rejection — contacts originating inside a guard never become ink.
 */
enum class GuardEdge { BOTTOM, LEFT, RIGHT }

data class PalmGuardState(
    val enabled: Boolean = true,
    val edge: GuardEdge = GuardEdge.BOTTOM,
    val sizePx: Float = 160f,
    val locked: Boolean = false,
    val minSizePx: Float = 64f,
    val maxSizePx: Float = 480f
)

data class WritingWindowState(
    val enabled: Boolean = false,
    /** Axis-aligned rect in view pixels. */
    val left: Float = 0f,
    val top: Float = 0f,
    val right: Float = 0f,
    val bottom: Float = 0f
)

class PalmGuardController(
    var guard: PalmGuardState = PalmGuardState(),
    var writingWindow: WritingWindowState = WritingWindowState(),
    var experimentalSizeFilter: Boolean = false
) {
    fun setHandedness(edge: GuardEdge) {
        if (guard.locked) return
        guard = guard.copy(edge = edge)
    }

    fun resize(deltaPx: Float) {
        if (guard.locked) return
        val next = (guard.sizePx + deltaPx).coerceIn(guard.minSizePx, guard.maxSizePx)
        guard = guard.copy(sizePx = next)
    }

    fun setLocked(locked: Boolean) {
        guard = guard.copy(locked = locked)
    }

    /**
     * True if a contact that begins at (x,y) in view space must be blocked from ink.
     */
    fun blocksInkOrigin(x: Float, y: Float, viewW: Float, viewH: Float): Boolean {
        if (guard.enabled && pointInGuard(x, y, viewW, viewH)) return true
        if (writingWindow.enabled && !pointInWindow(x, y)) return true
        return false
    }

    fun pointInGuard(x: Float, y: Float, viewW: Float, viewH: Float): Boolean {
        if (!guard.enabled) return false
        return when (guard.edge) {
            GuardEdge.BOTTOM -> y >= viewH - guard.sizePx
            GuardEdge.LEFT -> x <= guard.sizePx
            GuardEdge.RIGHT -> x >= viewW - guard.sizePx
        }
    }

    fun pointInWindow(x: Float, y: Float): Boolean {
        val w = writingWindow
        return x >= w.left && x <= w.right && y >= w.top && y <= w.bottom
    }

    /** Experimental contact-size filter — default off; never sole defense. */
    fun experimentalRejectBySize(size: Float, threshold: Float = 0.35f): Boolean {
        if (!experimentalSizeFilter) return false
        return size >= threshold
    }
}
