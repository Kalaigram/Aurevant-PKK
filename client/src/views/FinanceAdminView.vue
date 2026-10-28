<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { api } from '../lib/api'
import { rupiah, tanggal } from '../lib/format'
import { useCompany } from '../composables/useCompany'
import { useAccessStore } from '../stores/access'
import { useAsync } from '../composables/useAsync'
import { useConfirm } from '../composables/useConfirm'
import AppBadge from '../components/ui/AppBadge.vue'
import AppBanner from '../components/ui/AppBanner.vue'
import AppButton from '../components/ui/AppButton.vue'
import AppCard from '../components/ui/AppCard.vue'
import AppCheckbox from '../components/ui/AppCheckbox.vue'
import AppDateInput from '../components/ui/AppDateInput.vue'
import AppField from '../components/ui/AppField.vue'
import AppInput from '../components/ui/AppInput.vue'
import AppPageHeader from '../components/ui/AppPageHeader.vue'
import AppSelect from '../components/ui/AppSelect.vue'
import AppTable from '../components/ui/AppTable.vue'
import AppTabs from '../components/ui/AppTabs.vue'
import AppTextarea from '../components/ui/AppTextarea.vue'

interface Cabang {
  id: number
  code: string
  name: string
}
interface Kategori {
  id: number
  name: string
}
interface Akun {
  id: number
  parentId: number | null
  code: string
  name: string
  type: string
  isActive: boolean
  description: string | null
}
interface Anggaran {
  id: number
  year: number
  month: number | null
  branchId: number | null
  branchName: string | null
  categoryId: number | null
  categoryName: string | null
  amount: string
  used: string
  remaining: string
  note: string | null
}
interface Berulang {
  id: number
  branchId: number
  branchName: string | null
  categoryId: number | null
  categoryName: string | null
  amount: string
  method: string
  description: string
  frequency: string
  nextRunAt: string
  lastRunAt: string | null
  isActive: boolean
}
interface Aset {
  id: number
  branchId: number | null
  branchName: string | null
  name: string
  code: string | null
  category: string | null
  acquisitionDate: string
  acquisitionCost: string
  salvageValue: string
  usefulLifeMonths: number
  accumulatedDepreciation: string
  monthlyDepreciation: string
  bookValue: string
  status: string
  note: string | null
  lastDepreciatedAt: string | null
}
interface Rekonsiliasi {
  id: number
  branchId: number | null
  branchName: string | null
  accountId: number
  accountCode: string | null
  accountName: string | null
  period: string
  statementBalance: string
  bookBalance: string
  difference: string
  isBalanced: boolean
  status: string
  note: string | null
  reconciledAt: string | null
}

const TIPE_AKUN = [
  { value: 'aset', label: 'Aset' },
  { value: 'liabilitas', label: 'Liabilitas' },
  { value: 'ekuitas', label: 'Ekuitas' },
  { value: 'pendapatan', label: 'Pendapatan' },
  { value: 'beban', label: 'Beban' },
]
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
const FREKUENSI = [
  { value: 'harian', label: 'Harian' },
  { value: 'mingguan', label: 'Mingguan' },
  { value: 'bulanan', label: 'Bulanan' },
]
const METODE = [
  { value: 'tunai', label: 'Tunai' },
  { value: 'transfer', label: 'Transfer' },
]
const STATUS_ASET = [
  { value: 'aktif', label: 'Aktif' },
  { value: 'dilepas', label: 'Dilepas' },
]

const hariIni = new Date().toISOString().slice(0, 10)
const { auth, companyId } = useCompany()
const access = useAccessStore()
const { confirm } = useConfirm()
const { loading: loadingData, error: errorData, run: runData } = useAsync()
const { loading: loadingAksi, error: errorAksi, run: runAksi } = useAsync()

const tab = ref('akun')
const okMsg = ref('')
const tahunAnggaran = ref(String(new Date().getFullYear()))
const akun = ref<Akun[]>([])
const anggaran = ref<Anggaran[]>([])
const berulang = ref<Berulang[]>([])
const aset = ref<Aset[]>([])
const rekonsiliasi = ref<Rekonsiliasi[]>([])
const cabang = ref<Cabang[]>([])
const kategori = ref<Kategori[]>([])

const bisaAkun = computed(
  () => Boolean(auth.user?.isSuperAdmin) || access.can('biaya.setujui'),
)
const bisaTutup = computed(
  () => Boolean(auth.user?.isSuperAdmin) || access.can('periode.tutup'),
)
const error = computed(() => errorData.value || errorAksi.value)

