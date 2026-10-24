<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { api } from '../lib/api'
import { rupiah, rupiahRingkas, tanggal } from '../lib/format'
import { labelAgingBucket } from '../lib/labels'
import { useCompany } from '../composables/useCompany'
import { useAccessStore } from '../stores/access'
import { useAsync } from '../composables/useAsync'
import { usePagedList } from '../composables/usePagedList'
import AppCard from '../components/ui/AppCard.vue'
import AppBadge from '../components/ui/AppBadge.vue'
import AppBanner from '../components/ui/AppBanner.vue'
import AppButton from '../components/ui/AppButton.vue'
import AppCheckbox from '../components/ui/AppCheckbox.vue'
import AppEmpty from '../components/ui/AppEmpty.vue'
import AppField from '../components/ui/AppField.vue'
import AppMoneyInput from '../components/ui/AppMoneyInput.vue'
import AppPageHeader from '../components/ui/AppPageHeader.vue'
import AppPager from '../components/ui/AppPager.vue'
import AppSelect from '../components/ui/AppSelect.vue'
import AppSkeleton from '../components/ui/AppSkeleton.vue'
import AppStat from '../components/ui/AppStat.vue'

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
      `/companies/${companyId.value}/supplier-bills?per_page=${perPage}&page=${page}${hanyaBelumLunas.value ? '&unpaid=1' : ''}`,
      auth.token,
    ),
  { perPage: 20 },
)
const { loading: loadingMutasi, error: errorMutasi, run } = useAsync()
const error = computed(() => errorList.value || errorMutasi.value)
const aging = ref<Aging | null>(null)
const bayarId = ref<number | null>(null)
const bayarNominal = ref<number | null>(null)
const bayarCara = ref('tunai')

const bolehBayar = computed(() => access.can('biaya.setujui'))
const BUCKETS = ['lancar', '1_30', '31_60', '61_90', 'lebih_90']
const opsiMetode = [
  { value: 'tunai', label: 'Tunai' },
  { value: 'transfer', label: 'Transfer' },
]

const bucketTone = (bucket: string): 'default' | 'warning' | 'negative' => {
  if (bucket === 'lebih_90') return 'negative'
  if (bucket === 'lancar') return 'default'
  return 'warning'
}

const persenLunas = (t: Tagihan) => {
  const total = Number(t.amount)
  if (!total) return 0
  return Math.min(100, Math.max(0, Math.round((Number(t.paidAmount) / total) * 100)))
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

function gantiFilter(value: boolean) {
  hanyaBelumLunas.value = value
  muatTagihan(1)
}

function mulaiBayar(t: Tagihan) {
  bayarId.value = t.id
  bayarNominal.value = Number(t.remaining)
  bayarCara.value = 'tunai'
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
    bayarNominal.value = null
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
    <AppPageHeader
      eyebrow="Kewajiban ke pemasok"
      title="Hutang pemasok"
      subtitle="Pantau tagihan yang terbentuk dari penerimaan PO dan catat pembayarannya."
      icon="account_balance"
    >
      <template #actions>
        <AppButton variant="ghost" :loading="loading" @click="muat">Muat ulang</AppButton>
      </template>
    </AppPageHeader>

    <AppBanner tone="green" :message="okMsg" />
    <AppBanner tone="red" :message="error" />

    <div v-if="aging" class="mb-4 grid grid-cols-2 gap-3 md:grid-cols-3 2xl:grid-cols-6">
      <AppStat
        v-for="bucket in BUCKETS"
        :key="bucket"
        :label="labelAgingBucket(bucket)"
        :value="rupiahRingkas(aging.buckets[bucket] ?? 0)"
        icon="schedule"
        :tone="bucketTone(bucket)"
      />
      <AppStat
        label="Total hutang"
        :value="rupiahRingkas(aging.total)"
        :hint="rupiah(aging.total)"
        icon="account_balance"
        tone="negative"
      />
    </div>

    <div class="mb-4">
      <AppCheckbox
        :model-value="hanyaBelumLunas"
        label="Hanya yang belum lunas"
        hint="Sembunyikan tagihan yang sudah selesai."
        @update:model-value="gantiFilter"
      />
    </div>

    <div v-if="loading" class="grid gap-3 md:grid-cols-2" aria-busy="true">
      <AppCard v-for="i in 4" :key="i">
        <AppSkeleton :lines="4" />
      </AppCard>
      <span class="sr-only" role="status">Memuat tagihan...</span>
    </div>

    <AppEmpty
      v-else-if="!list.length"
      icon="receipt_long"
      title="Tidak ada tagihan"
      :message="
        hanyaBelumLunas
          ? 'Semua tagihan pada filter ini sudah lunas.'
          : 'Tagihan akan muncul setelah barang dari PO diterima.'
      "
    />

    <div v-else class="grid gap-3 md:grid-cols-2">
      <AppCard v-for="t in list" :key="t.id">
        <div class="flex items-start justify-between gap-3">
          <div class="min-w-0">
            <p class="font-display font-semibold text-ink">Tagihan #{{ t.id }}</p>
            <p class="mt-0.5 text-xs text-secondary">Jatuh tempo {{ tanggal(t.dueAt) }}</p>
          </div>
          <AppBadge :tone="t.isPaid ? 'green' : 'amber'" dot>
            {{ t.isPaid ? 'Lunas' : 'Belum lunas' }}
          </AppBadge>
        </div>

        <div
          class="mt-3 h-2 overflow-hidden rounded-full bg-surface-low"
          role="progressbar"
          :aria-label="`Pembayaran tagihan ${t.id}`"
          aria-valuemin="0"
          aria-valuemax="100"
          :aria-valuenow="persenLunas(t)"
        >
          <div
            class="h-full rounded-full bg-primary-container transition-[width] duration-300"
            :style="{ width: `${persenLunas(t)}%` }"
          />
        </div>

        <div class="mt-2 flex flex-wrap items-baseline justify-between gap-2 text-sm">
          <p class="text-secondary">
            {{ rupiah(t.paidAmount) }} dari {{ rupiah(t.amount) }}
          </p>
          <p class="font-semibold tabular-nums text-ink">Sisa {{ rupiah(t.remaining) }}</p>
        </div>

        <div v-if="!t.isPaid && bolehBayar" class="mt-3 border-t border-outline/50 pt-3">
          <AppButton
            v-if="bayarId !== t.id"
            variant="ghost"
            size="sm"
            @click="mulaiBayar(t)"
          >
            Bayar tagihan
          </AppButton>
          <div v-else class="grid gap-2 sm:grid-cols-2">
            <AppField label="Nominal" required>
              <AppMoneyInput v-model="bayarNominal" />
            </AppField>
            <AppField label="Cara bayar" required>
              <AppSelect v-model="bayarCara" :options="opsiMetode" />
            </AppField>
            <div class="flex flex-wrap gap-2 sm:col-span-2">
              <AppButton
                size="sm"
                :loading="loadingMutasi"
                :disabled="!bayarNominal || bayarNominal <= 0"
                @click="bayar(t)"
              >
                Simpan pembayaran
              </AppButton>
              <AppButton size="sm" variant="ghost" @click="bayarId = null">Batal</AppButton>
            </div>
          </div>
        </div>
      </AppCard>
    </div>

    <AppPager :meta="meta" @page="setPage" @update:per-page="setPerPage" />
  </div>
</template>
