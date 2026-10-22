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
  default: 'bg-primary-soft text-primary',
  positive: 'bg-ok-bg text-ok',
  negative: 'bg-danger-bg text-danger',
  warning: 'bg-warn-bg text-warn',
}
</script>

<template>
  <div class="rounded-xl border border-outline/60 bg-card p-4 shadow-sm">
    <div class="flex items-start justify-between gap-3">
      <div class="min-w-0">
        <p class="text-xs font-medium text-secondary">{{ label }}</p>
        <AppSkeleton v-if="loading" class="mt-2" width="70%" height="1.5rem" />
        <p v-else class="font-display mt-1.5 truncate text-xl font-bold tabular-nums text-ink">
          {{ value }}
        </p>
      </div>
      <span class="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg" :class="tones[tone]">
        <AppIcon :name="icon" :size="20" />
      </span>
    </div>
    <div v-if="hint || $slots.default" class="mt-3 text-xs text-secondary">
      <slot>
        <span>{{ hint }}</span>
      </slot>
    </div>
  </div>
</template>
