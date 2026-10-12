<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue'
import { api, type PageMeta } from '../lib/api'
import { waktuRelatif } from '../lib/format'
import { useCompany } from '../composables/useCompany'
import { useAsync } from '../composables/useAsync'
import AppCard from '../components/ui/AppCard.vue'
import AppBadge from '../components/ui/AppBadge.vue'
import AppBanner from '../components/ui/AppBanner.vue'
import AppButton from '../components/ui/AppButton.vue'
import AppEmpty from '../components/ui/AppEmpty.vue'
import AppPager from '../components/ui/AppPager.vue'
import AppIcon from '../components/ui/AppIcon.vue'

interface Notif {
  id: number
  type: string
  title: string
  body: string | null
  read_at: string | null
  created_at?: string
}

interface Prefs { type: string; enabled: boolean }

const { auth, companyId } = useCompany()
const { loading, error, run } = useAsync()
const list = ref<Notif[]>([])
const meta = ref<PageMeta | null>(null)
const unread = ref(0)
const tab = ref<'semua' | 'belum'>('semua')
const prefs = ref<Prefs[]>([])
let timer: number | undefined

async function muat(page = 1) {
  const q = `?per_page=15&page=${page}&company_id=${companyId.value}` + (tab.value === 'belum' ? '&unread=1' : '')
  const res = await run(() => api.get<{ data: Notif[]; meta: PageMeta }>(`/notifications${q}`, auth.token))
  if (res) {
    const raw = res as unknown as { data: Notif[]; meta?: PageMeta } | Notif[]
    if (Array.isArray(raw)) list.value = raw
    else { list.value = raw.data; meta.value = raw.meta ?? null }
  }
  const c = await run(() => api.get<{ unread: number }>('/notifications/unread-count', auth.token))
  if (c) unread.value = c.unread
}

async function baca(n: Notif) {
  await run(() => api.post(`/notifications/${n.id}/read`, undefined, auth.token))
  muat(meta.value?.current_page ?? 1)
}

async function bacaSemua() {
  await run(() => api.post(`/notifications/read-all?company_id=${companyId.value}`, undefined, auth.token))
  muat(1)
}

async function muatPrefs() {
  prefs.value = (await run(() => api.get<Prefs[]>('/notification-preferences', auth.token))) ?? []
}

async function togglePref(p: Prefs) {
  await run(() => api.put('/notification-preferences', { type: p.type, enabled: !p.enabled }, auth.token))
  muatPrefs()
}

onMounted(() => {
  muat()
  muatPrefs()
  timer = window.setInterval(() => muat(meta.value?.current_page ?? 1), 30000)
})
onUnmounted(() => window.clearInterval(timer))
</script>

<template>
  <div>
    <div class="mb-4 flex flex-wrap items-center justify-between gap-2">
      <div class="flex items-center gap-3">
        <div class="flex h-12 w-12 items-center justify-center rounded-xl bg-primary-container text-on-primary">
          <AppIcon name="notifications" :size="28" />
        </div>
        <h1 class="font-display text-xl font-semibold">Notifikasi <AppBadge tone="red">{{ unread }} belum dibaca</AppBadge></h1>
      </div>
      <AppButton variant="ghost" @click="bacaSemua">Tandai semua dibaca</AppButton>
    </div>
    <AppBanner tone="red" :message="error" />
    <div class="mb-3 flex gap-2">
      <AppButton :variant="tab === 'semua' ? 'primary' : 'ghost'" @click="tab = 'semua'; muat()">Semua</AppButton>
      <AppButton :variant="tab === 'belum' ? 'primary' : 'ghost'" @click="tab = 'belum'; muat()">Belum dibaca</AppButton>
    </div>
    <p v-if="loading" class="text-sm text-secondary">Memuat…</p>
    <AppEmpty v-else-if="!list.length" message="Tidak ada notifikasi." />
    <AppCard v-for="n in list" :key="n.id" class="cursor-pointer transition hover:shadow" @click="baca(n)">
      <div class="flex items-start gap-3">
        <AppIcon name="info" :size="20" />
        <div class="min-w-0">
          <p class="font-semibold">{{ n.title }} <AppBadge v-if="!n.read_at" tone="blue">baru</AppBadge></p>
          <p class="text-sm text-secondary">{{ n.body }}</p>
          <p class="mt-1 font-mono text-xs text-secondary">{{ n.type }} · {{ waktuRelatif(n.created_at) }}</p>
        </div>
      </div>
    </AppCard>
    <AppPager :meta="meta" @page="muat" />
    <AppCard title="Preferensi" class="mt-4">
      <div v-for="p in prefs" :key="p.type" class="flex items-center justify-between border-b border-surface-low py-2 text-sm last:border-0">
        <span class="font-mono text-xs">{{ p.type }}</span>
        <AppButton variant="ghost" @click="togglePref(p)">{{ p.enabled ? 'Matikan' : 'Nyalakan' }}</AppButton>
      </div>
    </AppCard>
  </div>
</template>
