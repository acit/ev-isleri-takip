import Foundation
import CryptoKit

/// Firebase REST üzerinden çalışan kimlik + Realtime Database istemcisi.
///
/// Android'deki Firebase SDK'sının yaptığı işi doğrudan REST API ile yapar;
/// ek bağımlılık (GoogleService-Info.plist / SPM paketi) gerektirmez.
///
/// - Auth: `identitytoolkit.googleapis.com/v1/accounts:*` (e-posta/şifre)
/// - DB:   `{dbUrl}/aile_grubu/{groupId}/...json?auth={idToken}`
///
/// Wire format Android ile birebir aynıdır: her kayıt bir JSON nesnesi
/// (`id`, `syncVersion` + alanlar), tablolar `aile_grubu/{groupId}/` altında.
final class FirebaseSyncClient {

    // MARK: - Yapılandırma

    /// Firebase Web API Key (Firebase Console → Project Settings → General).
    /// NOT: Bu anahtar yalnızca API'yi tanımlar; yetki vermez — kurallar (rules) verir.
    static var apiKey: String = ""

    /// Realtime Database URL (örn. `https://aile-takip-app-default-rtdb.europe-west1.firebasedatabase.app`).
    /// Bölge farklıysa sondaki `.firebasedatabase.app` alan adı değişir.
    static var databaseURL: String = ""

    /// Kalıcı yapılandırmayı yükler (uygulama açılışında çağrılır).
    static func loadSavedConfig() {
        let d = UserDefaults.standard
        apiKey = d.string(forKey: "firebase_api_key") ?? ""
        databaseURL = d.string(forKey: "firebase_db_url") ?? ""
    }

    /// Yapılandırmayı kalıcı saklar.
    static func saveConfig(apiKey: String, databaseURL: String) {
        Self.apiKey = apiKey.trimmingCharacters(in: .whitespaces)
        Self.databaseURL = databaseURL.trimmingCharacters(in: .whitespaces)
        let d = UserDefaults.standard
        d.set(Self.apiKey, forKey: "firebase_api_key")
        d.set(Self.databaseURL, forKey: "firebase_db_url")
    }

    /// Kalıcı saklama (token'lar cihazda tutulur).
    private let defaults: UserDefaults

    // MARK: - Oturum durumu

    private(set) var uid: String?
    private(set) var idToken: String?
    private(set) var refreshToken: String?
    private(set) var tokenExpiresAt: Date = .distantPast

    private let session: URLSession

    init(defaults: UserDefaults = .standard) {
        self.defaults = defaults
        let config = URLSessionConfiguration.default
        config.timeoutIntervalForRequest = 30
        config.timeoutIntervalForResource = 60
        self.session = URLSession(configuration: config)
        loadPersistedSession()
    }

    // MARK: - Hata türleri

    enum SyncError: LocalizedError {
        case notConfigured
        case notSignedIn
        case invalidURL
        case http(Int, String)
        case decoding(String)
        case network(String)

        var errorDescription: String? {
            switch self {
            case .notConfigured: return "Firebase ayarlanmadı (API key / DB URL eksik)"
            case .notSignedIn: return "Oturum açık değil"
            case .invalidURL: return "Geçersiz adres"
            case .http(let code, let message): return friendlyHTTPError(code, message)
            case .decoding(let detail): return "Yanıt okunamadı: \(detail)"
            case .network(let detail): return "Ağ hatası: \(detail)"
            }
        }

        private func friendlyHTTPError(_ code: Int, _ raw: String) -> String {
            switch code {
            case 401: return "Oturum süresi doldu, tekrar giriş yapın"
            case 403: return "Erişim reddedildi (üyelik onaylı mı?)"
            case 404: return "Veritabanı bulunamadı (URL/bölge doğru mu?)"
            case 429: return "Çok fazla deneme, lütfen bekleyin"
            default: return raw.isEmpty ? "Sunucu hatası (\(code))" : raw
            }
        }
    }

    // MARK: - Kimlik doğrulama (Identity Toolkit REST)

    struct AuthSuccess {
        let uid: String
        let idToken: String
        let refreshToken: String
        let expiresInSeconds: Int
    }

    /// Yeni aile hesabı oluşturur (Android `signUpWithEmail` paritesi).
    func signUp(email: String, password: String) async throws -> AuthSuccess {
        guard !Self.apiKey.isEmpty else { throw SyncError.notConfigured }
        let trimmed = email.trimmingCharacters(in: .whitespaces)
        guard trimmed.contains("@") else { throw SyncError.http(400, "Geçerli bir e-posta girin") }
        guard password.count >= 6 else { throw SyncError.http(400, "Şifre en az 6 karakter olmalı") }
        return try await identityRequest(endpoint: "accounts:signUp",
                                         email: trimmed, password: password)
    }

