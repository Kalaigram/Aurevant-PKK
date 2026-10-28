<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { api, downloadFile } from '../lib/api'
import { rupiah, tanggal } from '../lib/format'
import { useCompany } from '../composables/useCompany'
import { useAccessStore } from '../stores/access'
import { useAsync } from '../composables/useAsync'
import { usePagedList } from '../composables/usePagedList'
import { useConfirm } from '../composables/useConfirm'
import { useToast } from '../composables/useToast'
import AppAvatar from '../components/ui/AppAvatar.vue'
import AppBadge from '../components/ui/AppBadge.vue'
import AppBanner from '../components/ui/AppBanner.vue'
import AppButton from '../components/ui/AppButton.vue'
import AppCard from '../components/ui/AppCard.vue'
import AppStat from '../components/ui/AppStat.vue'
import AppField from '../components/ui/AppField.vue'
import AppInput from '../components/ui/AppInput.vue'
import AppModal from '../components/ui/AppModal.vue'
import AppPageHeader from '../components/ui/AppPageHeader.vue'
import AppPager from '../components/ui/AppPager.vue'
import AppSearch from '../components/ui/AppSearch.vue'
import AppTable from '../components/ui/AppTable.vue'
import AppTextarea from '../components/ui/AppTextarea.vue'

interface Pelanggan {
  id: number
  name: string
  phone: string | null
  address: string | null
  notes: string | null
}

interface FormPelanggan {
  name: string
  phone: string
  address: string
  notes: string
}

interface ProfilStats {
  transactions: number
  grossSpend: number
  refunds: number
  netSpend: number
  avgTicket: number
  lastPurchaseAt: string | null
  receivablesOutstanding: number
  receivablesOpen: number
  serviceOrders: number
  activeServiceOrders: number
}

interface RiwayatNota {
  id: number
  number: string
  status: string
  branchName: string
  grandTotal: number
  refund: number
  net: number
  createdAt: string
}

interface RiwayatOrder {
  id: number
  status: string
  branchName: string
  depositAmount: number
  total: number
  scheduledAt: string | null
  createdAt: string
  note: string | null
}

interface RiwayatPiutang {
  id: number
  branchName: string
  amount: number
  paidAmount: number
  remaining: number
  dueAt: string | null
  isPaid: boolean
}

interface ProfilPelanggan {
  customer: Pelanggan
  stats: ProfilStats
  sales: RiwayatNota[]
  serviceOrders: RiwayatOrder[]
  receivables: RiwayatPiutang[]
}

const STATUS_NOTA: Record<string, string> = {
  lunas: 'Lunas',
  retur: 'Retur',
  void: 'Void',
}

const STATUS_ORDER: Record<string, string> = {
  baru: 'Baru',
  diproses: 'Diproses',
  selesai: 'Selesai',
  dibayar: 'Dibayar',
  dibatalkan: 'Dibatalkan',
}

function toneNota(status: string): 'green' | 'red' | 'amber' {
  if (status === 'lunas') return 'green'
  if (status === 'void') return 'red'
  return 'amber'
}

function toneOrder(status: string): 'green' | 'red' | 'amber' | 'blue' {
  if (status === 'dibayar' || status === 'selesai') return 'green'
  if (status === 'dibatalkan') return 'red'
  if (status === 'diproses') return 'blue'
  return 'amber'
}

const { auth, companyId } = useCompany()
const access = useAccessStore()
const toast = useToast()
const { confirm } = useConfirm()
const eksporAsync = useAsync()
const mutasi = useAsync()
const profilAsync = useAsync()

const cari = ref('')
const formOpen = ref(false)
const sunting = ref<Pelanggan | null>(null)
const form = ref<FormPelanggan>(formKosong())
const profilOpen = ref(false)
const profil = ref<ProfilPelanggan | null>(null)

const {
  rows: list,
  meta,
  loading,
  error: errorDaftar,
  load,
  setPerPage,
} = usePagedList<Pelanggan>((page, perPage) => {
  const params = new URLSearchParams({ per_page: String(perPage), page: String(page) })
  if (cari.value.trim()) params.set('q', cari.value.trim())
  return api.getPage<Pelanggan>(`/companies/${companyId.value}/customers?${params}`, auth.token)
})

const bolehKelola = computed(() => access.can('pelanggan.kelola'))
const bolehEkspor = computed(() => access.can('pelanggan.ekspor'))
const error = computed(() => errorDaftar.value || eksporAsync.error.value)

function formKosong(): FormPelanggan {
  return { name: '', phone: '', address: '', notes: '' }
}

