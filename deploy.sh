#!/bin/bash

# Aile Takip Sistemi - Production Deployment Script
# Bu script sistemi production ortamına deploy eder

echo "🚀 Aile Takip Sistemi - Production Deployment"
echo "=============================================="

# Renk kodları
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Hata durumunda çık
set -e

# Fonksiyonlar
print_step() {
    echo -e "${BLUE}📋 $1${NC}"
}

print_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠️ $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

# 1. Gereksinimler kontrolü
print_step "Gereksinimler kontrol ediliyor..."

if ! command -v node &> /dev/null; then
    print_error "Node.js bulunamadı. Lütfen Node.js 16+ yükleyin."
    exit 1
fi

if ! command -v npm &> /dev/null; then
    print_error "npm bulunamadı. Lütfen npm yükleyin."
    exit 1
fi

NODE_VERSION=$(node --version | cut -d'v' -f2 | cut -d'.' -f1)
if [ "$NODE_VERSION" -lt 16 ]; then
    print_error "Node.js 16+ gerekli. Mevcut versiyon: $(node --version)"
    exit 1
fi

print_success "Node.js $(node --version) ve npm $(npm --version) hazır"

# 2. Dependencies yükleme
print_step "Dependencies yükleniyor..."

echo "Backend dependencies..."
cd backend
npm ci --only=production
print_success "Backend dependencies yüklendi"

echo "Frontend dependencies..."
cd ../frontend
npm ci --only=production
print_success "Frontend dependencies yüklendi"

cd ..

# 3. TypeScript build
print_step "Backend TypeScript build..."
cd backend
npm run build
print_success "Backend build tamamlandı"

# 4. React build
print_step "Frontend React build..."
cd ../frontend
npm run build
print_success "Frontend build tamamlandı"

cd ..

# 5. Production dosyalarını hazırla
print_step "Production dosyaları hazırlanıyor..."

# Build klasörü oluştur
mkdir -p production

# Backend dosyalarını kopyala
cp -r backend/dist production/
cp -r backend/node_modules production/
cp backend/package.json production/
cp backend/.env.production production/.env

# Frontend build'ini kopyala
cp -r frontend/build production/frontend

# Database klasörü oluştur
mkdir -p production/data

print_success "Production dosyaları hazırlandı"

# 6. PM2 ecosystem dosyası oluştur
print_step "PM2 yapılandırması oluşturuluyor..."

cat > production/ecosystem.config.js << 'EOF'
module.exports = {
  apps: [{
    name: 'aile-takip-backend',
    script: './dist/index.js',
    cwd: '/var/www/aile-takip/production',
    instances: 1,
    exec_mode: 'fork',
    env: {
      NODE_ENV: 'production',
      PORT: 5001
    },
    error_file: './logs/err.log',
    out_file: './logs/out.log',
    log_file: './logs/combined.log',
    time: true,
    max_memory_restart: '500M',
    node_args: '--max-old-space-size=512'
  }]
};
EOF

print_success "PM2 yapılandırması oluşturuldu"

# 7. Nginx yapılandırması oluştur
print_step "Nginx yapılandırması oluşturuluyor..."

cat > production/nginx.conf << 'EOF'
server {
    listen 80;
    server_name yourdomain.com www.yourdomain.com;
    root /var/www/aile-takip/production/frontend;
    index index.html;

    # Security headers
    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-XSS-Protection "1; mode=block" always;

    # Gzip compression
    gzip on;
    gzip_types text/plain text/css application/json application/javascript text/xml application/xml application/xml+rss text/javascript;

    # API proxy
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

    # Static files
    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff|woff2|ttf|eot)$ {
        expires 1y;
        add_header Cache-Control "public, immutable";
    }

    # React app
    location / {
        try_files $uri $uri/ /index.html;
    }

    # Health check
    location /health {
        return 200 "healthy\n";
        add_header Content-Type text/plain;
    }
}
EOF

print_success "Nginx yapılandırması oluşturuldu"

# 8. Deployment scripti oluştur
print_step "Server deployment scripti oluşturuluyor..."

cat > production/deploy-to-server.sh << 'EOF'
#!/bin/bash

# Server deployment script
# Bu script dosyaları sunucuya yükler ve servisleri başlatır

SERVER_USER="your_username"
SERVER_HOST="your_server_ip"
SERVER_PATH="/var/www/aile-takip"

echo "🚀 Sunucuya deployment başlıyor..."

# Dosyaları sunucuya kopyala
echo "📁 Dosyalar kopyalanıyor..."
rsync -avz --delete ./ $SERVER_USER@$SERVER_HOST:$SERVER_PATH/

# Sunucuda komutları çalıştır
echo "🔧 Sunucuda servisler yapılandırılıyor..."
ssh $SERVER_USER@$SERVER_HOST << 'ENDSSH'
cd /var/www/aile-takip

# Log klasörü oluştur
mkdir -p logs

# PM2 ile uygulamayı başlat
pm2 start ecosystem.config.js
pm2 save

# Nginx yapılandırmasını kopyala
sudo cp nginx.conf /etc/nginx/sites-available/aile-takip
sudo ln -sf /etc/nginx/sites-available/aile-takip /etc/nginx/sites-enabled/
sudo nginx -t && sudo systemctl reload nginx

echo "✅ Deployment tamamlandı!"
echo "🌐 Site: http://yourdomain.com"
echo "📊 PM2 Status: pm2 status"
echo "📝 Logs: pm2 logs aile-takip-backend"
ENDSSH

