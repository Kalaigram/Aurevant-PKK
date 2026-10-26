<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { api } from '../lib/api'
import { rupiah, tanggal } from '../lib/format'
import { labelSubscriptionStatus } from '../lib/labels'
import { useCompany } from '../composables/useCompany'
import { useAsync } from '../composables/useAsync'
import { usePagedList } from '../composables/usePagedList'
import AppBadge from '../components/ui/AppBadge.vue'
import AppBanner from '../components/ui/AppBanner.vue'
import AppButton from '../components/ui/AppButton.vue'
import AppCard from '../components/ui/AppCard.vue'
import AppField from '../components/ui/AppField.vue'
import AppInput from '../components/ui/AppInput.vue'
import AppPageHeader from '../components/ui/AppPageHeader.vue'
import AppPager from '../components/ui/AppPager.vue'
import AppSelect from '../components/ui/AppSelect.vue'
import AppSkeleton from '../components/ui/AppSkeleton.vue'
import AppTable from '../components/ui/AppTable.vue'

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

const OPSI_STATUS = [
  { value: 'trial', label: 'Uji coba' },
  { value: 'aktif', label: 'Aktif' },
  { value: 'tenggang', label: 'Tenggang (masih bisa tulis)' },
  { value: 'read_only', label: 'Hanya baca (tulis diblokir)' },
  { value: 'ditangguhkan', label: 'Ditangguhkan (tulis diblokir)' },
]

const { auth, companyId } = useCompany()
const { loading, error: errorUmum, run } = useAsync()
const sub = ref<Sub | null>(null)
const plan = ref<Plan | null>(null)
const plans = ref<Plan[]>([])
const jumlahCabang = ref(0)
const okMsg = ref('')

const adminLayanan = computed(() => auth.user?.isSuperAdmin ?? false)
const opsiPaket = computed(() => plans.value.map((p) => ({ value: String(p.id), label: p.name })))
const persenCabang = computed(() => {
  const max = plan.value?.maxBranches ?? 0
  if (!max) return 0
  return Math.min(100, Math.round((jumlahCabang.value / max) * 100))
})
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
  s === 'aktif' ? 'green' : s === 'trial' ? 'blue' : s === 'tenggang' ? 'amber' : 'red'
const toneTagihan = (s: string) => (s === 'lunas' ? 'green' : 'amber') as 'green' | 'amber'
const labelTagihan = (s: string) => (s === 'lunas' ? 'Lunas' : 'Belum lunas')
const tahapAktif = computed(() => TAHAP.findIndex((t) => t.key === sub.value?.status))

onMounted(async () => {
  await muatAwal()
  if (adminLayanan.value) await muatTagihan()
})
</script>

