package com.weave.notebook.ui

import android.content.Intent
import android.graphics.Color
import android.graphics.RectF
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.appcompat.app.AppCompatActivity
import com.weave.notebook.data.*
import com.weave.notebook.ink.InkCanvasView
import com.weave.notebook.model.*
import com.weave.notebook.guards.*
import com.weave.notebook.smooth.SmoothingLevel
import com.weave.notebook.tools.BrushCatalog
import com.weave.notebook.render.PagePreviewView
import com.weave.notebook.ui.NativeUi.dp
import java.util.UUID

class EditorActivity: AppCompatActivity() {
    private data class Session(var note:NotebookDocument, var index:Int=0, val history:NotebookHistory=NotebookHistory(), var revision:Int=0,
        var saved:Int=0, var failed:Boolean=false, var observer:(()->Unit)?=null, var zoom:Float=1f,var panX:Float=0f,var panY:Float=0f)
    private lateinit var session:Session
    private lateinit var root:LinearLayout
    private lateinit var canvas:InkCanvasView
    private lateinit var strip:InkCanvasView
    private lateinit var overview:PagePreviewView
    private lateinit var stripBox:LinearLayout
    private lateinit var pageRail:LinearLayout
    private lateinit var tools:LinearLayout
    private lateinit var options:LinearLayout
    private lateinit var status:TextView
    private lateinit var pageLabel:TextView
    private lateinit var undo:Button
    private lateinit var redo:Button
    private val prefs by lazy {getSharedPreferences("writing",0)}
    private var tool=ToolKind.PEN
    private var pagesExpanded=false
    private lateinit var pageScroll:ScrollView
    private var fullscreen=false
    private var brush=BrushKind.PEN
    private var color=Color.BLACK
    private var strokeWidth=0.45f
    private var shapes=false
    private var smoothing=SmoothingLevel.LOW
    private var stripVisible=false
    private var stripLeft=15f
    private var stripTop=40f
    private var exitAfterSave=false
    private var exportAfterSave=false
    private var loading=true

