import Foundation
import SwiftData

/// iOS Firebase senkronizasyon servisi — Android `FirebaseSyncService` paritesi.
///
/// Sorumluluklar:
/// - Aile hesabı (e-posta/şifre) oturumu ve kalıcılığı
/// - Aile grubu oluşturma / katılma isteği / onay-ret / üye çıkarma
/// - 17 tabloyu Firebase Realtime Database ile iki yönlü senkronize etme
/// - Çakışmada **son değiştiren kazanır** (`syncVersion`) + deterministik eşitlik bozucu
/// - Silme yayılımı (mezar taşları, `meta/tombstones`)
@MainActor
final class FirebaseSyncService: ObservableObject {

    // MARK: - Durum tipleri (Android paritesi)

    enum SyncState: Equatable {
        case disconnected
        case connecting
        case connected
        case syncing(table: String)
        case error(String)
    }

    enum AuthResult {
        case success(uid: String)
        case error(String)
    }

    enum Membership: Equatable {
        case signedOut
        case notMember(uid: String)
        case pending(uid: String)
        case rejected(uid: String)
        case member(uid: String)
        case error(String)

        static func == (lhs: Membership, rhs: Membership) -> Bool {
            switch (lhs, rhs) {
            case (.signedOut, .signedOut): return true
            case (.member(let a), .member(let b)): return a == b
            case (.pending(let a), .pending(let b)): return a == b
            case (.rejected(let a), .rejected(let b)): return a == b
            case (.notMember(let a), .notMember(let b)): return a == b
            case (.error(let a), .error(let b)): return a == b
            default: return false
            }
        }

        var isMember: Bool {
            if case .member = self { return true }
            return false
        }
    }

    struct JoinRequest: Identifiable {
        let uid: String
        var name: String = ""
        var passcodeProof: String = ""
        var requestedAt: Int64 = 0
        var id: String { uid }
    }

    // MARK: - Yayınlanan durum

    @Published private(set) var syncState: SyncState = .disconnected
    @Published private(set) var membership: Membership = .signedOut
    @Published private(set) var uid: String?
    @Published private(set) var pendingJoins: [JoinRequest] = []
    @Published private(set) var lastSyncTime: Date?
    @Published private(set) var syncedTables: Set<String> = []
    @Published var conflictsSolved: Int = 0
    /// Grubun aile şifresi özeti (katılım isteği ekranında eşleşme bilgisi için).
    @Published private(set) var groupPasscodeHash: String?

    /// Uzak veri uygulanırken çağrılır (UI'da sayaç/kullanıcı adı tazelenir).
    var onRemoteDataApplied: (() -> Void)?

    private let client: FirebaseSyncClient
    private let modelContext: ModelContext
    private var groupId: String?

    /// Yerel değişikliklerin uzaktan gelen veriyi ezmesini önler (echo koruması).
    private var suppressedTables: Set<String> = []

    init(modelContext: ModelContext, client: FirebaseSyncClient = FirebaseSyncClient()) {
        self.modelContext = modelContext
        self.client = client
        self.uid = client.uid
        if client.isSignedIn {
            membership = .notMember(uid ?? "")
        }
    }

    var isConfigured: Bool {
        !FirebaseSyncClient.apiKey.isEmpty && !FirebaseSyncClient.databaseURL.isEmpty
    }

    var isSignedIn: Bool { client.isSignedIn }

    /// Senkronize edilecek aile grubunu belirler (Android `setGroup` paritesi).
    func setGroup(_ id: String) {
        groupId = id
    }

    // MARK: - Kimlik doğrulama

    func signUp(email: String, password: String) async -> AuthResult {
        do {
            let result = try await client.signUp(email: email, password: password)
            uid = result.uid
            membership = .notMember(result.uid)
            return .success(uid: result.uid)
        } catch {
            return .error((error as? LocalizedError)?.errorDescription ?? error.localizedDescription)
        }
    }

    func signIn(email: String, password: String) async -> AuthResult {
        do {
            let result = try await client.signIn(email: email, password: password)
            uid = result.uid
            membership = .notMember(result.uid)
            return .success(uid: result.uid)
        } catch {
            return .error((error as? LocalizedError)?.errorDescription ?? error.localizedDescription)
        }
    }

    /// Aile hesabından çıkar; yerel veriler korunur.
    func signOut() {
        client.signOut()
        uid = nil
        membership = .signedOut
        pendingJoins = []
        syncState = .disconnected
    }

