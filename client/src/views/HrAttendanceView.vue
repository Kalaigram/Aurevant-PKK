<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { api } from '../lib/api'
import { tanggal } from '../lib/format'
import {
  labelAttendanceStatus,
  labelLeaveStatus,
  labelLeaveType,
} from '../lib/labels'
import { useCompany } from '../composables/useCompany'
import { useAccessStore } from '../stores/access'
import { useAsync } from '../composables/useAsync'
import { usePagedList } from '../composables/usePagedList'
import AppBadge from '../components/ui/AppBadge.vue'
import AppBanner from '../components/ui/AppBanner.vue'
import AppButton from '../components/ui/AppButton.vue'
import AppCard from '../components/ui/AppCard.vue'
import AppDateInput from '../components/ui/AppDateInput.vue'
import AppEmpty from '../components/ui/AppEmpty.vue'
import AppField from '../components/ui/AppField.vue'
import AppInput from '../components/ui/AppInput.vue'
import AppModal from '../components/ui/AppModal.vue'
import AppPageHeader from '../components/ui/AppPageHeader.vue'
import AppPager from '../components/ui/AppPager.vue'
import AppSelect from '../components/ui/AppSelect.vue'
import AppStat from '../components/ui/AppStat.vue'
import AppTable from '../components/ui/AppTable.vue'
import AppTabs from '../components/ui/AppTabs.vue'
import AppTextarea from '../components/ui/AppTextarea.vue'

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
const ICON: Record<string, string> = {
  hadir: 'check_circle',
  terlambat: 'schedule',
  alpa: 'cancel',
  izin: 'event_busy',
  sakit: 'sick',
  cuti: 'beach_access',
}
const TONE_STAT: Record<string, 'positive' | 'warning' | 'negative' | 'default'> = {
  hadir: 'positive',
  terlambat: 'warning',
  alpa: 'negative',
  izin: 'default',
  sakit: 'default',
  cuti: 'default',
}
const TONE_LEAVE: Record<string, 'green' | 'amber' | 'red'> = {
  disetujui: 'green',
  ditolak: 'red',
  diajukan: 'amber',
}
const opsiStatus = STATUS.map((status) => ({
  value: status,
  label: labelAttendanceStatus(status),
}))
const opsiStatusCuti = ['', 'diajukan', 'disetujui', 'ditolak'].map((status) => ({
  value: status,
  label: status ? labelLeaveStatus(status) : 'Semua status',
}))

const { auth, companyId } = useCompany()
const access = useAccessStore()
const { loading: loadingMutasi, error: errorMutasi, run } = useAsync()
const tab = ref<'hadir' | 'cuti'>('hadir')
const cabang = ref<Cabang[]>([])
const branchId = ref<number | null>(null)
const tanggalPick = ref(new Date().toISOString().slice(0, 10))
const {
  rows: list,
  meta: metaHadir,
  loading: loadingHadir,
  error: errorHadir,
  load: muatHadir,
  setPage: setPageHadir,
  setPerPage: setPerPageHadir,
} = usePagedList<Hadir>(
  (page, perPage) => {
    if (!branchId.value) return Promise.resolve({ data: [], meta: null })
    return api.getPage<Hadir>(
      `/companies/${companyId.value}/branches/${branchId.value}/attendance?date=${tanggalPick.value}&per_page=${perPage}&page=${page}`,
      auth.token,
    )
  },
  { perPage: 50 },
)
const okMsg = ref('')

const bolehLihat = computed(() => access.can('absensi.approve'))
const bolehKoreksi = computed(() => access.can('absensi.koreksi'))

const koreksiId = ref<number | null>(null)
const koreksiStatus = ref('hadir')
const koreksiAlasan = ref('')

