# Changelog

Aile Takip uygulamasının tüm değişiklikleri.

---

## [3.3.1] - 2026-08-31

### 🐛 Düzeltmeler
- iOS build workflow Export IPA adımı düzeltildi (`DerivedData` arama yolu)
- iOS proje dosyaları güncellemesi (assets, build phase)

---

## [3.3.0] - 2026-08-31

### 📱 Versiyon Yükseltme
- `versionCode` 5 → **6**
- `versionName` "3.1.0" → **"3.3.0"**
- Release APK imzalandı (v2/v3 modern signing)

---

## [3.2.0] - 2026-08-30

### 🍎 iOS Platform
- **SwiftUI** ile tam iOS uygulaması oluşturuldu
- SwiftData modelleri (13 model)
- Dashboard, Tasks ekranları
- Kişisel dağıtım rehberi (AltStore / TrollStore / TestFlight)
- Xcode proje yapısı ve export scripti

### 📱 Android Yenilikleri
- **QR/Barkod Tarama** — ML Kit + CameraX entegrasyonu
  - Barkod tarama (EAN/UPC/Code128)
  - QR kod okuma
  - Flaş kontrolü
  - Alışveriş ekranında barkod ile ürün ekleme
  - Senkronizasyonda QR ile grup kodu okuma
- **Örnek Veriler (Sample Data)**
  - Yılmaz Ailesi demo verileri
  - 33 alışveriş ürünü, 12 görev, 4 spor kulübü
  - 8 bütçe kategorisi, 18 harcama
  - 21 haftalık yemek planı, 8 not, 10 hatırlatıcı
- **Özel VectorDrawable İkon**
  - Ev + aile figürleri + kalp + güneş
  - Adaptive icon (Android 8.0+)
  - Monochrome icon (Android 13+ temalı ikon)
  - Gradient arka plan

---

## [3.1.0] - 2026-08-28

### ⚡ Performans Optimizasyonları
- **LRU Bitmap Cache** — 32MB memory cache, MD5 key, RGB_565 format
- **Lazy Image Loading** — Background thread decode, placeholder desteği
- **Compose Recomposition** — `derivedStateOf` ile 9 hesaplama optimize edildi
- **Flow Optimization** — 7 ana Flow'a `distinctUntilChanged()` eklendi
- **Memory Management** — `onTrimMemory()` ile otomatik cache temizleme
- **Coil 3** — Image loading library entegrasyonu

### 📎 Fotoğraf/Belge Ekleme
- **Notlara ek:** Galeri/Dosya ekleme, önizleme, kaldırma
- **Mesajlara ek:** Fotoğraf ekleme, thumbnail önizleme
- **FileProvider** — Güvenli dosya paylaşımı
- **Base64 Depolama** — Room DB'de optimal görsel saklama
- **Bitmap Optimizasyonu** — Maks. 800px genişlik, %70 JPEG kalite

### 🔔 Hatırlatıcılar — Gelişmiş
- **Tekrarlama Kuralları:** Tek sefer, günlük, haftalık, aylık, özel
- **Alarm Sesi Desteği:** 5 ses (Varsayılan, Alarm, Bildirim, Zil Sesi, Acil)
- **Titreşim Paterni:** Önciliğe göre (düşük: hafif, yüksek: yoğun)
- **Erteleme:** 5, 10, 15, 30 dakika seçenekleri
- **Bitiş Tarihi** ve **Aralık** ayarları

---

## [3.0.0] - 2026-08-26

### 🚀 Büyük Güncelleme — Yeni Özellikler
- **Notlar Ekranı** — Renk kodlu notlar, pin/archive, arama
- **Sağlık Paneli** — Su tüketimi, uyku, kalori, egzersiz takibi
- **Bildirim Sistemi** — WorkManager ile zamanlanmış hatırlatıcılar
- **Unit Testler** — 60+ test (TaskDao, NoteDao, Repository, ViewModel)

---

## [2.0.0] - 2026-01-27

### 🔄 Platform Migrasyonu
- **Node.js backend** → **Android Kotlin + Jetpack Compose**
- Room veritabanı entegrasyonu
- Firebase Realtime Database senkronizasyonu
- Firebase Authentication
- Aile Grubu sistemi

---

## [1.0.0] - 2026-01-27

### 🎉 İlk Versiyon
- Node.js + Express backend
- HTML/CSS/JavaScript frontend
- Basic CRUD operations

---

## 📋 Tüm Özellikler

### 📱 Ekranlar
| Ekran | Açıklama |
|-------|----------|
| Dashboard | Ana sayfa, aile özeti, hızlı erişim |
| Görevler | Yapılacaklar listesi, öncelik, tarih |
| Alışveriş | Liste yönetimi, kategori, barkod tarama |
| Bütçe | Gelir/gider takibi, kategoriler |
| Sağlık | Su, uyku, kalori, egzersiz |
| Spor | Kulüp üyelikleri, antrenman kayıtları |
| Notlar | Renkli notlar, arama, pin/archive |
| Mesajlar | Aile içi sohbet, dosya ekleme |
| Hatırlatıcılar | Alarm, tekrarlama, ses desteği |
| Senkronizasyon | Firebase, QR kod ile grup kurma |
| Barkod Tarama | ML Kit ile ürün/grup kodu okuma |

### 🛠 Teknolojiler
| Teknoloji | Kullanım |
|-----------|----------|
| Kotlin + Jetpack Compose | UI framework |
| Room Database | Yerel veritabanı |
| Firebase | Senkronizasyon + Auth |
| WorkManager | Arka plan bildirimleri |
| CameraX + ML Kit | Barkod/QR tarama |
| Coil 3 | Görsel yükleme |
| SwiftUI + SwiftData | iOS uygulaması |

---

> Bu dosya [Keep a Changelog](https://keepachangelog.com/tr/1.0.0/) formatına göre düzenlenmiştir.
