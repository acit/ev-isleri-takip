# Geliştirme Kılavuzu

## Yerel Ortamda Çalıştırma

### Seçenek 1: Tek komutla başlat
```bash
npm install-all      # Tüm paketleri kur
npm run dev          # Backend ve frontend'i aynı anda başlat
```

### Seçenek 2: Ayrı terminallerden başlat
```bash
# Terminal 1
cd backend
npm run dev

# Terminal 2
cd frontend
npm start
```

## Veritabanı Yönetimi

### İlk Kurulum
```bash
# PostgreSQL'de veritabanı oluştur
createdb ev_isleri_takip

# Şemayı yükle
psql -U postgres -d ev_isleri_takip < backend/src/models/database.sql
```

### Test Kullanıcısı Oluşturma
```bash
psql -U postgres -d ev_isleri_takip -c "
INSERT INTO families (name, created_by) VALUES ('Test Ailesi', 1);
INSERT INTO users (email, family_id, role, status, password_hash) 
VALUES ('test@example.com', 1, 'admin', 'active', '');
"
```

### Veritabanını Sıfırla
```bash
# Tüm verileri sil
dropdb ev_isleri_takip

# Yeni baştan kur
createdb ev_isleri_takip
psql -U postgres -d ev_isleri_takip < backend/src/models/database.sql
```

## Yeni Özellik Ekleme

### Adım 1: Veritabanı Şemasını Güncelle
- `backend/src/models/database.sql` düzenle
- SQL komutlarını ekle
- Veritabanını güncelle: `psql -U postgres -d ev_isleri_takip < backend/src/models/database.sql`

### Adım 2: Backend Kontrollerini Yazı
1. `backend/src/controllers/` altında dosya oluştur veya düzenle
2. CRUD fonksiyonlarını yaz (create, read, update, delete)
3. Her sorguda `WHERE family_id = $X` ekle

### Adım 3: Routes Ekle
- `backend/src/routes/` altında router tanımla
- authMiddleware'i kullan
- Controller fonksiyonlarını import et

### Adım 4: API İstemcisini Güncelle
- `frontend/src/utils/api.ts` açılı
- Yeni API metodlarını ekle
- Örnek:
```typescript
export const myFeatureAPI = {
  getAll: () => apiClient.get('/my-feature'),
  create: (data) => apiClient.post('/my-feature', data),
  update: (id, data) => apiClient.patch(`/my-feature/${id}`, data),
};
```

### Adım 5: Frontend Bileşenini Oluştur
- `frontend/src/pages/` veya `frontend/src/components/` altında dosya oluştur
- API istemcisini import et
- Veri çek ve görüntüle
- Zustand store'dan token al: `const token = useAuthStore((state) => state.token)`

## Testing

### Backend Test Yazma
```bash
cd backend
npm test
```

Dosya örneği: `backend/src/controllers/__tests__/tasks.test.ts`

### Frontend Test Yazma
```bash
cd frontend
npm test
```

## Debugging

### Backend Debugging
```bash
# VS Code'da Debugger kullan
# Run → Add Configuration → Node
# Program: ${workspaceFolder}/backend/dist/index.js

# Veya console.log ile
```

### Frontend Debugging
- React DevTools Chrome extension kur
- Tarayıcı DevTools'u aç (F12)
- Console ve Network sekmesini kontrol et

## API Testing

### Postman ile
1. Postman'ı aç
2. POST: `http://localhost:5000/api/auth/login-code`
3. Body: `{"email": "test@example.com"}`
4. Response'dan token al
5. Başka istekler için: Authorization → Bearer token

### cURL ile
```bash
# Login kodu gönder
curl -X POST http://localhost:5000/api/auth/login-code \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com"}'

# Kodu doğrula
curl -X POST http://localhost:5000/api/auth/verify-code \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","code":"123456"}'

# Token ile korunan endpoint'i çağır
curl -X GET http://localhost:5000/api/tasks \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"
```

## Deployment

### Heroku'ya Deploy

#### Backend
```bash
cd backend
heroku create your-app-backend
git push heroku main
```

#### Frontend
```bash
cd frontend
heroku create your-app-frontend
npm run build
git push heroku main
```

### Docker ile Local Test

```bash
# Backend
docker build -t ev-isleri-backend ./backend
docker run -p 5000:5000 -e DATABASE_URL=... ev-isleri-backend

# Frontend  
docker build -t ev-isleri-frontend ./frontend
docker run -p 3000:3000 ev-isleri-frontend
```

## Sık Sorulan Sorunlar

**S: "Module not found" hatası alıyorum**
Ç: `npm install` veya `npm install concurrently` (root klasörde)

**S: Backend porta bağlanamıyor**
Ç: `npm run dev:backend` veya PORT değiştir: `PORT=5001 npm run dev:backend`

**S: Frontend API'ye bağlanamıyor**
Ç: Backend çalışıyor mu? `.env.example`'i kontrol et, REACT_APP_API_URL doğru mu?

**S: Veritabanına bağlanamıyor**
Ç: PostgreSQL çalışıyor mu? DATABASE_URL doğru mu? `psql` test et

**S: Email gönderemiyor**
Ç: Gmail app password doğru mu? 2FA açık mı? `.env` dosyasını kontrol et

---

İyi geliştirmeler! 🚀
