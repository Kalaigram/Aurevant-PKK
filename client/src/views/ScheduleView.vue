<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { api } from '../lib/api'
import { tanggal } from '../lib/format'
import { useCompany } from '../composables/useCompany'
import { useAccessStore } from '../stores/access'
import { useAsync } from '../composables/useAsync'
import { useConfirm } from '../composables/useConfirm'
import AppBanner from '../components/ui/AppBanner.vue'
import AppButton from '../components/ui/AppButton.vue'
import AppCard from '../components/ui/AppCard.vue'
import AppDateInput from '../components/ui/AppDateInput.vue'
import AppEmpty from '../components/ui/AppEmpty.vue'
import AppField from '../components/ui/AppField.vue'
import AppIcon from '../components/ui/AppIcon.vue'
import AppInput from '../components/ui/AppInput.vue'
import AppModal from '../components/ui/AppModal.vue'
import AppPageHeader from '../components/ui/AppPageHeader.vue'
import AppSelect from '../components/ui/AppSelect.vue'
import AppSkeleton from '../components/ui/AppSkeleton.vue'
import AppTable from '../components/ui/AppTable.vue'
import AppTabs from '../components/ui/AppTabs.vue'

interface Shift {
  id: number
  name: string
  starts_at: string
  ends_at: string
  late_tolerance_minutes?: number
}
interface Pola {
  weekday: number
  shift_id: number | null
}
interface Libur {
  id: number
  date: string
  name: string
}
interface ScheduleDay {
  date: string
  shiftId: number | null
  shiftName: string | null
  startsAt: string | null
  endsAt: string | null
  overnight: boolean
  source: string
  isOff: boolean
}
interface TimRow {
  userId: number
  name: string
  role: string | null
  days: ScheduleDay[]
}

const HARI = ['Min', 'Sen', 'Sel', 'Rab', 'Kam', 'Jum', 'Sab']
const tab = ref<'jadwal' | 'shift' | 'pola' | 'libur'>('jadwal')

const { auth, companyId } = useCompany()
const access = useAccessStore()
const { loading, error, run } = useAsync()
const { confirm } = useConfirm()
const shifts = ref<Shift[]>([])
const libur = ref<Libur[]>([])
const okMsg = ref('')
const jadwalSaya = ref<ScheduleDay[]>([])
const tim = ref<TimRow[]>([])
const memuatJadwal = ref(false)

const bolehLihat = computed(() => access.can('jadwal.lihat'))
const bolehKelolaShift = computed(() => access.can('shift.kelola'))
const bolehLibur = computed(() => access.can('pengaturan.kelola'))

function iso(date: Date) {
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}
function awalMinggu(base = new Date()) {
  const date = new Date(base.getFullYear(), base.getMonth(), base.getDate())
  const day = date.getDay()
  date.setDate(date.getDate() + (day === 0 ? -6 : 1 - day))
  return date
}
function geserMinggu(offset: number) {
  const next = new Date(`${mingguMulai.value}T00:00:00`)
  next.setDate(next.getDate() + offset * 7)
  mingguMulai.value = iso(next)
  muatJadwal()
}

const mingguMulai = ref(iso(awalMinggu()))
const mingguSelesai = computed(() => {
  const d = new Date(`${mingguMulai.value}T00:00:00`)
  d.setDate(d.getDate() + 6)
  return iso(d)
})
const tanggalMinggu = computed(() => {
  const base = new Date(`${mingguMulai.value}T00:00:00`)
  return Array.from({ length: 7 }, (_, i) => {
    const d = new Date(base)
    d.setDate(d.getDate() + i)
    return iso(d)
  })
})
const labelMinggu = computed(() => {
  const a = new Date(`${mingguMulai.value}T00:00:00`)
  const b = new Date(`${mingguSelesai.value}T00:00:00`)
  const opsi: Intl.DateTimeFormatOptions = { day: 'numeric', month: 'short' }
  return `${a.toLocaleDateString('id-ID', opsi)} — ${b.toLocaleDateString('id-ID', { ...opsi, year: 'numeric' })}`
})
const hariPendek = (date: string) =>
  new Date(`${date}T00:00:00`).toLocaleDateString('id-ID', { weekday: 'short' })
