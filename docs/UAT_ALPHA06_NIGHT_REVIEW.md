# Semalam alpha06 — Night Review

## Fitur
- Peta aktivitas sesi dalam bin **15 menit**; ketuk batang untuk memfilter kejadian yang waktu rekamannya berada pada rentang tersebut.
- Cuplikan yang terpisah <=30 detik ditampilkan sebagai **kelompok virtual**, maksimum rentang 2 menit per kelompok. WAV/metadata asli tidak diubah.
- Peringkat indeks aktivitas 0–100 (gabungan puncak amplitudo WAV tersampel, durasi clip dan banyaknya cuplikan). Indeks BUKAN tingkat bahaya, diagnosis, atau peluang dengkuran.
- Peta hanya berdasarkan **rekaman yang disimpan**. Tidak ada baseline continuous-room audio sebelum/di antara cuplikan, sehingga tidak boleh memberi klaim grafik kondisi akustik kamar pada bagian tidak direkam.
- Pengguna dapat mengurutkan berdasarkan Menonjol, Waktu, Terbaru, Terpanjang; pilih grup untuk melihat clip; tombol Putar kelompok mengantrekan audionya.
- Amplitudo dihitung di background menggunakan pembacaan sampel terbatasi dari WAV lama; bila file hilang/malformed puncak default 0 dan playback akan menampilkan error, tanpa crash yang disengaja.
- Semua fitur alpha05 dan keystore stabil dipertahankan; versionCode 6 dan applicationId tetap.

## UAT-006
1. Pastikan alpha05 signed telah terpasang dan memiliki rekaman, label, dan pengaturan. Install alpha06 signed langsung di atasnya **tanpa uninstall**, verifikasi data tetap.
2. Buka sesi panjang (mis. >100 cuplikan). Peta aktivitas tampil; tap interval; daftar kelompok menyempit. Ketuk rentang sekali lagi / reset untuk melihat semua.
3. Pilih kelompok, cuplikan yang tergabung muncul di bawah. Play All / Putar kelompok dan waveform bekerja; stop/pergantian sesi/filter menghentikan audio lama.
4. Urutkan Menonjol/Waktu/Terbaru/Terpanjang; hasil urutan berubah. Periksa setiap waktu yang ditampilkan.
5. Hapus satu cuplikan terlabel yang tidak diperlukan: jumlah cuplikan, kelompok dan peta berubah sewaktu layar refresh, tapi rekaman lain tetap.
6. Rekam sesi baru dan uji mode Jadwal Berhenti; pastikan perekam tak terganggu oleh pembacaan amplitude sesi selesai.
7. Ingat bahwa peak yang dihitung hanya sampel terbatas, bukan nilai maksimum tepat ataupun klasifikasi dengkuran. Jangan menyimpulkan tidak ada suara pada jam tanpa clip.

## Non-goals
Tidak menggabungkan/menghapus WAV fisik, tidak mengganti detektor suara, tidak membuat full-night background acoustic envelope, tidak mengukur apnea/tahap tidur, tidak auto-label AI. Uji performa perangkat (200+ clip) penting sebelum promosi luas.
