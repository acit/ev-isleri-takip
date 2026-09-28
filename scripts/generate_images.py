# -*- coding: utf-8 -*-
"""
Aile Takip - gorsel uretici (Pillow).

Uretir:
  - Uygulama ikonu PNG'leri (mipmap-* tum yogunluklar + round)
  - Play Store ikonu (512x512)
  - Feature graphic (1024x500)
  - OG / paylasim gorseli (1200x630)
  - Tanitim posteri (1080x1350)
  - Onizleme galerisi (preview.html)

Kullanim:  python scripts/generate_images.py
"""
from PIL import Image, ImageDraw, ImageFont, ImageFilter
import os

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "art")
os.makedirs(OUT, exist_ok=True)

# ---------------- Marka renkleri ----------------
BG_TOP = (13, 71, 161)      # #0D47A1
BG_BOT = (21, 101, 192)     # #1565C0
RING = (25, 118, 210)       # #1976D2
ACCENT = (255, 112, 67)     # #FF7043 (catı)
DOOR = (66, 165, 245)       # #42A5F5
WINDOW = (41, 182, 246)     # #29B6F6
HEART = (239, 83, 80)       # #EF5350
GRASS = (102, 187, 106)     # #66BB6A
WHITE = (255, 255, 255)
CREAM = (232, 234, 246)
SKIN = (255, 204, 128)
FATHER = (30, 136, 229)
MOTHER = (233, 30, 99)
CHILD = (255, 167, 38)
SUN = (255, 241, 118)
TEXT_DARK = (13, 40, 90)


def _font(size, bold=True):
    for name in (["arialbd.ttf", "arial.ttf"] if bold else ["arial.ttf"]):
        try:
            return ImageFont.truetype(name, size)
        except OSError:
            continue
    try:
        return ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans%s.ttf"
                                  % ("-Bold" if bold else ""), size)
    except OSError:
        return ImageFont.load_default()


def _vgrad(size, top, bottom):
    w, h = size
    im = Image.new("RGB", size, top)
    px = im.load()
    for y in range(h):
        t = y / max(1, h - 1)
        c = tuple(int(top[i] + (bottom[i] - top[i]) * t) for i in range(3))
        for x in range(w):
            px[x, y] = c
    return im


def _aa_ellipse(draw, box, fill):
    """4x supersampled yuvarlak cizimi."""
    x0, y0, x1, y1 = box
    draw.ellipse(box, fill=fill)


def draw_house_scene(scale=1.0, size=(512, 512)):
    """Icon icin ev + aile + kalp sahnesi (vektor ikonun PNG uyarlamasi)."""
    W, H = size
    im = _vgrad(size, BG_TOP, BG_BOT)
    d = ImageDraw.Draw(im, "RGBA")

    # dekor halkalar
    cx, cy = W / 2, H / 2
    for r, alpha, wd in ((0.42 * W, 60, max(2, int(0.006 * W))),
                         (0.30 * W, 44, max(2, int(0.005 * W)))):
        d.ellipse([cx - r, cy - r, cx + r, cy + r], outline=(66, 165, 245, alpha), width=wd)

    S = W / 512.0 * scale

    # ---- Zemin ----
    gy = 76 / 108 * H
    d.rounded_rectangle([0.13 * W, gy, 0.87 * W, gy + 0.03 * H], radius=0.015 * H, fill=GRASS)

    # ---- Ev govdesi ----
    hx0, hy0, hx1, hy1 = 0.296 * W, 0.463 * H, 0.704 * W, 0.704 * H
    d.rectangle([hx0, hy0, hx1, hy1], fill=WHITE)
    d.polygon([(0.26 * W, 0.481 * H), (0.5 * W, 0.296 * H), (0.74 * W, 0.481 * H)], fill=ACCENT)
    d.polygon([(0.278 * W, 0.477 * H), (0.5 * W, 0.306 * H), (0.722 * W, 0.477 * H)], fill=(255, 138, 101))

    # baca
    d.rectangle([0.63 * W, 0.333 * H, 0.685 * W, 0.407 * H], fill=(229, 115, 115))

    # kapi
    d.rounded_rectangle([0.45 * W, 0.60 * H, 0.55 * W, hy1], radius=0.04 * W, fill=DOOR)
    d.ellipse([0.523 * W, 0.648 * H, 0.541 * W, 0.674 * H], fill=(255, 213, 79))

    # pencereler
    for wx in (0.315, 0.594):
        x0, y0, x1, y1 = wx * W, 0.5 * H, (wx + 0.098) * W, 0.593 * H
        d.rectangle([x0, y0, x1, y1], fill=WINDOW)
        d.rectangle([x0 + 0.008 * W, y0 + 0.008 * H, x1 - 0.008 * W, y1 - 0.008 * H], fill=(179, 229, 252))
        d.line([x0, (y0 + y1) / 2, x1, (y0 + y1) / 2], fill=WHITE, width=max(1, int(0.004 * W)))
        d.line([(x0 + x1) / 2, y0, (x0 + x1) / 2, y1], fill=WHITE, width=max(1, int(0.004 * W)))

    # ---- Kalp (cati ustu) ----
    def heart(cxh, cyh, s):
        d.ellipse([cxh - s, cyh - s * 0.55, cxh, cyh + s * 0.35], fill=HEART)
        d.ellipse([cxh, cyh - s * 0.55, cxh + s, cyh + s * 0.35], fill=HEART)
        d.polygon([(cxh - s * 0.97, cyh - s * 0.05), (cxh + s * 0.97, cyh - s * 0.05),
                   (cxh, cyh + s * 1.05)], fill=HEART)

    heart(0.5 * W, 0.24 * H, 0.075 * W)

    # ---- Aile ----
    def person(px, base_y, bw, bh, color, head_r):
        d.rounded_rectangle([px - bw / 2, base_y - bh, px + bw / 2, base_y],
                            radius=bw * 0.45, fill=color)
        d.ellipse([px - head_r, base_y - bh - head_r * 2.1,
                   px + head_r, base_y - bh - head_r * 0.1], fill=SKIN)

    gy_base = gy
    person(0.362 * W, gy_base, 0.082 * W, 0.086 * H, FATHER, 0.030 * W)
    person(0.5 * W, gy_base, 0.080 * W, 0.078 * H, MOTHER, 0.028 * W)
    person(0.642 * W, gy_base, 0.062 * W, 0.062 * H, CHILD, 0.023 * W)

    # ---- Gunes ----
    d.ellipse([0.775 * W, 0.08 * H, 0.855 * W, 0.152 * H], fill=SUN)
    return im


