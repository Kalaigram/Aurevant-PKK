<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { api } from '../lib/api'
import { tanggal } from '../lib/format'
import { labelLeaveStatus, labelLeaveType } from '../lib/labels'
import { useCompany } from '../composables/useCompany'
import { useAccessStore } from '../stores/access'
import { useAsync } from '../composables/useAsync'
import { usePagedList } from '../composables/usePagedList'
import AppBadge from '../components/ui/AppBadge.vue'
import AppBanner from '../components/ui/AppBanner.vue'
import AppButton from '../components/ui/AppButton.vue'
import AppCard from '../components/ui/AppCard.vue'
import AppDateInput from '../components/ui/AppDateInput.vue'
import AppField from '../components/ui/AppField.vue'
import AppIcon from '../components/ui/AppIcon.vue'
import AppPageHeader from '../components/ui/AppPageHeader.vue'
import AppPager from '../components/ui/AppPager.vue'
import AppSelect from '../components/ui/AppSelect.vue'
import AppStat from '../components/ui/AppStat.vue'
import AppTable from '../components/ui/AppTable.vue'
import AppTabs from '../components/ui/AppTabs.vue'
import AppTextarea from '../components/ui/AppTextarea.vue'

interface Cuti {
  id: number
  userId: number
  type: string
  fromDate: string
  toDate: string
  reason: string
  status: string
}

const TYPES = ['izin', 'sakit', 'cuti'] as const
const TONE: Record<string, 'green' | 'red' | 'amber'> = {
  disetujui: 'green',
  ditolak: 'red',
  diajukan: 'amber',
}
const STATUS_TABS = [
  { key: 'semua', label: 'Semua' },
  { key: 'diajukan', label: 'Diajukan' },
  { key: 'disetujui', label: 'Disetujui' },
  { key: 'ditolak', label: 'Ditolak' },
]

const { auth, companyId } = useCompany()
const access = useAccessStore()
const { loading: loadingMutasi, error: errorMutasi, run } = useAsync()
const status = ref('')
const okMsg = ref('')
const memproses = ref<number | null>(null)
const form = ref({ type: 'izin', from_date: '', to_date: '', reason: '' })
const bisaLihatSemua = computed(
  () =>
    Boolean(auth.user?.isSuperAdmin) ||
    access.can('absensi.approve') ||
    access.can('karyawan.kelola'),
)
const {
  rows: list,
  meta,
  loading: loadingList,
  error: errorList,
  load: muat,
  setPage,
  setPerPage,
} = usePagedList<Cuti>(
  (page, perPage) => {
    const me = auth.user?.id ?? 0
    const params = new URLSearchParams({ per_page: String(perPage), page: String(page) })
    if (!bisaLihatSemua.value) params.set('user_id', String(me))
    if (status.value) params.set('status', status.value)
    return api.getPage<Cuti>(
      `/companies/${companyId.value}/leaves?${params}`,
      auth.token,
    )
  },
  { perPage: 15 },
)
const error = computed(() => errorList.value || errorMutasi.value)
const bolehSetuju = computed(() => access.can('absensi.approve'))
const colspan = computed(() => (bolehSetuju.value ? 6 : 5))

const opsiTipe = TYPES.map((type) => ({ value: type, label: labelLeaveType(type) }))
const filterTab = computed({
  get: () => status.value || 'semua',
  set: (value: string) => {
    status.value = value === 'semua' ? '' : value
    void muat(1)
  },
})

const halamanRingkas = computed(() => ({
  diajukan: list.value.filter((item) => item.status === 'diajukan').length,
  disetujui: list.value.filter((item) => item.status === 'disetujui').length,
  ditolak: list.value.filter((item) => item.status === 'ditolak').length,
}))

function durasi(item: Cuti) {
  const from = new Date(`${item.fromDate}T00:00:00`).getTime()
  const to = new Date(`${item.toDate}T00:00:00`).getTime()
  if (Number.isNaN(from) || Number.isNaN(to) || to < from) return '—'
  return `${Math.round((to - from) / 864e5) + 1} hari`
}

const durasiForm = computed(() => {
  const from = new Date(`${form.value.from_date}T00:00:00`).getTime()
  const to = new Date(`${form.value.to_date}T00:00:00`).getTime()
  if (Number.isNaN(from) || Number.isNaN(to)) return 0
  if (to < from) return 0
  return Math.round((to - from) / 864e5) + 1
})
const bisaAjukan = computed(
  () =>
    Boolean(form.value.type) &&
    Boolean(form.value.from_date) &&
    Boolean(form.value.to_date) &&
    form.value.to_date >= form.value.from_date &&
    Boolean(form.value.reason.trim()),
)
const petunjukForm = computed(() => {
  if (!form.value.from_date || !form.value.to_date) return 'Lengkapi tanggal mulai dan selesai.'
  if (form.value.to_date < form.value.from_date) {
    return 'Tanggal selesai tidak boleh lebih awal dari tanggal mulai.'
  }
  if (!form.value.reason.trim()) return 'Alasan wajib diisi agar pengajuan bisa diproses.'
  return `Perkiraan durasi: ${durasiForm.value} hari.`
})

async function ajukan() {
  if (!bisaAjukan.value) return
  const res = await run(() =>
    api.post(
      `/companies/${companyId.value}/users/${auth.user?.id}/leaves`,
      { ...form.value },
      auth.token,
    ),
  )
  if (res) {
    okMsg.value = 'Pengajuan terkirim, menunggu atasan.'
    form.value = { type: 'izin', from_date: '', to_date: '', reason: '' }
    await muat(1)
  }
}

