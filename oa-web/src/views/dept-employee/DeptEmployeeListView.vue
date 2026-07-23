<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getMyDepartment, type MyDepartmentResponse } from '../../api/department'
import {
  getEmployeePage, createEmployee, updateEmployee, createAccount, resetPassword,
  type EmployeeInfo, type CreateEmployeeRequest, type UpdateEmployeeRequest
} from '../../api/employee'

const loading = ref(false)
const deptInfo = ref<MyDepartmentResponse | null>(null)
const list = ref<EmployeeInfo[]>([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const nameKeyword = ref('')

// 新增/编辑员工弹窗
const empDialogVisible = ref(false)
const isEdit = ref(false)
const currentEmpId = ref<number | null>(null)
const formLoading = ref(false)
const empForm = ref({
  employeeNo: '',
  name: '',
  position: '',
  phone: '',
})

// 开通账号弹窗
const accountDialogVisible = ref(false)
const selectedEmp = ref<EmployeeInfo | null>(null)
const accountForm = ref({ username: '', role: 'EMPLOYEE' })
const accountResult = ref('')

onMounted(async () => {
  await fetchDeptAndEmployees()
})

async function fetchDeptAndEmployees() {
  loading.value = true
  try {
    deptInfo.value = await getMyDepartment()
    if (deptInfo.value?.department?.id) {
      const res = await getEmployeePage({
        page: currentPage.value,
        size: pageSize.value,
        departmentId: deptInfo.value.department.id,
        name: nameKeyword.value || undefined,
      })
      list.value = res.records
      total.value = res.total
    }
  } catch (err: any) {
    ElMessage.error(err?.response?.data?.message || '加载部门员工失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  currentPage.value = 1
  fetchDeptAndEmployees()
}

function openAddEmpModal() {
  isEdit.value = false
  currentEmpId.value = null
  empForm.value = { employeeNo: '', name: '', position: '', phone: '' }
  empDialogVisible.value = true
}

function openEditEmpModal(row: EmployeeInfo) {
  isEdit.value = true
  currentEmpId.value = row.id
  empForm.value = {
    employeeNo: row.employeeNo,
    name: row.name,
    position: row.position || '',
    phone: row.phone || '',
  }
  empDialogVisible.value = true
}

async function handleEmpSubmit() {
  if (!empForm.value.name) {
    ElMessage.warning('请填写姓名')
    return
  }
  if (!deptInfo.value?.department?.id) {
    ElMessage.error('无法获取所在部门信息')
    return
  }
  formLoading.value = true
  try {
    if (isEdit.value && currentEmpId.value) {
      const updateData: UpdateEmployeeRequest = {
        name: empForm.value.name,
        departmentId: deptInfo.value.department.id,
        position: empForm.value.position,
        phone: empForm.value.phone,
      }
      await updateEmployee(currentEmpId.value, updateData)
      ElMessage.success('更新员工信息成功')
    } else {
      const createData: CreateEmployeeRequest = {
        employeeNo: empForm.value.employeeNo,
        name: empForm.value.name,
        departmentId: deptInfo.value.department.id,
        position: empForm.value.position,
        phone: empForm.value.phone,
      }
      await createEmployee(createData)
      ElMessage.success('新增部门员工成功')
    }
    empDialogVisible.value = false
    fetchDeptAndEmployees()
  } catch (err: any) {
    ElMessage.error(err?.response?.data?.message || '操作失败')
  } finally {
    formLoading.value = false
  }
}

function openAccountModal(row: EmployeeInfo) {
  selectedEmp.value = row
  accountForm.value = { username: '', role: 'EMPLOYEE' }
  accountResult.value = ''
  accountDialogVisible.value = true
}

async function handleCreateAccount() {
  if (!selectedEmp.value) return
  formLoading.value = true
  try {
    const pwd = await createAccount(selectedEmp.value.id, {
      username: accountForm.value.username || undefined,
      role: accountForm.value.role || 'EMPLOYEE',
    })
    accountResult.value = pwd
    ElMessage.success('账号开通成功')
    fetchDeptAndEmployees()
  } catch (err: any) {
    ElMessage.error(err?.response?.data?.message || '开通账号失败')
  } finally {
    formLoading.value = false
  }
}

async function handleResetPassword(row: EmployeeInfo) {
  if (!row.userId) return
  try {
    await ElMessageBox.confirm(`确定要为员工 ${row.name} 重置账号密码吗？`, '重置密码', {
      confirmButtonText: '确定重置',
      cancelButtonText: '取消',
      type: 'warning',
    })
    const newPwd = await resetPassword(row.userId)
    ElMessageBox.alert(`密码重置成功！新密码为：<strong>${newPwd}</strong>`, '重置结果', {
      dangerouslyUseHTMLString: true,
      confirmButtonText: '我知道了',
    })
  } catch {}
}

function getRoleTagType(role: string | null | undefined): string {
  if (!role) return 'info'
  const r = role.toUpperCase()
  if (r.includes('SUPER_ADMIN') || r.includes('ADMIN')) return 'danger'
  if (r.includes('DEPT_MANAGER') || r.includes('MANAGER')) return 'warning'
  return 'info'
}

function getRoleLabel(role: string | null | undefined): string {
  if (!role) return '普通员工'
  const r = role.toUpperCase()
  if (r.includes('SUPER_ADMIN') || r.includes('ADMIN')) return '超级管理员'
  if (r.includes('DEPT_MANAGER') || r.includes('MANAGER')) return '部门管理员'
  return '普通员工'
}
</script>

<template>
  <div class="dept-emp-page">
    <div class="page-header">
      <div>
        <h2>部门员工管理</h2>
        <span class="sub-title" v-if="deptInfo">所属部门：{{ deptInfo.department.name }}（共 {{ deptInfo.totalCount }} 人）</span>
      </div>
      <el-button type="primary" @click="openAddEmpModal">+ 新增本部门员工</el-button>
    </div>

    <div class="search-bar">
      <el-input v-model="nameKeyword" placeholder="按姓名检索" clearable style="width:240px" @keyup.enter="handleSearch">
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-button type="primary" @click="handleSearch">查询</el-button>
    </div>

    <el-table :data="list" v-loading="loading" border stripe class="emp-table" empty-text="本部门暂无员工记录">
      <el-table-column prop="employeeNo" label="工号" width="130" header-align="center" align="center" />
      <el-table-column prop="name" label="姓名" width="120" header-align="center" align="center">
        <template #default="{ row }">
          <span style="font-weight: 600;">{{ row.name }}</span>
          <el-tag v-if="row.id === deptInfo?.leader?.id" size="small" type="primary" style="margin-left: 6px;">负责人</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="position" label="职位" width="140" header-align="center" align="center">
        <template #default="{ row }">{{ row.position || '-' }}</template>
      </el-table-column>
      <el-table-column prop="phone" label="联系电话" width="150" header-align="center" align="center">
        <template #default="{ row }">{{ row.phone || '-' }}</template>
      </el-table-column>
      <el-table-column label="系统账号状态" width="140" header-align="center" align="center">
        <template #default="{ row }">
          <el-tag v-if="row.hasAccount" type="success" size="small">已开通账号</el-tag>
          <el-tag v-else type="info" size="small">未开通账号</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="系统角色" width="130" header-align="center" align="center">
        <template #default="{ row }">
          <template v-if="row.hasAccount">
            <el-tag :type="getRoleTagType(row.role)" size="small">
              {{ getRoleLabel(row.role) }}
            </el-tag>
          </template>
          <span v-else class="muted">-</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" min-width="220" fixed="right" header-align="center" align="center">
        <template #default="{ row }">
          <el-button size="small" type="primary" link @click="openEditEmpModal(row)">编辑</el-button>
          <el-button v-if="!row.hasAccount" size="small" type="success" link @click="openAccountModal(row)">开通账号</el-button>
          <el-button v-if="row.hasAccount" size="small" type="warning" link @click="handleResetPassword(row)">重置密码</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-wrapper">
      <el-pagination
        v-model:current-page="currentPage" v-model:page-size="pageSize"
        :page-sizes="[10, 20, 50]" :total="total"
        layout="total, sizes, prev, pager, next, jumper" background
        @current-change="fetchDeptAndEmployees"
      />
    </div>

    <!-- 新增/编辑本部门员工弹窗 -->
    <el-dialog v-model="empDialogVisible" :title="isEdit ? '编辑员工信息' : '新增本部门员工'" width="460px">
      <el-form :model="empForm" label-width="90px">
        <el-form-item label="姓名" required>
          <el-input v-model="empForm.name" placeholder="请输入员工姓名" />
        </el-form-item>
        <el-form-item label="职位">
          <el-input v-model="empForm.position" placeholder="如 前端开发工程师" />
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="empForm.phone" placeholder="请输入联系电话" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="empDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="formLoading" @click="handleEmpSubmit">{{ isEdit ? '保存修改' : '确认新增' }}</el-button>
      </template>
    </el-dialog>

    <!-- 开通账号弹窗 -->
    <el-dialog v-model="accountDialogVisible" title="开通系统账号" width="440px">
      <div v-if="!accountResult">
        <p style="margin-bottom: 16px;">
          为员工 <strong>{{ selectedEmp?.name }}</strong> 开通系统账号
        </p>
        <el-form :model="accountForm" label-width="90px">
          <el-form-item label="用户名">
            <el-input v-model="accountForm.username" placeholder="不填默认使用工号" />
          </el-form-item>
          <el-form-item label="系统角色" required>
            <el-select v-model="accountForm.role" style="width: 100%">
              <el-option label="普通员工 (EMPLOYEE)" value="EMPLOYEE" />
              <el-option label="部门管理员 (DEPT_MANAGER)" value="DEPT_MANAGER" />
            </el-select>
          </el-form-item>
        </el-form>
      </div>
      <div v-else class="result-box">
        <p>✅ 账号开通成功！</p>
        <p>初始密码为：</p>
        <div class="password-display" style="font-size: 20px; font-weight: bold; color: #409eff; text-align: center; margin: 12px 0;">{{ accountResult }}</div>
        <el-alert title="请告知员工并提醒其及时修改初始密码" type="warning" :closable="false" show-icon />
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
  </div>
</template>

<style scoped>
.dept-emp-page { padding: 20px 24px; background: #fff; border-radius: 8px; min-height: calc(100vh - 80px); }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; padding-bottom: 14px; border-bottom: 2px solid #f0f2f5; }
.page-header h2 { margin: 0 0 4px; font-size: 22px; font-weight: 700; color: #1d2129; letter-spacing: 1px; }
.sub-title { font-size: 13px; color: #86909c; }
.search-bar { display: flex; gap: 10px; margin-bottom: 16px; padding: 12px 16px; background: #f7f8fa; border-radius: 6px; align-items: center; }
.pagination-wrapper { display: flex; justify-content: flex-end; margin-top: 14px; }
.muted { color: #bbb; }
</style>
