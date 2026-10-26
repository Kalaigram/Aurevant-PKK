<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '../lib/api'
import { useCompany } from '../composables/useCompany'
import { useAccessStore } from '../stores/access'
import { useAsync } from '../composables/useAsync'
import { usePagedList } from '../composables/usePagedList'
import { useConfirm } from '../composables/useConfirm'
import { useToast } from '../composables/useToast'
import AppBadge from '../components/ui/AppBadge.vue'
import AppBanner from '../components/ui/AppBanner.vue'
import AppButton from '../components/ui/AppButton.vue'
import AppField from '../components/ui/AppField.vue'
import AppInput from '../components/ui/AppInput.vue'
import AppModal from '../components/ui/AppModal.vue'
import AppPageHeader from '../components/ui/AppPageHeader.vue'
import AppPager from '../components/ui/AppPager.vue'
import AppSearch from '../components/ui/AppSearch.vue'
import AppSelect from '../components/ui/AppSelect.vue'
import AppStat from '../components/ui/AppStat.vue'
import AppTable from '../components/ui/AppTable.vue'
import AppTextarea from '../components/ui/AppTextarea.vue'

interface Cabang {
  id: number
  code: string
  name: string
  type: string
  timezone: string
  address: string | null
  phone: string | null
  status: string
}

const TIPE = [
  { value: 'pusat', label: 'Kantor Pusat', tone: 'blue' },
  { value: 'cabang', label: 'Cabang Ritel', tone: 'green' },
  { value: 'gudang', label: 'Gudang Logistik', tone: 'amber' },
] as const
const ZONA: Record<string, string> = {
  'Asia/Jakarta': 'WIB',
  'Asia/Makassar': 'WITA',
  'Asia/Jayapura': 'WIT',
}
const WARNA_TILE = [
  'bg-primary-container text-on-primary',
  'bg-primary-soft text-primary-ink',
  'bg-info-bg text-info-ink',
  'bg-warn-bg text-warn-ink',
  'bg-ok-bg text-ok-ink',
  'bg-surface-low text-secondary',
]

const router = useRouter()
const { auth, companyId } = useCompany()
const access = useAccessStore()
const toast = useToast()
const { confirm } = useConfirm()
const { loading: simpanLoading, error: simpanError, run: runMutasi } = useAsync()

const cari = ref('')
const filterTipe = ref('')
const filterStatus = ref('')
const statistik = ref({ aktif: 0, nonaktif: 0 })
const formOpen = ref(false)
const sunting = ref<Cabang | null>(null)
const form = ref(formKosong())

const {
  rows: list,
  meta,
  loading,
  error: errorList,
  load,
  setPage,
  setPerPage,
} = usePagedList<Cabang>((page, perPage) => {
  const params = new URLSearchParams({ per_page: String(perPage), page: String(page) })
  if (cari.value.trim()) params.set('q', cari.value.trim())
  if (filterTipe.value) params.set('type', filterTipe.value)
  if (filterStatus.value) params.set('status', filterStatus.value)
  return api.getPage<Cabang>(`/companies/${companyId.value}/branches?${params}`, auth.token)
})

const error = computed(() => errorList.value || simpanError.value)
const bolehKelola = computed(() => access.can('cabang.kelola'))
const opsiTipe = TIPE.map((tipe) => ({ value: tipe.value, label: tipe.label }))
const opsiStatus = [
  { value: 'aktif', label: 'Aktif' },
  { value: 'nonaktif', label: 'Nonaktif' },
]
const totalUnit = computed(() => statistik.value.aktif + statistik.value.nonaktif)

const labelTipe = (type: string) => TIPE.find((item) => item.value === type)?.label ?? type
const labelStatus = (status: string) => (status === 'aktif' ? 'Aktif' : 'Nonaktif')
const toneTipe = (type: string) =>
  (TIPE.find((item) => item.value === type)?.tone ?? 'gray') as
    'green' | 'red' | 'amber' | 'blue' | 'gray'

function formKosong() {
  return {
    code: '',
    name: '',
    type: 'cabang',
    timezone: 'Asia/Jakarta',
    address: '',
    phone: '',
    status: 'aktif',
  }
}

async function muat() {
  await load(1)
}

async function muatStatistik() {
  const base = `/companies/${companyId.value}/branches?per_page=1`
  const [aktif, nonaktif] = await Promise.all([
    api.getPage<Cabang>(`${base}&status=aktif`, auth.token).catch(() => null),
    api.getPage<Cabang>(`${base}&status=nonaktif`, auth.token).catch(() => null),
  ])
  statistik.value = {
    aktif: aktif?.meta?.total ?? 0,
    nonaktif: nonaktif?.meta?.total ?? 0,
  }
}

