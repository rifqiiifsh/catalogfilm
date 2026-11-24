# Deploy Katalog Film ke Railway (Node.js + MySQL)

Panduan ini membuat backend bisa diakses melalui domain HTTPS yang tetap, tanpa IP laptop/HP. APK hanya perlu menggunakan URL publik API Railway.

## Yang perlu disiapkan

- Akun Railway.
- Repository GitHub berisi folder proyek `KatalogFilm`.
- Repository harus menyertakan folder `backend/` dan `backend/sql/schema.sql`.
- Jangan upload `backend/.env`, `local.properties`, API key, password, atau secret. File tersebut sudah masuk `.gitignore`.

## 1. Buat project Railway dan database MySQL

1. Login ke https://railway.com/.
2. Buat project baru.
3. Di project canvas, klik `+ New` → `Database` → `MySQL`.
4. Tunggu service MySQL berstatus online. Biarkan service database tetap private; backend akan mengaksesnya melalui private networking.
5. Pastikan nama service database adalah `MySQL`. Ini memudahkan pemakaian variable references di langkah berikutnya.

Railway menyediakan environment variables `MYSQLHOST`, `MYSQLPORT`, `MYSQLUSER`, `MYSQLPASSWORD`, dan `MYSQLDATABASE` pada service MySQL. Backend tidak perlu membuka database ke internet.

## 2. Import skema database

Skema proyek membuat database `katalog_film` dan tabel `users`.

Cara aman lewat Railway CLI + SSH tunnel:

1. Install Railway CLI sesuai petunjuk resmi: https://docs.railway.com/cli.
2. Pastikan MySQL command-line client tersedia. Jika menggunakan XAMPP di Windows, file `mysql.exe` biasanya ada di `C:\xampp\mysql\bin\mysql.exe`. Tambahkan folder itu ke PATH untuk sesi terminal ini jika `mysql` belum dikenali.
3. Buka CMD (bukan PowerShell), masuk ke folder repository yang sudah terhubung ke project Railway.
4. Jalankan `railway link` dan pilih project serta environment yang benar.
5. Jalankan perintah berikut dari root repository:

```bat
railway connect MySQL < backend\sql\schema.sql
```

Perintah `railway connect` akan memakai SSH tunnel jika database belum memiliki public TCP proxy. Jadi tidak perlu mempublikasikan port MySQL.

Jika CLI meminta memilih service, pilih `MySQL`. Setelah import, skema membuat database bernama `katalog_film`.

## 3. Deploy backend dari GitHub

1. Di project Railway, klik `+ New` → `GitHub Repo`.
2. Hubungkan GitHub jika diminta, lalu pilih repository proyek ini.
3. Klik service backend yang baru dibuat.
4. Buka `Settings` → `Source` → `Root Directory` dan isi:

```text
/backend
```

5. Pastikan build menggunakan `npm install` (atau biarkan Railway mendeteksi `backend/package.json`) dan start command menggunakan:

```text
npm start
```

6. Buka tab `Variables` pada service backend. Tambahkan variable berikut.

| Variable | Value |
|---|---|
| `DB_HOST` | `${{MySQL.MYSQLHOST}}` |
| `DB_PORT` | `${{MySQL.MYSQLPORT}}` |
| `DB_USER` | `${{MySQL.MYSQLUSER}}` |
| `DB_PASSWORD` | `${{MySQL.MYSQLPASSWORD}}` |
| `DB_NAME` | `katalog_film` |
| `JWT_SECRET` | Secret acak minimal 32 karakter |
| `NODE_ENV` | `production` |
| `GOOGLE_WEB_CLIENT_ID` | Web OAuth Client ID dari Google Cloud |
| `PUBLIC_BASE_URL` | URL domain Railway setelah dibuat, contoh `https://nama-api.up.railway.app` |

Jangan menambahkan variable `PORT` secara manual. Railway menyediakan port untuk service; `server.js` sudah membaca `process.env.PORT` dan bind ke `0.0.0.0`.

`JWT_SECRET` dapat dibuat di terminal lokal:

```powershell
node -e "console.log(require('node:crypto').randomBytes(48).toString('hex'))"
```

Salin hasilnya langsung ke Railway Variables. Jangan taruh secret itu dalam source code, GitHub, screenshot, atau chat.

