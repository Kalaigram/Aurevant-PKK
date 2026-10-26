<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { api, assetUrl } from '../lib/api'
import { rupiah } from '../lib/format'
import { useCompany } from '../composables/useCompany'
import { useAccessStore } from '../stores/access'
import { useAsync } from '../composables/useAsync'
import { usePagedList } from '../composables/usePagedList'
import { useConfirm } from '../composables/useConfirm'
import { useToast } from '../composables/useToast'
import AppAvatar from '../components/ui/AppAvatar.vue'
import AppBadge from '../components/ui/AppBadge.vue'
import AppBanner from '../components/ui/AppBanner.vue'
import AppButton from '../components/ui/AppButton.vue'
import AppCard from '../components/ui/AppCard.vue'
import AppCheckbox from '../components/ui/AppCheckbox.vue'
import AppField from '../components/ui/AppField.vue'
import AppIcon from '../components/ui/AppIcon.vue'
import AppInput from '../components/ui/AppInput.vue'
import AppModal from '../components/ui/AppModal.vue'
import AppMoneyInput from '../components/ui/AppMoneyInput.vue'
import AppPageHeader from '../components/ui/AppPageHeader.vue'
import AppPager from '../components/ui/AppPager.vue'
import AppSearch from '../components/ui/AppSearch.vue'
import AppSelect from '../components/ui/AppSelect.vue'
import AppTable from '../components/ui/AppTable.vue'
import AppTabs from '../components/ui/AppTabs.vue'

interface Kategori {
  id: number
  name: string
  itemCount: number
}

interface ItemPrice {
  branchId: number
  price: string
}

interface Item {
  id: number
  categoryId: number | null
  categoryName: string | null
  code: string
  name: string
  type: string
  unit: string | null
  imagePath: string | null
  imageUrl: string | null
  trackStock: boolean
  durationMinutes: number | null
  commissionType: string | null
  commissionValue: string | null
  status: string
  prices: ItemPrice[]
}

interface Supplier {
  id: number
  name: string
  phone: string | null
  address: string | null
}

interface ItemForm {
  code: string
  name: string
  type: string
  unit: string
  categoryId: string
  trackStock: boolean
  durationMinutes: number | null
  commissionType: string
  commissionValue: number | null
  status: string
}

const router = useRouter()
const { auth, companyId } = useCompany()

function bukaHarga() {
  router.push(`/c/${companyId.value}/prices`)
}
const access = useAccessStore()
const toast = useToast()
const { confirm } = useConfirm()

const kategoriAsync = useAsync()
const mutasi = useAsync()

const tab = ref('kategori')
const kategori = ref<Kategori[]>([])
const cariItem = ref('')
const filterTipe = ref('')
const filterKategori = ref('')

const {
  rows: items,
  meta: metaItem,
  loading: loadingItem,
  error: errorItem,
  load: loadItems,
  setPerPage: setPerPageItem,
} = usePagedList<Item>((page, perPage) => {
  const params = new URLSearchParams({ per_page: String(perPage), page: String(page) })
  if (filterTipe.value) params.set('type', filterTipe.value)
  if (filterKategori.value) params.set('category_id', filterKategori.value)
  if (cariItem.value.trim()) params.set('q', cariItem.value.trim())
  return api.getPage<Item>(`/companies/${companyId.value}/items?${params}`, auth.token)
})

const cariSupplier = ref('')
const {
  rows: suppliers,
  meta: metaSupplier,
  loading: loadingSupplier,
  error: errorSupplier,
  load: loadSuppliers,
  setPerPage: setPerPageSupplier,
} = usePagedList<Supplier>((page, perPage) => {
  const params = new URLSearchParams({ per_page: String(perPage), page: String(page) })
  if (cariSupplier.value.trim()) params.set('q', cariSupplier.value.trim())
  return api.getPage<Supplier>(`/companies/${companyId.value}/suppliers?${params}`, auth.token)
})

const kategoriOpen = ref(false)
const kategoriEdit = ref<Kategori | null>(null)
const kategoriNama = ref('')

