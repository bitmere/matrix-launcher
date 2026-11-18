package com.matrix.launcher

/** Own the ID from creation through commit, including failure to open the session. */
internal interface InstallSessionAdapter {
    fun create(): Int
    fun open(id: Int)
    fun writeAndSync()
    fun commit(id: Int)
    fun abandon(id: Int)
    fun close()
}

internal fun commitInstallSession(adapter: InstallSessionAdapter): Int {
    val id = adapter.create()
    try {
        adapter.open(id)
        adapter.writeAndSync()
        adapter.commit(id)
        return id
    } catch (failure: Exception) {
        runCatching { adapter.abandon(id) }
        throw failure
    } finally { runCatching { adapter.close() } }
}
