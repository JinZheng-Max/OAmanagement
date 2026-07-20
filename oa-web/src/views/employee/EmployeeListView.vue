<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAuthStore } from '../../stores/auth'
import {
  getEmployeePage,
  createEmployee,
  updateEmployee,
  createAccount,
  resetPassword,
  EmployeeInfo,
  CreateEmployeeRequest,
  UpdateEmployeeRequest,
} from '../../api/employee'
import { listActiveDepartments, type DepartmentInfo } from '../../api/department'

const auth = useAuthStore()

// ---- 状态变量 ----
const loading = ref(false)
const employeeList = ref<EmployeeInfo[]>([])

// 分页
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 搜索条件（拆分为三个独立字段）
const searchName = ref('')
const searchEmployeeNo = ref('')
const searchPhone = ref('')

// 弹窗控制
const dialogVisible = ref(false)
const isEdit = ref(false)
const formLoading = ref(false)
const currentEmployee = ref<EmployeeInfo | null>(null)

// 表单数据
const form = ref<CreateEmployeeRequest>({
  employeeNo: '',
  name: '',
  departmentId: null,
  position: '',
  phone: '',
  hireDate: '',
})
// 编辑时单独记录状态，因为 CreateRequest 没有 status
const editStatus = ref(1)

// 开通账号弹窗
const accountDialogVisible = ref(false)
const accountForm = ref({ username: '' })
const accountResult = ref('')

// 重置密码结果弹窗
const passwordDialogVisible = ref(false)
const passwordResult = ref('')

// 部门列表（用于下拉选择）
const departments = ref<DepartmentInfo[]>([])
async function loadDepartments() {
  try { departments.value = await listActiveDepartments() }
  catch { /* 部门列表加载失败不影响主体功能 */ }
}

// 详情抽屉
const detailDrawerVisible = ref(false)
const detailData = ref<EmployeeInfo | null>(null)

// ---- 生命周期 ----
onMounted(() => { fetchData(); loadDepartments() })

