<script setup lang="ts">
import { useAuthStore } from '../stores/auth'
import AppCard from '../components/ui/AppCard.vue'
import AppButton from '../components/ui/AppButton.vue'
import AppIcon from '../components/ui/AppIcon.vue'

const auth = useAuthStore()

async function keluar() {
  await auth.logout()
  window.location.href = '/login'
}
</script>

<template>
  <div class="mx-auto max-w-xl">
    <div class="mb-4 flex items-center gap-3">
      <div class="flex h-12 w-12 items-center justify-center rounded-xl bg-primary-container text-on-primary">
        <AppIcon name="account_circle" :size="28" />
      </div>
      <h1 class="font-display text-xl font-semibold">Profil saya</h1>
    </div>
    <AppCard title="Data diri">
      <dl class="space-y-2 text-sm">
        <div class="flex justify-between"><dt class="text-secondary">Nama</dt><dd class="font-medium">{{ auth.user?.name }}</dd></div>
        <div class="flex justify-between"><dt class="text-secondary">Email</dt><dd class="font-medium">{{ auth.user?.email }}</dd></div>
        <div class="flex justify-between"><dt class="text-secondary">No. HP</dt><dd class="font-medium">{{ auth.user?.phone ?? '–' }}</dd></div>
      </dl>
      <p class="mt-3 text-xs text-secondary">Hak akses diatur oleh pemilik perusahaan.</p>
    </AppCard>
    <AppButton variant="danger" @click="keluar">Keluar dari akun</AppButton>
  </div>
</template>
