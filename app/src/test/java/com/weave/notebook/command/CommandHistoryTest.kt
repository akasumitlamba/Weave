package com.weave.notebook.command

import com.google.common.truth.Truth.assertThat
import com.weave.notebook.model.BrushKind
import com.weave.notebook.model.StrokeRecord
import com.weave.notebook.model.StrokeSample
import org.junit.Test

class CommandHistoryTest {

    @Test
    fun undoRedo_restoresErasedStroke() {
        val history = CommandHistory()
        val stroke = stroke("s1", listOf(0f to 0f, 10f to 0f))
        val strokes = mutableListOf(stroke)
        history.push(EditorCommand.AddStroke(stroke))
        val erased = StrokeEraser.hitTest(strokes, 5f, 0f, 2f)
        assertThat(erased).isEqualTo(stroke)
        strokes.remove(erased)
        history.push(EditorCommand.EraseStroke(erased!!))
        val undo = history.undo()
        assertThat(undo).isInstanceOf(EditorCommand.EraseStroke::class.java)
        strokes.add((undo as EditorCommand.EraseStroke).stroke)
        assertThat(strokes).contains(stroke)
        val redo = history.redo()
        assertThat(redo).isInstanceOf(EditorCommand.EraseStroke::class.java)
    }

    @Test
    fun newEdit_clearsRedo() {
        val h = CommandHistory()
        val a = stroke("a", listOf(0f to 0f))
        val b = stroke("b", listOf(1f to 1f))
        h.push(EditorCommand.AddStroke(a))
        h.undo()
        assertThat(h.canRedo).isTrue()
        h.push(EditorCommand.AddStroke(b))
        assertThat(h.canRedo).isFalse()
    }

    @Test
    fun emptyHistory_undoNull() {
        assertThat(CommandHistory().undo()).isNull()
    }

    @Test
    fun memoryBounds_trimOldest() {
        val h = CommandHistory(maxCommands = 3)
        repeat(5) { i ->
            h.push(EditorCommand.AddStroke(stroke("s$i", listOf(i.toFloat() to 0f))))
        }
        var count = 0
        while (h.undo() != null) count++
        assertThat(count).isEqualTo(3)
    }

    @Test
    fun overlappingStrokes_hitsTopmost() {
        val bottom = stroke("b", listOf(0f to 0f, 10f to 0f))
        val top = stroke("t", listOf(0f to 0f, 10f to 0f))
        val hit = StrokeEraser.hitTest(listOf(bottom, top), 5f, 0f, 1f)
        assertThat(hit?.strokeId).isEqualTo("t")
    }

    private fun stroke(id: String, pts: List<Pair<Float, Float>>) = StrokeRecord(
        strokeId = id,
        pageId = "p",
        samples = pts.mapIndexed { i, (x, y) -> StrokeSample(x, y, i.toLong()) },
        brush = BrushKind.PEN,
        colorArgb = 0xFF000000.toInt(),
        widthMm = 0.45f
    )
}
