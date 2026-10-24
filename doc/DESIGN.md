# Design System — Aurevant PKK

Versi: 1.0  
Status: source of truth untuk polish UI web  
Platform: web desktop dan mobile  
Mode: light only

## 1. Arah Visual

**Calm green premium.**

Aurevant PKK adalah ruang kerja operasional, bukan landing page. UI harus terasa:

- tenang, rapi, dan dapat dipercaya;
- padat informasi tanpa terasa sesak;
- premium melalui hierarchy, spacing, dan detail, bukan dekorasi berlebihan;
- konsisten di seluruh halaman;
- ramah untuk pemakaian harian dalam waktu lama.

Kata kunci: **calm, clear, operational, precise, warm premium**.

Yang dihindari:

- navy/blue sebagai warna utama;
- gradient ramai dan shadow berlebihan;
- glassmorphism di seluruh permukaan;
- ikon berbeda-beda dari satu halaman ke halaman lain;
- layout landing page di dalam aplikasi;
- warna status yang hanya mengandalkan warna tanpa teks.

## 2. Prinsip

1. **Operational clarity** — informasi terpenting terlihat lebih dulu.
2. **One system** — semua halaman memakai token, komponen, dan pola yang sama.
3. **Quiet premium** — kualitas terasa dari tipografi, spacing, border, dan transisi halus.
4. **Accessible by default** — kontras, fokus, label, dan touch target bukan tambahan.
5. **Progressive disclosure** — detail tambahan muncul saat dibutuhkan, bukan sekaligus.
6. **Stable, not flashy** — animasi membantu orientasi, bukan menarik perhatian.

## 3. Warna

### 3.1 Primitive palette

| Token | Nilai | Penggunaan |
|---|---:|---|
| `primary` | `#005d42` | aksi utama, teks aktif, brand |
| `primary-container` | `#0b6b4f` | tombol utama, panel brand |
| `primary-hover` | `#004a35` | hover aksi utama |
| `primary-ink` | `#003d2b` | pressed state, teks sangat kuat |
| `primary-soft` | `#e7f2ed` | background lembut, icon tile |
| `secondary` | `#52635b` | teks sekunder |
| `surface` | `#f6f8f7` | background halaman |
| `surface-low` | `#edf2ef` | kontrol sekunder, header tabel |
| `surface-raised` | `#fbfcfb` | panel lembut di atas card |
| `card` | `#ffffff` | permukaan utama |
| `ink` | `#14201b` | teks utama |
| `outline` | `#d7e0db` | border default |
| `outline-strong` | `#b8c6bf` | hover border, pemisah kuat |

### 3.2 Status palette

| Status | Aksen | Teks | Background |
|---|---:|---:|---:|
| Success | `#15803d` | `#116437` | `#e7f6ec` |
| Danger | `#b42318` | `#b42318` | `#fde8e6` |
| Warning | `#b45309` | `#8a4b06` | `#fef3c7` |
| Info | `#1d4ed8` | `#1e40af` | `#e8efff` |

Aturan:

- teks normal harus memiliki kontras minimal **4.5:1**;
- teks besar minimal **3:1**;
- border dan icon minimal **3:1** terhadap background;
- status wajib disertai label teks, bukan warna saja;
- gunakan `outline` untuk batas netral dan status background untuk penekanan.

### 3.3 Background halaman

Gunakan base `surface` dengan ambient glow sangat halus:

```css
background-image:
  radial-gradient(70rem 42rem at 108% -12%, rgb(11 107 79 / 0.075), transparent 62%),
  linear-gradient(180deg, #f8faf9 0%, #f6f8f7 42%, #f3f6f4 100%);
```

Efek harus terasa sebagai pencahayaan, bukan pola yang mencolok.

## 4. Tipografi

### 4.1 Font

| Peran | Font | Fallback |
|---|---|---|
| Display / heading | Plus Jakarta Sans | Inter, system-ui, sans-serif |
| Body / UI | Inter | system-ui, -apple-system, Segoe UI, sans-serif |
| Icon | Material Symbols Outlined | — |

Font dimuat dari `client/index.html`. Jangan menambah font family baru tanpa memperbarui dokumen ini.

### 4.2 Type scale

| Token | Size | Line height | Penggunaan |
|---|---:|---:|---|
| `text-3xs` | 10px | 14px | badge kecil, label teknis |
| `text-2xs` | 11px | 16px | eyebrow, microcopy, label tabel |
| `text-xs` | 12px | 16px | metadata, hint |
| `text-sm` | 14px | 20px | body utama, form, tombol |
| `text-base` | 16px | 24px | body besar |
| `text-lg` | 18px | 28px | subjudul |
| `text-xl` | 20px | 28px | judul card besar |
| `text-2xl` | 24px | 32px | judul halaman mobile |
| `text-3xl` | 30px | 36px | hero dashboard |

Aturan:

