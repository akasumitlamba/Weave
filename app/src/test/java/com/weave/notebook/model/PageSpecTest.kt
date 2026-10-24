package com.weave.notebook.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PageSpecTest {

    @Test
    fun ruledSpacing_usesPageMillimeters() {
        val page = PageSpec(pageId = "p1", paperType = PaperType.RULED, ruleSpacingMm = 8f)
        assertThat(page.ruleSpacingMm).isEqualTo(8f)
        assertThat(PageGeometry.isAllowedRuleSpacing(6f)).isTrue()
        assertThat(PageGeometry.isAllowedRuleSpacing(10f)).isTrue()
        assertThat(PageGeometry.isAllowedRuleSpacing(7f)).isFalse()
    }

    @Test
    fun a4Dimensions_fixed() {
        val page = PageSpec(pageId = "p1")
        assertThat(page.widthMm).isEqualTo(210f)
        assertThat(page.heightMm).isEqualTo(297f)
        assertThat(page.aspectRatio).isWithin(0.0001f).of(210f / 297f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun invalidRuleSpacing_rejectedForRuled() {
        PageSpec(pageId = "p1", paperType = PaperType.RULED, ruleSpacingMm = 7f)
    }
}
