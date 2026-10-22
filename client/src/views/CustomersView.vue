<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { api, downloadFile, type PageMeta } from '../lib/api'
import { useCompany } from '../composables/useCompany'
import { useAccessStore } from '../stores/access'
import { useAsync } from '../composables/useAsync'
import AppCard from '../components/ui/AppCard.vue'
import AppBanner from '../components/ui/AppBanner.vue'
import AppButton from '../components/ui/AppButton.vue'
import AppField from '../components/ui/AppField.vue'
import AppEmpty from '../components/ui/AppEmpty.vue'
import AppPager from '../components/ui/AppPager.vue'
import AppIcon from '../components/ui/AppIcon.vue'

interface Pelanggan {
  id: number
  name: string
  phone: string | null
  address: string | null
}

const { auth, companyId } = useCompany()
const access = useAccessStore()
const { loading, error, run } = useAsync()
const list = ref<Pelanggan[]>([])
const meta = ref<PageMeta | null>(null)
const cari = ref('')
const okMsg = ref('')
const showForm = ref(false)
const sunting = ref<Pelanggan | null>(null)
const form = ref({ name: '', phone: '', address: '' })
const tanyaHapus = ref<Pelanggan | null>(null)

const bolehKelola = () => access.can('pelanggan.kelola')
const bolehEkspor = () => access.can('pelanggan.ekspor')

async function ekspor() {
  await run(() =>
    downloadFile(
      `/companies/${companyId.value}/exports/customers`,
      'pelanggan.csv',
      auth.token,
      'text/csv',
    ),
  )
  if (!error.value) okMsg.value = 'Data pelanggan diunduh.'
}

async function muat(page = 1) {
  okMsg.value = ''
  const res = await run(() =>
    api.getPage<Pelanggan>(
      `/companies/${companyId.value}/customers?per_page=15&page=${page}`,
      auth.token,
    ),
  )
  if (res) {
    const raw = res as unknown as { data: Pelanggan[]; meta?: PageMeta } | Pelanggan[]
    if (Array.isArray(raw)) list.value = raw
    else {
      list.value = raw.data
      meta.value = raw.meta ?? null
    }
  }
}

const tersaring = () => {
  const q = cari.value.trim().toLowerCase()
  if (!q) return list.value
  return list.value.filter((p) => p.name.toLowerCase().includes(q) || (p.phone ?? '').includes(q))
}

function bukaTambah() {
  sunting.value = null
  form.value = { name: '', phone: '', address: '' }
  showForm.value = true
}

function bukaUbah(p: Pelanggan) {
  sunting.value = p
  form.value = { name: p.name, phone: p.phone ?? '', address: p.address ?? '' }
  showForm.value = true
}

async function simpan() {
  const base = `/companies/${companyId.value}/customers`
  const body = {
    name: form.value.name.trim(),
    phone: form.value.phone.trim() || undefined,
    address: form.value.address.trim() || undefined,
  }
  const res = sunting.value
    ? await run(() => api.put(base + `/${sunting.value!.id}`, body, auth.token))
    : await run(() => api.post(base, body, auth.token))
  if (res) {
    showForm.value = false
    okMsg.value = sunting.value ? 'Pelanggan diperbarui.' : 'Pelanggan baru ditambahkan.'
    muat(meta.value?.current_page ?? 1)
  }
}

async function hapus() {
  if (!tanyaHapus.value) return
  await run(() =>
    api.del(`/companies/${companyId.value}/customers/${tanyaHapus.value!.id}`, auth.token),
  )
  if (!error.value) {
    tanyaHapus.value = null
    okMsg.value = 'Pelanggan dihapus.'
    muat(1)
  }
}

