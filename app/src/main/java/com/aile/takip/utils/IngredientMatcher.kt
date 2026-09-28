package com.aile.takip.utils

/**
 * Türkçe yemek adlarından malzeme çıkarımı ve envanterle gerçek eşleştirme.
 *
 * Strateji (basit ve şeffaf):
 *  1. [RECIPE_INGREDIENTS] sözlüğü: bilinen yemek adı -> (malzeme, adet)
 *  2. Eşleşmeyen yemeklerde [KEYWORD_INGREDIENTS] anahtar kelime taraması
 *  3. Envanter adıyla eşleştirme: normalizasyon + karşılıklı kelime içerme
 *
 * Amaç tam doğruluk değil; "muhtemelen eksik" malzemeleri önermek ve
 * kullanıcıya tek dokunuşla alışveriş listesine ekleme kolaylığı sağlamaktır.
 */
object IngredientMatcher {

    /** Bir yemeğin gereken malzemeleri: malzeme adı -> gereken adet. */
    data class Requirement(val name: String, val needed: Int)

    /** Bir malzemenin envanter durumu. */
    data class StockStatus(
        val requirement: Requirement,
        val inStock: Int,          // envanterde bulunan (0 = hiç yok)
        val inventoryName: String? // eşleşen envanter kaydı (null = hiç yok)
    ) {
        val missing: Int get() = maxOf(0, requirement.needed - inStock)
        val isMissing: Boolean get() = missing > 0
    }

    /**
     * Bilinen yemek tarifleri. Değerler tipik 4 kişilik porsiyon içindir.
     * Kapsamı kademeli büyütmek kolay: yalnızca bu sözlüğe ekleme yeterli.
     */
    private val RECIPE_INGREDIENTS: Map<String, List<Requirement>> = mapOf(
        "makarna" to reqs("Makarna" to 1, "Zeytinyağı" to 1, "Tuz" to 1, "Domates Salçası" to 1),
        "spagetti" to reqs("Spagetti" to 1, "Zeytinyağı" to 1, "Tuz" to 1, "Domates Salçası" to 1),
        "pilav" to reqs("Pirinç" to 1, "Tereyağı" to 1, "Tuz" to 1),
        "bulgur pilavı" to reqs("Bulgur" to 1, "Tereyağı" to 1, "Domates Salçası" to 1, "Tuz" to 1),
        "salata" to reqs("Marul" to 1, "Domates" to 2, "Salatalık" to 2, "Zeytinyağı" to 1, "Limon" to 1),
        "çoban salata" to reqs("Domates" to 2, "Salatalık" to 2, "Biber" to 1, "Soğan" to 1, "Peynir" to 1),
        "çorba" to reqs("Soğan" to 1, "Havuç" to 2, "Tuz" to 1),
        "mercimek çorbası" to reqs("Mercimek" to 2, "Soğan" to 1, "Havuç" to 2, "Tuz" to 1),
        "karnıyarık" to reqs("Patlıcan" to 4, "Kıyma" to 1, "Soğan" to 1, "Domates" to 2, "Pirinç" to 1),
        "izmir köfte" to reqs("Kıyma" to 1, "Patates" to 3, "Soğan" to 1, "Domates" to 2),
        "köfte" to reqs("Kıyma" to 1, "Soğan" to 1, "Ekmek" to 1, "Tuz" to 1),
        "tavuk" to reqs("Tavuk" to 1, "Tuz" to 1, "Baharat" to 1),
        "tavuk sote" to reqs("Tavuk" to 1, "Biber" to 2, "Soğan" to 1, "Domates Salçası" to 1),
        "balık" to reqs("Balık" to 1, "Limon" to 1, "Tuz" to 1),
        "omlet" to reqs("Yumurta" to 3, "Peynir" to 1, "Maydanoz" to 1),
        "menemen" to reqs("Yumurta" to 3, "Domates" to 2, "Biber" to 1, "Tereyağı" to 1),
        "pankek" to reqs("Un" to 1, "Yumurta" to 2, "Süt" to 1, "Şeker" to 1),
        "börek" to reqs("Yufka" to 3, "Peynir" to 2, "Yumurta" to 2, "Süt" to 1),
        "lahmacun" to reqs("Kıyma" to 1, "Soğan" to 1, "Domates" to 2, "Maydanoz" to 1),
        "pide" to reqs("Kıyma" to 1, "Peynir" to 1, "Domates" to 1, "Maydanoz" to 1),
        "dolma" to reqs("Biber" to 6, "Pirinç" to 1, "Soğan" to 1, "Domates Salçası" to 1),
        "sarma" to reqs("Asma Yaprağı" to 1, "Pirinç" to 1, "Soğan" to 1, "Zeytinyağı" to 1),
        "güveç" to reqs("Patates" to 3, "Patlıcan" to 2, "Tavuk" to 1, "Domates" to 2, "Biber" to 2),
        "mantı" to reqs("Un" to 2, "Kıyma" to 1, "Yoğurt" to 1, "Soğan" to 1),
        "kısır" to reqs("Bulgur" to 2, "Domates Salçası" to 1, "Maydanoz" to 1, "Soğan" to 1, "Limon" to 1),
        "humus" to reqs("Nohut" to 2, "Tahin" to 1, "Limon" to 1, "Sarımsak" to 1)
    )

