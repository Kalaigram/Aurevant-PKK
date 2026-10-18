# Peta Halaman per Hak Akses — Aurevant PKK

> Progres UI Web: **40 / 40 halaman selesai** — rincian per halaman di §13.
> Tanda: `[ ]` belum dikerjakan, `[~]` prompt Stitch sudah dibuat, `[x]` layar selesai + terverifikasi API.

Acuan API: `server/routes/api.php` (prefix `/api/v1`). Izin: `app/Domain/Access/Services/PermissionCatalog.php`.
Template role: `app/Domain/Access/Services/RoleTemplates.php` (Pemilik, Manajer Cabang, Kasir,
Staf Gudang, Pelaksana Jasa, Admin Keuangan, HR). Super admin: flag `users.is_super_admin`
+ middleware `super.admin`.

## 0. Konvensi yang mengikat semua halaman

- Base URL `/api/v1`, throttle 60/menit, kontrak `{status, message, data, errors}` + `meta` untuk list.
- 401 belum login, 403 tanpa izin, 404 data lain perusahaan / tidak ada, 422 validasi gagal.
- Semua route perusahaan memakai middleware `company.team` (Spatie Teams per `companies.id`),
  `scope.cabang` (user hanya melihat cabang penempatannya; tanpa penempatan = semua cabang),
  dan `subscription.aktif` (langganan kedaluwarsa → tulis diblokir, baca tetap boleh).
- Frontend WAJIB menyembunyikan menu/aksi bila user tak punya izinnya (backend tetap menolak 403).
- Key JSON camelCase (`branchId`, `grandTotal`, `endsAt`).

## 1. Halaman global (semua role yang login)

| Halaman | Penjelasan | Endpoint |
|---|---|---|
| Login / Register | Masuk email/HP + password; daftar akun baru. | `POST /auth/login`, `POST /auth/register` |
| Lupa / Atur Ulang Sandi | Kirim tautan reset via email (anti-enumerasi, throttle 6/menit); form atur ulang dari tautan email. | `POST /auth/forgot-password`, `POST /auth/reset-password` |
| Pilih Perusahaan | Daftar perusahaan yang bisa diakses user. | `GET /companies` |
| Profil Saya | Data diri + daftar izin per perusahaan + ganti kata sandi (sesi lain dicabut). | `GET /auth/me`, `POST /auth/change-password` |
| Dashboard | Ringkasan per role (lihat §2). Seksi muncul sesuai izin: keuangan (`keuangan.lihat`), penjualan (`nota.lihat`), operasional (`stok.transfer`/`stok.opname`), SDM (`jadwal.lihat`), penggajian (`gaji.kelola`), karyawan (`karyawan.kelola`). | `GET /companies/{company}/dashboard` |
| Notifikasi | Polling notifikasi (web) + push FCM (Android), tandai dibaca, atur preferensi, lihat/cabut device token. | `GET /notifications`, `GET /notifications/unread-count`, `POST /notifications/{id}/read`, `POST /notifications/read-all`, `GET+PUT /notification-preferences`, `POST+DELETE /device-tokens` |
| Persetujuan (inbox) | Satu antrean untuk void, diskon, refund, PO, transfer, opname. Siapa pun boleh mengajukan sesuai izin dasar; tombol Setujui/Tolak hanya untuk pemegang izin putuskan (backend 403 bila nekat). Lihat §9. | `GET /companies/{company}/approvals`, `POST /companies/{company}/approvals`, `GET /companies/{company}/approvals/{id}`, `POST .../approve`, `POST .../reject` |
| Absensi Saya | Clock-in/out (QR dinamis + geofence + selfie, 1 akun 1 perangkat), riwayat sendiri. | `POST /companies/{company}/attendance/clock-in`, `POST .../clock-out`, `GET .../attendance/me`, `GET .../branches/{branch}/attendance-qr` |
| Cuti Saya | Ajukan izin/sakit/cuti, lihat status. | `GET /companies/{company}/leaves`, `POST /companies/{company}/users/{user}/leaves` |
| Slip Gaji Saya | Slip per periode (milik sendiri; `gaji.kelola` boleh lihat semua). | `GET /companies/{company}/payroll-periods/{period}/users/{user}/slip` |
| Kartu Karyawan | Kartu digital + QR. | `GET /companies/{company}/users/{user}/card`, `GET .../card/qr` |
| Langganan (lihat) | Status paket + masa aktif perusahaan sendiri. | `GET /companies/{company}/subscription` |

