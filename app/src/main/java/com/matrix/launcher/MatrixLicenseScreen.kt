package com.matrix.launcher

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun MatrixLicenseScreen(back: () -> Unit) {
    val context = LocalContext.current
    val scroll = rememberScrollState()
    val initial = remember { FocusRequester() }
    val text by produceState("Loading licenses…") { value = withContext(Dispatchers.IO) {
        context.assets.open("licenses.txt").bufferedReader().use { it.readText() }
    } }
    Column {
        MatrixPageHeader("Licenses", back)
        MatrixScrollViewport(Modifier.weight(1f)) {
        Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.fillMaxSize().matrixScrollEdges({ scroll.canScrollBackward }, { scroll.canScrollForward }).verticalScroll(scroll).focusRequester(initial).focusable().padding(vertical = 16.dp))
        }
    }
    LaunchedEffect(Unit) { withFrameNanos { }; initial.requestFocus() }
}

@Composable
internal fun MatrixDataCredits(back: () -> Unit) {
    val initial = remember { FocusRequester() }
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        MatrixPageHeader("Data sources", back, initial)
        Text("Optional catalog integrations", style = MaterialTheme.typography.titleMedium)
        Image(painterResource(R.drawable.tmdb_logo), "TMDB", Modifier.size(154.dp, 20.dp))
        Text("This product uses the TMDB API but is not endorsed or certified by TMDB.", style = MaterialTheme.typography.bodyMedium)
        Text("themoviedb.org · Movie and series metadata and posters when TMDB search is enabled.", style = MaterialTheme.typography.bodySmall)
        Text("SIMKL", style = MaterialTheme.typography.titleMedium)
        Text("simkl.com · Connected lists, library and optional catalog search. SIMKL's shared account request limits apply.", style = MaterialTheme.typography.bodyMedium)
    }
    LaunchedEffect(Unit) { withFrameNanos { }; initial.requestFocus() }
}
