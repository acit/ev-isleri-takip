#!/usr/bin/env bash
# ============================================================
# Aile Takip — Sürüm Otomasyonu (make-release.sh)
#
# Tek komutla:
#   1. Sürüm numarasını büyüt (major|minor|patch|<x.y.z>)
#   2. Tüm sürüm kaynaklarını güncelle:
#      - app/build.gradle.kts        (versionName + versionCode+1)
#      - ios/.../project.pbxproj     (MARKETING_VERSION + CURRENT_PROJECT_VERSION)
#      - CHANGELOG.md                (yeni sürüm başlığı, Unreleased yoksa bugünden)
#   3. Ön kontroller: çalışma dizini temiz mi, main'de miyiz, testler geçiyor mu (--skip-tests ile atla)
#   4. Commit + tag (vX.Y.Z) + push  (--dry-run ile sadece göster)
#   5. GitHub Actions Release workflow'unu izle ve Release asset'lerini doğrula
#
# Kullanım:
#   ./scripts/make-release.sh patch               # 3.7.4 → 3.7.5
#   ./scripts/make-release.sh minor               # 3.7.4 → 3.8.0
#   ./scripts/make-release.sh major               # 3.7.4 → 4.0.0
#   ./scripts/make-release.sh 3.8.1               # doğrudan sürüm yaz
#   ./scripts/make-release.sh patch --dry-run     # hiçbir şeyi değiştirme
#   ./scripts/make-release.sh patch --skip-tests  # gradle test atla (yavaşsa)
#
# Gereksinimler: git, python (yaml doğrulama + pbxproj güncelleme), curl.
# ============================================================
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

# ---------------- Renkler ----------------
RED=$'\033[0;31m'; GREEN=$'\033[0;32m'; YELLOW=$'\033[1;33m'; BLUE=$'\033[0;34m'; NC=$'\033[0m'
info()  { echo "${BLUE}ℹ${NC}  $*"; }
ok()    { echo "${GREEN}✓${NC}  $*"; }
warn()  { echo "${YELLOW}⚠${NC}  $*"; }
die()   { echo "${RED}✗${NC}  $*" >&2; exit 1; }

# ---------------- Argümanlar ----------------
BUMP="${1:-}"
DRY_RUN=false
SKIP_TESTS=false
for arg in "${@:2}"; do
  case "$arg" in
    --dry-run)    DRY_RUN=true ;;
    --skip-tests) SKIP_TESTS=true ;;
    *) die "Bilinmeyen seçenek: $arg (kullanım: patch|minor|major|<x.y.z> [--dry-run] [--skip-tests])" ;;
  esac
done
[ -n "$BUMP" ] || die "Sürüm argümanı gerekli: patch|minor|major|<x.y.z>"

GRADLE_FILE="app/build.gradle.kts"
PBXPROJ_FILE="ios/AileTakip.xcodeproj/project.pbxproj"
CHANGELOG_FILE="CHANGELOG.md"

# ---------------- 0. Ön kontroller ----------------
info "Ön kontroller..."
BRANCH="$(git rev-parse --abbrev-ref HEAD)"
[ "$BRANCH" = "main" ] || die "main dalında değilsin (şu an: $BRANCH). Geçiş yap: git checkout main"
git diff --quiet && git diff --cached --quiet || die "Commit edilmemiş değişiklikler var. Önce temizle."
git fetch origin --quiet
LOCAL="$(git rev-parse HEAD)"
REMOTE="$(git rev-parse origin/main 2>/dev/null || echo "$LOCAL")"
[ "$LOCAL" = "$REMOTE" ] || die "main, origin/main ile senkron değil. git pull / push yap."
ok "Dizin temiz, main güncel ($LOCAL)"

# ---------------- 1. Mevcut sürümü oku ----------------
CURRENT_VERSION="$(grep -m1 'versionName' "$GRADLE_FILE" | sed 's/.*"\(.*\)".*/\1/')"
CURRENT_CODE="$(grep -m1 'versionCode' "$GRADLE_FILE" | sed 's/.*= \([0-9]*\).*/\1/')"
info "Mevcut sürüm: $CURRENT_VERSION (code $CURRENT_CODE)"

