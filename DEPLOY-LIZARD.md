# 🦎 Lizard Dağıtım Durumu (Deployment Handoff)

> Bu dosya, onaylanan "Lizard ile dağıtım" görevinin **sonucunu ve engelini** kaydeder.
> Hiçbir kaynak oluşturulmadı, hiçbir ücretli servis açılmadı, hiçbir sır paylaşılmadı.

Tarih: 2026-09-25

---

## 1. Envanter — Bu depoda ne var?

| Bileşen | Durum | Konum |
|---------|-------|-------|
| Android uygulaması (Kotlin + Jetpack Compose) | ✅ Var | `app/` |
| iOS uygulaması (SwiftUI + Xcode projesi) | ✅ Var | `ios/` |
| Yönetilen servis: Firebase Realtime Database | ✅ Var (kurallar) | `firebase/database.rules.json` |
| Yönetilen servis: Firebase Authentication | ✅ Var (uygulama kodunda) | `app/build.gradle.kts`, `app/google-services.json` |
| Dağıtım yolu: GitHub Actions → `v*` etiketi → APK/IPA | ✅ Var | `.github/workflows/release.yml` |

### Barındırılabilir (host edilebilir) bir servis **yok**

- `package.json` — **yok** (bu bir Node/web projesi değil)
- `Dockerfile` / `docker-compose` — **yok**
- HTTP API / arka plan işçisi (worker) — **yok**
- `frontend/` dizini — **boş**
- `backend/`, `production/`, `dist/` — `.gitignore` içinde ve depoda mevcut değil

**Sonuç:** Bu, son kullanıcıya APK/IPA olarak dağıtılan bir **mobil uygulama**.
Lizard gibi bir uygulama barındırma platformunun dağıtabileceği bir web süreci/API'si
bu depoda bulunmuyor. Uygulamanın gerçek dağıtım kanalı zaten **GitHub Releases**
(`AileTakip-v3.6.0-*.apk` / `.ipa`).

Uygulamanın ihtiyaç duyduğu yönetilen servis **Firebase**'tir (Auth + Realtime Database)
ve bu servis uygulama tarafında zaten bağlıdır; Lizard ile yeniden kurulacak bir şey yoktur.

---

## 2. Engel — Lizard CLI kullanılamadı

| Adım | Sonuç |
|------|-------|
| `lizard skills get core --json` | ❌ `lizard` komutu bulunamadı |
| `npm install -g @lizard-build/cli` | ❌ Sandbox reddetti (paket kurulumu engelli) |
| `lizard status --json` | ❌ Çalıştırılamadı (CLI yok) |

Kılavuz yüklenemediği için `lizard <command> --help --json` ile doğrulama da yapılamadı.
`npx` ve `sudo` prosedür gereği kullanılmaz.

---

## 3. Doğrulama Sonucu

- Oluşturulan kaynak: **yok**
- Genel (public) URL: **yok**
- Dağıtım doğrulaması: **YAPILAMADI**

Bu nedenle **dağıtım başarılı sayılmamıştır.**

---

## 4. Devam Etmek İçin (resmî prosedürdeki komutlar)

Lizard kurulabiliyorsa (yönetici erişimi olan bir terminalde):

```
npm install -g @lizard-build/cli
lizard skills get core --json
lizard status --json
```

- Kimlik doğrulama gerekirse `lizard login` çalıştırılır ve **CLI'nin döndürdüğü URL**
  kullanıcıya verilir. Şifre/token/ödeme bilgisi sohbete **asla** yazılmaz.
- Ücretli kaynak (PostgreSQL/Redis/S3/web servisi) oluşturmadan önce Lizard'ın
  gösterdiği güncel fiyat ile kullanıcıdan **açık onay** alınmalıdır.
- Oluşturulacak her kaynak için önce "bu uygulama buna gerçekten ihtiyaç duyuyor mu?"
  sorusu yanıtlanmalıdır.

---

## 5. Kapsam Notu (kapsam dışına çıkmamak için)

Bu depo için Lizard'da anlamlı bir dağıtım hedefi **yoktur**. Eğer amaç APK/IPA'nın
paylaşılabilir bir adresten indirilmesi ise iki seçenek vardır:

1. **Mevcut yol (önerilen, ek maliyet yok):** `v*` etiketi push edilir; GitHub Actions
   imzalı APK'yı üretip GitHub Release'e ekler — indirme linki hazır olur.
2. **Yeni web bileşeni:** İndirme sayfası veya bir yedekleme API'si **yeni bir uygulama**
   demektir. Bu, ayrı bir istek ve ayrı bir onay gerektirir; mevcut görev kapsamına
   dahil değildir.