const tanggalPendek = (date: string) =>
  new Date(`${date}T00:00:00`).toLocaleDateString('id-ID', { day: '2-digit', month: 'short' })

async function muatJadwal() {
  if (!bolehLihat.value) return
  memuatJadwal.value = true
  try {
    if (auth.user) {
      jadwalSaya.value =
        (await api.get<ScheduleDay[]>(
          `/companies/${companyId.value}/users/${auth.user.id}/schedule?from=${mingguMulai.value}&to=${mingguSelesai.value}`,
          auth.token,
        )) ?? []
    }
    tim.value =
      (await api.get<TimRow[]>(
        `/companies/${companyId.value}/schedule/team?from=${mingguMulai.value}&to=${mingguSelesai.value}`,
        auth.token,
      )) ?? []
  } catch {
    tim.value = []
  } finally {
    memuatJadwal.value = false
  }
}

const showShift = ref(false)
const sunting = ref<Shift | null>(null)
const fShift = ref({
  name: '',
  starts_at: '08:00',
  ends_at: '17:00',
  late_tolerance_minutes: 15,
})

const idKaryawan = ref('')
const pola = ref<(number | null)[]>([null, null, null, null, null, null, null])
const polaAda = ref(false)
const ovTanggal = ref('')
const ovShift = ref('')
const ovCatatan = ref('')
const fLiburTanggal = ref('')
const fLiburNama = ref('')

async function muatShift() {
  const res = await run(() =>
    api.getPage<Shift>(`/companies/${companyId.value}/work-shifts?per_page=100`, auth.token),
  )
  if (res) {
    const raw = res as unknown as { data: Shift[] } | Shift[]
    shifts.value = Array.isArray(raw) ? raw : raw.data
  }
}

function bukaTambah() {
  sunting.value = null
  fShift.value = { name: '', starts_at: '08:00', ends_at: '17:00', late_tolerance_minutes: 15 }
  showShift.value = true
}

function bukaUbah(s: Shift) {
  sunting.value = s
  fShift.value = {
    name: s.name,
    starts_at: s.starts_at,
    ends_at: s.ends_at,
    late_tolerance_minutes: s.late_tolerance_minutes ?? 15,
  }
  showShift.value = true
}

async function simpanShift() {
  const payload = {
    name: fShift.value.name.trim(),
    starts_at: fShift.value.starts_at,
    ends_at: fShift.value.ends_at,
    late_tolerance_minutes: Number(fShift.value.late_tolerance_minutes),
  }
  const base = `/companies/${companyId.value}/work-shifts`
  const res = sunting.value
    ? await run(() => api.put(base + `/${sunting.value!.id}`, payload, auth.token))
    : await run(() => api.post(base, payload, auth.token))
  if (res) {
    showShift.value = false
    okMsg.value = sunting.value ? 'Shift diperbarui.' : 'Shift baru dibuat.'
    await muatShift()
    await muatJadwal()
  }
}

async function hapusShift(s: Shift) {
  const setuju = await confirm({
    title: `Hapus shift ${s.name}?`,
    message: 'Pola yang memakai shift ini perlu diatur ulang.',
    confirmLabel: 'Hapus shift',
    tone: 'danger',
  })
  if (!setuju) return
  await run(() => api.del(`/companies/${companyId.value}/work-shifts/${s.id}`, auth.token))
  if (!error.value) {
    okMsg.value = 'Shift dihapus.'
    await muatShift()
    await muatJadwal()
  }
}

async function muatPola() {
  if (!idKaryawan.value) return
  okMsg.value = ''
  const res = await run(() =>
    api.get<Pola[]>(
      `/companies/${companyId.value}/users/${idKaryawan.value}/schedule-pattern`,
      auth.token,
    ),
  )
  if (res) {
    const arr: (number | null)[] = [null, null, null, null, null, null, null]
    for (const p of res as Pola[]) arr[p.weekday] = p.shift_id
    pola.value = arr
    polaAda.value = true
  }
}

async function simpanPola() {
  if (!idKaryawan.value) return
  const res = await run(() =>
    api.put(
      `/companies/${companyId.value}/users/${idKaryawan.value}/schedule-pattern`,
      { patterns: pola.value },
      auth.token,
    ),
  )
  if (res) okMsg.value = 'Pola mingguan tersimpan.'
}

