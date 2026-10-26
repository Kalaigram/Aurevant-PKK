<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { api } from '../lib/api'
import { rupiah, tanggal, terbilang } from '../lib/format'
import { labelPayrollStatus } from '../lib/labels'
import { useCompany } from '../composables/useCompany'
import { useAccessStore } from '../stores/access'
import { useAsync } from '../composables/useAsync'
import AppBadge from '../components/ui/AppBadge.vue'
import AppBanner from '../components/ui/AppBanner.vue'
import AppButton from '../components/ui/AppButton.vue'
import AppCard from '../components/ui/AppCard.vue'
import AppEmpty from '../components/ui/AppEmpty.vue'
import AppField from '../components/ui/AppField.vue'
import AppIcon from '../components/ui/AppIcon.vue'
import AppPageHeader from '../components/ui/AppPageHeader.vue'
import AppSelect from '../components/ui/AppSelect.vue'
import AppSkeleton from '../components/ui/AppSkeleton.vue'
import AppTable from '../components/ui/AppTable.vue'

interface SlipLine {
  label: string
  amount: string
  isManual: boolean
}
interface Periode {
  id: number
  name: string
  from: string
  to: string
  divisor: number
  status: string
}
interface Slip {
  period: Periode
  lines: SlipLine[]
  net: number
}

const { auth, companyId } = useCompany()
const access = useAccessStore()
const { loading, error, run } = useAsync()
const periods = ref<Periode[]>([])
const periodId = ref<string | number | null>(null)
const slip = ref<Slip | null>(null)
const nama = ref('')
const uuid = ref('')
const jabatan = ref<string | null>(null)

const opsiPeriode = computed(() =>
  periods.value.map((p) => ({
    value: p.id,
    label: `${p.name} (${tanggal(p.from)} — ${tanggal(p.to)})`,
  })),
)
const terima = computed(() => (slip.value?.lines ?? []).filter((l) => Number(l.amount) >= 0))
const potong = computed(() => (slip.value?.lines ?? []).filter((l) => Number(l.amount) < 0))
const bruto = computed(() => terima.value.reduce((s, l) => s + Number(l.amount), 0))
const totalPotong = computed(() => potong.value.reduce((s, l) => s + Number(l.amount), 0))
const statusTone = computed<'green' | 'amber' | 'blue' | 'gray'>(() => {
  const status = slip.value?.period.status
  return status === 'dibayar' || status === 'disetujui'
    ? 'green'
    : status === 'review'
      ? 'blue'
      : status === 'draft'
        ? 'amber'
        : 'gray'
})

function normalisasi(p: Periode & { date_from?: string; date_to?: string }): Periode {
  return {
    id: p.id,
    name: p.name,
    from: p.from ?? p.date_from ?? '',
    to: p.to ?? p.date_to ?? '',
    divisor: p.divisor,
    status: p.status,
  }
}

async function muatPeriode() {
  periods.value = []
  const me = await run(() =>
    api.get<(Periode & { date_from?: string; date_to?: string })[]>(
      `/companies/${companyId.value}/payroll-periods/me?per_page=50`,
      auth.token,
    ),
  )
  if (me?.length) {
    periods.value = me.map(normalisasi)
  } else if (access.can('gaji.kelola')) {
    const all = await run(() =>
      api.get<(Periode & { date_from?: string; date_to?: string })[]>(
        `/companies/${companyId.value}/payroll-periods?per_page=100`,
        auth.token,
      ),
    )
    if (all) periods.value = all.map(normalisasi)
  }
  const pertama = periods.value[0]
  if (pertama) {
    periodId.value = pertama.id
    await lihat()
  }
}

async function lihat() {
  if (!periodId.value || !auth.user) return
  const found = await run(() =>
    api.get<Slip>(
      `/companies/${companyId.value}/payroll-periods/${periodId.value}/users/${auth.user!.id}/slip`,
      auth.token,
    ),
  )
  slip.value = found
  if (found) {
    const card = await run(() =>
      api.get<{ name: string; employeeUuid: string; company: string; role: string | null }>(
        `/companies/${companyId.value}/users/${auth.user!.id}/card`,
        auth.token,
      ),
    )
    if (card) {
      nama.value = card.name
      uuid.value = card.employeeUuid
      jabatan.value = card.role
    }
  }
}

function cetak() {
  window.print()
}

onMounted(async () => {
  await access.load(companyId.value, auth.token)
  await muatPeriode()
})
</script>

