<script setup lang="ts">
import type { PageMeta } from '../../lib/api'
import AppButton from './AppButton.vue'

defineProps<{ meta: PageMeta | null }>()
const emit = defineEmits<{ page: [n: number] }>()
</script>

<template>
  <div v-if="meta && meta.last_page > 1" class="mt-3 flex items-center gap-2">
    <AppButton variant="ghost" :disabled="meta.current_page <= 1" @click="emit('page', meta.current_page - 1)">
      ‹ Prev
    </AppButton>
    <span class="text-sm text-slate-500">Hal {{ meta.current_page }} / {{ meta.last_page }} ({{ meta.total }})</span>
    <AppButton variant="ghost" :disabled="meta.current_page >= meta.last_page" @click="emit('page', meta.current_page + 1)">
      Next ›
    </AppButton>
  </div>
</template>
