# Changelog

All notable changes to Legacy Link are documented here.

## 1.3.0 - 2026-08-18

### Added

- GitHub Actions lint and debug APK build.
- Complete Gradle 8.9 wrapper with official distribution and wrapper checksums.
- Legacy/JARVIS server API documentation.
- Repository security, editor, dependency-update, and licensing files.

### Changed

- Notification sources are now empty by default and must be explicitly selected.
- QR scanning uses one lifecycle-safe activity result launcher.
- Readability of setup, picker, and status text is improved.
- App enumeration is limited to launchable apps rather than all installed packages.
- Android backup is disabled to keep pairing data off device backups.
- HTTP redirects are rejected and notification field size is bounded.
- Pausing mirroring now also pauses retry-queue delivery.

### Removed

- Generated Gradle, Android Studio, and APK build output from the repository package.
- Unused foreground-service and app-notification permissions.
- Debug-key signing from the release build type.
