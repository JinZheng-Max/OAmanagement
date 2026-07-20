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
        /** 个人信息 */
        {
          path: 'profile',
          name: 'profile',
          component: () => import('../views/profile/ProfileView.vue'),
        },
        /** 部门管理（仅管理员） */
        {
          path: 'departments',
          name: 'departments',
          component: () => import('../views/department/DepartmentListView.vue'),
        },
        /** 员工管理（仅管理员） */
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

  // 管理页面仅管理员可访问
  const isAdmin = JSON.parse(localStorage.getItem('oa_user') ?? 'null')?.role === 'ADMIN'
  if (['employees', 'departments'].includes(to.name as string) && !isAdmin) return { name: 'dashboard' }
})
export default router
