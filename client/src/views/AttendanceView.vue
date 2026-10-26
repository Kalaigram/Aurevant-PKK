<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { api, ApiError } from '../lib/api'
import { tanggal } from '../lib/format'
import { labelAttendanceStatus } from '../lib/labels'
import { useCompany } from '../composables/useCompany'
import { useAsync } from '../composables/useAsync'
import { usePagedList } from '../composables/usePagedList'
import AppBadge from '../components/ui/AppBadge.vue'
import AppBanner from '../components/ui/AppBanner.vue'
import AppButton from '../components/ui/AppButton.vue'
import AppCard from '../components/ui/AppCard.vue'
import AppDateInput from '../components/ui/AppDateInput.vue'
import AppEmpty from '../components/ui/AppEmpty.vue'
import AppField from '../components/ui/AppField.vue'
import AppIcon from '../components/ui/AppIcon.vue'
import AppPageHeader from '../components/ui/AppPageHeader.vue'
import AppPager from '../components/ui/AppPager.vue'
import AppSelect from '../components/ui/AppSelect.vue'
import AppStat from '../components/ui/AppStat.vue'
import AppTable from '../components/ui/AppTable.vue'

interface Branch {
  id: number
  code: string
  name: string
}
interface Row {
  id: number
  date: string
  status: string
  clockInAt: string | null
  clockOutAt: string | null
  lateMinutes: number
}
interface Shift {
  name: string
  starts_at: string
  ends_at: string
}
interface BranchQr {
  token: string
  expires_in: number
  qr_svg?: string
}

const { auth, companyId } = useCompany()
const page = useAsync()
const branches = ref<Branch[]>([])
const branchId = ref<number | null>(null)
const qr = ref<BranchQr | null>(null)
const sisaQr = ref(0)
const memuatQr = ref(false)
const selfie = ref<File | null>(null)
const selfieUrl = ref('')
const okMsg = ref('')
const formMsg = ref('')
const shiftHariIni = ref<Shift | null>(null)
const shiftTersedia = ref(true)
const awal = new Date(Date.now() - 29 * 864e5).toLocaleDateString('sv-SE')
const from = ref(awal)
const to = ref(new Date().toLocaleDateString('sv-SE'))
const {
  rows,
  meta,
  loading: loadingRiwayat,
  error: errorRiwayat,
  load: muatRiwayat,
  setPage,
  setPerPage,
} = usePagedList<Row>(
  (page, perPage) =>
    api.getPage<Row>(
      `/companies/${companyId.value}/attendance/me?from=${from.value}&to=${to.value}&per_page=${perPage}&page=${page}`,
      auth.token,
    ),
  { perPage: 15 },
)
let timer: number | undefined

const opsiCabang = computed(() =>
  branches.value.map((b) => ({ value: b.id, label: `${b.code} — ${b.name}` })),
)
const tone = (s: string) =>
  s === 'hadir' ? 'green' : s === 'terlambat' ? 'amber' : s === 'alpa' ? 'red' : 'blue'
const ringkas = computed(() => {
  const hitung = (s: string) => rows.value.filter((r) => r.status === s).length
  return {
    hadir: hitung('hadir'),
    terlambat: hitung('terlambat'),
    alpa: hitung('alpa'),
    cuti: rows.value.filter((r) => ['izin', 'sakit', 'cuti'].includes(r.status)).length,
  }
})

function jam(iso: string | null) {
  if (!iso) return '—'
  return new Date(iso).toLocaleTimeString('id-ID', { hour: '2-digit', minute: '2-digit' })
}

function deviceId() {
  let id = localStorage.getItem('aurevant-device')
  if (!id) {
    id = 'web-' + Math.random().toString(36).slice(2, 10)
    localStorage.setItem('aurevant-device', id)
  }
  return id
}

