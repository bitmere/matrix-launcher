package com.matrix.launcher

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject


/** Per-provider request/response handling, extracted from [MatrixAiClient.discover] (P0/P1 split).
 * Each workflow is an internal extension on MatrixAiClient so [MatrixAiClient.discover] keeps
 * its public signature and dispatches to it; bodies are otherwise verbatim. */
internal suspend fun MatrixAiClient.discoverWithGemini(ctx: AiDiscoverContext): List<AiRecommendation> = withContext(Dispatchers.IO) {
    val key = ctx.key
    val model = ctx.model
    val maxTokens = ctx.maxTokens
    val explicitYear = ctx.explicitYear
    val isRecency = ctx.isRecency
    val systemPrompt = ctx.systemPrompt
    val userPrompt = ctx.userPrompt
    val finalInstruct = ctx.finalInstruct
    val searchActions = ctx.searchActions
    val onProgress = ctx.onProgress
        // --- 1. GOOGLE GEMINI MCP WORKFLOW ---
        val base = activeUrl.trim().removeSuffix("/")
        val endpoint = "$base/models/$model:generateContent?key=$key"
        val contents = JSONArray().apply {
            put(JSONObject().apply {
                put("role", "user")
                put("parts", JSONArray().apply {
                    put(JSONObject().apply { put("text", "$systemPrompt\n\n$userPrompt") })
                })
            })
        }

        if (webSearch) {
            val geminiBody = JSONObject().apply {
                put("contents", contents)
                put("tools", MatrixMcpEngine.geminiToolsSchema())
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.4)
                    put("maxOutputTokens", maxTokens)
                })
            }
            onProgress?.invoke("Consulting Gemini ($model) with in-app MCP tools…")
            val (gCode, gResponse) = postJson(endpoint, "", geminiBody)
            if (gCode in 200..299) {
                val gJson = runCatching { JSONObject(gResponse) }.getOrNull()
                val candidate = gJson?.optJSONArray("candidates")?.optJSONObject(0)
                val finishReason = candidate?.optString("finishReason", "")
                if (finishReason.equals("SAFETY", ignoreCase = true)) {
                    throw IllegalStateException("Google Gemini safety filter blocked query.")
                }
                val parts = candidate?.optJSONObject("content")?.optJSONArray("parts")
                val funcCalls = (0 until (parts?.length() ?: 0))
                    .mapNotNull { parts?.optJSONObject(it)?.optJSONObject("functionCall") }
                if (funcCalls.isNotEmpty()) {
                    onProgress?.invoke("Executing ${funcCalls.size} in-app MCP tool(s) in parallel…")
                    val toolResults = executeGeminiMcpTools(funcCalls, ctx)

                    candidate?.optJSONObject("content")?.let { contents.put(it) }
                    contents.put(JSONObject().apply {
                        put("role", "function")
                        put("parts", JSONArray().apply {
                            toolResults.forEach { (funcName, toolResult) ->
                                put(JSONObject().apply {
                                    put("functionResponse", JSONObject().apply {
                                        put("name", funcName)
                                        put("response", JSONObject().apply { put("output", toolResult) })
                                    })
                                })
                            }
                        })
                    })
                    contents.put(JSONObject().apply {
                        put("role", "user")
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply { put("text", finalInstruct) })
                        })
                    })

                    onProgress?.invoke("Live facts retrieved. Generating recommendations with Gemini ($model)…")
                    val turn2Body = JSONObject().apply {
                        put("contents", contents)
                        put("generationConfig", JSONObject().apply {
                            put("responseMimeType", "application/json")
                            put("temperature", 0.4)
                            put("maxOutputTokens", maxTokens)
                        })
                    }
                    val (g2Code, g2Response) = postJson(endpoint, "", turn2Body)
                    if (g2Code in 200..299) {
                        val g2Json = runCatching { JSONObject(g2Response) }.getOrNull()
                        val parts2 = g2Json?.optJSONArray("candidates")?.optJSONObject(0)?.optJSONObject("content")?.optJSONArray("parts")
                        val text = (0 until (parts2?.length() ?: 0))
                            .mapNotNull { parts2?.optJSONObject(it)?.optString("text") }
                            .firstOrNull { it.isNotBlank() } ?: g2Response
                        val recs = parseRecommendations(text, explicitYear)
                        updateSummary(recs, ctx.query, ctx.explicitYear, ctx.isRecency, text)
                        return@withContext recs
                    }
                } else {
                    val text = (0 until (parts?.length() ?: 0))
                        .mapNotNull { parts?.optJSONObject(it)?.optString("text") }
                        .firstOrNull { it.isNotBlank() } ?: gResponse
                    val recs = parseRecommendations(text, explicitYear)
                    updateSummary(recs, ctx.query, ctx.explicitYear, ctx.isRecency, text)
                    return@withContext recs
                }
            }
        }

        // Gemini fallback / direct mode
        val directBody = JSONObject().apply {
            put("contents", contents)
            put("generationConfig", JSONObject().apply {
                put("responseMimeType", "application/json")
                put("temperature", 0.4)
                put("maxOutputTokens", maxTokens)
            })
        }
        onProgress?.invoke("Generating recommendations with Gemini ($model)…")
        val (dirCode, dirResponse) = postJson(endpoint, "", directBody)
        if (dirCode in 200..299) {
            val gJson = runCatching { JSONObject(dirResponse) }.getOrNull()
            val text = gJson?.optJSONArray("candidates")?.optJSONObject(0)?.optJSONObject("content")?.optJSONArray("parts")?.optJSONObject(0)?.optString("text") ?: dirResponse
            val recs = parseRecommendations(text, explicitYear)
            updateSummary(recs, ctx.query, ctx.explicitYear, ctx.isRecency, text)
            return@withContext recs
        } else {
            val err = runCatching { JSONObject(dirResponse).optJSONObject("error")?.optString("message") }.getOrNull() ?: dirResponse.take(120)
            throw IllegalStateException("Google Gemini error ($dirCode): $err")
        }
}

