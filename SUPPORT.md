# Legacy Link support

Legacy Link is the Android companion for the Legacy AI assistant ecosystem, developed by Legacy Studios.

Support and privacy email: legacystudiosofficial@gmail.com

## Set up your link

1. Run a compatible assistant receiver. The Google Play build needs a valid HTTPS endpoint implementing GET /phone-ping and authenticated POST /phone-notify.
2. Pair with the QR in your assistant's Phone link settings, or enter the assistant address and pairing secret.
3. Review the notification-sharing disclosure and grant Android notification access.
4. Select the banking or other apps you want to share. No source is selected automatically.
5. Leave forwarding enabled. The assistant must remain reachable from the phone.

The personal APK also supports HTTP over an encrypted private network such as Tailscale. The Google Play build requires HTTPS. Legacy Link itself does not include or host your assistant receiver.

## Notification access is denied

Use Settings → Notification access blocked? in the app. Follow the restricted-settings instructions in the notification-access guide linked below. The app cannot override Android's permission decision.

## Connection fails

Confirm the address, certificate, port, pairing secret, network reachability and receiver routes. A rejected secret requires a fresh secret from the assistant. The Play build cannot pair with a plain HTTP address. Do not put a real server secret in a public issue.

## Notifications are missing

Check the source selection, forwarding switch, in-app agreement and Android notification access. Only new selected notifications are captured. Ongoing notifications are skipped. Android, the source app, a work profile or device policy may hide sensitive content. Offline events are kept in a local queue of up to 200 and retried when the link is active again.

## Privacy and deletion

Use Settings → Privacy policy for the full policy. Settings → Clear local data removes the phone's pairing, queue and selections, and pauses forwarding. It does not erase copies already sent to your chosen assistant.

Privacy: https://shahydlegacyofficial-creator.github.io/legacy-Link/privacy.html

Deletion: https://shahydlegacyofficial-creator.github.io/legacy-Link/deletion.html

Notification-access help: https://shahydlegacyofficial-creator.github.io/legacy-Link/notification-access.html

## Contacting support

Include the app version, Android version, phone model, whether you installed the Play or personal build, and steps that reproduce the problem. Replace addresses and notification text with harmless examples. Never include bank passwords, balances, account identifiers, pairing secrets or one-time codes.
