# Monitoring Progress — Aurevant PKK

Terakhir diperbarui: 2026-10-26

Branch: `feat/ui-design-system`

Commit terakhir: `2adaf5f fix(service): snapshot order prices and prevent double stock`

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

## Setelah P0

### Gap web

- [ ] Stok minimum dan alert.
- [ ] Retur supplier.
- [ ] Ekspor laporan.
- [ ] Top produk dan perbandingan cabang.
- [ ] Customer 360.
- [ ] Booking/agenda jasa.
- [ ] Estimasi dan paket jasa.
- [ ] HR: saldo cuti, lembur, tukar shift.
- [ ] Finance: budget, COA, recurring, aset, rekonsiliasi.
- [ ] Plan limits.
- [ ] Sesi perangkat dan 2FA.
- [ ] Notifikasi lengkap.

### Android

- [ ] Pisahkan dirty tree Android dari pekerjaan web.
- [ ] Build/run pertama setelah izin user.
- [ ] Theme calm green light-only, hapus ungu + dynamic color.
- [ ] Purchase/PO, shift close, scheduling, customer CRUD.
- [ ] Finance journal/report/periods.
- [ ] Detail service order + DP, line-item picker, approval submission.
- [ ] HR partial.

### Landing page

- [ ] Struktur marketing lengkap.
- [ ] Copywriting dan visual.
- [ ] Responsive + aksesibilitas.
- [ ] CTA registrasi/demo.

## Verifikasi terakhir

```text
php artisan test                         148 passed, 1328 assertions  (P0)
php artisan test --filter=ItemTest       4 passed, 68 assertions      (katalog)
pint targeted                            lolos
npm run type-check                       lolos
npm run lint                             lolos
npm run build                            lolos
php artisan migrate --force              migrasi image_path berjalan
```

QA katalog:

```text
item sementara QAIMG462949
thumbnail: /storage/item-images/... 640x640
link gambar penuh: ada
overflow mobile: tidak ada
cleanup item: berhasil
```

## Jangan dilakukan tanpa izin

- Jangan build/run Android.
- Jangan stage file Android, `postman/`, atau `android/.kotlin/`.
- Jangan stage perubahan whitespace di `PermissionCatalog.php`.
- Jangan memperbaiki kegagalan Pint lama di `ViewTeamSchedule.php` dan
  `PermissionCatalog.php` tanpa scope terpisah.
