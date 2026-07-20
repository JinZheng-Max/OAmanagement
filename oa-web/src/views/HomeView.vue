<script setup lang="ts">
/**
 * 首页 — 工作台（含侧边导航栏）
 *
 * 这里改造成一个布局页面：左侧是导航菜单，右侧是内容区
 * 后续其他功能模块（部门、考勤、请假等）都会在左侧添加导航项
 */
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const router = useRouter()
const route = useRoute()

/** 退出登录 */
async function signOut() {
  await auth.logout()
  await router.replace('/login')
}

/** 菜单点击导航 */
function handleMenuSelect(index: string) {
  router.push(index)
}
</script>

<template>
  <div class="layout">
    <!-- ===== 侧边栏 ===== -->
    <aside class="sidebar">
      <div class="logo">
        <strong>智办 AI OA</strong>
        <span class="version">v0.1</span>
      </div>

      <!-- el-menu 是 Element Plus 的导航菜单组件 -->
      <!-- :default-active 绑定 route.path 让菜单高亮跟随路由变化 -->
      <el-menu
        :default-active="route.path"
        @select="handleMenuSelect"
        router
        class="sidebar-menu"
      >
        <el-menu-item index="/">
          <el-icon><span>🏠</span></el-icon>
          <span>工作台</span>
        </el-menu-item>

        <!-- 个人信息（仅普通员工可见） -->
        <el-menu-item v-if="!auth.isAdmin" index="/profile">
          <el-icon><User /></el-icon>
          <span>个人信息</span>
        </el-menu-item>
        <!-- 我的部门（仅普通员工可见） -->
        <el-menu-item v-if="!auth.isAdmin" index="/my-department">
          <el-icon><HomeFilled /></el-icon>
          <span>我的部门</span>
        </el-menu-item>

        <!-- 部门管理（仅管理员可见） -->
        <el-menu-item v-if="auth.isAdmin" index="/departments">
          <el-icon><span>🏢</span></el-icon>
          <span>部门管理</span>
        </el-menu-item>

        <!-- 员工管理（仅管理员可见） -->
        <el-menu-item v-if="auth.isAdmin" index="/employees">
          <el-icon><span>👥</span></el-icon>
          <span>员工管理</span>
        </el-menu-item>

        <!-- 后续模块预留位置（暂时注释掉） -->
        <!--
        <el-menu-item index="/departments">
          <el-icon><span>🏢</span></el-icon>
          <span>部门管理</span>
        </el-menu-item>
        <el-menu-item index="/attendance">
          <el-icon><span>📋</span></el-icon>
          <span>考勤管理</span>
        </el-menu-item>
        <el-menu-item index="/leaves">
          <el-icon><span>📝</span></el-icon>
          <span>请假审批</span>
        </el-menu-item>
        -->
      </el-menu>

      <!-- 底部用户信息 -->
      <div class="sidebar-footer">
        <div class="user-info">
          <span class="username">{{ auth.user?.username }}</span>
          <el-tag size="small" :type="auth.isAdmin ? 'danger' : 'info'">
            {{ auth.isAdmin ? '管理员' : '员工' }}
          </el-tag>
        </div>
        <el-button size="small" @click="signOut">退出</el-button>
      </div>
    </aside>

    <!-- ===== 主内容区 ===== -->
    <main class="main-content">
      <!--
        router-view 用于显示子路由的内容：
        / 路径 → HomeView 原本的内容（工作台）
        /employees → EmployeeListView
        其他路由后续添加
      -->
      <router-view />
    </main>
  </div>
</template>

<style scoped>
.layout {
  display: flex;
  height: 100vh;
  background: #f2f5fa;
}

/* 侧边栏 */
.sidebar {
  width: 220px;
  background: #fff;
  border-right: 1px solid #e4e7ed;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
}

.logo {
  padding: 20px 16px 12px;
  font-size: 18px;
  color: #172033;
  display: flex;
  align-items: center;
  gap: 8px;
}

.version {
  font-size: 11px;
  color: #999;
  font-weight: normal;
}

.sidebar-menu {
  border-right: none;
  flex: 1;
}

/* 侧边栏底部 */
.sidebar-footer {
  padding: 12px 16px;
  border-top: 1px solid #e4e7ed;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.user-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.username {
  font-size: 13px;
  color: #333;
}

/* 主内容区 */
.main-content {
  flex: 1;
  overflow-y: auto;
  padding: 0;
}
</style>
