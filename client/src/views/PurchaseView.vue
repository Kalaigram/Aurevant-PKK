<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { api } from '../lib/api'
import { rupiah, tanggal } from '../lib/format'
import { useCompany } from '../composables/useCompany'
import { usePagedList } from '../composables/usePagedList'
import { useAccessStore } from '../stores/access'
import { useAsync } from '../composables/useAsync'
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
type Aksi = 'submit' | 'approve' | 'order' | 'cancel'

interface POItem {
  itemId: number
  itemName: string | null
  unit: string | null
  qtyOrdered: string
  qtyReceived: string
  unitPrice: string
}

interface PO {
  id: number
  branchId: number
  branchName: string | null
  supplierId: number
  supplierName: string | null
  supplierAddress: string | null
  status: string
  note: string | null
  createdAt: string | null
  total: string
  items: POItem[]
  bill: { amount: string; paidAmount: string; dueAt: string | null } | null
}

interface Supplier {
  id: number
  name: string
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
}

interface ReceiptItem {
  itemId: number
  itemName: string | null
  unit: string | null
  qtyReceived: string
  unitCost: string
}

interface Receipt {
  id: number
  dueAt: string | null
  note: string | null
  createdAt: string | null
  items: ReceiptItem[]
}

interface BarisBuat {
  itemId: number | null
  qty: number | null
  unitPrice: number | null
}

interface BarisTerima {
  itemId: number
  itemName: string
  unit: string | null
  remaining: number
  qty: number | null
  unitCost: number | null
}

const { auth, companyId } = useCompany()
const access = useAccessStore()
const toast = useToast()
const { confirm } = useConfirm()

const detailAsync = useAsync()
const riwayat = useAsync()
const referensi = useAsync()
const aksi = useAsync()
const buatAsync = useAsync()
const terimaAsync = useAsync()

const cabang = ref<Cabang[]>([])
const suppliers = ref<Supplier[]>([])
const items = ref<KatalogItem[]>([])
const tab = ref('semua')
const cari = ref('')
const filterCabang = ref<number | null>(null)

const daftar = usePagedList<PO>((page, perPage) => {
  const params = new URLSearchParams({
    per_page: String(perPage),
    page: String(page),
  })
  if (tab.value !== 'semua') params.set('status', tab.value)
  if (filterCabang.value !== null) params.set('branch_id', String(filterCabang.value))
  if (cari.value.trim()) params.set('q', cari.value.trim())
  return api.getPage<PO>(
    `/companies/${companyId.value}/purchase-orders?${params}`,
    auth.token,
  )
}, { perPage: 15 })

const detail = ref<PO | null>(null)
const detailOpen = ref(false)
const receipts = ref<Receipt[]>([])

const showBuat = ref(false)
const fBranch = ref<number | null>(null)
const fSupplier = ref<number | null>(null)
const fNote = ref('')
const fLines = ref<BarisBuat[]>([{ itemId: null, qty: 1, unitPrice: null }])

const showTerima = ref(false)
const tLines = ref<BarisTerima[]>([])
const tDue = ref('')
const tNote = ref('')

const tabs = [
  { key: 'semua', label: 'Semua' },
  { key: 'draft', label: 'Draft' },
  { key: 'diajukan', label: 'Diajukan' },
  { key: 'disetujui', label: 'Disetujui' },
  { key: 'dipesan', label: 'Dipesan' },
  { key: 'selesai', label: 'Selesai' },
  { key: 'dibatalkan', label: 'Dibatalkan' },
]

const statusLabel: Record<string, string> = {
  draft: 'Draft',
  diajukan: 'Diajukan',
  disetujui: 'Disetujui',
  dipesan: 'Dipesan',
  selesai: 'Selesai',
  dibatalkan: 'Dibatalkan',
}

