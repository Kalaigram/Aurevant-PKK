<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { api, type PageMeta } from '../lib/api'
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
import AppPager from '../components/ui/AppPager.vue'
import AppIcon from '../components/ui/AppIcon.vue'

interface POItem {
  itemId: number
  qtyOrdered: string
  qtyReceived: string
  unitPrice: string
}
interface PO {
  id: number
  branchId: number
  supplierId: number
  status: string
  note: string | null
  items: POItem[]
  bill: { amount: string; paidAmount: string; dueAt: string | null } | null
}
interface Supplier {
  id: number
  name: string
}
interface Cabang {
  id: number
  code: string
  name: string
}

const { auth, companyId } = useCompany()
const access = useAccessStore()
const { loading, error, run } = useAsync()
const cabang = ref<Cabang[]>([])
const suppliers = ref<Supplier[]>([])
const list = ref<PO[]>([])
const meta = ref<PageMeta | null>(null)
const filterCabang = ref('')
const detail = ref<PO | null>(null)
const terima = ref<
  { id: number; dueAt?: string | null; items?: { itemId: number; qty: string }[] }[]
>([])
const okMsg = ref('')
const showBuat = ref(false)
const fBranch = ref('')
const fSupplier = ref('')
const fNote = ref('')
const fLines = ref<{ item_id: number | null; qty_ordered: number; unit_price: number }[]>([
  { item_id: null, qty_ordered: 1, unit_price: 0 },
])
const showTerima = ref(false)
const tLines = ref<{ item_id: number; qty_received: number; unit_cost: number }[]>([])
const tDue = ref('')
const tNote = ref('')

const bolehBuat = () => access.can('po.buat')
const bolehSetujui = () => access.can('po.setujui')
const tone = (s: string) =>
  s === 'selesai' ? 'green' : s === 'dibatalkan' ? 'red' : s === 'dipesan' ? 'blue' : 'amber'
const sisa = (it: POItem) => Math.max(0, Number(it.qtyOrdered) - Number(it.qtyReceived))

async function muat(page = 1) {
  okMsg.value = ''
  const res = await run(() =>
    api.getPage<PO>(
      `/companies/${companyId.value}/purchase-orders?per_page=15&page=${page}${filterCabang.value ? `&branch_id=${filterCabang.value}` : ''}`,
      auth.token,
    ),
  )
  if (res) {
    const raw = res as unknown as { data: PO[]; meta?: PageMeta } | PO[]
    if (Array.isArray(raw)) list.value = raw
    else {
      list.value = raw.data
      meta.value = raw.meta ?? null
    }
  }
}

async function lihat(id: number) {
  detail.value = await run(() =>
    api.get<PO>(`/companies/${companyId.value}/purchase-orders/${id}`, auth.token),
  )
  terima.value = []
  const riw = await api
    .get<{ id: number; dueAt?: string | null; items?: { itemId: number; qty: string }[] }[]>(
      `/companies/${companyId.value}/purchase-orders/${id}/receipts`,
      auth.token,
    )
    .catch(() => null)
  if (riw) {
    const raw = riw as unknown as
      | { id: number; dueAt?: string | null; items?: { itemId: number; qty: string }[] }[]
      | { data: { id: number; dueAt?: string | null; items?: { itemId: number; qty: string }[] }[] }
    terima.value = Array.isArray(raw) ? raw : (raw.data ?? [])
  }
}

async function buat() {
  const res = await run(() =>
    api.post(
      `/companies/${companyId.value}/purchase-orders`,
      {
        branch_id: Number(fBranch.value),
        supplier_id: Number(fSupplier.value),
        items: fLines.value
          .filter((l) => l.item_id)
          .map((l) => ({
            item_id: l.item_id,
            qty_ordered: l.qty_ordered,
            unit_price: l.unit_price,
          })),
        note: fNote.value || undefined,
      },
      auth.token,
    ),
  )
  if (res) {
    showBuat.value = false
    okMsg.value = 'PO dibuat sebagai draft.'
    muat(1)
  }
}

async function jalan(aksi: string, body: unknown, pesan: string) {
  if (!detail.value) return
  const res = await run(() =>
    api.post(
      `/companies/${companyId.value}/purchase-orders/${detail.value!.id}/${aksi}`,
      body,
      auth.token,
    ),
  )
  if (res) {
    okMsg.value = pesan
    showTerima.value = false
    lihat(detail.value.id)
    muat(meta.value?.current_page ?? 1)
  }
}

function bukaTerima() {
  if (!detail.value) return
  tLines.value = detail.value.items
    .filter((it) => sisa(it) > 0)
    .map((it) => ({ item_id: it.itemId, qty_received: sisa(it), unit_cost: Number(it.unitPrice) }))
  tDue.value = ''
  tNote.value = ''
  showTerima.value = true
}

onMounted(async () => {
  await access.load(companyId.value, auth.token)
  cabang.value =
    (await run(() => api.get<Cabang[]>(`/companies/${companyId.value}/branches`, auth.token))) ?? []
  suppliers.value =
    (await run(() =>
      api.get<Supplier[]>(`/companies/${companyId.value}/suppliers?per_page=100`, auth.token),
    )) ?? []
  muat()
})
</script>