const itemOpen = ref(false)
const itemEdit = ref<Item | null>(null)
const itemForm = ref<ItemForm>(formItemKosong())
const berkasItem = ref<HTMLInputElement | null>(null)
const itemGambar = ref<File | null>(null)
const itemGambarHapus = ref(false)
const itemGambarPratinjau = ref('')

const supplierOpen = ref(false)
const supplierEdit = ref<Supplier | null>(null)
const supplierForm = ref({ name: '', phone: '', address: '' })

const bolehKatalog = computed(() => access.can('katalog.kelola'))
const error = computed(
  () =>
    errorItem.value ||
    errorSupplier.value ||
    kategoriAsync.error.value ||
    (itemOpen.value || supplierOpen.value || kategoriOpen.value ? '' : mutasi.error.value),
)

const tabs = computed(() => [
  { key: 'kategori', label: 'Kategori', count: kategori.value.length },
  { key: 'item', label: 'Item', count: metaItem.value?.total ?? 0 },
  { key: 'supplier', label: 'Supplier', count: metaSupplier.value?.total ?? 0 },
])

const opsiKategori = computed(() =>
  kategori.value.map((k) => ({ value: String(k.id), label: k.name })),
)

const gambarItem = computed(() => {
  if (itemGambarPratinjau.value) return itemGambarPratinjau.value
  if (!itemGambarHapus.value) return assetUrl(itemEdit.value?.imageUrl)
  return ''
})

const opsiTipe = [
  { value: 'barang', label: 'Barang' },
  { value: 'jasa', label: 'Jasa' },
]

function formItemKosong(): ItemForm {
  return {
    code: '',
    name: '',
    type: 'barang',
    unit: 'pcs',
    categoryId: '',
    trackStock: true,
    durationMinutes: null,
    commissionType: '',
    commissionValue: null,
    status: 'aktif',
  }
}

function bersihkanPratinjauGambar() {
  if (itemGambarPratinjau.value.startsWith('blob:')) {
    URL.revokeObjectURL(itemGambarPratinjau.value)
  }
  itemGambarPratinjau.value = ''
}

function tutupItem() {
  itemOpen.value = false
  bersihkanPratinjauGambar()
  itemGambar.value = null
  itemGambarHapus.value = false
  if (berkasItem.value) berkasItem.value.value = ''
}

function pilihGambar(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return
  if (!['image/jpeg', 'image/png', 'image/webp'].includes(file.type)) {
    toast.error('Pilih gambar JPG, PNG, atau WEBP.')
    return
  }
  if (file.size > 2 * 1024 * 1024) {
    toast.error('Ukuran gambar maksimal 2 MB.')
    return
  }

  bersihkanPratinjauGambar()
  itemGambar.value = file
  itemGambarHapus.value = false
  itemGambarPratinjau.value = URL.createObjectURL(file)
}

function hapusGambar() {
  if (itemGambar.value) {
    itemGambar.value = null
    bersihkanPratinjauGambar()
    if (berkasItem.value) berkasItem.value.value = ''
    return
  }

  if (itemGambarHapus.value) {
    itemGambarHapus.value = false
    return
  }

  if (itemEdit.value?.imageUrl) {
    itemGambarHapus.value = true
    bersihkanPratinjauGambar()
  }
}

async function muatKategori() {
  const data = await kategoriAsync.run(() =>
    api.get<Kategori[]>(`/companies/${companyId.value}/categories?per_page=100`, auth.token),
  )
  kategori.value = data ?? []
}

async function jalankan(fn: () => Promise<unknown>, pesan: string): Promise<boolean> {
  await mutasi.run(fn)
  if (mutasi.error.value) {
    toast.error(mutasi.error.value)
    return false
  }
  toast.success(pesan)
  return true
}

function bukaKategori(k?: Kategori) {
  mutasi.error.value = ''
  kategoriEdit.value = k ?? null
  kategoriNama.value = k?.name ?? ''
  kategoriOpen.value = true
}

async function simpanKategori() {
  const nama = kategoriNama.value.trim()
  if (!nama) return
  const base = `/companies/${companyId.value}/categories`
  const sukses = await jalankan(
    () =>
      kategoriEdit.value
        ? api.put(base + `/${kategoriEdit.value.id}`, { name: nama }, auth.token)
        : api.post(base, { name: nama }, auth.token),
    kategoriEdit.value ? 'Kategori diperbarui.' : 'Kategori ditambahkan.',
  )
  if (sukses) {
    kategoriOpen.value = false
    await Promise.all([muatKategori(), loadItems()])
  }
}

