<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { api, assetUrl } from '../lib/api'
import { rupiah, tanggal } from '../lib/format'
import { useAuthStore } from '../stores/auth'
import { useAsync } from '../composables/useAsync'
import AppAvatar from '../components/ui/AppAvatar.vue'
import AppBadge from '../components/ui/AppBadge.vue'
import AppBanner from '../components/ui/AppBanner.vue'
import AppButton from '../components/ui/AppButton.vue'
import AppEmpty from '../components/ui/AppEmpty.vue'
import AppIcon from '../components/ui/AppIcon.vue'

interface Company { id: number; name: string; slug: string; status: string; email: string | null; phone: string | null; logoUrl: string | null }
interface Extra { branches: string[]; sub: string; ends: string | null; invoice: { id: number; amount: string; dueAt: string | null } | null }
interface Invoice { id: number; amount: string; status: string; dueAt: string | null }

const router = useRouter()
const auth = useAuthStore()
const page = useAsync()
const list = ref<Company[]>([])
const extra = ref<Record<number, Extra>>({})
const showDaftar = ref(false)
const showTagihan = ref<Company | null>(null)
const daftar = ref({ name: '', branch_code: '', branch_name: '', timezone: 'Asia/Jakarta' })

const TIMEZONES = ['Asia/Jakarta', 'Asia/Makassar', 'Asia/Jayapura']

async function muat() {
  const companies = (await page.run(() => api.get<Company[]>('/companies', auth.token))) ?? []
  list.value = Array.isArray(companies) ? companies : []
  await Promise.all(list.value.map(async (c) => {
    const [branches, sub, invoices] = await Promise.all([
      page.run(() => api.get<{ code: string }[]>(`/companies/${c.id}/branches`, auth.token)),
      page.run(() => api.get<{ status: string; endsAt: string | null; trialEndsAt: string | null }>(`/companies/${c.id}/subscription`, auth.token)),
      page.run(() => api.get<{ data: Invoice[] }>(`/companies/${c.id}/subscription-invoices?per_page=5`, auth.token)),
    ])
    const inv = (Array.isArray(invoices) ? invoices : (invoices?.data ?? [])).find((i) => i.status === 'menunggu') ?? null
    extra.value[c.id] = {
      branches: (branches ?? []).map((b) => b.code),
      sub: sub?.status ?? c.status,
      ends: sub?.endsAt ?? sub?.trialEndsAt ?? null,
      invoice: inv ? { id: inv.id, amount: inv.amount, dueAt: inv.dueAt } : null,
    }
  }))
}

function pilih(c: Company) {
  auth.pickCompany(c.id, c.name, c.logoUrl)
  router.push(`/c/${c.id}/dashboard`)
}

async function keluar() {
  await auth.logout()
  window.location.href = '/login'
}

async function daftarkan() {
  const res = await page.run(() =>
    api.post<{ id: number; name: string }>(`/companies`, { ...daftar.value }, auth.token),
  )
  if (res) {
    showDaftar.value = false
    daftar.value = { name: '', branch_code: '', branch_name: '', timezone: 'Asia/Jakarta' }
    muat()
  }
}

onMounted(muat)
</script>

