<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { api, downloadFile } from '../lib/api'
import { rupiah, tanggal } from '../lib/format'
import { useCompany } from '../composables/useCompany'
import { useToast } from '../composables/useToast'
import { useAccessStore } from '../stores/access'
import { useAsync } from '../composables/useAsync'
import { usePagedList } from '../composables/usePagedList'
import AppBadge from '../components/ui/AppBadge.vue'
import AppBanner from '../components/ui/AppBanner.vue'
import AppButton from '../components/ui/AppButton.vue'
import AppCard from '../components/ui/AppCard.vue'
import AppDateInput from '../components/ui/AppDateInput.vue'
import AppEmpty from '../components/ui/AppEmpty.vue'
import AppField from '../components/ui/AppField.vue'
import AppPageHeader from '../components/ui/AppPageHeader.vue'
import AppPager from '../components/ui/AppPager.vue'
import AppSkeleton from '../components/ui/AppSkeleton.vue'
import AppTable from '../components/ui/AppTable.vue'
import AppTabs from '../components/ui/AppTabs.vue'

interface Baris {
  account?: { code?: string; name?: string }
  debit?: string | number
  credit?: string | number
}
interface Entri {
  id: number
  branch_id?: number | null
  date?: string
  memo?: string
  source_type?: string
  source_id?: number | null
  lines?: Baris[]
}

const SUMBER: Record<string, string> = {
  sale: 'Penjualan',
  sale_void: 'Void nota',
  sale_return: 'Retur',
  po_receipt: 'Terima barang PO',
  payroll_accrue: 'Beban gaji',
  payroll_pay: 'Bayar gaji',
  expense: 'Biaya',
  bill_payment: 'Bayar hutang',
  receivable: 'Piutang baru',
  receivable_payment: 'Cicilan piutang',
}

const { auth, companyId } = useCompany()
const access = useAccessStore()
const toast = useToast()
const tab = ref('jurnal')
const exporting = ref<'' | 'xlsx' | 'pdf'>('')
const hariIni = new Date()
const dari = ref(
  new Date(hariIni.getFullYear(), hariIni.getMonth(), 1).toLocaleDateString('sv-SE'),
)
const sampai = ref(hariIni.toLocaleDateString('sv-SE'))
const tabs = [
  { key: 'jurnal', label: 'Jurnal' },
  { key: 'neraca', label: 'Neraca saldo' },
]
const {
  rows: list,
  meta,
  loading: loadingJurnal,
  error: errorJurnal,
  load: muatJurnal,
  setPage,
  setPerPage,
} = usePagedList<Entri>(
  (page, perPage) =>
    api.getPage<Entri>(
      `/companies/${companyId.value}/journal-entries?per_page=${perPage}&page=${page}&from=${dari.value}&to=${sampai.value}`,
      auth.token,
    ),
  { perPage: 15 },
)
const { loading: loadingNeraca, error: errorNeraca, run } = useAsync()
const error = computed(() => errorJurnal.value || errorNeraca.value)
const memuat = computed(() => loadingJurnal.value || loadingNeraca.value)
const periodeLabel = computed(() => `${tanggal(dari.value)} - ${tanggal(sampai.value)}`)
const neraca = ref<{
  rows: { code: string; name: string; debit: string | number; credit: string | number }[]
  total_debit: string | number
  total_credit: string | number
} | null>(null)

const boleh = computed(() => access.can('keuangan.lihat'))

async function muatNeraca() {
  const res = await run(() =>
    api.get(
      `/companies/${companyId.value}/reports/trial-balance?from=${dari.value}&to=${sampai.value}`,
      auth.token,
    ),
  )
  if (res) neraca.value = res as typeof neraca.value
}

async function unduhNeraca(format: 'xlsx' | 'pdf') {
  if (!neraca.value || exporting.value) return
  exporting.value = format

  try {
    const path = `/companies/${companyId.value}/reports/trial-balance/export?from=${dari.value}&to=${sampai.value}&format=${format}`
    await downloadFile(
      path,
      `neraca-saldo-${dari.value}_${sampai.value}.${format}`,
      auth.token,
      format === 'pdf'
        ? 'application/pdf'
        : 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    )
    toast.success(`${format === 'pdf' ? 'PDF' : 'Excel'} diunduh.`)
  } catch (e) {
    toast.error(e instanceof Error ? e.message : 'Gagal mengunduh neraca saldo.')
  } finally {
    exporting.value = ''
  }
}

const totalBaris = (e: Entri, k: 'debit' | 'credit') =>
  (e.lines ?? []).reduce((s, l) => s + Number(l[k] ?? 0), 0)
const labelSumber = (source?: string) => (source ? (SUMBER[source] ?? source) : 'Lainnya')

function tampilkan() {
  muatJurnal(1)
  muatNeraca()
}

onMounted(async () => {
  await access.load(companyId.value, auth.token)
  await muatJurnal()
  await muatNeraca()
})
</script>

