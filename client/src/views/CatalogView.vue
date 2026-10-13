<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { api, type PageMeta } from '../lib/api'
import { rupiah } from '../lib/format'
import { useCompany } from '../composables/useCompany'
import { useAccessStore } from '../stores/access'
import { useAsync } from '../composables/useAsync'
import AppCard from '../components/ui/AppCard.vue'
import AppBadge from '../components/ui/AppBadge.vue'
import AppBanner from '../components/ui/AppBanner.vue'
import AppButton from '../components/ui/AppButton.vue'
import AppField from '../components/ui/AppField.vue'
import AppEmpty from '../components/ui/AppEmpty.vue'
import AppPager from '../components/ui/AppPager.vue'
import AppIcon from '../components/ui/AppIcon.vue'

interface Kategori {
  id: number
  name: string
}
interface Item {
  id: number
  code: string
  name: string
  type: string
  unit: string | null
  categoryId: number | null
  trackStock: boolean
  status: string
  commissionType?: string | null
  commissionValue?: string | null
}
interface Supplier {
  id: number
  name: string
  phone: string | null
  address: string | null
}
interface Cabang {
  id: number
  code: string
  name: string
}

const { auth, companyId } = useCompany()
const access = useAccessStore()
const { loading, error, run } = useAsync()
const tab = ref<'kategori' | 'item' | 'harga' | 'supplier'>('kategori')
const kategori = ref<Kategori[]>([])
const items = ref<Item[]>([])
const metaItem = ref<PageMeta | null>(null)
const suppliers = ref<Supplier[]>([])
const cabang = ref<Cabang[]>([])
const filterTipe = ref('')
const okMsg = ref('')

const showKat = ref(false)
const namaKat = ref('')
const idKat = ref<number | null>(null)
const showItem = ref(false)
const suntingItem = ref<Item | null>(null)
const fItem = ref({
  code: '',
  name: '',
  type: 'barang',
  unit: 'pcs',
  category_id: '',
  track_stock: true,
  commission_type: '',
  commission_value: 0,
})
const tanyaHapus = ref<{ jenis: string; id: number; nama: string } | null>(null)
const showSup = ref(false)
const suntingSup = ref<Supplier | null>(null)
const fSup = ref({ name: '', phone: '', address: '' })
const hargaCabang = ref('')
const hargaMap = ref<Record<number, string>>({})
const hargaBaru = ref<Record<number, number>>({})

const bolehKatalog = () => access.can('katalog.kelola')
const bolehHarga = () => access.can('harga.atur')

async function muatKategori() {
  kategori.value =
    (await run(() =>
      api.get<Kategori[]>(`/companies/${companyId.value}/categories?per_page=100`, auth.token),
    )) ?? []
}

async function muatItem(page = 1) {
  const res = await run(() =>
    api.get<{ data: Item[]; meta: PageMeta }>(
      `/companies/${companyId.value}/items?per_page=15&page=${page}${filterTipe.value ? `&type=${filterTipe.value}` : ''}`,
      auth.token,
    ),
  )
  if (res) {
    const raw = res as unknown as { data: Item[]; meta?: PageMeta } | Item[]
    if (Array.isArray(raw)) items.value = raw
    else {
      items.value = raw.data
      metaItem.value = raw.meta ?? null
    }
  }
}

async function muatSupplier() {
  suppliers.value =
    (await run(() =>
      api.get<Supplier[]>(`/companies/${companyId.value}/suppliers?per_page=100`, auth.token),
    )) ?? []
}

async function muatHarga() {
  if (!hargaCabang.value) return
  const map: Record<number, string> = {}
  for (const it of items.value.length ? items.value : []) {
    try {
      const h = await api.get<{ price: string }>(
        `/companies/${companyId.value}/items/${it.id}/prices/${hargaCabang.value}`,
        auth.token,
      )
      map[it.id] = h.price
    } catch {
      /* belum ada harga */
    }
  }
  if (!items.value.length) {
    const semua =
      (await run(() =>
        api.get<Item[]>(`/companies/${companyId.value}/items?per_page=100`, auth.token),
      )) ?? []
    for (const it of semua) {
      try {
        const h = await api.get<{ price: string }>(
          `/companies/${companyId.value}/items/${it.id}/prices/${hargaCabang.value}`,
          auth.token,
        )
        map[it.id] = h.price
      } catch {
        /* belum ada harga */
      }
    }
    items.value = semua
  }
  hargaMap.value = map
}

