package com.weave.notebook.ui

/**
 * Pure controller for toolbar selection and enabled states.
 * Keeps hit-target policy documented for layout tests.
 */
class EditorChromeController(
    initial: EditorChromeState = EditorChromeState()
) {
    var state: EditorChromeState = initial
        private set

    fun setNoteTitle(title: String) {
        state = state.copy(noteTitle = title)
    }

    fun selectTool(tool: ToolKind) {
        state = state.copy(selectedTool = tool)
    }

    fun setHistory(canUndo: Boolean, canRedo: Boolean) {
        state = state.copy(canUndo = canUndo, canRedo = canRedo)
    }

    fun setSaveLabel(label: SaveLabel) {
        state = state.copy(saveLabel = label)
    }

    fun setPage(index: Int, count: Int) {
        require(count >= 1)
        require(index in 0 until count)
        state = state.copy(pageIndex = index, pageCount = count)
    }

    fun setZoomPercent(percent: Int) {
        state = state.copy(zoomPercent = percent.coerceIn(50, 500))
    }

    fun setWriteMode(write: Boolean) {
        state = state.copy(writeMode = write)
        if (!write) {
            state = state.copy(selectedTool = ToolKind.HAND)
        }
    }

    fun isToolSelected(tool: ToolKind): Boolean = state.selectedTool == tool

    companion object {
        /** Product minimum touch target in density-independent pixels. */
        const val MIN_TOUCH_TARGET_DP = 48
    }
}
