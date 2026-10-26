package com.weave.notebook.ui

import android.app.ProgressDialog
import android.graphics.Color
import android.os.Bundle
import android.provider.DocumentsContract
import android.view.Gravity
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.weave.notebook.data.*
import com.weave.notebook.pdf.PdfSceneExporter
import com.weave.notebook.render.PagePreviewView
import com.weave.notebook.ui.NativeUi.dp
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

class ExportActivity:AppCompatActivity() {
    private lateinit var note:NotebookDocument
    private lateinit var filename:EditText
    private lateinit var first:EditText
    private lateinit var last:EditText
    private lateinit var selection:RadioGroup
    private lateinit var ruled:CheckBox
    private lateinit var preview:PagePreviewView
    private lateinit var caption:TextView
    private val choiceIds=IntArray(3) {android.view.View.generateViewId()}
    private var shown=0
    private var current=0
    private var chosen=listOf<NotebookPage>()
    private var includeLines=true
    private var pendingUri:android.net.Uri?=null
    private var restored:Bundle?=null
    private var chosenIndices=IntArray(0)
    private var exporting=false
    private val cancel=AtomicBoolean(false)
    private val picker=registerForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri->
        if(uri==null)return@registerForActivityResult
        pendingUri=uri
        if(::note.isInitialized)exportTo(uri)
    }
    private fun exportTo(uri:android.net.Uri) {
        pendingUri=null
        chosen=chosenIndices.map {note.pages[it]}
        if(chosen.isEmpty())return
        exporting=true;cancel.set(false)
        @Suppress("DEPRECATION") val progress=ProgressDialog(this).apply {
            setTitle("Exporting PDF");setMessage("Rendering ${chosen.size} pages…")
            setCancelable(false);setButton(ProgressDialog.BUTTON_NEGATIVE,"Cancel") {_,_->cancel.set(true)};show()
        }
        val snapshots=chosen.map {PdfSceneExporter.PageSnapshot(it.spec,it.strokes)}
        val temp=File.createTempFile("weave-export-",".pdf",cacheDir)
        NotebookIO.submit({
            try {
                temp.outputStream().use { out->
                    val result=PdfSceneExporter(includeLines).export(snapshots,out,{cancel.get()}) {page->
                        runOnUiThread {progress.setMessage("Rendered page $page of ${snapshots.size}")}
                    }
                    if(result is PdfSceneExporter.ExportResult.Err) error(result.message)
                }
                check(!cancel.get()) {"Export cancelled"}
                contentResolver.openOutputStream(uri,"w")?.use {out->temp.inputStream().use {input->
                    val buffer=ByteArray(8192)
                    while(true) {check(!cancel.get()) {"Export cancelled"};val n=input.read(buffer);if(n<0)break;out.write(buffer,0,n)}
                    out.flush()
                }} ?: error("Cannot write to the selected file")
                check(!cancel.get()) {"Export cancelled"}
            } catch(e:Exception) {
                runCatching {DocumentsContract.deleteDocument(contentResolver,uri)}
                throw e
            } finally {temp.delete()}
        }) {result->
            exporting=false;progress.dismiss()
            if(!isDestroyed) result.onSuccess {AlertDialog.Builder(this).setTitle("PDF saved").setMessage("${snapshots.size} pages exported.").setPositiveButton("Done") {_,_->finish()}.show()}
                .onFailure {AlertDialog.Builder(this).setTitle(if(cancel.get())"Export cancelled" else "Export failed").setMessage(it.message).setPositiveButton("OK",null).show()}
        }
    }
    override fun onCreate(state:Bundle?) {
        super.onCreate(state)
        NativeUi.applyTheme(this)
        restored=state
        chosenIndices=state?.getIntArray("chosen")?:IntArray(0)
        includeLines=state?.getBoolean("includeLines",true)?:true
        current=intent.getIntExtra("page",0)
        setContentView(NativeUi.text(this,"Loading export…",20f).apply {gravity=Gravity.CENTER})
        val id=intent.getStringExtra(EditorActivity.EXTRA_NOTE_ID)?:run {finish();return}
        NotebookIO.submit({NotebookIO.database.load(id)?:error("Notebook not found")}) {r->r.onSuccess {note=it;shown=(state?.getInt("shown")?:current).coerceIn(it.pages.indices);build();pendingUri?.let {uri->exportTo(uri)}}.onFailure {finish()}}
    }
    private fun build() {
        val root=NativeUi.column(this).apply {setPadding(dp(22),dp(12),dp(22),dp(12));setBackgroundColor(NativeUi.BG)}
        val header=NativeUi.row(this);header.addView(NativeUi.button(this,"‹ Back") {finish()});header.addView(NativeUi.text(this,"Export PDF",26f,true).apply {setPadding(dp(16),0,0,0)})
        root.addView(header)
        val wide=resources.configuration.screenWidthDp>resources.configuration.screenHeightDp
        val body=LinearLayout(this).apply {orientation=if(wide)LinearLayout.HORIZONTAL else LinearLayout.VERTICAL}
        val left=NativeUi.column(this)
        preview=PagePreviewView(this)
        left.addView(preview,LinearLayout.LayoutParams(-1,0,1f))
        val nav=NativeUi.row(this).apply {gravity=Gravity.CENTER}
        nav.addView(NativeUi.button(this,"‹") {shown=(shown-1).coerceAtLeast(0);renderPreview()})
        caption=NativeUi.text(this,"",14f);nav.addView(caption)
        nav.addView(NativeUi.button(this,"›") {shown=(shown+1).coerceAtMost(note.pages.lastIndex);renderPreview()})
        left.addView(nav)
        body.addView(left,if(wide)LinearLayout.LayoutParams(0,-1,1f) else LinearLayout.LayoutParams(-1,dp(260)))
        val settings=NativeUi.column(this).apply {setPadding(dp(24),dp(12),dp(12),0)}
        settings.addView(NativeUi.text(this,"File name",16f,true));filename=NativeUi.input(this,"File name","${note.title}.pdf");settings.addView(filename)
        settings.addView(NativeUi.text(this,"Pages",16f,true))
        selection=RadioGroup(this)
        listOf("All pages","Current page (${current+1})","Range").forEachIndexed {i,label->selection.addView(RadioButton(this).apply {text=label;id=choiceIds[i];minHeight=dp(48)})}
        selection.check(choiceIds[0]);settings.addView(selection)
        val range=NativeUi.row(this);first=NativeUi.input(this,"From","1");last=NativeUi.input(this,"To","${note.pages.size}")
        first.inputType=2;last.inputType=2
        range.addView(first,LinearLayout.LayoutParams(0,dp(52),1f));range.addView(NativeUi.text(this," to ",14f));range.addView(last,LinearLayout.LayoutParams(0,dp(52),1f))
        settings.addView(range)
        first.isEnabled=false;last.isEnabled=false
        selection.setOnCheckedChangeListener {_,id->first.isEnabled=id==choiceIds[2];last.isEnabled=id==choiceIds[2]}
        ruled=CheckBox(this).apply {text="Include ruled lines";isChecked=true;minHeight=dp(52)};settings.addView(ruled)
        settings.addView(NativeUi.text(this,"Paper  A4 · 210 × 297 mm\nPage orientation  Portrait",16f).apply {setPadding(0,dp(16),0,dp(16))})
        val buttons=NativeUi.row(this).apply {setPadding(dp(24),dp(6),dp(12),dp(6))}
        buttons.addView(NativeUi.button(this,"Cancel") {finish()})
        buttons.addView(NativeUi.button(this,"Save PDF",true) {savePdf()})
        val scroll=ScrollView(this).apply {addView(settings)}
        val settingsPanel=NativeUi.column(this)
        settingsPanel.addView(scroll,LinearLayout.LayoutParams(-1,0,1f));settingsPanel.addView(buttons)
        body.addView(settingsPanel,if(wide)LinearLayout.LayoutParams(0,-1,1.1f) else LinearLayout.LayoutParams(-1,0,1f))
        root.addView(body,LinearLayout.LayoutParams(-1,0,1f));setContentView(root)
        restored?.let {b->filename.setText(b.getString("filename",filename.text.toString()));first.setText(b.getString("first","1"));last.setText(b.getString("last",note.pages.size.toString()));selection.check(choiceIds[b.getInt("selection",0).coerceIn(0,2)]);ruled.isChecked=b.getBoolean("ruled",true)}
        renderPreview()
    }
    private fun renderPreview() {preview.page=note.pages[shown].spec;preview.strokes=note.pages[shown].strokes;caption.text="Page ${shown+1} of ${note.pages.size}"}
    private fun savePdf() {
        val range=when(selection.checkedRadioButtonId) {
            choiceIds[1]->current..current
            choiceIds[2]->{val a=first.text.toString().toIntOrNull();val b=last.text.toString().toIntOrNull()
                if(a==null || b==null || a<1 || b>note.pages.size || a>b) {first.error="Use a range from 1 to ${note.pages.size}";return};(a-1)..(b-1)}
            else->note.pages.indices
        }
        val name=filename.text.toString().trim()
        if(name.isBlank() || name.contains('/') || !name.endsWith(".pdf",true)) {filename.error="Enter a filename ending in .pdf";return}
        chosenIndices=range.toList().toIntArray();includeLines=ruled.isChecked;picker.launch(name)
    }
    override fun onSaveInstanceState(out:Bundle) {
        super.onSaveInstanceState(out);out.putIntArray("chosen",chosenIndices);out.putBoolean("includeLines",includeLines);out.putInt("shown",shown)
        if(::filename.isInitialized) {out.putString("filename",filename.text.toString());out.putString("first",first.text.toString());out.putString("last",last.text.toString());out.putInt("selection",choiceIds.indexOf(selection.checkedRadioButtonId));out.putBoolean("ruled",ruled.isChecked)}
    }
    override fun onDestroy() {if(exporting)cancel.set(true);super.onDestroy()}
}