## 2. Dashboard per role (sumber seksi)

`GET /companies/{company}/dashboard` selalu mengembalikan `company` + `subscription`.
Seksi tambahan hanya bila izinnya ada — frontend cukup render apa yang dikembalikan:

| Seksi | Syarat izin | Isi |
|---|---|---|
| `keuangan` | `keuangan.lihat` | Kas bersih + laba bersih bulan berjalan, total hutang & piutang |
| `penjualan` | `nota.lihat` | Omzet + transaksi hari ini, jumlah shift terbuka |
| `operasional` | `stok.transfer` / `stok.opname` | Transfer/opname berstatus diajukan |
| `sdm` | `jadwal.lihat` | Hadir/terlambat/alpa hari ini, cuti diajukan |
| `penggajian` | `gaji.kelola` | Periode gaji per status |
| `karyawan` | `karyawan.kelola` | Headcount aktif |

## 3. Pemilik (`*` — semua izin)

Melihat semua halaman di dokumen ini, plus eksklusif:

| Halaman | Penjelasan | Endpoint |
|---|---|---|
| Cabang | CRUD cabang/gudang + zona waktu. | `GET+POST /companies/{company}/branches`, `GET+PUT+DELETE .../branches/{branch}` (`cabang.kelola`) |
| Katalog | CRUD kategori, item barang/jasa, supplier; hapus butuh konfirmasi. | `.../categories`, `.../items`, `.../suppliers` (`katalog.kelola`) |
| Harga per Cabang | Atur harga jual per cabang (riwayat harga + audit `price.set`). | `PUT .../items/{item}/prices/{branch}` (`harga.atur`) |
| Role & Akses | Lihat katalog izin + template, CRUD role, ubah isi role. | `GET .../access/permissions`, `GET .../access/templates`, `GET+POST .../access/roles`, `GET+PUT+DELETE .../access/roles/{role}` (ubah: `pengaturan.kelola`) |
| Penempatan | Tempatkan karyawan ke cabang + role + tanggal; tutup riwayat. | `GET .../access/assignments`, `POST .../access/users/{user}/assignments`, `GET .../access/assignments/{id}`, `POST .../close` (`karyawan.kelola`) |
| Beri/Cabut Role | Assign/revoke role ke user. | `POST .../access/users/{user}/roles`, `DELETE .../users/{user}/roles/{role}` (`karyawan.kelola`) |
| Audit Log | Filter aksi/entitas + before/after + IP. | `GET .../audit-logs` (`audit.lihat`) |
| Impor / Ekspor | Template CSV, impor item/stok/supplier/karyawan (dry-run + error per baris), ekspor item. | `GET .../imports/template/{type}`, `POST .../imports/{type}`, `GET .../exports/items` (`impor.jalankan`) |
| Konsolidasi | Laba rugi gabungan semua cabang + rebuild ringkasan. | `GET .../reports/consolidation`, `POST .../reports/refresh` (`laporan.konsolidasi`) |
| Hari Libur | Libur perusahaan (nasional milik super admin, §10). | `GET .../holidays`, `POST .../holidays`, `DELETE .../holidays/{id}` (`pengaturan.kelola`) |

## 4. Manajer Cabang

`transaksi.buat/diskon/void`, `retur.buat/setujui`, `nota.lihat`, `shift.buka/tutup`,
`stok.transfer`, `stok.opname`, `po.buat`, `biaya.input`, `jadwal.lihat/atur`,
`absensi.koreksi/approve`, `pelanggan.lihat/kelola`, `dashboard.konsolidasi`.

