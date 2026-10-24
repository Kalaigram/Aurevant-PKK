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
    class="flex flex-col items-center justify-center rounded-2xl border border-dashed border-outline-strong/60 bg-surface-raised/70 px-6 text-center"
    :class="compact ? 'py-7' : 'py-12'"
  >
    <span class="mb-3.5 flex h-12 w-12 items-center justify-center rounded-2xl bg-primary-soft/80 text-primary-ink">
      <AppIcon :name="icon" :size="24" />
    </span>
    <p v-if="title" class="font-display text-base font-semibold text-ink">{{ title }}</p>
    <p class="mt-1.5 max-w-sm text-sm leading-relaxed text-secondary">
      {{ message || 'Belum ada data.' }}
    </p>
    <div v-if="$slots.default" class="mt-4">
      <slot />
    </div>
  </div>
</template>