const cutiStatus = ref('')
const {
  rows: cuti,
  meta: metaCuti,
  loading: loadingCuti,
  error: errorCuti,
  load: muatCuti,
  setPage: setPageCuti,
  setPerPage: setPerPageCuti,
} = usePagedList<Cuti>(
  (page, perPage) =>
    api.getPage<Cuti>(
      `/companies/${companyId.value}/leaves?per_page=${perPage}&page=${page}${cutiStatus.value ? `&status=${cutiStatus.value}` : ''}`,
      auth.token,
    ),
  { perPage: 20 },
)
const error = computed(() => errorHadir.value || errorCuti.value || errorMutasi.value)
const idPerangkat = ref('')
const memprosesCuti = ref<number | null>(null)

const tabs = computed(() => [
  { key: 'hadir', label: 'Harian cabang', count: metaHadir.value?.total ?? 0 },
  { key: 'cuti', label: 'Pengajuan cuti', count: metaCuti.value?.total ?? 0 },
])
const opsiCabang = computed(() =>
  cabang.value.map((item) => ({ value: item.id, label: `${item.code} — ${item.name}` })),
)
const colspanHadir = computed(() => (bolehKoreksi.value ? 6 : 5))
const colspanCuti = computed(() => (bolehLihat.value ? 6 : 5))

const ringkas = computed(() => {
  const m: Record<string, number> = {}
  for (const h of list.value) m[h.status] = (m[h.status] ?? 0) + 1
  return m
})

function durasiCuti(item: Cuti) {
  if (!item.fromDate || !item.toDate) return '—'
  const from = new Date(`${item.fromDate}T00:00:00`).getTime()
  const to = new Date(`${item.toDate}T00:00:00`).getTime()
  if (Number.isNaN(from) || Number.isNaN(to) || to < from) return '—'
  return `${Math.round((to - from) / 864e5) + 1} hari`
}

async function muatCabang() {
  cabang.value =
    (await run(() => api.get<Cabang[]>(`/companies/${companyId.value}/branches`, auth.token))) ?? []
  if (!branchId.value && cabang.value.length) branchId.value = cabang.value[0]!.id
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
  if (res || !errorMutasi.value) {
    okMsg.value = 'Koreksi tersimpan dan tercatat di audit.'
    koreksiId.value = null
    koreksiAlasan.value = ''
    muatHadir()
  }
}

async function putuskanCuti(id: number, setuju: boolean) {
  if (memprosesCuti.value !== null) return
  memprosesCuti.value = id
  const res = await run(() =>
    api.post(`/companies/${companyId.value}/leaves/${id}/decide`, { approve: setuju }, auth.token),
  )
  if (res || !errorMutasi.value) {
    okMsg.value = setuju ? 'Pengajuan disetujui.' : 'Pengajuan ditolak.'
    await muatCuti()
  }
  memprosesCuti.value = null
}

