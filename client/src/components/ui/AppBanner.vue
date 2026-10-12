<script setup lang="ts">
withDefaults(defineProps<{ tone?: 'red' | 'green' | 'amber'; message?: string }>(), {
  tone: 'red',
  message: '',
})

const styles: Record<string, string> = {
  red: 'bg-red-100 text-red-900',
  green: 'bg-green-100 text-green-900',
  amber: 'bg-amber-100 text-amber-900',
}
</script>

<template>
  <p v-if="message" class="mb-4 rounded-lg px-4 py-2.5 text-sm" :class="styles[tone]">
    {{ message }}
  </p>
</template>
