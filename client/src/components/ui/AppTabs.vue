<script setup lang="ts">
interface TabItem {
  key: string
  label: string
  count?: number
  disabled?: boolean
}

withDefaults(
  defineProps<{
    items: TabItem[]
    modelValue: string
    label?: string
  }>(),
  { label: 'Tab' },
)

const emit = defineEmits<{ 'update:modelValue': [key: string] }>()

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
    :aria-label="label"
    class="flex gap-1 overflow-x-auto border-b border-outline/70"
    @keydown="onKeydown"
  >
    <button
      v-for="item in items"
      :id="`tab-${item.key}`"
      :key="item.key"
      type="button"
      role="tab"
      :aria-selected="modelValue === item.key"
      :tabindex="modelValue === item.key ? 0 : -1"
      :disabled="item.disabled"
      class="relative inline-flex shrink-0 items-center gap-1.5 border-b-2 px-3 py-2.5 text-sm font-medium transition focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/25 disabled:opacity-40"
      :class="
        modelValue === item.key
          ? 'border-primary text-primary'
          : 'border-transparent text-secondary hover:border-outline-strong hover:text-ink'
      "
      @click="select(item)"
    >
      {{ item.label }}
      <span
        v-if="item.count !== undefined"
        class="rounded-full bg-surface-low px-1.5 py-0.5 text-[10px] font-semibold text-secondary"
      >
        {{ item.count }}
      </span>
    </button>
  </div>
</template>
