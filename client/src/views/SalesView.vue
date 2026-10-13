<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
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

interface NotaItem {
  saleItemId?: number
  id?: number
  itemId: number
  itemName?: string
  qty: string
  price: string
  lineTotal: string
}
interface Nota {
  id: number
  number: string
  status: string
  subtotal: string
  discountTotal: string
  taxTotal: string
  grandTotal: string
  items: NotaItem[]
  payments: { method: string; amount: string }[]
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
const branchId = ref<number | null>(null)
const list = ref<Nota[]>([])
const meta = ref<PageMeta | null>(null)
const filterStatus = ref('')
const cariNomor = ref('')
const detail = ref<Nota | null>(null)
const okMsg = ref('')

const dialogVoid = ref(false)
const alasanVoid = ref('')
const dialogRetur = ref(false)
const barisRetur = ref<{ sale_item_id: number; qty: number; condition: string }[]>([])
const alasanRetur = ref('')
const metodeRetur = ref('tunai')

const bolehVoid = computed(() => access.can('transaksi.void'))
const bolehRetur = computed(() => access.can('retur.buat'))

const tone = (s: string) => (s === 'lunas' ? 'green' : s === 'void' ? 'red' : 'amber')

async function muat(page = 1) {
  okMsg.value = ''
  if (!branchId.value) return
  const res = await run(() =>
    api.get<{ data: Nota[]; meta: PageMeta }>(
      `/companies/${companyId.value}/branches/${branchId.value}/sales?per_page=15&page=${page}`,
      auth.token,
    ),
  )
  if (res) {
    const raw = res as unknown as { data: Nota[]; meta?: PageMeta } | Nota[]
    if (Array.isArray(raw)) list.value = raw
    else {
      list.value = raw.data
      meta.value = raw.meta ?? null
    }
  }
}

const tersaring = computed(() =>
  list.value.filter(
    (n) =>
      (!filterStatus.value || n.status === filterStatus.value) &&
      (!cariNomor.value.trim() ||
        n.number.toLowerCase().includes(cariNomor.value.trim().toLowerCase())),
  ),
)

async function lihat(id: number) {
  detail.value = await run(() =>
    api.get<Nota>(
      `/companies/${companyId.value}/branches/${branchId.value}/sales/${id}`,
      auth.token,
    ),
  )
  barisRetur.value = []
}

async function voidSekarang() {
  if (!detail.value || !branchId.value) return
  const res = await run(() =>
    api.post(
      `/companies/${companyId.value}/branches/${branchId.value}/sales/${detail.value!.id}/void`,
      {
        reason: alasanVoid.value,
      },
      auth.token,
    ),
  )
  if (res) {
    okMsg.value = 'Nota dibatalkan, stok kembali.'
    dialogVoid.value = false
    alasanVoid.value = ''
    lihat(detail.value.id)
    muat(meta.value?.current_page ?? 1)
  }
}

function siapkanRetur() {
  if (!detail.value) return
  barisRetur.value = detail.value.items.map((it) => ({
    sale_item_id: it.saleItemId ?? it.id ?? 0,
    qty: 0,
    condition: 'layak',
  }))
  dialogRetur.value = true
}

async function returSekarang() {
  if (!detail.value || !branchId.value) return
  const lines = barisRetur.value.filter((l) => l.qty > 0 && l.sale_item_id > 0)
  const res = await run(() =>
    api.post(
      `/companies/${companyId.value}/branches/${branchId.value}/sales/${detail.value!.id}/returns`,
      {
        items: lines,
        reason: alasanRetur.value,
        refund_method: metodeRetur.value,
      },
      auth.token,
    ),
  )
  if (res) {
    okMsg.value = 'Retur tercatat.'
    dialogRetur.value = false
    alasanRetur.value = ''
    lihat(detail.value.id)
    muat(meta.value?.current_page ?? 1)
  }
}

async function ajukanVoid() {
  if (!detail.value) return
  const res = await run(() =>
    api.post(
      `/companies/${companyId.value}/approvals`,
      {
        action: 'sale.void',
        entity_id: detail.value!.id,
        payload: { reason: alasanVoid.value || 'Minta void dari halaman nota' },
      },
      auth.token,
    ),
  )
  if (res) {
    okMsg.value = 'Pengajuan void terkirim ke atasan.'
    dialogVoid.value = false
  }
}

onMounted(async () => {
  await access.load(companyId.value, auth.token)
  cabang.value =
    (await run(() => api.get<Cabang[]>(`/companies/${companyId.value}/branches`, auth.token))) ?? []
  branchId.value =
    (auth.branchId && cabang.value.some((b) => b.id === auth.branchId)
      ? auth.branchId
      : cabang.value[0]?.id) ?? null
  muat()
})
</script>

<template>
  <div>
    <div class="mb-4 flex items-center gap-3">
      <div
        class="flex h-12 w-12 items-center justify-center rounded-xl bg-primary-container text-on-primary"
      >
        <AppIcon name="receipt_long" :size="28" />
      </div>
      <h1 class="font-display text-xl font-semibold">Nota penjualan</h1>
    </div>
    <AppBanner tone="green" :message="okMsg" />
    <AppBanner tone="red" :message="error" />

