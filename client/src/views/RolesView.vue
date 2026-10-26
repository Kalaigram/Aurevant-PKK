<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { api } from '../lib/api'
import { useCompany } from '../composables/useCompany'
import { useAccessStore } from '../stores/access'
import { useAsync } from '../composables/useAsync'
import { usePagedList } from '../composables/usePagedList'
import AppCard from '../components/ui/AppCard.vue'
import AppBadge from '../components/ui/AppBadge.vue'
import AppBanner from '../components/ui/AppBanner.vue'
import AppButton from '../components/ui/AppButton.vue'
import AppEmpty from '../components/ui/AppEmpty.vue'
import AppField from '../components/ui/AppField.vue'
import AppIcon from '../components/ui/AppIcon.vue'
import AppInput from '../components/ui/AppInput.vue'
import AppModal from '../components/ui/AppModal.vue'
import AppPageHeader from '../components/ui/AppPageHeader.vue'
import AppPager from '../components/ui/AppPager.vue'
import AppSelect from '../components/ui/AppSelect.vue'
import AppSkeleton from '../components/ui/AppSkeleton.vue'

interface Role { id: number; name: string; permissions: string[] }

const GRUP: { judul: string; prefix: string[] }[] = [
  { judul: 'Transaksi', prefix: ['transaksi.', 'jasa.proses'] },
  { judul: 'Retur', prefix: ['retur.'] },
  { judul: 'Kasir', prefix: ['nota.', 'shift.buka', 'shift.tutup'] },
  { judul: 'Stok', prefix: ['stok.'] },
  { judul: 'Pembelian', prefix: ['po.'] },
  { judul: 'Keuangan', prefix: ['keuangan.', 'biaya.', 'periode.', 'laporan.'] },
  { judul: 'Jadwal', prefix: ['jadwal.', 'shift.kelola'] },
  { judul: 'Absensi', prefix: ['absensi.'] },
  { judul: 'Gaji', prefix: ['gaji.'] },
  { judul: 'Katalog', prefix: ['katalog.', 'harga.'] },
  { judul: 'Organisasi', prefix: ['cabang.', 'karyawan.'] },
  { judul: 'Sistem', prefix: ['audit.', 'dashboard.', 'pelanggan.', 'impor.', 'pengaturan.', 'langganan.'] },
]

const { auth, companyId } = useCompany()
const access = useAccessStore()
const { loading: loadingMutasi, error: errorMutasi, run } = useAsync()
const {
  rows: roles,
  meta,
  loading: loadingList,
  error: errorList,
  load: muat,
  setPage,
  setPerPage,
} = usePagedList<Role>(
  (page, perPage) =>
    api.getPage<Role>(
      `/companies/${companyId.value}/access/roles?per_page=${perPage}&page=${page}`,
      auth.token,
    ),
  { perPage: 15 },
)
const error = computed(() => errorList.value || errorMutasi.value)
const katalog = ref<string[]>([])
const templates = ref<string[]>([])
const okMsg = ref('')

const showForm = ref(false)
const sunting = ref<Role | null>(null)
const nama = ref('')
const template = ref('')
const dipilih = ref<string[]>([])
const tanyaHapus = ref<Role | null>(null)

const bolehKelola = computed(() => access.can('pengaturan.kelola'))
const opsiTemplate = computed(() => templates.value.map((t) => ({ value: t, label: t })))

function grupUntuk(perm: string): string {
  return GRUP.find((g) => g.prefix.some((p) => perm.startsWith(p)))?.judul ?? 'Lainnya'
}

async function muatReferensi() {
  katalog.value = (await run(() => api.get<string[]>(`/companies/${companyId.value}/access/permissions`, auth.token))) ?? []
  templates.value = (await run(() => api.get<string[]>(`/companies/${companyId.value}/access/templates`, auth.token))) ?? []
}

function bukaTambah() {
  sunting.value = null
  nama.value = ''
  template.value = ''
  dipilih.value = []
  showForm.value = true
}

async function bukaUbah(r: Role) {
  const detail = await run(() => api.get<Role>(`/companies/${companyId.value}/access/roles/${r.id}`, auth.token))
  if (!detail) return
  sunting.value = detail
  nama.value = detail.name
  template.value = ''
  dipilih.value = [...detail.permissions]
  showForm.value = true
}

