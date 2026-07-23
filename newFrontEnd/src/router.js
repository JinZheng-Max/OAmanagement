import { createRouter, createWebHistory } from 'vue-router'
import Login from './views/Login.vue'
import Dashboard from './views/Dashboard.vue'
import Attendance from './views/Attendance.vue'
import Leave from './views/Leave.vue'
import AiAssistant from './views/AiAssistant.vue'
import ContentManage from './views/ContentManage.vue'
import OrgManage from './views/OrgManage.vue'

const routes = [
  { path: '/login', component: Login },
  { path: '/', redirect: '/dashboard' },
  { path: '/dashboard', component: Dashboard, meta: { requiresAuth: true } },
  { path: '/attendance', component: Attendance, meta: { requiresAuth: true } },
  { path: '/leave', component: Leave, meta: { requiresAuth: true } },
  { path: '/ai-assistant', component: AiAssistant, meta: { requiresAuth: true } },
  { path: '/content', component: ContentManage, meta: { requiresAuth: true } },
  { path: '/org', component: OrgManage, meta: { requiresAuth: true } },
  { path: '/:pathMatch(.*)*', redirect: '/dashboard' }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  const role = localStorage.getItem('role')

  if (to.meta.requiresAuth && !token) {
    next('/login')
    return
  }

  if (to.meta.requiresAdmin && !(role === 'SUPER_ADMIN' || role === 'ADMIN' || role === 'DEPT_MANAGER')) {
    next('/dashboard')
    return
  }

  if (to.path === '/login' && token) {
    next('/dashboard')
    return
  }

  next()
})

export default router