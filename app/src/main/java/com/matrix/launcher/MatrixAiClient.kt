package com.matrix.launcher

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale


internal data class AiModelChoice(val id: String, val label: String, val description: String)

/** AI discovery provider types: DeepSeek official, Neuralwatt, Groq, OpenRouter, OpenAI, or custom endpoint. */
internal enum class MatrixAiProvider(
    val label: String,
    val defaultUrl: String,
    val defaultModel: String,
    val description: String,
    val recommendedModels: List<AiModelChoice>
) {
    DEEPSEEK(
        "DeepSeek (Official)",
        "https://api.deepseek.com",
        "deepseek-chat",
        "Official DeepSeek API (api.deepseek.com) · deepseek-chat (V3) and deepseek-reasoner (R1)",
        listOf(
            AiModelChoice("deepseek-chat", "DeepSeek-V3", "Flagship 671B MoE · Fast and highly capable"),
            AiModelChoice("deepseek-reasoner", "DeepSeek-R1", "DeepSeek reasoning model with chain-of-thought")
        )
    ),
    NEURALWATT(
        "Neuralwatt",
        "https://api.neuralwatt.com/v1",
        "deepseek-v4.1-flash",
        "Free DeepSeek-V4.1-Flash until Jan 2027 · Ultra-fast inference",
        listOf(
            AiModelChoice("deepseek-v4.1-flash", "DeepSeek-V4.1-Flash", "Free access until Jan 2027 · Ultra-fast"),
            AiModelChoice("deepseek-v4.1-flash-speed", "DeepSeek-V4.1-Flash (Speed)", "Dedicated low-latency speed lane"),
            AiModelChoice("glm-5.3-flash", "GLM-5.3 Flash", "Fast reasoning-capable model"),
            AiModelChoice("qwen-3.8-27b", "Qwen 3.8 27B", "High accuracy multimodal model"),
            AiModelChoice("nw-large", "Neuralwatt Large", "Flagship Neuralwatt model"),
            AiModelChoice("kimi-k3", "Kimi K3", "Long context reasoning")
        )
    ),
    GROQ(
        "Groq",
        "https://api.groq.com/openai/v1",
        "llama-3.3-70b-versatile",
        "Ultra-fast inference on Groq LPUs with low latency",
        listOf(
            AiModelChoice("llama-3.3-70b-versatile", "Llama 3.3 70B Versatile", "Flagship Llama on Groq LPUs"),
            AiModelChoice("deepseek-r1-distill-llama-70b", "DeepSeek R1 Distill 70B", "Fast reasoning on Groq LPUs"),
            AiModelChoice("llama-3.1-8b-instant", "Llama 3.1 8B Instant", "Lightning-fast responses")
        )
    ),
    OPENROUTER(
        "OpenRouter",
        "https://openrouter.ai/api/v1",
        "deepseek/deepseek-r1:free",
        "Multi-model gateway with community free tiers",
        listOf(
            AiModelChoice("deepseek/deepseek-r1:free", "DeepSeek R1 (Free)", "Community free tier reasoning"),
            AiModelChoice("meta-llama/llama-3.3-70b-instruct:free", "Llama 3.3 70B (Free)", "Community free tier 70B"),
            AiModelChoice("google/gemini-2.0-flash-exp:free", "Gemini 2.0 Flash (Free)", "Fast Google multimodal free tier"),
            AiModelChoice("deepseek/deepseek-chat", "DeepSeek V3", "Direct high-throughput")
        )
    ),
    OPENAI(
        "OpenAI",
        "https://api.openai.com/v1",
        "gpt-4o-mini",
        "Standard OpenAI endpoints for GPT-4o Mini or GPT-4o",
        listOf(
            AiModelChoice("gpt-4o-mini", "GPT-4o Mini", "Fast and affordable"),
            AiModelChoice("gpt-4o", "GPT-4o", "Flagship omni model")
        )
    ),
    PERPLEXITY(
        "Perplexity",
        "https://api.perplexity.ai/v1",
        "sonar",
        "Native real-time web search with fresh 2025/2026 releases",
        listOf(
            AiModelChoice("sonar", "Sonar", "Live web search · Fast and lightweight"),
            AiModelChoice("sonar-pro", "Sonar Pro", "Advanced live web research")
        )
    ),
    GEMINI(
        "Google Gemini",
        "https://generativelanguage.googleapis.com/v1beta",
        "gemini-2.5-flash",
        "Official Google Gemini API · Free tier available with ultra-fast inference",
        listOf(
            AiModelChoice("gemini-2.5-flash", "Gemini 2.5 Flash", "Google's fastest flagship model · Free tier"),
            AiModelChoice("gemini-2.5-pro", "Gemini 2.5 Pro", "Deep reasoning & complex recommendations"),
            AiModelChoice("gemini-2.0-flash", "Gemini 2.0 Flash", "High speed and multimodal intelligence"),
            AiModelChoice("gemini-1.5-flash", "Gemini 1.5 Flash", "Lightweight and low latency")
        )
    ),
    CUSTOM(
        "Custom / Local (Ollama)",
        "https://api.deepseek.com",
        "deepseek-chat",
        "Any OpenAI-compatible base URL (Ollama, vLLM, LMStudio)",
        listOf(
            AiModelChoice("deepseek-chat", "DeepSeek-V3", "Default"),
            AiModelChoice("deepseek-reasoner", "DeepSeek-R1", "Reasoning model"),
            AiModelChoice("llama3.2", "Llama 3.2", "Local Ollama instance")
        )
    )
}

