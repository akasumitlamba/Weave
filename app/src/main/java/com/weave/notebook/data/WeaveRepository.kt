package com.weave.notebook.data

/**
 * Process-wide offline repository. No network. Survives within the app process;
 * durable SQLite mirroring expands in later backup/migration work.
 */
object WeaveRepository {
    val store: InMemoryNoteStore = InMemoryNoteStore()
}
