import { http, ApiResult } from './http'

export interface SourceRef {
  id: string
  sourceId: number
  summary: string
}

export interface AnswerResult {
  question: string
  answer: string
  sources: SourceRef[]
}

export interface AiSource {
  id: number
  title: string
  category: string
  description: string
  originalFileName: string
  fileExtension: string
  fileSize: number
  parseStatus: string
  indexStatus: string
  accessScope: string
  accessDepartmentId: number | null
  minRoleLevel: number
  status: string
  version: number
  uploaderId: number
  createTime: string
  updateTime: string
}

export interface OaAiCitation {
  id: number
  sessionId: number
  sourceType: string
  sourceId: number
  chunkId: number | null
  sourceTitle: string
  fragmentSummary: string
  score: number | null
  sourceVersion: number | null
}

export async function askAI(question: string): Promise<AnswerResult> {
  const res = await http.post<ApiResult<AnswerResult>>('/ai/chat', { question })
  return res.data.data
}

/** 查询文档列表 */
export async function listSources(params?: {
  title?: string
  category?: string
  status?: string
}): Promise<AiSource[]> {
  const res = await http.get<ApiResult<AiSource[]>>('/ai/sources', { params })
  return res.data.data
}

/** 上传知识文档 */
export async function uploadSource(formData: FormData): Promise<{ id: number; title: string; status: string }> {
  const res = await http.post<ApiResult<{ id: number; title: string; status: string }>>('/ai/sources', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
  return res.data.data
}

/** 修改文档可见范围 */
export async function updateSourceAccess(id: number, data: {
  accessScope: string
  accessDepartmentId?: number | null
  minRoleLevel?: number
}): Promise<void> {
  await http.put(`/ai/sources/${id}/access`, data)
}

/** 启用或停用文档 */
export async function updateSourceStatus(id: number, status: string): Promise<void> {
  await http.put(`/ai/sources/${id}/status`, { status })
}

/** 重新索引文档 */
export async function reindexSource(id: number): Promise<void> {
  await http.post(`/ai/sources/${id}/reindex`)
}

/** 查询回答引用来源 */
export async function getSessionCitations(sessionId: number): Promise<OaAiCitation[]> {
  const res = await http.get<ApiResult<OaAiCitation[]>>(`/ai/sessions/${sessionId}/citations`)
  return res.data.data
}

/** 生成学习计划 */
export async function generateLearningPlan(topic: string): Promise<AnswerResult> {
  const res = await http.post<ApiResult<AnswerResult>>('/ai/learning-plans', { topic })
  return res.data.data
}

export async function getAiHealth(): Promise<any> {
  const res = await http.get<ApiResult<any>>('/ai/health')
  return res.data.data
}
