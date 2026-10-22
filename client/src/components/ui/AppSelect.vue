<script setup lang="ts">
import AppIcon from './AppIcon.vue'

interface SelectOption {
  value: string | number
  label: string
  disabled?: boolean
}

defineOptions({ inheritAttrs: false })

const props = withDefaults(
  defineProps<{
    options?: SelectOption[]
    placeholder?: string
    disabled?: boolean
    invalid?: boolean
    id?: string
  }>(),
  {
    options: () => [],
    placeholder: '',
    disabled: false,
    invalid: false,
    id: undefined,
  },
)

const model = defineModel<string | number | null>({ default: '' })

function onChange(event: Event) {
  const raw = (event.target as HTMLSelectElement).value
  if (raw === '') {
    model.value = null
    return
  }
  const found = props.options.find((option) => String(option.value) === raw)
  model.value = found ? found.value : raw
}
</script>

<template>
  <div class="relative">
    <select
      v-bind="$attrs"
      :id="id"
      :value="model === null ? '' : model"
      :disabled="disabled"
      :aria-invalid="invalid || undefined"
      class="w-full appearance-none rounded-lg border bg-card py-2.5 pl-3 pr-9 text-sm text-ink shadow-sm transition focus:outline-none focus:ring-2 focus:ring-primary/15 disabled:cursor-not-allowed disabled:bg-surface-low disabled:text-secondary"
      :class="invalid ? 'border-danger focus:border-danger' : 'border-outline focus:border-primary'"
      @change="onChange"
    >
      <option v-if="placeholder" value="">{{ placeholder }}</option>
      <slot>
        <option
          v-for="option in options"
          :key="String(option.value)"
          :value="option.value"
          :disabled="option.disabled"
        >
          {{ option.label }}
        </option>
      </slot>
    </select>
    <AppIcon
      name="expand_more"
      :size="18"
      class="pointer-events-none absolute right-2.5 top-1/2 -translate-y-1/2 text-secondary"
    />
  </div>
</template>
