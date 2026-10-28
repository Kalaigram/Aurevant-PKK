<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { api } from '../lib/api'
import { tanggal } from '../lib/format'
import { labelLeaveStatus } from '../lib/labels'
import { useCompany } from '../composables/useCompany'
import { useAccessStore } from '../stores/access'
import { useAsync } from '../composables/useAsync'
import { usePagedList } from '../composables/usePagedList'
import AppBadge from '../components/ui/AppBadge.vue'
import AppBanner from '../components/ui/AppBanner.vue'
import AppButton from '../components/ui/AppButton.vue'
import AppCard from '../components/ui/AppCard.vue'
import AppDateInput from '../components/ui/AppDateInput.vue'
import AppField from '../components/ui/AppField.vue'
import AppInput from '../components/ui/AppInput.vue'
import AppPageHeader from '../components/ui/AppPageHeader.vue'
import AppPager from '../components/ui/AppPager.vue'
import AppSelect from '../components/ui/AppSelect.vue'
import AppTable from '../components/ui/AppTable.vue'
import AppTabs from '../components/ui/AppTabs.vue'
import AppTextarea from '../components/ui/AppTextarea.vue'

interface Penempatan {
  userId: number
  userName: string | null
}
interface Saldo {
  id: number | null
  userId: number
  userName: string
  year: number
  entitlement: string
  carryover: string
  used: string
  available: string
}
interface Lembur {
  id: number
  userId: number
  userName: string
  date: string
  startTime: string
  endTime: string
  minutes: number
  reason: string
  status: string
  note: string | null
}
interface TukarShift {
  id: number
  requesterId: number
  requesterName: string
  targetUserId: number
  targetName: string
  date: string
  requesterShiftName: string | null
  targetShiftName: string | null
  note: string | null
  status: string
}

const TONE: Record<string, 'green' | 'red' | 'amber'> = {
  disetujui: 'green',
  ditolak: 'red',
  diajukan: 'amber',
}
const STATUS = [
  { value: '', label: 'Semua status' },
  { value: 'diajukan', label: 'Diajukan' },
  { value: 'disetujui', label: 'Disetujui' },
  { value: 'ditolak', label: 'Ditolak' },
]

const { auth, companyId } = useCompany()
const access = useAccessStore()
const sekarang = new Date()

const tab = ref('saldo')
const tahun = ref(sekarang.getFullYear())
const okMsg = ref('')
const referensi = ref<Penempatan[]>([])
const saldo = ref<Saldo[]>([])
const filterStatusLembur = ref('')
const filterStatusTukar = ref('')

const { loading: loadingSaldo, error: errorSaldo, run: runSaldo } = useAsync()
const { loading: loadingMutasi, error: errorMutasi, run: runMutasi } = useAsync()

const bisaKelolaSaldo = computed(
  () => Boolean(auth.user?.isSuperAdmin) || access.can('absensi.approve', 'karyawan.kelola'),
)
const bisaSetujuiLembur = computed(
  () => Boolean(auth.user?.isSuperAdmin) || access.can('absensi.approve'),
)
const bisaKelolaTukar = computed(
  () => Boolean(auth.user?.isSuperAdmin) || access.can('shift.kelola'),
)

const opsiKaryawan = computed(() =>
  referensi.value
    .filter((item) => item.userName)
    .map((item) => ({ value: String(item.userId), label: item.userName as string }))
    .sort((a, b) => a.label.localeCompare(b.label)),
)
const opsiRekan = computed(() =>
  opsiKaryawan.value.filter((item) => item.value !== String(auth.user?.id ?? 0)),
)
const opsiTahun = computed(() =>
  [sekarang.getFullYear() - 1, sekarang.getFullYear(), sekarang.getFullYear() + 1].map(
    (year) => ({ value: String(year), label: String(year) }),
  ),
)

const formSaldo = ref({
  user_id: '',
  year: sekarang.getFullYear(),
  entitlement: '12',
  carryover: '0',
})
const formLembur = ref({
  user_id: '',
  date: sekarang.toISOString().slice(0, 10),
  start_time: '',
  end_time: '',
  reason: '',
})
const formTukar = ref({
  target_user_id: '',
  date: sekarang.toISOString().slice(0, 10),
  note: '',
})

