# Semalam — Android prototype (0.1.0-alpha01)

**Status: source-code prototype, not yet device-certified and not a medical device.**

Semalam is a privacy-first sleep sound diary. Start a session while the app is visible, lock the screen, and stop it in the morning. The local audio engine calibrates to the room for 12 seconds, segments **sound activity** into WAV clips, and lets you play, label, or delete recordings. No account, cloud, or internet permission is required.

## What works in source

- Kotlin + Jetpack Compose interface in Indonesian (dark theme)
- Android microphone foreground service with an ongoing notification and Stop action
- AudioRecord mono PCM 16 kHz, sampled continuously while the service is running
- Partial wake lock and user-visible recording status; max session 12 hours
- Adaptive room-noise threshold (12-second calibration), 1-second pre-roll, 1.5-second trailing context, 22-second max event clip
- WAV clips in app-private storage, JSON session metadata and manual labels
- Playback, session history, clip deletion, session deletion, session status and error notices
- Optional daily bedtime reminder; never starts the microphone automatically
- Basic algorithm tests; GitHub Actions workflow that will build an APK **when pushed to a GitHub repository**

## Not implemented or validated

- No automatic snoring/sleep-talking classification; all detected clips are generic audio events
- No voice fingerprint, AI model, movement sensor, sleep stages, apnea screening, or iOS
- No Room database yet: POC uses JSON metadata to reduce build complexity
- No physical Android overnight test yet; OEM battery behavior and microphone conflict handling are unverified
- No verified APK generated in the authoring environment (Android SDK/Gradle unavailable there)

## Quick-start GitHub workflow

1. Create a **private** empty repository named `semalam-android` under the GitHub account `one2pret`. Do not initialize it with a README or license.
2. Extract the provided `semalam-android-source.zip` locally.
3. Open Terminal in the extracted `semalam-android` directory and run:

   ```bash
   git init
   git branch -M main
   git add .
   git commit -m "feat: bootstrap Semalam audio feasibility prototype"
   git remote add origin https://github.com/one2pret/semalam-android.git
   git push -u origin main
   ```

4. Open GitHub repository → **Actions** → **Android Testing APK** → latest successful run → **Artifacts** → `semalam-android-testing-apk`.
5. Download ZIP artifact, extract `app-debug.apk`, and install it on the Android device.

**Important:** This workflow has been authored but **not executed**. The first CI run is the compile/build acceptance gate. Check CI logs and fix issues before claiming a verified APK.

## Testing package ID / signing

- Debug APK package: `dev.one2pret.semalam.prototype.debug`.
- Future production package: `dev.one2pret.semalam.prototype` (not released).
- Each fresh GitHub runner generates a **different default debug keystore**; new APKs may fail to update older ones with `INSTALL_FAILED_UPDATE_INCOMPATIBLE`.
- For repeatable in-place testing updates, configure GitHub Actions secrets for one persistent testing keystore (see `docs/ANDROID_BUILD_AND_SIGNING.md`) **before** wider distribution. Otherwise uninstall the previous testing APK first (this deletes its local recordings).
- Never commit `.jks`, secrets, or private sleep recordings.

## Local Android Studio

Import the project, install Android SDK Platform 35 and JDK 17, and sync Gradle. Gradle version must be 8.11.1. This archive includes `gradle/wrapper/gradle-wrapper.properties` but **does not include wrapper binaries** because Gradle is unavailable in the authoring environment. You can build with Gradle 8.11.1 installed on your machine (`gradle :app:assembleDebug`) or run the GitHub workflow. See `docs/ANDROID_BUILD_AND_SIGNING.md`.

## First smoke test

1. Open Semalam on a physical Android device and allow microphone access.
2. Tap **Mulai Tidur / Rekam** while the app is visible; wait **12–15 seconds** for room calibration.
3. Make a sustained sound for 2–3 seconds at a normal speaking volume; remain quiet for another 3 seconds.
4. Lock the screen, repeat the test, then press **Hentikan Perekaman** in the app or persistent notification.
5. Open the new session. Check whether the WAV clip appears, plays, and can be deleted.
6. Move to the longer overnight UAT only after this passes.

See `docs/UAT_7_NIGHTS.md` and `docs/ARCHITECTURE.md` for acceptance gates and operational safeguards.

## Safety/privacy

- App-private local files; `allowBackup=false`, platform data extraction rules exclude files.
- No internet permission or telemetry.
- Sessions must be explicitly started by the user; a reminder never starts the microphone.
- Obtain consent of other people who may be recorded.
- Do not place a charging phone under a pillow or under bedding.
- This is a sound-event recorder. It cannot establish sleep quality, sleep duration, sleep stage, or medical conditions.
