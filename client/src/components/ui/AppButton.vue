<script setup lang="ts">
withDefaults(
  defineProps<{ variant?: 'primary' | 'secondary' | 'danger' | 'ghost'; loading?: boolean; type?: 'button' | 'submit' }>(),
  { variant: 'primary', loading: false, type: 'button' },
)

const styles: Record<string, string> = {
  primary: 'bg-primary-container text-on-primary hover:bg-primary',
  secondary: 'bg-surface-low text-ink hover:bg-outline/40',
  danger: 'bg-danger text-white hover:brightness-95',
  ghost: 'bg-transparent text-primary border border-outline hover:bg-surface-low',
}
</script>

<template>
  <button
    :type="type"
    :disabled="loading"
    class="inline-flex items-center justify-center gap-2 rounded-lg px-4 py-2.5 text-sm font-semibold transition-colors disabled:cursor-wait disabled:opacity-60"
    :class="styles[variant]"
  >
    <slot>{{ loading ? 'Memproses…' : '' }}</slot>
  </button>
</template>
