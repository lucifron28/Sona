package com.example.sona.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.MaterialTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun SongArtwork(
    artworkUri: String?,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    spinLogoWithVinyl: Boolean = true,
) {
    val context = LocalContext.current
    val artwork by produceState<ImageBitmap?>(initialValue = null, artworkUri) {
        value = withContext(Dispatchers.IO) {
            artworkUri?.let { context.loadArtwork(it)?.asImageBitmap() }
        }
    }

    if (artwork == null) {
        SonaDefaultAlbumArt(
            isPlaying = isPlaying,
            modifier = modifier,
            spinLogoWithVinyl = spinLogoWithVinyl,
        )
    } else {
        Image(
            bitmap = artwork!!,
            contentDescription = "Album artwork",
            modifier = modifier.clip(MaterialTheme.shapes.small),
            contentScale = ContentScale.Crop,
        )
    }
}

private fun Context.loadArtwork(artworkUri: String): Bitmap? = runCatching {
    val uri = Uri.parse(artworkUri)
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    contentResolver.openInputStream(uri).use { input ->
        if (input != null) BitmapFactory.decodeStream(input, null, bounds)
    }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
        null
    } else {
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight)
        }
        contentResolver.openInputStream(uri).use { input ->
            input?.let { BitmapFactory.decodeStream(it, null, options) }
        }
    }
}.getOrNull()

private fun sampleSize(width: Int, height: Int): Int {
    var sample = 1
    while (width / sample > MAX_ARTWORK_DIMENSION || height / sample > MAX_ARTWORK_DIMENSION) {
        sample *= 2
    }
    return sample
}

private const val MAX_ARTWORK_DIMENSION = 1024
