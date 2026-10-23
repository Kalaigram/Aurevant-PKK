<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { api } from '../lib/api'
import { rupiah, tanggal } from '../lib/format'
import { useCompany } from '../composables/useCompany'
import { useAsync } from '../composables/useAsync'
import { usePagedList } from '../composables/usePagedList'
import AppCard from '../components/ui/AppCard.vue'
import AppBadge from '../components/ui/AppBadge.vue'
import AppBanner from '../components/ui/AppBanner.vue'
import AppButton from '../components/ui/AppButton.vue'
import AppField from '../components/ui/AppField.vue'
import AppEmpty from '../components/ui/AppEmpty.vue'
import AppPager from '../components/ui/AppPager.vue'

interface Sub {
  planId: number | null
  planName: string | null
  status: string
  trialEndsAt: string | null
  endsAt: string | null
  allowsWrites: boolean
}
interface Plan {
  id: number
  name: string
  maxBranches: number
  maxEmployees: number
  features: string[] | null
  price: string
}
interface Tagihan {
  id: number
  plan?: { id: number; name: string } | null
  amount: string
  status: string
  dueAt?: string | null
  paidAt?: string | null
  note?: string | null
}

const TAHAP = [
  { key: 'trial', nama: 'Uji coba', ket: 'Akses tulis 14 hari, kuota terbatas.' },
  { key: 'aktif', nama: 'Produksi penuh', ket: 'Akses penuh total.' },
  { key: 'tenggang', nama: 'Tenggang (+7 hari)', ket: 'Peringatan admin.' },
  { key: 'read_only', nama: 'Hanya baca', ket: 'Tulis ditolak, baca tetap bisa.' },
  { key: 'ditangguhkan', nama: 'Ditangguhkan', ket: 'Layanan dikarantina.' },
]

const { auth, companyId } = useCompany()
const { loading, error: errorUmum, run } = useAsync()
const sub = ref<Sub | null>(null)
const plan = ref<Plan | null>(null)
const plans = ref<Plan[]>([])
const jumlahCabang = ref(0)
const okMsg = ref('')

const adminLayanan = computed(() => auth.user?.isSuperAdmin ?? false)
const fKelola = ref({ status: '', plan_id: '', ends_at: '' })
const {
  rows: tagihan,
  meta: metaTagihan,
  loading: loadingTagihan,
  error: errorTagihan,
  load: muatTagihan,
  setPage,
  setPerPage,
} = usePagedList<Tagihan>(
  (page, perPage) =>
    api.getPage<Tagihan>(
      `/companies/${companyId.value}/subscription-invoices?per_page=${perPage}&page=${page}`,
      auth.token,
    ),
  { perPage: 20 },
)
const error = computed(() => errorUmum.value || errorTagihan.value)
const fTagihan = ref({ plan_id: '', amount: 0, due_at: '', note: '' })

async function simpanKelola() {
  const res = await run(() =>
    api.put(
      `/companies/${companyId.value}/subscription`,
      {
        status: fKelola.value.status,
        plan_id: fKelola.value.plan_id === '' ? undefined : Number(fKelola.value.plan_id),
        ends_at: fKelola.value.ends_at || undefined,
      },
      auth.token,
    ),
  )
  if (res || !errorUmum.value) {
    okMsg.value = 'Langganan perusahaan diperbarui (tercatat di audit).'
    await muatAwal()
  }
}

async function terbitkan() {
  const res = await run(() =>
    api.post(
      `/companies/${companyId.value}/subscription-invoices`,
      {
        plan_id: fTagihan.value.plan_id === '' ? undefined : Number(fTagihan.value.plan_id),
        amount: Number(fTagihan.value.amount),
        due_at: fTagihan.value.due_at,
        note: fTagihan.value.note || undefined,
      },
      auth.token,
    ),
  )
  if (res) {
    okMsg.value = 'Tagihan diterbitkan. Tagihan otomatis juga dibuat sistem H-7.'
    fTagihan.value = { plan_id: '', amount: 0, due_at: '', note: '' }
    await Promise.all([muatTagihan(1), muatAwal()])
  }
}

async function lunaskan(t: Tagihan) {
  const res = await run(() =>
    api.post(`/companies/${companyId.value}/subscription-invoices/${t.id}/pay`, {}, auth.token),
  )
  if (res || !errorUmum.value) {
    okMsg.value = 'Tagihan lunas — masa aktif +30 hari.'
    await Promise.all([muatTagihan(), muatAwal()])
  }
}

