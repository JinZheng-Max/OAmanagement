<template>
  <div class="ai-container">
    <div class="page-header">
      <h2 class="page-title">🤖 AI 智能助手</h2>
      <p class="page-subtitle">智能问答、全文检索，帮您快速找到答案</p>
    </div>

    <div class="tab-switcher">
      <button 
        class="tab-btn" 
        :class="{ active: activeTab === 'qa' }"
        @click="activeTab = 'qa'"
      >
        <div class="tab-glow"></div>
        <el-icon class="tab-icon"><ChatDotRound /></el-icon>
        <span class="tab-label">AI 问答</span>
        <div class="tab-indicator"></div>
      </button>
      <button 
        v-if="isAdmin || isDeptAdmin"
        class="tab-btn" 
        :class="{ active: activeTab === 'knowledge' }"
        @click="activeTab = 'knowledge'"
      >
        <div class="tab-glow"></div>
        <el-icon class="tab-icon"><Document /></el-icon>
        <span class="tab-label">知识库管理</span>
        <div class="tab-indicator"></div>
      </button>
    </div>

    <transition name="tab-fade" mode="out-in">
      <div v-if="activeTab === 'qa'" key="qa" class="tab-content">
        <div class="chat-container">
          <div class="chat-history">
            <div v-for="(msg, i) in chatHistory" :key="i" :class="['chat-item', msg.role]">
              <div class="chat-avatar">
                <el-icon v-if="msg.role === 'assistant'"><ChatDotRound /></el-icon>
                <el-icon v-else><User /></el-icon>
              </div>
              <div class="chat-bubble">
                <p>{{ msg.text }}</p>
                <div v-if="msg.sources && msg.sources.length" class="chat-sources">
                  <p class="sources-title">💡 匹配制度依据：</p>
                  <div class="sources-list">
                    <el-tag v-for="s in msg.sources" :key="s.id" size="small" type="info">
                      《{{ s.summary }}》
                    </el-tag>
                  </div>
                </div>
              </div>
            </div>
            <div v-if="isThinking" class="chat-item assistant thinking">
              <div class="chat-avatar thinking-avatar">
                <el-icon><ChatDotRound /></el-icon>
              </div>
              <div class="chat-bubble thinking-bubble">
                <div class="thinking-content">
                  <span class="thinking-text">AI正在思考</span>
                  <span class="thinking-dots">
                    <span class="dot"></span>
                    <span class="dot"></span>
                    <span class="dot"></span>
                  </span>
                </div>
              </div>
            </div>
          </div>
          <div class="chat-input">
            <el-input v-model="question" placeholder="问一问AI：年假怎么休？请假找谁审批？" @keyup.enter="askAi" :disabled="isThinking" size="large">
              <template #append>
                <el-button type="primary" @click="askAi" :disabled="isThinking">
                  <el-icon v-if="!isThinking"><ArrowRight /></el-icon>
                  <el-icon v-else class="loading-icon"><Loading /></el-icon>
                  {{ isThinking ? '思考中' : '发送' }}
                </el-button>
              </template>
            </el-input>
          </div>
        </div>
      </div>

      <div v-else-if="activeTab === 'knowledge'" key="knowledge" class="tab-content">
        <div class="knowledge-container">
          <div class="knowledge-header">
            <div class="knowledge-filters">
              <el-input v-model="knowledgeKeyword" placeholder="搜索文档标题..." size="large" @keyup.enter="loadSources" style="width: 300px; margin-right: 12px;">
                <template #append>
                  <el-button type="primary" @click="loadSources">
                    <el-icon><Search /></el-icon>
                  </el-button>
                </template>
              </el-input>
              <el-select v-model="filterCategory" placeholder="选择分类" style="width: 160px; margin-right: 12px;" @change="loadSources">
                <el-option label="全部分类" value="" />
                <el-option v-for="cat in sourceCategories" :key="cat.value" :label="cat.label" :value="cat.value" />
              </el-select>
              <el-select v-model="filterStatus" placeholder="选择状态" style="width: 140px;" @change="loadSources">
                <el-option label="全部状态" value="" />
                <el-option label="启用" value="ENABLED" />
                <el-option label="停用" value="DISABLED" />
              </el-select>
            </div>
            <div class="knowledge-actions">
              <el-button type="primary" size="large" @click="uploadModal = true">
                <el-icon><Plus /></el-icon>
                上传文档
              </el-button>
            </div>
          </div>

          <div class="source-list" v-loading="sourceLoading">
            <div v-for="item in sourceList" :key="item.id" class="source-card">
              <div class="source-header">
                <div class="source-title-row">
                  <div class="source-icon">
                    <el-icon><Document /></el-icon>
                  </div>
                  <div>
                    <h3 class="source-title">{{ item.title || item.fileName }}</h3>
                    <div class="source-meta">
                      <span>{{ item.category || '未分类' }}</span>
                      <span>·</span>
                      <span>{{ item.uploaderName || '未知' }}</span>
                      <span>·</span>
                      <span>{{ formatTime(item.createTime) }}</span>
                    </div>
                  </div>
                </div>
                <div class="source-status" :class="item.status === 'ENABLED' ? 'enabled' : 'disabled'">
                  {{ item.status === 'ENABLED' ? '已启用' : '已停用' }}
                </div>
              </div>
              
              <p class="source-desc" v-if="item.description">{{ item.description }}</p>
              
              <div class="source-permissions">
                <div class="perm-item">
                  <span class="perm-label">可见范围</span>
                  <span class="perm-value">{{ getAccessScopeLabel(item.accessScope) }}</span>
                </div>
                <div class="perm-item">
                  <span class="perm-label">角色等级</span>
                  <span class="perm-value">{{ getRoleLevelLabel(item.minRoleLevel) }}</span>
                </div>
              </div>

              <div class="source-footer">
                <div class="source-stats">
                  <span>📁 文件: {{ item.fileName }}</span>
                  <span v-if="item.fileSize">📏 {{ formatFileSize(item.fileSize) }}</span>
                </div>
                <div class="source-actions">
                  <el-button v-if="item.status === 'ENABLED'" type="warning" size="small" @click="updateStatus(item.id, 'DISABLED')">
                    <el-icon><CircleClose /></el-icon>
                    停用
                  </el-button>
                  <el-button v-if="item.status === 'DISABLED'" type="success" size="small" @click="updateStatus(item.id, 'ENABLED')">
                    <el-icon><CircleCheck /></el-icon>
                    启用
                  </el-button>
                  <el-button type="primary" size="small" @click="openEditAccessModal(item)">
                    <el-icon><EditPen /></el-icon>
                    修改权限
                  </el-button>
                  <el-button type="info" size="small" @click="reindexSourceApi(item.id)">
                    <el-icon><RefreshLeft /></el-icon>
                    重新索引
                  </el-button>
                </div>
              </div>
            </div>
            <el-empty v-if="!sourceLoading && sourceList.length === 0" description="暂无知识库文档" />
          </div>
        </div>
      </div>
    </transition>

    <el-dialog v-model="uploadModal" title="上传知识文档" width="600px" append-to-body>
      <el-form :model="uploadForm" label-width="100px">
        <el-form-item label="文档文件 <span style='color:red'>*</span>" required>
          <el-upload
            class="upload-demo"
            action="#"
            :auto-upload="false"
            :show-file-list="true"
            :on-change="handleFileChange"
            :on-remove="handleFileRemove"
            accept=".doc,.docx,.pdf"
            :file-list="uploadFileList"
          >
            <el-button type="primary" size="small">选择文件</el-button>
            <template #tip>
              <div class="el-upload__tip">支持 .doc, .docx, .pdf 格式，大小不超过 50MB</div>
            </template>
          </el-upload>
        </el-form-item>
        <el-form-item label="文档标题 <span style='color:red'>*</span>" required>
          <el-input v-model="uploadForm.title" placeholder="请输入文档标题" />
        </el-form-item>
        <el-form-item label="所属分类 <span style='color:red'>*</span>" required>
          <el-select v-model="uploadForm.category" placeholder="请选择分类" style="width: 100%">
            <el-option v-for="cat in sourceCategories" :key="cat.value" :label="cat.label" :value="cat.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="文档简介 <span style='color:red'>*</span>" required>
          <el-input v-model="uploadForm.description" type="textarea" :rows="3" placeholder="请描述文档内容..." />
        </el-form-item>
        <el-form-item label="可见范围 <span style='color:red'>*</span>" required v-if="isAdmin">
          <el-select v-model="uploadForm.accessScope" placeholder="请选择可见范围" style="width: 100%">
            <el-option label="全部可见" value="ALL" />
            <el-option v-for="dept in departments" :key="dept.id" :label="dept.name" :value="String(dept.id)" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="isDeptAdmin">
          <div class="dept-admin-hint">
            <span class="hint-label">可见范围：</span>
            <span class="hint-value">{{ myDepartmentName }}</span>
          </div>
        </el-form-item>
        <el-form-item label="最低角色等级 <span style='color:red'>*</span>" required>
          <el-select v-model="uploadForm.minRoleLevel" placeholder="请选择" style="width: 100%">
            <el-option v-for="level in roleLevels" :key="level.value" :label="level.label" :value="level.value" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="uploadModal = false">取消</el-button>
        <el-button type="primary" @click="submitUpload" :disabled="!uploadFileList.length">上传文档</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="editAccessModal" title="修改文档权限" width="500px" append-to-body>
      <el-form :model="editAccessForm" label-width="100px">
        <el-form-item label="可见范围 <span style='color:red'>*</span>" required>
          <el-select v-model="editAccessForm.accessScope" placeholder="请选择可见范围" style="width: 100%">
            <el-option label="全部可见" value="ALL" />
            <el-option v-for="dept in departments" :key="dept.id" :label="dept.name" :value="String(dept.id)" />
          </el-select>
        </el-form-item>
        <el-form-item label="最低角色等级 <span style='color:red'>*</span>" required>
          <el-select v-model="editAccessForm.minRoleLevel" placeholder="请选择" style="width: 100%">
            <el-option v-for="level in roleLevels" :key="level.value" :label="level.label" :value="level.value" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="editAccessForm.accessScope === 'DEPARTMENT'" label="指定部门">
          <el-select v-model="editAccessForm.accessDepartmentIds" placeholder="请选择部门" multiple style="width: 100%">
            <el-option v-for="dept in departments" :key="dept.id" :label="dept.name" :value="dept.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editAccessModal = false">取消</el-button>
        <el-button type="primary" @click="submitEditAccess">保存修改</el-button>
      </template>
    </el-dialog>

    </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  ChatDotRound, User, ArrowRight, Search, Loading, Document, Plus, 
  Upload, CircleCheck, CircleClose, EditPen, RefreshLeft 
} from '@element-plus/icons-vue'
import { chat, uploadSource, getSources, updateSourceAccess, updateSourceStatus, reindexSource, batchImport, sourceCategories, accessScopes, roleLevels } from '../api/ai'
import { listActiveDepartments } from '../api/org'

