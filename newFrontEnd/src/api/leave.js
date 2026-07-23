import http from './http'

/** 分页查询请假申请与审批列表 */
export async function getLeavePage(params) {
  const res = await http.get('/leaves', { params })
  return res.data
}

/** 查询指定请假申请详情 */
export async function getLeaveById(id) {
  const res = await http.get(`/leaves/${id}`)
  return res.data
}

/** 提交请假申请 */
export async function createLeave(data) {
  const res = await http.post('/leaves', data)
  return res.data
}

/** 撤回请假申请 */
export async function withdrawLeave(id) {
  const res = await http.post(`/leaves/${id}/withdraw`)
  return res.data
}

/** 审批请假申请 */
export async function auditLeave(id, data) {
  const res = await http.post(`/leaves/${id}/audit`, data)
  return res.data
}

/** 上传请假附件 */
export async function uploadAttachment(leaveId, file) {
  const formData = new FormData()
  formData.append('file', file)
  const res = await http.post(`/leaves/${leaveId}/attachments`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
  return res.data
}

/** 获取请假单据附件列表 */
export async function getAttachments(leaveId) {
  const res = await http.get(`/leaves/${leaveId}/attachments`)
  return res.data
}

/** 获取代理下载链接 */
export function getDownloadUrl(attachId) {
  return `/api/leaves/attachments/${attachId}/download`
}