Variabel SMTP (untuk Lupa Password) perlu ditambahkan setelah punya kredensial SMTP:

- `SMTP_HOST`
- `SMTP_PORT` (umumnya `587`)
- `SMTP_SECURE` (`false` untuk port 587; `true` untuk port 465)
- `SMTP_USER`
- `SMTP_PASS`
- `MAIL_FROM`

7. Klik Deploy / Redeploy setelah semua variables tersimpan. Buka deployment logs dan pastikan backend berhasil mulai.

## 4. Buat domain HTTPS tetap

1. Buka service backend.
2. Masuk `Settings` → `Networking` → `Public Networking`.
3. Klik `Generate Domain`.
4. Railway akan memberikan domain seperti `nama-service-production.up.railway.app`.
5. Pakai URL HTTPS tersebut; Railway menyediakan SSL otomatis untuk domain Railway.
6. Setelah domain terbentuk, ubah `PUBLIC_BASE_URL` di Variables menjadi domain tersebut tanpa slash terakhir, lalu redeploy.

Domain Railway yang di-generate tidak membutuhkan pembelian domain sendiri. Domain akan tetap sama selama service/domain tidak dihapus atau diganti. Kalau ingin domain sendiri seperti `api.katalogfilm.com`, domain tersebut perlu dimiliki dan DNS diarahkan sesuai instruksi Railway.

## 5. Tes backend

Buka di browser:

```text
https://DOMAIN-RAILWAY-KAMU.up.railway.app/api/health
```

Respons yang diharapkan:

```json
{
  "status": "ok",
  "service": "katalog-film-api",
  "database": "connected"
}
```

`database: connected` menandakan koneksi ke MySQL aktif. Pastikan juga import skema selesai sebelum menguji daftar/login.

## 6. Sambungkan APK ke domain

Di komputer pengembangan, buka file lokal Android Studio `KatalogFilm/local.properties` (file ini tidak di-commit ke Git). Ubah hanya `BACKEND_BASE_URL`:

```properties
BACKEND_BASE_URL=https://DOMAIN-RAILWAY-KAMU.up.railway.app/
```

Biarkan baris `TMDB_API_KEY` dan `GOOGLE_WEB_CLIENT_ID` tetap ada dengan nilai yang benar. Klik `File` → `Sync Project with Gradle Files`, lalu Run/build ulang APK.

Setelah itu APK bisa memanggil backend melalui internet dari jaringan berbeda. Pengguna tidak perlu berada di Wi-Fi laptop dan tidak perlu mengetahui IP server.

## 7. Catatan paket dan biaya

Railway Free saat ini menyertakan kredit penggunaan $1 per bulan. Service Free bisa sleep setelah idle dan bangun lagi saat menerima request. Trial awal memberikan kredit sementara. Railway Hobby memiliki minimum usage $5 per bulan dan mencakup $5 usage credits; pemakaian di atas kredit bisa ditagihkan. Periksa halaman pricing Railway sebelum mengaktifkan paket berbayar atau memasukkan metode pembayaran.

Untuk uji coba awal, mulai dari Free. Pantau Usage sebelum meninggalkan backend untuk penggunaan terus-menerus.

## Troubleshooting

- `database: connected` gagal: periksa variable references `DB_HOST`, `DB_PORT`, `DB_USER`, `DB_PASSWORD`, dan `DB_NAME`; pastikan service database bernama `MySQL`.
- Error `Unknown database 'katalog_film'`: jalankan lagi import `backend/sql/schema.sql` lewat `railway connect MySQL`.
- API domain 502: periksa deployment logs; backend harus start dengan `npm start` dan listen menggunakan `process.env.PORT`.
- Aplikasi Android tetap gagal menghubungi backend: cek `BACKEND_BASE_URL`, wajib `https://`, domain harus persis, dan akhiri dengan `/`.
- Google login gagal: nilai `GOOGLE_WEB_CLIENT_ID` di Android `local.properties` dan Railway backend harus sama persis dengan OAuth Client ID tipe Web application.
- Lupa Password belum mengirim email: lengkapi seluruh variables SMTP dan pastikan `PUBLIC_BASE_URL` memakai domain HTTPS aktif.
