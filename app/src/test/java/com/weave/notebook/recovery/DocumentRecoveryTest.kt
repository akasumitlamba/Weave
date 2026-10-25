package com.weave.notebook.recovery

import com.weave.notebook.backup.DocumentArchive
import com.weave.notebook.data.*
import com.weave.notebook.model.*
import com.weave.notebook.shape.*
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.util.UUID

class DocumentRecoveryTest {
    private fun stroke(pid:String)=StrokeRecord(UUID.randomUUID().toString(),pid,
        listOf(StrokeSample(12f,14f,4_000_000_000L),StrokeSample(40f,14f,4_000_000_100L)),BrushKind.PEN,0xFF123456.toInt(),0.45f)
    @Test fun codec_preserves64bitTimeRawAndFittedGeometry() {
        val raw=stroke(UUID.randomUUID().toString())
        val fit=ShapeCommit.replaceWithShape(raw,ShapeCandidate(FittedShape.Line(12f,14f,40f,14f),1f,"line")).first
        assertEquals(fit,StrokeCodec.decode(StrokeCodec.encode(fit)))
        assertEquals(raw.samples,StrokeCodec.decode(StrokeCodec.encode(fit)).rawSamples)
    }
    @Test fun malformedCountAndTruncationRejected() {
        val raw=stroke(UUID.randomUUID().toString())
        val encoded=StrokeCodec.encode(raw)
        for(n in listOf(0,3,10,encoded.size-1)) {
            assertTrue(runCatching {StrokeCodec.decode(encoded.copyOf(n))}.isFailure)
        }
    }
    @Test fun archive_preservesOrderTemplateAndGeometryUnderNewIds() {
        val id=UUID.randomUUID().toString();val second=UUID.randomUUID().toString()
        val a=NotebookPage(PageSpec(id,PaperType.RULED,6f),listOf(stroke(id)))
        val b=NotebookPage(PageSpec(second,PaperType.PLAIN),listOf(stroke(second)))
        val note=NotebookDocument(title="Physics \\n =",pages=listOf(a,b))
        val restored=DocumentArchive.decode(ByteArrayInputStream(DocumentArchive.encode(note)))
        assertNotEquals(note.id,restored.id);assertEquals(note.title,restored.title)
        assertEquals(note.pages.map {it.spec.paperType},restored.pages.map {it.spec.paperType})
        assertEquals(6f,restored.pages[0].spec.ruleSpacingMm)
        assertEquals(a.strokes[0].samples,restored.pages[0].strokes[0].samples)
        assertNotEquals(id,restored.pages[0].spec.pageId)
        assertEquals(restored.pages[0].spec.pageId,restored.pages[0].strokes[0].pageId)
    }
    @Test fun history_restoresErasedStrokeAtItsOriginalLayerPosition() {
        val id=UUID.randomUUID().toString();val low=stroke(id);val high=stroke(id)
        val original=NotebookDocument(title="X",pages=listOf(NotebookPage(PageSpec(id),listOf(low,high))))
        val erased=original.copy(pages=listOf(original.pages[0].copy(strokes=listOf(high))))
        val h=NotebookHistory();h.record(original)
        assertEquals(listOf(low,high),h.undo(erased)!!.pages[0].strokes)
        assertEquals(listOf(high),h.redo(original)!!.pages[0].strokes)
        h.record(original);assertFalse(h.canRedo)
    }
    @Test fun pageOperationsUndoTogetherWithPaperChanges() {
        val a=NotebookDocument(title="X");val h=NotebookHistory()
        val b=a.copy(pages=a.pages+NotebookPage(PageSpec(UUID.randomUUID().toString(),PaperType.PLAIN)))
        h.record(a);assertEquals(1,h.undo(b)!!.pages.size)
        assertEquals(2,h.redo(a)!!.pages.size)
    }
    @Test fun eraser_interpolatesSparseGestureAndKeepsMissedStrokes() {
        val pid=UUID.randomUUID().toString()
        val a=stroke(pid).copy(samples=listOf(StrokeSample(50f,30f,0),StrokeSample(50f,60f,1)))
        val b=stroke(pid).copy(samples=listOf(StrokeSample(150f,30f,0),StrokeSample(150f,60f,1)))
        val erased=com.weave.notebook.command.EraserPath.hitIds(listOf(a,b),listOf(StrokeSample(20f,40f,0),StrokeSample(80f,40f,10)),1f)
        assertEquals(setOf(a.strokeId),erased)
    }
}
