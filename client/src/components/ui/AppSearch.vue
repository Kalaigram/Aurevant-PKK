<script setup lang="ts">
import { onBeforeUnmount, ref, watch } from 'vue'
import AppIcon from './AppIcon.vue'

defineOptions({ inheritAttrs: false })

const props = withDefaults(
  defineProps<{
    placeholder?: string
    label?: string
    loading?: boolean
    disabled?: boolean
    debounce?: number
  }>(),
  {
    placeholder: 'Cari…',
    label: 'Cari',
    loading: false,
    disabled: false,
    debounce: 300,
  },
)

const model = defineModel<string>({ default: '' })
const emit = defineEmits<{ search: [value: string]; clear: [] }>()
const input = ref<HTMLInputElement | null>(null)
let timer: number | undefined

function emitSearch() {
  if (timer) window.clearTimeout(timer)
  timer = undefined
  emit('search', model.value.trim())
}

function onEnter() {
  emitSearch()
}

function clear() {
  model.value = ''
  if (timer) window.clearTimeout(timer)
  emit('search', '')
  emit('clear')
  input.value?.focus()
}

watch(model, () => {
  if (timer) window.clearTimeout(timer)
  timer = window.setTimeout(emitSearch, props.debounce)
})

onBeforeUnmount(() => {
  if (timer) window.clearTimeout(timer)
})
</script>

<template>
  <div
    class="flex min-h-11 items-center gap-2 rounded-xl border border-outline/90 bg-card px-3.5 transition focus-within:border-primary focus-within:ring-2 focus-within:ring-primary/30"
  >
    <AppIcon name="search" :size="18" class="text-secondary" />
    <input
      ref="input"
      v-bind="$attrs"
      v-model="model"
      type="search"
      :aria-label="label"
      :placeholder="placeholder"
      :disabled="disabled"
      class="min-w-0 flex-1 bg-transparent py-2.5 text-sm text-ink placeholder:text-secondary/65 focus:outline-none [&::-webkit-search-cancel-button]:hidden"
      @keydown.enter.prevent="onEnter"
    />
    <svg
      v-if="loading"
      class="h-4 w-4 animate-spin text-primary"
      viewBox="0 0 24 24"
      fill="none"
      aria-hidden="true"
    >
      <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
      <path class="opacity-90" fill="currentColor" d="M4 12a8 8 0 0 1 8-8v4a4 4 0 0 0-4 4H4z" />
    </svg>
    <button
      v-else-if="model"
      type="button"
      class="flex h-8 w-8 items-center justify-center rounded-lg text-secondary transition hover:bg-surface-low hover:text-ink focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/55"
      aria-label="Hapus pencarian"
      @click="clear"
    >
      <AppIcon name="close" :size="16" />
    </button>
  </div>
</template>
