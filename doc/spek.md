bantu gw brainstorming aplikasi manajemen bisnis jasa dan barang multicabang dengan hak akses super admin, pemilik perusahaan, dan karyawan ( manajer cabang dan karyawan biasa )


Konsepnya solid, dan tantangan intinya ada di **data multi-cabang + pembagian hak akses**. Ini brainstorming gw, nanti lu koreksi bagian yang nggak cocok.

## Struktur hierarki

**Super Admin → Perusahaan → Cabang → Karyawan.** Gw asumsiin super admin itu pengelola platform (bukan bagian dari satu perusahaan). Kalau asumsi ini salah, arsitekturnya beda, makanya gw tanya di bawah.

## Matriks hak akses (draft)

| Area | Super Admin | Pemilik | Manajer Cabang | Karyawan |
|---|---|---|---|---|
| Kelola perusahaan & langganan | ✅ | – | – | – |
| Kelola cabang | support saja | ✅ | – | – |
| Kelola akun karyawan | – | semua cabang | cabang sendiri | – |
| Katalog barang/jasa & harga | – | ✅ | ajukan / ubah terbatas | lihat |
| Stok | – | semua cabang | cabang sendiri (opname, approve transfer) | input / lihat |
| Transaksi | – | lihat semua | cabang sendiri + approve void/refund | buat transaksi |
| Laporan | statistik platform | konsolidasi semua cabang | cabang sendiri | hasil shift sendiri |

## Modul fitur

- **Master data:** perusahaan, cabang, kategori, barang, jasa, pajak, diskon
- **Barang:** stok per cabang, transfer antar cabang, stok opname, purchase order ke supplier, alert stok minimum
- **Jasa:** katalog layanan, durasi, booking/jadwal, assign karyawan, status pengerjaan, paket/membership
- **Transaksi (POS):** satu nota bisa campur barang + jasa, split payment, void/refund pakai approval, cetak struk
- **Karyawan:** absensi, shift, komisi/insentif, target
- **Keuangan:** kas harian, closing shift, biaya operasional per cabang, laba rugi per cabang, piutang/hutang
- **Dashboard & laporan:** konsolidasi buat pemilik, perbandingan antar cabang, top produk, performa karyawan
- **Pelanggan:** riwayat transaksi, loyalty/poin
- **Audit log:** siapa mengubah apa, kapan

## Hal tricky yang sering kelewat

1. **Harga seragam atau beda per cabang?** Ini mempengaruhi desain database dari awal.
2. **Jasa yang makan barang**, misal servis yang ganti sparepart. Stok harus kepotong otomatis.
3. **Alur approval:** void, diskon besar, opname, transfer stok. Siapa yang approve siapa?
4. **Role fixed atau custom (RBAC fleksibel)?** Fixed lebih cepat dibangun, custom lebih fleksibel buat bisnis yang beda-beda.
5. **Karyawan pindah/rangkap cabang**, dan akses datanya gimana.
6. **Mode offline buat kasir** kalau internet cabang tidak stabil. Ini mahal dikerjain, jadi layak diputuskan dari awal ada atau nggak.

## Saran scope MVP

Mulai dari **kasir + stok per cabang + laporan konsolidasi pemilik**. Payroll, loyalty, dan offline mode ditunda dulu.

Satu hal yang paling nentuin arah: ini mau dibikin sebagai **platform SaaS** yang bisa dipakai banyak perusahaan berbeda (super admin ngurus semua tenant), atau **sistem internal** buat satu perusahaan tertentu (super admin cuma admin teknis)?


beri saya pertanyaan yang bagi anda masih ambigu

Berikut pertanyaan yang menurut saya masih ambigu, saya urutkan dari yang paling berpengaruh pada arsitektur.

**Fondasi**
1. Ini **platform SaaS** untuk banyak perusahaan, atau **sistem internal** untuk satu perusahaan? Jawaban ini menentukan apakah super admin mengelola banyak tenant atau hanya admin teknis.
2. Target bisnisnya **spesifik** (misalnya laundry, bengkel, salon, toko ritel) atau **generik** untuk semua jenis usaha? Alur jasa sangat bergantung pada ini.
3. Perkiraan skalanya berapa perusahaan, cabang, dan karyawan di tahap awal?
4. Tujuan proyeknya apa: tugas kuliah, lomba, atau produk yang akan benar-benar dipakai bisnis? Ini memengaruhi seberapa dalam scope-nya.

**Hak akses**
5. Apakah super admin boleh **melihat atau mengubah data bisnis** suatu perusahaan (misalnya untuk support), atau hanya mengelola akun dan langganan?
6. Apakah satu pemilik bisa punya **lebih dari satu perusahaan**?
7. Apakah role-nya **tetap** (4 role itu saja) atau pemilik bisa membuat role custom dengan izin yang diatur sendiri?
8. Apakah satu karyawan hanya boleh di **satu cabang**, atau bisa rangkap atau berpindah?
9. Apakah ada pihak lain di luar 4 role itu, misalnya **pelanggan** yang login untuk booking atau melihat riwayat, atau supplier?

**Barang dan jasa**
10. Apakah **katalog dan harga seragam** di semua cabang, atau tiap cabang bisa punya harga dan item sendiri?
11. Apakah jasa memakai **booking atau jadwal**, atau langsung dikerjakan saat pelanggan datang? Apakah jasa bisa menghabiskan stok barang (misalnya servis dengan sparepart)?
12. Apakah perlu pembelian ke supplier (PO, penerimaan barang), atau stok cukup dicatat masuk dan keluar saja?

**Transaksi dan keuangan**
13. Apakah aplikasi hanya **mencatat** pembayaran, atau terintegrasi dengan QRIS atau payment gateway?
14. Seberapa jauh cakupan keuangannya: hanya laporan penjualan, atau sampai laba rugi, hutang piutang, dan payroll?

**Teknis**
15. Platform yang diinginkan apa: web, mobile, desktop, atau kombinasi? Apakah kasir harus tetap bisa bekerja **saat offline**?

Tidak perlu menjawab semuanya. Kalau ada yang dilewati, saya buat asumsi dan saya tandai jelas supaya bisa Anda koreksi nanti. Kalau waktunya terbatas, nomor **1, 2, 10, dan 11** yang paling menentukan.

1. untuk banyak perusahaan 2. target bisnisnya universal 3. skala nasional 4. tugas sekolah dan siap deployment 5. pilih yang paling make sense 6. iya bisa 7. ada role custom, tapi subrole karyawan, anda plan yang paling make sense , sesuaikan subrole dengan fitur autorisasi8. bisa pindah jika dia pindah penempatan 9. gausah kayanya 10. tiap cabang punya harga sendiri  11. pilih yang paling make sense untuk solve ini 12.  perlu 13. mencatat pembayaran saja 14.  sampai laba rugi, dan lain lain 15. web + mobile dengan vue laravel dan kotlin jetpack compose, entar juga ada fitur absensi dan penggajian berdasarkan absen, misal 1/30 dia ga hadir, gajinya dipotong 1/30 per bulan, paham? untuk absensi generate card dan barcode di profil karyawan

Paham. Aturan potong gaji 1/30 per hari tidak hadir itu pro-rata harian: `potongan = gaji pokok ÷ 30 × jumlah hari tidak hadir`. Misalnya gaji pokok Rp3.000.000 dan absen 1 hari, maka potongannya Rp100.000. Detail edge case-nya ada di bagian penggajian di bawah.

Ini keputusan saya untuk poin yang Anda serahkan, ditambah implikasi dari jawaban Anda.

## Arsitektur multi-tenant
Untuk SaaS skala nasional, saya sarankan **satu database bersama dengan kolom `company_id`** di setiap tabel bisnis, diamankan lewat global scope di Laravel. Ini jauh lebih mudah dirawat daripada database per tenant, dan cukup kuat kalau indeksnya benar. Karena skalanya nasional, **zona waktu (WIB/WITA/WIT) disimpan per cabang**. Ini berpengaruh ke absensi, closing shift, dan laporan harian.

