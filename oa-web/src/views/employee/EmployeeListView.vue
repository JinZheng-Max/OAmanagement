<script setup lang="ts">
/**
 * 员工管理列表页
 *
 * Vue 3 的 <script setup> 语法：不用写 export default，直接写逻辑
 * 所有顶层变量和函数都会自动暴露给模板使用
 */
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

// ---- 状态变量（ref 包裹的响应式数据） ----
const auth = useAuthStore()
const loading = ref(false)         // 表格加载状态
const employeeList = ref<EmployeeInfo[]>([])  // 员工列表数据

// 分页参数
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 搜索条件
const searchKeyword = ref('')
const searchDepartmentId = ref<number | null>(null)

// 弹窗控制
const dialogVisible = ref(false)      // 新增/编辑弹窗
const isEdit = ref(false)             // 是编辑还是新增
const formLoading = ref(false)        // 提交按钮加载状态
const currentEmployee = ref<EmployeeInfo | null>(null)  // 当前操作的员工

// 表单数据
const form = ref<CreateEmployeeRequest>({
  employeeNo: '',
  name: '',
  departmentId: null,
  position: '',
  phone: '',
  hireDate: '',
})

// 开通账号弹窗
const accountDialogVisible = ref(false)
const accountForm = ref({ username: '' })
const accountResult = ref('')

// 重置密码结果弹窗
const passwordDialogVisible = ref(false)
const passwordResult = ref('')

// 详情抽屉
const detailDrawerVisible = ref(false)
const detailData = ref<EmployeeInfo | null>(null)

// ---- 生命周期钩子 ----
onMounted(() => {
  fetchData()  // 页面加载时自动查询
})

// ---- 方法 ----

/**
 * 加载员工列表数据
 * async/await：异步函数，等待后端返回后再继续
 */
async function fetchData() {
  loading.value = true
  try {
    const result = await getEmployeePage({
      page: currentPage.value,
      size: pageSize.value,
      keyword: searchKeyword.value || undefined,
      departmentId: searchDepartmentId.value,
    })
    employeeList.value = result.records
    total.value = result.total
  } catch (err: any) {
    ElMessage.error(err?.response?.data?.message || '加载员工列表失败')
  } finally {
    loading.value = false
  }
}

/** 搜索按钮 */
function handleSearch() {
  currentPage.value = 1  // 搜索时回到第一页
  fetchData()
}

/** 重置搜索 */
function handleReset() {
  searchKeyword.value = ''
  searchDepartmentId.value = null
  currentPage.value = 1
  fetchData()
}

/** 翻页时触发 */
function handlePageChange(page: number) {
  currentPage.value = page
  fetchData()
}

/** 每页条数改变时触发 */
function handleSizeChange(size: number) {
  pageSize.value = size
  currentPage.value = 1
  fetchData()
}

// ---- 新增/编辑 ----

/** 打开新增弹窗 */
function openAddDialog() {
  isEdit.value = false
  currentEmployee.value = null
  form.value = { employeeNo: '', name: '', departmentId: null, position: '', phone: '', hireDate: '' }
  dialogVisible.value = true
}

/** 打开编辑弹窗 */
function openEditDialog(row: EmployeeInfo) {
  isEdit.value = true
  currentEmployee.value = row
  form.value = {
    employeeNo: row.employeeNo,   // 编辑时也显示工号（仅供查看）
    name: row.name,
    departmentId: row.departmentId,
    position: row.position,
    phone: row.phone,
    hireDate: row.hireDate,
  }
  dialogVisible.value = true
}

