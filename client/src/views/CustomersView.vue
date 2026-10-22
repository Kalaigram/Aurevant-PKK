<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { api, downloadFile } from '../lib/api'
import { useCompany } from '../composables/useCompany'
import { useAccessStore } from '../stores/access'
import { useAsync } from '../composables/useAsync'
import { usePagedList } from '../composables/usePagedList'
import { useConfirm } from '../composables/useConfirm'
import { useToast } from '../composables/useToast'
import AppBanner from '../components/ui/AppBanner.vue'
import AppButton from '../components/ui/AppButton.vue'
import AppCard from '../components/ui/AppCard.vue'
import AppField from '../components/ui/AppField.vue'
import AppInput from '../components/ui/AppInput.vue'
import AppModal from '../components/ui/AppModal.vue'
import AppPageHeader from '../components/ui/AppPageHeader.vue'
import AppPager from '../components/ui/AppPager.vue'
import AppSearch from '../components/ui/AppSearch.vue'
import AppTable from '../components/ui/AppTable.vue'
import AppTextarea from '../components/ui/AppTextarea.vue'

interface Pelanggan {
  id: number
  name: string
  phone: string | null
  address: string | null
  notes: string | null
}

interface FormPelanggan {
  name: string
  phone: string
  address: string
  notes: string
}

const { auth, companyId } = useCompany()
const access = useAccessStore()
const toast = useToast()
const { confirm } = useConfirm()
const eksporAsync = useAsync()
const mutasi = useAsync()

const cari = ref('')
const formOpen = ref(false)
const sunting = ref<Pelanggan | null>(null)
const form = ref<FormPelanggan>(formKosong())

const {
  rows: list,
  meta,
  loading,
  error: errorDaftar,
  load,
  setPerPage,
} = usePagedList<Pelanggan>((page, perPage) => {
  const params = new URLSearchParams({ per_page: String(perPage), page: String(page) })
  if (cari.value.trim()) params.set('q', cari.value.trim())
  return api.getPage<Pelanggan>(`/companies/${companyId.value}/customers?${params}`, auth.token)
})

const bolehKelola = computed(() => access.can('pelanggan.kelola'))
const bolehEkspor = computed(() => access.can('pelanggan.ekspor'))
const error = computed(() => errorDaftar.value || eksporAsync.error.value)

function formKosong(): FormPelanggan {
  return { name: '', phone: '', address: '', notes: '' }
}

async function ekspor() {
  await eksporAsync.run(() =>
    downloadFile(
      `/companies/${companyId.value}/exports/customers`,
      'pelanggan.csv',
      auth.token,
      'text/csv',
    ),
  )
  if (eksporAsync.error.value) {
    toast.error(eksporAsync.error.value)
    return
  }
  toast.success('Data pelanggan diunduh.')
}

function bukaTambah() {
  mutasi.error.value = ''
  sunting.value = null
  form.value = formKosong()
  formOpen.value = true
}

function bukaUbah(p: Pelanggan) {
  mutasi.error.value = ''
  sunting.value = p
  form.value = {
    name: p.name,
    phone: p.phone ?? '',
    address: p.address ?? '',
    notes: p.notes ?? '',
  }
  formOpen.value = true
}

async function simpan() {
  const f = form.value
  if (!f.name.trim()) return
  const body = {
    name: f.name.trim(),
    phone: f.phone.trim() || null,
    address: f.address.trim() || null,
    notes: f.notes.trim() || null,
  }
  const base = `/companies/${companyId.value}/customers`
  await mutasi.run(() =>
    sunting.value
      ? api.put(base + `/${sunting.value.id}`, body, auth.token)
      : api.post(base, body, auth.token),
  )
  if (mutasi.error.value) {
    toast.error(mutasi.error.value)
    return
  }
  formOpen.value = false
  toast.success(sunting.value ? 'Pelanggan diperbarui.' : 'Pelanggan ditambahkan.')
  await load()
}

async function hapus(p: Pelanggan) {
  const setuju = await confirm({
    title: 'Hapus pelanggan?',
    message: `${p.name} akan dihapus. Pelanggan yang sudah punya transaksi tidak bisa dihapus.`,
    confirmLabel: 'Hapus',
    tone: 'danger',
  })
  if (!setuju) return
  await mutasi.run(() => api.del(`/companies/${companyId.value}/customers/${p.id}`, auth.token))
  if (mutasi.error.value) {
    toast.error(mutasi.error.value)
    return
  }
  toast.success('Pelanggan dihapus.')
  await load()
}

