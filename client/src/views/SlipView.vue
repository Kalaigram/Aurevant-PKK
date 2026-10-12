<script setup lang="ts">
import { ref } from 'vue'
import { api } from '../lib/api'
import { rupiah, tanggal } from '../lib/format'
import { useCompany } from '../composables/useCompany'
import { useAsync } from '../composables/useAsync'
import AppCard from '../components/ui/AppCard.vue'
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

async function lihat() {
  if (!periodId.value || !auth.user) return
  slip.value = await run(() =>
    api.get<Slip>(`/companies/${companyId.value}/payroll-periods/${periodId.value}/users/${auth.user!.id}/slip`, auth.token),
  )
}

function cetak() {
  window.print()
}
</script>

<template>
  <div class="mx-auto max-w-2xl">
    <div class="mb-4 flex items-center gap-3">
      <div class="flex h-12 w-12 items-center justify-center rounded-xl bg-primary-container text-on-primary">
        <AppIcon name="receipt_long" :size="28" />
      </div>
      <h1 class="font-display text-xl font-semibold">Slip gaji saya</h1>
    </div>
    <AppBanner tone="red" :message="error" />
    <AppCard title="Pilih periode">
      <AppField label="ID periode (tanya HR bila belum tahu)">
        <input v-model="periodId" type="number" class="w-full rounded-lg border border-outline bg-card px-3 py-2.5 focus:border-primary focus:outline-none" placeholder="cth. 3" />
      </AppField>
      <AppButton :loading="loading" class="mt-3" @click="lihat">Lihat slip</AppButton>
    </AppCard>

    <AppEmpty v-if="!slip && !loading" message="Masukkan ID periode untuk melihat slip." />
    <AppCard v-if="slip" title="Slip gaji">
      <div class="mb-3 flex items-center justify-between">
        <div>
          <p class="font-semibold">{{ slip.period.name }}</p>
          <p class="text-sm text-secondary">{{ tanggal(slip.period.from) }} – {{ tanggal(slip.period.to) }} · status {{ slip.period.status }}</p>
        </div>
        <AppButton variant="ghost" class="no-print" @click="cetak">Cetak</AppButton>
      </div>
      <table class="w-full text-sm">
        <tbody>
          <tr v-for="(l, i) in slip.lines" :key="i" class="border-b border-surface-low last:border-0">
            <td class="py-2">{{ l.label }}</td>
            <td class="py-2 text-right font-medium" :class="Number(l.amount) < 0 ? 'text-danger' : ''">{{ rupiah(l.amount) }}</td>
          </tr>
          <tr>
            <td class="py-2 font-extrabold">Gaji bersih</td>
            <td class="py-2 text-right font-extrabold">{{ rupiah(slip.net) }}</td>
          </tr>
        </tbody>
      </table>
    </AppCard>
  </div>
</template>