async function ekspor() {
  await eksporAsync.run(() =>
    downloadFile(
      `/companies/${companyId.value}/exports/customers`,
      'pelanggan.csv',
      auth.token,
      'text/csv',
    ),
  )
  if (eksporAsync.error.value) {
    toast.error(eksporAsync.error.value)
    return
  }
  toast.success('Data pelanggan diunduh.')
}

function bukaTambah() {
  mutasi.error.value = ''
  sunting.value = null
  form.value = formKosong()
  formOpen.value = true
}

function bukaUbah(p: Pelanggan) {
  mutasi.error.value = ''
  sunting.value = p
  form.value = {
    name: p.name,
    phone: p.phone ?? '',
    address: p.address ?? '',
    notes: p.notes ?? '',
  }
  formOpen.value = true
}

async function bukaProfil(p: Pelanggan) {
  profilOpen.value = true
  profil.value = null
  const data = await profilAsync.run(() =>
    api.get<ProfilPelanggan>(
      `/companies/${companyId.value}/customers/${p.id}/profile`,
      auth.token,
    ),
  )
  if (data) profil.value = data
}

async function simpan() {
  const f = form.value
  if (!f.name.trim()) return
  const body = {
    name: f.name.trim(),
    phone: f.phone.trim() || null,
    address: f.address.trim() || null,
    notes: f.notes.trim() || null,
  }
  const base = `/companies/${companyId.value}/customers`
  await mutasi.run(() =>
    sunting.value
      ? api.put(base + `/${sunting.value.id}`, body, auth.token)
      : api.post(base, body, auth.token),
  )
  if (mutasi.error.value) {
    toast.error(mutasi.error.value)
    return
  }
  formOpen.value = false
  toast.success(sunting.value ? 'Pelanggan diperbarui.' : 'Pelanggan ditambahkan.')
  await load()
}

async function hapus(p: Pelanggan) {
  const setuju = await confirm({
    title: 'Hapus pelanggan?',
    message: `${p.name} akan dihapus. Pelanggan yang sudah punya transaksi tidak bisa dihapus.`,
    confirmLabel: 'Hapus',
    tone: 'danger',
  })
  if (!setuju) return
  await mutasi.run(() => api.del(`/companies/${companyId.value}/customers/${p.id}`, auth.token))
  if (mutasi.error.value) {
    toast.error(mutasi.error.value)
    return
  }
  toast.success('Pelanggan dihapus.')
  await load()
}

onMounted(async () => {
  await access.load(companyId.value, auth.token)
  await load(1)
})
</script>

