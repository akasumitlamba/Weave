package com.weave.notebook.render

import android.graphics.Path
import com.weave.notebook.model.StrokeSample
import kotlin.math.hypot

/**
 * Builds a single filled outline for a stroke so translucent highlighters
 * do not self-darken on overlapping segments within one stroke.
 */
object StrokeOutlineBuilder {
    fun buildFilledOutline(samples: List<StrokeSample>, widthMm: Float): Path {
        val path = Path()
        if (samples.isEmpty()) return path
        val r = widthMm * 0.5f
        if (samples.size == 1) {
            path.addCircle(samples[0].xMm, samples[0].yMm, r, Path.Direction.CW)
            return path
        }
        val left = ArrayList<Pair<Float, Float>>(samples.size)
        val right = ArrayList<Pair<Float, Float>>(samples.size)
        for (i in samples.indices) {
            val prev = samples[if (i == 0) 0 else i - 1]
            val next = samples[if (i == samples.lastIndex) i else i + 1]
            val dx = next.xMm - prev.xMm
            val dy = next.yMm - prev.yMm
            val len = hypot(dx, dy).coerceAtLeast(1e-4f)
            val nx = -dy / len
            val ny = dx / len
            val p = samples[i]
            left += (p.xMm + nx * r) to (p.yMm + ny * r)
            right += (p.xMm - nx * r) to (p.yMm - ny * r)
        }
        path.moveTo(left.first().first, left.first().second)
        for (i in 1 until left.size) path.lineTo(left[i].first, left[i].second)
        for (i in right.lastIndex downTo 0) path.lineTo(right[i].first, right[i].second)
        path.close()
        return path
    }
}