- Heading memakai `font-display`, body memakai `font-sans`.
- Judul halaman: `font-bold`, tracking negatif tipis (`-0.02em`).
- Label form: 13px, semibold.
- Eyebrow: 11px, uppercase, tracking `0.1em`, maksimal 2 baris.
- Jangan memakai teks 10px untuk informasi penting.
- Angka finansial memakai `tabular-nums`.
- Hindari `truncate` pada judul halaman; gunakan wrapping.

## 5. Spacing dan Layout

Basis spacing: **4px**.

| Kebutuhan | Nilai |
|---|---:|
| gap antar elemen kecil | 8px |
| gap antar field | 12–16px |
| padding card | 16–20px |
| padding halaman mobile | 16px |
| padding halaman desktop | 32px |
| gap antar section | 20–24px |
| sidebar desktop | 272px |
| header desktop | 60–64px |
| content max width | 1560px |

Aturan:

- Mobile: satu kolom.
- Tablet: 2 kolom untuk kartu stat.
- Desktop: 3–4 kolom untuk stat, 2 kolom untuk form besar.
- Tabel lebar harus punya horizontal scroll yang jelas.
- Jangan membuat card di dalam card tanpa alasan hierarchy.

## 6. Radius, Border, Elevation

### 6.1 Radius

| Token | Nilai | Penggunaan |
|---|---:|---|
| `rounded-lg` | 8px | elemen kecil |
| `rounded-xl` | 12px | tombol, input, menu |
| `rounded-2xl` | 16px | card, panel, modal |
| `rounded-3xl` | 24px | hero, auth panel |

### 6.2 Border

- Default: `1px solid outline/70`.
- Hover: `outline-strong`.
- Fokus: border primary + ring.
- Jangan memakai border hitam pekat.

### 6.3 Shadow

| Token | Nilai | Penggunaan |
|---|---|---|
| `shadow-card` | `0 1px 2px rgb(20 32 27 / 0.04), 0 18px 40px -30px rgb(20 32 27 / 0.22)` | card umum |
| `shadow-lifted` | `0 2px 4px rgb(20 32 27 / 0.05), 0 24px 56px -26px rgb(20 32 27 / 0.28)` | card hover |
| `shadow-float` | `0 12px 24px -12px rgb(20 32 27 / 0.18), 0 42px 80px -32px rgb(20 32 27 / 0.32)` | modal, bottom nav |
| `shadow-sidebar` | `18px 0 50px -36px rgb(0 61 43 / 0.5)` | sidebar |

Elevation hanya untuk memisahkan lapisan, bukan dekorasi.

## 7. Komponen

### 7.1 Button

Varian: `primary`, `secondary`, `danger`, `ghost`, `subtle`.

| Size | Min height | Padding | Teks |
|---|---:|---|---|
| `sm` | 32px | 12px | 12px |
| `md` | 40px | 16px | 14px |
| `lg` | 44px | 20px | 14px |

Aturan:

- Satu primary action per area.
- Gunakan `block` hanya untuk CTA form atau mobile.
- Loading: spinner + `aria-busy`.
- Disabled: opacity 55%, cursor not-allowed.
- Focus: ring 2px primary/55, offset 2px.

### 7.2 Input dan Field

- Tinggi minimum 44px.
- Radius 12px.
- Border default `outline/90`.
- Hover border `outline-strong`.
- Fokus: border primary + ring 2px primary/30.
- Error: border danger + ring danger/20.
- Placeholder: secondary/65.
- Label selalu terlihat; placeholder bukan pengganti label.
- Hint dan error memakai `aria-describedby`.
- Password memakai tombol visibility berlabel.

### 7.3 Card

- Radius 16px.
- Border `outline/70`.
- Background `card`.
- Shadow `shadow-card`.
- Padding default 16px mobile, 20px desktop.
- Header memakai border-bottom tipis.
- `interactive` hanya untuk card yang benar-benar bisa diklik.
- Jangan memakai `!p-3`; gunakan prop `padding`.

### 7.4 Stat

- Label uppercase 11px, secondary.
- Value 24px, bold, tabular.
- Icon tile 40px, radius 12px.
- Hint 12px.
- Tone: default, positive, negative, warning.
- Hover hanya untuk feedback halus, bukan animasi berlebihan.

### 7.5 Badge

- Radius full.
- Tinggi visual 20–22px.
- Teks 11px semibold.
- Selalu punya label.
- Dot opsional untuk status aktif.
- Gunakan token status, bukan raw Tailwind color.

### 7.6 Table

- Wrapper radius 16px, border, `shadow-card`.
- Header 11px uppercase, background surface-low.
- Header sticky.
- Row height nyaman, minimal 44px.
- Hover row halus.
- Angka rata kanan bila berupa nominal.
- Selalu sediakan loading skeleton, empty state, dan horizontal overflow.
- Caption tersedia untuk pembaca layar.

### 7.7 Modal

