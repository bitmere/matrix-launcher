package com.matrix.launcher

import android.content.SharedPreferences
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class MatrixSearchTest {
    /** Read-only SharedPreferences returning zero-values by return type,
     * for tests where production code only reads config (MatrixAiClient). */
    private fun dummyPrefs(): SharedPreferences =
        java.lang.reflect.Proxy.newProxyInstance(
            SharedPreferences::class.java.classLoader,
            arrayOf(SharedPreferences::class.java)
        ) { _, method, _ ->
            when (method.returnType) {
                Boolean::class.javaPrimitiveType -> false
                Int::class.javaPrimitiveType -> 0
                Long::class.javaPrimitiveType -> 0L
                Float::class.javaPrimitiveType -> 0f
                String::class.java -> ""
                else -> null
            }
        } as SharedPreferences

    @Test fun stremioMappingRequiresAValidImdbIdentity() {
        assertEquals("tt42", tmdbImdbId(JSONObject("""{"imdb_id":"tt42"}""")))
        for (value in listOf("null", "42", "\"tmdb:42\"", "\"tt42/other\""))
            assertNull(tmdbImdbId(JSONObject("{\"imdb_id\":$value}")))
        assertNull(tmdbImdbId(JSONObject()))
    }
    @Test fun malformedSimklEntriesDoNotDiscardValidSearchOrListResults() {
        val values = org.json.JSONArray("""[null,7,"bad",{},
            {"title":"Valid","endpoint_type":"movies","ids":{"imdb":"tt42"}},
            {"title":"No identifier","endpoint_type":"movies"}]""")
        assertEquals(1, SimklClient.searchPage(JSONObject().put("items", values)).items.size)
        assertEquals(1, SimklClient.mediaItems(values, "movies", 24).size)
        assertTrue(SimklClient.mediaItems(values, "movies", 4).isEmpty())
        assertTrue(SimklClient.mediaItems(null, "movies", 24).isEmpty())
        val library = org.json.JSONArray("""[null,5,{"status":"watching","show":{"title":"Missing ID"}},
            {"status":"watching","show":{"title":"Valid","ids":{"imdb":"tt42"}}}]""")
        assertEquals(1, SimklClient.libraryPage(library, "shows", "watching", 1).items.size)
        assertTrue(SimklClient.libraryPage(library, "shows", "watching", 2).items.isEmpty())
    }
    @Test fun simklMixedSearchKeepsEachTitlesTypeAndRejectsUnusableIds() {
        val response = SimklClient.parseResponse("""[
            {"title":"Movie","year":2022,"endpoint_type":"movies","ids":{"tmdb":"42"},"poster":null,"ratings":{"simkl":{"rating":8.1,"votes":12},"imdb":{"rating":7.8,"votes":25},"mal":{"rating":0,"votes":0}}},
            {"title":"Show","endpoint_type":"tv","ids":{"simkl_id":43},"poster":"1/show","status":"ended","ep_count":85},
            {"title":"Anime film","endpoint_type":"anime","type":"movie","ids":{"tmdb":"44"}},
            {"title":"No ID","endpoint_type":"movies"},
            {"title":"Unknown kind","endpoint_type":"person","ids":{"tmdb":"45"}}
        ]""")
        val items = SimklClient.searchPage(response).items
        assertEquals(3, items.size)
        assertEquals("matrix-title://meta/movie/tmdb%3A42", items[0].intent)
        assertEquals("", items[0].poster)
        assertEquals("Movie", items[0].title)
        assertEquals("2022\nSIMKL 8.1 · IMDb 7.8", items[0].metadata)
        assertEquals("Movie", items[0].mediaType)
        assertEquals("Series", items[1].mediaType)
        assertEquals("Ended · 85 episodes", items[1].summary)
        assertEquals("Anime movie", items[2].mediaType)
        assertEquals("matrix-title://meta/series/simkl%3A43", items[1].intent)
        assertEquals("matrix-title://meta/movie/tmdb%3A44", items[2].intent)
        assertEquals("expired_token", SimklClient.parseResponse("""{"error":"expired_token"}""").getString("error"))
        assertTrue(SimklClient.searchPage(SimklClient.parseResponse("[]")).items.isEmpty())
    }
    @Test fun tmdbFiltersPeopleAndBuildsTypedLinksWithoutExtraLookupCalls() {
        val cards = tmdbCards(JSONObject("""{"results":[
            {"media_type":"movie","id":42,"title":"Movie","release_date":"2022-01-01","poster_path":"/valid.jpg","vote_average":7.6,"vote_count":15,"overview":"A short synopsis."},
            {"media_type":"tv","id":43,"name":"Series","poster_path":null},
            {"media_type":"person","id":44,"name":"Actor"},
            {"media_type":"movie","id":0,"title":"Invalid"},
            {"media_type":"movie","id":45,"title":"Bad image","poster_path":"https://elsewhere/image"}
        ]}"""))
        assertEquals(3, cards.size)
        assertEquals("matrix-title://meta/movie/tmdb%3A42", cards[0].link)
        assertEquals("https://image.tmdb.org/t/p/w342/valid.jpg", cards[0].image)
        assertEquals("Movie", cards[0].title)
        assertEquals("2022\nTMDB 7.6", cards[0].metadata)
        assertEquals("Movie", cards[0].mediaType)
        assertEquals("A short synopsis.", cards[0].summary)
        assertEquals("Series", cards[1].mediaType)
        assertEquals("", cards[1].summary)
        assertEquals("matrix-title://meta/series/tmdb%3A43", cards[1].link)
        assertEquals("", cards[1].image)
        assertEquals("", cards[2].image)
        assertTrue(tmdbCards(JSONObject("""{"results":[]}""")).isEmpty())
    }

    @Test fun aiRecommendationsParseCleanlyWithAndWithoutReasoningTags() {
        val client = MatrixAiClient(dummyPrefs())
        val withThink = """
            <think>
            The user wants mind-bending sci-fi movies. Let me consider Inception, Interstellar, and Dark.
            </think>
            ```json
            [
                {"title": "Inception", "year": "2010", "type": "movie", "imdb_id": "tt1375666", "reason": "Dream-within-a-dream thriller."},
                {"title": "Dark", "year": "2017", "type": "series", "imdb_id": "tt5753856", "reason": "Complex time-travel mystery."}
            ]
            ```
        """.trimIndent()
        val parsed = client.parseRecommendations(withThink)
        assertEquals(2, parsed.size)
        assertEquals("Inception", parsed[0].title)
        assertEquals("2010", parsed[0].year)
        assertEquals("movie", parsed[0].type)
        assertEquals("tt1375666", parsed[0].imdbId)
        assertEquals("Dream-within-a-dream thriller.", parsed[0].reason)

        assertEquals("Dark", parsed[1].title)
        assertEquals("2017", parsed[1].year)
        assertEquals("series", parsed[1].type)
        assertEquals("tt5753856", parsed[1].imdbId)

        // Unclosed think tag (e.g. truncated stream) returns empty
        val truncated = "<think>The user wants..."
        assertTrue(client.parseRecommendations(truncated).isEmpty())

        // Raw JSON without fences
        val rawJson = """[{"title": "Primer", "year": "2004", "type": "movie", "reason": "Low-budget time travel."}]"""
        val parsedRaw = client.parseRecommendations(rawJson)
        assertEquals(1, parsedRaw.size)
        assertEquals("Primer", parsedRaw[0].title)
        assertEquals("", parsedRaw[0].imdbId)

        // Surrounding conversational text and markdown
        val conversational = """
            Here are my curated recommendations for you:
            ```json
            [
                {"title": "Solaris", "year": "1972", "type": "movie", "reason": "Philosophical sci-fi.", "imdb_id": "tt0069293"},
                {"title": "", "year": "2020", "type": "movie", "reason": "Blank title should be ignored"},
                {"title": "Severance", "year": "2022", "type": "tv show", "reason": "Dystopian workplace thriller."}
            ]
            ```
            Hope you enjoy watching these!
        """.trimIndent()
        val parsedConv = client.parseRecommendations(conversational)
        assertEquals(2, parsedConv.size)
        assertEquals("Solaris", parsedConv[0].title)
        assertEquals("tt0069293", parsedConv[0].imdbId)
        assertEquals("Severance", parsedConv[1].title)
        assertEquals("series", parsedConv[1].type)

        // Malformed JSON syntax gracefully returns empty list without throwing
        val malformed = """[{"title": "Broken", "year": ]"""
        assertTrue(client.parseRecommendations(malformed).isEmpty())
        assertTrue(client.parseRecommendations("No JSON array here").isEmpty())
    }

    @Test fun extractExplicitYearDetectsRealisticReleaseYearsAndIgnoresFuturisticTitles() {
        assertEquals("2026", extractExplicitYear("2026 thrillers"))
        assertEquals("2026", extractExplicitYear("best sci-fi movies from 2026"))
        assertEquals("1999", extractExplicitYear("top action 1999"))
        assertEquals("2024", extractExplicitYear("2024 horror"))
        assertNull(extractExplicitYear("thrillers"))
        assertNull(extractExplicitYear("blade runner 2049")) // futuristic year 2049 is beyond calendar window
        assertNull(extractExplicitYear("wong kar-wai 2046"))
    }

    @Test fun parseRecommendationsEnforcesExplicitYearStrictlyDroppingLeakage() {
        val client = MatrixAiClient(dummyPrefs())

        val mixedJson = """
            [
                {"title": "The Substance", "year": "2024", "type": "movie", "reason": "Body horror."},
                {"title": "Sinners", "year": "2025", "type": "movie", "reason": "Ryan Coogler vampire horror."},
                {"title": "Project Hail Mary", "year": "2026", "type": "movie", "reason": "Andy Weir sci-fi."},
                {"title": "Avengers: Secret Wars", "year": "2026", "type": "movie", "reason": "Marvel multiverse climax."}
            ]
        """.trimIndent()

        // When explicitYear is null, all items are parsed
        val allParsed = client.parseRecommendations(mixedJson, null)
        assertEquals(4, allParsed.size)

        // When explicitYear is "2026", 2024 and 2025 movies are strictly dropped
        val strict2026 = client.parseRecommendations(mixedJson, "2026")
        assertEquals(2, strict2026.size)
        assertEquals("Project Hail Mary", strict2026[0].title)
        assertEquals("2026", strict2026[0].year)
        assertEquals("Avengers: Secret Wars", strict2026[1].title)
        assertEquals("2026", strict2026[1].year)
    }

    @Test fun parseRecommendationsExtractsAiPlainLanguageSummary() {
        val client = MatrixAiClient(dummyPrefs())

        val jsonWithSummary = """
            {
                "summary": "I researched confirmed 2026 theatrical and streaming releases, focusing on anticipated sci-fi thrillers and verifying direct playback availability.",
                "recommendations": [
                    {"title": "Project Hail Mary", "year": "2026", "type": "movie", "imdb_id": "tt12042772", "reason": "Andy Weir astronaut mission"}
                ]
            }
        """.trimIndent()

        val recs = client.parseRecommendations(jsonWithSummary, "2026")
        assertEquals(1, recs.size)
        assertEquals("Project Hail Mary", recs[0].title)
        assertEquals("I researched confirmed 2026 theatrical and streaming releases, focusing on anticipated sci-fi thrillers and verifying direct playback availability.", client.lastExtractedSummary)
    }

    @Test fun aiProviderDefaultsAndModels() {
        // (provider, default URL, default model, expected recommended models)
        data class ProviderDefaults(
            val provider: MatrixAiProvider,
            val url: String,
            val model: String,
            val models: List<String>
        )
        val cases = listOf(
            ProviderDefaults(MatrixAiProvider.DEEPSEEK, "https://api.deepseek.com", "deepseek-chat",
                listOf("deepseek-chat", "deepseek-reasoner")),
            ProviderDefaults(MatrixAiProvider.PERPLEXITY, "https://api.perplexity.ai/v1", "sonar",
                listOf("sonar", "sonar-pro")),
            ProviderDefaults(MatrixAiProvider.GEMINI, "https://generativelanguage.googleapis.com/v1beta", "gemini-2.5-flash",
                listOf("gemini-2.5-flash", "gemini-2.5-pro", "gemini-2.0-flash"))
        )
        for ((provider, url, model, expected) in cases) {
            assertEquals(url, provider.defaultUrl)
            assertEquals(model, provider.defaultModel)
            for (id in expected) assertTrue(provider.recommendedModels.any { it.id == id })
        }
    }

    @Test fun webSearchEngineCleansHtmlAndParsesSnippets() {
        assertEquals("Hello & world", MatrixWebSearchEngine.cleanHtml("<b>Hello</b> &amp; <i>world</i>"))
        assertEquals("Tom's \"Movie\"", MatrixWebSearchEngine.cleanHtml("Tom&#039;s &quot;Movie&quot;"))
        assertEquals("Director's ‘Masterpiece’ – 2025…", MatrixWebSearchEngine.cleanHtml("Director&#39;s &#8216;Masterpiece&#8217; &#8211; 2025&hellip;"))
        assertEquals("Film #42: “Great”", MatrixWebSearchEngine.cleanHtml("Film &#35;42: &#8220;Great&#8221;"))

        val mockHtml = """
            <h2 class="result__title"><a class="result__a" href="http://example.com/1">Movie Title 1</a></h2>
            <a class="result__snippet" href="http://example.com/1">This is a great 2026 Telugu film snippet.</a>
            <h2 class="result__title"><a class="result__a" href="http://example.com/2">Movie Title 2</a></h2>
            <a class="result__snippet" href="http://example.com/2">Short</a>
        """.trimIndent()
        val snippets = MatrixWebSearchEngine.parseDuckDuckGoHtml(mockHtml)
        assertEquals(1, snippets.size)
        assertEquals("Movie Title 1", snippets[0].title)
        assertEquals("This is a great 2026 Telugu film snippet.", snippets[0].snippet)
        assertEquals("DuckDuckGo", snippets[0].source)
    }

    @Test fun allAiProvidersHaveValidConfiguration() {
        for (provider in MatrixAiProvider.entries) {
            assertTrue("Provider ${provider.name} label should not be blank", provider.label.isNotBlank())
            assertTrue("Provider ${provider.name} defaultUrl should not be blank", provider.defaultUrl.isNotBlank())
            assertTrue("Provider ${provider.name} defaultModel should not be blank", provider.defaultModel.isNotBlank())
            assertTrue("Provider ${provider.name} description should not be blank", provider.description.isNotBlank())
            assertTrue("Provider ${provider.name} should have recommended models", provider.recommendedModels.isNotEmpty())
            for (model in provider.recommendedModels) {
                assertTrue("Model ID should not be blank in ${provider.name}", model.id.isNotBlank())
                assertTrue("Model label should not be blank in ${provider.name}", model.label.isNotBlank())
            }
        }
    }

    @Test fun webSearchEngineParsesWikipediaJson() {
        val mockJson = """
            {
                "query": {
                    "search": [
                        {
                            "title": "List of Telugu films of 2025",
                            "snippet": "A list of <span class=\"searchmatch\">Telugu</span> films released in 2025."
                        }
                    ]
                }
            }
        """.trimIndent()
        val parsed = MatrixWebSearchEngine.parseWikipediaJson(mockJson)
        assertEquals(1, parsed.size)
        assertEquals("List of Telugu films of 2025", parsed[0].title)
        assertEquals("A list of Telugu films released in 2025.", parsed[0].snippet)
        assertEquals("Wikipedia", parsed[0].source)
    }

    @Test fun webSearchEngineFormatsPromptContext() {
        val snippets = listOf(
            WebSnippet("Pushpa 2", "Action film releasing in December 2024.", "DuckDuckGo"),
            WebSnippet("Game Changer", "Political thriller releasing in January 2025.", "Wikipedia")
        )
        val formatted = MatrixWebSearchEngine.formatPromptContext(snippets)
        assertTrue(formatted.contains("LIVE REAL-TIME WEB CONTEXT"))
        assertTrue(formatted.contains("[Pushpa 2]: Action film releasing in December 2024."))
        assertTrue(formatted.contains("[Game Changer]: Political thriller releasing in January 2025."))
    }

    @Test fun webSearchCacheEvictsEldestPastThirtyTwoEntries() {
        // BoundedCache backs the web-search cache (D-05): 32 entries, LRU.
        val cache = BoundedCache<String, Int>(32)
        for (i in 0 until 33) cache["q$i"] = i
        assertEquals(32, cache.size)
        assertFalse(cache.containsKey("q0"))
        assertTrue(cache.containsKey("q32"))

        // Access-order: touching q1 keeps it past the next eviction.
        assertEquals(1, cache["q1"])
        cache["q33"] = 33
        assertEquals(32, cache.size)
        assertTrue(cache.containsKey("q1"))
        assertFalse(cache.containsKey("q2"))
    }

    @Test fun concurrentModelsCacheAccessSafety() {
        // Map-backed prefs so writes round-trip: the read-only dummy returns
        // "" for every getString, which the provider getter maps to its
        // NEURALWATT default. A ConcurrentHashMap keeps the backing store
        // thread-safe, so the test exercises the client's own thread-safety.
        val client = MatrixAiClient(mapBackedPrefs(java.util.concurrent.ConcurrentHashMap()))
        val threads = (1..8).map {
            Thread {
                repeat(50) {
                    client.provider = MatrixAiProvider.DEEPSEEK
                    client.apiKey = "sk-test"
                    client.customUrl = "https://custom.endpoint.com"
                }
            }
        }
        threads.forEach { it.start() }
        threads.forEach { it.join() }
        // All threads wrote the same values, so the final state is deterministic.
        assertEquals(MatrixAiProvider.DEEPSEEK, client.provider)
        assertEquals("sk-test", client.apiKey)
        assertEquals("https://custom.endpoint.com", client.customUrl)
    }

    @Test fun mcpEngineToolSchemasListAllThreeTools() {
        val expected = listOf("web_search", "tv_context", "lookup_stream_availability")
        val openAiTools = MatrixMcpEngine.openAiToolsSchema()
        assertEquals(3, openAiTools.length())
        val openAiNames = (0 until openAiTools.length()).map {
            openAiTools.getJSONObject(it).getJSONObject("function").getString("name")
        }
        assertTrue(openAiNames.containsAll(expected))

        val geminiTools = MatrixMcpEngine.geminiToolsSchema()
        assertEquals(1, geminiTools.length())
        val decls = geminiTools.getJSONObject(0).getJSONArray("functionDeclarations")
        assertEquals(3, decls.length())
        val geminiNames = (0 until decls.length()).map { decls.getJSONObject(it).getString("name") }
        assertTrue(geminiNames.containsAll(expected))
    }

    @Test fun mcpEngineTvContextProducesValidJson() {
        val contextJson = MatrixMcpEngine.getTvContext(null)
        val obj = JSONObject(contextJson)
        assertTrue(obj.has("current_date"))
        assertTrue(obj.has("current_year"))
        assertTrue(obj.getInt("current_year") >= 2025)
        assertTrue(obj.getJSONArray("installed_media_apps").length() > 0)
    }

    @Test fun mcpEngineExecuteToolRoutesKnownAndUnknownTools() = kotlinx.coroutines.runBlocking {
        val res = MatrixMcpEngine.executeTool("tv_context", JSONObject(), null)
        val obj = JSONObject(res)
        assertTrue(obj.has("current_date"))
        assertTrue(obj.has("installed_media_apps"))

        assertEquals("Unknown tool: unknown_tool", MatrixMcpEngine.executeTool("unknown_tool", JSONObject(), null))
    }

    @Test fun recentSearchesAddDeduplicateAndClear() {
        val prefs = mapBackedPrefs()
        val repo = MatrixSearchRepository(prefs)
        assertTrue(repo.recentSearches().isEmpty())

        repo.addRecentSearch("Interstellar")
        repo.addRecentSearch("Pushpa 2")
        repo.addRecentSearch("Interstellar")

        val recents = repo.recentSearches()
        assertEquals(2, recents.size)
        assertEquals("Interstellar", recents[0])
        assertEquals("Pushpa 2", recents[1])

        repo.clearRecentSearches()
        assertTrue(repo.recentSearches().isEmpty())
    }

    /** No live data: exercises the offline fallback path of fetchDynamicAiSuggestions() with no network. */
    private object EmptySuggestionsSource : DynamicSuggestionsSource {
        override fun topMetas(type: String) = emptyList<JSONObject>()
        override suspend fun webSearch(query: String, maxResults: Int) = emptyList<WebSnippet>()
    }

    @Test fun dynamicAiSuggestionsProduceContextualQueries() = kotlinx.coroutines.runBlocking {
        val map = mutableMapOf<String, Any>()
        val prefs = java.lang.reflect.Proxy.newProxyInstance(
            android.content.SharedPreferences::class.java.classLoader,
            arrayOf(android.content.SharedPreferences::class.java)
        ) { _, method, args ->
            when (method.name) {
                "getString" -> map[args[0] as String] ?: args[1]
                else -> null
            }
        } as android.content.SharedPreferences

        val repo = MatrixSearchRepository(prefs, suggestionsSource = EmptySuggestionsSource)
        val suggestions = repo.fetchDynamicAiSuggestions()
        assertTrue(suggestions.isNotEmpty())
        assertTrue(suggestions.size in 1..6)
        assertTrue(suggestions.all { it.isNotBlank() })
    }

    @Test fun dynamicAiSuggestionsUseLiveGroundingWhenAvailable() = kotlinx.coroutines.runBlocking {
        val year = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
        val source = object : DynamicSuggestionsSource {
            override fun topMetas(type: String) = listOf(
                JSONObject("""{"name":"Test Movie","genres":["Sci-Fi"]}""")
            )
            override suspend fun webSearch(query: String, maxResults: Int) =
                listOf(WebSnippet(title = "Test Movie review", snippet = ""))
        }
        val repo = MatrixSearchRepository(mapBackedPrefs(), suggestionsSource = source)
        val suggestions = repo.fetchDynamicAiSuggestions()
        assertTrue(suggestions.contains("Movies like Test Movie"))
        assertTrue(suggestions.contains("Top Sci-Fi movies $year"))
        assertTrue(suggestions.size in 1..6)
        assertTrue(suggestions.all { it.isNotBlank() })
    }

    @Test fun aiResultCountPersistsAndClampsRange() {
        val prefs = mapBackedPrefs()
        val client = MatrixAiClient(prefs)
        assertEquals(12, client.resultCount)

        client.resultCount = 100
        assertEquals(100, client.resultCount)

        client.resultCount = 50
        assertEquals(50, client.resultCount)

        client.resultCount = 200
        assertEquals(100, client.resultCount)

        client.resultCount = 2
        assertEquals(5, client.resultCount)
    }

    @Test fun cinemetaCatalogSearchParsesMetasAndFiltersInvalidIds() {
        val json = JSONObject("""{
            "metas": [
                {
                    "id": "tt0133093",
                    "type": "movie",
                    "name": "The Matrix",
                    "poster": "https://images.metahub.space/poster/medium/tt0133093/img.jpg",
                    "releaseInfo": "1999",
                    "imdbRating": "8.7",
                    "description": "A computer hacker learns from mysterious rebels about the true nature of his reality."
                },
                {
                    "id": "tt0903747",
                    "type": "series",
                    "name": "Breaking Bad",
                    "poster": "https://images.metahub.space/poster/medium/tt0903747/img.jpg",
                    "releaseInfo": "2008-2013",
                    "imdbRating": "9.5",
                    "description": "A chemistry teacher diagnosed with cancer turns to manufacturing methamphetamine."
                },
                {
                    "id": "tt1234567",
                    "type": "tv",
                    "name": "Anime Series",
                    "releaseInfo": "2024"
                },
                {
                    "id": "kitsu:1234",
                    "type": "series",
                    "name": "Filtered non-IMDb ID"
                },
                {
                    "id": "42",
                    "type": "movie",
                    "name": "Filtered numeric ID"
                },
                null
            ]
        }""")
        val cards = CinemetaClient.parseCards(json)
        assertEquals(3, cards.size)

        // Item 0: Movie
        assertEquals("cinemeta:tt0133093", cards[0].id)
        assertEquals("The Matrix", cards[0].title)
        assertEquals("matrix-title://meta/movie/tt0133093", cards[0].link)
        assertEquals("https://images.metahub.space/poster/medium/tt0133093/img.jpg", cards[0].image)
        assertEquals(com.matrix.launcher.core.MediaLinks.SOURCE, cards[0].source)
        assertEquals("1999 · IMDb 8.7", cards[0].metadata)
        assertEquals("Movie", cards[0].mediaType)
        assertTrue(cards[0].summary.contains("computer hacker"))

        // Item 1: Series
        assertEquals("cinemeta:tt0903747", cards[1].id)
        assertEquals("Breaking Bad", cards[1].title)
        assertEquals("matrix-title://meta/series/tt0903747", cards[1].link)
        assertEquals("2008-2013 · IMDb 9.5", cards[1].metadata)
        assertEquals("Series", cards[1].mediaType)

        // Item 2: "tv" mapped to "series"
        assertEquals("cinemeta:tt1234567", cards[2].id)
        assertEquals("Series", cards[2].mediaType)
        assertEquals("matrix-title://meta/series/tt1234567", cards[2].link)
        assertEquals("2024", cards[2].metadata)

        // Empty response
        assertTrue(CinemetaClient.parseCards(JSONObject()).isEmpty())
        assertTrue(CinemetaClient.parseCards(JSONObject("""{"metas":[]}""")).isEmpty())
    }

    @Test fun topMetasParseKeepsOnlyTheRequestedItems() {
        val json = JSONObject("""{"metas":[
            {"name":"A"},{"name":"B"},{"name":"C"},{"name":"D"},{"name":"E"}
        ]}""")
        val kept = CinemetaClient.parseTopMetas(json, 2)
        assertEquals(listOf("A", "B"), kept.map { it.optString("name") })
        assertEquals(5, CinemetaClient.parseTopMetas(json, Int.MAX_VALUE).size)
        assertTrue(CinemetaClient.parseTopMetas(JSONObject(), 2).isEmpty())
    }

    @Test fun extractRatingBadgeParsesVariousFormats() {
        assertEquals("★ 8.7", extractRatingBadge("1999 · IMDb 8.7"))
        assertEquals("★ 9.5", extractRatingBadge("2008-2013 · IMDb 9.5"))
        assertEquals("★ 7.6", extractRatingBadge("2022\nTMDB 7.6"))
        assertEquals("★ 8.1", extractRatingBadge("2022 · SIMKL 8.1"))
        assertEquals("★ 7.8", extractRatingBadge("2022\nSIMKL 8.1 · IMDb 7.8"))
        assertEquals("★ 8.4", extractRatingBadge("★ 8.4"))
        assertEquals("★ 8.5", extractRatingBadge("8.5/10"))
        assertNull(extractRatingBadge("2026 · Upcoming"))
        assertNull(extractRatingBadge("2024 · Movie"))
        assertNull(extractRatingBadge("2024"))
        assertNull(extractRatingBadge(""))
    }

    @Test fun catalogProviderIsEnabledAndReadyByDefault() {
        val prefs = mapBackedPrefs()
        val repo = MatrixSearchRepository(prefs)

        // CATALOG is enabled and ready with zero setup
        assertTrue(repo.enabled(MatrixSearchProvider.CATALOG))
        assertTrue(repo.ready(MatrixSearchProvider.CATALOG))
        assertTrue(repo.providers().contains(MatrixSearchProvider.CATALOG))

        // Other providers require setup/keys
        assertFalse(repo.enabled(MatrixSearchProvider.SIMKL))
        assertFalse(repo.ready(MatrixSearchProvider.SIMKL))
        assertFalse(repo.enabled(MatrixSearchProvider.TMDB))
        assertFalse(repo.ready(MatrixSearchProvider.TMDB))
        assertFalse(repo.enabled(MatrixSearchProvider.AI))

        // Providers list defaults to just CATALOG
        assertEquals(listOf(MatrixSearchProvider.CATALOG), repo.providers())

        // User can disable CATALOG if search is not desired
        repo.enable(MatrixSearchProvider.CATALOG, false)
        assertFalse(repo.enabled(MatrixSearchProvider.CATALOG))
        assertTrue(repo.providers().isEmpty())

        // And re-enable it
        repo.enable(MatrixSearchProvider.CATALOG, true)
        assertTrue(repo.enabled(MatrixSearchProvider.CATALOG))
        assertEquals(listOf(MatrixSearchProvider.CATALOG), repo.providers())
    }

    @Test fun appSearchFilteringCaseInsensitiveAndTrimsWhitespace() {
        val apps = listOf(
            LaunchApp("com.stremio.one", "Stremio"),
            LaunchApp("com.nuvio.tv", "Nuvio"),
            LaunchApp("com.netflix.ninja", "Netflix"),
            LaunchApp("com.google.android.youtube.tv", "YouTube"),
            LaunchApp("org.videolan.vlc", "VLC")
        )

        // Query matching title prefix or substring
        val stremioResults = filterApps(apps, "stre")
        assertEquals(1, stremioResults.size)
        assertEquals("Stremio", stremioResults[0].title)

        // Trims whitespace and case-insensitive
        val nuvioResults = filterApps(apps, "   NUVIO  ")
        assertEquals(1, nuvioResults.size)
        assertEquals("Nuvio", nuvioResults[0].title)

        // Multi-match substring with exact and prefix ranking
        val tResults = filterApps(apps, "t")
        assertEquals(3, tResults.size)
        // Netflix and Stremio contain 't', YouTube starts with 'You' and contains 't'
        assertTrue(tResults.any { it.title == "Stremio" })
        assertTrue(tResults.any { it.title == "Netflix" })
        assertTrue(tResults.any { it.title == "YouTube" })

        // Exact match comes before substring match
        val appsWithExact = listOf(
            LaunchApp("com.other.stremio", "Best Stremio Helper"),
            LaunchApp("com.stremio.one", "Stremio"),
            LaunchApp("com.stremio.addon", "Stremio Addon")
        )
        val ranked = filterApps(appsWithExact, "Stremio")
        assertEquals("Stremio", ranked[0].title)
        assertEquals("Stremio Addon", ranked[1].title)
        assertEquals("Best Stremio Helper", ranked[2].title)

        // Blank or whitespace-only returns empty
        assertTrue(filterApps(apps, "").isEmpty())
        assertTrue(filterApps(apps, "   ").isEmpty())

        // No matches returns empty
        assertTrue(filterApps(apps, "Kodi").isEmpty())
    }

    @Test fun rankCinemetaResultsInterleavesMoviesAndSeriesWithTieredPrecision() {
        val movies = listOf(
            MediaCard("cinemeta:tt0133093", "The Matrix", "", "matrix-title://meta/movie/tt0133093", com.matrix.launcher.core.MediaLinks.SOURCE, true, 0, 0),
            MediaCard("cinemeta:tt0234215", "The Matrix Reloaded", "", "matrix-title://meta/movie/tt0234215", com.matrix.launcher.core.MediaLinks.SOURCE, true, 0, 0),
            MediaCard("cinemeta:tt9847360", "A Glitch in the Matrix", "", "matrix-title://meta/movie/tt9847360", com.matrix.launcher.core.MediaLinks.SOURCE, true, 0, 0)
        )
        val series = listOf(
            MediaCard("cinemeta:tt30849138", "Matrix: Generation", "", "matrix-title://meta/series/tt30849138", com.matrix.launcher.core.MediaLinks.SOURCE, true, 0, 0),
            MediaCard("cinemeta:tt27796295", "The Religion Matrix", "", "matrix-title://meta/series/tt27796295", com.matrix.launcher.core.MediaLinks.SOURCE, true, 0, 0)
        )

        // Searching "matrix" should rank "The Matrix" (exact without 'The') above prefix ("Matrix: Generation")
        // and both above contains ("A Glitch in the Matrix")
        val ranked = CinemetaClient.rankResults(movies, series, "matrix")
        assertEquals("The Matrix", ranked[0].title)
        assertEquals("cinemeta:tt0133093", ranked[0].id)
        // Prefix matches follow
        assertTrue(ranked[1].title in listOf("The Matrix Reloaded", "Matrix: Generation"))
        assertTrue(ranked[2].title in listOf("The Matrix Reloaded", "Matrix: Generation"))

        // Searching a franchise without exact matches interleaves movies and series
        val swMovies = listOf(
            MediaCard("m1", "Star Wars: A New Hope", "", "", "", true, 0, 0),
            MediaCard("m2", "Star Wars: Empire", "", "", "", true, 0, 0)
        )
        val swSeries = listOf(
            MediaCard("s1", "Star Wars: The Clone Wars", "", "", "", true, 0, 0),
            MediaCard("s2", "Star Wars: Andor", "", "", "", true, 0, 0)
        )
        val interleaved = CinemetaClient.rankResults(swMovies, swSeries, "Star Wars")
        assertEquals(4, interleaved.size)
        assertEquals("m1", interleaved[0].id)
        assertEquals("s1", interleaved[1].id)
        assertEquals("m2", interleaved[2].id)
        assertEquals("s2", interleaved[3].id)
    }

    @Test fun isRecencyQueryDistinguishesNewReleasesFromTimelessGenres() {
        assertTrue(isRecencyQuery("latest action movies"))
        assertTrue(isRecencyQuery("new sci-fi releases"))
        assertTrue(isRecencyQuery("movies in theatres"))
        assertTrue(isRecencyQuery("top box office hits this year"))
        assertTrue(isRecencyQuery("recent telugu films"))
        assertTrue(isRecencyQuery("upcoming marvel movies"))

        assertFalse(isRecencyQuery("cyberpunk movies"))
        assertFalse(isRecencyQuery("best Christopher Nolan films"))
        assertFalse(isRecencyQuery("classic film noir"))
        assertFalse(isRecencyQuery("mind bending psychological thrillers"))
        assertFalse(isRecencyQuery("90s space movies"))
    }

    @Test fun cinemetaClientAvailabilityHandlesBlankTitleAndSafeParsing() {
        assertEquals("Missing title", CinemetaClient.checkAvailability("", "2024"))
        assertEquals("Missing title", CinemetaClient.checkAvailability("   ", "2024"))

        val emptyParsed = CinemetaClient.parseCards(org.json.JSONObject())
        assertTrue(emptyParsed.isEmpty())

        val invalidMetas = CinemetaClient.parseCards(org.json.JSONObject("""{"metas":[{"id":"kodi123"}]}"""))
        assertTrue(invalidMetas.isEmpty())
    }

    @Test fun cleanTitleAndMatchesYearDisambiguateRegionalCinemaAndHomonyms() {
        // Kick (Telugu 2009) vs Kick-Ass (2010) vs Kick (Hindi 2014)
        assertEquals("kick", cleanTitle("Kick"))
        assertEquals("kickass", cleanTitle("Kick-Ass"))
        assertEquals("kick", cleanTitle("Kick (2009)"))
        assertEquals("kick", cleanTitle("The Kick"))
        assertNotEquals(cleanTitle("Kick"), cleanTitle("Kick-Ass"))

        // Year matching with exact tolerance (0) separates homonyms released in adjacent years
        // Dhamaka (2022 Telugu) vs Dhamaka (2021 Hindi)
        assertEquals("dhamaka", cleanTitle("Dhamaka"))
        assertTrue(matchesYear("2022", "2022", tolerance = 0))
        assertFalse(matchesYear("2022", "2021", tolerance = 0))
        assertTrue(matchesYear("2022", "2021", tolerance = 1))

        // Kick (2009 Telugu) vs Kick (2014 Hindi)
        assertTrue(matchesYear("2009", "2009", tolerance = 0))
        assertFalse(matchesYear("2009", "2014", tolerance = 1))

        // Default tolerance (1) keeps close years, drops distant ones and
        // rejects blank/invalid input — Eagle 2024 (Ravi Teja) vs 1991/2011.
        assertTrue(matchesYear("2024", "2024"))
        assertTrue(matchesYear("2024", "2023"))
        assertFalse(matchesYear("2024", "2011"))
        assertFalse(matchesYear("2024", "1991"))
        assertFalse(matchesYear("2024", "1982"))
        assertFalse(matchesYear("", "2024"))
        assertFalse(matchesYear("invalid", "2024"))
    }

    @Test fun forceRefreshBypassesTheTenMinuteSearchCache() {
        // The reported scenario: searching "Telugu movies 2026" twice
        // returns the cached cards; a force refresh must not. This asserts
        // the exact cache-read decision search() uses, via the test-only
        // seams — no network, no provider fetch.
        val repository = MatrixSearchRepository(FakeSharedPreferences())
        val seeded = listOf(
            MediaCard("m1", "Telugu movies 2026", "", "", "", true, 0, 0),
            MediaCard("m2", "Telugu movies 2026 (2)", "", "", "", true, 0, 0)
        )
        repository.seedSearchCacheForTest(MatrixSearchProvider.CATALOG, "Telugu movies 2026", seeded)
        val key = repository.searchCacheKey(MatrixSearchProvider.CATALOG, "Telugu movies 2026")
        // A second search without forceRefresh returns the cached cards.
        assertEquals(seeded, repository.cachedSearchCards(key, forceRefresh = false))
        // The same query with forceRefresh skips the cache.
        assertNull(repository.cachedSearchCards(key, forceRefresh = true))
        // The force-refresh read does not evict the entry: a later normal
        // search still hits it (a real search() overwrites it on write).
        assertEquals(seeded, repository.cachedSearchCards(key, forceRefresh = false))
        // A different query misses the cache.
        val otherKey = repository.searchCacheKey(MatrixSearchProvider.CATALOG, "Telugu movies 2025")
        assertNull(repository.cachedSearchCards(otherKey, forceRefresh = false))
    }

    @Test fun staleSearchCacheEntryIsAMiss() {
        // The 10-minute TTL: an entry seeded 11 minutes ago must miss, so a
        // search re-fetches instead of serving stale cards.
        val repository = MatrixSearchRepository(FakeSharedPreferences())
        val seeded = listOf(MediaCard("m1", "Telugu movies 2026", "", "", "", true, 0, 0))
        repository.seedSearchCacheForTest(
            MatrixSearchProvider.CATALOG, "Telugu movies 2026", seeded,
            at = System.currentTimeMillis() - 11 * 60 * 1000
        )
        val key = repository.searchCacheKey(MatrixSearchProvider.CATALOG, "Telugu movies 2026")
        assertNull(repository.cachedSearchCards(key, forceRefresh = false))
    }

    @Test fun aiResolutionGroupingCollapsesRepeatedTitlesButKeepsRemakesSeparate() {
        // D-02 grouping: identical suggestions resolve once; the same title
        // with a different year or IMDb id (a remake, a corrected id) stays
        // separate. Groups come out in first-appearance order.
        val repository = MatrixSearchRepository(FakeSharedPreferences())
        val recs = listOf(
            AiRecommendation("Inception", "2010", "movie", "Dream-within-a-dream thriller.", "tt1375666"),
            AiRecommendation("Inception", "2010", "movie", "Same title again.", "tt1375666"),
            AiRecommendation("Inception", "2010", "movie", "Third time.", "tt1375666"),
            AiRecommendation("The Lion King", "1994", "movie", "Original.", "tt0110357"),
            AiRecommendation("The Lion King", "2019", "movie", "Remake.", "tt6105098"),
            AiRecommendation("Dark", "2017", "series", "Time-travel mystery.", "tt5753856"),
            AiRecommendation("Dark", "2017", "series", "Same title, no id.")
        )
        val groups = repository.groupRecommendationsForResolution(recs)
        assertEquals(5, groups.size)
        // Repeated identical suggestions collapse to one group.
        assertEquals(3, groups.values.first { it[0].title == "Inception" }.size)
        // Different year / different IMDb id stay separate.
        assertEquals(2, groups.values.count { it[0].title == "The Lion King" })
        // Same title, year and type but a missing IMDb id is a different key.
        assertEquals(2, groups.values.count { it[0].title == "Dark" })
        // First-appearance order is preserved.
        assertEquals(
            listOf("Inception", "The Lion King", "The Lion King", "Dark", "Dark"),
            groups.values.map { it[0].title }
        )
    }
}

