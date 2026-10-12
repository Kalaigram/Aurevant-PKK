<script setup lang="ts">
defineProps<{ label: string; error?: string }>()
</script>

<template>
  <div>
    <label class="mb-1 block text-sm text-secondary">{{ label }}</label>
    <slot />
    <p v-if="error" class="mt-1 text-sm text-danger">{{ error }}</p>
  </div>
</template>
