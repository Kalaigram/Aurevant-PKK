<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { api, type PageMeta } from '../lib/api'
import { rupiah } from '../lib/format'
import { useCompany } from '../composables/useCompany'
import { useAccessStore } from '../stores/access'
import { useAsync } from '../composables/useAsync'
import AppCard from '../components/ui/AppCard.vue'
import AppBanner from '../components/ui/AppBanner.vue'
import AppButton from '../components/ui/AppButton.vue'
import AppField from '../components/ui/AppField.vue'
import AppEmpty from '../components/ui/AppEmpty.vue'
import AppIcon from '../components/ui/AppIcon.vue'

interface Cabang {
  id: number
  code: string
  name: string
}
interface Item {
  id: number
  code: string
  name: string
  type: string
  unit: string | null
}

const { auth, companyId } = useCompany()
const access = useAccessStore()
const { loading, error, run } = useAsync()
const cabang = ref<Cabang[]>([])
const branchId = ref<number | null>(null)
const items = ref<Item[]>([])
const meta = ref<PageMeta | null>(null)
const cari = ref('')
const filterTipe = ref('')
const hargaMap = ref<Record<number, string>>({})
const hargaEdit = ref<Record<number, number>>({})
const okMsg = ref('')
const menyimpanId = ref<number | null>(null)

const bolehUbah = computed(() => access.can('harga.atur'))

const tersaring = computed(() => {
  const q = cari.value.trim().toLowerCase()
  return items.value.filter(
    (i) =>
      (!q || i.name.toLowerCase().includes(q) || i.code.toLowerCase().includes(q)) &&
      (!filterTipe.value || i.type === filterTipe.value),
  )
})
const sudahAdaHarga = computed(() => Object.keys(hargaMap.value).length)
const belumAdaHarga = computed(
  () => tersaring.value.filter((i) => !(i.id in hargaMap.value)).length,
)

async function muatCabang() {
  const res =
    (await run(() => api.get<Cabang[]>(`/companies/${companyId.value}/branches`, auth.token))) ?? []
  cabang.value = res
  if (!branchId.value && res.length) branchId.value = res[0]!.id
}

async function muatItem(page = 1) {
  const q = `/companies/${companyId.value}/items?per_page=50&page=${page}${filterTipe.value ? `&type=${filterTipe.value}` : ''}`
  const res = await run(() => api.getPage<Item>(q, auth.token))
  if (res) {
    const raw = res as unknown as { data: Item[]; meta?: PageMeta } | Item[]
    if (Array.isArray(raw)) items.value = raw
    else {
      items.value = raw.data
      meta.value = raw.meta ?? null
    }
  }
}

async function muatHarga() {
  if (!branchId.value) return
  hargaMap.value = {}
  hargaEdit.value = {}
  for (const it of items.value) {
    try {
      const h = await api.get<{ price: string }>(
        `/companies/${companyId.value}/items/${it.id}/prices/${branchId.value}`,
        auth.token,
      )
      hargaMap.value = { ...hargaMap.value, [it.id]: h.price }
    } catch {
      /* belum ada harga untuk cabang ini */
    }
  }
}

async function muatSemua() {
  okMsg.value = ''
  await muatItem()
  await muatHarga()
}

async function simpanHarga(it: Item) {
  const nilai = hargaEdit.value[it.id]
  if (nilai === undefined || nilai === null || Number.isNaN(Number(nilai)) || Number(nilai) < 0) {
    okMsg.value = ''
    return
  }
  menyimpanId.value = it.id
  const res = await run(() =>
    api.put(
      `/companies/${companyId.value}/items/${it.id}/prices/${branchId.value}`,
      { price: Number(nilai) },
      auth.token,
    ),
  )
  menyimpanId.value = null
  if (res) {
    hargaMap.value = { ...hargaMap.value, [it.id]: String(nilai) }
    const baru = { ...hargaEdit.value }
    delete baru[it.id]
    hargaEdit.value = baru
    okMsg.value = `Harga ${it.name} tersimpan.`
  }
}

watch(branchId, () => {
  void muatHarga()
})

onMounted(async () => {
  await access.load(companyId.value, auth.token)
  await muatCabang()
  await muatSemua()
})
</script>

