<script setup lang="ts">
import AppIcon from './AppIcon.vue'
import AppSkeleton from './AppSkeleton.vue'

withDefaults(
  defineProps<{
    label: string
    value?: string | number
    icon?: string
    hint?: string
    tone?: 'default' | 'positive' | 'negative' | 'warning'
    loading?: boolean
  }>(),
  { value: '', icon: 'insights', hint: '', tone: 'default', loading: false },
)

const tones = {
  default: 'bg-primary-soft text-primary-ink',
  positive: 'bg-ok-bg text-ok-ink',
  negative: 'bg-danger-bg text-danger',
  warning: 'bg-warn-bg text-warn-ink',
}
</script>

<template>
  <div
    class="rounded-2xl border border-outline/70 bg-card p-4 shadow-card transition-shadow duration-150 hover:shadow-lifted sm:p-5"
  >
    <div class="flex items-start justify-between gap-3">
      <div class="min-w-0">
        <p class="text-2xs font-semibold uppercase tracking-[0.08em] text-secondary">{{ label }}</p>
        <AppSkeleton v-if="loading" class="mt-2" width="70%" height="1.75rem" />
        <p v-else class="font-display mt-1.5 truncate text-2xl font-bold tabular-nums tracking-tight text-ink">
          {{ value }}
        </p>
      </div>
      <span class="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl" :class="tones[tone]">
        <AppIcon :name="icon" :size="21" />
      </span>
    </div>
    <div v-if="hint || $slots.default" class="mt-2.5 text-xs leading-relaxed text-secondary">
      <slot>
        <span>{{ hint }}</span>
      </slot>
    </div>
  </div>
</template>
