import http from './http'

/** 查询员工今日打卡任务卡片 */
export async function getTodayPersonalTasks() {
  const res = await http.get('/attendance/today-tasks')
  return res.data
}

/** 签到打卡 */
export async function checkIn(attendanceId) {
  const res = await http.post('/attendance/check-in', null, {
    params: { attendanceId }
  })
  return res.data
}

/** 签退打卡 */
export async function checkOut(attendanceId) {
  const res = await http.post('/attendance/check-out', null, {
    params: { attendanceId }
  })
  return res.data
}

/** 申请考勤补签 */
export async function applyReplenishment(attendanceId, reason) {
  const res = await http.post('/attendance/replenish/apply', { attendanceId, reason })
  return res.data
}

/** 审批考勤补签 */
export async function approveReplenishment(attendanceId, approved, comment) {
  const res = await http.post('/attendance/replenish/approve', { attendanceId, approved, comment })
  return res.data
}

/** 查询补签纪录列表 */
export async function getReplenishRecords(params) {
  const res = await http.get('/attendance/replenish/records', { params })
  return res.data
}

/** 查询个人历史考勤记录 */
export async function getPersonalRecords(params) {
  const res = await http.get('/attendance/records', { params })
  return res.data
}

/** 【管理员】查询全员/部门考勤看板 */
export async function getAdminRecords(params) {
  const res = await http.get('/attendance/admin/records', { params })
  return res.data
}

/** 查询部门考勤规则 */
export async function getDepartmentRules(departmentId) {
  const res = await http.get('/attendance/rules', { params: { departmentId } })
  return res.data
}

/** 保存/修改部门考勤规则 */
export async function saveDepartmentRule(rule) {
  const res = await http.post('/attendance/rules', rule)
  return res.data
}

/** 删除部门考勤规则 */
export async function deleteDepartmentRule(id) {
  const res = await http.delete(`/attendance/rules/${id}`)
  return res.data
}

/** 管理员手动按部门发布考勤 */
export async function publishDepartmentAttendance(departmentId, targetDate, sessionName) {
  const res = await http.post('/attendance/publish-department', null, {
    params: { departmentId, targetDate, sessionName }
  })
  return res.data
}