const activeTab = ref('qa')
const question = ref('')
const chatHistory = ref([{ role: 'assistant', text: '您好！我是智办 AI 智能助手，输入您的疑问或关键词，我将从企业知识库与制度公告中为您即时解答。' }])
const isThinking = ref(false)

const storedRole = ref(localStorage.getItem('role') || 'EMPLOYEE')
const isAdmin = computed(() => storedRole.value === 'SUPER_ADMIN' || storedRole.value === 'ADMIN')
const isDeptAdmin = computed(() => storedRole.value === 'DEPT_MANAGER')

const myDepartmentId = ref(localStorage.getItem('departmentId') || '')
const myDepartmentName = ref(localStorage.getItem('departmentName') || '')

const knowledgeKeyword = ref('')
const filterCategory = ref('')
const filterStatus = ref('')
const sourceList = ref([])
const sourceLoading = ref(false)

const uploadModal = ref(false)
const uploadForm = ref({
  title: '',
  category: '',
  description: '',
  accessScope: '',
  minRoleLevel: 1
})
const uploadFileList = ref([])

const editAccessModal = ref(false)
const editAccessForm = ref({
  id: null,
  accessScope: 'ALL',
  minRoleLevel: 1,
  accessDepartmentIds: []
})
const departments = ref([])

