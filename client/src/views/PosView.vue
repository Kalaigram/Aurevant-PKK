<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { api, downloadFile } from '../lib/api'
import { rupiah } from '../lib/format'
import { useCompany } from '../composables/useCompany'
import { useAccessStore } from '../stores/access'
import { useAsync } from '../composables/useAsync'
import AppCard from '../components/ui/AppCard.vue'
import AppBadge from '../components/ui/AppBadge.vue'
import AppBanner from '../components/ui/AppBanner.vue'
import AppButton from '../components/ui/AppButton.vue'
import AppField from '../components/ui/AppField.vue'
import AppEmpty from '../components/ui/AppEmpty.vue'
import AppIcon from '../components/ui/AppIcon.vue'

interface Cabang {
  id: number
  code: string
  name: string
}
interface Item {
  id: number
  code: string
  name: string
  type: string
  unit: string | null
}
interface Harga extends Item {
  price: string
}
interface Baris {
  item: Harga
  qty: number
  discPct: number
  discAmt: number
  taxPct: number
}
interface Bayar {
  method: string
  amount: number
  tendered: number | null
  reference: string
}
interface Struk {
  id: number
  number: string
  status: string
  subtotal: string
  discountTotal: string
  taxTotal: string
  grandTotal: string
  items: { itemId: number; qty: string; price: string; lineTotal: string }[]
  payments: { method: string; amount: string; tendered: string | null }[]
}

const METHODS = ['tunai', 'transfer', 'qris', 'ewallet', 'dp']
const { auth, companyId } = useCompany()
const access = useAccessStore()
const page = useAsync()
const cabang = ref<Cabang[]>([])
const branchId = ref<number | null>(null)
const shiftId = ref<number | null>(null)
const shiftInfo = ref('')
const bukaKas = ref(0)
const tutupKas = ref(0)
const katalog = ref<Harga[]>([])
const cari = ref('')
const keranjang = ref<Baris[]>([])
const bayar = ref<Bayar[]>([{ method: 'tunai', amount: 0, tendered: null, reference: '' }])
const struk = ref<Struk | null>(null)
const okMsg = ref('')
const formMsg = ref('')

const bolehKasir = computed(() => access.can('transaksi.buat'))
const bolehBuka = computed(() => access.can('shift.buka'))
const bolehTutup = computed(() => access.can('shift.tutup'))

const tampilKatalog = computed(() => {
  const q = cari.value.trim().toLowerCase()
  if (!q) return katalog.value
  return katalog.value.filter(
    (i) => i.name.toLowerCase().includes(q) || i.code.toLowerCase().includes(q),
  )
})

function barisTotal(b: Baris) {
  const gross = Number(b.item.price) * b.qty
  const disc = b.discPct > 0 ? (gross * b.discPct) / 100 : b.discAmt
  const tax = ((gross - disc) * b.taxPct) / 100
  return { gross, disc, total: gross - disc + tax }
}
const subtotal = computed(() => keranjang.value.reduce((s, b) => s + barisTotal(b).gross, 0))
const diskon = computed(() => keranjang.value.reduce((s, b) => s + barisTotal(b).disc, 0))
const total = computed(() => keranjang.value.reduce((s, b) => s + barisTotal(b).total, 0))
const dibayar = computed(() => bayar.value.reduce((s, p) => s + (Number(p.amount) || 0), 0))
const kembalian = computed(() => {
  const tunai = bayar.value.find((p) => p.method === 'tunai')
  if (!tunai || !tunai.tendered) return 0
  return Math.max(0, tunai.tendered - total.value)
})

function kunciShift() {
  return `aurevant-shift-${companyId.value}-${branchId.value}`
}

function tambah(item: Harga) {
  const ada = keranjang.value.find((b) => b.item.id === item.id)
  if (ada) ada.qty++
  else keranjang.value.push({ item, qty: 1, discPct: 0, discAmt: 0, taxPct: 0 })
  struk.value = null
}

async function muatKatalog() {
  if (!branchId.value) return
  // harga khusus per cabang: yang tanpa harga disembunyikan dari kasir
  const items =
    (await page.run(() =>
      api.get<Item[]>(`/companies/${companyId.value}/items?per_page=100`, auth.token),
    )) ?? []
  const out: Harga[] = []
  for (const it of items) {
    try {
      const h = await api.get<{ price: string }>(
        `/companies/${companyId.value}/items/${it.id}/prices/${branchId.value}`,
        auth.token,
      )
      out.push({ ...it, price: h.price })
    } catch {
      /* tanpa harga di cabang ini: lewati */
    }
  }
  katalog.value = out
}

