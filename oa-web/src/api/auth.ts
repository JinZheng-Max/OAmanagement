import { http, type ApiResult } from './http'

export interface LoginResponse {
  token: string
  tokenType: 'Bearer'
  expiresIn: number
  userInfo: { id: number; username: string; role: 'ADMIN' | 'EMPLOYEE'; employeeId: number | null }
}
export async function login(username: string, password: string) {
  const response = await http.post<ApiResult<LoginResponse>>('/auth/login', { username, password })
  return response.data.data
}
export async function logout() { await http.post('/auth/logout') }
