package com.matrix.launcher

import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Monotonic revision of the shared-preferences state observed by [MatrixSettingsState]. */
internal data class MatrixSettingsSnapshot(val revision: Long)

/**
 * Exposes a [StateFlow] that bumps its [MatrixSettingsSnapshot.revision] whenever any
 * SharedPreferences value changes, so Compose can re-read settings without polling.
 *
 * The listener is intentionally never unregistered: the owning repository lives for the app
 * lifetime, same as before, so there is no lifecycle point at which unregistering would be
 * correct. No ViewModel, DataStore, Navigation or new dependency is involved.
 */
internal class MatrixSettingsState(private val prefs: SharedPreferences) {
    private val _snapshot = MutableStateFlow(MatrixSettingsSnapshot(0))
    val snapshot: StateFlow<MatrixSettingsSnapshot> = _snapshot.asStateFlow()

    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
        _snapshot.value = MatrixSettingsSnapshot(_snapshot.value.revision + 1)
    }

    init {
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    fun refresh() {
        _snapshot.value = MatrixSettingsSnapshot(_snapshot.value.revision + 1)
    }
}
