package com.weave.notebook

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.view.MotionEvent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.weave.notebook.data.*
import com.weave.notebook.model.*
import com.weave.notebook.ink.InkCanvasView
import com.weave.notebook.pdf.PdfSceneExporter
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID
import java.io.File

@RunWith(AndroidJUnit4::class)
class TabletFlowTest {
    private val context get()=InstrumentationRegistry.getInstrumentation().targetContext
    @Test fun sqlite_reopenRestoresAcknowledgedInkOrderAndTemplate() {
        val p=UUID.randomUUID().toString()
        val strokes=List(3) {i->StrokeRecord(UUID.randomUUID().toString(),p,listOf(StrokeSample(20f+i,20f,4_000_000_000L)),BrushKind.PEN,Color.BLUE,0.45f)}
        val note=NotebookDocument(title="Recovery test",pages=listOf(NotebookPage(PageSpec(p,PaperType.RULED,6f),strokes)))
        val first=NotebookDatabase(context);first.save(note);first.close()
        val second=NotebookDatabase(context)
        try {assertEquals(note,second.load(note.id))} finally {second.delete(note.id);second.close()}
    }
    @Test fun sqlite_failedTransactionKeepsPreviousSavedNote() {
        val note=NotebookDocument(title="Failure test")
        val db=NotebookDatabase(context);db.save(note)
        val bad=StrokeRecord("invalid",note.pages[0].spec.pageId,listOf(StrokeSample(Float.NaN,0f,0)),BrushKind.PEN,Color.BLACK,0.45f)
        try {
            assertTrue(runCatching {db.save(note.copy(title="Should roll back",pages=listOf(note.pages[0].copy(strokes=listOf(bad)))))}.isFailure)
            assertEquals(note,db.load(note.id))
        } finally {db.delete(note.id);db.close()}
    }
    @Test fun pdf_sceneHasA4PageOrderAndInkWithNoEditorChrome() {
        val file=File(context.cacheDir,"tablet-test.pdf")
        val pages=listOf(Color.BLUE,Color.RED).map {color->
            val id=UUID.randomUUID().toString()
            PdfSceneExporter.PageSnapshot(PageSpec(id,PaperType.PLAIN),listOf(StrokeRecord(UUID.randomUUID().toString(),id,listOf(StrokeSample(100f,140f,0),StrokeSample(110f,150f,1)),BrushKind.MARKER,color,6f)))
        }
        file.outputStream().use {assertTrue(PdfSceneExporter().export(pages,it) is PdfSceneExporter.ExportResult.Ok)}
        PdfRenderer(ParcelFileDescriptor.open(file,ParcelFileDescriptor.MODE_READ_ONLY)).use {r->
            assertEquals(2,r.pageCount)
            for(i in 0..1) r.openPage(i).use {p->
                assertEquals(595,p.width);assertEquals(842,p.height)
                val bitmap=Bitmap.createBitmap(595,842,Bitmap.Config.ARGB_8888)
                p.render(bitmap,null,null,PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                val pixel=bitmap.getPixel(298,412)
                if(i==0)assertTrue(Color.blue(pixel)>Color.red(pixel)) else assertTrue(Color.red(pixel)>Color.blue(pixel))
                assertEquals(Color.WHITE,bitmap.getPixel(10,10));bitmap.recycle()
            }
        };file.delete()
    }
    @Test fun input_cancelRollsBackAndNavigationCreatesNoInk() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val v=InkCanvasView(context);v.layout(0,0,800,1000);var commits=0
            v.onStroke={_,_->commits++}
            fun event(action:Int,x:Float,y:Float) {val e=MotionEvent.obtain(0,0,action,x,y,0);v.onTouchEvent(e);e.recycle()}
            event(MotionEvent.ACTION_DOWN,400f,400f);event(MotionEvent.ACTION_MOVE,450f,450f);event(MotionEvent.ACTION_CANCEL,450f,450f)
            assertEquals(0,commits)
            v.inkEnabled=false;event(MotionEvent.ACTION_DOWN,400f,400f);event(MotionEvent.ACTION_UP,420f,420f);assertEquals(0,commits)
            v.inkEnabled=true;event(MotionEvent.ACTION_DOWN,400f,400f);event(MotionEvent.ACTION_UP,420f,420f);assertEquals(1,commits)
        }
    }
    @Test fun twentyPageNotebook_backupRestoreAndIncrementalSave() {
        val pages=List(20) {pageIndex->
            val id=UUID.randomUUID().toString()
            NotebookPage(PageSpec(id,if(pageIndex%2==0)PaperType.RULED else PaperType.PLAIN,8f),List(25) {n->
                StrokeRecord(UUID.randomUUID().toString(),id,List(100) {point->StrokeSample(20f+point,20f+n*8f+ kotlin.math.sin(point*0.1f),point.toLong())},BrushKind.PEN,Color.BLACK,0.45f)
            })
        }
        val note=NotebookDocument(title="Stress verification",pages=pages)
        val db=NotebookDatabase(context)
        var restored:NotebookDocument?=null
        try {
            db.save(note)
            val reopened=db.load(note.id)!!
            assertEquals(500,reopened.pages.sumOf {it.strokes.size})
            val archive=com.weave.notebook.backup.DocumentArchive.encode(reopened)
            restored=com.weave.notebook.backup.DocumentArchive.decode(archive.inputStream())
            db.save(restored)
            assertNotEquals(note.id,restored.id)
            assertEquals(50_000,db.load(restored.id)!!.pages.sumOf {p->p.strokes.sumOf {it.samples.size}})
            val changed=note.copy(title="Updated title")
            db.save(changed);assertEquals("Updated title",db.load(note.id)!!.title)
        } finally {db.delete(note.id);restored?.let {db.delete(it.id)};db.close()}
    }
    @Test fun pdf_cancellationDoesNotReportSuccess() {
        val p=PdfSceneExporter.PageSnapshot(PageSpec(UUID.randomUUID().toString()),emptyList())
        val output=java.io.ByteArrayOutputStream()
        val result=PdfSceneExporter().export(listOf(p,p),output,{true})
        assertTrue(result is PdfSceneExporter.ExportResult.Err)
    }

}
