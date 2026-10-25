package com.weave.notebook.model

/**
 * Canonical A4 page geometry in millimeters.
 * Portrait page is always 210 × 297 mm regardless of screen rotation.
 */
object PageGeometry {
    const val WIDTH_MM = 210f
    const val HEIGHT_MM = 297f
    const val DEFAULT_RULE_SPACING_MM = 8f
    const val RULE_SPACING_6_MM = 6f
    const val RULE_SPACING_8_MM = 8f
    const val RULE_SPACING_10_MM = 10f
    const val RULE_MARGIN_MM = 15f

    /** PDF points per millimeter (72 / 25.4). */
    const val MM_TO_PDF_POINTS = 72f / 25.4f

    val ALLOWED_RULE_SPACINGS = floatArrayOf(
        RULE_SPACING_6_MM,
        RULE_SPACING_8_MM,
        RULE_SPACING_10_MM
    )

    fun isAllowedRuleSpacing(mm: Float): Boolean =
        ALLOWED_RULE_SPACINGS.any { kotlin.math.abs(it - mm) < 0.001f }
}

enum class PaperType {
    PLAIN,
    RULED
}

/**
 * Immutable page description. Template never influences stroke coordinates.
 */
data class PageSpec(
    val pageId: String,
    val paperType: PaperType = PaperType.RULED,
    val ruleSpacingMm: Float = PageGeometry.DEFAULT_RULE_SPACING_MM,
    val widthMm: Float = PageGeometry.WIDTH_MM,
    val heightMm: Float = PageGeometry.HEIGHT_MM
) {
    init {
        require(widthMm > 0f && heightMm > 0f)
        require(paperType == PaperType.PLAIN || PageGeometry.isAllowedRuleSpacing(ruleSpacingMm))
    }

    val aspectRatio: Float get() = widthMm / heightMm
}

enum class BrushKind {
    PEN,
    MARKER,
    HIGHLIGHTER,
    ERASER
}

data class StrokeSample(
    val xMm: Float,
    val yMm: Float,
    val tMs: Long
)

data class StrokeRecord(
    val strokeId: String,
    val pageId: String,
    val samples: List<StrokeSample>,
    val brush: BrushKind,
    val colorArgb: Int,
    val widthMm: Float,
    val smoothingVersion: Int = 0,
    val fittedShape: FittedShape? = null,
    val rawSamples: List<StrokeSample>? = null
)

sealed class FittedShape {
    data class Line(val x1: Float, val y1: Float, val x2: Float, val y2: Float) : FittedShape()
    data class Circle(val cx: Float, val cy: Float, val radius: Float) : FittedShape()
    data class Ellipse(val cx: Float, val cy: Float, val rx: Float, val ry: Float, val rotationRad: Float) : FittedShape()
    data class Polygon(val points: List<Pair<Float, Float>>, val kind: PolygonKind) : FittedShape()
}

enum class PolygonKind {
    TRIANGLE,
    RECTANGLE,
    SQUARE
}