def make_icon(size):
    """Kare ikon: gradyan zemin + sahne."""
    return draw_house_scene(size=(size, size))


def make_round_icon(size):
    """Yogunluklar arasi yumusak gecis icin dairesel maskeli ikon."""
    im = make_icon(size).convert("RGBA")
    mask = Image.new("L", (size, size), 0)
    md = ImageDraw.Draw(mask)
    md.ellipse([0, 0, size, size], fill=255)
    im.putalpha(mask)
    return im


def make_play_icon():
    im = make_icon(512).convert("RGB")
    im.save(os.path.join(OUT, "play-store-icon-512.png"))
    # Ayni dosyayi 192 de sakla
    im.resize((192, 192), Image.LANCZOS).save(os.path.join(OUT, "icon-192.png"))
    return im


def make_feature_graphic():
    W, H = 1024, 500
    im = _vgrad((W, H), BG_TOP, BG_BOT)
    d = ImageDraw.Draw(im, "RGBA")

    # Sol tarafta ikon sahnesi
    icon = draw_house_scene(size=(400, 400))
    im.paste(icon, (-40, 50))

    # Sag tarafta metin
    f_title = _font(64)
    f_sub = _font(30, bold=False)
    f_tag = _font(22, bold=False)

    d.text((400, 150), "Aile Takip", font=f_title, fill=WHITE)
    d.text((402, 232), "Görev · Alışveriş · Bütçe · Sağlık", font=f_sub, fill=(187, 222, 251))
    d.text((402, 280), "Tüm aile tek uygulamada, gerçek zamanlı senkron",
           font=f_tag, fill=(144, 202, 249))

    # Ozellik rozetleri
    badges = ["Otomatik Senkron", "QR Davet", "Çakışma Çözümü"]
    bx = 402
    for b in badges:
        tw = d.textlength(b, font=f_tag)
        d.rounded_rectangle([bx, 330, bx + tw + 28, 368], radius=19,
                            fill=(255, 255, 255, 38), outline=(144, 202, 249, 180), width=2)
        d.text((bx + 14, 338), b, font=f_tag, fill=WHITE)
        bx += tw + 44

    im.save(os.path.join(OUT, "feature-graphic-1024x500.png"))
    return im


def make_og_image():
    W, H = 1200, 630
    im = _vgrad((W, H), BG_TOP, (16, 90, 170))
    d = ImageDraw.Draw(im, "RGBA")

    icon = draw_house_scene(size=(420, 420))
    im.paste(icon, (60, 105))

    f_title = _font(76)
    f_sub = _font(34, bold=False)

    d.text((540, 200), "Aile Takip v3.6.1", font=f_title, fill=WHITE)
    d.text((543, 300), "Ev işlerinden sağlığa — ailenizin dijital yuvası",
           font=f_sub, fill=(187, 222, 251))

    # alt serit
    d.rectangle([0, H - 70, W, H], fill=(10, 40, 90, 200))
    f_small = _font(24, bold=False)
    d.text((40, H - 52), "com.aile.takip  ·  Android 8.0+  ·  Offline-first  ·  Firebase senkron",
           font=f_small, fill=(144, 202, 249))

    im.save(os.path.join(OUT, "og-share-1200x630.png"))
    return im


