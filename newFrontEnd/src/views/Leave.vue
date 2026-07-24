<template>
  <div class="leave-container">
    <div class="page-header">
      <div class="header-left">
        <div class="header-icon">
          <el-icon><Ticket /></el-icon>
        </div>
        <div>
          <h1 class="page-title">{{ isAdmin ? '全员请假审批' : (isDeptAdmin ? '部门请假与审批管理' : '📝 我的请假申请') }}</h1>
          <p class="page-subtitle">{{ isAdmin ? '管理全公司员工请假申请，高效处理审批流程' : (isDeptAdmin ? '审批本部门员工请假申请' : '查看和管理我的请假申请') }}</p>
        </div>
      </div>
      <div class="header-actions" v-if="!isAdmin">
        <el-button type="primary" size="large" @click="dialogVisible = true">
          <el-icon><Plus /></el-icon>
          提交请假单
        </el-button>
      </div>
    </div>

    <div v-if="isDeptAdmin" class="tab-switcher">
      <button class="tab-btn" :class="{ active: activeDeptTab === 'DEPT_LEAVES' }" @click="activeDeptTab = 'DEPT_LEAVES'; loadLeaves()">
        <el-icon><CircleCheck /></el-icon>
        <span>部门审批</span>
      </button>
      <button class="tab-btn" :class="{ active: activeDeptTab === 'MY_LEAVES' }" @click="activeDeptTab = 'MY_LEAVES'; loadLeaves()">
        <el-icon><User /></el-icon>
        <span>我的审批</span>
      </button>
    </div>

    <div class="filter-card">
      <div class="filter-row">
        <div class="filter-group">
          <span class="filter-label">状态筛选</span>
          <div class="filter-chips">
            <span 
              class="filter-chip" 
              :class="{ active: statusFilter === 'ALL' }"
              @click="statusFilter = 'ALL'; loadLeaves()"
            >全部</span>
            <span 
              class="filter-chip pending" 
              :class="{ active: statusFilter === 'PENDING' }"
              @click="statusFilter = 'PENDING'; loadLeaves()"
            >待审批</span>
            <span 
              class="filter-chip approved" 
              :class="{ active: statusFilter === 'APPROVED' }"
              @click="statusFilter = 'APPROVED'; loadLeaves()"
            >已通过</span>
            <span 
              class="filter-chip rejected" 
              :class="{ active: statusFilter === 'REJECTED' }"
              @click="statusFilter = 'REJECTED'; loadLeaves()"
            >已驳回</span>
            <span 
              class="filter-chip withdrawn" 
              :class="{ active: statusFilter === 'WITHDRAWN' }"
              @click="statusFilter = 'WITHDRAWN'; loadLeaves()"
            >已撤回</span>
          </div>
        </div>
        <div class="filter-group">
          <span class="filter-label">请假类型</span>
          <div class="filter-chips">
            <span 
              class="filter-chip" 
              :class="{ active: typeFilter === 'ALL' }"
              @click="typeFilter = 'ALL'; loadLeaves()"
            >全部类型</span>
            <span 
              class="filter-chip personal" 
              :class="{ active: typeFilter === 'PERSONAL' }"
              @click="typeFilter = 'PERSONAL'; loadLeaves()"
            >事假</span>
            <span 
              class="filter-chip sick" 
              :class="{ active: typeFilter === 'SICK' }"
              @click="typeFilter = 'SICK'; loadLeaves()"
            >病假</span>
            <span 
              class="filter-chip annual" 
              :class="{ active: typeFilter === 'ANNUAL' }"
              @click="typeFilter = 'ANNUAL'; loadLeaves()"
            >年假</span>
          </div>
        </div>
      </div>
    </div>

    <div class="leave-list">
      <div 
        class="leave-card" 
        v-for="(leave, index) in paginatedLeaves" 
        :key="leave.id"
        :style="{ animationDelay: index * 0.05 + 's' }"
      >
        <div class="leave-header">
          <div class="leave-type-tag" :class="getTypeClass(leave.type)">
            <el-icon><Bell /></el-icon>
            <span>{{ getTypeName(leave.type) }}</span>
          </div>
          <div class="leave-status-tag" :class="getStatusClass(leave.status)">
            <el-icon><CircleCheck v-if="leave.status === 'APPROVED'" /><Clock v-else-if="leave.status === 'PENDING'" /><CircleClose v-else /></el-icon>
            <span>{{ getStatusName(leave.status) }}</span>
          </div>
        </div>

        <div class="leave-body">
          <div class="leave-info">
            <div class="applicant-info">
              <div class="applicant-avatar">
                <el-icon><User /></el-icon>
              </div>
              <div class="applicant-detail">
                <span class="applicant-name">{{ leave.applicantName }}</span>
                <span class="applicant-no">{{ leave.employeeNo }}</span>
              </div>
            </div>

            <div class="leave-details">
              <div class="detail-item">
                <el-icon class="detail-icon"><Clock /></el-icon>
                <div class="detail-content">
                  <span class="detail-label">请假时间</span>
                  <span class="detail-value">{{ formatTime(leave.startTime) }} - {{ formatTime(leave.endTime) }}</span>
                </div>
              </div>
              <div class="detail-item">
                <el-icon class="detail-icon"><EditPen /></el-icon>
                <div class="detail-content">
                  <span class="detail-label">请假事由</span>
                  <span class="detail-value">{{ leave.reason }}</span>
                </div>
              </div>
              <div class="detail-item" v-if="leave.attachments && leave.attachments.length > 0">
                <el-icon class="detail-icon"><Paperclip /></el-icon>
                <div class="detail-content">
                  <span class="detail-label">证明附件 ({{ leave.attachments.length }})</span>
                  <div class="attachment-chips-wrapper" style="display: flex; flex-wrap: wrap; gap: 8px; margin-top: 6px;">
                    <div 
                      v-for="att in leave.attachments" 
                      :key="att.id" 
                      style="display: flex; align-items: center; background: #f8fafc; border: 1px solid #e2e8f0; padding: 4px 10px; border-radius: 6px; font-size: 13px; color: #334155;"
                    >
                      <span style="margin-right: 8px; max-width: 180px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-weight: 500;">{{ att.fileName }}</span>
                      <el-button type="success" link size="small" @click="previewFile(att)" style="padding: 0 4px;">
                        <el-icon><View /></el-icon> 预览
                      </el-button>
                      <el-button type="primary" link size="small" @click="downloadFile(att)" style="padding: 0 4px;">
                        <el-icon><Download /></el-icon> 下载
                      </el-button>
                    </div>
                  </div>
                </div>
              </div>
              <div class="detail-item" v-if="leave.audits && leave.audits.length > 0">
                <el-icon class="detail-icon"><Message /></el-icon>
                <div class="detail-content">
                  <span class="detail-label">审批日志</span>
                  <div class="audit-logs">
                    <div v-for="aud in leave.audits" :key="aud.id" class="audit-item">
                      <span class="auditor">{{ aud.auditorName }}:</span>
                      <span :class="aud.action === 'REJECTED' ? 'reject-text' : 'audit-text'">
                        {{ aud.comment || (aud.action === 'APPROVED' ? '同意' : '驳回') }}
                      </span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="leave-footer">
          <div class="leave-time">{{ formatFullTime(leave.createTime) }}</div>
          <div class="leave-actions">
            <template v-if="isAdmin && isPending(leave.status)">
              <el-button type="success" size="small" @click="approveLeave(leave)">
                <el-icon><CircleCheck /></el-icon>
                同意
              </el-button>
              <el-button type="danger" size="small" @click="openRejectModal(leave)">
                <el-icon><CircleClose /></el-icon>
                驳回
              </el-button>
            </template>
            <template v-else-if="isDeptAdmin && activeDeptTab === 'APPROVAL' && isPending(leave.status)">
              <el-button type="success" size="small" @click="approveLeave(leave)">
                <el-icon><CircleCheck /></el-icon>
                同意
              </el-button>
              <el-button type="danger" size="small" @click="openRejectModal(leave)">
                <el-icon><CircleClose /></el-icon>
                驳回
              </el-button>
              <el-button type="warning" size="small" @click="openEscalateModal(leave)">
                <el-icon><Promotion /></el-icon>
                上报高管
              </el-button>
            </template>
            <template v-else-if="(!isDeptAdmin || activeDeptTab === 'MY_LEAVES') && isPending(leave.status) && !isAdmin">
              <el-button type="warning" size="small" @click="withdraw(leave.id)">
                <el-icon><RefreshLeft /></el-icon>
                撤回
              </el-button>
            </template>
            <span v-else class="leave-archived">已归档</span>
          </div>
        </div>
      </div>
    </div>

    <div class="pagination-container">
      <span class="pagination-info">总计 {{ displayLeaves.length }} 条记录</span>
      <el-pagination
        v-model:current-page="leaveCurrentPage"
        v-model:page-size="leavePageSize"
        :page-sizes="[5, 10]"
        :total="displayLeaves.length"
        layout="sizes, prev, pager, next, jumper"
      />
    </div>

    <el-dialog v-model="dialogVisible" title="提交请假申请" width="520px">
      <el-form :model="form" label-width="90px">
        <el-form-item required>
          <template #label>请假类型 <span style="color:red">*</span></template>
          <el-select v-model="form.type" style="width: 100%" placeholder="请选择请假类型">
            <el-option label="事假" value="PERSONAL" />
            <el-option label="病假" value="SICK" />
            <el-option label="年假" value="ANNUAL" />
          </el-select>
        </el-form-item>
        <el-form-item required>
          <template #label>请假时间 <span style="color:red">*</span></template>
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
        <el-form-item required>
          <template #label>请假事由 <span style="color:red">*</span></template>
          <el-input v-model="form.reason" type="textarea" :rows="4" placeholder="请详细说明请假原因..." />
        </el-form-item>
        <el-form-item label="证明附件">
          <el-upload
            v-model:file-list="fileList"
            action=""
            :auto-upload="false"
            :limit="3"
            accept=".jpg,.png,.jpeg,.pdf,.doc,.docx,.zip"
          >
            <el-button type="primary" size="small">
              <el-icon><Upload /></el-icon>
              上传证明附件
            </el-button>
            <template #tip>
              <div class="el-upload__tip" style="font-size:12px; color:#94a3b8;">支持图片、PDF、Word 等格式的离岗/假条证明文件</div>
            </template>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submit">提交申请</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="rejectModalVisible" title="驳回申请" width="420px">
      <el-input v-model="rejectionReason" type="textarea" :rows="4" placeholder="请输入驳回理由，将通知申请人" />
      <template #footer>
        <el-button @click="rejectModalVisible = false">取消</el-button>
        <el-button type="danger" @click="confirmReject">确认驳回</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="escalateModalVisible" title="上报高管" width="420px">
      <el-input v-model="escalationNote" type="textarea" :rows="4" placeholder="请输入上报说明" />
      <template #footer>
        <el-button @click="escalateModalVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmEscalate">确认上报</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showAttachmentModal" title="证明附件列表" width="480px">
      <div v-if="currentAttachments.length > 0" class="attachment-list-wrapper">
        <div v-for="att in currentAttachments" :key="att.id" class="attachment-item">
          <div class="attachment-info">
            <el-icon><Document /></el-icon>
            <span class="attachment-name" :title="att.fileName">{{ att.fileName }}</span>
          </div>
          <div class="attachment-actions">
            <el-button type="success" size="small" @click="previewFile(att)">
              <el-icon><View /></el-icon> 预览
            </el-button>
            <el-button type="primary" size="small" @click="downloadFile(att)">
              <el-icon><Download /></el-icon> 下载
            </el-button>
          </div>
        </div>
      </div>
      <el-empty v-else description="暂无证明附件" />
      <template #footer>
        <el-button @click="showAttachmentModal = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- ===== 附件大图在线预览 弹窗 ===== -->
    <el-dialog v-model="previewVisible" title="证明附件预览" width="700px" append-to-body align-center>
      <div style="text-align: center; max-height: 550px; overflow: auto; padding: 12px;">
        <img v-if="isPreviewImage" :src="previewUrl" style="max-width: 100%; height: auto; border-radius: 8px; box-shadow: 0 4px 12px rgba(0,0,0,0.15);" alt="附件预览" />
      </div>
      <template #footer>
        <el-button type="primary" @click="previewVisible = false">关闭预览</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { 
  Ticket, Plus, CircleCheck, Clock, CircleClose, 
  User, EditPen, Message, Paperclip, RefreshLeft, 
  Promotion, Document, Bell, Upload, View, Download
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

const isAdmin = ref(false)
const isDeptAdmin = ref(false)
const loading = ref(false)
const submitting = ref(false)
const fileList = ref([])
const displayLeaves = ref([])
const dialogVisible = ref(false)
const rejectModalVisible = ref(false)
const escalateModalVisible = ref(false)
const showAttachmentModal = ref(false)
const previewVisible = ref(false)
const previewUrl = ref('')
const isPreviewImage = ref(false)
const currentLeave = ref(null)
const currentAttachments = ref([])
const rejectionReason = ref('')
const escalationNote = ref('')
const statusFilter = ref('ALL')
const typeFilter = ref('ALL')
const activeDeptTab = ref('DEPT_LEAVES')

const leaveCurrentPage = ref(1)
const leavePageSize = ref(5)

const form = ref({ type: '', reason: '' })
const timeRange = ref([])

const paginatedLeaves = computed(() => {
  const start = (leaveCurrentPage.value - 1) * leavePageSize.value
  const end = start + leavePageSize.value
  return displayLeaves.value.slice(start, end)
})

const isPending = (status) => status === 'PENDING'

const getTypeName = (type) => {
  const map = { PERSONAL: '事假', SICK: '病假', ANNUAL: '年假' }
  return map[type] || '其他'
}

const getTypeClass = (type) => {
  const map = { PERSONAL: 'personal', SICK: 'sick', ANNUAL: 'annual' }
  return map[type] || 'default'
}

const getStatusName = (status) => {
  const map = { PENDING: '待审批', APPROVED: '已通过', REJECTED: '已驳回', WITHDRAWN: '已撤回' }
  return map[status] || '未知'
}

const getStatusClass = (status) => {
  const map = { PENDING: 'pending', APPROVED: 'approved', REJECTED: 'rejected', WITHDRAWN: 'withdrawn' }
  return map[status] || 'default'
}

const formatTime = (time) => {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}

const formatFullTime = (time) => {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}

const loadLeaves = async () => {
  loading.value = true
  try {
    const params = {}
    if (statusFilter.value !== 'ALL') params.status = statusFilter.value
    if (typeFilter.value !== 'ALL') params.type = typeFilter.value
    if (isDeptAdmin.value && activeDeptTab.value === 'MY_LEAVES') {
      params.mine = true
    } else if (isDeptAdmin.value && activeDeptTab.value === 'DEPT_LEAVES') {
      params.department = true
    }
    const res = await getLeavePage(params)
    displayLeaves.value = res.data?.records || res.data || []
  } catch (err) {
    displayLeaves.value = []
  } finally {
    loading.value = false
  }
}

const submit = async () => {
  if (!form.value.type || !timeRange.value.length || !form.value.reason) {
    ElMessage.warning('请填写完整的请假信息')
    return
  }
  submitting.value = true
  try {
    const res = await createLeave({
      type: form.value.type,
      startTime: timeRange.value[0],
      endTime: timeRange.value[1],
      reason: form.value.reason
    })

    const leaveId = res.data?.id || res.data
    if (leaveId && fileList.value.length > 0) {
      for (const f of fileList.value) {
        if (f.raw) {
          await uploadAttachment(leaveId, f.raw).catch(() => null)
        }
      }
    }

    ElMessage.success('请假申请提交成功！')
    dialogVisible.value = false
    form.value = { type: '', reason: '' }
    timeRange.value = []
    fileList.value = []
    await loadLeaves()
  } catch (err) {
    ElMessage.error(err.response?.data?.message || '提交失败')
  } finally {
    submitting.value = false
  }
}

const approveLeave = async (row) => {
  try {
    await auditLeave(row.id, { action: 'APPROVED' })
    ElMessage.success('审批通过')
    await loadLeaves()
  } catch (err) {
    ElMessage.error('操作失败')
  }
}

const openRejectModal = (row) => {
  currentLeave.value = row
  rejectionReason.value = ''
  rejectModalVisible.value = true
}

const confirmReject = async () => {
  if (!rejectionReason.value.trim()) {
    ElMessage.warning('请填写驳回理由')
    return
  }
  try {
    await auditLeave(currentLeave.value.id, { action: 'REJECTED', comment: rejectionReason.value })
    ElMessage.success('已驳回')
    rejectModalVisible.value = false
    await loadLeaves()
  } catch (err) {
    ElMessage.error('操作失败')
  }
}

const openEscalateModal = (row) => {
  currentLeave.value = row
  escalationNote.value = ''
  escalateModalVisible.value = true
}

const confirmEscalate = async () => {
  try {
    await auditLeave(currentLeave.value.id, { action: 'ESCALATE', comment: escalationNote.value })
    ElMessage.success('已呈报高管')
    escalateModalVisible.value = false
    await loadLeaves()
  } catch (err) {
    ElMessage.error('操作失败')
  }
}

const withdraw = async (id) => {
  try {
    await withdrawLeave(id)
    ElMessage.success('已撤回')
    await loadLeaves()
  } catch (err) {
    ElMessage.error('操作失败')
  }
}

const openAttachments = async (id) => {
  currentAttachments.value = []
  showAttachmentModal.value = true
  try {
    const res = await getAttachments(id)
    currentAttachments.value = res.data || []
  } catch (err) {
    ElMessage.error('获取附件失败')
  }
}

const previewFile = (att) => {
  const url = getDownloadUrl(att.id)
  const name = (att.fileName || '').toLowerCase()
  if (name.endsWith('.png') || name.endsWith('.jpg') || name.endsWith('.jpeg') || name.endsWith('.gif') || name.endsWith('.webp') || name.endsWith('.svg')) {
    previewUrl.value = url
    isPreviewImage.value = true
    previewVisible.value = true
  } else {
    window.open(url, '_blank')
  }
}

const downloadFile = (att) => {
  const id = typeof att === 'object' ? att.id : att
  const url = getDownloadUrl(id)
  const a = document.createElement('a')
  a.href = url
  if (typeof att === 'object' && att.fileName) {
    a.download = att.fileName
  }
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
}

onMounted(async () => {
  const role = localStorage.getItem('role') || ''
  isAdmin.value = role === 'ADMIN' || role === 'SUPER_ADMIN'
  isDeptAdmin.value = role === 'DEPARTMENT_ADMIN' || role === 'DEPT_MANAGER'
  await loadLeaves()
})
</script>

<style scoped>
.leave-container {
  padding: 24px;
  min-height: 100vh;
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding: 28px 32px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 20px;
  box-shadow: 0 12px 40px rgba(102, 126, 234, 0.3);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 20px;
}

.header-icon {
  width: 56px;
  height: 56px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  color: #ffffff;
}

.page-title {
  font-size: 26px;
  font-weight: 700;
  color: #ffffff;
  margin: 0;
}

.page-subtitle {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.85);
  margin: 4px 0 0 0;
}