function posisi(): Promise<{ lat: number; lng: number }> {
  return new Promise((resolve, reject) => {
    if (!navigator.geolocation) return reject(new Error('Browser tidak mendukung lokasi.'))
    navigator.geolocation.getCurrentPosition(
      (p) => resolve({ lat: p.coords.latitude, lng: p.coords.longitude }),
      () => reject(new Error('Izin lokasi ditolak.')),
      { timeout: 10000 },
    )
  })
}

function mulaiHitung() {
  window.clearInterval(timer)
  timer = window.setInterval(() => {
    if (!qr.value) return
    sisaQr.value -= 1
    if (sisaQr.value <= 0) muatQr()
  }, 1000)
}

async function muatQr() {
  if (!branchId.value || memuatQr.value) return
  memuatQr.value = true
  try {
    const res = await api.get<BranchQr>(
      `/companies/${companyId.value}/branches/${branchId.value}/attendance-qr`,
      auth.token,
    )
    qr.value = res
    sisaQr.value = res.expires_in
    mulaiHitung()
  } catch (e) {
    formMsg.value = e instanceof ApiError ? e.message : 'Gagal memuat QR cabang.'
  } finally {
    memuatQr.value = false
  }
}

async function clock(path: 'clock-in' | 'clock-out') {
  okMsg.value = ''
  formMsg.value = ''
  if (!branchId.value || !qr.value) {
    formMsg.value = 'QR cabang belum siap. Tunggu sebentar atau minta kode baru.'
    return
  }
  if (path === 'clock-in' && !selfie.value) {
    formMsg.value = 'Foto selfie wajib untuk clock-in.'
    return
  }
  try {
    const { lat, lng } = await posisi()
    const form = new FormData()
    form.append('branch_id', String(branchId.value))
    form.append('qr_token', qr.value.token)
    form.append('lat', String(lat))
    form.append('lng', String(lng))
    form.append('device_id', deviceId())
    if (path === 'clock-in' && selfie.value) form.append('selfie', selfie.value)
    const res = await page.run(() =>
      api.postForm(`/companies/${companyId.value}/attendance/${path}`, form, auth.token),
    )
    if (res) {
      okMsg.value = path === 'clock-in' ? 'Clock-in tercatat.' : 'Clock-out tercatat.'
      if (path === 'clock-in') hapusSelfie()
      await muatRiwayat(1)
    } else {
      formMsg.value = page.error.value
    }
  } catch (e) {
    formMsg.value = e instanceof Error ? e.message : 'Gagal membaca lokasi.'
  }
}

function onFile(e: Event) {
  const file = (e.target as HTMLInputElement).files?.[0] ?? null
  hapusSelfie()
  selfie.value = file
  if (file) selfieUrl.value = URL.createObjectURL(file)
}

function hapusSelfie() {
  if (selfieUrl.value) URL.revokeObjectURL(selfieUrl.value)
  selfieUrl.value = ''
  selfie.value = null
}


async function muatShift() {
  if (!auth.user) return
  const today = new Date().toLocaleDateString('sv-SE')
  try {
    const days = await api.get<
      { date: string; shiftName: string | null; startsAt: string | null; endsAt: string | null; isOff: boolean }[]
    >(
      `/companies/${companyId.value}/users/${auth.user.id}/schedule?from=${today}&to=${today}`,
      auth.token,
    )
    const d = days?.[0]
    shiftHariIni.value =
      d && !d.isOff && d.shiftName
        ? { name: d.shiftName, starts_at: d.startsAt ?? '', ends_at: d.endsAt ?? '' }
        : null
  } catch {
    shiftHariIni.value = null
    shiftTersedia.value = false
  }
}

function cetak() {
  window.print()
}

onMounted(async () => {
  branches.value =
    (await page.run(() =>
      api.get<Branch[]>(`/companies/${companyId.value}/branches`, auth.token),
    )) ?? []
  if (branches.value.length) {
    branchId.value = branches.value[0]!.id
    await muatQr()
  }
  await muatShift()
  await muatRiwayat(1)
})