const opsiCabang = computed(() =>
  cabang.value.map((item) => ({ value: String(item.id), label: `${item.code} — ${item.name}` })),
)
const opsiKategori = computed(() =>
  kategori.value.map((item) => ({ value: String(item.id), label: item.name })),
)
const opsiAkun = computed(() =>
  akun.value.map((item) => ({ value: String(item.id), label: `${item.code} — ${item.name}` })),
)
const opsiTahun = computed(() =>
  [String(new Date().getFullYear() - 1), String(new Date().getFullYear()), String(new Date().getFullYear() + 1)].map(
    (year) => ({ value: year, label: year }),
  ),
)
const opsiBulan = BULAN.map((label, index) => ({ value: String(index + 1), label }))
const tabs = computed(() => [
  { key: 'akun', label: 'Bagan akun', count: akun.value.length },
  { key: 'anggaran', label: 'Anggaran', count: anggaran.value.length },
  { key: 'berulang', label: 'Biaya berulang', count: berulang.value.length },
  { key: 'aset', label: 'Aset tetap', count: aset.value.length },
  { key: 'rekonsiliasi', label: 'Rekonsiliasi', count: rekonsiliasi.value.length },
])

const editAkun = ref<number | null>(null)
const formAkun = ref(formAkunBaru())
const editAnggaran = ref<number | null>(null)
const formAnggaran = ref(formAnggaranBaru())
const editBerulang = ref<number | null>(null)
const formBerulang = ref(formBerulangBaru())
const editAset = ref<number | null>(null)
const formAset = ref(formAsetBaru())
const formRekon = ref(formRekonBaru())

function formAkunBaru() {
  return { code: '', name: '', type: 'aset', parent_id: '', description: '', is_active: true }
}
function formAnggaranBaru() {
  return {
    branch_id: '',
    year: Number(tahunAnggaran.value),
    month: '',
    expense_category_id: '',
    amount: 0,
    note: '',
  }
}
function formBerulangBaru() {
  return {
    branch_id: '',
    expense_category_id: '',
    amount: 0,
    method: 'tunai',
    description: '',
    frequency: 'bulanan',
    next_run_at: hariIni,
    is_active: true,
  }
}
function formAsetBaru() {
  return {
    branch_id: '',
    name: '',
    code: '',
    category: '',
    acquisition_date: hariIni,
    acquisition_cost: 0,
    salvage_value: 0,
    useful_life_months: 12,
    status: 'aktif',
    note: '',
  }
}
function formRekonBaru() {
  return {
    branch_id: '',
    account_id: '',
    period: hariIni.slice(0, 7),
    statement_balance: 0,
    note: '',
  }
}

function periodeAnggaran(row: Anggaran) {
  return row.month ? `${BULAN[row.month - 1]} ${row.year}` : `Tahunan ${row.year}`
}

function teksCabang(id: number | null, name: string | null) {
  return id === null ? 'Semua cabang' : (name ?? `Cabang #${id}`)
}

async function muatSemua() {
  await runData(async () => {
    const [branches, categories, accounts, budgets, recurring, assets, recs] =
      await Promise.all([
        api.getPage<Cabang>(`/companies/${companyId.value}/branches?per_page=100`, auth.token),
        api.getPage<Kategori>(
          `/companies/${companyId.value}/expense-categories?per_page=100`,
          auth.token,
        ),
        api.get<Akun[]>(`/companies/${companyId.value}/chart-of-accounts`, auth.token),
        api.get<Anggaran[]>(
          `/companies/${companyId.value}/budgets?year=${tahunAnggaran.value}`,
          auth.token,
        ),
        api.get<Berulang[]>(`/companies/${companyId.value}/recurring-expenses`, auth.token),
        api.get<Aset[]>(`/companies/${companyId.value}/fixed-assets`, auth.token),
        api.get<Rekonsiliasi[]>(
          `/companies/${companyId.value}/bank-reconciliations`,
          auth.token,
        ),
      ])
    cabang.value = branches.data
    kategori.value = categories.data
    akun.value = accounts
    anggaran.value = budgets
    berulang.value = recurring
    aset.value = assets
    rekonsiliasi.value = recs
  })
}

async function sukses(fn: () => Promise<unknown>, pesan: string) {
  await runAksi(fn)
  if (!errorAksi.value) {
    okMsg.value = pesan
    await muatSemua()
  }
}

function ubahAkun(item: Akun) {
  editAkun.value = item.id
  formAkun.value = {
    code: item.code,
    name: item.name,
    type: item.type,
    parent_id: item.parentId === null ? '' : String(item.parentId),
    description: item.description ?? '',
    is_active: item.isActive,
  }
}
function batalAkun() {
  editAkun.value = null
  formAkun.value = formAkunBaru()
}
async function simpanAkun() {
  const payload = {
    code: formAkun.value.code.trim(),
    name: formAkun.value.name.trim(),
    type: formAkun.value.type,
    parent_id: formAkun.value.parent_id ? Number(formAkun.value.parent_id) : null,
    description: formAkun.value.description.trim() || null,
    is_active: formAkun.value.is_active,
  }
  if (!payload.code || !payload.name) return
  const path = editAkun.value
    ? `/companies/${companyId.value}/chart-of-accounts/${editAkun.value}`
    : `/companies/${companyId.value}/chart-of-accounts`
  await sukses(
    () => (editAkun.value ? api.put(path, payload, auth.token) : api.post(path, payload, auth.token)),
    editAkun.value ? 'Akun diperbarui.' : 'Akun dibuat.',
  )
  if (!errorAksi.value) batalAkun()
}
async function hapusAkun(item: Akun) {
  if (
    !(await confirm({
      title: `Hapus akun ${item.code}?`,
      message: 'Akun yang punya jurnal atau sub-akun tidak bisa dihapus.',
      confirmLabel: 'Hapus',
      tone: 'danger',
    }))
  )
    return
  await sukses(
    () => api.del(`/companies/${companyId.value}/chart-of-accounts/${item.id}`, auth.token),
    'Akun dihapus.',
  )
}

