<script setup lang="ts">
import { computed, nextTick, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api } from '../lib/api'
import { useAsync } from '../composables/useAsync'
import AuthLayout from '../components/AuthLayout.vue'
import AppBanner from '../components/ui/AppBanner.vue'
import AppButton from '../components/ui/AppButton.vue'
import AppField from '../components/ui/AppField.vue'
import AppIcon from '../components/ui/AppIcon.vue'
import AppInput from '../components/ui/AppInput.vue'

type FieldErrors = {
  email: string
  password: string
  password_confirmation: string
}

const route = useRoute()
const router = useRouter()
const { loading, error, run } = useAsync()
const email = ref((route.query.email as string | undefined) ?? '')
const token = (route.query.token as string | undefined) ?? ''
const sandi = ref('')
const konfirmasi = ref('')
const showPw = ref(false)
const showPw2 = ref(false)
const berhasil = ref(false)
const errors = ref<FieldErrors>({
  email: '',
  password: '',
  password_confirmation: '',
})

const tokenAda = computed(() => token.length > 0)

function validasi() {
  const nilaiEmail = email.value.trim()
  errors.value = {
    email: !nilaiEmail
      ? 'Email wajib diisi.'
      : /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(nilaiEmail)
        ? ''
        : 'Format email belum tepat.',
    password: sandi.value.length >= 8 ? '' : 'Kata sandi minimal 8 karakter.',
    password_confirmation: !konfirmasi.value
      ? 'Ulangi kata sandi baru.'
      : sandi.value === konfirmasi.value
        ? ''
        : 'Ulangi sandi belum sama.',
  }
  return !Object.values(errors.value).some(Boolean)
}

async function simpan() {
  if (!validasi()) {
    await nextTick()
    const pertama = errors.value.email
      ? 'reset-email'
      : errors.value.password
        ? 'reset-pw'
        : 'reset-pw2'
    document.getElementById(pertama)?.focus()
    return
  }
  await run(() =>
    api.post('/auth/reset-password', {
      email: email.value.trim(),
      token,
      password: sandi.value,
      password_confirmation: konfirmasi.value,
    }),
  )
  if (!error.value) berhasil.value = true
}
</script>

<template>
  <AuthLayout tab="login">
    <div
      v-if="!tokenAda"
      class="rounded-2xl border border-warn/30 bg-warn-bg/70 p-5 sm:p-6"
    >
      <span
        class="flex h-11 w-11 items-center justify-center rounded-2xl bg-card text-warn-ink shadow-card ring-1 ring-warn/25"
      >
        <AppIcon name="link_off" :size="23" />
      </span>
      <h2 class="mt-4 font-display text-xl font-bold tracking-[-0.02em] text-warn-ink">
        Tautan tidak lengkap
      </h2>
      <p class="mt-1.5 text-sm leading-relaxed text-warn-ink/80">
        Tautan atur ulang tidak valid atau sudah kedaluwarsa. Minta tautan baru untuk melanjutkan.
      </p>
      <div class="mt-5 grid gap-2">
        <AppButton size="lg" block @click="router.push('/lupa-password')">
          <AppIcon name="send" :size="18" />
          Minta tautan baru
        </AppButton>
        <AppButton variant="ghost" block @click="router.push('/login')">
          Kembali ke halaman masuk
        </AppButton>
      </div>
    </div>

    <div
      v-else-if="berhasil"
      class="rounded-2xl border border-ok/25 bg-ok-bg/70 p-5 text-center sm:p-6"
    >
      <span
        class="mx-auto flex h-12 w-12 items-center justify-center rounded-2xl bg-card text-ok-ink shadow-card ring-1 ring-ok/20"
      >
        <AppIcon name="check_circle" :size="26" />
      </span>
      <h2 class="mt-4 font-display text-xl font-bold tracking-[-0.02em] text-ok-ink">
        Kata sandi berhasil diubah
      </h2>
      <p class="mt-1.5 text-sm leading-relaxed text-ok-ink/80">
        Gunakan kata sandi baru untuk masuk kembali ke ruang kerja Anda.
      </p>
      <AppButton size="lg" block class="mt-5" @click="router.push('/login')">
        <AppIcon name="login" :size="18" />
        Masuk dengan kata sandi baru
      </AppButton>
    </div>

    <template v-else>
      <h2 class="font-display text-2xl font-bold tracking-[-0.02em] text-ink">
        Atur ulang kata sandi
      </h2>
      <p class="mt-1.5 text-sm leading-relaxed text-secondary">
        Buat kata sandi baru minimal 8 karakter untuk mengamankan akun Anda.
      </p>

      <form novalidate class="mt-5" @submit.prevent="simpan">
        <AppBanner tone="red" :message="error" />

        <div class="space-y-4">
          <AppField label="Email terdaftar" for="reset-email" required :error="errors.email">
            <AppInput
              id="reset-email"
              v-model="email"
              type="email"
              autocomplete="email"
              placeholder="nama@email.com"
              :invalid="Boolean(errors.email)"
              @update:model-value="errors.email = ''"
            >
              <template #prefix>
                <AppIcon name="mail" :size="19" class="text-secondary" />
              </template>
            </AppInput>
          </AppField>

          <AppField
            label="Kata sandi baru"
            for="reset-pw"
            required
            :error="errors.password"
            hint="Minimal 8 karakter."
          >
            <AppInput
              id="reset-pw"
              v-model="sandi"
              :type="showPw ? 'text' : 'password'"
              autocomplete="new-password"
              placeholder="Kata sandi baru"
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
            label="Ulangi kata sandi baru"
            for="reset-pw2"
            required
            :error="errors.password_confirmation"
          >
            <AppInput
              id="reset-pw2"
              v-model="konfirmasi"
              :type="showPw2 ? 'text' : 'password'"
              autocomplete="new-password"
              placeholder="Ketik ulang kata sandi baru"
              :invalid="Boolean(errors.password_confirmation)"
              @update:model-value="errors.password_confirmation = ''"
            >
              <template #prefix>
                <AppIcon name="lock_reset" :size="19" class="text-secondary" />
              </template>
              <template #suffix>
                <button
                  type="button"
                  class="-mr-1 flex h-9 w-9 items-center justify-center rounded-lg text-secondary transition hover:bg-surface-low hover:text-ink focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/55"
                  :aria-label="
                    showPw2 ? 'Sembunyikan ulangi sandi' : 'Tampilkan ulangi sandi'
                  "
                  :aria-pressed="showPw2"
                  @click="showPw2 = !showPw2"
                >
                  <AppIcon :name="showPw2 ? 'visibility_off' : 'visibility'" :size="19" />
                </button>
              </template>
            </AppInput>
          </AppField>
        </div>

        <AppButton type="submit" size="lg" block :loading="loading" class="mt-5">
          <AppIcon name="lock_reset" :size="18" />
          Simpan kata sandi baru
        </AppButton>
      </form>

      <RouterLink
        to="/lupa-password"
        class="mt-5 inline-flex items-center gap-1.5 rounded text-sm font-semibold text-primary transition hover:underline focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/55 focus-visible:ring-offset-2"
      >
        <AppIcon name="arrow_back" :size="18" />
        Minta tautan baru
      </RouterLink>
    </template>
  </AuthLayout>
</template>
