# ✅ Sürüm Yayınlama Kontrol Listesi (Release Checklist)

Aile Takip — Android APK + iOS IPA yayını için adım adım liste.
Her sürümde bu listeyi baştan sona işaretleyin.

> Bu sürüm: **v3.7.3** (`versionCode` 20) — haftalık market listesi (DataStore, son 8 hafta) + haftalar arası karşılaştırma özeti (Yeni/Tekrar/Düşen)
> Not: ML Kit Text Recognition eklendiği için APK boyutu ~81 MB'e çıktı (model APK içinde). İleride `com.google.mlkit:text-recognition` bundled yerinePlay Store indirmeli modelle küçültülebilir
> Yerel derleme (2026-09-26): imzalı release APK üretildi → `AileTakip-v3.6.1-release.apk` (kök dizin); eski APK'lar silindi
> Market görselleri: `art/` klasörü (Play Store ikonu, feature graphic, OG, banner, poster) — `python scripts/generate_images.py` ile üretilir
> Otomatik yayın: `v*` etiketi (tag) push edildiğinde `.github/workflows/release.yml` çalışır.

---

## 1. 🧩 Sürüm Numarası

- [x] `app/build.gradle.kts` içinde `versionName` güncellendi (örn. `"3.6.0"`)
- [x] `app/build.gradle.kts` içinde `versionCode` artırıldı (her sürümde **+1**, asla geri gitmez)
- [x] iOS sürümü hizalandı: `ios/AileTakip.xcodeproj/project.pbxproj` → `MARKETING_VERSION`
      ve `CURRENT_PROJECT_VERSION`
- [x] `CHANGELOG.md` içine yeni sürüm başlığı eklendi (`## [3.6.0] - YYYY-AA-GG`)

---

## 2. 🔐 İmza Anahtarı (Keystore)

- [x] `aile-takip-release.jks` mevcut (yoksa: `keytool -genkey -v -keystore aile-takip-release.jks -alias aile-takip -keyalg RSA -keysize 2048 -validity 10000`)
- [x] Keystore **git'e commit edilmedi** (`.gitignore` içinde)
- [ ] GitHub Secrets tanımlı:
  - [ ] `KEYSTORE_BASE64` (`base64 -w0 aile-takip-release.jks`)
  - [ ] `KEYSTORE_PASSWORD`
  - [ ] `KEY_PASSWORD`
- [ ] Keystore ve şifreler güvenli bir yerde yedeklendi (kaybolursa uygulama güncellenemez!)

---

## 3. 🔥 Firebase (Kimlik + Senkronizasyon)

- [ ] `app/google-services.json` gerçek Firebase projesinden indirildi
- [ ] Realtime Database oluşturuldu (bölge: `europe-west1`)
- [ ] Authentication → **Email/Password** etkin
- [ ] Authentication → **Anonymous** **KAPALI** (anonim hesaplar kimlik kanıtlamaz)
- [ ] `firebase/database.rules.json` **olduğu gibi** publish edildi (test modu `true/true` DEĞİL)
- [ ] Kurallar üyeliği gerçekten zorunlu kılıyor: `meta/members/{auth.uid}` yoksa okuma/yazma reddediliyor
- [ ] `aile_grubu/{grupId}/meta/` altında `ownerUid`, `passcodeHash`, `createdAt`, `members/` oluşuyor
- [ ] `aile_grubu/{grupId}/joins/` altında katılım istekleri görünüyor
- [ ] `aile_grubu/{grupId}/meta/tombstones/` altında silinen kayıtların mezar taşları oluşuyor
- [ ] Yeni grup kimliği **tahmin edilemez** üretiliyor (16 rastgele karakter, `aile_…`)
- [ ] Aile şifresi ve davet kodu yalnızca aile bireyleriyle paylaşıldı
- [ ] Bir üyenin erişimi kaldırıldığında (`meta/members/{uid}` silme) senkron duruyor

---

## 4. 🧪 Test

