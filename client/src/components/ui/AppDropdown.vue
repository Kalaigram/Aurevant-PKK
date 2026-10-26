<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, useId } from 'vue'
import AppIcon from './AppIcon.vue'

interface DropdownItem {
  label?: string
  icon?: string
  to?: string
  danger?: boolean
  disabled?: boolean
  divider?: boolean
  onSelect?: () => void
}

const props = withDefaults(
  defineProps<{
    items: DropdownItem[]
    label?: string
    align?: 'left' | 'right'
    menuWidth?: number
    triggerClass?: string
  }>(),
  { label: '', align: 'right', menuWidth: 192, triggerClass: '' },
)

const open = ref(false)
const trigger = ref<HTMLElement | null>(null)
const menu = ref<HTMLElement | null>(null)
const position = ref<Record<string, string>>({})
const menuId = `dropdown-${useId()}`

const triggerClasses = computed(() => [
  'inline-flex min-h-10 items-center justify-center gap-2 rounded-xl text-ink transition hover:bg-surface-low focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/55 focus-visible:ring-offset-1',
  props.label ? 'px-3.5 py-2 text-sm font-medium' : 'h-10 w-10 p-2',
  props.triggerClass,
])

function updatePosition() {
  const rect = trigger.value?.getBoundingClientRect()
  if (!rect) return
  const estimated = Math.min(props.items.length * 40 + 16, 320)
  let top = rect.bottom + 6
  if (top + estimated > window.innerHeight - 8) top = Math.max(8, rect.top - estimated - 6)
  const width = Math.max(rect.width, props.menuWidth)
  const left = props.align === 'left' ? rect.left : rect.right - width
  position.value = {
    top: `${Math.round(top)}px`,
    left: `${Math.round(Math.min(Math.max(8, left), window.innerWidth - width - 8))}px`,
    width: `${Math.round(width)}px`,
  }
}

async function openMenu() {
  open.value = true
  await nextTick()
  updatePosition()
  menu.value?.querySelector<HTMLElement>('[role="menuitem"]:not(:disabled)')?.focus()
}

function close() {
  open.value = false
}

function toggle() {
  if (open.value) close()
  else void openMenu()
}

function choose(item: DropdownItem) {
  if (item.disabled) return
  close()
  trigger.value?.focus()
  item.onSelect?.()
}

function onPointerDown(event: PointerEvent) {
  const target = event.target
  if (!(target instanceof Node)) return
  if (trigger.value?.contains(target) || menu.value?.contains(target)) return
  close()
}

function onScroll() {
  if (open.value) updatePosition()
}

function onMenuKeydown(event: KeyboardEvent) {
  if (event.key === 'Escape') {
    event.preventDefault()
    close()
    trigger.value?.focus()
    return
  }
  if (event.key === 'Tab') {
    close()
    return
  }
  if (!['ArrowDown', 'ArrowUp', 'Home', 'End'].includes(event.key)) return
  event.preventDefault()
  const items = Array.from(
    menu.value?.querySelectorAll<HTMLElement>('[role="menuitem"]:not(:disabled)') ?? [],
  )
  if (!items.length) return
  const current = items.findIndex((item) => item === document.activeElement)
  let next = current
  if (event.key === 'Home') next = 0
  else if (event.key === 'End') next = items.length - 1
  else if (event.key === 'ArrowDown') next = (current + 1) % items.length
  else if (event.key === 'ArrowUp') next = (current - 1 + items.length) % items.length
  items[next]?.focus()
}

onMounted(() => {
  document.addEventListener('pointerdown', onPointerDown)
  window.addEventListener('scroll', onScroll, true)
  window.addEventListener('resize', onScroll)
})

onBeforeUnmount(() => {
  document.removeEventListener('pointerdown', onPointerDown)
  window.removeEventListener('scroll', onScroll, true)
  window.removeEventListener('resize', onScroll)
})
</script>

<template>
  <div class="relative inline-flex">
    <button
      ref="trigger"
      type="button"
      :aria-label="label || 'Buka menu'"
      aria-haspopup="menu"
      :aria-expanded="open"
      :aria-controls="open ? menuId : undefined"
      :class="triggerClasses"
      @click="toggle"
      @keydown.down.prevent="openMenu"
      @keydown.escape="close"
    >
      <slot name="trigger" :open="open">
        {{ label }}
        <AppIcon v-if="label" name="expand_more" :size="16" />
      </slot>
    </button>

    <Teleport to="body">
      <Transition
        enter-active-class="transition duration-150 ease-out"
        enter-from-class="opacity-0 scale-95"
        enter-to-class="opacity-100 scale-100"
        leave-active-class="transition duration-100 ease-in"
        leave-from-class="opacity-100 scale-100"
        leave-to-class="opacity-0 scale-95"
      >
        <div
          v-if="open"
          :id="menuId"
          ref="menu"
          role="menu"
          :aria-label="label || 'Menu'"
          class="fixed z-[90] max-h-[min(320px,calc(100vh-16px))] origin-top-right overflow-y-auto overscroll-contain rounded-xl border border-outline/70 bg-card p-1 shadow-float"
          :style="position"
          @keydown="onMenuKeydown"
        >
          <template v-for="(item, index) in items" :key="index">
            <div v-if="item.divider" role="separator" class="my-1 border-t border-outline/60" />
            <router-link
              v-else-if="item.to && !item.disabled"
              :to="item.to"
              role="menuitem"
              class="flex min-h-10 w-full items-center gap-2.5 rounded-lg px-3 py-2 text-sm transition hover:bg-surface-low focus-visible:bg-surface-low focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-inset focus-visible:ring-primary/55"
              :class="item.danger ? 'text-danger' : 'text-ink'"
              @click="close"
            >
              <AppIcon v-if="item.icon" :name="item.icon" :size="18" />
              {{ item.label }}
            </router-link>
            <button
              v-else
              type="button"
              role="menuitem"
              :disabled="item.disabled"
              class="flex min-h-10 w-full items-center gap-2.5 rounded-lg px-3 py-2 text-left text-sm transition hover:bg-surface-low focus-visible:bg-surface-low focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-inset focus-visible:ring-primary/55 disabled:cursor-not-allowed disabled:opacity-40"
              :class="item.danger ? 'text-danger' : 'text-ink'"
              @click="choose(item)"
            >
              <AppIcon v-if="item.icon" :name="item.icon" :size="18" />
              {{ item.label }}
            </button>
          </template>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>