    override fun onCreate(state:Bundle?) {
        super.onCreate(state)
        NativeUi.applyTheme(this)
        onBackPressedDispatcher.addCallback(this,object:androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {if(fullscreen)toggleFullscreen() else leave()}
        })
        window.statusBarColor=NativeUi.BG;window.navigationBarColor=NativeUi.BG
        val id=intent.getStringExtra(EXTRA_NOTE_ID) ?: state?.getString("noteId")
        if(id==null) {finish();return}
        tool=runCatching {ToolKind.valueOf(prefs.getString("tool","PEN")!!)}.getOrDefault(ToolKind.PEN)
        if(tool==ToolKind.HAND)tool=ToolKind.PEN
        fullscreen=state?.getBoolean("fullscreen")?:false
        pagesExpanded=state?.getBoolean("pagesExpanded")?:false
        shapes=prefs.getBoolean("shapes",false)
        smoothing=runCatching {SmoothingLevel.valueOf(prefs.getString("smoothing","LOW")!!)}.getOrDefault(SmoothingLevel.LOW)
        stripVisible=state?.getBoolean("strip")?:false;stripLeft=state?.getFloat("stripLeft")?:15f;stripTop=state?.getFloat("stripTop")?:40f
        val retained=lastCustomNonConfigurationInstance as? Session
        setContentView(NativeUi.text(this,"Opening notebook…",20f).apply {gravity=Gravity.CENTER})
        if(retained!=null) {session=retained;loading=false;build();return}
        NotebookIO.submit({NotebookIO.database.load(id)?:error("Notebook could not be found")}) {r->
            if(isFinishing || isDestroyed) return@submit
            r.onSuccess {doc->
                session=Session(doc,(state?.getInt("page")?:prefs.getInt("page.$id",0)).coerceIn(doc.pages.indices))
                if(state!=null) {session.zoom=state.getFloat("zoom",1f);session.panX=state.getFloat("panX");session.panY=state.getFloat("panY")}
                loading=false;build()
            }.onFailure {MaterialAlertDialogBuilder(this).setTitle("Could not open notebook").setMessage(it.message).setPositiveButton("Back") {_,_->finish()}.show()}
        }
    }
    override fun onRetainCustomNonConfigurationInstance():Any? {rememberViewport();return if(loading) null else session}
    override fun onSaveInstanceState(out:Bundle) {
        super.onSaveInstanceState(out);if(loading)return
        rememberViewport();out.putString("noteId",session.note.id);out.putInt("page",session.index)
        out.putFloat("zoom",session.zoom);out.putFloat("panX",session.panX);out.putFloat("panY",session.panY)
        out.putBoolean("pagesExpanded",pagesExpanded);out.putBoolean("fullscreen",fullscreen);out.putBoolean("strip",stripVisible);out.putFloat("stripLeft",stripLeft);out.putFloat("stripTop",stripTop)
    }
    override fun onPause() {if(!loading) {canvas.cancelWork();strip.cancelWork();rememberViewport();saveGuard()};super.onPause()}
    private fun rememberViewport() {if(!loading && ::canvas.isInitialized) {session.zoom=canvas.viewport.zoom;session.panX=canvas.viewport.panXPx;session.panY=canvas.viewport.panYPx}}
    private fun build() {
        // Always consume system/cutout insets, including transient bars in fullscreen.
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window,false)
        androidx.core.view.WindowInsetsControllerCompat(window,window.decorView).apply {
            systemBarsBehavior=androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            if(fullscreen)hide(androidx.core.view.WindowInsetsCompat.Type.systemBars()) else show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
        }
        root=NativeUi.column(this).apply {setBackgroundColor(NativeUi.BG)}
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(root) {v,insets->
            val bars=insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars() or androidx.core.view.WindowInsetsCompat.Type.displayCutout())
            v.setPadding(bars.left,bars.top,bars.right,bars.bottom);insets
        }
        val landscape=resources.configuration.screenWidthDp>resources.configuration.screenHeightDp
        val header=NativeUi.row(this).apply {setPadding(dp(4),0,dp(4),0)}
        header.addView(sideAction("‹ Back") {leave()})
        header.addView(NativeUi.text(this,session.note.title,17f,true).apply {maxLines=1;ellipsize=android.text.TextUtils.TruncateAt.END;setPadding(dp(8),0,dp(8),0)},LinearLayout.LayoutParams(0,-2,1f))
        status=NativeUi.text(this,"Saved",11f).apply {setPadding(dp(6),0,dp(6),0);setOnClickListener {save()};contentDescription="Save status. Tap to retry a failed save"}
        header.addView(status)
        header.addView(sideAction(if(fullscreen)"Exit fullscreen" else "Fullscreen") {toggleFullscreen()})
        header.addView(sideAction("⋮") {moreOptions()})
        root.addView(header)
        val workspace=FrameLayout(this)
        canvas=InkCanvasView(this)
        workspace.addView(canvas,FrameLayout.LayoutParams(-1,-1))
        val left=NativeUi.column(this).apply {
            gravity=Gravity.CENTER_HORIZONTAL;setPadding(0,dp(4),0,dp(4))
            background=NativeUi.rounded(NativeUi.SURFACE,dp(18).toFloat(),NativeUi.LINE);elevation=dp(3).toFloat()
        }
        undo=sideAction("↶ Undo") {undo()};redo=sideAction("↷ Redo") {redo()}
        left.addView(undo);left.addView(redo)
        left.addView(sideAction("Pages") {pagesExpanded=!pagesExpanded;pageScroll.visibility=if(pagesExpanded)View.VISIBLE else View.GONE})
        pageLabel=NativeUi.text(this,"",10f).apply {gravity=Gravity.CENTER;setPadding(0,0,0,dp(6))}
        left.addView(pageLabel)
        workspace.addView(left,FrameLayout.LayoutParams(dp(44),-2,Gravity.TOP or Gravity.START).apply {leftMargin=dp(8);topMargin=dp(12)})
        pageRail=NativeUi.column(this).apply {setPadding(dp(6),dp(6),dp(6),dp(6))}
        pageScroll=ScrollView(this).apply {addView(pageRail);background=NativeUi.rounded(NativeUi.SURFACE,dp(12).toFloat(),NativeUi.LINE);elevation=dp(3).toFloat();visibility=if(pagesExpanded)View.VISIBLE else View.GONE}
        workspace.addView(pageScroll,FrameLayout.LayoutParams(dp(100),-1,Gravity.TOP or Gravity.START).apply {leftMargin=dp(58);topMargin=dp(12);bottomMargin=dp(12)})
        tools=NativeUi.column(this).apply {gravity=Gravity.CENTER_HORIZONTAL}
        options=NativeUi.column(this).apply {gravity=Gravity.CENTER_HORIZONTAL}
        val right=NativeUi.column(this).apply {
            gravity=Gravity.CENTER_HORIZONTAL;setPadding(dp(2),dp(6),dp(2),dp(6))
            background=NativeUi.rounded(NativeUi.SURFACE,dp(18).toFloat(),NativeUi.LINE);elevation=dp(3).toFloat()
        }
        right.addView(tools)
        right.addView(View(this).apply {setBackgroundColor(NativeUi.LINE)},LinearLayout.LayoutParams(dp(28),dp(1)).apply {topMargin=dp(5);bottomMargin=dp(5)})
        right.addView(options)
        workspace.addView(ScrollView(this).apply {isVerticalScrollBarEnabled=false;clipToPadding=false;addView(right)},FrameLayout.LayoutParams(dp(48),-2,Gravity.TOP or Gravity.END).apply {rightMargin=dp(8);topMargin=dp(12);bottomMargin=dp(12)})
        root.addView(workspace,LinearLayout.LayoutParams(-1,0,1f))
        stripBox=NativeUi.column(this).apply {background=NativeUi.rounded(NativeUi.SURFACE);visibility=if(stripVisible) View.VISIBLE else View.GONE}
        val stripActions=NativeUi.row(this)
        stripActions.addView(NativeUi.text(this,"Zoom writing",16f,true).apply {setPadding(dp(12),0,dp(8),0)})
        stripActions.addView(NativeUi.button(this,"←") {moveStrip(-20f,0f)})
        stripActions.addView(NativeUi.button(this,"→") {moveStrip(20f,0f)})
        stripActions.addView(NativeUi.button(this,"↑") {moveStrip(0f,-current().spec.ruleSpacingMm)})
        stripActions.addView(NativeUi.button(this,"Next line ↓") {moveStrip(0f,current().spec.ruleSpacingMm)})
        stripActions.addView(NativeUi.spacer(this))
        stripActions.addView(NativeUi.button(this,"Close") {toggleStrip()})
        stripBox.addView(stripActions)
        overview=PagePreviewView(this).apply {contentDescription="Zoom writing source. Drag to choose a region";onSelectRegion={x,y->strip.cancelWork();stripLeft=(x-45).coerceIn(0f,120f);stripTop=(y-15).coerceIn(0f,267f);updateStrip()}}
        if(!landscape)stripBox.addView(overview,LinearLayout.LayoutParams(-1,dp(72)))
        strip=InkCanvasView(this)
        stripBox.addView(strip,LinearLayout.LayoutParams(-1,dp(if(landscape) 150 else 200)))
        if(!fullscreen)root.addView(stripBox)
        setContentView(root)
        androidx.core.view.ViewCompat.requestApplyInsets(root)
        canvas.onViewport={viewChanged()}
        listOf(canvas,strip).forEach {v->
            v.onStroke={raw,fitted->
                changePage(current().copy(strokes=current().strokes+raw))
                if(fitted!=null) changePage(current().copy(strokes=current().strokes.dropLast(1)+fitted))
            }
            v.onErasePreview={ids->listOf(canvas,strip).forEach {it.erasedPreview=ids;it.invalidate()}}
            v.onErase={ids->if(ids.isNotEmpty())changePage(current().copy(strokes=current().strokes.filter {it.strokeId !in ids}))}
            v.onShapeStatus={recognized->if(recognized)status.text="Shape recognized" else updateStatus()}
        }
        val guardEdge=runCatching {GuardEdge.valueOf(prefs.getString("guardEdge","BOTTOM")!!)}.getOrDefault(GuardEdge.BOTTOM)
        listOf(canvas,strip).forEach {v->v.palmGuards.guard=v.palmGuards.guard.copy(enabled=false,edge=guardEdge,locked=prefs.getBoolean("guardLock",false),sizePx=prefs.getFloat(if(v==strip)"stripGuardSize" else "guardSize",dp(if(v==strip)40 else 90).toFloat()))}
        canvas.post {canvas.viewport.restore(session.zoom,session.panX,session.panY);viewChanged();updateStrip()}
        session.observer={if(!isDestroyed)updateStatus()}
        renderTools();renderPage();updateStatus()
    }
    private fun current()=session.note.pages[session.index]
    private fun selectTool(t:ToolKind) {canvas.cancelWork();strip.cancelWork();tool=t;prefs.edit().putString("tool",t.name).apply();renderTools()}
    private fun renderTools() {
        tools.removeAllViews()
        ToolKind.entries.filter {it!=ToolKind.HAND}.forEach { t->
            val label=when(t) {ToolKind.PEN->"Pen";ToolKind.MARKER->"Marker";ToolKind.HIGHLIGHTER->"Highlighter";ToolKind.ERASER->"Eraser";ToolKind.HAND->"Hand"}
            tools.addView(NativeUi.tool(this,label,t==tool) {selectTool(t)}.apply {text="";compoundDrawablePadding=0;layoutParams=LinearLayout.LayoutParams(dp(44),dp(48));setOnLongClickListener {selectTool(t);toolOptions();true}})
        }
        brush=when(tool) {ToolKind.MARKER->BrushKind.MARKER;ToolKind.HIGHLIGHTER->BrushKind.HIGHLIGHTER;ToolKind.ERASER->BrushKind.ERASER;else->BrushKind.PEN}
        strokeWidth=prefs.getFloat("width.$brush",BrushCatalog.defaultWidth(brush))
        color=prefs.getInt("color.$brush",BrushCatalog.colorsFor(brush).first())
        options.removeAllViews()
        options.addView(NativeUi.button(this,"${formatWidth(strokeWidth)} ▾") {toolOptions()}.apply {
            contentDescription="Thickness, ${formatWidth(strokeWidth)} millimetres";textSize=12f;setPadding(0,0,0,0)
            background=android.graphics.drawable.ColorDrawable(Color.TRANSPARENT);layoutParams=LinearLayout.LayoutParams(dp(44),dp(44));tooltipText="Thickness"
        })
        if(brush!=BrushKind.ERASER) options.addView(NativeUi.button(this,"▾") {colorOptions()}.apply {
            contentDescription="Ink color";textSize=14f;setPadding(dp(8),0,dp(4),0)
            val chip=object:android.graphics.drawable.Drawable() {
                val p=android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
                override fun draw(c:android.graphics.Canvas) {
                    p.style=android.graphics.Paint.Style.FILL;p.color=color or Color.BLACK;c.drawCircle(bounds.exactCenterX(),bounds.exactCenterY(),dp(8).toFloat(),p)
                    p.style=android.graphics.Paint.Style.STROKE;p.strokeWidth=dp(1).toFloat();p.color=NativeUi.MUTED;c.drawCircle(bounds.exactCenterX(),bounds.exactCenterY(),dp(8).toFloat(),p)
                }
                override fun setAlpha(a:Int){};override fun setColorFilter(f:android.graphics.ColorFilter?){}
                @Deprecated("Deprecated in Android") override fun getOpacity()=android.graphics.PixelFormat.TRANSLUCENT
            }.apply {setBounds(0,0,dp(24),dp(24))}
            setCompoundDrawables(chip,null,null,null);compoundDrawablePadding=0
            background=android.graphics.drawable.ColorDrawable(Color.TRANSPARENT);layoutParams=LinearLayout.LayoutParams(dp(44),dp(44));tooltipText="Ink color"
        })
        applyTool()
    }
    private fun applyTool() {
        listOf(canvas,strip).forEach {v->v.cancelWork();v.brush=brush;v.widthMm=strokeWidth;v.colorArgb=color;v.inkEnabled=true;v.holdShapes=shapes && !fullscreen;v.pipeline.smoother.level=smoothing}
    }
    private fun renderPage() {
        session.index=session.index.coerceIn(session.note.pages.indices)
        val page=current()
        listOf(canvas,strip).forEach {it.page=page.spec;it.strokes=page.strokes;it.invalidate()}
        pageLabel.text="${session.index+1} / ${session.note.pages.size}"
        undo.isEnabled=session.history.canUndo;redo.isEnabled=session.history.canRedo
        undo.alpha=if(undo.isEnabled)1f else 0.4f;redo.alpha=if(redo.isEnabled)1f else 0.4f
        overview.page=page.spec;overview.strokes=page.strokes;overview.invalidate()
        renderRail();updateStrip()
        prefs.edit().putInt("page.${session.note.id}",session.index).apply()
    }
    private fun renderRail() {
        pageRail.removeAllViews()
        session.note.pages.forEachIndexed {i,p->
            val holder=NativeUi.column(this).apply {setPadding(dp(4),dp(4),dp(4),dp(4));background=NativeUi.rounded(if(i==session.index)NativeUi.SELECTED else NativeUi.SURFACE,dp(8).toFloat(),if(i==session.index)NativeUi.TEAL else NativeUi.LINE);setOnClickListener {switchPage(i)};setOnLongClickListener {pageMenu(i);true}}
            holder.addView(PagePreviewView(this).apply {page=p.spec;strokes=p.strokes;contentDescription="Page ${i+1}";setOnClickListener {switchPage(i)};setOnLongClickListener {pageMenu(i);true}},LinearLayout.LayoutParams(dp(76),dp(108)))
            holder.addView(NativeUi.text(this,"Page ${i+1}",11f).apply {gravity=Gravity.CENTER})
            pageRail.addView(holder,LinearLayout.LayoutParams(-1,-2).apply {bottomMargin=dp(8)})
        }
        pageRail.addView(sideAction("＋ Page") {addPage()})
    }
    private fun changePage(page:NotebookPage) {edit(session.note.copy(pages=session.note.pages.toMutableList().also {it[session.index]=page}))}
    private fun edit(next:NotebookDocument) {session.history.record(session.note);session.note=next.copy(modified=System.currentTimeMillis());renderPage();save()}
    private fun undo() {canvas.cancelWork();strip.cancelWork();session.history.undo(session.note)?.let {session.note=it.copy(modified=System.currentTimeMillis());renderPage();save()}}
    private fun redo() {canvas.cancelWork();strip.cancelWork();session.history.redo(session.note)?.let {session.note=it.copy(modified=System.currentTimeMillis());renderPage();save()}}
    private fun save() {
        if(loading)return
        val note=session.note;val target=++session.revision;updateStatus()
        val shared=session
        NotebookIO.submit({NotebookIO.database.save(note)}) {r->
            r.onSuccess {shared.saved=target;shared.failed=false}.onFailure {shared.failed=true}
            shared.observer?.invoke()
            if(!isDestroyed) {
                updateStatus()
                if(r.isSuccess && shared.saved==shared.revision) {
                    if(exitAfterSave) finish()
                    if(exportAfterSave) {exportAfterSave=false;startActivity(Intent(this,ExportActivity::class.java).putExtra(EXTRA_NOTE_ID,note.id).putExtra("page",session.index))}
                } else if(r.isFailure) {exitAfterSave=false;exportAfterSave=false;Toast.makeText(this,"Save failed. Tap the status to retry. Your previous saved copy is intact.",Toast.LENGTH_LONG).show()}
            }
        }
    }
    private fun updateStatus() {if(!::session.isInitialized || !::status.isInitialized)return;status.text=when {session.failed->"Save failed · Retry";session.saved<session.revision->"Saving…";else->"Saved"};status.setTextColor(if(session.failed)NativeUi.ERROR else NativeUi.MUTED)}
    private fun switchPage(i:Int) {if(i !in session.note.pages.indices)return;canvas.cancelWork();strip.cancelWork();session.index=i;canvas.viewport.fitPage();renderPage();viewChanged()}
    private fun addPage() {val p=NotebookPage(PageSpec(UUID.randomUUID().toString(),current().spec.paperType,current().spec.ruleSpacingMm));session.index=session.note.pages.size;edit(session.note.copy(pages=session.note.pages+p))}
    private fun pageDialog() {
        val items=session.note.pages.mapIndexed {i,p->"Page ${i+1}  ·  ${if(p.spec.paperType==PaperType.RULED) "Lined" else "Plain"}"}.toTypedArray()
        MaterialAlertDialogBuilder(this).setTitle("Pages · tap to open").setItems(items) {_,i->switchPage(i);pageMenu(i)}.setPositiveButton("＋ Add page") {_,_->addPage()}.setNegativeButton("Close",null).show()
    }
    private fun pageMenu(index:Int) {
        switchPage(index)
        MaterialAlertDialogBuilder(this).setTitle("Page ${index+1}").setItems(arrayOf("Write on this page","Change paper","Duplicate page","Move earlier","Move later","Delete page")) {_,which->
            when(which) {
                1->paperDialog()
                2->{val p=current();val id=UUID.randomUUID().toString();val copy=NotebookPage(p.spec.copy(pageId=id),p.strokes.map {it.copy(strokeId=UUID.randomUUID().toString(),pageId=id)})
                    val pages=session.note.pages.toMutableList();pages.add(index+1,copy);session.index=index+1;edit(session.note.copy(pages=pages))}
                3,4->{val to=index+if(which==3)-1 else 1;if(to in session.note.pages.indices) {val pages=session.note.pages.toMutableList();val p=pages.removeAt(index);pages.add(to,p);session.index=to;edit(session.note.copy(pages=pages))}}
                5->if(session.note.pages.size>1) MaterialAlertDialogBuilder(this).setTitle("Delete page ${index+1}?").setMessage("Undo can restore this page while the editor stays open.").setNegativeButton("Cancel",null).setPositiveButton("Delete") {_,_->val pages=session.note.pages.toMutableList();pages.removeAt(index);session.index=minOf(index,pages.lastIndex);edit(session.note.copy(pages=pages))}.show()
                    else Toast.makeText(this,"Keep at least one page in a notebook",Toast.LENGTH_SHORT).show()
            }
        }.show()
    }
    private fun paperDialog() {
        canvas.cancelWork();strip.cancelWork()
        val content=NativeUi.column(this).apply {setPadding(dp(16),dp(8),dp(16),dp(16))}
        val dialog=MaterialAlertDialogBuilder(this).setTitle("Paper").setView(content).setNegativeButton("Cancel",null).create()
        listOf("Plain","Lined · 6 mm","Lined · 8 mm","Lined · 10 mm").forEachIndexed {i,label->
            val row=NativeUi.row(this).apply {setPadding(dp(8),dp(8),dp(8),dp(8));background=NativeUi.rounded(NativeUi.SURFACE)}
            row.addView(PagePreviewView(this).apply {page=current().spec.copy(paperType=if(i==0)PaperType.PLAIN else PaperType.RULED,ruleSpacingMm=when(i){1->6f;3->10f;else->8f})},LinearLayout.LayoutParams(dp(48),dp(66)))
            row.addView(NativeUi.text(this,label,16f).apply {setPadding(dp(20),0,0,0)})
            row.contentDescription=label;row.setOnClickListener {
                changePage(current().copy(spec=current().spec.copy(paperType=if(i==0)PaperType.PLAIN else PaperType.RULED,ruleSpacingMm=when(i){1->6f;3->10f;else->8f})));dialog.dismiss()
            };content.addView(row,LinearLayout.LayoutParams(-1,dp(82)).apply {bottomMargin=dp(6)})
        };dialog.show()
    }
    private fun viewChanged() {canvas.invalidate();rememberViewport()}
    private fun compact(button:Button):Button = button.apply {text="";compoundDrawablePadding=0;setPadding(dp(12),0,dp(12),0);layoutParams=LinearLayout.LayoutParams(dp(44),dp(44))}
    private fun sideAction(label:String,click:()->Unit)=compact(NativeUi.button(this,label,click=click))
    private fun toggleFullscreen() {
        canvas.cancelWork();strip.cancelWork();rememberViewport();saveGuard()
        fullscreen=!fullscreen;build()
    }
    private fun toggleStrip() {canvas.cancelWork();strip.cancelWork();stripVisible=!stripVisible;stripBox.visibility=if(stripVisible)View.VISIBLE else View.GONE;canvas.post {updateStrip()}}
    private fun moveStrip(dx:Float,dy:Float) {strip.cancelWork();stripLeft=(stripLeft+dx).coerceIn(0f,120f);stripTop=(stripTop+dy).coerceIn(0f,267f);updateStrip()}
    private fun updateStrip() {if(!::strip.isInitialized)return;val r=RectF(stripLeft,stripTop,stripLeft+90f,stripTop+30f);canvas.sourceIndicator=if(stripVisible && !fullscreen)r else null;overview.sourceIndicator=r;overview.invalidate();strip.region(r);canvas.invalidate()}
    private fun saveGuard() {
        if(!::canvas.isInitialized)return
        val g=canvas.palmGuards.guard
        prefs.edit().putBoolean("guard",g.enabled).putString("guardEdge",g.edge.name).putBoolean("guardLock",g.locked)
            .putFloat("guardSize",g.sizePx).putFloat("stripGuardSize",strip.palmGuards.guard.sizePx).apply()
    }
    private fun guardDialog() {
        canvas.cancelWork();strip.cancelWork()
        MaterialAlertDialogBuilder(this).setTitle("Palm guard").setItems(arrayOf("Off","Bottom","Left handed · left edge","Right handed · right edge","Lock / unlock size","Writing window · centre","Clear writing window")) {_,i->
            listOf(canvas,strip).forEach {v->
                val g=v.palmGuards
                when(i) {
                    0->g.guard=g.guard.copy(enabled=false)
                    1,2,3->g.guard=g.guard.copy(enabled=true,edge=when(i){2->GuardEdge.LEFT;3->GuardEdge.RIGHT;else->GuardEdge.BOTTOM},sizePx=dp(if(v==strip)40 else 90).toFloat())
                    4->g.setLocked(!g.guard.locked)
                    5->g.writingWindow=WritingWindowState(true,v.width*0.12f,v.height*0.15f,v.width*0.88f,v.height*0.8f)
                    6->g.writingWindow=WritingWindowState()
                };v.invalidate()
            };saveGuard()
        }.show()
    }
    private fun moreOptions() {
        val content=NativeUi.column(this).apply {setPadding(dp(16),dp(8),dp(16),dp(16))}
        val dialog=MaterialAlertDialogBuilder(this).setTitle("Notebook").setView(content).setNegativeButton("Close",null).create()
        fun item(label:String,action:()->Unit) {content.addView(NativeUi.button(this,label) {dialog.dismiss();action()}.apply {gravity=Gravity.CENTER_VERTICAL;layoutParams=LinearLayout.LayoutParams(-1,dp(52)).apply {bottomMargin=dp(6)}})}
        item("Paper") {paperDialog()}
        item("Export PDF") {exportAfterSave=true;save()}
        item("Page actions") {pageMenu(session.index)}
        item("Fit page") {canvas.cancelWork();canvas.viewport.fitPage();viewChanged()}
        if(!fullscreen)content.addView(com.google.android.material.materialswitch.MaterialSwitch(this).apply {
            text="Hold to straighten shapes";setTextColor(NativeUi.TEXT);isChecked=shapes;minHeight=dp(52)
            setOnCheckedChangeListener {_,checked->shapes=checked;prefs.edit().putBoolean("shapes",checked).apply();applyTool()}
        })
        item("Stroke smoothing") {settings()}
        dialog.show()
    }
    private fun formatWidth(value:Float)=if(value==value.toInt().toFloat())value.toInt().toString() else value.toString()
    private fun toolOptions() {
        canvas.cancelWork();strip.cancelWork()
        val content=NativeUi.column(this).apply {setPadding(dp(16),dp(8),dp(16),dp(16))}
        val dialog=palette("Thickness · mm",content)
        BrushCatalog.widthsFor(brush).forEach {w->
            val row=NativeUi.row(this).apply {background=NativeUi.rounded(if(w==strokeWidth)NativeUi.SELECTED else NativeUi.SURFACE);setPadding(dp(12),0,dp(12),0)}
            row.addView(object:View(this) {
                val paint=android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {color=NativeUi.TEXT;strokeCap=android.graphics.Paint.Cap.ROUND;strokeWidth=(1f+10f*w/12f)*resources.displayMetrics.density}
                override fun onDraw(c:android.graphics.Canvas) {c.drawLine(dp(8).toFloat(),height/2f,width-dp(20).toFloat(),height/2f,paint)}
            },LinearLayout.LayoutParams(dp(84),dp(48)))
            row.addView(NativeUi.text(this,"${formatWidth(w)} mm${if(w==BrushCatalog.defaultWidth(brush))" · Default" else ""}",15f))
            row.contentDescription="Width ${formatWidth(w)} mm";row.isSelected=w==strokeWidth
            row.setOnClickListener {prefs.edit().putFloat("width.$brush",w).apply();renderTools();dialog.dismiss()}
            content.addView(row,LinearLayout.LayoutParams(-1,dp(52)).apply {bottomMargin=dp(4)})
        };dialog.showAsDropDown(options,dp(44)-dp(260),0,Gravity.START)
    }
    private fun colorOptions() {
        canvas.cancelWork();strip.cancelWork()
        val colors=BrushCatalog.colorsFor(brush)
        val names=if(brush==BrushKind.HIGHLIGHTER)listOf("Yellow","Pink","Green") else listOf("Black","Blue","Red","Green")
        val row=NativeUi.row(this).apply {gravity=Gravity.CENTER;setPadding(dp(16),dp(16),dp(16),dp(16))}
        val dialog=palette("Ink color",row)
        colors.forEachIndexed {i,c->row.addView(NativeUi.dot(this,names[i],c,12,c==color) {prefs.edit().putInt("color.$brush",c).apply();renderTools();dialog.dismiss()})}
        dialog.showAsDropDown(options,dp(44)-dp(260),0,Gravity.START)
    }
    private fun palette(title:String,content:View):PopupWindow {
        val panel=NativeUi.column(this).apply {
            background=NativeUi.rounded(NativeUi.SURFACE,dp(16).toFloat(),NativeUi.LINE)
            addView(NativeUi.text(this@EditorActivity,title,14f,true).apply {setPadding(dp(20),dp(16),dp(16),dp(4))})
            addView(content)
        }
        return PopupWindow(panel,dp(260),LinearLayout.LayoutParams.WRAP_CONTENT,true).apply {
            setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));elevation=dp(12).toFloat();isOutsideTouchable=true
        }
    }
    private fun settings() {
        canvas.cancelWork();strip.cancelWork()
        MaterialAlertDialogBuilder(this).setTitle("Handwriting smoothing").setSingleChoiceItems(arrayOf("Off · raw input","Low · default","Medium"),smoothing.ordinal) {dialog,i->
            smoothing=SmoothingLevel.entries[i];prefs.edit().putString("smoothing",smoothing.name).apply();applyTool();dialog.dismiss()
        }.setNegativeButton("Close",null).show()
    }
    private fun leave() {if(loading){finish();return};canvas.cancelWork();strip.cancelWork();exitAfterSave=true;save()}
    companion object {const val EXTRA_NOTE_ID="extra_note_id";const val EXTRA_NOTE_TITLE="extra_note_title"}
}
