<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { api } from '../lib/api'
import { tanggal } from '../lib/format'
import { useAuthStore } from '../stores/auth'
import { useAsync } from '../composables/useAsync'
import AppCard from '../components/ui/AppCard.vue'
import AppBanner from '../components/ui/AppBanner.vue'
import AppButton from '../components/ui/AppButton.vue'
import AppField from '../components/ui/AppField.vue'
import AppEmpty from '../components/ui/AppEmpty.vue'

interface Libur {
  id: number
  date: string
  name: string
}

const auth = useAuthStore()
const { loading, error, run } = useAsync()
const list = ref<Libur[]>([])
const okMsg = ref('')
const fTanggal = ref('')
const fNama = ref('')

async function muat() {
  okMsg.value = ''
  const res = await run(() => api.get<Libur[]>('/holidays', auth.token))
  if (res)
    list.value = (
      Array.isArray(res) ? res : ((res as unknown as { data: Libur[] }).data ?? [])
    ) as Libur[]
}

async function tambah() {
  if (!fTanggal.value || !fNama.value.trim()) return
  const res = await run(() =>
    api.post('/holidays', { date: fTanggal.value, name: fNama.value.trim() }, auth.token),
  )
  if (res) {
    okMsg.value = 'Libur nasional ditambahkan — berlaku semua perusahaan.'
    fTanggal.value = ''
    fNama.value = ''
    muat()
  }
}

async function hapus(id: number) {
  await run(() => api.del(`/holidays/${id}`, auth.token))
  if (!error.value) {
    okMsg.value = 'Libur nasional dihapus.'
    muat()
  }
}

onMounted(muat)
</script>

<template>
  <div class="mx-auto max-w-2xl">
    <p class="text-xs font-semibold uppercase tracking-wider text-secondary">
      Platform · khusus admin layanan
    </p>
    <h1 class="font-display mb-4 text-xl font-semibold">Libur nasional</h1>
    <AppBanner tone="green" :message="okMsg" />
    <AppBanner tone="red" :message="error" />
    <p class="mb-3 text-xs text-secondary">
      Berlaku untuk semua perusahaan. Libur khusus satu perusahaan diatur di Jadwal & Shift
      masing-masing perusahaan.
    </p>

    <AppCard>
      <h2 class="font-display font-semibold">Tambah libur nasional</h2>
      <div class="mt-2 grid grid-cols-[1fr_2fr_auto] gap-2">
        <input
          v-model="fTanggal"
          type="date"
          class="rounded-lg bg-surface-low px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-primary"
        />
        <input
          v-model="fNama"
          maxlength="255"
          placeholder="cth. Hari kemerdekaan"
          class="rounded-lg bg-surface-low px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-primary"
        />
        <AppButton @click="tambah">Tambah</AppButton>
      </div>
    </AppCard>

    <AppCard class="mt-3">
      <p v-if="loading" class="text-sm text-secondary">Memuat…</p>
      <AppEmpty v-else-if="!list.length" message="Belum ada libur nasional." />
      <ul v-else class="divide-y divide-outline/60">
        <li v-for="l in list" :key="l.id" class="flex items-center justify-between py-2 text-sm">
          <span
            ><strong>{{ tanggal(l.date) }}</strong> — {{ l.name }}</span
          >
          <AppButton variant="ghost" @click="hapus(l.id)">Hapus</AppButton>
        </li>
      </ul>
    </AppCard>

    <AppField label="Catatan" class="mt-3">
      <p class="text-xs text-secondary">
        Jangan confuse dengan Libur Perusahaan (tab di Jadwal & Shift, wewenang pengatur
        perusahaan).
      </p>
    </AppField>
  </div>
</template>
