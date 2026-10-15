# Prompt Stitch — Batch C (Manajemen, 9 halaman)

> Berlaku penuh "Aturan global untuk SEMUA prompt" dari `doc/stitch-batch-a.md`
> (tempel di awal tiap prompt Batch C juga). Base `/api/v1`, kontrak
> `{status,message,data,errors}` + `meta`, 401 → login, 422 → errors server,
> tanggal Y-m-d, jam H:i, Rp id-ID. DILARANG mengarang endpoint/field/enum.

---

## C1. Cabang

```
CRUD cabang/gudang (KHUSUS izin cabang.kelola untuk tulis; lihat bebas).
GET .../branches → [{id, code, name, type, timezone, status}].
Tambah: POST .../branches {code (wajib max 20, unik per perusahaan),
name (wajib max 255), type (opsional, HANYA pusat/cabang/gudang — default
cabang bila tak diisi? JANGAN asumsikan default, tampilkan dropdown dengan
ketiga opsi), timezone (opsional, HANYA Asia/Jakarta/Asia/Makassar/Asia/Jayapura),
status (opsional, HANYA aktif/nonaktif)}.
PUT/DELETE .../branches/{id}. Satu kartu cabang menampilkan kode + zona waktu
(WIB/WITA/WIT dari timezone-nya) + tombol "Pilih" untuk ganti cabang aktif
(state client, bukan API). Hapus pakai konfirmasi + tampilkan error server
(bila cabang masih dipakai, JANGAN karang aturan).
```

## C2. Harga per Cabang (bagian dari Katalog)

```
Layar matriks harga: baris = item (GET .../items?type&category_id&per_page),
kolom = cabang (GET .../branches). Sel = harga jual cabang itu (dari data item
per cabang bila tersedia di respons; JANGAN karang endpoint harga massal —
bila respons items tak membawa harga cabang, tampilkan tombol "Atur" per sel).
Ubah: PUT .../items/{itemId}/prices/{branchId} {price:number ≥0} (KHUSUS izin
harga.atur; tanpa izin sembunyikan tombol). Sukses → refresh sel + audit
tercatat otomatis (price.set). Tampilkan riwayat? TIDAK ada endpoint riwayat
harga — jangan buat tab riwayat.
```

## C3. Role & Akses

```
Manajemen role (KHUSUS izin pengaturan.kelola untuk tulis).
- Referensi: GET .../access/permissions → daftar string izin; GET
  .../access/templates → peta nama template → daftar izin. Render sebagai
  checkbox matrix read-only untuk perbandingan (JANGAN karang izin baru —
  hanya dari respons; izin yang ada: transaksi.buat/diskon/void, jasa.proses,
  retur.buat/setujui, nota.lihat, shift.buka/tutup, stok.transfer/opname,
  po.buat/setujui, keuangan.lihat, biaya.input/setujui, periode.tutup,
  laporan.konsolidasi, jadwal.lihat/atur, shift.kelola, absensi.koreksi/approve,
  gaji.lihat_sendiri/kelola/setujui, katalog.kelola, harga.atur, cabang.kelola,
  karyawan.kelola, audit.lihat, dashboard.konsolidasi, pelanggan.lihat/kelola/
  ekspor, impor.jalankan, pengaturan.kelola, langganan.kelola).
- Role: GET .../access/roles (+ detail .../{roleId} berisi permissions).
  Buat: POST .../access/roles {name (wajib, unik per perusahaan), template?
  (HANYA nama dari /templates — Pemilik/Manajer Cabang/Kasir/Staf Gudang/
  Pelaksana Jasa/Admin Keuangan/HR), permissions? (array string, opsional)}.
  Ubah: PUT .../access/roles/{id} {name, permissions?}. Hapus: DELETE (konfirmasi).
- Catatan: template HANYA untuk isi awal; setelah dibuat yang berlaku adalah
  permissions milik role itu.
```

## C4. Karyawan & Penempatan

