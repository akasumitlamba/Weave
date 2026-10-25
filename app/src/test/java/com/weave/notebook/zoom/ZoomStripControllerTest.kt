package com.weave.notebook.zoom

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ZoomStripControllerTest {

    @Test
    fun inverseMapping_roundTripsInsideSource() {
        val c = ZoomStripController(ZoomStripState(leftMm = 20f, topMm = 50f, widthMm = 100f, heightMm = 20f))
        val (sx, sy) = c.mapStripToPage(10f, 5f)
        val back = c.mapPageToStrip(sx, sy)!!
        assertThat(back.first).isWithin(0.001f).of(10f)
        assertThat(back.second).isWithin(0.001f).of(5f)
    }

    @Test
    fun nextLine_movesSourceOnly() {
        val c = ZoomStripController(ZoomStripState(topMm = 40f, heightMm = 24f))
        c.nextLine(8f)
        assertThat(c.state.topMm).isEqualTo(48f)
        assertThat(c.state.heightMm).isEqualTo(24f)
    }

    @Test
    fun magnification_doesNotChangeSavedCoordinates() {
        val c = ZoomStripController()
        val before = c.mapStripToPage(1f, 1f)
        c.setMagnification(4f)
        val after = c.mapStripToPage(1f, 1f)
        assertThat(after).isEqualTo(before)
    }
}
