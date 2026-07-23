import { http, ApiResult } from './http'

export interface AttendanceRuleInfo {
  id?: number
  departmentId: number
  departmentName?: string
  sessionName: string
  checkInStartTime: string
  normalCheckInEndTime: string
  checkInEndTime: string
  normalCheckOutStartTime: string
  checkOutEndTime: string
  enabled?: number
  createTime?: string
  updateTime?: string
}

/** 查询部门考勤规则列表 */
export async function getDepartmentRules(departmentId?: number): Promise<AttendanceRuleInfo[]> {
  const res = await http.get<ApiResult<AttendanceRuleInfo[]>>('/attendance/rules', {
    params: { departmentId }
  })
  return res.data.data
}

/** 新建或更新部门考勤规则 */
export async function saveDepartmentRule(rule: AttendanceRuleInfo): Promise<AttendanceRuleInfo> {
  const res = await http.post<ApiResult<AttendanceRuleInfo>>('/attendance/rules', rule)
  return res.data.data
}

/** 删除部门考勤规则 */
export async function deleteDepartmentRule(id: number): Promise<void> {
  await http.delete(`/attendance/rules/${id}`)
}

/** 手动触发部门发布考勤任务 */
export async function publishDepartmentAttendance(departmentId: number, targetDate?: string, sessionName?: string): Promise<string> {
  const res = await http.post<ApiResult<string>>('/attendance/publish-department', null, {
    params: { departmentId, targetDate, sessionName }
  })
  return res.data.message || '发布成功'
}
