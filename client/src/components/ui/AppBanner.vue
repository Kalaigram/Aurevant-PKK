<script setup lang="ts">
import { computed } from 'vue'
import AppIcon from './AppIcon.vue'

const props = withDefaults(
  defineProps<{
    tone?: 'red' | 'green' | 'amber' | 'blue' | 'gray'
    message?: string
    title?: string
    dismissible?: boolean
    icon?: string
  }>(),
  { tone: 'red', message: '', dismissible: false, icon: '' },
)

const emit = defineEmits<{ dismiss: [] }>()

const styles = {
  red: 'border-danger/25 bg-danger-bg/75 text-danger',
  green: 'border-ok/25 bg-ok-bg text-ok-ink',
  amber: 'border-warn/30 bg-warn-bg/85 text-warn-ink',
  blue: 'border-info/25 bg-info-bg text-info-ink',
  gray: 'border-outline bg-surface-low text-ink',
}

const icons = {
  red: 'error',
  green: 'check_circle',
  amber: 'warning',
  blue: 'info',
  gray: 'info',
}

const role = computed(() => (props.tone === 'red' ? 'alert' : 'status'))
const ikon = computed(() => props.icon || icons[props.tone])
</script>

<template>
  <div
    v-if="message || $slots.default"
    :role="role"
    class="mb-4 flex items-start gap-3 rounded-xl border px-4 py-3 text-sm"
    :class="styles[tone]"
  >
    <AppIcon :name="ikon" :size="20" class="mt-0.5 opacity-90" />
    <div class="min-w-0 flex-1">
      <p v-if="title" class="font-semibold">{{ title }}</p>
      <div class="leading-relaxed">
        <slot>{{ message }}</slot>
      </div>
    </div>
    <button
      v-if="dismissible"
      type="button"
      class="-mr-1 rounded-lg p-1.5 opacity-70 transition hover:bg-ink/5 hover:opacity-100 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-current focus-visible:ring-offset-2"
      aria-label="Tutup pemberitahuan"
      @click="emit('dismiss')"
    >
      <AppIcon name="close" :size="18" />
    </button>
  </div>
</template>
