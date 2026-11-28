package com.matrix.launcher

import android.os.Build
import android.view.WindowManager
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import java.util.function.Consumer
import kotlin.math.roundToInt

internal val LocalMatrixSurfaceVisible = compositionLocalOf { true }
/** Enhancement only. MatrixPanel's frost/solid contrast floor never depends on this callback. */
@Composable
internal fun MatrixDialogBlur() {
    val view = LocalView.current
    val window = (view.parent as? DialogWindowProvider)?.window
    val tier = LocalMatrixCapabilities.current.material
    val radius = with(LocalDensity.current) { MatrixGlass.blurRadius.toPx().roundToInt() }
    DisposableEffect(window, tier, radius) {
        if (Build.VERSION.SDK_INT >= 31 && window != null) {
            val manager = view.context.getSystemService(WindowManager::class.java)
            val originalRadius = window.attributes.blurBehindRadius
            val originalFlag = window.attributes.flags and WindowManager.LayoutParams.FLAG_BLUR_BEHIND
            var disposed = false
            fun apply(supported: Boolean) {
                if (disposed) return
                val enabled = dialogBlurEnabled(tier, supported)
                window.attributes = window.attributes.apply { blurBehindRadius = if (enabled) radius else 0 }
                if (enabled) window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                else window.clearFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
            }
            val listener = Consumer<Boolean> { supported -> apply(supported) }
            var registered = false
            try {
                apply(manager.isCrossWindowBlurEnabled)
                manager.addCrossWindowBlurEnabledListener(view.context.mainExecutor, listener)
                registered = true
            } catch (_: RuntimeException) { apply(false) }
            onDispose {
                disposed = true
                if (registered) manager.removeCrossWindowBlurEnabledListener(listener)
                window.attributes = window.attributes.apply { blurBehindRadius = originalRadius }
                if (originalFlag != 0) window.addFlags(originalFlag)
                else window.clearFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
            }
        } else onDispose { }
    }
}
