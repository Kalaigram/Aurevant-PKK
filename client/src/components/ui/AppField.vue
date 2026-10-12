<script setup lang="ts">
defineProps<{ label: string; error?: string }>()
</script>

<template>
  <div>
    <label class="mb-1 block text-sm text-slate-500">{{ label }}</label>
    <slot />
    <p v-if="error" class="mt-1 text-sm text-red-600">{{ error }}</p>
  </div>
</template>
