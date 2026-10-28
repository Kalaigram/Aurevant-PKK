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
- [x] Stok: `stock_movements` append-only, saldo turunan, moving average, alert stok minimum per item per cabang
- [x] Transfer antar cabang (draft → diajukan → disetujui → dikirim → diterima + in transit)
- [x] Stok opname (hitung → selisih → penyesuaian + approval)
- [x] PO supplier: draft → diajukan → disetujui → dipesan → diterima → selesai, approval nominal ditunda
- [x] Penerimaan barang (stok & HPP berubah saat diterima) + retur supplier per penerimaan (stok turun, tagihan supplier terkoreksi)
- [x] Kasir: shift (buka/tutup + selisih), nota campur barang+jasa, snapshot harga, split payment, DP
- [x] Void/retur/refund (approval langsung; PIN manajer + batas diskon per role ditunda)
- [x] Mesin approval tunggal (inbox + eksekusi: void, diskon, refund, PO, transfer, opname)
- [x] Agenda order jasa: filter rentang tanggal, kelompok per hari, filter teknisi, dan input jam jadwal
- [x] Estimasi biaya jasa PDF (rincian pekerjaan, jadwal/teknisi, DP, sisa; aman lintas tenant)
- [x] Paket jasa: bundling beberapa jasa dengan satu harga paket, CRUD web, dan pemakaian saat membuat order
- [x] Nomor nota atomik `{kode cabang}-{yyMMdd}-{urut}` + idempotency key
- [x] Struk PDF (`GET .../sales/{sale}/receipt`, tombol di Nota + POS; Android: dibuat lokal di HP via `PdfDocument`; cetak thermal Bluetooth menyusul butuh printer fisik)
- [x] Ekspor CSV pelanggan (`GET .../exports/customers`, izin `pelanggan.ekspor`)
- [x] Customer 360: profil pelanggan + statistik, riwayat transaksi, order jasa, dan piutang
- [x] Laporan dasar + tabel ringkasan harian per cabang
- [x] Ekspor laporan Excel/PDF (laba rugi, harian, arus kas, neraca, neraca saldo, top produk, konsolidasi; filter cabang)
- [x] Top produk & jasa + perbandingan cabang (rata-rata transaksi, marjin; filter cabang)
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
- [x] Saldo cuti per karyawan/tahun + pemakaian otomatis saat cuti disetujui
- [x] Lembur: pengajuan, keputusan, dan notifikasi
- [x] Tukar shift: pengajuan, keputusan, dan override jadwal

## Fase 3 - Keuangan lanjutan

- [x] Jurnal otomatis penuh + laba rugi per cabang & konsolidasi
- [x] Hutang/piutang + aging + biaya operasional + tutup periode (blokir posting + buka lagi)
- [x] Bagan akun hierarki, anggaran per cabang/kategori, biaya berulang, aset tetap, dan rekonsiliasi bank
- [x] BPJS + PPh 21 annualisasi + PTKP per status (TER bulanan menyusul bila perlu)
- [x] Invoice langganan manual + otomatis harian + bayar perpanjang 30 hari
- [x] Audit log diperluas (keuangan + langganan + periode) + arus kas + neraca

## Persiapan praluncur

- [x] Alpa otomatis menyala: `attendance:mark-absent` dijadwalkan 00:30, opsi `--company` jadi opsional (kosong = semua perusahaan aktif), skip libur sudah di jalur
- [x] Akun karyawan: `POST /companies/{company}/users` (password awal sekali-tampil) + reset sandi oleh HR
- [x] Pemulihan kata sandi: `forgot-password` / `reset-password` (email, throttle) + `change-password` cabut sesi lain
- [x] Push FCM: server HTTP v1 (`PushSender` + `SendPushNotification` job) + Android daftar token otomatis (drop-in `google-services.json` di `android/app/`)
- [x] Reminder langganan: `subscription:remind` 08:00 H-7/H-3/H-1/H-0 + auto `read_only` + email Pemilik
- [x] Sesi perangkat (daftar/cabut) + 2FA TOTP + kode pemulihan
- [x] Plan limits: kuota cabang dan karyawan aktif + ringkasan pemakaian di langganan

