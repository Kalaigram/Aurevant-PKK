<script setup lang="ts">
import { useId } from 'vue'

defineOptions({ inheritAttrs: false })

withDefaults(
  defineProps<{
    label?: string
    hint?: string
    disabled?: boolean
    id?: string
  }>(),
  { label: '', hint: '', disabled: false, id: undefined },
)

const model = defineModel<boolean>({ default: false })
const generatedId = `checkbox-${useId()}`
</script>

<template>
  <label class="flex cursor-pointer items-start gap-2.5" :class="disabled ? 'cursor-not-allowed opacity-60' : ''">
    <input
      v-bind="$attrs"
      :id="id || generatedId"
      v-model="model"
      type="checkbox"
      :disabled="disabled"
      class="mt-0.5 h-4 w-4 rounded border-outline text-primary accent-primary focus:ring-2 focus:ring-primary/20"
    />
    <span v-if="label || $slots.default" class="min-w-0">
      <span class="block text-sm text-ink">
        <slot>{{ label }}</slot>
      </span>
      <span v-if="hint" class="mt-0.5 block text-xs text-secondary">{{ hint }}</span>
    </span>
  </label>
</template>
