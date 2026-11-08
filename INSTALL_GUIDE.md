# Install and connect Legacy Link 2.1

## Choose the build

- Personal installation: `play-store/builds/Legacy-Link-2.1.0-personal.apk`. Supports HTTPS or HTTP over an encrypted private network such as Tailscale. Debug-signed using the same local debug identity as the earlier personal APK.
- Google Play: `play-store/builds/Legacy-Link-2.1.0-play.aab` is for the publisher to upload to Play Console. The Play build requires a valid HTTPS receiver. An AAB is not a file you tap to install directly.

The APK supports Android 8.0 and newer. Copy the personal APK to your phone, open it, and approve installation from your browser/file manager if Android requests it.

## Set up the receiver

Run Legacy AI or another compatible receiver providing GET /phone-ping and authenticated POST /phone-notify. Use your receiver's HTTPS address for the Play build. For the personal build, both devices can use the same private Tailscale network and the receiver's http://100.x.x.x:3000 address. Plain HTTP is not encrypted outside an encrypted network.

See [SERVER_API.md](SERVER_API.md) for the contract and [build/signing guidance](play-store/documentation/BUILD_AND_SIGNING.md) for HTTPS proxy setup.

## Connect the phone

1. Open Settings in Legacy Link. Scan the receiver's pairing QR or enter its address and pairing secret, then Save & test connection.
2. Review the notification-sharing disclosure and choose I agree if you want to proceed.
3. Grant Legacy Link access in Android's notification-access settings.
4. In Apps, select the banking or other sources you want to share. The default selection is empty.
5. Leave Notification forwarding enabled. Post a harmless notification from a selected app and verify it reaches the intended receiver.

## “App was denied access”

Android may block sensitive permissions for a sideloaded APK. If you trust your build, open Settings → Apps → Legacy Link → More/three-dot menu → Allow restricted settings, follow Android's prompt, then return to notification access. In Legacy Link 2.1, Settings → Notification access blocked? opens an explanation and an App info shortcut.

Full guide: [NOTIFICATION_ACCESS.md](NOTIFICATION_ACCESS.md). Device-management or manufacturer policies can prevent access; the app cannot override Android. It also cannot bypass banking-app or OS notification redaction.

## Queue and local data

Up to 200 undelivered events can be kept on the phone. Pausing forwarding pauses queued delivery. Deselecting a source stops future capture but does not erase old queued entries. Settings → Clear local data removes the pairing, sources, queue, consent and local status, and pauses the link. Copies already sent must be deleted through the receiver's controls.

## Update/signature conflicts

Android only updates apps signed with the same identity. The Play signing identity differs from the personal debug identity. If replacing an installation requires uninstalling, its pairing and queue will be removed; preserve anything needed and pair again afterward.

Support: legacystudiosofficial@gmail.com. Do not include real notifications, financial details or secrets in a support message.
