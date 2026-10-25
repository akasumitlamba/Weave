package com.weave.notebook.input

/**
 * One captured pointer sample for the debug input probe.
 * Coordinates are view pixels; no personal handwriting is persisted to disk.
 */
data class InputSample(
    val eventAction: String,
    val pointerId: Int,
    val pointerIndex: Int,
    val toolType: Int,
    val toolTypeLabel: String,
    val x: Float,
    val y: Float,
    val size: Float,
    val pressure: Float,
    val orientation: Float,
    val eventTimeMs: Long,
    val isHistorical: Boolean,
    val canceled: Boolean
)

enum class StylusIdentityFinding {
    STYLUS_DISTINCT,
    FINGER_ONLY,
    MIXED_OR_UNKNOWN,
    UNVERIFIED
}

data class DeviceInputReport(
    val stylusIdentity: StylusIdentityFinding,
    val sawFinger: Boolean,
    val sawStylus: Boolean,
    val sawEraser: Boolean,
    val sawMouse: Boolean,
    val pressureRangeObserved: ClosedFloatingPointRange<Float>?,
    val sampleCount: Int,
    val notes: String
)
