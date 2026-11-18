package com.matrix.launcher

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets


/** HTTP transport for the AI client (P0/P1 split). [MatrixAiClient.postJson] was a private
 * member of MatrixAiClient; it is now an internal extension on MatrixAiClient (same body) so the
 * extracted provider workflows in MatrixAiProviders.kt can use it. */
internal fun MatrixAiClient.postJson(endpoint: String, authHeader: String, body: JSONObject,
    connect: (String) -> HttpURLConnection = { URL(it).openConnection() as HttpURLConnection }
): Pair<Int, String> {
    val conn = connect(endpoint).apply {
        connectTimeout = 12000
        readTimeout = 30000
        requestMethod = "POST"
        doOutput = true
        setRequestProperty("Content-Type", "application/json")
        if (authHeader.isNotBlank()) {
            if (authHeader.startsWith("x-goog-api-key:")) {
                setRequestProperty("x-goog-api-key", authHeader.substringAfter(":").trim())
            } else {
                setRequestProperty("Authorization", authHeader)
            }
        }
        setRequestProperty("User-Agent", "MatrixLauncher (Android TV)")
    }
    return try {
        conn.outputStream.use { it.write(body.toString().toByteArray(StandardCharsets.UTF_8)) }
        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else (conn.errorStream ?: conn.inputStream)
        val text = stream?.use { BoundedInput.readText(it, 1024 * 1024) }.orEmpty()
        code to text
    } finally {
        conn.disconnect()
    }
}