    /** Sözlükte olmayan yemekler için anahtar kelime -> muhtemel malzeme. */
    private val KEYWORD_INGREDIENTS: List<Pair<String, Requirement>> = listOf(
        "tavuk" to Requirement("Tavuk", 1),
        "kıyma" to Requirement("Kıyma", 1),
        "köfte" to Requirement("Kıyma", 1),
        "balık" to Requirement("Balık", 1),
        "pilav" to Requirement("Pirinç", 1),
        "makarna" to Requirement("Makarna", 1),
        "çorba" to Requirement("Soğan", 1),
        "salata" to Requirement("Marul", 1),
        "börek" to Requirement("Yufka", 3),
        "menemen" to Requirement("Yumurta", 3),
        "omlet" to Requirement("Yumurta", 3),
        "patates" to Requirement("Patates", 3),
        "patlıcan" to Requirement("Patlıcan", 3),
        "nohut" to Requirement("Nohut", 2),
        "fasulye" to Requirement("Fasulye", 2),
        "yoğurt" to Requirement("Yoğurt", 1),
        "ıspanak" to Requirement("Ispanak", 1),
        "kırmızı" to Requirement("Domates Salçası", 1) // kırmızı köfte/güveç vb.
    )

    /** Her tarifte bulunan temel malzemeler — envanterde varsa yeterli sayılır. */
    private val PANTRY_WORDS = listOf("tuz", "baharat", "su", "yağ", "şeker", "un")

    private fun reqs(vararg pairs: Pair<String, Int>): List<Requirement> =
        pairs.map { Requirement(it.first, it.second) }

    /**
     * Yemek adından gereken malzemeleri çıkarır.
     * Önce tarif sözlüğü (uzun ad önce eşleşsin diye uzunluğa göre sıralı), sonra anahtar tarama.
     */
    fun extractIngredients(dish: String): List<Requirement> {
        val d = normalize(dish)
        if (d.isBlank()) return emptyList()

        // 1) Tarif sözlüğü — en uzun eşleşen tarif kazanır ("izmir köfte" > "köfte")
        val recipe = RECIPE_INGREDIENTS.entries
            .filter { d.contains(normalize(it.key)) }
            .maxByOrNull { it.key.length }
        if (recipe != null) return recipe.value

        // 2) Anahtar kelime taraması
        val found = KEYWORD_INGREDIENTS
            .filter { (kw, _) -> d.contains(normalize(kw)) }
            .map { it.second }
        if (found.isNotEmpty()) return found.distinctBy { it.name }

        // 3) Bilinmiyorsa: temel malzeme önerisi (kullanıcı düzenleyebilir)
        return listOf(Requirement("Malzeme", 1))
    }

    /**
     * Gerekli malzemeleri envanterle gerçekten eşleştirir.
     *
     * Eşleştirme kuralı: normalize edilmiş adlarda **karşılıklı kelime kapsaması**
     * (örn. gerekli "Domates Salçası" ↔ envanter "Salça Domates" eşleşir;
     * "Domates" ↔ "Domates Salçası" da eşleşir — kapsama yeterlidir).
     * Her tarifte olan tuz/baharat gibi kiler ürünleri yalnızca envanterde
     * hiç yoksa önerilir.
     */
    fun matchWithInventory(
        requirements: List<Requirement>,
        inventory: List<Pair<String, Int>> // (ad, miktar)
    ): List<StockStatus> {
        return requirements.map { req ->
            val reqNorm = normalize(req.name)
            val reqWords = reqNorm.split(" ").filter { it.isNotBlank() }

            val match = inventory.mapNotNull { (name, qty) ->
                val invNorm = normalize(name)
                if (invNorm.isBlank()) return@mapNotNull null
                val invWords = invNorm.split(" ").filter { it.isNotBlank() }

                val contains = reqWords.any { rw -> invWords.any { it.contains(rw) || rw.contains(it) } }
                if (contains) name to qty else null
            }.maxByOrNull { it.second } // en çok stoklu eşleşme

            val inStock = match?.second ?: 0
            // Kiler ürünleri (tuz/baharat...) envanterde hiç yoksa bile "eksik" olarak
            // gürültü yapmasın: yalnızca adet gereksinimi 2+ ise veya envanterde eşleşme
            // yoksa ve kiler kelimesi değilse raporla.
            val isPantry = PANTRY_WORDS.any { reqNorm.contains(it) }
            val effectiveStock = if (isPantry && match == null) req.needed else inStock

            StockStatus(req, effectiveStock, match?.first)
        }.sortedWith(compareByDescending<StockStatus> { it.isMissing }.thenByDescending { it.missing })
    }

    /** Tek yemek için eksikleri döner. */
    fun missingFor(dish: String, inventory: List<Pair<String, Int>>): List<StockStatus> =
        matchWithInventory(extractIngredients(dish), inventory).filter { it.isMissing }

    /**
     * Türkçe karakterleri sadeleştirir ve küçük harfe çevirir.
     *
     * Dikkat: Türkçe 'İ' (U+0130) lowercase'de 'i' + birleştirici nokta (U+0307)
     * üretir; nokta temizlenmezse "İzmir" != "izmir" olur ve eşleşme kaçar.
     */
    private fun normalize(s: String): String = s.lowercase()
        .replace("\u0307", "")                       // combining dot above (İ'nin artığı) SİL — boşluk yapma, kelimeyi bölmesin
        .replace('ç', 'c').replace('ğ', 'g').replace('ı', 'i').replace('ö', 'o')
        .replace('ş', 's').replace('ü', 'u').replace('î', 'i').replace('â', 'a')
        .replace(Regex("""[^a-z0-9 ]"""), " ")      // kalan noktalama/semboller
        .replace(Regex("""\s+"""), " ").trim()
}
