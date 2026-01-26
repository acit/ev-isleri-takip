# 🚀 Production Deployment Rehberi

Bu rehber, Aile Takip Sistemi'ni production ortamına nasıl deploy edeceğinizi açıklar.

## 📋 Deployment Seçenekleri

### 1. 🐳 Docker ile Deployment (Önerilen)
### 2. ☁️ Cloud Platform Deployment
### 3. 🖥️ VPS/Dedicated Server Deployment

---

## 🐳 Docker Deployment

### Hızlı Başlangıç
```bash
# 1. Repository'yi klonlayın
git clone <repository-url>
cd aile-takip-sistemi

# 2. Environment dosyasını oluşturun
cp .env.example .env
# .env dosyasını production değerleriyle düzenleyin

# 3. Docker Compose ile başlatın
docker-compose up -d

# 4. Uygulamayı kontrol edin
curl http://localhost:3000
curl http://localhost:5001/api/health
```

### Environment Yapılandırması (.env)
```env
# Database
DATABASE_URL=sqlite:/app/data/database.sqlite
# Veya PostgreSQL için:
# DATABASE_URL=postgresql://user:password@postgres:5432/aile_takip

# JWT
JWT_SECRET=your_super_secure_jwt_secret_key_here
JWT_EXPIRE=7d

# Email (Gmail)
EMAIL_SERVICE=gmail
EMAIL_USER=your-email@gmail.com
EMAIL_PASSWORD=your-16-digit-app-password
EMAIL_FROM="Aile Takip Sistemi <your-email@gmail.com>"

# Frontend URL
FRONTEND_URL=https://yourdomain.com

# PostgreSQL (opsiyonel)
POSTGRES_USER=aile_user
POSTGRES_PASSWORD=secure_database_password
```

### Docker Compose Profilleri
```bash
# Sadece temel uygulama
docker-compose up -d

# PostgreSQL ile
docker-compose --profile postgres up -d

# Redis cache ile
docker-compose --profile redis up -d

# Tüm servisler
docker-compose --profile postgres --profile redis up -d
```

---

## ☁️ Cloud Platform Deployment

### Vercel (Frontend)
```bash
# 1. Vercel CLI kurulumu
npm i -g vercel

# 2. Frontend deploy
cd frontend
vercel --prod

# 3. Environment variables ayarlayın
# REACT_APP_API_URL=https://your-backend-url.com/api
```

### Railway (Backend)
```bash
# 1. Railway CLI kurulumu
npm i -g @railway/cli

# 2. Railway'e login
railway login

# 3. Proje oluştur ve deploy
railway init
railway add postgresql
railway deploy
```

### Heroku (Full Stack)
```bash
# 1. Heroku CLI kurulumu
# 2. Heroku app oluştur
heroku create aile-takip-app

# 3. PostgreSQL addon ekle
heroku addons:create heroku-postgresql:hobby-dev

# 4. Environment variables
heroku config:set JWT_SECRET=your_secret
heroku config:set EMAIL_USER=your-email@gmail.com
heroku config:set EMAIL_PASSWORD=your-app-password

# 5. Deploy
git push heroku main
```

### AWS (Advanced)
```yaml
# docker-compose.aws.yml
version: '3.8'
services:
  app:
    image: your-ecr-repo/aile-takip:latest
    environment:
      - DATABASE_URL=${RDS_URL}
      - EMAIL_USER=${SES_EMAIL}
    deploy:
      replicas: 2
      resources:
        limits:
          memory: 512M
        reservations:
          memory: 256M
```

---

## 🖥️ VPS/Dedicated Server

### Ubuntu 22.04 Kurulumu
```bash
# 1. Sistem güncellemesi
sudo apt update && sudo apt upgrade -y

# 2. Node.js kurulumu
curl -fsSL https://deb.nodesource.com/setup_18.x | sudo -E bash -
sudo apt-get install -y nodejs

# 3. PM2 kurulumu
sudo npm install -g pm2

# 4. Nginx kurulumu
sudo apt install nginx -y

# 5. SSL sertifikası (Let's Encrypt)
sudo apt install certbot python3-certbot-nginx -y
```

### Uygulama Kurulumu
```bash
# 1. Uygulama dizini
sudo mkdir -p /var/www/aile-takip
sudo chown $USER:$USER /var/www/aile-takip
cd /var/www/aile-takip

# 2. Repository klonla
git clone <repository-url> .

# 3. Dependencies yükle
npm run install:all

# 4. Environment ayarla
cp backend/.env.example backend/.env
# .env dosyasını düzenleyin

# 5. Build
npm run build

# 6. PM2 ile başlat
pm2 start backend/dist/index.js --name "aile-takip-backend"
pm2 startup
pm2 save
```

### Nginx Yapılandırması
```nginx
# /etc/nginx/sites-available/aile-takip
server {
    listen 80;
    server_name yourdomain.com www.yourdomain.com;
    
    # Frontend
    location / {
        root /var/www/aile-takip/frontend/build;
        try_files $uri $uri/ /index.html;
    }
    
    # Backend API
    location /api/ {
        proxy_pass http://localhost:5001;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_cache_bypass $http_upgrade;
    }
}
```

```bash
# Nginx yapılandırmasını aktifleştir
sudo ln -s /etc/nginx/sites-available/aile-takip /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl reload nginx

# SSL sertifikası
sudo certbot --nginx -d yourdomain.com -d www.yourdomain.com
```

---

## 🔒 Güvenlik Yapılandırması

### Firewall (UFW)
```bash
sudo ufw default deny incoming
sudo ufw default allow outgoing
sudo ufw allow ssh
sudo ufw allow 'Nginx Full'
sudo ufw enable
```

