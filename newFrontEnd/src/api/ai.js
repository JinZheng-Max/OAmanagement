import http from './http'

export async function chat(question) {
  const res = await http.post('/ai/chat', { question })
  return res.data
}

export async function uploadSource(formData) {
  const res = await http.post('/ai/sources', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
  return res.data
}

export async function getSources(params = {}) {
  const res = await http.get('/ai/sources', { params })
  return res.data
}

export async function updateSourceAccess(id, data) {
  const res = await http.put(`/ai/sources/${id}/access`, data)
  return res.data
}

export async function updateSourceStatus(id, status) {
  const res = await http.put(`/ai/sources/${id}/status`, { status })
  return res.data
}

export async function uploadSourceVersion(id, formData) {
  const res = await http.post(`/ai/sources/${id}/version`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
  return res.data
}

export async function reindexSource(id) {
  const res = await http.post(`/ai/sources/${id}/reindex`)
  return res.data
}

export async function batchImport(params = {}) {
  const res = await http.post('/ai/batch-import', params)
  return res.data
}

export const sourceCategories = [
  { value: '企业文化', label: '企业文化' },
  { value: '新人培训', label: '新人培训' },
  { value: '技术部资料', label: '技术部资料' },
  { value: '人事制度', label: '人事制度' },
  { value: '财务制度', label: '财务制度' },
  { value: '行政管理', label: '行政管理' },
  { value: '法务合规', label: '法务合规' },
  { value: '信息安全', label: '信息安全' },
  { value: '部门资料', label: '部门资料' },
  { value: '其他', label: '其他' }
]

export const accessScopes = [
  { value: 'ALL', label: '全公司' },
  { value: 'DEPARTMENT', label: '指定部门' }
]

export const roleLevels = [
  { value: 1, label: '全员可见' },
  { value: 2, label: '经理级以上' },
  { value: 3, label: '仅管理员' }
]
