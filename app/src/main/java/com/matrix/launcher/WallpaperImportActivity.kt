package com.matrix.launcher

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.tv.material3.Text
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.core.content.IntentCompat
import androidx.lifecycle.lifecycleScope
import androidx.tv.material3.MaterialTheme
import coil.compose.AsyncImage
import com.matrix.launcher.core.SharedImagePolicy
import kotlinx.coroutines.launch

/** User-confirmed, bounded image import. Incoming intents are never forwarded or auto-applied. */
class WallpaperImportActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val uris = sharedImages(intent, packageName + ".theme")
        if (uris.isEmpty()) { Toast.makeText(this, "Share a JPEG or PNG image using a content provider.", Toast.LENGTH_LONG).show();finish();return }
        val repository = MatrixWallpaperRepository(applicationContext, getSharedPreferences("personal_home", MODE_PRIVATE))
        setContent { MatrixTvTheme {
            var busy by remember { mutableStateOf(false) }
            var error by remember { mutableStateOf("") }
            val initial = remember { FocusRequester() }
            CompositionLocalProvider(LocalMatrixPagePath provides "Shared picture") {
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.CenterEnd) {
                    MatrixPanel {
                        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                            MatrixPageHeader("Import wallpaper", ::finish)
                            Text(error.ifBlank { "${uris.size} selected picture(s). Save for offline use, up to 20 imports. Your current wallpaper stays selected; use Choose saved wallpaper to change it." },
                                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            AsyncImage(uris.first(), "Shared picture preview", Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(MatrixLayout.cardCorner)), contentScale = ContentScale.Fit)
                            if (busy) MatrixLoadState("Importing pictures…", true)
                            else Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)) {
                                MatrixButton(onClick = ::finish, role = MatrixControlRole.Secondary) { Text("Cancel") }
                                MatrixButton(onClick = {
                                    busy = true
                                    lifecycleScope.launch {
                                        try {
                                            val count = repository.import(uris)
                                            if (count > 0) { Toast.makeText(this@WallpaperImportActivity, "$count pictures imported.", Toast.LENGTH_LONG).show();finish() }
                                            else error = "No supported images imported. Try a JPEG or PNG smaller than 10 MB."
                                        } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
                                        catch (failure: Exception) { error = "Import failed. Reopen or share the picture and try again." }
                                        finally { busy = false }
                                    }
                                }, modifier = Modifier.focusRequester(initial), role = MatrixControlRole.Commit) { Text("Import") }
                            }
                        }
                    }
                }
            }
            LaunchedEffect(busy) { if (!busy) { withFrameNanos { };initial.requestFocus() } }
        } }
    }
}

internal fun sharedImages(intent: Intent, ownAuthority: String): List<Uri> {
    val values = try { when (intent.action) {
        Intent.ACTION_VIEW -> listOfNotNull(intent.data)
        Intent.ACTION_SEND -> listOfNotNull(IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java))
        Intent.ACTION_SEND_MULTIPLE -> IntentCompat.getParcelableArrayListExtra(intent, Intent.EXTRA_STREAM, Uri::class.java).orEmpty()
        else -> emptyList()
    } } catch (_: RuntimeException) { return emptyList() }
    return values.take(SharedImagePolicy.LIMIT).distinct().filter { SharedImagePolicy.allowed(intent.type, it.toString(), ownAuthority) }
}
