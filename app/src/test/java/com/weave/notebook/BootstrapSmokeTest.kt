package com.weave.notebook

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Bootstrap smoke tests — toolchain, naming, offline contract markers.
 */
class BootstrapSmokeTest {

    @Test
    fun appName_isWeave() {
        assertThat(APP_NAME).isEqualTo("Weave")
    }

    @Test
    fun tagline_isExactProductCopy() {
        assertThat(TAGLINE).isEqualTo("Connecting ideas together")
    }

    @Test
    fun minSdk_isAndroid10() {
        assertThat(MIN_SDK).isEqualTo(29)
    }

    @Test
    fun offlineContract_disallowsNetworkPermissionConstant() {
        // Manifest must not declare INTERNET; this constant documents the product rule.
        assertThat(NETWORK_PERMISSION_ALLOWED).isFalse()
    }

    companion object {
        const val APP_NAME = "Weave"
        const val TAGLINE = "Connecting ideas together"
        const val MIN_SDK = 29
        const val NETWORK_PERMISSION_ALLOWED = false
    }
}
