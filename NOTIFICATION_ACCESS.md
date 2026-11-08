# Fix “App was denied access” for Legacy Link

Android can restrict notification-listener access for sideloaded apps, including APKs opened from a browser, file manager or messaging app. The system warning is controlled by Android, not by Legacy Link's notification code. The app cannot grant this permission itself.

## On Android 13 and later

1. Close the denied-access dialog.
2. Open Android Settings → Apps → Legacy Link. You may need See all apps or App info.
3. Open the three-dot More menu and choose Allow restricted settings, if shown.
4. Follow Android's instructions, including device-lock confirmation if requested.
5. Return to Legacy Link, review the notification-sharing disclosure, and choose Open Android permissions.
6. Enable Legacy Link in Android's notification-access screen and confirm the system prompt.
7. Pair the app and select the sources you want to forward.

In version 2.1, Settings → Notification access blocked? → Open App info opens the relevant app-information page. The permission button also opens Legacy Link's own notification-access page on supported Android versions.

Only enable sensitive access for a build you trust. If the menu is unavailable, a work profile, administrator policy, device protection setting or manufacturer variation may be involved. Ask your device administrator where appropriate. Do not disable Play Protect or other device protection as a workaround. Actual Google Play installs can be handled differently from sideloaded APKs; approval and installation source remain Android's decision.

After access is enabled, Android or the banking app can still hide sensitive notification content. Legacy Link forwards only the text Android actually exposes to the listener. It does not bypass bank-app protections.

Google's official instructions: https://support.google.com/android/answer/12623953

Support: legacystudiosofficial@gmail.com. Include the phone model, Android/OS version, Legacy Link version and installation source. Do not send financial information or pairing secrets.
