<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { api } from '../lib/api'
import { rupiah, tanggal } from '../lib/format'
import { useCompany } from '../composables/useCompany'
import { useAccessStore } from '../stores/access'
import { useAsync } from '../composables/useAsync'
import AppBarChart from '../components/ui/AppBarChart.vue'
import AppBanner from '../components/ui/AppBanner.vue'
import AppButton from '../components/ui/AppButton.vue'
import AppCard from '../components/ui/AppCard.vue'
import AppDateInput from '../components/ui/AppDateInput.vue'
import AppEmpty from '../components/ui/AppEmpty.vue'
import AppField from '../components/ui/AppField.vue'
import AppPageHeader from '../components/ui/AppPageHeader.vue'
import AppSelect from '../components/ui/AppSelect.vue'
import AppSkeleton from '../components/ui/AppSkeleton.vue'
import AppStat from '../components/ui/AppStat.vue'
import AppTable from '../components/ui/AppTable.vue'
import AppTabs from '../components/ui/AppTabs.vue'

interface Cabang {
  id: number
  code: string
  name: string
}
interface DailyRow {
  branchId: number
  date: string
  omzet: string | number
  transactions: number
  hppTotal: string | number
  discountTotal: string | number
  taxTotal: string | number
  cashTunai: string | number
  refundTotal: string | number
  netOmzet: string | number
}
interface ConsolidationRow {
  branchId: number
  branchCode: string
  branchName: string
  omzet: number
  transactions: number
  hpp: number
  refunds: number
  net: number
}
interface LedgerDetail {
  code: string
  name: string
  debit: string | number
  credit: string | number
  saldo: string | number
  bersih?: string | number
}

type Tab = 'labarugi' | 'harian' | 'kas' | 'neraca' | 'konsolidasi'

const { auth, companyId } = useCompany()
const access = useAccessStore()
const { loading, error, run } = useAsync()
const tab = ref<Tab>('labarugi')
const cabang = ref<Cabang[]>([])
const branchId = ref<string | number | null>(null)
const hariIni = new Date()
const dari = ref(
  new Date(hariIni.getFullYear(), hariIni.getMonth(), 1).toLocaleDateString('sv-SE'),
)
const sampai = ref(hariIni.toLocaleDateString('sv-SE'))
const hasil = ref<Record<string, unknown> | null>(null)
const okMsg = ref('')

const bolehKeuangan = computed(() => access.can('keuangan.lihat'))
const bolehKonsolidasi = computed(() => access.can('laporan.konsolidasi'))
const tabs = computed(() => [
  { key: 'labarugi', label: 'Laba rugi' },
  { key: 'harian', label: 'Harian' },
  { key: 'kas', label: 'Arus kas' },
  { key: 'neraca', label: 'Neraca' },
  ...(bolehKonsolidasi.value ? [{ key: 'konsolidasi', label: 'Konsolidasi' }] : []),
])
const opsiCabang = computed(() => [
  { value: '', label: 'Semua cabang' },
  ...cabang.value.map((c) => ({ value: c.id, label: `${c.code} — ${c.name}` })),
])
const cabangQ = computed(() =>
  branchId.value === null || branchId.value === '' ? '' : `&branch_id=${branchId.value}`,
)

const angka = (v: unknown) => (v === null || v === undefined || v === '' ? 0 : Number(v))
const rObj = (v: unknown) =>
  v !== null && typeof v === 'object' ? (v as Record<string, unknown>) : {}
const daftar = <T,>(v: unknown): T[] => (Array.isArray(v) ? (v as T[]) : [])
const namaCabang = (id: number) => cabang.value.find((c) => c.id === id)?.name ?? `Cabang #${id}`

const judulLaporan = computed(
  () =>
    ({
      labarugi: 'Laporan laba rugi',
      harian: 'Ringkasan harian',
      kas: 'Laporan arus kas',
      neraca: 'Neraca',
      konsolidasi: 'Laporan konsolidasi',
    })[tab.value],
)
const periodeLabel = computed(() =>
  tab.value === 'neraca'
    ? `Per ${tanggal(String(hasil.value?.['as_of'] ?? sampai.value))}`
    : `${tanggal(dari.value)} — ${tanggal(sampai.value)}`,
)

