<template>
  <div class="dashboard-container">
    <div class="page-header">
      <h1 class="page-title">📊 {{ isAdmin ? '运维数据大屏' : '我的工作台' }}</h1>
      <p class="page-subtitle">{{ isAdmin ? '实时监控组织运营数据' : '快速访问您的工作信息' }}</p>
    </div>

    <div class="stats-grid admin-stats" v-if="isAdmin">
      <div class="stat-card employee-card">
        <div class="stat-icon">
          <el-icon><User /></el-icon>
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ metrics.employeeCount }}</div>
          <div class="stat-label">在册员工总数</div>
        </div>
        <div class="stat-trend up">
          <el-icon><TrendCharts /></el-icon>
          <span>+12%</span>
        </div>
      </div>
      <div class="stat-card dept-card">
        <div class="stat-icon">
          <el-icon><OfficeBuilding /></el-icon>
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ metrics.departmentCount }}</div>
          <div class="stat-label">集团下设部门</div>
        </div>
        <div class="stat-trend up">
          <el-icon><TrendCharts /></el-icon>
          <span>+5%</span>
        </div>
      </div>
      <div class="stat-card pending-record-card">
        <div class="stat-icon">
          <el-icon><EditPen /></el-icon>
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ metrics.pendingRecordCount }}</div>
          <div class="stat-label">待补录条数</div>
        </div>
        <div class="stat-trend" :class="metrics.pendingRecordCount > 0 ? 'warn' : 'up'">
          <el-icon><WarnTriangleFilled v-if="metrics.pendingRecordCount > 0" /><CircleCheck v-else /></el-icon>
          <span>{{ metrics.pendingRecordCount > 0 ? '待处理' : '无' }}</span>
        </div>
      </div>
      <div class="stat-card leave-card">
        <div class="stat-icon">
          <el-icon><Timer /></el-icon>
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ metrics.pendingLeaveCount }}</div>
          <div class="stat-label">待审批请假条数</div>
        </div>
        <div class="stat-trend" :class="metrics.pendingLeaveCount > 0 ? 'warn' : 'up'">
          <el-icon><WarnTriangleFilled v-if="metrics.pendingLeaveCount > 0" /><CircleCheck v-else /></el-icon>
          <span>{{ metrics.pendingLeaveCount > 0 ? '待处理' : '无' }}</span>
        </div>
      </div>
    </div>

    <div class="stats-grid" v-else>
      <div class="stat-card my-attendance-card">
        <div class="stat-icon">
          <el-icon><Clock /></el-icon>
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ myStats.monthAttendanceDays }}</div>
          <div class="stat-label">本月出勤天数</div>
        </div>
        <div class="stat-trend up">
          <el-icon><CircleCheck /></el-icon>
          <span>全勤</span>
        </div>
      </div>
      <div class="stat-card my-leave-card">
        <div class="stat-icon">
          <el-icon><Timer /></el-icon>
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ myStats.pendingLeaveCount }}</div>
          <div class="stat-label">{{ isDeptAdmin ? '部门员工的待审批请假条数' : '待请假审批条数' }}</div>
        </div>
        <div class="stat-trend" :class="myStats.pendingLeaveCount > 0 ? 'warn' : 'up'">
          <el-icon><WarnTriangleFilled v-if="myStats.pendingLeaveCount > 0" /><CircleCheck v-else /></el-icon>
          <span>{{ myStats.pendingLeaveCount > 0 ? '待处理' : '无' }}</span>
        </div>
      </div>
      <div class="stat-card my-apply-card">
        <div class="stat-icon">
          <el-icon><EditPen /></el-icon>
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ myStats.pendingRecordCount }}</div>
          <div class="stat-label">{{ isDeptAdmin ? '部门员工的待审批补录条数' : '待补录审批条数' }}</div>
        </div>
        <div class="stat-trend" :class="myStats.pendingRecordCount > 0 ? 'warn' : 'up'">
          <el-icon><WarnTriangleFilled v-if="myStats.pendingRecordCount > 0" /><CircleCheck v-else /></el-icon>
          <span>{{ myStats.pendingRecordCount > 0 ? '待处理' : '无' }}</span>
        </div>
      </div>
      <div class="stat-card my-info-card">
        <div class="stat-icon">
          <el-icon><OfficeBuilding /></el-icon>
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ myStats.departmentName }}</div>
          <div class="stat-label">所属部门</div>
        </div>
      </div>
    </div>

    <div class="row-container" v-if="isAdmin">
      <div class="info-panel">
        <div class="panel-header">
          <h3>📋 快捷操作</h3>
        </div>
        <div class="quick-actions">
          <div class="action-btn" @click="goTo('/org')">
            <el-icon class="action-icon"><UserFilled /></el-icon>
            <span>组织管理</span>
          </div>
          <div class="action-btn" @click="goTo('/attendance')">
            <el-icon class="action-icon"><Calendar /></el-icon>
            <span>考勤管理</span>
          </div>
          <div class="action-btn" @click="goTo('/leave')">
            <el-icon class="action-icon"><Ticket /></el-icon>
            <span>审批管理</span>
          </div>
          <div class="action-btn" @click="goTo('/content')">
            <el-icon class="action-icon"><Document /></el-icon>
            <span>公告管理</span>
          </div>
          <div class="action-btn" @click="goTo('/ai-assistant')">
            <el-icon class="action-icon"><ChatDotRound /></el-icon>
            <span>AI助手</span>
          </div>
        </div>
      </div>
      <div class="info-panel">
        <div class="panel-header">
          <h3>📊 各部门待审批请假条数</h3>
        </div>
        <div class="activity-list-scroll">
          <div class="activity-item" v-for="(item, i) in departmentPendingLeaves" :key="i">
            <div class="activity-dot" :class="item.count > 0 ? 'pending' : 'system'"></div>
            <div class="activity-content">
              <div class="activity-title">{{ item.departmentName }}: {{ item.count }} 条待审批</div>
              <div class="activity-time">更新于 {{ item.updateTime }}</div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="row-container" v-else>
      <div class="info-panel">
        <div class="panel-header">
          <h3>📋 快捷操作</h3>
        </div>
        <div class="quick-actions">
          <div class="action-btn" @click="goTo('/attendance')">
            <el-icon class="action-icon"><Clock /></el-icon>
            <span>考勤打卡</span>
          </div>
          <div class="action-btn" @click="goTo('/leave')">
            <el-icon class="action-icon"><Ticket /></el-icon>
            <span>请假申请</span>
          </div>
          <div class="action-btn" @click="goTo('/content')">
            <el-icon class="action-icon"><Document /></el-icon>
            <span>公告制度</span>
          </div>
          <div class="action-btn" @click="goTo('/ai-assistant')">
            <el-icon class="action-icon"><ChatDotRound /></el-icon>
            <span>AI助手</span>
          </div>
        </div>
      </div>
      <div class="info-panel">
        <div class="panel-header">
          <h3>📢 最新公告</h3>
        </div>
        <div class="news-list-scroll">
          <div class="news-item" v-for="(item, i) in recentNews" :key="i" @click="goToContent(item)">
            <div class="news-tag" :class="item.type">{{ item.type === 'ANNOUNCEMENT' ? '公告' : '制度' }}</div>
            <div class="news-content">
              <div class="news-title">{{ item.title }}</div>
              <div class="news-time">{{ item.time }}</div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="bottom-panel single-card" v-if="isAdmin">
      <div class="chart-card">
        <div class="panel-header">
          <h3>📝 各部门待补录条数</h3>
        </div>
        <div class="activity-list-scroll">
          <div class="activity-item" v-for="(item, i) in departmentPendingRecords" :key="i">
            <div class="activity-dot" :class="item.count > 0 ? 'pending' : 'system'"></div>
            <div class="activity-content">
              <div class="activity-title">{{ item.departmentName }}: {{ item.count }} 条待补录</div>
              <div class="activity-time">更新于 {{ item.updateTime }}</div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="bottom-panel" v-else-if="isDeptAdmin">
      <div class="chart-card">
        <div class="panel-header">
          <h3>📝 当前待请假审批</h3>
        </div>
        <div class="leave-app-list-scroll">
          <div class="leave-app-item" v-for="(item, i) in deptPendingLeaves" :key="i">
            <div class="leave-app-time">{{ item.time }}</div>
            <div class="leave-app-content">
              <div class="leave-app-title">{{ item.employeeName }} - {{ item.type }}</div>
              <div class="leave-app-desc">{{ item.reason }}</div>
            </div>
            <div class="leave-app-status pending">待审批</div>
          </div>
          <div v-if="deptPendingLeaves.length === 0" class="empty-state">
            <el-icon class="empty-icon"><CircleCheck /></el-icon>
            <p>暂无待审批请假申请</p>
          </div>
        </div>
      </div>
      <div class="chart-card">
        <div class="panel-header">
          <h3>📝 部门员工待审批补录</h3>
        </div>
        <div class="leave-app-list-scroll">
          <div class="leave-app-item" v-for="(item, i) in deptPendingReplenish" :key="i">
            <div class="leave-app-time">{{ item.time }}</div>
            <div class="leave-app-content">
              <div class="leave-app-title">{{ item.employeeName }} - {{ item.sessionName }}</div>
              <div class="leave-app-desc">{{ item.reason }}</div>
            </div>
            <div class="leave-app-status pending">待审批</div>
          </div>
          <div v-if="deptPendingReplenish.length === 0" class="empty-state">
            <el-icon class="empty-icon"><CircleCheck /></el-icon>
            <p>暂无待审批补录申请</p>
          </div>
        </div>
      </div>
    </div>

    <div class="bottom-panel" v-else>
      <div class="chart-card">
        <div class="panel-header">
          <h3>📝 我的请假记录</h3>
        </div>
        <div class="leave-app-list-scroll">
          <div class="leave-app-item" v-for="(item, i) in myLeaveApplications" :key="i">
            <div class="leave-app-time">{{ item.time }}</div>
            <div class="leave-app-content">
              <div class="leave-app-title">{{ item.title }}</div>
              <div class="leave-app-desc">{{ item.desc }}</div>
            </div>
            <div class="leave-app-status" :class="item.status">
              {{ item.status === 'approved' ? '已通过' : item.status === 'rejected' ? '已驳回' : '待审批' }}
            </div>
          </div>
          <div v-if="myLeaveApplications.length === 0" class="empty-state">
            <el-icon class="empty-icon"><Calendar /></el-icon>
            <p>暂无请假记录</p>
          </div>
        </div>
      </div>
      <div class="chart-card">
        <div class="panel-header">
          <h3>📝 我的考勤补录</h3>
        </div>
        <div class="leave-app-list-scroll">
          <div class="leave-app-item" v-for="(item, i) in myReplenishRecords" :key="i">
            <div class="leave-app-time">{{ item.time }}</div>
            <div class="leave-app-content">
              <div class="leave-app-title">{{ item.sessionName }}</div>
              <div class="leave-app-desc">{{ item.reason }}</div>
            </div>
            <div class="leave-app-status" :class="item.status">
              {{ item.status === 'approved' ? '已通过' : item.status === 'rejected' ? '已驳回' : '待审批' }}
            </div>
          </div>
          <div v-if="myReplenishRecords.length === 0" class="empty-state">
            <el-icon class="empty-icon"><Calendar /></el-icon>
            <p>暂无补录记录</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, OfficeBuilding, Clock, Timer, Document, TrendCharts, CircleCheck, WarnTriangleFilled, Upload, UserFilled, Calendar, Ticket, EditPen, Refresh, ChatDotRound } from '@element-plus/icons-vue'
