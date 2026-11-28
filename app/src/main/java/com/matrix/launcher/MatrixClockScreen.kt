package com.matrix.launcher

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/** Low-brightness, minute-driven burn-in movement; no continuously running animation. */
@Composable
internal fun MatrixClockScreen() {
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    val locale = LocalLocale.current.platformLocale
    val timeFormat = remember(locale) { DateTimeFormatter.ofPattern("h:mm", locale) }
    val dateFormat = remember(locale) { DateTimeFormatter.ofPattern("EEEE, d MMMM", locale) }
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val wallpapers = remember { MatrixWallpaperRepository(context, context.getSharedPreferences("personal_home", 0)) }
    var image by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(lifecycle) { lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
        while (isActive) { now = LocalDateTime.now(); image = wallpapers.current(); delay(60_000L - System.currentTimeMillis() % 60_000L) }
    } }
    Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
        image?.let { AsyncImage(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .7f)))
        Column(Modifier.offset { IntOffset(((now.minute % 7 - 3) * 12).dp.roundToPx(), ((now.minute % 5 - 2) * 10).dp.roundToPx()) }, horizontalAlignment = Alignment.CenterHorizontally) {
            Text(now.format(timeFormat), style = MaterialTheme.typography.displayLarge)
            Text(now.format(dateFormat), style = MaterialTheme.typography.bodyLarge)
        }
    }
}
