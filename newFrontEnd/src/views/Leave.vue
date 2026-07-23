<template>
  <div class="leave-container">
    <!-- 页眉与快捷提交按钮 (全角色均可发起请假申请) -->
    <div class="page-header">
      <div class="header-top">
        <div>
          <h2 class="page-title">📋 {{ isAdmin ? '全公司请假审批管理 (最高管理员终审)' : (isDeptAdmin ? '部门请假与审批管理 (一级/二级审核)' : '我的请假申请中心') }}</h2>
          <p class="page-subtitle">{{ isAdmin ? '查看全公司请假档案、审批部门经理请假及处理下级呈报的请假申请' : (isDeptAdmin ? '审批本部门员工请假、上放请假至高管或提交个人请假申请' : '在线填报请假单、上传凭证附件并跟踪多级审批进度') }}</p>
        </div>
        <div style="display: flex; gap: 12px;">
          <el-button type="primary" size="large" @click="dialogVisible = true">
            <el-icon><Plus /></el-icon>
            提交请假单
          </el-button>
        </div>
      </div>
    </div>

    <!-- 部门经理 Tab 视图切换：部门审批 vs 个人申请 -->
    <div class="view-tab-switcher" v-if="isDeptAdmin">
      <button 
        class="tab-btn" 
        :class="{ active: activeDeptTab === 'APPROVAL' }" 
        @click="activeDeptTab = 'APPROVAL'"
      >
        <span class="tab-icon">📋</span>
        <span>本部门员工请假审批</span>
        <span class="badge" v-if="pendingAuditCount > 0">{{ pendingAuditCount }}</span>
      </button>
      <button 
        class="tab-btn" 
        :class="{ active: activeDeptTab === 'MY_LEAVES' }" 
        @click="activeDeptTab = 'MY_LEAVES'"
      >
        <span class="tab-icon">👤</span>
        <span>我的个人请假记录</span>
      </button>
    </div>

    <!-- 检索与状态筛选工具栏 -->
    <div class="filter-card">
      <div class="filter-row">
        <div class="filter-group">
          <span class="filter-label">状态筛选：</span>
          <el-radio-group v-model="statusFilter" size="default">
            <el-radio-button label="ALL">全部状态</el-radio-button>
            <el-radio-button label="PENDING">待审批</el-radio-button>
            <el-radio-button label="APPROVED">已通过</el-radio-button>
            <el-radio-button label="REJECTED">已驳回</el-radio-button>
            <el-radio-button label="WITHDRAWN">已撤回</el-radio-button>
          </el-radio-group>
        </div>

        <div class="filter-group">
          <span class="filter-label">请假类型：</span>
          <el-select v-model="typeFilter" placeholder="全部类型" clearable style="width: 140px">
            <el-option label="全部类型" value="ALL" />
            <el-option label="事假" value="PERSONAL" />
            <el-option label="病假" value="SICK" />
            <el-option label="年假" value="ANNUAL" />
          </el-select>
        </div>
      </div>
    </div>

    <!-- ===== 主表单展板 ===== -->
    <el-card class="leave-card">
      <el-table :data="displayLeaves" stripe style="width: 100%" v-loading="loading">
        <el-table-column v-if="isAdmin || (isDeptAdmin && activeDeptTab === 'APPROVAL')" prop="applicantName" label="申请人" width="160">
          <template #default="scope">
            <div style="font-weight: 600; color: #1a1a2e; display: flex; align-items: center; gap: 6px;">
              <span>{{ scope.row.applicantName || '未知员工' }}</span>
              <el-tag v-if="isEscalated(scope.row)" type="danger" size="small" effect="dark">📌 呈报高管</el-tag>
            </div>
            <div style="font-size: 12px; color: #94a3b8;" v-if="scope.row.applicantNo">{{ scope.row.applicantNo }}</div>
          </template>
        </el-table-column>

        <el-table-column prop="typeName" label="请假类型" width="110">
          <template #default="scope">
            <el-tag :type="getTypeTag(scope.row.type)" size="small">{{ scope.row.typeName }}</el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="startTime" label="开始时间" width="165" />
        <el-table-column prop="endTime" label="结束时间" width="165" />

        <el-table-column prop="reason" label="请假事由" min-width="180" />

        <el-table-column label="凭证附件" width="110">
          <template #default="scope">
            <el-button type="primary" link size="small" @click="openAttachments(scope.row.id)">
              <el-icon><Paperclip /></el-icon>
              查看附件
            </el-button>
          </template>
        </el-table-column>

        <el-table-column prop="statusName" label="审批状态" width="110">
          <template #default="scope">
            <el-tag :type="getStatusType(scope.row.status)" size="small">{{ scope.row.statusName }}</el-tag>
          </template>
        </el-table-column>

        <el-table-column label="多级审批日志 / 批示意见" min-width="220">
          <template #default="scope">
            <div v-if="scope.row.audits && scope.row.audits.length > 0" class="audit-log-box">
              <div v-for="aud in scope.row.audits" :key="aud.id" class="audit-log-item">
                <span class="auditor-name">{{ aud.auditorName }}:</span>
                <span class="auditor-comment" :class="{ 'reject-text': aud.action === 'REJECTED', 'escalate-text': aud.action === 'ESCALATE' }">
                  {{ aud.comment || (aud.action === 'APPROVED' ? '同意' : '驳回') }}
                </span>
              </div>
            </div>
            <span v-else style="color: #94a3b8; font-size: 12px;">待处理中</span>
          </template>
        </el-table-column>

        <!-- 多级分流操作列 -->
        <el-table-column label="审批 / 操作" width="240" fixed="right">
          <template #default="scope">
            <!-- 1. 超级管理员：终审全公司请假 (包括部门经理申请 + 上放呈报申请 + 普通员工申请) -->
            <template v-if="isAdmin && isPending(scope.row.status)">
              <el-button type="success" size="small" @click="approveLeave(scope.row)">
                <el-icon><CircleCheck /></el-icon>
                同意
              </el-button>
              <el-button type="danger" size="small" @click="openRejectModal(scope.row)">
                <el-icon><CircleClose /></el-icon>
                驳回
              </el-button>
            </template>

            <!-- 2. 部门经理：审核本部门员工请假 + 支持上放呈报高管批示 -->
            <template v-else-if="isDeptAdmin && activeDeptTab === 'APPROVAL' && isPending(scope.row.status)">
              <el-button type="success" size="small" @click="approveLeave(scope.row)">
                <el-icon><CircleCheck /></el-icon>
                同意
              </el-button>
              <el-button type="danger" size="small" @click="openRejectModal(scope.row)">
                <el-icon><CircleClose /></el-icon>
                驳回
              </el-button>
              <el-button type="warning" plain size="small" @click="openEscalateModal(scope.row)">
                <el-icon><Promotion /></el-icon>
                上报高管
              </el-button>
            </template>

            <!-- 3. 个人申请撤回按钮 (待审批状态下可撤回) -->
            <template v-else-if="(!isDeptAdmin || activeDeptTab === 'MY_LEAVES') && isPending(scope.row.status) && !isAdmin">
              <el-button type="warning" size="small" @click="withdraw(scope.row.id)">
                <el-icon><RefreshLeft /></el-icon>
                撤回申请
              </el-button>
            </template>

            <span v-else style="color: #cbd5e1; font-size: 13px;">流程已归档</span>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="displayLeaves.length === 0 && !loading" description="暂无符合条件的请假记录" />
    </el-card>

    <!-- ===== 新请假单填报 (含 OSS 附件上传) 弹窗 ===== -->
    <el-dialog v-model="dialogVisible" title="新请假单填报" width="540px" append-to-body align-center :close-on-click-modal="false">
      <el-form :model="form" label-position="top">
        <el-form-item label="请假类别" required>
          <el-select v-model="form.type" placeholder="请选择请假类型" style="width: 100%">
            <el-option label="🌴 年假" value="ANNUAL" />
            <el-option label="🤒 病假" value="SICK" />
            <el-option label="📝 事假" value="PERSONAL" />
          </el-select>
        </el-form-item>

        <el-form-item label="起止时间" required>
          <el-date-picker 
            v-model="timeRange" 
            type="datetimerange" 
            value-format="YYYY-MM-DD HH:mm:ss" 
            range-separator="至" 
            start-placeholder="开始时间" 
            end-placeholder="结束时间"
            style="width: 100%" 
          />
        </el-form-item>

        <el-form-item label="事由详情" required>
          <el-input v-model="form.reason" type="textarea" :rows="4" placeholder="请详细说明请假原因..." />
        </el-form-item>

        <!-- 附件上传模块 -->
        <el-form-item label="凭证附件 (支持诊断证明、假条、图片等文档)">
          <el-upload
            action=""
            :auto-upload="false"
            :show-file-list="true"
            :on-change="handleFileChange"
            :on-remove="handleFileRemove"
            :file-list="fileList"
            multiple
            style="width: 100%"
          >
            <el-button type="default">
              <el-icon><Upload /></el-icon>
              选择并添加凭证附件
            </el-button>
            <template #tip>
              <div class="el-upload__tip">
                可上传医院证明、出差事由图片或关联文档，自动同步存储至阿里云 OSS 存储
              </div>
            </template>
          </el-upload>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="submit">确认提交</el-button>
      </template>
    </el-dialog>

    <!-- ===== 驳回理由填写 弹窗 ===== -->
    <el-dialog v-model="rejectModalVisible" title="驳回请假申请" width="480px" append-to-body align-center :close-on-click-modal="false">
      <el-form label-position="top">
        <el-form-item label="驳回理由（必填）" required>
          <el-input 
            v-model="rejectionReason" 
            type="textarea" 
            :rows="4" 
            placeholder="请详细填写驳回该请假申请的具体理由与说明..." 
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectModalVisible = false">取消</el-button>
        <el-button type="danger" :loading="auditLoading" @click="confirmReject">确认驳回</el-button>
      </template>
    </el-dialog>

    <!-- ===== 呈报最高管理员 弹窗 ===== -->
    <el-dialog v-model="escalateModalVisible" title="呈报最高管理员批示" width="480px" append-to-body align-center :close-on-click-modal="false">
      <el-form label-position="top">
        <el-form-item label="呈报说明 (如假程较长/需高管特批)" required>
          <el-input 
            v-model="escalationNote" 
            type="textarea" 
            :rows="4" 
            placeholder="请输入上报至最高管理员批示的说明与原因..." 
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="escalateModalVisible = false">取消</el-button>
        <el-button type="warning" :loading="auditLoading" @click="confirmEscalate">确认呈报高管</el-button>
      </template>
    </el-dialog>

    <!-- ===== 附件明细与下载弹窗 ===== -->
    <el-dialog v-model="showAttachmentModal" title="请假凭证附件" width="500px" append-to-body align-center>
      <div v-loading="loadingAttachments" style="min-height: 100px;">
        <div v-if="currentAttachments.length > 0" class="attachment-list">
          <div v-for="att in currentAttachments" :key="att.id" class="attachment-item">
            <div class="att-info">
              <el-icon class="att-icon"><Paperclip /></el-icon>
              <div class="att-detail">
                <span class="att-name">{{ att.fileName }}</span>
                <span class="att-size">{{ (att.fileSize / 1024).toFixed(1) }} KB</span>
              </div>
            </div>
            <el-button type="primary" link size="small" @click="downloadFile(att.id)">
              <el-icon><Download /></el-icon>
              下载附件
            </el-button>
          </div>
        </div>
        <el-empty v-else description="该请假单未上传附件" />
      </div>
      <template #footer>
        <el-button @click="showAttachmentModal = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { 
  Plus, 
  CircleCheck, 
  CircleClose, 
  RefreshLeft, 
  ArrowLeft, 
  OfficeBuilding, 
  Document,
  Upload,
  Paperclip,
  Download,
  Promotion
} from '@element-plus/icons-vue'
import { 
  getLeavePage, 
  createLeave, 
  withdrawLeave, 
  auditLeave, 
  uploadAttachment, 
  getAttachments, 
  getDownloadUrl 
} from '../api/leave'

