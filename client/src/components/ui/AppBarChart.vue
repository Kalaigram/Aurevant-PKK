<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(
  defineProps<{
    values: number[]
    labels?: string[]
    height?: number
    label?: string
    formatValue?: (value: number) => string
  }>(),
  { labels: () => [], height: 180, label: 'Grafik batang', formatValue: undefined },
)

const bars = computed(() => {
  const max = Math.max(...props.values.map((value) => Math.abs(value)), 1)
  return props.values.map((value, index) => ({
    value,
    label: props.labels[index] ?? '',
    height: value === 0 ? 2 : Math.max(2, (Math.abs(value) / max) * 100),
    display: props.formatValue ? props.formatValue(value) : value.toLocaleString('id-ID'),
  }))
})

const description = computed(() =>
  bars.value.map((bar) => `${bar.label || 'Data'}: ${bar.display}`).join(', '),
)
</script>

<template>
  <div v-if="values.length" class="w-full" role="img" :aria-label="`${label}. ${description}`">
    <div class="flex items-end gap-1.5" :style="{ height: `${height}px` }">
      <div
        v-for="(bar, index) in bars"
        :key="index"
        class="group flex h-full min-w-0 flex-1 items-end"
        :title="`${bar.label}${bar.label ? ': ' : ''}${bar.display}`"
      >
        <div
          class="w-full rounded-t-md bg-primary-container/80 transition-colors group-hover:bg-primary"
          :style="{ height: `${bar.height}%` }"
        />
      </div>
    </div>
    <div v-if="labels.length" class="mt-2 flex gap-1.5">
      <span
        v-for="(label, index) in labels"
        :key="index"
        class="min-w-0 flex-1 truncate text-center text-[10px] text-secondary"
      >
        {{ label }}
      </span>
    </div>
  </div>
  <p v-else class="py-8 text-center text-sm text-secondary">Belum ada data grafik.</p>
</template>
