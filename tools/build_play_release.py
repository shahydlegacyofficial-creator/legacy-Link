"""Create/reuse a private local upload key and build the signed Google Play bundle.

The private key and credentials live outside the repository. Only the upload
certificate and signed public build artifacts are copied into play-store/builds.
"""
import argparse
import getpass
import json
import os
from pathlib import Path
import secrets
import shutil
import subprocess

root = Path(__file__).resolve().parents[1]
parser = argparse.ArgumentParser()
parser.add_argument("--key-dir", type=Path, default=root.parent / "private-release")
args = parser.parse_args()
private = args.key_dir.resolve()
assert not private.is_relative_to(root), "Keep signing credentials outside this repository."
private.mkdir(parents=True, exist_ok=True)
if os.name == "nt":
    identity = subprocess.check_output(["whoami"], text=True).strip()
    subprocess.run(["icacls", str(private), "/inheritance:r", "/grant:r", identity + ":(OI)(CI)F", "SYSTEM:(OI)(CI)F"], check=True, capture_output=True)
else:
    private.chmod(0o700)
credentials_file = private / "signing-credentials.json"
keystore = private / "legacy-link-upload.p12"
java_home = os.environ.get("JAVA_HOME")
assert java_home, "Set JAVA_HOME to JDK 17 or a compatible JDK before building."
keytool = str(Path(java_home) / "bin" / ("keytool.exe" if os.name == "nt" else "keytool"))
environment = os.environ.copy()
if credentials_file.exists():
    credentials = json.loads(credentials_file.read_text(encoding="utf-8"))
    assert keystore.exists(), "Existing signing credentials have no matching keystore. Restore your key backup."
else:
    assert not keystore.exists(), "An existing key has no credentials file. Do not overwrite it."
    password = secrets.token_urlsafe(40)
    credentials = {"alias": "legacy-link-upload", "store_password": password, "key_password": password}
    environment["LEGACY_UPLOAD_STORE_PASSWORD"] = password
    subprocess.run([keytool, "-genkeypair", "-keystore", str(keystore), "-storetype", "PKCS12", "-alias", credentials["alias"], "-keyalg", "RSA", "-keysize", "3072", "-sigalg", "SHA256withRSA", "-validity", "10000", "-dname", "CN=Legacy Studios", "-storepass:env", "LEGACY_UPLOAD_STORE_PASSWORD", "-keypass:env", "LEGACY_UPLOAD_STORE_PASSWORD"], check=True, env=environment)
    credentials_file.write_text(json.dumps(credentials, indent=2), encoding="utf-8")
    if os.name != "nt":
        credentials_file.chmod(0o600)
        keystore.chmod(0o600)
environment.update({"LEGACY_UPLOAD_STORE": str(keystore), "LEGACY_UPLOAD_ALIAS": credentials["alias"], "LEGACY_UPLOAD_STORE_PASSWORD": credentials["store_password"], "LEGACY_UPLOAD_KEY_PASSWORD": credentials["key_password"]})
builds = root / "play-store/builds"
builds.mkdir(parents=True, exist_ok=True)
subprocess.run([keytool, "-exportcert", "-rfc", "-keystore", str(keystore), "-alias", credentials["alias"], "-storepass:env", "LEGACY_UPLOAD_STORE_PASSWORD", "-file", str(builds / "upload-certificate.pem")], check=True, env=environment)
wrapper = root / ("gradlew.bat" if os.name == "nt" else "gradlew")
subprocess.run([str(wrapper), "testPersonalDebugUnitTest", "testPlayDebugUnitTest", "lintPersonalDebug", "lintPlayDebug", "assemblePersonalDebug", "assemblePlayRelease", "bundlePlayRelease", "--console=plain", "--no-daemon"], cwd=root, env=environment, check=True)
shutil.copy2(root / "app/build/outputs/bundle/playRelease/app-play-release.aab", builds / "Legacy-Link-2.1.0-play.aab")
shutil.copy2(root / "app/build/outputs/apk/play/release/app-play-release.apk", builds / "Legacy-Link-2.1.0-play.apk")
shutil.copy2(root / "app/build/outputs/apk/personal/debug/app-personal-debug.apk", builds / "Legacy-Link-2.1.0-personal.apk")
print("Signed Play bundle and APK, plus the personal APK, are in play-store/builds. Private upload credentials remain outside the repository.")
