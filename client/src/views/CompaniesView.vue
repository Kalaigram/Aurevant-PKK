<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '../lib/api'
import { useAuthStore } from '../stores/auth'
import { useAsync } from '../composables/useAsync'
import AppBadge from '../components/ui/AppBadge.vue'
import AppBanner from '../components/ui/AppBanner.vue'
import AppEmpty from '../components/ui/AppEmpty.vue'
import AppIcon from '../components/ui/AppIcon.vue'

interface Company {
  id: number
  name: string
  slug: string
  status: string
}

const router = useRouter()
const auth = useAuthStore()
const { loading, error, run } = useAsync()
const list = ref<Company[]>([])

onMounted(async () => {
  list.value = (await run(() => api.get<Company[]>('/companies', auth.token))) ?? []
})

function pilih(c: Company) {
  auth.pickCompany(c.id, c.name)
  router.push(`/c/${c.id}/dashboard`)
}
</script>

<template>
  <div class="mx-auto max-w-3xl p-4 md:p-6">
    <div class="mb-4 flex items-center gap-3">
      <div class="flex h-12 w-12 items-center justify-center rounded-xl bg-primary-container text-on-primary">
        <AppIcon name="domain" :size="28" />
      </div>
      <div>
        <h1 class="font-display text-xl font-semibold">Pilih perusahaan</h1>
        <p class="text-sm text-secondary">Anda tergabung di {{ list.length }} perusahaan.</p>
      </div>
    </div>
    <AppBanner tone="red" :message="error" />
    <p v-if="loading" class="text-sm text-secondary">Memuat…</p>
    <AppEmpty v-else-if="!list.length" message="Belum ada perusahaan. Minta pemilik menambahkan Anda." />
    <div class="grid gap-3 md:grid-cols-2">
      <button
        v-for="c in list" :key="c.id" @click="pilih(c)"
        class="cursor-pointer rounded-xl bg-card p-4 text-left shadow-sm transition hover:shadow"
      >
        <div class="flex items-center justify-between gap-2">
          <p class="font-display font-semibold">{{ c.name }}</p>
          <AppBadge :tone="c.status === 'aktif' ? 'green' : 'red'">{{ c.status }}</AppBadge>
        </div>
        <p class="mt-1 text-sm text-secondary">{{ c.slug }}</p>
      </button>
    </div>
  </div>
</template>
