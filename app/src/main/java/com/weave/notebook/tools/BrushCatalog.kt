package com.weave.notebook.tools

import com.weave.notebook.model.BrushKind

/**
 * Tool widths (mm) and color presets. Tool changes affect new strokes only.
 */
object BrushCatalog {
    val PEN_WIDTHS_MM = floatArrayOf(0.25f, 0.45f, 0.7f, 1.0f, 1.5f)
    const val PEN_DEFAULT_MM = 0.45f

    val MARKER_WIDTHS_MM = floatArrayOf(1f, 2f, 3f, 4f, 6f)
    const val MARKER_DEFAULT_MM = 2f

    val HIGHLIGHTER_WIDTHS_MM = floatArrayOf(2f, 5f, 8f, 12f)
    const val HIGHLIGHTER_DEFAULT_MM = 5f

    val INK_COLORS = intArrayOf(
        0xFF000000.toInt(), // black
        0xFF1A5FB4.toInt(), // blue
        0xFFC01C28.toInt(), // red
        0xFF2EC27E.toInt()  // green
    )

    val HIGHLIGHTER_COLORS = intArrayOf(
        0x80F6D32D.toInt(), // yellow
        0x80F661C4.toInt(), // pink
        0x8057E389.toInt()  // green
    )

    fun widthsFor(brush: BrushKind): FloatArray = when (brush) {
        BrushKind.PEN -> PEN_WIDTHS_MM
        BrushKind.MARKER -> MARKER_WIDTHS_MM
        BrushKind.HIGHLIGHTER -> HIGHLIGHTER_WIDTHS_MM
        BrushKind.ERASER -> floatArrayOf(2f, 4f, 8f)
    }

    fun defaultWidth(brush: BrushKind): Float = when (brush) {
        BrushKind.PEN -> PEN_DEFAULT_MM
        BrushKind.MARKER -> MARKER_DEFAULT_MM
        BrushKind.HIGHLIGHTER -> HIGHLIGHTER_DEFAULT_MM
        BrushKind.ERASER -> 4f
    }

    fun colorsFor(brush: BrushKind): IntArray = when (brush) {
        BrushKind.HIGHLIGHTER -> HIGHLIGHTER_COLORS
        else -> INK_COLORS
    }

    fun isOpaque(brush: BrushKind): Boolean = brush != BrushKind.HIGHLIGHTER
}

data class ToolSettings(
    val brush: BrushKind = BrushKind.PEN,
    val widthMm: Float = BrushCatalog.PEN_DEFAULT_MM,
    val colorArgb: Int = BrushCatalog.INK_COLORS[0]
) {
    fun withBrush(brush: BrushKind): ToolSettings {
        val colors = BrushCatalog.colorsFor(brush)
        val color = if (colorArgb in colors) colorArgb else colors.first()
        return copy(brush = brush, widthMm = BrushCatalog.defaultWidth(brush), colorArgb = color)
    }
}
