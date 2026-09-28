package com.aile.takip.utils

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Fiş/fatura fotoğrafından OCR ile okunan ham metni yapısal fatura alanlarına
 * dönüştürür (başlık, tutar, tarih, kategori tahmini).
 *
 * Tamamen yerel çalışır: ML Kit zaten metni çıkarır, burada yalnızca
 * Türkçe fiş kalıpları (TOPLAM, TUTAR, KDV, Tarih ...) ayrıştırılır.
 */
object ReceiptParser {

    data class ParsedReceipt(
        val title: String,
        val amount: Double?,
        val dueDate: String,      // yyyy-MM-dd; bulunamazsa ""
        val category: String,     // tahmin; bulunamazsa "Genel"
        val confidence: Float     // 0..1 — kaç alan bulundu
    )

    /** Türkçe fişlerde toplam tutar satırı anahtar kelimeleri (öncelik sırasıyla). */
    private val TOTAL_KEYS = listOf(
        "TOPLAM", "TOPLAM TUTAR", "GENEL TOPLAM", "ÖDENECEK", "ODENEN",
        "NAKİT", "NAKIT", "TUTAR", "TOTAL", "BKD", "ARA TOPLAM"
    )

    /** Kategori tahmini için mağaza/satır anahtar kelimeleri. */
    private val CATEGORY_HINTS = linkedMapOf(
        "ECZANE" to "Sağlık", "ECZ" to "Sağlık", "İLAÇ" to "Sağlık",
        "MARKET" to "Market", "MIGROS" to "Market", "A101" to "Market",
        "BİM" to "Market", "BIM" to "Market", "ŞOK" to "Market", "SOK" to "Market",
        "CARREFOUR" to "Market", "RAMSTORE" to "Market",
        "BENZİN" to "Ulaşım", "PETROL" to "Ulaşım", "OPET" to "Ulaşım",
        "SHELL" to "Ulaşım", "PO" to "Ulaşım",
        "ELEKTRİK" to "Faturalar", "SU FATURA" to "Faturalar",
        "DOĞALGAZ" to "Faturalar", "İNTERNET" to "Faturalar", "TELEKOM" to "Faturalar",
        "RESTAURANT" to "Yemek", "LOKANTA" to "Yemek", "KAFE" to "Yemek", "CAF" to "Yemek"
    )

    /**
     * OCR çıktısını ayrıştırır.
     *
     * @param rawText ML Kit TextRecognition çıktısı (satır satır metin)
     */
    fun parse(rawText: String): ParsedReceipt {
        val lines = rawText.lines().map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.isEmpty()) {
            return ParsedReceipt("", null, "", "Genel", 0f)
        }

        val amount = extractAmount(lines)
        val date = extractDate(lines)
        val title = extractTitle(lines)
        val category = guessCategory(rawText)
        var fields = 0
        if (amount != null) fields++
        if (date.isNotEmpty()) fields++
        if (title.isNotBlank()) fields++
        if (category != "Genel") fields++
        val confidence = (fields / 4f).coerceIn(0f, 1f)

