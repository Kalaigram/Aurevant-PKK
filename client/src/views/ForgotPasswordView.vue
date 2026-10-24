<script setup lang="ts">
import { nextTick, ref } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '../lib/api'
import { useAsync } from '../composables/useAsync'
import AuthLayout from '../components/AuthLayout.vue'
import AppBanner from '../components/ui/AppBanner.vue'
import AppButton from '../components/ui/AppButton.vue'
import AppField from '../components/ui/AppField.vue'
import AppIcon from '../components/ui/AppIcon.vue'
import AppInput from '../components/ui/AppInput.vue'

const router = useRouter()
const { loading, error, run } = useAsync()
const email = ref('')
const emailError = ref('')
const terkirim = ref(false)

function validasi() {
  const nilai = email.value.trim()
  emailError.value = !nilai
    ? 'Email wajib diisi.'
    : /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(nilai)
      ? ''
      : 'Format email belum tepat.'
  return !emailError.value
}

async function kirim() {
  if (!validasi()) {
    await nextTick()
    document.getElementById('lupa-email')?.focus()
    return
  }
  await run(() => api.post('/auth/forgot-password', { email: email.value.trim() }))
  if (!error.value) terkirim.value = true
}

function gunakanEmailLain() {
  terkirim.value = false
  emailError.value = ''
}
</script>

<template>
  <AuthLayout tab="login">
    <template v-if="!terkirim">
      <h2 class="font-display text-2xl font-bold tracking-[-0.02em] text-ink">
        Lupa kata sandi?
      </h2>
      <p class="mt-1.5 text-sm leading-relaxed text-secondary">
        Masukkan email akun Anda. Kami akan mengirim tautan untuk membuat kata sandi baru.
      </p>

      <form novalidate class="mt-5" @submit.prevent="kirim">
        <AppBanner tone="red" :message="error" />
        <AppField label="Email terdaftar" for="lupa-email" required :error="emailError">
          <AppInput
            id="lupa-email"
            v-model="email"
            type="email"
            autocomplete="email"
            placeholder="nama@email.com"
            :invalid="Boolean(emailError)"
            @update:model-value="emailError = ''"
          >
            <template #prefix>
              <AppIcon name="mail" :size="19" class="text-secondary" />
            </template>
          </AppInput>
        </AppField>
        <AppButton type="submit" size="lg" block :loading="loading" class="mt-5">
          <AppIcon name="send" :size="18" />
          Kirim tautan atur ulang
        </AppButton>
      </form>

      <RouterLink
        to="/login"
        class="mt-5 inline-flex items-center gap-1.5 rounded text-sm font-semibold text-primary transition hover:underline focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/55 focus-visible:ring-offset-2"
      >
        <AppIcon name="arrow_back" :size="18" />
        Kembali ke halaman masuk
      </RouterLink>
    </template>

    <template v-else>
      <div class="rounded-2xl border border-ok/25 bg-ok-bg/70 p-5 text-center sm:p-6">
        <span
          class="mx-auto flex h-12 w-12 items-center justify-center rounded-2xl bg-card text-ok-ink shadow-card ring-1 ring-ok/20"
        >
          <AppIcon name="mark_email_read" :size="25" />
        </span>
        <h2 class="mt-4 font-display text-xl font-bold tracking-[-0.02em] text-ok-ink">
          Periksa email Anda
        </h2>
        <p class="mt-1.5 text-sm leading-relaxed text-ok-ink/80">
          Jika <strong class="font-semibold">{{ email.trim() }}</strong> terdaftar, tautan atur
          ulang kata sandi sudah dikirim.
        </p>
        <p class="mt-2 text-xs leading-relaxed text-ok-ink/70">
          Tautan berlaku terbatas. Cek juga folder spam atau promosi.
        </p>
      </div>

      <div class="mt-5 grid gap-2">
        <AppButton size="lg" block @click="router.push('/login')">
          <AppIcon name="login" :size="18" />
          Kembali ke halaman masuk
        </AppButton>
        <AppButton variant="ghost" block @click="gunakanEmailLain">
          Gunakan email lain
        </AppButton>
      </div>
    </template>
  </AuthLayout>
</template>
