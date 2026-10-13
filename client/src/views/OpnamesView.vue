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

interface OItem {
  itemId: number
  qtySystem: string
  qtyCounted: string
}
interface Opname {
  id: number
  branchId: number
  status: string
  note: string | null
  items: OItem[]
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
const list = ref<Opname[]>([])
const meta = ref<PageMeta | null>(null)
const detail = ref<Opname | null>(null)
const okMsg = ref('')
const showBuat = ref(false)
const fLines = ref<{ item_id: number | null; qty_counted: number }[]>([
  { item_id: null, qty_counted: 0 },
])
const fNote = ref('')

const boleh = () => access.can('stok.opname')
const tone = (s: string) => (s === 'disetujui' ? 'green' : s === 'ditolak' ? 'red' : 'amber')
const selisih = (it: OItem) => Number(it.qtyCounted) - Number(it.qtySystem)

async function muat(page = 1) {
  okMsg.value = ''
  if (!branchId.value) return
  const res = await run(() =>
    api.get<{ data: Opname[]; meta: PageMeta }>(
      `/companies/${companyId.value}/branches/${branchId.value}/stock/opnames?per_page=15&page=${page}`,
      auth.token,
    ),
  )
  if (res) {
    const raw = res as unknown as { data: Opname[]; meta?: PageMeta } | Opname[]
    if (Array.isArray(raw)) list.value = raw
    else {
      list.value = raw.data
      meta.value = raw.meta ?? null
    }
  }
}

async function lihat(id: number) {
  if (!branchId.value) return
  detail.value = await run(() =>
    api.get<Opname>(
      `/companies/${companyId.value}/branches/${branchId.value}/stock/opnames/${id}`,
      auth.token,
    ),
  )
}

async function buat() {
  if (!branchId.value) return
  const res = await run(() =>
    api.post(
      `/companies/${companyId.value}/branches/${branchId.value}/stock/opnames`,
      {
        items: fLines.value
          .filter((l) => l.item_id)
          .map((l) => ({ item_id: l.item_id, qty_counted: l.qty_counted })),
        note: fNote.value || undefined,
      },
      auth.token,
    ),
  )
  if (res) {
    showBuat.value = false
    okMsg.value = 'Opname dibuat sebagai draft.'
    fLines.value = [{ item_id: null, qty_counted: 0 }]
    fNote.value = ''
    muat(1)
  }
}

async function jalan(aksi: 'submit' | 'approve' | 'reject') {
  if (!detail.value || !branchId.value) return
  const res = await run(() =>
    api.post(
      `/companies/${companyId.value}/branches/${branchId.value}/stock/opnames/${detail.value!.id}/${aksi}`,
      {},
      auth.token,
    ),
  )
  if (res) {
    okMsg.value =
      aksi === 'submit'
        ? 'Opname diajukan.'
        : aksi === 'approve'
          ? 'Opname disetujui, stok menyesuaikan.'
          : 'Opname ditolak.'
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
})
</script>

<template>
  <div>
    <div class="mb-4 flex flex-wrap items-center justify-between gap-2">
      <div class="flex items-center gap-3">
        <div
          class="flex h-12 w-12 items-center justify-center rounded-xl bg-primary-container text-on-primary"
        >
          <AppIcon name="content_paste_search" :size="28" />
        </div>
        <h1 class="font-display text-xl font-semibold">Stok opname</h1>
      </div>
      <AppButton v-if="boleh()" @click="showBuat = true"
        ><AppIcon name="add" :size="18" />Opname baru</AppButton
      >
    </div>
    <AppBanner tone="green" :message="okMsg" />
    <AppBanner tone="red" :message="error" />

    <div class="mb-3">
      <select
        v-model="branchId"
        class="rounded-lg border border-outline bg-card px-3 py-2 text-sm"
        @change="muat(1)"
      >
        <option v-for="b in cabang" :key="b.id" :value="b.id">{{ b.code }} — {{ b.name }}</option>
      </select>
    </div>

    <div class="grid gap-4 lg:grid-cols-5">
      <div class="lg:col-span-3">
        <AppEmpty v-if="!loading && !list.length" message="Belum ada opname." />
        <AppCard
          v-for="o in list"
          :key="o.id"
          class="cursor-pointer transition hover:shadow"
          @click="lihat(o.id)"
        >
          <div class="flex items-center justify-between gap-2">
            <p class="font-semibold">Opname #{{ o.id }}</p>
            <AppBadge :tone="tone(o.status)">{{ o.status }}</AppBadge>
          </div>
          <p class="mt-1 text-sm text-secondary">
            {{ o.items.length }} baris{{ o.note ? ` · ${o.note}` : '' }}
          </p>
        </AppCard>
        <AppPager :meta="meta" @page="muat" />
      </div>
      <div class="lg:col-span-2">
        <AppEmpty v-if="!detail" message="Pilih opname untuk detail." />
        <AppCard v-if="detail" title="Detail opname">
          <p class="font-semibold">
            #{{ detail.id }} <AppBadge :tone="tone(detail.status)">{{ detail.status }}</AppBadge>
          </p>
          <table class="mt-2 w-full text-sm">
            <thead>
              <tr class="text-left text-xs uppercase text-secondary">
                <th class="py-1">Item</th>
                <th class="py-1 text-right">Sistem</th>
                <th class="py-1 text-right">Hitung</th>
                <th class="py-1 text-right">Selisih</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(it, i) in detail.items" :key="i" class="border-t border-surface-low">
                <td class="py-1.5">Item #{{ it.itemId }}</td>
                <td class="py-1.5 text-right font-mono">{{ it.qtySystem }}</td>
                <td class="py-1.5 text-right font-mono">{{ it.qtyCounted }}</td>
                <td
                  class="py-1.5 text-right font-mono"
                  :class="selisih(it) !== 0 ? 'font-bold text-amber-700' : ''"
                >
                  {{ selisih(it) }}
                </td>
              </tr>
            </tbody>
          </table>
          <div v-if="boleh()" class="mt-3 flex flex-wrap gap-2">
            <AppButton v-if="detail.status === 'draft'" :loading="loading" @click="jalan('submit')"
              >Ajukan</AppButton
            >
            <AppButton
              v-if="detail.status === 'diajukan'"
              :loading="loading"
              @click="jalan('approve')"
              >Setujui</AppButton
            >
            <AppButton
              v-if="detail.status === 'diajukan'"
              variant="danger"
              :loading="loading"
              @click="jalan('reject')"
              >Tolak</AppButton
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
        <h3 class="font-display font-semibold">Opname baru</h3>
        <div v-for="(l, i) in fLines" :key="i" class="mt-2 grid grid-cols-2 gap-1">
          <input
            v-model.number="l.item_id"
            type="number"
            placeholder="ID item"
            class="rounded-lg bg-surface-low px-2 py-2 text-sm outline-none focus:ring-2 focus:ring-primary"
          />
          <div class="flex gap-1">
            <input
              v-model.number="l.qty_counted"
              type="number"
              min="0"
              step="any"
              placeholder="Hasil hitung"
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
          @click="fLines.push({ item_id: null, qty_counted: 0 })"
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