async function simpanOverride() {
  if (!idKaryawan.value || !ovTanggal.value) return
  const res = await run(() =>
    api.post(
      `/companies/${companyId.value}/users/${idKaryawan.value}/schedule-overrides`,
      {
        date: ovTanggal.value,
        work_shift_id: ovShift.value === '' ? null : Number(ovShift.value),
        note: ovCatatan.value || undefined,
      },
      auth.token,
    ),
  )
  if (res) {
    okMsg.value = ovShift.value === '' ? 'Hari itu diliburkan.' : 'Jadwal tanggal itu diubah.'
    ovTanggal.value = ''
    ovCatatan.value = ''
    await muatJadwal()
  }
}

async function hapusOverride() {
  if (!idKaryawan.value || !ovTanggal.value) return
  const res = await run(() =>
    api.delBody(
      `/companies/${companyId.value}/users/${idKaryawan.value}/schedule-overrides`,
      { date: ovTanggal.value },
      auth.token,
    ),
  )
  if (res || !error.value) {
    okMsg.value = 'Jadwal khusus tanggal itu dihapus.'
    ovTanggal.value = ''
    await muatJadwal()
  }
}

function cetak() {
  window.print()
}

async function muatLibur() {
  const res = await run(() =>
    api.get<Libur[]>(`/companies/${companyId.value}/holidays`, auth.token),
  )
  if (res) {
    const raw = res as unknown as { data: Libur[] } | Libur[]
    libur.value = Array.isArray(raw) ? raw : (raw.data ?? [])
  }
}

async function tambahLibur() {
  const res = await run(() =>
    api.post(
      `/companies/${companyId.value}/holidays`,
      { date: fLiburTanggal.value, name: fLiburNama.value.trim() },
      auth.token,
    ),
  )
  if (res) {
    okMsg.value = 'Hari libur ditambahkan.'
    fLiburTanggal.value = ''
    fLiburNama.value = ''
    await muatLibur()
    await muatJadwal()
  }
}

async function hapusLibur(item: Libur) {
  const setuju = await confirm({
    title: `Hapus ${item.name}?`,
    message: `Hari libur ${tanggal(item.date)} akan dihapus.`,
    confirmLabel: 'Hapus',
    tone: 'danger',
  })
  if (!setuju) return
  await run(() => api.del(`/companies/${companyId.value}/holidays/${item.id}`, auth.token))
  if (!error.value) {
    okMsg.value = 'Hari libur dihapus.'
    await muatLibur()
    await muatJadwal()
  }
}

onMounted(async () => {
  await access.load(companyId.value, auth.token)
  await muatShift()
  await muatLibur()
  await muatJadwal()
})
</script>

