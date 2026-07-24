<template>
  <aside class="sidebar">
    <div class="sidebar-bg"></div>
    <div class="sidebar-glow"></div>
    <div class="sidebar-header">
      <div class="brand">
        <div class="brand-icon animate-float">
          <el-icon><OfficeBuilding /></el-icon>
        </div>
        <div class="brand-text">
          <span class="brand-name">智办办公</span>
          <span class="brand-subtitle">智能协同平台</span>
        </div>
      </div>
    </div>
    <nav class="sidebar-nav">
      <el-menu :default-active="activeMenu" class="sidebar-menu" router>
        <div class="menu-section">
          <span class="section-label">业务中心</span>
          <el-menu-item index="/dashboard" class="menu-item">
            <div class="menu-icon-wrap">
              <el-icon class="menu-icon-spin"><Odometer /></el-icon>
            </div>
            <span>{{ isAdmin ? '数据统计大屏' : '我的工作台' }}</span>
          </el-menu-item>
          <el-menu-item index="/ai-assistant" class="menu-item">
            <div class="menu-icon-wrap">
              <el-icon class="menu-icon-bounce"><ChatDotRound /></el-icon>
            </div>
            <span>AI 智能问答</span>
          </el-menu-item>
          <el-menu-item index="/attendance" class="menu-item">
            <div class="menu-icon-wrap">
              <el-icon class="menu-icon-pulse"><Checked /></el-icon>
            </div>
            <span>{{ isAdmin ? '全员考勤看板' : (isDeptAdmin ? '部门考勤管理' : '考勤签到') }}</span>
          </el-menu-item>
          <el-menu-item index="/leave" class="menu-item">
            <div class="menu-icon-wrap">
              <el-icon class="menu-icon-wiggle"><Calendar /></el-icon>
            </div>
            <span>{{ isAdmin ? '全员请假审批' : (isDeptAdmin ? '部门请假审批' : '请假申请') }}</span>
          </el-menu-item>
          <el-menu-item index="/content" class="menu-item">
            <div class="menu-icon-wrap">
              <el-icon class="menu-icon-float"><Document /></el-icon>
            </div>
            <span>公告制度中心</span>
          </el-menu-item>
          <el-menu-item index="/org" class="menu-item">
            <div class="menu-icon-wrap">
              <el-icon class="menu-icon-rotate"><Briefcase /></el-icon>
            </div>
            <span>{{ isAdmin ? '组织与员工管理' : '部门成员档案' }}</span>
          </el-menu-item>
        </div>
      </el-menu>
    </nav>
    <div class="sidebar-footer">
      <div class="user-card">
        <div class="user-avatar">
          <el-icon><User /></el-icon>
        </div>
        <div class="user-info">
          <span class="user-name">{{ username }}</span>
          <span class="user-role">{{ roleText }}</span>
        </div>
      </div>
    </div>
  </aside>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ChatDotRound, Checked, Calendar, Document, Odometer, Briefcase, OfficeBuilding, User } from '@element-plus/icons-vue'

const route = useRoute()
const activeMenu = computed(() => route.path)
const storedRole = ref(localStorage.getItem('role') || 'EMPLOYEE')

const isAdmin = computed(() => storedRole.value === 'SUPER_ADMIN' || storedRole.value === 'ADMIN')
const isDeptAdmin = computed(() => storedRole.value === 'DEPT_MANAGER')
const username = ref(localStorage.getItem('username') || '未知用户')

const roleText = computed(() => {
  switch (storedRole.value) {
    case 'SUPER_ADMIN':
    case 'ADMIN':
      return '超级管理员'
    case 'DEPT_MANAGER':
      return '部门经理'
    default:
      return '员工'
  }
})
</script>

<style scoped>
.sidebar {
  width: 250px;
  height: 100vh;
  position: fixed;
  top: 0;
  left: 0;
  display: flex;
  flex-direction: column;
  box-shadow: 6px 0 40px rgba(0, 0, 0, 0.4);
  z-index: 100;
  overflow: hidden;
  background: rgba(26, 26, 46, 0.95);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
}

.sidebar-bg {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: 
    radial-gradient(ellipse at 20% 20%, rgba(102, 126, 234, 0.15) 0%, transparent 50%),
    radial-gradient(ellipse at 80% 80%, rgba(118, 75, 162, 0.1) 0%, transparent 50%),
    radial-gradient(ellipse at 50% 50%, rgba(79, 172, 254, 0.05) 0%, transparent 60%);
  pointer-events: none;
  z-index: 0;
}

.sidebar-glow {
  position: absolute;
  top: -100px;
  right: -50px;
  width: 300px;
  height: 300px;
  background: radial-gradient(circle, rgba(102, 126, 234, 0.1) 0%, transparent 70%);
  animation: glowPulse 4s ease-in-out infinite;
  pointer-events: none;
  z-index: 0;
}

@keyframes glowPulse {
  0%, 100% {
    opacity: 0.5;
    transform: scale(1);
  }
  50% {
    opacity: 1;
    transform: scale(1.1);
  }
}

.sidebar-header {
  padding: 28px 24px;
  background: rgba(102, 126, 234, 0.08);
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  position: relative;
  z-index: 1;
}

.brand {
  display: flex;
  align-items: center;
  gap: 14px;
}

.brand-icon {
  width: 48px;
  height: 48px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 6px 20px rgba(102, 126, 234, 0.4);
  transition: all 0.3s ease;
}

.brand-icon:hover {
  transform: scale(1.05);
  box-shadow: 0 8px 28px rgba(102, 126, 234, 0.5);
}

.brand-icon :deep(.el-icon) {
  font-size: 26px;
  color: white;
}

