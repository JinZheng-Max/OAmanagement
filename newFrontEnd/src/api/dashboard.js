import http from './http'

/** 获取超级管理员运维大屏统计算法数据包 */
export async function getAdminDashboardStats() {
  const res = await http.get('/departments/dashboard/admin/stats')
  return res.data
}