| Halaman | Penjelasan | Endpoint |
|---|---|---|
| Kasir / POS | Buka shift, buat nota campur barang+jasa (diskon salah-satu, PPN, split payment, DP, idempotency), tutup shift + selisih. | `POST .../branches/{branch}/shifts`, `POST .../branches/{branch}/sales` (`transaksi.buat`, `shift.buka/tutup`) |
| Nota | Cari + detail nota cabang. | `GET .../branches/{branch}/sales`, `GET .../{sale}` (`nota.lihat`) |
| Void & Retur | Void langsung + setujui pengajuan void/diskon/refund dari inbox (punya ketiga izin putuskan). | `POST .../sales/{sale}/void` (`transaksi.void`), `POST .../sales/{sale}/returns` (`retur.buat`), inbox §1 |
| Transfer Stok | Buat → ajukan → setujui → kirim → terima → batal (penuh, satu izin). | `.../stock-transfers` + `/submit /approve /ship /receive /cancel` (`stok.transfer`) |
| Opname | Buat → ajukan → setujui/tolak (penuh, satu izin). | `.../branches/{branch}/stock/opnames` + `/submit /approve /reject` (`stok.opname`) |
| Pembelian | Buat + ajukan PO; setujui/esan/terima/batal TIDAK bisa (butuh `po.setujui`). | `GET+POST .../purchase-orders`, `.../submit` (`po.buat`) |
| Biaya | Catat biaya draft; setujui TIDAK bisa (butuh `biaya.setujui`). | `GET+POST .../expenses` (`biaya.input`), `GET .../expense-categories` |
| Jadwal & Absensi | Atur pola/roster/override, lihat kehadiran cabang, koreksi + putuskan cuti, reset device. | `GET .../work-shifts`, `.../users/{user}/schedule*` (lihat `jadwal.lihat`, ubah `shift.kelola`), `GET .../branches/{branch}/attendance` + `DELETE .../users/{user}/device` (`absensi.approve`), `POST .../leaves/{leave}/decide` (`absensi.approve`), `PUT .../attendances/{id}` (`absensi.koreksi`) |
| Pelanggan & Jasa | CRUD pelanggan, kelola order jasa (buat, DP, proses, selesai, bayar, batal — proses/selesai juga bisa oleh pelaksana via `jasa.proses`). | `.../customers` (`pelanggan.lihat/kelola`), `.../branches/{branch}/service-orders*` (`transaksi.buat`) |

Batasan: tanpa `keuangan.lihat` (tidak ada laporan/jurnal), tanpa `po.setujui`/`biaya.setujui`
(tombol setujui PO & biaya disembunyikan), tanpa `katalog.kelola`/`harga.atur`.

## 5. Kasir

`transaksi.buat`, `nota.lihat`, `shift.buka/tutup`, `pelanggan.lihat`.

| Halaman | Penjelasan | Endpoint |
|---|---|---|
| Kasir / POS | Sama seperti Manajer minus void/retur langsung. | §4 (tanpa void/returns) |
| Nota | Cari + detail (cabang sendiri). | `GET .../branches/{branch}/sales`, `GET .../{sale}` |
| Pengajuan | Minta void + nota diskon ke atasan (refund TIDAK bisa: butuh `retur.buat`). Tombol Setujui/Tolak disembunyikan. | `POST .../approvals` (aksi `sale.void`, `sale.discount`) |
| Pelanggan | Lihat saja (kelola butuh `pelanggan.kelola`). | `GET .../customers`, `GET .../{customer}` |

Batasan: tidak bisa void/retur/diskon langsung, tidak bisa setujui apa pun, tidak ada stok/PO/laporan.

## 6. Staf Gudang

`stok.transfer`, `stok.opname`, `po.buat`, `nota.lihat`.

| Halaman | Penjelasan | Endpoint |
|---|---|---|
| Stok | Saldo + riwayat mutasi per cabang; catat mutasi manual (izin `stok.opname`). | `GET .../branches/{branch}/stock`, `GET .../stock/movements`, `POST .../stock/movements` |
| Transfer / Opname | Penuh seperti Manajer (§4). | sama seperti §4 |
| Pembelian | Buat + ajukan PO; lihat penerimaan (`GET .../receipts` tanpa izin khusus). | `GET+POST .../purchase-orders`, `.../submit` |
| Nota | Lihat saja. | `GET .../branches/{branch}/sales` |

