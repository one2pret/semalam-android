# Semalam alpha07 — Playback Audio Boost & microphone acoustic envelope

## Audio Boost
- Kontrol 0 / +3 / +6 / +9 dB di panel pemutar kelompok.
- `android.media.audiofx.LoudnessEnhancer` diterapkan pada audio session MediaPlayer. Tidak mengubah atau mengganti file WAV; tidak mengatasi pembatas volume perangkat.
- 0 dB default **setiap membuka aplikasi**; nilai tidak disimpan agar tidak terjadi lonjakan suara antar-sesi.
- Jika efek tidak didukung, aplikasi kembali ke 0 dB dan menampilkan pesan.
- **Peringatan:** naikkan volume secara hati-hati. Audio Boost tidak menyediakan hard brick-wall limiter di semua perangkat; terdapat risiko clipping dan suara mendadak keras. Jangan menguji dengan earphone pada volume maksimal.

## Envelope mikrofon
- Saat perekaman user-initiated aktif, setiap kira-kira 5 detik menyimpan rata-rata dan puncak level digital **dBFS**, bukan dB SPL, dari input yang sama dengan detektor.
- Disimpan sebagai `sleep_sessions/<session-id>/acoustic_levels.csv` di private storage; batch flush tiap ~60 detik dan saat sesi berhenti.
- Tidak menyimpan full-night PCM, tidak mengunggah audio; batas 12 jam, kurang dari 600 KB CSV.
- Grafik fluktuasi baru muncul untuk sesi yang dibuat di alpha07 atau lebih baru. Riwayat alpha06 tetap tampil melalui peta aktivitas dari cuplikan.
- Ketuk grafik untuk memfilter kelompok kejadian pada bin 15 menit. Tidak mengeklaim bahwa lonjakan itu dengkuran.

## UAT-007
1. Pasang APK signed alpha07 **di atas alpha06**; pastikan sistem melakukan Update, bukan instalasi aplikasi kedua. Rekaman, label, dan settings alpha06 tidak berubah.
2. Putar cuplikan pendek dengan **speaker HP volume rendah**; tes 0/+3/+6/+9 dB bertahap dan kembali 0. Pastikan audio tidak pecah pada level yang dipakai.
3. Uji auto-next, pindah kelompok, stop, dan kembali ke aplikasi: gain kembali 0 dB setelah aplikasi dibuka ulang.
4. Buat sesi baru selama 3–5 menit. Buat 2–3 kejadian suara dengan jeda hening, lalu berhenti; grafik fluktuasi muncul dan mempunyai batang berbeda.
5. Ketuk salah satu area grafik: peta kelompok tersaring ke rentang 15 menit; cuplikan lama tetap dapat diputar.
6. Pastikan satu sesi lama alpha06 tidak memiliki grafik baru (penjelasan kompatibilitas), namun daftar lama tetap utuh.
7. Tes durasi layar terkunci dan auto-stop pada malam berikutnya; periksa pertambahan penyimpanan dan suhu perangkat.

## Batas
Semalam belum mampu mendiagnosis gangguan tidur, membedakan otomatis mengigau/dengkuran atau menyaring musik YouTube. Peak dBFS relatif dan skor prioritas tidak boleh dilabeli dB SPL/keparahan medis. Level envelope bukan audio utuh; potongan hening tidak dapat didengarkan kembali.