## Hak akses (poin 5 dan 7)
- **Super Admin:** mengelola tenant, langganan, dan suspend. Secara default **tidak bisa melihat data bisnis**. Untuk kebutuhan support, ada mode "lihat sebagai" yang butuh izin dan selalu tercatat di audit log.
- **Pemilik:** role bawaan per perusahaan, punya akses penuh ke semua cabang, dan bisa memiliki beberapa perusahaan.
- **Karyawan:** pakai **role custom berbasis permission**. Role = kumpulan permission (misalnya `stok.opname`, `transaksi.void`, `absensi.approve`) + **scope** (milik sendiri / cabang / seluruh perusahaan).
- **Subrole** berupa template yang bisa diubah pemilik, bukan hardcode: Manajer Cabang, Kasir, Staf Gudang, Pelaksana Jasa, Admin Keuangan, dan HR.
- Di Laravel ini cocok dengan `spatie/laravel-permission` mode teams, dengan company sebagai team.
- **Penempatan karyawan** disimpan sebagai riwayat (cabang, role, tanggal mulai, tanggal selesai). Data transaksi lama tetap menempel ke cabang lama, dan role bisa berbeda di tiap penempatan.

## Barang dan jasa (poin 11)
Satu katalog dengan item bertipe **barang** atau **jasa**. Jasa bisa dikonfigurasi dengan:
- Mode **langsung** (walk-in) atau **berjadwal**. Booking dibuat oleh staf karena pelanggan tidak login.
- **Bahan habis pakai** (resep sederhana) yang otomatis memotong stok saat jasa selesai.
- Status order: baru → diproses → selesai → dibayar.

Harga disimpan per item per cabang. Stok pakai **moving average** untuk HPP supaya laba rugi tidak rumit.

## Alur antar modul
- **PO supplier:** PO → penerimaan barang → stok bertambah → hutang supplier → HPP.
- **Transaksi:** penjualan → stok berkurang → kas/piutang → pendapatan.
- **Penggajian:** absensi → gaji bersih → masuk sebagai biaya di laba rugi.
- **Laba rugi:** pendapatan − HPP − biaya operasional − gaji, per cabang dan konsolidasi.

## Absensi dan penggajian
Beberapa aturan yang perlu diputuskan, dan saran saya:
- **Pembagi 30** dibuat konfigurasi per perusahaan (default 30), karena ada perusahaan yang memakai hari kerja aktual.
- Tiap **jenis ketidakhadiran** punya flag "memotong gaji atau tidak". Misalnya cuti dan sakit bersurat tidak memotong, alpa memotong.
- Keterlambatan, lembur, dan libur nasional bisa jadi fase lanjutan.
- **Kartu karyawan** (foto, nama, jabatan, cabang, ID, barcode/QR) di-generate otomatis dari profil dan bisa diunduh sebagai PDF atau gambar.
- Barcode statis mudah difoto lalu dititipkan ke orang lain. Mitigasinya: scan di perangkat cabang, ditambah geofence GPS atau selfie, atau QR dinamis yang berubah berkala.

## Urutan pengerjaan
1. **Fase 1:** tenant, autentikasi, RBAC, cabang, katalog, stok + PO, kasir/transaksi, laporan dasar.
2. **Fase 2:** HR (penempatan, absensi, kartu, penggajian).
3. **Fase 3:** laba rugi lengkap, hutang/piutang, dan audit log yang diperluas.

Pertanyaan berikutnya: absensi dilakukan dengan **kartu karyawan yang di-scan di perangkat milik cabang** (tablet atau scanner), atau **karyawan scan sendiri dari HP lewat aplikasi mobile**? Jawabannya menentukan alur absensi, kebutuhan perangkat di cabang, dan cara mencegah titip absen.

nah pertanyaan itu riset paling masuk akal dan paling aman

Saya putuskan pakai **dua jalur absensi dengan satu jalur utama**, dan jalur utamanya lewat HP karyawan. Alasannya ada di perbandingan ini.

| | Jalur utama: scan QR cabang lewat HP karyawan | Jalur cadangan: kartu di-scan di perangkat cabang |
|---|---|---|
| Identitas | Akun + HP yang terikat ke karyawan | Kartu karyawan |
| Risiko titip absen | Rendah (login pribadi, HP terikat) | Sedang (kartu bisa dititipkan) |
| Butuh apa | Karyawan punya smartphone | Tablet/scanner di cabang |
| Cocok untuk | Mayoritas karyawan | Karyawan tanpa smartphone, atau HP rusak |

## Lapisan keamanan (untuk jalur utama)
1. **QR cabang dinamis.** Layar atau tablet cabang menampilkan QR yang berganti tiap 30-60 detik (token berbasis waktu, divalidasi server). Foto QR yang dikirim ke teman tidak berguna.
2. **Ikat 1 akun ke 1 perangkat.** Ganti HP harus disetujui HR atau manajer.
3. **Geofence per cabang.** Absen hanya sah dalam radius lokasi cabang. Aplikasi juga mendeteksi lokasi palsu (mock location) dan memakai Play Integrity API untuk mendeteksi perangkat yang dimodifikasi.
4. **Selfie saat absen.** Disimpan sebagai bukti dan bisa direview manajer. Pencocokan wajah otomatis ditunda ke fase lanjutan.
5. **Waktu dari server**, bukan dari HP, dan dikonversi ke zona waktu cabang.
6. **Koreksi absensi hanya lewat pengajuan + persetujuan**, dan semuanya tercatat di audit log.

Untuk jalur cadangan, kartu di-scan di perangkat cabang dan wajib dikombinasikan dengan **selfie + PIN karyawan**, supaya kartu yang dititipkan saja tidak cukup.

## Kartu karyawan
- Di-generate dari profil (PDF/PNG): foto, nama, ID karyawan, jabatan, cabang, perusahaan, dan kode.
- Saya sarankan **QR code**, bukan barcode 1D. QR lebih toleran terhadap goresan dan lebih mudah dibaca kamera HP.
- Isi QR berupa **ID opaque (UUID)**, bukan data pribadi seperti NIK. Dengan begitu kartu yang hilang tidak membocorkan data, dan kartu saja tidak bisa dipakai absen tanpa faktor kedua.
- Kartu versi digital juga ditampilkan di aplikasi mobile.

## Dampak ke penggajian
Supaya aturan potong 1/30 bisa jalan, sistem perlu tahu **kapan seseorang seharusnya hadir**. Artinya dibutuhkan:
- **Jadwal kerja/shift** per karyawan dan **kalender libur** (nasional + per perusahaan).
- Proses otomatis di akhir hari (per zona waktu cabang) yang menandai **alpa** kalau tidak ada absen, izin, cuti, atau libur.
- Status absensi (hadir, terlambat, alpa, izin, sakit, cuti) yang masing-masing punya flag "memotong gaji atau tidak".

## Privasi
Selfie dan lokasi termasuk data pribadi. Ambil lokasi **hanya pada saat absen**, bukan pelacakan terus-menerus. Tampilkan persetujuan di aplikasi dan tetapkan masa simpan foto (misalnya 90 hari). Karena aplikasinya akan dideploy sungguhan, ini sebaiknya selaras dengan UU Pelindungan Data Pribadi (UU No. 27 Tahun 2022).

Pertanyaan berikutnya, karena sangat menentukan cara mendeteksi alpa: apakah jam kerja karyawan di bisnis target Anda umumnya **shift tetap** (jam yang sama tiap hari) atau **jadwal bergilir** yang diatur manajer tiap minggu atau bulan?
untuk pertanyaan iini pilih yang paling terbaikj dari semua aspek

Saya putuskan **mendukung keduanya lewat satu model jadwal yang fleksibel**. Karena target bisnisnya universal, toko ritel biasanya shift tetap, sedangkan klinik, salon, dan restoran sering bergilir. Memilih satu saja berarti mengunci sebagian pasar.

## Model jadwal (3 lapis)
1. **Master shift** per perusahaan: nama, jam mulai, jam selesai, toleransi terlambat. Mendukung **shift lintas hari** (misalnya 22:00-06:00).
2. **Pola default per karyawan** (mingguan): misalnya Senin-Jumat shift Pagi, Sabtu-Minggu libur. Ini yang dipakai bisnis dengan shift tetap, dan cukup diatur sekali.
3. **Roster/override per tanggal**: manajer cabang menimpa pola default untuk tanggal tertentu, atau menyusun jadwal bergilir satu minggu atau satu bulan sekaligus lewat tampilan kalender.

**Jadwal efektif** pada suatu tanggal ditentukan dengan urutan: override per tanggal → pola default → libur. Bisnis shift tetap tidak perlu menyentuh roster sama sekali.

