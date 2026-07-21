<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Search,
  Document,
  Reading,
  EditPen,
  Delete,
  Promotion,
  Switch,
  RefreshRight,
  View,
  CollectionTag,
  FolderOpened
} from '@element-plus/icons-vue'
import {
  getEmployeeContentsApi,
  getAdminContentsApi,
  searchContentsApi,
  saveDraftApi,
  deleteDraftApi,
  publishContentApi,
  unpublishContentApi,
  getContentDetailApi,
  reindexEsApi,
  type ContentDetailVO,
  type ContentSaveDTO,
  type EsReindexResultVO
} from '../../api/content'
import { useAuthStore } from '../../stores/auth'

const auth = useAuthStore()
const isAdmin = computed(() => {
  const role = auth.user?.role || ''
  return ['SUPER_ADMIN', 'DEPT_MANAGER', 'ADMIN', 'ROLE_ADMIN'].includes(role)
})

// 当前 Tab 选项: 'search' | 'announcements' | 'policies' | 'admin'
const activeTab = ref('announcements')

// ================= 全局/普通检索状态 =================
const keyword = ref('')
const searchCategory = ref('')
const searchType = ref('')
const searchLoading = ref(false)
const searchResults = ref<ContentDetailVO[]>([])
const searchTotal = ref(0)
const searchPage = ref(1)

// ================= 公告制度列表状态 =================
const contents = ref<ContentDetailVO[]>([])
const contentTotal = ref(0)
const contentPage = ref(1)
const currentCategory = ref('ALL')
const listLoading = ref(false)

// ================= 详情 Modal =================
const detailModalVisible = ref(false)
const currentDetail = ref<ContentDetailVO | null>(null)
const detailLoading = ref(false)

// ================= 管理员模态框与草稿编辑 =================
const draftModalVisible = ref(false)
const submitLoading = ref(false)
const draftForm = ref<ContentSaveDTO>({
  type: 'ANNOUNCEMENT',
  title: '',
  category: '行政公告',
  body: '',
  scope: 'ALL',
  accessDepartmentId: undefined
})

// ================= 全量重建 ES 索引 =================
const reindexLoading = ref(false)
const reindexResult = ref<EsReindexResultVO | null>(null)

// 常用分类选项
const categoryOptions = ['行政公告', '人事制度', '财务规范', '安全规范', '技术标准']

onMounted(() => {
  fetchEmployeeContents()
})

// 切换分类 Tab
function handleCategoryChange(cat: string) {
  currentCategory.value = cat
  contentPage.value = 1
  fetchEmployeeContents()
}

// 员工获取已发布公告
async function fetchEmployeeContents() {
  listLoading.value = true
  try {
    const typeFilter = activeTab.value === 'announcements' ? 'ANNOUNCEMENT' : activeTab.value === 'policies' ? 'POLICY' : undefined
    const catFilter = currentCategory.value === 'ALL' ? undefined : currentCategory.value
    const res = await getEmployeeContentsApi({
      type: typeFilter,
      category: catFilter,
      page: contentPage.value,
      size: 9
    })
    contents.value = res.list || []
    contentTotal.value = res.total || 0
  } catch (err: any) {
    ElMessage.error(err?.message || '获取内容列表失败')
  } finally {
    listLoading.value = false
  }
}

// 管理员获取全部列表 (含草稿/发布/下架)
async function fetchAdminContents() {
  listLoading.value = true
  try {
    const res = await getAdminContentsApi({
      page: contentPage.value,
      size: 10
    })
    contents.value = res.list || []
    contentTotal.value = res.total || 0
  } catch (err: any) {
    ElMessage.error(err?.message || '获取管理员列表失败')
  } finally {
    listLoading.value = false
  }
}

// 执行 ES 全文检索
async function handleSearch() {
  if (!keyword.value.trim() && !searchCategory.value && !searchType.value) {
    ElMessage.warning('请输入搜索关键词或选择筛选条件')
    return
  }
  activeTab.value = 'search'
  searchLoading.value = true
  try {
    const res = await searchContentsApi({
      keyword: keyword.value.trim(),
      type: searchType.value || undefined,
      category: searchCategory.value || undefined,
      page: searchPage.value,
      size: 10
    })
    searchResults.value = res.list || []
    searchTotal.value = res.total || 0
  } catch (err: any) {
    ElMessage.error(err?.message || '全文检索异常')
  } finally {
    searchLoading.value = false
  }
}