<template>
  <div>
    <div class="mb-4 flex flex-wrap items-center justify-between gap-2">
      <div class="flex items-center gap-3">
        <div
          class="flex h-12 w-12 items-center justify-center rounded-xl bg-primary-container text-on-primary"
        >
          <AppIcon name="shopping_cart" :size="28" />
        </div>
        <h1 class="font-display text-xl font-semibold">Pembelian</h1>
      </div>
      <AppButton v-if="bolehBuat()" @click="showBuat = true"
        ><AppIcon name="add" :size="18" />PO baru</AppButton
      >
    </div>
    <AppBanner tone="green" :message="okMsg" />
    <AppBanner tone="red" :message="error" />

    <div class="mb-3">
      <select
        v-model="filterCabang"
        class="rounded-lg border border-outline bg-card px-3 py-2 text-sm"
        @change="muat(1)"
      >
        <option value="">Semua cabang</option>
        <option v-for="b in cabang" :key="b.id" :value="b.id">{{ b.code }} — {{ b.name }}</option>
      </select>
    </div>

    <div class="grid gap-4 lg:grid-cols-5">
      <div class="lg:col-span-3">
        <AppEmpty v-if="!loading && !list.length" message="Belum ada PO." />
        <AppCard
          v-for="o in list"
          :key="o.id"
          class="cursor-pointer transition hover:shadow"
          @click="lihat(o.id)"
        >
          <div class="flex items-center justify-between gap-2">
            <p class="font-semibold">
              PO #{{ o.id }}
              <span class="font-normal text-secondary">{{ o.items.length }} jenis</span>
            </p>
            <AppBadge :tone="tone(o.status)">{{ o.status }}</AppBadge>
          </div>
          <p class="mt-1 text-sm text-secondary">{{ o.note ?? '' }}</p>
        </AppCard>
        <AppPager :meta="meta" @page="muat" />
      </div>
      <div class="lg:col-span-2">
        <AppEmpty v-if="!detail" message="Pilih PO untuk detail & tindakan." />
        <AppCard v-if="detail" title="Detail PO">
          <p class="font-semibold">
            #{{ detail.id }} <AppBadge :tone="tone(detail.status)">{{ detail.status }}</AppBadge>
          </p>
          <table class="mt-2 w-full text-sm">
            <tbody>
              <tr
                v-for="(it, i) in detail.items"
                :key="i"
                class="border-b border-surface-low last:border-0"
              >
                <td class="py-1.5">
                  Item #{{ it.itemId }}<br /><span class="text-xs text-secondary"
                    >pesan {{ it.qtyOrdered }} · terima {{ it.qtyReceived }}</span
                  >
                </td>
                <td class="py-1.5 text-right">{{ rupiah(it.unitPrice) }}</td>
              </tr>
            </tbody>
          </table>
          <p v-if="detail.bill" class="mt-2 text-sm">
            Tagihan: {{ rupiah(detail.bill.amount) }} · dibayar {{ rupiah(detail.bill.paidAmount)
            }}{{ detail.bill.dueAt ? ` · jatuh tempo ${detail.bill.dueAt}` : '' }}
          </p>
          <div class="mt-3 flex flex-wrap gap-2">
            <AppButton
              v-if="bolehBuat() && detail.status === 'draft'"
              :loading="loading"
              @click="jalan('submit', {}, 'PO diajukan.')"
              >Ajukan</AppButton
            >
            <template v-if="bolehSetujui()">
              <AppButton
                v-if="detail.status === 'diajukan'"
                :loading="loading"
                @click="jalan('approve', {}, 'PO disetujui.')"
                >Setujui</AppButton
              >
              <AppButton
                v-if="detail.status === 'disetujui'"
                :loading="loading"
                @click="jalan('order', {}, 'PO dipesan ke supplier.')"
                >Pesan</AppButton
              >
              <AppButton
                v-if="['disetujui', 'dipesan'].includes(detail.status)"
                variant="secondary"
                @click="bukaTerima"
                >Terima barang</AppButton
              >
              <AppButton
                v-if="['draft', 'diajukan', 'disetujui', 'dipesan'].includes(detail.status)"
                variant="danger"
                @click="jalan('cancel', {}, 'PO dibatalkan.')"
                >Batalkan</AppButton
              >
            </template>
          </div>
          <div class="mt-4 border-t border-outline pt-3">
            <h3 class="font-display text-sm font-semibold">Riwayat penerimaan</h3>
            <p v-if="!terima.length" class="mt-1 text-xs text-secondary">
              Belum ada barang diterima untuk PO ini.
            </p>
            <ul v-else class="mt-2 space-y-2">
              <li
                v-for="r in terima"
                :key="r.id"
                class="rounded-lg bg-surface-low px-3 py-2 text-sm"
              >
                <p class="font-semibold">
                  Terima #{{ r.id }}{{ r.dueAt ? ` · jatuh tempo ${r.dueAt}` : '' }}
                </p>
                <p v-for="it in r.items ?? []" :key="it.itemId" class="text-xs text-secondary">
                  Item #{{ it.itemId }} — {{ it.qty }} pcs
                </p>
              </li>
            </ul>
          </div>
        </AppCard>
      </div>
    </div>

    <div
      v-if="showBuat"
      class="fixed inset-0 z-20 flex items-center justify-center bg-black/40 p-4"
      @click.self="showBuat = false"
    >
      <div class="max-h-[90vh] w-full max-w-md overflow-y-auto rounded-xl bg-card p-5 shadow-xl">
        <h3 class="font-display font-semibold">PO baru</h3>
        <div class="mt-3 grid grid-cols-2 gap-2">
          <AppField label="Cabang">
            <select
              v-model="fBranch"
              class="w-full rounded-lg bg-surface-low px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-primary"
            >
              <option value="">Pilih…</option>
              <option v-for="b in cabang" :key="b.id" :value="b.id">{{ b.code }}</option>
            </select>
          </AppField>
          <AppField label="Supplier">
            <select
              v-model="fSupplier"
              class="w-full rounded-lg bg-surface-low px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-primary"
            >
              <option value="">Pilih…</option>
              <option v-for="s in suppliers" :key="s.id" :value="s.id">{{ s.name }}</option>
            </select>
          </AppField>
        </div>
        <div v-for="(l, i) in fLines" :key="i" class="mt-2 grid grid-cols-3 gap-1">
          <input
            v-model.number="l.item_id"
            type="number"
            placeholder="ID item"
            class="rounded-lg bg-surface-low px-2 py-2 text-sm outline-none focus:ring-2 focus:ring-primary"
          />
          <input
            v-model.number="l.qty_ordered"
            type="number"
            min="0.001"
            step="any"
            placeholder="Qty"
            class="rounded-lg bg-surface-low px-2 py-2 text-sm outline-none"
          />
          <div class="flex gap-1">
            <input
              v-model.number="l.unit_price"
              type="number"
              min="0"
              placeholder="Harga"
              class="w-full rounded-lg bg-surface-low px-2 py-2 text-sm outline-none"
            />
            <button
              v-if="fLines.length > 1"
              class="text-xs text-danger"
              @click="fLines.splice(i, 1)"
            >
              ✕
            </button>
          </div>
        </div>
        <AppButton
          variant="ghost"
          class="mt-1"
          @click="fLines.push({ item_id: null, qty_ordered: 1, unit_price: 0 })"
          >+ Baris</AppButton
        >
        <div class="mt-2">
          <AppField label="Catatan">
            <input
              v-model="fNote"
              maxlength="500"
              class="w-full rounded-lg bg-surface-low px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-primary"
            />
          </AppField>
        </div>
        <div class="mt-4 flex justify-end gap-2">
          <AppButton variant="ghost" @click="showBuat = false">Batal</AppButton>
          <AppButton :loading="loading" @click="buat">Buat draft</AppButton>
        </div>
      </div>
    </div>

    <div
      v-if="showTerima"
      class="fixed inset-0 z-20 flex items-center justify-center bg-black/40 p-4"
      @click.self="showTerima = false"
    >
      <div class="max-h-[90vh] w-full max-w-md overflow-y-auto rounded-xl bg-card p-5 shadow-xl">
        <h3 class="font-display font-semibold">Terima barang</h3>
        <p class="text-xs text-secondary">
          Stok, HPP, dan tagihan terbentuk otomatis. Jatuh tempo memengaruhi aging hutang.
        </p>
        <div
          v-for="(l, i) in tLines"
          :key="i"
          class="mt-2 grid grid-cols-2 gap-1 rounded-lg bg-surface-low p-2 text-sm"
        >
          <span>Item #{{ l.item_id }}</span>
          <span class="flex gap-1">
            <input
              v-model.number="l.qty_received"
              type="number"
              min="0"
              step="any"
              placeholder="Qty terima"
              class="w-full rounded border border-outline bg-card px-2 py-1"
            />
            <input
              v-model.number="l.unit_cost"
              type="number"
              min="0"
              placeholder="Modal"
              class="w-full rounded border border-outline bg-card px-2 py-1"
            />
          </span>
        </div>
        <div class="mt-2 grid grid-cols-2 gap-2">
          <AppField label="Jatuh tempo (opsional)">
            <input
              v-model="tDue"
              type="date"
              class="w-full rounded-lg bg-surface-low px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-primary"
            />
          </AppField>
          <AppField label="Catatan">
            <input
              v-model="tNote"
              maxlength="500"
              class="w-full rounded-lg bg-surface-low px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-primary"
            />
          </AppField>
        </div>
        <div class="mt-4 flex justify-end gap-2">
          <AppButton variant="ghost" @click="showTerima = false">Batal</AppButton>
          <AppButton
            :loading="loading"
            @click="
              jalan(
                'receive',
                { items: tLines, due_at: tDue || undefined, note: tNote || undefined },
                'Penerimaan dicatat.',
              )
            "
            >Simpan terima</AppButton
          >
        </div>
      </div>
    </div>
  </div>
</template>
