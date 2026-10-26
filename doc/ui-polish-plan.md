# UI Polish Plan — Aurevant PKK

Status: **Phase 9 selesai — siap review dan commit**
Branch: `feat/ui-design-system`  
Design system: [`doc/DESIGN.md`](./DESIGN.md)  
Arah visual: **calm green premium**  
Mode: light only

## 1. Tujuan

Melakukan polish UI web secara menyeluruh dan terukur tanpa mengubah alur bisnis.

Hasil yang ingin dicapai:

- seluruh halaman memakai satu sistem visual;
- hierarchy halaman lebih jelas;
- tabel, form, modal, dan navigasi konsisten;
- pengalaman mobile dan keyboard lebih baik;
- tidak ada hardcoded style baru di luar token;
- perubahan bisa di-review dan di-rollback per fase.

## 2. Guardrail

- Jangan mengubah API, permission, atau alur data. Pengecualian hanya untuk bug endpoint yang sudah menjadi kontrak UI/dokumen dan disetujui saat review.
- Jangan menyentuh file Android.
- Jangan menambah dependency UI baru.
- Jangan menambah dark mode.
- Jangan memakai raw hex/status color baru di view.
- Jangan membuat modal custom; gunakan `AppModal` atau `AppConfirm`.
- Jangan memakai `outline-none` tanpa focus ring pengganti.
- Jangan mengubah halaman dengan data yang belum dipahami.
- Jangan men-stage file Android, `postman/`, atau `android/.kotlin/`.
- Commit hanya setelah verifikasi dan persetujuan.
- Setiap fase harus lolos type-check, lint, dan build sebelum lanjut.

## 3. Baseline Audit

### High

- [x] Modal custom `z-20` berada di bawah header `z-30` dan bottom nav `z-40`.
  - `ExpensesView`, `ReceivablesView`, `RolesView`, `PlansView`, `TenantsView`, `ProfileView` sudah memakai `AppModal`.
- [x] Focus ring shared components sudah diganti ring 2px dengan kontras cukup; audit per view menyusul di fase halaman.
- [x] Baris/card click-only di halaman operasional sudah punya aksi keyboard.
  - `ApprovalsView`, `SalesView`, `PurchaseView`, `TransfersView`, `OpnamesView`, `ServicesView`.
- [x] Baris/card click-only di halaman admin sudah diganti aksi eksplisit atau dilengkapi keyboard.
  - `NotificationsView` card `role="button"` + Enter/Space, `AuditView` tombol Inspeksi, `TenantsView` tombol Detail (Phase 8).
- [x] Kontrol POS sudah diberi label, touch target, dan layout mobile.
- [x] Microcopy sidebar kontrasnya terlalu rendah — diperbaiki di Phase 2.
- [x] Demo credential tampil permanen di login - dibatasi ke development atau env demo di Phase 3.

### Medium

- [ ] Dua sistem page header: `AppPageHeader` vs heading manual.
- [ ] Stat card diduplikasi di banyak halaman.
- [x] `AppCard` sudah punya padding prop; migrasi `!p-3` dilakukan saat polish view.
- [ ] Tabel mentah di `LeavesView`, `HrAttendanceView`, `TenantsView`, `JournalView`.
- [ ] Filter bar dan field belum seragam.
- [ ] Loading state literal “Memuat…” di banyak halaman.
- [ ] Tabel mobile belum punya strategi seragam.
- [ ] Raw Tailwind status colors tersebar di beberapa komponen.
- [x] `AppTabs` sudah mendukung `aria-controls` lewat prop `panel-prefix`; pemasangan panel dilakukan per view.

### Low

- [ ] Label status/bucket masih ada yang mentah.
- [ ] Type scale 10–11px terlalu sering.
- [ ] Beberapa tanggal/kode belum diformat.
- [ ] Beberapa tombol ikon belum punya accessible name.

## 4. Urutan Eksekusi

### Phase 0 — Design contract

- [x] Buat `doc/DESIGN.md`.
- [x] Buat rencana bertahap ini.
- [x] Tetapkan branch kerja.
- [x] Review dan approval arah visual.
- [x] Kunci token final sebelum menyentuh komponen.

**Gate:** jangan mulai Phase 1 sebelum `DESIGN.md` disetujui.

### Phase 1 — Foundation dan shared components

Tujuan: satu perubahan di komponen bersama otomatis memperbaiki seluruh halaman.