    /// Mevcut aile hesabıyla giriş yapar (Android `signInWithEmail` paritesi).
    func signIn(email: String, password: String) async throws -> AuthSuccess {
        guard !Self.apiKey.isEmpty else { throw SyncError.notConfigured }
        let trimmed = email.trimmingCharacters(in: .whitespaces)
        guard !trimmed.isEmpty else { throw SyncError.http(400, "Geçerli bir e-posta girin") }
        return try await identityRequest(endpoint: "accounts:signInWithPassword",
                                         email: trimmed, password: password)
    }

    /// Hesaptan çıkar; saklanan oturum bilgilerini temizler.
    func signOut() {
        uid = nil
        idToken = nil
        refreshToken = nil
        tokenExpiresAt = .distantPast
        defaults.removeObject(forKey: "sync_uid")
        defaults.removeObject(forKey: "sync_id_token")
        defaults.removeObject(forKey: "sync_refresh_token")
        defaults.removeObject(forKey: "sync_token_expires_at")
    }

    /// Açılışta kalıcı oturumu yükler.
    private func loadPersistedSession() {
        uid = defaults.string(forKey: "sync_uid")
        idToken = defaults.string(forKey: "sync_id_token")
        refreshToken = defaults.string(forKey: "sync_refresh_token")
        let exp = defaults.double(forKey: "sync_token_expires_at")
        tokenExpiresAt = exp > 0 ? Date(timeIntervalSince1970: exp) : .distantPast
    }

    var isSignedIn: Bool { uid != nil && idToken != nil }

    private func identityRequest(endpoint: String, email: String, password: String) async throws -> AuthSuccess {
        guard let url = URL(string: "https://identitytoolkit.googleapis.com/v1/\(endpoint)?key=\(Self.apiKey)") else {
            throw SyncError.invalidURL
        }
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        let body: [String: Any] = ["email": email, "password": password, "returnSecureToken": true]
        request.httpBody = try JSONSerialization.data(withJSONObject: body)

        let (data, response) = try await session.data(for: request)
        guard let http = response as? HTTPURLResponse else { throw SyncError.network("yanıt yok") }
        guard (200..<300).contains(http.statusCode) else {
            throw SyncError.http(http.statusCode, identityErrorMessage(from: data))
        }

        guard let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
              let token = json["idToken"] as? String,
              let refresh = json["refreshToken"] as? String,
              let expires = json["expiresIn"] as? String, let seconds = Int(expires),
              let localId = json["localId"] as? String else {
            throw SyncError.decoding("auth yanıtı")
        }

        uid = localId
        idToken = token
        refreshToken = refresh
        tokenExpiresAt = Date().addingTimeInterval(TimeInterval(seconds) - 60)
        defaults.set(localId, forKey: "sync_uid")
        defaults.set(token, forKey: "sync_id_token")
        defaults.set(refresh, forKey: "sync_refresh_token")
        defaults.set(tokenExpiresAt.timeIntervalSince1970, forKey: "sync_token_expires_at")

        return AuthSuccess(uid: localId, idToken: token,
                           refreshToken: refresh, expiresInSeconds: seconds)
    }

    private func identityErrorMessage(from data: Data) -> String {
        guard let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
              let error = json["error"] as? [String: Any],
              let message = error["message"] as? String else { return "Kimlik doğrulama hatası" }
        switch message {
        case let m where m.contains("EMAIL_EXISTS"):
            return "Bu e-posta zaten kayıtlı"
        case let m where m.contains("EMAIL_NOT_FOUND") || m.contains("INVALID_LOGIN_CREDENTIALS") || m.contains("INVALID_PASSWORD"):
            return "E-posta veya şifre hatalı"
        case let m where m.contains("WEAK_PASSWORD"):
            return "Şifre en az 6 karakter olmalı"
        case let m where m.contains("INVALID_EMAIL"):
            return "Geçersiz e-posta adresi"
        case let m where m.contains("TOO_MANY_ATTEMPTS"):
            return "Çok fazla deneme. Lütfen biraz bekleyin"
        case let m where m.contains("NETWORK"):
            return "İnternet bağlantısı yok"
        default:
            return message
        }
    }

