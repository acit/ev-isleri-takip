# 🏠 Aile Takip — iOS Sürümü

Aile Takip uygulamasının iOS (SwiftUI) versiyonu.

## 📱 Özellikler

| Özellik | Durum | Açıklama |
|---------|-------|----------|
| ✅ Görev Yönetimi | Tamamlandı | Görev ekleme, tamamlama, silme |
| ✅ Alışveriş Listesi | Tamamlandı | Kategorili alışveriş, işaretleme |
| ✅ Mesajlaşma | Tamamlandı | Aile içi sohbet |
| ✅ Notlar | Tamamlandı | Kategorili notlar, pinleme |
| ✅ Hatırlatıcılar | Tamamlandı | Tekrarlayan hatırlatıcılar |
| ✅ Faturalar | Tamamlandı | Fatura takibi |
| ✅ Sağlık Dashboard | Tamamlandı | Kalori, su, uyku, egzersiz |
| ✅ Aile Üyeleri | Tamamlandı | Üye yönetimi, puanlama |
| ✅ Örnek Veriler | Tamamlandı | Yılmaz Ailesi demo verileri |
| 🔄 QR/Barkod Tarama | Planlandı | CameraKit entegrasyonu |
| 🔄 Firebase Sync | Planlandı | Realtime Database entegrasyonu |

## 🛠 Teknoloji

| Teknoloji | Kullanım |
|-----------|----------|
| **SwiftUI** | UI Framework |
| **SwiftData** | Yerel veri tabanı (iOS 17+) |
| **MVVM** | Mimari pattern |
| **Combine** | Reactive programming |

## 📂 Proje Yapısı

```
ios/
├── AileTakip/
│   ├── AileTakipApp.swift          # App entry point
│   ├── Models/
│   │   └── Models.swift            # SwiftData modelleri
│   ├── Views/
│   │   ├── ContentView.swift       # Ana tab bar + tüm ekranlar
│   │   ├── DashboardView.swift     # Ana sayfa dashboard
│   │   └── TasksView.swift         # Görev yönetimi
│   ├── ViewModels/
│   │   └── MainViewModel.swift     # İş mantığı + CRUD
│   ├── Services/                   # Firebase, Notification vb.
│   └── Resources/                  # Assets, ikonlar
└── README-iOS.md                   # Bu dosya
```

## 🚀 Çalıştırma

### Ön Koşullar
- **Xcode 15+**
- **iOS 17.0+** (SwiftData için)
- **Apple Developer Account** (yayın için)

### Adımlar

1. Xcode'da yeni proje oluşturun:
   ```
   File → New → Project → iOS → App
   Product Name: AileTakip
   Organization Identifier: com.aile.takip
   Storage: SwiftData
   ```

2. `ios/AileTakip/` altındaki dosyaları Xcode projesine kopyalayın

3. Run (⌘ + R) ile çalıştırın

---

## 📲 iOS App Store'a Yayın Rehberi

### Adım 1: Apple Developer Hesabı Oluşturma

