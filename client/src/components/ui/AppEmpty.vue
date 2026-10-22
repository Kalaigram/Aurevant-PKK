<script setup lang="ts">
import AppIcon from './AppIcon.vue'

withDefaults(
  defineProps<{
    message?: string
    title?: string
    icon?: string
    compact?: boolean
  }>(),
  { message: '', title: '', icon: 'inbox', compact: false },
)
</script>

<template>
  <div
    class="flex flex-col items-center justify-center rounded-xl border border-dashed border-outline bg-card px-6 text-center"
    :class="compact ? 'py-6' : 'py-10'"
  >
    <span class="mb-3 flex h-11 w-11 items-center justify-center rounded-full bg-surface-low text-secondary">
      <AppIcon :name="icon" :size="24" />
    </span>
    <p v-if="title" class="font-display text-sm font-semibold text-ink">{{ title }}</p>
    <p class="mt-1 max-w-sm text-sm text-secondary">{{ message || 'Belum ada data.' }}</p>
    <div v-if="$slots.default" class="mt-4">
      <slot />
    </div>
  </div>
</template>
