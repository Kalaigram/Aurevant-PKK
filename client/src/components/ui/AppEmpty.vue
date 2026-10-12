<script setup lang="ts">
defineProps<{ message?: string }>()
</script>

<template>
  <p class="rounded-xl border border-dashed border-slate-300 bg-white p-6 text-center text-sm text-slate-500">
    {{ message ?? 'Belum ada data.' }}
  </p>
</template>
