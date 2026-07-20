import { createRouter, createWebHistory } from 'vue-router'
import LoginView from '../views/LoginView.vue'
import HomeView from '../views/HomeView.vue'
import EmployeeListView from '../views/employee/EmployeeListView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', name: 'login', component: LoginView, meta: { public: true } },
    {
      path: '/',
      component: HomeView,
      children: [
        /** 首页工作台（欢迎页） */
        {
          path: '',
          name: 'dashboard',
          component: () => import('../views/dashboard/DashboardView.vue'),
        },
        /** 员工管理 */
        {
          path: 'employees',
          name: 'employees',
          component: EmployeeListView,
        },
        /** 考勤管理 */
        {
          path: 'attendance',
          name: 'attendance',
          component: () => import('../views/attendance/AttendanceView.vue'),
        },
      ],
    },
    { path: '/:pathMatch(.*)*', redirect: '/' },
  ],
})
router.beforeEach((to) => {
  const authenticated = Boolean(localStorage.getItem('oa_token'))
  if (!to.meta.public && !authenticated) return { name: 'login', query: { redirect: to.fullPath } }
  if (to.name === 'login' && authenticated) return { name: 'dashboard' }
})
export default router
