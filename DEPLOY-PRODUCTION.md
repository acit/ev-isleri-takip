# 🚀 Production Deployment Rehberi

## 📋 Adım Adım Production'a Geçiş

### Adım 1: Firebase Projesi Oluştur

1. [Firebase Console](https://console.firebase.google.com/) gidin
2. Yeni proje oluşturun: `aile-takip-prod`
3. Android uygulaması ekleyin:
   - Package: `com.aile.takip`
   - App nickname: `Aile Takip`

### Adım 2: google-services.json'u Güncelle

1. Firebase Console → Proje Ayarları
2. **"Uygulamalar"** sekmesinden Android uygulamasını seçin
3. **"google-services.json'u indir"** butonuna tıklayın
4. Dosyayı `app/google-services.json` olarak değiştirin

```bash
cp ~/Downloads/google-services.json app/google-services.json
```

### Adım 3: Firebase Servislerini Aktifleştir

#### Realtime Database:
1. Firebase Console → **Realtime Database** → **"Oluştur"**
2. Bölge: **europe-west1** (Türkiye)
3. Kurallar (Production):

```json
{
  "rules": {
    "aile_grubu": {
      "$group_id": {
        ".read": "auth != null",
        ".write": "auth != null"
      }
    }
  }
}
```

#### Authentication:
1. Firebase Console → **Authentication** → **"Başla"**
2. **Anonymous** giriş etkinleştirin
3. Opsiyonel: **Email/Password** ekleyin

### Adım 4: Release APK Oluştur

```bash
# Release APK'sı (imzalı)
./gradlew assembleRelease

# APK konumu
app/build/outputs/apk/release/app-release.apk
```

### Adım 5: APK'yı Test Cihazına Kur

```bash
# USB ile
adb install app/build/outputs/apk/release/app-release.apk

# veya APK'yı telefona gönderip kurun
```

---

## 📱 Uygulama İçi Firebase Kullanımı

### Mevcut Özellikler:
- ✅ Anonymous giriş (otomatik)
- ✅ Grup ID ile bağlantı
- ✅ Gerçek zamanlı senkronizasyon
- ✅ Tüm verilerin senkronizasyonu

### Kullanım Akışı:
1. Uygulamayı açın
2. **"Senkronizasyon"** ekranına gidin
3. **Grup ID** girin (veya QR kod ile tarayın)
4. **"Bağlan"** butonuna tıklayın
5. Veriler otomatik olarak senkronize edilir

---

## 🔐 Güvenlik Kuralları

### Test Modu (Development):
```json
{
  "rules": {
    ".read": true,
    ".write": true
  }
}
```

### Production Modu:
```json
{
  "rules": {
    "aile_grubu": {
      "$group_id": {
        ".read": "auth != null",
        ".write": "auth != null",
        "uyeler": {
          ".read": "auth != null",
          ".write": "auth != null"
        },
        "veriler": {
          ".read": "auth != null",
          ".write": "auth != null"
        }
      }
    }
  }
}
```

---

## 📊 Monitörleme ve Loglar

### Firebase Console'da:
- **Realtime Database** → Verileri canlı izleyin
- **Authentication** → Kullanıcı girişlerini görün
- **Analytics** → Kullanım istatistikleri
- **Crashlytics** → Hata raporları (opsiyonel)

### Hata Ayıklama:
```bash
# Logcat ile Firebase loglarını查看
adb logcat | grep -i firebase
```

---

## 🔄 Güncelleme Akışı

### Yeni Versiyon Yayınlama:
1. `build.gradle.kts`'de versiyonu artırın
2. Release APK oluşturun
3. APK'yı test cihazına kurun
4. Test edin
5. Üretim cihazlarına yayınlayın

### Veri Koruma:
- Room veritabanı migration ile veriler korunur
- Firebase'deki veriler cihaz değişikliğinde korunur
- Yedekleme: Firebase Automatic Backups

---

## 💰 Maliyet Tahmini

### Küçük Kullanım (10 aile):
- Realtime Database: ~100 MB depolama
- Transfer: ~1 GB/ay
- **Maliyet: Ücretsiz**

### Orta Kullanım (100 aile):
- Realtime Database: ~1 GB depolama
- Transfer: ~10 GB/ay
- **Maliyet: ~$5/ay**

### Büyük Kullanım (1000+ aile):
- Realtime Database: ~10 GB depolama
- Transfer: ~100 GB/ay
- **Maliyet: ~$25/ay**

---

## 🔗 Faydalı Linkler

- [Firebase Console](https://console.firebase.google.com/)
- [Realtime Database Docs](https://firebase.google.com/docs/database)
- [Firebase Android Setup](https://firebase.google.com/docs/android/setup)
- [Firebase Security Rules](https://firebase.google.com/docs/database/security)
- [Firebase Pricing](https://firebase.google.com/pricing)
