<template>
  <div class="content-container">
    <!-- 头部页眉与管理员全量重建按钮 -->
    <div class="page-header">
      <div class="header-top">
        <div>
          <h1 class="page-title">📄 {{ isAdmin ? '公告制度管理中心' : '公告制度中心' }}</h1>
          <p class="page-subtitle">{{ isAdmin ? '发布、下架、重构 ES 索引与维护公司公告制度规范' : '浏览全公司及本部门最新发布的通知公告与规章制度' }}</p>
        </div>
        <div style="display: flex; gap: 12px;">
          <el-button v-if="isAdmin" type="warning" plain size="large" :loading="reindexLoading" @click="handleReindexEs">
            <el-icon><RefreshRight /></el-icon>
            重构 ES 索引
          </el-button>
          <el-button v-if="canPublish" type="primary" size="large" @click="openCreateModal()">
            <el-icon><Plus /></el-icon>
            写草稿 / 发布内容
          </el-button>
        </div>
      </div>
    </div>

    <!-- ES 搜索引擎智能检索栏与分类 Chips -->
    <div class="search-box-card">
      <div class="search-form-row">
        <el-input
          v-model="searchKeyword"
          placeholder="智能检索公告标题或全文正文 (支持 ES 全文高亮匹配)..."
          clearable
          style="flex: 1; min-width: 260px;"
          size="large"
          @keyup.enter="handleSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>

        <el-select v-model="searchCategory" placeholder="所属分类" clearable size="large" style="width: 160px">
          <el-option label="全部分类" value="" />
          <el-option v-for="cat in categoryOptions" :key="cat" :label="cat" :value="cat" />
        </el-select>

        <el-select v-model="searchType" placeholder="类型" clearable size="large" style="width: 140px">
          <el-option label="全部类型" value="" />
          <el-option label="📢 公告" value="ANNOUNCEMENT" />
          <el-option label="📋 制度" value="POLICY" />
        </el-select>

        <el-button type="primary" size="large" :loading="searchLoading" @click="handleSearch">
          <el-icon><Search /></el-icon>
          全文检索
        </el-button>
        <el-button v-if="searchMode || searchKeyword || searchCategory || searchType" size="large" @click="clearSearch">
          <el-icon><RefreshRight /></el-icon>
          重置 / 返回列表
        </el-button>
      </div>

      <!-- 分类 Chip 快捷筛选 -->
      <div class="category-chips">
        <span class="chip-label">快捷分类：</span>
        <span 
          class="chip-item"
          :class="{ active: selectedCategory === 'ALL' }"
          @click="handleSelectCategory('ALL')"
        >全部</span>
        <span 
          v-for="cat in categoryOptions"
          :key="cat"
          class="chip-item"
          :class="{ active: selectedCategory === cat }"
          @click="handleSelectCategory(cat)"
        >{{ cat }}</span>
      </div>
    </div>

    <!-- 检索模式 vs 正常 Tab 列表模式 -->
    <div class="main-content">
      <div v-if="searchMode" class="search-result-section">
        <div class="search-summary-bar" style="display: flex; justify-content: space-between; align-items: center;">
          <span>🔍 找到 {{ searchResults.length }} 条符合条件的内容结果：</span>
          <el-button type="primary" link size="small" @click="clearSearch">
            <el-icon><RefreshRight /></el-icon>
            返回完整公告列表
          </el-button>
        </div>
        <div class="content-list">
          <div class="content-card" v-for="item in searchResults" :key="item.id">
            <div class="content-main">
              <div class="content-tags">
                <span class="content-tag" :class="item.type === 'ANNOUNCEMENT' ? 'announcement-tag' : 'policy-tag'">
                  {{ item.type === 'ANNOUNCEMENT' ? '公告' : '制度' }}
                </span>
                <span class="content-tag category-tag">{{ item.category || '综合' }}</span>
              </div>
              <h3 class="content-title" v-html="item.highlightTitle || item.title"></h3>
              <p class="content-preview" v-html="item.highlightBody || (item.body?.substring(0, 120) + '...')"></p>
              <div class="content-meta">
                <span class="content-time">📅 {{ item.createTime || item.publishTime || '-' }}</span>
                <span class="content-author">👤 {{ item.authorName || '系统' }}</span>
              </div>
            </div>
            <div class="content-actions">
              <el-button type="text" size="small" @click="viewDetail(item)">查看详情</el-button>
            </div>
          </div>
          <el-empty v-if="searchResults.length === 0" description="未找到符合条件的内容结果">
            <el-button type="primary" size="default" @click="clearSearch">
              <el-icon><RefreshRight /></el-icon>
              恢复显示全部公告与制度
            </el-button>
          </el-empty>
        </div>
      </div>

      <div v-else class="tabs-section">
        <div class="content-tab-switcher">
          <button 
            class="content-tab-btn" 
            :class="{ active: activeTab === 'ANNOUNCEMENT' }"
            @click="activeTab = 'ANNOUNCEMENT'"
          >
            <div class="tab-glow"></div>
            <span class="tab-emoji">📢</span>
            <span class="tab-text">公司公告</span>
            <div class="tab-indicator"></div>
          </button>
          <button 
            class="content-tab-btn" 
            :class="{ active: activeTab === 'POLICY' }"
            @click="activeTab = 'POLICY'"
          >
            <div class="tab-glow"></div>
            <span class="tab-emoji">📋</span>
            <span class="tab-text">规章制度</span>
            <div class="tab-indicator"></div>
          </button>
        </div>

        <transition name="content-fade" mode="out-in">
          <div class="content-list" :key="activeTab">
            <div class="content-card" v-for="item in currentTabList" :key="item.id">
              <div class="content-main">
                <div class="content-tags">
                  <span class="content-tag" :class="item.type === 'ANNOUNCEMENT' ? 'announcement-tag' : 'policy-tag'">
                    {{ item.type === 'ANNOUNCEMENT' ? '公告' : '制度' }}
                  </span>
                  <span class="content-tag category-tag" v-if="item.category">{{ item.category }}</span>
                  <span class="content-status" :class="item.status">{{ statusMap[item.status] }}</span>
                </div>
                <h3 class="content-title">{{ item.title }}</h3>
                <p class="content-preview">{{ item.body?.substring(0, 120) }}...</p>
                <div class="content-meta">
                  <span class="content-time">📅 {{ item.createdAt }}</span>
                  <span class="content-author">👤 {{ item.authorName || '系统' }}</span>
                  <span class="content-version">📌 v{{ item.version || 1 }}</span>
                  <span class="content-view">👁️ {{ item.viewCount || 0 }} 次浏览</span>
                </div>
              </div>
              <div class="content-actions">
                <el-button v-if="canPublish && item.status === 'DRAFT'" type="primary" size="small" @click="publish(item.id)">发布上架</el-button>
                <el-button v-if="canPublish && item.status === 'DRAFT'" size="small" @click="openCreateModal(item)">编辑草稿</el-button>
                <el-button v-if="canPublish && item.status === 'DRAFT'" type="danger" plain size="small" @click="deleteDraft(item.id)">删除</el-button>
                <el-button v-if="canUnpublish(item)" type="danger" size="small" @click="unpublish(item.id)">下架</el-button>
                <el-button type="text" size="small" @click="viewDetail(item)">查看详情</el-button>
              </div>
            </div>
            <el-empty v-if="currentTabList.length === 0" description="暂无相关公告或制度文件" />
          </div>
        </transition>
      </div>
    </div>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" title="公告制度详情" width="720px" :close-on-click-modal="false" append-to-body align-center>
      <div class="detail-content" v-if="selectedItem">
        <div class="detail-header">
          <div class="detail-icon">
            <el-icon v-if="selectedItem?.type === 'ANNOUNCEMENT'"><Bell /></el-icon>
            <el-icon v-else><Document /></el-icon>
          </div>
          <div class="detail-tags">
            <span class="detail-tag" :class="selectedItem?.type">{{ typeMap[selectedItem?.type] || '文件' }}</span>
            <span class="detail-status" :class="selectedItem?.status">{{ statusMap[selectedItem?.status] || '已发布' }}</span>
          </div>
        </div>
        <h2 class="detail-title" v-html="selectedItem?.highlightTitle || selectedItem?.title"></h2>
        <div class="detail-meta">
          <span>📅 发布日期：{{ selectedItem?.createdAt }}</span>
          <span>👤 发布人：{{ selectedItem?.authorName || '系统管理员' }}</span>
          <span>📌 发布范围：{{ getScopeText(selectedItem) }}</span>
        </div>
        <div class="detail-divider"></div>
        <div class="detail-body" v-html="selectedItem?.highlightBody || selectedItem?.body"></div>
        <div class="detail-actions" v-if="canUnpublish(selectedItem)" style="margin-top: 20px;">
          <el-button type="danger" size="small" @click="unpublish(selectedItem.id); detailVisible = false;">下架公告</el-button>
        </div>
      </div>
    </el-dialog>

    <!-- 草稿/编辑/发布 模态框 -->
    <el-dialog :title="createForm.id ? '编辑草稿/公告' : ('新建' + (createForm.type === 'ANNOUNCEMENT' ? '公告' : '制度'))" v-model="showCreateDialog" width="650px" append-to-body align-center :close-on-click-modal="false">
      <el-form :model="createForm" label-width="100px">
        <el-form-item label="文件类型">
          <el-radio-group v-model="createForm.type">
            <el-radio label="ANNOUNCEMENT">📢 通知公告</el-radio>
            <el-radio label="POLICY">📋 规章制度</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="核心标题" required>
          <el-input v-model="createForm.title" placeholder="请输入核心标题" />
        </el-form-item>
        <el-form-item label="所属分类">
          <el-select v-model="createForm.category" placeholder="请选择或输入" allow-create filterable style="width: 100%">
            <el-option v-for="cat in categoryOptions" :key="cat" :label="cat" :value="cat" />
          </el-select>
        </el-form-item>
        <el-form-item label="正文内容" required>
          <el-input v-model="createForm.body" type="textarea" :rows="8" placeholder="请输入详细正文内容..." />
        </el-form-item>
        <el-form-item label="可见范围" v-if="isAdmin">
          <el-select v-model="createForm.scope" placeholder="请选择" style="width: 100%">
            <el-option label="全公司可见 (ALL)" value="ALL" />
            <el-option label="指定部门可见 (DEPARTMENT)" value="DEPARTMENT" />
          </el-select>
        </el-form-item>
        <el-form-item label="目标部门" v-if="isAdmin && createForm.scope === 'DEPARTMENT'">
          <el-select v-model="createForm.accessDepartmentId" placeholder="请选择可见部门" style="width: 100%">
            <el-option v-for="dept in departmentList" :key="dept.id" :label="dept.name" :value="dept.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="发布范围" v-if="isDeptAdmin">
          <el-tag type="info">本部门可见 (自动绑定您所属部门)</el-tag>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="info" plain :loading="submitLoading" @click="saveDraftContent(false)">存为草稿</el-button>
        <el-button type="primary" :loading="submitLoading" @click="saveDraftContent(true)">直接发布上架</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Bell, Document, Plus, Search, RefreshRight } from '@element-plus/icons-vue'
