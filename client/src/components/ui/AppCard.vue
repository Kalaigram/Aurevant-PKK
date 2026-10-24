<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(
  defineProps<{
    title?: string
    subtitle?: string
    flush?: boolean
    padding?: 'none' | 'sm' | 'md' | 'lg'
    tone?: 'default' | 'muted' | 'highlight'
  }>(),
  { title: '', subtitle: '', flush: false, padding: 'md', tone: 'default' },
)

const tones = {
  default: 'border-outline/70 bg-card',
  muted: 'border-outline/60 bg-surface-raised',
  highlight: 'border-primary/20 bg-primary-soft/40',
}

const bodyClass = computed(() => {
  if (props.flush || props.padding === 'none') return ''
  return {
    sm: 'p-3.5 sm:p-4',
    md: 'p-4 sm:p-5',
    lg: 'p-5 sm:p-6',
  }[props.padding]
})
</script>

<template>
  <section
    class="app-card overflow-hidden rounded-2xl border shadow-card"
    :class="tones[tone]"
  >
    <header
      v-if="title || subtitle || $slots.header || $slots.actions"
      class="flex items-start justify-between gap-3 border-b border-outline/55 px-4 py-3.5 sm:px-5"
    >
      <slot name="header">
        <div class="min-w-0">
          <h3 v-if="title" class="font-display text-sm font-semibold text-ink">{{ title }}</h3>
          <p v-if="subtitle" class="mt-0.5 text-xs leading-relaxed text-secondary">{{ subtitle }}</p>
        </div>
      </slot>
      <div v-if="$slots.actions" class="flex shrink-0 items-center gap-2">
        <slot name="actions" />
      </div>
    </header>
    <div :class="bodyClass">
      <slot />
    </div>
    <footer v-if="$slots.footer" class="border-t border-outline/55 bg-surface-low/45 px-4 py-3 sm:px-5">
      <slot name="footer" />
    </footer>
  </section>
</template>