function toggleIzin(p: string) {
  dipilih.value = dipilih.value.includes(p) ? dipilih.value.filter((x) => x !== p) : [...dipilih.value, p]
}

async function simpan() {
  const base = `/companies/${companyId.value}/access/roles`
  const body: Record<string, unknown> = { name: nama.value.trim() }
  if (sunting.value) {
    body.permissions = dipilih.value
  } else if (template.value) {
    body.template = template.value
  } else {
    body.permissions = dipilih.value
  }
  const res = sunting.value
    ? await run(() => api.put(base + `/${sunting.value!.id}`, body, auth.token))
    : await run(() => api.post(base, body, auth.token))
  if (res) {
    showForm.value = false
    okMsg.value = sunting.value ? 'Role diperbarui.' : 'Role baru dibuat.'
    muat(meta.value?.current_page ?? 1)
  }
}

async function hapus() {
  if (!tanyaHapus.value) return
  await run(() => api.del(`/companies/${companyId.value}/access/roles/${tanyaHapus.value!.id}`, auth.token))
  if (!errorMutasi.value) {
    tanyaHapus.value = null
    okMsg.value = 'Role dihapus.'
    muat(1)
  }
}

onMounted(async () => {
  await access.load(companyId.value, auth.token)
  muat()
  muatReferensi()
})
</script>

