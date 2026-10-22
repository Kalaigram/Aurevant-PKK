<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { api, type PageMeta } from '../lib/api'
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

interface TItem {
  itemId: number
  qtyRequested: string
  qtyShipped?: string
  qtyReceived?: string
}
interface Transfer {
  id: number
  fromBranchId: number
  toBranchId: number
  status: string
  note: string | null
  items: TItem[]
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
const list = ref<Transfer[]>([])
const meta = ref<PageMeta | null>(null)
const detail = ref<Transfer | null>(null)
const okMsg = ref('')
const showBuat = ref(false)
const fFrom = ref('')
const fTo = ref('')
const fNote = ref('')
const fLines = ref<{ item_id: number | null; qty: number }[]>([{ item_id: null, qty: 1 }])

const boleh = () => access.can('stok.transfer')
const tone = (s: string) =>
  s === 'diterima' || s === 'disetujui'
    ? 'green'
    : s === 'dibatalkan'
      ? 'red'
      : s === 'dikirim'
        ? 'blue'
        : 'amber'
const kode = (id: number) => cabang.value.find((b) => b.id === id)?.code ?? `#${id}`

const AKSI: Record<string, string[]> = {
  draft: ['submit', 'cancel'],
  diajukan: ['approve', 'cancel'],
  disetujui: ['ship'],
  dikirim: ['receive'],
}
const LABEL_AKSI: Record<string, string> = {
  submit: 'Ajukan',
  approve: 'Setujui',
  ship: 'Kirim',
  receive: 'Terima',
  cancel: 'Batalkan',
}

async function muat(page = 1) {
  okMsg.value = ''
  const res = await run(() =>
    api.getPage<Transfer>(
      `/companies/${companyId.value}/stock-transfers?per_page=15&page=${page}`,
      auth.token,
    ),
  )
  if (res) {
    const raw = res as unknown as { data: Transfer[]; meta?: PageMeta } | Transfer[]
    if (Array.isArray(raw)) list.value = raw
    else {
      list.value = raw.data
      meta.value = raw.meta ?? null
    }
  }
}

async function lihat(id: number) {
  detail.value = await run(() =>
    api.get<Transfer>(`/companies/${companyId.value}/stock-transfers/${id}`, auth.token),
  )
}

async function buat() {
  const res = await run(() =>
    api.post(
      `/companies/${companyId.value}/stock-transfers`,
      {
        from_branch_id: Number(fFrom.value),
        to_branch_id: Number(fTo.value),
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
    okMsg.value = 'Transfer dibuat sebagai draft.'
    muat(1)
  }
}

async function jalan(aksi: string) {
  if (!detail.value) return
  const res = await run(() =>
    api.post(
      `/companies/${companyId.value}/stock-transfers/${detail.value!.id}/${aksi}`,
      {},
      auth.token,
    ),
  )
  if (res) {
    okMsg.value = `Transfer ${LABEL_AKSI[aksi]}.`
    lihat(detail.value.id)
    muat(meta.value?.current_page ?? 1)
  }
}

onMounted(async () => {
  await access.load(companyId.value, auth.token)
  cabang.value =
    (await run(() => api.get<Cabang[]>(`/companies/${companyId.value}/branches`, auth.token))) ?? []
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
          <AppIcon name="swap_horiz" :size="28" />
        </div>
        <h1 class="font-display text-xl font-semibold">Transfer stok</h1>
      </div>
      <AppButton v-if="boleh()" @click="showBuat = true"
        ><AppIcon name="add" :size="18" />Transfer baru</AppButton
      >
    </div>
    <AppBanner tone="green" :message="okMsg" />
    <AppBanner tone="red" :message="error" />

    <div class="grid gap-4 lg:grid-cols-5">
      <div class="lg:col-span-3">
        <AppEmpty v-if="!loading && !list.length" message="Belum ada transfer." />
        <AppCard
          v-for="t in list"
          :key="t.id"
          class="cursor-pointer transition hover:shadow"
          @click="lihat(t.id)"
        >
          <div class="flex items-center justify-between gap-2">
            <p class="font-semibold">
              Transfer #{{ t.id }}
              <span class="font-normal text-secondary"
                >{{ kode(t.fromBranchId) }} → {{ kode(t.toBranchId) }}</span
              >
            </p>
            <AppBadge :tone="tone(t.status)">{{ t.status }}</AppBadge>
          </div>
          <p class="mt-1 text-sm text-secondary">
            {{ t.items.length }} jenis barang{{ t.note ? ` · ${t.note}` : '' }}
          </p>
        </AppCard>
        <AppPager :meta="meta" @page="muat" />
      </div>
      <div class="lg:col-span-2">
        <AppEmpty v-if="!detail" message="Pilih transfer untuk detail & tindakan." />
        <AppCard v-if="detail" title="Detail transfer">
          <p class="font-semibold">
            #{{ detail.id }} <AppBadge :tone="tone(detail.status)">{{ detail.status }}</AppBadge>
          </p>
          <p class="text-sm text-secondary">
            {{ kode(detail.fromBranchId) }} → {{ kode(detail.toBranchId) }}
          </p>
          <table class="mt-2 w-full text-sm">
            <tbody>
              <tr
                v-for="(it, i) in detail.items"
                :key="i"
                class="border-b border-surface-low last:border-0"
              >
                <td class="py-1.5">Item #{{ it.itemId }}</td>
                <td class="py-1.5 text-right">
                  minta {{ it.qtyRequested }}{{ it.qtyShipped ? ` · kirim ${it.qtyShipped}` : ''
                  }}{{ it.qtyReceived ? ` · terima ${it.qtyReceived}` : '' }}
                </td>
              </tr>
            </tbody>
          </table>
          <div v-if="boleh()" class="mt-3 flex flex-wrap gap-2">
            <AppButton
              v-for="a in AKSI[detail.status] ?? []"
              :key="a"
              :variant="a === 'cancel' ? 'danger' : 'primary'"
              :loading="loading"
              @click="jalan(a)"
              >{{ LABEL_AKSI[a] }}</AppButton
            >
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
        <h3 class="font-display font-semibold">Transfer baru</h3>
        <div class="mt-3 grid grid-cols-2 gap-2">
          <AppField label="Dari cabang">
            <select
              v-model="fFrom"
              class="w-full rounded-lg bg-surface-low px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-primary"
            >
              <option value="">Pilih…</option>
              <option v-for="b in cabang" :key="b.id" :value="b.id">{{ b.code }}</option>
            </select>
          </AppField>
          <AppField label="Ke cabang">
            <select
              v-model="fTo"
              class="w-full rounded-lg bg-surface-low px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-primary"
            >
              <option value="">Pilih…</option>
              <option v-for="b in cabang" :key="b.id" :value="b.id">{{ b.code }}</option>
            </select>
          </AppField>
        </div>
        <div v-for="(l, i) in fLines" :key="i" class="mt-2 grid grid-cols-3 gap-1">
          <input
            v-model.number="l.item_id"
            type="number"
            placeholder="ID item"
            class="col-span-2 rounded-lg bg-surface-low px-2 py-2 text-sm outline-none focus:ring-2 focus:ring-primary"
          />
          <div class="flex gap-1">
            <input
              v-model.number="l.qty"
              type="number"
              min="0.001"
              step="any"
              placeholder="Qty"
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
        <AppButton variant="ghost" class="mt-1" @click="fLines.push({ item_id: null, qty: 1 })"
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
  </div>
</template>