.header-actions {
  display: flex;
  gap: 12px;
}

.header-actions .el-button {
  background: rgba(255, 255, 255, 0.25);
  border: 1px solid rgba(255, 255, 255, 0.3);
  color: #ffffff;
  border-radius: 12px;
  padding: 10px 24px;
  font-weight: 500;
}

.header-actions .el-button:hover {
  background: rgba(255, 255, 255, 0.4);
}

.tab-switcher {
  display: flex;
  gap: 8px;
  margin-bottom: 20px;
  padding: 8px;
  background: #ffffff;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  width: fit-content;
}

.tab-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 28px;
  border: none;
  border-radius: 10px;
  background: transparent;
  color: #64748b;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
}

.tab-btn.active {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #ffffff;
  box-shadow: 0 4px 15px rgba(102, 126, 234, 0.3);
}

.filter-card {
  background: #ffffff;
  border-radius: 16px;
  padding: 20px 24px;
  margin-bottom: 24px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.04);
}

.filter-row {
  display: flex;
  gap: 32px;
  flex-wrap: wrap;
}

.filter-group {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.filter-label {
  font-size: 13px;
  font-weight: 600;
  color: #475569;
}

.filter-chips {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.filter-chip {
  padding: 8px 16px;
  border-radius: 20px;
  background: #f1f5f9;
  color: #64748b;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.3s ease;
  border: 1px solid transparent;
}

.filter-chip:hover {
  background: #e2e8f0;
}

.filter-chip.active {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #ffffff;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

.filter-chip.pending.active {
  background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%);
}

.filter-chip.approved.active {
  background: linear-gradient(135deg, #22c55e 0%, #16a34a 100%);
}

.filter-chip.rejected.active {
  background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%);
}

.filter-chip.withdrawn.active {
  background: linear-gradient(135deg, #64748b 0%, #475569 100%);
}

.filter-chip.personal.active {
  background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%);
}

.filter-chip.sick.active {
  background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%);
}

.filter-chip.annual.active {
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
}

.leave-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.leave-card {
  background: #ffffff;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.04);
  border: 1px solid rgba(226, 232, 240, 0.6);
  animation: cardSlideIn 0.4s ease forwards;
  opacity: 0;
  transform: translateY(10px);
}

@keyframes cardSlideIn {
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.leave-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f1f5f9;
}

.leave-type-tag {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 500;
}

.leave-type-tag.personal {
  background: rgba(245, 158, 11, 0.1);
  color: #f59e0b;
}

.leave-type-tag.sick {
  background: rgba(239, 68, 68, 0.1);
  color: #ef4444;
}

.leave-type-tag.annual {
  background: rgba(59, 130, 246, 0.1);
  color: #3b82f6;
}

.leave-status-tag {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 500;
}

.leave-status-tag.pending {
  background: rgba(245, 158, 11, 0.1);
  color: #f59e0b;
}

.leave-status-tag.approved {
  background: rgba(34, 197, 94, 0.1);
  color: #22c55e;
}

.leave-status-tag.rejected {
  background: rgba(239, 68, 68, 0.1);
  color: #ef4444;
}

.leave-status-tag.withdrawn {
  background: rgba(100, 116, 139, 0.1);
  color: #64748b;
}

.leave-body {
  margin-bottom: 20px;
}

.leave-info {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.applicant-info {
  display: flex;
  align-items: center;
  gap: 14px;
}

.applicant-avatar {
  width: 48px;
  height: 48px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  color: #ffffff;
}

.applicant-detail {
  display: flex;
  flex-direction: column;
}

.applicant-name {
  font-size: 16px;
  font-weight: 600;
  color: #1e293b;
}

.applicant-no {
  font-size: 13px;
  color: #94a3b8;
}

.leave-details {
  display: flex;
  flex-direction: column;
  gap: 14px;
  margin-left: 62px;
}

.detail-item {
  display: flex;
  gap: 12px;
}

.detail-icon {
  width: 32px;
  height: 32px;
  background: #f8fafc;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  color: #667eea;
  flex-shrink: 0;
}

.detail-content {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.detail-label {
  font-size: 12px;
  color: #94a3b8;
}

.detail-value {
  font-size: 14px;
  color: #334155;
  font-weight: 500;
}

.audit-logs {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.audit-item {
  font-size: 13px;
}

.auditor {
  font-weight: 600;
  color: #64748b;
}

.audit-text {
  color: #334155;
}

.reject-text {
  color: #ef4444;
}

.leave-attachments {
  margin-left: 62px;
}

.leave-attachments .el-button {
  color: #667eea;
  font-size: 13px;
}

.leave-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 16px;
  border-top: 1px solid #f1f5f9;
}

.leave-time {
  font-size: 12px;
  color: #94a3b8;
}

.leave-actions {
  display: flex;
  gap: 8px;
  align-items: center;
}

.leave-archived {
  font-size: 13px;
  color: #94a3b8;
  padding: 6px 12px;
  background: #f1f5f9;
  border-radius: 8px;
}

.pagination-container {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 24px;
  padding: 16px 24px;
  background: #ffffff;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
}

.pagination-info {
  font-size: 14px;
  color: #64748b;
}

.attachment-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid #f1f5f9;
}

.attachment-info {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 14px;
  color: #334155;
}
</style>
