const BASE =
  ((import.meta.env.VITE_API_URL as string | undefined) ?? 'http://127.0.0.1:8000') + '/api/v1'
const ORIGIN = BASE.replace(/\/api\/v1\/?$/, '')

export class ApiError extends Error {
  status: number
  errors: Record<string, string[]> | null
  constructor(status: number, message: string, errors: Record<string, string[]> | null = null) {
    super(message)
    this.status = status
    this.errors = errors
  }
}

export interface PageMeta {
  total: number
  per_page: number
  current_page: number
  last_page: number
  from?: number
  to?: number
  path?: string
  links?: { url: string | null; label: string; active: boolean }[]
}

export interface ApiPage<T> {
  data: T[]
  meta: PageMeta | null
}

interface ApiEnvelope<T> {
  status: string
  message: string
  data: T
  errors: Record<string, string[]> | null
  meta?: PageMeta
}

let onUnauthorized: (() => void) | null = null
export function setOnUnauthorized(fn: () => void) {
  onUnauthorized = fn
}

async function requestEnvelope<T>(
  path: string,
  init: RequestInit = {},
  token?: string | null,
): Promise<ApiEnvelope<T>> {
  const headers: Record<string, string> = { Accept: 'application/json' }
  if (token) headers['Authorization'] = `Bearer ${token}`
  if (init.body !== undefined && !(init.body instanceof FormData)) {
    headers['Content-Type'] = 'application/json'
  }
  const res = await fetch(BASE + path, { ...init, headers: { ...headers, ...init.headers } })
  if (res.status === 401 && token) onUnauthorized?.()
  if (res.status === 204) {
    return {
      status: 'success',
      message: '',
      data: undefined as T,
      errors: null,
    }
  }
  const body = (await res.json().catch(() => null)) as ApiEnvelope<T> | null
  if (!res.ok || !body || body.status !== 'success') {
    const message = body?.message ?? `HTTP ${res.status}`
    throw new ApiError(res.status, message, body?.errors ?? null)
  }
  return body
}

async function request<T>(path: string, init: RequestInit = {}, token?: string | null): Promise<T> {
  const body = await requestEnvelope<T>(path, init, token)
  return body.data
}

export const api = {
  get: <T>(path: string, token?: string | null) => request<T>(path, { method: 'GET' }, token),
  getPage: async <T>(path: string, token?: string | null): Promise<ApiPage<T>> => {
    const body = await requestEnvelope<T[]>(path, { method: 'GET' }, token)
    return {
      data: Array.isArray(body.data) ? body.data : [],
      meta: body.meta ?? null,
    }
  },
  post: <T>(path: string, payload?: unknown, token?: string | null) =>
    request<T>(
      path,
      { method: 'POST', body: payload === undefined ? undefined : JSON.stringify(payload) },
      token,
    ),
  postForm: <T>(path: string, form: FormData, token?: string | null) =>
    request<T>(path, { method: 'POST', body: form }, token),
  put: <T>(path: string, payload: unknown, token?: string | null) =>
    request<T>(path, { method: 'PUT', body: JSON.stringify(payload) }, token),
  patch: <T>(path: string, payload: unknown, token?: string | null) =>
    request<T>(path, { method: 'PATCH', body: JSON.stringify(payload) }, token),
  del: <T>(path: string, token?: string | null) => request<T>(path, { method: 'DELETE' }, token),
  delBody: <T>(path: string, payload: unknown, token?: string | null) =>
    request<T>(path, { method: 'DELETE', body: JSON.stringify(payload) }, token),
}

export function assetUrl(path?: string | null): string {
  if (!path) return ''
  if (/^(https?:)?\/\//i.test(path) || path.startsWith('data:') || path.startsWith('blob:')) {
    return path
  }
  return `${ORIGIN}${path.startsWith('/') ? '' : '/'}${path}`
}

export async function fetchBlob(path: string, token?: string | null): Promise<string> {
  const res = await fetch(BASE + path, {
    headers: { Accept: 'image/svg+xml', ...(token ? { Authorization: `Bearer ${token}` } : {}) },
  })
  if (res.status === 401 && token) onUnauthorized?.()
  if (!res.ok) throw new ApiError(res.status, `HTTP ${res.status}`)
  return URL.createObjectURL(await res.blob())
}

export { BASE as API_BASE, ORIGIN as API_ORIGIN }

/** Unduh berkas non-JSON (PDF/CSV) dengan token. Gagal → lempar ApiError. */
export async function downloadFile(
  path: string,
  filename: string,
  token?: string | null,
  accept = '*/*',
): Promise<void> {
  const res = await fetch(BASE + path, {
    headers: { Accept: accept, ...(token ? { Authorization: `Bearer ${token}` } : {}) },
  })
  if (res.status === 401 && token) onUnauthorized?.()
  if (!res.ok) {
    const body = (await res.json().catch(() => null)) as { message?: string } | null
    throw new ApiError(res.status, body?.message ?? `HTTP ${res.status}`)
  }
  const url = URL.createObjectURL(await res.blob())
  const anchor = document.createElement('a')
  anchor.href = url
  anchor.download = filename
  anchor.style.display = 'none'
  document.body.appendChild(anchor)
  anchor.click()
  anchor.remove()
  window.setTimeout(() => URL.revokeObjectURL(url), 1000)
}
