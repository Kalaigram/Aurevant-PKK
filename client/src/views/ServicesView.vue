<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { api, downloadFile } from '../lib/api'
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
import AppEmpty from '../components/ui/AppEmpty.vue'
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
type Mode = 'daftar' | 'agenda'

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
  packageId: number | null
  packageName: string | null
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

interface PaketItem {
  itemId: number
  itemName: string | null
  qty: string
}

interface Paket {
  id: number
  code: string
  name: string
  price: string
  status: string
  itemCount: number
  items: PaketItem[]
}

interface BarisJasa {
  itemId: number | null
  qty: number | null
}

interface AgendaHari {
  key: string
  label: string
  orders: Order[]
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
const agendaAsync = useAsync()
const estimasiAsync = useAsync()

const mode = ref<Mode>('daftar')
const agenda = ref<Order[]>([])
const agendaFrom = ref(inputTanggal(new Date()))
const agendaTo = ref(inputTanggal(tambahHari(new Date(), 7)))
const agendaTeknisi = ref('semua')

const cabang = ref<Cabang[]>([])
const pelanggan = ref<Customer[]>([])
const jasa = ref<Jasa[]>([])
const paket = ref<Paket[]>([])
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
const fPackageId = ref<number | null>(null)
const fScheduled = ref('')
const fScheduledTime = ref('')
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

const modeTabs = [
  { key: 'daftar', label: 'Daftar order' },
  { key: 'agenda', label: 'Agenda' },
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
const paketDipilih = computed(
  () => paket.value.find((p) => p.id === fPackageId.value) ?? null,
)
const opsiPaket = computed(() =>
  paket.value
    .filter((p) => p.status === 'aktif')
    .map((p) => ({
      value: p.id,
      label: `${p.name} - ${rupiah(p.price)}`,
    })),
)
const opsiJasa = computed(() => {
  const dipakaiPaket = new Set(paketDipilih.value?.items.map((item) => item.itemId) ?? [])
  return jasa.value
    .filter((j) => !dipakaiPaket.has(j.id))
    .map((j) => ({
      value: j.id,
      label: `${j.code} - ${j.name}${j.unit ? ` (${j.unit})` : ''}`,
    }))
})
const opsiMetode = [
  { value: 'tunai', label: 'Tunai' },
  { value: 'transfer', label: 'Transfer' },
  { value: 'qris', label: 'QRIS' },
  { value: 'ewallet', label: 'E-wallet' },
]

const opsiAgendaTeknisi = computed(() => {
  const teknisi = new Map<string, string>([
    ['semua', 'Semua teknisi'],
    ['none', 'Belum ditugaskan'],
  ])
  for (const order of agenda.value) {
    if (order.assigneeId !== null) {
      teknisi.set(String(order.assigneeId), order.assigneeName ?? `Teknisi #${order.assigneeId}`)
    }
  }
  return [...teknisi].map(([value, label]) => ({ value, label }))
})

const agendaTampil = computed(() =>
  agenda.value.filter((order) => {
    if (agendaTeknisi.value === 'none') return order.assigneeId === null
    if (agendaTeknisi.value !== 'semua') {
      return String(order.assigneeId) === agendaTeknisi.value
    }
    return true
  }),
)

const agendaHari = computed<AgendaHari[]>(() => {
  const hari = new Map<string, Order[]>()
  for (const order of agendaTampil.value) {
    if (!order.scheduledAt) continue
    const key = kunciHari(order.scheduledAt)
    const daftar = hari.get(key) ?? []
    daftar.push(order)
    hari.set(key, daftar)
  }
  return [...hari.entries()]
    .sort(([a], [b]) => a.localeCompare(b))
    .map(([key, orders]) => ({
      key,
      label: labelHari(orders[0]!.scheduledAt),
      orders,
    }))
})

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
const barisSah = computed(() =>
  fLines.value.every((line) => line.itemId !== null && line.qty !== null && Number(line.qty) > 0),
)
const buatSah = computed(
  () =>
    !duplikatJasa.value &&
    ((fPackageId.value !== null && barisSah.value) ||
      (fPackageId.value === null && fLines.value.length > 0 && barisSah.value)),
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
  if (!iso) return '�?"'
  return new Date(iso).toLocaleString('id-ID', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}

function tambahHari(date: Date, hari: number) {
  const next = new Date(date)
  next.setDate(next.getDate() + hari)
  return next
}

function inputTanggal(date: Date) {
  const bulan = String(date.getMonth() + 1).padStart(2, '0')
  const tanggal = String(date.getDate()).padStart(2, '0')
  return `${date.getFullYear()}-${bulan}-${tanggal}`
}

function kunciHari(iso: string) {
  return inputTanggal(new Date(iso))
}

function labelHari(iso: string | null | undefined) {
  if (!iso) return 'Tanpa jadwal'
  return new Date(iso).toLocaleDateString('id-ID', {
    weekday: 'long',
    day: 'numeric',
    month: 'long',
    year: 'numeric',
  })
}

function jamJadwal(iso: string | null | undefined) {
  if (!iso) return 'Seharian'
  const date = new Date(iso)
  if (date.getHours() === 0 && date.getMinutes() === 0) return 'Seharian'
  return date.toLocaleTimeString('id-ID', { hour: '2-digit', minute: '2-digit' })
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
  if (status === 'diproses')
    return 'Pekerjaan sedang berjalan. Selesaikan untuk memotong bahan dan mencatat komisi.'
  if (status === 'selesai') return 'Pekerjaan selesai. Terbitkan nota setelah pembayaran diterima.'
  if (status === 'dibayar') return 'Order sudah dibayar dan nota sudah terbit.'
  return 'Order dibatalkan. Tidak ada perubahan stok atau pembayaran.'
}

function gantiCabang(value: string | number | null) {
  if (value === null) return
  branchId.value = Number(value)
  detailOpen.value = false
  agenda.value = []
  if (mode.value === 'agenda') void muatAgenda()
  else void load(1)
}

function gantiStatus(key: string) {
  statusTab.value = key
  void load(1)
}

function setCustomer(value: string | number | null) {
  fCustomer.value = value === null ? null : Number(value)
}

function setPaket(value: string | number | null) {
  fPackageId.value = value === null ? null : Number(value)
  if (fPackageId.value !== null) {
    fLines.value = []
  } else if (fLines.value.length === 0) {
    fLines.value = [{ itemId: null, qty: 1 }]
  }
}

function setLineItem(line: BarisJasa, value: string | number | null) {
  line.itemId = value === null ? null : Number(value)
}

function setMetode(value: string | number | null) {
  bayarMetode.value = value === null ? 'tunai' : String(value)
}

function setAgendaTeknisi(value: string | number | null) {
  agendaTeknisi.value = value === null ? 'semua' : String(value)
}

async function muatAgenda() {
  if (!branchId.value) return
  if (!agendaFrom.value || !agendaTo.value) {
    toast.warning('Isi rentang tanggal agenda terlebih dahulu.')
    return
  }
  if (agendaFrom.value > agendaTo.value) {
    toast.warning('Tanggal awal agenda tidak boleh melewati tanggal akhir.')
    return
  }
  const params = new URLSearchParams({
    per_page: '100',
    scheduled_from: agendaFrom.value,
    scheduled_to: agendaTo.value,
  })
  const res = await agendaAsync.run(() =>
    api.getPage<Order>(
      `/companies/${companyId.value}/branches/${branchId.value}/service-orders?${params}`,
      auth.token,
    ),
  )
  if (!res) {
    if (agendaAsync.error.value) toast.error(agendaAsync.error.value)
    return
  }
  agenda.value = res.data.filter((order) => order.status !== 'dibatalkan')
  if (
    agendaTeknisi.value !== 'semua' &&
    agendaTeknisi.value !== 'none' &&
    !agenda.value.some((order) => String(order.assigneeId) === agendaTeknisi.value)
  ) {
    agendaTeknisi.value = 'semua'
  }
}

function gantiMode(key: string) {
  mode.value = key === 'agenda' ? 'agenda' : 'daftar'
  if (mode.value === 'agenda') void muatAgenda()
  else void load(1)
}

async function agendaMingguIni() {
  agendaFrom.value = inputTanggal(new Date())
  agendaTo.value = inputTanggal(tambahHari(new Date(), 7))
  agendaTeknisi.value = 'semua'
  await muatAgenda()
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

async function unduhEstimasi() {
  if (!detail.value || !branchId.value) return
  const id = detail.value.id
  await estimasiAsync.run(() =>
    downloadFile(
      `/companies/${companyId.value}/branches/${branchId.value}/service-orders/${id}/estimate`,
      `estimasi-jasa-${id}.pdf`,
      auth.token,
      'application/pdf',
    ),
  )
  if (estimasiAsync.error.value) toast.error(estimasiAsync.error.value)
  else toast.success('Estimasi PDF diunduh.')
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
  if (mode.value === 'agenda') await muatAgenda()
}

async function proses() {
  await aksi('process', { assignee_id: assigneeId.value ?? undefined }, 'Order mulai diproses.')
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
  fPackageId.value = null
  fScheduled.value = ''
  fScheduledTime.value = ''
  fNote.value = ''
  fLines.value = [{ itemId: null, qty: 1 }]
  showBuat.value = true
}

function tambahBaris() {
  fLines.value.push({ itemId: null, qty: 1 })
}

function hapusBaris(index: number) {
  if (fLines.value.length <= 1) {
    if (fPackageId.value !== null) fLines.value = []
    return
  }
  fLines.value.splice(index, 1)
}

async function buat() {
  if (!branchId.value || !buatSah.value) return
  const res = await buatAsync.run(() =>
    api.post<Order>(
      `/companies/${companyId.value}/branches/${branchId.value}/service-orders`,
      {
        customer_id: fCustomer.value ?? undefined,
        package_id: fPackageId.value ?? undefined,
        scheduled_at: fScheduled.value
          ? `${fScheduled.value}T${fScheduledTime.value || '00:00'}:00`
          : undefined,
        note: fNote.value.trim() || undefined,
        items:
          fLines.value.length > 0
            ? fLines.value.map((line) => ({
                item_id: line.itemId,
                qty: Number(line.qty),
              }))
            : undefined,
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
  if (mode.value === 'agenda') await muatAgenda()
}

onMounted(async () => {
  await access.load(companyId.value, auth.token)
  const data = await referensiAsync.run(() =>
    Promise.all([
      api.get<Cabang[]>(`/companies/${companyId.value}/branches`, auth.token),
      api.getPage<Jasa>(`/companies/${companyId.value}/items?type=jasa&per_page=100`, auth.token),
      api.getPage<Customer>(`/companies/${companyId.value}/customers?per_page=100`, auth.token),
      api.getPage<Paket>(`/companies/${companyId.value}/service-packages?per_page=100`, auth.token),
    ]),
  )
  if (!data) return
  const [b, j, c, p] = data
  cabang.value = b
  jasa.value = j.data
  pelanggan.value = c.data
  paket.value = p.data
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
          v-if="mode === 'daftar'"
          v-model="cari"
          class="min-w-56 flex-1"
          placeholder="Cari nama atau telepon pelanggan..."
          @search="load(1)"
        />
      </div>

      <AppTabs
        :items="modeTabs"
        :model-value="mode"
        label="Tampilan order jasa"
        @update:model-value="gantiMode"
      />

      <div v-if="mode === 'daftar'" class="mt-3">
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
            <td>
              <div
                class="flex flex-col items-start gap-1 sm:flex-row sm:items-center sm:justify-between"
              >
                <AppBadge :tone="tone(o.status)" dot>{{ labelStatus(o.status) }}</AppBadge>
                <button
                  type="button"
                  class="rounded text-xs font-semibold text-primary focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/55"
                  :aria-label="`Lihat order jasa nomor ${o.id}`"
                  @click.stop="lihat(o)"
                >
                  Detail
                </button>
              </div>
            </td>
          </tr>
        </AppTable>
        <AppPager :meta="meta" @page="load" @update:per-page="setPerPage" />
      </div>

      <div v-else class="mt-3">
        <div class="flex flex-wrap items-end gap-2">
          <AppField label="Dari tanggal" class="w-full sm:w-44">
            <AppDateInput v-model="agendaFrom" />
          </AppField>
          <AppField label="Sampai tanggal" class="w-full sm:w-44">
            <AppDateInput v-model="agendaTo" />
          </AppField>
          <AppField label="Teknisi" class="w-full sm:w-56">
            <AppSelect
              :model-value="agendaTeknisi"
              :options="opsiAgendaTeknisi"
              @update:model-value="setAgendaTeknisi"
            />
          </AppField>
          <AppButton :loading="agendaAsync.loading.value" @click="muatAgenda">
            Tampilkan
          </AppButton>
          <AppButton variant="ghost" @click="agendaMingguIni">7 hari ke depan</AppButton>
        </div>

        <AppBanner tone="red" :message="agendaAsync.error.value" />

        <div v-if="agendaAsync.loading.value" class="mt-4 space-y-2">
          <div v-for="i in 3" :key="i" class="h-16 animate-pulse rounded-lg bg-surface-low" />
        </div>
        <AppEmpty
          v-else-if="!agendaTampil.length"
          class="mt-4"
          icon="event_busy"
          title="Agenda kosong"
          message="Tidak ada order terjadwal pada rentang tanggal dan teknisi ini."
        />
        <div v-else class="mt-4 space-y-5">
          <p class="text-xs text-secondary">{{ agendaTampil.length }} order terjadwal</p>
          <section v-for="hari in agendaHari" :key="hari.key">
            <h3 class="text-sm font-semibold capitalize text-ink">{{ hari.label }}</h3>
            <div
              class="mt-2 divide-y divide-outline/60 overflow-hidden rounded-xl border border-outline/70"
            >
              <button
                v-for="o in hari.orders"
                :key="o.id"
                type="button"
                class="flex w-full flex-wrap items-center gap-3 px-3 py-3 text-left transition hover:bg-surface-low/70 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/55"
                :aria-label="`Buka order jasa nomor ${o.id}`"
                @click="lihat(o)"
              >
                <span class="w-16 shrink-0 text-sm font-semibold tabular-nums text-primary-ink">
                  {{ jamJadwal(o.scheduledAt) }}
                </span>
                <span class="min-w-0 flex-1">
                  <span class="font-mono text-2xs text-secondary">JSA #{{ o.id }}</span>
                  <span class="block truncate font-medium text-ink">{{ namaPelanggan(o) }}</span>
                  <span class="block truncate text-xs text-secondary">{{
                    ringkasPekerjaan(o)
                  }}</span>
                </span>
                <span class="hidden w-36 shrink-0 text-right text-xs sm:block">
                  <span class="block text-secondary">Teknisi</span>
                  <span class="block truncate font-medium text-ink">
                    {{ o.assigneeName ?? 'Belum ditugaskan' }}
                  </span>
                </span>
                <span class="shrink-0 text-right">
                  <span class="block text-sm font-semibold tabular-nums">{{
                    rupiah(o.total)
                  }}</span>
                  <AppBadge class="mt-1" :tone="tone(o.status)" dot>
                    {{ labelStatus(o.status) }}
                  </AppBadge>
                </span>
              </button>
            </div>
          </section>
        </div>
      </div>
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
          <span v-if="detail.packageName" class="text-sm text-secondary">
            - Paket {{ detail.packageName }}
          </span>
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
        <AppButton
          v-if="detail && !['dibayar', 'dibatalkan'].includes(detail.status)"
          variant="ghost"
          :loading="estimasiAsync.loading.value"
          @click="unduhEstimasi"
        >
          Estimasi PDF
        </AppButton>
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
      <div class="grid gap-3 sm:grid-cols-3">
        <AppField label="Pelanggan (opsional)">
          <AppSelect
            :model-value="fCustomer"
            :options="opsiPelanggan"
            placeholder="Tanpa nama"
            @update:model-value="setCustomer"
          />
        </AppField>
        <AppField label="Tanggal jadwal (opsional)">
          <AppDateInput v-model="fScheduled" />
        </AppField>
        <AppField label="Jam (opsional)">
          <AppInput v-model="fScheduledTime" type="time" :disabled="!fScheduled" />
        </AppField>
        <AppField label="Paket jasa (opsional)" class="sm:col-span-3">
          <AppSelect
            :model-value="fPackageId"
            :options="opsiPaket"
            placeholder="Tanpa paket"
            @update:model-value="setPaket"
          />
        </AppField>
      </div>

      <div
        v-if="paketDipilih"
        class="mt-3 rounded-lg border border-primary/30 bg-primary/5 px-3 py-2 text-sm"
      >
        <div class="flex flex-wrap items-center justify-between gap-2">
          <p class="font-medium text-ink">
            {{ paketDipilih.name }}
            <span class="font-normal text-secondary">({{ paketDipilih.itemCount }} jasa)</span>
          </p>
          <p class="font-semibold tabular-nums">{{ rupiah(paketDipilih.price) }}</p>
        </div>
        <p class="mt-1 text-xs text-secondary">
          {{ paketDipilih.items.map((item) => `${item.itemName ?? 'Jasa'} x${Number(item.qty)}`).join(', ') }}
        </p>
        <p class="mt-1 text-xs text-secondary">
          Jasa paket sudah termasuk. Tambah baris hanya untuk jasa di luar paket.
        </p>
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
