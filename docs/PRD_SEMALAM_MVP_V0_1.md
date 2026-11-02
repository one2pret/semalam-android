# Semalam — PRD dan Rencana Implementasi MVP v0.1

**Status:** Draft untuk keputusan sebelum pembuatan repository  
**Tanggal:** 2 November 2026  
**Platform pertama:** Android native (Kotlin + Jetpack Compose)  
**Positioning:** Sleep Sound Diary + Sleep Acoustic Monitor  
**Tagline kerja:** Kenali suara dan kejadian menarik saat kamu tidur.

## 1. Ringkasan keputusan

Semalam membantu pengguna menemukan, mendengarkan, dan mencatat peristiwa suara di malam hari dengan pengalaman minimal: **Mulai Tidur → Letakkan HP → Hentikan sesi → Lihat cuplikan dan riwayat**. Aplikasi tetap berguna bila tidak ada kejadian suara yang terdeteksi. Aplikasi tidak menyatakan pengguna benar-benar tidur selama sesi berlangsung.

**Keputusan v0.1:** offline-first; tanpa akun, server, langganan, cloud AI, atau iklan; Android dahulu; rekam hanya setelah tindakan pengguna; simpan cuplikan kejadian, bukan rekaman utuh secara default; klasifikasi jenis suara secara manual; tidak ada diagnosis medis.

## 2. Masalah, target pengguna, dan nilai

**Masalah:** Pengguna tidak tahu apakah saat tidur mengeluarkan suara tertentu, sulit memutar rekaman panjang, dan belum mempunyai catatan peristiwa per malam yang mudah ditinjau.

**Pengguna awal:** (1) orang dewasa yang penasaran dengan suara tidurnya; (2) orang yang ingin memonitor perubahan dengkuran dan mengumpulkan catatan untuk didiskusikan dengan profesional jika perlu. Penggunaan dengan pasangan/teman sekamar membutuhkan persetujuan pihak yang mungkin ikut terekam.

**Pekerjaan utama (JTBD):** "Saat bangun, saya ingin tahu kejadian suara yang patut didengar selama sesi malam tadi, tanpa memeriksa rekaman berjam-jam."

**Hipotesis produk:** Rasa penasaran mendorong percobaan pertama; riwayat dan kemudahan menemukan cuplikan mendorong penggunaan berulang. Ini masih asumsi yang perlu dibuktikan.

## 3. Tujuan dan batasan

### Tujuan MVP
1. Merekam sesi audio selama layar terkunci dengan kendali eksplisit dan indikator yang jelas.
2. Menemukan segmen kejadian suara secara lokal dan menyimpan cuplikan terpilih.
3. Menampilkan timeline, memutar cuplikan, serta mengizinkan pengguna memberi label manual.
4. Memungkinkan melihat riwayat, menghapus cuplikan/sesi, dan mengelola penyimpanan.
5. Menilai keandalan teknis dan kegunaan melalui uji tujuh malam.

### Di luar cakupan v0.1
- Penentuan fase tidur, total waktu tidur sebenarnya, skor kualitas tidur, atau diagnosis sleep apnea.
- Deteksi otomatis dengkuran versus bicara dengan klaim akurasi tanpa dataset validasi.
- Transkripsi ucapan, pengenalan pembicara, terjemahan, atau "analisis mimpi".
- iOS, login, sinkronisasi cloud, media sosial, iklan, dan fitur premium.
- Perekaman diam-diam atau aktivasi otomatis pada jam tertentu.

## 4. Kebutuhan fungsional dan kriteria penerimaan

