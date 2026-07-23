<script setup lang="ts">
/**
 * 首页工作台 — 欢迎页 / 仪表盘 (支持多场次考勤与状态提醒)
 */
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { useAuthStore } from '../../stores/auth'
import {
  getTodayPersonalTasks,
  checkIn,
  checkOut,
  applyReplenishment,
  type AttendanceRecord
} from '../../api/attendance'
import { ElMessage, ElMessageBox } from 'element-plus'

const auth = useAuthStore()

// 时钟状态
const currentTime = ref('')
const currentDate = ref('')
let timerId: any = null

function updateClock() {
  const now = new Date()
  currentTime.value = now.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit', second: '2-digit' })
  const days = ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六']
  currentDate.value = `${now.getFullYear()}年${now.getMonth() + 1}月${now.getDate()}日 ${days[now.getDay()]}`
}

// 考勤多场次状态
const todayTasks = ref<AttendanceRecord[]>([])
const loadingMap = ref<Record<number, boolean>>({})

// 补签申请弹窗
const replenishDialogVisible = ref(false)
const targetTask = ref<AttendanceRecord | null>(null)
const replenishReason = ref('')
const replenishLoading = ref(false)

async function fetchTodayTasks() {
  try {
    const list = await getTodayPersonalTasks()
    todayTasks.value = list
  } catch (e) {
    console.error('Failed to load today tasks', e)
  }
}

async function handleCheckInTask(task: AttendanceRecord) {
  loadingMap.value[task.id] = true
  try {
    const res = await checkIn(task.id)
    if (res.status === 'LATE') {
      ElMessage.warning('签到成功，由于已超过正常签到时间，本次标记为【迟到】')
    } else {
      ElMessage.success('签到成功！已记录内网打卡时间')
    }
    fetchTodayTasks()
  } catch (error: any) {
    const apiResult = error.response?.data
    ElMessage.error(apiResult?.message || '签到失败')
  } finally {
    loadingMap.value[task.id] = false
  }
}

async function handleCheckOutTask(task: AttendanceRecord) {
  const now = new Date()
  const nowHHmm = now.toTimeString().slice(0, 5)
  const normalStart = task.normalCheckOutStartTime || '11:50'

  if (nowHHmm < normalStart) {
    try {
      await ElMessageBox.confirm(
        `当前时间 (${nowHHmm}) 早于正常签退最早时间 (${normalStart})，确定签退将记录为【早退】状态。是否继续？`,
        '早退打卡提示',
        {
          confirmButtonText: '确定签退 (记早退)',
          cancelButtonText: '取消',
          type: 'warning'
        }
      )
    } catch {
      return
    }
  }

  loadingMap.value[task.id] = true
  try {
    const res = await checkOut(task.id)
    if (res.status === 'EARLY_LEAVE') {
      ElMessage.warning('签退成功，本次打卡记录为【早退】')
    } else {
      ElMessage.success('签退成功！打卡记录更新完成')
    }
    fetchTodayTasks()
  } catch (error: any) {
    const apiResult = error.response?.data
    ElMessage.error(apiResult?.message || '签退失败')
  } finally {
    loadingMap.value[task.id] = false
  }
}

function openReplenishModal(task: AttendanceRecord) {
  targetTask.value = task
  replenishReason.value = ''
  replenishDialogVisible.value = true
}

async function submitReplenish() {
  if (!targetTask.value) return
  if (!replenishReason.value.trim()) {
    ElMessage.warning('请输入补签申请原因')
    return
  }
  replenishLoading.value = true
  try {
    await applyReplenishment(targetTask.value.id, replenishReason.value.trim())
    ElMessage.success('补签申请提交成功，请等待部门经理或管理员审批')
    replenishDialogVisible.value = false
    fetchTodayTasks()
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || '提交失败')
  } finally {
    replenishLoading.value = false
  }
}