async function hapusKategori(k: Kategori) {
  const setuju = await confirm({
    title: 'Hapus kategori?',
    message: `Kategori ${k.name} akan dihapus. Item di dalamnya tetap ada tanpa kategori.`,
    confirmLabel: 'Hapus',
    tone: 'danger',
  })
  if (!setuju) return
  const sukses = await jalankan(
    () => api.del(`/companies/${companyId.value}/categories/${k.id}`, auth.token),
    'Kategori dihapus.',
  )
  if (sukses) {
    if (filterKategori.value === String(k.id)) filterKategori.value = ''
    await Promise.all([muatKategori(), loadItems()])
  }
}

function bukaItem(it?: Item) {
  mutasi.error.value = ''
  bersihkanPratinjauGambar()
  itemGambar.value = null
  itemGambarHapus.value = false
  if (berkasItem.value) berkasItem.value.value = ''
  itemEdit.value = it ?? null
  itemForm.value = it
    ? {
        code: it.code,
        name: it.name,
        type: it.type,
        unit: it.unit ?? '',
        categoryId: it.categoryId ? String(it.categoryId) : '',
        trackStock: it.trackStock,
        durationMinutes: it.durationMinutes,
        commissionType: it.commissionType ?? '',
        commissionValue: it.commissionValue !== null ? Number(it.commissionValue) : null,
        status: it.status,
      }
    : formItemKosong()
  itemOpen.value = true
}

async function simpanItem() {
  const f = itemForm.value
  if (!f.code.trim() || !f.name.trim()) return
  if (f.type === 'barang' && !f.unit.trim()) return

  const body: Record<string, unknown> = {
    code: f.code.trim(),
    name: f.name.trim(),
    type: f.type,
    unit: f.type === 'barang' ? f.unit.trim() : null,
    category_id: f.categoryId ? Number(f.categoryId) : null,
    track_stock: f.trackStock,
    duration_minutes: f.type === 'jasa' ? f.durationMinutes : null,
    commission_type: f.commissionType || null,
    commission_value: f.commissionType ? Number(f.commissionValue ?? 0) : null,
    status: f.status,
  }

  const base = `/companies/${companyId.value}/items`
  const sukses = await jalankan(
    async () => {
      const item = itemEdit.value
        ? await api.put<Item>(base + `/${itemEdit.value.id}`, body, auth.token)
        : await api.post<Item>(base, body, auth.token)

      if (itemGambar.value) {
        const form = new FormData()
        form.append('image', itemGambar.value)
        await api.postForm<Item>(`${base}/${item.id}/image`, form, auth.token)
      } else if (itemGambarHapus.value && itemEdit.value) {
        await api.del<Item>(`${base}/${item.id}/image`, auth.token)
      }

      return item
    },
    itemEdit.value ? 'Item diperbarui.' : 'Item ditambahkan.',
  )
  if (sukses) {
    tutupItem()
    await Promise.all([loadItems(), muatKategori()])
  }
}

async function hapusItem(it: Item) {
  const setuju = await confirm({
    title: 'Hapus item?',
    message: `${it.name} (${it.code}) akan dihapus. Item yang sudah dipakai transaksi tidak bisa dihapus.`,
    confirmLabel: 'Hapus',
    tone: 'danger',
  })
  if (!setuju) return
  const sukses = await jalankan(
    () => api.del(`/companies/${companyId.value}/items/${it.id}`, auth.token),
    'Item dihapus.',
  )
  if (sukses) await Promise.all([loadItems(), muatKategori()])
}

function bukaSupplier(s?: Supplier) {
  mutasi.error.value = ''
  supplierEdit.value = s ?? null
  supplierForm.value = {
    name: s?.name ?? '',
    phone: s?.phone ?? '',
    address: s?.address ?? '',
  }
  supplierOpen.value = true
}

