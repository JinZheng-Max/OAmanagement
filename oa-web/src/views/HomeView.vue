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

        <!-- 个人信息（普通员工 + 部门管理员可见） -->
        <el-menu-item v-if="!auth.isSuperAdmin" index="/profile">
          <el-icon><User /></el-icon>
          <span>个人信息</span>
        </el-menu-item>

        <!-- 我的部门（普通员工 + 部门管理员可见） -->
        <el-menu-item v-if="!auth.isSuperAdmin" index="/my-department">
          <el-icon><HomeFilled /></el-icon>
          <span>我的部门</span>
        </el-menu-item>

        <!-- 部门员工管理（仅部门管理员可见） -->
        <el-menu-item v-if="auth.isDeptManager" index="/dept-employees">
          <el-icon><span>👥</span></el-icon>
          <span>部门员工</span>
        </el-menu-item>

        <!-- 请假（管理员和部门经理：审批管理 / 员工：我的请假） -->
        <el-menu-item v-if="auth.isAdmin" index="/leaves">
          <el-icon><Document /></el-icon>
          <span>请假审批</span>
        </el-menu-item>
        <el-menu-item v-if="!auth.isAdmin" index="/leaves">
          <el-icon><Edit /></el-icon>
          <span>我的请假</span>
        </el-menu-item>

        <!-- 部门管理（仅超级管理员可见） -->
        <el-menu-item v-if="auth.isSuperAdmin" index="/departments">
          <el-icon><span>🏢</span></el-icon>
          <span>部门管理</span>
        </el-menu-item>

        <!-- 员工管理（仅超级管理员可见） -->
        <el-menu-item v-if="auth.isSuperAdmin" index="/employees">
          <el-icon><span>👥</span></el-icon>
          <span>员工管理</span>
        </el-menu-item>

        <!-- AI 智能问答 -->
        <el-menu-item index="/ai-chat">
          <el-icon><span>🤖</span></el-icon>
          <span>智能问答</span>
        </el-menu-item>

        <!-- 知识库管理 -->
        <el-menu-item index="/ai-sources">
          <el-icon><span>📚</span></el-icon>
          <span>知识库管理</span>
        </el-menu-item>

        <!-- 考勤管理 -->
        <el-menu-item index="/attendance">
          <el-icon><span>📋</span></el-icon>
          <span>考勤管理</span>
        </el-menu-item>

        <!-- 公告制度与全文检索 -->
        <el-menu-item index="/contents">
          <el-icon><span>📢</span></el-icon>
          <span>公告制度</span>
        </el-menu-item>
      </el-menu>

      <!-- 底部用户信息 -->
      <div class="sidebar-footer">
        <div class="user-info">
          <span class="username">{{ auth.user?.username }}</span>
          <el-tag size="small" :type="auth.isSuperAdmin ? 'danger' : (auth.isDeptManager ? 'warning' : 'info')">
            {{ auth.isSuperAdmin ? '总管理员' : (auth.isDeptManager ? '部门经理' : '员工') }}
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
