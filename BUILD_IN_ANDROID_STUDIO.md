# Build Legacy Link 2.1

## Requirements

- Android Studio compatible with AGP 8.13, Android SDK 36 and Build Tools 35.0.0.
- JDK 17 or a compatible newer JDK.
- The checksum-pinned Gradle 8.14.3 wrapper included in this repository.
- Internet access for initial dependencies; an Android 8.0+ phone or emulator for testing.

## Open and run

Open the repository root containing settings.gradle.kts. Set a suitable Gradle JDK and Android SDK location, then sync. In Build Variants, choose personalDebug for private-network development or playDebug to check the HTTPS-only flavor. Choose a test device and run.

## Command line

```powershell
.\gradlew.bat testPersonalDebugUnitTest testPlayDebugUnitTest lintPersonalDebug lintPlayDebug assemblePersonalDebug assemblePlayDebug
```

On macOS/Linux use ./gradlew with the same tasks. If necessary, make the wrapper executable. Outputs are in app/build/outputs/apk/personal/debug and app/build/outputs/apk/play/debug.

## Signed Play upload

Keep the upload key outside the repository. With JAVA_HOME configured, run:

```powershell
python tools/build_play_release.py --key-dir ../private-release
```

The helper reuses an existing private key or creates a new one for a new app, runs checks, and builds the signed Play AAB/APK and personal debug APK. Do not create a different key when updating an existing Play app. See [BUILD_AND_SIGNING.md](play-store/documentation/BUILD_AND_SIGNING.md) for environment variables, backup and certificate details.

## Verification tools

- tools/emulator_smoke.py: synthetic notification relay, consent, queue, pause/resume, App info helper and deletion checks on a disposable emulator.
- tools/capture_play_store.py: signed Play-release UI/HTTPS validation and real phone screenshots.
- tools/render_store_pages.py: static privacy/support/deletion pages.
- tools/render_play_assets.py: exact-dimension Play artwork from actual captures.

Never commit local.properties, private keystores, signing credentials, server secrets or real notification data.
