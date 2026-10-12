# Prompt Stitch — Batch A (Global, 11 halaman)

> Cara pakai: salin SATU prompt utuh ke Stitch, generate, bawa hasilnya ke `client/`.
> Tandai `[~]` di `doc/halaman.md` §13 per prompt yang sudah dibuatkan; `[x]` bila layar
> sudah ditempel ke API dan lolos verifikasi.

## Aturan global untuk SEMUA prompt (tempel di awal tiap prompt)

```
Stack: Vue 3.5 + TypeScript + Pinia + Vue Router (tanpa Tailwind, tanpa UI library,
tanpa axios — pakai fetch). Bahasa UI: Indonesia. Mobile-first responsif.

API: base URL dari env VITE_API_URL + "/api/v1". Token Sanctum di header
"Authorization: Bearer <token>" (dari data.token saat login, simpan di Pinia + localStorage).
Semua respons sukses: {status:"success", message, data, errors:null}.
Semua respons gagal: {status:"error", message, data:null, errors:{field:[pesan]}}.
Kode: 401 token hilang/kedaluwarsa → buang token, kembali ke Login.
422 → tampilkan errors per field persis dari server, JANGAN karang pesan sendiri.
403 → banner "Akses ditolak". 404 → state kosong. Daftar: data = array + meta{total,
per_page, current_page, last_page} untuk paginasi (tombol prev/next + per_page).

DILARANG mengarang nama endpoint, nama field, atau enum. Hanya pakai yang tertulis
di prompt ini. Tanggal format Y-m-d. Uang: number, tampil "Rp" + toLocaleString("id-ID").
```

---

## A1. Login / Register

```
Buatkan 2 layar auth modern untuk aplikasi SaaS "Aurevant PKK", gaya bersih ala
aplikasi kasir profesional (biru tua + aksen hijau), kartu tengah, logo teks "Aurevant PKK".

LAYAR LOGIN — form: "login" (email ATAU nomor HP, satu input), "password" + tombol
Masuk. Link ke Register. Saat submit: POST /auth/login body {login, password}.
Sukses (data: {user:{id,name,email,phone}, token}): simpan token + user ke Pinia,
redirect ke Pilih Perusahaan. Gagal 422: tampilkan errors per field dari server.

LAYAR REGISTER — form: "name" (wajib), "email" (wajib, format email),
"phone" (opsional), "password" (min 8), "password_confirmation" (harus sama).
POST /auth/register body {name, email, phone, password, password_confirmation}.
Sukses 201: respons sama seperti login (user + token) → redirect Pilih Perusahaan.
Tampilkan pesan sukses dari message server ("Pendaftaran berhasil.").

Validasi client minimal: required + format email + min 8 + konfirmasi sama;
aturan final ikut server. Tombol loading saat request. JANGAN tambah field
username/otp/nama perusahaan — tidak ada di API.
```

## A2. Pilih Perusahaan

```
Layar "Pilih Perusahaan" setelah login: grid kartu perusahaan milik user.
GET /companies → data: [{id, name, slug, status}] ("aktif"/"ditangguhkan").
Tiap kartu: nama, slug, badge status (hijau aktif, merah ditangguhkan).
Klik kartu: simpan company aktif ke Pinia (id + name), redirect ke Dashboard
dengan route /c/:companyId/dashboard. Bila kosong: empty state + tombol
"Buat Perusahaan" → POST /companies (cari tahu field dari error 422 bila perlu;
jangan karang field — tampilkan form name + slug saja dan biarkan server
memvalidasi). Header ada tombol Keluar: POST /auth/logout lalu buang token.
```

## A3. Profil Saya

```
Layar Profil: kartu data diri dari GET /auth/me → data: {id, name, email, phone}.
Tampilkan keempat field apa adanya (read-only — TIDAK ada endpoint update profil,
jangan buat form edit). Di bawahnya: tombol Keluar (POST /auth/logout),
info versi app, dan daftar izin saya per perusahaan aktif (minta backend TIDAK
menyediakan endpoint izin-per-user — jadi JANGAN tampilkan daftar izin, cukup
tulis catatan "hak akses diatur pemilik perusahaan").
```

## A4. Dashboard

