<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { api } from '../lib/api'
import { rupiah, rupiahRingkas, tanggal } from '../lib/format'
import { labelPayrollComponentType, labelPayrollStatus } from '../lib/labels'
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
import AppCheckbox from '../components/ui/AppCheckbox.vue'
import AppDateInput from '../components/ui/AppDateInput.vue'
import AppEmpty from '../components/ui/AppEmpty.vue'
import AppField from '../components/ui/AppField.vue'
import AppInput from '../components/ui/AppInput.vue'
import AppModal from '../components/ui/AppModal.vue'
import AppMoneyInput from '../components/ui/AppMoneyInput.vue'
import AppPageHeader from '../components/ui/AppPageHeader.vue'
import AppPager from '../components/ui/AppPager.vue'
import AppSearch from '../components/ui/AppSearch.vue'
import AppSelect from '../components/ui/AppSelect.vue'
import AppStat from '../components/ui/AppStat.vue'
import AppTable from '../components/ui/AppTable.vue'
import AppTabs from '../components/ui/AppTabs.vue'

interface Komponen {
  id: number
  name: string
  calcType: string
  deductible: boolean
  taxable: boolean
  bpjsBase: boolean
  active: boolean
}
interface Periode {
  id: number
  name: string
  from: string
  to: string
  divisor: number
  status: string
}
interface Penempatan {
  userId: number
  userName: string | null
  endedAt: string | null
}
interface BarisGaji {
  id?: number
  label: string
  amount: number | string
  isManual?: boolean
  is_manual?: boolean
}
interface KaryawanGaji {
  user_id: number
  name: string
  net: number | string
  lines: BarisGaji[]
}
interface DetailPeriode {
  period: Periode
  employees: KaryawanGaji[]
}

const STATUS_PERIODE = ['draft', 'review', 'disetujui', 'dibayar'] as const
const JENIS = ['monthly_fixed', 'daily_rate', 'commission', 'manual'] as const
const statusTone = (status: string) =>
  (status === 'dibayar' || status === 'disetujui'
    ? 'green'
    : status === 'review'
      ? 'blue'
      : 'gray') as 'green' | 'blue' | 'gray'

const { auth, companyId } = useCompany()
const access = useAccessStore()
const toast = useToast()
const { confirm } = useConfirm()

const tab = ref<'komponen' | 'periode'>('komponen')
const referensi = ref<Penempatan[]>([])
const cariKomponen = ref('')
const filterKomponen = ref('')
const cariPeriode = ref('')
const filterStatusPeriode = ref('')

const { loading: mutasiLoading, error: mutasiError, run: runMutasi } = useAsync()
const { loading: detailLoading, error: detailError, run: runDetail } = useAsync()
const { loading: referensiLoading, error: referensiError, run: runReferensi } = useAsync()

const {
  rows: komponen,
  meta: metaKomponen,
  loading: loadingKomponen,
  error: errorKomponen,
  load: loadKomponen,
  setPage: setPageKomponen,
  setPerPage: setPerPageKomponen,
} = usePagedList<Komponen>((page, perPage) => {
  const params = new URLSearchParams({ per_page: String(perPage), page: String(page) })
  if (cariKomponen.value.trim()) params.set('q', cariKomponen.value.trim())
  if (filterKomponen.value === 'aktif') params.set('active', '1')
  if (filterKomponen.value === 'nonaktif') params.set('active', '0')
  return api.getPage<Komponen>(
    `/companies/${companyId.value}/salary-components?${params}`,
    auth.token,
  )
})

const {
  rows: periode,
  meta: metaPeriode,
  loading: loadingPeriode,
  error: errorPeriode,
  load: loadPeriode,
  setPage: setPagePeriode,
  setPerPage: setPerPagePeriode,
} = usePagedList<Periode>((page, perPage) => {
  const params = new URLSearchParams({ per_page: String(perPage), page: String(page) })
  if (cariPeriode.value.trim()) params.set('q', cariPeriode.value.trim())
  if (filterStatusPeriode.value) params.set('status', filterStatusPeriode.value)
  return api.getPage<Periode>(`/companies/${companyId.value}/payroll-periods?${params}`, auth.token)
})

