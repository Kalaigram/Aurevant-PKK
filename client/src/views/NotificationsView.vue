<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { api, type PageMeta } from '../lib/api'
import { waktuRelatif } from '../lib/format'
import { useCompany } from '../composables/useCompany'
import { useAsync } from '../composables/useAsync'
import AppCard from '../components/ui/AppCard.vue'
import AppBadge from '../components/ui/AppBadge.vue'
import AppBanner from '../components/ui/AppBanner.vue'
import AppButton from '../components/ui/AppButton.vue'
import AppEmpty from '../components/ui/AppEmpty.vue'
import AppPager from '../components/ui/AppPager.vue'
import AppIcon from '../components/ui/AppIcon.vue'

interface Notif {
  id: number
  type: string
  title: string
  body: string | null
  link: string | null
  read_at: string | null
  created_at?: string
}

interface Prefs {
  type: string
  enabled: boolean
}

const JENIS: Record<string, { label: string; icon: string; hint: string }> = {
  'transfer.diajukan': {
    label: 'Transfer diajukan',
    icon: 'swap_horiz',
    hint: 'Cabang lain memohon stok',
  },
  'transfer.disetujui': {
    label: 'Transfer disetujui',
    icon: 'swap_horiz',
    hint: 'Pengeluaran stok terverifikasi',
  },
  'transfer.dikirim': {
    label: 'Transfer dikirim',
    icon: 'local_shipping',
    hint: 'Barang dalam perjalanan',
  },
  'transfer.diterima': {
    label: 'Transfer diterima',
    icon: 'inventory',
    hint: 'Barang sampai di cabang tujuan',
  },
  'po.diajukan': {
    label: 'PO diajukan',
    icon: 'shopping_cart',
    hint: 'Pesanan pembelian butuh otorisasi',
  },
  'po.disetujui': {
    label: 'PO disetujui',
    icon: 'shopping_cart',
    hint: 'Pesanan diteruskan ke vendor',
  },
  'po.diterima': {
    label: 'PO diterima',
    icon: 'package_2',
    hint: 'Gudang mencatat penerimaan barang',
  },
  'sale.void': { label: 'Nota dibatalkan', icon: 'cancel', hint: 'Pembatalan nota transaksi' },
  'sale.retur': {
    label: 'Ada retur',
    icon: 'assignment_return',
    hint: 'Pengembalian barang pelanggan',
  },
  'opname.disetujui': {
    label: 'Opname disetujui',
    icon: 'fact_check',
    hint: 'Selisih stok disetujui',
  },
  'shift.selisih': {
    label: 'Selisih kas shift',
    icon: 'warning',
    hint: 'Kas fisik beda dengan catatan',
  },
  'approval.diajukan': {
    label: 'Minta persetujuan',
    icon: 'approval',
    hint: 'Perlu respon atasan',
  },
}

const GROUPS: { title: string; types: string[] }[] = [
  {
    title: 'Logistik & perpindahan stok',
    types: ['transfer.diajukan', 'transfer.disetujui', 'transfer.dikirim', 'transfer.diterima'],
  },
  { title: 'Pengadaan barang (PO)', types: ['po.diajukan', 'po.disetujui', 'po.diterima'] },
  {
    title: 'Penjualan, shift & otorisasi',
    types: ['sale.void', 'sale.retur', 'shift.selisih', 'opname.disetujui', 'approval.diajukan'],
  },
]

const router = useRouter()
const { auth, companyId } = useCompany()
const { loading, error, run } = useAsync()
const list = ref<Notif[]>([])
const meta = ref<PageMeta | null>(null)
const unread = ref(0)
const tab = ref<'semua' | 'belum'>('semua')
const prefs = ref<Prefs[]>([])
const showPrefs = ref(false)
const perangkat = ref<{ token: string; platform: string | null }[]>([])
let timer: number | undefined

function jenis(t: string) {
  return JENIS[t] ?? { label: 'Pemberitahuan', icon: 'info', hint: '' }
}

async function muat(page = 1) {
  const q =
    `?per_page=15&page=${page}&company_id=${companyId.value}` +
    (tab.value === 'belum' ? '&unread=1' : '')
  const res = await run(() =>
    api.get<{ data: Notif[]; meta: PageMeta }>(`/notifications${q}`, auth.token),
  )
  if (res) {
    const raw = res as unknown as { data: Notif[]; meta?: PageMeta } | Notif[]
    if (Array.isArray(raw)) list.value = raw
    else {
      list.value = raw.data
      meta.value = raw.meta ?? null
    }
  }
  const c = await run(() => api.get<{ unread: number }>('/notifications/unread-count', auth.token))
  if (c) unread.value = c.unread
}

async function buka(n: Notif) {
  await run(() => api.post(`/notifications/${n.id}/read`, undefined, auth.token))
  if (n.link?.startsWith('/approvals/')) {
    router.push(`/c/${companyId.value}/approvals`)
    return
  }
  muat(meta.value?.current_page ?? 1)
}

async function bacaSemua() {
  await run(() =>
    api.post(`/notifications/read-all?company_id=${companyId.value}`, undefined, auth.token),
  )
  muat(1)
}

async function muatPrefs() {
  prefs.value = (await run(() => api.get<Prefs[]>('/notification-preferences', auth.token))) ?? []
}

async function muatPerangkat() {
  perangkat.value =
    (await run(() =>
      api.get<{ token: string; platform: string | null }[]>('/device-tokens', auth.token),
    )) ?? []
}

