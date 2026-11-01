# Monitoring Progress — Aurevant PKK

Terakhir diperbarui: 2026-10-27

Branch: `feat/ui-design-system`

Commit fitur terakhir: `081323a feat(stock): add per-branch minimum stock alerts`

Dokumen ini adalah papan monitoring kerja aktif. Acuan produk tetap `PROGRESS.md`,
`doc/spek.md`, dan `doc/analisis-fitur-praluncur.md`.

## Status saat ini

- [x] Phase 9 UI polish selesai dan di-commit.
- [x] Gambar item katalog selesai di backend dan frontend.
- [x] Bug simpan item tanpa komisi diperbaiki (`commission_value: null` -> `0`).
- [x] QA visual katalog desktop + mobile lolos.
- [x] Verifikasi katalog: 141 tes server, `type-check`, `lint`, dan `build` lolos.
- [x] P0 tenant/branch scope, offboarding, service order, dan PII selesai.
- [x] Verifikasi P0: 148 tes server lolos, 1328 assertions; Pint targeted lolos.
- [x] Perubahan P0 sudah di-commit (`e791673`, `2adaf5f`).
- [x] S04 stok minimum dan alert selesai (backend, web, dashboard, notifikasi).
- [x] Verifikasi S04: 150 tes server lolos, 1369 assertions; type-check, lint, build lolos.
- [x] S05 retur supplier selesai (backend, web, riwayat, notifikasi, jurnal, stok, hutang).
- [x] Verifikasi S05: 156 tes server lolos, 1460 assertions; type-check, lint, build lolos.
- [x] S06 ekspor laporan selesai (Excel/PDF untuk laba rugi, harian, arus kas, neraca, neraca saldo, konsolidasi).
- [x] Verifikasi S06: 161 tes server lolos, 1490 assertions; type-check, lint, build lolos.
- [ ] File Android masih dirty dan belum boleh di-build sebelum izin.

## Selesai — Gambar item katalog

Backend:

- [x] Migrasi `items.image_path` nullable.
- [x] `ItemDTO` mengekspos `imagePath` dan `imageUrl`.
- [x] Endpoint `POST /companies/{company}/items/{item}/image`.
- [x] Endpoint `DELETE /companies/{company}/items/{item}/image`.
- [x] Upload gambar baru menghapus file lama.
- [x] Hapus item ikut menghapus file gambar.
- [x] Validasi JPG/JPEG/PNG/WEBP maksimal 2 MB.
- [x] Test upload, replace, validasi, scope lintas perusahaan, dan cleanup.

Frontend:

- [x] Form katalog punya pilihan gambar + pratinjau.
- [x] Thumbnail tampil di tabel dan bisa dibuka ukuran penuh.
- [x] Gambar diunggah setelah item dibuat/diperbarui.
- [x] Hapus gambar mengikuti simpan form.
- [x] Object URL dibersihkan saat modal ditutup.
- [x] QA desktop 1440px dan mobile 390px tanpa overflow halaman.

Catatan bug yang sudah diperbaiki:

- Form mengirim `commission_value: null` saat item tanpa komisi.
- Kolom DB `commission_value` adalah `NOT NULL default 0`, sehingga update gagal 500.
- Controller sekarang menormalisasi komisi null menjadi `0`; regression test ditambahkan.

## Selesai — P0 tenant dan otorisasi

Urutan pengerjaan:

1. [x] Perketat `/companies` agar hanya perusahaan dengan assignment aktif; superadmin tetap melihat semua.
2. [x] `SetCompanyTeam` menolak user non-superadmin tanpa assignment aktif.
3. [x] `Access::scopeBranches()` fail-closed `[]` tanpa assignment; `null` hanya untuk assignment seluruh perusahaan.
4. [x] Urutan middleware: `auth:sanctum` -> `company.team` -> `scope.cabang` -> `subscription.aktif`.
5. [x] Tutup assignment mencabut role tim + token bila user tidak punya assignment aktif lain.
6. [x] Tolak penutupan assignment `Pemilik` terakhir.
7. [x] Audit saat assignment ditutup; revoke role sudah tercatat.
8. [x] Service order memakai snapshot harga dan tidak memotong bahan dua kali.
9. [x] `PayServiceOrder` memakai harga snapshot, bukan harga cabang terbaru.
10. [x] Filter PII untuk cuti, kartu karyawan, role, dan assignment.
11. [x] `WithPermissions::owner()` membuat assignment; helper `member()` untuk karyawan.
12. [x] `CompanyTest`/`ScopeTest`/`AccessTest` diperbarui untuk perilaku baru.
13. [x] Test regresi P0 ditambahkan dan suite penuh lolos.

