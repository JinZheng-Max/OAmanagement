<template>
  <header class="header">
    <div class="header-bg"></div>
    <div class="header-left">
      <div class="breadcrumbs">
        <el-icon class="breadcrumb-icon"><House /></el-icon>
        <span class="breadcrumb-divider">/</span>
        <span class="breadcrumb-item">{{ pageTitle }}</span>
      </div>
    </div>
    <div class="header-center">
      <h1 class="page-title">
        <span class="title-icon" :class="titleIconClass">
          <el-icon v-if="currentIcon === 'dashboard'" class="icon-spin"><Odometer /></el-icon>
          <el-icon v-else-if="currentIcon === 'ai'" class="icon-bounce"><ChatDotRound /></el-icon>
          <el-icon v-else-if="currentIcon === 'attendance'" class="icon-pulse"><Clock /></el-icon>
          <el-icon v-else-if="currentIcon === 'leave'" class="icon-wiggle"><Calendar /></el-icon>
          <el-icon v-else-if="currentIcon === 'content'" class="icon-float"><Document /></el-icon>
          <el-icon v-else-if="currentIcon === 'org'" class="icon-rotate"><OfficeBuilding /></el-icon>
        </span>
        {{ pageTitle }}
      </h1>
    </div>
    <div class="header-right">
      <div class="header-actions">
        <el-dropdown @command="handleCommand" class="user-dropdown">
          <div class="user-trigger">
            <div class="user-avatar">
              <el-icon><User /></el-icon>
            </div>
            <span class="user-name">{{ username }}</span>
            <el-icon class="arrow-icon"><ArrowDown /></el-icon>
          </div>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="profile">
                <el-icon><UserFilled /></el-icon>
                个人信息
              </el-dropdown-item>
              <el-dropdown-item command="logout">
                <el-icon><SwitchButton /></el-icon>
                退出登录
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </div>

    <el-dialog title="个人信息" v-model="showProfileDialog" width="520px" :close-on-click-modal="false" append-to-body align-center>
      <el-form :model="profileForm" label-width="100px" class="profile-form">
        <el-form-item label="工号">
          <el-input v-model="profileForm.employeeNo" disabled />
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="profileForm.name" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="部门">
          <el-input v-model="profileForm.departmentName" disabled />
        </el-form-item>
        <el-form-item label="职务">
          <el-input v-model="profileForm.position" disabled />
        </el-form-item>
        <el-form-item label="入职日期">
          <el-input v-model="profileForm.hireDate" disabled />
        </el-form-item>
        <el-form-item required>
          <template #label>手机号 <span style="color:red">*</span></template>
          <el-input v-model="profileForm.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item required>
          <template #label>身份证号 <span style="color:red">*</span></template>
          <el-input v-model="profileForm.idCard" placeholder="请输入身份证号" />
        </el-form-item>
        <el-form-item required>
          <template #label>邮箱 <span style="color:red">*</span></template>
          <el-input v-model="profileForm.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-divider>修改密码</el-divider>
        <el-form-item label="旧密码">
          <el-input v-model="profileForm.oldPassword" type="password" placeholder="如需修改密码请输入旧密码" show-password />
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="profileForm.newPassword" type="password" placeholder="请输入新密码" show-password />
        </el-form-item>
        <el-form-item label="确认密码">
          <el-input v-model="profileForm.confirmPassword" type="password" placeholder="请确认新密码" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showProfileDialog = false">取消</el-button>
        <el-button type="primary" :loading="saveLoading" @click="saveProfile">保存修改</el-button>
      </template>
    </el-dialog>
  </header>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ArrowDown, User, SwitchButton, House, Odometer, ChatDotRound, Clock, Calendar, Document, OfficeBuilding, UserFilled } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getMyProfile, logout, changePassword } from '../api/auth'
import { updateProfile } from '../api/org'

const router = useRouter()
const route = useRoute()
const username = ref(localStorage.getItem('username') || '未知用户')
const showProfileDialog = ref(false)
const saveLoading = ref(false)