// 查看详情
async function openDetail(item: ContentDetailVO) {
  detailModalVisible.value = true
  detailLoading.value = true
  try {
    const res = await getContentDetailApi(item.id)
    currentDetail.value = res
    // 保留高亮片段
    if (currentDetail.value) {
      if (item.highlightTitle) currentDetail.value.highlightTitle = item.highlightTitle
      if (item.highlightBody) currentDetail.value.highlightBody = item.highlightBody
    }
  } catch (err: any) {
    ElMessage.error(err?.message || '查看详情失败')
  } finally {
    detailLoading.value = false
  }
}

// 打开新建/编辑草稿框
function openDraftModal(item?: ContentDetailVO) {
  if (item) {
    draftForm.value = {
      id: item.id,
      type: item.type,
      title: item.title,
      category: item.category || '行政公告',
      body: item.body,
      scope: item.scope || 'ALL',
      accessDepartmentId: item.accessDepartmentId
    }
  } else {
    draftForm.value = {
      type: 'ANNOUNCEMENT',
      title: '',
      category: '行政公告',
      body: '',
      scope: 'ALL',
      accessDepartmentId: undefined
    }
  }
  draftModalVisible.value = true
}

// 保存草稿
async function handleSaveDraft() {
  if (!draftForm.value.title.trim() || !draftForm.value.body.trim()) {
    ElMessage.warning('请填写完整的标题和正文')
    return
  }
  submitLoading.value = true
  try {
    const res = await saveDraftApi(draftForm.value)
    ElMessage.success('草稿保存成功！')
    if (res.highlightTitle && res.highlightTitle.includes('重复')) {
      ElMessage.warning(res.highlightTitle)
    }
    draftModalVisible.value = false
    if (activeTab.value === 'admin') fetchAdminContents()
    else fetchEmployeeContents()
  } catch (err: any) {
    ElMessage.error(err?.message || '保存草稿失败')
  } finally {
    submitLoading.value = false
  }
}

// 发布内容
async function handlePublish(id: number) {
  try {
    await publishContentApi(id)
    ElMessage.success('发布成功！已同步至 ES 索引')
    fetchAdminContents()
  } catch (err: any) {
    ElMessage.error(err?.message || '发布失败')
  }
}

// 下架内容
async function handleUnpublish(id: number) {
  try {
    await unpublishContentApi(id)
    ElMessage.success('已下架！已从 ES 索引移除')
    fetchAdminContents()
  } catch (err: any) {
    ElMessage.error(err?.message || '下架失败')
  }
}

// 删除草稿
async function handleDeleteDraft(id: number) {
  try {
    await ElMessageBox.confirm('确认删除该草稿吗？删除后不可恢复', '提示', { type: 'warning' })
    await deleteDraftApi(id)
    ElMessage.success('草稿已删除')
    fetchAdminContents()
  } catch {
    // ignore
  }
}

// 手动一键全量重建 ES 索引
async function handleReindexEs() {
  reindexLoading.value = true
  try {
    const res = await reindexEsApi()
    reindexResult.value = res
    ElMessage.success(`全量重建成功！耗时: ${res.costMillis} ms, 成功同步: ${res.successCount} 条`)
  } catch (err: any) {
    ElMessage.error(err?.message || '重建 ES 索引失败')
  } finally {
    reindexLoading.value = false
  }
}

function handleTabClick(tabName: string) {
  activeTab.value = tabName
  contentPage.value = 1
  if (tabName === 'admin') {
    fetchAdminContents()
  } else if (tabName !== 'search') {
    fetchEmployeeContents()
  }
}
</script>

