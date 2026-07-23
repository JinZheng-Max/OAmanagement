import { http, type ApiResult } from './http'

export interface LeaveRecord {
  id: number
  applicantId: number
  applicantName: string
  applicantNo: string
  type: string
  startTime: string
  endTime: string
  reason: string
  status: string      // PENDING / APPROVED / REJECTED / WITHDRAWN
  createTime: string
  updateTime: string
  audits: {
    id: number
    auditorName: string
    action: string
    comment: string | null
    auditTime: string
  }[]
}

export interface PageResult<T> {
  records: T[]; total: number; size: number; current: number; pages: number
}

export async function getLeavePage(params: {
  page?: number; size?: number; status?: string; type?: string
}): Promise<PageResult<LeaveRecord>> {
  const res = await http.get<ApiResult<PageResult<LeaveRecord>>>('/leaves', { params })
  return res.data.data
}

export async function getLeaveById(id: number): Promise<LeaveRecord> {
  const res = await http.get<ApiResult<LeaveRecord>>(`/leaves/${id}`)
  return res.data.data
}

export async function createLeave(data: {
  type: string; startTime: string; endTime: string; reason: string
}): Promise<LeaveRecord> {
  const res = await http.post<ApiResult<LeaveRecord>>('/leaves', data)
  return res.data.data
}

export async function withdrawLeave(id: number): Promise<void> {
  await http.post(`/leaves/${id}/withdraw`)
}

export async function auditLeave(id: number, data: {
  action: string; comment?: string
}): Promise<void> {
  await http.post(`/leaves/${id}/audit`, data)
}

/** 附件信息 */
export interface AttachmentInfo {
  id: number; fileName: string; fileUrl: string; fileSize: number; mimeType: string
}

/** 上传附件 */
export async function uploadAttachment(leaveId: number, file: File): Promise<AttachmentInfo> {
  const formData = new FormData()
  formData.append('file', file)
  // 不手动设置 Content-Type，让浏览器自动带上 boundary 参数
  const res = await http.post<ApiResult<AttachmentInfo>>(`/leaves/${leaveId}/attachments`, formData)
  return res.data.data
}

/** 获取附件列表 */
export async function getAttachments(leaveId: number): Promise<AttachmentInfo[]> {
  const res = await http.get<ApiResult<AttachmentInfo[]>>(`/leaves/${leaveId}/attachments`)
  return res.data.data
}

/** 下载附件（后端代理流式下载） */
export function getDownloadUrl(attachId: number): string {
  return `/api/leaves/attachments/${attachId}/download`
}
