<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox, ElTooltip } from 'element-plus'
import {
  listSources, uploadSource, updateSourceStatus, updateSourceAccess, reindexSource
} from '../../api/ai'
import { listActiveDepartments } from '../../api/department'
import type { AiSource } from '../../api/ai'
import type { DepartmentInfo } from '../../api/department'

const sources = ref<AiSource[]>([])
const departments = ref<DepartmentInfo[]>([])
const loading = ref(false)
const uploadLoading = ref(false)
const uploadDialog = ref(false)
const accessDialog = ref(false)
const currentAccessItem = ref<AiSource | null>(null)

const categories = ['企业文化', '新人培训', '技术部资料', '人事制度', '财务制度', '行政管理', '法务合规', '信息安全', '部门资料', '其他']
const roleLevels = [
  { value: 1, label: '全员可见', desc: '普通员工及以上均可访问' },
  { value: 2, label: '经理级以上', desc: '部门经理及以上可访问' },
  { value: 3, label: '仅管理员', desc: '总管理员可访问' },
]

const uploadForm = ref({
  title: '',
  category: '',
  description: '',
  accessScope: 'ALL',
  accessDepartmentIds: [] as number[],
  minRoleLevel: 1,
  file: null as File | null,
})

const accessForm = ref({
  accessScope: 'ALL',
  accessDepartmentIds: [] as number[],
  minRoleLevel: 1,
})

/** 构建部门ID->名称的映射 */
const deptMap = computed(() => {
  const map: Record<number, string> = {}
  departments.value.forEach(d => { map[d.id] = d.name })
  return map
})

/** 将逗号分隔的部门ID转为部门名列表 */
function getDeptNames(deptIdsStr: string | null | undefined): string[] {
  if (!deptIdsStr) return []
  return deptIdsStr.split(',').map(id => {
    const numId = parseInt(id.trim())
    return deptMap.value[numId] || `部门#${numId}`
  }).filter(Boolean)
}

async function fetchData() {
  loading.value = true
  try {
    sources.value = await listSources()
  } catch { /* ignore */ } finally { loading.value = false }
}

async function fetchDepartments() {
  try {
    departments.value = await listActiveDepartments()
  } catch { /* ignore */ }
}

async function handleUpload() {
  if (!uploadForm.value.file) { ElMessage.warning('请选择文件'); return }
  uploadLoading.value = true
  const fd = new FormData()
  fd.append('file', uploadForm.value.file)
  if (uploadForm.value.title) fd.append('title', uploadForm.value.title)
  if (uploadForm.value.category) fd.append('category', uploadForm.value.category)
  if (uploadForm.value.description) fd.append('description', uploadForm.value.description)
  fd.append('accessScope', uploadForm.value.accessScope)
  if (uploadForm.value.accessDepartmentIds.length > 0) {
    fd.append('accessDepartmentIds', uploadForm.value.accessDepartmentIds.join(','))
  }
  fd.append('minRoleLevel', String(uploadForm.value.minRoleLevel))
  try {
    await uploadSource(fd)
    ElMessage.success('上传成功，正在后台索引...')
    uploadDialog.value = false
    resetUploadForm()
    setTimeout(fetchData, 2000)
  } catch (err: any) {
    ElMessage.error(err?.response?.data?.message || '上传失败')
  } finally { uploadLoading.value = false }
}

async function toggleStatus(item: AiSource) {
  const newStatus = item.status === 'ENABLED' ? 'DISABLED' : 'ENABLED'
  const action = newStatus === 'ENABLED' ? '启用' : '停用'
  try {
    await ElMessageBox.confirm(`确定${action}文档「${item.title}」吗？`, '提示', {
      confirmButtonText: action,
      cancelButtonText: '取消',
      type: 'warning',
    })
    await updateSourceStatus(item.id, newStatus)
    item.status = newStatus
    ElMessage.success(`已${action}`)
  } catch { /* cancelled */ }
}