async function muatAwal() {
  sub.value = await run(() =>
    api.get<Sub>(`/companies/${companyId.value}/subscription`, auth.token),
  )
  if (sub.value) {
    plans.value = (await run(() => api.get<Plan[]>('/plans', auth.token))) ?? []
    plan.value = plans.value.find((p) => p.id === sub.value!.planId) ?? null
    const branches =
      (await run(() => api.get<unknown[]>(`/companies/${companyId.value}/branches`, auth.token))) ??
      []
    jumlahCabang.value = branches.length
    fKelola.value = {
      status: sub.value.status,
      plan_id: sub.value.planId === null ? '' : String(sub.value.planId),
      ends_at: (sub.value.endsAt ?? '').slice(0, 10),
    }
  }
}

const tone = (s: string) =>
  s === 'aktif' || s === 'trial' ? 'green' : s === 'tenggang' ? 'amber' : 'red'
const tahapAktif = computed(() => TAHAP.findIndex((t) => t.key === sub.value?.status))

onMounted(async () => {
  await muatAwal()
  if (adminLayanan.value) await muatTagihan()
})
</script>

<template>
  <div class="mx-auto max-w-2xl">
    <p class="text-xs font-semibold uppercase tracking-wider text-secondary">Pengaturan sistem</p>
    <h1 class="font-display mb-4 text-xl font-semibold">Status langganan perusahaan</h1>
    <AppBanner tone="green" :message="okMsg" />
    <AppBanner tone="red" :message="error" />
    <p v-if="loading" class="text-sm text-secondary">Memuat…</p>

    <template v-if="sub">
      <AppCard>
        <p class="text-xs font-semibold uppercase tracking-wider text-secondary">Tingkat layanan</p>
        <div class="mt-1 flex flex-wrap items-center justify-between gap-2">
          <h2 class="font-display text-lg font-bold">{{ sub.planName ?? 'Tanpa paket' }}</h2>
          <AppBadge :tone="tone(sub.status)">{{ sub.status }}</AppBadge>
        </div>
        <div class="mt-3 grid grid-cols-2 gap-3 text-sm">
          <div>
            <p class="text-xs uppercase tracking-wider text-secondary">Masa aktif hingga</p>
            <p class="font-semibold">{{ tanggal(sub.endsAt ?? sub.trialEndsAt) }}</p>
          </div>
          <div>
            <p class="text-xs uppercase tracking-wider text-secondary">Izin tulis</p>
            <p class="font-semibold" :class="sub.allowsWrites ? 'text-primary' : 'text-danger'">
              {{ sub.allowsWrites ? 'Ya' : 'Diblokir' }}
            </p>
          </div>
        </div>
        <div v-if="plan" class="mt-2 text-sm text-secondary">
          Harga paket {{ rupiah(plan.price) }} · maks {{ plan.maxBranches }} cabang &
          {{ plan.maxEmployees }} karyawan.
        </div>
        <AppBanner
          v-if="!sub.allowsWrites"
          tone="red"
          message="Penulisan diblokir, namun laporan dan riwayat tetap bisa dibaca. Hubungi pemilik perusahaan."
        />
      </AppCard>

      <AppCard title="Alur status" class="mt-4">
        <ol class="space-y-2">
          <li v-for="(t, i) in TAHAP" :key="t.key" class="flex items-start gap-3">
            <span
              class="mt-0.5 flex h-6 w-6 shrink-0 items-center justify-center rounded-full text-xs font-bold"
              :class="
                i === tahapAktif
                  ? 'bg-primary-container text-on-primary'
                  : i < tahapAktif
                    ? 'bg-primary-container/20 text-primary'
                    : 'bg-surface-low text-secondary'
              "
              >{{ i < tahapAktif ? '✓' : i + 1 }}</span
            >
            <div>
              <p class="text-sm font-semibold" :class="i === tahapAktif ? 'text-primary' : ''">
                {{ t.nama }}<span v-if="i === tahapAktif"> (sekarang)</span>
              </p>
              <p class="text-xs text-secondary">{{ t.ket }}</p>
            </div>
          </li>
        </ol>
      </AppCard>

      <AppCard title="Kuota dipakai" class="mt-4">
        <p class="text-sm">
          Cabang: <strong>{{ jumlahCabang }}{{ plan ? ` / ${plan.maxBranches}` : '' }}</strong>
        </p>
        <div v-if="plan?.features?.length" class="mt-3">
          <p class="text-xs uppercase tracking-wider text-secondary">Fitur paket aktif</p>
          <div class="mt-1 flex flex-wrap gap-1.5">
            <span
              v-for="f in plan.features"
              :key="f"
              class="rounded-full bg-surface-low px-2.5 py-0.5 text-xs font-medium"
              >{{ f }}</span
            >
          </div>
        </div>
      </AppCard>

      <AppCard title="Cara perpanjang" class="mt-4">
        <ul class="list-disc space-y-1 pl-5 text-sm text-secondary">
          <li>Tanpa potong otomatis — tagihan diterbitkan admin layanan.</li>
          <li>Aktivasi setelah pembayaran terkonfirmasi manual.</li>
        </ul>
      </AppCard>

      <template v-if="adminLayanan">
        <AppCard title="Kelola langganan (admin layanan)" class="mt-4">
          <div class="grid gap-2 md:grid-cols-3">
            <AppField label="Status">
              <select
                v-model="fKelola.status"
                class="w-full rounded-lg bg-surface-low px-3 py-2 text-sm"
              >
                <option value="trial">Uji coba</option>
                <option value="aktif">Aktif</option>
                <option value="tenggang">Tenggang (masih bisa tulis)</option>
                <option value="read_only">Hanya baca (tulis diblokir)</option>
                <option value="ditangguhkan">Ditangguhkan (tulis diblokir)</option>
              </select>
            </AppField>
            <AppField label="Paket">
              <select
                v-model="fKelola.plan_id"
                class="w-full rounded-lg bg-surface-low px-3 py-2 text-sm"
              >
                <option value="">— tetap —</option>
                <option v-for="p in plans" :key="p.id" :value="p.id">{{ p.name }}</option>
              </select>
            </AppField>
            <AppField label="Aktif sampai"
              ><input
                v-model="fKelola.ends_at"
                type="date"
                class="w-full rounded-lg bg-surface-low px-3 py-2 text-sm"
            /></AppField>
          </div>
          <AppButton class="mt-2" @click="simpanKelola">Simpan perubahan</AppButton>
        </AppCard>

        <AppCard title="Tagihan langganan" class="mt-4">
          <div class="grid gap-2 md:grid-cols-2">
            <AppField label="Paket (opsional)"
              ><select
                v-model="fTagihan.plan_id"
                class="w-full rounded-lg bg-surface-low px-3 py-2 text-sm"
              >
                <option value="">— ikut paket aktif —</option>
                <option v-for="p in plans" :key="p.id" :value="p.id">{{ p.name }}</option>
              </select></AppField
            >
            <AppField label="Nominal (Rp)"
              ><input
                v-model.number="fTagihan.amount"
                type="number"
                min="0"
                class="w-full rounded-lg bg-surface-low px-3 py-2 text-sm"
            /></AppField>
            <AppField label="Jatuh tempo"
              ><input
                v-model="fTagihan.due_at"
                type="date"
                class="w-full rounded-lg bg-surface-low px-3 py-2 text-sm"
            /></AppField>
            <AppField label="Catatan"
              ><input
                v-model="fTagihan.note"
                maxlength="500"
                class="w-full rounded-lg bg-surface-low px-3 py-2 text-sm"
            /></AppField>
          </div>
          <AppButton class="mt-2" @click="terbitkan">Terbitkan tagihan</AppButton>
          <p v-if="loadingTagihan" class="mt-3 text-sm text-secondary">Memuat tagihan.</p>
          <AppEmpty
            v-else-if="!tagihan.length"
            message="Belum ada tagihan untuk perusahaan ini."
          />
          <ul v-else class="mt-3 divide-y divide-outline/60">
            <li
              v-for="t in tagihan"
              :key="t.id"
              class="flex flex-wrap items-center justify-between gap-2 py-2 text-sm"
            >
              <span
                ><strong>#{{ t.id }}</strong> · {{ t.plan?.name ?? '—' }} · {{ rupiah(t.amount) }} ·
                jatuh tempo {{ tanggal(t.dueAt) }} ·
                <AppBadge :tone="t.status === 'lunas' ? 'green' : 'amber'">{{
                  t.status
                }}</AppBadge></span
              >
              <AppButton v-if="t.status !== 'lunas'" variant="ghost" @click="lunaskan(t)"
                >Tandai lunas (+30 hari)</AppButton
              >
            </li>
          </ul>
          <AppPager :meta="metaTagihan" @page="setPage" @update:per-page="setPerPage" />
        </AppCard>
      </template>
    </template>
  </div>
</template>