    // MARK: - Grup oluşturma / katılma

    /// Yeni aile grubu kurar; kuran kişi ilk üye olur.
    func createFamily(newGroupId: String, passcode: String, memberName: String) async -> Membership {
        guard let uid = uid else { return fail(.signedOut) }
        guard !newGroupId.isBlank else { return fail(.error("Grup kimliği oluşturulamadı")) }
        guard passcode.count >= 4 else { return fail(.error("Aile şifresi en az 4 karakter olmalı")) }
        guard isConfigured else { return fail(.error("Firebase ayarlanmadı")) }

        groupId = newGroupId
        syncState = .connecting
        let now = Int64(Date().timeIntervalSince1970 * 1000)
        let hash = FirebaseSyncClient.sha256Hex(passcode)
        do {
            let root = "aile_grubu/\(newGroupId)"
            try await client.put(path: "\(root)/meta/ownerUid", value: uid)
            try await client.put(path: "\(root)/meta/passcodeHash", value: hash)
            try await client.put(path: "\(root)/meta/createdAt", value: now)
            try await client.put(path: "\(root)/meta/members/\(uid)", value: [
                "name": memberName, "role": "Kurucu", "joinedAt": now, "approvedBy": uid
            ])
            return await refreshMembership()
        } catch {
            return fail(.error(error.localizedDescription))
        }
    }

    /// Mevcut gruba katılım isteği gönderir; onay bir aile bireyinden gelir.
    func requestJoin(passcode: String, memberName: String) async -> Membership {
        guard let uid = uid else { return fail(.signedOut) }
        guard let gid = groupId, !gid.isBlank else { return fail(.error("Grup ID gerekli")) }
        guard passcode.count >= 4 else { return fail(.error("Aile şifresi en az 4 karakter olmalı")) }
        guard isConfigured else { return fail(.error("Firebase ayarlanmadı")) }

        let now = Int64(Date().timeIntervalSince1970 * 1000)
        do {
            try await client.put(path: "aile_grubu/\(gid)/joins/\(uid)", value: [
                "uid": uid, "name": memberName,
                "passcodeProof": FirebaseSyncClient.sha256Hex(passcode),
                "status": "pending", "requestedAt": now
            ])
            membership = .pending(uid)
            return membership
        } catch {
            return fail(.error(error.localizedDescription))
        }
    }

    /// Üyeliği sunucudan yeniden kontrol eder; üye ise ilk indirmeyi başlatır.
    @discardableResult
    func refreshMembership() async -> Membership {
        guard let uid = uid else { return fail(.signedOut) }
        guard let gid = groupId, !gid.isBlank else { return fail(.signedOut) }
        guard isConfigured else { return fail(.error("Firebase ayarlanmadı")) }
        do {
            let memberSnap = try await client.get(path: "aile_grubu/\(gid)/meta/members/\(uid)")
            if memberSnap != nil && (memberSnap as? [String: Any]) != nil {
                return await becomeMember()
            }
            let join = try await client.get(path: "aile_grubu/\(gid)/joins/\(uid)")
            let status = (join as? [String: Any])?["status"] as? String
            switch status {
            case "approved":
                return await becomeMember()
            case "rejected":
                membership = .rejected(uid)
                return membership
            case "pending":
                membership = .pending(uid)
                return membership
            default:
                membership = .notMember(uid)
                return membership
            }
        } catch {
            return fail(.error(error.localizedDescription))
        }
    }

    /// Katılım isteğini onaylar (yalnızca mevcut üye yapabilir).
    func approveJoin(_ uid: String, name: String, role: String = "Üye") async -> Bool {
        guard let gid = groupId, let me = self.uid else { return false }
        let now = Int64(Date().timeIntervalSince1970 * 1000)
        do {
            try await client.put(path: "aile_grubu/\(gid)/meta/members/\(uid)", value: [
                "name": name.isBlank ? "Üye" : name, "role": role,
                "joinedAt": now, "approvedBy": me
            ])
            try await client.put(path: "aile_grubu/\(gid)/joins/\(uid)/status", value: "approved")
            return true
        } catch {
            return false
        }
    }

    /// Katılım isteğini reddeder.
    func rejectJoin(_ candidateUid: String) async -> Bool {
        guard let gid = groupId else { return false }
        do {
            try await client.put(path: "aile_grubu/\(gid)/joins/\(candidateUid)/status", value: "rejected")
            return true
        } catch {
            return false
        }
    }