Catatan desain:

- `RequireBranchScope` tetap mengecek cabang pada route yang membawa konteks cabang.
- Route company-wide tidak dipaksa membawa `branch_id`; akses tetap dijaga permission dan
  keanggotaan aktif di `SetCompanyTeam`.
- Superadmin sengaja bypass pengecekan assignment dan daftar perusahaan.

## Selesai — S04 stok minimum dan alert

Backend:

- [x] Migrasi `stock_balances.min_stock` nullable, presisi `decimal(15,3)`.
- [x] `StockBalanceDTO` mengekspos `minStock` dan `lowStock`.
- [x] Endpoint `PUT /companies/{company}/branches/{branch}/stock/{item}/minimum`.
- [x] Seting minimum memakai permission `stok.opname` dan mencatat audit `stock.minimum.update`.
- [x] Filter daftar saldo `low_stock=1` (qty <= minimum).
- [x] `LowStockAlert` mengirim notifikasi `stok.menipis` hanya saat saldo turun melewati batas.
- [x] Alert dipanggil dari mutasi stok dan pengiriman transfer keluar.
- [x] Dashboard menambahkan `operasional.stok_menipis` dan `stok_menipis_items` sesuai scope cabang.
- [x] Tipe notifikasi `stok.menipis` terdaftar untuk preferensi pengguna.

Frontend:

- [x] Tabel stok menampilkan kolom minimum dan badge `Menipis`.
- [x] Filter "Hanya menipis" pada tab saldo.
- [x] Modal "Atur minimum" per item; kosongkan nilai untuk menonaktifkan alert.
- [x] Widget "Stok menipis" di dashboard dengan tautan ke halaman stok.
- [x] Notifikasi `stok.menipis` punya label, grup preferensi, dan tautan `/stock`.

Verifikasi:

- [x] `StockTest` mencakup permission, validasi, filter, dashboard, crossing alert, dan scope item.
- [x] `TransferTest` mencakup alert saat transfer keluar melewati minimum.
- [x] Migrasi dijalankan di database dev.
- [x] QA browser: filter, tombol + modal minimum, dan widget dashboard tampil tanpa error.

## Selesai — S05 retur supplier

Backend:

- [x] Migrasi `supplier_returns` dan `supplier_return_items`.
- [x] Retur terikat ke satu penerimaan (`goods_receipt`), bukan langsung ke PO.
- [x] Qty retur dibatasi `qty_received - qty_already_returned`; baris duplikat ditolak.
- [x] Nilai retur memakai `unit_cost` saat penerimaan dan dibatasi sisa hutang supplier.
- [x] Stok keluar memakai tipe `retur_supplier` dan HPP rata-rata berjalan.
- [x] Jurnal retur: debit `2110`, kredit `1310`, selisih harga ke `5100`.
- [x] Tagihan supplier berkurang; `paid_amount` tidak diubah.
- [x] Audit `po.retur`, notifikasi `po.retur`, dan permission existing `po.setujui`.
- [x] Endpoint daftar retur + buat retur per penerimaan.

Frontend:

- [x] Detail PO menampilkan riwayat retur dan qty yang sudah diretur per penerimaan.
- [x] Tombol `Retur supplier` di kartu penerimaan dan footer detail PO.
- [x] Modal retur: pilih penerimaan, qty per baris, alasan, catatan, dan ringkasan sisa hutang.
- [x] Validasi qty, alasan wajib, dan total retur tidak melebihi sisa hutang.
- [x] Notifikasi `po.retur` punya label dan grup preferensi.
- [x] Riwayat stok menampilkan label `Retur ke supplier`.

Verifikasi:

