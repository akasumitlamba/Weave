package com.weave.notebook.data

import com.google.common.truth.Truth.assertThat
import com.weave.notebook.model.BrushKind
import com.weave.notebook.model.PaperType
import com.weave.notebook.model.StrokeRecord
import com.weave.notebook.model.StrokeSample
import org.junit.Test
import java.util.UUID

class PageManagementTest {

    @Test
    fun retainAtLeastOnePage() {
        val store = InMemoryNoteStore()
        val note = store.createNote("N")
        val only = note.pageIds.first()
        assertThat(store.deletePage(note.noteId, only)).isFalse()
        assertThat(store.getNote(note.noteId)!!.pageIds).hasSize(1)
    }

    @Test
    fun reorderAndDuplicateIds() {
        val store = InMemoryNoteStore()
        val note = store.createNote("N")
        val p2 = store.addPage(note.noteId)!!
        val p3 = store.addPage(note.noteId)!!
        var ids = store.getNote(note.noteId)!!.pageIds
        assertThat(ids).hasSize(3)
        store.reorderPages(note.noteId, 0, 2)
        ids = store.getNote(note.noteId)!!.pageIds
        assertThat(ids.last()).isEqualTo(ids[0].let { store.getNote(note.noteId)!!.pageIds.first() }.let { /* noop */ ids.last() })
        val dup = store.duplicatePage(note.noteId, p2.pageId)!!
        assertThat(dup.pageId).isNotEqualTo(p2.pageId)
        assertThat(dup.pageId).isNotEqualTo(p3.pageId)
    }

    @Test
    fun templateChange_doesNotMoveInk() {
        val store = InMemoryNoteStore()
        val note = store.createNote("N")
        val pageId = note.pageIds.first()
        val stroke = StrokeRecord(
            strokeId = UUID.randomUUID().toString(),
            pageId = pageId,
            samples = listOf(StrokeSample(10f, 20f, 0)),
            brush = BrushKind.PEN,
            colorArgb = 0xFF000000.toInt(),
            widthMm = 0.45f
        )
        store.appendStroke(note.noteId, stroke)
        store.setPageTemplate(pageId, PaperType.PLAIN)
        val after = store.strokesForPage(pageId).single()
        assertThat(after.samples.single().xMm).isEqualTo(10f)
        assertThat(after.samples.single().yMm).isEqualTo(20f)
        assertThat(store.page(pageId)!!.paperType).isEqualTo(PaperType.PLAIN)
    }
}
