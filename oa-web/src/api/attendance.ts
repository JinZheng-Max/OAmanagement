import { http, ApiResult } from './http'
export interface AttendanceRecord {
  id: number
  employeeId: number
  employeeName: string
  workDate: string
  checkIn: string | null
  checkOut: string | null
  checkInIp: string | null
  checkOutIp: string | null
  status: 'CHECKED_IN' | 'CHECKED_OUT' | 'UNCHECKED' | 'LEAVE_EARLY'
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

/** 签到 */
export async function checkIn(): Promise<AttendanceRecord> {
  const res = await http.post<ApiResult<AttendanceRecord>>('/attendance/check-in')
  return res.data.data
}

/** 签退 */
export async function checkOut(): Promise<AttendanceRecord> {
  const res = await http.post<ApiResult<AttendanceRecord>>('/attendance/check-out')
  return res.data.data
}

/** 查询个人考勤记录 */
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
