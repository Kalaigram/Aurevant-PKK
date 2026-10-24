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
  <header class="mb-6 flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
    <div class="flex min-w-0 items-start gap-3.5">
      <span
        v-if="icon"
        class="flex h-11 w-11 shrink-0 items-center justify-center rounded-2xl bg-primary-soft text-primary-ink shadow-card"
      >
        <AppIcon :name="icon" :size="23" />
      </span>
      <div class="min-w-0">
        <p v-if="eyebrow" class="text-2xs font-semibold uppercase tracking-[0.1em] text-secondary">
          {{ eyebrow }}
        </p>
        <h1 class="font-display text-balance text-2xl font-bold leading-tight tracking-[-0.02em] text-ink sm:text-3xl">
          {{ title }}
        </h1>
        <p v-if="subtitle" class="mt-1.5 max-w-2xl text-pretty text-sm leading-relaxed text-secondary">
          {{ subtitle }}
        </p>
        <slot name="meta" />
      </div>
    </div>
    <div v-if="$slots.actions" class="flex shrink-0 flex-wrap items-center gap-2">
      <slot name="actions" />
    </div>
  </header>
</template>