1. [developer.apple.com](https://developer.apple.com) adresine gidin
2. "Account" → "Enroll" tıklayın
3. Kişisel veya Kurumsal hesap seçin:
   - **Kişisel (Individual):** Yıllık **$99 USD**
   - **Kurumsal (Organization):** Yıllık **$299 USD** + D-U-N-S numarası gerekli
4. Kimlik doğrulamasını tamamlayın (1-3 iş günü sürebilir)

### Adım 2: Xcode'da Proje Ayarları

1. **Signing & Capabilities** bölümünde:
   - "Automatically manage signing" ✓
   - Team: Kendi Apple Developer hesabınızı seçin
   - Bundle Identifier: `com.aile.takip` (benzersiz olmalı)

2. **General** bölümünde:
   - Version: `1.0`
   - Build: `1`
   - Deployment Target: `iOS 17.0`
   - Devices: `iPhone + iPad`

### Adım 3: App Store Connect'te Uygulama Oluşturma

1. [appstoreconnect.apple.com](https://appstoreconnect.apple.com) adresine gidin
2. "My Apps" → "+" → "New App" tıklayın
3. Doldurun:
   - **Platform:** iOS
   - **Name:** Aile Takip
   - **Primary Language:** Turkish
   - **Bundle ID:** `com.aile.takip`
   - **SKU:** `aile-takip-ios`
   - **User Access:** Full Access

### Adım 4: Gerekli Görseller Hazırlayın

| Görsel | Boyut | Açıklama |
|--------|-------|----------|
| **App Icon** | 1024x1024 PNG | Uygulama ikonu |
| **Screenshot (6.7")** | 1290 x 2796 | iPhone 15 Pro Max |
| **Screenshot (6.5")** | 1242 x 2688 | iPhone 11 Pro Max |
| **Screenshot (5.5")** | 1242 x 2208 | iPhone 8 Plus |
| **Screenshot (iPad 12.9")** | 2048 x 2732 | iPad Pro |
| **App Preview** | Maks. 30 saniye | Video önizleme (isteğe bağlı) |

> **İpucu:** Screenshot'ları Xcode'da `Window → Devices and Simulators` ile alabilirsiniz.

### Adım 5: Uygulama Bilgilerini Girin

App Store Connect'te:

#### A. App Information
- **Name:** Aile Takip
- **Subtitle:** Aile içi görev, alışveriş ve sağlık takibi
- **Category:** Lifestyle / Productivity
- **Content Rights:** Bu uygulama üçüncü taraf içeriği içermez
- **Age Rating:** 4+ (Tüm yaşlar)

#### B. Pricing and Availability
- **Price:** Free (Ücretsiz) veya seçtiğiniz fiyat
- **Availability:** Tüm ülkeler (veya seçili ülkeler)

#### C. App Privacy
Privacy Nutrition Labels doldurun:
- **Contact Info:** Name (SwiftData'da saklanır)
- **Usage Data:** Toplanmaz
- **Diagnostics:** Toplanmaz

### Adım 6: Build'i Yükleme

1. Xcode'da: **Product** → **Archive**
2. Archive hazır olunca: **Distribute App** → **App Store Connect**
3. "Upload" seçin
4. Bitmapping seçeneklerini kabul edin
5. **Upload** tıklayın

> İlk yükleme 5-15 dakika sürebilir. Processing tamamlanana kadar bekleyin.

### Adım 7: TestFlight ile Test

1. App Store Connect → **TestFlight** bölümüne gidin
2. Yüklediğiniz build otomatik olarak görünecek
3. **Internal Testing:**
   - Ekibinize davet edin (maks. 100 kişi)
   - Link: Birkaç dakika içinde aktif olur
4. **External Testing:**
   - Beta testçileri davet edin (maks. 10,000 kişi)
   - Apple Review gerekli (1-2 gün)

### Adım 8: App Review'a Gönderme

1. App Store Connect → Uygulamanızı seçin
2. **App Store** → **iOS App** section
3. Tüm alanları doldurun:
   - **Description:** Uygulamanızı tanıtın
   - **Keywords:** aile, görev, alışveriş, sağlık, takip, fatura
   - **Support URL:** Bir web sitesi gerekli
   - **Privacy Policy URL:** Gerekli (örn: https://example.com/privacy)
4. **Build** seçin (TestFlight'tan yüklediğiniz)
5. **Submit for Review** tıklayın

### Adım 9: Onay ve Yayınlama

- **Review süresi:** Genellikle 24-48 saat
- **Reddedilme durumunda:** Feedback'i okuyun, düzeltin, yeniden gönderin
- **Onaylanınca:** "Release" butonuna basın

---

## ⚠️ Dikkat Edilmesi Gerekenler

### Reddedilme Nedenleri (En Sık)

1. **Crash/Bug:** Uygulama açılmamalı veya çökmemeli
2. **Incomplete metadata:** Eksik açıklama, screenshot, destek URL
3. **Privacy issues:** Gizlilik etiketleri yanlış doldurulmuş
4. **Design issues:** Human Interface Guidelines'a uygun olmayan tasarım
5. **Placeholder content:** Gerçek olmayan demo veriler visible olmamalı

### Gereken Sayfalar

- **Privacy Policy:** Zorunlu (ücretsiz hosting: GitHub Pages, Netlify)
- **Support URL:** Zorunlu (bir e-posta adresi yeterli)
- **Terms of Service:** Önerilen

---

## 🔄 Güncelleme Gönderme

Her yeni sürüm için:

1. Xcode'da version ve build numarasını artırın
2. Archive → Upload → App Store Connect
3. Yeni build'i seçin
4. Release Notes yazın
5. Submit for Review

---

## 💡 İpuçları

### Hızlı Yayın İçin
1. Önce TestFlight ile test edin
2. Privacy Policy'i GitHub Pages'te ücretsiz oluşturun
3. Screenshot'ları自制 (Canva, Figma)
4. Açıklamayı Türkçe yazın (Apple Türkçe destekliyor)

### Maliyet Özeti
| Kalem | Tutar |
|-------|-------|
| Apple Developer (yıllık) | $99 USD |
| Privacy Policy hosting | Ücretsiz (GitHub Pages) |
| Toplam (ilk yıl) | ~$99 USD |

### Alternatif: Flutter veya React Native
Eğer hem Android hem iOS tek kod tabanıyla yayınlanmak istenirse:
- **Flutter:** Dart ile yazılır, Material 3 desteği var
- **React Native:** JavaScript ile yazılır, daha geniş topluluk
- **Kotlin Multiplatform:** Mevcut Android kodu paylaşılabilir

---

## 📧 Destek

Sorularınız için: [GitHub Issues](https://github.com/acit/ev-isleri-takip/issues)

---

**Başarılar! 🎉**