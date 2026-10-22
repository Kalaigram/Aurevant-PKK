<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { api, type PageMeta } from '../lib/api'
import { tanggal } from '../lib/format'
import { formatValue, labelAksi, labelEntitas, labelField } from '../lib/labels'
import { useCompany } from '../composables/useCompany'
import { useAccessStore } from '../stores/access'
import { useAsync } from '../composables/useAsync'
import AppModal from '../components/ui/AppModal.vue'
import AppCard from '../components/ui/AppCard.vue'
import AppBadge from '../components/ui/AppBadge.vue'
import AppBanner from '../components/ui/AppBanner.vue'
import AppButton from '../components/ui/AppButton.vue'
import AppField from '../components/ui/AppField.vue'
import AppEmpty from '../components/ui/AppEmpty.vue'
import AppPager from '../components/ui/AppPager.vue'
import AppIcon from '../components/ui/AppIcon.vue'

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

const { auth, companyId } = useCompany()
const access = useAccessStore()
const { loading, error, run } = useAsync()
const list = ref<Approval[]>([])
const meta = ref<PageMeta | null>(null)
const status = ref('diajukan')
const filterAksi = ref('')
const detail = ref<Approval | null>(null)
const pengaju = ref<{ name: string; role: string | null } | null>(null)
const okMsg = ref('')
const konfirmasi = ref<'setuju' | 'tolak' | null>(null)
const catatan = ref('')
const galatCatatan = ref(false)

const form = ref({ action: 'sale.void', entity_id: '', reason: '', note: '' })
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
  item.entityType ? `${labelEntitas(item.entityType)} #${item.entityId ?? '—'}` : 'Tanpa data terkait'

const bolehAjukan = () => ACTIONS.filter((a) => access.can(a.req))
const bolehPutuskan = (action: string) => {
  const found = ACTIONS.find((a) => a.value === action)
  return found ? access.can(found.decide) : false
}
const tone = (s: string) => (s === 'disetujui' ? 'green' : s === 'ditolak' ? 'red' : 'amber')

async function muat(page = 1) {
  okMsg.value = ''
  const res = await run(() =>
    api.getPage<Approval>(
      `/companies/${companyId.value}/approvals?status=${status.value}${filterAksi.value ? `&action=${filterAksi.value}` : ''}&per_page=15&page=${page}`,
      auth.token,
    ),
  )
  if (res) {
    const raw = res as unknown as { data: Approval[]; meta?: PageMeta } | Approval[]
    if (Array.isArray(raw)) list.value = raw
    else { list.value = raw.data; meta.value = raw.meta ?? null }
  }
}

async function lihat(id: number) {
  konfirmasi.value = null
  catatan.value = ''
  tampilMentah.value = false
  const found = await run(() => api.get<Approval>(`/companies/${companyId.value}/approvals/${id}`, auth.token))
  detail.value = found
  pengaju.value = null
  if (found) {
    const card = await run(() =>
      api.get<{ name: string; role: string | null }>(`/companies/${companyId.value}/users/${found.requestedBy}/card`, auth.token),
    )
    if (card) pengaju.value = { name: card.name, role: card.role }
  }
}

async function ajukan() {
  const payload: Record<string, unknown> = {}
  if (form.value.reason) payload.reason = form.value.reason
  const res = await run(() =>
    api.post<Approval>(`/companies/${companyId.value}/approvals`, {
      action: form.value.action,
      entity_id: form.value.entity_id ? Number(form.value.entity_id) : undefined,
      payload,
      note: form.value.note || undefined,
    }, auth.token),
  )
  if (res) {
    okMsg.value = 'Pengajuan terkirim, menunggu atasan.'
    form.value = { action: 'sale.void', entity_id: '', reason: '', note: '' }
    muat(1)
  }
}

async function putuskan(setuju: boolean) {
  if (!detail.value) return
  if (!setuju && !catatan.value.trim()) {
    galatCatatan.value = true
    return
  }
  galatCatatan.value = false
  const res = await run(() =>
    api.post<Approval>(
      `/companies/${companyId.value}/approvals/${detail.value!.id}/${setuju ? 'approve' : 'reject'}`,
      { note: catatan.value || undefined }, auth.token,
    ),
  )
  if (res) {
    okMsg.value = setuju ? 'Disetujui dan dijalankan.' : 'Ditolak.'
    konfirmasi.value = null
    catatan.value = ''
    detail.value = res
    muat(meta.value?.current_page ?? 1)
  }
}

