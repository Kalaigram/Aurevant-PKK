<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { api } from '../lib/api'
import { rupiah, tanggal } from '../lib/format'
import { useCompany } from '../composables/useCompany'
import { useAccessStore } from '../stores/access'
import { useAsync } from '../composables/useAsync'
import { usePagedList } from '../composables/usePagedList'
import { useConfirm } from '../composables/useConfirm'
import { useToast } from '../composables/useToast'
import AppBadge from '../components/ui/AppBadge.vue'
import AppBanner from '../components/ui/AppBanner.vue'
import AppButton from '../components/ui/AppButton.vue'
import AppCard from '../components/ui/AppCard.vue'
import AppDateInput from '../components/ui/AppDateInput.vue'
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

interface OrderItem {
  itemId: number
  itemCode: string | null
  itemName: string | null
  unit: string | null
  qty: string
  price: string
  commissionAmount: string
}

interface Order {
  id: number
  branchId: number
  customerId: number | null
  customerName: string | null
  assigneeId: number | null
  assigneeName: string | null
  status: string
  depositAmount: string
  scheduledAt: string | null
  note: string | null
  total: string
  createdAt: string | null
  items: OrderItem[]
}

interface Cabang {
  id: number
  code: string
  name: string
}

interface Customer {
  id: number
  name: string
  phone: string | null
}

interface Jasa {
  id: number
  code: string
  name: string
  unit: string | null
}

interface BarisJasa {
  itemId: number | null
  qty: number | null
}

const STATUS_LABEL: Record<string, string> = {
  baru: 'Baru',
  diproses: 'Diproses',
  selesai: 'Selesai',
  dibayar: 'Dibayar',
  dibatalkan: 'Dibatalkan',
}

const { auth, companyId } = useCompany()
const access = useAccessStore()
const toast = useToast()
const { confirm } = useConfirm()

const referensiAsync = useAsync()
const detailAsync = useAsync()
const buatAsync = useAsync()
const aksiAsync = useAsync()

const cabang = ref<Cabang[]>([])
const pelanggan = ref<Customer[]>([])
const jasa = ref<Jasa[]>([])
const branchId = ref<number | null>(null)
const statusTab = ref('semua')
const cari = ref('')

const {
  rows: list,
  meta,
  loading,
  error: errorDaftar,
  load,
  setPerPage,
} = usePagedList<Order>((page, perPage) => {
  if (!branchId.value) return Promise.resolve({ data: [], meta: null })
  const params = new URLSearchParams({ per_page: String(perPage), page: String(page) })
  if (statusTab.value !== 'semua') params.set('status', statusTab.value)
  if (cari.value.trim()) params.set('q', cari.value.trim())
  return api.getPage<Order>(
    `/companies/${companyId.value}/branches/${branchId.value}/service-orders?${params}`,
    auth.token,
  )
})

const detailOpen = ref(false)
const detail = ref<Order | null>(null)
const nominalDp = ref<number | null>(null)
const assigneeId = ref<number | null>(null)
const bayarMetode = ref('tunai')
const bayarReferensi = ref('')
const bayarNominal = ref<number | null>(null)

const showBuat = ref(false)
const fCustomer = ref<number | null>(null)
const fScheduled = ref('')
const fNote = ref('')
const fLines = ref<BarisJasa[]>([{ itemId: null, qty: 1 }])

const bolehBuat = computed(() => access.can('transaksi.buat'))
const bolehProses = computed(() => access.can('jasa.proses'))
const error = computed(() => errorDaftar.value || referensiAsync.error.value)

const statusTabs = [
  { key: 'semua', label: 'Semua' },
  { key: 'baru', label: 'Baru' },
  { key: 'diproses', label: 'Diproses' },
  { key: 'selesai', label: 'Selesai' },
  { key: 'dibayar', label: 'Dibayar' },
  { key: 'dibatalkan', label: 'Dibatalkan' },
]

