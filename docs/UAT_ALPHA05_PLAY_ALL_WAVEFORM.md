# Semalam alpha05 — Pemutar antrian dan waveform

## Lingkup
- **Play All** sesi yang dipilih mengikuti filter yang sedang aktif. Seluruh cuplikan tetap tersimpan lokal.
- Saat cuplikan selesai, pemutar bergerak otomatis ke cuplikan berikutnya. Tombol sebelumnya, berikutnya, jeda/lanjut, dan stop.
- Pemutar menunjukkan posisi cuplikan dan jumlah total di antrian.
- Saat cuplikan diputar, waveform WAV tampil dengan progres berwarna. Tampilan ini bukan spectrogram dan **bukan** AI pendeteksi mendengkur.
- Tidak mengubah struktur penyimpanan sesi JSON atau WAV, tidak memigrasi/menghapus data.
- Identitas APK testing tetap `dev.one2pret.semalam.prototype.testing`; versionCode naik dari **4** ke **5**. GitHub Signed Testing Workflow di main memakai keystore yang sama.

## UAT-005: Upgrade tanpa uninstall
1. Dengan alpha04 signed terinstal, buat cuplikan sesi pendek; beri label dan pastikan bisa diputar.
2. Instal alpha05 **signed testing** dari workflow Signed Semalam Testing APK pada `main`. Jangan memakai artifact PR biasa atau APK debug sementara.
3. Periksa Android menunjukkan update, bukan aplikasi terpisah.
4. Periksa cuplikan alpha04, label, status sesi, dan pengaturan auto-stop tetap tersedia.

## UAT-005: Pemutaran
1. Siapkan sesi minimal 3 cuplikan, tekan Play All; rekaman harus berjalan berurutan tanpa tekan manual tiap klip.
2. Saat sedang memainkan klip, lihat waveform dan progres. Coba Jeda → Lanjut → Berikutnya → Sebelumnya → Stop.
3. Pilih filter Belum ditinjau; Play All hanya mencakup klip di filter itu.
4. Beralih sesi atau filter saat memutar; antrian berhenti, tidak ada audio lama yang terus berjalan.
5. Hapus klip: antrian berhenti sebelum penghapusan. Uji playback pada klip hilang, tidak crash.
6. Uji 150+ cuplikan, navigasi dan memori perangkat tetap wajar.
7. Uji saat layar mati: playback biasa boleh mengikuti kebijakan Android. Pemutaran background yang panjang/MediaSession bukan lingkup alpha05.

## Batas
Belum ada tombol memberi label langsung dari pemutar yang tetap tinggal pada satu panel; belum ada pengelompokan klip berdekatan. Klasifikasi AI dengkuran/bicara/lingkungan belum diimplementasikan. Jumlah kategori dashboard tetap berdasarkan label manual.
