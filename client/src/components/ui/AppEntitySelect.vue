<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, ref, useId, watch } from 'vue'
import AppIcon from './AppIcon.vue'

interface EntityOption {
  value: string | number
  label: string
  hint?: string
}

const props = withDefaults(
  defineProps<{
    fetch: (query: string) => Promise<EntityOption[]>
    label?: string
    placeholder?: string
    disabled?: boolean
    clearable?: boolean
    invalid?: boolean
    initialLabel?: string
    id?: string
  }>(),
  {
    label: '',
    placeholder: 'Cari…',
    disabled: false,
    clearable: true,
    invalid: false,
    initialLabel: '',
    id: undefined,
  },
)

const model = defineModel<string | number | null>({ default: null })
const emit = defineEmits<{ select: [option: EntityOption | null] }>()

const input = ref<HTMLInputElement | null>(null)
const root = ref<HTMLElement | null>(null)
const menu = ref<HTMLElement | null>(null)
const options = ref<EntityOption[]>([])
const open = ref(false)
const query = ref('')
const loading = ref(false)
const error = ref('')
const active = ref(0)
const selectedLabel = ref(props.initialLabel)
const position = ref<Record<string, string>>({})
const generatedId = `entity-${useId()}`
const menuId = `${generatedId}-menu`
let requestId = 0
let timer: number | undefined

const display = computed(() => (open.value ? query.value : selectedLabel.value))
const activeId = computed(() =>
  open.value && options.value[active.value] ? `${generatedId}-option-${active.value}` : undefined,
)

function optionId(index: number) {
  return `${generatedId}-option-${index}`
}

async function load(search: string) {
  const current = ++requestId
  loading.value = true
  error.value = ''
  try {
    const result = await props.fetch(search)
    if (current !== requestId) return
    options.value = result
    active.value = 0
    if (model.value !== null) {
      const selected = result.find((option) => String(option.value) === String(model.value))
      if (selected) selectedLabel.value = selected.label
    }
  } catch {
    if (current !== requestId) return
    options.value = []
    error.value = 'Gagal memuat pilihan.'
  } finally {
    if (current === requestId) loading.value = false
  }
}

function updatePosition() {
  const rect = input.value?.getBoundingClientRect()
  if (!rect) return
  const width = Math.max(rect.width, 240)
  const height = Math.min(288, options.value.length * 44 + 16)
  let top = rect.bottom + 4
  if (top + height > window.innerHeight - 8) top = Math.max(8, rect.top - height - 4)
  position.value = {
    top: `${Math.round(top)}px`,
    left: `${Math.round(Math.min(Math.max(8, rect.left), window.innerWidth - width - 8))}px`,
    width: `${Math.round(width)}px`,
    maxHeight: '288px',
  }
}

async function openMenu() {
  if (props.disabled || open.value) return
  open.value = true
  query.value = ''
  await nextTick()
  updatePosition()
  void load('')
}

function close() {
  open.value = false
  query.value = ''
}

function selectOption(option: EntityOption) {
  model.value = option.value
  selectedLabel.value = option.label
  open.value = false
  query.value = ''
  emit('select', option)
  input.value?.focus()
}

function clear() {
  model.value = null
  selectedLabel.value = ''
  query.value = ''
  emit('select', null)
  if (open.value) void load('')
  else input.value?.focus()
}

function onInput(event: Event) {
  const value = (event.target as HTMLInputElement).value
  query.value = value
  if (!open.value) {
    open.value = true
    void nextTick(updatePosition)
  }
  if (model.value !== null) {
    model.value = null
    selectedLabel.value = ''
    emit('select', null)
  }
  if (timer) window.clearTimeout(timer)
  timer = window.setTimeout(() => {
    void load(value)
    nextTick(updatePosition)
  }, 250)
}

function move(step: number) {
  if (!options.value.length) return
  active.value = Math.min(Math.max(0, active.value + step), options.value.length - 1)
}

function onKeydown(event: KeyboardEvent) {
  if (event.key === 'ArrowDown') {
    event.preventDefault()
    if (!open.value) void openMenu()
    else move(1)
    return
  }
  if (event.key === 'ArrowUp') {
    event.preventDefault()
    if (!open.value) void openMenu()
    else move(-1)
    return
  }
  if (event.key === 'Enter') {
    event.preventDefault()
    if (!open.value) {
      void openMenu()
      return
    }
    const option = options.value[active.value]
    if (option) selectOption(option)
    return
  }
  if (event.key === 'Escape') {
    if (open.value) {
      event.preventDefault()
      close()
    }
    return
  }
  if (event.key === 'Tab') close()
}

