<script setup lang="ts">
withDefaults(
  defineProps<{
    title?: string
    subtitle?: string
    flush?: boolean
  }>(),
  { title: '', subtitle: '', flush: false },
)
</script>

<template>
  <section class="app-card overflow-hidden rounded-xl border border-outline/60 bg-card shadow-sm">
    <header
      v-if="title || subtitle || $slots.header || $slots.actions"
      class="flex items-start justify-between gap-3 border-b border-outline/50 px-4 py-3"
    >
      <slot name="header">
        <div class="min-w-0">
          <h3 v-if="title" class="font-display text-sm font-semibold text-ink">{{ title }}</h3>
          <p v-if="subtitle" class="mt-0.5 text-xs text-secondary">{{ subtitle }}</p>
        </div>
      </slot>
      <div v-if="$slots.actions" class="flex shrink-0 items-center gap-2">
        <slot name="actions" />
      </div>
    </header>
    <div :class="flush ? '' : 'p-4'">
      <slot />
    </div>
    <footer v-if="$slots.footer" class="border-t border-outline/50 px-4 py-3">
      <slot name="footer" />
    </footer>
  </section>
</template>