<template>
  <div>
    <AppPageHeader
      eyebrow="Waktu kerja karyawan"
      title="Jadwal & shift"
      subtitle="Jadwal mingguan Anda dan tim, pola kerja, shift, serta hari libur."
      icon="calendar_month"
    >
      <template #actions>
        <AppButton v-if="tab === 'jadwal'" class="no-print" variant="ghost" @click="cetak">
          <AppIcon name="print" :size="18" />Cetak
        </AppButton>
      </template>
    </AppPageHeader>

    <AppBanner tone="green" :message="okMsg" />
    <AppBanner tone="red" :message="error" />

    <AppTabs
      v-model="tab"
      class="no-print"
      :items="[
        { key: 'jadwal', label: 'Jadwal' },
        { key: 'shift', label: 'Shift' },
        { key: 'pola', label: 'Pola & roster' },
        { key: 'libur', label: 'Hari libur' },
      ]"
      label="Jadwal dan shift"
      id-prefix="schedule-tab"
      panel-prefix="schedule-panel"
    />

    <div
      v-if="tab === 'jadwal'"
      id="schedule-panel-jadwal"
      role="tabpanel"
      aria-labelledby="schedule-tab-jadwal"
      class="mt-4"
    >
      <div class="no-print mb-3 flex flex-wrap items-center justify-between gap-2">
        <div class="flex flex-wrap items-center gap-1">
          <AppButton
            variant="ghost"
            aria-label="Minggu sebelumnya"
            @click="geserMinggu(-1)"
          >
            <AppIcon name="chevron_left" :size="18" />
          </AppButton>
          <p class="min-w-[190px] text-center text-sm font-semibold text-ink">{{ labelMinggu }}</p>
          <AppButton
            variant="ghost"
            aria-label="Minggu berikutnya"
            @click="geserMinggu(1)"
          >
            <AppIcon name="chevron_right" :size="18" />
          </AppButton>
          <AppButton variant="ghost" @click="geserMinggu(0)">Minggu ini</AppButton>
        </div>
        <p class="text-xs text-secondary">Minggu dimulai Senin.</p>
      </div>

      <div class="print-only mb-4 border-b border-outline pb-3">
        <p class="font-display text-lg font-bold">{{ auth.companyName ?? 'Aurevant' }}</p>
        <p class="text-sm">Jadwal kerja {{ labelMinggu }}</p>
      </div>

      <AppEmpty
        v-if="!bolehLihat"
        message="Anda tidak punya akses melihat jadwal. Hubungi HR bila perlu."
      />
      <template v-else>
        <AppCard>
          <h2 class="font-display font-semibold">Jadwal saya</h2>
          <div v-if="memuatJadwal" class="mt-3 grid grid-cols-2 gap-2 sm:grid-cols-4 lg:grid-cols-7">
            <AppSkeleton
              v-for="hari in 7"
              :key="`skeleton-jadwal-${hari}`"
              height="5.5rem"
              rounded="lg"
            />
          </div>
          <div v-else class="mt-3 grid grid-cols-2 gap-2 sm:grid-cols-4 lg:grid-cols-7">
            <div
              v-for="d in jadwalSaya"
              :key="d.date"
              class="rounded-lg border p-2 text-center"
              :class="
                !d.isOff && d.shiftName
                  ? 'border-primary/20 bg-primary-soft/50'
                  : 'border-outline/60 bg-surface-low'
              "
            >
              <p class="text-[11px] font-semibold uppercase tracking-wide text-secondary">
                {{ hariPendek(d.date) }}
              </p>
              <p class="text-sm font-semibold text-ink">{{ tanggalPendek(d.date) }}</p>
              <template v-if="!d.isOff && d.shiftName">
                <p class="mt-1 text-sm font-semibold text-primary">{{ d.shiftName }}</p>
                <p class="text-[11px] text-secondary">{{ d.startsAt }}–{{ d.endsAt }}</p>
              </template>
              <p v-else class="mt-1 text-sm text-secondary">Libur</p>
            </div>
            <p v-if="!jadwalSaya.length" class="col-span-full text-sm text-secondary">
              Belum ada pola jadwal untuk akun Anda.
            </p>
          </div>
        </AppCard>

        <AppCard class="mt-4">
          <div class="flex flex-wrap items-center justify-between gap-2">
            <h2 class="font-display font-semibold">Jadwal tim</h2>
            <p class="text-xs text-secondary">{{ tim.length }} karyawan aktif</p>
          </div>
          <AppTable
            class="mt-3"
            caption="Jadwal tim minggu ini"
            :loading="memuatJadwal"
            :empty="!tim.length"
            :colspan="8"
            empty-title="Belum ada jadwal tim"
            empty-message="Belum ada karyawan aktif dengan penempatan."
          >
            <template #head>
              <tr>
                <th class="sticky left-0 z-10 bg-card">Karyawan</th>
                <th v-for="d in tanggalMinggu" :key="d" class="min-w-[92px] text-center">
                  <span class="block font-semibold">{{ hariPendek(d) }}</span>
                  <span class="block text-[10px] font-normal text-secondary">{{ tanggalPendek(d) }}</span>
                </th>
              </tr>
            </template>
            <tr v-for="row in tim" :key="row.userId">
              <td class="sticky left-0 z-10 min-w-[160px] bg-card">
                <p class="font-medium">{{ row.name }}</p>
                <p v-if="row.role" class="text-xs text-secondary">{{ row.role }}</p>
              </td>
              <td v-for="day in tanggalMinggu" :key="`${row.userId}-${day}`" class="text-center">
                <template v-for="d in row.days.filter((x) => x.date === day)" :key="d.date">
                  <div
                    v-if="!d.isOff && d.shiftName"
                    class="rounded-md bg-primary-soft px-1.5 py-1 text-center"
                  >
                    <p class="text-xs font-semibold text-primary">{{ d.shiftName }}</p>
                    <p class="text-[10px] text-secondary">{{ d.startsAt }}–{{ d.endsAt }}</p>
                  </div>
                  <span v-else class="text-xs text-secondary">—</span>
                </template>
              </td>
            </tr>
          </AppTable>
        </AppCard>
      </template>
    </div>

    <div
      v-if="tab === 'shift'"
      id="schedule-panel-shift"
      role="tabpanel"
      aria-labelledby="schedule-tab-shift"
      class="mt-4"
    >
      <div class="mb-3 flex flex-wrap items-center justify-between gap-2">
        <p class="text-sm text-secondary">{{ shifts.length }} jenis shift. Jam format 24 jam.</p>
        <AppButton v-if="bolehKelolaShift" @click="bukaTambah">
          <AppIcon name="add" :size="18" />Tambah shift
        </AppButton>
      </div>
      <div
        v-if="loading && !shifts.length"
        class="grid gap-3 md:grid-cols-2"
      >
        <AppSkeleton
          v-for="item in 2"
          :key="`skeleton-shift-${item}`"
          height="6.5rem"
          rounded="xl"
        />
      </div>
      <div v-else class="grid gap-3 md:grid-cols-2">
        <AppCard v-for="s in shifts" :key="s.id">
          <div class="flex items-center justify-between gap-2">
            <h2 class="font-display font-semibold">{{ s.name }}</h2>
            <span v-if="bolehKelolaShift" class="flex gap-1">
              <AppButton variant="ghost" @click="bukaUbah(s)">Ubah</AppButton>
              <AppButton variant="ghost" @click="hapusShift(s)">Hapus</AppButton>
            </span>
          </div>
          <p class="mt-1 font-mono text-sm">{{ s.starts_at }} — {{ s.ends_at }}</p>
          <p class="text-xs text-secondary">
            Toleransi telat: {{ s.late_tolerance_minutes ?? 0 }} menit
          </p>
        </AppCard>
      </div>
      <AppEmpty
        v-if="!loading && !shifts.length"
        title="Belum ada shift"
        message="Buat shift pagi, sore, atau malam sebelum menyusun pola kerja."
      />
    </div>

    <div
      v-if="tab === 'pola'"
      id="schedule-panel-pola"
      role="tabpanel"
      aria-labelledby="schedule-tab-pola"
      class="mt-4 grid gap-3 lg:grid-cols-2"
    >
      <AppCard>
        <h2 class="font-display font-semibold">Pola mingguan karyawan</h2>
        <p class="text-xs text-secondary">Atur sekali untuk yang jamnya tetap tiap minggu.</p>
        <AppField label="ID karyawan (lihat di Karyawan & Penempatan)" class="mt-2">
          <div class="flex gap-2">
            <AppInput v-model="idKaryawan" inputmode="numeric" placeholder="cth. 12" />
            <AppButton variant="ghost" @click="muatPola">Muat</AppButton>
          </div>
        </AppField>
        <div v-if="polaAda" class="mt-3 grid grid-cols-2 gap-2 sm:grid-cols-4 xl:grid-cols-7">
          <label
            v-for="(h, i) in HARI"
            :key="h"
            class="rounded-xl border border-outline/60 bg-surface-low p-2"
          >
            <span class="text-xs font-semibold text-ink">{{ h }}</span>
            <select
              v-model="pola[i]"
              :disabled="!bolehKelolaShift"
              :aria-label="`Shift hari ${h}`"
              class="mt-1 min-h-9 w-full rounded-lg border border-outline bg-card px-2 py-1 text-xs text-ink transition focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/55 disabled:cursor-not-allowed disabled:text-secondary"
            >
              <option :value="null">Libur</option>
              <option v-for="s in shifts" :key="s.id" :value="s.id">{{ s.name }}</option>
            </select>
          </label>
        </div>
        <p v-else class="mt-3 text-sm text-secondary">
          Pilih karyawan lalu klik Muat untuk menampilkan pola mingguan.
        </p>
        <AppButton v-if="polaAda && bolehKelolaShift" class="mt-3" @click="simpanPola">
          Simpan pola
        </AppButton>
      </AppCard>

      <AppCard>
        <h2 class="font-display font-semibold">Ubah jadwal tanggal tertentu</h2>
        <p class="text-xs text-secondary">Untuk lembur, tukar hari, atau liburkan sehari.</p>
        <div class="mt-2 grid gap-2 sm:grid-cols-2">
          <AppField label="Tanggal">
            <AppDateInput v-model="ovTanggal" />
          </AppField>
          <AppField label="Shift pengganti">
            <AppSelect v-model="ovShift" placeholder="Liburkan hari itu">
              <option value="">Liburkan hari itu</option>
              <option v-for="s in shifts" :key="s.id" :value="s.id">{{ s.name }}</option>
            </AppSelect>
          </AppField>
        </div>
        <AppField label="Catatan (mis. alasan lembur)" class="mt-2">
          <AppInput v-model="ovCatatan" maxlength="500" placeholder="Opsional" />
        </AppField>
        <div v-if="bolehKelolaShift" class="mt-2 flex gap-2">
          <AppButton @click="simpanOverride">Simpan</AppButton>
          <AppButton variant="ghost" @click="hapusOverride">Hapus jadwal khusus</AppButton>
        </div>
      </AppCard>
    </div>

    <div
      v-if="tab === 'libur'"
      id="schedule-panel-libur"
      role="tabpanel"
      aria-labelledby="schedule-tab-libur"
      class="mt-4"
    >
      <AppCard>
        <div class="flex flex-wrap items-center justify-between gap-2">
          <div>
            <h2 class="font-display font-semibold">Hari libur perusahaan</h2>
            <p class="mt-1 text-xs text-secondary">{{ libur.length }} hari terdaftar.</p>
          </div>
        </div>
        <div v-if="bolehLibur" class="mt-3 grid gap-2 sm:grid-cols-[1fr_2fr_auto] sm:items-end">
          <AppField label="Tanggal">
            <AppDateInput v-model="fLiburTanggal" />
          </AppField>
          <AppField label="Nama hari libur">
            <AppInput v-model="fLiburNama" maxlength="255" placeholder="cth. Libur tahun baru" />
          </AppField>
          <AppButton :disabled="!fLiburTanggal || !fLiburNama.trim()" @click="tambahLibur">
            Tambah
          </AppButton>
        </div>
        <div class="mt-4">
          <AppTable
            caption="Hari libur perusahaan"
            :empty="!libur.length"
            :colspan="bolehLibur ? 3 : 2"
            empty-title="Belum ada hari libur"
            empty-message="Tambahkan hari libur perusahaan agar jadwal dan gaji tidak salah hitung."
          >
            <template #head>
              <tr>
                <th>Tanggal</th>
                <th>Nama</th>
                <th v-if="bolehLibur" class="text-right">Aksi</th>
              </tr>
            </template>
            <tr v-for="l in libur" :key="l.id">
              <td class="whitespace-nowrap font-semibold text-ink">{{ tanggal(l.date) }}</td>
              <td>{{ l.name }}</td>
              <td v-if="bolehLibur" class="text-right">
                <AppButton size="sm" variant="ghost" @click="hapusLibur(l)">Hapus</AppButton>
              </td>
            </tr>
          </AppTable>
        </div>
      </AppCard>
    </div>

    <AppModal
      :open="showShift"
      :title="sunting ? 'Ubah shift' : 'Tambah shift'"
      description="Jam memakai format 24 jam."
      size="md"
      @close="showShift = false"
    >
      <form id="form-shift" class="space-y-3" @submit.prevent="simpanShift">
        <AppField label="Nama shift (wajib)">
          <AppInput v-model="fShift.name" maxlength="255" placeholder="cth. Pagi" />
        </AppField>
        <div class="grid grid-cols-2 gap-2">
          <AppField label="Mulai (jam:menit)">
            <AppInput v-model="fShift.starts_at" type="time" />
          </AppField>
          <AppField label="Selesai (jam:menit)">
            <AppInput v-model="fShift.ends_at" type="time" />
          </AppField>
        </div>
        <AppField label="Toleransi telat (menit, 0–600)">
          <AppInput v-model.number="fShift.late_tolerance_minutes" type="number" />
        </AppField>
      </form>
      <template #footer>
        <AppButton variant="ghost" @click="showShift = false">Batal</AppButton>
        <AppButton type="submit" form="form-shift" :loading="loading">Simpan</AppButton>
      </template>
    </AppModal>
  </div>
</template>