```
Dua tab. Izin karyawan.kelola untuk tulis.
- PENEMPATAN: GET .../access/assignments?user_id&active&per_page → riwayat
  {user, branch (bisa null = semua cabang), role, started_at, ended_at}.
  Tempatkan: POST .../access/users/{userId}/assignments {branch_id? (nullable —
  kosong = akses semua cabang), role_id? (nullable), started_at (Y-m-d, wajib)}.
  Akhiri: POST .../access/assignments/{id}/close {ended_at (Y-m-d, wajib)}.
  User tanpa baris penempatan = akses penuh (aturan server) — tampilkan badge
  "Akses penuh" pada karyawan tanpa penempatan aktif, JANGAN blokir.
- ROLE USER: POST .../access/users/{userId}/roles {role_id? — lihat respons/
  error 422 untuk kunci pastinya, JANGAN karang; bila ragu tampilkan dropdown
  role + kirim role_id} dan DELETE .../access/users/{userId}/roles/{roleId}.
- Daftar karyawan: TIDAK ada endpoint list user — gunakan riwayat penempatan
  + hasil impor sebagai sumber; JANGAN karang GET /users. Untuk memilih user
  (assign role/penempatan/cuti/slip), sediakan input ID + nama manual sementara
  dengan catatan "pilih dari daftar penempatan".
```

## C5. Jadwal & Shift

```
Tiga area. Izin: lihat jadwal.lihat; ubah shift.kelola; libur perusahaan
pengaturan.kelola.
- MASTER SHIFT: GET .../work-shifts → {id, name, starts_at, ends_at (format
  H:i — input type=time), late_tolerance_minutes (0–600)}. CRUD:
  POST .../work-shifts, PUT .../work-shifts/{id}, DELETE .../{id} (shift.kelola).
- POLA MINGGUAN per karyawan: GET .../users/{userId}/schedule-pattern →
  array 7 slot (Senin–Minggu) berisi work_shift_id atau null (libur).
  Simpan: PUT .../users/{userId}/schedule-pattern {patterns: array TEPAT 7,
  tiap elemen integer id shift atau null} (shift.kelola). Render sebagai
  grid 7 kolom dengan dropdown shift per hari.
- OVERRIDE/ROSTER: POST .../users/{userId}/schedule-overrides {date (Y-m-d,
  wajib), work_shift_id? (nullable — null = liburkan hari itu), note? max 500}
  (shift.kelola); hapus: DELETE .../schedule-overrides {date} (body, bukan URL).
- LIHAT JADWAL: GET .../users/{userId}/schedule?from&to (jadwal.lihat) →
  kalender hasil resolusi (libur > roster > pola). Tampilkan asal tiap hari
  (pola/roster/libur) bila ada di respons; JANGAN karang.
- LIBUR: GET .../holidays; tambah/hapus .../holidays(+/{id}) {date, name}
  (pengaturan.kelola; tanpa izin sembunyikan tombol).
```

## C6. Kehadiran & Cuti (kelola HR)

```
Monitoring HR (izin absensi.approve + absensi.koreksi):
- Harian cabang: GET .../branches/{branchId}/attendance?date (default hari ini)
  &per_page → status per orang (hadir/terlambat/alpa/izin/sakit/cuti) +
  clockInAt/clockOutAt/lateMinutes. Filter tanggal + badge warna.
- Koreksi: PUT .../attendances/{attendanceId} {status (HANYA enam status di
  atas), note?} (absensi.koreksi) — wajib isi alasan (note) di UI walau opsional
  di API, demi audit.
- Cuti: GET .../leaves?user_id&status (diajukan/disetujui/ditolak) + tombol
  Putuskan: POST .../leaves/{leaveId}/decide {approve:boolean} (absensi.approve).
- Perangkat: 1 akun 1 perangkat — bila karyawan ganti HP: DELETE
  .../users/{userId}/device (absensi.approve) + konfirmasi.
Tanpa kedua izin: halaman ini tidak tampil sama sekali (403).
```

## C7. Penggajian