const dailyRows = computed(() => daftar<DailyRow>(hasil.value?.['rows']))
const dailyTotals = computed(() => rObj(hasil.value?.['totals']))
const consolidationRows = computed(() => daftar<ConsolidationRow>(hasil.value?.['branches']))
const consolidationGrand = computed(() => rObj(hasil.value?.['grand']))
const kasDetail = computed(() => daftar<LedgerDetail>(hasil.value?.['detail']))
const rincianAkun = computed(() => daftar<LedgerDetail>(hasil.value?.['detail']))
const aktiva = computed(() => rObj(hasil.value?.['aktiva']))
const kewajiban = computed(() => rObj(hasil.value?.['kewajiban']))
const aktivaTotal = computed(() => angka(aktiva.value['total']))
const kewajibanTotal = computed(() => angka(kewajiban.value['total']))
const modalTotal = computed(() => angka(hasil.value?.['modal']))
const neracaSeimbang = computed(
  () => Math.abs(aktivaTotal.value - (kewajibanTotal.value + modalTotal.value)) < 0.01,
)
const neracaTanggal = computed(() => String(hasil.value?.['as_of'] ?? sampai.value))

const akunAktiva = computed(() => [
  { label: 'Kas & bank', value: angka(aktiva.value['kas']) },
  { label: 'Piutang usaha', value: angka(aktiva.value['piutang']) },
  { label: 'Persediaan', value: angka(aktiva.value['persediaan']) },
])
const akunKewajiban = computed(() => [
  { label: 'Hutang supplier', value: angka(kewajiban.value['hutang_supplier']) },
  { label: 'Hutang pajak', value: angka(kewajiban.value['hutang_pajak']) },
  { label: 'Hutang gaji', value: angka(kewajiban.value['hutang_gaji']) },
])

const grafikHarian = computed(() => {
  const perTanggal = new Map<string, number>()
  for (const row of dailyRows.value) {
    perTanggal.set(row.date, (perTanggal.get(row.date) ?? 0) + angka(row.netOmzet))
  }
  const entries = [...perTanggal.entries()].sort(([a], [b]) => a.localeCompare(b))
  return {
    labels: entries.map(([d]) => d.slice(5)),
    values: entries.map(([, v]) => v),
  }
})
const grafikKonsolidasi = computed(() => ({
  labels: consolidationRows.value.map((row) => row.branchCode),
  values: consolidationRows.value.map((row) => row.net),
}))

async function tampilkan() {
  okMsg.value = ''
  hasil.value = null
  if (!bolehKeuangan.value) return
  const base = `/companies/${companyId.value}`
  const q = `?from=${dari.value}&to=${sampai.value}${cabangQ.value}`
  const path =
    tab.value === 'labarugi'
      ? `${base}/reports/profit-loss${q}`
      : tab.value === 'harian'
        ? `${base}/reports/daily${q}`
        : tab.value === 'kas'
          ? `${base}/reports/cash-flow${q}`
          : tab.value === 'neraca'
            ? `${base}/reports/balance-sheet?as_of=${sampai.value}`
            : `${base}/reports/consolidation?from=${dari.value}&to=${sampai.value}`
  const res = await run(() => api.get<Record<string, unknown>>(path, auth.token))
  if (res) hasil.value = res
}

async function segarkan() {
  const res = await run(() =>
    api.post(
      `/companies/${companyId.value}/reports/refresh`,
      {
        date: sampai.value,
        ...(branchId.value ? { branch_id: Number(branchId.value) } : {}),
      },
      auth.token,
    ),
  )
  if (res || !error.value) {
    okMsg.value = 'Ringkasan harian dibangun ulang.'
    tampilkan()
  }
}

function cetak() {
  window.print()
}

