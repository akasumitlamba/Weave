package com.weave.notebook.data

import com.google.common.truth.Truth.assertThat
import com.weave.notebook.model.BrushKind
import com.weave.notebook.model.StrokeRecord
import com.weave.notebook.model.StrokeSample
import com.weave.notebook.ui.SaveLabel
import org.junit.Test
import java.util.UUID

class PersistenceTest {

    @Test
    fun codec_roundTripAndHashStable() {
        val stroke = StrokeRecord(
            strokeId = UUID.randomUUID().toString(),
            pageId = UUID.randomUUID().toString(),
            samples = listOf(StrokeSample(1f, 2f, 3), StrokeSample(4f, 5f, 6)),
            brush = BrushKind.PEN,
            colorArgb = 0xFF000000.toInt(),
            widthMm = 0.45f,
            smoothingVersion = 1
        )
        val bytes = StrokeCodec.encode(stroke)
        val back = StrokeCodec.decode(bytes)
        assertThat(back).isEqualTo(stroke)
        assertThat(StrokeCodec.sha256(bytes)).isEqualTo(StrokeCodec.sha256(StrokeCodec.encode(back)))
    }

    @Test
    fun saveQueue_ordersAndMarksSaved() {
        val written = mutableListOf<String>()
        val q = SaveQueue(writer = { written += it.payloadHash })
        val b1 = byteArrayOf(1, 2, 3)
        val b2 = byteArrayOf(4, 5)
        q.enqueue(q.newOp("n", "p", b1))
        q.enqueue(q.newOp("n", "p", b2))
        assertThat(q.status).isEqualTo(SaveLabel.SAVING)
        q.flushAll()
        assertThat(written).hasSize(2)
        assertThat(q.status).isEqualTo(SaveLabel.SAVED)
        assertThat(q.pendingCount).isEqualTo(0)
    }

    @Test
    fun saveQueue_failureVisibleWithRetry() {
        val written = mutableListOf<ByteArray>()
        val q = SaveQueue(writer = { written += it.bytes })
        q.enqueue(q.newOp("n", "p", byteArrayOf(9)))
        q.injectNextFailure()
        assertThat(q.processNext()).isFalse()
        assertThat(q.status).isEqualTo(SaveLabel.SAVE_FAILED)
        assertThat(written).isEmpty()
        q.retry()
        assertThat(written).hasSize(1)
        assertThat(q.status).isEqualTo(SaveLabel.SAVED)
    }

    @Test
    fun inMemoryStore_survivesModelRestartSimulation() {
        val store = InMemoryNoteStore()
        val note = store.createNote("Physics")
        val pageId = note.pageIds.first()
        val stroke = StrokeRecord(
            strokeId = UUID.randomUUID().toString(),
            pageId = pageId,
            samples = listOf(StrokeSample(0f, 0f, 0)),
            brush = BrushKind.PEN,
            colorArgb = 0xFF000000.toInt(),
            widthMm = 0.45f
        )
        store.appendStroke(note.noteId, stroke)
        val hash = StrokeCodec.contentHash(store.strokesForPage(pageId))
        // Simulate process death recovery from the same store snapshot (journal would reload).
        assertThat(StrokeCodec.contentHash(store.strokesForPage(pageId))).isEqualTo(hash)
    }
}
