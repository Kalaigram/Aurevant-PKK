# Prompt Stitch — Batch D+E (Keuangan 6 + Super Admin 4 halaman)

> Berlaku penuh "Aturan global untuk SEMUA prompt" dari `doc/stitch-batch-a.md`
> (tempel di awal tiap prompt juga). Base `/api/v1`, kontrak
> `{status,message,data,errors}` + `meta`, 401 → login, 422 → errors server,
> tanggal Y-m-d, Rp id-ID. DILARANG mengarang endpoint/field/enum.
> Catatan umum batch ini: SEMUA angka uang dari API berupa STRING desimal
> ("250000.00") — parse Number() untuk tampilan. Semua halaman ini butuh izin
> keuangan (KHUSUS Admin Keuangan/Pemilik) KECUALI grup E (KHUSUS super admin);
> tanpa izin sembunyikan menu (backend 403).

---

## D1. Biaya

```
Biaya operasional: daftar + kategori + alur draft → disetujui/ditolak.
- Kategori: GET .../expense-categories?per_page → [{id, name}]. Tambah:
  POST .../expense-categories {name (wajib, unik)} (biaya.setujui). Hapus:
  DELETE .../expense-categories/{id} (biaya.setujui).
- Daftar: GET .../expenses?per_page&branch_id&status=draft|disetujui|ditolak →
  {id, branchId, categoryId, categoryName, amount, method (tunai/transfer),
  date, description, status}. Detail: GET .../expenses/{id}.
- Catat: POST .../expenses {branch_id (wajib), expense_category_id (wajib),
  amount (>0, wajib), method? (tunai/transfer), date (Y-m-d, wajib),
  description (wajib max 500)} (biaya.input) → status draft.
- Putuskan: POST .../expenses/{id}/approve atau .../reject (biaya.setujui).
  Approve menjurnal otomatis + masuk laba rugi; yang masih draft BELUM masuk
  laporan (tampilkan hint ini). Bila periode bulan tanggal biaya sudah ditutup
  → 422 (tampilkan pesan server).
```

## D2. Hutang

```
Hutang supplier (terbentuk otomatis dari penerimaan PO — JANGAN input manual):
- Daftar: GET .../supplier-bills?per_page&unpaid (boolean, true = belum lunas)
  → {id, amount, paidAmount, remaining (string "250000.00"), dueAt (Y-m-d/null),
  isPaid}. Detail: GET .../supplier-bills/{id}.
- Bayar: POST .../supplier-bills/{id}/pay {amount (>0, wajib — server menolak
  422 bila melebihi remaining), method? (tunai/transfer), paid_at? (Y-m-d),
  note? max 500} (biaya.setujui). Parsial boleh; lunas → isPaid true.
  Tampilkan progress amount → paidAmount → remaining per tagihan.
- Aging: GET .../supplier-bills-aging → {buckets:{lancar, "1_30", "31_60",
  "61_90", "lebih_90"}, total} (angka, bukan string). Render 5 kartu bucket +
  total; merah untuk lebih_90. Jelaskan makna: hari keterlambatan dari dueAt.
```

## D3. Piutang

```
Mirip Hutang, arah sebaliknya (bon pelanggan):
- Daftar: GET .../receivables?per_page&unpaid → {id, branchId, customerId
  (bisa null = bon tanpa nama), amount, paidAmount, remaining, dueAt, isPaid}.
- Catat: POST .../receivables {branch_id (wajib), customer_id? (nullable —
  bila diisi harus milik perusahaan, 404 bila tidak), amount (>0),
  due_at? (Y-m-d), note?} (biaya.setujui). Method TIDAK ada di sini (bukan
  kas) — jangan tambahkan field method.
- Terima cicilan: POST .../receivables/{id}/pay {amount (>0, ≤ remaining),
  method? (tunai/transfer/qris/ewallet — EMPAT opsi di sini, beda dengan hutang),
  paid_at?, note?} (biaya.setujui).
- Aging: GET .../receivables-aging → struktur sama seperti aging hutang.
```

## D4. Jurnal (entri + neraca saldo)

```
Buku besar read-only (TIDAK ada input jurnal manual — jangan buat formnya):
- Entri: GET .../journal-entries?per_page&from&to (Y-m-d) → [{id, branch_id
  (bisa null, mis. gaji), date, memo, source_type, source_id,
  lines:[{account:{code, name}, debit, credit}]}]. Filter tanggal + cari memo/
  source. source_type yang mungkin: sale, sale_void, sale_return, po_receipt,
  payroll_accrue, payroll_pay, expense, bill_payment, receivable,
  receivable_payment (JANGAN tambah tipe lain; render label ramah dari peta ini).
  Tiap entri seimbang debit = kredit (jaminan server; tampilkan total per entri).
- Neraca saldo: GET .../reports/trial-balance?from&to (keduanya WAJIB) →
  {rows:[{code, name, debit, credit}], total_debit, total_credit}.
  Tabel + total; sorot bila total_debit ≠ total_credit (seharusnya tak terjadi).
Izin: keuangan.lihat.
```

## D5. Laporan (laba rugi, harian, arus kas, neraca, konsolidasi)

