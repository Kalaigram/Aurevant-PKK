<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { api } from '../lib/api'
import { tanggal } from '../lib/format'
import { labelSubscriptionStatus, labelTenantStatus } from '../lib/labels'
import { useAuthStore } from '../stores/auth'
import { useAsync } from '../composables/useAsync'
import { usePagedList } from '../composables/usePagedList'
import AppBadge from '../components/ui/AppBadge.vue'
import AppBanner from '../components/ui/AppBanner.vue'
import AppButton from '../components/ui/AppButton.vue'
import AppCard from '../components/ui/AppCard.vue'
import AppModal from '../components/ui/AppModal.vue'
import AppPageHeader from '../components/ui/AppPageHeader.vue'
import AppPager from '../components/ui/AppPager.vue'
import AppSearch from '../components/ui/AppSearch.vue'
import AppSelect from '../components/ui/AppSelect.vue'
import AppTable from '../components/ui/AppTable.vue'

interface Tenant {
  id: number
  name: string
  slug: string
  status: string
  branchesCount: number
  usersCount: number
  planName: string | null
  subscriptionStatus: string | null
  endsAt: string | null
  createdAt: string
}

const OPSI_STATUS = [
  { value: 'aktif', label: 'Aktif' },
  { value: 'ditangguhkan', label: 'Ditangguhkan' },
  { value: 'diarsipkan', label: 'Diarsipkan' },
]

const auth = useAuthStore()
const { loading: loadingMutasi, error: errorMutasi, run } = useAsync()
const cari = ref('')
const status = ref('')
const detail = ref<Tenant | null>(null)
const okMsg = ref('')
const tanya = ref<{ t: Tenant; ke: string } | null>(null)
const {
  rows: list,
  meta,
  loading: loadingList,
  error: errorList,
  load: muat,
  setPage,
  setPerPage,
} = usePagedList<Tenant>(
  (page, perPage) => {
    const q =
      `per_page=${perPage}&page=${page}` +
      (cari.value.trim() ? `&search=${encodeURIComponent(cari.value.trim())}` : '') +
      (status.value ? `&status=${status.value}` : '')
    return api.getPage<Tenant>(`/admin/companies?${q}`, auth.token)
  },
  { perPage: 15 },
)
const error = computed(() => errorList.value || errorMutasi.value)

function tone(s: string) {
  if (s === 'aktif') return 'green'
  if (s === 'trial') return 'blue'
  if (s === 'tenggang') return 'amber'
  return 'red'
}
const totalUnit = computed(() => meta.value?.total ?? list.value.length)

async function lihat(id: number) {
  detail.value = await run(() => api.get<Tenant>(`/admin/companies/${id}`, auth.token))
}

async function ubahStatus() {
  if (!tanya.value) return
  const res = await run(() => api.patch(`/admin/companies/${tanya.value!.t.id}`, { status: tanya.value!.ke }, auth.token))
  if (res) {
    okMsg.value = `Perusahaan ${tanya.value.ke === 'aktif' ? 'diaktifkan' : tanya.value.ke === 'ditangguhkan' ? 'ditangguhkan' : 'diarsipkan'}.`
    const id = tanya.value.t.id
    tanya.value = null
    if (detail.value?.id === id) lihat(id)
    muat(meta.value?.current_page ?? 1)
  }
}

onMounted(() => muat())
</script>

