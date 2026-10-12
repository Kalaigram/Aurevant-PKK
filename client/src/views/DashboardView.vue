<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { api } from '../lib/api'
import { rupiah } from '../lib/format'
import { useCompany } from '../composables/useCompany'
import { useAsync } from '../composables/useAsync'
import AppCard from '../components/ui/AppCard.vue'
import AppBadge from '../components/ui/AppBadge.vue'
import AppBanner from '../components/ui/AppBanner.vue'

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

const subStatusTone = (s: unknown) =>
  s === 'aktif' || s === 'trial' ? 'green' : s === 'tenggang' ? 'amber' : 'red'
</script>

<template>
  <div>
    <h1 class="mb-1 text-xl font-bold">Dashboard</h1>
    <p v-if="data" class="mb-4 text-sm text-slate-500">
      Periode {{ (data as Record<string, string>).month_start }} s/d {{ (data as Record<string, string>).today }}
      · Langganan <AppBadge :tone="subStatusTone((data.subscription as Record<string, string>)?.status)">
        {{ (data.subscription as Record<string, string>)?.status ?? '-' }}
      </AppBadge>
    </p>
    <AppBanner tone="red" :message="error" />
    <p v-if="loading" class="text-sm text-slate-500">Memuat…</p>

    <template v-if="data">
      <div v-if="section('keuangan')" class="mb-4 grid gap-3 md:grid-cols-4">
        <AppCard><p class="text-sm text-slate-500">Kas bersih bulan ini</p><p class="text-xl font-extrabold">{{ rupiah(section('keuangan')!.kas_bersih_bulan_ini) }}</p></AppCard>
        <AppCard><p class="text-sm text-slate-500">Laba bersih bulan ini</p><p class="text-xl font-extrabold">{{ rupiah(section('keuangan')!.laba_bersih_bulan_ini) }}</p></AppCard>
        <AppCard><p class="text-sm text-slate-500">Total hutang</p><p class="text-xl font-extrabold">{{ rupiah(section('keuangan')!.hutang_total) }}</p></AppCard>
        <AppCard><p class="text-sm text-slate-500">Total piutang</p><p class="text-xl font-extrabold">{{ rupiah(section('keuangan')!.piutang_total) }}</p></AppCard>
      </div>

      <div v-if="section('penjualan')" class="mb-4 grid gap-3 md:grid-cols-3">
        <AppCard><p class="text-sm text-slate-500">Omzet hari ini</p><p class="text-xl font-extrabold">{{ rupiah(section('penjualan')!.omzet_hari_ini) }}</p></AppCard>
        <AppCard><p class="text-sm text-slate-500">Transaksi hari ini</p><p class="text-xl font-extrabold">{{ section('penjualan')!.transaksi_hari_ini }}</p></AppCard>
        <AppCard><p class="text-sm text-slate-500">Shift terbuka</p><p class="text-xl font-extrabold">{{ section('penjualan')!.shift_terbuka }}</p></AppCard>
      </div>

      <div class="grid gap-3 md:grid-cols-3">
        <AppCard v-if="section('operasional')" title="Perlu perhatian">
          <p class="text-sm">Transfer diajukan: <strong>{{ section('operasional')!.transfer_diajukan ?? 0 }}</strong></p>
          <p class="text-sm">Opname diajukan: <strong>{{ section('operasional')!.opname_diajukan ?? 0 }}</strong></p>
        </AppCard>
        <AppCard v-if="section('sdm')" title="Kehadiran hari ini">
          <p class="text-sm">Hadir <strong>{{ section('sdm')!.hadir }}</strong> · Terlambat <strong>{{ section('sdm')!.terlambat }}</strong> · Alpa <strong>{{ section('sdm')!.alpa }}</strong></p>
          <p class="text-sm">Cuti diajukan: <strong>{{ section('sdm')!.cuti_diajukan }}</strong></p>
        </AppCard>
        <AppCard v-if="section('karyawan')" title="Karyawan">
          <p class="text-xl font-extrabold">{{ section('karyawan')!.total }} <span class="text-sm font-normal text-slate-500">aktif</span></p>
        </AppCard>
      </div>

      <AppCard v-if="section('penggajian')" title="Periode gaji" class="mt-3">
        <p class="text-sm">{{ Object.entries(section('penggajian')!).map(([k, v]) => `${k}: ${v}`).join(' · ') || 'Belum ada periode.' }}</p>
      </AppCard>
    </template>
  </div>
</template>
