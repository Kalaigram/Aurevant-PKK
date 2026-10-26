<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { api, assetUrl } from '../lib/api'
import { useCompany } from '../composables/useCompany'
import { useAccessStore } from '../stores/access'
import { useAsync } from '../composables/useAsync'
import { useConfirm } from '../composables/useConfirm'
import { useToast } from '../composables/useToast'
import AppAvatar from '../components/ui/AppAvatar.vue'
import AppBanner from '../components/ui/AppBanner.vue'
import AppButton from '../components/ui/AppButton.vue'
import AppCard from '../components/ui/AppCard.vue'
import AppField from '../components/ui/AppField.vue'
import AppIcon from '../components/ui/AppIcon.vue'
import AppInput from '../components/ui/AppInput.vue'
import AppPageHeader from '../components/ui/AppPageHeader.vue'
import AppSkeleton from '../components/ui/AppSkeleton.vue'
import AppTextarea from '../components/ui/AppTextarea.vue'

interface CompanyProfile {
  id: number
  name: string
  legalName: string | null
  taxId: string | null
  email: string | null
  phone: string | null
  address: string | null
  logoPath: string | null
  logoUrl: string | null
}

const { auth, companyId } = useCompany()
const access = useAccessStore()
const toast = useToast()
const { confirm } = useConfirm()
const { loading, error, run } = useAsync()
const { loading: simpanLoading, error: simpanError, run: runSimpan } = useAsync()
const { loading: logoLoading, error: logoError, run: runLogo } = useAsync()

const profil = ref<CompanyProfile | null>(null)
const berkas = ref<HTMLInputElement | null>(null)
const form = ref({
  name: '',
  legal_name: '',
  tax_id: '',
  email: '',
  phone: '',
  address: '',
})

const bolehKelola = computed(() => access.can('pengaturan.kelola'))

onMounted(async () => {
  const data = await run(() =>
    api.get<CompanyProfile>(`/companies/${companyId.value}/profile`, auth.token),
  )
  if (data) isiForm(data)
})

function isiForm(data: CompanyProfile) {
  profil.value = data
  form.value = {
    name: data.name,
    legal_name: data.legalName ?? '',
    tax_id: data.taxId ?? '',
    email: data.email ?? '',
    phone: data.phone ?? '',
    address: data.address ?? '',
  }
}

function simpan() {
  return runSimpan(async () => {
    const data = await api.put<CompanyProfile>(
      `/companies/${companyId.value}/profile`,
      {
        name: form.value.name.trim(),
        legal_name: form.value.legal_name.trim() || null,
        tax_id: form.value.tax_id.trim() || null,
        email: form.value.email.trim() || null,
        phone: form.value.phone.trim() || null,
        address: form.value.address.trim() || null,
      },
      auth.token,
    )
    isiForm(data)
    auth.setCompanyIdentity(data.name, data.logoUrl)
    toast.success('Profil perusahaan disimpan.')
  })
}

async function pilihLogo(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return
  if (!['image/jpeg', 'image/png', 'image/webp'].includes(file.type)) {
    toast.error('Pilih logo JPG, PNG, atau WEBP.')
    return
  }
  if (file.size > 2 * 1024 * 1024) {
    toast.error('Ukuran logo maksimal 2 MB.')
    return
  }

  const body = new FormData()
  body.append('logo', file)
  const data = await runLogo(() =>
    api.postForm<CompanyProfile>(`/companies/${companyId.value}/logo`, body, auth.token),
  )
  if (!data) return
  isiForm(data)
  auth.setCompanyIdentity(data.name, data.logoUrl)
  toast.success('Logo perusahaan diperbarui.')
}

async function hapusLogo() {
  const setuju = await confirm({
    title: 'Hapus logo perusahaan?',
    message: 'Logo akan diganti dengan ikon perusahaan default.',
    confirmLabel: 'Hapus logo',
    tone: 'danger',
  })
  if (!setuju) return
  const data = await runLogo(() =>
    api.del<CompanyProfile>(`/companies/${companyId.value}/logo`, auth.token),
  )
  if (!data) return
  isiForm(data)
  auth.setCompanyIdentity(data.name, data.logoUrl)
  toast.success('Logo perusahaan dihapus.')
}
</script>

