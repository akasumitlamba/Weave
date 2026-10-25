package com.weave.notebook.lifecycle

import android.os.Bundle
import com.weave.notebook.ui.ToolKind
import com.weave.notebook.zoom.ZoomStripState

/**
 * Preserves viewport, page, mode, and tool across rotation / process recreation.
 */
data class EditorSessionState(
    val noteId: String? = null,
    val noteTitle: String = "Untitled note",
    val tool: ToolKind = ToolKind.PEN,
    val writeMode: Boolean = true,
    val zoomPercent: Int = 100,
    val pageIndex: Int = 0,
    val pageCount: Int = 1,
    val panXPx: Float = 0f,
    val panYPx: Float = 0f,
    val zoomStrip: ZoomStripState = ZoomStripState()
) {
    fun toBundle(): Bundle = Bundle().apply {
        putString(KEY_NOTE_ID, noteId)
        putString(KEY_TITLE, noteTitle)
        putString(KEY_TOOL, tool.name)
        putBoolean(KEY_WRITE, writeMode)
        putInt(KEY_ZOOM, zoomPercent)
        putInt(KEY_PAGE_INDEX, pageIndex)
        putInt(KEY_PAGE_COUNT, pageCount)
        putFloat(KEY_PAN_X, panXPx)
        putFloat(KEY_PAN_Y, panYPx)
        putBoolean(KEY_STRIP_ENABLED, zoomStrip.enabled)
        putFloat(KEY_STRIP_MAG, zoomStrip.magnification)
        putFloat(KEY_STRIP_L, zoomStrip.leftMm)
        putFloat(KEY_STRIP_T, zoomStrip.topMm)
        putFloat(KEY_STRIP_W, zoomStrip.widthMm)
        putFloat(KEY_STRIP_H, zoomStrip.heightMm)
    }

    companion object {
        private const val KEY_NOTE_ID = "noteId"
        private const val KEY_TITLE = "title"
        private const val KEY_TOOL = "tool"
        private const val KEY_WRITE = "write"
        private const val KEY_ZOOM = "zoom"
        private const val KEY_PAGE_INDEX = "pageIndex"
        private const val KEY_PAGE_COUNT = "pageCount"
        private const val KEY_PAN_X = "panX"
        private const val KEY_PAN_Y = "panY"
        private const val KEY_STRIP_ENABLED = "stripEnabled"
        private const val KEY_STRIP_MAG = "stripMag"
        private const val KEY_STRIP_L = "stripL"
        private const val KEY_STRIP_T = "stripT"
        private const val KEY_STRIP_W = "stripW"
        private const val KEY_STRIP_H = "stripH"

        fun fromBundle(b: Bundle?): EditorSessionState {
            if (b == null) return EditorSessionState()
            val tool = b.getString(KEY_TOOL)?.let { runCatching { ToolKind.valueOf(it) }.getOrNull() } ?: ToolKind.PEN
            return EditorSessionState(
                noteId = b.getString(KEY_NOTE_ID),
                noteTitle = b.getString(KEY_TITLE) ?: "Untitled note",
                tool = tool,
                writeMode = b.getBoolean(KEY_WRITE, true),
                zoomPercent = b.getInt(KEY_ZOOM, 100),
                pageIndex = b.getInt(KEY_PAGE_INDEX, 0),
                pageCount = b.getInt(KEY_PAGE_COUNT, 1),
                panXPx = b.getFloat(KEY_PAN_X, 0f),
                panYPx = b.getFloat(KEY_PAN_Y, 0f),
                zoomStrip = ZoomStripState(
                    enabled = b.getBoolean(KEY_STRIP_ENABLED, false),
                    magnification = b.getFloat(KEY_STRIP_MAG, 2.5f),
                    leftMm = b.getFloat(KEY_STRIP_L, 15f),
                    topMm = b.getFloat(KEY_STRIP_T, 40f),
                    widthMm = b.getFloat(KEY_STRIP_W, 180f),
                    heightMm = b.getFloat(KEY_STRIP_H, 24f)
                )
            )
        }
    }
}

/** Cancels active stroke / shape hold timers on lifecycle interruption. */
class InterruptedWorkGuard {
    var strokeActive: Boolean = false
        private set
    var holdArmed: Boolean = false
        private set

    fun onStrokeBegin() {
        strokeActive = true
        holdArmed = true
    }

    fun onStrokeEnd() {
        strokeActive = false
        holdArmed = false
    }

    fun onLifecycleInterrupt(): Boolean {
        val had = strokeActive || holdArmed
        strokeActive = false
        holdArmed = false
        return had
    }
}
