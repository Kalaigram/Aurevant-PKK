<script setup lang="ts">
import { onBeforeUnmount, onMounted } from 'vue'
import type { ToastItem } from '../../composables/useToast'
import AppIcon from './AppIcon.vue'

const props = defineProps<{ item: ToastItem }>()
const emit = defineEmits<{ dismiss: [id: number] }>()

const icons = {
  success: 'check_circle',
  error: 'error',
  warning: 'warning',
  info: 'info',
}
const tones = {
  success: 'border-l-ok text-ok',
  error: 'border-l-danger text-danger',
  warning: 'border-l-warn text-warn',
  info: 'border-l-info text-info',
}

let timer: number | undefined
let remaining = props.item.duration
let started = 0

function start() {
  if (remaining <= 0) return
  started = Date.now()
  timer = window.setTimeout(() => emit('dismiss', props.item.id), remaining)
}

function pause() {
  if (timer === undefined) return
  window.clearTimeout(timer)
  timer = undefined
  remaining = Math.max(0, remaining - (Date.now() - started))
}

function resume() {
  if (timer === undefined) start()
}

onMounted(start)
onBeforeUnmount(() => {
  if (timer !== undefined) window.clearTimeout(timer)
})
</script>

<template>
  <div
    :role="item.tone === 'error' ? 'alert' : 'status'"
    aria-atomic="true"
    class="pointer-events-auto flex w-full items-start gap-3 rounded-2xl border border-l-4 border-outline/70 bg-card p-4 shadow-float"
    :class="tones[item.tone]"
    @mouseenter="pause"
    @mouseleave="resume"
    @focusin="pause"
    @focusout="resume"
  >
    <AppIcon :name="icons[item.tone]" :size="20" class="mt-0.5" />
    <div class="min-w-0 flex-1">
      <p v-if="item.title" class="text-sm font-semibold text-ink">{{ item.title }}</p>
      <p class="text-sm leading-relaxed text-secondary">{{ item.message }}</p>
    </div>
    <button
      type="button"
      class="-mr-1 flex h-8 w-8 shrink-0 items-center justify-center rounded-lg text-secondary transition hover:bg-surface-low hover:text-ink focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/55"
      aria-label="Tutup notifikasi"
      @click="emit('dismiss', item.id)"
    >
      <AppIcon name="close" :size="16" />
    </button>
  </div>
</template>