import { reindexEsApi, getEmployeeContentsApi } from '../api/content'
import { getLeavePage } from '../api/leave'
import { getDepartmentPage, getEmployeePage, getMyDepartment } from '../api/org'
import { getAdminRecords, getReplenishRecords } from '../api/attendance'

const userId = ref(localStorage.getItem('userId') || null)

const router = useRouter()
const metrics = ref({
  employeeCount: 0,
  departmentCount: 0,
  todayAttendanceRate: '100%',
  pendingLeaveCount: 0,
  publishedContentCount: 0
})
const storedRole = ref(localStorage.getItem('role') || 'EMPLOYEE')
const isAdmin = computed(() => storedRole.value === 'SUPER_ADMIN' || storedRole.value === 'ADMIN')
const isDeptAdmin = computed(() => storedRole.value === 'DEPT_MANAGER')
const userDepartmentId = ref(localStorage.getItem('departmentId') || null)

const myStats = ref({
  monthAttendanceDays: 22,
  pendingLeaveCount: 0,
  pendingRecordCount: 0,
  departmentName: '未知部门'
})

const attendanceChart = ref([85, 92, 78, 95, 88, 90, 82, 96, 89, 93, 76, 88, 91, 85, 94])
const myAttendanceChart = ref([100, 100, 100, 100, 100, 0, 0, 100, 100, 100, 100, 100, 0, 0, 100])