| ID | Prioritas | Kebutuhan | Kriteria penerimaan |
|---|---|---|---|
| FR-01 | P0 | Onboarding dan persetujuan mikrofon | Penjelasan jelas, izin diminta saat diperlukan; tanpa izin tidak ada perekaman. |
| FR-02 | P0 | Tombol Mulai Tidur | Hanya satu sesi aktif; saat dimulai aplikasi terlihat dan notifikasi perekaman hadir. |
| FR-03 | P0 | Perekaman saat layar terkunci | Sesi berlanjut selama kondisi OS/perangkat mengizinkan; interupsi tidak ditutup-tutupi. |
| FR-04 | P0 | Tombol Hentikan | Dapat dihentikan dari aplikasi dan notifikasi; mikrofon serta service dilepas. |
| FR-05 | P0 | Deteksi kejadian non-sunyi | Menggunakan ambang adaptif + hysteresis; menandai kejadian sebagai "suara terdeteksi", bukan diagnosis. |
| FR-06 | P0 | Simpan cuplikan | Ada waktu mulai, durasi, berkas lokal, dan konteks sebelum/sesudah kejadian. |
| FR-07 | P0 | Halaman laporan pagi | Menampilkan durasi sesi, banyaknya cuplikan, timeline, dan status sesi lengkap/terputus. |
| FR-08 | P0 | Playback | Pengguna bisa memutar, pause, dan berpindah cuplikan tanpa mengubah data asli. |
| FR-09 | P0 | Label manual | Pilihan: mirip dengkuran, mirip bicara, suara sekitar, lainnya, tidak yakin. |
| FR-10 | P0 | Riwayat | Menampilkan daftar sesi dengan tanggal, durasi sesi, dan jumlah cuplikan. |
| FR-11 | P0 | Hapus rekaman | Hapus cuplikan atau seluruh sesi termasuk file dan record database. |
| FR-12 | P0 | Penanganan interupsi | Panggilan/mikrofon direbut, storage penuh, crash, izin dicabut: ada status kesalahan/gap yang dapat dilihat. |
| FR-13 | P1 | Preferensi sensitivitas | Pilihan rendah/normal/tinggi dengan default normal, tidak diberi makna klinis. |
| FR-14 | P1 | Catatan pagi singkat | Pengguna dapat mencatat "merasa segar", kondisi kamar, atau catatan bebas secara opsional. |
| FR-15 | P0 | Privasi lokal | Tanpa pengiriman audio secara otomatis; audio privat dan dikecualikan dari backup otomatis. |

## 5. Alur UX dan rancangan layar

**Alur utama:** Onboarding → Beranda → Sesi aktif → Hentikan → Laporan pagi → Cuplikan → Riwayat.

### Screen A — Onboarding (pertama kali)
- Judul: "Apa yang terdengar saat kamu tidur?"
- Informasi: "Semalam merekam suara sekitar saat sesi aktif. Rekaman disimpan di HP ini."
- Pengingat persetujuan orang lain di kamar dan penjelasan batasan kesehatan.
- CTA: "Izinkan mikrofon"; tautan "Privasi dan penghapusan".

### Screen B — Beranda / mulai tidur
- Hero: ikon bulan, satu tombol besar **Mulai Tidur**.
- Status terakhir: "Sesi terakhir: tadi malam" atau "Belum ada rekaman".
- Panduan singkat: taruh HP di permukaan stabil, tidak tertutup bantal, sebaiknya terhubung daya sesuai keselamatan perangkat.
- Navigasi bawah: **Malam Ini**, **Riwayat**, **Pengaturan**.

### Screen C — Sesi aktif
- Menampilkan **Sedang merekam** dan penghitung durasi sesi.
- Indikator mikrofon dan notifikasi persistent dengan tindakan **Hentikan**.
- Layar boleh terkunci; status di layar bukan jaminan tidak ada interupsi.
- Tanpa grafik berlebihan yang mengganggu tidur.

### Screen D — Laporan pagi
- Judul: "Ini suara yang tertangkap semalam".
- KPI: durasi perekaman, jumlah cuplikan, status sesi.
- Timeline waktu kejadian dan daftar cuplikan dengan jam, durasi, playback, label.
- Empty state: "Tidak ada kejadian suara yang tersimpan; ini tidak berarti tidak ada dengkuran."
- Catatan pagi opsional dan tautan hapus rekaman.

