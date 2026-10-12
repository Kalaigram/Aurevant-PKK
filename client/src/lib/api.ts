const BASE = (import.meta.env.VITE_API_URL as string | undefined ?? 'http://127.0.0.1:8000') + '/api/v1'

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
}

interface ApiEnvelope<T> {
  status: string
  message: string
  data: T
  errors: Record<string, string[]> | null
}

let onUnauthorized: (() => void) | null = null
export function setOnUnauthorized(fn: () => void) {
  onUnauthorized = fn
}

async function request<T>(path: string, init: RequestInit = {}, token?: string | null): Promise<T> {
  const headers: Record<string, string> = { Accept: 'application/json' }
  if (token) headers['Authorization'] = `Bearer ${token}`
  if (init.body !== undefined && !(init.body instanceof FormData)) {
    headers['Content-Type'] = 'application/json'
  }
  const res = await fetch(BASE + path, { ...init, headers: { ...headers, ...init.headers } })
  if (res.status === 401) {
    onUnauthorized?.()
    throw new ApiError(401, 'Sesi berakhir. Silakan login lagi.')
  }
  const body = (await res.json().catch(() => null)) as ApiEnvelope<T> | null
  if (!res.ok || !body || body.status !== 'success') {
    const message = body?.message ?? `HTTP ${res.status}`
    throw new ApiError(res.status, message, body?.errors ?? null)
  }
  return body.data
}

export const api = {
  get: <T>(path: string, token?: string | null) => request<T>(path, { method: 'GET' }, token),
  post: <T>(path: string, payload?: unknown, token?: string | null) =>
    request<T>(path, { method: 'POST', body: payload === undefined ? undefined : JSON.stringify(payload) }, token),
  postForm: <T>(path: string, form: FormData, token?: string | null) =>
    request<T>(path, { method: 'POST', body: form }, token),
  put: <T>(path: string, payload: unknown, token?: string | null) =>
    request<T>(path, { method: 'PUT', body: JSON.stringify(payload) }, token),
  del: <T>(path: string, token?: string | null) => request<T>(path, { method: 'DELETE' }, token),
}

export async function fetchBlob(path: string, token?: string | null): Promise<string> {
  const res = await fetch(BASE + path, {
    headers: { Accept: 'image/svg+xml', ...(token ? { Authorization: `Bearer ${token}` } : {}) },
  })
  if (res.status === 401) {
    onUnauthorized?.()
    throw new ApiError(401, 'Sesi berakhir. Silakan login lagi.')
  }
  if (!res.ok) throw new ApiError(res.status, `HTTP ${res.status}`)
  return URL.createObjectURL(await res.blob())
}

export { BASE as API_BASE }
