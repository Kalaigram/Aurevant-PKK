<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { api } from '../lib/api'
import { rupiah } from '../lib/format'
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
import AppField from '../components/ui/AppField.vue'
import AppInput from '../components/ui/AppInput.vue'
import AppModal from '../components/ui/AppModal.vue'
import AppPageHeader from '../components/ui/AppPageHeader.vue'
import AppPager from '../components/ui/AppPager.vue'
import AppSelect from '../components/ui/AppSelect.vue'
import AppTable from '../components/ui/AppTable.vue'
import AppTabs from '../components/ui/AppTabs.vue'
import AppTextarea from '../components/ui/AppTextarea.vue'

type Tone = 'green' | 'red' | 'amber' | 'blue' | 'gray'

interface TransferItem {
  itemId: number
  itemCode: string | null
  itemName: string | null
  unit: string | null
  qtyRequested: string
  qtyShipped: string | null
  qtyReceived: string | null
  costPerUnit: string | null
}

interface Transfer {
  id: number
  fromBranchId: number
  fromBranchName: string | null
  fromBranchCode: string | null
  toBranchId: number
  toBranchName: string | null
  toBranchCode: string | null
  status: string
  note: string | null
  createdAt: string | null
  shippedAt: string | null
  receivedAt: string | null
  items: TransferItem[]
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

interface BarisBuat {
  itemId: number | null
  qty: number | null
}

interface BarisTerima {
  itemId: number
  itemName: string
  unit: string | null
  shipped: number
  qty: number | null
}

const { auth, companyId } = useCompany()
const access = useAccessStore()
const toast = useToast()
const { confirm } = useConfirm()

const detailAsync = useAsync()
const referensi = useAsync()
const aksi = useAsync()
const buatAsync = useAsync()
const terimaAsync = useAsync()

const cabang = ref<Cabang[]>([])
const items = ref<KatalogItem[]>([])
const tab = ref('semua')

const daftar = usePagedList<Transfer>((page, perPage) => {
  const params = new URLSearchParams({
    per_page: String(perPage),
    page: String(page),
  })
  if (tab.value !== 'semua') params.set('status', tab.value)
  return api.getPage<Transfer>(`/companies/${companyId.value}/stock-transfers?${params}`, auth.token)
}, { perPage: 15 })
const detail = ref<Transfer | null>(null)
const detailOpen = ref(false)

const showBuat = ref(false)
const fFrom = ref<number | null>(null)
const fTo = ref<number | null>(null)
const fNote = ref('')
const fLines = ref<BarisBuat[]>([{ itemId: null, qty: 1 }])

const showTerima = ref(false)
const tLines = ref<BarisTerima[]>([])

const tabs = [
  { key: 'semua', label: 'Semua' },
  { key: 'draft', label: 'Draft' },
  { key: 'diajukan', label: 'Diajukan' },
  { key: 'disetujui', label: 'Disetujui' },
  { key: 'dikirim', label: 'Dikirim' },
  { key: 'diterima', label: 'Diterima' },
  { key: 'dibatalkan', label: 'Dibatalkan' },
]

const statusLabel: Record<string, string> = {
  draft: 'Draft',
  diajukan: 'Diajukan',
  disetujui: 'Disetujui',
  dikirim: 'Dikirim',
  diterima: 'Diterima',
  dibatalkan: 'Dibatalkan',
}

const boleh = computed(() => access.can('stok.transfer'))
const error = computed(
  () => daftar.error.value || detailAsync.error.value || referensi.error.value,
)
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
    fFrom.value !== null &&
    fTo.value !== null &&
    fFrom.value !== fTo.value &&
    fLines.value.length > 0 &&
    !duplikat.value &&
    fLines.value.every((line) => line.itemId !== null && Number(line.qty ?? 0) > 0),
)

const barisTerima = computed(() =>
  tLines.value.filter((line) => Number(line.qty ?? 0) > 0),
)
const terimaSah = computed(
  () =>
    barisTerima.value.length > 0 &&
    tLines.value.every(
      (line) =>
        line.qty !== null && Number(line.qty) >= 0 && Number(line.qty) <= line.shipped + 0.0001,
    ),
)
const barisKurang = computed(
  () => tLines.value.filter((line) => Number(line.qty ?? 0) < line.shipped).length,
)

