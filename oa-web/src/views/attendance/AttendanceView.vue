<script setup lang="ts">
/**
 * 考勤管理综合页面
 * 包含：个人考勤记录、考勤看板与手动发布、部门独立考勤规则维护、补签申请与级联审批
 */
import { ref, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAuthStore } from '../../stores/auth'
import {
  getPersonalRecords,
  getAdminRecords,
  applyReplenishment,
  approveReplenishment,
  getReplenishRecords,
  type AttendanceRecord
} from '../../api/attendance'
import {
  getDepartmentRules,
  saveDepartmentRule,
  deleteDepartmentRule,
  publishDepartmentAttendance,
  type AttendanceRuleInfo
} from '../../api/attendanceRule'
import { listActiveDepartments, type DepartmentInfo } from '../../api/department'

const auth = useAuthStore()
const activeTab = ref('personal')

// 权限判定
const isDeptManager = computed(() => auth.user?.role === 'DEPT_MANAGER')
const isSuperAdmin = computed(() => auth.user?.role === 'SUPER_ADMIN' || (auth.user?.role as string) === 'ADMIN')

// 部门列表
const departmentList = ref<DepartmentInfo[]>([])

async function loadDepartments() {
  try {
    departmentList.value = await listActiveDepartments()
  } catch (e) {
    console.error('Failed to load departments', e)
  }
}

// ================= Tab 1: 个人考勤记录 =================
const personalLoading = ref(false)
const personalList = ref<AttendanceRecord[]>([])
const personalTotal = ref(0)
const personalPage = ref(1)
const personalSize = ref(10)
const personalDateRange = ref<[string, string] | null>(null)

async function fetchPersonalRecords() {
  personalLoading.value = true
  try {
    const res = await getPersonalRecords({
      page: personalPage.value,
      size: personalSize.value,
      startDate: personalDateRange.value ? personalDateRange.value[0] : undefined,
      endDate: personalDateRange.value ? personalDateRange.value[1] : undefined
    })
    personalList.value = res.records
    personalTotal.value = res.total
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '查询个人记录失败')
  } finally {
    personalLoading.value = false
  }
}

// 补签申请弹窗
const replenishDialogVisible = ref(false)
const targetRecord = ref<AttendanceRecord | null>(null)
const applyReason = ref('')
const applyLoading = ref(false)

function openApplyModal(record: AttendanceRecord) {
  targetRecord.value = record
  applyReason.value = ''
  replenishDialogVisible.value = true
}

async function handleApplySubmit() {
  if (!targetRecord.value) return
  if (!applyReason.value.trim()) {
    ElMessage.warning('请输入补签申请原因')
    return
  }
  applyLoading.value = true
  try {
    await applyReplenishment(targetRecord.value.id, applyReason.value.trim())
    ElMessage.success('补签申请提交成功，请等待审批')
    replenishDialogVisible.value = false
    fetchPersonalRecords()
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '申请提交失败')
  } finally {
    applyLoading.value = false
  }
}

// ================= Tab 2: 考勤看板与手动发布 =================
const adminLoading = ref(false)
const adminList = ref<AttendanceRecord[]>([])
const adminTotal = ref(0)
const adminPage = ref(1)
const adminSize = ref(10)
const adminDeptId = ref<number | undefined>(undefined)
const adminStatus = ref('')
const adminDateRange = ref<[string, string] | null>(null)

async function fetchAdminRecords() {
  adminLoading.value = true
  try {
    const res = await getAdminRecords({
      page: adminPage.value,
      size: adminSize.value,
      departmentId: adminDeptId.value,
      status: adminStatus.value || undefined,
      startDate: adminDateRange.value ? adminDateRange.value[0] : undefined,
      endDate: adminDateRange.value ? adminDateRange.value[1] : undefined
    })
    adminList.value = res.records
    adminTotal.value = res.total
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '查询考勤看板失败')
  } finally {
    adminLoading.value = false
  }
}

// 手动发布考勤弹窗
const publishDialogVisible = ref(false)
const publishDeptId = ref<number | undefined>(undefined)
const publishTargetDate = ref(new Date().toISOString().split('T')[0])
const publishSessionName = ref('')
const publishLoading = ref(false)

function openPublishModal() {
  publishTargetDate.value = new Date().toISOString().split('T')[0]
  publishSessionName.value = ''
  publishDialogVisible.value = true
}

