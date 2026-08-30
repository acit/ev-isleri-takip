# 📲 iOS Kişisel Dağıtım Rehberi (GitHub Üzerinden)

App Store olmadan uygulamanızı dağıtmak için **3 ana yöntem** var:

---

## 🏆 Yöntem 1: GitHub Releases + Sideloading (En Kolay)

Uygulamayı .ipa dosyası olarak GitHub'a yükleyin, kullanıcılar indirip kursun.

### Adım 1: Xcode'da IPA Oluşturun

1. Xcode'da: **Product** → **Archive**
2. Archive hazır olunca: **Distribute App**
3. **"App Store Connect" DEĞİL**, şunlardan birini seçin:
   - **"Ad Hoc"** (belirli cihazlara)
   - **"Enterprise"** (kurumsal hesap varsa)
4. **Export** → Bir klasöre kaydedin
5. Oluşan `.ipa` dosyasını bulun

### Adım 2: GitHub'a Release Oluşturun

```bash
# Git tag ekleyin
git tag v1.0.0
git push origin v1.0.0
```

Sonra GitHub'da:
1. Repository → **Releases** → **Create a new release**
2. Tag: `v1.0.0`
3. Title: `Aile Takip v1.0.0`
4. Description yazın
5. `.ipa` dosyasını **"Attach binaries"** bölümüne sürükleyin
6. **Publish release** tıklayın

### Adım 3: Kullanıcılar Nasıl Yükler

Kullanıcılar şu uygulamalardan birini kullanır:

| Uygulama | Platform | Ücretsiz mi? | Açıklama |
|----------|----------|--------------|----------|
| **AltStore** | macOS/Windows | ✅ Evet | En popüler, 7 gün yenileme |
| **Sideloadly** | macOS/Windows | ✅ Evet | Kolay kullanım |
| **Cydia Impactor** | macOS/Windows | ✅ Evet | Klasik yöntem |
| **TrollStore** | iOS 14-17 | ✅ Evet | Kalıcı kurulum ( jailbreak gerektirmez) |

### AltStore ile Yükleme Adımları:

1. [AltStore](https://altstore.io) indirin
2. Bilgisayara kurun (iTunes gerekli)
3. iPhone'u USB ile bilgisayara bağlayın
4. AltStore'u açın → "My Apps" → "+" tıklayın
5. `.ipa` dosyasını seçin
6. Apple ID ile giriş yapın
7. 7 gün sonra yenilemeniz gerekir (AltStore otomatik yapar)

---

## 🔄 Yöntem 2: TestFlight (Beta Dağıtımı)

TestFlight, Apple'ın resmi beta dağıtım aracıdır. **100 kişiye kadar** dağıtılabilir.

### Adım 1: App Store Connect'te Uygulama Oluşturun

> **Not:** Uygulamayı yayınlamayacaksınız, sadece TestFlight için kullanacaksınız.

1. [appstoreconnect.apple.com](https://appstoreconnect.apple.com) → **My Apps** → **+**
2. Adım 3'teki bilgileri doldurun
3. **App Store** bölümüne geçmeyin, sadece **TestFlight** bölümüne gidin

### Adım 2: Build Yükleme

1. Xcode: **Product** → **Archive**
2. **Distribute App** → **App Store Connect** → **Upload**
3. 5-15 dakika işleme alınır

### Adım 3: TestFlight'a Davet

1. App Store Connect → **TestFlight** → Internal Testing
2. **"Create Internal Group"** → Ekip üyelerinizi ekleyin
3. Davet linki gönderilir
4. Kullanıcılar **TestFlight** uygulamasını App Store'dan indirir
5. Davet linkini açar → Uygulamayı yükler

### Avantajları:
- ✅ Apple Developer Account gerekli ($99/yıl)
- ✅ 100 internal tester (Apple ID gerekmez)
- ✅ 10,000 external tester (Apple ID gerekir)
- ✅ 90 gün geçerli
- ✅ Jailbreak gerektirmez

### Dezavantajları:
- ❌ Yıllık $99 USD
- ❌ 90 gün sonra yeniden yüklemeniz gerekir
- ❌ Her 30 günde 1 build limiti

---

## 🛡 Yöntem 3: TrollStore (Kalıcı Kurulum — iOS 14-17)

TrollStore, jailbreak olmadan kalıcı imza uygulayan bir araçtır.

### Avantajları:
- ✅ Kalıcı kurulum (yenileme gerekmez)
- ✅ Ücretsiz
- ✅ Jailbreak gerektirmez

### Dezavantajları:
- ❌ Sadece iOS 14-17 arası
- ❌ Kurulumu biraz karmaşık
- ❌ Her cihaza tek tek kurulur

### Kurulum Adımları:

1. [TrollStore GitHub](https://github.com/opa334/TrollStore) adresinden talimatları takip edin
2. Cihazınıza uygun yöntemi seçin (misaka, Dopamine, vb.)
3. TrollStore'u kurun
4. `.ipa` dosyasını TrollStore'a yükleyin
5. Uygulama kalıcı olarak yüklenir

---

## 📋 Karşılaştırma Tablosu

| Özellik | GitHub + Sideloading | TestFlight | TrollStore |
|---------|---------------------|------------|------------|
| **Apple Developer** | Gerekmez | $99/yıl | Gerekmez |
| **Jailbreak** | Gerekmez | Gerekmez | Gerekmez |
| **Kişi Sayısı** | Sınırsız | 100-10,000 | Sınırsız |
| **Süre** | 7 gün* | 90 gün | Kalıcı |
| **Dağıtım Kolaylığı** | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐ |
| **Güvenilirlik** | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ |

> *AltStore ile 7 gün sonra otomatik yenilenir (bilgisayar gerekli)

---

## 🚀 Önerilen Strateji

**En pratik yol:** GitHub Releases + AltStore

1. GitHub'da `.ipa` dosyasını yayınlayın
2. Kullanıcılara AltStore kurulumu için talimat gönderin
3. 7 günde bir bilgisayara bağlanarak yenileyin

**Eğer 100 kişiye kadar dağıtacaksanız:** TestFlight

1. Apple Developer hesabı alın ($99/yıl)
2. TestFlight ile dağıtın
3. Kullanıcılar App Store'dan TestFlight indirir

---

## 📝 GitHub README'a Eklenecek Bölüm

```markdown
## 📲 iOS Kurulumu

### Yöntem 1: AltStore ile (Önerilen)
1. [AltStore](https://altstore.io) indirin
2. iPhone'u bilgisayara bağlayın
3. `AileTakip.ipa` dosyasını yükleyin

### Yöntem 2: Sideloadly ile
1. [Sideloadly](https://sideloadly.io) indirin
2. iPhone'u bilgisayara bağlayın
3. IPA dosyasını seçin → Start

### Yöntem 3: TrollStore ile (Kalıcı)
1. [TrollStore](https://github.com/opa334/TrollStore) kurun
2. IPA dosyasını TrollStore'a yükleyin
3. Kalıcı olarak yüklenir
```

---

## ⚠️ Dikkat

1. **Apple Developer hesabı olmadan** sadece sideloading yapılabilir
2. **7 gün yenileme** zorunluluğu sideloading'de var
3. **TrollStore** sadece belirli iOS sürümlerinde çalışır
4. **Dağıtım linkini** herkese açmayın, güvensiz kaynaklardan indirmeyin
