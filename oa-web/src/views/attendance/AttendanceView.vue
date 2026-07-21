<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useAuthStore } from '../../stores/auth'
import {
  getPersonalRecords,
  getAdminRecords,
  saveOrUpdateAdminRecord,
  updateAdminRecord,
  publishDailyAttendance,
  type AttendanceRecord,
  type QueryParams
} from '../../api/attendance'
import { ElMessage } from 'element-plus'

const auth = useAuthStore()

// Active tab
const activeTab = ref('personal')

// Date utilities
const getTodayStr = () => new Date().toISOString().split('T')[0]
const getDaysAgoStr = (days: number) => {
  const d = new Date()
  d.setDate(d.getDate() - days)
  return d.toISOString().split('T')[0]
}

// Personal Query State
const personalRecords = ref<AttendanceRecord[]>([])
const personalTotal = ref(0)
const personalPage = ref(1)
const personalSize = ref(10)
const personalDateRange = ref<[string, string]>([getDaysAgoStr(30), getTodayStr()])
const personalStats = ref({ checkedIn: 0, checkedOut: 0, unchecked: 0 })
const personalLoading = ref(false)

// Admin Query State
const adminRecords = ref<AttendanceRecord[]>([])
const adminTotal = ref(0)
const adminPage = ref(1)
const adminSize = ref(10)
const adminDateRange = ref<[string, string]>([getDaysAgoStr(30), getTodayStr()])
const adminEmployeeId = ref<number | null>(null)
const adminDepartmentId = ref<number | null>(null)
const adminStatus = ref<string>('')
const adminStats = ref({ checkedIn: 0, checkedOut: 0, unchecked: 0 })
const adminLoading = ref(false)

// Format timestamps
function formatTime(dateTimeStr: string | null) {
  if (!dateTimeStr) return '-'
  try {
    const date = new Date(dateTimeStr)
    return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit', second: '2-digit' })
  } catch (e) {
    return dateTimeStr
  }
}

// Load Personal Records
async function fetchPersonal() {
  personalLoading.value = true
  try {
    const params: QueryParams = {
      page: personalPage.value,
      size: personalSize.value,
      startDate: personalDateRange.value?.[0],
      endDate: personalDateRange.value?.[1]
    }
    const result = await getPersonalRecords(params)
    personalRecords.value = result.records
    personalTotal.value = result.total
    personalStats.value = result.statistics || { checkedIn: 0, checkedOut: 0, unchecked: 0 }
  } catch (error: any) {
    const apiResult = error.response?.data
    if (apiResult?.code === 40302) {
      ElMessage.warning(apiResult.message)
      if (apiResult.data) {
        personalRecords.value = apiResult.data.records
        personalTotal.value = apiResult.data.total
        personalStats.value = apiResult.data.statistics || { checkedIn: 0, checkedOut: 0, unchecked: 0 }
      }
    } else {
      ElMessage.error(apiResult?.message || '获取个人考勤记录失败')
    }
  } finally {
    personalLoading.value = false
  }
}

// Load Admin Records
async function fetchAdmin() {
  if (!auth.isAdmin) return
  adminLoading.value = true
  try {
    const params: QueryParams = {
      page: adminPage.value,
      size: adminSize.value,
      startDate: adminDateRange.value?.[0],
      endDate: adminDateRange.value?.[1],
      employeeId: adminEmployeeId.value || null,
      departmentId: adminDepartmentId.value || null,
      status: adminStatus.value || undefined
    }
    const result = await getAdminRecords(params)
    adminRecords.value = result.records
    adminTotal.value = result.total
    adminStats.value = result.statistics || { checkedIn: 0, checkedOut: 0, unchecked: 0 }
  } catch (error: any) {
    const apiResult = error.response?.data
    ElMessage.error(apiResult?.message || '获取考勤看板数据失败')
  } finally {
    adminLoading.value = false
  }
}

function handlePersonalSearch() {
  personalPage.value = 1
  fetchPersonal()
}

function handleAdminSearch() {
  adminPage.value = 1
  fetchAdmin()
}