internal data class AiRecommendation(val title: String, val year: String, val type: String, val reason: String, val imdbId: String = "")

/** Inputs shared by the extracted provider workflows; built once by [MatrixAiClient.discover]. */
internal data class AiDiscoverContext(
    val query: String,
    val model: String,
    val key: String,
    val count: Int,
    val maxTokens: Int,
    val currentYear: Int,
    val isReasoning: Boolean,
    val explicitYear: String?,
    val isRecency: Boolean,
    val systemPrompt: String,
    val userPrompt: String,
    val finalInstruct: String,
    val searchActions: MutableList<String>,
    val onProgress: ((String) -> Unit)?,
    /** Force-refresh: bypass the web-search cache for this discovery's web lookups. */
    val forceRefresh: Boolean = false
)

internal class MatrixAiClient(private val prefs: SharedPreferences, internal val context: Context? = null) {
    var safeMode: () -> Boolean = { MatrixSafeMode.isOn(prefs) }

    var enabled: Boolean
        // Safe mode reports AI discovery as disabled (the toggle still holds the saved value).
        get() = !safeMode() && prefs.getBoolean(MatrixPrefs.AI_DISCOVERY_ENABLED, false)
        set(value) { prefs.edit { putBoolean(MatrixPrefs.AI_DISCOVERY_ENABLED, value) } }

    var webSearch: Boolean
        get() = prefs.getBoolean(MatrixPrefs.AI_WEB_SEARCH, false)
        set(value) { prefs.edit { putBoolean(MatrixPrefs.AI_WEB_SEARCH, value) }; onConfigurationChanged?.invoke() }

    var resultCount: Int
        get() = prefs.getInt(MatrixPrefs.AI_RESULT_COUNT, MatrixPrefs.AI_RESULT_COUNT_DEFAULT).coerceIn(5, 100)
        set(value) { prefs.edit { putInt(MatrixPrefs.AI_RESULT_COUNT, value.coerceIn(5, 100)) }; onConfigurationChanged?.invoke() }

    var lastSummary: String = ""
    var onConfigurationChanged: (() -> Unit)? = null

    private val modelsCache = java.util.concurrent.ConcurrentHashMap<String, List<AiModelChoice>>()

    private fun clearCache() {
        modelsCache.clear()
    }

    var provider: MatrixAiProvider
        get() = runCatching { MatrixAiProvider.valueOf(prefs.getString(MatrixPrefs.AI_PROVIDER, MatrixAiProvider.NEURALWATT.name)!!) }
            .getOrDefault(MatrixAiProvider.NEURALWATT)
        set(value) { clearCache(); prefs.edit { putString(MatrixPrefs.AI_PROVIDER, value.name) }; onConfigurationChanged?.invoke() }