<template>
  <div class="mx-auto max-w-3xl">
    <AppPageHeader
      eyebrow="Identitas usaha"
      title="Profil perusahaan"
      subtitle="Nama, kontak, dan logo yang tampil di aplikasi serta kartu karyawan."
      icon="storefront"
    />

    <AppBanner tone="red" :message="error" />

    <div v-if="loading && !profil" class="grid gap-3" aria-busy="true">
      <AppCard><AppSkeleton :lines="3" /></AppCard>
      <AppCard><AppSkeleton :lines="6" /></AppCard>
      <span class="sr-only" role="status">Memuat profil perusahaan…</span>
    </div>

    <template v-else-if="profil">
      <AppCard title="Logo perusahaan" subtitle="Gunakan gambar persegi agar tampil rapi.">
        <div class="flex flex-col gap-4 sm:flex-row sm:items-center">
          <AppAvatar
            :name="profil.name"
            :src="assetUrl(profil.logoUrl)"
            size="xl"
            square
            class="ring-outline"
            fallback-icon="storefront"
          />
          <div class="min-w-0 flex-1">
            <p class="text-sm font-semibold text-ink">{{ profil.name }}</p>
            <p class="mt-0.5 text-xs text-secondary">
              JPG, PNG, atau WEBP. Maksimal 2 MB. Logo lama otomatis diganti.
            </p>
            <input
              ref="berkas"
              type="file"
              accept="image/jpeg,image/png,image/webp"
              class="hidden"
              @change="pilihLogo"
            />
            <div class="mt-3 flex flex-wrap gap-2">
              <AppButton
                size="sm"
                variant="secondary"
                :loading="logoLoading"
                :disabled="!bolehKelola"
                @click="berkas?.click()"
              >
                <AppIcon name="upload" :size="16" />Pilih logo
              </AppButton>
              <AppButton
                v-if="profil.logoUrl"
                size="sm"
                variant="ghost"
                :loading="logoLoading"
                :disabled="!bolehKelola"
                @click="hapusLogo"
              >
                <AppIcon name="delete" :size="16" />Hapus logo
              </AppButton>
            </div>
          </div>
        </div>
        <AppBanner tone="red" :message="logoError" />
      </AppCard>

      <AppCard title="Identitas & kontak" class="mt-3">
        <AppBanner tone="red" :message="simpanError" />
        <div class="grid gap-3 sm:grid-cols-2">
          <AppField label="Nama perusahaan" required hint="Nama yang tampil di aplikasi.">
            <AppInput v-model="form.name" :disabled="!bolehKelola" />
          </AppField>
          <AppField label="Nama badan usaha" optional>
            <AppInput v-model="form.legal_name" :disabled="!bolehKelola" placeholder="Contoh: PT Surya Jaya" />
          </AppField>
          <AppField label="NPWP" optional>
            <AppInput v-model="form.tax_id" :disabled="!bolehKelola" placeholder="00.000.000.0-000.000" />
          </AppField>
          <AppField label="Email perusahaan" optional>
            <AppInput v-model="form.email" type="email" :disabled="!bolehKelola" placeholder="halo@perusahaan.id" />
          </AppField>
          <AppField label="Nomor telepon" optional>
            <AppInput v-model="form.phone" type="tel" :disabled="!bolehKelola" placeholder="021xxxxxxx" />
          </AppField>
          <AppField label="Alamat" optional class="sm:col-span-2">
            <AppTextarea v-model="form.address" :rows="3" :disabled="!bolehKelola" placeholder="Alamat kantor pusat" />
          </AppField>
        </div>
        <div class="mt-4 flex items-center justify-between gap-3">
          <p class="text-xs text-secondary">
            Data ini juga dipakai pada kartu karyawan.
          </p>
          <AppButton
            :loading="simpanLoading"
            :disabled="!bolehKelola || !form.name.trim()"
            @click="simpan"
          >
            <AppIcon name="save" :size="18" />Simpan profil
          </AppButton>
        </div>
        <p v-if="!bolehKelola" class="mt-3 text-xs text-secondary">
          Hanya pemilik atau pengelola pengaturan yang dapat mengubah profil perusahaan.
        </p>
      </AppCard>
    </template>
  </div>
</template>