function ubahAnggaran(item: Anggaran) {
  editAnggaran.value = item.id
  formAnggaran.value = {
    branch_id: item.branchId === null ? '' : String(item.branchId),
    year: item.year,
    month: item.month === null ? '' : String(item.month),
    expense_category_id: item.categoryId === null ? '' : String(item.categoryId),
    amount: Number(item.amount),
    note: item.note ?? '',
  }
}
function batalAnggaran() {
  editAnggaran.value = null
  formAnggaran.value = formAnggaranBaru()
}
async function simpanAnggaran() {
  if (!formAnggaran.value.amount) return
  const payload = {
    branch_id: formAnggaran.value.branch_id ? Number(formAnggaran.value.branch_id) : null,
    year: Number(formAnggaran.value.year),
    month: formAnggaran.value.month ? Number(formAnggaran.value.month) : null,
    expense_category_id: formAnggaran.value.expense_category_id
      ? Number(formAnggaran.value.expense_category_id)
      : null,
    amount: Number(formAnggaran.value.amount),
    note: formAnggaran.value.note.trim() || null,
  }
  const path = editAnggaran.value
    ? `/companies/${companyId.value}/budgets/${editAnggaran.value}`
    : `/companies/${companyId.value}/budgets`
  await sukses(
    () =>
      editAnggaran.value ? api.put(path, payload, auth.token) : api.post(path, payload, auth.token),
    editAnggaran.value ? 'Anggaran diperbarui.' : 'Anggaran dibuat.',
  )
  if (!errorAksi.value) batalAnggaran()
}
async function hapusAnggaran(item: Anggaran) {
  if (
    !(await confirm({
      title: 'Hapus anggaran?',
      message: `${periodeAnggaran(item)} · ${item.categoryName ?? 'semua kategori'}`,
      confirmLabel: 'Hapus',
      tone: 'danger',
    }))
  )
    return
  await sukses(
    () => api.del(`/companies/${companyId.value}/budgets/${item.id}`, auth.token),
    'Anggaran dihapus.',
  )
}

function ubahBerulang(item: Berulang) {
  editBerulang.value = item.id
  formBerulang.value = {
    branch_id: String(item.branchId),
    expense_category_id: item.categoryId === null ? '' : String(item.categoryId),
    amount: Number(item.amount),
    method: item.method,
    description: item.description,
    frequency: item.frequency,
    next_run_at: item.nextRunAt.slice(0, 10),
    is_active: item.isActive,
  }
}
function batalBerulang() {
  editBerulang.value = null
  formBerulang.value = formBerulangBaru()
}
async function simpanBerulang() {
  if (!formBerulang.value.branch_id || !formBerulang.value.description.trim()) return
  if (!formBerulang.value.amount) return
  const payload = {
    branch_id: Number(formBerulang.value.branch_id),
    expense_category_id: formBerulang.value.expense_category_id
      ? Number(formBerulang.value.expense_category_id)
      : null,
    amount: Number(formBerulang.value.amount),
    method: formBerulang.value.method,
    description: formBerulang.value.description.trim(),
    frequency: formBerulang.value.frequency,
    next_run_at: formBerulang.value.next_run_at,
    is_active: formBerulang.value.is_active,
  }
  const path = editBerulang.value
    ? `/companies/${companyId.value}/recurring-expenses/${editBerulang.value}`
    : `/companies/${companyId.value}/recurring-expenses`
  await sukses(
    () =>
      editBerulang.value ? api.put(path, payload, auth.token) : api.post(path, payload, auth.token),
    editBerulang.value ? 'Template diperbarui.' : 'Template biaya berulang dibuat.',
  )
  if (!errorAksi.value) batalBerulang()
}
async function hapusBerulang(item: Berulang) {
  if (
    !(await confirm({
      title: 'Hapus template biaya?',
      message: item.description,
      confirmLabel: 'Hapus',
      tone: 'danger',
    }))
  )
    return
  await sukses(
    () => api.del(`/companies/${companyId.value}/recurring-expenses/${item.id}`, auth.token),
    'Template dihapus.',
  )
}
async function jalankanBerulang() {
  if (
    !(await confirm({
      title: 'Jalankan biaya jatuh tempo?',
      message: 'Draft biaya akan dibuat untuk semua template yang jatuh tempo.',
      confirmLabel: 'Jalankan',
    }))
  )
    return
  const res = await runAksi(() =>
    api.post<{ created: number }>(
      `/companies/${companyId.value}/recurring-expenses/run-due`,
      {},
      auth.token,
    ),
  )
  if (!errorAksi.value) {
    okMsg.value = `${res?.created ?? 0} draft biaya dibuat.`
    await muatSemua()
  }
}

