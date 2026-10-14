<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { api, type PageMeta } from '../lib/api'
import { tanggal } from '../lib/format'
import { useCompany } from '../composables/useCompany'
import { useAccessStore } from '../stores/access'
import { useAsync } from '../composables/useAsync'
import AppCard from '../components/ui/AppCard.vue'
import AppBanner from '../components/ui/AppBanner.vue'
import AppButton from '../components/ui/AppButton.vue'
import AppField from '../components/ui/AppField.vue'
import AppEmpty from '../components/ui/AppEmpty.vue'
import AppIcon from '../components/ui/AppIcon.vue'

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

const HARI = ['Min', 'Sen', 'Sel', 'Rab', 'Kam', 'Jum', 'Sab']

const { auth, companyId } = useCompany()
const access = useAccessStore()
const { loading, error, run } = useAsync()
const tab = ref<'shift' | 'pola' | 'libur'>('shift')
const shifts = ref<Shift[]>([])
const okMsg = ref('')

const bolehLihat = computed(() => access.can('jadwal.lihat'))
const bolehKelolaShift = computed(() => access.can('shift.kelola'))
const bolehLibur = computed(() => access.can('pengaturan.kelola'))

const showShift = ref(false)
const sunting = ref<Shift | null>(null)
const fShift = ref({ name: '', starts_at: '08:00', ends_at: '17:00', late_tolerance_minutes: 15 })
const tanyaHapus = ref<Shift | null>(null)

const idKaryawan = ref('')
const pola = ref<(number | null)[]>([null, null, null, null, null, null, null])
const polaAda = ref(false)
const ovTanggal = ref('')
const ovShift = ref('')
const ovCatatan = ref('')
const jadwalDari = ref('')
const jadwalSampai = ref('')
const jadwalHasil = ref<unknown[]>([])

const libur = ref<Libur[]>([])

async function muatShift() {
  const res = await run(() =>
    api.get<{ data: Shift[]; meta: PageMeta }>(
      `/companies/${companyId.value}/work-shifts?per_page=100`,
      auth.token,
    ),
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
    muatShift()
  }
}

async function hapusShift() {
  if (!tanyaHapus.value) return
  await run(() =>
    api.del(`/companies/${companyId.value}/work-shifts/${tanyaHapus.value!.id}`, auth.token),
  )
  if (!error.value) {
    tanyaHapus.value = null
    okMsg.value = 'Shift dihapus.'
    muatShift()
  }
}

async function muatPola() {
  if (!idKaryawan.value) return
  okMsg.value = ''
  jadwalHasil.value = []
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
    okMsg.value = ovShift.value === '' ? 'Hari itu dil liburkan.' : 'Jadwal tanggal itu diubah.'
    ovTanggal.value = ''
    ovCatatan.value = ''
  }
}

async function hapusOverride() {
  if (!idKaryawan.value || !ovTanggal.value) return
  const res = await run(() =>
    api.delBody(
      `/companies/${companyId.value}/users/${idKaryawan.value}/schedule-overrides`,
      {
        date: ovTanggal.value,
      },
      auth.token,
    ),
  )
  if (res || !error.value) {
    okMsg.value = 'Jadwal khusus tanggal itu dihapus.'
    ovTanggal.value = ''
  }
}

async function lihatJadwal() {
  if (!idKaryawan.value || !jadwalDari.value || !jadwalSampai.value) return
  const res = await run(() =>
    api.get<unknown[]>(
      `/companies/${companyId.value}/users/${idKaryawan.value}/schedule?from=${jadwalDari.value}&to=${jadwalSampai.value}`,
      auth.token,
    ),
  )
  if (res) jadwalHasil.value = res as unknown[]
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

const fLiburTanggal = ref('')
const fLiburNama = ref('')

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
    muatLibur()
  }
}

async function hapusLibur(id: number) {
  await run(() => api.del(`/companies/${companyId.value}/holidays/${id}`, auth.token))
  if (!error.value) {
    okMsg.value = 'Hari libur dihapus.'
    muatLibur()
  }
}

onMounted(async () => {
  await access.load(companyId.value, auth.token)
  await muatShift()
  await muatLibur()
})
</script>

