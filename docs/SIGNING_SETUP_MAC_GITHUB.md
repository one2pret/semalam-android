# Semalam — Keystore & update APK di MacBook / GitHub

Status: konfigurasi dirancang, **tidak ada secret atau kunci pribadi dalam repository**. Jangan unggah .jks, kata sandi, atau base64 ke chat, issue, pull request, log, atau file publik.

## 1. Siapkan MacBook

Buka Terminal:

```bash
brew install gh
gh auth login
java -version
keytool -help
mkdir -p "$HOME/.android/semalam"
chmod 700 "$HOME/.android/semalam"
```

Jika `java` atau `keytool` belum tersedia, instal JDK 17 terlebih dahulu. Buat key **di Mac sendiri** dan masukkan password melalui prompt interaktif:

```bash
keytool -genkeypair -v \
  -keystore "$HOME/.android/semalam/semalam-testing.jks" \
  -alias semalam-testing \
  -keyalg RSA -keysize 3072 -validity 10000 \
  -storetype PKCS12 \
  -dname "CN=Semalam Testing, O=One2pret, C=ID"
```

Gunakan kata sandi kuat. Untuk build Android, **gunakan password key yang sama dengan store** saat diminta; jangan menaruhnya di command line. Simpan key pada penyimpanan terenkripsi dan buat cadangan offline yang aman.

## 2. Upload sebagai GitHub Actions Secrets dari Terminal Mac

```bash
gh auth status
gh secret set SEMALAM_TESTING_KEYSTORE_BASE64 -R one2pret/semalam-android < <(base64 < "$HOME/.android/semalam/semalam-testing.jks" | tr -d '\n')
gh secret set SEMALAM_TESTING_KEY_ALIAS -R one2pret/semalam-android --body "semalam-testing"
```

Untuk password, **hindari password dalam argument CLI**. Gunakan prompt senyap berikut pada terminal **zsh** MacBook (tidak menulis password ke history):

```zsh
read -rs 'SEMALAM_PW?Masukkan password keystore: '
printf '\n'
printf '%s' "$SEMALAM_PW" | gh secret set SEMALAM_TESTING_STORE_PASSWORD -R one2pret/semalam-android
printf '%s' "$SEMALAM_PW" | gh secret set SEMALAM_TESTING_KEY_PASSWORD -R one2pret/semalam-android
unset SEMALAM_PW
```

Variabel yang harus diatur:

- SEMALAM_TESTING_STORE_PASSWORD
- SEMALAM_TESTING_KEY_PASSWORD

Jika key password sama dengan store password, isi keduanya dengan nilai yang sama. Periksa NAMA secrets saja dengan `gh secret list -R one2pret/semalam-android`; jangan pernah menampilkan nilainya.

## 3. Jalankan GitHub Actions setelah PR signing digabungkan

Di repo GitHub, Actions > Signed Semalam Testing APK > Run workflow > main. Workflow **gagal dengan jelas** jika salah satu secret belum tersedia. Hanya build dari main yang memakai signing key; PR memakai workflow debug biasa.

## 4. Aturan update dan data

Semalam memakai identitas testing baru yang permanen:
`dev.one2pret.semalam.prototype.testing`.

- Versi pertama di identitas ini: versionCode 4.
- Berikutnya gunakan versionCode 5, 6, dst. dan signing alias/key yang **sama**.
- alpha01/02/03 merupakan **aplikasi berbeda**, data tidak otomatis berpindah. Jangan uninstall sebelum menyimpan/menghapus rekaman yang diperlukan.
- APK yang ditandatangani kunci lain tidak bisa meng-update aplikasi ini. **Jangan kehilangan keystore cadangan.**
- Jangan menganggap signed testing APK sebagai rilis Google Play production.

Sebelum CI dianggap siap, pasang APK signed pertama, buat sebuah sesi dan label, lalu install versi upgrade bertanda tangan sama (versionCode lebih tinggi). Pastikan sesi dan file masih ada.