const askAi = async () => {
  if (!question.value.trim() || isThinking.value) return
  const q = question.value.trim()
  chatHistory.value.push({ role: 'user', text: q })
  question.value = ''
  isThinking.value = true

  try {
    const res = await chat(q)
    const data = res.data || res
    chatHistory.value.push({ role: 'assistant', text: data.answer || '暂无相关信息', sources: data.sources || [] })
  } catch (err) {
    chatHistory.value.push({
      role: 'assistant',
      text: '智能助手正在进行算法维护，请稍后再试。'
    })
  } finally {
    isThinking.value = false
  }
}

const loadSources = async () => {
  sourceLoading.value = true
  try {
    const params = {}
    if (knowledgeKeyword.value) params.title = knowledgeKeyword.value
    if (filterCategory.value) params.category = filterCategory.value
    if (filterStatus.value) params.status = filterStatus.value
    const res = await getSources(params)
    sourceList.value = res.records || res.data || []
  } catch (err) {
    ElMessage.error('加载失败')
  } finally {
    sourceLoading.value = false
  }
}

const handleFileChange = (file) => {
  uploadFileList.value = [file]
}

const handleFileRemove = () => {
  uploadFileList.value = []
}

const submitUpload = async () => {
  if (!uploadFileList.value.length) {
    ElMessage.warning('请选择文档文件')
    return
  }
  if (isAdmin.value && !uploadForm.value.accessScope) {
    ElMessage.warning('请选择可见范围')
    return
  }
  const file = uploadFileList.value[0].raw
  const formData = new FormData()
  formData.append('file', file)
  if (uploadForm.value.title) formData.append('title', uploadForm.value.title)
  if (uploadForm.value.category) formData.append('category', uploadForm.value.category)
  if (uploadForm.value.description) formData.append('description', uploadForm.value.description)
  
  let accessScope, accessDepartmentIds
  if (isDeptAdmin.value) {
    accessScope = 'DEPARTMENT'
    accessDepartmentIds = myDepartmentId.value
  } else {
    accessScope = uploadForm.value.accessScope === 'ALL' ? 'ALL' : 'DEPARTMENT'
    accessDepartmentIds = uploadForm.value.accessScope !== 'ALL' ? uploadForm.value.accessScope : ''
  }
  formData.append('accessScope', accessScope)
  if (accessDepartmentIds) {
    formData.append('accessDepartmentIds', accessDepartmentIds)
  }
  formData.append('minRoleLevel', uploadForm.value.minRoleLevel)
  
  try {
    await uploadSource(formData)
    ElMessage.success('上传成功')
    uploadModal.value = false
    uploadForm.value = { title: '', category: '', description: '', accessScope: '', minRoleLevel: 1 }
    uploadFileList.value = []
    loadSources()
  } catch (err) {
    ElMessage.error('上传失败')
  }
}