const departmentPendingLeaves = ref([])
const departmentPendingRecords = ref([])
const deptPendingLeaves = ref([])
const deptPendingReplenish = ref([])
const recentNews = ref([])
const myLeaveApplications = ref([])
const myReplenishRecords = ref([])

import { getAdminDashboardStats } from '../api/dashboard'

const loadDashboard = async () => {
  try {
    const res = await getAdminDashboardStats()
    if (res && res.data) {
      const data = res.data
      metrics.value.employeeCount = data.employeeCount || 0
      metrics.value.departmentCount = data.departmentCount || 0
      metrics.value.pendingLeaveCount = data.pendingLeaveCount || 0
      metrics.value.pendingRecordCount = data.pendingRecordCount || 0
      departmentPendingLeaves.value = data.departmentPendingLeaves || []
      departmentPendingRecords.value = data.departmentPendingRecords || []
    }
  } catch (err) {
    console.error('加载运维统计大包数据失败', err)
  }
}

const loadRecentNews = async () => {
  try {
    const res = await getEmployeeContentsApi({ page: 1, size: 5 })
    if (res && res.data && res.data.list) {
      recentNews.value = res.data.list.slice(0, 4).map(c => ({
        title: c.title,
        time: formatTime(c.createTime || c.updateTime),
        type: c.type || 'ANNOUNCEMENT'
      }))
    }
  } catch (err) {
    console.error('加载公告数据失败', err)
  }
}

const formatTime = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  const now = new Date()
  const diff = now - date
  const days = Math.floor(diff / (1000 * 60 * 60 * 24))
  if (days === 0) {
    const hours = Math.floor(diff / (1000 * 60 * 60))
    if (hours === 0) {
      const minutes = Math.floor(diff / (1000 * 60))
      return minutes <= 0 ? '刚刚' : `${minutes}分钟前`
    }
    return `${hours}小时前`
  } else if (days === 1) {
    return '昨天'
  } else if (days < 7) {
    return `${days}天前`
  } else {
    return `${Math.floor(days / 7)}周前`
  }
}

