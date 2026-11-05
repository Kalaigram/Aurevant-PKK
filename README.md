<p align="center">
  <img src="logo/logopkk-icon.png" alt="Logo Aurevant PKK" width="120" />
</p>

<h1 align="center">Aurevant PKK</h1>

<p align="center">
  <strong>Satu alur data untuk kasir, stok, tim, dan keuangan.</strong><br />
  Aplikasi operasional multi-cabang untuk bisnis barang dan jasa.
</p>

<p align="center">
  <img alt="Vue 3" src="https://img.shields.io/badge/Vue-3-42b883?logo=vuedotjs&logoColor=white" />
  <img alt="Laravel 13" src="https://img.shields.io/badge/Laravel-13-FF2D20?logo=laravel&logoColor=white" />
  <img alt="Kotlin" src="https://img.shields.io/badge/Kotlin-Android-7F52FF?logo=kotlin&logoColor=white" />
  <img alt="MySQL" src="https://img.shields.io/badge/MySQL-8-4479A1?logo=mysql&logoColor=white" />
</p>

Aurevant PKK menyatukan kasir, stok, order jasa, absensi, penggajian, dan keuangan dalam satu alur data. Repo ini berisi tiga aplikasi yang memakai API yang sama:

- `client/` - aplikasi web (Vue 3)
- `server/` - API dan backend (Laravel)
- `android/` - aplikasi Android (Kotlin + Jetpack Compose)

## Daftar isi

