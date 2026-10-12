<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { api, type PageMeta } from '../lib/api'
import { tanggal } from '../lib/format'
import { useCompany } from '../composables/useCompany'
import { useAsync } from '../composables/useAsync'
import AppCard from '../components/ui/AppCard.vue'
import AppBadge from '../components/ui/AppBadge.vue'
import AppBanner from '../components/ui/AppBanner.vue'
import AppButton from '../components/ui/AppButton.vue'
import AppField from '../components/ui/AppField.vue'
import AppEmpty from '../components/ui/AppEmpty.vue'
import AppPager from '../components/ui/AppPager.vue'

interface Cuti { id: number; userId: number; type: string; fromDate: string; toDate: string; reason: string; status: string }

const TYPES = ['izin', 'sakit', 'cuti']
const { auth, companyId } = useCompany()
const { loading, error, run } = useAsync()
const list = ref<Cuti[]>([])
const meta = ref<PageMeta | null>(null)
const status = ref('')
const okMsg = ref('')
const form = ref({ type: 'izin', from_date: '', to_date: '', reason: '' })

const tone = (s: string) => (s === 'disetujui' ? 'green' : s === 'ditolak' ? 'red' : 'amber')

async function muat(page = 1) {
  okMsg.value = ''
  const me = auth.user?.id ?? 0
  const res = await run(() =>
    api.get<{ data: Cuti[]; meta: PageMeta }>(
      `/companies/${companyId.value}/leaves?user_id=${me}${status.value ? `&status=${status.value}` : ''}&per_page=15&page=${page}`,
      auth.token,
    ),
  )
  if (res) {
    const raw = res as unknown as { data: Cuti[]; meta?: PageMeta } | Cuti[]
    if (Array.isArray(raw)) list.value = raw
    else { list.value = raw.data; meta.value = raw.meta ?? null }
  }
}

async function ajukan() {
  const res = await run(() =>
    api.post(`/companies/${companyId.value}/users/${auth.user?.id}/leaves`, { ...form.value }, auth.token),
  )
  if (res) {
    okMsg.value = 'Pengajuan terkirim.'
    form.value = { type: 'izin', from_date: '', to_date: '', reason: '' }
    muat(1)
  }
}

onMounted(() => muat())
</script>

<template>
  <div>
    <h1 class="mb-4 text-xl font-bold">Cuti saya</h1>
    <AppBanner tone="green" :message="okMsg" />
    <AppBanner tone="red" :message="error" />
    <div class="grid gap-4 lg:grid-cols-2">
      <AppCard title="Ajukan izin / sakit / cuti">
        <AppField label="Jenis">
          <select v-model="form.type" class="w-full rounded-lg border border-slate-300 px-3 py-2.5">
            <option v-for="t in TYPES" :key="t" :value="t">{{ t }}</option>
          </select>
        </AppField>
        <div class="mt-3 grid grid-cols-2 gap-2">
          <AppField label="Dari tanggal"><input v-model="form.from_date" type="date" class="w-full rounded-lg border border-slate-300 px-3 py-2.5" /></AppField>
          <AppField label="Sampai tanggal"><input v-model="form.to_date" type="date" class="w-full rounded-lg border border-slate-300 px-3 py-2.5" /></AppField>
        </div>
        <div class="mt-3">
          <AppField label="Alasan">
            <textarea v-model="form.reason" rows="3" maxlength="500" class="w-full rounded-lg border border-slate-300 px-3 py-2.5" placeholder="Contoh: kontrol ke dokter gigi" />
          </AppField>
        </div>
        <AppButton :loading="loading" class="mt-3" @click="ajukan">Kirim pengajuan</AppButton>
      </AppCard>
      <div>
        <div class="mb-3 flex gap-2">
          <AppButton :variant="!status ? 'primary' : 'ghost'" @click="status = ''; muat()">Semua</AppButton>
          <AppButton
            v-for="s in ['diajukan', 'disetujui', 'ditolak']" :key="s"
            :variant="status === s ? 'primary' : 'ghost'" @click="status = s; muat()"
          >{{ s }}</AppButton>
        </div>
        <AppEmpty v-if="!loading && !list.length" message="Belum ada pengajuan." />
        <AppCard v-for="c in list" :key="c.id">
          <div class="flex items-center justify-between gap-2">
            <p class="font-semibold capitalize">{{ c.type }} <span class="font-normal text-slate-500">{{ tanggal(c.fromDate) }} – {{ tanggal(c.toDate) }}</span></p>
            <AppBadge :tone="tone(c.status)">{{ c.status }}</AppBadge>
          </div>
          <p class="mt-1 text-sm text-slate-500">{{ c.reason }}</p>
        </AppCard>
        <AppPager :meta="meta" @page="muat" />
      </div>
    </div>
  </div>
</template>
