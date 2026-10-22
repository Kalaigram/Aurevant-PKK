<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { RouterLink, RouterView, useRoute, useRouter } from 'vue-router'
import { api } from './lib/api'
import { useAuthStore } from './stores/auth'
import { useAccessStore } from './stores/access'
import { useConfirm } from './composables/useConfirm'
import AppIcon from './components/ui/AppIcon.vue'
import AppAvatar from './components/ui/AppAvatar.vue'
import AppBadge from './components/ui/AppBadge.vue'
import AppButton from './components/ui/AppButton.vue'
import AppConfirm from './components/ui/AppConfirm.vue'
import AppDropdown from './components/ui/AppDropdown.vue'
import AppModal from './components/ui/AppModal.vue'
import AppToast from './components/ui/AppToast.vue'

interface Item {
  to: string
  label: string
  icon: string
  perms?: string[]
}
interface Group {
  title: string
  items: Item[]
}
interface MenuItem {
  label?: string
  icon?: string
  to?: string
  danger?: boolean
  divider?: boolean
  onSelect?: () => void
}
interface QuickItem {
  to: string
  label: string
  icon: string
  badge?: number
}

const NAV_KEY = 'aurevant-nav'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const access = useAccessStore()
const { confirm } = useConfirm()
const companyId = computed(() => Number(route.params.companyId ?? auth.companyId ?? 0))
const unread = ref(0)
const menuOpen = ref(false)
const buka = ref<Record<string, boolean>>(loadNav())
const tanggal = new Date().toLocaleDateString('id-ID', {
  weekday: 'long',
  day: 'numeric',
  month: 'long',
  year: 'numeric',
})
let badgeTimer: number | undefined

function loadNav(): Record<string, boolean> {
  try {
    const raw = localStorage.getItem(NAV_KEY)
    return raw ? (JSON.parse(raw) as Record<string, boolean>) : {}
  } catch {
    return {}
  }
}

function simpanNav() {
  try {
    localStorage.setItem(NAV_KEY, JSON.stringify(buka.value))
  } catch {
    return
  }
}

function isOpen(title: string) {
  return buka.value[title] ?? true
}

function toggleGroup(title: string) {
  buka.value[title] = !isOpen(title)
  simpanNav()
}

async function keluar() {
  const setuju = await confirm({
    title: 'Keluar dari akun?',
    message: 'Sesi di perangkat ini akan diakhiri. Data yang belum disimpan akan hilang.',
    confirmLabel: 'Keluar',
    tone: 'danger',
  })
  if (!setuju) return
  await auth.logout()
  access.reset()
  window.location.href = '/login'
}

function gantiPerusahaan() {
  menuOpen.value = false
  void router.push('/companies')
}

const APPROVAL_PERMS = [
  'transaksi.buat',
  'transaksi.void',
  'transaksi.diskon',
  'retur.buat',
  'retur.setujui',
  'po.buat',
  'po.setujui',
  'stok.transfer',
  'stok.opname',
]

