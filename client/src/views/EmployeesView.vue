<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { api, type PageMeta } from '../lib/api'
import { tanggal } from '../lib/format'
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

interface Penempatan { id: number; userId: number; branchId: number | null; roleId: number | null; roleName: string | null; startedAt: string; endedAt: string | null }
interface Cabang { id: number; code: string; name: string }
interface Role { id: number; name: string }

const { auth, companyId } = useCompany()
const access = useAccessStore()
const { loading, error, run } = useAsync()
const list = ref<Penempatan[]>([])
const meta = ref<PageMeta | null>(null)
const tab = ref<'tempat' | 'role'>('tempat')
const hanyaAktif = ref(false)
const cabang = ref<Cabang[]>([])
const roles = ref<Role[]>([])
const namaUser = ref<Record<number, string>>({})
const okMsg = ref('')

const form = ref({ user_id: '', branch_id: '', role_id: '', started_at: '' })
const akhiri = ref<{ id: number; ended_at: string } | null>(null)
const beri = ref({ user_id: '', role: '' })
const cabut = ref({ user_id: '', role: '' })

const bolehKelola = computed(() => access.can('karyawan.kelola'))

const totalOrang = computed(() => new Set(list.value.map((p) => p.userId)).size)
const aktif = computed(() => list.value.filter((p) => !p.endedAt).length)
const arsip = computed(() => list.value.filter((p) => p.endedAt).length)
const aksesPenuh = computed(() => list.value.filter((p) => !p.endedAt && p.branchId === null).length)
const tersaring = computed(() => hanyaAktif.value ? list.value.filter((p) => !p.endedAt) : list.value)
const kodeCabang = (id: number | null) => (id === null ? '' : (cabang.value.find((b) => b.id === id)?.code ?? `#${id}`))
const namaPengguna = (id: number) => namaUser.value[id] ?? `ID ${id}`

async function muatNama(ids: number[]) {
  for (const id of ids) {
    if (namaUser.value[id]) continue
    const card = await run(() => api.get<{ name: string }>(`/companies/${companyId.value}/users/${id}/card`, auth.token))
    if (card) namaUser.value[id] = card.name
  }
}

async function muat(page = 1) {
  okMsg.value = ''
  const res = await run(() =>
    api.get<{ data: Penempatan[]; meta: PageMeta }>(
      `/companies/${companyId.value}/access/assignments?per_page=15&page=${page}`, auth.token,
    ),
  )
  if (res) {
    const raw = res as unknown as { data: Penempatan[]; meta?: PageMeta } | Penempatan[]
    if (Array.isArray(raw)) list.value = raw
    else { list.value = raw.data; meta.value = raw.meta ?? null }
    muatNama([...new Set(list.value.map((p) => p.userId))])
  }
}

async function muatReferensi() {
  cabang.value = (await run(() => api.get<Cabang[]>(`/companies/${companyId.value}/branches`, auth.token))) ?? []
  const r = await run(() => api.get<{ data: Role[] }>(`/companies/${companyId.value}/access/roles?per_page=100`, auth.token))
  if (r) roles.value = Array.isArray(r) ? r : (r.data ?? [])
}

async function tempatkan() {
  const res = await run(() =>
    api.post(`/companies/${companyId.value}/access/users/${form.value.user_id}/assignments`, {
      branch_id: form.value.branch_id ? Number(form.value.branch_id) : null,
      role_id: form.value.role_id ? Number(form.value.role_id) : null,
      started_at: form.value.started_at,
    }, auth.token),
  )
  if (res) {
    okMsg.value = 'Penempatan tersimpan.'
    form.value = { user_id: '', branch_id: '', role_id: '', started_at: '' }
    muat(1)
  }
}

async function tutup() {
  if (!akhiri.value) return
  const res = await run(() =>
    api.post(`/companies/${companyId.value}/access/assignments/${akhiri.value!.id}/close`, {
      ended_at: akhiri.value!.ended_at,
    }, auth.token),
  )
  if (res) {
    akhiri.value = null
    okMsg.value = 'Penempatan diakhiri.'
    muat(meta.value?.current_page ?? 1)
  }
}

async function beriRole() {
  const res = await run(() =>
    api.post(`/companies/${companyId.value}/access/users/${beri.value.user_id}/roles`, { role: beri.value.role }, auth.token),
  )
  if (res) {
    okMsg.value = 'Role diberikan.'
    beri.value = { user_id: '', role: '' }
  }
}

