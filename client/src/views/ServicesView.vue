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

interface OrderItem {
  itemId: number
  itemName?: string
  qty: string
  price: string
  commissionAmount?: string
}
interface Order {
  id: number
  customerId: number | null
  customerName?: string
  assigneeId: number | null
  status: string
  depositAmount: string
  note: string | null
  items: OrderItem[]
}
interface Cabang {
  id: number
  code: string
  name: string
}
interface Item {
  id: number
  code: string
  name: string
}

const STATUS = ['baru', 'diproses', 'selesai', 'dibayar', 'dibatalkan']
const { auth, companyId } = useCompany()
const access = useAccessStore()
const { loading, error, run } = useAsync()
const cabang = ref<Cabang[]>([])
const branchId = ref<number | null>(null)
const list = ref<Order[]>([])
const meta = ref<PageMeta | null>(null)
const filterStatus = ref('')
const detail = ref<Order | null>(null)
const okMsg = ref('')

const showBuat = ref(false)
const pelanggan = ref<{ id: number; name: string }[]>([])
const jasa = ref<Item[]>([])
const fCustomer = ref('')
const fNote = ref('')
const fLines = ref<{ item_id: number | null; qty: number }[]>([{ item_id: null, qty: 1 }])
const nominalDp = ref(20000)
const idTeknisi = ref('')
const bayarNominal = ref(0)
const bayarMetode = ref('tunai')

const bolehBuat = computed(() => access.can('transaksi.buat'))
const bolehProses = computed(() => access.can('jasa.proses'))

const tone = (s: string) =>
  s === 'dibayar' || s === 'selesai'
    ? 'green'
    : s === 'dibatalkan'
      ? 'red'
      : s === 'diproses'
        ? 'blue'
        : 'amber'
const tersaring = computed(() =>
  !filterStatus.value ? list.value : list.value.filter((o) => o.status === filterStatus.value),
)

async function muat(page = 1) {
  okMsg.value = ''
  if (!branchId.value) return
  const res = await run(() =>
    api.getPage<Order>(
      `/companies/${companyId.value}/branches/${branchId.value}/service-orders?per_page=15&page=${page}`,
      auth.token,
    ),
  )
  if (res) {
    const raw = res as unknown as { data: Order[]; meta?: PageMeta } | Order[]
    if (Array.isArray(raw)) list.value = raw
    else {
      list.value = raw.data
      meta.value = raw.meta ?? null
    }
  }
}

async function lihat(id: number) {
  detail.value = await run(() =>
    api.get<Order>(
      `/companies/${companyId.value}/branches/${branchId.value}/service-orders/${id}`,
      auth.token,
    ),
  )
}

function gantiCabang() {
  void muat(1)
  void muatReferensi()
}

async function muatReferensi() {
  if (!branchId.value) return
  pelanggan.value =
    (await run(() =>
      api.get<{ id: number; name: string }[]>(
        `/companies/${companyId.value}/customers?per_page=100`,
        auth.token,
      ),
    )) ?? []
  const items =
    (await run(() =>
      api.get<Item[]>(`/companies/${companyId.value}/items?type=jasa&per_page=100`, auth.token),
    )) ?? []
  jasa.value = items
}

async function buat() {
  if (!branchId.value) return
  const res = await run(() =>
    api.post<Order>(
      `/companies/${companyId.value}/branches/${branchId.value}/service-orders`,
      {
        customer_id: fCustomer.value ? Number(fCustomer.value) : undefined,
        items: fLines.value
          .filter((l) => l.item_id)
          .map((l) => ({ item_id: l.item_id, qty: l.qty })),
        note: fNote.value || undefined,
      },
      auth.token,
    ),
  )
  if (res) {
    showBuat.value = false
    fCustomer.value = ''
    fNote.value = ''
    fLines.value = [{ item_id: null, qty: 1 }]
    okMsg.value = 'Order jasa dibuat.'
    muat(1)
  }
}

