<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { api, type PageMeta } from '../lib/api'
import { useCompany } from '../composables/useCompany'
import { useAccessStore } from '../stores/access'
import { useAsync } from '../composables/useAsync'
import AppCard from '../components/ui/AppCard.vue'
import AppBadge from '../components/ui/AppBadge.vue'
import AppBanner from '../components/ui/AppBanner.vue'
import AppButton from '../components/ui/AppButton.vue'
import AppField from '../components/ui/AppField.vue'
import AppEmpty from '../components/ui/AppEmpty.vue'

interface Cabang {
  id: number
  code: string
  name: string
}
interface Hadir {
  id: number
  status: string
  clockInAt?: string | null
  clockOutAt?: string | null
  lateMinutes?: number
  [k: string]: unknown
}
interface Cuti {
  id: number
  userId?: number
  type: string
  fromDate?: string
  toDate?: string
  reason?: string
  status: string
  [k: string]: unknown
}

const STATUS = ['hadir', 'terlambat', 'alpa', 'izin', 'sakit', 'cuti'] as const
const TONE: Record<string, 'green' | 'amber' | 'red' | 'blue' | 'gray'> = {
  hadir: 'green',
  terlambat: 'amber',
  alpa: 'red',
  izin: 'blue',
  sakit: 'blue',
  cuti: 'blue',
}

const { auth, companyId } = useCompany()
const access = useAccessStore()
const { loading, error, run } = useAsync()
const tab = ref<'hadir' | 'cuti'>('hadir')
const cabang = ref<Cabang[]>([])
const branchId = ref<number | null>(null)
const tanggalPick = ref(new Date().toISOString().slice(0, 10))
const list = ref<Hadir[]>([])
const meta = ref<PageMeta | null>(null)
const okMsg = ref('')

const bolehLihat = computed(() => access.can('absensi.approve'))
const bolehKoreksi = computed(() => access.can('absensi.koreksi'))

const koreksiId = ref<number | null>(null)
const koreksiStatus = ref('hadir')
const koreksiAlasan = ref('')

const cuti = ref<Cuti[]>([])
const cutiStatus = ref('')
const idPerangkat = ref('')

const ringkas = computed(() => {
  const m: Record<string, number> = {}
  for (const h of list.value) m[h.status] = (m[h.status] ?? 0) + 1
  return m
})

async function muatCabang() {
  cabang.value =
    (await run(() => api.get<Cabang[]>(`/companies/${companyId.value}/branches`, auth.token))) ?? []
  if (!branchId.value && cabang.value.length) branchId.value = cabang.value[0]!.id
}

async function muatHadir(page = 1) {
  if (!branchId.value) return
  okMsg.value = ''
  const res = await run(() =>
    api.get<{ data: Hadir[]; meta: PageMeta }>(
      `/companies/${companyId.value}/branches/${branchId.value}/attendance?date=${tanggalPick.value}&per_page=50&page=${page}`,
      auth.token,
    ),
  )
  if (res) {
    const raw = res as unknown as { data: Hadir[]; meta?: PageMeta } | Hadir[]
    if (Array.isArray(raw)) list.value = raw
    else {
      list.value = raw.data
      meta.value = raw.meta ?? null
    }
  }
}

async function simpanKoreksi() {
  if (!koreksiId.value || !koreksiAlasan.value.trim()) return
  const res = await run(() =>
    api.put(
      `/companies/${companyId.value}/attendances/${koreksiId.value}`,
      {
        status: koreksiStatus.value,
        note: koreksiAlasan.value.trim(),
      },
      auth.token,
    ),
  )
  if (res || !error.value) {
    okMsg.value = 'Koreksi tersimpan dan tercatat di audit.'
    koreksiId.value = null
    koreksiAlasan.value = ''
    muatHadir()
  }
}

async function muatCuti() {
  okMsg.value = ''
  const q = `/companies/${companyId.value}/leaves?per_page=50${cutiStatus.value ? `&status=${cutiStatus.value}` : ''}`
  const res = await run(() => api.get<{ data: Cuti[] }>(q, auth.token))
  if (res) {
    const raw = res as unknown as { data: Cuti[] } | Cuti[]
    cuti.value = Array.isArray(raw) ? raw : raw.data
  }
}