Batasan: tidak ada kasir (tanpa `transaksi.buat`), tidak ada approve PO, tidak ada keuangan.

## 7. Pelaksana Jasa

`nota.lihat`, `jadwal.lihat`, `jasa.proses`, `gaji.lihat_sendiri`:

| Halaman | Penjelasan | Endpoint |
|---|---|---|
| Order Jasa (eksekusi) | Proses + selesaikan order jasa yang ditugaskan (bahan terpotong + komisi saat selesai). Buat order, DP, bayar, batal: TIDAK bisa (butuh `transaksi.buat` — ranah kasir/front desk). | `POST .../service-orders/{order}/process`, `POST .../complete` (`jasa.proses`) |
| Jadwal Saya | Pola + roster pribadi. | `GET .../users/{user}/schedule` (`jadwal.lihat`) |
| Nota | Lihat nota (mis. komisi jasa). | `GET .../branches/{branch}/sales` (`nota.lihat`) |
| Absensi / Cuti / Slip / Kartu | Halaman global §1. | §1 |

## 8. Admin Keuangan

`keuangan.lihat`, `biaya.input/setujui`, `periode.tutup`, `laporan.konsolidasi`,
`gaji.kelola`, `nota.lihat`, `dashboard.konsolidasi`.

| Halaman | Penjelasan | Endpoint |
|---|---|---|
| Biaya | Kategori (CRUD) + catat → setujui/tolak. Setujui menjurnal beban+kredit kas dan masuk laba rugi. | `.../expense-categories` (ubah: `biaya.setujui`), `GET+POST .../expenses` (`biaya.input`), `.../approve /reject` (`biaya.setujui`) |
| Hutang | Tagihan supplier (dari penerimaan PO), bayar parsial/penuh (tolak lebih bayar), aging `lancar/1_30/31_60/61_90/lebih_90`. | `GET .../supplier-bills`, `GET .../{bill}`, `POST .../{bill}/pay` (`biaya.setujui`), `GET .../supplier-bills-aging` |
| Piutang | Catat bon + cicilan + aging. | `GET+POST .../receivables`, `POST .../{receivable}/pay` (`biaya.setujui`), `GET .../receivables-aging` |
| Jurnal | Daftar entri + neraca saldo. | `GET .../journal-entries`, `GET .../reports/trial-balance` (`keuangan.lihat`) |
| Laporan | Laba rugi (cabang/konsolidasi), harian, arus kas, neraca (aktiva = kewajiban + modal). | `GET .../reports/profit-loss?from&to&branch_id`, `GET .../reports/daily`, `GET .../reports/cash-flow`, `GET .../reports/balance-sheet`, `GET .../reports/consolidation` |
| Tutup Periode | Tutup/buka bulan (blokir posting ke bulan tutup; tolak bila ada biaya draft / gaji belum dibayar). | `GET .../accounting-periods`, `POST .../close`, `POST .../{period}/reopen` (`periode.tutup`) |
| Penggajian | Komponen (tetap/harian/komisi + flag `taxable`/`bpjs_base` → PPh annualisasi + BPJS 4%), assign ke karyawan, periode (hitung → review → bayar). Setujui periode TIDAK bisa (butuh `gaji.setujui`). | `.../salary-components*`, `POST .../users/{user}/salary-components`, `.../payroll-periods*` + `/calculate /adjustments /review /pay` (`gaji.kelola`) |

## 9. HR

`karyawan.kelola`, `jadwal.lihat/atur`, `shift.kelola`, `absensi.koreksi/approve`,
`gaji.kelola/setujui`.