internal suspend fun MatrixAiClient.discoverWithPerplexity(ctx: AiDiscoverContext): List<AiRecommendation> = withContext(Dispatchers.IO) {
    val key = ctx.key
    val model = ctx.model
    val explicitYear = ctx.explicitYear
    val systemPrompt = ctx.systemPrompt
    val userPrompt = ctx.userPrompt
    val onProgress = ctx.onProgress
        // --- 2. PERPLEXITY WORKFLOW ---
        val base = activeUrl.trim().removeSuffix("/")
        val endpoint = if (base.endsWith("/v1")) "$base/responses" else "$base/v1/responses"
        val perpBody = JSONObject().apply {
            put("model", model)
            put("input", "$systemPrompt\n\n$userPrompt")
            put("instructions", "You are a concise JSON-only media discovery assistant.")
            put("stream", false)
        }
        onProgress?.invoke("Searching live web with Perplexity ($model)…")
        val (pCode, pResponse) = postJson(endpoint, "Bearer $key", perpBody)
        if (pCode in 200..299) {
            val json = runCatching { JSONObject(pResponse) }.getOrNull()
            val text = json?.optString("output") ?: json?.optString("output_text") ?: pResponse
            val recs = parseRecommendations(text, explicitYear)
            updateSummary(recs, ctx.query, ctx.explicitYear, ctx.isRecency, text)
            return@withContext recs
        } else {
            throw IllegalStateException("Perplexity error ($pCode): $pResponse")
        }
}

