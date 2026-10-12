<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue'
import { api, fetchBlob } from '../lib/api'
import { useCompany } from '../composables/useCompany'
import { useAsync } from '../composables/useAsync'
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
    <div class="mb-4 flex items-center gap-3">
      <div class="flex h-12 w-12 items-center justify-center rounded-xl bg-primary-container text-on-primary">
        <AppIcon name="badge" :size="28" />
      </div>
      <h1 class="font-display text-xl font-semibold">Kartu karyawan</h1>
    </div>
    <AppBanner tone="red" :message="error" />
    <p v-if="loading" class="text-sm text-secondary">Memuat…</p>
    <div v-if="card" class="rounded-xl bg-primary p-5 text-white shadow">
      <div class="flex items-center gap-4">
        <div class="flex h-14 w-14 items-center justify-center rounded-full bg-white/20 text-xl font-extrabold">
          {{ card.name.charAt(0).toUpperCase() }}
        </div>
        <div>
          <p class="font-display font-bold">{{ card.name }}</p>
          <p class="text-sm text-emerald-50/85">{{ card.role ?? 'Karyawan' }}{{ card.branch ? ` · ${card.branch}` : '' }}</p>
          <p class="text-sm text-emerald-50/85">{{ card.company }}</p>
        </div>
      </div>
      <div class="mt-4 flex items-end justify-between gap-4">
        <div class="text-xs text-emerald-50/85">
          <p>{{ card.email }}</p>
          <p>{{ card.phone ?? '' }}</p>
          <p class="mt-1 font-mono">{{ card.employeeUuid }}</p>
        </div>
        <img v-if="qrUrl" :src="qrUrl" alt="QR kartu karyawan" class="h-24 w-24 rounded-lg bg-white p-1" />
      </div>
      <AppButton variant="ghost" class="no-print mt-4 !border-white/40 !text-white" @click="cetak">Cetak kartu</AppButton>
    </div>
  </div>
</template>