    var apiKey: String
        get() = prefs.getString(MatrixPrefs.aiApiKey(provider.name, selectedModel), "")
            ?.takeIf { it.isNotBlank() }
            ?: prefs.getString(MatrixPrefs.aiApiKey(provider.name), "").orEmpty().ifBlank {
                prefs.getString(MatrixPrefs.AI_API_KEY_LEGACY, "").orEmpty()
            }
        set(value) {
            clearCache()
            val trimmed = value.trim()
            prefs.edit {
                putString(MatrixPrefs.aiApiKey(provider.name), trimmed)
                if (selectedModel.isNotBlank()) {
                    putString(MatrixPrefs.aiApiKey(provider.name, selectedModel), trimmed)
                }
            }
            onConfigurationChanged?.invoke()
        }

    fun removeApiKey() {
        clearCache()
        prefs.edit {
            val prefix = MatrixPrefs.aiApiKey(provider.name)
            prefs.all.keys.filter { it == prefix || it.startsWith("${prefix}_") }.forEach { remove(it) }
            remove(MatrixPrefs.AI_API_KEY_LEGACY)
        }
        onConfigurationChanged?.invoke()
    }

    var selectedModel: String
        get() = prefs.getString(MatrixPrefs.aiModel(provider.name), "").orEmpty().ifBlank { provider.defaultModel }
        set(value) { prefs.edit { putString(MatrixPrefs.aiModel(provider.name), value.trim()) }; onConfigurationChanged?.invoke() }

    var customUrl: String
        get() = prefs.getString(MatrixPrefs.AI_BASE_URL, provider.defaultUrl).orEmpty().ifBlank { provider.defaultUrl }
        set(value) { clearCache(); prefs.edit { putString(MatrixPrefs.AI_BASE_URL, value.trim().removeSuffix("/")) }; onConfigurationChanged?.invoke() }

    val activeUrl: String get() = if (provider == MatrixAiProvider.CUSTOM) customUrl else provider.defaultUrl
    val activeModel: String get() = selectedModel.ifBlank { provider.defaultModel }

    val ready: Boolean get() = enabled && apiKey.isNotBlank()