        return ParsedReceipt(
            title = title.ifBlank { "Fiş" },
            amount = amount,
            dueDate = date,
            category = category,
            confidence = confidence
        )
    }

    /** Toplam değil — atlanacak satır anahtarları (alt toplam, kdv, iskonto vb.). */
    private val EXCLUDED_KEYS = listOf("ARA TOPLAM", "KDV", "ISKONTO", "İNDİRİM", "İADE", "KAMBRO")

    /**
     * Toplam tutarı bulur: en yüksek öncelikli anahtar kelimeyi içeren satırdaki
     * en büyük para değeri. Tl işaretli sayılar (1.234,56 TL / 1234.56) desteklenir.
     */
    private fun extractAmount(lines: List<String>): Double? {
        // 1) Anahtar kelime geçen satırlardaki para değerleri
        for (key in TOTAL_KEYS) {
            for (line in lines) {
                val upper = line.uppercase(Locale.getDefault())
                if (!upper.contains(key)) continue
                // "ARA TOPLAM" / "KDV" satırları gerçek toplam değildir
                if (EXCLUDED_KEYS.any { upper.contains(it) }) continue
                amountFromLine(line)?.let { return it }
            }
        }
        // 2) Hiç anahtar yoksa: fişteki en büyük para değeri (toplam genelde en büyüktür)
        return lines.maxOfOrNull { amountFromLine(it) ?: 0.0 }?.takeIf { it > 0.0 }
    }

    /** Bir satırdaki tüm para değerlerini çözüp en büyüğünü döner. */
    private fun amountFromLine(line: String): Double? {
        // 1.234,56 / 1234,56 / 1234.56 / 1,234.56 biçimleri
        val regex = Regex("""(\d{1,3}(?:[.,]\d{3})*(?:[.,]\d{2})|\d+(?:[.,]\d{2})?)""")
        return regex.findAll(line.replace("TL", "").replace("₺", ""))
            .mapNotNull { m ->
                normalizeNumber(m.value)?.takeIf { it in 0.01..1_000_000.0 }
            }
            .maxOrNull()
    }


    /** "1.234,56" -> 1234.56 ; "1,234.56" -> 1234.56 ; "1234" -> 1234.0 */
    private fun normalizeNumber(raw: String): Double? {
        var s = raw
        val hasDot = s.contains('.')
        val hasComma = s.contains(',')
        if (hasDot && hasComma) {
            s = if (s.lastIndexOf(',') > s.lastIndexOf('.')) {
                s.replace(".", "").replace(',', '.')   // 1.234,56 (TR)
            } else {
                s.replace(",", "")                      // 1,234.56 (EN)
            }
        } else if (hasComma) {
            // 234,56 -> ondalık; 1,234 -> binlik
            s = if (Regex("""\d{1,3},\d{2}$""").containsMatchIn(s)) s.replace(',', '.')
            else s.replace(",", "")
        } else if (hasDot) {
            // 234.56 -> ondalık; 1.234 -> binlik
            s = if (Regex("""\d{1,3}\.\d{2}$""").containsMatchIn(s)) s
            else s.replace(".", "")
        }
        return s.toDoubleOrNull()
    }

    /**
     * Tarihi bulur: dd.MM.yyyy, dd/MM/yyyy, dd-MM-yyyy, dd.MM.yy biçimleri.
     * Gün geçersse (örn. 32.13.2026) son kullanma/son ödeme olarak gelecekteki
     * ay/yıl kabul edilir.
     */
    private fun extractDate(lines: List<String>): String {
        val dateRegex = Regex("""(\d{1,2})[.,/\-\s](\d{1,2})[.,/\-\s](\d{2,4})""")
        for (line in lines) {
            val m = dateRegex.find(line) ?: continue
            val day = m.groupValues[1].toIntOrNull() ?: continue
            val month = m.groupValues[2].toIntOrNull() ?: continue
            var year = m.groupValues[3].toIntOrNull() ?: continue
            if (year < 100) year += 2000
            if (day in 1..31 && month in 1..12 && year in 2020..2100) {
                return try {
                    val cal = Calendar.getInstance()
                    cal.clear()
                    cal.set(year, month - 1, day)
                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                    sdf.isLenient = false
                    sdf.format(cal.time)
                } catch (_: Exception) {
                    continue
                }
            }
        }
        return ""
    }

    /** Başlık: ilk anlamlı satır (harf içeren, sayı/sembol olmayan). */
    private fun extractTitle(lines: List<String>): String {
        for (line in lines.take(8)) {
            val cleaned = line.replace(Regex("""[^A-Za-zÇĞİÖŞÜçğıöşü ]"""), "").trim()
            if (cleaned.length >= 3) return cleaned
        }
        return ""
    }

    private fun guessCategory(rawText: String): String {
        val upper = rawText.uppercase(Locale.getDefault())
        for ((key, cat) in CATEGORY_HINTS) {
            if (upper.contains(key)) return cat
        }
        return "Genel"
    }

    /** Bugünün tarihi — tarih okunamadıysa son ödeme bugün+7 gün olarak önerilir. */
    fun suggestedDueDate(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(
            Date(System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000L)
        )
}
