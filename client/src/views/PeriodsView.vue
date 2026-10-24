<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { api } from '../lib/api'
import { tanggal } from '../lib/format'
import { useCompany } from '../composables/useCompany'
import { useAccessStore } from '../stores/access'
import { useAsync } from '../composables/useAsync'
import { usePagedList } from '../composables/usePagedList'
import { useConfirm } from '../composables/useConfirm'
import { useToast } from '../composables/useToast'
import AppBadge from '../components/ui/AppBadge.vue'
import AppBanner from '../components/ui/AppBanner.vue'
import AppButton from '../components/ui/AppButton.vue'
import AppCard from '../components/ui/AppCard.vue'
import AppField from '../components/ui/AppField.vue'
import AppInput from '../components/ui/AppInput.vue'
import AppModal from '../components/ui/AppModal.vue'
import AppPageHeader from '../components/ui/AppPageHeader.vue'
import AppPager from '../components/ui/AppPager.vue'
import AppSelect from '../components/ui/AppSelect.vue'
import AppStat from '../components/ui/AppStat.vue'
import AppTable from '../components/ui/AppTable.vue'
import AppTabs from '../components/ui/AppTabs.vue'

interface Periode {
  id: number
  year: number
  month: number
  label: string
  status: string
  closedAt: string | null
}

const BULAN = [
  'Januari',
  'Februari',
  'Maret',
  'April',
  'Mei',
  'Juni',
  'Juli',
  'Agustus',
  'September',
  'Oktober',
  'November',
  'Desember',
]

const { auth, companyId } = useCompany()
const access = useAccessStore()
const toast = useToast()
const { confirm } = useConfirm()
const { loading: mutasiLoading, error: mutasiError, run: runMutasi } = useAsync()

const tabStatus = ref('semua')
const statistik = ref({ total: 0, buka: 0, tutup: 0 })
const statistikLoading = ref(false)
const tutupOpen = ref(false)
const tahun = ref<number | null>(new Date().getFullYear())
const bulan = ref(String(new Date().getMonth() + 1))

const {
  rows: list,
  meta,
  loading,
  error: errorList,
  load,
  setPage,
  setPerPage,
} = usePagedList<Periode>((page, perPage) => {
  const params = new URLSearchParams({ per_page: String(perPage), page: String(page) })
  if (tabStatus.value !== 'semua') params.set('status', tabStatus.value)
  return api.getPage<Periode>(
    `/companies/${companyId.value}/accounting-periods?${params}`,
    auth.token,
  )
})

const error = computed(() => errorList.value || mutasiError.value)
const boleh = computed(() => access.can('periode.tutup'))
const tabs = computed(() => [
  { key: 'semua', label: 'Semua', count: statistik.value.total },
  { key: 'buka', label: 'Terbuka', count: statistik.value.buka },
  { key: 'tutup', label: 'Tertutup', count: statistik.value.tutup },
])
const opsiBulan = BULAN.map((nama, index) => ({
  value: String(index + 1),
  label: nama,
}))
const statusLabel = (status: string) => (status === 'buka' ? 'Terbuka' : 'Tertutup')
const statusTone = (status: string) => (status === 'buka' ? 'green' : 'gray')

async function muatStatistik() {
  statistikLoading.value = true
  try {
    const base = `/companies/${companyId.value}/accounting-periods?per_page=1`
    const [semua, buka, tutup] = await Promise.all([
      api.getPage<Periode>(base, auth.token).catch(() => null),
      api.getPage<Periode>(`${base}&status=buka`, auth.token).catch(() => null),
      api.getPage<Periode>(`${base}&status=tutup`, auth.token).catch(() => null),
    ])
    statistik.value = {
      total: semua?.meta?.total ?? 0,
      buka: buka?.meta?.total ?? 0,
      tutup: tutup?.meta?.total ?? 0,
    }
  } finally {
    statistikLoading.value = false
  }
}

async function terapkanStatus() {
  await load(1)
}

function bukaTutup() {
  tahun.value = new Date().getFullYear()
  bulan.value = String(new Date().getMonth() + 1)
  tutupOpen.value = true
}

async function tutupPeriode() {
  if (!tahun.value) return
  const res = await runMutasi(() =>
    api.post(
      `/companies/${companyId.value}/accounting-periods/close`,
      {
        year: Number(tahun.value),
        month: Number(bulan.value),
      },
      auth.token,
    ),
  )
  if (!res) return

  tutupOpen.value = false
  toast.success('Periode ditutup. Posting ke bulan itu ditolak.')
  await Promise.all([load(1), muatStatistik()])
}

