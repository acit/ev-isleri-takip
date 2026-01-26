# 🚀 Production Deployment Rehberi

## 📦 Hazırlanan Dosyalar

Bu klasörde production deployment için gerekli tüm dosyalar hazırlanmıştır:

```
production/
├── dist/                 # Backend build dosyaları
├── frontend/             # Frontend build dosyaları  
├── package.json          # Backend dependencies
├── .env                  # Production environment variables
├── ecosystem.config.js   # PM2 yapılandırması
├── nginx.conf           # Nginx yapılandırması
└── DEPLOYMENT_GUIDE.md  # Bu rehber
```

## 🖥️ VPS/Dedicated Server Deployment

### 1. Sunucu Hazırlığı (Ubuntu 22.04)
```bash
# Sistem güncellemesi
sudo apt update && sudo apt upgrade -y

# Gerekli paketler
sudo apt install nginx nodejs npm -y

# PM2 kurulumu
sudo npm install -g pm2

# Uygulama klasörü
sudo mkdir -p /var/www/aile-takip
sudo chown $USER:$USER /var/www/aile-takip
```

### 2. Dosyaları Sunucuya Yükle
```bash
# Bu production klasörünün içeriğini sunucuya kopyala
scp -r ./* user@server:/var/www/aile-takip/

# Veya rsync ile
rsync -avz --delete ./ user@server:/var/www/aile-takip/
```

### 3. Sunucuda Kurulum
```bash
# Sunucuya SSH ile bağlan
ssh user@server

# Uygulama klasörüne git
cd /var/www/aile-takip

# Production dependencies yükle
npm ci --only=production

# Log klasörü oluştur
mkdir -p logs

# Database klasörü oluştur
mkdir -p data

# PM2 ile uygulamayı başlat
pm2 start ecosystem.config.js
pm2 startup
pm2 save
```

### 4. Nginx Yapılandırması
```bash
# Nginx config dosyasını kopyala
sudo cp nginx.conf /etc/nginx/sites-available/aile-takip

# Site'ı aktifleştir
sudo ln -sf /etc/nginx/sites-available/aile-takip /etc/nginx/sites-enabled/

# Default site'ı kapat (opsiyonel)
sudo rm -f /etc/nginx/sites-enabled/default

# Nginx test et
sudo nginx -t

# Nginx'i yeniden başlat
sudo systemctl reload nginx
```

### 5. SSL Certificate (Let's Encrypt)
```bash
# Certbot kurulumu
sudo apt install certbot python3-certbot-nginx -y

# SSL sertifikası al
sudo certbot --nginx -d yourdomain.com -d www.yourdomain.com

# Otomatik yenileme test et
sudo certbot renew --dry-run
```

## 🐳 Docker Deployment

### Docker Compose ile
```bash
# Docker ve Docker Compose kurulu olmalı
docker-compose up -d

# Logları kontrol et
docker-compose logs -f
```

### Tek Container ile
```bash
# Image build et
docker build -t aile-takip .

# Container çalıştır
docker run -d \
  --name aile-takip \
  -p 3000:80 \
  -p 5001:5001 \
  -v $(pwd)/data:/app/data \
  aile-takip
```

## ☁️ Cloud Platform Deployment

### Vercel (Frontend Only)
```bash
cd frontend
npm install -g vercel
vercel --prod

# Environment variables ekle:
# REACT_APP_API_URL=https://your-backend-url.com/api
```

### Railway (Backend)
```bash
npm install -g @railway/cli
railway login
railway init
railway add postgresql  # Opsiyonel
railway deploy
```

### Heroku (Full Stack)
```bash
# Heroku CLI kurulu olmalı
heroku create aile-takip-app

# PostgreSQL addon
heroku addons:create heroku-postgresql:hobby-dev

# Environment variables
heroku config:set NODE_ENV=production
heroku config:set JWT_SECRET=your_super_secure_secret
heroku config:set EMAIL_USER=your-email@gmail.com
heroku config:set EMAIL_PASSWORD=your-app-password

# Deploy
git push heroku main
```

## 🔧 Environment Variables

`.env` dosyasını production değerleriyle güncelleyin:

```env
NODE_ENV=production
PORT=5001
DATABASE_URL=sqlite:./data/database.sqlite
JWT_SECRET=your_super_secure_jwt_secret_key_here
JWT_EXPIRE=7d

# Gmail Configuration
EMAIL_SERVICE=gmail
EMAIL_USER=erhan.koksal@gmail.com
EMAIL_PASSWORD=your_real_gmail_app_password_here
EMAIL_FROM="Aile Takip Sistemi <erhan.koksal@gmail.com>"

# Frontend URL
FRONTEND_URL=https://yourdomain.com
```

### Gmail App Password Kurulumu
1. Gmail hesabınızda 2FA aktif olmalı
2. https://myaccount.google.com/apppasswords
3. "Aile Takip Sistemi" için yeni App Password oluşturun
4. 16 haneli kodu .env dosyasına ekleyin (boşluksuz)

## 📊 Monitoring ve Yönetim

### PM2 Komutları
```bash
# Status kontrol
pm2 status

# Logları görüntüle
pm2 logs aile-takip-backend

# Uygulamayı yeniden başlat
pm2 restart aile-takip-backend

# Monitoring
pm2 monit

# Process'i durdur
pm2 stop aile-takip-backend

# Process'i sil
pm2 delete aile-takip-backend
```