function resetAdminFilters() {
  adminDateRange.value = [getDaysAgoStr(30), getTodayStr()]
  adminEmployeeId.value = null
  adminDepartmentId.value = null
  adminStatus.value = ''
  adminPage.value = 1
  fetchAdmin()
}

// Load appropriate data on mount
onMounted(() => {
  fetchPersonal()
  if (auth.isAdmin) {
    fetchAdmin()
  }
})
// Watch tab switches
watch(activeTab, (tab) => {
  if (tab === 'personal') {
    fetchPersonal()
  } else if (tab === 'admin') {
    fetchAdmin()
  }
})

// Dialog State
const adminDialogVisible = ref(false)
const isEdit = ref(false)
const submitLoading = ref(false)
const adminFormRef = ref<any>(null)
interface AdminForm {
  id?: number
  employeeId?: number
  workDate: string
  status: 'CHECKED_IN' | 'CHECKED_OUT' | 'UNCHECKED' | 'LEAVE_EARLY'
  checkIn: string | null
  checkInIp: string | null
  checkOut: string | null
  checkOutIp: string | null
}

const adminForm = ref<AdminForm>({
  employeeId: undefined,
  workDate: '',
  status: 'UNCHECKED',
  checkIn: null,
  checkInIp: null,
  checkOut: null,
  checkOutIp: null
})

