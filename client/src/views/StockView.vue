<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { api, type PageMeta } from '../lib/api'
import { useCompany } from '../composables/useCompany'
import { useAccessStore } from '../stores/access'
import { useAsync } from '../composables/useAsync'
import AppBanner from '../components/ui/AppBanner.vue'
import AppButton from '../components/ui/AppButton.vue'
import AppField from '../components/ui/AppField.vue'
import AppEmpty from '../components/ui/AppEmpty.vue'
import AppPager from '../components/ui/AppPager.vue'
import AppIcon from '../components/ui/AppIcon.vue'

interface Saldo {
  itemId: number
  itemCode?: string
  itemName?: string
  qty: string
  avgCost?: string
}
interface Gerak {
  id: number
  itemId: number
  type: string
  qty: string
  unitCost: string | null
  balanceAfter: string
  note: string | null
  createdAt?: string
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
const tab = ref<'saldo' | 'riwayat'>('saldo')
const saldo = ref<Saldo[]>([])
const gerak = ref<Gerak[]>([])
const meta = ref<PageMeta | null>(null)
const cari = ref('')
const okMsg = ref('')
const showSesuaikan = ref(false)
const fItem = ref('')
const fTipe = ref('penyesuaian')
const fQty = ref(0)
const fCost = ref(0)

const bolehSesuaikan = computed(() => access.can('stok.opname'))
const tersaring = computed(() => {
  const q = cari.value.trim().toLowerCase()
  if (!q) return saldo.value
  return saldo.value.filter(
    (s) =>
      String(s.itemId).includes(q) ||
      (s.itemName ?? '').toLowerCase().includes(q) ||
      (s.itemCode ?? '').toLowerCase().includes(q),
  )
})

const saldoMeta = ref<PageMeta | null>(null)

async function muatSaldo(page = 1) {
  if (!branchId.value) return
  const res = await run(() =>
    api.get<{ data: Saldo[]; meta: PageMeta }>(
      `/companies/${companyId.value}/branches/${branchId.value}/stock?per_page=50&page=${page}`,
      auth.token,
    ),
  )
  if (res) {
    const raw = res as unknown as { data: Saldo[]; meta?: PageMeta } | Saldo[]
    if (Array.isArray(raw)) saldo.value = raw
    else {
      saldo.value = raw.data
      saldoMeta.value = raw.meta ?? null
    }
  }
}

async function muatRiwayat(page = 1) {
  if (!branchId.value) return
  const res = await run(() =>
    api.get<{ data: Gerak[]; meta: PageMeta }>(
      `/companies/${companyId.value}/branches/${branchId.value}/stock/movements?per_page=15&page=${page}`,
      auth.token,
    ),
  )
  if (res) {
    const raw = res as unknown as { data: Gerak[]; meta?: PageMeta } | Gerak[]
    if (Array.isArray(raw)) gerak.value = raw
    else {
      gerak.value = raw.data
      meta.value = raw.meta ?? null
    }
  }
}

async function simpanSesuai() {
  if (!branchId.value) return
  const res = await run(() =>
    api.post(
      `/companies/${companyId.value}/branches/${branchId.value}/stock/movements`,
      {
        item_id: Number(fItem.value),
        type: fTipe.value,
        qty: fQty.value,
        ...(fTipe.value === 'pembelian' ? { unit_cost: fCost.value } : {}),
      },
      auth.token,
    ),
  )
  if (res) {
    showSesuaikan.value = false
    okMsg.value = 'Mutasi tercatat di audit.'
    fItem.value = ''
    fQty.value = 0
    muatSaldo()
    muatRiwayat(1)
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
  muatSaldo(1)
  muatRiwayat(1)
})
</script>

<template>
  <div>
    <div class="mb-4 flex flex-wrap items-center justify-between gap-2">
      <div class="flex items-center gap-3">
        <div
          class="flex h-12 w-12 items-center justify-center rounded-xl bg-primary-container text-on-primary"
        >
          <AppIcon name="inventory_2" :size="28" />
        </div>
        <h1 class="font-display text-xl font-semibold">Stok cabang</h1>
      </div>
      <AppButton v-if="bolehSesuaikan" @click="showSesuaikan = true">Penyesuaian manual</AppButton>
    </div>
    <AppBanner tone="green" :message="okMsg" />
    <AppBanner tone="red" :message="error" />

    <div class="mb-3 flex flex-wrap gap-2">
      <select
        v-model="branchId"
        class="rounded-lg border border-outline bg-card px-3 py-2 text-sm"
        @change="
          muatSaldo(1)
          muatRiwayat(1)
        "
      >
        <option v-for="b in cabang" :key="b.id" :value="b.id">{{ b.code }} — {{ b.name }}</option>
      </select>
      <AppButton :variant="tab === 'saldo' ? 'primary' : 'ghost'" @click="tab = 'saldo'"
        >Saldo</AppButton
      >
      <AppButton :variant="tab === 'riwayat' ? 'primary' : 'ghost'" @click="tab = 'riwayat'"
        >Riwayat mutasi</AppButton
      >
    </div>

