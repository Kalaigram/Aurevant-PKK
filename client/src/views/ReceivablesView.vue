<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { api } from '../lib/api'
import { rupiah, tanggal } from '../lib/format'
import { useCompany } from '../composables/useCompany'
import { useAccessStore } from '../stores/access'
import { useAsync } from '../composables/useAsync'
import AppCard from '../components/ui/AppCard.vue'
import AppBadge from '../components/ui/AppBadge.vue'
import AppBanner from '../components/ui/AppBanner.vue'
import AppButton from '../components/ui/AppButton.vue'
import AppField from '../components/ui/AppField.vue'
import AppEmpty from '../components/ui/AppEmpty.vue'

interface Bon {
  id: number
  branchId?: number
  customerId?: number | null
  amount: string
  paidAmount: string
  remaining: string
  dueAt?: string | null
  isPaid: boolean
}
interface Cabang {
  id: number
  name: string
}

const { auth, companyId } = useCompany()
const access = useAccessStore()
const { loading, error, run } = useAsync()
const list = ref<Bon[]>([])
const aging = ref<{ buckets: Record<string, number>; total: number } | null>(null)
const hanyaBelumLunas = ref(true)
const okMsg = ref('')
const cabang = ref<Cabang[]>([])
const showForm = ref(false)
const fBon = ref({ branch_id: '', customer_id: '', amount: 0, due_at: '', note: '' })
const bayarId = ref<number | null>(null)
const bayarNominal = ref(0)
const bayarCara = ref('tunai')

const boleh = computed(() => access.can('biaya.setujui'))

async function muat() {
  okMsg.value = ''
  const res = await run(() =>
    api.get<Bon[]>(
      `/companies/${companyId.value}/receivables?per_page=50${hanyaBelumLunas.value ? '&unpaid=true' : ''}`,
      auth.token,
    ),
  )
  if (res)
    list.value = (
      Array.isArray(res) ? res : ((res as unknown as { data: Bon[] }).data ?? [])
    ) as Bon[]
  const ag = await api
    .get<{ buckets: Record<string, number>; total: number }>(
      `/companies/${companyId.value}/receivables-aging`,
      auth.token,
    )
    .catch(() => null)
  if (ag) aging.value = ag as { buckets: Record<string, number>; total: number }
  cabang.value =
    (await api
      .get<Cabang[]>(`/companies/${companyId.value}/branches`, auth.token)
      .catch(() => [])) ?? []
}

async function simpan() {
  const res = await run(() =>
    api.post(
      `/companies/${companyId.value}/receivables`,
      {
        branch_id: Number(fBon.value.branch_id),
        customer_id: fBon.value.customer_id === '' ? undefined : Number(fBon.value.customer_id),
        amount: Number(fBon.value.amount),
        due_at: fBon.value.due_at || undefined,
        note: fBon.value.note || undefined,
      },
      auth.token,
    ),
  )
  if (res) {
    showForm.value = false
    okMsg.value = 'Bon pelanggan tercatat.'
    fBon.value = { branch_id: '', customer_id: '', amount: 0, due_at: '', note: '' }
    muat()
  }
}

function mulaiTerima(b: Bon) {
  bayarId.value = b.id
  bayarNominal.value = Number(b.remaining)
}

async function terima(b: Bon) {
  if (!bayarNominal.value || bayarNominal.value <= 0) return
  const res = await run(() =>
    api.post(
      `/companies/${companyId.value}/receivables/${b.id}/pay`,
      {
        amount: Number(bayarNominal.value),
        method: bayarCara.value,
      },
      auth.token,
    ),
  )
  if (res || !error.value) {
    okMsg.value = 'Cicilan tercatat.'
    bayarId.value = null
    bayarNominal.value = 0
    muat()
  }
}

onMounted(async () => {
  await access.load(companyId.value, auth.token)
  await muat()
})
</script>

