package com.weave.notebook.smooth

import com.google.common.truth.Truth.assertThat
import com.weave.notebook.model.StrokeSample
import org.junit.Test
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.random.Random

class StrokeSmootherTest {

    @Test
    fun dots_surviveSimplification() {
        val s = StrokeSmoother(SmoothingLevel.MEDIUM)
        val raw = listOf(StrokeSample(10f, 10f, 0), StrokeSample(10.01f, 10.01f, 1))
        val out = s.simplifyAfterUp(raw)
        assertThat(out.first().xMm).isWithin(0.05f).of(10f)
        assertThat(out.last().xMm).isWithin(0.05f).of(10.01f)
    }

    @Test
    fun corners_retainedOnAngularLetter() {
        // Synthetic "N" corners
        val raw = listOf(
            StrokeSample(0f, 0f, 0),
            StrokeSample(0f, 10f, 1),
            StrokeSample(5f, 0f, 2),
            StrokeSample(5f, 10f, 3)
        )
        val out = StrokeSmoother(SmoothingLevel.LOW).simplifyAfterUp(raw, errorMm = 0.2f)
        assertThat(out.size).isAtLeast(3)
        assertThat(out.first().xMm).isWithin(0.01f).of(raw.first().xMm)
        assertThat(out.first().yMm).isWithin(0.01f).of(raw.first().yMm)
        assertThat(out.last().xMm).isWithin(0.2f).of(raw.last().xMm)
        assertThat(out.last().yMm).isWithin(0.2f).of(raw.last().yMm)
    }

    @Test
    fun jitterStraightSegment_deviationBounded() {
        val rng = Random(42)
        val raw = (0..40).map { i ->
            StrokeSample(i * 0.5f, (rng.nextFloat() - 0.5f) * 0.4f, i.toLong())
        }
        val smoother = StrokeSmoother(SmoothingLevel.LOW)
        smoother.resetLive()
        val live = raw.map { smoother.filterLive(it) }
        val maxDev = live.maxOf { abs(it.yMm) }
        assertThat(maxDev).isLessThan(0.35f)
        val simplified = StrokeSmoother(SmoothingLevel.LOW).simplifyAfterUp(raw)
        assertThat(hypot(simplified.first().xMm - raw.first().xMm, 0f)).isLessThan(0.01f)
    }

    @Test
    fun off_preservesExactSamples() {
        val raw = listOf(
            StrokeSample(1f, 2f, 0),
            StrokeSample(3f, 4f, 1),
            StrokeSample(5f, 6f, 2)
        )
        val s = StrokeSmoother(SmoothingLevel.OFF)
        assertThat(s.simplifyAfterUp(raw)).isEqualTo(raw)
        assertThat(s.filterLive(raw[0])).isEqualTo(raw[0])
    }

    @Test
    fun cursiveLoopFixture_endpointsStable() {
        val raw = (0..36).map { i ->
            val a = i * (Math.PI * 2 / 36)
            StrokeSample(
                (10 + 3 * kotlin.math.cos(a)).toFloat(),
                (10 + 2 * kotlin.math.sin(a)).toFloat(),
                i.toLong()
            )
        }
        val out = StrokeSmoother(SmoothingLevel.MEDIUM).simplifyAfterUp(raw)
        assertThat(out.first().xMm).isWithin(0.01f).of(raw.first().xMm)
        assertThat(out.last().xMm).isWithin(1.0f).of(raw.last().xMm)
        assertThat(out.last().yMm).isWithin(1.0f).of(raw.last().yMm)
        assertThat(out.size).isAtLeast(2)
    }
}
