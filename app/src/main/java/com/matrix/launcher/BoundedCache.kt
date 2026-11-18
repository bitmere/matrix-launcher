package com.matrix.launcher

/** A small access-ordered LRU map that evicts the eldest entry on write once
 * past [maxSize]. Callers must synchronize on the instance: unlike
 * ConcurrentHashMap, LinkedHashMap is not thread-safe. Used for the small
 * in-memory caches (web search, AI per-search memos) so none of them can
 * grow for the life of the process. */
internal class BoundedCache<K, V>(private val maxSize: Int) : LinkedHashMap<K, V>(maxSize, .75f, true) {
    override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, V>?): Boolean = size > maxSize
}
