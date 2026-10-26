<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { API_BASE, api } from '../lib/api'
import { useCompany } from '../composables/useCompany'
import { useAccessStore } from '../stores/access'
import { useAsync } from '../composables/useAsync'
import AppBanner from '../components/ui/AppBanner.vue'
import AppButton from '../components/ui/AppButton.vue'
import AppCard from '../components/ui/AppCard.vue'
import AppCheckbox from '../components/ui/AppCheckbox.vue'
import AppEmpty from '../components/ui/AppEmpty.vue'
import AppField from '../components/ui/AppField.vue'
import AppIcon from '../components/ui/AppIcon.vue'
import AppPageHeader from '../components/ui/AppPageHeader.vue'

type Jenis = 'items' | 'stock' | 'suppliers' | 'employees'

const KOLOM: Record<Jenis, string> = {
  items: 'code, name, type, unit, category, branch_code, price',
  stock: 'item_code, branch_code, qty, unit_cost',
  suppliers: 'name, phone, address',
  employees: 'name, email, phone, branch_code, role, password, pt kp_status'.replace(
    'pt kp',
    'ptkp',
  ),
}
const LABEL: Record<Jenis, string> = {
  items: 'Produk & jasa',
  stock: 'Stok awal',
  suppliers: 'Pemasok',
  employees: 'Karyawan',
}
const PTKP = ['TK/0', 'TK/1', 'TK/2', 'TK/3', 'K/0', 'K/1', 'K/2', 'K/3']

const { auth, companyId } = useCompany()
const access = useAccessStore()
const { loading, error, run } = useAsync()
const jenis = ref<Jenis>('items')
const pratinjau = ref(true)
const berkas = ref<File | null>(null)
const berkasInput = ref<HTMLInputElement | null>(null)
const okMsg = ref('')
const galatBaris = ref<string[]>([])
const jumlahImpor = ref<number | null>(null)

const boleh = computed(() => access.can('impor.jalankan'))

function pilihJenis(k: string) {
  if (k === 'items' || k === 'stock' || k === 'suppliers' || k === 'employees') jenis.value = k
}

function bukaPilihBerkas() {
  if (!berkasInput.value) return
  berkasInput.value.value = ''
  berkasInput.value.click()
}

function pilihBerkas(e: Event) {
  const input = e.target as HTMLInputElement
  berkas.value = input.files?.[0] ?? null
  galatBaris.value = []
  jumlahImpor.value = null
}

async function unduhTemplate() {
  okMsg.value = ''
  const res = await fetch(
    `${API_BASE}/companies/${companyId.value}/imports/template/${jenis.value}`,
    {
      headers: {
        Accept: 'text/csv',
        ...(auth.token ? { Authorization: `Bearer ${auth.token}` } : {}),
      },
    },
  )
  if (!res.ok) {
    okMsg.value = ''
    return
  }
  const blob = await res.blob()
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `template-${jenis.value}.csv`
  a.click()
  URL.revokeObjectURL(url)
  okMsg.value = 'Template diunduh. Isi sesuai kolom lalu unggah kembali.'
}

async function unduhEkspor() {
  const res = await fetch(`${API_BASE}/companies/${companyId.value}/exports/items`, {
    headers: {
      Accept: 'text/csv',
      ...(auth.token ? { Authorization: `Bearer ${auth.token}` } : {}),
    },
  })
  if (!res.ok) return
  const blob = await res.blob()
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = 'ekspor-item.csv'
  a.click()
  URL.revokeObjectURL(url)
  okMsg.value = 'Ekspor item diunduh.'
}

async function unggah() {
  if (!berkas.value) return
  okMsg.value = ''
  galatBaris.value = []
  jumlahImpor.value = null
  const form = new FormData()
  form.append('file', berkas.value)
  if (pratinjau.value) form.append('dry_run', '1')
  const res = await run(() =>
    api.postForm<{ imported: number; dry_run: boolean }>(
      `/companies/${companyId.value}/imports/${jenis.value}`,
      form,
      auth.token,
    ),
  )
  if (res) {
    const raw = res as unknown as { imported: number; dry_run: boolean }
    jumlahImpor.value = raw.imported
    okMsg.value = pratinjau.value
      ? `Pratinjau valid: ${raw.imported} baris siap diimpor. Matikan mode pratinjau lalu unggah ulang untuk menyimpan.`
      : `Impor selesai: ${raw.imported} baris tersimpan.`
  } else if (error.value) {
    galatBaris.value = [error.value]
  }
}

onMounted(async () => {
  await access.load(companyId.value, auth.token)
})
</script>