function bukaTambah() {
  sunting.value = null
  form.value = formKosong()
  formOpen.value = true
}

function bukaUbah(cabang: Cabang) {
  sunting.value = cabang
  form.value = {
    code: cabang.code,
    name: cabang.name,
    type: cabang.type,
    timezone: cabang.timezone,
    address: cabang.address ?? '',
    phone: cabang.phone ?? '',
    status: cabang.status,
  }
  formOpen.value = true
}

function payload() {
  return {
    code: form.value.code.trim().toUpperCase(),
    name: form.value.name.trim(),
    type: form.value.type,
    timezone: form.value.timezone,
    address: form.value.address.trim() || undefined,
    phone: form.value.phone.trim() || undefined,
    status: form.value.status,
  }
}

async function simpan() {
  const base = `/companies/${companyId.value}/branches`
  const res = sunting.value
    ? await runMutasi(() => api.put(base + `/${sunting.value!.id}`, payload(), auth.token))
    : await runMutasi(() => api.post(base, payload(), auth.token))

  if (res) {
    formOpen.value = false
    toast.success(sunting.value ? 'Cabang diperbarui.' : 'Cabang baru didaftarkan.')
    await Promise.all([load(meta.value?.current_page ?? 1), muatStatistik()])
  }
}

async function hapus(cabang: Cabang) {
  const yakin = await confirm({
    title: `Hapus cabang ${cabang.name}?`,
    message: 'Cabang yang masih punya transaksi atau shift tidak bisa dihapus.',
    confirmLabel: 'Hapus cabang',
    tone: 'danger',
  })
  if (!yakin) return

  await runMutasi(() => api.del(`/companies/${companyId.value}/branches/${cabang.id}`, auth.token))
  if (!simpanError.value) {
    toast.success('Cabang dihapus.')
    await Promise.all([load(), muatStatistik()])
  }
}

function pilih(cabang: Cabang) {
  auth.pickBranch(cabang.id, cabang.code)
  router.push(`/c/${companyId.value}/dashboard`)
}

onMounted(async () => {
  await access.load(companyId.value, auth.token)
  await Promise.all([muat(), muatStatistik()])
})
</script>

