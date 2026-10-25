package com.weave.notebook.perf

/**
 * LRU tile/cache budget helpers. Device profiling remains NOT RUN without tablet.
 */
class RenderCacheBudget(
    val maxBytes: Long = 32L * 1024L * 1024L
) {
    private data class Entry(val key: String, val bytes: Long)
    private val order = LinkedHashMap<String, Entry>(16, 0.75f, true)
    var usedBytes: Long = 0
        private set

    fun put(key: String, bytes: Long) {
        require(bytes >= 0)
        order.remove(key)?.let { usedBytes -= it.bytes }
        order[key] = Entry(key, bytes)
        usedBytes += bytes
        evict()
    }

    fun get(key: String): Long? = order[key]?.bytes

    fun contains(key: String): Boolean = order.containsKey(key)

    private fun evict() {
        val it = order.entries.iterator()
        while (usedBytes > maxBytes && it.hasNext()) {
            val e = it.next()
            usedBytes -= e.value.bytes
            it.remove()
        }
    }
}

data class PerfBudgetNotes(
    val inkMainThreadP95Ms: Double? = null,
    val measuredOnDevice: Boolean = false,
    val notes: String = "Device profiling NOT RUN — budgets are targets only."
)
