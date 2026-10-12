<script setup lang="ts">
withDefaults(defineProps<{ tone?: 'green' | 'red' | 'amber' | 'blue' | 'gray' }>(), { tone: 'gray' })

const styles: Record<string, string> = {
  green: 'bg-green-100 text-green-900',
  red: 'bg-danger-bg text-red-900',
  amber: 'bg-amber-100 text-amber-900',
  blue: 'bg-blue-100 text-blue-900',
  gray: 'bg-surface-low text-secondary',
}
</script>

<template>
  <span class="inline-block rounded-full px-2.5 py-0.5 text-xs font-semibold" :class="styles[tone]">
    <slot />
  </span>
</template>