const loadDeptPendingLeaves = async () => {
  try {
    const res = await getLeavePage({ page: 1, size: 10, status: 'PENDING' })
    if (res && res.data && res.data.records) {
      deptPendingLeaves.value = res.data.records.map(leave => ({
        time: leave.createTime?.replace('T', ' ')?.substring(0, 16) || '',
        employeeName: leave.applicantName || '',
        type: leave.type === 'ANNUAL' ? '年假' : leave.type === 'SICK' ? '病假' : leave.type === 'PERSONAL' ? '事假' : '其他',
        reason: leave.reason || ''
      }))
    }
  } catch (err) {
    console.error('加载待审批数据失败', err)
  }
}

const rebuildIndex = async () => {
  try {
    const res = await reindexEsApi()
    if (res && res.code === 200) ElMessage.success('Elasticsearch 全量索引重建任务已成功触发')
    else ElMessage.success('索引重建下发完成')
  } catch (err) {
    ElMessage.error(err.response?.data?.message || '重建索引失败')
  }
}

const goTo = (path) => {
  router.push(path)
}

const goToContent = (item) => {
  router.push({ path: '/content', query: { type: item.type, title: encodeURIComponent(item.title) } })
}

const generateCalendar = async () => {
  const now = new Date()
  const year = now.getFullYear()
  const month = now.getMonth()
  const firstDay = new Date(year, month, 1)
  const lastDay = new Date(year, month + 1, 0)
  const startDay = firstDay.getDay()
  const totalDays = lastDay.getDate()
  
  const days = []
  
  const prevMonthLastDay = new Date(year, month, 0).getDate()
  for (let i = startDay - 1; i >= 0; i--) {
    days.push({
      date: prevMonthLastDay - i,
      hasAttendance: false,
      otherMonth: true
    })
  }
  
  const attendanceMap = {}
  try {
    const currentYear = new Date().getFullYear()
    const currentMonth = new Date().getMonth()
    const startDate = `${currentYear}-${String(currentMonth + 1).padStart(2, '0')}-01`
    const endDate = `${currentYear}-${String(currentMonth + 1).padStart(2, '0')}-${totalDays}`
    
    const attRes = await getAdminRecords({ 
      page: 1, 
      size: 100,
      startDate: startDate,
      endDate: endDate
    }).catch(() => null)
    
    if (attRes && attRes.data && attRes.data.records) {
      attRes.data.records.forEach(r => {
        if (r.employeeId !== userId.value) return
        const date = new Date(r.recordDate || r.createTime)
        const day = date.getDate()
        if (!attendanceMap[day]) {
          attendanceMap[day] = { morningCheckIn: false, morningCheckOut: false, afternoonCheckIn: false, afternoonCheckOut: false }
        }
        const isReplenished = r.status === 'REPLENISHED'
        if (r.sessionName && r.sessionName.includes('上午')) {
          if (r.checkIn || isReplenished) attendanceMap[day].morningCheckIn = true
          if (r.checkOut || isReplenished) attendanceMap[day].morningCheckOut = true
        } else if (r.sessionName && r.sessionName.includes('下午')) {
          if (r.checkIn || isReplenished) attendanceMap[day].afternoonCheckIn = true
          if (r.checkOut || isReplenished) attendanceMap[day].afternoonCheckOut = true
        } else {
          if (r.checkIn || isReplenished) attendanceMap[day].morningCheckIn = true
          if (r.checkOut || isReplenished) attendanceMap[day].morningCheckOut = true
        }
      })
    }
  } catch (err) {
    console.error('加载考勤记录失败', err)
  }
  
  const today = new Date().getDate()
  for (let i = 1; i <= totalDays; i++) {
    const dayData = attendanceMap[i]
    let status = 'pending'
    if (i < today) {
      status = dayData && dayData.morningCheckIn && dayData.morningCheckOut && dayData.afternoonCheckIn && dayData.afternoonCheckOut ? 'completed' : 'failed'
    } else if (i === today) {
      status = dayData && dayData.morningCheckIn && dayData.morningCheckOut && dayData.afternoonCheckIn && dayData.afternoonCheckOut ? 'completed' : 'pending'
    }
    days.push({
      date: i,
      status: status,
      otherMonth: false
    })
  }
  
  const remainingCells = 42 - days.length
  for (let i = 1; i <= remainingCells; i++) {
    days.push({
      date: i,
      hasAttendance: false,
      otherMonth: true
    })
  }
  
  calendarDays.value = days
}

