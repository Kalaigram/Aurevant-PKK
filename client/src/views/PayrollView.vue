<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { api } from '../lib/api'
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

interface Komponen {
  id: number
  name: string
  calcType: string
  deductible: boolean
  taxable?: boolean
  bpjs_base?: boolean
  bpjsBase?: boolean
  active?: boolean
}
interface Periode {
  id: number
  name: string
  from?: string
  to?: string
  divisor?: number
  status: string
  [k: string]: unknown
}

const JENIS: Record<string, string> = {
  monthly_fixed: 'Tetap bulanan (potong alpa 1/pembagi per hari)',
  daily_rate: 'Harian (nominal × hari hadir)',
  commission: 'Komisi jasa periode itu',
  manual: 'Input nominal manual',
}

const { auth, companyId } = useCompany()
const access = useAccessStore()
const { loading, error, run } = useAsync()
const tab = ref<'komponen' | 'periode'>('komponen')
const komponen = ref<Komponen[]>([])
const periode = ref<Periode[]>([])
const okMsg = ref('')

const bolehKelola = computed(() => access.can('gaji.kelola'))
const bolehSetuju = computed(() => access.can('gaji.setujui'))

const showKom = ref(false)
const sunting = ref<Komponen | null>(null)
const fKom = ref({
  name: '',
  calc_type: 'monthly_fixed',
  deductible: true,
  taxable: false,
  bpjs_base: false,
  active: true,
})

const idTaut = ref('')
const idKomTaut = ref('')
const nominalTaut = ref(0)

const fPer = ref({ name: '', date_from: '', date_to: '', divisor: 30 })
const idPeriode = ref<number | null>(null)
const detail = ref<Record<string, unknown> | null>(null)
const hasilHitung = ref<{ user_id: number; net: number | string }[]>([])

const idSesuai = ref('')
const labelSesuai = ref('')
const nominalSesuai = ref(0)

const tonePeriode = (s: string) =>
  s === 'dibayar' || s === 'disetujui' ? 'green' : s === 'review' ? 'blue' : 'gray'

async function muatKomponen() {
  const res = await run(() =>
    api.get<Komponen[]>(`/companies/${companyId.value}/salary-components`, auth.token),
  )
  if (res)
    komponen.value = (
      Array.isArray(res) ? res : ((res as unknown as { data: Komponen[] }).data ?? [])
    ) as Komponen[]
}

async function muatPeriode() {
  const res = await run(() =>
    api.get<Periode[]>(`/companies/${companyId.value}/payroll-periods`, auth.token),
  )
  if (res) {
    const raw = res as unknown as { data: Periode[] } | Periode[]
    periode.value = Array.isArray(raw) ? raw : (raw.data ?? [])
    if (!idPeriode.value && periode.value.length) idPeriode.value = periode.value[0]!.id
  }
}

function bukaKomBaru() {
  sunting.value = null
  fKom.value = {
    name: '',
    calc_type: 'monthly_fixed',
    deductible: true,
    taxable: false,
    bpjs_base: false,
    active: true,
  }
  showKom.value = true
}

function bukaKomUbah(k: Komponen) {
  sunting.value = k
  fKom.value = {
    name: k.name,
    calc_type: k.calcType,
    deductible: k.deductible,
    taxable: k.taxable ?? false,
    bpjs_base: k.bpjs_base ?? k.bpjsBase ?? false,
    active: k.active ?? true,
  }
  showKom.value = true
}

async function simpanKomponen() {
  const payload = {
    name: fKom.value.name.trim(),
    calc_type: fKom.value.calc_type,
    deductible: fKom.value.deductible,
    taxable: fKom.value.taxable,
    bpjs_base: fKom.value.bpjs_base,
    active: fKom.value.active,
  }
  const base = `/companies/${companyId.value}/salary-components`
  const res = sunting.value
    ? await run(() => api.put(base + `/${sunting.value!.id}`, payload, auth.token))
    : await run(() => api.post(base, payload, auth.token))
  if (res || !error.value) {
    showKom.value = false
    okMsg.value = 'Komponen tersimpan.'
    muatKomponen()
  }
}

async function tautkan() {
  if (!idTaut.value || !idKomTaut.value) return
  const res = await run(() =>
    api.post(
      `/companies/${companyId.value}/users/${idTaut.value}/salary-components`,
      {
        component_id: Number(idKomTaut.value),
        amount: Number(nominalTaut.value),
      },
      auth.token,
    ),
  )
  if (res || !error.value) {
    okMsg.value = 'Komponen ditetapkan ke karyawan.'
    idTaut.value = ''
  }
}

