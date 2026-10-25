package com.weave.notebook.integration

import com.google.common.truth.Truth.assertThat
import com.weave.notebook.backup.NotebookArchive
import com.weave.notebook.command.CommandHistory
import com.weave.notebook.command.EditorCommand
import com.weave.notebook.data.InMemoryNoteStore
import com.weave.notebook.data.SaveQueue
import com.weave.notebook.data.StrokeCodec
import com.weave.notebook.export.ExportJobController
import com.weave.notebook.export.ExportProgress
import com.weave.notebook.export.ExportRequest
import com.weave.notebook.export.PageSelection
import com.weave.notebook.model.BrushKind
import com.weave.notebook.model.PaperType
import com.weave.notebook.model.StrokeRecord
import com.weave.notebook.model.StrokeSample
import com.weave.notebook.ui.SaveLabel
import org.junit.Test
import java.util.UUID

/**
 * Integration-style suite across editing, save queue, backup, and export validation.
 */
class EndToEndNoteFlowTest {

    @Test
    fun editSaveBackupExport_happyPath() {
        val store = InMemoryNoteStore()
        val note = store.createNote("Study")
        val pageId = note.pageIds.first()
        store.addPage(note.noteId, PaperType.PLAIN)
        val stroke = StrokeRecord(
            strokeId = UUID.randomUUID().toString(),
            pageId = pageId,
            samples = listOf(StrokeSample(1f, 1f, 0), StrokeSample(2f, 2f, 1)),
            brush = BrushKind.PEN,
            colorArgb = 0xFF000000.toInt(),
            widthMm = 0.45f,
            smoothingVersion = 1
        )
        store.appendStroke(note.noteId, stroke)
        val history = CommandHistory()
        history.push(EditorCommand.AddStroke(stroke))

        val committed = mutableListOf<String>()
        val queue = SaveQueue(writer = { committed += it.payloadHash })
        val bytes = StrokeCodec.encode(stroke)
        queue.enqueue(queue.newOp(note.noteId, pageId, bytes))
        queue.flushAll()
        assertThat(queue.status).isEqualTo(SaveLabel.SAVED)

        val exported = NotebookArchive.exportNote(store, note.noteId) as NotebookArchive.Result.Ok
        val restored = NotebookArchive.restoreAsNewNote(store, exported.bytes) as NotebookArchive.Result.Restored
        assertThat(store.getNote(restored.noteId)).isNotNull()
        assertThat(store.getNote(note.noteId)).isNotNull()

        val req = ExportRequest(filename = "Study.pdf", selection = PageSelection.ALL, pageCount = 2)
        assertThat(req.validated()).isInstanceOf(ExportRequest.Validation.Valid::class.java)
        val job = ExportJobController()
        job.start()
        job.completeSuccess()
        assertThat(job.progress).isEqualTo(ExportProgress.SUCCESS)
    }

    @Test
    fun saveFailure_keepsNotesEditable() {
        val store = InMemoryNoteStore()
        val note = store.createNote("X")
        val q = SaveQueue(writer = { error("disk full") })
        q.enqueue(q.newOp(note.noteId, note.pageIds.first(), byteArrayOf(1)))
        assertThat(q.processNext()).isFalse()
        assertThat(q.status).isEqualTo(SaveLabel.SAVE_FAILED)
        assertThat(store.getNote(note.noteId)).isNotNull()
    }
}
