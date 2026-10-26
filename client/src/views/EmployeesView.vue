<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { api, assetUrl } from '../lib/api'
import { tanggal } from '../lib/format'
import { useCompany } from '../composables/useCompany'
import { useAccessStore } from '../stores/access'
import { useAsync } from '../composables/useAsync'
import { usePagedList } from '../composables/usePagedList'
import { useToast } from '../composables/useToast'
import AppAvatar from '../components/ui/AppAvatar.vue'
import AppBadge from '../components/ui/AppBadge.vue'
import AppBanner from '../components/ui/AppBanner.vue'
import AppButton from '../components/ui/AppButton.vue'
import AppCard from '../components/ui/AppCard.vue'
import AppCheckbox from '../components/ui/AppCheckbox.vue'
import AppDateInput from '../components/ui/AppDateInput.vue'
import AppField from '../components/ui/AppField.vue'
import AppInput from '../components/ui/AppInput.vue'
import AppModal from '../components/ui/AppModal.vue'
import AppPageHeader from '../components/ui/AppPageHeader.vue'
import AppPager from '../components/ui/AppPager.vue'
import AppSearch from '../components/ui/AppSearch.vue'
import AppSelect from '../components/ui/AppSelect.vue'
import AppStat from '../components/ui/AppStat.vue'
import AppTable from '../components/ui/AppTable.vue'
import AppTabs from '../components/ui/AppTabs.vue'

interface Penempatan {
  id: number
  userId: number
  userName: string | null
  userEmail: string | null
  userAvatarUrl: string | null
  branchId: number | null
  branchCode: string | null
  branchName: string | null
  roleId: number | null
  roleName: string | null
  startedAt: string
  endedAt: string | null
}
interface Cabang {
  id: number
  code: string
  name: string
}
interface Role {
  id: number
  name: string
}

const PTKP = ['TK/0', 'TK/1', 'TK/2', 'TK/3', 'K/0', 'K/1', 'K/2', 'K/3']

const { auth, companyId } = useCompany()
const access = useAccessStore()
const toast = useToast()

const tab = ref<'tempat' | 'role'>('tempat')
const cari = ref('')
const filterCabang = ref('')
const filterRole = ref('')
const hanyaAktif = ref(false)
const referensi = ref<Penempatan[]>([])
const cabang = ref<Cabang[]>([])
const roles = ref<Role[]>([])
const statistik = ref({ aktif: 0, arsip: 0 })

const { loading: mutasiLoading, error: mutasiError, run: runMutasi } = useAsync()
const { loading: referensiLoading, error: referensiError, run: runReferensi } = useAsync()

const {
  rows: list,
  meta,
  loading,
  error: errorList,
  load,
  setPage,
  setPerPage,
} = usePagedList<Penempatan>((page, perPage) => {
  const params = new URLSearchParams({ per_page: String(perPage), page: String(page) })
  if (cari.value.trim()) params.set('q', cari.value.trim())
  if (filterCabang.value) params.set('branch_id', filterCabang.value)
  if (filterRole.value) params.set('role_id', filterRole.value)
  if (hanyaAktif.value) params.set('active', '1')
  return api.getPage<Penempatan>(
    `/companies/${companyId.value}/access/assignments?${params}`,
    auth.token,
  )
})

const error = computed(() => errorList.value || mutasiError.value || referensiError.value)
const bolehKelola = computed(() => access.can('karyawan.kelola'))
const loadingHalaman = computed(() => loading.value || referensiLoading.value)

const opsiCabang = computed(() =>
  cabang.value.map((item) => ({ value: String(item.id), label: `${item.code} — ${item.name}` })),
)
const opsiRole = computed(() => roles.value.map((item) => ({ value: item.name, label: item.name })))
const opsiRoleId = computed(() =>
  roles.value.map((item) => ({ value: String(item.id), label: item.name })),
)
const opsiKaryawan = computed(() => {
  const seen = new Map<number, string>()
  for (const item of referensi.value) {
    if (item.userName && !seen.has(item.userId)) seen.set(item.userId, item.userName)
  }
  return [...seen.entries()]
    .map(([id, name]) => ({ value: String(id), label: name }))
    .sort((a, b) => a.label.localeCompare(b.label))
})
const tabs = computed(() => [
  { key: 'tempat', label: 'Penempatan', count: meta.value?.total ?? 0 },
  { key: 'role', label: 'Role pengguna', count: roles.value.length },
])

