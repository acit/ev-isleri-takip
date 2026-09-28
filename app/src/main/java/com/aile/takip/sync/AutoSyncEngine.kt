package com.aile.takip.sync

import com.google.gson.Gson
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Otomatik senkron motoru.
 *
 * Cihazdaki her tabloyu izler; bir kayıt eklenip/güncellenip silindiğinde
 * **yalnızca değişen satırı** aile grubuna gönderir (diff tabanlı).
 *
 * Tasarım notu: motor Firebase ve Room'a doğrudan bağlı değildir; yalnızca
 * [SyncSource] (okuma) ve [SyncSink] (yazma) arayüzlerini bilir. Karar mantığı
 * [computeDiff] içinde **yan etkisiz** tutulur, böylece ağ olmadan test edilebilir.
 */
@OptIn(FlowPreview::class)
class AutoSyncEngine(
    private val sources: SyncSource,
    private val sink: SyncSink,
    dispatcher: CoroutineContext = Dispatchers.IO
) {
    // Senkron akışlarından kaçan hata uygulamayı çökürmesin; motor çalışmaya devam etsin
    private val supervisor = CoroutineExceptionHandler { _, t ->
        android.util.Log.e("AutoSyncEngine", "Senkron akış hatası yutlandı", t)
    }

    private val scope = CoroutineScope(SupervisorJob() + dispatcher + supervisor)
    private var jobs = mutableListOf<Job>()
    private val gson = Gson()

    /** table -> (recordId -> son gönderilen JSON) */
    private val snapshots = ConcurrentHashMap<String, MutableMap<String, String>>()

    /** Uzaktan veri uygulandığında bir sonraki farkı "temel durum" say. */
    private val skipBaseline = ConcurrentHashMap.newKeySet<String>()

    private val paused = AtomicBoolean(false)

    private val _enabled = MutableStateFlow(true)
    val enabled: StateFlow<Boolean> = _enabled

    private val _pendingCount = MutableStateFlow(0)
    val pendingCount: StateFlow<Int> = _pendingCount

    private val _pushCount = MutableStateFlow(0)
    val pushCount: StateFlow<Int> = _pushCount

    private val _lastPushAt = MutableStateFlow(0L)
    val lastPushAt: StateFlow<Long> = _lastPushAt

    fun setEnabled(value: Boolean) {
        _enabled.value = value
    }

    /** Toplu işlem (ör. tam push) sırasında gönderimi duraklatır. */
    fun pause() {
        paused.set(true)
    }

    fun resume() {
        paused.set(false)
    }

    /** Uzaktan veri uygulandığında çağrılır: gelen veri geri gönderilmez (echo koruması). */
    fun ignoreNextChange(table: String) {
        skipBaseline.add(table)
    }

    /** Tüm tabloları izlemeye başlar. Tekrar çağrılırsa önce durdurur. */
    fun start() {
        stop()
        for ((table, flow) in sources.flows()) {
            jobs.add(watch(table, flow))
        }
    }

    fun stop() {
        jobs.forEach { it.cancel() }
        jobs.clear()
        snapshots.clear()
        skipBaseline.clear()
        _pendingCount.value = 0
    }

    fun shutdown() {
        stop()
        scope.cancel()
    }

    private fun watch(table: String, flow: Flow<List<SyncRow>>): Job = scope.launch {
        flow.conflate().debounce(DEBOUNCE_MS).collect { rows ->
            try {
                when (val diff = computeDiff(table, rows)) {
                    is SyncDiff.Changes -> applyChanges(table, diff)
                    else -> Unit
                }
            } catch (e: Exception) {
                // Tek tablodaki hata diğer tabloların senkronunu engellemesin
                android.util.Log.e("AutoSyncEngine", "Tablo senkronu başarısız: $table", e)
            }
        }
    }

    /**
     * Gelen satırları önceki anlık görüntüyle karşılaştırır ve ne yapılacağına karar verir.
     *
     * Yan etkisi yoktur (gönderim yapmaz), bu yüzden doğrudan test edilebilir.
     * Temel durum kurulduğunda/gönderim kapalıyken anlık görüntü güncellenir;
     * duraklatıldığında güncellenmez ki değişiklik kaybolmasın.
     */
    internal fun computeDiff(table: String, rows: List<SyncRow>): SyncDiff {
        val currentJson = HashMap<String, String>(rows.size * 2)
        val currentContent = HashMap<String, Map<String, Any>>(rows.size * 2)
        for (row in rows) {
            currentJson[row.id] = gson.toJson(row.content)
            currentContent[row.id] = row.content
        }

        val existing = snapshots[table]
        // İlk okuma veya uzaktan veri uygulandı: temel durumu kur, gönderme.
        if (existing == null || skipBaseline.remove(table)) {
            snapshots[table] = currentJson.toMutableMap()
            return SyncDiff.Baseline
        }

        if (!_enabled.value) {
            snapshots[table] = currentJson.toMutableMap()
            return SyncDiff.Disabled
        }

        if (paused.get()) {
            // Anlık görüntü KORUNUR: değişiklikler devam edince gönderilir.
            return SyncDiff.Paused
        }

        val changed = currentJson.filter { (id, json) -> existing[id] != json }
        val removed = existing.keys.filter { it !in currentJson }
        if (changed.isEmpty() && removed.isEmpty()) return SyncDiff.Empty

        return SyncDiff.Changes(
            changed = changed.map { it.key to (currentContent[it.key] ?: emptyMap()) },
            removed = removed
        )
    }

    /** Tespit edilen değişiklikleri gönderir ve anlık görüntüyü ilerletir. */
    internal suspend fun applyChanges(table: String, diff: SyncDiff.Changes) {
        val snapshot = snapshots[table] ?: return
        val total = diff.changed.size + diff.removed.size
        _pendingCount.value += total
        try {
            for ((id, content) in diff.changed) {
                sink.push(table, id, content)
                snapshot[id] = gson.toJson(content)
            }
            for (id in diff.removed) {
                // Mezar taşı bırak: diğer cihazlar da silsin
                sink.deleteWithTombstone(table, id)
                snapshot.remove(id)
            }
            _lastPushAt.value = System.currentTimeMillis()
            _pushCount.value += total
        } catch (e: Exception) {
            // Tek bir tablodaki hata diğer tabloların senkronunu durdurmasın
        } finally {
            _pendingCount.value = maxOf(0, _pendingCount.value - total)
        }
    }

    private companion object {
        const val DEBOUNCE_MS = 400L
    }
}
