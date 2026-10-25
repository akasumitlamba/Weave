package com.weave.notebook.data

import com.weave.notebook.model.BrushKind
import com.weave.notebook.model.StrokeRecord
import com.weave.notebook.model.StrokeSample
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest
import java.util.UUID

/**
 * Versioned compact stroke encoding for the page journal.
 * Schema version is stored alongside payloads for migrations.
 */
object StrokeCodec {
    const val SCHEMA_VERSION = 1

    fun encode(stroke: StrokeRecord): ByteArray {
        val samples = stroke.samples
        // version + 2 uuids + brush + color + width + smoothing + count + samples(x,y,t)
        val bytes = ByteBuffer.allocate(4 + 16 + 16 + 4 + 4 + 4 + 4 + 4 + samples.size * 12)
            .order(ByteOrder.LITTLE_ENDIAN)
        bytes.putInt(SCHEMA_VERSION)
        putUuid(bytes, UUID.fromString(stroke.strokeId))
        putUuid(bytes, UUID.fromString(stroke.pageId))
        bytes.putInt(stroke.brush.ordinal)
        bytes.putInt(stroke.colorArgb)
        bytes.putFloat(stroke.widthMm)
        bytes.putInt(stroke.smoothingVersion)
        bytes.putInt(samples.size)
        for (s in samples) {
            bytes.putFloat(s.xMm)
            bytes.putFloat(s.yMm)
            bytes.putInt(s.tMs.toInt())
        }
        return bytes.array()
    }

    fun decode(bytes: ByteArray): StrokeRecord {
        val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        val version = buf.int
        require(version == SCHEMA_VERSION) { "Unsupported stroke schema $version" }
        val strokeId = getUuid(buf).toString()
        val pageId = getUuid(buf).toString()
        val brush = BrushKind.entries[buf.int]
        val color = buf.int
        val width = buf.float
        val smoothing = buf.int
        val count = buf.int
        val samples = ArrayList<StrokeSample>(count)
        repeat(count) {
            samples += StrokeSample(buf.float, buf.float, buf.int.toLong())
        }
        return StrokeRecord(strokeId, pageId, samples, brush, color, width, smoothing)
    }

    fun sha256(bytes: ByteArray): String {
        val dig = MessageDigest.getInstance("SHA-256").digest(bytes)
        return dig.joinToString("") { "%02x".format(it) }
    }

    fun contentHash(strokes: List<StrokeRecord>): String {
        val md = MessageDigest.getInstance("SHA-256")
        for (s in strokes.sortedBy { it.strokeId }) {
            md.update(encode(s))
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }

    private fun putUuid(buf: ByteBuffer, id: UUID) {
        buf.putLong(id.mostSignificantBits)
        buf.putLong(id.leastSignificantBits)
    }

    private fun getUuid(buf: ByteBuffer): UUID = UUID(buf.long, buf.long)
}
