<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue'
import { api, fetchBlob } from '../lib/api'
import { useCompany } from '../composables/useCompany'
import { useAsync } from '../composables/useAsync'
import AppCard from '../components/ui/AppCard.vue'
import AppBanner from '../components/ui/AppBanner.vue'
import AppButton from '../components/ui/AppButton.vue'

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
    <h1 class="mb-4 text-xl font-bold">Kartu karyawan</h1>
    <AppBanner tone="red" :message="error" />
    <p v-if="loading" class="text-sm text-slate-500">Memuat…</p>
    <div v-if="card" class="rounded-2xl border-2 border-primary-900 bg-white p-5">
      <div class="flex items-center gap-4">
        <div class="flex h-14 w-14 items-center justify-center rounded-full bg-primary-900 text-xl font-extrabold text-white">
          {{ card.name.charAt(0).toUpperCase() }}
        </div>
        <div>
          <p class="font-bold">{{ card.name }}</p>
          <p class="text-sm text-slate-500">{{ card.role ?? 'Karyawan' }}{{ card.branch ? ` · ${card.branch}` : '' }}</p>
          <p class="text-sm text-slate-500">{{ card.company }}</p>
        </div>
      </div>
      <div class="mt-4 flex items-center justify-between gap-4">
        <div class="text-xs text-slate-500">
          <p>{{ card.email }}</p>
          <p>{{ card.phone ?? '' }}</p>
          <p class="mt-1 font-mono">{{ card.employeeUuid }}</p>
        </div>
        <img v-if="qrUrl" :src="qrUrl" alt="QR kartu karyawan" class="h-24 w-24" />
      </div>
      <AppButton variant="ghost" class="no-print mt-4" @click="cetak">Cetak kartu</AppButton>
    </div>
  </div>
</template>