function onPointerDown(event: PointerEvent) {
  const target = event.target
  if (!(target instanceof Node)) return
  if (root.value?.contains(target) || menu.value?.contains(target)) return
  close()
}

function onViewportChange() {
  if (open.value) updatePosition()
}

watch(open, (value) => {
  if (value) {
    document.addEventListener('pointerdown', onPointerDown)
    window.addEventListener('scroll', onViewportChange, true)
    window.addEventListener('resize', onViewportChange)
    return
  }
  document.removeEventListener('pointerdown', onPointerDown)
  window.removeEventListener('scroll', onViewportChange, true)
  window.removeEventListener('resize', onViewportChange)
})

watch(model, (value) => {
  if (value === null) {
    selectedLabel.value = ''
    return
  }
  const found = options.value.find((option) => String(option.value) === String(value))
  if (found) selectedLabel.value = found.label
  else if (!selectedLabel.value) selectedLabel.value = props.initialLabel
})

watch(
  () => props.initialLabel,
  (value) => {
    if (model.value !== null && !selectedLabel.value) selectedLabel.value = value
  },
)

onBeforeUnmount(() => {
  if (timer) window.clearTimeout(timer)
  document.removeEventListener('pointerdown', onPointerDown)
  window.removeEventListener('scroll', onViewportChange, true)
  window.removeEventListener('resize', onViewportChange)
})
</script>

<template>
  <div ref="root" class="relative">
    <div
      class="flex items-center gap-2 rounded-lg border bg-card px-3 shadow-sm transition focus-within:ring-2 focus-within:ring-primary/15"
      :class="invalid ? 'border-danger focus-within:border-danger' : 'border-outline focus-within:border-primary'"
    >
      <AppIcon name="search" :size="17" class="text-secondary" />
      <input
        ref="input"
        :id="id"
        :value="display"
        type="text"
        role="combobox"
        :aria-label="label || placeholder"
        :aria-expanded="open"
        :aria-controls="menuId"
        aria-autocomplete="list"
        :aria-activedescendant="activeId"
        :aria-invalid="invalid || undefined"
        :placeholder="selectedLabel || placeholder"
        :disabled="disabled"
        autocomplete="off"
        class="min-w-0 flex-1 bg-transparent py-2.5 text-sm text-ink placeholder:text-secondary/60 focus:outline-none disabled:cursor-not-allowed disabled:text-secondary"
        @focus="openMenu"
        @input="onInput"
        @keydown="onKeydown"
      />
      <button
        v-if="clearable && model !== null && !disabled"
        type="button"
        class="rounded-md p-1 text-secondary transition hover:bg-surface-low hover:text-ink focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/30"
        aria-label="Kosongkan pilihan"
        @mousedown.prevent
        @click.stop="clear"
      >
        <AppIcon name="close" :size="16" />
      </button>
      <svg
        v-if="loading"
        class="h-4 w-4 animate-spin text-primary"
        viewBox="0 0 24 24"
        fill="none"
        aria-hidden="true"
      >
        <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
        <path class="opacity-90" fill="currentColor" d="M4 12a8 8 0 0 1 8-8v4a4 4 0 0 0-4 4H4z" />
      </svg>
      <AppIcon v-else name="expand_more" :size="18" class="text-secondary" />
    </div>

    <Teleport to="body">
      <div
        v-if="open"
        :id="menuId"
        ref="menu"
        role="listbox"
        class="fixed z-[90] overflow-y-auto rounded-xl border border-outline/70 bg-card p-1 shadow-xl"
        :style="position"
      >
        <div v-if="loading" class="px-3 py-3 text-sm text-secondary">Memuat pilihan…</div>
        <div v-else-if="error" class="px-3 py-3 text-sm text-danger">{{ error }}</div>
        <div v-else-if="!options.length" class="px-3 py-3 text-sm text-secondary">
          Tidak ada hasil.
        </div>
        <button
          v-for="(option, index) in options"
          :id="optionId(index)"
          :key="String(option.value)"
          type="button"
          role="option"
          :aria-selected="String(model) === String(option.value)"
          class="flex w-full flex-col rounded-lg px-3 py-2 text-left transition"
          :class="
            index === active
              ? 'bg-primary-soft text-primary'
              : 'text-ink hover:bg-surface-low'
          "
          @mousedown.prevent
          @mouseenter="active = index"
          @click="selectOption(option)"
        >
          <span class="text-sm font-medium">{{ option.label }}</span>
          <span v-if="option.hint" class="text-xs text-secondary">{{ option.hint }}</span>
        </button>
      </div>
    </Teleport>
  </div>
</template>
