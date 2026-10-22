import { readonly, ref } from 'vue'

export type ToastTone = 'success' | 'error' | 'info' | 'warning'

export interface ToastItem {
  id: number
  tone: ToastTone
  title?: string
  message: string
  duration: number
}

export interface ToastInput {
  message: string
  title?: string
  tone?: ToastTone
  duration?: number
}

const items = ref<ToastItem[]>([])
let nextId = 1

const DEFAULT_DURATION: Record<ToastTone, number> = {
  success: 4000,
  error: 6500,
  info: 4500,
  warning: 5500,
}

function push(input: string | ToastInput): number {
  const data = typeof input === 'string' ? { message: input } : input
  const tone = data.tone ?? 'info'
  const id = nextId++
  items.value = [
    ...items.value.slice(-3),
    {
      id,
      tone,
      title: data.title,
      message: data.message,
      duration: data.duration ?? DEFAULT_DURATION[tone],
    },
  ]
  return id
}

function dismiss(id: number) {
  items.value = items.value.filter((toast) => toast.id !== id)
}

export function useToast() {
  return {
    toasts: readonly(items),
    push,
    dismiss,
    success: (message: string, title?: string) => push({ message, title, tone: 'success' }),
    error: (message: string, title?: string) => push({ message, title, tone: 'error' }),
    info: (message: string, title?: string) => push({ message, title, tone: 'info' }),
    warning: (message: string, title?: string) => push({ message, title, tone: 'warning' }),
  }
}
