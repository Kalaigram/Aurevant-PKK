<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { api, downloadFile } from '../lib/api'
import { rupiah } from '../lib/format'
import { useCompany } from '../composables/useCompany'
import { useAccessStore } from '../stores/access'
import { useAsync } from '../composables/useAsync'
import { useConfirm } from '../composables/useConfirm'
import AppCard from '../components/ui/AppCard.vue'
import AppBadge from '../components/ui/AppBadge.vue'
import AppBanner from '../components/ui/AppBanner.vue'
import AppButton from '../components/ui/AppButton.vue'
import AppField from '../components/ui/AppField.vue'
import AppEmpty from '../components/ui/AppEmpty.vue'
import AppInput from '../components/ui/AppInput.vue'
import AppMoneyInput from '../components/ui/AppMoneyInput.vue'
import AppPageHeader from '../components/ui/AppPageHeader.vue'
import AppSearch from '../components/ui/AppSearch.vue'
import AppSelect from '../components/ui/AppSelect.vue'

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
  prices?: { branchId: number; price: string }[]
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
  amount: number | null
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
const { confirm } = useConfirm()
const page = useAsync()
const cabang = ref<Cabang[]>([])
const branchId = ref<number | null>(null)
const shiftId = ref<number | null>(null)
const shiftInfo = ref('')
const bukaKas = ref<number | null>(0)
const tutupKas = ref<number | null>(0)
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
const cabangAktif = computed(() => cabang.value.find((b) => b.id === branchId.value) ?? null)
const opsiCabang = computed(() =>
  cabang.value.map((b) => ({ value: b.id, label: `${b.code} - ${b.name}` })),
)
const METODE_LABEL: Record<string, string> = {
  tunai: 'Tunai',
  transfer: 'Transfer',
  qris: 'QRIS',
  ewallet: 'E-wallet',
  dp: 'Uang muka',
}
const TIPE_LABEL: Record<string, string> = {
  barang: 'Barang',
  jasa: 'Jasa',
}
const STATUS_STRUK: Record<string, string> = {
  lunas: 'Lunas',
  void: 'Void',
  retur: 'Retur',
}
const labelMetode = (method: string) => METODE_LABEL[method] ?? method
const labelTipe = (type: string) => TIPE_LABEL[type] ?? type
const labelStatusStruk = (status: string) => STATUS_STRUK[status] ?? status
const namaItem = (id: number) => katalog.value.find((item) => item.id === id)?.name ?? `Barang #${id}`

function kunciShift() {
  return `aurevant-shift-${companyId.value}-${branchId.value}`
}

function muatShiftTersimpan() {
  if (!branchId.value) {
    shiftId.value = null
    shiftInfo.value = ''
    return
  }
  const tersimpan = localStorage.getItem(kunciShift())
  shiftId.value = tersimpan ? Number(tersimpan) : null
  shiftInfo.value = tersimpan ? `Melanjutkan shift #${tersimpan}.` : ''
}

function gantiCabang(value: string | number | null) {
  branchId.value = value === null ? null : Number(value)
  keranjang.value = []
  bayar.value = [{ method: 'tunai', amount: 0, tendered: null, reference: '' }]
  struk.value = null
  formMsg.value = ''
  muatShiftTersimpan()
  void muatKatalog()
}

function tambah(item: Harga) {
  const ada = keranjang.value.find((b) => b.item.id === item.id)
  if (ada) ada.qty++
  else keranjang.value.push({ item, qty: 1, discPct: 0, discAmt: 0, taxPct: 0 })
  struk.value = null
}

function setAngka(
  baris: Baris,
  key: 'qty' | 'discPct' | 'discAmt' | 'taxPct',
  value: string | number | null,
) {
  baris[key] = Number(value) || 0
}

