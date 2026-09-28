package com.aile.takip.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FamilyInviteCodecTest {

    @Test
    fun `encode then decode round trips`() {
        val code = FamilyInviteCodec.encode("aile_ABC234", "4821")
        val decoded = FamilyInviteCodec.decode(code)

        assertEquals("aile_ABC234", decoded?.groupId)
        assertEquals("4821", decoded?.passcode)
    }

    @Test
    fun `decode reads invite out of the shared message`() {
        val text = FamilyInviteCodec.shareText("aile_ZZ9K7B", "9182")
        val decoded = FamilyInviteCodec.decode(text)

        assertEquals("aile_ZZ9K7B", decoded?.groupId)
        assertEquals("9182", decoded?.passcode)
    }

    @Test
    fun `decode prefers the compact code when present`() {
        val raw = "selam ${FamilyInviteCodec.encode("aile_TARGET1", "1111")} son"
        val decoded = FamilyInviteCodec.decode(raw)

        assertEquals("aile_TARGET1", decoded?.groupId)
        assertEquals("1111", decoded?.passcode)
    }

    @Test
    fun `decode returns null for unrelated text`() {
        assertNull(FamilyInviteCodec.decode("https://example.com/abc"))
        assertNull(FamilyInviteCodec.decode(""))
        assertNull(FamilyInviteCodec.decode("   "))
        assertNull(FamilyInviteCodec.decode("AILETAKIP:g=aile_X"))
    }

    @Test
    fun `share text never exposes only half of the credentials`() {
        val text = FamilyInviteCodec.shareText("aile_SECRET9", "7788")
        assertTrue(text.contains("aile_SECRET9"))
        assertTrue(text.contains("7788"))
    }
}

class GroupIdTest {

    @Test
    fun `group ids are prefixed and long enough to be unguessable`() {
        val id = SyncPreferences.newGroupId()

        assertTrue("group id should be prefixed", id.startsWith("aile_"))
        assertEquals(21, id.length)
    }

    @Test
    fun `group ids avoid ambiguous characters`() {
        val ambiguous = setOf('0', '1', 'I', 'O')
        repeat(50) {
            val id = SyncPreferences.newGroupId().removePrefix("aile_")
            assertTrue("unexpected character in $id", id.none { it in ambiguous })
        }
    }

    @Test
    fun `group ids are unique`() {
        val ids = (1..1000).map { SyncPreferences.newGroupId() }.toSet()
        assertEquals(1000, ids.size)
    }

    @Test
    fun `two consecutive ids differ`() {
        assertNotEquals(SyncPreferences.newGroupId(), SyncPreferences.newGroupId())
    }
}