<template>
  <div>
    <p class="text-xs font-semibold uppercase tracking-wider text-secondary">
      Tagihan ke pelanggan
    </p>
    <div class="mb-4 flex flex-wrap items-center justify-between gap-2">
      <h1 class="font-display text-xl font-semibold">Piutang (bon)</h1>
      <AppButton v-if="boleh" @click="showForm = true">Catat bon</AppButton>
    </div>
    <AppBanner tone="green" :message="okMsg" />
    <AppBanner tone="red" :message="error" />

    <div v-if="aging" class="mb-4 grid grid-cols-2 gap-3 lg:grid-cols-6">
      <AppCard v-for="(v, k) in aging.buckets" :key="k" class="!p-3">
        <p class="text-xs text-secondary">{{ k }}</p>
        <p class="font-display text-lg font-bold">{{ rupiah(v) }}</p>
      </AppCard>
      <AppCard class="!p-3"
        ><p class="text-xs text-secondary">Total</p>
        <p class="font-display text-lg font-bold">{{ rupiah(aging.total) }}</p></AppCard
      >
    </div>

    <label class="mb-3 flex items-center gap-2 text-sm">
      <input
        v-model="hanyaBelumLunas"
        type="checkbox"
        class="h-4 w-4 accent-emerald-700"
        @change="() => muat()"
      />
      Hanya yang belum lunas
    </label>

    <p v-if="loading" class="text-sm text-secondary">Memuat…</p>
    <AppEmpty v-else-if="!list.length" message="Tidak ada bon pada filter ini." />
    <div class="grid gap-3 md:grid-cols-2">
      <AppCard v-for="b in list" :key="b.id">
        <div class="flex items-center justify-between gap-2">
          <strong class="font-display">Bon #{{ b.id }}</strong>
          <AppBadge :tone="b.isPaid ? 'green' : 'amber'">{{
            b.isPaid ? 'Lunas' : 'Belum lunas'
          }}</AppBadge>
        </div>
        <div class="mt-2 h-2 overflow-hidden rounded bg-surface-low">
          <div
            class="h-full bg-primary-container"
            :style="{
              width: `${Number(b.amount) ? (Number(b.paidAmount) / Number(b.amount)) * 100 : 0}%`,
            }"
          />
        </div>
        <p class="mt-1 text-sm">
          {{ rupiah(b.paidAmount) }} dari {{ rupiah(b.amount) }} · sisa
          <strong>{{ rupiah(b.remaining) }}</strong>
        </p>
        <p class="text-xs text-secondary">
          Jatuh tempo: {{ tanggal(b.dueAt)
          }}{{ b.customerId ? ` · Pelanggan #${b.customerId}` : ' · Tanpa nama' }}
        </p>
        <div v-if="!b.isPaid && boleh" class="mt-2">
          <AppButton v-if="bayarId !== b.id" variant="ghost" @click="mulaiTerima(b)">Terima cicilan</AppButton>
          <div v-else class="flex flex-wrap items-end gap-2">
            <AppField label="Nominal (Rp)"
              ><input
                v-model.number="bayarNominal"
                type="number"
                min="0"
                class="w-32 rounded-lg bg-surface-low px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-primary"
            /></AppField>
            <AppField label="Cara"
              ><select v-model="bayarCara" class="rounded-lg bg-surface-low px-3 py-2 text-sm">
                <option value="tunai">Tunai</option>
                <option value="transfer">Transfer</option>
                <option value="qris">QRIS</option>
                <option value="ewallet">E-wallet</option>
              </select></AppField
            >
            <AppButton @click="terima(b)">Simpan</AppButton>
            <AppButton variant="ghost" @click="bayarId = null">Batal</AppButton>
          </div>
        </div>
      </AppCard>
    </div>

    <div
      v-if="showForm"
      class="fixed inset-0 z-20 flex items-center justify-center bg-black/40 p-4"
      @click.self="showForm = false"
    >
      <form class="w-full max-w-md rounded-xl bg-card p-5 shadow-xl" @submit.prevent="simpan">
        <h3 class="font-display font-semibold">Catat bon baru</h3>
        <div class="mt-2 grid grid-cols-2 gap-2">
          <AppField label="Cabang"
            ><select
              v-model="fBon.branch_id"
              class="w-full rounded-lg bg-surface-low px-3 py-2 text-sm"
            >
              <option value="">— pilih —</option>
              <option v-for="c in cabang" :key="c.id" :value="c.id">{{ c.name }}</option>
            </select></AppField
          >
          <AppField label="ID pelanggan (opsional)"
            ><input
              v-model="fBon.customer_id"
              inputmode="numeric"
              placeholder="Kosong = tanpa nama"
              class="w-full rounded-lg bg-surface-low px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-primary"
          /></AppField>
        </div>
        <div class="grid grid-cols-2 gap-2">
          <AppField label="Nominal (Rp)"
            ><input
              v-model.number="fBon.amount"
              type="number"
              min="0"
              class="w-full rounded-lg bg-surface-low px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-primary"
          /></AppField>
          <AppField label="Jatuh tempo"
            ><input
              v-model="fBon.due_at"
              type="date"
              class="w-full rounded-lg bg-surface-low px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-primary"
          /></AppField>
        </div>
        <AppField label="Catatan"
          ><input
            v-model="fBon.note"
            maxlength="500"
            class="w-full rounded-lg bg-surface-low px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-primary"
        /></AppField>
        <div class="mt-3 flex justify-end gap-2">
          <AppButton variant="ghost" type="button" @click="showForm = false">Batal</AppButton>
          <AppButton type="submit">Simpan</AppButton>
        </div>
      </form>
    </div>
  </div>
</template>
