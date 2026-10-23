import { defineStore } from 'pinia'
import { api } from '../lib/api'

export interface AuthUser {
  id: number
  name: string
  email: string
  phone: string | null
  avatarPath: string | null
  avatarUrl: string | null
  employeeUuid: string | null
  ptkpStatus: string | null
  isSuperAdmin: boolean
}

interface Stored {
  token: string | null
  user: AuthUser | null
  companyId: number | null
  companyName: string | null
  companyLogoUrl: string | null
  branchId: number | null
  branchCode: string | null
}

function load(): Stored {
  try {
    const raw = localStorage.getItem('aurevant-auth')
    if (raw) return JSON.parse(raw) as Stored
  } catch { /* abaikan */ }
  return {
    token: null,
    user: null,
    companyId: null,
    companyName: null,
    companyLogoUrl: null,
    branchId: null,
    branchCode: null,
  }
}

export const useAuthStore = defineStore('auth', {
  state: () => load(),
  getters: {
    loggedIn: (s) => s.token !== null && s.user !== null,
  },
  actions: {
    persist() {
      localStorage.setItem(
        'aurevant-auth',
        JSON.stringify({ token: this.token, user: this.user, companyId: this.companyId, companyName: this.companyName, companyLogoUrl: this.companyLogoUrl, branchId: this.branchId, branchCode: this.branchCode }),
      )
    },
    async login(login: string, password: string) {
      const data = await api.post<{ user: AuthUser; token: string }>('/auth/login', { login, password })
      this.token = data.token
      this.user = data.user
      this.companyId = null
      this.companyName = null
      this.companyLogoUrl = null
      this.branchId = null
      this.branchCode = null
      this.persist()
    },
    async register(payload: { name: string; email: string; phone?: string; password: string; password_confirmation: string }) {
      const data = await api.post<{ user: AuthUser; token: string }>('/auth/register', payload)
      this.token = data.token
      this.user = data.user
      this.companyId = null
      this.companyName = null
      this.companyLogoUrl = null
      this.branchId = null
      this.branchCode = null
      this.persist()
    },
    async refreshMe() {
      if (!this.token) return
      this.user = await api.get<AuthUser>('/auth/me', this.token)
      this.persist()
    },
    pickCompany(id: number, name: string, logoUrl: string | null = null) {
      this.companyId = id
      this.companyName = name
      this.companyLogoUrl = logoUrl
      this.branchId = null
      this.branchCode = null
      this.persist()
    },
    setCompanyIdentity(name: string, logoUrl: string | null) {
      this.companyName = name
      this.companyLogoUrl = logoUrl
      this.persist()
    },
    pickBranch(id: number, code: string) {
      this.branchId = id
      this.branchCode = code
      this.persist()
    },
    async logout() {
      try {
        if (this.token) await api.post('/auth/logout', undefined, this.token)
      } catch { /* tetap keluar lokal */ }
      this.token = null
      this.user = null
      this.companyId = null
      this.companyName = null
      this.companyLogoUrl = null
      this.branchId = null
      this.branchCode = null
      this.persist()
    },
  },
})
