<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { api } from '../lib/api'
import { rupiah } from '../lib/format'
import { useCompany } from '../composables/useCompany'
import { useAccessStore } from '../stores/access'
import { useAsync } from '../composables/useAsync'
import { usePagedList } from '../composables/usePagedList'
import { useToast } from '../composables/useToast'
import AppBadge from '../components/ui/AppBadge.vue'
import AppBanner from '../components/ui/AppBanner.vue'
import AppButton from '../components/ui/AppButton.vue'
import AppCard from '../components/ui/AppCard.vue'
import AppField from '../components/ui/AppField.vue'
import AppInput from '../components/ui/AppInput.vue'
import AppPageHeader from '../components/ui/AppPageHeader.vue'
import AppPager from '../components/ui/AppPager.vue'
import AppSearch from '../components/ui/AppSearch.vue'
import AppSelect from '../components/ui/AppSelect.vue'
import AppTable from '../components/ui/AppTable.vue'

interface Cabang {
  id: number
  code: string
  name: string
}

interface ItemPrice {
  branchId: number
  price: string
}

interface Item {
  id: number
  code: string
  name: string
  type: string
  unit: string | null
  prices: ItemPrice[]
}

const { auth, companyId } = useCompany()
const access = useAccessStore()
const toast = useToast()
const referensi = useAsync()
const simpanAsync = useAsync()

const cabang = ref<Cabang[]>([])
const branchId = ref<number | null>(null)
const cari = ref('')
const filterTipe = ref('')
const hargaEdit = ref<Record<number, number | null>>({})
const menyimpanId = ref<number | null>(null)

const bolehUbah = computed(() => access.can('harga.atur'))

const {
  rows: items,
  meta,
  loading,
  error: errorDaftar,
  load: loadItems,
  setPerPage,
} = usePagedList<Item>((page, perPage) => {
  const params = new URLSearchParams({ per_page: String(perPage), page: String(page) })
  if (filterTipe.value) params.set('type', filterTipe.value)
  if (cari.value.trim()) params.set('q', cari.value.trim())
  return api.getPage<Item>(`/companies/${companyId.value}/items?${params}`, auth.token)
})

const opsiCabang = computed(() =>
  cabang.value.map((b) => ({ value: b.id, label: `${b.name} (${b.code})` })),
)

const opsiTipe = [
  { value: 'barang', label: 'Barang' },
  { value: 'jasa', label: 'Jasa' },
]

const hargaMap = computed<Record<number, string>>(() => {
  const map: Record<number, string> = {}
  for (const item of items.value) {
    const price = item.prices.find((row) => row.branchId === branchId.value)
    if (price) map[item.id] = price.price
  }
  return map
})

const sudahAdaHarga = computed(() => items.value.filter((item) => item.id in hargaMap.value).length)
const belumAdaHarga = computed(() => items.value.length - sudahAdaHarga.value)
const error = computed(() => referensi.error.value || errorDaftar.value)

async function muatCabang() {
  const data = await referensi.run(() =>
    api.get<Cabang[]>(`/companies/${companyId.value}/branches`, auth.token),
  )
  cabang.value = data ?? []
  if (!branchId.value && cabang.value.length) {
    branchId.value =
      auth.branchId && cabang.value.some((b) => b.id === auth.branchId)
        ? auth.branchId
        : cabang.value[0]!.id
  }
}

function gantiCabang(value: string | number | null) {
  if (value === null) return
  branchId.value = Number(value)
  hargaEdit.value = {}
}

function cariUlang() {
  hargaEdit.value = {}
  void loadItems(1)
}

function gantiTipe() {
  hargaEdit.value = {}
  void loadItems(1)
}

async function simpanHarga(item: Item) {
  if (!branchId.value) return
  const nilai = hargaEdit.value[item.id]
  if (nilai === undefined || nilai === null || Number.isNaN(Number(nilai)) || Number(nilai) < 0) {
    toast.warning('Isi harga baru terlebih dahulu.')
    return
  }

  menyimpanId.value = item.id
  await simpanAsync.run(() =>
    api.put(
      `/companies/${companyId.value}/items/${item.id}/prices/${branchId.value}`,
      { price: Number(nilai) },
      auth.token,
    ),
  )
  menyimpanId.value = null

  if (simpanAsync.error.value) {
    toast.error(simpanAsync.error.value)
    return
  }

  const lain = item.prices.filter((row) => row.branchId !== branchId.value)
  item.prices = [...lain, { branchId: branchId.value, price: String(nilai) }]
  const sisa = { ...hargaEdit.value }
  delete sisa[item.id]
  hargaEdit.value = sisa
  toast.success(`Harga ${item.name} tersimpan.`)
}

