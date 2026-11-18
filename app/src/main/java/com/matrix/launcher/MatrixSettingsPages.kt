package com.matrix.launcher

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.core.content.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Data-driven home settings pages (P1/N4).
 *
 * The page ids, titles, descriptions, defaults and writes here are exactly
 * the ones that used to be inline in MatrixHomeApp's `when (page)` in
 * MatrixHomeScreen.kt; only the location changed. MatrixHomeApp dispatches
 * these ids through [matrixHomeSettingsPage] / [MatrixSettingsPage] and keeps
 * every other branch inline.
 *
 * Pages in this registry: "settings", "appearance", "idle-clock", "channels",
 * "limits", "size", "about", "theme", "backup".
 *
 * Pages deliberately left inline in MatrixHomeScreen.kt:
 * - "aot-control": owns LaunchedEffect state (DEX status / compiling) and its
 *   status rows; forcing it into this registry would hide that state.
 * - "data-credits" / "licenses": delegate to other screens (MatrixDataCredits
 *   / MatrixLicenseScreen), not SettingsPage/MatrixMenu definitions.
 * - "apps" / "favorites" / "sources" / "app-options" / "order" / "list" /
 *   "inputs" / "search" / "integrations" / "simkl…" / "wallpaper…" /
 *   "home-…": not home settings pages.
 *
 * Titles/descriptions come from string resources. [SettingsPageDef.descriptionRes]
 * is 0 when a page has no static description: "settings" has none, and
 * "channels" / "about" compute theirs in [SettingsPageDef.build] (listings
 * permission / app version). Dynamic row text (idle-clock minutes, per-app
 * card counts) is likewise built in [SettingsPageDef.build], which is a
 * @Composable lambda so it can call stringResource.
 */
internal class MatrixSettingsPageScope(
    val repository: ComposeHomeRepository,
    val snapshot: HomeSnapshot,
    val loading: Boolean,
    val reloadStatus: String,
    val listingsAllowed: Boolean,
    val allowListings: () -> Unit,
    val push: (String) -> Unit,
    val back: () -> Unit,
    val refresh: (Boolean) -> Unit,
    val open: (Intent) -> Unit,
    val openOrderPage: (OrderPage) -> Unit,
    val context: Context,
    /**
     * Everything below has a default, so the scope can be built by
     * MatrixHomeScreen.kt without passing them: notices toast on the settings
     * context, recreate restarts the hosting activity, and the SAF calls
     * delegate to the launchers ComposeHomeActivity registers in onCreate
     * (see MatrixSaf). A caller may still pass explicit callbacks.
     */
    val notice: (String) -> Unit = { message ->
        android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show()
    },
    val recreate: () -> Unit = { (context as? android.app.Activity)?.recreate() },
    val openDocument: (mimeTypes: Array<String>, onResult: (android.net.Uri?) -> Unit) -> Unit =
        { mimeTypes, onResult -> MatrixSaf.openDocument?.invoke(mimeTypes, onResult) },
    val createDocument: (fileName: String, mimeType: String, onResult: (android.net.Uri?) -> Unit) -> Unit =
        { fileName, mimeType, onResult -> MatrixSaf.createDocument?.invoke(fileName, mimeType, onResult) }
)

/** What a registry page renders: a [SettingsPage] action list or a [MatrixMenu]. */
internal sealed interface SettingsPageContent {
    /** Null means "use the def's [SettingsPageDef.descriptionRes]" (or "" when 0). */
    val description: String?

    data class Actions(
        val actions: List<SettingAction>,
        override val description: String? = null
    ) : SettingsPageContent

    data class Reader(val title: String, val text: String, val back: () -> Unit) : SettingsPageContent {
        override val description: String? = null
    }

    data class Menu(
        val entries: List<MatrixMenuItem>,
        override val description: String? = null,
        val onSelect: (Int) -> Unit
    ) : SettingsPageContent
}

internal data class SettingsPageDef(
    val id: String,
    val titleRes: Int,
    /** Static description resource, or 0 when there is none / it is dynamic (see [SettingsPageContent.description]). */
    val descriptionRes: Int,
    val build: @Composable (MatrixSettingsPageScope) -> SettingsPageContent
)

