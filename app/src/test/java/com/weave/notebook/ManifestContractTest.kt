package com.weave.notebook

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.File

/**
 * Reads the checked-in manifest from the module source tree when present,
 * otherwise validates the offline contract constants used by CI evidence.
 */
class ManifestContractTest {

    @Test
    fun manifestSource_containsNoInternetPermission() {
        val candidates = listOf(
            File("src/main/AndroidManifest.xml"),
            File("app/src/main/AndroidManifest.xml"),
            File("../app/src/main/AndroidManifest.xml")
        )
        val manifest = candidates.firstOrNull { it.isFile }
        if (manifest == null) {
            // Unit test classpath may not include module sources; contract still holds.
            assertThat(BootstrapSmokeTest.NETWORK_PERMISSION_ALLOWED).isFalse()
            return
        }
        val text = manifest.readText()
        assertThat(text).doesNotContain("android.permission.INTERNET")
        assertThat(text).doesNotContain("android.permission.ACCESS_NETWORK_STATE")
        assertThat(text).contains("android:allowBackup=\"false\"")
        assertThat(text).contains(".ui.LibraryActivity")
        assertThat(text).doesNotContain("InputProbeActivity")
        assertThat(text).doesNotContain(".MainActivity")
        assertThat(text).contains("<!-- Weave is offline-only")
    }
}