function csvCell(value: unknown) {
  const text = value === null || value === undefined ? '' : String(value)
  return `"${text.replace(/"/g, '""')}"`
}

function unduhCsv() {
  let rows: (string | number)[][] = []
  if (tab.value === 'labarugi') {
    rows = [
      ['Kode', 'Nama akun', 'Saldo'],
      ...rincianAkun.value.map((d) => [d.code, d.name, angka(d.saldo)]),
    ]
  } else if (tab.value === 'kas') {
    rows = [
      ['Kode', 'Nama akun', 'Debit', 'Kredit', 'Bersih'],
      ...kasDetail.value.map((d) => [
        d.code,
        d.name,
        angka(d.debit),
        angka(d.credit),
        angka(d.bersih),
      ]),
    ]
  } else if (tab.value === 'harian') {
    rows = [
      [
        'Tanggal',
        'Cabang',
        'Omzet',
        'Transaksi',
        'HPP',
        'Diskon',
        'Pajak',
        'Tunai',
        'Refund',
        'Net',
      ],
      ...dailyRows.value.map((r) => [
        r.date,
        namaCabang(r.branchId),
        angka(r.omzet),
        r.transactions,
        angka(r.hppTotal),
        angka(r.discountTotal),
        angka(r.taxTotal),
        angka(r.cashTunai),
        angka(r.refundTotal),
        angka(r.netOmzet),
      ]),
      [
        'TOTAL',
        '',
        angka(dailyTotals.value['omzet']),
        angka(dailyTotals.value['transactions']),
        angka(dailyTotals.value['hpp']),
        angka(dailyTotals.value['discount']),
        angka(dailyTotals.value['tax']),
        angka(dailyTotals.value['cash']),
        angka(dailyTotals.value['refunds']),
        angka(dailyTotals.value['net']),
      ],
    ]
  } else if (tab.value === 'neraca') {
    rows = [
      ['Kelompok', 'Akun', 'Nilai'],
      ...akunAktiva.value.map((a) => ['Aktiva', a.label, a.value]),
      ['Aktiva', 'Total aktiva', aktivaTotal.value],
      ...akunKewajiban.value.map((a) => ['Kewajiban', a.label, a.value]),
      ['Kewajiban', 'Total kewajiban', kewajibanTotal.value],
      ['Modal', 'Modal', modalTotal.value],
    ]
  } else {
    rows = [
      ['Cabang', 'Omzet', 'Transaksi', 'HPP', 'Refund', 'Net'],
      ...consolidationRows.value.map((r) => [
        `${r.branchCode} — ${r.branchName}`,
        r.omzet,
        r.transactions,
        r.hpp,
        r.refunds,
        r.net,
      ]),
      [
        'TOTAL',
        angka(consolidationGrand.value['omzet']),
        angka(consolidationGrand.value['transactions']),
        angka(consolidationGrand.value['hpp']),
        angka(consolidationGrand.value['refunds']),
        angka(consolidationGrand.value['net']),
      ],
    ]
  }

  if (rows.length < 2) {
    okMsg.value = 'Belum ada data untuk diekspor.'
    return
  }
  const csv = '\uFEFF' + rows.map((row) => row.map(csvCell).join(',')).join('\r\n')
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `${tab.value}-${dari.value}-${sampai.value}.csv`
  a.click()
  URL.revokeObjectURL(url)
  okMsg.value = 'CSV diunduh.'
}

watch(tab, () => {
  if (bolehKeuangan.value) tampilkan()
})

onMounted(async () => {
  await access.load(companyId.value, auth.token)
  cabang.value =
    (await api
      .get<Cabang[]>(`/companies/${companyId.value}/branches`, auth.token)
      .catch(() => [])) ?? []
  await tampilkan()
})
</script>