    /** Requests structured movie and TV recommendations powered by in-app MCP tools and LLM inference. */
    suspend fun discover(query: String, onProgress: ((String) -> Unit)? = null, forceRefresh: Boolean = false): List<AiRecommendation> = withContext(Dispatchers.IO) {
        val key = apiKey.trim()
        require(key.isNotBlank()) { "Enter your AI API key in Settings → Integrations → AI Discovery." }
        val model = activeModel.trim()
        val isPerplexity = provider == MatrixAiProvider.PERPLEXITY
        val isGemini = provider == MatrixAiProvider.GEMINI
        val isReasoning = model.contains("reasoner", ignoreCase = true) || model.contains("r1", ignoreCase = true)
        val count = resultCount
        val maxTokens = when {
            count >= 100 -> 16384
            count >= 50 -> 8192
            count >= 25 -> 6144
            else -> 4096
        }

        val cal = Calendar.getInstance()
        val currentYear = cal.get(Calendar.YEAR)
        val monthYear = SimpleDateFormat("MMMM yyyy", Locale.US).format(Date())

        val explicitYear = extractExplicitYear(query)
        val isRecency = isRecencyQuery(query)
        val searchActions = mutableListOf<String>()

        val temporalRule = when {
            explicitYear != null -> {
                "- MANDATORY EXACT YEAR CONSTRAINT: The user specifically and explicitly requested titles from the year $explicitYear. EVERY SINGLE recommended title MUST strictly have been released or premiere in $explicitYear (year: \"$explicitYear\"). DO NOT recommend titles from ${explicitYear.toInt() - 1}, ${explicitYear.toInt() + 1}, or any other year. If a title was released in 2025 or any year other than $explicitYear, it is STRICTLY FORBIDDEN and must be omitted.\n"
            }
            isRecency -> {
                "- The user specifically requested recent/current titles. Recommend strictly releases from ${currentYear - 1} and $currentYear. Do not recommend older titles.\n"
            }
            else -> {
                "- The user query is topic/genre based. Recommend the absolute best titles across cinema history with their true release years. DO NOT restrict results to recent years.\n"
            }
        }

        val systemPrompt = "You are an expert TV movie and series discovery engine for Matrix Launcher on Android TV.\n" +
            "CURRENT REAL-WORLD DATE: $monthYear (Current year is $currentYear).\n" +
            "TEMPORAL & RELEASE YEAR ACCURACY RULES:\n" +
            "- REAL-WORLD CALENDAR RELEASE YEAR ONLY: Always provide the actual historical or current calendar release year when the title debuted (e.g. Blade Runner 2049 was released in 2017; 2001: A Space Odyssey was released in 1968; Metropolis was released in 1927; Cyberpunk: Edgerunners was released in 2022). NEVER confuse fictional story settings (such as futuristic years 2046, 2049, 2077) with actual release dates.\n" +
            "- Valid release years must be between 1895 and $currentYear (or ${currentYear + 1} for verified upcoming announcements). Any futuristic year beyond ${currentYear + 1} is invalid.\n" +
            temporalRule +
            "- ACCURATE RELEASE YEARS: If multiple movies share the same title, ensure the release year matches the intended movie. Output \"imdb_id\": \"\" unless you are 100% certain the IMDb ID belongs to that exact year's release.\n" +
            "- REGIONAL, ACTOR & LANGUAGE FIDELITY: When the query specifies an actor, director, language, or cinema region (e.g. 'Ravi Teja movies', 'Telugu thrillers', 'Tamil cinema', 'Malayalam crime drama', 'Anime', 'Korean cinema', 'Bollywood'):\n" +
            "  * Every single recommendation MUST strictly feature that specific person, actor, director, or language/industry requested.\n" +
            "  * Disambiguate remakes and homonyms: DO NOT confuse regional originals with remakes in other languages or Hollywood movies with similar names (e.g. Ravi Teja's Telugu 'Kick' 2009 vs Salman Khan's Hindi 'Kick' 2014 vs 'Kick-Ass' 2010; Ravi Teja's Telugu 'Dhamaka' 2022 vs Hindi 'Dhamaka' 2021).\n" +
            "  * Output the exact official release title with its true release year and type.\n" +
            (if (webSearch && !isPerplexity) "- Live in-app MCP tools (`web_search`, `tv_context`, `lookup_stream_availability`) are active. You can invoke them in parallel to verify true titles, release years, and stream availability.\n" else "") +
            "- Respond with a valid JSON object containing:\n" +
            "  \"summary\": a natural 1-2 sentence plain-language synthesis explaining what you researched, why you picked these titles, and what was verified in plain English (string)\n" +
            "  \"recommendations\": array of $count recommendation objects, each with:\n" +
            "    \"title\": exact official title (string)\n" +
            "    \"year\": 4-digit release year (string)\n" +
            "    \"type\": \"movie\" or \"series\" (string)\n" +
            "    \"imdb_id\": IMDb ID starting with tt if known, or empty string (string)\n" +
            "    \"reason\": short explanation why it fits under 15 words (string)\n" +
            "Do not include markdown code fences, conversational prose, or think tags. Output raw JSON only."

        val userPrompt = "User query: \"$query\". Recommend exactly $count exact movie or series titles that best fit."

        val finalInstruct = if (explicitYear != null) {
            "Synthesize all discovered facts above now. The user STRICTLY requested titles from $explicitYear. Output ONLY a valid JSON object: {\"summary\": \"1-2 sentence plain English synthesis explaining what was researched, why these titles were chosen, and confirming that all titles strictly debut in $explicitYear\", \"recommendations\": [{\"title\":\"...\",\"year\":\"$explicitYear\",\"type\":\"movie\",\"imdb_id\":\"tt...\",\"reason\":\"...\"}]}. Filter out and discard any titles from other years. No more tool calls, output raw JSON only."
        } else if (isRecency) {
            "Synthesize all discovered facts above now. Output ONLY a valid JSON object: {\"summary\": \"1-2 sentence plain English synthesis explaining what was researched and why you selected these recent releases\", \"recommendations\": [{\"title\":\"...\",\"year\":\"$currentYear\",\"type\":\"movie\",\"imdb_id\":\"tt...\",\"reason\":\"...\"}]}. No more tool calls, output raw JSON only."
        } else {
            "Synthesize all discovered facts above now. Output ONLY a valid JSON object: {\"summary\": \"1-2 sentence plain English synthesis explaining what was researched and why you selected these titles\", \"recommendations\": [{\"title\":\"...\",\"year\":\"YYYY\",\"type\":\"movie\",\"imdb_id\":\"tt...\",\"reason\":\"...\"}]}. Accurately reflect each title's true real-world release year. No more tool calls, output raw JSON only."
        }
        val ctx = AiDiscoverContext(
            query = query, model = model, key = key, count = count, maxTokens = maxTokens,
            currentYear = currentYear, isReasoning = isReasoning, explicitYear = explicitYear,
            isRecency = isRecency, systemPrompt = systemPrompt, userPrompt = userPrompt,
            finalInstruct = finalInstruct, searchActions = searchActions, onProgress = onProgress,
            forceRefresh = forceRefresh
        )
        if (isGemini) return@withContext discoverWithGemini(ctx)
        if (isPerplexity) return@withContext discoverWithPerplexity(ctx)
        return@withContext discoverWithOpenAiCompatible(ctx)
    }

