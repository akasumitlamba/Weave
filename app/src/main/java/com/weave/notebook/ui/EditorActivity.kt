package com.weave.notebook.ui

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

import androidx.core.content.ContextCompat
import com.weave.notebook.R
import com.weave.notebook.databinding.ActivityEditorBinding
import com.weave.notebook.model.PageSpec
import com.weave.notebook.model.PaperType
import java.util.UUID

/**
 * Responsive editor shell: landscape and portrait toolbars, page controls,
 * accessible tool states. Live ink arrives in later milestones.
 */
class EditorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditorBinding
    private val chrome = EditorChromeController()

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

        binding.pagePreview.page = PageSpec(
            pageId = UUID.randomUUID().toString(),
            paperType = PaperType.RULED
        )

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

        binding.undoButton.setOnClickListener { /* history in PR 10 */ }
        binding.redoButton.setOnClickListener { /* history in PR 10 */ }
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
            binding.pagePreview.viewport.fitPage()
            binding.pagePreview.invalidate()
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
    }

    private fun bindTool(button: Button, tool: ToolKind) {
        button.setOnClickListener {
            chrome.selectTool(tool)
            if (tool == ToolKind.HAND) chrome.setWriteMode(false) else chrome.setWriteMode(true)
            renderChrome()
        }
    }

    private fun applyZoomToPreview() {
        val factor = chrome.state.zoomPercent / 100f
        val w = binding.pagePreview.width.toFloat().coerceAtLeast(1f)
        val h = binding.pagePreview.height.toFloat().coerceAtLeast(1f)
        binding.pagePreview.viewport.setZoom(factor, w / 2f, h / 2f)
        binding.pagePreview.invalidate()
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
        private const val STATE_TITLE = "state_title"
        private const val STATE_TOOL = "state_tool"
        private const val STATE_WRITE = "state_write"
        private const val STATE_ZOOM = "state_zoom"
        private const val STATE_PAGE_INDEX = "state_page_index"
        private const val STATE_PAGE_COUNT = "state_page_count"
    }
}