const loadMyLeaveApplications = async () => {
  try {
    const deptId = userDepartmentId.value
    
    const [leaveRes, recordRes] = await Promise.all([
      getLeavePage({ page: 1, size: 100, department: deptId }).catch(() => null),
      getReplenishRecords({ page: 1, size: 100, departmentId: deptId }).catch(() => null)
    ])
    
    if (leaveRes && leaveRes.data && leaveRes.data.records) {
      const pendingLeaves = leaveRes.data.records.filter(l => l.status === 'PENDING')
      myLeaveApplications.value = pendingLeaves.slice(0, 5).map(leave => ({
        time: leave.createTime?.substring(0, 10) || '',
        title: leave.type === 'ANNUAL' ? '年假申请' : leave.type === 'SICK' ? '病假申请' : '事假申请',
        desc: leave.reason || '',
        status: leave.status === 'APPROVED' ? 'approved' : leave.status === 'REJECTED' ? 'rejected' : 'pending'
      }))
      myStats.value.pendingLeaveCount = pendingLeaves.length
    }
    
    if (recordRes && recordRes.data) {
      const records = recordRes.data.records || recordRes.data.list || []
      const pendingRecords = records.filter(r => r.status === 'PENDING')
      myStats.value.pendingRecordCount = pendingRecords.length
      
      if (isDeptAdmin.value) {
        deptPendingReplenish.value = pendingRecords.slice(0, 5).map(r => ({
          time: r.createTime?.substring(0, 10) || '',
          employeeName: r.employeeName || '',
          sessionName: r.sessionName || '',
          reason: r.reason || ''
        }))
      } else {
        const empRecords = records.filter(r => r.employeeId === userId.value || r.employeeId === Number(userId.value))
        myReplenishRecords.value = empRecords.slice(0, 5).map(r => ({
          time: r.createTime?.substring(0, 10) || '',
          sessionName: r.sessionName || '',
          reason: r.reason || '',
          status: r.status === 'APPROVED' ? 'approved' : r.status === 'REJECTED' ? 'rejected' : 'pending'
        }))
      }
    }
    
    const myDeptRes = await getMyDepartment().catch(() => null)
    if (myDeptRes && myDeptRes.data && myDeptRes.data.department) {
      myStats.value.departmentName = myDeptRes.data.department.name
    }
  } catch (err) {
    console.error('加载个人关联数据失败', err)
  }
}

onMounted(() => {
  if (!localStorage.getItem('token')) return
  loadRecentNews()
  if (isAdmin.value) {
    loadDashboard()
  } else if (isDeptAdmin.value) {
    loadDeptPendingLeaves()
    loadMyLeaveApplications()
  } else {
    loadMyLeaveApplications()
  }
})
</script>

<style scoped>
.dashboard-container {
  padding: 32px;
  background: linear-gradient(135deg, #f0f4f8 0%, #e8ecf1 50%, #f5f7fa 100%);
  min-height: 100vh;
  position: relative;
  overflow: hidden;
}

.dashboard-container::before {
  content: '';
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: 
    radial-gradient(circle at 20% 80%, rgba(99, 102, 241, 0.08) 0%, transparent 50%),
    radial-gradient(circle at 80% 20%, rgba(6, 182, 212, 0.08) 0%, transparent 50%),
    radial-gradient(circle at 50% 50%, rgba(34, 197, 94, 0.04) 0%, transparent 70%);
  pointer-events: none;
  z-index: 0;
}

.page-header {
  margin-bottom: 40px;
  position: relative;
  z-index: 1;
}

.page-title {
  font-size: 36px;
  font-weight: 800;
  color: #0f172a;
  margin: 0;
  letter-spacing: -0.5px;
}

.page-subtitle {
  font-size: 15px;
  color: #64748b;
  margin: 12px 0 0 0;
  font-weight: 400;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 20px;
  margin-bottom: 40px;
  position: relative;
  z-index: 1;
}

.stat-card {
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(24px);
  -webkit-backdrop-filter: blur(24px);
  border-radius: 24px;
  padding: 32px 28px;
  box-shadow: 
    0 4px 20px rgba(0, 0, 0, 0.04),
    0 1px 3px rgba(0, 0, 0, 0.06);
  display: flex;
  align-items: center;
  gap: 20px;
  position: relative;
  overflow: hidden;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  border: 1px solid rgba(255, 255, 255, 0.8);
}

.stat-card::after {
  content: '';
  position: absolute;
  top: -60%;
  right: -60%;
  width: 120%;
  height: 120%;
  background: radial-gradient(circle, rgba(102, 126, 234, 0.06) 0%, transparent 70%);
  opacity: 0;
  transition: opacity 0.4s ease;
}

.stat-card:hover {
  transform: translateY(-8px) scale(1.02);
  box-shadow: 
    0 20px 40px rgba(0, 0, 0, 0.08),
    0 8px 24px rgba(0, 0, 0, 0.06);
}

.stat-card:hover::after {
  opacity: 1;
}

.stat-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 5px;
  height: 100%;
  border-radius: 24px 0 0 24px;
}

