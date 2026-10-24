<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { api } from '../lib/api'
import { rupiah, tanggal } from '../lib/format'
import { useCompany } from '../composables/useCompany'
import { useAccessStore } from '../stores/access'
import { useAsync } from '../composables/useAsync'
import { usePagedList } from '../composables/usePagedList'
import { useConfirm } from '../composables/useConfirm'
import AppCard from '../components/ui/AppCard.vue'
import AppBadge from '../components/ui/AppBadge.vue'
import AppBanner from '../components/ui/AppBanner.vue'
import AppButton from '../components/ui/AppButton.vue'
import AppDateInput from '../components/ui/AppDateInput.vue'
import AppEmpty from '../components/ui/AppEmpty.vue'
import AppField from '../components/ui/AppField.vue'
import AppInput from '../components/ui/AppInput.vue'
import AppModal from '../components/ui/AppModal.vue'
import AppMoneyInput from '../components/ui/AppMoneyInput.vue'
import AppPageHeader from '../components/ui/AppPageHeader.vue'
import AppPager from '../components/ui/AppPager.vue'
import AppSelect from '../components/ui/AppSelect.vue'
import AppSkeleton from '../components/ui/AppSkeleton.vue'
import AppTabs from '../components/ui/AppTabs.vue'

interface Kategori {
  id: number
  name: string
}
interface Cabang {
  id: number
  code: string
  name: string
}
interface Biaya {
  id: number
  branchId?: number
  categoryId?: number
  categoryName?: string
  amount: string
  method?: string
  date: string
  description?: string
  status: string
}

const { auth, companyId } = useCompany()
const access = useAccessStore()
const { confirm } = useConfirm()
const tab = ref('biaya')
const filterStatus = ref('')
const okMsg = ref('')
const {
  rows: list,
  meta,
  loading: loadingList,
  error: errorList,
  load: muatBiaya,
  setPage,
  setPerPage,
} = usePagedList<Biaya>(
  (page, perPage) =>
    api.getPage<Biaya>(
      `/companies/${companyId.value}/expenses?per_page=${perPage}&page=${page}${filterStatus.value ? `&status=${filterStatus.value}` : ''}`,
      auth.token,
    ),
  { perPage: 20 },
)
const { loading: loadingMutasi, error: errorMutasi, run } = useAsync()
const error = computed(() => errorList.value || errorMutasi.value)
const kategori = ref<Kategori[]>([])
const cabang = ref<Cabang[]>([])

const bolehCatat = computed(() => access.can('biaya.input'))
const bolehPutus = computed(() => access.can('biaya.setujui'))

const opsiStatus = [
  { value: 'draft', label: 'Draft' },
  { value: 'disetujui', label: 'Disetujui' },
  { value: 'ditolak', label: 'Ditolak' },
]
const opsiMetode = [
  { value: 'tunai', label: 'Tunai' },
  { value: 'transfer', label: 'Transfer' },
]
const opsiCabang = computed(() =>
  cabang.value.map((c) => ({ value: c.id, label: `${c.code} - ${c.name}` })),
)
const opsiKategori = computed(() =>
  kategori.value.map((k) => ({ value: k.id, label: k.name })),
)
const tabs = computed(() => [
  { key: 'biaya', label: 'Daftar biaya' },
  { key: 'kategori', label: 'Kategori', count: kategori.value.length },
])

const STATUS_LABEL: Record<string, string> = {
  draft: 'Draft',
  disetujui: 'Disetujui',
  ditolak: 'Ditolak',
}
const METODE_LABEL: Record<string, string> = {
  tunai: 'Tunai',
  transfer: 'Transfer',
}
const labelStatus = (status: string) => STATUS_LABEL[status] ?? status
const labelMetode = (method?: string) => (method ? (METODE_LABEL[method] ?? method) : '—')
const tone = (status: string): 'green' | 'red' | 'amber' =>
  status === 'disetujui' ? 'green' : status === 'ditolak' ? 'red' : 'amber'
const namaCabang = (id?: number) =>
  id ? (cabang.value.find((c) => c.id === id)?.name ?? `Cabang #${id}`) : '—'

function formKosong() {
  return {
    branch_id: null as number | null,
    expense_category_id: null as number | null,
    amount: null as number | null,
    method: 'tunai',
    date: new Date().toISOString().slice(0, 10),
    description: '',
  }
}

const showForm = ref(false)
const fBiaya = ref(formKosong())
const namaKat = ref('')
const galatKategori = ref('')

function bukaForm() {
  fBiaya.value = formKosong()
  showForm.value = true
}

async function muatCabang() {
  cabang.value =
    (await run(() => api.get<Cabang[]>(`/companies/${companyId.value}/branches`, auth.token))) ?? []
}

async function muatKategori() {
  const res = await run(() =>
    api.get<Kategori[]>(
      `/companies/${companyId.value}/expense-categories?per_page=100`,
      auth.token,
    ),
  )
  if (res)
    kategori.value = (
      Array.isArray(res) ? res : ((res as unknown as { data: Kategori[] }).data ?? [])
    ) as Kategori[]
}

