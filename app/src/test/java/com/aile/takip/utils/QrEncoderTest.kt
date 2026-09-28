package com.aile.takip.utils

import com.aile.takip.sync.FamilyInviteCodec
import com.google.zxing.BinaryBitmap
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.BitMatrix
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Üretilen QR kodun gerçekten okunabilir olduğunu kanıtlar:
 * encode → matris → (kamera yerine) ZXing okuyucu → davet bilgisi.
 */
class QrEncoderTest {

    /** BitMatrix'i, tarayıcının gördüğü siyah/beyaz görüntüye çevirip çözer. */
    private fun decode(matrix: BitMatrix): String? {
        val width = matrix.width
        val height = matrix.height
        val pixels = IntArray(width * height)
        for (y in 0 until height) {
            val rowOffset = y * width
            for (x in 0 until width) {
                pixels[rowOffset + x] = if (matrix.get(x, y)) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
            }
        }
        val source = RGBLuminanceSource(width, height, pixels)
        return QRCodeReader().decode(BinaryBitmap(HybridBinarizer(source))).text
    }

    @Test
    fun `invite code survives a QR round trip`() {
        val code = FamilyInviteCodec.encode("aile_QR7K29ZXC4", "4821")

        val decoded = decode(QrEncoder.encode(code, 512))

        assertEquals(code, decoded)
    }

    @Test
    fun `scanned QR can be turned back into group id and passcode`() {
        val matrix = QrEncoder.encode(FamilyInviteCodec.encode("aile_JOINME42", "9182"), 512)

        val invite = FamilyInviteCodec.decode(decode(matrix) ?: "")

        assertNotNull("QR çözülemedi", invite)
        assertEquals("aile_JOINME42", invite?.groupId)
        assertEquals("9182", invite?.passcode)
    }

    @Test
    fun `qr is square and keeps its quiet zone`() {
        val matrix = QrEncoder.encode(FamilyInviteCodec.encode("aile_TEST1", "1234"), 320)

        assertEquals(matrix.width, matrix.height)
        // Sessiz alan olmazsa kameralar kodu bulmakta zorlanır
        assertTrue("sol üst sessiz alan yok", !matrix.get(0, 0))
        assertTrue("sağ alt sessiz alan yok", !matrix.get(matrix.width - 1, matrix.height - 1))
    }

    @Test
    fun `tiny requested sizes are clamped instead of crashing`() {
        val matrix = QrEncoder.encode(FamilyInviteCodec.encode("aile_X", "1234"), 1)

        assertTrue("çok küçük matris üretildi", matrix.width >= 64)
    }
}