async function putuskanCuti(id: number, setuju: boolean) {
  const res = await run(() =>
    api.post(`/companies/${companyId.value}/leaves/${id}/decide`, { approve: setuju }, auth.token),
  )
  if (res || !error.value) {
    okMsg.value = setuju ? 'Pengajuan disetujui.' : 'Pengajuan ditolak.'
    muatCuti()
  }
}

async function resetPerangkat() {
  if (!idPerangkat.value) return
  const res = await run(() =>
    api.del(`/companies/${companyId.value}/users/${idPerangkat.value}/device`, auth.token),
  )
  if (res || !error.value) {
    okMsg.value = 'Perangkat karyawan direset. Ia bisa login dari HP baru.'
    idPerangkat.value = ''
  }
}

const jam = (iso: unknown) =>
  typeof iso === 'string' && iso
    ? new Date(iso).toLocaleTimeString('id-ID', { hour: '2-digit', minute: '2-digit' })
    : '—'

onMounted(async () => {
  await access.load(companyId.value, auth.token)
  await muatCabang()
  await muatHadir()
  await muatCuti()
})
</script>

<template>
  <div>
    <p class="text-xs font-semibold uppercase tracking-wider text-secondary">
      Pengawasan kehadiran
    </p>
    <h1 class="font-display mb-4 text-xl font-semibold">Kehadiran & cuti karyawan</h1>
    <AppBanner tone="green" :message="okMsg" />
    <AppBanner tone="red" :message="error" />

    <AppEmpty
      v-if="!bolehLihat"
      message="Halaman ini hanya untuk yang diberi wewenang menyetujui absensi."
    />
    <template v-else>
      <div class="mb-3 flex gap-2">
        <AppButton :variant="tab === 'hadir' ? 'primary' : 'ghost'" @click="tab = 'hadir'"
          >Harian cabang</AppButton
        >
        <AppButton :variant="tab === 'cuti' ? 'primary' : 'ghost'" @click="tab = 'cuti'"
          >Pengajuan cuti</AppButton
        >
      </div>

      <div v-if="tab === 'hadir'">
        <div class="mb-3 flex flex-wrap items-center gap-2">
          <label class="flex items-center gap-1.5 text-sm text-secondary"
            >Cabang
            <select
              v-model="branchId"
              class="rounded-lg border border-outline bg-card px-2 py-2 text-sm"
              @change="() => muatHadir()"
            >
              <option v-for="c in cabang" :key="c.id" :value="c.id">{{ c.name }}</option>
            </select>
          </label>
          <input
            v-model="tanggalPick"
            type="date"
            class="rounded-lg border border-outline bg-card px-2 py-2 text-sm"
            @change="() => muatHadir()"
          />
          <AppButton variant="ghost" @click="() => muatHadir()">Muat ulang</AppButton>
        </div>

        <div class="mb-3 flex flex-wrap gap-2">
          <AppCard v-for="s in STATUS" :key="s" class="!p-3">
            <p class="text-xs uppercase tracking-wider text-secondary">{{ s }}</p>
            <p class="font-display text-xl font-bold">{{ ringkas[s] ?? 0 }}</p>
          </AppCard>
        </div>

        <p v-if="loading" class="text-sm text-secondary">Memuat…</p>
        <AppEmpty
          v-else-if="!list.length"
          message="Tidak ada catatan kehadiran pada tanggal ini."
        />
        <div v-else class="overflow-x-auto rounded-xl border border-outline bg-card">
          <table class="w-full min-w-[560px] text-sm">
            <thead>
              <tr
                class="border-b border-outline text-left text-xs uppercase tracking-wider text-secondary"
              >
                <th class="px-4 py-2">ID</th>
                <th class="px-4 py-2">Status</th>
                <th class="px-4 py-2">Masuk</th>
                <th class="px-4 py-2">Pulang</th>
                <th class="px-4 py-2 text-right">Telat</th>
                <th v-if="bolehKoreksi" class="px-4 py-2" />
              </tr>
            </thead>
            <tbody>
              <tr v-for="h in list" :key="h.id" class="border-b border-outline/60 last:border-0">
                <td class="px-4 py-2 font-mono">#{{ h.id }}</td>
                <td class="px-4 py-2">
                  <AppBadge :tone="TONE[h.status] ?? 'gray'">{{ h.status }}</AppBadge>
                </td>
                <td class="px-4 py-2 font-mono">{{ jam(h.clockInAt) }}</td>
                <td class="px-4 py-2 font-mono">{{ jam(h.clockOutAt) }}</td>
                <td class="px-4 py-2 text-right">{{ h.lateMinutes ?? 0 }} mnt</td>
                <td v-if="bolehKoreksi" class="px-4 py-2 text-right">
                  <AppButton variant="ghost" @click="koreksiId = h.id">Koreksi</AppButton>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <p class="mt-1 text-xs text-secondary">
          Nama karyawan lihat di Karyawan & Penempatan — API hanya mengembalikan status dan waktu.
        </p>

        <AppCard v-if="koreksiId && bolehKoreksi" class="mt-3">
          <h2 class="font-display font-semibold">Koreksi absen #{{ koreksiId }}</h2>
          <p class="text-xs text-secondary">Alasan wajib diisi — tercatat di audit.</p>
          <div class="mt-2 grid gap-2 md:grid-cols-3">
            <AppField label="Status baru">
              <select
                v-model="koreksiStatus"
                class="w-full rounded-lg bg-surface-low px-3 py-2 text-sm"
              >
                <option v-for="s in STATUS" :key="s" :value="s">{{ s }}</option>
              </select>
            </AppField>
            <AppField label="Alasan koreksi (wajib)" class="md:col-span-2">
              <input
                v-model="koreksiAlasan"
                maxlength="500"
                placeholder="cth. Lupa bawa HP, ada bukti foto"
                class="w-full rounded-lg bg-surface-low px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-primary"
              />
            </AppField>
          </div>
          <div class="mt-2 flex gap-2">
            <AppButton :disabled="!koreksiAlasan.trim()" @click="simpanKoreksi"
              >Simpan koreksi</AppButton
            >
            <AppButton variant="ghost" @click="koreksiId = null">Batal</AppButton>
          </div>
        </AppCard>

        <AppCard class="mt-3">
          <h2 class="font-display font-semibold">Reset perangkat karyawan</h2>
          <p class="text-xs text-secondary">Untuk yang ganti HP (satu akun satu perangkat).</p>
          <div class="mt-2 flex gap-2">
            <input
              v-model="idPerangkat"
              inputmode="numeric"
              placeholder="ID karyawan"
              class="w-40 rounded-lg bg-surface-low px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-primary"
            />
            <AppButton variant="ghost" @click="resetPerangkat">Reset perangkat</AppButton>
          </div>
        </AppCard>
      </div>

      <div v-if="tab === 'cuti'">
        <div class="mb-3 flex items-center gap-2">
          <label class="flex items-center gap-1.5 text-sm text-secondary"
            >Status
            <select
              v-model="cutiStatus"
              class="rounded-lg border border-outline bg-card px-2 py-2 text-sm"
              @change="() => muatCuti()"
            >
              <option value="">Semua</option>
              <option value="diajukan">Diajukan</option>
              <option value="disetujui">Disetujui</option>
              <option value="ditolak">Ditolak</option>
            </select>
          </label>
          <AppButton variant="ghost" @click="muatCuti">Muat ulang</AppButton>
        </div>
        <AppEmpty v-if="!cuti.length" message="Tidak ada pengajuan pada filter ini." />
        <div class="grid gap-3 md:grid-cols-2">
          <AppCard v-for="c in cuti" :key="c.id">
            <div class="flex items-center justify-between gap-2">
              <strong>#{{ c.id }} · {{ c.type }}</strong>
              <AppBadge
                :tone="
                  c.status === 'disetujui' ? 'green' : c.status === 'ditolak' ? 'red' : 'amber'
                "
                >{{ c.status }}</AppBadge
              >
            </div>
            <p class="mt-1 text-sm">{{ c.fromDate ?? '' }} → {{ c.toDate ?? '' }}</p>
            <p v-if="c.reason" class="mt-1 text-sm text-secondary">{{ c.reason }}</p>
            <div v-if="c.status === 'diajukan'" class="mt-2 flex gap-2">
              <AppButton @click="putuskanCuti(c.id, true)">Setujui</AppButton>
              <AppButton variant="ghost" @click="putuskanCuti(c.id, false)">Tolak</AppButton>
            </div>
          </AppCard>
        </div>
      </div>
    </template>
  </div>
</template>