<template>
  <div>
    <AppPageHeader
      eyebrow="Kontrol akses berbasis peran"
      title="Role & hak akses"
      subtitle="Atur kumpulan izin yang menentukan batas akses tiap pengguna."
      icon="admin_panel_settings"
    >
      <template #meta>
        <div class="mt-2 flex flex-wrap gap-2">
          <AppBadge tone="blue">{{ meta?.total ?? roles.length }} role</AppBadge>
          <AppBadge tone="gray">{{ katalog.length }} izin tersedia</AppBadge>
        </div>
      </template>
      <template #actions>
        <AppButton v-if="bolehKelola" @click="bukaTambah">
          <AppIcon name="add" :size="18" />Buat role
        </AppButton>
      </template>
    </AppPageHeader>

    <AppBanner tone="green" :message="okMsg" />
    <AppBanner tone="red" :message="error" />

    <div v-if="loadingList" class="grid gap-3 md:grid-cols-2 xl:grid-cols-3" aria-busy="true">
      <AppCard v-for="i in 3" :key="i"><AppSkeleton :lines="4" /></AppCard>
      <span class="sr-only" role="status">Memuat role…</span>
    </div>
    <AppEmpty
      v-else-if="!roles.length"
      icon="shield_person"
      title="Belum ada role"
      message="Buat role pertama atau gunakan template izin yang tersedia."
    >
      <AppButton v-if="bolehKelola" @click="bukaTambah">Buat role</AppButton>
    </AppEmpty>
    <div v-else class="grid gap-3 md:grid-cols-2 xl:grid-cols-3">
      <AppCard v-for="r in roles" :key="r.id">
        <div class="flex items-start justify-between gap-2">
          <div class="flex min-w-0 items-center gap-2.5">
            <span class="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-primary-soft text-primary-ink">
              <AppIcon name="manage_accounts" :size="20" />
            </span>
            <h2 class="truncate font-display font-semibold text-ink">{{ r.name }}</h2>
          </div>
          <AppBadge tone="gray">{{ r.permissions.length }} izin</AppBadge>
        </div>
        <p class="mt-3 line-clamp-2 font-mono text-2xs leading-relaxed text-secondary">
          {{ r.permissions.join(', ') || 'Tanpa izin.' }}
        </p>
        <div v-if="bolehKelola" class="mt-3 flex gap-2 border-t border-outline/50 pt-3">
          <AppButton
            size="sm"
            variant="ghost"
            :aria-label="`Ubah role ${r.name}`"
            @click="bukaUbah(r)"
          >
            Ubah
          </AppButton>
          <AppButton
            size="sm"
            variant="ghost"
            class="text-danger"
            :aria-label="`Hapus role ${r.name}`"
            @click="tanyaHapus = r"
          >
            Hapus
          </AppButton>
        </div>
      </AppCard>
    </div>
    <AppPager :meta="meta" @page="setPage" @update:per-page="setPerPage" />

    <AppCard title="Katalog izin (baca saja)" class="mt-4">
      <div class="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
        <div v-for="g in GRUP" :key="g.judul">
          <p class="mb-1 text-xs font-semibold uppercase tracking-wider text-secondary">{{ g.judul }}</p>
          <label v-for="p in katalog.filter((x) => grupUntuk(x) === g.judul)" :key="p" class="flex cursor-default items-center gap-2 py-0.5 text-sm">
            <input type="checkbox" checked disabled class="h-4 w-4 accent-primary" />
            <span class="font-mono text-xs">{{ p }}</span>
          </label>
        </div>
      </div>
      <p class="mt-3 text-xs text-secondary">Template hanya mengisi nilai awal; yang berlaku izin milik role itu.</p>
    </AppCard>

    <AppModal
      :open="showForm"
      :title="sunting ? 'Ubah role' : 'Buat role baru'"
      description="Nama role harus unik. Izin yang dicentang berlaku untuk semua pengguna dengan role ini."
      size="lg"
      @close="showForm = false"
    >
      <AppBanner tone="red" :message="errorMutasi" />
      <div class="space-y-3">
        <AppField label="Nama role" required>
          <AppInput
            v-model="nama"
            maxlength="255"
            placeholder="cth. Supervisor Shift"
            data-autofocus
          />
        </AppField>
        <AppField
          v-if="!sunting"
          label="Template awal"
          optional
          hint="Template hanya mengisi izin awal; Anda tetap bisa menyesuaikannya."
        >
          <AppSelect
            v-model="template"
            placeholder="Tanpa template (pilih manual)"
            :options="opsiTemplate"
          />
        </AppField>
        <div v-if="sunting || !template">
          <p class="mb-1.5 text-[13px] font-semibold text-ink">
            Izin <span class="font-normal text-secondary">({{ dipilih.length }} dipilih)</span>
          </p>
          <div class="max-h-64 space-y-3 overflow-y-auto rounded-xl border border-outline/70 bg-surface-low/50 p-3">
            <div v-for="g in GRUP" :key="g.judul">
              <p class="mb-1 text-2xs font-semibold uppercase tracking-[0.08em] text-secondary">
                {{ g.judul }}
              </p>
              <label
                v-for="p in katalog.filter((x) => grupUntuk(x) === g.judul)"
                :key="p"
                class="flex cursor-pointer items-center gap-2 rounded-lg px-1 py-1 text-sm hover:bg-card"
              >
                <input
                  type="checkbox"
                  :checked="dipilih.includes(p)"
                  class="h-4 w-4 rounded border-outline accent-primary focus:ring-2 focus:ring-primary/30"
                  @change="toggleIzin(p)"
                />
                <span class="font-mono text-xs text-ink">{{ p }}</span>
              </label>
            </div>
          </div>
        </div>
      </div>
      <template #footer>
        <AppButton variant="ghost" @click="showForm = false">Batal</AppButton>
        <AppButton
          :loading="loadingMutasi"
          :disabled="!nama.trim() || (!sunting && !template && !dipilih.length)"
          @click="simpan"
        >
          {{ sunting ? 'Simpan perubahan' : 'Buat role' }}
        </AppButton>
      </template>
    </AppModal>

    <AppModal
      :open="!!tanyaHapus"
      :title="`Hapus role ${tanyaHapus?.name ?? ''}?`"
      description="Pengguna yang memakai role ini kehilangan izinnya. Tindakan ini tidak bisa dibatalkan."
      size="sm"
      @close="tanyaHapus = null"
    >
      <AppBanner tone="red" :message="errorMutasi" />
      <template #footer>
        <AppButton variant="ghost" @click="tanyaHapus = null">Batal</AppButton>
        <AppButton variant="danger" :loading="loadingMutasi" @click="hapus">
          Ya, hapus
        </AppButton>
      </template>
    </AppModal>
  </div>
</template>