<template>
  <div>
    <AppPageHeader
      eyebrow="Pelanggan"
      title="Pelanggan"
      subtitle="Simpan kontak pelanggan agar transaksi dan order jasa lebih cepat."
      icon="group"
    >
      <template #actions>
        <AppButton v-if="bolehEkspor" variant="ghost" :loading="eksporAsync.loading.value" @click="ekspor">
          Ekspor CSV
        </AppButton>
        <AppButton v-if="bolehKelola" @click="bukaTambah">Pelanggan baru</AppButton>
      </template>
    </AppPageHeader>

    <AppBanner tone="red" :message="error" />

    <AppCard>
      <div class="mb-3 flex flex-wrap items-end gap-2">
        <AppSearch
          v-model="cari"
          class="min-w-56 flex-1"
          placeholder="Cari nama atau nomor telepon..."
          @search="load(1)"
        />
      </div>

      <AppTable
        :loading="loading"
        :empty="!list.length"
        :colspan="5"
        empty-title="Belum ada pelanggan"
        empty-message="Tambahkan pelanggan atau ubah kata kunci pencarian."
      >
        <template #head>
          <tr>
            <th>Pelanggan</th>
            <th>Telepon</th>
            <th class="hidden md:table-cell">Alamat</th>
            <th class="hidden lg:table-cell">Catatan</th>
            <th v-if="bolehKelola" class="text-right">Aksi</th>
          </tr>
        </template>
        <tr v-for="p in list" :key="p.id">
          <td>
            <div class="flex items-center gap-2.5">
              <AppAvatar :name="p.name" size="sm" />
              <button
                type="button"
                class="rounded text-left font-medium text-primary transition hover:text-primary-ink focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/55"
                :aria-label="`Buka profil ${p.name}`"
                @click="bukaProfil(p)"
              >
                {{ p.name }}
              </button>
            </div>
          </td>
          <td class="font-mono text-xs text-secondary">{{ p.phone ?? '—' }}</td>
          <td class="hidden max-w-xs truncate text-secondary md:table-cell">
            {{ p.address ?? '—' }}
          </td>
          <td class="hidden max-w-xs truncate text-secondary lg:table-cell">
            {{ p.notes ?? '—' }}
          </td>
          <td v-if="bolehKelola" class="text-right">
            <div class="flex justify-end gap-2">
              <AppButton variant="ghost" size="sm" @click="bukaUbah(p)">Ubah</AppButton>
              <AppButton
                variant="ghost"
                size="sm"
                class="text-danger"
                :aria-label="`Hapus pelanggan ${p.name}`"
                @click="hapus(p)"
              >
                Hapus
              </AppButton>
            </div>
          </td>
        </tr>
      </AppTable>
      <AppPager :meta="meta" @page="load" @update:per-page="setPerPage" />
    </AppCard>

    <AppModal
      :open="formOpen"
      :title="sunting ? 'Ubah pelanggan' : 'Pelanggan baru'"
      description="Nomor telepon boleh kosong, tetapi harus unik jika diisi."
      @close="formOpen = false"
    >
      <AppBanner tone="red" :message="mutasi.error.value" />
      <form id="pelanggan-form" class="grid gap-3" @submit.prevent="simpan">
        <AppField label="Nama pelanggan">
          <AppInput v-model="form.name" maxlength="255" data-autofocus />
        </AppField>
        <AppField label="Telepon">
          <AppInput v-model="form.phone" maxlength="30" placeholder="cth. 0812 3456 7890" />
        </AppField>
        <AppField label="Alamat">
          <AppTextarea v-model="form.address" :rows="2" :maxlength="500" />
        </AppField>
        <AppField label="Catatan">
          <AppTextarea
            v-model="form.notes"
            :rows="2"
            :maxlength="500"
            placeholder="cth. pelanggan langganan, preferensi servis"
          />
        </AppField>
      </form>
      <template #footer>
        <AppButton variant="ghost" @click="formOpen = false">Batal</AppButton>
        <AppButton
          type="submit"
          form="pelanggan-form"
          :loading="mutasi.loading.value"
          :disabled="!form.name.trim()"
        >
          Simpan
        </AppButton>
      </template>
    </AppModal>

    <AppModal
      :open="profilOpen"
      title="Profil pelanggan"
      description="Riwayat transaksi, order jasa, dan piutang pelanggan ini."
      size="xl"
      @close="profilOpen = false"
    >
      <AppBanner tone="red" :message="profilAsync.error.value" />
      <div v-if="profilAsync.loading.value && !profil" class="py-8 text-center">
        <p class="text-sm text-secondary" role="status">Memuat profil pelanggan...</p>
      </div>
      <template v-else-if="profil">
        <div class="flex flex-wrap items-start gap-3 rounded-2xl border border-outline/70 bg-surface-low/50 p-4">
          <AppAvatar :name="profil.customer.name" size="xl" />
          <div class="min-w-0 flex-1">
            <p class="font-display text-lg font-semibold text-ink">{{ profil.customer.name }}</p>
            <p class="mt-0.5 text-sm text-secondary">
              {{ profil.customer.phone ?? 'Tanpa nomor telepon' }}
              <span aria-hidden="true"> · </span>
              {{ profil.customer.address ?? 'Alamat belum diisi' }}
            </p>
            <p v-if="profil.customer.notes" class="mt-1.5 text-sm text-secondary">
              Catatan: {{ profil.customer.notes }}
            </p>
          </div>
          <AppButton
            v-if="bolehKelola"
            variant="ghost"
            size="sm"
            @click="profilOpen = false; bukaUbah(profil.customer)"
          >
            Ubah data
          </AppButton>
        </div>

        <div class="mt-4 grid gap-3 sm:grid-cols-2 xl:grid-cols-3">
          <AppStat
            label="Transaksi"
            :value="profil.stats.transactions.toLocaleString('id-ID')"
            icon="receipt_long"
            :hint="profil.stats.lastPurchaseAt ? `Terakhir ${tanggal(profil.stats.lastPurchaseAt)}` : 'Belum ada transaksi'"
          />
          <AppStat label="Net belanja" :value="rupiah(profil.stats.netSpend)" icon="payments" />
          <AppStat
            label="Rata-rata / transaksi"
            :value="rupiah(profil.stats.avgTicket)"
            icon="calculate"
          />
          <AppStat
            label="Refund"
            :value="rupiah(profil.stats.refunds)"
            icon="undo"
            :tone="profil.stats.refunds > 0 ? 'warning' : 'default'"
          />
          <AppStat
            label="Piutang"
            :value="rupiah(profil.stats.receivablesOutstanding)"
            icon="request_quote"
            :tone="profil.stats.receivablesOutstanding > 0 ? 'warning' : 'default'"
            :hint="`${profil.stats.receivablesOpen} tagihan belum lunas`"
          />
          <AppStat
            label="Order jasa aktif"
            :value="profil.stats.activeServiceOrders.toLocaleString('id-ID')"
            icon="build"
            :hint="`${profil.stats.serviceOrders} total order`"
          />
        </div>

        <section class="mt-5">
          <div class="flex flex-wrap items-center justify-between gap-2">
            <h3 class="font-display font-semibold">Riwayat transaksi</h3>
            <RouterLink
              v-if="bolehKelola"
              :to="`/c/${companyId}/sales`"
              class="text-xs font-semibold text-primary hover:text-primary-ink"
            >
              Lihat semua nota
            </RouterLink>
          </div>
          <AppTable
            class="mt-2"
            :empty="!profil.sales.length"
            :colspan="7"
            empty-message="Belum ada transaksi untuk pelanggan ini."
          >
            <template #head>
              <tr>
                <th>Nota</th>
                <th>Tanggal</th>
                <th class="hidden md:table-cell">Cabang</th>
                <th>Status</th>
                <th class="text-right">Total</th>
                <th class="hidden text-right sm:table-cell">Refund</th>
                <th class="text-right">Net</th>
              </tr>
            </template>
            <tr v-for="s in profil.sales" :key="s.id">
              <td class="font-mono text-xs">{{ s.number }}</td>
              <td class="whitespace-nowrap text-secondary">{{ tanggal(s.createdAt) }}</td>
              <td class="hidden text-secondary md:table-cell">{{ s.branchName }}</td>
              <td>
                <AppBadge :tone="toneNota(s.status)" dot>
                  {{ STATUS_NOTA[s.status] ?? s.status }}
                </AppBadge>
              </td>
              <td class="text-right tabular-nums">{{ rupiah(s.grandTotal) }}</td>
              <td class="hidden text-right tabular-nums sm:table-cell">{{ rupiah(s.refund) }}</td>
              <td class="text-right font-medium tabular-nums">{{ rupiah(s.net) }}</td>
            </tr>
          </AppTable>
        </section>

        <section class="mt-5">
          <h3 class="font-display font-semibold">Order jasa</h3>
          <AppTable
            class="mt-2"
            :empty="!profil.serviceOrders.length"
            :colspan="6"
            empty-message="Belum ada order jasa untuk pelanggan ini."
          >
            <template #head>
              <tr>
                <th>Order</th>
                <th>Status</th>
                <th class="hidden md:table-cell">Cabang</th>
                <th>Jadwal</th>
                <th class="text-right">DP</th>
                <th class="text-right">Total</th>
              </tr>
            </template>
            <tr v-for="o in profil.serviceOrders" :key="o.id">
              <td>
                <p class="font-medium">#{{ o.id }}</p>
                <p v-if="o.note" class="max-w-xs truncate text-xs text-secondary">{{ o.note }}</p>
              </td>
              <td>
                <AppBadge :tone="toneOrder(o.status)" dot>
                  {{ STATUS_ORDER[o.status] ?? o.status }}
                </AppBadge>
              </td>
              <td class="hidden text-secondary md:table-cell">{{ o.branchName }}</td>
              <td class="whitespace-nowrap text-secondary">
                {{ o.scheduledAt ? tanggal(o.scheduledAt) : 'Walk-in' }}
              </td>
              <td class="text-right tabular-nums">{{ rupiah(o.depositAmount) }}</td>
              <td class="text-right font-medium tabular-nums">{{ rupiah(o.total) }}</td>
            </tr>
          </AppTable>
        </section>

        <section class="mt-5">
          <h3 class="font-display font-semibold">Piutang pelanggan</h3>
          <AppTable
            class="mt-2"
            :empty="!profil.receivables.length"
            :colspan="6"
            empty-message="Tidak ada piutang untuk pelanggan ini."
          >
            <template #head>
              <tr>
                <th class="hidden md:table-cell">Cabang</th>
                <th class="text-right">Tagihan</th>
                <th class="text-right">Dibayar</th>
                <th class="text-right">Sisa</th>
                <th>Jatuh tempo</th>
                <th>Status</th>
              </tr>
            </template>
            <tr v-for="r in profil.receivables" :key="r.id">
              <td class="hidden text-secondary md:table-cell">{{ r.branchName }}</td>
              <td class="text-right tabular-nums">{{ rupiah(r.amount) }}</td>
              <td class="text-right tabular-nums">{{ rupiah(r.paidAmount) }}</td>
              <td class="text-right font-medium tabular-nums">{{ rupiah(r.remaining) }}</td>
              <td class="whitespace-nowrap text-secondary">{{ r.dueAt ? tanggal(r.dueAt) : '—' }}</td>
              <td>
                <AppBadge :tone="r.isPaid ? 'green' : 'amber'" dot>
                  {{ r.isPaid ? 'Lunas' : 'Terbuka' }}
                </AppBadge>
              </td>
            </tr>
          </AppTable>
        </section>
      </template>
    </AppModal>
  </div>
</template>
