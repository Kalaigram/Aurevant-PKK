import { defineStore } from 'pinia'
import { api } from '../lib/api'
import { useAuthStore } from './auth'

/** Izin user di perusahaan aktif. Di-cache per perusahaan. */
export const useAccessStore = defineStore('access', {
  state: () => ({ companyId: 0 as number, permissions: [] as string[], loaded: false }),
  actions: {
    async load(companyId: number, token: string | null) {
      if (this.loaded && this.companyId === companyId) return
      this.companyId = companyId
      this.permissions = []
      this.loaded = false
      if (!token) return
      const data = await api.get<{ permissions: string[] }>(
        `/companies/${companyId}/access/my-permissions`, token,
      )
      this.permissions = data.permissions
      this.loaded = true
    },
    can(...perms: string[]) {
      return perms.some((p) => this.permissions.includes(p))
    },
    reset() {
      this.companyId = 0
      this.permissions = []
      this.loaded = false
    },
  },
})

export function accessForCompany(companyId: number) {
  const auth = useAuthStore()
  const access = useAccessStore()
  if (auth.companyId !== companyId) {
    auth.companyId = companyId
    auth.persist()
  }
  return { auth, access, ready: access.load(companyId, auth.token) }
}
