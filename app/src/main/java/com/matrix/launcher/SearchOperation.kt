package com.matrix.launcher

/** Workers may publish only while their operation is current. UI calls begin/invalidate on Main. */
internal class SearchOperation {
    private val generation = java.util.concurrent.atomic.AtomicLong()
    fun begin(): Long = generation.incrementAndGet()
    fun isCurrent(id: Long): Boolean = generation.get() == id
    fun invalidate() { generation.incrementAndGet() }
}
