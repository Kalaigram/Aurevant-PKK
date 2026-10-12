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
import AppIcon from '../components/ui/AppIcon.vue'

interface Approval {
  id: number
  entityType: string | null
  entityId: number | null
  action: string
  actionLabel: string
  status: 'diajukan' | 'disetujui' | 'ditolak'
  payload: Record<string, unknown> | null
  note: string | null
  decidedAt: string | null
}

const ACTIONS = ['sale.void', 'sale.discount', 'sale.refund', 'po.approve', 'transfer.approve', 'opname.approve']
const NEED_ENTITY = ['sale.void', 'sale.refund', 'po.approve', 'transfer.approve', 'opname.approve']

const { auth, companyId } = useCompany()
const { loading, error, run } = useAsync()
const list = ref<Approval[]>([])
const meta = ref<PageMeta | null>(null)
const status = ref('diajukan')
const detail = ref<Approval | null>(null)
const okMsg = ref('')

const form = ref({ action: 'sale.void', entity_id: '', reason: '', note: '' })

const tone = (s: string) => (s === 'disetujui' ? 'green' : s === 'ditolak' ? 'red' : 'amber')

async function muat(page = 1) {
  okMsg.value = ''
  const res = await run(() =>
    api.get<{ data: Approval[]; meta: PageMeta }>(
      `/companies/${companyId.value}/approvals?status=${status.value}&per_page=15&page=${page}`, auth.token,
    ),
  )
  if (res) {
    const raw = res as unknown as { data: Approval[]; meta?: PageMeta } | Approval[]
    if (Array.isArray(raw)) list.value = raw
    else { list.value = raw.data; meta.value = raw.meta ?? null }
  }
}

async function lihat(id: number) {
  detail.value = await run(() => api.get<Approval>(`/companies/${companyId.value}/approvals/${id}`, auth.token))
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
  const res = await run(() =>
    api.post<Approval>(
      `/companies/${companyId.value}/approvals/${detail.value!.id}/${setuju ? 'approve' : 'reject'}`,
      {}, auth.token,
    ),
  )
  if (res) {
    okMsg.value = setuju ? 'Disetujui dan dijalankan.' : 'Ditolak.'
    detail.value = res
    muat(meta.value?.current_page ?? 1)
  }
}

onMounted(() => muat())
</script>

<template>
  <div>
    <div class="mb-4 flex items-center gap-3">
      <div class="flex h-12 w-12 items-center justify-center rounded-xl bg-primary-container text-on-primary">
        <AppIcon name="fact_check" :size="28" />
      </div>
      <h1 class="font-display text-xl font-semibold">Kotak persetujuan</h1>
    </div>
    <AppBanner tone="green" :message="okMsg" />
    <AppBanner tone="red" :message="error" />

    <div class="grid gap-4 lg:grid-cols-2">
      <div>
        <AppCard title="Ajukan baru">
          <AppField label="Aksi">
            <select v-model="form.action" class="w-full rounded-lg border border-outline bg-card px-3 py-2.5 focus:border-primary focus:outline-none">
              <option v-for="a in ACTIONS" :key="a" :value="a">{{ a }}</option>
            </select>
          </AppField>
          <div v-if="NEED_ENTITY.includes(form.action)" class="mt-3">
            <AppField label="ID data (mis. ID nota / PO)">
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
        </AppCard>
      </div>

      <div>
        <div class="mb-3 flex gap-2">
          <AppButton
            v-for="s in ['diajukan', 'disetujui', 'ditolak']" :key="s"
            :variant="status === s ? 'primary' : 'ghost'" @click="status = s; muat()"
          >{{ s }}</AppButton>
        </div>
        <AppEmpty v-if="!loading && !list.length" message="Tidak ada pengajuan." />
        <AppCard v-for="a in list" :key="a.id" class="cursor-pointer transition hover:shadow" @click="lihat(a.id)">
          <div class="flex items-center justify-between gap-2">
            <p class="font-semibold">{{ a.actionLabel }} <span class="font-normal text-secondary">#{{ a.id }}</span></p>
            <AppBadge :tone="tone(a.status)">{{ a.status }}</AppBadge>
          </div>
          <p class="font-mono text-xs text-secondary">{{ a.action }}{{ a.entityId ? ` · data #${a.entityId}` : '' }}</p>
        </AppCard>
        <AppPager :meta="meta" @page="muat" />

        <AppCard v-if="detail" title="Detail pengajuan" class="mt-4">
          <p class="text-sm"><strong>{{ detail.actionLabel }}</strong> · <AppBadge :tone="tone(detail.status)">{{ detail.status }}</AppBadge></p>
          <pre class="mt-2 overflow-x-auto rounded-lg bg-surface-low p-3 text-xs">{{ JSON.stringify(detail.payload, null, 2) }}</pre>
          <p v-if="detail.note" class="mt-2 text-sm text-secondary">Catatan: {{ detail.note }}</p>
          <p v-if="detail.decidedAt" class="text-xs text-secondary">Diputus {{ tanggal(detail.decidedAt) }}</p>
          <div v-if="detail.status === 'diajukan'" class="mt-3 flex gap-2">
            <AppButton :loading="loading" @click="putuskan(true)">Setujui & jalankan</AppButton>
            <AppButton variant="danger" :loading="loading" @click="putuskan(false)">Tolak</AppButton>
          </div>
        </AppCard>
      </div>
    </div>
  </div>
</template>
