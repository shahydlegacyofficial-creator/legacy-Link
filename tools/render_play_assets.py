"""Create exact-size Play artwork from original vector geometry and real app captures."""
from pathlib import Path
import json
from PIL import Image, ImageDraw, ImageFont, ImageOps

root = Path(__file__).resolve().parents[1]
output = root / "play-store/metadata/en-US/images"
screens = output / "phoneScreenshots"
raw = root / "play-store/assets/raw-phone"
screens.mkdir(parents=True, exist_ok=True)
font_path = root / "app/src/main/res/font/manrope.ttf"
BG, SURFACE, LINE, INK, MUTED, BLUE = "#0B0E13", "#141922", "#293342", "#F1F4F8", "#A0ADC0", "#AECDF7"


def font(size, weight=450):
    result = ImageFont.truetype(str(font_path), size)
    result.set_variation_by_axes([weight])
    return result


def mark(draw, x, y, size, fill=INK):
    def polygon(points, color):
        draw.polygon([(x + px * size/108, y + py * size/108) for px, py in points], fill=color)
    polygon([(28,30),(35,30),(35,71),(53,71),(49.5,78),(28,78)], fill)
    polygon([(42,63),(60,29),(83,78),(75,78),(60,45),(49,66)], fill)
    polygon([(46,71),(66,71),(69.3,78),(42.3,78)], BLUE)


# Full-bleed square: Google Play supplies its own corner mask and shadow.
icon = Image.new("RGB", (1536,1536), "#10151E")
mark(ImageDraw.Draw(icon), 0, 0, 1536)
icon.resize((512,512), Image.Resampling.LANCZOS).save(output / "icon.png", optimize=True)

feature = Image.new("RGB", (2048,1000), BG)
d = ImageDraw.Draw(feature)
d.text((128,90), "Legacy Link", font=font(54,650), fill=INK)
d.multiline_text((128,245), "Your notifications.\nYour Legacy.", font=font(104,650), fill=INK, spacing=12)
d.text((128,808), "A direct link to your AI assistant.", font=font(35), fill=MUTED)
# Quiet connection geometry, not a duplicate launcher-icon tile.
d.rounded_rectangle((1410,300,1590,600), radius=28, fill=SURFACE, outline=LINE, width=3)
d.rounded_rectangle((1438,334,1562,552), radius=16, outline=INK, width=5)
d.line((1476,576,1524,576), fill=INK, width=5)
d.line((1604,450,1740,450), fill=BLUE, width=3)
mark(d, 1704,320,300)
feature.resize((1024,500), Image.Resampling.LANCZOS).save(output / "featureGraphic.png", optimize=True)

specs = [
    ("01-overview", "Your phone.\nYour Legacy.", "Selected notifications. One direct link."),
    ("02-sources", "Choose what\nreaches Legacy.", "Find your banking apps and other essentials."),
    ("03-selected", "Your sources.\nYour decision.", "Review exactly which apps are selected."),
    ("04-pairing", "Pair once.\nKeep the link.", "Scan a QR code or enter your HTTPS receiver."),
    ("05-disclosure", "Know what\nyou share.", "Review the disclosure before allowing access."),
    ("06-paused", "A pause,\non your terms.", "Stop forwarding and queued delivery together."),
    ("07-privacy", "Privacy,\nwithin reach.", "Understand the link and your data controls."),
    ("08-clear-data", "Start fresh,\nwhen you choose.", "Clear pairing, sources and waiting notifications."),
]
manifest = []
for stem, title, subtitle in specs:
    capture_path = raw / (stem + ".png")
    capture = Image.open(capture_path).convert("RGB")
    assert capture.size == (1080,1920), f"Use a real 1080x1920 capture: {capture_path}"
    canvas = Image.new("RGB", (1080,1920), BG)
    draw = ImageDraw.Draw(canvas)
    draw.text((72,58), "LEGACY LINK  /  LEGACY STUDIOS", font=font(21,650), fill=MUTED)
    draw.multiline_text((72,124), title, font=font(76,650), fill=INK, spacing=4)
    assert draw.textbbox((0,0), subtitle, font=font(27))[2] < 936
    draw.text((72,330), subtitle, font=font(27), fill=MUTED)
    # The entire app capture is preserved; no banking data or UI state is painted in.
    draw.rounded_rectangle((132,414,948,1852), radius=48, fill="#1C232F", outline=LINE, width=2)
    phone = capture.resize((792,1408), Image.Resampling.LANCZOS)
    mask = Image.new("L", phone.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle((0,0,791,1407), radius=37, fill=255)
    canvas.paste(phone, (144,428), mask)
    filename = stem + ".png"
    canvas.save(screens / filename, optimize=True)
    manifest.append({"file": filename, "title": title.replace("\n", " "), "alt_text": subtitle + " Captured from the Legacy Link Google Play interface.", "size": [1080,1920], "source_capture": "assets/raw-phone/" + filename})

board = Image.new("RGB", (1600,1750), BG)
bd = ImageDraw.Draw(board)
bd.text((70,45), "Legacy Link · Google Play assets", font=font(45,650), fill=INK)
bd.text((70,115), "Eight phone images · real app screens · no real banking data", font=font(23), fill=MUTED)
for index, (stem, _, _) in enumerate(specs):
    thumb = Image.open(screens / (stem + ".png")).resize((330,587), Image.Resampling.LANCZOS)
    x, y = 70 + (index % 4) * 375, 190 + (index // 4) * 720
    board.paste(thumb, (x,y))
    bd.text((x,y+604), stem.replace('-', ' ', 1), font=font(20), fill=MUTED)
bd.text((70,1670), "LEGACY STUDIOS   ·   512px ICON   ·   1024 × 500 FEATURE GRAPHIC   ·   1080 × 1920 PHONE IMAGES", font=font(20,650), fill=MUTED)
board.save(root / "play-store/Store-Assets-Preview.png", optimize=True)
(root / "play-store/assets/image-manifest.json").write_text(json.dumps(manifest, indent=2) + "\n", encoding="utf-8")
print("Rendered 512px icon, 1024x500 feature graphic, eight phone images, alt text and preview.")
