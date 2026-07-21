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
        { path: 'profile', name: 'profile', component: () => import('../views/profile/ProfileView.vue') },
        /** 我的部门 */
        { path: 'my-department', name: 'myDepartment', component: () => import('../views/my-dept/MyDepartmentView.vue') },
        /** 部门管理（仅超级管理员） */
        {
          path: 'departments',
          name: 'departments',
          component: () => import('../views/department/DepartmentListView.vue'),
        },
        /** 部门员工管理（仅部门管理员） */
        {
          path: 'dept-employees',
          name: 'deptEmployees',
          component: () => import('../views/dept-employee/DeptEmployeeListView.vue'),
        },
        /** 员工管理（仅超级管理员） */
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
        /** 公告制度与全文检索 */
        {
          path: 'contents',
          name: 'contents',
          component: () => import('../views/content/ContentView.vue'),
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

  // 管理页面权限判定（仅超级管理员可访问全公司部门管理和员工管理）
  const role = JSON.parse(localStorage.getItem('oa_user') ?? 'null')?.role || ''
  const roleUpper = role.toUpperCase()
  const isSuperAdmin = ['SUPER_ADMIN', 'ADMIN', 'ROLE_ADMIN'].includes(roleUpper)
  const isDeptManagerOrSuper = isSuperAdmin || roleUpper.includes('DEPT_MANAGER') || roleUpper.includes('MANAGER')

  if (['employees', 'departments'].includes(to.name as string) && !isSuperAdmin) return { name: 'dashboard' }
  if (to.name === 'deptEmployees' && !isDeptManagerOrSuper) return { name: 'dashboard' }
})
export default router