async function putuskan(item: Cuti, setuju: boolean) {
  if (memproses.value !== null) return
  memproses.value = item.id
  const res = await run(() =>
    api.post(
      `/companies/${companyId.value}/leaves/${item.id}/decide`,
      { approve: setuju },
      auth.token,
    ),
  )
  if (res) {
    okMsg.value = setuju ? 'Pengajuan disetujui.' : 'Pengajuan ditolak.'
    await muat()
  }
  memproses.value = null
}

onMounted(async () => {
  await access.load(companyId.value, auth.token)
  await muat()
})
</script>

<template>
  <div>
    <AppPageHeader
      eyebrow="Kepegawaian · saya"
      title="Cuti, izin & sakit"
      subtitle="Ajukan ketidakhadiran dan pantau keputusan atasan dalam satu tempat."
      icon="event_note"
    />

    <AppBanner tone="green" :message="okMsg" />
    <AppBanner tone="red" :message="error" />

    <div class="mb-2 flex flex-wrap items-end justify-between gap-2">
      <p class="text-xs text-secondary">
        Ringkasan {{ list.length }} pengajuan pada halaman ini.
      </p>
    </div>
    <div class="mb-4 grid grid-cols-2 gap-3 sm:grid-cols-3">
      <AppStat
        label="Menunggu"
        :value="halamanRingkas.diajukan"
        icon="hourglass_top"
        tone="warning"
      />
      <AppStat
        label="Disetujui"
        :value="halamanRingkas.disetujui"
        icon="check_circle"
        tone="positive"
      />
      <AppStat
        class="col-span-2 sm:col-span-1"
        label="Ditolak"
        :value="halamanRingkas.ditolak"
        icon="cancel"
        tone="negative"
      />
    </div>

    <div class="grid gap-4 lg:grid-cols-5">
      <div class="min-w-0 lg:col-span-3">
        <AppTabs
          v-model="filterTab"
          :items="STATUS_TABS"
          label="Filter status pengajuan"
          id-prefix="leave-tab"
        />
        <div class="mt-3">
          <AppTable
            caption="Riwayat pengajuan cuti"
            :loading="loadingList"
            :empty="!list.length"
            :colspan="colspan"
            empty-title="Belum ada pengajuan"
            empty-message="Ganti filter status atau ajukan cuti baru dari formulir di samping."
          >
            <template #head>
              <tr>
                <th>Jenis</th>
                <th>Rentang</th>
                <th>Durasi</th>
                <th>Alasan</th>
                <th>Status</th>
                <th v-if="bolehSetuju" class="text-right">Keputusan</th>
              </tr>
            </template>
            <tr v-for="item in list" :key="item.id">
              <td class="font-semibold text-ink">{{ labelLeaveType(item.type) }}</td>
              <td class="whitespace-nowrap text-sm">
                {{ tanggal(item.fromDate) }} – {{ tanggal(item.toDate) }}
              </td>
              <td class="text-sm tabular-nums">{{ durasi(item) }}</td>
              <td class="max-w-56 text-sm text-secondary">
                <p class="truncate" :title="item.reason">{{ item.reason || '—' }}</p>
              </td>
              <td>
                <AppBadge :tone="TONE[item.status] ?? 'amber'" dot>
                  {{ labelLeaveStatus(item.status) }}
                </AppBadge>
              </td>
              <td v-if="bolehSetuju" class="text-right">
                <div
                  v-if="item.status === 'diajukan'"
                  class="flex justify-end gap-1"
                >
                  <AppButton
                    size="sm"
                    variant="ghost"
                    :loading="memproses === item.id"
                    :disabled="memproses !== null && memproses !== item.id"
                    @click="putuskan(item, true)"
                  >
                    Setujui
                  </AppButton>
                  <AppButton
                    size="sm"
                    variant="ghost"
                    :disabled="memproses !== null"
                    @click="putuskan(item, false)"
                  >
                    Tolak
                  </AppButton>
                </div>
                <span v-else class="text-xs text-secondary">Selesai</span>
              </td>
            </tr>
          </AppTable>
          <AppPager :meta="meta" @page="setPage" @update:per-page="setPerPage" />
        </div>
      </div>

      <div class="min-w-0 lg:col-span-2">
        <AppCard title="Formulir pengajuan">
          <form class="space-y-3" @submit.prevent="ajukan">
            <AppField label="Jenis ketidakhadiran" required>
              <AppSelect v-model="form.type" :options="opsiTipe" />
            </AppField>
            <div class="grid gap-3 sm:grid-cols-2">
              <AppField label="Dari tanggal" required>
                <AppDateInput v-model="form.from_date" />
              </AppField>
              <AppField label="Sampai tanggal" required>
                <AppDateInput v-model="form.to_date" />
              </AppField>
            </div>
            <p class="text-xs leading-relaxed text-secondary">{{ petunjukForm }}</p>
            <AppField label="Alasan keperluan" required>
              <AppTextarea
                v-model="form.reason"
                :rows="3"
                :maxlength="500"
                :show-count="true"
                placeholder="cth. Kontrol ke dokter gigi"
              />
            </AppField>
            <p class="rounded-xl bg-surface-low p-3 text-xs leading-relaxed text-secondary">
              Cuti dan sakit tidak memotong gaji. Izin tanpa persetujuan berisiko tercatat alpa
              (potong 1/30 gaji pokok per hari).
            </p>
            <AppButton type="submit" :loading="loadingMutasi" :disabled="!bisaAjukan" block>
              <AppIcon name="send" :size="18" />Kirim pengajuan
            </AppButton>
          </form>
        </AppCard>
      </div>
    </div>
  </div>
</template>
