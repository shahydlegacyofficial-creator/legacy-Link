# Legacy Link install guide

This guide connects an Android phone to Legacy AI, JARVIS, or another compatible assistant running on a computer.

## 1. Connect the phone and computer privately

Tailscale is the recommended network path.

1. Install [Tailscale](https://tailscale.com/download) on the computer and sign in.
2. Install Tailscale from Google Play on the phone and use the same account.
3. In the computer's Tailscale app, note its `100.x.x.x` address or MagicDNS name.
4. Keep the assistant running and listening on port `3000` or your configured port.

The receiver must provide the routes described in [SERVER_API.md](SERVER_API.md).

## 2. Put the source on GitHub

1. Extract the GitHub-ready ZIP.
2. Create a new GitHub repository named `legacy-link`.
3. Upload everything **inside** the extracted `legacy-link` folder. The repository root should contain `app`, `.github`, `README.md`, and `settings.gradle.kts`.
4. Commit the files to the `main` branch.
5. Open **Actions → Build Legacy Link APK**. The workflow starts after the first push, or you can select **Run workflow**.
6. Open the successful run and download the `legacy-link-debug-apk` artifact.
7. Extract the artifact to get `app-debug.apk`.

If GitHub asks whether to enable Actions for the repository, enable them before running the workflow.

## 3. Install the APK

1. Copy `app-debug.apk` to the phone using USB, Tailscale file transfer, or another private method.
2. Tap the APK on the phone.
3. If Android blocks the install, allow **Install unknown apps** for the file manager or browser you used.
4. Complete the installation and open **Legacy Link**.

The Actions artifact is debug-signed for personal testing. Use a private production signing key before wider distribution.

## 4. Pair Legacy Link

1. Tap **Grant notification access**.
2. Find **Legacy Link**, turn access on, and confirm Android's warning.
3. In the assistant, open its phone-link settings and display the pairing QR.
4. Tap **Scan pairing QR** in the app and scan it. You can also enter the host and secret manually.
5. Tap **Save & test connection**.
6. Tap **Choose apps** and select only the apps whose notifications the assistant should receive.

No apps are selected by default. Forwarding starts only after you make a selection.

## 5. Confirm delivery

Send a test notification from one selected app. Legacy Link should show that it is connected and the assistant should receive the event within a few seconds.

If the computer is asleep or offline, the phone stores up to 200 recent events and retries later. Turning **Mirroring on** off pauses new delivery and queue retries.

## Troubleshooting

### “Can't reach Legacy”

- Confirm both devices appear in the same Tailscale account.
- Use the computer's `http://100.x.x.x:3000` address, not the browser HTTPS port unless your server supports it.
- Confirm Legacy/JARVIS is running and `GET /phone-ping` returns a `2xx` response.
- Allow the assistant's Node.js or server process through Windows Firewall on **Private** networks.
- From the phone browser, open `http://YOUR-PC-IP:3000/phone-ping`. It should return JSON such as `{"ok":true}`.

### “Reached Legacy, but the secret is wrong”

- Generate or display a fresh pairing secret on the assistant.
- Re-scan the QR or carefully replace the secret in the app.
- Do not include spaces or extra line breaks.

### No notifications arrive

- Re-open **Grant notification access** and verify Legacy Link is enabled.
- Open **Choose apps** and confirm the source app is ticked.
- Make sure **Mirroring on** is enabled.
- Some notification types, including persistent music controls and ongoing service notices, are intentionally ignored.

### The app picker does not show an app

Use **Add another app by package name** at the bottom of the picker. Android package visibility is intentionally limited to launchable apps.

### GitHub Actions fails

- Confirm `.github/workflows/build-apk.yml` was uploaded.
- Confirm the repository uses a `main` branch.
- Re-run the failed job once to rule out a temporary dependency-download failure.
- Open the failed **Lint and build APK** step and copy its error when asking for help.