const lembur = usePagedList<Lembur>((page, perPage) => {
  const params = new URLSearchParams({ per_page: String(perPage), page: String(page) })
  if (filterStatusLembur.value) params.set('status', filterStatusLembur.value)
  return api.getPage<Lembur>(
    `/companies/${companyId.value}/overtime-requests?${params}`,
    auth.token,
  )
})

const tukar = usePagedList<TukarShift>((page, perPage) => {
  const params = new URLSearchParams({ per_page: String(perPage), page: String(page) })
  if (filterStatusTukar.value) params.set('status', filterStatusTukar.value)
  return api.getPage<TukarShift>(
    `/companies/${companyId.value}/shift-swaps?${params}`,
    auth.token,
  )
})

const error = computed(
  () =>
    errorSaldo.value ||
    errorMutasi.value ||
    lembur.error.value ||
    tukar.error.value,
)
const tabs = computed(() => [
  { key: 'saldo', label: 'Saldo cuti', count: saldo.value.length },
  { key: 'lembur', label: 'Lembur', count: lembur.meta.value?.total ?? lembur.rows.value.length },
  { key: 'tukar', label: 'Tukar shift', count: tukar.meta.value?.total ?? tukar.rows.value.length },
])

watch(
  () => formSaldo.value.user_id,
  (id) => {
    const found = saldo.value.find((row) => String(row.userId) === String(id))
    if (!found) return
    formSaldo.value.entitlement = found.entitlement
    formSaldo.value.carryover = found.carryover
  },
)

watch(tahun, () => void muatSaldo())
watch(filterStatusLembur, () => void lembur.load(1))
watch(filterStatusTukar, () => void tukar.load(1))

async function muatSaldo() {
  const rows = await runSaldo(() =>
    api.get<Saldo[]>(
      `/companies/${companyId.value}/leave-balances?year=${tahun.value}`,
      auth.token,
    ),
  )
  if (rows) saldo.value = rows
}

async function muatReferensi() {
  if (!bisaKelolaSaldo.value && !bisaKelolaTukar.value) return
  const page = await api
    .getPage<Penempatan>(
      `/companies/${companyId.value}/access/assignments?per_page=100&active=1`,
      auth.token,
    )
    .catch(() => null)
  if (page) referensi.value = page.data
}

async function simpanSaldo() {
  if (!formSaldo.value.user_id) return
  const res = await runMutasi(() =>
    api.put<Saldo>(
      `/companies/${companyId.value}/users/${formSaldo.value.user_id}/leave-balance`,
      {
        year: Number(formSaldo.value.year),
        entitlement: Number(formSaldo.value.entitlement),
        carryover: Number(formSaldo.value.carryover || 0),
      },
      auth.token,
    ),
  )
  if (res) {
    okMsg.value = 'Saldo cuti disimpan.'
    await muatSaldo()
  }
}

async function ajukanLembur() {
  if (!formLembur.value.date || !formLembur.value.start_time || !formLembur.value.end_time) return
  if (!formLembur.value.reason.trim()) return
  const payload: Record<string, unknown> = {
    date: formLembur.value.date,
    start_time: formLembur.value.start_time,
    end_time: formLembur.value.end_time,
    reason: formLembur.value.reason.trim(),
  }
  if (bisaSetujuiLembur.value && formLembur.value.user_id) {
    payload.user_id = Number(formLembur.value.user_id)
  }
  const res = await runMutasi(() =>
    api.post(`/companies/${companyId.value}/overtime-requests`, payload, auth.token),
  )
  if (res) {
    okMsg.value = 'Pengajuan lembur dibuat.'
    formLembur.value = { ...formLembur.value, start_time: '', end_time: '', reason: '' }
    await lembur.load(1)
  }
}

async function putuskanLembur(item: Lembur, approve: boolean) {
  const res = await runMutasi(() =>
    api.post(
      `/companies/${companyId.value}/overtime-requests/${item.id}/decide`,
      { approve },
      auth.token,
    ),
  )
  if (res) {
    okMsg.value = approve ? 'Lembur disetujui.' : 'Lembur ditolak.'
    await lembur.reload()
  }
}

