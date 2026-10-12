<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { api } from '../lib/api'
import { tanggal } from '../lib/format'
import { useCompany } from '../composables/useCompany'
import { useAsync } from '../composables/useAsync'
import AppCard from '../components/ui/AppCard.vue'
import AppBadge from '../components/ui/AppBadge.vue'
import AppBanner from '../components/ui/AppBanner.vue'
import AppIcon from '../components/ui/AppIcon.vue'

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
    <div class="mb-4 flex items-center gap-3">
      <div class="flex h-12 w-12 items-center justify-center rounded-xl bg-primary-container text-on-primary">
        <AppIcon name="workspace_premium" :size="28" />
      </div>
      <h1 class="font-display text-xl font-semibold">Langganan</h1>
    </div>
    <AppBanner tone="red" :message="error" />
    <p v-if="loading" class="text-sm text-secondary">Memuat…</p>
    <AppCard v-if="sub" title="Status paket">
      <div class="flex items-center justify-between">
        <p class="font-display font-semibold">{{ sub.planName ?? 'Tanpa paket' }}</p>
        <AppBadge :tone="tone(sub.status)">{{ sub.status }}</AppBadge>
      </div>
      <p class="mt-2 text-sm text-secondary">
        {{ sub.endsAt ? `Aktif sampai ${tanggal(sub.endsAt)}.` : sub.trialEndsAt ? `Masa coba sampai ${tanggal(sub.trialEndsAt)}.` : '' }}
      </p>
      <AppBanner
        v-if="!sub.allowsWrites" tone="red"
        message="Langganan bermasalah — penulisan diblokir, data tetap bisa dibaca. Hubungi pemilik perusahaan."
      />
      <p class="mt-2 text-xs text-secondary">Perpanjangan dilakukan oleh admin layanan.</p>
    </AppCard>
  </div>
</template>
