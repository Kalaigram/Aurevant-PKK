<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { api } from '../lib/api'
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

onMounted(async () => {
  data.value = await run(() => api.get<Record<string, unknown>>(`/companies/${companyId.value}/dashboard`, auth.token))
})

function section(key: string): Record<string, number> | null {
  const s = data.value?.[key]
  return s !== null && typeof s === 'object' ? (s as Record<string, number>) : null
}

const sub = () => (data.value?.subscription ?? {}) as Record<string, string>
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
            <h1 class="font-display text-lg font-semibold">{{ (data?.company as Record<string, string>)?.name ?? '…' }}</h1>
            <AppBadge :tone="subTone()">{{ sub().status ?? '…' }}</AppBadge>
          </div>
          <p class="text-sm text-secondary">
            Langganan sampai <strong class="text-ink">{{ sub().endsAt ?? sub().trialEndsAt ?? '–' }}</strong>
            · Periode {{ (data as Record<string, string>)?.month_start }} s/d {{ (data as Record<string, string>)?.today }}
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
          <AppCard>
            <div class="flex items-center gap-2 text-sm text-secondary"><AppIcon name="account_balance_wallet" :size="18" />Kas bersih bulan ini</div>
            <p class="font-display mt-1 text-xl font-bold">{{ rupiah(section('keuangan')!.kas_bersih_bulan_ini) }}</p>
          </AppCard>
          <AppCard>
            <div class="flex items-center gap-2 text-sm text-secondary"><AppIcon name="trending_up" :size="18" />Laba bersih bulan ini</div>
            <p class="font-display mt-1 text-xl font-bold">{{ rupiah(section('keuangan')!.laba_bersih_bulan_ini) }}</p>
          </AppCard>
          <AppCard>
            <div class="flex items-center gap-2 text-sm text-secondary"><AppIcon name="credit_card" :size="18" />Total hutang</div>
            <p class="font-display mt-1 text-xl font-bold">{{ rupiah(section('keuangan')!.hutang_total) }}</p>
          </AppCard>
          <AppCard>
            <div class="flex items-center gap-2 text-sm text-secondary"><AppIcon name="receipt" :size="18" />Total piutang</div>
            <p class="font-display mt-1 text-xl font-bold">{{ rupiah(section('keuangan')!.piutang_total) }}</p>
          </AppCard>
        </div>
      </section>

      <section v-if="section('penjualan')">
        <div class="mb-2 flex items-center gap-2">
          <span class="h-2.5 w-2.5 rounded-full bg-primary-container" />
          <h2 class="font-display text-base font-semibold">Penjualan Hari Ini</h2>
        </div>
        <div class="mb-5 grid grid-cols-1 gap-3 sm:grid-cols-3">
          <AppCard>
            <div class="flex items-center gap-2 text-sm text-secondary"><AppIcon name="payments" :size="18" />Omzet</div>
            <p class="font-display mt-1 text-xl font-bold">{{ rupiah(section('penjualan')!.omzet_hari_ini) }}</p>
          </AppCard>
          <AppCard>
            <div class="flex items-center gap-2 text-sm text-secondary"><AppIcon name="receipt_long" :size="18" />Transaksi</div>
            <p class="font-display mt-1 text-xl font-bold">{{ section('penjualan')!.transaksi_hari_ini }}</p>
          </AppCard>
          <AppCard>
            <div class="flex items-center gap-2 text-sm text-secondary"><AppIcon name="point_of_sale" :size="18" />Shift terbuka</div>
            <p class="font-display mt-1 text-xl font-bold">{{ section('penjualan')!.shift_terbuka }}</p>
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
