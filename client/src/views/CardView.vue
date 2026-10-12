<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue'
import { api, fetchBlob } from '../lib/api'
import { useCompany } from '../composables/useCompany'
import { useAsync } from '../composables/useAsync'
import AppCard from '../components/ui/AppCard.vue'
import AppBanner from '../components/ui/AppBanner.vue'
import AppButton from '../components/ui/AppButton.vue'
import AppIcon from '../components/ui/AppIcon.vue'

interface Card {
  name: string
  email: string
  phone: string | null
  employeeUuid: string
  company: string
  branch: string | null
  role: string | null
}

const { auth, companyId } = useCompany()
const { loading, error, run } = useAsync()
const card = ref<Card | null>(null)
const qrUrl = ref('')
const sisi = ref<'depan' | 'belakang'>('depan')

function deviceId() {
  return localStorage.getItem('aurevant-device') ?? 'belum terikat di perangkat ini'
}

onMounted(async () => {
  if (!auth.user) return
  card.value = await run(() =>
    api.get<Card>(`/companies/${companyId.value}/users/${auth.user!.id}/card`, auth.token),
  )
  if (card.value) {
    qrUrl.value = await run(() => fetchBlob(`/companies/${companyId.value}/users/${auth.user!.id}/card/qr`, auth.token)) ?? ''
  }
})
onUnmounted(() => { if (qrUrl.value) URL.revokeObjectURL(qrUrl.value) })

function cetak() {
  window.print()
}
</script>

<template>
  <div class="mx-auto max-w-md">
    <p class="text-xs font-semibold uppercase tracking-wider text-secondary">Kredensial kepegawaian</p>
    <div class="mb-4 flex items-center justify-between">
      <h1 class="font-display text-xl font-semibold">Kartu identitas digital</h1>
      <div class="flex gap-1 rounded-lg bg-surface-low p-1 text-sm font-semibold">
        <button class="rounded-md px-3 py-1" :class="sisi === 'depan' ? 'bg-card text-primary shadow-sm' : 'text-secondary'" @click="sisi = 'depan'">Depan</button>
        <button class="rounded-md px-3 py-1" :class="sisi === 'belakang' ? 'bg-card text-primary shadow-sm' : 'text-secondary'" @click="sisi = 'belakang'">Belakang</button>
      </div>
    </div>
    <AppBanner tone="red" :message="error" />
    <p v-if="loading" class="text-sm text-secondary">Memuat…</p>

    <div v-if="card && sisi === 'depan'" class="rounded-xl bg-primary p-5 text-white shadow">
      <div class="flex items-start justify-between">
        <div>
          <p class="font-display font-bold">{{ card.company }}</p>
          <p class="text-xs uppercase tracking-wider text-emerald-50/80">Kartu karyawan</p>
        </div>
        <AppIcon name="badge" :size="28" />
      </div>
      <h2 class="font-display mt-4 text-xl font-bold">{{ card.name }}</h2>
      <p class="text-sm font-semibold text-emerald-100">{{ card.role ?? 'Karyawan' }}</p>
      <p class="text-sm text-emerald-50/85">{{ card.branch ?? '' }}</p>
      <div class="mt-4 flex items-end justify-between gap-4">
        <p class="font-mono text-xs text-emerald-50/85">{{ card.employeeUuid }}</p>
        <img v-if="qrUrl" :src="qrUrl" alt="QR kartu karyawan" class="h-24 w-24 rounded-lg bg-white p-1" />
      </div>
      <p class="mt-2 text-[11px] text-emerald-50/70">QR ini untuk validasi kehadiran, bukan data pribadi.</p>
    </div>

    <div v-if="card && sisi === 'belakang'" class="rounded-xl bg-primary p-5 text-white shadow">
      <p class="font-display font-bold">{{ card.company }}</p>
      <div class="mt-3 space-y-1 text-sm">
        <p>{{ card.email }}</p>
        <p>{{ card.phone ?? '' }}</p>
      </div>
      <div class="mt-4 flex items-center justify-center">
        <img v-if="qrUrl" :src="qrUrl" alt="QR kartu karyawan" class="h-36 w-36 rounded-lg bg-white p-2" />
      </div>
      <p class="mt-2 text-center text-[11px] text-emerald-50/70">Tunjukkan QR ini saat diminta HR atau perangkat cabang.</p>
    </div>

    <AppCard title="Rincian data (hanya baca)" class="mt-4">
      <dl v-if="card" class="space-y-2 text-sm">
        <div class="flex justify-between"><dt class="text-secondary">Nama</dt><dd class="font-semibold">{{ card.name }}</dd></div>
        <div class="flex justify-between"><dt class="text-secondary">Cabang</dt><dd class="font-semibold">{{ card.branch ?? '–' }}</dd></div>
        <div class="flex justify-between"><dt class="text-secondary">ID karyawan</dt><dd class="font-mono font-semibold text-primary">{{ card.employeeUuid }}</dd></div>
      </dl>
    </AppCard>

    <AppCard title="Perangkat terikat" class="mt-4">
      <p class="font-mono text-xs">{{ deviceId() }}</p>
      <p class="mt-1 text-xs text-secondary">Satu akun satu perangkat. Ganti HP atau hilang? Minta HR meresetnya.</p>
    </AppCard>

    <div class="no-print mt-4 flex gap-2">
      <AppButton variant="ghost" @click="cetak"><AppIcon name="print" :size="18" />Cetak kartu</AppButton>
    </div>
  </div>
</template>