onMounted(async () => {
  await access.load(companyId.value, auth.token)
  await muatCabang()
  await loadItems(1)
})
</script>

<template>
  <div>
    <AppPageHeader
      eyebrow="Katalog"
      title="Harga per cabang"
      subtitle="Atur harga jual tiap item untuk cabang yang dipilih. Perubahan harga tercatat di audit."
      icon="sell"
    >
      <template #actions>
        <AppButton variant="ghost" :loading="loading" @click="loadItems()">Muat ulang</AppButton>
      </template>
    </AppPageHeader>

    <AppBanner tone="red" :message="error" />

    <div class="mb-3 grid grid-cols-2 gap-3 lg:grid-cols-4">
      <AppCard>
        <p class="text-xs uppercase tracking-wider text-secondary">Cabang</p>
        <p class="font-display text-2xl font-bold">{{ cabang.length }}</p>
      </AppCard>
      <AppCard>
        <p class="text-xs uppercase tracking-wider text-secondary">Item total</p>
        <p class="font-display text-2xl font-bold">{{ meta?.total ?? 0 }}</p>
      </AppCard>
      <AppCard>
        <p class="text-xs uppercase tracking-wider text-secondary">Sudah ada harga</p>
        <p class="font-display text-2xl font-bold">{{ sudahAdaHarga }}</p>
        <p class="mt-0.5 text-[11px] text-secondary">di halaman ini</p>
      </AppCard>
      <AppCard>
        <p class="text-xs uppercase tracking-wider text-secondary">Belum ada harga</p>
        <p class="font-display text-2xl font-bold" :class="belumAdaHarga ? 'text-warn' : ''">
          {{ belumAdaHarga }}
        </p>
        <p class="mt-0.5 text-[11px] text-secondary">di halaman ini</p>
      </AppCard>
    </div>

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
          placeholder="Cari nama atau kode item..."
          @search="cariUlang"
        />
        <AppSelect
          v-model="filterTipe"
          class="w-36"
          placeholder="Semua tipe"
          :options="opsiTipe"
          @update:model-value="gantiTipe"
        />
      </div>

      <p
        v-if="!bolehUbah"
        class="mb-3 rounded-lg bg-surface-low px-3 py-2 text-sm text-secondary"
      >
        Anda hanya bisa melihat harga. Perubahan harga hanya untuk yang diberi wewenang mengatur
        harga.
      </p>

      <AppTable
        :loading="loading"
        :empty="!items.length"
        :colspan="bolehUbah ? 4 : 3"
        empty-title="Tidak ada item"
        empty-message="Ubah pencarian atau tambahkan item baru di Katalog."
      >
        <template #head>
          <tr>
            <th>Item</th>
            <th>Jenis</th>
            <th class="text-right">Harga berlaku</th>
            <th v-if="bolehUbah" class="text-right">Harga baru</th>
          </tr>
        </template>
        <tr v-for="it in items" :key="it.id">
          <td>
            <p class="font-medium text-ink">{{ it.name }}</p>
            <p class="font-mono text-xs text-secondary">
              {{ it.code }}<span v-if="it.unit"> · {{ it.unit }}</span>
            </p>
          </td>
          <td><AppBadge :tone="it.type === 'jasa' ? 'blue' : 'gray'">{{ it.type }}</AppBadge></td>
          <td class="text-right">
            <span v-if="it.id in hargaMap" class="font-semibold tabular-nums">
              {{ rupiah(hargaMap[it.id]) }}
            </span>
            <AppBadge v-else tone="amber">Belum diatur</AppBadge>
          </td>
          <td v-if="bolehUbah" class="text-right">
            <div class="flex items-center justify-end gap-2">
              <AppInput
                v-model="hargaEdit[it.id]"
                type="number"
                min="0"
                class="w-32"
                :placeholder="hargaMap[it.id] ?? 'cth. 50000'"
              />
              <AppButton
                size="sm"
                :loading="menyimpanId === it.id"
                :disabled="hargaEdit[it.id] === undefined || hargaEdit[it.id] === null"
                @click="simpanHarga(it)"
              >
                Simpan
              </AppButton>
            </div>
          </td>
        </tr>
      </AppTable>
      <AppPager :meta="meta" @page="loadItems" @update:per-page="setPerPage" />

      <p class="mt-4 text-xs text-secondary">
        Harga yang tampil adalah harga berlaku saat ini. Riwayat perubahan harga dapat dilihat di
        audit.
      </p>
    </AppCard>
  </div>
</template>
