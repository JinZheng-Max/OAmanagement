<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  getDepartmentPage, createDepartment, updateDepartment, updateDepartmentStatus,
  type DepartmentInfo,
} from '../../api/department'
import { getEmployeesByDepartment, type EmployeeInfo } from '../../api/employee'

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
const form = ref({ code: '', name: '', leaderId: null as number | null, sort: 0 })

// 部门人员
const deptEmployees = ref<Record<number, EmployeeInfo[]>>({})
const currentDeptEmployees = ref<EmployeeInfo[]>([])
const detailVisible = ref(false)
const detailDept = ref<DepartmentInfo | null>(null)
const detailEmployees = ref<EmployeeInfo[]>([])

onMounted(() => fetchData())

async function fetchData() {
  loading.value = true
  try {
    const r = await getDepartmentPage({ page: currentPage.value, size: pageSize.value, keyword: keyword.value || undefined })
    list.value = r.records; total.value = r.total
  } catch { ElMessage.error('加载失败') }
  finally { loading.value = false }
}

/** 查看部门人员 */
async function viewDetail(row: DepartmentInfo) {
  detailDept.value = row
  detailVisible.value = true
  if (!deptEmployees.value[row.id]) {
    try {
      deptEmployees.value[row.id] = await getEmployeesByDepartment(row.id)
    } catch { deptEmployees.value[row.id] = [] }
  }
  detailEmployees.value = deptEmployees.value[row.id] || []
}

function handleSearch() { currentPage.value = 1; fetchData() }
function handleReset() { keyword.value = ''; currentPage.value = 1; fetchData() }

function openAddDialog() {
  isEdit.value = false; currentDept.value = null
  form.value = { code: '', name: '', leaderId: null, sort: 0 }
  currentDeptEmployees.value = []
  dialogVisible.value = true
}

async function openEditDialog(row: DepartmentInfo) {
  isEdit.value = true; currentDept.value = row
  form.value = { code: row.code, name: row.name, leaderId: row.leaderId, sort: row.sort }
  if (!deptEmployees.value[row.id]) {
    try { deptEmployees.value[row.id] = await getEmployeesByDepartment(row.id) } catch { deptEmployees.value[row.id] = [] }
  }
  currentDeptEmployees.value = deptEmployees.value[row.id] || []
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
  const newStatus = row.status === 1 ? 0 : 1
  try {
    await updateDepartmentStatus(row.id, newStatus)
    row.status = newStatus
    ElMessage.success(newStatus === 1 ? '已启用' : '已停用')
  } catch { ElMessage.error('操作失败') }
}
</script>

