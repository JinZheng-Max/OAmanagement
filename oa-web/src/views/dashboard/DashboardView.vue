<script setup lang="ts">
/**
 * 首页工作台 — 欢迎页 / 仪表盘
 */
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { useAuthStore } from '../../stores/auth'
import { checkIn, checkOut, getPersonalRecords, type AttendanceRecord } from '../../api/attendance'
import { ElMessage } from 'element-plus'

const auth = useAuthStore()

// Ticking Clock State
const currentTime = ref('')
const currentDate = ref('')
let timerId: any = null

function updateClock() {
  const now = new Date()
  currentTime.value = now.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit', second: '2-digit' })
  const days = ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六']
  currentDate.value = `${now.getFullYear()}年${now.getMonth() + 1}月${now.getDate()}日 ${days[now.getDay()]}`
}

// Attendance Check-in/out State
const todayRecord = ref<AttendanceRecord | null>(null)
const loading = ref(false)

async function fetchTodayStatus() {
  try {
    const todayStr = new Date().toISOString().split('T')[0]
    const result = await getPersonalRecords({ startDate: todayStr, endDate: todayStr })
    if (result.records && result.records.length > 0) {
      todayRecord.value = result.records[0]
    } else {
      todayRecord.value = null
    }
  } catch (e) {
    console.error('Failed to load today status', e)
  }
}

async function handleCheckIn() {
  loading.value = true
  try {
    const record = await checkIn()
    todayRecord.value = record
    ElMessage.success('签到成功！已记录内网打卡时间')
  } catch (error: any) {
    const apiResult = error.response?.data
    if (apiResult?.code === 40910 && apiResult.data) {
      todayRecord.value = apiResult.data
      ElMessage.warning(apiResult.message)
    } else {
      ElMessage.error(apiResult?.message || '签到失败，请确认是否处于公司内网环境')
    }
  } finally {
    loading.value = false
  }
}

async function handleCheckOut() {
  loading.value = true
  try {
    const record = await checkOut()
    todayRecord.value = record
    ElMessage.success('签退成功！打卡记录更新完成')
  } catch (error: any) {
    const apiResult = error.response?.data
    if (apiResult?.code === 40911) {
      ElMessage.warning(apiResult.message)
    } else {
      ElMessage.error(apiResult?.message || '签退失败')
    }
  } finally {
    loading.value = false
  }
}

function formatTime(dateTimeStr: string | null) {
  if (!dateTimeStr) return '-'
  try {
    const date = new Date(dateTimeStr)
    return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit', second: '2-digit' })
  } catch (e) {
    return dateTimeStr
  }
}

onMounted(() => {
  updateClock()
  timerId = setInterval(updateClock, 1000)
  fetchTodayStatus()
})

onBeforeUnmount(() => {
  if (timerId) clearInterval(timerId)
})
</script>

<template>
  <div class="dashboard">
    <!-- Welcome Header Banner -->
    <div class="welcome-card">
      <div class="welcome-text">
        <h1>欢迎回来，{{ auth.user?.username }} 🎉</h1>
        <p class="subtitle">智办 AI OA 全方位提升组织效率。今日考勤打卡状态可在下方卡片操作。</p>
      </div>
    </div>

    <!-- Main Widget Grid -->
    <div class="dashboard-grid">
      <!-- 1. Real-time Attendance Checkin Widget -->
      <div class="widget-card attendance-widget">
        <div class="widget-header">
          <div class="widget-icon">📋</div>
          <div class="widget-title">
            <h3>考勤打卡</h3>
            <p>快速完成今日签到与签退（仅支持内网）</p>
          </div>
        </div>

        <div class="clock-panel">
          <div class="clock-time">{{ currentTime }}</div>
          <div class="clock-date">{{ currentDate }}</div>
        </div>

        <div class="status-indicator">
          <div class="status-item">
            <span class="status-dot" :class="{ active: todayRecord }"></span>
            <span class="status-label">签到：</span>
            <span class="status-time" v-if="todayRecord?.checkIn">
              {{ formatTime(todayRecord.checkIn) }}
              <el-tag size="small" type="info" class="ip-tag">{{ todayRecord.checkInIp }}</el-tag>
            </span>
            <span class="status-time empty" v-else>未打卡</span>
          </div>
          <div class="status-item">
            <span class="status-dot" :class="{ active: todayRecord?.status === 'CHECKED_OUT' || todayRecord?.status === 'LEAVE_EARLY' }"></span>
            <span class="status-label">签退：</span>
            <span class="status-time" v-if="todayRecord?.checkOut">
              {{ formatTime(todayRecord.checkOut) }}
              <el-tag size="small" type="info" class="ip-tag">{{ todayRecord.checkOutIp }}</el-tag>
              <el-tag v-if="todayRecord?.status === 'LEAVE_EARLY'" size="small" type="warning" class="status-tag" style="margin-left: 5px;">早退</el-tag>
            </span>
            <span class="status-time empty" v-else>未打卡</span>
          </div>
        </div>

        <div class="widget-actions">
          <el-button
            type="success"
            size="large"
            :disabled="!!todayRecord"
            :loading="loading"
            @click="handleCheckIn"
            class="action-btn check-in-btn"
          >
            {{ todayRecord ? '已完成签到' : '点击签到 (Check In)' }}
          </el-button>
          <el-button
            type="primary"
            size="large"
            :disabled="!todayRecord || todayRecord.status === 'CHECKED_OUT' || todayRecord.status === 'LEAVE_EARLY'"
            :loading="loading"
            @click="handleCheckOut"
            class="action-btn check-out-btn"
          >
            {{ todayRecord?.status === 'CHECKED_OUT' || todayRecord?.status === 'LEAVE_EARLY' ? '已完成签退' : '点击签退 (Check Out)' }}
          </el-button>
        </div>
      </div>

      <!-- Other Modules Cards -->
      <div class="other-cards">
        <div class="info-card">
          <div class="card-icon">👥</div>
          <div class="card-body">
            <h3>员工管理</h3>
            <p>管理系统员工档案、开通账号、安全重置密码。</p>
            <el-button type="primary" size="small" link @click="$router.push('/employees')" style="margin-top: 8px;">
              立即进入 &gt;
            </el-button>
          </div>
        </div>

        <div class="info-card coming-soon">
          <div class="card-icon">🏢</div>
          <div class="card-body">
            <h3>部门管理 <el-tag size="small" type="info">即将上线</el-tag></h3>
            <p>集中式管理组织架构，直观树形层级展示。</p>
          </div>
        </div>

        <div class="info-card coming-soon">
          <div class="card-icon">📝</div>
          <div class="card-body">
            <h3>请假审批 <el-tag size="small" type="info">即将上线</el-tag></h3>
            <p>在线自主提交假期申请，工作流自动节点流转。</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.dashboard {
  padding: 32px;
  background: #f2f5fa;
  min-height: calc(100vh - 64px);
}

