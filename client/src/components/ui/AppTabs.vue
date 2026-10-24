<script setup lang="ts">
import { useId } from 'vue'

interface TabItem {
  key: string
  label: string
  count?: number
  disabled?: boolean
}

const props = withDefaults(
  defineProps<{
    items: TabItem[]
    modelValue: string
    label?: string
    idPrefix?: string
    panelPrefix?: string
  }>(),
  { label: 'Tab', idPrefix: '', panelPrefix: '' },
)

const emit = defineEmits<{ 'update:modelValue': [key: string] }>()
const generatedId = useId()

function tabId(key: string) {
  return `${props.idPrefix || `tabs-${generatedId}`}-${key}`
}

function panelId(key: string) {
  return props.panelPrefix ? `${props.panelPrefix}-${key}` : undefined
}

function select(item: TabItem) {
  if (item.disabled) return
  emit('update:modelValue', item.key)
}

function onKeydown(event: KeyboardEvent) {
  const keys = ['ArrowLeft', 'ArrowRight', 'Home', 'End']
  if (!keys.includes(event.key)) return
  const tabs = Array.from(
    (event.currentTarget as HTMLElement).querySelectorAll<HTMLButtonElement>(
      '[role="tab"]:not(:disabled)',
    ),
  )
  if (!tabs.length) return
  event.preventDefault()
  const currentIndex = tabs.findIndex((tab) => tab === document.activeElement)
  let nextIndex = currentIndex
  if (event.key === 'Home') nextIndex = 0
  else if (event.key === 'End') nextIndex = tabs.length - 1
  else if (event.key === 'ArrowRight') nextIndex = (currentIndex + 1) % tabs.length
  else if (event.key === 'ArrowLeft') nextIndex = (currentIndex - 1 + tabs.length) % tabs.length
  const next = tabs[nextIndex]
  next?.focus()
  next?.click()
}
</script>

<template>
  <div
    role="tablist"
    aria-orientation="horizontal"
    :aria-label="label"
    class="flex gap-1 overflow-x-auto border-b border-outline/70"
    @keydown="onKeydown"
  >
    <button
      v-for="item in items"
      :id="tabId(item.key)"
      :key="item.key"
      type="button"
      role="tab"
      :aria-selected="modelValue === item.key"
      :aria-controls="panelId(item.key)"
      :tabindex="modelValue === item.key ? 0 : -1"
      :disabled="item.disabled"
      class="relative inline-flex min-h-11 shrink-0 items-center gap-1.5 border-b-2 px-3.5 py-2.5 text-sm font-medium transition focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/55 focus-visible:ring-offset-1 disabled:opacity-40"
      :class="
        modelValue === item.key
          ? 'border-primary text-primary-ink'
          : 'border-transparent text-secondary hover:border-outline-strong hover:text-ink'
      "
      @click="select(item)"
    >
      {{ item.label }}
      <span
        v-if="item.count !== undefined"
        class="rounded-full px-1.5 py-0.5 text-2xs font-semibold tabular-nums"
        :class="modelValue === item.key ? 'bg-primary-soft text-primary-ink' : 'bg-surface-low text-secondary'"
      >
        {{ item.count }}
      </span>
    </button>
  </div>
</template>
