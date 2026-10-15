# Aplikasi Android — Aurevant PKK

Acuan API: `server/routes/api.php` (prefix `/api/v1`). Kontrak `{status, message, data, errors}`.

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

## Fitur

| Layar | Endpoint |
|---|---|
| Login / Register | `POST /auth/login`, `POST /auth/register` |
| Pilih Perusahaan | `GET /companies` |
| Dashboard | `GET .../dashboard`, `GET .../access/my-permissions` |
| Absensi | `GET .../branches`, `GET .../attendance-qr`, `POST .../clock-in/out` (multipart + swafoto), `GET .../attendance/me` |
| Kasir | `POST .../shifts` + `/close`, `GET .../items` + `.../prices`, `POST .../sales`, `GET .../sales/{id}/receipt` (PDF, cetak via PrintManager) |
| Persetujuan | `GET .../approvals`, `POST .../approve`, `POST .../reject` |
| Notifikasi | `GET /notifications`, `POST .../read`, `POST .../read-all` |
| Cuti / Slip / Kartu | `POST .../users/{id}/leaves`, `GET .../slip`, `GET .../users/{id}/card` |

## Aturan absensi (sesuai spek)

- QR cabang dinamis dipindai dari layar cabang (ML Kit, di `AttendanceRoute`).
- Satu akun satu perangkat: ID dipakai dari `ANDROID_ID`; ganti HP → minta reset ke HR.
- Lokasi dari GPS saat absen (`FusedLocationProviderClient`); geofence final tetap divalidasi server.
- Swafoto via kamera sistem saat clock-in (wajib), disimpan sebagai `File` lalu `multipart`.
- Waktu absen memakai jam server (respons API), bukan jam HP.

## Yang belum (butuh perangkat/layanan sungguhan)

- Cetak thermal Bluetooth (ganti PrintManager → printer ESC/POS).
- FCM push (tanpa `google-services.json`, `device-tokens` belum didaftarkan dari aplikasi).
- Deteksi lokasi palsu + Play Integrity (antarmuka siap, implementasi menyusul).