## Deteksi alpa
- Proses otomatis per cabang dijalankan setelah **shift berakhir plus masa tenggang**, memakai zona waktu cabang. Untuk shift lintas hari, tanggalnya mengikuti **tanggal mulai shift**.
- Kalau jadwal efektifnya "kerja" dan tidak ada absen, izin, atau cuti yang disetujui, statusnya otomatis menjadi **alpa**.
- Libur nasional dan libur perusahaan menghapus kewajiban hadir.
- Pembagi 1/30 tidak terpengaruh jenis jadwal, karena potongan dihitung dari jumlah hari alpa yang berpotongan gaji.

## Aturan pendukung
- **Perubahan jadwal** setelah dipublikasikan perlu alasan dan tercatat di audit log. Jadwal yang sudah lewat tidak bisa diubah sembarangan, karena memengaruhi gaji.
- **Pindah penempatan** di tengah bulan: jadwal dan absensi mengikuti cabang pada tanggal itu, dan gajinya dihitung pro-rata lintas penempatan.
- **Tukar shift** antar karyawan dibuat sebagai fitur fase lanjutan (diajukan, lalu disetujui manajer).
- **Permission baru** untuk modul ini: `jadwal.lihat`, `jadwal.atur`, `shift.kelola`, dan `absensi.koreksi`.

## Ringkasan tabel
`shifts`, `employee_schedule_patterns`, `schedule_overrides`, `holidays`, `attendances`, `leave_requests`, `attendance_statuses` (dengan flag potong gaji).

Pertanyaan berikutnya: **potongan 1/30 itu dihitung dari gaji pokok saja, atau dari total penghasilan** (termasuk tunjangan tetap)? Dan apakah ada komponen yang dihitung per hari hadir, seperti uang makan atau transport harian? Ini menentukan struktur komponen gaji. Sekalian putuskan juga apakah **BPJS dan PPh 21** masuk scope atau ditunda, karena aplikasinya akan dideploy sungguhan.
pilih yang paling terbaik

Saya putuskan pakai **sistem komponen gaji yang fleksibel**. Tiap komponen punya aturan sendiri, jadi potongan 1/30 tetap berlaku tanpa mengunci struktur gaji yang berbeda di tiap bisnis.

## Struktur komponen gaji
Tiap komponen punya tipe, cara hitung, dan flag:

| Komponen | Cara hitung | Terpotong alpa 1/30? |
|---|---|---|
| Gaji pokok | Tetap bulanan | Ya |
| Tunjangan tetap (jabatan, dll.) | Tetap bulanan | Ya (default) |
| Uang makan / transport | Per hari hadir | Tidak, karena sudah dihitung dari kehadiran |
| Komisi jasa | Otomatis dari transaksi yang dikerjakan karyawan | Tidak |
| Bonus / lembur / potongan manual | Input HR | Sesuai pengaturan |

**Aturan inti:** potongan alpa = `(total komponen bertanda "terpotong alpa") ÷ pembagi × jumlah hari alpa`. Komponen per hari hadir tidak boleh dipotong lagi, karena itu akan jadi potongan ganda.

**Contoh:** gaji pokok Rp3.000.000, tunjangan tetap Rp600.000, uang makan Rp25.000 per hari hadir, hadir 22 hari, alpa 1 hari. Potongan alpa = Rp3.600.000 ÷ 30 × 1 = **Rp120.000**. Uang makan = 22 × Rp25.000 = Rp550.000.

Pembagi (default 30), basis potongan, dan potongan terlambat (default mati) bisa diatur per perusahaan.

## Proses penggajian
- **Periode dan cut-off** bisa diatur (misalnya tanggal 26 sampai 25).
- Status: draft → review → disetujui → dibayar. Karena pembayaran hanya dicatat, "dibayar" berupa penandaan manual.
- Setelah disetujui, periode **dikunci** dan slip gaji di-snapshot. Koreksi dilakukan lewat penyesuaian di periode berikutnya, bukan mengubah data lama.
- **Pro-rata** untuk karyawan baru, karyawan keluar, dan pindah penempatan di tengah periode.
- **Slip gaji** dalam bentuk PDF, dan karyawan bisa melihat slip miliknya sendiri di aplikasi mobile.
- Beban gaji dibebankan ke **cabang sesuai penempatan** dan masuk ke laba rugi saat periode disetujui.

## Hak akses
`gaji.lihat_sendiri`, `gaji.kelola`, dan `gaji.setujui`. Secara default hanya Pemilik, HR, dan Admin Keuangan yang bisa melihat gaji orang lain. **Manajer cabang tidak**, kecuali pemilik mengaktifkannya.

## BPJS dan PPh 21
Saya sarankan **ditunda ke fase 3, tapi disiapkan dari desain awal**:
- Komponen gaji sudah punya flag `kena pajak` dan `basis BPJS`.
- Tarif dan batas disimpan sebagai **data konfigurasi dengan tanggal berlaku**, bukan hardcode, supaya perubahan aturan tidak butuh perubahan kode.
- Perhitungan PPh 21 (skema TER yang berlaku sejak 2024, lewat PMK 168/2023) dan iuran BPJS sebaiknya diverifikasi ke aturan terbaru saat mulai diimplementasikan, karena aturannya bisa berubah.

Alasannya, kalau dibuat sekarang, fase 2 jadi terlalu berat. Bisnis kecil juga banyak yang belum memakainya, sehingga fitur ini bisa dinyalakan opsional per perusahaan.

## Keputusan yang sudah terkumpul
SaaS multi-tenant, RBAC custom dengan scope, penempatan dengan riwayat, harga per cabang, PO supplier, absensi QR dinamis lewat HP (kartu sebagai cadangan), jadwal fleksibel (pola tetap + roster), dan penggajian berbasis komponen.

Pertanyaan berikutnya: apakah Anda mau **membahas modul yang belum didalami** (stok, PO, dan keuangan/laba rugi), atau pembahasan sudah cukup dan saya **susun semuanya menjadi dokumen PRD**?

Berikut keputusan saya untuk tiga modul yang tersisa.

## 1. Stok

- **Stok hanya untuk barang.** Jasa tidak punya stok, kecuali bahan habis pakai yang dipotong saat jasa selesai.
- **Stok per cabang.** Gudang pusat dibuat sebagai cabang bertipe "gudang", jadi tidak perlu konsep lokasi tambahan.
- **Ledger stok bersifat append-only** (`stock_movements`). Semua perubahan lewat mutasi bertipe: pembelian, penjualan, retur, transfer, opname, penyesuaian, dan pemakaian bahan. Saldo adalah hasil turunan, dan koreksi dilakukan dengan mutasi baru, tidak mengubah data lama.
- **Satuan dan konversi** (dus → pcs) didukung. **Varian** (ukuran, warna) dan **batch/expired/serial** dibuat opsional per item lewat flag, karena targetnya universal.
- **Stok negatif dilarang** secara default (bisa diatur per perusahaan), dan pemotongan stok memakai transaksi database dengan row lock supaya dua kasir tidak menjual stok terakhir yang sama.
- **Valuasi: moving average per cabang**, diperbarui saat barang diterima.

**Transfer antar cabang:** draft → diajukan → disetujui → dikirim → diterima.
- Stok keluar dari cabang asal saat **dikirim**, dan masuk ke cabang tujuan saat **diterima**. Selama perjalanan stoknya berstatus *in transit*, jadi tidak hilang dari pencatatan.
- Selisih penerimaan dicatat dan perlu persetujuan.
- Nilai yang berpindah memakai average cost cabang asal.

**Stok opname:** hitung fisik → selisih → penyesuaian dengan persetujuan manajer atau pemilik. Penyesuaian yang sudah disetujui tidak bisa diubah.

Alert stok minimum dikonfigurasi per item per cabang.

## 2. PO dan supplier