const idPeriode = ref<number | null>(null)
const detailPeriode = ref<Periode | null>(null)
const karyawanGaji = ref<KaryawanGaji[]>([])
const rincian = ref<KaryawanGaji | null>(null)
const rincianOpen = ref(false)

const komOpen = ref(false)
const komEdit = ref<Komponen | null>(null)
const fKom = ref(formKomponenKosong())

const tautOpen = ref(false)
const komponenTaut = ref<Komponen[]>([])
const idTaut = ref('')
const idKomTaut = ref('')
const nominalTaut = ref<number | null>(null)

const perOpen = ref(false)
const fPer = ref(formPeriodeKosong())

const sesuaikanOpen = ref(false)
const idSesuai = ref('')
const labelSesuai = ref('')
const nominalSesuai = ref<number | null>(null)

const error = computed(
  () =>
    mutasiError.value ||
    detailError.value ||
    referensiError.value ||
    errorKomponen.value ||
    errorPeriode.value,
)
const bolehKelola = computed(() => access.can('gaji.kelola'))
const bolehSetuju = computed(() => access.can('gaji.setujui'))
const tabs = computed(() => [
  { key: 'komponen', label: 'Komponen', count: metaKomponen.value?.total ?? 0 },
  { key: 'periode', label: 'Periode', count: metaPeriode.value?.total ?? 0 },
])
const opsiKomponen = computed(() =>
  (komponenTaut.value.length ? komponenTaut.value : komponen.value).map((item) => ({
    value: String(item.id),
    label: item.name,
  })),
)
const opsiStatus = computed(() =>
  STATUS_PERIODE.map((status) => ({ value: status, label: labelPayrollStatus(status) })),
)
const opsiJenis = JENIS.map((value) => ({
  value,
  label: labelPayrollComponentType(value),
}))
const opsiKaryawan = computed(() => {
  const seen = new Map<number, string>()
  for (const item of referensi.value) {
    if (item.userName && !seen.has(item.userId)) seen.set(item.userId, item.userName)
  }
  return [...seen.entries()]
    .map(([id, name]) => ({ value: String(id), label: name }))
    .sort((a, b) => a.label.localeCompare(b.label))
})
const bisaHitung = computed(() => ['draft', 'review'].includes(detailPeriode.value?.status ?? ''))
const bisaReview = computed(() => detailPeriode.value?.status === 'draft')
const bisaSetujui = computed(() => bolehSetuju.value && detailPeriode.value?.status === 'review')
const bisaBayar = computed(() => detailPeriode.value?.status === 'disetujui')
const bisaSesuaikan = computed(() =>
  ['draft', 'review'].includes(detailPeriode.value?.status ?? ''),
)
const totalNet = computed(() =>
  karyawanGaji.value.reduce((total, item) => total + Number(item.net), 0),
)
const rataNet = computed(() =>
  karyawanGaji.value.length ? totalNet.value / karyawanGaji.value.length : 0,
)
const rentangDetail = computed(() =>
  detailPeriode.value
    ? `${tanggal(detailPeriode.value.from)} - ${tanggal(detailPeriode.value.to)}`
    : 'Pilih periode untuk melihat ringkasan.',
)
const periodeRingkas = computed(() => {
  const from = detailPeriode.value?.from
  if (!from) return '-'
  const date = new Date(`${from}T00:00:00`)
  if (Number.isNaN(date.getTime())) return '-'
  const bulan = date.toLocaleDateString('id-ID', { month: 'short' })
  return `${bulan} ${String(date.getFullYear()).slice(2)}`
})

function formKomponenKosong() {
  return {
    name: '',
    calc_type: 'monthly_fixed',
    deductible: true,
    taxable: false,
    bpjs_base: false,
    active: true,
  }
}

function formPeriodeKosong() {
  return {
    name: '',
    date_from: '',
    date_to: '',
    divisor: 30,
  }
}