```
Dashboard dinamis: GET /companies/{companyId}/dashboard → data SELALU berisi:
company{id,name,status}, subscription{status, endsAt}, monthStart, today (Y-m-d).
Seksi lain HANYA muncul bila user punya izinnya — render kondisional murni dari
kunci yang ada, JANGAN fetch endpoint lain sebagai fallback:

- keuangan: kartu Kas Bersih Bulan Ini (kas_bersih_bulan_ini), Laba Bersih Bulan Ini
  (laba_bersih_bulan_ini), Total Hutang (hutang_total), Total Piutang (piutang_total).
- penjualan: Omzet Hari Ini (omzet_hari_ini), Transaksi Hari Ini (transaksi_hari_ini),
  Shift Terbuka (shift_terbuka).
- operasional: Transfer Diajukan (transfer_diajukan, bila ada), Opname Diajukan
  (opname_diajukan, bila ada).
- sdm: Hadir (hadir), Terlambat (terlambat), Alpa (alpa) hari ini + Cuti Diajukan
  (cuti_diajukan). Ketuk angka → ke halaman Kehadiran & Cuti.
- penggajian: tampilkan peta status→jumlah apa adanya (kunci dinamis:
  draft/review/disetujui/dibayar).
- karyawan: Total Karyawan Aktif (total).

Header: nama perusahaan + badge status langganan (subscription.status:
trial/aktif/tenggang/read_only/ditangguhkan; merah bila read_only/ditangguhkan
+ teks "tulis diblokir, baca tetap bisa"). Semua angka Rp diformat id-ID.
```

## A5. Notifikasi

```
Halaman notifikasi dengan tab Semua/Belum Dibaca + badge count di menu:
- List: GET /notifications?per_page&company_id&unread (boolean) → item
  {id, company_id, user_id, type, title, body, link, read_at}; belum dibaca bila
  read_at null. Tipe yang mungkin: transfer.diajukan, transfer.disetujui,
  transfer.dikirim, transfer.diterima, po.diajukan, po.disetujui, po.diterima,
  sale.void, sale.retur, opname.disetujui, shift.selisih, approval.diajukan.
  JANGAN buat tipe lain. Klik item → POST /notifications/{id}/read lalu buka
  link bila ada.
- Badge: GET /notifications/unread-count → {unread} (angka).
- Tombol "Tandai semua dibaca": POST /notifications/read-all (query company_id opsional).
- Preferensi: GET /notification-preferences → daftar {type, enabled};
  PUT /notification-preferences body {type, enabled:boolean}. type HANYA dari daftar
  di atas (validasi server menolak yang lain).
- Polling otomatis tiap 30 detik untuk list + badge. Waktu relatif Indonesia
  ("5 mnt lalu") dihitung client dari created_at.
```

## A6. Persetujuan (inbox)

```
Inbox persetujuan tunggal. List: GET /companies/{id}/approvals?status=diajukan
|disetujui|ditolak&action&per_page → item {id, branchId, entityType, entityId,
action, actionLabel, status, payload, requestedBy, decidedBy, decidedAt, note}.
Filter status (default diajukan) + filter aksi. Detail:
GET /companies/{id}/approvals/{approvalId} + tombol Setujui
(POST .../approve body {note?}) dan Tolak (POST .../reject body {note?}).
Bila 403: "hanya pemegang izin putuskan". Aksi yang ada (JANGAN tambah):
- sale.void (label "Void nota"): payload {reason}. Ajukan butuh transaksi.buat,
  putuskan butuh transaksi.void. entityType "sale".
- sale.discount (label "Nota diskon"): payload {branch_id, lines:[{item_id, qty,
  discount_percent?, discount_amount?, tax_percent?}], payments:[{method, amount}],
  customer_id?, idempotency_key?}. Ajukan: transaksi.buat; putuskan:
  transaksi.diskon. method HANYA: tunai/transfer/qris/ewallet/dp.
- sale.refund (label "Retur/refund"): payload {lines:[{sale_item_id, qty,
  condition}], reason, refund_method}. condition HANYA layak/rusak; refund_method
  HANYA tunai/transfer. Ajukan: retur.buat; putuskan: retur.setujui.
- po.approve / transfer.approve / opname.approve: payload kosong, entityId wajib
  (entityType purchase_order/stock_transfer/stock_opname). Izin ajukan+putuskan:
  po.buat/po.setujui, stok.transfer/stok.transfer, stok.opname/stok.opname.
Form ajukan: POST /companies/{id}/approvals body {action, entity_id?, payload?,
note?}. Duplikat aktif → 422 ("Sudah ada pengajuan aktif"). Menyetujui yang
sudah diputuskan → 422. Tampilkan payload sebagai JSON rapi read-only di detail.
```

## A7. Absensi Saya

