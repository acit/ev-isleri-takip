package com.aile.takip.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [ReceiptParser] birim testleri — Türkçe fiş kalıpları.
 */
class ReceiptParserTest {

    @Test
    fun `TOPLAM satirindaki tutar bulunur`() {
        val text = """
            MIGROS JUNCTION
            Istanbul
            ----------------------
            EKMEK 2 x 7,50 15,00
            SÜT 1 x 24,90 24,90
            ----------------------
            ARA TOPLAM 39,90
            KDV %10 3,99
            TOPLAM 43,89 TL
        """.trimIndent()

        val r = ReceiptParser.parse(text)
        assertEquals(43.89, r.amount!!, 0.01)
    }

    @Test
    fun `binlik ayracli tutar dogru cozulur`() {
        val text = """
            ABC MARKET
            Fatura Tutari
            TOPLAM 1.234,56 TL
        """.trimIndent()

        val r = ReceiptParser.parse(text)
        assertEquals(1234.56, r.amount!!, 0.01)
    }

    @Test
    fun `Ingiliz binlik ayracı da doğru çözülür`() {
        val text = "TOTAL 1,234.56"
        val r = ReceiptParser.parse(text)
        assertEquals(1234.56, r.amount!!, 0.01)
    }

    @Test
    fun `tarih gun-ay-yil biciminde bulunur`() {
        val text = """
            ECZANE NEBAHAT
            Tarih: 25.12.2026 14:33
            TOPLAM 89,50 TL
        """.trimIndent()

        val r = ReceiptParser.parse(text)
        assertEquals("2026-12-25", r.dueDate)
        assertEquals("Sağlık", r.category)
    }

    @Test
    fun `baslik ilk anlamli satirdan alinir`() {
        val text = """
            BİM BIRLIK
            1234567890
            TOPLAM 55,00 TL
        """.trimIndent()

        val r = ReceiptParser.parse(text)
        assertEquals("BİM BIRLIK", r.title)
        assertEquals("Market", r.category)
    }

    @Test
    fun `anahtar kelime yoksa en buyuk tutar secilir`() {
        val text = """
            KAFE ADANA
            2 X KAHVALTI 180,00
            SERVIS 20,00
            200,00
        """.trimIndent()

        val r = ReceiptParser.parse(text)
        assertEquals(200.0, r.amount!!, 0.01)
        assertEquals("Yemek", r.category)
    }

    @Test
    fun `bos metin guvenli doner`() {
        val r = ReceiptParser.parse("")
        assertNull(r.amount)
        assertEquals("", r.dueDate)
        assertEquals("Genel", r.category)
        assertEquals(0f, r.confidence, 0.001f)
    }

    @Test
    fun `KDV satiri toplam olarak secilmez`() {
        val text = """
            ŞOK MARKET
            ÜRÜN A 32,50
            KDV %10 3,25
            TOPLAM 35,75 TL
        """.trimIndent()

        val r = ReceiptParser.parse(text)
        assertEquals(35.75, r.amount!!, 0.01)
    }

    @Test
    fun `guven skoru bulunan alan sayisiyla artar`() {
        val text = """
            BİM
            Tarih 01.03.2027
            TOPLAM 100,00 TL
        """.trimIndent()

        val r = ReceiptParser.parse(text)
        assertTrue("confidence beklenen: >0.5, gelen: ${r.confidence}", r.confidence > 0.5f)
    }
}
