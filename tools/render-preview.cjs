// Build a review board from unmodified emulator screenshots and the vector icon.
const fs = require('node:fs');
const path = require('node:path');
const sharp = require(process.env.SHARP_MODULE || 'sharp');
const root = path.resolve(__dirname, '..');
const png = file => 'data:image/png;base64,' + fs.readFileSync(path.join(root, file)).toString('base64');
const screens = [['01-overview', 'Overview'], ['02-apps', 'Your selected sources'], ['04-settings', 'Pairing & connection']];
let svg = `<svg xmlns="http://www.w3.org/2000/svg" width="1360" height="1120" viewBox="0 0 1360 1120">
<rect width="1360" height="1120" fill="#0B0E13"/>
<text x="80" y="70" fill="#AECDF7" font-family="Segoe UI, sans-serif" font-size="13" letter-spacing="3">LEGACY STUDIOS / ANDROID COMPANION</text>
<text x="80" y="133" fill="#F1F4F8" font-family="Segoe UI, sans-serif" font-size="43" font-weight="600">Legacy Link. Reconnected.</text>
<text x="80" y="177" fill="#A0ADC0" font-family="Segoe UI, sans-serif" font-size="20">A direct line from your phone to your Legacy AI ecosystem.</text>
<image x="1168" y="56" width="106" height="106" href="${png('design/legacy-link-icon.png')}"/>`;
screens.forEach(([file, label], i) => {
  const x = 80 + i * 435;
  svg += `<defs><clipPath id="screen${i}"><rect x="${x}" y="250" width="330" height="733.333" rx="26"/></clipPath></defs>
  <rect x="${x-7}" y="243" width="344" height="747.333" rx="32" fill="#1C232F" stroke="#344155"/>
  <image x="${x}" y="250" width="330" height="733.333" clip-path="url(#screen${i})" href="${png(`design/qa/${file}.png`)}"/>
  <text x="${x}" y="1038" fill="#F1F4F8" font-family="Segoe UI, sans-serif" font-size="20">${label.replace('&', '&amp;')}</text>`;
});
svg += `<text x="80" y="1093" fill="#A0ADC0" font-family="Segoe UI, sans-serif" font-size="12" letter-spacing="1.5">LEGACY LINK 2.0.0 · NATIVE ANDROID · ACTUAL APP SCREENS</text></svg>`;
sharp(Buffer.from(svg)).png().toFile(path.join(root, 'design/Legacy-Link-Preview.png'));
