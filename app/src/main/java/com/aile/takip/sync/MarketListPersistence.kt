package com.aile.takip.sync

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.aile.takip.utils.MarketListStore as MarketListStoreData
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.marketDataStore by preferencesDataStore(name = "aile_market_lists")

/**
 * Haftalık market listelerinin kalıcı saklanması (DataStore, JSON).
 *
 * Son [MAX_WEEKS] hafta tutulur; eskiler otomatik silinir.
 * Anahtar: "week:2026-W39" -> JSON
 */
class MarketListPersistence(private val context: Context) {

    private val gson = Gson()

    private object Keys {
        val index = stringPreferencesKey("weeks_index") // "2026-W39,2026-W38,..."
        fun week(key: String) = stringPreferencesKey("week:$key")
    }

    /** Tüm saklanan haftaların anahtarları (yeni → eski). */
    suspend fun weekKeys(): List<String> =
        context.marketDataStore.data.map { p ->
            p[Keys.index]?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
        }.first()

    /** Bir haftanın listesi (yoksa null). */
    suspend fun get(weekKey: String): MarketListStoreData.WeeklyList? {
        val json = context.marketDataStore.data.map { it[Keys.week(weekKey)] }.first() ?: return null
        return try {
            gson.fromJson(json, MarketListStoreWeeklyList::class.java)?.toWeeklyList()
        } catch (_: Exception) {
            null
        }
    }

    /** Listeyi kaydeder; endeksi güncel tutar; MAX_WEEKS aşılırsa eskileri siler. */
    suspend fun save(list: MarketListStoreData.WeeklyList) {
        context.marketDataStore.edit { p ->
            p[Keys.week(list.weekKey)] = gson.toJson(MarketListStoreWeeklyList.from(list))
            val keys = (p[Keys.index]?.split(",")?.filter { it.isNotBlank() } ?: emptyList())
                .filter { it != list.weekKey }
                .toMutableList()
            keys.add(0, list.weekKey)
            // Eskileri sil
            keys.drop(MAX_WEEKS).forEach { stale -> p.remove(Keys.week(stale)) }
            p[Keys.index] = keys.take(MAX_WEEKS).joinToString(",")
        }
    }

    /** Belirli haftayı siler (UI'dan gerekirse). */
    suspend fun delete(weekKey: String) {
        context.marketDataStore.edit { p ->
            p.remove(Keys.week(weekKey))
            val keys = (p[Keys.index]?.split(",")?.filter { it.isNotBlank() } ?: emptyList())
                .filter { it != weekKey }
            p[Keys.index] = keys.joinToString(",")
        }
    }

    companion object {
        const val MAX_WEEKS = 8
    }
}

/** Gson için açık tipli liste modeli (type-safe, generic karmaşası yok). */
data class MarketListStoreWeeklyList(
    val weekKey: String,
    val createdAt: Long,
    val items: List<MarketListItemJson>
) {
    fun toWeeklyList(): MarketListStoreData.WeeklyList =
        MarketListStoreData.WeeklyList(
            weekKey = weekKey,
            createdAt = createdAt,
            items = items.map { MarketListStoreData.Item(it.name, it.quantity, it.category, it.dishes) }
        )

    companion object {
        fun from(list: MarketListStoreData.WeeklyList) = MarketListStoreWeeklyList(
            weekKey = list.weekKey,
            createdAt = list.createdAt,
            items = list.items.map { MarketListItemJson(it.name, it.quantity, it.category, it.dishes) }
        )
    }
}

data class MarketListItemJson(
    val name: String = "",
    val quantity: Int = 0,
    val category: String = "Yemek İçin",
    val dishes: List<String> = emptyList()
)
