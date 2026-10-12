<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { api, ApiError, type PageMeta } from '../lib/api'
import { tanggal } from '../lib/format'
import { useCompany } from '../composables/useCompany'
import { useAsync } from '../composables/useAsync'
import AppCard from '../components/ui/AppCard.vue'
import AppBadge from '../components/ui/AppBadge.vue'
import AppBanner from '../components/ui/AppBanner.vue'
import AppButton from '../components/ui/AppButton.vue'
import AppField from '../components/ui/AppField.vue'
import AppPager from '../components/ui/AppPager.vue'
import AppIcon from '../components/ui/AppIcon.vue'

interface Branch { id: number; code: string; name: string }
interface Row { id: number; date: string; status: string; clockInAt: string | null; clockOutAt: string | null; lateMinutes: number }
interface Shift { name: string; starts_at: string; ends_at: string }

const { auth, companyId } = useCompany()
const page = useAsync()
const rows = ref<Row[]>([])
const meta = ref<PageMeta | null>(null)
const branches = ref<Branch[]>([])
const branchId = ref<number | null>(null)
const qr = ref<{ token: string; expires_in: number } | null>(null)
const selfie = ref<File | null>(null)
const okMsg = ref('')
const formMsg = ref('')
const shiftHariIni = ref<Shift | null>(null)
const from = ref(new Date(Date.now() - 29 * 864e5).toISOString().slice(0, 10))
const to = ref(new Date().toISOString().slice(0, 10))

const tone = (s: string) => (s === 'hadir' ? 'green' : s === 'terlambat' ? 'amber' : s === 'alpa' ? 'red' : 'blue')
const ringkas = computed(() => {
  const hitung = (s: string) => rows.value.filter((r) => r.status === s).length
  return { hadir: hitung('hadir'), terlambat: hitung('terlambat'), alpa: hitung('alpa'), cuti: rows.value.filter((r) => ['izin', 'sakit', 'cuti'].includes(r.status)).length }
})

function jam(iso: string | null) {
  if (!iso) return '–'
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

async function muatQr() {
  if (!branchId.value) return
  qr.value = await page.run(() =>
    api.get<{ token: string; expires_in: number }>(
      `/companies/${companyId.value}/branches/${branchId.value}/attendance-qr`, auth.token,
    ),
  )
}

async function clock(path: 'clock-in' | 'clock-out') {
  okMsg.value = ''
  formMsg.value = ''
  if (!branchId.value || !qr.value) return
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
    const res = await page.run(() => api.postForm(`/companies/${companyId.value}/attendance/${path}`, form, auth.token))
    if (res) {
      okMsg.value = path === 'clock-in' ? 'Clock-in tercatat.' : 'Clock-out tercatat.'
      muatRiwayat(1)
    } else {
      formMsg.value = page.error.value
    }
  } catch (e) {
    formMsg.value = e instanceof Error ? e.message : 'Gagal membaca lokasi.'
  }
}

function onFile(e: Event) {
  selfie.value = (e.target as HTMLInputElement).files?.[0] ?? null
}

async function muatRiwayat(p = 1) {
  const res = await page.run(() =>
    api.get<{ data: Row[]; meta: PageMeta }>(
      `/companies/${companyId.value}/attendance/me?from=${from.value}&to=${to.value}&per_page=15&page=${p}`, auth.token,
    ),
  )
  if (res) {
    const raw = res as unknown as { data: Row[]; meta?: PageMeta } | Row[]
    if (Array.isArray(raw)) rows.value = raw
    else { rows.value = raw.data; meta.value = raw.meta ?? null }
  }
}

async function muatShift() {
  if (!auth.user) return
  const today = new Date().toISOString().slice(0, 10)
  try {
    const days = await api.get<{ date: string; shiftName: string | null; startsAt: string | null; endsAt: string | null; isOff: boolean }[]>(
      `/companies/${companyId.value}/users/${auth.user.id}/schedule?from=${today}&to=${today}`, auth.token,
    )
    const d = days?.[0]
    shiftHariIni.value = d && !d.isOff && d.shiftName ? { name: d.shiftName, starts_at: d.startsAt ?? '', ends_at: d.endsAt ?? '' } : null
  } catch (e) {
    if (!(e instanceof ApiError && e.status === 403)) throw e
  }
}

onMounted(async () => {
  branches.value = (await page.run(() => api.get<Branch[]>(`/companies/${companyId.value}/branches`, auth.token))) ?? []
  if (branches.value.length) {
    branchId.value = branches.value[0]!.id
    muatQr()
  }
  muatShift()
  muatRiwayat(1)
})
</script>