async function simpanKat() {
  const base = `/companies/${companyId.value}/categories`
  const res = idKat.value
    ? await run(() => api.put(base + `/${idKat.value}`, { name: namaKat.value.trim() }, auth.token))
    : await run(() => api.post(base, { name: namaKat.value.trim() }, auth.token))
  if (res) {
    showKat.value = false
    okMsg.value = 'Kategori tersimpan.'
    muatKategori()
  }
}

function bukaItemBaru() {
  suntingItem.value = null
  fItem.value = {
    code: '',
    name: '',
    type: 'barang',
    unit: 'pcs',
    category_id: '',
    track_stock: true,
    commission_type: '',
    commission_value: 0,
  }
  showItem.value = true
}

function bukaItemUbah(it: Item) {
  suntingItem.value = it
  fItem.value = {
    code: it.code,
    name: it.name,
    type: it.type,
    unit: it.unit ?? '',
    category_id: it.categoryId?.toString() ?? '',
    track_stock: it.trackStock,
    commission_type: it.commissionType ?? '',
    commission_value: Number(it.commissionValue ?? 0),
  }
  showItem.value = true
}

async function simpanItem() {
  const body: Record<string, unknown> = {
    code: fItem.value.code.trim(),
    name: fItem.value.name.trim(),
    type: fItem.value.type,
    track_stock: fItem.value.track_stock,
  }
  if (fItem.value.type === 'barang') body.unit = fItem.value.unit || undefined
  if (fItem.value.category_id) body.category_id = Number(fItem.value.category_id)
  if (fItem.value.commission_type) {
    body.commission_type = fItem.value.commission_type
    body.commission_value = fItem.value.commission_value
  }
  const base = `/companies/${companyId.value}/items`
  const res = suntingItem.value
    ? await run(() => api.put(base + `/${suntingItem.value!.id}`, body, auth.token))
    : await run(() => api.post(base, body, auth.token))
  if (res) {
    showItem.value = false
    okMsg.value = suntingItem.value ? 'Item diperbarui.' : 'Item baru ditambahkan.'
    muatItem(1)
  }
}

async function simpanHarga(itemId: number) {
  const nilai = hargaBaru.value[itemId]
  if (nilai === undefined || !hargaCabang.value) return
  const res = await run(() =>
    api.put(
      `/companies/${companyId.value}/items/${itemId}/prices/${hargaCabang.value}`,
      { price: nilai },
      auth.token,
    ),
  )
  if (res) {
    okMsg.value = 'Harga tersimpan.'
    muatHarga()
  }
}

function bukaSupBaru() {
  suntingSup.value = null
  fSup.value = { name: '', phone: '', address: '' }
  showSup.value = true
}

function bukaSupUbah(s: Supplier) {
  suntingSup.value = s
  fSup.value = { name: s.name, phone: s.phone ?? '', address: s.address ?? '' }
  showSup.value = true
}

async function simpanSup() {
  const base = `/companies/${companyId.value}/suppliers`
  const body = {
    name: fSup.value.name.trim(),
    phone: fSup.value.phone.trim() || undefined,
    address: fSup.value.address.trim() || undefined,
  }
  const res = suntingSup.value
    ? await run(() => api.put(base + `/${suntingSup.value!.id}`, body, auth.token))
    : await run(() => api.post(base, body, auth.token))
  if (res) {
    showSup.value = false
    okMsg.value = suntingSup.value ? 'Supplier diperbarui.' : 'Supplier baru ditambahkan.'
    muatSupplier()
  }
}

async function hapus() {
  if (!tanyaHapus.value) return
  const path =
    tanyaHapus.value.jenis === 'kategori'
      ? 'categories'
      : tanyaHapus.value.jenis === 'item'
        ? 'items'
        : 'suppliers'
  await run(() =>
    api.del(`/companies/${companyId.value}/${path}/${tanyaHapus.value!.id}`, auth.token),
  )
  if (!error.value) {
    tanyaHapus.value = null
    okMsg.value = 'Data dihapus.'
    muatKategori()
    muatItem(1)
    muatSupplier()
  }
}

onMounted(async () => {
  await access.load(companyId.value, auth.token)
  cabang.value =
    (await run(() => api.get<Cabang[]>(`/companies/${companyId.value}/branches`, auth.token))) ?? []
  hargaCabang.value = String(cabang.value[0]?.id ?? '')
  muatKategori()
  muatItem()
  muatSupplier()
})
</script>

