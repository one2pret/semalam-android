# Semalam architecture — proof of concept

```text
MainActivity (Compose UI)
  |-- Runtime permission (RECORD_AUDIO)
  |-- User action: Start / Stop
  |-- History / playback / manual labels / deletion
  |-- BedtimeSchedule preferences & TimePicker
                |
                v
RecordingService (foregroundServiceType=microphone)
  |-- Notification: recording indicator, Stop
  |-- Partial wake lock (12-hour maximum)
  |-- AudioRecord / PCM mono 16 kHz
  |-- SoundEventEngine (pure Kotlin)
  |     12-sec ambient sample (lower quartile RMS)
  |     threshold = clamp(background + 14 dBFS, -52 .. -28)
  |     >=4 loud buffers, 1-second pre-roll, 1.5-second trailing silence
  |     max clip length 22 sec; generic sound event only
  |-- WavFiles (PCM16 WAV)
  |-- SessionStore (JSON metadata + WAV files)
                |
                v
app-private filesDir/sleep_sessions/<UUID>/
  session.json
  clip_<UUID>.wav
```

## Session metadata

- `id`, `startedMs`, `finishedMs`, `lastHeartbeatMs`, `status`, `message`
- `clips[]`: `id`, `fileName`, `offsetMs`, `durationMs`, `label`
- Status values: `recording`, `completed`, `interrupted`, `auto_stopped`

## Reliability, risks, and non-goals

- Android 14+ imposes foreground-service microphone restrictions; the start action must occur while an Activity is visible.
- Foreground notification remains present while recording. Service is `START_NOT_STICKY`; a process kill is **not** silently recovered by restarting microphone capture.
- `SessionStore` updates heartbeat roughly every 30 seconds; next launch can mark a stale session as interrupted (after 2 minutes). Forced-stop may prevent a graceful status update.
- A partial wake lock avoids CPU suspension at the expense of battery. Service auto-stops at 12 hours. Test thermal and battery drain on physical hardware.
- Full audio is **not** stored. Only clips are saved when energy rises above threshold; quiet snoring may be missed.
- 240 clips/session safety cap, <=22 seconds per clip, and low-space protection (100MB free-space threshold). Cap reached does not stop session, but some events won't be saved.
- Consecutive or continuous noise can generate multiple adjacent events; no automatic classification or voice identity inference.
- No database migration system in POC; switch to Room before stable production release.
- Sound magnitude is relative to the phone/microphone, not calibrated decibels SPL.

## Next iterations

1. Physical device tests: lock screen, call/microphone conflict, process kill, low battery, file availability, event relevance.
2. Move metadata to Room and add application-level structured error logs that never include raw audio.
3. Add user-adjustable sensitivity and retention policy; verify upload/backup remains disabled.
4. Only after labeled examples, consider on-device classifier with held-out tests; no medical claims.