<template>
  <div>
    <AppPageHeader
      class="no-print"
      eyebrow="Kinerja usaha"
      title="Laporan keuangan"
      subtitle="Laba rugi, ringkasan harian, arus kas, neraca, dan konsolidasi cabang."
      icon="assessment"
    >
      <template #actions>
        <AppButton variant="ghost" @click="unduhCsv">Ekspor CSV</AppButton>
        <AppButton variant="ghost" @click="cetak">Cetak</AppButton>
      </template>
    </AppPageHeader>

    <AppBanner tone="green" :message="okMsg" />
    <AppBanner tone="red" :message="error" />
    <AppEmpty
      v-if="!bolehKeuangan"
      icon="lock"
      title="Akses terbatas"
      message="Halaman ini hanya untuk yang diberi wewenang melihat keuangan."
    />

    <template v-else>
      <AppTabs v-model="tab" :items="tabs" label="Jenis laporan" class="no-print" />

      <div class="no-print mt-3 flex flex-wrap items-end gap-2">
        <template v-if="tab !== 'konsolidasi' && tab !== 'neraca'">
          <AppField label="Dari" class="w-40">
            <AppDateInput v-model="dari" />
          </AppField>
          <AppField label="Sampai" class="w-40">
            <AppDateInput v-model="sampai" />
          </AppField>
        </template>
        <AppField v-if="tab === 'neraca'" label="Per tanggal" class="w-44">
          <AppDateInput v-model="sampai" />
        </AppField>
        <AppField v-if="tab !== 'konsolidasi' && tab !== 'neraca'" label="Cabang" class="w-56">
          <AppSelect v-model="branchId" :options="opsiCabang" />
        </AppField>
        <AppButton :loading="loading" @click="tampilkan">Tampilkan</AppButton>
        <AppButton
          v-if="tab === 'harian' || tab === 'konsolidasi'"
          variant="ghost"
          :loading="loading"
          @click="segarkan"
          >Bangun ulang ringkasan</AppButton
        >
      </div>

      <div class="print-only mb-4 border-b border-outline pb-3">
        <p class="font-display text-lg font-bold">{{ auth.companyName ?? 'Aurevant' }}</p>
        <p class="text-sm">{{ judulLaporan }}</p>
        <p class="text-xs text-secondary">{{ periodeLabel }}</p>
      </div>

      <div v-if="loading" class="mt-4" aria-busy="true">
        <div class="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
          <AppCard v-for="i in 4" :key="i">
            <AppSkeleton :lines="2" />
          </AppCard>
        </div>
        <AppCard class="mt-4">
          <AppSkeleton :lines="6" />
        </AppCard>
        <span class="sr-only" role="status">Menghitung laporan...</span>
      </div>

      <template v-else-if="hasil">
        <div v-if="tab === 'labarugi'" class="mt-4">
          <div class="grid gap-3 sm:grid-cols-2 lg:grid-cols-3 2xl:grid-cols-5">
            <AppStat label="Pendapatan" :value="rupiah(angka(hasil['pendapatan']))" icon="trending_up" />
            <AppStat label="HPP" :value="rupiah(angka(hasil['hpp']))" icon="inventory_2" />
            <AppStat label="Laba kotor" :value="rupiah(angka(hasil['laba_kotor']))" icon="account_balance_wallet" />
            <AppStat label="Beban" :value="rupiah(angka(hasil['beban']))" icon="receipt_long" tone="warning" />
            <AppStat
              label="Laba bersih"
              :value="rupiah(angka(hasil['laba_bersih']))"
              icon="savings"
              :tone="angka(hasil['laba_bersih']) >= 0 ? 'positive' : 'negative'"
            />
          </div>
          <AppCard class="mt-4">
            <h2 class="font-display font-semibold">Rincian akun</h2>
            <AppTable class="mt-2" :empty="!rincianAkun.length" :colspan="3" empty-message="Belum ada jurnal pada periode ini.">
              <template #head>
                <tr>
                  <th>Kode</th>
                  <th>Nama akun</th>
                  <th class="text-right">Saldo</th>
                </tr>
              </template>
              <tr v-for="d in rincianAkun" :key="d.code">
                <td class="font-mono text-xs text-secondary">{{ d.code }}</td>
                <td>{{ d.name }}</td>
                <td class="text-right font-medium tabular-nums">{{ rupiah(d.saldo) }}</td>
              </tr>
            </AppTable>
          </AppCard>
        </div>

        <div v-if="tab === 'kas'" class="mt-4">
          <div class="grid gap-3 sm:grid-cols-3">
            <AppStat label="Kas masuk" :value="rupiah(angka(hasil['kas_masuk']))" icon="south_west" tone="positive" />
            <AppStat label="Kas keluar" :value="rupiah(angka(hasil['kas_keluar']))" icon="north_east" tone="negative" />
            <AppStat label="Kas bersih" :value="rupiah(angka(hasil['kas_bersih']))" icon="account_balance" />
          </div>
          <AppCard class="mt-4">
            <h2 class="font-display font-semibold">Rincian arus kas</h2>
            <AppTable class="mt-2" :empty="!kasDetail.length" :colspan="5" empty-message="Belum ada mutasi kas.">
              <template #head>
                <tr>
                  <th>Kode</th>
                  <th>Nama akun</th>
                  <th class="text-right">Debit</th>
                  <th class="text-right">Kredit</th>
                  <th class="text-right">Bersih</th>
                </tr>
              </template>
              <tr v-for="d in kasDetail" :key="d.code">
                <td class="font-mono text-xs text-secondary">{{ d.code }}</td>
                <td>{{ d.name }}</td>
                <td class="text-right tabular-nums">{{ rupiah(d.debit) }}</td>
                <td class="text-right tabular-nums">{{ rupiah(d.credit) }}</td>
                <td class="text-right font-medium tabular-nums">{{ rupiah(d.bersih) }}</td>
              </tr>
            </AppTable>
          </AppCard>
        </div>

        <div v-if="tab === 'harian'" class="mt-4">
          <div class="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
            <AppStat label="Omzet" :value="rupiah(angka(dailyTotals['omzet']))" icon="payments" />
            <AppStat label="Transaksi" :value="angka(dailyTotals['transactions']).toLocaleString('id-ID')" icon="receipt_long" />
            <AppStat label="Refund" :value="rupiah(angka(dailyTotals['refunds']))" icon="undo" tone="warning" />
            <AppStat label="Net omzet" :value="rupiah(angka(dailyTotals['net']))" icon="insights" tone="positive" />
          </div>
          <AppCard class="mt-4">
            <div class="flex flex-wrap items-center justify-between gap-2">
              <h2 class="font-display font-semibold">Tren net omzet harian</h2>
              <p class="text-xs text-secondary">Ringkasan per tanggal</p>
            </div>
            <AppBarChart
              class="mt-3"
              :values="grafikHarian.values"
              :labels="grafikHarian.labels"
              :format-value="rupiah"
              label="Tren net omzet harian"
            />
          </AppCard>
          <AppCard class="mt-4">
            <h2 class="font-display font-semibold">Ringkasan harian per cabang</h2>
            <AppTable class="mt-2" :empty="!dailyRows.length" :colspan="10" empty-message="Belum ada ringkasan pada periode ini.">
              <template #head>
                <tr>
                  <th>Tanggal</th>
                  <th>Cabang</th>
                  <th class="text-right">Omzet</th>
                  <th class="text-right">Trx</th>
                  <th class="text-right">HPP</th>
                  <th class="text-right">Diskon</th>
                  <th class="text-right">Pajak</th>
                  <th class="text-right">Tunai</th>
                  <th class="text-right">Refund</th>
                  <th class="text-right">Net</th>
                </tr>
              </template>
              <tr v-for="r in dailyRows" :key="`${r.branchId}-${r.date}`">
                <td class="whitespace-nowrap">{{ tanggal(r.date) }}</td>
                <td>{{ namaCabang(r.branchId) }}</td>
                <td class="text-right tabular-nums">{{ rupiah(r.omzet) }}</td>
                <td class="text-right tabular-nums">{{ r.transactions }}</td>
                <td class="text-right tabular-nums">{{ rupiah(r.hppTotal) }}</td>
                <td class="text-right tabular-nums">{{ rupiah(r.discountTotal) }}</td>
                <td class="text-right tabular-nums">{{ rupiah(r.taxTotal) }}</td>
                <td class="text-right tabular-nums">{{ rupiah(r.cashTunai) }}</td>
                <td class="text-right tabular-nums">{{ rupiah(r.refundTotal) }}</td>
                <td class="text-right font-medium tabular-nums">{{ rupiah(r.netOmzet) }}</td>
              </tr>
              <template #foot>
                <tr class="font-semibold">
                  <td colspan="2">Total</td>
                  <td class="text-right tabular-nums">{{ rupiah(angka(dailyTotals['omzet'])) }}</td>
                  <td class="text-right tabular-nums">{{ angka(dailyTotals['transactions']) }}</td>
                  <td class="text-right tabular-nums">{{ rupiah(angka(dailyTotals['hpp'])) }}</td>
                  <td class="text-right tabular-nums">{{ rupiah(angka(dailyTotals['discount'])) }}</td>
                  <td class="text-right tabular-nums">{{ rupiah(angka(dailyTotals['tax'])) }}</td>
                  <td class="text-right tabular-nums">{{ rupiah(angka(dailyTotals['cash'])) }}</td>
                  <td class="text-right tabular-nums">{{ rupiah(angka(dailyTotals['refunds'])) }}</td>
                  <td class="text-right tabular-nums">{{ rupiah(angka(dailyTotals['net'])) }}</td>
                </tr>
              </template>
            </AppTable>
          </AppCard>
        </div>

        <div v-if="tab === 'neraca'" class="mt-4">
          <div class="grid gap-3 sm:grid-cols-3">
            <AppStat label="Total aktiva" :value="rupiah(aktivaTotal)" icon="account_balance" />
            <AppStat label="Total kewajiban" :value="rupiah(kewajibanTotal)" icon="request_quote" tone="warning" />
            <AppStat label="Modal" :value="rupiah(modalTotal)" icon="savings" />
          </div>
          <AppCard class="mt-4">
            <div class="flex flex-wrap items-center justify-between gap-2">
              <h2 class="font-display font-semibold">Neraca per {{ tanggal(neracaTanggal) }}</h2>
              <p class="text-xs font-medium" :class="neracaSeimbang ? 'text-ok' : 'text-danger'">
                {{ neracaSeimbang ? 'Seimbang' : 'Tidak seimbang' }}
              </p>
            </div>
            <div class="mt-3 grid gap-4 md:grid-cols-3">
              <div>
                <p class="text-xs font-semibold uppercase tracking-wider text-secondary">Aktiva</p>
                <dl class="mt-2 space-y-1.5 text-sm">
                  <div v-for="a in akunAktiva" :key="a.label" class="flex justify-between gap-3">
                    <dt class="text-secondary">{{ a.label }}</dt>
                    <dd class="tabular-nums">{{ rupiah(a.value) }}</dd>
                  </div>
                  <div class="flex justify-between gap-3 border-t border-outline/60 pt-1.5 font-semibold">
                    <dt>Total</dt>
                    <dd class="tabular-nums">{{ rupiah(aktivaTotal) }}</dd>
                  </div>
                </dl>
              </div>
              <div>
                <p class="text-xs font-semibold uppercase tracking-wider text-secondary">Kewajiban</p>
                <dl class="mt-2 space-y-1.5 text-sm">
                  <div v-for="a in akunKewajiban" :key="a.label" class="flex justify-between gap-3">
                    <dt class="text-secondary">{{ a.label }}</dt>
                    <dd class="tabular-nums">{{ rupiah(a.value) }}</dd>
                  </div>
                  <div class="flex justify-between gap-3 border-t border-outline/60 pt-1.5 font-semibold">
                    <dt>Total</dt>
                    <dd class="tabular-nums">{{ rupiah(kewajibanTotal) }}</dd>
                  </div>
                </dl>
              </div>
              <div>
                <p class="text-xs font-semibold uppercase tracking-wider text-secondary">Modal</p>
                <dl class="mt-2 space-y-1.5 text-sm">
                  <div class="flex justify-between gap-3">
                    <dt class="text-secondary">Modal pemilik</dt>
                    <dd class="tabular-nums">{{ rupiah(modalTotal) }}</dd>
                  </div>
                  <div class="flex justify-between gap-3 border-t border-outline/60 pt-1.5 font-semibold">
                    <dt>Total</dt>
                    <dd class="tabular-nums">{{ rupiah(modalTotal) }}</dd>
                  </div>
                </dl>
              </div>
            </div>
          </AppCard>
        </div>

        <div v-if="tab === 'konsolidasi'" class="mt-4">
          <div class="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
            <AppStat label="Omzet grup" :value="rupiah(angka(consolidationGrand['omzet']))" icon="payments" />
            <AppStat label="Transaksi" :value="angka(consolidationGrand['transactions']).toLocaleString('id-ID')" icon="receipt_long" />
            <AppStat label="HPP" :value="rupiah(angka(consolidationGrand['hpp']))" icon="inventory_2" />
            <AppStat label="Net omzet" :value="rupiah(angka(consolidationGrand['net']))" icon="insights" tone="positive" />
          </div>
          <AppCard class="mt-4">
            <h2 class="font-display font-semibold">Net omzet per cabang</h2>
            <AppBarChart
              class="mt-3"
              :values="grafikKonsolidasi.values"
              :labels="grafikKonsolidasi.labels"
              :format-value="rupiah"
              label="Net omzet per cabang"
            />
          </AppCard>
          <AppCard class="mt-4">
            <h2 class="font-display font-semibold">Konsolidasi semua cabang</h2>
            <AppTable class="mt-2" :empty="!consolidationRows.length" :colspan="6" empty-message="Belum ada data konsolidasi.">
              <template #head>
                <tr>
                  <th>Cabang</th>
                  <th class="text-right">Omzet</th>
                  <th class="text-right">Trx</th>
                  <th class="text-right">HPP</th>
                  <th class="text-right">Refund</th>
                  <th class="text-right">Net</th>
                </tr>
              </template>
              <tr v-for="r in consolidationRows" :key="r.branchId">
                <td>
                  <p class="font-medium">{{ r.branchName }}</p>
                  <p class="font-mono text-xs text-secondary">{{ r.branchCode }}</p>
                </td>
                <td class="text-right tabular-nums">{{ rupiah(r.omzet) }}</td>
                <td class="text-right tabular-nums">{{ r.transactions }}</td>
                <td class="text-right tabular-nums">{{ rupiah(r.hpp) }}</td>
                <td class="text-right tabular-nums">{{ rupiah(r.refunds) }}</td>
                <td class="text-right font-medium tabular-nums">{{ rupiah(r.net) }}</td>
              </tr>
              <template #foot>
                <tr class="font-semibold">
                  <td>Total grup</td>
                  <td class="text-right tabular-nums">{{ rupiah(angka(consolidationGrand['omzet'])) }}</td>
                  <td class="text-right tabular-nums">{{ angka(consolidationGrand['transactions']) }}</td>
                  <td class="text-right tabular-nums">{{ rupiah(angka(consolidationGrand['hpp'])) }}</td>
                  <td class="text-right tabular-nums">{{ rupiah(angka(consolidationGrand['refunds'])) }}</td>
                  <td class="text-right tabular-nums">{{ rupiah(angka(consolidationGrand['net'])) }}</td>
                </tr>
              </template>
            </AppTable>
          </AppCard>
        </div>
      </template>

      <AppEmpty
        v-else
        icon="assessment"
        title="Belum ada laporan"
        message="Pilih periode dan cabang, lalu tekan Tampilkan untuk memuat laporan."
      >
        <AppButton @click="tampilkan">Tampilkan laporan</AppButton>
      </AppEmpty>
    </template>
  </div>
</template>

