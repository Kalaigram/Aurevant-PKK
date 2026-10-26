<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue'
import { api, assetUrl, fetchBlob } from '../lib/api'
import { useCompany } from '../composables/useCompany'
import { useAsync } from '../composables/useAsync'
import AppAvatar from '../components/ui/AppAvatar.vue'
import AppBanner from '../components/ui/AppBanner.vue'
import AppButton from '../components/ui/AppButton.vue'
import AppCard from '../components/ui/AppCard.vue'
import AppEmpty from '../components/ui/AppEmpty.vue'
import AppIcon from '../components/ui/AppIcon.vue'
import AppPageHeader from '../components/ui/AppPageHeader.vue'
import AppSkeleton from '../components/ui/AppSkeleton.vue'
import AppTabs from '../components/ui/AppTabs.vue'

interface Card {
  name: string
  email: string
  phone: string | null
  employeeUuid: string
  avatarUrl: string | null
  company: string
  companyLogoUrl: string | null
  companyEmail: string | null
  companyPhone: string | null
  branch: string | null
  role: string | null
}

const { auth, companyId } = useCompany()
const { loading, error, run } = useAsync()
const card = ref<Card | null>(null)
const qrUrl = ref('')
const sisi = ref<'depan' | 'belakang'>('depan')

function deviceId() {
  return localStorage.getItem('aurevant-device') ?? 'belum terikat di perangkat ini'
}

onMounted(async () => {
  if (!auth.user) return
  card.value = await run(() =>
    api.get<Card>(`/companies/${companyId.value}/users/${auth.user!.id}/card`, auth.token),
  )
  if (card.value) {
    qrUrl.value = await run(() => fetchBlob(`/companies/${companyId.value}/users/${auth.user!.id}/card/qr`, auth.token)) ?? ''
  }
})
onUnmounted(() => { if (qrUrl.value) URL.revokeObjectURL(qrUrl.value) })

function cetak() {
  window.print()
}
</script>

