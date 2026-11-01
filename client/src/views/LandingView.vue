<script setup lang="ts">
import { onBeforeMount, onBeforeUnmount, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import AppIcon from '../components/ui/AppIcon.vue'

const root = ref<HTMLElement | null>(null)
const menuOpen = ref(false)
const scrolled = ref(false)
const year = new Date().getFullYear()
let observer: IntersectionObserver | undefined

const navItems = [
  { label: 'Alur', href: '#alur' },
  { label: 'Modul', href: '#modul' },
  { label: 'Peran', href: '#peran' },
  { label: 'Android', href: '#mobile' },
  { label: 'Kontrol', href: '#kontrol' },
]

const saleFlow = [
  {
    no: '01',
    label: 'Nota',
    title: 'Jasa servis AC + Freon R32',
    detail: 'Rp235.000, dibayar transfer.',
  },
  {
    no: '02',
    label: 'Stok',
    title: 'Freon R32 berkurang 1',
    detail: 'Stok cabang Cibubur: 5 menjadi 4.',
  },
  {
    no: '03',
    label: 'Komisi',
    title: 'Komisi pelaksana dihitung',
    detail: 'Rp15.000 untuk teknisi yang mengerjakan order.',
  },
  {
    no: '04',
    label: 'Jurnal',
    title: 'Kas dan pendapatan tercatat',
    detail: 'Rp235.000 masuk ke jurnal keuangan.',
  },
]

const hrFlow = [
  {
    no: '01',
    label: 'Absensi',
    title: 'QR cabang dan selfie',
    detail: 'Masuk 08:02, lokasi sesuai area cabang.',
  },
  {
    no: '02',
    label: 'Jadwal',
    title: 'Shift 08:00 sampai 17:00',
    detail: 'Pola jadwal cabang berlaku untuk hari ini.',
  },
  {
    no: '03',
    label: 'Payroll',
    title: 'Kehadiran masuk komponen gaji',
    detail: 'Potongan 1/30 dihitung dari komponen gaji.',
  },
  {
    no: '04',
    label: 'Biaya',
    title: 'Beban gaji masuk laba rugi',
    detail: 'Tercatat pada cabang tempat karyawan bertugas.',
  },
]

const modules = [
  {
    no: '01',
    title: 'Operasional cabang',
    detail:
      'POS barang dan jasa, buka dan tutup shift, transfer stok, opname, pembelian, penerimaan, dan harga per cabang.',
  },
  {
    no: '02',
    title: 'Order jasa',
    detail: 'Jadwal teknisi, paket jasa, bahan habis pakai, komisi pelaksana, dan estimasi biaya.',
  },
  {
    no: '03',
    title: 'SDM dan payroll',
    detail:
      'Absensi QR, GPS, dan selfie; jadwal tiga lapis; cuti, lembur, tukar shift; payroll 1/30; slip PDF; kartu karyawan.',
  },
  {
    no: '04',
    title: 'Keuangan',
    detail:
      'Jurnal otomatis, laba rugi per cabang dan konsolidasi, neraca, arus kas, hutang, piutang, aging, tutup periode, ekspor Excel dan PDF.',
  },
  {
    no: '05',
    title: 'Pelanggan dan akses',
    detail: 'Customer 360, role dan scope akses, serta penempatan karyawan di cabang.',
  },
  {
    no: '06',
    title: 'Kontrol',
    detail: 'Isolasi data per perusahaan, 2FA, sesi perangkat, dan audit log.',
  },
]

const roles = [
  {
    name: 'Pemilik',
    work: 'Menyetujui pengeluaran, memantau cabang, dan membaca laporan.',
    access: 'Konsolidasi, laba rugi, audit log.',
  },
  {
    name: 'Manajer cabang',
    work: 'Membuka shift, mengatur stok, jadwal, dan persetujuan cabang.',
    access: 'Operasional cabang yang dipimpin.',
  },
  {
    name: 'HR dan keuangan',
    work: 'Memeriksa absensi, menjalankan payroll, dan menutup periode.',
    access: 'Slip, jurnal, dan laporan.',
  },
  {
    name: 'Karyawan',
    work: 'Absen, mengajukan cuti atau lembur, dan menukar shift.',
    access: 'Jadwal, slip, dan kartu karyawan.',
  },
]

const androidPoints = [
  {
    title: 'QR cabang dan selfie',
    detail: 'QR berubah secara berkala, lokasi dicek, dan selfie dilampirkan.',
  },
  {
    title: 'Jadwal dan tukar shift',
    detail: 'Lihat shift, ajukan cuti atau lembur, dan minta tukar shift.',
  },
  {
    title: 'Slip dan kartu karyawan',
    detail: 'Buka slip PDF dan kartu karyawan dari aplikasi.',
  },
  {
    title: 'Kasir dan persetujuan',
    detail: 'Jalankan POS dan setujui pengajuan dari perangkat.',
  },
  {
    title: 'Notifikasi',
    detail: 'Pengajuan, persetujuan, dan pengingat masuk ke notifikasi.',
  },
]

const controls = [
  { label: 'Perusahaan', value: 'Data terpisah untuk setiap perusahaan.' },
  { label: 'Peran', value: 'Role dan scope menentukan halaman yang bisa dibuka.' },
  { label: 'Login', value: '2FA dan sesi perangkat yang bisa dicabut.' },
  { label: 'Jejak', value: 'Audit log append-only untuk perubahan penting.' },
  { label: 'Ekspor', value: 'Laporan keuangan dan operasional dalam Excel dan PDF.' },
]

const steps = [
  {
    no: '01',
    title: 'Buat akun pengelola',
    detail: 'Daftar dengan email. Akun ini menjadi pintu masuk perusahaan.',
  },
  {
    no: '02',
    title: 'Tambahkan perusahaan dan cabang',
    detail: 'Isi profil perusahaan, buat cabang pertama, dan atur zona waktu.',
  },
  {
    no: '03',
    title: 'Susun tim dan peran',
    detail: 'Buat akun karyawan, pilih template peran, lalu mulai dari alur tersibuk.',
  },
]

const faqs = [
  {
    no: '01',
    q: 'Apakah satu nota bisa berisi barang dan jasa?',
    a: 'Bisa. Katalog memiliki item barang dan jasa, dan satu nota bisa memakai keduanya. Order jasa juga mendukung jadwal, paket, bahan habis pakai, dan komisi pelaksana.',
  },
  {
    no: '02',
    q: 'Bagaimana harga bisa berbeda di setiap cabang?',
    a: 'Harga jual disimpan per item per cabang. Cabang bisa memakai harga berbeda tanpa menduplikasi katalog.',
  },
  {
    no: '03',
    q: 'Apakah absensi harus memakai aplikasi Android?',
    a: 'Absensi utama memakai aplikasi Android dengan QR dinamis cabang, geofence, dan selfie. Kartu karyawan tersedia sebagai jalur cadangan di perangkat cabang.',
  },
  {
    no: '04',
    q: 'Apakah data antar perusahaan terpisah?',
    a: 'Ya. Setiap data terikat ke perusahaan, dan peran serta scope menentukan data yang bisa dibuka.',
  },
  {
    no: '05',
    q: 'Laporan apa yang bisa diekspor?',
    a: 'Laporan keuangan dan operasional bisa diekspor ke Excel dan PDF, termasuk laba rugi, neraca, arus kas, dan laporan konsolidasi.',
  },
  {
    no: '06',
    q: 'Apa yang termasuk Paket Gratis?',
    a: 'Paket Gratis mencakup 1 cabang dan 5 karyawan. Paket Dasar dan Pro menambah kapasitas cabang, karyawan, dan fitur.',
  },
]

function onScroll() {
  scrolled.value = window.scrollY > 12
}

function closeMenu() {
  menuOpen.value = false
}

onBeforeMount(() => {
  document.documentElement.classList.add('landing-ready')
})

onMounted(() => {
  onScroll()
  window.addEventListener('scroll', onScroll, { passive: true })

  if (!('IntersectionObserver' in window)) {
    root.value?.querySelectorAll<HTMLElement>('.landing-reveal').forEach((el) => {
      el.classList.add('is-visible')
    })
    return
  }

  observer = new IntersectionObserver(
    (entries) => {
      for (const entry of entries) {
        if (!entry.isIntersecting) continue
        entry.target.classList.add('is-visible')
        observer?.unobserve(entry.target)
      }
    },
    { threshold: 0.1, rootMargin: '0px 0px -6% 0px' },
  )

  root.value?.querySelectorAll<HTMLElement>('.landing-reveal').forEach((el) => {
    observer?.observe(el)
  })
})

onBeforeUnmount(() => {
  observer?.disconnect()
  window.removeEventListener('scroll', onScroll)
  document.documentElement.classList.remove('landing-ready')
})
</script>

<template>
  <div id="top" ref="root" class="landing-page min-h-screen overflow-x-clip bg-surface text-ink">
    <header
      class="sticky top-0 z-50 border-b transition-[background-color,border-color,box-shadow] duration-200"
      :class="
        scrolled
          ? 'border-outline/70 bg-surface/92 shadow-card backdrop-blur-xl'
          : 'border-transparent bg-surface/75 backdrop-blur-sm'
      "
    >
      <div
        class="mx-auto flex h-[72px] max-w-7xl items-center justify-between gap-4 px-4 sm:px-6 lg:px-8"
      >
        <RouterLink
          to="/"
          class="flex min-h-11 items-center gap-3 rounded-xl focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/55 focus-visible:ring-offset-2"
          aria-label="Aurevant PKK, beranda"
          @click="closeMenu"
        >
          <span
            class="flex h-10 w-10 items-center justify-center rounded-xl bg-primary font-display text-base font-extrabold text-on-primary shadow-card"
          >
            A
          </span>
          <span class="min-w-0">
            <span class="block font-display text-[15px] font-bold tracking-tight text-ink"
              >Aurevant PKK</span
            >
            <span class="block text-2xs text-secondary">Operasional barang &amp; jasa</span>
          </span>
        </RouterLink>

        <nav class="hidden items-center gap-0.5 lg:flex" aria-label="Navigasi beranda">
          <a
            v-for="item in navItems"
            :key="item.href"
            :href="item.href"
            class="flex min-h-11 items-center rounded-xl px-3 text-sm font-medium text-secondary transition-colors hover:bg-surface-low hover:text-ink focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/55"
          >
            {{ item.label }}
          </a>
        </nav>

        <div class="hidden items-center gap-2 lg:flex">
          <RouterLink
            to="/login"
            class="flex min-h-11 items-center rounded-xl px-4 text-sm font-semibold text-ink transition-colors hover:bg-surface-low focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/55"
          >
            Masuk
          </RouterLink>
          <RouterLink
            to="/register"
            class="inline-flex min-h-11 items-center justify-center gap-2 rounded-xl bg-primary-container px-5 text-sm font-semibold text-on-primary shadow-sm transition-[background-color,box-shadow,transform] duration-150 hover:bg-primary-hover hover:shadow-card focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/55 focus-visible:ring-offset-2 active:translate-y-px"
          >
            Buat akun
            <AppIcon name="arrow_forward" :size="17" />
          </RouterLink>
        </div>

        <button
          type="button"
          class="flex h-11 w-11 items-center justify-center rounded-xl border border-outline/70 bg-card text-ink transition hover:border-outline-strong hover:bg-surface-low focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/55 lg:hidden"
          :aria-expanded="menuOpen"
          aria-controls="landing-mobile-menu"
          :aria-label="menuOpen ? 'Tutup menu' : 'Buka menu'"
          @click="menuOpen = !menuOpen"
        >
          <AppIcon :name="menuOpen ? 'close' : 'menu'" :size="22" />
        </button>
      </div>

      <Transition name="landing-menu">
        <div
          v-if="menuOpen"
          id="landing-mobile-menu"
          class="border-t border-outline/60 bg-card/95 px-4 pb-4 pt-3 shadow-float backdrop-blur-xl lg:hidden"
        >
          <nav class="mx-auto grid max-w-7xl gap-1" aria-label="Navigasi beranda mobile">
            <a
              v-for="item in navItems"
              :key="item.href"
              :href="item.href"
              class="flex min-h-11 items-center rounded-xl px-3 text-sm font-medium text-ink transition hover:bg-surface-low focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/55"
              @click="closeMenu"
            >
              {{ item.label }}
            </a>
          </nav>
          <div class="mx-auto mt-3 grid max-w-7xl gap-2 border-t border-outline/60 pt-3">
            <RouterLink
              to="/login"
              class="flex min-h-11 items-center justify-center rounded-xl border border-outline/80 px-4 text-sm font-semibold text-ink transition hover:border-outline-strong hover:bg-surface-low focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/55"
              @click="closeMenu"
            >
              Masuk
            </RouterLink>
            <RouterLink
              to="/register"
              class="flex min-h-11 items-center justify-center gap-2 rounded-xl bg-primary-container px-4 text-sm font-semibold text-on-primary shadow-sm transition hover:bg-primary-hover focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/55 focus-visible:ring-offset-2"
              @click="closeMenu"
            >
              Buat akun
              <AppIcon name="arrow_forward" :size="17" />
            </RouterLink>
          </div>
        </div>
      </Transition>
    </header>

    <main id="konten">
      <section class="landing-hero relative isolate overflow-hidden border-b border-outline/60">
        <div class="landing-ledger pointer-events-none absolute inset-0 -z-10" aria-hidden="true" />

        <div
          class="mx-auto grid max-w-7xl gap-14 px-4 py-16 sm:px-6 lg:grid-cols-[1.04fr_0.96fr] lg:items-center lg:px-8 lg:py-24"
        >
          <div>
            <p
              class="landing-reveal flex items-center gap-3 text-2xs font-semibold uppercase tracking-[0.14em] text-primary"
            >
              <span class="h-px w-8 bg-primary" aria-hidden="true" />
              Aplikasi operasional untuk bisnis barang dan jasa
            </p>
            <h1
              class="landing-reveal mt-5 max-w-2xl font-display text-[2.6rem] font-bold leading-[1.06] tracking-[-0.04em] text-balance text-ink sm:text-5xl lg:text-[3.6rem]"
            >
              Kelola cabang dari nota kasir sampai slip gaji.
            </h1>
            <p
              class="landing-reveal mt-5 max-w-xl text-base leading-relaxed text-secondary sm:text-lg"
            >
              Aurevant PKK mencatat penjualan barang dan jasa, memotong stok cabang, menghitung
              absensi, dan menyusun laporan keuangan. Tim Anda tidak perlu merekap ulang.
            </p>

            <div class="landing-reveal mt-8 flex flex-col gap-3 sm:flex-row sm:items-center">
              <RouterLink
                to="/register"
                class="inline-flex min-h-12 items-center justify-center gap-2 rounded-xl bg-primary-container px-6 text-sm font-semibold text-on-primary shadow-card transition-[background-color,box-shadow,transform] duration-150 hover:bg-primary-hover hover:shadow-lifted focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/55 focus-visible:ring-offset-2 active:translate-y-px"
              >
                Buat akun pengelola
                <AppIcon name="arrow_forward" :size="18" />
              </RouterLink>
              <a
                href="#alur"
                class="inline-flex min-h-12 items-center justify-center gap-2 rounded-xl border border-outline/80 bg-card/70 px-6 text-sm font-semibold text-ink transition hover:border-outline-strong hover:bg-surface-low focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/55 focus-visible:ring-offset-2"
              >
                Lihat alur
                <AppIcon name="south" :size="18" />
              </a>
            </div>

            <ul
              class="landing-reveal mt-8 grid max-w-xl grid-cols-1 gap-x-6 gap-y-2.5 text-xs font-medium text-secondary sm:grid-cols-2"
            >
              <li
                v-for="item in [
                  'POS barang dan jasa',
                  'Stok per cabang',
                  'Absensi QR, GPS, selfie',
                  'Payroll 1/30',
                ]"
                :key="item"
                class="flex items-start gap-2"
              >
                <AppIcon name="check" :size="15" class="mt-0.5 shrink-0 text-primary" />
                {{ item }}
              </li>
            </ul>

            <p class="landing-reveal mt-7 text-xs leading-relaxed text-secondary">
              Paket Gratis tersedia untuk 1 cabang dan 5 karyawan, di web dan Android.
            </p>
          </div>

          <div class="landing-reveal relative mx-auto w-full max-w-md">
            <article
              class="relative overflow-hidden rounded-2xl border border-outline/80 bg-card shadow-float"
              aria-label="Contoh nota penjualan dengan item barang dan jasa, serta catatan stok, komisi, dan jurnal yang ikut berubah."
            >
              <div
                class="flex items-start justify-between gap-4 border-b border-dashed border-outline-strong px-5 pb-4 pt-5 sm:px-6 sm:pt-6"
              >
                <div>
                  <p
                    class="font-mono text-3xs font-semibold uppercase tracking-[0.14em] text-secondary"
                  >
                    Contoh nota
                  </p>
                  <p class="mt-1 font-display text-lg font-bold tracking-tight text-ink">A-1042</p>
                  <p class="mt-1 text-2xs text-secondary">Cabang Cibubur · 14:32</p>
                </div>
                <span
                  class="-rotate-6 rounded-md border-2 border-primary px-2.5 py-1 font-mono text-2xs font-bold uppercase tracking-[0.1em] text-primary"
                >
                  Lunas
                </span>
              </div>

              <div class="px-5 py-5 font-mono text-xs sm:px-6">
                <div class="space-y-3 text-ink">
                  <div class="flex items-baseline justify-between gap-4">
                    <span>Jasa servis AC</span>
                    <span class="shrink-0 tabular-nums">150.000</span>
                  </div>
                  <div class="flex items-baseline justify-between gap-4">
                    <span>Freon R32 × 1</span>
                    <span class="shrink-0 tabular-nums">85.000</span>
                  </div>
                </div>
                <div
                  class="mt-4 flex items-baseline justify-between border-t border-dashed border-outline-strong pt-3 text-sm font-bold text-ink"
                >
                  <span>Total</span>
                  <span class="tabular-nums">Rp235.000</span>
                </div>
              </div>

              <div class="border-t border-outline/60 bg-surface-low/45 px-5 py-4 sm:px-6">
                <p class="text-2xs font-semibold uppercase tracking-[0.12em] text-secondary">
                  Ikut tercatat
                </p>
                <dl class="mt-3 space-y-2.5 text-xs">
                  <div class="flex items-baseline justify-between gap-4">
                    <dt class="text-secondary">Stok Freon R32</dt>
                    <dd class="font-mono font-semibold text-ink">5 → 4</dd>
                  </div>
                  <div class="flex items-baseline justify-between gap-4">
                    <dt class="text-secondary">Komisi teknisi</dt>
                    <dd class="font-mono font-semibold text-ink">Rp15.000</dd>
                  </div>
                  <div class="flex items-baseline justify-between gap-4">
                    <dt class="text-secondary">Kas masuk</dt>
                    <dd class="font-mono font-semibold text-ink">Rp235.000</dd>
                  </div>
                </dl>
              </div>
            </article>
            <p class="mt-4 text-center text-2xs leading-relaxed text-secondary">
              Data ilustrasi. Satu nota memakai katalog barang dan jasa yang sama.
            </p>
          </div>
        </div>
      </section>

      <section id="alur" class="relative overflow-hidden bg-primary-ink text-white">
        <div class="mx-auto max-w-7xl px-4 py-20 sm:px-6 lg:px-8 lg:py-28">
          <div class="grid gap-12 lg:grid-cols-[0.82fr_1.18fr]">
            <div class="landing-reveal lg:sticky lg:top-28 lg:self-start">
              <p class="text-2xs font-semibold uppercase tracking-[0.16em] text-white/55">Alur</p>
              <h2
                class="mt-4 font-display text-3xl font-bold tracking-[-0.03em] text-balance sm:text-4xl"
              >
                Satu nota memperbarui stok, menghitung komisi, dan mencatat jurnal.
              </h2>
              <p class="mt-4 max-w-lg text-sm leading-relaxed text-white/70">
                Kasir menyimpan nota campuran barang dan jasa. Data yang sama dipakai untuk stok
                cabang, komisi pelaksana, dan jurnal keuangan. Tidak ada input ulang di modul lain.
              </p>
              <p class="mt-8 border-l-2 border-white/20 pl-4 text-xs leading-relaxed text-white/60">
                Contoh transaksi di halaman ini memakai angka ilustrasi.
              </p>
            </div>

            <div>
              <div class="landing-reveal">
                <p class="font-mono text-2xs uppercase tracking-[0.14em] text-white/60">
                  01 / Penjualan
                </p>
                <ol class="mt-4 border-t border-white/15">
                  <li
                    v-for="item in saleFlow"
                    :key="item.no"
                    class="grid grid-cols-[2rem_1fr] gap-x-3 border-b border-white/15 py-4"
                  >
                    <span class="row-span-2 font-mono text-2xs text-white/55">{{ item.no }}</span>
                    <p class="text-2xs font-semibold uppercase tracking-[0.12em] text-white/60">
                      {{ item.label }}
                    </p>
                    <div class="mt-1">
                      <p class="text-sm font-semibold text-white">{{ item.title }}</p>
                      <p class="mt-1 text-xs leading-relaxed text-white/60">{{ item.detail }}</p>
                    </div>
                  </li>
                </ol>
              </div>

              <div class="landing-reveal mt-12">
                <p class="font-mono text-2xs uppercase tracking-[0.14em] text-white/60">
                  02 / Kehadiran
                </p>
                <h3 class="mt-3 font-display text-lg font-bold text-white">
                  Absensi memakai alur yang sama.
                </h3>
                <p class="mt-2 max-w-lg text-sm leading-relaxed text-white/65">
                  QR cabang, shift, payroll, dan biaya gaji membaca catatan kehadiran yang sama.
                </p>
                <ol class="mt-4 border-t border-white/15">
                  <li
                    v-for="item in hrFlow"
                    :key="item.no"
                    class="grid grid-cols-[2rem_1fr] gap-x-3 border-b border-white/15 py-4"
                  >
                    <span class="row-span-2 font-mono text-2xs text-white/55">{{ item.no }}</span>
                    <p class="text-2xs font-semibold uppercase tracking-[0.12em] text-white/60">
                      {{ item.label }}
                    </p>
                    <div class="mt-1">
                      <p class="text-sm font-semibold text-white">{{ item.title }}</p>
                      <p class="mt-1 text-xs leading-relaxed text-white/60">{{ item.detail }}</p>
                    </div>
                  </li>
                </ol>
              </div>
            </div>
          </div>
        </div>
      </section>

      <section id="modul" class="border-b border-outline/60">
        <div class="mx-auto max-w-7xl px-4 py-20 sm:px-6 lg:px-8 lg:py-28">
          <div class="grid gap-10 lg:grid-cols-[0.72fr_1.28fr] lg:gap-16">
            <div class="landing-reveal lg:sticky lg:top-28 lg:self-start">
              <p class="text-2xs font-semibold uppercase tracking-[0.16em] text-primary">Modul</p>
              <h2
                class="mt-4 font-display text-3xl font-bold tracking-[-0.03em] text-balance text-ink sm:text-4xl"
              >
                Enam modul memakai catatan yang sama.
              </h2>
              <p class="mt-4 max-w-md text-sm leading-relaxed text-secondary">
                Mulai dari kasir atau absensi. Modul lain membaca data yang sudah ada, jadi catatan
                pelanggan, stok, dan jurnal tidak disalin ke daftar baru.
              </p>
            </div>

            <div class="landing-reveal border-t border-outline/70">
              <article
                v-for="module in modules"
                :key="module.no"
                class="grid gap-2 border-b border-outline/70 py-5 sm:grid-cols-[2.5rem_1fr] sm:gap-4"
              >
                <p class="font-mono text-xs font-semibold text-primary">{{ module.no }}</p>
                <div>
                  <h3 class="font-display text-base font-bold text-ink">{{ module.title }}</h3>
                  <p class="mt-1 max-w-2xl text-sm leading-relaxed text-secondary">
                    {{ module.detail }}
                  </p>
                </div>
              </article>
            </div>
          </div>
        </div>
      </section>

      <section id="peran" class="border-b border-outline/60 bg-surface-low/45">
        <div class="mx-auto max-w-7xl px-4 py-20 sm:px-6 lg:px-8 lg:py-24">
          <div
            class="landing-reveal flex flex-col gap-4 md:flex-row md:items-end md:justify-between"
          >
            <div class="max-w-2xl">
              <p class="text-2xs font-semibold uppercase tracking-[0.16em] text-primary">Peran</p>
              <h2
                class="mt-4 font-display text-3xl font-bold tracking-[-0.03em] text-balance text-ink sm:text-4xl"
              >
                Setiap peran membuka halaman yang berbeda.
              </h2>
              <p class="mt-4 text-sm leading-relaxed text-secondary">
                Hak akses ditentukan oleh peran dan penempatan cabang. Pemilik melihat konsolidasi;
                kasir bekerja di meja kasir.
              </p>
            </div>
            <p class="font-mono text-2xs uppercase tracking-[0.12em] text-secondary">
              7 template peran
            </p>
          </div>

          <div class="landing-reveal mt-10 border-t border-outline/70">
            <div
              class="hidden grid-cols-[1fr_1.45fr_1.1fr] gap-6 border-b border-outline/70 py-3 text-2xs font-semibold uppercase tracking-[0.12em] text-secondary sm:grid"
            >
              <span>Peran</span>
              <span>Yang dikerjakan</span>
              <span>Akses utama</span>
            </div>
            <article
              v-for="role in roles"
              :key="role.name"
              class="grid gap-3 border-b border-outline/70 py-4 sm:grid-cols-[1fr_1.45fr_1.1fr] sm:gap-6 sm:py-5"
            >
              <div>
                <p
                  class="text-2xs font-semibold uppercase tracking-[0.1em] text-secondary sm:hidden"
                >
                  Peran
                </p>
                <h3 class="font-display text-sm font-bold text-ink">{{ role.name }}</h3>
              </div>
              <div>
                <p
                  class="text-2xs font-semibold uppercase tracking-[0.1em] text-secondary sm:hidden"
                >
                  Yang dikerjakan
                </p>
                <p class="text-sm leading-relaxed text-secondary">{{ role.work }}</p>
              </div>
              <div>
                <p
                  class="text-2xs font-semibold uppercase tracking-[0.1em] text-secondary sm:hidden"
                >
                  Akses utama
                </p>
                <p class="text-sm leading-relaxed text-secondary">{{ role.access }}</p>
              </div>
            </article>
          </div>

          <p class="landing-reveal mt-4 text-xs leading-relaxed text-secondary">
            Template lain tersedia untuk Kasir, Staf Gudang, dan Pelaksana Jasa.
          </p>
        </div>
      </section>

      <section id="mobile" class="border-b border-outline/60">
        <div
          class="mx-auto grid max-w-7xl gap-14 px-4 py-20 sm:px-6 lg:grid-cols-[1.08fr_0.92fr] lg:items-center lg:px-8 lg:py-28"
        >
          <div>
            <div class="landing-reveal">
              <p class="text-2xs font-semibold uppercase tracking-[0.16em] text-primary">Android</p>
              <h2
                class="mt-4 font-display text-3xl font-bold tracking-[-0.03em] text-balance text-ink sm:text-4xl"
              >
                Absensi, jadwal, dan persetujuan dari HP.
              </h2>
              <p class="mt-4 max-w-xl text-sm leading-relaxed text-secondary">
                Karyawan memindai QR cabang, mengirim lokasi, dan melampirkan selfie. Manajer
                menyetujui cuti, lembur, atau tukar shift dari aplikasi yang sama.
              </p>
            </div>

            <ul class="landing-reveal mt-8 border-t border-outline/70">
              <li
                v-for="(point, index) in androidPoints"
                :key="point.title"
                class="grid grid-cols-[2rem_1fr] gap-x-3 border-b border-outline/70 py-3.5"
              >
                <span class="row-span-2 font-mono text-2xs text-primary">
                  {{ String(index + 1).padStart(2, '0') }}
                </span>
                <p class="text-sm font-semibold text-ink">{{ point.title }}</p>
                <p class="mt-0.5 text-xs leading-relaxed text-secondary">{{ point.detail }}</p>
              </li>
            </ul>

            <div class="landing-reveal mt-7 flex flex-col gap-3 sm:flex-row">
              <RouterLink
                to="/register"
                class="inline-flex min-h-11 items-center justify-center gap-2 rounded-xl bg-primary-container px-5 text-sm font-semibold text-on-primary shadow-sm transition hover:bg-primary-hover focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/55 focus-visible:ring-offset-2"
              >
                Buat akun pengelola
                <AppIcon name="arrow_forward" :size="17" />
              </RouterLink>
              <RouterLink
                to="/login"
                class="inline-flex min-h-11 items-center justify-center rounded-xl border border-outline/80 bg-card px-5 text-sm font-semibold text-ink transition hover:border-outline-strong hover:bg-surface-low focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/55"
              >
                Sudah punya akun
              </RouterLink>
            </div>
          </div>

          <div class="landing-reveal relative mx-auto w-full max-w-[19rem]">
            <div
              class="relative rounded-[2.3rem] border-[8px] border-primary-ink bg-primary-ink p-1 shadow-float"
              role="img"
              aria-label="Contoh layar absensi Android Aurevant PKK dengan QR cabang, status lokasi, selfie, dan tombol absen masuk."
            >
              <div class="overflow-hidden rounded-[1.7rem] bg-surface">
                <div
                  class="flex items-center justify-between border-b border-outline/60 bg-card px-4 py-3"
                >
                  <div>
                    <p class="font-display text-xs font-bold text-ink">Aurevant PKK</p>
                    <p class="mt-0.5 text-3xs text-secondary">Cabang Cibubur</p>
                  </div>
                  <span
                    class="flex h-7 w-7 items-center justify-center rounded-full bg-primary-soft text-primary-ink"
                  >
                    <AppIcon name="notifications" :size="15" />
                  </span>
                </div>

                <div class="p-4" aria-hidden="true">
                  <p class="text-3xs font-semibold uppercase tracking-[0.12em] text-secondary">
                    Absensi masuk
                  </p>
                  <p class="mt-1 font-display text-3xl font-bold tracking-[-0.04em] text-ink">
                    08:02
                  </p>
                  <p class="mt-1 text-2xs text-secondary">Hari ini · Shift 08:00 sampai 17:00</p>

                  <div class="mt-4 rounded-2xl border border-primary/20 bg-card p-4 text-center">
                    <AppIcon name="qr_code_2" :size="92" class="mx-auto text-primary" />
                    <p class="mt-2 text-2xs font-semibold text-ink">QR cabang</p>
                    <p class="mt-0.5 text-3xs text-secondary">Berubah secara berkala</p>
                  </div>

                  <div class="mt-3 grid grid-cols-2 gap-2">
                    <div class="rounded-lg border border-outline/60 bg-card px-2.5 py-2">
                      <p class="text-3xs text-secondary">Lokasi</p>
                      <p class="mt-0.5 text-2xs font-semibold text-ink">Di dalam area</p>
                    </div>
                    <div class="rounded-lg border border-outline/60 bg-card px-2.5 py-2">
                      <p class="text-3xs text-secondary">Selfie</p>
                      <p class="mt-0.5 text-2xs font-semibold text-ink">Terlampir</p>
                    </div>
                  </div>

                  <div
                    class="mt-3 flex min-h-10 items-center justify-center rounded-lg bg-primary text-xs font-semibold text-white"
                  >
                    Absen masuk
                  </div>

                  <div
                    class="mt-4 grid grid-cols-4 border-t border-outline/60 pt-2 text-center text-3xs text-secondary"
                  >
                    <span class="font-semibold text-primary">Absensi</span>
                    <span>Jadwal</span>
                    <span>Slip</span>
                    <span>Kartu</span>
                  </div>
                </div>
              </div>
            </div>
            <p class="mt-4 text-center text-2xs leading-relaxed text-secondary">
              Tampilan aplikasi Android dengan data ilustrasi.
            </p>
          </div>
        </div>
      </section>

      <section id="kontrol" class="bg-primary-ink text-white">
        <div class="mx-auto max-w-7xl px-4 py-20 sm:px-6 lg:px-8 lg:py-24">
          <div class="grid gap-10 lg:grid-cols-[0.85fr_1.15fr] lg:gap-16">
            <div class="landing-reveal">
              <p class="text-2xs font-semibold uppercase tracking-[0.16em] text-white/55">
                Kontrol
              </p>
              <h2
                class="mt-4 font-display text-3xl font-bold tracking-[-0.03em] text-balance sm:text-4xl"
              >
                Akses dibatasi oleh perusahaan, peran, dan cabang.
              </h2>
              <p class="mt-4 max-w-lg text-sm leading-relaxed text-white/70">
                Data setiap perusahaan terpisah. Di dalam perusahaan, peran dan scope menentukan
                halaman yang bisa dibuka.
              </p>
              <p class="mt-6 border-l-2 border-white/20 pl-4 text-xs leading-relaxed text-white/60">
                Tidak ada akses lintas perusahaan.
              </p>
            </div>

            <dl class="landing-reveal border-t border-white/15">
              <div
                v-for="control in controls"
                :key="control.label"
                class="grid gap-1 border-b border-white/15 py-4 sm:grid-cols-[7rem_1fr] sm:gap-4"
              >
                <dt class="font-mono text-2xs uppercase tracking-[0.12em] text-white/60">
                  {{ control.label }}
                </dt>
                <dd class="text-sm leading-relaxed text-white/75">{{ control.value }}</dd>
              </div>
            </dl>
          </div>
        </div>
      </section>

      <section id="faq" class="border-b border-outline/60">
        <div class="mx-auto max-w-4xl px-4 py-20 sm:px-6 lg:px-8 lg:py-24">
          <div class="landing-reveal">
            <p class="text-2xs font-semibold uppercase tracking-[0.16em] text-primary">FAQ</p>
            <h2
              class="mt-4 font-display text-3xl font-bold tracking-[-0.03em] text-ink sm:text-4xl"
            >
              Pertanyaan yang sering muncul.
            </h2>
          </div>

          <div class="landing-reveal mt-8 border-t border-outline/70">
            <details v-for="faq in faqs" :key="faq.no" class="group border-b border-outline/70">
              <summary
                class="flex min-h-14 cursor-pointer list-none items-center gap-3 py-4 text-sm font-semibold text-ink focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/55 focus-visible:ring-offset-2"
              >
                <span class="font-mono text-2xs font-normal text-secondary">{{ faq.no }}</span>
                <span class="flex-1">{{ faq.q }}</span>
                <AppIcon
                  name="add"
                  :size="19"
                  class="text-secondary transition-transform duration-200 group-open:rotate-45 motion-reduce:transition-none"
                />
              </summary>
              <p class="max-w-3xl pb-4 pl-8 text-sm leading-relaxed text-secondary">{{ faq.a }}</p>
            </details>
          </div>
        </div>
      </section>

      <section id="mulai">
        <div class="mx-auto max-w-7xl px-4 py-20 sm:px-6 lg:px-8 lg:py-24">
          <div class="landing-reveal max-w-2xl">
            <p class="text-2xs font-semibold uppercase tracking-[0.16em] text-primary">Mulai</p>
            <h2
              class="mt-4 font-display text-3xl font-bold tracking-[-0.03em] text-balance text-ink sm:text-4xl"
            >
              Buat akun, lalu tambahkan cabang pertama.
            </h2>
            <p class="mt-4 text-sm leading-relaxed text-secondary">
              Paket Gratis mencakup 1 cabang dan 5 karyawan. Paket Dasar dan Pro menambah kapasitas
              saat tim bertambah.
            </p>
          </div>

          <ol class="landing-reveal mt-10 grid border-y border-outline/70 md:grid-cols-3">
            <li
              v-for="(step, index) in steps"
              :key="step.no"
              class="border-b border-outline/70 py-6 last:border-b-0 md:border-b-0 md:border-l md:border-outline/70 md:px-6 md:first:border-l-0"
              :class="index === 0 ? 'md:pl-0' : ''"
            >
              <p class="font-mono text-xs font-semibold text-primary">{{ step.no }}</p>
              <h3 class="mt-4 font-display text-base font-bold text-ink">{{ step.title }}</h3>
              <p class="mt-2 text-sm leading-relaxed text-secondary">{{ step.detail }}</p>
            </li>
          </ol>

          <div class="landing-reveal mt-8 flex flex-col gap-3 sm:flex-row sm:items-center">
            <RouterLink
              to="/register"
              class="inline-flex min-h-12 items-center justify-center gap-2 rounded-xl bg-primary-container px-6 text-sm font-semibold text-on-primary shadow-card transition hover:bg-primary-hover hover:shadow-lifted focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/55 focus-visible:ring-offset-2"
            >
              Buat akun pengelola
              <AppIcon name="arrow_forward" :size="18" />
            </RouterLink>
            <RouterLink
              to="/login"
              class="inline-flex min-h-12 items-center justify-center rounded-xl px-3 text-sm font-semibold text-secondary transition hover:text-ink focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/55 focus-visible:ring-offset-2"
            >
              Sudah punya akun? Masuk
            </RouterLink>
          </div>
        </div>
      </section>
    </main>

    <footer class="border-t border-outline/60 bg-surface">
      <div class="mx-auto max-w-7xl px-4 py-12 sm:px-6 lg:px-8">
        <div class="grid gap-10 md:grid-cols-[1.4fr_0.8fr_0.8fr]">
          <div>
            <RouterLink
              to="/"
              class="flex min-h-11 w-fit items-center gap-3 rounded-xl focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/55 focus-visible:ring-offset-2"
              aria-label="Aurevant PKK, beranda"
            >
              <span
                class="flex h-10 w-10 items-center justify-center rounded-xl bg-primary font-display text-base font-extrabold text-on-primary shadow-card"
              >
                A
              </span>
              <span>
                <span class="block font-display text-[15px] font-bold tracking-tight text-ink"
                  >Aurevant PKK</span
                >
                <span class="block text-2xs text-secondary">Operasional barang &amp; jasa</span>
              </span>
            </RouterLink>
            <p class="mt-4 max-w-sm text-sm leading-relaxed text-secondary">
              Kasir, stok, order jasa, absensi, payroll, dan keuangan untuk bisnis multi-cabang.
            </p>
          </div>

          <div>
            <p class="text-2xs font-semibold uppercase tracking-[0.14em] text-secondary">
              Jelajahi
            </p>
            <nav class="mt-4 grid gap-1" aria-label="Navigasi footer">
              <a
                v-for="item in navItems"
                :key="item.href"
                :href="item.href"
                class="flex min-h-11 items-center rounded-lg text-sm text-secondary transition hover:text-ink focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/55"
              >
                {{ item.label }}
              </a>
              <a
                href="#faq"
                class="flex min-h-11 items-center rounded-lg text-sm text-secondary transition hover:text-ink focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/55"
              >
                FAQ
              </a>
            </nav>
          </div>

          <div>
            <p class="text-2xs font-semibold uppercase tracking-[0.14em] text-secondary">Akun</p>
            <nav class="mt-4 grid gap-1" aria-label="Navigasi akun">
              <RouterLink
                to="/login"
                class="flex min-h-11 items-center rounded-lg text-sm text-secondary transition hover:text-ink focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/55"
              >
                Masuk
              </RouterLink>
              <RouterLink
                to="/register"
                class="flex min-h-11 items-center rounded-lg text-sm text-secondary transition hover:text-ink focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/55"
              >
                Daftar akun
              </RouterLink>
              <RouterLink
                to="/lupa-password"
                class="flex min-h-11 items-center rounded-lg text-sm text-secondary transition hover:text-ink focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/55"
              >
                Lupa sandi
              </RouterLink>
            </nav>
          </div>
        </div>

        <div
          class="mt-10 flex flex-col gap-2 border-t border-outline/60 pt-6 text-2xs text-secondary sm:flex-row sm:items-center sm:justify-between"
        >
          <p>© {{ year }} Aurevant PKK.</p>
          <p>Untuk bisnis barang dan jasa.</p>
        </div>
      </div>
    </footer>
  </div>
</template>
