# 🔥 Firebase Production Kurulum Rehberi

## 📋 Adım Adım Firebase Ayarlari

### Adım 1: Firebase Projesi Oluştur

1. [Firebase Console](https://console.firebase.google.com/) adresine gidin
2. **"Proje Ekle"** butonuna tıklayın
3. Proje adı: `aile-takip-app`
4. Google Analytics'i **devre dışı bırakabilirsiniz** (opsiyonel)
5. **"Proje Oluştur"** butonuna tıklayın

### Adım 2: Android Uygulaması Ekle

1. Firebase Console'da proje seçin
2. **</>** (Android) simgesine tıklayın
3. Doldurun:
   - **Package name**: `com.aile.takip`
   - **App nickname**: `Aile Takip`
   - **Debug signing certificate SHA-1**: (opsiyonel, Google Sign-In için gerekli)
4. **"Uygulamayı Ekle"** butonuna tıklayın

### Adım 3: google-services.json İndir

1. **"Uygulamayı Ekle"** sonrası `google-services.json` dosyası indirilir
2. Bu dosyayı `app/google-services.json` olarak değiştirin

**⚠️ ÖNEMLİ:** Bu dosyayı Git'e push ETMEYIN! (.gitignore'da olmalı)

### Adım 4: Firebase Realtime Database Oluştur

1. Firebase Console → **Realtime Database** → **"Oluştur"**
2. Bölge seçin: **europe-west1** (Türkiye'ye en yakın)
3. **"Test modu"** ile başlayın (sonra production'a geçin)

### Adım 5: Database Kurallarını Ayarla

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

### Adım 6: Firebase Auth Ayarla

1. Firebase Console → **Authentication** → **"Başla"**
2. **Anonymous** giriş etkinleştirin (şimdilik yeterli)
3. İleride **Email/Password** veya **Google Sign-In** ekleyebilirsiniz

### Adım 7: Android Uygulamasını Güncelle

`app/google-services.json` dosyasını indirdikten sonra:

```bash
# Debug APK'sı için (test)
./gradlew assembleDebug

# Release APK'sı için (production)
./gradlew assembleRelease
```

---

## 🔐 Production Database Kuralları

Test modundan çıktıktan sonra şu kuralları kullanın:

```json
{
  "rules": {
    "aile_grubu": {
      "$group_id": {
        // Sadece authenticated kullanıcılar okuyabilir
        ".read": "auth != null && root.child('aile_grubu').child($group_id).child('uyeler').child(auth.uid).exists()",
        
        // Sadece authenticated kullanıcılar yazabilir
        ".write": "auth != null && root.child('aile_grubu').child($group_id).child('uyeler').child(auth.uid).exists()",
        
        "uyeler": {
          "$user_id": {
            ".read": "auth != null",
            ".write": "auth != null && auth.uid == $user_id"
          }
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

## 📱 Uygulama İçi Firebase Bağlantısı

### Mevcut Durum:
- Uygulama anonymous giriş yapıyor
- Grup ID ile bağlantı kuruluyor
- Veriler Realtime Database'e yazılıyor

### Production İçin Yapılacaklar:

1. **Anonymous Auth → Email Auth:** Gerçek kullanıcı hesapları
2. **Grup Oluşturma:** Her aile kendi grubunu oluştursun
3. **Davet Sistemi:** QR kod ile aile üyelerini davet etme
4. **Offline Support:** Çevrimdışı çalışma + senkronizasyon

---

## 🚀 Deployment Adımları

### 1. google-services.json'u Güncelle
```bash
# Firebase Console'dan indirilen dosyayı kopyalayın
cp ~/Downloads/google-services.json app/google-services.json
```

### 2. Build
```bash
# Debug (test için)
./gradlew assembleDebug

# Release (production)
./gradlew assembleRelease
```

### 3. APK'ları Yükle
- **Google Play Store:** APK'yı Play Console'a yükleyin
- **Dağıtım:** Firebase App Distribution ile test edicilere gönderin

---

## 🔧 Sorun Giderme

### "Firebase initially configured" Hatası
- `google-services.json` dosyası eksik veya hatalı
- Firebase Console'dan doğru dosyayı indirin

### "Permission Denied" Hatası
- Database kuralları çok katı
- Test modunda herkese izin verin: `{ "rules": { ".read": true, ".write": true } }`

### "Network Error" Hatası
- İnternet bağlantısı kontrol edin
- Firebase region'ı doğru seçin (europe-west1)

---

## 📊 Firebase Ücretlendirmesi

| Servis | Ücretsiz Limit | Aşım Ücreti |
|--------|----------------|---------------|
| **Realtime Database** | 1 GB depolama, 10 GB/ay transfer | $5/GB |
| **Authentication** | Sınırsız | Ücretsiz |
| **Cloud Functions** | 2M çağrı/ay | $0.40/milyon |
| **Storage** | 5 GB | $0.026/GB/ay |

**Not:** Küçük aileler için ücretsiz plan yeterli olacaktır.

---

## 🔗 Faydalı Linkler

- [Firebase Console](https://console.firebase.google.com/)
- [Firebase Android Setup](https://firebase.google.com/docs/android/setup)
- [Realtime Database Rules](https://firebase.google.com/docs/database/security)
- [Firebase Pricing](https://firebase.google.com/pricing)