import {
  saveDraftApi,
  deleteDraftApi,
  publishContentApi,
  unpublishContentApi,
  getAdminContentsApi,
  getEmployeeContentsApi,
  getContentDetailApi,
  searchContentsApi,
  reindexEsApi
} from '../api/content'
import { getDepartmentPage } from '../api/org'

const route = useRoute()
const list = ref([])
const activeTab = ref('ANNOUNCEMENT')
const selectedCategory = ref('ALL')

const searchKeyword = ref('')
const searchCategory = ref('')
const searchType = ref('')
const searchLoading = ref(false)
const searchMode = ref(false)
const searchResults = ref([])

const reindexLoading = ref(false)
const submitLoading = ref(false)
const detailVisible = ref(false)
const selectedItem = ref(null)
const showCreateDialog = ref(false)
const departmentList = ref([])

const categoryOptions = ['行政公告', '人事制度', '财务规范', '安全规范', '技术标准']

const storedRole = ref(localStorage.getItem('role') || 'EMPLOYEE')
const isAdmin = computed(() => storedRole.value === 'SUPER_ADMIN' || storedRole.value === 'ADMIN')
const isDeptAdmin = computed(() => storedRole.value === 'DEPT_MANAGER')
const userDepartmentId = ref(localStorage.getItem('departmentId') || null)
const canPublish = computed(() => isAdmin.value || isDeptAdmin.value)