<template>
  <div>
    <div class="mb-4 flex items-center gap-3">
      <div
        class="flex h-12 w-12 items-center justify-center rounded-xl bg-primary-container text-on-primary"
      >
        <AppIcon name="category" :size="28" />
      </div>
      <h1 class="font-display text-xl font-semibold">Katalog</h1>
    </div>
    <AppBanner tone="green" :message="okMsg" />
    <AppBanner tone="red" :message="error" />

    <div class="mb-3 flex gap-2">
      <AppButton
        v-for="t in [
          ['kategori', 'Kategori'],
          ['item', 'Item'],
          ['harga', 'Harga cabang'],
          ['supplier', 'Supplier'],
        ]"
        :key="t[0]"
        :variant="tab === t[0] ? 'primary' : 'ghost'"
        @click="tab = t[0] as typeof tab"
        >{{ t[1] }}</AppButton
      >
    </div>

    <div v-if="tab === 'kategori'">
      <AppButton
        v-if="bolehKatalog()"
        @click="
          idKat = null
          namaKat = ''
          showKat = true
        "
        ><AppIcon name="add" :size="18" />Kategori</AppButton
      >
      <div class="mt-3 flex flex-wrap gap-2">
        <AppCard v-for="k in kategori" :key="k.id">
          <div class="flex items-center gap-3">
            <p class="font-semibold">{{ k.name }}</p>
            <AppButton
              v-if="bolehKatalog()"
              variant="ghost"
              @click="
                idKat = k.id
                namaKat = k.name
                showKat = true
              "
              >Ubah</AppButton
            >
            <button
              v-if="bolehKatalog()"
              class="text-xs text-danger"
              @click="tanyaHapus = { jenis: 'kategori', id: k.id, nama: k.name }"
            >
              Hapus
            </button>
          </div>
        </AppCard>
      </div>
      <AppEmpty v-if="!kategori.length" message="Belum ada kategori." />
    </div>

    <div v-if="tab === 'item'">
      <div class="mb-3 flex flex-wrap gap-2">
        <select
          v-model="filterTipe"
          class="rounded-lg border border-outline bg-card px-3 py-2 text-sm"
          @change="muatItem(1)"
        >
          <option value="">Semua tipe</option>
          <option value="barang">Barang</option>
          <option value="jasa">Jasa</option>
        </select>
        <AppButton v-if="bolehKatalog()" @click="bukaItemBaru"
          ><AppIcon name="add" :size="18" />Item</AppButton
        >
      </div>
      <AppEmpty v-if="!loading && !items.length" message="Belum ada item." />
      <div class="overflow-x-auto rounded-xl bg-card shadow-sm">
        <table class="w-full min-w-[640px] text-sm">
          <thead>
            <tr class="text-left text-xs uppercase text-secondary">
              <th class="px-4 py-2">Kode / Nama</th>
              <th class="px-4 py-2">Tipe</th>
              <th class="px-4 py-2">Satuan</th>
              <th class="px-4 py-2">Stok?</th>
              <th class="px-4 py-2">Komisi</th>
              <th class="px-4 py-2">Status</th>
              <th v-if="bolehKatalog()" class="px-4 py-2 text-right">Aksi</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="it in items" :key="it.id" class="border-t border-surface-low">
              <td class="px-4 py-2">
                <strong>{{ it.name }}</strong>
                <span class="font-mono text-xs text-secondary">{{ it.code }}</span>
              </td>
              <td class="px-4 py-2">
                <AppBadge :tone="it.type === 'jasa' ? 'blue' : 'gray'">{{ it.type }}</AppBadge>
              </td>
              <td class="px-4 py-2">{{ it.unit ?? '–' }}</td>
              <td class="px-4 py-2">{{ it.trackStock ? 'Ya' : 'Tidak' }}</td>
              <td class="px-4 py-2 text-xs">
                {{ it.commissionType ? `${it.commissionType} ${it.commissionValue}` : '–' }}
              </td>
              <td class="px-4 py-2">{{ it.status }}</td>
              <td v-if="bolehKatalog()" class="px-4 py-2 text-right">
                <AppButton variant="ghost" @click="bukaItemUbah(it)">Ubah</AppButton>
                <button
                  class="ml-1 text-xs text-danger"
                  @click="tanyaHapus = { jenis: 'item', id: it.id, nama: it.name }"
                >
                  Hapus
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <AppPager :meta="metaItem" @page="muatItem" />
    </div>

    <div v-if="tab === 'harga'">
      <div class="mb-3 flex items-center gap-2">
        <span class="text-sm text-secondary">Cabang:</span>
        <select
          v-model="hargaCabang"
          class="rounded-lg border border-outline bg-card px-3 py-2 text-sm"
          @change="muatHarga"
        >
          <option v-for="b in cabang" :key="b.id" :value="b.id">{{ b.code }} — {{ b.name }}</option>
        </select>
        <AppButton variant="ghost" @click="muatHarga">Muat</AppButton>
      </div>
      <p v-if="!bolehHarga()" class="text-sm text-secondary">
        Hanya pemilik izin harga yang bisa mengubah.
      </p>
      <div class="overflow-x-auto rounded-xl bg-card shadow-sm">
        <table class="w-full min-w-[480px] text-sm">
          <thead>
            <tr class="text-left text-xs uppercase text-secondary">
              <th class="px-4 py-2">Item</th>
              <th class="px-4 py-2 text-right">Harga saat ini</th>
              <th class="px-4 py-2 text-right">Harga baru</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="it in items" :key="it.id" class="border-t border-surface-low">
              <td class="px-4 py-2">
                <strong>{{ it.name }}</strong>
                <span class="font-mono text-xs text-secondary">{{ it.code }}</span>
              </td>
              <td class="px-4 py-2 text-right font-mono">
                {{ hargaMap[it.id] ? rupiah(hargaMap[it.id]) : '–' }}
              </td>
              <td class="px-4 py-2">
                <span v-if="bolehHarga()" class="flex justify-end gap-1">
                  <input
                    v-model.number="hargaBaru[it.id]"
                    type="number"
                    min="0"
                    class="w-32 rounded border border-outline bg-card px-2 py-1 text-right"
                  />
                  <AppButton variant="ghost" @click="simpanHarga(it.id)">Simpan</AppButton>
                </span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <div v-if="tab === 'supplier'">
      <AppButton v-if="bolehKatalog()" @click="bukaSupBaru"
        ><AppIcon name="add" :size="18" />Supplier</AppButton
      >
      <div class="mt-3 grid gap-3 md:grid-cols-2">
        <AppCard v-for="s in suppliers" :key="s.id">
          <div class="flex items-start justify-between gap-2">
            <div>
              <p class="font-semibold">{{ s.name }}</p>
              <p class="font-mono text-xs text-secondary">{{ s.phone ?? '' }}</p>
              <p class="text-sm text-secondary">{{ s.address ?? '' }}</p>
            </div>
            <div v-if="bolehKatalog()" class="flex gap-1">
              <AppButton variant="ghost" @click="bukaSupUbah(s)">Ubah</AppButton>
              <button
                class="text-xs text-danger"
                @click="tanyaHapus = { jenis: 'supplier', id: s.id, nama: s.name }"
              >
                Hapus
              </button>
            </div>
          </div>
        </AppCard>
      </div>
      <AppEmpty v-if="!suppliers.length" message="Belum ada supplier." />
    </div>

    <div
      v-if="showKat"
      class="fixed inset-0 z-20 flex items-center justify-center bg-black/40 p-4"
      @click.self="showKat = false"
    >
      <form class="w-full max-w-sm rounded-xl bg-card p-5 shadow-xl" @submit.prevent="simpanKat">
        <h3 class="font-display font-semibold">{{ idKat ? 'Ubah kategori' : 'Kategori baru' }}</h3>
        <div class="mt-3">
          <AppField label="Nama (wajib)">
            <input
              v-model="namaKat"
              maxlength="255"
              class="w-full rounded-lg bg-surface-low px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-primary"
            />
          </AppField>
        </div>
        <div class="mt-4 flex justify-end gap-2">
          <AppButton variant="ghost" @click="showKat = false">Batal</AppButton>
          <AppButton type="submit" :loading="loading">Simpan</AppButton>
        </div>
      </form>
    </div>

    <div
      v-if="showItem"
      class="fixed inset-0 z-20 flex items-center justify-center bg-black/40 p-4"
      @click.self="showItem = false"
    >
      <form
        class="max-h-[90vh] w-full max-w-md overflow-y-auto rounded-xl bg-card p-5 shadow-xl"
        @submit.prevent="simpanItem"
      >
        <h3 class="font-display font-semibold">{{ suntingItem ? 'Ubah item' : 'Item baru' }}</h3>
        <div class="mt-3 grid grid-cols-2 gap-2">
          <AppField label="Kode (wajib, unik)">
            <input
              v-model="fItem.code"
              class="w-full rounded-lg bg-surface-low px-3 py-2 font-mono text-sm outline-none focus:ring-2 focus:ring-primary"
            />
          </AppField>
          <AppField label="Tipe">
            <select
              v-model="fItem.type"
              class="w-full rounded-lg bg-surface-low px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-primary"
            >
              <option value="barang">Barang</option>
              <option value="jasa">Jasa</option>
            </select>
          </AppField>
        </div>
        <div class="mt-3">
          <AppField label="Nama (wajib)">
            <input
              v-model="fItem.name"
              maxlength="255"
              class="w-full rounded-lg bg-surface-low px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-primary"
            />
          </AppField>
        </div>
        <div class="mt-3 grid grid-cols-2 gap-2">
          <AppField label="Satuan (wajib bila barang)">
            <input
              v-model="fItem.unit"
              maxlength="20"
              class="w-full rounded-lg bg-surface-low px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-primary"
            />
          </AppField>
          <AppField label="Kategori">
            <select
              v-model="fItem.category_id"
              class="w-full rounded-lg bg-surface-low px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-primary"
            >
              <option value="">Tanpa kategori</option>
              <option v-for="k in kategori" :key="k.id" :value="k.id">{{ k.name }}</option>
            </select>
          </AppField>
        </div>
        <label class="mt-3 flex cursor-pointer items-center gap-2 text-sm">
          <input v-model="fItem.track_stock" type="checkbox" class="h-4 w-4 accent-primary" /> Lacak
          stok
        </label>
        <div class="mt-3 grid grid-cols-2 gap-2">
          <AppField label="Komisi">
            <select
              v-model="fItem.commission_type"
              class="w-full rounded-lg bg-surface-low px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-primary"
            >
              <option value="">Tanpa komisi</option>
              <option value="persen">Persen (%)</option>
              <option value="nominal">Nominal (Rp)</option>
            </select>
          </AppField>
          <AppField label="Nilai komisi">
            <input
              v-model.number="fItem.commission_value"
              type="number"
              min="0"
              class="w-full rounded-lg bg-surface-low px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-primary"
            />
          </AppField>
        </div>
        <div class="mt-4 flex justify-end gap-2">
          <AppButton variant="ghost" @click="showItem = false">Batal</AppButton>
          <AppButton type="submit" :loading="loading">Simpan</AppButton>
        </div>
      </form>
    </div>

    <div
      v-if="showSup"
      class="fixed inset-0 z-20 flex items-center justify-center bg-black/40 p-4"
      @click.self="showSup = false"
    >
      <form class="w-full max-w-sm rounded-xl bg-card p-5 shadow-xl" @submit.prevent="simpanSup">
        <h3 class="font-display font-semibold">
          {{ suntingSup ? 'Ubah supplier' : 'Supplier baru' }}
        </h3>
        <div class="mt-3">
          <AppField label="Nama (wajib)">
            <input
              v-model="fSup.name"
              maxlength="255"
              class="w-full rounded-lg bg-surface-low px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-primary"
            />
          </AppField>
        </div>
        <div class="mt-3">
          <AppField label="Telepon (unik)">
            <input
              v-model="fSup.phone"
              class="w-full rounded-lg bg-surface-low px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-primary"
            />
          </AppField>
        </div>
        <div class="mt-3">
          <AppField label="Alamat">
            <textarea
              v-model="fSup.address"
              rows="2"
              maxlength="500"
              class="w-full rounded-lg bg-surface-low px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-primary"
            />
          </AppField>
        </div>
        <div class="mt-4 flex justify-end gap-2">
          <AppButton variant="ghost" @click="showSup = false">Batal</AppButton>
          <AppButton type="submit" :loading="loading">Simpan</AppButton>
        </div>
      </form>
    </div>

    <div
      v-if="tanyaHapus"
      class="fixed inset-0 z-20 flex items-center justify-center bg-black/40 p-4"
      @click.self="tanyaHapus = null"
    >
      <div class="w-full max-w-sm rounded-xl bg-card p-5 shadow-xl">
        <h3 class="font-display font-semibold">Hapus {{ tanyaHapus.nama }}?</h3>
        <p class="mt-1 text-sm text-secondary">
          Data yang masih dipakai transaksi tidak bisa dihapus.
        </p>
        <div class="mt-4 flex justify-end gap-2">
          <AppButton variant="ghost" @click="tanyaHapus = null">Batal</AppButton>
          <AppButton variant="danger" :loading="loading" @click="hapus">Ya, hapus</AppButton>
        </div>
      </div>
    </div>
  </div>
</template>
