<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { api } from '../lib/api'
import { tanggal } from '../lib/format'
import { useAuthStore } from '../stores/auth'
import { useAsync } from '../composables/useAsync'
import { useConfirm } from '../composables/useConfirm'
import AppBanner from '../components/ui/AppBanner.vue'
import AppButton from '../components/ui/AppButton.vue'
import AppCard from '../components/ui/AppCard.vue'
import AppField from '../components/ui/AppField.vue'
import AppInput from '../components/ui/AppInput.vue'
import AppPageHeader from '../components/ui/AppPageHeader.vue'
import AppTable from '../components/ui/AppTable.vue'

interface Libur {
  id: number
  date: string
  name: string
}

const auth = useAuthStore()
const { confirm } = useConfirm()
const { loading, error, run } = useAsync()
const list = ref<Libur[]>([])
const okMsg = ref('')
const fTanggal = ref('')
const fNama = ref('')

async function muat() {
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

async function hapus(libur: Libur) {
  const setuju = await confirm({
    title: `Hapus libur ${libur.name}?`,
    message: `${tanggal(libur.date)} tidak lagi dihitung sebagai libur nasional untuk semua perusahaan.`,
    confirmLabel: 'Hapus libur',
    tone: 'danger',
  })
  if (!setuju) return
  await run(() => api.del(`/holidays/${libur.id}`, auth.token))
  if (!error.value) {
    okMsg.value = 'Libur nasional dihapus.'
    muat()
  }
}

onMounted(muat)
</script>

<template>
  <div class="mx-auto max-w-3xl">
    <AppPageHeader
      eyebrow="Platform · khusus admin layanan"
      title="Libur nasional"
      subtitle="Tanggal libur ini berlaku untuk semua perusahaan di platform."
      icon="event"
    />

    <AppBanner tone="green" :message="okMsg" />
    <AppBanner tone="red" :message="error" />
    <AppBanner
      tone="blue"
      message="Libur khusus satu perusahaan diatur terpisah di halaman Jadwal & Shift perusahaan tersebut."
    />

    <AppCard
      title="Tambah libur nasional"
      subtitle="Gunakan nama resmi agar mudah dikenali seluruh pengguna."
    >
      <form id="holiday-form" class="grid gap-3 sm:grid-cols-[1fr_2fr_auto]" @submit.prevent="tambah">
        <AppField label="Tanggal" required>
          <AppInput v-model="fTanggal" type="date" />
        </AppField>
        <AppField label="Nama libur" required>
          <AppInput v-model="fNama" maxlength="255" placeholder="cth. Hari kemerdekaan" />
        </AppField>
        <div class="flex items-end">
          <AppButton
            type="submit"
            form="holiday-form"
            :loading="loading"
            :disabled="!fTanggal || !fNama.trim()"
            block
          >
            Tambah
          </AppButton>
        </div>
      </form>
    </AppCard>

    <AppCard
      title="Daftar libur nasional"
      :subtitle="`${list.length} tanggal terdaftar.`"
      class="mt-4"
    >
      <AppTable
        :loading="loading"
        :empty="!list.length"
        :colspan="3"
        empty-title="Belum ada libur nasional"
        empty-message="Tambahkan tanggal libur melalui formulir di atas."
      >
        <template #head>
          <tr>
            <th>Tanggal</th>
            <th>Nama libur</th>
            <th class="text-right">Aksi</th>
          </tr>
        </template>
        <tr v-for="l in list" :key="l.id">
          <td class="whitespace-nowrap font-medium text-ink">{{ tanggal(l.date) }}</td>
          <td>{{ l.name }}</td>
          <td class="text-right">
            <AppButton
              size="sm"
              variant="ghost"
              class="text-danger"
              :aria-label="`Hapus libur ${l.name}`"
              @click="hapus(l)"
            >
              Hapus
            </AppButton>
          </td>
        </tr>
      </AppTable>
    </AppCard>
  </div>
</template>