const opsiCabang = computed(() =>
  cabang.value.map((b) => ({ value: b.id, label: `${b.code} — ${b.name}` })),
)
const opsiPelanggan = computed(() =>
  pelanggan.value.map((p) => ({
    value: p.id,
    label: `${p.name}${p.phone ? ` · ${p.phone}` : ''}`,
  })),
)
const opsiJasa = computed(() =>
  jasa.value.map((j) => ({
    value: j.id,
    label: `${j.code} — ${j.name}${j.unit ? ` (${j.unit})` : ''}`,
  })),
)
const opsiMetode = [
  { value: 'tunai', label: 'Tunai' },
  { value: 'transfer', label: 'Transfer' },
  { value: 'qris', label: 'QRIS' },
  { value: 'ewallet', label: 'E-wallet' },
]

const totalDetail = computed(() => Number(detail.value?.total ?? 0))
const sisaDetail = computed(() =>
  Math.max(0, totalDetail.value - Number(detail.value?.depositAmount ?? 0)),
)
const duplikatJasa = computed(() => {
  const seen = new Set<number>()
  for (const line of fLines.value) {
    if (line.itemId === null) continue
    if (seen.has(line.itemId)) return true
    seen.add(line.itemId)
  }
  return false
})
const buatSah = computed(
  () =>
    fLines.value.length > 0 &&
    !duplikatJasa.value &&
    fLines.value.every(
      (line) => line.itemId !== null && line.qty !== null && Number(line.qty) > 0,
    ),
)

function tone(status: string): Tone {
  if (status === 'dibayar' || status === 'selesai') return 'green'
  if (status === 'dibatalkan') return 'red'
  if (status === 'diproses') return 'blue'
  return 'amber'
}

