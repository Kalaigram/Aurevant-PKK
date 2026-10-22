<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { api, type PageMeta } from '../lib/api'
import { tanggal } from '../lib/format'
import { useCompany } from '../composables/useCompany'
import { useAccessStore } from '../stores/access'
import { useAsync } from '../composables/useAsync'
import AppCard from '../components/ui/AppCard.vue'
import AppBadge from '../components/ui/AppBadge.vue'
import AppBanner from '../components/ui/AppBanner.vue'
import AppButton from '../components/ui/AppButton.vue'
import AppField from '../components/ui/AppField.vue'
import AppEmpty from '../components/ui/AppEmpty.vue'
import AppPager from '../components/ui/AppPager.vue'
import AppIcon from '../components/ui/AppIcon.vue'

interface Cuti { id: number; userId: number; type: string; fromDate: string; toDate: string; reason: string; status: string }

const TYPES = ['izin', 'sakit', 'cuti']
const { auth, companyId } = useCompany()
const access = useAccessStore()
const { loading, error, run } = useAsync()
const list = ref<Cuti[]>([])
const meta = ref<PageMeta | null>(null)
const status = ref('')
const okMsg = ref('')
const form = ref({ type: 'izin', from_date: '', to_date: '', reason: '' })

const tone = (s: string) => (s === 'disetujui' ? 'green' : s === 'ditolak' ? 'red' : 'amber')
const hitung = (s: string) => list.value.filter((c) => c.status === s).length
const durasi = (c: Cuti) => {
  const hari = Math.round((new Date(c.toDate).getTime() - new Date(c.fromDate).getTime()) / 864e5) + 1
  return `${hari} hari`
}
const estimasi = computed(() => {
  if (!form.value.from_date || !form.value.to_date) return ''
  const hari = Math.round((new Date(form.value.to_date).getTime() - new Date(form.value.from_date).getTime()) / 864e5) + 1
  return hari > 0 ? `Perkiraan durasi: ${hari} hari.` : 'Tanggal selesai lebih awal dari tanggal mulai.'
})

