# Semalam v0.1-alpha02 — Smoke Test & Storage Safety

## What changed
- Fix stale "Memulai/Menghentikan" UI messages via recorder phase.
- Show 12-second calibration progress, live relative microphone dBFS, and detected/saved counts.
- Guard clip label/delete during active recording; failed file delete now returns an error without silently removing metadata.
- Show total local audio/metadata storage; faster buffered WAV writing.
- Treat prolonged all-zero mic input as interrupted, not completed.

## Installation safety
- Alpha02 testing package is `dev.one2pret.semalam.prototype.alpha02`, intentionally different from alpha01's `dev.one2pret.semalam.prototype.debug` because GitHub Actions debug signing is not yet stable.
- Both apps can coexist. **Do not uninstall alpha01 until its saved clips are no longer needed.**
- No migration between app sandboxes; alpha01 recordings are not automatically visible in alpha02.
- This is an interim test flavor. Later update-in-place builds require stable signing credentials stored as encrypted GitHub Secrets, never in this public repository.

## UAT A — recording and calibration
1. Install alpha02 alongside alpha01; open alpha02 and grant microphone permission.
2. Tap Start, verify calibration progress to 100% over ~12s.
3. Speak clearly 3–5s, pause 4–5s. Verify input level and event/saved count >0.
4. Stop. The screen must show ready (no stale stopping message).
5. Replay a clip and confirm it is understandable.
6. Re-test with screen locked for 30m (permission, active foreground notification, playback).

## UAT B — deletion and storage
1. In a completed session, note the total local storage display.
2. Delete one clip; ensure it disappears, cannot play, and used bytes decrease.
3. Delete the entire completed session; ensure all clips and metadata disappear.
4. While recording, label/delete should be disabled. Verify no crash.
5. No automatic deletion is configured yet; retain explicit user control.

## UAT C — battery and temperature
- Measure battery % at start/end over 30min, screen locked, same location; compare with an idle-phone baseline.
- Note whether charging, airplane mode, power saving, and room fan/AC were on.
- Record number of clips, app storage before/after, battery drop, unusual heat, interruptions.
- Continue full-night UAT only if 30min recording reliably works and the device remains safe.

## Known limitations
- Sound event detection does **not** identify snoring or sleep disorders.
- dBFS is digital input level, not measured room loudness in dB SPL.
- Deleting files while the app's process crashes or storage fails is not yet transactionally atomic; treat UI errors seriously.
- Stored WAV clips are uncompressed, at most 240 per session. No global quota or auto-retention yet.
- SessionStore remains JSON-based POC; Room migration is deferred.
