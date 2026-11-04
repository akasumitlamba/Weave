package com.weave.notebook.data

import com.weave.notebook.model.*
import java.io.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest
import java.util.UUID

/** v2 retains 64-bit event time, fitted geometry and original freehand samples. Reads v1 too. */
object StrokeCodec {
    const val SCHEMA_VERSION = 2
    private const val MAGIC = 0x57563253
    private const val MAX_SAMPLES = 100_000
    fun encode(stroke: StrokeRecord): ByteArray {
        val bytes = ByteArrayOutputStream()
        DataOutputStream(bytes).use { d ->
            d.writeInt(MAGIC); d.writeInt(SCHEMA_VERSION)
            d.writeUTF(stroke.strokeId); d.writeUTF(stroke.pageId)
            d.writeInt(stroke.brush.ordinal); d.writeInt(stroke.colorArgb); d.writeFloat(stroke.widthMm)
            d.writeInt(stroke.smoothingVersion)
            writeSamples(d, stroke.samples)
            d.writeBoolean(stroke.rawSamples != null)
            stroke.rawSamples?.let { writeSamples(d, it) }
            when(val s = stroke.fittedShape) {
                null -> d.writeInt(0)
                is FittedShape.Line -> { d.writeInt(1); listOf(s.x1,s.y1,s.x2,s.y2).forEach { d.writeFloat(it) } }
                is FittedShape.Circle -> { d.writeInt(2); listOf(s.cx,s.cy,s.radius).forEach { d.writeFloat(it) } }
                is FittedShape.Ellipse -> { d.writeInt(3); listOf(s.cx,s.cy,s.rx,s.ry,s.rotationRad).forEach { d.writeFloat(it) } }
                is FittedShape.Polygon -> { d.writeInt(4); d.writeInt(s.kind.ordinal); d.writeInt(s.points.size); s.points.forEach { d.writeFloat(it.first); d.writeFloat(it.second) } }
            }
        }
        return bytes.toByteArray()
    }
    private fun writeSamples(d: DataOutputStream, samples: List<StrokeSample>) {
        require(samples.size <= MAX_SAMPLES)
        d.writeInt(samples.size)
        samples.forEach { require(it.xMm.isFinite() && it.yMm.isFinite()); d.writeFloat(it.xMm); d.writeFloat(it.yMm); d.writeLong(it.tMs) }
    }
    private fun readSamples(d: DataInputStream): List<StrokeSample> {
        val count = d.readInt(); require(count in 0..MAX_SAMPLES && count.toLong()*16 <= d.available())
        return List(count) { StrokeSample(d.readFloat(),d.readFloat(),d.readLong()).also { require(it.xMm.isFinite() && it.yMm.isFinite()) } }
    }
    fun decode(bytes: ByteArray): StrokeRecord {
        require(bytes.size in 4..4_000_000)
        if (ByteBuffer.wrap(bytes).int != MAGIC) return decodeV1(bytes)
        val d = DataInputStream(ByteArrayInputStream(bytes))
        d.readInt(); require(d.readInt() == SCHEMA_VERSION)
        val id = d.readUTF().also { UUID.fromString(it) }; val page = d.readUTF().also { UUID.fromString(it) }
        val brush = BrushKind.entries[d.readInt()]; val color = d.readInt(); val width = d.readFloat()
        require(width.isFinite() && width > 0f && width <= 50f)
        val smoothing = d.readInt(); val samples = readSamples(d); val raw = if(d.readBoolean()) readSamples(d) else null
        val shape = when(d.readInt()) {
            0 -> null
            1 -> FittedShape.Line(d.readFloat(),d.readFloat(),d.readFloat(),d.readFloat())
            2 -> FittedShape.Circle(d.readFloat(),d.readFloat(),d.readFloat())
            3 -> FittedShape.Ellipse(d.readFloat(),d.readFloat(),d.readFloat(),d.readFloat(),d.readFloat())
            4 -> { val kind = PolygonKind.entries[d.readInt()]; val n = d.readInt(); require(n in 3..4); FittedShape.Polygon(List(n) { d.readFloat() to d.readFloat() },kind) }
            else -> error("Invalid fitted shape")
        }
        require(d.available() == 0)
        return StrokeRecord(id,page,samples,brush,color,width,smoothing,shape,raw)
    }
    private fun decodeV1(bytes: ByteArray): StrokeRecord {
        val b = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        require(b.int == 1)
        val id = UUID(b.long,b.long).toString(); val pid = UUID(b.long,b.long).toString()
        val brush = BrushKind.entries[b.int]; val color = b.int; val width = b.float; val smooth = b.int
        val n = b.int; require(n in 0..MAX_SAMPLES && b.remaining().toLong() == n.toLong()*12)
        val samples = List(n) { StrokeSample(b.float,b.float,b.int.toLong()) }
        require(width.isFinite() && width > 0 && samples.all { it.xMm.isFinite() && it.yMm.isFinite() })
        return StrokeRecord(id,pid,samples,brush,color,width,smooth)
    }
    fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
    fun contentHash(strokes: List<StrokeRecord>): String {
        val d = MessageDigest.getInstance("SHA-256")
        strokes.sortedBy { it.strokeId }.forEach { d.update(encode(it)) }
        return d.digest().joinToString("") { "%02x".format(it) }
    }
}
