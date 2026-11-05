# UAT Semalam alpha08 — Hapus audio saja, arsipkan laporan

## Dua tindakan berbeda

1. **Hapus audio saja (pertahankan laporan)**: menghapus file WAV di sesi selesai, mempertahankan `session.json`, waktu/durasi, clip IDs, label, jumlah cuplikan, grup kejadian, dan `acoustic_levels.csv` untuk grafik fluktuasi alpha07. Puncak sampel tiap klip disalin ke metadata **sebelum penghapusan** sehingga indeks aktivitas tetap tersedia setelah file WAV hilang. Arsip tidak dapat diputar.
2. **Hapus seluruh sesi dan laporan**: menghapus keseluruhan folder sesi termasuk WAV, JSON, ringkasan akustik, grafik, label. Tidak dapat dipulihkan tanpa cadangan.

Operasi dilakukan ketika sesi selesai, melalui dialog konfirmasi terpisah. Playback dihentikan sebelum penghapusan. Pembersihan berjalan di background I/O agar UI tidak macet.

## Kompatibilitas
- Metadata `audioDeleted` (default false untuk rekaman alpha07) dan `sampledPeak` (nullable) ditambahkan secara opsional ke JSON sesi. Legacy JSON tetap bisa dibaca.
- Pada audio-only delete, semua WAV sesi dihapus tetapi daftar `SoundClip` tetap ditampilkan sebagai riwayat. Tombol Play atau Putar Kelompok disembunyikan setelah terarsip, label tetap bisa diperbaiki.
- Sesi dari alpha06 yang tidak mempunyai `acoustic_levels.csv` tetap memiliki histogram jumlah cuplikan dan kelompok; **grafik fluktuasi penuh tidak bisa direkonstruksi** setelah audio dihapus.
- Audio-only delete tidak dijalankan saat sesi masih direkam. Jika proses gagal sebagian, UI menunjukkan kegagalan. Jalankan kembali ketika aman. Jangan menganggap WAV sudah terhapus sampai operasi sukses.
- `applicationId` stable testing sama, versionCode naik ke 8, signed APK dari trusted `main`.

## UAT-008
1. Pastikan **alpha07 signed** sudah ada, memiliki sesi dengan beberapa WAV dan grafik fluktuasi, kemudian update ke alpha08 signed **tanpa uninstall**.
2. Buka sesi sebelumnya: dengarkan WAV dan catat jumlah klip, label, indeks aktivitas, serta grafik.
3. Pilih **Hapus audio saja (pertahankan laporan)**. Konfirmasi; pastikan penyimpanan lokal turun dan ada status `Audio dihapus`.
4. Pastikan tanggal/durasi, jumlah klip, label, histogram/peta aktivitas, kelompok kejadian dan indeks aktivitas tetap **sama**; grafik fluktuasi alpha07 masih muncul.
5. Pilih suatu kelompok arsip: tidak ada tombol Putar Kelompok, Play All, atau Putar yang masih aktif untuk WAV terhapus. Label tetap tersedia.
6. Buat sesi baru dan lakukan perekaman, audio/pemutaran bekerja seperti biasa. Pastikan penghapusan sesi lain tidak mengganggu.
7. Pilih **Hapus seluruh sesi dan laporan** hanya pada sesi uji yang aman untuk dihapus; pastikan seluruh item dan grafik hilang.
8. Uji penghapusan dengan storage terbatas atau file yang hilang secara manual jika dapat disimulasikan aman; penghapusan sebagian harus ditampilkan gagal dan bisa diulang.

## Risiko yang perlu diverifikasi
- Pembaruan tidak boleh mengubah keystore, package ID, atau menghapus data lama.
- Arsip grafik dihitung dari metadata (jumlah clip, label, puncak sampel) dan, jika ada, file CSV akustik lima detik. Waveform detail tiap klip TIDAK dipertahankan.
- Penghapusan WAV permanen; belum tersedia restore atau backup.
