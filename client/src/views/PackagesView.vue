<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { api } from '../lib/api'
import { rupiah } from '../lib/format'
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
import AppField from '../components/ui/AppField.vue'
import AppInput from '../components/ui/AppInput.vue'
import AppModal from '../components/ui/AppModal.vue'
import AppMoneyInput from '../components/ui/AppMoneyInput.vue'
import AppPageHeader from '../components/ui/AppPageHeader.vue'
import AppPager from '../components/ui/AppPager.vue'
import AppSearch from '../components/ui/AppSearch.vue'
import AppSelect from '../components/ui/AppSelect.vue'
import AppTable from '../components/ui/AppTable.vue'

interface PaketItem {
  itemId: number
  itemCode: string | null
  itemName: string | null
  unit: string | null
  qty: string
}

interface Paket {
  id: number
  companyId: number
  code: string
  name: string
  price: string
  status: string
  itemCount: number
  items: PaketItem[]
}

interface Jasa {
  id: number
  code: string
  name: string
  unit: string | null
  status: string
}

interface BarisPaket {
  itemId: number | null
  qty: number | null
}

const { auth, companyId } = useCompany()
const access = useAccessStore()
const toast = useToast()
const { confirm } = useConfirm()

const referensiAsync = useAsync()
const mutasi = useAsync()

const cari = ref('')
const jasa = ref<Jasa[]>([])

const {
  rows: paket,
  meta,
  loading,
  error: errorDaftar,
  load,
  setPerPage,
} = usePagedList<Paket>((page, perPage) => {
  const params = new URLSearchParams({ per_page: String(perPage), page: String(page) })
  if (cari.value.trim()) params.set('q', cari.value.trim())
  return api.getPage<Paket>(`/companies/${companyId.value}/service-packages?${params}`, auth.token)
})

const paketOpen = ref(false)
const paketEdit = ref<Paket | null>(null)
const fCode = ref('')
const fName = ref('')
const fPrice = ref<number | null>(null)
const fStatus = ref('aktif')
const fItems = ref<BarisPaket[]>([])

const bolehKatalog = computed(() => access.can('katalog.kelola'))
const error = computed(() => errorDaftar.value || referensiAsync.error.value)
const opsiJasa = computed(() =>
  jasa.value.map((j) => ({
    value: j.id,
    label: `${j.code} - ${j.name}${j.unit ? ` (${j.unit})` : ''}`,
  })),
)
const duplikatJasa = computed(() => {
  const seen = new Set<number>()
  for (const line of fItems.value) {
    if (line.itemId === null) continue
    if (seen.has(line.itemId)) return true
    seen.add(line.itemId)
  }
  return false
})
const paketSah = computed(
  () =>
    fCode.value.trim() !== '' &&
    fName.value.trim() !== '' &&
    fPrice.value !== null &&
    Number(fPrice.value) >= 0 &&
    fItems.value.length > 0 &&
    !duplikatJasa.value &&
    fItems.value.every(
      (line) => line.itemId !== null && line.qty !== null && Number(line.qty) > 0,
    ),
)

function isiPaket(p: Paket) {
  fCode.value = p.code
  fName.value = p.name
  fPrice.value = Number(p.price)
  fStatus.value = p.status
  fItems.value = p.items.map((item) => ({ itemId: item.itemId, qty: Number(item.qty) }))
}

function bukaPaket(p?: Paket) {
  mutasi.error.value = ''
  paketEdit.value = p ?? null
  if (p) {
    isiPaket(p)
  } else {
    fCode.value = ''
    fName.value = ''
    fPrice.value = null
    fStatus.value = 'aktif'
    fItems.value = [{ itemId: null, qty: 1 }]
  }
  paketOpen.value = true
}

function tambahBaris() {
  fItems.value.push({ itemId: null, qty: 1 })
}

function hapusBaris(index: number) {
  if (fItems.value.length <= 1) return
  fItems.value.splice(index, 1)
}

async function simpan() {
  if (!paketSah.value) return
  const body = {
    code: fCode.value.trim(),
    name: fName.value.trim(),
    price: Number(fPrice.value),
    status: fStatus.value,
    items: fItems.value.map((line) => ({
      item_id: line.itemId,
      qty: Number(line.qty),
    })),
  }
  const base = `/companies/${companyId.value}/service-packages`
  const res = await mutasi.run(() =>
    paketEdit.value
      ? api.put<Paket>(`${base}/${paketEdit.value.id}`, body, auth.token)
      : api.post<Paket>(base, body, auth.token),
  )
  if (!res) {
    if (mutasi.error.value) toast.error(mutasi.error.value)
    return
  }
  paketOpen.value = false
  toast.success(paketEdit.value ? 'Paket jasa diperbarui.' : 'Paket jasa dibuat.')
  await load(1)
}

async function hapus(p: Paket) {
  const setuju = await confirm({
    title: 'Hapus paket jasa?',
    message: `Paket ${p.name} akan dihapus. Order lama tetap menyimpan rincian jasanya.`,
    confirmLabel: 'Hapus',
    tone: 'danger',
  })
  if (!setuju) return
  const res = await mutasi.run(() =>
    api.del(`/companies/${companyId.value}/service-packages/${p.id}`, auth.token),
  )
  if (!res) {
    if (mutasi.error.value) toast.error(mutasi.error.value)
    return
  }
  toast.success('Paket jasa dihapus.')
  await load(1)
}

