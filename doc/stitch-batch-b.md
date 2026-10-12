# Prompt Stitch — Batch B (Operasional, 10 halaman)

> Berlaku penuh "Aturan global untuk SEMUA prompt" dari `doc/stitch-batch-a.md`
> (tempel di awal tiap prompt Batch B juga). Base `/api/v1`, kontrak
> `{status,message,data,errors}` + `meta`, 401 → login, 422 → errors server,
> tanggal Y-m-d, Rp id-ID. DILARANG mengarang endpoint/field/enum.

---

## B1. Kasir / POS

```
Layar kasir cepat (POS) untuk toko & bengkel, mobile-friendly, 3 kolom di desktop
(katalog kiri, keranjang kanan, bayar bawah/samping).

AWAL: pilih cabang (dari daftar cabang perusahaan — GET /companies/{id}/branches
→ [{id, code, name}]), lalu status shift: BUKA shift bila belum ada
(POST .../branches/{branchId}/shifts body {opening_cash:number min 0} → 201;
bila 422 "sudah ada shift terbuka", lanjut pakai yang ada).
Cari item: GET .../items?type=barang|jasa&category_id&per_page (tampilkan nama,
kode, harga cabang — harga per cabang, jangan karang harga).
Harga yang dipakai WAJIB dari server (hitung ulang boleh untuk preview, tapi
total final = respons server).

KERANJANG per baris: item_id, qty (>0), discount_percent (0–100, nullable) ATAU
discount_amount (≥0, nullable) — KEDUANYA BERSAMAAN DILARANG (server 422),
tax_percent (0–100, nullable). Aturan diskon-%-vs-nominal tampilkan sebagai
hint di UI.

BAYAR: payments minimal 1: method HANYA tunai/transfer/qris/ewallet/dp;
amount (>0); tendered (opsional, untuk kembalian tunai); reference (opsional,
max 100, untuk transfer/qris). Total payments harus pas dengan grandTotal
(server menolak bila kurang/lebih — tampilkan errornya). idempotency_key:
string unik per percobaan bayar (mis. crypto.randomUUID()) agar tombol bayar
ganda tidak membuat 2 nota; bila respons "Duplikat dicegah (idempotent.)"
tampilkan nota yang ada.
POST .../branches/{branchId}/sales body {idempotency_key?, items, payments}.
Sukses 201 → struk: number, status, subtotal, discountTotal, taxTotal,
grandTotal, items[{itemId, qty, price, discountPercent, discountAmount,
taxPercent, lineTotal}], payments[{method, amount, tendered, reference}].
Semua angka uang dari server berupa STRING desimal ("90000.00") — parse
dengan Number() untuk tampilan, jangan format ulang nilainya.

TUTUP shift: POST .../branches/{branchId}/shifts/{shiftId}/close body
{closing_cash_counted:number min 0} → tampilkan selisih (difference) + notifikasi
bila tidak nol. Izin: shift.buka, shift.tutup, transaksi.buat.
```

## B2. Nota (daftar + detail)

```
Daftar + detail nota satu cabang. GET .../branches/{branchId}/sales?per_page
→ item {id, number, status, subtotal, discountTotal, taxTotal, grandTotal,
items, payments} (struktur sama seperti struk B1). Status: lunas/void/retur.
Filter client: status + cari number. Detail: GET .../sales/{saleId} +
GET .../sales/{saleId}/returns (daftar retur/void nota itu — endpoint returns
list per cabang: GET .../branches/{branchId}/returns).
Tombol aksi sesuai status & izin (sembunyikan bila tanpa izin, backend 403):
- status lunas + transaksi.void → Void (alasan wajib → B3).
- status lunas + retur.buat → Retur (→ B3).
- status void/retur → badge saja, tidak ada aksi.
Izin lihat: nota.lihat.
```

## B3. Void & Retur

