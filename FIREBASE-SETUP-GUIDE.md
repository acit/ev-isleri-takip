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
   - **Email/Password** → **Enable** yap → **Save**
   - **Anonymous** provider'ı **kapat** (kullanılmıyor)

### ✅ Doğrulama:
- Email/Password provider yeşil "Enabled" etiketini göstermeli

> **Neden Anonymous değil?** Anonim hesaplar kolayca yeniden üretilebilir ve
> "kim olduğunu" kanıtlamaz. Veritabanı kuralları erişimi **gerçek üye kimliğine (UID)**
> bağlamak için e-posta/şifre hesaplarına ihtiyaç duyar.

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
2. Mevcut kuralları sil ve repodaki **`firebase/database.rules.json`** dosyasının
   tamamını yapıştır
3. **"Publish"** butonuna bas

### 📝 Bu kurallar ne yapar?

- `auth != null` **tek başına yeterli değildir**. Her okuma/yazma ayrıca:
  `meta/members/{auth.uid}` düğümünün **var olmasını** şart koşar.
- Yani bir kullanıcı, grubun **onaylı üyesi değilse** hiçbir veriyi okuyamaz veya yazamaz —
  grup kimliğini bilse bile.
- `meta/members/$uid` düğümü:
  - grubu **ilk kuran** kişi tarafından yazılabilir (üye listesi henüz boşken), veya
  - **mevcut bir üye** tarafından yazılabilir (onay / üye çıkarma).
  Kullanıcı kendi kendini **sonradan** üye yapamaz.
- `joins/$uid` düğümü: kişi yalnızca **kendi** katılım isteğini yazabilir/okuyabilir;
  mevcut üyeler hepsini görür.

### 🔁 Katılım akışı (onay zorunlu)

1. Her aile bireyi kendi e-posta/şifresiyle **aile hesabı** açar (gerçek UID)
2. **Kurucu**: yeni grup oluşturur (tahmin edilemez grup kimliği üretilir) ve aile şifresini belirler
3. Diğerleri: davet kodundaki **grup kimliği + aile şifresi** ile **katılma isteği** gönderir
4. Mevcut bir aile bireyi uygulamada isteği görür (şifre eşleşmesi işaretlenir) ve **onaylar**
5. Onaydan sonra veritabanı kuralları o UID'ye erişim verir ve senkron başlar

> ⚠️ Şifre kontrolü yalnızca onaylayan aile bireyinin ekranında bilgi olarak gösterilir;
> erişimi **asıl zorunlu kılan şey UID tabanlı kurallardır**.

---

## ⚖️ Çakışma Çözümü (Aynı Kayıt İki Cihazda Değişirse)

Her kayıtta `syncVersion` (son değişiklik zamanı) tutulur. Bir kayıt iki cihazda
değiştiğinde **son değiştiren kazanır**:

| Durum | Sonuç |
|-------|-------|
| Yerel `syncVersion` > uzak | Uzak veri **yazılmaz**; yerel veri geri gönderilir |
| Uzak `syncVersion` > yerel | Yerel kayıt güncellenir |
| Sürümler eşit, içerik aynı | Yazma yok (gereksiz trafik önlenir) |
| Sürümler eşit, içerik farklı | İçerik karşılaştırmasıyla **deterministik** karar (tüm cihazlar aynı sonuca varır) |

### 🪦 Silme yayılımı (mezar taşı)

Bir kayıt silindiğinde yalnızca veri düğümü kaldırılırsa diğer cihazlar bunu
göremez ve kayıt geri gelebilir. Bunun için `meta/tombstones/{tablo}/{id}`
altında `deletedAt` zamanı bırakılır:

- Kayıt **silindikten sonra düzenlenmişse** (`syncVersion` > `deletedAt`) düzenleme kazanır
  ve kayıt geri gelir.
- Aksi halde silme uygulanır.
- Mezar taşları **30 gün** sonra otomatik temizlenir.