<template>
  <div class="mx-auto max-w-4xl p-4 md:p-6">
    <div class="mb-4 flex flex-wrap items-center justify-between gap-3">
      <div class="flex items-center gap-3">
        <div class="flex h-12 w-12 items-center justify-center rounded-xl bg-primary-container text-on-primary">
          <AppIcon name="domain" :size="28" />
        </div>
        <div>
          <p class="text-xs font-semibold uppercase tracking-wider text-secondary">Gerbang multi-perusahaan</p>
          <h1 class="font-display text-xl font-semibold">Pilih perusahaan</h1>
          <p class="font-mono text-xs text-secondary">{{ auth.user?.name }} ({{ list.length }} terhubung)</p>
        </div>
      </div>
      <AppButton variant="ghost" @click="keluar"><AppIcon name="logout" :size="18" />Keluar</AppButton>
    </div>
    <AppBanner tone="red" :message="page.error.value" />
    <p v-if="page.loading.value" class="text-sm text-secondary">Memuat…</p>
    <AppEmpty v-else-if="!list.length" message="Belum ada perusahaan. Daftarkan yang pertama di bawah." />

    <div class="grid gap-3 md:grid-cols-2">
      <div v-for="c in list" :key="c.id" class="rounded-xl bg-card p-4 shadow-sm transition hover:shadow">
        <div class="flex items-start gap-3">
          <AppAvatar :name="c.name" :src="assetUrl(c.logoUrl)" size="lg" square
            class="bg-primary-container text-on-primary ring-primary/20" />
          <div class="min-w-0 flex-1">
            <div class="flex items-center justify-between gap-2">
              <h2 class="font-display truncate font-semibold">{{ c.name }}</h2>
              <AppBadge :tone="extra[c.id]?.sub === 'aktif' || extra[c.id]?.sub === 'trial' ? 'green' : 'red'">
                {{ extra[c.id]?.sub ?? c.status }}
              </AppBadge>
            </div>
            <p class="mt-0.5 font-mono text-xs text-secondary">{{ c.slug }}</p>
            <p v-if="c.email || c.phone" class="mt-1 truncate text-xs text-secondary">
              {{ [c.email, c.phone].filter(Boolean).join(' · ') }}
            </p>
          </div>
        </div>
        <p v-if="extra[c.id]?.branches.length" class="mt-2 text-sm">
          <span class="text-secondary">Cabang: </span>{{ extra[c.id]!.branches.join(', ') }}
        </p>
        <p v-if="extra[c.id]?.ends" class="mt-1 text-sm text-secondary">
          Berlaku sampai <strong class="text-ink">{{ tanggal(extra[c.id]!.ends) }}</strong>
        </p>
        <div v-if="['read_only', 'ditangguhkan'].includes(extra[c.id]?.sub ?? '')" class="mt-2 rounded-lg bg-danger-bg p-2 text-sm text-red-900">
          Penulisan dibatasi sementara.
          <button class="font-semibold text-primary underline" @click="showTagihan = c">Lihat tagihan</button>
        </div>
        <AppButton class="mt-3 w-full" @click="pilih(c)">
          Pilih & masuk dasbor<AppIcon name="arrow_forward" :size="18" />
        </AppButton>
      </div>

      <button class="cursor-pointer rounded-xl border-2 border-dashed border-outline bg-card p-4 text-left transition hover:border-primary" @click="showDaftar = true">
        <div class="flex h-full min-h-28 flex-col items-start justify-center gap-1">
          <span class="flex items-center gap-2 font-display font-semibold"><AppIcon name="domain_add" :size="22" />Daftarkan perusahaan baru</span>
          <span class="text-sm text-secondary">Tambah usaha lain ke akun Anda.</span>
        </div>
      </button>
    </div>

    <div v-if="showDaftar" class="fixed inset-0 z-20 flex items-center justify-center bg-black/40 p-4" @click.self="showDaftar = false">
      <div class="w-full max-w-md rounded-xl bg-card p-5 shadow-xl">
        <h3 class="font-display font-semibold">Registrasi usaha baru</h3>
        <label class="mt-3 block text-sm font-medium" for="nc-name">Nama perusahaan</label>
        <input id="nc-name" v-model="daftar.name" class="mt-1 w-full rounded-lg bg-surface-low px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-primary" placeholder="Contoh: PT Surya Jaya" />
        <div class="mt-3 grid grid-cols-2 gap-2">
          <div>
            <label class="block text-sm font-medium" for="nc-code">Kode cabang pertama</label>
            <input id="nc-code" v-model="daftar.branch_code" class="mt-1 w-full rounded-lg bg-surface-low px-3 py-2 font-mono text-sm outline-none focus:ring-2 focus:ring-primary" placeholder="PST" />
          </div>
          <div>
            <label class="block text-sm font-medium" for="nc-branch">Nama cabang</label>
            <input id="nc-branch" v-model="daftar.branch_name" class="mt-1 w-full rounded-lg bg-surface-low px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-primary" placeholder="Pusat" />
          </div>
        </div>
        <label class="mt-3 block text-sm font-medium" for="nc-tz">Zona waktu</label>
        <select id="nc-tz" v-model="daftar.timezone" class="mt-1 w-full rounded-lg bg-surface-low px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-primary">
          <option v-for="tz in TIMEZONES" :key="tz" :value="tz">{{ tz }}</option>
        </select>
        <div class="mt-4 flex justify-end gap-2">
          <AppButton variant="ghost" @click="showDaftar = false">Batal</AppButton>
          <AppButton :loading="page.loading.value" @click="daftarkan">Simpan & buka</AppButton>
        </div>
      </div>
    </div>

    <div v-if="showTagihan" class="fixed inset-0 z-20 flex items-center justify-center bg-black/40 p-4" @click.self="showTagihan = null">
      <div class="w-full max-w-md rounded-xl bg-card p-5 shadow-xl">
        <h3 class="font-display font-semibold">Langganan dibatasi</h3>
        <p class="mt-1 text-sm text-secondary">
          Akses tulis <strong class="text-ink">{{ showTagihan.name }}</strong> dibatasi sementara karena masa berlaku habis.
        </p>
        <div v-if="extra[showTagihan.id]?.invoice" class="mt-3 rounded-lg bg-surface-low p-3 text-sm">
          <p>Tagihan menunggu: <strong>{{ rupiah(extra[showTagihan.id]!.invoice!.amount) }}</strong></p>
          <p class="text-secondary">Jatuh tempo {{ tanggal(extra[showTagihan.id]!.invoice!.dueAt) }}</p>
        </div>
        <p v-else class="mt-3 text-sm text-secondary">Belum ada tagihan menunggu. Hubungi admin layanan.</p>
        <div class="mt-4 flex justify-end">
          <AppButton variant="ghost" @click="showTagihan = null">Tutup</AppButton>
        </div>
      </div>
    </div>
  </div>
</template>
