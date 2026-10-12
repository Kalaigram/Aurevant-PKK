<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { useAsync } from '../composables/useAsync'
import AuthLayout from '../components/AuthLayout.vue'
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
  <AuthLayout tab="login">
    <form @submit.prevent="masuk">
      <h2 class="font-display text-xl font-semibold">Selamat datang kembali</h2>
      <p class="mb-4 mt-1 text-sm text-secondary">Masuk dengan email atau nomor HP terdaftar.</p>
      <AppBanner tone="red" :message="error" />
      <AppField label="Email atau nomor HP">
        <input v-model="login" class="w-full rounded-lg border border-outline bg-card px-3 py-2.5 focus:border-primary focus:outline-none" autocomplete="username" />
      </AppField>
      <div class="mt-3">
        <AppField label="Kata sandi">
          <input v-model="password" type="password" class="w-full rounded-lg border border-outline bg-card px-3 py-2.5 focus:border-primary focus:outline-none" autocomplete="current-password" />
        </AppField>
      </div>
      <AppButton type="submit" :loading="loading" class="mt-5 w-full">Masuk</AppButton>
    </form>
  </AuthLayout>
</template>
