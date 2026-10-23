<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { api } from '../lib/api'
import { rupiah, tanggal } from '../lib/format'
import { useCompany } from '../composables/useCompany'
import { useAccessStore } from '../stores/access'
import { useAsync } from '../composables/useAsync'
import { usePagedList } from '../composables/usePagedList'
import AppCard from '../components/ui/AppCard.vue'
import AppBadge from '../components/ui/AppBadge.vue'
import AppBanner from '../components/ui/AppBanner.vue'
import AppButton from '../components/ui/AppButton.vue'
import AppField from '../components/ui/AppField.vue'
import AppEmpty from '../components/ui/AppEmpty.vue'
import AppPager from '../components/ui/AppPager.vue'

interface Tagihan {
  id: number
  amount: string
  paidAmount: string
  remaining: string
  dueAt?: string | null
  isPaid: boolean
}
interface Aging {
  buckets: Record<string, number>
  total: number
}

const { auth, companyId } = useCompany()
const access = useAccessStore()
const hanyaBelumLunas = ref(true)
const okMsg = ref('')
const {
  rows: list,
  meta,
  loading,
  error: errorList,
  load: muatTagihan,
  setPage,
  setPerPage,
} = usePagedList<Tagihan>(
  (page, perPage) =>
    api.getPage<Tagihan>(
      `/companies/${companyId.value}/supplier-bills?per_page=${perPage}&page=${page}${hanyaBelumLunas.value ? '&unpaid=true' : ''}`,
      auth.token,
    ),
  { perPage: 20 },
)
const { error: errorMutasi, run } = useAsync()
const error = computed(() => errorList.value || errorMutasi.value)
const aging = ref<Aging | null>(null)
const bayarId = ref<number | null>(null)
const bayarNominal = ref(0)
const bayarCara = ref('tunai')

const bolehBayar = computed(() => access.can('biaya.setujui'))

const BUCKET: Record<string, string> = {
  lancar: 'Belum jatuh tempo',
  '1_30': 'Telat 1–30 hari',
  '31_60': 'Telat 31–60 hari',
  '61_90': 'Telat 61–90 hari',
  lebih_90: 'Telat > 90 hari',
}

async function muatAging() {
  const ag = await api
    .get<Aging>(`/companies/${companyId.value}/supplier-bills-aging`, auth.token)
    .catch(() => null)
  if (ag) aging.value = ag as Aging
}

async function muat() {
  await Promise.all([muatTagihan(1), muatAging()])
}

function mulaiBayar(t: Tagihan) {
  bayarId.value = t.id
  bayarNominal.value = Number(t.remaining)
}

async function bayar(t: Tagihan) {
  if (!bayarNominal.value || bayarNominal.value <= 0) return
  const res = await run(() =>
    api.post(
      `/companies/${companyId.value}/supplier-bills/${t.id}/pay`,
      {
        amount: Number(bayarNominal.value),
        method: bayarCara.value,
      },
      auth.token,
    ),
  )
  if (res || !errorMutasi.value) {
    okMsg.value = 'Pembayaran tercatat.'
    bayarId.value = null
    bayarNominal.value = 0
    await Promise.all([muatTagihan(), muatAging()])
  }
}

onMounted(async () => {
  await access.load(companyId.value, auth.token)
  await muat()
})
</script>

<template>
  <div>
    <p class="text-xs font-semibold uppercase tracking-wider text-secondary">
      Kewajiban ke pemasok
    </p>
    <h1 class="font-display mb-4 text-xl font-semibold">Hutang pemasok</h1>
    <AppBanner tone="green" :message="okMsg" />
    <AppBanner tone="red" :message="error" />
    <p class="mb-3 text-xs text-secondary">
      Tagihan terbentuk otomatis saat barang PO diterima — tidak diinput manual.
    </p>

    <div v-if="aging" class="mb-4 grid grid-cols-2 gap-3 lg:grid-cols-6">
      <AppCard v-for="(label, key) in BUCKET" :key="key" class="!p-3">
        <p class="text-xs text-secondary">{{ label }}</p>
        <p class="font-display text-lg font-bold" :class="key === 'lebih_90' ? 'text-danger' : ''">
          {{ rupiah(aging.buckets[key] ?? 0) }}
        </p>
      </AppCard>
      <AppCard class="!p-3"
        ><p class="text-xs text-secondary">Total</p>
        <p class="font-display text-lg font-bold">{{ rupiah(aging.total) }}</p></AppCard
      >
    </div>

    <label class="mb-3 flex items-center gap-2 text-sm">
      <input
        v-model="hanyaBelumLunas"
        type="checkbox"
        class="h-4 w-4 accent-emerald-700"
        @change="muatTagihan(1)"
      />
      Hanya yang belum lunas
    </label>

    <p v-if="loading" class="text-sm text-secondary">Memuat…</p>
    <AppEmpty v-else-if="!list.length" message="Tidak ada tagihan pada filter ini." />
    <div class="grid gap-3 md:grid-cols-2">
      <AppCard v-for="t in list" :key="t.id">
        <div class="flex items-center justify-between gap-2">
          <strong class="font-display">Tagihan #{{ t.id }}</strong>
          <AppBadge :tone="t.isPaid ? 'green' : 'amber'">{{
            t.isPaid ? 'Lunas' : 'Belum lunas'
          }}</AppBadge>
        </div>
        <div class="mt-2 h-2 overflow-hidden rounded bg-surface-low">
          <div
            class="h-full bg-primary-container"
            :style="{
              width: `${Number(t.amount) ? (Number(t.paidAmount) / Number(t.amount)) * 100 : 0}%`,
            }"
          />
        </div>
        <p class="mt-1 text-sm">
          {{ rupiah(t.paidAmount) }} dari {{ rupiah(t.amount) }} · sisa
          <strong>{{ rupiah(t.remaining) }}</strong>
        </p>
        <p class="text-xs text-secondary">Jatuh tempo: {{ tanggal(t.dueAt) }}</p>
        <div v-if="!t.isPaid && bolehBayar" class="mt-2">
          <AppButton v-if="bayarId !== t.id" variant="ghost" @click="mulaiBayar(t)">Bayar</AppButton>
          <div v-else class="flex flex-wrap items-end gap-2">
            <AppField label="Nominal (Rp)"
              ><input
                v-model.number="bayarNominal"
                type="number"
                min="0"
                class="w-32 rounded-lg bg-surface-low px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-primary"
            /></AppField>
            <AppField label="Cara"
              ><select v-model="bayarCara" class="rounded-lg bg-surface-low px-3 py-2 text-sm">
                <option value="tunai">Tunai</option>
                <option value="transfer">Transfer</option>
              </select></AppField
            >
            <AppButton @click="bayar(t)">Simpan</AppButton>
            <AppButton variant="ghost" @click="bayarId = null">Batal</AppButton>
          </div>
        </div>
      </AppCard>
    </div>
    <AppPager :meta="meta" @page="setPage" @update:per-page="setPerPage" />
  </div>
</template>