async function handlePublishSubmit() {
  if (!publishDeptId.value) {
    ElMessage.warning('请选择目标部门')
    return
  }
  publishLoading.value = true
  try {
    const msg = await publishDepartmentAttendance(publishDeptId.value, publishTargetDate.value, publishSessionName.value)
    ElMessage.success(msg)
    publishDialogVisible.value = false
    fetchAdminRecords()
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '发布任务失败')
  } finally {
    publishLoading.value = false
  }
}

// ================= Tab 3: 部门独立考勤规则维护 =================
const ruleLoading = ref(false)
const ruleList = ref<AttendanceRuleInfo[]>([])
const filterRuleDeptId = ref<number | undefined>(undefined)

const ruleDialogVisible = ref(false)
const ruleForm = ref<AttendanceRuleInfo>({
  departmentId: 0,
  sessionName: '上午场',
  checkInStartTime: '08:50',
  normalCheckInEndTime: '09:10',
  checkInEndTime: '12:10',
  normalCheckOutStartTime: '11:50',
  checkOutEndTime: '12:10',
  enabled: 1
})
const ruleFormLoading = ref(false)

async function fetchRules() {
  ruleLoading.value = true
  try {
    ruleList.value = await getDepartmentRules(filterRuleDeptId.value)
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '获取规则失败')
  } finally {
    ruleLoading.value = false
  }
}

function openAddRuleModal() {
  ruleForm.value = {
    departmentId: filterRuleDeptId.value || departmentList.value[0]?.id || 1,
    sessionName: '上午场',
    checkInStartTime: '08:50',
    normalCheckInEndTime: '09:10',
    checkInEndTime: '12:10',
    normalCheckOutStartTime: '11:50',
    checkOutEndTime: '12:10',
    enabled: 1
  }
  ruleDialogVisible.value = true
}

function openEditRuleModal(rule: AttendanceRuleInfo) {
  ruleForm.value = { ...rule }
  ruleDialogVisible.value = true
}

async function handleSaveRule() {
  if (!ruleForm.value.departmentId) {
    ElMessage.warning('请选择部门')
    return
  }
  if (!ruleForm.value.sessionName.trim()) {
    ElMessage.warning('请输入场次名称 (如 上午场 / 下午场)')
    return
  }
  ruleFormLoading.value = true
  try {
    await saveDepartmentRule(ruleForm.value)
    ElMessage.success('部门考勤规则保存成功')
    ruleDialogVisible.value = false
    fetchRules()
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '保存规则失败')
  } finally {
    ruleFormLoading.value = false
  }
}

async function handleDeleteRule(id?: number) {
  if (!id) return
  try {
    await ElMessageBox.confirm('确定要删除该部门考勤场次规则吗？删除后不再自动生成该场次任务。', '删除提示', { type: 'warning' })
    await deleteDepartmentRule(id)
    ElMessage.success('规则删除成功')
    fetchRules()
  } catch {}
}

// ================= Tab 4: 考勤补签审批看板 =================
const replenishListLoading = ref(false)
const replenishList = ref<AttendanceRecord[]>([])
const replenishTotal = ref(0)
const replenishPage = ref(1)
const replenishSize = ref(10)
const filterReplenishStatus = ref('PENDING')
const filterReplenishDeptId = ref<number | undefined>(undefined)

const approveDialogVisible = ref(false)
const currentApproveRecord = ref<AttendanceRecord | null>(null)
const approveComment = ref('')
const approveSubmitLoading = ref(false)

async function fetchReplenishRecords() {
  replenishListLoading.value = true
  try {
    const res = await getReplenishRecords({
      page: replenishPage.value,
      size: replenishSize.value,
      departmentId: filterReplenishDeptId.value,
      replenishStatus: filterReplenishStatus.value || undefined
    })
    replenishList.value = res.records
    replenishTotal.value = res.total
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '获取补签列表失败')
  } finally {
    replenishListLoading.value = false
  }
}

function openApproveModal(record: AttendanceRecord) {
  currentApproveRecord.value = record
  approveComment.value = ''
  approveDialogVisible.value = true
}

