# 🔥 Firebase Console Kurulum Rehberi

Bu rehber, **Aile Takip** uygulaması için Firebase Realtime Database ve Authentication ayarlarını adım adım anlatır.

---

## 📋 Ön Koşullar

- [Firebase Console](https://console.firebase.google.com/) hesabı
- `aile-takip-app` projesi oluşturulmuş olmalı
- `google-services.json` dosyası `app/` klasörüne indirilmiş olmalı

---

## 🔐 Adım 1: Authentication Aktifleştir

1. Firebase Console'da **aile-takip-app** projesini aç
2. Sol menüden **Build → Authentication** tıkla
3. **"Get started"** butonuna bas
4. **Sign-in providers** bölümünde:
   - **Anonymous** (Anonim) → **Enable** yap → **Save**
   - Diğer provider'ları şimdilik kapalı bırak

### ✅ Doğrulama:
- Anonymous provider yeşil "Enabled" etiketini göstermeli

---

## 🗄️ Adım 2: Realtime Database Oluştur

1. Sol menüden **Build → Realtime Database** tıkla
2. **"Create Database"** butonuna bas
3. **Region** seç: **europe-west1** (Türkiye'ye en yakın)
4. **Security rules** seçimi:
   - **"Start in test mode"** seç (şimdilik)
5. **"Enable"** butonuna bas

### ✅ Doğrulama:
- Database URL: `https://aile-takip-app-default-rtdb.firebaseio.com/` görünmeli
- **Data** sekmesinde boş bir root nodes olmalı

---

## 🔒 Adım 3: Security Rules Güncelle

1. Realtime Database sayfasında **"Rules"** sekmesine tıkla
2. Mevcut kuralları sil ve şunları yapıştır:

```json
{
  "rules": {
    "aile_grubu": {
      "$groupId": {
        ".read": "auth != null",
        ".write": "auth != null"
      }
    }
  }
}
```

3. **"Publish"** butonuna bas

### 📝 Açıklama:
- `auth != null` → Sadece giriş yapan kullanıcılar erişebilir
- `aile_grubu` → Uygulamadaki senkronizasyon yolu
- `$groupId` → Her aile grubu için ayrı veri alanı

---

## 🚀 Adım 4: Uygulamayı Test Et

1. APK'yı telefonuna kur
2. Uygulamayı aç
3. **Senkronizasyon** ekranına git
4. Bir **Grup ID** gir (örn: `test-ailesi`)
5. **"Bağlan"** butonuna bas

### ✅ Başarılı Bağlantı:
- "Bağlandı" yeşil durumu görünmeli
- Firebase Console → Realtime Database → Data'da `aile_grubu/test-ailesi/` yolu oluşmalı

---

## 🛠️ Sorun Giderme

### Hata: "Firebase baslatilamadi"
- `google-services.json` dosyasının güncel olduğundan emin ol
- Firebase Console'da Android uygulamasının package name'inin `com.aile.takip` olduğundan emin ol

### Hata: "Permission denied"
- Security rules'ın doğru olduğundan emin ol
- Kullanıcının anonymous olarak giriş yaptığını kontrol et

### Hata: "Database not found"
- Realtime Database'in europe-west1 bölgesinde oluşturulduğundan emin ol
- Database URL'inin doğru olduğundan emin ol

---

## 📊 Ücretsiz Kotası

Firebase Realtime Database ücretsiz kotası:

| Kaynak | Ücretsiz Kota |
|--------|---------------|
| **Depolama** | 1 GB |
| **Indirme** | 10 GB/ay |
| **Yükleme** | 10 GB/ay |
| **Eş Zamanlı Bağlantı** | 50.000 |

Bir aile uygulaması için yeterli! 🎉

---

## 🔐 Production Güvenlik Kuralları (İsteğe Bağlı)

Daha güvenli kurallar istiyorsan:

```json
{
  "rules": {
    "aile_grubu": {
      "$groupId": {
        ".read": "auth != null && root.child('aile_grubu').child($groupId).child('uyeler').child(auth.uid).exists()",
        ".write": "auth != null && root.child('aile_grubu').child($groupId).child('uyeler').child(auth.uid).exists()",
        "uyeler": {
          "$uid": {
            ".write": "auth != null && auth.uid == $uid"
          }
        }
      }
    }
  }
}
```

Bu kurallar sadece gruba üye olan kullanıcıların veri okumasına/yazmasına izin verir.

---

## 📱 iOS Kurulumu

iOS için ayrıca:
1. Firebase Console'da **iOS** uygulaması ekle
2. **Bundle ID**: `com.aile.takip`
3. `GoogleService-Info.plist` dosyasını `ios/` klasörüne indir
4. Xcode'da projeye sürükle-bırak

---

*Bu rehber 2026 için günceldir.*
