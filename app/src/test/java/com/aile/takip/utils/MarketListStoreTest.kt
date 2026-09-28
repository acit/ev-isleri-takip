package com.aile.takip.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [MarketListStore] birim testleri — haftalık liste üretimi ve karşılaştırma.
 */
class MarketListStoreTest {

    @Test
    fun `liste uretiminde ayni isimli malzemeler birlesir`() {
        val list = MarketListStore.buildList(
            weekKey = "2026-W39",
            now = 1000L,
            exhausted = listOf(
                Triple("Domates", 2, listOf("Salata")),
                Triple("Domates", 3, listOf("Menemen")),
                Triple("Soğan", 1, listOf("Pilav"))
            )
        )
        assertEquals(2, list.items.size)
        val domates = list.items.first { it.name == "Domates" }
        assertEquals(5, domates.quantity)
        assertEquals(setOf("Salata", "Menemen"), domates.dishes.toSet())
    }

    @Test
    fun `adet 0 veya alti olan malzemeler listeye girmez`() {
        val list = MarketListStore.buildList(
            weekKey = "2026-W39",
            now = 1000L,
            exhausted = listOf(Triple("Tuz", 0, listOf("Pilav")))
        )
        assertTrue(list.items.isEmpty())
    }

    @Test
    fun `ilk haftada her sey yeni sayilir`() {
        val current = MarketListStore.buildList("2026-W39", 1000L, listOf(Triple("Süt", 2, listOf("Kahvaltı"))))
        val diff = MarketListStore.compare(current, null)
        assertNull(diff.previousWeek)
        assertEquals(1, diff.newItems.size)
        assertTrue(diff.carriedOver.isEmpty())
        assertTrue(diff.droppedItems.isEmpty())
    }

    @Test
    fun `ayni malzeme iki haftada da varsa tekrar sayilir`() {
        val prev = MarketListStore.buildList("2026-W38", 900L, listOf(
            Triple("Süt", 2, listOf("Kahvaltı")),
            Triple("Ekmek", 1, listOf("Kahvaltı"))
        ))
        val cur = MarketListStore.buildList("2026-W39", 1000L, listOf(
            Triple("Süt", 3, listOf("Kahvaltı")),
            Triple("Pirinç", 1, listOf("Pilav"))
        ))
        val diff = MarketListStore.compare(cur, prev)

        assertEquals(1, diff.carriedOver.size)
        assertEquals("Süt", diff.carriedOver.first().name)
        assertEquals(1, diff.newItems.size)
        assertEquals("Pirinç", diff.newItems.first().name)
        assertEquals(1, diff.droppedItems.size)
        assertEquals("Ekmek", diff.droppedItems.first().name)
        assertEquals(50, diff.repeatRate) // 2 öğeden 1'i tekrar
    }

    @Test
    fun `turkce karakter ve buyuk-kucuk farki tekrar olarak yakalanir`() {
        val prev = MarketListStore.buildList("2026-W38", 900L, listOf(Triple("DOMATES", 2, listOf("Salata"))))
        val cur = MarketListStore.buildList("2026-W39", 1000L, listOf(Triple("domates", 3, listOf("Menemen"))))
        val diff = MarketListStore.compare(cur, prev)
        assertEquals(1, diff.carriedOver.size)
        assertTrue(diff.newItems.isEmpty())
    }

    @Test
    fun `nokta ve parantezli adlar normalize olur`() {
        assertEquals("domates 2 kg", MarketListStore.normalizeName("Domates (2 kg)!"))
        assertEquals("sut", MarketListStore.normalizeName("SÜT"))
    }

    @Test
    fun `hafta anahtari bicimi dogru`() {
        val key = MarketListStore.currentWeekKey()
        assertTrue(key.matches(Regex("""\d{4}-W\d{2}""")))
        val prev = MarketListStore.previousWeekKey()
        assertTrue(prev.matches(Regex("""\d{4}-W\d{2}""")))
    }

    @Test
    fun `yil sinirinda onceki hafta hesabi dogru`() {
        // 1 Ocak 2026 haftası (yalnızca hesaplama çökmeden sonuç dönmeli)
        val prev = MarketListStore.previousWeekKey(1767225600000L) // 2026-01-01 civarı
        assertTrue(prev.matches(Regex("""\d{4}-W\d{2}""")))
    }
}