const createForm = reactive({
  id: null,
  type: 'ANNOUNCEMENT',
  title: '',
  category: '行政公告',
  body: '',
  scope: 'ALL',
  accessDepartmentId: null
})

const canUnpublish = (item) => {
  if (!item || item.status !== 'PUBLISHED') return false
  return isAdmin.value || isDeptAdmin.value
}

const typeMap = {
  ANNOUNCEMENT: '公告',
  POLICY: '制度'
}

const statusMap = {
  DRAFT: '草稿',
  PUBLISHED: '已发布',
  UNPUBLISHED: '已下架'
}

const currentTabList = computed(() => {
  let filtered = list.value.filter(item => {
    if (activeTab.value === 'ANNOUNCEMENT') {
      return item.type === 'ANNOUNCEMENT' || !item.type
    } else {
      return item.type === 'POLICY'
    }
  })
  if (selectedCategory.value !== 'ALL') {
    filtered = filtered.filter(item => item.category === selectedCategory.value)
  }
  return filtered
})

const loadDepartments = async () => {
  try {
    const res = await getDepartmentPage({ page: 1, size: 100 })
    if (res && res.data) {
      departmentList.value = res.data.records || []
    }
  } catch (ignored) {}
}

const getScopeText = (item) => {
  if (!item) return '全公司'
  if (item.scope === 'ALL' || !item.scope) return '全公司'
  if (item.scope === 'DEPARTMENT') {
    if (item.accessDepartmentId) {
      const dept = departmentList.value.find(d => Number(d.id) === Number(item.accessDepartmentId))
      if (dept && dept.name) {
        return `指定部门 (${dept.name})`
      }
    }
    return '指定部门'
  }
  return item.scope
}

