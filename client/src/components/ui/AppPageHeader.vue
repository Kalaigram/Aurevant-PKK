<script setup lang="ts">
import AppIcon from './AppIcon.vue'

withDefaults(
  defineProps<{
    title: string
    subtitle?: string
    eyebrow?: string
    icon?: string
  }>(),
  { subtitle: '', eyebrow: '', icon: '' },
)
</script>

<template>
  <header class="mb-5 flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
    <div class="flex min-w-0 items-start gap-3">
      <span
        v-if="icon"
        class="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl bg-primary-soft text-primary"
      >
        <AppIcon :name="icon" :size="24" />
      </span>
      <div class="min-w-0">
        <p v-if="eyebrow" class="text-[11px] font-semibold uppercase tracking-wider text-secondary">
          {{ eyebrow }}
        </p>
        <h1 class="font-display truncate text-xl font-semibold tracking-tight text-ink sm:text-2xl">
          {{ title }}
        </h1>
        <p v-if="subtitle" class="mt-1 max-w-2xl text-sm text-secondary">{{ subtitle }}</p>
        <slot name="meta" />
      </div>
    </div>
    <div v-if="$slots.actions" class="flex shrink-0 flex-wrap items-center gap-2">
      <slot name="actions" />
    </div>
  </header>
</template>