async function bukaShift() {
  formMsg.value = ''
  okMsg.value = ''
  if (!branchId.value) return
  const res = await page.run(() =>
    api.post<{ id: number }>(
      `/companies/${companyId.value}/branches/${branchId.value}/shifts`,
      { opening_cash: bukaKas.value },
      auth.token,
    ),
  )
  if (res) {
    shiftId.value = res.id
    localStorage.setItem(kunciShift(), String(res.id))
    shiftInfo.value = `Shift #${res.id} terbuka.`
  } else if (page.error.value.includes('terbuka')) {
    const tersimpan = localStorage.getItem(kunciShift())
    shiftInfo.value = tersimpan
      ? `Sudah ada shift terbuka (#${tersimpan}, dari perangkat ini).`
      : 'Sudah ada shift terbuka (dibuka dari perangkat lain). Minta ID shift ke kasir yang membuka untuk menutupnya.'
    if (tersimpan) shiftId.value = Number(tersimpan)
  }
}

async function tutupShift() {
  formMsg.value = ''
  if (!branchId.value || !shiftId.value) {
    formMsg.value = 'Tidak ada ID shift untuk ditutup.'
    return
  }
  const res = await page.run(() =>
    api.post(
      `/companies/${companyId.value}/branches/${branchId.value}/shifts/${shiftId.value}/close`,
      {
        closing_cash_counted: tutupKas.value,
      },
      auth.token,
    ),
  )
  if (res) {
    const d = res as unknown as { difference?: string }
    okMsg.value = `Shift ditutup. Selisih: ${rupiah(d.difference ?? 0)}.`
    shiftId.value = null
    localStorage.removeItem(kunciShift())
    shiftInfo.value = ''
  }
}

function cetak() {
  window.print()
}

async function unduhStruk() {
  if (!struk.value || !branchId.value) return
  await page.run(() =>
    downloadFile(
      `/companies/${companyId.value}/branches/${branchId.value}/sales/${struk.value!.id}/receipt`,
      `struk-${struk.value!.number}.pdf`,
      auth.token,
      'application/pdf',
    ),
  )
  if (!page.error.value) okMsg.value = 'Struk PDF diunduh.'
}

async function bayarSekarang() {
  formMsg.value = ''
  okMsg.value = ''
  struk.value = null
  if (!branchId.value || !keranjang.value.length) return
  if (Math.abs(dibayar.value - total.value) > 0.009) {
    formMsg.value = `Total bayar (${rupiah(dibayar.value)}) harus pas dengan total belanja (${rupiah(total.value)}).`
    return
  }
  const res = await page.run(() =>
    api.post<{ sale: Struk; duplicate: boolean }>(
      `/companies/${companyId.value}/branches/${branchId.value}/sales`,
      {
        idempotency_key: crypto.randomUUID(),
        items: keranjang.value.map((b) => ({
          item_id: b.item.id,
          qty: b.qty,
          ...(b.discPct > 0 ? { discount_percent: b.discPct } : {}),
          ...(b.discPct <= 0 && b.discAmt > 0 ? { discount_amount: b.discAmt } : {}),
          ...(b.taxPct > 0 ? { tax_percent: b.taxPct } : {}),
        })),
        payments: bayar.value.map((p) => ({
          method: p.method,
          amount: p.amount,
          ...(p.tendered ? { tendered: p.tendered } : {}),
          ...(p.reference ? { reference: p.reference } : {}),
        })),
      },
      auth.token,
    ),
  )
  if (res) {
    struk.value = res.sale
    okMsg.value = res.duplicate
      ? 'Nota sudah pernah dibuat (duplikat dicegah).'
      : 'Pembayaran berhasil.'
    keranjang.value = []
    bayar.value = [{ method: 'tunai', amount: 0, tendered: null, reference: '' }]
  } else if (page.error.value) {
    formMsg.value = page.error.value
  }
}

onMounted(async () => {
  await access.load(companyId.value, auth.token)
  cabang.value =
    (await page.run(() =>
      api.get<Cabang[]>(`/companies/${companyId.value}/branches`, auth.token),
    )) ?? []
  const favorit =
    auth.branchId && cabang.value.some((b) => b.id === auth.branchId)
      ? auth.branchId
      : (cabang.value[0]?.id ?? null)
  branchId.value = favorit
  const tersimpan = branchId.value
    ? localStorage.getItem(`aurevant-shift-${companyId.value}-${branchId.value}`)
    : null
  if (tersimpan) {
    shiftId.value = Number(tersimpan)
    shiftInfo.value = `Melanjutkan shift #${tersimpan}.`
  }
  muatKatalog()
})
</script>

