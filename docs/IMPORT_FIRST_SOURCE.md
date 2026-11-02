# Import source pertama Semalam (Android)

Target: branch `feature/semalam-audio-prototype-v0.1` pada `one2pret/semalam-android`.

Repository ini diinisialisasi melalui konektor GitHub, tetapi unggahan source lengkap tetap memerlukan satu push dari Mac karena file ZIP lokal belum dapat dikirim langsung lewat konektor.

1. Unduh `semalam-android-source-v0.1.0-alpha01.zip` dari percakapan ChatGPT ke folder Downloads.
2. Buka Terminal Mac dan jalankan perintah berikut:

```bash
cd ~/Downloads
unzip -q semalam-android-source-v0.1.0-alpha01.zip -d semalam-import

git clone -b feature/semalam-audio-prototype-v0.1 https://github.com/one2pret/semalam-android.git semalam-git
rsync -av semalam-import/semalam-android/ semalam-git/
cd semalam-git
git add -A
git commit -m "feat: import native audio prototype v0.1 alpha01"
git push origin feature/semalam-audio-prototype-v0.1
```

3. Setelah push, cek Actions → Android Testing APK. Workflow ada di source yang diimpor dan seharusnya terpicu pada Pull Request ke `main` (bukan push ke branch feature pada konfigurasi awal).
4. Buka PR draft dari feature ke main, lalu periksa CI. Jangan merge atau distribusikan production sebelum build dan UAT lulus.

**Catatan:** Jangan tambahkan folder luar `semalam-android` di root repo; yang harus muncul di root adalah `app/`, `docs/`, `gradle/`, `build.gradle.kts`, dll. Repo saat ini public; jangan commit rekaman suara asli, file keystore, atau secret.