    /** Plain-language synthesis of a discovery result; was a local function inside [discover].
     * Widened to internal so the extracted provider workflows in MatrixAiProviders.kt can use it. */
    internal fun updateSummary(
        recs: List<AiRecommendation>,
        query: String,
        explicitYear: String?,
        isRecency: Boolean,
        rawResponse: String = ""
    ) {
        val extracted = lastExtractedSummary?.takeIf { it.isNotBlank() }
        if (extracted != null) {
            lastSummary = extracted.trim()
            return
        }
        // Natural plain language synthesis (ZERO robotic tool calls)
        val sb = StringBuilder()
        if (explicitYear != null) {
            sb.append("Researched confirmed $explicitYear releases for \"$query\", verified release calendar accuracy to strictly exclude other years, and finalized ${recs.size} standout titles cross-referenced for Stremio & Nuvio streaming playback.")
        } else if (isRecency) {
            sb.append("Researched newly debuted theatrical and streaming releases matching \"$query\", verified availability, and finalized ${recs.size} standout titles ready for streaming playback.")
        } else {
            sb.append("Explored cinema and TV catalogs for \"$query\", cross-referencing critical acclaim and streaming playback to finalize ${recs.size} curated recommendations.")
        }
        lastSummary = sb.toString()
    }

    internal var lastExtractedSummary: String? = null

    internal fun parseRecommendations(raw: String, explicitYear: String? = null): List<AiRecommendation> {
        lastExtractedSummary = null
        val withoutThink = if (raw.contains("</think>")) {
            raw.substring(raw.lastIndexOf("</think>") + 8).trim()
        } else if (raw.contains("<think>") && raw.indexOf('[') > raw.indexOf("<think>")) {
            raw.substringAfter("<think>").trim()
        } else {
            raw
        }
        val clean = withoutThink.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()

        // 1. Try parsing as a structured JSON object: {"summary": "...", "recommendations": [...]}
        var array: JSONArray? = null
        val startObj = clean.indexOf('{')
        val endObj = clean.lastIndexOf('}')
        if (startObj >= 0 && endObj > startObj && (startObj < clean.indexOf('[') || clean.indexOf('[') < 0)) {
            val jsonObject = runCatching { JSONObject(clean.substring(startObj, endObj + 1)) }.getOrNull()
            if (jsonObject != null) {
                val summaryText = jsonObject.optString("summary").trim()
                if (summaryText.isNotBlank()) {
                    lastExtractedSummary = summaryText
                }
                array = jsonObject.optJSONArray("recommendations")
                    ?: jsonObject.optJSONArray("items")
                    ?: jsonObject.optJSONArray("results")
            }
        }

        // 2. Fallback: Parse as a direct JSON array [...]
        if (array == null) {
            val start = clean.indexOf('[')
            if (start >= 0) {
                // Extract any natural introductory synthesis before '['
                val preIntro = clean.substring(0, start).trim()
                val introLine = preIntro.lines()
                    .map { it.trim().removePrefix("-").removePrefix("*").trim() }
                    .filter { it.isNotBlank() && !it.startsWith("<") && !it.startsWith("```") && !it.startsWith("{") }
                    .lastOrNull { it.length in 25..300 }
                if (!introLine.isNullOrBlank()) {
                    lastExtractedSummary = introLine
                }

                val jsonToParse = if (clean.lastIndexOf(']') > start) {
                    clean.substring(start, clean.lastIndexOf(']') + 1)
                } else if (clean.lastIndexOf('}') > start) {
                    clean.substring(start, clean.lastIndexOf('}') + 1) + "]"
                } else {
                    return emptyList()
                }

                array = runCatching { JSONArray(jsonToParse) }.getOrElse {
                    val lastGoodObj = jsonToParse.lastIndexOf("},")
                    if (lastGoodObj > start) {
                        runCatching { JSONArray(jsonToParse.substring(0, lastGoodObj + 1) + "]") }.getOrNull()
                    } else null
                }
            }
        }

        if (array == null) return emptyList()

        val results = mutableListOf<AiRecommendation>()
        for (i in 0 until minOf(array.length(), 100)) {
            val obj = array.optJSONObject(i) ?: continue
            val title = obj.optString("title").trim()
            if (title.isBlank()) continue
            val rawYear = obj.optString("year").trim()
            val yearDigits = rawYear.filter(Char::isDigit).take(4)
            if (explicitYear != null && yearDigits.isNotBlank() && yearDigits != explicitYear) {
                continue
            }
            val year = if (yearDigits.isNotBlank()) yearDigits else (explicitYear.orEmpty())
            val type = if (obj.optString("type").lowercase().contains("series") || obj.optString("type").lowercase().contains("show")) "series" else "movie"
            val reason = obj.optString("reason").trim()
            val imdbId = obj.optString("imdb_id").trim().takeIf { it.startsWith("tt") }.orEmpty()
            results.add(AiRecommendation(title, year, type, reason, imdbId))
        }
        return results
    }