<template>
  <div>
    <div class="mb-4 flex items-center gap-3">
      <div
        class="flex h-12 w-12 items-center justify-center rounded-xl bg-primary-container text-on-primary"
      >
        <AppIcon name="point_of_sale" :size="28" />
      </div>
      <h1 class="font-display text-xl font-semibold">Kasir</h1>
    </div>
    <AppBanner tone="green" :message="okMsg" />
    <AppBanner tone="red" :message="formMsg || page.error.value" />

    <AppCard title="Shift & cabang">
      <div class="flex flex-wrap items-end gap-2">
        <AppField label="Cabang">
          <select
            v-model="branchId"
            class="rounded-lg border border-outline bg-card px-3 py-2.5 text-sm focus:border-primary focus:outline-none"
            @change="muatKatalog"
          >
            <option v-for="b in cabang" :key="b.id" :value="b.id">
              {{ b.code }} — {{ b.name }}
            </option>
          </select>
        </AppField>
        <AppField v-if="bolehBuka && !shiftId" label="Kas awal">
          <input
            v-model.number="bukaKas"
            type="number"
            min="0"
            class="w-36 rounded-lg border border-outline bg-card px-3 py-2.5 text-sm focus:border-primary focus:outline-none"
          />
        </AppField>
        <AppButton v-if="bolehBuka && !shiftId" @click="bukaShift">Buka shift</AppButton>
        <AppField v-if="bolehTutup && shiftId" label="Hitung kas fisik">
          <input
            v-model.number="tutupKas"
            type="number"
            min="0"
            class="w-36 rounded-lg border border-outline bg-card px-3 py-2.5 text-sm focus:border-primary focus:outline-none"
          />
        </AppField>
        <AppButton v-if="bolehTutup && shiftId" variant="secondary" @click="tutupShift"
          >Tutup shift #{{ shiftId }}</AppButton
        >
      </div>
      <p v-if="shiftInfo" class="mt-2 text-sm text-secondary">{{ shiftInfo }}</p>
    </AppCard>

    <div v-if="bolehKasir" class="mt-4 grid gap-4 xl:grid-cols-5">
      <div class="xl:col-span-3">
        <AppField label="Cari barang / jasa">
          <input
            v-model="cari"
            class="w-full rounded-lg border border-outline bg-card px-3 py-2.5 text-sm focus:border-primary focus:outline-none"
            placeholder="Nama atau kode…"
          />
        </AppField>
        <div class="mt-2 grid gap-2 sm:grid-cols-2">
          <button
            v-for="it in tampilKatalog"
            :key="it.id"
            @click="tambah(it)"
            class="cursor-pointer rounded-xl bg-card p-3 text-left shadow-sm transition hover:shadow"
          >
            <p class="font-semibold">{{ it.name }}</p>
            <p class="font-mono text-xs text-secondary">{{ it.code }} · {{ it.type }}</p>
            <p class="font-display mt-1 font-bold text-primary">{{ rupiah(it.price) }}</p>
          </button>
        </div>
        <AppEmpty v-if="!tampilKatalog.length" message="Tidak ada item berharga di cabang ini." />
      </div>

      <div class="xl:col-span-2">
        <AppCard title="Keranjang">
          <AppEmpty v-if="!keranjang.length" message="Ketuk item untuk menambah." />
          <div
            v-for="b in keranjang"
            :key="b.item.id"
            class="border-b border-surface-low py-2 last:border-0"
          >
            <div class="flex items-center justify-between gap-2">
              <p class="font-semibold">{{ b.item.name }}</p>
              <button
                class="text-xs text-danger"
                @click="keranjang = keranjang.filter((x) => x !== b)"
              >
                buang
              </button>
            </div>
            <div class="mt-1 grid grid-cols-4 gap-1 text-sm">
              <label
                >Qty<input
                  v-model.number="b.qty"
                  type="number"
                  min="0.001"
                  step="any"
                  class="mt-0.5 w-full rounded border border-outline px-2 py-1"
              /></label>
              <label
                >Disc %<input
                  v-model.number="b.discPct"
                  type="number"
                  min="0"
                  max="100"
                  class="mt-0.5 w-full rounded border border-outline px-2 py-1"
              /></label>
              <label
                >Disc Rp<input
                  v-model.number="b.discAmt"
                  type="number"
                  min="0"
                  class="mt-0.5 w-full rounded border border-outline px-2 py-1"
              /></label>
              <label
                >PPN %<input
                  v-model.number="b.taxPct"
                  type="number"
                  min="0"
                  max="100"
                  class="mt-0.5 w-full rounded border border-outline px-2 py-1"
              /></label>
            </div>
            <p class="mt-1 text-right font-mono text-sm">{{ rupiah(barisTotal(b).total) }}</p>
          </div>
          <div class="mt-2 space-y-1 text-sm">
            <div class="flex justify-between">
              <span class="text-secondary">Subtotal</span><span>{{ rupiah(subtotal) }}</span>
            </div>
            <div class="flex justify-between">
              <span class="text-secondary">Diskon</span><span>{{ rupiah(diskon) }}</span>
            </div>
            <p class="font-display text-lg font-bold">Total {{ rupiah(total) }}</p>
          </div>
        </AppCard>

        <AppCard title="Bayar" class="mt-4">
          <div
            v-for="(p, i) in bayar"
            :key="i"
            class="mb-2 grid grid-cols-2 gap-1 rounded-lg bg-surface-low p-2"
          >
            <select
              v-model="p.method"
              class="rounded border border-outline bg-card px-2 py-1.5 text-sm"
            >
              <option v-for="m in METHODS" :key="m" :value="m">{{ m }}</option>
            </select>
            <input
              v-model.number="p.amount"
              type="number"
              min="0"
              placeholder="Nominal"
              class="rounded border border-outline bg-card px-2 py-1.5 text-sm"
            />
            <input
              v-model.number="p.tendered"
              type="number"
              min="0"
              placeholder="Tunai diterima (kembalian)"
              class="rounded border border-outline bg-card px-2 py-1.5 text-sm"
            />
            <div class="flex gap-1">
              <input
                v-model="p.reference"
                placeholder="Referensi"
                maxlength="100"
                class="w-full rounded border border-outline bg-card px-2 py-1.5 text-sm"
              />
              <button
                v-if="bayar.length > 1"
                class="text-xs text-danger"
                @click="bayar.splice(i, 1)"
              >
                ✕
              </button>
            </div>
          </div>
          <div class="flex gap-2">
            <AppButton
              variant="ghost"
              @click="
                bayar.push({
                  method: 'transfer',
                  amount: Math.max(0, total - dibayar),
                  tendered: null,
                  reference: '',
                })
              "
              >+ Split</AppButton
            >
            <AppButton :loading="page.loading.value" class="flex-1" @click="bayarSekarang"
              >Bayar {{ rupiah(total) }}</AppButton
            >
          </div>
          <p v-if="kembalian" class="mt-2 text-sm">
            Kembalian: <strong>{{ rupiah(kembalian) }}</strong>
          </p>
          <p class="mt-1 text-xs text-secondary">
            Satu persen dan satu nominal tidak boleh diisi bersamaan per baris.
          </p>
        </AppCard>

        <AppCard v-if="struk" title="Struk" class="mt-4">
          <p class="font-display font-bold">
            {{ struk.number }} <AppBadge tone="green">{{ struk.status }}</AppBadge>
          </p>
          <dl class="mt-2 space-y-1 text-sm">
            <div class="flex justify-between">
              <dt class="text-secondary">Subtotal</dt>
              <dd>{{ rupiah(struk.subtotal) }}</dd>
            </div>
            <div class="flex justify-between">
              <dt class="text-secondary">Diskon</dt>
              <dd>{{ rupiah(struk.discountTotal) }}</dd>
            </div>
            <div class="flex justify-between">
              <dt class="text-secondary">Pajak</dt>
              <dd>{{ rupiah(struk.taxTotal) }}</dd>
            </div>
            <div class="flex justify-between font-bold">
              <dt>Total</dt>
              <dd>{{ rupiah(struk.grandTotal) }}</dd>
            </div>
          </dl>
          <div v-for="p in struk.payments" :key="p.method" class="flex justify-between text-sm">
            <span class="text-secondary"
              >{{ p.method }}{{ p.tendered ? ` (terima ${rupiah(p.tendered)})` : '' }}</span
            >
            <span>{{ rupiah(p.amount) }}</span>
          </div>
          <div class="no-print mt-3 flex gap-2">
            <AppButton variant="ghost" @click="cetak">Cetak struk</AppButton>
            <AppButton variant="ghost" @click="unduhStruk">Unduh PDF</AppButton>
          </div>
        </AppCard>
      </div>
    </div>
    <AppEmpty v-else message="Anda tidak punya akses kasir." />
  </div>
</template>
