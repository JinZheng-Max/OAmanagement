import { defineStore } from 'pinia'
import * as authApi from '../api/auth'

type User = authApi.LoginResponse['userInfo']
export const useAuthStore = defineStore('auth', {
  state: () => ({ token: localStorage.getItem('oa_token'), user: JSON.parse(localStorage.getItem('oa_user') ?? 'null') as User | null }),
  getters: { isAuthenticated: (state) => Boolean(state.token), isAdmin: (state) => state.user?.role === 'ADMIN' },
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
