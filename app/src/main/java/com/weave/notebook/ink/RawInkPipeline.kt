package com.weave.notebook.ink

import com.weave.notebook.model.BrushKind
import com.weave.notebook.model.StrokeRecord
import com.weave.notebook.model.StrokeSample
import java.util.UUID

/**
 * Immediate live ink capture using pointer IDs and ordered samples.
 * Pure logic so unit tests can replay without MotionEvent / Robolectric.
 */
class RawInkPipeline(
    private val maxTransientSamples: Int = 20_000
) {
    data class PointerPoint(
        val pointerId: Int,
        val xMm: Float,
        val yMm: Float,
        val tMs: Long,
        val historical: Boolean = false
    )

    enum class Phase { IDLE, DRAWING }

    var phase: Phase = Phase.IDLE
        private set
    var activePointerId: Int? = null
        private set

    private val provisional = ArrayList<StrokeSample>(256)
    private var brush: BrushKind = BrushKind.PEN
    private var colorArgb: Int = 0xFF000000.toInt()
    private var widthMm: Float = 0.45f
    private var pageId: String = ""

    val provisionalSamples: List<StrokeSample> get() = provisional.toList()

    fun configureStroke(pageId: String, brush: BrushKind, colorArgb: Int, widthMm: Float) {
        this.pageId = pageId
        this.brush = brush
        this.colorArgb = colorArgb
        this.widthMm = widthMm
    }

    /**
     * Begin stroke with the first eligible contact. Additional contacts are ignored
     * for ink ownership (gesture arbitration refined in PR 06).
     */
    fun onDown(point: PointerPoint): Boolean {
        if (phase == Phase.DRAWING) return false
        phase = Phase.DRAWING
        activePointerId = point.pointerId
        provisional.clear()
        append(point)
        return true
    }

    fun onMove(points: List<PointerPoint>): Boolean {
        if (phase != Phase.DRAWING) return false
        val id = activePointerId ?: return false
        for (p in points) {
            if (p.pointerId != id) continue
            append(p)
        }
        return true
    }

    fun onUp(point: PointerPoint): StrokeRecord? {
        if (phase != Phase.DRAWING) return null
        val id = activePointerId ?: return null
        if (point.pointerId != id) return null
        append(point)
        val record = commitProvisional()
        reset()
        return record
    }

    /** CANCEL removes provisional ink and resets ownership. */
    fun onCancel(): Boolean {
        if (phase != Phase.DRAWING) {
            reset()
            return false
        }
        provisional.clear()
        reset()
        return true
    }

    fun reset() {
        phase = Phase.IDLE
        activePointerId = null
        provisional.clear()
    }

    private fun append(point: PointerPoint) {
        if (provisional.size >= maxTransientSamples) {
            // Bound memory: drop oldest interior sample, keep endpoints trend.
            if (provisional.size > 2) provisional.removeAt(1)
        }
        provisional += StrokeSample(point.xMm, point.yMm, point.tMs)
    }

    private fun commitProvisional(): StrokeRecord {
        return StrokeRecord(
            strokeId = UUID.randomUUID().toString(),
            pageId = pageId,
            samples = provisional.toList(),
            brush = brush,
            colorArgb = colorArgb,
            widthMm = widthMm,
            smoothingVersion = 0
        )
    }
}
