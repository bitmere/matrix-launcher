package com.matrix.launcher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dadb.AdbKeyPair
import dadb.Dadb
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

object AotOptimizationManager {
    val TRACKED_PACKAGES = mapOf(
        "com.stremio.one" to "Stremio",
        "com.nuvio.tv" to "Nuvio"
    )

    private fun getKeyPair(context: Context): AdbKeyPair {
        val priv = File(context.filesDir, "matrix_adbkey")
        val pub = File(context.filesDir, "matrix_adbkey.pub")
        if (!priv.exists() || !pub.exists()) {
            AdbKeyPair.generate(priv, pub)
        }
        return AdbKeyPair.read(priv, pub)
    }

    suspend fun runShell(context: Context, command: String, port: Int = 5555): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val keyPair = getKeyPair(context)
            Dadb.create("127.0.0.1", port, keyPair).use { dadb ->
                val response = dadb.shell(command)
                response.allOutput.trim()
            }
        }
    }

    suspend fun compilePackage(context: Context, packageName: String): Result<String> {
        return runShell(context, "cmd package compile -m speed -f $packageName")
    }

    suspend fun checkStatus(context: Context, packageName: String): String = withContext(Dispatchers.IO) {
        val res = runShell(context, "dumpsys package $packageName")
        val output = res.getOrNull() ?: return@withContext "unauthorized / offline"
        val match = Regex("""status=([a-zA-Z0-9_-]+)""").find(output)
        match?.groupValues?.get(1) ?: "unknown"
    }

    suspend fun compileAllTracked(context: Context): Map<String, String> = withContext(Dispatchers.IO) {
        val map = mutableMapOf<String, String>()
        for ((pkg, label) in TRACKED_PACKAGES) {
            val installed = try {
                context.packageManager.getPackageInfo(pkg, 0)
                true
            } catch (_: Exception) {
                false
            }
            if (installed) {
                val res = compilePackage(context, pkg)
                if (res.isSuccess && (res.getOrNull()?.contains("Success") == true || res.getOrNull()?.isBlank() == true)) {
                    map[label] = "Success (speed)"
                } else {
                    map[label] = res.exceptionOrNull()?.message ?: res.getOrDefault("Error")
                }
            } else {
                map[label] = "Not installed"
            }
        }
        map
    }
}

/**
 * Automatically catches Stremio and Nuvio app updates and compiles them to AOT speed in the background.
 */
class AotPackageUpdateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_PACKAGE_REPLACED || intent.action == Intent.ACTION_PACKAGE_ADDED) {
            val pkg = intent.data?.schemeSpecificPart ?: return
            if (pkg in AotOptimizationManager.TRACKED_PACKAGES) {
                val pending = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        AotOptimizationManager.compilePackage(context, pkg)
                    } finally {
                        pending.finish()
                    }
                }
            }
        }
    }
}