async function bukaLagi(periode: Periode) {
  const yakin = await confirm({
    title: `Buka lagi periode ${periode.label}?`,
    message: 'Posting backdate ke bulan ini akan diizinkan kembali.',
    confirmLabel: 'Buka periode',
  })
  if (!yakin) return

  const res = await runMutasi(() =>
    api.post(
      `/companies/${companyId.value}/accounting-periods/${periode.id}/reopen`,
      {},
      auth.token,
    ),
  )
  if (!res) return

  toast.success(`Periode ${periode.label} dibuka lagi.`)
  await Promise.all([load(), muatStatistik()])
}

onMounted(async () => {
  await access.load(companyId.value, auth.token)
  await Promise.all([load(1), muatStatistik()])
})
</script>

<template>
  <div>
    <AppPageHeader
      eyebrow="Kunci pembukuan"
      title="Tutup periode"
      subtitle="Kunci bulan buku agar posting backdate dan perubahan lama tidak masuk."
      icon="calendar_month"
    >
      <template #actions>
        <AppButton v-if="boleh" @click="bukaTutup">Tutup periode</AppButton>
      </template>
    </AppPageHeader>

    <AppBanner tone="red" :message="error" />

    <div class="mb-4 grid gap-3 sm:grid-cols-3">
      <AppStat
        label="Total periode"
        :value="statistik.total"
        icon="calendar_month"
        :loading="statistikLoading"
      />
      <AppStat
        label="Terbuka"
        :value="statistik.buka"
        icon="lock_open"
        tone="positive"
        :loading="statistikLoading"
      />
      <AppStat
        label="Tertutup"
        :value="statistik.tutup"
        icon="lock"
        :loading="statistikLoading"
      />
    </div>

    <AppCard>
      <AppTabs
        v-model="tabStatus"
        :items="tabs"
        label="Status periode"
        @update:model-value="terapkanStatus"
      />

      <div class="mt-4">
        <AppTable
          :loading="loading"
          :empty="!list.length"
          :colspan="boleh ? 4 : 3"
          empty-title="Belum ada periode"
          empty-message="Tutup bulan buku pertama untuk mulai mengunci posting."
        >
          <template #head>
            <tr>
              <th>Periode</th>
              <th>Status</th>
              <th>Ditutup pada</th>
              <th v-if="boleh" class="text-right">Aksi</th>
            </tr>
          </template>

          <tr v-for="item in list" :key="item.id">
            <td>
              <p class="font-semibold text-ink">{{ item.label }}</p>
              <p class="text-xs text-secondary">{{ item.month }}/{{ item.year }}</p>
            </td>
            <td>
              <AppBadge :tone="statusTone(item.status)" dot>
                {{ statusLabel(item.status) }}
              </AppBadge>
            </td>
            <td class="text-sm text-secondary">
              {{ item.closedAt ? tanggal(item.closedAt) : '—' }}
            </td>
            <td v-if="boleh" class="text-right">
              <AppButton
                v-if="item.status !== 'buka'"
                size="sm"
                variant="ghost"
                :aria-label="`Buka lagi periode ${item.label}`"
                @click="bukaLagi(item)"
              >
                Buka lagi
              </AppButton>
              <span v-else class="text-xs text-secondary">Masih bisa posting</span>
            </td>
          </tr>
        </AppTable>

        <AppPager :meta="meta" @page="setPage" @update:per-page="setPerPage" />
      </div>
    </AppCard>

    <AppModal
      :open="tutupOpen"
      title="Tutup bulan buku"
      description="Syarat: tidak ada biaya draft dan tidak ada gaji belum dibayar di bulan itu."
      size="sm"
      @close="tutupOpen = false"
    >
      <AppBanner tone="red" :message="mutasiError" />
      <form id="close-period-form" class="space-y-3" @submit.prevent="tutupPeriode">
        <AppField label="Tahun" required>
          <AppInput v-model="tahun" type="number" min="2000" max="2100" />
        </AppField>
        <AppField label="Bulan" required>
          <AppSelect v-model="bulan" :options="opsiBulan" />
        </AppField>
      </form>
      <template #footer>
        <AppButton variant="ghost" @click="tutupOpen = false">Batal</AppButton>
        <AppButton
          type="submit"
          form="close-period-form"
          :loading="mutasiLoading"
          :disabled="!tahun"
        >
          Tutup periode
        </AppButton>
      </template>
    </AppModal>
  </div>
</template>
