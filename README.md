# Katalog Film — Android Studio + Node.js + MySQL

Aplikasi Android native Java/XML untuk katalog film TMDB. Fitur aplikasi: onboarding, login/daftar, login Google, reset password melalui email, mode tamu, pilihan genre, beranda, pencarian suara, detail film, trailer, rating/ulasan, watchlist, profil, notifikasi, dan pengaturan.


## Deploy backend online tanpa IP laptop

Untuk memakai backend dari HP atau pengguna lain melalui internet, deploy folder `backend/` ke Railway dan tambahkan service MySQL. Panduan lengkap ada di [`backend/DEPLOY-RAILWAY.md`](backend/DEPLOY-RAILWAY.md). Setelah Railway membuat domain HTTPS, ubah `BACKEND_BASE_URL` di `local.properties` menjadi domain itu, lalu lakukan Gradle Sync dan build ulang APK.

## 1. Buka dan jalankan Android

1. Ekstrak ZIP, lalu buka folder `KatalogFilm` (folder yang berisi `settings.gradle`) di Android Studio.
2. Pakai JDK 17 atau lebih baru dan Android SDK Platform 35. Android Studio akan mengunduh komponen Gradle/dependensi saat Gradle Sync.
3. File `local.properties` sudah disertakan. File ini berisi `TMDB_API_KEY` yang diminta, URL backend emulator, serta tempat untuk Google Web Client ID.
4. Jalankan MySQL dan backend terlebih dahulu (lihat bagian 2), lalu jalankan emulator Android.
5. Untuk Android Emulator, URL backend default `http://10.0.2.2:3000/` sudah sesuai. Untuk HP fisik, ubah `BACKEND_BASE_URL` di `local.properties` menjadi alamat IP LAN laptop/PC, misalnya `http://192.168.1.10:3000/`. HP dan komputer harus berada di jaringan yang sama.
6. Pilih konfigurasi `app`, lalu tekan Run.

`local.properties` bersifat lokal dan sudah masuk `.gitignore`. Jangan commit file itu ke repository publik. Android Studio biasanya mengisi `sdk.dir` otomatis. Bila Android Studio meminta lokasi SDK, biarkan Android Studio menambah `sdk.dir` milik komputer tersebut; jangan menyalin lokasi SDK dari perangkat lain.

## 2. Jalankan backend Node.js + MySQL

Persyaratan: Node.js 20 atau lebih baru dan MySQL 8.x.

### Siapkan database

1. Buka phpMyAdmin atau MySQL Workbench.
2. Import file `backend/sql/schema.sql`. File itu membuat database `katalog_film` dan tabel `users`.
3. Pastikan layanan MySQL aktif.

### Konfigurasi backend

Di terminal:

```bash
cd backend
```

Salin `backend/.env.example` menjadi `backend/.env`. Isi konfigurasi MySQL sesuai instalasi lokal, kemudian buat JWT secret acak minimal 32 karakter. Contoh membuat secret:

```bash
node -e "console.log(require('node:crypto').randomBytes(48).toString('hex'))"
```

Masukkan hasilnya ke `JWT_SECRET` dalam `.env`. Jangan bagikan atau commit `.env`.

Install dependensi dan jalankan:

```bash
npm install
npm run dev
```

Untuk menjalankan tanpa mode watch:

```bash
npm start
```

Cek backend melalui `http://localhost:3000/api/health`. Respons sukses menunjukkan `status: ok` dan `database: connected`.

## 3. Aktifkan Login dengan Google

Login Google menggunakan Android Credential Manager. Backend memverifikasi Google ID token memakai `google-auth-library`; token diverifikasi di server, bukan dipercaya dari aplikasi.

1. Buka Google Cloud Console dan pilih/buat project.
2. Siapkan OAuth consent screen dan buat OAuth Client ID bertipe **Web application**.
3. Masukkan Web Client ID yang berakhiran `.apps.googleusercontent.com` ke dua tempat: `GOOGLE_WEB_CLIENT_ID` di `local.properties` Android dan `GOOGLE_WEB_CLIENT_ID` di `backend/.env`. Kedua nilainya harus sama persis.
4. Buat juga OAuth Client ID bertipe **Android** untuk package `com.katalogfilm.app` dengan SHA-1 signing certificate aplikasi. Untuk debug lokal, ambil SHA-1 dari Gradle `signingReport` atau jalankan `./gradlew signingReport` (Windows: `gradlew.bat signingReport`) setelah Gradle tersedia. Untuk rilis, daftarkan juga SHA-1 sertifikat signing rilis.
5. Simpan perubahan, restart backend, lalu lakukan Gradle Sync dan jalankan ulang aplikasi.

