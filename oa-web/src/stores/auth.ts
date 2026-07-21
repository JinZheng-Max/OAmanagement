import { defineStore } from 'pinia'
import * as authApi from '../api/auth'

type User = authApi.LoginResponse['userInfo']
export const useAuthStore = defineStore('auth', {
  state: () => ({ token: localStorage.getItem('oa_token'), user: JSON.parse(localStorage.getItem('oa_user') ?? 'null') as User | null }),
  getters: {
    isAuthenticated: (state) => Boolean(state.token),
    isSuperAdmin: (state) => {
      const r = (state.user?.role || '').toUpperCase()
      return r.includes('SUPER_ADMIN') || r === 'ADMIN' || r === 'ROLE_ADMIN'
    },
    isDeptManager: (state) => {
      const r = (state.user?.role || '').toUpperCase()
      return r.includes('DEPT_MANAGER') || r.includes('MANAGER')
    },
    isAdmin: (state) => {
      const r = (state.user?.role || '').toUpperCase()
      return r.includes('SUPER_ADMIN') || r.includes('DEPT_MANAGER') || r.includes('ADMIN')
    },
    roleText: (state) => {
      const r = (state.user?.role || '').toUpperCase()
      if (r.includes('SUPER_ADMIN') || r.includes('ADMIN')) return '超级管理员'
      if (r.includes('DEPT_MANAGER') || r.includes('MANAGER')) return '部门管理员'
      return '普通员工'
    }
  },
  actions: {
    async login(username: string, password: string) {
      const result = await authApi.login(username, password)
      this.token = result.token; this.user = result.userInfo
      localStorage.setItem('oa_token', result.token); localStorage.setItem('oa_user', JSON.stringify(result.userInfo))
    },
    async logout() {
      try { await authApi.logout() } finally {
        this.token = null; this.user = null; localStorage.removeItem('oa_token'); localStorage.removeItem('oa_user')
      }
    }
  }
})
