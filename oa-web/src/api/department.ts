import { http, ApiResult } from './http'
import type { EmployeeInfo } from './employee'

export interface DepartmentInfo {
  id: number
  code: string
  name: string
  leaderId: number | null
  leaderName: string | null
  employeeCount: number
  status: number
  sort: number
  createTime: string
  updateTime: string
}

export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

export interface CreateDepartmentRequest {
  code: string
  name: string
  leaderId?: number | null
  sort?: number
}

export interface UpdateDepartmentRequest {
  code?: string
  name?: string
  leaderId?: number | null
  sort?: number
}

export async function getDepartmentPage(params: {
  page: number; size: number; keyword?: string
}): Promise<PageResult<DepartmentInfo>> {
  const res = await http.get<ApiResult<PageResult<DepartmentInfo>>>('/departments', { params })
  return res.data.data
}

export async function getDepartmentById(id: number): Promise<DepartmentInfo> {
  const res = await http.get<ApiResult<DepartmentInfo>>(`/departments/${id}`)
  return res.data.data
}

export async function createDepartment(data: CreateDepartmentRequest): Promise<DepartmentInfo> {
  const res = await http.post<ApiResult<DepartmentInfo>>('/departments', data)
  return res.data.data
}

export async function updateDepartment(id: number, data: UpdateDepartmentRequest): Promise<DepartmentInfo> {
  const res = await http.put<ApiResult<DepartmentInfo>>(`/departments/${id}`, data)
  return res.data.data
}

export async function updateDepartmentStatus(id: number, status: number): Promise<void> {
  await http.put(`/departments/${id}/status`, { status })
}

export async function listActiveDepartments(): Promise<DepartmentInfo[]> {
  const res = await http.get<ApiResult<DepartmentInfo[]>>('/departments/active')
  return res.data.data
}

/** 我的部门详情响应 */
export interface MyDepartmentResponse {
  department: DepartmentInfo
  employees: EmployeeInfo[]
  totalCount: number
  positionCounts: Record<string, number>
  leader: EmployeeInfo | null
}

/** 获取当前员工所属部门详情 */
export async function getMyDepartment(): Promise<MyDepartmentResponse> {
  const res = await http.get<ApiResult<MyDepartmentResponse>>('/departments/my')
  return res.data.data
}
