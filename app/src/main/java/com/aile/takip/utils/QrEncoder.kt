package com.aile.takip.utils

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/**
 * Aile davet kodunu QR koda çevirir.
 *
 * Tarama tarafında ML Kit kullanılır; üretim (encode) için ZXing core kullanılır.
 * Kod üretimi tamamen cihazda yapılır, ağ gerekmez.
 *
 * ZXing varsayılan olarak 4 modül genişliğinde sessiz alan (quiet zone) bırakır;
 * bu, telefon kameralarının kodu güvenilir şekilde okuması için korunur.
 */
object QrEncoder {

    /** QR matrisini üretir. Android'e bağımlı değildir (birim testte kullanılabilir). */
    fun encode(
        content: String,
        sizePx: Int,
        errorCorrection: ErrorCorrectionLevel = ErrorCorrectionLevel.M
    ): BitMatrix {
        val hints: Map<EncodeHintType, Any> = mapOf(
            EncodeHintType.ERROR_CORRECTION to errorCorrection,
            EncodeHintType.CHARACTER_SET to "UTF-8"
        )
        return QRCodeWriter().encode(
            content,
            BarcodeFormat.QR_CODE,
            sizePx.coerceAtLeast(64),
            sizePx.coerceAtLeast(64),
            hints
        )
    }

    /** Matrisi ekranda gösterilebilecek bir bitmap'e çevirir. */
    fun toBitmap(
        matrix: BitMatrix,
        darkColor: Int = Color.BLACK,
        lightColor: Int = Color.WHITE
    ): Bitmap {
        val width = matrix.width
        val height = matrix.height
        val pixels = IntArray(width * height)
        for (y in 0 until height) {
            val rowOffset = y * width
            for (x in 0 until width) {
                pixels[rowOffset + x] = if (matrix.get(x, y)) darkColor else lightColor
            }
        }
        return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
            setPixels(pixels, 0, width, 0, 0, width, height)
        }
    }
}