// ---- 数据加载 ----
async function fetchData() {
  loading.value = true
  try {
    const result = await getEmployeePage({
      page: currentPage.value,
      size: pageSize.value,
      name: searchName.value || undefined,
      employeeNo: searchEmployeeNo.value || undefined,
      phone: searchPhone.value || undefined,
    })
    employeeList.value = result.records
    total.value = result.total
  } catch (err: any) {
    ElMessage.error(err?.response?.data?.message || '加载员工列表失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  currentPage.value = 1
  fetchData()
}

function handleReset() {
  searchName.value = ''
  searchEmployeeNo.value = ''
  searchPhone.value = ''
  currentPage.value = 1
  fetchData()
}

function handlePageChange(page: number) {
  currentPage.value = page
  fetchData()
}

function handleSizeChange(size: number) {
  pageSize.value = size
  currentPage.value = 1
  fetchData()
}

// ---- 新增/编辑 ----
function openAddDialog() {
  isEdit.value = false
  currentEmployee.value = null
  form.value = { employeeNo: '', name: '', departmentId: null, position: '', phone: '', hireDate: '' }
  editStatus.value = 1
  dialogVisible.value = true
}

function openEditDialog(row: EmployeeInfo) {
  isEdit.value = true
  currentEmployee.value = row
  form.value = {
    employeeNo: row.employeeNo,
    name: row.name,
    departmentId: row.departmentId,
    position: row.position,
    phone: row.phone,
    hireDate: row.hireDate,
  }
  editStatus.value = row.status
  dialogVisible.value = true
}

async function handleSubmit() {
  formLoading.value = true
  try {
    if (isEdit.value && currentEmployee.value) {
      const updateData: UpdateEmployeeRequest = {
        name: form.value.name,
        departmentId: form.value.departmentId,
        position: form.value.position,
        phone: form.value.phone,
        hireDate: form.value.hireDate,
        status: editStatus.value,  // <-- 现在可以修改状态了
      }
      await updateEmployee(currentEmployee.value.id, updateData)
      ElMessage.success('更新成功')
    } else {
      await createEmployee(form.value)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    fetchData()
  } catch (err: any) {
    ElMessage.error(err?.response?.data?.message || '操作失败')
  } finally {
    formLoading.value = false
  }
}

// ---- 查看详情 ----
function openDetailDrawer(row: EmployeeInfo) {
  detailData.value = row
  detailDrawerVisible.value = true
}

// ---- 开通账号 ----
function openAccountDialog(row: EmployeeInfo) {
  currentEmployee.value = row
  accountForm.value = { username: '' }
  accountResult.value = ''
  accountDialogVisible.value = true
}

async function handleCreateAccount() {
  if (!currentEmployee.value) return
  formLoading.value = true
  try {
    const password = await createAccount(currentEmployee.value.id, {
      username: accountForm.value.username || undefined,
    })
    accountResult.value = password
    ElMessage.success('账号开通成功')
    // 刷新列表以更新 hasAccount 状态
    fetchData()
  } catch (err: any) {
    ElMessage.error(err?.response?.data?.message || '开通账号失败')
  } finally {
    formLoading.value = false
  }
}

// ---- 重置密码 ----
async function handleResetPassword(userId: number) {
  try {
    await ElMessageBox.confirm('确认要重置该用户的密码吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    const password = await resetPassword(userId)
    passwordResult.value = password
    passwordDialogVisible.value = true
  } catch { /* 取消操作 */ }
}

/** 直接在表格中切换在职/离职状态 */
async function handleStatusChange(row: EmployeeInfo) {
  try {
    const newStatus = row.status === 1 ? 0 : 1
    await updateEmployee(row.id, { status: newStatus })
    row.status = newStatus
    ElMessage.success(newStatus === 1 ? '已设置为在职' : '已设置为离职')
  } catch (err: any) {
    ElMessage.error('状态更新失败')
    // 刷新恢复原始状态
    fetchData()
  }
}

const isAdmin = auth.isAdmin
</script>

<template>
  <div class="employee-page">
    <!-- ===== 页面标题栏 ===== -->
    <div class="page-header">
      <h2>员工管理</h2>
      <el-button type="primary" @click="openAddDialog">+ 新增员工</el-button>
    </div>

    <!-- ===== 搜索栏（3个字段 + 按钮） ===== -->
    <div class="search-bar">
      <el-input
        v-model="searchName"
        placeholder="按姓名搜索"
        clearable
        style="width: 180px"
        @keyup.enter="handleSearch"
      />
      <el-input
        v-model="searchEmployeeNo"
        placeholder="按工号搜索"
        clearable
        style="width: 180px"
        @keyup.enter="handleSearch"
      />
      <el-input
        v-model="searchPhone"
        placeholder="按手机号搜索"
        clearable
        style="width: 180px"
        @keyup.enter="handleSearch"
      />
      <el-button type="primary" @click="handleSearch">
        <el-icon><Search /></el-icon> 搜索
      </el-button>
      <el-button @click="handleReset">
        <el-icon><Refresh /></el-icon> 重置
      </el-button>
    </div>

    <!-- ===== 员工表格 ===== -->
    <el-table
      :data="employeeList"
      v-loading="loading"
      border
      stripe
      style="width: 100%"
      empty-text="暂无数据"
      class="employee-table"
    >
      <el-table-column prop="employeeNo" label="工号" width="120" header-align="center" align="center" />
      <el-table-column prop="name" label="姓名" width="110" header-align="center" align="center" />
      <el-table-column prop="departmentName" label="部门" width="140" header-align="center" align="center" />
      <el-table-column prop="position" label="职位" min-width="140" header-align="center" align="center" />
      <el-table-column prop="phone" label="手机号" width="140" header-align="center" align="center" />

      <!-- 系统账号列 -->
      <el-table-column label="系统账号" width="110" header-align="center" align="center">
        <template #default="{ row }">
          <el-tag :type="row.hasAccount ? 'success' : 'info'" size="small">
            {{ row.hasAccount ? '已开通' : '未开通' }}
          </el-tag>
        </template>
      </el-table-column>

      <!-- 状态列：开关 + 文字 -->
      <el-table-column label="状态" width="140" header-align="center" align="center">
        <template #default="{ row }">
          <div class="status-cell">
            <el-switch
              :model-value="row.status === 1"
              :active-text="row.status === 1 ? '在职' : ''"
              :inactive-text="row.status === 0 ? '离职' : ''"
              inline-prompt
              @change="handleStatusChange(row)"
            />
          </div>
        </template>
      </el-table-column>

      <el-table-column prop="hireDate" label="入职日期" width="120" header-align="center" align="center" />
      <el-table-column label="操作" width="320" fixed="right" header-align="center" align="center">
        <template #default="{ row }">
          <el-button size="small" type="primary" link @click="openDetailDrawer(row)">
            <el-icon><View /></el-icon> 查看
          </el-button>
          <el-button size="small" type="primary" link @click="openEditDialog(row)">
            <el-icon><Edit /></el-icon> 编辑
          </el-button>

          <template v-if="isAdmin">
            <el-button
              v-if="row.hasAccount && row.userId"
              size="small"
              type="warning"
              link
              @click="handleResetPassword(row.userId)"
            >
              <el-icon><Key /></el-icon> 重置密码
            </el-button>
            <el-button
              v-else
              size="small"
              type="success"
              link
              @click="openAccountDialog(row)"
            >
              <el-icon><Plus /></el-icon> 开通账号
            </el-button>
          </template>
        </template>
      </el-table-column>
    </el-table>

    <!-- ===== 分页 ===== -->
    <div class="pagination-wrapper">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :page-sizes="[10, 20, 50]"
        :total="total"
        layout="total, sizes, prev, pager, next, jumper"
        background
        @current-change="handlePageChange"
        @size-change="handleSizeChange"
      />
    </div>

    <!-- ===== 新增/编辑 弹窗 ===== -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑员工' : '新增员工'"
      width="540px"
      :close-on-click-modal="false"
    >
      <el-form :model="form" label-width="100px" @submit.prevent="handleSubmit">
        <el-form-item label="工号" required>
          <el-input v-model="form.employeeNo" :disabled="isEdit" placeholder="请输入工号" />
        </el-form-item>
        <el-form-item label="姓名" required>
          <el-input v-model="form.name" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="部门">
          <el-select v-model="form.departmentId" placeholder="请选择部门" clearable style="width:100%">
            <el-option
              v-for="d in departments"
              :key="d.id"
              :label="d.name"
              :value="d.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="职位">
          <el-input v-model="form.position" placeholder="请输入职位" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="form.phone" placeholder="请输入11位手机号" maxlength="11" />
        </el-form-item>
        <el-form-item label="入职日期">
          <el-input v-model="form.hireDate" placeholder="格式: yyyy-MM-dd" />
        </el-form-item>
        <!-- 编辑模式下显示状态选项 -->
        <el-form-item v-if="isEdit" label="在职状态">
          <el-radio-group v-model="editStatus">
            <el-radio :value="1">在职</el-radio>
            <el-radio :value="0">离职</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="formLoading" @click="handleSubmit">
          {{ isEdit ? '保存' : '确认新增' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- ===== 员工详情抽屉 ===== -->
    <el-drawer v-model="detailDrawerVisible" title="员工详情" size="400px">
      <template v-if="detailData">
        <div class="detail-item"><label>工号：</label><span>{{ detailData.employeeNo }}</span></div>
        <div class="detail-item"><label>姓名：</label><span>{{ detailData.name }}</span></div>
        <div class="detail-item"><label>部门：</label><span>{{ detailData.departmentName || '-' }}</span></div>
        <div class="detail-item"><label>职位：</label><span>{{ detailData.position || '-' }}</span></div>
        <div class="detail-item"><label>手机号：</label><span>{{ detailData.phone || '-' }}</span></div>
        <div class="detail-item">
          <label>系统账号：</label>
          <el-tag :type="detailData.hasAccount ? 'success' : 'info'" size="small">
            {{ detailData.hasAccount ? '已开通' : '未开通' }}
          </el-tag>
        </div>
        <div class="detail-item">
          <label>状态：</label>
          <el-tag :type="detailData.status === 1 ? 'success' : 'danger'" size="small">
            {{ detailData.status === 1 ? '在职' : '离职' }}
          </el-tag>
        </div>
        <div class="detail-item"><label>入职日期：</label><span>{{ detailData.hireDate || '-' }}</span></div>
        <div class="detail-item"><label>创建时间：</label><span>{{ detailData.createTime }}</span></div>
        <div class="detail-item"><label>更新时间：</label><span>{{ detailData.updateTime }}</span></div>
      </template>
    </el-drawer>

    <!-- ===== 开通账号弹窗 ===== -->
    <el-dialog v-model="accountDialogVisible" title="开通系统账号" width="420px" :close-on-click-modal="false">
      <div v-if="!accountResult">
        <p style="margin-bottom: 16px;">
          为员工 <strong>{{ currentEmployee?.name }}</strong> 开通系统账号
        </p>
        <p style="font-size: 13px; color: #666; margin-bottom: 16px;">
          开通后该员工将拥有系统登录权限，可使用账号密码登录 OA 系统
        </p>
        <el-form :model="accountForm" label-width="80px">
          <el-form-item label="用户名">
            <el-input v-model="accountForm.username" placeholder="不填则默认使用工号" />
          </el-form-item>
        </el-form>
      </div>
      <div v-else class="result-box">
        <p>✅ 账号开通成功！</p>
        <p>初始密码为：</p>
        <div class="password-display">{{ accountResult }}</div>
        <el-alert title="请立即告知员工，并提醒其首次登录后修改密码" type="warning" :closable="false" show-icon />
      </div>
      <template #footer>
        <template v-if="!accountResult">
          <el-button @click="accountDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="formLoading" @click="handleCreateAccount">确认开通</el-button>
        </template>
        <template v-else>
          <el-button @click="accountDialogVisible = false; accountResult = ''">关闭</el-button>
        </template>
      </template>
    </el-dialog>

    <!-- ===== 重置密码结果弹窗 ===== -->
    <el-dialog v-model="passwordDialogVisible" title="密码已重置" width="400px">
      <div class="result-box">
        <p>新密码为：</p>
        <div class="password-display">{{ passwordResult }}</div>
        <el-alert title="请立即告知员工并提醒其登录后修改密码" type="warning" :closable="false" show-icon />
      </div>
      <template #footer>
        <el-button @click="passwordDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.employee-page {
  padding: 16px 20px;
  background: #fff;
  border-radius: 8px;
  min-height: calc(100vh - 80px);
}

/* ---- 页面标题 ---- */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 14px;
  border-bottom: 2px solid #f0f2f5;
}
.page-header h2 {
  margin: 0;
  font-size: 22px;
  font-weight: 700;
  color: #1d2129;
  letter-spacing: 1px;
}
.page-header h2::before {
  content: '';
  display: inline-block;
  width: 4px;
  height: 22px;
  background: #409eff;
  border-radius: 2px;
  margin-right: 10px;
  vertical-align: -3px;
}

/* ---- 搜索栏 ---- */
.search-bar {
  display: flex;
  gap: 10px;
  margin-bottom: 16px;
  padding: 14px 16px;
  background: #f7f8fa;
  border-radius: 6px;
  align-items: center;
}
.search-bar :deep(.el-input__wrapper) {
  border-radius: 6px;
}

/* ---- 表格居中 ---- */
.employee-table :deep(th.el-table__cell) {
  text-align: center !important;
}
.employee-table :deep(.el-table__cell) {
  text-align: center;
}

/* ---- 分页 ---- */
.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 14px;
}

/* ---- 详情抽屉 ---- */
.detail-item {
  display: flex;
  padding: 10px 0;
  border-bottom: 1px solid #f0f0f0;
}
.detail-item label {
  width: 90px;
  color: #666;
  flex-shrink: 0;
}

/* ---- 状态列开关 ---- */
.status-cell {
  display: flex;
  justify-content: center;
  align-items: center;
}
.status-cell :deep(.el-switch) {
  --el-switch-on-color: #67c23a;
  --el-switch-off-color: #f56c6c;
}

/* ---- 密码展示 ---- */
.result-box { text-align: center; padding: 16px 0; }
.result-box p { margin: 8px 0; font-size: 14px; }
.password-display {
  display: inline-block;
  background: #f5f7fa;
  border: 1px dashed #409eff;
  border-radius: 4px;
  padding: 10px 24px;
  margin: 12px 0;
  font-size: 18px;
  font-weight: bold;
  letter-spacing: 2px;
  color: #409eff;
}
</style>