<template>
  <div>
    <p class="text-xs font-semibold uppercase tracking-wider text-secondary">
      Katalog & harga jual
    </p>
    <div class="mb-4 flex flex-wrap items-center justify-between gap-2">
      <h1 class="font-display text-xl font-semibold">Harga per cabang</h1>
      <span class="text-xs text-secondary">Perubahan harga tercatat otomatis di audit.</span>
    </div>
    <AppBanner tone="green" :message="okMsg" />
    <AppBanner tone="red" :message="error" />

    <div class="mb-4 grid grid-cols-2 gap-3 lg:grid-cols-4">
      <AppCard
        ><p class="text-xs uppercase tracking-wider text-secondary">Cabang</p>
        <p class="font-display text-2xl font-bold">{{ cabang.length }}</p></AppCard
      >
      <AppCard
        ><p class="text-xs uppercase tracking-wider text-secondary">Item tampil</p>
        <p class="font-display text-2xl font-bold">{{ tersaring.length }}</p></AppCard
      >
      <AppCard
        ><p class="text-xs uppercase tracking-wider text-secondary">Sudah ada harga</p>
        <p class="font-display text-2xl font-bold">{{ sudahAdaHarga }}</p></AppCard
      >
      <AppCard
        ><p class="text-xs uppercase tracking-wider text-secondary">Belum ada harga</p>
        <p class="font-display text-2xl font-bold">{{ belumAdaHarga }}</p></AppCard
      >
    </div>

    <div class="mb-3 flex flex-wrap items-center gap-2">
      <label class="flex items-center gap-1.5 text-sm text-secondary"
        >Cabang
        <select
          v-model="branchId"
          class="rounded-lg border border-outline bg-card px-2 py-2 text-sm focus:border-primary focus:outline-none"
        >
          <option v-for="c in cabang" :key="c.id" :value="c.id">{{ c.name }} ({{ c.code }})</option>
        </select>
      </label>
      <div class="relative min-w-52 flex-1">
        <AppIcon
          name="search"
          :size="18"
          class="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-secondary"
        />
        <input
          v-model="cari"
          class="w-full rounded-lg border border-outline bg-card py-2 pl-9 pr-3 text-sm focus:border-primary focus:outline-none"
          placeholder="Cari nama atau kode item…"
        />
      </div>
      <label class="flex items-center gap-1.5 text-sm text-secondary"
        >Jenis
        <select
          v-model="filterTipe"
          class="rounded-lg border border-outline bg-card px-2 py-2 text-sm focus:border-primary focus:outline-none"
          @change="muatSemua"
        >
          <option value="">Semua</option>
          <option value="barang">Barang</option>
          <option value="jasa">Jasa</option>
        </select>
      </label>
      <AppButton variant="ghost" @click="muatSemua">Muat ulang</AppButton>
    </div>

    <p v-if="!bolehUbah" class="mb-3 rounded-lg bg-surface-low px-3 py-2 text-sm text-secondary">
      Anda hanya bisa melihat harga. Perubahan harga hanya untuk yang diberi wewenang mengatur
      harga.
    </p>

    <p v-if="loading" class="text-sm text-secondary">Memuat…</p>
    <AppEmpty
      v-else-if="!tersaring.length"
      message="Tidak ada item yang cocok. Ubah pencarian atau tambah item di Katalog."
    />

    <div v-else class="overflow-x-auto rounded-xl border border-outline bg-card">
      <table class="w-full min-w-[640px] text-sm">
        <thead>
          <tr
            class="border-b border-outline text-left text-xs uppercase tracking-wider text-secondary"
          >
            <th class="px-4 py-2">Item</th>
            <th class="px-4 py-2">Jenis</th>
            <th class="px-4 py-2 text-right">Harga berlaku</th>
            <th v-if="bolehUbah" class="px-4 py-2 text-right">Harga baru</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="it in tersaring" :key="it.id" class="border-b border-outline/60 last:border-0">
            <td class="px-4 py-2">
              <strong>{{ it.name }}</strong>
              <span class="font-mono text-xs text-secondary">{{ it.code }}</span>
            </td>
            <td class="px-4 py-2 text-secondary">{{ it.type }}</td>
            <td class="px-4 py-2 text-right font-semibold">
              {{ it.id in hargaMap ? rupiah(hargaMap[it.id]) : '—' }}
            </td>
            <td v-if="bolehUbah" class="px-4 py-2">
              <div class="flex items-center justify-end gap-2">
                <input
                  v-model.number="hargaEdit[it.id]"
                  type="number"
                  min="0"
                  class="w-32 rounded-lg bg-surface-low px-3 py-1.5 text-right text-sm outline-none focus:ring-2 focus:ring-primary"
                  :placeholder="hargaMap[it.id] ?? 'cth. 50000'"
                />
                <AppButton
                  :disabled="menyimpanId === it.id || hargaEdit[it.id] === undefined"
                  @click="simpanHarga(it)"
                >
                  {{ menyimpanId === it.id ? '…' : 'Simpan' }}
                </AppButton>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <AppField label="Catatan" class="mt-4">
      <p class="text-xs text-secondary">
        Tidak ada riwayat harga di API — yang tampil hanya harga berlaku saat ini per cabang.
      </p>
    </AppField>
  </div>
</template>
