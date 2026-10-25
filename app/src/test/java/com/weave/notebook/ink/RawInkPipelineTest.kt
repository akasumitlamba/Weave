package com.weave.notebook.ink

import com.google.common.truth.Truth.assertThat
import com.weave.notebook.model.BrushKind
import org.junit.Before
import org.junit.Test

class RawInkPipelineTest {

    private lateinit var pipeline: RawInkPipeline

    @Before
    fun setUp() {
        pipeline = RawInkPipeline()
        pipeline.configureStroke("page-1", BrushKind.PEN, 0xFF000000.toInt(), 0.45f)
    }

    @Test
    fun continuousStroke_keepsHistoricalSamplesInOrder() {
        pipeline.onDown(pt(1, 0f, 0f, 0))
        pipeline.onMove(
            listOf(
                pt(1, 1f, 0f, 1, historical = true),
                pt(1, 2f, 0f, 2, historical = true),
                pt(1, 3f, 0f, 3, historical = false)
            )
        )
        val stroke = pipeline.onUp(pt(1, 4f, 0f, 4))
        assertThat(stroke).isNotNull()
        assertThat(stroke!!.samples.map { it.xMm }).containsExactly(0f, 1f, 2f, 3f, 4f).inOrder()
        assertThat(pipeline.phase).isEqualTo(RawInkPipeline.Phase.IDLE)
    }

    @Test
    fun singleDot_commitsOneSampleStroke() {
        pipeline.onDown(pt(7, 10f, 20f, 0))
        val stroke = pipeline.onUp(pt(7, 10f, 20f, 1))
        assertThat(stroke!!.samples).hasSize(2)
        assertThat(stroke.samples.first().xMm).isEqualTo(10f)
    }

    @Test
    fun cancel_removesProvisionalInk() {
        pipeline.onDown(pt(1, 0f, 0f, 0))
        pipeline.onMove(listOf(pt(1, 5f, 5f, 1)))
        assertThat(pipeline.provisionalSamples).isNotEmpty()
        assertThat(pipeline.onCancel()).isTrue()
        assertThat(pipeline.provisionalSamples).isEmpty()
        assertThat(pipeline.phase).isEqualTo(RawInkPipeline.Phase.IDLE)
        assertThat(pipeline.activePointerId).isNull()
    }

    @Test
    fun foreignPointerMove_ignoredWhileOwned() {
        pipeline.onDown(pt(1, 0f, 0f, 0))
        pipeline.onMove(listOf(pt(2, 50f, 50f, 1), pt(1, 1f, 1f, 2)))
        assertThat(pipeline.provisionalSamples.map { it.xMm }).containsExactly(0f, 1f).inOrder()
    }

    @Test
    fun outOfPagePoints_stillCapturedInModel() {
        // Clipping is a render concern; model keeps samples for recoverability.
        pipeline.onDown(pt(1, -5f, -5f, 0))
        pipeline.onMove(listOf(pt(1, 300f, 400f, 1)))
        val stroke = pipeline.onUp(pt(1, 300f, 400f, 2))
        assertThat(stroke!!.samples).hasSize(3)
    }

    private fun pt(
        id: Int,
        x: Float,
        y: Float,
        t: Long,
        historical: Boolean = false
    ) = RawInkPipeline.PointerPoint(id, x, y, t, historical)
}
