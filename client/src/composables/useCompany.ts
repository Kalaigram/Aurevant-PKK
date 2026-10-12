import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '../stores/auth'

/** ID perusahaan aktif dari route, sinkron ke store. */
export function useCompany() {
  const route = useRoute()
  const auth = useAuthStore()
  const companyId = computed(() => Number(route.params.companyId ?? auth.companyId ?? 0))
  return { auth, companyId }
}