async function cabutRole() {
  const res = await run(() =>
    api.del(`/companies/${companyId.value}/access/users/${cabut.value.user_id}/roles/${cabut.value.role}`, auth.token),
  )
  if (res !== null || !error.value) {
    okMsg.value = 'Role dicabut.'
    cabut.value = { user_id: '', role: '' }
  }
}

const idTerpakai = computed(() => [...new Set(list.value.map((p) => p.userId))])

onMounted(async () => {
  await access.load(companyId.value, auth.token)
  await muatReferensi()
  muat()
})
</script>

<template>
  <div>
    <p class="text-xs font-semibold uppercase tracking-wider text-secondary">Holding & pengguna</p>
    <h1 class="font-display mb-4 text-xl font-semibold">Karyawan & penempatan</h1>
    <AppBanner tone="green" :message="okMsg" />
    <AppBanner tone="red" :message="error" />

    <div class="mb-4 grid grid-cols-2 gap-3 lg:grid-cols-4">
      <AppCard><p class="text-xs uppercase tracking-wider text-secondary">Total karyawan</p><p class="font-display text-2xl font-bold">{{ totalOrang }} <span class="text-sm font-normal text-secondary">orang</span></p></AppCard>
      <AppCard><p class="text-xs uppercase tracking-wider text-secondary">Penempatan aktif</p><p class="font-display text-2xl font-bold">{{ aktif }}</p></AppCard>
      <AppCard><p class="text-xs uppercase tracking-wider text-secondary">Selesai / arsip</p><p class="font-display text-2xl font-bold">{{ arsip }}</p></AppCard>
      <AppCard><p class="text-xs uppercase tracking-wider text-secondary">Akses penuh</p><p class="font-display text-2xl font-bold">{{ aksesPenuh }}</p></AppCard>
    </div>

    <div class="mb-3 flex gap-2">
      <AppButton :variant="tab === 'tempat' ? 'primary' : 'ghost'" @click="tab = 'tempat'">Penempatan & mutasi</AppButton>
      <AppButton :variant="tab === 'role' ? 'primary' : 'ghost'" @click="tab = 'role'">Role pengguna</AppButton>
    </div>

    <div v-if="tab === 'tempat'">
      <AppCard v-if="bolehKelola" title="Tempatkan karyawan / mutasi cabang">
        <div class="grid gap-2 md:grid-cols-4">
          <AppField label="Karyawan">
            <select v-model="form.user_id" class="w-full rounded-lg border border-outline bg-card px-3 py-2.5 text-sm focus:border-primary focus:outline-none">
              <option value="">Pilih…</option>
              <option v-for="id in idTerpakai" :key="id" :value="id">{{ namaPengguna(id) }}</option>
            </select>
          </AppField>
          <AppField label="Cabang">
            <select v-model="form.branch_id" class="w-full rounded-lg border border-outline bg-card px-3 py-2.5 text-sm focus:border-primary focus:outline-none">
              <option value="">Semua cabang (akses penuh)</option>
              <option v-for="b in cabang" :key="b.id" :value="b.id">{{ b.code }} — {{ b.name }}</option>
            </select>
          </AppField>
          <AppField label="Role (opsional)">
            <select v-model="form.role_id" class="w-full rounded-lg border border-outline bg-card px-3 py-2.5 text-sm focus:border-primary focus:outline-none">
              <option value="">Tanpa role khusus</option>
              <option v-for="r in roles" :key="r.id" :value="r.id">{{ r.name }}</option>
            </select>
          </AppField>
          <AppField label="Tanggal mulai">
            <input v-model="form.started_at" type="date" class="w-full rounded-lg border border-outline bg-card px-3 py-2.5 text-sm focus:border-primary focus:outline-none" />
          </AppField>
        </div>
        <AppButton :loading="loading" class="mt-3" @click="tempatkan">Simpan penempatan</AppButton>
      </AppCard>

      <div class="mb-2 mt-4 flex items-center justify-between">
        <h2 class="font-display font-semibold">Riwayat penempatan</h2>
        <label class="flex cursor-pointer items-center gap-2 text-sm text-secondary">
          <input v-model="hanyaAktif" type="checkbox" class="h-4 w-4 accent-primary" /> Hanya aktif
        </label>
      </div>
      <AppEmpty v-if="!loading && !tersaring.length" message="Belum ada riwayat." />
      <div class="overflow-x-auto rounded-xl bg-card shadow-sm">
        <table class="w-full min-w-[720px] text-sm">
          <thead>
            <tr class="text-left text-xs uppercase text-secondary">
              <th class="px-4 py-2">Nama</th><th class="px-4 py-2">Cabang</th><th class="px-4 py-2">Role</th>
              <th class="px-4 py-2">Mulai</th><th class="px-4 py-2">Selesai</th><th class="px-4 py-2">Status</th>
              <th v-if="bolehKelola" class="px-4 py-2 text-right">Tindakan</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="p in tersaring" :key="p.id" class="border-t border-surface-low">
              <td class="px-4 py-2 font-semibold">{{ namaPengguna(p.userId) }}</td>
              <td class="px-4 py-2">
                <AppBadge v-if="p.branchId === null" tone="green">Akses penuh</AppBadge>
                <span v-else class="font-mono text-xs">{{ kodeCabang(p.branchId) }}</span>
              </td>
              <td class="px-4 py-2">{{ p.roleName ?? '–' }}</td>
              <td class="px-4 py-2">{{ tanggal(p.startedAt) }}</td>
              <td class="px-4 py-2">{{ p.endedAt ? tanggal(p.endedAt) : '–' }}</td>
              <td class="px-4 py-2"><AppBadge :tone="p.endedAt ? 'gray' : 'green'">{{ p.endedAt ? 'Selesai' : 'Aktif' }}</AppBadge></td>
              <td v-if="bolehKelola" class="px-4 py-2 text-right">
                <AppButton v-if="!p.endedAt" variant="ghost" @click="akhiri = { id: p.id, ended_at: '' }">Akhiri</AppButton>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <AppPager :meta="meta" @page="muat" />
    </div>

    <div v-else>
      <div class="grid gap-4 lg:grid-cols-2">
        <AppCard title="Beri role">
          <AppField label="Karyawan">
            <select v-model="beri.user_id" class="w-full rounded-lg border border-outline bg-card px-3 py-2.5 text-sm focus:border-primary focus:outline-none">
              <option value="">Pilih…</option>
              <option v-for="id in idTerpakai" :key="id" :value="id">{{ namaPengguna(id) }}</option>
            </select>
          </AppField>
          <div class="mt-3">
            <AppField label="Role">
              <select v-model="beri.role" class="w-full rounded-lg border border-outline bg-card px-3 py-2.5 text-sm focus:border-primary focus:outline-none">
                <option value="">Pilih…</option>
                <option v-for="r in roles" :key="r.id" :value="r.name">{{ r.name }}</option>
              </select>
            </AppField>
          </div>
          <AppButton :loading="loading" class="mt-3" @click="beriRole">Beri role</AppButton>
        </AppCard>
        <AppCard title="Cabut role">
          <AppField label="Karyawan">
            <select v-model="cabut.user_id" class="w-full rounded-lg border border-outline bg-card px-3 py-2.5 text-sm focus:border-primary focus:outline-none">
              <option value="">Pilih…</option>
              <option v-for="id in idTerpakai" :key="id" :value="id">{{ namaPengguna(id) }}</option>
            </select>
          </AppField>
          <div class="mt-3">
            <AppField label="Nama role persis">
              <select v-model="cabut.role" class="w-full rounded-lg border border-outline bg-card px-3 py-2.5 text-sm focus:border-primary focus:outline-none">
                <option value="">Pilih…</option>
                <option v-for="r in roles" :key="r.id" :value="r.name">{{ r.name }}</option>
              </select>
            </AppField>
          </div>
          <AppButton variant="danger" :loading="loading" class="mt-3" @click="cabutRole">Cabut role</AppButton>
        </AppCard>
      </div>
      <p v-if="!bolehKelola" class="mt-2 text-sm text-secondary">Hanya pemegang izin karyawan.kelola yang bisa mengubah.</p>
    </div>

    <div v-if="akhiri" class="fixed inset-0 z-20 flex items-center justify-center bg-black/40 p-4" @click.self="akhiri = null">
      <div class="w-full max-w-sm rounded-xl bg-card p-5 shadow-xl">
        <h3 class="font-display font-semibold">Akhiri penempatan?</h3>
        <div class="mt-3">
          <AppField label="Tanggal selesai">
            <input v-model="akhiri.ended_at" type="date" class="w-full rounded-lg border border-outline bg-card px-3 py-2.5 text-sm focus:border-primary focus:outline-none" />
          </AppField>
        </div>
        <div class="mt-4 flex justify-end gap-2">
          <AppButton variant="ghost" @click="akhiri = null">Batal</AppButton>
          <AppButton :loading="loading" @click="tutup">Akhiri</AppButton>
        </div>
      </div>
    </div>
  </div>
</template>