- [x] Update token di `client/src/assets/main.css`.
- [x] Tambah shadow, type scale, focus ring, ambient background, page transition.
- [x] Rapikan `AppButton`, `AppCard`, `AppStat`, `AppBadge`, `AppBanner`.
- [x] Rapikan `AppInput`, `AppSearch`, `AppSelect`, `AppTextarea`, `AppMoneyInput`, `AppField`.
- [x] Rapikan `AppTable`, `AppPager`, `AppEmpty`, `AppSkeleton`.
- [x] Rapikan `AppModal`, `AppConfirm`, `AppDropdown`, `AppTabs`.
- [x] Rapikan `AppPageHeader`.

**Acceptance:**

- seluruh komponen punya state default, hover, focus, disabled, loading, error;
- tidak ada raw color baru;
- tidak ada regresi type-check;
- build CSS bersih.

**Status:** selesai dan di-commit — `type-check`, `lint`, dan `build` lulus (177 modul, CSS 67.13 kB).

**Commit:** `4bf1cb6 feat(web): foundation design system dan shared components`

### Phase 2 — App shell dan navigasi

File utama: `client/src/App.vue`.

- [x] Sidebar: contrast, spacing, active state, company switcher, footer user.
- [x] Header: tinggi, glass, tanggal, notifikasi, avatar, mobile trigger.
- [x] Main content: max width, page transition, padding.
- [x] Footer.
- [x] Bottom nav mobile: floating, active state, badge, safe area.
- [x] Drawer menu mobile: grouping, touch target, current page.
- [x] Skip link dan focus order.

**Acceptance:**

- sidebar nyaman dibaca;
- nav aktif terlihat jelas;
- mobile bottom nav tidak menutupi konten;
- semua icon-only button punya label;
- keyboard traversal masuk akal.

**Status:** selesai dan di-commit — `type-check`, `lint`, dan `build` lulus (CSS 69.74 kB). Visual QA desktop 1440px dan mobile 390px: tidak ada horizontal overflow, bottom nav 5 item terlihat, sheet menu dapat di-scroll sampai tombol aksi. Tombol mengambang Vue DevTools hanya muncul di dev dan tidak masuk build produksi.

**Commit:** `06a3187 feat(ui): premium app shell and navigation`

### Phase 3 — Auth dan pemilihan perusahaan

File:

- `client/src/components/AuthLayout.vue`
- `client/src/views/LoginView.vue`
- `client/src/views/RegisterView.vue`
- `client/src/views/ForgotPasswordView.vue`
- `client/src/views/ResetPasswordView.vue`
- `client/src/views/CompaniesView.vue`

- [x] Auth layout: brand panel, form hierarchy, mobile version.
- [x] Form fields memakai `AppInput`/`AppField`.
- [x] Password visibility dan error state.
- [x] Demo credential dipindahkan ke environment demo atau dihapus.
- [x] Companies: kartu perusahaan, status, cabang, tagihan, modal shared.
- [x] Empty/loading state.

**Status:** selesai - `type-check`, `lint`, dan `build` lulus (CSS 73.03 kB). Visual QA desktop 1440px dan mobile 390px untuk login, register, forgot/reset token kosong, companies, dan modal: tidak ada horizontal overflow. Demo credential aktif otomatis di development atau lewat `VITE_SHOW_DEMO_CREDENTIALS=true`; build produksi default tidak memuat kredensial demo.

**Commit:** `d5b62cd feat(ui): auth and company selection polish`

### Phase 4 — Dashboard

File: `client/src/views/DashboardView.vue`.

- [x] Hero perusahaan + status + periode.
- [x] Quick actions sesuai permission.
- [x] Finansial memakai `AppStat`.
- [x] Penjualan memakai `AppStat` + sparkline.
- [x] Perlu perhatian.
- [x] Kehadiran, karyawan, payroll.
- [x] Loading skeleton.
- [x] Format tanggal dan status.

**Status:** selesai — `type-check`, `lint`, dan `build` lulus (CSS 78.15 kB). Visual QA dengan backend demo: desktop 1440px, tablet 768px/1024px, dan mobile 390px/375px tanpa horizontal overflow. Role owner menampilkan seluruh seksi; role kasir hanya menerima penjualan dan aksi yang diizinkan. Tidak ada console error atau request gagal.

**Commit:** `feat(ui): dashboard hierarchy and data cards`

### Phase 5 — Operasional

File:

- `PosView`
- `SalesView`
- `ServicesView`
- `PurchaseView`
- `CatalogView`
- `PricesView`
- `CustomersView`
- `StockView`
- `TransfersView`
- `OpnamesView`
- `ApprovalsView`