<template>
  <div>
    <p class="text-xs font-semibold uppercase tracking-wider text-secondary">Portal absensi mandiri</p>
    <div class="mb-4 flex flex-wrap items-center justify-between gap-2">
      <h1 class="font-display text-xl font-semibold">Absensi saya</h1>
      <p v-if="shiftHariIni" class="text-sm text-secondary">
        Shift hari ini: <strong class="text-ink">{{ shiftHariIni.name }} ({{ shiftHariIni.starts_at }}–{{ shiftHariIni.ends_at }})</strong>
      </p>
    </div>
    <AppBanner tone="green" :message="okMsg" />
    <AppBanner tone="red" :message="formMsg || page.error.value" />

    <div class="grid gap-4 lg:grid-cols-2">
      <div>
        <AppCard title="Status hari ini">
          <div class="flex gap-2">
            <AppButton :loading="page.loading.value" @click="clock('clock-in')">
              <AppIcon name="login" :size="18" />Clock-in
            </AppButton>
            <AppButton variant="secondary" :loading="page.loading.value" @click="clock('clock-out')">
              <AppIcon name="logout" :size="18" />Clock-out
            </AppButton>
          </div>
        </AppCard>
        <AppCard title="Verifikasi" class="mt-4">
          <AppField label="Cabang">
            <select v-model="branchId" class="w-full rounded-lg border border-outline bg-card px-3 py-2.5 focus:border-primary focus:outline-none" @change="muatQr">
              <option v-for="b in branches" :key="b.id" :value="b.id">{{ b.code }} — {{ b.name }}</option>
            </select>
          </AppField>
          <div class="mt-3 rounded-lg bg-surface-low p-3">
            <p class="text-xs uppercase tracking-wider text-secondary">Kode cabang aktif</p>
            <p class="break-all font-mono text-sm font-semibold">{{ qr?.token ?? '—' }}</p>
            <p v-if="qr" class="mt-1 text-xs text-secondary">Berganti ±{{ qr.expires_in }} detik · <button class="font-semibold text-primary" @click="muatQr">minta kode baru</button></p>
          </div>
          <div class="mt-3">
            <AppField label="Foto selfie (wajib clock-in, maks 2 MB)">
              <input type="file" accept="image/*" capture="user" class="text-sm" @change="onFile" />
            </AppField>
          </div>
          <div class="mt-3 rounded-lg bg-surface-low p-3 text-sm">
            <p class="font-semibold">Perangkat terikat: <span class="font-mono text-xs">{{ deviceId() }}</span></p>
            <p class="mt-1 text-xs text-secondary">Satu akun satu perangkat. Ganti HP? Minta HR mereset perangkat Anda.</p>
          </div>
        </AppCard>
      </div>

      <AppCard title="Riwayat presensi">
        <div class="mb-3 grid grid-cols-4 gap-2 text-center">
          <div class="rounded-lg bg-surface-low p-2"><p class="text-[11px] uppercase text-secondary">Hadir</p><p class="font-display text-lg font-bold">{{ ringkas.hadir }}</p></div>
          <div class="rounded-lg bg-surface-low p-2"><p class="text-[11px] uppercase text-secondary">Terlambat</p><p class="font-display text-lg font-bold">{{ ringkas.terlambat }}</p></div>
          <div class="rounded-lg bg-surface-low p-2"><p class="text-[11px] uppercase text-secondary">Alpa</p><p class="font-display text-lg font-bold">{{ ringkas.alpa }}</p></div>
          <div class="rounded-lg bg-surface-low p-2"><p class="text-[11px] uppercase text-secondary">Cuti/Izin</p><p class="font-display text-lg font-bold">{{ ringkas.cuti }}</p></div>
        </div>
        <div class="mb-3 grid grid-cols-2 gap-2">
          <input v-model="from" type="date" class="rounded-lg border border-outline bg-card px-3 py-2 text-sm" @change="muatRiwayat(1)" />
          <input v-model="to" type="date" class="rounded-lg border border-outline bg-card px-3 py-2 text-sm" @change="muatRiwayat(1)" />
        </div>
        <div class="overflow-x-auto">
          <table class="w-full text-sm">
            <thead>
              <tr class="text-left text-xs uppercase text-secondary">
                <th class="py-2 pr-2">Tanggal</th>
                <th class="py-2 pr-2 text-center">Masuk</th>
                <th class="py-2 pr-2 text-center">Pulang</th>
                <th class="py-2 pr-2 text-center">Telat</th>
                <th class="py-2 text-right">Status</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="r in rows" :key="r.id" class="border-t border-surface-low">
                <td class="py-2 pr-2">{{ tanggal(r.date) }}</td>
                <td class="py-2 pr-2 text-center">{{ jam(r.clockInAt) }}</td>
                <td class="py-2 pr-2 text-center">{{ jam(r.clockOutAt) }}</td>
                <td class="py-2 pr-2 text-center">{{ r.lateMinutes ? `${r.lateMinutes} mnt` : '–' }}</td>
                <td class="py-2 text-right"><AppBadge :tone="tone(r.status)">{{ r.status }}</AppBadge></td>
              </tr>
            </tbody>
          </table>
        </div>
        <p class="mt-2 text-xs text-secondary">Alpa memotong gaji proporsional 1/30 per hari.</p>
        <AppPager :meta="meta" @page="muatRiwayat" />
      </AppCard>
    </div>
  </div>
</template>
