<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { api, ApiError } from '../lib/api'
import { rupiah } from '../lib/format'
import { useCompany } from '../composables/useCompany'
import { useAsync } from '../composables/useAsync'
import AppCard from '../components/ui/AppCard.vue'
import AppBadge from '../components/ui/AppBadge.vue'
import AppBanner from '../components/ui/AppBanner.vue'
import AppIcon from '../components/ui/AppIcon.vue'

const { auth, companyId } = useCompany()
const { loading, error, run } = useAsync()
const data = ref<Record<string, unknown> | null>(null)
const tren = ref<number[]>([])

onMounted(async () => {
  data.value = await run(() => api.get<Record<string, unknown>>(`/companies/${companyId.value}/dashboard`, auth.token))
  if (section('penjualan')) {
    try {
      const d = data.value as Record<string, string>
      const res = await api.get<{ rows: { date: string; netOmzet: string }[] }>(
        `/companies/${companyId.value}/reports/daily?from=${d.month_start}&to=${d.today}`, auth.token,
      )
      const perTanggal = new Map<string, number>()
      for (const r of res.rows ?? []) perTanggal.set(r.date, (perTanggal.get(r.date) ?? 0) + Number(r.netOmzet))
      tren.value = [...perTanggal.entries()].sort().map(([, v]) => v)
    } catch (e) {
      if (!(e instanceof ApiError && e.status === 403)) throw e
    }
  }
})

function sparkline(): string {
  const vals = tren.value
  if (vals.length < 2) return ''
  const max = Math.max(...vals)
  const min = Math.min(...vals)
  const span = max - min || 1
  const pts = vals.map((v, i) => `${(i / (vals.length - 1)) * 100},${34 - ((v - min) / span) * 30}`)
  return `M0,36 L${pts.join(' L')} L100,36 Z`
}

function section(key: string): Record<string, number> | null {
  const s = data.value?.[key]
  return s !== null && typeof s === 'object' ? (s as Record<string, number>) : null
}

const sub = () => (data.value?.subscription ?? {}) as Record<string, string>
const sel = (v: unknown) => (v !== null && typeof v === 'object' ? (v as Record<string, string>) : {})
const namaPerusahaan = () => sel(data.value?.company).name ?? '…'
const bulanAwal = () => sel(data.value).month_start
const hariIni = () => sel(data.value).today
const subTone = () => {
  const s = sub().status
  return s === 'aktif' || s === 'trial' ? 'green' : s === 'tenggang' ? 'amber' : 'red'
}
</script>

