<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '../lib/api'
import { waktuRelatif } from '../lib/format'
import { useCompany } from '../composables/useCompany'
import { useAsync } from '../composables/useAsync'
import { usePagedList } from '../composables/usePagedList'
import AppCard from '../components/ui/AppCard.vue'
import AppBadge from '../components/ui/AppBadge.vue'
import AppBanner from '../components/ui/AppBanner.vue'
import AppButton from '../components/ui/AppButton.vue'
import AppEmpty from '../components/ui/AppEmpty.vue'
import AppIcon from '../components/ui/AppIcon.vue'
import AppPageHeader from '../components/ui/AppPageHeader.vue'
import AppPager from '../components/ui/AppPager.vue'
import AppSkeleton from '../components/ui/AppSkeleton.vue'
import AppStat from '../components/ui/AppStat.vue'
import AppTabs from '../components/ui/AppTabs.vue'

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
  'po.retur': {
    label: 'Retur supplier',
    icon: 'assignment_return',
    hint: 'Barang dikembalikan ke supplier',
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
  'stok.menipis': {
    label: 'Stok menipis',
    icon: 'inventory_2',
    hint: 'Saldo mencapai batas minimum',
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
  'cuti.diajukan': { label: 'Pengajuan cuti', icon: 'event_note', hint: 'Menunggu persetujuan' },
  'cuti.disetujui': { label: 'Cuti disetujui', icon: 'event_available', hint: 'Pengajuan diterima' },
  'cuti.ditolak': { label: 'Cuti ditolak', icon: 'event_busy', hint: 'Pengajuan ditolak' },
  'lembur.diajukan': { label: 'Pengajuan lembur', icon: 'more_time', hint: 'Menunggu persetujuan' },
  'lembur.disetujui': { label: 'Lembur disetujui', icon: 'task_alt', hint: 'Lembur disetujui' },
  'lembur.ditolak': { label: 'Lembur ditolak', icon: 'hourglass_disabled', hint: 'Lembur ditolak' },
  'tukar_shift.diajukan': { label: 'Tukar shift', icon: 'swap_horiz', hint: 'Pengajuan pertukaran jadwal' },
  'tukar_shift.disetujui': { label: 'Tukar shift disetujui', icon: 'swap_horiz', hint: 'Jadwal sudah ditukar' },
  'tukar_shift.ditolak': { label: 'Tukar shift ditolak', icon: 'swap_horiz', hint: 'Pertukaran dibatalkan' },
  'biaya.diajukan': { label: 'Biaya diajukan', icon: 'receipt', hint: 'Biaya menunggu persetujuan' },
  'biaya.disetujui': { label: 'Biaya disetujui', icon: 'receipt_long', hint: 'Biaya disetujui' },
  'biaya.ditolak': { label: 'Biaya ditolak', icon: 'receipt', hint: 'Biaya ditolak' },
  'payroll.dibayar': { label: 'Gaji dibayar', icon: 'payments', hint: 'Payroll sudah dibayarkan' },
  'rekonsiliasi.selesai': { label: 'Rekonsiliasi selesai', icon: 'account_balance', hint: 'Rekonsiliasi bank ditutup' },
  'langganan.jatuh_tempo': {
    label: 'Langganan',
    icon: 'workspace_premium',
    hint: 'Masa aktif langganan / trial',
  },
}

const GROUPS: { title: string; types: string[] }[] = [
  {
    title: 'Logistik & perpindahan stok',
    types: [
      'transfer.diajukan',
      'transfer.disetujui',
      'transfer.dikirim',
      'transfer.diterima',
      'stok.menipis',
    ],
  },
  { title: 'Pengadaan barang (PO)', types: ['po.diajukan', 'po.disetujui', 'po.diterima', 'po.retur'] },
  {
    title: 'Penjualan, shift & otorisasi',
    types: ['sale.void', 'sale.retur', 'shift.selisih', 'opname.disetujui', 'approval.diajukan'],
  },
  {
    title: 'Kepegawaian',
    types: [
      'cuti.diajukan',
      'cuti.disetujui',
      'cuti.ditolak',
      'lembur.diajukan',
      'lembur.disetujui',
      'lembur.ditolak',
      'tukar_shift.diajukan',
      'tukar_shift.disetujui',
      'tukar_shift.ditolak',
    ],
  },
  {
    title: 'Keuangan & penggajian',
    types: [
      'biaya.diajukan',
      'biaya.disetujui',
      'biaya.ditolak',
      'rekonsiliasi.selesai',
      'payroll.dibayar',
    ],
  },
  { title: 'Langganan', types: ['langganan.jatuh_tempo'] },
]

const router = useRouter()
const { auth, companyId } = useCompany()
const { error: errorMutasi, run } = useAsync()
const unread = ref(0)
const tab = ref<'semua' | 'belum'>('semua')
const {
  rows: list,
  meta,
  loading,
  error: errorList,
  load: muatList,
  setPerPage,
} = usePagedList<Notif>(
  (page, perPage) =>
    api.getPage<Notif>(
      `/notifications?per_page=${perPage}&page=${page}&company_id=${companyId.value}${tab.value === 'belum' ? '&unread=1' : ''}`,
      auth.token,
    ),
  { perPage: 15 },
)
const error = computed(() => errorList.value || errorMutasi.value)
const prefs = ref<Prefs[]>([])
const prefsLoading = ref(true)
const showPrefs = ref(false)
const perangkat = ref<{ token: string; platform: string | null }[]>([])
const perangkatLoading = ref(true)
let timer: number | undefined

const tabItems = computed(() => [
  { key: 'semua', label: 'Semua' },
  { key: 'belum', label: 'Belum dibaca', count: unread.value },
])

function jenis(t: string) {
  return JENIS[t] ?? { label: 'Pemberitahuan', icon: 'info', hint: '' }
}

function gantiTab(key: string) {
  const next = key === 'belum' ? 'belum' : 'semua'
  if (next === tab.value) return
  tab.value = next
  muat(1)
}

async function muat(page?: number) {
  const [, c] = await Promise.all([
    muatList(page),
    run(() => api.get<{ unread: number }>('/notifications/unread-count', auth.token)),
  ])
  if (c) unread.value = c.unread
}

const TUJUAN: Record<string, string> = {
  '/approvals': 'approvals',
  '/subscription': 'subscription',
  '/stock': 'stock',
  '/leaves': 'leaves',
  '/hr': 'hr',
  '/expenses': 'expenses',
  '/finance': 'finance',
  '/payroll': 'payroll',
  '/slip': 'slip',
  '/profile': 'profile',
}

async function buka(n: Notif) {
  await run(() => api.post(`/notifications/${n.id}/read`, undefined, auth.token))
  const base = n.link?.startsWith('/approvals/') ? '/approvals' : n.link
  const tujuan = base ? TUJUAN[base] : undefined
  if (tujuan) {
    router.push(`/c/${companyId.value}/${tujuan}`)
    return
  }
  muat()
}

async function bacaSemua() {
  await run(() =>
    api.post(`/notifications/read-all?company_id=${companyId.value}`, undefined, auth.token),
  )
  muat(1)
}

async function muatPrefs() {
  prefsLoading.value = true
  try {
    prefs.value = (await run(() => api.get<Prefs[]>('/notification-preferences', auth.token))) ?? []
  } finally {
    prefsLoading.value = false
  }
}

async function muatPerangkat() {
  perangkatLoading.value = true
  try {
    perangkat.value =
      (await run(() =>
        api.get<{ token: string; platform: string | null }[]>('/device-tokens', auth.token),
      )) ?? []
  } finally {
    perangkatLoading.value = false
  }
}

async function cabutPerangkat(token: string) {
  await run(() => api.delBody('/device-tokens', { token }, auth.token))
  if (!errorMutasi.value) muatPerangkat()
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
  timer = window.setInterval(() => muat(), 30000)
})
onUnmounted(() => window.clearInterval(timer))
</script>