internal val matrixHomeSettingsPages: List<SettingsPageDef> = listOf(
    SettingsPageDef("settings", R.string.settings_home_title, 0) { scope ->
        val safeMode = scope.repository.safeMode
        SettingsPageContent.Actions(buildList {
            add(SettingAction(stringResource(R.string.settings_home_edit_favorites_title), stringResource(R.string.settings_home_edit_favorites_detail)) { scope.push("favorites") })
            add(SettingAction(stringResource(R.string.settings_home_channels_title), stringResource(R.string.settings_home_channels_detail)) { scope.push("channels") })
            add(SettingAction(stringResource(R.string.settings_home_appearance_title), stringResource(R.string.settings_home_appearance_detail)) { scope.push("appearance") })
            add(SettingAction(if (scope.loading) stringResource(R.string.settings_home_reloading_title) else stringResource(R.string.settings_home_reload_title),
                scope.reloadStatus.ifBlank { stringResource(R.string.settings_home_reload_detail) }, id = "reload") { if (!scope.loading) scope.refresh(true) })
            add(SettingAction(stringResource(R.string.settings_home_wallpapers_title), stringResource(R.string.settings_home_wallpapers_detail)) { scope.push("wallpapers") })
            add(SettingAction(stringResource(R.string.settings_home_integrations_title), stringResource(R.string.settings_home_integrations_detail)) { scope.push("integrations") })
            add(SettingAction(stringResource(R.string.settings_home_remote_title), stringResource(R.string.settings_home_remote_detail)) { scope.push("home-control") })
            add(SettingAction(stringResource(R.string.settings_home_aot_title), stringResource(R.string.settings_home_aot_detail)) { scope.push("aot-control") })
            if (safeMode) {
                add(SettingAction(stringResource(R.string.settings_safe_mode_active_title),
                    stringResource(R.string.settings_safe_mode_active_detail), isStatus = true))
                add(SettingAction(stringResource(R.string.settings_safe_mode_exit_title),
                    stringResource(R.string.settings_safe_mode_exit_detail), role = MatrixControlRole.Action) {
                    MatrixSafeMode.setEnabled(scope.repository.prefs, false)
                    scope.notice(scope.context.getString(R.string.settings_safe_mode_off))
                    scope.recreate()
                })
            }
            add(SettingAction(stringResource(R.string.settings_safe_mode_title),
                stringResource(R.string.settings_safe_mode_detail), checked = safeMode) {
                val next = !scope.repository.safeMode
                MatrixSafeMode.setEnabled(scope.repository.prefs, next)
                scope.notice(scope.context.getString(if (next) R.string.settings_safe_mode_on else R.string.settings_safe_mode_off))
                scope.recreate()
            })
            add(SettingAction(stringResource(R.string.settings_backup_title), stringResource(R.string.settings_backup_detail)) { scope.push("backup") })
            add(SettingAction(stringResource(R.string.settings_home_about_title), stringResource(R.string.settings_home_about_detail)) { scope.push("about") })
            add(SettingAction(stringResource(R.string.settings_home_tv_settings_title), stringResource(R.string.settings_home_tv_settings_detail)) { scope.open(SystemSettings.intent(scope.context, Settings.ACTION_SETTINGS)) })
        })
    },
    SettingsPageDef("appearance", R.string.settings_home_appearance_title, R.string.settings_appearance_description) { scope ->
        val idleMinutes = scope.repository.prefs.getInt(MatrixPrefs.IDLE_CLOCK_MINUTES, MatrixPrefs.IDLE_CLOCK_DEFAULT)
        SettingsPageContent.Actions(listOf(
            SettingAction(stringResource(R.string.settings_size_title), stringResource(R.string.settings_appearance_card_size_detail)) { scope.push("size") },
            SettingAction(stringResource(R.string.settings_theme_title), stringResource(R.string.settings_theme_detail)) { scope.push("theme") },
            SettingAction(stringResource(R.string.settings_home_wallpapers_title), stringResource(R.string.settings_appearance_wallpapers_detail)) { scope.push("wallpapers") },
            SettingAction(if (idleMinutes == 0) stringResource(R.string.settings_idle_clock_title_off) else pluralStringResource(R.plurals.settings_idle_clock_title_minutes, idleMinutes, idleMinutes),
                stringResource(R.string.settings_appearance_idle_clock_detail), id = "idle-clock") { scope.push("idle-clock") }
        ))
    },
    SettingsPageDef("idle-clock", R.string.settings_idle_clock_title, R.string.settings_idle_clock_description) { scope ->
        SettingsPageContent.Actions(listOf(0, 5, 10, 15, 30).map { minutes ->
            SettingAction(if (minutes == 0) stringResource(R.string.settings_idle_clock_off) else pluralStringResource(R.plurals.settings_idle_clock_minutes, minutes, minutes),
                selected = scope.repository.prefs.getInt(MatrixPrefs.IDLE_CLOCK_MINUTES, MatrixPrefs.IDLE_CLOCK_DEFAULT) == minutes) {
                scope.repository.prefs.edit { putInt(MatrixPrefs.IDLE_CLOCK_MINUTES, minutes) }; scope.back()
            }
        })
    },
    SettingsPageDef("channels", R.string.settings_home_channels_title, 0) { scope ->
        val orderPage = OrderPage(stringResource(R.string.settings_channels_order_title), MatrixPrefs.ROW_ORDER,
            HomeRows.sorted(scope.snapshot.allRows, HomeRows.order(scope.repository.prefs, MatrixPrefs.ROW_ORDER)) { it.id }.map { row ->
                OrderEntry(row.id, row.title,
                    app = if (row.id.startsWith("simkl-")) null else scope.snapshot.apps.find { app -> row.cards.firstOrNull()?.source == app.id },
                    icon = if (row.id.startsWith("simkl-")) R.drawable.ic_matrix_list else null)
            }, visibility = true)
        SettingsPageContent.Actions(
            buildList {
                add(SettingAction(stringResource(R.string.settings_channels_choose_apps_title), stringResource(R.string.settings_channels_choose_apps_detail)) { scope.push("sources") })
                add(SettingAction(stringResource(R.string.settings_channels_rows_order_title), stringResource(R.string.settings_channels_rows_order_detail)) {
                    scope.openOrderPage(orderPage)
                    scope.push("order")
                })
                if (!scope.listingsAllowed) add(SettingAction(stringResource(R.string.settings_channels_allow_listings_title), stringResource(R.string.settings_channels_allow_listings_detail)) { scope.allowListings() })
                add(SettingAction(stringResource(R.string.settings_limits_title), stringResource(R.string.settings_channels_cards_per_app_detail)) { scope.push("limits") })
            },
            description = if (scope.listingsAllowed) stringResource(R.string.settings_channels_description_allowed)
                else stringResource(R.string.settings_channels_description_required)
        )
    },
    SettingsPageDef("limits", R.string.settings_limits_title, 0) { scope ->
        val sources = scope.repository.prefs.getStringSet(MatrixPrefs.SOURCES, emptySet()).orEmpty()
        val sourceApps = scope.snapshot.apps.filter { it.id in sources }
        SettingsPageContent.Menu(
            entries = sourceApps.map {
                val limit = scope.repository.prefs.getInt(MatrixPrefs.rowLimit(it.id), MatrixPrefs.ROW_LIMIT_DEFAULT)
                MatrixMenuItem(it.title, pluralStringResource(R.plurals.settings_limits_detail, limit, limit), id = it.id)
            },
            onSelect = { index ->
                val app = sourceApps[index]
                val old = scope.repository.prefs.getInt(MatrixPrefs.rowLimit(app.id), MatrixPrefs.ROW_LIMIT_DEFAULT)
                scope.repository.prefs.edit { putInt(MatrixPrefs.rowLimit(app.id), if (old >= MatrixPrefs.ROW_LIMIT_DEFAULT) 4 else old + 4) }
                scope.refresh(false)
            }
        )
    },
    SettingsPageDef("size", R.string.settings_size_title, 0) { scope ->
        val sizes = listOf(
            stringResource(R.string.settings_size_large) to stringResource(R.string.settings_size_large_detail),
            stringResource(R.string.settings_size_medium) to stringResource(R.string.settings_size_medium_detail),
            stringResource(R.string.settings_size_compact) to stringResource(R.string.settings_size_compact_detail),
            stringResource(R.string.settings_size_small) to stringResource(R.string.settings_size_small_detail)
        )
        SettingsPageContent.Menu(
            entries = sizes.menuItems().mapIndexed { index, item ->
                // In safe mode the default column count is in effect, not the saved one.
                item.copy(selected = (if (scope.repository.safeMode) MatrixPrefs.LIST_COLUMNS_DEFAULT else MatrixSafeMode.effectiveColumns(scope.repository.prefs)) == index + 3)
            },
            onSelect = {
                scope.repository.prefs.edit { putInt(MatrixPrefs.LIST_COLUMNS, it + 3) }
                scope.refresh(false)
            }
        )
    },
    SettingsPageDef("about", R.string.settings_about_title, 0) { scope ->
        val ioScope = rememberCoroutineScope()
        val installedVersion by produceState("", scope.context) {
            value = withContext(Dispatchers.IO) { UpdateCheck.installedVersionName(scope.context) }
        }
        val updateState = remember { UpdateCheckController(scope.context, ioScope) }
        DisposableEffect(Unit) { onDispose { updateState.dispose() } }
        val owner = androidx.lifecycle.compose.LocalLifecycleOwner.current
        DisposableEffect(owner, updateState) {
            val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) updateState.onResume()
            }
            owner.lifecycle.addObserver(observer)
            onDispose { owner.lifecycle.removeObserver(observer) }
        }
        var readingNotes by remember { mutableStateOf<LatestRelease?>(null) }
        val updateUi = updateState.uiState
        // The update row's title, detail and action follow the one UpdateUiState;
        // the row count stays stable (one action row + an optional notes row)
        // so D-pad focus never jumps when the state changes.
        val (updateTitle, updateDetail, _) = when (val state = updateUi) {
            UpdateUiState.Idle -> Triple(
                stringResource(R.string.settings_about_update_title),
                stringResource(R.string.settings_about_update_detail), false
            )
            UpdateUiState.Checking -> Triple(
                stringResource(R.string.settings_about_update_title),
                stringResource(R.string.settings_about_update_checking), true
            )
            is UpdateUiState.UpToDate -> Triple(
                stringResource(R.string.settings_about_update_title),
                stringResource(R.string.settings_about_update_up_to_date, state.installedVersion), false
            )
            is UpdateUiState.Available -> Triple(
                stringResource(R.string.settings_about_update_download, state.release.versionName),
                stringResource(R.string.settings_about_update_available, state.release.versionName), false
            )
            is UpdateUiState.Downloading -> Triple(
                stringResource(R.string.settings_about_update_cancel),
                if (state.total != null && state.total > 0) {
                    stringResource(
                        R.string.settings_about_update_downloading,
                        (state.downloaded * 100 / state.total).toInt()
                    )
                } else {
                    stringResource(R.string.settings_about_update_downloading_unknown)
                }, false
            )
            is UpdateUiState.ReadyToInstall -> Triple(
                stringResource(R.string.settings_about_update_install),
                stringResource(R.string.settings_about_update_ready, state.release.versionName), false
            )
            UpdateUiState.Installing -> Triple(
                stringResource(R.string.settings_about_update_install),
                stringResource(R.string.settings_about_update_installing), true
            )
            is UpdateUiState.WaitingForConfirmation -> Triple(
                stringResource(R.string.settings_about_update_install),
                stringResource(R.string.settings_about_update_waiting), false
            )
            is UpdateUiState.Installed -> Triple(
                stringResource(R.string.settings_about_update_title),
                stringResource(R.string.settings_about_update_installed, state.versionName), false
            )
            is UpdateUiState.NeedsUnknownSources -> Triple(
                stringResource(R.string.settings_about_update_open_settings),
                stringResource(R.string.settings_about_update_needs_sources), false
            )
            is UpdateUiState.Error -> Triple(
                stringResource(R.string.settings_about_update_retry),
                state.message, false
            )
        }
        val releaseForNotes = when (val state = updateUi) {
            is UpdateUiState.Available -> state.release
            is UpdateUiState.Downloading -> state.release
            is UpdateUiState.ReadyToInstall -> state.release
            is UpdateUiState.NeedsUnknownSources -> state.release
            is UpdateUiState.WaitingForConfirmation -> state.release
            else -> null
        }
        val notes = readingNotes
        if (notes != null) {
            SettingsPageContent.Reader(
                stringResource(R.string.settings_about_update_notes_title, notes.versionName),
                notes.notes
            ) { readingNotes = null }
        } else SettingsPageContent.Actions(
            buildList {
                add(SettingAction(stringResource(R.string.settings_about_data_sources_title), stringResource(R.string.settings_about_data_sources_detail)) { scope.push("data-credits") })
                add(SettingAction(stringResource(R.string.settings_about_licenses_title)) { scope.push("licenses") })
                add(SettingAction(updateTitle, updateDetail, id = "manual-update") { updateState.onRowAction() })
                if (releaseForNotes != null && releaseForNotes.notes.isNotBlank()) {
                    add(SettingAction(
                        stringResource(R.string.settings_about_update_notes_title, releaseForNotes.versionName),
                        id = "manual-update-notes"
                    ) { readingNotes = releaseForNotes })
                }
            },
            description = stringResource(R.string.settings_about_description,
                installedVersion)
        )
    },
    SettingsPageDef("theme", R.string.settings_theme_title, R.string.settings_theme_description) { scope ->
        val theme = MatrixLayout.theme
        val ioScope = rememberCoroutineScope()
        var themeError by remember { mutableStateOf("") }
        fun applyLoaded(state: ThemeState) {
            if (state.ignoredReason != null) {
                themeError = state.ignoredReason
                return
            }
            themeError = ""
            MatrixTheme.apply(state)
            // The tileSize token is the LIST_COLUMNS value; Apply makes it live.
            scope.repository.prefs.edit { putInt(MatrixPrefs.LIST_COLUMNS, state.tileSize) }
            scope.recreate()
        }
        fun reloadAndApply() {
            ioScope.launch(Dispatchers.IO) {
                val loaded = MatrixTheme.load(scope.context.filesDir)
                withContext(Dispatchers.Main) { applyLoaded(loaded) }
            }
        }
        /** Tap a token row: advance its value, save theme.json, apply, restart Home. */
        fun applyEdited(newState: ThemeState) {
            ioScope.launch(Dispatchers.IO) {
                val saved = runCatching { MatrixTheme.save(scope.context.filesDir, newState) }.isSuccess
                withContext(Dispatchers.Main) {
                    if (!saved) { themeError = scope.context.getString(R.string.settings_theme_import_failed); return@withContext }
                    MatrixTheme.apply(newState)
                    // The tileSize token is the LIST_COLUMNS value; Apply makes it live.
                    scope.repository.prefs.edit { putInt(MatrixPrefs.LIST_COLUMNS, newState.tileSize) }
                    scope.recreate()
                }
            }
        }
        val ignoredReason = theme.ignoredReason ?: themeError.takeIf { it.isNotBlank() }
        SettingsPageContent.Actions(buildList {
            if (ignoredReason != null) {
                add(SettingAction(stringResource(R.string.settings_theme_ignored_title),
                    ignoredReason, isStatus = true))
            }
            val tokenRows = listOf(
                Triple("background", R.string.settings_theme_token_background, MatrixTheme.formatColor(theme.background)),
                Triple("panel", R.string.settings_theme_token_panel, MatrixTheme.formatColor(theme.panel)),
                Triple("surface", R.string.settings_theme_token_surface, MatrixTheme.formatColor(theme.surface)),
                Triple("text", R.string.settings_theme_token_text, MatrixTheme.formatColor(theme.text)),
                Triple("textSecondary", R.string.settings_theme_token_text_secondary, MatrixTheme.formatColor(theme.textSecondary)),
                Triple("accent", R.string.settings_theme_token_accent, MatrixTheme.formatColor(theme.accent)),
                Triple("radius", R.string.settings_theme_token_radius, stringResource(R.string.settings_theme_value_dp, theme.radiusDp)),
                Triple("gap", R.string.settings_theme_token_gap, stringResource(R.string.settings_theme_value_dp, theme.gapDp)),
                Triple("focusScale", R.string.settings_theme_token_focus_scale, stringResource(R.string.settings_theme_value_scale, theme.focusScale)),
                Triple("tileSize", R.string.settings_theme_token_tile_size, pluralStringResource(R.plurals.settings_theme_value_columns, theme.tileSize, theme.tileSize)),
            )
            for ((token, titleRes, detail) in tokenRows) {
                add(SettingAction(stringResource(titleRes), detail) {
                    applyEdited(MatrixTheme.nextState(theme, token))
                })
            }
            add(SettingAction(stringResource(R.string.settings_theme_apply_title),
                stringResource(R.string.settings_theme_apply_detail), role = MatrixControlRole.Action) { reloadAndApply() })
            add(SettingAction(stringResource(R.string.settings_theme_import_title),
                stringResource(R.string.settings_theme_import_detail), role = MatrixControlRole.Action) {
                scope.openDocument(arrayOf(MatrixBackup.MIME_TYPE)) { uri ->
                    if (uri == null) return@openDocument
                    ioScope.launch(Dispatchers.IO) {
                        val copied = runCatching {
                            scope.context.contentResolver.openInputStream(uri)?.use { input ->
                                MatrixTheme.import(scope.context.filesDir, input)
                            } ?: throw IllegalStateException("No input stream")
                        }
                        withContext(Dispatchers.Main) {
                            if (copied.isFailure) {
                                themeError = scope.context.getString(R.string.settings_theme_import_failed)
                            } else reloadAndApply()
                        }
                    }
                }
            })
            add(SettingAction(stringResource(R.string.settings_theme_reset_title),
                stringResource(R.string.settings_theme_reset_detail), role = MatrixControlRole.Action) {
                ioScope.launch(Dispatchers.IO) {
                    java.io.File(scope.context.filesDir, THEME_FILE_NAME).delete()
                    withContext(Dispatchers.Main) {
                        MatrixTheme.apply(ThemeState.defaults())
                        scope.recreate()
                    }
                }
            })
        })
    },
    SettingsPageDef("backup", R.string.settings_backup_title, R.string.settings_backup_description) { scope ->
        val ioScope = rememberCoroutineScope()
        val prefs = scope.repository.prefs
        val exportable = MatrixBackup.exportPrefs(prefs)
        val hasTheme by produceState(false, scope.context) {
            value = withContext(Dispatchers.IO) { MatrixBackup.readThemeJson(scope.context.filesDir) != null }
        }
        SettingsPageContent.Actions(listOf(
            SettingAction(stringResource(R.string.settings_backup_contents_title),
                pluralStringResource(R.plurals.settings_backup_contents_detail, exportable.size, exportable.size,
                    stringResource(if (hasTheme) R.string.settings_backup_theme_included
                        else R.string.settings_backup_theme_missing)),
                isStatus = true),
            SettingAction(stringResource(R.string.settings_backup_export_title),
                stringResource(R.string.settings_backup_export_detail), role = MatrixControlRole.Action) {
                scope.createDocument(MatrixBackup.defaultFileName(), MatrixBackup.MIME_TYPE) { uri ->
                    if (uri == null) return@createDocument
                    ioScope.launch(Dispatchers.IO) {
                        val ok = runCatching {
                            val json = MatrixBackup.encode(exportable, MatrixBackup.readThemeJson(scope.context.filesDir))
                            scope.context.contentResolver.openOutputStream(uri)?.use {
                                it.write(json.toByteArray(Charsets.UTF_8))
                            } ?: throw IllegalStateException("No output stream")
                        }.isSuccess
                        withContext(Dispatchers.Main) {
                            scope.notice(scope.context.getString(if (ok) R.string.settings_backup_exported
                                else R.string.settings_backup_export_failed))
                        }
                    }
                }
            },
            SettingAction(stringResource(R.string.settings_backup_import_title),
                stringResource(R.string.settings_backup_import_detail), role = MatrixControlRole.Action) {
                scope.openDocument(arrayOf(MatrixBackup.MIME_TYPE)) { uri ->
                    if (uri == null) return@openDocument
                    ioScope.launch(Dispatchers.IO) {
                        val text = runCatching {
                            scope.context.contentResolver.openInputStream(uri)
                                ?.use { BoundedInput.readText(it, MatrixBackup.MAX_BYTES) }
                        }.getOrNull()
                        val data = text?.let { runCatching { MatrixBackup.decode(it).getOrThrow() }.getOrNull() }
                        // Apply on IO, not Main: it writes theme.json to disk.
                        val restore = data?.let { runCatching { MatrixBackup.apply(scope.context, prefs, it) } }
                        val count = restore?.getOrNull()
                        withContext(Dispatchers.Main) {
                            if (data == null || count == null) {
                                scope.notice(scope.context.getString(if (restore?.exceptionOrNull() is BackupCommitException)
                                    R.string.settings_backup_partial_failure else R.string.settings_backup_import_failed))
                            } else {
                                scope.notice(scope.context.resources.getQuantityString(R.plurals.settings_backup_imported, count, count))
                                scope.recreate()
                            }
                        }
                    }
                }
            }
        ))
    }
)

internal fun matrixHomeSettingsPage(id: String): SettingsPageDef? =
    matrixHomeSettingsPages.firstOrNull { it.id == id }

@Composable
internal fun MatrixSettingsPage(def: SettingsPageDef, scope: MatrixSettingsPageScope) {
    DisposableEffect(def.id) {
        val cancelPending = MatrixSaf.cancelPending
        onDispose { cancelPending?.invoke() }
    }
    val content = def.build(scope)
    val title = stringResource(def.titleRes)
    val description = content.description
        ?: if (def.descriptionRes != 0) stringResource(def.descriptionRes) else ""
    when (content) {
        is SettingsPageContent.Reader -> MatrixReleaseNotesScreen(content.title, content.text, content.back)
        is SettingsPageContent.Actions -> SettingsPage(title, description, content.actions, scope.back)
        is SettingsPageContent.Menu -> MatrixMenu(title, content.entries, scope.back, description, content.onSelect)
    }
}
