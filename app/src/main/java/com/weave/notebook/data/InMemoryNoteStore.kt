package com.weave.notebook.data

import com.weave.notebook.model.PageSpec
import com.weave.notebook.model.PaperType
import com.weave.notebook.model.StrokeRecord
import java.util.UUID

data class NoteMeta(
    val noteId: String,
    val title: String,
    val modifiedAtMs: Long,
    val pageIds: List<String>
)

/**
 * Authoritative in-memory store used by unit tests and as the live model cache.
 * SQLite journal (Android) will mirror commits in a follow-on wiring step / instrumentation.
 */
class InMemoryNoteStore {
    private val notes = LinkedHashMap<String, NoteMeta>()
    private val pages = HashMap<String, PageSpec>()
    private val strokes = HashMap<String, MutableList<StrokeRecord>>() // pageId -> strokes

    fun createNote(title: String, paper: PaperType = PaperType.RULED): NoteMeta {
        val noteId = UUID.randomUUID().toString()
        val pageId = UUID.randomUUID().toString()
        val page = PageSpec(pageId = pageId, paperType = paper)
        pages[pageId] = page
        strokes[pageId] = mutableListOf()
        val meta = NoteMeta(noteId, title, System.currentTimeMillis(), listOf(pageId))
        notes[noteId] = meta
        return meta
    }

    fun listNotes(): List<NoteMeta> = notes.values.sortedByDescending { it.modifiedAtMs }

    fun getNote(noteId: String): NoteMeta? = notes[noteId]

    fun rename(noteId: String, title: String) {
        val n = notes[noteId] ?: return
        notes[noteId] = n.copy(title = title, modifiedAtMs = System.currentTimeMillis())
    }

    fun duplicate(noteId: String): NoteMeta? {
        val src = notes[noteId] ?: return null
        val copy = createNote(src.title + " copy")
        // Replace default page with cloned pages/strokes and new IDs.
        val newPageIds = mutableListOf<String>()
        for (pid in src.pageIds) {
            val page = pages[pid] ?: continue
            val newPid = UUID.randomUUID().toString()
            pages[newPid] = page.copy(pageId = newPid)
            strokes[newPid] = strokes[pid]?.map {
                it.copy(strokeId = UUID.randomUUID().toString(), pageId = newPid)
            }?.toMutableList() ?: mutableListOf()
            newPageIds += newPid
        }
        // Drop auto-created empty page from createNote
        val auto = copy.pageIds.first()
        pages.remove(auto)
        strokes.remove(auto)
        val meta = copy.copy(pageIds = newPageIds, modifiedAtMs = System.currentTimeMillis())
        notes[meta.noteId] = meta
        return meta
    }

    fun delete(noteId: String) {
        val n = notes.remove(noteId) ?: return
        n.pageIds.forEach {
            pages.remove(it)
            strokes.remove(it)
        }
    }

    fun appendStroke(noteId: String, stroke: StrokeRecord) {
        val list = strokes.getOrPut(stroke.pageId) { mutableListOf() }
        list += stroke
        notes[noteId]?.let { notes[noteId] = it.copy(modifiedAtMs = System.currentTimeMillis()) }
    }

    fun strokesForPage(pageId: String): List<StrokeRecord> = strokes[pageId]?.toList() ?: emptyList()

    fun page(pageId: String): PageSpec? = pages[pageId]

    fun replacePageStrokes(pageId: String, next: List<StrokeRecord>) {
        strokes[pageId] = next.toMutableList()
    }

    fun addPage(noteId: String, paper: PaperType = PaperType.RULED, ruleSpacingMm: Float = 8f): PageSpec? {
        val note = notes[noteId] ?: return null
        val pageId = UUID.randomUUID().toString()
        val page = PageSpec(pageId = pageId, paperType = paper, ruleSpacingMm = ruleSpacingMm)
        pages[pageId] = page
        strokes[pageId] = mutableListOf()
        notes[noteId] = note.copy(pageIds = note.pageIds + pageId, modifiedAtMs = System.currentTimeMillis())
        return page
    }

    fun duplicatePage(noteId: String, pageId: String): PageSpec? {
        val note = notes[noteId] ?: return null
        val src = pages[pageId] ?: return null
        val idx = note.pageIds.indexOf(pageId)
        if (idx < 0) return null
        val newId = UUID.randomUUID().toString()
        val page = src.copy(pageId = newId)
        pages[newId] = page
        strokes[newId] = strokes[pageId]?.map {
            it.copy(strokeId = UUID.randomUUID().toString(), pageId = newId)
        }?.toMutableList() ?: mutableListOf()
        val ids = note.pageIds.toMutableList()
        ids.add(idx + 1, newId)
        notes[noteId] = note.copy(pageIds = ids, modifiedAtMs = System.currentTimeMillis())
        return page
    }

    fun reorderPages(noteId: String, fromIndex: Int, toIndex: Int): Boolean {
        val note = notes[noteId] ?: return false
        if (fromIndex !in note.pageIds.indices || toIndex !in note.pageIds.indices) return false
        val ids = note.pageIds.toMutableList()
        val item = ids.removeAt(fromIndex)
        ids.add(toIndex, item)
        notes[noteId] = note.copy(pageIds = ids, modifiedAtMs = System.currentTimeMillis())
        return true
    }

    fun deletePage(noteId: String, pageId: String): Boolean {
        val note = notes[noteId] ?: return false
        if (note.pageIds.size <= 1) return false // retain at least one page
        if (pageId !in note.pageIds) return false
        val ids = note.pageIds.filterNot { it == pageId }
        pages.remove(pageId)
        strokes.remove(pageId)
        notes[noteId] = note.copy(pageIds = ids, modifiedAtMs = System.currentTimeMillis())
        return true
    }

    fun setPageTemplate(pageId: String, paper: PaperType, ruleSpacingMm: Float = 8f): Boolean {
        val page = pages[pageId] ?: return false
        // Template change never rescales or moves ink.
        pages[pageId] = page.copy(paperType = paper, ruleSpacingMm = ruleSpacingMm)
        return true
    }
}