    <div class="grid gap-4 lg:grid-cols-5">
      <div class="lg:col-span-3">
        <div class="mb-3 flex flex-wrap gap-2">
          <select
            v-model="branchId"
            class="rounded-lg border border-outline bg-card px-3 py-2 text-sm"
            @change="muat(1)"
          >
            <option v-for="b in cabang" :key="b.id" :value="b.id">
              {{ b.code }} — {{ b.name }}
            </option>
          </select>
          <select
            v-model="filterStatus"
            class="rounded-lg border border-outline bg-card px-3 py-2 text-sm"
          >
            <option value="">Semua status</option>
            <option value="lunas">Lunas</option>
            <option value="void">Void</option>
            <option value="retur">Retur</option>
          </select>
          <input
            v-model="cariNomor"
            class="rounded-lg border border-outline bg-card px-3 py-2 text-sm"
            placeholder="Cari nomor…"
          />
        </div>
        <AppEmpty v-if="!loading && !tersaring.length" message="Tidak ada nota." />
        <AppCard
          v-for="n in tersaring"
          :key="n.id"
          class="cursor-pointer transition hover:shadow"
          @click="lihat(n.id)"
        >
          <div class="flex items-center justify-between gap-2">
            <p class="font-mono font-semibold">{{ n.number }}</p>
            <AppBadge :tone="tone(n.status)">{{ n.status }}</AppBadge>
          </div>
          <p class="mt-1 text-sm text-secondary">
            {{ n.items.length }} baris ·
            <strong class="text-ink">{{ rupiah(n.grandTotal) }}</strong>
          </p>
        </AppCard>
        <AppPager :meta="meta" @page="muat" />
      </div>

      <div class="lg:col-span-2">
        <AppEmpty v-if="!detail" message="Pilih nota untuk melihat rincian." />
        <AppCard v-if="detail" title="Rincian nota">
          <p class="font-mono font-bold">
            {{ detail.number }} <AppBadge :tone="tone(detail.status)">{{ detail.status }}</AppBadge>
          </p>
          <table class="mt-2 w-full text-sm">
            <tbody>
              <tr
                v-for="(it, i) in detail.items"
                :key="i"
                class="border-b border-surface-low last:border-0"
              >
                <td class="py-1.5">{{ it.itemName ?? `Item #${it.itemId}` }} × {{ it.qty }}</td>
                <td class="py-1.5 text-right">{{ rupiah(it.lineTotal) }}</td>
              </tr>
            </tbody>
          </table>
          <dl class="mt-2 space-y-1 text-sm">
            <div class="flex justify-between">
              <dt class="text-secondary">Subtotal</dt>
              <dd>{{ rupiah(detail.subtotal) }}</dd>
            </div>
            <div class="flex justify-between">
              <dt class="text-secondary">Diskon</dt>
              <dd>{{ rupiah(detail.discountTotal) }}</dd>
            </div>
            <div class="flex justify-between">
              <dt class="text-secondary">Pajak</dt>
              <dd>{{ rupiah(detail.taxTotal) }}</dd>
            </div>
            <div class="flex justify-between font-bold">
              <dt>Total</dt>
              <dd>{{ rupiah(detail.grandTotal) }}</dd>
            </div>
          </dl>
          <div class="mt-2 space-y-1 text-sm">
            <div v-for="(p, i) in detail.payments" :key="i" class="flex justify-between">
              <span class="text-secondary">{{ p.method }}</span
              ><span>{{ rupiah(p.amount) }}</span>
            </div>
          </div>
          <div v-if="detail.status === 'lunas'" class="mt-3 flex gap-2">
            <AppButton v-if="bolehVoid" variant="danger" @click="dialogVoid = true">Void</AppButton>
            <AppButton v-if="bolehRetur" variant="secondary" @click="siapkanRetur">Retur</AppButton>
          </div>
        </AppCard>
      </div>
    </div>

