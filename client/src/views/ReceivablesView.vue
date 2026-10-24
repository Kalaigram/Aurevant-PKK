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
import AppDateInput from '../components/ui/AppDateInput.vue'
import AppEmpty from '../components/ui/AppEmpty.vue'
import AppField from '../components/ui/AppField.vue'
import AppInput from '../components/ui/AppInput.vue'
import AppModal from '../components/ui/AppModal.vue'
import AppMoneyInput from '../components/ui/AppMoneyInput.vue'
import AppPageHeader from '../components/ui/AppPageHeader.vue'
import AppPager from '../components/ui/AppPager.vue'
import AppSelect from '../components/ui/AppSelect.vue'
import AppSkeleton from '../components/ui/AppSkeleton.vue'
import AppStat from '../components/ui/AppStat.vue'

interface Bon {
  id: number
  branchId?: number
  customerId?: number | null
  amount: string
  paidAmount: string
  remaining: string
  dueAt?: string | null
  isPaid: boolean
}
interface Cabang {
  id: number
  name: string
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
  load: muatBon,
  setPage,
  setPerPage,
} = usePagedList<Bon>(
  (page, perPage) =>
    api.getPage<Bon>(
      `/companies/${companyId.value}/receivables?per_page=${perPage}&page=${page}${hanyaBelumLunas.value ? '&unpaid=1' : ''}`,
      auth.token,
    ),
  { perPage: 20 },
)
const { loading: loadingMutasi, error: errorMutasi, run } = useAsync()
const error = computed(() => errorList.value || errorMutasi.value)
const aging = ref<{ buckets: Record<string, number>; total: number } | null>(null)
const cabang = ref<Cabang[]>([])
const showForm = ref(false)
const fBon = ref(formKosong())
const bayarId = ref<number | null>(null)
const bayarNominal = ref<number | null>(null)
const bayarCara = ref('tunai')

const boleh = computed(() => access.can('biaya.setujui'))
const BUCKETS = ['lancar', '1_30', '31_60', '61_90', 'lebih_90']
const opsiMetode = [
  { value: 'tunai', label: 'Tunai' },
  { value: 'transfer', label: 'Transfer' },
  { value: 'qris', label: 'QRIS' },
  { value: 'ewallet', label: 'E-wallet' },
]
const opsiCabang = computed(() =>
  cabang.value.map((c) => ({ value: c.id, label: c.name })),
)

const bucketTone = (bucket: string): 'default' | 'warning' | 'negative' => {
  if (bucket === 'lebih_90') return 'negative'
  if (bucket === 'lancar') return 'default'
  return 'warning'
}

const persenLunas = (b: Bon) => {
  const total = Number(b.amount)
  if (!total) return 0
  return Math.min(100, Math.max(0, Math.round((Number(b.paidAmount) / total) * 100)))
}

function formKosong() {
  return {
    branch_id: null as number | null,
    customer_id: '',
    amount: null as number | null,
    due_at: '',
    note: '',
  }
}

function bukaForm() {
  fBon.value = formKosong()
  showForm.value = true
}

function gantiFilter(value: boolean) {
  hanyaBelumLunas.value = value
  muatBon(1)
}

async function muatAging() {
  const ag = await api
    .get<{ buckets: Record<string, number>; total: number }>(
      `/companies/${companyId.value}/receivables-aging`,
      auth.token,
    )
    .catch(() => null)
  if (ag) aging.value = ag as { buckets: Record<string, number>; total: number }
}

async function muatCabang() {
  cabang.value =
    (await api
      .get<Cabang[]>(`/companies/${companyId.value}/branches`, auth.token)
      .catch(() => [])) ?? []
}

async function muat() {
  await Promise.all([muatBon(1), muatAging(), muatCabang()])
}

async function simpan() {
  if (!fBon.value.branch_id || !fBon.value.amount) return
  const res = await run(() =>
    api.post(
      `/companies/${companyId.value}/receivables`,
      {
        branch_id: fBon.value.branch_id,
        customer_id: fBon.value.customer_id === '' ? undefined : Number(fBon.value.customer_id),
        amount: fBon.value.amount,
        due_at: fBon.value.due_at || undefined,
        note: fBon.value.note || undefined,
      },
      auth.token,
    ),
  )
  if (res) {
    showForm.value = false
    okMsg.value = 'Bon pelanggan tercatat.'
    fBon.value = formKosong()
    await Promise.all([muatBon(1), muatAging()])
  }
}

function mulaiTerima(b: Bon) {
  bayarId.value = b.id
  bayarNominal.value = Number(b.remaining)
  bayarCara.value = 'tunai'
}

