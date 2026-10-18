<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute } from 'vue-router'
import { api } from '../lib/api'
import { useAsync } from '../composables/useAsync'
import AuthLayout from '../components/AuthLayout.vue'
import AppButton from '../components/ui/AppButton.vue'
import AppBanner from '../components/ui/AppBanner.vue'
import AppIcon from '../components/ui/AppIcon.vue'

const route = useRoute()
const { loading, error, run } = useAsync()
const email = ref((route.query.email as string | undefined) ?? '')
const token = (route.query.token as string | undefined) ?? ''
const sandi = ref('')
const konfirmasi = ref('')
const showPw = ref(false)
const berhasil = ref(false)

const tokenAda = computed(() => token.length > 0)

async function simpan() {
  if (sandi.value !== konfirmasi.value) {
    error.value = 'Konfirmasi kata sandi tidak cocok.'
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
    <h2 class="font-display text-xl font-semibold">Atur ulang kata sandi</h2>
    <p class="mb-4 mt-1 text-sm text-secondary">
      Tautan dari email kami berlaku terbatas. Buat kata sandi baru minimal 8 karakter.
    </p>
    <AppBanner
      v-if="!tokenAda"
      tone="red"
      message="Tautan tidak lengkap. Buka kembali tautan dari email, atau minta tautan baru."
    />
    <AppBanner tone="red" :message="error" />
    <AppBanner
      tone="green"
      :message="berhasil ? 'Kata sandi berhasil diatur ulang.' : ''"
    />

    <template v-if="berhasil">
      <RouterLink to="/login">
        <AppButton class="w-full"
          ><AppIcon name="login" :size="18" />Masuk dengan kata sandi baru</AppButton
        >
      </RouterLink>
    </template>
    <form v-else-if="tokenAda" novalidate @submit.prevent="simpan">
      <label class="block text-sm font-medium" for="reset-email">
        Email terdaftar <span class="text-danger">*</span>
      </label>
      <div class="relative mt-1 flex items-center">
        <AppIcon name="mail" :size="20" class="pointer-events-none absolute left-3 text-secondary" />
        <input
          id="reset-email"
          v-model="email"
          type="email"
          class="w-full rounded-lg bg-card py-2.5 pl-10 pr-3 text-sm shadow-sm outline-none transition focus:bg-surface-low"
          placeholder="nama@email.com"
          autocomplete="email"
        />
      </div>
      <label class="mt-3 block text-sm font-medium" for="reset-pw">
        Kata sandi baru <span class="text-danger">*</span>
      </label>
      <div class="relative mt-1 flex items-center">
        <AppIcon name="lock" :size="20" class="pointer-events-none absolute left-3 text-secondary" />
        <input
          id="reset-pw"
          v-model="sandi"
          :type="showPw ? 'text' : 'password'"
          class="w-full rounded-lg bg-card py-2.5 pl-10 pr-10 text-sm shadow-sm outline-none transition focus:bg-surface-low"
          placeholder="Minimal 8 karakter"
          autocomplete="new-password"
        />
        <button
          type="button"
          :aria-label="showPw ? 'Sembunyikan sandi' : 'Tampilkan sandi'"
          class="absolute right-3 rounded p-1 text-secondary hover:text-ink"
          @click="showPw = !showPw"
        >
          <AppIcon :name="showPw ? 'visibility_off' : 'visibility'" :size="20" />
        </button>
      </div>
      <label class="mt-3 block text-sm font-medium" for="reset-pw2">
        Ulangi kata sandi baru <span class="text-danger">*</span>
      </label>
      <div class="relative mt-1 flex items-center">
        <AppIcon name="lock_reset" :size="20" class="pointer-events-none absolute left-3 text-secondary" />
        <input
          id="reset-pw2"
          v-model="konfirmasi"
          :type="showPw ? 'text' : 'password'"
          class="w-full rounded-lg bg-card py-2.5 pl-10 pr-3 text-sm shadow-sm outline-none transition focus:bg-surface-low"
          placeholder="Ketik ulang kata sandi baru"
          autocomplete="new-password"
        />
      </div>
      <AppButton type="submit" :loading="loading" class="mt-4 w-full">
        <AppIcon name="lock_reset" :size="18" />Simpan kata sandi baru
      </AppButton>
    </form>
    <RouterLink
      to="/lupa-password"
      class="mt-4 inline-flex items-center gap-1 text-sm font-semibold text-primary hover:underline"
    >
      <AppIcon name="arrow_back" :size="18" />Minta tautan baru
    </RouterLink>
  </AuthLayout>
</template>