```
Layar absensi karyawan: tombol Clock-In / Clock-Out + riwayat saya.
- QR cabang: GET /companies/{id}/branches/{branchId}/attendance-qr → tampilkan
  sebagai teks/kode (minta user scan via kamera HP bila tersedia; JANGAN
  mensimulasikan isi token — pakai nilai asli dari respons).
- Clock-In: multipart POST /companies/{id}/attendance/clock-in fields:
  branch_id (integer, required), qr_token (string dari QR, required),
  lat (-90..90, required), lng (-180..180, required), device_id (string ID
  perangkat, required), selfie (file gambar ≤2MB, required). Ambil lat/lng dari
  Geolocation API browser; selfie dari input file/camera. 1 akun 1 perangkat:
  bila 422 menyebut perangkat, arahkan minta reset ke HR.
- Clock-Out: multipart POST .../clock-out fields sama MINUS selfie
  (selfie hanya saat clock-in — jangan kirim field selfie di sini).
- Riwayat: GET .../attendance/me?from&to (Y-m-d)&per_page → status per hari:
  hadir/terlambat/alpa/izin/sakit/cuti + clockInAt/clockOutAt/lateMinutes.
  Tampilkan badge warna + total telat (menit) periode ini.
```

## A8. Cuti Saya

```
Pengajuan izin/sakit/cuti: tombol Ajukan membuka form type (dropdown HANYA:
izin/sakit/cuti — kunci AttendanceStatuses: hadir/terlambat/alpa TIDAK bisa
diajukan, jangan tampilkan), from_date + to_date (Y-m-d, to ≥ from),
reason (wajib, max 500). POST /companies/{id}/users/{userId}/leaves dengan
userId = id sendiri (endpoint mengizinkan user lain HANYA bila pemegang
absensi.approve — untuk layar ini selalu id sendiri).
List: GET /companies/{id}/leaves?user_id=<idku>&status=diajukan|disetujui|ditolak
→ {id, userId, type, fromDate, toDate, reason, status}. Badge status:
kuning diajukan, hijau disetujui, merah ditolak. Tombol putuskan (Setujui/Tolak
via POST .../leaves/{id}/decide {approve:boolean}) HANYA tampil bila user punya
izin absensi.approve (coba tampilkan berdasar role; backend 403 bila nekat).
```

## A9. Slip Gaji Saya

```
Pilih periode → tampilkan slip. ID periode didapat dari dashboard (seksi
penggajian, kunci status dinamis) atau dari riwayat (bila API list periode
per user belum ada, JANGAN karang endpoint — minta input ID periode manual
sementara dengan placeholder "tanya HR").
GET /companies/{id}/payroll-periods/{periodId}/users/{userId}/slip (userId = id
sendiri; aturan server: milik sendiri ATAU pemegang gaji.kelola, selain itu 403)
→ data: {period:{id, name, from, to, divisor, status}, lines:[{userId,
componentId, label, amount(string desimal), isManual}], net:number}.
Render: kop (nama, periode from–to, status), tabel rincian (label + amount;
negatif = potongan, tampil merah), baris khusus "BPJS karyawan
(1% Kes + 2% JHT + 1% JP)" dan "PPh 21 (annualisasi)" bila ada, total NET tebal.
Tombol cetak via window.print() + CSS print. JANGAN tampilkan komponen gaji
orang lain.
```

## A10. Kartu Karyawan

```
Kartu digital: GET /companies/{id}/users/{userId}/card → {name, email, phone,
employeeUuid, company, branch, role, qrContent}. Desain kartu ID modern
(foto placeholder inisial, nama, role @ branch, perusahaan, UUID kecil).
QR: GET .../card/qr → tampilkan apa adanya (SVG/gambar/teks dari server —
render respons asli, JANGAN generate QR sendiri dari qrContent).
Tombol unduh/screenshot kartu (html2canvas opsional — bila tidak ada, tombol
print). UserId default = id sendiri.
```

## A11. Langganan (lihat)

```
Kartu status langganan perusahaan: GET /companies/{id}/subscription →
{planId, planName, status, trialEndsAt, endsAt, allowsWrites}.
Status yang mungkin: trial/aktif/tenggang/read_only/ditangguhkan.
Tampilkan: nama paket (planName), status badge, "Aktif sampai {endsAt}" atau
"Trial sampai {trialEndsAt}". Bila allowsWrites false (read_only/ditangguhkan):
banner merah "Langganan bermasalah — penulisan diblokir, data tetap bisa dibaca".
Tombol "Perpanjang/Hubungi Admin" HANYA info (tidak ada endpoint bayar untuk
perusahaan — pembayaran oleh super admin). JANGAN buat form pembayaran.
```
