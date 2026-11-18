package com.matrix.launcher

import android.os.Bundle
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type

class ClockActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        androidx.core.view.WindowCompat.getInsetsController(window, window.decorView).hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
        setContent { MatrixTvTheme {
            val focus = remember { FocusRequester() }
            Box(Modifier.fillMaxSize().onPreviewKeyEvent {
                if (it.type == KeyEventType.KeyUp) finish(); true
            }.focusRequester(focus).focusable()) { MatrixClockScreen() }
            LaunchedEffect(Unit) { focus.requestFocus() }
        } }
    }
    override fun onPause() { super.onPause(); finish() }
    override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean { finish(); return true }
}