function labelStatus(status: string) {
  return STATUS_LABEL[status] ?? status
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

function qty(value: string | number) {
  return Number(value).toLocaleString('id-ID', { maximumFractionDigits: 3 })
}

function namaItem(item: OrderItem) {
  return item.itemName ?? `Item #${item.itemId}`
}

function namaPelanggan(order: Order) {
  return order.customerName ?? (order.customerId ? `Pelanggan #${order.customerId}` : 'Tanpa nama')
}

function ringkasPekerjaan(order: Order) {
  if (!order.items.length) return 'Belum ada item'
  const first = namaItem(order.items[0]!)
  return order.items.length > 1 ? `${first} +${order.items.length - 1}` : first
}

function subtotal(item: OrderItem) {
  return Number(item.qty) * Number(item.price)
}

function petunjuk(status: string) {
  if (status === 'baru') return 'Order baru. Catat DP bila ada, lalu proses ke teknisi.'
  if (status === 'diproses') return 'Pekerjaan sedang berjalan. Selesaikan untuk memotong bahan dan mencatat komisi.'
  if (status === 'selesai') return 'Pekerjaan selesai. Terbitkan nota setelah pembayaran diterima.'
  if (status === 'dibayar') return 'Order sudah dibayar dan nota sudah terbit.'
  return 'Order dibatalkan. Tidak ada perubahan stok atau pembayaran.'
}

function gantiCabang(value: string | number | null) {
  if (value === null) return
  branchId.value = Number(value)
  detailOpen.value = false
  void load(1)
}

function gantiStatus(key: string) {
  statusTab.value = key
  void load(1)
}

function setCustomer(value: string | number | null) {
  fCustomer.value = value === null ? null : Number(value)
}

function setLineItem(line: BarisJasa, value: string | number | null) {
  line.itemId = value === null ? null : Number(value)
}

function setMetode(value: string | number | null) {
  bayarMetode.value = value === null ? 'tunai' : String(value)
}

function resetAksi(order: Order) {
  nominalDp.value = null
  assigneeId.value = order.assigneeId
  bayarMetode.value = 'tunai'
  bayarReferensi.value = ''
  bayarNominal.value = Math.max(0, Number(order.total) - Number(order.depositAmount))
}

async function lihat(order: Order) {
  if (!branchId.value) return
  const res = await detailAsync.run(() =>
    api.get<Order>(
      `/companies/${companyId.value}/branches/${branchId.value}/service-orders/${order.id}`,
      auth.token,
    ),
  )
  if (!res) {
    if (detailAsync.error.value) toast.error(detailAsync.error.value)
    return
  }
  detail.value = res
  resetAksi(res)
  detailOpen.value = true
}

async function segarkanDetail(id: number) {
  if (!branchId.value) return
  const res = await detailAsync.run(() =>
    api.get<Order>(
      `/companies/${companyId.value}/branches/${branchId.value}/service-orders/${id}`,
      auth.token,
    ),
  )
  if (res) {
    detail.value = res
    resetAksi(res)
  } else if (detailAsync.error.value) {
    toast.error(detailAsync.error.value)
  }
}

async function aksi(path: string, body: unknown, pesan: string) {
  if (!detail.value || !branchId.value) return
  const id = detail.value.id
  await aksiAsync.run(() =>
    api.post(
      `/companies/${companyId.value}/branches/${branchId.value}/service-orders/${id}/${path}`,
      body,
      auth.token,
    ),
  )
  if (aksiAsync.error.value) {
    toast.error(aksiAsync.error.value)
    return
  }
  toast.success(pesan)
  await segarkanDetail(id)
  await load()
}

async function proses() {
  await aksi(
    'process',
    { assignee_id: assigneeId.value ?? undefined },
    'Order mulai diproses.',
  )
}

async function selesai() {
  if (!detail.value) return
  const setuju = await confirm({
    title: 'Selesaikan order?',
    message: `Order #${detail.value.id}. Bahan akan dipotong dan komisi teknisi dicatat.`,
    confirmLabel: 'Selesaikan',
  })
  if (!setuju) return
  await aksi('complete', {}, 'Order selesai, bahan terpotong.')
}

async function catatDp() {
  if (!nominalDp.value || nominalDp.value <= 0) {
    toast.warning('Isi nominal DP terlebih dahulu.')
    return
  }
  if (Number(detail.value?.depositAmount ?? 0) + nominalDp.value >= totalDetail.value) {
    toast.warning(`DP harus lebih kecil dari total order ${rupiah(totalDetail.value)}.`)
    return
  }
  await aksi('deposit', { amount: nominalDp.value }, 'DP tercatat.')
}

async function bayar() {
  if (!detail.value) return
  if (!bayarNominal.value || bayarNominal.value <= 0) {
    toast.warning('Isi nominal pembayaran terlebih dahulu.')
    return
  }
  if (Math.abs(bayarNominal.value - sisaDetail.value) > 0.009) {
    toast.warning(`Nominal harus sama dengan sisa ${rupiah(sisaDetail.value)}.`)
    return
  }
  const referensi = bayarReferensi.value.trim()
  if (bayarMetode.value !== 'tunai' && !referensi) {
    toast.warning('Isi referensi pembayaran non-tunai.')
    return
  }
  const setuju = await confirm({
    title: 'Terbitkan nota?',
    message: `Order #${detail.value.id} akan dibayar ${rupiah(bayarNominal.value)} dan nota diterbitkan.`,
    confirmLabel: 'Bayar & terbitkan',
  })
  if (!setuju) return
  await aksi(
    'pay',
    {
      payments: [
        {
          method: bayarMetode.value,
          amount: bayarNominal.value,
          reference: bayarMetode.value === 'tunai' ? undefined : referensi,
        },
      ],
    },
    'Pembayaran tercatat dan nota diterbitkan.',
  )
}

async function batalkan() {
  if (!detail.value) return
  const setuju = await confirm({
    title: 'Batalkan order?',
    message: `Order #${detail.value.id} akan dibatalkan.`,
    confirmLabel: 'Batalkan order',
    tone: 'danger',
  })
  if (!setuju) return
  await aksi('cancel', {}, 'Order dibatalkan.')
}

function bukaBuat() {
  buatAsync.error.value = ''
  fCustomer.value = null
  fScheduled.value = ''
  fNote.value = ''
  fLines.value = [{ itemId: null, qty: 1 }]
  showBuat.value = true
}

function tambahBaris() {
  fLines.value.push({ itemId: null, qty: 1 })
}

function hapusBaris(index: number) {
  if (fLines.value.length <= 1) return
  fLines.value.splice(index, 1)
}

async function buat() {
  if (!branchId.value || !buatSah.value) return
  const res = await buatAsync.run(() =>
    api.post<Order>(
      `/companies/${companyId.value}/branches/${branchId.value}/service-orders`,
      {
        customer_id: fCustomer.value ?? undefined,
        scheduled_at: fScheduled.value || undefined,
        note: fNote.value.trim() || undefined,
        items: fLines.value.map((line) => ({
          item_id: line.itemId,
          qty: Number(line.qty),
        })),
      },
      auth.token,
    ),
  )
  if (!res) {
    if (buatAsync.error.value) toast.error(buatAsync.error.value)
    return
  }
  showBuat.value = false
  detail.value = res
  resetAksi(res)
  detailOpen.value = true
  toast.success('Order jasa dibuat.')
  await load(1)
}

onMounted(async () => {
  await access.load(companyId.value, auth.token)
  const data = await referensiAsync.run(() =>
    Promise.all([
      api.get<Cabang[]>(`/companies/${companyId.value}/branches`, auth.token),
      api.getPage<Jasa>(
        `/companies/${companyId.value}/items?type=jasa&per_page=100`,
        auth.token,
      ),
      api.getPage<Customer>(
        `/companies/${companyId.value}/customers?per_page=100`,
        auth.token,
      ),
    ]),
  )
  if (!data) return
  const [b, j, c] = data
  cabang.value = b
  jasa.value = j.data
  pelanggan.value = c.data
  branchId.value =
    (auth.branchId && cabang.value.some((item) => item.id === auth.branchId)
      ? auth.branchId
      : cabang.value[0]?.id) ?? null
  await load(1)
})
</script>

<template>
  <div>
    <AppPageHeader
      eyebrow="Jasa"
      title="Order jasa"
      subtitle="Catat pekerjaan, DP, proses teknisi, dan pembayaran dalam satu alur."
      icon="engineering"
    >
      <template #actions>
        <AppButton variant="ghost" :loading="loading" @click="load()">Muat ulang</AppButton>
        <AppButton v-if="bolehBuat && branchId" @click="bukaBuat">Order baru</AppButton>
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
        <AppSearch
          v-model="cari"
          class="min-w-56 flex-1"
          placeholder="Cari nama atau telepon pelanggan..."
          @search="load(1)"
        />
      </div>

      <AppTabs
        :items="statusTabs"
        :model-value="statusTab"
        label="Status order jasa"
        @update:model-value="gantiStatus"
      />

      <AppTable
        class="mt-3"
        :loading="loading"
        :empty="!list.length"
        :colspan="5"
        empty-title="Belum ada order jasa"
        empty-message="Buat order baru atau ubah filter status/pencarian."
      >
        <template #head>
          <tr>
            <th>Order</th>
            <th>Pelanggan</th>
            <th class="hidden md:table-cell">Pekerjaan</th>
            <th class="text-right">Total</th>
            <th>Status</th>
          </tr>
        </template>
        <tr
          v-for="o in list"
          :key="o.id"
          class="cursor-pointer transition hover:bg-surface-low/70"
          @click="lihat(o)"
        >
          <td>
            <p class="font-mono text-sm font-semibold text-ink">JSA #{{ o.id }}</p>
            <p class="mt-0.5 text-xs text-secondary">{{ waktu(o.createdAt) }}</p>
            <p v-if="o.scheduledAt" class="text-xs text-secondary">
              Jadwal {{ tanggal(o.scheduledAt) }}
            </p>
          </td>
          <td>
            <p class="font-medium text-ink">{{ namaPelanggan(o) }}</p>
            <p v-if="o.assigneeName" class="text-xs text-secondary">
              Teknisi {{ o.assigneeName }}
            </p>
          </td>
          <td class="hidden text-secondary md:table-cell">{{ ringkasPekerjaan(o) }}</td>
          <td class="text-right font-semibold tabular-nums">{{ rupiah(o.total) }}</td>
          <td><AppBadge :tone="tone(o.status)" dot>{{ labelStatus(o.status) }}</AppBadge></td>
        </tr>
      </AppTable>
      <AppPager :meta="meta" @page="load" @update:per-page="setPerPage" />
    </AppCard>

    <AppModal
      :open="detailOpen"
      :title="detail ? `Order jasa #${detail.id}` : 'Detail order jasa'"
      :description="detail ? `Dibuat ${waktu(detail.createdAt)}` : ''"
      size="xl"
      @close="detailOpen = false"
    >
      <AppBanner tone="red" :message="aksiAsync.error.value" />
      <div v-if="detailAsync.loading.value" class="space-y-2">
        <div v-for="i in 4" :key="i" class="h-10 animate-pulse rounded-lg bg-surface-low" />
      </div>
      <div v-else-if="detail">
        <div class="flex flex-wrap items-center gap-2">
          <AppBadge :tone="tone(detail.status)" dot>{{ labelStatus(detail.status) }}</AppBadge>
          <span class="text-sm font-medium">{{ namaPelanggan(detail) }}</span>
          <span class="text-sm text-secondary">·</span>
          <span class="text-sm text-secondary">{{ detail.items.length }} pekerjaan</span>
          <span v-if="detail.scheduledAt" class="text-sm text-secondary">
            · Jadwal {{ tanggal(detail.scheduledAt) }}
          </span>
        </div>
        <p class="mt-2 text-sm text-secondary">{{ petunjuk(detail.status) }}</p>

        <p v-if="detail.note" class="mt-3 rounded-lg border border-outline/60 px-3 py-2 text-sm">
          <span class="text-secondary">Catatan: </span>{{ detail.note }}
        </p>

        <AppTable class="mt-4" :colspan="4" dense>
          <template #head>
            <tr>
              <th>Pekerjaan</th>
              <th class="text-right">Qty</th>
              <th class="text-right">Harga</th>
              <th class="text-right">Subtotal</th>
            </tr>
          </template>
          <tr v-for="(it, i) in detail.items" :key="`${it.itemId}-${i}`">
            <td>
              <p class="font-medium">{{ namaItem(it) }}</p>
              <p class="font-mono text-xs text-secondary">
                {{ it.itemCode ?? '' }}
                <span v-if="Number(it.commissionAmount) > 0">
                  · komisi {{ rupiah(it.commissionAmount) }}
                </span>
              </p>
            </td>
            <td class="text-right tabular-nums">
              {{ qty(it.qty) }}<span v-if="it.unit" class="text-secondary"> {{ it.unit }}</span>
            </td>
            <td class="text-right tabular-nums">{{ rupiah(it.price) }}</td>
            <td class="text-right font-semibold tabular-nums">{{ rupiah(subtotal(it)) }}</td>
          </tr>
        </AppTable>

        <div class="mt-3 flex justify-end">
          <div class="w-full max-w-xs space-y-1.5 rounded-lg bg-surface-low px-3 py-2 text-sm">
            <div class="flex justify-between gap-4">
              <span class="text-secondary">Total</span>
              <span class="font-semibold tabular-nums">{{ rupiah(detail.total) }}</span>
            </div>
            <div class="flex justify-between gap-4">
              <span class="text-secondary">DP masuk</span>
              <span class="tabular-nums">{{ rupiah(detail.depositAmount) }}</span>
            </div>
            <div class="flex justify-between gap-4 border-t border-outline/60 pt-1.5">
              <span class="text-secondary">Sisa</span>
              <span class="font-bold tabular-nums">{{ rupiah(sisaDetail) }}</span>
            </div>
          </div>
        </div>

        <div
          v-if="
            (bolehProses && ['baru', 'diproses'].includes(detail.status)) ||
            (bolehBuat &&
              (['baru', 'diproses'].includes(detail.status) ||
                (detail.status === 'selesai' && sisaDetail > 0)))
          "
          class="mt-4 space-y-3 rounded-lg border border-outline/60 p-3"
        >
          <div
            v-if="bolehProses && ['baru', 'diproses'].includes(detail.status)"
            class="flex flex-wrap items-end gap-2"
          >
            <AppField v-if="detail.status === 'baru'" label="ID teknisi (opsional)" class="w-40">
              <AppInput v-model="assigneeId" type="number" min="1" placeholder="ID user" />
            </AppField>
            <AppButton
              v-if="detail.status === 'baru'"
              :loading="aksiAsync.loading.value"
              @click="proses"
            >
              Proses
            </AppButton>
            <AppButton
              v-if="detail.status === 'diproses'"
              :loading="aksiAsync.loading.value"
              @click="selesai"
            >
              Selesaikan
            </AppButton>
          </div>

          <div
            v-if="bolehBuat && ['baru', 'diproses'].includes(detail.status)"
            class="flex flex-wrap items-end gap-2"
          >
            <AppField label="Tambah DP" class="w-48">
              <AppMoneyInput v-model="nominalDp" placeholder="Nominal DP" />
            </AppField>
            <AppButton variant="ghost" :loading="aksiAsync.loading.value" @click="catatDp">
              Catat DP
            </AppButton>
          </div>

          <div
            v-if="bolehBuat && detail.status === 'selesai' && sisaDetail > 0"
            class="flex flex-wrap items-end gap-2"
          >
            <AppField label="Metode bayar" class="w-40">
              <AppSelect
                :model-value="bayarMetode"
                :options="opsiMetode"
                @update:model-value="setMetode"
              />
            </AppField>
            <AppField label="Nominal bayar (sesuai sisa)" class="w-48">
              <AppMoneyInput v-model="bayarNominal" placeholder="Nominal" />
            </AppField>
            <AppField v-if="bayarMetode !== 'tunai'" label="Referensi" class="w-48">
              <AppInput v-model="bayarReferensi" maxlength="100" placeholder="No. referensi" />
            </AppField>
            <AppButton :loading="aksiAsync.loading.value" @click="bayar">
              Bayar & terbitkan nota
            </AppButton>
          </div>

          <div
            v-if="bolehBuat && ['baru', 'diproses'].includes(detail.status)"
            class="border-t border-outline/60 pt-3"
          >
            <AppButton variant="danger" :loading="aksiAsync.loading.value" @click="batalkan">
              Batalkan order
            </AppButton>
          </div>
        </div>
      </div>

      <template #footer>
        <AppButton variant="ghost" @click="detailOpen = false">Tutup</AppButton>
      </template>
    </AppModal>

    <AppModal
      :open="showBuat"
      title="Order jasa baru"
      description="Pilih jasa yang sudah punya harga di cabang ini. DP bisa dicatat setelah order dibuat."
      size="lg"
      @close="showBuat = false"
    >
      <AppBanner tone="red" :message="buatAsync.error.value" />
      <div class="grid gap-3 sm:grid-cols-2">
        <AppField label="Pelanggan (opsional)">
          <AppSelect
            :model-value="fCustomer"
            :options="opsiPelanggan"
            placeholder="Tanpa nama"
            @update:model-value="setCustomer"
          />
        </AppField>
        <AppField label="Jadwal (opsional)">
          <AppDateInput v-model="fScheduled" />
        </AppField>
      </div>

      <div class="mt-3 flex items-center justify-between">
        <p class="text-xs font-semibold uppercase tracking-wider text-secondary">Pekerjaan</p>
        <AppButton variant="ghost" size="sm" @click="tambahBaris">Tambah baris</AppButton>
      </div>

      <div
        v-for="(line, i) in fLines"
        :key="i"
        class="mt-2 grid items-end gap-2 rounded-lg border border-outline/60 p-2 sm:grid-cols-12"
      >
        <AppField label="Jasa" class="sm:col-span-8">
          <AppSelect
            :model-value="line.itemId"
            :options="opsiJasa"
            placeholder="Pilih jasa"
            @update:model-value="(value) => setLineItem(line, value)"
          />
        </AppField>
        <AppField label="Qty" class="sm:col-span-3">
          <AppInput v-model="line.qty" type="number" min="0.001" step="any" placeholder="1" />
        </AppField>
        <div class="sm:col-span-1">
          <button
            type="button"
            class="w-full rounded-lg px-2 py-2.5 text-xs font-semibold text-danger transition hover:bg-danger-bg disabled:cursor-not-allowed disabled:opacity-40"
            :disabled="fLines.length <= 1"
            @click="hapusBaris(i)"
          >
            Hapus
          </button>
        </div>
      </div>
      <p v-if="duplikatJasa" class="mt-2 text-xs text-danger">
        Jasa yang sama tidak boleh muncul di dua baris.
      </p>

      <AppField label="Catatan" class="mt-3">
        <AppTextarea
          v-model="fNote"
          :rows="2"
          :maxlength="500"
          placeholder="cth. Beat brebet, servis rutin"
        />
      </AppField>

      <template #footer>
        <AppButton variant="ghost" @click="showBuat = false">Batal</AppButton>
        <AppButton :loading="buatAsync.loading.value" :disabled="!buatSah" @click="buat">
          Buat order
        </AppButton>
      </template>
    </AppModal>
  </div>
</template>
