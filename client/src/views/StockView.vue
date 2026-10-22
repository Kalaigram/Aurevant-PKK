<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { api, type PageMeta } from '../lib/api'
import { rupiah } from '../lib/format'
import { useCompany } from '../composables/useCompany'
import { useAccessStore } from '../stores/access'
import { useAsync } from '../composables/useAsync'
import { useToast } from '../composables/useToast'
import AppBadge from '../components/ui/AppBadge.vue'
import AppBanner from '../components/ui/AppBanner.vue'
import AppButton from '../components/ui/AppButton.vue'
import AppCard from '../components/ui/AppCard.vue'
import AppField from '../components/ui/AppField.vue'
import AppInput from '../components/ui/AppInput.vue'
import AppModal from '../components/ui/AppModal.vue'
import AppMoneyInput from '../components/ui/AppMoneyInput.vue'
import AppPageHeader from '../components/ui/AppPageHeader.vue'
import AppPager from '../components/ui/AppPager.vue'
import AppSearch from '../components/ui/AppSearch.vue'
import AppSelect from '../components/ui/AppSelect.vue'
import AppTable from '../components/ui/AppTable.vue'
import AppTabs from '../components/ui/AppTabs.vue'
import AppTextarea from '../components/ui/AppTextarea.vue'

type Tone = 'green' | 'red' | 'amber' | 'blue' | 'gray'

interface Saldo {
  itemId: number
  itemCode: string
  itemName: string
  unit: string | null
  qty: string
  avgCost: string
}

interface Gerak {
  id: number
  itemId: number
  itemCode: string | null
  itemName: string | null
  unit: string | null
  type: string
  qty: string
  unitCost: string | null
  hppAmount: string | null
  balanceAfter: string
  note: string | null
  createdAt: string
}

interface Cabang {
  id: number
  code: string
  name: string
}

interface KatalogItem {
  id: number
  code: string
  name: string
  unit: string | null
  trackStock?: boolean
}

const { auth, companyId } = useCompany()
const access = useAccessStore()
const toast = useToast()
const saldoAsync = useAsync()
const mutasiAsync = useAsync()
const referensi = useAsync()
const aksi = useAsync()

const cabang = ref<Cabang[]>([])
const items = ref<KatalogItem[]>([])
const branchId = ref<number | null>(null)
const tab = ref('saldo')
const cari = ref('')
const perPage = ref(25)
const saldo = ref<Saldo[]>([])
const saldoMeta = ref<PageMeta | null>(null)
const gerak = ref<Gerak[]>([])
const mutasiMeta = ref<PageMeta | null>(null)

const showSesuaikan = ref(false)
const fItem = ref<number | null>(null)
const fTipe = ref('pembelian')
const fQty = ref<number | null>(null)
const fCost = ref<number | null>(null)
const fNote = ref('')

const bolehSesuaikan = computed(() => access.can('stok.opname'))
const error = computed(
  () => saldoAsync.error.value || mutasiAsync.error.value || referensi.error.value,
)
const tabs = [
  { key: 'saldo', label: 'Saldo' },
  { key: 'riwayat', label: 'Riwayat mutasi' },
]
const opsiCabang = computed(() =>
  cabang.value.map((b) => ({ value: b.id, label: `${b.code} — ${b.name}` })),
)
const opsiItem = computed(() =>
  items.value
    .filter((it) => it.trackStock !== false)
    .map((it) => ({
      value: it.id,
      label: `${it.code} — ${it.name}${it.unit ? ` (${it.unit})` : ''}`,
    })),
)

const tipe = computed(() =>
  fTipe.value === 'pembelian'
    ? { label: 'Masuk', hint: 'Qty menambah stok dan memperbarui HPP.' }
    : fTipe.value === 'penjualan'
      ? { label: 'Keluar', hint: 'Qty mengurangi stok dan memakai HPP rata-rata.' }
      : { label: 'Koreksi', hint: 'Qty positif menambah, negatif mengurangi stok.' },
)