const loadContents = async () => {
  try {
    const params = { page: 1, size: 100, type: activeTab.value }
    let rawRecords = []
    
    if (isAdmin.value) {
      // 超级管理员：获取全量库中的所有内容（包括草稿、已发布、已下架）
      const res = await getAdminContentsApi(params)
      if (res && res.data) {
        rawRecords = res.data.records || res.data.list || []
      }
    } else if (isDeptAdmin.value) {
      // 部门经理：获取面向全体发出的已发布公司公告 + 部门经理自己创建的草稿
      const empRes = await getEmployeeContentsApi(params).catch(() => null)
      const adminRes = await getAdminContentsApi(params).catch(() => null)
      
      const empList = (empRes && empRes.data) ? (empRes.data.list || empRes.data.records || []) : []
      const adminList = (adminRes && adminRes.data) ? (adminRes.data.records || adminRes.data.list || []) : []
      
      const map = new Map()
      empList.forEach(item => map.set(item.id, item))
      adminList.forEach(item => map.set(item.id, item))
      rawRecords = Array.from(map.values())
    } else {
      // 普通员工：获取面向受众公开的已发布公告
      const res = await getEmployeeContentsApi(params)
      if (res && res.data) {
        rawRecords = res.data.list || res.data.records || []
      }
    }

    list.value = rawRecords.map(item => ({
      ...item,
      createdAt: item.createTime || item.publishTime || ''
    }))
  } catch (err) {
    console.error('加载内容失败', err)
  }
}

const handleSelectCategory = (cat) => {
  selectedCategory.value = cat
  if (searchMode.value) {
    searchMode.value = false
    searchResults.value = []
    searchKeyword.value = ''
    loadContents()
  }
}

const handleSearch = async () => {
  if (!searchKeyword.value.trim() && !searchCategory.value && !searchType.value) {
    return ElMessage.warning('请输入检索关键词或选择分类类型')
  }
  searchLoading.value = true
  try {
    const res = await searchContentsApi({
      keyword: searchKeyword.value.trim(),
      type: searchType.value || undefined,
      category: searchCategory.value || undefined,
      page: 1,
      size: 20
    })
    if (res && res.data) {
      searchResults.value = res.data.list || res.data.records || []
      searchMode.value = true
      ElMessage.success(`检索完成，获取到 ${searchResults.value.length} 条匹配结果`)
    }
  } catch (err) {
    ElMessage.error(err.response?.data?.message || '全文检索失败')
  } finally {
    searchLoading.value = false
  }
}

const clearSearch = () => {
  searchKeyword.value = ''
  searchCategory.value = ''
  searchType.value = ''
  searchMode.value = false
  searchResults.value = []
  loadContents()
}

const handleReindexEs = async () => {
  reindexLoading.value = true
  try {
    const res = await reindexEsApi()
    if (res && res.data) {
      const { costMillis, successCount } = res.data
      ElMessage.success(`ES 全文索引重建成功！同步数据: ${successCount || 0} 条，耗时: ${costMillis || 0} ms`)
    } else {
      ElMessage.success('ES 索引重建完成')
    }
  } catch (err) {
    ElMessage.error(err.response?.data?.message || '重构 ES 索引失败')
  } finally {
    reindexLoading.value = false
  }
}