async function resetPerangkat() {
  if (!idPerangkat.value) return
  const res = await run(() =>
    api.del(`/companies/${companyId.value}/users/${idPerangkat.value}/device`, auth.token),
  )
  if (res || !errorMutasi.value) {
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
    <AppPageHeader
      eyebrow="Pengawasan kehadiran"
      title="Kehadiran & cuti karyawan"
      subtitle="Pantau kehadiran per cabang, koreksi absensi, dan putuskan pengajuan cuti."
      icon="fact_check"
    >
      <template #actions>
        <AppButton
          variant="ghost"
          :loading="tab === 'hadir' ? loadingHadir : loadingCuti"
          @click="tab === 'hadir' ? muatHadir(1) : muatCuti(1)"
        >
          Muat ulang
        </AppButton>
      </template>
    </AppPageHeader>

    <AppBanner tone="green" :message="okMsg" />
    <AppBanner tone="red" :message="error" />

    <AppEmpty
      v-if="!bolehLihat"
      title="Akses terbatas"
      message="Halaman ini hanya untuk yang diberi wewenang menyetujui absensi."
    />
    <template v-else>
      <AppTabs
        v-model="tab"
        :items="tabs"
        label="Kehadiran dan cuti"
        id-prefix="hr-tab"
        panel-prefix="hr-attendance"
      />

      <section
        v-if="tab === 'hadir'"
        id="hr-attendance-hadir"
        role="tabpanel"
        aria-labelledby="hr-tab-hadir"
        class="mt-4"
      >
        <div class="rounded-2xl border border-outline/60 bg-card p-3 shadow-card sm:p-4">
          <div class="grid gap-3 sm:grid-cols-2 lg:grid-cols-[minmax(0,1fr)_minmax(0,1fr)_auto] lg:items-end">
            <AppField label="Cabang">
              <AppSelect
                v-model="branchId"
                :options="opsiCabang"
                placeholder="Pilih cabang"
                @update:model-value="muatHadir(1)"
              />
            </AppField>
            <AppField label="Tanggal">
              <AppDateInput v-model="tanggalPick" @change="muatHadir(1)" />
            </AppField>
            <AppButton variant="ghost" :loading="loadingHadir" @click="muatHadir()">
              Muat ulang
            </AppButton>
          </div>
        </div>

        <div class="mt-4">
          <p class="mb-2 text-xs text-secondary">
            Ringkasan {{ list.length }} catatan pada halaman ini.
          </p>
          <div class="grid grid-cols-2 gap-3 sm:grid-cols-3 xl:grid-cols-6">
            <AppStat
              v-for="s in STATUS"
              :key="s"
              :label="labelAttendanceStatus(s)"
              :value="ringkas[s] ?? 0"
              :icon="ICON[s]"
              :tone="TONE_STAT[s]"
            />
          </div>
        </div>

        <div class="mt-4">
          <AppTable
            caption="Kehadiran harian cabang"
            :loading="loadingHadir"
            :empty="!list.length"
            :colspan="colspanHadir"
            empty-title="Belum ada catatan kehadiran"
            empty-message="Tidak ada catatan kehadiran pada cabang dan tanggal ini."
          >
            <template #head>
              <tr>
                <th>ID absensi</th>
                <th>Status</th>
                <th>Masuk</th>
                <th>Pulang</th>
                <th class="text-right">Telat</th>
                <th v-if="bolehKoreksi" class="text-right">Aksi</th>
              </tr>
            </template>
            <tr v-for="h in list" :key="h.id">
              <td class="font-mono text-xs">#{{ h.id }}</td>
              <td>
                <AppBadge :tone="TONE[h.status] ?? 'gray'" dot>
                  {{ labelAttendanceStatus(h.status) }}
                </AppBadge>
              </td>
              <td class="font-mono text-xs tabular-nums">{{ jam(h.clockInAt) }}</td>
              <td class="font-mono text-xs tabular-nums">{{ jam(h.clockOutAt) }}</td>
              <td class="text-right text-sm tabular-nums">
                {{ h.lateMinutes ? `${h.lateMinutes} mnt` : '—' }}
              </td>
              <td v-if="bolehKoreksi" class="text-right">
                <AppButton variant="ghost" size="sm" @click="koreksiId = h.id">
                  Koreksi
                </AppButton>
              </td>
            </tr>
          </AppTable>
          <p class="mt-2 text-xs text-secondary">
            Nama karyawan lihat di Karyawan & Penempatan — API hanya mengembalikan status dan waktu.
          </p>
          <AppPager
            :meta="metaHadir"
            :show-per-page="false"
            @page="setPageHadir"
            @update:per-page="setPerPageHadir"
          />
        </div>

        <AppCard class="mt-4">
          <h2 class="font-display font-semibold">Reset perangkat karyawan</h2>
          <p class="mt-1 text-xs text-secondary">
            Untuk yang ganti HP (satu akun satu perangkat).
          </p>
          <div class="mt-3 flex flex-col gap-2 sm:flex-row sm:items-end">
            <AppField label="ID karyawan" class="sm:max-w-48">
              <AppInput
                v-model="idPerangkat"
                inputmode="numeric"
                placeholder="cth. 12"
              />
            </AppField>
            <AppButton
              variant="ghost"
              :disabled="!idPerangkat"
              @click="resetPerangkat"
            >
              Reset perangkat
            </AppButton>
          </div>
        </AppCard>
      </section>

      <section
        v-else
        id="hr-attendance-cuti"
        role="tabpanel"
        aria-labelledby="hr-tab-cuti"
        class="mt-4"
      >
        <div class="rounded-2xl border border-outline/60 bg-card p-3 shadow-card sm:p-4">
          <div class="grid gap-3 sm:grid-cols-[minmax(0,1fr)_auto] sm:items-end">
            <AppField label="Status pengajuan">
              <AppSelect
                v-model="cutiStatus"
                :options="opsiStatusCuti"
                @update:model-value="muatCuti(1)"
              />
            </AppField>
            <AppButton variant="ghost" :loading="loadingCuti" @click="muatCuti()">
              Muat ulang
            </AppButton>
          </div>
        </div>

        <div class="mt-4">
          <AppTable
            caption="Pengajuan cuti karyawan"
            :loading="loadingCuti"
            :empty="!cuti.length"
            :colspan="colspanCuti"
            empty-title="Belum ada pengajuan cuti"
            empty-message="Tidak ada pengajuan pada filter status ini."
          >
            <template #head>
              <tr>
                <th>Jenis</th>
                <th>Rentang</th>
                <th>Durasi</th>
                <th>Alasan</th>
                <th>Status</th>
                <th v-if="bolehLihat" class="text-right">Aksi</th>
              </tr>
            </template>
            <tr v-for="c in cuti" :key="c.id">
              <td class="font-semibold text-ink">{{ labelLeaveType(c.type) }}</td>
              <td class="whitespace-nowrap text-sm">
                {{ tanggal(c.fromDate) }} – {{ tanggal(c.toDate) }}
              </td>
              <td class="text-sm tabular-nums">{{ durasiCuti(c) }}</td>
              <td class="max-w-64 text-sm text-secondary">
                <p class="truncate" :title="c.reason ?? ''">{{ c.reason || '—' }}</p>
              </td>
              <td>
                <AppBadge :tone="TONE_LEAVE[c.status] ?? 'amber'" dot>
                  {{ labelLeaveStatus(c.status) }}
                </AppBadge>
              </td>
              <td v-if="bolehLihat" class="text-right">
                <div
                  v-if="c.status === 'diajukan'"
                  class="flex justify-end gap-1"
                >
                  <AppButton
                    size="sm"
                    variant="ghost"
                    :loading="memprosesCuti === c.id"
                    :disabled="memprosesCuti !== null && memprosesCuti !== c.id"
                    @click="putuskanCuti(c.id, true)"
                  >
                    Setujui
                  </AppButton>
                  <AppButton
                    size="sm"
                    variant="ghost"
                    :disabled="memprosesCuti !== null"
                    @click="putuskanCuti(c.id, false)"
                  >
                    Tolak
                  </AppButton>
                </div>
                <span v-else class="text-xs text-secondary">Selesai</span>
              </td>
            </tr>
          </AppTable>
          <AppPager :meta="metaCuti" @page="setPageCuti" @update:per-page="setPerPageCuti" />
        </div>
      </section>
    </template>

    <AppModal
      :open="koreksiId !== null && bolehKoreksi"
      title="Koreksi absensi"
      :description="`Absensi #${koreksiId ?? ''} — alasan wajib diisi dan tercatat di audit.`"
      @close="koreksiId = null"
    >
      <form
        id="attendance-correction-form"
        class="space-y-3"
        @submit.prevent="simpanKoreksi"
      >
        <AppField label="Status baru" required>
          <AppSelect v-model="koreksiStatus" :options="opsiStatus" />
        </AppField>
        <AppField label="Alasan koreksi" required hint="Tercatat di audit dan tidak bisa dihapus.">
          <AppTextarea
            v-model="koreksiAlasan"
            :rows="3"
            :maxlength="500"
            :show-count="true"
            placeholder="cth. Lupa bawa HP, ada bukti foto"
          />
        </AppField>
      </form>
      <template #footer>
        <AppButton variant="ghost" @click="koreksiId = null">Batal</AppButton>
        <AppButton
          type="submit"
          form="attendance-correction-form"
          :loading="loadingMutasi"
          :disabled="!koreksiAlasan.trim()"
        >
          Simpan koreksi
        </AppButton>
      </template>
    </AppModal>
  </div>
</template>
