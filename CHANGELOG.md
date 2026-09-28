# Changelog

Aile Takip uygulamasının tüm değişiklikleri.

---

## [3.7.5] - 2026-09-29

### 📷 iOS QR Tarama Sağlamlaştırma + Test Altyapısı
- **Kamera izin akışı düzeltildi** — simülatörde/izinsiz cihazda boş ekran yerine
  açıklayıcı ekran: neden mesajı + (izin durumunda) "Ayarları Aç" + davet kodu
  **elle girme** bölümü; Ayarlar'dan dönüldüğünde izin otomatik yeniden değerlendirilir
- **QRScannerView neden bildirir** — kamera donanımı yok / izin reddi ayrımı yapılır
- **AileTakipTests target eklendi** — `FamilyInviteCodec` için 11 birim test:
  kompakt biçim, WhatsApp metnine gömülü kod, whitespace toleransı, bozuk girdiler
- **make-release.sh** — sürüm bump + tag + release izleme + asset doğrulamayı
  tek komutta birleştiren otomasyon scripti

---

## [3.7.4] - 2026-09-27

> iOS eklentisi: Senkronizasyon ekranına **QR ile katılım** — kamera ile davet kodu
> okutulur, grup kimliği + aile şifresi otomatik dolar, giriş yapılmışsa katılım isteği
> anında gönderilir (Android "tek tarama" akışının paritesi). iOS artık kendi davet
> QR kodunu da üretir (CIFilter, ek bağımlılık yok); kamera izni açıklaması eklendi.
> Ayrıca **canlı izleme (30 sn polling)**: uygulama açıkken uzak değişiklikler otomatik
> gelir (mesajlar, görevler vb. diğer cihazlardan); onay bekleyen kullanıcıların
> üyeliği de periyodik kontrol edilir. Ayarlardan kapatılabilir.

### 💬 Mesajlar + WhatsApp Daveti + iOS Paritesi
- **Sohbet sıralaması düzeltildi** — mesajlar artık kronolojik sıralanır (eski üstte,
  yeni en altta; WhatsApp davranışı). Önceden liste ters sıralı olduğundan en yeni
  mesaj en altta kaydırma sonrası görünmüyordu
- **WhatsApp ile davet** — Senkronizasyon ekranına tek dokunuşla davet metnini
  WhatsApp'a açan buton eklendi (`wa.me` deep link; WhatsApp yüklü değilse genel
  paylaşım ekranına düşer)
- **iOS tarafı güçlendirildi** — Notlar, Hatırlatıcılar, Faturalar, Envanter,
  Yemek Planı, Sağlık, Aile Üyeleri ve Senkronizasyon ekranları eklendi (SwiftUI);
  mesajlar iOS'ta da kronolojik sırayla gösterilir; iOS'ta WhatsApp ile davet paylaşımı
  (`UIApplication.canOpenURL` kontrolü + paylaşım sayfası yedeği)
- **iOS Firebase senkronizasyonu (Android paritesi)** — iOS artık Android ile aynı
  aile grubunu paylaşır: e-posta/şifre aile hesabı, grup oluşturma / onaylı katılım,
  17 tablonun iki yönlü senkronu. Wire format birebir aynı (REST üzerinden; ek bağımlılık
  yok). Çakışmada **son değiştiren kazanır** + deterministik eşitlik bozucu, silme
  yayılımı mezar taşlarıyla, otomatik push 2 sn debounce ile
