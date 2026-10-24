<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { api } from '../lib/api'
import { tanggal } from '../lib/format'
import { formatValue, labelAksi, labelEntitas, labelField } from '../lib/labels'
import { useCompany } from '../composables/useCompany'
import { useAccessStore } from '../stores/access'
import { useAsync } from '../composables/useAsync'
import { usePagedList } from '../composables/usePagedList'
import AppModal from '../components/ui/AppModal.vue'
import AppCard from '../components/ui/AppCard.vue'
import AppBadge from '../components/ui/AppBadge.vue'
import AppBanner from '../components/ui/AppBanner.vue'
import AppButton from '../components/ui/AppButton.vue'
import AppEmpty from '../components/ui/AppEmpty.vue'
import AppField from '../components/ui/AppField.vue'
import AppIcon from '../components/ui/AppIcon.vue'
import AppInput from '../components/ui/AppInput.vue'
import AppPageHeader from '../components/ui/AppPageHeader.vue'
import AppPager from '../components/ui/AppPager.vue'
import AppSelect from '../components/ui/AppSelect.vue'
import AppTabs from '../components/ui/AppTabs.vue'
import AppTextarea from '../components/ui/AppTextarea.vue'

interface Approval {
  id: number
  branchId: number | null
  entityType: string | null
  entityId: number | null
  action: string
  actionLabel: string
  status: 'diajukan' | 'disetujui' | 'ditolak'
  payload: Record<string, unknown> | null
  requestedBy: number
  decidedAt: string | null
  note: string | null
}

const ACTIONS = [
  { value: 'sale.void', label: 'Void nota', req: 'transaksi.buat', decide: 'transaksi.void' },
  { value: 'sale.discount', label: 'Nota diskon', req: 'transaksi.buat', decide: 'transaksi.diskon' },
  { value: 'sale.refund', label: 'Retur / refund', req: 'retur.buat', decide: 'retur.setujui' },
  { value: 'po.approve', label: 'Setujui PO', req: 'po.buat', decide: 'po.setujui' },
  { value: 'transfer.approve', label: 'Setujui transfer', req: 'stok.transfer', decide: 'stok.transfer' },
  { value: 'opname.approve', label: 'Setujui opname', req: 'stok.opname', decide: 'stok.opname' },
] as const
const NEED_ENTITY = ['sale.void', 'sale.refund', 'po.approve', 'transfer.approve', 'opname.approve']
const STATUS_TABS = [
  { key: 'diajukan', label: 'Menunggu' },
  { key: 'disetujui', label: 'Disetujui' },
  { key: 'ditolak', label: 'Ditolak' },
]
const STATUS_LABEL: Record<string, string> = {
  diajukan: 'Menunggu',
  disetujui: 'Disetujui',
  ditolak: 'Ditolak',
}

const { auth, companyId } = useCompany()
const access = useAccessStore()
const detailAsync = useAsync()
const pengajuAsync = useAsync()
const mutasiAsync = useAsync()
const status = ref('diajukan')
const filterAksi = ref('')
const {
  rows: list,
  meta,
  loading: loadingList,
  error: errorList,
  load: muat,
  setPage,
  setPerPage,
} = usePagedList<Approval>(
  (page, perPage) =>
    api.getPage<Approval>(
      `/companies/${companyId.value}/approvals?status=${status.value}${filterAksi.value ? `&action=${filterAksi.value}` : ''}&per_page=${perPage}&page=${page}`,
      auth.token,
    ),
  { perPage: 15 },
)
const loading = computed(() => loadingList.value || mutasiAsync.loading.value)
const error = computed(() => errorList.value || mutasiAsync.error.value)
const detail = ref<Approval | null>(null)
const detailOpen = ref(false)
const formOpen = ref(false)
const pengaju = ref<{ name: string; role: string | null } | null>(null)
const okMsg = ref('')
const konfirmasi = ref<'setuju' | 'tolak' | null>(null)
const catatan = ref('')
const galatCatatan = ref(false)

