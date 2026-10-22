<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(
  defineProps<{
    variant?: 'primary' | 'secondary' | 'danger' | 'ghost' | 'subtle'
    size?: 'sm' | 'md' | 'lg'
    loading?: boolean
    type?: 'button' | 'submit' | 'reset'
    disabled?: boolean
    block?: boolean
  }>(),
  {
    variant: 'primary',
    size: 'md',
    loading: false,
    type: 'button',
    disabled: false,
    block: false,
  },
)

const styles = {
  primary: 'bg-primary-container text-on-primary shadow-sm hover:bg-primary-hover',
  secondary: 'bg-surface-low text-ink hover:bg-outline/30',
  danger: 'bg-danger text-white shadow-sm hover:bg-red-800',
  ghost: 'border border-outline bg-transparent text-ink hover:bg-surface-low',
  subtle: 'bg-primary-soft text-primary hover:bg-primary-soft/70',
}

const sizes = {
  sm: 'min-h-8 px-3 py-1.5 text-xs',
  md: 'min-h-10 px-4 py-2.5 text-sm',
  lg: 'min-h-11 px-5 py-3 text-sm',
}

const classes = computed(() => [
  styles[props.variant],
  sizes[props.size],
  props.block ? 'w-full' : '',
])
</script>

<template>
  <button
    :type="type"
    :disabled="loading || disabled"
    :aria-busy="loading || undefined"
    class="inline-flex items-center justify-center gap-2 rounded-lg font-semibold transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/30 focus-visible:ring-offset-2 disabled:cursor-wait disabled:opacity-60"
    :class="classes"
  >
    <svg
      v-if="loading"
      class="h-4 w-4 animate-spin"
      viewBox="0 0 24 24"
      fill="none"
      aria-hidden="true"
    >
      <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
      <path
        class="opacity-90"
        fill="currentColor"
        d="M4 12a8 8 0 0 1 8-8v4a4 4 0 0 0-4 4H4z"
      />
    </svg>
    <slot>{{ loading ? 'Memproses…' : '' }}</slot>
  </button>
</template>
