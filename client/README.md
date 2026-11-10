<p align="center">
  <img src="public/brand/aurevant-icon.png" alt="Logo Aurevant" width="120" />
</p>

<h1 align="center">Aurevant Client</h1>

<p align="center">
  <strong>Satu alur data untuk kasir, stok, tim, dan keuangan.</strong><br />
  Aplikasi web operasional multi-cabang untuk bisnis barang dan jasa.
</p>

<p align="center">
  <img alt="Vue 3" src="https://img.shields.io/badge/Vue-3-42b883?logo=vuedotjs&logoColor=white" />
  <img alt="Vite" src="https://img.shields.io/badge/Vite-8-646CFF?logo=vite&logoColor=white" />
  <img alt="TypeScript" src="https://img.shields.io/badge/TypeScript-6-3178C6?logo=typescript&logoColor=white" />
  <img alt="Tailwind CSS" src="https://img.shields.io/badge/Tailwind-4-06B6D4?logo=tailwindcss&logoColor=white" />
  <img alt="Primary color" src="https://img.shields.io/badge/primary-%23005D42-005D42" />
</p>

Aurevant Client adalah aplikasi web untuk **Aurevant**, platform operasional multi-cabang yang menyatukan kasir, stok, order jasa, absensi, penggajian, dan keuangan dalam satu alur data. Repo ini berisi landing page publik, autentikasi, dashboard, halaman operasional, SDM, keuangan, laporan, dan pengaturan akses.

Repo ini adalah bagian dari ekosistem Aurevant:

- `Aurevant-Client` — aplikasi web Vue 3
- `Aurevant-Server` — API dan backend Laravel
- `Aurevant-PKK` — monorepo, aplikasi Android, dan dokumentasi produk

## Daftar isi

