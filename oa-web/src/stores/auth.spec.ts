import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useAuthStore } from './auth'
import * as authApi from '../api/auth'

vi.mock('../api/auth', () => ({ login: vi.fn(), logout: vi.fn() }))
const mockedLogin = vi.mocked(authApi.login)
const mockedLogout = vi.mocked(authApi.logout)

describe('auth store', () => {
  beforeEach(() => setActivePinia(createPinia()))

  it('persists a successful login without storing a password', async () => {
    mockedLogin.mockResolvedValue({ token: 'signed-token', tokenType: 'Bearer', expiresIn: 1800,
      userInfo: { id: 1, username: 'alice', role: 'EMPLOYEE', employeeId: 9 } })
    const store = useAuthStore()
    await store.login('alice', 'StrongPass1')
    expect(store.isAuthenticated).toBe(true)
    expect(localStorage.getItem('oa_token')).toBe('signed-token')
    expect(localStorage.getItem('oa_user')).not.toContain('StrongPass1')
  })

  it('clears local state even when remote logout fails', async () => {
    localStorage.setItem('oa_token', 'old-token')
    mockedLogout.mockRejectedValue(new Error('network unavailable'))
    const store = useAuthStore(); store.token = 'old-token'
    await expect(store.logout()).rejects.toThrow('network unavailable')
    expect(store.token).toBeNull(); expect(localStorage.getItem('oa_token')).toBeNull()
  })
})