function ubahAset(item: Aset) {
  editAset.value = item.id
  formAset.value = {
    branch_id: item.branchId === null ? '' : String(item.branchId),
    name: item.name,
    code: item.code ?? '',
    category: item.category ?? '',
    acquisition_date: item.acquisitionDate.slice(0, 10),
    acquisition_cost: Number(item.acquisitionCost),
    salvage_value: Number(item.salvageValue),
    useful_life_months: item.usefulLifeMonths,
    status: item.status,
    note: item.note ?? '',
  }
}
function batalAset() {
  editAset.value = null
  formAset.value = formAsetBaru()
}
async function simpanAset() {
  if (!formAset.value.name.trim() || !formAset.value.acquisition_cost) return
  const payload = {
    branch_id: formAset.value.branch_id ? Number(formAset.value.branch_id) : null,
    name: formAset.value.name.trim(),
    code: formAset.value.code.trim() || null,
    category: formAset.value.category.trim() || null,
    acquisition_date: formAset.value.acquisition_date,
    acquisition_cost: Number(formAset.value.acquisition_cost),
    salvage_value: Number(formAset.value.salvage_value || 0),
    useful_life_months: Number(formAset.value.useful_life_months),
    status: formAset.value.status,
    note: formAset.value.note.trim() || null,
  }
  const path = editAset.value
    ? `/companies/${companyId.value}/fixed-assets/${editAset.value}`
    : `/companies/${companyId.value}/fixed-assets`
  await sukses(
    () => (editAset.value ? api.put(path, payload, auth.token) : api.post(path, payload, auth.token)),
    editAset.value ? 'Aset diperbarui.' : 'Aset tetap dibuat.',
  )
  if (!errorAksi.value) batalAset()
}
async function hapusAset(item: Aset) {
  if (
    !(await confirm({
      title: 'Hapus aset tetap?',
      message: item.name,
      confirmLabel: 'Hapus',
      tone: 'danger',
    }))
  )
    return
  await sukses(
    () => api.del(`/companies/${companyId.value}/fixed-assets/${item.id}`, auth.token),
    'Aset dihapus.',
  )
}
async function susutkan(item: Aset) {
  if (
    !(await confirm({
      title: `Catat penyusutan ${item.name}?`,
      message: `Beban penyusutan bulanan ${rupiah(item.monthlyDepreciation)}.`,
      confirmLabel: 'Catat 1 bulan',
    }))
  )
    return
  await sukses(
    () =>
      api.post(
        `/companies/${companyId.value}/fixed-assets/${item.id}/depreciate`,
        { months: 1 },
        auth.token,
      ),
    'Penyusutan dicatat.',
  )
}

async function simpanRekon() {
  if (!formRekon.value.account_id || !formRekon.value.period) return
  const payload = {
    branch_id: formRekon.value.branch_id ? Number(formRekon.value.branch_id) : null,
    account_id: Number(formRekon.value.account_id),
    period: formRekon.value.period,
    statement_balance: Number(formRekon.value.statement_balance),
    note: formRekon.value.note.trim() || null,
  }
  await sukses(
    () =>
      api.post(`/companies/${companyId.value}/bank-reconciliations`, payload, auth.token),
    'Rekonsiliasi dibuat.',
  )
  if (!errorAksi.value) formRekon.value = formRekonBaru()
}
async function selesaikanRekon(item: Rekonsiliasi) {
  if (
    !(await confirm({
      title: 'Selesaikan rekonsiliasi?',
      message: item.isBalanced
        ? 'Saldo statement dan buku sudah sama.'
        : `Masih ada selisih ${rupiah(item.difference)}. Pastikan sudah ditinjau.`,
      confirmLabel: 'Selesaikan',
    }))
  )
    return
  await sukses(
    () =>
      api.post(
        `/companies/${companyId.value}/bank-reconciliations/${item.id}/resolve`,
        {},
        auth.token,
      ),
    'Rekonsiliasi diselesaikan.',
  )
}

watch(tahunAnggaran, () => void muatSemua())

onMounted(async () => {
  await access.load(companyId.value, auth.token)
  await muatSemua()
})
</script>