### SSL/TLS
```bash
# Let's Encrypt otomatik yenileme
sudo crontab -e
# Ekleyin: 0 12 * * * /usr/bin/certbot renew --quiet
```

### Database Güvenliği
```bash
# PostgreSQL için
sudo -u postgres psql
CREATE USER aile_user WITH PASSWORD 'secure_password';
CREATE DATABASE aile_takip OWNER aile_user;
GRANT ALL PRIVILEGES ON DATABASE aile_takip TO aile_user;
```

---

## 📊 Monitoring ve Logging

### PM2 Monitoring
```bash
# Process durumu
pm2 status

# Logları görüntüle
pm2 logs aile-takip-backend

# Restart
pm2 restart aile-takip-backend

# Memory/CPU monitoring
pm2 monit
```

### Nginx Logs
```bash
# Access logs
sudo tail -f /var/log/nginx/access.log

# Error logs
sudo tail -f /var/log/nginx/error.log
```

### Application Logs
```bash
# Backend logs
tail -f /var/www/aile-takip/backend/logs/app.log

# PM2 logs
pm2 logs --lines 100
```

---

## 🔄 CI/CD Pipeline

### GitHub Actions
```yaml
# .github/workflows/deploy.yml
name: Deploy to Production

on:
  push:
    branches: [main]

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Setup Node.js
        uses: actions/setup-node@v3
        with:
          node-version: '18'
          
      - name: Install dependencies
        run: npm run install:all
        
      - name: Run tests
        run: npm test
        
      - name: Build application
        run: npm run build
        
      - name: Deploy to server
        uses: appleboy/ssh-action@v0.1.5
        with:
          host: ${{ secrets.HOST }}
          username: ${{ secrets.USERNAME }}
          key: ${{ secrets.SSH_KEY }}
          script: |
            cd /var/www/aile-takip
            git pull origin main
            npm run install:all
            npm run build
            pm2 restart aile-takip-backend
```

---

## 🚨 Troubleshooting

### Yaygın Sorunlar

#### 1. Email Gönderilmiyor
```bash
# Gmail App Password kontrol
# .env dosyasında EMAIL_PASSWORD doğru mu?
# 2FA aktif mi?
```

#### 2. Database Bağlantı Hatası
```bash
# SQLite dosya izinleri
sudo chown -R $USER:$USER /var/www/aile-takip/backend/
chmod 664 /var/www/aile-takip/backend/database.sqlite
```

#### 3. Frontend 404 Hatası
```bash
# Nginx yapılandırması kontrol
sudo nginx -t
# Build dosyaları mevcut mu?
ls -la /var/www/aile-takip/frontend/build/
```

#### 4. API CORS Hatası
```javascript
// backend/src/index.ts
app.use(cors({
  origin: process.env.FRONTEND_URL || 'http://localhost:3000',
  credentials: true
}));
```

### Log Analizi
```bash
# Backend error logs
grep -i error /var/log/pm2/aile-takip-backend-error.log

# Nginx error logs
grep -i error /var/log/nginx/error.log

# System logs
journalctl -u nginx -f
```

---

## 📈 Performance Optimization

### Database Optimization
```sql
-- PostgreSQL için indexler
CREATE INDEX idx_tasks_user_id ON tasks(user_id);
CREATE INDEX idx_tasks_status ON tasks(status);
CREATE INDEX idx_inventory_category ON inventory(category);
```

### Nginx Caching
```nginx
# Static file caching
location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg)$ {
    expires 1y;
    add_header Cache-Control "public, immutable";
}

# API response caching
location /api/inventory {
    proxy_cache my_cache;
    proxy_cache_valid 200 5m;
    proxy_pass http://backend;
}
```

### CDN Integration
```javascript
// Frontend build için CDN
// package.json
{
  "homepage": "https://cdn.yourdomain.com"
}
```

---

## 🔄 Backup Strategy

### Database Backup
```bash
# SQLite backup
cp /var/www/aile-takip/backend/database.sqlite /backup/db-$(date +%Y%m%d).sqlite

# PostgreSQL backup
pg_dump -h localhost -U aile_user aile_takip > /backup/db-$(date +%Y%m%d).sql
```

### Automated Backup
```bash
# Crontab ekle
0 2 * * * /home/user/backup-script.sh
```

```bash
#!/bin/bash
# backup-script.sh
BACKUP_DIR="/backup"
DATE=$(date +%Y%m%d)

# Database backup
cp /var/www/aile-takip/backend/database.sqlite $BACKUP_DIR/db-$DATE.sqlite

# Application backup
tar -czf $BACKUP_DIR/app-$DATE.tar.gz /var/www/aile-takip

# Clean old backups (30 days)
find $BACKUP_DIR -name "*.sqlite" -mtime +30 -delete
find $BACKUP_DIR -name "*.tar.gz" -mtime +30 -delete
```

---

## 📞 Production Support

### Health Checks
```bash
# Application health
curl -f http://localhost:5001/api/health

# Database connection
curl -f http://localhost:5001/api/auth/email-status
```

### Monitoring Tools
- **Uptime monitoring**: UptimeRobot, Pingdom
- **Error tracking**: Sentry
- **Performance**: New Relic, DataDog
- **Logs**: ELK Stack, Grafana

### Maintenance Mode
```nginx
# Maintenance page
if (-f /var/www/maintenance.html) {
    return 503;
}

error_page 503 @maintenance;
location @maintenance {
    root /var/www/;
    rewrite ^(.*)$ /maintenance.html break;
}
```

---

**🎉 Production deployment tamamlandı!**

Herhangi bir sorun yaşarsanız:
- **GitHub Issues**: Bug reports
- **Email**: erhan.koksal@gmail.com
- **Documentation**: Detaylı rehberler

*Son güncelleme: 26 Ocak 2026*