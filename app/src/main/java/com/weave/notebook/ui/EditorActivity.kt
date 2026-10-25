package com.weave.notebook.ui

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import android.widget.Toast
import com.weave.notebook.R
import com.weave.notebook.command.CommandHistory
import com.weave.notebook.command.EditorCommand
import com.weave.notebook.command.StrokeEraser
import com.weave.notebook.databinding.ActivityEditorBinding
import com.weave.notebook.guards.GuardEdge
import com.weave.notebook.model.BrushKind
import com.weave.notebook.model.PageSpec
import com.weave.notebook.model.PaperType
import com.weave.notebook.tools.ToolSettings
import java.util.UUID




/**
 * Editor shell with live raw Pen ink (PR 05). Gesture modes refined in PR 06.
 */
class EditorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditorBinding
    private val chrome = EditorChromeController()
    private var toolSettings = ToolSettings()
    private val history = CommandHistory()



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val title = intent.getStringExtra(EXTRA_NOTE_TITLE)
            ?: savedInstanceState?.getString(STATE_TITLE)
            ?: getString(R.string.default_note_title)
        chrome.setNoteTitle(title)

        if (savedInstanceState != null) {
            val tool = savedInstanceState.getString(STATE_TOOL)?.let { ToolKind.valueOf(it) }
                ?: ToolKind.PEN
            chrome.selectTool(tool)
            chrome.setWriteMode(savedInstanceState.getBoolean(STATE_WRITE, true))
            chrome.setZoomPercent(savedInstanceState.getInt(STATE_ZOOM, 100))
            chrome.setPage(
                savedInstanceState.getInt(STATE_PAGE_INDEX, 0),
                savedInstanceState.getInt(STATE_PAGE_COUNT, 1)
            )
        }

        binding.inkCanvas.page = PageSpec(
            pageId = UUID.randomUUID().toString(),
            paperType = PaperType.RULED
        )
        binding.inkCanvas.brush = BrushKind.PEN
        binding.inkCanvas.widthMm = 0.45f
        binding.inkCanvas.onStrokeCommitted = { stroke ->
            if (toolSettings.brush == BrushKind.ERASER) {
                // Eraser tool: treat tap/stroke center as whole-stroke erase.
                val mid = stroke.samples.getOrNull(stroke.samples.size / 2)
                binding.inkCanvas.strokes.removeAll { it.strokeId == stroke.strokeId }
                if (mid != null) {
                    val hit = StrokeEraser.hitTest(
                        binding.inkCanvas.strokes.toList(),
                        mid.xMm,
                        mid.yMm,
                        radiusMm = toolSettings.widthMm
                    )
                    if (hit != null) {
                        binding.inkCanvas.strokes.remove(hit)
                        history.push(EditorCommand.EraseStroke(hit))
                    }
                }
            } else {
                history.push(EditorCommand.AddStroke(stroke))
            }
            chrome.setHistory(history.canUndo, history.canRedo)
            renderChrome()
            binding.inkCanvas.invalidate()
        }

        wireChrome()
        renderChrome()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(STATE_TITLE, chrome.state.noteTitle)
        outState.putString(STATE_TOOL, chrome.state.selectedTool.name)
        outState.putBoolean(STATE_WRITE, chrome.state.writeMode)
        outState.putInt(STATE_ZOOM, chrome.state.zoomPercent)
        outState.putInt(STATE_PAGE_INDEX, chrome.state.pageIndex)
        outState.putInt(STATE_PAGE_COUNT, chrome.state.pageCount)
    }

    private fun wireChrome() {
        binding.backButton.setOnClickListener { finish() }
        binding.noteTitle.text = chrome.state.noteTitle

        bindTool(binding.toolPen, ToolKind.PEN)
        bindTool(binding.toolMarker, ToolKind.MARKER)
        bindTool(binding.toolHighlighter, ToolKind.HIGHLIGHTER)
        bindTool(binding.toolEraser, ToolKind.ERASER)
        bindTool(binding.toolHand, ToolKind.HAND)

        binding.undoButton.setOnClickListener { applyUndo() }
        binding.redoButton.setOnClickListener { applyRedo() }
        binding.pagesButton.setOnClickListener { /* page drawer in PR 15 */ }
        binding.exportButton.setOnClickListener { /* export in PR 20 */ }

        binding.zoomOutButton.setOnClickListener {
            chrome.setZoomPercent(chrome.state.zoomPercent - 25)
            applyZoomToPreview()
            renderChrome()
        }
        binding.zoomInButton.setOnClickListener {
            chrome.setZoomPercent(chrome.state.zoomPercent + 25)
            applyZoomToPreview()
            renderChrome()
        }
        binding.fitButton.setOnClickListener {
            chrome.setZoomPercent(100)
            binding.inkCanvas.viewport.fitPage()
            binding.inkCanvas.invalidate()
            renderChrome()
        }

        binding.modeWriteButton.setOnClickListener {
            chrome.setWriteMode(true)
            chrome.selectTool(ToolKind.PEN)
            renderChrome()
        }
        binding.modeNavigateButton.setOnClickListener {
            chrome.setWriteMode(false)
            renderChrome()
        }

        binding.palmGuardButton.setOnClickListener {
            val g = binding.inkCanvas.palmGuards
            val next = when (g.guard.edge) {
                GuardEdge.BOTTOM -> GuardEdge.LEFT
                GuardEdge.LEFT -> GuardEdge.RIGHT
                GuardEdge.RIGHT -> GuardEdge.BOTTOM
            }
            g.setHandedness(next)
            Toast.makeText(this, R.string.palm_guard_hint, Toast.LENGTH_SHORT).show()
            binding.inkCanvas.invalidate()
        }
        binding.palmGuardButton.setOnLongClickListener {
            val g = binding.inkCanvas.palmGuards
            g.setLocked(!g.guard.locked)
            true
        }
    }

    private fun bindTool(button: Button, tool: ToolKind) {
        button.setOnClickListener {
            chrome.selectTool(tool)
            if (tool == ToolKind.HAND) chrome.setWriteMode(false) else chrome.setWriteMode(true)
            renderChrome()
        }
    }

    private fun applyUndo() {
        when (val cmd = history.undo()) {
            is EditorCommand.AddStroke -> binding.inkCanvas.strokes.removeAll { it.strokeId == cmd.stroke.strokeId }
            is EditorCommand.EraseStroke -> binding.inkCanvas.strokes.add(cmd.stroke)
            is EditorCommand.ReplaceStroke -> {
                binding.inkCanvas.strokes.removeAll { it.strokeId == cmd.after.strokeId }
                binding.inkCanvas.strokes.add(cmd.before)
            }
            null -> Unit
        }
        chrome.setHistory(history.canUndo, history.canRedo)
        renderChrome()
        binding.inkCanvas.invalidate()
    }

    private fun applyRedo() {
        when (val cmd = history.redo()) {
            is EditorCommand.AddStroke -> binding.inkCanvas.strokes.add(cmd.stroke)
            is EditorCommand.EraseStroke -> binding.inkCanvas.strokes.removeAll { it.strokeId == cmd.stroke.strokeId }
            is EditorCommand.ReplaceStroke -> {
                binding.inkCanvas.strokes.removeAll { it.strokeId == cmd.before.strokeId }
                binding.inkCanvas.strokes.add(cmd.after)
            }
            null -> Unit
        }
        chrome.setHistory(history.canUndo, history.canRedo)
        renderChrome()
        binding.inkCanvas.invalidate()
    }

    private fun applyZoomToPreview() {
        val factor = chrome.state.zoomPercent / 100f
        val w = binding.inkCanvas.width.toFloat().coerceAtLeast(1f)
        val h = binding.inkCanvas.height.toFloat().coerceAtLeast(1f)
        binding.inkCanvas.viewport.setZoom(factor, w / 2f, h / 2f)
        binding.inkCanvas.invalidate()
    }

    private fun renderChrome() {
        val s = chrome.state
        binding.noteTitle.text = s.noteTitle
        binding.saveStatus.text = when (s.saveLabel) {
            SaveLabel.SAVING -> getString(R.string.save_saving)
            SaveLabel.SAVED -> getString(R.string.save_saved)
            SaveLabel.SAVE_FAILED -> getString(R.string.save_failed)
        }
        binding.pageStatus.text = getString(R.string.page_status, s.pageIndex + 1, s.pageCount)
        binding.zoomStatus.text = getString(R.string.zoom_status, s.zoomPercent)

        styleTool(binding.toolPen, ToolKind.PEN)
        styleTool(binding.toolMarker, ToolKind.MARKER)
        styleTool(binding.toolHighlighter, ToolKind.HIGHLIGHTER)
        styleTool(binding.toolEraser, ToolKind.ERASER)
        styleTool(binding.toolHand, ToolKind.HAND)

        binding.undoButton.isEnabled = s.canUndo
        binding.redoButton.isEnabled = s.canRedo
        binding.undoButton.alpha = if (s.canUndo) 1f else 0.4f
        binding.redoButton.alpha = if (s.canRedo) 1f else 0.4f

        styleMode(binding.modeWriteButton, s.writeMode)
        styleMode(binding.modeNavigateButton, !s.writeMode)

        binding.inkCanvas.inkEnabled = s.writeMode && s.selectedTool != ToolKind.HAND
        val brush = when (s.selectedTool) {
            ToolKind.MARKER -> BrushKind.MARKER
            ToolKind.HIGHLIGHTER -> BrushKind.HIGHLIGHTER
            ToolKind.ERASER -> BrushKind.ERASER
            else -> BrushKind.PEN
        }
        if (toolSettings.brush != brush) {
            toolSettings = toolSettings.withBrush(brush)
        }
        binding.inkCanvas.brush = toolSettings.brush
        binding.inkCanvas.widthMm = toolSettings.widthMm
        binding.inkCanvas.colorArgb = toolSettings.colorArgb
    }

    private fun styleTool(button: Button, tool: ToolKind) {
        val selected = chrome.isToolSelected(tool)
        val bg = if (selected) R.color.teal_selection else R.color.paper_white
        val fg = if (selected) R.color.paper_white else R.color.charcoal
        button.setBackgroundColor(ContextCompat.getColor(this, bg))
        button.setTextColor(ContextCompat.getColor(this, fg))
        button.contentDescription = getString(R.string.tool_content_desc, tool.name.lowercase())
    }

    private fun styleMode(button: Button, selected: Boolean) {
        val bg = if (selected) R.color.teal_selection else R.color.paper_white
        val fg = if (selected) R.color.paper_white else R.color.charcoal
        button.setBackgroundColor(ContextCompat.getColor(this, bg))
        button.setTextColor(ContextCompat.getColor(this, fg))
    }

    companion object {
        const val EXTRA_NOTE_TITLE = "extra_note_title"
        const val EXTRA_NOTE_ID = "extra_note_id"

        private const val STATE_TITLE = "state_title"
        private const val STATE_TOOL = "state_tool"
        private const val STATE_WRITE = "state_write"
        private const val STATE_ZOOM = "state_zoom"
        private const val STATE_PAGE_INDEX = "state_page_index"
        private const val STATE_PAGE_COUNT = "state_page_count"
    }
}