### Nginx Komutları
```bash
# Status kontrol
sudo systemctl status nginx

# Yeniden başlat
sudo systemctl reload nginx

# Access logs
sudo tail -f /var/log/nginx/access.log

# Error logs
sudo tail -f /var/log/nginx/error.log

# Config test
sudo nginx -t
```

### Database Yönetimi
```bash
# Database dosyası izinleri
chmod 664 data/database.sqlite
chown $USER:$USER data/database.sqlite

# Backup oluştur
cp data/database.sqlite data/backup-$(date +%Y%m%d).sqlite

# Database boyutu kontrol
ls -lh data/database.sqlite
```

## 🚨 Troubleshooting

### Backend Çalışmıyor
```bash
# PM2 logları kontrol et
pm2 logs aile-takip-backend

# Process'i yeniden başlat
pm2 restart aile-takip-backend

# Port kullanımı kontrol et
sudo netstat -tlnp | grep :5001

# Environment variables kontrol et
pm2 env 0
```

### Frontend 404 Hatası
```bash
# Nginx config test et
sudo nginx -t

# Nginx logları kontrol et
sudo tail -f /var/log/nginx/error.log

# Frontend dosyaları mevcut mu?
ls -la /var/www/aile-takip/frontend/

# Nginx'i yeniden başlat
sudo systemctl reload nginx
```

### Email Gönderilmiyor
```bash
# Backend loglarında email hatalarını kontrol et
pm2 logs aile-takip-backend | grep -i email

# Environment variables kontrol et
grep EMAIL /var/www/aile-takip/.env

# Gmail App Password test et
# https://myaccount.google.com/apppasswords
```

### Database Hatası
```bash
# Database dosyası mevcut mu?
ls -la data/database.sqlite

# İzinler doğru mu?
chmod 664 data/database.sqlite
chown $USER:$USER data/database.sqlite

# Disk alanı kontrol et
df -h
```

## 🔒 Güvenlik

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

### Backup Strategy
```bash
# Otomatik backup scripti
cat > /home/$USER/backup.sh << 'EOF'
#!/bin/bash
DATE=$(date +%Y%m%d)
BACKUP_DIR="/home/$USER/backups"
mkdir -p $BACKUP_DIR

# Database backup
cp /var/www/aile-takip/data/database.sqlite $BACKUP_DIR/db-$DATE.sqlite

# Application backup
tar -czf $BACKUP_DIR/app-$DATE.tar.gz /var/www/aile-takip

# Clean old backups (30 days)
find $BACKUP_DIR -name "*.sqlite" -mtime +30 -delete
find $BACKUP_DIR -name "*.tar.gz" -mtime +30 -delete
EOF

chmod +x /home/$USER/backup.sh

# Crontab'a ekle (günlük 2:00'da)
echo "0 2 * * * /home/$USER/backup.sh" | crontab -
```

## 📈 Performance Optimization

### Nginx Caching
```nginx
# /etc/nginx/nginx.conf içine ekleyin
http {
    proxy_cache_path /var/cache/nginx levels=1:2 keys_zone=my_cache:10m max_size=10g 
                     inactive=60m use_temp_path=off;
}
```

### PM2 Cluster Mode
```javascript
// ecosystem.config.js - Cluster mode için
module.exports = {
  apps: [{
    name: 'aile-takip-backend',
    script: './dist/index.js',
    instances: 'max', // CPU core sayısı kadar instance
    exec_mode: 'cluster',
    // ... diğer ayarlar
  }]
};
```

## 📞 Destek

### Loglar
- **PM2 Logs**: `pm2 logs aile-takip-backend`
- **Nginx Access**: `/var/log/nginx/access.log`
- **Nginx Error**: `/var/log/nginx/error.log`
- **System**: `journalctl -u nginx -f`

### Monitoring Tools
- **PM2 Monitoring**: `pm2 monit`
- **System Resources**: `htop`, `free -h`, `df -h`
- **Network**: `netstat -tlnp`

### İletişim
- **GitHub Issues**: Bug reports ve feature requests
- **Email**: erhan.koksal@gmail.com
- **Documentation**: Kapsamlı rehberler mevcut

## ✅ Deployment Checklist

### Sunucu Hazırlığı
- [ ] Ubuntu 22.04 kurulu
- [ ] Node.js 16+ kurulu
- [ ] Nginx kurulu
- [ ] PM2 kurulu
- [ ] Domain DNS ayarları yapılmış

### Uygulama Kurulumu
- [ ] Production dosyaları yüklendi
- [ ] .env dosyası düzenlendi
- [ ] Dependencies yüklendi
- [ ] PM2 ile uygulama başlatıldı
- [ ] Nginx yapılandırıldı

### Güvenlik
- [ ] SSL sertifikası kuruldu
- [ ] Firewall yapılandırıldı
- [ ] Backup stratejisi oluşturuldu
- [ ] Monitoring kuruldu

### Test
- [ ] Website erişilebilir
- [ ] API endpoints çalışıyor
- [ ] Email gönderimi test edildi
- [ ] Login/Register çalışıyor
- [ ] Tüm sayfalar yükleniyor

**🎉 Production deployment başarıyla tamamlandı!**

---
*Son güncelleme: 26 Ocak 2026*
*Versiyon: 2.0.0 Production*