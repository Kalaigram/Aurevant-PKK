<script setup lang="ts">
withDefaults(
  defineProps<{
    lines?: number
    height?: string
    width?: string
    rounded?: 'sm' | 'md' | 'lg' | 'full'
  }>(),
  { lines: 1, height: '0.75rem', width: '100%', rounded: 'md' },
)

const roundedClass = {
  sm: 'rounded-sm',
  md: 'rounded-md',
  lg: 'rounded-lg',
  full: 'rounded-full',
}
</script>

<template>
  <div class="space-y-2" aria-hidden="true">
    <div
      v-for="line in lines"
      :key="line"
      class="animate-pulse bg-surface-low"
      :class="roundedClass[rounded]"
      :style="{
        height,
        width: line === lines ? width : '100%',
      }"
    />
  </div>
</template>
