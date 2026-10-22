<script setup lang="ts">
import { computed, ref, watch } from 'vue'

const props = withDefaults(
  defineProps<{
    name?: string
    src?: string
    size?: 'xs' | 'sm' | 'md' | 'lg' | 'xl'
    alt?: string
    square?: boolean
  }>(),
  { name: '', src: '', size: 'md', alt: '', square: false },
)

const failed = ref(false)
const sizes = {
  xs: 'h-6 w-6 text-[10px]',
  sm: 'h-8 w-8 text-xs',
  md: 'h-10 w-10 text-sm',
  lg: 'h-12 w-12 text-base',
  xl: 'h-16 w-16 text-xl',
}
const palette = [
  'bg-emerald-100 text-emerald-800',
  'bg-sky-100 text-sky-800',
  'bg-amber-100 text-amber-800',
  'bg-violet-100 text-violet-800',
  'bg-rose-100 text-rose-800',
]

watch(
  () => props.src,
  () => {
    failed.value = false
  },
)

const initials = computed(() => {
  const words = props.name.trim().split(/\s+/).filter(Boolean)
  return words
    .slice(0, 2)
    .map((word) => word.charAt(0).toUpperCase())
    .join('')
})

const tone = computed(() => {
  let hash = 0
  for (const char of props.name) hash = (hash * 31 + char.charCodeAt(0)) >>> 0
  return palette[hash % palette.length] ?? palette[0]
})

const label = computed(() => props.alt || props.name)
</script>

<template>
  <img
    v-if="src && !failed"
    :src="src"
    :alt="label"
    class="shrink-0 object-cover ring-1 ring-outline/60"
    :class="[sizes[size], square ? 'rounded-xl' : 'rounded-full']"
    @error="failed = true"
  />
  <span
    v-else
    role="img"
    :aria-label="label || 'Pengguna'"
    :title="name || undefined"
    class="inline-flex shrink-0 items-center justify-center font-semibold ring-1 ring-black/5"
    :class="[sizes[size], square ? 'rounded-xl' : 'rounded-full', tone]"
  >
    {{ initials || '?' }}
  </span>
</template>
