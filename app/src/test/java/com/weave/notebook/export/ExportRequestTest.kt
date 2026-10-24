package com.weave.notebook.export

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ExportRequestTest {

    @Test
    fun invalidRange_blocked() {
        val v = ExportRequest(selection = PageSelection.RANGE, rangeStart = 3, rangeEnd = 1, pageCount = 5).validated()
        assertThat(v).isInstanceOf(ExportRequest.Validation.Invalid::class.java)
    }

    @Test
    fun cancellation_isNotSuccess() {
        val job = ExportJobController()
        job.start()
        job.cancel()
        assertThat(job.isSuccess()).isFalse()
        assertThat(job.progress).isEqualTo(ExportProgress.CANCELLED)
    }

    @Test
    fun allPages_valid() {
        val v = ExportRequest(pageCount = 3).validated() as ExportRequest.Validation.Valid
        assertThat(v.pages).isEqualTo(1..3)
    }
}