- **Supplier milik perusahaan** (dipakai lintas cabang), menyimpan harga beli terakhir per item.
- **Alur PO:** draft → diajukan → disetujui → dipesan → diterima sebagian atau penuh → ditutup/dibatalkan.
- **Approval berdasarkan nilai:** misalnya di atas nominal tertentu harus disetujui pemilik. Ambang batasnya diatur per perusahaan.
- **Penerimaan barang** dicatat terpisah dari PO (mendukung penerimaan sebagian, lampiran nota atau foto). **Stok dan HPP berubah saat barang diterima, bukan saat PO dibuat.**
- **Pembelian terpusat dan per cabang** sama-sama didukung: PO ke gudang pusat lalu didistribusi lewat transfer, atau PO langsung untuk cabang.
- **Hutang supplier:** tagihan, jatuh tempo, pembayaran parsial (hanya dicatat), dan aging hutang.
- **Retur ke supplier** mengurangi stok dan hutang sekaligus.
- Pajak (PPN) dan diskon bisa ditandai inklusif atau eksklusif per dokumen.

## 3. Keuangan dan laba rugi

**Keputusan utama:** setiap kejadian bisnis (penjualan, retur, penerimaan barang, biaya, gaji, void) **otomatis membuat jurnal berpasangan di belakang layar** dengan chart of accounts default yang bisa disesuaikan. Pengguna tidak perlu melihat debit dan kredit.
- **Keuntungan:** laba rugi, arus kas, dan nilai persediaan konsisten antar modul dan bisa diaudit.
- **Kekurangan:** desain awalnya lebih berat daripada sekadar menjumlahkan transaksi.
- Karena penggajian, PO, dan retur ikut memengaruhi laba rugi, pendekatan ini lebih aman daripada menjumlahkan tabel satu per satu. Yang ditunda ke fase lanjutan hanya **antarmuka akuntansi** (jurnal manual, neraca penuh).

**Fitur keuangan:**
- **Kas per cabang** dan metode bayar (tunai, transfer, QRIS, e-wallet) dipetakan ke akun masing-masing. Karena pembayaran hanya dicatat, rekonsiliasi bank bersifat manual dan opsional.
- **Shift kasir:** buka shift dengan modal awal, tutup shift dengan hitung fisik versus sistem, dan selisih tercatat.
- **Biaya operasional:** kategori, per cabang, lampiran bukti, approval berdasarkan nominal, dan template biaya berulang (sewa, listrik).
- **Piutang:** penjualan kredit bersifat opsional per perusahaan, dengan jatuh tempo, pembayaran parsial, dan aging piutang.
- **Penutupan periode:** bulan yang sudah ditutup dikunci, koreksi lewat jurnal penyesuaian.
- **Biaya pusat:** dicatat di cabang pusat, alokasi ke cabang lain ditunda ke fase lanjutan.

**Laporan:** laba rugi per cabang, konsolidasi, dan perbandingan antar periode; margin per produk/jasa; arus kas sederhana; aging hutang dan piutang; serta nilai persediaan. Pelaporan pajak resmi di luar scope.

**Permission baru:** `keuangan.lihat`, `biaya.input`, `biaya.setujui`, `periode.tutup`, `laporan.konsolidasi`, `po.buat`, `po.setujui`, `stok.transfer`, `stok.opname`.

Satu modul inti yang belum dibahas mendalam adalah **kasir/transaksi** (alur void, refund, diskon, dan approval-nya). Apakah kita bahas itu dulu, atau langsung saya susun semua keputusan sejauh ini menjadi Claude Doc?

oke bahas alur transaksi

Berikut keputusan saya untuk modul transaksi (kasir dan order jasa).

## Prinsip
- **Nota yang sudah dibayar tidak bisa diedit.** Koreksi hanya lewat void atau retur, dua mekanisme yang berbeda.
- **Harga di baris nota disalin (snapshot)** dari harga cabang saat transaksi, jadi perubahan harga tidak memengaruhi nota lama.
- Setiap transaksi terikat ke **cabang, kasir, dan shift yang sedang terbuka**.

## Alur penjualan barang
1. Kasir membuka shift (modal awal).
2. Memilih item, dengan harga diambil dari harga cabang.
3. Menambah diskon dan pajak.
4. Mencatat pembayaran, lalu nota terbit.
5. Sistem otomatis memotong stok, membuat jurnal, dan menyiapkan struk.

Transaksi yang belum dibayar bisa disimpan sebagai **draft/hold** (misalnya pelanggan lupa dompet), tanpa jejak finansial.

## Alur jasa
Satu nota boleh berisi **campuran barang dan jasa**. Jasa punya dua pola:
- **Langsung:** dikerjakan dan dibayar di tempat, seperti kasir biasa.
- **Berstatus:** baru → diproses (pelaksana ditugaskan) → selesai → dibayar. Cocok untuk jasa yang butuh waktu (servis, laundry, perawatan).

**Uang muka (DP)** didukung. Aturan pengakuan yang saya sarankan:
- Stok barang terpotong saat barang diserahkan.
- **Bahan jasa terpotong saat jasa selesai.**
- Pendapatan jasa diakui saat jasa selesai. DP dicatat sebagai **uang muka pelanggan** (liabilitas) sampai saat itu.
- **Komisi karyawan** terhitung saat jasa selesai dan nota lunas. Kalau ada retur, komisinya dibalik dan nilainya masuk ke komponen gaji.

## Diskon dan pajak
- Diskon bisa per item atau per nota, dalam persen atau nominal.
- **Batas diskon per role** lewat permission. Di atas batas itu butuh **PIN manajer** atau approval.
- PPN bisa inklusif atau eksklusif, diatur per perusahaan. Promo otomatis (beli 2 gratis 1 dan sejenisnya) ditunda ke fase lanjutan.

## Pembayaran (hanya dicatat)
- Mendukung **split payment**: tunai, transfer, QRIS, e-wallet, kombinasi.
- Tunai menghitung kembalian. Metode non-tunai punya kolom referensi opsional (nomor transaksi) untuk mempermudah pencocokan.
- Pembayaran sebagian dan **kredit (piutang)** bersifat opsional per perusahaan dan terhubung ke modul piutang.
- Status nota: lunas, sebagian, belum dibayar.

## Void, retur, dan refund
| Situasi | Mekanisme | Aturan |
|---|---|---|
| Belum dibayar | Hapus draft | Tercatat di log |
| Sudah dibayar, shift belum ditutup | **Void** | Alasan wajib + approval manajer; stok dan jurnal dibalik |
| Shift sudah ditutup, atau barang dikembalikan | **Retur/refund** | Dokumen baru yang merujuk nota asli |

Detail retur:
- Bisa per item atau sebagian.
- Barang kembali ke stok layak jual, atau dicatat sebagai rusak.
- Refund dikembalikan lewat metode bayar tertentu.
- Batas waktu retur diatur per perusahaan.
- Retur jasa dicatat sebagai refund dengan alasan.
- **Nota asli tidak pernah diubah**, sehingga laporan periode yang sudah ditutup tetap konsisten.

## Mesin approval tunggal
Void, diskon besar, refund, PO, transfer, dan opname seharusnya **memakai satu mesin approval yang sama**: aturan berupa jenis aksi, ambang nilai, dan role penyetuju. Persetujuan bisa langsung di tempat (PIN manajer) atau jarak jauh lewat notifikasi di aplikasi mobile. Ini mengurangi duplikasi kode dan membuat audit seragam.

## Shift kasir
- Transaksi hanya bisa dibuat saat ada shift terbuka.
- Tutup shift menampilkan ringkasan per metode bayar, uang tunai fisik versus sistem, dan selisihnya.
- Selisih tunai tercatat sebagai kelebihan atau kekurangan kas, dan masuk ke laporan.

## Reliabilitas dan nomor nota
- **Nomor nota** dibuat server secara atomik dengan format `{kode cabang}-{yyMMdd}-{urut}`, jadi tidak bisa dimanipulasi dari klien.
- **Idempotency key** di setiap pengiriman transaksi, supaya sinyal buruk tidak membuat nota ganda.
- **Mode offline ditunda dari MVP.** API dan aplikasi mobile dirancang idempotent dan punya antrian retry, jadi offline bisa ditambahkan nanti tanpa merombak.
- **Struk:** PDF, bagikan lewat WhatsApp atau tautan, dan cetak ke printer thermal Bluetooth dari aplikasi Android.
- **Data pelanggan opsional** (nama dan kontak) di nota, untuk riwayat dan piutang. Pelanggan tetap tidak punya akun.

## Dampak jurnal otomatis
- **Penjualan lunas:** kas/bank (debit) lawan pendapatan dan PPN keluaran (kredit), ditambah HPP (debit) lawan persediaan (kredit).
- **DP:** kas (debit) lawan uang muka pelanggan (kredit).
- **Retur/void:** kebalikan dari jurnal di atas.

