package com.weave.notebook.smooth

import com.weave.notebook.model.StrokeSample
import kotlin.math.hypot
import kotlin.math.pow

enum class SmoothingLevel {
    OFF,
    LOW,
    MEDIUM
}

/**
 * Bounded low-pass filter + optional post-stroke simplification.
 * Preserves raw samples separately; never shifts finished words toward ruled lines.
 */
class StrokeSmoother(
    var level: SmoothingLevel = SmoothingLevel.LOW
) {
    data class FilterState(
        var x: Float = 0f,
        var y: Float = 0f,
        var initialized: Boolean = false
    )

    private val liveState = FilterState()

    fun resetLive() {
        liveState.initialized = false
    }

    /** Process one new sample for live rendering. Raw list remains authoritative. */
    fun filterLive(sample: StrokeSample): StrokeSample {
        if (level == SmoothingLevel.OFF) return sample
        val alpha = alphaFor(level)
        if (!liveState.initialized) {
            liveState.x = sample.xMm
            liveState.y = sample.yMm
            liveState.initialized = true
            return sample
        }
        liveState.x += alpha * (sample.xMm - liveState.x)
        liveState.y += alpha * (sample.yMm - liveState.y)
        return StrokeSample(liveState.x, liveState.y, sample.tMs)
    }

    /**
     * Post-UP simplification preserving endpoints and sharp corners within [errorMm].
     * Operates on a copy; does not mutate raw input.
     */
    fun simplifyAfterUp(
        raw: List<StrokeSample>,
        errorMm: Float = when (level) {
            SmoothingLevel.OFF -> 0f
            SmoothingLevel.LOW -> 0.15f
            SmoothingLevel.MEDIUM -> 0.35f
        }
    ): List<StrokeSample> {
        if (level == SmoothingLevel.OFF || raw.size < 3 || errorMm <= 0f) return raw
        val filtered = ArrayList<StrokeSample>(raw.size)
        resetLive()
        for (s in raw) filtered += filterLive(s)
        val simplified = rdp(filtered, errorMm).toMutableList()
        // Endpoints must match the raw stroke exactly (no post-lift visible shift).
        simplified[0] = raw.first()
        simplified[simplified.lastIndex] = raw.last()
        return simplified
    }

    fun midpointQuadraticPath(samples: List<StrokeSample>): List<Triple<Float, Float, Boolean>> {
        // Representation for renderer: list of (x,y,isMove); midpoints for quadratic segments.
        if (samples.isEmpty()) return emptyList()
        val out = ArrayList<Triple<Float, Float, Boolean>>(samples.size * 2)
        out += Triple(samples[0].xMm, samples[0].yMm, true)
        if (samples.size == 1) return out
        for (i in 1 until samples.size) {
            val prev = samples[i - 1]
            val cur = samples[i]
            val mx = (prev.xMm + cur.xMm) * 0.5f
            val my = (prev.yMm + cur.yMm) * 0.5f
            out += Triple(mx, my, false)
            out += Triple(cur.xMm, cur.yMm, false)
        }
        return out
    }

    private fun alphaFor(level: SmoothingLevel): Float = when (level) {
        SmoothingLevel.OFF -> 1f
        SmoothingLevel.LOW -> 0.45f
        SmoothingLevel.MEDIUM -> 0.25f
    }

    private fun rdp(points: List<StrokeSample>, epsilon: Float): List<StrokeSample> {
        if (points.size < 3) return points
        var maxDist = 0f
        var index = 0
        val first = points.first()
        val last = points.last()
        for (i in 1 until points.lastIndex) {
            val d = perpendicularDistance(points[i], first, last)
            if (d > maxDist) {
                index = i
                maxDist = d
            }
        }
        // Preserve sharp corners: if turning angle is large, keep the vertex.
        if (maxDist > epsilon || isSharpCorner(points, index)) {
            val left = rdp(points.subList(0, index + 1), epsilon)
            val right = rdp(points.subList(index, points.size), epsilon)
            return left.dropLast(1) + right
        }
        return listOf(first, last)
    }

    private fun isSharpCorner(points: List<StrokeSample>, index: Int): Boolean {
        if (index <= 0 || index >= points.lastIndex) return false
        val a = points[index - 1]
        val b = points[index]
        val c = points[index + 1]
        val abx = b.xMm - a.xMm
        val aby = b.yMm - a.yMm
        val bcx = c.xMm - b.xMm
        val bcy = c.yMm - b.yMm
        val dot = abx * bcx + aby * bcy
        val mag = hypot(abx, aby) * hypot(bcx, bcy)
        if (mag < 1e-4f) return false
        val cos = (dot / mag).coerceIn(-1f, 1f)
        return cos < 0.2f // ~78°+ turn
    }

    private fun perpendicularDistance(p: StrokeSample, a: StrokeSample, b: StrokeSample): Float {
        val dx = b.xMm - a.xMm
        val dy = b.yMm - a.yMm
        if (dx == 0f && dy == 0f) return hypot(p.xMm - a.xMm, p.yMm - a.yMm)
        val t = ((p.xMm - a.xMm) * dx + (p.yMm - a.yMm) * dy) / (dx.pow(2) + dy.pow(2))
        val projX = a.xMm + t * dx
        val projY = a.yMm + t * dy
        return hypot(p.xMm - projX, p.yMm - projY)
    }

    companion object {
        const val SMOOTHING_VERSION = 1
    }
}