const aksiConfig: Record<Aksi, { judul: string; pesan: string; deskripsi: string; tone: 'primary' | 'danger' }> = {
  submit: {
    judul: 'Ajukan PO',
    pesan: 'PO diajukan untuk persetujuan.',
    deskripsi: 'PO akan masuk antrean persetujuan atasan.',
    tone: 'primary',
  },
  approve: {
    judul: 'Setujui PO',
    pesan: 'PO disetujui.',
    deskripsi: 'PO yang disetujui bisa ditandai dipesan ke supplier.',
    tone: 'primary',
  },
  order: {
    judul: 'Tandai dipesan',
    pesan: 'PO ditandai sudah dipesan ke supplier.',
    deskripsi: 'Lakukan setelah PO benar-benar dikirim ke supplier.',
    tone: 'primary',
  },
  cancel: {
    judul: 'Batalkan PO',
    pesan: 'PO dibatalkan.',
    deskripsi: 'PO yang dibatalkan tidak bisa diproses atau diterima lagi.',
    tone: 'danger',
  },
}

const bolehBuat = computed(() => access.can('po.buat'))
const bolehSetujui = computed(() => access.can('po.setujui'))
const error = computed(
  () => daftar.error.value || detailAsync.error.value || referensi.error.value,
)

const opsiCabang = computed(() =>
  cabang.value.map((b) => ({ value: b.id, label: `${b.code} — ${b.name}` })),
)
const opsiSupplier = computed(() =>
  suppliers.value.map((s) => ({ value: s.id, label: s.name })),
)
const opsiItem = computed(() =>
  items.value.map((it) => ({
    value: it.id,
    label: `${it.code} — ${it.name}${it.unit ? ` (${it.unit})` : ''}`,
  })),
)

