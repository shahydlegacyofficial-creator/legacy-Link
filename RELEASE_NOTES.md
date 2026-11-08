# Legacy Link 2.0.0

This update replaces the original setup form with a native Android companion interface branded for Legacy Studios.

- Overview with setup status, a forwarding switch, selected-source and pending-delivery counts, and the last successful delivery time.
- Searchable app picker with All apps and Selected filters, real app icons, immediate saving, and manual package-name entry.
- Connection settings with QR scanning, manual pairing, masked secrets, visible validation, and actionable test results.
- Original L / angular-A icon inspired by the supplied Legacy Studios wordmark, including adaptive and monochrome variants.
- Bundled Manrope typography and a consistent graphite, platinum and ice-blue palette.

The notification receiver API and application ID are unchanged. Only selected sources are forwarded. Connection results describe the last test; the app does not continuously monitor server availability.

## Installing the personal APK

1. Transfer `Legacy-Link-2.0.0.apk` to your Android phone and open it.
2. Allow installation from the browser or file manager when Android asks.
3. Open Legacy Link. Pair using the QR in your assistant's Settings → Phone link, or enter its address and secret.
4. Allow notification access and choose your banking app in Apps.
5. Keep the phone and assistant reachable through HTTPS or your private Tailscale network.

The APK is debug-signed for personal sideloading. No signing keys are included in the source download. It supports Android 8.0 and newer and contains no native CPU-specific binaries.

If Android reports a signature conflict with an existing installation, that installation was signed with a different key. Keep the existing version until you are ready to replace it: uninstalling removes its pairing and undelivered queue. Install this version and pair again after saving any information you need.

## Build and checks

Configure `JAVA_HOME` with a JDK 17 or compatible JDK, and install Android SDK platform/build tools 34. Then run:

```powershell
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug
```

The installable output is `app/build/outputs/apk/debug/app-debug.apk`.

`tools/emulator_smoke.py` exercises the actual APK on a disposable Android emulator with synthetic notifications and a local receiver. It checks pairing, source selection, queue behavior and pause/resume. It never connects to a real bank or the user's assistant. Screenshots and the check result are saved in `design/qa/` when run.