async function handleReindex(item: AiSource) {
  try {
    await ElMessageBox.confirm(
      `重新索引将重新解析文档并更新向量库，适用于索引异常后的恢复操作。确定重新索引「${item.title}」吗？`,
      '重新索引',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'info' }
    )
    await reindexSource(item.id)
    ElMessage.success('已开始重新索引，稍后刷新')
    // 乐观更新状态，但不影响操作按钮
    setTimeout(fetchData, 3000)
  } catch { /* cancelled */ }
}

function openAccessDialog(item: AiSource) {
  currentAccessItem.value = item
  const ids: number[] = []
  if (item.accessDepartmentId) {
    item.accessDepartmentId.split(',').forEach(id => {
      const n = parseInt(id.trim())
      if (!isNaN(n)) ids.push(n)
    })
  }
  accessForm.value = {
    accessScope: item.accessScope,
    accessDepartmentIds: ids,
    minRoleLevel: item.minRoleLevel,
  }
  accessDialog.value = true
}

async function saveAccess() {
  if (!currentAccessItem.value) return
  const item = currentAccessItem.value
  try {
    const idsStr = accessForm.value.accessDepartmentIds.length > 0
      ? accessForm.value.accessDepartmentIds.join(',')
      : null
    await updateSourceAccess(item.id, {
      accessScope: accessForm.value.accessScope,
      accessDepartmentIds: idsStr,
      minRoleLevel: accessForm.value.minRoleLevel,
    })
    item.accessScope = accessForm.value.accessScope
    item.accessDepartmentId = idsStr
    item.minRoleLevel = accessForm.value.minRoleLevel
    ElMessage.success('权限范围已更新')
    accessDialog.value = false
  } catch (err: any) {
    ElMessage.error(err?.response?.data?.message || '更新失败')
  }
}

function resetUploadForm() {
  uploadForm.value = { title: '', category: '', description: '', accessScope: 'ALL', accessDepartmentIds: [], minRoleLevel: 1, file: null }
}

/** 文档就绪状态（面向管理员） */
function readinessStatus(item: AiSource): { text: string; type: string } {
  if (item.status === 'DISABLED') return { text: '已停用', type: 'info' }
  if (item.parseStatus === 'FAILED' || item.indexStatus === 'FAILED') return { text: '异常', type: 'danger' }
  if (item.indexStatus === 'SUCCESS') return { text: '已就绪', type: 'success' }
  if (item.parseStatus === 'PROCESSING' || item.indexStatus === 'PROCESSING') return { text: '索引中', type: 'warning' }
  return { text: '排队中', type: 'info' }
}

/** 角色等级可读标签 */
function roleLabel(level: number): string {
  return roleLevels.find(r => r.value === level)?.label || `Lv${level}`
}

onMounted(() => {
  fetchData()
  fetchDepartments()
})
</script>