const form = ref<{ action: string; entity_id: number | null; reason: string; note: string }>({
  action: 'sale.void',
  entity_id: null,
  reason: '',
  note: '',
})
const tampilMentah = ref(false)
const payloadRows = computed(() =>
  Object.entries(detail.value?.payload ?? {}).map(([key, value]) => ({
    key,
    label: labelField(key),
    value: formatValue(value),
  })),
)
const judulAksi = (item: Approval) => labelAksi(item.action, item.actionLabel)
const entitasLabel = (item: Approval) =>
  item.entityType ? `${labelEntitas(item.entityType)} #${item.entityId ?? '-'}` : 'Tanpa data terkait'
const labelStatus = (value: string) => STATUS_LABEL[value] ?? value
const tone = (value: string) => (value === 'disetujui' ? 'green' : value === 'ditolak' ? 'red' : 'amber')

const bolehAjukan = () => ACTIONS.filter((action) => access.can(action.req))
const opsiAjukan = computed(() =>
  bolehAjukan().map((action) => ({ value: action.value, label: action.label })),
)
const opsiAksi = ACTIONS.map((action) => ({ value: action.value, label: action.label }))
const bolehPutuskan = (action: string) => {
  const found = ACTIONS.find((item) => item.value === action)
  return found ? access.can(found.decide) : false
}

function formKosong() {
  return {
    action: opsiAjukan.value[0]?.value ?? '',
    entity_id: null,
    reason: '',
    note: '',
  }
}

function bukaForm() {
  mutasiAsync.error.value = ''
  form.value = formKosong()
  formOpen.value = true
}

async function lihat(id: number) {
  detailOpen.value = true
  detail.value = null
  pengaju.value = null
  tampilMentah.value = false
  const found = await detailAsync.run(() =>
    api.get<Approval>(`/companies/${companyId.value}/approvals/${id}`, auth.token),
  )
  if (!found) return
  detail.value = found
  const card = await pengajuAsync.run(() =>
    api.get<{ name: string; role: string | null }>(
      `/companies/${companyId.value}/users/${found.requestedBy}/card`,
      auth.token,
    ),
  )
  if (card) pengaju.value = { name: card.name, role: card.role }
}

function tutupDetail() {
  detailOpen.value = false
}

async function ajukan() {
  const payload: Record<string, unknown> = {}
  if (form.value.reason.trim()) payload.reason = form.value.reason.trim()
  const res = await mutasiAsync.run(() =>
    api.post<Approval>(
      `/companies/${companyId.value}/approvals`,
      {
        action: form.value.action,
        entity_id: form.value.entity_id ?? undefined,
        payload,
        note: form.value.note.trim() || undefined,
      },
      auth.token,
    ),
  )
  if (res) {
    okMsg.value = 'Pengajuan terkirim, menunggu atasan.'
    formOpen.value = false
    form.value = formKosong()
    muat(1)
  }
}

function bukaKonfirmasi(mode: 'setuju' | 'tolak') {
  mutasiAsync.error.value = ''
  galatCatatan.value = false
  catatan.value = ''
  detailOpen.value = false
  konfirmasi.value = mode
}

function batalKonfirmasi() {
  konfirmasi.value = null
  if (detail.value) detailOpen.value = true
}

async function putuskan(setuju: boolean) {
  if (!detail.value) return
  if (!setuju && !catatan.value.trim()) {
    galatCatatan.value = true
    return
  }
  galatCatatan.value = false
  const res = await mutasiAsync.run(() =>
    api.post<Approval>(
      `/companies/${companyId.value}/approvals/${detail.value!.id}/${setuju ? 'approve' : 'reject'}`,
      { note: catatan.value || undefined },
      auth.token,
    ),
  )
  if (res) {
    okMsg.value = setuju ? 'Disetujui dan dijalankan.' : 'Ditolak.'
    konfirmasi.value = null
    detailOpen.value = false
    catatan.value = ''
    detail.value = res
    muat()
  }
}

async function salinPayload() {
  if (detail.value?.payload) {
    await navigator.clipboard.writeText(JSON.stringify(detail.value.payload, null, 2))
  }
}

function gantiStatus(key: string) {
  status.value = key
  muat(1)
}