const loading = ref(false)
const submitLoading = ref(false)
const auditLoading = ref(false)
const tableData = ref([])
const dialogVisible = ref(false)
const timeRange = ref([])
const fileList = ref([])

const showAttachmentModal = ref(false)
const currentAttachments = ref([])
const loadingAttachments = ref(false)

// 驳回与呈报 Modal 逻辑
const rejectModalVisible = ref(false)
const rejectionReason = ref('')
const selectedLeaveForAudit = ref(null)

const escalateModalVisible = ref(false)
const escalationNote = ref('')

const form = reactive({ type: 'ANNUAL', reason: '' })
const storedRole = ref(localStorage.getItem('role') || 'EMPLOYEE')
const currentUserId = ref(localStorage.getItem('userId') || null)
const currentEmployeeId = ref(localStorage.getItem('employeeId') || null)

const isAdmin = computed(() => storedRole.value === 'SUPER_ADMIN' || storedRole.value === 'ADMIN')
const isDeptAdmin = computed(() => storedRole.value === 'DEPT_MANAGER')
const activeDeptTab = ref('APPROVAL') // 'APPROVAL' or 'MY_LEAVES'

const statusFilter = ref('ALL')
const typeFilter = ref('ALL')

const isPending = (status) => {
  return status === 'PENDING' || status === 0 || status === '0'
}