const duplikat = computed(() => {
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
    fBranch.value !== null &&
    fSupplier.value !== null &&
    fLines.value.length > 0 &&
    !duplikat.value &&
    fLines.value.every(
      (line) =>
        line.itemId !== null &&
        Number(line.qty ?? 0) > 0 &&
        line.unitPrice !== null &&
        line.unitPrice >= 0,
    ),
)

const totalDraft = computed(() =>
  fLines.value.reduce(
    (total, line) => total + Number(line.qty ?? 0) * Number(line.unitPrice ?? 0),
    0,
  ),
)

const barisTerima = computed(() =>
  tLines.value.filter((line) => Number(line.qty ?? 0) > 0),
)
const terimaSah = computed(
  () =>
    barisTerima.value.length > 0 &&
    barisTerima.value.every(
      (line) =>
        Number(line.qty) > 0 &&
        Number(line.qty) <= line.remaining + 0.0001 &&
        line.unitCost !== null &&
        line.unitCost >= 0,
    ),
)
const totalTerima = computed(() =>
  barisTerima.value.reduce(
    (total, line) => total + Number(line.qty ?? 0) * Number(line.unitCost ?? 0),
    0,
  ),
)

function tone(status: string): Tone {
  if (status === 'selesai') return 'green'
  if (status === 'dibatalkan') return 'red'
  if (status === 'disetujui' || status === 'dipesan') return 'blue'
  if (status === 'draft') return 'gray'
  return 'amber'
}

function labelStatus(status: string) {
  return statusLabel[status] ?? status
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

function sisa(it: POItem) {
  return Math.max(0, Number(it.qtyOrdered) - Number(it.qtyReceived))
}

function jumlahDiterima(po: PO) {
  return po.items.reduce((total, it) => total + Number(it.qtyReceived), 0)
}

function jumlahDipesan(po: PO) {
  return po.items.reduce((total, it) => total + Number(it.qtyOrdered), 0)
}

function namaItem(it: { itemName: string | null; itemId: number }) {
  return it.itemName ?? `Item #${it.itemId}`
}

function petunjuk(status: string) {
  if (status === 'draft') return 'PO masih draft. Ajukan ke atasan agar bisa disetujui.'
  if (status === 'diajukan') return 'Menunggu persetujuan atasan sebelum dikirim ke supplier.'
  if (status === 'disetujui') return 'PO sudah disetujui. Tandai dipesan setelah dikirim ke supplier.'
  if (status === 'dipesan') return 'Barang dalam perjalanan. Catat penerimaan saat barang datang.'
  if (status === 'selesai') return 'Seluruh barang sudah diterima dan PO selesai.'
  return 'PO dibatalkan dan tidak bisa diproses lagi.'
}

function gantiTab(key: string) {
  tab.value = key
  void daftar.load(1)
}

function gantiCabang(value: string | number | null) {
  filterCabang.value = value === null ? null : Number(value)
  void daftar.load(1)
}

function resetFilter() {
  tab.value = 'semua'
  cari.value = ''
  filterCabang.value = null
  void daftar.load(1)
}

async function lihat(id: number) {
  const res = await detailAsync.run(() =>
    api.get<PO>(`/companies/${companyId.value}/purchase-orders/${id}`, auth.token),
  )
  if (res) {
    detail.value = res
    detailOpen.value = true
    await muatRiwayat(id)
  } else if (detailAsync.error.value) {
    toast.error(detailAsync.error.value)
  }
}

async function muatRiwayat(id: number) {
  const res = await riwayat.run(() =>
    api.get<Receipt[]>(`/companies/${companyId.value}/purchase-orders/${id}/receipts?per_page=50`, auth.token),
  )
  receipts.value = res ?? []
}

async function transisi(kind: Aksi) {
  if (!detail.value) return
  const cfg = aksiConfig[kind]
  const setuju = await confirm({
    title: cfg.judul,
    message: `PO #${detail.value.id} — ${detail.value.supplierName ?? 'supplier'}. ${cfg.deskripsi}`,
    confirmLabel: cfg.judul,
    tone: cfg.tone,
  })
  if (!setuju) return

  const id = detail.value.id
  const res = await aksi.run(() =>
    api.post<PO>(
      `/companies/${companyId.value}/purchase-orders/${id}/${kind}`,
      {},
      auth.token,
    ),
  )
  if (res) {
    detail.value = res
    toast.success(cfg.pesan)
    await daftar.reload()
    await muatRiwayat(id)
  } else if (aksi.error.value) {
    toast.error(aksi.error.value)
  }
}

function bukaBuat() {
  fBranch.value =
    auth.branchId && cabang.value.some((b) => b.id === auth.branchId)
      ? auth.branchId
      : (cabang.value[0]?.id ?? null)
  fSupplier.value = null
  fNote.value = ''
  fLines.value = [{ itemId: null, qty: 1, unitPrice: null }]
  buatAsync.error.value = ''
  showBuat.value = true
}

function tambahBaris() {
  fLines.value.push({ itemId: null, qty: 1, unitPrice: null })
}

function hapusBaris(index: number) {
  if (fLines.value.length <= 1) return
  fLines.value.splice(index, 1)
}

async function buatPo() {
  if (!buatSah.value || fBranch.value === null || fSupplier.value === null) return
  const res = await buatAsync.run(() =>
    api.post<PO>(
      `/companies/${companyId.value}/purchase-orders`,
      {
        branch_id: fBranch.value,
        supplier_id: fSupplier.value,
        items: fLines.value.map((line) => ({
          item_id: line.itemId,
          qty_ordered: Number(line.qty),
          unit_price: Number(line.unitPrice),
        })),
        note: fNote.value.trim() || undefined,
      },
      auth.token,
    ),
  )
  if (res) {
    showBuat.value = false
    detail.value = res
    receipts.value = []
    detailOpen.value = true
    toast.success('PO dibuat sebagai draft.')
    await daftar.load(1)
  }
}

function tutupTerima() {
  showTerima.value = false
  if (detail.value) detailOpen.value = true
}

function bukaTerima() {
  if (!detail.value) return
  tLines.value = detail.value.items
    .filter((it) => sisa(it) > 0)
    .map((it) => ({
      itemId: it.itemId,
      itemName: namaItem(it),
      unit: it.unit,
      remaining: sisa(it),
      qty: sisa(it),
      unitCost: Number(it.unitPrice),
    }))
  tDue.value = ''
  tNote.value = ''
  terimaAsync.error.value = ''
  detailOpen.value = false
  showTerima.value = true
}

async function terimaSekarang() {
  if (!detail.value || !terimaSah.value) return
  const id = detail.value.id
  const res = await terimaAsync.run(() =>
    api.post<PO>(
      `/companies/${companyId.value}/purchase-orders/${id}/receive`,
      {
        items: barisTerima.value.map((line) => ({
          item_id: line.itemId,
          qty_received: Number(line.qty),
          unit_cost: Number(line.unitCost),
        })),
        due_at: tDue.value || undefined,
        note: tNote.value.trim() || undefined,
      },
      auth.token,
    ),
  )
  if (res) {
    showTerima.value = false
    detail.value = res
    detailOpen.value = true
    toast.success(
      res.status === 'selesai'
        ? 'Penerimaan dicatat. Seluruh barang sudah diterima.'
        : 'Penerimaan dicatat. Stok dan tagihan sudah diperbarui.',
    )
    await daftar.reload()
    await muatRiwayat(id)
  } else if (terimaAsync.error.value) {
    toast.error(terimaAsync.error.value)
  }
}

function cetak() {
  window.print()
}

onMounted(async () => {
  await access.load(companyId.value, auth.token)
  await referensi.run(async () => {
    const [b, s, i] = await Promise.all([
      api.get<Cabang[]>(`/companies/${companyId.value}/branches`, auth.token),
      api.getPage<Supplier>(`/companies/${companyId.value}/suppliers?per_page=100`, auth.token),
      api.getPage<KatalogItem>(`/companies/${companyId.value}/items?per_page=100`, auth.token),
    ])
    cabang.value = b ?? []
    suppliers.value = s.data
    items.value = i.data
  })
  await daftar.load(1)
})
</script>

<template>
  <div class="screen-only">
    <AppPageHeader
      eyebrow="Pembelian"
      title="Pesanan pembelian"
      subtitle="Kelola PO dari draft sampai barang diterima, lengkap dengan tagihan supplier."
      icon="shopping_cart"
    >
      <template #actions>
        <AppButton variant="ghost" :loading="daftar.loading.value" @click="daftar.reload()">
          Muat ulang
        </AppButton>
        <AppButton v-if="bolehBuat" @click="bukaBuat">PO baru</AppButton>
      </template>
    </AppPageHeader>

    <AppBanner tone="red" :message="error" />

    <AppCard>
      <div class="mb-3 flex flex-wrap items-end gap-2">
        <AppField label="Cabang" class="w-full sm:w-56">
          <AppSelect
            :model-value="filterCabang"
            :options="opsiCabang"
            placeholder="Semua cabang"
            @update:model-value="gantiCabang"
          />
        </AppField>
        <AppField label="Cari" class="min-w-48 flex-1">
          <AppSearch
            v-model="cari"
            placeholder="Nomor PO, supplier, atau catatan…"
            :loading="daftar.loading.value"
            @search="daftar.load(1)"
          />
        </AppField>
        <AppButton variant="ghost" @click="resetFilter">Reset</AppButton>
      </div>

      <AppTabs :items="tabs" :model-value="tab" label="Status PO" @update:model-value="gantiTab" />

      <AppTable
        class="mt-3"
        :loading="daftar.loading.value"
        :empty="!daftar.rows.value.length"
        :colspan="6"
        empty-title="Belum ada PO"
        empty-message="Buat PO baru atau ubah filter status dan cabang."
      >
        <template #head>
          <tr>
            <th>PO</th>
            <th>Supplier</th>
            <th class="hidden md:table-cell">Cabang</th>
            <th class="text-right">Nilai</th>
            <th class="text-right">Diterima</th>
            <th>Status</th>
          </tr>
        </template>
        <tr
          v-for="po in daftar.rows.value"
          :key="po.id"
          class="cursor-pointer transition hover:bg-surface-low/70"
          @click="lihat(po.id)"
        >
          <td>
            <p class="font-mono text-sm font-semibold text-ink">PO #{{ po.id }}</p>
            <p class="mt-0.5 text-xs text-secondary">{{ waktu(po.createdAt) }}</p>
          </td>
          <td>
            <p class="font-medium">{{ po.supplierName ?? '—' }}</p>
            <p class="text-xs text-secondary">{{ po.items.length }} jenis item</p>
          </td>
          <td class="hidden md:table-cell">{{ po.branchName ?? '—' }}</td>
          <td class="text-right font-medium tabular-nums">{{ rupiah(po.total) }}</td>
          <td class="text-right text-sm tabular-nums">
            {{ qty(jumlahDiterima(po)) }} / {{ qty(jumlahDipesan(po)) }}
          </td>
          <td>
            <div class="flex flex-col items-start gap-1 sm:flex-row sm:items-center sm:justify-between">
              <AppBadge :tone="tone(po.status)" dot>{{ labelStatus(po.status) }}</AppBadge>
              <button
                type="button"
                class="rounded text-xs font-semibold text-primary focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/55"
                :aria-label="`Lihat PO nomor ${po.id}`"
                @click.stop="lihat(po.id)"
              >
                Detail
              </button>
            </div>
          </td>
        </tr>
      </AppTable>
      <AppPager :meta="daftar.meta.value" @page="daftar.setPage" @update:per-page="daftar.setPerPage" />
    </AppCard>

    <AppModal
      :open="detailOpen"
      :title="detail ? `PO #${detail.id}` : 'Detail PO'"
      :description="detail ? `${detail.supplierName ?? 'Supplier'} · ${detail.branchName ?? 'Cabang'}` : ''"
      size="xl"
      @close="detailOpen = false"
    >
      <div v-if="detailAsync.loading.value" class="space-y-2">
        <div v-for="i in 4" :key="i" class="h-10 animate-pulse rounded-lg bg-surface-low" />
      </div>
      <div v-else-if="detail">
        <AppBanner tone="red" :message="aksi.error.value" />
        <div class="flex flex-wrap items-center gap-2">
          <AppBadge :tone="tone(detail.status)" dot>{{ labelStatus(detail.status) }}</AppBadge>
          <span class="text-sm text-secondary">{{ detail.items.length }} jenis</span>
          <span class="text-sm text-secondary">·</span>
          <span class="text-sm text-secondary">Dibuat {{ waktu(detail.createdAt) }}</span>
        </div>
        <p class="mt-2 text-sm text-secondary">{{ petunjuk(detail.status) }}</p>

        <div class="mt-3 grid gap-3 rounded-xl bg-surface-low p-3 text-sm sm:grid-cols-3">
          <div>
            <p class="text-xs font-semibold uppercase tracking-wider text-secondary">Supplier</p>
            <p class="mt-0.5 font-medium">{{ detail.supplierName ?? '—' }}</p>
            <p v-if="detail.supplierAddress" class="text-xs text-secondary">
              {{ detail.supplierAddress }}
            </p>
          </div>
          <div>
            <p class="text-xs font-semibold uppercase tracking-wider text-secondary">Cabang</p>
            <p class="mt-0.5 font-medium">{{ detail.branchName ?? '—' }}</p>
          </div>
          <div>
            <p class="text-xs font-semibold uppercase tracking-wider text-secondary">Nilai PO</p>
            <p class="mt-0.5 font-display text-base font-bold tabular-nums">
              {{ rupiah(detail.total) }}
            </p>
          </div>
        </div>
        <p v-if="detail.note" class="mt-2 rounded-lg border border-outline/60 px-3 py-2 text-sm">
          <span class="text-secondary">Catatan: </span>{{ detail.note }}
        </p>

        <AppTable class="mt-4" :colspan="6" dense>
          <template #head>
            <tr>
              <th>Item</th>
              <th class="text-right">Dipesan</th>
              <th class="text-right">Diterima</th>
              <th class="text-right">Sisa</th>
              <th class="text-right">Harga</th>
              <th class="text-right">Subtotal</th>
            </tr>
          </template>
          <tr v-for="(it, i) in detail.items" :key="`${it.itemId}-${i}`">
            <td>
              <p class="font-medium">{{ namaItem(it) }}</p>
              <p class="text-xs text-secondary">{{ it.unit ?? 'pcs' }}</p>
            </td>
            <td class="text-right tabular-nums">{{ qty(it.qtyOrdered) }}</td>
            <td class="text-right tabular-nums">{{ qty(it.qtyReceived) }}</td>
            <td class="text-right tabular-nums" :class="sisa(it) > 0 ? 'text-warn' : 'text-ok'">
              {{ qty(sisa(it)) }}
            </td>
            <td class="text-right tabular-nums">{{ rupiah(it.unitPrice) }}</td>
            <td class="text-right font-medium tabular-nums">
              {{ rupiah(Number(it.qtyOrdered) * Number(it.unitPrice)) }}
            </td>
          </tr>
        </AppTable>
        <div class="mt-2 flex justify-end text-sm">
          <p class="rounded-lg bg-surface-low px-3 py-2">
            Total PO: <span class="font-display font-bold tabular-nums">{{ rupiah(detail.total) }}</span>
          </p>
        </div>

        <div class="mt-4 grid gap-4 md:grid-cols-2">
          <div class="rounded-xl border border-outline/60 p-3">
            <p class="mb-2 text-xs font-semibold uppercase tracking-wider text-secondary">
              Tagihan supplier
            </p>
            <template v-if="detail.bill">
              <div class="flex justify-between gap-3 py-0.5 text-sm">
                <span class="text-secondary">Tagihan</span>
                <span class="font-medium tabular-nums">{{ rupiah(detail.bill.amount) }}</span>
              </div>
              <div class="flex justify-between gap-3 py-0.5 text-sm">
                <span class="text-secondary">Dibayar</span>
                <span class="tabular-nums">{{ rupiah(detail.bill.paidAmount) }}</span>
              </div>
              <div class="flex justify-between gap-3 border-t border-outline/60 pt-1.5 text-sm font-bold">
                <span>Sisa</span>
                <span class="tabular-nums">
                  {{ rupiah(Number(detail.bill.amount) - Number(detail.bill.paidAmount)) }}
                </span>
              </div>
              <p class="mt-2 text-xs text-secondary">
                Jatuh tempo: {{ detail.bill.dueAt ? tanggal(detail.bill.dueAt) : 'belum ditentukan' }}
              </p>
            </template>
            <p v-else class="text-sm text-secondary">
              Tagihan terbentuk otomatis saat barang pertama kali diterima.
            </p>
          </div>

          <div class="rounded-xl border border-outline/60 p-3">
            <p class="mb-2 text-xs font-semibold uppercase tracking-wider text-secondary">
              Riwayat penerimaan
            </p>
            <p v-if="!receipts.length" class="text-sm text-secondary">
              Belum ada barang diterima untuk PO ini.
            </p>
            <div v-else class="space-y-2">
              <div
                v-for="r in receipts"
                :key="r.id"
                class="rounded-lg bg-surface-low px-3 py-2 text-sm"
              >
                <div class="flex flex-wrap items-center justify-between gap-1">
                  <p class="font-semibold">Terima #{{ r.id }}</p>
                  <p class="text-xs text-secondary">{{ waktu(r.createdAt) }}</p>
                </div>
                <p v-for="(it, i) in r.items" :key="`${r.id}-${it.itemId}-${i}`" class="text-xs text-secondary">
                  {{ namaItem(it) }} — {{ qty(it.qtyReceived) }} {{ it.unit ?? 'pcs' }} ×
                  {{ rupiah(it.unitCost) }}
                </p>
                <p v-if="r.dueAt" class="mt-1 text-xs text-secondary">
                  Jatuh tempo {{ tanggal(r.dueAt) }}
                </p>
                <p v-if="r.note" class="text-xs text-secondary">{{ r.note }}</p>
              </div>
            </div>
          </div>
        </div>
      </div>

      <template #footer>
        <AppButton variant="ghost" @click="detailOpen = false">Tutup</AppButton>
        <AppButton variant="ghost" @click="cetak">Cetak PO</AppButton>
        <AppButton
          v-if="detail?.status === 'draft' && bolehBuat"
          :loading="aksi.loading.value"
          @click="transisi('submit')"
        >
          Ajukan
        </AppButton>
        <AppButton
          v-if="detail?.status === 'diajukan' && bolehSetujui"
          :loading="aksi.loading.value"
          @click="transisi('approve')"
        >
          Setujui
        </AppButton>
        <AppButton
          v-if="detail?.status === 'disetujui' && bolehSetujui"
          :loading="aksi.loading.value"
          @click="transisi('order')"
        >
          Tandai dipesan
        </AppButton>
        <AppButton
          v-if="detail?.status === 'dipesan' && bolehSetujui"
          variant="secondary"
          @click="bukaTerima"
        >
          Terima barang
        </AppButton>
        <AppButton
          v-if="detail && ['draft', 'diajukan', 'disetujui', 'dipesan'].includes(detail.status) && bolehSetujui"
          variant="danger"
          :loading="aksi.loading.value"
          @click="transisi('cancel')"
        >
          Batalkan
        </AppButton>
      </template>
    </AppModal>

    <AppModal
      :open="showBuat"
      title="PO baru"
      description="PO disimpan sebagai draft dan perlu diajukan untuk persetujuan."
      size="lg"
      @close="showBuat = false"
    >
      <AppBanner tone="red" :message="buatAsync.error.value" />
      <div class="grid gap-3 sm:grid-cols-2">
        <AppField label="Cabang" required>
          <AppSelect v-model="fBranch" :options="opsiCabang" placeholder="Pilih cabang" />
        </AppField>
        <AppField label="Supplier" required>
          <AppSelect v-model="fSupplier" :options="opsiSupplier" placeholder="Pilih supplier" />
        </AppField>
      </div>
      <p v-if="!suppliers.length" class="mt-2 text-xs text-warn">
        Belum ada supplier. Tambahkan supplier di menu Katalog & supplier terlebih dahulu.
      </p>

      <div class="mt-4 flex items-center justify-between">
        <p class="text-xs font-semibold uppercase tracking-wider text-secondary">Baris item</p>
        <AppButton variant="ghost" @click="tambahBaris">Tambah baris</AppButton>
      </div>
      <div
        v-for="(line, i) in fLines"
        :key="i"
        class="mt-2 rounded-lg border border-outline/60 p-2"
      >
        <div class="grid items-end gap-2 sm:grid-cols-12">
          <AppField label="Item" class="sm:col-span-6">
            <AppSelect v-model="line.itemId" :options="opsiItem" placeholder="Pilih item" />
          </AppField>
          <AppField label="Qty" class="sm:col-span-2">
            <AppInput v-model="line.qty" type="number" min="0.001" step="any" placeholder="1" />
          </AppField>
          <AppField label="Harga satuan" class="sm:col-span-3">
            <AppMoneyInput v-model="line.unitPrice" placeholder="0" />
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
        <p class="mt-1 text-right text-xs text-secondary tabular-nums">
          Subtotal {{ rupiah(Number(line.qty ?? 0) * Number(line.unitPrice ?? 0)) }}
        </p>
      </div>
      <p v-if="duplikat" class="mt-2 text-xs text-danger">
        Item yang sama tidak boleh muncul di dua baris.
      </p>

      <AppField label="Catatan" class="mt-3">
        <AppTextarea v-model="fNote" :rows="2" :maxlength="500" show-count placeholder="Contoh: minta kemasan dus" />
      </AppField>

      <div class="mt-3 flex items-center justify-between rounded-lg bg-surface-low px-3 py-2 text-sm">
        <span class="text-secondary">Total PO</span>
        <span class="font-display font-bold tabular-nums">{{ rupiah(totalDraft) }}</span>
      </div>

      <template #footer>
        <AppButton variant="ghost" @click="showBuat = false">Batal</AppButton>
        <AppButton :loading="buatAsync.loading.value" :disabled="!buatSah" @click="buatPo">
          Simpan draft
        </AppButton>
      </template>
    </AppModal>

    <AppModal
      :open="showTerima"
      title="Terima barang"
      description="Qty bisa diterima sebagian. Stok, HPP, dan tagihan diperbarui otomatis."
      size="lg"
      @close="tutupTerima"
    >
      <AppBanner tone="red" :message="terimaAsync.error.value" />
      <AppTable :colspan="4" dense>
        <template #head>
          <tr>
            <th>Item</th>
            <th class="text-right">Sisa</th>
            <th class="text-right">Qty terima</th>
            <th class="text-right">Modal satuan</th>
          </tr>
        </template>
        <tr v-for="line in tLines" :key="line.itemId">
          <td>
            <p class="font-medium">{{ line.itemName }}</p>
            <p class="text-xs text-secondary">{{ line.unit ?? 'pcs' }}</p>
          </td>
          <td class="text-right tabular-nums">{{ qty(line.remaining) }}</td>
          <td class="w-28">
            <AppInput
              v-model="line.qty"
              type="number"
              min="0"
              :max="line.remaining"
              step="any"
              placeholder="0"
            />
          </td>
          <td class="w-36">
            <AppMoneyInput v-model="line.unitCost" placeholder="0" />
          </td>
        </tr>
      </AppTable>
      <p v-if="!barisTerima.length" class="mt-2 text-xs text-secondary">
        Isi qty minimal satu baris untuk melanjutkan.
      </p>

      <div class="mt-3 grid gap-3 sm:grid-cols-2">
        <AppField label="Jatuh tempo (opsional)">
          <AppDateInput v-model="tDue" />
        </AppField>
        <AppField label="Catatan">
          <AppInput v-model="tNote" :maxlength="500" placeholder="Contoh: surat jalan no. 123" />
        </AppField>
      </div>
      <div class="mt-3 flex items-center justify-between rounded-lg bg-surface-low px-3 py-2 text-sm">
        <span class="text-secondary">Perkiraan tagihan</span>
        <span class="font-display font-bold tabular-nums">{{ rupiah(totalTerima) }}</span>
      </div>

      <template #footer>
        <AppButton variant="ghost" @click="tutupTerima">Batal</AppButton>
        <AppButton :loading="terimaAsync.loading.value" :disabled="!terimaSah" @click="terimaSekarang">
          Simpan penerimaan
        </AppButton>
      </template>
    </AppModal>
  </div>

  <div v-if="detail" class="print-only">
    <div class="flex items-start justify-between gap-4 border-b-2 border-black/70 pb-3">
      <div>
        <p class="font-display text-lg font-bold uppercase">{{ auth.companyName ?? 'Aurevant' }}</p>
        <p class="text-sm">{{ detail.branchName ?? '' }}</p>
        <p class="mt-1 text-xs text-secondary">Dokumen pesanan pembelian</p>
      </div>
      <div class="text-right">
        <p class="font-display text-lg font-bold uppercase">Purchase Order</p>
        <p class="font-mono text-sm">PO #{{ detail.id }}</p>
        <p class="text-xs">{{ tanggal(detail.createdAt) }}</p>
        <p class="text-xs uppercase">{{ labelStatus(detail.status) }}</p>
      </div>
    </div>

    <div class="mt-4 flex justify-between gap-6 text-sm">
      <div>
        <p class="text-xs font-semibold uppercase tracking-wider text-secondary">Kepada</p>
        <p class="font-semibold">{{ detail.supplierName ?? '—' }}</p>
        <p v-if="detail.supplierAddress" class="text-xs">{{ detail.supplierAddress }}</p>
      </div>
      <div class="text-right">
        <p class="text-xs font-semibold uppercase tracking-wider text-secondary">Cabang</p>
        <p class="font-semibold">{{ detail.branchName ?? '—' }}</p>
      </div>
    </div>

    <table class="app-table mt-4 w-full">
      <thead>
        <tr>
          <th class="text-left">Item</th>
          <th class="text-right">Qty</th>
          <th class="text-right">Harga</th>
          <th class="text-right">Subtotal</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="(it, i) in detail.items" :key="`${it.itemId}-${i}`">
          <td>{{ namaItem(it) }}</td>
          <td class="text-right">
            {{ qty(it.qtyOrdered) }} {{ it.unit ?? 'pcs' }}
          </td>
          <td class="text-right">{{ rupiah(it.unitPrice) }}</td>
          <td class="text-right">{{ rupiah(Number(it.qtyOrdered) * Number(it.unitPrice)) }}</td>
        </tr>
      </tbody>
      <tfoot>
        <tr>
          <td colspan="3" class="text-right font-bold">Total</td>
          <td class="text-right font-bold">{{ rupiah(detail.total) }}</td>
        </tr>
      </tfoot>
    </table>

    <p v-if="detail.note" class="mt-3 text-sm">
      <span class="font-semibold">Catatan: </span>{{ detail.note }}
    </p>
    <p v-if="detail.status === 'draft'" class="mt-3 text-sm font-bold uppercase">
      Draft — belum disetujui
    </p>

    <div class="mt-10 grid grid-cols-2 gap-10 text-center text-sm">
      <div>
        <p class="text-secondary">Dibuat oleh</p>
        <div class="mt-12 border-t border-black/60 pt-1">Gudang / Pembelian</div>
      </div>
      <div>
        <p class="text-secondary">Disetujui oleh</p>
        <div class="mt-12 border-t border-black/60 pt-1">Atasan</div>
      </div>
    </div>
  </div>
</template>