function tambahPembayaran() {
  bayar.value.push({
    method: 'transfer',
    amount: Math.max(0, total.value - dibayar.value),
    tendered: null,
    reference: '',
  })
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
    const harga = it.prices?.find((p) => p.branchId === branchId.value)?.price
    if (harga) out.push({ ...it, price: harga })
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
      { opening_cash: bukaKas.value ?? 0 },
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
        closing_cash_counted: tutupKas.value ?? 0,
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

function waktuSekarang() {
  return new Date().toLocaleString('id-ID', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
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

async function kosongkanKeranjang() {
  if (!keranjang.value.length) return
  const setuju = await confirm({
    title: 'Kosongkan keranjang?',
    message: 'Semua barang dan rincian pembayaran akan dihapus.',
    confirmLabel: 'Kosongkan',
    tone: 'danger',
  })
  if (!setuju) return
  keranjang.value = []
  bayar.value = [{ method: 'tunai', amount: 0, tendered: null, reference: '' }]
  struk.value = null
  formMsg.value = ''
}

async function bayarSekarang() {
  formMsg.value = ''
  okMsg.value = ''
  struk.value = null
  if (!branchId.value || !keranjang.value.length) return
  if (!shiftId.value) {
    formMsg.value = 'Buka shift kasir dulu sebelum menerima pembayaran.'
    return
  }
  if (Math.abs(dibayar.value - total.value) > 0.009) {
    formMsg.value = `Total bayar (${rupiah(dibayar.value)}) harus pas dengan total belanja (${rupiah(total.value)}).`
    return
  }
  const setuju = await confirm({
    title: 'Proses pembayaran?',
    message: `Terima pembayaran ${rupiah(dibayar.value)} untuk ${keranjang.value.length} jenis barang.`,
    confirmLabel: 'Bayar sekarang',
  })
  if (!setuju) return
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
          amount: Number(p.amount) || 0,
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
  muatShiftTersimpan()
  muatKatalog()
})
</script>

<template>
  <div>
    <div class="screen-only">
      <AppPageHeader
        eyebrow="Transaksi penjualan"
        title="Kasir"
        subtitle="Buka shift, pilih barang, terima pembayaran, lalu cetak struk."
        icon="point_of_sale"
      />
    <AppBanner tone="green" :message="okMsg" />
    <AppBanner tone="red" :message="formMsg || page.error.value" />

    <AppCard title="Shift & cabang" subtitle="Pilih cabang dan kelola shift kasir.">
      <div class="grid gap-3 sm:grid-cols-2 xl:grid-cols-4 xl:items-end">
        <AppField label="Cabang" class="sm:col-span-2 xl:col-span-1">
          <AppSelect
            :model-value="branchId"
            :options="opsiCabang"
            placeholder="Pilih cabang"
            @update:model-value="gantiCabang"
          />
        </AppField>
        <AppField v-if="bolehBuka && !shiftId" label="Kas awal">
          <AppMoneyInput v-model="bukaKas" />
        </AppField>
        <AppButton v-if="bolehBuka && !shiftId" class="w-full" @click="bukaShift">
          Buka shift
        </AppButton>
        <AppField v-if="bolehTutup && shiftId" label="Hitung kas fisik">
          <AppMoneyInput v-model="tutupKas" />
        </AppField>
        <AppButton
          v-if="bolehTutup && shiftId"
          variant="secondary"
          class="w-full"
          @click="tutupShift"
        >
          Tutup shift #{{ shiftId }}
        </AppButton>
      </div>
      <p v-if="shiftInfo" class="mt-3 text-sm text-secondary">{{ shiftInfo }}</p>
      <AppBanner
        v-if="!shiftId && bolehKasir"
        tone="amber"
        class="mt-3"
        message="Shift belum terbuka. Buka shift dulu agar pembayaran bisa diproses."
      />
    </AppCard>

    <div v-if="bolehKasir" class="mt-4 grid gap-4 xl:grid-cols-5">
      <div class="xl:col-span-3">
        <AppField label="Cari barang / jasa">
          <AppSearch
            v-model="cari"
            label="Cari barang atau jasa"
            placeholder="Nama atau kode item..."
          />
        </AppField>
        <div class="mt-3 grid gap-2 sm:grid-cols-2">
          <button
            v-for="it in tampilKatalog"
            :key="it.id"
            type="button"
            class="min-h-20 rounded-xl border border-outline/70 bg-card p-3 text-left shadow-card transition hover:border-outline-strong hover:bg-surface-low/40 hover:shadow-lifted focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/55 focus-visible:ring-offset-2"
            :aria-label="`Tambah ${it.name}, ${rupiah(it.price)}`"
            @click="tambah(it)"
          >
            <p class="font-semibold text-ink">{{ it.name }}</p>
            <p class="font-mono text-xs text-secondary">
              {{ it.code }} - {{ labelTipe(it.type) }}
              <span v-if="it.unit">- {{ it.unit }}</span>
            </p>
            <p class="font-display mt-1 font-bold text-primary">{{ rupiah(it.price) }}</p>
          </button>
        </div>
        <AppEmpty
          v-if="!tampilKatalog.length"
          class="mt-3"
          icon="search_off"
          title="Item tidak ditemukan"
          :message="cari ? 'Ubah kata kunci pencarian.' : 'Tidak ada item berharga di cabang ini.'"
        />
      </div>

      <div class="xl:col-span-2">
        <AppCard title="Keranjang">
          <template #actions>
            <AppButton v-if="keranjang.length" variant="ghost" @click="kosongkanKeranjang">
              Kosongkan
            </AppButton>
          </template>
          <AppEmpty v-if="!keranjang.length" message="Ketuk item untuk menambah." />
          <div
            v-for="b in keranjang"
            :key="b.item.id"
            class="border-b border-surface-low py-3 last:border-0"
          >
            <div class="flex items-start justify-between gap-2">
              <div class="min-w-0">
                <p class="truncate font-semibold text-ink">{{ b.item.name }}</p>
                <p class="font-mono text-xs text-secondary">
                  {{ b.item.code }} - {{ rupiah(b.item.price) }}
                </p>
              </div>
              <AppButton
                variant="ghost"
                size="sm"
                class="text-danger"
                :aria-label="`Hapus ${b.item.name} dari keranjang`"
                @click="keranjang = keranjang.filter((x) => x !== b)"
              >
                Hapus
              </AppButton>
            </div>
            <div class="mt-2 grid grid-cols-2 gap-2 sm:grid-cols-4">
              <AppField label="Qty">
                <AppInput
                  :model-value="b.qty"
                  type="number"
                  min="0.001"
                  step="any"
                  @update:model-value="setAngka(b, 'qty', $event)"
                />
              </AppField>
              <AppField label="Disc %">
                <AppInput
                  :model-value="b.discPct"
                  type="number"
                  min="0"
                  max="100"
                  @update:model-value="setAngka(b, 'discPct', $event)"
                />
              </AppField>
              <AppField label="Disc Rp">
                <AppInput
                  :model-value="b.discAmt"
                  type="number"
                  min="0"
                  @update:model-value="setAngka(b, 'discAmt', $event)"
                />
              </AppField>
              <AppField label="PPN %">
                <AppInput
                  :model-value="b.taxPct"
                  type="number"
                  min="0"
                  max="100"
                  @update:model-value="setAngka(b, 'taxPct', $event)"
                />
              </AppField>
            </div>
            <p class="mt-2 text-right font-mono text-sm font-semibold text-ink">
              {{ rupiah(barisTotal(b).total) }}
            </p>
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

        <AppCard title="Bayar" subtitle="Pilih metode dan isi nominal pembayaran." class="mt-4">
          <div
            v-for="(p, i) in bayar"
            :key="i"
            class="mb-3 rounded-xl border border-outline/70 bg-surface-low/60 p-3 last:mb-0"
          >
            <div class="mb-2 flex items-center justify-between gap-2">
              <p class="text-xs font-semibold uppercase tracking-wider text-secondary">
                Pembayaran {{ i + 1 }}
              </p>
              <AppButton
                v-if="bayar.length > 1"
                variant="ghost"
                size="sm"
                class="text-danger"
                :aria-label="`Hapus pembayaran ${i + 1}`"
                @click="bayar.splice(i, 1)"
              >
                Hapus
              </AppButton>
            </div>
            <div class="grid gap-2 sm:grid-cols-2">
              <AppField label="Metode" required>
                <AppSelect v-model="p.method">
                  <option v-for="m in METHODS" :key="m" :value="m">{{ labelMetode(m) }}</option>
                </AppSelect>
              </AppField>
              <AppField label="Nominal" required>
                <AppMoneyInput v-model="p.amount" />
              </AppField>
              <AppField
                v-if="p.method === 'tunai'"
                label="Uang diterima"
                hint="Dipakai untuk menghitung kembalian."
              >
                <AppMoneyInput v-model="p.tendered" />
              </AppField>
              <AppField v-else label="Referensi" hint="Nomor transaksi atau catatan pembayaran.">
                <AppInput v-model="p.reference" maxlength="100" placeholder="Opsional" />
              </AppField>
            </div>
          </div>
          <div class="mt-3 flex flex-col gap-2 sm:flex-row">
            <AppButton variant="ghost" @click="tambahPembayaran">Tambah pembayaran</AppButton>
            <AppButton :loading="page.loading.value" class="flex-1" @click="bayarSekarang">
              Bayar {{ rupiah(total) }}
            </AppButton>
          </div>
          <p v-if="kembalian" class="mt-2 text-sm text-secondary">
            Kembalian: <strong class="text-ink">{{ rupiah(kembalian) }}</strong>
          </p>
          <p class="mt-1 text-xs text-secondary">
            Isi diskon persen atau nominal, bukan keduanya, untuk setiap baris.
          </p>
        </AppCard>

        <AppCard v-if="struk" title="Struk" class="no-print mt-4">
          <p class="font-display font-bold">
            {{ struk.number }}
            <AppBadge :tone="struk.status === 'lunas' ? 'green' : 'amber'">
              {{ labelStatusStruk(struk.status) }}
            </AppBadge>
          </p>
          <div class="mt-2 space-y-1 border-y border-outline/60 py-2 text-sm">
            <div v-for="(item, i) in struk.items" :key="`${item.itemId}-${i}`" class="flex justify-between gap-2">
              <span class="min-w-0 truncate">{{ namaItem(item.itemId) }} × {{ Number(item.qty) }}</span>
              <span class="shrink-0 tabular-nums">{{ rupiah(item.lineTotal) }}</span>
            </div>
          </div>
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
          <div v-for="(p, i) in struk.payments" :key="`${p.method}-${i}`" class="flex justify-between text-sm">
            <span class="text-secondary"
              >{{ labelMetode(p.method) }}{{ p.tendered ? ` (terima ${rupiah(p.tendered)})` : '' }}</span
            >
            <span>{{ rupiah(p.amount) }}</span>
          </div>
          <div class="mt-3 flex gap-2">
            <AppButton variant="ghost" @click="cetak">Cetak struk</AppButton>
            <AppButton variant="ghost" @click="unduhStruk">Unduh PDF</AppButton>
          </div>
        </AppCard>
      </div>
    </div>
    <AppEmpty v-else message="Anda tidak punya akses kasir." />
    </div>

    <div v-if="struk" class="receipt print-only">
      <div class="text-center">
        <p class="text-base font-bold">{{ auth.companyName ?? 'Aurevant' }}</p>
        <p v-if="cabangAktif">{{ cabangAktif.name }}</p>
        <p class="mt-1">{{ waktuSekarang() }}</p>
      </div>
      <div class="mt-2 border-y border-dashed border-black py-2">
        <p class="font-semibold">Nota {{ struk.number }}</p>
        <div v-for="(item, i) in struk.items" :key="`print-${item.itemId}-${i}`" class="mt-1">
          <p>{{ namaItem(item.itemId) }}</p>
          <div class="flex justify-between">
            <span>{{ Number(item.qty) }} × {{ rupiah(item.price) }}</span>
            <span>{{ rupiah(item.lineTotal) }}</span>
          </div>
        </div>
      </div>
      <div class="mt-2 space-y-0.5">
        <div class="flex justify-between"><span>Subtotal</span><span>{{ rupiah(struk.subtotal) }}</span></div>
        <div class="flex justify-between"><span>Diskon</span><span>{{ rupiah(struk.discountTotal) }}</span></div>
        <div class="flex justify-between"><span>Pajak</span><span>{{ rupiah(struk.taxTotal) }}</span></div>
        <div class="flex justify-between border-t border-dashed border-black pt-1 font-bold">
          <span>Total</span><span>{{ rupiah(struk.grandTotal) }}</span>
        </div>
        <div v-for="(p, i) in struk.payments" :key="`print-pay-${i}`" class="flex justify-between">
          <span>{{ labelMetode(p.method) }}{{ p.tendered ? ` (terima ${rupiah(p.tendered)})` : '' }}</span>
          <span>{{ rupiah(p.amount) }}</span>
        </div>
      </div>
      <p class="mt-3 text-center">Terima kasih telah berbelanja.</p>
    </div>
  </div>
</template>
