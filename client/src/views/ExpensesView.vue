<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { api, type PageMeta } from '../lib/api'
import { rupiah, tanggal } from '../lib/format'
import { useCompany } from '../composables/useCompany'
import { useAccessStore } from '../stores/access'
import { useAsync } from '../composables/useAsync'
import AppCard from '../components/ui/AppCard.vue'
import AppBadge from '../components/ui/AppBadge.vue'
import AppBanner from '../components/ui/AppBanner.vue'
import AppButton from '../components/ui/AppButton.vue'
import AppField from '../components/ui/AppField.vue'
import AppEmpty from '../components/ui/AppEmpty.vue'

interface Kategori {
  id: number
  name: string
}
interface Cabang {
  id: number
  code: string
  name: string
}
interface Biaya {
  id: number
  branchId?: number
  categoryId?: number
  categoryName?: string
  amount: string
  method?: string
  date: string
  description?: string
  status: string
}

const { auth, companyId } = useCompany()
const access = useAccessStore()
const { loading, error, run } = useAsync()
const tab = ref<'biaya' | 'kategori'>('biaya')
const list = ref<Biaya[]>([])
const meta = ref<PageMeta | null>(null)
const kategori = ref<Kategori[]>([])
const cabang = ref<Cabang[]>([])
const filterStatus = ref('')
const okMsg = ref('')

const bolehCatat = computed(() => access.can('biaya.input'))
const bolehPutus = computed(() => access.can('biaya.setujui'))

const showForm = ref(false)
const fBiaya = ref({
  branch_id: '',
  expense_category_id: '',
  amount: 0,
  method: 'tunai',
  date: new Date().toISOString().slice(0, 10),
  description: '',
})
const namaKat = ref('')

async function muatCabang() {
  cabang.value =
    (await run(() => api.get<Cabang[]>(`/companies/${companyId.value}/branches`, auth.token))) ?? []
}

async function muatKategori() {
  const res = await run(() =>
    api.get<Kategori[]>(
      `/companies/${companyId.value}/expense-categories?per_page=100`,
      auth.token,
    ),
  )
  if (res)
    kategori.value = (
      Array.isArray(res) ? res : ((res as unknown as { data: Kategori[] }).data ?? [])
    ) as Kategori[]
}

async function muatBiaya(page = 1) {
  okMsg.value = ''
  const res = await run(() =>
    api.get<{ data: Biaya[]; meta: PageMeta }>(
      `/companies/${companyId.value}/expenses?per_page=20&page=${page}${filterStatus.value ? `&status=${filterStatus.value}` : ''}`,
      auth.token,
    ),
  )
  if (res) {
    const raw = res as unknown as { data: Biaya[]; meta?: PageMeta } | Biaya[]
    if (Array.isArray(raw)) list.value = raw
    else {
      list.value = raw.data
      meta.value = raw.meta ?? null
    }
  }
}

async function simpanBiaya() {
  const res = await run(() =>
    api.post(
      `/companies/${companyId.value}/expenses`,
      {
        branch_id: Number(fBiaya.value.branch_id),
        expense_category_id: Number(fBiaya.value.expense_category_id),
        amount: Number(fBiaya.value.amount),
        method: fBiaya.value.method || undefined,
        date: fBiaya.value.date,
        description: fBiaya.value.description.trim(),
      },
      auth.token,
    ),
  )
  if (res) {
    showForm.value = false
    okMsg.value = 'Biaya tercatat sebagai draft. Minta persetujuan agar masuk laporan.'
    fBiaya.value = {
      branch_id: '',
      expense_category_id: '',
      amount: 0,
      method: 'tunai',
      date: new Date().toISOString().slice(0, 10),
      description: '',
    }
    muatBiaya()
  }
}

async function putuskan(id: number, setuju: boolean) {
  const res = await run(() =>
    api.post(
      `/companies/${companyId.value}/expenses/${id}/${setuju ? 'approve' : 'reject'}`,
      {},
      auth.token,
    ),
  )
  if (res || !error.value) {
    okMsg.value = setuju ? 'Biaya disetujui dan dijurnal otomatis.' : 'Biaya ditolak.'
    muatBiaya()
  }
}

async function tambahKategori() {
  if (!namaKat.value.trim()) return
  const res = await run(() =>
    api.post(
      `/companies/${companyId.value}/expense-categories`,
      { name: namaKat.value.trim() },
      auth.token,
    ),
  )
  if (res) {
    namaKat.value = ''
    okMsg.value = 'Kategori ditambahkan.'
    muatKategori()
  }
}

async function hapusKategori(id: number) {
  await run(() => api.del(`/companies/${companyId.value}/expense-categories/${id}`, auth.token))
  if (!error.value) {
    okMsg.value = 'Kategori dihapus.'
    muatKategori()
  }
}

const tone = (s: string) => (s === 'disetujui' ? 'green' : s === 'ditolak' ? 'red' : 'amber')

onMounted(async () => {
  await access.load(companyId.value, auth.token)
  await muatCabang()
  await muatKategori()
  await muatBiaya()
})
</script>

