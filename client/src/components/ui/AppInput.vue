<script setup lang="ts">
defineOptions({ inheritAttrs: false })

const props = withDefaults(
  defineProps<{
    type?: 'text' | 'email' | 'password' | 'number' | 'search' | 'tel' | 'url' | 'date' | 'time'
    disabled?: boolean
    readonly?: boolean
    invalid?: boolean
    id?: string
  }>(),
  { type: 'text', disabled: false, readonly: false, invalid: false, id: undefined },
)

const model = defineModel<string | number | null>({ default: '' })

function onInput(event: Event) {
  const value = (event.target as HTMLInputElement).value
  if (props.type === 'number') {
    model.value = value === '' ? null : Number(value)
    return
  }
  model.value = value
}
</script>

<template>
  <div
    class="flex items-center gap-2 rounded-lg border bg-card px-3 shadow-sm transition focus-within:ring-2 focus-within:ring-primary/15"
    :class="invalid ? 'border-danger focus-within:border-danger' : 'border-outline focus-within:border-primary'"
  >
    <slot name="prefix" />
    <input
      v-bind="$attrs"
      :id="id"
      :type="type"
      :value="model ?? ''"
      :disabled="disabled"
      :readonly="readonly"
      :aria-invalid="invalid || undefined"
      class="min-w-0 flex-1 bg-transparent py-2.5 text-sm text-ink placeholder:text-secondary/60 focus:outline-none disabled:cursor-not-allowed disabled:text-secondary"
      @input="onInput"
    />
    <slot name="suffix" />
  </div>
</template>