function rincian(p: Paket) {
  return p.items
    .map((item) => `${item.itemName ?? `Item #${item.itemId}`} x${Number(item.qty)}`)
    .join(', ')
}

function labelStatus(status: string) {
  return status === 'aktif' ? 'Aktif' : status === 'nonaktif' ? 'Nonaktif' : status
}

onMounted(async () => {
  await access.load(companyId.value, auth.token)
  const data = await referensiAsync.run(() =>
    api.getPage<Jasa>(`/companies/${companyId.value}/items?type=jasa&per_page=100`, auth.token),
  )
  jasa.value = data?.data ?? []
  await load(1)
})
</script>

<template>
  <div>
    <AppPageHeader
      eyebrow="Katalog"
      title="Paket jasa"
      subtitle="Gabungkan beberapa jasa menjadi satu paket dengan harga khusus."
      icon="inventory_2"
    />

    <AppBanner tone="red" :message="error" />

    <AppCard>
      <div class="mb-3 flex flex-wrap items-end gap-2">
        <AppSearch
          v-model="cari"
          class="min-w-56 flex-1"
          placeholder="Cari kode atau nama paket..."
          @search="load(1)"
        />
        <AppButton v-if="bolehKatalog" @click="bukaPaket()">Paket baru</AppButton>
      </div>

      <AppTable
        :loading="loading"
        :empty="!paket.length"
        :colspan="5"
        empty-title="Belum ada paket jasa"
        empty-message="Buat paket untuk menggabungkan beberapa jasa dengan satu harga."
      >
        <template #head>
          <tr>
            <th>Paket</th>
            <th>Isi paket</th>
            <th class="text-right">Harga</th>
            <th>Status</th>
            <th v-if="bolehKatalog" class="text-right">Aksi</th>
          </tr>
        </template>
        <tr v-for="p in paket" :key="p.id">
          <td>
            <p class="font-medium text-ink">{{ p.name }}</p>
            <p class="font-mono text-xs text-secondary">{{ p.code }}</p>
          </td>
          <td class="max-w-md text-sm text-secondary">
            <span class="line-clamp-2">{{ rincian(p) }}</span>
            <span class="ml-1 whitespace-nowrap text-xs">({{ p.itemCount }} jasa)</span>
          </td>
          <td class="text-right font-medium tabular-nums">{{ rupiah(p.price) }}</td>
          <td>
            <AppBadge :tone="p.status === 'aktif' ? 'green' : 'gray'">
              {{ labelStatus(p.status) }}
            </AppBadge>
          </td>
          <td v-if="bolehKatalog" class="text-right">
            <div class="flex justify-end gap-2">
              <AppButton variant="ghost" size="sm" @click="bukaPaket(p)">Ubah</AppButton>
              <AppButton
                variant="ghost"
                size="sm"
                class="text-danger"
                :aria-label="`Hapus paket ${p.name}`"
                @click="hapus(p)"
              >
                Hapus
              </AppButton>
            </div>
          </td>
        </tr>
      </AppTable>
      <AppPager :meta="meta" @page="load" @update:per-page="setPerPage" />
    </AppCard>

    <AppModal
      :open="paketOpen"
      :title="paketEdit ? 'Ubah paket jasa' : 'Paket jasa baru'"
      description="Harga paket berlaku di semua cabang. Harga jasa per cabang tetap dipakai untuk menghitung pembagian komisi."
      size="lg"
      @close="paketOpen = false"
    >
      <AppBanner tone="red" :message="mutasi.error.value" />
      <form id="paket-form" @submit.prevent="simpan">
        <div class="grid gap-3 sm:grid-cols-2">
          <AppField label="Kode paket">
            <AppInput v-model="fCode" maxlength="50" class="font-mono" data-autofocus />
          </AppField>
          <AppField label="Status">
            <AppSelect v-model="fStatus">
              <option value="aktif">Aktif</option>
              <option value="nonaktif">Nonaktif</option>
            </AppSelect>
          </AppField>
          <AppField label="Nama paket" class="sm:col-span-2">
            <AppInput v-model="fName" maxlength="255" />
          </AppField>
          <AppField label="Harga paket" hint="Total harga yang dibayar pelanggan untuk seluruh isi paket.">
            <AppMoneyInput v-model="fPrice" />
          </AppField>
        </div>

        <div class="mt-4 flex items-center justify-between">
          <p class="text-xs font-semibold uppercase tracking-wider text-secondary">Jasa dalam paket</p>
          <AppButton variant="ghost" size="sm" @click="tambahBaris">Tambah jasa</AppButton>
        </div>

        <div
          v-for="(line, i) in fItems"
          :key="i"
          class="mt-2 grid items-end gap-2 rounded-lg border border-outline/60 p-2 sm:grid-cols-12"
        >
          <AppField label="Jasa" class="sm:col-span-8">
            <AppSelect v-model="line.itemId" :options="opsiJasa" placeholder="Pilih jasa" />
          </AppField>
          <AppField label="Qty" class="sm:col-span-3">
            <AppInput v-model="line.qty" type="number" min="0.001" step="any" placeholder="1" />
          </AppField>
          <div class="sm:col-span-1">
            <button
              type="button"
              class="w-full rounded-lg px-2 py-2.5 text-xs font-semibold text-danger transition hover:bg-danger-bg disabled:cursor-not-allowed disabled:opacity-40"
              :disabled="fItems.length <= 1"
              @click="hapusBaris(i)"
            >
              Hapus
            </button>
          </div>
        </div>
        <p v-if="duplikatJasa" class="mt-2 text-xs text-danger">
          Jasa yang sama tidak boleh muncul di dua baris paket.
        </p>
      </form>
      <template #footer>
        <AppButton variant="ghost" @click="paketOpen = false">Batal</AppButton>
        <AppButton
          type="submit"
          form="paket-form"
          :loading="mutasi.loading.value"
          :disabled="!paketSah"
        >
          Simpan
        </AppButton>
      </template>
    </AppModal>
  </div>
</template>
