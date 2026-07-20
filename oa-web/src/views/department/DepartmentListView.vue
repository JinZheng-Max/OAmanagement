<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  getDepartmentPage, createDepartment, updateDepartment, updateDepartmentStatus,
  DepartmentInfo,
} from '../../api/department'

const loading = ref(false)
const list = ref<DepartmentInfo[]>([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const keyword = ref('')

// 弹窗
const dialogVisible = ref(false)
const isEdit = ref(false)
const formLoading = ref(false)
const currentDept = ref<DepartmentInfo | null>(null)
const form = ref({ code: '', name: '', sort: 0 })

onMounted(() => fetchData())

async function fetchData() {
  loading.value = true
  try {
    const r = await getDepartmentPage({ page: currentPage.value, size: pageSize.value, keyword: keyword.value || undefined })
    list.value = r.records; total.value = r.total
  } catch (err: any) { ElMessage.error(err?.response?.data?.message || '加载失败') }
  finally { loading.value = false }
}

function handleSearch() { currentPage.value = 1; fetchData() }
function handleReset() { keyword.value = ''; currentPage.value = 1; fetchData() }

function openAddDialog() {
  isEdit.value = false; currentDept.value = null
  form.value = { code: '', name: '', sort: 0 }
  dialogVisible.value = true
}

function openEditDialog(row: DepartmentInfo) {
  isEdit.value = true; currentDept.value = row
  form.value = { code: row.code, name: row.name, sort: row.sort }
  dialogVisible.value = true
}

async function handleSubmit() {
  formLoading.value = true
  try {
    if (isEdit.value && currentDept.value) {
      await updateDepartment(currentDept.value.id, form.value)
      ElMessage.success('更新成功')
    } else {
      await createDepartment(form.value)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false; fetchData()
  } catch (err: any) { ElMessage.error(err?.response?.data?.message || '操作失败') }
  finally { formLoading.value = false }
}

async function toggleStatus(row: DepartmentInfo) {
  try {
    const newStatus = row.status === 1 ? 0 : 1
    await updateDepartmentStatus(row.id, newStatus)
    row.status = newStatus
    ElMessage.success(newStatus === 1 ? '已启用' : '已停用')
  } catch (err: any) { ElMessage.error(err?.response?.data?.message || '操作失败') }
}
</script>

<template>
  <div class="dept-page">
    <div class="page-header">
      <h2>部门管理</h2>
      <el-button type="primary" @click="openAddDialog">+ 新增部门</el-button>
    </div>

    <div class="search-bar">
      <el-input v-model="keyword" placeholder="按部门名称/编码搜索" clearable style="width:260px" @keyup.enter="handleSearch" />
      <el-button type="primary" @click="handleSearch"><el-icon><Search /></el-icon> 搜索</el-button>
      <el-button @click="handleReset"><el-icon><Refresh /></el-icon> 重置</el-button>
    </div>

    <el-table :data="list" v-loading="loading" border stripe class="dept-table" empty-text="暂无数据">
      <el-table-column prop="code" label="编码" width="140" header-align="center" align="center" />
      <el-table-column prop="name" label="部门名称" min-width="160" header-align="center" align="center" />
      <el-table-column prop="leaderName" label="负责人" width="130" header-align="center" align="center">
        <template #default="{ row }">{{ row.leaderName || '-' }}</template>
      </el-table-column>
      <el-table-column prop="sort" label="排序" width="80" header-align="center" align="center" />
      <el-table-column label="状态" width="130" header-align="center" align="center">
        <template #default="{ row }">
          <el-switch
            :model-value="row.status === 1"
            active-text="启用"
            inactive-text="停用"
            inline-prompt
            @change="toggleStatus(row)"
          />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right" header-align="center" align="center">
        <template #default="{ row }">
          <el-button size="small" type="primary" link @click="openEditDialog(row)">
            <el-icon><Edit /></el-icon> 编辑
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-wrapper">
      <el-pagination
        v-model:current-page="currentPage" v-model:page-size="pageSize"
        :page-sizes="[10,20,50]" :total="total"
        layout="total, sizes, prev, pager, next, jumper" background
        @current-change="fetchData" @size-change="() => { currentPage=1; fetchData() }"
      />
    </div>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="isEdit?'编辑部门':'新增部门'" width="460px" :close-on-click-modal="false">
      <el-form :model="form" label-width="90px">
        <el-form-item label="编码" required>
          <el-input v-model="form.code" :disabled="isEdit" placeholder="如 DEPT_MARKET" />
        </el-form-item>
        <el-form-item label="名称" required>
          <el-input v-model="form.name" placeholder="请输入部门名称" />
        </el-form-item>
        <el-form-item label="排序号">
          <el-input-number v-model="form.sort" :min="0" :max="999" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="formLoading" @click="handleSubmit">{{ isEdit?'保存':'确认新增' }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.dept-page { padding: 16px 20px; background: #fff; border-radius: 8px; min-height: calc(100vh - 80px); }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; padding-bottom: 14px; border-bottom: 2px solid #f0f2f5; }
.page-header h2 { margin: 0; font-size: 22px; font-weight: 700; color: #1d2129; }
.page-header h2::before { content: ''; display: inline-block; width: 4px; height: 22px; background: #409eff; border-radius: 2px; margin-right: 10px; vertical-align: -3px; }
.search-bar { display: flex; gap: 10px; margin-bottom: 16px; padding: 14px 16px; background: #f7f8fa; border-radius: 6px; align-items: center; }
.dept-table :deep(th.el-table__cell) { text-align: center !important; }
.dept-table :deep(.el-table__cell) { text-align: center; }
.pagination-wrapper { display: flex; justify-content: flex-end; margin-top: 14px; }
</style>
