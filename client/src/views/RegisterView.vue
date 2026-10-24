<script setup lang="ts">
import { nextTick, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { useAsync } from '../composables/useAsync'
import AuthLayout from '../components/AuthLayout.vue'
import AppBanner from '../components/ui/AppBanner.vue'
import AppButton from '../components/ui/AppButton.vue'
import AppField from '../components/ui/AppField.vue'
import AppIcon from '../components/ui/AppIcon.vue'
import AppInput from '../components/ui/AppInput.vue'

type FieldErrors = {
  name: string
  email: string
  password: string
  password_confirmation: string
}

const router = useRouter()
const auth = useAuthStore()
const { loading, error, run } = useAsync()
const form = ref({
  name: '',
  email: '',
  phone: '',
  password: '',
  password_confirmation: '',
})
const showPw = ref(false)
const showPw2 = ref(false)
const errors = ref<FieldErrors>({
  name: '',
  email: '',
  password: '',
  password_confirmation: '',
})

function validasi() {
  const email = form.value.email.trim()
  errors.value = {
    name: form.value.name.trim() ? '' : 'Nama lengkap wajib diisi.',
    email: !email
      ? 'Email wajib diisi.'
      : /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)
        ? ''
        : 'Format email belum tepat.',
    password:
      form.value.password.length >= 8 ? '' : 'Kata sandi minimal 8 karakter.',
    password_confirmation: !form.value.password_confirmation
      ? 'Ulangi kata sandi.'
      : form.value.password === form.value.password_confirmation
        ? ''
        : 'Ulangi sandi belum sama.',
  }
  return !Object.values(errors.value).some(Boolean)
}

async function daftar() {
  if (!validasi()) {
    await nextTick()
    const pertama = errors.value.name
      ? 'reg-name'
      : errors.value.email
        ? 'reg-email'
        : errors.value.password
          ? 'reg-pw'
          : 'reg-pw2'
    document.getElementById(pertama)?.focus()
    return
  }
  const ok = await run(() => auth.register({ ...form.value }))
  if (ok !== null) router.push('/companies')
}
</script>

<template>
  <AuthLayout tab="register">
    <h2 class="font-display text-2xl font-bold tracking-[-0.02em] text-ink">
      Buat akun pengelola
    </h2>
    <p class="mt-1.5 text-sm leading-relaxed text-secondary">
      Mulai gratis, lalu tambahkan perusahaan pertama Anda.
    </p>

    <form novalidate class="mt-5" @submit.prevent="daftar">
      <AppBanner tone="red" :message="error" />

      <div class="space-y-4">
        <AppField label="Nama lengkap" for="reg-name" required :error="errors.name">
          <AppInput
            id="reg-name"
            v-model="form.name"
            autocomplete="name"
            placeholder="Nama Anda"
            :invalid="Boolean(errors.name)"
            @update:model-value="errors.name = ''"
          >
            <template #prefix>
              <AppIcon name="badge" :size="19" class="text-secondary" />
            </template>
          </AppInput>
        </AppField>

        <AppField
          label="Email"
          for="reg-email"
          required
          :error="errors.email"
          hint="Gunakan email aktif untuk pemulihan akun."
        >
          <AppInput
            id="reg-email"
            v-model="form.email"
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

        <AppField label="Nomor HP" for="reg-phone" optional>
          <AppInput
            id="reg-phone"
            v-model="form.phone"
            type="tel"
            autocomplete="tel"
            placeholder="0812…"
          >
            <template #prefix>
              <AppIcon name="phone" :size="19" class="text-secondary" />
            </template>
          </AppInput>
        </AppField>

        <div class="grid gap-4 sm:grid-cols-2">
          <AppField
            label="Kata sandi"
            for="reg-pw"
            required
            :error="errors.password"
            hint="Minimal 8 karakter."
          >
            <AppInput
              id="reg-pw"
              v-model="form.password"
              :type="showPw ? 'text' : 'password'"
              autocomplete="new-password"
              placeholder="Buat kata sandi"
              :invalid="Boolean(errors.password)"
              @update:model-value="errors.password = ''"
            >
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
            label="Ulangi sandi"
            for="reg-pw2"
            required
            :error="errors.password_confirmation"
          >
            <AppInput
              id="reg-pw2"
              v-model="form.password_confirmation"
              :type="showPw2 ? 'text' : 'password'"
              autocomplete="new-password"
              placeholder="Ulangi kata sandi"
              :invalid="Boolean(errors.password_confirmation)"
              @update:model-value="errors.password_confirmation = ''"
            >
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
      </div>

      <AppButton type="submit" size="lg" block :loading="loading" class="mt-5">
        <AppIcon name="domain_add" :size="18" />
        Buat akun pengelola
      </AppButton>
    </form>
  </AuthLayout>
</template>
