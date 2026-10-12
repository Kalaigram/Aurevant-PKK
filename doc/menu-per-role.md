# Menu per Role — Aurevant PKK

Sumber: `PermissionCatalog`, `RoleTemplates`, middleware di `server/routes/api.php`,
dan gating menu di `client/src/App.vue` + `ApprovalController`/`DashboardController`.

## 0. Hirarki role

| Level | Cakupan | Cara dapat |
|---|---|---|
| **Super Admin** | Global, lintas perusahaan. Khusus halaman platform (§1). BUKAN pemilik data: tanpa role di perusahaan, ia ditolak middleware `permission:...` modul operasional. | Flag `users.is_super_admin` |
| **Pemilik Perusahaan** | Satu perusahaan, semua izin (`*`) + semua cabang. | Template `Pemilik` |
| **Karyawan + sub-role** | Satu perusahaan, izin sesuai template, bisa dibatasi cabang via penempatan (`branch_id`; tanpa penempatan = semua cabang). Pemilik boleh ubah isi template per perusahaan. | Template + `EmployeeAssignment` |

Aturan menu frontend: item tanpa izin disembunyikan (bukan menunggu 403).
Halaman Batch B–E yang belum dibangun tampil nonaktif berlabel "Segera".

Legenda: ✅ penuh · 🟡 terbatas (lihat/ajukan saja) · ❌ disembunyikan.

## 1. Super Admin — halaman platform

| Menu | Akses | Syarat |
|---|---|---|
| Paket (`/plans`) | ✅ CRUD | `super.admin` |
| Langganan perusahaan (ubah) | ✅ | `super.admin` |
| Invoice langganan (terbit + bayar) | ✅ | `super.admin` |
| Libur nasional | ✅ | `super.admin` |
| Semua halaman perusahaan lain | ❌ kecuali diberi role di perusahaan itu | middleware `permission:...` |

## 2. Pemilik Perusahaan — semua menu

Semua grup: Utama, Operasional & Penjualan, SDM, Keuangan, Sistem.
Satu-satunya yang tertutup: 4 halaman platform §1 (butuh flag super admin).

## 3. Manajer Cabang

Izin: `transaksi.buat/diskon/void`, `retur.buat/setujui`, `nota.lihat`,
`shift.buka/tutup`, `stok.transfer`, `stok.opname`, `po.buat`, `biaya.input`,
`jadwal.lihat/atur`, `absensi.koreksi/approve`, `pelanggan.lihat/kelola`,
`dashboard.konsolidasi`.

| Menu | Akses | Catatan |
|---|---|---|
| Dashboard | ✅ (tanpa seksi keuangan) | tanpa `keuangan.lihat` |
| Persetujuan | ✅ ajukan + putuskan void/diskon/retur/transfer/opname | tanpa `po.setujui` & `biaya.setujui` |
| Kasir / POS, Nota, Shift | ✅ | void & retur langsung bisa |
| Transfer, Opname | ✅ penuh | satu izin mencakup ajukan + putuskan |
| Pembelian | 🟡 buat + ajukan saja | setujui/esan/terima/batal ❌ |
| Biaya | 🟡 catat saja | setujui ❌ |
| Hutang / Piutang / Jurnal / Laporan / Periode | ❌ | tanpa `keuangan.lihat` & `biaya.setujui` |
| Katalog / Harga / Cabang | ❌ | tanpa `katalog.kelola`, `harga.atur`, `cabang.kelola` |
| Jadwal, Absensi, Cuti (putuskan), Pelanggan | ✅ | |
| Penggajian | ❌ | tanpa `gaji.kelola` |
| Karyawan, Role, Audit, Impor | ❌ | tanpa `karyawan.kelola`, `pengaturan.kelola`, `audit.lihat`, `impor.jalankan` |
| Absensi & Slip & Kartu pribadi | ✅ | halaman global |

## 4. Kasir

Izin: `transaksi.buat`, `nota.lihat`, `shift.buka/tutup`, `pelanggan.lihat`.