```
Satu halaman multi-tab. Semua butuh keuangan.lihat KECUALI konsolidasi
(laporan.konsolidasi).
- LABA RUGI: GET .../reports/profit-loss?from&to (wajib)&branch_id? →
  {detail:[{code, name, debit, credit, saldo}], pendapatan, hpp, laba_kotor,
  beban, laba_bersih}. Akun 4xxx = pendapatan, 51xx = HPP, 6xxx = beban
  (tampilkan pengelompokan ini). Filter cabang opsional.
- HARIAN: GET .../reports/daily?from&to&branch_id? → ringkasan harian per
  cabang (omzet, transaksi, HPP, diskon, pajak, refund — render kunci yang ada).
- ARUS KAS: GET .../reports/cash-flow?from&to&branch_id? → {detail (per akun
  kas 11xx + bersih), kas_masuk, kas_keluar, kas_bersih}.
- NERACA: GET .../reports/balance-sheet?as_of (Y-m-d, default hari ini) →
  {as_of, aktiva:{kas, piutang, persediaan, total},
  kewajiban:{hutang_supplier, hutang_pajak, hutang_gaji, total}, modal}.
  Tampilkan persamaan aktiva = kewajiban + modal + indikator seimbang.
- KONSOLIDASI (laporan.konsolidasi): GET .../reports/consolidation →
  gabungan semua cabang; tombol Refresh: POST .../reports/refresh (rebuild
  ringkasan harian). Tanpa izin ini sembunyikan tab.
```

## D6. Tutup Periode

```
Kalender periode akuntansi (KHUSUS izin periode.tutup untuk aksi; lihat bebas):
- Daftar: GET .../accounting-periods?per_page → [{id, year, month, label
  ("2026-10"), status: buka/tutup, closedAt}]. Badge hijau buka / merah tutup.
- Tutup: POST .../accounting-periods/close {year (2000–2100, wajib), month
  (1–12, wajib)}. Server menolak 422 bila: bulan depan/besok, sudah tutup,
  masih ada biaya draft di bulan itu, atau ada periode gaji belum dibayar
  yang bersinggungan — tampilkan pesan persisnya + daftar syarat di UI
  sebagai checklist pra-tutup.
- Buka lagi: POST .../accounting-periods/{id}/reopen.
- Peringatan tegas: bulan tutup MENOLAK semua posting jurnal backdate
  (nota/void/retur/PO/gaji/biaya/hutang/piutang ke tanggal itu → 422).
```

---

## E1. Paket (super admin)

```
CRUD paket langganan (KHUSUS super.admin — halaman tidak tampil untuk lainnya).
GET+POST /plans, GET+PUT+DELETE /plans/{id} (path TANPA company — global).
Field: name (wajib, unik), max_branches (integer ≥1, wajib),
max_employees (integer ≥1, wajib), features (array, nullable — render sebagai
daftar tag/bullet, input sebagai textarea satu-baris-satu-fitur),
price (≥0, wajib; 0 = gratis). Respons: {id, name, maxBranches, maxEmployees,
features, price(string)}. Hapus konfirmasi (gagal bila dipakai langganan —
tampilkan error server).
```

## E2. Langganan Perusahaan (kelola, super admin)

```
Ubah status langganan satu perusahaan (KHUSUS super.admin):
GET .../subscription (lihat, struktur Batch A A11) + form PUT
.../subscription {status (wajib, HANYA trial/aktif/tenggang/read_only/
ditangguhkan), plan_id? (integer, nullable), ends_at? (Y-m-d, nullable)}.
Jelaskan tiap status di UI: trial (masa coba), aktif (penuh), tenggang
(peringatan, masih tulis), read_only/ditangguhkan (tulis diblokir).
Audit tercatat otomatis (langganan.ubah).
```

## E3. Invoice Langganan (super admin)

```
Tagihan langganan per perusahaan (KHUSUS super.admin untuk terbit+bayar;
lihat bebas untuk member? Endpoint index/show TANPA middleware izin — tampilkan
untuk semua member perusahaan, tombol aksi hanya super admin):
- Daftar: GET .../subscription-invoices?per_page → {id, plan:{id,name}?,
  amount(string), status: menunggu/lunas, dueAt, paidAt, note}.
- Terbit: POST .../subscription-invoices {plan_id?, amount (>0), due_at
  (Y-m-d, wajib), note?} → status menunggu. (Otomatis harian oleh server untuk
  yang habis ≤7 hari — tampilkan hint ini, jangan duplikasi manual.)
- Bayar: POST .../subscription-invoices/{id}/pay → lunas + perpanjang +30 hari
  (tampilkan endsAt baru dari GET subscription). Bayar ganda → 422.
```

## E4. Libur Nasional (super admin)

```
Kelola libur nasional berlaku semua perusahaan (KHUSUS super.admin):
tabel GET /holidays (path global TANPA company) → [{id, date, name}];
tambah POST /holidays {date (Y-m-d, wajib), name (wajib max 255)};
hapus DELETE /holidays/{id}. Bedakan visual dari Libur Perusahaan (Batch C
C5 — endpoint .../companies/{id}/holidays, izin pengaturan.kelola):
dua tab "Nasional" vs "Perusahaan ini".
```