<template>
  <div>
    <AppPageHeader
      class="no-print"
      eyebrow="Kepegawaian & finansial"
      title="Slip gaji karyawan"
      subtitle="Pilih periode untuk melihat rincian penghasilan, potongan, dan gaji bersih."
      icon="receipt_long"
    >
      <template #actions>
        <AppButton variant="ghost" :disabled="!slip" @click="cetak">
          <AppIcon name="print" :size="18" />Cetak slip
        </AppButton>
      </template>
    </AppPageHeader>

    <AppBanner tone="red" :message="error" />

    <AppCard class="no-print" title="Pilih periode">
      <div class="flex flex-col gap-3 sm:flex-row sm:items-end">
        <AppField label="Periode penggajian" class="flex-1">
          <AppSelect
            v-model="periodId"
            :options="opsiPeriode"
            placeholder="Pilih periode"
            :disabled="!periods.length"
          />
        </AppField>
        <AppButton :loading="loading" :disabled="!periodId" @click="lihat">Lihat slip</AppButton>
      </div>
      <p v-if="!periods.length && !loading" class="mt-2 text-sm text-secondary">
        Belum ada periode slip untuk akun Anda. Hubungi HR bila ini keliru.
      </p>
    </AppCard>

    <div v-if="loading && !slip" class="mt-4 space-y-3" aria-hidden="true">
      <AppSkeleton height="7rem" rounded="xl" />
      <AppSkeleton :lines="4" height="0.85rem" rounded="lg" />
    </div>

    <AppEmpty
      v-else-if="!slip && !error"
      title="Slip belum ditampilkan"
      icon="receipt_long"
      message="Pilih periode untuk menampilkan slip gaji."
    />

    <div v-if="slip" class="print-area mt-4 overflow-hidden rounded-xl border border-outline/60 bg-card shadow-sm">
      <div class="border-b border-outline/60 bg-surface-low/50 p-5">
        <div class="flex flex-wrap items-start justify-between gap-3">
          <div>
            <p class="font-display text-lg font-bold text-ink">{{ auth.companyName ?? 'Aurevant' }}</p>
            <p class="mt-1 inline-block rounded bg-primary/10 px-2 py-0.5 text-[11px] font-semibold uppercase tracking-wider text-primary">
              Bukti pembayaran gaji
            </p>
          </div>
          <div class="no-print flex items-center gap-2">
            <AppBadge :tone="statusTone" dot>
              {{ labelPayrollStatus(slip.period.status) }}
            </AppBadge>
          </div>
        </div>

        <div class="mt-4 grid grid-cols-2 gap-3 text-sm md:grid-cols-3">
          <div>
            <p class="text-[11px] uppercase tracking-wider text-secondary">Nama</p>
            <p class="font-semibold text-ink">{{ nama || auth.user?.name }}</p>
          </div>
          <div>
            <p class="text-[11px] uppercase tracking-wider text-secondary">ID karyawan</p>
            <p class="font-mono font-semibold text-ink">{{ uuid || '—' }}</p>
          </div>
          <div>
            <p class="text-[11px] uppercase tracking-wider text-secondary">Jabatan</p>
            <p class="font-semibold text-ink">{{ jabatan ?? '—' }}</p>
          </div>
          <div>
            <p class="text-[11px] uppercase tracking-wider text-secondary">Periode</p>
            <p class="font-semibold text-ink">{{ slip.period.name }}</p>
          </div>
          <div>
            <p class="text-[11px] uppercase tracking-wider text-secondary">Rentang</p>
            <p class="font-semibold text-ink">{{ tanggal(slip.period.from) }} — {{ tanggal(slip.period.to) }}</p>
          </div>
          <div>
            <p class="text-[11px] uppercase tracking-wider text-secondary">Pembagi prorata</p>
            <p class="font-semibold text-ink">1/{{ slip.period.divisor }}</p>
          </div>
        </div>
      </div>

      <div class="grid gap-5 p-5 md:grid-cols-2">
        <div>
          <h3 class="font-display text-sm font-semibold text-ink">Penghasilan</h3>
          <AppTable
            class="mt-2"
            caption="Komponen penghasilan"
            :empty="!terima.length"
            :colspan="2"
            empty-message="Belum ada komponen penghasilan."
          >
            <template #head>
              <tr>
                <th>Komponen</th>
                <th class="text-right">Nominal</th>
              </tr>
            </template>
            <tr v-for="(l, i) in terima" :key="`terima-${i}`">
              <td>{{ l.label }}</td>
              <td class="text-right font-medium tabular-nums">{{ rupiah(l.amount) }}</td>
            </tr>
            <template #foot>
              <tr class="font-semibold">
                <td>Total bruto</td>
                <td class="text-right text-primary">{{ rupiah(bruto) }}</td>
              </tr>
            </template>
          </AppTable>
        </div>

        <div>
          <h3 class="font-display text-sm font-semibold text-ink">Potongan</h3>
          <AppTable
            class="mt-2"
            caption="Komponen potongan"
            :empty="!potong.length"
            :colspan="2"
            empty-message="Tidak ada potongan periode ini."
          >
            <template #head>
              <tr>
                <th>Komponen</th>
                <th class="text-right">Nominal</th>
              </tr>
            </template>
            <tr v-for="(l, i) in potong" :key="`potong-${i}`">
              <td>{{ l.label }}</td>
              <td class="text-right font-medium tabular-nums text-danger">{{ rupiah(l.amount) }}</td>
            </tr>
            <template #foot>
              <tr class="font-semibold">
                <td>Total potongan</td>
                <td class="text-right tabular-nums">{{ rupiah(totalPotong) }}</td>
              </tr>
            </template>
          </AppTable>
        </div>
      </div>

      <div class="mx-5 mb-5 rounded-xl bg-primary p-4 text-white">
        <p class="text-xs uppercase tracking-wider text-white/75">Gaji bersih diterima</p>
        <p class="font-display mt-1 text-2xl font-bold">{{ rupiah(slip.net) }}</p>
        <p class="mt-1 text-sm italic text-white/85">Terbilang: {{ terbilang(slip.net) }}</p>
      </div>

      <p class="px-5 pb-5 text-xs text-secondary">
        Slip ini rahasia dan hanya untuk Anda. Potongan alpa dihitung 1/{{ slip.period.divisor }} per hari.
      </p>
    </div>
  </div>
</template>
