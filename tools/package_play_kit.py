"""Validate public submission materials, create downloads, and stage safe uploads."""
from pathlib import Path
from PIL import Image
import hashlib
import json
import shutil
import subprocess
import xml.etree.ElementTree as ET
import zipfile

root = Path(__file__).resolve().parents[1]
kit = root / "play-store"
downloads = root.parent / "downloads"
downloads.mkdir(exist_ok=True)
sha = lambda p: hashlib.sha256(p.read_bytes()).hexdigest()
results = {}
for filename, limit in [("title.txt",30),("short_description.txt",80),("full_description.txt",4000),("changelogs/6.txt",500)]:
    value = (kit / "metadata/en-US" / filename).read_text(encoding="utf-8").strip()
    assert 0 < len(value) <= limit, filename
    results[filename] = {"characters":len(value),"limit":limit}
for name, size in [("icon.png",(512,512)),("featureGraphic.png",(1024,500))]:
    im = Image.open(kit / "metadata/en-US/images" / name)
    assert im.size == size and im.mode == "RGB", name
screens = sorted((kit / "metadata/en-US/images/phoneScreenshots").glob("*.png"))
assert len(screens) == 8
for p in screens:
    im = Image.open(p)
    assert im.size == (1080,1920) and im.mode == "RGB", p
results["artwork"] = {"phone_images":8,"phone_size":[1080,1920],"icon":[512,512],"feature":[1024,500],"mode":"RGB, no alpha"}
for flavor, report in [("personal", root / "design/qa-2.1-personal/result.json"),("play",kit / "verification/play-ui.json")]:
    data = json.loads(report.read_text())
    assert data["status"] == "passed"
    assert data["apk_sha256"] == sha(kit / f"builds/Legacy-Link-2.1.0-{flavor}.apk")
    results[flavor + "_emulator"] = data
android = "{http://schemas.android.com/apk/res/android}"
manifest = ET.parse(kit / "verification/play-manifest.xml").getroot()
assert manifest.attrib["package"] == "mv.legacy.link"
assert manifest.attrib[android+"versionCode"] == "6"
assert manifest.find("uses-sdk").attrib[android+"targetSdkVersion"] == "36"
application = manifest.find("application")
assert application.attrib[android+"usesCleartextTraffic"] == "false"
assert application.attrib[android+"allowBackup"] == "false"
assert application.attrib.get(android+"debuggable","false") == "false"
results["play_manifest"] = {"package":"mv.legacy.link","version":"2.1.0","version_code":6,"target_sdk":36,"min_sdk":26,"cleartext":False,"backup":False,"debuggable":False}
for flavor in ["personal","play"]:
    unit = ET.parse(root / f"app/build/test-results/test{flavor.title()}DebugUnitTest/TEST-mv.legacy.link.PairingTest.xml").getroot()
    assert unit.attrib["failures"] == "0" and unit.attrib["errors"] == "0"
    lint = ET.parse(root / f"app/build/reports/lint-results-{flavor}Debug.xml").getroot()
    issues = lint.findall("issue")
    assert not any(x.attrib["severity"] in ("Error","Fatal") for x in issues)
    results[flavor+"_checks"] = {"unit_tests":int(unit.attrib["tests"]),"failures":0,"lint_errors":0,"lint_warnings":sum(x.attrib["severity"]=="Warning" for x in issues)}
assert "legacystudiosofficial@gmail.com" in (root / "PRIVACY_POLICY.md").read_text()
verification = kit / "verification"
shutil.copy2(root / "design/qa-2.1-personal/result.json", verification / "personal-flow.json")
results["limitations"] = ["External HTTPS assistant and actual bank notifications require owner testing.","Android restricted-settings approval must be completed on the user's device.","Owner must confirm receiver/downstream AI data practices and provide private reviewer access in Play Console."]
(verification / "summary.json").write_text(json.dumps(results,indent=2)+"\n",encoding="utf-8")
checksums = []
for p in sorted((kit / "builds").iterdir()):
    if p.name == "SHA256SUMS.txt": continue
    checksums.append(sha(p)+"  "+p.name)
(kit / "builds/SHA256SUMS.txt").write_text("\n".join(checksums)+"\n",encoding="utf-8")
for p in (kit / "builds").glob("Legacy-Link-2.1.0-*"):
    shutil.copy2(p,downloads / p.name)
package = downloads / "Legacy-Link-2.1.0-Play-Submission-Kit.zip"
with zipfile.ZipFile(package,"w",zipfile.ZIP_DEFLATED,compresslevel=9) as archive:
    for p in sorted(kit.rglob("*")):
        if p.is_file(): archive.write(p,"play-store/"+p.relative_to(kit).as_posix())
    for directory in ["docs"]:
        for p in sorted((root / directory).rglob("*")):
            if p.is_file(): archive.write(p,p.relative_to(root).as_posix())
    for name in ["PRIVACY_POLICY.md","DATA_DELETION.md","SUPPORT.md","NOTIFICATION_ACCESS.md","LICENSE"]:
        archive.write(root/name,name)
files = subprocess.check_output(["git","ls-files","--cached","--others","--exclude-standard"],cwd=root,text=True).splitlines()
assert not any(Path(p).suffix.lower() in (".p12",".jks",".keystore") or Path(p).name in ("signing-credentials.json","local.properties") for p in files)
source = downloads / "Legacy-Link-2.1.0-Source.zip"
with zipfile.ZipFile(source,"w",zipfile.ZIP_DEFLATED,compresslevel=9) as archive:
    for rel in sorted(set(files)):
        p=root/rel
        if p.is_file(): archive.write(p,rel)
stage = root.parent / "github-public-final"
stage.mkdir(exist_ok=True)
for rel in sorted(set(files)):
    p=root/rel
    if p.is_file():
        target=stage/rel
        target.parent.mkdir(parents=True,exist_ok=True)
        shutil.copy2(p,target)
for p in [package,source]: shutil.copy2(p,stage/p.name)
(downloads/"Legacy-Link-2.1.0-SHA256SUMS.txt").write_text("\n".join(sha(p)+"  "+p.name for p in [package,source,*sorted((kit/"builds").glob("Legacy-Link-2.1.0-*"))])+"\n",encoding="utf-8")
print(json.dumps({"status":"passed","kit":str(package),"kit_bytes":package.stat().st_size,"source":str(source),"source_bytes":source.stat().st_size,"public_files":len(set(files)),"stage":str(stage)},indent=2))
