<script setup lang="ts">
import { computed, nextTick, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { api, assetUrl } from '../lib/api'
import { rupiah, tanggal } from '../lib/format'
import { useAuthStore } from '../stores/auth'
import { useAsync } from '../composables/useAsync'
import { useConfirm } from '../composables/useConfirm'
import AppAvatar from '../components/ui/AppAvatar.vue'
import AppBadge from '../components/ui/AppBadge.vue'
import AppBanner from '../components/ui/AppBanner.vue'
import AppButton from '../components/ui/AppButton.vue'
import AppCard from '../components/ui/AppCard.vue'
import AppEmpty from '../components/ui/AppEmpty.vue'
import AppField from '../components/ui/AppField.vue'
import AppIcon from '../components/ui/AppIcon.vue'
import AppInput from '../components/ui/AppInput.vue'
import AppModal from '../components/ui/AppModal.vue'
import AppPageHeader from '../components/ui/AppPageHeader.vue'
import AppSelect from '../components/ui/AppSelect.vue'
import AppSkeleton from '../components/ui/AppSkeleton.vue'

interface Company {
  id: number
  name: string
  slug: string
  status: string
  email: string | null
  phone: string | null
  logoUrl: string | null
}
interface Invoice {
  id: number
  amount: string
  status: string
  dueAt: string | null
}
interface Extra {
  branches: string[]
  sub: string
  ends: string | null
  invoice: { id: number; amount: string; dueAt: string | null } | null
}
type BadgeTone = 'green' | 'red' | 'amber' | 'blue' | 'gray'
type FieldErrors = { name: string; branch_code: string; branch_name: string }

const router = useRouter()
const auth = useAuthStore()
const page = useAsync()
const create = useAsync()
const { confirm } = useConfirm()
const list = ref<Company[]>([])
const extra = ref<Record<number, Extra>>({})
const showDaftar = ref(false)
const showTagihan = ref<Company | null>(null)
const daftar = ref({ name: '', branch_code: '', branch_name: '', timezone: 'Asia/Jakarta' })
const errors = ref<FieldErrors>({ name: '', branch_code: '', branch_name: '' })

const TIMEZONES = [
  { value: 'Asia/Jakarta', label: 'WIB · Asia/Jakarta' },
  { value: 'Asia/Makassar', label: 'WITA · Asia/Makassar' },
  { value: 'Asia/Jayapura', label: 'WIT · Asia/Jayapura' },
]

function statusInfo(status: string): { label: string; tone: BadgeTone; icon: string } {
  switch (status) {
    case 'aktif':
    case 'active':
      return { label: 'Aktif', tone: 'green', icon: 'check_circle' }
    case 'trial':
      return { label: 'Masa coba', tone: 'blue', icon: 'schedule' }
    case 'read_only':
      return { label: 'Hanya baca', tone: 'amber', icon: 'lock' }
    case 'ditangguhkan':
    case 'suspended':
      return { label: 'Ditangguhkan', tone: 'red', icon: 'block' }
    case 'expired':
      return { label: 'Kedaluwarsa', tone: 'red', icon: 'event_busy' }
    case 'nonaktif':
    case 'inactive':
      return { label: 'Tidak aktif', tone: 'gray', icon: 'pause_circle' }
    default:
      return { label: status || 'Tidak diketahui', tone: 'gray', icon: 'info' }
  }
}

function subStatus(c: Company) {
  return extra.value[c.id]?.sub ?? c.status
}

function cabang(c: Company) {
  return extra.value[c.id]?.branches ?? []
}

function dibatasi(c: Company) {
  return ['read_only', 'ditangguhkan', 'suspended', 'expired'].includes(subStatus(c))
}

async function muat() {
  const data = await page.run(async () => {
    const companies = await api.get<Company[]>('/companies', auth.token)
    const rows = Array.isArray(companies) ? companies : []
    const hasil: Record<number, Extra> = {}

    await Promise.all(
      rows.map(async (company) => {
        const [branches, sub, invoices] = await Promise.all([
          api.get<{ code: string }[]>(`/companies/${company.id}/branches`, auth.token),
          api.get<{ status: string; endsAt: string | null; trialEndsAt: string | null }>(
            `/companies/${company.id}/subscription`,
            auth.token,
          ),
          api.get<Invoice[] | { data: Invoice[] }>(
            `/companies/${company.id}/subscription-invoices?per_page=5`,
            auth.token,
          ),
        ])
        const daftarInvoice = Array.isArray(invoices) ? invoices : (invoices?.data ?? [])
        const pending = daftarInvoice.find((invoice) => invoice.status === 'menunggu') ?? null
        hasil[company.id] = {
          branches: (branches ?? []).map((branch) => branch.code),
          sub: sub?.status ?? company.status,
          ends: sub?.endsAt ?? sub?.trialEndsAt ?? null,
          invoice: pending
            ? { id: pending.id, amount: pending.amount, dueAt: pending.dueAt }
            : null,
        }
      }),
    )

    return { rows, hasil }
  })

  if (data) {
    list.value = data.rows
    extra.value = data.hasil
  }
}

function pilih(c: Company) {
  auth.pickCompany(c.id, c.name, c.logoUrl)
  router.push(`/c/${c.id}/dashboard`)
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
  window.location.href = '/login'
}

function resetDaftar() {
  daftar.value = { name: '', branch_code: '', branch_name: '', timezone: 'Asia/Jakarta' }
  errors.value = { name: '', branch_code: '', branch_name: '' }
}

function bukaDaftar() {
  resetDaftar()
  create.error.value = ''
  showDaftar.value = true
}

function tutupDaftar() {
  if (create.loading.value) return
  showDaftar.value = false
  resetDaftar()
}

function validasiDaftar() {
  errors.value = {
    name: daftar.value.name.trim() ? '' : 'Nama perusahaan wajib diisi.',
    branch_code: daftar.value.branch_code.trim() ? '' : 'Kode cabang wajib diisi.',
    branch_name: daftar.value.branch_name.trim() ? '' : 'Nama cabang wajib diisi.',
  }
  return !Object.values(errors.value).some(Boolean)
}

async function daftarkan() {
  if (!validasiDaftar()) {
    await nextTick()
    const pertama = errors.value.name
      ? 'nc-name'
      : errors.value.branch_code
        ? 'nc-code'
        : 'nc-branch'
    document.getElementById(pertama)?.focus()
    return
  }

  const res = await create.run(() =>
    api.post<{ id: number; name: string }>(
      '/companies',
      { ...daftar.value },
      auth.token,
    ),
  )
  if (res) {
    showDaftar.value = false
    resetDaftar()
    await muat()
  }
}

const tagihan = computed(() => {
  if (!showTagihan.value) return null
  return extra.value[showTagihan.value.id] ?? null
})

onMounted(muat)
</script>

<template>
  <div class="mx-auto w-full max-w-4xl">
    <AppPageHeader
      title="Pilih perusahaan"
      eyebrow="Gerbang multi-perusahaan"
      subtitle="Pilih ruang kerja yang ingin Anda kelola, atau daftarkan perusahaan baru."
      icon="domain"
    >
      <template #meta>
        <p class="mt-2 text-xs text-secondary">
          Masuk sebagai <span class="font-semibold text-ink">{{ auth.user?.name }}</span>
          <span aria-hidden="true"> · </span>{{ list.length }} perusahaan terhubung
        </p>
      </template>
      <template #actions>
        <AppButton variant="ghost" @click="keluar">
          <AppIcon name="logout" :size="18" />
          Keluar
        </AppButton>
      </template>
    </AppPageHeader>

    <AppBanner tone="red" :message="page.error.value" />

    <div
      v-if="page.loading.value"
      class="grid gap-4 md:grid-cols-2"
      aria-busy="true"
      aria-label="Memuat perusahaan"
    >
      <p class="sr-only" role="status">Memuat perusahaan…</p>
      <AppCard v-for="n in 2" :key="n" padding="md" class="shadow-card">
        <div class="flex items-center gap-3">
          <AppSkeleton height="3rem" width="3rem" rounded="xl" />
          <div class="flex-1 space-y-2">
            <AppSkeleton height="0.85rem" width="55%" />
            <AppSkeleton height="0.7rem" width="35%" />
          </div>
        </div>
        <div class="mt-4 space-y-2">
          <AppSkeleton height="0.7rem" width="80%" />
          <AppSkeleton height="0.7rem" width="60%" />
        </div>
      </AppCard>
    </div>

    <div
      v-else-if="!list.length && page.error.value"
      class="rounded-2xl border border-outline/70 bg-card p-6 text-center shadow-card"
    >
      <p class="text-sm text-secondary">Daftar perusahaan belum bisa dimuat.</p>
      <AppButton variant="ghost" class="mt-3" @click="muat">
        <AppIcon name="refresh" :size="18" />
        Coba lagi
      </AppButton>
    </div>

    <AppEmpty
      v-else-if="!list.length"
      title="Belum ada perusahaan"
      message="Daftarkan usaha pertama Anda untuk mulai mengelola operasional."
      icon="domain_add"
    >
      <AppButton @click="bukaDaftar">
        <AppIcon name="add_business" :size="18" />
        Daftarkan perusahaan
      </AppButton>
    </AppEmpty>

    <div v-else class="grid gap-4 md:grid-cols-2">
      <article
        v-for="c in list"
        :key="c.id"
        class="flex h-full flex-col overflow-hidden rounded-2xl border border-outline/70 bg-card shadow-card transition duration-150 hover:-translate-y-0.5 hover:shadow-lift"
      >
        <div class="flex flex-1 flex-col p-4 sm:p-5">
          <div class="flex items-start gap-3">
            <AppAvatar
              :name="c.name"
              :src="assetUrl(c.logoUrl)"
              size="lg"
              square
              class="bg-primary-container text-on-primary ring-primary/20"
            />
            <div class="min-w-0 flex-1">
              <div class="flex flex-wrap items-start justify-between gap-2">
                <div class="min-w-0">
                  <h2 class="font-display text-base font-semibold leading-snug text-ink">
                    {{ c.name }}
                  </h2>
                  <p class="mt-0.5 font-mono text-2xs text-secondary">{{ c.slug }}</p>
                </div>
                <AppBadge :tone="statusInfo(subStatus(c)).tone" dot>
                  {{ statusInfo(subStatus(c)).label }}
                </AppBadge>
              </div>
              <p
                v-if="c.email || c.phone"
                class="mt-1.5 truncate text-xs text-secondary"
                :title="[c.email, c.phone].filter(Boolean).join(' · ')"
              >
                {{ [c.email, c.phone].filter(Boolean).join(' · ') }}
              </p>
            </div>
          </div>

          <div v-if="auth.companyId === c.id" class="mt-3">
            <AppBadge tone="blue" dot>Terakhir dipakai</AppBadge>
          </div>

          <div v-if="cabang(c).length" class="mt-3.5">
            <p class="text-2xs font-semibold uppercase tracking-[0.08em] text-secondary">
              Cabang
            </p>
            <div class="mt-1.5 flex flex-wrap gap-1.5">
              <span
                v-for="code in cabang(c).slice(0, 3)"
                :key="code"
                class="inline-flex h-6 items-center rounded-md bg-surface-low px-2 font-mono text-2xs font-medium text-secondary ring-1 ring-inset ring-outline/60"
              >
                {{ code }}
              </span>
              <span
                v-if="cabang(c).length > 3"
                class="inline-flex h-6 items-center rounded-md bg-surface-low px-2 text-2xs font-medium text-secondary ring-1 ring-inset ring-outline/60"
                :title="cabang(c).join(', ')"
              >
                +{{ cabang(c).length - 3 }}
              </span>
            </div>
          </div>

          <p
            v-if="extra[c.id]?.ends"
            class="mt-3 flex items-center gap-1.5 text-xs text-secondary"
          >
            <AppIcon name="calendar_today" :size="15" class="text-secondary" />
            Berlaku sampai
            <strong class="font-semibold text-ink">{{ tanggal(extra[c.id]!.ends) }}</strong>
          </p>

          <div
            v-if="dibatasi(c)"
            class="mt-3 flex items-start gap-2.5 rounded-xl border border-warn/25 bg-warn-bg/60 p-3 text-warn-ink"
          >
            <AppIcon :name="statusInfo(subStatus(c)).icon" :size="18" class="mt-0.5 shrink-0" />
            <div class="min-w-0">
              <p class="text-xs font-semibold">Akses tulis dibatasi</p>
              <p class="mt-0.5 text-xs leading-relaxed text-warn-ink/80">
                Masa berlaku langganan sudah habis.
              </p>
              <p v-if="extra[c.id]?.invoice" class="mt-1 text-xs">
                Tagihan menunggu:
                <strong class="font-semibold">{{
                  rupiah(extra[c.id]!.invoice!.amount)
                }}</strong>
              </p>
              <button
                v-if="extra[c.id]?.invoice"
                type="button"
                class="mt-1.5 rounded font-semibold text-warn-ink underline transition hover:text-ink focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-warn/50 focus-visible:ring-offset-2"
                @click="showTagihan = c"
              >
                Lihat tagihan
              </button>
            </div>
          </div>
        </div>

        <div class="border-t border-outline/55 bg-surface-low/35 p-4 sm:px-5">
          <AppButton block @click="pilih(c)">
            Pilih & masuk dasbor
            <AppIcon name="arrow_forward" :size="18" />
          </AppButton>
        </div>
      </article>

      <button
        type="button"
        class="group flex min-h-48 flex-col items-start justify-center rounded-2xl border-2 border-dashed border-outline/80 bg-card/60 p-5 text-left transition hover:border-primary hover:bg-primary-soft/25 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/55"
        @click="bukaDaftar"
      >
        <span
          class="flex h-11 w-11 items-center justify-center rounded-2xl bg-primary-soft text-primary-ink transition group-hover:bg-primary group-hover:text-on-primary"
        >
          <AppIcon name="domain_add" :size="22" />
        </span>
        <span class="mt-3 font-display text-sm font-semibold text-ink">
          Daftarkan perusahaan baru
        </span>
        <span class="mt-1 text-xs leading-relaxed text-secondary">
          Tambah usaha lain ke akun Anda.
        </span>
      </button>
    </div>

    <AppModal
      :open="showDaftar"
      title="Daftarkan perusahaan"
      description="Tambahkan usaha baru beserta cabang pertamanya."
      @close="tutupDaftar"
    >
      <form id="form-perusahaan" novalidate class="space-y-4" @submit.prevent="daftarkan">
        <AppBanner tone="red" :message="create.error.value" />
        <AppField label="Nama perusahaan" for="nc-name" required :error="errors.name">
          <AppInput
            id="nc-name"
            v-model="daftar.name"
            placeholder="Contoh: PT Surya Jaya"
            :invalid="Boolean(errors.name)"
            data-autofocus
            @update:model-value="errors.name = ''"
          >
            <template #prefix>
              <AppIcon name="storefront" :size="19" class="text-secondary" />
            </template>
          </AppInput>
        </AppField>

        <div class="grid gap-4 sm:grid-cols-2">
          <AppField
            label="Kode cabang pertama"
            for="nc-code"
            required
            :error="errors.branch_code"
            hint="Contoh: PST"
          >
            <AppInput
              id="nc-code"
              v-model="daftar.branch_code"
              placeholder="PST"
              :invalid="Boolean(errors.branch_code)"
              @update:model-value="errors.branch_code = ''"
            />
          </AppField>
          <AppField
            label="Nama cabang"
            for="nc-branch"
            required
            :error="errors.branch_name"
          >
            <AppInput
              id="nc-branch"
              v-model="daftar.branch_name"
              placeholder="Pusat"
              :invalid="Boolean(errors.branch_name)"
              @update:model-value="errors.branch_name = ''"
            />
          </AppField>
        </div>

        <AppField label="Zona waktu" for="nc-tz" hint="Mengikuti lokasi operasional cabang.">
          <AppSelect id="nc-tz" v-model="daftar.timezone" :options="TIMEZONES" />
        </AppField>
      </form>

      <template #footer>
        <AppButton variant="ghost" @click="tutupDaftar">Batal</AppButton>
        <AppButton type="submit" form="form-perusahaan" :loading="create.loading.value">
          Simpan perusahaan
        </AppButton>
      </template>
    </AppModal>

    <AppModal
      :open="Boolean(showTagihan)"
      title="Langganan dibatasi"
      description="Akses tulis perusahaan dibatasi sementara."
      @close="showTagihan = null"
    >
      <div v-if="showTagihan" class="space-y-4">
        <div class="rounded-xl border border-outline/60 bg-surface-low/60 p-3.5">
          <p class="text-2xs font-semibold uppercase tracking-[0.08em] text-secondary">
            Perusahaan
          </p>
          <div class="mt-1 flex flex-wrap items-center justify-between gap-2">
            <p class="font-display text-sm font-semibold text-ink">{{ showTagihan.name }}</p>
            <AppBadge :tone="statusInfo(subStatus(showTagihan)).tone" dot>
              {{ statusInfo(subStatus(showTagihan)).label }}
            </AppBadge>
          </div>
        </div>

        <div
          v-if="tagihan?.invoice"
          class="rounded-xl border border-warn/25 bg-warn-bg/60 p-3.5 text-warn-ink"
        >
          <p class="text-2xs font-semibold uppercase tracking-[0.08em]">Tagihan menunggu</p>
          <p class="mt-1 font-display text-2xl font-bold tabular-nums">
            {{ rupiah(tagihan.invoice.amount) }}
          </p>
          <p class="mt-1 text-xs">
            Jatuh tempo
            <strong class="font-semibold">{{ tanggal(tagihan.invoice.dueAt) }}</strong>
          </p>
        </div>
        <p v-else class="text-sm leading-relaxed text-secondary">
          Belum ada tagihan menunggu. Hubungi admin layanan untuk memperpanjang akses perusahaan.
        </p>

        <p class="text-xs leading-relaxed text-secondary">
          Setelah pembayaran dikonfirmasi, akses tulis akan aktif kembali secara otomatis.
        </p>
      </div>

      <template #footer>
        <AppButton variant="ghost" @click="showTagihan = null">Tutup</AppButton>
      </template>
    </AppModal>
  </div>
</template>