.welcome-card {
  background: linear-gradient(135deg, #4f46e5 0%, #3b82f6 100%);
  border-radius: 16px;
  padding: 40px;
  color: #fff;
  margin-bottom: 32px;
  box-shadow: 0 10px 25px -5px rgba(59, 130, 246, 0.3);
}

.welcome-card h1 {
  margin: 0 0 10px;
  font-size: 26px;
  font-weight: 700;
}

.subtitle {
  margin: 0;
  opacity: 0.9;
  font-size: 15px;
}

.dashboard-grid {
  display: grid;
  grid-template-columns: 420px 1fr;
  gap: 32px;
}

@media (max-width: 900px) {
  .dashboard-grid {
    grid-template-columns: 1fr;
  }
}

/* Attendance Widget Card */
.widget-card {
  background: #fff;
  border-radius: 16px;
  padding: 28px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.04);
  border: 1px solid rgba(228, 231, 237, 0.5);
  display: flex;
  flex-direction: column;
}

.widget-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 24px;
}

.widget-icon {
  font-size: 32px;
}

.widget-title h3 {
  margin: 0 0 4px;
  font-size: 18px;
  color: #172033;
}

.widget-title p {
  margin: 0;
  color: #8c92a4;
  font-size: 12px;
}

/* Clock Panel */
.clock-panel {
  background: linear-gradient(to bottom, #f8fafc, #f1f5f9);
  border-radius: 12px;
  padding: 24px;
  text-align: center;
  margin-bottom: 24px;
  border: 1px solid #e2e8f0;
}

.clock-time {
  font-size: 38px;
  font-weight: 700;
  color: #1e293b;
  font-family: 'Courier New', Courier, monospace;
  letter-spacing: 1px;
}

.clock-date {
  font-size: 14px;
  color: #64748b;
  margin-top: 6px;
}

/* Status Indicator */
.status-indicator {
  display: flex;
  flex-direction: column;
  gap: 14px;
  margin-bottom: 28px;
}

.status-item {
  display: flex;
  align-items: center;
  font-size: 14px;
  color: #334155;
  background: #f8fafc;
  padding: 10px 16px;
  border-radius: 8px;
  border: 1px dashed #e2e8f0;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #cbd5e1;
  margin-right: 10px;
}

.status-dot.active {
  background: #10b981;
  box-shadow: 0 0 8px #10b981;
}

.status-label {
  font-weight: 600;
  margin-right: 4px;
}

.status-time {
  color: #1e293b;
  display: flex;
  align-items: center;
  gap: 8px;
}

.status-time.empty {
  color: #94a3b8;
  font-style: italic;
}

.ip-tag {
  font-size: 10px;
}

/* Widget Actions */
.widget-actions {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.action-btn {
  width: 100%;
  height: 48px;
  font-weight: 600;
  border-radius: 8px;
}

/* Other Cards */
.other-cards {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.info-card {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  display: flex;
  gap: 18px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.02);
  border: 1px solid rgba(228, 231, 237, 0.3);
  transition: all 0.2s;
}

.info-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 18px rgba(0, 0, 0, 0.05);
}

.info-card.coming-soon {
  opacity: 0.75;
  background: #fafafa;
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
  color: #64748b;
  font-size: 13px;
  line-height: 1.5;
}
</style>
