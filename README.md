# USB Toolkit for Android

Production-oriented USB storage utility built with **Kotlin**, **Jetpack Compose**, and **Material 3**.

## Features

- **USB status** – connection detection, capacity, used/free space, filesystem hint
- **Format screen** – filesystem selection (FAT32 / exFAT / NTFS) with explicit confirmation  
  *Note: Android does not expose public APIs for third-party apps to format volumes. The app never pretends a format succeeded.*
- **File manager** – separate Android / USB views via Storage Access Framework (SAF)
- **Copy** – multi-file copy with progress, speed, cancel, and optional SHA-256 verification
- **Downloads** – URL download with progress, cancel, optional SHA-256 check, history
- **Device Files** – copy user-provided files to USB and verify; no bundled payloads
- **Safely eject** – attempts orderly unmount (limited by platform restrictions)
- **Settings** – dark/light/system theme, verification toggle, auto-detect, about & licenses
- **Privacy** – local-only operations; no analytics, no ads, no silent uploads

## Requirements

- Android 8.0 (API 26) or higher
- USB OTG / USB-C host support on the device for external drives
- User must grant folder access via the system document picker (SAF)

## Permissions

| Permission | Reason |
|------------|--------|
| `INTERNET` | User-initiated downloads only |
| `ACCESS_NETWORK_STATE` | Optional Wi-Fi-only download preference |
| Storage (legacy / `MANAGE_EXTERNAL_STORAGE`) | Compatibility; primary access is via SAF |
| USB host feature | Optional, for device detection |

The app requests **only** what is needed and uses the modern Storage Access Framework for file access.

## Architecture

- Single-activity Compose UI
- ViewModel + StateFlow
- DataStore for preferences
- OkHttp for downloads
- DocumentFile for cross-storage operations
- BroadcastReceiver + StorageManager for USB presence

## Building

### Local

```bash
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk

./gradlew assembleRelease
# Unsigned release APK under app/build/outputs/apk/release/
```

Sign the release APK yourself for distribution. No signing keys are stored in the repository.

### CI (GitHub Actions)

The workflow `.github/workflows/build-apk.yml`:

- Triggers on push/PR to `main` and on `workflow_dispatch`
- Sets up JDK 17 and Android SDK
- Accepts licenses
- Builds both **debug** and **unsigned release** APKs
- Uploads artifacts:
  - `usb-toolkit-debug`
  - `usb-toolkit-release-unsigned`

## Important limitations (honest)

1. **Formatting** – Not possible via public Android APIs for third-party apps. The Format screen explains this clearly.
2. **Eject** – System may refuse unmount while files are open; the app reports the real result.
3. **Filesystem type** – Not reliably readable without privileged APIs; shown as Unknown when unknown.
4. **Deep recursive copy** – Current implementation copies top-level files; directories are created but deep recursion is intentionally kept simple for reliability.

## License

This project is provided as-is for educational and personal use. Third-party libraries retain their own licenses (Apache 2.0 for AndroidX, OkHttp, etc.).