const isEscalated = (leave) => {
  if (!leave || !leave.audits) return false
  return leave.audits.some(a => a.action === 'ESCALATE' || (a.comment && a.comment.includes('呈报')))
}

const pendingAuditCount = computed(() => {
  return tableData.value.filter(l => isPending(l.status)).length
})

const displayLeaves = computed(() => {
  let list = tableData.value

  if (isDeptAdmin.value) {
    if (activeDeptTab.value === 'MY_LEAVES') {
      list = list.filter(l => String(l.applicantId) === String(currentEmployeeId.value))
    }
  }

  if (statusFilter.value !== 'ALL') {
    list = list.filter(l => {
      if (statusFilter.value === 'PENDING') return isPending(l.status)
      return l.status === statusFilter.value
    })
  }

  if (typeFilter.value !== 'ALL') {
    list = list.filter(l => l.type === typeFilter.value)
  }

  return list
})

const getTypeTag = (type) => {
  switch (type) {
    case 'ANNUAL': return 'primary'
    case 'SICK': return 'warning'
    case 'PERSONAL': return 'danger'
    default: return 'info'
  }
}

const getStatusType = (status) => {
  switch (status) {
    case 'APPROVED':
    case 1: return 'success'
    case 'REJECTED':
    case 2: return 'danger'
    case 'WITHDRAWN': return 'info'
    default: return 'warning'
  }
}

