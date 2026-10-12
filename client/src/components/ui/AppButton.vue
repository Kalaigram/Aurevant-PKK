<script setup lang="ts">
withDefaults(
  defineProps<{ variant?: 'primary' | 'secondary' | 'danger' | 'ghost'; loading?: boolean; type?: 'button' | 'submit' }>(),
  { variant: 'primary', loading: false, type: 'button' },
)

const styles: Record<string, string> = {
  primary: 'bg-primary-700 text-white hover:bg-primary-900',
  secondary: 'bg-slate-700 text-white hover:bg-slate-800',
  danger: 'bg-red-600 text-white hover:bg-red-700',
  ghost: 'bg-transparent text-primary-700 border border-slate-300 hover:bg-slate-100',
}
</script>

<template>
  <button
    :type="type"
    :disabled="loading"
    class="rounded-lg px-4 py-2.5 text-sm font-semibold disabled:cursor-wait disabled:opacity-60"
    :class="styles[variant]"
  >
    <slot>{{ loading ? 'Memproses…' : '' }}</slot>
  </button>
</template>