- [x] Page header seragam.
- [x] Filter bar dan action bar.
- [x] Tabel/list loading, empty, mobile overflow.
- [x] Baris clickable dapat diakses keyboard.
- [x] POS: label pembayaran, layout mobile, touch target.
- [x] Detail/action modal memakai `AppModal`.
- [x] Status dan format angka konsisten.

**Status:** selesai — `type-check`, `lint`, dan `build` lulus (CSS 78.47 kB). Visual QA dengan backend demo: desktop 1440px untuk 11 halaman operasional, tablet 768px dan mobile 390px untuk POS/Approvals/Sales tanpa horizontal overflow. Detail modal diuji pada Approvals, Sales, Services, Purchase, Transfers, dan Opnames. POS diuji tambah item, split pembayaran, dan label metode; tidak ada console error atau request gagal.

**Commit:** `feat(ui): operational page polish`

### Phase 6 — Keuangan

File:

- `ExpensesView`
- `DebtsView`
- `ReceivablesView`
- `JournalView`
- `ReportsView`
- `PeriodsView`

- [x] Ganti modal custom dengan `AppModal`.
- [x] Stat memakai `AppStat`.
- [x] Filter dan field memakai komponen shared.
- [x] Tabel jurnal dan laporan.
- [x] Format bucket/status.
- [x] Empty/loading state.

**Status:** selesai — `type-check`, `lint`, dan `build` lulus (CSS 78.97 kB). Visual QA dengan backend demo: desktop 1440px untuk 6 halaman keuangan dan mobile 390px untuk layout utama. Modal Expenses/Receivables/Periods, pembayaran inline Debts, tab Neraca saldo Journal, dan tab Harian Reports diuji. Aging memakai nominal ringkas + nominal lengkap pada total; tidak ada horizontal overflow, console error, atau request gagal. Sekaligus diperbaiki query `unpaid=true` menjadi `unpaid=1` pada Debts/Receivables agar sesuai validasi boolean Laravel.

**Commit:** `feat(ui): finance page polish`

### Phase 7 — SDM

File:

- `AttendanceView`
- `HrAttendanceView`
- `LeavesView`
- `ScheduleView`
- `PayrollView`
- `SlipView`
- `EmployeesView`
- `CardView`

- [x] Tabel/filter shared.
- [x] Ringkasan kehadiran.
- [x] Schedule grid dan navigasi.
- [x] Payroll summary dan aksi.
- [x] Employee card/avatar dengan foto profil dan fallback ikon.
- [x] Format tanggal dan status.

**Status:** selesai dan sudah di-commit (`7f16605`) - `type-check`, `lint`, dan `build` lulus (CSS 77.38 kB). Visual QA dengan backend demo: desktop 1440px untuk 8 halaman, mobile 390px untuk layout utama, Card depan/belakang, tab cuti, tab jadwal/shift/pola/libur, serta tab payroll. Foto profil tampil di kartu, profil, header, sidebar, dan daftar karyawan; akun tanpa foto memakai ikon profil default, bukan inisial. Horizontal overflow mobile `LeavesView` sudah diperbaiki lewat `min-w-0` pada kolom grid. Tidak ada console error atau request gagal.

**Commit:** `feat(ui): HR page polish`

### Phase 8 — Admin dan pengaturan

File:

- `NotificationsView`
- `SubscriptionView`
- `CompanyProfileView`
- `ProfileView`
- `BranchesView`
- `RolesView`
- `TenantsView`
- `PlansView`
- `AuditView`
- `ImportView`
- `NationalHolidaysView`

- [x] Page header.
- [x] Modal shared.
- [x] Form dan tabel.
- [x] Status langganan.
- [x] Empty/loading state.
- [x] Akses keyboard.

**Status:** selesai dan sudah di-commit - `type-check`, `lint`, dan `build` lulus (CSS 75.85 kB). QA CDP dengan backend demo mencakup 11 halaman pada desktop 1440px dan mobile 390px. Tidak ditemukan horizontal overflow dokumen, kontrol tanpa label, tombol tanpa focus ring, console error, atau request gagal. Dialog Buat cabang, Buat role, Buat paket, dan logout diuji di desktop/mobile; fokus masuk ke panel dan panel tidak overflow. Detail tenant dan detail audit juga diperiksa. QA menemukan `GET /api/v1/holidays` belum terdaftar sehingga halaman Libur Nasional mendapat 405 meski endpoint sudah dirujuk UI dan dokumen; ditambahkan route baca super-admin dan test `ScheduleTest` (2 test, 27 assertion). Tidak ada perubahan kontrak POST/DELETE. Setelah perbaikan, seluruh halaman lulus tanpa error.

