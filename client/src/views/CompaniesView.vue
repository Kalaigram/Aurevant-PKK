<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '../lib/api'
import { useAuthStore } from '../stores/auth'
import { useAsync } from '../composables/useAsync'
import AppCard from '../components/ui/AppCard.vue'
import AppBadge from '../components/ui/AppBadge.vue'
import AppBanner from '../components/ui/AppBanner.vue'
import AppEmpty from '../components/ui/AppEmpty.vue'

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
  <div class="mx-auto max-w-3xl p-5">
    <h1 class="mb-1 text-xl font-bold">Pilih perusahaan</h1>
    <p class="mb-4 text-sm text-slate-500">Anda tergabung di {{ list.length }} perusahaan.</p>
    <AppBanner tone="red" :message="error" />
    <p v-if="loading" class="text-sm text-slate-500">Memuat…</p>
    <AppEmpty v-else-if="!list.length" message="Belum ada perusahaan. Minta pemilik menambahkan Anda." />
    <div class="grid gap-3 md:grid-cols-2">
      <button v-for="c in list" :key="c.id" @click="pilih(c)" class="cursor-pointer rounded-xl border border-slate-200 bg-white p-4 text-left transition hover:border-primary-700">
        <div class="font-semibold">{{ c.name }}</div>
        <div class="mt-1 flex items-center justify-between">
          <span class="text-sm text-slate-500">{{ c.slug }}</span>
          <AppBadge :tone="c.status === 'aktif' ? 'green' : 'red'">{{ c.status }}</AppBadge>
        </div>
      </button>
    </div>
  </div>
</template>
