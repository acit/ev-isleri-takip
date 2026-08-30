#!/bin/bash
# ============================================
# Aile Takip — iOS IPA Export Script
# App Store olmadan kişisel dağıtım için
# ============================================

set -e

# Renkli çıktılar
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}============================================${NC}"
echo -e "${BLUE}  Aile Takip — iOS IPA Export${NC}"
echo -e "${BLUE}============================================${NC}"
echo ""

# 1. Archive oluştur
echo -e "${YELLOW}📦 1/4: Archive oluşturuluyor...${NC}"
xcodebuild archive \
    -project AileTakip.xcodeproj \
    -scheme AileTakip \
    -archivePath build/AileTakip.xcarchive \
    -destination "generic/platform=iOS" \
    -configuration Release \
    CODE_SIGN_IDENTITY="" \
    CODE_SIGNING_REQUIRED=NO \
    CODE_SIGNING_ALLOWED=NO

if [ $? -ne 0 ]; then
    echo -e "${RED}❌ Archive oluşturulamadı!${NC}"
    exit 1
fi
echo -e "${GREEN}✅ Archive oluşturuldu${NC}"
echo ""

# 2. Export options plist oluştur
echo -e "${YELLOW}📄 2/4: Export options oluşturuluyor...${NC}"
cat > build/ExportOptions.plist << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>method</key>
    <string>ad-hoc</string>
    <key>stripSwiftSymbols</key>
    <true/>
    <key>compileBitcode</key>
    <false/>
    <key>signingStyle</key>
    <string>automatic</string>
</dict>
</plist>
EOF
echo -e "${GREEN}✅ Export options oluşturuldu${NC}"
echo ""

# 3. IPA export
echo -e "${YELLOW}📱 3/4: IPA export ediliyor...${NC}"
xcodebuild -exportArchive \
    -archivePath build/AileTakip.xcarchive \
    -exportOptionsPlist build/ExportOptions.plist \
    -exportPath build/ipa

if [ $? -ne 0 ]; then
    echo -e "${RED}❌ Export başarısız!${NC}"
    exit 1
fi
echo -e "${GREEN}✅ IPA export edildi${NC}"
echo ""

# 4. IPA dosyasını bul ve kopyala
echo -e "${YELLOW}🔍 4/4: IPA dosyası aranıyor...${NC}"
IPA_FILE=$(find build/ipa -name "*.ipa" | head -1)

if [ -z "$IPA_FILE" ]; then
    echo -e "${RED}❌ IPA dosyası bulunamadı!${NC}"
    exit 1
fi

# Root dizine kopyala
cp "$IPA_FILE" ../AileTakip-v1.0.0.ipa
echo -e "${GREEN}✅ IPA kopyalandı: AileTakip-v1.0.0.ipa${NC}"
echo ""

# Boyutu göster
IPA_SIZE=$(du -h ../AileTakip-v1.0.0.ipa | cut -f1)
echo -e "${BLUE}============================================${NC}"
echo -e "${GREEN}🎉 BAŞARILI!${NC}"
echo -e "${BLUE}============================================${NC}"
echo -e "📱 IPA: AileTakip-v1.0.0.ipa ($IPA_SIZE)"
echo ""
echo -e "${YELLOW}📌 Şimdi:${NC}"
echo -e "1. GitHub'da Release oluşturun"
echo -e "2. IPA dosyasını yükleyin"
echo -e "3. Kullanıcılara AltStore talimatlarını gönderin"
echo -e ""
echo -e "${BLUE}Detaylı rehber: ios/DISTRIBUTION-GUIDE.md${NC}"
