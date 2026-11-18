package com.matrix.launcher

import android.service.dreams.DreamService
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner

/** The system screensaver owns the same Compose screen and its lifecycle. */
open class ClockDream : DreamService(), LifecycleOwner, SavedStateRegistryOwner, ViewModelStoreOwner {
    private val registry = LifecycleRegistry(this)
    private val state = SavedStateRegistryController.create(this)
    override val lifecycle: Lifecycle get() = registry
    override val savedStateRegistry get() = state.savedStateRegistry
    override val viewModelStore = ViewModelStore()
    override fun onCreate() {
        super.onCreate(); state.performAttach(); state.performRestore(null)
        registry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    }
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        isInteractive = false; isFullscreen = true; isScreenBright = false
        setContentView(ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@ClockDream)
            setViewTreeSavedStateRegistryOwner(this@ClockDream)
            setViewTreeViewModelStoreOwner(this@ClockDream)
            setContent { MatrixTvTheme { MatrixClockScreen() } }
        })
    }
    override fun onDreamingStarted() { super.onDreamingStarted(); registry.handleLifecycleEvent(Lifecycle.Event.ON_START) }
    override fun onDreamingStopped() { registry.handleLifecycleEvent(Lifecycle.Event.ON_STOP); super.onDreamingStopped() }
    override fun onDestroy() { registry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY); viewModelStore.clear(); super.onDestroy() }
}
