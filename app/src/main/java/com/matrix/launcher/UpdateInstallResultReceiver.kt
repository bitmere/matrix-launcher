package com.matrix.launcher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Relays an install-session result to the update UI. The listener is set by
 * [UpdateCheckController] while an install is in flight; results arriving
 * with no listener are dropped (the user re-checks). onReceive runs on the
 * main thread, so the listener may update Compose state directly.
 */
internal object UpdateInstallEvents {

    internal sealed interface InstallResult {
        data object Installed : InstallResult
        data object WaitingForConfirmation : InstallResult
        data class Failed(val message: String) : InstallResult
    }

    private var listener: ((InstallResult) -> Unit)? = null
    private var activeOperation: String? = null

    fun setListener(operation: String? = null, listener: ((InstallResult) -> Unit)?) {
        activeOperation = operation
        this.listener = listener
    }

    fun clear(operation: String) {
        if (activeOperation == operation) { listener = null; activeOperation = null }
    }

    fun publish(operation: String?, result: InstallResult) {
        if (operation == null || activeOperation != operation) return
        val target = listener
        if (result !is InstallResult.WaitingForConfirmation) { listener = null; activeOperation = null }
        target?.invoke(result)
    }


}

/**
 * Receives the PackageInstaller session result for a manual update install.
 * Non-exported; declared in the manifest. The class is public (not internal)
 * so the framework can instantiate it.
 */
class UpdateInstallResultReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
        val apkPath = intent.getStringExtra(ApkInstaller.EXTRA_APK_PATH)
        val id = intent.getIntExtra(PackageInstaller.EXTRA_SESSION_ID, -1)
        val version = intent.getLongExtra(ApkInstaller.EXTRA_VERSION_CODE, -1)
        if (!PendingUpdateSession.matches(context, id, version, apkPath)) return
        fun publish(result: UpdateInstallEvents.InstallResult, settled: Boolean = true) {
            if (settled && result !is UpdateInstallEvents.InstallResult.WaitingForConfirmation) PendingUpdateSession.clear(context, id)
            UpdateInstallEvents.publish(intent.getStringExtra(ApkInstaller.EXTRA_OPERATION), result)
        }
        val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
        when (status) {
            PackageInstaller.STATUS_SUCCESS -> {
                // Keep cleanup off the broadcast/main thread and keep the receiver
                // alive until it finishes, even after starting the new activity.
                val pending = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try { apkPath?.let { File(it).delete() } }
                    finally { pending.finish() }
                }
                // Package replacement destroys the old process and its listener.
                // The explicit session callback starts a fresh launcher independently
                // of that listener; it does not change the selected Home helper.
                // AOSP allows the selected Home app to start activities from the
                // background. Installer callbacks alone do not grant that privilege;
                // OEM/helper configurations must be checked on a real TV.
                finishSuccessfulUpdate(
                    notify = { publish(UpdateInstallEvents.InstallResult.Installed) },
                    reopen = {
                        // A blocked or failed relaunch must never crash the receiver;
                        // the user can still return Home from the system UI.
                        runCatching {
                            context.startActivity(Intent(context, ComposeHomeActivity::class.java).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                            })
                        }
                    }
                )
            }
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                val confirm = pendingConfirmationIntent(intent)
                if (confirm != null) {
                    confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    try {
                        context.startActivity(confirm)
                        publish(UpdateInstallEvents.InstallResult.WaitingForConfirmation)
                    } catch (_: Exception) {
                        val settled = runCatching {
                            PendingUpdateSession.settle(PackageInstallerSessionStore(context.packageManager.packageInstaller), id)
                        }.getOrDefault(false)
                        publish(UpdateInstallEvents.InstallResult.Failed("Couldn’t open install confirmation. Try Install again."), settled)
                    }
                } else {
                    val settled = runCatching {
                        PendingUpdateSession.settle(PackageInstallerSessionStore(context.packageManager.packageInstaller), id)
                    }.getOrDefault(false)
                    publish(UpdateInstallEvents.InstallResult.Failed("The installer asked for confirmation but gave no screen to show."), settled)
                }
            }
            else -> {
                publish(UpdateInstallEvents.InstallResult.Failed(
                    message?.takeIf { it.isNotBlank() } ?: "The install failed. Try again."
                ))
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun pendingConfirmationIntent(intent: Intent): Intent? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
        } else {
            intent.getParcelableExtra(Intent.EXTRA_INTENT)
        }
}

/** A missing or failed old UI listener must never prevent returning after replacement. */
internal fun finishSuccessfulUpdate(notify: () -> Unit, reopen: () -> Unit) {
    try { notify() } finally { reopen() }
}
