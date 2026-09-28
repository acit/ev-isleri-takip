package com.aile.takip.sync

import org.junit.Assert.assertEquals
import org.junit.Test

class ConflictResolverTest {

    @Test
    fun `newer local record wins over remote`() {
        val winner = ConflictResolver.resolveUpdate(
            localVersion = 200L,
            remoteVersion = 100L,
            localContent = { "yerel" },
            remoteContent = { "uzak" }
        )

        assertEquals(ConflictWinner.LOCAL, winner)
    }

    @Test
    fun `newer remote record wins over local`() {
        val winner = ConflictResolver.resolveUpdate(
            localVersion = 100L,
            remoteVersion = 200L,
            localContent = { "yerel" },
            remoteContent = { "uzak" }
        )

        assertEquals(ConflictWinner.REMOTE, winner)
    }

    @Test
    fun `identical records with the same version need no write`() {
        val winner = ConflictResolver.resolveUpdate(
            localVersion = 100L,
            remoteVersion = 100L,
            localContent = { "{\"a\":1}" },
            remoteContent = { "{\"a\":1}" }
        )

        assertEquals(ConflictWinner.EQUAL, winner)
    }

    @Test
    fun `same version but different content is broken deterministically`() {
        // İki cihaz aynı milisaniyede düzenlerse sürümler eşit olur.
        // İki cihaz da AYNI kararı vermeli, yoksa kalıcı ayrışma olur.
        val fromDeviceA = ConflictResolver.resolveUpdate(
            localVersion = 100L,
            remoteVersion = 100L,
            localContent = { "{ \"title\": \"A\" }" },
            remoteContent = { "{ \"title\": \"B\" }" }
        )
        // Karşı cihazda rolleri yer değiştirir
        val fromDeviceB = ConflictResolver.resolveUpdate(
            localVersion = 100L,
            remoteVersion = 100L,
            localContent = { "{ \"title\": \"B\" }" },
            remoteContent = { "{ \"title\": \"A\" }" }
        )

        assertEquals(ConflictWinner.REMOTE, fromDeviceA)
        assertEquals(ConflictWinner.LOCAL, fromDeviceB)
        // Cihaz A uzak veriyi aldı (B), cihaz B yerelini korudu (B) → aynı sonuç
    }

    @Test
    fun `content is only serialized when versions tie`() {
        var localCalls = 0
        var remoteCalls = 0

        ConflictResolver.resolveUpdate(
            localVersion = 500L,
            remoteVersion = 100L,
            localContent = { localCalls++; "yerel" },
            remoteContent = { remoteCalls++; "uzak" }
        )

        assertEquals("sürümler farklıyken içerik üretilmemeli", 0, localCalls)
        assertEquals("sürümler farklıyken içerik üretilmemeli", 0, remoteCalls)
    }

    @Test
    fun `editing a record after it was deleted resurrects it`() {
        val winner = ConflictResolver.resolveDeletion(localVersion = 300L, deletedAt = 200L)

        assertEquals(ConflictWinner.LOCAL, winner)
    }

    @Test
    fun `deletion wins when the record was not edited afterwards`() {
        val winner = ConflictResolver.resolveDeletion(localVersion = 100L, deletedAt = 200L)

        assertEquals(ConflictWinner.REMOTE, winner)
    }

    @Test
    fun `deletion wins on an exact tie`() {
        val winner = ConflictResolver.resolveDeletion(localVersion = 200L, deletedAt = 200L)

        assertEquals(ConflictWinner.REMOTE, winner)
    }
}