    /// Üyeyi gruptan çıkarır (erişimi anında keser).
    func removeMember(_ memberUid: String) async -> Bool {
        guard let gid = groupId else { return false }
        do {
            try await client.delete(path: "aile_grubu/\(gid)/meta/members/\(memberUid)")
            return true
        } catch {
            return false
        }
    }

    /// Bekleyen katılım isteklerini sunucudan çeker.
    func loadPendingJoins() async {
        guard let gid = groupId, membership.isMember else { return }
        do {
            guard let joins = try await client.get(path: "aile_grubu/\(gid)/joins") as? [String: Any] else {
                pendingJoins = []
                return
            }
            var requests: [JoinRequest] = []
            for (candidateUid, raw) in joins {
                guard let dict = raw as? [String: Any] else { continue }
                guard (dict["status"] as? String ?? "pending") == "pending" else { continue }
                requests.append(JoinRequest(
                    uid: candidateUid,
                    name: dict["name"] as? String ?? "",
                    passcodeProof: dict["passcodeProof"] as? String ?? "",
                    requestedAt: dict["requestedAt"] as? Int64 ?? (dict["requestedAt"] as? NSNumber)?.int64Value ?? 0
                ))
            }
            pendingJoins = requests.sorted { $0.requestedAt < $1.requestedAt }
        } catch {
            pendingJoins = []
        }
    }

    /// Grubun şifre özetiyle isteğin doğru şifreyle geldiğini doğrular.
    func passcodeProofMatches(_ proof: String, expectedHash: String) -> Bool {
        !expectedHash.isEmpty && expectedHash == proof
    }

    private func fail(_ state: Membership) -> Membership {
        membership = state
        if case .error(let message) = state { syncState = .error(message) }
        return state
    }

    private func becomeMember() async -> Membership {
        guard let uid = uid else { return .signedOut }
        membership = .member(uid)
        syncState = .connected
        if let gid = groupId {
            groupPasscodeHash = try? await client.get(path: "aile_grubu/\(gid)/meta/passcodeHash") as? String
        }
        await loadPendingJoins()
        await initialDownload()
        return membership
    }

    // MARK: - Tablo senkronizasyonu (ilk indirme)

    /// Üye olunduğunda uzaktaki tüm tabloları çeker ve çakışma kurallarıyla uygular.
    func initialDownload() async {
        guard membership.isMember, let gid = groupId else { return }
        syncState = .syncing(table: "all")
        for table in SyncTables.all {
            await syncTableInternal(table)
        }
        lastSyncTime = Date()
        syncState = .connected
        onRemoteDataApplied?()
    }

    // MARK: - Push / delete (SyncSink paritesi)

    func push(table: String, id: String, data: [String: Any]) async {
        guard let gid = groupId else { return }
        try? await client.put(path: "aile_grubu/\(gid)/\(table)/\(id)", value: data)
    }

    func deleteWithTombstone(table: String, id: String) async {
        guard let gid = groupId, let uid = self.uid else { return }
        let now = Int64(Date().timeIntervalSince1970 * 1000)
        try? await client.put(path: "aile_grubu/\(gid)/meta/tombstones/\(table)/\(id)", value: [
            "deletedAt": now, "by": uid
        ])
        try? await client.delete(path: "aile_grubu/\(gid)/\(table)/\(id)")
    }

    /// Yerel verinin tamamını gruba gönderir (ilk kurulum veya manuel "Şimdi Senkronize Et").
    func pushAll() async {
        guard membership.isMember, let gid = groupId else { return }
        syncState = .syncing(table: "all")
        let coordinator = SyncDataCoordinator(modelContext: modelContext)
        for table in SyncTables.all {
            for row in coordinator.snapshot(table: table) {
                await push(table: table, id: row.id, data: row.content)
            }
        }
        try? await client.put(path: "aile_grubu/\(gid)/last_sync", value: Int64(Date().timeIntervalSince1970 * 1000))
        lastSyncTime = Date()
        syncState = .connected
        onRemoteDataApplied?()
    }

    /// Uzak tabloyu indirip çakışma kurallarıyla uygular; dönen değer işlenen kayıt sayısı.
    @discardableResult
    func syncTable(_ table: String) async -> Int {
        await syncTableInternal(table)
    }