> Tüm karşılaştırmalar tamamen sayı/zaman tabanlıdır; aynı veriyle tüm cihazlar
> aynı sonucu üretir, yani cihazlar arasında kalıcı ayrışma oluşmaz.

---

## 🚀 Adım 4: Uygulamayı Test Et

1. APK'yı telefonuna kur
2. Uygulamayı aç
3. **Senkronizasyon** ekranına git
4. **Aile hesabı** oluştur (e-posta + şifre, en az 6 karakter)
5. **"Aile Grubunu Oluştur"** ile aile şifresini belirle

### ✅ Başarılı Kurulum:
- "Aile üyesisiniz · Bağlı" durumu görünmeli
- Firebase Console → Authentication'da yeni kullanıcı görünmeli
- Realtime Database → Data'da `aile_grubu/<üretilen-grup-id>/meta/members/<uid>` oluşmalı

### 👥 İkinci cihazı ekleme (tek tarama)
1. İkinci cihazda da aile hesabı oluştur (farklı e-posta)
2. Birinci cihaz: **Senkronizasyon → Aile Bireylerini Davet Et** → **"QR Kodu Büyüt"**
   (QR kod cihazda üretilir, internet gerekmez)
3. İkinci cihaz: **Senkronizasyon → Var Olan Gruba Katıl → QR Tara**
4. QR okununca grup kimliği ve aile şifresi otomatik dolar; **katılma isteği hemen gönderilir**
5. Birinci cihazda **Katılım İstekleri** bölümünde isteği **Onayla**
6. Onaydan sonra iki cihaz otomatik senkronize olur

> QR kod çalışmazsa davet kodunu elle yapıştırmak da yeterlidir.

---

## 🛠️ Sorun Giderme

### Hata: "Firebase baslatilamadi"
- `google-services.json` dosyasının güncel olduğundan emin ol
- Firebase Console'da Android uygulamasının package name'inin `com.aile.takip` olduğundan emin ol

### Hata: "Permission denied"
- `firebase/database.rules.json` kurallarının **publish edildiğinden** emin ol
- Kullanıcının **aile hesabıyla giriş yaptığından** emin ol (Anonymous kapalı olmalı)
- Kullanıcının UID'sinin `meta/members/` altında bulunduğunu doğrula
  (yoksa katılım isteği gönderip bir aile bireyinin **onaylaması** gerekir)

### Hata: "Katılım isteği onaylanmıyor"
- İsteği gönderen kişi **doğru aile şifresini** girmiş olmalı
  (onaylayan ekranda "Aile şifresi eşleşmiyor" uyarısı çıkar)
- Group ID'nin davet koduyla birebir aynı olduğundan emin ol

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

## 🔐 Production Güvenlik Kuralları

Üretimde **`firebase/database.rules.json`** kullanılır. Özet mantık:

```
onaylı üye mi? = root.child('aile_grubu').child($gid).child('meta')
                     .child('members').child(auth.uid).exists()
```

- Tüm veri tabloları (görev, alışveriş, mesaj, not, hatırlatıcı, sağlık, spor, bütçe…)
  yalnızca **onaylı üyeler** tarafından okunur/yazılır.
- `meta/passcodeHash` yalnızca üyeler tarafından okunur.
- Kullanıcı kendi kendini üye yapamaz; üyelik **onay** ile verilir.
- Bir üyenin erişimini kesmek için `meta/members/{uid}` düğümü silinir.
  (Uygulama: **Senkronizasyon → üye listesi**)

> **Önemli:** Grup kimliği (`aile_XXXX…`) ailenin gizli anahtarıdır. 16 karakterlik
> rastgele üretilir; kısa/akılda kalıcı kimlikler (örn. `aile_1234`) kullanılmaz.

---

## 📱 iOS Kurulumu

iOS için ayrıca:
1. Firebase Console'da **iOS** uygulaması ekle
2. **Bundle ID**: `com.aile.takip`
3. `GoogleService-Info.plist` dosyasını `ios/` klasörüne indir
4. Xcode'da projeye sürükle-bırak

---

*Bu rehber 2026 için günceldir.*