<template>
  <div class="content-page">
    <!-- 头部视觉 Banner 与检索中心 -->
    <div class="search-banner">
      <div class="banner-inner">
        <h2 class="title">智办 AI - 企业公告与制度规范中心</h2>
        <p class="subtitle">精准全文检索，涵盖行政、人事、财务与安全制度合规指南</p>

        <div class="search-bar-wrap">
          <el-input
            v-model="keyword"
            placeholder="输入搜索关键词（如：考勤、报销、请假制度）..."
            class="search-input"
            clearable
            @keyup.enter="handleSearch"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>

          <el-select v-model="searchType" placeholder="类型" clearable class="filter-select">
            <el-option label="公告" value="ANNOUNCEMENT" />
            <el-option label="制度" value="POLICY" />
          </el-select>

          <el-select v-model="searchCategory" placeholder="分类" clearable class="filter-select">
            <el-option v-for="cat in categoryOptions" :key="cat" :label="cat" :value="cat" />
          </el-select>

          <el-button type="primary" size="large" class="search-btn" :loading="searchLoading" @click="handleSearch">
            <el-icon><Search /></el-icon> 全文检索
          </el-button>
        </div>
      </div>
    </div>

    <!-- 主要内容容器 -->
    <div class="container">
      <!-- 导航 Tabs 与管理员区操作 -->
      <div class="top-nav">
        <div class="tab-list">
          <div
            class="tab-item"
            :class="{ active: activeTab === 'announcements' }"
            @click="handleTabClick('announcements')"
          >
            <el-icon><Reading /></el-icon> 智办公告
          </div>
          <div
            class="tab-item"
            :class="{ active: activeTab === 'policies' }"
            @click="handleTabClick('policies')"
          >
            <el-icon><Document /></el-icon> 规章制度
          </div>
          <div
            class="tab-item"
            :class="{ active: activeTab === 'search' }"
            @click="handleTabClick('search')"
          >
            <el-icon><Search /></el-icon> 搜索结果 <span v-if="searchTotal > 0" class="badge">{{ searchTotal }}</span>
          </div>
          <div
            v-if="isAdmin"
            class="tab-item admin-tab"
            :class="{ active: activeTab === 'admin' }"
            @click="handleTabClick('admin')"
          >
            <el-icon><EditPen /></el-icon> 内容发布与管理
          </div>
        </div>

        <!-- 管理员右侧快捷按钮 -->
        <div v-if="isAdmin" class="admin-actions">
          <el-button type="success" @click="openDraftModal()">
            <el-icon><EditPen /></el-icon> 新建草稿
          </el-button>
          <el-button type="warning" plain :loading="reindexLoading" @click="handleReindexEs">
            <el-icon><RefreshRight /></el-icon> 重建 ES 索引
          </el-button>
        </div>
      </div>

      <!-- 分类筛选子栏 (在员工浏览模式下展示) -->
      <div v-if="['announcements', 'policies'].includes(activeTab)" class="category-bar">
        <span
          class="cat-chip"
          :class="{ active: currentCategory === 'ALL' }"
          @click="handleCategoryChange('ALL')"
        >
          全部
        </span>
        <span
          v-for="cat in categoryOptions"
          :key="cat"
          class="cat-chip"
          :class="{ active: currentCategory === cat }"
          @click="handleCategoryChange(cat)"
        >
          {{ cat }}
        </span>
      </div>

      <!-- Tab 1 & 2: 员工浏览视图（网格卡片） -->
      <div v-if="['announcements', 'policies'].includes(activeTab)" class="cards-view" v-loading="listLoading">
        <el-empty v-if="contents.length === 0" description="暂无已发布的公告或制度" />
        <div class="cards-grid">
          <div v-for="item in contents" :key="item.id" class="content-card" @click="openDetail(item)">
            <div class="card-header">
              <el-tag :type="item.type === 'ANNOUNCEMENT' ? 'primary' : 'success'" size="small">
                {{ item.type === 'ANNOUNCEMENT' ? '公告' : '制度' }}
              </el-tag>
              <span class="category-tag">{{ item.category || '综合' }}</span>
              <span class="view-count">
                <el-icon><View /></el-icon> {{ item.viewCount || 0 }}
              </span>
            </div>
            <h3 class="card-title">{{ item.title }}</h3>
            <p class="card-body-snippet">{{ item.body }}</p>
            <div class="card-footer">
              <span class="pub-time">{{ item.publishTime ? item.publishTime.substring(0, 10) : '未发布' }}</span>
              <span class="scope-tag">{{ item.scope === 'ALL' ? '全公司公开' : '部门受限' }}</span>
            </div>
          </div>
        </div>

        <div class="pagination-wrap" v-if="contentTotal > 0">
          <el-pagination
            v-model:current-page="contentPage"
            :page-size="9"
            :total="contentTotal"
            layout="prev, pager, next, total"
            @current-change="fetchEmployeeContents"
          />
        </div>
      </div>

      <!-- Tab 3: ES 全文检索视图 (包含高亮) -->
      <div v-if="activeTab === 'search'" class="search-results-view" v-loading="searchLoading">
        <div class="search-summary" v-if="keyword">
          检索关键词：<strong class="kw-text">"{{ keyword }}"</strong>，共找到 {{ searchTotal }} 条匹配结果
        </div>
        <el-empty v-if="searchResults.length === 0" description="没有找到匹配的内容，请换个关键词试试" />
        
        <div class="search-list">
          <div v-for="item in searchResults" :key="item.id" class="search-item" @click="openDetail(item)">
            <div class="item-header">
              <el-tag :type="item.type === 'ANNOUNCEMENT' ? 'primary' : 'success'" size="small">
                {{ item.type === 'ANNOUNCEMENT' ? '公告' : '制度' }}
              </el-tag>
              <span class="category-name">{{ item.category }}</span>
              <span class="view-num"><el-icon><View /></el-icon> {{ item.viewCount }} 次浏览</span>
            </div>

            <!-- 标题高亮 -->
            <h3 class="item-title" v-html="item.highlightTitle || item.title"></h3>
            
            <!-- 正文高亮片段 -->
            <div class="item-snippet" v-html="item.highlightBody || item.body"></div>

            <div class="item-meta">
              <span>发布时间：{{ item.publishTime || item.createTime }}</span>
              <span>可见范围：{{ item.scope === 'ALL' ? '全公司' : '指定部门' }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Tab 4: 管理员管理列表视图 (含草稿、发布、下架、删除) -->
      <div v-if="activeTab === 'admin' && isAdmin" class="admin-view" v-loading="listLoading">
        <el-table :data="contents" border stripe style="width: 100%">
          <el-table-column prop="id" label="ID" width="70" />
          <el-table-column prop="title" label="标题" min-width="200" />
          <el-table-column prop="type" label="类型" width="100">
            <template #default="{ row }">
              <el-tag :type="row.type === 'ANNOUNCEMENT' ? 'primary' : 'success'">
                {{ row.type === 'ANNOUNCEMENT' ? '公告' : '制度' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="category" label="分类" width="120" />
          <el-table-column prop="status" label="状态" width="110">
            <template #default="{ row }">
              <el-tag v-if="row.status === 'PUBLISHED'" type="success">已发布</el-tag>
              <el-tag v-else-if="row.status === 'DRAFT'" type="info">草稿</el-tag>
              <el-tag v-else type="warning">已下架</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="version" label="版本" width="70" />
          <el-table-column prop="viewCount" label="浏览量" width="90" />
          <el-table-column prop="publishTime" label="发布时间" width="160" />
          <el-table-column label="操作" width="260" fixed="right">
            <template #default="{ row }">
              <el-button size="small" type="primary" link @click="openDetail(row)">查看</el-button>
              <el-button size="small" type="primary" link @click="openDraftModal(row)">编辑</el-button>
              <el-button
                v-if="row.status !== 'PUBLISHED'"
                size="small"
                type="success"
                link
                @click="handlePublish(row.id)"
              >
                发布
              </el-button>
              <el-button
                v-if="row.status === 'PUBLISHED'"
                size="small"
                type="warning"
                link
                @click="handleUnpublish(row.id)"
              >
                下架
              </el-button>
              <el-button
                v-if="row.status === 'DRAFT'"
                size="small"
                type="danger"
                link
                @click="handleDeleteDraft(row.id)"
              >
                删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="pagination-wrap" v-if="contentTotal > 0">
          <el-pagination
            v-model:current-page="contentPage"
            :page-size="10"
            :total="contentTotal"
            layout="prev, pager, next, total"
            @current-change="fetchAdminContents"
          />
        </div>
      </div>
    </div>

    <!-- 查看详情 Modal -->
    <el-dialog v-model="detailModalVisible" title="公告制度详情" width="700px" destroy-on-close>
      <div v-if="currentDetail" v-loading="detailLoading" class="detail-dialog-body">
        <div class="detail-header">
          <h2 v-html="currentDetail.highlightTitle || currentDetail.title"></h2>
          <div class="detail-tags">
            <el-tag :type="currentDetail.type === 'ANNOUNCEMENT' ? 'primary' : 'success'">
              {{ currentDetail.type === 'ANNOUNCEMENT' ? '公告' : '制度' }}
            </el-tag>
            <el-tag type="info">{{ currentDetail.category || '综合' }}</el-tag>
            <span class="meta-item">版本：v{{ currentDetail.version || 1 }}</span>
            <span class="meta-item">浏览量：{{ currentDetail.viewCount || 0 }} 次</span>
          </div>
        </div>

        <el-divider />

        <div class="detail-content-text" v-html="currentDetail.body"></div>

        <div class="detail-footer-info">
          <span>发布时间：{{ currentDetail.publishTime || '暂未发布' }}</span>
          <span>可见范围：{{ currentDetail.scope === 'ALL' ? '全公司公开' : '部门授权' }}</span>
        </div>
      </div>
    </el-dialog>

    <!-- 草稿维护 / 编辑 Modal -->
    <el-dialog
      v-model="draftModalVisible"
      :title="draftForm.id ? '编辑公告制度草稿' : '新建公告制度草稿'"
      width="680px"
      destroy-on-close
    >
      <el-form :model="draftForm" label-width="100px">
        <el-form-item label="内容类型" required>
          <el-radio-group v-model="draftForm.type">
            <el-radio value="ANNOUNCEMENT">公告 (Notice)</el-radio>
            <el-radio value="POLICY">规章制度 (Policy)</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="标题" required>
          <el-input v-model="draftForm.title" placeholder="请输入标题（勿包含敏感脚本）" maxLength="200" show-word-limit />
        </el-form-item>

        <el-form-item label="分类">
          <el-select v-model="draftForm.category" placeholder="请选择或输入分类" filterable allow-create style="width: 100%">
            <el-option v-for="cat in categoryOptions" :key="cat" :label="cat" :value="cat" />
          </el-select>
        </el-form-item>

        <el-form-item label="可见范围">
          <el-radio-group v-model="draftForm.scope">
            <el-radio value="ALL">全公司公开 (ALL)</el-radio>
            <el-radio value="DEPARTMENT">部门隔离 (DEPARTMENT)</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item v-if="draftForm.scope === 'DEPARTMENT'" label="授权部门 ID">
          <el-input-number v-model="draftForm.accessDepartmentId" :min="1" placeholder="填写部门ID" />
        </el-form-item>

        <el-form-item label="正文内容" required>
          <el-input
            v-model="draftForm.body"
            type="textarea"
            :rows="10"
            placeholder="支持HTML或纯文本正文内容（系统将自动进行XSS过滤防注入）"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="draftModalVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSaveDraft">保存草稿</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.content-page {
  min-height: 100vh;
  background: #f4f6fa;
}

/* 顶部搜索 Banner */
.search-banner {
  background: linear-gradient(135deg, #1e293b 0%, #0f172a 100%);
  color: #fff;
  padding: 40px 20px;
  text-align: center;
}

.banner-inner {
  max-width: 900px;
  margin: 0 auto;
}

.banner-inner .title {
  font-size: 26px;
  font-weight: 700;
  margin-bottom: 8px;
  letter-spacing: 0.5px;
}

.banner-inner .subtitle {
  font-size: 14px;
  color: #94a3b8;
  margin-bottom: 24px;
}

.search-bar-wrap {
  display: flex;
  gap: 10px;
  background: rgba(255, 255, 255, 0.1);
  padding: 8px;
  border-radius: 12px;
  backdrop-filter: blur(8px);
  border: 1px solid rgba(255, 255, 255, 0.15);
}

.search-input {
  flex: 1;
}

.filter-select {
  width: 120px;
}

.search-btn {
  padding: 0 24px;
  border-radius: 8px;
  font-weight: 600;
}

/* 容器区 */
.container {
  max-width: 1140px;
  margin: -20px auto 40px;
  padding: 0 16px;
}

/* 顶部导航 Tabs */
.top-nav {
  background: #fff;
  border-radius: 12px;
  padding: 12px 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  margin-bottom: 20px;
}

.tab-list {
  display: flex;
  gap: 16px;
}

.tab-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  color: #64748b;
  cursor: pointer;
  transition: all 0.2s ease;
}

.tab-item:hover {
  background: #f1f5f9;
  color: #0f172a;
}

.tab-item.active {
  background: #2563eb;
  color: #fff;
}

.tab-item .badge {
  background: #ef4444;
  color: #fff;
  font-size: 11px;
  padding: 2px 6px;
  border-radius: 10px;
}

.admin-actions {
  display: flex;
  gap: 10px;
}

/* 分类 Chips */
.category-bar {
  display: flex;
  gap: 10px;
  margin-bottom: 20px;
}

.cat-chip {
  padding: 6px 16px;
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 20px;
  font-size: 13px;
  color: #475569;
  cursor: pointer;
  transition: all 0.2s ease;
}

.cat-chip:hover {
  border-color: #2563eb;
  color: #2563eb;
}

.cat-chip.active {
  background: #e0e7ff;
  border-color: #6366f1;
  color: #4338ca;
  font-weight: 600;
}

/* 卡片视图 */
.cards-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
  gap: 20px;
}

.content-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  border: 1px solid #e2e8f0;
  cursor: pointer;
  transition: all 0.25s ease;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.content-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 24px rgba(0, 0, 0, 0.08);
  border-color: #93c5fd;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.category-tag {
  font-size: 12px;
  color: #64748b;
  background: #f1f5f9;
  padding: 2px 8px;
  border-radius: 4px;
}

.view-count {
  margin-left: auto;
  font-size: 12px;
  color: #94a3b8;
  display: flex;
  align-items: center;
  gap: 4px;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: #1e293b;
  margin-bottom: 8px;
  line-height: 1.4;
}

.card-body-snippet {
  font-size: 13px;
  color: #64748b;
  line-height: 1.6;
  margin-bottom: 16px;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-footer {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: #94a3b8;
  border-top: 1px solid #f1f5f9;
  padding-top: 12px;
}

/* 搜索结果视图 */
.search-results-view {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.search-summary {
  font-size: 14px;
  color: #475569;
  margin-bottom: 20px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f1f5f9;
}

.kw-text {
  color: #2563eb;
}

.search-item {
  padding: 16px 0;
  border-bottom: 1px solid #f1f5f9;
  cursor: pointer;
}

.search-item:last-child {
  border-bottom: none;
}

.item-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}

.category-name {
  font-size: 12px;
  color: #64748b;
}

.view-num {
  font-size: 12px;
  color: #94a3b8;
  margin-left: auto;
}

.item-title {
  font-size: 16px;
  font-weight: 600;
  color: #1d4ed8;
  margin-bottom: 6px;
}

.item-snippet {
  font-size: 13px;
  color: #475569;
  line-height: 1.6;
  margin-bottom: 8px;
}

.item-meta {
  font-size: 12px;
  color: #94a3b8;
  display: flex;
  gap: 20px;
}

/* 高亮文本样式 */
:deep(.highlight) {
  color: #dc2626;
  background: #fef2f2;
  font-weight: bold;
  font-style: normal;
  padding: 0 2px;
  border-radius: 2px;
}

/* 管理员视图 */
.admin-view {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

/* 详情 Modal */
.detail-header h2 {
  font-size: 20px;
  font-weight: 700;
  color: #0f172a;
  margin-bottom: 12px;
}

.detail-tags {
  display: flex;
  align-items: center;
  gap: 10px;
}

.meta-item {
  font-size: 12px;
  color: #64748b;
}

.detail-content-text {
  font-size: 15px;
  line-height: 1.8;
  color: #334155;
  min-height: 150px;
  white-space: pre-wrap;
}

.detail-footer-info {
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid #f1f5f9;
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: #94a3b8;
}

.pagination-wrap {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}
</style>