```
Dua dialog dari halaman Nota (B2), BUKAN halaman sendiri:
1. VOID: wajib reason (string max 500). POST .../sales/{saleId}/void {reason}
   (izin transaksi.void) → nota jadi void, stok barang kembali.
   Bila 403 (kasir tanpa izin): tawarkan "Ajukan ke atasan" → POST
   /companies/{id}/approvals {action:"sale.void", entity_id:saleId,
   payload:{reason}} (butuh transaksi.buat).
2. RETUR: pilih baris nota (sale_item_id dari items — SaleItemDTO TIDAK punya id
   baris sendiri; sale_item_id = ... CATATAN: respons items hanya membawa itemId
   tanpa id baris. JANGAN karang sale_item_id — ambil dari endpoint detail yang
   tersedia; bila tidak ada, tampilkan error jujur "API tidak mengembalikan id
   baris — hubungi backend". JANGAN tebak dari index array.)
   Per baris retur: qty (>0, ≤ sisa), condition HANYA layak/rusak.
   refund_method HANYA tunai/transfer/qris/ewallet. reason wajib max 500.
   POST .../sales/{saleId}/returns {items:[{sale_item_id, qty, condition}],
   reason, refund_method} (izin retur.buat). Bila 403: ajukan via approvals
   {action:"sale.refund", entity_id, payload:{lines, reason, refund_method}}
   (butuh retur.buat).
```

## B4. Pelanggan

```
CRUD pelanggan. GET .../customers?per_page → {id, name, phone, address}.
Tambah/ubah: name (wajib max 255), phone (opsional; unik per perusahaan —
server 422 bila duplikat), address (opsional max 500).
POST .../customers, PUT .../customers/{id}, DELETE .../customers/{id}
(ubah/hapus: pelanggan.kelola; lihat: pelanggan.lihat). Hapus pakai konfirmasi
+ tampilkan error server bila pelanggan masih dipakai (jangan karang aturan).
```

## B5. Order Jasa

```
Antrean order jasa bengkel per cabang dengan status: baru → diproses →
selesai → dibayar (atau dibatalkan).
- Buat: POST .../branches/{branchId}/service-orders {customer_id? (integer,
  nullable), items:[{item_id (HANYA type jasa — server 422 bila barang), qty}],
  note? max 500} (transaksi.buat) → status "baru".
- DP: POST .../{orderId}/deposit {amount>0} (transaksi.buat).
- Proses: POST .../{orderId}/process {assignee_id? (integer users.id, nullable)}
  (izin jasa.proses — pelaksana BISA, kasir TIDAK).
- Selesai: POST .../{orderId}/complete (jasa.proses) → bahan terpotong, komisi
  teknisi terhitung (commissionAmount).
- Bayar: POST .../{orderId}/pay {payments:[{method, amount}]} (transaksi.buat;
  DP lama otomatis jadi method "dp" pertama). Sukses 201 → struk nota biasa.
- Batal: POST .../{orderId}/cancel (transaksi.buat).
Board kanban 4 kolom status + filter pelanggan. Tampilkan depositAmount,
assignee, komisi bila ada di respons — JANGAN karang field; render hanya kunci
yang dikembalikan server.
```

## B6. Stok (saldo + mutasi)

```
Stok per cabang (read-mostly):
- Saldo: GET .../branches/{branchId}/stock → [{item, qty, ...}] tampilkan kode,
  nama, qty (string "10.000" → Number), unit. Filter cari nama/kode (client).
- Riwayat: GET .../branches/{branchId}/stock/movements?item_id&per_page →
  tipe/pemasukan/pengeluaran per baris (render apa adanya dari server).
- Mutasi manual (KHUSUS pemegang stok.opname): POST .../stock/movements
  {item_id (required), type HANYA pembelian/penjualan/penyesuaian,
  qty (required, NOT 0 — negatif untuk keluar), unit_cost (wajib bila type
  pembelian, ≥0)}. Bukan untuk jualan (jualan lewat POS) — beri label tegas
  "Penyesuaian manual (tercatat di audit)".
```

## B7. Transfer Stok