const updateStatus = async (id, status) => {
  try {
    await updateSourceStatus(id, status)
    ElMessage.success(status === 'ENABLED' ? '已启用' : '已停用')
    loadSources()
  } catch (err) {
    ElMessage.error('操作失败')
  }
}

const openEditAccessModal = (item) => {
  editAccessForm.value = {
    id: item.id,
    accessScope: item.accessScope || 'ALL',
    minRoleLevel: item.minRoleLevel || 1,
    accessDepartmentIds: item.accessDepartmentIds ? item.accessDepartmentIds.split(',').map(Number) : []
  }
  editAccessModal.value = true
}

const submitEditAccess = async () => {
  if (!editAccessForm.value.accessScope) {
    ElMessage.warning('请选择可见范围')
    return
  }
  try {
    const accessScope = editAccessForm.value.accessScope === 'ALL' ? 'ALL' : 'DEPARTMENT'
    const data = {
      accessScope: accessScope,
      minRoleLevel: editAccessForm.value.minRoleLevel
    }
    if (editAccessForm.value.accessScope !== 'ALL') {
      data.accessDepartmentIds = editAccessForm.value.accessScope
    }
    await updateSourceAccess(editAccessForm.value.id, data)
    ElMessage.success('权限修改成功')
    editAccessModal.value = false
    loadSources()
  } catch (err) {
    ElMessage.error('操作失败')
  }
}

const reindexSourceApi = async (id) => {
  try {
    await reindexSource(id)
    ElMessage.success('重新索引成功')
    loadSources()
  } catch (err) {
    ElMessage.error('操作失败')
  }
}

const loadDepartments = async () => {
  try {
    const res = await listActiveDepartments()
    departments.value = res.data || res.records || []
  } catch (err) {
    console.error('加载部门列表失败', err)
  }
}

const getAccessScopeLabel = (scope) => {
  return accessScopes.find(s => s.value === scope)?.label || '全公司'
}

const getRoleLevelLabel = (level) => {
  return roleLevels.find(l => l.value === level)?.label || '全员可见'
}

const formatTime = (time) => {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN')
}

const formatFileSize = (size) => {
  if (!size) return ''
  if (size < 1024) return size + ' B'
  if (size < 1024 * 1024) return (size / 1024).toFixed(1) + ' KB'
  return (size / (1024 * 1024)).toFixed(1) + ' MB'
}

