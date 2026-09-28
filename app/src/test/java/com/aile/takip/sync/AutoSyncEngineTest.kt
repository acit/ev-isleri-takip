package com.aile.takip.sync

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Gönderilenleri kaydeden sahte yazma tarafı (ağ yok). */
private class FakeSink : SyncSink {
    val pushed = mutableListOf<Pair<String, String>>()   // tablo -> kayıt kimliği
    val pushedContent = mutableListOf<Map<String, Any>>()
    val deleted = mutableListOf<Pair<String, String>>()  // tablo -> kayıt kimliği

    override suspend fun push(table: String, id: String, data: Map<String, Any>) {
        pushed += table to id
        pushedContent += data
    }

    override suspend fun deleteWithTombstone(table: String, id: String) {
        deleted += table to id
    }
}

private class FakeSource(
    private val table: String,
    private val flow: Flow<List<SyncRow>>
) : SyncSource {
    override fun flows(): Map<String, Flow<List<SyncRow>>> = mapOf(table to flow)
}

private fun row(id: String, title: String = "", version: Long = 100L) =
    SyncRow(id, mapOf("id" to id, "title" to title, "syncVersion" to version))

@OptIn(ExperimentalCoroutinesApi::class)
class AutoSyncEngineTest {

    private val table = "tasks"

    /** Sadece karar mantığını test eden motorlar için kaynağın önemi yok. */
    private fun idleEngine(sink: SyncSink) =
        AutoSyncEngine(FakeSource(table, MutableStateFlow<List<SyncRow>>(emptyList())), sink)

    // ============================================================
    // 1) DEĞİŞİKLİK TESPİTİ (diff)
    // ============================================================

    @Test
    fun `first emission only sets the baseline and pushes nothing`() {
        val sink = FakeSink()
        val engine = idleEngine(sink)

        val diff = engine.computeDiff(table, listOf(row("t1", "Alışveriş")))

        assertEquals(SyncDiff.Baseline, diff)
        assertTrue("temel durum gönderilmemeli", sink.pushed.isEmpty())
    }

    @Test
    fun `a new record is detected as a change`() {
        val engine = idleEngine(FakeSink())
        engine.computeDiff(table, listOf(row("t1", "Mevcut")))

        val diff = engine.computeDiff(table, listOf(row("t1", "Mevcut"), row("t2", "Yeni görev")))

        assertTrue("yeni kayıt değişiklik olarak görülmeli", diff is SyncDiff.Changes)
        assertEquals(listOf("t2"), (diff as SyncDiff.Changes).changed.map { it.first })
    }

    @Test
    fun `an edited record is detected as a change`() {
        val engine = idleEngine(FakeSink())
        engine.computeDiff(table, listOf(row("t1", "Eski başlık")))

        val diff = engine.computeDiff(table, listOf(row("t1", "Yeni başlık")))

        val changes = diff as SyncDiff.Changes
        assertEquals(listOf("t1"), changes.changed.map { it.first })
        assertEquals("Yeni başlık", changes.changed.first().second["title"])
    }

    @Test
    fun `unchanged rows produce no changes at all`() {
        val engine = idleEngine(FakeSink())
        val rows = listOf(row("t1", "A"), row("t2", "B"))
        engine.computeDiff(table, rows)

        assertEquals(SyncDiff.Empty, engine.computeDiff(table, rows))
    }

    @Test
    fun `only the changed record is sent, not the whole table`() = runTest {
        val sink = FakeSink()
        val engine = idleEngine(sink)
        val rows = (1..5).map { row("t$it", "Görev $it") }
        engine.computeDiff(table, rows)

        val edited = rows.toMutableList().also { it[2] = row("t3", "Görev 3 (düzenlendi)") }
        engine.applyChanges(table, engine.computeDiff(table, edited) as SyncDiff.Changes)

        assertEquals("yalnızca değişen kayıt gönderilmeli", 1, sink.pushed.size)
        assertEquals("t3", sink.pushed.first().second)
    }

