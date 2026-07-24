<template>
  <div class="login-container">
    <div class="login-bg"></div>
    <div class="bg-blobs">
      <div class="blob blob-1"></div>
      <div class="blob blob-2"></div>
      <div class="blob blob-3"></div>
      <div class="blob blob-4"></div>
      <div class="blob blob-5"></div>
    </div>
    <div class="floating-particles">
      <div class="particle" v-for="n in 30" :key="n" :style="getParticleStyle(n)"></div>
    </div>
    <div class="login-content">
      <div class="login-card">
        <div class="card-left">
          <div class="card-glow"></div>
          <div class="brand-section">
            <div class="logo">
              <el-icon><OfficeBuilding /></el-icon>
            </div>
            <h2>智办AI OA</h2>
            <p class="brand-slogan">智启办公新境界</p>
            <div class="feature-list">
              <div class="feature-item">
                <el-icon><TrendCharts /></el-icon>
                <span>AI智能助理</span>
              </div>
              <div class="feature-item">
                <el-icon><Clock /></el-icon>
                <span>高效考勤管理</span>
              </div>
              <div class="feature-item">
                <el-icon><Document /></el-icon>
                <span>智能审批流程</span>
              </div>
            </div>
          </div>
        </div>
        <div class="card-right">
          <div class="login-header">
            <h3>欢迎回来</h3>
            <p>请登录您的账号</p>
          </div>
          <el-form :model="form" label-position="top" class="login-form">
            <el-form-item label="账号">
              <el-input v-model="form.username" placeholder="请输入用户名" size="large">
                <template #prefix>
                  <el-icon><User /></el-icon>
                </template>
              </el-input>
            </el-form-item>
            <el-form-item label="密码">
              <el-input v-model="form.password" type="password" show-password placeholder="请输入密码" size="large">
                <template #prefix>
                  <el-icon><Lock /></el-icon>
                </template>
              </el-input>
            </el-form-item>
            <div class="remember-row">
              <el-checkbox v-model="rememberMe" class="remember-checkbox">记住账号和密码</el-checkbox>
            </div>
            <el-button type="primary" class="login-btn" :loading="loading" @click="onSubmit">
              <span>登 录</span>
              <el-icon class="btn-icon"><ArrowRight /></el-icon>
            </el-button>
          </el-form>
          
          <div class="login-footer">
            <span>管理员: admin / 123456</span>
            <span>员工: 工号 / 123456</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import axios from 'axios'
import { OfficeBuilding, User, Lock, ArrowRight, TrendCharts, Clock, Document } from '@element-plus/icons-vue'
import { login as loginApi } from '../api/auth'
import { getEmployeeById } from '../api/org'

const router = useRouter()
const loading = ref(false)
const rememberMe = ref(false)

const form = reactive({ username: '', password: '' })

onMounted(() => {
  const savedUser = localStorage.getItem('remember_username')
  const savedPass = localStorage.getItem('remember_password')
  const savedFlag = localStorage.getItem('remember_flag')
  if (savedFlag === 'true' && savedUser) {
    form.username = savedUser
    form.password = savedPass || ''
    rememberMe.value = true
  }
})

const getParticleStyle = (n) => {
  const left = (n * 17) % 100
  const top = (n * 23) % 100
  const size = 2 + (n % 6)
  const delay = n * 0.2
  const duration = 12 + (n % 8)
  return {
    left: `${left}%`,
    top: `${top}%`,
    width: `${size}px`,
    height: `${size}px`,
    animationDelay: `${delay}s`,
    animationDuration: `${duration}s`
  }
}