## Permission dan tabel inti
Permission: `transaksi.buat`, `transaksi.diskon` (dengan batas), `transaksi.void`, `retur.buat`, `retur.setujui`, `shift.buka`, `shift.tutup`, `nota.lihat` (scope sendiri, cabang, atau semua).

Tabel inti: `sales`, `sales_items`, `sales_payments`, `sales_returns`, `sales_return_items`, `service_orders`, `cash_shifts`, `approvals`, `customers`.

Modul inti sudah tercakup semua (tenant, RBAC, katalog, stok, PO, transaksi, HR, penggajian, keuangan). Apakah Anda mau saya **susun semuanya jadi satu Claude Doc** sekarang, atau ada modul pendukung (dashboard, notifikasi, pelanggan, audit log) yang mau dibahas dulu?

bahas modul pendukung

Berikut keputusan saya untuk modul pendukung. Beberapa di antaranya (langganan SaaS, onboarding) penting karena Anda menargetkan deployment sungguhan dengan banyak perusahaan.

## 1. Audit log
- **Append-only:** siapa (user, role, penempatan), apa (aksi dan entitas), kapan (UTC + zona cabang), dari mana (IP, perangkat, web atau mobile), dan **before/after** untuk data sensitif.
- **Yang dicatat:** perubahan harga, penyesuaian stok, void/refund, approval, gaji, perubahan role dan permission, koreksi absensi, login, dan semua aksi super admin saat "lihat sebagai".
- Tidak ada fitur edit atau hapus. Tabel dipartisi per bulan, dan masa simpannya diatur per perusahaan.
- **Siapa yang melihat:** pemilik melihat log perusahaannya. Super admin hanya melihat log level platform dan log impersonasi.
- Pencatatan lewat event listener dan antrian (queue) supaya tidak memperlambat transaksi.

## 2. Notifikasi
- **Kanal:** in-app (ikon lonceng), push mobile (FCM), dan email. WhatsApp ditunda karena butuh API resmi dan biaya per pesan.
- **Jenis:** approval masuk atau hasilnya, stok menipis, transfer yang perlu diterima, hutang jatuh tempo, jadwal baru atau berubah, pengingat absen, selisih shift, slip gaji terbit, dan langganan hampir habis.
- **Penerima ditentukan permission + scope**, jadi approval diskon cabang A hanya sampai ke yang berwenang di cabang A.
- Preferensi per pengguna per jenis notifikasi, ringkasan harian opsional untuk pemilik, serta **dedup dan throttling** supaya alert stok tidak berulang terus.
- Untuk MVP, tampilan web cukup polling berkala. Realtime (Laravel Reverb) ditambah belakangan.

## 3. Dashboard (per role)
| Role | Isi utama |
|---|---|
| Pemilik | Omzet dan laba (estimasi), perbandingan antar cabang, tren, kas, hutang jatuh tempo, nilai persediaan, kehadiran hari ini, antrian approval |
| Manajer cabang | Penjualan hari ini, stok menipis, absensi tim, order jasa berjalan, approval menunggu, shift terbuka |
| Karyawan | Tombol absen, jadwal, shift dan transaksi saya, komisi bulan ini, slip gaji |
| Super admin | Tenant aktif, pertumbuhan, pendapatan langganan, tenant mendekati batas paket, kesehatan sistem (error dan antrian) |

**Performa:** untuk skala nasional, dashboard tidak boleh menghitung dari transaksi mentah. Gunakan **tabel ringkasan harian per cabang** yang diperbarui oleh job terjadwal atau event, plus cache. Filter cabang dan periode, dan semua laporan bisa diekspor ke PDF atau Excel.

## 4. Pelanggan (CRM ringan)
- Data pelanggan **milik perusahaan** (lintas cabang), dengan deteksi duplikat berdasarkan nomor HP. Pembeli walk-in boleh tanpa data.
- Isinya: kontak, riwayat transaksi, piutang, DP, dan catatan. Pelanggan tetap **tidak punya akun**.
- Loyalty dan poin ditunda, tetapi skemanya dibuat tidak menutup kemungkinan itu.
- **Privasi:** kumpulkan data seminimal mungkin, sediakan mekanisme anonimisasi atas permintaan, dan batasi ekspor lewat permission (`pelanggan.lihat`, `pelanggan.kelola`, `pelanggan.ekspor`).

## 5. Langganan dan billing SaaS
Ini yang membedakan produk SaaS dari aplikasi internal.
- **Paket** (misalnya Gratis, Dasar, Pro) dengan **batas**: jumlah cabang, jumlah karyawan, dan fitur opsional (penggajian, PO, dan sejenisnya) lewat feature flag per paket.
- Status tenant: trial → aktif → masa tenggang → **read-only** → ditangguhkan. Data **tidak pernah langsung dihapus**, dan pemilik tetap bisa mengekspor datanya.
- **Pembayaran langganan** untuk MVP berupa konfirmasi manual oleh super admin (invoice dan bukti transfer). Gateway pembayaran otomatis ditambah di fase lanjutan.
- Notifikasi otomatis ke pemilik sebelum masa langganan habis.

## 6. Onboarding dan pengaturan perusahaan
- **Wizard awal:** daftar akun → buat perusahaan → cabang pertama (zona waktu) → pilih template role → impor data awal → undang karyawan.
- **Impor dan ekspor Excel/CSV** untuk produk, stok awal, supplier, dan karyawan, dengan pratinjau dan laporan kesalahan per baris sebelum disimpan.
- **Undangan karyawan** lewat email atau tautan aktivasi.
- **Pengaturan perusahaan:** pajak, pembagi gaji, ambang approval, format nomor nota, serta logo dan identitas di struk dan kartu karyawan.

## 7. Akun dan keamanan
- Login dengan email atau nomor HP + password. Laravel Sanctum untuk web (SPA) dan mobile.
- **2FA** opsional, dan sangat disarankan untuk pemilik dan super admin.
- Sesi per perangkat yang bisa dicabut, pembatasan percobaan login (rate limiting), dan pemulihan password.
- Absensi memakai pengikatan perangkat seperti yang sudah diputuskan.

## 8. Kebutuhan non-fungsional (ringkas)
- Antrian dan cache (Redis), pemantauan error (misalnya Sentry), dan backup database terjadwal dengan uji pemulihan.
- Soft delete untuk data bisnis, bahasa Indonesia terlebih dahulu, dan format mata uang Rupiah.
- Pembatasan data antar-tenant diuji otomatis (tes yang memastikan perusahaan A tidak bisa mengakses data perusahaan B). Ini risiko terbesar di SaaS multi-tenant.

**Permission tambahan:** `audit.lihat`, `dashboard.konsolidasi`, `langganan.kelola` (super admin), `impor.jalankan`, `pengaturan.kelola`.

Semua modul sudah terbahas. Apakah sekarang saya susun seluruh keputusan menjadi satu Claude Doc yang rapi (struktur PRD: masalah, peran, fitur per modul, alur, permission, fase pengerjaan)?