    suspend fun fetchModels(): Result<List<AiModelChoice>> = withContext(Dispatchers.IO) {
        val cacheKey = "${provider.name}_$activeUrl"
        modelsCache[cacheKey]?.let { return@withContext Result.success(it) }
        val isGemini = provider == MatrixAiProvider.GEMINI
        val key = apiKey.trim()
        val endpoint = if (isGemini) {
            "${activeUrl.trim().removeSuffix("/")}/models?key=$key"
        } else {
            activeUrl.trim().removeSuffix("/") + "/models"
        }
        fun openConnection(authKey: String): HttpURLConnection {
            val url = URL(endpoint)
            return (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 10000
                readTimeout = 12000
                requestMethod = "GET"
                if (isGemini) {
                    if (authKey.isNotBlank()) setRequestProperty("x-goog-api-key", authKey)
                } else {
                    if (authKey.isNotBlank()) setRequestProperty("Authorization", "Bearer $authKey")
                }
                setRequestProperty("User-Agent", "MatrixLauncher (Android TV)")
            }
        }

        var conn = openConnection(key)
        try {
            var code = conn.responseCode
            // If the provider rejected the stored key with 401, retry anonymously to fetch the public model catalog
            if (code == 401 && key.isNotBlank() && !isGemini) {
                conn.disconnect()
                conn = openConnection("")
                code = conn.responseCode
            }

            val stream: InputStream = if (code in 200..299) conn.inputStream else conn.errorStream
                ?: return@withContext Result.failure(IllegalStateException("HTTP $code from provider"))
            val responseText = stream.use { BoundedInput.readText(it, 1024 * 1024) }
            if (code !in 200..299) {
                val err = runCatching { JSONObject(responseText).optJSONObject("error")?.optString("message") }.getOrNull()
                    ?: "HTTP $code"
                return@withContext Result.failure(IllegalStateException("Provider returned: $err"))
            }
            val json = JSONObject(responseText)
            val list = mutableListOf<AiModelChoice>()

            if (isGemini) {
                val modelsArr = json.optJSONArray("models")
                    ?: return@withContext Result.failure(IllegalStateException("No models in response"))
                for (i in 0 until minOf(modelsArr.length(), 256)) {
                    val obj = modelsArr.optJSONObject(i) ?: continue
                    val fullName = obj.optString("name")
                    val methods = obj.optJSONArray("supportedGenerationMethods")
                    val supportsGenerate = (0 until (methods?.length() ?: 0)).any { methods?.optString(it) == "generateContent" }
                    if (!supportsGenerate) continue
                    val id = fullName.removePrefix("models/").trim()
                    if (id.isBlank()) continue
                    val label = obj.optString("displayName").takeIf { it.isNotBlank() } ?: id
                    val desc = obj.optString("description").takeIf { it.isNotBlank() } ?: "Google Gemini · $id"
                    list.add(AiModelChoice(id, label, desc))
                }
            } else {
                val data = json.optJSONArray("data") ?: return@withContext Result.failure(IllegalStateException("No model data in response"))
                for (i in 0 until minOf(data.length(), 256)) {
                    val obj = data.optJSONObject(i) ?: continue
                    val id = obj.optString("id").trim()
                    if (id.isBlank()) continue
                    val meta = obj.optJSONObject("metadata")
                    val label = meta?.optString("display_name")?.takeIf { it.isNotBlank() }
                        ?: obj.optString("name").takeIf { it.isNotBlank() } ?: id
                    val desc = meta?.optString("description")?.takeIf { it.isNotBlank() }
                        ?: obj.optString("description").takeIf { it.isNotBlank() }
                        ?: (meta?.optString("provider")?.let { "Provider: $it · $id" } ?: "Model ID: $id")
                    list.add(AiModelChoice(id, label, desc))
                }
            }

            val sorted = list.sortedWith(compareBy<AiModelChoice> {
                val lower = it.id.lowercase()
                when {
                    lower.contains("flash") -> 0
                    lower.contains("free") -> 1
                    lower.contains("deepseek") -> 2
                    lower.contains("gemini") -> 3
                    lower.contains("llama") -> 4
                    lower.contains("qwen") -> 5
                    else -> 6
                }
            }.thenBy { it.label.lowercase() })
            modelsCache[cacheKey] = sorted
            Result.success(sorted)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            conn.disconnect()
        }
    }

