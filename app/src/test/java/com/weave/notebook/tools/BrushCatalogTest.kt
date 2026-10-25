package com.weave.notebook.tools

import com.google.common.truth.Truth.assertThat
import com.weave.notebook.model.BrushKind
import org.junit.Test

class BrushCatalogTest {

    @Test
    fun penWidths_inProductRange() {
        assertThat(BrushCatalog.PEN_DEFAULT_MM).isEqualTo(0.45f)
        assertThat(BrushCatalog.PEN_WIDTHS_MM.first()).isEqualTo(0.25f)
        assertThat(BrushCatalog.PEN_WIDTHS_MM.last()).isEqualTo(1.5f)
    }

    @Test
    fun toolChange_resetsToDefaultWidthForBrush() {
        val s = ToolSettings(brush = BrushKind.PEN, widthMm = 1.5f, colorArgb = BrushCatalog.INK_COLORS[1])
        val marker = s.withBrush(BrushKind.MARKER)
        assertThat(marker.brush).isEqualTo(BrushKind.MARKER)
        assertThat(marker.widthMm).isEqualTo(BrushCatalog.MARKER_DEFAULT_MM)
    }

    @Test
    fun highlighter_isTranslucentPreset() {
        for (c in BrushCatalog.HIGHLIGHTER_COLORS) {
            assertThat((c ushr 24) and 0xFF).isLessThan(0xFF)
        }
        assertThat(BrushCatalog.isOpaque(BrushKind.HIGHLIGHTER)).isFalse()
        assertThat(BrushCatalog.isOpaque(BrushKind.PEN)).isTrue()
    }
}
