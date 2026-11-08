# Build and signing

## Build variants

The app retains ID mv.legacy.link and minSdk 26, and now compiles/targets API 36. Both variants include the same Legacy Studios UI and privacy controls.

- play: requires HTTPS and disallows cleartext network traffic. Use its signed AAB for Google Play.
- personal: supports HTTPS and HTTP for appropriately encrypted private-network setups. Its debug APK is for personal installation, including the original Tailscale workflow.

Toolchain: JDK 17 or compatible; Gradle 8.14.3 (checksum pinned); Android Gradle Plugin 8.13.0; Kotlin 2.2.20; Android SDK 36 and build tools 35.0.0. These choices support API 36 without migrating the app to a new UI framework.

## Reproduce a signed Play build

Set JAVA_HOME and your Android SDK path, then run:

```powershell
python tools/build_play_release.py --key-dir ../private-release
```

The script creates a new private upload key only if no key exists there, or reuses that directory's existing key. It restricts the private directory's access and never prints passwords. It runs unit tests, lint, builds the Play release AAB/APK and the personal debug APK, and copies public outputs to play-store/builds. The private key is not included in the submission ZIP or GitHub.

Private files: legacy-link-upload.p12 and signing-credentials.json. Back them up privately. Do not regenerate a key for an existing Play app without following Google's upload-key process. The public upload-certificate.pem can be shared; it is not the private key. Google Play App Signing's app-signing certificate may differ from the upload certificate.

For another build environment, set LEGACY_UPLOAD_STORE, LEGACY_UPLOAD_STORE_PASSWORD, LEGACY_UPLOAD_ALIAS and LEGACY_UPLOAD_KEY_PASSWORD as protected environment variables, then run:

```powershell
.\gradlew.bat testPersonalDebugUnitTest testPlayDebugUnitTest lintPersonalDebug lintPlayDebug bundlePlayRelease
```

If signing variables are absent, Gradle can produce an unsigned release bundle for build checks. The kit's published play.aab is checked as signed; an unsigned artifact is not the upload-ready replacement.

## Personal installation and signature changes

The personal debug APK uses this machine's existing Android debug key, preserving the earlier personal APK's signing identity. The Play APK is signed with the upload key, and Play-delivered installations use Play App Signing. Android cannot update an installation signed with a different key. Replacing it can require uninstalling, which removes pairing and the waiting queue; preserve anything needed and re-pair afterward.

## HTTPS receiver

The Play app requires a valid certificate and does not accept cleartext addresses or follow redirects. Put your existing /phone-ping and /phone-notify receiver behind a valid HTTPS reverse proxy. For example, a Caddy configuration on a server you control can be:

```text
assistant.your-domain.example {
    reverse_proxy 127.0.0.1:3000
}
```

Replace the example domain with a real domain you control, configure DNS and network reachability, and follow your proxy's certificate setup. Do not paste a raw HTTP or private-only address into Play review instructions. The existing personal build remains available for private Tailscale HTTP setups.