const groups = computed<Group[]>(() => {
  const id = companyId.value
  const all: Group[] = []
  if (auth.user?.isSuperAdmin) {
    all.push({
      title: 'Platform',
      items: [
        { to: '/admin/tenants', label: 'Perusahaan', icon: 'domain' },
        { to: '/admin/plans', label: 'Paket', icon: 'workspace_premium' },
        { to: '/admin/holidays', label: 'Libur Nasional', icon: 'event' },
      ],
    })
  }
  if (!id) return all
  const c = `/c/${id}`
  const item = (to: string, label: string, icon: string, perms?: string[]): Item => ({
    to,
    label,
    icon,
    perms,
  })
  const semua = [
    { title: 'Utama', items: [item(`${c}/dashboard`, 'Dashboard', 'dashboard')] },
    {
      title: 'Operasional',
      items: [
        item(`${c}/approvals`, 'Persetujuan', 'fact_check', APPROVAL_PERMS),
        item(`${c}/pos`, 'Kasir / POS', 'point_of_sale', [
          'transaksi.buat',
          'shift.buka',
          'shift.tutup',
        ]),
        item(`${c}/sales`, 'Nota', 'receipt_long', ['nota.lihat']),
        item(`${c}/customers`, 'Pelanggan', 'group', ['pelanggan.lihat', 'pelanggan.kelola']),
        item(`${c}/services`, 'Order Jasa', 'engineering', ['transaksi.buat', 'jasa.proses']),
        item(`${c}/catalog`, 'Katalog', 'category', ['katalog.kelola', 'harga.atur']),
        item(`${c}/prices`, 'Harga per Cabang', 'sell', ['katalog.kelola', 'harga.atur']),
        item(`${c}/stock`, 'Stok', 'inventory_2', ['stok.transfer', 'stok.opname', 'po.buat']),
        item(`${c}/transfers`, 'Transfer Stok', 'swap_horiz', ['stok.transfer']),
        item(`${c}/opnames`, 'Opname', 'content_paste_search', ['stok.opname']),
        item(`${c}/purchase`, 'Pembelian', 'shopping_cart', ['po.buat', 'po.setujui']),
      ],
    },
    {
      title: 'Keuangan',
      items: [
        item(`${c}/expenses`, 'Biaya', 'receipt', ['biaya.input', 'biaya.setujui']),
        item(`${c}/debts`, 'Hutang', 'arrow_upward', ['biaya.setujui', 'keuangan.lihat']),
        item(`${c}/receivables`, 'Piutang', 'arrow_downward', ['biaya.setujui', 'keuangan.lihat']),
        item(`${c}/journal`, 'Jurnal', 'menu_book', ['keuangan.lihat']),
        item(`${c}/reports`, 'Laporan', 'bar_chart', ['keuangan.lihat', 'laporan.konsolidasi']),
        item(`${c}/periods`, 'Tutup Periode', 'lock', ['periode.tutup']),
      ],
    },
    {
      title: 'SDM',
      items: [
        item(`${c}/attendance`, 'Absensi', 'fingerprint'),
        item(`${c}/leaves`, 'Cuti', 'event_note'),
        item(`${c}/schedule`, 'Jadwal & Shift', 'calendar_month', ['jadwal.lihat', 'shift.kelola']),
        item(`${c}/hr-attendance`, 'Kehadiran Karyawan', 'how_to_reg', [
          'absensi.approve',
          'absensi.koreksi',
        ]),
        item(`${c}/payroll`, 'Penggajian', 'payments', ['gaji.kelola', 'gaji.setujui']),
        item(`${c}/slip`, 'Slip Gaji', 'receipt_long'),
        item(`${c}/card`, 'Kartu', 'badge'),
      ],
    },
    {
      title: 'Sistem',
      items: [
        item(`${c}/notifications`, 'Notifikasi', 'notifications'),
        item(`${c}/subscription`, 'Langganan', 'workspace_premium'),
        item(`${c}/branches`, 'Cabang', 'domain'),
        item(`${c}/roles`, 'Role & Akses', 'manage_accounts', ['pengaturan.kelola']),
        item(`${c}/employees`, 'Karyawan', 'groups', ['karyawan.kelola']),
        item(`${c}/audit`, 'Audit Log', 'plagiarism', ['audit.lihat']),
        item(`${c}/import`, 'Impor & Ekspor', 'upload_file', ['impor.jalankan']),
        item(`${c}/profile`, 'Profil', 'account_circle'),
      ],
    },
  ]
  return [
    ...all,
    ...semua
      .map((group) => ({
        ...group,
        items: group.items.filter(
          (menu) => !menu.perms || !access.loaded || access.can(...menu.perms),
        ),
      }))
      .filter((group) => group.items.length > 0),
  ]
})

const allItems = computed(() => groups.value.flatMap((group) => group.items))

const mobileQuick = computed<QuickItem[]>(() => {
  if (!companyId.value) return []
  const c = `/c/${companyId.value}`
  const items: QuickItem[] = [{ to: `${c}/dashboard`, label: 'Dashboard', icon: 'dashboard' }]
  const kasir = allItems.value.find((menu) => menu.to.endsWith('/pos'))
  if (kasir) items.push({ to: kasir.to, label: 'Kasir', icon: kasir.icon })
  items.push({ to: `${c}/attendance`, label: 'Absensi', icon: 'fingerprint' })
  items.push({
    to: `${c}/notifications`,
    label: 'Notifikasi',
    icon: 'notifications',
    badge: unread.value,
  })
  return items
})

