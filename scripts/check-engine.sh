#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
if ! command -v kotlinc >/dev/null; then
  echo "kotlinc belum tersedia. Jalankan Gradle test dengan Android Studio / GitHub Actions."
  exit 2
fi
TMP=$(mktemp -d)
trap 'rm -rf "$TMP"' EXIT
cat > "$TMP/SmokeTest.kt" <<'KOTLIN'
import dev.one2pret.semalam.SoundEventEngine
fun main() {
  var count = 0
  val e = SoundEventEngine(onEvent = { _, _, _ -> count++ })
  repeat(190) { e.accept(ShortArray(1024) { 40 }) }
  check(e.isCalibrated)
  repeat(25) { e.accept(ShortArray(1024) { 9000 }) }
  repeat(30) { e.accept(ShortArray(1024) { 40 }) }
  e.finish()
  check(count == 1) { "Expected 1 event, actual $count" }
  println("PASS: Audio event segmenter (synthetic samples)")
}
KOTLIN
kotlinc app/src/main/java/dev/one2pret/semalam/SoundEventEngine.kt "$TMP/SmokeTest.kt" -include-runtime -d "$TMP/smoke.jar"
java -jar "$TMP/smoke.jar"
