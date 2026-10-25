package com.weave.notebook.shape

import com.google.common.truth.Truth.assertThat
import com.weave.notebook.model.FittedShape
import com.weave.notebook.model.StrokeSample
import org.junit.Test

class ShapeRecognizerTest {

    private val recognizer = ShapeRecognizer(minConfidence = 0.6f, minMargin = 0.05f)

    @Test
    fun openLine_withStableEndpoints_classifiesLine() {
        val samples = (0..20).map { StrokeSample(it.toFloat(), 0f, it.toLong()) }
        val c = recognizer.classify(samples)
        assertThat(c).isNotNull()
        assertThat(c!!.shape).isInstanceOf(FittedShape.Line::class.java)
    }

    @Test
    fun roughClosedLoop_classifiesCircle() {
        val samples = (0..36).map { i ->
            val a = i * (Math.PI * 2 / 36)
            StrokeSample((20 + 8 * kotlin.math.cos(a)).toFloat(), (20 + 8 * kotlin.math.sin(a)).toFloat(), i.toLong())
        }
        val c = recognizer.classify(samples)
        assertThat(c?.shape).isInstanceOf(FittedShape.Circle::class.java)
    }

    @Test
    fun wideClosedLoop_prefersEllipse() {
        val samples = (0..36).map { i ->
            val a = i * (Math.PI * 2 / 36)
            StrokeSample((30 + 12 * kotlin.math.cos(a)).toFloat(), (30 + 5 * kotlin.math.sin(a)).toFloat(), i.toLong())
        }
        val c = recognizer.classify(samples)
        assertThat(c?.shape).isInstanceOf(FittedShape.Ellipse::class.java)
    }

    @Test
    fun tinyDot_noRecognition() {
        val samples = listOf(StrokeSample(1f, 1f, 0), StrokeSample(1.1f, 1.1f, 1))
        assertThat(recognizer.classify(samples)).isNull()
    }

    @Test
    fun dwellController_requiresHoldTime() {
        var now = 100L
        val hold = ShapeHoldController(recognizer, clock = { now })
        val samples = (0..20).map { StrokeSample(it.toFloat(), 0f, it.toLong()) }.toMutableList()
        // Begin hold with a stable endpoint (does not yet meet dwell duration).
        repeat(5) { i -> samples += StrokeSample(20f, 0f, 100L + i) }
        hold.onStrokeProgress(samples, toolAllowsHold = true)
        assertThat(hold.preview).isNull()
        // Advance virtual clock past dwell threshold while endpoint stays put.
        now = 800L
        samples += StrokeSample(20f, 0f, 800L)
        hold.onDwellTick(samples, toolAllowsHold = true)
        assertThat(hold.preview).isNotNull()
    }

    @Test
    fun highlighterTool_disabled() {
        val hold = ShapeHoldController(recognizer, clock = { 1000 })
        val samples = (0..20).map { StrokeSample(it.toFloat(), 0f, it.toLong()) }
        hold.onStrokeProgress(samples, toolAllowsHold = false)
        assertThat(hold.preview).isNull()
    }
}