const userMenu = computed<MenuItem[]>(() => {
  const items: MenuItem[] = []
  if (companyId.value) {
    items.push({
      label: 'Profil saya',
      icon: 'account_circle',
      to: `/c/${companyId.value}/profile`,
    })
  }
  items.push({ label: 'Ganti perusahaan', icon: 'domain', to: '/companies' })
  items.push({ divider: true })
  items.push({ label: 'Keluar', icon: 'logout', danger: true, onSelect: () => void keluar() })
  return items
})

const companyLabel = computed(
  () =>
    auth.companyName ??
    (auth.companyId ? `Perusahaan #${auth.companyId}` : auth.user?.isSuperAdmin ? 'Platform' : 'Aurevant PKK'),
)

function aktifDiGrup(group: Group) {
  return group.items.some((menu) => route.path === menu.to || route.path.startsWith(menu.to + '/'))
}

watch(
  companyId,
  (id) => {
    if (id) {
      void access.load(id, auth.token).catch(() => undefined)
      void muatBadge()
    }
  },
  { immediate: true },
)

watch(
  () => route.path,
  () => {
    menuOpen.value = false
    for (const group of groups.value) {
      if (aktifDiGrup(group)) {
        buka.value[group.title] = true
        simpanNav()
      }
    }
  },
)

async function muatBadge() {
  if (!auth.token) return
  try {
    const data = await api.get<{ unread: number }>('/notifications/unread-count', auth.token)
    unread.value = data.unread
  } catch {
    /* badge opsional */
  }
}

onMounted(() => {
  void muatBadge()
  badgeTimer = window.setInterval(muatBadge, 60000)
})

onBeforeUnmount(() => {
  if (badgeTimer !== undefined) window.clearInterval(badgeTimer)
})
</script>