    /// Süresi dolmak üzereyse refresh token ile yeniler.
    private func ensureFreshToken() async throws -> String {
        guard var token = idToken, let _ = uid else { throw SyncError.notSignedIn }
        if Date() < tokenExpiresAt { return token }
        guard let refresh = refreshToken, !Self.apiKey.isEmpty else { throw SyncError.notSignedIn }

        guard let url = URL(string: "https://securetoken.googleapis.com/v1/token?key=\(Self.apiKey)") else {
            throw SyncError.invalidURL
        }
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = try JSONSerialization.data(
            withJSONObject: ["grant_type": "refresh_token", "refresh_token": refresh])

        let (data, response) = try await session.data(for: request)
        guard let http = response as? HTTPURLResponse, (200..<300).contains(http.statusCode) else {
            throw SyncError.http((response as? HTTPURLResponse)?.statusCode ?? 0, "Oturum yenilenemedi")
        }
        guard let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
              let newToken = json["id_token"] as? String,
              let newRefresh = json["refresh_token"] as? String,
              let expires = json["expires_in"] as? String, let seconds = Int(expires) else {
            throw SyncError.decoding("token yenileme yanıtı")
        }
        token = newToken
        idToken = newToken
        refreshToken = newRefresh
        tokenExpiresAt = Date().addingTimeInterval(TimeInterval(seconds) - 60)
        defaults.set(newToken, forKey: "sync_id_token")
        defaults.set(newRefresh, forKey: "sync_refresh_token")
        defaults.set(tokenExpiresAt.timeIntervalSince1970, forKey: "sync_token_expires_at")
        return token
    }

    // MARK: - Realtime Database (REST)

    private func dbBaseURL() throws -> URL {
        // Kök URL: yollar "aile_grubu/..." biçiminde tam yol olarak verilir
        guard let base = URL(string: Self.databaseURL.trimmingCharacters(in: CharacterSet(charactersIn: "/"))) else {
            throw SyncError.invalidURL
        }
        return base
    }

    /// Bir yolun tamamını okur (GET `{path}.json`).
    func get(path: String) async throws -> Any? {
        let token = try await ensureFreshToken()
        var components = URLComponents(url: try childURL(path), resolvingAgainstBaseURL: false)!
        components.queryItems = [URLQueryItem(name: "auth", value: token)]
        var request = URLRequest(url: components.url!)
        request.httpMethod = "GET"
        return try await execute(request, path: path)
    }

    /// Bir yola yazar (PUT `{path}.json`) — Android `setValue` paritesi.
    func put(path: String, value: Any) async throws {
        let token = try await ensureFreshToken()
        var components = URLComponents(url: try childURL(path), resolvingAgainstBaseURL: false)!
        components.queryItems = [URLQueryItem(name: "auth", value: token)]
        var request = URLRequest(url: components.url!)
        request.httpMethod = "PUT"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = try JSONSerialization.data(withJSONObject: value)
        _ = try await execute(request, path: path)
    }

    /// Bir yolu siler (DELETE `{path}.json`) — Android `removeValue` paritesi.
    func delete(path: String) async throws {
        let token = try await ensureFreshToken()
        var components = URLComponents(url: try childURL(path), resolvingAgainstBaseURL: false)!
        components.queryItems = [URLQueryItem(name: "auth", value: token)]
        var request = URLRequest(url: components.url!)
        request.httpMethod = "DELETE"
        _ = try await execute(request, path: path)
    }

    private func childURL(_ path: String) throws -> URL {
        var base = try dbBaseURL()
        for component in path.split(separator: "/") {
            base.appendPathComponent(String(component))
        }
        base.appendPathExtension("json")
        return base
    }

    private func execute(_ request: URLRequest, path: String) async throws -> Any? {
        let (data, response): (Data, URLResponse)
        do {
            (data, response) = try await session.data(for: request)
        } catch {
            throw SyncError.network(error.localizedDescription)
        }
        guard let http = response as? HTTPURLResponse else { throw SyncError.network("yanıt yok") }
        guard (200..<300).contains(http.statusCode) else {
            var message = ""
            if let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
               let err = json["error"] as? String { message = err }
            throw SyncError.http(http.statusCode, message)
        }
        guard !data.isEmpty, data != Data("null".utf8) else { return nil }
        do {
            return try JSONSerialization.jsonObject(with: data)
        } catch {
            throw SyncError.decoding(path)
        }
    }

    // MARK: - Yardımcılar

    static func sha256Hex(_ value: String) -> String {
        let digest = SHA256.hash(data: Data(value.utf8))
        return digest.map { String(format: "%02x", $0) }.joined()
    }
}