| Menu | Akses | Catatan |
|---|---|---|
| Dashboard | ✅ (seksi penjualan bila `nota.lihat`) | |
| Persetujuan | 🟡 ajukan void + diskon saja | refund ❌ (tanpa `retur.buat`); putuskan ❌ semua |
| Kasir / POS, Nota, Shift | ✅ | void/retur langsung ❌ |
| Pelanggan | 🟡 lihat saja | kelola ❌ |
| Order Jasa | 🟡 buat/DP/bayar/batal; proses/selesai ❌ | tanpa `jasa.proses` |
| Stok, Transfer, Opname, Pembelian, Keuangan, SDM kelola | ❌ | |
| Absensi & Cuti & Slip & Kartu pribadi | ✅ | |

## 5. Staf Gudang

Izin: `stok.transfer`, `stok.opname`, `po.buat`, `nota.lihat`.

| Menu | Akses | Catatan |
|---|---|---|
| Stok (saldo, riwayat, mutasi manual) | ✅ | |
| Transfer, Opname | ✅ penuh | |
| Pembelian | 🟡 buat + ajukan; terima barang ❌ | terima butuh `po.setujui` |
| Nota | 🟡 lihat saja | |
| Persetujuan | ✅ ajukan + putuskan transfer/opname/PO-diajukan | void/diskon/refund/biaya ❌ |
| Kasir / POS | ❌ | tanpa `transaksi.buat` |
| Keuangan, Penggajian, SDM kelola | ❌ | |
| Absensi & Cuti & Slip & Kartu pribadi | ✅ | |

## 6. Pelaksana Jasa (teknisi)

Izin: `nota.lihat`, `jadwal.lihat`, `jasa.proses`, `gaji.lihat_sendiri`.

| Menu | Akses | Catatan |
|---|---|---|
| Order Jasa | 🟡 proses + selesai saja | buat/DP/bayar/batal ❌ (tanpa `transaksi.buat`) |
| Jadwal saya | ✅ | |
| Nota | 🟡 lihat saja | |
| Persetujuan | ❌ disembunyikan | tanpa izin ajukan/putuskan apa pun |
| Kasir, Stok, Keuangan, SDM kelola | ❌ | |
| Absensi & Cuti & Slip & Kartu pribadi | ✅ | |

## 7. Admin Keuangan

Izin: `keuangan.lihat`, `biaya.input/setujui`, `periode.tutup`,
`laporan.konsolidasi`, `gaji.kelola`, `nota.lihat`, `dashboard.konsolidasi`.

| Menu | Akses | Catatan |
|---|---|---|
| Dashboard | ✅ seksi keuangan | |
| Biaya | ✅ penuh (kategori + catat + putuskan) | |
| Hutang, Piutang | ✅ penuh | |
| Jurnal, Laporan (semua + konsolidasi), Tutup Periode | ✅ | |
| Penggajian | 🟡 kelola + hitung + review + bayar; setujui periode ❌ | tanpa `gaji.setujui` |
| Nota | 🟡 lihat saja | |
| Persetujuan | 🟡 pantau (tanpa hak putuskan modul operasional) | |
| Kasir, Stok, PO, SDM kelola, Katalog | ❌ | |
| Absensi & Cuti & Slip & Kartu pribadi | ✅ | |

## 8. HR

Izin: `karyawan.kelola`, `jadwal.lihat/atur`, `shift.kelola`,
`absensi.koreksi/approve`, `gaji.kelola/setujui`.

| Menu | Akses | Catatan |
|---|---|---|
| Karyawan (role + penempatan) | ✅ | |
| Jadwal & Shift (master + pola + override) | ✅ | libur perusahaan ❌ (tanpa `pengaturan.kelola`) |
| Kehadiran (lihat, koreksi, putuskan cuti, reset device) | ✅ | |
| Penggajian | ✅ penuh (termasuk setujui periode) | |
| Kartu karyawan siapa pun | ✅ | |
| Kasir, Stok, PO, Keuangan, Katalog | ❌ | |
| Persetujuan | 🟡 pantau | putuskan modul operasional ❌ |
| Absensi & Cuti & Slip pribadi | ✅ | |

## 9. Batasan cabang (semua karyawan)

- Menu cabang-spesifik (POS, stok, kehadiran) otomatis terfilter ke cabang
  penempatan via `scope.cabang`. Tanpa baris penempatan = semua cabang.
- Ganti cabang tidak ada di UI selain lewat penempatan (diatur pemilik/HR).