echo "🎉 Deployment başarıyla tamamlandı!"
EOF

chmod +x production/deploy-to-server.sh

print_success "Server deployment scripti oluşturuldu"

# 9. Docker dosyaları kopyala
print_step "Docker dosyaları kopyalanıyor..."
cp Dockerfile production/
cp docker-compose.yml production/
cp nginx.conf production/docker-nginx.conf

# 10. Deployment rehberi oluştur
print_step "Deployment rehberi oluşturuluyor..."

cat > production/DEPLOYMENT_GUIDE.md << 'EOF'
# 🚀 Production Deployment Rehberi

## 📦 Hazırlanan Dosyalar

Bu klasörde production deployment için gerekli tüm dosyalar hazırlanmıştır:

- `dist/` - Backend build dosyaları
- `frontend/` - Frontend build dosyaları
- `node_modules/` - Production dependencies
- `ecosystem.config.js` - PM2 yapılandırması
- `nginx.conf` - Nginx yapılandırması
- `.env` - Production environment variables

## 🖥️ VPS/Dedicated Server Deployment

### 1. Sunucu Hazırlığı
```bash
# Ubuntu 22.04 için
sudo apt update && sudo apt upgrade -y
sudo apt install nginx nodejs npm -y
sudo npm install -g pm2

# Uygulama klasörü oluştur
sudo mkdir -p /var/www/aile-takip
sudo chown $USER:$USER /var/www/aile-takip
```

### 2. Dosyaları Yükle
```bash
# Bu klasörün içeriğini sunucuya kopyala
scp -r ./* user@server:/var/www/aile-takip/
```

### 3. Servisleri Başlat
```bash
cd /var/www/aile-takip
pm2 start ecosystem.config.js
pm2 startup
pm2 save

# Nginx yapılandır
sudo cp nginx.conf /etc/nginx/sites-available/aile-takip
sudo ln -s /etc/nginx/sites-available/aile-takip /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl reload nginx
```

## 🐳 Docker Deployment

```bash
# Docker ile çalıştır
docker-compose up -d

# Veya tek container
docker build -t aile-takip .
docker run -d -p 3000:80 -p 5001:5001 aile-takip
```

## ☁️ Cloud Platform Deployment

### Vercel (Frontend)
```bash
cd frontend
vercel --prod
```

### Railway (Backend)
```bash
railway login
railway init
railway deploy
```

### Heroku (Full Stack)
```bash
heroku create aile-takip-app
heroku addons:create heroku-postgresql:hobby-dev
git push heroku main
```

## 🔧 Environment Variables

Production ortamında `.env` dosyasını düzenleyin:

```env
NODE_ENV=production
PORT=5001
DATABASE_URL=sqlite:./data/database.sqlite
JWT_SECRET=your_super_secure_jwt_secret
EMAIL_USER=your-email@gmail.com
EMAIL_PASSWORD=your-gmail-app-password
FRONTEND_URL=https://yourdomain.com
```

## 📊 Monitoring

```bash
# PM2 status
pm2 status
pm2 logs aile-takip-backend
pm2 monit

# Nginx logs
sudo tail -f /var/log/nginx/access.log
sudo tail -f /var/log/nginx/error.log
```

## 🔒 SSL Certificate

```bash
# Let's Encrypt
sudo apt install certbot python3-certbot-nginx
sudo certbot --nginx -d yourdomain.com
```

## 🚨 Troubleshooting

### Backend çalışmıyor
```bash
pm2 restart aile-takip-backend
pm2 logs aile-takip-backend
```

### Frontend 404 hatası
```bash
sudo nginx -t
sudo systemctl reload nginx
```

### Database hatası
```bash
# Dosya izinleri
chmod 664 data/database.sqlite
chown $USER:$USER data/database.sqlite
```

## 📞 Destek

- GitHub Issues: Bug reports
- Email: erhan.koksal@gmail.com
- Documentation: Kapsamlı rehberler

**🎉 Production deployment başarıyla tamamlandı!**
EOF

print_success "Deployment rehberi oluşturuldu"

# 11. Özet
echo ""
echo "🎉 Production Build Tamamlandı!"
echo "================================"
echo ""
print_success "✅ Backend build: backend/dist/"
print_success "✅ Frontend build: frontend/build/"
print_success "✅ Production files: production/"
echo ""
echo "📁 Production klasörü içeriği:"
echo "   - dist/ (Backend build)"
echo "   - frontend/ (Frontend build)"
echo "   - node_modules/ (Dependencies)"
echo "   - ecosystem.config.js (PM2 config)"
echo "   - nginx.conf (Nginx config)"
echo "   - .env (Environment variables)"
echo "   - DEPLOYMENT_GUIDE.md (Rehber)"
echo ""
echo "🚀 Deployment seçenekleri:"
echo "   1. VPS/Dedicated Server: production/deploy-to-server.sh"
echo "   2. Docker: docker-compose up -d"
echo "   3. Cloud Platform: Vercel, Railway, Heroku"
echo ""
print_warning "⚠️ .env dosyasındaki EMAIL_PASSWORD'u gerçek App Password ile değiştirin"
print_warning "⚠️ nginx.conf'daki yourdomain.com'u gerçek domain ile değiştirin"
echo ""
print_success "🎯 Sistem production deployment için hazır!"