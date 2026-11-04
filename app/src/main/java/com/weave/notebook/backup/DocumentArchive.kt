package com.weave.notebook.backup

import com.weave.notebook.data.*
import com.weave.notebook.model.*
import java.io.*
import java.util.UUID
import java.util.zip.*

/** Bounded, validated archive. Decode completes before the database is touched. */
object DocumentArchive {
    private const val LIMIT = 64 * 1024 * 1024
    fun encode(note: NotebookDocument): ByteArray {
        val bytes = ByteArrayOutputStream()
        ZipOutputStream(bytes).use { zip ->
            zip.putNextEntry(ZipEntry("notebook.bin"))
            val d = DataOutputStream(zip)
            d.writeInt(0x57564232); d.writeInt(1); d.writeUTF(note.title); d.writeInt(note.pages.size)
            note.pages.forEach { p ->
                d.writeInt(p.spec.paperType.ordinal); d.writeFloat(p.spec.ruleSpacingMm); d.writeInt(p.strokes.size)
                p.strokes.forEach { s -> val b = StrokeCodec.encode(s); d.writeInt(b.size); d.write(b) }
            }
            d.flush(); zip.closeEntry()
        }
        require(bytes.size() <= LIMIT) { "Notebook is too large for this backup" }
        return bytes.toByteArray()
    }
    fun decode(input: InputStream): NotebookDocument {
        val payload = ZipInputStream(input).use { zip ->
            val entry = zip.nextEntry ?: error("Empty backup")
            require(entry.name == "notebook.bin" && !entry.isDirectory) { "Unsupported backup" }
            val bytes = zip.readBytesBounded(LIMIT)
            require(zip.nextEntry == null) { "Unexpected backup entry" }
            bytes
        }
        val d = DataInputStream(ByteArrayInputStream(payload))
        require(d.readInt() == 0x57564232 && d.readInt() == 1) { "Unsupported backup version" }
        val title = d.readUTF(); val count = d.readInt(); require(count in 1..500)
        val pages = List(count) {
            val type = PaperType.entries[d.readInt()]; val spacing = d.readFloat()
            require(PageGeometry.isAllowedRuleSpacing(spacing))
            val id = UUID.randomUUID().toString(); val n = d.readInt(); require(n in 0..100_000)
            val strokes = List(n) {
                val size = d.readInt(); require(size in 4..4_000_000 && size <= d.available())
                val b = ByteArray(size); d.readFully(b)
                StrokeCodec.decode(b).copy(strokeId=UUID.randomUUID().toString(),pageId=id)
            }
            NotebookPage(PageSpec(id,type,spacing),strokes)
        }
        require(d.available() == 0)
        return NotebookDocument(title=title,pages=pages)
    }
    fun InputStream.readBytesBounded(limit: Int): ByteArray {
        val output = ByteArrayOutputStream(); val buffer = ByteArray(8192)
        while(true) {
            val n = read(buffer); if(n < 0) break
            require(output.size().toLong()+n <= limit) { "Backup exceeds size limit" }
            output.write(buffer,0,n)
        }
        return output.toByteArray()
    }
}
