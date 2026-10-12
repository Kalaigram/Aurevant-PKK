<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { api } from '../lib/api'
import { useCompany } from '../composables/useCompany'
import { useAsync } from '../composables/useAsync'
import AppCard from '../components/ui/AppCard.vue'
import AppButton from '../components/ui/AppButton.vue'
import AppIcon from '../components/ui/AppIcon.vue'

interface Card {
  name: string
  email: string
  phone: string | null
  employeeUuid: string
  company: string
  branch: string | null
  role: string | null
}

const { auth, companyId } = useCompany()
const { loading, error, run } = useAsync()
const card = ref<Card | null>(null)
const tanyaKeluar = ref(false)

onMounted(async () => {
  if (!auth.user) return
  card.value = await run(() =>
    api.get<Card>(`/companies/${companyId.value}/users/${auth.user!.id}/card`, auth.token),
  )
})

async function keluar() {
  await auth.logout()
  window.location.href = '/login'
}
</script>

<template>
  <div class="mx-auto max-w-2xl">
    <p class="text-xs font-semibold uppercase tracking-wider text-secondary">Identitas & sesi resmi</p>
    <h1 class="font-display mb-4 text-xl font-semibold">Profil pengguna & keamanan akun</h1>
    <AppBanner tone="red" :message="error" />
    <p v-if="loading" class="text-sm text-secondary">Memuat…</p>

    <AppCard>
      <div class="flex items-start justify-between gap-2">
        <div>
          <p class="font-mono text-xs text-secondary">UID #{{ auth.user?.id }}</p>
          <h2 class="font-display mt-1 text-lg font-semibold">{{ auth.user?.name }}</h2>
          <p class="text-sm text-secondary">{{ card?.company ?? auth.companyName }}<span v-if="card?.branch"> · {{ card.branch }}</span></p>
        </div>
        <div v-if="card?.role" class="rounded bg-primary-container/15 px-2 py-0.5 text-xs font-semibold text-primary">{{ card.role }}</div>
      </div>
      <dl class="mt-4 space-y-3 text-sm">
        <div>
          <dt class="text-xs uppercase tracking-wider text-secondary">ID akun</dt>
          <dd class="font-semibold">{{ auth.user?.id }}</dd>
        </div>
        <div>
          <dt class="text-xs uppercase tracking-wider text-secondary">Nama lengkap</dt>
          <dd class="font-semibold">{{ auth.user?.name }}</dd>
        </div>
        <div>
          <dt class="text-xs uppercase tracking-wider text-secondary">Email resmi</dt>
          <dd class="font-semibold">{{ auth.user?.email }}</dd>
        </div>
        <div>
          <dt class="text-xs uppercase tracking-wider text-secondary">Nomor HP</dt>
          <dd class="font-semibold">{{ auth.user?.phone ?? '–' }}
            <span v-if="auth.user?.phone" class="ml-1 rounded bg-primary-container/15 px-1.5 py-0.5 text-xs font-semibold text-primary">Aktif</span>
          </dd>
        </div>
      </dl>
    </AppCard>

    <AppCard title="Hak akses & organisasi" class="mt-3">
      <dl class="space-y-3 text-sm">
        <div>
          <dt class="text-xs uppercase tracking-wider text-secondary">Perusahaan</dt>
          <dd class="font-semibold">{{ card?.company ?? auth.companyName }}</dd>
        </div>
        <div>
          <dt class="text-xs uppercase tracking-wider text-secondary">Peran operasional</dt>
          <dd class="font-semibold">{{ card?.role ?? '–' }}</dd>
        </div>
      </dl>
      <p class="mt-3 text-xs text-secondary">Hak akses lengkap diatur oleh pemilik perusahaan.</p>
    </AppCard>

    <AppCard title="Sesi perangkat & keamanan" class="mt-3">
      <p class="text-sm text-secondary">Satu akun terikat satu perangkat untuk mencegah titip absen.</p>
      <AppButton variant="danger" class="mt-3" @click="tanyaKeluar = true">
        <AppIcon name="logout" :size="18" />Keluar dari sesi ini
      </AppButton>
    </AppCard>

    <AppCard title="Tentang aplikasi" class="mt-3">
      <dl class="space-y-2 text-sm">
        <div class="flex justify-between"><dt class="text-secondary">Platform</dt><dd class="font-semibold">Aurevant PKK</dd></div>
        <div class="flex justify-between"><dt class="text-secondary">Status API</dt><dd class="font-semibold text-primary">Terhubung</dd></div>
      </dl>
    </AppCard>

    <div v-if="tanyaKeluar" class="fixed inset-0 z-20 flex items-center justify-center bg-black/40 p-4" @click.self="tanyaKeluar = false">
      <div class="w-full max-w-sm rounded-xl bg-card p-5 shadow-xl">
        <h3 class="font-display font-semibold">Keluar dari aplikasi?</h3>
        <p class="mt-1 text-sm text-secondary">Sesi di perangkat ini akan diakhiri. Anda perlu masuk lagi.</p>
        <div class="mt-4 flex justify-end gap-2">
          <AppButton variant="ghost" @click="tanyaKeluar = false">Batal</AppButton>
          <AppButton variant="danger" @click="keluar">Ya, keluar</AppButton>
        </div>
      </div>
    </div>
  </div>
</template>
