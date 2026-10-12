<script setup lang="ts">
import { computed, ref } from 'vue'
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
const form = ref({ name: '', email: '', phone: '', password: '', password_confirmation: '' })
const showPw = ref(false)
const showPw2 = ref(false)

const emailOk = computed(() => form.value.email === '' || /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.value.email))
const sama = computed(() => form.value.password_confirmation === '' || form.value.password === form.value.password_confirmation)

async function daftar() {
  const ok = await run(() => auth.register({ ...form.value }))
  if (ok !== null) router.push('/companies')
}
</script>

<template>
  <AuthLayout tab="register">
    <form novalidate @submit.prevent="daftar">
      <h2 class="font-display text-xl font-semibold">Daftarkan usaha baru</h2>
      <p class="mb-4 mt-1 text-sm text-secondary">Buat akun pengelola, gratis untuk memulai.</p>
      <AppBanner tone="red" :message="error" />
      <label class="block text-sm font-medium" for="reg-name">Nama lengkap <span class="text-danger">*</span></label>
      <div class="relative mt-1 flex items-center">
        <AppIcon name="badge" :size="20" class="pointer-events-none absolute left-3 text-secondary" />
        <input id="reg-name" v-model="form.name" class="w-full rounded-lg bg-card py-2.5 pl-10 pr-3 text-sm shadow-sm outline-none transition focus:bg-surface-low" placeholder="Nama Anda" autocomplete="name" />
      </div>
      <label class="mt-3 block text-sm font-medium" for="reg-email">Email <span class="text-danger">*</span></label>
      <div class="relative mt-1 flex items-center">
        <AppIcon name="mail" :size="20" class="pointer-events-none absolute left-3 text-secondary" />
        <input id="reg-email" v-model="form.email" type="email" class="w-full rounded-lg bg-card py-2.5 pl-10 pr-3 text-sm shadow-sm outline-none transition focus:bg-surface-low" placeholder="nama@email.com" autocomplete="email" />
      </div>
      <p v-if="!emailOk" class="mt-1 text-xs text-danger">Format email belum tepat.</p>
      <div class="mt-3 flex items-center justify-between">
        <label class="text-sm font-medium" for="reg-phone">Nomor HP</label>
        <span class="rounded bg-surface-low px-2 py-0.5 text-xs text-secondary">Opsional</span>
      </div>
      <div class="relative mt-1 flex items-center">
        <AppIcon name="phone" :size="20" class="pointer-events-none absolute left-3 text-secondary" />
          <input id="reg-phone" v-model="form.phone" class="w-full rounded-lg bg-card py-2.5 pl-10 pr-3 text-sm shadow-sm outline-none transition focus:bg-surface-low" placeholder="0812…" autocomplete="tel" />
      </div>
      <div class="mt-3 grid grid-cols-1 gap-3 md:grid-cols-2">
        <div>
          <label class="block text-sm font-medium" for="reg-pw">Kata sandi <span class="text-danger">*</span></label>
          <div class="relative mt-1 flex items-center">
            <input id="reg-pw" v-model="form.password" :type="showPw ? 'text' : 'password'" class="w-full rounded-lg bg-card px-3 py-2.5 text-sm shadow-sm outline-none transition focus:bg-surface-low" placeholder="Minimal 8 karakter" autocomplete="new-password" />
            <button type="button" :aria-label="showPw ? 'Sembunyikan' : 'Tampilkan'" class="absolute right-2 rounded p-1 text-secondary hover:text-ink" @click="showPw = !showPw">
              <AppIcon :name="showPw ? 'visibility_off' : 'visibility'" :size="18" />
            </button>
          </div>
        </div>
        <div>
          <label class="block text-sm font-medium" for="reg-pw2">Ulangi sandi <span class="text-danger">*</span></label>
          <div class="relative mt-1 flex items-center">
            <input id="reg-pw2" v-model="form.password_confirmation" :type="showPw2 ? 'text' : 'password'" class="w-full rounded-lg bg-card px-3 py-2.5 text-sm shadow-sm outline-none transition focus:bg-surface-low" placeholder="Ulangi sandi" autocomplete="new-password" />
            <button type="button" :aria-label="showPw2 ? 'Sembunyikan' : 'Tampilkan'" class="absolute right-2 rounded p-1 text-secondary hover:text-ink" @click="showPw2 = !showPw2">
              <AppIcon :name="showPw2 ? 'visibility_off' : 'visibility'" :size="18" />
            </button>
          </div>
        </div>
      </div>
      <p v-if="!sama" class="mt-1 flex items-center gap-1 text-xs text-danger"><AppIcon name="cancel" :size="16" />Ulangi sandi belum sama.</p>
      <AppButton type="submit" :loading="loading" class="mt-4 w-full">
        <AppIcon name="domain_add" :size="18" />Buat akun usaha
      </AppButton>
    </form>
  </AuthLayout>
</template>