onMounted(async () => {
  await access.load(companyId.value, auth.token)
  await load(1)
})
</script>

<template>
  <div>
    <AppPageHeader
      eyebrow="Pelanggan"
      title="Pelanggan"
      subtitle="Simpan kontak pelanggan agar transaksi dan order jasa lebih cepat."
      icon="group"
    >
      <template #actions>
        <AppButton v-if="bolehEkspor" variant="ghost" :loading="eksporAsync.loading.value" @click="ekspor">
          Ekspor CSV
        </AppButton>
        <AppButton v-if="bolehKelola" @click="bukaTambah">Pelanggan baru</AppButton>
      </template>
    </AppPageHeader>

    <AppBanner tone="red" :message="error" />

    <AppCard>
      <div class="mb-3 flex flex-wrap items-end gap-2">
        <AppSearch
          v-model="cari"
          class="min-w-56 flex-1"
          placeholder="Cari nama atau nomor telepon..."
          @search="load(1)"
        />
      </div>

      <AppTable
        :loading="loading"
        :empty="!list.length"
        :colspan="5"
        empty-title="Belum ada pelanggan"
        empty-message="Tambahkan pelanggan atau ubah kata kunci pencarian."
      >
        <template #head>
          <tr>
            <th>Pelanggan</th>
            <th>Telepon</th>
            <th class="hidden md:table-cell">Alamat</th>
            <th class="hidden lg:table-cell">Catatan</th>
            <th v-if="bolehKelola" class="text-right">Aksi</th>
          </tr>
        </template>
        <tr v-for="p in list" :key="p.id">
          <td>
            <div class="flex items-center gap-2.5">
              <span
                class="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-primary-soft text-xs font-bold text-primary"
              >
                {{ p.name.charAt(0).toUpperCase() }}
              </span>
              <span class="font-medium text-ink">{{ p.name }}</span>
            </div>
          </td>
          <td class="font-mono text-xs text-secondary">{{ p.phone ?? '—' }}</td>
          <td class="hidden max-w-xs truncate text-secondary md:table-cell">
            {{ p.address ?? '—' }}
          </td>
          <td class="hidden max-w-xs truncate text-secondary lg:table-cell">
            {{ p.notes ?? '—' }}
          </td>
          <td v-if="bolehKelola" class="text-right">
            <div class="flex justify-end gap-2">
              <AppButton variant="ghost" size="sm" @click="bukaUbah(p)">Ubah</AppButton>
              <button
                type="button"
                class="text-xs font-semibold text-danger hover:underline"
                @click="hapus(p)"
              >
                Hapus
              </button>
            </div>
          </td>
        </tr>
      </AppTable>
      <AppPager :meta="meta" @page="load" @update:per-page="setPerPage" />
    </AppCard>

    <AppModal
      :open="formOpen"
      :title="sunting ? 'Ubah pelanggan' : 'Pelanggan baru'"
      description="Nomor telepon boleh kosong, tetapi harus unik jika diisi."
      @close="formOpen = false"
    >
      <AppBanner tone="red" :message="mutasi.error.value" />
      <form id="pelanggan-form" class="grid gap-3" @submit.prevent="simpan">
        <AppField label="Nama pelanggan">
          <AppInput v-model="form.name" maxlength="255" data-autofocus />
        </AppField>
        <AppField label="Telepon">
          <AppInput v-model="form.phone" maxlength="30" placeholder="cth. 0812 3456 7890" />
        </AppField>
        <AppField label="Alamat">
          <AppTextarea v-model="form.address" :rows="2" :maxlength="500" />
        </AppField>
        <AppField label="Catatan">
          <AppTextarea
            v-model="form.notes"
            :rows="2"
            :maxlength="500"
            placeholder="cth. pelanggan langganan, preferensi servis"
          />
        </AppField>
      </form>
      <template #footer>
        <AppButton variant="ghost" @click="formOpen = false">Batal</AppButton>
        <AppButton
          type="submit"
          form="pelanggan-form"
          :loading="mutasi.loading.value"
          :disabled="!form.name.trim()"
        >
          Simpan
        </AppButton>
      </template>
    </AppModal>
  </div>
</template>
