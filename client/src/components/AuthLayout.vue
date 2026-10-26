<script setup lang="ts">
import AppIcon from './ui/AppIcon.vue'

defineProps<{ tab: 'login' | 'register' }>()

const keunggulan = [
  {
    icon: 'point_of_sale',
    title: 'Operasional harian',
    desc: 'Kasir, stok, dan pembelian tetap rapi.',
  },
  {
    icon: 'groups',
    title: 'Tim dan cabang',
    desc: 'Akses mengikuti peran di setiap cabang.',
  },
  {
    icon: 'monitoring',
    title: 'Kontrol keuangan',
    desc: 'Biaya, laba, dan laporan dalam satu pandangan.',
  },
]
</script>

<template>
  <div class="relative min-h-screen overflow-hidden bg-surface-low">
    <div class="ambient-glow pointer-events-none absolute inset-0 opacity-70" aria-hidden="true" />
    <div
      class="relative mx-auto flex min-h-screen w-full max-w-[1180px] flex-col px-4 py-4 sm:px-6 lg:px-8 lg:py-8"
    >
      <header class="flex items-center justify-between gap-4 py-2 lg:hidden">
        <RouterLink
          to="/login"
          class="flex items-center gap-3 rounded-xl focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/55"
        >
          <span
            class="flex h-10 w-10 items-center justify-center rounded-xl bg-primary font-display text-sm font-bold text-on-primary shadow-card"
          >
            A
          </span>
          <span>
            <span class="block font-display text-[15px] font-bold tracking-tight text-ink"
              >Aurevant PKK</span
            >
            <span class="block text-2xs text-secondary">Portal operasional</span>
          </span>
        </RouterLink>
        <span
          class="inline-flex items-center gap-1.5 rounded-full border border-outline/70 bg-card/80 px-3 py-1.5 text-2xs font-semibold text-secondary shadow-card"
        >
          <AppIcon name="shield_lock" :size="14" />
          Aman
        </span>
      </header>

      <div
        class="my-auto grid w-full overflow-hidden rounded-3xl border border-outline/70 bg-card shadow-float lg:grid-cols-[1.04fr_0.96fr]"
      >
        <aside class="relative hidden flex-col overflow-hidden bg-primary p-10 text-white lg:flex">
          <div
            class="ambient-glow pointer-events-none absolute inset-0 opacity-80"
            aria-hidden="true"
          />
          <div class="relative flex flex-1 flex-col">
            <div class="flex items-center gap-3">
              <span
                class="flex h-11 w-11 items-center justify-center rounded-2xl bg-white/[0.14] font-display text-lg font-extrabold tracking-tight shadow-card ring-1 ring-white/20"
              >
                A
              </span>
              <div>
                <p class="font-display text-lg font-bold tracking-tight">Aurevant PKK</p>
                <p class="text-2xs text-white/70">Manajemen bisnis multi-cabang</p>
              </div>
            </div>

            <div class="mt-14 max-w-md">
              <p class="text-2xs font-semibold uppercase tracking-[0.16em] text-white/70">
                Ruang kerja operasional
              </p>
              <h1
                class="mt-3 font-display text-3xl font-bold leading-tight tracking-[-0.02em] text-balance"
              >
                Satu tempat untuk seluruh cabang Anda.
              </h1>
              <p class="mt-3 text-sm leading-relaxed text-white/75">
                Pantau penjualan, stok, tim, dan keuangan tanpa berpindah aplikasi.
              </p>
            </div>

            <div class="mt-8 grid gap-3">
              <div
                v-for="item in keunggulan"
                :key="item.title"
                class="flex items-start gap-3 rounded-2xl border border-white/10 bg-white/[0.06] p-3.5"
              >
                <span
                  class="flex h-9 w-9 shrink-0 items-center justify-center rounded-xl bg-white/10 text-white ring-1 ring-white/10"
                >
                  <AppIcon :name="item.icon" :size="19" />
                </span>
                <div>
                  <p class="text-[13px] font-semibold text-white">{{ item.title }}</p>
                  <p class="mt-0.5 text-xs leading-relaxed text-white/80">{{ item.desc }}</p>
                </div>
              </div>
            </div>

            <div class="mt-auto flex items-start gap-3 border-t border-white/10 pt-6">
              <AppIcon name="verified_user" :size="20" class="mt-0.5 shrink-0 text-white/75" />
              <p class="text-xs leading-relaxed text-white/70">
                <span class="font-semibold text-white">Data terpisah per perusahaan.</span>
                Setiap cabang dan peran hanya melihat data yang menjadi haknya.
              </p>
            </div>
          </div>
        </aside>

        <section class="flex flex-col justify-center p-5 sm:p-8 lg:p-10">
          <div class="mx-auto w-full max-w-md">
            <nav
              class="grid grid-cols-2 gap-1 rounded-xl border border-outline/60 bg-surface-low p-1"
              aria-label="Pilih formulir"
            >
              <RouterLink
                to="/login"
                class="flex min-h-11 items-center justify-center gap-2 rounded-lg px-3 text-sm font-semibold transition focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/55"
                :class="
                  tab === 'login'
                    ? 'bg-card text-primary-ink shadow-card ring-1 ring-outline/60'
                    : 'text-secondary hover:bg-card/60 hover:text-ink'
                "
                :aria-current="tab === 'login' ? 'page' : undefined"
              >
                <AppIcon name="login" :size="18" />
                Masuk
              </RouterLink>
              <RouterLink
                to="/register"
                class="flex min-h-11 items-center justify-center gap-2 rounded-lg px-3 text-sm font-semibold transition focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/55"
                :class="
                  tab === 'register'
                    ? 'bg-card text-primary-ink shadow-card ring-1 ring-outline/60'
                    : 'text-secondary hover:bg-card/60 hover:text-ink'
                "
                :aria-current="tab === 'register' ? 'page' : undefined"
              >
                <AppIcon name="person_add" :size="18" />
                Daftar
              </RouterLink>
            </nav>

            <div class="mt-7">
              <slot />
            </div>

            <div
              class="mt-7 flex items-start gap-2.5 border-t border-outline/60 pt-4 text-2xs leading-relaxed text-secondary"
            >
              <AppIcon name="lock" :size="15" class="mt-0.5 shrink-0 text-primary" />
              <p>Sesi tersimpan aman di perangkat ini. Keluar kapan saja dari menu akun.</p>
            </div>
          </div>
        </section>
      </div>

      <p class="py-4 text-center text-2xs text-secondary">
        Aurevant PKK · Portal operasional multi-cabang
      </p>
    </div>
  </div>
</template>
