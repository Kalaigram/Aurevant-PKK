# Progress Aurevant PKK

Acuan: `doc/spek.md`. Cara pakai: ubah `[ ]` jadi `[x]` setiap item selesai.
Format commit yang dipakai: `feat:`, `fix:`, `chore:`.

## Setup awal

- [x] `git init` + branch `main` + commit awal
- [x] `client/` — Vue 3 + Vite + TS + Router + Pinia + ESLint/Prettier
- [x] `server/` — Laravel 13 API-only + Sanctum + `.env` MySQL (`aurevant_pkk`)
- [x] `android/` — aplikasi Kotlin (auth, perusahaan, dashboard, absensi QR+GPS+swafoto, kasir+struk PDF, persetujuan, notifikasi, cuti/slip/kartu; lihat `doc/android.md`)
- [x] `.gitignore` diperkuat (env, keystore, service-account)
- [x] MySQL Laragon jalan + `CREATE DATABASE aurevant_pkk` + `php artisan migrate` lolos

## Fase 1 — MVP

- [x] Tenant: `companies`, registrasi perusahaan, suspend/aktif
- [x] Autentikasi: Sanctum (web SPA + mobile), login email/HP, rate limit + lupa/atur ulang kata sandi via email + ganti kata sandi dari profil (cabut sesi lain)
- [x] RBAC custom: `roles` + `permissions` + scope (sendiri/cabang/perusahaan), template subrole
- [x] Cabang: CRUD + zona waktu (WIB/WITA/WIT), tipe gudang
- [x] Katalog: kategori, item barang/jasa, harga per cabang (pajak/diskon per dokumen ditunda)
- [x] Stok: `stock_movements` append-only, saldo turunan, moving average (alert stok minimum ditunda)
- [x] Transfer antar cabang (draft → diajukan → disetujui → dikirim → diterima + in transit)
- [x] Stok opname (hitung → selisih → penyesuaian + approval)
- [x] PO supplier: draft → diajukan → disetujui → dipesan → diterima → selesai, approval nominal ditunda
- [x] Penerimaan barang (stok & HPP berubah saat diterima; retur supplier ditunda)
- [x] Kasir: shift (buka/tutup + selisih), nota campur barang+jasa, snapshot harga, split payment, DP
- [x] Void/retur/refund (approval langsung; PIN manajer + batas diskon per role ditunda)
- [x] Mesin approval tunggal (inbox + eksekusi: void, diskon, refund, PO, transfer, opname)
- [x] Nomor nota atomik `{kode cabang}-{yyMMdd}-{urut}` + idempotency key
- [x] Struk PDF (`GET .../sales/{sale}/receipt`, tombol di Nota + POS; Android: dibuat lokal di HP via `PdfDocument`; cetak thermal Bluetooth menyusul butuh printer fisik)
- [x] Ekspor CSV pelanggan (`GET .../exports/customers`, izin `pelanggan.ekspor`)
- [x] Laporan dasar + tabel ringkasan harian per cabang
- [x] Audit log append-only (event + queue)
- [x] Notifikasi in-app (web polling) + push FCM ke HP Android (device token otomatis; aktif setelah `google-services.json` dipasang) + email pengingat langganan
- [x] Onboarding wizard (buat company) + impor/ekspor CSV + buat akun karyawan langsung dari web (password awal sekali-tampil + reset oleh HR; undangan email & impor Excel ditunda)
- [x] Langganan: paket + status trial/aktif/grace/read-only/suspend, konfirmasi manual + pengingat H-7/H-3/H-1/H-0 ke Pemilik (in-app + email) + auto read-only saat lewat
- [x] Tes isolasi tenant (A tidak bisa akses data B) lolos 100%

## Fase 2 — SDM

- [x] Penempatan karyawan (riwayat cabang + role + tanggal)
- [x] Jadwal: master shift + pola mingguan + roster/override per tanggal
- [x] Absensi QR dinamis (HP) + geofence + 1 akun 1 perangkat + selfie + waktu server
- [x] Jalur cadangan: kartu QR (UUID) + selfie + PIN di perangkat cabang
- [x] Deteksi alpa otomatis terjadwal harian 00:30 semua perusahaan aktif (skip hari libur & cuti) + status (hadir/terlambat/alpa/izin/sakit/cuti + flag potong gaji)
- [x] Kartu karyawan PDF/PNG + versi digital di mobile
- [x] Penggajian komponen fleksibel (1/30, per-hari-hadir, komisi) + periode + kunci + slip PDF
- [x] Dashboard per role (keuangan/penjualan/operasional/SDM/penggajian/karyawan)

## Fase 3 — Keuangan lanjutan

- [x] Jurnal otomatis penuh + laba rugi per cabang & konsolidasi
- [x] Hutang/piutang + aging + biaya operasional + tutup periode (blokir posting + buka lagi)
- [x] BPJS + PPh 21 annualisasi + PTKP per status (TER bulanan menyusul bila perlu)
- [x] Invoice langganan manual + otomatis harian + bayar perpanjang 30 hari
- [x] Audit log diperluas (keuangan + langganan + periode) + arus kas + neraca

## Persiapan praluncur

- [x] Alpa otomatis menyala: `attendance:mark-absent` dijadwalkan 00:30, opsi `--company` jadi opsional (kosong = semua perusahaan aktif), skip libur sudah di jalur
- [x] Akun karyawan: `POST /companies/{company}/users` (password awal sekali-tampil) + reset sandi oleh HR
- [x] Pemulihan kata sandi: `forgot-password` / `reset-password` (email, throttle) + `change-password` cabut sesi lain
- [x] Push FCM: server HTTP v1 (`PushSender` + `SendPushNotification` job) + Android daftar token otomatis (drop-in `google-services.json` di `android/app/`)
- [x] Reminder langganan: `subscription:remind` 08:00 H-7/H-3/H-1/H-0 + auto `read_only` + email Pemilik

## Ditunda (di luar scope)

- Mode offline kasir, payment gateway, loyalty/poin, notifikasi WhatsApp
- Promo otomatis, face-match otomatis, jurnal manual/neraca penuh
- Alokasi biaya pusat, tukar shift, rekonsiliasi bank otomatis, pelaporan pajak resmi