<template>
  <div>
    <AppPageHeader
      eyebrow="Monitoring kanal pusat"
      title="Notifikasi & aktivitas bisnis"
      subtitle="Pantau peristiwa operasional penting dan atur kanal yang ingin Anda terima."
      icon="notifications"
    >
      <template #actions>
        <AppButton variant="ghost" @click="bacaSemua">
          <AppIcon name="mark_email_read" :size="18" />Tandai semua dibaca
        </AppButton>
        <AppButton
          variant="secondary"
          :aria-expanded="showPrefs"
          aria-controls="preferensi-notifikasi"
          @click="showPrefs = !showPrefs"
        >
          <AppIcon name="settings" :size="18" />Preferensi
        </AppButton>
      </template>
    </AppPageHeader>

    <AppBanner tone="red" :message="error" />

    <div class="mb-5 grid grid-cols-2 gap-3">
      <AppStat
        label="Belum dibaca"
        :value="unread"
        icon="mark_email_unread"
        :tone="unread ? 'warning' : 'default'"
        hint="Perlu ditinjau"
      />
      <AppStat
        label="Total periode ini"
        :value="meta?.total ?? list.length"
        icon="inbox"
        hint="Sesuai filter aktif"
      />
    </div>

    <div v-if="showPrefs" id="preferensi-notifikasi" class="mb-4">
      <AppCard
        title="Preferensi notifikasi"
        subtitle="Pilih jenis peristiwa yang tetap dikirim ke akun Anda."
      >
        <AppSkeleton v-if="prefsLoading" :lines="4" />
        <div v-else>
          <div v-for="g in GROUPS" :key="g.title" class="mb-4 last:mb-0">
            <p class="mb-1.5 text-2xs font-semibold uppercase tracking-[0.08em] text-secondary">
              {{ g.title }}
            </p>
            <div
              v-for="t in g.types"
              :key="t"
              class="flex items-center justify-between gap-3 border-b border-outline/50 py-2.5 text-sm last:border-0"
            >
              <div class="min-w-0">
                <p class="font-medium text-ink">{{ jenis(t).label }}</p>
                <p class="text-xs text-secondary">{{ jenis(t).hint }}</p>
              </div>
              <AppButton
                v-if="prefOf(t)"
                size="sm"
                variant="ghost"
                :aria-label="`${prefOf(t)!.enabled ? 'Matikan' : 'Nyalakan'} notifikasi ${jenis(t).label}`"
                @click="togglePref(prefOf(t)!)"
              >
                {{ prefOf(t)!.enabled ? 'Matikan' : 'Nyalakan' }}
              </AppButton>
            </div>
          </div>
        </div>
      </AppCard>
    </div>

    <div class="mb-4">
      <AppTabs
        :items="tabItems"
        :model-value="tab"
        label="Filter notifikasi"
        @update:model-value="gantiTab"
      />
    </div>

    <div v-if="loading" class="space-y-3" aria-busy="true">
      <AppCard v-for="i in 3" :key="i">
        <AppSkeleton :lines="3" />
      </AppCard>
      <span class="sr-only" role="status">Memuat notifikasi…</span>
    </div>
    <AppEmpty
      v-else-if="!list.length"
      icon="notifications_off"
      title="Tidak ada notifikasi"
      :message="
        tab === 'belum'
          ? 'Semua notifikasi sudah dibaca.'
          : 'Notifikasi baru akan muncul di sini saat ada aktivitas.'
      "
    />
    <div v-else class="space-y-3">
      <AppCard
        v-for="n in list"
        :key="n.id"
        role="button"
        tabindex="0"
        class="cursor-pointer transition hover:shadow-lifted focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/55 focus-visible:ring-offset-2"
        :aria-label="`${n.title} — ${n.read_at ? 'sudah dibaca' : 'belum dibaca'}`"
        @click="buka(n)"
        @keydown.enter.prevent="buka(n)"
        @keydown.space.prevent="buka(n)"
      >
        <div class="flex items-start gap-3">
          <div
            class="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-primary-soft text-primary-ink"
          >
            <AppIcon :name="jenis(n.type).icon" :size="20" />
          </div>
          <div class="min-w-0 flex-1">
            <div class="flex flex-wrap items-start justify-between gap-2">
              <p class="font-semibold text-ink">{{ n.title }}</p>
              <AppBadge :tone="n.read_at ? 'gray' : 'red'" :dot="!n.read_at">
                {{ n.read_at ? 'Dibaca' : 'Baru' }}
              </AppBadge>
            </div>
            <p v-if="n.body" class="mt-1 text-sm leading-relaxed text-secondary">{{ n.body }}</p>
            <p class="mt-1.5 text-xs text-secondary">
              {{ jenis(n.type).label }} · {{ waktuRelatif(n.created_at) }}
            </p>
          </div>
        </div>
      </AppCard>
    </div>
    <AppPager :meta="meta" @page="muat" @update:per-page="setPerPage" />

    <AppCard
      title="Perangkat terdaftar"
      subtitle="Didaftarkan otomatis dari aplikasi Android saat login."
      class="mt-5"
    >
      <p class="mb-2 text-xs leading-relaxed text-secondary">
        Cabut akses bila ponsel hilang atau berganti agar sesi lama tidak tetap aktif.
      </p>
      <AppSkeleton v-if="perangkatLoading" :lines="2" />
      <AppEmpty
        v-else-if="!perangkat.length"
        icon="phonelink_erase"
        title="Belum ada perangkat"
        message="Perangkat akan tercatat setelah Anda masuk lewat aplikasi Android."
        compact
      />
      <ul v-else class="divide-y divide-outline/60">
        <li
          v-for="d in perangkat"
          :key="d.token"
          class="flex flex-wrap items-center justify-between gap-3 py-3 text-sm"
        >
          <span class="min-w-0 flex-1">
            <span class="font-mono font-medium text-ink">{{ d.platform ?? 'Perangkat' }}</span>
            <span class="block truncate font-mono text-xs text-secondary">{{ d.token }}</span>
          </span>
          <AppButton
            size="sm"
            variant="ghost"
            :aria-label="`Cabut akses perangkat ${d.platform ?? d.token}`"
            @click="cabutPerangkat(d.token)"
          >
            Cabut
          </AppButton>
        </li>
      </ul>
    </AppCard>
  </div>
</template>