async function muatReferensi() {
  const res = await runReferensi(() =>
    api.getPage<Penempatan>(
      `/companies/${companyId.value}/access/assignments?active=1&per_page=100`,
      auth.token,
    ),
  )
  if (res) referensi.value = res.data
}

async function terapkanFilterKomponen() {
  await loadKomponen(1)
}

function bukaKomBaru() {
  komEdit.value = null
  fKom.value = formKomponenKosong()
  komOpen.value = true
}

function bukaKomUbah(item: Komponen) {
  komEdit.value = item
  fKom.value = {
    name: item.name,
    calc_type: item.calcType,
    deductible: item.deductible,
    taxable: item.taxable,
    bpjs_base: item.bpjsBase,
    active: item.active,
  }
  komOpen.value = true
}

async function simpanKomponen() {
  const payload = {
    name: fKom.value.name.trim(),
    calc_type: fKom.value.calc_type,
    deductible: fKom.value.deductible,
    taxable: fKom.value.taxable,
    bpjs_base: fKom.value.bpjs_base,
    active: fKom.value.active,
  }
  const base = `/companies/${companyId.value}/salary-components`
  const res = komEdit.value
    ? await runMutasi(() => api.put(base + `/${komEdit.value!.id}`, payload, auth.token))
    : await runMutasi(() => api.post(base, payload, auth.token))
  if (!res) return

  komOpen.value = false
  toast.success(komEdit.value ? 'Komponen diperbarui.' : 'Komponen dibuat.')
  await loadKomponen(metaKomponen.value?.current_page ?? 1)
}

async function bukaTaut() {
  idTaut.value = ''
  idKomTaut.value = ''
  nominalTaut.value = null
  tautOpen.value = true

  const res = await runReferensi(() =>
    api.getPage<Komponen>(`/companies/${companyId.value}/salary-components?per_page=100`, auth.token),
  )
  if (res) komponenTaut.value = res.data
}

async function tautkan() {
  if (!idTaut.value || !idKomTaut.value) return
  await runMutasi(() =>
    api.post(
      `/companies/${companyId.value}/users/${idTaut.value}/salary-components`,
      {
        component_id: Number(idKomTaut.value),
        amount: nominalTaut.value ?? 0,
      },
      auth.token,
    ),
  )
  if (mutasiError.value) return

  tautOpen.value = false
  toast.success('Komponen ditetapkan ke karyawan.')
}

function bukaPeriodeBaru() {
  fPer.value = formPeriodeKosong()
  perOpen.value = true
}

async function buatPeriode() {
  const res = await runMutasi(() =>
    api.post<Periode>(
      `/companies/${companyId.value}/payroll-periods`,
      {
        name: fPer.value.name.trim(),
        date_from: fPer.value.date_from,
        date_to: fPer.value.date_to,
        divisor: Number(fPer.value.divisor),
      },
      auth.token,
    ),
  )
  if (!res) return

  perOpen.value = false
  cariPeriode.value = ''
  filterStatusPeriode.value = ''
  idPeriode.value = res.id
  toast.success('Periode gaji dibuat.')
  await loadPeriode(1)
  await muatDetail()
}

async function muatDetail() {
  if (!idPeriode.value) {
    detailPeriode.value = null
    karyawanGaji.value = []
    return
  }
  const res = await runDetail(() =>
    api.get<DetailPeriode>(
      `/companies/${companyId.value}/payroll-periods/${idPeriode.value}`,
      auth.token,
    ),
  )
  if (!res) return
  detailPeriode.value = res.period
  karyawanGaji.value = res.employees ?? []
}

function pilihPeriode(item: Periode) {
  idPeriode.value = item.id
  void muatDetail()
}

async function gantiHalamanPeriode(page: number) {
  await setPagePeriode(page)
  if (!periode.value.some((item) => item.id === idPeriode.value)) {
    idPeriode.value = periode.value[0]?.id ?? null
  }
  await muatDetail()
}

async function terapkanFilterPeriode() {
  await loadPeriode(1)
  if (!periode.value.some((item) => item.id === idPeriode.value)) {
    idPeriode.value = periode.value[0]?.id ?? null
  }
  await muatDetail()
}