- [x] `./gradlew test` — tüm birim testleri geçti
- [x] `./gradlew :app:compileDebugKotlin` hatasız
- [x] `./gradlew assembleDebug` başarılı
- [ ] Cihazda manuel duman testi:
  - [ ] Uygulama açılıyor, çökme yok
  - [ ] PIN ile giriş / PIN sıfırlama çalışıyor
  - [ ] Görev, alışveriş, not, hatırlatıcı ekleme/düzenleme/silme
  - [ ] Aile üyesi ekleme, düzenleme, puan verme, silme
  - [ ] Senkronizasyon: iki cihaz aynı grup + şifre ile bağlanıyor
  - [ ] **Auto-sync:** 1. cihazda değişiklik → 2. cihazda birkaç saniye içinde görünüyor
  - [ ] **Silme senkronu:** 1. cihazda silinen kayıt 2. cihazdan da kalkıyor
  - [ ] **Çakışma:** iki cihazda da çevrimdışıyken aynı kayıt düzenlenince, bağlanınca
        son değiştiren kazanıyor ve iki cihaz aynı içeriğe yakınsıyor
  - [ ] **Alan bütünlüğü:** ürün fotoğrafı/barkod/fiyat ve not ekleri senkron sonrası kaybolmuyor
  - [ ] **QR davet:** davet ekranında QR kod üretiliyor ve başka bir telefon kamerasıyla okunabiliyor
  - [ ] **Tek tarama:** QR okutulunca grup bilgileri doluyor ve katılma isteği gönderiliyor
  - [ ] **Onay akışı:** yeni cihaz katılma isteği gönderiyor, üye onaylayınca senkron başlıyor
  - [ ] **Onaysız erişim yok:** onaylanmayan hesap grup kimliğini bilse de veri okuyamıyor
  - [ ] **Şifre işareti:** yanlış aile şifresiyle gelen istek "eşleşmiyor" olarak işaretleniyor ve onay butonu kapalı
  - [ ] İnternet kapalıyken uygulama çalışıyor; açılınca değişiklikler gönderiliyor
  - [ ] Karanlık mod / Material You / dil değişimi çalışıyor

---

## 5. 📦 Yerel Derleme (İsteğe Bağlı)

```bash
# İmzalı release APK
JAVA_HOME=<jdk17> ./gradlew clean assembleRelease

# Çıktı
app/build/outputs/apk/release/app-release.apk
```

- [x] `./gradlew assembleRelease` başarılı — imzalı APK `AileTakip-v3.6.0-release.apk` adıyla kök dizine kopyalandı (SHA-256 doğrulandı)
- [x] APK imzası doğrulandı: `apksigner verify --verbose app/build/outputs/apk/release/app-release.apk`
- [x] APK boyutu makul (release ≈ 31,7 MB)
- [ ] Test cihazına kuruldu: `adb install -r app/build/outputs/apk/release/app-release.apk`
- [ ] Mevcut sürümün üzerine güncelleme yapılabiliyor (veri kaybı yok)

---

## 6. 🏷️ Yayınlama (Tag + GitHub Release)

- [ ] Tüm değişiklikler commit'lendi, çalışma alanı temiz (`git status`)
- [ ] `main` dalı güncel ve `origin`'e push edildi
- [ ] Etiket oluşturuldu ve push edildi:

```bash
git tag -a v3.6.0 -m "Aile Takip v3.6.0"
git push origin v3.6.0
```

- [ ] GitHub Actions → **Release** workflow yeşil (Android + iOS job'ları)
- [ ] GitHub Release oluştu ve dosyalar yüklendi:
  - [ ] `AileTakip-v3.6.0-release.apk` (imzalı)
  - [ ] `AileTakip-v3.6.0-debug.apk`
  - [ ] `AileTakip-v3.6.0.ipa`
- [ ] Release notları gözden geçirildi

---

## 7. 📣 Yayın Sonrası

- [ ] Aile bireylerine yeni sürüm duyuruldu (davet linki / grup mesajı)
- [ ] Eski sürüm kullanan cihazlarda senkron uyumu doğrulandı
- [ ] Firebase Console → Realtime Database'de veri akışı izlendi
- [ ] Firebase Console → Authentication'da anonim kullanıcılar görünüyor
- [ ] Olası çökmeler için `adb logcat | grep -i "AndroidRuntime\|FATAL"` kontrol edildi
- [ ] Android sürümü kullanıma açıldı, iOS dağıtımı (AltStore/TestFlight) hazır

---

## 8. 🔁 Geri Alma (Rollback)

- [ ] Sorun çıkarsa: GitHub Release'i **taslak (draft)** yap veya asset'leri kaldır
- [ ] Kritik hata varsa yeni `versionCode` ile düzeltme sürümü yayınla (**eski etiketi silme/silme** —
      kullanıcılar güncellemeyi alamaz)
- [ ] `fallbackToDestructiveMigration()` kullanıldığı için şema değişikliklerinde
      veri kaybı riskini not et

---

## 📋 Hızlı Referans

| Adım | Komut / Dosya |
|------|----------------|
| Sürüm yükselt | `app/build.gradle.kts` → `versionCode` / `versionName` |
| Değişiklik günlüğü | `CHANGELOG.md` |
| Debug derleme | `./gradlew assembleDebug` |
| Release derleme | `./gradlew assembleRelease` |
| Testler | `./gradlew test` |
| Yayınlama | `git tag vX.Y.Z && git push origin vX.Y.Z` |
| Workflow | `.github/workflows/release.yml` |
| Firebase kurulum | `FIREBASE-SETUP-GUIDE.md`, `DEPLOY-PRODUCTION.md` |
| Veritabanı kuralları | `firebase/database.rules.json` |