async function buatPeriode() {
  const res = await run(() =>
    api.post(
      `/companies/${companyId.value}/payroll-periods`,
      {
        name: fPer.value.name.trim(),
        date_from: fPer.value.date_from,
        date_to: fPer.value.date_to,
        divisor: Number(fPer.value.divisor),
      },
      auth.token,
    ),
  )
  if (res) {
    okMsg.value = 'Periode gaji dibuat.'
    fPer.value = { name: '', date_from: '', date_to: '', divisor: 30 }
    muatPeriode()
  }
}

async function muatDetail() {
  if (!idPeriode.value) return
  hasilHitung.value = []
  const res = await run(() =>
    api.get<Record<string, unknown>>(
      `/companies/${companyId.value}/payroll-periods/${idPeriode.value}`,
      auth.token,
    ),
  )
  if (res) detail.value = res as Record<string, unknown>
}

async function hitung() {
  if (!idPeriode.value) return
  const res = await run(() =>
    api.post<{ employees: { user_id: number; net: number | string }[] }>(
      `/companies/${companyId.value}/payroll-periods/${idPeriode.value}/calculate`,
      {},
      auth.token,
    ),
  )
  if (res) {
    const raw = res as unknown as { employees?: { user_id: number; net: number | string }[] }
    hasilHitung.value = raw.employees ?? []
    okMsg.value = `Perhitungan selesai: ${hasilHitung.value.length} karyawan.`
    muatDetail()
  }
}

async function tambahSesuai() {
  if (!idPeriode.value || !idSesuai.value || !labelSesuai.value.trim()) return
  const res = await run(() =>
    api.post(
      `/companies/${companyId.value}/payroll-periods/${idPeriode.value}/users/${idSesuai.value}/adjustments`,
      {
        label: labelSesuai.value.trim(),
        amount: Number(nominalSesuai.value),
      },
      auth.token,
    ),
  )
  if (res || !error.value) {
    okMsg.value = 'Penyesuaian tersimpan (negatif = potongan).'
    labelSesuai.value = ''
    nominalSesuai.value = 0
    muatDetail()
  }
}

async function alur(aksi: 'review' | 'approve' | 'pay') {
  if (!idPeriode.value) return
  const res = await run(() =>
    api.post(
      `/companies/${companyId.value}/payroll-periods/${idPeriode.value}/${aksi}`,
      {},
      auth.token,
    ),
  )
  if (res || !error.value) {
    okMsg.value =
      aksi === 'review'
        ? 'Periode dikirim ke review.'
        : aksi === 'approve'
          ? 'Periode disetujui dan dikunci.'
          : 'Periode dibayar dan dijurnal.'
    muatPeriode()
    muatDetail()
  }
}

onMounted(async () => {
  await access.load(companyId.value, auth.token)
  await muatKomponen()
  await muatPeriode()
  await muatDetail()
})
</script>

