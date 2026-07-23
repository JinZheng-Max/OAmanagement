<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import dayjs from 'dayjs'
import { useAuthStore } from '../../stores/auth'
import {
  getLeavePage, createLeave, withdrawLeave, auditLeave,
  getAttachments, uploadAttachment, getDownloadUrl,
  type LeaveRecord, type AttachmentInfo,
} from '../../api/leave'

const auth = useAuthStore()
const isAdmin = auth.isAdmin
const loading = ref(false)
const list = ref<LeaveRecord[]>([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const filterStatus = ref('')
const filterType = ref('')

// 提交请假
const formVisible = ref(false)
const formLoading = ref(false)
const form = ref({ type: 'SICK', startTime: null as Date | null, endTime: null as Date | null, reason: '' })
const selectedFiles = ref<File[]>([])
const uploadedAttachments = ref<AttachmentInfo[]>([])

// ===== 新版：综合详情窗口（详情+附件预览+审批） =====
const detailVisible = ref(false)
const detailData = ref<LeaveRecord | null>(null)
const detailAttachments = ref<AttachmentInfo[]>([])
const detailApproving = ref(false)
const previewVis = ref(false)
const previewImg = ref('')

const typeOptions = [
  { value: 'ANNUAL', label: '年假' }, { value: 'SICK', label: '病假' },
  { value: 'PERSONAL', label: '事假' }, { value: 'OTHER', label: '其他' },
]
const statusMap: Record<string, { label: string; type: string }> = {
  PENDING: { label: '待审批', type: 'warning' },
  APPROVED: { label: '已通过', type: 'success' },
  REJECTED: { label: '已驳回', type: 'danger' },
  WITHDRAWN: { label: '已撤回', type: 'info' },
}
const isImage = (m: string) => m?.startsWith('image/')

// 时间限制
const disabledStartTime = (time: Date) => time.getTime() < Date.now() - 60000
const disabledEndTime = (time: Date) => {
  if (!form.value.startTime) return time.getTime() < Date.now() - 60000
  return time.getTime() <= form.value.startTime.getTime()
}

onMounted(() => fetchData())

async function fetchData() {
  loading.value = true
  try {
    const r = await getLeavePage({ page: currentPage.value, size: pageSize.value, status: filterStatus.value || undefined, type: filterType.value || undefined })
    list.value = r.records; total.value = r.total
  } catch { ElMessage.error('获取失败') }
  finally { loading.value = false }
}

function handleSearch() { currentPage.value = 1; fetchData() }

// ---- 提交请假 ----
function openForm() {
  form.value = { type: 'SICK', startTime: null, endTime: null, reason: '' }
  selectedFiles.value = []; uploadedAttachments.value = []
  formVisible.value = true
}

function onFileSelect(upload: any) {
  const file = upload.raw
  if (file.size / 1024 / 1024 > 10) { ElMessage.warning('文件不能超过10MB'); return }
  selectedFiles.value.push(file)
}

function formatDate(d: Date | null) { return d ? dayjs(d).format('YYYY-MM-DD HH:mm:ss') : '' }

async function submitLeave() {
  if (!form.value.startTime || !form.value.endTime) { ElMessage.warning('请选择完整的起止时间'); return }
  if (form.value.endTime.getTime() <= form.value.startTime.getTime()) { ElMessage.warning('结束时间必须大于开始时间'); return }
  if (selectedFiles.value.length === 0) { ElMessage.warning('请上传证明材料'); return }
  formLoading.value = true
  try {
    const newLeave = await createLeave({ type: form.value.type, startTime: formatDate(form.value.startTime), endTime: formatDate(form.value.endTime), reason: form.value.reason })
    let failCount = 0
    for (const f of selectedFiles.value) {
      try { await uploadAttachment(newLeave.id, f) } catch { failCount++ }
    }
    if (failCount === 0) {
      ElMessage.success('提交成功')
    } else {
      ElMessage.warning(`请假已提交，但 ${failCount} 个附件上传失败`)
    }
    selectedFiles.value = []; formVisible.value = false; fetchData()
  } catch (err: any) { ElMessage.error(err?.response?.data?.message || '提交失败') }
  finally { formLoading.value = false }
}

// ---- 撤回 ----
async function doWithdraw(id: number) {
  try {
    await ElMessageBox.confirm('确认撤回该申请？', '提示', { type: 'warning' })
    await withdrawLeave(id); ElMessage.success('已撤回'); fetchData()
  } catch {}
}

// ===== 新版：打开综合详情窗口 =====
async function openDetail(row: LeaveRecord) {
  detailData.value = row
  detailAttachments.value = []
  detailVisible.value = true
  try { detailAttachments.value = await getAttachments(row.id) } catch {}
}

// ---- 从详情窗口直接审批 ----
async function doAudit(action: string) {
  if (!detailData.value) return
  let comment = ''
  if (action === 'REJECTED') {
    try {
      const result = await ElMessageBox.prompt('请填写驳回理由', '驳回申请', {
        confirmButtonText: '确认驳回', cancelButtonText: '取消',
        inputType: 'textarea', inputPlaceholder: '请说明驳回原因...',
        inputValidator: (v: string) => !!v.trim() || '驳回理由不能为空',
      })
      comment = result.value
    } catch { return }  // 用户取消
  }
  detailApproving.value = true
  try {
    await auditLeave(detailData.value.id, { action, comment: comment || undefined })
    ElMessage.success(action === 'APPROVED' ? '已通过' : '已驳回')
    detailVisible.value = false; fetchData()
  } catch (err: any) { ElMessage.error(err?.response?.data?.message || '审批失败') }
  finally { detailApproving.value = false }
}

/** 带认证头下载附件 */
async function doDownload(attachId: number, fileName: string) {
  try {
    const token = localStorage.getItem('oa_token')
    const res = await fetch(`/api/leaves/attachments/${attachId}/download`, {
      headers: { Authorization: `Bearer ${token}` },
    })
    if (!res.ok) { ElMessage.error('下载失败'); return }
    const blob = await res.blob()
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url; a.download = fileName
    document.body.appendChild(a); a.click()
    document.body.removeChild(a); URL.revokeObjectURL(url)
  } catch { ElMessage.error('下载失败') }
}

const typeLabel = (t: string) => typeOptions.find(o => o.value === t)?.label || t
</script>

<template>
  <div class="leave-page">
    <div class="page-header">
      <h2>请假审批</h2>
      <el-button type="primary" @click="openForm">+ 提交申请</el-button>
    </div>

    <div class="search-bar">
      <el-select v-model="filterStatus" placeholder="按状态" clearable style="width:130px" @change="handleSearch">
        <el-option label="待审批" value="PENDING" /><el-option label="已通过" value="APPROVED" />
        <el-option label="已驳回" value="REJECTED" /><el-option label="已撤回" value="WITHDRAWN" />
      </el-select>
      <el-select v-model="filterType" placeholder="按类型" clearable style="width:130px" @change="handleSearch">
        <el-option v-for="o in typeOptions" :key="o.value" :label="o.label" :value="o.value" />
      </el-select>
      <el-button type="primary" @click="handleSearch">查询</el-button>
    </div>

    <el-table :data="list" v-loading="loading" border stripe class="leave-table" empty-text="暂无记录">
      <el-table-column type="index" label="#" width="50" header-align="center" align="center" />
      <el-table-column prop="applicantName" label="申请人" width="100" header-align="center" align="center" />
      <el-table-column label="类型" width="75" header-align="center" align="center">
        <template #default="{ row }">{{ typeLabel(row.type) }}</template>
      </el-table-column>
      <el-table-column label="开始" width="150" header-align="center" align="center">
        <template #default="{ row }">{{ row.startTime?.replace('T',' ') }}</template>
      </el-table-column>
      <el-table-column label="结束" width="150" header-align="center" align="center">
        <template #default="{ row }">{{ row.endTime?.replace('T',' ') }}</template>
      </el-table-column>
      <el-table-column prop="reason" label="原因" min-width="120" header-align="center" align="center" show-overflow-tooltip />
      <el-table-column label="状态" width="85" header-align="center" align="center">
        <template #default="{ row }">
          <el-tag :type="statusMap[row.status]?.type" size="small">{{ statusMap[row.status]?.label }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="180" fixed="right" header-align="center" align="center">
        <template #default="{ row }">
          <el-button size="small" type="primary" link @click="openDetail(row)">详情</el-button>
          <el-button v-if="row.status==='PENDING' && !isAdmin" size="small" type="danger" link @click="doWithdraw(row.id)">撤回</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-wrapper">
      <el-pagination v-model:current-page="currentPage" v-model:page-size="pageSize"
        :page-sizes="[10,20,50]" :total="total" layout="total, sizes, prev, pager, next, jumper" background
        @current-change="fetchData" @size-change="() => { currentPage=1; fetchData() }" />
    </div>

    <!-- ===== 提交请假弹窗 ===== -->
    <el-dialog v-model="formVisible" title="提交请假申请" width="540px" :close-on-click-modal="false">
      <el-form :model="form" label-width="90px">
        <el-form-item label="请假类型" required>
          <el-select v-model="form.type" style="width:100%">
            <el-option v-for="o in typeOptions" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="开始" required>
          <el-date-picker v-model="form.startTime" type="datetime" placeholder="选择开始时间" style="width:100%"
            :disabled-date="disabledStartTime" format="YYYY-MM-DD HH:mm" />
        </el-form-item>
        <el-form-item label="结束" required>
          <el-date-picker v-model="form.endTime" type="datetime" placeholder="选择结束时间" style="width:100%"
            :disabled-date="disabledEndTime" format="YYYY-MM-DD HH:mm" />
        </el-form-item>
        <el-form-item label="原因" required>
          <el-input v-model="form.reason" type="textarea" :rows="3" placeholder="请填写请假原因" />
        </el-form-item>
        <el-form-item label="证明材料" required>
          <div style="width:100%;">
            <el-upload :auto-upload="false" :show-file-list="false" :on-change="onFileSelect" accept="image/*,.pdf">
              <el-button size="small" type="primary" plain><el-icon><Plus /></el-icon> 添加文件</el-button>
              <span style="font-size:12px;color:#999;margin-left:10px;">JPG/PNG/PDF，最多10MB</span>
            </el-upload>
            <div v-if="selectedFiles.length" style="margin-top:8px;">
              <div v-for="(f,i) in selectedFiles" :key="i" class="file-row">
                <el-icon><Document /></el-icon>
                <span class="file-name">{{ f.name }}</span>
                <span class="file-size">{{ (f.size/1024).toFixed(1) }}KB</span>
                <el-button size="small" text type="danger" @click="selectedFiles.splice(i,1)">删除</el-button>
              </div>
            </div>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="formLoading" @click="submitLeave">提交</el-button>
      </template>
    </el-dialog>

    <!-- ===== 综合详情窗口（详情 + 附件轮播 + 审批）===== -->
    <el-dialog v-model="detailVisible" width="900px" :close-on-click-modal="false" class="detail-dialog">
      <template #title>
        <span style="font-weight:600;font-size:17px;color:#1d2129;">
          请假详情 #{{ detailData?.id }}
          <el-tag v-if="detailData" :type="statusMap[detailData.status]?.type" size="small" style="margin-left:8px;">
            {{ statusMap[detailData.status]?.label }}
          </el-tag>
        </span>
      </template>

      <template v-if="detailData">
        <div class="detail-grid">
          <!-- 左栏：申请信息 -->
          <div class="detail-left">
            <div class="detail-row"><label>申请人</label><span>{{ detailData.applicantName }} ({{ detailData.applicantNo }})</span></div>
            <div class="detail-row"><label>类　型</label><span>{{ typeLabel(detailData.type) }}</span></div>
            <div class="detail-row"><label>开始时间</label><span>{{ detailData.startTime?.replace('T',' ') }}</span></div>
            <div class="detail-row"><label>结束时间</label><span>{{ detailData.endTime?.replace('T',' ') }}</span></div>
            <div class="detail-row"><label>原　因</label><span>{{ detailData.reason }}</span></div>
            <div class="detail-row"><label>创建时间</label><span>{{ detailData.createTime }}</span></div>
            <!-- 审批记录 -->
            <div v-if="detailData.audits?.length" style="margin-top:12px;">
              <div style="font-size:14px;font-weight:600;color:#1d2129;margin-bottom:6px;">审批记录</div>
              <div v-for="a in detailData.audits" :key="a.id" class="audit-item">
                <span class="audit-badge" :class="a.action">{{ a.action==='APPROVED'?'通过':'驳回' }}</span>
                <span>{{ a.auditorName }} · {{ a.auditTime?.replace('T',' ') }}</span>
                <span v-if="a.comment" style="color:#666;display:block;margin-top:2px;">意见：{{ a.comment }}</span>
              </div>
            </div>
          </div>
          <!-- 右栏：附件轮播 -->
          <div class="detail-right">
            <div style="font-size:14px;font-weight:600;color:#1d2129;margin-bottom:10px;">
              附件（{{ detailAttachments.length }}）
            </div>
            <div v-if="detailAttachments.length" class="carousel-wrapper">
              <el-carousel :interval="0" arrow="always" height="280px" indicator-position="none">
                <el-carousel-item v-for="att in detailAttachments" :key="att.id">
                  <div class="carousel-content">
                    <img v-if="isImage(att.mimeType)" :src="att.fileUrl" class="carousel-img" @click="previewImg=att.fileUrl; previewVis=true" />
                    <div v-else class="file-placeholder">
                      <el-icon :size="48" color="#409eff"><Document /></el-icon>
                      <div class="file-name">{{ att.fileName }}</div>
                    </div>
                    <div class="carousel-footer">
                      <span class="file-name">{{ att.fileName }}</span>
                      <span class="file-size">({{ (att.fileSize/1024).toFixed(1) }}KB)</span>
                      <a href="javascript:;" class="download-link" @click="doDownload(att.id, att.fileName)">
                        <el-icon><Download /></el-icon> 下载
                      </a>
                    </div>
                  </div>
                </el-carousel-item>
              </el-carousel>
            </div>
            <div v-else class="no-att">暂无附件</div>
          </div>
        </div>

        <!-- 底部审批按钮（仅管理员 + 待审批状态） -->
        <div v-if="isAdmin && detailData.status==='PENDING'" class="audit-footer">
          <span style="font-size:14px;color:#666;">审批操作：</span>
          <el-button type="success" :loading="detailApproving" @click="doAudit('APPROVED')">
            <el-icon><Check /></el-icon> 通过
          </el-button>
          <el-button type="danger" :loading="detailApproving" @click="doAudit('REJECTED')">
            <el-icon><Close /></el-icon> 驳回
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 图片大图预览 -->
    <el-dialog v-model="previewVis" title="图片预览" width="700px" :close-on-click-modal="true">
      <img :src="previewImg" style="width:100%;" />
    </el-dialog>
  </div>
</template>

<style scoped>
.leave-page { padding: 16px 20px; background: #fff; border-radius: 8px; min-height: calc(100vh - 80px); }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; padding-bottom: 14px; border-bottom: 2px solid #f0f2f5; }
.page-header h2 { margin: 0; font-size: 22px; font-weight: 700; color: #1d2129; }
.page-header h2::before { content: ''; display: inline-block; width: 4px; height: 22px; background: #409eff; border-radius: 2px; margin-right: 10px; vertical-align: -3px; }
.search-bar { display: flex; gap: 10px; margin-bottom: 16px; padding: 14px 16px; background: #f7f8fa; border-radius: 6px; align-items: center; flex-wrap:wrap; }
.leave-table :deep(th.el-table__cell) { text-align: center !important; }
.leave-table :deep(.el-table__cell) { text-align: center; }
.pagination-wrapper { display: flex; justify-content: flex-end; margin-top: 14px; }
.file-row { display:flex; align-items:center; gap:8px; padding:4px 0; font-size:13px; }
.file-name { flex:1; overflow:hidden; text-overflow:ellipsis; white-space:nowrap; }
.file-size { color:#999; font-size:12px; white-space:nowrap; }

/* 综合详情窗口 */
.detail-dialog :deep(.el-dialog__body) { padding: 20px 24px; }
.detail-grid { display: flex; gap: 28px; min-height: 300px; }
.detail-left { flex: 1; min-width: 0; background: #fafbfc; border-radius: 8px; padding: 16px 18px; }
.detail-right { width: 380px; flex-shrink: 0; background: #fafbfc; border-radius: 8px; padding: 16px 18px; }
.detail-row { display: flex; padding: 8px 0; border-bottom: 1px solid #f0f0f0; font-size: 13px; }
.detail-row:last-child { border-bottom: none; }
.detail-row label { width: 75px; color: #666; flex-shrink: 0; font-weight: 500; }
.detail-row span { color: #1d2129; }

.audit-item { padding: 6px 0; border-bottom: 1px dashed #f5f5f5; font-size: 13px; }
.audit-badge { display:inline-block; padding:1px 8px; border-radius:3px; font-size:12px; font-weight:600; margin-right:6px; }
.audit-badge.APPROVED { background:#e1f3d8; color:#67c23a; }
.audit-badge.REJECTED { background:#fde2e2; color:#f56c6c; }

/* 轮播 */
.carousel-wrapper { border: 1px solid #e8edf3; border-radius: 10px; overflow: hidden; background: #fff; }
.carousel-content { text-align: center; padding: 14px; }
.carousel-img { max-width:100%; max-height:230px; object-fit:contain; cursor:zoom-in; border-radius:4px; }
.file-placeholder { padding: 70px 0; }
.carousel-footer { display:flex; align-items:center; justify-content:center; gap:10px; margin-top:10px; padding-top:8px; border-top:1px solid #f0f0f0; font-size:13px; }
.no-att { text-align:center; color:#bbb; padding:70px 0; font-size:14px; }
:deep(.el-carousel__arrow) { background: rgba(64,158,255,0.8); color:#fff; font-size:18px; }
:deep(.el-carousel__arrow:hover) { background: #409eff; }
.download-link { display:inline-flex; align-items:center; gap:4px; color:#409eff; text-decoration:none; font-size:13px; }
.download-link:hover { color:#66b1ff; }

/* 底部审批 */
.audit-footer { display:flex; align-items:center; gap:14px; margin-top:18px; padding:16px 20px; background:linear-gradient(135deg,#f0f9ff,#f5f0ff); border-radius:10px; }
</style>
