<script setup lang="ts">
import AppIcon from './ui/AppIcon.vue'

defineProps<{ tab: 'login' | 'register' }>()
</script>

<template>
  <div class="flex min-h-screen items-center justify-center bg-surface-low p-4">
    <div class="flex w-full max-w-4xl items-stretch overflow-hidden rounded-xl bg-card shadow-lg">
      <div class="hidden w-2/5 flex-col justify-between bg-primary p-8 text-white lg:flex">
        <div class="flex items-center gap-2">
          <AppIcon name="corporate_fare" :size="28" />
          <p class="font-display text-xl font-bold">Aurevant PKK</p>
        </div>
        <div>
          <p class="font-display text-2xl font-semibold leading-snug">Operasional multi-cabang dalam satu layar.</p>
          <p class="mt-2 text-sm text-emerald-50/80">Kasir, stok, jasa, SDM, dan keuangan — terisolasi per perusahaan.</p>
        </div>
        <div class="flex items-center gap-2 text-sm text-emerald-50/80">
          <AppIcon name="verified_user" :size="18" />
          <p>Token Sanctum per sesi, hak akses per cabang.</p>
        </div>
      </div>
      <div class="w-full p-6 sm:p-8 lg:w-3/5">
        <div class="mb-6 flex gap-2 rounded-lg bg-surface-low p-1">
          <RouterLink
            to="/login"
            class="flex flex-1 items-center justify-center gap-2 rounded-lg px-4 py-2 text-sm font-semibold transition-colors"
            :class="tab === 'login' ? 'bg-card text-primary shadow-sm' : 'text-secondary'"
          ><AppIcon name="login" :size="18" />Masuk</RouterLink>
          <RouterLink
            to="/register"
            class="flex flex-1 items-center justify-center gap-2 rounded-lg px-4 py-2 text-sm font-semibold transition-colors"
            :class="tab === 'register' ? 'bg-card text-primary shadow-sm' : 'text-secondary'"
          ><AppIcon name="person_add" :size="18" />Daftar</RouterLink>
        </div>
        <slot />
      </div>
    </div>
  </div>
</template>
