# Android build, testing, and repeatable APK updates

## Build environment

- Java 17, Gradle 8.11.1, AGP 8.9.2, Kotlin 2.1.20
- Android SDK Platform 35, Build Tools 35.0.0
- CI workflow: `.github/workflows/android-testing-apk.yml`
- Build command: `gradle :app:testDebugUnitTest :app:assembleDebug`

This source archive does not contain a Gradle wrapper executable or JAR. Use installed Gradle 8.11.1 locally, or GitHub Actions (which installs Gradle).

## Critical: signing for stable update installs

Android allows upgrading an installed APK only when its package ID and signing certificate match and its versionCode is appropriate. Debug APKs built in separate ephemeral runners use different default debug signing certificates. That means repeated CI runs **can cause a signature conflict** when updating.

For private repeatable testing, generate one **persistent** testing keystore on a trusted local computer. Keep it backed up in a secure location; never commit it.

Example command on Mac with JDK `keytool`:

```bash
keytool -genkeypair -v -keystore "$HOME/semalam-testing.jks" \
  -alias semalam-testing -keyalg RSA -keysize 3072 -validity 3650
```

Then add these GitHub **Actions repository secrets** (not variables):

- `SEMALAM_TESTING_KEYSTORE_BASE64`: contents of `base64 < "$HOME/semalam-testing.jks" | tr -d '\n'` (on macOS use `base64 -i "$HOME/semalam-testing.jks" | tr -d '\n'` if needed)
- `SEMALAM_TESTING_STORE_PASSWORD`: keystore password
- `SEMALAM_TESTING_KEY_ALIAS`: `semalam-testing`
- `SEMALAM_TESTING_KEY_PASSWORD`: private key password

The workflow decodes the keystore only inside the runner. The same certificate must be retained for each test APK. Safeguard and back up the keystore; lost keys cannot sign compatible updates.

**Practical note:** If you already installed a differently signed alpha APK, the first transition to stable testing signing requires uninstalling the old testing APK (local audio will be erased) or making a backup through an explicitly designed feature later. Best to set up stable testing signing before sharing APKs broadly.

Increment `versionCode` when distributing a new build; `versionName` should also reflect change history. For alpha source built more than once without version bumps, reinstall with same cert may work through ADB but does not ensure predictable release updates. Configure CI version code strategy before public beta.

## Installation

1. GitHub → Actions → select completed `Android Testing APK` workflow.
2. Download artifact `semalam-android-testing-apk` and extract ZIP.
3. Install `app-debug.apk` (allow install from the chosen file manager if prompted).
4. Open app, grant microphone, inspect ongoing notification; do smoke test.

## Production guardrails

- Keep `dev.one2pret.semalam.prototype.debug` and production distinct.
- Never publish alpha as a health diagnosis product.
- No release signing configured: do not run release deployment as-is.
- App currently opts out of backups, and uninstall removes private recordings. Inform testers before uninstalling.
