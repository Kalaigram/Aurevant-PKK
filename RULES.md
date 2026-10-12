# Rules — Aurevant PKK

Aturan arsitektur & clean code untuk semua pengerjaan fitur.
Acuan spek: `doc/spek.md`. Progres: `PROGRESS.md`.

## 0. Prinsip umum (semua stack)

- Dependensi hanya menunjuk ke dalam: UI → use case → domain. Domain tidak boleh
  tahu framework (Eloquent, Retrofit, Pinia).
- Satu fitur = satu folder vertikal (`kasir`, `stok`, `absensi`, `gaji`).
  Jangan tambah file ke folder global kecuali benar-benar lintas fitur.
- Satu class/fungsi = satu aksi (`CreateSale`, `ApproveVoid`, `GetTodos`).
- Nama permission sama di semua sisi: `transaksi.void`, `stok.opname`, dst.
- Kontrak API: `{status, message, data, errors}`. Nomor nota & idempotency key
  dibuat di server, tidak pernah dari klien.
- Key di dalam `data` pakai camelCase (`branchId`, `trackStock`), ikut gaya
  frontend. Jangan pakai snake_case di respons baru.
- Kredensial asli hanya di `.env` lokal. Contoh di `.env.example` saja.

## 1. Vue (`client/`) — feature-based modular

```
src/
  features/<fitur>/ → <Fitur>View.vue, <fitur>.store.ts, <fitur>.api.ts, <fitur>.types.ts
  shared/           → components/ (atom), composables/, utils/
  core/             → api-client.ts, auth/, router/
  stores/           → hanya state global (user, tenant aktif)
```

- Composition API + `<script setup>` + TypeScript strict. Tanpa Options API di kode baru.
- Satu Pinia store per fitur. Dilarang satu store raksasa.
- Logika di `composables/`, bukan di komponen. Komponen hanya render + event.
- `ref` untuk primitif, `reactive` untuk objek; destructure reactive pakai `toRefs`.
- Expose state pakai `readonly`. Jangan mutasi state store langsung dari komponen.
- `npm run lint` dan `npm run format` harus bersih sebelum commit.

## 2. Laravel (`server/`) — 4 lapis

```
app/
  Domain/         → Entities, ValueObjects, interface Repository, Events (tanpa Laravel)
  Application/    → UseCases + DTOs (satu class satu aksi, kembalikan DTO)
  Infrastructure/ → Eloquent, Sanctum, FCM, queue (implementasi interface Domain)
  Http/           → Controllers tipis: validasi → panggil use case → format respons
  Shared/         → ApiResponse (format {status, message, data, errors})
```

- Controller dilarang query Eloquent langsung. Query hanya di repository.
- Signature controller WAJIB menyertakan semua param model dari route
  (`Company $company`, dst.). Param skalar tanpa model di depannya terisi
  secara posisional dan salah (pernah: `{type}` terisi id company).
- Binding interface → implementasi di `AppServiceProvider`.
- Aturan bisnis spek (potongan gaji 1/30, moving average HPP, nomor nota atomik)
  tinggal di Domain/Application supaya unit-testable tanpa database.
- Semua query bisnis wajib lewat global scope `company_id`. Tes isolasi tenant
  (A tidak bisa akses data B) wajib lolos sebelum merge.
- Style: `./vendor/bin/pint --test` bersih. Migration selalu ada rollback (`down`).
- Spatie teams: tim = `companies.id` (kolom `company_id`), di-set otomatis oleh
  middleware `company.team` dari route `{company}`. Scope cabang vs perusahaan
  dibaca dari penempatan (`branch_id` null = seluruh perusahaan).
- Endpoint mutasi wajib `permission:...` (middleware spatie, team-aware).
  Endpoint baca cukup auth. Scope cabang: middleware `scope.cabang` untuk
  `{branch}`/`branch_id`, cek manual via `Access::` untuk resource termuat
  (transfer: sisi relevan; show: salah satu sisi cukup).
- Di test: setiap ganti team atau ubah role, refresh model user dulu
  (`$user->fresh()`) karena spatie memoisasi relasi. Trait `WithPermissions::owner()`
  memberi semua izin untuk user test.
- Assert skalar pakai `assertSame`, bukan `assertEquals` — `assertEquals(0, null)`
  lolos semu dan menyembunyikan bug (pernah kejadian di unread-count).
- Cek guest (401) harus sebelum `actingAs` pertama di test itu.

### Pembagian peran (controller tetap kurus)

- Repository (`Infrastructure/`) — semua query. Contoh: `CompanyEloquentRepository`.
- UseCase/Service (`Application/`) — orkestrasi satu aksi bisnis.
  Contoh: `ListCompanies`.
- Job + Queue — kerja berat/async: impor Excel, ringkasan harian dashboard,
  kirim push FCM.
- Event + Listener — efek samping: audit log (wajib via event + queue sesuai
  spek), notifikasi approval, stok menipis.
- Policy — otorisasi scope (milik sendiri / cabang / perusahaan), melengkapi
  `spatie/permission`.
