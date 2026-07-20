import { http, type ApiResult } from './http'

export interface LoginResponse {
  token: string
  tokenType: 'Bearer'
  expiresIn: number
  userInfo: { id: number; username: string; role: 'ADMIN' | 'EMPLOYEE'; employeeId: number | null }
}

/** 个人信息（含员工档案） */
export interface UserProfile {
  id: number
  username: string
  role: string
  status: number
  employeeId: number | null
  employee: {
    id: number
    employeeNo: string
    name: string
    departmentId: number | null
    departmentName: string | null
    position: string | null
    phone: string | null
    status: number
    hireDate: string | null
  } | null
}

export async function login(username: string, password: string) {
  const response = await http.post<ApiResult<LoginResponse>>('/auth/login', { username, password })
  return response.data.data
}
export async function logout() { await http.post('/auth/logout') }

/** 获取当前登录用户的完整信息 */
export async function getMyProfile(): Promise<UserProfile> {
  const res = await http.get<ApiResult<UserProfile>>('/auth/me')
  return res.data.data
}