async function gantiPerHalamanPeriode(size: number) {
  await setPerPagePeriode(size)
  if (!periode.value.some((item) => item.id === idPeriode.value)) {
    idPeriode.value = periode.value[0]?.id ?? null
  }
  await muatDetail()
}

async function muatUlangPeriode() {
  await loadPeriode()
  if (!periode.value.some((item) => item.id === idPeriode.value)) {
    idPeriode.value = periode.value[0]?.id ?? null
  }
  await muatDetail()
}

async function hitung() {
  if (!idPeriode.value) return
  const res = await runMutasi(() =>
    api.post<{ employees: { user_id: number; net: number | string }[] }>(
      `/companies/${companyId.value}/payroll-periods/${idPeriode.value}/calculate`,
      {},
      auth.token,
    ),
  )
  if (!res) return

  toast.success(`Perhitungan selesai: ${res.employees?.length ?? 0} karyawan.`)
  await Promise.all([loadPeriode(), muatDetail()])
}

async function alur(aksi: 'review' | 'approve' | 'pay') {
  if (!idPeriode.value) return

  if (aksi === 'approve' || aksi === 'pay') {
    const yakin = await confirm({
      title: aksi === 'approve' ? 'Setujui dan kunci periode?' : 'Tandai periode sebagai dibayar?',
      message:
        aksi === 'approve'
          ? 'Setelah disetujui, gaji tidak bisa dihitung ulang atau disesuaikan.'
          : 'Jurnal pembayaran gaji akan dibuat dan periode terkunci.',
      confirmLabel: aksi === 'approve' ? 'Setujui & kunci' : 'Tandai dibayar',
      tone: aksi === 'approve' ? 'primary' : 'danger',
    })
    if (!yakin) return
  }

  const res = await runMutasi(() =>
    api.post(
      `/companies/${companyId.value}/payroll-periods/${idPeriode.value}/${aksi}`,
      {},
      auth.token,
    ),
  )
  if (!res) return

  toast.success(
    aksi === 'review'
      ? 'Periode dikirim ke review.'
      : aksi === 'approve'
        ? 'Periode disetujui dan dikunci.'
        : 'Periode dibayar dan dijurnal.',
  )
  await Promise.all([loadPeriode(), muatDetail()])
}

function bukaSesuaikan() {
  idSesuai.value = ''
  labelSesuai.value = ''
  nominalSesuai.value = null
  sesuaikanOpen.value = true
}

async function tambahSesuai() {
  if (!idPeriode.value || !idSesuai.value || !labelSesuai.value.trim()) return
  const res = await runMutasi(() =>
    api.post(
      `/companies/${companyId.value}/payroll-periods/${idPeriode.value}/users/${idSesuai.value}/adjustments`,
      {
        label: labelSesuai.value.trim(),
        amount: nominalSesuai.value ?? 0,
      },
      auth.token,
    ),
  )
  if (!res) return

  sesuaikanOpen.value = false
  toast.success('Penyesuaian tersimpan.')
  await muatDetail()
}

function bukaRincian(item: KaryawanGaji) {
  rincian.value = item
  rincianOpen.value = true
}

onMounted(async () => {
  await access.load(companyId.value, auth.token)
  await Promise.all([muatReferensi(), loadKomponen(1), loadPeriode(1)])
  idPeriode.value = periode.value[0]?.id ?? null
  await muatDetail()
})
</script>