<template>
  <div>
    <AppPageHeader
      eyebrow="Buku besar otomatis"
      title="Jurnal & neraca saldo"
      subtitle="Jurnal dibuat otomatis oleh sistem. Pilih rentang tanggal untuk meninjau mutasi dan neraca saldo."
      icon="menu_book"
    />

    <AppBanner tone="red" :message="error" />

    <AppEmpty
      v-if="!boleh"
      icon="lock"
      title="Akses terbatas"
      message="Halaman ini hanya untuk yang diberi wewenang melihat keuangan."
    />

    <template v-else>
      <AppTabs
        :model-value="tab"
        :items="tabs"
        label="Jenis laporan"
        class="mb-4"
        @update:model-value="tab = $event"
      />

      <div class="mb-4 flex flex-wrap items-end gap-2">
        <AppField label="Dari" class="w-full sm:w-40">
          <AppDateInput v-model="dari" />
        </AppField>
        <AppField label="Sampai" class="w-full sm:w-40">
          <AppDateInput v-model="sampai" />
        </AppField>
        <AppButton :loading="memuat" @click="tampilkan">Tampilkan</AppButton>
        <template v-if="tab === 'neraca'">
          <AppButton
            variant="ghost"
            :loading="exporting === 'xlsx'"
            :disabled="!neraca || exporting !== ''"
            @click="unduhNeraca('xlsx')"
            >Ekspor Excel</AppButton
          >
          <AppButton
            variant="ghost"
            :loading="exporting === 'pdf'"
            :disabled="!neraca || exporting !== ''"
            @click="unduhNeraca('pdf')"
            >Ekspor PDF</AppButton
          >
        </template>
        <p class="text-xs text-secondary sm:ml-auto sm:pb-2.5">{{ periodeLabel }}</p>
      </div>

      <div v-if="tab === 'jurnal'">
        <div v-if="loadingJurnal" class="space-y-3" aria-busy="true">
          <AppCard v-for="i in 3" :key="i">
            <AppSkeleton :lines="5" />
          </AppCard>
          <span class="sr-only" role="status">Memuat jurnal...</span>
        </div>

        <AppEmpty
          v-else-if="!list.length"
          icon="menu_book"
          title="Tidak ada entri jurnal"
          message="Tidak ada jurnal pada rentang tanggal ini. Coba perluas periode."
        />

        <div v-else class="space-y-3">
          <AppCard v-for="e in list" :key="e.id">
            <div class="flex flex-wrap items-start justify-between gap-3">
              <div class="min-w-0">
                <div class="flex flex-wrap items-center gap-2">
                  <p class="font-display font-semibold text-ink">#{{ e.id }}</p>
                  <AppBadge tone="blue">{{ labelSumber(e.source_type) }}</AppBadge>
                </div>
                <p class="mt-1 text-sm leading-relaxed text-secondary">
                  {{ e.memo || 'Tanpa memo' }}
                </p>
              </div>
              <p class="text-xs text-secondary">{{ tanggal(e.date) }}</p>
            </div>

            <AppTable
              class="mt-3"
              dense
              :colspan="4"
              :caption="`Rincian jurnal #${e.id}`"
              :empty="!(e.lines?.length)"
              empty-message="Tidak ada baris jurnal."
            >
              <template #head>
                <tr>
                  <th>Kode</th>
                  <th>Akun</th>
                  <th class="text-right">Debit</th>
                  <th class="text-right">Kredit</th>
                </tr>
              </template>
              <tr v-for="(l, i) in e.lines ?? []" :key="i">
                <td class="font-mono text-xs text-secondary">{{ l.account?.code }}</td>
                <td>{{ l.account?.name }}</td>
                <td class="text-right tabular-nums">{{ rupiah(l.debit ?? 0) }}</td>
                <td class="text-right tabular-nums">{{ rupiah(l.credit ?? 0) }}</td>
              </tr>
              <template #foot>
                <tr class="font-semibold">
                  <td colspan="2" class="text-xs text-secondary">Total (debit = kredit)</td>
                  <td class="text-right tabular-nums">{{ rupiah(totalBaris(e, 'debit')) }}</td>
                  <td class="text-right tabular-nums">{{ rupiah(totalBaris(e, 'credit')) }}</td>
                </tr>
              </template>
            </AppTable>
          </AppCard>
        </div>

        <AppPager :meta="meta" @page="setPage" @update:per-page="setPerPage" />
      </div>

      <AppCard v-else>
        <div class="flex flex-wrap items-center justify-between gap-2">
          <div>
            <h2 class="font-display font-semibold text-ink">Neraca saldo</h2>
            <p class="mt-0.5 text-xs text-secondary">{{ periodeLabel }}</p>
          </div>
          <AppBadge tone="gray">Trial balance</AppBadge>
        </div>

        <AppTable
          class="mt-3"
          :loading="loadingNeraca"
          :empty="!neraca?.rows.length"
          :colspan="4"
          caption="Neraca saldo"
          empty-title="Belum ada data"
          empty-message="Atur rentang tanggal lalu tekan Tampilkan."
        >
          <template #head>
            <tr>
              <th>Kode</th>
              <th>Akun</th>
              <th class="text-right">Debit</th>
              <th class="text-right">Kredit</th>
            </tr>
          </template>
          <tr v-for="r in neraca?.rows ?? []" :key="r.code">
            <td class="font-mono text-xs text-secondary">{{ r.code }}</td>
            <td>{{ r.name }}</td>
            <td class="text-right tabular-nums">{{ rupiah(r.debit) }}</td>
            <td class="text-right tabular-nums">{{ rupiah(r.credit) }}</td>
          </tr>
          <template v-if="neraca" #foot>
            <tr class="font-semibold">
              <td colspan="2">Total</td>
              <td class="text-right tabular-nums">{{ rupiah(neraca.total_debit) }}</td>
              <td class="text-right tabular-nums">{{ rupiah(neraca.total_credit) }}</td>
            </tr>
          </template>
        </AppTable>

        <p
          v-if="
            neraca?.rows.length && Number(neraca.total_debit) !== Number(neraca.total_credit)
          "
          class="mt-3 text-sm text-danger"
        >
          Total tidak seimbang — hubungi pengembang.
        </p>
        <p v-else-if="neraca?.rows.length" class="mt-2 text-xs text-secondary">
          Total debit dan kredit seimbang.
        </p>
      </AppCard>
    </template>
  </div>
</template>