async function ajukanTukar() {
  if (!formTukar.value.target_user_id || !formTukar.value.date) return
  const res = await runMutasi(() =>
    api.post(
      `/companies/${companyId.value}/shift-swaps`,
      {
        target_user_id: Number(formTukar.value.target_user_id),
        date: formTukar.value.date,
        note: formTukar.value.note.trim() || null,
      },
      auth.token,
    ),
  )
  if (res) {
    okMsg.value = 'Pengajuan tukar shift dibuat.'
    formTukar.value = { ...formTukar.value, target_user_id: '', note: '' }
    await tukar.load(1)
  }
}

async function putuskanTukar(item: TukarShift, approve: boolean) {
  const res = await runMutasi(() =>
    api.post(
      `/companies/${companyId.value}/shift-swaps/${item.id}/decide`,
      { approve },
      auth.token,
    ),
  )
  if (res) {
    okMsg.value = approve ? 'Tukar shift disetujui.' : 'Tukar shift ditolak.'
    await tukar.reload()
  }
}

onMounted(async () => {
  await access.load(companyId.value, auth.token)
  await Promise.all([muatSaldo(), muatReferensi(), lembur.load(), tukar.load()])
})
</script>

<template>
  <div>
    <AppPageHeader
      eyebrow="Kepegawaian"
      title="Saldo cuti, lembur & tukar shift"
      subtitle="Kelola hak cuti tahunan, pengajuan lembur, dan pertukaran jadwal antar karyawan."
      icon="more_time"
    />

    <AppBanner tone="green" :message="okMsg" />
    <AppBanner tone="red" :message="error" />

    <AppTabs v-model="tab" :items="tabs" label="Bagian kepegawaian" id-prefix="hr-extra-tab" />

    <div v-if="tab === 'saldo'" class="mt-4 grid gap-4 lg:grid-cols-5">
      <div class="min-w-0 lg:col-span-3">
        <AppCard title="Saldo cuti tahunan">
          <div class="mb-3 flex flex-wrap items-end justify-between gap-2">
            <p class="text-xs text-secondary">
              Sisa = hak + carryover − terpakai. Cuti yang disetujui otomatis memotong saldo.
            </p>
            <div class="w-28">
              <AppSelect v-model="tahun" :options="opsiTahun" aria-label="Tahun saldo cuti" />
            </div>
          </div>
          <AppTable
            caption="Saldo cuti karyawan"
            :loading="loadingSaldo"
            :empty="!saldo.length"
            :colspan="6"
            empty-title="Belum ada data saldo"
            empty-message="Saldo default 12 hari akan muncul setelah karyawan aktif."
          >
            <template #head>
              <tr>
                <th>Karyawan</th>
                <th>Hak</th>
                <th>Carryover</th>
                <th>Terpakai</th>
                <th>Sisa</th>
                <th>Tahun</th>
              </tr>
            </template>
            <tr v-for="row in saldo" :key="row.userId">
              <td class="font-semibold text-ink">{{ row.userName }}</td>
              <td class="tabular-nums">{{ row.entitlement }}</td>
              <td class="tabular-nums">{{ row.carryover }}</td>
              <td class="tabular-nums">{{ row.used }}</td>
              <td class="font-semibold tabular-nums text-primary-ink">{{ row.available }}</td>
              <td class="text-secondary">{{ row.year }}</td>
            </tr>
          </AppTable>
        </AppCard>
      </div>

      <div v-if="bisaKelolaSaldo" class="min-w-0 lg:col-span-2">
        <AppCard title="Atur saldo" subtitle="Hanya pengelola absensi/kepegawaian.">
          <form class="space-y-3" @submit.prevent="simpanSaldo">
            <AppField label="Karyawan" required>
              <AppSelect
                v-model="formSaldo.user_id"
                placeholder="Pilih karyawan"
                :options="opsiKaryawan"
              />
            </AppField>
            <div class="grid gap-3 sm:grid-cols-3">
              <AppField label="Tahun" required>
                <AppInput v-model.number="formSaldo.year" type="number" min="2000" max="2100" />
              </AppField>
              <AppField label="Hak (hari)" required>
                <AppInput v-model="formSaldo.entitlement" type="number" min="0" max="365" />
              </AppField>
              <AppField label="Carryover" optional>
                <AppInput v-model="formSaldo.carryover" type="number" min="0" max="365" />
              </AppField>
            </div>
            <AppButton
              type="submit"
              :loading="loadingMutasi"
              :disabled="!formSaldo.user_id"
              block
            >
              Simpan saldo
            </AppButton>
          </form>
        </AppCard>
      </div>
    </div>

    <div v-else-if="tab === 'lembur'" class="mt-4 grid gap-4 lg:grid-cols-5">
      <div class="min-w-0 lg:col-span-3">
        <AppCard title="Pengajuan lembur">
          <div class="mb-3 flex justify-end">
            <div class="w-40">
              <AppSelect
                v-model="filterStatusLembur"
                :options="STATUS"
                aria-label="Filter status lembur"
              />
            </div>
          </div>
          <AppTable
            caption="Pengajuan lembur"
            :loading="lembur.loading.value"
            :empty="!lembur.rows.value.length"
            :colspan="bisaSetujuiLembur ? 6 : 5"
            empty-title="Belum ada pengajuan lembur"
            empty-message="Ajukan lembur dari formulir di samping."
          >
            <template #head>
              <tr>
                <th>Karyawan</th>
                <th>Tanggal</th>
                <th>Jam</th>
                <th>Durasi</th>
                <th>Status</th>
                <th v-if="bisaSetujuiLembur" class="text-right">Keputusan</th>
              </tr>
            </template>
            <tr v-for="row in lembur.rows.value" :key="row.id">
              <td>
                <p class="font-semibold text-ink">{{ row.userName }}</p>
                <p class="max-w-52 truncate text-xs text-secondary" :title="row.reason">
                  {{ row.reason }}
                </p>
              </td>
              <td class="whitespace-nowrap">{{ tanggal(row.date) }}</td>
              <td class="whitespace-nowrap text-sm">{{ row.startTime }}–{{ row.endTime }}</td>
              <td class="whitespace-nowrap text-sm tabular-nums">{{ row.minutes }} menit</td>
              <td>
                <AppBadge :tone="TONE[row.status] ?? 'amber'" dot>
                  {{ labelLeaveStatus(row.status) }}
                </AppBadge>
              </td>
              <td v-if="bisaSetujuiLembur" class="text-right">
                <div v-if="row.status === 'diajukan'" class="flex justify-end gap-1">
                  <AppButton size="sm" variant="ghost" :loading="loadingMutasi" @click="putuskanLembur(row, true)">
                    Setujui
                  </AppButton>
                  <AppButton size="sm" variant="ghost" :disabled="loadingMutasi" @click="putuskanLembur(row, false)">
                    Tolak
                  </AppButton>
                </div>
                <span v-else class="text-xs text-secondary">Selesai</span>
              </td>
            </tr>
          </AppTable>
          <AppPager
            :meta="lembur.meta.value"
            @page="lembur.setPage"
            @update:per-page="lembur.setPerPage"
          />
        </AppCard>
      </div>

      <div class="min-w-0 lg:col-span-2">
        <AppCard title="Ajukan lembur">
          <form class="space-y-3" @submit.prevent="ajukanLembur">
            <AppField v-if="bisaSetujuiLembur" label="Karyawan" optional>
              <AppSelect
                v-model="formLembur.user_id"
                placeholder="Diri sendiri"
                :options="opsiKaryawan"
              />
            </AppField>
            <AppField label="Tanggal" required>
              <AppDateInput v-model="formLembur.date" />
            </AppField>
            <div class="grid gap-3 sm:grid-cols-2">
              <AppField label="Mulai" required>
                <AppInput v-model="formLembur.start_time" type="time" />
              </AppField>
              <AppField label="Selesai" required>
                <AppInput v-model="formLembur.end_time" type="time" />
              </AppField>
            </div>
            <AppField label="Alasan / pekerjaan" required>
              <AppTextarea v-model="formLembur.reason" :rows="3" :maxlength="500" :show-count="true" />
            </AppField>
            <AppButton
              type="submit"
              :loading="loadingMutasi"
              :disabled="!formLembur.start_time || !formLembur.end_time || !formLembur.reason.trim()"
              block
            >
              Kirim pengajuan
            </AppButton>
          </form>
        </AppCard>
      </div>
    </div>

    <div v-else class="mt-4 grid gap-4 lg:grid-cols-5">
      <div class="min-w-0 lg:col-span-3">
        <AppCard title="Pertukaran shift">
          <div class="mb-3 flex justify-end">
            <div class="w-40">
              <AppSelect
                v-model="filterStatusTukar"
                :options="STATUS"
                aria-label="Filter status tukar shift"
              />
            </div>
          </div>
          <AppTable
            caption="Pengajuan tukar shift"
            :loading="tukar.loading.value"
            :empty="!tukar.rows.value.length"
            :colspan="bisaKelolaTukar ? 6 : 5"
            empty-title="Belum ada tukar shift"
            empty-message="Ajukan pertukaran jadwal dari formulir di samping."
          >
            <template #head>
              <tr>
                <th>Tanggal</th>
                <th>Pengaju</th>
                <th>Rekan</th>
                <th>Shift</th>
                <th>Status</th>
                <th v-if="bisaKelolaTukar" class="text-right">Keputusan</th>
              </tr>
            </template>
            <tr v-for="row in tukar.rows.value" :key="row.id">
              <td class="whitespace-nowrap">{{ tanggal(row.date) }}</td>
              <td class="font-semibold text-ink">{{ row.requesterName }}</td>
              <td>{{ row.targetName }}</td>
              <td class="text-xs text-secondary">
                {{ row.requesterShiftName ?? 'Libur' }} ⇄ {{ row.targetShiftName ?? 'Libur' }}
              </td>
              <td>
                <AppBadge :tone="TONE[row.status] ?? 'amber'" dot>
                  {{ labelLeaveStatus(row.status) }}
                </AppBadge>
              </td>
              <td v-if="bisaKelolaTukar" class="text-right">
                <div v-if="row.status === 'diajukan'" class="flex justify-end gap-1">
                  <AppButton size="sm" variant="ghost" :loading="loadingMutasi" @click="putuskanTukar(row, true)">
                    Setujui
                  </AppButton>
                  <AppButton size="sm" variant="ghost" :disabled="loadingMutasi" @click="putuskanTukar(row, false)">
                    Tolak
                  </AppButton>
                </div>
                <span v-else class="text-xs text-secondary">Selesai</span>
              </td>
            </tr>
          </AppTable>
          <AppPager
            :meta="tukar.meta.value"
            @page="tukar.setPage"
            @update:per-page="tukar.setPerPage"
          />
        </AppCard>
      </div>

      <div class="min-w-0 lg:col-span-2">
        <AppCard title="Ajukan tukar shift">
          <form class="space-y-3" @submit.prevent="ajukanTukar">
            <AppField label="Tukar dengan" required>
              <AppSelect
                v-model="formTukar.target_user_id"
                placeholder="Pilih rekan"
                :options="opsiRekan"
              />
            </AppField>
            <AppField label="Tanggal" required>
              <AppDateInput v-model="formTukar.date" />
            </AppField>
            <AppField label="Catatan" optional>
              <AppTextarea v-model="formTukar.note" :rows="3" :maxlength="500" />
            </AppField>
            <p class="rounded-xl bg-surface-low p-3 text-xs leading-relaxed text-secondary">
              Shift yang berlaku pada tanggal tersebut akan ditukar otomatis setelah disetujui.
            </p>
            <AppButton
              type="submit"
              :loading="loadingMutasi"
              :disabled="!formTukar.target_user_id || !formTukar.date"
              block
            >
              Kirim pengajuan
            </AppButton>
          </form>
        </AppCard>
      </div>
    </div>
  </div>
</template>
