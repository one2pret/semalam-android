# Semalam alpha03 — dashboard, kategori, jadwal berhenti

Fungsi utama: pengguna merekam kejadian suara. Pengelompokan dengkuran, bicara saat tidur, dan suara luar berbasis **label manual sesudah mendengarkan**, BUKAN identifikasi AI/diagnosis medis. Tidak dapat memastikan pemilik suara atau gangguan tidur.

Auto-stop: dipilih sebelum memulai sesi (default off; jam default 06:00). Perekaman harus dimulai secara manual. Saat service aktif, deadline berikutnya dalam waktu lokal dicek saat tiap loop read; bila tercapai, engine menyimpan cuplikan dan service berhenti. Sesi/status dan file tetap ada. Cap pengamanan 12 jam tetap berlaku.

Paket alpha03 terpisah dari alpha02 karena signing debug CI tidak stabil: dev.one2pret.semalam.prototype.alpha03. File alpha02 tidak bermigrasi. Jangan uninstall alpha02 sebelum data diperlukan selesai diperiksa.

## UAT-004
1. Rekam selama 1 menit, tunggu kalibrasi, buat beberapa suara, hentikan, lalu putar cuplikan.
2. Beri label Mirip berbicara, Mirip dengkuran, Suara sekitar sesuai audio; dashboard harus segera mengubah angka sesuai label.
3. Uji filter kategori dan tombol hapus, pastikan angka berubah.
4. Aktifkan jadwal berhenti 2-3 menit mendatang, mulai rekam dan kunci layar; pastikan setelah batas service berhenti dan cuplikan sebelumnya masih dapat diputar.
5. Uji pengingat waktu tidur tetap hanya notifikasi, tidak menyalakan mikrofon otomatis.
6. Uji suara YouTube dari speaker; label secara manual; belum ada fitur penyaring musik.

Rencana berikutnya: pengenalan akustik otomatis *hanya setelah* dataset berlabel, model on-device, dan pengujian precision/recall. Tidak ada klaim apnea atau fase tidur.
