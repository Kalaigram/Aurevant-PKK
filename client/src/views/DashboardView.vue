<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { api, assetUrl } from '../lib/api'
import { rupiah, tanggal } from '../lib/format'
import { useCompany } from '../composables/useCompany'
import { useAsync } from '../composables/useAsync'
import { useAccessStore } from '../stores/access'
import AppAvatar from '../components/ui/AppAvatar.vue'
import AppBadge from '../components/ui/AppBadge.vue'
import AppBanner from '../components/ui/AppBanner.vue'
import AppButton from '../components/ui/AppButton.vue'
import AppCard from '../components/ui/AppCard.vue'
import AppIcon from '../components/ui/AppIcon.vue'
import AppSkeleton from '../components/ui/AppSkeleton.vue'
import AppStat from '../components/ui/AppStat.vue'

type BadgeTone = 'green' | 'red' | 'amber' | 'blue' | 'gray'

interface Keuangan {
  kas_bersih_bulan_ini: number
  laba_bersih_bulan_ini: number
  hutang_total: number
  piutang_total: number
}
interface Penjualan {
  omzet_hari_ini: number
  transaksi_hari_ini: number
  shift_terbuka: number
}
interface Operasional {
  transfer_diajukan: number
  opname_diajukan: number
}
interface Sdm {
  hadir: number
  terlambat: number
  alpa: number
  cuti_diajukan: number
}
interface DashboardData {
  company: { id: number; name: string; status: string }
  subscription: { status: string; ends_at: string | null }
  month_start: string
  today: string
  keuangan?: Keuangan
  penjualan?: Penjualan
  operasional?: Operasional
  sdm?: Sdm
  penggajian?: Record<string, number>
  karyawan?: { total: number }
}
interface DailyRow {
  date: string
  netOmzet?: number | string
}
interface AksiCepat {
  to: string
  label: string
  desc: string
  icon: string
}
interface Antrian {
  label: string
  desc: string
  count: number
  icon: string
}

const APPROVAL_PERMS = [
  'transaksi.buat',
  'transaksi.void',
  'transaksi.diskon',
  'retur.buat',
  'retur.setujui',
  'po.buat',
  'po.setujui',
  'stok.transfer',
  'stok.opname',
]

const SUBSCRIPTION_STATUS: Record<string, { label: string; tone: BadgeTone }> = {
  aktif: { label: 'Aktif', tone: 'green' },
  trial: { label: 'Uji coba', tone: 'blue' },
  tenggang: { label: 'Masa tenggang', tone: 'amber' },
  read_only: { label: 'Hanya baca', tone: 'amber' },
  ditangguhkan: { label: 'Ditangguhkan', tone: 'red' },
  tanpa_langganan: { label: 'Tanpa langganan', tone: 'gray' },
}

const PAYROLL_LABELS: Record<string, string> = {
  draft: 'Draf',
  review: 'Review',
  disetujui: 'Disetujui',
  dibayar: 'Dibayar',
}

const PAYROLL_TONES: Record<string, BadgeTone> = {
  draft: 'gray',
  review: 'amber',
  disetujui: 'blue',
  dibayar: 'green',
}

const DOT_TONES: Record<BadgeTone, string> = {
  green: 'bg-ok',
  red: 'bg-danger',
  amber: 'bg-warn',
  blue: 'bg-info',
  gray: 'bg-outline-strong',
}

const { auth, companyId } = useCompany()
const access = useAccessStore()
const { loading, error, run } = useAsync()
const data = ref<DashboardData | null>(null)
const tren = ref<number[]>([])
const trenLoading = ref(false)
const awal = ref(true)

const base = computed(() => `/c/${companyId.value}`)
const keuangan = computed(() => data.value?.keuangan ?? null)
const penjualan = computed(() => data.value?.penjualan ?? null)
const operasional = computed(() => data.value?.operasional ?? null)
const sdm = computed(() => data.value?.sdm ?? null)
const karyawan = computed(() => data.value?.karyawan ?? null)

