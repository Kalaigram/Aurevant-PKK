<script setup lang="ts">
defineProps<{ message?: string }>()
</script>

<template>
  <p class="rounded-xl border border-dashed border-outline bg-card p-6 text-center text-sm text-secondary">
    {{ message ?? 'Belum ada data.' }}
  </p>
</template>
