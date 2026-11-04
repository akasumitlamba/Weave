package com.weave.notebook.pdf

import android.graphics.pdf.PdfDocument
import com.weave.notebook.model.BrushKind
import com.weave.notebook.model.PageGeometry
import com.weave.notebook.model.PageSpec
import com.weave.notebook.model.PaperType
import com.weave.notebook.model.StrokeRecord
import com.weave.notebook.render.PageCanvasRenderer
import com.weave.notebook.geom.ViewportTransform
import java.io.ByteArrayOutputStream
import java.io.OutputStream

/**
 * Renders committed scene snapshots to portrait A4 PDF pages.
 * Uses Android PdfDocument integer points: 595 × 842 (~A4).
 */
class PdfSceneExporter(
    private val includeRuledLines: Boolean = true
) {
    data class PageSnapshot(
        val page: PageSpec,
        val strokes: List<StrokeRecord>
    )

    fun export(
        pages: List<PageSnapshot>,
        out: OutputStream,
        cancelled: () -> Boolean = { false },
        progress: (Int) -> Unit = {}
    ): ExportResult {
        if (pages.isEmpty()) return ExportResult.Err("no pages")
        val doc = PdfDocument()
        return try {
            val renderer = PageCanvasRenderer()
            pages.forEachIndexed { index, snap ->
                check(!cancelled()) { "Export cancelled" }
                val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH_PT, PAGE_HEIGHT_PT, index + 1).create()
                val pdfPage = doc.startPage(pageInfo)
                val canvas = pdfPage.canvas
                val viewport = ViewportTransform(snap.page.widthMm, snap.page.heightMm)
                viewport.setViewSize(PAGE_WIDTH_PT.toFloat(), PAGE_HEIGHT_PT.toFloat())
                viewport.fitPage()
                val pageForDraw = if (!includeRuledLines && snap.page.paperType == PaperType.RULED) {
                    snap.page.copy(paperType = PaperType.PLAIN)
                } else snap.page
                // Exclude chrome: only page + strokes.
                val ink = snap.strokes.filter { it.brush != BrushKind.ERASER }
                renderer.drawPage(canvas, viewport, pageForDraw, ink)
                doc.finishPage(pdfPage)
                progress(index + 1)
            }
            check(!cancelled()) { "Export cancelled" }
            doc.writeTo(out)
            doc.close()
            ExportResult.Ok(pageCount = pages.size, widthPt = PAGE_WIDTH_PT, heightPt = PAGE_HEIGHT_PT)
        } catch (t: Throwable) {
            runCatching { doc.close() }
            ExportResult.Err(t.message ?: "export failed")
        }
    }

    fun exportToBytes(pages: List<PageSnapshot>): ExportResult {
        val baos = ByteArrayOutputStream()
        return when (val r = export(pages, baos)) {
            is ExportResult.Ok -> r.copy(bytes = baos.toByteArray())
            is ExportResult.Err -> r
        }
    }

    sealed class ExportResult {
        data class Ok(
            val pageCount: Int,
            val widthPt: Int,
            val heightPt: Int,
            val bytes: ByteArray? = null
        ) : ExportResult()
        data class Err(val message: String) : ExportResult()
    }

    companion object {
        /** Android PdfDocument integer approximation of A4 (595.276 × 841.890). */
        const val PAGE_WIDTH_PT = 595
        const val PAGE_HEIGHT_PT = 842

        fun a4ErrorMm(): Pair<Float, Float> {
            val exactW = PageGeometry.WIDTH_MM * PageGeometry.MM_TO_PDF_POINTS
            val exactH = PageGeometry.HEIGHT_MM * PageGeometry.MM_TO_PDF_POINTS
            val errW = kotlin.math.abs(exactW - PAGE_WIDTH_PT) / PageGeometry.MM_TO_PDF_POINTS
            val errH = kotlin.math.abs(exactH - PAGE_HEIGHT_PT) / PageGeometry.MM_TO_PDF_POINTS
            return errW to errH
        }
    }
}