- [x] `SupplierReturnTest`: 5 test, 87 assertions.
- [x] `StockLedgerTest`: 9 test, 26 assertions.
- [x] Migrasi dijalankan di database dev.
- [x] QA browser desktop: retur 1 botol dari PO #10 berhasil; stok 40 -> 39, tagihan 750.000 -> 720.000, riwayat retur tampil.
- [x] QA mobile 390px: modal retur tanpa overflow halaman dan input qty tetap terjangkau.

## Selesai — S06 ekspor laporan

Backend:

- [x] Endpoint `GET /companies/{company}/reports/{report}/export` dengan format `xlsx` dan `pdf`.
- [x] Mendukung laba rugi, laporan harian, arus kas, neraca, neraca saldo, dan konsolidasi.
- [x] Validasi tanggal, format, dan cabang; cabang lintas perusahaan menghasilkan 404.
- [x] Filter cabang untuk laba rugi, harian, dan arus kas; konsolidasi tetap butuh izin `laporan.konsolidasi`.
- [x] `ReportExportDocument`, `BuildReportExport`, `ReportXlsxWriter`, dan view PDF generik.
- [x] Excel memakai format Rupiah, header bergaya, dan auto-width; PDF otomatis landscape untuk tabel lebar.

Frontend:

- [x] Tombol `Ekspor Excel` dan `Ekspor PDF` di halaman laporan.
- [x] Tombol ekspor neraca saldo di tab neraca halaman jurnal.
- [x] State loading, disable saat data belum siap, dan notifikasi hasil unduhan.
- [x] CSV dan cetak lama tetap tersedia.

Verifikasi:

- [x] `ReportExportTest`: 5 test, 30 assertions.
- [x] Full suite: 161 test, 1490 assertions.
- [x] `type-check`, `lint`, dan `build` lolos.
- [x] QA API: XLSX keenam laporan dan PDF laba rugi + neraca saldo berhasil diunduh.
- [x] Isi XLSX diperiksa: judul/periode benar, neraca saldo seimbang (Rp 30.296.000 debit = kredit).

## Selesai — S13 top produk & perbandingan cabang

Backend:

- [x] Endpoint `GET /companies/{company}/reports/top-products` dengan filter tanggal, cabang, dan limit 1–50.
- [x] Agregasi qty, omzet, HPP, marjin, dan jumlah transaksi dari nota `lunas`/`retur`.
- [x] `SalesPerformanceRepositoryInterface` + implementasi Eloquent; hasil diurutkan berdasarkan omzet.
- [x] Konsolidasi menambahkan `avgTicket` dan `margin` per cabang serta total grup.
- [x] Ekspor `top-products` (Excel/PDF) dan kolom rata-rata/marjin pada ekspor konsolidasi.

Frontend:

- [x] Tab `Top produk` dengan filter tanggal/cabang, statistik, grafik top 5, tabel lengkap, dan CSV.
- [x] Tombol ekspor Excel/PDF memakai endpoint dan slug `top-products`.
- [x] Tabel konsolidasi menampilkan rata-rata per transaksi dan marjin.

Verifikasi:

- [x] `ReportTest` top produk + konsolidasi: qty/omzet/HPP/marjin/transaksi, filter cabang, izin 403.
- [x] Full suite: 162 test, 1515 assertions.
- [x] `type-check`, `lint`, dan `build` lolos.
- [x] QA API: endpoint mengembalikan 8 produk demo, konsolidasi menghitung rata-rata/marjin, ekspor XLSX 200 OK.

## Selesai — Customer 360

Backend:

- [x] Endpoint `GET /companies/{company}/customers/{customer}/profile` dengan izin `pelanggan.lihat`.
- [x] Ringkasan transaksi, omzet kotor, refund, net belanja, rata-rata transaksi, dan pembelian terakhir.
- [x] Piutang terbuka, total order jasa, dan order jasa aktif.
- [x] Riwayat 10 transaksi terakhir (termasuk refund/net), order jasa, dan piutang.
- [x] Semua query dibatasi perusahaan; pelanggan lintas perusahaan menghasilkan 404.

Frontend:

- [x] Nama pelanggan di daftar membuka modal profil 360.
- [x] Statistik pelanggan, riwayat transaksi, order jasa, dan piutang dalam satu tampilan.
- [x] Status nota/order memakai badge dan data uang diformat Rupiah.

Verifikasi:

- [x] `CustomerTest`: profil + refund + piutang + order, izin 403, lintas perusahaan 404.
- [x] Full suite: 163 test, 1531 assertions.
- [x] `type-check`, `lint`, dan `build` lolos.
- [x] QA API demo: profil Acong Wijaya memuat 1 transaksi, net Rp76.500, piutang Rp600.000, 2 order jasa, dan 2 piutang.

## Selesai - Agenda order jasa

Backend:

- [x] Filter daftar order jasa dengan `scheduled_from` dan `scheduled_to` (+ validasi rentang).
- [x] Hasil agenda diurutkan berdasarkan jadwal terdekat, tetap dibatasi cabang.
- [x] Order tanpa jadwal tidak ikut masuk agenda.

Frontend:

- [x] Tab `Agenda` di Order Jasa dengan rentang tanggal dan filter teknisi.
- [x] Agenda dikelompokkan per hari, menampilkan jam, pelanggan, pekerjaan, teknisi, total, dan status.
- [x] Order baru bisa mengisi tanggal + jam jadwal.
- [x] Pintasan `7 hari ke depan`; agenda ikut disegarkan setelah aksi order.

Verifikasi:

- [x] `ServiceOrderTest`: filter rentang tanggal, urutan agenda, pengecualian order tanpa jadwal, dan rentang terbalik 422.
- [x] Full suite: 164 test, 1541 assertions.
- [x] `type-check`, `lint`, dan `build` lolos.
- [x] QA API demo: 14 order terbaca; belum ada order terjadwal di data demo sehingga agenda kosong sesuai data.

## Selesai - Estimasi biaya jasa

Backend:

- [x] Endpoint `GET /companies/{company}/branches/{branch}/service-orders/{order}/estimate` menghasilkan PDF.
- [x] Rincian jasa, qty, harga snapshot, total, DP diterima, dan sisa estimasi.
- [x] Memuat pelanggan, jadwal, teknisi, dan catatan; dokumen ditandai bukan nota.
- [x] Akses dibatasi cabang dan tenant (guest 401, tenant lain 403).

Frontend:

- [x] Tombol `Estimasi PDF` pada detail order yang belum dibayar/dibatalkan.
- [x] Unduhan memakai token API dan umpan balik toast.

Verifikasi:

- [x] `ServiceOrderTest`: PDF valid, nama file, guest 401, tenant lain 403.
- [x] Full suite: 165 test, 1549 assertions.
- [x] `type-check`, `lint`, dan `build` lolos.
- [x] QA API demo: estimasi order #14 (`diproses`) terunduh sebagai PDF valid (878 KB).

## Selesai - Paket jasa

Backend:

- [x] Migrasi `service_packages`, `service_package_items`, dan `service_orders.service_package_id`.
- [x] CRUD `GET/POST/PUT/DELETE /companies/{company}/service-packages` dengan izin `katalog.kelola`.
- [x] Paket hanya boleh berisi item bertipe jasa milik company yang sama.
- [x] `CreateServiceOrder` menerima `package_id`; jasa paket diperluas menjadi baris order dengan harga paket dialokasikan proporsional terhadap harga jasa di cabang.
- [x] Paket nonaktif, jasa duplikat dengan baris manual, dan paket lintas tenant ditolak.
- [x] Order menyimpan `service_package_id` dan DTO mengekspos `packageId`/`packageName`.

Frontend:

- [x] Halaman `Paket Jasa` (`/c/:companyId/packages`) untuk daftar, cari, buat, ubah, dan hapus paket.
- [x] Form paket berisi kode, nama, harga, status, dan baris jasa + qty.
- [x] Selector paket pada modal order jasa; jasa paket disembunyikan dari pilihan baris tambahan.
- [x] Detail order menampilkan nama paket.

Verifikasi:

- [x] `ServicePackageTest`: guest 401, CRUD scoped company, validasi jasa/barang, kode unik, paket nonaktif ditolak.
- [x] `ServiceOrderTest`: order dari paket total sesuai harga paket, alokasi harga per jasa, dan penolakan jasa duplikat.
- [x] `ForbiddenTest`: mutasi paket tanpa izin 403.
- [x] Full suite: 169 test, 1594 assertions.
- [x] `type-check`, `lint`, dan `build` lolos.
- [x] QA API demo: paket 2 jasa harga 100.000 membuat order #15 total 100.000 (alokasi 77.272,73 + 22.727,27); data QA dibersihkan.

## Selesai - HR: saldo cuti, lembur, tukar shift

Backend:

- [x] Tabel `leave_balances`, `overtime_requests`, `shift_swaps` + model dan DTO.
- [x] `GET/PUT /companies/{company}/leave-balances` dan `/users/{user}/leave-balances` (HR/pengelola).
- [x] Saldo cuti per tahun + carryover; pengajuan cuti memeriksa saldo dan persetujuan memotong saldo otomatis.
- [x] Lembur: ajukan, daftar, putuskan; notifikasi `lembur.diajukan`/`lembur.disetujui`/`lembur.ditolak`.
- [x] Tukar shift: ajukan, daftar, putuskan; persetujuan membuat override jadwal dan notifikasi `tukar_shift.*`.
- [x] Semua endpoint dibatasi tenant/cabang dan izin yang sudah ada (`absensi.approve`, `shift.kelola`, `karyawan.kelola`).

Frontend:

- [x] Halaman `/hr` (`HrExtrasView.vue`): tab saldo cuti, lembur, dan tukar shift.
- [x] Kelola saldo cuti karyawan, keputusan lembur/tukar shift, dan form pengajuan.
- [x] `LeavesView.vue`: pengelola bisa memuat semua pengajuan cuti, bukan hanya milik sendiri.
- [x] Nav `Saldo & Lembur` ditambahkan.

## Selesai - Finance admin

Backend:

- [x] Bagan akun hierarki: parent/child, tipe, urutan, kode unik per perusahaan.
- [x] Penghapusan akun ditolak bila masih punya anak atau baris jurnal; ubah kode ditolak bila sudah dipakai jurnal.
- [x] Anggaran tahunan/bulanan per cabang/kategori + realisasi dari biaya disetujui.
- [x] Biaya berulang + `run-due`: membuat biaya draft dan memajukan `next_run_at`.
- [x] Aset tetap + penyusutan garis lurus manual (maksimal 120 bulan, berhenti di nilai residu).
- [x] Rekonsiliasi bank: saldo buku dari jurnal, selisih, status `draft`/`selesai`, kunci setelah selesai.
- [x] Audit dan notifikasi `rekonsiliasi.selesai`; izin baca `keuangan.lihat`, tulis `biaya.setujui`, rekonsiliasi `periode.tutup`.

Frontend:

- [x] Halaman `/finance` (`FinanceAdminView.vue`) dengan tab akun, anggaran, biaya berulang, aset tetap, dan rekonsiliasi.
- [x] Form CRUD, aksi jalankan biaya berulang, susutkan aset, dan selesaikan rekonsiliasi.
- [x] Route `/finance` + nav `Akun & Anggaran` dengan guard izin.

## Selesai - Plan limits

- [x] `SubscriptionLimits` menghitung cabang dan karyawan aktif.
- [x] `CreateBranch` dan `CreateEmployee` menolak 422 saat kuota paket habis.
- [x] Ringkasan langganan mengekspos `maxBranches`, `maxEmployees`, `branchesUsed`, dan `employeesUsed`.
- [x] `SubscriptionView.vue` menampilkan pemakaian kuota; limit tidak aktif bila tidak ada langganan/paket.

## Selesai - Sesi perangkat dan 2FA

Backend:

- [x] `GET /auth/sessions`, `DELETE /auth/sessions/{token}`, `DELETE /auth/sessions` (semua sesi lain).
- [x] Login menyimpan nama perangkat (`device_name`) dan menolak sesi saat ini untuk dicabut.
- [x] 2FA TOTP tanpa dependensi baru: setup, aktifkan, status, nonaktifkan dengan konfirmasi password.
- [x] Delapan kode pemulihan terenkripsi + hash; kode pemulihan sekali pakai.
- [x] Login menerima `two_factor_code` dan menolak 422 bila 2FA aktif tanpa kode yang valid.

