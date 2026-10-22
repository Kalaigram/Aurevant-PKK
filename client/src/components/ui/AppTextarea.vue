<script setup lang="ts">
import { computed } from 'vue'

defineOptions({ inheritAttrs: false })

withDefaults(
  defineProps<{
    rows?: number
    maxlength?: number
    showCount?: boolean
    disabled?: boolean
    invalid?: boolean
    id?: string
  }>(),
  { rows: 3, maxlength: undefined, showCount: false, disabled: false, invalid: false, id: undefined },
)

const model = defineModel<string>({ default: '' })
const count = computed(() => String(model.value ?? '').length)
</script>

<template>
  <div>
    <div
      class="rounded-lg border bg-card px-3 py-2 shadow-sm transition focus-within:ring-2 focus-within:ring-primary/15"
      :class="invalid ? 'border-danger focus-within:border-danger' : 'border-outline focus-within:border-primary'"
    >
      <textarea
        v-bind="$attrs"
        :id="id"
        v-model="model"
        :rows="rows"
        :maxlength="maxlength"
        :disabled="disabled"
        :aria-invalid="invalid || undefined"
        class="w-full resize-y bg-transparent text-sm leading-relaxed text-ink placeholder:text-secondary/60 focus:outline-none disabled:cursor-not-allowed disabled:text-secondary"
      />
    </div>
    <p v-if="showCount && maxlength" class="mt-1 text-right text-xs text-secondary">
      {{ count }}/{{ maxlength }}
    </p>
  </div>
</template>
