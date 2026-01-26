# Proje Yapısı ve Dosya Açıklaması

## Backend Dosyaları

### `src/controllers/`
Her controllerda belirli bir özellik için iş mantığı vardır:

- **auth.ts**: Login, invite, join family işlemleri
- **tasks.ts**: Görev oluşturma, güncelleme, tamamlama
- **inventory.ts**: Stok ekleme, güncelleme, düşük stok kontrol
- **shopping.ts**: Alışveriş listesi oluşturma, email gönderme
- **budget.ts**: Bütçe belirleme, harcama kaydetme

### `src/routes/`
Her dosya express Router tanımlar:

- **auth.ts**: `/api/auth/*` endpoints
- **tasks.ts**: `/api/tasks/*` endpoints
- Diğer route dosyaları benzer şekilde

### `src/middleware/auth.ts`
JWT token doğrulama ve role kontrol:

```typescript
export const authMiddleware = (req, res, next) => {
  // Token doğrula
  // userId ve familyId'yi req'e ekle
}
```

Kullanım: `router.get('/', authMiddleware, controller.getAll)`

### `src/config/`

- **database.ts**: PostgreSQL connection pool
- **email.ts**: Nodemailer transporter ve sendEmail fonksiyonu

### `src/models/database.sql`
Tüm veritabanı tabloları, ilişkiler ve indexler

### `src/index.ts`
Ana sunucu dosyası:
- Express app oluştur
- Middleware'leri ekle
- Routerları bağla
- Server'ı başlat

## Frontend Dosyaları

### `src/pages/`
Tam sayfalar (React Router ile):

- **LoginPage.tsx**: Email ve giriş kodu formu

Gelecek:
- DashboardPage
- TasksPage
- InventoryPage
- ShoppingPage
- BudgetPage

### `src/store/authStore.ts`
Zustand state store:
- token, userId, familyId sakla
- localStorage'a otomatik koru
- setAuth() ve clearAuth() fonksiyonları

### `src/utils/api.ts`
Axios istemcisi ve API metodları:
```typescript
export const taskAPI = {
  create: (data) => apiClient.post('/tasks', data),
  getAll: () => apiClient.get('/tasks'),
  // ...
}
```

### `src/App.tsx`
React Router setup:
- /login → LoginPage
- /dashboard → (henüz yapılmadı)
- Redirect logic'i

### `src/index.tsx`
React uygulamasını DOM'a bağla

## Konfigürasyon Dosyaları

### Backend
- **package.json**: Dependencies ve scripts
- **tsconfig.json**: TypeScript ayarları
- **.env.example**: Ortam değişkenleri template
- **.env**: Gerçek değerler (git ignore'da)

### Frontend
- **package.json**: React dependencies
- **tsconfig.json**: TypeScript ve paths ayarları
- **public/index.html**: HTML template

## Veritabanı Tabloları

```
users ─────┬──> families
           │
           ├──> tasks
           │     ├──> task_history
           │     └──> expenses
           │
           ├──> inventory
           │
           ├──> shopping_lists ──> shopping_list_items
           │
           ├──> budgets
           │
           └──> invite_tokens
```

Her tablo family_id ile izole edilir (privacy).

## Örnek İş Akışı

```
Kullanıcı
  ↓
[LoginPage] → email gir → API: POST /auth/login-code
  ↓
Backend: Email al, 6 haneli kod oluştur, email gönder
  ↓
Kullanıcı: Kodu gir → API: POST /auth/verify-code
  ↓
Backend: Token oluştur (JWT), user bilgilerini döndür
  ↓
Frontend: Token ve familyId'yi Zustand store'a kaydet
  ↓
[Dashboard] → Görevleri göster → API: GET /tasks (header'da token)
  ↓
Backend: Middleware token kontrol → userId çek → family_id ile filtrele → tasks dön
  ↓
Frontend: Tasks renderla
```

---

Daha detaylı bilgi için bak:
- [Backend Controllers](../backend/src/controllers/)
- [Frontend Utilities](../frontend/src/utils/)
- [Database Schema](../backend/src/models/database.sql)
