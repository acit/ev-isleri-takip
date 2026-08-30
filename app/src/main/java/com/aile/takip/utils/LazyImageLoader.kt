package com.aile.takip.utils

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Lazy-loading image composable for Base64 encoded images.
 * Uses BitmapCache for in-memory caching, decodes on background thread,
 * shows placeholder while loading.
 */
@Composable
fun LazyBase64Image(
    base64: String,
    modifier: Modifier = Modifier,
    maxWidth: Int = 400,
    contentDescription: String? = null,
    contentScale: ContentScale = ContentScale.Crop,
    placeholderSize: Dp = 24.dp
) {
    var imageBitmap by remember(base64) { mutableStateOf<ImageBitmap?>(null) }
    var isLoading by remember(base64) { mutableStateOf(true) }

    LaunchedEffect(base64) {
        if (base64.isBlank()) {
            isLoading = false
            return@LaunchedEffect
        }
        isLoading = true
        val bitmap = BitmapCache.getOrDecode(base64, maxWidth)
        imageBitmap = bitmap?.asImageBitmap()
        isLoading = false
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        when {
            isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(placeholderSize),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            imageBitmap != null -> {
                Image(
                    bitmap = imageBitmap!!,
                    contentDescription = contentDescription,
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)),
                    contentScale = contentScale
                )
            }
            else -> {
                Icon(
                    imageVector = if (base64.isBlank()) Icons.Default.Image else Icons.Default.BrokenImage,
                    contentDescription = contentDescription,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(placeholderSize)
                )
            }
        }
    }
}

/**
 * Remember an image bitmap from Base64 with caching.
 * This avoids decoding the same Base64 on every recomposition.
 */
@Composable
fun rememberBase64Bitmap(base64: String, maxWidth: Int = 400): ImageBitmap? {
    var bitmap by remember(base64) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(base64) {
        if (base64.isBlank()) return@LaunchedEffect
        val decoded = withContext(Dispatchers.IO) {
            BitmapCache.getOrDecode(base64, maxWidth)
        }
        bitmap = decoded?.asImageBitmap()
    }

    return bitmap
}