onBeforeUnmount(() => {
  window.clearInterval(timer)
  hapusSelfie()
})
</script>

<template>
  <div>
    <AppPageHeader
      class="no-print"
      eyebrow="Portal absensi mandiri"
      title="Absensi saya"
      subtitle="Pindai QR cabang, ambil selfie, dan clock-in dari lokasi Anda."
      icon="fingerprint"
    >
      <template #actions>
        <AppButton variant="ghost" @click="cetak">
          <AppIcon name="print" :size="18" />Cetak riwayat
        </AppButton>
      </template>
    </AppPageHeader>

    <div class="print-only mb-4 border-b border-outline pb-3">
      <p class="font-display text-lg font-bold">{{ auth.companyName ?? 'Aurevant' }}</p>
      <p class="text-sm">Riwayat absensi {{ auth.user?.name }}</p>
      <p class="text-xs text-secondary">{{ tanggal(from) }} — {{ tanggal(to) }}</p>
    </div>

    <AppBanner tone="green" :message="okMsg" />
    <AppBanner tone="red" :message="formMsg || page.error.value || errorRiwayat" />

    <div class="grid gap-4 lg:grid-cols-[minmax(0,0.9fr)_minmax(0,1.1fr)]">
      <div class="no-print space-y-4">
        <AppCard title="Status hari ini">
          <div class="flex flex-wrap gap-2">
            <AppButton :loading="page.loading.value" @click="clock('clock-in')">
              <AppIcon name="login" :size="18" />Clock-in
            </AppButton>
            <AppButton
              variant="secondary"
              :loading="page.loading.value"
              @click="clock('clock-out')"
            >
              <AppIcon name="logout" :size="18" />Clock-out
            </AppButton>
          </div>
          <div class="mt-3 rounded-lg bg-surface-low p-3 text-sm">
            <p class="font-medium text-ink">
              {{
                shiftHariIni
                  ? `${shiftHariIni.name} (${shiftHariIni.starts_at}–${shiftHariIni.ends_at})`
                  : shiftTersedia
                    ? 'Tidak ada shift hari ini'
                    : 'Jadwal shift tidak tersedia untuk akun ini'
              }}
            </p>
            <p class="mt-1 text-xs text-secondary">
              Slip absensi memakai QR cabang, lokasi GPS, dan selfie untuk clock-in.
            </p>
          </div>
        </AppCard>

        <AppCard title="Verifikasi kehadiran">
          <AppField label="Cabang">
            <AppSelect
              v-model="branchId"
              :options="opsiCabang"
              placeholder="Pilih cabang"
              @update:model-value="muatQr"
            />
          </AppField>

          <div class="mt-3 rounded-xl border border-outline/60 bg-surface-low p-4 text-center">
            <img
              v-if="qr?.qr_svg"
              :src="qr.qr_svg"
              alt="QR absensi cabang"
              class="mx-auto h-48 w-48 rounded-lg bg-white p-2"
            />
            <div v-else class="flex h-48 items-center justify-center text-sm text-secondary">
              {{ memuatQr ? 'Memuat QR…' : 'QR belum tersedia' }}
            </div>
            <p class="mt-2 text-xs text-secondary">
              <template v-if="qr">
                QR aktif · diperbarui dalam <strong class="text-ink">{{ sisaQr }}</strong> detik
              </template>
              <template v-else>Pilih cabang untuk memuat QR.</template>
            </p>
            <AppButton v-if="qr" class="mt-2" size="sm" variant="subtle" @click="muatQr">
              Minta QR baru
            </AppButton>
            <p v-if="qr" class="mt-2 break-all font-mono text-3xs text-secondary">
              {{ qr.token }}
            </p>
          </div>

          <div class="mt-3">
            <AppField label="Foto selfie (wajib clock-in, maks 2 MB)">
              <input
                type="file"
                accept="image/*"
                capture="user"
                class="block w-full text-sm text-secondary file:mr-3 file:rounded-lg file:border-0 file:bg-primary-soft file:px-3 file:py-2 file:text-sm file:font-semibold file:text-primary"
                @change="onFile"
              />
            </AppField>
            <div v-if="selfieUrl" class="mt-2 flex items-center gap-3 rounded-lg bg-surface-low p-2">
              <img :src="selfieUrl" alt="Pratinjau selfie" class="h-16 w-16 rounded-lg object-cover" />
              <div class="min-w-0 flex-1">
                <p class="truncate text-sm font-medium text-ink">{{ selfie?.name }}</p>
                <p class="text-xs text-secondary">Pratinjau foto absensi.</p>
              </div>
              <AppButton variant="ghost" @click="hapusSelfie">Hapus</AppButton>
            </div>
          </div>

          <div class="mt-3 rounded-lg bg-surface-low p-3 text-sm">
            <p class="font-semibold text-ink">
              Perangkat terikat: <span class="font-mono text-xs">{{ deviceId() }}</span>
            </p>
            <p class="mt-1 text-xs text-secondary">
              Satu akun satu perangkat. Ganti HP? Minta HR mereset perangkat Anda.
            </p>
          </div>
        </AppCard>
      </div>

      <div class="print-area space-y-4">
        <div>
          <p class="mb-2 text-xs text-secondary">
            Ringkasan {{ rows.length }} catatan pada halaman ini.
          </p>
          <div class="grid grid-cols-2 gap-3 sm:grid-cols-4">
            <AppStat label="Hadir" :value="ringkas.hadir" icon="check_circle" tone="positive" />
            <AppStat label="Terlambat" :value="ringkas.terlambat" icon="schedule" tone="warning" />
            <AppStat label="Alpa" :value="ringkas.alpa" icon="cancel" tone="negative" />
            <AppStat label="Cuti / izin" :value="ringkas.cuti" icon="event_busy" />
          </div>
        </div>

        <AppCard title="Riwayat presensi">
          <div class="no-print mb-3 grid grid-cols-2 gap-2">
            <AppField label="Dari">
              <AppDateInput v-model="from" @change="muatRiwayat(1)" />
            </AppField>
            <AppField label="Sampai">
              <AppDateInput v-model="to" @change="muatRiwayat(1)" />
            </AppField>
          </div>
          <AppTable
            caption="Riwayat presensi"
            :loading="loadingRiwayat"
            :empty="!rows.length"
            :colspan="5"
            empty-title="Belum ada presensi"
            empty-message="Belum ada presensi pada rentang tanggal ini."
          >
            <template #head>
              <tr>
                <th>Tanggal</th>
                <th class="text-center">Masuk</th>
                <th class="text-center">Pulang</th>
                <th class="text-center">Telat</th>
                <th class="text-right">Status</th>
              </tr>
            </template>
            <tr v-for="r in rows" :key="r.id">
              <td class="whitespace-nowrap">{{ tanggal(r.date) }}</td>
              <td class="text-center tabular-nums">{{ jam(r.clockInAt) }}</td>
              <td class="text-center tabular-nums">{{ jam(r.clockOutAt) }}</td>
              <td class="text-center tabular-nums">
                {{ r.lateMinutes ? `${r.lateMinutes} mnt` : '—' }}
              </td>
              <td class="text-right">
                <AppBadge :tone="tone(r.status)">{{ labelAttendanceStatus(r.status) }}</AppBadge>
              </td>
            </tr>
          </AppTable>
          <p class="mt-2 text-xs text-secondary">Alpa memotong gaji proporsional 1/30 per hari.</p>
          <AppPager :meta="meta" @page="setPage" @update:per-page="setPerPage" />
        </AppCard>
      </div>
    </div>

    <AppEmpty
      v-if="!branches.length && !page.loading.value"
      title="Belum ada cabang aktif"
      message="Hubungi pemilik atau admin untuk menambahkan cabang sebelum absensi."
    />
  </div>
</template>