<template>
  <div class="dept-page">
    <div class="page-header">
      <h2>部门管理</h2>
      <el-button type="primary" @click="openAddDialog">+ 新增部门</el-button>
    </div>

    <div class="search-bar">
      <el-input v-model="keyword" placeholder="按部门名称/编码搜索" clearable style="width:260px" @keyup.enter="handleSearch">
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-button type="primary" @click="handleSearch">搜索</el-button>
      <el-button @click="handleReset">重置</el-button>
    </div>

    <el-table :data="list" v-loading="loading" border stripe class="dept-table" empty-text="暂无数据">
      <el-table-column type="index" label="#" width="55" header-align="center" align="center" />
      <el-table-column prop="code" label="代码" width="130" header-align="center" align="center" />
      <el-table-column prop="name" label="部门名称" min-width="150" header-align="center" align="center" />
      <el-table-column prop="leaderName" label="负责人" width="120" header-align="center" align="center">
        <template #default="{ row }">
          <span v-if="row.leaderName" class="leader-tag">{{ row.leaderName }}</span>
          <span v-else class="muted">-</span>
        </template>
      </el-table-column>
      <el-table-column prop="employeeCount" label="人数" width="70" header-align="center" align="center" />
      <el-table-column label="状态" width="120" header-align="center" align="center">
        <template #default="{ row }">
          <el-switch :model-value="row.status===1" inline-prompt @change="toggleStatus(row)" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="220" fixed="right" header-align="center" align="center">
        <template #default="{ row }">
          <el-button size="small" type="primary" link @click="viewDetail(row)">
            <el-icon><View /></el-icon> 成员
          </el-button>
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
    <el-dialog v-model="dialogVisible" :title="isEdit?'编辑部门':'新增部门'" width="480px" :close-on-click-modal="false">
      <el-form :model="form" label-width="90px">
        <el-form-item label="编码" required>
          <el-input v-model="form.code" :disabled="isEdit" placeholder="如 DEPT_MARKET" />
        </el-form-item>
        <el-form-item label="名称" required>
          <el-input v-model="form.name" placeholder="请输入部门名称" />
        </el-form-item>
        <el-form-item label="负责人">
          <el-select v-model="form.leaderId" placeholder="请选择本部门成员" clearable style="width:100%">
            <el-option v-for="e in currentDeptEmployees" :key="e.id" :label="`${e.name} (${e.employeeNo})`" :value="e.id" />
            <template #empty><span class="muted" style="font-size:13px;">该部门暂无员工</span></template>
          </el-select>
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

    <!-- 部门成员弹窗 -->
    <el-dialog v-model="detailVisible" :title="`${detailDept?.name || ''} - 成员列表`" width="520px" :close-on-click-modal="false">
      <div v-if="detailEmployees.length" class="member-grid">
        <div v-for="emp in detailEmployees" :key="emp.id" class="member-card">
          <div class="member-avatar">{{ emp.name?.charAt(0) || '?' }}</div>
          <div class="member-info">
            <div class="member-name">{{ emp.name }}</div>
            <div class="member-meta">{{ emp.employeeNo }} · {{ emp.position || '-' }}</div>
          </div>
          <el-tag v-if="emp.id === detailDept?.leaderId" size="small" type="primary" class="leader-badge">负责人</el-tag>
        </div>
      </div>
      <div v-else class="empty-state">
        <el-empty :image-size="80" description="该部门暂无员工" />
      </div>
    </el-dialog>
  </div>
</template>

<style scoped>
.dept-page { padding: 16px 20px; background: #fff; border-radius: 8px; min-height: calc(100vh - 80px); }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; padding-bottom: 14px; border-bottom: 2px solid #f0f2f5; }
.page-header h2 { margin: 0; font-size: 22px; font-weight: 700; color: #1d2129; letter-spacing: 1px; }
.page-header h2::before { content: ''; display: inline-block; width: 4px; height: 22px; background: #409eff; border-radius: 2px; margin-right: 10px; vertical-align: -3px; }
.search-bar { display: flex; gap: 10px; margin-bottom: 16px; padding: 14px 16px; background: #f7f8fa; border-radius: 6px; align-items: center; }
.dept-table :deep(th.el-table__cell) { text-align: center !important; }
.dept-table :deep(.el-table__cell) { text-align: center; }
.pagination-wrapper { display: flex; justify-content: flex-end; margin-top: 14px; }
.muted { color: #bbb; }
.leader-tag { color: #409eff; font-weight: 500; }

/* 成员弹窗网格 */
.member-grid { display: grid; grid-template-columns: 1fr; gap: 8px; }
.member-card {
  display: flex; align-items: center; gap: 14px;
  padding: 12px 16px; border-radius: 8px;
  border: 1px solid #ebeef5; transition: all 0.2s;
}
.member-card:hover { border-color: #409eff; box-shadow: 0 2px 8px rgba(64,158,255,0.1); }
.member-avatar {
  width: 40px; height: 40px; border-radius: 50%;
  background: linear-gradient(135deg, #409eff, #79bbff);
  color: #fff; font-size: 16px; font-weight: 600;
  display: flex; align-items: center; justify-content: center; flex-shrink: 0;
}
.member-info { flex: 1; min-width: 0; }
.member-name { font-size: 14px; font-weight: 600; color: #1d2129; }
.member-meta { font-size: 12px; color: #86909c; margin-top: 2px; }
.leader-badge { flex-shrink: 0; }
.empty-state { padding: 20px 0; }
</style>
