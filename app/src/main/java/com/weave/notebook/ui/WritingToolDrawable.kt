package com.weave.notebook.ui

import android.graphics.*
import android.graphics.drawable.Drawable

/** Resolution-independent instrument silhouettes with a common scale and themed barrels. */
class WritingToolDrawable(private val tool:String, private val size:Int):Drawable() {
    private val paint=Paint(Paint.ANTI_ALIAS_FLAG)
    override fun draw(canvas:Canvas) {
        canvas.save();canvas.translate(bounds.left.toFloat(),bounds.top.toFloat())
        canvas.scale(bounds.width()/32f,bounds.height()/42f)
        fun shape(color:Int,vararg points:Float) {
            paint.color=color;paint.style=Paint.Style.FILL
            val path=Path();path.moveTo(points[0],points[1]);for(i in 2 until points.size step 2)path.lineTo(points[i],points[i+1]);path.close();canvas.drawPath(path,paint)
        }
        fun box(color:Int,l:Float,t:Float,r:Float,b:Float,corner:Float=2f) {
            paint.color=color;paint.style=Paint.Style.FILL;canvas.drawRoundRect(l,t,r,b,corner,corner,paint)
        }
        val body=NativeUi.TEXT;val accent=NativeUi.TEAL;val inset=NativeUi.SURFACE
        when(tool) {
            "Pen"->{
                box(body,11f,2f,21f,27f,3f);box(accent,11f,4f,21f,8f,1f)
                box(inset,13f,10f,14.4f,24f,0.7f)
                shape(body,11f,28f,21f,28f,17f,39f,15f,39f)
                box(inset,15.4f,28f,16.6f,35f,0.5f)
                box(body,20f,5f,23f,18f,1.2f)
            }
            "Marker"->{
                box(body,8f,2f,24f,25f,3f);box(accent,8f,6f,24f,10f,0f)
                box(inset,11f,12f,12.5f,22f,0.7f)
                box(body,10f,26f,22f,31f,1f)
                shape(body,12f,32f,20f,32f,20f,37f,12f,40f)
            }
            "Highlighter"->{
                box(body,5f,2f,27f,26f,5f);box(accent,5f,7f,27f,15f,0f)
                box(inset,8f,17f,10f,23f,1f);box(body,8f,26f,24f,30f,1f)
                shape(accent,9f,31f,23f,31f,23f,36f,9f,40f)
            }
            "Eraser"->{
                canvas.rotate(-25f,16f,21f)
                box(body,7f,5f,25f,37f,4f);box(accent,7f,5f,25f,18f,4f)
                box(accent,7f,13f,25f,18f,0f);box(inset,10f,21f,11.5f,32f,0.7f)
                box(inset,8f,34f,24f,35.5f,0.7f)
            }
        };canvas.restore()
    }
    override fun getIntrinsicWidth()=size
    override fun getIntrinsicHeight()=(size*42f/32).toInt()
    override fun setAlpha(alpha:Int){paint.alpha=alpha}
    override fun setColorFilter(filter:ColorFilter?){paint.colorFilter=filter}
    @Deprecated("Deprecated in Android") override fun getOpacity()=PixelFormat.TRANSLUCENT
}