function formatTime(dateTimeStr: string | null) {
  if (!dateTimeStr) return ''
  try {
    const date = new Date(dateTimeStr)
    return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit', second: '2-digit' })
  } catch {
    return dateTimeStr
  }
}

function getStatusBadge(task: AttendanceRecord) {
  if (task.replenishStatus === 'PENDING') return { type: 'warning', label: '补签待审批' }
  if (task.replenishStatus === 'APPROVED' || task.status === 'REPLENISHED') return { type: 'success', label: '已补签' }
  if (task.replenishStatus === 'REJECTED') return { type: 'danger', label: '补签被驳回' }

  switch (task.status) {
    case 'CHECKED_IN': return { type: 'success', label: '正常签到' }
    case 'CHECKED_OUT': return { type: 'success', label: '正常签退' }
    case 'LATE': return { type: 'warning', label: '迟到' }
    case 'EARLY_LEAVE': return { type: 'warning', label: '早退' }
    case 'ABSENT': return { type: 'danger', label: '缺勤' }
    default: return { type: 'info', label: '待打卡' }
  }
}

onMounted(() => {
  updateClock()
  timerId = setInterval(updateClock, 1000)
  fetchTodayTasks()
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
        <p class="subtitle">智办 AI OA 各部门考勤规则独立配置，多场次精细管理。请完成今日打卡。</p>
      </div>
    </div>

    <!-- Clock & Multi-Session Attendance Section -->
    <div class="dashboard-grid">
      <!-- Attendance Section -->
      <div class="widget-card attendance-widget" style="grid-column: span 12;">
        <div class="widget-header">
          <div class="widget-icon">📋</div>
          <div class="widget-title">
            <h3>今日部门考勤打卡</h3>
            <p>实时打卡与考勤场次看板（内网限制）</p>
          </div>
        </div>

        <div class="clock-panel" style="margin-bottom: 20px;">
          <div class="clock-time">{{ currentTime }}</div>
          <div class="clock-date">{{ currentDate }}</div>
        </div>

        <!-- 场次考勤卡片网格 -->
        <div v-if="todayTasks.length > 0" class="session-grid">
          <div v-for="task in todayTasks" :key="task.id" class="session-card">
            <div class="session-header">
              <span class="session-title">📌 {{ task.sessionName || '默认场次' }}</span>
              <el-tag :type="getStatusBadge(task).type" effect="dark" size="small">
                {{ getStatusBadge(task).label }}
              </el-tag>
            </div>

            <div class="session-info">
              <p>🟢 签到时间段：<strong>{{ task.checkInStartTime }} ~ {{ task.normalCheckInEndTime }}</strong> (最晚 {{ task.checkInEndTime }})</p>
              <p>🔴 签退时间段：<strong>{{ task.normalCheckOutStartTime }} ~ {{ task.checkOutEndTime }}</strong></p>
            </div>

            <div class="session-record">
              <div class="record-line">
                <span>签到：</span>
                <span v-if="task.checkIn" class="time-text">{{ formatTime(task.checkIn) }} <small>({{ task.checkInIp }})</small></span>
                <span v-else class="text-muted">未签到</span>
              </div>
              <div class="record-line">
                <span>签退：</span>
                <span v-if="task.checkOut" class="time-text">{{ formatTime(task.checkOut) }} <small>({{ task.checkOutIp }})</small></span>
                <span v-else class="text-muted">未签退</span>
              </div>
            </div>

            <div class="session-actions">
              <el-button
                type="success"
                size="default"
                :disabled="!!task.checkIn || ['CHECKED_IN', 'CHECKED_OUT', 'LATE', 'EARLY_LEAVE', 'REPLENISHED'].includes(task.status)"
                :loading="loadingMap[task.id]"
                @click="handleCheckInTask(task)"
              >
                签到
              </el-button>

              <el-button
                type="primary"
                size="default"
                :disabled="!task.checkIn || ['CHECKED_OUT', 'EARLY_LEAVE', 'REPLENISHED'].includes(task.status)"
                :loading="loadingMap[task.id]"
                @click="handleCheckOutTask(task)"
              >
                签退
              </el-button>

              <el-button
                v-if="['UNCHECKED', 'LATE', 'EARLY_LEAVE', 'ABSENT'].includes(task.status) && task.replenishStatus !== 'PENDING'"
                type="warning"
                plain
                size="default"
                @click="openReplenishModal(task)"
              >
                申请补签
              </el-button>
            </div>
          </div>
        </div>

        <div v-else class="empty-task-prompt" style="text-align: center; padding: 30px; color: #94a3b8; background: #f8fafc; border-radius: 8px;">
          <p>今日暂无发布的考勤任务，若需要打卡请联系部门管理员发布。</p>
        </div>
      </div>
    </div>

    <!-- 补签申请 弹窗 -->
    <el-dialog v-model="replenishDialogVisible" title="提交考勤补签申请" width="480px" :close-on-click-modal="false">
      <div v-if="targetTask">
        <el-descriptions :column="1" border size="small" style="margin-bottom: 16px;">
          <el-descriptions-item label="工作日期">{{ targetTask.workDate }}</el-descriptions-item>
          <el-descriptions-item label="考勤场次">{{ targetTask.sessionName }}</el-descriptions-item>
          <el-descriptions-item label="当前状态">
            <el-tag :type="getStatusBadge(targetTask).type" size="small">{{ getStatusBadge(targetTask).label }}</el-tag>
          </el-descriptions-item>
        </el-descriptions>

        <el-form label-position="top">
          <el-form-item label="补签原因说明" required>
            <el-input
              v-model="replenishReason"
              type="textarea"
              :rows="3"
              placeholder="请输入异常打卡或未打卡的原因说明..."
            />
          </el-form-item>
        </el-form>
      </div>

      <template #footer>
        <el-button @click="replenishDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="replenishLoading" @click="submitReplenish">提交申请</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.dashboard {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.welcome-card {
  background: linear-gradient(135deg, #1e293b 0%, #334155 100%);
  color: #fff;
  padding: 24px 30px;
  border-radius: 12px;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.08);
}
.welcome-card h1 {
  font-size: 22px;
  margin: 0 0 6px 0;
  font-weight: 700;
}
.welcome-card .subtitle {
  margin: 0;
  color: #94a3b8;
  font-size: 14px;
}

.dashboard-grid {
  display: grid;
  grid-template-columns: repeat(12, 1fr);
  gap: 20px;
}

.widget-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px 24px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.04);
}