const openCreateModal = (item = null) => {
  loadDepartments()
  if (item) {
    createForm.id = item.id
    createForm.type = item.type || activeTab.value
    createForm.title = item.title || ''
    createForm.category = item.category || '行政公告'
    createForm.body = item.body || ''
    createForm.scope = item.scope || 'ALL'
    createForm.accessDepartmentId = item.accessDepartmentId || null
  } else {
    createForm.id = null
    createForm.type = activeTab.value
    createForm.title = ''
    createForm.category = '行政公告'
    createForm.body = ''
    createForm.scope = isDeptAdmin.value ? 'DEPARTMENT' : 'ALL'
    createForm.accessDepartmentId = isDeptAdmin.value ? Number(userDepartmentId.value) : null
  }
  showCreateDialog.value = true
}

const saveDraftContent = async (publishNow = false) => {
  if (!createForm.title || !createForm.body) {
    return ElMessage.warning('请填写完整的核心标题与正文内容')
  }
  
  const payload = {
    id: createForm.id || undefined,
    type: createForm.type,
    title: createForm.title,
    category: createForm.category || '行政公告',
    body: createForm.body,
    scope: isDeptAdmin.value ? 'DEPARTMENT' : createForm.scope,
    accessDepartmentId: createForm.scope === 'DEPARTMENT' ? createForm.accessDepartmentId : null
  }
  
  submitLoading.value = true
  try {
    const draftRes = await saveDraftApi(payload)
    const savedId = draftRes?.data?.id || createForm.id
    
    if (publishNow && savedId) {
      await publishContentApi(savedId)
      ElMessage.success('公告文件直接发布上架成功，同步更新 ES 全文索引！')
    } else {
      ElMessage.success('已保存为草稿文件！')
    }
    
    showCreateDialog.value = false
    loadContents()
  } catch (err) {
    ElMessage.error(err.response?.data?.message || '操作失败')
  } finally {
    submitLoading.value = false
  }
}

const deleteDraft = async (id) => {
  try {
    await ElMessageBox.confirm('确认要删除该草稿文件吗？删除后不可恢复', '删除提示', {
      type: 'warning',
      confirmButtonText: '确定删除',
      cancelButtonText: '取消'
    })
    await deleteDraftApi(id)
    ElMessage.success('草稿已彻底删除')
    loadContents()
  } catch (ignored) {}
}

const publish = async (id) => {
  try {
    await publishContentApi(id)
    ElMessage.success('发布上架成功，已同步更新 ES 全文索引！')
    loadContents()
  } catch (err) {
    ElMessage.error(err.response?.data?.message || '发布失败')
  }
}

const unpublish = async (id) => {
  try {
    await unpublishContentApi(id)
    ElMessage.success('公告已下架，已从 ES 检索索引库移除')
    loadContents()
  } catch (err) {
    ElMessage.error(err.response?.data?.message || '下架失败')
  }
}

const viewDetail = async (item) => {
  try {
    const detailRes = await getContentDetailApi(item.id)
    if (detailRes && detailRes.data) {
      selectedItem.value = {
        ...detailRes.data,
        createdAt: detailRes.data.createTime || detailRes.data.publishTime || ''
      }
    } else {
      selectedItem.value = item
    }
  } catch (ignored) {
    selectedItem.value = item
  }
  detailVisible.value = true
}

const checkUrlParams = () => {
  const { type, title } = route.query
  if (type && title) {
    const decodedTitle = decodeURIComponent(title)
    const item = list.value.find(i => i.type === type && i.title === decodedTitle)
    if (item) {
      viewDetail(item)
    }
    activeTab.value = type
  }
}

onMounted(async () => {
  if (!localStorage.getItem('token')) return
  await loadDepartments()
  await loadContents()
  checkUrlParams()
})

watch(() => activeTab.value, () => {
  if (!searchMode.value) loadContents()
})

watch(() => route.query, () => {
  checkUrlParams()
}, { deep: true })
</script>

<style scoped>
.content-container {
  max-width: 1400px;
}

.page-header {
  margin-bottom: 20px;
}

.page-title {
  font-size: 28px;
  font-weight: bold;
  color: #1a1a2e;
  margin: 0;
}

.page-subtitle {
  font-size: 14px;
  color: #888;
  margin: 8px 0 0 0;
}

