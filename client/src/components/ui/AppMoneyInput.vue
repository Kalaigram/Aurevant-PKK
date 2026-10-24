<script setup lang="ts">
import { ref, watch } from 'vue'

defineOptions({ inheritAttrs: false })

withDefaults(
  defineProps<{
    placeholder?: string
    disabled?: boolean
    readonly?: boolean
    invalid?: boolean
    id?: string
  }>(),
  {
    placeholder: '0',
    disabled: false,
    readonly: false,
    invalid: false,
    id: undefined,
  },
)

const model = defineModel<number | null>({ default: null })
const display = ref('')

function format(value: number | null) {
  if (value === null || Number.isNaN(value)) return ''
  return value.toLocaleString('id-ID')
}

watch(
  model,
  (value) => {
    display.value = format(value)
  },
  { immediate: true },
)

function onInput(event: Event) {
  const raw = (event.target as HTMLInputElement).value
  const digits = raw.replace(/\D/g, '')
  display.value = digits ? Number(digits).toLocaleString('id-ID') : ''
  model.value = digits ? Number(digits) : null
}

function onBlur() {
  display.value = format(model.value)
}
</script>

<template>
  <div
    class="flex min-h-11 items-center gap-2 rounded-xl border bg-card px-3.5 transition focus-within:ring-2"
    :class="
      invalid
        ? 'border-danger focus-within:border-danger focus-within:ring-danger/20'
        : 'border-outline/90 focus-within:border-primary focus-within:ring-primary/30'
    "
  >
    <span class="text-sm font-medium text-secondary">Rp</span>
    <input
      v-bind="$attrs"
      :id="id"
      :value="display"
      type="text"
      inputmode="numeric"
      autocomplete="off"
      :placeholder="placeholder"
      :disabled="disabled"
      :readonly="readonly"
      :aria-invalid="invalid || undefined"
      class="min-w-0 flex-1 bg-transparent py-2.5 text-sm tabular-nums text-ink placeholder:text-secondary/65 focus:outline-none disabled:cursor-not-allowed disabled:text-secondary"
      @input="onInput"
      @blur="onBlur"
    />
  </div>
</template>
