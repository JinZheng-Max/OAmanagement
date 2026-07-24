import http from './http'

// ======================== 部门接口 ========================

/** 分页查询部门 */
export async function getDepartmentPage(params) {
  const res = await http.get('/departments', { params })
  return res.data
}

/** 查询当前员工所属部门详情 */
export async function getMyDepartment() {
  const res = await http.get('/departments/my')
  return res.data
}

/** 查询所有启用中的部门列表 */
export async function listActiveDepartments() {
  const res = await http.get('/departments/active')
  return res.data
}

/** 查询指定部门详情 */
export async function getDepartmentById(id) {
  const res = await http.get(`/departments/${id}`)
  return res.data
}

/** 新增部门 */
export async function createDepartment(data) {
  const res = await http.post('/departments', data)
  return res.data
}

/** 修改部门 */
export async function updateDepartment(id, data) {
  const res = await http.put(`/departments/${id}`, data)
  return res.data
}

/** 启停部门状态 */
export async function updateDepartmentStatus(id, status) {
  const res = await http.put(`/departments/${id}/status`, { status })
  return res.data
}

/** 删除部门 */
export async function deleteDepartment(id) {
  const res = await http.delete(`/departments/${id}`)
  return res.data
}

// ======================== 员工接口 ========================

/** 分页查询员工列表 */
export async function getEmployeePage(params) {
  const res = await http.get('/employees', { params })
  return res.data
}

/** 按部门查询员工列表 */
export async function getEmployeesByDepartment(departmentId) {
  const res = await http.get(`/employees/by-department/${departmentId}`)
  return res.data
}

/** 查询员工详情 */
export async function getEmployeeById(id) {
  const res = await http.get(`/employees/${id}`)
  return res.data
}

/** 新增员工 */
export async function createEmployee(data) {
  const res = await http.post('/employees', data)
  return res.data
}

/** 更新员工档案 */
export async function updateEmployee(id, data) {
  const res = await http.put(`/employees/${id}`, data)
  return res.data
}

/** 员工修改个人资料 */
export async function updateProfile(data) {
  const res = await http.put('/employees/profile', data)
  return res.data
}

/** 为员工开通账号 */
export async function createAccount(id, data) {
  const res = await http.post(`/employees/${id}/account`, data)
  return res.data
}

/** 重置密码 */
export async function resetPassword(userId) {
  const res = await http.put(`/employees/account/${userId}/reset-password`)
  return res.data
}

/** 批量导入员工 (Excel / CSV) */
export async function importEmployees(file) {
  const formData = new FormData()
  formData.append('file', file)
  const res = await http.post('/employees/import', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
  return res.data
}