const subInfo = computed<{ label: string; tone: BadgeTone }>(() => {
  const status = data.value?.subscription.status ?? ''
  return (
    SUBSCRIPTION_STATUS[status] ?? {
      label: status ? status.replaceAll('_', ' ') : 'Tidak diketahui',
      tone: 'gray',
    }
  )
})

const tulisDibatasi = computed(() =>
  ['read_only', 'ditangguhkan', 'tanpa_langganan'].includes(data.value?.subscription.status ?? ''),
)

const masaAktif = computed(() =>
  data.value?.subscription.ends_at ? tanggal(data.value.subscription.ends_at) : 'Belum diatur',
)

const periode = computed(() => {
  if (!data.value) return '-'
  return `${tanggal(data.value.month_start)} – ${tanggal(data.value.today)}`
})

const bisaLaporan = computed(
  () => access.loaded && access.can('keuangan.lihat', 'laporan.konsolidasi'),
)
const bisaNota = computed(() => access.loaded && access.can('nota.lihat'))
const bisaPayroll = computed(() => access.loaded && access.can('gaji.kelola', 'gaji.setujui'))
const bisaKaryawan = computed(() => access.loaded && access.can('karyawan.kelola'))
const bisaKehadiran = computed(
  () => access.loaded && access.can('absensi.approve', 'absensi.koreksi'),
)
const linkKehadiran = computed(() => `${base.value}/${bisaKehadiran.value ? 'hr-attendance' : 'attendance'}`)

const aksiCepat = computed<AksiCepat[]>(() => {
  if (!access.loaded) return []
  const items: AksiCepat[] = []
  if (access.can('transaksi.buat', 'shift.buka', 'shift.tutup')) {
    items.push({ to: `${base.value}/pos`, label: 'Buka kasir', desc: 'Transaksi & shift', icon: 'point_of_sale' })
  }
  if (access.can(...APPROVAL_PERMS)) {
    items.push({ to: `${base.value}/approvals`, label: 'Persetujuan', desc: 'Tinjau pengajuan', icon: 'fact_check' })
  }
  if (access.can('biaya.input')) {
    items.push({ to: `${base.value}/expenses`, label: 'Catat biaya', desc: 'Biaya operasional', icon: 'receipt' })
  }
  if (access.can('keuangan.lihat', 'laporan.konsolidasi')) {
    items.push({ to: `${base.value}/reports`, label: 'Laporan', desc: 'Omzet & laba', icon: 'bar_chart' })
  }
  items.push({ to: `${base.value}/attendance`, label: 'Absensi', desc: 'Clock in & riwayat', icon: 'fingerprint' })
  items.push({ to: `${base.value}/notifications`, label: 'Notifikasi', desc: 'Pemberitahuan terbaru', icon: 'notifications' })
  return items.slice(0, 6)
})

const antrian = computed<Antrian[]>(() => {
  const o = operasional.value
  if (!o) return []
  return [
    { label: 'Transfer stok', desc: 'Menunggu persetujuan', count: o.transfer_diajukan, icon: 'swap_horiz' },
    { label: 'Opname stok', desc: 'Menunggu persetujuan', count: o.opname_diajukan, icon: 'content_paste_search' },
  ]
})
const antrianAktif = computed(() => antrian.value.filter((item) => item.count > 0))

const kehadiran = computed(() => {
  const s = sdm.value
  if (!s) return []
  return [
    { label: 'Hadir', value: s.hadir, dot: DOT_TONES.green },
    { label: 'Terlambat', value: s.terlambat, dot: DOT_TONES.amber },
    { label: 'Alpa', value: s.alpa, dot: DOT_TONES.red },
    { label: 'Cuti', value: s.cuti_diajukan, dot: DOT_TONES.blue },
  ]
})

