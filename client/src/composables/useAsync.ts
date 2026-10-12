import { ref } from 'vue'

/** Bungkus panggilan API: loading + pesan error siap tampil. */
export function useAsync() {
  const loading = ref(false)
  const error = ref('')

  async function run<T>(fn: () => Promise<T>): Promise<T | null> {
    loading.value = true
    error.value = ''
    try {
      return await fn()
    } catch (e) {
      error.value = e instanceof Error && 'errors' in e
        ? Object.values((e as { errors: Record<string, string[]> | null }).errors ?? {}).flat().join(' ') || e.message
        : e instanceof Error ? e.message : 'Terjadi kesalahan.'
      return null
    } finally {
      loading.value = false
    }
  }

  return { loading, error, run }
}