function tone(status: string): Tone {
  if (status === 'diterima') return 'green'
  if (status === 'dibatalkan') return 'red'
  if (status === 'dikirim') return 'blue'
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

function namaItem(it: { itemName: string | null; itemId: number }) {
  return it.itemName ?? `Item #${it.itemId}`
}

function petunjuk(status: string) {
  if (status === 'draft') return 'Transfer masih draft. Ajukan untuk mendapatkan persetujuan.'
  if (status === 'diajukan') return 'Menunggu persetujuan sebelum barang dikirim.'
  if (status === 'disetujui') return 'Transfer disetujui. Kirim barang untuk mengurangi stok asal.'
  if (status === 'dikirim') return 'Barang dalam perjalanan. Catat penerimaan di cabang tujuan.'
  if (status === 'diterima') return 'Barang sudah diterima di cabang tujuan.'
  return 'Transfer dibatalkan.'
}

function gantiTab(key: string) {
  tab.value = key
  void daftar.load(1)
}


async function lihat(id: number) {
  const res = await detailAsync.run(() =>
    api.get<Transfer>(`/companies/${companyId.value}/stock-transfers/${id}`, auth.token),
  )
  if (res) {
    detail.value = res
    detailOpen.value = true
  } else if (detailAsync.error.value) {
    toast.error(detailAsync.error.value)
  }
}

const aksiConfig: Record<string, { judul: string; pesan: string; deskripsi: string; tone: 'primary' | 'danger' }> = {
  submit: {
    judul: 'Ajukan transfer',
    pesan: 'Transfer diajukan.',
    deskripsi: 'Transfer masuk antrean persetujuan.',
    tone: 'primary',
  },
  approve: {
    judul: 'Setujui transfer',
    pesan: 'Transfer disetujui.',
    deskripsi: 'Transfer bisa dikirim setelah disetujui.',
    tone: 'primary',
  },
  ship: {
    judul: 'Kirim transfer',
    pesan: 'Transfer dikirim. Stok asal berkurang.',
    deskripsi: 'Stok cabang asal akan langsung dikurangi.',
    tone: 'primary',
  },
  cancel: {
    judul: 'Batalkan transfer',
    pesan: 'Transfer dibatalkan.',
    deskripsi: 'Transfer draft atau yang belum disetujui akan dibatalkan.',
    tone: 'danger',
  },
}

async function transisi(kind: 'submit' | 'approve' | 'ship' | 'cancel') {
  if (!detail.value) return
  const cfg = aksiConfig[kind]!
  const setuju = await confirm({
    title: cfg.judul,
    message: `Transfer #${detail.value.id} — ${detail.value.fromBranchName ?? ''} ke ${detail.value.toBranchName ?? ''}. ${cfg.deskripsi}`,
    confirmLabel: cfg.judul,
    tone: cfg.tone,
  })
  if (!setuju) return

  const id = detail.value.id
  const res = await aksi.run(() =>
    api.post<Transfer>(
      `/companies/${companyId.value}/stock-transfers/${id}/${kind}`,
      {},
      auth.token,
    ),
  )
  if (res) {
    detail.value = res
    toast.success(cfg.pesan)
    await daftar.reload()
  } else if (aksi.error.value) {
    toast.error(aksi.error.value)
  }
}

function bukaBuat() {
  fFrom.value =
    auth.branchId && cabang.value.some((b) => b.id === auth.branchId)
      ? auth.branchId
      : (cabang.value[0]?.id ?? null)
  fTo.value = null
  fNote.value = ''
  fLines.value = [{ itemId: null, qty: 1 }]
  buatAsync.error.value = ''
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
  if (!buatSah.value) return
  const res = await buatAsync.run(() =>
    api.post<Transfer>(
      `/companies/${companyId.value}/stock-transfers`,
      {
        from_branch_id: fFrom.value,
        to_branch_id: fTo.value,
        items: fLines.value.map((line) => ({
          item_id: line.itemId,
          qty: Number(line.qty),
        })),
        note: fNote.value.trim() || undefined,
      },
      auth.token,
    ),
  )
  if (res) {
    showBuat.value = false
    detail.value = res
    detailOpen.value = true
    toast.success('Transfer dibuat sebagai draft.')
    await daftar.load(1)
  }
}

function bukaTerima() {
  if (!detail.value) return
  tLines.value = detail.value.items.map((it) => {
    const shipped = Number(it.qtyShipped ?? it.qtyRequested)
    return {
      itemId: it.itemId,
      itemName: namaItem(it),
      unit: it.unit,
      shipped,
      qty: shipped,
    }
  })
  terimaAsync.error.value = ''
  detailOpen.value = false
  showTerima.value = true
}

function tutupTerima() {
  showTerima.value = false
  if (detail.value) detailOpen.value = true
}

async function terimaSekarang() {
  if (!detail.value || !terimaSah.value) return
  const id = detail.value.id
  const res = await terimaAsync.run(() =>
    api.post<Transfer>(
      `/companies/${companyId.value}/stock-transfers/${id}/receive`,
      {
        items: tLines.value.map((line) => ({
          item_id: line.itemId,
          qty_received: Number(line.qty),
        })),
      },
      auth.token,
    ),
  )
  if (res) {
    showTerima.value = false
    detail.value = res
    detailOpen.value = true
    toast.success('Penerimaan transfer dicatat. Stok tujuan bertambah.')
    await daftar.reload()
  } else if (terimaAsync.error.value) {
    toast.error(terimaAsync.error.value)
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
  await daftar.load(1)
})
</script>

<template>
  <div class="screen-only">
    <AppPageHeader
      eyebrow="Persediaan"
      title="Transfer stok"
      subtitle="Kirim barang antar cabang dengan alur persetujuan dan pencatatan selisih terima."
      icon="swap_horiz"
    >
      <template #actions>
        <AppButton variant="ghost" :loading="daftar.loading.value" @click="daftar.reload()">Muat ulang</AppButton>
        <AppButton v-if="boleh" @click="bukaBuat">Transfer baru</AppButton>
      </template>
    </AppPageHeader>

    <AppBanner tone="red" :message="error" />

    <AppCard>
      <AppTabs :items="tabs" :model-value="tab" label="Status transfer" @update:model-value="gantiTab" />

      <AppTable
        class="mt-3"
        :loading="daftar.loading.value"
        :empty="!daftar.rows.value.length"
        :colspan="5"
        empty-title="Belum ada transfer"
        empty-message="Buat transfer baru atau ubah filter status."
      >
        <template #head>
          <tr>
            <th>Transfer</th>
            <th>Rute</th>
            <th class="text-right">Item</th>
            <th class="hidden sm:table-cell">Catatan</th>
            <th>Status</th>
          </tr>
        </template>
        <tr
          v-for="tr in daftar.rows.value"
          :key="tr.id"
          class="cursor-pointer transition hover:bg-surface-low/70"
          @click="lihat(tr.id)"
        >
          <td>
            <p class="font-mono text-sm font-semibold text-ink">TRF #{{ tr.id }}</p>
            <p class="mt-0.5 text-xs text-secondary">{{ waktu(tr.createdAt) }}</p>
          </td>
          <td>
            <p class="font-medium">
              {{ tr.fromBranchName ?? `#${tr.fromBranchId}` }}
              <span class="text-secondary">→</span>
              {{ tr.toBranchName ?? `#${tr.toBranchId}` }}
            </p>
            <p class="font-mono text-xs text-secondary">
              {{ tr.fromBranchCode ?? '—' }} → {{ tr.toBranchCode ?? '—' }}
            </p>
          </td>
          <td class="text-right tabular-nums">{{ tr.items.length }}</td>
          <td class="hidden sm:table-cell text-secondary">{{ tr.note ?? '—' }}</td>
          <td><AppBadge :tone="tone(tr.status)" dot>{{ labelStatus(tr.status) }}</AppBadge></td>
        </tr>
      </AppTable>
      <AppPager :meta="daftar.meta.value" @page="daftar.setPage" @update:per-page="daftar.setPerPage" />
    </AppCard>

    <AppModal
      :open="detailOpen"
      :title="detail ? `Transfer #${detail.id}` : 'Detail transfer'"
      :description="
        detail
          ? `${detail.fromBranchName ?? ''} → ${detail.toBranchName ?? ''} · ${waktu(detail.createdAt)}`
          : ''
      "
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
          <span v-if="detail.shippedAt" class="text-sm text-secondary">· Dikirim {{ waktu(detail.shippedAt) }}</span>
          <span v-if="detail.receivedAt" class="text-sm text-secondary">· Diterima {{ waktu(detail.receivedAt) }}</span>
        </div>
        <p class="mt-2 text-sm text-secondary">{{ petunjuk(detail.status) }}</p>

        <div class="mt-3 grid gap-3 rounded-xl bg-surface-low p-3 text-sm sm:grid-cols-2">
          <div>
            <p class="text-xs font-semibold uppercase tracking-wider text-secondary">Dari</p>
            <p class="mt-0.5 font-medium">{{ detail.fromBranchName ?? `#${detail.fromBranchId}` }}</p>
            <p class="font-mono text-xs text-secondary">{{ detail.fromBranchCode ?? '—' }}</p>
          </div>
          <div>
            <p class="text-xs font-semibold uppercase tracking-wider text-secondary">Ke</p>
            <p class="mt-0.5 font-medium">{{ detail.toBranchName ?? `#${detail.toBranchId}` }}</p>
            <p class="font-mono text-xs text-secondary">{{ detail.toBranchCode ?? '—' }}</p>
          </div>
        </div>
        <p v-if="detail.note" class="mt-2 rounded-lg border border-outline/60 px-3 py-2 text-sm">
          <span class="text-secondary">Catatan: </span>{{ detail.note }}
        </p>

        <AppTable class="mt-4" :colspan="5" dense>
          <template #head>
            <tr>
              <th>Item</th>
              <th class="text-right">Diminta</th>
              <th class="text-right">Dikirim</th>
              <th class="text-right">Diterima</th>
              <th class="text-right">Modal / unit</th>
            </tr>
          </template>
          <tr v-for="(it, i) in detail.items" :key="`${it.itemId}-${i}`">
            <td>
              <p class="font-medium">{{ namaItem(it) }}</p>
              <p class="font-mono text-xs text-secondary">{{ it.itemCode ?? '' }} · {{ it.unit ?? 'pcs' }}</p>
            </td>
            <td class="text-right tabular-nums">{{ qty(it.qtyRequested) }}</td>
            <td class="text-right tabular-nums">{{ it.qtyShipped === null ? '—' : qty(it.qtyShipped) }}</td>
            <td class="text-right tabular-nums">
              {{ it.qtyReceived === null ? '—' : qty(it.qtyReceived) }}
            </td>
            <td class="text-right tabular-nums">
              {{ it.costPerUnit === null ? '—' : rupiah(it.costPerUnit) }}
            </td>
          </tr>
        </AppTable>
      </div>

      <template #footer>
        <AppButton variant="ghost" @click="detailOpen = false">Tutup</AppButton>
        <template v-if="boleh">
          <AppButton
            v-if="detail?.status === 'draft'"
            :loading="aksi.loading.value"
            @click="transisi('submit')"
          >
            Ajukan
          </AppButton>
          <AppButton
            v-if="detail?.status === 'diajukan'"
            :loading="aksi.loading.value"
            @click="transisi('approve')"
          >
            Setujui
          </AppButton>
          <AppButton
            v-if="detail?.status === 'disetujui'"
            :loading="aksi.loading.value"
            @click="transisi('ship')"
          >
            Kirim
          </AppButton>
          <AppButton
            v-if="detail?.status === 'dikirim'"
            variant="secondary"
            @click="bukaTerima"
          >
            Terima
          </AppButton>
          <AppButton
            v-if="detail && ['draft', 'diajukan'].includes(detail.status)"
            variant="danger"
            :loading="aksi.loading.value"
            @click="transisi('cancel')"
          >
            Batalkan
          </AppButton>
        </template>
      </template>
    </AppModal>

    <AppModal
      :open="showBuat"
      title="Transfer baru"
      description="Transfer disimpan sebagai draft dan perlu diajukan untuk persetujuan."
      size="lg"
      @close="showBuat = false"
    >
      <AppBanner tone="red" :message="buatAsync.error.value" />
      <div class="grid gap-3 sm:grid-cols-2">
        <AppField label="Dari cabang" required>
          <AppSelect v-model="fFrom" :options="opsiCabang" placeholder="Pilih cabang asal" />
        </AppField>
        <AppField label="Ke cabang" required>
          <AppSelect v-model="fTo" :options="opsiCabang" placeholder="Pilih cabang tujuan" />
        </AppField>
      </div>
      <p v-if="fFrom !== null && fFrom === fTo" class="mt-1 text-xs text-danger">
        Cabang asal dan tujuan harus berbeda.
      </p>

      <div class="mt-4 flex items-center justify-between">
        <p class="text-xs font-semibold uppercase tracking-wider text-secondary">Baris item</p>
        <AppButton variant="ghost" @click="tambahBaris">Tambah baris</AppButton>
      </div>
      <div
        v-for="(line, i) in fLines"
        :key="i"
        class="mt-2 grid items-end gap-2 rounded-lg border border-outline/60 p-2 sm:grid-cols-12"
      >
        <AppField label="Item" class="sm:col-span-8">
          <AppSelect v-model="line.itemId" :options="opsiItem" placeholder="Pilih item" />
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
      <p v-if="duplikat" class="mt-2 text-xs text-danger">
        Item yang sama tidak boleh muncul di dua baris.
      </p>

      <AppField label="Catatan" class="mt-3">
        <AppTextarea v-model="fNote" :rows="2" :maxlength="500" show-count placeholder="Contoh: restok mingguan toko" />
      </AppField>

      <template #footer>
        <AppButton variant="ghost" @click="showBuat = false">Batal</AppButton>
        <AppButton :loading="buatAsync.loading.value" :disabled="!buatSah" @click="buat">
          Simpan draft
        </AppButton>
      </template>
    </AppModal>

    <AppModal
      :open="showTerima"
      title="Terima transfer"
      description="Catat qty yang benar-benar diterima. Selisih menjadi tanggung jawab pengiriman."
      size="lg"
      @close="tutupTerima"
    >
      <AppBanner tone="red" :message="terimaAsync.error.value" />
      <AppTable :colspan="4" dense>
        <template #head>
          <tr>
            <th>Item</th>
            <th class="text-right">Dikirim</th>
            <th class="text-right">Qty diterima</th>
            <th class="text-right">Selisih</th>
          </tr>
        </template>
        <tr v-for="line in tLines" :key="line.itemId">
          <td>
            <p class="font-medium">{{ line.itemName }}</p>
            <p class="text-xs text-secondary">{{ line.unit ?? 'pcs' }}</p>
          </td>
          <td class="text-right tabular-nums">{{ qty(line.shipped) }}</td>
          <td class="w-28">
            <AppInput
              v-model="line.qty"
              type="number"
              min="0"
              :max="line.shipped"
              step="any"
            />
          </td>
          <td
            class="text-right tabular-nums"
            :class="line.shipped - Number(line.qty ?? 0) > 0 ? 'text-warn' : 'text-ok'"
          >
            {{ qty(line.shipped - Number(line.qty ?? 0)) }}
          </td>
        </tr>
      </AppTable>
      <p v-if="!barisTerima.length" class="mt-2 text-xs text-secondary">
        Isi qty minimal satu baris untuk melanjutkan.
      </p>

      <div
        class="mt-3 flex flex-wrap items-center justify-between gap-2 rounded-lg bg-surface-low px-3 py-2 text-sm"
      >
        <span class="text-secondary">
          {{ barisTerima.length }} dari {{ tLines.length }} baris diisi
        </span>
        <span v-if="barisKurang > 0" class="font-semibold text-warn">
          {{ barisKurang }} baris kurang
        </span>
        <span v-else class="font-semibold text-ok">Lengkap</span>
      </div>

      <template #footer>
        <AppButton variant="ghost" @click="tutupTerima">Batal</AppButton>
        <AppButton :loading="terimaAsync.loading.value" :disabled="!terimaSah" @click="terimaSekarang">
          Simpan penerimaan
        </AppButton>
      </template>
    </AppModal>
  </div>
</template>
