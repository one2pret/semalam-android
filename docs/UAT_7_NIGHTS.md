# Semalam testing plan — physical device

**Scope:** 0.1.0-alpha01 audio feasibility. This is not a sleep-medical validation study. Do not infer diagnosis.

## Gate A — before overnight trials

- [ ] APK built by CI successfully; unit tests green.
- [ ] APK installs on Android phone; package `dev.one2pret.semalam.prototype.debug`.
- [ ] Permission denial correctly blocks recording.
- [ ] Recording can begin when the app is open, notification appears.
- [ ] Wait 12–15s; speak/make sound 2–3s; a WAV clip appears and plays.
- [ ] Lock screen, record another event; clip can be played later.
- [ ] Stop via app and via notification; microphone stops and session reports completion.
- [ ] File and session deletion remove media and metadata.
- [ ] Reminder setting triggers a notification near its scheduled time; opens app, never turns on mic automatically.
- [ ] Re-test with notifications disabled: record still explicitly controllable.
- [ ] Safe charging/placement: phone open on stable bedside surface with ventilation, not under pillow.

If Gate A fails, do not begin 7-night study. Fix recording stability first.

## Seven-night observation log

| Night | Test goal | Observable result |
|---|---|---|
| 1 | Baseline room noise | start, stop, clips count, file playback |
| 2 | Lock screen all night | recording completion, any error and gap |
| 3 | Fan / AC in room | false positives, continuous noise behaviour |
| 4 | Bedtime reminder | reminder shown, started manually, schedule preserved |
| 5 | Normal sound variability | relevancy of user-reviewed clips |
| 6 | History and deletions | delete sample, retain older sessions, storage check |
| 7 | Repeat complete routine | trend in battery use, false positives, ease of use |

## Daily notes template

- Date, phone model, Android version, app version
- Approximate phone-to-bed distance; fan/AC/traffic conditions
- Start and stop timestamps; elapsed recording session (not actual sleep time)
- Battery at start / end; power mode; charger connected?; phone temperature perception
- Number of clips; number manually judged relevant; false events; obvious missed events if independently observed
- Screen locked for how long? Any app crash or silent recording interval?
- Free storage before / after; total session file size
- Morning usefulness rating 1–5 and one open comment

## Proposed technical acceptance thresholds

- 7 intended overnight sessions are stored with honest completed/interrupted statuses (target 100% visibility).
- At least 6/7 sessions complete without unexplained recorder termination (target for pilot, not a proven reliability estimate).
- Playback and deletion pass in all manually tested sessions.
- No upload of sound data, no recording without user action.
- Review at least 30 clips across nights, aiming for >=80% that contain the sound event users wanted to hear. This tests **event relevance**, not snoring-classifier accuracy.
- Battery drain and storage growth measured, not predetermined; decide thresholds only after device baseline.

## Go / iterate / stop

**GO** to small beta if recording stability is adequate and users find meaningful clips.
**ITERATE** if audio works but too many clips are false positives, or battery/storage unacceptable.
**STOP** feature growth if reliable overnight recording cannot be achieved on the target Android device.
