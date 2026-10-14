<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { api } from './lib/api'
import { useAuthStore } from './stores/auth'
import { useAccessStore } from './stores/access'
import AppIcon from './components/ui/AppIcon.vue'

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

const route = useRoute()
const auth = useAuthStore()
const access = useAccessStore()
const companyId = computed(() => Number(route.params.companyId ?? auth.companyId ?? 0))
const unread = ref(0)
const jam = ref('')
const buka = ref<Record<string, boolean>>({})
let timer: number | undefined

function tick() {
  jam.value = new Date().toLocaleTimeString('id-ID', { hour: '2-digit', minute: '2-digit' })
}

async function keluar() {
  await auth.logout()
  access.reset()
  window.location.href = '/login'
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
      items: [{ to: '/admin/tenants', label: 'Perusahaan', icon: 'domain' }],
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
      .map((g) => ({
        ...g,
        items: g.items.filter((m) => !m.perms || !access.loaded || access.can(...m.perms)),
      }))
      .filter((g) => g.items.length > 0),
  ]
})

function aktifDiGrup(g: Group) {
  return g.items.some((m) => route.path === m.to || route.path.startsWith(m.to + '/'))
}

watch(
  companyId,
  (id) => {
    if (id) {
      void access.load(id, auth.token)
      for (const g of groups.value) {
        if (!(g.title in buka.value)) buka.value[g.title] = true
      }
    }
  },
  { immediate: true },
)

watch(
  () => route.path,
  () => {
    for (const g of groups.value) {
      if (aktifDiGrup(g)) buka.value[g.title] = true
    }
  },
)

async function muatBadge() {
  if (!auth.token) return
  try {
    const c = await api.get<{ unread: number }>('/notifications/unread-count', auth.token)
    unread.value = c.unread
  } catch {
    /* badge opsional */
  }
}

onMounted(() => {
  tick()
  timer = window.setInterval(tick, 15000)
  muatBadge()
})
onUnmounted(() => window.clearInterval(timer))
</script>

<template>
  <div v-if="!auth.loggedIn"><RouterView /></div>
  <div v-else class="flex min-h-screen">
    <nav
      class="sticky top-0 flex h-screen w-60 shrink-0 flex-col gap-0.5 overflow-y-auto bg-primary p-3 text-white max-md:fixed max-md:inset-x-0 max-md:bottom-0 max-md:top-auto max-md:h-auto max-md:flex-row max-md:overflow-x-auto max-md:p-2"
    >
      <div class="px-2 pb-3 pt-2 max-md:hidden">
        <p class="font-display text-lg font-bold">Aurevant PKK</p>
        <p class="text-xs text-emerald-50/75">Operasional Multi-Cabang</p>
      </div>
      <template v-for="gr in groups" :key="gr.title">
        <div class="max-md:hidden">
          <button
            class="flex w-full items-center justify-between rounded-lg px-3 py-2 text-[11px] font-semibold uppercase tracking-wider text-emerald-50/60 hover:bg-white/5"
            :aria-expanded="buka[gr.title] ?? true"
            @click="buka[gr.title] = !(buka[gr.title] ?? true)"
          >
            {{ gr.title }}
            <AppIcon
              name="expand_more"
              :size="16"
              class="transition-transform duration-200 motion-reduce:transition-none"
              :class="(buka[gr.title] ?? true) ? '' : '-rotate-90'"
            />
          </button>
          <div
            class="grid transition-[grid-template-rows] duration-200 ease-in-out motion-reduce:transition-none"
            :class="
              (buka[gr.title] ?? true) ? '[grid-template-rows:1fr]' : '[grid-template-rows:0fr]'
            "
          >
            <div class="overflow-hidden">
              <RouterLink
                v-for="m in gr.items"
                :key="m.to"
                :to="m.to"
                class="mb-0.5 flex items-center gap-3 rounded-lg px-3 py-2 text-sm text-emerald-50/90 transition-colors hover:bg-white/10 hover:text-white"
                active-class="bg-white/15! text-white!"
                ><AppIcon :name="m.icon" :size="20" />{{ m.label }}
                <span
                  v-if="m.label === 'Notifikasi' && unread"
                  class="ml-auto rounded-full bg-danger px-1.5 text-[10px] font-bold text-white"
                  >{{ unread }}</span
                >
              </RouterLink>
            </div>
          </div>
        </div>
        <template v-for="m in gr.items" :key="m.to">
          <RouterLink
            :to="m.to"
            class="hidden items-center gap-3 whitespace-nowrap rounded-lg px-3 py-2 text-sm text-emerald-50/90 hover:bg-white/10 max-md:flex"
            active-class="bg-white/15! text-white!"
            ><AppIcon :name="m.icon" :size="20" />{{ m.label }}</RouterLink
          >
        </template>
      </template>
      <div class="flex-1 max-md:hidden" />
      <RouterLink
        to="/companies"
        class="flex items-center gap-3 rounded-lg px-3 py-2 text-sm text-emerald-50/90 hover:bg-white/10 max-md:whitespace-nowrap"
      >
        <AppIcon name="domain" :size="20" /><span class="max-md:hidden">Ganti Perusahaan</span>
      </RouterLink>
      <a
        href="#"
        class="flex items-center gap-3 rounded-lg px-3 py-2 text-sm text-emerald-50/90 hover:bg-white/10 max-md:whitespace-nowrap"
        @click.prevent="keluar"
      >
        <AppIcon name="logout" :size="20" /><span class="max-md:hidden">Keluar</span>
      </a>
    </nav>
    <div class="flex min-w-0 flex-1 flex-col">
      <header
        class="sticky top-0 z-10 flex items-center justify-between gap-2 border-b border-outline/60 bg-card px-4 py-2.5 md:px-6"
      >
        <strong class="font-display truncate">{{
          auth.companyName ?? 'Perusahaan #' + (auth.companyId ?? '-')
        }}</strong>
        <div class="flex items-center gap-3 text-sm text-secondary">
          <span class="font-mono text-xs max-sm:hidden">{{ jam }}</span>
          <RouterLink
            :to="`/c/${companyId}/notifications`"
            class="relative rounded-full p-1.5 hover:bg-surface-low"
            aria-label="Notifikasi"
          >
            <AppIcon name="notifications" :size="22" />
            <span
              v-if="unread"
              class="absolute -right-0.5 -top-0.5 rounded-full bg-danger px-1 text-[10px] font-bold text-white"
              >{{ unread }}</span
            >
          </RouterLink>
          <span
            class="flex h-8 w-8 items-center justify-center rounded-full bg-primary-container text-sm font-bold text-on-primary"
          >
            {{ (auth.user?.name ?? '?').charAt(0).toUpperCase() }}
          </span>
        </div>
      </header>
      <main class="min-w-0 flex-1 p-4 md:p-6 max-md:pb-24">
        <RouterView :key="$route.fullPath" />
      </main>
      <footer
        class="flex flex-col items-center justify-between gap-1 border-t border-outline/60 bg-card px-4 py-2 text-xs text-secondary sm:flex-row md:px-6"
      >
        <span>Aurevant PKK · operasional multi-cabang</span>
        <span>{{ auth.user?.name }}</span>
      </footer>
    </div>
  </div>
</template>
