<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { api } from '../lib/api'
import { diffFields, formatValue, labelAksi, labelEntitas } from '../lib/labels'
import { useCompany } from '../composables/useCompany'
import { usePagedList } from '../composables/usePagedList'
import AppBadge from '../components/ui/AppBadge.vue'
import AppBanner from '../components/ui/AppBanner.vue'
import AppButton from '../components/ui/AppButton.vue'
import AppCard from '../components/ui/AppCard.vue'
import AppEmpty from '../components/ui/AppEmpty.vue'
import AppField from '../components/ui/AppField.vue'
import AppIcon from '../components/ui/AppIcon.vue'
import AppInput from '../components/ui/AppInput.vue'
import AppPageHeader from '../components/ui/AppPageHeader.vue'
import AppPager from '../components/ui/AppPager.vue'
import AppTable from '../components/ui/AppTable.vue'

interface Baris {
  id: number
  branchId: number | null
  userId: number | null
  userName: string | null
  action: string
  entityType: string
  entityId: number | string | null
  before: Record<string, unknown> | null
  after: Record<string, unknown> | null
  ip: string | null
  createdAt: string
}

const { auth, companyId } = useCompany()
const aksi = ref('')
const tipe = ref('')
const cariId = ref('')
const detail = ref<Baris | null>(null)
const tampilMentah = ref(false)
const {
  rows: list,
  meta,
  loading,
  error,
  load: muat,
  setPage,
  setPerPage,
} = usePagedList<Baris>(
  (page, perPage) => {
    const q =
      `per_page=${perPage}&page=${page}` +
      (aksi.value.trim() ? `&action=${encodeURIComponent(aksi.value.trim())}` : '') +
      (tipe.value.trim() ? `&entity_type=${encodeURIComponent(tipe.value.trim())}` : '')
    return api.getPage<Baris>(`/companies/${companyId.value}/audit-logs?${q}`, auth.token)
  },
  { perPage: 15 },
)

const tersaring = computed(() => {
  const q = cariId.value.trim()
  if (!q) return list.value
  return list.value.filter(
    (r) => String(r.entityId ?? '').includes(q) || String(r.id) === q,
  )
})
const diff = computed(() => diffFields(detail.value?.before ?? null, detail.value?.after ?? null))

watch(list, () => {
  if (detail.value && !tersaring.value.some((r) => r.id === detail.value!.id)) {
    detail.value = null
  }
})