const tambahOpen = ref(false)
const tempatOpen = ref(false)
const kredensial = ref<{ email: string; password: string } | null>(null)
const resetTarget = ref<{ userId: number; name: string } | null>(null)
const resetPw = ref('')
const resetHasil = ref('')
const akhiriId = ref<number | null>(null)
const akhirTanggal = ref(new Date().toISOString().slice(0, 10))

const karyawanBaru = ref(formKaryawanKosong())
const formTempat = ref(formTempatKosong())
const beri = ref({ user_id: '', role: '' })
const cabut = ref({ user_id: '', role: '' })

function formKaryawanKosong() {
  return {
    name: '',
    email: '',
    phone: '',
    branch_id: '',
    role_id: '',
    ptkp_status: 'TK/0',
    password: '',
  }
}

function formTempatKosong() {
  return {
    user_id: '',
    branch_id: '',
    role_id: '',
    started_at: new Date().toISOString().slice(0, 10),
  }
}

const namaPengguna = (item: Penempatan) => item.userName ?? `Pengguna #${item.userId}`
const cabangPengguna = (item: Penempatan) =>
  item.branchId === null
    ? 'Akses penuh'
    : (item.branchCode ?? item.branchName ?? `#${item.branchId}`)

async function muatStatistik() {
  const base = `/companies/${companyId.value}/access/assignments?per_page=1`
  const [aktif, semua] = await Promise.all([
    api.getPage<Penempatan>(`${base}&active=1`, auth.token).catch(() => null),
    api.getPage<Penempatan>(base, auth.token).catch(() => null),
  ])
  const totalAktif = aktif?.meta?.total ?? 0
  statistik.value = {
    aktif: totalAktif,
    arsip: Math.max(0, (semua?.meta?.total ?? 0) - totalAktif),
  }
}

async function muatReferensi() {
  const assignments = await runReferensi(() =>
    api.getPage<Penempatan>(
      `/companies/${companyId.value}/access/assignments?per_page=100`,
      auth.token,
    ),
  )
  if (assignments) referensi.value = assignments.data

  const branches = await runReferensi(() =>
    api.getPage<Cabang>(`/companies/${companyId.value}/branches?per_page=100`, auth.token),
  )
  if (branches) cabang.value = branches.data

  const roleList = await runReferensi(() =>
    api.getPage<Role>(`/companies/${companyId.value}/access/roles?per_page=100`, auth.token),
  )
  if (roleList) roles.value = roleList.data
}

async function muat() {
  await load(1)
}

function bukaTambah() {
  karyawanBaru.value = formKaryawanKosong()
  tambahOpen.value = true
}

function bukaTempat(item?: Penempatan) {
  formTempat.value = item
    ? {
        user_id: String(item.userId),
        branch_id: item.branchId ? String(item.branchId) : '',
        role_id: item.roleId ? String(item.roleId) : '',
        started_at: new Date().toISOString().slice(0, 10),
      }
    : formTempatKosong()
  tempatOpen.value = true
}

async function tambahKaryawan() {
  const res = await runMutasi(() =>
    api.post<{ user: { id: number; name: string }; initialPassword: string }>(
      `/companies/${companyId.value}/users`,
      {
        name: karyawanBaru.value.name.trim(),
        email: karyawanBaru.value.email.trim(),
        phone: karyawanBaru.value.phone.trim() || null,
        branch_id: karyawanBaru.value.branch_id ? Number(karyawanBaru.value.branch_id) : null,
        role_id: karyawanBaru.value.role_id ? Number(karyawanBaru.value.role_id) : null,
        ptkp_status: karyawanBaru.value.ptkp_status || null,
        password: karyawanBaru.value.password || null,
      },
      auth.token,
    ),
  )
  if (!res) return

  tambahOpen.value = false
  kredensial.value = {
    email: karyawanBaru.value.email.trim(),
    password: res.initialPassword,
  }
  toast.success('Akun karyawan dibuat.')
  await Promise.all([load(1), muatStatistik(), muatReferensi()])
}