async function simpanBiaya() {
  if (
    !fBiaya.value.branch_id ||
    !fBiaya.value.expense_category_id ||
    !fBiaya.value.amount ||
    !fBiaya.value.description.trim()
  ) {
    return
  }
  const res = await run(() =>
    api.post(
      `/companies/${companyId.value}/expenses`,
      {
        branch_id: fBiaya.value.branch_id,
        expense_category_id: fBiaya.value.expense_category_id,
        amount: fBiaya.value.amount,
        method: fBiaya.value.method || undefined,
        date: fBiaya.value.date,
        description: fBiaya.value.description.trim(),
      },
      auth.token,
    ),
  )
  if (res) {
    showForm.value = false
    okMsg.value = 'Biaya tercatat sebagai draft. Minta persetujuan agar masuk laporan.'
    fBiaya.value = formKosong()
    muatBiaya(1)
  }
}

async function putuskan(id: number, setuju: boolean) {
  const res = await run(() =>
    api.post(
      `/companies/${companyId.value}/expenses/${id}/${setuju ? 'approve' : 'reject'}`,
      {},
      auth.token,
    ),
  )
  if (res || !errorMutasi.value) {
    okMsg.value = setuju ? 'Biaya disetujui dan dijurnal otomatis.' : 'Biaya ditolak.'
    muatBiaya()
  }
}

async function tambahKategori() {
  const name = namaKat.value.trim()
  if (!name) {
    galatKategori.value = 'Nama kategori wajib diisi.'
    return
  }
  galatKategori.value = ''
  const res = await run(() =>
    api.post(`/companies/${companyId.value}/expense-categories`, { name }, auth.token),
  )
  if (res) {
    namaKat.value = ''
    okMsg.value = 'Kategori ditambahkan.'
    muatKategori()
  }
}

async function hapusKategori(k: Kategori) {
  const yakin = await confirm({
    title: `Hapus kategori ${k.name}?`,
    message: 'Biaya lama tetap tersimpan, tetapi kategori ini tidak bisa dipakai lagi.',
    confirmLabel: 'Hapus kategori',
    tone: 'danger',
  })
  if (!yakin) return
  await run(() => api.del(`/companies/${companyId.value}/expense-categories/${k.id}`, auth.token))
  if (!errorMutasi.value) {
    okMsg.value = 'Kategori dihapus.'
    muatKategori()
  }
}

function gantiStatus(value: string | number | null) {
  filterStatus.value = value === null ? '' : String(value)
  muatBiaya(1)
}

onMounted(async () => {
  await access.load(companyId.value, auth.token)
  await muatCabang()
  await muatKategori()
  await muatBiaya()
})
</script>

