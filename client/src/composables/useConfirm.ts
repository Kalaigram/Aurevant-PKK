import { readonly, ref } from 'vue'

export type ConfirmTone = 'primary' | 'danger'

export interface ConfirmOptions {
  title: string
  message?: string
  confirmLabel?: string
  cancelLabel?: string
  tone?: ConfirmTone
  dismissible?: boolean
}

export interface ConfirmState {
  open: boolean
  title: string
  message: string
  confirmLabel: string
  cancelLabel: string
  tone: ConfirmTone
  dismissible: boolean
}

const state = ref<ConfirmState>({
  open: false,
  title: '',
  message: '',
  confirmLabel: 'Ya, lanjutkan',
  cancelLabel: 'Batal',
  tone: 'primary',
  dismissible: true,
})

let resolveCurrent: ((value: boolean) => void) | null = null

function settle(value: boolean) {
  const resolve = resolveCurrent
  resolveCurrent = null
  state.value = { ...state.value, open: false }
  resolve?.(value)
}

export function useConfirm() {
  function confirm(options: ConfirmOptions): Promise<boolean> {
    if (resolveCurrent) {
      resolveCurrent(false)
      resolveCurrent = null
    }
    state.value = {
      open: true,
      title: options.title,
      message: options.message ?? '',
      confirmLabel: options.confirmLabel ?? 'Ya, lanjutkan',
      cancelLabel: options.cancelLabel ?? 'Batal',
      tone: options.tone ?? 'primary',
      dismissible: options.dismissible ?? true,
    }
    return new Promise<boolean>((resolve) => {
      resolveCurrent = resolve
    })
  }

  return {
    state: readonly(state),
    confirm,
    accept: () => settle(true),
    cancel: () => settle(false),
  }
}
