package com.matrix.launcher

/** Completed authentication must not leave editable setup/QR screens in Back history. */
internal fun connectedAccountHistory(history: List<String>): List<String> =
    history.dropLastWhile { it == "simkl" || it.startsWith("simkl-") } + "simkl"
