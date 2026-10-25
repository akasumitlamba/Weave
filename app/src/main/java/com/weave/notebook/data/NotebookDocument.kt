package com.weave.notebook.data

import com.weave.notebook.model.*
import java.util.UUID

data class NotebookPage(val spec: PageSpec, val strokes: List<StrokeRecord> = emptyList())
data class NotebookDocument(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val modified: Long = System.currentTimeMillis(),
    val pages: List<NotebookPage> = listOf(NotebookPage(PageSpec(UUID.randomUUID().toString())))
) {
    fun duplicate(): NotebookDocument = copy(id = UUID.randomUUID().toString(), title = "$title copy",
        modified = System.currentTimeMillis(), pages = pages.map { p ->
            val id = UUID.randomUUID().toString()
            NotebookPage(p.spec.copy(pageId = id), p.strokes.map { it.copy(strokeId = UUID.randomUUID().toString(), pageId = id) })
        })
}

/** Immutable lists share stroke samples. Bounds apply to undo and redo together. */
class NotebookHistory {
    private val undo = ArrayDeque<NotebookDocument>()
    private val redo = ArrayDeque<NotebookDocument>()
    val canUndo get() = undo.isNotEmpty()
    val canRedo get() = redo.isNotEmpty()
    fun record(before: NotebookDocument) {
        undo.addLast(before); redo.clear()
        while (undo.size > 60 || (undo.size > 1 && undo.sumOf { d -> d.pages.sumOf { p -> p.strokes.sumOf { it.samples.size } } } > 500_000)) {
            undo.removeFirst()
        }
    }
    fun undo(current: NotebookDocument): NotebookDocument? = undo.removeLastOrNull()?.also { redo.addLast(current) }
    fun redo(current: NotebookDocument): NotebookDocument? = redo.removeLastOrNull()?.also { undo.addLast(current) }
}