function gantiAksi(value: string | number | null) {
  filterAksi.value = value === null ? '' : String(value)
  muat(1)
}

function resetFilter() {
  filterAksi.value = ''
  muat(1)
}

onMounted(async () => {
  await access.load(companyId.value, auth.token)
  form.value = formKosong()
  muat()
})
</script>

<template>
  <div>
    <AppPageHeader
      eyebrow="Pusat otorisasi multi-cabang"
      title="Kotak persetujuan"
      subtitle="Tinjau pengajuan dari cabang, verifikasi data, lalu setujui atau tolak dengan jejak audit."
      icon="fact_check"
    >
      <template #actions>
        <AppButton variant="ghost" :loading="loadingList" @click="muat()">Muat ulang</AppButton>
        <AppButton v-if="opsiAjukan.length" @click="bukaForm">Ajukan baru</AppButton>
      </template>
    </AppPageHeader>

    <AppBanner tone="green" :message="okMsg" />
    <AppBanner tone="red" :message="error" />

    <AppCard>
      <div class="flex flex-col gap-3 lg:flex-row lg:items-end lg:justify-between">
        <AppTabs
          :items="STATUS_TABS"
          :model-value="status"
          label="Status pengajuan"
          @update:model-value="gantiStatus"
        />
        <div class="flex flex-wrap items-end gap-2">
          <AppField label="Jenis aksi" class="w-full sm:w-56">
            <AppSelect
              :model-value="filterAksi"
              :options="opsiAksi"
              placeholder="Semua aksi"
              @update:model-value="gantiAksi"
            />
          </AppField>
          <AppButton v-if="filterAksi" variant="ghost" @click="resetFilter">Reset</AppButton>
        </div>
      </div>

      <div class="mt-4 flex flex-wrap items-center justify-between gap-2">
        <p class="text-sm text-secondary">
          {{
            status === 'diajukan'
              ? 'Antrian menunggu otorisasi, yang terlama dulu.'
              : 'Riwayat keputusan pengajuan.'
          }}
        </p>
        <p v-if="meta" class="text-xs tabular-nums text-secondary">{{ meta.total }} pengajuan</p>
      </div>

      <div v-if="loadingList" class="mt-3 space-y-2" aria-busy="true">
        <div v-for="i in 4" :key="i" class="h-20 animate-pulse rounded-2xl bg-surface-low" />
        <span class="sr-only" role="status">Memuat pengajuan...</span>
      </div>

      <AppEmpty
        v-else-if="!list.length"
        class="mt-3"
        icon="task_alt"
        title="Tidak ada pengajuan"
        :message="
          status === 'diajukan'
            ? 'Antrian bersih. Pengajuan baru dari cabang akan muncul di sini.'
            : 'Belum ada riwayat untuk status ini.'
        "
      />

      <div v-else class="mt-3 space-y-2">
        <button
          v-for="a in list"
          :key="a.id"
          type="button"
          class="group flex w-full items-start gap-3 rounded-2xl border border-outline/70 bg-card p-4 text-left shadow-card transition hover:border-outline-strong hover:bg-surface-low/40 hover:shadow-lifted focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/55 focus-visible:ring-offset-2"
          :aria-label="`Lihat pengajuan ${judulAksi(a)} nomor ${a.id}`"
          @click="lihat(a.id)"
        >
          <span
            class="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-primary-soft text-primary-ink"
          >
            <AppIcon name="fact_check" :size="21" />
          </span>
          <span class="min-w-0 flex-1">
            <span class="flex flex-wrap items-center justify-between gap-2">
              <span class="truncate font-display text-sm font-semibold text-ink">
                {{ judulAksi(a) }}
                <span class="font-normal text-secondary">#{{ a.id }}</span>
              </span>
              <AppBadge :tone="tone(a.status)" dot>{{ labelStatus(a.status) }}</AppBadge>
            </span>
            <span class="mt-1 block truncate text-sm text-secondary">
              {{ a.note ?? entitasLabel(a) }}
            </span>
            <span class="mt-1 block text-xs text-secondary">
              {{ entitasLabel(a) }} - diajukan oleh pengguna #{{ a.requestedBy }}
            </span>
          </span>
          <AppIcon
            name="chevron_right"
            :size="18"
            class="mt-1 shrink-0 text-secondary transition group-hover:translate-x-0.5"
          />
        </button>
      </div>

      <AppPager :meta="meta" @page="setPage" @update:per-page="setPerPage" />
    </AppCard>

    <AppModal
      :open="formOpen"
      title="Ajukan persetujuan"
      description="Pengajuan akan masuk ke antrian atasan yang berhak memutuskan."
      size="md"
      @close="formOpen = false"
    >
      <AppBanner tone="red" :message="mutasiAsync.error.value" />
      <div class="grid gap-3">
        <AppField label="Jenis pengajuan" required>
          <AppSelect v-model="form.action" :options="opsiAjukan" placeholder="Pilih jenis" />
        </AppField>
        <AppField
          v-if="NEED_ENTITY.includes(form.action)"
          label="ID data terkait"
          required
          hint="Nomor nota, PO, transfer, atau opname yang diajukan."
        >
          <AppInput v-model="form.entity_id" type="number" min="1" placeholder="cth. 128" />
        </AppField>
        <AppField label="Alasan" hint="Jelaskan singkat kenapa pengajuan ini diperlukan.">
          <AppTextarea v-model="form.reason" :rows="3" :maxlength="500" show-count />
        </AppField>
        <AppField label="Catatan tambahan" optional>
          <AppTextarea v-model="form.note" :rows="2" :maxlength="500" />
        </AppField>
      </div>
      <template #footer>
        <AppButton variant="ghost" @click="formOpen = false">Batal</AppButton>
        <AppButton
          :loading="mutasiAsync.loading.value"
          :disabled="!form.action || (NEED_ENTITY.includes(form.action) && !form.entity_id)"
          @click="ajukan"
        >
          Kirim pengajuan
        </AppButton>
      </template>
    </AppModal>

    <AppModal
      :open="detailOpen"
      :title="detail ? `${judulAksi(detail)} #${detail.id}` : 'Detail pengajuan'"
      :description="detail ? `${entitasLabel(detail)}${pengaju ? ` - ${pengaju.name}` : ''}` : ''"
      size="lg"
      @close="tutupDetail"
    >
      <div v-if="detailAsync.loading.value && !detail" class="space-y-2" aria-busy="true">
        <div v-for="i in 4" :key="i" class="h-10 animate-pulse rounded-lg bg-surface-low" />
        <span class="sr-only" role="status">Memuat detail pengajuan...</span>
      </div>
      <AppBanner v-else-if="detailAsync.error.value" tone="red" :message="detailAsync.error.value" />
      <template v-else-if="detail">
        <div class="flex flex-wrap items-center gap-2">
          <AppBadge :tone="tone(detail.status)" dot>{{ labelStatus(detail.status) }}</AppBadge>
          <span class="text-sm text-secondary">{{ entitasLabel(detail) }}</span>
        </div>

        <dl class="mt-3 grid gap-2 text-sm sm:grid-cols-2">
          <div class="rounded-xl bg-surface-low px-3 py-2">
            <dt class="text-xs text-secondary">Diajukan oleh</dt>
            <dd class="font-medium text-ink">
              {{ pengaju?.name ?? `Pengguna #${detail.requestedBy}` }}
              <span v-if="pengaju?.role" class="font-normal text-secondary">({{ pengaju.role }})</span>
            </dd>
          </div>
          <div class="rounded-xl bg-surface-low px-3 py-2">
            <dt class="text-xs text-secondary">Jenis aksi</dt>
            <dd class="font-medium text-ink">{{ judulAksi(detail) }}</dd>
          </div>
          <div class="rounded-xl bg-surface-low px-3 py-2">
            <dt class="text-xs text-secondary">Status</dt>
            <dd class="font-medium text-ink">{{ labelStatus(detail.status) }}</dd>
          </div>
          <div class="rounded-xl bg-surface-low px-3 py-2">
            <dt class="text-xs text-secondary">Diputus</dt>
            <dd class="font-medium text-ink">
              {{ detail.decidedAt ? tanggal(detail.decidedAt) : 'Belum diputus' }}
            </dd>
          </div>
        </dl>

        <div class="mt-4 flex items-center justify-between gap-2">
          <p class="text-xs font-semibold uppercase tracking-wider text-secondary">Data verifikasi</p>
          <div class="flex gap-3">
            <button
              type="button"
              class="rounded text-xs font-semibold text-primary focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/55"
              @click="tampilMentah = !tampilMentah"
            >
              {{ tampilMentah ? 'Ringkas' : 'Mentah' }}
            </button>
            <button
              type="button"
              class="rounded text-xs font-semibold text-primary focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/55"
              @click="salinPayload"
            >
              Salin
            </button>
          </div>
        </div>
        <pre
          v-if="tampilMentah"
          class="mt-1 overflow-x-auto rounded-lg bg-surface-low p-3 text-xs"
        >{{ JSON.stringify(detail.payload, null, 2) }}</pre>
        <dl v-else class="mt-2 divide-y divide-outline/50 text-sm">
          <div v-for="row in payloadRows" :key="row.key" class="flex justify-between gap-4 py-2">
            <dt class="shrink-0 text-secondary">{{ row.label }}</dt>
            <dd class="break-words text-right font-medium text-ink">{{ row.value }}</dd>
          </div>
          <p v-if="!payloadRows.length" class="py-2 text-sm text-secondary">
            Tidak ada data tambahan.
          </p>
        </dl>

        <p v-if="detail.note" class="mt-3 rounded-xl bg-surface-low px-3 py-2 text-sm text-secondary">
          <span class="font-medium text-ink">Catatan:</span> {{ detail.note }}
        </p>
        <p
          v-if="detail.status === 'diajukan' && !bolehPutuskan(detail.action)"
          class="mt-3 text-sm text-secondary"
        >
          Hanya atasan yang berhak memutuskan pengajuan ini.
        </p>
      </template>
      <template #footer>
        <AppButton variant="ghost" @click="tutupDetail">Tutup</AppButton>
        <template v-if="detail?.status === 'diajukan' && bolehPutuskan(detail.action)">
          <AppButton variant="danger" :loading="loading" @click="bukaKonfirmasi('tolak')">
            Tolak
          </AppButton>
          <AppButton :loading="loading" @click="bukaKonfirmasi('setuju')">
            Setujui &amp; jalankan
          </AppButton>
        </template>
      </template>
    </AppModal>

    <AppModal
      :open="Boolean(konfirmasi && detail)"
      :title="konfirmasi === 'setuju' ? 'Setujui pengajuan?' : 'Tolak pengajuan?'"
      :description="
        detail
          ? `${judulAksi(detail)} #${detail.id}. ${
              konfirmasi === 'setuju'
                ? 'Aksi langsung dijalankan dan tercatat di audit.'
                : 'Pengaju akan melihat alasan Anda.'
            }`
          : ''
      "
      size="sm"
      @close="batalKonfirmasi"
    >
      <AppBanner tone="red" :message="mutasiAsync.error.value" />
      <AppField
        :label="`Catatan${konfirmasi === 'tolak' ? ' (wajib diisi)' : ' (opsional)'}`"
        :required="konfirmasi === 'tolak'"
        :error="galatCatatan ? 'Tulis alasan penolakan terlebih dahulu.' : ''"
      >
        <AppTextarea
          v-model="catatan"
          :rows="3"
          :maxlength="500"
          show-count
          :invalid="galatCatatan"
          placeholder="Tambahkan konteks untuk pengaju..."
        />
      </AppField>
      <template #footer>
        <AppButton variant="ghost" @click="batalKonfirmasi">Batal</AppButton>
        <AppButton
          :variant="konfirmasi === 'setuju' ? 'primary' : 'danger'"
          :loading="loading"
          :disabled="konfirmasi === 'tolak' && !catatan.trim()"
          @click="putuskan(konfirmasi === 'setuju')"
        >
          {{ konfirmasi === 'setuju' ? 'Jalankan persetujuan' : 'Tolak pengajuan' }}
        </AppButton>
      </template>
    </AppModal>
  </div>
</template>