    <div
      v-if="dialogVoid"
      class="fixed inset-0 z-20 flex items-center justify-center bg-black/40 p-4"
      @click.self="dialogVoid = false"
    >
      <div class="w-full max-w-sm rounded-xl bg-card p-5 shadow-xl">
        <h3 class="font-display font-semibold">Batalkan nota {{ detail?.number }}?</h3>
        <p class="mt-1 text-sm text-secondary">Stok barang kembali. Tindakan ini tercatat.</p>
        <div class="mt-3">
          <AppField label="Alasan (wajib)">
            <textarea
              v-model="alasanVoid"
              rows="2"
              maxlength="500"
              class="w-full rounded-lg border border-outline bg-card px-3 py-2 text-sm focus:border-primary focus:outline-none"
            />
          </AppField>
        </div>
        <div class="mt-4 flex justify-end gap-2">
          <AppButton variant="ghost" @click="dialogVoid = false">Batal</AppButton>
          <AppButton v-if="bolehVoid" variant="danger" :loading="loading" @click="voidSekarang"
            >Ya, void</AppButton
          >
          <AppButton v-else :loading="loading" @click="ajukanVoid">Ajukan ke atasan</AppButton>
        </div>
      </div>
    </div>

    <div
      v-if="dialogRetur"
      class="fixed inset-0 z-20 flex items-center justify-center bg-black/40 p-4"
      @click.self="dialogRetur = false"
    >
      <div class="max-h-[90vh] w-full max-w-md overflow-y-auto rounded-xl bg-card p-5 shadow-xl">
        <h3 class="font-display font-semibold">Retur barang</h3>
        <div
          v-for="(l, i) in barisRetur"
          :key="i"
          class="mt-2 grid grid-cols-3 gap-1 rounded-lg bg-surface-low p-2 text-sm"
        >
          <span class="col-span-3 font-medium">{{
            detail?.items[i]?.itemName ?? `Item #${l.sale_item_id}`
          }}</span>
          <label
            >Qty<input
              v-model.number="l.qty"
              type="number"
              min="0"
              step="any"
              class="mt-0.5 w-full rounded border border-outline bg-card px-2 py-1"
          /></label>
          <label class="col-span-2"
            >Kondisi
            <select
              v-model="l.condition"
              class="mt-0.5 w-full rounded border border-outline bg-card px-2 py-1"
            >
              <option value="layak">Layak (stok kembali)</option>
              <option value="rusak">Rusak</option>
            </select>
          </label>
        </div>
        <p class="mt-1 text-xs text-secondary">
          Isi qty hanya untuk baris yang diretur. Bila ID baris tidak tersedia, ajukan lewat inbox
          Persetujuan.
        </p>
        <div class="mt-3 grid grid-cols-2 gap-2">
          <AppField label="Kembali via">
            <select
              v-model="metodeRetur"
              class="w-full rounded-lg border border-outline bg-card px-3 py-2 text-sm"
            >
              <option value="tunai">Tunai</option>
              <option value="transfer">Transfer</option>
              <option value="qris">QRIS</option>
              <option value="ewallet">E-wallet</option>
            </select>
          </AppField>
          <AppField label="Alasan (wajib)">
            <input
              v-model="alasanRetur"
              maxlength="500"
              class="w-full rounded-lg border border-outline bg-card px-3 py-2 text-sm"
            />
          </AppField>
        </div>
        <div class="mt-4 flex justify-end gap-2">
          <AppButton variant="ghost" @click="dialogRetur = false">Batal</AppButton>
          <AppButton :loading="loading" @click="returSekarang">Simpan retur</AppButton>
        </div>
      </div>
    </div>
  </div>
</template>