async function terima(b: Bon) {
  if (!bayarNominal.value || bayarNominal.value <= 0) return
  const res = await run(() =>
    api.post(
      `/companies/${companyId.value}/receivables/${b.id}/pay`,
      {
        amount: Number(bayarNominal.value),
        method: bayarCara.value,
      },
      auth.token,
    ),
  )
  if (res || !errorMutasi.value) {
    okMsg.value = 'Cicilan tercatat.'
    bayarId.value = null
    bayarNominal.value = null
    await Promise.all([muatBon(), muatAging()])
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
      eyebrow="Tagihan ke pelanggan"
      title="Piutang (bon)"
      subtitle="Catat bon pelanggan dan pantau cicilan yang sudah diterima."
      icon="request_quote"
    >
      <template #actions>
        <AppButton v-if="boleh" @click="bukaForm">Catat bon</AppButton>
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
        label="Total piutang"
        :value="rupiahRingkas(aging.total)"
        :hint="rupiah(aging.total)"
        icon="account_balance_wallet"
        tone="negative"
      />
    </div>

    <div class="mb-4">
      <AppCheckbox
        :model-value="hanyaBelumLunas"
        label="Hanya yang belum lunas"
        hint="Sembunyikan bon yang sudah selesai."
        @update:model-value="gantiFilter"
      />
    </div>

    <div v-if="loading" class="grid gap-3 md:grid-cols-2" aria-busy="true">
      <AppCard v-for="i in 4" :key="i">
        <AppSkeleton :lines="4" />
      </AppCard>
      <span class="sr-only" role="status">Memuat piutang...</span>
    </div>

    <AppEmpty
      v-else-if="!list.length"
      icon="request_quote"
      title="Tidak ada bon"
      :message="
        hanyaBelumLunas
          ? 'Semua bon pada filter ini sudah lunas.'
          : 'Bon pelanggan yang dicatat akan muncul di sini.'
      "
    >
      <AppButton v-if="boleh" @click="bukaForm">Catat bon</AppButton>
    </AppEmpty>

    <div v-else class="grid gap-3 md:grid-cols-2">
      <AppCard v-for="b in list" :key="b.id">
        <div class="flex items-start justify-between gap-3">
          <div class="min-w-0">
            <p class="font-display font-semibold text-ink">Bon #{{ b.id }}</p>
            <p class="mt-0.5 text-xs text-secondary">
              {{ b.customerId ? `Pelanggan #${b.customerId}` : 'Tanpa nama pelanggan' }}
            </p>
          </div>
          <AppBadge :tone="b.isPaid ? 'green' : 'amber'" dot>
            {{ b.isPaid ? 'Lunas' : 'Belum lunas' }}
          </AppBadge>
        </div>

        <div
          class="mt-3 h-2 overflow-hidden rounded-full bg-surface-low"
          role="progressbar"
          :aria-label="`Pembayaran bon ${b.id}`"
          aria-valuemin="0"
          aria-valuemax="100"
          :aria-valuenow="persenLunas(b)"
        >
          <div
            class="h-full rounded-full bg-primary-container transition-[width] duration-300"
            :style="{ width: `${persenLunas(b)}%` }"
          />
        </div>

        <div class="mt-2 flex flex-wrap items-baseline justify-between gap-2 text-sm">
          <p class="text-secondary">
            {{ rupiah(b.paidAmount) }} dari {{ rupiah(b.amount) }}
          </p>
          <p class="font-semibold tabular-nums text-ink">Sisa {{ rupiah(b.remaining) }}</p>
        </div>
        <p class="mt-1 text-xs text-secondary">Jatuh tempo {{ tanggal(b.dueAt) }}</p>

        <div v-if="!b.isPaid && boleh" class="mt-3 border-t border-outline/50 pt-3">
          <AppButton
            v-if="bayarId !== b.id"
            variant="ghost"
            size="sm"
            @click="mulaiTerima(b)"
          >
            Terima cicilan
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
                @click="terima(b)"
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

    <AppModal
      :open="showForm"
      title="Catat bon baru"
      description="Bon akan masuk ke daftar piutang dan bisa dicicil bertahap."
      size="md"
      @close="showForm = false"
    >
      <AppBanner tone="red" :message="errorMutasi" />
      <form id="receivable-form" class="grid gap-3" @submit.prevent="simpan">
        <div class="grid gap-3 sm:grid-cols-2">
          <AppField label="Cabang" required>
            <AppSelect
              v-model="fBon.branch_id"
              :options="opsiCabang"
              placeholder="Pilih cabang"
            />
          </AppField>
          <AppField label="ID pelanggan" optional hint="Kosongkan untuk bon tanpa nama.">
            <AppInput
              v-model="fBon.customer_id"
              inputmode="numeric"
              placeholder="cth. 12"
            />
          </AppField>
          <AppField label="Nominal" required>
            <AppMoneyInput v-model="fBon.amount" />
          </AppField>
          <AppField label="Jatuh tempo" optional>
            <AppDateInput v-model="fBon.due_at" />
          </AppField>
          <AppField label="Catatan" optional class="sm:col-span-2">
            <AppInput v-model="fBon.note" maxlength="500" placeholder="Tambahkan konteks bon" />
          </AppField>
        </div>
      </form>
      <template #footer>
        <AppButton variant="ghost" @click="showForm = false">Batal</AppButton>
        <AppButton
          type="submit"
          form="receivable-form"
          :loading="loadingMutasi"
          :disabled="!fBon.branch_id || !fBon.amount"
        >
          Simpan bon
        </AppButton>
      </template>
    </AppModal>
  </div>
</template>
