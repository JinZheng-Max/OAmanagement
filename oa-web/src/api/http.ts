import axios from 'axios'

export interface ApiResult<T> { code: number; message: string; data: T; traceId: string }

export const http = axios.create({ baseURL: '/api', timeout: 15000 })
http.interceptors.request.use((config) => {
  const token = localStorage.getItem('oa_token')
  if (token && config.url !== '/auth/login') config.headers.Authorization = `Bearer ${token}`
  return config
})
http.interceptors.response.use((response) => response, (error) => {
  if (error.response?.status === 401) {
    localStorage.removeItem('oa_token')
    localStorage.removeItem('oa_user')
    if (location.pathname !== '/login') location.assign('/login')
  }
  return Promise.reject(error)
})