async function salinPayload() {
  if (detail.value?.payload) await navigator.clipboard.writeText(JSON.stringify(detail.value.payload, null, 2))
}

onMounted(async () => {
  await access.load(companyId.value, auth.token)
  const bisa = bolehAjukan()
  form.value.action = bisa.length ? bisa[0]!.value : ''
  muat()
})
</script>

<template>
  <div>
    <p class="text-xs font-semibold uppercase tracking-wider text-secondary">Pusat otorisasi multi-cabang</p>
    <div class="mb-4 flex flex-wrap items-center justify-between gap-2">
      <h1 class="font-display text-xl font-semibold">Kotak persetujuan</h1>
      <p class="text-sm text-secondary">Keputusan hanya oleh atasan yang berhak.</p>
    </div>
    <AppBanner tone="green" :message="okMsg" />
    <AppBanner tone="red" :message="error" />

    <div class="grid gap-4 lg:grid-cols-5">
      <div class="lg:col-span-2">
        <AppCard title="Ajukan baru">
          <template v-if="!bolehAjukan().length">
            <p class="text-sm text-secondary">Anda tidak punya hak mengajukan persetujuan apa pun.</p>
          </template>
          <template v-else>
            <AppField label="Jenis pengajuan">
              <select v-model="form.action" class="w-full rounded-lg border border-outline bg-card px-3 py-2.5 focus:border-primary focus:outline-none">
                <option v-for="a in bolehAjukan()" :key="a.value" :value="a.value">{{ a.label }}</option>
              </select>
            </AppField>
            <div v-if="NEED_ENTITY.includes(form.action)" class="mt-3">
              <AppField label="Nomor data (ID nota, PO, transfer, atau opname)">
                <input v-model="form.entity_id" type="number" class="w-full rounded-lg border border-outline bg-card px-3 py-2.5 focus:border-primary focus:outline-none" />
              </AppField>
            </div>
            <div class="mt-3">
              <AppField label="Alasan">
                <textarea v-model="form.reason" class="w-full rounded-lg border border-outline bg-card px-3 py-2.5 focus:border-primary focus:outline-none" rows="2" />
              </AppField>
            </div>
            <AppButton :loading="loading" class="mt-3" @click="ajukan">Kirim pengajuan</AppButton>
            <p class="mt-2 text-xs text-secondary">Nota diskon & retur diajukan dari halaman Nota (Batch B).</p>
          </template>
        </AppCard>

        <AppCard v-if="detail" title="Detail pengajuan" class="mt-4">
          <div class="flex items-center justify-between gap-2">
            <p class="font-display font-semibold">{{ judulAksi(detail) }} <span class="font-normal text-secondary">#{{ detail.id }}</span></p>
            <AppBadge :tone="tone(detail.status)">{{ detail.status }}</AppBadge>
          </div>
          <p class="mt-1 text-sm text-secondary">
            Diajukan oleh <strong class="text-ink">{{ pengaju?.name ?? '—' }}</strong>
            <span v-if="pengaju?.role"> ({{ pengaju.role }})</span>
            · {{ entitasLabel(detail) }}
          </p>
          <div class="mt-3 flex items-center justify-between">
            <p class="text-xs font-semibold uppercase tracking-wider text-secondary">Data verifikasi</p>
            <div class="flex gap-3">
              <button class="text-xs font-semibold text-primary" @click="tampilMentah = !tampilMentah">
                {{ tampilMentah ? 'Ringkas' : 'Mentah' }}
              </button>
              <button class="text-xs font-semibold text-primary" @click="salinPayload">Salin</button>
            </div>
          </div>
          <pre v-if="tampilMentah" class="mt-1 overflow-x-auto rounded-lg bg-surface-low p-3 text-xs">{{ JSON.stringify(detail.payload, null, 2) }}</pre>
          <dl v-else class="mt-2 divide-y divide-outline/50 text-sm">
            <div v-for="row in payloadRows" :key="row.key" class="flex justify-between gap-4 py-2">
              <dt class="shrink-0 text-secondary">{{ row.label }}</dt>
              <dd class="break-words text-right font-medium text-ink">{{ row.value }}</dd>
            </div>
            <p v-if="!payloadRows.length" class="py-2 text-sm text-secondary">Tidak ada data tambahan.</p>
          </dl>
          <p v-if="detail.note" class="mt-2 text-sm text-secondary">Catatan: {{ detail.note }}</p>
          <p v-if="detail.decidedAt" class="text-xs text-secondary">Diputus {{ tanggal(detail.decidedAt) }}</p>
          <div v-if="detail.status === 'diajukan' && bolehPutuskan(detail.action)" class="mt-3 flex gap-2">
            <AppButton :loading="loading" @click="konfirmasi = 'setuju'">Setujui & jalankan</AppButton>
            <AppButton variant="danger" :loading="loading" @click="konfirmasi = 'tolak'">Tolak</AppButton>
          </div>
          <p v-else-if="detail.status === 'diajukan'" class="mt-3 text-sm text-secondary">Hanya atasan yang berhak memutuskan pengajuan ini.</p>
        </AppCard>
      </div>

      <div class="lg:col-span-3">
        <div class="mb-3 flex flex-wrap gap-2">
          <AppButton
            v-for="s in ['diajukan', 'disetujui', 'ditolak']" :key="s"
            :variant="status === s ? 'primary' : 'ghost'" @click="status = s; muat()"
          >{{ s }}<span v-if="s === 'diajukan' && meta"> ({{ meta.total }})</span></AppButton>
          <select v-model="filterAksi" class="rounded-lg border border-outline bg-card px-3 py-2 text-sm focus:border-primary focus:outline-none" @change="muat()">
            <option value="">Semua aksi</option>
            <option v-for="a in ACTIONS" :key="a.value" :value="a.value">{{ a.label }}</option>
          </select>
        </div>
        <p class="mb-2 text-sm text-secondary">Antrian menunggu otorisasi, yang terlama dulu.</p>
        <AppEmpty v-if="!loading && !list.length" message="Tidak ada pengajuan." />
        <AppCard v-for="a in list" :key="a.id" class="cursor-pointer transition hover:shadow" @click="lihat(a.id)">
          <div class="flex items-center gap-3">
            <div class="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg bg-surface-low text-primary">
              <AppIcon name="fact_check" :size="20" />
            </div>
            <div class="min-w-0 flex-1">
              <div class="flex items-center justify-between gap-2">
                <p class="truncate font-semibold">{{ judulAksi(a) }} <span class="font-normal text-secondary">#{{ a.id }}</span></p>
                <AppBadge :tone="tone(a.status)">{{ a.status }}</AppBadge>
              </div>
              <p class="truncate text-sm text-secondary">{{ a.note ?? entitasLabel(a) }}</p>
            </div>
          </div>
        </AppCard>
        <AppPager :meta="meta" @page="muat" />
      </div>
    </div>

    <AppModal
      :open="Boolean(konfirmasi && detail)"
      :title="konfirmasi === 'setuju' ? 'Setujui pengajuan?' : 'Tolak pengajuan?'"
      :description="`${detail ? judulAksi(detail) : ''} #${detail?.id ?? ''}. ${konfirmasi === 'setuju' ? 'Aksi langsung dijalankan dan tercatat di audit.' : 'Pengaju akan melihat alasan Anda.'}`"
      size="sm"
      @close="konfirmasi = null"
    >
      <label class="block text-sm font-medium text-ink" for="keputusan-catatan">
        Catatan{{ konfirmasi === 'tolak' ? ' (wajib diisi)' : ' (opsional)' }}
      </label>
      <textarea
        id="keputusan-catatan"
        v-model="catatan"
        rows="3"
        class="mt-1 w-full rounded-lg border border-outline bg-card px-3 py-2 text-sm focus:border-primary focus:outline-none"
      />
      <p v-if="galatCatatan" class="mt-1 text-xs text-danger">Tulis dulu alasan penolakan.</p>
      <template #footer>
        <AppButton variant="ghost" @click="konfirmasi = null">Batal</AppButton>
        <AppButton
          :variant="konfirmasi === 'setuju' ? 'primary' : 'danger'"
          :loading="loading"
          @click="putuskan(konfirmasi === 'setuju')"
        >
          {{ konfirmasi === 'setuju' ? 'Jalankan persetujuan' : 'Tolak pengajuan' }}
        </AppButton>
      </template>
    </AppModal>
  </div>
</template>