.widget-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}
.widget-icon {
  font-size: 24px;
}
.widget-title h3 {
  margin: 0;
  font-size: 17px;
  color: #1e293b;
}
.widget-title p {
  margin: 2px 0 0 0;
  font-size: 12px;
  color: #64748b;
}

.clock-panel {
  text-align: center;
  background: #f8fafc;
  padding: 16px;
  border-radius: 10px;
}
.clock-time {
  font-size: 36px;
  font-weight: 800;
  color: #0f172a;
  letter-spacing: 2px;
}
.clock-date {
  font-size: 13px;
  color: #64748b;
  margin-top: 4px;
}

.session-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(360px, 1fr));
  gap: 16px;
}

.session-card {
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  padding: 16px;
  background: #ffffff;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.session-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #f1f5f9;
  padding-bottom: 10px;
}
.session-title {
  font-weight: 700;
  color: #1e293b;
  font-size: 15px;
}

.session-info p {
  margin: 4px 0;
  font-size: 12px;
  color: #475569;
}

.session-record {
  background: #f8fafc;
  padding: 10px 12px;
  border-radius: 6px;
  font-size: 13px;
}
.record-line {
  display: flex;
  justify-content: space-between;
  margin: 4px 0;
}
.time-text {
  font-weight: 600;
  color: #0f172a;
}
.text-muted {
  color: #94a3b8;
}

.session-actions {
  display: flex;
  gap: 10px;
  margin-top: 4px;
}
</style>
