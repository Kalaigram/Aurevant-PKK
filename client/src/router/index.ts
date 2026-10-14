import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { setOnUnauthorized } from '../lib/api'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/', redirect: '/companies' },
    { path: '/login', name: 'login', component: () => import('../views/LoginView.vue') },
    { path: '/register', name: 'register', component: () => import('../views/RegisterView.vue') },
    {
      path: '/companies',
      name: 'companies',
      component: () => import('../views/CompaniesView.vue'),
      meta: { auth: true },
    },
    {
      path: '/admin/tenants',
      name: 'tenants',
      component: () => import('../views/TenantsView.vue'),
      meta: { auth: true, superAdmin: true },
    },
    {
      path: '/c/:companyId',
      component: () => import('../views/CompanyShell.vue'),
      meta: { auth: true, company: true },
      children: [
        { path: '', redirect: { name: 'dashboard' } },
        {
          path: 'dashboard',
          name: 'dashboard',
          component: () => import('../views/DashboardView.vue'),
        },
        {
          path: 'notifications',
          name: 'notifications',
          component: () => import('../views/NotificationsView.vue'),
        },
        {
          path: 'approvals',
          name: 'approvals',
          component: () => import('../views/ApprovalsView.vue'),
        },
        {
          path: 'attendance',
          name: 'attendance',
          component: () => import('../views/AttendanceView.vue'),
        },
        { path: 'leaves', name: 'leaves', component: () => import('../views/LeavesView.vue') },
        { path: 'slip', name: 'slip', component: () => import('../views/SlipView.vue') },
        { path: 'card', name: 'card', component: () => import('../views/CardView.vue') },
        {
          path: 'subscription',
          name: 'subscription',
          component: () => import('../views/SubscriptionView.vue'),
        },
        { path: 'profile', name: 'profile', component: () => import('../views/ProfileView.vue') },
        {
          path: 'branches',
          name: 'branches',
          component: () => import('../views/BranchesView.vue'),
        },
        { path: 'roles', name: 'roles', component: () => import('../views/RolesView.vue') },
        {
          path: 'employees',
          name: 'employees',
          component: () => import('../views/EmployeesView.vue'),
        },
        { path: 'audit', name: 'audit', component: () => import('../views/AuditView.vue') },
        { path: 'pos', name: 'pos', component: () => import('../views/PosView.vue') },
        { path: 'sales', name: 'sales', component: () => import('../views/SalesView.vue') },
        {
          path: 'customers',
          name: 'customers',
          component: () => import('../views/CustomersView.vue'),
        },
        {
          path: 'services',
          name: 'services',
          component: () => import('../views/ServicesView.vue'),
        },
        { path: 'catalog', name: 'catalog', component: () => import('../views/CatalogView.vue') },
        { path: 'stock', name: 'stock', component: () => import('../views/StockView.vue') },
        {
          path: 'transfers',
          name: 'transfers',
          component: () => import('../views/TransfersView.vue'),
        },
        { path: 'opnames', name: 'opnames', component: () => import('../views/OpnamesView.vue') },
        {
          path: 'purchase',
          name: 'purchase',
          component: () => import('../views/PurchaseView.vue'),
        },
        { path: 'prices', name: 'prices', component: () => import('../views/PricesView.vue') },
        { path: 'import', name: 'import', component: () => import('../views/ImportView.vue') },
        {
          path: 'schedule',
          name: 'schedule',
          component: () => import('../views/ScheduleView.vue'),
        },
        {
          path: 'hr-attendance',
          name: 'hr-attendance',
          component: () => import('../views/HrAttendanceView.vue'),
        },
        { path: 'payroll', name: 'payroll', component: () => import('../views/PayrollView.vue') },
        {
          path: 'expenses',
          name: 'expenses',
          component: () => import('../views/ExpensesView.vue'),
        },
        { path: 'debts', name: 'debts', component: () => import('../views/DebtsView.vue') },
        {
          path: 'receivables',
          name: 'receivables',
          component: () => import('../views/ReceivablesView.vue'),
        },
        { path: 'journal', name: 'journal', component: () => import('../views/JournalView.vue') },
        { path: 'reports', name: 'reports', component: () => import('../views/ReportsView.vue') },
        { path: 'periods', name: 'periods', component: () => import('../views/PeriodsView.vue') },
      ],
    },
    { path: '/:pathMatch(.*)*', redirect: '/companies' },
  ],
})

setOnUnauthorized(() => {
  const auth = useAuthStore()
  auth.token = null
  auth.user = null
  auth.persist()
  router.push('/login')
})

router.beforeEach((to) => {
  const auth = useAuthStore()
  if (to.meta.auth && !auth.loggedIn) return '/login'
  if ((to.name === 'login' || to.name === 'register') && auth.loggedIn) return '/companies'
  if (to.meta.superAdmin && !auth.user?.isSuperAdmin) return '/companies'
  if (to.meta.company) {
    const id = Number(to.params.companyId)
    if (!Number.isFinite(id)) return '/companies'
    if (auth.companyId !== null && auth.companyId !== id) {
      auth.companyId = id
      auth.companyName = null
      auth.persist()
    }
  }
  return true
})

export default router