# ---------------- 2. Yeni sürümü hesapla ----------------
if [[ "$BUMP" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
  NEW_VERSION="$BUMP"
else
  IFS='.' read -r MAJOR MINOR PATCH <<< "$CURRENT_VERSION"
  case "$BUMP" in
    major) NEW_VERSION="$((MAJOR + 1)).0.0" ;;
    minor) NEW_VERSION="$MAJOR.$((MINOR + 1)).0" ;;
    patch) NEW_VERSION="$MAJOR.$MINOR.$((PATCH + 1))" ;;
    *) die "Geçersiz sürüm: $BUMP" ;;
  esac
fi
NEW_CODE=$((CURRENT_CODE + 1))
TAG="v$NEW_VERSION"
[ "$NEW_VERSION" != "$CURRENT_VERSION" ] || die "Sürüm değişmedi ($CURRENT_VERSION). Farklı bir bump ver."
if git rev-parse -q --verify "refs/tags/$TAG" >/dev/null; then
  die "$TAG etiketi zaten var. Farklı bir sürüm seç veya etiketi sil."
fi
ok "Hedef sürüm: $NEW_VERSION (code $NEW_CODE, etiket $TAG)"

if $DRY_RUN; then
  warn "DRY-RUN: hiçbir dosya değiştirilmedi, commit/tag/push yapılmadı."
  echo "  Yapılacaklar:"
  echo "   - $GRADLE_FILE     → versionName \"$NEW_VERSION\", versionCode $NEW_CODE"
  echo "   - $PBXPROJ_FILE    → MARKETING_VERSION $NEW_VERSION, CURRENT_PROJECT_VERSION $NEW_CODE"
  echo "   - $CHANGELOG_FILE  → ## [$NEW_VERSION] - $(date +%Y-%m-%d) başlığı"
  echo "   - git commit \"Release $NEW_VERSION (code $NEW_CODE)\" + tag $TAG + push"
  echo "   - GitHub Actions Release run izle → APK/IPA asset doğrula"
  exit 0
fi

# ---------------- 3. Testler (--skip-tests değilse) ----------------
if ! $SKIP_TESTS; then
  info "Gradle testleri çalışıyor (atlamak için --skip-tests)..."
  ./gradlew :app:testDebugUnitTest --console=plain -q || die "Testler başarısız — release iptal."
  ok "Birim testleri yeşil"
else
  warn "Testler atlandı (--skip-tests)"
fi

# ---------------- 4. Sürüm dosyalarını güncelle ----------------
info "Sürüm dosyaları güncelleniyor..."

python - "$NEW_VERSION" "$NEW_CODE" <<'PYEOF'
import io, re, sys
sys.stdout.reconfigure(encoding='utf-8', errors='replace')
version, code = sys.argv[1], sys.argv[2]

# --- app/build.gradle.kts ---
p = 'app/build.gradle.kts'
s = io.open(p, encoding='utf-8').read()
s2, n1 = re.subn(r'(versionCode = )\d+', r'\g<1>' + code, s, count=1)
s2, n2 = re.subn(r'(versionName = )"[^"]+"', r'\g<1>"' + version + '"', s2, count=1)
assert n1 == 1 and n2 == 1, 'gradle güncellenemedi'
io.open(p, 'w', encoding='utf-8', newline='\n').write(s2)

# --- pbxproj: her iki config'de de güncelle ---
p = 'ios/AileTakip.xcodeproj/project.pbxproj'
s = io.open(p, encoding='utf-8').read()
s2, n1 = re.subn(r'(MARKETING_VERSION = )[^;]+;', r'\g<1>' + version + ';', s)
s2, n2 = re.subn(r'(CURRENT_PROJECT_VERSION = )\d+;', r'\g<1>' + code + ';', s2)
assert n1 >= 2 and n2 >= 2, f'pbxproj güncellenemedi ({n1}/{n2})'
io.open(p, 'w', encoding='utf-8', newline='\n').write(s2)
print(f'  gradle + pbxproj → {version} (code {code})')
PYEOF

# --- CHANGELOG.md: yeni başlık ekle (mevcut en üst sürümün üstüne) ---
TODAY="$(date +%Y-%m-%d)"
python - "$NEW_VERSION" "$TODAY" <<'PYEOF'
import io, sys
sys.stdout.reconfigure(encoding='utf-8', errors='replace')
version, today = sys.argv[1], sys.argv[2]
p = 'CHANGELOG.md'
s = io.open(p, encoding='utf-8').read()
header = f'## [{version}] - {today}'
if header in s:
    print('  CHANGELOG başlığı zaten var')