const handleFileChange = (file, list) => {
  fileList.value = list
}

const handleFileRemove = (file, list) => {
  fileList.value = list
}

const loadLeaves = async () => {
  loading.value = true
  try {
    const res = await getLeavePage({ page: 1, size: 100 })
    if (res && res.data && (res.data.records || res.data.list)) {
      const records = res.data.records || res.data.list || []
      tableData.value = records.map(item => ({
        ...item,
        typeName: item.type === 'ANNUAL' ? '年假' : item.type === 'SICK' ? '病假' : item.type === 'PERSONAL' ? '事假' : '其他',
        statusName: item.status === 'APPROVED' ? '已通过' : item.status === 'REJECTED' ? '已驳回' : item.status === 'WITHDRAWN' ? '已撤回' : '待审批'
      }))
    }
  } catch (err) {
    console.error('加载请假记录失败', err)
  } finally {
    loading.value = false
  }
}

const openAttachments = async (leaveId) => {
  showAttachmentModal.value = true
  loadingAttachments.value = true
  currentAttachments.value = []
  try {
    const res = await getAttachments(leaveId)
    if (res && res.data) {
      currentAttachments.value = res.data || []
    }
  } catch (err) {
    ElMessage.error('获取凭证附件失败')
  } finally {
    loadingAttachments.value = false
  }
}

const downloadFile = (attachId) => {
  const url = getDownloadUrl(attachId)
  window.open(url, '_blank')
}

const submit = async () => {
  if (!form.type || !timeRange.value || !timeRange.value.length || !form.reason) {
    return ElMessage.warning('请填写完整的时间起止、请假类型和事由详情')
  }
  
  const payload = {
    type: form.type,
    startTime: timeRange.value[0],
    endTime: timeRange.value[1],
    reason: form.reason
  }
  
  submitLoading.value = true
  try {
    const res = await createLeave(payload)
    if (res && res.code === 200 && res.data) {
      const leaveId = res.data.id

      if (fileList.value && fileList.value.length > 0) {
        for (const fileItem of fileList.value) {
          if (fileItem.raw) {
            await uploadAttachment(leaveId, fileItem.raw).catch(() => null)
          }
        }
      }

      ElMessage.success('请假单提交成功！已发送至上级主管进行审批')
      dialogVisible.value = false
      timeRange.value = []
      fileList.value = []
      form.type = 'ANNUAL'
      form.reason = ''
      loadLeaves()
    }
  } catch (err) {
    ElMessage.error(err.response?.data?.message || '提交失败')
  } finally {
    submitLoading.value = false
  }
}

const withdraw = async (id) => {
  try {
    const res = await withdrawLeave(id)
    if (res && res.code === 200) {
      ElMessage.success('请假申请已成功撤回')
      loadLeaves()
    }
  } catch (err) {
    ElMessage.error(err.response?.data?.message || '撤回失败')
  }
}