- [Fitur utama](#fitur-utama)
- [Stack](#stack)
- [Struktur repo](#struktur-repo)
- [Prasyarat](#prasyarat)
- [Menjalankan project](#menjalankan-project)
- [Environment](#environment)
- [Scripts](#scripts)
- [Design system](#design-system)
- [Akun demo](#akun-demo)
- [Kualitas kode](#kualitas-kode)
- [Repo terkait](#repo-terkait)
- [Dokumentasi](#dokumentasi)
- [Catatan keamanan](#catatan-keamanan)
- [Batasan saat ini](#batasan-saat-ini)

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
- Notifikasi in-app dan ekspor laporan Excel/PDF.
- Landing page publik dengan CTA registrasi dan login.
- Layout responsif untuk desktop, tablet, dan mobile.

## Stack

| Bagian | Teknologi |
|---|---|
| Framework | Vue 3 |
| Build tool | Vite 8 |
| Bahasa | TypeScript 6 |
| State | Pinia |
| Routing | Vue Router 5 |
| Styling | Tailwind CSS 4 |
| Icons | Material Symbols melalui komponen internal |
| Linting | ESLint, Oxlint, Prettier |
| Testing | Type-check dan build verification |

## Struktur repo

```text
.
├── public/
│   └── brand/              # Logo dan ikon Aurevant
├── src/
│   ├── assets/             # CSS global dan design token
│   ├── components/         # Komponen UI bersama
│   ├── composables/        # Composable Vue
│   ├── lib/                # API client dan helper
│   ├── router/             # Route dan guard
│   ├── stores/             # Pinia store
│   └── views/              # Halaman aplikasi dan landing page
├── index.html
├── package.json
├── vite.config.ts
├── tsconfig.json
└── .env.example
```

## Prasyarat

- Node.js `^22.18.0` atau `>=24.12.0`
- npm
- Backend Aurevant Server berjalan di `http://127.0.0.1:8000`
- Database backend sudah dimigrasi dan di-seed untuk akun demo

## Menjalankan project

### 1. Clone dan install dependency

```bash
git clone https://github.com/Kalaigram/Aurevant-Client.git
cd Aurevant-Client
npm install
```

### 2. Siapkan environment

```bash
cp .env.example .env
```

Di PowerShell:

```powershell
Copy-Item .env.example .env
```

Isi `.env`:

```env
VITE_API_URL=http://127.0.0.1:8000
VITE_SHOW_DEMO_CREDENTIALS=false
```

`VITE_SHOW_DEMO_CREDENTIALS` otomatis aktif saat development. Set `true` jika ingin menampilkan akun demo di halaman login pada build tertentu.

### 3. Jalankan backend

Ikuti README [Aurevant Server](https://github.com/Kalaigram/Aurevant-Server), lalu pastikan API berjalan di `http://127.0.0.1:8000`.

### 4. Jalankan web

```bash
npm run dev
```

Web berjalan di `http://localhost:5173`. Secara default web memanggil API di `http://127.0.0.1:8000/api/v1`.

## Environment

| Variable | Fungsi |
|---|---|
| `VITE_API_URL` | Base URL backend, default `http://127.0.0.1:8000` |
| `VITE_SHOW_DEMO_CREDENTIALS` | Menampilkan akun demo di halaman login |

Jangan commit `.env`. Gunakan `.env.example` sebagai sumber konfigurasi.

## Scripts

| Perintah | Fungsi |
|---|---|
| `npm run dev` | Menjalankan Vite development server |
| `npm run build` | Type-check lalu build production |
| `npm run preview` | Preview hasil build |
| `npm run type-check` | Menjalankan `vue-tsc` |
| `npm run lint` | Menjalankan Oxlint dan ESLint |
| `npm run format` | Memformat source dengan Prettier |

## Design system

Aurevant memakai arah visual **calm green premium**: hijau hutan, krem hangat, dan aksen emas.

| Token | Nilai |
|---|---|
| Primary | **#005D42** |
| Primary container | **#0B6B4F** |
| Primary hover | **#004A35** |
| Primary ink | **#003D2B** |
| Primary soft | **#E7F2ED** |
| Surface | **#F6F8F7** |
| Ink | **#14201B** |
| Outline | **#D7E0DB** |
| Logo tile | **#F7F4EC** |

Font:

- Headline: Plus Jakarta Sans
- Body: Inter

Token warna dan tipografi berada di `src/assets/main.css`.

## Akun demo

Tersedia setelah backend dijalankan dengan `php artisan migrate --seed`:

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

## Kualitas kode

```bash
npm run type-check
npm run lint
npm run build
```

Sebelum commit, jalankan pemeriksaan yang relevan dan pastikan `git status` tidak memuat file rahasia.

## Repo terkait

| Repo | Isi |
|---|---|
| [`Aurevant-Client`](https://github.com/Kalaigram/Aurevant-Client) | Aplikasi web Vue 3 |
| [`Aurevant-Server`](https://github.com/Kalaigram/Aurevant-Server) | API dan backend Laravel |
| [`Aurevant-PKK`](https://github.com/Kalaigram/Aurevant-PKK) | Monorepo, Android, dan dokumentasi produk |

## Dokumentasi

Dokumentasi produk dan aturan bisnis ada di monorepo:

- [`doc/spek.md`](https://github.com/Kalaigram/Aurevant-PKK/blob/main/doc/spek.md) — spesifikasi produk dan aturan bisnis
- [`doc/halaman.md`](https://github.com/Kalaigram/Aurevant-PKK/blob/main/doc/halaman.md) — peta halaman dan endpoint
- [`doc/DESIGN.md`](https://github.com/Kalaigram/Aurevant-PKK/blob/main/doc/DESIGN.md) — design system calm green

## Catatan keamanan

- Jangan commit `.env`, kredensial asli, atau file konfigurasi lokal.
- Gunakan `.env.example` untuk konfigurasi baru.
- Jangan memakai akun demo di production.
- Pastikan backend memakai HTTPS saat deployment.

## Batasan saat ini

- Web bergantung pada API Aurevant Server yang berjalan.
- Mode offline kasir belum tersedia.
- Beberapa alur lanjutan Android berada di repo monorepo dan belum sepenuhnya tersedia di web.
- Deployment produksi memerlukan konfigurasi API, CORS, dan HTTPS yang benar.
