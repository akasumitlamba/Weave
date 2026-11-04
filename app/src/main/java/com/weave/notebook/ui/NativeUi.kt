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
    var TEAL=0xFF087F83.toInt();private set
    var ON_TEAL=Color.WHITE;private set
    var TEXT=0xFF202A30.toInt();private set
    var MUTED=0xFF68757C.toInt();private set
    var BG=0xFFF3F4F3.toInt();private set
    var SURFACE=Color.WHITE;private set
    var LINE=0xFFD8DFDF.toInt();private set
    var SELECTED=0xFFD6ECEC.toInt();private set
    var ERROR=0xFFB3261E.toInt();private set
    fun applyTheme(activity:android.app.Activity) {
        fun color(attr:Int)=com.google.android.material.color.MaterialColors.getColor(activity,attr,TEXT)
        TEAL=color(com.google.android.material.R.attr.colorPrimary)
        ON_TEAL=color(com.google.android.material.R.attr.colorOnPrimary)
        TEXT=color(com.google.android.material.R.attr.colorOnSurface)
        MUTED=color(com.google.android.material.R.attr.colorOnSurfaceVariant)
        BG=color(com.google.android.material.R.attr.colorSurface)
        SURFACE=color(com.google.android.material.R.attr.colorSurfaceContainer)
        LINE=color(com.google.android.material.R.attr.colorOutlineVariant)
        SELECTED=color(com.google.android.material.R.attr.colorPrimaryContainer)
        ERROR=color(com.google.android.material.R.attr.colorError)
        activity.window.statusBarColor=BG;activity.window.navigationBarColor=BG
        androidx.core.view.WindowInsetsControllerCompat(activity.window,activity.window.decorView).apply {
            val light=com.google.android.material.color.MaterialColors.isColorLight(BG)
            isAppearanceLightStatusBars=light;isAppearanceLightNavigationBars=light
        }
    }
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
        background=rounded(if(selected) TEAL else SURFACE,c.dp(9).toFloat(),if(selected) TEAL else LINE)
        backgroundTintList=null;setTextColor(if(selected) ON_TEAL else TEXT)
        layoutParams=LinearLayout.LayoutParams(-2,c.dp(48)).apply { setMargins(c.dp(3),c.dp(3),c.dp(3),c.dp(3)) }
        val iconName=when(s) { "‹ Back"->"Back";"↶ Undo"->"Undo";"↷ Redo"->"Redo";"＋ New note"->"New note";else->s }
        if(iconName in setOf("Back","Undo","Redo","Pages","Paper","Export PDF","Fit","Fit width","Zoom writing","Palm guard","Settings","New note","＋ Page","‹","›","−","＋","Write","Navigate","Fullscreen","Exit fullscreen","⋮")) {
            val icon=LineIcon(iconName,if(selected) ON_TEAL else TEXT,c.dp(21)).apply {setBounds(0,0,c.dp(21),c.dp(21))}
            setCompoundDrawables(icon,null,null,null);compoundDrawablePadding=c.dp(7)
            text=when(s) {"‹ Back"->"";"↶ Undo"->"Undo";"↷ Redo"->"Redo";"＋ New note"->"New note";"‹","›","−","＋","⋮","Exit fullscreen"->"";else->s}
            tag=iconName
        }
        if(s in setOf("↶ Undo","↷ Redo","Pages","Export PDF","Fit")) background=android.graphics.drawable.ColorDrawable(Color.TRANSPARENT)
        if(s in setOf("‹ Back","⋮","‹","›","−","＋","←","→","↑","Exit fullscreen")) {
            minimumWidth=c.dp(48);setPadding(c.dp(12),0,c.dp(12),0)
            layoutParams=LinearLayout.LayoutParams(c.dp(48),c.dp(48))
            compoundDrawablePadding=0;background=android.graphics.drawable.ColorDrawable(Color.TRANSPARENT)
        }
        setOnClickListener { click() }
    }
    fun select(view:TextView,selected:Boolean) {
        view.background=rounded(if(selected) TEAL else SURFACE,view.context.dp(9).toFloat(),if(selected) TEAL else LINE)
        view.setTextColor(if(selected) ON_TEAL else TEXT);view.isSelected=selected
        (view.tag as? String)?.let {name->
            val c=view.context;val icon=LineIcon(name,if(selected)ON_TEAL else TEXT,c.dp(21)).apply {setBounds(0,0,c.dp(21),c.dp(21))}
            view.setCompoundDrawables(icon,null,null,null)
        }
    }
    fun tool(c:Context,label:String,selected:Boolean,click:()->Unit)=button(c,label,selected,click).apply {
        text=label;textSize=if(c.resources.configuration.screenWidthDp<700)9f else 10f;maxLines=1;setPadding(c.dp(1),c.dp(5),c.dp(1),c.dp(4))
        val icon=if(label in setOf("Pen","Marker","Highlighter","Eraser")) WritingToolDrawable(label,c.dp(26)).apply {setBounds(0,0,c.dp(26),c.dp(34))}
            else LineIcon(label,TEXT,c.dp(25)).apply {setBounds(0,0,c.dp(25),c.dp(25))}
        setCompoundDrawables(null,icon,null,null);compoundDrawablePadding=c.dp(3)
        background=rounded(if(selected)SELECTED else Color.TRANSPARENT,c.dp(8).toFloat(),if(selected)TEAL else Color.TRANSPARENT)
        setTextColor(TEXT)
        layoutParams=LinearLayout.LayoutParams(c.dp(if(c.resources.configuration.screenWidthDp<700)48 else 60),c.dp(64))
    }
    fun width(c:Context,value:Float,selected:Boolean,isDefault:Boolean,maximum:Float,click:()->Unit)=button(c,"",false,click).apply {
        val number=if(value==value.toInt().toFloat())value.toInt().toString() else value.toString()
        contentDescription="Width $number mm${if(isDefault) ", default" else ""}";tooltipText=contentDescription
        isSelected=selected
        background=object:android.graphics.drawable.Drawable() {
            val p=android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
            override fun draw(canvas:android.graphics.Canvas) {
                val d=c.resources.displayMetrics.density;val cx=bounds.exactCenterX();val top=bounds.top.toFloat()
                p.style=android.graphics.Paint.Style.FILL;p.color=if(selected)SELECTED else SURFACE
                canvas.drawRoundRect(bounds.left+2*d,top+2*d,bounds.right-2*d,bounds.bottom-2*d,7*d,7*d,p)
                if(selected) {p.style=android.graphics.Paint.Style.STROKE;p.strokeWidth=d;p.color=TEAL;canvas.drawRoundRect(bounds.left+2*d,top+2*d,bounds.right-2*d,bounds.bottom-2*d,7*d,7*d,p)}
                p.style=android.graphics.Paint.Style.STROKE;p.strokeCap=android.graphics.Paint.Cap.ROUND;p.color=TEXT;p.strokeWidth=(1+5*value/maximum)*d
                canvas.drawLine(cx-12*d,top+11*d,cx+12*d,top+11*d,p)
                p.style=android.graphics.Paint.Style.FILL;p.textAlign=android.graphics.Paint.Align.CENTER;p.textSize=11*c.resources.displayMetrics.scaledDensity;p.color=if(selected)TEAL else TEXT
                canvas.drawText(number,cx,top+29*d,p)
                if(isDefault) {p.textSize=8*c.resources.displayMetrics.scaledDensity;canvas.drawText("Default",cx,top+41*d,p)}
            }
            override fun setAlpha(alpha:Int){}
            override fun setColorFilter(filter:android.graphics.ColorFilter?){}
            @Deprecated("Deprecated in Android") override fun getOpacity()=android.graphics.PixelFormat.TRANSLUCENT
        }
        layoutParams=LinearLayout.LayoutParams(c.dp(48),c.dp(48))
    }
    fun dot(c:Context,label:String,ink:Int,radius:Int,selected:Boolean,click:()->Unit)=button(c,"",false,click).apply {
        contentDescription=label;tooltipText=label
        background=object:android.graphics.drawable.Drawable() {
            val p=android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
            override fun draw(canvas:android.graphics.Canvas) {
                val x=bounds.exactCenterX();val y=bounds.exactCenterY()
                p.style=android.graphics.Paint.Style.FILL;p.color=ink or 0xFF000000.toInt();canvas.drawCircle(x,y,c.dp(radius).toFloat(),p)
                p.style=android.graphics.Paint.Style.STROKE;p.strokeWidth=c.dp(1).toFloat();p.color=LINE;canvas.drawCircle(x,y,c.dp(radius).toFloat(),p)
                if(selected) {p.color=TEAL;canvas.drawCircle(x,y,c.dp(radius+4).toFloat(),p)}
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
        setPadding(c.dp(12),c.dp(8),c.dp(12),c.dp(8));background=rounded(SURFACE,c.dp(8).toFloat(),LINE)
        layoutParams=LinearLayout.LayoutParams(-1,c.dp(56)).apply { bottomMargin=c.dp(16) }
        contentDescription=hint
    }
}