| Halaman | Penjelasan | Endpoint |
|---|---|---|
| Karyawan | Buat akun karyawan (password awal sekali-tampil / acak) + reset sandi + beri/cabut role + penempatan cabang + riwayat. | `POST .../users` + `POST .../users/{user}/reset-password`, `.../access/users/{user}/roles*`, `.../access/users/{user}/assignments`, `.../access/assignments*` (`karyawan.kelola`) |
| Shift & Jadwal | CRUD master shift, pola mingguan, override, lihat jadwal siapa pun, libur nasional: TIDAK (butuh `pengaturan.kelola`). | `GET+POST+PUT+DELETE .../work-shifts` (ubah: `shift.kelola`), `.../users/{user}/schedule-pattern` (`jadwal.lihat`, ganti: `shift.kelola`), `.../schedule-overrides` (`shift.kelola`) |
| Absensi | Kehadiran per cabang per hari, koreksi, putuskan cuti, reset device 1-akun-1-perangkat. | `GET .../branches/{branch}/attendance` + `DELETE .../users/{user}/device` (`absensi.approve`), `PUT .../attendances/{id}` (`absensi.koreksi`), `POST .../leaves/{leave}/decide` |
| Penggajian | Penuh: seperti Admin Keuangan (§8) DITAMBAH setujui periode. | §8 + `POST .../payroll-periods/{period}/approve` (`gaji.setujui`) |
| Kartu Karyawan | Lihat/cetak kartu + QR siapa pun. | `GET .../users/{user}/card`, `GET .../card/qr` |

## 10. Super Admin (`is_super_admin`, middleware `super.admin`)

| Halaman | Penjelasan | Endpoint |
|---|---|---|
| Paket | CRUD paket langganan (nama, batas cabang/karyawan, fitur, harga). | `GET+POST /plans`, `GET+PUT+DELETE /plans/{plan}` |
| Langganan Perusahaan | Ubah status (trial/aktif/tenggang/read-only/ditangguhkan), paket, masa aktif. | `PUT /companies/{company}/subscription` |
| Invoice Langganan | Terbitkan + tandai lunas (otomatis perpanjang +30 hari). | `GET+POST .../subscription-invoices`, `GET .../{invoice}`, `POST .../{invoice}/pay` |
| Libur Nasional | Berlaku semua perusahaan. | `POST /holidays`, `DELETE /holidays/{holiday}` |

## 11. Matriks izin → halaman (ringkas)

| Izin | Halaman yang dibuka |
|---|---|
| `transaksi.buat` | POS, buat order jasa + DP/bayar/batal, pengajuan void/diskon |
| `jasa.proses` | Proses + selesaikan order jasa |
| `transaksi.diskon` | Putuskan nota diskon (inbox) |
| `transaksi.void` | Void langsung + putuskan void (inbox) |
| `retur.buat` | Retur langsung + ajukan refund |
| `retur.setujui` | Putuskan refund (inbox) |
| `nota.lihat` | Nota + seksi dashboard penjualan |
| `shift.buka` / `shift.tutup` | Buka / tutup shift |
| `stok.transfer` | Transfer (penuh) + ajukan & putuskan (inbox) |
| `stok.opname` | Mutasi manual + opname (penuh) + ajukan & putuskan (inbox) |
| `po.buat` | Buat + ajukan PO (+ ajukan `po.approve`) |
| `po.setujui` | Setujui/esan/terima/batal PO + putuskan (inbox) |
| `keuangan.lihat` | Jurnal, semua laporan, seksi dashboard keuangan |
| `biaya.input` / `biaya.setujui` | Catat / kelola + putuskan biaya; bayar hutang/piutang (`setujui`) |
| `periode.tutup` | Tutup/buka periode |
| `laporan.konsolidasi` | Konsolidasi + rebuild ringkasan |
| `jadwal.lihat` | Lihat jadwal + seksi dashboard SDM (ubah jadwal: `shift.kelola`) |
| `shift.kelola` | Master shift + pola + override |
| `absensi.approve` | Kehadiran cabang, putuskan cuti, reset device |
| `absensi.koreksi` | Koreksi absensi |
| `gaji.kelola` | Komponen + periode + hitung/review/bayar + slip siapa pun |
| `gaji.setujui` | Setujui periode gaji |
| `katalog.kelola` / `harga.atur` | Katalog+supplier / harga per cabang |
| `cabang.kelola` | CRUD cabang |
| `karyawan.kelola` | Role user + penempatan + seksi dashboard karyawan |
| `pengaturan.kelola` | CRUD role + libur perusahaan |
| `audit.lihat` | Audit log |
| `impor.jalankan` | Impor/ekspor CSV |
| `pelanggan.lihat` / `pelanggan.kelola` | Lihat / CRUD pelanggan |
| `dashboard.konsolidasi` | Penerima notifikasi selisih shift (bukan gerbang halaman) |