### Screen E — Detail cuplikan
- Pemutar audio, jam/durasi, waveform sederhana, label manual, tombol hapus.
- Hindari menampilkan transkrip atau menebak siapa yang bersuara.

### Screen F — Riwayat & pengaturan
- Daftar sesi per tanggal; detail; opsi hapus per sesi/semua.
- Preferensi sensitivitas, periode penyimpanan (mis. 7/30 hari), penggunaan ruang, kebijakan privasi.
- Fitur share/export audio **tidak** ada dalam v0.1; dapat dinilai ulang setelah uji privasi.

**Gaya:** Tema gelap lembut untuk sebelum tidur; kontras cukup; tombol besar; label dalam Bahasa Indonesia; pembaca layar; teks bisa mengikuti ukuran font sistem. Tidak ada klaim "kualitas tidur" dari rekaman semata.

## 6. Arsitektur Android Kotlin

### Pilihan stack
- **UI:** Kotlin, Jetpack Compose, Navigation, ViewModel, StateFlow.
- **Service:** foreground service bertipe `microphone` untuk sesi berlangsung.
- **Audio:** `AudioRecord` untuk sampel PCM; sumber `UNPROCESSED` jika didukung atau fallback yang diuji, misalnya `VOICE_RECOGNITION`.
- **DSP:** penghitung RMS/energi dan estimasi noise floor, threshold adaptif, hysteresis, penggabungan kejadian dekat, circular buffer untuk pre-roll.
- **Berkas:** cuplikan AAC/M4A dengan encoder yang tersedia pada perangkat; metadata/file disimpan lokal di ruang privat aplikasi.
- **Database:** Room; preferensi dengan DataStore.
- **Pemutaran:** pemutar lokal Android yang sesuai; tidak memerlukan server.
- **Pengujian:** unit test algoritma, instrumented test, uji perangkat fisik minimal dua produsen/versi Android.

### Aliran data
`Compose UI → ViewModel/Use Cases → RecordingSessionController → Foreground Microphone Service → AudioRecord → EventDetector → ClipWriter → Room + private audio files → Report/History`.

Untuk memastikan tidak kehilangan konteks, rancang ring buffer sekitar 2–3 detik sebelum event dan 2–3 detik setelah event; nilai ini adalah **konfigurasi percobaan**, bukan aturan baku. Kelompokkan puncak yang berdekatan; beri batas durasi dan kuota cuplikan per sesi; beri penanda ketika batas tercapai. Hindari merekam file satu malam penuh secara default. Uji apakah suara kipas/AC membuat event tidak terkendali.

### Status dan ketahanan
`IDLE → STARTING → RECORDING → STOPPING → COMPLETED`; cabang `INTERRUPTED`, `FAILED`, `INCOMPLETE`. Simpan metadata sesi sejak mulai dan checkpoint berkala agar durasi/gap tetap dapat dipulihkan sesudah proses mati. Jangan otomatis mengaktifkan kembali mikrofon dari boot/background tanpa interaksi dan izin yang sah. Jika input audio disenyapkan OS, tampilkan masalah kualitas/rekaman, bukan hasil kosong yang menyesatkan.

### Permissions dan kebijakan
- `RECORD_AUDIO` setelah pemberitahuan tujuan penggunaan.
- `FOREGROUND_SERVICE` dan `FOREGROUND_SERVICE_MICROPHONE`, service `android:foregroundServiceType="microphone"` sesuai target API.
- Android 14+: microphone foreground service dimulai ketika Activity terlihat (kecuali pengecualian yang berlaku); bukan otomatis dari boot.
- Notifikasi sesi aktif, aksi stop, disclosure privasi jelas, deklarasi foreground service di Play Console saat distribusi.
- Health Apps Declaration: kategori **Sleep Management**, bila dipublikasikan di Google Play, dengan kebijakan privasi publik dan disclosure yang sesuai.

