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

- [x] Tenant: `companies`, registrasi perusahaan, suspend/aktif
- [x] Autentikasi: Sanctum (web SPA + mobile), login email/HP, rate limit (2FA opsional ditunda)
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
- [ ] Mesin approval tunggal (void, diskon, refund, PO, transfer, opname)
- [x] Nomor nota atomik `{kode cabang}-{yyMMdd}-{urut}` + idempotency key
- [ ] Struk PDF + cetak thermal (Android)
- [x] Laporan dasar + tabel ringkasan harian per cabang
- [x] Audit log append-only (event + queue)
- [x] Notifikasi in-app (polling dulu; FCM/email ditunda)
- [x] Onboarding wizard (buat company) + impor/ekspor CSV (Excel + undangan karyawan ditunda)
- [x] Langganan: paket + status trial/aktif/grace/read-only/suspend, konfirmasi manual
- [x] Tes isolasi tenant (A tidak bisa akses data B) lolos 100%

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

- [x] Jurnal otomatis penuh + laba rugi per cabang & konsolidasi
- [x] Hutang/piutang + aging + biaya operasional + tutup periode (blokir posting + buka lagi)
- [x] BPJS + PPh 21 sederhana dari flag komponen (annualisasi + PTKP menyusul)
- [x] Invoice langganan manual + bayar perpanjang 30 hari (otomatis penuh menyusul)
- [ ] Audit log diperluas + laporan keuangan lanjutan

## Ditunda (di luar scope)

- Mode offline kasir, payment gateway, loyalty/poin, notifikasi WhatsApp
- Promo otomatis, face-match otomatis, jurnal manual/neraca penuh
- Alokasi biaya pusat, tukar shift, rekonsiliasi bank otomatis, pelaporan pajak resmi