<template>
  <div>
    <AppPageHeader
      eyebrow="Keuangan lanjutan"
      title="Bagan akun, anggaran & rekonsiliasi"
      subtitle="Kelola master keuangan, biaya berulang, aset tetap, dan pencocokan rekening bank."
      icon="account_balance"
    />

    <AppBanner tone="green" :message="okMsg" />
    <AppBanner tone="red" :message="error" />

    <AppTabs v-model="tab" :items="tabs" label="Bagian keuangan lanjutan" id-prefix="fin-admin-tab" />

    <div v-if="tab === 'akun'" class="mt-4 grid gap-4 lg:grid-cols-5">
      <div class="min-w-0 lg:col-span-3">
        <AppCard title="Bagan akun" subtitle="Kode akun unik per perusahaan; akun yang sudah berjurnal tidak bisa dihapus.">
          <AppTable
            caption="Bagan akun"
            :loading="loadingData"
            :empty="!akun.length"
            :colspan="5"
            empty-title="Belum ada akun"
            empty-message="Akun standar dibuat otomatis saat perusahaan dibuat."
          >
            <template #head>
              <tr>
                <th>Kode</th>
                <th>Nama</th>
                <th>Tipe</th>
                <th>Status</th>
                <th v-if="bisaAkun" class="text-right">Aksi</th>
              </tr>
            </template>
            <tr v-for="row in akun" :key="row.id">
              <td class="font-mono text-sm font-semibold text-ink">{{ row.code }}</td>
              <td>
                <p class="font-medium text-ink">{{ row.name }}</p>
                <p v-if="row.description" class="max-w-56 truncate text-xs text-secondary">
                  {{ row.description }}
                </p>
              </td>
              <td class="text-sm text-secondary">{{ row.type }}</td>
              <td>
                <AppBadge :tone="row.isActive ? 'green' : 'gray'">
                  {{ row.isActive ? 'Aktif' : 'Nonaktif' }}
                </AppBadge>
              </td>
              <td v-if="bisaAkun" class="text-right">
                <div class="flex justify-end gap-1">
                  <AppButton size="sm" variant="ghost" @click="ubahAkun(row)">Ubah</AppButton>
                  <AppButton size="sm" variant="ghost" @click="hapusAkun(row)">Hapus</AppButton>
                </div>
              </td>
            </tr>
          </AppTable>
        </AppCard>
      </div>

      <div v-if="bisaAkun" class="min-w-0 lg:col-span-2">
        <AppCard :title="editAkun ? 'Ubah akun' : 'Tambah akun'">
          <form class="space-y-3" @submit.prevent="simpanAkun">
            <div class="grid gap-3 sm:grid-cols-2">
              <AppField label="Kode" required>
                <AppInput v-model="formAkun.code" maxlength="20" />
              </AppField>
              <AppField label="Tipe" required>
                <AppSelect v-model="formAkun.type" :options="TIPE_AKUN" />
              </AppField>
            </div>
            <AppField label="Nama akun" required>
              <AppInput v-model="formAkun.name" maxlength="255" />
            </AppField>
            <AppField label="Akun induk" optional>
              <AppSelect v-model="formAkun.parent_id" placeholder="Tanpa induk" :options="opsiAkun" />
            </AppField>
            <AppField label="Keterangan" optional>
              <AppTextarea v-model="formAkun.description" :rows="2" :maxlength="500" />
            </AppField>
            <AppCheckbox v-model="formAkun.is_active" label="Akun aktif" />
            <div class="flex gap-2">
              <AppButton type="submit" :loading="loadingAksi" block>
                {{ editAkun ? 'Simpan perubahan' : 'Tambah akun' }}
              </AppButton>
              <AppButton v-if="editAkun" type="button" variant="ghost" @click="batalAkun">
                Batal
              </AppButton>
            </div>
          </form>
        </AppCard>
      </div>
    </div>

    <div v-else-if="tab === 'anggaran'" class="mt-4 grid gap-4 lg:grid-cols-5">
      <div class="min-w-0 lg:col-span-3">
        <AppCard title="Anggaran biaya">
          <div class="mb-3 flex justify-end">
            <div class="w-28">
              <AppSelect v-model="tahunAnggaran" :options="opsiTahun" aria-label="Tahun anggaran" />
            </div>
          </div>
          <AppTable
            caption="Anggaran biaya"
            :loading="loadingData"
            :empty="!anggaran.length"
            :colspan="bisaAkun ? 6 : 5"
            empty-title="Belum ada anggaran"
            empty-message="Tentukan batas biaya per periode, cabang, dan kategori."
          >
            <template #head>
              <tr>
                <th>Periode</th>
                <th>Cabang</th>
                <th>Kategori</th>
                <th>Anggaran</th>
                <th>Terpakai / sisa</th>
                <th v-if="bisaAkun" class="text-right">Aksi</th>
              </tr>
            </template>
            <tr v-for="row in anggaran" :key="row.id">
              <td class="whitespace-nowrap text-sm">{{ periodeAnggaran(row) }}</td>
              <td class="text-sm">{{ teksCabang(row.branchId, row.branchName) }}</td>
              <td class="text-sm">{{ row.categoryName ?? 'Semua kategori' }}</td>
              <td class="whitespace-nowrap text-sm tabular-nums">{{ rupiah(row.amount) }}</td>
              <td class="whitespace-nowrap text-sm tabular-nums">
                <span class="text-secondary">{{ rupiah(row.used) }}</span>
                <span class="text-secondary"> / </span>
                <span class="font-semibold text-ink">{{ rupiah(row.remaining) }}</span>
              </td>
              <td v-if="bisaAkun" class="text-right">
                <div class="flex justify-end gap-1">
                  <AppButton size="sm" variant="ghost" @click="ubahAnggaran(row)">Ubah</AppButton>
                  <AppButton size="sm" variant="ghost" @click="hapusAnggaran(row)">Hapus</AppButton>
                </div>
              </td>
            </tr>
          </AppTable>
        </AppCard>
      </div>

      <div v-if="bisaAkun" class="min-w-0 lg:col-span-2">
        <AppCard :title="editAnggaran ? 'Ubah anggaran' : 'Tambah anggaran'">
          <form class="space-y-3" @submit.prevent="simpanAnggaran">
            <div class="grid gap-3 sm:grid-cols-2">
              <AppField label="Tahun" required>
                <AppInput v-model.number="formAnggaran.year" type="number" min="2000" max="2100" />
              </AppField>
              <AppField label="Bulan" optional>
                <AppSelect v-model="formAnggaran.month" placeholder="Tahunan" :options="opsiBulan" />
              </AppField>
            </div>
            <AppField label="Cabang" optional>
              <AppSelect v-model="formAnggaran.branch_id" placeholder="Semua cabang" :options="opsiCabang" />
            </AppField>
            <AppField label="Kategori" optional>
              <AppSelect
                v-model="formAnggaran.expense_category_id"
                placeholder="Semua kategori"
                :options="opsiKategori"
              />
            </AppField>
            <AppField label="Batas anggaran (Rp)" required>
              <AppInput v-model.number="formAnggaran.amount" type="number" min="0" />
            </AppField>
            <AppField label="Catatan" optional>
              <AppTextarea v-model="formAnggaran.note" :rows="2" :maxlength="500" />
            </AppField>
            <div class="flex gap-2">
              <AppButton type="submit" :loading="loadingAksi" block>
                {{ editAnggaran ? 'Simpan perubahan' : 'Tambah anggaran' }}
              </AppButton>
              <AppButton v-if="editAnggaran" type="button" variant="ghost" @click="batalAnggaran">
                Batal
              </AppButton>
            </div>
          </form>
        </AppCard>
      </div>
    </div>

    <div v-else-if="tab === 'berulang'" class="mt-4 grid gap-4 lg:grid-cols-5">
      <div class="min-w-0 lg:col-span-3">
        <AppCard title="Biaya berulang">
          <div class="mb-3 flex justify-end">
            <AppButton variant="secondary" :loading="loadingAksi" @click="jalankanBerulang">
              Jalankan jatuh tempo
            </AppButton>
          </div>
          <AppTable
            caption="Template biaya berulang"
            :loading="loadingData"
            :empty="!berulang.length"
            :colspan="bisaAkun ? 6 : 5"
            empty-title="Belum ada template"
            empty-message="Template membuat draft biaya otomatis sesuai jadwal."
          >
            <template #head>
              <tr>
                <th>Keterangan</th>
                <th>Cabang</th>
                <th>Nominal</th>
                <th>Jadwal</th>
                <th>Status</th>
                <th v-if="bisaAkun" class="text-right">Aksi</th>
              </tr>
            </template>
            <tr v-for="row in berulang" :key="row.id">
              <td>
                <p class="font-medium text-ink">{{ row.description }}</p>
                <p class="text-xs text-secondary">{{ row.categoryName ?? 'Tanpa kategori' }}</p>
              </td>
              <td class="text-sm">{{ row.branchName ?? `#${row.branchId}` }}</td>
              <td class="whitespace-nowrap text-sm tabular-nums">{{ rupiah(row.amount) }}</td>
              <td class="text-xs text-secondary">
                {{ row.frequency }} · berikutnya {{ tanggal(row.nextRunAt) }}
              </td>
              <td>
                <AppBadge :tone="row.isActive ? 'green' : 'gray'">
                  {{ row.isActive ? 'Aktif' : 'Nonaktif' }}
                </AppBadge>
              </td>
              <td v-if="bisaAkun" class="text-right">
                <div class="flex justify-end gap-1">
                  <AppButton size="sm" variant="ghost" @click="ubahBerulang(row)">Ubah</AppButton>
                  <AppButton size="sm" variant="ghost" @click="hapusBerulang(row)">Hapus</AppButton>
                </div>
              </td>
            </tr>
          </AppTable>
        </AppCard>
      </div>

      <div v-if="bisaAkun" class="min-w-0 lg:col-span-2">
        <AppCard :title="editBerulang ? 'Ubah template' : 'Tambah template'">
          <form class="space-y-3" @submit.prevent="simpanBerulang">
            <AppField label="Cabang" required>
              <AppSelect v-model="formBerulang.branch_id" placeholder="Pilih cabang" :options="opsiCabang" />
            </AppField>
            <AppField label="Kategori" optional>
              <AppSelect
                v-model="formBerulang.expense_category_id"
                placeholder="Tanpa kategori"
                :options="opsiKategori"
              />
            </AppField>
            <AppField label="Keterangan" required>
              <AppInput v-model="formBerulang.description" maxlength="500" />
            </AppField>
            <div class="grid gap-3 sm:grid-cols-2">
              <AppField label="Nominal (Rp)" required>
                <AppInput v-model.number="formBerulang.amount" type="number" min="1" />
              </AppField>
              <AppField label="Metode">
                <AppSelect v-model="formBerulang.method" :options="METODE" />
              </AppField>
            </div>
            <div class="grid gap-3 sm:grid-cols-2">
              <AppField label="Frekuensi" required>
                <AppSelect v-model="formBerulang.frequency" :options="FREKUENSI" />
              </AppField>
              <AppField label="Jatuh tempo berikutnya" required>
                <AppDateInput v-model="formBerulang.next_run_at" />
              </AppField>
            </div>
            <AppCheckbox v-model="formBerulang.is_active" label="Template aktif" />
            <div class="flex gap-2">
              <AppButton type="submit" :loading="loadingAksi" block>
                {{ editBerulang ? 'Simpan perubahan' : 'Tambah template' }}
              </AppButton>
              <AppButton v-if="editBerulang" type="button" variant="ghost" @click="batalBerulang">
                Batal
              </AppButton>
            </div>
          </form>
        </AppCard>
      </div>
    </div>

    <div v-else-if="tab === 'aset'" class="mt-4 grid gap-4 lg:grid-cols-5">
      <div class="min-w-0 lg:col-span-3">
        <AppCard title="Aset tetap">
          <AppTable
            caption="Aset tetap"
            :loading="loadingData"
            :empty="!aset.length"
            :colspan="bisaAkun ? 6 : 5"
            empty-title="Belum ada aset"
            empty-message="Catat aset dan susutkan dengan metode garis lurus."
          >
            <template #head>
              <tr>
                <th>Aset</th>
                <th>Perolehan</th>
                <th>Biaya / susut</th>
                <th>Nilai buku</th>
                <th>Status</th>
                <th v-if="bisaAkun" class="text-right">Aksi</th>
              </tr>
            </template>
            <tr v-for="row in aset" :key="row.id">
              <td>
                <p class="font-medium text-ink">{{ row.name }}</p>
                <p class="text-xs text-secondary">{{ row.code ?? row.category ?? '—' }}</p>
              </td>
              <td class="whitespace-nowrap text-sm">{{ tanggal(row.acquisitionDate) }}</td>
              <td class="whitespace-nowrap text-sm tabular-nums">
                {{ rupiah(row.acquisitionCost) }}
                <span class="block text-xs text-secondary">susut {{ rupiah(row.monthlyDepreciation) }}/bln</span>
              </td>
              <td class="whitespace-nowrap text-sm font-semibold tabular-nums">
                {{ rupiah(row.bookValue) }}
              </td>
              <td>
                <AppBadge :tone="row.status === 'aktif' ? 'green' : 'gray'">{{ row.status }}</AppBadge>
              </td>
              <td v-if="bisaAkun" class="text-right">
                <div class="flex flex-wrap justify-end gap-1">
                  <AppButton size="sm" variant="ghost" @click="susutkan(row)">Susutkan</AppButton>
                  <AppButton size="sm" variant="ghost" @click="ubahAset(row)">Ubah</AppButton>
                  <AppButton size="sm" variant="ghost" @click="hapusAset(row)">Hapus</AppButton>
                </div>
              </td>
            </tr>
          </AppTable>
        </AppCard>
      </div>

      <div v-if="bisaAkun" class="min-w-0 lg:col-span-2">
        <AppCard :title="editAset ? 'Ubah aset' : 'Tambah aset'">
          <form class="space-y-3" @submit.prevent="simpanAset">
            <AppField label="Nama aset" required>
              <AppInput v-model="formAset.name" maxlength="255" />
            </AppField>
            <div class="grid gap-3 sm:grid-cols-2">
              <AppField label="Kode" optional>
                <AppInput v-model="formAset.code" maxlength="50" />
              </AppField>
              <AppField label="Kategori" optional>
                <AppInput v-model="formAset.category" maxlength="100" />
              </AppField>
            </div>
            <AppField label="Cabang" optional>
              <AppSelect v-model="formAset.branch_id" placeholder="Semua cabang" :options="opsiCabang" />
            </AppField>
            <AppField label="Tanggal perolehan" required>
              <AppDateInput v-model="formAset.acquisition_date" />
            </AppField>
            <div class="grid gap-3 sm:grid-cols-2">
              <AppField label="Harga perolehan (Rp)" required>
                <AppInput v-model.number="formAset.acquisition_cost" type="number" min="1" />
              </AppField>
              <AppField label="Nilai residu (Rp)">
                <AppInput v-model.number="formAset.salvage_value" type="number" min="0" />
              </AppField>
            </div>
            <div class="grid gap-3 sm:grid-cols-2">
              <AppField label="Umur manfaat (bulan)" required>
                <AppInput v-model.number="formAset.useful_life_months" type="number" min="1" max="600" />
              </AppField>
              <AppField label="Status">
                <AppSelect v-model="formAset.status" :options="STATUS_ASET" />
              </AppField>
            </div>
            <AppField label="Catatan" optional>
              <AppTextarea v-model="formAset.note" :rows="2" :maxlength="500" />
            </AppField>
            <div class="flex gap-2">
              <AppButton type="submit" :loading="loadingAksi" block>
                {{ editAset ? 'Simpan perubahan' : 'Tambah aset' }}
              </AppButton>
              <AppButton v-if="editAset" type="button" variant="ghost" @click="batalAset">
                Batal
              </AppButton>
            </div>
          </form>
        </AppCard>
      </div>
    </div>

    <div v-else class="mt-4 grid gap-4 lg:grid-cols-5">
      <div class="min-w-0 lg:col-span-3">
        <AppCard title="Rekonsiliasi bank" subtitle="Saldo buku dihitung dari jurnal pada periode terpilih.">
          <AppTable
            caption="Rekonsiliasi bank"
            :loading="loadingData"
            :empty="!rekonsiliasi.length"
            :colspan="bisaTutup ? 6 : 5"
            empty-title="Belum ada rekonsiliasi"
            empty-message="Buat rekonsiliasi untuk mencocokkan saldo statement dan buku."
          >
            <template #head>
              <tr>
                <th>Akun</th>
                <th>Periode</th>
                <th>Statement</th>
                <th>Buku</th>
                <th>Selisih</th>
                <th v-if="bisaTutup" class="text-right">Aksi</th>
              </tr>
            </template>
            <tr v-for="row in rekonsiliasi" :key="row.id">
              <td>
                <p class="font-medium text-ink">{{ row.accountName ?? `Akun #${row.accountId}` }}</p>
                <p class="text-xs text-secondary">{{ row.accountCode }}</p>
              </td>
              <td class="whitespace-nowrap text-sm">{{ row.period }}</td>
              <td class="whitespace-nowrap text-sm tabular-nums">{{ rupiah(row.statementBalance) }}</td>
              <td class="whitespace-nowrap text-sm tabular-nums">{{ rupiah(row.bookBalance) }}</td>
              <td class="whitespace-nowrap text-sm tabular-nums">
                <span :class="row.isBalanced ? 'text-primary' : 'text-danger'">
                  {{ rupiah(row.difference) }}
                </span>
                <span class="block text-xs text-secondary">
                  {{ row.isBalanced ? 'Cocok' : row.status === 'selesai' ? 'Selesai' : 'Draft' }}
                </span>
              </td>
              <td v-if="bisaTutup" class="text-right">
                <AppButton
                  v-if="row.status !== 'selesai'"
                  size="sm"
                  variant="ghost"
                  :loading="loadingAksi"
                  @click="selesaikanRekon(row)"
                >
                  Selesaikan
                </AppButton>
                <span v-else class="text-xs text-secondary">Selesai</span>
              </td>
            </tr>
          </AppTable>
        </AppCard>
      </div>

      <div v-if="bisaTutup" class="min-w-0 lg:col-span-2">
        <AppCard title="Buat rekonsiliasi">
          <form class="space-y-3" @submit.prevent="simpanRekon">
            <AppField label="Akun" required>
              <AppSelect v-model="formRekon.account_id" placeholder="Pilih akun" :options="opsiAkun" />
            </AppField>
            <AppField label="Cabang" optional>
              <AppSelect v-model="formRekon.branch_id" placeholder="Semua cabang" :options="opsiCabang" />
            </AppField>
            <div class="grid gap-3 sm:grid-cols-2">
              <AppField label="Periode" required hint="Format YYYY-MM">
                <AppInput v-model="formRekon.period" placeholder="2026-10" maxlength="7" />
              </AppField>
              <AppField label="Saldo statement (Rp)" required>
                <AppInput v-model.number="formRekon.statement_balance" type="number" />
              </AppField>
            </div>
            <AppField label="Catatan" optional>
              <AppTextarea v-model="formRekon.note" :rows="2" :maxlength="500" />
            </AppField>
            <AppButton
              type="submit"
              :loading="loadingAksi"
              :disabled="!formRekon.account_id || !formRekon.period"
              block
            >
              Simpan rekonsiliasi
            </AppButton>
          </form>
        </AppCard>
      </div>
    </div>
  </div>
</template>
