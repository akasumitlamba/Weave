package com.weave.notebook.ui

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.widget.AppCompatButton

object NativeUi {
    const val TEAL=0xFF087F83.toInt()
    const val TEXT=0xFF202A30.toInt()
    const val MUTED=0xFF68757C.toInt()
    const val BG=0xFFF3F4F3.toInt()
    const val LINE=0xFFD8DFDF.toInt()
    fun Context.dp(v:Int)=(v*resources.displayMetrics.density+0.5f).toInt()
    fun rounded(color:Int, radius:Float=10f, border:Int=Color.TRANSPARENT)=GradientDrawable().apply {
        setColor(color); cornerRadius=radius; setStroke(1,border)
    }
    fun text(c:Context,s:String,size:Float=16f,bold:Boolean=false)=TextView(c).apply {
        text=s;textSize=size;setTextColor(TEXT);if(bold) setTypeface(typeface,Typeface.BOLD)
    }
    fun row(c:Context)=LinearLayout(c).apply { orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL }
    fun column(c:Context)=LinearLayout(c).apply { orientation=LinearLayout.VERTICAL }
    fun button(c:Context,s:String,selected:Boolean=false,click:()->Unit):AppCompatButton = AppCompatButton(c).apply {
        stateListAnimator=null;elevation=0f;text=s;isAllCaps=false;textSize=14f;contentDescription=s;minWidth=c.dp(48);minimumWidth=c.dp(48);minimumHeight=c.dp(48)
        setPadding(c.dp(12),0,c.dp(12),0)
        background=rounded(if(selected) TEAL else Color.WHITE,c.dp(9).toFloat(),if(selected) TEAL else LINE)
        backgroundTintList=null;setTextColor(if(selected) Color.WHITE else TEXT)
        layoutParams=LinearLayout.LayoutParams(-2,c.dp(48)).apply { setMargins(c.dp(3),c.dp(3),c.dp(3),c.dp(3)) }
        val iconName=when(s) { "‹ Back"->"Back";"↶ Undo"->"Undo";"↷ Redo"->"Redo";"＋ New note"->"New note";else->s }
        if(iconName in setOf("Back","Undo","Redo","Pages","Paper","Export PDF","Fit","Fit width","Zoom writing","Palm guard","Settings","New note","＋ Page","‹","›","−","＋","Write","Navigate","⋮")) {
            val icon=LineIcon(iconName,if(selected) Color.WHITE else TEXT,c.dp(21)).apply {setBounds(0,0,c.dp(21),c.dp(21))}
            setCompoundDrawables(icon,null,null,null);compoundDrawablePadding=c.dp(7)
            text=when(s) {"‹ Back"->"";"↶ Undo"->"Undo";"↷ Redo"->"Redo";"＋ New note"->"New note";"‹","›","−","＋","⋮"->"";else->s}
            tag=iconName
        }
        if(s in setOf("↶ Undo","↷ Redo","Pages","Export PDF","Fit")) background=android.graphics.drawable.ColorDrawable(Color.TRANSPARENT)
        if(s in setOf("‹ Back","⋮","‹","›","−","＋","←","→","↑")) {
            minimumWidth=c.dp(48);setPadding(c.dp(12),0,c.dp(12),0)
            layoutParams=LinearLayout.LayoutParams(c.dp(48),c.dp(48))
            compoundDrawablePadding=0;background=android.graphics.drawable.ColorDrawable(Color.TRANSPARENT)
        }
        setOnClickListener { click() }
    }
    fun select(view:TextView,selected:Boolean) {
        view.background=rounded(if(selected) TEAL else Color.WHITE,view.context.dp(9).toFloat(),if(selected) TEAL else LINE)
        view.setTextColor(if(selected) Color.WHITE else TEXT);view.isSelected=selected
        (view.tag as? String)?.let {name->
            val c=view.context;val icon=LineIcon(name,if(selected)Color.WHITE else TEXT,c.dp(21)).apply {setBounds(0,0,c.dp(21),c.dp(21))}
            view.setCompoundDrawables(icon,null,null,null)
        }
    }
    fun tool(c:Context,label:String,selected:Boolean,click:()->Unit)=button(c,label,selected,click).apply {
        text=label;textSize=if(c.resources.configuration.screenWidthDp<700)9f else 10f;maxLines=1;setPadding(c.dp(1),c.dp(5),c.dp(1),c.dp(4))
        val icon=LineIcon(label,if(selected)TEAL else TEXT,c.dp(25)).apply {setBounds(0,0,c.dp(25),c.dp(25))}
        setCompoundDrawables(null,icon,null,null);compoundDrawablePadding=c.dp(3)
        background=rounded(if(selected)0xFFD6ECEC.toInt() else Color.TRANSPARENT,c.dp(8).toFloat(),if(selected)TEAL else Color.TRANSPARENT)
        setTextColor(TEXT)
        layoutParams=LinearLayout.LayoutParams(c.dp(if(c.resources.configuration.screenWidthDp<700)48 else 60),c.dp(62))
    }
    fun dot(c:Context,label:String,ink:Int,radius:Int,selected:Boolean,click:()->Unit)=button(c,"",false,click).apply {
        contentDescription=label;tooltipText=label
        background=object:android.graphics.drawable.Drawable() {
            val p=android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
            override fun draw(canvas:android.graphics.Canvas) {
                val x=bounds.exactCenterX();val y=bounds.exactCenterY()
                p.style=android.graphics.Paint.Style.FILL;p.color=ink or 0xFF000000.toInt();canvas.drawCircle(x,y,c.dp(radius).toFloat(),p)
                if(selected) {p.style=android.graphics.Paint.Style.STROKE;p.strokeWidth=c.dp(1).toFloat();p.color=TEAL;canvas.drawCircle(x,y,c.dp(radius+4).toFloat(),p)}
            }
            override fun setAlpha(alpha:Int){}
            override fun setColorFilter(filter:android.graphics.ColorFilter?){}
            @Deprecated("Deprecated in Android") override fun getOpacity()=android.graphics.PixelFormat.TRANSLUCENT
        }
        layoutParams=LinearLayout.LayoutParams(c.dp(48),c.dp(48))
    }
    fun scrollRow(c:Context,row:LinearLayout)=HorizontalScrollView(c).apply {
        isHorizontalScrollBarEnabled=false;addView(row);isFillViewport=false
    }
    fun spacer(c:Context)=View(c).apply { layoutParams=LinearLayout.LayoutParams(0,1,1f) }
    fun input(c:Context,hint:String,value:String="")=EditText(c).apply {
        this.hint=hint;setText(value);setSingleLine();setTextColor(TEXT);textSize=16f
        setPadding(c.dp(12),c.dp(8),c.dp(12),c.dp(8));background=rounded(Color.WHITE,c.dp(8).toFloat(),LINE)
        layoutParams=LinearLayout.LayoutParams(-1,c.dp(56)).apply { bottomMargin=c.dp(16) }
        contentDescription=hint
    }
}