## 12. Izin yang dihapus (dulu cadangan, kini dibersihkan)

`jadwal.atur` dan `langganan.kelola` **dihapus dari katalog** (tes `PermissionGuardTest`
menjamin tiap izin katalog terpakai di backend/frontend): ubah jadwal memakai `shift.kelola`
(sesuai spek C5), dan langganan dikunci `super.admin` tanpa peran perusahaan.
`pelanggan.ekspor` kini dienforce di `GET .../exports/customers`;
`gaji.lihat_sendiri` kini dienforce di slip (milik sendiri perlu izin ini, atau `gaji.kelola`).

## 13. Progres UI Web (Stitch) per halaman

Satu halaman = satu prompt Stitch + satu layar Vue. Centang `[x]` bila layar selesai dan
menempel ke API-nya; `[~]` bila prompt sudah dibuat tapi layar belum jadi.

### A. Global — 11/11 (prompt: 11/11, layar: 11/11)

- [x] Login / Register
- [x] Pilih Perusahaan
- [x] Profil Saya
- [x] Dashboard
- [x] Notifikasi
- [x] Persetujuan (inbox)
- [x] Absensi Saya
- [x] Cuti Saya
- [x] Slip Gaji Saya
- [x] Kartu Karyawan
- [x] Langganan (lihat)

### B. Operasional — 10/10 (prompt: 10/10, layar: 10/10)

- [x] Kasir / POS
- [x] Nota
- [x] Void & Retur (dialog di layar Nota)
- [x] Pelanggan
- [x] Order Jasa
- [x] Stok (saldo + mutasi)
- [x] Transfer Stok
- [x] Opname
- [x] Pembelian (PO + penerimaan)
- [x] Katalog (kategori + item + supplier)

### C. Manajemen — 9/9 (prompt: 9/9, layar: 9/9)

- [x] Cabang
- [x] Role & Akses
- [x] Karyawan & Penempatan
- [x] Audit Log
- [x] Harga per Cabang
- [x] Jadwal & Shift
- [x] Kehadiran & Cuti (kelola HR)
- [x] Penggajian (komponen + periode)
- [x] Impor / Ekspor

### D. Keuangan — 6/6 (prompt: 6/6, layar: 6/6)

- [x] Biaya
- [x] Hutang
- [x] Piutang
- [x] Jurnal (entri + neraca saldo)
- [x] Laporan (laba rugi, harian, arus kas, neraca, konsolidasi)
- [x] Tutup Periode

### E. Super Admin — 4/4 (prompt: 4/4, layar: 4/4)

- [x] Paket (`/admin/plans`)
- [x] Langganan Perusahaan kelola (bagian "Kelola langganan" di halaman Langganan, khusus admin layanan)
- [x] Invoice Langganan (bagian "Tagihan langganan" di halaman Langganan, khusus admin layanan)
- [x] Libur Nasional (`/admin/holidays`)

### F. Langganan (perusahaan, lihat) — 0/1

- [ ] Status Langganan (sudah tercakup halaman global "Langganan (lihat)" — tidak dihitung ganda)

### G. Praluncur (tambahan auth & onboarding) — 2/2

- [x] Lupa Password (`/lupa-password`) — kirim tautan reset via email
- [x] Atur Ulang Password (`/reset-password?token&email`) — dipakai tautan dari email

Tambahan non-halaman: tombol "Lupa sandi?" di Login, kartu "Tambah karyawan baru" + aksi
"Reset sandi" di halaman Karyawan & Penempatan, dan kartu "Ganti kata sandi" di Profil Saya.