const payrollEntries = computed(() => {
  const p = data.value?.penggajian
  if (!p) return []
  const order = ['draft', 'review', 'disetujui', 'dibayar']
  return Object.entries(p)
    .sort(([a], [b]) => {
      const ia = order.indexOf(a)
      const ib = order.indexOf(b)
      return (ia === -1 ? 99 : ia) - (ib === -1 ? 99 : ib)
    })
    .map(([status, count]) => ({
      status,
      count,
      label: PAYROLL_LABELS[status] ?? status.replaceAll('_', ' '),
      tone: PAYROLL_TONES[status] ?? 'gray',
    }))
})

const sparkline = computed(() => {
  if (tren.value.length < 2) return null
  const min = Math.min(...tren.value)
  const max = Math.max(...tren.value)
  const span = max - min
  const lastIndex = tren.value.length - 1
  const points = tren.value.map((value, index) => ({
    x: (index / lastIndex) * 100,
    y: span === 0 ? 22 : 34 - ((value - min) / span) * 26,
  }))
  const line = points.map((point, index) => `${index === 0 ? 'M' : 'L'}${point.x.toFixed(1)},${point.y.toFixed(1)}`).join(' ')
  const area = `${line} L100,38 L0,38 Z`
  return { line, area, last: points[points.length - 1] ?? { x: 100, y: 22 } }
})

async function muatTren() {
  const d = data.value
  if (!d?.penjualan) {
    tren.value = []
    return
  }
  trenLoading.value = true
  try {
    const res = await api.get<{ rows: DailyRow[] }>(
      `/companies/${companyId.value}/reports/daily?from=${d.month_start}&to=${d.today}`,
      auth.token,
    )
    const perTanggal = new Map<string, number>()
    for (const row of res.rows ?? []) {
      perTanggal.set(row.date, (perTanggal.get(row.date) ?? 0) + Number(row.netOmzet ?? 0))
    }
    tren.value = [...perTanggal.entries()]
      .sort(([a], [b]) => a.localeCompare(b))
      .map(([, value]) => value)
  } catch {
    tren.value = []
  } finally {
    trenLoading.value = false
  }
}

async function muat() {
  data.value = await run(() =>
    api.get<DashboardData>(`/companies/${companyId.value}/dashboard`, auth.token),
  )
  awal.value = false
  if (data.value?.penjualan) void muatTren()
}

onMounted(() => {
  void muat()
})
</script>