const onSubmit = async () => {
  if (!form.username || !form.password) return ElMessage.warning('请填写账号和密码')
  loading.value = true
  try {
    const res = await loginApi(form.username, form.password)
    if (res && res.code === 200) {
      const data = res.data || {}
      const token = data.token
      const user = data.userInfo || data.user || {}

      if (rememberMe.value) {
        localStorage.setItem('remember_username', form.username)
        localStorage.setItem('remember_password', form.password)
        localStorage.setItem('remember_flag', 'true')
      } else {
        localStorage.removeItem('remember_username')
        localStorage.removeItem('remember_password')
        localStorage.removeItem('remember_flag')
      }

      localStorage.setItem('token', token)
      localStorage.setItem('username', user.username || '')
      localStorage.setItem('role', user.role || '')
      localStorage.setItem('userId', user.id || '')
      localStorage.setItem('employeeId', user.employeeId || '')
      
      if (user.employeeId) {
        try {
          const empRes = await getEmployeeById(user.employeeId)
          if (empRes && empRes.code === 200 && empRes.data) {
            localStorage.setItem('departmentId', empRes.data.departmentId || '')
            localStorage.setItem('departmentName', empRes.data.departmentName || '')
          }
        } catch (ignored) {}
      }
      
      ElMessage.success('登录成功')
      window.location.href = '/dashboard'
    } else {
      ElMessage.error(res?.message || '登录失败')
    }
  } catch (err) {
    console.error('登录异常:', err)
    ElMessage.error(err.response?.data?.message || err.message || '网络或网关连接异常')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-container {
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  position: relative;
  overflow: hidden;
  background: linear-gradient(135deg, #0f0f23 0%, #1a1a2e 40%, #16213e 70%, #0f3460 100%);
}

.login-bg {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: 
    radial-gradient(circle at 10% 90%, rgba(102, 126, 234, 0.2) 0%, transparent 40%),
    radial-gradient(circle at 90% 10%, rgba(118, 75, 162, 0.18) 0%, transparent 40%),
    radial-gradient(circle at 50% 50%, rgba(79, 172, 254, 0.05) 0%, transparent 60%),
    radial-gradient(circle at 30% 20%, rgba(240, 147, 251, 0.1) 0%, transparent 50%),
    radial-gradient(circle at 70% 80%, rgba(16, 185, 129, 0.08) 0%, transparent 50%);
}

.bg-blobs {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  overflow: hidden;
}

.blob {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.4;
}

.blob-1 {
  width: 500px;
  height: 500px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  top: -150px;
  right: -100px;
  animation: blobFloat1 25s ease-in-out infinite;
}

.blob-2 {
  width: 400px;
  height: 400px;
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
  bottom: -100px;
  left: -80px;
  animation: blobFloat2 30s ease-in-out infinite reverse;
}

.blob-3 {
  width: 350px;
  height: 350px;
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
  top: 30%;
  left: 10%;
  animation: blobFloat3 22s ease-in-out infinite;
}

.blob-4 {
  width: 300px;
  height: 300px;
  background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%);
  bottom: 20%;
  right: 20%;
  animation: blobFloat4 28s ease-in-out infinite reverse;
}

.blob-5 {
  width: 250px;
  height: 250px;
  background: linear-gradient(135deg, #fc4a1a 0%, #f7b733 100%);
  top: 50%;
  right: 10%;
  animation: blobFloat5 24s ease-in-out infinite;
}

@keyframes blobFloat1 {
  0%, 100% { transform: translate(0, 0) scale(1); }
  25% { transform: translate(30px, -40px) scale(1.1); }
  50% { transform: translate(-20px, 30px) scale(0.95); }
  75% { transform: translate(25px, 20px) scale(1.05); }
}

@keyframes blobFloat2 {
  0%, 100% { transform: translate(0, 0) scale(1); }
  25% { transform: translate(-30px, 30px) scale(1.1); }
  50% { transform: translate(20px, -20px) scale(0.95); }
  75% { transform: translate(-25px, -30px) scale(1.05); }
}

@keyframes blobFloat3 {
  0%, 100% { transform: translate(0, 0) scale(1); }
  25% { transform: translate(40px, 20px) scale(1.05); }
  50% { transform: translate(-30px, -30px) scale(0.95); }
  75% { transform: translate(20px, 40px) scale(1.1); }
}

@keyframes blobFloat4 {
  0%, 100% { transform: translate(0, 0) scale(1); }
  25% { transform: translate(-40px, -20px) scale(1.05); }
  50% { transform: translate(30px, 30px) scale(0.95); }
  75% { transform: translate(-20px, -40px) scale(1.1); }
}

@keyframes blobFloat5 {
  0%, 100% { transform: translate(0, 0) scale(1); }
  25% { transform: translate(20px, 40px) scale(1.1); }
  50% { transform: translate(-20px, -20px) scale(0.95); }
  75% { transform: translate(40px, -30px) scale(1.05); }
}

.floating-particles {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  overflow: hidden;
}

.particle {
  position: absolute;
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.8), rgba(118, 75, 162, 0.8));
  border-radius: 50%;
  animation: particleFloat linear infinite;
}

@keyframes particleFloat {
  0% { transform: translateY(100vh) translateX(0) scale(1); opacity: 0; }
  10% { opacity: 1; }
  90% { opacity: 1; }
  100% { transform: translateY(-50px) translateX(30px) scale(0.5); opacity: 0; }
}

.login-content {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 900px;
  padding: 0 24px;
}

.login-card {
  display: flex;
  width: 100%;
  border-radius: 28px;
  overflow: hidden;
  box-shadow: 
    0 25px 80px rgba(0, 0, 0, 0.4),
    0 0 0 1px rgba(255, 255, 255, 0.1);
}

.card-left {
  flex: 1;
  min-width: 420px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 50%, #667eea 100%);
  position: relative;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
}

.card-glow {
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: radial-gradient(circle, rgba(255, 255, 255, 0.1) 0%, transparent 60%);
  animation: glowPulse 4s ease-in-out infinite;
}

@keyframes glowPulse {
  0%, 100% { opacity: 0.5; }
  50% { opacity: 0.8; }
}

.brand-section {
  position: relative;
  z-index: 1;
  text-align: center;
  padding: 60px 40px;
}

.logo {
  width: 100px;
  height: 100px;
  margin: 0 auto 32px;
  background: rgba(255, 255, 255, 0.2);
  backdrop-filter: blur(10px);
  border-radius: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 48px;
  color: white;
  box-shadow: 0 16px 40px rgba(0, 0, 0, 0.2);
  transition: all 0.3s ease;
  animation: logoFloat 3s ease-in-out infinite;
}

@keyframes logoFloat {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-10px); }
}

