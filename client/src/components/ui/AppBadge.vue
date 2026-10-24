<script setup lang="ts">
withDefaults(
  defineProps<{
    tone?: 'green' | 'red' | 'amber' | 'blue' | 'gray'
    dot?: boolean
  }>(),
  { tone: 'gray', dot: false },
)

const styles = {
  green: 'bg-ok-bg text-ok-ink ring-ok/20',
  red: 'bg-danger-bg text-danger ring-danger/20',
  amber: 'bg-warn-bg text-warn-ink ring-warn/25',
  blue: 'bg-info-bg text-info-ink ring-info/20',
  gray: 'bg-surface-low text-secondary ring-outline/70',
}
</script>

<template>
  <span
    class="inline-flex min-h-5 items-center gap-1.5 rounded-full px-2.5 py-0.5 text-2xs font-semibold ring-1 ring-inset"
    :class="styles[tone]"
  >
    <span v-if="dot" class="h-1.5 w-1.5 rounded-full bg-current" aria-hidden="true" />
    <slot />
  </span>
</template>
