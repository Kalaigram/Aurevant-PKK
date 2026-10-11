# Progress Aurevant PKK

Acuan: `doc/spek.md`. Cara pakai: ubah `[ ]` jadi `[x]` setiap item selesai.
Format commit yang dipakai: `feat:`, `fix:`, `chore:`.

## Setup awal

- [x] `git init` + branch `main` + commit awal
- [x] `client/` — Vue 3 + Vite + TS + Router + Pinia + ESLint/Prettier
- [x] `server/` — Laravel 13 API-only + Sanctum + `.env` MySQL (`aurevant_pkk`)
- [x] `android/` — project Kotlin ter-commit terpisah
- [x] `.gitignore` diperkuat (env, keystore, service-account)
- [ ] MySQL Laragon jalan + `CREATE DATABASE aurevant_pkk` + `php artisan migrate` lolos

## Fase 1 — MVP

- [ ] Tenant: `companies`, registrasi perusahaan, suspend/aktif
- [ ] Autentikasi: Sanctum (web SPA + mobile), login email/HP, 2FA opsional, rate limit
- [ ] RBAC custom: `roles` + `permissions` + scope (sendiri/cabang/perusahaan), template subrole
- [ ] Cabang: CRUD + zona waktu (WIB/WITA/WIT), tipe gudang
- [ ] Katalog: kategori, item barang/jasa, pajak, diskon, harga per cabang
- [ ] Stok: `stock_movements` append-only, saldo turunan, moving average, alert stok minimum
- [ ] Transfer antar cabang (draft → diajukan → disetujui → dikirim → diterima + in transit)
- [ ] Stok opname (hitung → selisih → penyesuaian + approval)
- [ ] PO supplier: draft → diajukan → disetujui → dipesan → diterima → ditutup, approval nominal
- [ ] Penerimaan barang + retur supplier (stok & HPP berubah saat diterima)
- [ ] Kasir: shift (buka/tutup + selisih), nota campur barang+jasa, snapshot harga, split payment, DP
- [ ] Void/retur/refund + batas diskon per role + PIN manajer
- [ ] Mesin approval tunggal (void, diskon, refund, PO, transfer, opname)
- [ ] Nomor nota atomik `{kode cabang}-{yyMMdd}-{urut}` + idempotency key
- [ ] Struk PDF + cetak thermal (Android)
- [ ] Laporan dasar + tabel ringkasan harian per cabang
- [ ] Audit log append-only (event + queue)
- [ ] Notifikasi in-app (polling dulu)
- [ ] Onboarding wizard + impor/ekspor Excel/CSV
- [ ] Langganan: paket + status trial/aktif/grace/read-only/suspend, konfirmasi manual
- [ ] Tes isolasi tenant (A tidak bisa akses data B) lolos 100%

## Fase 2 — SDM

- [ ] Penempatan karyawan (riwayat cabang + role + tanggal)
- [ ] Jadwal: master shift + pola mingguan + roster/override per tanggal
- [ ] Absensi QR dinamis (HP) + geofence + 1 akun 1 perangkat + selfie + waktu server
- [ ] Jalur cadangan: kartu QR (UUID) + selfie + PIN di perangkat cabang
- [ ] Deteksi alpa otomatis + status (hadir/terlambat/alpa/izin/sakit/cuti + flag potong gaji)
- [ ] Kartu karyawan PDF/PNG + versi digital di mobile
- [ ] Penggajian komponen fleksibel (1/30, per-hari-hadir, komisi) + periode + kunci + slip PDF
- [ ] Dashboard per role lengkap

## Fase 3 — Keuangan lanjutan

- [ ] Jurnal otomatis penuh + laba rugi per cabang & konsolidasi
- [ ] Hutang/piutang + aging + biaya operasional + tutup periode
- [ ] BPJS + PPh 21 (flag komponen + tarif bertanggal, verifikasi aturan terbaru)
- [ ] Billing langganan otomatis
- [ ] Audit log diperluas + laporan keuangan lanjutan

## Ditunda (di luar scope)

- Mode offline kasir, payment gateway, loyalty/poin, notifikasi WhatsApp
- Promo otomatis, face-match otomatis, jurnal manual/neraca penuh
- Alokasi biaya pusat, tukar shift, rekonsiliasi bank otomatis, pelaporan pajak resmi
