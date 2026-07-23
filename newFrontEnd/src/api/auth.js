import http from './http'

/** 用户登录 */
export async function login(username, password) {
  const res = await http.post('/auth/login', { username, password })
  return res.data
}

/** 用户登出 */
export async function logout() {
  const res = await http.post('/auth/logout')
  return res.data
}

/** 获取当前登录用户及关联员工详情 */
export async function getMyProfile() {
  const res = await http.get('/auth/me')
  return res.data
}

/** 修改个人密码 */
export async function changePassword(oldPassword, newPassword) {
  const res = await http.put('/auth/password', { oldPassword, newPassword })
  return res.data
}
