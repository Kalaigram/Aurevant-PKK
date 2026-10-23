<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { api } from '../lib/api'
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
type Aksi = 'submit' | 'approve' | 'reject'

interface OpnameItem {
  itemId: number
  itemCode: string | null
  itemName: string | null
  unit: string | null
  qtySystem: string
  qtyCounted: string
  diff: string
}

interface Opname {
  id: number
  branchId: number
  branchName: string | null
  status: string
  note: string | null
  createdAt: string | null
  items: OpnameItem[]
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

interface BarisHitung {
  itemId: number | null
  qtyCounted: number | null
}

const { auth, companyId } = useCompany()
const access = useAccessStore()
const toast = useToast()
const { confirm } = useConfirm()

const detailAsync = useAsync()
const referensi = useAsync()
const aksi = useAsync()
const buatAsync = useAsync()

const cabang = ref<Cabang[]>([])
const items = ref<KatalogItem[]>([])
const branchId = ref<number | null>(null)
const tab = ref('semua')

const daftar = usePagedList<Opname>((page, perPage) => {
  if (!branchId.value) return Promise.resolve({ data: [], meta: null })
  const params = new URLSearchParams({
    per_page: String(perPage),
    page: String(page),
  })
  if (tab.value !== 'semua') params.set('status', tab.value)
  return api.getPage<Opname>(
    `/companies/${companyId.value}/branches/${branchId.value}/stock/opnames?${params}`,
    auth.token,
  )
}, { perPage: 15 })

const detail = ref<Opname | null>(null)
const detailOpen = ref(false)

const showBuat = ref(false)
const fNote = ref('')
const fLines = ref<BarisHitung[]>([{ itemId: null, qtyCounted: 0 }])

const tabs = [
  { key: 'semua', label: 'Semua' },
  { key: 'draft', label: 'Draft' },
  { key: 'diajukan', label: 'Diajukan' },
  { key: 'disetujui', label: 'Disetujui' },
  { key: 'ditolak', label: 'Ditolak' },
]

const statusLabel: Record<string, string> = {
  draft: 'Draft',
  diajukan: 'Diajukan',
  disetujui: 'Disetujui',
  ditolak: 'Ditolak',
}

const aksiConfig: Record<Aksi, { judul: string; pesan: string; deskripsi: string; tone: 'primary' | 'danger' }> = {
  submit: {
    judul: 'Ajukan opname',
    pesan: 'Opname diajukan.',
    deskripsi: 'Opname masuk antrean persetujuan atasan.',
    tone: 'primary',
  },
  approve: {
    judul: 'Setujui opname',
    pesan: 'Opname disetujui, stok menyesuaikan.',
    deskripsi: 'Selisih akan langsung diterapkan ke saldo stok cabang.',
    tone: 'primary',
  },
  reject: {
    judul: 'Tolak opname',
    pesan: 'Opname ditolak.',
    deskripsi: 'Stok tidak berubah dan opname dianggap selesai.',
    tone: 'danger',
  },
}

const boleh = computed(() => access.can('stok.opname'))
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
    fLines.value.length > 0 &&
    !duplikat.value &&
    fLines.value.every(
      (line) =>
        line.itemId !== null &&
        line.qtyCounted !== null &&
        Number(line.qtyCounted) >= 0,
    ),
)

