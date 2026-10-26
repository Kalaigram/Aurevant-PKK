<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { api } from '../lib/api'
import { rupiah } from '../lib/format'
import { useAuthStore } from '../stores/auth'
import { useAsync } from '../composables/useAsync'
import AppBadge from '../components/ui/AppBadge.vue'
import AppBanner from '../components/ui/AppBanner.vue'
import AppButton from '../components/ui/AppButton.vue'
import AppCard from '../components/ui/AppCard.vue'
import AppEmpty from '../components/ui/AppEmpty.vue'
import AppField from '../components/ui/AppField.vue'
import AppInput from '../components/ui/AppInput.vue'
import AppModal from '../components/ui/AppModal.vue'
import AppPageHeader from '../components/ui/AppPageHeader.vue'
import AppSkeleton from '../components/ui/AppSkeleton.vue'
import AppTextarea from '../components/ui/AppTextarea.vue'

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
  const res = await run(() => api.get<Paket[]>('/plans', auth.token))
  if (res)
    list.value = (
      Array.isArray(res) ? res : ((res as unknown as { data: Paket[] }).data ?? [])
    ) as Paket[]
}

const priceLabel = (price: string) => (Number(price) === 0 ? 'Gratis' : rupiah(price))

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
  <div class="mx-auto max-w-4xl">
    <AppPageHeader
      eyebrow="Platform · khusus admin layanan"
      title="Paket langganan"
      subtitle="Atur harga, kuota, dan fitur yang tersedia untuk setiap tingkat layanan."
      icon="sell"
    >
      <template #meta>
        <div class="mt-2">
          <AppBadge tone="blue">{{ list.length }} paket tersedia</AppBadge>
        </div>
      </template>
      <template #actions>
        <AppButton @click="bukaTambah">Buat paket</AppButton>
      </template>
    </AppPageHeader>

    <AppBanner tone="green" :message="okMsg" />
    <AppBanner tone="red" :message="error" />

    <div v-if="loading && !list.length" class="grid gap-3 md:grid-cols-2" aria-busy="true">
      <AppCard v-for="i in 4" :key="i"><AppSkeleton :lines="5" /></AppCard>
      <span class="sr-only" role="status">Memuat paket…</span>
    </div>
    <AppEmpty
      v-else-if="!list.length"
      icon="sell"
      title="Belum ada paket"
      message="Buat paket Gratis, Dasar, atau Pro untuk mulai menawarkan langganan."
    >
      <AppButton @click="bukaTambah">Buat paket pertama</AppButton>
    </AppEmpty>
    <div v-else class="grid gap-3 md:grid-cols-2">
      <AppCard v-for="p in list" :key="p.id">
        <div class="flex items-start justify-between gap-3">
          <div>
            <h2 class="font-display text-lg font-semibold text-ink">{{ p.name }}</h2>
            <p class="mt-0.5 text-xs text-secondary">
              Maks {{ p.maxBranches }} cabang · {{ p.maxEmployees }} karyawan
            </p>
          </div>
          <p class="whitespace-nowrap font-display text-lg font-bold text-ink">
            {{ priceLabel(p.price) }}
          </p>
        </div>
        <div v-if="p.features?.length" class="mt-3 flex flex-wrap gap-1.5">
          <span
            v-for="f in p.features"
            :key="f"
            class="rounded-full bg-primary-soft px-2.5 py-0.5 text-xs font-medium text-primary-ink"
            >{{ f }}</span
          >
        </div>
        <p v-else class="mt-3 text-xs text-secondary">Belum ada fitur tambahan.</p>
        <div class="mt-4 flex gap-2 border-t border-outline/50 pt-3">
          <AppButton size="sm" variant="ghost" :aria-label="`Ubah paket ${p.name}`" @click="bukaUbah(p)">
            Ubah
          </AppButton>
          <AppButton
            size="sm"
            variant="ghost"
            class="text-danger"
            :aria-label="`Hapus paket ${p.name}`"
            @click="tanyaHapus = p"
          >
            Hapus
          </AppButton>
        </div>
      </AppCard>
    </div>

    <AppModal
      :open="showForm"
      :title="sunting ? 'Ubah paket' : 'Buat paket'"
      description="Nama paket harus unik. Harga 0 ditampilkan sebagai Gratis."
      @close="showForm = false"
    >
      <AppBanner tone="red" :message="error" />
      <form id="plan-form" class="space-y-3" @submit.prevent="simpan">
        <AppField label="Nama paket" required>
          <AppInput
            v-model="form.name"
            maxlength="255"
            placeholder="cth. Pro"
            data-autofocus
          />
        </AppField>
        <div class="grid grid-cols-2 gap-3">
          <AppField label="Maks cabang" required>
            <AppInput v-model="form.max_branches" type="number" min="1" />
          </AppField>
          <AppField label="Maks karyawan" required>
            <AppInput v-model="form.max_employees" type="number" min="1" />
          </AppField>
        </div>
        <AppField label="Fitur" optional hint="Satu baris untuk satu fitur.">
          <AppTextarea
            v-model="form.features"
            :rows="3"
            placeholder="penggajian&#10;po supplier"
          />
        </AppField>
        <AppField label="Harga (Rp)" required hint="Isi 0 untuk paket gratis.">
          <AppInput v-model="form.price" type="number" min="0" />
        </AppField>
      </form>
      <template #footer>
        <AppButton variant="ghost" @click="showForm = false">Batal</AppButton>
        <AppButton
          type="submit"
          form="plan-form"
          :loading="loading"
          :disabled="!form.name.trim() || form.max_branches < 1 || form.max_employees < 1"
        >
          Simpan paket
        </AppButton>
      </template>
    </AppModal>

    <AppModal
      :open="!!tanyaHapus"
      :title="`Hapus paket ${tanyaHapus?.name ?? ''}?`"
      description="Paket yang masih dipakai langganan aktif tidak dapat dihapus."
      size="sm"
      @close="tanyaHapus = null"
    >
      <AppBanner tone="red" :message="error" />
      <template #footer>
        <AppButton variant="ghost" @click="tanyaHapus = null">Batal</AppButton>
        <AppButton variant="danger" :loading="loading" @click="hapus">Hapus paket</AppButton>
      </template>
    </AppModal>
  </div>
</template>