    @Test
    fun `after a change is sent it is not sent again`() = runTest {
        val sink = FakeSink()
        val engine = idleEngine(sink)
        engine.computeDiff(table, listOf(row("t1", "A")))
        val edited = listOf(row("t1", "B"))
        engine.applyChanges(table, engine.computeDiff(table, edited) as SyncDiff.Changes)

        val next = engine.computeDiff(table, edited)

        assertEquals(SyncDiff.Empty, next)
        assertEquals(1, sink.pushed.size)
    }

    // ============================================================
    // 2) SİLME YAYILIMI
    // ============================================================

    @Test
    fun `a deleted record is reported as removed`() {
        val engine = idleEngine(FakeSink())
        engine.computeDiff(table, listOf(row("t1"), row("t2")))

        val diff = engine.computeDiff(table, listOf(row("t1")))

        val changes = diff as SyncDiff.Changes
        assertEquals(listOf("t2"), changes.removed)
        assertTrue("silinen kayıt gönderilmemeli", changes.changed.isEmpty())
    }

    @Test
    fun `deleting propagates a tombstone and does not push the record`() = runTest {
        val sink = FakeSink()
        val engine = idleEngine(sink)
        engine.computeDiff(table, listOf(row("t1"), row("t2")))

        engine.applyChanges(table, engine.computeDiff(table, listOf(row("t1"))) as SyncDiff.Changes)

        assertEquals(listOf(table to "t2"), sink.deleted)
        assertTrue("silme gönderim değil, mezar taşıdır", sink.pushed.isEmpty())
    }

    @Test
    fun `a deletion is not propagated twice`() = runTest {
        val sink = FakeSink()
        val engine = idleEngine(sink)
        engine.computeDiff(table, listOf(row("t1"), row("t2")))
        val remaining = listOf(row("t1"))
        engine.applyChanges(table, engine.computeDiff(table, remaining) as SyncDiff.Changes)

        val next = engine.computeDiff(table, remaining)

        assertEquals(SyncDiff.Empty, next)
        assertEquals("silme tekrarlanmamalı", 1, sink.deleted.size)
    }

    @Test
    fun `adding and deleting in one batch are both handled`() = runTest {
        val sink = FakeSink()
        val engine = idleEngine(sink)
        engine.computeDiff(table, listOf(row("t1"), row("t2")))

        engine.applyChanges(
            table,
            engine.computeDiff(table, listOf(row("t1"), row("t3", "Yeni"))) as SyncDiff.Changes
        )

        assertEquals(listOf(table to "t3"), sink.pushed)
        assertEquals(listOf(table to "t2"), sink.deleted)
    }

    // ============================================================
    // 3) ECHO KORUMASI
    // ============================================================

    @Test
    fun `remote data applied locally is not pushed back`() {
        val engine = idleEngine(FakeSink())
        engine.computeDiff(table, listOf(row("t1", "Yerel")))

        // Uzak veri uygulanıyor (FirebaseSyncService bu bayrağı bırakır)
        engine.ignoreNextChange(table)
        val diff = engine.computeDiff(table, listOf(row("t1", "Uzak")))

        assertEquals("uzaktan gelen veri gönderilmemeli", SyncDiff.Baseline, diff)
    }

    @Test
    fun `echo protection does not swallow a later local edit`() {
        val engine = idleEngine(FakeSink())
        engine.computeDiff(table, listOf(row("t1", "Yerel")))

        engine.ignoreNextChange(table)
        engine.computeDiff(table, listOf(row("t1", "Uzak")))   // temel durum: Uzak

        val diff = engine.computeDiff(table, listOf(row("t1", "Uzak + yerel düzenleme")))

        assertTrue("yerel düzenleme hâlâ tespit edilmeli", diff is SyncDiff.Changes)
        assertEquals(listOf("t1"), (diff as SyncDiff.Changes).changed.map { it.first })
    }