async function tempatkan() {
  const res = await runMutasi(() =>
    api.post(
      `/companies/${companyId.value}/access/users/${formTempat.value.user_id}/assignments`,
      {
        branch_id: formTempat.value.branch_id ? Number(formTempat.value.branch_id) : null,
        role_id: formTempat.value.role_id ? Number(formTempat.value.role_id) : null,
        started_at: formTempat.value.started_at,
      },
      auth.token,
    ),
  )
  if (!res) return

  tempatOpen.value = false
  toast.success('Penempatan tersimpan.')
  await Promise.all([load(), muatStatistik(), muatReferensi()])
}

function bukaReset(item: Penempatan) {
  resetTarget.value = { userId: item.userId, name: namaPengguna(item) }
  resetPw.value = ''
  resetHasil.value = ''
}

function bukaAkhiri(item: Penempatan) {
  akhiriId.value = item.id
  akhirTanggal.value = new Date().toISOString().slice(0, 10)
}

async function resetSandi() {
  if (!resetTarget.value) return
  const res = await runMutasi(() =>
    api.post<{ initialPassword: string }>(
      `/companies/${companyId.value}/users/${resetTarget.value!.userId}/reset-password`,
      { password: resetPw.value || null },
      auth.token,
    ),
  )
  if (res) resetHasil.value = res.initialPassword
}

async function tutupPenempatan() {
  if (!akhiriId.value) return
  const res = await runMutasi(() =>
    api.post(
      `/companies/${companyId.value}/access/assignments/${akhiriId.value}/close`,
      { ended_at: akhirTanggal.value },
      auth.token,
    ),
  )
  if (!res) return

  akhiriId.value = null
  toast.success('Penempatan diakhiri.')
  await Promise.all([load(), muatStatistik()])
}

async function beriRole() {
  const res = await runMutasi(() =>
    api.post(
      `/companies/${companyId.value}/access/users/${beri.value.user_id}/roles`,
      { role: beri.value.role },
      auth.token,
    ),
  )
  if (!res) return

  toast.success('Role diberikan.')
  beri.value = { user_id: '', role: '' }
}

async function cabutRole() {
  await runMutasi(() =>
    api.del(
      `/companies/${companyId.value}/access/users/${cabut.value.user_id}/roles/${encodeURIComponent(cabut.value.role)}`,
      auth.token,
    ),
  )
  if (mutasiError.value) return

  toast.success('Role dicabut.')
  cabut.value = { user_id: '', role: '' }
}

async function salin(teks: string) {
  try {
    await navigator.clipboard.writeText(teks)
    toast.success('Disalin ke papan klip.')
  } catch {
    toast.warning('Tidak bisa menyalin otomatis — catat manual.')
  }
}

onMounted(async () => {
  await access.load(companyId.value, auth.token)
  await Promise.all([muatReferensi(), muat(), muatStatistik()])
})
</script>