.employee-card::before { background: linear-gradient(180deg, #6366f1 0%, #8b5cf6 100%); }
.dept-card::before { background: linear-gradient(180deg, #06b6d4 0%, #3b82f6 100%); }
.attendance-card::before { background: linear-gradient(180deg, #22c55e 0%, #10b981 100%); }
.leave-card::before { background: linear-gradient(180deg, #f59e0b 0%, #ef4444 100%); }
.content-card::before { background: linear-gradient(180deg, #8b5cf6 0%, #ec4899 100%); }
.my-attendance-card::before { background: linear-gradient(180deg, #22c55e 0%, #10b981 100%); }
.my-leave-card::before { background: linear-gradient(180deg, #6366f1 0%, #8b5cf6 100%); }
.my-apply-card::before { background: linear-gradient(180deg, #f59e0b 0%, #f97316 100%); }
.my-info-card::before { background: linear-gradient(180deg, #06b6d4 0%, #3b82f6 100%); }

.stat-icon {
  width: 72px;
  height: 72px;
  border-radius: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32px;
  flex-shrink: 0;
  position: relative;
}

.stat-icon::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  border-radius: 20px;
  opacity: 0;
  transition: opacity 0.3s ease;
}

.stat-card:hover .stat-icon::before {
  opacity: 0.1;
}

.employee-card .stat-icon { background: linear-gradient(135deg, #e0e7ff 0%, #ede9fe 100%); color: #6366f1; }
.dept-card .stat-icon { background: linear-gradient(135deg, #cffafe 0%, #dbeafe 100%); color: #06b6d4; }
.attendance-card .stat-icon { background: linear-gradient(135deg, #dcfce7 0%, #d1fae5 100%); color: #22c55e; }
.leave-card .stat-icon { background: linear-gradient(135deg, #fef3c7 0%, #fee2e2 100%); color: #f59e0b; }
.pending-record-card .stat-icon { background: linear-gradient(135deg, #fef3c7 0%, #fee2e2 100%); color: #f59e0b; }
.content-card .stat-icon { background: linear-gradient(135deg, #ede9fe 0%, #fce7f3 100%); color: #8b5cf6; }
.my-attendance-card .stat-icon { background: linear-gradient(135deg, #dcfce7 0%, #d1fae5 100%); color: #22c55e; }
.my-leave-card .stat-icon { background: linear-gradient(135deg, #e0e7ff 0%, #ede9fe 100%); color: #6366f1; }
.my-apply-card .stat-icon { background: linear-gradient(135deg, #fef3c7 0%, #fee2e2 100%); color: #f59e0b; }
.my-info-card .stat-icon { background: linear-gradient(135deg, #cffafe 0%, #dbeafe 100%); color: #06b6d4; }

.stat-content { flex: 1; min-width: 0; }

.stat-value {
  font-size: 40px;
  font-weight: 800;
  color: #0f172a;
  line-height: 1.1;
  letter-spacing: -1px;
  white-space: nowrap;
}

.stat-label {
  font-size: 14px;
  color: #64748b;
  margin-top: 6px;
  font-weight: 500;
}

.stat-trend {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 12px;
  padding: 8px 14px;
  border-radius: 24px;
  flex-shrink: 0;
  font-weight: 500;
}

.stat-trend.up { background: #f0fdf4; color: #16a34a; }
.stat-trend.warn { background: #fffbeb; color: #d97706; }

.row-container {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
  margin-bottom: 40px;
  position: relative;
  z-index: 1;
}

.info-panel {
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(24px);
  -webkit-backdrop-filter: blur(24px);
  border-radius: 24px;
  padding: 32px;
  box-shadow: 
    0 4px 20px rgba(0, 0, 0, 0.04),
    0 1px 3px rgba(0, 0, 0, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.8);
  transition: all 0.3s ease;
}

.info-panel:hover {
  box-shadow: 
    0 12px 32px rgba(0, 0, 0, 0.06),
    0 4px 16px rgba(0, 0, 0, 0.04);
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
}

.panel-header h3 {
  font-size: 19px;
  font-weight: 700;
  color: #0f172a;
  margin: 0;
}

.panel-header::after {
  content: '';
  position: absolute;
  bottom: -8px;
  left: 0;
  width: 40px;
  height: 3px;
  background: linear-gradient(90deg, #6366f1 0%, #8b5cf6 100%);
  border-radius: 2px;
}

.quick-actions {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 14px;
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 18px 20px;
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
  border-radius: 16px;
  cursor: pointer;
  transition: all 0.35s cubic-bezier(0.4, 0, 0.2, 1);
  border: 2px solid transparent;
  position: relative;
  overflow: hidden;
}

.action-btn::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(135deg, #eff6ff 0%, #dbeafe 100%);
  opacity: 0;
  transition: opacity 0.35s ease;
}

.action-btn:hover {
  border-color: #6366f1;
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(99, 102, 241, 0.15);
}

.action-btn:hover::before {
  opacity: 1;
}

.action-btn:active {
  transform: translateY(0);
}

.action-icon {
  font-size: 28px;
  color: #6366f1;
  flex-shrink: 0;
  position: relative;
  z-index: 1;
}

.action-btn span {
  font-size: 15px;
  font-weight: 600;
  color: #334155;
  position: relative;
  z-index: 1;
}

.es-section { padding-top: 4px; }

.es-desc {
  font-size: 14px;
  color: #64748b;
  line-height: 1.7;
  margin: 0 0 24px 0;
}

.rebuild-btn {
  width: 100%;
  padding: 16px;
  font-weight: 600;
  border-radius: 14px;
}

.news-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.news-item {
  display: flex;
  gap: 14px;
  padding: 16px 18px;
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
  border-radius: 14px;
  transition: all 0.35s cubic-bezier(0.4, 0, 0.2, 1);
  cursor: pointer;
  border: 2px solid transparent;
  position: relative;
  overflow: hidden;
}

.news-item::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(135deg, #eff6ff 0%, #dbeafe 100%);
  opacity: 0;
  transition: opacity 0.35s ease;
}

.news-item:hover {
  border-color: #6366f1;
  transform: translateX(6px);
  box-shadow: 0 4px 16px rgba(99, 102, 241, 0.1);
}

.news-item:hover::before {
  opacity: 1;
}

.news-tag {
  font-size: 12px;
  padding: 5px 10px;
  border-radius: 6px;
  flex-shrink: 0;
  font-weight: 600;
  position: relative;
  z-index: 1;
}

.news-tag.ANNOUNCEMENT {
  background: linear-gradient(135deg, #dbeafe 0%, #bfdbfe 100%);
  color: #1d4ed8;
}

.news-tag.POLICY {
  background: linear-gradient(135deg, #fef3c7 0%, #fde68a 100%);
  color: #b45309;
}

.news-content { flex: 1; min-width: 0; position: relative; z-index: 1; }

.news-title {
  font-size: 15px;
  color: #1e293b;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.news-time {
  font-size: 13px;
  color: #94a3b8;
  margin-top: 6px;
}

.bottom-panel {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
  position: relative;
  z-index: 1;
}

.bottom-panel.single-card {
  grid-template-columns: 1fr;
}

.chart-card {
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(24px);
  -webkit-backdrop-filter: blur(24px);
  border-radius: 24px;
  padding: 32px;
  box-shadow: 
    0 4px 20px rgba(0, 0, 0, 0.04),
    0 1px 3px rgba(0, 0, 0, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.8);
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}

.chart-card:hover {
  transform: translateY(-6px);
  box-shadow: 
    0 16px 36px rgba(0, 0, 0, 0.06),
    0 8px 20px rgba(0, 0, 0, 0.04);
}

.mini-chart {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  height: 160px;
  padding-top: 24px;
  padding-bottom: 32px;
}

.chart-bar {
  width: 28px;
  background: linear-gradient(180deg, #6366f1 0%, #a5b4fc 100%);
  border-radius: 14px 14px 6px 6px;
  display: flex;
  flex-direction: column;
  align-items: center;
  position: relative;
  transition: all 0.35s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 6px 16px rgba(99, 102, 241, 0.25);
  flex-shrink: 0;
}

.chart-bar:hover {
  background: linear-gradient(180deg, #4f46e5 0%, #6366f1 100%);
  transform: scaleY(1.08);
  box-shadow: 0 10px 24px rgba(99, 102, 241, 0.35);
}

.bar-label {
  position: absolute;
  bottom: -28px;
  font-size: 12px;
  color: #94a3b8;
  font-weight: 500;
}

.activity-list {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.activity-item {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  padding: 14px 16px;
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
  border-radius: 12px;
  transition: all 0.3s ease;
}

.activity-item:hover {
  background: linear-gradient(135deg, #f1f5f9 0%, #e2e8f0 100%);
}

.activity-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  margin-top: 6px;
  flex-shrink: 0;
  position: relative;
}

.activity-dot::after {
  content: '';
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 20px;
  height: 20px;
  border-radius: 50%;
  opacity: 0;
  transition: opacity 0.3s ease;
}

.activity-dot.pending { 
  background: #f59e0b; 
  box-shadow: 0 0 0 0 rgba(245, 158, 11, 0.4);
  animation: pulse-warning 2s infinite;
}
.activity-dot.approved { background: #22c55e; }
.activity-dot.rejected { background: #ef4444; }
.activity-dot.system { background: #3b82f6; }

@keyframes pulse-warning {
  0% { box-shadow: 0 0 0 0 rgba(245, 158, 11, 0.4); }
  70% { box-shadow: 0 0 0 10px rgba(245, 158, 11, 0); }
  100% { box-shadow: 0 0 0 0 rgba(245, 158, 11, 0); }
}

.activity-content { flex: 1; min-width: 0; }

.activity-title {
  font-size: 15px;
  color: #1e293b;
  font-weight: 600;
}

.activity-time {
  font-size: 13px;
  color: #94a3b8;
  margin-top: 4px;
}

.leave-app-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.leave-app-item {
  display: flex;
  gap: 18px;
  padding: 18px 20px;
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
  border-radius: 16px;
  align-items: center;
  transition: all 0.35s cubic-bezier(0.4, 0, 0.2, 1);
  border: 2px solid transparent;
}

.leave-app-item:hover {
  border-color: #6366f1;
  box-shadow: 0 4px 16px rgba(99, 102, 241, 0.1);
  transform: translateY(-2px);
}

.leave-app-time {
  font-size: 14px;
  font-weight: 700;
  color: #6366f1;
  flex-shrink: 0;
  width: 100px;
  font-family: 'Monaco', 'Menlo', monospace;
}

.leave-app-content { flex: 1; min-width: 0; }

.leave-app-title {
  font-size: 15px;
  color: #0f172a;
  font-weight: 700;
}

.leave-app-desc {
  font-size: 13px;
  color: #64748b;
  margin-top: 6px;
  line-height: 1.5;
}

.leave-app-status {
  font-size: 13px;
  padding: 6px 14px;
  border-radius: 24px;
  font-weight: 600;
  flex-shrink: 0;
}

.leave-app-status.approved {
  background: linear-gradient(135deg, #dcfce7 0%, #bbf7d0 100%);
  color: #15803d;
}

.leave-app-status.rejected {
  background: linear-gradient(135deg, #fee2e2 0%, #fecaca 100%);
  color: #b91c1c;
}

.leave-app-status.pending {
  background: linear-gradient(135deg, #fef3c7 0%, #fde68a 100%);
  color: #b45309;
  box-shadow: 0 0 0 0 rgba(245, 158, 11, 0.4);
  animation: pulse-warning 2s infinite;
}

.empty-state {
  text-align: center;
  padding: 48px 0;
  color: #94a3b8;
}

.empty-icon {
  font-size: 56px;
  margin-bottom: 16px;
  color: #cbd5e1;
  transition: color 0.3s ease;
}

.empty-state:hover .empty-icon {
  color: #94a3b8;
}

.empty-state p {
  margin: 0;
  font-size: 15px;
  font-weight: 500;
}

@media (max-width: 1024px) {
  .row-container, .bottom-panel { grid-template-columns: 1fr; }
}

@media (max-width: 768px) {
  .dashboard-container { padding: 20px; }
  .page-title { font-size: 28px; }
  .stats-grid { grid-template-columns: 1fr; }
  .quick-actions { grid-template-columns: 1fr; }
  .stat-card { padding: 24px 20px; }
  .stat-value { font-size: 32px; }
  .stat-icon { width: 60px; height: 60px; font-size: 26px; }
  .info-panel, .chart-card { padding: 24px; }
}

@media (max-width: 480px) {
  .dashboard-container { padding: 16px; }
  .page-title { font-size: 24px; }
  .page-subtitle { font-size: 13px; }
  .stat-card { padding: 20px 16px; gap: 16px; }
  .stat-value { font-size: 28px; }
  .stat-icon { width: 52px; height: 52px; font-size: 22px; }
}

.admin-stats {
  grid-template-columns: repeat(4, 1fr);
}

.activity-list-scroll,
.news-list-scroll,
.leave-app-list-scroll {
  max-height: 280px;
  overflow-y: auto;
  padding-right: 8px;
}

.activity-list-scroll::-webkit-scrollbar,
.news-list-scroll::-webkit-scrollbar,
.leave-app-list-scroll::-webkit-scrollbar {
  width: 6px;
}

.activity-list-scroll::-webkit-scrollbar-track,
.news-list-scroll::-webkit-scrollbar-track,
.leave-app-list-scroll::-webkit-scrollbar-track {
  background: #f1f5f9;
  border-radius: 3px;
}

.activity-list-scroll::-webkit-scrollbar-thumb,
.news-list-scroll::-webkit-scrollbar-thumb,
.leave-app-list-scroll::-webkit-scrollbar-thumb {
  background: #cbd5e1;
  border-radius: 3px;
}

.activity-list-scroll::-webkit-scrollbar-thumb:hover,
.news-list-scroll::-webkit-scrollbar-thumb:hover,
.leave-app-list-scroll::-webkit-scrollbar-thumb:hover {
  background: #94a3b8;
}

.attendance-calendar {
  margin-top: 20px;
}

.calendar-header {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 4px;
  margin-bottom: 12px;
}

.calendar-weekday {
  text-align: center;
  font-size: 13px;
  font-weight: 600;
  color: #64748b;
  padding: 8px 0;
}

.calendar-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 4px;
}

.calendar-day {
  aspect-ratio: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  background: #f8fafc;
  transition: all 0.2s ease;
  cursor: pointer;
}

.calendar-day:hover {
  background: #e2e8f0;
}

.calendar-day.completed {
  background: linear-gradient(135deg, #dbeafe 0%, #bfdbfe 100%);
  color: #1d4ed8;
  font-weight: 600;
}

.calendar-day.failed {
  background: linear-gradient(135deg, #fee2e2 0%, #fecaca 100%);
  color: #b91c1c;
  font-weight: 600;
}

.calendar-day.pending {
  background: #f8fafc;
}

.calendar-day.other-month {
  opacity: 0.3;
  pointer-events: none;
}

.day-number {
  font-size: 14px;
}
</style>