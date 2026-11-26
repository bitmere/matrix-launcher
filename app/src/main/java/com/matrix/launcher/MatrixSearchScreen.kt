package com.matrix.launcher

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Query, provider controls and results stay on one screen; only explicit submission fetches. */

@Composable
internal fun MatrixSearchScreen(repository: ComposeHomeRepository, windowWidth: androidx.compose.ui.unit.Dp, back: () -> Unit,
    integrations: () -> Unit, open: (MediaCard) -> Unit) {
    val providers = repository.search.providers()
    val savedProvider = repository.prefs.getString(MatrixPrefs.LAST_SEARCH_PROVIDER, null)
    val defaultProvider = if (savedProvider != null && providers.any { it.name == savedProvider }) savedProvider
        else if (providers.any { it == MatrixSearchProvider.CATALOG }) MatrixSearchProvider.CATALOG.name
        else if (providers.any { it == MatrixSearchProvider.AI }) MatrixSearchProvider.AI.name
        else providers.firstOrNull()?.name.orEmpty()
    var providerName by rememberSaveable { mutableStateOf(defaultProvider) }
    val provider = providers.find { it.name == providerName } ?: providers.firstOrNull()
    var query by rememberSaveable { mutableStateOf("") }
    var searchJob by remember { mutableStateOf<Job?>(null) }
    var searchProgress by remember { mutableStateOf("") }
    val searchSteps = remember { mutableStateListOf<String>() }
    var error by remember { mutableStateOf("") }
    var cards by remember { mutableStateOf<List<MediaCard>?>(null) }
    var matchedApps by remember { mutableStateOf<List<LaunchApp>>(emptyList()) }
    var busy by remember { mutableStateOf(false) }
    var recents by remember { mutableStateOf(repository.search.recentSearches()) }
    val operation = remember { SearchOperation() }
    var fieldFocused by remember { mutableStateOf(false) }
    val field = remember { FocusRequester() }
    val clearButton = remember { FocusRequester() }
    val searchButton = remember { FocusRequester() }
    val firstResult = remember { FocusRequester() }
    val firstApp = remember { FocusRequester() }
    val grid = rememberLazyGridState()
    val scope = rememberCoroutineScope()
    val inspection = LocalMatrixUiInspection.current
    SideEffect {
        inspection?.searchProvider = provider?.name ?: "none"
        inspection?.searchResults = (cards?.size ?: 0) + matchedApps.size
        inspection?.searchBusy = busy
    }
    DisposableEffect(inspection) {
        onDispose { operation.begin(); searchJob?.cancel(); inspection?.searchProvider = "none"; inspection?.searchResults = 0; inspection?.searchBusy = false }
    }
    val context = LocalContext.current
    val catalogUnavailableText = stringResource(R.string.search_catalog_unavailable)
    var isListening by remember { mutableStateOf(false) }
    var recognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }
    var voiceSessionId by remember { mutableIntStateOf(0) }

    fun stopListeningHardware() {
        voiceSessionId++
        try {
            recognizer?.setRecognitionListener(null)
            recognizer?.cancel()
            recognizer?.destroy()
        } catch (_: Exception) {}
        recognizer = null
        isListening = false
    }

    fun submit(overrideQuery: String? = null, forceRefresh: Boolean = false) {
        stopListeningHardware()
        val selected = provider ?: return
        val value = (overrideQuery ?: query).trim()
        if (overrideQuery != null) query = overrideQuery
        if (value.length !in 2..120) { error = "Enter 2–120 characters."; return }
        error = ""
        busy = true
        cards = null
        matchedApps = emptyList()
        searchSteps.clear()
        searchSteps.add("Query: \"$value\"")
        val initialMsg = when (selected) {
            MatrixSearchProvider.AI -> "Consulting ${repository.search.ai.activeModel} (${repository.search.ai.provider.label})…"
            MatrixSearchProvider.CATALOG -> "Searching Cinemeta catalog…"
            else -> "Searching ${selected.title}…"
        }
        searchProgress = initialMsg
        searchSteps.add(initialMsg)
        val generation = operation.begin()
        searchJob?.cancel()
        searchJob = scope.launch {
            try {
                val apps = repository.searchApps(value)
                currentCoroutineContext().ensureActive()
                if (!operation.isCurrent(generation)) return@launch
                matchedApps = apps
                val results = repository.search.search(selected, value, forceRefresh = forceRefresh, onProgress = { progress ->
                    scope.launch {
                        if (operation.isCurrent(generation)) {
                            searchProgress = progress
                            if (searchSteps.lastOrNull() != progress) searchSteps.add(progress)
                        }
                    }
                })
                currentCoroutineContext().ensureActive()
                if (!operation.isCurrent(generation)) return@launch
                cards = results
                busy = false
                error = ""
                if (!cards.isNullOrEmpty() || matchedApps.isNotEmpty()) {
                    if (matchedApps.isNotEmpty() && cards.isNullOrEmpty()) {
                        repository.search.addRecentSearch(value)
                    }
                    recents = repository.search.recentSearches()
                    if (matchedApps.isNotEmpty()) {
                        withFrameNanos { }; firstApp.requestFocus()
                    } else {
                        grid.scrollToItem(0); withFrameNanos { }; firstResult.requestFocus()
                    }
                }
            }
            catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (failure: Exception) {
                if (!operation.isCurrent(generation) || !currentCoroutineContext().isActive) return@launch
                val detail = failure.localizedMessage?.takeIf { it.isNotBlank() } ?: failure.message
                error = if (failure is CatalogUnavailableException) {
                    catalogUnavailableText
                } else {
                    if (detail != null) detail else "Search unavailable. Check the connection, provider credential and API usage in Settings → Integrations."
                }
                android.util.Log.e("MatrixSearch", "Search error for $selected: $detail", failure)
            } finally { if (operation.isCurrent(generation) && currentCoroutineContext().isActive) { busy = false; searchProgress = "" } }
        }
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_PAUSE || event == androidx.lifecycle.Lifecycle.Event.ON_STOP) {
                stopListeningHardware()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            stopListeningHardware()
        }
    }

    LaunchedEffect(isListening, voiceSessionId) {
        if (isListening) {
            val sessionAtStart = voiceSessionId
            delay(10_000L)
            if (isListening && voiceSessionId == sessionAtStart && !busy) {
                stopListeningHardware()
                error = "No speech detected. Tap mic to try again."
            }
        }
    }

    val voiceIntent = remember {
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Say a movie or series title")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
    }
    val voiceFallback = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull { it.isNotBlank() }?.trim()?.removeSuffix(".")?.trim()
            if (spoken != null) { submit(spoken.take(120)) }
            else if (!busy) error = "No speech received. Try again or type a title."
        }
    }
    fun startListening() {
        stopListeningHardware()
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            try { voiceFallback.launch(voiceIntent) }
            catch (_: Exception) { error = "Voice search is unavailable on this device." }
            return
        }
        error = ""
        val sr = try {
            SpeechRecognizer.createSpeechRecognizer(context)
        } catch (e: Exception) {
            try { voiceFallback.launch(voiceIntent) }
            catch (_: Exception) { error = "Voice recognition error: ${e.message}" }
            return
        }
        val currentSession = voiceSessionId
        recognizer = sr
        isListening = true
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        try {
            sr.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    if (voiceSessionId != currentSession) return
                    isListening = true
                }
                override fun onBeginningOfSpeech() {
                    if (voiceSessionId != currentSession) return
                    isListening = true
                }
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {
                    if (voiceSessionId != currentSession) return
                    isListening = false
                }
                override fun onError(errorCode: Int) {
                    if (voiceSessionId != currentSession || busy) return
                    stopListeningHardware()
                    if (busy) return
                    when (errorCode) {
                        SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized. Speak closer to the TV remote."
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected. Tap mic to try again."
                        SpeechRecognizer.ERROR_AUDIO -> "Audio recording error. Check TV remote microphone."
                        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Voice network error. Check connection."
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission is required."
                        else -> null
                    }?.let { error = it }
                }
                override fun onResults(results: Bundle?) {
                    if (voiceSessionId != currentSession) return
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val spoken = matches?.firstOrNull()?.trim()?.removeSuffix(".")?.trim()
                    stopListeningHardware()
                    if (!spoken.isNullOrBlank()) {
                        submit(spoken.take(120))
                    } else if (!busy) {
                        error = "No speech recognized. Speak closer to the TV remote."
                    }
                }
                override fun onPartialResults(partialResults: Bundle?) {
                    if (voiceSessionId != currentSession || busy) return
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val spoken = matches?.firstOrNull()?.trim()?.removeSuffix(".")?.trim()
                    if (!spoken.isNullOrBlank()) { query = spoken.take(120) }
                }
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
            sr.startListening(intent)
        } catch (e: Exception) {
            stopListeningHardware()
            try { voiceFallback.launch(voiceIntent) }
            catch (_: Exception) { error = "Voice recognition error: ${e.message}" }
        }
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startListening()
        else error = "Microphone permission is required for voice search."
    }
    fun toggleVoice() {
        if (isListening || recognizer != null) {
            stopListeningHardware()
            return
        }
        val hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        if (!hasPermission) permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        else startListening()
    }
    val voiceAvailable = remember(context) { SpeechRecognizer.isRecognitionAvailable(context) || voiceIntent.resolveActivity(context.packageManager) != null }
    val voicePulse = rememberInfiniteTransition(label = "voicePulse")
    val pulseAlpha by voicePulse.animateFloat(
        initialValue = .35f, targetValue = .95f,
        animationSpec = infiniteRepeatable(tween(650, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "voiceAlpha"
    )
    val pulseScale by voicePulse.animateFloat(
        initialValue = 1f, targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(650, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "voiceScale"
    )
    val voiceRotation by voicePulse.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(2500, easing = LinearEasing), RepeatMode.Restart),
        label = "voiceRotation"
    )
    fun navigateBack() {
        if (isListening || recognizer != null) {
            stopListeningHardware()
            return
        }
        val hasResults = !cards.isNullOrEmpty() || matchedApps.isNotEmpty()
        if (hasResults && !fieldFocused) {
            field.requestFocus()
        } else {
            searchJob?.cancel()
            back()
        }
    }
    BackHandler { navigateBack() }
    if (provider == null) {
        SettingsPage("Search", "Enable a search provider in Settings → Integrations. Catalog provides keyless search; SIMKL needs a connected account; TMDB needs your own API key.",
            listOf(SettingAction("Integrations", "Enable Catalog, SIMKL or set up TMDB") { integrations() }), back)
    } else Column {
        Row(Modifier.fillMaxWidth().padding(bottom = 16.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val searchTitle = if (provider == MatrixSearchProvider.AI) "AI Discovery (${repository.search.ai.activeModel})" else provider.title
            val searchPlaceholder = when {
                isListening -> "🎙️ Listening… Speak a title"
                provider == MatrixSearchProvider.AI -> "Ask AI · ${repository.search.ai.activeModel} (e.g. mind-bending sci-fi)"
                provider == MatrixSearchProvider.CATALOG -> "Search movies and series (Catalog)"
                else -> "Search ${provider.title} movies and series"
            }
            MatrixIconButton(R.drawable.ic_matrix_back, "Back", ::navigateBack, compact = true)
            MatrixTextEntry("Search · $searchTitle", query, { query = it.take(120) }, field,
                Modifier.weight(1f).onFocusChanged { fieldFocused = it.hasFocus },
                imeAction = ImeAction.Search, showLabel = false, compact = true,
                placeholder = searchPlaceholder,
                next = { (if (query.isNotBlank()) clearButton else searchButton).requestFocus() }, submit = { submit() })
            if (query.isNotBlank()) {
                MatrixIconButton(R.drawable.ic_matrix_clear, "Clear text", {
                    operation.begin()
                    searchJob?.cancel()
                    busy = false
                    searchProgress = ""
                    searchSteps.clear()
                    query = ""
                    cards = null
                    matchedApps = emptyList()
                    error = ""
                    field.requestFocus()
                }, compact = true, modifier = Modifier.focusRequester(clearButton))
            }
            MatrixIconButton(R.drawable.ic_search, "Submit search", { submit() }, compact = true,
                modifier = Modifier.focusRequester(searchButton))
            if (query.isNotBlank()) {
                // Force-refresh: re-runs the current query bypassing the caches.
                MatrixIconButton(R.drawable.ic_home_refresh, stringResource(R.string.search_refresh_results),
                    { submit(forceRefresh = true) }, compact = true)
            }
            if (voiceAvailable) {
                if (isListening) {
                    // Single theme accent for the listening glow; no hard-coded palette.
                    val voiceAccent = MaterialTheme.colorScheme.primary
                    Box(
                        modifier = Modifier
                            .graphicsLayer {
                                scaleX = pulseScale
                                scaleY = pulseScale
                            }
                            .drawBehind {
                                val cx = size.width / 2f
                                val cy = size.height / 2f
                                val radius = size.minDimension / 2f
                                val rad = Math.toRadians(voiceRotation.toDouble())

                                // 1. Ethereal Ambient Voice Glow (Soft ChatGPT / Gemini Live bloom)
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            voiceAccent.copy(alpha = 0.35f * pulseAlpha),
                                            voiceAccent.copy(alpha = 0.20f * pulseAlpha),
                                            voiceAccent.copy(alpha = 0.10f),
                                            Color.Transparent
                                        ),
                                        center = Offset(cx, cy),
                                        radius = radius * 1.50f
                                    ),
                                    radius = radius * 1.50f,
                                    center = Offset(cx, cy)
                                )

                                // 2. Luminous Organic Floating Orbit Blob (seamless k=1 integer orbit)
                                val orbitOffset = Offset(
                                    x = cx + (cos(rad) * radius * 0.14f).toFloat(),
                                    y = cy + (sin(rad) * radius * 0.14f).toFloat()
                                )
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            voiceAccent.copy(alpha = 0.30f * pulseAlpha),
                                            voiceAccent.copy(alpha = 0.15f),
                                            Color.Transparent
                                        ),
                                        center = orbitOffset,
                                        radius = radius * 1.20f
                                    ),
                                    radius = radius * 1.20f,
                                    center = orbitOffset
                                )

                                // 3. Micro-thin Shimmer Accent Ring (1dp, soft 35% alpha, seamless 360 loop)
                                rotate(degrees = voiceRotation, pivot = Offset(cx, cy)) {
                                    drawCircle(
                                        brush = Brush.sweepGradient(
                                            colors = listOf(
                                                voiceAccent.copy(alpha = 0.31f),
                                                voiceAccent.copy(alpha = 0.19f),
                                                voiceAccent.copy(alpha = 0.31f),
                                                voiceAccent.copy(alpha = 0.19f),
                                                voiceAccent.copy(alpha = 0.31f)
                                            ),
                                            center = Offset(cx, cy)
                                        ),
                                        radius = radius * 0.98f,
                                        center = Offset(cx, cy),
                                        style = Stroke(width = 1.dp.toPx())
                                    )
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        MatrixIconButton(
                            icon = R.drawable.ic_voice,
                            label = "Listening (click to stop)",
                            onClick = ::toggleVoice,
                            compact = true,
                            role = MatrixControlRole.Action
                        )
                    }
                } else {
                    MatrixIconButton(
                        icon = R.drawable.ic_voice,
                        label = "Voice search",
                        onClick = ::toggleVoice,
                        compact = true,
                        role = MatrixControlRole.Action
                    )
                }
            }
        }
        if (providers.size > 1) Row(Modifier.padding(bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            providers.forEach { choice ->
                val isSelected = choice == provider
                MatrixButton(onClick = { if (choice != provider) {
                    stopListeningHardware()
                    searchJob?.cancel(); busy = false; cards = null; error = ""; matchedApps = emptyList(); providerName = choice.name
                    repository.prefs.edit { putString(MatrixPrefs.LAST_SEARCH_PROVIDER, choice.name) }
                } }, role = if (isSelected) MatrixControlRole.Commit else MatrixControlRole.Secondary) {
                    Text(choice.title)
                }
            }
        }
        when {
            busy -> {
                MatrixSearchProgress(
                    progress = searchProgress,
                    steps = searchSteps,
                    provider = provider,
                    aiModel = repository.search.ai.activeModel,
                    aiProviderLabel = repository.search.ai.provider.label
                )
            }
            !cards.isNullOrEmpty() || matchedApps.isNotEmpty() -> {
                val results = cards.orEmpty()
                val providerLabel = if (provider == MatrixSearchProvider.AI) {
                    "${repository.search.ai.activeModel} (${repository.search.ai.provider.label})"
                } else {
                    provider.title
                }
                Column(Modifier.fillMaxSize()) {
                    if (matchedApps.isNotEmpty()) {
                        Column(Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                            Text(
                                text = "Installed Apps (${matchedApps.size})",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                            )
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                itemsIndexed(matchedApps, key = { _, app -> app.id }, contentType = { _, _ -> "app" }) { index, app ->
                                    MatrixAppCard(
                                        app = app,
                                        repository = repository,
                                        width = 160.dp,
                                        onFocus = { _, _ -> },
                                        modifier = if (index == 0) Modifier.focusRequester(firstApp) else Modifier,
                                        showTitle = true,
                                        onClick = {
                                            val appIntent = repository.appIntent(app.id)
                                            if (appIntent != null) {
                                                try {
                                                    context.startActivity(appIntent)
                                                } catch (_: Exception) {
                                                    error = "Cannot open ${app.title}"
                                                }
                                            } else {
                                                error = "Cannot open ${app.title}"
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                    if (results.isNotEmpty()) {
                        val aiSummary = repository.search.lastAiSummary
                        if (provider == MatrixSearchProvider.AI && aiSummary.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp, vertical = 6.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(MatrixLayout.panelCorner))
                                    .border(
                                        width = 1.dp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                                        shape = RoundedCornerShape(MatrixLayout.panelCorner)
                                    )
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(7.dp)
                                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                                        )
                                        Text(
                                            text = stringResource(R.string.search_ai_summary_title),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Text(
                                        text = aiSummary,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
                                    )
                                }
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${results.size} titles found · $providerLabel",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                        AnimatedVisibility(
                            visible = true,
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            enter = fadeIn(tween(350)) + slideInVertically(initialOffsetY = { 30 }, animationSpec = tween(350))
                        ) {
                            MatrixMediaGrid(results, windowWidth, MatrixSafeMode.effectiveColumns(repository.prefs, repository.safeMode), grid, repository,
                                results.first().id, firstResult, { _, _ -> }, open,
                                Modifier.fillMaxSize())
                        }
                    } else if (cards != null) {
                        Text(
                            text = "No movies or series matching \"$query\" found in $providerLabel.",
                            modifier = Modifier.padding(8.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            error.isNotBlank() -> MatrixLoadState(error)
            cards == null -> {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    if (recents.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Recent searches",
                                style = MaterialTheme.typography.titleMedium
                            )
                            MatrixButton(
                                onClick = {
                                    repository.search.clearRecentSearches()
                                    recents = emptyList()
                                },
                                role = MatrixControlRole.Destructive
                            ) {
                                Text("Clear all")
                            }
                        }

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(recents, key = { it }) { item ->
                                MatrixButton(
                                    onClick = {
                                        query = item
                                        submit(item)
                                    },
                                    role = MatrixControlRole.Action
                                ) {
                                    Text(item)
                                }
                            }
                        }
                    }

                    if (provider == MatrixSearchProvider.AI) {
                        val dynamicSuggestionsState = produceState<List<String>?>(initialValue = null, key1 = provider) {
                            value = repository.search.fetchDynamicAiSuggestions()
                        }
                        val suggestions = dynamicSuggestionsState.value
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Suggested for AI Discovery",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
                                )
                                if (suggestions == null) {
                                    Text(
                                        text = "· Discovering live trends…",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                    )
                                }
                            }
                            if (suggestions == null) {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(4) { idx ->
                                        Box(
                                            modifier = Modifier
                                                .width((140 + idx * 30).dp)
                                                .height(36.dp)
                                                .background(
                                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                                    MatrixControlStyle.shape
                                                )
                                        )
                                    }
                                }
                            } else {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(suggestions, key = { it }) { suggestion ->
                                        MatrixButton(
                                            onClick = {
                                                query = suggestion
                                                submit(suggestion)
                                            },
                                            role = MatrixControlRole.Secondary
                                        ) {
                                            Text("✨ $suggestion")
                                        }
                                    }
                                }
                            }
                        }
                    } else if (recents.isEmpty()) {
                        Text(
                            text = "Find a movie or series. Choose a poster to open details in your selected media app.",
                            modifier = Modifier.padding(8.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            else -> MatrixLoadState("No matches. Change the query above and search again.")
        }
    }
    LaunchedEffect(provider == null) { if (provider != null) { withFrameNanos { }; field.requestFocus() } }
}
@Composable
private fun MatrixSearchProgress(
    progress: String,
    steps: List<String>,
    provider: MatrixSearchProvider,
    aiModel: String,
    aiProviderLabel: String
) {
    val isAi = provider == MatrixSearchProvider.AI
    val activeText = progress.ifBlank {
        if (isAi) "Consulting $aiModel ($aiProviderLabel)…" else "Searching ${provider.title}…"
    }

    val colors = MaterialTheme.colorScheme
    // Theme radius token (default 24dp), not the pill-shaped control shape:
    // MatrixControlStyle.shape is RoundedCornerShape(50) for buttons.
    val shape = RoundedCornerShape(MatrixLayout.panelCorner)
    Box(
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 840.dp)
                .fillMaxWidth(0.85f)
                .background(colors.surfaceVariant, shape)
                .border(1.dp, colors.onSurface.copy(alpha = 0.12f), shape)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MatrixSearchOrb()
                    Text(
                        text = stringResource(
                            if (isAi) R.string.search_progress_ai_discovery else R.string.search_progress_searching
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.onSurface
                    )
                }
                if (isAi) {
                    Text(
                        text = "$aiModel · $aiProviderLabel",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            AnimatedContent(
                targetState = activeText,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(220, delayMillis = 40)) +
                        slideInVertically(animationSpec = tween(220, delayMillis = 40)) { h -> h / 2 })
                        .togetherWith(fadeOut(animationSpec = tween(100)))
                },
                label = "activeStreamText"
            ) { msg ->
                Text(
                    text = msg,
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.onSurface
                )
            }

            if (steps.size > 1) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.onSurface.copy(alpha = 0.06f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val displaySteps = steps.takeLast(4)
                    displaySteps.forEachIndexed { index, step ->
                        val isLatest = index == displaySteps.lastIndex
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = if (isLatest) "▶" else "✓",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isLatest) colors.primary else colors.onSurfaceVariant
                            )
                            Text(
                                text = step,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isLatest) colors.onSurface else colors.onSurface.copy(alpha = 0.65f),
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}
