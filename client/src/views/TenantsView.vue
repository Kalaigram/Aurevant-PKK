<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { api, type PageMeta } from '../lib/api'
import { tanggal } from '../lib/format'
import { useAuthStore } from '../stores/auth'
import { useAsync } from '../composables/useAsync'
import AppCard from '../components/ui/AppCard.vue'
import AppBadge from '../components/ui/AppBadge.vue'
import AppBanner from '../components/ui/AppBanner.vue'
import AppButton from '../components/ui/AppButton.vue'
import AppEmpty from '../components/ui/AppEmpty.vue'
import AppPager from '../components/ui/AppPager.vue'
import AppIcon from '../components/ui/AppIcon.vue'

interface Tenant {
  id: number
  name: string
  slug: string
  status: string
  branchesCount: number
  usersCount: number
  planName: string | null
  subscriptionStatus: string | null
  endsAt: string | null
  createdAt: string
}

const auth = useAuthStore()
const { loading, error, run } = useAsync()
const list = ref<Tenant[]>([])
const meta = ref<PageMeta | null>(null)
const cari = ref('')
const status = ref('')
const detail = ref<Tenant | null>(null)
const okMsg = ref('')
const tanya = ref<{ t: Tenant; ke: string } | null>(null)

function tone(s: string) {
  if (s === 'aktif') return 'green'
  if (s === 'trial') return 'blue'
  if (s === 'tenggang') return 'amber'
  return 'red'
}
const totalUnit = computed(() => meta.value?.total ?? list.value.length)

async function muat(page = 1) {
  okMsg.value = ''
  const q = `?per_page=15&page=${page}` +
    (cari.value.trim() ? `&search=${encodeURIComponent(cari.value.trim())}` : '') +
    (status.value ? `&status=${status.value}` : '')
  const res = await run(() => api.getPage<Tenant>(`/admin/companies${q}`, auth.token))
  if (res) {
    const raw = res as unknown as { data: Tenant[]; meta?: PageMeta } | Tenant[]
    if (Array.isArray(raw)) list.value = raw
    else { list.value = raw.data; meta.value = raw.meta ?? null }
  }
}

async function lihat(id: number) {
  detail.value = await run(() => api.get<Tenant>(`/admin/companies/${id}`, auth.token))
}

async function ubahStatus() {
  if (!tanya.value) return
  const res = await run(() => api.patch(`/admin/companies/${tanya.value!.t.id}`, { status: tanya.value!.ke }, auth.token))
  if (res) {
    okMsg.value = `Perusahaan ${tanya.value.ke === 'aktif' ? 'diaktifkan' : tanya.value.ke === 'ditangguhkan' ? 'ditangguhkan' : 'diarsipkan'}.`
    const id = tanya.value.t.id
    tanya.value = null
    if (detail.value?.id === id) lihat(id)
    muat(meta.value?.current_page ?? 1)
  }
}

onMounted(() => muat())
</script>