const profileForm = ref({
  employeeNo: '',
  name: '',
  departmentName: '',
  position: '',
  hireDate: '',
  phone: '',
  idCard: '',
  email: '',
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const pageTitles = {
  '/dashboard': '数据中心',
  '/ai-assistant': 'AI助手',
  '/attendance': '考勤管理',
  '/leave': '请假管理',
  '/content': '公告制度',
  '/org': '组织管理'
}

const iconMap = {
  '/dashboard': 'dashboard',
  '/ai-assistant': 'ai',
  '/attendance': 'attendance',
  '/leave': 'leave',
  '/content': 'content',
  '/org': 'org'
}

const pageTitle = computed(() => pageTitles[route.path] || '智办OA')
const currentIcon = computed(() => iconMap[route.path] || 'dashboard')
const titleIconClass = computed(() => `icon-${currentIcon.value}`)

const loadProfile = async () => {
  const token = localStorage.getItem('token')
  if (!token) return
  try {
    const res = await getMyProfile()
    if (res && res.code === 200) {
      const user = res.data
      if (user.username) {
        username.value = user.username
        localStorage.setItem('username', user.username)
      }
      if (user.employee) {
        profileForm.value.employeeNo = user.employee.employeeNo || ''
        profileForm.value.name = user.employee.name || user.username || ''
        profileForm.value.departmentName = user.employee.departmentName || ''
        profileForm.value.position = user.employee.position || ''
        profileForm.value.hireDate = user.employee.hireDate || ''
        profileForm.value.phone = user.employee.phone || ''
        profileForm.value.idCard = user.employee.idCard || ''
        profileForm.value.email = user.employee.email || ''
      }
    }
  } catch (err) {
    if (err.response?.status !== 401) {
      console.error('加载用户信息失败', err)
    }
  }
}

const handleCommand = async (command) => {
  if (command === 'logout') {
    try {
      await logout()
    } catch (err) {
      console.error('登出异常', err)
    }
    localStorage.clear()
    ElMessage.success('已安全退出')
    router.push('/login')
  } else if (command === 'profile') {
    loadProfile()
    showProfileDialog.value = true
  }
}

const saveProfile = async () => {
  if (!profileForm.value.phone) {
    return ElMessage.warning('请输入手机号')
  }
  if (!profileForm.value.idCard) {
    return ElMessage.warning('请输入身份证号')
  }
  if (!profileForm.value.email) {
    return ElMessage.warning('请输入邮箱')
  }

  if (profileForm.value.newPassword) {
    if (!profileForm.value.oldPassword) {
      return ElMessage.warning('请输入旧密码')
    }
    if (!profileForm.value.confirmPassword) {
      return ElMessage.warning('请确认新密码')
    }
    if (profileForm.value.newPassword !== profileForm.value.confirmPassword) {
      return ElMessage.warning('两次输入的密码不一致')
    }
    if (profileForm.value.newPassword.length < 6) {
      return ElMessage.warning('新密码长度不能少于6位')
    }
  }

  saveLoading.value = true
  try {
    await updateProfile({
      name: profileForm.value.name,
      phone: profileForm.value.phone,
      idCard: profileForm.value.idCard,
      email: profileForm.value.email
    })
    
    if (profileForm.value.newPassword) {
      await changePassword(profileForm.value.oldPassword, profileForm.value.newPassword)
    }

    ElMessage.success('个人资料保存成功')
    showProfileDialog.value = false
    profileForm.value.oldPassword = ''
    profileForm.value.newPassword = ''
    profileForm.value.confirmPassword = ''
    loadProfile()
  } catch (err) {
    ElMessage.error(err.response?.data?.message || '保存失败')
  } finally {
    saveLoading.value = false
  }
}

onMounted(() => {
  loadProfile()
})
</script>

<style scoped>
.header {
  height: 72px;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-bottom: 1px solid rgba(255, 255, 255, 0.5);
  display: flex;
  align-items: center;
  padding: 0 32px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);
  position: sticky;
  top: 0;
  z-index: 50;
  overflow: hidden;
}

.header-bg {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: 
    radial-gradient(ellipse at 10% 50%, rgba(102, 126, 234, 0.08) 0%, transparent 50%),
    radial-gradient(ellipse at 90% 50%, rgba(118, 75, 162, 0.06) 0%, transparent 50%);
  pointer-events: none;
}

.header-left {
  flex: 1;
  position: relative;
  z-index: 1;
}

.breadcrumbs {
  display: flex;
  align-items: center;
  gap: 10px;
}

.breadcrumb-icon {
  font-size: 16px;
  color: #667eea;
}

.breadcrumb-divider {
  color: #cbd5e1;
  font-size: 14px;
}

.breadcrumb-item {
  font-size: 14px;
  color: #64748b;
  font-weight: 500;
}

.header-center {
  flex: 2;
  text-align: center;
  position: relative;
  z-index: 1;
}

.page-title {
  font-size: 22px;
  font-weight: 700;
  color: #1a1a2e;
  margin: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
}

.title-icon {
  font-size: 28px;
  color: #667eea;
}

.icon-spin {
  animation: icon-spin 8s linear infinite;
}

.icon-bounce {
  animation: icon-bounce 2s ease-in-out infinite;
}

.icon-pulse {
  animation: icon-pulse 2s ease-in-out infinite;
}

.icon-wiggle {
  animation: icon-wiggle 1.5s ease-in-out infinite;
}

.icon-float {
  animation: icon-float 3s ease-in-out infinite;
}

.icon-rotate {
  animation: icon-rotate 6s ease-in-out infinite;
}

@keyframes icon-spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

@keyframes icon-bounce {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-6px); }
}

