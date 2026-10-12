<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { api, type PageMeta } from '../lib/api'
import { useCompany } from '../composables/useCompany'
import { useAsync } from '../composables/useAsync'
import AppCard from '../components/ui/AppCard.vue'
import AppBanner from '../components/ui/AppBanner.vue'
import AppButton from '../components/ui/AppButton.vue'
import AppField from '../components/ui/AppField.vue'
import AppEmpty from '../components/ui/AppEmpty.vue'
import AppPager from '../components/ui/AppPager.vue'
import AppIcon from '../components/ui/AppIcon.vue'

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
const { loading, error, run } = useAsync()
const list = ref<Baris[]>([])
const meta = ref<PageMeta | null>(null)
const aksi = ref('')
const tipe = ref('')
const cariId = ref('')
const detail = ref<Baris | null>(null)

const tersaring = computed(() => {
  const q = cariId.value.trim()
  if (!q) return list.value
  return list.value.filter((r) => String(r.entityId ?? '').includes(q) || String(r.id) === q)
})

function waktu(iso: string) {
  return new Date(iso).toLocaleString('id-ID', { day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit' })
}

async function muat(page = 1) {
  const q = `?per_page=15&page=${page}` +
    (aksi.value.trim() ? `&action=${encodeURIComponent(aksi.value.trim())}` : '') +
    (tipe.value.trim() ? `&entity_type=${encodeURIComponent(tipe.value.trim())}` : '')
  const res = await run(() => api.get<{ data: Baris[]; meta: PageMeta }>(`/companies/${companyId.value}/audit-logs${q}`, auth.token))
  if (res) {
    const raw = res as unknown as { data: Baris[]; meta?: PageMeta } | Baris[]
    if (Array.isArray(raw)) list.value = raw
    else { list.value = raw.data; meta.value = raw.meta ?? null }
    if (detail.value && !tersaring.value.some((r) => r.id === detail.value!.id)) detail.value = null
  }
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
    <p class="text-xs font-semibold uppercase tracking-wider text-secondary">Jejak audit append-only</p>
    <h1 class="font-display mb-4 text-xl font-semibold">Audit log forensik</h1>
    <div class="mb-4 flex items-start gap-2 rounded-xl bg-amber-100 p-3 text-sm text-amber-900">
      <AppIcon name="warning" :size="20" />
      <p>Log tidak bisa diubah maupun dihapus. Setiap aksi penting tercatat otomatis beserta sebelum-sesudahnya.</p>
    </div>
    <AppBanner tone="red" :message="error" />

    <div class="grid gap-4 xl:grid-cols-5">
      <div class="xl:col-span-3">
        <AppCard>
          <div class="grid gap-2 md:grid-cols-4">
            <AppField label="Aksi (cth. sale.void)">
              <input v-model="aksi" class="w-full rounded-lg border border-outline bg-card px-3 py-2 font-mono text-sm focus:border-primary focus:outline-none" placeholder="sale.void" @keyup.enter="muat(1)" />
            </AppField>
            <AppField label="Tipe entitas (cth. sale)">
              <input v-model="tipe" class="w-full rounded-lg border border-outline bg-card px-3 py-2 font-mono text-sm focus:border-primary focus:outline-none" placeholder="sale" @keyup.enter="muat(1)" />
            </AppField>
            <AppField label="Cari ID">
              <input v-model="cariId" class="w-full rounded-lg border border-outline bg-card px-3 py-2 font-mono text-sm focus:border-primary focus:outline-none" placeholder="ID baris / entitas" />
            </AppField>
            <div class="flex items-end gap-2">
              <AppButton :loading="loading" @click="muat(1)">Saring</AppButton>
              <AppButton variant="ghost" @click="aturUlang">Reset</AppButton>
            </div>
          </div>
        </AppCard>

        <AppEmpty v-if="!loading && !tersaring.length" message="Tidak ada baris cocok. Ubah filter." />
        <div class="mt-3 overflow-x-auto rounded-xl bg-card shadow-sm">
          <table class="w-full min-w-[640px] text-sm">
            <thead>
              <tr class="text-left text-xs uppercase text-secondary">
                <th class="px-4 py-2">Waktu</th><th class="px-4 py-2">Pelaku</th>
                <th class="px-4 py-2">Aksi</th><th class="px-4 py-2">Entitas</th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="r in tersaring" :key="r.id"
                class="cursor-pointer border-t border-surface-low hover:bg-surface-low/60"
                :class="detail?.id === r.id ? 'bg-surface-low' : ''"
                @click="detail = r"
              >
                <td class="whitespace-nowrap px-4 py-2">{{ waktu(r.createdAt) }}</td>
                <td class="px-4 py-2">{{ r.userName ?? (r.userId ? `ID ${r.userId}` : 'Sistem') }}</td>
                <td class="px-4 py-2 font-mono text-xs">{{ r.action }}</td>
                <td class="px-4 py-2 font-mono text-xs">{{ r.entityType }}{{ r.entityId !== null ? ` #${r.entityId}` : '' }}</td>
              </tr>
            </tbody>
          </table>
        </div>
        <AppPager :meta="meta" @page="muat" />
      </div>

      <div class="xl:col-span-2">
        <AppCard v-if="detail" title="Inspeksi rinci">
          <dl class="space-y-1.5 text-sm">
            <div class="flex justify-between"><dt class="text-secondary">Aksi terpilih</dt><dd class="font-mono text-xs font-semibold">{{ detail.action }}</dd></div>
            <div class="flex justify-between"><dt class="text-secondary">ID entitas</dt><dd class="font-mono text-xs">{{ detail.entityType }} #{{ detail.entityId ?? '–' }}</dd></div>
            <div class="flex justify-between"><dt class="text-secondary">Pelaku</dt><dd class="font-semibold">{{ detail.userName ?? 'Sistem' }}</dd></div>
            <div class="flex justify-between"><dt class="text-secondary">Cabang</dt><dd>{{ detail.branchId ? `#${detail.branchId}` : 'Pusat' }}</dd></div>
            <div class="flex justify-between"><dt class="text-secondary">IP</dt><dd class="font-mono text-xs">{{ detail.ip ?? '–' }}</dd></div>
          </dl>
          <div class="mt-3 grid grid-cols-2 gap-2">
            <div>
              <div class="mb-1 flex items-center justify-between">
                <p class="text-xs font-semibold uppercase tracking-wider text-secondary">Sebelum</p>
                <button class="text-xs font-semibold text-primary" @click="salin(detail.before)">Salin</button>
              </div>
              <pre class="overflow-x-auto rounded-lg bg-ink p-3 font-mono text-[11px] leading-relaxed text-emerald-50">{{ JSON.stringify(detail.before, null, 2) }}</pre>
            </div>
            <div>
              <div class="mb-1 flex items-center justify-between">
                <p class="text-xs font-semibold uppercase tracking-wider text-secondary">Sesudah</p>
                <button class="text-xs font-semibold text-primary" @click="salin(detail.after)">Salin</button>
              </div>
              <pre class="overflow-x-auto rounded-lg bg-ink p-3 font-mono text-[11px] leading-relaxed text-emerald-50">{{ JSON.stringify(detail.after, null, 2) }}</pre>
            </div>
          </div>
        </AppCard>
        <AppEmpty v-else message="Klik salah satu baris untuk inspeksi before → after." />
      </div>
    </div>
  </div>
</template>
