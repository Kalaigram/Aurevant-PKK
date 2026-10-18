<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { useAsync } from '../composables/useAsync'
import AuthLayout from '../components/AuthLayout.vue'
import AppButton from '../components/ui/AppButton.vue'
import AppBanner from '../components/ui/AppBanner.vue'
import AppIcon from '../components/ui/AppIcon.vue'

const router = useRouter()
const auth = useAuthStore()
const { loading, error, run } = useAsync()
const login = ref('')
const password = ref('')
const showPw = ref(false)
const ingat = ref(true)

async function masuk() {
  const ok = await run(() => auth.login(login.value.trim(), password.value))
  if (ok !== null) router.push('/companies')
}
</script>

<template>
  <AuthLayout tab="login">
    <form novalidate @submit.prevent="masuk">
      <h2 class="font-display text-xl font-semibold">Selamat datang di portal bisnis</h2>
      <p class="mb-4 mt-1 text-sm text-secondary">Masuk dengan kredensial terdaftar untuk mengelola usaha Anda.</p>
      <AppBanner tone="red" :message="error" />
      <label class="block text-sm font-medium" for="login-id">Email atau nomor HP <span class="text-danger">*</span></label>
      <div class="relative mt-1 flex items-center">
        <AppIcon name="alternate_email" :size="20" class="pointer-events-none absolute left-3 text-secondary" />
        <input
          id="login-id" v-model="login"
          class="w-full rounded-lg bg-card py-2.5 pl-10 pr-3 text-sm shadow-sm outline-none transition focus:bg-surface-low"
          placeholder="nama@email.com atau 0812…" autocomplete="username"
        />
      </div>
      <div class="mt-3 flex items-center justify-between">
        <label class="text-sm font-medium" for="login-pw">Kata sandi <span class="text-danger">*</span></label>
        <RouterLink to="/lupa-password" class="text-xs font-semibold text-primary hover:underline">Lupa sandi?</RouterLink>
      </div>
      <div class="relative mt-1 flex items-center">
        <AppIcon name="lock" :size="20" class="pointer-events-none absolute left-3 text-secondary" />
        <input
          id="login-pw" v-model="password" :type="showPw ? 'text' : 'password'"
          class="w-full rounded-lg bg-card py-2.5 pl-10 pr-10 text-sm shadow-sm outline-none transition focus:bg-surface-low"
          placeholder="Kata sandi akun" autocomplete="current-password"
        />
        <button type="button" :aria-label="showPw ? 'Sembunyikan sandi' : 'Tampilkan sandi'" class="absolute right-3 rounded p-1 text-secondary hover:text-ink" @click="showPw = !showPw">
          <AppIcon :name="showPw ? 'visibility_off' : 'visibility'" :size="20" />
        </button>
      </div>
      <label class="mt-3 flex cursor-pointer select-none items-center gap-2 text-sm text-secondary">
        <input v-model="ingat" type="checkbox" class="h-4 w-4 accent-primary" />
        Ingat saya di perangkat ini
      </label>
      <AppButton type="submit" :loading="loading" class="mt-4 w-full">
        <AppIcon name="arrow_forward" :size="18" />Masuk ke perusahaan
      </AppButton>
      <details class="mt-4 rounded-lg bg-surface-low p-3 text-xs text-secondary">
        <summary class="cursor-pointer font-semibold text-primary">Coba akun demo (sandi: password123)</summary>
        <ul class="mt-2 space-y-1">
          <li><button type="button" class="font-mono text-primary underline" @click="login = 'pemilik@demo.id'">pemilik@demo.id</button> — akses penuh</li>
          <li><button type="button" class="font-mono text-primary underline" @click="login = 'kasir@demo.id'">kasir@demo.id</button> — kasir & pengajuan</li>
          <li><button type="button" class="font-mono text-primary underline" @click="login = 'teknisi@demo.id'">teknisi@demo.id</button> — eksekusi jasa</li>
          <li><button type="button" class="font-mono text-primary underline" @click="login = 'keuangan@demo.id'">keuangan@demo.id</button> — biaya & laporan</li>
          <li><button type="button" class="font-mono text-primary underline" @click="login = 'superadmin@demo.id'">superadmin@demo.id</button> — admin platform</li>
        </ul>
      </details>
    </form>
  </AuthLayout>
</template>
