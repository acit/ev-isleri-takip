package com.aile.takip.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aile.takip.utils.QrEncoder

/**
 * Verilen içeriği QR kod olarak gösterir.
 *
 * Bitmap, içerik/boyut/renk değişmedikçe yeniden üretilmez.
 * `filterQuality = None` ile keskin kenarlar korunur (tarayıcılar için önemli).
 */
@Composable
fun QrCodeView(
    content: String,
    modifier: Modifier = Modifier,
    size: Dp = 220.dp,
    darkColor: Color = Color.Black,
    lightColor: Color = Color.White
) {
    val density = LocalDensity.current
    val sizePx = with(density) { size.roundToPx() }.coerceAtLeast(64)
    val darkArgb = darkColor.toArgb()
    val lightArgb = lightColor.toArgb()

    val bitmap = remember(content, sizePx, darkArgb, lightArgb) {
        runCatching {
            QrEncoder.toBitmap(QrEncoder.encode(content, sizePx), darkArgb, lightArgb)
        }.getOrNull()
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = lightColor
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Aile daveti QR kodu",
                modifier = Modifier.size(size).padding(8.dp),
                contentScale = ContentScale.Fit,
                filterQuality = FilterQuality.None
            )
        } else {
            Box(
                modifier = Modifier.size(size),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "QR kod oluşturulamadı",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.DarkGray
                )
            }
        }
    }
}
