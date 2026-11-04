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

    var sourceIndicator: android.graphics.RectF? = null
    var onSelectRegion: ((Float,Float)->Unit)? = null
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

    override fun onTouchEvent(event: android.view.MotionEvent): Boolean {
        val select=onSelectRegion ?: return super.onTouchEvent(event)
        if(event.actionMasked==android.view.MotionEvent.ACTION_DOWN || event.actionMasked==android.view.MotionEvent.ACTION_MOVE) {
            val p=viewport.screenToPage(event.x,event.y);select(p[0],p[1]);return true
        }
        return true
    }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(com.weave.notebook.ui.NativeUi.SURFACE)
        renderer.drawPage(canvas, viewport, page, strokes,minimumRulePx=resources.displayMetrics.density*0.5f)
        sourceIndicator?.let {r->
            val a=viewport.pageToScreen(r.left,r.top);val b=viewport.pageToScreen(r.right,r.bottom)
            val paint=android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {color=0xFF087F83.toInt();style=android.graphics.Paint.Style.STROKE;strokeWidth=2f}
            canvas.drawRect(a[0],a[1],b[0],b[1],paint)
        }
    }
}