async function simpanSupplier() {
  const f = supplierForm.value
  if (!f.name.trim()) return
  const body = {
    name: f.name.trim(),
    phone: f.phone.trim() || null,
    address: f.address.trim() || null,
  }
  const base = `/companies/${companyId.value}/suppliers`
  const sukses = await jalankan(
    () =>
      supplierEdit.value
        ? api.put(base + `/${supplierEdit.value.id}`, body, auth.token)
        : api.post(base, body, auth.token),
    supplierEdit.value ? 'Supplier diperbarui.' : 'Supplier ditambahkan.',
  )
  if (sukses) {
    supplierOpen.value = false
    await loadSuppliers()
  }
}

async function hapusSupplier(s: Supplier) {
  const setuju = await confirm({
    title: 'Hapus supplier?',
    message: `Supplier ${s.name} akan dihapus dari daftar.`,
    confirmLabel: 'Hapus',
    tone: 'danger',
  })
  if (!setuju) return
  const sukses = await jalankan(
    () => api.del(`/companies/${companyId.value}/suppliers/${s.id}`, auth.token),
    'Supplier dihapus.',
  )
  if (sukses) await loadSuppliers()
}

const labelTipe = (type: string) =>
  type === 'barang' ? 'Barang' : type === 'jasa' ? 'Jasa' : type
const labelStatusItem = (status: string) =>
  status === 'aktif' ? 'Aktif' : status === 'nonaktif' ? 'Nonaktif' : status

function komisi(it: Item) {
  if (!it.commissionType) return '—'
  return it.commissionType === 'persen'
    ? `${Number(it.commissionValue ?? 0)}%`
    : rupiah(it.commissionValue)
}

onMounted(async () => {
  await access.load(companyId.value, auth.token)
  await Promise.all([muatKategori(), loadItems(1), loadSuppliers(1)])
})
</script>

