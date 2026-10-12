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
const form = ref({ name: '', email: '', phone: '', password: '', password_confirmation: '' })

async function daftar() {
  const ok = await run(() => auth.register({ ...form.value }))
  if (ok !== null) router.push('/companies')
}
</script>

<template>
  <div class="flex min-h-screen items-center justify-center bg-primary-900 p-4">
    <form class="w-full max-w-md rounded-2xl bg-white p-8" @submit.prevent="daftar">
      <h1 class="mb-1 text-center text-2xl font-extrabold text-gray-900">Aurevant PKK</h1>
      <p class="mb-5 text-center text-sm text-slate-500">Buat akun, gratis untuk memulai.</p>
      <AppBanner tone="red" :message="error" />
      <AppField label="Nama lengkap">
        <input v-model="form.name" class="w-full rounded-lg border border-slate-300 px-3 py-2.5" autocomplete="name" />
      </AppField>
      <div class="mt-3">
        <AppField label="Email">
          <input v-model="form.email" type="email" class="w-full rounded-lg border border-slate-300 px-3 py-2.5" autocomplete="email" />
        </AppField>
      </div>
      <div class="mt-3">
        <AppField label="Nomor HP (boleh dikosongkan)">
          <input v-model="form.phone" class="w-full rounded-lg border border-slate-300 px-3 py-2.5" autocomplete="tel" />
        </AppField>
      </div>
      <div class="mt-3 grid grid-cols-2 gap-3 max-md:grid-cols-1">
        <AppField label="Kata sandi (min. 8)">
          <input v-model="form.password" type="password" class="w-full rounded-lg border border-slate-300 px-3 py-2.5" autocomplete="new-password" />
        </AppField>
        <AppField label="Ulangi sandi">
          <input v-model="form.password_confirmation" type="password" class="w-full rounded-lg border border-slate-300 px-3 py-2.5" autocomplete="new-password" />
        </AppField>
      </div>
      <AppButton type="submit" :loading="loading" class="mt-5 w-full">Daftar</AppButton>
      <p class="mt-4 text-center text-sm text-slate-500">
        Sudah punya akun? <RouterLink to="/login" class="font-semibold text-primary-700">Masuk</RouterLink>
      </p>
    </form>
  </div>
</template>
