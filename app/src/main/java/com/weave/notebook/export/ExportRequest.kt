package com.weave.notebook.export

/**
 * PDF export settings UI model. Paper size/orientation are read-only A4 portrait.
 */
enum class PageSelection { ALL, CURRENT, RANGE }

data class ExportRequest(
    val filename: String = "note.pdf",
    val selection: PageSelection = PageSelection.ALL,
    val rangeStart: Int = 1, // 1-based inclusive
    val rangeEnd: Int = 1,
    val includeRuledLines: Boolean = true,
    val pageCount: Int = 1,
    val currentPage: Int = 1
) {
    fun validated(): Validation {
        if (!filename.lowercase().endsWith(".pdf")) {
            return Validation.Invalid("Filename must end with .pdf")
        }
        return when (selection) {
            PageSelection.ALL -> Validation.Valid(IntRange(1, pageCount.coerceAtLeast(1)))
            PageSelection.CURRENT -> {
                if (currentPage !in 1..pageCount) Validation.Invalid("Current page out of range")
                else Validation.Valid(currentPage..currentPage)
            }
            PageSelection.RANGE -> {
                if (rangeStart < 1 || rangeEnd > pageCount || rangeStart > rangeEnd) {
                    Validation.Invalid("Invalid page range")
                } else Validation.Valid(rangeStart..rangeEnd)
            }
        }
    }

    sealed class Validation {
        data class Valid(val pages: IntRange) : Validation()
        data class Invalid(val reason: String) : Validation()
    }
}

enum class ExportProgress { IDLE, RUNNING, SUCCESS, CANCELLED, FAILED }

class ExportJobController {
    var progress: ExportProgress = ExportProgress.IDLE
        private set
    var cancelled: Boolean = false
        private set

    fun start() {
        cancelled = false
        progress = ExportProgress.RUNNING
    }

    fun cancel() {
        if (progress == ExportProgress.RUNNING) {
            cancelled = true
            progress = ExportProgress.CANCELLED
        }
    }

    fun completeSuccess() {
        if (!cancelled) progress = ExportProgress.SUCCESS
    }

    fun completeFailure() {
        if (progress != ExportProgress.CANCELLED) progress = ExportProgress.FAILED
    }

    fun isSuccess(): Boolean = progress == ExportProgress.SUCCESS
}
