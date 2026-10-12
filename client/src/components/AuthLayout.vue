<script setup lang="ts">
import AppIcon from './ui/AppIcon.vue'

defineProps<{ tab: 'login' | 'register' }>()
</script>

<template>
  <div class="flex min-h-screen items-center justify-center bg-surface-low p-4">
    <div class="mx-auto my-auto flex w-full max-w-5xl items-stretch overflow-hidden rounded-xl bg-card shadow-xl">
      <div class="relative hidden w-5/12 flex-col justify-between overflow-hidden bg-primary p-8 text-white lg:flex">
        <div class="space-y-4">
          <div class="inline-flex items-center gap-2 rounded-full bg-white/15 px-3 py-1">
            <AppIcon name="corporate_fare" :size="18" />
            <span class="text-xs font-semibold uppercase tracking-wider">Enterprise Edition</span>
          </div>
          <div>
            <h1 class="font-display text-2xl font-bold tracking-tight">Aurevant PKK</h1>
            <p class="mt-1 text-sm text-emerald-50/85">Platform Manajemen Bisnis Multi-Cabang</p>
          </div>
        </div>
        <div class="space-y-2 rounded-xl bg-white/10 p-4">
          <p class="text-xs font-semibold uppercase tracking-wider text-emerald-50/85">Satu akun untuk semua cabang</p>
          <div class="grid grid-cols-2 gap-2 text-sm">
            <div class="rounded-lg bg-white/10 p-2"><p class="font-display font-semibold">Kasir & Stok</p></div>
            <div class="rounded-lg bg-white/10 p-2"><p class="font-display font-semibold">SDM & Gaji</p></div>
          </div>
        </div>
        <div class="flex items-center gap-2">
          <AppIcon name="verified_user" :size="18" />
          <p class="text-xs text-emerald-50/85">Data tiap perusahaan terisolasi penuh.</p>
        </div>
      </div>
      <div class="flex w-full flex-col justify-center p-6 sm:p-8 lg:w-7/12">
        <div class="mb-6 flex gap-1 rounded-xl bg-surface-low p-1">
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
        <div class="mt-6 flex items-center justify-between gap-2 rounded-lg bg-surface-low p-3 text-xs text-secondary">
          <span class="flex items-center gap-1.5"><AppIcon name="sync_alt" :size="16" />Token tersimpan aman di perangkat ini</span>
          <span class="font-semibold text-primary">Tenant Secured</span>
        </div>
      </div>
    </div>
  </div>
</template>
