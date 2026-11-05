<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { RouterLink, RouterView, useRoute, useRouter } from 'vue-router'
import { api, assetUrl } from './lib/api'
import { useAuthStore } from './stores/auth'
import { useAccessStore } from './stores/access'
import { useConfirm } from './composables/useConfirm'
import AppIcon from './components/ui/AppIcon.vue'
import AppLogo from './components/ui/AppLogo.vue'
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
        item(`${c}/packages`, 'Paket Jasa', 'inventory_2', ['katalog.kelola']),
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
        item(`${c}/finance`, 'Akun & Anggaran', 'account_balance', [
          'keuangan.lihat',
          'biaya.setujui',
          'periode.tutup',
        ]),
        item(`${c}/periods`, 'Tutup Periode', 'lock', ['periode.tutup']),
      ],
    },
    {
      title: 'SDM',
      items: [
        item(`${c}/attendance`, 'Absensi', 'fingerprint'),
        item(`${c}/leaves`, 'Cuti', 'event_note'),
        item(`${c}/hr`, 'Saldo & Lembur', 'more_time'),
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
        item(`${c}/company`, 'Profil Perusahaan', 'storefront', ['pengaturan.kelola']),
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
    class="sr-only z-[110] rounded-xl bg-card px-4 py-2.5 text-sm font-semibold text-primary-ink shadow-float ring-1 ring-outline/60 focus:not-sr-only focus:fixed focus:left-4 focus:top-4 focus:outline-none focus:ring-2 focus:ring-primary/55"
  >
    Lewati ke konten
  </a>

  <div v-if="!auth.loggedIn"><RouterView /></div>

  <div v-else class="app-shell flex min-h-screen">
    <aside
      class="app-sidebar sticky top-0 hidden h-screen w-[17rem] shrink-0 overflow-hidden bg-primary text-white shadow-sidebar md:flex"
    >
      <div class="ambient-glow pointer-events-none absolute inset-0 opacity-60" aria-hidden="true" />
      <div class="relative flex h-full min-h-0 w-full flex-col">
        <div class="flex items-center gap-3 px-5 pb-3 pt-5">
          <RouterLink
            to="/companies"
            class="flex h-11 w-11 shrink-0 items-center justify-center rounded-2xl transition hover:bg-white/10 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-white/70"
            aria-label="Ganti perusahaan"
          >
            <AppLogo size="md" framed />
          </RouterLink>
          <div class="min-w-0">
            <p class="font-display truncate text-[15px] font-bold leading-tight tracking-tight text-white">
              Aurevant PKK
            </p>
            <p class="mt-0.5 truncate text-2xs text-white/70">Operasional multi-cabang</p>
          </div>
        </div>

        <div class="px-4 pb-3">
          <RouterLink
            to="/companies"
            :title="companyLabel"
            class="group flex items-center gap-3 rounded-2xl bg-white/[0.08] p-3 ring-1 ring-white/10 transition hover:bg-white/[0.12] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-white/60"
          >
            <AppAvatar
              :name="companyLabel"
              :src="assetUrl(auth.companyLogoUrl)"
              size="sm"
              square
              class="!ring-white/20" fallback-icon="storefront"
            />
            <div class="min-w-0 flex-1">
              <p class="text-2xs uppercase tracking-[0.1em] text-white/80">Perusahaan aktif</p>
              <p class="truncate text-[13px] font-semibold text-white">{{ companyLabel }}</p>
            </div>
            <AppIcon
              name="unfold_more"
              :size="18"
              class="text-white/70 transition group-hover:text-white"
            />
          </RouterLink>
        </div>

        <nav class="mt-1 min-h-0 flex-1 overflow-y-auto px-3 pb-4" aria-label="Navigasi utama">
          <template v-for="group in groups" :key="group.title">
            <button
              type="button"
              class="mt-3 flex min-h-9 w-full items-center justify-between rounded-lg px-2.5 py-1.5 text-left text-2xs font-semibold uppercase tracking-[0.14em] text-white/70 transition hover:bg-white/5 hover:text-white focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-white/45"
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
              <div class="overflow-hidden" :inert="!isOpen(group.title)">
                <RouterLink
                  v-for="menu in group.items"
                  :key="menu.to"
                  :to="menu.to"
                  class="app-nav-link mb-0.5 flex min-h-10 items-center gap-3 rounded-xl px-3 py-2 text-[13px] font-medium text-white/85 transition-colors hover:bg-white/10 hover:text-white focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-white/55"
                  active-class="router-link-active bg-white/15! text-white! shadow-sm"
                >
                  <AppIcon :name="menu.icon" :size="19" />
                  <span class="truncate">{{ menu.label }}</span>
                  <span
                    v-if="menu.label === 'Notifikasi' && unread"
                    class="ml-auto inline-flex h-5 min-w-5 items-center justify-center rounded-full bg-danger px-1.5 text-3xs font-bold leading-none text-white ring-2 ring-primary"
                  >
                    {{ unread > 99 ? '99+' : unread }}
                  </span>
                </RouterLink>
              </div>
            </div>
          </template>
        </nav>

        <div class="border-t border-white/10 p-3">
          <div class="flex items-center gap-2.5 rounded-2xl bg-white/[0.06] p-2.5 ring-1 ring-white/10">
            <AppAvatar :name="auth.user?.name" :src="assetUrl(auth.user?.avatarUrl)" size="sm" />
            <div class="min-w-0 flex-1">
              <p class="truncate text-[13px] font-semibold text-white">{{ auth.user?.name }}</p>
              <p class="truncate text-2xs text-white/80">{{ auth.user?.email }}</p>
            </div>
            <button
              type="button"
              class="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl text-white/70 transition hover:bg-white/10 hover:text-white focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-white/55"
              aria-label="Keluar dari akun"
              title="Keluar"
              @click="keluar"
            >
              <AppIcon name="logout" :size="19" />
            </button>
          </div>
        </div>
      </div>
    </aside>

    <div class="flex min-w-0 flex-1 flex-col">
      <header
        class="app-header app-glass sticky top-0 z-30 flex min-h-16 items-center justify-between gap-3 border-b border-outline/50 px-4 py-2 md:px-8"
      >
        <div class="flex min-w-0 items-center gap-2.5">
          <button
            type="button"
            class="flex h-11 w-11 items-center justify-center rounded-xl text-ink transition hover:bg-surface-low focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/55 md:hidden"
            aria-label="Buka menu"
            @click="menuOpen = true"
          >
            <AppIcon name="menu" :size="22" />
          </button>
          <RouterLink
            to="/companies"
            class="flex h-9 w-9 shrink-0 items-center justify-center rounded-xl md:hidden"
            aria-label="Ganti perusahaan"
          >
            <AppLogo size="sm" />
          </RouterLink>
          <div class="min-w-0">
            <p class="hidden text-2xs font-semibold uppercase tracking-[0.1em] text-secondary sm:block">
              {{ auth.user?.isSuperAdmin && !companyId ? 'Administrasi platform' : 'Perusahaan aktif' }}
            </p>
            <RouterLink
              to="/companies"
              :title="companyLabel"
              class="block truncate font-display text-[15px] font-semibold text-ink transition hover:text-primary sm:text-base"
            >
              {{ companyLabel }}
            </RouterLink>
          </div>
        </div>

        <div class="flex shrink-0 items-center gap-1 sm:gap-2">
          <span
            class="hidden items-center gap-1.5 rounded-full border border-outline/60 bg-surface-raised/80 px-3 py-1.5 text-2xs text-secondary lg:flex"
          >
            <AppIcon name="calendar_today" :size="14" />
            {{ tanggal }}
          </span>
          <RouterLink
            v-if="companyId"
            :to="`/c/${companyId}/notifications`"
            class="relative flex h-11 w-11 items-center justify-center rounded-xl text-ink transition hover:bg-surface-low focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/55"
            :aria-label="unread ? `Notifikasi, ${unread} belum dibaca` : 'Notifikasi'"
          >
            <AppIcon name="notifications" :size="21" />
            <span
              v-if="unread"
              class="absolute right-1.5 top-1.5 flex h-4 min-w-4 items-center justify-center rounded-full bg-danger px-1 text-3xs font-bold leading-none text-white ring-2 ring-card"
            >
              {{ unread > 99 ? '99+' : unread }}
            </span>
          </RouterLink>
          <AppDropdown :items="userMenu" label="Menu pengguna" align="right">
            <template #trigger>
              <AppAvatar :name="auth.user?.name" :src="assetUrl(auth.user?.avatarUrl)" size="sm" />
            </template>
          </AppDropdown>
        </div>
      </header>

      <main
        id="konten"
        tabindex="-1"
        class="app-main mx-auto w-full max-w-[1560px] min-w-0 flex-1 px-4 py-6 focus-visible:outline-none md:px-8 md:py-8 max-md:pb-32"
      >
        <RouterView v-slot="{ Component, route: currentRoute }">
          <Transition name="page" mode="out-in">
            <component :is="Component" :key="currentRoute.fullPath" />
          </Transition>
        </RouterView>
      </main>

      <footer
        class="app-footer hidden items-center justify-between border-t border-outline/50 bg-surface/60 px-8 py-3 text-2xs text-secondary md:flex"
      >
        <span>Aurevant PKK · operasional multi-cabang</span>
        <span>{{ auth.user?.name }}</span>
      </footer>
    </div>
  </div>

  <nav
    v-if="auth.loggedIn && mobileQuick.length"
    class="app-bottom-nav fixed inset-x-0 bottom-0 z-40 px-3 pb-[max(0.75rem,env(safe-area-inset-bottom))] md:hidden"
    aria-label="Navigasi cepat"
  >
    <div
      class="mx-auto flex max-w-md items-stretch gap-1 rounded-2xl border border-outline/60 bg-card/95 p-1.5 shadow-float backdrop-blur-xl"
    >
      <RouterLink
        v-for="quick in mobileQuick"
        :key="quick.to"
        :to="quick.to"
        class="flex min-h-12 flex-1 flex-col items-center justify-center gap-1 rounded-xl px-1 text-2xs font-medium text-secondary transition focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/55"
        active-class="bg-primary-soft! text-primary-ink!"
      >
        <span class="relative flex">
          <AppIcon :name="quick.icon" :size="21" />
          <span
            v-if="quick.badge"
            class="absolute -right-2.5 -top-1.5 flex h-4 min-w-4 items-center justify-center rounded-full bg-danger px-1 text-3xs font-bold leading-none text-white ring-2 ring-card"
          >
            {{ quick.badge > 99 ? '99+' : quick.badge }}
          </span>
        </span>
        <span class="max-w-full truncate">{{ quick.label }}</span>
      </RouterLink>
      <button
        type="button"
        class="flex min-h-12 flex-1 flex-col items-center justify-center gap-1 rounded-xl px-1 text-2xs font-medium text-secondary transition focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/55"
        :class="menuOpen ? 'bg-primary-soft text-primary-ink' : ''"
        aria-label="Buka menu lengkap"
        aria-haspopup="dialog"
        :aria-expanded="menuOpen"
        @click="menuOpen = true"
      >
        <AppIcon name="menu" :size="21" />
        <span>Menu</span>
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
      <div class="flex items-center gap-3 rounded-2xl border border-outline/60 bg-surface-low/60 p-3.5">
        <AppAvatar :name="auth.user?.name" :src="assetUrl(auth.user?.avatarUrl)" size="md" />
        <div class="min-w-0 flex-1">
          <p class="truncate text-sm font-semibold text-ink">{{ auth.user?.name }}</p>
          <p class="truncate text-xs text-secondary">{{ auth.user?.email }}</p>
          <p v-if="companyId" class="mt-0.5 truncate text-2xs text-secondary">
            {{ companyLabel }}
          </p>
        </div>
        <RouterLink
          to="/companies"
          class="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl border border-outline/70 bg-card text-secondary transition hover:border-outline-strong hover:text-ink focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/55"
          aria-label="Ganti perusahaan"
          @click="menuOpen = false"
        >
          <AppIcon name="domain" :size="19" />
        </RouterLink>
      </div>

      <div v-for="group in groups" :key="group.title">
        <p class="mb-1.5 px-1 text-2xs font-semibold uppercase tracking-[0.1em] text-secondary">
          {{ group.title }}
        </p>
        <div class="grid gap-1">
          <RouterLink
            v-for="menu in group.items"
            :key="menu.to"
            :to="menu.to"
            class="app-mobile-link flex min-h-11 items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-medium text-ink transition hover:bg-surface-low focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/55"
            active-class="router-link-active bg-primary-soft! text-primary-ink!"
            @click="menuOpen = false"
          >
            <span
              class="app-mobile-icon flex h-8 w-8 shrink-0 items-center justify-center rounded-lg bg-surface-low text-secondary"
            >
              <AppIcon :name="menu.icon" :size="19" />
            </span>
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
        <AppButton variant="danger" block @click="menuOpen = false; keluar()">
          <AppIcon name="logout" :size="18" />Keluar
        </AppButton>
      </div>
    </div>
  </AppModal>
</template>
