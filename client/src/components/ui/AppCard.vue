<script setup lang="ts">
defineProps<{ title?: string }>()
</script>

<template>
  <section class="rounded-xl bg-card p-4 shadow-sm">
    <h3 v-if="title" class="font-display mb-3 text-base font-semibold">{{ title }}</h3>
    <slot />
  </section>
</template>