```
Alur: draft → diajukan → disetujui → dikirim → diterima (atau dibatalkan
dari draft/diajukan). Hanya SATU izin: stok.transfer (ajukan + putuskan sama).
- Buat: POST /companies/{id}/stock-transfers {from_branch_id, to_branch_id
  (wajib beda — server 422 bila sama), items:[{item_id, qty>0}], note? max 500}.
- Tombol per status, sembunyikan yang tak berlaku: draft → [Ajukan] [Batal];
  diajukan → [Setujui] [Batal]; disetujui → [Kirim]; dikirim → [Terima].
  Endpoint: POST .../stock-transfers/{transferId}/submit|approve|ship|receive|cancel.
- Tiap transisi yang salah status → 422 dengan pesan server (tampilkan).
- Catatan: transfer yang disubmit otomatis masuk inbox Persetujuan global;
  menyetujui dari inbox ATAU dari sini hasilnya sama (idempotent per status).
```

## B8. Opname

```
Stok opname per cabang: buat → diajukan → disetujui (selisih → penyesuaian
stok otomatis) atau ditolak. Izin stok.opname untuk semua langkah.
- Buat: POST .../branches/{branchId}/stock/opnames {items:[{item_id,
  qty_counted ≥0}], note? max 500} → status draft.
- Detail: tampilkan per baris qty_system vs qty_counted + selisih (hitung
  client untuk preview; angka resmi ikut server). Tombol: draft → [Ajukan];
  diajukan → [Setujui] [Tolak].
  POST .../opnames/{opnameId}/submit|approve|reject.
- Setelah disetujui: badge + info penyesuaian terjadi (jangan klaim jurnal
  tertentu — tidak ada di API).
```

## B9. Pembelian (PO + penerimaan)

```
Alur: draft → diajukan → disetujui → dipesan → (diterima) → selesai; batal
dari draft/diajukan/disetujui/dipesan.
- Buat: POST .../purchase-orders {branch_id, supplier_id, items (min 1),
  note?} (po.buat). Item PO: {item_id, qty_ordered, unit_price} — kunci ini
  dari error 422 bila ragu, JANGAN karang (mis. jangan pakai qty saja).
- Tombol: [Ajukan] (po.buat), [Setujui] [Batal] (po.setujui), [Pesan]
  (po.setujui), [Terima] (po.setujui) membuka form penerimaan:
  items:[{item_id, qty_received>0 (≤ sisa), unit_cost ≥0}], due_at? (Y-m-d
  tanggal jatuh tempo tagihan — memengaruhi aging hutang!), note?.
  POST .../purchase-orders/{orderId}/receive.
- Terima parsial boleh (sisa tetap terbuka); terima penuh → selesai.
  Stok + HPP berubah + tagihan supplier terbentuk OTOMATIS (jangan input manual).
- Riwayat penerimaan: GET .../purchase-orders/{orderId}/receipts.
- PO yang disubmit otomatis masuk inbox Persetujuan (aksi po.approve).
```

## B10. Katalog (kategori + item + supplier + harga)

```
Empat tab. Izin: katalog.kelola (semua CRUD), harga.atur (khusus harga).
- Kategori: GET+POST .../categories, GET+PUT+DELETE .../categories/{id} —
  field: name (wajib). Tampilkan daftar untuk dipakai dropdown item.
- Item: GET .../items?type=barang|jasa&category_id&per_page. Tambah/ubah:
  code (wajib, unik per perusahaan), name (wajib max 255), type HANYA
  barang/jasa, unit (WAJIB bila barang, nullable bila jasa, max 20),
  category_id? (harus ada — 404 bila tidak), track_stock (boolean,-benar untuk
  stok), commission_type (nullable, HANYA persen/nominal),
  commission_value (nullable, ≥0). PUT/DELETE .../items/{id}.
- Harga cabang: PUT .../items/{itemId}/prices/{branchId} {price ≥0}
  (harga.atur). Tampilkan matriks item × cabang (harga dari GET items per
  cabang bila tersedia; JANGAN karang endpoint harga massal).
- Supplier: GET+POST .../suppliers, GET+PUT+DELETE .../suppliers/{id} —
  name wajib, phone unik per perusahaan, address max 500.
```
