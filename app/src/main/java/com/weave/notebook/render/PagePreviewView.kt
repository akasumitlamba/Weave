package com.weave.notebook.render

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.weave.notebook.R
import com.weave.notebook.geom.ViewportTransform
import com.weave.notebook.model.PageSpec
import com.weave.notebook.model.PaperType
import com.weave.notebook.model.StrokeRecord
import java.util.UUID

/**
 * Basic Canvas page renderer host for A4 plain/ruled templates.
 */
class PagePreviewView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val renderer = PageCanvasRenderer()
    val viewport = ViewportTransform()

    var page: PageSpec = PageSpec(
        pageId = UUID.randomUUID().toString(),
        paperType = PaperType.RULED
    )
        set(value) {
            field = value
            invalidate()
        }

    var strokes: List<StrokeRecord> = emptyList()
        set(value) {
            field = value
            invalidate()
        }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 0 && h > 0) {
            viewport.setViewSize(w.toFloat(), h.toFloat())
            viewport.fitPage()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(ContextCompat.getColor(context, R.color.workspace_bg))
        renderer.drawPage(canvas, viewport, page, strokes)
    }
}