    private func syncTableInternal(_ table: String) async -> Int {
        guard membership.isMember, let gid = groupId else { return 0 }
        syncState = .syncing(table: table)
        defer { if case .syncing = syncState { syncState = .connected } }

        guard let remote = try? await client.get(path: "aile_grubu/\(gid)/\(table)") as? [String: Any] else {
            return 0
        }
        var applied = 0
        let coordinator = SyncDataCoordinator(modelContext: modelContext)
        let localRecords = coordinator.snapshot(table: table)

        for (key, raw) in remote {
            guard let remoteMap = raw as? [String: Any],
                  let id = remoteMap["id"] as? String, id == key else { continue }
            let remoteVersion = (remoteMap["syncVersion"] as? NSNumber)?.int64Value ?? 0

            if let local = localRecords.first(where: { $0.id == id }) {
                let localVersion = (local.content["syncVersion"] as? NSNumber)?.int64Value ?? 0
                let winner = ConflictResolver.resolveUpdate(
                    localVersion: localVersion,
                    remoteVersion: remoteVersion,
                    localContent: { SyncJSON.canonical(local.content) },
                    remoteContent: { SyncJSON.canonical(remoteMap) }
                )
                switch winner {
                case .local:
                    conflictsSolved += 1
                    await push(table: table, id: id, data: local.content)
                    continue
                case .equal:
                    continue
                case .remote:
                    break
                }
            }
            if coordinator.applyRemote(table: table, map: remoteMap) {
                applied += 1
            }
        }

        // Mezar taşlarını uygula (silme yayılımı)
        await applyTombstones(table: table, coordinator: coordinator)
        if applied > 0 {
            lastSyncTime = Date()
            onRemoteDataApplied?()
        }
        return applied
    }

    private func applyTombstones(table: String, coordinator: SyncDataCoordinator) async {
        guard let gid = groupId else { return }
        guard let tombstones = try? await client.get(path: "aile_grubu/\(gid)/meta/tombstones/\(table)") as? [String: Any] else {
            return
        }
        let localRecords = coordinator.snapshot(table: table)
        for (id, raw) in tombstones {
            guard let info = raw as? [String: Any],
                  let deletedAt = (info["deletedAt"] as? NSNumber)?.int64Value else { continue }
            guard let local = localRecords.first(where: { $0.id == id }) else { continue }
            let localVersion = (local.content["syncVersion"] as? NSNumber)?.int64Value ?? 0
            if ConflictResolver.resolveDeletion(localVersion: localVersion, deletedAt: deletedAt) == .local {
                conflictsSolved += 1
                await push(table: table, id: id, data: local.content)
            } else {
                coordinator.deleteLocal(table: table, id: id)
            }
        }
    }

    /// Yerel kaydı siler ve mezar taşını yaymak için çağrılır (UI silme akışı).
    func deleteAndSync(table: String, id: String) async {
        let coordinator = SyncDataCoordinator(modelContext: modelContext)
        coordinator.deleteLocal(table: table, id: id)
        await deleteWithTombstone(table: table, id: id)
        onRemoteDataApplied?()
    }

    func disconnect() {
        syncState = .disconnected
        if let uid = uid { membership = .notMember(uid) }
    }
}

// MARK: - Küçük yardımcılar

private extension String {
    var isBlank: Bool { trimmingCharacters(in: .whitespacesAndNewlines).isEmpty }
}

/// Basit JSON kanonikleştirme: deterministik çakışma karşılaştırması için
/// anahtarları sıralı, sayıları tutarlı biçimde string'e çevirir.
enum SyncJSON {
    static func canonical(_ value: Any) -> String {
        if let dict = value as? [String: Any] {
            let keys = dict.keys.sorted()
            return "{" + keys.map { "\"\($0)\":\(canonical(dict[$0] ?? ""))" }.joined(separator: ",") + "}"
        }
        if let array = value as? [Any] {
            return "[" + array.map { canonical($0) }.joined(separator: ",") + "]"
        }
        if let number = value as? NSNumber {
            return number.stringValue
        }
        if let bool = value as? Bool { return bool ? "true" : "false" }
        if let string = value as? String {
            let escaped = string
                .replacingOccurrences(of: "\\", with: "\\\\")
                .replacingOccurrences(of: "\"", with: "\\\"")
                .replacingOccurrences(of: "\n", with: "\\n")
            return "\"\(escaped)\""
        }
        return "null"
    }
}