<template>
  <div class="mx-auto max-w-3xl">
    <AppPageHeader
      eyebrow="Pengaturan sistem"
      title="Status langganan perusahaan"
      subtitle="Pantau masa aktif, kuota, dan tagihan layanan perusahaan."
      icon="workspace_premium"
    />

    <AppBanner tone="green" :message="okMsg" />
    <AppBanner tone="red" :message="error" />

    <div v-if="loading && !sub" class="grid gap-3" aria-busy="true">
      <AppCard><AppSkeleton :lines="4" /></AppCard>
      <AppCard><AppSkeleton :lines="5" /></AppCard>
      <span class="sr-only" role="status">Memuat langganan…</span>
    </div>

    <template v-else-if="sub">
      <AppCard>
        <p class="text-xs font-semibold uppercase tracking-wider text-secondary">Tingkat layanan</p>
        <div class="mt-1 flex flex-wrap items-center justify-between gap-2">
          <h2 class="font-display text-lg font-bold text-ink">{{ sub.planName ?? 'Tanpa paket' }}</h2>
          <AppBadge :tone="tone(sub.status)" dot>{{ labelSubscriptionStatus(sub.status) }}</AppBadge>
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
        <div class="flex items-baseline justify-between gap-3 text-sm">
          <p class="text-secondary">Cabang aktif</p>
          <p class="font-semibold tabular-nums text-ink">
            {{ jumlahCabang }}<span v-if="plan" class="font-normal text-secondary"> / {{ plan.maxBranches }}</span>
          </p>
        </div>
        <div
          v-if="plan"
          class="mt-2 h-2 overflow-hidden rounded-full bg-surface-low"
          role="progressbar"
          aria-label="Pemakaian kuota cabang"
          aria-valuemin="0"
          aria-valuemax="100"
          :aria-valuenow="persenCabang"
        >
          <div
            class="h-full rounded-full bg-primary-container transition-[width] duration-300"
            :style="{ width: `${persenCabang}%` }"
          />
        </div>
        <div v-if="plan?.features?.length" class="mt-4">
          <p class="text-2xs font-semibold uppercase tracking-[0.08em] text-secondary">
            Fitur paket aktif
          </p>
          <div class="mt-1.5 flex flex-wrap gap-1.5">
            <span
              v-for="f in plan.features"
              :key="f"
              class="rounded-full bg-primary-soft px-2.5 py-0.5 text-xs font-medium text-primary-ink"
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
        <AppCard
          title="Kelola langganan"
          subtitle="Khusus admin layanan. Setiap perubahan tercatat di audit."
          class="mt-4"
        >
          <div class="grid gap-3 md:grid-cols-3">
            <AppField label="Status">
              <AppSelect v-model="fKelola.status" :options="OPSI_STATUS" />
            </AppField>
            <AppField label="Paket">
              <AppSelect
                v-model="fKelola.plan_id"
                placeholder="— tetap —"
                :options="opsiPaket"
              />
            </AppField>
            <AppField label="Aktif sampai">
              <AppInput v-model="fKelola.ends_at" type="date" />
            </AppField>
          </div>
          <AppButton class="mt-3" @click="simpanKelola">Simpan perubahan</AppButton>
        </AppCard>

        <AppCard
          title="Tagihan langganan"
          subtitle="Terbitkan tagihan manual atau tandai pembayaran yang sudah diterima."
          class="mt-4"
        >
          <div class="grid gap-3 md:grid-cols-2">
            <AppField label="Paket" optional>
              <AppSelect
                v-model="fTagihan.plan_id"
                placeholder="— ikut paket aktif —"
                :options="opsiPaket"
              />
            </AppField>
            <AppField label="Nominal (Rp)">
              <AppInput v-model.number="fTagihan.amount" type="number" min="0" />
            </AppField>
            <AppField label="Jatuh tempo">
              <AppInput v-model="fTagihan.due_at" type="date" />
            </AppField>
            <AppField label="Catatan" optional>
              <AppInput v-model="fTagihan.note" maxlength="500" />
            </AppField>
          </div>
          <AppButton class="mt-3" @click="terbitkan">Terbitkan tagihan</AppButton>

          <AppTable
            class="mt-4"
            :loading="loadingTagihan"
            :empty="!tagihan.length"
            :colspan="6"
            empty-title="Belum ada tagihan"
            empty-message="Tagihan manual bisa diterbitkan di atas; sistem juga membuat tagihan otomatis H-7."
          >
            <template #head>
              <tr>
                <th>Tagihan</th>
                <th>Paket</th>
                <th>Nominal</th>
                <th>Jatuh tempo</th>
                <th>Status</th>
                <th class="text-right">Aksi</th>
              </tr>
            </template>
            <tr v-for="t in tagihan" :key="t.id">
              <td>
                <p class="font-semibold text-ink">#{{ t.id }}</p>
                <p v-if="t.note" class="max-w-48 truncate text-xs text-secondary">{{ t.note }}</p>
              </td>
              <td class="text-secondary">{{ t.plan?.name ?? '—' }}</td>
              <td class="whitespace-nowrap font-medium tabular-nums">{{ rupiah(t.amount) }}</td>
              <td class="whitespace-nowrap text-secondary">{{ tanggal(t.dueAt) }}</td>
              <td>
                <AppBadge :tone="toneTagihan(t.status)" dot>{{ labelTagihan(t.status) }}</AppBadge>
              </td>
              <td class="text-right">
                <AppButton
                  v-if="t.status !== 'lunas'"
                  size="sm"
                  variant="ghost"
                  :aria-label="`Tandai tagihan ${t.id} lunas`"
                  @click="lunaskan(t)"
                >
                  Tandai lunas
                </AppButton>
                <span v-else class="text-xs text-secondary">Selesai</span>
              </td>
            </tr>
          </AppTable>
          <AppPager :meta="metaTagihan" @page="setPage" @update:per-page="setPerPage" />
        </AppCard>
      </template>
    </template>
  </div>
</template>
