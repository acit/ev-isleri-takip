# 🏠 Aile Takip Sistemi - Production Build

Bu klasör production deployment için hazırlanmış dosyaları içerir.

## 📦 İçerik

- `dist/` - Backend build dosyaları (TypeScript → JavaScript)
- `frontend/` - Frontend build dosyaları (React optimized)
- `package.json` - Backend dependencies
- `.env` - Production environment variables
- `ecosystem.config.js` - PM2 yapılandırması
- `nginx.conf` - Nginx yapılandırması
- `start.sh` - Hızlı başlatma scripti
- `DEPLOYMENT_GUIDE.md` - Detaylı deployment rehberi

## 🚀 Hızlı Başlangıç

### 1. Sunucuya Yükle
```bash
# Bu klasörün içeriğini sunucuya kopyala
scp -r ./* user@server:/var/www/aile-takip/
```

### 2. Başlat
```bash
# Sunucuda
cd /var/www/aile-takip
chmod +x start.sh
./start.sh
```

### 3. Nginx Yapılandır
```bash
sudo cp nginx.conf /etc/nginx/sites-available/aile-takip
sudo ln -s /etc/nginx/sites-available/aile-takip /etc/nginx/sites-enabled/
sudo nginx -t && sudo systemctl reload nginx
```

## ⚙️ Environment Variables

`.env` dosyasını düzenleyin:

```env
NODE_ENV=production
PORT=5001
JWT_SECRET=your_super_secure_secret
EMAIL_USER=erhan.koksal@gmail.com
EMAIL_PASSWORD=your_gmail_app_password
FRONTEND_URL=https://yourdomain.com
```

## 📊 Monitoring

```bash
# PM2 status
pm2 status

# Loglar
pm2 logs aile-takip-backend

# Restart
pm2 restart aile-takip-backend
```

## 📚 Detaylı Rehber

Kapsamlı deployment rehberi için `DEPLOYMENT_GUIDE.md` dosyasını inceleyin.

## 🆘 Destek

- **Email**: erhan.koksal@gmail.com
- **GitHub**: Issues bölümü
- **Docs**: DEPLOYMENT_GUIDE.md

---
**🎉 Production ready - Deploy etmeye hazır!**