const approveLeave = async (leave) => {
  try {
    const res = await auditLeave(leave.id, { 
      action: 'APPROVED', 
      comment: isAdmin.value ? '最高管理员审批通过' : '部门经理审核同意'
    })
    if (res && res.code === 200) {
      ElMessage.success('已审批同意该请假申请！')
      loadLeaves()
    }
  } catch (err) {
    ElMessage.error(err.response?.data?.message || '审批处理失败')
  }
}

const openRejectModal = (leave) => {
  selectedLeaveForAudit.value = leave
  rejectionReason.value = ''
  rejectModalVisible.value = true
}

const confirmReject = async () => {
  if (!rejectionReason.value || !rejectionReason.value.trim()) {
    return ElMessage.warning('驳回请假申请必须填写具体的驳回理由！')
  }
  auditLoading.value = true
  try {
    const res = await auditLeave(selectedLeaveForAudit.value.id, {
      action: 'REJECTED',
      comment: rejectionReason.value.trim()
    })
    if (res && res.code === 200) {
      ElMessage.success('已驳回该请假申请！')
      rejectModalVisible.value = false
      loadLeaves()
    }
  } catch (err) {
    ElMessage.error(err.response?.data?.message || '驳回处理失败')
  } finally {
    auditLoading.value = false
  }
}

const openEscalateModal = (leave) => {
  selectedLeaveForAudit.value = leave
  escalationNote.value = '假程较长/特殊事由，呈报最高管理员批示'
  escalateModalVisible.value = true
}

const confirmEscalate = async () => {
  if (!escalationNote.value || !escalationNote.value.trim()) {
    return ElMessage.warning('请填写呈报最高管理员的具体说明！')
  }
  auditLoading.value = true
  try {
    const res = await auditLeave(selectedLeaveForAudit.value.id, {
      action: 'ESCALATE',
      comment: `【呈报最高管理员批示】${escalationNote.value.trim()}`
    })
    if (res && res.code === 200) {
      ElMessage.success('已将该申请成功呈报至最高管理员！')
      escalateModalVisible.value = false
      loadLeaves()
    }
  } catch (err) {
    ElMessage.error(err.response?.data?.message || '呈报处理失败')
  } finally {
    auditLoading.value = false
  }
}

onMounted(() => {
  if (!localStorage.getItem('token')) return
  loadLeaves()
})
</script>

<style scoped>
.leave-container {
  max-width: 1400px;
}

.page-header {
  margin-bottom: 20px;
}

.page-title {
  font-size: 26px;
  font-weight: bold;
  color: #1a1a2e;
  margin: 0;
}

.page-subtitle {
  font-size: 14px;
  color: #888;
  margin: 6px 0 0 0;
}

.view-tab-switcher {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
}

.tab-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 24px;
  border-radius: 14px;
  border: 1px solid rgba(255, 255, 255, 0.6);
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(12px);
  font-size: 15px;
  font-weight: 600;
  color: #64748b;
  cursor: pointer;
  transition: all 0.3s ease;
}

.tab-btn:hover {
  background: rgba(255, 255, 255, 0.95);
  color: #1a1a2e;
}

.tab-btn.active {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #ffffff;
  box-shadow: 0 6px 20px rgba(102, 126, 234, 0.35);
}

.badge {
  background: #ef4444;
  color: #fff;
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 10px;
}

.filter-card {
  margin-bottom: 20px;
  padding: 16px 20px;
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(16px);
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.6);
}

.filter-row {
  display: flex;
  flex-wrap: wrap;
  gap: 24px;
  align-items: center;
}

.filter-group {
  display: flex;
  align-items: center;
  gap: 8px;
}

.filter-label {
  font-size: 13px;
  font-weight: 600;
  color: #64748b;
}

.leave-card {
  border-radius: 20px;
  border: 1px solid rgba(255, 255, 255, 0.6);
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(16px);
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.04);
}

.audit-log-box {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.audit-log-item {
  font-size: 12px;
  line-height: 1.4;
}

.auditor-name {
  font-weight: 600;
  color: #475569;
  margin-right: 4px;
}

.auditor-comment {
  color: #334155;
}

.reject-text {
  color: #ef4444;
  font-weight: 600;
}

.escalate-text {
  color: #d97706;
  font-weight: 600;
}

.attachment-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.attachment-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: #f8fafc;
  border-radius: 10px;
  border: 1px solid #e2e8f0;
}

.att-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.att-icon {
  font-size: 18px;
  color: #667eea;
}

.att-detail {
  display: flex;
  flex-direction: column;
}

.att-name {
  font-size: 14px;
  font-weight: 600;
  color: #1e293b;
}

.att-size {
  font-size: 12px;
  color: #94a3b8;
}
</style>