- [Fitur utama](#fitur-utama)
- [Stack](#stack)
- [Struktur repo](#struktur-repo)
- [Prasyarat](#prasyarat)
- [Menjalankan project](#menjalankan-project)
- [Akun demo](#akun-demo)
- [Kontrak API](#kontrak-api)
- [Testing dan pemeriksaan kualitas](#testing-dan-pemeriksaan-kualitas)
- [Dokumentasi](#dokumentasi)
- [Batasan saat ini](#batasan-saat-ini)
- [Catatan keamanan](#catatan-keamanan)

## Fitur utama

### Operasional

- Kasir dengan shift, nota barang dan jasa, diskon, DP, split payment, void, retur, dan refund.
- Katalog item, harga per cabang, stok per cabang, transfer antar cabang, dan stok opname.
- Purchase order, penerimaan barang bertahap, retur supplier, dan tagihan supplier.
- Order jasa, agenda teknisi, estimasi biaya, paket jasa, dan Customer 360.

### SDM

- Jadwal shift, pola mingguan, roster, dan override per tanggal.
- Absensi QR dinamis, geofence, selfie, dan satu akun satu perangkat.
- Cuti, saldo cuti, lembur, tukar shift, dan persetujuan.
- Penggajian dengan komponen fleksibel, slip PDF, dan kartu karyawan.

### Keuangan dan platform

- Jurnal otomatis, laba rugi, neraca, arus kas, dan laporan per cabang maupun konsolidasi.
- Hutang, piutang, biaya operasional, anggaran, aset tetap, dan rekonsiliasi bank.
- Multi-perusahaan, cabang, role, permission, dan audit log.
- Paket langganan, masa trial, pengingat, dan pembatasan kuota.
- Notifikasi in-app, push FCM untuk Android, dan ekspor laporan Excel/PDF.
- Landing page publik dengan CTA registrasi dan login.

## Stack

| Bagian | Teknologi |
|---|---|
| Web | Vue 3, Vite, TypeScript, Pinia, Vue Router, Tailwind CSS |
| API | Laravel 13, PHP 8.3+, Sanctum, MySQL, Spatie Permission |
| Android | Kotlin, Jetpack Compose, Hilt, Retrofit, DataStore |
| Tools | ESLint, Oxlint, Prettier, PHPUnit, Pint, Gradle |

## Struktur repo

```text
.
├── client/                 # Aplikasi web Vue
│   ├── src/
│   │   ├── views/          # Halaman dan landing page
│   │   ├── components/     # Komponen bersama
│   │   ├── router/         # Route dan guard
│   │   ├── stores/         # Pinia store
│   │   └── assets/         # Token warna dan CSS global
│   └── .env.example
├── server/                 # API Laravel
│   ├── app/
│   │   ├── Domain/         # Entitas, kontrak repository, aturan bisnis
│   │   ├── Application/    # Use case dan DTO
│   │   ├── Infrastructure/ # Eloquent, repository, integrasi eksternal
│   │   └── Http/           # Controller dan middleware
│   ├── database/seeders/   # Permission, plan, dan data demo
│   └── routes/api.php      # Endpoint /api/v1
├── android/                # Aplikasi Android
│   └── app/src/main/java/com/aurevant/pkk/
├── logo/                   # Logo dan ikon aplikasi
├── doc/                    # Spesifikasi, desain, dan catatan progres
├── PROGRESS.md             # Checklist progres fitur
└── RULES.md                # Aturan arsitektur dan workflow
```

## Prasyarat

- PHP 8.3 atau lebih baru
- Composer 2
- MySQL 8 atau MariaDB yang kompatibel
- Node.js `^22.18.0` atau `>=24.12.0`
- npm
- JDK 17 atau lebih baru
- Android Studio dan Android SDK 36 untuk membangun aplikasi Android

## Menjalankan project

### 1. Clone dan siapkan database

```bash
git clone https://github.com/Kalaigram/Aurevant-PKK.git
cd Aurevant-PKK
```

Buat database MySQL:

```sql
CREATE DATABASE aurevant_pkk
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
```

### 2. Jalankan backend

```bash
cd server
composer install
cp .env.example .env
php artisan key:generate
php artisan migrate --seed
php artisan storage:link
php artisan serve --host=0.0.0.0 --port=8000
```

Di PowerShell, gunakan `Copy-Item .env.example .env` sebagai pengganti `cp`.

`migrate --seed` membuat permission, paket langganan, dan data demo. Untuk database tanpa data demo, jalankan `php artisan migrate` saja.

Jalankan queue worker di terminal terpisah. Diperlukan untuk audit log, notifikasi, dan proses latar belakang:

```bash
cd server
php artisan queue:work
```

Untuk menjalankan scheduler lokal:

```bash
cd server
php artisan schedule:work
```

### 3. Jalankan web

```bash
cd client
npm install
cp .env.example .env
npm run dev
```

Di PowerShell, gunakan `Copy-Item .env.example .env`.

Web berjalan di `http://localhost:5173`. Secara default web memanggil API di `http://127.0.0.1:8000/api/v1`.

Jika ingin menampilkan akun demo di halaman login, ubah `client/.env`:

```env
VITE_SHOW_DEMO_CREDENTIALS=true
```

### 4. Jalankan Android

Buka folder `android/` di Android Studio, lalu:

```bash
cd android
./gradlew :app:assembleDebug
```

Di Windows:

```powershell
cd android
.\gradlew.bat :app:assembleDebug
```

Base URL emulator ada di `android/app/src/main/java/com/aurevant/pkk/di/NetworkModule.kt`:

```text
http://10.0.2.2:8000/api/v1/
```

Untuk HP fisik, ganti dengan IP LAN komputer yang menjalankan backend. `google-services.json` bersifat opsional; tanpa file itu aplikasi tetap berjalan, tetapi push FCM nonaktif.

## Akun demo

Tersedia setelah `php artisan migrate --seed`:

| Peran | Email |
|---|---|
| Super Admin | `superadmin@demo.id` |
| Pemilik | `pemilik@demo.id` |
| Manajer | `manajer@demo.id` |
| Kasir | `kasir@demo.id` |
| Gudang | `gudang@demo.id` |
| Teknisi | `teknisi@demo.id` |
| HR | `hr@demo.id` |
| Keuangan | `keuangan@demo.id` |

Sandi semua akun demo: `password123`.

Akun ini hanya untuk pengembangan lokal. Jangan memakai data atau sandi demo di environment produksi.

## Kontrak API

- Base path: `/api/v1`
- Autentikasi: Bearer token Sanctum
- Format respons:

```json
{
  "status": true,
  "message": "Pesan untuk pengguna",
  "data": {},
  "errors": null
}
```

- Semua query bisnis dibatasi oleh `company_id`.
- Permission memakai nama yang sama di backend, web, dan Android.
- Nomor nota dan idempotency key dibuat di server.

## Testing dan pemeriksaan kualitas

Backend:

```bash
cd server
php artisan test
./vendor/bin/pint --test
```

Web:

```bash
cd client
npm run type-check
npm run lint
npm run build
```

Android:

```bash
cd android
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
```

Gunakan `.\gradlew.bat` di Windows.

Sebelum commit, jalankan pemeriksaan yang relevan dengan perubahan dan pastikan `git status` tidak memuat file rahasia.

## Dokumentasi

| Dokumen | Isi |
|---|---|
| [`doc/spek.md`](doc/spek.md) | Spesifikasi produk dan aturan bisnis |
| [`doc/monitoring-progress.md`](doc/monitoring-progress.md) | Progres implementasi dan hasil verifikasi |
| [`doc/android.md`](doc/android.md) | Layar, endpoint, dan catatan Android |
| [`doc/DESIGN.md`](doc/DESIGN.md) | Design system calm green |
| [`doc/halaman.md`](doc/halaman.md) | Peta halaman web |
| [`doc/menu-per-role.md`](doc/menu-per-role.md) | Menu per role dan permission |
| [`RULES.md`](RULES.md) | Aturan arsitektur dan workflow |
| [`PROGRESS.md`](PROGRESS.md) | Checklist fitur per fase |

## Batasan saat ini

- Mode offline kasir, payment gateway, loyalty, dan notifikasi WhatsApp belum termasuk.
- Cetak thermal Bluetooth di Android menunggu pengujian dengan printer fisik.
- Beberapa alur lanjutan Android, termasuk pengujian runtime tukar shift, masih dalam pengembangan.
- Deployment produksi memerlukan HTTPS, SMTP, queue worker, scheduler, storage, dan konfigurasi Firebase bila push notification dipakai.

## Catatan keamanan

- Jangan commit `.env`, keystore, `google-services.json`, service account, atau kredensial asli.
- Gunakan salinan dari `.env.example` untuk konfigurasi lokal.
- Ganti seluruh kredensial demo dan set `APP_DEBUG=false` sebelum produksi.
- Batasi akses database dan storage sesuai environment.