function tone(status: string): Tone {
  if (status === 'disetujui') return 'green'
  if (status === 'ditolak') return 'red'
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

function qtyBersign(value: string | number | null | undefined) {
  const number = Number(value ?? 0)
  if (number > 0) return `+${qty(number)}`
  if (number < 0) return `-${qty(Math.abs(number))}`
  return '0'
}

function namaItem(it: { itemName: string | null; itemId: number }) {
  return it.itemName ?? `Item #${it.itemId}`
}

function jumlahSelisih(opname: Opname) {
  return opname.items.filter((it) => Number(it.diff) !== 0).length
}

function petunjuk(status: string) {
  if (status === 'draft') return 'Opname masih draft. Ajukan ke atasan untuk mendapatkan persetujuan.'
  if (status === 'diajukan') return 'Menunggu persetujuan. Stok belum berubah sampai disetujui.'
  if (status === 'disetujui') return 'Opname disetujui dan selisih sudah diterapkan ke stok.'
  return 'Opname ditolak. Stok tidak berubah.'
}

function gantiTab(key: string) {
  tab.value = key
  void daftar.load(1)
}

function gantiCabang(value: string | number | null) {
  branchId.value = value === null ? null : Number(value)
  detail.value = null
  detailOpen.value = false
  void daftar.load(1)
}

async function lihat(id: number) {
  if (!branchId.value) return
  const res = await detailAsync.run(() =>
    api.get<Opname>(
      `/companies/${companyId.value}/branches/${branchId.value}/stock/opnames/${id}`,
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

async function transisi(kind: Aksi) {
  if (!detail.value || !branchId.value) return
  const cfg = aksiConfig[kind]
  const setuju = await confirm({
    title: cfg.judul,
    message: `Opname #${detail.value.id}. ${cfg.deskripsi}`,
    confirmLabel: cfg.judul,
    tone: cfg.tone,
  })
  if (!setuju) return

  const id = detail.value.id
  const res = await aksi.run(() =>
    api.post<Opname>(
      `/companies/${companyId.value}/branches/${branchId.value}/stock/opnames/${id}/${kind}`,
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
  fNote.value = ''
  fLines.value = [{ itemId: null, qtyCounted: 0 }]
  buatAsync.error.value = ''
  showBuat.value = true
}

function tambahBaris() {
  fLines.value.push({ itemId: null, qtyCounted: 0 })
}

function hapusBaris(index: number) {
  if (fLines.value.length <= 1) return
  fLines.value.splice(index, 1)
}

async function buat() {
  if (!branchId.value || !buatSah.value) return
  const res = await buatAsync.run(() =>
    api.post<Opname>(
      `/companies/${companyId.value}/branches/${branchId.value}/stock/opnames`,
      {
        items: fLines.value.map((line) => ({
          item_id: line.itemId,
          qty_counted: Number(line.qtyCounted),
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
    toast.success('Opname dibuat sebagai draft.')
    await daftar.load(1)
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
  await daftar.load(1)
})
</script>

<template>
  <div>
    <AppPageHeader
      eyebrow="Persediaan"
      title="Stok opname"
      subtitle="Hitung fisik stok cabang, ajukan selisih, dan terapkan setelah disetujui."
      icon="content_paste_search"
    >
      <template #actions>
        <AppButton variant="ghost" :loading="daftar.loading.value" @click="daftar.reload()">Muat ulang</AppButton>
        <AppButton v-if="boleh && branchId" @click="bukaBuat">Opname baru</AppButton>
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
      </div>

      <AppTabs :items="tabs" :model-value="tab" label="Status opname" @update:model-value="gantiTab" />

      <AppTable
        class="mt-3"
        :loading="daftar.loading.value"
        :empty="!daftar.rows.value.length"
        :colspan="5"
        empty-title="Belum ada opname"
        empty-message="Buat opname baru atau ubah filter status."
      >
        <template #head>
          <tr>
            <th>Opname</th>
            <th class="text-right">Item</th>
            <th class="text-right">Item selisih</th>
            <th class="hidden sm:table-cell">Catatan</th>
            <th>Status</th>
          </tr>
        </template>
        <tr
          v-for="o in daftar.rows.value"
          :key="o.id"
          class="cursor-pointer transition hover:bg-surface-low/70"
          @click="lihat(o.id)"
        >
          <td>
            <p class="font-mono text-sm font-semibold text-ink">OPN #{{ o.id }}</p>
            <p class="mt-0.5 text-xs text-secondary">{{ waktu(o.createdAt) }}</p>
          </td>
          <td class="text-right tabular-nums">{{ o.items.length }}</td>
          <td
            class="text-right font-medium tabular-nums"
            :class="jumlahSelisih(o) > 0 ? 'text-warn' : 'text-secondary'"
          >
            {{ jumlahSelisih(o) }}
          </td>
          <td class="hidden sm:table-cell text-secondary">{{ o.note ?? '—' }}</td>
          <td><AppBadge :tone="tone(o.status)" dot>{{ labelStatus(o.status) }}</AppBadge></td>
        </tr>
      </AppTable>
      <AppPager :meta="daftar.meta.value" @page="daftar.setPage" @update:per-page="daftar.setPerPage" />
    </AppCard>

    <AppModal
      :open="detailOpen"
      :title="detail ? `Opname #${detail.id}` : 'Detail opname'"
      :description="detail ? `${detail.branchName ?? ''} · ${waktu(detail.createdAt)}` : ''"
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
          <span class="text-sm text-secondary">{{ detail.items.length }} item</span>
          <span class="text-sm text-secondary">·</span>
          <span class="text-sm text-secondary">{{ jumlahSelisih(detail) }} item selisih</span>
        </div>
        <p class="mt-2 text-sm text-secondary">{{ petunjuk(detail.status) }}</p>

        <p v-if="detail.note" class="mt-3 rounded-lg border border-outline/60 px-3 py-2 text-sm">
          <span class="text-secondary">Catatan: </span>{{ detail.note }}
        </p>

        <AppTable class="mt-4" :colspan="5" dense>
          <template #head>
            <tr>
              <th>Item</th>
              <th class="text-right">Sistem</th>
              <th class="text-right">Hitung fisik</th>
              <th class="text-right">Selisih</th>
              <th class="hidden sm:table-cell">Satuan</th>
            </tr>
          </template>
          <tr v-for="(it, i) in detail.items" :key="`${it.itemId}-${i}`">
            <td>
              <p class="font-medium">{{ namaItem(it) }}</p>
              <p class="font-mono text-xs text-secondary">{{ it.itemCode ?? '' }}</p>
            </td>
            <td class="text-right tabular-nums">{{ qty(it.qtySystem) }}</td>
            <td class="text-right tabular-nums">{{ qty(it.qtyCounted) }}</td>
            <td
              class="text-right font-semibold tabular-nums"
              :class="Number(it.diff) !== 0 ? 'text-warn' : 'text-secondary'"
            >
              {{ qtyBersign(it.diff) }}
            </td>
            <td class="hidden text-secondary sm:table-cell">{{ it.unit ?? 'pcs' }}</td>
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
            v-if="detail?.status === 'diajukan'"
            variant="danger"
            :loading="aksi.loading.value"
            @click="transisi('reject')"
          >
            Tolak
          </AppButton>
        </template>
      </template>
    </AppModal>

    <AppModal
      :open="showBuat"
      title="Opname baru"
      description="Masukkan hasil hitung fisik. Qty sistem diambil otomatis saat opname dibuat."
      size="lg"
      @close="showBuat = false"
    >
      <AppBanner tone="red" :message="buatAsync.error.value" />
      <div class="flex items-center justify-between">
        <p class="text-xs font-semibold uppercase tracking-wider text-secondary">Baris hitung</p>
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
        <AppField label="Qty hasil hitung" class="sm:col-span-3">
          <AppInput v-model="line.qtyCounted" type="number" min="0" step="any" placeholder="0" />
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
      <p class="mt-2 text-xs text-secondary">
        Item yang tidak dihitung tidak perlu dimasukkan. Selisih baru diterapkan setelah opname disetujui.
      </p>

      <AppField label="Catatan" class="mt-3">
        <AppTextarea v-model="fNote" :rows="2" :maxlength="500" show-count placeholder="Contoh: opname bulanan area gudang" />
      </AppField>

      <template #footer>
        <AppButton variant="ghost" @click="showBuat = false">Batal</AppButton>
        <AppButton :loading="buatAsync.loading.value" :disabled="!buatSah" @click="buat">
          Simpan draft
        </AppButton>
      </template>
    </AppModal>
  </div>
</template>
