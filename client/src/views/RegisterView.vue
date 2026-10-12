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
const form = ref({ name: '', email: '', phone: '', password: '', password_confirmation: '' })

async function daftar() {
  const ok = await run(() => auth.register({ ...form.value }))
  if (ok !== null) router.push('/companies')
}
</script>

<template>
  <AuthLayout tab="register">
    <form @submit.prevent="daftar">
      <h2 class="font-display text-xl font-semibold">Buat akun usaha</h2>
      <p class="mb-4 mt-1 text-sm text-secondary">Gratis untuk memulai, tanpa kartu kredit.</p>
      <AppBanner tone="red" :message="error" />
      <AppField label="Nama lengkap">
        <input v-model="form.name" class="w-full rounded-lg border border-outline bg-card px-3 py-2.5 focus:border-primary focus:outline-none" autocomplete="name" />
      </AppField>
      <div class="mt-3 grid grid-cols-2 gap-3 max-sm:grid-cols-1">
        <AppField label="Email">
          <input v-model="form.email" type="email" class="w-full rounded-lg border border-outline bg-card px-3 py-2.5 focus:border-primary focus:outline-none" autocomplete="email" />
        </AppField>
        <AppField label="No. HP (opsional)">
          <input v-model="form.phone" class="w-full rounded-lg border border-outline bg-card px-3 py-2.5 focus:border-primary focus:outline-none" autocomplete="tel" />
        </AppField>
      </div>
      <div class="mt-3 grid grid-cols-2 gap-3 max-sm:grid-cols-1">
        <AppField label="Kata sandi (min. 8)">
          <input v-model="form.password" type="password" class="w-full rounded-lg border border-outline bg-card px-3 py-2.5 focus:border-primary focus:outline-none" autocomplete="new-password" />
        </AppField>
        <AppField label="Ulangi sandi">
          <input v-model="form.password_confirmation" type="password" class="w-full rounded-lg border border-outline bg-card px-3 py-2.5 focus:border-primary focus:outline-none" autocomplete="new-password" />
        </AppField>
      </div>
      <AppButton type="submit" :loading="loading" class="mt-5 w-full">Daftar</AppButton>
    </form>
  </AuthLayout>
</template>