<template>
  <div>
    <AppPageHeader
      eyebrow="Operasional harian"
      title="Biaya operasional"
      subtitle="Catat pengeluaran cabang, ajukan persetujuan, dan kelola kategori biaya."
      icon="receipt_long"
    >
      <template #actions>
        <AppButton v-if="bolehCatat && tab === 'biaya'" @click="bukaForm">Catat biaya</AppButton>
      </template>
    </AppPageHeader>

    <AppBanner tone="green" :message="okMsg" />
    <AppBanner tone="red" :message="error" />

    <AppTabs
      :model-value="tab"
      :items="tabs"
      label="Tampilan biaya"
      class="mb-4"
      @update:model-value="tab = $event"
    />

    <div v-if="tab === 'biaya'">
      <div class="mb-4 flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
        <AppField label="Status" class="w-full sm:w-52">
          <AppSelect
            :model-value="filterStatus || null"
            :options="opsiStatus"
            placeholder="Semua status"
            @update:model-value="gantiStatus"
          />
        </AppField>
        <p class="text-xs leading-relaxed text-secondary sm:pb-2.5">
          Yang masih draft belum masuk laporan laba rugi.
        </p>
      </div>

      <div v-if="loadingList" class="grid gap-3 md:grid-cols-2" aria-busy="true">
        <AppCard v-for="i in 4" :key="i">
          <AppSkeleton :lines="4" />
        </AppCard>
        <span class="sr-only" role="status">Memuat biaya...</span>
      </div>

      <AppEmpty
        v-else-if="!list.length"
        icon="receipt_long"
        title="Belum ada biaya"
        :message="
          filterStatus
            ? 'Tidak ada biaya dengan status ini. Ubah filter untuk melihat data lain.'
            : 'Biaya yang dicatat akan muncul di sini sebagai draft.'
        "
      >
        <AppButton v-if="bolehCatat" @click="bukaForm">Catat biaya</AppButton>
      </AppEmpty>

      <div v-else class="grid gap-3 md:grid-cols-2">
        <AppCard v-for="b in list" :key="b.id">
          <div class="flex items-start justify-between gap-3">
            <div class="min-w-0">
              <p class="font-display text-lg font-bold tabular-nums text-ink">
                {{ rupiah(b.amount) }}
              </p>
              <p class="mt-1 text-sm leading-relaxed text-ink">
                {{ b.description ?? 'Tanpa keterangan' }}
              </p>
            </div>
            <AppBadge :tone="tone(b.status)" dot>{{ labelStatus(b.status) }}</AppBadge>
          </div>

          <dl class="mt-3 grid grid-cols-2 gap-x-3 gap-y-1 text-xs text-secondary">
            <div>
              <dt class="sr-only">Kategori</dt>
              <dd>{{ b.categoryName ?? 'Tanpa kategori' }}</dd>
            </div>
            <div class="text-right">
              <dt class="sr-only">Tanggal</dt>
              <dd>{{ tanggal(b.date) }}</dd>
            </div>
            <div>
              <dt class="sr-only">Cabang</dt>
              <dd>{{ namaCabang(b.branchId) }}</dd>
            </div>
            <div class="text-right">
              <dt class="sr-only">Cara bayar</dt>
              <dd>{{ labelMetode(b.method) }}</dd>
            </div>
          </dl>

          <div
            v-if="b.status === 'draft' && bolehPutus"
            class="mt-3 flex flex-wrap gap-2 border-t border-outline/50 pt-3"
          >
            <AppButton size="sm" :disabled="loadingMutasi" @click="putuskan(b.id, true)">
              Setujui
            </AppButton>
            <AppButton
              size="sm"
              variant="ghost"
              :disabled="loadingMutasi"
              @click="putuskan(b.id, false)"
            >
              Tolak
            </AppButton>
          </div>
        </AppCard>
      </div>

      <AppPager :meta="meta" @page="setPage" @update:per-page="setPerPage" />
    </div>

    <AppCard v-else>
      <div class="flex flex-col gap-1">
        <h2 class="font-display font-semibold text-ink">Kategori biaya</h2>
        <p class="text-xs leading-relaxed text-secondary">
          Kategori membantu merapikan laporan pengeluaran.
        </p>
      </div>

      <div v-if="bolehPutus" class="mt-4 flex flex-col gap-2 sm:flex-row sm:items-end">
        <AppField label="Nama kategori" class="flex-1" :error="galatKategori">
          <AppInput
            v-model="namaKat"
            maxlength="255"
            placeholder="cth. Listrik & air"
            @input="galatKategori = ''"
          />
        </AppField>
        <AppButton
          class="w-full sm:w-auto"
          :loading="loadingMutasi"
          @click="tambahKategori"
        >
          Tambah
        </AppButton>
      </div>

      <ul v-if="kategori.length" class="mt-4 divide-y divide-outline/60">
        <li
          v-for="k in kategori"
          :key="k.id"
          class="flex items-center justify-between gap-3 py-2.5 text-sm"
        >
          <span class="min-w-0 text-ink">{{ k.name }}</span>
          <AppButton
            v-if="bolehPutus"
            variant="ghost"
            size="sm"
            class="text-danger"
            :aria-label="`Hapus kategori ${k.name}`"
            @click="hapusKategori(k)"
          >
            Hapus
          </AppButton>
        </li>
      </ul>
      <AppEmpty
        v-else
        class="mt-4"
        compact
        icon="category"
        title="Belum ada kategori"
        message="Tambahkan kategori agar biaya bisa dikelompokkan."
      />
    </AppCard>

    <AppModal
      :open="showForm"
      title="Catat biaya baru"
      description="Biaya akan disimpan sebagai draft dan menunggu persetujuan."
      size="md"
      @close="showForm = false"
    >
      <AppBanner tone="red" :message="errorMutasi" />
      <form id="expense-form" class="grid gap-3" @submit.prevent="simpanBiaya">
        <div class="grid gap-3 sm:grid-cols-2">
          <AppField label="Cabang" required>
            <AppSelect
              v-model="fBiaya.branch_id"
              :options="opsiCabang"
              placeholder="Pilih cabang"
            />
          </AppField>
          <AppField label="Kategori" required>
            <AppSelect
              v-model="fBiaya.expense_category_id"
              :options="opsiKategori"
              placeholder="Pilih kategori"
            />
          </AppField>
          <AppField label="Nominal" required>
            <AppMoneyInput v-model="fBiaya.amount" />
          </AppField>
          <AppField label="Cara bayar" required>
            <AppSelect v-model="fBiaya.method" :options="opsiMetode" />
          </AppField>
          <AppField label="Tanggal" required class="sm:col-span-2">
            <AppDateInput v-model="fBiaya.date" />
          </AppField>
          <AppField
            label="Keterangan"
            required
            class="sm:col-span-2"
            hint="Tuliskan kebutuhan biaya secara singkat."
          >
            <AppInput
              v-model="fBiaya.description"
              maxlength="500"
              placeholder="cth. Bayar listrik bulan ini"
            />
          </AppField>
        </div>
      </form>
      <template #footer>
        <AppButton variant="ghost" @click="showForm = false">Batal</AppButton>
        <AppButton
          type="submit"
          form="expense-form"
          :loading="loadingMutasi"
          :disabled="
            !fBiaya.branch_id ||
            !fBiaya.expense_category_id ||
            !fBiaya.amount ||
            !fBiaya.description.trim()
          "
        >
          Simpan draft
        </AppButton>
      </template>
    </AppModal>
  </div>
</template>