/** Test-only in-memory SharedPreferences. Production code never uses this;
 * it exists so the force-refresh test can build a real repository on the
 * plain JVM (no Robolectric, no Mockito). MatrixAiClient reads prefs only
 * lazily, so constructing the repository needs no Android runtime. */
private class FakeSharedPreferences : SharedPreferences {
    private val values = mutableMapOf<String, Any?>()
    override fun getAll(): Map<String, *> = values.toMap()
    override fun getString(key: String, defValue: String?): String? = values[key] as? String ?: defValue
    override fun getStringSet(key: String, defValues: Set<String>?): Set<String>? =
        @Suppress("UNCHECKED_CAST") (values[key] as? Set<String> ?: defValues)
    override fun getInt(key: String, defValue: Int): Int = values[key] as? Int ?: defValue
    override fun getLong(key: String, defValue: Long): Long = values[key] as? Long ?: defValue
    override fun getFloat(key: String, defValue: Float): Float = values[key] as? Float ?: defValue
    override fun getBoolean(key: String, defValue: Boolean): Boolean = values[key] as? Boolean ?: defValue
    override fun contains(key: String): Boolean = values.containsKey(key)
    override fun edit(): SharedPreferences.Editor = FakeEditor()
    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {}
    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {}

    private inner class FakeEditor : SharedPreferences.Editor {
        private val pending = mutableMapOf<String, Any?>()
        private val removals = mutableSetOf<String>()
        private var clearAll = false
        override fun putString(key: String, value: String?): SharedPreferences.Editor = apply { pending[key] = value }
        override fun putStringSet(key: String, values: Set<String>?): SharedPreferences.Editor = apply { pending[key] = values }
        override fun putInt(key: String, value: Int): SharedPreferences.Editor = apply { pending[key] = value }
        override fun putLong(key: String, value: Long): SharedPreferences.Editor = apply { pending[key] = value }
        override fun putFloat(key: String, value: Float): SharedPreferences.Editor = apply { pending[key] = value }
        override fun putBoolean(key: String, value: Boolean): SharedPreferences.Editor = apply { pending[key] = value }
        override fun remove(key: String): SharedPreferences.Editor = apply { removals += key }
        override fun clear(): SharedPreferences.Editor = apply { clearAll = true }
        override fun commit(): Boolean { apply(); return true }
        override fun apply() {
            if (clearAll) values.clear()
            removals.forEach { values.remove(it) }
            values.putAll(pending)
        }
    }
}
