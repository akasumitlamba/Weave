package com.weave.notebook

import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.weave.notebook.data.*
import com.weave.notebook.ink.InkCanvasView
import com.weave.notebook.model.*
import com.weave.notebook.ui.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Synthetic on-device interaction, never represented as human handwriting acceptance. */
@RunWith(AndroidJUnit4::class)
class DeviceAcceptanceTest {
    private val inst=InstrumentationRegistry.getInstrumentation()
    private val context get()=inst.targetContext
    private fun waitFor(condition:()->Boolean) {
        val end=SystemClock.uptimeMillis()+15000
        while(SystemClock.uptimeMillis()<end) {if(condition())return;Thread.sleep(100)}
        error("Timed out waiting for tablet flow")
    }
    private fun views(root:View):List<View> = listOf(root)+if(root is ViewGroup)(0 until root.childCount).flatMap {views(root.getChildAt(it))} else emptyList()
    private fun click(a:Activity,label:String) {
        inst.runOnMainSync {val v=views(a.window.decorView).first {it.contentDescription?.toString()==label || (it is TextView && it.text.toString()==label)};assertTrue(v.performClick())}
        inst.waitForIdleSync()
    }
    private fun screenshot(name:String) {
        inst.waitForIdleSync();Thread.sleep(150)
        val bitmap=inst.uiAutomation.takeScreenshot();File(context.getExternalFilesDir(null),name).outputStream().use {bitmap.compress(Bitmap.CompressFormat.PNG,100,it)};bitmap.recycle()
    }
    @Test fun tablet_editorUndoRedoRotationAndSavedReopen() {
        val library=inst.startActivitySync(Intent(context,LibraryActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        inst.waitForIdleSync()
        Thread.sleep(400)
        inst.uiAutomation.rootInActiveWindow?.findAccessibilityNodeInfosByText("START WRITING")?.firstOrNull()?.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK)
        inst.waitForIdleSync()
        screenshot("weave-library-landscape.png")
        val monitor=inst.addMonitor(EditorActivity::class.java.name,null,false)
        click(library,"＋ New note")
        Thread.sleep(200)
        screenshot("weave-new-note.png")
        val dialogRoot=inst.uiAutomation.rootInActiveWindow
        fun findEdit(node:android.view.accessibility.AccessibilityNodeInfo):android.view.accessibility.AccessibilityNodeInfo? {
            if(node.className=="android.widget.EditText")return node
            for(i in 0 until node.childCount) {val child=node.getChild(i)?:continue;findEdit(child)?.let {return it}}
            return null
        }
        val field=findEdit(dialogRoot) ?: error("New note name field missing")
        field.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_SET_TEXT,android.os.Bundle().apply {putCharSequence(android.view.accessibility.AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,"Tablet verification")})
        dialogRoot.findAccessibilityNodeInfosByText("CREATE").first {it.text?.toString()?.equals("CREATE",true)==true}.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK)
        val a=inst.waitForMonitorWithTimeout(monitor,15000) ?: error("Create did not open editor")
        inst.removeMonitor(monitor)
        val id=a.intent.getStringExtra(EditorActivity.EXTRA_NOTE_ID)!!
        val db=NotebookDatabase(context);val note=db.load(id)!!;db.close()
        try {
        waitFor {var ready=false;inst.runOnMainSync {ready=views(a.window.decorView).any {it is InkCanvasView}};ready}
        var canvas:InkCanvasView?=null
        inst.runOnMainSync {canvas=views(a.window.decorView).filterIsInstance<InkCanvasView>().first()}
        click(a,"Pen")
        val t=SystemClock.uptimeMillis()
        inst.runOnMainSync {
            val v=canvas!!
            fun event(action:Int,x:Float,y:Float,dt:Long) {val p=v.viewport.pageToScreen(x,y);val e=MotionEvent.obtain(t,t+dt,action,p[0],p[1],0);v.onTouchEvent(e);e.recycle()}
            event(MotionEvent.ACTION_DOWN,30f,50f,0)
            for(i in 1..60)event(MotionEvent.ACTION_MOVE,30f+i,50f+kotlin.math.sin(i*0.2f)*3, i*10L)
            event(MotionEvent.ACTION_UP,90f,50f,650)
        }
        fun savedCount():Int {val d=NotebookDatabase(context);return try {d.load(note.id)!!.pages.first().strokes.size}finally {d.close()}}
        waitFor {savedCount()==1}
        click(a,"↶ Undo");waitFor {savedCount()==0}
        click(a,"↷ Redo");waitFor {savedCount()==1}
        fun dialogClick(label:String) {
            var found:android.view.accessibility.AccessibilityNodeInfo?=null
            waitFor {found=inst.uiAutomation.rootInActiveWindow?.findAccessibilityNodeInfosByText(label)?.firstOrNull {it.text?.toString()?.equals(label,true)==true};found!=null}
            val node=found ?: error("Missing dialog item $label")
            var target=node
            while(!target.isClickable && target.parent!=null)target=target.parent
            assertTrue(target.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK));inst.waitForIdleSync()
        }
        fun thickness(activity:Activity):View {var result:View?=null;inst.runOnMainSync {result=views(activity.window.decorView).first {it.contentDescription?.toString()?.startsWith("Thickness,")==true}};return result!!}
        fun chooseWidth(activity:Activity,label:String) {val control=thickness(activity);inst.runOnMainSync {control.performClick()};inst.waitForIdleSync();if(label=="0.25 mm")screenshot("weave-thickness-menu.png");dialogClick(label)}
        chooseWidth(a,"0.25 mm");chooseWidth(a,"0.45 mm · Default")
        inst.runOnMainSync {assertEquals(0.45f,views(a.window.decorView).filterIsInstance<InkCanvasView>().first().widthMm,0f)}
        click(a,"Eraser");chooseWidth(a,"2 mm");chooseWidth(a,"8 mm");chooseWidth(a,"4 mm · Default")
        inst.runOnMainSync {
            val v=views(a.window.decorView).filterIsInstance<InkCanvasView>().first()
            fun event(action:Int,x:Float,y:Float) {val p=v.viewport.pageToScreen(x,y);val now=SystemClock.uptimeMillis();val e=MotionEvent.obtain(now,now,action,p[0],p[1],0);v.onTouchEvent(e);e.recycle()}
            event(MotionEvent.ACTION_DOWN,30f,40f);event(MotionEvent.ACTION_MOVE,30f,55f)
            assertEquals(1,v.erasedPreview.size);assertEquals(1,v.strokes.size)
        }
        assertEquals(1,savedCount());Thread.sleep(100);screenshot("weave-eraser-before-lift.png")
        inst.runOnMainSync {
            val v=views(a.window.decorView).filterIsInstance<InkCanvasView>().first();val p=v.viewport.pageToScreen(30f,55f)
            val now=SystemClock.uptimeMillis();val e=MotionEvent.obtain(now,now,MotionEvent.ACTION_UP,p[0],p[1],0);v.onTouchEvent(e);e.recycle()
        }
        waitFor {savedCount()==0};click(a,"↶ Undo");waitFor {savedCount()==1};click(a,"↷ Redo");waitFor {savedCount()==0};click(a,"↶ Undo");waitFor {savedCount()==1}
        click(a,"Pen")
        inst.runOnMainSync {
            val all=views(a.window.decorView)
            val ink=all.filterIsInstance<InkCanvasView>().first()
            val pen=all.first {it.contentDescription=="Pen"}
            val pos=IntArray(2);pen.getLocationOnScreen(pos)
            val canvasPos=IntArray(2);ink.getLocationOnScreen(canvasPos)
            assertTrue(pos[0]>=canvasPos[0]+ink.width)
            assertFalse(all.filterIsInstance<TextView>().any {it.text=="Fit" || it.text=="Fullscreen" || it.text=="Pen"})
            val bottom=canvasPos[1]+ink.height
            val navigation=androidx.core.view.ViewCompat.getRootWindowInsets(ink)!!.getInsets(androidx.core.view.WindowInsetsCompat.Type.navigationBars())
            assertTrue(kotlin.math.abs(a.window.decorView.height-navigation.bottom-bottom)<=2)
            val header=all.first {it.contentDescription=="Fullscreen"};header.getLocationOnScreen(pos)
            val insets=androidx.core.view.ViewCompat.getRootWindowInsets(header)!!.getInsets(androidx.core.view.WindowInsetsCompat.Type.statusBars())
            assertTrue(pos[1]>=insets.top)
        }
        click(a,"Pages");screenshot("weave-pages-expanded.png");click(a,"Pages")
        click(a,"Ink color");inst.uiAutomation.rootInActiveWindow.findAccessibilityNodeInfosByText("Blue").first().performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK);inst.waitForIdleSync()
        screenshot("weave-editor-landscape.png")
        click(a,"⋮");dialogClick("Paper");screenshot("weave-paper-menu.png");dialogClick("Cancel")
        // Recreate with the same note, then verify the acknowledged stroke from SQLite.
        inst.runOnMainSync {a.finish()};inst.waitForIdleSync()
        val b=inst.startActivitySync(Intent(context,EditorActivity::class.java).putExtra(EditorActivity.EXTRA_NOTE_ID,note.id).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        waitFor {var ready=false;inst.runOnMainSync {ready=views(b.window.decorView).filterIsInstance<InkCanvasView>().firstOrNull()?.strokes?.size==1};ready}
        val exportMonitor=inst.addMonitor(ExportActivity::class.java.name,null,false)
        click(b,"⋮");dialogClick("Export PDF")
        val export=inst.waitForMonitorWithTimeout(exportMonitor,15000)?:error("Export did not open")
        inst.removeMonitor(exportMonitor)
        waitFor {var ready=false;inst.runOnMainSync {ready=views(export.window.decorView).any {it is android.widget.EditText}};ready}
        inst.waitForIdleSync();Thread.sleep(650);screenshot("weave-export-landscape.png")
        click(export,"‹ Back")
        click(b,"Fullscreen");Thread.sleep(650);inst.waitForIdleSync()
        inst.runOnMainSync {
            val visible=views(b.window.decorView)
            assertFalse(visible.any {it.contentDescription?.toString() in setOf("Hand","Navigate","Write")})
            assertFalse(visible.filterIsInstance<TextView>().any {it.text.toString()=="Hold shapes" || it.text.toString().matches(Regex("[0-9]+%"))})
            assertTrue(visible.any {it.contentDescription=="Exit fullscreen"})
            assertNull(visible.filterIsInstance<InkCanvasView>().first().sourceIndicator)
            assertFalse(visible.filterIsInstance<TextView>().any {it.text=="Zoom writing"})
        }
        assertNotNull(thickness(b));screenshot("weave-fullscreen-landscape.png")
        inst.runOnMainSync {b.requestedOrientation=ActivityInfo.SCREEN_ORIENTATION_PORTRAIT}
        Thread.sleep(1500);inst.waitForIdleSync();screenshot("weave-fullscreen-portrait.png")
        var rotated:Activity?=null
        inst.runOnMainSync {rotated=androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(androidx.test.runner.lifecycle.Stage.RESUMED).first {it is EditorActivity}}
        val active=rotated!!;assertNotNull(thickness(active));click(active,"Exit fullscreen")
        Thread.sleep(650);screenshot("weave-editor-portrait.png")
        assertNotNull(thickness(active))
        inst.runOnMainSync {active.finish()};inst.waitForIdleSync()
        } finally {
            inst.runOnMainSync {androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(androidx.test.runner.lifecycle.Stage.RESUMED).filter {it is EditorActivity && it.intent.getStringExtra(EditorActivity.EXTRA_NOTE_ID)==id}.forEach {it.finish()}}
            inst.waitForIdleSync();val cleanup=NotebookDatabase(context);cleanup.delete(id);cleanup.close()
        }
    }
}