const sesuaikanSah = computed(
  () =>
    fItem.value !== null &&
    fQty.value !== null &&
    fQty.value !== 0 &&
    (fTipe.value !== 'pembelian' || (fCost.value !== null && fCost.value >= 0)),
)

const totalNilai = computed(() =>
  saldo.value.reduce(
    (total, row) => total + Number(row.qty) * Number(row.avgCost ?? 0),
    0,
  ),
)

function tone(type: string): Tone {
  if (['pembelian', 'transfer_masuk', 'retur', 'void'].includes(type)) return 'green'
  if (['penjualan', 'transfer_keluar', 'pemakaian_bahan'].includes(type)) return 'red'
  if (['penyesuaian', 'opname'].includes(type)) return 'amber'
  return 'blue'
}

const tipeLabel: Record<string, string> = {
  pembelian: 'Masuk pembelian',
  penjualan: 'Keluar penjualan',
  penyesuaian: 'Koreksi manual',
  opname: 'Opname',
  transfer_masuk: 'Transfer masuk',
  transfer_keluar: 'Transfer keluar',
  retur: 'Retur',
  void: 'Void',
  pemakaian_bahan: 'Pemakaian bahan',
}

function labelTipe(type: string) {
  return tipeLabel[type] ?? type
}

function waktu(iso: string | null | undefined) {
  if (!iso) return '—'
  return new Date(iso).toLocaleString('id-ID', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}

function qty(value: string | number | null | undefined) {
  return Number(value ?? 0).toLocaleString('id-ID', { maximumFractionDigits: 3 })
}

function qtyBersign(value: string | number) {
  const number = Number(value)
  const text = qty(Math.abs(number))
  return number > 0 ? `+${text}` : number < 0 ? `-${text}` : '0'
}

async function muatSaldo(page = saldoMeta.value?.current_page ?? 1) {
  if (!branchId.value) return
  const params = new URLSearchParams({
    per_page: String(perPage.value),
    page: String(page),
  })
  if (cari.value.trim()) params.set('q', cari.value.trim())
  const res = await saldoAsync.run(() =>
    api.getPage<Saldo>(
      `/companies/${companyId.value}/branches/${branchId.value}/stock?${params}`,
      auth.token,
    ),
  )
  if (res) {
    saldo.value = res.data
    saldoMeta.value = res.meta
  }
}

async function muatRiwayat(page = mutasiMeta.value?.current_page ?? 1) {
  if (!branchId.value) return
  const params = new URLSearchParams({
    per_page: String(perPage.value),
    page: String(page),
  })
  if (cari.value.trim()) params.set('q', cari.value.trim())
  const res = await mutasiAsync.run(() =>
    api.getPage<Gerak>(
      `/companies/${companyId.value}/branches/${branchId.value}/stock/movements?${params}`,
      auth.token,
    ),
  )
  if (res) {
    gerak.value = res.data
    mutasiMeta.value = res.meta
  }
}

function muatAktif(page = 1) {
  if (tab.value === 'saldo') void muatSaldo(page)
  else void muatRiwayat(page)
}

function cariSekarang() {
  muatAktif(1)
}

function gantiTab(key: string) {
  tab.value = key
  cari.value = ''
  muatAktif(1)
}

function gantiCabang(value: string | number | null) {
  branchId.value = value === null ? null : Number(value)
  saldo.value = []
  gerak.value = []
  if (tab.value === 'saldo') void muatSaldo(1)
  else void muatRiwayat(1)
}

function gantiPerPage(value: number) {
  perPage.value = value
  muatAktif(1)
}

function resetFilter() {
  cari.value = ''
  muatAktif(1)
}

function bukaSesuaikan() {
  fItem.value = null
  fTipe.value = 'pembelian'
  fQty.value = null
  fCost.value = null
  fNote.value = ''
  aksi.error.value = ''
  showSesuaikan.value = true
}

async function simpanSesuai() {
  if (!branchId.value || !sesuaikanSah.value) return
  const res = await aksi.run(() =>
    api.post(
      `/companies/${companyId.value}/branches/${branchId.value}/stock/movements`,
      {
        item_id: fItem.value,
        type: fTipe.value,
        qty: fQty.value,
        unit_cost: fTipe.value === 'pembelian' ? fCost.value : undefined,
        note: fNote.value.trim() || undefined,
      },
      auth.token,
    ),
  )
  if (res) {
    showSesuaikan.value = false
    toast.success('Mutasi stok dicatat.')
    await muatSaldo(1)
    await muatRiwayat(1)
  } else if (aksi.error.value) {
    toast.error(aksi.error.value)
  }
}

onMounted(async () => {
  await access.load(companyId.value, auth.token)
  await referensi.run(async () => {
    const [b, i] = await Promise.all([
      api.get<Cabang[]>(`/companies/${companyId.value}/branches`, auth.token),
      api.getPage<KatalogItem>(`/companies/${companyId.value}/items?per_page=100`, auth.token),
    ])
    cabang.value = b ?? []
    items.value = i.data
  })
  branchId.value =
    (auth.branchId && cabang.value.some((b) => b.id === auth.branchId)
      ? auth.branchId
      : (cabang.value[0]?.id ?? null))
  await Promise.all([muatSaldo(1), muatRiwayat(1)])
})
</script>

<template>
  <div>
    <AppPageHeader
      eyebrow="Persediaan"
      title="Stok cabang"
      subtitle="Pantau saldo, riwayat mutasi, dan catat koreksi stok dengan jejak audit."
      icon="inventory_2"
    >
      <template #actions>
        <AppButton variant="ghost" :loading="saldoAsync.loading.value || mutasiAsync.loading.value" @click="muatAktif()">
          Muat ulang
        </AppButton>
        <AppButton v-if="bolehSesuaikan" @click="bukaSesuaikan">Penyesuaian manual</AppButton>
      </template>
    </AppPageHeader>

    <AppBanner tone="red" :message="error" />

    <AppCard>
      <div class="mb-3 flex flex-wrap items-end gap-2">
        <AppField label="Cabang" class="w-full sm:w-64">
          <AppSelect
            :model-value="branchId"
            :options="opsiCabang"
            placeholder="Pilih cabang"
            @update:model-value="gantiCabang"
          />
        </AppField>
        <AppField label="Cari" class="min-w-48 flex-1">
          <AppSearch
            v-model="cari"
            :placeholder="tab === 'saldo' ? 'Kode atau nama item…' : 'Item atau catatan mutasi…'"
            :loading="saldoAsync.loading.value || mutasiAsync.loading.value"
            @search="cariSekarang"
          />
        </AppField>
        <AppButton variant="ghost" @click="resetFilter">Reset</AppButton>
      </div>

      <AppTabs :items="tabs" :model-value="tab" label="Tampilan stok" @update:model-value="gantiTab" />

      <div v-if="tab === 'saldo'" class="mt-3">
        <AppTable
          :loading="saldoAsync.loading.value"
          :empty="!saldo.length"
          :colspan="5"
          empty-title="Belum ada saldo stok"
          empty-message="Stok terbentuk dari pembelian, transfer, atau penyesuaian."
        >
          <template #head>
            <tr>
              <th>Item</th>
              <th class="text-right">Qty</th>
              <th class="text-right">Rata-rata modal</th>
              <th class="text-right">Nilai persediaan</th>
              <th class="hidden sm:table-cell">Satuan</th>
            </tr>
          </template>
          <tr v-for="row in saldo" :key="row.itemId">
            <td>
              <p class="font-medium">{{ row.itemName }}</p>
              <p class="font-mono text-xs text-secondary">{{ row.itemCode }}</p>
            </td>
            <td class="text-right font-medium tabular-nums">{{ qty(row.qty) }}</td>
            <td class="text-right tabular-nums">{{ rupiah(row.avgCost) }}</td>
            <td class="text-right font-medium tabular-nums">
              {{ rupiah(Number(row.qty) * Number(row.avgCost ?? 0)) }}
            </td>
            <td class="hidden text-secondary sm:table-cell">{{ row.unit ?? 'pcs' }}</td>
          </tr>
        </AppTable>
        <div class="mt-2 flex flex-wrap items-center justify-between gap-2 text-sm">
          <p class="text-secondary">
            Total nilai halaman ini:
            <span class="font-semibold text-ink">{{ rupiah(totalNilai) }}</span>
          </p>
          <AppPager :meta="saldoMeta" @page="muatSaldo" @update:per-page="gantiPerPage" />
        </div>
      </div>

      <div v-else class="mt-3">
        <AppTable
          :loading="mutasiAsync.loading.value"
          :empty="!gerak.length"
          :colspan="6"
          empty-title="Belum ada mutasi"
          empty-message="Mutasi muncul dari pembelian, penjualan, transfer, dan koreksi stok."
        >
          <template #head>
            <tr>
              <th>Waktu</th>
              <th>Item</th>
              <th>Tipe</th>
              <th class="text-right">Qty</th>
              <th class="text-right">Saldo setelah</th>
              <th class="hidden md:table-cell">Catatan</th>
            </tr>
          </template>
          <tr v-for="row in gerak" :key="row.id">
            <td class="whitespace-nowrap text-sm text-secondary">{{ waktu(row.createdAt) }}</td>
            <td>
              <p class="font-medium">{{ row.itemName ?? `Item #${row.itemId}` }}</p>
              <p class="font-mono text-xs text-secondary">{{ row.itemCode ?? '' }}</p>
            </td>
            <td><AppBadge :tone="tone(row.type)">{{ labelTipe(row.type) }}</AppBadge></td>
            <td
              class="text-right font-medium tabular-nums"
              :class="Number(row.qty) < 0 ? 'text-danger' : 'text-ok'"
            >
              {{ qtyBersign(row.qty) }}
            </td>
            <td class="text-right tabular-nums">{{ qty(row.balanceAfter) }}</td>
            <td class="hidden md:table-cell text-secondary">{{ row.note ?? '—' }}</td>
          </tr>
        </AppTable>
        <AppPager class="mt-2" :meta="mutasiMeta" @page="muatRiwayat" @update:per-page="gantiPerPage" />
      </div>
    </AppCard>

    <AppModal
      :open="showSesuaikan"
      title="Penyesuaian manual"
      description="Catat mutasi koreksi. Tindakan ini masuk ke audit log."
      size="md"
      @close="showSesuaikan = false"
    >
      <AppBanner tone="red" :message="aksi.error.value" />
      <AppField label="Item" required>
        <AppSelect v-model="fItem" :options="opsiItem" placeholder="Pilih item" />
      </AppField>
      <div class="mt-3 grid gap-3 sm:grid-cols-2">
        <AppField label="Jenis mutasi" required>
          <AppSelect v-model="fTipe">
            <option value="pembelian">Masuk (pembelian)</option>
            <option value="penjualan">Keluar (penjualan)</option>
            <option value="penyesuaian">Koreksi (bisa + / -)</option>
          </AppSelect>
        </AppField>
        <AppField label="Qty" required>
          <AppInput v-model="fQty" type="number" step="any" placeholder="0" />
        </AppField>
      </div>
      <p class="mt-1 text-xs text-secondary">{{ tipe.hint }}</p>
      <AppField v-if="fTipe === 'pembelian'" label="Modal per unit" required class="mt-3">
        <AppMoneyInput v-model="fCost" placeholder="0" />
      </AppField>
      <AppField label="Catatan" class="mt-3">
        <AppTextarea v-model="fNote" :rows="2" :maxlength="500" show-count placeholder="Alasan koreksi atau nomor dokumen" />
      </AppField>
      <template #footer>
        <AppButton variant="ghost" @click="showSesuaikan = false">Batal</AppButton>
        <AppButton :loading="aksi.loading.value" :disabled="!sesuaikanSah" @click="simpanSesuai">
          Catat mutasi
        </AppButton>
      </template>
    </AppModal>
  </div>
</template>