.brand-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.brand-name {
  font-size: 18px;
  font-weight: 700;
  color: white;
  letter-spacing: 0.5px;
}

.brand-subtitle {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.4);
}

.sidebar-nav {
  flex: 1;
  padding: 20px 0;
  overflow-y: auto;
}

.sidebar-menu {
  border-right: none;
  background: transparent;
}

.menu-section {
  padding: 8px 16px;
}

.section-label {
  display: block;
  padding: 8px 16px 6px;
  font-size: 11px;
  color: rgba(255, 255, 255, 0.5);
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 1px;
}

.sidebar-menu :deep(.el-menu-item) {
  height: 48px;
  line-height: 48px;
  margin: 4px 10px;
  border-radius: 12px;
  color: #ffffff !important;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  padding-left: 16px;
  overflow: hidden;
}

.sidebar-menu :deep(.el-menu-item)::after {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(102, 126, 234, 0.1), transparent);
  transition: left 0.5s ease;
}

.sidebar-menu :deep(.el-menu-item:hover)::after {
  left: 100%;
}

.sidebar-menu :deep(.el-menu-item) span,
.sidebar-menu :deep(.el-menu-item) .el-tooltip,
.sidebar-menu :deep(.el-menu-item) * {
  color: #ffffff !important;
}

.sidebar-menu :deep(.el-menu-item:hover) {
  background: rgba(102, 126, 234, 0.18);
  transform: translateX(4px);
}

.sidebar-menu :deep(.el-menu-item:hover) span,
.sidebar-menu :deep(.el-menu-item:hover) .el-tooltip,
.sidebar-menu :deep(.el-menu-item:hover) * {
  color: #667eea !important;
}

.sidebar-menu :deep(.el-menu-item.is-active) {
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.3) 0%, rgba(118, 75, 162, 0.3) 100%);
  box-shadow: 0 4px 16px rgba(102, 126, 234, 0.3);
  transform: translateX(2px);
}

.sidebar-menu :deep(.el-menu-item.is-active) span,
.sidebar-menu :deep(.el-menu-item.is-active) .el-tooltip,
.sidebar-menu :deep(.el-menu-item.is-active) * {
  color: #667eea !important;
}

.sidebar-menu :deep(.el-menu-item.is-active)::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 4px;
  height: 24px;
  background: linear-gradient(180deg, #667eea 0%, #764ba2 100%);
  border-radius: 0 4px 4px 0;
  box-shadow: 0 0 10px rgba(102, 126, 234, 0.5);
}

.menu-icon-wrap {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.08);
  margin-right: 12px;
  transition: all 0.25s ease;
}

.sidebar-menu :deep(.el-menu-item:hover) .menu-icon-wrap {
  background: rgba(102, 126, 234, 0.2);
}

.sidebar-menu :deep(.el-menu-item.is-active) .menu-icon-wrap {
  background: rgba(102, 126, 234, 0.3);
}

.menu-icon-wrap :deep(.el-icon) {
  font-size: 17px;
  color: #ffffff !important;
}

.sidebar-menu :deep(.el-menu-item:hover) .menu-icon-wrap .el-icon {
  color: #667eea !important;
}

.sidebar-menu :deep(.el-menu-item.is-active) .menu-icon-wrap .el-icon {
  color: #667eea !important;
}

.sidebar-footer {
  padding: 16px 20px;
  background: rgba(0, 0, 0, 0.2);
  border-top: 1px solid rgba(255, 255, 255, 0.06);
}

.user-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 14px;
  background: rgba(255, 255, 255, 0.04);
  border-radius: 12px;
  transition: all 0.25s ease;
}

.user-card:hover {
  background: rgba(255, 255, 255, 0.08);
}

.user-avatar {
  width: 38px;
  height: 38px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.user-avatar :deep(.el-icon) {
  font-size: 18px;
  color: white;
}

.user-info {
  display: flex;
  flex-direction: column;
  gap: 1px;
  flex: 1;
}

.user-name {
  font-size: 14px;
  font-weight: 600;
  color: white;
}

.user-role {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.45);
}

.sidebar-nav::-webkit-scrollbar {
  width: 4px;
}

.sidebar-nav::-webkit-scrollbar-track {
  background: transparent;
}

.sidebar-nav::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.1);
  border-radius: 2px;
}

.sidebar-nav::-webkit-scrollbar-thumb:hover {
  background: rgba(255, 255, 255, 0.2);
}

.menu-icon-spin {
  animation: menu-spin 8s linear infinite;
}

.menu-icon-bounce {
  animation: menu-bounce 2s ease-in-out infinite;
}

.menu-icon-pulse {
  animation: menu-pulse 2s ease-in-out infinite;
}

.menu-icon-wiggle {
  animation: menu-wiggle 1.5s ease-in-out infinite;
}

.menu-icon-float {
  animation: menu-float 3s ease-in-out infinite;
}

.menu-icon-rotate {
  animation: menu-rotate 6s ease-in-out infinite;
}

@keyframes menu-spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

@keyframes menu-bounce {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-3px); }
}

@keyframes menu-pulse {
  0%, 100% { transform: scale(1); opacity: 0.8; }
  50% { transform: scale(1.15); opacity: 1; }
}

@keyframes menu-wiggle {
  0%, 100% { transform: rotate(-3deg); }
  50% { transform: rotate(3deg); }
}

@keyframes menu-float {
  0%, 100% { transform: translateY(0) rotate(0deg); }
  25% { transform: translateY(-2px) rotate(1deg); }
  75% { transform: translateY(2px) rotate(-1deg); }
}

@keyframes menu-rotate {
  0%, 100% { transform: perspective(500px) rotateY(0deg); }
  50% { transform: perspective(500px) rotateY(180deg); }
}
</style>