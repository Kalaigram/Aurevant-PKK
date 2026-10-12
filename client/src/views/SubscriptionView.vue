<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { api } from '../lib/api'
import { rupiah, tanggal } from '../lib/format'
import { useCompany } from '../composables/useCompany'
import { useAsync } from '../composables/useAsync'
import AppCard from '../components/ui/AppCard.vue'
import AppBadge from '../components/ui/AppBadge.vue'
import AppBanner from '../components/ui/AppBanner.vue'

interface Sub {
  planId: number | null
  planName: string | null
  status: string
  trialEndsAt: string | null
  endsAt: string | null
  allowsWrites: boolean
}
interface Plan { id: number; name: string; maxBranches: number; maxEmployees: number; features: string[] | null; price: string }

const TAHAP = [
  { key: 'trial', nama: 'Uji coba', ket: 'Akses tulis 14 hari, kuota terbatas.' },
  { key: 'aktif', nama: 'Produksi penuh', ket: 'Akses penuh total.' },
  { key: 'tenggang', nama: 'Tenggang (+7 hari)', ket: 'Peringatan admin.' },
  { key: 'read_only', nama: 'Hanya baca', ket: 'Tulis ditolak, baca tetap bisa.' },
  { key: 'ditangguhkan', nama: 'Ditangguhkan', ket: 'Layanan dikarantina.' },
]

const { auth, companyId } = useCompany()
const { loading, error, run } = useAsync()
const sub = ref<Sub | null>(null)
const plan = ref<Plan | null>(null)
const jumlahCabang = ref(0)

const tone = (s: string) => (s === 'aktif' || s === 'trial' ? 'green' : s === 'tenggang' ? 'amber' : 'red')
const tahapAktif = computed(() => TAHAP.findIndex((t) => t.key === sub.value?.status))

onMounted(async () => {
  sub.value = await run(() => api.get<Sub>(`/companies/${companyId.value}/subscription`, auth.token))
  if (sub.value) {
    const plans = (await run(() => api.get<Plan[]>('/plans', auth.token))) ?? []
    plan.value = plans.find((p) => p.id === sub.value!.planId) ?? null
    const branches = (await run(() => api.get<unknown[]>(`/companies/${companyId.value}/branches`, auth.token))) ?? []
    jumlahCabang.value = branches.length
  }
})
</script>

<template>
  <div class="mx-auto max-w-2xl">
    <p class="text-xs font-semibold uppercase tracking-wider text-secondary">Pengaturan sistem</p>
    <h1 class="font-display mb-4 text-xl font-semibold">Status langganan perusahaan</h1>
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
          Harga paket {{ rupiah(plan.price) }} · maks {{ plan.maxBranches }} cabang & {{ plan.maxEmployees }} karyawan.
        </div>
        <AppBanner
          v-if="!sub.allowsWrites" tone="red"
          message="Penulisan diblokir, namun laporan dan riwayat tetap bisa dibaca. Hubungi pemilik perusahaan."
        />
      </AppCard>

      <AppCard title="Alur status" class="mt-4">
        <ol class="space-y-2">
          <li v-for="(t, i) in TAHAP" :key="t.key" class="flex items-start gap-3">
            <span
              class="mt-0.5 flex h-6 w-6 shrink-0 items-center justify-center rounded-full text-xs font-bold"
              :class="i === tahapAktif ? 'bg-primary-container text-on-primary' : i < tahapAktif ? 'bg-primary-container/20 text-primary' : 'bg-surface-low text-secondary'"
            >{{ i < tahapAktif ? '✓' : i + 1 }}</span>
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
            <span v-for="f in plan.features" :key="f" class="rounded-full bg-surface-low px-2.5 py-0.5 text-xs font-medium">{{ f }}</span>
          </div>
        </div>
      </AppCard>

      <AppCard title="Cara perpanjang" class="mt-4">
        <ul class="list-disc space-y-1 pl-5 text-sm text-secondary">
          <li>Tanpa potong otomatis — tagihan diterbitkan admin layanan.</li>
          <li>Aktivasi setelah pembayaran terkonfirmasi manual.</li>
        </ul>
      </AppCard>
    </template>
  </div>
</template>