<template>
  <div class="mx-auto max-w-2xl">
    <AppPageHeader
      class="no-print"
      eyebrow="Kredensial kepegawaian"
      title="Kartu identitas digital"
      subtitle="Tunjukkan kartu ini saat validasi kehadiran atau saat diminta HR."
      icon="badge"
    >
      <template #actions>
        <AppButton variant="ghost" :disabled="!card" @click="cetak">
          <AppIcon name="print" :size="18" />Cetak kartu
        </AppButton>
      </template>
    </AppPageHeader>

    <div class="print-only mb-4 border-b border-outline pb-3">
      <p class="font-display text-lg font-bold">{{ card?.company ?? auth.companyName ?? 'Aurevant' }}</p>
      <p class="text-sm">Kartu karyawan {{ card?.name ?? auth.user?.name }}</p>
    </div>

    <AppBanner tone="red" :message="error" />

    <AppTabs
      v-model="sisi"
      class="no-print"
      :items="[
        { key: 'depan', label: 'Sisi depan' },
        { key: 'belakang', label: 'Sisi belakang' },
      ]"
      label="Sisi kartu"
      id-prefix="card-tab"
      panel-prefix="card-panel"
    />

    <div v-if="loading && !card" class="mt-4 space-y-3" aria-hidden="true">
      <AppSkeleton height="16rem" rounded="xl" />
      <AppSkeleton height="4rem" rounded="xl" />
    </div>

    <AppEmpty
      v-else-if="!card && !error"
      class="mt-4"
      title="Kartu belum tersedia"
      icon="badge"
      message="Kartu karyawan belum bisa dimuat. Hubungi HR bila data Anda sudah aktif."
    />

    <div v-if="card" class="print-area mt-4 space-y-4">
      <section
        v-if="sisi === 'depan'"
        id="card-panel-depan"
        role="tabpanel"
        aria-labelledby="card-tab-depan"
      >
        <div class="overflow-hidden rounded-2xl bg-primary p-5 text-white shadow-float sm:p-6">
          <div class="flex items-start justify-between gap-3">
            <div class="flex min-w-0 items-center gap-2.5">
              <AppAvatar
                :name="card.company"
                :src="assetUrl(card.companyLogoUrl)"
                size="sm"
                square
                class="ring-white/30"
                fallback-icon="storefront"
              />
              <div class="min-w-0">
                <p class="font-display truncate font-bold">{{ card.company }}</p>
                <p class="text-2xs uppercase tracking-[0.12em] text-white/70">Kartu karyawan</p>
              </div>
            </div>
            <AppIcon name="badge" :size="28" class="shrink-0 text-white/85" />
          </div>

          <div class="mt-5 flex items-center gap-3.5">
            <AppAvatar
              :name="card.name"
              :src="assetUrl(card.avatarUrl)"
              size="xl"
              class="ring-2 ring-white/40"
            />
            <div class="min-w-0">
              <h2 class="font-display truncate text-xl font-bold">{{ card.name }}</h2>
              <p class="text-sm font-semibold text-white/85">{{ card.role ?? 'Karyawan' }}</p>
              <p class="text-sm text-white/75">{{ card.branch ?? 'Semua cabang' }}</p>
            </div>
          </div>

          <div class="mt-5 flex items-end justify-between gap-4 border-t border-white/15 pt-4">
            <div class="min-w-0">
              <p class="text-2xs uppercase tracking-[0.12em] text-white/60">ID karyawan</p>
              <p class="mt-1 break-all font-mono text-xs text-white/85">{{ card.employeeUuid }}</p>
              <p class="mt-2 max-w-52 text-2xs leading-relaxed text-white/65">
                QR ini untuk validasi kehadiran, bukan data pribadi.
              </p>
            </div>
            <img
              v-if="qrUrl"
              :src="qrUrl"
              alt="QR kartu karyawan"
              class="h-24 w-24 shrink-0 rounded-lg bg-white p-1"
            />
          </div>
        </div>
      </section>

      <section
        v-else
        id="card-panel-belakang"
        role="tabpanel"
        aria-labelledby="card-tab-belakang"
      >
        <div class="overflow-hidden rounded-2xl bg-primary p-5 text-white shadow-float sm:p-6">
          <div class="flex items-center justify-between gap-3">
            <div>
              <p class="font-display font-bold">{{ card.company }}</p>
              <p class="text-2xs uppercase tracking-[0.12em] text-white/70">Kontak & validasi</p>
            </div>
            <AppIcon name="contact_page" :size="26" class="text-white/85" />
          </div>

          <div class="mt-4 space-y-1 rounded-xl bg-white/10 p-3 text-sm">
            <p class="break-all">{{ card.email }}</p>
            <p v-if="card.phone">{{ card.phone }}</p>
            <p v-if="card.companyEmail || card.companyPhone" class="pt-2 text-xs text-white/75">
              {{ [card.companyEmail, card.companyPhone].filter(Boolean).join(' · ') }}
            </p>
          </div>

          <div class="mt-4 flex items-center justify-center">
            <img
              v-if="qrUrl"
              :src="qrUrl"
              alt="QR kartu karyawan"
              class="h-36 w-36 rounded-lg bg-white p-2"
            />
          </div>
          <p class="mt-2 text-center text-2xs text-white/65">
            Tunjukkan QR ini saat diminta HR atau perangkat cabang.
          </p>
        </div>
      </section>

      <AppCard title="Rincian data" subtitle="Hanya baca">
        <dl v-if="card" class="divide-y divide-outline/60 text-sm">
          <div class="flex items-center justify-between gap-3 py-2 first:pt-0 last:pb-0">
            <dt class="text-secondary">Nama</dt>
            <dd class="text-right font-semibold text-ink">{{ card.name }}</dd>
          </div>
          <div class="flex items-center justify-between gap-3 py-2">
            <dt class="text-secondary">Cabang</dt>
            <dd class="text-right font-semibold text-ink">{{ card.branch ?? 'Semua cabang' }}</dd>
          </div>
          <div class="flex items-center justify-between gap-3 py-2 last:pb-0">
            <dt class="text-secondary">ID karyawan</dt>
            <dd class="break-all text-right font-mono text-xs font-semibold text-primary">
              {{ card.employeeUuid }}
            </dd>
          </div>
        </dl>
      </AppCard>

      <AppCard title="Perangkat terikat">
        <p class="break-all font-mono text-xs text-ink">{{ deviceId() }}</p>
        <p class="mt-1.5 text-xs leading-relaxed text-secondary">
          Satu akun satu perangkat. Ganti HP atau hilang? Minta HR meresetnya.
        </p>
      </AppCard>
    </div>
  </div>
</template>