.brand-section h2 {
  font-size: 40px;
  font-weight: 800;
  color: white;
  margin: 0 0 16px 0;
  letter-spacing: -1px;
}

.brand-slogan {
  font-size: 18px;
  color: rgba(255, 255, 255, 0.9);
  margin: 0 0 48px 0;
  font-weight: 500;
  letter-spacing: 2px;
}

.feature-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 14px;
  color: rgba(255, 255, 255, 0.9);
  font-size: 15px;
  font-weight: 500;
  padding: 14px 20px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 16px;
  backdrop-filter: blur(10px);
  transition: all 0.3s ease;
}

.feature-item:hover {
  background: rgba(255, 255, 255, 0.15);
  transform: translateX(8px);
}

.feature-item el-icon {
  font-size: 20px;
}

.card-right {
  flex: 1;
  min-width: 420px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(30px);
  padding: 56px 50px;
  display: flex;
  flex-direction: column;
}

.login-header {
  margin-bottom: 40px;
  text-align: left;
}

.login-header h3 {
  font-size: 28px;
  font-weight: 700;
  color: #1a1a2e;
  margin: 0 0 8px 0;
}

.login-header p {
  font-size: 15px;
  color: #64748b;
  margin: 0;
}

.login-form {
  flex: 1;
  margin-bottom: 24px;
}

.login-form :deep(.el-form-item) {
  margin-bottom: 28px;
}

.login-form :deep(.el-form-item__label) {
  font-size: 15px;
  font-weight: 600;
  color: #334155;
  letter-spacing: 0.5px;
  margin-bottom: 12px;
}

.login-form :deep(.el-input__wrapper) {
  background: rgba(248, 250, 252, 0.9);
  border-radius: 16px;
  border: 2px solid rgba(0, 0, 0, 0.06);
  padding: 18px 20px;
  transition: all 0.3s ease;
}

.login-form :deep(.el-input__wrapper:hover) {
  border-color: rgba(102, 126, 234, 0.3);
  box-shadow: 0 0 0 4px rgba(102, 126, 234, 0.05);
}

.login-form :deep(.el-input__wrapper.is-focus) {
  border-color: #667eea;
  box-shadow: 0 0 0 5px rgba(102, 126, 234, 0.1);
}

.login-form :deep(.el-input__inner) {
  font-size: 16px;
  font-weight: 500;
  color: #1a1a2e;
  height: 28px;
  line-height: 28px;
}

.login-form :deep(.el-input__prefix) {
  color: #64748b;
}

.remember-row {
  margin-bottom: 24px;
  display: flex;
  align-items: center;
}

.remember-checkbox :deep(.el-checkbox__label) {
  font-size: 14px;
  color: #64748b;
  font-weight: 500;
}

.login-btn {
  width: 100%;
  height: 60px;
  font-size: 17px;
  font-weight: 700;
  border-radius: 18px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  box-shadow: 0 8px 24px rgba(102, 126, 234, 0.4);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  position: relative;
  overflow: hidden;
}

.login-btn::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.3), transparent);
  transition: left 0.6s ease;
}

.login-btn:hover {
  transform: translateY(-3px);
  box-shadow: 0 12px 36px rgba(102, 126, 234, 0.5);
}

.login-btn:hover::before {
  left: 100%;
}

.btn-icon {
  font-size: 19px;
  transition: transform 0.3s ease;
}

.login-btn:hover .btn-icon {
  transform: translateX(6px);
}

.login-footer {
  display: flex;
  justify-content: center;
  gap: 24px;
  padding-top: 24px;
  border-top: 1px solid rgba(0, 0, 0, 0.06);
}

.login-footer span {
  font-size: 12px;
  color: #94a3b8;
  background: rgba(102, 126, 234, 0.05);
  padding: 8px 16px;
  border-radius: 10px;
}
</style>