function waktu(iso: string) {
  return new Date(iso).toLocaleString('id-ID', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}

function pilih(row: Baris) {
  detail.value = row
  tampilMentah.value = false
}

function aturUlang() {
  aksi.value = ''
  tipe.value = ''
  cariId.value = ''
  detail.value = null
  muat(1)
}

async function salin(obj: unknown) {
  await navigator.clipboard.writeText(JSON.stringify(obj, null, 2))
}

onMounted(() => muat())
</script>

<template>
  <div>
    <AppPageHeader
      eyebrow="Jejak audit append-only"
      title="Audit log forensik"
      subtitle="Setiap aksi penting tercatat otomatis beserta perubahan sebelum dan sesudahnya."
      icon="policy"
    />

    <div class="mb-4 flex items-start gap-2 rounded-xl border border-warn/20 bg-warn-bg p-3 text-sm text-warn">
      <AppIcon name="warning" :size="20" />
      <p>Log tidak bisa diubah maupun dihapus. Gunakan filter untuk menelusuri aktivitas tertentu.</p>
    </div>
    <AppBanner tone="red" :message="error" />

    <div class="grid gap-4 xl:grid-cols-5">
      <div class="xl:col-span-3">
        <AppCard>
          <div class="grid gap-2 md:grid-cols-4">
            <AppField label="Aksi (cth. sale.void)">
              <AppInput
                v-model="aksi"
                class="font-mono"
                placeholder="sale.void"
                @keyup.enter="muat(1)"
              />
            </AppField>
            <AppField label="Tipe entitas (cth. sale)">
              <AppInput
                v-model="tipe"
                class="font-mono"
                placeholder="sale"
                @keyup.enter="muat(1)"
              />
            </AppField>
            <AppField label="Cari ID">
              <AppInput v-model="cariId" class="font-mono" placeholder="ID baris / entitas" />
            </AppField>
            <div class="flex items-end gap-2">
              <AppButton :loading="loading" @click="muat(1)">Saring</AppButton>
              <AppButton variant="ghost" @click="aturUlang">Reset</AppButton>
            </div>
          </div>
        </AppCard>

        <AppTable
          class="mt-3"
          :loading="loading"
          :empty="!tersaring.length"
          :colspan="6"
          empty-title="Tidak ada baris cocok"
          empty-message="Ubah kata kunci atau reset filter untuk melihat aktivitas lain."
        >
          <template #head>
            <tr>
              <th>Waktu</th>
              <th>Pelaku</th>
              <th>Aksi</th>
              <th>Entitas</th>
              <th class="text-right">ID</th>
              <th class="text-right">Aksi</th>
            </tr>
          </template>
          <tr
            v-for="r in tersaring"
            :key="r.id"
            class="transition hover:bg-surface-low/70"
            :class="detail?.id === r.id ? 'bg-surface-low' : ''"
          >
            <td class="whitespace-nowrap">{{ waktu(r.createdAt) }}</td>
            <td>{{ r.userName ?? (r.userId ? `ID ${r.userId}` : 'Sistem') }}</td>
            <td>
              <p class="text-sm font-medium">{{ labelAksi(r.action) }}</p>
              <p class="font-mono text-2xs text-secondary">{{ r.action }}</p>
            </td>
            <td>
              <p class="text-sm">{{ labelEntitas(r.entityType) }}</p>
              <p class="font-mono text-2xs text-secondary">
                {{ r.entityType }}{{ r.entityId !== null ? ` #${r.entityId}` : '' }}
              </p>
            </td>
            <td class="text-right font-mono text-xs">#{{ r.id }}</td>
            <td class="text-right">
              <AppButton
                size="sm"
                variant="ghost"
                :aria-label="`Inspeksi audit ${r.action} #${r.id}`"
                @click="pilih(r)"
              >
                Inspeksi
              </AppButton>
            </td>
          </tr>
        </AppTable>
        <AppPager :meta="meta" @page="setPage" @update:per-page="setPerPage" />
      </div>

      <div class="xl:col-span-2">
        <AppCard v-if="detail" title="Inspeksi rinci">
          <div class="flex items-start justify-between gap-2">
            <div>
              <p class="font-display font-semibold text-ink">{{ labelAksi(detail.action) }}</p>
              <p class="mt-0.5 font-mono text-[11px] text-secondary">{{ detail.action }}</p>
            </div>
            <AppBadge tone="blue">#{{ detail.id }}</AppBadge>
          </div>

          <dl class="mt-3 space-y-1.5 text-sm">
            <div class="flex justify-between gap-3">
              <dt class="text-secondary">Entitas</dt>
              <dd class="text-right font-medium">{{ labelEntitas(detail.entityType) }}</dd>
            </div>
            <div class="flex justify-between gap-3">
              <dt class="text-secondary">ID entitas</dt>
              <dd class="font-mono text-xs">{{ detail.entityType }} #{{ detail.entityId ?? '—' }}</dd>
            </div>
            <div class="flex justify-between gap-3">
              <dt class="text-secondary">Pelaku</dt>
              <dd class="font-medium">{{ detail.userName ?? (detail.userId ? `ID ${detail.userId}` : 'Sistem') }}</dd>
            </div>
            <div class="flex justify-between gap-3">
              <dt class="text-secondary">Cabang</dt>
              <dd>{{ detail.branchId ? `#${detail.branchId}` : 'Pusat' }}</dd>
            </div>
            <div class="flex justify-between gap-3">
              <dt class="text-secondary">Waktu</dt>
              <dd>{{ waktu(detail.createdAt) }}</dd>
            </div>
            <div class="flex justify-between gap-3">
              <dt class="text-secondary">IP</dt>
              <dd class="font-mono text-xs">{{ detail.ip ?? '—' }}</dd>
            </div>
          </dl>

          <div class="mt-4 flex items-center justify-between">
            <p class="text-xs font-semibold uppercase tracking-wider text-secondary">Perubahan data</p>
            <button
              type="button"
              class="rounded px-1 text-xs font-semibold text-primary focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/55"
              :aria-pressed="tampilMentah"
              @click="tampilMentah = !tampilMentah"
            >
              {{ tampilMentah ? 'Ringkas' : 'Mentah' }}
            </button>
          </div>

          <div v-if="tampilMentah" class="mt-2 grid gap-2 sm:grid-cols-2">
            <div>
              <div class="mb-1 flex items-center justify-between">
                <p class="text-2xs font-semibold uppercase tracking-[0.08em] text-secondary">Sebelum</p>
                <button
                  type="button"
                  class="rounded px-1 text-2xs font-semibold text-primary focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/55"
                  aria-label="Salin data sebelum perubahan"
                  @click="salin(detail.before)"
                >
                  Salin
                </button>
              </div>
              <pre class="max-h-64 overflow-auto rounded-xl bg-ink p-3 font-mono text-2xs leading-relaxed text-primary-soft">{{ JSON.stringify(detail.before, null, 2) }}</pre>
            </div>
            <div>
              <div class="mb-1 flex items-center justify-between">
                <p class="text-2xs font-semibold uppercase tracking-[0.08em] text-secondary">Sesudah</p>
                <button
                  type="button"
                  class="rounded px-1 text-2xs font-semibold text-primary focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/55"
                  aria-label="Salin data sesudah perubahan"
                  @click="salin(detail.after)"
                >
                  Salin
                </button>
              </div>
              <pre class="max-h-64 overflow-auto rounded-xl bg-ink p-3 font-mono text-2xs leading-relaxed text-primary-soft">{{ JSON.stringify(detail.after, null, 2) }}</pre>
            </div>
          </div>

          <AppTable v-else class="mt-2" :empty="!diff.length" :colspan="3" empty-message="Tidak ada perubahan nilai pada peristiwa ini.">
            <template #head>
              <tr>
                <th>Field</th>
                <th>Sebelum</th>
                <th>Sesudah</th>
              </tr>
            </template>
            <tr v-for="row in diff" :key="row.key">
              <td>
                <p class="font-medium">{{ row.label }}</p>
                <p class="font-mono text-[10px] text-secondary">{{ row.key }}</p>
              </td>
              <td class="align-top text-secondary">{{ formatValue(row.before) }}</td>
              <td class="align-top font-medium text-primary">{{ formatValue(row.after) }}</td>
            </tr>
          </AppTable>
        </AppCard>
        <AppEmpty v-else message="Klik salah satu baris untuk inspeksi before → after." />
      </div>
    </div>
  </div>
</template>
