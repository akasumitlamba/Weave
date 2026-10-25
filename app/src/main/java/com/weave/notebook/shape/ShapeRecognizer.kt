package com.weave.notebook.shape

import com.weave.notebook.model.FittedShape
import com.weave.notebook.model.StrokeSample
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

data class ShapeCandidate(
    val shape: FittedShape,
    val confidence: Float,
    val label: String
)

/**
 * Lightweight geometry for hold-to-recognize: line, circle, ellipse first.
 * Conservative thresholds prefer freehand over false snaps.
 */
class ShapeRecognizer(
    var dwellMs: Long = 650L,
    var holdRadiusMm: Float = 2f,
    var minConfidence: Float = 0.72f,
    var minMargin: Float = 0.08f
) {
    fun classify(samples: List<StrokeSample>): ShapeCandidate? {
        if (samples.size < 4) return null
        val pathLen = pathLength(samples)
        if (pathLen < 8f) return null // tiny stroke / dot

        val line = fitLine(samples)
        val circle = fitCircle(samples)
        val ellipse = fitEllipse(samples)
        val ranked = listOfNotNull(line, circle, ellipse).sortedByDescending { it.confidence }
        if (ranked.isEmpty()) return null
        val best = ranked.first()
        val second = ranked.getOrNull(1)?.confidence ?: 0f
        if (best.confidence < minConfidence) return null
        if (best.confidence - second < minMargin) return null
        return best
    }

    fun endpointStable(samples: List<StrokeSample>, nowMs: Long, holdStartMs: Long): Boolean {
        if (samples.isEmpty()) return false
        if (nowMs - holdStartMs < dwellMs) return false
        val end = samples.last()
        // Only samples collected during the hold itself must stay in radius.
        val window = samples.filter { it.tMs >= holdStartMs }
        if (window.isEmpty()) return false
        return window.all { hypot(it.xMm - end.xMm, it.yMm - end.yMm) <= holdRadiusMm }
    }

    private fun fitLine(samples: List<StrokeSample>): ShapeCandidate? {
        val a = samples.first()
        val b = samples.last()
        val len = hypot(b.xMm - a.xMm, b.yMm - a.yMm).coerceAtLeast(1e-3f)
        var err = 0f
        for (s in samples) {
            err += distToSegment(s.xMm, s.yMm, a.xMm, a.yMm, b.xMm, b.yMm)
        }
        err /= samples.size
        val score = (1f - (err / (len * 0.08f)).coerceIn(0f, 1f))
        val closure = hypot(a.xMm - b.xMm, a.yMm - b.yMm)
        val openBonus = if (closure > 4f) 0.05f else -0.2f
        val confidence = (score + openBonus).coerceIn(0f, 1f)
        return ShapeCandidate(FittedShape.Line(a.xMm, a.yMm, b.xMm, b.yMm), confidence, "line")
    }

    private fun fitCircle(samples: List<StrokeSample>): ShapeCandidate? {
        val xs = samples.map { it.xMm }
        val ys = samples.map { it.yMm }
        val cx = xs.average().toFloat()
        val cy = ys.average().toFloat()
        val radii = samples.map { hypot(it.xMm - cx, it.yMm - cy) }
        val r = radii.average().toFloat()
        if (r < 3f) return null
        val radialErr = radii.map { abs(it - r) }.average().toFloat()
        val closure = hypot(samples.first().xMm - samples.last().xMm, samples.first().yMm - samples.last().yMm)
        val closed = closure <= r * 0.35f
        if (!closed) return null
        val score = (1f - (radialErr / (r * 0.2f)).coerceIn(0f, 1f))
        return ShapeCandidate(FittedShape.Circle(cx, cy, r), score, "circle")
    }

    private fun fitEllipse(samples: List<StrokeSample>): ShapeCandidate? {
        val minX = samples.minOf { it.xMm }
        val maxX = samples.maxOf { it.xMm }
        val minY = samples.minOf { it.yMm }
        val maxY = samples.maxOf { it.yMm }
        val rx = (maxX - minX) / 2f
        val ry = (maxY - minY) / 2f
        if (rx < 3f || ry < 3f) return null
        val aspect = max(rx, ry) / min(rx, ry)
        if (aspect < 1.25f) return null // prefer circle when nearly round
        val cx = (minX + maxX) / 2f
        val cy = (minY + maxY) / 2f
        val closure = hypot(samples.first().xMm - samples.last().xMm, samples.first().yMm - samples.last().yMm)
        if (closure > max(rx, ry) * 0.4f) return null
        var err = 0f
        for (s in samples) {
            val nx = (s.xMm - cx) / rx
            val ny = (s.yMm - cy) / ry
            err += abs(sqrt(nx * nx + ny * ny) - 1f)
        }
        err /= samples.size
        val score = (1f - (err / 0.35f).coerceIn(0f, 1f)) * 0.95f
        return ShapeCandidate(FittedShape.Ellipse(cx, cy, rx, ry, 0f), score, "ellipse")
    }

    private fun pathLength(samples: List<StrokeSample>): Float {
        var len = 0f
        for (i in 1 until samples.size) {
            len += hypot(samples[i].xMm - samples[i - 1].xMm, samples[i].yMm - samples[i - 1].yMm)
        }
        return len
    }

    private fun distToSegment(px: Float, py: Float, x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val dx = x2 - x1
        val dy = y2 - y1
        if (dx == 0f && dy == 0f) return hypot(px - x1, py - y1)
        val t = (((px - x1) * dx + (py - y1) * dy) / (dx * dx + dy * dy)).coerceIn(0f, 1f)
        return hypot(px - (x1 + t * dx), py - (y1 + t * dy))
    }
}

/**
 * Virtual-clock dwell hold controller. Can fire without new MOVE events.
 */
class ShapeHoldController(
    val recognizer: ShapeRecognizer = ShapeRecognizer(),
    private val clock: () -> Long = { System.currentTimeMillis() }
) {
    private var holdStartMs: Long? = null
    private var armed = true
    var preview: ShapeCandidate? = null
        private set

    fun onStrokeProgress(samples: List<StrokeSample>, toolAllowsHold: Boolean) {
        if (!toolAllowsHold || !armed || samples.isEmpty()) {
            clearPreview()
            return
        }
        val now = clock()
        val end = samples.last()
        val stable = samples.takeLast(5).all {
            hypot(it.xMm - end.xMm, it.yMm - end.yMm) <= recognizer.holdRadiusMm
        }
        if (!stable) {
            holdStartMs = null
            clearPreview()
            return
        }
        if (holdStartMs == null) holdStartMs = now
        if (recognizer.endpointStable(samples, now, holdStartMs!!)) {
            preview = recognizer.classify(samples)
        }
    }

    /** Called when no MOVE arrives but finger still down. */
    fun onDwellTick(samples: List<StrokeSample>, toolAllowsHold: Boolean) {
        onStrokeProgress(samples, toolAllowsHold)
    }

    fun onMoveBeyondHold() {
        armed = false
        clearPreview()
        holdStartMs = null
    }

    fun commit(): ShapeCandidate? {
        val p = preview
        clearPreview()
        holdStartMs = null
        armed = true
        return p
    }

    fun cancel() {
        clearPreview()
        holdStartMs = null
        armed = true
    }

    fun resetForNewStroke() {
        cancel()
    }

    private fun clearPreview() {
        preview = null
    }
}