<template>
  <AppToast />
  <AppConfirm />

  <a
    href="#konten"
    class="sr-only z-[110] rounded-lg bg-card px-4 py-2 text-sm font-semibold text-primary shadow-lg focus:not-sr-only focus:fixed focus:left-4 focus:top-4"
  >
    Lewati ke konten
  </a>

  <div v-if="!auth.loggedIn"><RouterView /></div>

  <div v-else class="app-shell flex min-h-screen">
    <aside
      class="app-sidebar sticky top-0 hidden h-screen w-64 shrink-0 flex-col bg-primary text-white md:flex"
    >
      <div class="flex items-center gap-3 px-4 pb-3 pt-5">
        <RouterLink
          to="/companies"
          class="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-white/15 font-display text-lg font-bold tracking-tight text-white ring-1 ring-white/20"
          aria-label="Ganti perusahaan"
        >
          A
        </RouterLink>
        <div class="min-w-0">
          <p class="font-display truncate text-base font-bold leading-tight">Aurevant PKK</p>
          <p class="truncate text-[11px] text-emerald-50/60">Operasional multi-cabang</p>
        </div>
      </div>

      <div class="px-3 pb-2">
        <RouterLink
          to="/companies"
          class="flex items-center gap-3 rounded-xl bg-white/10 p-2.5 ring-1 ring-white/10 transition hover:bg-white/15 focus-visible:ring-2 focus-visible:ring-white/50"
        >
          <AppAvatar :name="companyLabel" size="sm" square class="ring-white/20" />
          <div class="min-w-0 flex-1">
            <p class="text-[10px] uppercase tracking-wider text-emerald-50/55">Perusahaan aktif</p>
            <p class="truncate text-sm font-semibold text-white">{{ companyLabel }}</p>
          </div>
          <AppIcon name="unfold_more" :size="18" class="text-emerald-50/60" />
        </RouterLink>
      </div>

      <nav class="mt-1 flex-1 overflow-y-auto px-3 pb-4" aria-label="Navigasi utama">
        <template v-for="group in groups" :key="group.title">
          <button
            type="button"
            class="mt-2 flex w-full items-center justify-between rounded-lg px-2.5 py-1.5 text-[10px] font-semibold uppercase tracking-wider text-emerald-50/50 transition hover:bg-white/5 hover:text-emerald-50/80"
            :aria-expanded="isOpen(group.title)"
            @click="toggleGroup(group.title)"
          >
            {{ group.title }}
            <AppIcon
              name="expand_more"
              :size="15"
              class="transition-transform duration-200 motion-reduce:transition-none"
              :class="isOpen(group.title) ? '' : '-rotate-90'"
            />
          </button>
          <div
            class="grid transition-[grid-template-rows] duration-200 ease-in-out motion-reduce:transition-none"
            :class="isOpen(group.title) ? '[grid-template-rows:1fr]' : '[grid-template-rows:0fr]'"
          >
            <div class="overflow-hidden">
              <RouterLink
                v-for="menu in group.items"
                :key="menu.to"
                :to="menu.to"
                class="mb-0.5 flex items-center gap-3 rounded-lg px-2.5 py-2 text-[13px] text-emerald-50/85 transition-colors hover:bg-white/10 hover:text-white focus-visible:ring-2 focus-visible:ring-white/40"
                active-class="bg-white/15! text-white!"
              >
                <AppIcon :name="menu.icon" :size="19" />
                <span class="truncate">{{ menu.label }}</span>
                <span
                  v-if="menu.label === 'Notifikasi' && unread"
                  class="ml-auto rounded-full bg-danger px-1.5 py-0.5 text-[10px] font-bold leading-none text-white"
                >
                  {{ unread > 99 ? '99+' : unread }}
                </span>
              </RouterLink>
            </div>
          </div>
        </template>
      </nav>

      <div class="border-t border-white/10 p-3">
        <div class="flex items-center gap-2.5 rounded-xl p-1.5">
          <AppAvatar :name="auth.user?.name" size="sm" />
          <div class="min-w-0 flex-1">
            <p class="truncate text-sm font-semibold text-white">{{ auth.user?.name }}</p>
            <p class="truncate text-[11px] text-emerald-50/55">{{ auth.user?.email }}</p>
          </div>
          <button
            type="button"
            class="rounded-lg p-2 text-emerald-50/70 transition hover:bg-white/10 hover:text-white focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-white/40"
            aria-label="Keluar dari akun"
            title="Keluar"
            @click="keluar"
          >
            <AppIcon name="logout" :size="19" />
          </button>
        </div>
      </div>
    </aside>

    <div class="flex min-w-0 flex-1 flex-col">
      <header
        class="app-header sticky top-0 z-30 flex items-center justify-between gap-3 border-b border-outline/60 bg-card/95 px-4 py-2.5 backdrop-blur md:px-7"
      >
        <div class="flex min-w-0 items-center gap-2.5">
          <button
            type="button"
            class="rounded-lg p-2 text-ink transition hover:bg-surface-low focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/30 md:hidden"
            aria-label="Buka menu"
            @click="menuOpen = true"
          >
            <AppIcon name="menu" :size="22" />
          </button>
          <RouterLink
            to="/companies"
            class="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg bg-primary font-display text-sm font-bold text-white sm:hidden"
            aria-label="Ganti perusahaan"
          >
            A
          </RouterLink>
          <div class="min-w-0">
            <p class="hidden text-[10px] font-semibold uppercase tracking-wider text-secondary sm:block">
              {{ auth.user?.isSuperAdmin && !companyId ? 'Administrasi platform' : 'Perusahaan aktif' }}
            </p>
            <RouterLink
              to="/companies"
              class="block truncate font-display text-sm font-semibold text-ink hover:text-primary sm:text-base"
            >
              {{ companyLabel }}
            </RouterLink>
          </div>
        </div>

        <div class="flex shrink-0 items-center gap-1.5 sm:gap-3">
          <span class="hidden text-xs text-secondary lg:block">{{ tanggal }}</span>
          <RouterLink
            v-if="companyId"
            :to="`/c/${companyId}/notifications`"
            class="relative rounded-lg p-2 text-ink transition hover:bg-surface-low focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/30"
            :aria-label="unread ? `Notifikasi, ${unread} belum dibaca` : 'Notifikasi'"
          >
            <AppIcon name="notifications" :size="21" />
            <span
              v-if="unread"
              class="absolute right-0.5 top-0.5 flex h-4 min-w-4 items-center justify-center rounded-full bg-danger px-1 text-[9px] font-bold leading-none text-white ring-2 ring-card"
            >
              {{ unread > 99 ? '99+' : unread }}
            </span>
          </RouterLink>
          <AppDropdown :items="userMenu" label="Menu pengguna" align="right">
            <template #trigger>
              <AppAvatar :name="auth.user?.name" size="sm" />
            </template>
          </AppDropdown>
        </div>
      </header>

      <main id="konten" tabindex="-1" class="app-main min-w-0 flex-1 px-4 py-5 md:px-8 md:py-7 max-md:pb-28">
        <RouterView :key="$route.fullPath" />
      </main>

      <footer
        class="app-footer hidden items-center justify-between border-t border-outline/60 bg-card px-7 py-2 text-[11px] text-secondary md:flex"
      >
        <span>Aurevant PKK · operasional multi-cabang</span>
        <span>{{ auth.user?.name }}</span>
      </footer>
    </div>
  </div>

  <nav
    v-if="auth.loggedIn && mobileQuick.length"
    class="app-bottom-nav fixed inset-x-0 bottom-0 z-40 border-t border-outline/70 bg-card/95 backdrop-blur md:hidden"
    style="padding-bottom: env(safe-area-inset-bottom)"
    aria-label="Navigasi cepat"
  >
    <div class="mx-auto flex max-w-lg items-stretch px-1">
      <RouterLink
        v-for="quick in mobileQuick"
        :key="quick.to"
        :to="quick.to"
        class="flex flex-1 flex-col items-center gap-0.5 py-2 text-[10px] font-medium text-secondary transition focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/30"
        active-class="text-primary!"
      >
        <span class="relative">
          <AppIcon :name="quick.icon" :size="22" />
          <span
            v-if="quick.badge"
            class="absolute -right-2 -top-1 flex h-3.5 min-w-3.5 items-center justify-center rounded-full bg-danger px-0.5 text-[8px] font-bold leading-none text-white"
          >
            {{ quick.badge > 99 ? '99+' : quick.badge }}
          </span>
        </span>
        {{ quick.label }}
      </RouterLink>
      <button
        type="button"
        class="flex flex-1 flex-col items-center gap-0.5 py-2 text-[10px] font-medium text-secondary transition focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/30"
        :class="menuOpen ? 'text-primary' : ''"
        aria-label="Buka menu lengkap"
        @click="menuOpen = true"
      >
        <AppIcon name="menu" :size="22" />
        Menu
      </button>
    </div>
  </nav>

  <AppModal
    :open="menuOpen"
    title="Menu"
    description="Semua halaman yang bisa Anda akses."
    size="sheet"
    @close="menuOpen = false"
  >
    <div class="space-y-5">
      <div class="flex items-center gap-3 rounded-xl bg-surface-low p-3">
        <AppAvatar :name="auth.user?.name" size="md" />
        <div class="min-w-0">
          <p class="truncate text-sm font-semibold text-ink">{{ auth.user?.name }}</p>
          <p class="truncate text-xs text-secondary">{{ auth.user?.email }}</p>
        </div>
      </div>

      <div v-for="group in groups" :key="group.title">
        <p class="mb-1.5 px-1 text-[10px] font-semibold uppercase tracking-wider text-secondary">
          {{ group.title }}
        </p>
        <div class="grid gap-0.5">
          <RouterLink
            v-for="menu in group.items"
            :key="menu.to"
            :to="menu.to"
            class="flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm text-ink transition hover:bg-surface-low focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/30"
            active-class="bg-primary-soft! text-primary!"
            @click="menuOpen = false"
          >
            <AppIcon :name="menu.icon" :size="20" class="text-secondary" />
            <span class="flex-1">{{ menu.label }}</span>
            <AppBadge v-if="menu.label === 'Notifikasi' && unread" tone="red">
              {{ unread > 99 ? '99+' : unread }}
            </AppBadge>
          </RouterLink>
        </div>
      </div>

      <div class="grid gap-2 border-t border-outline/60 pt-4">
        <AppButton variant="ghost" block @click="gantiPerusahaan">
          <AppIcon name="domain" :size="18" />Ganti perusahaan
        </AppButton>
        <AppButton variant="danger" block @click="keluar">
          <AppIcon name="logout" :size="18" />Keluar
        </AppButton>
      </div>
    </div>
  </AppModal>
</template>
