package com.weave.notebook.geom

import com.google.common.truth.Truth.assertThat
import com.weave.notebook.model.PageGeometry
import org.junit.Test
import kotlin.math.abs

class ViewportTransformTest {

    @Test
    fun roundTrip_pageToScreenToPage_errorBelowTolerance() {
        val vp = ViewportTransform()
        for (size in listOf(1200f to 1920f, 1920f to 1200f, 800f to 1280f)) {
            vp.setViewSize(size.first, size.second)
            vp.fitPage()
            for (zoom in listOf(0.5f, 1f, 1.5f, 2.5f, 5f)) {
                vp.setZoom(zoom, size.first / 2f, size.second / 2f)
                val points = listOf(
                    0f to 0f,
                    PageGeometry.WIDTH_MM to 0f,
                    0f to PageGeometry.HEIGHT_MM,
                    PageGeometry.WIDTH_MM to PageGeometry.HEIGHT_MM,
                    105f to 148.5f
                )
                for ((x, y) in points) {
                    val screen = vp.pageToScreen(x, y)
                    val back = vp.screenToPage(screen[0], screen[1])
                    assertThat(abs(back[0] - x)).isLessThan(0.01f)
                    assertThat(abs(back[1] - y)).isLessThan(0.01f)
                }
            }
        }
    }

    @Test
    fun zoomAnchor_staysStable() {
        val vp = ViewportTransform()
        vp.setViewSize(1200f, 1920f)
        vp.fitPage()
        val anchorX = 600f
        val anchorY = 900f
        val pageBefore = vp.screenToPage(anchorX, anchorY).copyOf()
        vp.setZoom(2.5f, anchorX, anchorY)
        val pageAfter = vp.screenToPage(anchorX, anchorY)
        assertThat(abs(pageAfter[0] - pageBefore[0])).isLessThan(0.01f)
        assertThat(abs(pageAfter[1] - pageBefore[1])).isLessThan(0.01f)
    }

    @Test
    fun a4Aspect_stableAcrossViewportOrientations() {
        val portrait = ViewportTransform()
        portrait.setViewSize(1200f, 1920f)
        portrait.fitPage()
        val landscape = ViewportTransform()
        landscape.setViewSize(1920f, 1200f)
        landscape.fitPage()
        val ratio = PageGeometry.WIDTH_MM / PageGeometry.HEIGHT_MM
        assertThat(portrait.pageWidthMm / portrait.pageHeightMm).isWithin(0.0001f).of(ratio)
        assertThat(landscape.pageWidthMm / landscape.pageHeightMm).isWithin(0.0001f).of(ratio)
    }

    @Test
    fun pageBoundary_cornersMapInsideViewWhenFit() {
        val vp = ViewportTransform()
        vp.setViewSize(1200f, 1920f)
        vp.fitPage()
        val tl = vp.pageToScreen(0f, 0f)
        val br = vp.pageToScreen(PageGeometry.WIDTH_MM, PageGeometry.HEIGHT_MM)
        assertThat(tl[0]).isAtLeast(0f)
        assertThat(tl[1]).isAtLeast(0f)
        assertThat(br[0]).isAtMost(1200f)
        assertThat(br[1]).isAtMost(1920f)
    }
}
