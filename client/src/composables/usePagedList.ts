import { ref, type Ref } from 'vue'
import type { PageMeta } from '../lib/api'
import { fieldErrors } from '../lib/format'

export interface PagedResult<T> {
  data: T[]
  meta: PageMeta | null
}

export function usePagedList<T>(
  fetcher: (page: number, perPage: number) => Promise<PagedResult<T>>,
  options: { perPage?: number } = {},
) {
  const rows = ref([]) as Ref<T[]>
  const meta = ref<PageMeta | null>(null)
  const loading = ref(false)
  const error = ref('')
  const page = ref(1)
  const perPage = ref(options.perPage ?? 15)
  let requestId = 0

  async function load(target = page.value) {
    const current = ++requestId
    loading.value = true
    error.value = ''
    try {
      const result = await fetcher(Math.max(1, target), perPage.value)
      if (current !== requestId) return
      rows.value = result.data
      meta.value = result.meta
      page.value = result.meta?.current_page ?? Math.max(1, target)
    } catch (e) {
      if (current !== requestId) return
      error.value = fieldErrors(e)
    } finally {
      if (current === requestId) loading.value = false
    }
  }

  async function reload() {
    await load(page.value)
  }

  async function setPage(target: number) {
    if (target < 1) return
    await load(target)
  }

  async function setPerPage(size: number) {
    perPage.value = size
    await load(1)
  }

  return {
    rows,
    meta,
    loading,
    error,
    page,
    perPage,
    load,
    reload,
    setPage,
    setPerPage,
  }
}