<template>
  <div class="source-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-left">
        <h2>📚 知识文档管理</h2>
        <p class="subtitle">管理企业知识文档 — 上传后自动解析并加入 AI 知识库</p>
      </div>
      <el-button type="primary" size="default" @click="uploadDialog = true">
        <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" style="margin-right:4px;vertical-align:-2px"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="17 8 12 3 7 8"/><line x1="12" y1="3" x2="12" y2="15"/></svg>
        上传文档
      </el-button>
    </div>

    <!-- 文档表格 -->
    <el-table
      :data="sources"
      v-loading="loading"
      stripe
      style="width:100%"
      :header-cell-style="{ background: '#f7f8fa', color: '#1d2129', fontWeight: 600 }"
      row-key="id"
    >
      <!-- 文档标题 -->
      <el-table-column prop="title" label="文档标题" min-width="200" show-overflow-tooltip />

      <!-- 分类 -->
      <el-table-column label="分类" width="110">
        <template #default="{ row }">
          <el-tag v-if="row.category" size="small" effect="plain" style="border-radius:3px">
            {{ row.category }}
          </el-tag>
          <span v-else class="no-data">-</span>
        </template>
      </el-table-column>

      <!-- 状态（替换原来的解析/索引） -->
      <el-table-column label="状态" width="85" align="center">
        <template #default="{ row }">
          <el-tag :type="readinessStatus(row).type" size="small" effect="light" style="border-radius:10px;padding:0 10px;font-size:12px">
            {{ readinessStatus(row).text }}
          </el-tag>
        </template>
      </el-table-column>

      <!-- 可见范围 + 角色等级 -->
      <el-table-column label="可见范围" min-width="160">
        <template #default="{ row }">
          <div class="scope-cell">
            <!-- 全公司 -->
            <template v-if="row.accessScope === 'ALL'">
              <el-tag type="success" size="small" effect="plain" style="border-radius:3px">全公司</el-tag>
            </template>
            <!-- 指定部门：显示部门名 -->
            <template v-else>
              <el-tag type="warning" size="small" effect="plain" style="border-radius:3px;flex-shrink:0">部门</el-tag>
              <span class="dept-names" v-if="getDeptNames(row.accessDepartmentId).length">
                <template v-for="(name, vi) in getDeptNames(row.accessDepartmentId)" :key="vi">
                  <template v-if="vi < 2">
                    <span class="dept-name">{{ name }}</span>
                    <span v-if="vi === 0 && getDeptNames(row.accessDepartmentId).length > 1" class="dept-sep">、</span>
                  </template>
                  <template v-else-if="vi === 2">
                    <el-tooltip placement="top" :content="getDeptNames(row.accessDepartmentId).join('、')">
                      <span class="dept-more">...+{{ getDeptNames(row.accessDepartmentId).length - 1 }}</span>
                    </el-tooltip>
                  </template>
                </template>
              </span>
              <span v-else class="no-data">指定部门</span>
            </template>
            <!-- 角色等级标签 -->
            <el-tag type="info" size="small" effect="plain" style="border-radius:3px;margin-left:4px;font-size:11px">
              {{ roleLabel(row.minRoleLevel) }}
            </el-tag>
          </div>
        </template>
      </el-table-column>

      <!-- 上传时间 -->
      <el-table-column label="上传时间" width="165">
        <template #default="{ row }">
          <span class="time-text">{{ row.createTime }}</span>
        </template>
      </el-table-column>

      <!-- 操作 -->
      <el-table-column label="操作" width="235" fixed="right">
        <template #default="{ row }">
          <div class="action-btns">
            <el-button
              size="small"
              :type="row.status === 'ENABLED' ? 'default' : 'success'"
              plain
              @click="toggleStatus(row)"
            >
              {{ row.status === 'ENABLED' ? '停用' : '启用' }}
            </el-button>
            <el-button size="small" plain @click="openAccessDialog(row)">权限</el-button>
            <el-tooltip content="重新解析文档并更新向量库，用于异常恢复" placement="top">
              <el-button size="small" plain type="warning" @click="handleReindex(row)">重索引</el-button>
            </el-tooltip>
          </div>
        </template>
      </el-table-column>

      <!-- 空状态 -->
      <template #empty>
        <div class="empty-state">
          <div style="font-size:40px;margin-bottom:8px">📄</div>
          <p style="color:#86909c;font-size:14px">暂无知识文档</p>
          <p style="color:#c0c4cc;font-size:12px">点击右上角"上传文档"添加企业知识</p>
        </div>
      </template>
    </el-table>

    <!-- 上传文档对话框 -->
    <el-dialog v-model="uploadDialog" title="上传知识文档" width="500px" :close-on-click-modal="false">
      <el-form :model="uploadForm" label-width="90px">
        <el-form-item label="文件">
          <el-upload
            :auto-upload="false"
            :limit="1"
            accept=".doc,.docx,.pdf"
            :on-change="(f) => uploadForm.file = f.raw"
            :file-list="[]"
          >
            <el-button size="small">选择文件</el-button>
            <template #tip><span style="font-size:12px;color:#999;margin-left:8px">支持 doc / docx / pdf</span></template>
          </el-upload>
        </el-form-item>
        <el-form-item label="文档标题">
          <el-input v-model="uploadForm.title" placeholder="留空则使用文件名" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="uploadForm.category" placeholder="自动识别" clearable style="width:100%">
            <el-option v-for="c in categories" :key="c" :label="c" :value="c" />
          </el-select>
        </el-form-item>
        <el-form-item label="可见范围">
          <el-radio-group v-model="uploadForm.accessScope">
            <el-radio value="ALL">全公司</el-radio>
            <el-radio value="DEPARTMENT">指定部门</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="uploadForm.accessScope === 'DEPARTMENT'" label="选择部门">
          <el-select v-model="uploadForm.accessDepartmentIds" multiple collapse-tags collapse-tags-tooltip style="width:100%">
            <el-option v-for="d in departments" :key="d.id" :label="d.name" :value="d.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="最低角色">
          <el-select v-model="uploadForm.minRoleLevel" style="width:100%">
            <el-option v-for="r in roleLevels" :key="r.value" :label="r.label" :value="r.value" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="uploadDialog = false">取消</el-button>
        <el-button type="primary" :loading="uploadLoading" @click="handleUpload">上传并索引</el-button>
      </template>
    </el-dialog>

    <!-- 权限修改对话框 -->
    <el-dialog v-model="accessDialog" title="修改文档权限" width="450px" :close-on-click-modal="false">
      <el-form :model="accessForm" label-width="90px">
        <el-form-item label="可见范围">
          <el-radio-group v-model="accessForm.accessScope">
            <el-radio value="ALL">全公司</el-radio>
            <el-radio value="DEPARTMENT">指定部门</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="accessForm.accessScope === 'DEPARTMENT'" label="选择部门">
          <el-select v-model="accessForm.accessDepartmentIds" multiple collapse-tags collapse-tags-tooltip style="width:100%" placeholder="请选择部门">
            <el-option v-for="d in departments" :key="d.id" :label="d.name" :value="d.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="最低角色">
          <el-select v-model="accessForm.minRoleLevel" style="width:100%">
            <el-option v-for="r in roleLevels" :key="r.value" :label="`${r.label} — ${r.desc}`" :value="r.value" />
          </el-select>
          <div style="font-size:12px;color:#999;margin-top:4px">
            {{ roleLevels.find(r => r.value === accessForm.minRoleLevel)?.desc }}
          </div>
        </el-form-item>
        <div style="background:#f7f8fa;border-radius:6px;padding:10px 12px;font-size:12px;color:#86909c;margin-top:4px">
          <p style="margin:0 0 4px 0">💡 修改权限后，旧权限对应的向量缓存将被清除，新权限即时生效。</p>
          <p style="margin:0">角色说明：全员可见(普通员工) → 经理级以上(部门经理) → 仅管理员(总管理员)</p>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="accessDialog = false">取消</el-button>
        <el-button type="primary" @click="saveAccess">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.source-page {
  padding: 24px 32px;
  max-width: 1200px;
  margin: 0 auto;
}

