import { http, ApiResult } from './http'

export interface ContentSaveDTO {
  id?: number
  type: string
  title: string
  category?: string
  body: string
  scope?: string
  accessDepartmentId?: number
}

export interface ContentQueryDTO {
  type?: string
  category?: string
  status?: string
  page?: number
  size?: number
}

export interface ContentSearchDTO {
  keyword?: string
  type?: string
  category?: string
  startDate?: string
  endDate?: string
  page?: number
  size?: number
}

export interface ContentDetailVO {
  id: number
  type: string
  title: string
  category?: string
  body: string
  status: string
  scope: string
  accessDepartmentId?: number
  publisherId?: number
  publishTime?: string
  version?: number
  viewCount?: number
  highlightTitle?: string
  highlightBody?: string
  createTime?: string
  updateTime?: string
}

export interface PageResult<T> {
  list: T[]
  total: number
  page: number
  size: number
}

export interface EsReindexResultVO {
  total: number
  successCount: number
  failureCount: number
  costMillis: number
  message: string
}

export async function saveDraftApi(data: ContentSaveDTO): Promise<ContentDetailVO> {
  const res = await http.post<ApiResult<ContentDetailVO>>('/contents/draft', data)
  return res.data.data
}

export async function deleteDraftApi(id: number): Promise<void> {
  await http.delete(`/contents/draft/${id}`)
}

export async function publishContentApi(id: number): Promise<ContentDetailVO> {
  const res = await http.post<ApiResult<ContentDetailVO>>(`/contents/${id}/publish`)
  return res.data.data
}

export async function unpublishContentApi(id: number): Promise<void> {
  await http.post(`/contents/${id}/unpublish`)
}

export async function getAdminContentsApi(params: ContentQueryDTO): Promise<PageResult<ContentDetailVO>> {
  const res = await http.get<ApiResult<PageResult<ContentDetailVO>>>('/contents/admin/page', { params })
  return res.data.data
}

export async function getEmployeeContentsApi(params: ContentQueryDTO): Promise<PageResult<ContentDetailVO>> {
  const res = await http.get<ApiResult<PageResult<ContentDetailVO>>>('/contents/page', { params })
  return res.data.data
}

export async function getContentDetailApi(id: number): Promise<ContentDetailVO> {
  const res = await http.get<ApiResult<ContentDetailVO>>(`/contents/${id}`)
  return res.data.data
}

export async function searchContentsApi(params: ContentSearchDTO): Promise<PageResult<ContentDetailVO>> {
  const res = await http.get<ApiResult<PageResult<ContentDetailVO>>>('/contents/search', { params })
  return res.data.data
}

export async function reindexEsApi(): Promise<EsReindexResultVO> {
  const res = await http.post<ApiResult<EsReindexResultVO>>('/contents/admin/reindex')
  return res.data.data
}