### Model data minimal
**SleepSession:** id, startedAt, endedAt, status, captureDurationSec, interruptionDurationSec, sourceDevice, createdAt, note, errorCode.  
**AudioEvent:** id, sessionId, startedAt, durationMs, audioPath, peakLevelRelative, eventSource (`ENERGY`), manualLabel, createdAt, qualityFlag.  
**UserSettings:** sensitivity, retentionDays, consentVersion, notificationPreference.  

Gunakan timestamp absolut dan zona waktu untuk tanggal tampilan. Jangan mengartikan durasi sesi sebagai durasi tidur. Tidak perlu entitas user/login.

## 7. Privacy, security, dan keselamatan

1. Seluruh pemrosesan dan penyimpanan v0.1 di perangkat; tidak perlu izin internet untuk fitur inti.
2. Audio mungkin memuat ucapan pribadi atau suara orang lain; beri pengungkapan jelas dan minta pengguna memastikan persetujuan orang sekitar.
3. Hindari backup otomatis cuplikan (atur Android data extraction/backup rules); gunakan penyimpanan internal aplikasi dan hapus file saat pengguna meminta.
4. Data tidak ditransmisikan otomatis, tidak direkam diam-diam, dan tidak berisi fitur "dengarkan orang lain".
5. Pengguna dapat menghapus satu cuplikan, satu sesi, atau seluruh data; retensi default 7 hari dapat diuji sebagai asumsi UX.
6. Tidak menampilkan skor medis/apnea atau menentukan fase tidur. Jika ada keluhan seperti jeda napas, terengah, kantuk berat, anjurkan evaluasi profesional.
7. Tidak mengumpulkan audio tester untuk developer tanpa pilihan dan persetujuan tersendiri. Untuk evaluasi manual, tester boleh menandai cuplikan di HP mereka dan mengirim angka/feedback tanpa audio.

## 8. Rencana pengujian fungsional (sebelum beta tujuh malam)

| Test | Skenario | Hasil yang harus terjadi |
|---|---|---|
| T-01 | Mulai sesi dan kunci layar 30 menit | Perekaman berjalan; indikator/notifikasi jelas. |
| T-02 | Hentikan via UI dan notifikasi | Mic berhenti, service selesai, sesi tersimpan sekali. |
| T-03 | Mic permission ditolak/dicabut | Tidak ada capture; penjelasan dapat dimengerti. |
| T-04 | Panggilan masuk/aplikasi lain menggunakan mikrofon | Status interrupted/silenced tercatat; tidak mengklaim sukses palsu. |
| T-05 | Terjadi reboot/force stop/tutup proses | Sesi ditandai incomplete; tidak start ulang diam-diam. |
| T-06 | Sisa ruang rendah | Peringatan dan penyelesaian aman; tidak merusak data lama. |
| T-07 | Ruangan sunyi, kipas, suara orang, simulasi audio terkontrol | Jumlah event dan false trigger ditinjau manual. |
| T-08 | Hapus cuplikan dan seluruh data | File fisik dan record Room hilang; tidak muncul kembali. |
| T-09 | Perubahan sensitivitas | Diterapkan pada sesi berikut, tidak mengubah data lama. |
| T-10 | Upgrade APK | Data terjaga bila `applicationId` dan signing key sama serta `versionCode` meningkat. |

## 9. Eksperimen tujuh malam

**Peserta:** 10–15 orang dewasa sukarela dengan perangkat Android beragam. Rekaman dan label tetap di perangkat; hasil riset dikumpulkan sebagai angka/feedback anonim atau pseudonim, tanpa audio. Perlu persetujuan nyata dari orang lain yang bisa ikut terekam.

**Pra-malam (Hari 0):** Tes izin/stop/playback 10–15 menit, instruksi jarak HP konsisten, cek kapasitas dan baterai, catat model/OS (jika tester bersedia). Uji interupsi dan telepon **pada siang hari**, jangan sengaja mengganggu tidur.

