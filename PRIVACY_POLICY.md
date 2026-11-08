# Legacy Link Privacy Policy

Effective date: 8 November 2026

Legacy Link is developed by Legacy Studios. For privacy questions or requests, contact legacystudiosofficial@gmail.com. This policy describes Legacy Link 2.1 and later, including the Google Play and personal builds.

## What Legacy Link does

Legacy Link connects selected Android notifications to a compatible Legacy AI assistant or another assistant server that you configure. It does not provide banking accounts, initiate payments, sign in to your bank, or read your bank through a banking API. Notification access can expose sensitive information, so you choose the sources you want to share.

Forwarding requires a configured assistant address and secret, your agreement to the in-app data-sharing disclosure, Android notification access, at least one selected source, and forwarding being enabled. No source is selected by default. The notification listener can operate while Legacy Link is closed or not in use.

## Notification information

For selected sources, the app processes the source app's name and package identifier, notification title and text, and an event timestamp. It prefers the expanded text when Android provides it. Notification text may contain financial balances, payment or transaction details, account references, names, messages, email or SMS content, one-time codes, or other personal information included by the source app. Android or the source app may hide some content.

The app filters out sources you have not selected before extracting notification title and text. Android supplies notification events to the service, but Legacy Link does not forward the complete notification feed. It skips ongoing notifications such as persistent media controls. The app does not extract notification images, attachments, audio, or video.

## App selection, pairing, and camera

The app reads the names, identifiers, and icons of launchable installed apps locally to display the source picker. Your searches and the full app inventory are not uploaded. The name and identifier of a selected source are included when its notification is sent. You may also enter a package name manually.

The assistant address, pairing secret, selected sources, consent choice, forwarding setting, last connection-test result, and last successful-delivery time are stored in the app's private local storage. The pairing secret authenticates requests to your chosen assistant. Saving and testing a connection sends a synthetic pairing-test event to that assistant.

Camera access is used only when you choose to scan a pairing QR code. QR frames are processed on the phone. Legacy Link does not upload or retain camera photos or video. A pairing QR may contain your assistant address and secret; those values are handled like a manually entered pairing.

## Where information goes

Selected notification events go directly from your phone to the assistant address you configure. The app does not use a Legacy Studios notification relay, advertising service, analytics SDK, or crash-reporting SDK. Legacy Studios does not automatically receive your notification feed through this app.

Your chosen server receives the event content, the pairing secret used to authenticate the request, and connection information ordinarily visible to a server, such as the connecting network address. The server operator controls what happens after receipt. Your assistant may retain notifications or pass content to AI providers or other integrations that you configure. Review those services and their privacy practices before selecting sources. This app cannot inspect, control, or erase copies held by independent assistant servers or their providers.

Legacy Studios does not sell notification information or use it for advertising. If you contact support, we receive the email and any information you choose to include. Do not send pairing secrets, bank credentials, private messages, or one-time codes to support.

## Security and transport

The Google Play build requires HTTPS assistant addresses and uses Android's normal certificate validation. It does not follow HTTP redirects when transmitting notifications or the pairing secret. The separately distributed personal build also supports HTTP for private-network setups. Plain HTTP is not encrypted by itself; use HTTPS or an appropriately encrypted private network for that build.

Local preferences and the queue use Android's app-private storage. Legacy Link does not add its own database-encryption layer; device-level storage protection depends on Android and your device settings. Android app-data backup and device transfer are disabled for the app. Protect your device lock and assistant credentials, and rotate the pairing secret if it becomes exposed.

## Local queue and retention

If delivery fails, up to 200 undelivered notification events are retained locally. A full queue keeps the newest 200 events and replaces older entries. Queued entries remain until successfully delivered, replaced by newer entries, or removed through Clear local data, Android's app-data controls, or uninstalling. They do not expire after a fixed number of days.

Retries occur when the app opens, the listener reconnects, the phone restarts, another selected notification arrives, or forwarding is resumed. Pausing forwarding also pauses queued delivery. Once delivery succeeds, the corresponding queued copy is removed. The app stores a last-delivery timestamp, rather than maintaining a separate history of delivered notification bodies. Receiver-side retention is controlled by your assistant and its integrations.

Support correspondence may be retained to address the request and meet applicable obligations. You can ask us to access or delete information you have sent to our support address; we will assess the request under applicable law. We cannot erase information from a server that we do not operate.

## Your choices and deletion

You can deselect source apps to stop future capture from them, pause all forwarding, or revoke Android notification access. Deselecting an app or revoking access does not erase previously queued entries; pause forwarding or clear local data if you do not want those entries sent.

In Legacy Link, open Settings and choose Clear local data, then confirm. This removes the saved pairing and secret, selected apps, waiting notifications, consent choice, and local status, and pauses forwarding. Android's Clear storage or uninstall controls can also remove app-local data. These actions do not erase the original notifications in other apps or copies already sent to your assistant. Request deletion of those copies from the receiver operator and any connected providers.

Legacy Link does not create a Legacy Studios user account. There is no in-app account to delete. Deletion instructions are available at https://shahydlegacyofficial-creator.github.io/legacy-Link/deletion.html.

## Intended audience

Legacy Link is intended for adult users who manage their own assistant and notification sources. It is not designed or marketed for children under 18, and it does not ask children to provide information. If you believe information from a child has been sent to Legacy Studios, contact us so we can address the request.

## Website and changes

The public support and policy pages are hosted on GitHub Pages. The hosting provider may process ordinary website request information under its own privacy practices. These pages contain no Legacy Studios advertising or analytics scripts.

We may update this policy when the app or its data practices change. The effective date will identify the current version. Material changes to the notification-sharing disclosure will require a renewed in-app agreement where appropriate.

## Contact

Legacy Studios — legacystudiosofficial@gmail.com

Support: https://shahydlegacyofficial-creator.github.io/legacy-Link/support.html
