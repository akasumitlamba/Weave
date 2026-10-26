package com.weave.notebook.ui

import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.format.DateUtils
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.weave.notebook.data.*
import com.weave.notebook.backup.DocumentArchive
import com.weave.notebook.model.*
import com.weave.notebook.render.PagePreviewView
import com.weave.notebook.ui.NativeUi.dp

class LibraryActivity: AppCompatActivity() {
    private var libraryReady=false
    private lateinit var grid: GridLayout
    private lateinit var status: TextView
    private lateinit var recent:TextView
    private var docs=listOf<NotebookDocument>()
    private var backup:NotebookDocument?=null
    private var backupId:String?=null
    private val restore=registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if(uri!=null) {
            status.visibility=View.VISIBLE;status.text="Restoring notebook…"
            NotebookIO.submit({
                val note=contentResolver.openInputStream(uri)?.use { DocumentArchive.decode(it) } ?: error("Cannot open backup")
                NotebookIO.database.save(note);note
            }) { r -> r.onSuccess { refresh();toast("Notebook restored") }.onFailure { error(it) } }
        }
    }
    private val saveBackup=registerForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        val snapshot=backup;val id=backupId?:snapshot?.id;backup=null;backupId=null
        if(uri!=null && id!=null) {
            status.visibility=View.VISIBLE;status.text="Writing editable backup…"
            NotebookIO.submit({
                val note=snapshot?:NotebookIO.database.load(id)?:error("Notebook not found")
                val bytes=DocumentArchive.encode(note)
                contentResolver.openOutputStream(uri,"w")?.use { it.write(bytes);it.flush() } ?: error("Cannot write backup")
            }) { r -> r.onSuccess {status.visibility=View.GONE;toast("Backup saved")}.onFailure { error(it) } }
        }
    }
    override fun onCreate(state:Bundle?) {
        val splash=installSplashScreen()
        splash.setKeepOnScreenCondition { !libraryReady }
        super.onCreate(state)
        com.google.android.material.color.DynamicColors.applyToActivityIfAvailable(this)
        NativeUi.applyTheme(this)
        backupId=state?.getString("backupId")
        window.statusBarColor=NativeUi.BG;window.navigationBarColor=NativeUi.BG
        val root=NativeUi.column(this).apply { setBackgroundColor(NativeUi.BG);setPadding(dp(28),dp(16),dp(28),dp(12)) }
        val head=NativeUi.row(this)
        val brand=NativeUi.column(this)
        brand.addView(NativeUi.text(this,"Weave",32f,true))
        brand.addView(NativeUi.text(this,"Connecting ideas together",15f).apply {setTextColor(NativeUi.MUTED)})
        val identity=NativeUi.row(this)
        identity.addView(ImageView(this).apply {setImageResource(com.weave.notebook.R.drawable.weave_owl);scaleType=ImageView.ScaleType.FIT_CENTER;contentDescription="Weave owl"},LinearLayout.LayoutParams(dp(44),dp(52)).apply {rightMargin=dp(12)})
        identity.addView(brand)
        head.addView(identity,LinearLayout.LayoutParams(0,-2,1f))
        head.addView(NativeUi.button(this,"Restore") {restore.launch(arrayOf("application/octet-stream","application/zip","*/*"))})
        head.addView(NativeUi.button(this,"＋ New note",true) {newNote()})
        root.addView(head)
        recent=NativeUi.text(this,"Recent notes",18f,true).apply {setPadding(0,dp(26),0,dp(18))};root.addView(recent)
        grid=GridLayout(this)
        val scroll=ScrollView(this).apply {addView(grid);isFillViewport=true}
        root.addView(scroll,LinearLayout.LayoutParams(-1,0,1f))
        status=NativeUi.text(this,"",13f).apply {gravity=Gravity.CENTER;setPadding(0,dp(10),0,dp(4));setTextColor(NativeUi.MUTED)}
        root.addView(status);setContentView(root)
    }

    override fun onSaveInstanceState(out:Bundle) {super.onSaveInstanceState(out);out.putString("backupId",backupId?:backup?.id)}
    override fun onResume() {super.onResume();refresh()}
    private fun refresh() {
        NotebookIO.submit({NotebookIO.database.list()}) {r->libraryReady=true;r.onSuccess { docs=it;render() }.onFailure {error(it)} }
    }
    private fun render() {
        grid.removeAllViews();recent.visibility=if(docs.isEmpty())View.GONE else View.VISIBLE
        val columns=when {resources.configuration.screenWidthDp>=840->3;resources.configuration.screenWidthDp>=600->2;else->1}
        grid.columnCount=columns
        val cardWidth=(resources.displayMetrics.widthPixels-dp(56))/columns
        docs.forEach {note->
            val card=NativeUi.column(this).apply {
                background=NativeUi.rounded(NativeUi.SURFACE,dp(14).toFloat(),NativeUi.LINE)
                setPadding(dp(14),dp(14),dp(14),dp(12));elevation=dp(1).toFloat()
                contentDescription="${note.title}, ${note.pages.size} pages";setOnClickListener {open(note)}
            }
            val preview=PagePreviewView(this).apply {page=note.pages.first().spec;strokes=note.pages.first().strokes;setBackgroundColor(Color.WHITE)}
            val content=NativeUi.row(this)
            content.addView(preview,LinearLayout.LayoutParams(dp(100),dp(150)))
            val details=NativeUi.column(this).apply {setPadding(dp(12),0,0,0)}
            val row=NativeUi.row(this)
            row.addView(NativeUi.text(this,note.title,17f,true).apply {maxLines=3},LinearLayout.LayoutParams(0,-2,1f))
            row.addView(NativeUi.button(this,"⋮") {menu(note)}.apply {layoutParams=LinearLayout.LayoutParams(dp(48),dp(48));setPadding(dp(8),0,dp(8),0)})
            details.addView(row)
            details.addView(NativeUi.text(this,"${note.pages.size} ${if(note.pages.size==1) "page" else "pages"}",14f).apply {setTextColor(NativeUi.MUTED);setPadding(0,dp(8),0,dp(5))})
            details.addView(NativeUi.text(this,"Updated ${DateUtils.getRelativeTimeSpanString(note.modified)}",12f).apply {setTextColor(NativeUi.MUTED)})
            content.addView(details,LinearLayout.LayoutParams(0,-2,1f));card.addView(content)
            grid.addView(card,GridLayout.LayoutParams().apply {width=cardWidth-dp(12);setMargins(dp(6),dp(6),dp(6),dp(12))})
        }
        status.text="";status.visibility=View.GONE
    }
    private fun newNote() {
        val box=NativeUi.column(this).apply {setPadding(dp(24),dp(12),dp(24),dp(8))}
        box.addView(NativeUi.text(this,"Note name",15f));val title=NativeUi.input(this,"Note name","Untitled note");box.addView(title)
        box.addView(NativeUi.text(this,"Paper",15f))
        val paper=RadioGroup(this).apply {orientation=RadioGroup.HORIZONTAL}
        val lined=RadioButton(this).apply {text="Lined";id=View.generateViewId();isChecked=true;minHeight=dp(48)}
        val plain=RadioButton(this).apply {text="Plain";id=View.generateViewId();minHeight=dp(48)}
        paper.addView(lined,RadioGroup.LayoutParams(0,dp(48),1f));paper.addView(plain,RadioGroup.LayoutParams(0,dp(48),1f));box.addView(paper)
        val previews=NativeUi.row(this)
        listOf(PaperType.RULED,PaperType.PLAIN).forEach {type->
            previews.addView(PagePreviewView(this).apply {page=PageSpec(java.util.UUID.randomUUID().toString(),type);contentDescription=if(type==PaperType.RULED)"Lined A4 paper" else "Plain A4 paper";setOnClickListener {if(type==PaperType.RULED)lined.isChecked=true else plain.isChecked=true}},LinearLayout.LayoutParams(0,dp(130),1f).apply {setMargins(dp(8),0,dp(8),0)})
        };box.addView(previews)
        box.addView(NativeUi.text(this,"A4 · 210 × 297 mm",14f).apply {setPadding(0,dp(12),0,0)})
        val dialog=AlertDialog.Builder(this).setTitle("New note").setView(box).setNegativeButton("Cancel",null)
            .setPositiveButton("Create") {_,_->
                val note=NotebookDocument(title=title.text.toString().trim().ifEmpty {"Untitled note"},pages=listOf(NotebookPage(PageSpec(java.util.UUID.randomUUID().toString(),if(plain.isChecked) PaperType.PLAIN else PaperType.RULED))))
                NotebookIO.submit({NotebookIO.database.save(note)}) {r->r.onSuccess {open(note)}.onFailure {error(it)}}
            }.show()
        dialog.window?.setLayout(minOf(dp(420),resources.displayMetrics.widthPixels-dp(36)),-2)
    }
    private fun menu(note:NotebookDocument) {
        AlertDialog.Builder(this).setTitle(note.title).setItems(arrayOf("Rename","Duplicate","Backup notebook","Delete")) {_,which->
            when(which) {
                0->{val name=NativeUi.input(this,"Note name",note.title)
                    AlertDialog.Builder(this).setTitle("Rename notebook").setView(name).setNegativeButton("Cancel",null).setPositiveButton("Rename") {_,_->save(note.copy(title=name.text.toString().trim().ifEmpty {note.title},modified=System.currentTimeMillis()))}.show()}
                1->save(note.duplicate())
                2->{backup=note;backupId=note.id;saveBackup.launch("${note.title}.weave")}
                3->AlertDialog.Builder(this).setTitle("Delete ${note.title}?").setMessage("This deletes the notebook and all its pages from this tablet. Export a backup first if you need a copy.")
                    .setNegativeButton("Cancel",null).setPositiveButton("Delete") {_,_->NotebookIO.submit({NotebookIO.database.delete(note.id)}) {r->r.onSuccess {refresh()}.onFailure {error(it)}}}.show()
            }
        }.show()
    }
    private fun save(note:NotebookDocument) {NotebookIO.submit({NotebookIO.database.save(note)}) {r->r.onSuccess {refresh()}.onFailure {error(it)}}}
    private fun open(note:NotebookDocument) {startActivity(Intent(this,EditorActivity::class.java).putExtra(EditorActivity.EXTRA_NOTE_ID,note.id))}
    private fun toast(s:String) {Toast.makeText(this,s,Toast.LENGTH_LONG).show()}
    private fun error(e:Throwable) {status.visibility=View.GONE;AlertDialog.Builder(this).setTitle("Could not complete operation").setMessage(e.message?:"Please try again. Your saved notebooks have not been removed.").setPositiveButton("OK",null).show()}
}
