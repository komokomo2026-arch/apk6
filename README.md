# EKAMBANI MAIN — Android WebView project

This project builds a lightweight Android WebView wrapper for:

`https://komokomo2026-arch.github.io/posnew6/`

## Important behavior

- The first successful website load requires internet.
- WebView uses its persistent cache and enables the website's service worker where supported.
- When offline, the app attempts to load cached content. If the page cannot be loaded, it shows a clean **No internet connection** screen with a retry button.
- Full offline operation is not guaranteed by WebView cache alone. The website must have a correctly configured PWA service worker that precaches all essential files and supports offline navigation/data.
- The app hides Android status/navigation bars for an immersive full-screen experience and supports HTML video custom-fullscreen callbacks.
- Camera permission is restricted to the configured GitHub Pages host.
- Only HTTPS links on the configured host remain inside the app. Other links open externally.
- File selection and HTTPS downloads are supported.

## URL privacy and security warning

The URL is not shown in an address bar, but it is **not secret**. It is embedded in the APK and is also visible to the device/network while the app connects. A technically capable user can extract it. Do not treat a hidden URL as authentication or security.

HTML, CSS, and JavaScript delivered by the website remain inspectable. Never put API secrets, passwords, private keys, or privileged credentials in browser code. Put secrets behind a properly authenticated server-side API.

## Build locally

Requirements: JDK 17, Android SDK 35, and Gradle 8.9.

From the project root:

```bash
gradle assembleDebug
```

APK output:

`app/build/outputs/apk/debug/app-debug.apk`

## Build with GitHub Actions

The separate workflow file supplied with this project is:

`.github/workflows/build-apk.yml`

Create that folder in your GitHub repository and upload the workflow file there. The ZIP intentionally does not include the workflow, so it can be uploaded separately.

## Google sign-in / Drive API note

Google Identity Services and some OAuth flows intentionally restrict embedded WebViews. Existing Google Drive backup/sign-in behavior must be tested on a real Android device. If Google sign-in is blocked, use the supported system-browser OAuth flow or a secure server-side integration; do not weaken OAuth security.

## Before production

1. Test on real Android devices.
2. Confirm the website PWA service worker caches the app shell, modules, CSS, JS, and required assets.
3. Test IndexedDB data persistence and offline workflows.
4. Test camera/barcode scanning and file uploads.
5. Test Google Drive authorization and backup.
6. Create a properly signed release APK and protect the signing key in GitHub Actions Secrets. The supplied workflow creates a debug APK for initial testing.