<template>
  <div>
    <p class="text-xs font-semibold uppercase tracking-wider text-secondary">
      Gaji berbasis kehadiran
    </p>
    <h1 class="font-display mb-4 text-xl font-semibold">Penggajian</h1>
    <AppBanner tone="green" :message="okMsg" />
    <AppBanner tone="red" :message="error" />

    <AppEmpty v-if="!bolehKelola" message="Halaman ini hanya untuk pengelola gaji." />
    <template v-else>
      <div class="mb-3 flex gap-2">
        <AppButton :variant="tab === 'komponen' ? 'primary' : 'ghost'" @click="tab = 'komponen'"
          >Komponen</AppButton
        >
        <AppButton :variant="tab === 'periode' ? 'primary' : 'ghost'" @click="tab = 'periode'"
          >Periode</AppButton
        >
      </div>

      <div v-if="tab === 'komponen'">
        <div class="mb-3 flex items-center justify-between">
          <p class="text-sm text-secondary">
            {{ komponen.length }} komponen. Tidak ada tombol hapus di API.
          </p>
          <AppButton @click="bukaKomBaru">Tambah komponen</AppButton>
        </div>
        <div class="grid gap-3 md:grid-cols-2">
          <AppCard v-for="k in komponen" :key="k.id">
            <div class="flex items-center justify-between gap-2">
              <h2 class="font-display font-semibold">{{ k.name }}</h2>
              <AppButton variant="ghost" @click="bukaKomUbah(k)">Ubah</AppButton>
            </div>
            <p class="text-xs text-secondary">{{ JENIS[k.calcType] ?? k.calcType }}</p>
            <p class="mt-1 text-xs">
              {{ k.deductible ? 'Terpotong alpa' : 'Tidak terpotong alpa' }}
              <span v-if="k.taxable"> · Kena pajak</span>
              <span v-if="k.bpjs_base ?? k.bpjsBase"> · Basis BPJS</span>
              <span v-if="k.active === false"> · Nonaktif</span>
            </p>
          </AppCard>
        </div>
        <AppCard class="mt-3">
          <h2 class="font-display font-semibold">Tetapkan ke karyawan</h2>
          <div class="mt-2 grid gap-2 md:grid-cols-4">
            <AppField label="ID karyawan"
              ><input
                v-model="idTaut"
                inputmode="numeric"
                class="w-full rounded-lg bg-surface-low px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-primary"
                placeholder="cth. 12"
            /></AppField>
            <AppField label="Komponen"
              ><select
                v-model="idKomTaut"
                class="w-full rounded-lg bg-surface-low px-3 py-2 text-sm"
              >
                <option value="">— pilih —</option>
                <option v-for="k in komponen" :key="k.id" :value="k.id">{{ k.name }}</option>
              </select></AppField
            >
            <AppField label="Nominal (Rp)"
              ><input
                v-model.number="nominalTaut"
                type="number"
                min="0"
                class="w-full rounded-lg bg-surface-low px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-primary"
            /></AppField>
            <div class="flex items-end"><AppButton @click="tautkan">Tetapkan</AppButton></div>
          </div>
        </AppCard>
      </div>

      <div v-if="tab === 'periode'">
        <AppCard>
          <h2 class="font-display font-semibold">Buat periode</h2>
          <div class="mt-2 grid gap-2 md:grid-cols-4">
            <AppField label="Nama periode"
              ><input
                v-model="fPer.name"
                maxlength="255"
                placeholder="cth. Gaji Oktober"
                class="w-full rounded-lg bg-surface-low px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-primary"
            /></AppField>
            <AppField label="Dari"
              ><input
                v-model="fPer.date_from"
                type="date"
                class="w-full rounded-lg bg-surface-low px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-primary"
            /></AppField>
            <AppField label="Sampai"
              ><input
                v-model="fPer.date_to"
                type="date"
                class="w-full rounded-lg bg-surface-low px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-primary"
            /></AppField>
            <AppField label="Pembagi (1–31)"
              ><input
                v-model.number="fPer.divisor"
                type="number"
                min="1"
                max="31"
                class="w-full rounded-lg bg-surface-low px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-primary"
            /></AppField>
          </div>
          <AppButton class="mt-2" @click="buatPeriode">Buat periode</AppButton>
        </AppCard>

        <div class="mt-3 flex flex-wrap items-center gap-2">
          <label class="flex items-center gap-1.5 text-sm text-secondary"
            >Periode
            <select
              v-model="idPeriode"
              class="rounded-lg border border-outline bg-card px-2 py-2 text-sm"
              @change="muatDetail"
            >
              <option v-for="p in periode" :key="p.id" :value="p.id">
                {{ p.name }} ({{ p.status }})
              </option>
            </select>
          </label>
          <AppBadge
            v-if="idPeriode"
            :tone="tonePeriode(periode.find((p) => p.id === idPeriode)?.status ?? '')"
            >{{ periode.find((p) => p.id === idPeriode)?.status }}</AppBadge
          >
          <AppButton
            variant="ghost"
            @click="
              () => {
                muatPeriode()
                muatDetail()
              }
            "
            >Muat ulang</AppButton
          >
        </div>

        <div v-if="idPeriode" class="mt-3 flex flex-wrap gap-2">
          <AppButton @click="hitung">Hitung gaji</AppButton>
          <AppButton variant="ghost" @click="() => alur('review')">Kirim review</AppButton>
          <AppButton v-if="bolehSetuju" variant="ghost" @click="() => alur('approve')"
            >Setujui & kunci</AppButton
          >
          <AppButton variant="ghost" @click="() => alur('pay')">Tandai dibayar</AppButton>
        </div>
        <p v-if="!bolehSetuju" class="mt-1 text-xs text-secondary">
          Tombol setujui hanya untuk yang diberi wewenang menyetujui gaji.
        </p>

        <div
          v-if="hasilHitung.length"
          class="mt-3 overflow-x-auto rounded-xl border border-outline bg-card"
        >
          <table class="w-full min-w-[420px] text-sm">
            <thead>
              <tr
                class="border-b border-outline text-left text-xs uppercase tracking-wider text-secondary"
              >
                <th class="px-4 py-2">ID karyawan</th>
                <th class="px-4 py-2 text-right">Bersih</th>
                <th class="px-4 py-2 text-right">Slip</th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="h in hasilHitung"
                :key="h.user_id"
                class="border-b border-outline/60 last:border-0"
              >
                <td class="px-4 py-2 font-mono">#{{ h.user_id }}</td>
                <td class="px-4 py-2 text-right font-semibold">{{ rupiah(h.net) }}</td>
                <td class="px-4 py-2 text-right">
                  <RouterLink
                    :to="`/c/${$route.params.companyId}/slip?period=${idPeriode}&user=${h.user_id}`"
                    class="text-primary underline"
                    >Lihat</RouterLink
                  >
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <AppCard class="mt-3">
          <h2 class="font-display font-semibold">Penyesuaian manual</h2>
          <p class="text-xs text-secondary">Nominal boleh negatif untuk potongan (cth. -50000).</p>
          <div class="mt-2 grid gap-2 md:grid-cols-3">
            <AppField label="ID karyawan"
              ><input
                v-model="idSesuai"
                inputmode="numeric"
                class="w-full rounded-lg bg-surface-low px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-primary"
            /></AppField>
            <AppField label="Keterangan"
              ><input
                v-model="labelSesuai"
                maxlength="255"
                placeholder="cth. Lembur malam"
                class="w-full rounded-lg bg-surface-low px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-primary"
            /></AppField>
            <AppField label="Nominal (Rp, bisa minus)"
              ><input
                v-model.number="nominalSesuai"
                type="number"
                class="w-full rounded-lg bg-surface-low px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-primary"
            /></AppField>
          </div>
          <AppButton class="mt-2" variant="ghost" @click="tambahSesuai"
            >Simpan penyesuaian</AppButton
          >
        </AppCard>

        <p v-if="loading" class="mt-2 text-sm text-secondary">Memuat…</p>
      </div>

      <div
        v-if="showKom"
        class="fixed inset-0 z-20 flex items-center justify-center bg-black/40 p-4"
        @click.self="showKom = false"
      >
        <form
          class="w-full max-w-md rounded-xl bg-card p-5 shadow-xl"
          @submit.prevent="simpanKomponen"
        >
          <h3 class="font-display font-semibold">
            {{ sunting ? 'Ubah komponen' : 'Tambah komponen' }}
          </h3>
          <AppField label="Nama (wajib)"
            ><input
              v-model="fKom.name"
              maxlength="255"
              class="w-full rounded-lg bg-surface-low px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-primary"
              placeholder="cth. Tunjangan jabatan"
          /></AppField>
          <AppField label="Cara hitung"
            ><select
              v-model="fKom.calc_type"
              class="w-full rounded-lg bg-surface-low px-3 py-2 text-sm"
            >
              <option v-for="(label, key) in JENIS" :key="key" :value="key">{{ label }}</option>
            </select></AppField
          >
          <div class="mt-2 grid grid-cols-2 gap-2 text-sm">
            <label class="flex items-center gap-2"
              ><input
                v-model="fKom.deductible"
                type="checkbox"
                class="h-4 w-4 accent-emerald-700"
              />Terpotong alpa</label
            >
            <label class="flex items-center gap-2"
              ><input
                v-model="fKom.taxable"
                type="checkbox"
                class="h-4 w-4 accent-emerald-700"
              />Kena pajak</label
            >
            <label class="flex items-center gap-2"
              ><input
                v-model="fKom.bpjs_base"
                type="checkbox"
                class="h-4 w-4 accent-emerald-700"
              />Basis BPJS</label
            >
            <label class="flex items-center gap-2"
              ><input
                v-model="fKom.active"
                type="checkbox"
                class="h-4 w-4 accent-emerald-700"
              />Aktif</label
            >
          </div>
          <div class="mt-3 flex justify-end gap-2">
            <AppButton variant="ghost" type="button" @click="showKom = false">Batal</AppButton>
            <AppButton type="submit">Simpan</AppButton>
          </div>
        </form>
      </div>
    </template>
  </div>
</template>
