package com.weave.notebook.a11y

import com.google.common.truth.Truth.assertThat
import com.weave.notebook.ui.EditorChromeController
import org.junit.Test
import java.io.File

class AccessibilityContractTest {
    @Test
    fun minTouchTarget_remains48dp() {
        assertThat(EditorChromeController.MIN_TOUCH_TARGET_DP).isAtLeast(48)
    }

    @Test
    fun firstRunHelp_documentsPassiveLimits() {
        val candidates = listOf(
            File("src/main/res/values/strings.xml"),
            File("app/src/main/res/values/strings.xml")
        )
        val f = candidates.first { it.isFile }
        val text = f.readText()
        assertThat(text).contains("first_run_body")
        assertThat(text).contains("palm")
        assertThat(text).contains("Navigate")
    }

    @Test
    fun essentialControls_haveContentDescriptionsInLayouts() {
        val layout = File("app/src/main/res/layout/activity_editor.xml")
            .takeIf { it.isFile } ?: File("src/main/res/layout/activity_editor.xml")
        val text = layout.readText()
        assertThat(text).contains("contentDescription")
        assertThat(text).contains("@dimen/min_touch_target")
    }
}