async function handleApprove(approved: boolean) {
  if (!currentApproveRecord.value) return
  approveSubmitLoading.value = true
  try {
    await approveReplenishment(currentApproveRecord.value.id, approved, approveComment.value.trim())
    ElMessage.success(approved ? '补签已同意' : '补签已驳回')
    approveDialogVisible.value = false
    fetchReplenishRecords()
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '审批操作失败')
  } finally {
    approveSubmitLoading.value = false
  }
}

function getStatusBadge(status: string, replenishStatus?: string) {
  if (replenishStatus === 'PENDING') return { type: 'warning', label: '补签待审批' }
  if (replenishStatus === 'APPROVED' || status === 'REPLENISHED') return { type: 'success', label: '已补签' }
  if (replenishStatus === 'REJECTED') return { type: 'danger', label: '补签被驳回' }

  switch (status) {
    case 'CHECKED_IN': return { type: 'success', label: '正常签到' }
    case 'CHECKED_OUT': return { type: 'success', label: '正常签退' }
    case 'LATE': return { type: 'warning', label: '迟到' }
    case 'EARLY_LEAVE': return { type: 'warning', label: '早退' }
    case 'ABSENT': return { type: 'danger', label: '缺勤' }
    default: return { type: 'info', label: '未打卡' }
  }
}

onMounted(() => {
  loadDepartments()
  fetchPersonalRecords()
  fetchAdminRecords()
  fetchRules()
  fetchReplenishRecords()
})
</script>