/* 页面头部 */
.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 24px;
  flex-wrap: wrap;
  gap: 12px;
}
.header-left h2 {
  margin: 0;
  font-size: 22px;
  color: #1d2129;
  font-weight: 600;
}
.subtitle {
  font-size: 13px;
  color: #86909c;
  margin: 4px 0 0 0;
}

/* 可见范围单元格 */
.scope-cell {
  display: flex;
  align-items: center;
  flex-wrap: nowrap;
  gap: 2px;
}
.dept-names {
  display: inline-flex;
  align-items: center;
  flex-wrap: nowrap;
  margin-left: 2px;
  font-size: 12px;
  color: #606266;
}
.dept-name {
  white-space: nowrap;
}
.dept-sep {
  color: #c0c4cc;
}
.dept-more {
  color: #409eff;
  cursor: pointer;
  white-space: nowrap;
}

/* 上传时间 */
.time-text {
  font-size: 13px;
  color: #666;
}

/* 操作按钮组 */
.action-btns {
  display: flex;
  gap: 6px;
  flex-wrap: nowrap;
}
.action-btns .el-button {
  padding: 5px 10px;
  font-size: 12px;
}

/* 空状态 */
.empty-state {
  text-align: center;
  padding: 60px 0;
}

/* 无数据占位 */
.no-data {
  color: #c0c4cc;
  font-size: 13px;
}

/* 全局圆角 */
.el-table {
  border-radius: 8px;
  overflow: hidden;
}
</style>