<template>
  <div>
    <AppPageHeader
      eyebrow="Holding & pengguna"
      title="Karyawan & penempatan"
      subtitle="Kelola akun karyawan, mutasi cabang, role, dan riwayat penempatan."
      icon="badge"
    >
      <template #actions>
        <template v-if="bolehKelola && tab === 'tempat'">
          <AppButton variant="ghost" @click="bukaTempat()">Tempatkan</AppButton>
          <AppButton @click="bukaTambah">Tambah karyawan</AppButton>
        </template>
      </template>
    </AppPageHeader>

    <AppBanner tone="red" :message="error" />

    <div class="mb-4 grid grid-cols-2 gap-3 lg:grid-cols-4">
      <AppStat
        label="Total penempatan"
        :value="meta?.total ?? 0"
        icon="assignment_ind"
        :loading="loading"
      />
      <AppStat label="Aktif" :value="statistik.aktif" icon="groups" tone="positive" />
      <AppStat label="Selesai / arsip" :value="statistik.arsip" icon="archive" />
      <AppStat label="Cabang terdaftar" :value="cabang.length" icon="storefront" />
    </div>

    <AppCard>
      <AppTabs
        v-model="tab"
        :items="tabs"
        label="Bagian karyawan"
        id-prefix="employee-tab"
        panel-prefix="employee-panel"
      />

      <div
        v-if="tab === 'tempat'"
        id="employee-panel-tempat"
        role="tabpanel"
        aria-labelledby="employee-tab-tempat"
        class="mt-4"
      >
        <div class="mb-3 flex flex-wrap items-end gap-2">
          <AppSearch
            v-model="cari"
            class="min-w-56 flex-1"
            placeholder="Cari nama atau email karyawan..."
            @search="muat"
          />
          <AppSelect
            v-model="filterCabang"
            class="w-52"
            aria-label="Filter cabang"
            placeholder="Semua cabang"
            :options="opsiCabang"
            @update:model-value="muat"
          />
          <AppSelect
            v-model="filterRole"
            class="w-44"
            aria-label="Filter role"
            placeholder="Semua role"
            :options="opsiRoleId"
            @update:model-value="muat"
          />
          <div class="pb-2.5">
            <AppCheckbox
              v-model="hanyaAktif"
              label="Hanya aktif"
              @update:model-value="muat"
            />
          </div>
        </div>

        <AppTable
          caption="Penempatan karyawan"
          :loading="loadingHalaman"
          :empty="!list.length"
          :colspan="bolehKelola ? 7 : 6"
          empty-title="Belum ada penempatan"
          empty-message="Tambahkan karyawan atau ubah filter pencarian."
        >
          <template #head>
            <tr>
              <th>Karyawan</th>
              <th>Cabang</th>
              <th>Role</th>
              <th>Mulai</th>
              <th>Selesai</th>
              <th>Status</th>
              <th v-if="bolehKelola" class="text-right">Aksi</th>
            </tr>
          </template>

          <tr v-for="item in list" :key="item.id">
            <td>
              <div class="flex items-center gap-2.5">
                <AppAvatar
                  :name="namaPengguna(item)"
                  :src="assetUrl(item.userAvatarUrl)"
                  size="sm"
                />
                <div class="min-w-0">
                  <p class="font-semibold text-ink">{{ namaPengguna(item) }}</p>
                  <p class="text-xs text-secondary">{{ item.userEmail ?? `#${item.userId}` }}</p>
                </div>
              </div>
            </td>
            <td>
              <AppBadge v-if="item.branchId === null" tone="green">Akses penuh</AppBadge>
              <template v-else>
                <p class="text-sm">{{ cabangPengguna(item) }}</p>
                <p v-if="item.branchName" class="text-xs text-secondary">{{ item.branchName }}</p>
              </template>
            </td>
            <td class="text-sm">{{ item.roleName ?? '—' }}</td>
            <td class="text-sm">{{ tanggal(item.startedAt) }}</td>
            <td class="text-sm">{{ item.endedAt ? tanggal(item.endedAt) : '—' }}</td>
            <td>
              <AppBadge :tone="item.endedAt ? 'gray' : 'green'">
                {{ item.endedAt ? 'Selesai' : 'Aktif' }}
              </AppBadge>
            </td>
            <td v-if="bolehKelola" class="text-right">
              <div class="flex justify-end gap-1">
                <AppButton v-if="!item.endedAt" size="sm" variant="ghost" @click="bukaTempat(item)">
                  Mutasi
                </AppButton>
                <AppButton size="sm" variant="ghost" @click="bukaReset(item)">
                  Reset sandi
                </AppButton>
                <AppButton v-if="!item.endedAt" size="sm" variant="ghost" @click="bukaAkhiri(item)">
                  Akhiri
                </AppButton>
              </div>
            </td>
          </tr>
        </AppTable>

        <AppPager :meta="meta" @page="setPage" @update:per-page="setPerPage" />
      </div>

      <div
        v-else
        id="employee-panel-role"
        role="tabpanel"
        aria-labelledby="employee-tab-role"
        class="mt-4"
      >
        <div class="grid gap-4 lg:grid-cols-2">
          <div class="rounded-xl border border-outline/70 p-4">
            <h2 class="font-display font-semibold">Beri role</h2>
            <p class="mt-1 text-xs text-secondary">
              Role berlaku untuk perusahaan aktif. Pilih karyawan dan role yang tersedia.
            </p>
            <div class="mt-3 space-y-3">
              <AppField label="Karyawan">
                <AppSelect
                  v-model="beri.user_id"
                  placeholder="Pilih karyawan"
                  :options="opsiKaryawan"
                />
              </AppField>
              <AppField label="Role">
                <AppSelect v-model="beri.role" placeholder="Pilih role" :options="opsiRole" />
              </AppField>
            </div>
            <AppButton
              class="mt-3"
              :loading="mutasiLoading"
              :disabled="!bolehKelola || !beri.user_id || !beri.role"
              @click="beriRole"
            >
              Beri role
            </AppButton>
          </div>

          <div class="rounded-xl border border-outline/70 p-4">
            <h2 class="font-display font-semibold">Cabut role</h2>
            <p class="mt-1 text-xs text-secondary">Akses langsung dicabut setelah role dilepas.</p>
            <div class="mt-3 space-y-3">
              <AppField label="Karyawan">
                <AppSelect
                  v-model="cabut.user_id"
                  placeholder="Pilih karyawan"
                  :options="opsiKaryawan"
                />
              </AppField>
              <AppField label="Role">
                <AppSelect v-model="cabut.role" placeholder="Pilih role" :options="opsiRole" />
              </AppField>
            </div>
            <AppButton
              variant="danger"
              class="mt-3"
              :loading="mutasiLoading"
              :disabled="!bolehKelola || !cabut.user_id || !cabut.role"
              @click="cabutRole"
            >
              Cabut role
            </AppButton>
          </div>
        </div>
        <p v-if="!bolehKelola" class="mt-3 text-sm text-secondary">
          Hanya pemegang izin karyawan.kelola yang bisa mengubah role.
        </p>
      </div>
    </AppCard>

    <AppModal
      :open="tambahOpen"
      title="Tambah karyawan baru"
      description="Akun langsung aktif dengan penempatan hari ini. Kosongkan sandi untuk membuat sandi acak."
      size="lg"
      @close="tambahOpen = false"
    >
      <form id="employee-form" class="space-y-3" @submit.prevent="tambahKaryawan">
        <div class="grid gap-3 sm:grid-cols-2">
          <AppField label="Nama lengkap" required>
            <AppInput v-model="karyawanBaru.name" placeholder="Nama karyawan" />
          </AppField>
          <AppField label="Email" required>
            <AppInput v-model="karyawanBaru.email" type="email" placeholder="nama@email.com" />
          </AppField>
          <AppField label="Nomor HP" optional>
            <AppInput v-model="karyawanBaru.phone" type="tel" placeholder="08xx..." />
          </AppField>
          <AppField label="Status pajak (PTKP)">
            <AppSelect
              v-model="karyawanBaru.ptkp_status"
              :options="PTKP.map((status) => ({ value: status, label: status }))"
            />
          </AppField>
          <AppField label="Cabang">
            <AppSelect
              v-model="karyawanBaru.branch_id"
              placeholder="Semua cabang (akses penuh)"
              :options="opsiCabang"
            />
          </AppField>
          <AppField label="Role">
            <AppSelect
              v-model="karyawanBaru.role_id"
              placeholder="Tanpa role khusus"
              :options="opsiRoleId"
            />
          </AppField>
        </div>
        <AppField
          label="Kata sandi awal"
          optional
          hint="Minimal 8 karakter; kosong berarti sandi acak."
        >
          <AppInput
            v-model="karyawanBaru.password"
            type="password"
            autocomplete="new-password"
            placeholder="Min. 8 karakter"
          />
        </AppField>
      </form>

      <template #footer>
        <AppButton variant="ghost" @click="tambahOpen = false">Batal</AppButton>
        <AppButton
          type="submit"
          form="employee-form"
          :loading="mutasiLoading"
          :disabled="!karyawanBaru.name.trim() || !karyawanBaru.email.trim()"
        >
          Buat akun karyawan
        </AppButton>
      </template>
    </AppModal>

    <AppModal
      :open="tempatOpen"
      title="Tempatkan karyawan"
      description="Simpan penempatan atau mutasi cabang beserta role-nya."
      @close="tempatOpen = false"
    >
      <form id="placement-form" class="space-y-3" @submit.prevent="tempatkan">
        <AppField label="Karyawan" required>
          <AppSelect
            v-model="formTempat.user_id"
            placeholder="Pilih karyawan"
            :options="opsiKaryawan"
          />
        </AppField>
        <AppField label="Cabang">
          <AppSelect
            v-model="formTempat.branch_id"
            placeholder="Semua cabang (akses penuh)"
            :options="opsiCabang"
          />
        </AppField>
        <div class="grid gap-3 sm:grid-cols-2">
          <AppField label="Role" optional>
            <AppSelect
              v-model="formTempat.role_id"
              placeholder="Tanpa role khusus"
              :options="opsiRoleId"
            />
          </AppField>
          <AppField label="Tanggal mulai" required>
            <AppDateInput v-model="formTempat.started_at" />
          </AppField>
        </div>
      </form>

      <template #footer>
        <AppButton variant="ghost" @click="tempatOpen = false">Batal</AppButton>
        <AppButton
          type="submit"
          form="placement-form"
          :loading="mutasiLoading"
          :disabled="!formTempat.user_id || !formTempat.started_at"
        >
          Simpan penempatan
        </AppButton>
      </template>
    </AppModal>

    <AppModal
      :open="!!kredensial"
      title="Kredensial awal"
      description="Hanya ditampilkan sekali. Catat atau salin sekarang."
      size="sm"
      @close="kredensial = null"
    >
      <p v-if="kredensial" class="rounded-lg bg-surface-low p-3 font-mono text-sm">
        {{ kredensial.email }} / {{ kredensial.password }}
      </p>
      <template #footer>
        <AppButton
          v-if="kredensial"
          variant="ghost"
          @click="salin(kredensial.email + ' / ' + kredensial.password)"
        >
          Salin
        </AppButton>
        <AppButton @click="kredensial = null">Tutup</AppButton>
      </template>
    </AppModal>

    <AppModal
      :open="!!resetTarget"
      :title="`Reset sandi — ${resetTarget?.name ?? ''}`"
      description="Semua sesi karyawan dicabut dan wajib masuk ulang dengan sandi baru."
      size="sm"
      @close="resetTarget = null"
    >
      <AppField label="Kata sandi baru" optional hint="Kosongkan untuk membuat sandi acak.">
        <AppInput
          v-model="resetPw"
          type="password"
          autocomplete="new-password"
          placeholder="Kata sandi baru"
        />
      </AppField>
      <template v-if="resetHasil">
        <p class="mt-3 rounded-lg bg-surface-low p-3 font-mono text-sm">{{ resetHasil }}</p>
        <p class="mt-1 text-xs text-secondary">Catat sekarang — hanya ditampilkan sekali.</p>
      </template>
      <template #footer>
        <AppButton variant="ghost" @click="resetTarget = null">Tutup</AppButton>
        <AppButton v-if="!resetHasil" :loading="mutasiLoading" @click="resetSandi">
          Reset
        </AppButton>
        <AppButton v-else variant="ghost" @click="salin(resetHasil)">Salin sandi</AppButton>
      </template>
    </AppModal>

    <AppModal
      :open="akhiriId !== null"
      title="Akhiri penempatan?"
      description="Riwayat penempatan tetap tersimpan setelah diakhiri."
      size="sm"
      @close="akhiriId = null"
    >
      <form id="end-placement-form" @submit.prevent="tutupPenempatan">
        <AppField label="Tanggal selesai" required>
          <AppDateInput v-model="akhirTanggal" />
        </AppField>
      </form>
      <template #footer>
        <AppButton variant="ghost" @click="akhiriId = null">Batal</AppButton>
        <AppButton
          type="submit"
          form="end-placement-form"
          :loading="mutasiLoading"
          :disabled="!akhirTanggal"
        >
          Akhiri
        </AppButton>
      </template>
    </AppModal>
  </div>
</template>
