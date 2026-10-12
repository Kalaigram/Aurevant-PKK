<script setup lang="ts">
import { computed, ref } from 'vue'
import { api } from '../lib/api'
import { rupiah, tanggal, terbilang } from '../lib/format'
import { useCompany } from '../composables/useCompany'
import { useAsync } from '../composables/useAsync'
import AppCard from '../components/ui/AppCard.vue'
import AppBadge from '../components/ui/AppBadge.vue'
import AppBanner from '../components/ui/AppBanner.vue'
import AppButton from '../components/ui/AppButton.vue'
import AppField from '../components/ui/AppField.vue'
import AppEmpty from '../components/ui/AppEmpty.vue'
import AppIcon from '../components/ui/AppIcon.vue'

interface SlipLine { label: string; amount: string; isManual: boolean }
interface Slip {
  period: { id: number; name: string; from: string; to: string; divisor: number; status: string }
  lines: SlipLine[]
  net: number
}

const { auth, companyId } = useCompany()
const { loading, error, run } = useAsync()
const periodId = ref('')
const slip = ref<Slip | null>(null)
const nama = ref('')
const uuid = ref('')
const jabatan = ref<string | null>(null)

const terima = computed(() => (slip.value?.lines ?? []).filter((l) => Number(l.amount) >= 0))
const potong = computed(() => (slip.value?.lines ?? []).filter((l) => Number(l.amount) < 0))
const bruto = computed(() => terima.value.reduce((s, l) => s + Number(l.amount), 0))
const totalPotong = computed(() => potong.value.reduce((s, l) => s + Number(l.amount), 0))

async function lihat() {
  if (!periodId.value || !auth.user) return
  const found = await run(() =>
    api.get<Slip>(`/companies/${companyId.value}/payroll-periods/${periodId.value}/users/${auth.user!.id}/slip`, auth.token),
  )
  slip.value = found
  if (found) {
    const card = await run(() =>
      api.get<{ name: string; employeeUuid: string; company: string; role: string | null }>(
        `/companies/${companyId.value}/users/${auth.user!.id}/card`, auth.token,
      ),
    )
    if (card) {
      nama.value = card.name
      uuid.value = card.employeeUuid
      jabatan.value = card.role
    }
  }
}

function cetak() {
  window.print()
}
</script>

<template>
  <div class="mx-auto max-w-2xl">
    <p class="text-xs font-semibold uppercase tracking-wider text-secondary">Kepegawaian & finansial</p>
    <h1 class="font-display mb-4 text-xl font-semibold">Slip gaji karyawan</h1>
    <AppBanner tone="red" :message="error" />
    <AppCard title="Pilih periode">
      <AppField label="ID periode (tanya HR bila belum tahu)">
        <input v-model="periodId" type="number" class="w-full rounded-lg border border-outline bg-card px-3 py-2.5 focus:border-primary focus:outline-none" placeholder="cth. 3" />
      </AppField>
      <AppButton :loading="loading" class="mt-3" @click="lihat">Lihat slip</AppButton>
    </AppCard>

    <AppEmpty v-if="!slip && !loading" message="Masukkan ID periode untuk melihat slip." />
    <template v-if="slip">
      <div class="rounded-xl bg-card p-5 shadow-sm">
        <div class="flex flex-wrap items-start justify-between gap-2">
          <div>
            <p class="font-display font-bold">{{ auth.companyName }}</p>
            <p class="mt-1 inline-block rounded bg-primary/10 px-2 py-0.5 text-xs font-semibold uppercase tracking-wider text-primary">Bukti pembayaran gaji</p>
          </div>
          <div class="flex gap-2">
            <AppBadge :tone="slip.period.status === 'dibayar' ? 'green' : 'amber'">{{ slip.period.status }}</AppBadge>
            <AppButton variant="ghost" class="no-print" @click="cetak"><AppIcon name="print" :size="18" />Cetak</AppButton>
          </div>
        </div>

        <div class="mt-4 grid grid-cols-2 gap-3 text-sm md:grid-cols-3">
          <div><p class="text-xs uppercase tracking-wider text-secondary">Nama</p><p class="font-semibold">{{ nama }}</p></div>
          <div><p class="text-xs uppercase tracking-wider text-secondary">ID karyawan</p><p class="font-mono font-semibold">{{ uuid }}</p></div>
          <div><p class="text-xs uppercase tracking-wider text-secondary">Jabatan</p><p class="font-semibold">{{ jabatan ?? '–' }}</p></div>
          <div><p class="text-xs uppercase tracking-wider text-secondary">Periode</p><p class="font-semibold">{{ slip.period.name }}</p></div>
          <div><p class="text-xs uppercase tracking-wider text-secondary">Rentang</p><p class="font-semibold">{{ tanggal(slip.period.from) }} – {{ tanggal(slip.period.to) }}</p></div>
          <div><p class="text-xs uppercase tracking-wider text-secondary">Pembagi prorata</p><p class="font-semibold">1/{{ slip.period.divisor }}</p></div>
        </div>

        <h3 class="font-display mt-5 font-semibold">Penghasilan</h3>
        <table class="w-full text-sm">
          <tbody>
            <tr v-for="(l, i) in terima" :key="i" class="border-b border-surface-low last:border-0">
              <td class="py-2">{{ l.label }}</td>
              <td class="py-2 text-right font-medium">{{ rupiah(l.amount) }}</td>
            </tr>
            <tr>
              <td class="py-2 font-bold">Total bruto</td>
              <td class="py-2 text-right font-bold text-primary">{{ rupiah(bruto) }}</td>
            </tr>
          </tbody>
        </table>

        <h3 class="font-display mt-4 font-semibold">Potongan</h3>
        <table class="w-full text-sm">
          <tbody>
            <tr v-if="!potong.length"><td class="py-2 text-secondary">Tidak ada potongan periode ini.</td></tr>
            <tr v-for="(l, i) in potong" :key="i" class="border-b border-surface-low last:border-0">
              <td class="py-2">{{ l.label }}</td>
              <td class="py-2 text-right font-medium text-danger">{{ rupiah(l.amount) }}</td>
            </tr>
            <tr v-if="potong.length">
              <td class="py-2 font-bold">Total potongan</td>
              <td class="py-2 text-right font-bold">{{ rupiah(totalPotong) }}</td>
            </tr>
          </tbody>
        </table>

        <div class="mt-4 rounded-xl bg-primary p-4 text-white">
          <p class="text-xs uppercase tracking-wider text-emerald-50/80">Gaji bersih diterima</p>
          <p class="font-display text-2xl font-bold">{{ rupiah(slip.net) }}</p>
          <p class="mt-1 text-sm italic text-emerald-50/90">Terbilang: {{ terbilang(slip.net) }}</p>
        </div>
        <p class="mt-3 text-xs text-secondary">Slip ini rahasia dan hanya untuk Anda. Potongan alpa dihitung 1/{{ slip.period.divisor }} per hari.</p>
      </div>
    </template>
  </div>
</template>
