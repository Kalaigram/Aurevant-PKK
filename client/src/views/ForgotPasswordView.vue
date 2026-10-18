<script setup lang="ts">
import { ref } from 'vue'
import { api } from '../lib/api'
import { useAsync } from '../composables/useAsync'
import AuthLayout from '../components/AuthLayout.vue'
import AppButton from '../components/ui/AppButton.vue'
import AppBanner from '../components/ui/AppBanner.vue'
import AppIcon from '../components/ui/AppIcon.vue'

const { loading, error, run } = useAsync()
const email = ref('')
const terkirim = ref(false)

async function kirim() {
  await run(() => api.post('/auth/forgot-password', { email: email.value.trim() }))
  if (!error.value) terkirim.value = true
}
</script>

<template>
  <AuthLayout tab="login">
    <h2 class="font-display text-xl font-semibold">Lupa kata sandi?</h2>
    <p class="mb-4 mt-1 text-sm text-secondary">
      Masukkan email akun Anda. Kami kirim tautan untuk mengatur ulang kata sandi.
    </p>
    <AppBanner tone="red" :message="error" />
    <AppBanner
      tone="green"
      :message="
        terkirim
          ? 'Jika email terdaftar, tautan atur ulang kata sandi sudah dikirim. Cek kotak masuk (juga folder spam).'
          : ''
      "
    />
    <form novalidate @submit.prevent="kirim">
      <label class="block text-sm font-medium" for="lupa-email">
        Email terdaftar <span class="text-danger">*</span>
      </label>
      <div class="relative mt-1 flex items-center">
        <AppIcon name="mail" :size="20" class="pointer-events-none absolute left-3 text-secondary" />
        <input
          id="lupa-email"
          v-model="email"
          type="email"
          class="w-full rounded-lg bg-card py-2.5 pl-10 pr-3 text-sm shadow-sm outline-none transition focus:bg-surface-low"
          placeholder="nama@email.com"
          autocomplete="email"
        />
      </div>
      <AppButton type="submit" :loading="loading" class="mt-4 w-full">
        <AppIcon name="send" :size="18" />Kirim tautan atur ulang
      </AppButton>
    </form>
    <RouterLink
      to="/login"
      class="mt-4 inline-flex items-center gap-1 text-sm font-semibold text-primary hover:underline"
    >
      <AppIcon name="arrow_back" :size="18" />Kembali ke halaman masuk
    </RouterLink>
  </AuthLayout>
</template>
