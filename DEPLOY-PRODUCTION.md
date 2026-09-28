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

```
firebase/database.rules.json  (repodaki dosyanın tamamı)
```

Bu kurallar, bir kullanıcının gruba erişebilmesi için
`aile_grubu/$group_id/meta/members/{auth.uid}` düğümünde **onaylı üye** olmasını şart koşar.
Grup kimliğini bilen ama onaylanmayan hiç kimse veri okuyamaz/yazamaz.
Üyelik **onay** ile verilir: yeni cihaz katılma isteği gönderir, mevcut bir aile bireyi
uygulamadan onaylar.

#### Authentication:
1. Firebase Console → **Authentication** → **"Başla"**
2. **Email/Password** girişini etkinleştirin
3. **Anonymous** girişi **kapatın** (kullanılmıyor)

> Senkronizasyon erişimi artık **gerçek üye kimliğine (UID)** bağlıdır. Her aile bireyi
> kendi e-posta/şifresiyle "aile hesabı" açar. Anonim hesaplar kimlik kanıtlamadığı için
> veritabanı kurallarında kullanılmaz.

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
- ✅ E-posta/şifre ile aile hesabı (gerçek UID)
- ✅ Onaylı üyelik (grup kurma + katılma isteği)
- ✅ Gerçek zamanlı, otomatik senkronizasyon
- ✅ Tüm verilerin senkronizasyonu

### Kurucu (ilk cihaz) akışı:
1. Uygulamayı açın → **"Senkronizasyon"** ekranına gidin
2. **Aile hesabı** oluşturun (e-posta + şifre)
3. **"Aile Grubunu Oluştur"** ile aile şifresini belirleyin
4. Veriler gruba taşınır ve otomatik senkron başlar

### Katılan cihaz akışı:
1. **Aile hesabı** oluşturun (kendi e-postanızla)
2. Davet kodunu yapıştırın (veya QR tarayın)
3. **"Katılma İsteği Gönder"** → onay beklenir
4. Bir aile bireyi onaylayınca senkron otomatik başlar

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
`firebase/database.rules.json` dosyasını olduğu gibi yayınlayın. Özet:

- Tüm veri tabloları (görev, alışveriş, mesaj, not, hatırlatıcı, sağlık, spor, bütçe…)
  yalnızca **onaylı üyeler** tarafından okunur/yazılır.
- `meta/passcodeHash` yalnızca üyeler tarafından okunur (onaylarken şifre doğrulaması için).
- `joins/{uid}` yalnızca istek sahibi ve mevcut üyeler tarafından görülür.
- Üye erişimini kesmek için `meta/members/{uid}` düğümü silinir.

> ⚠️ Grup kimliği ailenin gizli anahtarıdır; uygulama tarafından 16 karakterlik
> rastgele üretilir. Grup kimliğini bilen ama aile şifresi/onayı olmayan kişi katılamaz.

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

### Şema Değişiklikleri:
- `fallbackToDestructiveMigration()` kullanıldığı için Room şeması değişirse cihaz verisi silinir.
  Şema değiştirmeden önce gerçek bir `Migration` yazmayı tercih edin.
- Veritabanı (`firebase/database.rules.json`) yapısı: `meta/` (üyelik), `joins/` (istekler),
  ve veri tabloları. Yapı değişirse kuralları da güncelleyin.

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
