package com.weave.notebook.input

import android.view.MotionEvent
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class InputProbeRecorderTest {

    @Test
    fun recordsMultiPointerAndCancelSynthetics() {
        val recorder = InputProbeRecorder(capacity = 64)
        recorder.recordSynthetic(
            InputSample(
                eventAction = "DOWN",
                pointerId = 1,
                pointerIndex = 0,
                toolType = MotionEvent.TOOL_TYPE_FINGER,
                toolTypeLabel = "FINGER",
                x = 10f,
                y = 10f,
                size = 0.2f,
                pressure = 1f,
                orientation = 0f,
                eventTimeMs = 1L,
                isHistorical = false,
                canceled = false
            )
        )
        recorder.recordSynthetic(
            InputSample(
                eventAction = "POINTER_DOWN",
                pointerId = 2,
                pointerIndex = 1,
                toolType = MotionEvent.TOOL_TYPE_FINGER,
                toolTypeLabel = "FINGER",
                x = 40f,
                y = 40f,
                size = 0.3f,
                pressure = 1f,
                orientation = 0f,
                eventTimeMs = 2L,
                isHistorical = false,
                canceled = false
            )
        )
        recorder.recordSynthetic(
            InputSample(
                eventAction = "CANCEL",
                pointerId = 1,
                pointerIndex = 0,
                toolType = MotionEvent.TOOL_TYPE_FINGER,
                toolTypeLabel = "FINGER",
                x = 12f,
                y = 12f,
                size = 0.2f,
                pressure = 1f,
                orientation = 0f,
                eventTimeMs = 3L,
                isHistorical = false,
                canceled = true
            )
        )

        val snap = recorder.snapshot()
        assertThat(snap).hasSize(3)
        assertThat(snap.map { it.pointerId }).containsExactly(1, 2, 1).inOrder()
        assertThat(snap.last().canceled).isTrue()

        val report = recorder.buildReport(deviceVerified = false)
        assertThat(report.stylusIdentity).isEqualTo(StylusIdentityFinding.UNVERIFIED)
        assertThat(report.sawFinger).isTrue()
        assertThat(report.sawStylus).isFalse()
    }

    @Test
    fun capacity_evictsOldest() {
        val recorder = InputProbeRecorder(capacity = 2)
        repeat(3) { i ->
            recorder.recordSynthetic(
                InputSample(
                    eventAction = "MOVE",
                    pointerId = i,
                    pointerIndex = 0,
                    toolType = MotionEvent.TOOL_TYPE_STYLUS,
                    toolTypeLabel = "STYLUS",
                    x = i.toFloat(),
                    y = 0f,
                    size = 0.1f,
                    pressure = 0.5f,
                    orientation = 0f,
                    eventTimeMs = i.toLong(),
                    isHistorical = false,
                    canceled = false
                )
            )
        }
        assertThat(recorder.snapshot()).hasSize(2)
        assertThat(recorder.snapshot().map { it.pointerId }).containsExactly(1, 2).inOrder()
        val verified = recorder.buildReport(deviceVerified = true)
        assertThat(verified.stylusIdentity).isEqualTo(StylusIdentityFinding.STYLUS_DISTINCT)
    }

}
