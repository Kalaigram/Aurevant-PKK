<script setup lang="ts">
import { computed } from 'vue'
import type { PageMeta } from '../../lib/api'
import AppIcon from './AppIcon.vue'

const props = withDefaults(
  defineProps<{
    meta: PageMeta | null
    perPageOptions?: number[]
    showPerPage?: boolean
  }>(),
  {
    perPageOptions: () => [10, 15, 25, 50],
    showPerPage: true,
  },
)

const emit = defineEmits<{ page: [n: number]; 'update:perPage': [n: number] }>()

const current = computed(() => props.meta?.current_page ?? 1)
const last = computed(() => props.meta?.last_page ?? 1)
const total = computed(() => props.meta?.total ?? 0)
const from = computed(
  () => props.meta?.from ?? (total.value ? (current.value - 1) * (props.meta?.per_page ?? 15) + 1 : 0),
)
const to = computed(
  () =>
    props.meta?.to ??
    Math.min(current.value * (props.meta?.per_page ?? 15), total.value),
)

const pages = computed<(number | '…')[]>(() => {
  const lastPage = last.value
  if (lastPage <= 7) return Array.from({ length: lastPage }, (_, index) => index + 1)
  const set = new Set<number>([1, lastPage, current.value - 1, current.value, current.value + 1])
  const valid = [...set].filter((page) => page >= 1 && page <= lastPage).sort((a, b) => a - b)
  const result: (number | '…')[] = []
  valid.forEach((page, index) => {
    const previous = valid[index - 1]
    if (previous !== undefined && page - previous > 1) result.push('…')
    result.push(page)
  })
  return result
})

const perPageList = computed(() => {
  const options = new Set(props.perPageOptions)
  if (props.meta?.per_page) options.add(props.meta.per_page)
  return [...options].sort((a, b) => a - b)
})

function go(page: number) {
  if (page < 1 || page > last.value || page === current.value) return
  emit('page', page)
}

function changePerPage(event: Event) {
  const value = Number((event.target as HTMLSelectElement).value)
  if (Number.isFinite(value)) emit('update:perPage', value)
}
</script>

<template>
  <div
    v-if="meta && total > 0"
    class="mt-3 flex flex-col gap-3 text-sm text-secondary sm:flex-row sm:items-center sm:justify-between"
  >
    <p class="text-xs tabular-nums">
      Menampilkan <span class="font-medium text-ink">{{ from }}–{{ to }}</span> dari
      <span class="font-medium text-ink">{{ total }}</span>
    </p>

    <nav v-if="last > 1" class="flex flex-wrap items-center gap-1.5" aria-label="Paginasi">
      <button
        type="button"
        class="flex h-11 w-11 items-center justify-center rounded-xl border border-outline/80 bg-card text-ink transition hover:border-outline-strong hover:bg-surface-low focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/55 focus-visible:ring-offset-1 disabled:cursor-not-allowed disabled:opacity-40 sm:h-9 sm:w-9"
        :disabled="current <= 1"
        aria-label="Halaman sebelumnya"
        @click="go(current - 1)"
      >
        <AppIcon name="chevron_left" :size="18" />
      </button>
      <template v-for="(page, index) in pages" :key="`${page}-${index}`">
        <span v-if="page === '…'" class="px-1 text-xs text-secondary" aria-hidden="true">…</span>
        <button
          v-else
          type="button"
          class="min-h-11 min-w-11 rounded-xl px-2.5 text-xs font-semibold transition focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/55 focus-visible:ring-offset-1 sm:min-h-9 sm:min-w-9"
          :class="
            page === current
              ? 'bg-primary-container text-on-primary shadow-card'
              : 'border border-outline/80 bg-card text-ink hover:border-outline-strong hover:bg-surface-low'
          "
          :aria-current="page === current ? 'page' : undefined"
          :aria-label="`Halaman ${page}`"
          @click="go(page)"
        >
          {{ page }}
        </button>
      </template>
      <button
        type="button"
        class="flex h-11 w-11 items-center justify-center rounded-xl border border-outline/80 bg-card text-ink transition hover:border-outline-strong hover:bg-surface-low focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/55 focus-visible:ring-offset-1 disabled:cursor-not-allowed disabled:opacity-40 sm:h-9 sm:w-9"
        :disabled="current >= last"
        aria-label="Halaman berikutnya"
        @click="go(current + 1)"
      >
        <AppIcon name="chevron_right" :size="18" />
      </button>
    </nav>

    <label v-if="showPerPage" class="flex items-center gap-2 text-xs">
      <span>Baris</span>
      <select
        class="min-h-9 rounded-lg border border-outline/80 bg-card px-2.5 py-1.5 text-xs text-ink transition hover:border-outline-strong focus:border-primary focus:outline-none focus:ring-2 focus:ring-primary/30"
        :value="meta.per_page"
        aria-label="Jumlah baris per halaman"
        @change="changePerPage"
      >
        <option v-for="size in perPageList" :key="size" :value="size">{{ size }}</option>
      </select>
    </label>
  </div>
</template>
