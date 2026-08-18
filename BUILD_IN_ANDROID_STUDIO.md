# Build Legacy Link in Android Studio

## Requirements

- Android Studio with Android SDK 34
- Java 17 (Android Studio's bundled JDK is suitable)
- Internet access for the first dependency download
- An Android 8.0 or newer phone for testing

## 1. Open and sync

1. Open Android Studio.
2. Select **File → Open** and choose the repository root containing `settings.gradle.kts`.
3. Trust the project when prompted.
4. Let Gradle sync finish.
5. If asked for a Gradle JDK, choose Android Studio's bundled Java 17 runtime.

## 2. Run on a phone

1. On the phone, enable **Developer options** by tapping **Build number** seven times in **Settings → About phone**.
2. Enable **USB debugging** in Developer options.
3. Connect the phone by USB and accept the authorization prompt.
4. Select the phone in Android Studio's device menu.
5. Click **Run**.

## 3. Build an installable test APK

Use **Build → Build Bundle(s) / APK(s) → Build APK(s)** with the `debug` variant selected.

The file is created at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

You can also build from a terminal:

```bash
./gradlew lintDebug assembleDebug
```

On Windows Command Prompt or PowerShell:

```powershell
.\gradlew.bat lintDebug assembleDebug
```

## Production releases

The debug APK is suitable for testing and personal sideloading. For public distribution:

1. Create a private Android signing key.
2. Store the keystore and passwords outside Git.
3. Add a release signing configuration that reads local or CI secrets.
4. Build `assembleRelease` and preserve the signing key for all future updates.

Never commit `.jks`, `.keystore`, `keystore.properties`, or signing passwords. They are already covered by `.gitignore`.

## Common fixes

- **SDK location not found:** open **File → Project Structure → SDK Location**, or let Android Studio create `local.properties`.
- **Java version error:** set **Gradle JDK** to Java 17.
- **Missing SDK 34:** install Android 14 / API 34 through **SDK Manager**.
- **Dependency download timeout:** retry Gradle sync after checking the network or proxy.
- **Phone not listed:** reconnect USB, accept the phone prompt, and try a data-capable cable.