onMounted(() => {
  if ((isAdmin.value || isDeptAdmin.value) && activeTab.value === 'knowledge') {
    loadSources()
    loadDepartments()
  }
})
</script>

<style scoped>
.ai-container {
  max-width: 1400px;
}

.page-header {
  margin-bottom: 32px;
}

.page-title {
  font-size: 30px;
  font-weight: 800;
  color: #1a1a2e;
  margin: 0 0 10px 0;
  letter-spacing: -0.5px;
}

.page-subtitle {
  font-size: 15px;
  color: #64748b;
  margin: 0;
  font-weight: 500;
}

.tab-switcher {
  display: flex;
  gap: 16px;
  margin-bottom: 32px;
}

.tab-btn {
  flex: 1;
  position: relative;
  height: 72px;
  padding: 0 32px;
  background: rgba(255, 255, 255, 0.6);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.5);
  border-radius: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  cursor: pointer;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden;
}

.tab-btn:hover {
  background: rgba(255, 255, 255, 0.8);
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
  border-color: rgba(102, 126, 234, 0.2);
}

.tab-btn.active {
  background: rgba(255, 255, 255, 0.95);
  box-shadow: 0 8px 30px rgba(102, 126, 234, 0.15);
  border-color: rgba(102, 126, 234, 0.3);
}

.tab-glow {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.05) 0%, rgba(118, 75, 162, 0.03) 100%);
  opacity: 0;
  transition: opacity 0.4s ease;
}

.tab-btn.active .tab-glow {
  opacity: 1;
}

.tab-icon {
  font-size: 24px;
  color: #94a3b8;
  transition: all 0.4s ease;
}

.tab-btn.active .tab-icon {
  color: #667eea;
  transform: scale(1.1);
}

.tab-label {
  font-size: 18px;
  font-weight: 700;
  color: #64748b;
  transition: all 0.4s ease;
}

.tab-btn.active .tab-label {
  color: #1a1a2e;
}

.tab-indicator {
  position: absolute;
  bottom: 0;
  left: 15%;
  right: 15%;
  height: 4px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 2px;
  opacity: 0;
  transform: scaleX(0);
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}

.tab-btn.active .tab-indicator {
  opacity: 1;
  transform: scaleX(1);
}

.tab-content {
  min-height: 560px;
}

.tab-fade-enter-active,
.tab-fade-leave-active {
  transition: all 0.3s ease;
}

.tab-fade-enter-from {
  opacity: 0;
  transform: translateY(10px);
}

.tab-fade-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}

.chat-container {
  height: 560px;
  display: flex;
  flex-direction: column;
  background: rgba(255, 255, 255, 0.6);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-radius: 24px;
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.5);
  box-shadow: 0 4px 30px rgba(0, 0, 0, 0.05);
}

.chat-history {
  flex: 1;
  overflow-y: auto;
  padding: 32px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.chat-item {
  display: flex;
  gap: 14px;
  animation: fadeInUp 0.4s ease-out;
}

@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

.chat-item.user {
  justify-content: flex-end;
}

.chat-item.user .chat-bubble {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border-radius: 20px 6px 20px 20px;
  box-shadow: 0 4px 16px rgba(102, 126, 234, 0.3);
}

.chat-avatar {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: all 0.3s ease;
}

.chat-item.assistant .chat-avatar {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

.chat-item.user .chat-avatar {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
  box-shadow: 0 4px 12px rgba(240, 147, 251, 0.3);
}

.chat-avatar :deep(.el-icon) {
  font-size: 22px;
  color: white;
}

.thinking-avatar {
  animation: avatar-pulse 2s ease-in-out infinite;
}

@keyframes avatar-pulse {
  0%, 100% { transform: scale(1); box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3); }
  50% { transform: scale(1.05); box-shadow: 0 6px 16px rgba(102, 126, 234, 0.4); }
}

.thinking-bubble {
  background: rgba(255, 255, 255, 0.6) !important;
}

.thinking-content {
  display: flex;
  align-items: center;
  gap: 8px;
}

.thinking-text {
  font-size: 15px;
  color: #64748b;
}

.thinking-dots {
  display: flex;
  gap: 4px;
}

.thinking-dots .dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #667eea;
  animation: dot-bounce 1.4s ease-in-out infinite;
}

.thinking-dots .dot:nth-child(1) { animation-delay: 0s; }
.thinking-dots .dot:nth-child(2) { animation-delay: 0.2s; }
.thinking-dots .dot:nth-child(3) { animation-delay: 0.4s; }

