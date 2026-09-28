package com.aile.takip.sync

import kotlinx.coroutines.flow.Flow

/** Senkronize edilebilir tek bir kayıt: kimlik + gönderilebilir içerik. */
data class SyncRow(val id: String, val content: Map<String, Any>)

/**
 * Motorun **yazma** tarafı.
 *
 * Somut Firebase bağımlılığı yerine arayüz: motor bu sayede ağ olmadan test edilebilir.
 */
interface SyncSink {
    suspend fun push(table: String, id: String, data: Map<String, Any>)
    suspend fun deleteWithTombstone(table: String, id: String)
}

/** İzlenecek tabloları sağlar: tablo adı -> o tablonun satır akışı. */
interface SyncSource {
    fun flows(): Map<String, Flow<List<SyncRow>>>
}

/**
 * Bir tablonun önceki durumla karşılaştırılmasının sonucu.
 *
 * [Baseline], [Disabled] ve [Paused] durumlarında **hiçbir şey gönderilmez**;
 * farkları şudur: [Baseline] anlık görüntüyü günceller, [Paused] güncellemez
 * (duraklama sırasındaki değişiklikler kaybolmaz, sonra gönderilir).
 */
sealed interface SyncDiff {
    /** İlk okuma: temel durum kuruldu, gönderilmez. */
    data object Baseline : SyncDiff

    /** Otomatik senkron kapalı: sadece temellendir. */
    data object Disabled : SyncDiff

    /** Duraklatıldı: anlık görüntü korunur, değişiklikler sonra gönderilir. */
    data object Paused : SyncDiff

    /** Hiçbir şey değişmedi. */
    data object Empty : SyncDiff

    /** Gönderilecek değişiklikler ve silinecek kimlikler. */
    data class Changes(
        val changed: List<Pair<String, Map<String, Any>>>,
        val removed: List<String>
    ) : SyncDiff
}
