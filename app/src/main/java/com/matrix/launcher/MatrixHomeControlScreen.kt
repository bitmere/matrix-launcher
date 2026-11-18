package com.matrix.launcher

import android.Manifest
import android.app.role.RoleManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Changes are opt-in. Preview launch never binds services, grants permissions or replaces Home. */
@Composable
internal fun MatrixHomeControlScreen(page: String, repository: ComposeHomeRepository, push: (String) -> Unit,
    back: () -> Unit, open: (Intent) -> Unit, notice: (String) -> Unit) {
    val context = LocalContext.current
    val prefs = repository.prefs
    var revision by remember { mutableIntStateOf(0) }
    fun mode(capture: Boolean) {
        prefs.edit { putBoolean("matrix_home_redirect", true); putBoolean("matrix_home_key_filter", capture) }
        HomeKeyService.updateHomeRedirect(); revision++
        notice("Enable this app's Home control in Accessibility and turn off other launcher overrides first.")
        open(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }
    val config by produceState<RemoteConfig?>(null, revision) {
        value = withContext(Dispatchers.IO) { RemoteConfig.load(context.applicationContext) }
    }
    if (config == null) { MatrixStatusPage("Home controls", "Reading remote configuration...", true, back); return }
    when (page) {
        "home-control" -> SettingsPage("Matrix Home setup", "Only enable one launcher's override. The accessibility shortcut toggle alone does not bind Home control.", listOf(
            SettingAction("Home control · " + if (HomeRecovery.bound(context)) "Active" else "Not running", "Enable Matrix Launcher · Home control in Accessibility", id = "home-status") { open(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
            SettingAction("Choose default Home", "Some Google TV firmware keeps its own launcher; Home capture is the fallback") {
                val roles = if (Build.VERSION.SDK_INT >= 29) context.getSystemService(RoleManager::class.java) else null
                open(if (Build.VERSION.SDK_INT >= 29 && roles != null && roles.isRoleAvailable(RoleManager.ROLE_HOME)) roles.createRequestRoleIntent(RoleManager.ROLE_HOME) else Intent(Settings.ACTION_HOME_SETTINGS))
            },
            SettingAction("Home redirect options", "Capture, window redirect and controller passthrough") { push("home-redirect") },
            SettingAction("Startup recovery", "Repairs the master switch while selected; manufacturer auto-start access may also be required", checked = HomeRecovery.enabled(context)) {
                if (!HomeRecovery.enabled(context) && context.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) != PackageManager.PERMISSION_GRANTED) push("home-recovery-permission")
                else { HomeRecovery.enable(context, !HomeRecovery.enabled(context)); HomeRecovery.repair(context); revision++ }
            },
            SettingAction("Remote mappings", "Configured app/Input/Settings buttons are honored by Home capture") { push("home-remote") },
            SettingAction("Search in header", "Display the search icon in the top header toolbar", checked = prefs.getBoolean("home_search_enabled", true)) {
                prefs.edit { putBoolean("home_search_enabled", !prefs.getBoolean("home_search_enabled", true)) }
                revision++
            }
        ), back)
        "home-redirect" -> SettingsPage("Home redirect", "Home key filtering may make held-volume buttons move one step per press on some TCL firmware.", listOf(
            SettingAction("Window-only redirect", "Normal key handling; rapid Home presses may escape to Google TV", MatrixControlRole.Action) { mode(false) },
            SettingAction("Capture Home key", "Consumes Home and supports configured remote mappings") { push("home-capture-confirm") },
            SettingAction("Stop Matrix redirect", "Does not alter other launchers' services", MatrixControlRole.Secondary) {
                prefs.edit { putBoolean("matrix_home_redirect", false) }; HomeRecovery.enable(context, false); HomeKeyService.updateHomeRedirect(); revision++; notice("Matrix redirect stopped.")
            },
            SettingAction("Controller passthrough", "Pause filtering while a controller is connected", checked = prefs.getBoolean("matrix_gamepad_passthrough", config?.gamepadPassthrough ?: false)) {
                prefs.edit { putBoolean("matrix_gamepad_passthrough", !prefs.getBoolean("matrix_gamepad_passthrough", config?.gamepadPassthrough ?: false)) }; HomeKeyService.updateHomeRedirect(); revision++
            }
        ), back)
        "home-capture-confirm" -> SettingsPage("Capture Home?", "Test the real Home key repeatedly, D-pad navigation and holding Volume Up after enabling. Use window-only mode if key repeats are more important on your firmware.",
            listOf(SettingAction("Set up Home capture", role = MatrixControlRole.Action) { mode(true) }), back)
        "home-recovery-permission" -> SettingsPage("Recovery permission", "Run once from a trusted computer:\n\nadb shell pm grant ${context.packageName} android.permission.WRITE_SECURE_SETTINGS\n\nThen return to Home setup and enable recovery. No notification access is required.", emptyList(), back)
        "home-remote" -> {
            SettingsPage("Remote & buttons", "Input and Settings requests use this Compose Home. Mappings use config.xml or the existing config.json theme overlay; unknown keys are ignored.", listOf(
                SettingAction("Input button", "${config?.inputKeycodes?.size ?: 0} configured keycodes · opens the same Inputs page as the toolbar") { open(HomeRequest.intent(context, HomeRequest.ACTION_VIEW_INPUTS)) },
                SettingAction("Settings button", "Open the shared quick-settings panel") { open(Intent(context, QuickSettingsActivity::class.java)) }
            ), back)
        }
    }
}