<template>
  <div class="mx-auto w-full max-w-6xl space-y-6">
    <div v-if="(loading && !data) || awal" class="space-y-6" aria-busy="true">
      <p class="sr-only" role="status">Memuat dashboard…</p>
      <div class="rounded-3xl border border-outline/60 bg-card p-5 shadow-card sm:p-7">
        <div class="flex items-start gap-4">
          <AppSkeleton width="3.5rem" height="3.5rem" rounded="xl" />
          <div class="min-w-0 flex-1 pt-1">
            <AppSkeleton width="8rem" height="0.65rem" />
            <AppSkeleton class="mt-2" width="60%" height="1.75rem" />
            <AppSkeleton class="mt-2" width="9rem" height="1rem" />
          </div>
        </div>
        <div class="mt-6 grid grid-cols-2 gap-3">
          <AppSkeleton height="4.5rem" rounded="xl" />
          <AppSkeleton height="4.5rem" rounded="xl" />
        </div>
      </div>
      <div class="grid grid-cols-2 gap-2.5 lg:grid-cols-3">
        <AppSkeleton v-for="i in 6" :key="i" height="4.25rem" rounded="xl" />
      </div>
      <div class="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
        <AppStat v-for="i in 4" :key="i" label="Ringkasan" loading />
      </div>
    </div>

    <AppCard v-else-if="!data" padding="lg" class="mx-auto max-w-xl">
      <AppBanner tone="red" :message="error || 'Dashboard tidak dapat dimuat.'" />
      <AppButton variant="secondary" @click="muat">Coba lagi</AppButton>
    </AppCard>

    <template v-else>
      <section
        class="relative overflow-hidden rounded-3xl bg-gradient-to-br from-primary-container via-primary to-primary-ink text-white shadow-float"
      >
        <div class="ambient-glow pointer-events-none absolute inset-0 opacity-50" aria-hidden="true" />
        <div class="relative p-5 sm:p-7">
          <div class="flex flex-col gap-6 lg:flex-row lg:items-end lg:justify-between">
            <div class="min-w-0">
              <div class="flex items-start gap-4">
                <span
                  class="flex h-14 w-14 shrink-0 items-center justify-center rounded-2xl bg-white/10 p-1 ring-1 ring-white/20"
                >
                  <AppAvatar
                    :name="data.company.name"
                    :src="assetUrl(auth.companyLogoUrl)"
                    size="lg"
                    square
                    class="!ring-0"
                  />
                </span>
                <div class="min-w-0">
                  <p class="text-2xs font-semibold uppercase tracking-[0.14em] text-white/65">
                    Ruang kerja perusahaan
                  </p>
                  <h1
                    class="font-display mt-1.5 text-2xl font-bold leading-tight tracking-tight text-white sm:text-3xl"
                  >
                    {{ data.company.name }}
                  </h1>
                  <div class="mt-2.5 flex flex-wrap items-center gap-2">
                    <AppBadge :tone="subInfo.tone" dot>{{ subInfo.label }}</AppBadge>
                    <span class="text-xs text-white/65">Data per {{ tanggal(data.today) }}</span>
                  </div>
                </div>
              </div>
            </div>
            <dl class="grid grid-cols-2 gap-2.5 sm:gap-3">
              <div class="min-w-0 rounded-2xl bg-white/[0.08] px-3.5 py-3 ring-1 ring-white/10">
                <dt class="text-2xs font-semibold uppercase tracking-[0.1em] text-white/60">
                  Masa aktif
                </dt>
                <dd class="mt-1 truncate text-xs font-semibold text-white sm:text-sm">
                  {{ masaAktif }}
                </dd>
              </div>
              <div class="min-w-0 rounded-2xl bg-white/[0.08] px-3.5 py-3 ring-1 ring-white/10">
                <dt class="text-2xs font-semibold uppercase tracking-[0.1em] text-white/60">
                  Periode ringkasan
                </dt>
                <dd class="mt-1 text-xs font-semibold leading-snug text-white sm:text-sm">
                  {{ periode }}
                </dd>
              </div>
            </dl>
          </div>
        </div>
        <div
          v-if="tulisDibatasi"
          class="relative flex items-start gap-3 border-t border-white/10 bg-primary-ink/35 px-5 py-3.5 text-sm leading-relaxed text-white/80 sm:px-7"
        >
          <AppIcon name="lock" :size="18" class="mt-0.5 shrink-0 text-white/70" />
          <p>
            <span class="font-semibold text-white">Akses tulis dibatasi.</span>
            Laporan dan riwayat tetap dapat dibaca. Hubungi pemilik perusahaan untuk memperpanjang
            langganan.
          </p>
        </div>
      </section>

      <section v-if="aksiCepat.length" aria-labelledby="aksi-cepat" class="space-y-3">
        <div>
          <h2 id="aksi-cepat" class="font-display text-base font-semibold tracking-tight text-ink sm:text-lg">
            Aksi cepat
          </h2>
          <p class="mt-0.5 text-sm text-secondary">Langkah yang paling sering Anda gunakan.</p>
        </div>
        <div class="grid grid-cols-2 gap-2.5 lg:grid-cols-3">
          <RouterLink
            v-for="aksi in aksiCepat"
            :key="aksi.to"
            :to="aksi.to"
            class="group flex flex-col items-start gap-2.5 rounded-2xl border border-outline/70 bg-card p-3.5 shadow-card transition hover:border-primary/30 hover:shadow-lifted focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/55 sm:flex-row sm:items-center sm:gap-3 sm:p-4"
          >
            <span
              class="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-primary-soft text-primary-ink transition group-hover:bg-primary-container group-hover:text-on-primary"
            >
              <AppIcon :name="aksi.icon" :size="20" />
            </span>
            <span class="min-w-0 flex-1">
              <span class="block text-sm font-semibold text-ink sm:truncate">{{ aksi.label }}</span>
              <span class="mt-0.5 hidden truncate text-xs text-secondary sm:block">
                {{ aksi.desc }}
              </span>
            </span>
            <span
              class="hidden shrink-0 text-secondary transition group-hover:translate-x-0.5 group-hover:text-primary sm:block"
            >
              <AppIcon name="arrow_forward" :size="18" />
            </span>
          </RouterLink>
        </div>
      </section>

      <section v-if="keuangan" aria-labelledby="ringkasan-finansial" class="space-y-3">
        <div class="flex flex-wrap items-end justify-between gap-3">
          <div>
            <h2
              id="ringkasan-finansial"
              class="font-display text-base font-semibold tracking-tight text-ink sm:text-lg"
            >
              Ringkasan finansial
            </h2>
            <p class="mt-0.5 text-sm text-secondary">
              Posisi bulan berjalan hingga {{ tanggal(data.today) }}.
            </p>
          </div>
          <RouterLink
            v-if="bisaLaporan"
            :to="`${base}/reports`"
            class="inline-flex items-center gap-1 text-sm font-semibold text-primary hover:text-primary-ink focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/55"
          >
            Buka laporan
            <AppIcon name="arrow_forward" :size="16" />
          </RouterLink>
        </div>
        <div class="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
          <AppStat
            label="Kas bersih"
            :value="rupiah(keuangan.kas_bersih_bulan_ini)"
            icon="account_balance_wallet"
            :tone="keuangan.kas_bersih_bulan_ini >= 0 ? 'positive' : 'negative'"
            hint="Bulan berjalan"
          />
          <AppStat
            label="Laba bersih"
            :value="rupiah(keuangan.laba_bersih_bulan_ini)"
            icon="trending_up"
            :tone="keuangan.laba_bersih_bulan_ini >= 0 ? 'positive' : 'negative'"
            hint="Setelah beban"
          />
          <AppStat
            label="Hutang total"
            :value="rupiah(keuangan.hutang_total)"
            icon="arrow_upward"
            tone="warning"
            hint="Belum dibayar"
          />
          <AppStat
            label="Piutang total"
            :value="rupiah(keuangan.piutang_total)"
            icon="arrow_downward"
            hint="Belum tertagih"
          />
        </div>
      </section>

      <section v-if="penjualan" aria-labelledby="penjualan-hari-ini" class="space-y-3">
        <div class="flex flex-wrap items-end justify-between gap-3">
          <div>
            <h2
              id="penjualan-hari-ini"
              class="font-display text-base font-semibold tracking-tight text-ink sm:text-lg"
            >
              Penjualan hari ini
            </h2>
            <p class="mt-0.5 text-sm text-secondary">Aktivitas nota dan shift kasir.</p>
          </div>
          <RouterLink
            v-if="bisaNota"
            :to="`${base}/sales`"
            class="inline-flex items-center gap-1 text-sm font-semibold text-primary hover:text-primary-ink focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/55"
          >
            Lihat nota
            <AppIcon name="arrow_forward" :size="16" />
          </RouterLink>
        </div>
        <div class="grid gap-3 sm:grid-cols-2 xl:grid-cols-3">
          <AppStat
            label="Omzet hari ini"
            :value="rupiah(penjualan.omzet_hari_ini)"
            icon="payments"
            :tone="penjualan.omzet_hari_ini > 0 ? 'positive' : 'default'"
          >
            <div class="flex items-center gap-3">
              <svg
                v-if="sparkline"
                viewBox="0 0 100 40"
                preserveAspectRatio="none"
                class="h-8 min-w-0 flex-1 text-primary"
                aria-hidden="true"
              >
                <defs>
                  <linearGradient id="dashboard-sparkline" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="0%" stop-color="currentColor" stop-opacity="0.22" />
                    <stop offset="100%" stop-color="currentColor" stop-opacity="0" />
                  </linearGradient>
                </defs>
                <path :d="sparkline.area" fill="url(#dashboard-sparkline)" />
                <path
                  :d="sparkline.line"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="2"
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  vector-effect="non-scaling-stroke"
                />
                <circle :cx="sparkline.last.x" :cy="sparkline.last.y" r="2" class="fill-primary" />
              </svg>
              <AppSkeleton v-else-if="trenLoading" width="100%" height="1.75rem" rounded="md" />
              <span v-else class="text-xs text-secondary">Belum ada tren harian.</span>
              <span v-if="sparkline" class="shrink-0 text-2xs font-medium text-secondary">
                {{ tren.length }} hari
              </span>
            </div>
          </AppStat>
          <AppStat
            label="Transaksi"
            :value="penjualan.transaksi_hari_ini"
            icon="receipt_long"
            hint="Nota hari ini"
          />
          <AppStat
            label="Shift terbuka"
            :value="penjualan.shift_terbuka"
            icon="point_of_sale"
            :tone="penjualan.shift_terbuka > 0 ? 'positive' : 'default'"
            hint="Kasir aktif"
          />
        </div>
      </section>

      <section
        v-if="operasional || sdm"
        aria-labelledby="perlu-perhatian"
        class="space-y-3"
      >
        <div>
          <h2
            id="perlu-perhatian"
            class="font-display text-base font-semibold tracking-tight text-ink sm:text-lg"
          >
            Perlu perhatian
          </h2>
          <p class="mt-0.5 text-sm text-secondary">
            Antrian persetujuan dan kehadiran hari ini.
          </p>
        </div>
        <div class="grid gap-4 lg:grid-cols-2">
          <AppCard
            v-if="operasional"
            title="Antrian persetujuan"
            subtitle="Pengajuan yang menunggu keputusan"
          >
            <div
              v-if="antrianAktif.length === 0"
              class="flex items-center gap-2 rounded-xl bg-ok-bg/70 p-3 text-sm text-ok-ink"
            >
              <AppIcon name="check_circle" :size="18" />
              Tidak ada antrian. Semua pengajuan sudah ditinjau.
            </div>
            <div v-else class="space-y-2">
              <RouterLink
                v-for="item in antrianAktif"
                :key="item.label"
                :to="`${base}/approvals`"
                class="group flex items-center gap-3 rounded-xl border border-outline/60 bg-surface-raised/60 p-3 transition hover:border-primary/30 hover:bg-primary-soft/30 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/55"
              >
                <span
                  class="flex h-9 w-9 shrink-0 items-center justify-center rounded-xl bg-warn-bg text-warn-ink"
                >
                  <AppIcon :name="item.icon" :size="18" />
                </span>
                <span class="min-w-0 flex-1">
                  <span class="block text-sm font-semibold text-ink">{{ item.label }}</span>
                  <span class="block text-xs text-secondary">{{ item.desc }}</span>
                </span>
                <span
                  class="rounded-full bg-warn-bg px-2.5 py-0.5 text-xs font-bold tabular-nums text-warn-ink"
                >
                  {{ item.count }}
                </span>
                <AppIcon name="chevron_right" :size="18" class="shrink-0 text-secondary" />
              </RouterLink>
            </div>
            <RouterLink
              :to="`${base}/approvals`"
              class="mt-3 inline-flex items-center gap-1 text-xs font-semibold text-primary hover:text-primary-ink focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/55"
            >
              Buka kotak persetujuan
              <AppIcon name="arrow_forward" :size="15" />
            </RouterLink>
          </AppCard>

          <AppCard
            v-if="sdm"
            title="Kehadiran hari ini"
            subtitle="Ringkasan absensi tim"
          >
            <template #actions>
              <RouterLink
                v-if="bisaKehadiran"
                :to="linkKehadiran"
                class="text-xs font-semibold text-primary hover:text-primary-ink focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/55"
              >
                Detail
              </RouterLink>
            </template>
            <div class="grid grid-cols-2 gap-2.5">
              <div
                v-for="item in kehadiran"
                :key="item.label"
                class="rounded-xl border border-outline/50 bg-surface-raised/70 p-3"
              >
                <div class="flex items-center gap-1.5">
                  <span class="h-2 w-2 rounded-full" :class="item.dot" aria-hidden="true" />
                  <p class="text-2xs font-semibold uppercase tracking-[0.08em] text-secondary">
                    {{ item.label }}
                  </p>
                </div>
                <p class="font-display mt-1.5 text-xl font-bold tabular-nums text-ink">
                  {{ item.value }}
                </p>
              </div>
            </div>
            <RouterLink
              v-if="sdm.cuti_diajukan > 0"
              :to="`${base}/leaves`"
              class="mt-3 flex items-center gap-2 rounded-xl bg-info-bg/60 px-3 py-2.5 text-xs font-semibold text-info-ink transition hover:bg-info-bg focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/55"
            >
              <AppIcon name="event_note" :size="16" />
              {{ sdm.cuti_diajukan }} pengajuan cuti menunggu
              <AppIcon name="arrow_forward" :size="15" class="ml-auto" />
            </RouterLink>
          </AppCard>
        </div>
      </section>

      <section
        v-if="karyawan || payrollEntries.length"
        aria-labelledby="tim-penggajian"
        class="space-y-3"
      >
        <div>
          <h2
            id="tim-penggajian"
            class="font-display text-base font-semibold tracking-tight text-ink sm:text-lg"
          >
            Tim & penggajian
          </h2>
          <p class="mt-0.5 text-sm text-secondary">Karyawan aktif dan status periode gaji.</p>
        </div>
        <div class="grid gap-4 lg:grid-cols-2">
          <AppCard v-if="karyawan" title="Tim aktif" subtitle="Karyawan terdaftar di perusahaan">
            <div class="flex items-center justify-between gap-4">
              <div>
                <p class="font-display text-4xl font-bold tabular-nums tracking-tight text-ink">
                  {{ karyawan.total }}
                </p>
                <p class="mt-1 text-sm text-secondary">karyawan aktif</p>
              </div>
              <span
                class="flex h-12 w-12 items-center justify-center rounded-2xl bg-primary-soft text-primary-ink"
              >
                <AppIcon name="groups" :size="26" />
              </span>
            </div>
            <RouterLink
              v-if="bisaKaryawan"
              :to="`${base}/employees`"
              class="mt-4 inline-flex items-center gap-1 text-xs font-semibold text-primary hover:text-primary-ink focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/55"
            >
              Kelola karyawan
              <AppIcon name="arrow_forward" :size="15" />
            </RouterLink>
          </AppCard>

          <AppCard
            v-if="payrollEntries.length"
            title="Penggajian"
            subtitle="Status periode berjalan"
          >
            <template #actions>
              <RouterLink
                v-if="bisaPayroll"
                :to="`${base}/payroll`"
                class="text-xs font-semibold text-primary hover:text-primary-ink focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/55"
              >
                Kelola
              </RouterLink>
            </template>
            <div class="grid grid-cols-2 gap-2.5 sm:grid-cols-4">
              <div
                v-for="item in payrollEntries"
                :key="item.status"
                class="rounded-xl border border-outline/50 bg-surface-raised/70 p-3"
              >
                <p class="text-2xs font-semibold uppercase tracking-[0.08em] text-secondary">
                  {{ item.label }}
                </p>
                <div class="mt-1.5 flex items-center justify-between gap-2">
                  <span class="font-display text-xl font-bold tabular-nums text-ink">
                    {{ item.count }}
                  </span>
                  <span
                    class="h-2 w-2 rounded-full"
                    :class="DOT_TONES[item.tone]"
                    aria-hidden="true"
                  />
                </div>
              </div>
            </div>
          </AppCard>
        </div>
      </section>
    </template>
  </div>
</template>