<template>
  <div>
    <AppPageHeader
      eyebrow="Gaji berbasis kehadiran"
      title="Penggajian"
      subtitle="Atur komponen gaji, hitung periode, setujui, dan tandai pembayaran."
      icon="payments"
    >
      <template #actions>
        <template v-if="bolehKelola">
          <template v-if="tab === 'komponen'">
            <AppButton variant="ghost" @click="bukaTaut">Tetapkan komponen</AppButton>
            <AppButton @click="bukaKomBaru">Komponen baru</AppButton>
          </template>
          <AppButton v-else @click="bukaPeriodeBaru">Periode baru</AppButton>
        </template>
      </template>
    </AppPageHeader>

    <AppBanner tone="red" :message="error" />

    <AppEmpty
      v-if="!bolehKelola"
      title="Akses terbatas"
      message="Halaman ini hanya untuk pengelola gaji."
    />

    <template v-else>
      <div class="mb-4 grid grid-cols-2 gap-3 lg:grid-cols-4">
        <AppStat
          label="Komponen"
          :value="metaKomponen?.total ?? 0"
          icon="payments"
          :loading="loadingKomponen"
        />
        <AppStat
          label="Periode"
          :value="metaPeriode?.total ?? 0"
          icon="calendar_month"
          :loading="loadingPeriode"
        />
        <AppStat
          label="Karyawan aktif"
          :value="opsiKaryawan.length"
          icon="groups"
          :loading="referensiLoading"
        />
        <AppStat
          label="Periode dipilih"
          :value="periodeRingkas"
          icon="event_available"
          :hint="rentangDetail"
        />
      </div>

      <AppCard>
        <AppTabs v-model="tab" :items="tabs" label="Bagian penggajian" />

        <div v-if="tab === 'komponen'" class="mt-4">
          <div class="mb-3 flex flex-wrap items-end gap-2">
            <AppSearch
              v-model="cariKomponen"
              class="min-w-56 flex-1"
              placeholder="Cari nama komponen..."
              @search="terapkanFilterKomponen"
            />
            <AppSelect
              v-model="filterKomponen"
              class="w-40"
              aria-label="Filter status komponen"
              placeholder="Semua status"
              :options="[
                { value: 'aktif', label: 'Aktif' },
                { value: 'nonaktif', label: 'Nonaktif' },
              ]"
              @update:model-value="terapkanFilterKomponen"
            />
          </div>

          <AppTable
            caption="Komponen gaji"
            :loading="loadingKomponen || referensiLoading"
            :empty="!komponen.length"
            :colspan="6"
            empty-title="Belum ada komponen"
            empty-message="Tambahkan komponen gaji atau ubah filter pencarian."
          >
            <template #head>
              <tr>
                <th>Komponen</th>
                <th>Jenis</th>
                <th>Potong alpa</th>
                <th>Pajak / BPJS</th>
                <th>Status</th>
                <th class="text-right">Aksi</th>
              </tr>
            </template>

            <tr v-for="item in komponen" :key="item.id">
              <td class="font-semibold text-ink">{{ item.name }}</td>
              <td class="text-sm">{{ labelPayrollComponentType(item.calcType) }}</td>
              <td>
                <AppBadge :tone="item.deductible ? 'amber' : 'gray'">
                  {{ item.deductible ? 'Ya' : 'Tidak' }}
                </AppBadge>
              </td>
              <td class="text-xs text-secondary">
                <span v-if="item.taxable">Pajak</span>
                <span v-if="item.taxable && item.bpjsBase"> · </span>
                <span v-if="item.bpjsBase">BPJS</span>
                <span v-if="!item.taxable && !item.bpjsBase">—</span>
              </td>
              <td>
                <AppBadge :tone="item.active ? 'green' : 'gray'">
                  {{ item.active ? 'Aktif' : 'Nonaktif' }}
                </AppBadge>
              </td>
              <td class="text-right">
                <AppButton size="sm" variant="ghost" @click="bukaKomUbah(item)">Ubah</AppButton>
              </td>
            </tr>
          </AppTable>

          <AppPager
            :meta="metaKomponen"
            @page="setPageKomponen"
            @update:per-page="setPerPageKomponen"
          />
        </div>

        <div v-else class="mt-4">
          <div class="mb-3 flex flex-wrap items-end gap-2">
            <AppSearch
              v-model="cariPeriode"
              class="min-w-56 flex-1"
              placeholder="Cari nama periode..."
              @search="terapkanFilterPeriode"
            />
            <AppSelect
              v-model="filterStatusPeriode"
              class="w-40"
              aria-label="Filter status periode"
              placeholder="Semua status"
              :options="opsiStatus"
              @update:model-value="terapkanFilterPeriode"
            />
            <AppButton variant="ghost" @click="muatUlangPeriode">Muat ulang</AppButton>
          </div>

          <AppTable
            caption="Periode penggajian"
            :loading="loadingPeriode"
            :empty="!periode.length"
            :colspan="5"
            empty-title="Belum ada periode gaji"
            empty-message="Buat periode baru atau ubah filter pencarian."
          >
            <template #head>
              <tr>
                <th>Periode</th>
                <th>Rentang</th>
                <th>Pembagi</th>
                <th>Status</th>
                <th class="text-right">Aksi</th>
              </tr>
            </template>

            <tr v-for="item in periode" :key="item.id">
              <td>
                <p class="font-semibold text-ink">{{ item.name }}</p>
                <p v-if="item.id === idPeriode" class="text-xs text-primary">Sedang dibuka</p>
              </td>
              <td class="text-sm text-secondary">
                {{ tanggal(item.from) }} – {{ tanggal(item.to) }}
              </td>
              <td class="text-sm">{{ item.divisor }} hari</td>
              <td>
                <AppBadge :tone="statusTone(item.status)">{{ labelPayrollStatus(item.status) }}</AppBadge>
              </td>
              <td class="text-right">
                <AppButton
                  size="sm"
                  :variant="item.id === idPeriode ? 'subtle' : 'ghost'"
                  @click="pilihPeriode(item)"
                >
                  {{ item.id === idPeriode ? 'Dibuka' : 'Buka' }}
                </AppButton>
              </td>
            </tr>
          </AppTable>

          <AppPager
            :meta="metaPeriode"
            @page="gantiHalamanPeriode"
            @update:per-page="gantiPerHalamanPeriode"
          />

          <div
            v-if="idPeriode"
            class="mt-5 rounded-xl border border-outline/70 bg-surface-low/30 p-4"
          >
            <div class="flex flex-wrap items-start justify-between gap-3">
              <div>
                <p class="text-[11px] font-semibold uppercase tracking-wider text-secondary">
                  Detail periode
                </p>
                <h2 class="font-display text-lg font-semibold text-ink">
                  {{ detailPeriode?.name ?? 'Memuat periode...' }}
                </h2>
                <p v-if="detailPeriode" class="mt-0.5 text-xs text-secondary">
                  {{ tanggal(detailPeriode.from) }} – {{ tanggal(detailPeriode.to) }} · pembagi
                  {{ detailPeriode.divisor }} hari
                </p>
              </div>
              <AppBadge v-if="detailPeriode" :tone="statusTone(detailPeriode.status)">
                {{ labelPayrollStatus(detailPeriode.status) }}
              </AppBadge>
            </div>

            <div class="mt-3 flex flex-wrap gap-2">
              <AppButton v-if="bisaHitung" :loading="mutasiLoading" @click="hitung">
                Hitung gaji
              </AppButton>
              <AppButton
                v-if="bisaReview"
                variant="ghost"
                :loading="mutasiLoading"
                @click="alur('review')"
              >
                Kirim review
              </AppButton>
              <AppButton
                v-if="bisaSetujui"
                variant="ghost"
                :loading="mutasiLoading"
                @click="alur('approve')"
              >
                Setujui & kunci
              </AppButton>
              <AppButton
                v-if="bisaBayar"
                variant="ghost"
                :loading="mutasiLoading"
                @click="alur('pay')"
              >
                Tandai dibayar
              </AppButton>
              <AppButton v-if="bisaSesuaikan" variant="ghost" @click="bukaSesuaikan">
                Penyesuaian
              </AppButton>
            </div>

            <div
              v-if="bolehSetuju && detailPeriode?.status === 'review'"
              class="mt-2 text-xs text-secondary"
            >
              Setujui hanya jika perhitungan sudah sesuai.
            </div>

            <div class="mt-4 grid grid-cols-2 gap-3 lg:grid-cols-3">
              <AppStat
                label="Karyawan dihitung"
                :value="karyawanGaji.length"
                icon="groups"
                :loading="detailLoading"
              />
              <AppStat
                label="Total gaji bersih"
                :value="rupiahRingkas(totalNet)"
                :hint="rupiah(totalNet)"
                icon="account_balance_wallet"
                tone="positive"
                :loading="detailLoading"
              />
              <AppStat
                label="Rata-rata bersih"
                :value="rupiahRingkas(rataNet)"
                :hint="rupiah(rataNet)"
                icon="trending_up"
                :loading="detailLoading"
              />
            </div>

            <div class="mt-4">
              <AppTable
                caption="Rincian gaji karyawan periode terpilih"
                :loading="detailLoading"
                :empty="!karyawanGaji.length"
                :colspan="3"
                empty-title="Belum ada hasil perhitungan"
                empty-message="Klik Hitung gaji untuk membuat rincian karyawan."
                dense
              >
                <template #head>
                  <tr>
                    <th>Karyawan</th>
                    <th class="text-right">Bersih</th>
                    <th class="text-right">Aksi</th>
                  </tr>
                </template>

                <tr v-for="item in karyawanGaji" :key="item.user_id">
                  <td class="font-semibold text-ink">{{ item.name }}</td>
                  <td class="text-right font-semibold">{{ rupiah(item.net) }}</td>
                  <td class="text-right">
                    <div class="flex justify-end gap-1">
                      <AppButton size="sm" variant="ghost" @click="bukaRincian(item)">
                        Rincian
                      </AppButton>
                      <RouterLink
                        :to="`/c/${companyId}/slip?period=${idPeriode}&user=${item.user_id}`"
                        class="inline-flex min-h-8 items-center rounded-lg border border-outline px-3 py-1.5 text-xs font-semibold text-ink transition hover:bg-surface-low"
                      >
                        Slip
                      </RouterLink>
                    </div>
                  </td>
                </tr>
              </AppTable>
            </div>
          </div>
        </div>
      </AppCard>
    </template>

    <AppModal
      :open="komOpen"
      :title="komEdit ? 'Ubah komponen' : 'Tambah komponen'"
      description="Komponen dipakai saat perhitungan gaji periode."
      @close="komOpen = false"
    >
      <form id="salary-component-form" class="space-y-3" @submit.prevent="simpanKomponen">
        <AppField label="Nama komponen" required>
          <AppInput v-model="fKom.name" maxlength="255" placeholder="cth. Tunjangan jabatan" />
        </AppField>
        <AppField label="Cara hitung">
          <AppSelect v-model="fKom.calc_type" :options="opsiJenis" />
        </AppField>
        <div class="grid gap-2 text-sm sm:grid-cols-2">
          <AppCheckbox v-model="fKom.deductible" label="Terpotong alpa" />
          <AppCheckbox v-model="fKom.taxable" label="Kena pajak" />
          <AppCheckbox v-model="fKom.bpjs_base" label="Basis BPJS" />
          <AppCheckbox v-model="fKom.active" label="Aktif" />
        </div>
      </form>
      <template #footer>
        <AppButton variant="ghost" @click="komOpen = false">Batal</AppButton>
        <AppButton
          type="submit"
          form="salary-component-form"
          :loading="mutasiLoading"
          :disabled="!fKom.name.trim()"
        >
          Simpan
        </AppButton>
      </template>
    </AppModal>

    <AppModal
      :open="tautOpen"
      title="Tetapkan komponen ke karyawan"
      description="Nominal akan dipakai pada perhitungan gaji berikutnya."
      @close="tautOpen = false"
    >
      <form id="assign-component-form" class="space-y-3" @submit.prevent="tautkan">
        <AppField label="Karyawan" required>
          <AppSelect v-model="idTaut" placeholder="Pilih karyawan" :options="opsiKaryawan" />
        </AppField>
        <AppField label="Komponen" required>
          <AppSelect v-model="idKomTaut" placeholder="Pilih komponen" :options="opsiKomponen" />
        </AppField>
        <AppField label="Nominal" required>
          <AppMoneyInput v-model="nominalTaut" />
        </AppField>
      </form>
      <template #footer>
        <AppButton variant="ghost" @click="tautOpen = false">Batal</AppButton>
        <AppButton
          type="submit"
          form="assign-component-form"
          :loading="mutasiLoading"
          :disabled="!idTaut || !idKomTaut"
        >
          Tetapkan
        </AppButton>
      </template>
    </AppModal>

    <AppModal
      :open="perOpen"
      title="Buat periode gaji"
      description="Periode menjadi wadah perhitungan gaji karyawan."
      @close="perOpen = false"
    >
      <form id="payroll-period-form" class="space-y-3" @submit.prevent="buatPeriode">
        <AppField label="Nama periode" required>
          <AppInput v-model="fPer.name" maxlength="255" placeholder="cth. Gaji Oktober 2026" />
        </AppField>
        <div class="grid gap-3 sm:grid-cols-2">
          <AppField label="Dari tanggal" required>
            <AppDateInput v-model="fPer.date_from" />
          </AppField>
          <AppField label="Sampai tanggal" required>
            <AppDateInput v-model="fPer.date_to" />
          </AppField>
        </div>
        <AppField label="Pembagi hari" hint="Umumnya 30 hari untuk potongan alpa.">
          <AppInput v-model="fPer.divisor" type="number" min="1" max="31" />
        </AppField>
      </form>
      <template #footer>
        <AppButton variant="ghost" @click="perOpen = false">Batal</AppButton>
        <AppButton
          type="submit"
          form="payroll-period-form"
          :loading="mutasiLoading"
          :disabled="!fPer.name.trim() || !fPer.date_from || !fPer.date_to"
        >
          Buat periode
        </AppButton>
      </template>
    </AppModal>

    <AppModal
      :open="sesuaikanOpen"
      :title="`Penyesuaian — ${detailPeriode?.name ?? ''}`"
      description="Nominal positif adalah tambahan, negatif adalah potongan."
      @close="sesuaikanOpen = false"
    >
      <form id="payroll-adjustment-form" class="space-y-3" @submit.prevent="tambahSesuai">
        <AppField label="Karyawan" required>
          <AppSelect v-model="idSesuai" placeholder="Pilih karyawan" :options="opsiKaryawan" />
        </AppField>
        <AppField label="Keterangan" required>
          <AppInput
            v-model="labelSesuai"
            maxlength="255"
            placeholder="cth. Lembur malam"
          />
        </AppField>
        <AppField label="Nominal" hint="Contoh: -50000 untuk potongan.">
          <AppInput v-model="nominalSesuai" type="number" />
        </AppField>
      </form>
      <template #footer>
        <AppButton variant="ghost" @click="sesuaikanOpen = false">Batal</AppButton>
        <AppButton
          type="submit"
          form="payroll-adjustment-form"
          :loading="mutasiLoading"
          :disabled="!idSesuai || !labelSesuai.trim()"
        >
          Simpan penyesuaian
        </AppButton>
      </template>
    </AppModal>

    <AppModal
      :open="rincianOpen"
      :title="`Rincian gaji — ${rincian?.name ?? ''}`"
      size="lg"
      @close="rincianOpen = false"
    >
      <AppTable caption="Rincian gaji karyawan" :empty="!rincian?.lines?.length" :colspan="2" dense>
        <template #head>
          <tr>
            <th>Komponen</th>
            <th class="text-right">Nominal</th>
          </tr>
        </template>
        <tr v-for="(line, index) in rincian?.lines ?? []" :key="line.id ?? index">
          <td>
            {{ line.label }}
            <span
              v-if="line.isManual ?? line.is_manual"
              class="ml-1 text-[10px] font-semibold uppercase tracking-wider text-secondary"
            >
              manual
            </span>
          </td>
          <td class="text-right font-semibold">{{ rupiah(line.amount) }}</td>
        </tr>
        <template #foot>
          <tr class="border-t border-outline">
            <td class="font-semibold">Total bersih</td>
            <td class="text-right font-bold">{{ rupiah(rincian?.net ?? 0) }}</td>
          </tr>
        </template>
      </AppTable>
    </AppModal>
  </div>
</template>
