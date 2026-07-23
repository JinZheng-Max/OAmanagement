import http from './http'

/** 创建/修改草稿或内容 (管理员) */
export async function saveDraftApi(data) {
  const res = await http.post('/contents/draft', data)
  return res.data
}

/** 删除草稿 (管理员) */
export async function deleteDraftApi(id) {
  const res = await http.delete(`/contents/draft/${id}`)
  return res.data
}

/** 发布公告制度 */
export async function publishContentApi(id) {
  const res = await http.post(`/contents/${id}/publish`)
  return res.data
}

/** 下架公告制度 */
export async function unpublishContentApi(id) {
  const res = await http.post(`/contents/${id}/unpublish`)
  return res.data
}

/** 管理员分页查询公告列表 */
export async function getAdminContentsApi(params) {
  const res = await http.get('/contents/admin/page', { params })
  return res.data
}

/** 员工浏览已发布公告制度列表 */
export async function getEmployeeContentsApi(params) {
  const res = await http.get('/contents/page', { params })
  return res.data
}

/** 查看公告制度详情 */
export async function getContentDetailApi(id) {
  const res = await http.get(`/contents/${id}`)
  return res.data
}

/** 关键词全文检索 (ES 搜索引擎) */
export async function searchContentsApi(params) {
  const res = await http.get('/contents/search', { params })
  return res.data
}

/** 手动触发 ES 索引全量重建 */
export async function reindexEsApi() {
  const res = await http.post('/contents/admin/reindex')
  return res.data
}
