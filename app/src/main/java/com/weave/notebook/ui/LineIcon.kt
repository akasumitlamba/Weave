package com.weave.notebook.ui

import android.graphics.*
import android.graphics.drawable.Drawable

/** Native vector line icons, drawn in a 24-unit grid to match the handoff reference. */
class LineIcon(private val name:String,private val tint:Int=NativeUi.TEXT,private val size:Int=24):Drawable() {
    private val p=Paint(Paint.ANTI_ALIAS_FLAG).apply {color=tint;style=Paint.Style.STROKE;strokeWidth=1.7f;strokeCap=Paint.Cap.ROUND;strokeJoin=Paint.Join.ROUND}
    override fun draw(c:Canvas) {
        c.save();c.translate(bounds.left.toFloat(),bounds.top.toFloat());c.scale(bounds.width()/24f,bounds.height()/24f)
        fun path(vararg points:Float) {val q=Path();q.moveTo(points[0],points[1]);for(i in 2 until points.size step 2)q.lineTo(points[i],points[i+1]);c.drawPath(q,p)}
        when(name) {
            "Pen"->{path(4f,20f,5.5f,14f,16f,3.5f,20.5f,8f,10f,18.5f,4f,20f);path(14.5f,5f,19f,9.5f);path(5.5f,14f,10f,18.5f)}
            "Marker"->{path(3f,21f,7f,16f,5f,14f,15f,4f,20f,9f,10f,19f,8f,17f);path(13f,6f,18f,11f)}
            "Highlighter"->{path(4f,20f,8f,16f,6f,14f,15f,5f,20f,10f,11f,19f,9f,17f);path(3f,21f,10f,21f);path(13f,7f,18f,12f)}
            "Eraser"->{path(3f,14f,13f,4f,21f,12f,13f,20f,9f,20f,3f,14f);path(8f,9f,16f,17f);path(12f,20f,21f,20f)}
            "Hand","Navigate"->{path(8f,13f,8f,6f,10f,5f,11f,6f,11f,12f,11f,3f,13f,2.5f,14f,4f,14f,12f,14f,5f,16f,4.5f,17f,6f,17f,13f,17f,8f,19f,7.5f,20f,9f,20f,16f,17f,21f,10f,21f,4f,15f,4f,13f,6f,12f,8f,14f)}
            "Undo"->{path(8f,4f,3f,9f,8f,14f);path(3f,9f,14f,9f);c.drawArc(10f,9f,22f,21f,-90f,230f,false,p)}
            "Redo"->{c.save();c.scale(-1f,1f,12f,12f);path(8f,4f,3f,9f,8f,14f);path(3f,9f,14f,9f);c.drawArc(10f,9f,22f,21f,-90f,230f,false,p);c.restore()}
            "Pages","Paper"->{path(6f,3f,15f,3f,20f,8f,20f,21f,6f,21f,6f,3f);path(14f,3f,14f,9f,20f,9f)}
            "Export PDF","Backup notebook"->{path(4f,13f,4f,21f,20f,21f,20f,13f);path(12f,16f,12f,3f,7f,8f);path(12f,3f,17f,8f)}
            "Fullscreen"->{path(8f,3f,3f,3f,3f,8f);path(3f,3f,9f,9f);path(16f,3f,21f,3f,21f,8f);path(21f,3f,15f,9f);path(3f,16f,3f,21f,8f,21f);path(3f,21f,9f,15f);path(16f,21f,21f,21f,21f,16f);path(21f,21f,15f,15f)}
            "Exit fullscreen"->{path(3f,8f,8f,8f,8f,3f);path(21f,8f,16f,8f,16f,3f);path(3f,16f,8f,16f,8f,21f);path(21f,16f,16f,16f,16f,21f)}
            "Fit","Fit width"->{path(8f,3f,3f,3f,3f,8f);path(16f,3f,21f,3f,21f,8f);path(3f,16f,3f,21f,8f,21f);path(16f,21f,21f,21f,21f,16f)}
            "Back","‹"->path(15f,5f,8f,12f,15f,19f)
            "›"->path(9f,5f,16f,12f,9f,19f)
            "Zoom writing"->{c.drawCircle(10f,10f,6f,p);path(15f,15f,21f,21f);path(7f,10f,13f,10f);path(10f,7f,10f,13f)}
            "Palm guard"->{c.drawRoundRect(4f,5f,20f,21f,4f,4f,p);path(8f,9f,16f,9f);path(8f,12f,16f,12f)}
            "Settings","⋮"->{p.style=Paint.Style.FILL;for(y in listOf(5f,12f,19f))c.drawCircle(12f,y,1.5f,p);p.style=Paint.Style.STROKE}
            "New note","Add page","＋","＋ Page"->{path(5f,12f,19f,12f);path(12f,5f,12f,19f)}
            "−"->{c.drawCircle(12f,12f,9f,p);path(8f,12f,16f,12f)}
            "Write"->{path(5f,20f,6f,14f,16f,4f,20f,8f,10f,18f,5f,20f)}
            else->c.drawCircle(12f,12f,6f,p)
        }
        c.restore()
    }
    override fun getIntrinsicWidth()=size
    override fun getIntrinsicHeight()=size
    override fun setAlpha(alpha:Int){p.alpha=alpha}
    override fun setColorFilter(filter:ColorFilter?){p.colorFilter=filter}
    @Deprecated("Deprecated in Android") override fun getOpacity()=PixelFormat.TRANSLUCENT
}