.search-box-card {
  margin-bottom: 24px;
  padding: 20px 24px;
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(16px);
  border-radius: 18px;
  border: 1px solid rgba(255, 255, 255, 0.6);
  box-shadow: 0 10px 30px rgba(0,0,0,0.04);
}

.search-form-row {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  align-items: center;
}

.category-chips {
  margin-top: 16px;
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  align-items: center;
}

.chip-label {
  font-size: 13px;
  color: #64748b;
  font-weight: 600;
  margin-right: 4px;
}

.chip-item {
  padding: 5px 14px;
  border-radius: 20px;
  font-size: 13px;
  background: rgba(102, 126, 234, 0.08);
  color: #475569;
  cursor: pointer;
  transition: all 0.2s ease;
  font-weight: 500;
}

.chip-item:hover, .chip-item.active {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #ffffff;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

.search-summary-bar {
  margin-bottom: 16px;
  font-weight: 600;
  color: #667eea;
  font-size: 15px;
}

.main-content {
  display: grid;
  grid-template-columns: 1fr;
  gap: 24px;
}

.tabs-section {
  flex: 1;
}

.content-tab-switcher {
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
}

.content-tab-btn {
  position: relative;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 28px;
  border: none;
  background: rgba(255, 255, 255, 0.6);
  backdrop-filter: blur(12px);
  border-radius: 16px;
  font-size: 16px;
  font-weight: 600;
  color: #64748b;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.03);
}

.content-tab-btn:hover {
  transform: translateY(-2px);
  color: #1a1a2e;
  background: rgba(255, 255, 255, 0.9);
}

.content-tab-btn.active {
  color: #ffffff;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  box-shadow: 0 8px 24px rgba(102, 126, 234, 0.35);
}

.content-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.content-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24px;
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(16px);
  border-radius: 20px;
  border: 1px solid rgba(255, 255, 255, 0.5);
  box-shadow: 0 8px 32px rgba(31, 38, 135, 0.04);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.content-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 16px 48px rgba(31, 38, 135, 0.08);
  border-color: rgba(102, 126, 234, 0.4);
}

.content-main {
  flex: 1;
}

.content-tags {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
  align-items: center;
}

.content-tag {
  padding: 4px 12px;
  border-radius: 8px;
  font-size: 12px;
  font-weight: 600;
}

.announcement-tag {
  background: rgba(102, 126, 234, 0.1);
  color: #667eea;
}

.policy-tag {
  background: rgba(118, 75, 162, 0.1);
  color: #764ba2;
}

.category-tag {
  background: rgba(16, 185, 129, 0.1) !important;
  color: #10b981 !important;
}

.content-status {
  padding: 4px 12px;
  border-radius: 8px;
  font-size: 12px;
  font-weight: 600;
}

.content-status.PUBLISHED {
  background: rgba(16, 185, 129, 0.1);
  color: #10b981;
}

.content-status.DRAFT {
  background: rgba(245, 158, 11, 0.1);
  color: #f59e0b;
}

.content-status.UNPUBLISHED {
  background: rgba(100, 116, 139, 0.1);
  color: #64748b;
}

.content-title {
  font-size: 18px;
  font-weight: bold;
  color: #1a1a2e;
  margin: 0 0 8px 0;
}

.content-preview {
  font-size: 14px;
  color: #64748b;
  margin: 0 0 16px 0;
  line-height: 1.6;
}

.content-meta {
  display: flex;
  gap: 16px;
  font-size: 13px;
  color: #94a3b8;
}

.content-actions {
  display: flex;
  gap: 8px;
  align-items: center;
  margin-left: 24px;
}

.detail-content {
  padding: 12px 0;
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.detail-title {
  font-size: 22px;
  font-weight: bold;
  color: #1a1a2e;
  margin-bottom: 12px;
}

.detail-meta {
  display: flex;
  gap: 20px;
  font-size: 14px;
  color: #64748b;
  margin-bottom: 20px;
}

.detail-divider {
  height: 1px;
  background: #e2e8f0;
  margin-bottom: 20px;
}

.detail-body {
  font-size: 15px;
  color: #334155;
  line-height: 1.8;
  min-height: 160px;
}

.content-fade-enter-active,
.content-fade-leave-active {
  transition: opacity 0.3s ease;
}

.content-fade-enter-from,
.content-fade-leave-to {
  opacity: 0;
}
</style>