<template>
  <div>
    <div class="mb-4 flex flex-col justify-between gap-3 rounded-xl bg-card p-4 shadow-sm xl:flex-row xl:items-center">
      <div class="flex items-center gap-3">
        <div class="flex h-12 w-12 items-center justify-center rounded-xl bg-primary-container text-on-primary">
          <AppIcon name="storefront" :size="28" />
        </div>
        <div>
          <div class="flex flex-wrap items-center gap-2">
            <h1 class="font-display text-lg font-semibold">{{ namaPerusahaan() }}</h1>
            <AppBadge :tone="subTone()">{{ sub().status ?? '…' }}</AppBadge>
          </div>
          <p class="text-sm text-secondary">
            Langganan sampai <strong class="text-ink">{{ sub().endsAt ?? sub().trialEndsAt ?? '–' }}</strong>
            · Periode {{ bulanAwal() }} s/d {{ hariIni() }}
          </p>
        </div>
      </div>
      <RouterLink
        :to="`/c/${companyId}/approvals`"
        class="inline-flex items-center gap-2 rounded-lg bg-primary-container px-4 py-2 text-sm font-semibold text-on-primary transition-colors hover:bg-primary"
      ><AppIcon name="fact_check" :size="18" />Kotak Persetujuan</RouterLink>
    </div>
    <AppBanner tone="red" :message="error" />
    <p v-if="loading" class="text-sm text-secondary">Memuat…</p>

    <template v-if="data">
      <section v-if="section('keuangan')">
        <div class="mb-2 flex items-center gap-2">
          <span class="h-2.5 w-2.5 rounded-full bg-primary-container" />
          <h2 class="font-display text-base font-semibold">Likuiditas & Ringkasan Finansial</h2>
        </div>
        <div class="mb-5 grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-4">
          <AppCard v-for="k in [
            { icon: 'account_balance_wallet', label: 'Kas bersih bulan ini', value: rupiah(section('keuangan')!.kas_bersih_bulan_ini) },
            { icon: 'trending_up', label: 'Laba bersih bulan ini', value: rupiah(section('keuangan')!.laba_bersih_bulan_ini) },
            { icon: 'credit_card', label: 'Total hutang', value: rupiah(section('keuangan')!.hutang_total) },
            { icon: 'receipt', label: 'Total piutang', value: rupiah(section('keuangan')!.piutang_total) },
          ]" :key="k.label">
            <div class="flex items-center gap-2 text-sm text-secondary">
              <span class="flex h-8 w-8 items-center justify-center rounded-lg bg-surface-low text-primary"><AppIcon :name="k.icon" :size="18" /></span>
              {{ k.label }}
            </div>
            <p class="font-display mt-2 text-xl font-bold">{{ k.value }}</p>
          </AppCard>
        </div>
      </section>

      <section v-if="section('penjualan')">
        <div class="mb-2 flex items-center gap-2">
          <span class="h-2.5 w-2.5 rounded-full bg-primary-container" />
          <h2 class="font-display text-base font-semibold">Penjualan Hari Ini</h2>
        </div>
        <div class="mb-5 grid grid-cols-1 gap-3 sm:grid-cols-3">
          <AppCard v-for="k in [
            { icon: 'payments', label: 'Omzet hari ini', value: rupiah(section('penjualan')!.omzet_hari_ini), spark: true },
            { icon: 'receipt_long', label: 'Transaksi hari ini', value: String(section('penjualan')!.transaksi_hari_ini), spark: false },
            { icon: 'point_of_sale', label: 'Shift terbuka', value: String(section('penjualan')!.shift_terbuka), spark: false },
          ]" :key="k.label">
            <div class="flex items-center gap-2 text-sm text-secondary">
              <span class="flex h-8 w-8 items-center justify-center rounded-lg bg-surface-low text-primary"><AppIcon :name="k.icon" :size="18" /></span>
              {{ k.label }}
            </div>
            <p class="font-display mt-2 text-xl font-bold">{{ k.value }}</p>
            <svg v-if="k.spark && tren.length > 1" viewBox="0 0 100 36" class="mt-2 h-9 w-full" aria-hidden="true">
              <path :d="sparkline()" class="fill-primary-container/25" stroke="none" />
            </svg>
            <p v-if="k.spark && tren.length > 1" class="mt-1 text-[11px] text-secondary">Tren omzet harian bulan ini</p>
          </AppCard>
        </div>
      </section>

      <div class="grid gap-3 md:grid-cols-3">
        <AppCard v-if="section('operasional')" title="Perlu perhatian">
          <p class="text-sm">Transfer diajukan: <strong>{{ section('operasional')!.transfer_diajukan ?? 0 }}</strong></p>
          <p class="text-sm">Opname diajukan: <strong>{{ section('operasional')!.opname_diajukan ?? 0 }}</strong></p>
          <RouterLink :to="`/c/${companyId}/approvals`" class="mt-2 inline-block text-sm font-semibold text-primary">Tinjau →</RouterLink>
        </AppCard>
        <AppCard v-if="section('sdm')" title="Kehadiran hari ini">
          <p class="text-sm">Hadir <strong>{{ section('sdm')!.hadir }}</strong> · Terlambat <strong>{{ section('sdm')!.terlambat }}</strong> · Alpa <strong>{{ section('sdm')!.alpa }}</strong></p>
          <p class="text-sm">Cuti diajukan: <strong>{{ section('sdm')!.cuti_diajukan }}</strong></p>
        </AppCard>
        <AppCard v-if="section('karyawan')" title="Karyawan">
          <p class="font-display text-xl font-bold">{{ section('karyawan')!.total }} <span class="text-sm font-normal text-secondary">aktif</span></p>
        </AppCard>
      </div>

      <AppCard v-if="section('penggajian')" title="Periode gaji" class="mt-3">
        <p class="text-sm">{{ Object.entries(section('penggajian')!).map(([k, v]) => `${k}: ${v}`).join(' · ') || 'Belum ada periode.' }}</p>
      </AppCard>
    </template>
  </div>
</template>
