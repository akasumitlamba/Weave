package com.weave.notebook.backup

import com.weave.notebook.data.InMemoryNoteStore
import com.weave.notebook.data.StrokeCodec
import com.weave.notebook.model.PageSpec
import com.weave.notebook.model.PaperType
import com.weave.notebook.model.StrokeRecord
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Versioned editable notebook archive.
 * Manifest is a simple line-oriented format (no org.json dependency for unit tests).
 * Restore always imports as a new note; rejects unsafe paths and bad schemas.
 */
object NotebookArchive {
    const val MANIFEST_VERSION = 1
    const val MANIFEST_NAME = "manifest.txt"

    sealed class Result {
        data class Ok(val bytes: ByteArray) : Result()
        data class Restored(val noteId: String) : Result()
        data class Err(val message: String) : Result()
    }

    fun exportNote(store: InMemoryNoteStore, noteId: String): Result {
        val note = store.getNote(noteId) ?: return Result.Err("note missing")
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zip ->
            val manifest = buildString {
                appendLine("version=$MANIFEST_VERSION")
                appendLine("title=${escape(note.title)}")
                appendLine("modifiedAtMs=${note.modifiedAtMs}")
                appendLine("pages=${note.pageIds.joinToString(",")}")
            }
            zip.putNextEntry(ZipEntry(MANIFEST_NAME))
            zip.write(manifest.toByteArray(Charsets.UTF_8))
            zip.closeEntry()
            for (pageId in note.pageIds) {
                if (!isSafeId(pageId)) return Result.Err("unsafe page id")
                val page = store.page(pageId) ?: continue
                val pageTxt = buildString {
                    appendLine("pageId=${page.pageId}")
                    appendLine("paperType=${page.paperType.name}")
                    appendLine("ruleSpacingMm=${page.ruleSpacingMm}")
                }
                zip.putNextEntry(ZipEntry("pages/$pageId.txt"))
                zip.write(pageTxt.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
                store.strokesForPage(pageId).forEachIndexed { index, stroke ->
                    zip.putNextEntry(ZipEntry("pages/$pageId/strokes/$index.bin"))
                    zip.write(StrokeCodec.encode(stroke))
                    zip.closeEntry()
                }
            }
        }
        return Result.Ok(baos.toByteArray())
    }

    fun restoreAsNewNote(store: InMemoryNoteStore, archive: ByteArray): Result {
        return try {
            var title = "Restored note"
            var pageIds = listOf<String>()
            val pageSpecs = HashMap<String, PageSpec>()
            val pageStrokes = HashMap<String, MutableList<StrokeRecord>>()
            var version = -1
            ZipInputStream(ByteArrayInputStream(archive)).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    val name = entry.name
                    if (name.contains("..") || name.startsWith("/")) {
                        return Result.Err("unsafe path")
                    }
                    val data = zip.readBytes()
                    when {
                        name == MANIFEST_NAME -> {
                            val map = parseKv(String(data, Charsets.UTF_8))
                            version = map["version"]?.toIntOrNull() ?: -1
                            if (version != MANIFEST_VERSION) return Result.Err("unsupported schema $version")
                            title = unescape(map["title"] ?: title)
                            pageIds = map["pages"].orEmpty().split(',').filter { it.isNotBlank() }
                        }
                        name.matches(Regex("""pages/[^/]+\.txt""")) -> {
                            val map = parseKv(String(data, Charsets.UTF_8))
                            val id = map["pageId"] ?: return Result.Err("missing pageId")
                            if (!isSafeId(id)) return Result.Err("unsafe page id")
                            pageSpecs[id] = PageSpec(
                                pageId = id,
                                paperType = PaperType.valueOf(map["paperType"] ?: "RULED"),
                                ruleSpacingMm = map["ruleSpacingMm"]?.toFloatOrNull() ?: 8f
                            )
                        }
                        name.matches(Regex("""pages/[^/]+/strokes/\d+\.bin""")) -> {
                            val pageId = name.split("/")[1]
                            if (!isSafeId(pageId)) return Result.Err("unsafe page id")
                            pageStrokes.getOrPut(pageId) { mutableListOf() }.add(StrokeCodec.decode(data))
                        }
                    }
                    zip.closeEntry()
                }
            }
            if (version != MANIFEST_VERSION) return Result.Err("missing/invalid manifest")
            if (pageIds.isEmpty()) return Result.Err("no pages")
            val created = store.createNote(title)
            val auto = created.pageIds.first()
            for (oldId in pageIds) {
                val spec = pageSpecs[oldId] ?: return Result.Err("missing page $oldId")
                val newPage = store.addPage(created.noteId, spec.paperType, spec.ruleSpacingMm)
                    ?: return Result.Err("page create failed")
                val mapped = pageStrokes[oldId].orEmpty().map {
                    it.copy(strokeId = UUID.randomUUID().toString(), pageId = newPage.pageId)
                }
                store.replacePageStrokes(newPage.pageId, mapped)
            }
            store.deletePage(created.noteId, auto)
            Result.Restored(created.noteId)
        } catch (t: Throwable) {
            Result.Err(t.message ?: "restore failed")
        }
    }

    private fun parseKv(text: String): Map<String, String> =
        text.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() && it.contains('=') }
            .associate {
                val i = it.indexOf('=')
                it.substring(0, i) to it.substring(i + 1)
            }

    private fun escape(s: String): String = s.replace("\\", "\\\\").replace("\n", "\\n")
    private fun unescape(s: String): String = s.replace("\\n", "\n").replace("\\\\", "\\")

    private fun isSafeId(id: String): Boolean =
        id.isNotBlank() && !id.contains("..") && !id.contains('/') && !id.contains('\\')
}