<template>
  <div>
    <AppPageHeader
      eyebrow="Katalog"
      title="Katalog"
      subtitle="Kelola kategori, item barang/jasa, dan supplier dalam satu tempat."
      icon="category"
    >
      <template #actions>
        <AppButton variant="ghost" @click="bukaHarga">Harga cabang</AppButton>
      </template>
    </AppPageHeader>

    <AppBanner tone="red" :message="error" />

    <AppCard>
      <AppTabs v-model="tab" :items="tabs" label="Bagian katalog" />

      <div v-if="tab === 'kategori'" class="mt-3">
        <div class="mb-3 flex flex-wrap items-center justify-between gap-2">
          <p class="text-sm text-secondary">
            {{ kategori.length }} kategori aktif. Item tanpa kategori tetap bisa dijual.
          </p>
          <AppButton v-if="bolehKatalog" @click="bukaKategori()">Kategori baru</AppButton>
        </div>
        <AppTable
          :loading="kategoriAsync.loading.value"
          :empty="!kategori.length"
          :colspan="3"
          empty-title="Belum ada kategori"
          empty-message="Tambahkan kategori agar item lebih mudah dicari."
        >
          <template #head>
            <tr>
              <th>Nama kategori</th>
              <th class="text-right">Item</th>
              <th v-if="bolehKatalog" class="text-right">Aksi</th>
            </tr>
          </template>
          <tr v-for="k in kategori" :key="k.id">
            <td class="font-medium">{{ k.name }}</td>
            <td class="text-right tabular-nums">{{ k.itemCount }}</td>
            <td v-if="bolehKatalog" class="text-right">
              <div class="flex justify-end gap-2">
                <AppButton variant="ghost" size="sm" @click="bukaKategori(k)">Ubah</AppButton>
                <AppButton
                  variant="ghost"
                  size="sm"
                  class="text-danger"
                  :aria-label="`Hapus kategori ${k.name}`"
                  @click="hapusKategori(k)"
                >
                  Hapus
                </AppButton>
              </div>
            </td>
          </tr>
        </AppTable>
      </div>

      <div v-if="tab === 'item'" class="mt-3">
        <div class="mb-3 flex flex-wrap items-end gap-2">
          <AppSearch
            v-model="cariItem"
            class="min-w-56 flex-1"
            placeholder="Cari kode atau nama item..."
            @search="loadItems(1)"
          />
          <AppSelect
            v-model="filterTipe"
            class="w-36"
            placeholder="Semua tipe"
            :options="opsiTipe"
            @update:model-value="loadItems(1)"
          />
          <AppSelect
            v-model="filterKategori"
            class="w-44"
            placeholder="Semua kategori"
            :options="opsiKategori"
            @update:model-value="loadItems(1)"
          />
          <AppButton v-if="bolehKatalog" @click="bukaItem()">Item baru</AppButton>
        </div>

        <AppTable
          :loading="loadingItem"
          :empty="!items.length"
          :colspan="7"
          empty-title="Belum ada item"
          empty-message="Tambahkan barang atau jasa, atau ubah kata kunci pencarian."
        >
          <template #head>
            <tr>
              <th>Item</th>
              <th>Kategori</th>
              <th>Tipe</th>
              <th class="hidden md:table-cell">Satuan</th>
              <th class="hidden lg:table-cell">Komisi / durasi</th>
              <th>Status</th>
              <th v-if="bolehKatalog" class="text-right">Aksi</th>
            </tr>
          </template>
          <tr v-for="it in items" :key="it.id">
            <td>
              <div class="flex items-center gap-3">
                <a
                  v-if="it.imageUrl"
                  :href="assetUrl(it.imageUrl)"
                  target="_blank"
                  rel="noopener"
                  class="shrink-0 rounded-xl focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/55 focus-visible:ring-offset-2"
                  :aria-label="`Buka gambar ${it.name}`"
                  title="Buka gambar"
                >
                  <AppAvatar
                    :src="assetUrl(it.imageUrl)"
                    :alt="`Gambar ${it.name}`"
                    size="lg"
                    square
                    fallback-icon="category"
                  />
                </a>
                <AppAvatar
                  v-else
                  :name="it.name"
                  size="lg"
                  square
                  fallback-icon="category"
                />
                <div class="min-w-0">
                  <p class="font-medium text-ink">{{ it.name }}</p>
                  <p class="font-mono text-xs text-secondary">{{ it.code }}</p>
                </div>
              </div>
            </td>
            <td class="text-secondary">{{ it.categoryName ?? '—' }}</td>
            <td>
              <AppBadge :tone="it.type === 'jasa' ? 'blue' : 'gray'">
                {{ labelTipe(it.type) }}
              </AppBadge>
            </td>
            <td class="hidden text-secondary md:table-cell">
              {{ it.unit ?? '—' }}
              <span v-if="it.trackStock" class="ml-1 text-[11px] text-primary">stok</span>
            </td>
            <td class="hidden text-sm text-secondary lg:table-cell">
              <span v-if="komisi(it) !== '—'">{{ komisi(it) }}</span>
              <span v-if="it.durationMinutes" class="ml-1">· {{ it.durationMinutes }} mnt</span>
              <span v-if="komisi(it) === '—' && !it.durationMinutes">—</span>
            </td>
            <td>
              <AppBadge :tone="it.status === 'aktif' ? 'green' : 'gray'">
                {{ labelStatusItem(it.status) }}
              </AppBadge>
            </td>
            <td v-if="bolehKatalog" class="text-right">
              <div class="flex justify-end gap-2">
                <AppButton variant="ghost" size="sm" @click="bukaItem(it)">Ubah</AppButton>
                <AppButton
                  variant="ghost"
                  size="sm"
                  class="text-danger"
                  :aria-label="`Hapus item ${it.name}`"
                  @click="hapusItem(it)"
                >
                  Hapus
                </AppButton>
              </div>
            </td>
          </tr>
        </AppTable>
        <AppPager :meta="metaItem" @page="loadItems" @update:per-page="setPerPageItem" />
      </div>

      <div v-if="tab === 'supplier'" class="mt-3">
        <div class="mb-3 flex flex-wrap items-end gap-2">
          <AppSearch
            v-model="cariSupplier"
            class="min-w-56 flex-1"
            placeholder="Cari nama atau telepon supplier..."
            @search="loadSuppliers(1)"
          />
          <AppButton v-if="bolehKatalog" @click="bukaSupplier()">Supplier baru</AppButton>
        </div>

        <AppTable
          :loading="loadingSupplier"
          :empty="!suppliers.length"
          :colspan="4"
          empty-title="Belum ada supplier"
          empty-message="Tambahkan supplier untuk mempermudah pembelian."
        >
          <template #head>
            <tr>
              <th>Supplier</th>
              <th>Telepon</th>
              <th class="hidden md:table-cell">Alamat</th>
              <th v-if="bolehKatalog" class="text-right">Aksi</th>
            </tr>
          </template>
          <tr v-for="s in suppliers" :key="s.id">
            <td class="font-medium">{{ s.name }}</td>
            <td class="font-mono text-xs text-secondary">{{ s.phone ?? '—' }}</td>
            <td class="hidden max-w-md truncate text-secondary md:table-cell">
              {{ s.address ?? '—' }}
            </td>
            <td v-if="bolehKatalog" class="text-right">
              <div class="flex justify-end gap-2">
                <AppButton variant="ghost" size="sm" @click="bukaSupplier(s)">Ubah</AppButton>
                <AppButton
                  variant="ghost"
                  size="sm"
                  class="text-danger"
                  :aria-label="`Hapus supplier ${s.name}`"
                  @click="hapusSupplier(s)"
                >
                  Hapus
                </AppButton>
              </div>
            </td>
          </tr>
        </AppTable>
        <AppPager :meta="metaSupplier" @page="loadSuppliers" @update:per-page="setPerPageSupplier" />
      </div>
    </AppCard>

    <AppModal
      :open="kategoriOpen"
      :title="kategoriEdit ? 'Ubah kategori' : 'Kategori baru'"
      description="Kategori membantu merapikan daftar item."
      size="sm"
      @close="kategoriOpen = false"
    >
      <AppBanner tone="red" :message="mutasi.error.value" />
      <form id="kategori-form" @submit.prevent="simpanKategori">
        <AppField label="Nama kategori">
          <AppInput v-model="kategoriNama" maxlength="255" data-autofocus />
        </AppField>
      </form>
      <template #footer>
        <AppButton variant="ghost" @click="kategoriOpen = false">Batal</AppButton>
        <AppButton
          type="submit"
          form="kategori-form"
          :loading="mutasi.loading.value"
          :disabled="!kategoriNama.trim()"
        >
          Simpan
        </AppButton>
      </template>
    </AppModal>

    <AppModal
      :open="itemOpen"
      :title="itemEdit ? 'Ubah item' : 'Item baru'"
      description="Barang memakai stok dan satuan. Jasa bisa memakai durasi dan komisi."
      size="lg"
      @close="tutupItem"
    >
      <AppBanner tone="red" :message="mutasi.error.value" />
      <form id="item-form" class="grid gap-3 sm:grid-cols-2" @submit.prevent="simpanItem">
        <AppField label="Kode item">
          <AppInput v-model="itemForm.code" maxlength="50" class="font-mono" data-autofocus />
        </AppField>
        <AppField label="Tipe">
          <AppSelect v-model="itemForm.type">
            <option value="barang">Barang</option>
            <option value="jasa">Jasa</option>
          </AppSelect>
        </AppField>
        <AppField label="Nama item" class="sm:col-span-2">
          <AppInput v-model="itemForm.name" maxlength="255" />
        </AppField>
        <AppField
          label="Gambar item"
          optional
          class="sm:col-span-2"
          hint="JPG, PNG, atau WEBP. Maksimal 2 MB. Klik gambar di daftar untuk melihat ukuran penuh."
        >
          <div class="flex flex-col gap-3 rounded-xl border border-outline/60 p-3 sm:flex-row sm:items-center">
            <AppAvatar
              :src="gambarItem"
              :name="itemForm.name"
              :alt="itemForm.name ? `Gambar ${itemForm.name}` : 'Pratinjau gambar item'"
              size="xl"
              square
              fallback-icon="category"
              class="ring-outline"
            />
            <div class="min-w-0 flex-1">
              <p class="text-xs text-secondary">
                {{
                  itemEdit?.imageUrl
                    ? 'Gambar baru akan menggantikan gambar lama.'
                    : 'Tambahkan foto agar item mudah dikenali.'
                }}
              </p>
              <input
                ref="berkasItem"
                type="file"
                accept="image/jpeg,image/png,image/webp"
                class="hidden"
                @change="pilihGambar"
              />
              <div class="mt-3 flex flex-wrap gap-2">
                <AppButton size="sm" variant="secondary" @click="berkasItem?.click()">
                  <AppIcon name="upload" :size="16" />{{ gambarItem ? 'Ganti gambar' : 'Pilih gambar' }}
                </AppButton>
                <AppButton
                  v-if="gambarItem || itemGambarHapus"
                  size="sm"
                  variant="ghost"
                  @click="hapusGambar"
                >
                  <AppIcon name="delete" :size="16" />
                  {{
                    itemGambar
                      ? 'Batalkan pilihan'
                      : itemGambarHapus
                        ? 'Batalkan hapus'
                        : 'Hapus gambar'
                  }}
                </AppButton>
              </div>
            </div>
          </div>
        </AppField>
        <AppField label="Kategori">
          <AppSelect v-model="itemForm.categoryId" placeholder="Tanpa kategori" :options="opsiKategori" />
        </AppField>
        <AppField v-if="itemForm.type === 'barang'" label="Satuan">
          <AppInput v-model="itemForm.unit" maxlength="20" placeholder="pcs" />
        </AppField>
        <AppField v-else label="Durasi pengerjaan">
          <AppInput v-model="itemForm.durationMinutes" type="number" min="1" placeholder="menit">
            <template #suffix><span class="text-xs text-secondary">menit</span></template>
          </AppInput>
        </AppField>
        <AppField label="Komisi">
          <AppSelect v-model="itemForm.commissionType">
            <option value="">Tanpa komisi</option>
            <option value="persen">Persen (%)</option>
            <option value="nominal">Nominal (Rp)</option>
          </AppSelect>
        </AppField>
        <AppField v-if="itemForm.commissionType" label="Nilai komisi">
          <AppMoneyInput
            v-if="itemForm.commissionType === 'nominal'"
            v-model="itemForm.commissionValue"
          />
          <AppInput
            v-else
            v-model="itemForm.commissionValue"
            type="number"
            min="0"
            step="any"
          >
            <template #suffix><span class="text-xs text-secondary">%</span></template>
          </AppInput>
        </AppField>
        <AppField label="Status">
          <AppSelect v-model="itemForm.status">
            <option value="aktif">Aktif</option>
            <option value="nonaktif">Nonaktif</option>
          </AppSelect>
        </AppField>
        <div class="flex items-end pb-1">
          <AppCheckbox
            v-model="itemForm.trackStock"
            label="Lacak stok"
            hint="Matikan untuk barang yang tidak disimpan di gudang."
          />
        </div>
      </form>
      <template #footer>
        <AppButton variant="ghost" @click="tutupItem">Batal</AppButton>
        <AppButton
          type="submit"
          form="item-form"
          :loading="mutasi.loading.value"
          :disabled="
            !itemForm.code.trim() ||
            !itemForm.name.trim() ||
            (itemForm.type === 'barang' && !itemForm.unit.trim())
          "
        >
          Simpan
        </AppButton>
      </template>
    </AppModal>

    <AppModal
      :open="supplierOpen"
      :title="supplierEdit ? 'Ubah supplier' : 'Supplier baru'"
      description="Simpan kontak supplier untuk kebutuhan pembelian."
      @close="supplierOpen = false"
    >
      <AppBanner tone="red" :message="mutasi.error.value" />
      <form id="supplier-form" class="grid gap-3" @submit.prevent="simpanSupplier">
        <AppField label="Nama supplier">
          <AppInput v-model="supplierForm.name" maxlength="255" data-autofocus />
        </AppField>
        <AppField label="Telepon">
          <AppInput v-model="supplierForm.phone" maxlength="30" />
        </AppField>
        <AppField label="Alamat">
          <AppInput v-model="supplierForm.address" maxlength="500" />
        </AppField>
      </form>
      <template #footer>
        <AppButton variant="ghost" @click="supplierOpen = false">Batal</AppButton>
        <AppButton
          type="submit"
          form="supplier-form"
          :loading="mutasi.loading.value"
          :disabled="!supplierForm.name.trim()"
        >
          Simpan
        </AppButton>
      </template>
    </AppModal>
  </div>
</template>
