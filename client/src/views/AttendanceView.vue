<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { api, type PageMeta } from '../lib/api'
import { tanggal } from '../lib/format'
import { useCompany } from '../composables/useCompany'
import { useAsync } from '../composables/useAsync'
import AppCard from '../components/ui/AppCard.vue'
import AppBadge from '../components/ui/AppBadge.vue'
import AppBanner from '../components/ui/AppBanner.vue'
import AppButton from '../components/ui/AppButton.vue'
import AppField from '../components/ui/AppField.vue'
import AppPager from '../components/ui/AppPager.vue'

interface Branch { id: number; code: string; name: string }
interface Row { id: number; date: string; status: string; clockInAt: string | null; clockOutAt: string | null; lateMinutes: number }

const { auth, companyId } = useCompany()
const { loading, error, run } = useAsync()
const branches = ref<Branch[]>([])
const branchId = ref<number | null>(null)
const qr = ref<{ token: string; expires_in: number } | null>(null)
const selfie = ref<File | null>(null)
const okMsg = ref('')
const formMsg = ref('')
const rows = ref<Row[]>([])
const meta = ref<PageMeta | null>(null)
const from = ref(new Date(Date.now() - 29 * 864e5).toISOString().slice(0, 10))
const to = ref(new Date().toISOString().slice(0, 10))

const tone = (s: string) => (s === 'hadir' ? 'green' : s === 'terlambat' ? 'amber' : s === 'alpa' ? 'red' : 'blue')
const deviceId = () => {
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
  qr.value = await run(() =>
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
  const { loading: busy, error: failed, run: exec } = useAsync()
  void busy
  try {
    const { lat, lng } = await posisi()
    const form = new FormData()
    form.append('branch_id', String(branchId.value))
    form.append('qr_token', qr.value.token)
    form.append('lat', String(lat))
    form.append('lng', String(lng))
    form.append('device_id', deviceId())
    if (path === 'clock-in' && selfie.value) form.append('selfie', selfie.value)
    const res = await exec(() => api.postForm(`/companies/${companyId.value}/attendance/${path}`, form, auth.token))
    if (res) {
      okMsg.value = path === 'clock-in' ? 'Clock-in tercatat.' : 'Clock-out tercatat.'
      muatRiwayat(1)
    } else {
      formMsg.value = failed.value
    }
  } catch (e) {
    formMsg.value = e instanceof Error ? e.message : 'Gagal membaca lokasi.'
  }
}

function onFile(e: Event) {
  selfie.value = (e.target as HTMLInputElement).files?.[0] ?? null
}

async function muatRiwayat(page = 1) {
  const res = await run(() =>
    api.get<{ data: Row[]; meta: PageMeta }>(
      `/companies/${companyId.value}/attendance/me?from=${from.value}&to=${to.value}&per_page=15&page=${page}`, auth.token,
    ),
  )
  if (res) {
    const raw = res as unknown as { data: Row[]; meta?: PageMeta } | Row[]
    if (Array.isArray(raw)) rows.value = raw
    else { rows.value = raw.data; meta.value = raw.meta ?? null }
  }
}

onMounted(async () => {
  branches.value = (await run(() => api.get<Branch[]>(`/companies/${companyId.value}/branches`, auth.token))) ?? []
  if (branches.value.length) {
    branchId.value = branches.value[0]!.id
    muatQr()
  }
  muatRiwayat(1)
})
</script>

<template>
  <div>
    <h1 class="mb-4 text-xl font-bold">Absensi saya</h1>
    <AppBanner tone="green" :message="okMsg" />
    <AppBanner tone="red" :message="formMsg || error" />

    <div class="grid gap-4 lg:grid-cols-2">
      <AppCard title="Clock-in / clock-out">
        <AppField label="Cabang">
          <select v-model="branchId" class="w-full rounded-lg border border-slate-300 px-3 py-2.5" @change="muatQr">
            <option v-for="b in branches" :key="b.id" :value="b.id">{{ b.code }} — {{ b.name }}</option>
          </select>
        </AppField>
        <div class="mt-3 rounded-lg bg-slate-100 p-3 text-center">
          <p class="text-xs text-slate-500">Token QR cabang (minta QR terbaru bila kedaluwarsa)</p>
          <p class="break-all font-mono text-sm font-semibold">{{ qr?.token ?? '—' }}</p>
          <p v-if="qr" class="text-xs text-slate-500">berlaku ±{{ qr.expires_in }} dtk · <button class="text-primary-700 underline" @click="muatQr">muat ulang</button></p>
        </div>
        <div class="mt-3">
          <AppField label="Foto selfie (wajib clock-in, maks 2 MB)">
            <input type="file" accept="image/*" capture="user" @change="onFile" class="text-sm" />
          </AppField>
        </div>
        <div class="mt-3 flex gap-2">
          <AppButton :loading="loading" @click="clock('clock-in')">Clock-in</AppButton>
          <AppButton variant="secondary" :loading="loading" @click="clock('clock-out')">Clock-out</AppButton>
        </div>
        <p class="mt-2 text-xs text-slate-500">Satu akun satu perangkat. Ganti HP? Minta HR mereset perangkat Anda.</p>
      </AppCard>

      <AppCard title="Riwayat 30 hari">
        <div class="mb-3 grid grid-cols-2 gap-2">
          <input v-model="from" type="date" class="rounded-lg border border-slate-300 px-3 py-2" @change="muatRiwayat(1)" />
          <input v-model="to" type="date" class="rounded-lg border border-slate-300 px-3 py-2" @change="muatRiwayat(1)" />
        </div>
        <div v-for="r in rows" :key="r.id" class="flex items-center justify-between border-b border-slate-100 py-2 text-sm last:border-0">
          <span>{{ tanggal(r.date) }}</span>
          <span><AppBadge :tone="tone(r.status)">{{ r.status }}</AppBadge>
            <span v-if="r.lateMinutes" class="ml-2 text-xs text-amber-700">telat {{ r.lateMinutes }} mnt</span></span>
        </div>
        <AppPager :meta="meta" @page="muatRiwayat" />
      </AppCard>
    </div>
  </div>
</template>
