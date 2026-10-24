<script setup lang="ts">
import { nextTick, onBeforeUnmount, onMounted, ref, useId, watch } from 'vue'

const props = withDefaults(
  defineProps<{
    label: string
    error?: string
    hint?: string
    for?: string
    required?: boolean
    optional?: boolean
  }>(),
  { error: '', hint: '', for: '', required: false, optional: false },
)

const generatedId = `field-${useId()}`
const hintId = `${generatedId}-hint`
const errorId = `${generatedId}-error`
const root = ref<HTMLElement | null>(null)
const controlId = ref(props.for || generatedId)
let observer: MutationObserver | null = null

function findControl() {
  const container = root.value
  if (!container) return null
  return container.querySelector<HTMLElement>(
    'input:not([type="hidden"]), select, textarea, [contenteditable="true"]',
  )
}

function syncControl() {
  const control = findControl()
  if (!control) return
  if (!control.id) control.id = props.for || generatedId
  controlId.value = props.for || control.id

  if (props.error) {
    control.setAttribute('aria-invalid', 'true')
    const describedBy = [props.hint ? hintId : '', errorId].filter(Boolean).join(' ')
    control.setAttribute('aria-describedby', describedBy)
  } else {
    control.removeAttribute('aria-invalid')
    if (props.hint) control.setAttribute('aria-describedby', hintId)
    else control.removeAttribute('aria-describedby')
  }
}

onMounted(async () => {
  await nextTick()
  syncControl()
  if (root.value) {
    observer = new MutationObserver(syncControl)
    observer.observe(root.value, { childList: true, subtree: true })
  }
})

onBeforeUnmount(() => observer?.disconnect())

watch(
  () => [props.error, props.hint, props.for],
  async () => {
    await nextTick()
    syncControl()
  },
)
</script>

<template>
  <div ref="root" class="min-w-0">
    <label :for="controlId" class="mb-1.5 flex items-center gap-1 text-[13px] font-semibold text-ink">
      {{ label }}
      <span v-if="required" class="text-danger" aria-hidden="true">*</span>
      <span v-if="optional" class="text-xs font-normal text-secondary">(opsional)</span>
    </label>
    <slot :id="controlId" />
    <p v-if="hint && !error" :id="hintId" class="mt-1.5 text-xs leading-relaxed text-secondary">
      {{ hint }}
    </p>
    <p
      v-if="error"
      :id="errorId"
      class="mt-1.5 text-xs font-medium leading-relaxed text-danger"
      aria-live="polite"
    >
      {{ error }}
    </p>
  </div>
</template>