async function muat(page = 1) {
  okMsg.value = ''
  const me = auth.user?.id ?? 0
  const res = await run(() =>
    api.getPage<Cuti>(
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
    okMsg.value = 'Pengajuan terkirim, menunggu atasan.'
    form.value = { type: 'izin', from_date: '', to_date: '', reason: '' }
    muat(1)
  }
}

async function putuskan(c: Cuti, setuju: boolean) {
  const res = await run(() =>
    api.post(`/companies/${companyId.value}/leaves/${c.id}/decide`, { approve: setuju }, auth.token),
  )
  if (res) {
    okMsg.value = setuju ? 'Pengajuan disetujui.' : 'Pengajuan ditolak.'
    muat(meta.value?.current_page ?? 1)
  }
}

onMounted(async () => {
  await access.load(companyId.value, auth.token)
  muat()
})
</script>

<template>
  <div>
    <p class="text-xs font-semibold uppercase tracking-wider text-secondary">Kepegawaian • saya</p>
    <h1 class="font-display mb-4 text-xl font-semibold">Pengajuan & riwayat cuti / izin</h1>
    <AppBanner tone="green" :message="okMsg" />
    <AppBanner tone="red" :message="error" />

    <div class="mb-4 grid grid-cols-3 gap-3">
      <AppCard><p class="text-xs uppercase tracking-wider text-secondary">Menunggu</p><p class="font-display text-2xl font-bold">{{ hitung('diajukan') }}</p></AppCard>
      <AppCard><p class="text-xs uppercase tracking-wider text-secondary">Disetujui</p><p class="font-display text-2xl font-bold">{{ hitung('disetujui') }}</p></AppCard>
      <AppCard><p class="text-xs uppercase tracking-wider text-secondary">Ditolak</p><p class="font-display text-2xl font-bold">{{ hitung('ditolak') }}</p></AppCard>
    </div>

    <div class="grid gap-4 lg:grid-cols-5">
      <div class="lg:col-span-3">
        <div class="mb-3 flex gap-2">
          <AppButton :variant="!status ? 'primary' : 'ghost'" @click="status = ''; muat()">Semua</AppButton>
          <AppButton
            v-for="s in ['diajukan', 'disetujui', 'ditolak']" :key="s"
            :variant="status === s ? 'primary' : 'ghost'" @click="status = s; muat()"
          >{{ s }}</AppButton>
        </div>
        <AppEmpty v-if="!loading && !list.length" message="Belum ada pengajuan. Ganti filter atau ajukan baru." />
        <div class="overflow-x-auto rounded-xl bg-card shadow-sm">
          <table class="w-full min-w-[640px] text-sm">
            <thead>
              <tr class="text-left text-xs uppercase text-secondary">
                <th class="px-4 py-2">Tipe</th>
                <th class="px-4 py-2">Rentang</th>
                <th class="px-4 py-2 text-center">Durasi</th>
                <th class="px-4 py-2">Alasan</th>
                <th class="px-4 py-2">Status</th>
                <th v-if="access.can('absensi.approve')" class="px-4 py-2 text-right">Keputusan</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="c in list" :key="c.id" class="border-t border-surface-low">
                <td class="px-4 py-2 font-semibold capitalize">{{ c.type }}</td>
                <td class="px-4 py-2">{{ tanggal(c.fromDate) }} – {{ tanggal(c.toDate) }}</td>
                <td class="px-4 py-2 text-center">{{ durasi(c) }}</td>
                <td class="max-w-56 truncate px-4 py-2 text-secondary" :title="c.reason">{{ c.reason }}</td>
                <td class="px-4 py-2"><AppBadge :tone="tone(c.status)">{{ c.status }}</AppBadge></td>
                <td v-if="access.can('absensi.approve')" class="px-4 py-2 text-right">
                  <span v-if="c.status !== 'diajukan'" class="text-xs text-secondary">Selesai</span>
                  <span v-else class="flex justify-end gap-1">
                    <AppButton variant="ghost" @click="putuskan(c, true)">Ya</AppButton>
                    <AppButton variant="ghost" @click="putuskan(c, false)">Tidak</AppButton>
                  </span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <AppPager :meta="meta" @page="muat" />
      </div>

      <div class="lg:col-span-2">
        <AppCard title="Formulir pengajuan">
          <AppField label="Tipe">
            <select v-model="form.type" class="w-full rounded-lg border border-outline bg-card px-3 py-2.5 focus:border-primary focus:outline-none">
              <option v-for="t in TYPES" :key="t" :value="t">{{ t }}</option>
            </select>
          </AppField>
          <div class="mt-3 grid grid-cols-2 gap-2">
            <AppField label="Dari (Y-m-d)"><input v-model="form.from_date" type="date" class="w-full rounded-lg border border-outline bg-card px-3 py-2.5 focus:border-primary focus:outline-none" /></AppField>
            <AppField label="Sampai (≥ dari)"><input v-model="form.to_date" type="date" class="w-full rounded-lg border border-outline bg-card px-3 py-2.5 focus:border-primary focus:outline-none" /></AppField>
          </div>
          <p v-if="estimasi" class="mt-1 text-xs text-secondary">{{ estimasi }}</p>
          <div class="mt-3">
            <AppField label="Alasan keperluan">
              <textarea v-model="form.reason" rows="3" maxlength="500" class="w-full rounded-lg border border-outline bg-card px-3 py-2.5 focus:border-primary focus:outline-none" placeholder="Contoh: kontrol ke dokter gigi" />
            </AppField>
          </div>
          <p class="mt-2 rounded-lg bg-surface-low p-2 text-xs text-secondary">
            Cuti dan sakit tidak memotong gaji. Izin tanpa persetujuan berisiko tercatat alpa (potong 1/30 gaji pokok per hari).
          </p>
          <AppButton :loading="loading" class="mt-3 w-full" @click="ajukan">
            <AppIcon name="send" :size="18" />Kirim pengajuan
          </AppButton>
        </AppCard>
      </div>
    </div>
  </div>
</template>