async function cabutPerangkat(token: string) {
  await run(() => api.delBody('/device-tokens', { token }, auth.token))
  if (!error.value) muatPerangkat()
}

async function togglePref(p: Prefs) {
  await run(() =>
    api.put('/notification-preferences', { type: p.type, enabled: !p.enabled }, auth.token),
  )
  muatPrefs()
}

function prefOf(type: string): Prefs | undefined {
  return prefs.value.find((p) => p.type === type)
}

onMounted(() => {
  muat()
  muatPrefs()
  muatPerangkat()
  timer = window.setInterval(() => muat(meta.value?.current_page ?? 1), 30000)
})
onUnmounted(() => window.clearInterval(timer))
</script>

<template>
  <div>
    <p class="text-xs font-semibold uppercase tracking-wider text-secondary">
      Monitoring kanal pusat
    </p>
    <div class="mb-4 flex flex-wrap items-center justify-between gap-3">
      <h1 class="font-display text-xl font-semibold">Notifikasi & aktivitas bisnis</h1>
      <div class="flex gap-2">
        <AppButton variant="ghost" @click="bacaSemua">Tandai semua dibaca</AppButton>
        <AppButton variant="ghost" @click="showPrefs = !showPrefs"
          ><AppIcon name="settings" :size="18" />Preferensi</AppButton
        >
      </div>
    </div>
    <AppBanner tone="red" :message="error" />

    <div class="mb-4 grid grid-cols-2 gap-3">
      <AppCard>
        <p class="text-xs uppercase tracking-wider text-secondary">Belum ditindaklanjuti</p>
        <p class="font-display mt-1 text-2xl font-bold">{{ unread }}</p>
      </AppCard>
      <AppCard>
        <p class="text-xs uppercase tracking-wider text-secondary">Total periode ini</p>
        <p class="font-display mt-1 text-2xl font-bold">{{ meta?.total ?? list.length }}</p>
      </AppCard>
    </div>

    <div v-if="showPrefs" class="mb-4">
      <AppCard title="Preferensi notifikasi">
        <div v-for="g in GROUPS" :key="g.title" class="mb-3 last:mb-0">
          <p class="mb-1 text-xs font-semibold uppercase tracking-wider text-secondary">
            {{ g.title }}
          </p>
          <div
            v-for="t in g.types"
            :key="t"
            class="flex items-center justify-between border-b border-surface-low py-2 text-sm last:border-0"
          >
            <div>
              <p class="font-medium">{{ jenis(t).label }}</p>
              <p class="text-xs text-secondary">{{ jenis(t).hint }}</p>
            </div>
            <AppButton v-if="prefOf(t)" variant="ghost" @click="togglePref(prefOf(t)!)">{{
              prefOf(t)!.enabled ? 'Matikan' : 'Nyalakan'
            }}</AppButton>
          </div>
        </div>
      </AppCard>
    </div>

    <div class="mb-3 flex gap-2">
      <AppButton
        :variant="tab === 'semua' ? 'primary' : 'ghost'"
        @click="
          tab = 'semua'
          muat()
        "
        >Semua</AppButton
      >
      <AppButton
        :variant="tab === 'belum' ? 'primary' : 'ghost'"
        @click="
          tab = 'belum'
          muat()
        "
        >Belum dibaca</AppButton
      >
    </div>
    <p v-if="loading" class="text-sm text-secondary">Memuat…</p>
    <AppEmpty v-else-if="!list.length" message="Tidak ada notifikasi." />
    <AppCard
      v-for="n in list"
      :key="n.id"
      class="cursor-pointer transition hover:shadow"
      @click="buka(n)"
    >
      <div class="flex items-start gap-3">
        <div
          class="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg bg-surface-low text-primary"
        >
          <AppIcon :name="jenis(n.type).icon" :size="20" />
        </div>
        <div class="min-w-0 flex-1">
          <div class="flex items-center justify-between gap-2">
            <p class="font-semibold">{{ n.title }}</p>
            <AppBadge :tone="n.read_at ? 'gray' : 'red'">{{
              n.read_at ? 'Dibaca' : 'Baru'
            }}</AppBadge>
          </div>
          <p class="mt-0.5 text-sm text-secondary">{{ n.body }}</p>
          <p class="mt-1 text-xs text-secondary">
            {{ jenis(n.type).label }} · {{ waktuRelatif(n.created_at) }}
          </p>
        </div>
      </div>
    </AppCard>
    <AppPager :meta="meta" @page="muat" />

    <AppCard title="Perangkat terdaftar" class="mt-4">
      <p class="text-xs text-secondary">
        Perangkat didaftarkan otomatis dari aplikasi Android saat login. Cabut akses bila HP hilang
        atau berganti.
      </p>
      <AppEmpty v-if="!perangkat.length" message="Belum ada perangkat terdaftar." />
      <ul v-else class="mt-2 divide-y divide-outline/60">
        <li
          v-for="d in perangkat"
          :key="d.token"
          class="flex items-center justify-between gap-2 py-2 text-sm"
        >
          <span class="min-w-0">
            <span class="font-mono">{{ d.platform ?? 'perangkat' }}</span>
            <span class="block truncate font-mono text-xs text-secondary">{{ d.token }}</span>
          </span>
          <AppButton variant="ghost" @click="cabutPerangkat(d.token)">Cabut</AppButton>
        </li>
      </ul>
    </AppCard>
  </div>
</template>
