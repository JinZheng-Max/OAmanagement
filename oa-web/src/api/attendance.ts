import { http, ApiResult } from './http'

export interface AttendanceRecord {
  id: number
  employeeId: number
  employeeName: string
  departmentName?: string
  workDate: string
  sessionName?: string
  checkIn: string | null
  checkOut: string | null
  checkInIp: string | null
  checkOutIp: string | null
  checkInStartTime?: string | null
  normalCheckInEndTime?: string | null
  checkInEndTime?: string | null
  normalCheckOutStartTime?: string | null
  checkOutEndTime?: string | null
  status: 'CHECKED_IN' | 'CHECKED_OUT' | 'UNCHECKED' | 'LATE' | 'EARLY_LEAVE' | 'ABSENT' | 'REPLENISHED'
  replenishStatus?: 'NONE' | 'PENDING' | 'APPROVED' | 'REJECTED'
  replenishReason?: string | null
  approverId?: number | null
  approverName?: string | null
  approveTime?: string | null
  approveComment?: string | null
  createTime: string
  updateTime: string
}

export interface AttendancePageResult {
  records: AttendanceRecord[]
  total: number
  size: number
  current: number
  pages: number
  statistics: {
    checkedIn: number
    checkedOut: number
    unchecked: number
  }
}

export interface QueryParams {
  page?: number
  size?: number
  startDate?: string
  endDate?: string
  employeeId?: number | null
  departmentId?: number | null
  status?: string
}

/** 查询员工今日考勤任务清单 */
export async function getTodayPersonalTasks(): Promise<AttendanceRecord[]> {
  const res = await http.get<ApiResult<AttendanceRecord[]>>('/attendance/today-tasks')
  return res.data.data
}

/** 签到打卡 */
export async function checkIn(attendanceId?: number): Promise<AttendanceRecord> {
  const res = await http.post<ApiResult<AttendanceRecord>>('/attendance/check-in', null, {
    params: { attendanceId }
  })
  return res.data.data
}

/** 签退打卡 */
export async function checkOut(attendanceId?: number): Promise<AttendanceRecord> {
  const res = await http.post<ApiResult<AttendanceRecord>>('/attendance/check-out', null, {
    params: { attendanceId }
  })
  return res.data.data
}

/** 申请考勤补签 */
export async function applyReplenishment(attendanceId: number, reason: string): Promise<AttendanceRecord> {
  const res = await http.post<ApiResult<AttendanceRecord>>('/attendance/replenish/apply', { attendanceId, reason })
  return res.data.data
}

/** 审批考勤补签 */
export async function approveReplenishment(attendanceId: number, approved: boolean, comment?: string): Promise<AttendanceRecord> {
  const res = await http.post<ApiResult<AttendanceRecord>>('/attendance/replenish/approve', { attendanceId, approved, comment })
  return res.data.data
}

/** 查询补签纪录列表 */
export async function getReplenishRecords(params: QueryParams & { replenishStatus?: string }): Promise<AttendancePageResult> {
  const res = await http.get<ApiResult<AttendancePageResult>>('/attendance/replenish/records', { params })
  return res.data.data
}

/** 查询个人历史考勤记录 */
export async function getPersonalRecords(params: QueryParams): Promise<AttendancePageResult> {
  const res = await http.get<ApiResult<AttendancePageResult>>('/attendance/records', { params })
  return res.data.data
}

/** 查询管理员考勤记录板 */
export async function getAdminRecords(params: QueryParams): Promise<AttendancePageResult> {
  const res = await http.get<ApiResult<AttendancePageResult>>('/attendance/admin/records', { params })
  return res.data.data
}

/** 管理员补录/新增考勤记录 */
export async function saveOrUpdateAdminRecord(record: Partial<AttendanceRecord>): Promise<AttendanceRecord> {
  const res = await http.post<ApiResult<AttendanceRecord>>('/attendance/admin/records', record)
  return res.data.data
}

/** 管理员修改考勤记录 */
export async function updateAdminRecord(id: number, record: Partial<AttendanceRecord>): Promise<AttendanceRecord> {
  const res = await http.put<ApiResult<AttendanceRecord>>(`/attendance/admin/records/${id}`, record)
  return res.data.data
}

/** 管理员手动触发发布当日考勤 */
export async function publishDailyAttendance(): Promise<string> {
  const res = await http.post<ApiResult<string>>('/attendance/admin/publish')
  return res.data.data
}

export interface PublishTaskParams {
  startDate: string
  endDate?: string
  checkInStart?: string
  checkInEnd?: string
  checkOutStart?: string
  checkOutEnd?: string
  departmentId?: number | null
}

/** 超级管理员发布指定时间段签到任务 */
export async function publishAttendanceTask(params: PublishTaskParams): Promise<string> {
  const res = await http.post<ApiResult<string>>('/attendance/admin/publish-task', params)
  return res.data.data
}