Frontend:

- [x] `ProfileView.vue`: daftar sesi, cabut sesi, cabut sesi lain, dan kartu 2FA (secret manual, URI otpauth, kode pemulihan).
- [x] `LoginView.vue`: field kode 2FA muncul otomatis saat server meminta langkah kedua.
- [x] `stores/auth.ts`: kirim `device_name` dan `two_factor_code` opsional.

## Selesai - Notifikasi dan bukti biaya

- [x] `payroll.dibayar` dikirim ke setiap karyawan yang punya baris gaji saat payroll dibayar.
- [x] `biaya.diajukan`, `biaya.disetujui`, dan `biaya.ditolak` terhubung ke penerima yang tepat.
- [x] `cuti.*`, `lembur.*`, `tukar_shift.*`, dan `rekonsiliasi.selesai` masuk preferensi notifikasi.
- [x] `ExpensesView.vue`: unggah bukti JPG/PNG/WEBP/PDF maksimal 2 MB, hapus bukti, kirim multipart.
- [x] `FinanceController` menyimpan bukti ke disk `public` (`bukti-biaya/`) dan mengekspos `proofUrl`.

## Setelah P0

### Gap web

- [x] Stok minimum dan alert.
- [x] Retur supplier.
- [x] Ekspor laporan.
- [x] Top produk dan perbandingan cabang.
- [x] Customer 360.
- [x] Booking/agenda jasa.
- [x] Estimasi jasa.
- [x] Paket jasa (bundling beberapa jasa dengan satu harga).
- [x] HR: saldo cuti, lembur, tukar shift.
- [x] Finance: budget, COA, recurring, aset, rekonsiliasi.
- [x] Plan limits.
- [x] Sesi perangkat dan 2FA.
- [x] Notifikasi lengkap.

### Android

- [ ] Pisahkan dirty tree Android dari pekerjaan web.
- [ ] Build/run pertama setelah izin user.
- [ ] Theme calm green light-only, hapus ungu + dynamic color.
- [ ] Purchase/PO, shift close, scheduling, customer CRUD.
- [ ] Finance journal/report/periods.
- [ ] Detail service order + DP, line-item picker, approval submission.
- [ ] HR partial.

### Landing page

- [x] Struktur marketing lengkap: hero, alur, modul, peran, Android, kontrol akses, FAQ.
- [x] Copywriting dan visual calm green, termasuk audit aturan no-ai-slop.
- [x] Responsive 390/768/1440, reduced motion, focus state, dan label aksesibilitas dasar.
- [x] CTA registrasi dan login.
- [ ] CTA demo (belum tersedia).

## Verifikasi terakhir

```text
php artisan test                         181 passed, 1731 assertions  (pasca-P0 lengkap)
pint targeted                            lolos
npm run type-check                       lolos
npm run lint                             lolos
npm run build                            lolos
php artisan migrate --force              10 migrasi baru berjalan di DB demo
```

QA katalog:

```text
item sementara QAIMG462949
thumbnail: /storage/item-images/... 640x640
link gambar penuh: ada
overflow mobile: tidak ada
cleanup item: berhasil
```

## QA API demo (pasca-P0)

Login `pemilik@demo.id` di company 1, lalu GET endpoint baru:

```text
chart-of-accounts       16 akun (hierarki terbaca)
budgets/recurring       0 data (tabel baru siap)
fixed-assets            0 data
bank-reconciliations    0 data
leave-balances          7 saldo karyawan
overtime/shift-swaps    0 data
sessions                200 OK
two-factor              enabled=false
subscription            4/100 cabang, 7/1000 karyawan
```

Dua token QA `QA Web` dibersihkan setelah pengujian.

## Jangan dilakukan tanpa izin

- Jangan build/run Android.
- Jangan stage file Android, `postman/`, atau `android/.kotlin/`.
- Jangan stage perubahan whitespace di `PermissionCatalog.php`.
- Jangan memperbaiki kegagalan Pint lama di `ViewTeamSchedule.php` dan
  `PermissionCatalog.php` tanpa scope terpisah.
