<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { api, downloadFile } from '../lib/api'
import { rupiah } from '../lib/format'
import { useCompany } from '../composables/useCompany'
import { usePagedList } from '../composables/usePagedList'
import { useAccessStore } from '../stores/access'
import { useAsync } from '../composables/useAsync'
import { useToast } from '../composables/useToast'
import AppBadge from '../components/ui/AppBadge.vue'
import AppBanner from '../components/ui/AppBanner.vue'
import AppButton from '../components/ui/AppButton.vue'
import AppCard from '../components/ui/AppCard.vue'
import AppDateInput from '../components/ui/AppDateInput.vue'
import AppField from '../components/ui/AppField.vue'
import AppInput from '../components/ui/AppInput.vue'
import AppModal from '../components/ui/AppModal.vue'
import AppPageHeader from '../components/ui/AppPageHeader.vue'
import AppPager from '../components/ui/AppPager.vue'
import AppSearch from '../components/ui/AppSearch.vue'
import AppSelect from '../components/ui/AppSelect.vue'
import AppTable from '../components/ui/AppTable.vue'
import AppTabs from '../components/ui/AppTabs.vue'
import AppTextarea from '../components/ui/AppTextarea.vue'

interface NotaItem {
  saleItemId: number
  itemId: number
  itemName: string | null
  qty: string
  price: string
  lineTotal: string
  returnedQty: string | null
}

interface NotaReturn {
  id: number
  saleId: number
  kind: string
  reason: string
  refundMethod: string | null
  refundAmount: string
  status: string
}

interface Nota {
  id: number
  number: string
  status: string
  createdAt: string | null
  cashierName: string | null
  customerName: string | null
  subtotal: string
  discountTotal: string
  taxTotal: string
  grandTotal: string
  items: NotaItem[]
  payments: { method: string; amount: string; tendered?: string | null; reference?: string | null }[]
  returns?: NotaReturn[]
}

interface Cabang {
  id: number
  code: string
  name: string
}

interface BarisRetur {
  saleItemId: number
  itemName: string
  qtyTerjual: number
  sudahRetur: number
  lineTotal: number
  qty: number | null
  condition: string
}

const { auth, companyId } = useCompany()
const access = useAccessStore()
const toast = useToast()
const referensi = useAsync()
const detailAsync = useAsync()
const aksi = useAsync()

const cabang = ref<Cabang[]>([])
const branchId = ref<number | null>(null)
const tab = ref('semua')
const cari = ref('')
const dari = ref('')
const sampai = ref('')

const daftar = usePagedList<Nota>((page, perPage) => {
  if (!branchId.value) return Promise.resolve({ data: [], meta: null })
  const params = new URLSearchParams({
    per_page: String(perPage),
    page: String(page),
  })
  if (tab.value !== 'semua') params.set('status', tab.value)
  if (cari.value.trim()) params.set('q', cari.value.trim())
  if (dari.value) params.set('from', dari.value)
  if (sampai.value) params.set('to', sampai.value)
  return api.getPage<Nota>(
    `/companies/${companyId.value}/branches/${branchId.value}/sales?${params}`,
    auth.token,
  )
}, { perPage: 15 })
const detail = ref<Nota | null>(null)
const detailOpen = ref(false)

const dialogVoid = ref(false)
const alasanVoid = ref('')
const dialogRetur = ref(false)
const alasanRetur = ref('')
const metodeRetur = ref('tunai')
const barisRetur = ref<BarisRetur[]>([])

const bolehVoid = computed(() => access.can('transaksi.void'))
const bolehRetur = computed(() => access.can('retur.buat'))
const error = computed(
  () =>
    referensi.error.value ||
    daftar.error.value ||
    detailAsync.error.value ||
    aksi.error.value,
)
const tabs = [
  { key: 'semua', label: 'Semua' },
  { key: 'lunas', label: 'Lunas' },
  { key: 'retur', label: 'Retur' },
  { key: 'void', label: 'Void' },
]
const opsiCabang = computed(() =>
  cabang.value.map((b) => ({ value: b.id, label: `${b.code} — ${b.name}` })),
)
const cabangAktif = computed(() => cabang.value.find((b) => b.id === branchId.value))
const metodeLabel: Record<string, string> = {
  tunai: 'Tunai',
  transfer: 'Transfer',
  qris: 'QRIS',
  ewallet: 'E-wallet',
  dp: 'DP',
}

