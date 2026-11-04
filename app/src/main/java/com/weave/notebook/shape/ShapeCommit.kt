package com.weave.notebook.shape

import com.weave.notebook.command.EditorCommand
import com.weave.notebook.model.StrokeRecord
import com.weave.notebook.model.StrokeSample

/**
 * Commits a fitted shape while retaining the raw stroke for undo recovery.
 * First undo restores freehand; second undo removes the stroke.
 */
object ShapeCommit {
    fun replaceWithShape(raw: StrokeRecord, candidate: ShapeCandidate): Pair<StrokeRecord, EditorCommand.ReplaceStroke> {
        val fittedSamples = samplesForShape(candidate)
        val fitted = raw.copy(samples = fittedSamples, fittedShape = candidate.shape, rawSamples = raw.samples)
        return fitted to EditorCommand.ReplaceStroke(before = raw, after = fitted)
    }

    private fun samplesForShape(candidate: ShapeCandidate): List<StrokeSample> {
        return when (val s = candidate.shape) {
            is com.weave.notebook.model.FittedShape.Line -> listOf(
                StrokeSample(s.x1, s.y1, 0),
                StrokeSample(s.x2, s.y2, 1)
            )
            is com.weave.notebook.model.FittedShape.Circle -> (0..36).map { i ->
                val a = i * (Math.PI * 2 / 36)
                StrokeSample(
                    (s.cx + s.radius * kotlin.math.cos(a)).toFloat(),
                    (s.cy + s.radius * kotlin.math.sin(a)).toFloat(),
                    i.toLong()
                )
            }
            is com.weave.notebook.model.FittedShape.Ellipse -> (0..36).map { i ->
                val a = i * (Math.PI * 2 / 36)
                StrokeSample(
                    (s.cx + s.rx * kotlin.math.cos(a)).toFloat(),
                    (s.cy + s.ry * kotlin.math.sin(a)).toFloat(),
                    i.toLong()
                )
            }
            is com.weave.notebook.model.FittedShape.Polygon -> {
                val pts = s.points + s.points.first()
                pts.mapIndexed { i, p -> StrokeSample(p.first, p.second, i.toLong()) }
            }
        }
    }
}
