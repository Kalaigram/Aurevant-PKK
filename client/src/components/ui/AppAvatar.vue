<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import AppIcon from './AppIcon.vue'

const props = withDefaults(
  defineProps<{
    name?: string
    src?: string
    size?: 'xs' | 'sm' | 'md' | 'lg' | 'xl'
    alt?: string
    square?: boolean
    fallbackIcon?: string
  }>(),
  { name: '', src: '', size: 'md', alt: '', square: false, fallbackIcon: 'person' },
)

const failed = ref(false)

const sizes = {
  xs: 'h-6 w-6',
  sm: 'h-8 w-8',
  md: 'h-10 w-10',
  lg: 'h-12 w-12',
  xl: 'h-16 w-16',
}
const iconSizes = {
  xs: 14,
  sm: 17,
  md: 21,
  lg: 25,
  xl: 33,
}

watch(
  () => props.src,
  () => {
    failed.value = false
  },
)

const label = computed(() => props.alt || props.name || 'Foto profil')
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
    :aria-label="label"
    :title="name || undefined"
    class="inline-flex shrink-0 items-center justify-center bg-surface-low text-secondary ring-1 ring-outline/60"
    :class="[sizes[size], square ? 'rounded-xl' : 'rounded-full']"
  >
    <AppIcon :name="fallbackIcon" :size="iconSizes[size]" />
  </span>
</template>
