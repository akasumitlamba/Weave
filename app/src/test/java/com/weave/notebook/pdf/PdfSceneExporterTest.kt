package com.weave.notebook.pdf

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PdfSceneExporterTest {

    @Test
    fun a4Bounds_withinPointTwoMm() {
        val (errW, errH) = PdfSceneExporter.a4ErrorMm()
        assertThat(errW).isLessThan(0.2f)
        assertThat(errH).isLessThan(0.2f)
    }

    @Test
    fun pageDimensions_areDocumentedApproximation() {
        assertThat(PdfSceneExporter.PAGE_WIDTH_PT).isEqualTo(595)
        assertThat(PdfSceneExporter.PAGE_HEIGHT_PT).isEqualTo(842)
    }
}
