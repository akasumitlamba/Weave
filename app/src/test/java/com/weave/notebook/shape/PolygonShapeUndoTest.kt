package com.weave.notebook.shape

import com.google.common.truth.Truth.assertThat
import com.weave.notebook.command.CommandHistory
import com.weave.notebook.command.EditorCommand
import com.weave.notebook.model.BrushKind
import com.weave.notebook.model.FittedShape
import com.weave.notebook.model.PolygonKind
import com.weave.notebook.model.StrokeRecord
import com.weave.notebook.model.StrokeSample
import org.junit.Test

class PolygonShapeUndoTest {

    private val recognizer = ShapeRecognizer(minConfidence = 0.55f, minMargin = 0.03f)

    @Test
    fun triangleFixture_classifiesTriangle() {
        val samples = boxLikePolyline(
            listOf(0f to 0f, 10f to 0f, 5f to 8f, 0f to 0f)
        )
        val c = recognizer.classify(samples)
        assertThat(c?.shape).isInstanceOf(FittedShape.Polygon::class.java)
        assertThat((c!!.shape as FittedShape.Polygon).kind).isEqualTo(PolygonKind.TRIANGLE)
    }

    @Test
    fun squareFixture_classifiesSquareOrRectangle() {
        val samples = boxLikePolyline(
            listOf(0f to 0f, 10f to 0f, 10f to 10f, 0f to 10f, 0f to 0f)
        )
        val c = recognizer.classify(samples)
        assertThat(c?.shape).isInstanceOf(FittedShape.Polygon::class.java)
        val kind = (c!!.shape as FittedShape.Polygon).kind
        assertThat(kind == PolygonKind.SQUARE || kind == PolygonKind.RECTANGLE).isTrue()
    }

    @Test
    fun handwritingNegative_usuallyUnchanged() {
        // Scribble / letter-like
        val samples = (0..40).map { i ->
            StrokeSample(i * 0.3f, kotlin.math.sin(i / 2f) * 2f, i.toLong())
        }
        assertThat(recognizer.classify(samples)).isNull()
    }

    @Test
    fun undo_restoresRawThenRemoves() {
        val raw = StrokeRecord(
            strokeId = "raw",
            pageId = "p",
            samples = boxLikePolyline(listOf(0f to 0f, 10f to 0f, 10f to 10f, 0f to 10f, 0f to 0f)),
            brush = BrushKind.PEN,
            colorArgb = 0xFF000000.toInt(),
            widthMm = 0.45f
        )
        val candidate = ShapeCandidate(
            shape = FittedShape.Polygon(
                listOf(0f to 0f, 10f to 0f, 10f to 10f, 0f to 10f),
                PolygonKind.SQUARE
            ),
            confidence = 0.9f,
            label = "square"
        )
        val (fitted, replace) = ShapeCommit.replaceWithShape(raw, candidate)
        val strokes = mutableListOf(fitted)
        val history = CommandHistory()
        history.push(EditorCommand.AddStroke(raw))
        history.push(replace)

        val firstUndo = history.undo() as EditorCommand.ReplaceStroke
        strokes.removeAll { it.strokeId == firstUndo.after.strokeId }
        strokes.add(firstUndo.before)
        assertThat(strokes.single().fittedShape).isNull()

        val secondUndo = history.undo() as EditorCommand.AddStroke
        strokes.removeAll { it.strokeId == secondUndo.stroke.strokeId }
        assertThat(strokes).isEmpty()
    }

    private fun boxLikePolyline(corners: List<Pair<Float, Float>>): List<StrokeSample> {
        val out = mutableListOf<StrokeSample>()
        var t = 0L
        for (i in 1 until corners.size) {
            val a = corners[i - 1]
            val b = corners[i]
            for (s in 0..8) {
                val f = s / 8f
                out += StrokeSample(a.first + (b.first - a.first) * f, a.second + (b.second - a.second) * f, t++)
            }
        }
        return out
    }
}
