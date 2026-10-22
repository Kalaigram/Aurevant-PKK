<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(
  defineProps<{
    values: number[]
    height?: number
    color?: string
    filled?: boolean
    label?: string
  }>(),
  {
    height: 40,
    color: 'var(--color-primary-container)',
    filled: true,
    label: 'Tren data',
  },
)

const geometry = computed(() => {
  const values = props.values
  if (!values.length) return { line: 'M0,34 L100,34', area: 'M0,34 L100,34 L100,36 L0,36 Z' }
  const max = Math.max(...values)
  const min = Math.min(...values)
  const span = max - min || 1
  const points = values.map((value, index) => {
    const x = values.length === 1 ? 50 : (index / (values.length - 1)) * 100
    const y = 34 - ((value - min) / span) * 28
    return [x, y] as const
  })
  const line = points
    .map(([x, y], index) => `${index === 0 ? 'M' : 'L'}${x.toFixed(2)},${y.toFixed(2)}`)
    .join(' ')
  return { line, area: `${line} L100,36 L0,36 Z` }
})
</script>

<template>
  <svg
    viewBox="0 0 100 36"
    preserveAspectRatio="none"
    role="img"
    :aria-label="label"
    class="w-full"
    :style="{ height: `${height}px` }"
  >
    <title>{{ label }}</title>
    <path v-if="filled" :d="geometry.area" :fill="color" opacity="0.12" />
    <path
      :d="geometry.line"
      :stroke="color"
      fill="none"
      stroke-width="2"
      stroke-linecap="round"
      stroke-linejoin="round"
      vector-effect="non-scaling-stroke"
    />
  </svg>
</template>
