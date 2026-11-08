"""Package only source files and a verified, installable personal APK."""
import argparse
import hashlib
import json
import shutil
import subprocess
import zipfile
from pathlib import Path

root = Path(__file__).resolve().parents[1]
parser = argparse.ArgumentParser()
parser.add_argument("--output", type=Path, default=root.parent / "downloads")
args = parser.parse_args()
destination = args.output.resolve()
destination.mkdir(parents=True, exist_ok=True)
apk = root / "app/build/outputs/apk/debug/app-debug.apk"
digest = hashlib.sha256(apk.read_bytes()).hexdigest()
verification = json.loads((root / "design/qa-small/result.json").read_text(encoding="utf-8"))
assert verification["status"] == "passed" and verification["apk_sha256"] == digest, "Test the final APK before packaging."
shutil.copy2(apk, destination / "Legacy-Link-2.0.0.apk")
shutil.copy2(root / "design/Legacy-Link-Preview.png", destination / "Legacy-Link-Preview.png")
shutil.copy2(root / "design/legacy-link-icon.png", destination / "Legacy-Link-Icon.png")
shutil.copy2(root / "RELEASE_NOTES.md", destination / "Install-Legacy-Link.md")
shutil.copy2(root / "design/qa-small/result.json", destination / "Verification.json")
files = subprocess.check_output(["git", "ls-files", "--cached", "--others", "--exclude-standard", "-z"], cwd=root).decode("utf-8").split("\0")
archive = destination / "Legacy-Link-2.0.0-source.zip"
with zipfile.ZipFile(archive, "w", zipfile.ZIP_DEFLATED, compresslevel=9) as package:
    for name in sorted(set(files)):
        source = root / name
        if name and source.is_file():
            assert source.suffix not in (".jks", ".keystore") and source.name != "local.properties"
            package.write(source, "legacy-link/" + name)
with zipfile.ZipFile(archive) as package:
    assert package.testzip() is None
    names = package.namelist()
    assert "legacy-link/gradle/wrapper/gradle-wrapper.jar" in names
    assert "legacy-link/app/src/main/java/mv/legacy/link/MainActivity.kt" in names
checksums = []
for file in sorted(destination.iterdir()):
    if file.is_file() and file.name != "SHA256SUMS.txt":
        checksums.append(f"{hashlib.sha256(file.read_bytes()).hexdigest()}  {file.name}")
(destination / "SHA256SUMS.txt").write_text("\n".join(checksums) + "\n", encoding="utf-8")
print(json.dumps({"apk_sha256": digest, "source_files": len(names), "downloads": [{"name": p.name, "bytes": p.stat().st_size} for p in sorted(destination.iterdir()) if p.is_file()]}, indent=2))
