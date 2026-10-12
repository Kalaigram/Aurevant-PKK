<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '../lib/api'
import { useAuthStore } from '../stores/auth'
import { useAccessStore } from '../stores/access'
import { useAsync } from '../composables/useAsync'
import AppCard from '../components/ui/AppCard.vue'
import AppBadge from '../components/ui/AppBadge.vue'
import AppBanner from '../components/ui/AppBanner.vue'
import AppButton from '../components/ui/AppButton.vue'
import AppField from '../components/ui/AppField.vue'
import AppEmpty from '../components/ui/AppEmpty.vue'
import AppIcon from '../components/ui/AppIcon.vue'

interface Cabang {
  id: number
  code: string
  name: string
  type: string
  timezone: string
  address: string | null
  phone: string | null
  status: string
}

const TIPE = [
  { value: 'pusat', label: 'Kantor Pusat', tone: 'blue' },
  { value: 'cabang', label: 'Cabang Ritel', tone: 'green' },
  { value: 'gudang', label: 'Gudang Logistik', tone: 'amber' },
] as const
const ZONA: Record<string, string> = { 'Asia/Jakarta': 'WIB', 'Asia/Makassar': 'WITA', 'Asia/Jayapura': 'WIT' }
const WARNA_TILE = ['bg-primary-container', 'bg-tertiary', 'bg-secondary', 'bg-amber-600', 'bg-blue-700', 'bg-teal-700']

const router = useRouter()
const auth = useAuthStore()
const access = useAccessStore()
const { loading, error, run } = useAsync()
const list = ref<Cabang[]>([])
const cari = ref('')
const filterTipe = ref('semua')
const filterStatus = ref('semua')
const bolehKelola = computed(() => access.can('cabang.kelola'))

const showForm = ref(false)
const sunting = ref<Cabang | null>(null)
const form = ref({ code: '', name: '', type: 'cabang', timezone: 'Asia/Jakarta', address: '', phone: '', status: 'aktif' })
const tanyaHapus = ref<Cabang | null>(null)
const okMsg = ref('')

const tersaring = computed(() => {
  const q = cari.value.trim().toLowerCase()
  return list.value.filter((b) =>
    (!q || b.name.toLowerCase().includes(q) || b.code.toLowerCase().includes(q) || (b.address ?? '').toLowerCase().includes(q)) &&
    (filterTipe.value === 'semua' || b.type === filterTipe.value) &&
    (filterStatus.value === 'semua' || b.status === filterStatus.value),
  )
})
const totalAktif = computed(() => list.value.filter((b) => b.status === 'aktif').length)
const totalNonaktif = computed(() => list.value.filter((b) => b.status !== 'aktif').length)
const cakupanZona = computed(() => [...new Set(list.value.map((b) => ZONA[b.timezone] ?? b.timezone))].join(' & ') || '–')
const labelTipe = (t: string) => TIPE.find((x) => x.value === t)?.label ?? t
const toneTipe = (t: string) => (TIPE.find((x) => x.value === t)?.tone ?? 'gray') as 'green' | 'red' | 'amber' | 'blue' | 'gray'

async function muat() {
  okMsg.value = ''
  list.value = (await run(() => api.get<Cabang[]>(`/companies/${auth.companyId}/branches`, auth.token))) ?? []
}

function bukaTambah() {
  sunting.value = null
  form.value = { code: '', name: '', type: 'cabang', timezone: 'Asia/Jakarta', address: '', phone: '', status: 'aktif' }
  showForm.value = true
}

function bukaUbah(b: Cabang) {
  sunting.value = b
  form.value = { code: b.code, name: b.name, type: b.type, timezone: b.timezone, address: b.address ?? '', phone: b.phone ?? '', status: b.status }
  showForm.value = true
}

function payload() {
  return {
    code: form.value.code.trim().toUpperCase(),
    name: form.value.name.trim(),
    type: form.value.type,
    timezone: form.value.timezone,
    address: form.value.address.trim() || undefined,
    phone: form.value.phone.trim() || undefined,
    status: form.value.status,
  }
}

async function simpan() {
  const base = `/companies/${auth.companyId}/branches`
  const res = sunting.value
    ? await run(() => api.put(base + `/${sunting.value!.id}`, payload(), auth.token))
    : await run(() => api.post(base, payload(), auth.token))
  if (res) {
    showForm.value = false
    okMsg.value = sunting.value ? 'Cabang diperbarui.' : 'Cabang baru didaftarkan.'
    muat()
  }
}

async function hapus() {
  if (!tanyaHapus.value) return
  await run(() => api.del(`/companies/${auth.companyId}/branches/${tanyaHapus.value!.id}`, auth.token))
  if (!error.value) {
    tanyaHapus.value = null
    okMsg.value = 'Cabang dihapus.'
    muat()
  }
}