onMounted(async () => {
  await access.load(companyId.value, auth.token)
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
          <AppIcon name="group" :size="28" />
        </div>
        <h1 class="font-display text-xl font-semibold">Pelanggan</h1>
      </div>
      <div class="flex gap-2">
        <AppButton v-if="bolehEkspor()" variant="ghost" @click="ekspor"
          ><AppIcon name="download" :size="18" />Ekspor</AppButton
        >
        <AppButton v-if="bolehKelola()" @click="bukaTambah"
          ><AppIcon name="add" :size="18" />Tambah</AppButton
        >
      </div>
    </div>
    <AppBanner tone="green" :message="okMsg" />
    <AppBanner tone="red" :message="error" />
    <AppField label="Cari nama / telepon">
      <input
        v-model="cari"
        class="max-w-sm rounded-lg border border-outline bg-card px-3 py-2.5 text-sm focus:border-primary focus:outline-none"
        placeholder="cth. Dudi"
      />
    </AppField>
    <AppEmpty v-if="!loading && !tersaring().length" message="Belum ada pelanggan." />
    <div class="mt-3 grid gap-3 md:grid-cols-2 xl:grid-cols-3">
      <AppCard v-for="p in tersaring()" :key="p.id">
        <div class="flex items-start justify-between gap-2">
          <div class="flex items-center gap-2">
            <span
              class="flex h-9 w-9 items-center justify-center rounded-full bg-surface-low font-bold text-primary"
              >{{ p.name.charAt(0).toUpperCase() }}</span
            >
            <div>
              <p class="font-semibold">{{ p.name }}</p>
              <p class="font-mono text-xs text-secondary">{{ p.phone ?? '–' }}</p>
            </div>
          </div>
          <div v-if="bolehKelola()" class="flex gap-1">
            <AppButton variant="ghost" @click="bukaUbah(p)">Ubah</AppButton>
            <AppButton variant="ghost" @click="tanyaHapus = p"
              ><span class="text-danger">Hapus</span></AppButton
            >
          </div>
        </div>
        <p v-if="p.address" class="mt-2 text-sm text-secondary">{{ p.address }}</p>
      </AppCard>
    </div>
    <AppPager :meta="meta" @page="muat" />

    <div
      v-if="showForm"
      class="fixed inset-0 z-20 flex items-center justify-center bg-black/40 p-4"
      @click.self="showForm = false"
    >
      <form class="w-full max-w-md rounded-xl bg-card p-5 shadow-xl" @submit.prevent="simpan">
        <h3 class="font-display font-semibold">
          {{ sunting ? 'Ubah pelanggan' : 'Pelanggan baru' }}
        </h3>
        <div class="mt-3">
          <AppField label="Nama (wajib)">
            <input
              v-model="form.name"
              maxlength="255"
              class="w-full rounded-lg bg-surface-low px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-primary"
            />
          </AppField>
        </div>
        <div class="mt-3">
          <AppField label="Telepon (unik, boleh kosong)">
            <input
              v-model="form.phone"
              class="w-full rounded-lg bg-surface-low px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-primary"
            />
          </AppField>
        </div>
        <div class="mt-3">
          <AppField label="Alamat">
            <textarea
              v-model="form.address"
              rows="2"
              maxlength="500"
              class="w-full rounded-lg bg-surface-low px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-primary"
            />
          </AppField>
        </div>
        <div class="mt-4 flex justify-end gap-2">
          <AppButton variant="ghost" @click="showForm = false">Batal</AppButton>
          <AppButton type="submit" :loading="loading">Simpan</AppButton>
        </div>
      </form>
    </div>

    <div
      v-if="tanyaHapus"
      class="fixed inset-0 z-20 flex items-center justify-center bg-black/40 p-4"
      @click.self="tanyaHapus = null"
    >
      <div class="w-full max-w-sm rounded-xl bg-card p-5 shadow-xl">
        <h3 class="font-display font-semibold">Hapus {{ tanyaHapus.name }}?</h3>
        <p class="mt-1 text-sm text-secondary">
          Pelanggan yang masih punya transaksi tidak bisa dihapus.
        </p>
        <div class="mt-4 flex justify-end gap-2">
          <AppButton variant="ghost" @click="tanyaHapus = null">Batal</AppButton>
          <AppButton variant="danger" :loading="loading" @click="hapus">Ya, hapus</AppButton>
        </div>
      </div>
    </div>
  </div>
</template>