else:
    anchor = '---\n\n## ['
    idx = s.find(anchor)
    assert idx != -1, 'CHANGELOG yapısı beklenmedik'
    insert_at = s.find('## [', idx)
    block = f'{header}\n\n### Değişiklikler\n- (Buraya sürüm notlarını yazın)\n\n'
    s = s[:insert_at] + block + s[insert_at:]
    io.open(p, 'w', encoding='utf-8', newline='\n').write(s)
    print(f'  CHANGELOG → {header} (notları doldurun!)')
PYEOF
ok "Sürüm dosyaları güncellendi"

# ---------------- 5. Commit + tag + push ----------------
info "Commit + tag + push..."
git add "$GRADLE_FILE" "$PBXPROJ_FILE" "$CHANGELOG_FILE"
git commit -m "$(cat <<EOF
Release $NEW_VERSION (code $NEW_CODE)

- versionName/versionCode + MARKETING_VERSION/CURRENT_PROJECT_VERSION
- CHANGELOG başlığı eklendi (notları doldurunuz)
EOF
)"
git push origin main
git tag -a "$TAG" -m "Aile Takip $NEW_VERSION"
git push origin "$TAG"
ok "Push edildi: main + $TAG"

# ---------------- 6. Release workflow'unu izle ----------------
info "GitHub Actions Release run bekleniyor..."
sleep 20
RUN_ID="$(curl -s "https://api.github.com/repos/$(git remote get-url origin | sed 's/.*github.com[:/]//;s/\.git$//')/actions/runs?event=push&per_page=1" | grep -m1 '"id"' | grep -o '[0-9]*')"
[ -n "$RUN_ID" ] || die "Release run bulunamadı (Actions sekmesinden kontrol edin)"
echo "  Run: https://github.com/$(git remote get-url origin | sed 's/.*github.com[:/]//;s/\.git$//')/actions/runs/$RUN_ID"

TIMEOUT=900; ELAPSED=0
while [ $ELAPSED -lt $TIMEOUT ]; do
  sleep 30; ELAPSED=$((ELAPSED + 30))
  STATUS="$(curl -s "https://api.github.com/repos/$(git remote get-url origin | sed 's/.*github.com[:/]//;s/\.git$//')/actions/runs/$RUN_ID" | grep -m1 '"status"' | sed 's/.*: "\([^"]*\)".*/\1/')"
  CONCLUSION="$(curl -s "https://api.github.com/repos/$(git remote get-url origin | sed 's/.*github.com[:/]//;s/\.git$//')/actions/runs/$RUN_ID" | grep -m1 '"conclusion"' | sed 's/.*: "\([^"]*\)".*/\1/')"
  echo "  [$ELAPSED s] status=$STATUS conclusion=$CONCLUSION"
  [ "$STATUS" = "completed" ] && break
done

if [ "$CONCLUSION" = "success" ]; then
  ok "Release workflow başarılı!"
else
  die "Release workflow $CONCLUSION — ios-build-logs artifact'ini indirip inceleyin."
fi

# ---------------- 7. Release asset'lerini doğrula ----------------
info "Release asset'leri doğrulanıyor..."
REPO="$(git remote get-url origin | sed 's/.*github.com[:/]//;s/\.git$//')"
sleep 10
python - "$REPO" "$TAG" <<'PYEOF'
import json, io, sys, urllib.request
sys.stdout.reconfigure(encoding='utf-8', errors='replace')
repo, tag = sys.argv[1], sys.argv[2]
with urllib.request.urlopen(f'https://api.github.com/repos/{repo}/releases/tags/{tag}') as r:
    rel = json.load(r)
assets = rel.get('assets', [])
print(f"  Release: {rel.get('name')} | {rel.get('html_url')}")
for a in assets:
    print(f"   asset: {a['name']}  {a['size']/1024/1024:.2f} MB  [{a['state']}]")
names = {a['name'] for a in assets}
expected = [f'AileTakip-{tag}-release.apk', f'AileTakip-{tag}-debug.apk', f'AileTakip-{tag}.ipa']
missing = [e for e in expected if not any(e in n for n in names)]
if missing:
    print(f"  ⚠ Eksik asset'ler: {missing}")
    sys.exit(2)
print('  ✓ Tüm beklenen asset\'ler mevcut')
PYEOF

echo ""
echo "${GREEN}══════════════════════════════════════${NC}"
echo "${GREEN}🎉 $TAG yayında!${NC}"
echo "  https://github.com/$REPO/releases/tag/$TAG"
echo "${GREEN}══════════════════════════════════════${NC}"