Kalau Web Client ID belum diisi, tombol Google menampilkan pesan konfigurasi dan tidak akan berpura-pura login berhasil. Kalau konfigurasi OAuth/Client ID salah, backend akan menolak ID token.

## 4. Aktifkan Lupa Password melalui email

Fitur ini mengirim tautan reset yang berlaku 15 menit. Penerima membuka tautan, memasukkan password baru, dan backend menyimpan password baru dalam bentuk hash bcrypt.

Isi variabel SMTP di `backend/.env`:

- `SMTP_HOST`: alamat server SMTP penyedia email.
- `SMTP_PORT`: umumnya 587 untuk STARTTLS atau 465 untuk TLS.
- `SMTP_SECURE`: `false` untuk 587, `true` untuk 465.
- `SMTP_USER` dan `SMTP_PASS`: kredensial SMTP.
- `MAIL_FROM`: alamat pengirim yang diizinkan penyedia SMTP.
- `PUBLIC_BASE_URL`: alamat backend yang dapat dibuka dari perangkat penerima email.

Contoh lokal pada satu komputer: `PUBLIC_BASE_URL=http://localhost:3000`. Untuk HP fisik di Wi-Fi yang sama, gunakan IP LAN komputer, misalnya `http://192.168.1.10:3000`. Untuk penggunaan publik, deploy backend ke server dengan HTTPS lalu pakai domain HTTPS tersebut. Jangan gunakan `localhost` jika tautan dibuka di HP lain, karena `localhost` akan menunjuk ke HP itu sendiri.

Sesudah mengubah `.env`, restart Node.js. Jika kredensial SMTP belum diisi, endpoint reset akan mengembalikan error konfigurasi dan email tidak terkirim.

## 5. Konfigurasi TMDB

`local.properties` sudah memuat properti `TMDB_API_KEY` sesuai nilai yang diberikan. Build Android membacanya ke `BuildConfig.TMDB_API_KEY`. Untuk mengganti key, ubah nilai properti itu lalu lakukan Gradle Sync.

## 6. Struktur teknis

- `app/src/main/java/.../ui`: Activity, Fragment, adapter, dan bottom sheet.
- `app/src/main/java/.../data/remote`: Retrofit API TMDB dan API autentikasi Node.js.
- `app/src/main/java/.../data/local`: Room untuk watchlist, review, riwayat, notifikasi, dan cache profil.
- `app/src/main/java/.../data/Repository.java`: penghubung UI ke TMDB, backend auth, dan Room.
- `app/src/main/res/values/strings.xml`: teks antarmuka Android, label, pesan, dan string-array.
- `backend/src`: Express API, autentikasi, verifikasi Google, JWT, dan pengiriman email.
- `backend/sql/schema.sql`: skema MySQL.

Akun dan autentikasi disimpan di MySQL. Watchlist, ulasan, riwayat tontonan, preferensi, serta notifikasi masih disimpan lokal di perangkat melalui Room/SharedPreferences; belum disinkronkan ke server.

## 7. Endpoint backend

- `POST /api/auth/register` — daftar menggunakan email dan password.
- `POST /api/auth/login` — login menggunakan email dan password.
- `POST /api/auth/google` — verifikasi Google ID token dan login/daftar.
- `POST /api/auth/forgot-password` — kirim email reset.
- `POST /api/auth/reset-password` — validasi token dan ubah password.
- `GET /api/health` — cek koneksi backend + MySQL.
- `GET /api/me` — contoh endpoint terproteksi JWT.

## 8. Catatan build

Proyek menggunakan Android Gradle Plugin 8.7.3, Gradle 8.9, Java 17, compile/target SDK 35, dan min SDK 26. Launcher `gradlew`/`gradlew.bat` yang disertakan akan mengunduh Gradle 8.9 bila belum tersedia pada komputer. Android Studio juga dapat mengelola distribusi Gradle saat membuka proyek. Build akhir tetap perlu dijalankan di komputer yang memiliki Android SDK Platform 35 serta akses untuk mengunduh dependensi Gradle.

TMDB digunakan sebagai penyedia data film. Aplikasi ini tidak didukung atau disertifikasi oleh TMDB.
