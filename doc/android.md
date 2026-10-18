# Aplikasi Android — Aurevant PKK

Acuan API: `server/routes/api.php` (prefix `/api/v1`). Kontrak `{status, message, data, errors}`.

> Status layar: **fondasi (auth, absensi QR, kasir, persetujuan) + 7 layar
> role karyawan** — Nota, Order Jasa, Gudang, Tim, Keuangan, Penggajian,
> Pelanggan. Menu bawah + menu ⋮ disaring per izin
> (`domain/usecase/MenuPolicy.kt`, cermin `doc/menu-per-role.md`).
> Belum diuji di emulator/perangkat — verifikasi: code review + unit test
> JVM (`CartCalculatorTest`, `MenuPolicyTest`). Build APK belakangan.

## Cara jalan

1. Nyalakan backend Laravel (`php artisan serve --host=0.0.0.0`, MySQL jalan).
2. Emulator: `BASE_URL` di `di/NetworkModule.kt` sudah `http://10.0.2.2:8000/api/v1/`
   (loopback host). HP fisik: ganti ke `http://<IP-LAN>:8000/api/v1/`.
3. Build: `JAVA_HOME="<Android Studio>/jbr" ./gradlew :app:assembleDebug`.

## Struktur (MVVM + Clean 3 lapis, sesuai RULES.md §3)

```
ui/features/<fitur>/ → <Fitur>Screen (dumb), <Fitur>Route (smart), <Fitur>ViewModel
domain/               → model, interface repository, usecase (satu file satu aksi)
data/                 → DTO, mapper, repository impl, Retrofit API
di/                   → modul Hilt (NetworkModule, RepoModule)
core/                 → network (envelope, interceptor, sesi), ui (komponen)
```

Alur UDF: event → `UiAction` (sealed) → `ViewModel.handleAction()` →
`UiState` (`StateFlow`) → Compose re-render.

## Layar per role (cermin `doc/menu-per-role.md`)

| Layar (route) | Role utama | Syarat tampil |
|---|---|---|
| Dasbor (`dashboard`) | semua | selalu (+ shortcut sesuai izin) |
| Absensi (`attendance`) | semua | selalu (QR dinamis + GPS + swafoto + 1 akun 1 perangkat) |
| Kasir (`pos`) | Kasir, Manajer | `transaksi.buat` |
| Nota (`nota`) | Kasir, Manajer | `nota.lihat`; void langsung butuh `transaksi.void`, selebihnya diajukan |
| Order Jasa (`jasa`) | Kasir, Pelaksana | `transaksi.buat` (buat/DP/bayar/batal) / `jasa.proses` (proses/selesai) |
| Gudang (`gudang`) | Staf Gudang, Manajer | `stok.transfer` / `stok.opname` / `po.buat` (tab Stok/Transfer/Opname) |
| Persetujuan (`approvals`) | pengaju + pemutus | bisa mengajukan (`transaksi.buat`, `retur.buat`, `po.buat`, `stok.*`) |
| Tim (`tim`) | HR, Manajer | `absensi.approve` / `absensi.koreksi` / `gaji.kelola` |
| Keuangan (`keuangan`) | Admin Keuangan | `keuangan.lihat` / `biaya.input` (biaya, hutang, piutang, laba rugi) |
| Penggajian (`gaji`) | HR, Keuangan | `gaji.kelola` (hitung → review → setujui → bayar; setujui butuh `gaji.setujui`) |
| Notifikasi, Cuti saya, Slip saya, Kartu, Pelanggan | semua / sesuai izin | selalu / `pelanggan.lihat` |

Menu bawah maks 5 tab (Dasbor + Absensi + prioritas role); sisanya di menu ⋮.

## Fitur → endpoint

| Layar | Endpoint |
|---|---|
| Login / Register | `POST /auth/login`, `POST /auth/register` |
| Pilih Perusahaan | `GET /companies` |
| Dashboard | `GET .../dashboard`, `GET .../access/my-permissions` |
| Absensi | `GET .../branches`, `GET .../attendance-qr`, `POST .../clock-in/out` (multipart + swafoto), `GET .../attendance/me` |
| Kasir | `POST .../shifts` + `/close`, `GET .../items` + `.../prices`, `POST .../sales`, struk PDF dibuat lokal di HP (`PdfDocument`, tanpa unduh) → cetak via PrintManager (web memakai `GET .../sales/{sale}/receipt`) |
| Nota | `GET .../sales`, `GET .../sales/{id}`, `POST .../void`, `POST .../approvals` (pengajuan void/retur) |
| Order Jasa | `GET/POST .../service-orders`, `.../deposit`, `.../process`, `.../complete`, `.../pay`, `.../cancel` |
| Gudang | `GET .../stock`, `GET/POST .../stock/movements`, `GET/POST .../stock-transfers` + `/submit/approve/ship/receive/cancel`, `GET/POST .../stock/opnames` + `/submit/approve/reject` |
| Tim | `GET .../branches/{id}/attendance`, `GET .../leaves?status=diajukan`, `POST .../leaves/{id}/decide`, `PUT .../attendances/{id}`, `DELETE .../users/{id}/device`, `GET .../users/{id}/schedule` |
| Keuangan | `GET .../expense-categories`, `GET/POST .../expenses` + `/approve/reject`, `GET .../supplier-bills` + `.../pay`, `GET .../receivables` + `.../pay`, `GET .../reports/profit-loss` |
| Penggajian | `GET .../payroll-periods`, `POST .../calculate/review/approve/pay` |
| Persetujuan | `GET .../approvals`, `POST .../approve`, `POST .../reject` |
| Notifikasi | `GET /notifications`, `POST .../read`, `POST .../read-all` |
| Cuti / Slip / Kartu / Pelanggan | `POST .../users/{id}/leaves`, `GET .../slip`, `GET .../users/{id}/card`, `GET .../customers` |

## Aturan absensi (sesuai spek)

- QR cabang dinamis dipindai dari layar cabang (ML Kit, di `AttendanceRoute`).
- Satu akun satu perangkat: ID dipakai dari `ANDROID_ID`; ganti HP → minta reset ke HR (layar Tim).
- Lokasi dari GPS saat absen (`FusedLocationProviderClient`); geofence final tetap divalidasi server.
- Swafoto via kamera sistem saat clock-in (wajib), disimpan sebagai `File` lalu `multipart`.
- Waktu absen memakai jam server (respons API), bukan jam HP.

## Batasan jujur (mobile v1)

- **Retur barang** di HP hanya **pengajuan** (`sale.refund` + alasan);
  eksekusi akhir oleh manajer di web — API tidak membuka ID baris nota.
- Cetak thermal Bluetooth (ganti PrintManager → printer ESC/POS); struk PDF digambar lokal di HP (instan, tanpa jaringan), endpoint web `GET .../receipt` tetap ada untuk web.
- FCM push (tanpa `google-services.json`, `device-tokens` belum didaftarkan dari aplikasi).
- Deteksi lokasi palsu + Play Integrity (antarmuka siap, implementasi menyusul).
- Keuangan web penuh (jurnal, neraca, tutup periode, impor) tetap di web.