<template>
  <div>
    <p class="text-xs font-semibold uppercase tracking-wider text-secondary">
      Waktu kerja karyawan
    </p>
    <h1 class="font-display mb-4 text-xl font-semibold">Jadwal & shift</h1>
    <AppBanner tone="green" :message="okMsg" />
    <AppBanner tone="red" :message="error" />

    <div class="mb-3 flex gap-2">
      <AppButton :variant="tab === 'shift' ? 'primary' : 'ghost'" @click="tab = 'shift'"
        >Shift</AppButton
      >
      <AppButton :variant="tab === 'pola' ? 'primary' : 'ghost'" @click="tab = 'pola'"
        >Pola & roster</AppButton
      >
      <AppButton :variant="tab === 'libur' ? 'primary' : 'ghost'" @click="tab = 'libur'"
        >Hari libur</AppButton
      >
    </div>

    <div v-if="tab === 'shift'">
      <div class="mb-3 flex items-center justify-between">
        <p class="text-sm text-secondary">{{ shifts.length }} jenis shift. Jam format 24 jam.</p>
        <AppButton v-if="bolehKelolaShift" @click="bukaTambah"
          ><AppIcon name="add" :size="18" />Tambah shift</AppButton
        >
      </div>
      <p v-if="loading" class="text-sm text-secondary">Memuat…</p>
      <div class="grid gap-3 md:grid-cols-2">
        <AppCard v-for="s in shifts" :key="s.id">
          <div class="flex items-center justify-between gap-2">
            <h2 class="font-display font-semibold">{{ s.name }}</h2>
            <span v-if="bolehKelolaShift" class="flex gap-1">
              <AppButton variant="ghost" @click="bukaUbah(s)">Ubah</AppButton>
              <AppButton variant="ghost" @click="tanyaHapus = s">Hapus</AppButton>
            </span>
          </div>
          <p class="mt-1 font-mono text-sm">{{ s.starts_at }} – {{ s.ends_at }}</p>
          <p class="text-xs text-secondary">
            Toleransi telat: {{ s.late_tolerance_minutes ?? 0 }} menit
          </p>
        </AppCard>
      </div>
      <AppEmpty
        v-if="!loading && !shifts.length"
        message="Belum ada shift. Buat shift pagi/sore/malam dulu."
      />
    </div>

    <div v-if="tab === 'pola'" class="grid gap-3 lg:grid-cols-2">
      <AppCard>
        <h2 class="font-display font-semibold">Pola mingguan karyawan</h2>
        <p class="text-xs text-secondary">Atur sekali untuk yang jamnya tetap tiap minggu.</p>
        <AppField label="ID karyawan (lihat di Karyawan & Penempatan)" class="mt-2">
          <div class="flex gap-2">
            <input
              v-model="idKaryawan"
              inputmode="numeric"
              class="w-full rounded-lg bg-surface-low px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-primary"
              placeholder="cth. 12"
            />
            <AppButton variant="ghost" @click="muatPola">Muat</AppButton>
          </div>
        </AppField>
        <div v-if="polaAda" class="mt-3 grid grid-cols-7 gap-1">
          <div v-for="(h, i) in HARI" :key="h" class="rounded-lg bg-surface-low p-1.5 text-center">
            <p class="text-xs font-semibold">{{ h }}</p>
            <select
              v-model="pola[i]"
              :disabled="!bolehKelolaShift"
              class="mt-1 w-full rounded bg-card px-1 py-1 text-xs"
            >
              <option :value="null">Libur</option>
              <option v-for="s in shifts" :key="s.id" :value="s.id">{{ s.name }}</option>
            </select>
          </div>
        </div>
        <AppButton v-if="polaAda && bolehKelolaShift" class="mt-3" @click="simpanPola"
          >Simpan pola</AppButton
        >
      </AppCard>

      <div class="space-y-3">
        <AppCard>
          <h2 class="font-display font-semibold">Ubah jadwal tanggal tertentu</h2>
          <p class="text-xs text-secondary">Untuk lembur, tukar hari, atau liburkan sehari.</p>
          <div class="mt-2 grid grid-cols-2 gap-2">
            <AppField label="Tanggal"
              ><input
                v-model="ovTanggal"
                type="date"
                class="w-full rounded-lg bg-surface-low px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-primary"
            /></AppField>
            <AppField label="Shift pengganti"
              ><select
                v-model="ovShift"
                class="w-full rounded-lg bg-surface-low px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-primary"
              >
                <option value="">Liburkan hari itu</option>
                <option v-for="s in shifts" :key="s.id" :value="s.id">{{ s.name }}</option>
              </select></AppField
            >
          </div>
          <AppField label="Catatan (mis. alasan lembur)"
            ><input
              v-model="ovCatatan"
              maxlength="500"
              class="w-full rounded-lg bg-surface-low px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-primary"
              placeholder="Opsional"
          /></AppField>
          <div v-if="bolehKelolaShift" class="mt-2 flex gap-2">
            <AppButton @click="simpanOverride">Simpan</AppButton>
            <AppButton variant="ghost" @click="hapusOverride">Hapus jadwal khusus</AppButton>
          </div>
        </AppCard>

        <AppCard v-if="bolehLihat">
          <h2 class="font-display font-semibold">Lihat jadwal efektif</h2>
          <div class="mt-2 grid grid-cols-2 gap-2">
            <AppField label="Dari"
              ><input
                v-model="jadwalDari"
                type="date"
                class="w-full rounded-lg bg-surface-low px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-primary"
            /></AppField>
            <AppField label="Sampai"
              ><input
                v-model="jadwalSampai"
                type="date"
                class="w-full rounded-lg bg-surface-low px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-primary"
            /></AppField>
          </div>
          <AppButton class="mt-2" variant="ghost" @click="lihatJadwal">Tampilkan</AppButton>
          <ul class="mt-2 space-y-1 text-sm">
            <li
              v-for="(j, i) in jadwalHasil"
              :key="i"
              class="rounded bg-surface-low px-2 py-1 font-mono text-xs"
            >
              {{ JSON.stringify(j) }}
            </li>
          </ul>
          <p class="mt-1 text-xs text-secondary">
            Urutan berlaku: hari libur &gt; jadwal khusus &gt; pola mingguan.
          </p>
        </AppCard>
      </div>
    </div>

    <div v-if="tab === 'libur'">
      <AppCard>
        <div class="flex items-center justify-between">
          <h2 class="font-display font-semibold">Hari libur perusahaan</h2>
          <span class="text-xs text-secondary">{{ libur.length }} hari</span>
        </div>
        <div v-if="bolehLibur" class="mt-2 grid grid-cols-[1fr_2fr_auto] gap-2">
          <input
            v-model="fLiburTanggal"
            type="date"
            class="rounded-lg bg-surface-low px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-primary"
          />
          <input
            v-model="fLiburNama"
            maxlength="255"
            placeholder="cth. Libur tahun baru"
            class="rounded-lg bg-surface-low px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-primary"
          />
          <AppButton @click="tambahLibur">Tambah</AppButton>
        </div>
        <ul class="mt-3 divide-y divide-outline/60">
          <li v-for="l in libur" :key="l.id" class="flex items-center justify-between py-2 text-sm">
            <span
              ><strong>{{ tanggal(l.date) }}</strong> — {{ l.name }}</span
            >
            <AppButton v-if="bolehLibur" variant="ghost" @click="hapusLibur(l.id)">Hapus</AppButton>
          </li>
        </ul>
        <AppEmpty v-if="!libur.length" message="Belum ada hari libur perusahaan." />
      </AppCard>
    </div>

    <div
      v-if="showShift"
      class="fixed inset-0 z-20 flex items-center justify-center bg-black/40 p-4"
      @click.self="showShift = false"
    >
      <form class="w-full max-w-md rounded-xl bg-card p-5 shadow-xl" @submit.prevent="simpanShift">
        <h3 class="font-display font-semibold">{{ sunting ? 'Ubah shift' : 'Tambah shift' }}</h3>
        <AppField label="Nama shift (wajib)"
          ><input
            v-model="fShift.name"
            maxlength="255"
            class="w-full rounded-lg bg-surface-low px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-primary"
            placeholder="cth. Pagi"
        /></AppField>
        <div class="grid grid-cols-2 gap-2">
          <AppField label="Mulai (jam:menit)"
            ><input
              v-model="fShift.starts_at"
              type="time"
              class="w-full rounded-lg bg-surface-low px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-primary"
          /></AppField>
          <AppField label="Selesai (jam:menit)"
            ><input
              v-model="fShift.ends_at"
              type="time"
              class="w-full rounded-lg bg-surface-low px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-primary"
          /></AppField>
        </div>
        <AppField label="Toleransi telat (menit, 0–600)"
          ><input
            v-model.number="fShift.late_tolerance_minutes"
            type="number"
            min="0"
            max="600"
            class="w-full rounded-lg bg-surface-low px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-primary"
        /></AppField>
        <div class="mt-3 flex justify-end gap-2">
          <AppButton variant="ghost" type="button" @click="showShift = false">Batal</AppButton>
          <AppButton type="submit">Simpan</AppButton>
        </div>
      </form>
    </div>

    <div
      v-if="tanyaHapus"
      class="fixed inset-0 z-20 flex items-center justify-center bg-black/40 p-4"
      @click.self="tanyaHapus = null"
    >
      <div class="w-full max-w-sm rounded-xl bg-card p-5 shadow-xl">
        <h3 class="font-display font-semibold">Hapus shift {{ tanyaHapus.name }}?</h3>
        <p class="text-sm text-secondary">Pola yang memakai shift ini perlu diatur ulang.</p>
        <div class="mt-3 flex justify-end gap-2">
          <AppButton variant="ghost" @click="tanyaHapus = null">Batal</AppButton>
          <AppButton @click="hapusShift">Hapus</AppButton>
        </div>
      </div>
    </div>
  </div>
</template>
