package com.matrix.launcher

/**
 * Temporary diagnostic for bug 2 (2026-11-22): a size pick only shows its
 * checkmark after leaving the page and coming back. The static update chain
 * reads correct end to end, so this traces the chain at runtime instead of
 * guessing a fix: persisted value written → listener revision bump →
 * parent revision → `def.build` result → built menu's selected flags.
 *
 * ONE flag, ONE file: flip [enabled] to true only in the diagnostic build,
 * run one size press on the TV, and pull `adb logcat -s MatrixSettingsDiag`
 * (or a bug report taken with the size page on screen). Delete this file
 * and the `// DIAG-BUG2` call sites to remove the diagnostic completely.
 * Behaviour is unchanged when [enabled] is false (the default).
 */
internal object MatrixSettingsDiag {
    /** Flip to true only in the diagnostic build. Never true in a release. */
    var enabled = false

    private const val TAG = "MatrixSettingsDiag"

    /** The detail string is lazy: nothing is built or read while disabled. */
    fun log(event: String, detail: () -> String) {
        if (enabled) android.util.Log.d(TAG, "$event :: ${detail()}")
    }

    /** One-line call site for the built-menu checkpoint (see MatrixSettingsPage). */
    fun logMenuBuilt(pageId: String, content: SettingsPageContent) {
        if (content is SettingsPageContent.Menu) log("menu-built") {
            "page=$pageId selected=${content.entries.mapIndexedNotNull { i, e -> i.takeIf { e.selected == true } }} " +
                "checked=${content.entries.mapIndexedNotNull { i, e -> i.takeIf { e.checked == true } }}"
        }
    }
}
