// Render the original vector mark for legacy launchers and the download preview.
const fs = require('node:fs');
const path = require('node:path');
const sharp = require(process.env.SHARP_MODULE || 'sharp');
const root = path.resolve(__dirname, '..');
const svg = fs.readFileSync(path.join(root, 'design/legacy-link-icon.svg'));
(async () => {
  await sharp(svg).resize(1024).png().toFile(path.join(root, 'design/legacy-link-icon.png'));
  for (const [density, size] of Object.entries({mdpi: 48, hdpi: 72, xhdpi: 96, xxhdpi: 144, xxxhdpi: 192})) {
    await sharp(svg).resize(size).png().toFile(path.join(root, `app/src/main/res/mipmap-${density}/ic_launcher.png`));
  }
})();
