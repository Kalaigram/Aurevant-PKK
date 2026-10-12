<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { api } from '../lib/api'
import { tanggal } from '../lib/format'
import { useCompany } from '../composables/useCompany'
import { useAsync } from '../composables/useAsync'
import AppCard from '../components/ui/AppCard.vue'
import AppBadge from '../components/ui/AppBadge.vue'
import AppBanner from '../components/ui/AppBanner.vue'

interface Sub {
  planId: number | null
  planName: string | null
  status: string
  trialEndsAt: string | null
  endsAt: string | null
  allowsWrites: boolean
}

const { auth, companyId } = useCompany()
const { loading, error, run } = useAsync()
const sub = ref<Sub | null>(null)

const tone = (s: string) => (s === 'aktif' || s === 'trial' ? 'green' : s === 'tenggang' ? 'amber' : 'red')

onMounted(async () => {
  sub.value = await run(() => api.get<Sub>(`/companies/${companyId.value}/subscription`, auth.token))
})
</script>

<template>
  <div class="mx-auto max-w-xl">
    <h1 class="mb-4 text-xl font-bold">Langganan</h1>
    <AppBanner tone="red" :message="error" />
    <p v-if="loading" class="text-sm text-slate-500">Memuat…</p>
    <AppCard v-if="sub" title="Status paket">
      <div class="flex items-center justify-between">
        <p class="font-semibold">{{ sub.planName ?? 'Tanpa paket' }}</p>
        <AppBadge :tone="tone(sub.status)">{{ sub.status }}</AppBadge>
      </div>
      <p class="mt-2 text-sm text-slate-500">
        {{ sub.endsAt ? `Aktif sampai ${tanggal(sub.endsAt)}.` : sub.trialEndsAt ? `Masa coba sampai ${tanggal(sub.trialEndsAt)}.` : '' }}
      </p>
      <AppBanner
        v-if="!sub.allowsWrites" tone="red"
        message="Langganan bermasalah — penulisan diblokir, data tetap bisa dibaca. Hubungi pemilik perusahaan."
      />
      <p class="mt-2 text-xs text-slate-400">Perpanjangan dilakukan oleh admin layanan.</p>
    </AppCard>
  </div>
</template>