/** 提交表单（新增或编辑） */
async function handleSubmit() {
  formLoading.value = true
  try {
    if (isEdit.value && currentEmployee.value) {
      // 编辑模式：只传需要修改的字段
      const updateData: UpdateEmployeeRequest = {
        name: form.value.name,
        departmentId: form.value.departmentId,
        position: form.value.position,
        phone: form.value.phone,
        hireDate: form.value.hireDate,
      }
      await updateEmployee(currentEmployee.value.id, updateData)
      ElMessage.success('更新成功')
    } else {
      // 新增模式
      await createEmployee(form.value)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    fetchData()  // 刷新列表
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
  } catch {
    // 用户取消了操作，不需要处理
  }
}

/** 判断是否管理员 — 直接用 auth.isAdmin 在模板中保持响应式 */
</script>

<template>
  <div class="employee-page">
    <!-- ===== 页面标题栏 ===== -->
    <div class="page-header">
      <h2>员工管理</h2>
      <el-button type="primary" @click="openAddDialog">+ 新增员工</el-button>
    </div>

    <!-- ===== 搜索栏 ===== -->
    <div class="search-bar">
      <el-input
        v-model="searchKeyword"
        placeholder="搜索姓名 / 工号 / 手机号"
        clearable
        style="width: 280px"
        @keyup.enter="handleSearch"
      />
      <el-button type="primary" @click="handleSearch">搜索</el-button>
      <el-button @click="handleReset">重置</el-button>
    </div>

    <!-- ===== 员工表格 ===== -->
    <el-table
      :data="employeeList"
      v-loading="loading"
      border
      stripe
      style="width: 100%"
      empty-text="暂无数据"
    >
      <el-table-column prop="employeeNo" label="工号" width="120" />
      <el-table-column prop="name" label="姓名" width="120" />
      <el-table-column prop="departmentName" label="部门" width="140" />
      <el-table-column prop="position" label="职位" min-width="150" />
      <el-table-column prop="phone" label="手机号" width="140" />
      <el-table-column prop="status" label="状态" width="90">
        <!--
          slot-scope 接收当前行的数据
          row.status === 1 显示"在职"（绿色），否则显示"离职"（灰色）
        -->
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">
            {{ row.status === 1 ? '在职' : '离职' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="hireDate" label="入职日期" width="120" />
      <el-table-column label="操作" width="280" fixed="right">
        <template #default="{ row }">
          <el-button size="small" type="primary" link @click="openDetailDrawer(row)">
            查看
          </el-button>
          <el-button size="small" type="primary" link @click="openEditDialog(row)">
            编辑
          </el-button>
          <!-- 只有管理员才能看到开通账号 -->
          <template v-if="auth.isAdmin">
            <el-button size="small" type="success" link @click="openAccountDialog(row)">
              开通账号
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
      width="520px"
      :close-on-click-modal="false"
    >
      <el-form
        :model="form"
        label-width="90px"
        @submit.prevent="handleSubmit"
        class="employee-form"
      >
        <el-form-item label="工号" required>
          <el-input v-model="form.employeeNo" :disabled="isEdit" placeholder="请输入工号" />
        </el-form-item>
        <el-form-item label="姓名" required>
          <el-input v-model="form.name" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="部门">
          <el-input v-model="form.departmentId" placeholder="部门ID（数字）" type="number" />
          <div class="form-tip">暂时输入数字，后续会改为下拉选择</div>
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
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="formLoading" @click="handleSubmit">
          {{ isEdit ? '保存' : '确认新增' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- ===== 员工详情抽屉 ===== -->
    <el-drawer
      v-model="detailDrawerVisible"
      title="员工详情"
      size="400px"
    >
      <template v-if="detailData">
        <div class="detail-item"><label>工号：</label><span>{{ detailData.employeeNo }}</span></div>
        <div class="detail-item"><label>姓名：</label><span>{{ detailData.name }}</span></div>
        <div class="detail-item"><label>部门：</label><span>{{ detailData.departmentName || '-' }}</span></div>
        <div class="detail-item"><label>职位：</label><span>{{ detailData.position || '-' }}</span></div>
        <div class="detail-item"><label>手机号：</label><span>{{ detailData.phone || '-' }}</span></div>
        <div class="detail-item">
          <label>状态：</label>
          <el-tag :type="detailData.status === 1 ? 'success' : 'info'" size="small">
            {{ detailData.status === 1 ? '在职' : '离职' }}
          </el-tag>
        </div>
        <div class="detail-item"><label>入职日期：</label><span>{{ detailData.hireDate || '-' }}</span></div>
        <div class="detail-item"><label>创建时间：</label><span>{{ detailData.createTime }}</span></div>
        <div class="detail-item"><label>更新时间：</label><span>{{ detailData.updateTime }}</span></div>
      </template>
    </el-drawer>

    <!-- ===== 开通账号弹窗 ===== -->
    <el-dialog
      v-model="accountDialogVisible"
      title="开通系统账号"
      width="420px"
      :close-on-click-modal="false"
    >
      <!-- 内容区：未开通时显示表单，开通后显示密码 -->
      <div v-if="!accountResult">
        <p style="margin-bottom: 16px;">为员工 <strong>{{ currentEmployee?.name }}</strong> 开通系统账号</p>
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

      <!-- 底部按钮 -->
      <template #footer>
        <template v-if="!accountResult">
          <el-button @click="accountDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="formLoading" @click="handleCreateAccount">
            确认开通
          </el-button>
        </template>
        <template v-else>
          <el-button @click="accountDialogVisible = false; accountResult = ''">关闭</el-button>
        </template>
      </template>
    </el-dialog>

    <!-- ===== 重置密码结果弹窗 ===== -->
    <el-dialog
      v-model="passwordDialogVisible"
      title="密码已重置"
      width="400px"
    >
      <div class="result-box">
        <p>新密码为：</p>
        <div class="password-display">{{ passwordResult }}</div>
        <el-alert title="请立即告知员工，并提醒其登录后修改密码" type="warning" :closable="false" show-icon />
      </div>
      <template #footer>
        <el-button @click="passwordDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
/**
 * scoped 表示样式只作用于当前组件，不影响其他页面
 */
.employee-page {
  padding: 24px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0;
  font-size: 20px;
  color: #172033;
}

.search-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}

.form-tip {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}

.employee-form .el-form-item {
  margin-bottom: 18px;
}

/* 详情抽屉样式 */
.detail-item {
  display: flex;
  padding: 12px 0;
  border-bottom: 1px solid #f0f0f0;
}

.detail-item label {
  width: 80px;
  color: #666;
  flex-shrink: 0;
}

/* 密码展示框样式 */
.result-box {
  text-align: center;
  padding: 16px 0;
}

.result-box p {
  margin: 8px 0;
  font-size: 14px;
}

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
