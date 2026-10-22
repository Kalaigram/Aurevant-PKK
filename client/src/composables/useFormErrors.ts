import { ref } from 'vue'
import { ApiError } from '../lib/api'
import { fieldErrors } from '../lib/format'

export function useFormErrors() {
  const errors = ref<Record<string, string>>({})
  const message = ref('')

  function clear() {
    errors.value = {}
    message.value = ''
  }

  function set(error: unknown) {
    clear()
    if (error instanceof ApiError && error.errors) {
      const next: Record<string, string> = {}
      for (const [field, list] of Object.entries(error.errors)) {
        const first = list[0]
        if (first) next[field] = first
      }
      errors.value = next
      if (!Object.keys(next).length) message.value = fieldErrors(error)
      return
    }
    message.value = fieldErrors(error)
  }

  return {
    errors,
    message,
    clear,
    set,
    errorFor: (field: string) => errors.value[field] ?? '',
  }
}
