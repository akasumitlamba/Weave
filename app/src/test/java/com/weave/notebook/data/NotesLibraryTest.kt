package com.weave.notebook.data

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class NotesLibraryTest {

    @Test
    fun emptyLibrary_createOpenIndependentNotes() {
        val store = InMemoryNoteStore()
        assertThat(store.listNotes()).isEmpty()
        val a = store.createNote("A")
        val b = store.createNote("B")
        assertThat(store.listNotes().map { it.noteId }).containsExactly(b.noteId, a.noteId)
        assertThat(a.noteId).isNotEqualTo(b.noteId)
        assertThat(a.pageIds.first()).isNotEqualTo(b.pageIds.first())
    }

    @Test
    fun duplicate_getsNewIds() {
        val store = InMemoryNoteStore()
        val a = store.createNote("Physics")
        val dup = store.duplicate(a.noteId)!!
        assertThat(dup.noteId).isNotEqualTo(a.noteId)
        assertThat(dup.pageIds.first()).isNotEqualTo(a.pageIds.first())
        assertThat(dup.title).contains("copy")
    }

    @Test
    fun delete_requiresExplicitCall() {
        val store = InMemoryNoteStore()
        val a = store.createNote("X")
        store.delete(a.noteId)
        assertThat(store.getNote(a.noteId)).isNull()
        assertThat(store.listNotes()).isEmpty()
    }

    @Test
    fun rename_updatesTitle() {
        val store = InMemoryNoteStore()
        val a = store.createNote("Old")
        store.rename(a.noteId, "New")
        assertThat(store.getNote(a.noteId)!!.title).isEqualTo("New")
    }
}