```
Alur periode: draft → review → disetujui → dibayar (dikunci). Izin gaji.kelola
semua langkah KECUALI approve (gaji.setujui).
- KOMPONEN: GET .../salary-components → {id, name, calc_type, deductible,
  taxable, bpjs_base, active}. Tambah/ubah: name wajib; calc_type HANYA
  monthly_fixed/daily_rate/commission/manual; deductible/taxable/bpjs_base/active
  boolean. Arti flag (tampilkan sebagai hint, JANGAN hitung di client):
  monthly_fixed = gaji bulanan penuh (potong alpa 1/divisor per hari),
  daily_rate = × hari hadir, commission = komisi jasa periode itu,
  manual = input nominal, taxable = kena PPh annualisasi, bpjs_base = kena
  BPJS 4% (1% Kes + 2% JHT + 1% JP). POST/PUT .../salary-components(+/{id}),
  hapus? TIDAK ada endpoint hapus — jangan buat tombol hapus.
- ASSIGN: POST .../users/{userId}/salary-components {component_id (wajib),
  amount (wajib ≥0)}.
- PERIODE: GET .../payroll-periods; buat POST .../payroll-periods {name wajib,
  date_from + date_to (Y-m-d, to ≥ from), divisor? (1–31, default ikut server)}.
  Detail: GET .../payroll-periods/{id} (ringkasan per karyawan).
- HITUNG: POST .../{id}/calculate (gaji.kelola) → hasil {period, employees:
  [{user_id, net}]}. Penyesuaian manual: POST .../{id}/users/{userId}/adjustments
  {label wajib max 255, amount (bisa negatif untuk potongan!)}.
- KUNCI: [Kirim Review] (gaji.kelola) → [Setujui] (gaji.setujui, sembunyikan
  tanpa izin) → [Bayar] (gaji.kelola, menjurnal beban gaji). Periode dibayar
  terkunci (calculate ditolak 422 — tampilkan pesan server).
- Slip per orang: link ke halaman Slip (Batch A) — JANGAN duplikasi.
```

## C8. Audit Log

```
Forensik aktivitas (KHUSUS audit.lihat; tanpa izin halaman tidak tampil).
GET .../audit-logs?action&entity_type&per_page → [{id, companyId, branchId,
userId, action, entityType, entityId, before, after, ip, createdAt? — render
kunci waktu apa adanya dari respons, JANGAN karang nama field waktu}].
Filter aksi ( Bebas ketik + saran dari aksi yang pernah terlihat, mis.
price.set, sale.void, po.approve, approval.setujui, periode.tutup, biaya.setujui,
langganan.bayar — JANGAN hardcode daftar lengkap, backend tidak menyediakan
endpoint daftar aksi). Tampilkan before→after sebagai diff JSON rapi.
Contoh pola dari test: filter sale.void mengembalikan meta.total + before.status
"lunas" → after.status "void".
```

## C9. Impor / Ekspor (KHUSUS impor.jalankan)

```
- Template: GET .../imports/template/{type} (type HANYA items/stock/suppliers/
  employees; selain itu 404) → unduh CSV. Header kolom:
  items: code,name,type,unit,category,branch_code,price;
  stock: item_code,branch_code,qty,unit_cost;
  suppliers: name,phone,address;
  employees: name,email,phone,branch_code,role,password,ptkp_status.
  Tampilkan header ini sebagai panduan kolom di UI (JANGAN karang kolom lain).
  ptkp_status HANYA: TK/0,TK/1,TK/2,TK/3,K/0,K/1,K/2,K/3 (kosong = TK/0).
- Impor: multipart POST .../imports/{type} fields: file (CSV ≤2MB, wajib),
  dry_run (boolean, opsional). SELALU tawarkan mode "Pratinjau dulu (dry_run)"
  sebelum eksekusi. Respons sukses: {imported:count, dry_run:bool} + message
  ("Pratinjau valid." vs "Impor selesai."); Gagal: error "File tidak bisa
  dibaca sebagai CSV." / "File kosong atau tanpa baris data." + rincian error
  per baris (tampilkan nomor baris + pesan). Password kosong saat impor karyawan
  = acak oleh server (tampilkan hint ini).
- Ekspor item: GET .../exports/items → unduh CSV.
- Ekspor pelanggan: GET .../exports/customers → unduh CSV (kolom: name, phone,
  address). KHUSUS izin pelanggan.ekspor (tanpa izin sembunyikan tombol).
```
