package com.aile.takip.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [IngredientMatcher] birim testleri — Türkçe yemek/malzeme eşleştirme.
 */
class IngredientMatcherTest {

    private val inventory = listOf(
        "Pirinç" to 2,
        "Tuz" to 1,
        "Zeytinyağı" to 1,
        "Domates Salçası" to 1,
        "Yumurta" to 6,
        "Soğan" to 3
    )

    @Test
    fun `bilinen tarif tam malzeme listesi dondurur`() {
        val reqs = IngredientMatcher.extractIngredients("Mercimek Çorbası")
        assertTrue(reqs.any { it.name == "Mercimek" })
        assertTrue(reqs.any { it.name == "Havuç" })
    }

    @Test
    fun `uzun tarif adi oncelikli eslesir - izmir kofte`() {
        val reqs = IngredientMatcher.extractIngredients("İzmir Köfte")
        // "köfte" değil, "izmir köfte" tarifinin malzemeleri gelmeli
        assertTrue(reqs.any { it.name == "Patates" })
        assertTrue(reqs.any { it.name == "Domates" })
    }

    @Test
    fun `turkce karakter farkina ragmen eslesir`() {
        val reqs = IngredientMatcher.extractIngredients("IZMIR KOFTE")
        assertTrue(reqs.size > 2)
    }

    @Test
    fun `bilinmeyen yemekte anahtar kelime taranir`() {
        val reqs = IngredientMatcher.extractIngredients("Fırında Tavuk Baget")
        assertTrue(reqs.any { it.name == "Tavuk" })
    }

    @Test
    fun `envanterde olan malzeme eksik sayilmaz`() {
        // Pirinç: 2 var, pilav 1 gerektirir -> eksik yok
        val missing = IngredientMatcher.missingFor("Pilav", inventory)
        assertFalse(missing.any { it.requirement.name == "Pirinç" })
    }

    @Test
    fun `envanterde olmayan malzeme eksik raporlanir`() {
        val missing = IngredientMatcher.missingFor("Pilav", inventory)
        assertTrue(missing.any { it.requirement.name == "Tereyağı" && it.missing >= 1 })
    }

    @Test
    fun `kelime sirasi farkli olsa da eslesir - domates salcasi`() {
        // Envanterde "Domates Salçası" var; gereksinim "Domates Salçası" — birebir
        // Ayrıca ters sırada "Salça Domates" yazsa da eşleşmeli
        val inv = listOf("Salça Domates" to 1)
        val missing = IngredientMatcher.missingFor("Makarna", inv)
        assertFalse(missing.any { it.requirement.name == "Domates Salçası" && it.inStock == 0 })
    }

    @Test
    fun `kiler urunleri envanter yoksa gurultu yapmaz`() {
        // "Tuz" envanterde yok ama kiler ürünü — eksik listesinde en öne çıkmamalı
        val missing = IngredientMatcher.missingFor("Omlet", inventory)
        // Yumurta var, Peynir/Maydanoz yok → bunlar önerilmeli; Tuz önerilmemeli
        assertFalse(missing.any { it.requirement.name == "Tuz" })
        assertTrue(missing.any { it.requirement.name == "Peynir" })
    }

    @Test
    fun `bos yemek adi guvenli`() {
        assertTrue(IngredientMatcher.extractIngredients("").isEmpty())
        assertTrue(IngredientMatcher.missingFor("", inventory).isEmpty())
    }
}