<template>
  <div class="mx-auto max-w-5xl p-4 md:p-6">
    <div class="mb-4 flex items-center gap-3">
      <div class="flex h-12 w-12 items-center justify-center rounded-xl bg-primary-container text-on-primary">
        <AppIcon name="domain" :size="28" />
      </div>
      <div>
        <p class="text-xs font-semibold uppercase tracking-wider text-secondary">Administrasi platform</p>
        <h1 class="font-display text-xl font-semibold">Perusahaan terdaftar ({{ totalUnit }})</h1>
      </div>
    </div>
    <AppBanner tone="green" :message="okMsg" />
    <AppBanner tone="red" :message="error" />

    <div class="mb-3 flex flex-wrap gap-2">
      <div class="relative min-w-52 flex-1">
        <AppIcon name="search" :size="18" class="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-secondary" />
        <input v-model="cari" class="w-full rounded-lg border border-outline bg-card py-2 pl-9 pr-3 text-sm focus:border-primary focus:outline-none" placeholder="Cari nama atau slug…" @keyup.enter="muat(1)" />
      </div>
      <select v-model="status" class="rounded-lg border border-outline bg-card px-3 py-2 text-sm focus:border-primary focus:outline-none" @change="muat(1)">
        <option value="">Semua status</option>
        <option value="aktif">Aktif</option>
        <option value="ditangguhkan">Ditangguhkan</option>
        <option value="diarsipkan">Diarsipkan</option>
      </select>
      <AppButton @click="muat(1)">Cari</AppButton>
    </div>

    <AppEmpty v-if="!loading && !list.length" message="Tidak ada perusahaan cocok." />
    <div class="overflow-x-auto rounded-xl bg-card shadow-sm">
      <table class="w-full min-w-[720px] text-sm">
        <thead>
          <tr class="text-left text-xs uppercase text-secondary">
            <th class="px-4 py-2">Perusahaan</th><th class="px-4 py-2 text-center">Cabang</th>
            <th class="px-4 py-2 text-center">User</th><th class="px-4 py-2">Paket</th>
            <th class="px-4 py-2">Berlaku s/d</th><th class="px-4 py-2">Status</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="t in list" :key="t.id" class="cursor-pointer border-t border-surface-low hover:bg-surface-low/60" @click="lihat(t.id)">
            <td class="px-4 py-2">
              <p class="font-semibold">{{ t.name }}</p>
              <p class="font-mono text-xs text-secondary">{{ t.slug }}</p>
            </td>
            <td class="px-4 py-2 text-center">{{ t.branchesCount }}</td>
            <td class="px-4 py-2 text-center">{{ t.usersCount }}</td>
            <td class="px-4 py-2">{{ t.planName ?? '–' }}<span v-if="t.subscriptionStatus" class="text-secondary"> ({{ t.subscriptionStatus }})</span></td>
            <td class="px-4 py-2">{{ tanggal(t.endsAt) }}</td>
            <td class="px-4 py-2"><AppBadge :tone="tone(t.status)">{{ t.status }}</AppBadge></td>
          </tr>
        </tbody>
      </table>
    </div>
    <AppPager :meta="meta" @page="muat" />

    <AppCard v-if="detail" title="Detail perusahaan" class="mt-4">
      <div class="flex flex-wrap items-start justify-between gap-2">
        <div>
          <p class="font-display font-semibold">{{ detail.name }}</p>
          <p class="text-sm text-secondary">Terdaftar {{ tanggal(detail.createdAt) }} · {{ detail.branchesCount }} cabang · {{ detail.usersCount }} user</p>
        </div>
        <AppBadge :tone="tone(detail.status)">{{ detail.status }}</AppBadge>
      </div>
      <div class="mt-3 flex flex-wrap gap-2">
        <AppButton
          v-for="s in ['aktif', 'ditangguhkan', 'diarsipkan'].filter((x) => x !== detail?.status)" :key="s"
          :variant="s === 'aktif' ? 'primary' : s === 'ditangguhkan' ? 'danger' : 'ghost'"
          @click="tanya = { t: detail, ke: s }"
        >{{ s === 'aktif' ? 'Aktifkan' : s === 'ditangguhkan' ? 'Tangguhkan' : 'Arsipkan' }}</AppButton>
      </div>
      <p class="mt-2 text-xs text-secondary">Menangguhkan memblokir penulisan di perusahaan itu; baca tetap boleh.</p>
    </AppCard>

    <div v-if="tanya" class="fixed inset-0 z-20 flex items-center justify-center bg-black/40 p-4" @click.self="tanya = null">
      <div class="w-full max-w-sm rounded-xl bg-card p-5 shadow-xl">
        <h3 class="font-display font-semibold">Ubah status {{ tanya.t.name }}?</h3>
        <p class="mt-1 text-sm text-secondary">Status baru: <strong class="text-ink">{{ tanya.ke }}</strong>.</p>
        <div class="mt-4 flex justify-end gap-2">
          <AppButton variant="ghost" @click="tanya = null">Batal</AppButton>
          <AppButton :loading="loading" @click="ubahStatus">Ya, ubah</AppButton>
        </div>
      </div>
    </div>
  </div>
</template>