<template>
  <div class="attendance-page">
    <div class="page-header">
      <h2>考勤管理中心</h2>
      <p class="subtitle">各部门考勤规则独立制定维护，多场次精确打卡、补签审批与历史记录追溯</p>
    </div>

    <el-tabs v-model="activeTab" type="border-card">
      <!-- ===== Tab 1: 我的考勤记录 ===== -->
      <el-tab-pane label="📅 我的考勤记录" name="personal">
        <div class="filter-bar">
          <el-date-picker
            v-model="personalDateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            @change="fetchPersonalRecords"
          />
          <el-button type="primary" @click="fetchPersonalRecords">查询</el-button>
        </div>

        <el-table v-loading="personalLoading" :data="personalList" border stripe style="width: 100%; margin-top: 16px;">
          <el-table-column prop="workDate" label="工作日期" width="120" />
          <el-table-column prop="sessionName" label="考勤场次" width="110" />
          <el-table-column prop="checkIn" label="签到打卡时间" min-width="160">
            <template #default="{ row }">
              <span v-if="row.checkIn">{{ row.checkIn }} <small>({{ row.checkInIp }})</small></span>
              <span v-else class="text-muted">--</span>
            </template>
          </el-table-column>
          <el-table-column prop="checkOut" label="签退打卡时间" min-width="160">
            <template #default="{ row }">
              <span v-if="row.checkOut">{{ row.checkOut }} <small>({{ row.checkOutIp }})</small></span>
              <span v-else class="text-muted">--</span>
            </template>
          </el-table-column>
          <el-table-column label="考勤状态" width="130">
            <template #default="{ row }">
              <el-tag :type="getStatusBadge(row.status, row.replenishStatus).type">
                {{ getStatusBadge(row.status, row.replenishStatus).label }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="补签申请说明 / 审批意见" min-width="220">
            <template #default="{ row }">
              <div v-if="row.replenishReason" style="font-size: 12px;">
                <p style="margin: 0; color: #475569;"><strong>原因：</strong>{{ row.replenishReason }}</p>
                <p v-if="row.approveComment" style="margin: 2px 0 0 0; color: #0284c7;"><strong>审批：</strong>{{ row.approveComment }} ({{ row.approverName }})</p>
              </div>
              <span v-else class="text-muted">--</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120" fixed="right">
            <template #default="{ row }">
              <el-button
                v-if="['UNCHECKED', 'LATE', 'EARLY_LEAVE', 'ABSENT'].includes(row.status) && row.replenishStatus !== 'PENDING'"
                type="warning"
                size="small"
                plain
                @click="openApplyModal(row)"
              >
                申请补签
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="pagination-bar">
          <el-pagination
            v-model:current-page="personalPage"
            v-model:page-size="personalSize"
            :total="personalTotal"
            layout="total, prev, pager, next, jumper"
            @current-change="fetchPersonalRecords"
          />
        </div>
      </el-tab-pane>

      <!-- ===== Tab 2: 考勤看板与发布 ===== -->
      <el-tab-pane v-if="isDeptManager || isSuperAdmin" label="📊 部门考勤看板与发布" name="admin">
        <div class="filter-bar">
          <el-select v-if="isSuperAdmin" v-model="adminDeptId" placeholder="选择部门" clearable style="width: 160px" @change="fetchAdminRecords">
            <el-option v-for="d in departmentList" :key="d.id" :label="d.name" :value="d.id" />
          </el-select>

          <el-select v-model="adminStatus" placeholder="状态筛选" clearable style="width: 140px" @change="fetchAdminRecords">
            <el-option label="正常签到" value="CHECKED_IN" />
            <el-option label="迟到" value="LATE" />
            <el-option label="早退" value="EARLY_LEAVE" />
            <el-option label="正常签退" value="CHECKED_OUT" />
            <el-option label="未打卡" value="UNCHECKED" />
            <el-option label="已补签" value="REPLENISHED" />
          </el-select>

          <el-date-picker
            v-model="adminDateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            @change="fetchAdminRecords"
          />

          <el-button type="primary" @click="fetchAdminRecords">刷新数据</el-button>

          <el-button type="success" plain style="margin-left: auto;" @click="openPublishModal">
            ➕ 手动发布指定部门考勤
          </el-button>
        </div>

        <el-table v-loading="adminLoading" :data="adminList" border stripe style="width: 100%; margin-top: 16px;">
          <el-table-column prop="workDate" label="工作日期" width="120" />
          <el-table-column prop="employeeName" label="员工姓名" width="120" />
          <el-table-column prop="departmentName" label="部门" width="120" />
          <el-table-column prop="sessionName" label="场次" width="110" />
          <el-table-column prop="checkIn" label="签到时间" min-width="160">
            <template #default="{ row }">
              <span v-if="row.checkIn">{{ row.checkIn }}</span>
              <span v-else class="text-muted">未签到</span>
            </template>
          </el-table-column>
          <el-table-column prop="checkOut" label="签退时间" min-width="160">
            <template #default="{ row }">
              <span v-if="row.checkOut">{{ row.checkOut }}</span>
              <span v-else class="text-muted">未签退</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="120">
            <template #default="{ row }">
              <el-tag :type="getStatusBadge(row.status, row.replenishStatus).type">
                {{ getStatusBadge(row.status, row.replenishStatus).label }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>

        <div class="pagination-bar">
          <el-pagination
            v-model:current-page="adminPage"
            v-model:page-size="adminSize"
            :total="adminTotal"
            layout="total, prev, pager, next, jumper"
            @current-change="fetchAdminRecords"
          />
        </div>
      </el-tab-pane>

      <!-- ===== Tab 3: 部门独立考勤规则配置 ===== -->
      <el-tab-pane v-if="isDeptManager || isSuperAdmin" label="⚙️ 部门考勤规则维护" name="rules">
        <div class="filter-bar">
          <el-select v-if="isSuperAdmin" v-model="filterRuleDeptId" placeholder="选择部门规则" clearable style="width: 180px" @change="fetchRules">
            <el-option v-for="d in departmentList" :key="d.id" :label="d.name" :value="d.id" />
          </el-select>

          <el-button type="primary" @click="fetchRules">查询</el-button>
          <el-button type="success" @click="openAddRuleModal">➕ 添加场次规则</el-button>
        </div>

        <el-table v-loading="ruleLoading" :data="ruleList" border stripe style="width: 100%; margin-top: 16px;">
          <el-table-column prop="departmentName" label="所属部门" width="140" />
          <el-table-column prop="sessionName" label="考勤场次名称" width="130" />
          <el-table-column label="签到窗口 (允许开始 ~ 正常截止 ~ 最晚)" min-width="260">
            <template #default="{ row }">
              <span style="color: #10b981; font-weight: 600;">{{ row.checkInStartTime }}</span>
              ~ 正常 <span style="color: #f59e0b; font-weight: 600;">{{ row.normalCheckInEndTime }}</span>
              ~ 最晚 {{ row.checkInEndTime }}
            </template>
          </el-table-column>
          <el-table-column label="签退窗口 (正常最早 ~ 最晚)" min-width="220">
            <template #default="{ row }">
              正常最早 <span style="color: #3b82f6; font-weight: 600;">{{ row.normalCheckOutStartTime }}</span>
              ~ 最晚 {{ row.checkOutEndTime }}
            </template>
          </el-table-column>
          <el-table-column label="状态" width="90">
            <template #default="{ row }">
              <el-tag :type="row.enabled === 1 ? 'success' : 'info'">
                {{ row.enabled === 1 ? '启用' : '禁用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="150" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" size="small" link @click="openEditRuleModal(row)">编辑</el-button>
              <el-button type="danger" size="small" link @click="handleDeleteRule(row.id)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <!-- ===== Tab 4: 考勤补签审批看板 ===== -->
      <el-tab-pane v-if="isDeptManager || isSuperAdmin" label="✍️ 考勤补签审批" name="replenish">
        <div class="filter-bar">
          <el-select v-model="filterReplenishStatus" placeholder="审批状态" clearable style="width: 140px" @change="fetchReplenishRecords">
            <el-option label="待审批" value="PENDING" />
            <el-option label="已通过" value="APPROVED" />
            <el-option label="已驳回" value="REJECTED" />
          </el-select>
          <el-button type="primary" @click="fetchReplenishRecords">刷新列表</el-button>
        </div>

        <el-table v-loading="replenishListLoading" :data="replenishList" border stripe style="width: 100%; margin-top: 16px;">
          <el-table-column prop="workDate" label="工作日期" width="120" />
          <el-table-column prop="employeeName" label="申请人" width="120" />
          <el-table-column prop="departmentName" label="部门" width="120" />
          <el-table-column prop="sessionName" label="考勤场次" width="110" />
          <el-table-column prop="replenishReason" label="补签原因" min-width="220" />
          <el-table-column label="审批状态" width="120">
            <template #default="{ row }">
              <el-tag :type="getStatusBadge(row.status, row.replenishStatus).type">
                {{ getStatusBadge(row.status, row.replenishStatus).label }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="approverName" label="审批人" width="120" />
          <el-table-column prop="approveComment" label="审批意见" min-width="160" />
          <el-table-column label="操作" width="120" fixed="right">
            <template #default="{ row }">
              <el-button
                v-if="row.replenishStatus === 'PENDING'"
                type="primary"
                size="small"
                @click="openApproveModal(row)"
              >
                审批
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="pagination-bar">
          <el-pagination
            v-model:current-page="replenishPage"
            v-model:page-size="replenishSize"
            :total="replenishTotal"
            layout="total, prev, pager, next, jumper"
            @current-change="fetchReplenishRecords"
          />
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- ===== 提交补签 弹窗 ===== -->
    <el-dialog v-model="replenishDialogVisible" title="提交考勤补签申请" width="480px">
      <div v-if="targetRecord">
        <el-descriptions :column="1" border size="small" style="margin-bottom: 16px;">
          <el-descriptions-item label="工作日期">{{ targetRecord.workDate }}</el-descriptions-item>
          <el-descriptions-item label="考勤场次">{{ targetRecord.sessionName }}</el-descriptions-item>
          <el-descriptions-item label="当前状态">
            <el-tag :type="getStatusBadge(targetRecord.status, targetRecord.replenishStatus).type" size="small">
              {{ getStatusBadge(targetRecord.status, targetRecord.replenishStatus).label }}
            </el-tag>
          </el-descriptions-item>
        </el-descriptions>

        <el-form label-position="top">
          <el-form-item label="补签原因说明" required>
            <el-input v-model="applyReason" type="textarea" :rows="3" placeholder="请输入因公无法即时打卡或系统异常的原因说明..." />
          </el-form-item>
        </el-form>
      </div>

      <template #footer>
        <el-button @click="replenishDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="applyLoading" @click="handleApplySubmit">确认提交</el-button>
      </template>
    </el-dialog>

    <!-- ===== 规则编辑 弹窗 ===== -->
    <el-dialog v-model="ruleDialogVisible" :title="ruleForm.id ? '编辑部门考勤规则' : '添加部门考勤规则'" width="560px">
      <el-form :model="ruleForm" label-width="140px">
        <el-form-item label="所属部门" required>
          <el-select v-model="ruleForm.departmentId" style="width: 100%">
            <el-option v-for="d in departmentList" :key="d.id" :label="d.name" :value="d.id" />
          </el-select>
        </el-form-item>

        <el-form-item label="考勤场次名称" required>
          <el-input v-model="ruleForm.sessionName" placeholder="如：上午场 / 下午场 / 晚班场" />
        </el-form-item>

        <el-form-item label="允许签到开始" required>
          <el-time-picker v-model="ruleForm.checkInStartTime" value-format="HH:mm" format="HH:mm" placeholder="08:50" style="width: 100%" />
        </el-form-item>

        <el-form-item label="正常签到截止" required>
          <el-time-picker v-model="ruleForm.normalCheckInEndTime" value-format="HH:mm" format="HH:mm" placeholder="09:10 (超时计迟到)" style="width: 100%" />
        </el-form-item>

        <el-form-item label="签到最晚截止" required>
          <el-time-picker v-model="ruleForm.checkInEndTime" value-format="HH:mm" format="HH:mm" placeholder="12:10" style="width: 100%" />
        </el-form-item>

        <el-form-item label="正常签退最早" required>
          <el-time-picker v-model="ruleForm.normalCheckOutStartTime" value-format="HH:mm" format="HH:mm" placeholder="11:50 (早于计早退)" style="width: 100%" />
        </el-form-item>

        <el-form-item label="签退最晚截止" required>
          <el-time-picker v-model="ruleForm.checkOutEndTime" value-format="HH:mm" format="HH:mm" placeholder="12:10" style="width: 100%" />
        </el-form-item>

        <el-form-item label="启用状态">
          <el-radio-group v-model="ruleForm.enabled">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="ruleDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="ruleFormLoading" @click="handleSaveRule">保存规则</el-button>
      </template>
    </el-dialog>

    <!-- ===== 手动发布 弹窗 ===== -->
    <el-dialog v-model="publishDialogVisible" title="手动发布部门考勤任务" width="460px">
      <el-form label-width="110px">
        <el-form-item label="目标部门" required>
          <el-select v-model="publishDeptId" style="width: 100%" placeholder="选择发布部门">
            <el-option v-for="d in departmentList" :key="d.id" :label="d.name" :value="d.id" />
          </el-select>
        </el-form-item>

        <el-form-item label="考勤日期" required>
          <el-date-picker v-model="publishTargetDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>

        <el-form-item label="考勤场次">
          <el-input v-model="publishSessionName" placeholder="不填则发布该部门所有启用的场次" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="publishDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="publishLoading" @click="handlePublishSubmit">确认发布</el-button>
      </template>
    </el-dialog>

    <!-- ===== 补签审批 弹窗 ===== -->
    <el-dialog v-model="approveDialogVisible" title="考勤补签审批" width="480px">
      <div v-if="currentApproveRecord">
        <el-descriptions :column="1" border size="small" style="margin-bottom: 16px;">
          <el-descriptions-item label="申请人">{{ currentApproveRecord.employeeName }} ({{ currentApproveRecord.departmentName }})</el-descriptions-item>
          <el-descriptions-item label="工作日期">{{ currentApproveRecord.workDate }}</el-descriptions-item>
          <el-descriptions-item label="考勤场次">{{ currentApproveRecord.sessionName }}</el-descriptions-item>
          <el-descriptions-item label="补签原因">{{ currentApproveRecord.replenishReason }}</el-descriptions-item>
        </el-descriptions>

        <el-form label-position="top">
          <el-form-item label="审批意见 (可选)">
            <el-input v-model="approveComment" type="textarea" :rows="2" placeholder="请输入审批意见说明..." />
          </el-form-item>
        </el-form>
      </div>

      <template #footer>
        <el-button type="danger" plain :loading="approveSubmitLoading" @click="handleApprove(false)">驳回</el-button>
        <el-button type="success" :loading="approveSubmitLoading" @click="handleApprove(true)">同意补签</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.attendance-page {
  padding: 16px 20px;
  background: #fff;
  border-radius: 8px;
  min-height: calc(100vh - 80px);
}

.page-header {
  margin-bottom: 16px;
  padding-bottom: 14px;
  border-bottom: 2px solid #f0f2f5;
}
.page-header h2 {
  margin: 0;
  font-size: 22px;
  font-weight: 700;
  color: #1d2129;
}
.page-header .subtitle {
  margin: 6px 0 0 0;
  font-size: 13px;
  color: #64748b;
}

.filter-bar {
  display: flex;
  gap: 12px;
  align-items: center;
  margin-bottom: 12px;
}

.pagination-bar {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

.text-muted {
  color: #94a3b8;
}
</style>
