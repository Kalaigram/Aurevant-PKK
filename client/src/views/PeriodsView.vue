<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { api } from '../lib/api'
import { tanggal } from '../lib/format'
import { useCompany } from '../composables/useCompany'
import { useAccessStore } from '../stores/access'
import { useAsync } from '../composables/useAsync'
import AppCard from '../components/ui/AppCard.vue'
import AppBadge from '../components/ui/AppBadge.vue'
import AppBanner from '../components/ui/AppBanner.vue'
import AppButton from '../components/ui/AppButton.vue'
import AppEmpty from '../components/ui/AppEmpty.vue'

interface Periode {
  id: number
  year: number
  month: number
  label: string
  status: string
  closedAt?: string | null
}

const { auth, companyId } = useCompany()
const access = useAccessStore()
const { loading, error, run } = useAsync()
const list = ref<Periode[]>([])
const okMsg = ref('')
const tahun = ref(new Date().getFullYear())
const bulan = ref(new Date().getMonth() + 1)

const boleh = computed(() => access.can('periode.tutup'))
const terbuka = computed(() => list.value.filter((p) => p.status === 'buka').length)
const tertutup = computed(() => list.value.filter((p) => p.status !== 'buka').length)

async function muat() {
  okMsg.value = ''
  const res = await run(() =>
    api.get<Periode[]>(`/companies/${companyId.value}/accounting-periods?per_page=50`, auth.token),
  )
  if (res)
    list.value = (
      Array.isArray(res) ? res : ((res as unknown as { data: Periode[] }).data ?? [])
    ) as Periode[]
}

async function tutup() {
  const res = await run(() =>
    api.post(
      `/companies/${companyId.value}/accounting-periods/close`,
      {
        year: Number(tahun.value),
        month: Number(bulan.value),
      },
      auth.token,
    ),
  )
  if (res || !error.value) {
    okMsg.value = 'Periode ditutup. Posting ke bulan itu ditolak.'
    muat()
  }
}

async function bukaLagi(p: Periode) {
  const res = await run(() =>
    api.post(`/companies/${companyId.value}/accounting-periods/${p.id}/reopen`, {}, auth.token),
  )
  if (res || !error.value) {
    okMsg.value = `Periode ${p.label} dibuka lagi.`
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
    <p class="text-xs font-semibold uppercase tracking-wider text-secondary">Kunci pembukuan</p>
    <h1 class="font-display mb-4 text-xl font-semibold">Tutup periode</h1>
    <AppBanner tone="green" :message="okMsg" />
    <AppBanner tone="red" :message="error" />

    <div class="mb-4 grid grid-cols-2 gap-3">
      <AppCard
        ><p class="text-xs uppercase tracking-wider text-secondary">Terbuka</p>
        <p class="font-display text-2xl font-bold">{{ terbuka }}</p></AppCard
      >
      <AppCard
        ><p class="text-xs uppercase tracking-wider text-secondary">Tertutup</p>
        <p class="font-display text-2xl font-bold">{{ tertutup }}</p></AppCard
      >
    </div>

    <AppCard v-if="boleh" class="mb-3">
      <h2 class="font-display font-semibold">Tutup bulan</h2>
      <p class="text-xs text-secondary">
        Syarat: tidak ada biaya draft & tidak ada gaji belum dibayar yang bersinggungan. Bulan yang
        sudah tutup menolak posting backdate (nota, retur, PO, gaji, hutang/piutang).
      </p>
      <div class="mt-2 flex flex-wrap items-end gap-2">
        <label class="text-sm"
          >Tahun
          <input
            v-model.number="tahun"
            type="number"
            min="2000"
            max="2100"
            class="ml-1 w-24 rounded-lg bg-surface-low px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-primary"
        /></label>
        <label class="text-sm"
          >Bulan
          <input
            v-model.number="bulan"
            type="number"
            min="1"
            max="12"
            class="ml-1 w-20 rounded-lg bg-surface-low px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-primary"
        /></label>
        <AppButton @click="tutup">Tutup periode</AppButton>
      </div>
    </AppCard>

    <p v-if="loading" class="text-sm text-secondary">Memuat…</p>
    <AppEmpty v-else-if="!list.length" message="Belum ada periode." />
    <div class="grid gap-3 md:grid-cols-2">
      <AppCard v-for="p in list" :key="p.id">
        <div class="flex items-center justify-between gap-2">
          <strong class="font-display">{{ p.label }}</strong>
          <AppBadge :tone="p.status === 'buka' ? 'green' : 'red'">{{ p.status }}</AppBadge>
        </div>
        <p class="mt-1 text-xs text-secondary">
          {{
            p.status === 'buka'
              ? 'Masih bisa posting.'
              : `Ditutup${p.closedAt ? ' · ' + tanggal(p.closedAt) : ''}.`
          }}
        </p>
        <AppButton
          v-if="p.status !== 'buka' && boleh"
          variant="ghost"
          class="mt-2"
          @click="bukaLagi(p)"
          >Buka lagi</AppButton
        >
      </AppCard>
    </div>
  </div>
</template>