<template>
  <div>
    <AppPageHeader
      eyebrow="Struktur organisasi & cabang"
      title="Daftar cabang perusahaan"
      subtitle="Kelola unit operasional, tipe, zona waktu, dan statusnya."
      icon="store"
    >
      <template #actions>
        <AppButton v-if="bolehKelola" @click="bukaTambah">Tambah cabang</AppButton>
      </template>
    </AppPageHeader>

    <AppBanner tone="red" :message="error" />

    <div class="mb-5 grid grid-cols-2 gap-3 lg:grid-cols-4">
      <AppStat label="Total unit" :value="totalUnit" icon="domain" hint="Semua status" />
      <AppStat
        label="Status aktif"
        :value="statistik.aktif"
        icon="check_circle"
        tone="positive"
        hint="Siap dipakai bertransaksi"
      />
      <AppStat
        label="Nonaktif"
        :value="statistik.nonaktif"
        icon="pause_circle"
        hint="Tidak muncul di pilihan operasional"
      />
      <AppStat
        label="Cabang dipilih"
        :value="auth.branchCode ?? '—'"
        icon="pin_drop"
        hint="Konteks sesi saat ini"
      />
    </div>

    <div class="mb-3 flex flex-wrap items-end gap-2">
      <AppSearch
        v-model="cari"
        class="min-w-56 flex-1"
        placeholder="Cari nama, kode, atau alamat..."
        @search="muat"
      />
      <AppSelect
        v-model="filterTipe"
        class="w-44"
        aria-label="Filter tipe cabang"
        placeholder="Semua tipe"
        :options="opsiTipe"
        @update:model-value="muat"
      />
      <AppSelect
        v-model="filterStatus"
        class="w-36"
        aria-label="Filter status cabang"
        placeholder="Semua status"
        :options="opsiStatus"
        @update:model-value="muat"
      />
    </div>

    <AppTable
      :loading="loading"
      :empty="!list.length"
      :colspan="bolehKelola ? 6 : 5"
      empty-title="Belum ada cabang"
      empty-message="Ubah pencarian atau daftarkan cabang baru."
    >
      <template #head>
        <tr>
          <th>Cabang</th>
          <th>Tipe</th>
          <th>Zona waktu</th>
          <th>Kontak</th>
          <th>Status</th>
          <th v-if="bolehKelola" class="text-right">Aksi</th>
        </tr>
      </template>

      <tr v-for="(cabang, index) in list" :key="cabang.id">
        <td>
          <div class="flex items-center gap-3">
            <span
              class="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg font-display text-xs font-bold"
              :class="WARNA_TILE[index % WARNA_TILE.length]"
            >
              {{ cabang.code.slice(0, 3).toUpperCase() }}
            </span>
            <div class="min-w-0">
              <p class="truncate font-semibold text-ink">{{ cabang.name }}</p>
              <p class="font-mono text-xs text-secondary">{{ cabang.code }}</p>
            </div>
          </div>
        </td>
        <td>
          <AppBadge :tone="toneTipe(cabang.type)">{{ labelTipe(cabang.type) }}</AppBadge>
        </td>
        <td>
          <span class="text-sm">{{ ZONA[cabang.timezone] ?? cabang.timezone }}</span>
          <p class="text-xs text-secondary">{{ cabang.timezone }}</p>
        </td>
        <td>
          <p class="max-w-56 truncate text-sm">{{ cabang.address || '—' }}</p>
          <p v-if="cabang.phone" class="font-mono text-xs text-secondary">{{ cabang.phone }}</p>
        </td>
        <td>
          <AppBadge :tone="cabang.status === 'aktif' ? 'green' : 'gray'" :dot="cabang.status === 'aktif'">
            {{ labelStatus(cabang.status) }}
          </AppBadge>
        </td>
        <td v-if="bolehKelola" class="text-right">
          <div class="flex justify-end gap-1">
            <AppButton
              size="sm"
              variant="subtle"
              :disabled="cabang.status !== 'aktif'"
              :aria-label="`Pilih cabang ${cabang.name}`"
              @click="pilih(cabang)"
            >
              Pilih
            </AppButton>
            <AppButton
              size="sm"
              variant="ghost"
              :aria-label="`Ubah cabang ${cabang.name}`"
              @click="bukaUbah(cabang)"
            >
              Ubah
            </AppButton>
            <AppButton
              size="sm"
              variant="ghost"
              class="text-danger"
              :aria-label="`Hapus cabang ${cabang.name}`"
              @click="hapus(cabang)"
            >
              Hapus
            </AppButton>
          </div>
        </td>
      </tr>

      <template #empty-action>
        <AppButton v-if="bolehKelola" @click="bukaTambah">Tambah cabang</AppButton>
      </template>
    </AppTable>

    <AppPager :meta="meta" @page="setPage" @update:per-page="setPerPage" />

    <AppModal
      :open="formOpen"
      :title="sunting ? 'Ubah cabang' : 'Tambah cabang baru'"
      description="Konfigurasi unit operasional perusahaan."
      @close="formOpen = false"
    >
      <AppBanner tone="red" :message="simpanError" />
      <form id="branch-form" class="space-y-3" @submit.prevent="simpan">
        <div class="grid gap-3 sm:grid-cols-2">
          <AppField label="Kode" required>
            <AppInput
              v-model="form.code"
              maxlength="20"
              class="font-mono uppercase"
              placeholder="cth. PST"
              data-autofocus
            />
          </AppField>
          <AppField label="Status">
            <AppSelect v-model="form.status" :options="opsiStatus" />
          </AppField>
        </div>
        <AppField label="Nama cabang" required>
          <AppInput
            v-model="form.name"
            maxlength="255"
            placeholder="cth. Pusat Ciledug"
          />
        </AppField>
        <div class="grid gap-3 sm:grid-cols-2">
          <AppField label="Tipe">
            <AppSelect v-model="form.type" :options="opsiTipe" />
          </AppField>
          <AppField label="Zona waktu">
            <AppSelect
              v-model="form.timezone"
              :options="[
                { value: 'Asia/Jakarta', label: 'Asia/Jakarta (WIB)' },
                { value: 'Asia/Makassar', label: 'Asia/Makassar (WITA)' },
                { value: 'Asia/Jayapura', label: 'Asia/Jayapura (WIT)' },
              ]"
            />
          </AppField>
        </div>
        <AppField label="Alamat" optional>
          <AppTextarea v-model="form.address" :rows="2" :maxlength="500" />
        </AppField>
        <AppField label="Telepon" optional>
          <AppInput
            v-model="form.phone"
            maxlength="30"
            class="font-mono"
            placeholder="08xx..."
          />
        </AppField>
      </form>

      <template #footer>
        <AppButton variant="ghost" @click="formOpen = false">Batal</AppButton>
        <AppButton
          type="submit"
          form="branch-form"
          :loading="simpanLoading"
          :disabled="!form.code.trim() || !form.name.trim()"
        >
          Simpan cabang
        </AppButton>
      </template>
    </AppModal>
  </div>
</template>
