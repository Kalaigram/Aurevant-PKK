<script setup lang="ts">
import { computed, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from './stores/auth'
import { useAccessStore } from './stores/access'
import AppIcon from './components/ui/AppIcon.vue'

const route = useRoute()
const auth = useAuthStore()
const access = useAccessStore()
const companyId = computed(() => Number(route.params.companyId ?? auth.companyId ?? 0))

watch(companyId, (id) => {
  if (id) void access.load(id, auth.token)
}, { immediate: true })

async function keluar() {
  await auth.logout()
  access.reset()
  window.location.href = '/login'
}

const APPROVAL_PERMS = [
  'transaksi.buat', 'transaksi.void', 'transaksi.diskon',
  'retur.buat', 'retur.setujui', 'po.buat', 'po.setujui',
  'stok.transfer', 'stok.opname',
]

const menu = computed(() => {
  const id = companyId.value
  if (!id) return []
  const c = `/c/${id}`
  const items = [
    { to: `${c}/dashboard`, label: 'Dashboard', icon: 'dashboard' },
    { to: `${c}/notifications`, label: 'Notifikasi', icon: 'notifications' },
    { to: `${c}/approvals`, label: 'Persetujuan', icon: 'fact_check', perms: APPROVAL_PERMS },
    { to: `${c}/attendance`, label: 'Absensi', icon: 'fingerprint' },
    { to: `${c}/leaves`, label: 'Cuti', icon: 'event_note' },
    { to: `${c}/slip`, label: 'Slip Gaji', icon: 'receipt_long' },
    { to: `${c}/card`, label: 'Kartu', icon: 'badge' },
    { to: `${c}/subscription`, label: 'Langganan', icon: 'workspace_premium' },
    { to: `${c}/profile`, label: 'Profil', icon: 'account_circle' },
  ]
  return items.filter((m) => !('perms' in m) || !access.loaded || access.can(...(m.perms as string[])))
})
</script>

<template>
  <div v-if="!auth.loggedIn"><RouterView /></div>
  <div v-else class="flex min-h-screen">
    <nav class="sticky top-0 flex h-screen w-60 flex-col gap-1 bg-primary p-4 text-white max-md:fixed max-md:inset-x-0 max-md:bottom-0 max-md:top-auto max-md:h-auto max-md:flex-row max-md:overflow-x-auto max-md:p-2">
      <div class="px-2 pb-5 pt-2 max-md:hidden">
        <p class="font-display text-lg font-bold">Aurevant PKK</p>
        <p class="text-xs text-emerald-100/80">Operasional Multi-Cabang</p>
      </div>
      <RouterLink
        v-for="m in menu"
        :key="m.to"
        :to="m.to"
        class="flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm text-emerald-50/90 transition-colors hover:bg-white/10 hover:text-white max-md:whitespace-nowrap"
        active-class="bg-white/15! text-white!"
      ><AppIcon :name="m.icon" :size="20" />{{ m.label }}</RouterLink>
      <div class="flex-1 max-md:hidden" />
      <RouterLink to="/companies" class="flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm text-emerald-50/90 hover:bg-white/10 max-md:whitespace-nowrap">
        <AppIcon name="domain" :size="20" />Ganti Perusahaan
      </RouterLink>
      <a href="#" class="flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm text-emerald-50/90 hover:bg-white/10 max-md:whitespace-nowrap" @click.prevent="keluar">
        <AppIcon name="logout" :size="20" />Keluar
      </a>
    </nav>
    <main class="min-w-0 flex-1 p-4 md:p-6 max-md:pb-24">
      <div class="mb-4 flex flex-wrap items-center justify-between gap-2">
        <strong class="font-display">{{ auth.companyName ?? 'Perusahaan #' + (auth.companyId ?? '-') }}</strong>
        <span class="text-sm text-secondary">{{ auth.user?.name }}</span>
      </div>
      <RouterView :key="$route.fullPath" />
    </main>
  </div>
</template>
