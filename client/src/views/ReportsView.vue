<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { api } from '../lib/api'
import { rupiah, tanggal } from '../lib/format'
import { useCompany } from '../composables/useCompany'
import { useAccessStore } from '../stores/access'
import { useAsync } from '../composables/useAsync'
import AppCard from '../components/ui/AppCard.vue'
import AppBanner from '../components/ui/AppBanner.vue'
import AppButton from '../components/ui/AppButton.vue'
import AppField from '../components/ui/AppField.vue'
import AppEmpty from '../components/ui/AppEmpty.vue'

interface Cabang {
  id: number
  name: string
}

const { auth, companyId } = useCompany()
const access = useAccessStore()
const { loading, error, run } = useAsync()
const tab = ref<'labarugi' | 'harian' | 'kas' | 'neraca' | 'konsolidasi'>('labarugi')
const cabang = ref<Cabang[]>([])
const branchId = ref('')
const dari = ref(new Date().toISOString().slice(0, 10))
const sampai = ref(new Date().toISOString().slice(0, 10))
const hasil = ref<Record<string, unknown> | null>(null)
const okMsg = ref('')

const bolehKeuangan = computed(() => access.can('keuangan.lihat'))
const bolehKonsolidasi = computed(() => access.can('laporan.konsolidasi'))

const cabangQ = computed(() => (branchId.value === '' ? '' : `&branch_id=${branchId.value}`))

async function tampilkan() {
  okMsg.value = ''
  hasil.value = null
  const base = `/companies/${companyId.value}`
  const q = tab.value === 'neraca' ? '' : `?from=${dari.value}&to=${sampai.value}${cabangQ.value}`
  const path =
    tab.value === 'labarugi'
      ? `${base}/reports/profit-loss${q}`
      : tab.value === 'harian'
        ? `${base}/reports/daily${q}`
        : tab.value === 'kas'
          ? `${base}/reports/cash-flow${q}`
          : tab.value === 'neraca'
            ? `${base}/reports/balance-sheet?as_of=${sampai.value}`
            : `${base}/reports/consolidation`
  const res = await run(() => api.get<Record<string, unknown>>(path, auth.token))
  if (res) hasil.value = res as Record<string, unknown>
}

async function segarkan() {
  const res = await run(() =>
    api.post(`/companies/${companyId.value}/reports/refresh`, {}, auth.token),
  )
  if (res || !error.value) {
    okMsg.value = 'Ringkasan harian dibangun ulang.'
    tampilkan()
  }
}

const angka = (v: unknown) => (v === null || v === undefined || v === '' ? 0 : Number(v))
const rObj = (v: unknown) =>
  v !== null && typeof v === 'object' ? (v as Record<string, unknown>) : {}
const aktivaTotal = computed(() => angka(rObj(hasil.value?.['aktiva'])['total']))
const kewajibanTotal = computed(() => angka(rObj(hasil.value?.['kewajiban'])['total']))
const modalTotal = computed(() => angka(hasil.value?.['modal']))
const neracaSeimbang = computed(() => aktivaTotal.value === kewajibanTotal.value + modalTotal.value)
const neracaTanggal = computed(() => String(hasil.value?.['as_of'] ?? sampai.value))
const rincianAkun = computed(() => {
  const d = hasil.value?.['detail']
  return Array.isArray(d) ? (d as { code: string; name: string; saldo: string | number }[]) : []
})

onMounted(async () => {
  await access.load(companyId.value, auth.token)
  cabang.value =
    (await api
      .get<Cabang[]>(`/companies/${companyId.value}/branches`, auth.token)
      .catch(() => [])) ?? []
  await tampilkan()
})
</script>

