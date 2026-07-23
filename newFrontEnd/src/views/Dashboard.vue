<template>
  <div class="dashboard-container">
    <div class="page-header">
      <h1 class="page-title">📊 {{ isAdmin ? '运维数据大屏' : '我的工作台' }}</h1>
      <p class="page-subtitle">{{ isAdmin ? '实时监控组织运营数据' : '快速访问您的工作信息' }}</p>
    </div>

    <div class="stats-grid" v-if="isAdmin">
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
      <div class="stat-card attendance-card">
        <div class="stat-icon">
          <el-icon><Clock /></el-icon>
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ metrics.todayAttendanceRate }}</div>
          <div class="stat-label">今日考勤到岗率</div>
        </div>
        <div class="stat-trend up">
          <el-icon><CircleCheck /></el-icon>
          <span>正常</span>
        </div>
      </div>
      <div class="stat-card leave-card">
        <div class="stat-icon">
          <el-icon><Timer /></el-icon>
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ metrics.pendingLeaveCount }}</div>
          <div class="stat-label">待处理审批单</div>
        </div>
        <div class="stat-trend warn">
          <el-icon><WarnTriangleFilled /></el-icon>
          <span>待处理</span>
        </div>
      </div>
      <div class="stat-card content-card">
        <div class="stat-icon">
          <el-icon><Document /></el-icon>
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ metrics.publishedContentCount }}</div>
          <div class="stat-label">全量同步知识数</div>
        </div>
        <div class="stat-trend up">
          <el-icon><Upload /></el-icon>
          <span>已同步</span>
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
          <el-icon><Ticket /></el-icon>
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ myStats.annualLeaveRemaining }}</div>
          <div class="stat-label">剩余年假天数</div>
        </div>
        <div class="stat-trend up">
          <el-icon><CircleCheck /></el-icon>
          <span>可用</span>
        </div>
      </div>
      <div class="stat-card my-apply-card">
        <div class="stat-icon">
          <el-icon><Timer /></el-icon>
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ myStats.pendingApplyCount }}</div>
          <div class="stat-label">待审批申请</div>
        </div>
        <div class="stat-trend" :class="myStats.pendingApplyCount > 0 ? 'warn' : 'up'">
          <el-icon><WarnTriangleFilled v-if="myStats.pendingApplyCount > 0" /><CircleCheck v-else /></el-icon>
          <span>{{ myStats.pendingApplyCount > 0 ? '待处理' : '无' }}</span>
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
        <div class="stat-trend up">
          <el-icon><OfficeBuilding /></el-icon>
          <span>查看</span>
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
            <el-icon class="action-icon"><EditPen /></el-icon>
            <span>内容管理</span>
          </div>
        </div>
      </div>
      <div class="info-panel">
        <div class="panel-header">
          <h3>⚙️ 搜索引擎运维</h3>
        </div>
        <div class="es-section">
          <p class="es-desc">当底层数据库产生大规模脏数据或制度大面积变更导致高亮不同步时，可在此手动执行全量重置同步：</p>
          <el-button type="danger" size="large" class="rebuild-btn" @click="rebuildIndex">
            <el-icon><Refresh /></el-icon>
            全量重建 Elasticsearch 索引
          </el-button>
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
        <div class="news-list">
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

    <div class="bottom-panel" v-if="isAdmin">
      <div class="chart-card">
        <div class="panel-header">
          <h3>📈 本月考勤统计</h3>
        </div>
        <div class="mini-chart">
          <div class="chart-bar" v-for="(val, i) in attendanceChart" :key="i" :style="{ height: val + '%' }">
            <span class="bar-label">{{ i + 1 }}日</span>
          </div>
        </div>
      </div>
      <div class="chart-card">
        <div class="panel-header">
          <h3>📊 各部门待审批动态</h3>
        </div>
        <div class="activity-list">
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

    <div class="bottom-panel" v-else-if="isDeptAdmin">
      <div class="chart-card">
        <div class="panel-header">
          <h3>📝 当前待请假审批</h3>
        </div>
        <div class="leave-app-list">
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
          <h3>📊 本月考勤趋势</h3>
        </div>
        <div class="mini-chart">
          <div class="chart-bar" v-for="(val, i) in myAttendanceChart" :key="i" :style="{ height: val + '%' }">
            <span class="bar-label">{{ i + 1 }}日</span>
          </div>
        </div>
      </div>
    </div>

    <div class="bottom-panel" v-else>
      <div class="chart-card">
        <div class="panel-header">
          <h3>📝 我的请假记录</h3>
        </div>
        <div class="leave-app-list">
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
          <h3>📊 本月考勤趋势</h3>
        </div>
        <div class="mini-chart">
          <div class="chart-bar" v-for="(val, i) in myAttendanceChart" :key="i" :style="{ height: val + '%' }">
            <span class="bar-label">{{ i + 1 }}日</span>
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
import { getAdminRecords } from '../api/attendance'

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
  annualLeaveRemaining: 5,
  pendingApplyCount: 0,
  departmentName: '未知部门'
})

const attendanceChart = ref([85, 92, 78, 95, 88, 90, 82, 96, 89, 93, 76, 88, 91, 85, 94])
const myAttendanceChart = ref([100, 100, 100, 100, 100, 0, 0, 100, 100, 100, 100, 100, 0, 0, 100])

const departmentPendingLeaves = ref([])
const deptPendingLeaves = ref([])
const recentNews = ref([])
const myLeaveApplications = ref([])

const loadDashboard = async () => {
  try {
    const [empRes, deptRes, attRes, leaveRes] = await Promise.all([
      getEmployeePage({ page: 1, size: 1 }).catch(() => null),
      getDepartmentPage({ page: 1, size: 100 }).catch(() => null),
      getAdminRecords({ page: 1, size: 1 }).catch(() => null),
      getLeavePage({ page: 1, size: 100, status: 'PENDING' }).catch(() => null)
    ])

    if (empRes && empRes.data) metrics.value.employeeCount = empRes.data.total || 0
    if (deptRes && deptRes.data) metrics.value.departmentCount = deptRes.data.total || 0
    if (attRes && attRes.data) metrics.value.publishedContentCount = attRes.data.total || 0
    
    const pendingList = leaveRes && leaveRes.data ? (leaveRes.data.records || []) : []
    metrics.value.pendingLeaveCount = pendingList.length

    if (deptRes && deptRes.data && deptRes.data.records) {
      departmentPendingLeaves.value = deptRes.data.records.map(dept => {
        const count = pendingList.filter(l => l.departmentId === dept.id).length
        return {
          departmentName: dept.name,
          count: count,
          updateTime: count > 0 ? '刚刚' : '正常'
        }
      })
    }
  } catch (err) {
    console.error('加载仪表盘失败', err)
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

const loadMyLeaveApplications = async () => {
  try {
    const res = await getLeavePage({ page: 1, size: 5 })
    if (res && res.data && res.data.records) {
      myLeaveApplications.value = res.data.records.map(leave => ({
        time: leave.createTime?.substring(0, 10) || '',
        title: leave.type === 'ANNUAL' ? '年假申请' : leave.type === 'SICK' ? '病假申请' : '事假申请',
        desc: leave.reason || '',
        status: leave.status === 'APPROVED' ? 'approved' : leave.status === 'REJECTED' ? 'rejected' : 'pending'
      }))
      myStats.value.pendingApplyCount = res.data.records.filter(l => l.status === 'PENDING').length
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
</style>