<script setup lang="ts">
withDefaults(
  defineProps<{
    tone?: 'green' | 'red' | 'amber' | 'blue' | 'gray'
    dot?: boolean
  }>(),
  { tone: 'gray', dot: false },
)

const styles = {
  green: 'bg-ok-bg text-emerald-800 ring-ok/15',
  red: 'bg-danger-bg text-red-800 ring-danger/15',
  amber: 'bg-warn-bg text-amber-800 ring-warn/20',
  blue: 'bg-info-bg text-blue-800 ring-info/15',
  gray: 'bg-surface-low text-secondary ring-outline/60',
}
</script>

<template>
  <span
    class="inline-flex items-center gap-1.5 rounded-full px-2.5 py-0.5 text-[11px] font-semibold ring-1 ring-inset"
    :class="styles[tone]"
  >
    <span v-if="dot" class="h-1.5 w-1.5 rounded-full bg-current" aria-hidden="true" />
    <slot />
  </span>
</template>