<template>
  <div>
    <p class="text-xs font-semibold uppercase tracking-wider text-secondary">Operasional harian</p>
    <h1 class="font-display mb-4 text-xl font-semibold">Biaya operasional</h1>
    <AppBanner tone="green" :message="okMsg" />
    <AppBanner tone="red" :message="error" />

    <div class="mb-3 flex gap-2">
      <AppButton :variant="tab === 'biaya' ? 'primary' : 'ghost'" @click="tab = 'biaya'"
        >Daftar biaya</AppButton
      >
      <AppButton :variant="tab === 'kategori' ? 'primary' : 'ghost'" @click="tab = 'kategori'"
        >Kategori</AppButton
      >
    </div>

    <div v-if="tab === 'biaya'">
      <div class="mb-3 flex flex-wrap items-center gap-2">
        <label class="flex items-center gap-1.5 text-sm text-secondary"
          >Status
          <select
            v-model="filterStatus"
            class="rounded-lg border border-outline bg-card px-2 py-2 text-sm"
            @change="() => muatBiaya()"
          >
            <option value="">Semua</option>
            <option value="draft">Draft</option>
            <option value="disetujui">Disetujui</option>
            <option value="ditolak">Ditolak</option>
          </select>
        </label>
        <AppButton v-if="bolehCatat" @click="showForm = true">Catat biaya</AppButton>
      </div>
      <p class="mb-2 text-xs text-secondary">Yang masih draft belum masuk laporan laba rugi.</p>
      <p v-if="loading" class="text-sm text-secondary">Memuat…</p>
      <AppEmpty v-else-if="!list.length" message="Belum ada biaya pada filter ini." />
      <div class="grid gap-3 md:grid-cols-2">
        <AppCard v-for="b in list" :key="b.id">
          <div class="flex items-center justify-between gap-2">
            <strong class="font-display">{{ rupiah(b.amount) }}</strong>
            <AppBadge :tone="tone(b.status)">{{ b.status }}</AppBadge>
          </div>
          <p class="mt-1 text-sm">{{ b.description ?? '—' }}</p>
          <p class="mt-0.5 text-xs text-secondary">
            {{ b.categoryName ?? '' }} · {{ tanggal(b.date) }} · {{ b.method ?? '' }}
          </p>
          <div v-if="b.status === 'draft' && bolehPutus" class="mt-2 flex gap-2">
            <AppButton @click="putuskan(b.id, true)">Setujui</AppButton>
            <AppButton variant="ghost" @click="putuskan(b.id, false)">Tolak</AppButton>
          </div>
        </AppCard>
      </div>
    </div>

    <AppCard v-else>
      <h2 class="font-display font-semibold">Kategori biaya</h2>
      <div v-if="bolehPutus" class="mt-2 flex gap-2">
        <input
          v-model="namaKat"
          maxlength="255"
          placeholder="cth. Listrik & air"
          class="flex-1 rounded-lg bg-surface-low px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-primary"
        />
        <AppButton @click="tambahKategori">Tambah</AppButton>
      </div>
      <ul class="mt-3 divide-y divide-outline/60">
        <li
          v-for="k in kategori"
          :key="k.id"
          class="flex items-center justify-between py-2 text-sm"
        >
          <span>{{ k.name }}</span>
          <AppButton v-if="bolehPutus" variant="ghost" @click="hapusKategori(k.id)"
            >Hapus</AppButton
          >
        </li>
      </ul>
      <AppEmpty v-if="!kategori.length" message="Belum ada kategori." />
    </AppCard>

    <div
      v-if="showForm"
      class="fixed inset-0 z-20 flex items-center justify-center bg-black/40 p-4"
      @click.self="showForm = false"
    >
      <form class="w-full max-w-md rounded-xl bg-card p-5 shadow-xl" @submit.prevent="simpanBiaya">
        <h3 class="font-display font-semibold">Catat biaya baru</h3>
        <div class="mt-2 grid grid-cols-2 gap-2">
          <AppField label="Cabang"
            ><select
              v-model="fBiaya.branch_id"
              class="w-full rounded-lg bg-surface-low px-3 py-2 text-sm"
            >
              <option value="">— pilih —</option>
              <option v-for="c in cabang" :key="c.id" :value="c.id">{{ c.name }}</option>
            </select></AppField
          >
          <AppField label="Kategori"
            ><select
              v-model="fBiaya.expense_category_id"
              class="w-full rounded-lg bg-surface-low px-3 py-2 text-sm"
            >
              <option value="">— pilih —</option>
              <option v-for="k in kategori" :key="k.id" :value="k.id">{{ k.name }}</option>
            </select></AppField
          >
        </div>
        <div class="grid grid-cols-2 gap-2">
          <AppField label="Nominal (Rp)"
            ><input
              v-model.number="fBiaya.amount"
              type="number"
              min="0"
              class="w-full rounded-lg bg-surface-low px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-primary"
          /></AppField>
          <AppField label="Cara bayar"
            ><select
              v-model="fBiaya.method"
              class="w-full rounded-lg bg-surface-low px-3 py-2 text-sm"
            >
              <option value="tunai">Tunai</option>
              <option value="transfer">Transfer</option>
            </select></AppField
          >
        </div>
        <AppField label="Tanggal"
          ><input
            v-model="fBiaya.date"
            type="date"
            class="w-full rounded-lg bg-surface-low px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-primary"
        /></AppField>
        <AppField label="Keterangan (wajib)"
          ><input
            v-model="fBiaya.description"
            maxlength="500"
            placeholder="cth. Bayar listrik bulan ini"
            class="w-full rounded-lg bg-surface-low px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-primary"
        /></AppField>
        <div class="mt-3 flex justify-end gap-2">
          <AppButton variant="ghost" type="button" @click="showForm = false">Batal</AppButton>
          <AppButton type="submit">Simpan draft</AppButton>
        </div>
      </form>
    </div>
  </div>
</template>
