package com.aile.takip.sync

/** Çakışmada hangi tarafın kazandığı. */
enum class ConflictWinner {
    /** Yerel kayıt daha yeni: uzak veri yazılmaz, yerel veri geri gönderilir. */
    LOCAL,

    /** Uzak kayıt daha yeni (veya eşitlikte kazanan): yerel kayıt güncellenir. */
    REMOTE,

    /** İki taraf da birebir aynı: yazmaya gerek yok. */
    EQUAL
}

/**
 * Aynı kayıt iki cihazda değiştiğinde kazananı belirler.
 *
 * Kural: **son değiştiren kazanır** (`syncVersion` büyük olan).
 *
 * Aynı `syncVersion`'a sahip ama farklı içerikli kayıtlar (iki cihaz aynı
 * milisaniyede düzenlerse) içerik karşılaştırmasıyla deterministik şekilde
 * çözülür. Böylece **tüm cihazlar aynı sonuca varır**: iki cihaz birbirine
 * zıt karar verip ayrışamaz.
 *
 * İçerik karşılaştırması yalnızca sürümler eşitse gerekir; bu yüzden
 * serileştirme tembel yapılır ([localContent] / [remoteContent] lambda'ları
 * sadece gerekince çağrılır).
 */
object ConflictResolver {

    fun resolveUpdate(
        localVersion: Long,
        remoteVersion: Long,
        localContent: () -> String,
        remoteContent: () -> String
    ): ConflictWinner = when {
        localVersion > remoteVersion -> ConflictWinner.LOCAL
        localVersion < remoteVersion -> ConflictWinner.REMOTE
        else -> {
            val local = localContent()
            val remote = remoteContent()
            when {
                local == remote -> ConflictWinner.EQUAL
                // Deterministik eşitlik bozucu: her iki cihaz da aynı kararı verir
                local > remote -> ConflictWinner.LOCAL
                else -> ConflictWinner.REMOTE
            }
        }
    }

    /**
     * Silme ile düzenleme çakışması.
     *
     * Bir kayıt silindikten **sonra** düzenlenmişse (`syncVersion` > `deletedAt`)
     * düzenleme kazanır ve kayıt geri gelir; aksi halde silme uygulanır.
     */
    fun resolveDeletion(localVersion: Long, deletedAt: Long): ConflictWinner =
        if (localVersion > deletedAt) ConflictWinner.LOCAL else ConflictWinner.REMOTE
}
