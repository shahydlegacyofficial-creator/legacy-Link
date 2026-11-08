# Release test plan

## Automated checks

- Pairing validation: valid HTTP/HTTPS, port and URI validation, rejection of header-control characters, and rejection of HTTP for the Play build.
- Android lint and compilation for both flavors.
- Signature verification, package/version/API checks, release debuggable flag, HTTPS manifest flag and AAB structure.
- Metadata character limits, PNG color mode/dimensions/aspect, screenshot count and checksum checks.
- Emulator notification relay: opt-in sources, explicit sharing agreement, permission settings, unselected-source rejection, selected synthetic-event delivery, offline queue, pause/resume and data reset.
- Emulator UI for Play flavor: HTTPS rejection message, disclosure, policy/local-data controls, source picker, QR launch and the App info troubleshooting shortcut.

The generated verification documents report which checks actually ran. No real bank data, physical bank account or production assistant credential is used in automated tests. Do not represent these checks as Google approval or a physical-device test.

## Physical-device checks to complete before production

1. Install through internal testing on the affected phone and at least one other supported Android version. Check gestures, dark mode, larger text, QR camera and rotation.
2. Confirm Android's notification-access flow, including the restricted-settings helper for a sideloaded personal build. A work profile or managed device may prohibit access.
3. Pair a dedicated HTTPS test receiver, accept disclosure and select only test sources. Post harmless alerts; check payload/source filtering and the actual receiver's retention.
4. Test airplane mode/offline delivery, queue cap, pause/resume, receiver restart, device reboot, clearing local data and re-pairing.
5. Verify that clearing local data prevents later queued delivery and that a fresh install forwards nothing before agreement/source selection.
6. Check how the actual banking app and Android version expose or redact notification text. The app must not claim to bypass OS or bank restrictions.
7. Inspect logs and screen recordings to ensure they contain no real secrets or financial data. Use synthetic examples for bug reports and review.

## Review operations

Keep the reviewer receiver online throughout the review period. Check the Play pre-launch report. Collect real tester feedback and resolve findings. If your account requires closed testing, recruit actual eligible testers and meet the Console's continuous opt-in requirements; do not substitute automated emulators or generated reviews.
