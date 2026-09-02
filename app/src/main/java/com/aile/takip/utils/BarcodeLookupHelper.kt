package com.aile.takip.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * GS1 Turkey Barkod Arama Servisi
 * https://gs1tr.org/view/verified/search.php
 * 
 * Barkod numarasina gore urun bilgilerini ceker.
 */
object BarcodeLookupHelper {

    data class ProductInfo(
        val barcode: String = "",
        val name: String = "",
        val brand: String = "",
        val category: String = "",
        val description: String = "",
        val found: Boolean = false
    )

    /**
     * Verilen barkod icin GS1 Turkey servisinde arama yapar.
     * Coroutine disinda calistirilmalidir (withContext(Dispatchers.IO)).
     */
    suspend fun lookupBarcode(barcode: String): ProductInfo {
        return withContext(Dispatchers.IO) {
            try {
                // GS1 Turkey search URL
                val encodedBarcode = URLEncoder.encode(barcode, "UTF-8")
                val url = URL("https://gs1tr.org/view/verified/search.php#!")
                
                // Try to fetch product info
                // Note: GS1 Turkey requires POST request with specific parameters
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                connection.setRequestProperty("User-Agent", "AileTakip/3.3.1")
                connection.doOutput = true
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                
                val postData = "searchTerm=$encodedBarcode&searchType=barcode"
                connection.outputStream.write(postData.toByteArray())
                
                val responseCode = connection.responseCode
                
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().readText()
                    parseProductResponse(response, barcode)
                } else {
                    // Fallback: Try alternative lookup methods
                    fallbackLookup(barcode)
                }
            } catch (e: Exception) {
                // If GS1 service is unavailable, try fallback
                fallbackLookup(barcode)
            }
        }
    }

    /**
     * GS1 Turkey yanitini parse eder.
     * Gercek servis yanitina gore guncellenmelidir.
     */
    private fun parseProductResponse(response: String, barcode: String): ProductInfo {
        // GS1 Turkey response parsing
        // Gercek yanit formatina gore guncellenmeli
        return if (response.contains("productName") || response.contains("product_name")) {
            ProductInfo(
                barcode = barcode,
                name = extractField(response, "productName") ?: extractField(response, "product_name") ?: "",
                brand = extractField(response, "brand") ?: "",
                category = guessCategory(extractField(response, "productName") ?: extractField(response, "product_name") ?: ""),
                description = extractField(response, "description") ?: "",
                found = true
            )
        } else {
            ProductInfo(
                barcode = barcode,
                name = "",
                found = false
            )
        }
    }

    /**
     * Yanit HTML/JSON icinden alan degerini cikarir.
     */
    private fun extractField(response: String, fieldName: String): String? {
        // Simple extraction - gercek parse logigi buraya eklenecek
        val patterns = listOf(
            "\"$fieldName\"\\s*:\\s*\"([^\"]+)\"".toRegex(),
            "<[^>]*$fieldName[^>]*>([^<]+)<".toRegex(),
            "$fieldName=([^&\"]+)".toRegex()
        )
        
        for (pattern in patterns) {
            val match = pattern.find(response)
            if (match != null) {
                return match.groupValues[1].trim()
            }
        }
        return null
    }

    /**
     * GS1 servisi calismazsa alternatif yontemle arama yapar.
     * Yerel veritabanindan veya baska bir servisten arar.
     */
    private suspend fun fallbackLookup(barcode: String): ProductInfo {
        // Turk barcode prefix kontrolu
        // GS1 Turkiye: 869 prefix
        val prefix = if (barcode.length >= 3) barcode.substring(0, 3) else ""
        
        return when (prefix) {
            "869" -> ProductInfo(
                barcode = barcode,
                name = guessProductFromPrefix(barcode),
                brand = "",
                category = guessCategory(guessProductFromPrefix(barcode)),
                found = false
            )
            else -> ProductInfo(
                barcode = barcode,
                name = "",
                found = false
            )
        }
    }

    /**
     * Barkod on ekine gore urun tahmini yapar.
     * Gercek uygulamada veritabani kullanilmalidir.
     */
    private fun guessProductFromPrefix(barcode: String): String {
        // Bu sadece ornek - gercek uygulamada veritabani kullanilmalidir
        val prefix = if (barcode.length >= 4) barcode.substring(0, 4) else barcode
        
        val productPrefixes = mapOf(
            "8690" to "Turk Urunu",
            "8691" to "Turk Urunu",
            "8692" to "Turk Urunu",
            "8693" to "Turk Urunu",
            "8694" to "Turk Urunu",
            "8695" to "Turk Urunu",
            "8696" to "Turk Urunu",
            "8697" to "Turk Urunu",
            "8698" to "Turk Urunu",
            "8699" to "Turk Urunu"
        )
        
        return productPrefixes[prefix] ?: "Bilinmeyen Urun"
    }

    /**
     * Urun adina gore kategori tahmini yapar.
     */
    fun guessCategory(productName: String): String {
        val name = productName.lowercase()
        
        return when {
            // Gida
            name.containsAny("süt", "yoğurt", "peynir", "tereyağı", "yumurta", "ekmek", "sıvı yağ") -> "Gıda"
            name.containsAny("meyve", "sebze", "domates", "biber", "soğan", "patates", "havuç") -> "Gıda"
            name.containsAny("et", "tavuk", "balık", "kıyma", "sosis", "sucuk") -> "Gıda"
            name.containsAny("makarna", "pirinç", "bulgur", "un", "şeker", "tuz") -> "Gıda"
            name.containsAny("çikolata", "bisküvi", "kek", "kurabiye", "ketenpäre") -> "Gıda"
            name.containsAny("meyve suyu", "su", "çay", "kahve", "kola", "gazoz") -> "Gıda"
            name.containsAny("konserve", "salça", "zeytinyağı", "sirke") -> "Gıda"
            
            // Temizlik
            name.containsAny("deterjan", "çamaşır", "bulaşık", "yumuşatıcı") -> "Temizlik"
            name.containsAny("sabun", "şampuan", "duş jeli", "deodorant") -> "Kişisel Bakım"
            name.containsAny("diş macunu", "diş fırçası", "ağız bakım") -> "Kişisel Bakım"
            name.containsAny("kağıt", "peçete", "tuvalet kağıdı", "havlu") -> "Temizlik"
            name.containsAny("bez", "mop", " sünger", "fırça") -> "Temizlik"
            
            // Kisisel Bakim
            name.containsAny("kreml", "losyon", "güneş kremi", "cilt") -> "Kişisel Bakım"
            name.containsAny("parfüm", "kolonya", "parfüm") -> "Kişisel Bakım"
            
            // Saglik
            name.containsAny("ilaç", "vitamin", "takviye", "sağlık") -> "Sağlık"
            name.containsAny("maskara", "ruj", "makyaj") -> "Kişisel Bakım"
            
            // Ev
            name.containsAny("ampul", "pil", "priz", "anahtar") -> "Ev Gereçleri"
            name.containsAny("tabak", "bardak", "kaşık", "çatal", "bıçak") -> "Ev Gereçleri"
            name.containsAny("tencere", "tava", "fırın", "kap") -> "Ev Gereçleri"
            
            // Cocuk
            name.containsAny("bebek", "çocuk", "oyuncak", "biberon") -> "Çocuk"
            
            else -> "Genel"
        }
    }

    private fun String.containsAny(vararg keywords: String): Boolean {
        return keywords.any { this.contains(it, ignoreCase = true) }
    }
}
