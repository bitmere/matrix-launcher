package com.matrix.launcher

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.json.JSONObject
import java.util.Calendar


/** MCP tools / web-search handling for the AI client (P0/P1 split).
 * The two parallel tool-execution loops in [MatrixAiClient.discover] differed only in the
 * progress actor ("Gemini" vs "AI"); they now share [recordMcpToolAction]. The query helpers
 * below moved here verbatim from MatrixAiClient.kt. */
internal fun MatrixAiClient.recordMcpToolAction(
    funcName: String,
    args: JSONObject,
    query: String,
    searchActions: MutableList<String>,
    onProgress: ((String) -> Unit)?,
    actor: String
) {
    when (funcName) {
        "web_search" -> {
            val q = args.optString("query", query)
            searchActions.add("Searched web: \"$q\"")
            onProgress?.invoke("$actor searched live web: \"$q\"…")
        }
        "tv_context" -> {
            searchActions.add("Inspected TV apps & environment")
            onProgress?.invoke("$actor inspecting TV platform context…")
        }
        "lookup_stream_availability" -> {
            val t = args.optString("title", "")
            searchActions.add("Checked Cinemeta stream availability: \"$t\"")
            onProgress?.invoke("$actor checking stream availability: \"$t\"…")
        }
        else -> onProgress?.invoke("$actor invoking tool: $funcName…")
    }
}

internal suspend fun MatrixAiClient.executeGeminiMcpTools(
    funcCalls: List<JSONObject>,
    ctx: AiDiscoverContext,
    execute: suspend (String, JSONObject) -> String = { name, args -> MatrixMcpEngine.executeTool(name, args, context, ctx.forceRefresh) }
): List<Pair<String, String>> {
    val query = ctx.query
    val searchActions = ctx.searchActions
    val onProgress = ctx.onProgress
    require(funcCalls.size <= 16) { "Too many AI tool calls" }
    val throttle = kotlinx.coroutines.sync.Semaphore(4)
    return coroutineScope {
        funcCalls.map { fc ->
            val funcName = fc.optString("name")
            val args = fc.optJSONObject("args") ?: JSONObject()
            recordMcpToolAction(funcName, args, query, searchActions, onProgress, "Gemini")
            async(Dispatchers.IO) { throttle.withPermit {
                val result = execute(funcName, args)
                funcName to result
            } }
        }.awaitAll()
    }
}

internal suspend fun MatrixAiClient.executeOpenAiMcpTools(
    callsList: List<Triple<String, String, JSONObject>>,
    ctx: AiDiscoverContext,
    execute: suspend (String, JSONObject) -> String = { name, args -> MatrixMcpEngine.executeTool(name, args, context, ctx.forceRefresh) }
): List<Pair<String, Pair<String, String>>> {
    val query = ctx.query
    val searchActions = ctx.searchActions
    val onProgress = ctx.onProgress
    require(callsList.size <= 16) { "Too many AI tool calls" }
    val throttle = kotlinx.coroutines.sync.Semaphore(4)
    return coroutineScope {
        callsList.map { (callId, funcName, args) ->
            recordMcpToolAction(funcName, args, query, searchActions, onProgress, "AI")
            async(Dispatchers.IO) { throttle.withPermit {
                val toolResult = execute(funcName, args)
                callId to (funcName to toolResult)
            } }
        }.awaitAll()
    }
}

/** Determines if a discovery query explicitly requests newly released or current theatrical/streaming content. */
internal fun isRecencyQuery(query: String): Boolean {
    val q = query.lowercase()
    val words = listOf("latest", "new", "recent", "current", "this year", "upcoming", "in theatres", "in theaters", "box office", "just released", "now showing")
    return words.any { q.contains(it) }
}

private val YEAR_REGEX = Regex("""\b(19\d{2}|20\d{2})\b""")

/** Extracts an explicit release year (1900..currentYear+2) if the user query specifically mentions one. */
internal fun extractExplicitYear(query: String): String? {
    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    val maxFutureYear = currentYear + 2
    return YEAR_REGEX.findAll(query)
        .map { it.value }
        .firstOrNull { y ->
            val num = y.toIntOrNull() ?: 0
            num in 1900..maxFutureYear
        }
}