function labelStatus(status: string) {
  return status === 'lunas' ? 'Lunas' : status === 'void' ? 'Void' : status === 'retur' ? 'Retur' : status
}

function tone(status: string) {
  return status === 'lunas' ? 'green' : status === 'void' ? 'red' : 'amber'
}

function waktu(iso: string | null | undefined) {
  if (!iso) return '-'
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

function gantiTab(key: string) {
  tab.value = key
  detailOpen.value = false
  void daftar.load(1)
}

function gantiCabang(value: string | number | null) {
  branchId.value = value === null ? null : Number(value)
  detailOpen.value = false
  void daftar.load(1)
}

function resetFilter() {
  tab.value = 'semua'
  cari.value = ''
  dari.value = ''
  sampai.value = ''
  void daftar.load(1)
}

async function lihat(id: number) {
  const res = await detailAsync.run(() =>
    api.get<Nota>(
      `/companies/${companyId.value}/branches/${branchId.value}/sales/${id}`,
      auth.token,
    ),
  )
  if (res) {
    detail.value = res
    detailOpen.value = true
  } else if (detailAsync.error.value) {
    toast.error(detailAsync.error.value)
  }
}

async function unduhStruk() {
  if (!detail.value) return
  const id = detail.value.id
  await aksi.run(() =>
    downloadFile(
      `/companies/${companyId.value}/branches/${branchId.value}/sales/${id}/receipt`,
      `struk-${detail.value!.number}.pdf`,
      auth.token,
      'application/pdf',
    ),
  )
  if (!aksi.error.value) toast.success('Struk PDF diunduh.')
}

function cetakStruk() {
  window.print()
}

function bukaVoid() {
  if (!detail.value) return
  alasanVoid.value = ''
  detailOpen.value = false
  dialogVoid.value = true
}

async function voidSekarang() {
  if (!detail.value || !branchId.value) return
  const id = detail.value.id
  if (!alasanVoid.value.trim()) return
  const res = await aksi.run(() =>
    api.post(
      `/companies/${companyId.value}/branches/${branchId.value}/sales/${id}/void`,
      { reason: alasanVoid.value.trim() },
      auth.token,
    ),
  )
  if (res) {
    toast.success(`Nota ${detail.value?.number ?? ''} dibatalkan, stok dikembalikan.`)
    dialogVoid.value = false
    await daftar.reload()
    await lihat(id)
  }
}

async function ajukanVoid() {
  if (!detail.value) return
  const id = detail.value.id
  const res = await aksi.run(() =>
    api.post(
      `/companies/${companyId.value}/approvals`,
      {
        action: 'sale.void',
        entity_id: id,
        payload: { reason: alasanVoid.value.trim() || 'Minta void dari halaman nota' },
      },
      auth.token,
    ),
  )
  if (res) {
    toast.success('Pengajuan void terkirim ke atasan.')
    dialogVoid.value = false
  }
}

function bukaRetur() {
  if (!detail.value) return
  barisRetur.value = detail.value.items.map((it) => ({
    saleItemId: it.saleItemId,
    itemName: it.itemName ?? `Item #${it.itemId}`,
    qtyTerjual: Number(it.qty),
    sudahRetur: Number(it.returnedQty ?? 0),
    lineTotal: Number(it.lineTotal),
    qty: null,
    condition: 'layak',
  }))
  alasanRetur.value = ''
  metodeRetur.value = 'tunai'
  detailOpen.value = false
  dialogRetur.value = true
}

const barisDipilih = computed(() =>
  barisRetur.value.filter((line) => Number(line.qty ?? 0) > 0),
)
const returSah = computed(
  () =>
    barisDipilih.value.length > 0 &&
    alasanRetur.value.trim().length > 0 &&
    barisDipilih.value.every(
      (line) =>
        Number(line.qty) <= line.qtyTerjual - line.sudahRetur + 0.0001 &&
        Number(line.qty) > 0,
    ),
)
const perkiraanRetur = computed(() =>
  barisDipilih.value.reduce((total, line) => {
    if (line.qtyTerjual <= 0) return total
    return total + (line.lineTotal * Number(line.qty ?? 0)) / line.qtyTerjual
  }, 0),
)

async function returSekarang() {
  if (!detail.value || !branchId.value || !returSah.value) return
  const id = detail.value.id
  const lines = barisDipilih.value.map((line) => ({
    sale_item_id: line.saleItemId,
    qty: Number(line.qty),
    condition: line.condition,
  }))
  const res = await aksi.run(() =>
    api.post(
      `/companies/${companyId.value}/branches/${branchId.value}/sales/${id}/returns`,
      {
        items: lines,
        reason: alasanRetur.value.trim(),
        refund_method: metodeRetur.value,
      },
      auth.token,
    ),
  )
  if (res) {
    toast.success('Retur tercatat.')
    dialogRetur.value = false
    await daftar.reload()
    await lihat(id)
  } else if (aksi.error.value) {
    toast.error(aksi.error.value)
  }
}

onMounted(async () => {
  await access.load(companyId.value, auth.token)
  const res = await referensi.run(() =>
    api.get<Cabang[]>(`/companies/${companyId.value}/branches`, auth.token),
  )
  cabang.value = res ?? []
  branchId.value =
    (auth.branchId && cabang.value.some((b) => b.id === auth.branchId)
      ? auth.branchId
      : cabang.value[0]?.id) ?? null
  await daftar.load(1)
})
</script>

<template>
  <div class="screen-only">
    <AppPageHeader
      eyebrow="Penjualan"
      title="Nota penjualan"
      subtitle="Telusuri transaksi cabang, cetak struk, batalkan nota, atau catat retur barang."
      icon="receipt_long"
    >
      <template #actions>
        <AppButton variant="ghost" :loading="daftar.loading.value" @click="daftar.reload()">
          Muat ulang
        </AppButton>
      </template>
    </AppPageHeader>

    <AppBanner tone="red" :message="error" />

    <AppCard>
      <div class="mb-3 flex flex-wrap items-end gap-2">
        <AppField label="Cabang" class="w-full sm:w-56">
          <AppSelect
            :model-value="branchId"
            :options="opsiCabang"
            placeholder="Pilih cabang"
            @update:model-value="gantiCabang"
          />
        </AppField>
        <AppField label="Dari tanggal" class="w-full sm:w-40">
          <AppDateInput v-model="dari" @change="daftar.load(1)" />
        </AppField>
        <AppField label="Sampai tanggal" class="w-full sm:w-40">
          <AppDateInput v-model="sampai" @change="daftar.load(1)" />
        </AppField>
        <AppField label="Cari" class="min-w-48 flex-1">
          <AppSearch
            v-model="cari"
            placeholder="Nomor nota atau pelanggan…"
            :loading="daftar.loading.value"
            @search="daftar.load(1)"
          />
        </AppField>
        <AppButton variant="ghost" @click="resetFilter">Reset</AppButton>
      </div>

      <AppTabs :items="tabs" :model-value="tab" label="Status nota" @update:model-value="gantiTab" />

      <AppTable
        class="mt-3"
        :loading="daftar.loading.value"
        :empty="!daftar.rows.value.length"
        :colspan="6"
        empty-title="Belum ada nota"
        empty-message="Ubah filter atau buat transaksi baru di kasir."
      >
        <template #head>
          <tr>
            <th>Nota</th>
            <th class="hidden sm:table-cell">Kasir</th>
            <th class="hidden md:table-cell">Pelanggan</th>
            <th class="text-right">Item</th>
            <th class="text-right">Total</th>
            <th>Status</th>
          </tr>
        </template>
        <tr
          v-for="n in daftar.rows.value"
          :key="n.id"
          class="cursor-pointer transition hover:bg-surface-low/70"
          @click="lihat(n.id)"
        >
          <td>
            <p class="font-mono text-sm font-semibold text-ink">{{ n.number }}</p>
            <p class="mt-0.5 text-xs text-secondary">{{ waktu(n.createdAt) }}</p>
          </td>
          <td class="hidden sm:table-cell">{{ n.cashierName ?? '—' }}</td>
          <td class="hidden md:table-cell">{{ n.customerName ?? 'Umum' }}</td>
          <td class="text-right tabular-nums">{{ n.items.length }}</td>
          <td class="text-right font-medium tabular-nums">{{ rupiah(n.grandTotal) }}</td>
          <td><AppBadge :tone="tone(n.status)" dot>{{ labelStatus(n.status) }}</AppBadge></td>
        </tr>
      </AppTable>
      <AppPager
        :meta="daftar.meta.value"
        @page="daftar.setPage"
        @update:per-page="daftar.setPerPage"
      />
    </AppCard>

    <AppModal
      :open="detailOpen"
      :title="detail ? `Nota ${detail.number}` : 'Rincian nota'"
      :description="detail ? `${waktu(detail.createdAt)} · ${cabangAktif?.code ?? ''}` : ''"
      size="xl"
      @close="detailOpen = false"
    >
      <div v-if="detailAsync.loading.value" class="space-y-2">
        <div v-for="i in 4" :key="i" class="h-10 animate-pulse rounded-lg bg-surface-low" />
      </div>
      <div v-else-if="detail">
        <div class="flex flex-wrap items-center gap-2">
          <AppBadge :tone="tone(detail.status)" dot>{{ labelStatus(detail.status) }}</AppBadge>
          <span class="text-sm text-secondary">{{ detail.items.length }} baris</span>
          <span class="text-sm text-secondary">·</span>
          <span class="text-sm text-secondary">Kasir {{ detail.cashierName ?? '—' }}</span>
          <span class="text-sm text-secondary">·</span>
          <span class="text-sm text-secondary">{{ detail.customerName ?? 'Pelanggan umum' }}</span>
        </div>

        <AppTable class="mt-4" :colspan="4" dense>
          <template #head>
            <tr>
              <th>Item</th>
              <th class="text-right">Qty</th>
              <th class="text-right">Harga</th>
              <th class="text-right">Total</th>
            </tr>
          </template>
          <tr v-for="it in detail.items" :key="it.saleItemId">
            <td>
              <p class="font-medium">{{ it.itemName ?? `Item #${it.itemId}` }}</p>
              <p v-if="Number(it.returnedQty ?? 0) > 0" class="text-xs text-warn">
                Sudah diretur {{ qty(it.returnedQty) }} dari {{ qty(it.qty) }}
              </p>
            </td>
            <td class="text-right tabular-nums">{{ qty(it.qty) }}</td>
            <td class="text-right tabular-nums">{{ rupiah(it.price) }}</td>
            <td class="text-right tabular-nums">{{ rupiah(it.lineTotal) }}</td>
          </tr>
        </AppTable>

        <div class="mt-4 grid gap-4 md:grid-cols-2">
          <div class="rounded-xl bg-surface-low p-3">
            <p class="mb-2 text-xs font-semibold uppercase tracking-wider text-secondary">Pembayaran</p>
            <div
              v-for="(p, i) in detail.payments"
              :key="i"
              class="flex justify-between gap-3 py-0.5 text-sm"
            >
              <span class="text-secondary">{{ metodeLabel[p.method] ?? p.method }}</span>
              <span class="font-medium tabular-nums">{{ rupiah(p.amount) }}</span>
            </div>
            <p v-if="!detail.payments.length" class="text-sm text-secondary">Tidak ada pembayaran.</p>
          </div>
          <dl class="space-y-1.5 text-sm">
            <div class="flex justify-between gap-3">
              <dt class="text-secondary">Subtotal</dt>
              <dd class="tabular-nums">{{ rupiah(detail.subtotal) }}</dd>
            </div>
            <div class="flex justify-between gap-3">
              <dt class="text-secondary">Diskon</dt>
              <dd class="tabular-nums">{{ rupiah(detail.discountTotal) }}</dd>
            </div>
            <div class="flex justify-between gap-3">
              <dt class="text-secondary">Pajak</dt>
              <dd class="tabular-nums">{{ rupiah(detail.taxTotal) }}</dd>
            </div>
            <div class="flex justify-between gap-3 border-t border-outline/60 pt-1.5 text-base font-bold">
              <dt>Total</dt>
              <dd class="tabular-nums">{{ rupiah(detail.grandTotal) }}</dd>
            </div>
          </dl>
        </div>

        <div v-if="detail.returns?.length" class="mt-4">
          <p class="mb-2 text-xs font-semibold uppercase tracking-wider text-secondary">
            Riwayat void / retur
          </p>
          <div class="space-y-1.5">
            <div
              v-for="r in detail.returns"
              :key="r.id"
              class="flex flex-wrap items-center justify-between gap-2 rounded-lg border border-outline/60 px-3 py-2 text-sm"
            >
              <div>
                <p class="font-medium">{{ r.kind === 'void' ? 'Void nota' : 'Retur barang' }}</p>
                <p class="text-xs text-secondary">{{ r.reason }}</p>
              </div>
              <div class="text-right">
                <p class="font-medium tabular-nums">{{ rupiah(r.refundAmount) }}</p>
                <p class="text-xs text-secondary">
                  {{ r.refundMethod ? metodeLabel[r.refundMethod] ?? r.refundMethod : 'Tanpa refund' }}
                </p>
              </div>
            </div>
          </div>
        </div>
      </div>

      <template #footer>
        <AppButton variant="ghost" @click="detailOpen = false">Tutup</AppButton>
        <AppButton variant="ghost" :loading="aksi.loading.value" @click="unduhStruk">
          Struk PDF
        </AppButton>
        <AppButton variant="ghost" @click="cetakStruk">Cetak</AppButton>
        <AppButton
          v-if="detail?.status === 'lunas' && bolehRetur"
          variant="secondary"
          @click="bukaRetur"
        >
          Retur
        </AppButton>
        <AppButton
          v-if="detail?.status === 'lunas' && bolehVoid"
          variant="danger"
          @click="bukaVoid"
        >
          Void
        </AppButton>
        <AppButton
          v-else-if="detail?.status === 'lunas'"
          variant="danger"
          @click="bukaVoid"
        >
          Ajukan void
        </AppButton>
      </template>
    </AppModal>

    <AppModal
      :open="dialogVoid"
      title="Batalkan nota"
      description="Stok barang kembali dan tindakan ini tercatat di audit log."
      size="sm"
      @close="dialogVoid = false"
    >
      <AppBanner tone="red" :message="aksi.error.value" />
      <p class="text-sm text-secondary">
        Nota <span class="font-mono font-semibold text-ink">{{ detail?.number }}</span> akan
        dibatalkan.
      </p>
      <AppField label="Alasan" required class="mt-3">
        <AppTextarea v-model="alasanVoid" :rows="3" :maxlength="500" show-count placeholder="Contoh: salah input item" />
      </AppField>
      <template #footer>
        <AppButton variant="ghost" @click="dialogVoid = false">Batal</AppButton>
        <AppButton
          :variant="bolehVoid ? 'danger' : 'primary'"
          :loading="aksi.loading.value"
          :disabled="!alasanVoid.trim()"
          @click="bolehVoid ? voidSekarang() : ajukanVoid()"
        >
          {{ bolehVoid ? 'Ya, void' : 'Ajukan ke atasan' }}
        </AppButton>
      </template>
    </AppModal>

    <AppModal
      :open="dialogRetur"
      title="Retur barang"
      description="Isi qty hanya untuk baris yang dikembalikan."
      size="lg"
      @close="dialogRetur = false"
    >
      <AppBanner tone="red" :message="aksi.error.value" />
      <AppTable :colspan="5" dense>
        <template #head>
          <tr>
            <th>Item</th>
            <th class="text-right">Terjual</th>
            <th class="text-right">Sisa</th>
            <th class="text-right">Qty retur</th>
            <th>Kondisi</th>
          </tr>
        </template>
        <tr v-for="line in barisRetur" :key="line.saleItemId">
          <td>
            <p class="font-medium">{{ line.itemName }}</p>
            <p class="text-xs text-secondary tabular-nums">{{ rupiah(line.lineTotal) }}</p>
          </td>
          <td class="text-right tabular-nums">{{ qty(line.qtyTerjual) }}</td>
          <td class="text-right tabular-nums">{{ qty(line.qtyTerjual - line.sudahRetur) }}</td>
          <td class="w-28">
            <AppInput
              v-model="line.qty"
              type="number"
              min="0"
              :max="line.qtyTerjual - line.sudahRetur"
              step="any"
              placeholder="0"
            />
          </td>
          <td class="w-36">
            <AppSelect v-model="line.condition">
              <option value="layak">Layak</option>
              <option value="rusak">Rusak</option>
            </AppSelect>
          </td>
        </tr>
      </AppTable>
      <div class="mt-3 grid gap-3 sm:grid-cols-2">
        <AppField label="Kembali via" required>
          <AppSelect v-model="metodeRetur">
            <option value="tunai">Tunai</option>
            <option value="transfer">Transfer</option>
            <option value="qris">QRIS</option>
            <option value="ewallet">E-wallet</option>
          </AppSelect>
        </AppField>
        <AppField label="Alasan" required>
          <AppInput v-model="alasanRetur" :maxlength="500" placeholder="Contoh: barang cacat" />
        </AppField>
      </div>
      <div class="mt-3 flex items-center justify-between rounded-lg bg-surface-low px-3 py-2 text-sm">
        <span class="text-secondary">Perkiraan refund</span>
        <span class="font-display font-bold tabular-nums">{{ rupiah(perkiraanRetur) }}</span>
      </div>
      <p v-if="!barisDipilih.length" class="mt-2 text-xs text-secondary">
        Isi qty minimal satu baris untuk melanjutkan.
      </p>
      <template #footer>
        <AppButton variant="ghost" @click="dialogRetur = false">Batal</AppButton>
        <AppButton :loading="aksi.loading.value" :disabled="!returSah" @click="returSekarang">
          Simpan retur
        </AppButton>
      </template>
    </AppModal>
  </div>

  <div v-if="detail" class="receipt print-only">
    <div class="text-center">
      <p class="font-bold">{{ auth.companyName ?? 'Aurevant' }}</p>
      <p>{{ cabangAktif?.name ?? '' }}</p>
      <p v-if="cabangAktif">Cabang {{ cabangAktif.code }}</p>
    </div>
    <div class="my-2 border-y border-dashed border-black/40 py-1">
      <div class="flex justify-between"><span>Nota</span><span>{{ detail.number }}</span></div>
      <div class="flex justify-between"><span>Waktu</span><span>{{ waktu(detail.createdAt) }}</span></div>
      <div class="flex justify-between"><span>Kasir</span><span>{{ detail.cashierName ?? '-' }}</span></div>
      <div class="flex justify-between"><span>Pelanggan</span><span>{{ detail.customerName ?? 'Umum' }}</span></div>
    </div>
    <div class="space-y-1">
      <div v-for="it in detail.items" :key="it.saleItemId">
        <p>{{ it.itemName ?? `Item #${it.itemId}` }}</p>
        <div class="flex justify-between">
          <span>{{ qty(it.qty) }} x {{ rupiah(it.price) }}</span>
          <span>{{ rupiah(it.lineTotal) }}</span>
        </div>
      </div>
    </div>
    <div class="my-2 border-y border-dashed border-black/40 py-1">
      <div class="flex justify-between"><span>Subtotal</span><span>{{ rupiah(detail.subtotal) }}</span></div>
      <div class="flex justify-between"><span>Diskon</span><span>{{ rupiah(detail.discountTotal) }}</span></div>
      <div class="flex justify-between"><span>Pajak</span><span>{{ rupiah(detail.taxTotal) }}</span></div>
      <div class="flex justify-between font-bold"><span>Total</span><span>{{ rupiah(detail.grandTotal) }}</span></div>
    </div>
    <div class="space-y-0.5">
      <div v-for="(p, i) in detail.payments" :key="i" class="flex justify-between">
        <span>{{ metodeLabel[p.method] ?? p.method }}</span>
        <span>{{ rupiah(p.amount) }}</span>
      </div>
    </div>
    <p class="mt-2 text-center">Terima kasih atas kunjungan Anda.</p>
    <p v-if="detail.status !== 'lunas'" class="mt-1 text-center font-bold uppercase">
      {{ labelStatus(detail.status) }}
    </p>
  </div>
</template>