- DTO + ApiResponse — transfer data antar lapis + format kontrak.

## 3. Kotlin (`android/`) — MVVM + Clean 3 lapis

```
ui/features/<fitur>/ → <Fitur>Screen (dumb), <Fitur>Route (smart), <Fitur>ViewModel
domain/               → model, interface repository, usecase (satu file satu aksi)
data/                 → DTO, mapper, repository impl, Retrofit API
di/                   → modul Hilt
```

- Alur UDF: event → `UiAction` (sealed) → `ViewModel.handleAction()` →
  `UiState` → Compose re-render. Tanpa callback hell antar layar.
- Composable "smart" (pegang ViewModel) vs "dumb" (cuma render, previewable).
- State via `StateFlow` + `collectAsStateWithLifecycle`. Aturan absensi
  (QR, geofence, Play Integrity) di `data`, logikanya di `domain`.
- Ikuti Kotlin Coding Conventions. Fungsi composable kecil dan bernama jelas.

## 4. Workflow fitur (wajib)

- Satu fitur = satu branch: `feat/<nama-fitur>` dari `main`. Jangan campur fitur
  dalam satu branch/commit (contoh yang benar: commit server & android dipisah).
- Commit jelas per langkah: `feat:`, `fix:`, `chore:`, `docs:`.
  Contoh: `feat: tambah usecase ApproveVoid`, bukan `update`.
- Setiap selesai fitur: centang `[ ]` → `[x]` di `PROGRESS.md` dalam commit yang sama.
- File sensitif (`.env`, keystore, `google-services.json`) tidak boleh
  lolos commit — cek `git status` sebelum commit.
- Prinsip YAGNI: service, job, event, policy baru dibuat saat ada fitur yang
  butuh, bukan di depan.

## 5. Alur UI (Stitch, selalu belakangan)
- UI dikerjakan setelah API selesai dan terverifikasi. Jangan bikin halaman dulu.
- Setiap API selesai: saya siapkan daftar halaman apa saja + prompt siap-pakai
  untuk di-generate di Stitch.
- Isi prompt: struktur halaman, komponen, state, endpoint yang dipakai,
  permission yang berlaku, dan format data {status, message, data(, meta), errors}.

## 6. Kesiapan scaling / load balancer (nanti, saat deployment)

- LB tidak dipasang selama masih satu instance lokal. Yang dijaga dari sekarang:
  aplikasi harus stateless dan siap di-balance.
- Syarat yang sudah terpenuhi: auth token Sanctum (tanpa sticky session),
  health check `GET /up` untuk probe LB.
- Syarat sebelum naik ke 2+ instance: `CACHE_STORE`, `SESSION_DRIVER`, dan
  `QUEUE_CONNECTION` pindah ke Redis; upload file pindah ke storage bersama
  (S3/minio), bukan disk lokal.
- LB-nya sendiri (Nginx upstream / LB cloud) dipasang saat deployment, tidak
  di repo ini.

## 7. Bahasa UI (manusiawi, bukan teknis)

- Semua teks yang dibaca user (judul, label, tombol, error, empty state) harus
  bahasa manusia yang wajar: kalimat lengkap, sopan, tanpa istilah teknis
  (tanpa: ID/entitas/payload/token/endpoint/HTTP/422/due/Sanctum/dll).
  Pengecualian: istilah bisnis yang memang dipakai user (nota, shift, opname,
  PO, DP, BPJS, PPh) boleh dipakai apa adanya.
- ID/nomor teknis (ID nota, UUID, token) boleh tampil sebagai data, tapi label
  dan penjelasnya harus manusiawi ("Nomor nota", bukan "entity_id").
- Pesan error validasi tampil apa adanya dari server (`errors`), tapi kalimat
  tambahan di sekitarnya harus manusiawi. Jangan menampilkan kode HTTP mentah
  ke user (401/403/422/500 diterjemahkan jadi kalimat).
- Menu dan tombol aksi disesuaikan ke izin user (lihat §8): yang tidak boleh
  dipakai disembunyikan, bukan sekadar menunggu 403.
- Nama permission mentah (`transaksi.void`, `izin: keuangan.lihat`) dilarang
  muncul di UI. Peta izin → label manusiawi tinggal di frontend.

## 8. Menu & halaman per role (frontend)

- Frontend wajib tahu izin user: `GET /companies/{company}/access/my-permissions`
  → `{permissions: [...]}`. Di-cache per perusahaan aktif, di-refresh saat
  ganti perusahaan / login ulang.
- Setiap item menu mendeklarasikan syarat izinnya; tanpa izin itu menu
  disembunyikan. Halaman global (dashboard, notifikasi, absensi, cuti, slip,
  kartu, langganan, profil) tampil untuk semua role.
- Tombol aksi dalam halaman ikut izin yang sama (setujui/tolak, hapus, bayar).
  Aturan tampil = aturan backend (middleware `permission:...`), jangan longgar,
  jangan ketat sendiri.
