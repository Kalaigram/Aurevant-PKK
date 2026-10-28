<script setup lang="ts">
import { nextTick, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { useAsync } from '../composables/useAsync'
import AuthLayout from '../components/AuthLayout.vue'
import AppBadge from '../components/ui/AppBadge.vue'
import AppBanner from '../components/ui/AppBanner.vue'
import AppButton from '../components/ui/AppButton.vue'
import AppField from '../components/ui/AppField.vue'
import AppIcon from '../components/ui/AppIcon.vue'
import AppInput from '../components/ui/AppInput.vue'

const router = useRouter()
const auth = useAuthStore()
const { loading, error, run } = useAsync()
const login = ref('')
const password = ref('')
const kode2fa = ref('')
const butuh2fa = ref(false)
const showPw = ref(false)
const errors = ref({ login: '', password: '' })

const demoAktif =
  import.meta.env.DEV || import.meta.env.VITE_SHOW_DEMO_CREDENTIALS === 'true'
const akunDemo = demoAktif
  ? [
      { login: 'pemilik@demo.id', password: 'password123', peran: 'Pemilik usaha · akses penuh' },
      { login: 'kasir@demo.id', password: 'password123', peran: 'Kasir · transaksi & pengajuan' },
      { login: 'teknisi@demo.id', password: 'password123', peran: 'Teknisi · order jasa' },
      { login: 'keuangan@demo.id', password: 'password123', peran: 'Keuangan · biaya & laporan' },
      {
        login: 'superadmin@demo.id',
        password: 'password123',
        peran: 'Admin platform · semua tenant',
      },
    ]
  : []

function validasi() {
  const nilai = login.value.trim()
  errors.value = {
    login: !nilai
      ? 'Masukkan email atau nomor HP.'
      : nilai.includes('@') && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(nilai)
        ? 'Format email belum tepat.'
        : '',
    password: password.value ? '' : 'Masukkan kata sandi.',
  }
  return !errors.value.login && !errors.value.password
}

async function masuk() {
  if (!validasi()) {
    await nextTick()
    document.getElementById(errors.value.login ? 'login-id' : 'login-pw')?.focus()
    return
  }
  const ok = await run(() =>
    auth.login(login.value.trim(), password.value, kode2fa.value.trim() || undefined),
  )
  if (ok !== null) {
    router.push('/companies')
    return
  }
  if (error.value.toLowerCase().includes('2fa')) {
    butuh2fa.value = true
    await nextTick()
    document.getElementById('login-2fa')?.focus()
  }
}

function pakaiDemo(akun: (typeof akunDemo)[number]) {
  login.value = akun.login
  password.value = akun.password
  errors.value = { login: '', password: '' }
}
</script>

<template>
  <AuthLayout tab="login">
    <h2 class="font-display text-2xl font-bold tracking-[-0.02em] text-ink">
      Masuk ke ruang kerja
    </h2>
    <p class="mt-1.5 text-sm leading-relaxed text-secondary">
      Kelola operasional, tim, dan laporan perusahaan Anda dari satu tempat.
    </p>

    <form novalidate class="mt-5" @submit.prevent="masuk">
      <AppBanner tone="red" :message="error" />

      <div class="space-y-4">
        <AppField
          label="Email atau nomor HP"
          for="login-id"
          required
          :error="errors.login"
          hint="Gunakan kredensial akun yang sudah terdaftar."
        >
          <AppInput
            id="login-id"
            v-model="login"
            type="text"
            autocomplete="username"
            placeholder="nama@email.com atau 0812…"
            :invalid="Boolean(errors.login)"
            @update:model-value="errors.login = ''"
          >
            <template #prefix>
              <AppIcon name="alternate_email" :size="19" class="text-secondary" />
            </template>
          </AppInput>
        </AppField>

        <AppField
          label="Kata sandi"
          for="login-pw"
          required
          :error="errors.password"
        >
          <template #action>
            <RouterLink
              to="/lupa-password"
              class="rounded text-xs font-semibold text-primary transition hover:underline focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/55 focus-visible:ring-offset-2"
            >
              Lupa sandi?
            </RouterLink>
          </template>
          <AppInput
            id="login-pw"
            v-model="password"
            :type="showPw ? 'text' : 'password'"
            autocomplete="current-password"
            placeholder="Kata sandi akun"
            :invalid="Boolean(errors.password)"
            @update:model-value="errors.password = ''"
          >
            <template #prefix>
              <AppIcon name="lock" :size="19" class="text-secondary" />
            </template>
            <template #suffix>
              <button
                type="button"
                class="-mr-1 flex h-9 w-9 items-center justify-center rounded-lg text-secondary transition hover:bg-surface-low hover:text-ink focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/55"
                :aria-label="showPw ? 'Sembunyikan kata sandi' : 'Tampilkan kata sandi'"
                :aria-pressed="showPw"
                @click="showPw = !showPw"
              >
                <AppIcon :name="showPw ? 'visibility_off' : 'visibility'" :size="19" />
              </button>
            </template>
          </AppInput>
        </AppField>

        <AppField
          v-if="butuh2fa"
          label="Kode 2FA"
          for="login-2fa"
          required
          hint="Masukkan 6 digit dari aplikasi authenticator atau kode pemulihan."
        >
          <AppInput
            id="login-2fa"
            v-model="kode2fa"
            type="text"
            inputmode="numeric"
            autocomplete="one-time-code"
            maxlength="50"
            placeholder="123456"
          >
            <template #prefix>
              <AppIcon name="shield_lock" :size="19" class="text-secondary" />
            </template>
          </AppInput>
        </AppField>
      </div>

      <AppButton type="submit" size="lg" block :loading="loading" class="mt-5">
        <AppIcon name="login" :size="18" />
        Masuk ke perusahaan
      </AppButton>
    </form>

    <details
      v-if="demoAktif"
      class="group mt-5 rounded-xl border border-outline/70 bg-surface-low/60 p-3.5"
    >
      <summary
        class="flex cursor-pointer list-none items-center justify-between gap-3 rounded-lg text-xs font-semibold text-ink focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/55 focus-visible:ring-offset-2"
      >
        <span class="flex items-center gap-2">
          <AppIcon name="science" :size="17" class="text-primary" />
          Akun demo
          <AppBadge tone="gray">Dev</AppBadge>
        </span>
        <AppIcon
          name="expand_more"
          :size="18"
          class="text-secondary transition-transform group-open:rotate-180"
        />
      </summary>
      <p class="mt-2 text-xs leading-relaxed text-secondary">
        Pilih peran untuk mengisi kredensial. Sandi semua akun:
        <code class="rounded bg-card px-1.5 py-0.5 font-mono text-2xs text-ink">{{
          akunDemo[0]?.password
        }}</code>
      </p>
      <div class="mt-3 grid gap-1.5">
        <button
          v-for="akun in akunDemo"
          :key="akun.login"
          type="button"
          class="flex min-h-11 items-center justify-between gap-3 rounded-lg border border-outline/60 bg-card px-3 py-2 text-left transition hover:border-outline-strong hover:bg-surface-raised focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/55"
          @click="pakaiDemo(akun)"
        >
          <span class="min-w-0">
            <span class="block truncate font-mono text-xs text-ink">{{ akun.login }}</span>
            <span class="mt-0.5 block text-2xs text-secondary">{{ akun.peran }}</span>
          </span>
          <AppIcon name="north_east" :size="17" class="shrink-0 text-secondary" />
        </button>
      </div>
    </details>
  </AuthLayout>
</template>