<template>
  <div>
    <AppPageHeader
      eyebrow="Administrasi platform"
      title="Perusahaan terdaftar"
      :subtitle="`Kelola ${totalUnit} tenant, status layanan, dan akses platform.`"
      icon="domain"
    />

    <AppBanner tone="green" :message="okMsg" />
    <AppBanner tone="red" :message="error" />

    <div class="mb-4 flex flex-wrap items-end gap-2">
      <AppSearch
        v-model="cari"
        class="min-w-56 flex-1"
        placeholder="Cari nama atau slug perusahaan..."
        @search="muat(1)"
      />
      <AppSelect
        v-model="status"
        class="w-44"
        aria-label="Filter status perusahaan"
        placeholder="Semua status"
        :options="OPSI_STATUS"
        @update:model-value="muat(1)"
      />
      <AppButton variant="ghost" :loading="loadingList" @click="muat(1)">Muat ulang</AppButton>
    </div>

    <AppTable
      :loading="loadingList"
      :empty="!list.length"
      :colspan="7"
      empty-title="Tidak ada perusahaan"
      empty-message="Ubah kata kunci atau filter status untuk menemukan tenant lain."
    >
      <template #head>
        <tr>
          <th>Perusahaan</th>
          <th class="text-center">Cabang</th>
          <th class="text-center">Pengguna</th>
          <th>Paket</th>
          <th>Berlaku s/d</th>
          <th>Status</th>
          <th class="text-right">Aksi</th>
        </tr>
      </template>
      <tr v-for="t in list" :key="t.id" class="transition hover:bg-surface-low/60">
        <td>
          <p class="font-semibold text-ink">{{ t.name }}</p>
          <p class="font-mono text-xs text-secondary">{{ t.slug }}</p>
        </td>
        <td class="text-center tabular-nums">{{ t.branchesCount }}</td>
        <td class="text-center tabular-nums">{{ t.usersCount }}</td>
        <td>
          <p>{{ t.planName ?? '—' }}</p>
          <p v-if="t.subscriptionStatus" class="text-xs text-secondary">
            {{ labelSubscriptionStatus(t.subscriptionStatus) }}
          </p>
        </td>
        <td class="whitespace-nowrap text-secondary">{{ tanggal(t.endsAt) }}</td>
        <td>
          <AppBadge :tone="tone(t.status)" :dot="t.status === 'aktif'">
            {{ labelTenantStatus(t.status) }}
          </AppBadge>
        </td>
        <td class="text-right">
          <AppButton
            size="sm"
            variant="ghost"
            :aria-label="`Lihat detail ${t.name}`"
            @click="lihat(t.id)"
          >
            Detail
          </AppButton>
        </td>
      </tr>
    </AppTable>
    <AppPager :meta="meta" @page="setPage" @update:per-page="setPerPage" />

    <AppCard v-if="detail" class="mt-4">
      <template #header>
        <div class="min-w-0">
          <p class="font-display font-semibold text-ink">Detail perusahaan</p>
          <p class="mt-0.5 text-xs text-secondary">
            Terdaftar {{ tanggal(detail.createdAt) }} · {{ detail.branchesCount }} cabang ·
            {{ detail.usersCount }} pengguna
          </p>
        </div>
      </template>
      <template #actions>
        <AppButton size="sm" variant="ghost" aria-label="Tutup detail" @click="detail = null">
          Tutup
        </AppButton>
      </template>

      <div class="flex flex-wrap items-start justify-between gap-3">
        <div>
          <p class="font-display text-lg font-semibold text-ink">{{ detail.name }}</p>
          <p class="mt-0.5 font-mono text-xs text-secondary">{{ detail.slug }}</p>
        </div>
        <AppBadge :tone="tone(detail.status)" dot>{{ labelTenantStatus(detail.status) }}</AppBadge>
      </div>

      <dl class="mt-4 grid gap-3 text-sm sm:grid-cols-2">
        <div>
          <dt class="text-2xs font-semibold uppercase tracking-[0.08em] text-secondary">Paket</dt>
          <dd class="mt-0.5 font-semibold text-ink">{{ detail.planName ?? 'Tanpa paket' }}</dd>
        </div>
        <div>
          <dt class="text-2xs font-semibold uppercase tracking-[0.08em] text-secondary">Status langganan</dt>
          <dd class="mt-0.5 font-semibold text-ink">
            {{ detail.subscriptionStatus ? labelSubscriptionStatus(detail.subscriptionStatus) : '—' }}
          </dd>
        </div>
        <div>
          <dt class="text-2xs font-semibold uppercase tracking-[0.08em] text-secondary">Berlaku sampai</dt>
          <dd class="mt-0.5 font-semibold text-ink">{{ tanggal(detail.endsAt) }}</dd>
        </div>
        <div>
          <dt class="text-2xs font-semibold uppercase tracking-[0.08em] text-secondary">Kapasitas</dt>
          <dd class="mt-0.5 font-semibold text-ink">
            {{ detail.branchesCount }} cabang · {{ detail.usersCount }} pengguna
          </dd>
        </div>
      </dl>

      <div class="mt-4 flex flex-wrap gap-2 border-t border-outline/50 pt-4">
        <AppButton
          v-for="s in ['aktif', 'ditangguhkan', 'diarsipkan'].filter((x) => x !== detail?.status)"
          :key="s"
          size="sm"
          :variant="s === 'aktif' ? 'primary' : s === 'ditangguhkan' ? 'danger' : 'ghost'"
          @click="tanya = { t: detail, ke: s }"
        >
          {{ s === 'aktif' ? 'Aktifkan' : s === 'ditangguhkan' ? 'Tangguhkan' : 'Arsipkan' }}
        </AppButton>
      </div>
      <p class="mt-2 text-xs leading-relaxed text-secondary">
        Menangguhkan memblokir penulisan di perusahaan tersebut; akses baca tetap tersedia.
      </p>
    </AppCard>

    <AppModal
      :open="!!tanya"
      :title="`Ubah status ${tanya?.t.name ?? ''}?`"
      :description="`Status baru: ${labelTenantStatus(tanya?.ke ?? '')}. Pastikan perubahan ini sudah dikonfirmasi.`"
      size="sm"
      @close="tanya = null"
    >
      <AppBanner tone="red" :message="errorMutasi" />
      <template #footer>
        <AppButton variant="ghost" @click="tanya = null">Batal</AppButton>
        <AppButton :loading="loadingMutasi" @click="ubahStatus">Ya, ubah</AppButton>
      </template>
    </AppModal>
  </div>
</template>
