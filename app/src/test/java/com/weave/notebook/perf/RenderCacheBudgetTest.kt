package com.weave.notebook.perf

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class RenderCacheBudgetTest {
    @Test
    fun evictsOldestWhenOverBudget() {
        val cache = RenderCacheBudget(maxBytes = 100)
        cache.put("a", 40)
        cache.put("b", 40)
        cache.put("c", 40)
        assertThat(cache.usedBytes).isAtMost(100)
        assertThat(cache.contains("a")).isFalse()
        assertThat(cache.contains("c")).isTrue()
    }
}
