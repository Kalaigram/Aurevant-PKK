<script setup lang="ts">
withDefaults(defineProps<{ tone?: 'green' | 'red' | 'amber' | 'blue' | 'gray' }>(), { tone: 'gray' })

const styles: Record<string, string> = {
  green: 'bg-green-100 text-green-800',
  red: 'bg-red-100 text-red-800',
  amber: 'bg-amber-100 text-amber-800',
  blue: 'bg-blue-100 text-blue-800',
  gray: 'bg-slate-200 text-slate-600',
}
</script>

<template>
  <span class="inline-block rounded-full px-2.5 py-0.5 text-xs font-semibold" :class="styles[tone]">
    <slot />
  </span>
</template>
