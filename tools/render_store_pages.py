"""Render controlled, plain Markdown policy/support sources to static HTML."""
from pathlib import Path
import html
import json
import re

root = Path(__file__).resolve().parents[1]
site = root / "docs"
site.mkdir(exist_ok=True)
(site / ".nojekyll").touch()
css = '''@font-face{font-family:Manrope;src:url(assets/manrope.ttf) format('truetype');font-weight:200 800;font-display:swap}*{box-sizing:border-box}html{color-scheme:dark}body{margin:0;background:#0b0e13;color:#f1f4f8;font:16px/1.8 Manrope,system-ui,sans-serif}header,main,footer{max-width:850px;margin:auto;padding:30px 28px}header{display:flex;justify-content:space-between;gap:22px;border-bottom:1px solid #293342}a{color:#aecdf7;text-underline-offset:4px}nav{display:flex;flex-wrap:wrap;gap:20px;font-size:13px}.brand{color:#f1f4f8;text-decoration:none;font-weight:750;letter-spacing:-.04em;font-size:21px}h1{font-size:clamp(32px,7vw,49px);letter-spacing:-.05em;line-height:1.16;margin:30px 0}h2{font-size:23px;line-height:1.4;letter-spacing:-.02em;margin:42px 0 12px}p,li{color:#c4cddb}ul,ol{padding-left:24px}li{margin:9px 0}footer{margin-top:42px;border-top:1px solid #293342;color:#a0adc0;font-size:13px}.eyebrow{font-size:11px;letter-spacing:.2em;color:#a0adc0}.cards{display:grid;grid-template-columns:1fr 1fr;gap:14px}.card{padding:22px;border-radius:20px;background:#141922}.card a{font-size:20px;font-weight:650}.card p{font-size:14px;margin-bottom:0}a:focus-visible{outline:2px solid #aecdf7;outline-offset:5px}@media(max-width:600px){header{display:block}nav{margin-top:16px}.cards{grid-template-columns:1fr}}'''
(site / "style.css").write_text(css + '\nbody{overflow-wrap:anywhere}', encoding="utf-8")
assets = site / "assets"
assets.mkdir(exist_ok=True)
(assets / "manrope.ttf").write_bytes((root / "app/src/main/res/font/manrope.ttf").read_bytes())
(assets / "Manrope-OFL.txt").write_bytes((root / "design/Manrope-OFL.txt").read_bytes())


def inline(text):
    escaped = html.escape(text)
    return re.sub(r"https://[^\s<]+", lambda m: '<a href="' + m.group(0).rstrip('.') + '">' + m.group(0).rstrip('.') + '</a>' + ('.' if m.group(0).endswith('.') else ''), escaped)


def render(markdown):
    output = []
    for block in markdown.strip().split("\n\n"):
        lines = block.splitlines()
        if block.startswith("# "):
            output.append("<h1>" + inline(block[2:]) + "</h1>")
        elif block.startswith("## "):
            output.append("<h2>" + inline(block[3:]) + "</h2>")
        elif all(re.match(r"\d+\. ", line) for line in lines):
            output.append("<ol>" + "".join("<li>" + inline(re.sub(r"^\d+\. ", "", line)) + "</li>" for line in lines) + "</ol>")
        elif all(line.startswith("- ") for line in lines):
            output.append("<ul>" + "".join("<li>" + inline(line[2:]) + "</li>" for line in lines) + "</ul>")
        else:
            output.append("<p>" + inline(" ".join(lines)) + "</p>")
    return "\n".join(output)


def page(title, body):
    return f'''<!doctype html><html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>{html.escape(title)} · Legacy Studios</title><meta name="description" content="Legacy Link Android companion: support, privacy and user data controls."><link rel="stylesheet" href="style.css"></head><body><header><a class="brand" href="index.html">Legacy Link</a><nav aria-label="Support navigation"><a href="support.html">Support</a><a href="privacy.html">Privacy</a><a href="deletion.html">Delete data</a></nav></header><main>{body}</main><footer>Legacy Studios · <a href="mailto:legacystudiosofficial@gmail.com">legacystudiosofficial@gmail.com</a><br>Updated 8 November 2026 · No advertising or analytics scripts.</footer></body></html>'''


for source, destination, title in [("PRIVACY_POLICY.md", "privacy.html", "Legacy Link Privacy Policy"), ("DATA_DELETION.md", "deletion.html", "Delete Legacy Link data"), ("SUPPORT.md", "support.html", "Legacy Link support"), ("NOTIFICATION_ACCESS.md", "notification-access.html", "Notification access help")]:
    (site / destination).write_text(page(title, render((root / source).read_text(encoding="utf-8"))), encoding="utf-8")
home = '''<p class="eyebrow">LEGACY AI ECOSYSTEM</p><h1>Your phone.<br>Your Legacy.</h1><p>Legacy Link connects selected Android notifications to the assistant you configure. Pair, choose your sources, and stay in control of what is shared.</p><div class="cards"><section class="card"><a href="support.html">Set up your link</a><p>Pairing, HTTPS and source selection.</p></section><section class="card"><a href="notification-access.html">Notification access help</a><p>Resolve Android restricted-settings messages.</p></section><section class="card"><a href="privacy.html">Privacy policy</a><p>Understand notification sharing and local storage.</p></section><section class="card"><a href="deletion.html">Delete local data</a><p>Remove pairing, selections and waiting events.</p></section></div><h2>Contact Legacy Studios</h2><p>Email <a href="mailto:legacystudiosofficial@gmail.com">legacystudiosofficial@gmail.com</a>. Please exclude financial information, one-time codes and pairing secrets.</p><p><a href="https://github.com/shahydlegacyofficial-creator/legacy-Link">Project, source and submission kit on GitHub</a></p>'''
(site / "index.html").write_text(page("Legacy Link support", home), encoding="utf-8")
metadata = {"package_name": "mv.legacy.link", "version_name": "2.1.0", "version_code": 6, "developer_name": "Legacy Studios", "support_email": "legacystudiosofficial@gmail.com", "default_language": "en-US", "category_suggestion": "Tools", "privacy_url": "https://shahydlegacyofficial-creator.github.io/legacy-Link/privacy.html", "support_url": "https://shahydlegacyofficial-creator.github.io/legacy-Link/", "deletion_url": "https://shahydlegacyofficial-creator.github.io/legacy-Link/deletion.html", "target_sdk": 36, "min_sdk": 26, "intended_audience_suggestion": "18+", "contains_ads": False, "account_creation": False}
(root / "play-store/listing.json").write_text(json.dumps(metadata, indent=2) + "\n", encoding="utf-8")
print("Rendered policy, support, deletion and permission-help HTML pages and listing metadata.")
