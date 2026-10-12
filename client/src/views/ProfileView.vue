<script setup lang="ts">
import { useAuthStore } from '../stores/auth'
import AppCard from '../components/ui/AppCard.vue'
import AppButton from '../components/ui/AppButton.vue'

const auth = useAuthStore()

async function keluar() {
  await auth.logout()
  window.location.href = '/login'
}
</script>

<template>
  <div class="mx-auto max-w-xl">
    <h1 class="mb-4 text-xl font-bold">Profil saya</h1>
    <AppCard title="Data diri">
      <dl class="space-y-2 text-sm">
        <div class="flex justify-between"><dt class="text-slate-500">Nama</dt><dd class="font-medium">{{ auth.user?.name }}</dd></div>
        <div class="flex justify-between"><dt class="text-slate-500">Email</dt><dd class="font-medium">{{ auth.user?.email }}</dd></div>
        <div class="flex justify-between"><dt class="text-slate-500">No. HP</dt><dd class="font-medium">{{ auth.user?.phone ?? '–' }}</dd></div>
      </dl>
      <p class="mt-3 text-xs text-slate-500">Hak akses diatur oleh pemilik perusahaan.</p>
    </AppCard>
    <AppButton variant="danger" @click="keluar">Keluar dari akun</AppButton>
  </div>
</template>
