<script setup lang="ts">
/**
 * 首页工作台 — 欢迎页 / 仪表盘
 */
import { useAuthStore } from '../../stores/auth'
const auth = useAuthStore()
</script>

<template>
  <div class="dashboard">
    <div class="welcome-card">
      <h1>欢迎回来，{{ auth.user?.username }} 🎉</h1>
      <p class="subtitle">认证基线已就绪，请从左侧导航选择功能模块。</p>
    </div>

    <div class="info-cards">
      <div class="info-card">
        <div class="card-icon">👥</div>
        <div class="card-body">
          <h3>员工管理</h3>
          <p>管理员工档案、开通账号、重置密码</p>
        </div>
      </div>

      <div class="info-card coming-soon">
        <div class="card-icon">🏢</div>
        <div class="card-body">
          <h3>部门管理 <el-tag size="small" type="info">即将上线</el-tag></h3>
          <p>管理组织架构与部门信息</p>
        </div>
      </div>

      <div class="info-card coming-soon">
        <div class="card-icon">📋</div>
        <div class="card-body">
          <h3>考勤管理 <el-tag size="small" type="info">即将上线</el-tag></h3>
          <p>签到签退与考勤统计</p>
        </div>
      </div>

      <div class="info-card coming-soon">
        <div class="card-icon">📝</div>
        <div class="card-body">
          <h3>请假审批 <el-tag size="small" type="info">即将上线</el-tag></h3>
          <p>在线请假与审批流程</p>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.dashboard {
  padding: 32px;
}

.welcome-card {
  background: linear-gradient(135deg, #409eff 0%, #337ecc 100%);
  border-radius: 12px;
  padding: 40px;
  color: #fff;
  margin-bottom: 32px;
}

.welcome-card h1 {
  margin: 0 0 8px;
  font-size: 24px;
}

.subtitle {
  margin: 0;
  opacity: 0.85;
  font-size: 14px;
}

.info-cards {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
}

.info-card {
  background: #fff;
  border-radius: 10px;
  padding: 24px;
  display: flex;
  gap: 16px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
  transition: transform 0.2s;
}

.info-card:hover {
  transform: translateY(-2px);
}

.info-card.coming-soon {
  opacity: 0.7;
}

.card-icon {
  font-size: 32px;
  line-height: 1;
}

.card-body h3 {
  margin: 0 0 6px;
  font-size: 16px;
  color: #172033;
}

.card-body p {
  margin: 0;
  color: #666;
  font-size: 13px;
}
</style>
