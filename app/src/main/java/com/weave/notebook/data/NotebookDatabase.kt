package com.weave.notebook.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.Handler
import android.os.Looper
import java.util.concurrent.Executors

/** One SQLite transaction acknowledges metadata, ordered pages and changed stroke blobs together. */
class NotebookDatabase(context: Context) : SQLiteOpenHelper(context, "weave.db", null, 1) {
    private val committed = HashMap<String, NotebookDocument>()
    init { setWriteAheadLoggingEnabled(true) }
    override fun onConfigure(db: SQLiteDatabase) { db.setForeignKeyConstraintsEnabled(true) }
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE notes(id TEXT PRIMARY KEY,title TEXT NOT NULL,modified INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE pages(id TEXT PRIMARY KEY,note TEXT NOT NULL REFERENCES notes(id) ON DELETE CASCADE,position INTEGER NOT NULL,paper TEXT NOT NULL,spacing REAL NOT NULL)")
        db.execSQL("CREATE INDEX page_order ON pages(note,position)")
        db.execSQL("CREATE TABLE strokes(id TEXT PRIMARY KEY,page TEXT NOT NULL REFERENCES pages(id) ON DELETE CASCADE,position INTEGER NOT NULL,payload BLOB NOT NULL)")
        db.execSQL("CREATE INDEX stroke_order ON strokes(page,position)")
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) { error("Unsupported database migration") }

    fun list(): List<NotebookDocument> = readableDatabase.rawQuery("SELECT id FROM notes ORDER BY modified DESC", null).use { c ->
        buildList { while (c.moveToNext()) load(c.getString(0))?.let { add(it) } }
    }
    fun load(id: String): NotebookDocument? {
        val db = readableDatabase
        val meta = db.rawQuery("SELECT title,modified FROM notes WHERE id=?", arrayOf(id)).use { c ->
            if (!c.moveToFirst()) return null
            c.getString(0) to c.getLong(1)
        }
        val pages = db.rawQuery("SELECT id,paper,spacing FROM pages WHERE note=? ORDER BY position", arrayOf(id)).use { c ->
            buildList { while (c.moveToNext()) {
                val pid = c.getString(0)
                val strokes = db.rawQuery("SELECT payload FROM strokes WHERE page=? ORDER BY position", arrayOf(pid)).use { s ->
                    buildList { while (s.moveToNext()) add(StrokeCodec.decode(s.getBlob(0))) }
                }
                add(NotebookPage(com.weave.notebook.model.PageSpec(pid,
                    com.weave.notebook.model.PaperType.valueOf(c.getString(1)), c.getFloat(2)), strokes))
            } }
        }
        check(pages.isNotEmpty()) { "Notebook has no pages" }
        return NotebookDocument(id, meta.first, meta.second, pages).also { committed[id]=it }
    }
    fun save(document: NotebookDocument) {
        require(document.pages.isNotEmpty())
        require(document.pages.map { it.spec.pageId }.distinct().size == document.pages.size)
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.insertWithOnConflict("notes", null, ContentValues().apply {
                put("id", document.id); put("title", document.title); put("modified", document.modified)
            }, SQLiteDatabase.CONFLICT_IGNORE)
            db.update("notes", ContentValues().apply { put("title", document.title); put("modified", document.modified) }, "id=?", arrayOf(document.id))
            val old = db.rawQuery("SELECT id FROM pages WHERE note=?", arrayOf(document.id)).use { c ->
                buildList { while(c.moveToNext()) add(c.getString(0)) }
            }
            old.filter { pid -> document.pages.none { it.spec.pageId == pid } }.forEach { db.delete("pages", "id=?", arrayOf(it)) }
            document.pages.forEachIndexed { index, page ->
                val pid = page.spec.pageId
                val values = ContentValues().apply {
                    put("id", pid); put("note", document.id); put("position", index)
                    put("paper", page.spec.paperType.name); put("spacing", page.spec.ruleSpacingMm)
                }
                db.insertWithOnConflict("pages", null, values, SQLiteDatabase.CONFLICT_IGNORE)
                db.update("pages", values, "id=?", arrayOf(pid))
                // Only changed pages write stroke data. Never write on MOVE.
                val previousPage=committed[document.id]?.pages?.firstOrNull {it.spec.pageId==pid}
                if(previousPage?.strokes === page.strokes) return@forEachIndexed
                val previous = db.rawQuery("SELECT payload FROM strokes WHERE page=? ORDER BY position", arrayOf(pid)).use { c ->
                    buildList { while(c.moveToNext()) add(c.getBlob(0)) }
                }
                val encoded = page.strokes.map { s -> require(s.pageId == pid); StrokeCodec.encode(s) }
                if (previous.size != encoded.size || previous.indices.any { !previous[it].contentEquals(encoded[it]) }) {
                    db.delete("strokes", "page=?", arrayOf(pid))
                    page.strokes.forEachIndexed { pos, s ->
                        db.insertOrThrow("strokes", null, ContentValues().apply {
                            put("id", s.strokeId); put("page", pid); put("position", pos); put("payload", encoded[pos])
                        })
                    }
                }
            }
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
        committed[document.id]=document
    }
    fun delete(id: String) { writableDatabase.delete("notes", "id=?", arrayOf(id));committed.remove(id) }
}

/** Serialized IO; callbacks always run on the main thread after commit or failure. */
object NotebookIO {
    private val worker = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())
    lateinit var database: NotebookDatabase
    fun <T> submit(work: () -> T, done: (Result<T>) -> Unit) {
        worker.execute { val result = runCatching(work); main.post { done(result) } }
    }
}
