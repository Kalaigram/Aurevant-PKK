<script setup lang="ts">
withDefaults(defineProps<{ name: string; size?: number; filled?: boolean }>(), { size: 20, filled: false })
</script>

<template>
  <span
    class="material-symbols-outlined shrink-0"
    :style="{ fontSize: size + 'px', fontVariationSettings: `'FILL' ${filled ? 1 : 0}` }"
    aria-hidden="true"
  >{{ name }}</span>
</template>
