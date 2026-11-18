package com.matrix.launcher

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Rect
import androidx.compose.foundation.lazy.LazyListState

/** ADB-only logical diagnostics: no titles, account identifiers, media links or credentials. */
internal class MatrixUiInspection {
    var page = "home"
        set(value) { if (field != value) { focusedOption = -1; focusedState = "none" }; field = value }
    var focusedOption = -1
    var focusedState = "none"
    val icons = mutableMapOf<String, MatrixIconGeometry>()
    val rows = mutableMapOf<String, MatrixRowGeometry>()
    var homeRow = -1
    var homeCard = -1
    var homeColumn: LazyListState? = null
    var deferredRepeats = 0
    var navigationDowns = 0
    var navigationRepeats = 0
    var searchProvider = "none"
    var searchResults = 0
    var searchBusy = false
    fun verticalSummary(): String {
        val state = homeColumn ?: return "pending"
        val info = state.layoutInfo
        return "first=${state.firstVisibleItemIndex} offset=${state.firstVisibleItemScrollOffset} last=${info.visibleItemsInfo.lastOrNull()?.index ?: -1} total=${info.totalItemsCount} scrolling=${state.isScrollInProgress} downs=$navigationDowns repeats=$navigationRepeats deferred=$deferredRepeats"
    }
}
internal val LocalMatrixUiInspection = staticCompositionLocalOf<MatrixUiInspection?> { null }

/** Read layout only when requested by ADB; no per-frame Compose state or media identifiers. */
internal class MatrixRowGeometry(private val state: LazyListState, private val columns: Int, private val width: Int) {
    fun summary(): String {
        val info = state.layoutInfo
        val end = info.viewportEndOffset - info.afterContentPadding
        val visible = info.visibleItemsInfo.filter { it.offset + it.size > 0 && it.offset < end }
        val partial = visible.count { it.offset < -1 || it.offset + it.size > end + 1 }
        return "slots=$columns width=$width viewport=${info.viewportSize.width} first=${visible.firstOrNull()?.index ?: -1} last=${visible.lastOrNull()?.index ?: -1} partial=$partial scrolling=${state.isScrollInProgress}"
    }
}

internal class MatrixIconGeometry {
    var button: Rect? = null
    var icon: Rect? = null
    fun summary(): String {
        val frame = button ?: return "pending"
        val glyph = icon ?: return "pending"
        return "button=${frame.width}x${frame.height} icon=${glyph.width}x${glyph.height} offset=${glyph.center.x-frame.center.x},${glyph.center.y-frame.center.y}"
    }
}