- Scrim `#0b1712/45` + blur 2px.
- Panel radius 16–24px.
- Mobile: bottom sheet dengan handle.
- Desktop: centered.
- Wajib: focus trap, Escape, scroll lock, restore focus.
- Footer memakai background surface-low/50.
- Modal custom `z-20` tidak diperbolehkan.

### 7.8 Navigasi

- Sidebar desktop 272px, gradient hijau gelap.
- Item aktif memakai background putih transparan dan indikator kiri.
- Microcopy sidebar minimal opacity 75%.
- Mobile: bottom nav mengambang, maksimal 5 item.
- Header: sticky, glass halus, tinggi 60–64px.
- Skip link wajib tersedia.

### 7.9 Page Header

- Eyebrow opsional.
- Judul memakai wrapping.
- Subtitle maksimal 2 baris.
- Actions di kanan pada desktop, di bawah pada mobile.
- Gunakan `AppPageHeader` sebagai default.

### 7.10 Empty, Loading, Error

- Loading memakai skeleton, bukan teks “Memuat…”.
- Empty state memakai icon tile, judul, deskripsi, dan CTA bila relevan.
- Error memakai banner dengan bahasa manusia dan langkah berikutnya.
- Jangan menampilkan error mentah dari API.

## 8. Interaksi dan Motion

| Durasi | Penggunaan |
|---:|---|
| 120ms | hover warna |
| 160ms | fade, page transition |
| 200ms | card lift, modal |
| 220ms | page enter |

Easing default: `cubic-bezier(0.22, 1, 0.36, 1)`.

Aturan:

- Hover lift maksimal 2px.
- Jangan animate width/height kecuali perlu.
- Hormati `prefers-reduced-motion`.
- Fokus keyboard tidak boleh bergantung pada animasi.

## 9. Aksesibilitas

- Kontras teks minimal 4.5:1.
- Touch target minimal 44x44px.
- Semua icon-only button punya `aria-label`.
- Semua field punya label.
- Baris/card clickable harus berupa link/button atau punya role + keyboard handler.
- Modal harus menahan fokus dan mengembalikan fokus.
- Tab harus punya relasi tablist/tab/tabpanel.
- Status tidak boleh disampaikan hanya dengan warna.
- Gunakan `aria-live` untuk feedback penting.
- Tabel memakai caption atau heading yang jelas.

## 10. Pola Halaman

### Dashboard

1. Hero perusahaan + status + periode.
2. Quick actions.
3. Stat finansial.
4. Stat penjualan.
5. Perlu perhatian, kehadiran, karyawan.
6. Payroll.

### List / Index

1. Page header + primary action.
2. Filter bar.
3. Summary stat bila relevan.
4. Table/card list.
5. Pagination.
6. Empty/loading state.

### Detail

1. Page header + back action.
2. Status dan metadata.
3. Konten utama 2 kolom di desktop.
4. Action bar yang jelas.
5. Riwayat/aktivitas.

### Form

1. Page header.
2. Section dengan label.
3. Field full-width di mobile.
4. Error di dekat field.
5. Sticky footer action di mobile bila form panjang.

### Auth

1. Brand panel kiri di desktop.
2. Form fokus di kanan.
3. Tab login/register.
4. Error dan success banner.
5. Demo credential hanya jika memang bagian dari demo environment.

## 11. Do / Don't

Do:

- gunakan token;
- gunakan `App*` components;
- gunakan icon Material Symbols;
- jaga satu primary action;
- uji keyboard dan mobile.

Don't:

- menambah library UI baru;
- memakai raw hex di halaman;
- membuat modal custom;
- memakai `outline-none` tanpa focus ring pengganti;
- memakai teks 10px untuk informasi penting;
- menambahkan dark mode setengah jadi.

## 12. Implementation Map

| Kebutuhan | Lokasi |
|---|---|
| Token warna, shadow, font | `client/src/assets/main.css` |
| Font dan Material Symbols | `client/index.html` |
| Button | `client/src/components/ui/AppButton.vue` |
| Card | `client/src/components/ui/AppCard.vue` |
| Stat | `client/src/components/ui/AppStat.vue` |
| Input | `client/src/components/ui/AppInput.vue` |
| Select | `client/src/components/ui/AppSelect.vue` |
| Search | `client/src/components/ui/AppSearch.vue` |
| Table | `client/src/components/ui/AppTable.vue` |
| Modal | `client/src/components/ui/AppModal.vue` |
| Page header | `client/src/components/ui/AppPageHeader.vue` |
| Navigasi | `client/src/App.vue` |
| Auth | `client/src/components/AuthLayout.vue` |

## 13. Governance

- Setiap perubahan visual harus mengikuti dokumen ini.
- Jika butuh token baru, tambahkan di `main.css` dan dokumentasikan di sini.
- Jangan menambah hardcoded color baru di view.
- Komponen baru harus punya state: default, hover, focus, disabled, loading, error.
- Perubahan besar harus melalui phased plan di `doc/ui-polish-plan.md`.
