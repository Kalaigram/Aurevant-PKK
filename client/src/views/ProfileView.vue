<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { api, assetUrl } from '../lib/api'
import type { AuthUser } from '../stores/auth'
import { useCompany } from '../composables/useCompany'
import { useAsync } from '../composables/useAsync'
import { useConfirm } from '../composables/useConfirm'
import { useToast } from '../composables/useToast'
import AppAvatar from '../components/ui/AppAvatar.vue'
import AppBadge from '../components/ui/AppBadge.vue'
import AppBanner from '../components/ui/AppBanner.vue'
import AppButton from '../components/ui/AppButton.vue'
import AppCard from '../components/ui/AppCard.vue'
import AppField from '../components/ui/AppField.vue'
import AppIcon from '../components/ui/AppIcon.vue'
import AppInput from '../components/ui/AppInput.vue'
import AppModal from '../components/ui/AppModal.vue'
import AppPageHeader from '../components/ui/AppPageHeader.vue'
import AppSkeleton from '../components/ui/AppSkeleton.vue'

interface Card {
  name: string
  email: string
  phone: string | null
  employeeUuid: string
  company: string
  branch: string | null
  role: string | null
}

interface Sesi {
  id: number
  name: string
  createdAt: string | null
  lastUsedAt: string | null
  isCurrent: boolean
}

interface Status2fa {
  enabled: boolean
  pending: boolean
}

interface Setup2fa {
  secret: string
  otpauthUri: string
  recoveryCodes: string[]
}

const { auth, companyId } = useCompany()
const toast = useToast()
const { confirm } = useConfirm()
const { loading, error, run } = useAsync()
const { loading: profilLoading, error: profilError, run: runProfil } = useAsync()
const { loading: fotoLoading, error: fotoError, run: runFoto } = useAsync()
const { loading: pwLoading, error: pwError, run: runPw } = useAsync()
const { loading: sesiLoading, error: sesiError, run: runSesi } = useAsync()
const { error: sesiAksiError, run: runSesiAksi } = useAsync()
const { loading: tfLoading, error: tfError, run: runTf } = useAsync()
const card = ref<Card | null>(null)
const tanyaKeluar = ref(false)
const berkas = ref<HTMLInputElement | null>(null)
const ganti = ref({ current_password: '', password: '', password_confirmation: '' })
const profil = ref({
  name: auth.user?.name ?? '',
  phone: auth.user?.phone ?? '',
})
const okPw = ref('')
const sesi = ref<Sesi[]>([])
const tf = ref<Status2fa>({ enabled: false, pending: false })
const tfSetup = ref<Setup2fa | null>(null)
const tfCode = ref('')
const tfPassword = ref('')
const tfOk = ref('')

onMounted(async () => {
  if (!auth.user) return
  void muatSesi()
  void muat2fa()
  card.value = await run(() =>
    api.get<Card>(`/companies/${companyId.value}/users/${auth.user!.id}/card`, auth.token),
  )
})

function waktu(nilai: string | null) {
  return nilai
    ? new Date(nilai).toLocaleString('id-ID', { dateStyle: 'medium', timeStyle: 'short' })
    : '–'
}

async function muatSesi() {
  const data = await runSesi(() => api.get<Sesi[]>('/auth/sessions', auth.token))
  if (data) sesi.value = data
}

async function akhiriSesi(item: Sesi) {
  const setuju = await confirm({
    title: 'Akhiri sesi ini?',
    message: `Perangkat "${item.name}" akan dikeluarkan dan harus masuk ulang.`,
    confirmLabel: 'Akhiri sesi',
    tone: 'danger',
  })
  if (!setuju) return
  const ok = await runSesiAksi(() => api.del(`/auth/sessions/${item.id}`, auth.token))
  if (ok === null) return
  toast.success('Sesi perangkat diakhiri.')
  await muatSesi()
}

async function akhiriSesiLain() {
  const setuju = await confirm({
    title: 'Akhiri semua sesi lain?',
    message: 'Semua perangkat selain perangkat ini akan dikeluarkan.',
    confirmLabel: 'Akhiri semua',
    tone: 'danger',
  })
  if (!setuju) return
  const ok = await runSesiAksi(() => api.del('/auth/sessions', auth.token))
  if (ok === null) return
  toast.success('Semua sesi lain diakhiri.')
  await muatSesi()
}

