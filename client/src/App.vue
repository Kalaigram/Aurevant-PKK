<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { api } from './lib/api'
import { useAuthStore } from './stores/auth'
import { useAccessStore } from './stores/access'
import AppIcon from './components/ui/AppIcon.vue'

interface Item { to?: string; label: string; icon: string; perms?: string[]; segera?: boolean }
interface Group { title: string; items: Item[] }

const route = useRoute()
const auth = useAuthStore()
const access = useAccessStore()
const companyId = computed(() => Number(route.params.companyId ?? auth.companyId ?? 0))
const unread = ref(0)
const jam = ref('')
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
  'transaksi.buat', 'transaksi.void', 'transaksi.diskon',
  'retur.buat', 'retur.setujui', 'po.buat', 'po.setujui',
  'stok.transfer', 'stok.opname',
]

const groups = computed<Group[]>(() => {
  const id = companyId.value
  if (!id) return []
  const c = `/c/${id}`
  const semu = (label: string, icon: string, to: string, perms?: string[]): Item => ({ label, icon, to, perms })
  const nanti = (label: string, icon: string): Item => ({ label, icon, segera: true })
  const lihat = (it: Item) => !it.perms || !access.loaded || access.can(...it.perms)
  const g = (title: string, items: Item[]): Group => ({ title, items: items.filter(lihat) })
  return [
    g('Utama', [semu('Dashboard', 'dashboard', `${c}/dashboard`)]),
    g('Operasional & Penjualan', [
      semu('Persetujuan', 'fact_check', `${c}/approvals`, APPROVAL_PERMS),
      nanti('Kasir / POS', 'point_of_sale'), nanti('Nota', 'receipt_long'),
      nanti('Pelanggan', 'group'), nanti('Order Jasa', 'engineering'),
      nanti('Katalog', 'category'), nanti('Stok', 'inventory_2'),
      nanti('Transfer Stok', 'swap_horiz'), nanti('Opname', 'content_paste_search'),
      nanti('Pembelian (PO)', 'shopping_cart'),
    ]),
    g('SDM', [
      semu('Absensi', 'fingerprint', `${c}/attendance`),
      semu('Cuti', 'event_note', `${c}/leaves`),
      semu('Slip Gaji', 'receipt_long', `${c}/slip`),
      semu('Kartu', 'badge', `${c}/card`),
      nanti('Jadwal & Shift', 'schedule'), nanti('Kehadiran', 'how_to_reg'),
      nanti('Penggajian', 'payments'), nanti('Karyawan', 'groups'),
    ]),
    g('Keuangan', [
      nanti('Biaya', 'request_quote'), nanti('Hutang', 'credit_card'),
      nanti('Piutang', 'receipt'), nanti('Jurnal', 'book'),
      nanti('Laporan', 'assessment'), nanti('Tutup Periode', 'lock_clock'),
    ]),
    g('Sistem', [
      semu('Notifikasi', 'notifications', `${c}/notifications`),
      semu('Langganan', 'workspace_premium', `${c}/subscription`),
      semu('Profil', 'account_circle', `${c}/profile`),
      nanti('Role & Akses', 'manage_accounts'), nanti('Audit Log', 'plagiarism'),
      nanti('Impor / Ekspor', 'upload_file'),
    ]),
  ].filter((gr) => gr.items.length > 0)
})

async function muatBadge() {
  if (!auth.token) return
  try {
    const c = await api.get<{ unread: number }>('/notifications/unread-count', auth.token)
    unread.value = c.unread
  } catch { /* badge opsional */ }
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
    <nav class="sticky top-0 flex h-screen w-60 shrink-0 flex-col gap-0.5 overflow-y-auto bg-primary p-3 text-white max-md:fixed max-md:inset-x-0 max-md:bottom-0 max-md:top-auto max-md:h-auto max-md:flex-row max-md:overflow-x-auto max-md:p-2">
      <div class="px-2 pb-3 pt-2 max-md:hidden">
        <p class="font-display text-lg font-bold">Aurevant PKK</p>
        <p class="text-xs text-emerald-50/75">Operasional Multi-Cabang</p>
      </div>
      <template v-for="gr in groups" :key="gr.title">
        <p class="px-3 pb-1 pt-3 text-[11px] font-semibold uppercase tracking-wider text-emerald-50/60 max-md:hidden">{{ gr.title }}</p>
        <template v-for="m in gr.items" :key="m.label">
          <RouterLink
            v-if="m.to" :to="m.to"
            class="flex items-center gap-3 rounded-lg px-3 py-2 text-sm text-emerald-50/90 transition-colors hover:bg-white/10 hover:text-white max-md:whitespace-nowrap"
            active-class="bg-white/15! text-white!"
          ><AppIcon :name="m.icon" :size="20" />{{ m.label }}</RouterLink>
          <span
            v-else
            class="flex cursor-not-allowed items-center gap-3 rounded-lg px-3 py-2 text-sm text-emerald-50/40 max-md:whitespace-nowrap"
            :title="`${m.label} — segera hadir`"
          ><AppIcon :name="m.icon" :size="20" />{{ m.label }}
            <span class="rounded bg-white/10 px-1.5 text-[10px]">Segera</span></span>
        </template>
      </template>
      <div class="flex-1 max-md:hidden" />
      <RouterLink to="/companies" class="flex items-center gap-3 rounded-lg px-3 py-2 text-sm text-emerald-50/90 hover:bg-white/10 max-md:whitespace-nowrap">
        <AppIcon name="domain" :size="20" />Ganti Perusahaan
      </RouterLink>
      <a href="#" class="flex items-center gap-3 rounded-lg px-3 py-2 text-sm text-emerald-50/90 hover:bg-white/10 max-md:whitespace-nowrap" @click.prevent="keluar">
        <AppIcon name="logout" :size="20" />Keluar
      </a>
    </nav>
    <div class="flex min-w-0 flex-1 flex-col">
      <header class="sticky top-0 z-10 flex items-center justify-between gap-2 border-b border-outline/60 bg-card px-4 py-2.5 md:px-6">
        <strong class="font-display truncate">{{ auth.companyName ?? 'Perusahaan #' + (auth.companyId ?? '-') }}</strong>
        <div class="flex items-center gap-3 text-sm text-secondary">
          <span class="font-mono text-xs max-sm:hidden">{{ jam }}</span>
          <RouterLink :to="`/c/${companyId}/notifications`" class="relative rounded-full p-1.5 hover:bg-surface-low" aria-label="Notifikasi">
            <AppIcon name="notifications" :size="22" />
            <span v-if="unread" class="absolute -right-0.5 -top-0.5 rounded-full bg-danger px-1 text-[10px] font-bold text-white">{{ unread }}</span>
          </RouterLink>
          <span class="flex h-8 w-8 items-center justify-center rounded-full bg-primary-container text-sm font-bold text-on-primary">
            {{ (auth.user?.name ?? '?').charAt(0).toUpperCase() }}
          </span>
        </div>
      </header>
      <main class="min-w-0 flex-1 p-4 md:p-6 max-md:pb-24">
        <RouterView :key="$route.fullPath" />
      </main>
      <footer class="flex flex-col items-center justify-between gap-1 border-t border-outline/60 bg-card px-4 py-2 text-xs text-secondary sm:flex-row md:px-6">
        <span>Aurevant PKK · operasional multi-cabang</span>
        <span>{{ auth.user?.name }}</span>
      </footer>
    </div>
  </div>
</template>