const adminRules = {
  employeeId: [{ required: true, message: '请输入员工ID', trigger: 'blur' }],
  workDate: [{ required: true, message: '请选择日期', trigger: 'change' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
}

function openAddDialog() {
  isEdit.value = false
  adminForm.value = {
    employeeId: undefined,
    workDate: getTodayStr(),
    status: 'UNCHECKED',
    checkIn: null,
    checkInIp: null,
    checkOut: null,
    checkOutIp: null
  }
  adminDialogVisible.value = true
}

function openEditDialog(row: AttendanceRecord) {
  isEdit.value = true
  adminForm.value = {
    id: row.id,
    employeeId: row.employeeId,
    workDate: row.workDate,
    status: row.status,
    checkIn: row.checkIn,
    checkInIp: row.checkInIp,
    checkOut: row.checkOut,
    checkOutIp: row.checkOutIp
  }
  adminDialogVisible.value = true
}

function closeAdminDialog() {
  if (adminFormRef.value) {
    adminFormRef.value.resetFields()
  }
}

async function submitAdminForm() {
  if (!adminFormRef.value) return
  await adminFormRef.value.validate(async (valid: boolean) => {
    if (!valid) return
    submitLoading.value = true
    try {
      if (isEdit.value && adminForm.value.id) {
        await updateAdminRecord(adminForm.value.id, adminForm.value)
        ElMessage.success('修改考勤记录成功')
      } else {
        await saveOrUpdateAdminRecord(adminForm.value)
        ElMessage.success('新增考勤补录成功')
      }
      adminDialogVisible.value = false
      fetchAdmin()
    } catch (error: any) {
      const apiResult = error.response?.data
      ElMessage.error(apiResult?.message || '保存考勤记录失败')
    } finally {
      submitLoading.value = false
    }
  })
}

const publishLoading = ref(false)
async function triggerPublish() {
  publishLoading.value = true
  try {
    const msg = await publishDailyAttendance()
    ElMessage.success(msg || '初始化今日考勤成功')
    fetchAdmin()
  } catch (error: any) {
    const apiResult = error.response?.data
    ElMessage.error(apiResult?.message || '初始化今日考勤失败')
  } finally {
    publishLoading.value = false
  }
}
</script>

<template>
  <div class="attendance-page">
    <div class="header">
      <div class="title-section">
        <h1>考勤管理</h1>
        <p class="subtitle">查看个人每日考勤历史，或管理全员考勤看板。</p>
      </div>
    </div>

    <el-tabs v-model="activeTab" class="custom-tabs">
      <!-- ===== 我的考勤 Tab ===== -->
      <el-tab-pane label="我的考勤" name="personal">
        <div class="pane-content">
          <!-- Statistics Cards -->
          <div class="stats-row">
            <div class="stats-card checked-in">
              <div class="stats-icon">📥</div>
              <div class="stats-body">
                <span class="label">已签到天数</span>
                <span class="value">{{ personalStats.checkedIn }}</span>
              </div>
            </div>
            <div class="stats-card checked-out">
              <div class="stats-icon">📤</div>
              <div class="stats-body">
                <span class="label">已签退天数</span>
                <span class="value">{{ personalStats.checkedOut }}</span>
              </div>
            </div>
            <div class="stats-card unchecked">
              <div class="stats-icon">❌</div>
              <div class="stats-body">
                <span class="label">未签到天数</span>
                <span class="value">{{ personalStats.unchecked }}</span>
              </div>
            </div>
          </div>

          <!-- Filters -->
          <div class="filter-card">
            <el-form :inline="true" class="filter-form">
              <el-form-item label="日期范围">
                <el-date-picker
                  v-model="personalDateRange"
                  type="daterange"
                  range-separator="至"
                  start-placeholder="开始日期"
                  end-placeholder="结束日期"
                  value-format="YYYY-MM-DD"
                  :clearable="false"
                  style="width: 260px;"
                />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="handlePersonalSearch" :loading="personalLoading">查询</el-button>
              </el-form-item>
            </el-form>
          </div>

          <!-- Table -->
          <div class="table-card">
            <el-table :data="personalRecords" v-loading="personalLoading" border style="width: 100%" class="custom-table">
              <el-table-column prop="workDate" label="日期" width="120" align="center" />
              <el-table-column label="签到时间" align="center">
                <template #default="scope">
                  <div class="time-ip">
                    <span class="time-text">{{ formatTime(scope.row.checkIn) }}</span>
                    <el-tag v-if="scope.row.checkInIp" size="small" type="info" class="ip-tag">{{ scope.row.checkInIp }}</el-tag>
                  </div>
                </template>
              </el-table-column>
              <el-table-column label="签退时间" align="center">
                <template #default="scope">
                  <div class="time-ip">
                    <span class="time-text">{{ formatTime(scope.row.checkOut) }}</span>
                    <el-tag v-if="scope.row.checkOutIp" size="small" type="info" class="ip-tag">{{ scope.row.checkOutIp }}</el-tag>
                  </div>
                </template>
              </el-table-column>
              <el-table-column prop="status" label="状态" width="120" align="center">
                <template #default="scope">
                  <el-tag v-if="scope.row.status === 'CHECKED_OUT'" type="primary" effect="dark">已签退</el-tag>
                  <el-tag v-else-if="scope.row.status === 'LEAVE_EARLY'" type="warning" effect="dark">早退</el-tag>
                  <el-tag v-else-if="scope.row.status === 'CHECKED_IN'" type="success" effect="dark">已签到</el-tag>
                  <el-tag v-else type="danger" effect="dark">未签到</el-tag>
                </template>
              </el-table-column>

            </el-table>

            <!-- Pagination -->
            <div class="pagination-container">
              <el-pagination
                v-model:current-page="personalPage"
                v-model:page-size="personalSize"
                :page-sizes="[10, 20, 50]"
                layout="total, sizes, prev, pager, next, jumper"
                :total="personalTotal"
                @size-change="fetchPersonal"
                @current-change="fetchPersonal"
              />
            </div>
          </div>
        </div>
      </el-tab-pane>

      <!-- ===== 考勤看板 Tab (Admin Only) ===== -->
      <el-tab-pane label="考勤看板" name="admin" v-if="auth.isAdmin">
        <div class="pane-content">
          <!-- Statistics Cards -->
          <div class="stats-row">
            <div class="stats-card checked-in">
              <div class="stats-icon">👥</div>
              <div class="stats-body">
                <span class="label">{{ auth.isSuperAdmin ? '全员已签到人次' : '部门已签到人次' }}</span>
                <span class="value">{{ adminStats.checkedIn }}</span>
              </div>
            </div>
            <div class="stats-card checked-out">
              <div class="stats-icon">📋</div>
              <div class="stats-body">
                <span class="label">{{ auth.isSuperAdmin ? '全员已签退人次' : '部门已签退人次' }}</span>
                <span class="value">{{ adminStats.checkedOut }}</span>
              </div>
            </div>
            <div class="stats-card unchecked">
              <div class="stats-icon">⚠️</div>
              <div class="stats-body">
                <span class="label">{{ auth.isSuperAdmin ? '全员未签到人次' : '部门未签到人次' }}</span>
                <span class="value">{{ adminStats.unchecked }}</span>
              </div>
            </div>
          </div>

          <!-- Filters -->
          <div class="filter-card">
            <el-form :inline="true" class="filter-form">
              <el-form-item label="日期范围">
                <el-date-picker
                  v-model="adminDateRange"
                  type="daterange"
                  range-separator="至"
                  start-placeholder="开始"
                  end-placeholder="结束"
                  value-format="YYYY-MM-DD"
                  :clearable="false"
                  style="width: 240px;"
                />
              </el-form-item>
              <el-form-item label="员工ID">
                <el-input-number v-model="adminEmployeeId" :min="1" placeholder="员工ID" controls-position="right" style="width: 110px;" />
              </el-form-item>
              <el-form-item v-if="auth.isSuperAdmin" label="部门ID">
                <el-input-number v-model="adminDepartmentId" :min="1" placeholder="部门ID" controls-position="right" style="width: 110px;" />
              </el-form-item>
              <el-form-item label="状态">
                <el-select v-model="adminStatus" placeholder="全部" clearable style="width: 110px;">
                  <el-option label="已签到" value="CHECKED_IN" />
                  <el-option label="已签退" value="CHECKED_OUT" />
                  <el-option label="未签到" value="UNCHECKED" />
                </el-select>
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="handleAdminSearch" :loading="adminLoading">筛选</el-button>
                <el-button @click="resetAdminFilters">重置</el-button>
              </el-form-item>
              <el-form-item style="margin-left: auto; margin-right: 0;">
                <el-button type="success" @click="openAddDialog">新增补录</el-button>
                <el-button type="warning" @click="triggerPublish" :loading="publishLoading">初始化今日考勤</el-button>              </el-form-item>
            </el-form>
          </div>

          <!-- Table -->
          <div class="table-card">
            <el-table :data="adminRecords" v-loading="adminLoading" border style="width: 100%" class="custom-table">
              <el-table-column prop="employeeName" label="员工姓名" width="130" align="center" />
              <el-table-column prop="employeeId" label="员工ID" width="100" align="center" />
              <el-table-column prop="workDate" label="日期" width="110" align="center" />
              <el-table-column label="签到明细" align="center">
                <template #default="scope">
                  <div class="time-ip">
                    <span class="time-text">{{ formatTime(scope.row.checkIn) }}</span>
                    <el-tag v-if="scope.row.checkInIp" size="small" type="info" class="ip-tag">{{ scope.row.checkInIp }}</el-tag>
                  </div>
                </template>
              </el-table-column>
              <el-table-column label="签退明细" align="center">
                <template #default="scope">
                  <div class="time-ip">
                    <span class="time-text">{{ formatTime(scope.row.checkOut) }}</span>
                    <el-tag v-if="scope.row.checkOutIp" size="small" type="info" class="ip-tag">{{ scope.row.checkOutIp }}</el-tag>
                  </div>
                </template>
              </el-table-column>
              <el-table-column prop="status" label="状态" width="110" align="center">
                <template #default="scope">
                  <el-tag v-if="scope.row.status === 'CHECKED_OUT'" type="primary" effect="dark">已签退</el-tag>
                  <el-tag v-else-if="scope.row.status === 'LEAVE_EARLY'" type="warning" effect="dark">早退</el-tag>
                  <el-tag v-else-if="scope.row.status === 'CHECKED_IN'" type="success" effect="dark">已签到</el-tag>
                  <el-tag v-else type="danger" effect="dark">未签到</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="100" align="center">
                <template #default="scope">
                  <el-button type="primary" size="small" @click="openEditDialog(scope.row)">编辑</el-button>
                </template>
              </el-table-column>
            </el-table>
            <!-- Pagination -->
            <div class="pagination-container">
              <el-pagination
                v-model:current-page="adminPage"
                v-model:page-size="adminSize"
                :page-sizes="[10, 20, 50]"
                layout="total, sizes, prev, pager, next, jumper"
                :total="adminTotal"
                @size-change="fetchAdmin"
                @current-change="fetchAdmin"
              />
            </div>
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- Edit/Add Dialog -->
    <el-dialog
      v-model="adminDialogVisible"
      :title="isEdit ? '修改考勤记录' : '新增补录考勤'"
      width="500px"
      @close="closeAdminDialog"
    >
      <el-form :model="adminForm" :rules="adminRules" ref="adminFormRef" label-width="100px" class="dialog-form">
        <el-form-item label="员工ID" prop="employeeId">
          <el-input-number v-model="adminForm.employeeId" :min="1" :disabled="isEdit" placeholder="请输入员工ID" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="工作日期" prop="workDate">
          <el-date-picker
            v-model="adminForm.workDate"
            type="date"
            placeholder="选择日期"
            value-format="YYYY-MM-DD"
            :disabled="isEdit"
            style="width: 100%;"
          />
        </el-form-item>
        <el-form-item label="考勤状态" prop="status">
          <el-select v-model="adminForm.status" placeholder="请选择状态" style="width: 100%;">
            <el-option label="未签到" value="UNCHECKED" />
            <el-option label="已签到" value="CHECKED_IN" />
            <el-option label="已签退" value="CHECKED_OUT" />
            <el-option label="早退" value="LEAVE_EARLY" />
          </el-select>
        </el-form-item>
        <el-form-item label="签到时间" prop="checkIn">
          <el-date-picker
            v-model="adminForm.checkIn"
            type="datetime"
            placeholder="选择签到时间"
            value-format="YYYY-MM-DDTHH:mm:ss"
            style="width: 100%;"
          />
        </el-form-item>
        <el-form-item label="签到IP" prop="checkInIp">
          <el-input v-model="adminForm.checkInIp" placeholder="例如: 192.168.1.100" />
        </el-form-item>
        <el-form-item label="签退时间" prop="checkOut">
          <el-date-picker
            v-model="adminForm.checkOut"
            type="datetime"
            placeholder="选择签退时间"
            value-format="YYYY-MM-DDTHH:mm:ss"
            style="width: 100%;"
          />
        </el-form-item>
        <el-form-item label="签退IP" prop="checkOutIp">
          <el-input v-model="adminForm.checkOutIp" placeholder="例如: 192.168.1.100" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="adminDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitAdminForm" :loading="submitLoading">提交</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.attendance-page {
  padding: 24px;
  background: #f2f5fa;
  min-height: calc(100vh - 48px);
}

.header {
  margin-bottom: 20px;
}

.title-section h1 {
  margin: 0 0 6px;
  font-size: 24px;
  color: #172033;
}

.subtitle {
  margin: 0;
  color: #666;
  font-size: 14px;
}

.custom-tabs :deep(.el-tabs__nav-wrap::after) {
  display: none;
}

.custom-tabs :deep(.el-tabs__header) {
  margin-bottom: 24px;
}

.pane-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* Stats Cards */
.stats-row {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 16px;
}

.stats-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.stats-icon {
  font-size: 32px;
  background: #f4f6fa;
  width: 56px;
  height: 56px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.stats-card.checked-in .stats-icon {
  background: #f0f9eb;
}
.stats-card.checked-out .stats-icon {
  background: #ecf5ff;
}
.stats-card.unchecked .stats-icon {
  background: #fef0f0;
}

.stats-body {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.stats-body .label {
  font-size: 13px;
  color: #8c92a4;
}

.stats-body .value {
  font-size: 24px;
  font-weight: bold;
  color: #172033;
}

/* Filter Card */
.filter-card {
  background: #fff;
  border-radius: 10px;
  padding: 16px 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.filter-form :deep(.el-form-item) {
  margin-bottom: 0;
  margin-right: 18px;
}

/* Table Card */
.table-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.custom-table {
  border-radius: 8px;
  overflow: hidden;
}

.time-ip {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.time-text {
  font-family: monospace;
  font-size: 14px;
  color: #333;
}

.ip-tag {
  font-size: 10px;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>
