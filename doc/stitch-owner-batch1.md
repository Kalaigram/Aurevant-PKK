# Prompt Stitch — Pemilik Batch 1 (4 halaman admin)

> Alur: salin SATU prompt → generate di Stitch → kirim hasilnya ke sini
> → saya bangun versi Vue + sambung API + verifikasi.
> Semua prompt memakai design system project "Aurevant PKK Operations Platform".

## Token desain (tempel di tiap prompt)

```
Fonts: Plus Jakarta Sans (judul, 600/700), Inter (isi 400/500/600).
Material Symbols Outlined untuk semua ikon (jangan emoji).
Warna: primary #005d42 (sidebar/panel), primary-container #047857 (tombol utama,
teks putih), surface #f9f9ff (background), surface-container-lowest #ffffff (kartu),
surface-low #f1f3ff, on-surface #141b2b, secondary #545f73, outline #bdc9c1,
error #ba1a1a, error-container #ffdad6, warning amber #D97706, success #16A34A.
Radius: rounded-lg/rounded-xl. Kartu: bg putih + shadow-sm.
Pola: header ikon-tile (kotak 48px bg primary-container + ikon putih) + judul;
KPI/statistik kecil; tabel di desktop; bahasa Indonesia natural (hindari istilah
teknis di teks user). JANGAN pakai gradient. JANGAN tampilkan kode izin mentah
seperti "cabang.kelola" di UI. Data uang format Rp id-ID, tanggal Y-m-d.
```

## O1. Halaman Cabang

```
[Token desain di atas]

Halaman "Cabang" untuk pemilik perusahaan (Vue SPA nanti; sekarang HTML statis
rapi sebagai acuan visual). Daftar kartu cabang: tiap kartu ada tile inisial,
nama, kode (mono), badge tipe (pusat/cabang/gudang), badge status
(aktif/nonaktif), zona waktu (Asia/Jakarta=WIB, Asia/Makassar=WITA,
Asia/Jayapura=WIT), alamat + telepon bila ada, tombol "Pilih" dan menu
Ubah/Hapus. Tombol "Tambah cabang" membuka modal form: code (wajib, max 20,
unik), name (wajib), type (dropdown: pusat/cabang/gudang), timezone (dropdown
3 zona), address + phone (opsional), status (aktif/nonaktif). Hapus pakai
konfirmasi. Sertakan state kosong yang ramah. Field persis ini (jangan tambah
field lain): code, name, type, timezone, address, phone, status.
```

## O2. Halaman Role & Akses

```
[Token desain di atas]

Halaman "Role & Akses" untuk pemilik. Tiga area:
1. Katalog izin read-only berupa matriks checkbox per grup (Transaksi, Stok, PO,
   Keuangan, Jadwal, Absensi, Gaji, Katalog, Karyawan, Sistem) — tampilkan 30 izin
   ini persis (jangan tambah/kurangi): transaksi.buat, transaksi.diskon,
   transaksi.void, jasa.proses, retur.buat, retur.setujui, nota.lihat, shift.buka,
   shift.tutup, stok.transfer, stok.opname, po.buat, po.setujui, keuangan.lihat,
   biaya.input, biaya.setujui, periode.tutup, laporan.konsolidasi, jadwal.lihat,
   jadwal.atur, shift.kelola, absensi.koreksi, absensi.approve, gaji.lihat_sendiri,
   gaji.kelola, gaji.setujui, katalog.kelola, harga.atur, cabang.kelola,
   karyawan.kelola, audit.lihat, dashboard.konsolidasi, pelanggan.lihat,
   pelanggan.kelola, pelanggan.ekspor, impor.jalankan, pengaturan.kelola,
   langganan.kelola.
2. Daftar role + template awal (Pemilik, Manajer Cabang, Kasir, Staf Gudang,
   Pelaksana Jasa, Admin Keuangan, HR): buat role baru (nama + pilih template +
   centang izin), ubah isi izin role, hapus role dengan konfirmasi.
3. Catatan kecil: template hanya isi awal; yang berlaku izin milik role itu.
```

## O3. Halaman Karyawan & Penempatan

```
[Token desain di atas]

Halaman "Karyawan & Penempatan" dua tab:
1. Tab Penempatan: tabel riwayat (nama karyawan, cabang — kosong berarti akses
   semua cabang dengan badge "Akses penuh", role, mulai, selesai). Form
   "Tempatkan": pilih karyawan (dropdown), cabang (dropdown + opsi "Semua cabang"),
   role (dropdown, opsional), tanggal mulai (wajib). Tombol "Akhiri" per baris
   aktif membuka dialog tanggal selesai. Field persis: branch_id (nullable),
   role_id (nullable), started_at (wajib), ended_at (wajib saat akhiri).
2. Tab Role user: pilih karyawan → beri role (dropdown role) / cabut role
   per baris. TIDAK ada endpoint daftar user — sediakan input ID + nama manual
   dengan catatan "pilih dari daftar penempatan".
JANGAN buat: edit profil user, buat user baru, upload foto.
```

## O4. Halaman Audit Log

```
[Token desain di atas]

Halaman forensik "Audit Log" read-only: filter aksi (bebas ketik + saran:
price.set, sale.void, po.approve, approval.setujui, periode.tutup, biaya.setujui,
langganan.bayar) dan filter tipe entitas; tabel hasil: waktu, pelaku (ID user),
aksi, entitas (tipe + ID), cabang; klik baris membuka panel before → after
sebagai diff JSON rapi. Sertakan paginasi (prev/next + total). TIDAK ada tombol
tambah/ubah/hapus — log bersifat append-only. Contoh isi: aksi sale.void dengan
before {status: lunas} → after {status: void}.
```
