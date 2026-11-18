package com.matrix.launcher

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/** Same shared settings rows as Home, over the current app in a separate translucent task. */
class QuickSettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MatrixTvTheme {
            BackHandler { finish() }
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .35f)), contentAlignment = Alignment.CenterEnd) {
                MatrixPanel {
                    SettingsPage("Quick settings", "Back returns to the current app.", listOf(
                        SettingAction("TV settings") { open(SystemSettings.intent(this@QuickSettingsActivity, Settings.ACTION_SETTINGS)) },
                        SettingAction("Picture") { open(SystemSettings.intent(this@QuickSettingsActivity, Settings.ACTION_DISPLAY_SETTINGS)) },
                        SettingAction("Network") { open(SystemSettings.intent(this@QuickSettingsActivity, Settings.ACTION_WIFI_SETTINGS)) },
                        SettingAction("Apps") { open(SystemSettings.intent(this@QuickSettingsActivity, Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS)) },
                        SettingAction("Matrix settings") { open(HomeRequest.intent(this@QuickSettingsActivity, HomeRequest.ACTION_LAUNCHER_SETTINGS)) }
                    ), ::finish)
                }
            }
        } }
    }
    private fun open(intent: Intent) {
        try { startActivity(intent) }
        catch (failure: android.content.ActivityNotFoundException) { Toast.makeText(this, "Unavailable on this TV", Toast.LENGTH_SHORT).show() }
        catch (failure: SecurityException) { Toast.makeText(this, "Not permitted by this TV", Toast.LENGTH_SHORT).show() }
        finish()
    }
    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); finish() }
    override fun onPause() { super.onPause(); finish() }
}
