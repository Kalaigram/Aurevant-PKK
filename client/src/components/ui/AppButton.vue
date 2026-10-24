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
  danger: 'bg-danger text-on-primary shadow-sm hover:bg-danger-hover',
  ghost: 'border border-outline bg-transparent text-ink hover:border-outline-strong hover:bg-surface-low',
  subtle: 'bg-primary-soft text-primary-ink hover:bg-primary-soft/70',
}

const sizes = {
  sm: 'min-h-8 px-3 py-1.5 text-xs',
  md: 'min-h-10 px-4 py-2.5 text-sm',
  lg: 'min-h-11 px-5 py-3 text-sm',
}

const stateClass = computed(() =>
  props.loading ? 'cursor-wait' : 'disabled:cursor-not-allowed disabled:opacity-55',
)

const classes = computed(() => [
  styles[props.variant],
  sizes[props.size],
  stateClass.value,
  props.block ? 'w-full' : '',
])
</script>

<template>
  <button
    :type="type"
    :disabled="loading || disabled"
    :aria-busy="loading || undefined"
    class="inline-flex items-center justify-center gap-2 rounded-xl font-semibold transition-[background-color,border-color,box-shadow,transform] duration-150 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/55 focus-visible:ring-offset-2 active:translate-y-px disabled:shadow-none"
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
