<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { API_BASE, api } from '../lib/api'
import { useCompany } from '../composables/useCompany'
import { useAccessStore } from '../stores/access'
import { useAsync } from '../composables/useAsync'
import AppCard from '../components/ui/AppCard.vue'
import AppBanner from '../components/ui/AppBanner.vue'
import AppButton from '../components/ui/AppButton.vue'
import AppField from '../components/ui/AppField.vue'
import AppEmpty from '../components/ui/AppEmpty.vue'
import AppIcon from '../components/ui/AppIcon.vue'

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
const okMsg = ref('')
const galatBaris = ref<string[]>([])
const jumlahImpor = ref<number | null>(null)

const boleh = computed(() => access.can('impor.jalankan'))

function pilihJenis(k: string) {
  if (k === 'items' || k === 'stock' || k === 'suppliers' || k === 'employees') jenis.value = k
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
    <p class="text-xs font-semibold uppercase tracking-wider text-secondary">Data massal</p>
    <div class="mb-4 flex flex-wrap items-center justify-between gap-2">
      <h1 class="font-display text-xl font-semibold">Impor & ekspor data</h1>
      <AppButton variant="ghost" @click="unduhEkspor"
        ><AppIcon name="download" :size="18" />Ekspor item (CSV)</AppButton
      >
    </div>
    <AppBanner tone="green" :message="okMsg" />
    <AppBanner tone="red" :message="error" />

    <AppEmpty
      v-if="!boleh"
      message="Anda tidak punya wewenang impor data. Minta pemilik perusahaan untuk memberi akses."
    />
    <div v-else class="grid gap-3 lg:grid-cols-2">
      <AppCard>
        <h2 class="font-display font-semibold">1. Unduh template</h2>
        <p class="text-xs text-secondary">
          Pilih jenis data, unduh templatenya, lalu isi di spreadsheet.
        </p>
        <div class="mt-3 grid grid-cols-2 gap-2">
          <button
            v-for="(label, key) in LABEL"
            :key="key"
            class="rounded-lg border px-3 py-2 text-left text-sm"
            :class="
              jenis === key
                ? 'border-primary bg-primary-container/10 font-semibold'
                : 'border-outline'
            "
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
        <AppField label="Berkas CSV (wajib)" class="mt-3">
          <input type="file" accept=".csv,text/csv" class="w-full text-sm" @change="pilihBerkas" />
        </AppField>
        <label class="mt-2 flex items-center gap-2 text-sm">
          <input v-model="pratinjau" type="checkbox" class="h-4 w-4 accent-emerald-700" />
          Mode pratinjau dulu (disarankan)
        </label>
        <AppButton class="mt-3" :disabled="!berkas || loading" @click="unggah">
          <AppIcon name="upload" :size="18" />{{
            pratinjau ? 'Pratinjau berkas' : 'Simpan hasil impor'
          }}
        </AppButton>
        <p v-if="loading" class="mt-2 text-sm text-secondary">Memproses…</p>
        <p v-if="jumlahImpor !== null" class="mt-2 text-sm font-semibold text-primary">
          {{ jumlahImpor }} baris.
        </p>
        <ul v-if="galatBaris.length" class="mt-2 space-y-1 rounded-lg bg-surface-low p-3 text-xs">
          <li v-for="(g, i) in galatBaris" :key="i">{{ g }}</li>
        </ul>
      </AppCard>
    </div>
  </div>
</template>
