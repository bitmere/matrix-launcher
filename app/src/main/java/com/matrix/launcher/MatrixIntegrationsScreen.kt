package com.matrix.launcher

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*

import androidx.compose.ui.res.stringResource

/** Integrations settings tree, split out of MatrixSearchScreen.kt (P0/P1 split).
 * P0 N2: the AI-key row shows only "Configured · ••••" + the last 4 characters, never the full key.
 * Static titles/descriptions/help lines use string resources (see coordinator's strings.xml list);
 * dynamic strings (provider label, model name, counts) stay in code. Behaviour otherwise unchanged. */
@Composable
internal fun MatrixIntegrationsScreen(page: String, repository: ComposeHomeRepository, push: (String) -> Unit, back: () -> Unit) {
    val search = repository.search
    var revision by remember { mutableIntStateOf(0) }
    val available by produceState<List<MatrixMediaApp>?>(null, repository) {
        value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { repository.media.available() }
    }
    revision // Re-read local setup after a toggle without refreshing Home or making network calls.
    when (page) {
        "media-app" -> {
            val choices = available
            val stremioDetail = stringResource(R.string.integrations_media_app_stremio_detail)
            val nuvioDetail = stringResource(R.string.integrations_media_app_nuvio_detail)
            if (choices == null) MatrixStatusPage(stringResource(R.string.integrations_media_app_title), stringResource(R.string.integrations_media_app_checking), true, back)
            else SettingsPage(stringResource(R.string.integrations_media_app_title),
                stringResource(R.string.integrations_media_app_description_prefix) + " " +
                    if (choices.isEmpty()) stringResource(R.string.integrations_media_app_description_none)
                    else stringResource(R.string.integrations_media_app_description_automatic),
                listOf(SettingAction(stringResource(R.string.integrations_media_app_automatic_title), stringResource(R.string.integrations_media_app_automatic_detail), selected = repository.media.preferred == "auto") {
                    repository.media.preferred = "auto"; revision++
                }) + choices.map { app -> SettingAction(app.label,
                    if (app == MatrixMediaApp.STREMIO) stremioDetail else nuvioDetail,
                    selected = repository.media.preferred == app.name, id = app.name) {
                    repository.media.preferred = app.name; revision++
                } }, back)
        }
        "integrations" -> SettingsPage(stringResource(R.string.integrations_title), stringResource(R.string.integrations_description), listOf(
            SettingAction(stringResource(R.string.integrations_media_app_title), when (repository.media.preferred) {
                "auto" -> stringResource(R.string.integrations_media_app_automatic_provider_detail)
                else -> MatrixMediaApp.entries.find { it.name == repository.media.preferred }?.label ?: stringResource(R.string.integrations_media_app_choose_app_detail)
            }) { push("media-app") },
            SettingAction(stringResource(R.string.integrations_catalog_title), if (search.enabled(MatrixSearchProvider.CATALOG)) stringResource(R.string.integrations_catalog_enabled_detail) else stringResource(R.string.integrations_catalog_off_detail)) { push("catalog") },
            SettingAction(stringResource(R.string.integrations_simkl_title), if (repository.accounts.connected) stringResource(R.string.integrations_simkl_connected_detail) else stringResource(R.string.integrations_simkl_connect_detail)) { push("simkl") },
            SettingAction(stringResource(R.string.integrations_tmdb_title), if (search.ready(MatrixSearchProvider.TMDB)) stringResource(R.string.integrations_tmdb_configured_detail) else stringResource(R.string.integrations_tmdb_optional_detail)) { push("tmdb") },
            SettingAction(stringResource(R.string.integrations_ai_title), if (search.ai.ready) "Configured · ${search.ai.provider.label} (${search.ai.activeModel})" else stringResource(R.string.integrations_ai_discovery_detail)) { push("ai") }
        ), back)
        "catalog" -> SettingsPage(stringResource(R.string.integrations_catalog_title), stringResource(R.string.integrations_catalog_page_description), listOf(
            SettingAction(stringResource(R.string.integrations_use_in_search_title), stringResource(R.string.integrations_catalog_use_detail), checked = search.enabled(MatrixSearchProvider.CATALOG)) {
                search.enable(MatrixSearchProvider.CATALOG, !search.enabled(MatrixSearchProvider.CATALOG)); revision++
            }
        ), back)
        "ai" -> SettingsPage(stringResource(R.string.integrations_ai_title), stringResource(R.string.integrations_ai_page_description), listOfNotNull(
            SettingAction(stringResource(R.string.integrations_use_in_search_title), stringResource(R.string.integrations_ai_use_detail), checked = search.enabled(MatrixSearchProvider.AI)) {
                if (!search.ai.ready && search.ai.apiKey.isBlank()) push("ai-key")
                else { search.enable(MatrixSearchProvider.AI, !search.enabled(MatrixSearchProvider.AI)); revision++ }
            },
            SettingAction("Provider · " + search.ai.provider.label, search.ai.provider.description) { push("ai-provider") },
            SettingAction("Model · " + search.ai.activeModel, stringResource(R.string.integrations_ai_model_detail)) { push("ai-model") },
            SettingAction("Result count · ${search.ai.resultCount} titles", when (search.ai.resultCount) {
                100 -> stringResource(R.string.integrations_ai_result_count_detail_100)
                50 -> stringResource(R.string.integrations_ai_result_count_detail_50)
                25 -> stringResource(R.string.integrations_ai_result_count_detail_25)
                15 -> stringResource(R.string.integrations_ai_result_count_detail_15)
                else -> "Standard · ${search.ai.resultCount} titles (fastest)"
            }) { push("ai-count") },
            SettingAction(stringResource(R.string.integrations_ai_web_search_title), if (search.ai.webSearch) stringResource(R.string.integrations_ai_web_search_enabled_detail) else stringResource(R.string.integrations_ai_web_search_off_detail), checked = search.ai.webSearch) {
                search.ai.webSearch = !search.ai.webSearch; revision++
            },
            SettingAction(if (search.ai.apiKey.isBlank()) stringResource(R.string.integrations_ai_add_key_title) else stringResource(R.string.integrations_ai_change_key_title), if (search.ai.apiKey.isNotBlank()) stringResource(R.string.integrations_ai_key_configured_prefix) + search.ai.apiKey.takeLast(4) else "Enter ${search.ai.provider.label} API key") { push("ai-key") },
            if (search.ai.ready) SettingAction(stringResource(R.string.integrations_test_connection_title), "Send a test ping to ${search.ai.provider.label} (${search.ai.activeModel})") { push("ai-test") } else null,
            if (search.ai.provider == MatrixAiProvider.CUSTOM) SettingAction("Base URL · " + search.ai.activeUrl, stringResource(R.string.integrations_ai_base_url_detail)) { push("ai-endpoint") } else null,
            if (search.ai.apiKey.isNotBlank()) SettingAction(stringResource(R.string.integrations_ai_remove_key_title), "Disables AI Discovery for ${search.ai.provider.label}", role = MatrixControlRole.Destructive) {
                search.enable(MatrixSearchProvider.AI, false); search.ai.removeApiKey(); revision++
            } else null
        ), back)
        "ai-count" -> {
            val countDetail100 = stringResource(R.string.integrations_ai_count_detail_100)
            val countDetail50 = stringResource(R.string.integrations_ai_count_detail_50)
            val countDetail25 = stringResource(R.string.integrations_ai_result_count_detail_25)
            val countDetail15 = stringResource(R.string.integrations_ai_result_count_detail_15)
            val countDetail10 = stringResource(R.string.integrations_ai_count_detail_10)
            SettingsPage(stringResource(R.string.integrations_ai_count_title), stringResource(R.string.integrations_ai_count_description), listOf(10, 15, 25, 50, 100).map { count ->
            SettingAction(
                "$count titles",
                when (count) {
                    100 -> countDetail100
                    50 -> countDetail50
                    25 -> countDetail25
                    15 -> countDetail15
                    else -> countDetail10
                },
                selected = search.ai.resultCount == count
            ) {
                search.ai.resultCount = count; search.clearCache(); revision++; back()
            }
        }, back)
        }
        "ai-provider" -> SettingsPage(stringResource(R.string.integrations_ai_provider_title), stringResource(R.string.integrations_ai_provider_description), MatrixAiProvider.entries.map { p ->
            SettingAction(p.label, p.description, selected = search.ai.provider == p) {
                search.ai.provider = p; search.clearCache(); revision++; back()
            }
        }, back)
        "ai-model" -> {
            val currentProvider = search.ai.provider
            val current = search.ai.activeModel
            val liveModelsState = produceState<List<AiModelChoice>?>(initialValue = null, key1 = currentProvider, key2 = revision) {
                value = search.ai.fetchModels().getOrNull()
            }
            val liveModels = liveModelsState.value
            val models = if (!liveModels.isNullOrEmpty()) liveModels else currentProvider.recommendedModels
            val isLive = !liveModels.isNullOrEmpty()
            val subtitle = if (isLive) {
                "${models.size} live models available from ${currentProvider.label} API."
            } else if (liveModelsState.value == null) {
                "Loading live models from ${currentProvider.label} API…"
            } else {
                "Showing recommended models for ${currentProvider.label}."
            }

            val choices = models.map { model ->
                SettingAction(model.label, model.description + " · " + model.id, selected = current == model.id) {
                    search.ai.selectedModel = model.id; search.clearCache(); revision++; back()
                }
            } + listOf(
                SettingAction(stringResource(R.string.integrations_ai_model_custom_title), "Enter any model ID supported by ${currentProvider.label} (Current: $current)") {
                    push("ai-model-custom")
                }
            )
            SettingsPage("AI Model · " + currentProvider.label, subtitle, choices, back)
        }
        "ai-test" -> {
            val testState = produceState<Result<String>?>(initialValue = null, key1 = revision) {
                value = search.ai.testConnection()
            }
            when (val res = testState.value) {
                null -> MatrixStatusPage(stringResource(R.string.integrations_ai_test_title), "Sending test ping to ${search.ai.provider.label} (${search.ai.activeModel})…", true, back)
                else -> res.fold(
                    onSuccess = { msg ->
                        SettingsPage(stringResource(R.string.integrations_ai_connection_title), msg, listOf(
                            SettingAction(stringResource(R.string.integrations_done_title), stringResource(R.string.integrations_ai_test_done_detail), role = MatrixControlRole.Commit) { back() }
                        ), back)
                    },
                    onFailure = { err ->
                        SettingsPage(stringResource(R.string.integrations_ai_connection_failed_title), err.message ?: stringResource(R.string.integrations_unknown_error), listOf(
                            SettingAction(stringResource(R.string.integrations_retry_title), stringResource(R.string.integrations_test_again_detail), role = MatrixControlRole.Action) { revision++ },
                            SettingAction(stringResource(R.string.integrations_back_title), stringResource(R.string.integrations_ai_test_done_detail), role = MatrixControlRole.Secondary) { back() }
                        ), back)
                    }
                )
            }
        }
        "ai-model-custom" -> SettingsForm(stringResource(R.string.integrations_ai_custom_model_title), "Enter the exact model identifier supported by ${search.ai.provider.label} (e.g. deepseek-v4.1-flash, deepseek-r1, llama-3.3-70b).",
            listOf(FormField("model", stringResource(R.string.integrations_ai_custom_model_field), search.ai.activeModel)), back) {
            val m = it["model"].orEmpty().trim()
            if (m.isNotBlank()) { search.ai.selectedModel = m; search.clearCache() }
            revision++; back()
        }
        "ai-key" -> SettingsForm("${search.ai.provider.label} API key",
            when (search.ai.provider) {
                MatrixAiProvider.DEEPSEEK -> stringResource(R.string.integrations_ai_key_help_deepseek)
                MatrixAiProvider.NEURALWATT -> stringResource(R.string.integrations_ai_key_help_neuralwatt)
                MatrixAiProvider.GROQ -> stringResource(R.string.integrations_ai_key_help_groq)
                MatrixAiProvider.OPENROUTER -> stringResource(R.string.integrations_ai_key_help_openrouter)
                MatrixAiProvider.OPENAI -> stringResource(R.string.integrations_ai_key_help_openai)
                MatrixAiProvider.PERPLEXITY -> stringResource(R.string.integrations_ai_key_help_perplexity)
                MatrixAiProvider.GEMINI -> stringResource(R.string.integrations_ai_key_help_gemini)
                MatrixAiProvider.CUSTOM -> stringResource(R.string.integrations_ai_key_help_custom)
            },
            listOf(FormField("key", stringResource(R.string.integrations_ai_key_field), search.ai.apiKey, secret = true)), back) {
            search.ai.apiKey = it["key"].orEmpty()
            if (search.ai.apiKey.isNotBlank()) search.enable(MatrixSearchProvider.AI, true)
            revision++; back()
        }
        "ai-endpoint" -> SettingsForm(stringResource(R.string.integrations_ai_endpoint_title), stringResource(R.string.integrations_ai_endpoint_help),
            listOf(FormField("url", stringResource(R.string.integrations_ai_endpoint_field), search.ai.customUrl)), back) {
            search.ai.customUrl = it["url"].orEmpty()
            revision++; back()
        }
        "tmdb" -> SettingsPage(stringResource(R.string.integrations_tmdb_title), stringResource(R.string.integrations_tmdb_page_description), listOfNotNull(
            SettingAction(stringResource(R.string.integrations_use_in_search_title), stringResource(R.string.integrations_tmdb_use_detail), checked = search.enabled(MatrixSearchProvider.TMDB)) {
                if (!search.ready(MatrixSearchProvider.TMDB)) push("tmdb-key")
                else { search.enable(MatrixSearchProvider.TMDB, !search.enabled(MatrixSearchProvider.TMDB)); revision++ }
            },
            SettingAction(if (search.tmdbKey.isBlank()) stringResource(R.string.integrations_tmdb_add_credential_title) else stringResource(R.string.integrations_tmdb_change_credential_title), stringResource(R.string.integrations_tmdb_credential_detail)) { push("tmdb-key") },
            if (search.ready(MatrixSearchProvider.TMDB)) SettingAction(stringResource(R.string.integrations_test_connection_title), stringResource(R.string.integrations_tmdb_test_detail)) { push("tmdb-test") } else null,
            SettingAction(stringResource(R.string.integrations_tmdb_remove_title), stringResource(R.string.integrations_tmdb_remove_detail), role = MatrixControlRole.Destructive) {
                search.enable(MatrixSearchProvider.TMDB, false); search.tmdbKey = ""; revision++
            }.takeIf { search.tmdbKey.isNotBlank() }
        ), back)
        "tmdb-test" -> {
            val testState = produceState<Result<String>?>(initialValue = null, key1 = revision) {
                value = search.testTmdbConnection()
            }
            when (val res = testState.value) {
                null -> MatrixStatusPage(stringResource(R.string.integrations_tmdb_test_title), stringResource(R.string.integrations_tmdb_test_checking), true, back)
                else -> res.fold(
                    onSuccess = { msg ->
                        SettingsPage(stringResource(R.string.integrations_tmdb_connection_title), msg, listOf(
                            SettingAction(stringResource(R.string.integrations_done_title), stringResource(R.string.integrations_tmdb_test_done_detail), role = MatrixControlRole.Commit) { back() }
                        ), back)
                    },
                    onFailure = { err ->
                        SettingsPage(stringResource(R.string.integrations_tmdb_connection_failed_title), err.message ?: stringResource(R.string.integrations_unknown_error), listOf(
                            SettingAction(stringResource(R.string.integrations_retry_title), stringResource(R.string.integrations_test_again_detail), role = MatrixControlRole.Action) { revision++ },
                            SettingAction(stringResource(R.string.integrations_back_title), stringResource(R.string.integrations_tmdb_test_done_detail), role = MatrixControlRole.Secondary) { back() }
                        ), back)
                    }
                )
            }
        }
        "tmdb-key" -> SettingsForm(stringResource(R.string.integrations_tmdb_key_title), stringResource(R.string.integrations_tmdb_key_help),
            listOf(FormField("key", stringResource(R.string.integrations_tmdb_key_field), search.tmdbKey, secret = true)), back) {
            search.tmdbKey = it["key"].orEmpty(); back()
        }
    }
}
