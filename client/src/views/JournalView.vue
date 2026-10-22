<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { api, type PageMeta } from '../lib/api'
import { rupiah, tanggal } from '../lib/format'
import { useCompany } from '../composables/useCompany'
import { useAccessStore } from '../stores/access'
import { useAsync } from '../composables/useAsync'
import AppCard from '../components/ui/AppCard.vue'
import AppBanner from '../components/ui/AppBanner.vue'
import AppButton from '../components/ui/AppButton.vue'
import AppField from '../components/ui/AppField.vue'
import AppEmpty from '../components/ui/AppEmpty.vue'

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
const { loading, error, run } = useAsync()
const tab = ref<'jurnal' | 'neraca'>('jurnal')
const list = ref<Entri[]>([])
const meta = ref<PageMeta | null>(null)
const dari = ref(new Date().toISOString().slice(0, 10))
const sampai = ref(new Date().toISOString().slice(0, 10))
const neraca = ref<{
  rows: { code: string; name: string; debit: string | number; credit: string | number }[]
  total_debit: string | number
  total_credit: string | number
} | null>(null)

const boleh = computed(() => access.can('keuangan.lihat'))

async function muatJurnal(page = 1) {
  const res = await run(() =>
    api.getPage<Entri>(
      `/companies/${companyId.value}/journal-entries?per_page=15&page=${page}&from=${dari.value}&to=${sampai.value}`,
      auth.token,
    ),
  )
  if (res) {
    const raw = res as unknown as { data: Entri[]; meta?: PageMeta } | Entri[]
    if (Array.isArray(raw)) list.value = raw
    else {
      list.value = raw.data
      meta.value = raw.meta ?? null
    }
  }
}

async function muatNeraca() {
  const res = await run(() =>
    api.get(
      `/companies/${companyId.value}/reports/trial-balance?from=${dari.value}&to=${sampai.value}`,
      auth.token,
    ),
  )
  if (res) neraca.value = res as typeof neraca.value
}

const totalBaris = (e: Entri, k: 'debit' | 'credit') =>
  (e.lines ?? []).reduce((s, l) => s + Number(l[k] ?? 0), 0)

onMounted(async () => {
  await access.load(companyId.value, auth.token)
  await muatJurnal()
  await muatNeraca()
})
</script>

<template>
  <div>
    <p class="text-xs font-semibold uppercase tracking-wider text-secondary">Buku besar otomatis</p>
    <h1 class="font-display mb-4 text-xl font-semibold">Jurnal & neraca saldo</h1>
    <AppBanner tone="red" :message="error" />
    <AppEmpty
      v-if="!boleh"
      message="Halaman ini hanya untuk yang diberi wewenang melihat keuangan."
    />
    <template v-else>
      <p class="mb-3 text-xs text-secondary">
        Jurnal dibuat otomatis oleh sistem (tidak ada input manual).
      </p>
      <div class="mb-3 flex flex-wrap items-end gap-2">
        <AppField label="Dari"
          ><input
            v-model="dari"
            type="date"
            class="rounded-lg border border-outline bg-card px-2 py-2 text-sm"
        /></AppField>
        <AppField label="Sampai"
          ><input
            v-model="sampai"
            type="date"
            class="rounded-lg border border-outline bg-card px-2 py-2 text-sm"
        /></AppField>
        <AppButton
          variant="ghost"
          @click="
            () => {
              muatJurnal()
              muatNeraca()
            }
          "
          >Tampilkan</AppButton
        >
        <span class="flex gap-2">
          <AppButton :variant="tab === 'jurnal' ? 'primary' : 'ghost'" @click="tab = 'jurnal'"
            >Jurnal</AppButton
          >
          <AppButton :variant="tab === 'neraca' ? 'primary' : 'ghost'" @click="tab = 'neraca'"
            >Neraca saldo</AppButton
          >
        </span>
      </div>

      <div v-if="tab === 'jurnal'">
        <p v-if="loading" class="text-sm text-secondary">Memuat…</p>
        <AppEmpty v-else-if="!list.length" message="Tidak ada entri jurnal pada rentang ini." />
        <div class="space-y-3">
          <AppCard v-for="e in list" :key="e.id">
            <div class="flex flex-wrap items-center justify-between gap-2 text-sm">
              <strong
                >#{{ e.id }} · {{ SUMBER[e.source_type ?? ''] ?? e.source_type }} —
                {{ e.memo ?? '' }}</strong
              >
              <span class="text-xs text-secondary">{{ tanggal(e.date) }}</span>
            </div>
            <table class="mt-2 w-full text-sm">
              <tbody>
                <tr v-for="(l, i) in e.lines ?? []" :key="i" class="border-t border-outline/50">
                  <td class="py-1 font-mono text-xs text-secondary">{{ l.account?.code }}</td>
                  <td class="py-1">{{ l.account?.name }}</td>
                  <td class="py-1 text-right">{{ rupiah(l.debit ?? 0) }}</td>
                  <td class="py-1 text-right">{{ rupiah(l.credit ?? 0) }}</td>
                </tr>
              </tbody>
              <tfoot>
                <tr class="border-t border-outline font-semibold">
                  <td colspan="2" class="py-1 text-xs text-secondary">Total (debit = kredit)</td>
                  <td class="py-1 text-right">{{ rupiah(totalBaris(e, 'debit')) }}</td>
                  <td class="py-1 text-right">{{ rupiah(totalBaris(e, 'credit')) }}</td>
                </tr>
              </tfoot>
            </table>
          </AppCard>
        </div>
      </div>

      <AppCard v-else>
        <h2 class="font-display font-semibold">Neraca saldo</h2>
        <div v-if="neraca" class="mt-2 overflow-x-auto">
          <table class="w-full min-w-[520px] text-sm">
            <thead>
              <tr class="text-left text-xs uppercase tracking-wider text-secondary">
                <th class="py-1">Kode</th>
                <th class="py-1">Akun</th>
                <th class="py-1 text-right">Debit</th>
                <th class="py-1 text-right">Kredit</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="r in neraca.rows" :key="r.code" class="border-t border-outline/50">
                <td class="py-1 font-mono text-xs">{{ r.code }}</td>
                <td class="py-1">{{ r.name }}</td>
                <td class="py-1 text-right">{{ rupiah(r.debit) }}</td>
                <td class="py-1 text-right">{{ rupiah(r.credit) }}</td>
              </tr>
            </tbody>
            <tfoot>
              <tr class="border-t border-outline font-bold">
                <td colspan="2" class="py-1">Total</td>
                <td class="py-1 text-right">{{ rupiah(neraca.total_debit) }}</td>
                <td class="py-1 text-right">{{ rupiah(neraca.total_credit) }}</td>
              </tr>
            </tfoot>
          </table>
          <p
            v-if="Number(neraca.total_debit) !== Number(neraca.total_credit)"
            class="mt-2 text-sm text-danger"
          >
            Total tidak seimbang — hubungi pengembang.
          </p>
        </div>
        <AppEmpty v-else message="Belum ada data. Atur rentang tanggal lalu tampilkan." />
      </AppCard>
    </template>
  </div>
</template>
