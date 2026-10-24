<script setup lang="ts">
import { nextTick, onBeforeUnmount, ref, useId, watch } from 'vue'
import AppIcon from './AppIcon.vue'

const props = withDefaults(
  defineProps<{
    open: boolean
    title?: string
    description?: string
    size?: 'sm' | 'md' | 'lg' | 'xl' | 'sheet'
    closeOnBackdrop?: boolean
    hideClose?: boolean
  }>(),
  {
    title: '',
    description: '',
    size: 'md',
    closeOnBackdrop: true,
    hideClose: false,
  },
)

const emit = defineEmits<{ close: [] }>()
const panel = ref<HTMLElement | null>(null)
const titleId = `modal-title-${useId()}`
const descriptionId = `modal-desc-${useId()}`

const sizes = {
  sm: 'sm:max-w-sm',
  md: 'sm:max-w-lg',
  lg: 'sm:max-w-2xl',
  xl: 'sm:max-w-4xl',
  sheet: 'sm:max-w-lg',
}

let previousFocus: HTMLElement | null = null
let scrollLocked = false
let previousOverflow = ''
let previousPadding = ''

function focusables() {
  if (!panel.value) return []
  return Array.from(
    panel.value.querySelectorAll<HTMLElement>(
      'a[href], button:not([disabled]), textarea:not([disabled]), input:not([disabled]), select:not([disabled]), [tabindex]:not([tabindex="-1"])',
    ),
  ).filter((element) => element.offsetParent !== null && element.getAttribute('aria-hidden') !== 'true')
}

function lockScroll() {
  if (scrollLocked) return
  scrollLocked = true
  previousOverflow = document.body.style.overflow
  previousPadding = document.body.style.paddingRight
  const gap = window.innerWidth - document.documentElement.clientWidth
  document.body.style.overflow = 'hidden'
  if (gap > 0) document.body.style.paddingRight = `${gap}px`
}

function unlockScroll() {
  if (!scrollLocked) return
  scrollLocked = false
  document.body.style.overflow = previousOverflow
  document.body.style.paddingRight = previousPadding
}

async function onOpen() {
  previousFocus = document.activeElement instanceof HTMLElement ? document.activeElement : null
  lockScroll()
  await nextTick()
  const target =
    panel.value?.querySelector<HTMLElement>('[data-autofocus]') ??
    focusables()[0] ??
    panel.value
  target?.focus()
}

function close() {
  emit('close')
}

function onKeydown(event: KeyboardEvent) {
  if (event.key === 'Escape') {
    if (props.closeOnBackdrop) {
      event.preventDefault()
      close()
    }
    return
  }
  if (event.key !== 'Tab') return
  const elements = focusables()
  if (!elements.length) {
    event.preventDefault()
    panel.value?.focus()
    return
  }
  const first = elements[0]
  const last = elements[elements.length - 1]
  if (!first || !last) return
  if (event.shiftKey && document.activeElement === first) {
    event.preventDefault()
    last.focus()
  } else if (!event.shiftKey && document.activeElement === last) {
    event.preventDefault()
    first.focus()
  }
}

watch(
  () => props.open,
  async (open) => {
    if (open) {
      await onOpen()
      return
    }
    unlockScroll()
    previousFocus?.focus()
    previousFocus = null
  },
  { immediate: true },
)

onBeforeUnmount(unlockScroll)
</script>

<template>
  <Teleport to="body">
    <Transition name="modal">
      <div
        v-if="open"
        class="modal-overlay fixed inset-0 z-50 flex items-end justify-center bg-scrim/45 backdrop-blur-[2px] sm:items-center sm:p-4"
        @click.self="closeOnBackdrop && close()"
        @keydown="onKeydown"
      >
        <div
          ref="panel"
          role="dialog"
          aria-modal="true"
          :aria-labelledby="title ? titleId : undefined"
          :aria-describedby="description ? descriptionId : undefined"
          :aria-label="!title ? description || 'Dialog' : undefined"
          tabindex="-1"
          class="modal-panel flex max-h-[92dvh] w-full flex-col overflow-hidden rounded-t-3xl border border-outline/60 bg-card shadow-float outline-none sm:rounded-2xl"
          :class="sizes[size]"
        >
          <div
            class="mx-auto mt-2.5 h-1.5 w-10 shrink-0 rounded-full bg-outline-strong/70 sm:hidden"
            aria-hidden="true"
          />
          <header
            v-if="title || description || !hideClose"
            class="flex items-start justify-between gap-4 border-b border-outline/60 px-4 py-4 sm:px-5"
          >
            <div class="min-w-0">
              <h2 v-if="title" :id="titleId" class="font-display text-lg font-semibold tracking-tight text-ink">
                {{ title }}
              </h2>
              <p v-if="description" :id="descriptionId" class="mt-1 text-sm leading-relaxed text-secondary">
                {{ description }}
              </p>
            </div>
            <button
              v-if="!hideClose"
              type="button"
              class="-mr-1 flex h-10 w-10 shrink-0 items-center justify-center rounded-xl text-secondary transition hover:bg-surface-low hover:text-ink focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/55"
              aria-label="Tutup dialog"
              @click="close"
            >
              <AppIcon name="close" :size="20" />
            </button>
          </header>
          <div class="min-h-0 flex-1 overflow-y-auto px-4 py-4 sm:px-5 sm:py-5">
            <slot />
          </div>
          <footer
            v-if="$slots.footer"
            class="flex flex-wrap items-center justify-end gap-2 border-t border-outline/60 bg-surface-low/45 px-4 py-3 pb-[max(0.75rem,env(safe-area-inset-bottom))] sm:px-5 sm:pb-3"
          >
            <slot name="footer" />
          </footer>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>
