<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from './stores/auth'

const route = useRoute()
const auth = useAuthStore()
const companyId = computed(() => Number(route.params.companyId ?? auth.companyId ?? 0))

async function keluar() {
  await auth.logout()
  window.location.href = '/login'
}

const menu = computed(() => {
  const id = companyId.value
  if (!id) return []
  const c = `/c/${id}`
  return [
    { to: `${c}/dashboard`, label: 'Dashboard' },
    { to: `${c}/notifications`, label: 'Notifikasi' },
    { to: `${c}/approvals`, label: 'Persetujuan' },
    { to: `${c}/attendance`, label: 'Absensi' },
    { to: `${c}/leaves`, label: 'Cuti' },
    { to: `${c}/slip`, label: 'Slip Gaji' },
    { to: `${c}/card`, label: 'Kartu' },
    { to: `${c}/subscription`, label: 'Langganan' },
    { to: `${c}/profile`, label: 'Profil' },
  ]
})
</script>

<template>
  <div v-if="!auth.loggedIn"><RouterView /></div>
  <div v-else class="flex min-h-screen">
    <nav class="sticky top-0 flex h-screen w-56 flex-col gap-1 bg-primary-900 p-4 text-white max-md:fixed max-md:inset-x-0 max-md:bottom-0 max-md:top-auto max-md:h-auto max-md:flex-row max-md:overflow-x-auto max-md:p-2">
      <div class="px-2 pb-4 pt-2 text-lg font-extrabold max-md:hidden">Aurevant PKK</div>
      <RouterLink
        v-for="m in menu"
        :key="m.to"
        :to="m.to"
        class="flex items-center justify-between rounded-lg px-3 py-2 text-sm text-emerald-100 hover:bg-white/10 hover:text-white max-md:whitespace-nowrap"
        active-class="bg-white/15! text-white!"
      >{{ m.label }}</RouterLink>
      <div class="flex-1 max-md:hidden" />
      <RouterLink to="/companies" class="rounded-lg px-3 py-2 text-sm text-emerald-100 hover:bg-white/10 max-md:whitespace-nowrap">Ganti Perusahaan</RouterLink>
      <a href="#" class="rounded-lg px-3 py-2 text-sm text-emerald-100 hover:bg-white/10 max-md:whitespace-nowrap" @click.prevent="keluar">Keluar</a>
    </nav>
    <main class="min-w-0 flex-1 p-5 max-md:pb-20">
      <div class="mb-4 flex flex-wrap items-center justify-between gap-2">
        <strong>{{ auth.companyName ?? 'Perusahaan #' + (auth.companyId ?? '-') }}</strong>
        <span class="text-sm text-slate-500">{{ auth.user?.name }}</span>
      </div>
      <RouterView :key="$route.fullPath" />
    </main>
  </div>
</template>