**Commit:** `feat(ui): admin settings and accessibility polish`

**Commit API:** `fix(api): add national holiday list endpoint`

### Phase 9 — QA dan hardening

- [x] Cek 375px, 390px, 768px, 1024px, 1440px, 1600px.
- [x] Cek keyboard: tab order, focus, Escape, modal trap.
- [x] Cek kontras teks kecil dan badge.
- [x] Cek `prefers-reduced-motion`.
- [x] Cek tabel overflow dan bottom nav overlap.
- [x] Smoke test per role demo.
- [x] `npm run type-check`.
- [x] `npm run lint`.
- [x] `npm run build`.
- [x] Review diff; pastikan file Android tidak ikut.

**Status:** selesai dan siap review - matrix responsif 35 route owner, 3 route super-admin, dan 4 route auth pada 6 viewport lulus tanpa overflow dokumen/tabel, bottom nav overlap, kontrol tanpa nama, field tanpa label, console error, atau request gagal. Tiga overflow grid diperbaiki di `AttendanceView`, `AuditView`, dan `ScheduleView`. Audit kontras 43 route (owner, super-admin, auth, dan kartu karyawan) lulus; label muted di shell, dashboard, auth, dan kartu dinaikkan ke opasitas aman. 23 pemeriksaan keyboard lulus untuk skip link, tab order, focus ring, dropdown, focus trap, Escape, pemulihan fokus, scroll lock, dan reduced-motion. Smoke test 9 akun demo lulus login UI dan 3-6 route utama per role tanpa console error atau HTTP >=400. `AppDropdown` kini mengembalikan fokus ke trigger sebelum membuka aksi seperti konfirmasi logout. `type-check`, `lint`, dan `build` lulus (177 modul, CSS 75.54 kB). Diff hanya menyentuh 8 file `client/`; file Android, `postman/`, `android/.kotlin/`, dan `PermissionCatalog.php` tidak di-stage.

**Commit:** `fix(ui): responsive and accessibility hardening`

## 5. Page Inventory

| Grup | Halaman | Status |
|---|---|---|
| Foundation | `main.css`, shared components | selesai |
| Auth | Login, Register, Forgot, Reset | selesai |
| Gateway | Companies | selesai |
| Dashboard | Dashboard | selesai |
| Operasional | POS, Sales, Services, Purchase, Catalog, Prices, Customers, Stock, Transfers, Opnames, Approvals | selesai |
| Keuangan | Expenses, Debts, Receivables, Journal, Reports, Periods | selesai |
| SDM | Attendance, HrAttendance, Leaves, Schedule, Payroll, Slip, Employees, Card | selesai |
| Admin | Notifications, Subscription, CompanyProfile, Profile, Branches, Roles, Tenants, Plans, Audit, Import, NationalHolidays | selesai |
| Shell | App.vue, AuthLayout, CompanyShell | selesai |

Status yang dipakai: `belum`, `sedang`, `selesai`, `blocked`.

## 6. Verifikasi

Per fase:

```bash
cd client
npm run type-check
npm run lint
npm run build
```

Manual:

- desktop 1440px;
- tablet 768px;
- mobile 375px;
- keyboard only;
- satu akun owner dan satu akun role terbatas;
- halaman dengan data kosong;
- halaman dengan data panjang.

## 7. Commit dan Rollback

- Satu fase satu commit.
- Jangan gabungkan polish dengan perubahan backend.
- Jangan commit file Android.
- Jika satu fase bermasalah, revert commit fase tersebut, bukan seluruh branch.
- Jangan push sebelum seluruh rangkaian selesai dan disetujui.

## 8. Definition of Done

- Semua halaman memakai `AppPageHeader` atau pola yang setara.
- Tidak ada modal custom.
- Semua form punya label dan error state.
- Semua interactive element punya focus state dan accessible name.
- Semua tabel punya loading, empty, dan overflow mobile.
- Tidak ada hardcoded status color baru.
- Tidak ada teks “Memuat…” untuk loading utama.
- Kontras teks minimal 4.5:1.
- Mobile bottom nav tidak menutupi konten.
- Type-check, lint, dan build bersih.
- File Android dan file tidak terkait tidak ikut di-stage.