## Ditunda (di luar scope)

- Mode offline kasir, payment gateway, loyalty/poin, notifikasi WhatsApp
- Promo otomatis, face-match otomatis, jurnal manual/neraca penuh
- Alokasi biaya pusat, rekonsiliasi bank otomatis, pelaporan pajak resmi

## Upgrade UI web (calm green)

Rencana phased disetujui 2026-10-22. Fokus web dulu; Android tidak diubah pada fase ini.

### Fase 1 - Fondasi (selesai)

- [x] Token warna calm green + surface/ink/outline, focus ring, reduced motion, dan aturan print (`.print-area`, struk 80mm)
- [x] Komponen bersama: button, badge, banner, card, field, input, textarea, select, checkbox, search, money/date input, empty, pager, skeleton, avatar, tabs, page header, stat, sparkline, bar chart, table, modal, confirm, toast, dropdown, entity select
- [x] Composable: `useToast`, `useConfirm`, `useFormErrors`, `usePagedList`
- [x] Shell baru: sidebar hijau gelap, grup nav collapsible (localStorage `aurevant-nav`), company switcher, user dropdown, bottom nav + menu sheet mobile, skip link
- [x] `api.ts`: `getPage` (meta pagination), envelope request, 204, 401 hanya saat ada token, `assetUrl`, blob download
- [x] Migrasi 22 panggilan paginasi di 21 view dari `api.get` ke `api.getPage`; meta kini terbaca AppPager
- [x] Verifikasi: `npm run type-check`, `npm run lint`, `npm run build` lolos; 126 tes server lolos
- [x] Smoke test headless: login 1440px, dashboard 1440px, dashboard 504px (header, sidebar/bottom nav, konten)
- [ ] Migrasi seluruh daftar ke `usePagedList` + `AppTable`/`AppPager` penuh (menyusul di pass per halaman)
- [x] Pass visual P0: laporan, slip, jadwal, approval/audit, POS, absensi, print

### Fase 2 - P0 (selesai)

- [x] Laporan: tabel/grafik/CSV/Excel/PDF/print + filter tanggal/cabang; konsolidasi mengirim rentang tanggal
- [x] Slip gaji: pilih periode (`/payroll-periods/me` + fallback HR) + print 80mm
- [x] Jadwal: jadwal sendiri, grid tim 7 hari, kelola shift/pola/override/libur, print
- [x] Approval/audit: label human-readable, payload detail, diff before → after, filter
- [x] POS: struk print lengkap (item, cabang, perusahaan), konfirmasi bayar/kosongkan, guard shift, harga katalog dari daftar item (tanpa N+1)
- [x] Absensi: QR SVG server + countdown auto-refresh, selfie + preview, riwayat AppTable, print
- [x] Verifikasi P0: `type-check`, `lint`, `build` lolos; 126 tes server lolos; smoke 7 halaman

### Berikutnya (Fase 2 - P1)

- [x] Nota/Penjualan: filter status/tanggal/cari, tabel, detail, void/retur, cetak 80mm + PDF
- [x] Pembelian/PO: filter status/cabang/cari, tabel, detail item + tagihan, buat PO, alur aksi, terima barang bertahap, print A4
- [x] Stok/transfer/opname: filter server, tabel, detail item, alur aksi, penyesuaian/terima bertahap
- [x] Katalog/harga/pelanggan/jasa: pencarian server, tabel+pager, form modal, detail order jasa + aksi
- [x] Cabang/karyawan/payroll/periode: filter server, tabel+pager, form modal, aksi alur payroll/periode
- [x] Migrasi daftar tersisa ke `usePagedList`/`AppTable`/`AppPager`
- [x] DB 3 tier: branding/kontak, upload logo/foto/avatar, expose field tersisa