Aplikasi Manajemen Bisnis Multicabang — Product Requirement Document
Disusun bersama Claude · 11 Oktober 2026 · Status: draft hasil brainstorming
1. Ringkasan Proyek
Platform SaaS multi-tenant untuk mengelola bisnis jasa dan barang yang memiliki banyak cabang. Satu platform dipakai banyak perusahaan. Tiap perusahaan punya banyak cabang, karyawan dengan hak akses berbeda, dan data yang terisolasi dari perusahaan lain.
• Target bisnis: universal (ritel, salon, bengkel, laundry, klinik, dan sejenisnya), skala nasional.
• Konteks proyek: dikerjakan sebagai tugas akademik, tetapi dirancang siap deployment.
• Platform: web (Vue + Laravel) dan mobile (Kotlin Jetpack Compose).
• Hierarki: Super Admin → Perusahaan → Cabang → Karyawan.
2. Tujuan
• Satu sistem untuk transaksi barang dan jasa, stok, pembelian, SDM, penggajian, dan keuangan.
• Pemilik melihat laporan konsolidasi dan perbandingan antar cabang.
• Hak akses fleksibel lewat role custom berbasis permission.
• Data antar perusahaan benar-benar terisolasi.
• Siap dipakai di produksi: aman, bisa diaudit, dan bisa berlangganan.
3. Ringkasan Keputusan
Topik
Keputusan
Asal
Model bisnis
SaaS untuk banyak perusahaan
Anda
Target dan skala
Bisnis universal, skala nasional
Anda
Tujuan proyek
Tugas akademik dan siap deployment
Anda
Kepemilikan
Satu pemilik bisa punya lebih dari satu perusahaan
Anda
Role
Role custom; subrole karyawan disesuaikan dengan fitur otorisasi
Anda (desain: rekomendasi)
Penempatan
Karyawan bisa pindah cabang bila penempatan berubah
Anda
Pelanggan
Tidak punya akun
Anda
Harga
Berbeda per cabang
Anda
Pembelian
Ada PO ke supplier
Anda
Pembayaran
Hanya dicatat, tanpa payment gateway
Anda
Keuangan
Sampai laba rugi dan laporan terkait
Anda
Teknologi
Vue + Laravel (web), Kotlin Jetpack Compose (mobile)
Anda
Absensi dan gaji
Absensi dan penggajian berdasarkan absensi; potongan 1/30 per hari tidak hadir; kartu dan barcode di profil
Anda
Akses super admin
Tidak melihat data bisnis secara default; impersonasi berizin dan tercatat
Rekomendasi
Absensi
QR cabang dinamis lewat HP karyawan; kartu sebagai cadangan
Rekomendasi
Jadwal
Pola tetap + roster per tanggal
Rekomendasi
Gaji
Sistem komponen gaji fleksibel
Rekomendasi
Jurnal
Jurnal berpasangan otomatis di belakang layar
Rekomendasi
4. Arsitektur dan Multi-Tenant
• Isolasi data: satu database bersama dengan kolom company_id di semua tabel bisnis, diamankan lewat global scope Laravel. Tes otomatis memastikan perusahaan A tidak bisa mengakses data perusahaan B.
• Zona waktu: disimpan per cabang (WIB/WITA/WIT). Database menyimpan UTC. Absensi, closing shift, dan laporan harian mengikuti zona waktu cabang.
• Autentikasi: Laravel Sanctum untuk web (SPA) dan mobile; login email atau nomor HP; 2FA opsional (disarankan untuk pemilik dan super admin); sesi per perangkat yang bisa dicabut; rate limiting.
• Infrastruktur: antrian dan cache dengan Redis; audit log lewat event listener dan queue; pemantauan error; backup terjadwal beserta uji pemulihan.
• Prinsip data: soft delete untuk data bisnis; nota dan mutasi stok bersifat immutable; mata uang Rupiah; bahasa Indonesia terlebih dahulu.
• API: idempotent (idempotency key) dengan antrian retry di mobile, sehingga mode offline bisa ditambahkan nanti tanpa merombak.
5. Peran dan Hak Akses
5.1 Peran
• Super Admin: pengelola platform. Mengelola tenant, paket, langganan, dan suspend. Secara default tidak bisa melihat data bisnis. Untuk support tersedia mode "lihat sebagai" yang butuh izin dan selalu tercatat di audit log.
• Pemilik: role bawaan per perusahaan dengan akses penuh ke semua cabang. Satu pemilik bisa memiliki beberapa perusahaan.
• Karyawan: memakai role custom. Role = kumpulan permission + scope (milik sendiri / cabang / seluruh perusahaan).
5.2 Template subrole karyawan
Manajer Cabang, Kasir, Staf Gudang, Pelaksana Jasa, Admin Keuangan, dan HR. Semuanya berupa template yang bisa diubah pemilik, bukan hardcode. Implementasi yang disarankan: spatie/laravel-permission mode teams dengan company sebagai team.
5.3 Matriks akses ringkas
Area
Super Admin
Pemilik
Manajer Cabang
Karyawan
Perusahaan dan langganan
✅
–
–
–
Cabang
support saja
✅
–
–
Akun karyawan
–
semua cabang
cabang sendiri
–
Katalog dan harga
–
✅
ajukan / ubah terbatas
lihat
Stok
–
semua cabang
cabang sendiri
input / lihat
Transaksi
–
lihat semua
cabang sendiri + approval
buat transaksi
Gaji orang lain
–
✅
tidak (bisa diaktifkan pemilik)
hanya milik sendiri
Laporan
statistik platform
konsolidasi
cabang sendiri
hasil shift sendiri
5.4 Penempatan karyawan
Penempatan disimpan sebagai riwayat (cabang, role, tanggal mulai, tanggal selesai). Satu karyawan punya satu penempatan aktif. Transaksi lama tetap menempel ke cabang lama, dan role bisa berbeda di tiap penempatan.
6. Katalog Barang dan Jasa
• Satu katalog dengan item bertipe barang atau jasa, kategori, pajak, dan diskon.
• Harga per item per cabang.
• Barang: satuan dan konversi (dus → pcs), varian opsional, batch/expired/serial opsional lewat flag per item.
• Jasa: durasi, mode langsung atau berstatus, dan bahan habis pakai (resep sederhana) yang memotong stok saat jasa selesai.
7. Stok
• Stok hanya untuk barang, dicatat per cabang. Gudang pusat dibuat sebagai cabang bertipe "gudang".
• Ledger append-only (stock_movements): pembelian, penjualan, retur, transfer, opname, penyesuaian, pemakaian bahan. Saldo adalah hasil turunan; koreksi lewat mutasi baru.
• Stok negatif dilarang secara default (bisa diatur per perusahaan). Pemotongan memakai transaksi database dan row lock.
• Valuasi: moving average per cabang, diperbarui saat barang diterima.
• Alert stok minimum per item per cabang.
7.1 Transfer antar cabang
Alur: draft → diajukan → disetujui → dikirim → diterima.
• Stok keluar dari cabang asal saat dikirim dan masuk ke tujuan saat diterima. Selama perjalanan statusnya in transit.
• Selisih penerimaan dicatat dan perlu persetujuan.
• Nilai berpindah memakai average cost cabang asal.
7.2 Stok opname
Hitung fisik → selisih → penyesuaian dengan persetujuan. Penyesuaian yang disetujui tidak bisa diubah.
8. PO dan Supplier
• Supplier milik perusahaan (lintas cabang), menyimpan harga beli terakhir per item.
• Alur PO: draft → diajukan → disetujui → dipesan → diterima sebagian atau penuh → ditutup atau dibatalkan.
• Approval berdasarkan nilai: ambang batas diatur per perusahaan.
• Penerimaan barang dicatat terpisah (mendukung penerimaan sebagian, lampiran nota atau foto). Stok dan HPP berubah saat barang diterima, bukan saat PO dibuat.
• Mendukung pembelian terpusat (PO ke gudang pusat lalu transfer) dan pembelian per cabang.
• Hutang supplier: tagihan, jatuh tempo, pembayaran parsial (dicatat saja), dan aging hutang.
• Retur ke supplier mengurangi stok dan hutang.
• PPN dan diskon bisa ditandai inklusif atau eksklusif per dokumen.
9. Transaksi (Kasir dan Jasa)
9.1 Prinsip
• Nota yang sudah dibayar tidak bisa diedit. Koreksi hanya lewat void atau retur.
• Harga di baris nota disalin (snapshot) dari harga cabang saat transaksi.
• Setiap transaksi terikat ke cabang, kasir, dan shift yang terbuka.
9.2 Alur penjualan
1. Kasir membuka shift (modal awal).
2. Memilih item (harga cabang), menambah diskon dan pajak.
3. Mencatat pembayaran; nota terbit.
4. Sistem memotong stok, membuat jurnal, dan menyiapkan struk.
Transaksi yang belum dibayar bisa disimpan sebagai draft/hold tanpa jejak finansial.
9.3 Jasa
Satu nota boleh berisi campuran barang dan jasa. Pola jasa: langsung (dikerjakan dan dibayar di tempat) atau berstatus (baru → diproses → selesai → dibayar). Uang muka (DP) didukung.
• Stok barang terpotong saat diserahkan; bahan jasa terpotong saat jasa selesai.
• Pendapatan jasa diakui saat jasa selesai. DP dicatat sebagai uang muka pelanggan.
• Komisi karyawan terhitung saat jasa selesai dan nota lunas; retur membalik komisi.
9.4 Diskon dan pajak
Diskon per item atau per nota (persen atau nominal) dengan batas per role. Di atas batas butuh PIN manajer atau approval. PPN inklusif atau eksklusif diatur per perusahaan.
9.5 Pembayaran (hanya dicatat)
• Split payment: tunai, transfer, QRIS, e-wallet, atau kombinasi. Tunai menghitung kembalian; non-tunai punya kolom referensi opsional.
• Pembayaran sebagian dan kredit (piutang) bersifat opsional per perusahaan.
• Status nota: lunas, sebagian, belum dibayar.
9.6 Void, retur, dan refund
Situasi
Mekanisme
Aturan
Belum dibayar
Hapus draft
Tercatat di log
Sudah dibayar, shift belum ditutup
Void
Alasan wajib dan approval manajer; stok dan jurnal dibalik
Shift sudah ditutup, atau barang dikembalikan
Retur/refund
Dokumen baru yang merujuk nota asli
Retur bisa per item atau sebagian, barang kembali ke stok layak jual atau dicatat rusak, refund lewat metode bayar tertentu, dan batas waktu retur diatur per perusahaan. Nota asli tidak pernah diubah.
9.7 Shift kasir
Transaksi hanya bisa dibuat saat ada shift terbuka. Tutup shift menampilkan ringkasan per metode bayar, uang tunai fisik versus sistem, dan selisihnya (dicatat sebagai kelebihan atau kekurangan kas).
9.8 Reliabilitas
• Nomor nota dibuat server secara atomik dengan format {kode cabang}-{yyMMdd}-{urut}.
• Idempotency key di setiap pengiriman transaksi.
• Struk: PDF, bagikan lewat WhatsApp atau tautan, dan cetak ke printer thermal Bluetooth dari aplikasi Android.
• Data pelanggan di nota bersifat opsional.
9.9 Mesin approval tunggal
Void, diskon besar, refund, PO, transfer, dan opname memakai satu mesin approval: aturan berupa jenis aksi, ambang nilai, dan role penyetuju. Persetujuan bisa langsung di tempat (PIN manajer) atau jarak jauh lewat notifikasi di aplikasi mobile.
10. SDM: Jadwal, Absensi, dan Kartu
10.1 Jadwal kerja (mendukung shift tetap dan bergilir)
1. Master shift per perusahaan: nama, jam mulai dan selesai, toleransi terlambat; mendukung shift lintas hari.
2. Pola default per karyawan (mingguan).
3. Roster/override per tanggal oleh manajer cabang.
Jadwal efektif: override per tanggal → pola default → libur. Perubahan jadwal setelah dipublikasikan perlu alasan dan tercatat; jadwal yang sudah lewat tidak bisa diubah sembarangan. Tukar shift antar karyawan masuk fase lanjutan.
10.2 Absensi
Jalur utama: karyawan scan QR cabang lewat HP.
• QR cabang dinamis, berganti tiap 30-60 detik (token berbasis waktu, divalidasi server).
• Satu akun terikat ke satu perangkat; ganti HP perlu persetujuan HR atau manajer.
• Geofence per cabang, deteksi lokasi palsu, dan Play Integrity API.
• Selfie saat absen sebagai bukti yang bisa direview manajer.
• Waktu dari server, dikonversi ke zona waktu cabang.
• Koreksi absensi hanya lewat pengajuan dan persetujuan, tercatat di audit log.
Jalur cadangan: kartu karyawan di-scan di perangkat milik cabang (tablet atau scanner), wajib dikombinasikan dengan selfie dan PIN karyawan.
10.3 Deteksi alpa
Proses otomatis per cabang berjalan setelah shift berakhir ditambah masa tenggang (zona waktu cabang). Jika jadwal efektif "kerja" dan tidak ada absen, izin, atau cuti yang disetujui, statusnya menjadi alpa. Untuk shift lintas hari, tanggal mengikuti tanggal mulai shift. Libur nasional dan libur perusahaan menghapus kewajiban hadir.
Status absensi: hadir, terlambat, alpa, izin, sakit, cuti. Masing-masing punya flag "memotong gaji atau tidak".
10.4 Kartu karyawan
• Di-generate otomatis dari profil (PDF atau PNG): foto, nama, ID karyawan, jabatan, cabang, perusahaan, dan kode.
• Kode berupa QR dengan isi ID opaque (UUID), bukan data pribadi. Kartu saja tidak cukup untuk absen.
• Versi digital tersedia di aplikasi mobile.
10.5 Privasi
Selfie dan lokasi adalah data pribadi. Lokasi diambil hanya saat absen, ada persetujuan di aplikasi, dan masa simpan foto ditetapkan (misalnya 90 hari). Selaraskan dengan UU Pelindungan Data Pribadi (UU No. 27 Tahun 2022).
11. Penggajian
11.1 Komponen gaji
Komponen
Cara hitung
Terpotong alpa?
Gaji pokok
Tetap bulanan
Ya
Tunjangan tetap
Tetap bulanan
Ya (default)
Uang makan / transport
Per hari hadir
Tidak (sudah berbasis kehadiran)
Komisi jasa
Otomatis dari transaksi
Tidak
Bonus / lembur / potongan manual
Input HR
Sesuai pengaturan
Aturan inti: potongan alpa = (total komponen bertanda "terpotong alpa") ÷ pembagi × jumlah hari alpa. Pembagi default 30 dan bisa diatur per perusahaan. Komponen per hari hadir tidak dipotong lagi, untuk menghindari potongan ganda.
Contoh: gaji pokok Rp3.000.000 + tunjangan tetap Rp600.000, uang makan Rp25.000 per hari hadir, hadir 22 hari, alpa 1 hari. Potongan alpa = Rp3.600.000 ÷ 30 × 1 = Rp120.000. Uang makan = 22 × Rp25.000 = Rp550.000.
11.2 Proses
• Periode dan cut-off bisa diatur (misalnya tanggal 26 sampai 25).
• Status: draft → review → disetujui → dibayar (penandaan manual karena pembayaran hanya dicatat).
• Setelah disetujui, periode dikunci dan slip gaji di-snapshot. Koreksi lewat penyesuaian di periode berikutnya.
• Pro-rata untuk karyawan baru, karyawan keluar, dan pindah penempatan di tengah periode.
• Slip gaji PDF; karyawan melihat slip miliknya di aplikasi mobile.
• Beban gaji dibebankan ke cabang sesuai penempatan dan masuk laba rugi saat periode disetujui.
• Secara default hanya Pemilik, HR, dan Admin Keuangan yang melihat gaji orang lain. Manajer cabang tidak, kecuali diaktifkan pemilik.
11.3 BPJS dan PPh 21
Ditunda ke fase 3, tetapi disiapkan dari awal: komponen punya flag kena pajak dan basis BPJS, serta tarif dan batas disimpan sebagai konfigurasi dengan tanggal berlaku. Aturan (termasuk skema TER PPh 21 menurut PMK 168/2023) harus diverifikasi ke ketentuan terbaru saat diimplementasikan. Fitur ini bisa dinyalakan opsional per perusahaan.
12. Keuangan dan Laba Rugi
Keputusan utama: setiap kejadian bisnis (penjualan, retur, penerimaan barang, biaya, gaji, void) otomatis membuat jurnal berpasangan di belakang layar dengan chart of accounts default yang bisa disesuaikan. Pengguna tidak perlu melihat debit dan kredit. Antarmuka akuntansi (jurnal manual, neraca penuh) ditunda.
• Kas per cabang dan metode bayar (tunai, transfer, QRIS, e-wallet) dipetakan ke akun masing-masing. Rekonsiliasi bank bersifat manual dan opsional.
• Biaya operasional: kategori, per cabang, lampiran bukti, approval berdasarkan nominal, dan template biaya berulang (sewa, listrik).
• Piutang: penjualan kredit opsional, jatuh tempo, pembayaran parsial, aging piutang.
• Penutupan periode: bulan yang ditutup dikunci; koreksi lewat jurnal penyesuaian.
• Biaya pusat: dicatat di cabang pusat; alokasi ke cabang lain ditunda.
Laporan: laba rugi per cabang, konsolidasi, dan perbandingan antar periode; margin per produk dan jasa; arus kas sederhana; aging hutang dan piutang; nilai persediaan. Pelaporan pajak resmi di luar scope.
Contoh jurnal otomatis:
• Penjualan lunas: kas/bank (debit) lawan pendapatan dan PPN keluaran (kredit); HPP (debit) lawan persediaan (kredit).
• DP: kas (debit) lawan uang muka pelanggan (kredit).
• Retur atau void: kebalikan dari jurnal di atas.
13. Modul Pendukung
13.1 Audit log
Append-only, mencatat siapa (user, role, penempatan), apa, kapan (UTC dan zona cabang), dari mana (IP, perangkat, web atau mobile), dan before/after untuk data sensitif. Yang dicatat: perubahan harga, penyesuaian stok, void dan refund, approval, gaji, perubahan role dan permission, koreksi absensi, login, dan aksi super admin saat "lihat sebagai". Tabel dipartisi per bulan dengan masa simpan per perusahaan. Pemilik melihat log perusahaannya; super admin hanya log level platform dan impersonasi.
13.2 Notifikasi
• Kanal: in-app, push mobile (FCM), dan email. WhatsApp ditunda.
• Jenis: approval, stok menipis, transfer perlu diterima, hutang jatuh tempo, jadwal baru atau berubah, pengingat absen, selisih shift, slip gaji terbit, dan langganan hampir habis.
• Penerima ditentukan permission dan scope. Ada preferensi per pengguna, ringkasan harian opsional untuk pemilik, serta dedup dan throttling.
• MVP web memakai polling; realtime (Laravel Reverb) belakangan.
13.3 Dashboard per role
Role
Isi utama
Pemilik
Omzet dan laba (estimasi), perbandingan cabang, tren, kas, hutang jatuh tempo, nilai persediaan, kehadiran hari ini, antrian approval
Manajer cabang
Penjualan hari ini, stok menipis, absensi tim, order jasa berjalan, approval menunggu, shift terbuka
Karyawan
Tombol absen, jadwal, shift dan transaksi saya, komisi bulan ini, slip gaji
Super admin
Tenant aktif, pertumbuhan, pendapatan langganan, tenant mendekati batas paket, kesehatan sistem
Untuk performa, dashboard memakai tabel ringkasan harian per cabang (diperbarui job atau event) plus cache. Laporan bisa diekspor ke PDF dan Excel.
13.4 Pelanggan (CRM ringan)
Data milik perusahaan (lintas cabang) dengan deteksi duplikat berdasarkan nomor HP; pembeli walk-in boleh tanpa data. Berisi kontak, riwayat transaksi, piutang, DP, dan catatan. Pelanggan tidak punya akun. Loyalty ditunda. Kumpulkan data seminimal mungkin dan sediakan anonimisasi atas permintaan.
13.5 Langganan dan billing SaaS
• Paket (misalnya Gratis, Dasar, Pro) dengan batas jumlah cabang, jumlah karyawan, dan fitur opsional lewat feature flag.
• Status tenant: trial → aktif → masa tenggang → read-only → ditangguhkan. Data tidak langsung dihapus dan pemilik tetap bisa mengekspor datanya.
• Pembayaran langganan di MVP berupa konfirmasi manual oleh super admin; gateway otomatis di fase lanjutan.
• Notifikasi otomatis ke pemilik sebelum langganan habis.
13.6 Onboarding dan pengaturan
• Wizard awal: daftar akun → buat perusahaan → cabang pertama (zona waktu) → pilih template role → impor data awal → undang karyawan.
• Impor dan ekspor Excel/CSV untuk produk, stok awal, supplier, dan karyawan, dengan pratinjau dan laporan kesalahan per baris.
• Undangan karyawan lewat email atau tautan aktivasi.
• Pengaturan perusahaan: pajak, pembagi gaji, ambang approval, format nomor nota, serta logo dan identitas di struk dan kartu.
14. Alur Antar Modul
• PO: PO → penerimaan barang → stok bertambah → hutang supplier → HPP.
• Transaksi: penjualan → stok berkurang → kas atau piutang → pendapatan.
• Jasa: jasa selesai → bahan terpotong → pendapatan diakui → komisi masuk gaji.
• Penggajian: absensi → gaji bersih → biaya di laba rugi.
• Laba rugi: pendapatan − HPP − biaya operasional − gaji, per cabang dan konsolidasi.
15. Daftar Permission (draft)
• Transaksi: transaksi.buat, transaksi.diskon (dengan batas), transaksi.void, retur.buat, retur.setujui, nota.lihat, shift.buka, shift.tutup.
• Stok dan PO: stok.transfer, stok.opname, po.buat, po.setujui.
• Keuangan: keuangan.lihat, biaya.input, biaya.setujui, periode.tutup, laporan.konsolidasi.
• SDM: jadwal.lihat, jadwal.atur, shift.kelola, absensi.koreksi, absensi.approve.
• Gaji: gaji.lihat_sendiri, gaji.kelola, gaji.setujui.
• Lainnya: audit.lihat, dashboard.konsolidasi, pelanggan.lihat, pelanggan.kelola, pelanggan.ekspor, impor.jalankan, pengaturan.kelola, langganan.kelola (super admin).
Setiap permission dipasangkan dengan scope: milik sendiri, cabang, atau seluruh perusahaan.
16. Tabel Data Inti (draft)
• Tenant dan akses: companies, branches, employees, employee_assignments, roles, permissions, approvals, audit_logs, subscriptions.
• Katalog dan stok: items, item_prices, stock_movements, stock_transfers, stock_opnames.
• Pembelian: suppliers, purchase_orders, goods_receipts, supplier_bills.
• Transaksi: sales, sales_items, sales_payments, sales_returns, sales_return_items, service_orders, cash_shifts, customers.
• SDM dan gaji: shifts, employee_schedule_patterns, schedule_overrides, holidays, attendances, leave_requests, attendance_statuses, salary_components, payroll_periods, payslips.
• Keuangan: accounts (chart of accounts), journal_entries, expenses, receivables, daily_branch_summaries.
17. Scope dan Fase Pengerjaan
Fase 1 (MVP)
Tenant, autentikasi, RBAC custom, cabang, katalog dan harga per cabang, stok (transfer dan opname), PO dan penerimaan barang, kasir/transaksi (void, retur, shift), mesin approval, laporan dasar, audit log, notifikasi in-app, onboarding dan impor, serta langganan dengan konfirmasi manual.
Fase 2
SDM: penempatan, jadwal dan roster, absensi (QR dinamis, kartu cadangan), kartu karyawan, dan penggajian berbasis absensi. Dashboard per role yang lebih lengkap.
Fase 3
Laba rugi lengkap, hutang dan piutang, antarmuka laporan keuangan yang diperluas, BPJS dan PPh 21, billing otomatis.
Di luar scope (ditunda)
Mode offline kasir, payment gateway, loyalty dan poin, notifikasi WhatsApp, promo otomatis, pencocokan wajah otomatis, jurnal manual dan neraca penuh, alokasi biaya pusat ke cabang, tukar shift, rekonsiliasi bank otomatis, dan pelaporan pajak resmi.
18. Risiko
• Kebocoran data antar tenant: risiko terbesar. Mitigasi: global scope, tes isolasi otomatis, dan code review khusus untuk query.
• Kompleksitas jurnal otomatis: mulai dari beberapa kejadian utama, lalu perluas bertahap.
• Edge case jadwal dan alpa: lintas hari, zona waktu, dan pindah penempatan perlu skenario uji khusus.
• Privasi data absensi (selfie dan lokasi): persetujuan, minimisasi, dan masa simpan.
• Aturan pajak dan BPJS berubah: simpan sebagai konfigurasi berlaku-tanggal dan verifikasi saat implementasi.
• Scope besar untuk proyek akademik: disiplin pada fase dan MVP.
19. Ukuran Keberhasilan (usulan)
• Tes isolasi tenant lulus 100% sebelum rilis.
• Satu perusahaan bisa menyelesaikan onboarding sampai transaksi pertama tanpa bantuan manual.
• Selisih stok dan kas per shift dapat ditelusuri sampai ke mutasi atau transaksi penyebabnya.
• Laporan laba rugi konsolidasi cocok dengan jumlah laporan per cabang.
• Jumlah kasus titip absen yang lolos mendekati nol (diukur dari tinjauan manajer).

Primary: Emerald #047857

Background: Putih #FFFFFF

Surface: Abu-abu sangat muda #F9FAFB

Text: Abu-abu gelap #111827

Success: Hijau #16A34A

Warning: Amber #D97706

Danger: Merah #DC2626