    <div v-if="tab === 'saldo'">
      <AppField label="Cari kode / nama">
        <input
          v-model="cari"
          class="max-w-sm rounded-lg border border-outline bg-card px-3 py-2.5 text-sm"
          placeholder="cth. OLI"
        />
      </AppField>
      <AppEmpty v-if="!loading && !tersaring.length" message="Tidak ada stok." />
      <div class="mt-3 overflow-x-auto rounded-xl bg-card shadow-sm">
        <table class="w-full min-w-[480px] text-sm">
          <thead>
            <tr class="text-left text-xs uppercase text-secondary">
              <th class="px-4 py-2">Item</th>
              <th class="px-4 py-2 text-right">Qty</th>
              <th class="px-4 py-2 text-right">Rata-rata modal</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="s in tersaring" :key="s.itemId" class="border-t border-surface-low">
              <td class="px-4 py-2">
                <strong>{{ s.itemName ?? `Item #${s.itemId}` }}</strong>
                <span class="font-mono text-xs text-secondary">{{ s.itemCode ?? '' }}</span>
              </td>
              <td class="px-4 py-2 text-right font-mono">{{ s.qty }}</td>
              <td class="px-4 py-2 text-right font-mono">{{ s.avgCost ?? '–' }}</td>
            </tr>
          </tbody>
        </table>
      </div>
      <AppPager :meta="saldoMeta" @page="muatSaldo" />
    </div>

    <div v-else>
      <div class="overflow-x-auto rounded-xl bg-card shadow-sm">
        <table class="w-full min-w-[560px] text-sm">
          <thead>
            <tr class="text-left text-xs uppercase text-secondary">
              <th class="px-4 py-2">Tipe</th>
              <th class="px-4 py-2 text-right">Qty</th>
              <th class="px-4 py-2 text-right">Saldo sesudah</th>
              <th class="px-4 py-2">Catatan</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="g in gerak" :key="g.id" class="border-t border-surface-low">
              <td class="px-4 py-2">
                <span class="rounded bg-surface-low px-2 py-0.5 text-xs font-semibold">{{
                  g.type
                }}</span>
              </td>
              <td
                class="px-4 py-2 text-right font-mono"
                :class="Number(g.qty) < 0 ? 'text-danger' : ''"
              >
                {{ g.qty }}
              </td>
              <td class="px-4 py-2 text-right font-mono">{{ g.balanceAfter }}</td>
              <td class="px-4 py-2 text-secondary">{{ g.note ?? '–' }}</td>
            </tr>
          </tbody>
        </table>
      </div>
      <AppPager :meta="meta" @page="muatRiwayat" />
    </div>

    <div
      v-if="showSesuaikan"
      class="fixed inset-0 z-20 flex items-center justify-center bg-black/40 p-4"
      @click.self="showSesuaikan = false"
    >
      <form class="w-full max-w-sm rounded-xl bg-card p-5 shadow-xl" @submit.prevent="simpanSesuai">
        <h3 class="font-display font-semibold">Penyesuaian manual</h3>
        <p class="text-xs text-secondary">
          Tercatat di audit. Bukan untuk jualan (jualan lewat kasir).
        </p>
        <div class="mt-3">
          <AppField label="ID item (wajib)">
            <input
              v-model="fItem"
              type="number"
              class="w-full rounded-lg bg-surface-low px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-primary"
            />
          </AppField>
        </div>
        <div class="mt-3 grid grid-cols-2 gap-2">
          <AppField label="Tipe">
            <select
              v-model="fTipe"
              class="w-full rounded-lg bg-surface-low px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-primary"
            >
              <option value="pembelian">Masuk</option>
              <option value="penjualan">Keluar</option>
              <option value="penyesuaian">Penyesuaian</option>
            </select>
          </AppField>
          <AppField label="Qty (negatif = keluar)">
            <input
              v-model.number="fQty"
              type="number"
              step="any"
              class="w-full rounded-lg bg-surface-low px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-primary"
            />
          </AppField>
        </div>
        <div v-if="fTipe === 'pembelian'" class="mt-3">
          <AppField label="Modal per unit (wajib untuk masuk)">
            <input
              v-model.number="fCost"
              type="number"
              min="0"
              class="w-full rounded-lg bg-surface-low px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-primary"
            />
          </AppField>
        </div>
        <div class="mt-4 flex justify-end gap-2">
          <AppButton variant="ghost" @click="showSesuaikan = false">Batal</AppButton>
          <AppButton type="submit" :loading="loading">Catat</AppButton>
        </div>
      </form>
    </div>
  </div>
</template>