async function muat2fa() {
  const data = await runTf(() => api.get<Status2fa>('/auth/two-factor', auth.token))
  if (data) tf.value = data
}

async function mulai2fa() {
  tfOk.value = ''
  const data = await runTf(() =>
    api.post<Setup2fa>('/auth/two-factor/setup', undefined, auth.token),
  )
  if (!data) return
  tfSetup.value = data
  tf.value = { enabled: false, pending: true }
}

async function aktifkan2fa() {
  tfOk.value = ''
  const ok = await runTf(() =>
    api.post('/auth/two-factor/enable', { code: tfCode.value.trim() }, auth.token),
  )
  if (ok === null) return
  tf.value = { enabled: true, pending: false }
  tfSetup.value = null
  tfCode.value = ''
  tfOk.value = '2FA aktif. Simpan kode pemulihan Anda di tempat aman.'
  toast.success('Verifikasi dua langkah aktif.')
}

async function matikan2fa() {
  const setuju = await confirm({
    title: 'Nonaktifkan 2FA?',
    message: 'Akun Anda tidak lagi meminta kode saat masuk dari perangkat baru.',
    confirmLabel: 'Nonaktifkan',
    tone: 'danger',
  })
  if (!setuju) return
  tfOk.value = ''
  const ok = await runTf(() =>
    api.post('/auth/two-factor/disable', { password: tfPassword.value }, auth.token),
  )
  if (ok === null) return
  tf.value = { enabled: false, pending: false }
  tfPassword.value = ''
  tfOk.value = '2FA dinonaktifkan.'
  toast.success('Verifikasi dua langkah nonaktif.')
}

async function simpanProfil() {
  const data = await runProfil(() =>
    api.put<AuthUser>(
      '/auth/profile',
      { name: profil.value.name.trim(), phone: profil.value.phone.trim() || null },
      auth.token,
    ),
  )
  if (!data) return
  auth.user = data
  auth.persist()
  profil.value = { name: data.name, phone: data.phone ?? '' }
  toast.success('Data diri berhasil disimpan.')
}

async function pilihFoto(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return
  if (!['image/jpeg', 'image/png', 'image/webp'].includes(file.type)) {
    toast.error('Pilih foto JPG, PNG, atau WEBP.')
    return
  }
  if (file.size > 2 * 1024 * 1024) {
    toast.error('Ukuran foto maksimal 2 MB.')
    return
  }

  const form = new FormData()
  form.append('avatar', file)
  const data = await runFoto(() => api.postForm<AuthUser>('/auth/avatar', form, auth.token))
  if (!data) return
  auth.user = data
  auth.persist()
  toast.success('Foto profil diperbarui.')
}

async function hapusFoto() {
  const setuju = await confirm({
    title: 'Hapus foto profil?',
    message: 'Foto akan diganti dengan ikon profil default.',
    confirmLabel: 'Hapus foto',
    tone: 'danger',
  })
  if (!setuju) return
  const data = await runFoto(() => api.del<AuthUser>('/auth/avatar', auth.token))
  if (!data) return
  auth.user = data
  auth.persist()
  toast.success('Foto profil dihapus.')
}

async function gantiSandi() {
  okPw.value = ''
  await runPw(() =>
    api.post('/auth/change-password', {
      current_password: ganti.value.current_password,
      password: ganti.value.password,
      password_confirmation: ganti.value.password_confirmation,
    }, auth.token),
  )
  if (!pwError.value) {
    okPw.value = 'Kata sandi berhasil diganti. Sesi di perangkat lain telah dikeluarkan.'
    ganti.value = { current_password: '', password: '', password_confirmation: '' }
  }
}

async function keluar() {
  await auth.logout()
  window.location.href = '/login'
}
</script>

