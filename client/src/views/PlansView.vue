<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { api } from '../lib/api'
import { rupiah } from '../lib/format'
import { useAuthStore } from '../stores/auth'
import { useAsync } from '../composables/useAsync'
import AppCard from '../components/ui/AppCard.vue'
import AppBanner from '../components/ui/AppBanner.vue'
import AppButton from '../components/ui/AppButton.vue'
import AppField from '../components/ui/AppField.vue'
import AppEmpty from '../components/ui/AppEmpty.vue'

interface Paket {
  id: number
  name: string
  maxBranches: number
  maxEmployees: number
  features: string[] | null
  price: string
}

const auth = useAuthStore()
const { loading, error, run } = useAsync()
const list = ref<Paket[]>([])
const okMsg = ref('')
const showForm = ref(false)
const sunting = ref<Paket | null>(null)
const form = ref({ name: '', max_branches: 1, max_employees: 5, features: '', price: 0 })
const tanyaHapus = ref<Paket | null>(null)

async function muat() {
  okMsg.value = ''
  const res = await run(() => api.get<Paket[]>('/plans', auth.token))
  if (res)
    list.value = (
      Array.isArray(res) ? res : ((res as unknown as { data: Paket[] }).data ?? [])
    ) as Paket[]
}

function bukaTambah() {
  sunting.value = null
  form.value = { name: '', max_branches: 1, max_employees: 5, features: '', price: 0 }
  showForm.value = true
}

function bukaUbah(p: Paket) {
  sunting.value = p
  form.value = {
    name: p.name,
    max_branches: p.maxBranches,
    max_employees: p.maxEmployees,
    features: (p.features ?? []).join('\n'),
    price: Number(p.price),
  }
  showForm.value = true
}

function payload() {
  return {
    name: form.value.name.trim(),
    max_branches: Number(form.value.max_branches),
    max_employees: Number(form.value.max_employees),
    features: form.value.features
      .split('\n')
      .map((s) => s.trim())
      .filter(Boolean),
    price: Number(form.value.price),
  }
}

async function simpan() {
  const res = sunting.value
    ? await run(() => api.put(`/plans/${sunting.value!.id}`, payload(), auth.token))
    : await run(() => api.post('/plans', payload(), auth.token))
  if (res || !error.value) {
    showForm.value = false
    okMsg.value = sunting.value ? 'Paket diperbarui.' : 'Paket baru dibuat.'
    muat()
  }
}

async function hapus() {
  if (!tanyaHapus.value) return
  await run(() => api.del(`/plans/${tanyaHapus.value!.id}`, auth.token))
  if (!error.value) {
    tanyaHapus.value = null
    okMsg.value = 'Paket dihapus.'
    muat()
  }
}

onMounted(muat)
</script>

<template>
  <div class="mx-auto max-w-3xl">
    <p class="text-xs font-semibold uppercase tracking-wider text-secondary">
      Platform · khusus admin layanan
    </p>
    <div class="mb-4 flex flex-wrap items-center justify-between gap-2">
      <h1 class="font-display text-xl font-semibold">Paket langganan</h1>
      <AppButton @click="bukaTambah">Buat paket</AppButton>
    </div>
    <AppBanner tone="green" :message="okMsg" />
    <AppBanner tone="red" :message="error" />

    <p v-if="loading" class="text-sm text-secondary">Memuat…</p>
    <AppEmpty
      v-else-if="!list.length"
      message="Belum ada paket. Buat paket Gratis / Dasar / Pro."
    />
    <div class="grid gap-3 md:grid-cols-2">
      <AppCard v-for="p in list" :key="p.id">
        <div class="flex items-center justify-between gap-2">
          <h2 class="font-display font-semibold">{{ p.name }}</h2>
          <strong>{{ rupiah(p.price) }}</strong>
        </div>
        <p class="mt-1 text-xs text-secondary">
          Maks {{ p.maxBranches }} cabang · {{ p.maxEmployees }} karyawan · 0 = gratis
        </p>
        <div v-if="p.features?.length" class="mt-2 flex flex-wrap gap-1.5">
          <span
            v-for="f in p.features"
            :key="f"
            class="rounded-full bg-surface-low px-2.5 py-0.5 text-xs"
            >{{ f }}</span
          >
        </div>
        <div class="mt-3 flex gap-2">
          <AppButton variant="ghost" @click="bukaUbah(p)">Ubah</AppButton>
          <AppButton variant="ghost" @click="tanyaHapus = p">Hapus</AppButton>
        </div>
      </AppCard>
    </div>

    <div
      v-if="showForm"
      class="fixed inset-0 z-20 flex items-center justify-center bg-black/40 p-4"
      @click.self="showForm = false"
    >
      <form class="w-full max-w-md rounded-xl bg-card p-5 shadow-xl" @submit.prevent="simpan">
        <h3 class="font-display font-semibold">{{ sunting ? 'Ubah paket' : 'Buat paket' }}</h3>
        <AppField label="Nama (wajib, unik)"
          ><input
            v-model="form.name"
            maxlength="255"
            class="w-full rounded-lg bg-surface-low px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-primary"
            placeholder="cth. Pro"
        /></AppField>
        <div class="grid grid-cols-2 gap-2">
          <AppField label="Maks cabang (≥1)"
            ><input
              v-model.number="form.max_branches"
              type="number"
              min="1"
              class="w-full rounded-lg bg-surface-low px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-primary"
          /></AppField>
          <AppField label="Maks karyawan (≥1)"
            ><input
              v-model.number="form.max_employees"
              type="number"
              min="1"
              class="w-full rounded-lg bg-surface-low px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-primary"
          /></AppField>
        </div>
        <AppField label="Fitur (satu baris satu fitur)">
          <textarea
            v-model="form.features"
            rows="3"
            class="w-full rounded-lg bg-surface-low px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-primary"
            placeholder="penggajian&#10;po supplier"
          />
        </AppField>
        <AppField label="Harga (Rp, 0 = gratis)"
          ><input
            v-model.number="form.price"
            type="number"
            min="0"
            class="w-full rounded-lg bg-surface-low px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-primary"
        /></AppField>
        <div class="mt-3 flex justify-end gap-2">
          <AppButton variant="ghost" type="button" @click="showForm = false">Batal</AppButton>
          <AppButton type="submit">Simpan</AppButton>
        </div>
      </form>
    </div>

    <div
      v-if="tanyaHapus"
      class="fixed inset-0 z-20 flex items-center justify-center bg-black/40 p-4"
      @click.self="tanyaHapus = null"
    >
      <div class="w-full max-w-sm rounded-xl bg-card p-5 shadow-xl">
        <h3 class="font-display font-semibold">Hapus paket {{ tanyaHapus.name }}?</h3>
        <p class="text-sm text-secondary">Gagal bila masih dipakai langganan aktif.</p>
        <div class="mt-3 flex justify-end gap-2">
          <AppButton variant="ghost" @click="tanyaHapus = null">Batal</AppButton>
          <AppButton @click="hapus">Hapus</AppButton>
        </div>
      </div>
    </div>
  </div>
</template>
