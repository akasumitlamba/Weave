package com.weave.notebook.ui

/**
 * Drawing / navigation tools shown in the editor chrome.
 * Persist selection in later PRs; this milestone only models UI state.
 */
enum class ToolKind {
    PEN,
    MARKER,
    HIGHLIGHTER,
    ERASER,
    HAND
}

data class EditorChromeState(
    val noteTitle: String = "Untitled note",
    val selectedTool: ToolKind = ToolKind.PEN,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val saveLabel: SaveLabel = SaveLabel.SAVED,
    val pageIndex: Int = 0,
    val pageCount: Int = 1,
    val zoomPercent: Int = 100,
    val writeMode: Boolean = true
)

enum class SaveLabel {
    SAVING,
    SAVED,
    SAVE_FAILED
}
