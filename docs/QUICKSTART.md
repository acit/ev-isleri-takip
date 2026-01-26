# Hızlı Başlangıç Rehberi

Bu rehber, projeyi çalıştırmak için adım adım talimatlar sağlar.

## 1️⃣ Ön Gereksinimler

- **Node.js** v16+ (https://nodejs.org)
- **PostgreSQL** v12+ (https://www.postgresql.org)
- **npm** (Node.js ile birlikte gelir)
- **Git**

## 2️⃣ Veritabanını Kurma

### Windows
```powershell
# PostgreSQL'i yüklediyseniz, command prompt açın
createdb ev_isleri_takip
psql -U postgres -d ev_isleri_takip -f backend/src/models/database.sql
```

### macOS/Linux
```bash
createdb ev_isleri_takip
psql ev_isleri_takip < backend/src/models/database.sql
```

## 3️⃣ Backend Kurulumu

```bash
# Backend klasörüne git
cd backend

# Paketleri kur
npm install

# .env dosyasını oluştur
cp .env.example .env

# .env dosyasını düzenle (Gmail ve PostgreSQL bilgilerini ekle)
# Editörde aç: .env
```

### .env Dosyası Örneği
```
DATABASE_URL=postgresql://postgres:password@localhost:5432/ev_isleri_takip
JWT_SECRET=your_secret_key_at_least_15_chars_long
EMAIL_USER=your.email@gmail.com
EMAIL_PASSWORD=your_app_password
FRONTEND_URL=http://localhost:3000
PORT=5000
```

**Gmail Şifresi**: Gmail kullanıyorsanız, hesap şifrenizi değil uygulama şifresi kullanın:
1. Google Account → Security
2. App passwords ara
3. Gmail → Windows Computer seç
4. Oluşturulan şifreyi kopyala

## 4️⃣ Backend Çalıştırma

```bash
# Backend klasöründeyken
npm run dev
```

Backend hazır olduğunda: `Server running on port 5000`

## 5️⃣ Frontend Kurulumu

Yeni terminal açın ve:

```bash
# Proje klasörüne dön
cd frontend

# Paketleri kur
npm install

# Frontend'i çalıştır
npm start
```

Frontend açılacak: http://localhost:3000

## 🧪 Test Etme

### Giriş Testi

1. Frontend'de http://localhost:3000 açın
2. Bir email girin (henüz hesap yoksa, frontend hata gösterecek)
3. Backend'de veritabanında test kullanıcısı oluştur:

```sql
INSERT INTO users (email, family_id, role, status, password_hash) 
VALUES ('test@example.com', 1, 'admin', 'active', '');

INSERT INTO families (name, created_by) 
VALUES ('Test Ailesi', 1);
```

4. Email girip giriş kodunu alın
5. 6 haneli kodu girin

## 📁 Proje Dosyaları

```
ev-isleri-takip/
├── backend/              ← Node.js API (port 5000)
│   └── src/
│       ├── controllers/  ← İş mantığı
│       ├── routes/       ← API endpoint'leri
│       ├── models/       ← Veritabanı şeması
│       └── config/       ← DB ve Email ayarları
├── frontend/             ← React UI (port 3000)
│   └── src/
│       ├── pages/        ← Sayfalar
│       ├── utils/        ← API istemcisi
│       └── store/        ← Durum yönetimi
├── docs/                 ← Belgeler
└── README.md             ← Ana belge
```

## 🔧 Yaygın Sorunlar

### "Cannot connect to database"
- PostgreSQL çalışıyor mu? → `psql -U postgres` ile test et
- DATABASE_URL doğru mu? → `.env` dosyasını kontrol et
- Veritabanı oluşturuldu mu? → `createdb ev_isleri_takip`

### "Email sending failed"
- Gmail app password doğru mu?
- 2-factor authentication açık mı?
- Daha az güvenli uygulamalar izni vermiştin mi?

### "Port 5000 already in use"
```bash
# Port değiştir
PORT=5001 npm run dev
```

### "npm install başarısız"
```bash
# Node_modules sil
rm -r node_modules package-lock.json
npm install
```

## 📚 Sonraki Adımlar

1. **Dashboard Sayfası**: Ana sayfayı tasarla ve işlevselleştir
2. **Görev Oluşturma**: Kullanıcılar görev ekleyebilsin
3. **Stok Yönetimi**: Envanter takibi ara yüzü
4. **Alışveriş Listeleri**: Otomatik liste oluşturma
5. **Bütçe Takibi**: Harcama kaydetme ve rapor

## 🆘 Yardım

- API Endpoints: [README.md](../README.md#-api-endpoints) bölümüne bak
- Kod Detayları: [copilot-instructions.md](../.github/copilot-instructions.md)
- Veritabanı: [database.sql](../backend/src/models/database.sql)

---

**Hepsi bitti! Backend ve frontend'i açık tutun. Geliştirmeye başlayabilirsin.** 🚀