| Hari | Fokus | Aktivitas dan data yang dicatat |
|---|---|---|
| Malam 1 | Baseline | HP di lokasi biasa; catat berhasil mulai/stop, durasi, jumlah event. |
| Malam 2 | Keterulangan | Ulangi posisi dan kondisi semirip mungkin; catat perbedaan. |
| Malam 3 | Kebisingan sekitar | Catat ada/tidaknya kipas/AC/sumber suara lain tanpa memaksakan perubahan kondisi tidur. |
| Malam 4 | Keandalan layar terkunci | Perhatikan persistensi sesi, notifikasi, dan kegagalan/gap. |
| Malam 5 | Kualitas cuplikan | Label 5–10 cuplikan acak/terpilih, catat mana yang tidak relevan. |
| Malam 6 | Kegunaan | Uji kemudahan menemukan peristiwa dan menghapus rekaman. |
| Malam 7 | Retensi dan nilai | Rekam seperti biasa; survei singkat apakah aplikasi ingin terus dipakai dan mengapa. |

**Form feedback pagi (kurang dari 1 menit):** Apakah sesi berjalan? Berapa cuplikan ditinjau? Berapa cuplikan relevan? Ada interupsi/masalah baterai? Apakah hasil berguna (skala 1–5)? Satu komentar.

**Kriteria lulus sebagai target desain, bukan klaim:**
- 100% sesi memiliki persetujuan eksplisit dan akses stop; 0 insiden data/audio dibagikan otomatis.
- ≥90% sesi yang benar-benar dimulai selesai normal atau menampilkan penyebab interupsi dengan benar; evaluasi terpisah untuk sesi ≥6 jam.
- ≥80% dari sampel cuplikan yang ditinjau pengguna dianggap mengandung peristiwa suara yang relevan; laporkan berapa sampel dinilai dan distribusi perangkat, bukan hanya persentase.
- Minimal 50% peserta memakai aplikasi pada ≥4 dari 7 malam; setidaknya 60% tester memberi nilai kegunaan ≥4/5.
- 0 crash kritis yang menyebabkan kehilangan seluruh riwayat; seluruh permintaan hapus lolos pengujian.
- Bandingkan tambahan konsumsi baterai dengan idle baseline pada subset perangkat; tentukan batas final setelah uji teknis pendahuluan, jangan mengarang angka performa.

**Keputusan akhir:** GO apabila reliabilitas dan privasi lulus dan ada bukti penggunaan berulang; ITERATE bila perekaman stabil tetapi deteksi bising/tidak relevan; STOP/REDESIGN jika reliabilitas mikrofon/privacy buruk atau retensi hampir nihil.

## 10. Roadmap implementasi dan gerbang keputusan

| Tahap | Hasil nyata | Gate sebelum melanjutkan |
|---|---|---|
| 0 — Freeze PRD | PRD, daftar risiko, acceptance criteria, kebijakan data | Tidak ada klaim medis atau auto-record diam-diam. |
| 1 — Audio feasibility spike | APK minimal start/stop mic + notifikasi + screen lock | Berhasil pada 2 perangkat; status interupsi jelas. |
| 2 — Core recording | Service, state machine, checkpoint, audio event storage | Pengujian 6–8 jam tanpa crash kritis. |
| 3 — Event detection | Energi/noise-floor, cuplikan, batas storage | False positives terkendali pada tes sunyi/AC/suara. |
| 4 — UI & database | Compose Home, Active, Report, History, Settings; Room | Data benar setelah restart/update; delete berfungsi. |
| 5 — Internal UAT | Matriks T-01 s.d. T-10 dan dokumentasi bug | Tidak ada isu kritis yang belum diselesaikan. |
| 6 — Beta tujuh malam | Test 10–15 orang, metrik dan interview | Keputusan GO / ITERATE / STOP. |
| 7 — v0.2 jika GO | Klasifikasi suara on-device setelah label dan evaluasi | Laporan precision/recall per kelas pada data uji terpisah. |
| 8 — Distribusi/iOS jika ada bukti | Play internal/closed test, iOS feasibility | Kebijakan, signing, privacy dan support siap. |

