package com.weave.notebook.ui

import android.graphics.*
import android.graphics.drawable.Drawable

/** Compact writing instruments with distinct nibs and barrels, rather than generic glyphs. */
class WritingToolDrawable(private val tool:String, private val size:Int):Drawable() {
    private val paint=Paint(Paint.ANTI_ALIAS_FLAG)
    override fun draw(canvas:Canvas) {
        canvas.save();canvas.translate(bounds.left.toFloat(),bounds.top.toFloat())
        canvas.scale(bounds.width()/32f,bounds.height()/42f)
        fun polygon(color:Int,vararg points:Float) {
            paint.color=color;paint.style=Paint.Style.FILL
            val path=Path();path.moveTo(points[0],points[1]);for(i in 2 until points.size step 2)path.lineTo(points[i],points[i+1]);path.close();canvas.drawPath(path,paint)
        }
        fun box(color:Int,l:Float,t:Float,r:Float,b:Float,corner:Float=2f) {paint.color=color;paint.style=Paint.Style.FILL;canvas.drawRoundRect(l,t,r,b,corner,corner,paint)}
        val charcoal=0xFF34434B.toInt();val silver=0xFFD3DCE0.toInt()
        when(tool) {
            "Pen"->{
                box(charcoal,10f,2f,22f,29f,3f);box(NativeUi.TEAL,10f,3f,22f,10f)
                box(silver,12f,13f,14f,25f,1f);box(silver,19f,4f,21f,17f,1f)
                polygon(0xFFE3C795.toInt(),10f,29f,22f,29f,16f,40f)
                polygon(charcoal,13.8f,36f,18.2f,36f,16f,40f)
            }
            "Marker"->{
                box(charcoal,7f,2f,25f,29f,4f);box(silver,8f,9f,24f,24f)
                box(0xFF738993.toInt(),10f,12f,12f,22f,1f);box(charcoal,9f,27f,23f,32f,1f)
                polygon(charcoal,11f,32f,21f,32f,21f,37f,11f,41f)
            }
            "Highlighter"->{
                box(0xFFECC83F.toInt(),5f,2f,27f,28f,5f)
                box(0xFFFFE88A.toInt(),8f,6f,11f,24f,1f);box(charcoal,5f,25f,27f,29f,1f)
                polygon(0xFFC99E17.toInt(),9f,29f,23f,29f,23f,35f,9f,40f)
                polygon(0xFFF6D32D.toInt(),9f,35f,23f,30f,23f,35f,9f,40f)
            }
            "Eraser"->{
                polygon(0xFFE6A7AC.toInt(),9f,3f,26f,10f,23f,21f,6f,14f)
                polygon(0xFFC3D3DC.toInt(),6f,14f,23f,21f,18f,39f,1f,32f)
                polygon(0xFF657D8B.toInt(),6f,19f,21.5f,25.5f,19f,34f,3.5f,27.5f)
                polygon(0xFFDFE7EB.toInt(),1f,32f,18f,39f,23f,21f,25f,20f,20f,41f,0f,33f)
            }
        }
        canvas.restore()
    }
    override fun getIntrinsicWidth()=size
    override fun getIntrinsicHeight()=(size*42f/32).toInt()
    override fun setAlpha(alpha:Int){paint.alpha=alpha}
    override fun setColorFilter(filter:ColorFilter?){paint.colorFilter=filter}
    @Deprecated("Deprecated in Android") override fun getOpacity()=PixelFormat.TRANSLUCENT
}
