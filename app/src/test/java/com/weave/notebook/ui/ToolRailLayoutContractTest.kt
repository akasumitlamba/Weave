package com.weave.notebook.ui

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.File

/**
 * Guards the editor tool rail: tools must sit in a horizontal strip along the edge,
 * never a vertical sidebar column.
 */
class ToolRailLayoutContractTest {

    @Test
    fun portraitToolRail_isHorizontalEdgeStrip() {
        val text = read("app/src/main/res/layout/activity_editor.xml")
        assertToolRailHorizontal(text)
    }

    @Test
    fun landscapeToolRail_isHorizontalEdgeStrip() {
        val text = read("app/src/main/res/layout-land/activity_editor.xml")
        assertToolRailHorizontal(text)
    }

    private fun assertToolRailHorizontal(layoutXml: String) {
        assertThat(layoutXml).contains("android:id=\"@+id/toolRail\"")
        val railBlock = layoutXml.substringAfter("android:id=\"@+id/toolRail\"")
            .substringBefore("android:id=\"@+id/inkCanvas\"")
        assertThat(railBlock).contains("android:orientation=\"horizontal\"")
        assertThat(railBlock).doesNotContain("android:orientation=\"vertical\"")
        for (id in listOf("toolPen", "toolMarker", "toolHighlighter", "toolEraser", "toolHand")) {
            assertThat(railBlock).contains("@+id/$id")
        }
    }

    private fun read(path: String): String {
        val f = File(path).takeIf { it.isFile } ?: File(path.removePrefix("app/"))
        assertThat(f.isFile).isTrue()
        return f.readText()
    }
}
