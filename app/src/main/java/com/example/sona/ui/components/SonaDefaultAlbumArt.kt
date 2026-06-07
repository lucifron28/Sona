package com.example.sona.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.sona.R
import com.example.sona.ui.theme.SonaTheme

@Composable
fun SonaDefaultAlbumArt(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    spinLogoWithVinyl: Boolean = true
) {
    val rotation = remember { Animatable(0f) }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            rotation.animateTo(
                targetValue = rotation.value + 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(3000, easing = LinearEasing)
                )
            )
        } else {
            rotation.stop()
        }
    }

    val labelColor = MaterialTheme.colorScheme.primaryContainer
    val holeColor = MaterialTheme.colorScheme.background
    val ringColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
    val backingColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.36f)
    val rimColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
    val grooveColor = Color.White.copy(alpha = 0.05f)

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .background(backingColor, CircleShape)
            .border(width = 1.dp, color = ringColor, shape = CircleShape)
            .padding(VinylRingPadding),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .graphicsLayer {
                    rotationZ = rotation.value
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2, size.height / 2)
                val radius = size.minDimension / 2
                val rimStroke = 1.dp.toPx()

                // Draw vinyl disc (always dark to represent a vinyl record)
                drawCircle(
                    color = Color(0xFF181818),
                    radius = radius,
                    center = center
                )

                drawCircle(
                    color = rimColor,
                    radius = radius - rimStroke,
                    center = center,
                    style = Stroke(width = rimStroke)
                )

                // Draw grooves
                val grooveCount = 4
                for (i in 1..grooveCount) {
                    drawCircle(
                        color = grooveColor,
                        radius = radius * (0.4f + 0.12f * i),
                        center = center,
                        style = Stroke(width = 1.dp.toPx())
                    )
                }

                // Draw center label (theme-aware)
                drawCircle(
                    color = labelColor,
                    radius = radius * 0.35f,
                    center = center
                )

                // Draw center hole (theme-aware background)
                drawCircle(
                    color = holeColor,
                    radius = radius * 0.05f,
                    center = center
                )
            }

            // Sona Logo in the center
            // TODO: Replace with R.drawable.sona_symbol if a specific symbol asset is provided later.
            // Currently using R.drawable.sona_logo as the fallback/placeholder.
            Image(
                painter = painterResource(id = R.drawable.sona_logo),
                contentDescription = "Sona Logo",
                modifier = Modifier
                    .fillMaxSize(0.25f)
                    .graphicsLayer {
                        if (!spinLogoWithVinyl) {
                            // Counter-rotate the logo so it appears upright while the vinyl spins
                            rotationZ = -rotation.value
                        }
                    }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SonaDefaultAlbumArtPreview_Playing() {
    SonaTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            SonaDefaultAlbumArt(
                isPlaying = true,
                modifier = Modifier.size(200.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SonaDefaultAlbumArtPreview_Paused() {
    SonaTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            SonaDefaultAlbumArt(
                isPlaying = false,
                modifier = Modifier.size(200.dp)
            )
        }
    }
}

private val VinylRingPadding = 3.dp
