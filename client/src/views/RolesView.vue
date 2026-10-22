<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { api, type PageMeta } from '../lib/api'
import { useCompany } from '../composables/useCompany'
import { useAccessStore } from '../stores/access'
import { useAsync } from '../composables/useAsync'
import AppCard from '../components/ui/AppCard.vue'
import AppBadge from '../components/ui/AppBadge.vue'
import AppBanner from '../components/ui/AppBanner.vue'
import AppButton from '../components/ui/AppButton.vue'
import AppField from '../components/ui/AppField.vue'
import AppEmpty from '../components/ui/AppEmpty.vue'
import AppPager from '../components/ui/AppPager.vue'
import AppIcon from '../components/ui/AppIcon.vue'

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
const { loading, error, run } = useAsync()
const roles = ref<Role[]>([])
const meta = ref<PageMeta | null>(null)
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

function grupUntuk(perm: string): string {
  return GRUP.find((g) => g.prefix.some((p) => perm.startsWith(p)))?.judul ?? 'Lainnya'
}

async function muat(page = 1) {
  okMsg.value = ''
  const res = await run(() =>
    api.getPage<Role>(`/companies/${companyId.value}/access/roles?per_page=15&page=${page}`, auth.token),
  )
  if (res) {
    const raw = res as unknown as { data: Role[]; meta?: PageMeta } | Role[]
    if (Array.isArray(raw)) roles.value = raw
    else { roles.value = raw.data; meta.value = raw.meta ?? null }
  }
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
  if (!error.value) {
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
    <p class="text-xs font-semibold uppercase tracking-wider text-secondary">Kontrol akses berbasis peran</p>
    <div class="mb-4 flex flex-wrap items-center justify-between gap-2">
      <h1 class="font-display text-xl font-semibold">Role & hak akses</h1>
      <div class="flex gap-2">
        <AppBadge tone="blue">{{ roles.length }} role</AppBadge>
        <AppBadge tone="blue">{{ katalog.length }} izin</AppBadge>
        <AppButton v-if="bolehKelola" @click="bukaTambah"><AppIcon name="add" :size="18" />Buat role</AppButton>
      </div>
    </div>
    <AppBanner tone="green" :message="okMsg" />
    <AppBanner tone="red" :message="error" />

    <AppEmpty v-if="!loading && !roles.length" message="Belum ada role." />
    <div class="grid gap-3 md:grid-cols-2 xl:grid-cols-3">
      <AppCard v-for="r in roles" :key="r.id">
        <div class="flex items-start justify-between gap-2">
          <div class="flex items-center gap-2">
            <span class="flex h-9 w-9 items-center justify-center rounded-lg bg-surface-low text-primary">
              <AppIcon name="manage_accounts" :size="20" />
            </span>
            <h2 class="font-display font-semibold">{{ r.name }}</h2>
          </div>
          <AppBadge tone="gray">{{ r.permissions.length }} izin</AppBadge>
        </div>
        <p class="mt-2 line-clamp-2 font-mono text-[11px] leading-relaxed text-secondary">{{ r.permissions.join(', ') || 'Tanpa izin.' }}</p>
        <div v-if="bolehKelola" class="mt-3 flex gap-2">
          <AppButton variant="ghost" @click="bukaUbah(r)">Ubah</AppButton>
          <AppButton variant="ghost" @click="tanyaHapus = r"><span class="text-danger">Hapus</span></AppButton>
        </div>
      </AppCard>
    </div>
    <AppPager :meta="meta" @page="muat" />

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

    <div v-if="showForm" class="fixed inset-0 z-20 flex items-center justify-center bg-black/40 p-4" @click.self="showForm = false">
      <div class="max-h-[90vh] w-full max-w-lg overflow-y-auto rounded-xl bg-card p-5 shadow-xl">
        <h3 class="font-display font-semibold">{{ sunting ? 'Ubah role' : 'Buat role baru' }}</h3>
        <div class="mt-3">
          <AppField label="Nama role (wajib, unik)">
            <input v-model="nama" maxlength="255" class="w-full rounded-lg bg-surface-low px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-primary" placeholder="cth. Supervisor Shift" />
          </AppField>
        </div>
        <div v-if="!sunting" class="mt-3">
          <AppField label="Template awal (opsional — izin mengikuti template)">
            <select v-model="template" class="w-full rounded-lg bg-surface-low px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-primary">
              <option value="">Tanpa template (pilih manual)</option>
              <option v-for="t in templates" :key="t" :value="t">{{ t }}</option>
            </select>
          </AppField>
        </div>
        <div v-if="sunting || !template" class="mt-3">
          <p class="mb-1 text-sm font-medium">Izin ({{ dipilih.length }} dipilih)</p>
          <div class="max-h-56 space-y-3 overflow-y-auto rounded-lg bg-surface-low p-3">
            <div v-for="g in GRUP" :key="g.judul">
              <p class="mb-1 text-[11px] font-semibold uppercase tracking-wider text-secondary">{{ g.judul }}</p>
              <label v-for="p in katalog.filter((x) => grupUntuk(x) === g.judul)" :key="p" class="flex cursor-pointer items-center gap-2 py-0.5 text-sm">
                <input type="checkbox" :checked="dipilih.includes(p)" class="h-4 w-4 accent-primary" @change="toggleIzin(p)" />
                <span class="font-mono text-xs">{{ p }}</span>
              </label>
            </div>
          </div>
        </div>
        <div class="mt-4 flex justify-end gap-2">
          <AppButton variant="ghost" @click="showForm = false">Batal</AppButton>
          <AppButton :loading="loading" @click="simpan">{{ sunting ? 'Simpan' : 'Buat role' }}</AppButton>
        </div>
      </div>
    </div>

    <div v-if="tanyaHapus" class="fixed inset-0 z-20 flex items-center justify-center bg-black/40 p-4" @click.self="tanyaHapus = null">
      <div class="w-full max-w-sm rounded-xl bg-card p-5 shadow-xl">
        <h3 class="font-display font-semibold">Hapus role {{ tanyaHapus.name }}?</h3>
        <p class="mt-1 text-sm text-secondary">Pengguna yang memakai role ini kehilangan izinnya. Tindakan ini tidak bisa dibatalkan.</p>
        <div class="mt-4 flex justify-end gap-2">
          <AppButton variant="ghost" @click="tanyaHapus = null">Batal</AppButton>
          <AppButton variant="danger" :loading="loading" @click="hapus">Ya, hapus</AppButton>
        </div>
      </div>
    </div>
  </div>
</template>