async function aksi(path: string, body: unknown, pesan: string) {
  if (!detail.value || !branchId.value) return
  const res = await run(() =>
    api.post(
      `/companies/${companyId.value}/branches/${branchId.value}/service-orders/${detail.value!.id}/${path}`,
      body,
      auth.token,
    ),
  )
  if (res) {
    okMsg.value = pesan
    lihat(detail.value.id)
    muat(meta.value?.current_page ?? 1)
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
  muatReferensi()
})
</script>

<template>
  <div>
    <div class="mb-4 flex flex-wrap items-center justify-between gap-2">
      <div class="flex items-center gap-3">
        <div
          class="flex h-12 w-12 items-center justify-center rounded-xl bg-primary-container text-on-primary"
        >
          <AppIcon name="engineering" :size="28" />
        </div>
        <h1 class="font-display text-xl font-semibold">Order jasa</h1>
      </div>
      <AppButton v-if="bolehBuat" @click="showBuat = true"
        ><AppIcon name="add" :size="18" />Order baru</AppButton
      >
    </div>
    <AppBanner tone="green" :message="okMsg" />
    <AppBanner tone="red" :message="error" />

    <div class="mb-3 flex flex-wrap gap-2">
      <select
        v-model="branchId"
        class="rounded-lg border border-outline bg-card px-3 py-2 text-sm"
        @change="gantiCabang"
      >
        <option v-for="b in cabang" :key="b.id" :value="b.id">{{ b.code }} — {{ b.name }}</option>
      </select>
      <select
        v-model="filterStatus"
        class="rounded-lg border border-outline bg-card px-3 py-2 text-sm"
      >
        <option value="">Semua status</option>
        <option v-for="s in STATUS" :key="s" :value="s">{{ s }}</option>
      </select>
    </div>

    <div class="grid gap-4 lg:grid-cols-5">
      <div class="lg:col-span-3">
        <AppEmpty v-if="!loading && !tersaring.length" message="Belum ada order jasa." />
        <AppCard
          v-for="o in tersaring"
          :key="o.id"
          class="cursor-pointer transition hover:shadow"
          @click="lihat(o.id)"
        >
          <div class="flex items-center justify-between gap-2">
            <p class="font-semibold">
              Order #{{ o.id }}
              <span class="font-normal text-secondary">{{
                o.customerName ?? (o.customerId ? `pelanggan #${o.customerId}` : 'tanpa nama')
              }}</span>
            </p>
            <AppBadge :tone="tone(o.status)">{{ o.status }}</AppBadge>
          </div>
          <p class="mt-1 text-sm text-secondary">
            {{ o.items.length }} pekerjaan · DP {{ rupiah(o.depositAmount)
            }}{{ o.note ? ` · ${o.note}` : '' }}
          </p>
        </AppCard>
        <AppPager :meta="meta" @page="muat" />
      </div>

      <div class="lg:col-span-2">
        <AppEmpty v-if="!detail" message="Pilih order untuk detail & tindakan." />
        <AppCard v-if="detail" title="Detail order">
          <p class="font-semibold">
            Order #{{ detail.id }}
            <AppBadge :tone="tone(detail.status)">{{ detail.status }}</AppBadge>
          </p>
          <div
            v-for="(it, i) in detail.items"
            :key="i"
            class="flex justify-between border-b border-surface-low py-1.5 text-sm last:border-0"
          >
            <span
              >{{ it.itemName ?? `Item #${it.itemId}` }} × {{ it.qty
              }}{{ it.commissionAmount ? ` (komisi ${rupiah(it.commissionAmount)})` : '' }}</span
            >
            <span>{{ rupiah(it.price) }}</span>
          </div>
          <p class="mt-2 text-sm text-secondary">DP masuk: {{ rupiah(detail.depositAmount) }}</p>
          <div class="mt-3 flex flex-wrap gap-2">
            <template
              v-if="bolehProses && (detail.status === 'baru' || detail.status === 'diproses')"
            >
              <input
                v-model="idTeknisi"
                type="number"
                placeholder="ID teknisi"
                class="w-28 rounded-lg border border-outline bg-card px-2 py-2 text-sm"
              />
              <AppButton
                :loading="loading"
                @click="
                  aksi(
                    'process',
                    { assignee_id: idTeknisi ? Number(idTeknisi) : undefined },
                    'Order diproses.',
                  )
                "
                >Proses</AppButton
              >
              <AppButton
                v-if="detail.status === 'diproses'"
                :loading="loading"
                @click="aksi('complete', {}, 'Order selesai, bahan terpotong.')"
                >Selesai</AppButton
              >
            </template>
            <template v-if="bolehBuat">
              <span v-if="detail.status === 'baru'" class="flex gap-1">
                <input
                  v-model.number="nominalDp"
                  type="number"
                  min="1"
                  class="w-28 rounded-lg border border-outline bg-card px-2 py-2 text-sm"
                  placeholder="DP"
                />
                <AppButton
                  variant="ghost"
                  @click="aksi('deposit', { amount: nominalDp }, 'DP tercatat.')"
                  >Catat DP</AppButton
                >
              </span>
              <span v-if="detail.status === 'selesai'" class="flex gap-1">
                <select
                  v-model="bayarMetode"
                  class="rounded-lg border border-outline bg-card px-2 py-2 text-sm"
                >
                  <option value="tunai">Tunai</option>
                  <option value="transfer">Transfer</option>
                  <option value="qris">QRIS</option>
                  <option value="ewallet">E-wallet</option>
                </select>
                <input
                  v-model.number="bayarNominal"
                  type="number"
                  min="1"
                  class="w-32 rounded-lg border border-outline bg-card px-2 py-2 text-sm"
                  placeholder="Nominal"
                />
                <AppButton
                  variant="secondary"
                  @click="
                    aksi(
                      'pay',
                      { payments: [{ method: bayarMetode, amount: bayarNominal }] },
                      'Dibayar.',
                    )
                  "
                  >Bayar</AppButton
                >
              </span>
              <AppButton
                v-if="['baru', 'diproses'].includes(detail.status)"
                variant="danger"
                @click="aksi('cancel', {}, 'Order dibatalkan.')"
                >Batalkan</AppButton
              >
            </template>
          </div>
          <p class="mt-2 text-xs text-secondary">
            DP lama otomatis dihitung sebagai pembayaran pertama.
          </p>
        </AppCard>
      </div>
    </div>

    <div
      v-if="showBuat"
      class="fixed inset-0 z-20 flex items-center justify-center bg-black/40 p-4"
      @click.self="showBuat = false"
    >
      <div class="max-h-[90vh] w-full max-w-md overflow-y-auto rounded-xl bg-card p-5 shadow-xl">
        <h3 class="font-display font-semibold">Order jasa baru</h3>
        <div class="mt-3 grid gap-2">
          <AppField label="Pelanggan (opsional)">
            <select
              v-model="fCustomer"
              class="w-full rounded-lg bg-surface-low px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-primary"
            >
              <option value="">Tanpa nama</option>
              <option v-for="p in pelanggan" :key="p.id" :value="p.id">{{ p.name }}</option>
            </select>
          </AppField>
          <div v-for="(l, i) in fLines" :key="i" class="grid grid-cols-3 gap-1">
            <select
              v-model="l.item_id"
              class="col-span-2 rounded-lg bg-surface-low px-2 py-2 text-sm outline-none focus:ring-2 focus:ring-primary"
            >
              <option :value="null">Pilih jasa…</option>
              <option v-for="j in jasa" :key="j.id" :value="j.id">{{ j.name }}</option>
            </select>
            <div class="flex gap-1">
              <input
                v-model.number="l.qty"
                type="number"
                min="0.001"
                step="any"
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
          <AppButton variant="ghost" @click="fLines.push({ item_id: null, qty: 1 })"
            >+ Pekerjaan</AppButton
          >
          <AppField label="Catatan">
            <input
              v-model="fNote"
              maxlength="500"
              class="w-full rounded-lg bg-surface-low px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-primary"
              placeholder="cth. Beat brebet"
            />
          </AppField>
        </div>
        <div class="mt-4 flex justify-end gap-2">
          <AppButton variant="ghost" @click="showBuat = false">Batal</AppButton>
          <AppButton :loading="loading" @click="buat">Buat order</AppButton>
        </div>
      </div>
    </div>
  </div>
</template>