<template>
  <div>
    <AppPageHeader
      eyebrow="Data massal"
      title="Impor & ekspor data"
      subtitle="Siapkan data dalam CSV, pratinjau lebih dulu, lalu simpan setelah hasilnya sesuai."
      icon="upload_file"
    >
      <template #actions>
        <AppButton variant="ghost" @click="unduhEkspor">
          <AppIcon name="download" :size="18" />Ekspor item (CSV)
        </AppButton>
      </template>
    </AppPageHeader>

    <AppBanner tone="green" :message="okMsg" />
    <AppBanner tone="red" :message="error" />

    <AppEmpty
      v-if="!boleh"
      icon="lock"
      title="Akses impor diperlukan"
      message="Anda tidak punya wewenang impor data. Minta pemilik perusahaan untuk memberi akses."
    />
    <div v-else class="grid gap-4 lg:grid-cols-2">
      <AppCard>
        <h2 class="font-display font-semibold">1. Unduh template</h2>
        <p class="text-xs text-secondary">
          Pilih jenis data, unduh templatenya, lalu isi di spreadsheet.
        </p>
        <div class="mt-3 grid grid-cols-2 gap-2">
          <button
            v-for="(label, key) in LABEL"
            :key="key"
            type="button"
            class="rounded-xl border px-3 py-2.5 text-left text-sm transition focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/55"
            :class="
              jenis === key
                ? 'border-primary bg-primary-soft font-semibold text-primary-ink'
                : 'border-outline bg-card text-ink hover:border-outline-strong hover:bg-surface-low'
            "
            :aria-pressed="jenis === key"
            @click="pilihJenis(key)"
          >
            {{ label }}
          </button>
        </div>
        <AppField label="Kolom template" class="mt-3">
          <p class="rounded-lg bg-surface-low px-3 py-2 font-mono text-xs">{{ KOLOM[jenis] }}</p>
        </AppField>
        <p v-if="jenis === 'employees'" class="mt-2 text-xs text-secondary">
          Status pajak (PTKP) yang boleh dipakai: {{ PTKP.join(', ') }}. Kosong = TK/0. Kata sandi
          kosong = dibuat acak oleh sistem.
        </p>
        <AppButton class="mt-3" @click="unduhTemplate"
          ><AppIcon name="download" :size="18" />Unduh template {{ LABEL[jenis] }}</AppButton
        >
      </AppCard>

      <AppCard>
        <h2 class="font-display font-semibold">2. Unggah & pratinjau</h2>
        <p class="text-xs text-secondary">
          Selalu pratinjau dulu sebelum menyimpan. Maksimal 2 MB, format CSV.
        </p>
        <AppField
          label="Berkas CSV"
          required
          hint="Maksimal 2 MB. Gunakan pemisah koma sesuai template."
          class="mt-3"
        >
          <input
            ref="berkasInput"
            type="file"
            accept=".csv,text/csv"
            class="hidden"
            @change="pilihBerkas"
          />
          <div class="flex flex-wrap items-center gap-2 rounded-xl border border-dashed border-outline-strong/70 bg-surface-low/50 p-3">
            <AppButton size="sm" variant="secondary" @click="bukaPilihBerkas">
              <AppIcon name="attach_file" :size="16" />Pilih berkas
            </AppButton>
            <p class="min-w-0 flex-1 truncate text-sm text-secondary">
              {{ berkas?.name ?? 'Belum ada berkas dipilih' }}
            </p>
          </div>
        </AppField>
        <div class="mt-3">
          <AppCheckbox
            v-model="pratinjau"
            label="Mode pratinjau dulu"
            hint="Disarankan agar kesalahan baris terlihat sebelum data disimpan."
          />
        </div>
        <AppButton class="mt-3" :disabled="!berkas || loading" :loading="loading" @click="unggah">
          <AppIcon name="upload" :size="18" />{{
            pratinjau ? 'Pratinjau berkas' : 'Simpan hasil impor'
          }}
        </AppButton>
        <p v-if="jumlahImpor !== null" class="mt-3 rounded-lg bg-ok-bg px-3 py-2 text-sm font-semibold text-ok-ink">
          {{ jumlahImpor }} baris diproses.
        </p>
        <ul
          v-if="galatBaris.length"
          class="mt-3 space-y-1 rounded-xl border border-danger/20 bg-danger-bg/60 p-3 text-xs text-danger"
        >
          <li v-for="(g, i) in galatBaris" :key="i">{{ g }}</li>
        </ul>
      </AppCard>
    </div>
  </div>
</template>