function pilih(b: Cabang) {
  auth.pickBranch(b.id, b.code)
  router.push(`/c/${auth.companyId}/dashboard`)
}

onMounted(async () => {
  if (auth.companyId) {
    await access.load(auth.companyId, auth.token)
    muat()
  }
})
</script>

<template>
  <div>
    <p class="text-xs font-semibold uppercase tracking-wider text-secondary">Struktur organisasi & cabang</p>
    <div class="mb-4 flex flex-wrap items-center justify-between gap-2">
      <h1 class="font-display text-xl font-semibold">Daftar cabang perusahaan</h1>
      <div class="flex gap-2">
        <AppButton v-if="bolehKelola" @click="bukaTambah"><AppIcon name="add" :size="18" />Tambah cabang</AppButton>
      </div>
    </div>
    <AppBanner tone="green" :message="okMsg" />
    <AppBanner tone="red" :message="error" />

    <div class="mb-4 grid grid-cols-2 gap-3 lg:grid-cols-4">
      <AppCard><p class="text-xs uppercase tracking-wider text-secondary">Total unit</p><p class="font-display text-2xl font-bold">{{ list.length }} <span class="text-sm font-normal text-secondary">entitas</span></p></AppCard>
      <AppCard><p class="text-xs uppercase tracking-wider text-secondary">Status aktif</p><p class="font-display text-2xl font-bold">{{ totalAktif }} <span class="text-sm font-normal text-secondary">cabang</span></p></AppCard>
      <AppCard><p class="text-xs uppercase tracking-wider text-secondary">Nonaktif</p><p class="font-display text-2xl font-bold">{{ totalNonaktif }}</p></AppCard>
      <AppCard><p class="text-xs uppercase tracking-wider text-secondary">Cakupan wilayah</p><p class="font-display text-2xl font-bold">{{ cakupanZona }}</p></AppCard>
    </div>

    <div class="mb-3 flex flex-wrap items-center gap-2">
      <div class="relative min-w-52 flex-1">
        <AppIcon name="search" :size="18" class="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-secondary" />
        <input v-model="cari" class="w-full rounded-lg border border-outline bg-card py-2 pl-9 pr-3 text-sm focus:border-primary focus:outline-none" placeholder="Cari nama, kode, atau alamat…" />
      </div>
      <label class="flex items-center gap-1.5 text-sm text-secondary">Tipe
        <select v-model="filterTipe" class="rounded-lg border border-outline bg-card px-2 py-2 text-sm focus:border-primary focus:outline-none">
          <option value="semua">Semua</option>
          <option value="pusat">Kantor Pusat</option>
          <option value="cabang">Cabang Ritel</option>
          <option value="gudang">Gudang</option>
        </select>
      </label>
      <label class="flex items-center gap-1.5 text-sm text-secondary">Status
        <select v-model="filterStatus" class="rounded-lg border border-outline bg-card px-2 py-2 text-sm focus:border-primary focus:outline-none">
          <option value="semua">Semua</option>
          <option value="aktif">Aktif</option>
          <option value="nonaktif">Nonaktif</option>
        </select>
      </label>
    </div>

    <p v-if="loading" class="text-sm text-secondary">Memuat…</p>
    <AppEmpty v-else-if="!tersaring.length" message="Tidak ada cabang yang cocok. Ubah pencarian atau daftarkan cabang baru." />

    <div class="grid gap-3 md:grid-cols-2">
      <AppCard v-for="(b, i) in tersaring" :key="b.id">
        <div class="flex items-start gap-3">
          <div class="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl font-display text-sm font-bold text-white" :class="WARNA_TILE[i % WARNA_TILE.length]">
            {{ b.code.slice(0, 3).toUpperCase() }}
          </div>
          <div class="min-w-0 flex-1">
            <div class="flex items-center justify-between gap-2">
              <h2 class="font-display truncate font-semibold">{{ b.name }}</h2>
              <span class="flex items-center gap-1">
                <AppBadge :tone="toneTipe(b.type)">{{ labelTipe(b.type) }}</AppBadge>
                <AppBadge :tone="b.status === 'aktif' ? 'green' : 'gray'">{{ b.status }}</AppBadge>
              </span>
            </div>
            <p class="mt-0.5 font-mono text-xs text-secondary">{{ b.code }} · {{ ZONA[b.timezone] ?? b.timezone }} ({{ b.timezone }})</p>
          </div>
          <div v-if="bolehKelola" class="relative shrink-0">
            <details class="relative">
              <summary class="cursor-pointer list-none rounded-lg p-1.5 hover:bg-surface-low" aria-label="Menu cabang"><AppIcon name="more_vert" :size="20" /></summary>
              <div class="absolute right-0 z-10 w-36 rounded-lg border border-outline bg-card py-1 shadow-lg">
                <button class="block w-full px-3 py-2 text-left text-sm hover:bg-surface-low" @click="bukaUbah(b)">Ubah cabang</button>
                <button class="block w-full px-3 py-2 text-left text-sm text-danger hover:bg-surface-low" @click="tanyaHapus = b">Hapus cabang</button>
              </div>
            </details>
          </div>
        </div>
        <p v-if="b.address" class="mt-2 line-clamp-2 text-sm text-secondary">{{ b.address }}</p>
        <p v-if="b.phone" class="mt-0.5 font-mono text-xs text-secondary">{{ b.phone }}</p>
        <div class="mt-3 flex items-center justify-between gap-2">
          <span v-if="auth.branchCode === b.code" class="rounded bg-primary-container/15 px-2 py-0.5 text-xs font-semibold text-primary">Sedang dipilih</span>
          <span v-else />
          <AppButton variant="ghost" :disabled="b.status !== 'aktif'" @click="pilih(b)">Pilih</AppButton>
        </div>
      </AppCard>
    </div>

    <div v-if="showForm" class="fixed inset-0 z-20 flex items-center justify-center bg-black/40 p-4" @click.self="showForm = false">
      <form class="max-h-[90vh] w-full max-w-md overflow-y-auto rounded-xl bg-card p-5 shadow-xl" @submit.prevent="simpan">
        <h3 class="font-display font-semibold">{{ sunting ? 'Ubah cabang' : 'Tambah cabang baru' }}</h3>
        <p class="text-xs text-secondary">Konfigurasi unit operasional perusahaan.</p>
        <div class="mt-3 grid grid-cols-2 gap-2">
          <AppField label="Kode (wajib)">
            <input v-model="form.code" maxlength="20" class="w-full rounded-lg bg-surface-low px-3 py-2 font-mono text-sm uppercase outline-none focus:ring-2 focus:ring-primary" placeholder="cth. PST" />
          </AppField>
          <AppField label="Status">
            <select v-model="form.status" class="w-full rounded-lg bg-surface-low px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-primary">
              <option value="aktif">Aktif (bisa transaksi)</option>
              <option value="nonaktif">Nonaktif (ditangguhkan)</option>
            </select>
          </AppField>
        </div>
        <div class="mt-3">
          <AppField label="Nama cabang (wajib)">
            <input v-model="form.name" maxlength="255" class="w-full rounded-lg bg-surface-low px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-primary" placeholder="cth. Pusat Ciledug" />
          </AppField>
        </div>
        <div class="mt-3 grid grid-cols-2 gap-2">
          <AppField label="Tipe">
            <select v-model="form.type" class="w-full rounded-lg bg-surface-low px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-primary">
              <option value="pusat">Kantor Pusat</option>
              <option value="cabang">Cabang Ritel</option>
              <option value="gudang">Gudang Logistik</option>
            </select>
          </AppField>
          <AppField label="Zona waktu">
            <select v-model="form.timezone" class="w-full rounded-lg bg-surface-low px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-primary">
              <option value="Asia/Jakarta">Asia/Jakarta (WIB)</option>
              <option value="Asia/Makassar">Asia/Makassar (WITA)</option>
              <option value="Asia/Jayapura">Asia/Jayapura (WIT)</option>
            </select>
          </AppField>
        </div>
        <div class="mt-3">
          <AppField label="Alamat">
            <textarea v-model="form.address" rows="2" maxlength="500" class="w-full rounded-lg bg-surface-low px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-primary" />
          </AppField>
        </div>
        <div class="mt-3">
          <AppField label="Telepon">
            <input v-model="form.phone" maxlength="30" class="w-full rounded-lg bg-surface-low px-3 py-2 font-mono text-sm outline-none focus:ring-2 focus:ring-primary" />
          </AppField>
        </div>
        <div class="mt-4 flex justify-end gap-2">
          <AppButton variant="ghost" @click="showForm = false">Batal</AppButton>
          <AppButton type="submit" :loading="loading">Simpan cabang</AppButton>
        </div>
      </form>
    </div>

    <div v-if="tanyaHapus" class="fixed inset-0 z-20 flex items-center justify-center bg-black/40 p-4" @click.self="tanyaHapus = null">
      <div class="w-full max-w-sm rounded-xl bg-card p-5 shadow-xl">
        <h3 class="font-display font-semibold">Hapus cabang {{ tanyaHapus.name }}?</h3>
        <p class="mt-1 text-sm text-secondary">Cabang yang masih punya transaksi atau shift tidak bisa dihapus (server menolak).</p>
        <div class="mt-4 flex justify-end gap-2">
          <AppButton variant="ghost" @click="tanyaHapus = null">Batal</AppButton>
          <AppButton variant="danger" :loading="loading" @click="hapus">Ya, hapus</AppButton>
        </div>
      </div>
    </div>
  </div>
</template>