**Tidak ada repository, branch, PR, deployment, atau APK yang dibuat oleh dokumen ini.** Semuanya menunggu keputusan sesudah feasibility dan cakupan dikunci.

## 11. Rencana struktur repository ketika sudah disetujui

```text
semalam-android/
  app/                               # bootstrap, DI, navigation
  core/audio/                        # AudioRecord, capture, codec
  core/detection/                    # event detection, calibration
  core/data/                         # Room, DataStore, repositories
  core/designsystem/                 # theme, shared components
  feature/recording/                 # Home, Active Session
  feature/report/                    # Morning Report, playback
  feature/history/                   # Session history
  feature/settings/                  # consent, privacy, deletion
  docs/
    PRD_SEMALAM_MVP_V0_1.md
    ARCHITECTURE.md
    ADR/0001-offline-first.md
    AUDIO_PRIVACY.md
    UAT_7_NIGHTS.md
    RELEASE_WORKFLOW.md
  .github/workflows/                   # build, lint, test, artifact
  README.md
```

**Alur branch:** `main` stabil; `develop` integrasi opsional; `feature/*` → PR → lint/test/build → review → merge. Hindari workflow kompleks sebelum tim memerlukannya. Pisahkan `applicationId` internal-test dari production jika keduanya akan berdampingan. Untuk update pada aplikasi terpasang, pertahankan package/applicationId, signing key, dan tingkatkan `versionCode`. Simpan signing key di tempat aman, bukan repository.

## 12. Risiko dan mitigasi

| Risiko | Dampak | Mitigasi |
|---|---|---|
| Mic dimatikan/disenyapkan sistem atau aplikasi lain | Sesi tampak lengkap tapi tanpa audio | Callback capture-state, interruption flag, sesi tidak boleh salah diberi status lengkap. |
| Suara AC/kipas/pasangan terbaca sebagai kejadian | Cuplikan tidak relevan | Noise floor adaptif, label manual, evaluasi sampel. |
| Penyimpanan/baterai habis | Kegagalan sesi | Simpan event-only, quota, validasi sisa ruang, stop aman. |
| Pengguna menyangka diagnosis medis | Salah mengambil keputusan kesehatan | Bahasa "kejadian suara" dan disclaimer; jangan skor apnea/tidur. |
| Percakapan privat orang lain terekam | Risiko privasi | Consent jelas, tidak ada cloud/upload otomatis, delete, exclude backup. |
| APK update bentrok/signature berubah | Gagal install, data berisiko | Atur package, signing, flavor, versionCode sejak awal. |
| Terlalu banyak fitur | MVP lambat dan sulit diuji | Bekukan scope dan keputusan gate per fase. |

## 13. Keputusan akhir sebelum repository

**Sudah direkomendasikan:** Kotlin native Android; offline-first; fokus event audio; satu tombol mulai; label manual; Room + privat audio; tujuh malam; no medical claims.

**Masih perlu dibuktikan:** perilaku mic semalaman lintas perangkat, kualitas event detection, tambahan penggunaan baterai, relevansi cuplikan, retensi 7 malam.

**Tunda hingga sebelum rilis publik:** clearance nama Semalam/merek/domain, final application ID, desain logo, kebijakan privacy publik, klasifikasi Play Console Sleep Management, strategi support dan distribusi.

## 14. Referensi teknis dan klinis

- Android foreground service microphone: https://developer.android.com/develop/background-work/services/fgs/service-types
- Android background start limits: https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start
- Android microphone input sharing: https://developer.android.com/media/platform/sharing-audio-input
- Android media recording: https://developer.android.com/media/platform/mediarecorder
- Android architecture: https://developer.android.com/topic/architecture/recommendations
- Android backup exclusions: https://developer.android.com/identity/data/autobackup
- Google Play Health App declarations: https://support.google.com/googleplay/android-developer/answer/14738291
- Google Play Health Content & Services policy: https://support.google.com/googleplay/android-developer/answer/16679511
- AASM position statement (consumer sleep technology): https://pmc.ncbi.nlm.nih.gov/articles/PMC5940440/
