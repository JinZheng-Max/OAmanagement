import { http, ApiResult } from './http'

// ---- TypeScript 类型定义 ----

/** 员工信息（对应后端的 EmployeeResponse） */
export interface EmployeeInfo {
  id: number
  employeeNo: string
  name: string
  departmentId: number | null
  departmentName: string | null
  position: string | null
  phone: string | null
  status: number        // 1-在职 0-离职
  hasAccount: boolean   // 是否已开通系统账号
  userId: number | null // 关联的系统用户ID（已开通账号时有值）
  hireDate: string | null
  createTime: string
  updateTime: string
}

/** 分页结果（对应后端的 PageResult<EmployeeResponse>） */
export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

/** 新增员工请求参数 */
export interface CreateEmployeeRequest {
  employeeNo: string
  name: string
  departmentId?: number | null
  position?: string | null
  phone?: string | null
  hireDate?: string | null
}

/** 编辑员工请求参数 */
export interface UpdateEmployeeRequest {
  name?: string
  departmentId?: number | null
  position?: string | null
  phone?: string | null
  status?: number
  hireDate?: string | null
}

/** 开通账号请求参数 */
export interface CreateAccountRequest {
  username?: string
}

// ---- API 函数 ----

/**
 * 分页查询员工列表
 *
 * async/await 是 JavaScript 处理异步操作的方式：
 * await 会等待接口返回结果再继续往下执行
 */
export async function getEmployeePage(params: {
  page: number
  size: number
  departmentId?: number | null
  name?: string
  employeeNo?: string
  phone?: string
}): Promise<PageResult<EmployeeInfo>> {
  const res = await http.get<ApiResult<PageResult<EmployeeInfo>>>('/employees', { params })
  return res.data.data
}

/** 查询员工详情 */
export async function getEmployeeById(id: number): Promise<EmployeeInfo> {
  const res = await http.get<ApiResult<EmployeeInfo>>(`/employees/${id}`)
  return res.data.data
}

/** 新增员工 */
export async function createEmployee(data: CreateEmployeeRequest): Promise<EmployeeInfo> {
  const res = await http.post<ApiResult<EmployeeInfo>>('/employees', data)
  return res.data.data
}

/** 更新员工 */
export async function updateEmployee(id: number, data: UpdateEmployeeRequest): Promise<EmployeeInfo> {
  const res = await http.put<ApiResult<EmployeeInfo>>(`/employees/${id}`, data)
  return res.data.data
}

/** 开通账号 */
export async function createAccount(id: number, data: CreateAccountRequest): Promise<string> {
  const res = await http.post<ApiResult<string>>(`/employees/${id}/account`, data)
  return res.data.data
}

/** 重置密码 */
export async function resetPassword(userId: number): Promise<string> {
  const res = await http.put<ApiResult<string>>(`/employees/account/${userId}/reset-password`)
  return res.data.data
}

/** 修改密码 */
export async function changePassword(oldPassword: string, newPassword: string): Promise<void> {
  await http.put('/auth/password', { oldPassword, newPassword })
}

/** 按部门ID查询员工列表 */
export async function getEmployeesByDepartment(departmentId: number): Promise<EmployeeInfo[]> {
  const res = await http.get<ApiResult<EmployeeInfo[]>>(`/employees/by-department/${departmentId}`)
  return res.data.data
}

/** 员工修改自己的信息（姓名、手机号） */
export async function updateProfile(data: { name?: string; phone?: string }): Promise<EmployeeInfo> {
  const res = await http.put<ApiResult<EmployeeInfo>>('/employees/profile', data)
  return res.data.data
}