def make_poster():
    W, H = 1080, 1350
    im = _vgrad((W, H), BG_TOP, (10, 50, 120))
    d = ImageDraw.Draw(im, "RGBA")

    # ust serit
    d.rectangle([0, 0, W, 120], fill=(255, 255, 255, 18))

    icon = make_round_icon(360)
    im.paste(icon, (W // 2 - 180, 90), icon)

    f_title = _font(96)
    f_v = _font(40)
    f_h = _font(36)
    f_b = _font(28, bold=False)

    d.text((W // 2, 520), "Aile Takip", font=f_title, fill=WHITE, anchor="mm")
    d.text((W // 2, 590), "v3.6.1 — Android 16 (API 36) hazır", font=f_v,
           fill=(255, 213, 79), anchor="mm")

    feats = [
        ("🏠", "Ana Sayfa · Görevler · Alışveriş · Mesajlar"),
        ("🔄", "Otomatik senkron — her değişiklik anında aileye gider"),
        ("🛡️", "Çakışma çözümü: son değiştiren kazanır, veri kaybı yok"),
        ("📱", "QR davet: tek tarama ile aileye katıl"),
        ("💪", "Sağlık: kalori, su, uyku ve spor takibi"),
        ("🔐", "Onaylı üyelik — aile verisi yalnızca ailede"),
    ]
    y = 680
    for emoji, txt in feats:
        d.text((140, y), emoji, font=f_h, fill=WHITE)
        d.text((210, y + 6), txt, font=f_b, fill=(219, 234, 254))
        y += 84

    d.rounded_rectangle([140, y + 20, W - 140, y + 110], radius=24,
                        fill=(255, 255, 255, 26), outline=(144, 202, 249, 200), width=3)
    d.text((W // 2, y + 65), "AileTakip-v3.6.1-release.apk", font=f_h, fill=WHITE, anchor="mm")

    im.save(os.path.join(OUT, "promo-poster-1080x1350.png"))
    return im


def make_banner():
    W, H = 1500, 500
    im = _vgrad((W, H), BG_TOP, BG_BOT)
    d = ImageDraw.Draw(im, "RGBA")
    icon = draw_house_scene(size=(380, 380))
    im.paste(icon, (30, 60))
    f_t = _font(72)
    f_s = _font(32, bold=False)
    d.text((460, 170), "Aile Takip", font=f_t, fill=WHITE)
    d.text((464, 270), "Ailenizin dijital yuvası — görev, alışveriş, bütçe, sağlık",
           font=f_s, fill=(187, 222, 251))
    im.save(os.path.join(OUT, "banner-1500x500.png"))
    return im


def make_preview_html():
    import base64
    files = [
        ("play-store-icon-512.png", "Play Store İkonu (512×512)"),
        ("icon-192.png", "İkon 192"),
        ("feature-graphic-1024x500.png", "Feature Graphic (1024×500)"),
        ("og-share-1200x630.png", "OG / Paylaşım (1200×630)"),
        ("banner-1500x500.png", "Banner (1500×500)"),
        ("promo-poster-1080x1350.png", "Tanıtım Posteri (1080×1350)"),
    ]
    # Görselleri data-URI olarak göm (tek dosyalık önizleme sunucuları için)
    items = "\n".join(
        f'<figure><img src="data:image/png;base64,{base64.b64encode(open(os.path.join(OUT, f), "rb").read()).decode()}" alt="{t}"><figcaption>{t}</figcaption></figure>'
        for f, t in files
    )
    html = f"""<!DOCTYPE html>
<html lang="tr"><head><meta charset="utf-8">
<title>Aile Takip — Görseller</title>
<style>
body{{font-family:system-ui,sans-serif;background:#0a1929;color:#e3f2fd;margin:0;padding:32px}}
h1{{font-weight:700}} figure{{margin:0 0 32px}} img{{max-width:100%;border-radius:12px;display:block}}
figcaption{{margin-top:8px;color:#90caf9;font-size:14px}}
.grid{{display:grid;grid-template-columns:repeat(auto-fit,minmax(320px,1fr));gap:24px}}
</style></head><body>
<h1>Aile Takip — Üretilen Görseller</h1>
<div class="grid">{items}</div>
</body></html>"""
    with open(os.path.join(OUT, "preview.html"), "w", encoding="utf-8") as f:
        f.write(html)


if __name__ == "__main__":
    # 1) mipmap PNG ikonlari
    densities = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}
    for dpi, px in densities.items():
        d = os.path.join(ROOT, "app", "src", "main", "res", f"mipmap-{dpi}")
        os.makedirs(d, exist_ok=True)
        make_icon(px).convert("RGB").save(os.path.join(d, "ic_launcher.png"))
        make_round_icon(px).save(os.path.join(d, "ic_launcher_round.png"))
        print(f"mipmap-{dpi}: ic_launcher.png + ic_launcher_round.png ({px}px)")

    # 2) pazaryeri & tanitim gorselleri
    make_play_icon()
    make_feature_graphic()
    make_og_image()
    make_banner()
    make_poster()
    make_preview_html()
    print("\nTum gorseller uretildi -> art/ klasoru")