    @Test
    fun `a pushed record is not echoed back as a change`() = runTest {
        val sink = FakeSink()
        val engine = idleEngine(sink)
        engine.computeDiff(table, listOf(row("t1", "A")))
        val edited = listOf(row("t1", "B"))
        engine.applyChanges(table, engine.computeDiff(table, edited) as SyncDiff.Changes)

        // Firebase kendi yazdığımız veriyi geri gönderir
        engine.ignoreNextChange(table)
        assertEquals(SyncDiff.Baseline, engine.computeDiff(table, edited))
        assertEquals("kendi gönderdiğimiz veri tekrar gönderilmemeli", 1, sink.pushed.size)
    }

    @Test
    fun `disabled auto sync sends nothing but still tracks the baseline`() = runTest {
        val sink = FakeSink()
        val engine = idleEngine(sink)
        engine.computeDiff(table, listOf(row("t1", "A")))

        engine.setEnabled(false)
        val diff = engine.computeDiff(table, listOf(row("t1", "B")))
        assertEquals(SyncDiff.Disabled, diff)
        assertTrue(sink.pushed.isEmpty())

        // Kapalıyken değişiklik gönderilmez; tekrar açılınca yalnızca yeni değişiklikler gider
        engine.setEnabled(true)
        assertEquals(SyncDiff.Empty, engine.computeDiff(table, listOf(row("t1", "B"))))

        engine.applyChanges(table, engine.computeDiff(table, listOf(row("t1", "C"))) as SyncDiff.Changes)
        assertEquals(1, sink.pushed.size)
    }

    @Test
    fun `paused auto sync keeps changes for later instead of losing them`() = runTest {
        val sink = FakeSink()
        val engine = idleEngine(sink)
        engine.computeDiff(table, listOf(row("t1", "A")))

        engine.pause()
        assertEquals(SyncDiff.Paused, engine.computeDiff(table, listOf(row("t1", "B"))))
        assertTrue("duraklamada gönderim olmamalı", sink.pushed.isEmpty())

        engine.resume()
        val diff = engine.computeDiff(table, listOf(row("t1", "B")))
        assertTrue("duraklamadaki değişiklik sonra gönderilmeli", diff is SyncDiff.Changes)
        assertEquals(listOf("t1"), (diff as SyncDiff.Changes).changed.map { it.first })
    }

    @Test
    fun `pause wins over disabled so a bulk push cannot drop records`() {
        val engine = idleEngine(FakeSink())
        engine.computeDiff(table, listOf(row("t1", "A")))

        engine.setEnabled(false)
        engine.pause()

        assertEquals(SyncDiff.Disabled, engine.computeDiff(table, listOf(row("t1", "B"))))
    }

    // ============================================================
    // 4) AKIŞ (pipeline) — flow -> debounce -> sink
    // ============================================================

    @Test
    fun `flow changes reach the sink through the debounced pipeline`() = runTest {
        val rows = MutableStateFlow(listOf(row("t1", "İlk")))
        val sink = FakeSink()
        val engine = AutoSyncEngine(FakeSource(table, rows), sink, StandardTestDispatcher(testScheduler))

        engine.start()
        advanceTimeBy(1_000)
        assertTrue("ilk yayın temel durumdur, gönderilmez", sink.pushed.isEmpty())

        rows.value = listOf(row("t1", "İlk"), row("t2", "İkinci"))
        advanceTimeBy(1_000)

        assertEquals(1, sink.pushed.size)
        assertEquals("t2", sink.pushed.first().second)

        engine.shutdown()
    }

    @Test
    fun `rapid successive edits are debounced into a single push`() = runTest {
        val rows = MutableStateFlow(listOf(row("t1", "A")))
        val sink = FakeSink()
        val engine = AutoSyncEngine(FakeSource(table, rows), sink, StandardTestDispatcher(testScheduler))

        engine.start()
        advanceTimeBy(1_000)

        // Debounce penceresinden kısa aralıklarla 3 düzenleme
        rows.value = listOf(row("t1", "B"))
        advanceTimeBy(50)
        rows.value = listOf(row("t1", "C"))
        advanceTimeBy(50)
        rows.value = listOf(row("t1", "D"))
        advanceTimeBy(1_000)

        assertEquals("ara düzenlemeler tek gönderime indirilmeli", 1, sink.pushed.size)
        assertEquals("D", sink.pushedContent.first()["title"])

        engine.shutdown()
    }
}
