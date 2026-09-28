package com.aile.takip.utils

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

/**
 * Fiş/fatura fotoğrafını cihaz üstü OCR ile metne çevirir ve
 * [ReceiptParser] ile yapısal alanlara (başlık, tutar, tarih, kategori) ayrıştırır.
 *
 * Tamamen çevrimdışıdır; görüntü hiçbir sunucuya gönderilmez.
 */
object ReceiptOcr {

    /** OCR + ayrıştırma sonucu. */
    data class Result(
        val parsed: ReceiptParser.ParsedReceipt?,
        val rawText: String,
        val error: String? = null
    )

    /**
     * [uri] konumundaki görüntüyü okuyup fiş alanlarını çıkarır.
     * Geri çağrı main thread dışında çağrılabilir; sonuç addOnSuccessListener ile gelir.
     */
    fun scan(context: Context, uri: Uri, onDone: (Result) -> Unit) {
        val image = try {
            InputImage.fromFilePath(context, uri)
        } catch (e: Exception) {
            onDone(Result(null, "", "Görüntü okunamadı: ${e.message}"))
            return
        }
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                val raw = visionText.text
                val parsed = if (raw.isBlank()) null else ReceiptParser.parse(raw)
                onDone(
                    if (parsed == null) Result(null, raw, "Fişte metin bulunamadı")
                    else Result(parsed, raw)
                )
            }
            .addOnFailureListener { e ->
                onDone(Result(null, "", "OCR hatası: ${e.message}"))
            }
            .addOnCompleteListener {
                recognizer.close()
            }
    }
}