<template>
  <div class="mx-auto max-w-2xl">
    <AppPageHeader
      eyebrow="Identitas & sesi resmi"
      title="Profil pengguna & keamanan akun"
      subtitle="Kelola data diri, foto profil, dan sesi perangkat Anda."
      icon="account_circle"
    />

    <AppBanner tone="red" :message="error" />

    <AppCard>
      <div class="flex flex-col gap-4 sm:flex-row sm:items-center">
        <AppAvatar
          :name="auth.user?.name"
          :src="assetUrl(auth.user?.avatarUrl)"
          size="xl"
        />
        <div class="min-w-0 flex-1">
          <div class="flex flex-wrap items-start justify-between gap-2">
            <div>
              <h2 class="font-display text-lg font-semibold">{{ auth.user?.name }}</h2>
              <p class="text-sm text-secondary">
                {{ card?.company ?? auth.companyName }}
                <span v-if="card?.branch"> · {{ card.branch }}</span>
              </p>
            </div>
            <AppBadge v-if="card?.role" tone="blue">{{ card.role }}</AppBadge>
          </div>
          <div class="mt-3 flex flex-wrap gap-2">
            <input
              ref="berkas"
              type="file"
              accept="image/jpeg,image/png,image/webp"
              class="hidden"
              @change="pilihFoto"
            />
            <AppButton size="sm" variant="secondary" :loading="fotoLoading" @click="berkas?.click()">
              <AppIcon name="add_a_photo" :size="16" />Ganti foto
            </AppButton>
            <AppButton
              v-if="auth.user?.avatarUrl"
              size="sm"
              variant="ghost"
              :loading="fotoLoading"
              @click="hapusFoto"
            >
              <AppIcon name="delete" :size="16" />Hapus foto
            </AppButton>
          </div>
          <p class="mt-2 text-xs text-secondary">Foto JPG, PNG, atau WEBP maksimal 2 MB.</p>
        </div>
      </div>
      <AppBanner tone="red" :message="fotoError" />
      <dl v-if="loading" class="mt-5 grid gap-3 text-sm sm:grid-cols-2" aria-busy="true">
        <div v-for="i in 4" :key="i">
          <AppSkeleton width="45%" height="0.6rem" />
          <AppSkeleton class="mt-2" width="75%" />
        </div>
        <span class="sr-only" role="status">Memuat data keanggotaan…</span>
      </dl>
      <dl v-else class="mt-5 grid gap-3 text-sm sm:grid-cols-2">
        <div>
          <dt class="text-2xs font-semibold uppercase tracking-[0.08em] text-secondary">Email resmi</dt>
          <dd class="mt-0.5 font-semibold text-ink">{{ auth.user?.email }}</dd>
        </div>
        <div>
          <dt class="text-2xs font-semibold uppercase tracking-[0.08em] text-secondary">Nomor HP</dt>
          <dd class="mt-0.5 font-semibold text-ink">{{ auth.user?.phone ?? '–' }}</dd>
        </div>
        <div>
          <dt class="text-2xs font-semibold uppercase tracking-[0.08em] text-secondary">ID karyawan</dt>
          <dd class="mt-0.5 font-mono text-xs font-semibold text-ink">{{ auth.user?.employeeUuid ?? '–' }}</dd>
        </div>
        <div>
          <dt class="text-2xs font-semibold uppercase tracking-[0.08em] text-secondary">Status PTKP</dt>
          <dd class="mt-0.5 font-semibold text-ink">{{ auth.user?.ptkpStatus ?? '–' }}</dd>
        </div>
      </dl>
    </AppCard>

    <AppCard title="Data diri" class="mt-3">
      <p class="-mt-1 mb-3 text-xs text-secondary">
        Nama tampil di kartu karyawan dan riwayat aktivitas. Nomor HP dipakai untuk masuk.
      </p>
      <AppBanner tone="red" :message="profilError" />
      <div class="grid gap-3 sm:grid-cols-2">
        <AppField label="Nama lengkap" required>
          <AppInput v-model="profil.name" autocomplete="name" />
        </AppField>
        <AppField label="Nomor HP" optional>
          <AppInput v-model="profil.phone" type="tel" autocomplete="tel" placeholder="08xxxxxxxxxx" />
        </AppField>
      </div>
      <AppButton class="mt-3" :loading="profilLoading" :disabled="!profil.name.trim()" @click="simpanProfil">
        <AppIcon name="save" :size="18" />Simpan data diri
      </AppButton>
    </AppCard>

    <AppCard title="Hak akses & organisasi" class="mt-3">
      <dl class="space-y-3 text-sm">
        <div>
          <dt class="text-xs uppercase tracking-wider text-secondary">Perusahaan</dt>
          <dd class="font-semibold">{{ card?.company ?? auth.companyName }}</dd>
        </div>
        <div>
          <dt class="text-xs uppercase tracking-wider text-secondary">Peran operasional</dt>
          <dd class="font-semibold">{{ card?.role ?? '–' }}</dd>
        </div>
      </dl>
      <p class="mt-3 text-xs text-secondary">Hak akses lengkap diatur oleh pemilik perusahaan.</p>
    </AppCard>

    <AppCard title="Sesi perangkat & keamanan" class="mt-3">
      <p class="text-sm text-secondary">
        Daftar perangkat yang masih masuk. Satu akun terikat satu perangkat untuk mencegah titip absen.
      </p>
      <AppBanner tone="red" :message="sesiError || sesiAksiError" />
      <div v-if="sesiLoading" class="mt-3 space-y-2" aria-busy="true">
        <AppSkeleton v-for="i in 2" :key="i" height="2.5rem" />
      </div>
      <ul v-else class="mt-3 divide-y divide-outline/60">
        <li
          v-for="item in sesi"
          :key="item.id"
          class="flex flex-wrap items-center justify-between gap-2 py-2.5"
        >
          <div class="min-w-0">
            <p class="flex flex-wrap items-center gap-2 text-sm font-semibold text-ink">
              {{ item.name }}
              <AppBadge v-if="item.isCurrent" tone="green">Perangkat ini</AppBadge>
            </p>
            <p class="mt-0.5 text-xs text-secondary">
              Masuk {{ waktu(item.createdAt) }} · Terakhir aktif {{ waktu(item.lastUsedAt) }}
            </p>
          </div>
          <AppButton
            v-if="!item.isCurrent"
            size="sm"
            variant="ghost"
            @click="akhiriSesi(item)"
          >
            Akhiri
          </AppButton>
        </li>
      </ul>
      <p v-if="!sesiLoading && sesi.length === 0" class="mt-3 text-sm text-secondary">
        Tidak ada sesi aktif.
      </p>
      <div class="mt-3 flex flex-wrap gap-2">
        <AppButton size="sm" variant="secondary" :loading="sesiLoading" @click="muatSesi">
          <AppIcon name="refresh" :size="16" />Muat ulang
        </AppButton>
        <AppButton
          v-if="sesi.some((item) => !item.isCurrent)"
          size="sm"
          variant="danger"
          @click="akhiriSesiLain"
        >
          <AppIcon name="devices_off" :size="16" />Akhiri semua sesi lain
        </AppButton>
      </div>
      <AppButton variant="danger" class="mt-3" @click="tanyaKeluar = true">
        <AppIcon name="logout" :size="18" />Keluar dari sesi ini
      </AppButton>
    </AppCard>

    <AppCard title="Verifikasi dua langkah" class="mt-3">
      <div class="flex flex-wrap items-start justify-between gap-2">
        <p class="text-sm text-secondary">
          Minta kode dari aplikasi authenticator setiap kali masuk dari perangkat baru.
        </p>
        <AppBadge :tone="tf.enabled ? 'green' : 'gray'">
          {{ tf.enabled ? 'Aktif' : tf.pending ? 'Menunggu verifikasi' : 'Nonaktif' }}
        </AppBadge>
      </div>
      <AppBanner tone="green" :message="tfOk" />
      <AppBanner tone="red" :message="tfError" />

      <template v-if="!tf.enabled">
        <AppButton
          v-if="!tfSetup"
          class="mt-3"
          :loading="tfLoading"
          @click="mulai2fa"
        >
          <AppIcon name="shield_lock" :size="18" />Aktifkan 2FA
        </AppButton>
        <div v-else class="mt-3 space-y-3 rounded-xl border border-outline/70 bg-surface-low/60 p-3">
          <div>
            <p class="text-xs font-semibold uppercase tracking-wider text-secondary">Kunci manual</p>
            <code class="mt-1 block break-all rounded bg-card px-2 py-1.5 font-mono text-xs text-ink">
              {{ tfSetup.secret }}
            </code>
          </div>
          <p class="text-xs leading-relaxed text-secondary">
            Tambahkan akun di aplikasi authenticator memakai kunci di atas, atau buka tautan
            otpauth berikut di perangkat yang mendukung.
          </p>
          <a
            :href="tfSetup.otpauthUri"
            class="block break-all rounded bg-card px-2 py-1.5 font-mono text-2xs text-primary underline"
          >
            {{ tfSetup.otpauthUri }}
          </a>
          <div>
            <p class="text-xs font-semibold uppercase tracking-wider text-secondary">
              Kode pemulihan
            </p>
            <div class="mt-1 grid grid-cols-2 gap-1.5 sm:grid-cols-4">
              <code
                v-for="kode in tfSetup.recoveryCodes"
                :key="kode"
                class="rounded bg-card px-2 py-1 font-mono text-2xs text-ink"
              >
                {{ kode }}
              </code>
            </div>
            <p class="mt-1.5 text-2xs text-secondary">
              Simpan kode ini sekarang. Setiap kode hanya bisa dipakai sekali.
            </p>
          </div>
          <AppField label="Kode 6 digit dari authenticator" for="tf-code" required>
            <AppInput
              id="tf-code"
              v-model="tfCode"
              inputmode="numeric"
              autocomplete="one-time-code"
              maxlength="10"
              placeholder="123456"
            />
          </AppField>
          <AppButton
            :loading="tfLoading"
            :disabled="!tfCode.trim()"
            @click="aktifkan2fa"
          >
            <AppIcon name="verified_user" :size="18" />Verifikasi & aktifkan
          </AppButton>
        </div>
      </template>

      <template v-else>
        <AppField
          label="Kata sandi"
          for="tf-pw"
          hint="Konfirmasi kata sandi untuk menonaktifkan 2FA."
          class="mt-3"
        >
          <AppInput
            id="tf-pw"
            v-model="tfPassword"
            type="password"
            autocomplete="current-password"
          />
        </AppField>
        <AppButton
          variant="danger"
          class="mt-3"
          :loading="tfLoading"
          :disabled="!tfPassword"
          @click="matikan2fa"
        >
          <AppIcon name="shield" :size="18" />Nonaktifkan 2FA
        </AppButton>
      </template>
    </AppCard>

    <AppCard title="Ganti kata sandi" class="mt-3">
      <p class="-mt-1 mb-3 text-xs text-secondary">
        Sesi di perangkat lain akan otomatis dikeluarkan setelah kata sandi diganti.
      </p>
      <AppBanner tone="green" :message="okPw" />
      <AppBanner tone="red" :message="pwError" />
      <div class="grid gap-3 md:grid-cols-3">
        <AppField label="Kata sandi saat ini">
          <AppInput
            v-model="ganti.current_password"
            type="password"
            autocomplete="current-password"
          />
        </AppField>
        <AppField label="Kata sandi baru" hint="Minimal 8 karakter.">
          <AppInput
            v-model="ganti.password"
            type="password"
            autocomplete="new-password"
            placeholder="Min. 8 karakter"
          />
        </AppField>
        <AppField label="Ulangi kata sandi baru">
          <AppInput
            v-model="ganti.password_confirmation"
            type="password"
            autocomplete="new-password"
          />
        </AppField>
      </div>
      <AppButton
        :loading="pwLoading"
        :disabled="!ganti.current_password || !ganti.password || !ganti.password_confirmation"
        class="mt-3"
        @click="gantiSandi"
      >
        <AppIcon name="lock_reset" :size="18" />Simpan kata sandi
      </AppButton>
    </AppCard>

    <AppCard title="Tentang aplikasi" class="mt-3">
      <dl class="space-y-2 text-sm">
        <div class="flex justify-between"><dt class="text-secondary">Platform</dt><dd class="font-semibold">Aurevant PKK</dd></div>
        <div class="flex justify-between"><dt class="text-secondary">Status API</dt><dd class="font-semibold text-primary">Terhubung</dd></div>
      </dl>
    </AppCard>

    <AppModal
      :open="tanyaKeluar"
      title="Keluar dari aplikasi?"
      description="Sesi di perangkat ini akan diakhiri. Anda perlu masuk lagi untuk melanjutkan."
      size="sm"
      @close="tanyaKeluar = false"
    >
      <template #footer>
        <AppButton variant="ghost" @click="tanyaKeluar = false">Batal</AppButton>
        <AppButton variant="danger" @click="keluar">
          <AppIcon name="logout" :size="18" />Ya, keluar
        </AppButton>
      </template>
    </AppModal>
  </div>
</template>
