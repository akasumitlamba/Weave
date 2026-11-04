package com.weave.notebook.a11y

import com.google.common.truth.Truth.assertThat
import com.weave.notebook.ui.EditorChromeController
import org.junit.Test

class AccessibilityContractTest {
    @Test
    fun minTouchTarget_remains48dp() {
        assertThat(EditorChromeController.MIN_TOUCH_TARGET_DP).isAtLeast(48)
    }

}