@keyframes dot-bounce {
  0%, 80%, 100% { transform: scale(0.6); opacity: 0.5; }
  40% { transform: scale(1); opacity: 1; }
}

.loading-icon {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.chat-bubble {
  max-width: 70%;
  padding: 18px 24px;
  background: rgba(255, 255, 255, 0.7);
  border-radius: 6px 20px 20px 20px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.06);
  transition: all 0.3s ease;
}

.chat-bubble:hover {
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.08);
}

.chat-bubble p {
  margin: 0;
  font-size: 15px;
  line-height: 1.7;
}

.chat-item.user .chat-bubble p {
  color: white;
}

.chat-sources {
  margin-top: 14px;
  padding-top: 14px;
  border-top: 1px solid rgba(102, 126, 234, 0.1);
}

.sources-title {
  font-size: 12px;
  color: #e6a23c;
  margin: 0 0 10px 0;
  font-weight: 600;
}

.sources-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.chat-input {
  padding: 24px;
  border-top: 1px solid rgba(0, 0, 0, 0.06);
  background: rgba(255, 255, 255, 0.8);
}

.chat-input :deep(.el-input__wrapper) {
  border-radius: 16px;
  border: 1.5px solid rgba(0, 0, 0, 0.08);
  padding: 14px 18px;
}

.chat-input :deep(.el-input__wrapper:hover) {
  border-color: rgba(102, 126, 234, 0.3);
}

.chat-input :deep(.el-input__wrapper.is-focus) {
  border-color: #667eea;
  box-shadow: 0 0 0 4px rgba(102, 126, 234, 0.1);
}

.chat-input :deep(.el-button--primary) {
  border-radius: 12px;
  padding: 12px 28px;
  font-weight: 600;
}

.knowledge-container {
  background: rgba(255, 255, 255, 0.6);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-radius: 24px;
  padding: 32px;
  border: 1px solid rgba(255, 255, 255, 0.5);
  box-shadow: 0 4px 30px rgba(0, 0, 0, 0.05);
}

.knowledge-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 28px;
  flex-wrap: wrap;
  gap: 16px;
}

.knowledge-filters {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.knowledge-actions {
  display: flex;
  gap: 12px;
}

.source-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(480px, 1fr));
  gap: 20px;
}

.source-card {
  background: rgba(255, 255, 255, 0.8);
  border-radius: 16px;
  padding: 24px;
  border: 1px solid rgba(255, 255, 255, 0.6);
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  transition: all 0.3s ease;
}

.source-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
  border-color: rgba(102, 126, 234, 0.2);
}

.source-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 16px;
}

.source-title-row {
  display: flex;
  gap: 14px;
  flex: 1;
}

.source-icon {
  width: 44px;
  height: 44px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.source-icon :deep(.el-icon) {
  font-size: 22px;
  color: white;
}

.source-title {
  font-size: 16px;
  font-weight: 700;
  color: #1a1a2e;
  margin: 0 0 6px 0;
}

.source-meta {
  font-size: 13px;
  color: #94a3b8;
}

.source-status {
  padding: 6px 14px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
}

.source-status.enabled {
  background: rgba(16, 185, 129, 0.1);
  color: #10b981;
}

.source-status.disabled {
  background: rgba(148, 163, 184, 0.1);
  color: #94a3b8;
}

.source-desc {
  font-size: 14px;
  color: #64748b;
  line-height: 1.6;
  margin: 0 0 16px 0;
}

.source-permissions {
  display: flex;
  gap: 20px;
  margin-bottom: 16px;
  padding: 12px 16px;
  background: #f8fafc;
  border-radius: 10px;
}

.perm-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.perm-label {
  font-size: 12px;
  color: #94a3b8;
}

.perm-value {
  font-size: 13px;
  color: #334155;
  font-weight: 600;
}

.source-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 16px;
  border-top: 1px solid #f1f5f9;
}

.source-stats {
  display: flex;
  gap: 12px;
  font-size: 12px;
  color: #94a3b8;
}

.source-actions {
  display: flex;
  gap: 8px;
}

.dept-admin-hint {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  background: rgba(102, 126, 234, 0.08);
  border-radius: 10px;
}

.hint-label {
  font-size: 14px;
  color: #64748b;
}

.hint-value {
  font-size: 14px;
  font-weight: 600;
  color: #667eea;
}
</style>
