package com.aile.takip.utils

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Haftalık market listesi modeli ve haftalar arası karşılaştırma.
 *
 * Listeler "ISO hafta anahtarı" (örn. "2026-W39") ile saklanır; karşılaştırma
 * tamamen yan etkisiz (pure) olduğu için birim test edilebilir.
 */
object MarketListStore {

    /** Bir haftanın market listesi anlık görüntüsü. */
    data class WeeklyList(
        val weekKey: String,            // "2026-W39"
        val createdAt: Long,
        val items: List<Item>           // toplu eksikler (birleştirilmiş)
    )

    data class Item(
        val name: String,
        val quantity: Int,              // eksik adet
        val category: String = "Yemek İçin",
        val dishes: List<String> = emptyList() // hangi yemekler için gerekti
    )

    /** Haftalar arası fark özeti. */
    data class WeekDiff(
        val currentWeek: String,
        val previousWeek: String?,
        val carriedOver: List<Item>,    // geçen hafta da vardı (tekrarlanan ihtiyaç)
        val newItems: List<Item>,       // bu hafta yeni ihtiyaç
        val droppedItems: List<Item>    // geçen hafta vardı, bu hafta gerekmiyor
    ) {
        /** Bu haftanın tam listesi. */
        val currentItems: List<Item> get() = carriedOver + newItems

        /** Tekrar oranı: carried / current (0..100). */
        val repeatRate: Int
            get() {
                val cur = currentItems.size
                return if (cur == 0) 0 else (carriedOver.size * 100) / cur
            }
    }

    /**
     * [exhausted] malzeme listesinden (ad + eksik adet + yemekler) bir haftalık
     * liste üretir. Aynı isimli malzemeler birleştirilir, adetler toplanır.
     */
    fun buildList(weekKey: String, now: Long, exhausted: List<Triple<String, Int, List<String>>>): WeeklyList {
        val merged = LinkedHashMap<String, Item>()
        for ((name, qty, dishes) in exhausted) {
            if (qty <= 0) continue
            val existing = merged[name]
            if (existing == null) {
                merged[name] = Item(name, qty, dishes = dishes)
            } else {
                merged[name] = existing.copy(
                    quantity = existing.quantity + qty,
                    dishes = (existing.dishes + dishes).distinct()
                )
            }
        }
        return WeeklyList(
            weekKey = weekKey,
            createdAt = now,
            items = merged.values.sortedWith(compareByDescending<Item> { it.quantity }.thenBy { it.name })
        )
    }

    /**
     * Bu hafta ile önceki haftayı karşılaştırır.
     * Eşleştirme normalize ad üzerinden yapılır ("Domates" vs "domates" aynı).
     */
    fun compare(current: WeeklyList, previous: WeeklyList?): WeekDiff {
        if (previous == null) {
            return WeekDiff(
                currentWeek = current.weekKey,
                previousWeek = null,
                carriedOver = emptyList(),
                newItems = current.items,
                droppedItems = emptyList()
            )
        }
        val prevByName = previous.items.associateBy { normalizeName(it.name) }
        val curByName = current.items.associateBy { normalizeName(it.name) }

        val carried = current.items.filter { normalizeName(it.name) in prevByName }
        val newItems = current.items.filter { normalizeName(it.name) !in prevByName }
        val dropped = previous.items.filter { normalizeName(it.name) !in curByName }

        return WeekDiff(
            currentWeek = current.weekKey,
            previousWeek = previous.weekKey,
            carriedOver = carried,
            newItems = newItems,
            droppedItems = dropped
        )
    }

    /** "Domates (2 kg)" ile "domates" aynı sayılır. */
    fun normalizeName(name: String): String = name
        .lowercase()
        .replace("\u0307", "")                    // Türkçe İ artığı
        .replace('ç', 'c').replace('ğ', 'g').replace('ı', 'i')
        .replace('ö', 'o').replace('ş', 's').replace('ü', 'u')
        .replace(Regex("""[^a-z0-9 ]"""), " ")
        .replace(Regex("""\s+"""), " ").trim()

    /** Bu haftanın ISO hafta anahtarı: "2026-W39". */
    fun currentWeekKey(now: Long = System.currentTimeMillis()): String {
        val cal = Calendar.getInstance()
        cal.timeInMillis = now
        val year = cal.get(Calendar.YEAR)
        val week = cal.get(Calendar.WEEK_OF_YEAR)
        return "%d-W%02d".format(Locale.US, year, week)
    }

    /** Önceki haftanın anahtarı (yıl sınırını doğru işler). */
    fun previousWeekKey(now: Long = System.currentTimeMillis()): String {
        val cal = Calendar.getInstance()
        cal.timeInMillis = now
        cal.add(Calendar.WEEK_OF_YEAR, -1)
        val year = cal.get(Calendar.YEAR)
        val week = cal.get(Calendar.WEEK_OF_YEAR)
        return "%d-W%02d".format(Locale.US, year, week)
    }

    /** İnsan-okur tarih etiketi ("22-28 Eyl" gibi) — UI özetinde kullanılır. */
    fun weekLabel(weekKey: String, now: Long = System.currentTimeMillis()): String = try {
        val sdf = SimpleDateFormat("d MMM", Locale("tr"))
        val cal = Calendar.getInstance()
        cal.timeInMillis = now
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        // Haftanın Pazartesi'sine git
        var back = dayOfWeek - Calendar.MONDAY
        if (back < 0) back += 7
        cal.add(Calendar.DAY_OF_YEAR, -back)
        sdf.format(cal.time)
    } catch (_: Exception) {
        weekKey
    }
}