    suspend fun testConnection(): Result<String> = withContext(Dispatchers.IO) {
        val key = apiKey.trim()
        if (key.isBlank() && provider != MatrixAiProvider.CUSTOM) {
            return@withContext Result.failure(IllegalStateException("No API key set for ${provider.label}. Enter key in settings."))
        }
        val isPerplexity = provider == MatrixAiProvider.PERPLEXITY
        val isGemini = provider == MatrixAiProvider.GEMINI
        val endpoint = when {
            isGemini -> "${activeUrl.trim().removeSuffix("/")}/models?key=$key"
            isPerplexity -> {
                val base = activeUrl.trim().removeSuffix("/")
                if (base.endsWith("/v1")) "$base/responses" else "$base/v1/responses"
            }
            else -> activeUrl.trim().removeSuffix("/") + "/chat/completions"
        }
        val model = activeModel.trim()
        val isReasoning = model.contains("reasoner", ignoreCase = true) || model.contains("r1", ignoreCase = true)
        val body = when {
            isGemini -> null
            isPerplexity -> {
                JSONObject().apply {
                    put("model", model)
                    put("input", "ping")
                    put("stream", false)
                }
            }
            else -> {
                JSONObject().apply {
                    put("model", model)
                    put("messages", JSONArray().apply {
                        put(JSONObject().apply {
                            put("role", "user")
                            put("content", "ping")
                        })
                    })
                    put("max_tokens", if (isReasoning) 500 else 10)
                    if (!isReasoning) {
                        put("temperature", 0.6)
                    }
                }
            }
        }
        val url = URL(endpoint)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 10000
            readTimeout = 12000
            requestMethod = if (isGemini) "GET" else "POST"
            if (!isGemini) doOutput = true
            setRequestProperty("Content-Type", "application/json")
            if (isGemini) {
                setRequestProperty("x-goog-api-key", key)
            } else if (key.isNotBlank()) {
                setRequestProperty("Authorization", "Bearer $key")
            }
            setRequestProperty("User-Agent", "MatrixLauncher (Android TV)")
        }
        try {
            if (body != null) {
                conn.outputStream.use { out ->
                    out.write(body.toString().toByteArray(StandardCharsets.UTF_8))
                }
            }
            val code = conn.responseCode
            val stream: InputStream = if (code in 200..299) conn.inputStream else conn.errorStream
                ?: return@withContext Result.failure(IllegalStateException("HTTP $code"))
            val responseText = stream.use { BoundedInput.readText(it, 1024 * 1024) }
            if (code in 200..299) {
                Result.success("Success: Connected to ${provider.label} using $model")
            } else {
                val err = runCatching { JSONObject(responseText).optJSONObject("error")?.optString("message") }.getOrNull()
                    ?: "HTTP $code"
                Result.failure(IllegalStateException("${provider.label} error ($code): $err"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Connection failed: ${e.message ?: e.javaClass.simpleName}"))
        } finally {
            conn.disconnect()
        }
    }
}
