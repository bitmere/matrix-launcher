package com.matrix.launcher

import android.content.Context
import android.content.Intent
import com.matrix.launcher.core.MediaLinks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Android TV Model Context Protocol (MCP) engine for Matrix Launcher.
 * Provides live, in-process tools that AI models (Neuralwatt, DeepSeek, OpenAI, Groq, OpenRouter, Gemini)
 * can dynamically invoke to search the live web, inspect TV platform context, and check stream availability.
 */
internal object MatrixMcpEngine {

    private val POPULAR_TV_APPS = listOf(
        "com.nuvio.tv" to "Nuvio",
        "com.stremio.one" to "Stremio",
        "com.netflix.ninja" to "Netflix",
        "in.startv.hotstar" to "Disney+ Hotstar",
        "com.amazon.amazonvideo.livingroom" to "Prime Video",
        "com.jio.media.ondemand" to "JioCinema",
        "com.google.android.youtube.tv" to "YouTube",
        "io.gh.reisxd.tizentube.cobalt" to "TizenTube",
        "com.apple.atve.androidtv.appletv" to "Apple TV",
        "com.esaba.downloader" to "Downloader"
    )

    /**
     * Tool definition schema in standard OpenAI / DeepSeek / Neuralwatt / Groq / OpenRouter format.
     */
    fun openAiToolsSchema(): JSONArray = JSONArray().apply {
        put(JSONObject().apply {
            put("type", "function")
            put("function", JSONObject().apply {
                put("name", "web_search")
                put("description", "Searches the live web via DuckDuckGo and Wikipedia for movie and series titles, release years, plot summaries, reviews, and box office. For recent releases, include the current year in the query.")
                put("parameters", JSONObject().apply {
                    put("type", "object")
                    put("properties", JSONObject().apply {
                        put("query", JSONObject().apply {
                            put("type", "string")
                            put("description", "Specific search query for the live web (e.g. 'best cyberpunk movies' or 'movies in theatres 2026').")
                        })
                    })
                    put("required", JSONArray().apply { put("query") })
                })
            })
        })

        put(JSONObject().apply {
            put("type", "function")
            put("function", JSONObject().apply {
                put("name", "tv_context")
                put("description", "Returns the Android TV environment context: current calendar month and year, current year, and installed streaming/playback apps on this TV.")
                put("parameters", JSONObject().apply {
                    put("type", "object")
                    put("properties", JSONObject())
                })
            })
        })

        put(JSONObject().apply {
            put("type", "function")
            put("function", JSONObject().apply {
                put("name", "lookup_stream_availability")
                put("description", "Checks if a movie or series is available in the Cinemeta / Stremio media catalog with valid streams and IMDb rating.")
                put("parameters", JSONObject().apply {
                    put("type", "object")
                    put("properties", JSONObject().apply {
                        put("title", JSONObject().apply {
                            put("type", "string")
                            put("description", "Official title of the movie or series")
                        })
                        put("year", JSONObject().apply {
                            put("type", "string")
                            put("description", "4-digit release year")
                        })
                    })
                    put("required", JSONArray().apply { put("title") })
                })
            })
        })
    }

    /**
     * Tool definition schema for Google Gemini REST API.
     */
    fun geminiToolsSchema(): JSONArray = JSONArray().apply {
        put(JSONObject().apply {
            put("functionDeclarations", JSONArray().apply {
                put(JSONObject().apply {
                    put("name", "web_search")
                    put("description", "Searches the live web via DuckDuckGo and Wikipedia for movie and series titles, release years, plot summaries, and reviews.")
                    put("parameters", JSONObject().apply {
                        put("type", "OBJECT")
                        put("properties", JSONObject().apply {
                            put("query", JSONObject().apply {
                                put("type", "STRING")
                                put("description", "Specific search query for the live web")
                            })
                        })
                        put("required", JSONArray().apply { put("query") })
                    })
                })
                put(JSONObject().apply {
                    put("name", "tv_context")
                    put("description", "Returns current TV context: today's calendar date, current year, and installed streaming apps on this Android TV.")
                    put("parameters", JSONObject().apply {
                        put("type", "OBJECT")
                        put("properties", JSONObject())
                    })
                })
                put(JSONObject().apply {
                    put("name", "lookup_stream_availability")
                    put("description", "Checks if a movie or series is available in Cinemeta / Stremio media catalog with valid streams and IMDb rating.")
                    put("parameters", JSONObject().apply {
                        put("type", "OBJECT")
                        put("properties", JSONObject().apply {
                            put("title", JSONObject().apply {
                                put("type", "STRING")
                                put("description", "Official title of the movie or series")
                            })
                            put("year", JSONObject().apply {
                                put("type", "STRING")
                                put("description", "4-digit release year")
                            })
                        })
                        put("required", JSONArray().apply { put("title") })
                    })
                })
            })
        })
    }

    /**
     * Executes an MCP tool call and returns the tool output string.
     * With [forceRefresh] the web_search tool bypasses the web-search cache.
     */
    suspend fun executeTool(name: String, args: JSONObject, context: Context?, forceRefresh: Boolean = false): String = withContext(Dispatchers.IO) {
        when (name) {
            "web_search" -> {
                val query = args.optString("query", "").trim()
                if (query.isBlank()) return@withContext "No search query provided."
                val snippets = MatrixWebSearchEngine.search(query, maxResults = 8, forceRefresh = forceRefresh)
                if (snippets.isEmpty()) {
                    "No web search results found for: \"$query\"."
                } else {
                    MatrixWebSearchEngine.formatPromptContext(snippets)
                }
            }
            "tv_context" -> {
                getTvContext(context)
            }
            "lookup_stream_availability" -> {
                val title = args.optString("title", "").trim()
                val year = args.optString("year", "").trim()
                checkStreamAvailability(title, year)
            }
            else -> "Unknown tool: $name"
        }
    }

    fun getTvContext(context: Context?): String {
        val cal = Calendar.getInstance()
        val year = cal.get(Calendar.YEAR)
        val monthYear = SimpleDateFormat("MMMM yyyy", Locale.US).format(Date())

        val installedList = mutableListOf<String>()
        if (context != null) {
            val pm = context.packageManager
            for ((pkg, label) in POPULAR_TV_APPS) {
                runCatching {
                    if (pm.getLaunchIntentForPackage(pkg) != null) {
                        installedList.add(label)
                    }
                }
            }
        }
        if (installedList.isEmpty()) {
            installedList.addAll(listOf("Nuvio", "Stremio"))
        }

        return JSONObject().apply {
            put("current_date", monthYear)
            put("current_year", year)
            put("platform", "Android TV (Matrix Launcher)")
            put("installed_media_apps", JSONArray(installedList))
            put("stream_support", "Native Nuvio and Stremio deep linking with IMDb ID support")
        }.toString(2)
    }

    private fun checkStreamAvailability(title: String, year: String): String {
        return CinemetaClient.checkAvailability(title, year)
    }
}