@keyframes icon-pulse {
  0%, 100% { transform: scale(1); opacity: 1; }
  50% { transform: scale(1.1); opacity: 0.8; }
}

@keyframes icon-wiggle {
  0%, 100% { transform: rotate(-3deg); }
  50% { transform: rotate(3deg); }
}

@keyframes icon-float {
  0%, 100% { transform: translateY(0) rotate(0deg); }
  25% { transform: translateY(-4px) rotate(2deg); }
  75% { transform: translateY(4px) rotate(-2deg); }
}

@keyframes icon-rotate {
  0%, 100% { transform: perspective(1000px) rotateY(0deg); }
  50% { transform: perspective(1000px) rotateY(180deg); }
}

.header-right {
  flex: 1;
  display: flex;
  justify-content: flex-end;
  position: relative;
  z-index: 1;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-dropdown :deep(.el-dropdown-menu) {
  border-radius: 14px;
  border: none;
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.14);
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
}

.user-dropdown :deep(.el-dropdown-item) {
  padding: 12px 24px;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.25s ease;
  border-radius: 8px;
  margin: 4px 8px;
}

.user-dropdown :deep(.el-dropdown-item:hover) {
  background: rgba(102, 126, 234, 0.1);
  color: #667eea;
}

.user-trigger {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  padding: 8px 20px;
  border-radius: 14px;
  transition: all 0.3s ease;
  background: rgba(255, 255, 255, 0.6);
}

.user-trigger:hover {
  background: rgba(102, 126, 234, 0.1);
  transform: translateY(-2px);
}

.user-avatar {
  width: 40px;
  height: 40px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
  transition: all 0.3s ease;
}

.user-trigger:hover .user-avatar {
  box-shadow: 0 6px 20px rgba(102, 126, 234, 0.4);
  transform: scale(1.05);
}

.user-avatar :deep(.el-icon) {
  font-size: 19px;
  color: white;
}

.user-name {
  font-size: 14px;
  font-weight: 600;
  color: #1a1a2e;
}

.arrow-icon {
  font-size: 14px;
  color: #64748b;
  transition: transform 0.3s ease;
}

.user-trigger:hover .arrow-icon {
  transform: rotate(180deg);
}

.profile-form :deep(.el-form-item) {
  margin-bottom: 18px;
}

.profile-form :deep(.el-input__wrapper) {
  border-radius: 10px;
}

.profile-form :deep(.el-divider) {
  margin: 24px 0 20px;
}
</style>