<template>
  <div>
    <p class="text-xs font-semibold uppercase tracking-wider text-secondary">Kinerja usaha</p>
    <h1 class="font-display mb-4 text-xl font-semibold">Laporan keuangan</h1>
    <AppBanner tone="green" :message="okMsg" />
    <AppBanner tone="red" :message="error" />
    <AppEmpty
      v-if="!bolehKeuangan"
      message="Halaman ini hanya untuk yang diberi wewenang melihat keuangan."
    />
    <template v-else>
      <div class="mb-3 flex flex-wrap gap-2">
        <AppButton :variant="tab === 'labarugi' ? 'primary' : 'ghost'" @click="tab = 'labarugi'"
          >Laba rugi</AppButton
        >
        <AppButton :variant="tab === 'harian' ? 'primary' : 'ghost'" @click="tab = 'harian'"
          >Harian</AppButton
        >
        <AppButton :variant="tab === 'kas' ? 'primary' : 'ghost'" @click="tab = 'kas'"
          >Arus kas</AppButton
        >
        <AppButton :variant="tab === 'neraca' ? 'primary' : 'ghost'" @click="tab = 'neraca'"
          >Neraca</AppButton
        >
        <AppButton
          v-if="bolehKonsolidasi"
          :variant="tab === 'konsolidasi' ? 'primary' : 'ghost'"
          @click="tab = 'konsolidasi'"
          >Konsolidasi</AppButton
        >
      </div>

      <div class="mb-3 flex flex-wrap items-end gap-2">
        <template v-if="tab !== 'konsolidasi' && tab !== 'neraca'">
          <AppField label="Dari"
            ><input
              v-model="dari"
              type="date"
              class="rounded-lg border border-outline bg-card px-2 py-2 text-sm"
          /></AppField>
          <AppField label="Sampai"
            ><input
              v-model="sampai"
              type="date"
              class="rounded-lg border border-outline bg-card px-2 py-2 text-sm"
          /></AppField>
        </template>
        <AppField v-if="tab === 'neraca'" label="Per tanggal"
          ><input
            v-model="sampai"
            type="date"
            class="rounded-lg border border-outline bg-card px-2 py-2 text-sm"
        /></AppField>
        <AppField v-if="tab !== 'konsolidasi' && tab !== 'neraca'" label="Cabang"
          ><select
            v-model="branchId"
            class="rounded-lg border border-outline bg-card px-2 py-2 text-sm"
          >
            <option value="">Semua cabang</option>
            <option v-for="c in cabang" :key="c.id" :value="c.id">{{ c.name }}</option>
          </select></AppField
        >
        <AppButton @click="tampilkan">Tampilkan</AppButton>
        <AppButton
          v-if="tab === 'konsolidasi' && bolehKonsolidasi"
          variant="ghost"
          @click="segarkan"
          >Bangun ulang ringkasan</AppButton
        >
      </div>

      <p v-if="loading" class="text-sm text-secondary">Menghitung…</p>
      <template v-else-if="hasil">
        <div v-if="tab === 'labarugi'" class="grid gap-3 md:grid-cols-3">
          <AppCard
            ><p class="text-xs uppercase tracking-wider text-secondary">Pendapatan (4xxx)</p>
            <p class="font-display text-2xl font-bold">
              {{ rupiah(angka(hasil['pendapatan'])) }}
            </p></AppCard
          >
          <AppCard
            ><p class="text-xs uppercase tracking-wider text-secondary">HPP (51xx)</p>
            <p class="font-display text-2xl font-bold">
              {{ rupiah(angka(hasil['hpp'])) }}
            </p></AppCard
          >
          <AppCard
            ><p class="text-xs uppercase tracking-wider text-secondary">Laba kotor</p>
            <p class="font-display text-2xl font-bold">
              {{ rupiah(angka(hasil['laba_kotor'])) }}
            </p></AppCard
          >
          <AppCard
            ><p class="text-xs uppercase tracking-wider text-secondary">Beban (6xxx)</p>
            <p class="font-display text-2xl font-bold">
              {{ rupiah(angka(hasil['beban'])) }}
            </p></AppCard
          >
          <AppCard
            ><p class="text-xs uppercase tracking-wider text-secondary">Laba bersih</p>
            <p class="font-display text-2xl font-bold text-primary">
              {{ rupiah(angka(hasil['laba_bersih'])) }}
            </p></AppCard
          >
        </div>
        <div v-if="tab === 'kas'" class="grid gap-3 md:grid-cols-3">
          <AppCard
            ><p class="text-xs uppercase tracking-wider text-secondary">Kas masuk</p>
            <p class="font-display text-2xl font-bold">
              {{ rupiah(angka(hasil['kas_masuk'])) }}
            </p></AppCard
          >
          <AppCard
            ><p class="text-xs uppercase tracking-wider text-secondary">Kas keluar</p>
            <p class="font-display text-2xl font-bold">
              {{ rupiah(angka(hasil['kas_keluar'])) }}
            </p></AppCard
          >
          <AppCard
            ><p class="text-xs uppercase tracking-wider text-secondary">Kas bersih</p>
            <p class="font-display text-2xl font-bold text-primary">
              {{ rupiah(angka(hasil['kas_bersih'])) }}
            </p></AppCard
          >
        </div>
        <AppCard v-if="tab === 'neraca'">
          <h2 class="font-display font-semibold">Neraca per {{ tanggal(neracaTanggal) }}</h2>
          <div class="mt-2 grid gap-3 md:grid-cols-3">
            <div>
              <p class="text-xs uppercase tracking-wider text-secondary">Aktiva</p>
              <p class="font-display text-xl font-bold">{{ rupiah(aktivaTotal) }}</p>
            </div>
            <div>
              <p class="text-xs uppercase tracking-wider text-secondary">Kewajiban</p>
              <p class="font-display text-xl font-bold">{{ rupiah(kewajibanTotal) }}</p>
            </div>
            <div>
              <p class="text-xs uppercase tracking-wider text-secondary">Modal</p>
              <p class="font-display text-xl font-bold">{{ rupiah(modalTotal) }}</p>
            </div>
          </div>
          <p class="mt-2 text-xs" :class="neracaSeimbang ? 'text-primary' : 'text-danger'">
            Aktiva = Kewajiban + Modal — {{ neracaSeimbang ? 'seimbang' : 'tidak seimbang' }}.
          </p>
        </AppCard>
        <AppCard v-if="tab === 'harian' || tab === 'konsolidasi'">
          <h2 class="font-display font-semibold">
            {{ tab === 'harian' ? 'Ringkasan harian per cabang' : 'Konsolidasi semua cabang' }}
          </h2>
          <pre class="mt-2 max-h-96 overflow-auto rounded-lg bg-surface-low p-3 text-xs">{{
            JSON.stringify(hasil, null, 2)
          }}</pre>
        </AppCard>
        <AppCard v-if="tab === 'labarugi' && rincianAkun.length">
          <h2 class="font-display font-semibold">Rincian akun</h2>
          <table class="mt-2 w-full text-sm">
            <tbody>
              <tr v-for="(d, i) in rincianAkun" :key="i" class="border-t border-outline/50">
                <td class="py-1 font-mono text-xs text-secondary">{{ d.code }}</td>
                <td class="py-1">{{ d.name }}</td>
                <td class="py-1 text-right">{{ rupiah(d.saldo) }}</td>
              </tr>
            </tbody>
          </table>
        </AppCard>
      </template>
    </template>
  </div>
</template>
