package com.weave.notebook.data

import com.weave.notebook.ui.SaveLabel
import java.util.ArrayDeque
import java.util.concurrent.atomic.AtomicReference

/**
 * Serialized IO worker abstraction. UI observes [status] derived from commit acknowledgment.
 * Never silently drops the queue.
 */
class SaveQueue(
    private val writer: (SaveOp) -> Unit,
    private val nowMs: () -> Long = { System.currentTimeMillis() }
) {
    data class SaveOp(
        val noteId: String,
        val pageId: String,
        val payloadHash: String,
        val bytes: ByteArray,
        val enqueuedAtMs: Long
    )

    private val queue = ArrayDeque<SaveOp>()
    private val statusRef = AtomicReference(SaveLabel.SAVED)
    private var failNext = false

    val status: SaveLabel get() = statusRef.get()
    val pendingCount: Int get() = synchronized(queue) { queue.size }

    fun enqueue(op: SaveOp) {
        synchronized(queue) {
            queue.addLast(op)
            statusRef.set(SaveLabel.SAVING)
        }
    }

    /** Process one op on the IO worker. */
    fun processNext(): Boolean {
        val op = synchronized(queue) { queue.firstOrNull() } ?: run {
            if (statusRef.get() != SaveLabel.SAVE_FAILED) statusRef.set(SaveLabel.SAVED)
            return false
        }
        return try {
            if (failNext) {
                failNext = false
                error("injected failure")
            }
            writer(op)
            synchronized(queue) {
                queue.removeFirst()
                statusRef.set(if (queue.isEmpty()) SaveLabel.SAVED else SaveLabel.SAVING)
            }
            true
        } catch (_: Throwable) {
            statusRef.set(SaveLabel.SAVE_FAILED)
            false
        }
    }

    fun retry() {
        if (statusRef.get() == SaveLabel.SAVE_FAILED) {
            statusRef.set(SaveLabel.SAVING)
            processNext()
        }
    }

    fun flushAll() {
        while (pendingCount > 0 && status != SaveLabel.SAVE_FAILED) {
            if (!processNext()) break
        }
    }

    /** Test hook */
    fun injectNextFailure() {
        failNext = true
    }

    fun snapshotHashes(): List<String> = synchronized(queue) { queue.map { it.payloadHash } }

    fun newOp(noteId: String, pageId: String, bytes: ByteArray) = SaveOp(
        noteId = noteId,
        pageId = pageId,
        payloadHash = StrokeCodec.sha256(bytes),
        bytes = bytes,
        enqueuedAtMs = nowMs()
    )
}
