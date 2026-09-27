import Foundation

/// Çakışmada hangi tarafın kazandığı (Android `ConflictWinner` paritesi).
enum ConflictWinner {
    /// Yerel kayıt daha yeni: uzak veri yazılmaz, yerel veri geri gönderilir.
    case local
    /// Uzak kayıt daha yeni (veya eşitlikte kazanan): yerel kayıt güncellenir.
    case remote
    /// İki taraf da birebir aynı: yazmaya gerek yok.
    case equal
}

/// Aynı kayıt iki cihazda değiştiğinde kazananı belirler.
///
/// Kural: **son değiştiren kazanır** (`syncVersion` büyük olan).
/// Sürümler eşitse içerik karşılaştırmasıyla deterministik karar verilir;
/// böylece tüm cihazlar aynı sonuca varır ve ayrışma oluşmaz.
enum ConflictResolver {

    /// İçerik karşılaştırmasını yalnızca sürümler eşitse yapar (tembel).
    static func resolveUpdate(
        localVersion: Int64,
        remoteVersion: Int64,
        localContent: () -> String,
        remoteContent: () -> String
    ) -> ConflictWinner {
        if localVersion > remoteVersion { return .local }
        if localVersion < remoteVersion { return .remote }
        let local = localContent()
        let remote = remoteContent()
        if local == remote { return .equal }
        // Deterministik eşitlik bozucu: her iki cihaz da aynı kararı verir
        return local > remote ? .local : .remote
    }

    /// Silme ile düzenleme çakışması: silindikten sonra düzenlenmişse
    /// (`syncVersion` > `deletedAt`) düzenleme kazanır ve kayıt geri gelir.
    static func resolveDeletion(localVersion: Int64, deletedAt: Int64) -> ConflictWinner {
        localVersion > deletedAt ? .local : .remote
    }
}

/// Senkronize edilen tablolar (Android `DatabaseSyncSource.TABLE_NAMES` paritesi).
enum SyncTables {
    static let all = [
        "members", "tasks", "shopping", "messages", "invoices", "budgets",
        "expenses", "inventory", "meal_plans", "sports_clubs", "workout_logs",
        "calorie_logs", "menstrual_cycles", "notes", "reminders",
        "water_logs", "sleep_logs"
    ]
}
