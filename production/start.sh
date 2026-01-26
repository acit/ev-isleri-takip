#!/bin/bash

# Aile Takip Sistemi - Production Start Script
echo "🚀 Aile Takip Sistemi başlatılıyor..."

# Log klasörü oluştur
mkdir -p logs
mkdir -p data

# Environment variables kontrol et
if [ ! -f .env ]; then
    echo "❌ .env dosyası bulunamadı!"
    echo "📋 .env.example dosyasını kopyalayıp düzenleyin"
    exit 1
fi

# Node.js kontrol et
if ! command -v node &> /dev/null; then
    echo "❌ Node.js bulunamadı!"
    echo "📋 Node.js 16+ yükleyin"
    exit 1
fi

# PM2 kontrol et
if ! command -v pm2 &> /dev/null; then
    echo "📦 PM2 yükleniyor..."
    npm install -g pm2
fi

# Dependencies kontrol et
if [ ! -d node_modules ]; then
    echo "📦 Dependencies yükleniyor..."
    npm ci --only=production
fi

# PM2 ile başlat
echo "🔄 PM2 ile uygulama başlatılıyor..."
pm2 start ecosystem.config.js

# Status göster
pm2 status

echo ""
echo "✅ Aile Takip Sistemi başarıyla başlatıldı!"
echo "🌐 Backend: http://localhost:5001"
echo "📊 PM2 Status: pm2 status"
echo "📝 Logs: pm2 logs aile-takip-backend"
echo "🔄 Restart: pm2 restart aile-takip-backend"
echo ""