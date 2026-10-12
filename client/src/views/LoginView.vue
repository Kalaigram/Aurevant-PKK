<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { useAsync } from '../composables/useAsync'
import AppButton from '../components/ui/AppButton.vue'
import AppField from '../components/ui/AppField.vue'
import AppBanner from '../components/ui/AppBanner.vue'

const router = useRouter()
const auth = useAuthStore()
const { loading, error, run } = useAsync()
const login = ref('')
const password = ref('')

async function masuk() {
  const ok = await run(() => auth.login(login.value.trim(), password.value))
  if (ok !== null) router.push('/companies')
}
</script>

<template>
  <div class="flex min-h-screen items-center justify-center bg-primary-900 p-4">
    <form class="w-full max-w-md rounded-2xl bg-white p-8" @submit.prevent="masuk">
      <h1 class="mb-1 text-center text-2xl font-extrabold text-gray-900">Aurevant PKK</h1>
      <p class="mb-5 text-center text-sm text-slate-500">Masuk untuk mengelola usaha Anda.</p>
      <AppBanner tone="red" :message="error" />
      <AppField label="Email atau nomor HP">
        <input v-model="login" class="w-full rounded-lg border border-slate-300 px-3 py-2.5" placeholder="nama@email.com" autocomplete="username" />
      </AppField>
      <div class="mt-3">
        <AppField label="Kata sandi">
          <input v-model="password" type="password" class="w-full rounded-lg border border-slate-300 px-3 py-2.5" autocomplete="current-password" />
        </AppField>
      </div>
      <AppButton type="submit" :loading="loading" class="mt-5 w-full">Masuk</AppButton>
      <p class="mt-4 text-center text-sm text-slate-500">
        Belum punya akun? <RouterLink to="/register" class="font-semibold text-primary-700">Daftar di sini</RouterLink>
      </p>
    </form>
  </div>
</template>