- iOS sürümü Android ile hizalandı: **3.7.3 (20)**; projeye eksik kaynak dosyaları
  (TodosView, ShoppingView, BudgetView, ModulesView, ShareHelpers, Services/*) eklendi
- Düzeltme: Mesajlar ekranındaki sabit `${...}` metinleri artık gerçek değerleri gösterir
  (mesaj sayısı, karakter sayacı)

---

## [3.7.3] - 2026-09-26

### 📋 Haftalık Market Listesi + Hafta Karşılaştırması
- **Tek dokunuşla liste** — akıllı önerilerden haftalık market listesi üretilir,
  mükerrerler birleştirilir ve ISO hafta anahtarıyla (2026-W39) DataStore'a kaydedilir
  (son 8 hafta tutulur, eskiler otomatik silinir)
- **Haftalar arası özet** — 🔴 Yeni / 🔁 Tekrar / ⬇️ Düşen malzeme sayıları ve
  "geçen haftanın %X'i bu hafta da gerekiyor" oranı; Türkçe karakter ve yazım
  farklarına dayanıklı ad eşleştirmesi ("DOMATES" = "domates")
- Karşılaştırma tamamen yan etkisiz (pure) fonksiyon — 8 yeni birim testi

---

## [3.7.2] - 2026-09-26

### 🧠 Akıllı Alışveriş Önerisi (Yemek Planı ↔ Envanter)
- **Gerçek eşleştirme** — yemek planındaki her yemek için gereken malzemeler
  25+ Türkçe tarif sözlüğünden çıkarılır ve envanterle **normalize edilmiş kelime
  eşleştirmesiyle** karşılaştırılır ("Domates Salçası" ↔ "Salça Domates" eşleşir;
  Türkçe `İ` karakteri dahil karakter normalizasyonu yapılır)
- **Toplu gereksinim** — aynı malzeme birden fazla yemekte gerekiyorsa adetler
  toplanır, envanter stokları bir kez düşülür; her malzeme "hangi yemekler için
  lazım" bilgisiyle listelenir
- **Akıllı Öneri kartı** — Yemek Planı ekranında eksik malzemeler (ilk 3 + genişlet)
  tekil "Ekle" veya **Hepsini Alışveriş Listesine Ekle** ile listeye gider;
  listede zaten olanlar atlanır, ekleme sonrası öneriler anında tazelenir
- Otomatik liste doldurma kaldırıldı: öneriler kullanıcı onayıyla eklenir
- Kiler ürünleri (tuz/baharat/yağ) envanterde yoksa bile gürültü üretmez
- 9 yeni birim testi; `İ` lowercase birleştirici nokta hatası testte yakalandı

---

## [3.7.1] - 2026-09-26

### 🏷️ Hatırlatıcı Kategori Filtreleri
- **Sekme altı kategori çipleri** — Görev / Fatura / Stok / Genel (ve Tümü) ile
  otomatik hatırlatıcıları ayrı ayrı görüntüle; her çip o kategorideki aktif kayıt
  sayısını gösterir
- "Genel" çipi Etkinlik + Sağlık dahil diğer tüm kategorileri kapsar
- Boş durum mesajı seçili filtreye göre düzenlenir ("Görev hatırlatıcısı yok" vb.)

---

## [3.7.0] - 2026-09-26

### 🔗 Modüller Arası Otomatik Hatırlatıcı Senkronu
- **Görev → Hatırlatıcı (tam yaşam döngüsü)** — görev oluşturulunca son tarihten
  1 gün önce hatırlatıcı; **güncellenince** hatırlatıcı da güncellenir; **tamamlanınca**
  iptal; **silinince** hatırlatıcı da silinir
- **Fatura → Hatırlatıcı (tam yaşam döngüsü)** — son ödemeden 2 gün önce hatırlatıcı;
  tutar/tarih değişince yenilenir; **ödendiğinde** otomatik iptal; silinince temizlenir
- **Envanter → Stok hatırlatıcısı** — miktar eşik altına düştüğünde anında hatırlatıcı
  oluşturulur ve ürün alışveriş listesine eklenir; aynı ürün için tekrar oluşturmaz
- Tüm otomatik hatırlatıcılar Senkronizasyon'da `linkedId/linkedType` ile izlenir;
 manuel eklenenlerle karışmaz

### 🧾 Fiş/Fatura OCR (fotoğraftan okuma)
- **Fiş Tara** — barkod internette bulunamadığında fiş/fatura fotoğrafını okut;
  tutar, tarih, başlık ve kategori **cihaz üstü OCR** ile otomatik doldurulur
  (tamamen çevrimdışı, veri cihazdan çıkmaz)
- Türkçe fiş kalıpları: TOPLAM/TUTAR/ÖDENECEK satırları, ARA TOPLAM-KDV hariç,
  `1.234,56` / `1,234.56` para biçimleri, `25.12.2026` tarih biçimleri,
  mağaza adından kategori tahmini (eczane→Sağlık, market→Market, akaryakıt→Ulaşım…)
- Barkod bulunamadığında Alışveriş ekranında "Fiş Tara → Fatura" yönlendirmesi
- 9 yeni birim testi (fiş ayrıştırma kalıpları)

---

## [3.6.4] - 2026-09-26

### 🔴 Kritik Düzeltme — Açılış Çökmesinin Gerçek Kaynağı
- **Başlatma sırası hatası giderildi** — `MainViewModel` kurucusunda üyelik akışı
  dinlemeye alınırken, dinleyicinin yazdığı `syncEnabled` durumu henüz oluşturulmamıştı;
  ilk değer kurucu içinde senkron yayınlandığı için uygulama **her açılışta**
  `NullPointerException` ile çöküyordu. Durum tanımları artık kurucu işinden önce geliyor
- Cihazdan gelen okunabilir çökme raporuyla doğrulandı (`MainViewModel.kt:56`)

---

## [3.6.3] - 2026-09-26

### 🔧 Kritik Düzeltme — Açılışta Çökme
- **R8 minify kapatıldı** — bazı cihazlarda açılış sırasında karartılmış gözlemci
  zincirinde `NullPointerException (ArrayList.add)` ile çökme oluyordu; karartılmış
  stack (`A7.c9.onChanged`) R8 kaynaklı olduğunu doğruladı. APK boyutu biraz artar
  ancak uygulama **kararlı** açılır; olası yeni bir hata da artık **okunabilir** raporlanır

---

## [3.6.2] - 2026-09-26

### 🐞 Kararlılık
- **Senkron akışı hata koruması** — otomatik senkron sırasında beklenmeyen bir hata
  artık uygulamayı kapatmaz; hataya uğrayan tablo atlanır, diğerleri senkrona devam eder
- **Çökme raporu artık iki yerde** — dahili depolama yanında
  `Android/data/com.aile.takip/files/crash_logs/` altına da yazılır
  (dosya yöneticisinden erişilebilir)

---

## [3.6.1] - 2026-09-26

### 🛡️ Kararlılık ve Teşhis
- **Çökme kaydedici** — beklenmeyen bir hata olursa rapor cihazda dosyaya yazılır
  (`crash_logs`), USB/logcat olmadan teşhis edilebilir
- Hedef platform güncellendi: **Android 16 (API 36)** — `compileSdk`/`targetSdk` 35 → 36
- Hata ayıklama için sürüm kodu 13, sürüm adı 3.6.1

---

## [3.6.0] - 2026-09-25

### ⚖️ Çakışma Çözümü (Aynı Kayıt İki Cihazda Değişirse)
- **Son değiştiren kazanır** — her kayıtta tutulan `syncVersion` karşılaştırılır;
  daha yeni olan yerel kayıt **ezilmez**, uzak veri yazılmaz ve yerel veri geri gönderilir
- **Deterministik eşitlik bozucu** — sürümler eşitse içerik karşılaştırılır; iki cihaz da
  **aynı** kararı verir, böylece kalıcı ayrışma (divergence) oluşmaz
- **Silme yayılımı (mezar taşı)** — silinen kayıt diğer cihazlardan da silinir;
  kayıt silindikten **sonra** düzenlenmişse düzenleme kazanır ve kayıt geri gelir
- **Mezar taşı temizliği** — 30 günden eski kayıtlar otomatik silinir
- **Çakışma sayacı** — Senkronizasyon ekranında kaç kayıtta çakışma çözüldüğü gösterilir
- **Test edilebilir mimari** — senkron motoru Firebase/Room'a doğrudan bağlı değil; okuma (`SyncSource`)
  ve yazma (`SyncSink`) arayüzleri üzerinden çalışır, karar mantığı yan etkisiz birim testlerle doğrulanır

### 🧩 Senkron Veri Bütünlüğü (Önemli Düzeltme)
- **Kaybolan alanlar düzeltildi** — uzak veri uygulanırken bazı alanlar varsayılana dönüyordu
  (veri kaybı). Artık tüm alanlar taşınıyor:
  - Alışveriş: barkod, marka, açıklama, ürün fotoğrafı, birim/toplam fiyat, mağaza, not, son alım tarihi
  - Envanter: ürün fotoğrafı, son fiyat, ortalama fiyat, fiyat geçmişi, son mağaza
  - Mesaj: dosya ekleri
  - Not: dosya ekleri
  - Fatura: ekli fotoğraf
  - Hatırlatıcı: tekrar günleri, aralık, bitiş tarihi, alarm sesi, titreşim, erteleme süresi,
    son/sonraki tetiklenme
- **Veri kaybına karşı regresyon testi** — her kayıt tipi için `toMap → parse` round-trip testi
  (eksik alan anında test hatası verir)

### 🔄 Otomatik Senkronizasyon (Auto-Sync)
- **Her değişiklik anında aile bireylerine yayılır** — görev, alışveriş, mesaj, not, hatırlatıcı,
  bütçe, envanter, sağlık ve spor kayıtlarının tamamı değiştiği anda arka planda otomatik gönderilir
- **Değişiklik tespiti (diff tabanlı)** — yalnızca gerçekten değişen/silinen kayıtlar gönderilir,
  tüm tablo yeniden yüklenmez
- **Silme senkronu** — bir kayıt silindiğinde diğer cihazlarda da silinir
- **Senkron döngüsü koruması** — uzaktan gelen veri tekrar geri gönderilmez (echo engelleme)
- **Otomatik senkron anahtarı** — Ayarlar > Senkronizasyon ekranından açılıp kapatılabilir
- Uygulama açılışında otomatik bağlanma ve senkronu sürdürme

### 👨‍👩‍👧‍👦 Aile Üyesi Yönetimi — Yenilendi
- **Yeni Aile Üyeleri ekranı** — avatarlı, renk kodlu, aranabilir üye listesi
- **Şık ekleme/düzenleme sayfası** — canlı avatar önizleme, rol şablonları
  (Anne/Baba/Kız/Oğul/Büyükanne...), renk paleti seçici
- **Üye düzenleme** — mevcut üyelerin adı, rolü ve rengi güncellenebilir
- **Puan yönetimi** — hızlı puan ekleme
- **Silme onayı** — yanlışlıkla silmeyi engelleyen onay adımı
- **Bu cihazı kullanan kişi seçimi** — kim neyi yaptı bilgisi doğru atfedilir

### 🔐 Giriş Yönetimi ve Paylaşım
- **Aile hesabı (gerçek üye kimliği)** — senkronizasyon artık anonim oturum yerine
  e-posta/şifre ile açılan gerçek Firebase hesabı (UID) kullanır; cihazdaki PIN kilidi korunur
- **Aile daveti paylaşımı** — grup kimliği + şifre tek dokunuşla WhatsApp/mesaj ile paylaşılır
- **Cihazda QR kod üretimi** — davet ekranında QR kod uygulama içinde üretilir (tamamen çevrimdışı,
  ZXing core); büyütülmüş görünümde gösterilir
- **Tek tarama ile katılım** — aile bireyi "QR Tara" ile kodu okutur; grup kimliği ve şifre
  otomatik dolar ve katılma isteği tek adımda gönderilir
- **Profil üzerinden kullanıcı yönetimi** — kullanıcı değiştirme, PIN değiştirme, profil düzenleme, çıkış

### 🛡️ Güvenlik — Sadece Aile Bireyleri Erişebilir
- **UID tabanlı veritabanı kuralları** (`firebase/database.rules.json`) — her okuma/yazma
  `meta/members/{auth.uid}` düğümünün varlığını şart koşar
- **Onaylı katılım** — yeni cihaz katılma isteği gönderir; **yalnızca mevcut bir üye onaylayabilir**.
  Kullanıcı kendi kendini üye yapamaz (kurallar bunu engeller)
- **Şifre kanıtı** — katılım isteği aile şifresinin özetini taşır; onaylayan ekranda
  "aile şifresi eşleşiyor / eşleşmiyor" olarak doğrulanır, eşleşmezse onay butonu kapalıdır
- **Tahmin edilemez grup kimliği** — `aile_` + 16 rastgele karakter (eski `aile_1234` deseni kaldırıldı)
- **Erişim iptali** — bir üyenin erişimi kaldırıldığında anında kesilir
- Anonim giriş **kaldırıldı**; grubu bilen ama onaylanmayan hiç kimse veriye erişemez

### 🛡️ Yerel-Öncelikli (Offline-First) Veri
- Tüm veriler öncelikle cihazdaki Room veritabanında saklanır; internet gerekmez
- Senkronizasyon yalnızca aile grubuna aittir; cihaz dışına başka veri gitmez
- Bağlantı koptuğunda uygulama tam çalışmaya devam eder, bağlanınca değişiklikler gönderilir

### 📱 Sürüm
- `versionCode` 11 → **12**
- `versionName` "3.5.0" → **"3.6.0"**

---

## [3.5.0] - 2026-09-03

### 🍎 iOS & Altyapı
- iOS 26 Liquid Glass UI denemesi ve Xcode 18 uyumluluğu (sonra geri alındı)
- iOS CI workflow düzeltmeleri (simulator destination, .app arama, IPA paketleme)
- Firebase Console kurulum rehberi (Realtime Database + Authentication)
- `versionName` "3.4.2" → **"3.5.0"** (iOS ile hizalandı, `versionCode` 11)

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