internal suspend fun MatrixAiClient.discoverWithOpenAiCompatible(ctx: AiDiscoverContext): List<AiRecommendation> = withContext(Dispatchers.IO) {
    val key = ctx.key
    val model = ctx.model
    val count = ctx.count
    val query = ctx.query
    val maxTokens = ctx.maxTokens
    val currentYear = ctx.currentYear
    val isReasoning = ctx.isReasoning
    val explicitYear = ctx.explicitYear
    val isRecency = ctx.isRecency
    val systemPrompt = ctx.systemPrompt
    val userPrompt = ctx.userPrompt
    val searchActions = ctx.searchActions
    val onProgress = ctx.onProgress
        // --- 3. OPENAI / NEURALWATT / GROQ / OPENROUTER MCP WORKFLOW ---
        val endpoint = activeUrl.trim().removeSuffix("/") + "/chat/completions"
        val messages = JSONArray().apply {
            put(JSONObject().apply {
                put("role", "system")
                put("content", systemPrompt)
            })
            put(JSONObject().apply {
                put("role", "user")
                put("content", userPrompt)
            })
        }

        if (webSearch) {
            var currentIteration = 0
            val maxToolTurns = 3
            var currentBody = JSONObject().apply {
                put("model", model)
                put("messages", messages)
                put("tools", MatrixMcpEngine.openAiToolsSchema())
                put("stream", false)
                put("max_tokens", maxTokens)
                if (!isReasoning) put("temperature", 0.5)
            }

            while (currentIteration < maxToolTurns) {
                currentIteration++
                onProgress?.invoke("Consulting $model (MCP turn $currentIteration)…")
                val (tCode, tResponse) = postJson(endpoint, "Bearer $key", currentBody)
                if (tCode !in 200..299) break

                val json = runCatching { JSONObject(tResponse) }.getOrNull()
                val choice = json?.optJSONArray("choices")?.optJSONObject(0)
                val msg = choice?.optJSONObject("message")
                val toolCalls = msg?.optJSONArray("tool_calls")
                val content = msg?.optString("content", "") ?: ""

                // Detect raw DeepSeek DSML tool invocation if model emitted DSML inside content
                val dsmlQuery = if (content.contains("<｜DSML｜ invoke name=\"web_search\">") || content.contains("invoke name=\"web_search\"")) {
                    content.substringAfter("parameter name=\"query\"").substringAfter(">").substringBefore("</").trim()
                        .removePrefix("\"").removeSuffix("\"").trim()
                } else null

                if (toolCalls != null && toolCalls.length() > 0) {
                    messages.put(msg)
                    val callsList = (0 until toolCalls.length()).mapNotNull { i ->
                        val tc = toolCalls.optJSONObject(i) ?: return@mapNotNull null
                        val callId = tc.optString("id", "call_${currentIteration}_$i")
                        val func = tc.optJSONObject("function") ?: return@mapNotNull null
                        val funcName = func.optString("name")
                        val argsStr = func.optString("arguments", "{}")
                        val args = runCatching { JSONObject(argsStr) }.getOrDefault(JSONObject())
                        Triple(callId, funcName, args)
                    }

                    if (callsList.isNotEmpty()) {
                        onProgress?.invoke("Executing ${callsList.size} in-app MCP tool(s) in parallel…")
                        val toolOutputs = executeOpenAiMcpTools(callsList, ctx)

                        toolOutputs.forEach { (callId, nameAndResult) ->
                            val (funcName, toolResult) = nameAndResult
                            messages.put(JSONObject().apply {
                                put("role", "tool")
                                put("tool_call_id", callId)
                                put("name", funcName)
                                put("content", toolResult)
                            })
                        }
                    }

                    val isFinalTurn = currentIteration >= maxToolTurns - 1
                    if (isFinalTurn) {
                        val turnInstruct = if (explicitYear != null) {
                            "Synthesize all discovered facts above now. The user STRICTLY requested titles from $explicitYear. Output ONLY the raw JSON array of $count recommendation objects where EVERY title has year: \"$explicitYear\": [{\"title\":\"...\",\"year\":\"$explicitYear\",\"type\":\"movie\",\"imdb_id\":\"tt...\",\"reason\":\"...\"}]. Filter out and discard any titles from other years. No more tool calls, no conversational text."
                        } else if (isRecency) {
                            "Synthesize all discovered facts above now. Output ONLY the raw JSON array of $count recommendation objects: [{\"title\":\"...\",\"year\":\"$currentYear\",\"type\":\"movie\",\"imdb_id\":\"tt...\",\"reason\":\"...\"}]. No more tool calls, no conversational text."
                        } else {
                            "Synthesize all discovered facts above now. Output ONLY the raw JSON array of $count recommendation objects: [{\"title\":\"...\",\"year\":\"YYYY\",\"type\":\"movie\",\"imdb_id\":\"tt...\",\"reason\":\"...\"}]. Accurately reflect each title's true real-world release year. No more tool calls, no conversational text."
                        }
                        messages.put(JSONObject().apply {
                            put("role", "user")
                            put("content", turnInstruct)
                        })
                    }

                    currentBody = JSONObject().apply {
                        put("model", model)
                        put("messages", messages)
                        put("stream", false)
                        put("max_tokens", maxTokens)
                        if (!isFinalTurn) {
                            put("tools", MatrixMcpEngine.openAiToolsSchema())
                        }
                    }
                } else if (!dsmlQuery.isNullOrBlank()) {
                    searchActions.add("Searched web (DeepSeek DSML): \"$dsmlQuery\"")
                    onProgress?.invoke("AI researching live details: \"$dsmlQuery\"…")
                    val snippets = MatrixWebSearchEngine.search(dsmlQuery, maxResults = 8, forceRefresh = ctx.forceRefresh)
                    val formatted = MatrixWebSearchEngine.formatPromptContext(snippets)
                    messages.put(msg)
                    messages.put(JSONObject().apply {
                        put("role", "user")
                        put("content", "Live search results for \"$dsmlQuery\":\n$formatted\nSynthesize all discovered facts into the final JSON array of $count recommendation objects now. Output raw JSON array only.")
                    })
                    currentBody = JSONObject().apply {
                        put("model", model)
                        put("messages", messages)
                        put("stream", false)
                        put("max_tokens", maxTokens)
                    }
                } else {
                    val fullContent = content.ifBlank { msg?.optString("reasoning_content") }.orEmpty()
                    val recs = parseRecommendations(fullContent, explicitYear)
                    if (recs.isNotEmpty()) {
                        updateSummary(recs, ctx.query, ctx.explicitYear, ctx.isRecency, fullContent)
                        return@withContext recs
                    }
                    break
                }
            }
        }

        // --- 4. PRE-SEARCH FALLBACK / STANDARD INFERENCE ---
        val preSearchSnippets = if (webSearch) {
            val searchQ = when {
                explicitYear != null && !query.contains(explicitYear) -> "$query $explicitYear"
                isRecency && !query.contains(currentYear.toString()) -> "$query $currentYear"
                else -> query
            }
            searchActions.add("Searched web: \"$searchQ\"")
            onProgress?.invoke("Searching live web for \"$searchQ\"…")
            MatrixWebSearchEngine.search(searchQ, maxResults = 8, forceRefresh = ctx.forceRefresh)
        } else {
            emptyList()
        }
        val preSearchContext = MatrixWebSearchEngine.formatPromptContext(preSearchSnippets)
        if (preSearchSnippets.isNotEmpty()) {
            onProgress?.invoke("Found ${preSearchSnippets.size} live web facts. Consulting $model…")
        }

        val fallbackMessages = JSONArray().apply {
            put(JSONObject().apply {
                put("role", "system")
                put("content", "$preSearchContext$systemPrompt")
            })
            put(JSONObject().apply {
                put("role", "user")
                put("content", userPrompt)
            })
        }

        val directBody = JSONObject().apply {
            put("model", model)
            put("messages", fallbackMessages)
            put("stream", false)
            put("max_tokens", maxTokens)
            if (!isReasoning) put("temperature", 0.6)
        }

        onProgress?.invoke("Generating recommendations with $model…")
        val (finalCode, finalResponse) = postJson(endpoint, "Bearer $key", directBody)
        if (finalCode in 200..299) {
            val json = runCatching { JSONObject(finalResponse) }.getOrNull()
            val content = json?.optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("message")?.optString("content") ?: finalResponse
            val recs = parseRecommendations(content, explicitYear)
            updateSummary(recs, ctx.query, ctx.explicitYear, ctx.isRecency, content)
            recs
        } else {
            val err = runCatching { JSONObject(finalResponse).optJSONObject("error")?.optString("message") }.getOrNull() ?: finalResponse.take(120)
            throw IllegalStateException("${provider.label} error ($finalCode): $err")
        }
}
