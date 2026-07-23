<template>
  <div class="org-container" v-if="!loading">
    <div class="page-header" v-if="currentView === 'departments'">
      <div class="header-top">
        <div>
          <h2 class="page-title">🏢 组织管理</h2>
          <p class="page-subtitle">管理部门结构和员工档案信息</p>
        </div>
        <div style="display: flex; gap: 12px;">
          <el-button v-if="isAdmin" type="success" size="large" @click="showImportDialog = true">
            <el-icon><Upload /></el-icon>
            批量导入员工
          </el-button>
          <el-button v-if="isAdmin" type="primary" size="large" @click="showAddDeptDialog = true">
            <el-icon><Plus /></el-icon>
            添加部门
          </el-button>
        </div>
      </div>
    </div>

    <div class="page-header" v-else>
      <div class="header-top">
        <div>
          <h2 class="page-title">👥 {{ selectedDept?.name }} 员工详情</h2>
          <p class="page-subtitle">{{ isRegularUser ? '仅查看，无操作权限' : '查看该部门下所有员工信息' }}</p>
        </div>
        <el-button v-if="isAdmin" type="default" size="large" @click="goBack">
          <el-icon><ArrowLeft /></el-icon>
          返回部门列表
        </el-button>
      </div>
    </div>

    <div class="dept-cards" v-if="currentView === 'departments'">
      <div 
        class="dept-card" 
        v-for="dept in depts" 
        :key="dept.id"
        @click="goToEmployees(dept)"
      >
        <div class="dept-card-bg"></div>
        <div class="dept-card-content">
          <div class="dept-icon">
            <el-icon><OfficeBuilding /></el-icon>
          </div>
          <div class="dept-info">
            <h3 class="dept-name">{{ dept.name }}</h3>
            <p class="dept-code">部门编码：{{ dept.code }}</p>
            <p class="dept-employee-count">员工数量：{{ getDeptEmployeeCount(dept.id) }} 人</p>
            <div class="dept-meta">
              <span class="dept-status" :class="dept.status === 1 ? 'active' : 'inactive'">
                {{ dept.status === 1 ? '启用' : '停用' }}
              </span>
              <span class="dept-leader" v-if="getLeaderName(dept.leaderId)">
                负责人：{{ getLeaderName(dept.leaderId) }}
              </span>
            </div>
          </div>
          <div class="dept-arrow">
            <el-icon><ArrowRight /></el-icon>
          </div>
        </div>
      </div>
    </div>

    <div class="employee-section" v-else>
      <div class="employee-card">
        <div class="card-header">
          <span class="card-title">{{ selectedDept?.name }} 员工列表</span>
          <div class="card-actions" v-if="showAddEmployee">
            <el-button v-if="isAdmin" type="success" size="small" @click="showImportDialog = true">
              <el-icon><Upload /></el-icon>
              批量导入员工
            </el-button>
            <el-button type="primary" size="small" @click="showAddEmpDialog = true">
              <el-icon><Plus /></el-icon>
              添加员工
            </el-button>
          </div>
        </div>
        <div class="table-wrapper">
          <el-table :data="filteredEmployees" stripe :height="Math.max(300, Math.min(600, filteredEmployees.length * 60 + 80))">
            <el-table-column prop="employeeNo" label="工号" width="140" />
            <el-table-column prop="name" label="姓名" width="120" />
            <el-table-column prop="departmentName" label="所属部门" width="180" />
            <el-table-column prop="position" label="职务" width="160" />
            <el-table-column v-if="isAdmin" prop="phone" label="手机号" width="150" />
            <el-table-column prop="hireDate" label="入职日期" width="150" />
            <el-table-column prop="status" label="状态" width="120">
              <template #default="scope">
                <el-tag :type="scope.row.status === 1 ? 'success' : 'info'" size="small">
                  {{ scope.row.status === 1 ? '在职' : '离职' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column v-if="isAdmin || isDeptAdmin" label="操作" width="280">
              <template #default="scope">
                <el-button v-if="canEditEmployee(scope.row)" size="small" type="primary" @click="action('edit', scope.row)">
                  <el-icon><Edit /></el-icon>
                  编辑
                </el-button>
                <el-button v-if="canDeleteEmployee(scope.row)" size="small" type="danger" @click="action('delete', scope.row.id)">
                  <el-icon><Delete /></el-icon>
                  删除
                </el-button>
                <el-button v-if="isAdmin" size="small" type="warning" @click="setDeptAdmin(scope.row)">
                  <el-icon><User /></el-icon>
                  设置部门管理员
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
        <div v-if="filteredEmployees.length === 0" class="empty-state">
          <el-icon class="empty-icon"><User /></el-icon>
          <p>该部门暂无员工</p>
        </div>
      </div>
    </div>

    <el-dialog title="添加部门" v-model="showAddDeptDialog" width="500px" append-to-body align-center>
      <el-form :model="newDept" label-width="100px">
        <el-form-item label="部门名称">
          <el-input v-model="newDept.name" placeholder="请输入部门名称" />
        </el-form-item>
        <el-form-item label="部门编码">
          <el-input v-model="newDept.code" placeholder="请输入部门编码" />
        </el-form-item>
        <el-form-item label="部门负责人">
          <el-select v-model="newDept.leaderId" placeholder="请选择负责人">
            <el-option v-for="emp in employees" :key="emp.id" :label="emp.name" :value="emp.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="权重排序">
          <el-input-number v-model="newDept.sort" :min="0" :max="100" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddDeptDialog = false">取消</el-button>
        <el-button type="primary" @click="addDepartment">确认添加</el-button>
      </template>
    </el-dialog>

    <el-dialog :title="editingEmployee ? '编辑员工' : '添加员工'" v-model="showAddEmpDialog" width="500px" append-to-body align-center>
      <el-form :model="newEmployee" label-width="100px">
        <el-form-item label="员工姓名">
          <el-input v-model="newEmployee.name" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="职务">
          <el-input v-model="newEmployee.position" placeholder="请输入职务" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="newEmployee.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="入职日期">
          <el-date-picker v-model="newEmployee.hireDate" type="date" placeholder="选择入职日期" />
        </el-form-item>
        <el-form-item label="状态" v-if="editingEmployee">
          <el-select v-model="newEmployee.status" placeholder="请选择状态">
            <el-option label="在职" :value="1" />
            <el-option label="离职" :value="0" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="handleCloseEmpDialog">取消</el-button>
        <el-button type="primary" @click="addEmployee">确认{{ editingEmployee ? '编辑' : '添加' }}</el-button>
      </template>
    </el-dialog>

    <!-- ===== 批量导入员工 弹窗 ===== -->
    <el-dialog title="批量导入员工" v-model="showImportDialog" width="600px" append-to-body align-center :close-on-click-modal="false">
      <div class="import-dialog-body">
        <div class="template-section" style="margin-bottom: 20px; padding: 14px 18px; background: rgba(102, 126, 234, 0.05); border-radius: 12px; border: 1px dashed rgba(102, 126, 234, 0.3);">
          <p style="margin-bottom: 6px; font-weight: 600; color: #1a1a2e;">第 1 步：下载标准导入模版</p>
          <p style="font-size: 13px; color: #64748b; margin-bottom: 10px;">
            模版包含【姓名、部门、职位、电话号码】列属性。工号无需填写，系统自动分配（Smart开头）。
          </p>
          <el-button type="primary" link @click="downloadTemplate">
            <el-icon><Download /></el-icon> 点击下载标准导入模板 (.csv)
          </el-button>
        </div>

        <div class="upload-section" style="margin-bottom: 20px;">
          <p style="margin-bottom: 8px; font-weight: 600; color: #1a1a2e;">第 2 步：上传填写好的 Excel 或 CSV 文件</p>
          <el-upload
            drag
            action=""
            :auto-upload="false"
            :show-file-list="true"
            :limit="1"
            accept=".xlsx,.xls,.csv"
            :on-change="handleFileChange"
          >
            <el-icon class="el-icon--upload"><Upload /></el-icon>
            <div class="el-upload__text">
              将 Excel / CSV 文件拖到此处，或<em>点击选择文件</em>
            </div>
            <template #tip>
              <div class="el-upload__tip">
                支持扩展名为 .xlsx, .xls, .csv 的表格文件，系统将自动匹配部门并分配系统账号
              </div>
            </template>
          </el-upload>
        </div>

        <div v-if="importResult" class="result-section" style="margin-top: 16px; border-top: 1px solid #e2e8f0; padding-top: 16px;">
          <el-alert
            :title="`导入分析完成：解析总数 ${importResult.totalCount} 条，成功 ${importResult.successCount} 条，失败 ${importResult.failCount} 条（已自动开通系统账号，默认初始密码：123456）`"
            :type="importResult.failCount === 0 ? 'success' : 'warning'"
            show-icon
            :closable="false"
            style="margin-bottom: 12px;"
          />
          <div v-if="importResult.successDetails && importResult.successDetails.length > 0" style="margin-bottom: 12px;">
            <p style="font-size: 13px; font-weight: 600; color: #10b981; margin-bottom: 6px;">已自动开通账号及初始密码：</p>
            <ul style="max-height: 140px; overflow-y: auto; font-size: 12px; color: #047857; background: #ecfdf5; padding: 10px 16px 10px 32px; border-radius: 8px; border: 1px solid #a7f3d0;">
              <li v-for="(succ, idx) in importResult.successDetails" :key="idx">{{ succ }}</li>
            </ul>
          </div>
          <div v-if="importResult.errorMessages && importResult.errorMessages.length > 0">
            <p style="font-size: 13px; font-weight: 600; color: #ef4444; margin-bottom: 6px;">失败原因与排错明细：</p>
            <ul style="max-height: 140px; overflow-y: auto; font-size: 12px; color: #dc2626; background: #fff5f5; padding: 10px 16px 10px 32px; border-radius: 8px; border: 1px solid #fecaca;">
              <li v-for="(err, idx) in importResult.errorMessages" :key="idx">{{ err }}</li>
            </ul>
          </div>
        </div>
      </div>

      <template #footer>
        <el-button @click="showImportDialog = false; importResult = null">关闭</el-button>
        <el-button type="primary" :loading="importLoading" @click="handleImportSubmit">
          确认开始导入
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, Edit, Delete, ArrowLeft, ArrowRight, OfficeBuilding, User, Upload, Download } from '@element-plus/icons-vue'
import {
  getDepartmentPage,
  createDepartment,
  getEmployeePage,
  createEmployee,
  updateEmployee,
  createAccount,
  importEmployees,
  getMyDepartment
} from '../api/org'

const loading = ref(true)
const currentView = ref('departments')
const depts = ref([])
const employees = ref([])
const selectedDept = ref(null)

const showAddDeptDialog = ref(false)
const showAddEmpDialog = ref(false)
const showImportDialog = ref(false)
const importLoading = ref(false)
const importFile = ref(null)
const importResult = ref(null)
const editingEmployee = ref(null)

const handleFileChange = (file) => {
  importFile.value = file.raw
}

const downloadTemplate = () => {
  const csvContent = "\uFEFF姓名,部门,职位,电话号码\n张三,研发中心,Java开发工程师,13800000001\n李四,财务部,会计,13900000002\n"
  const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' })
  const link = document.createElement('a')
  link.href = URL.createObjectURL(blob)
  link.setAttribute('download', '员工批量导入模板.csv')
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
}

const handleImportSubmit = async () => {
  if (!importFile.value) {
    return ElMessage.warning('请选择需要上传的 Excel 或 CSV 文件')
  }
  importLoading.value = true
  try {
    const res = await importEmployees(importFile.value)
    if (res && res.data) {
      importResult.value = res.data
      ElMessage.success('批量导入处理完成')
      loadOrgData()
    }
  } catch (err) {
    ElMessage.error(err.response?.data?.message || '导入异常')
  } finally {
    importLoading.value = false
  }
}

const newDept = ref({
  name: '',
  code: '',
  leaderId: null,
  sort: 0
})

const newEmployee = ref({
  employeeNo: '',
  name: '',
  position: '',
  phone: '',
  hireDate: '',
  departmentId: null,
  status: 1
})

const role = ref(localStorage.getItem('role') || 'EMPLOYEE')
const userDepartmentId = ref(localStorage.getItem('departmentId') || null)

const isAdmin = computed(() => role.value === 'SUPER_ADMIN' || role.value === 'ADMIN')
const isDeptAdmin = computed(() => role.value === 'DEPT_MANAGER')
const isRegularUser = computed(() => role.value === 'EMPLOYEE')

const canEditEmployee = (employee) => {
  if (isAdmin.value) return true
  if (isDeptAdmin.value) return employee.departmentId === Number(userDepartmentId.value)
  return false
}

const canDeleteEmployee = (employee) => {
  if (isAdmin.value) return true
  if (isDeptAdmin.value) return employee.departmentId === Number(userDepartmentId.value)
  return false
}

const showAddEmployee = computed(() => isAdmin.value || isDeptAdmin.value)

const getDeptEmployeeCount = (deptId) => {
  return employees.value.filter(e => e.departmentId === deptId).length
}

const getLeaderName = (leaderId) => {
  if (!leaderId) return ''
  const leader = employees.value.find(e => e.id === leaderId)
  return leader ? leader.name : ''
}

const filteredEmployees = computed(() => {
  if (isAdmin.value) {
    if (!selectedDept.value) return []
    return employees.value.filter(e => e.departmentId === selectedDept.value.id)
  }
  return employees.value
})

const loadOrgData = async () => {
  try {
    if (isAdmin.value) {
      const dRes = await getDepartmentPage({ page: 1, size: 100 })
      if (dRes && dRes.data) depts.value = dRes.data.records || []

      const eRes = await getEmployeePage({ page: 1, size: 100 })
      if (eRes && eRes.data) employees.value = eRes.data.records || []
    } else {
      const myDeptRes = await getMyDepartment()
      if (myDeptRes && myDeptRes.data && myDeptRes.data.department) {
        selectedDept.value = myDeptRes.data.department
        employees.value = myDeptRes.data.employees || []
        currentView.value = 'employees'
      }
    }
  } catch (err) {
    console.error('加载组织数据失败', err)
  }
}

const goToEmployees = (dept) => {
  selectedDept.value = dept
  currentView.value = 'employees'
}

const goBack = () => {
  currentView.value = 'departments'
  selectedDept.value = null
}

const addDepartment = async () => {
  if (!newDept.value.name || !newDept.value.code) {
    ElMessage.warning('请填写部门名称和编码')
    return
  }
  try {
    await createDepartment(newDept.value)
    ElMessage.success('部门添加成功')
    showAddDeptDialog.value = false
    newDept.value = { name: '', code: '', leaderId: null, sort: 0 }
    loadOrgData()
  } catch (err) {
    ElMessage.error(err.response?.data?.message || '添加部门失败')
  }
}

const action = async (type, data) => {
  try {
    if (type === 'delete') {
      ElMessage.info('出于审计安全考虑，员工档案暂不支持直接删除')
    } else if (type === 'edit') {
      editingEmployee.value = { ...data }
      newEmployee.value = { 
        name: data.name, 
        position: data.position, 
        phone: data.phone, 
        hireDate: data.hireDate || '', 
        departmentId: data.departmentId,
        status: data.status !== undefined ? data.status : 1
      }
      showAddEmpDialog.value = true
    }
  } catch (err) {
    ElMessage.error('操作失败')
  }
}

const setDeptAdmin = async (employee) => {
  try {
    await createAccount(employee.id, { role: 'DEPT_MANAGER' })
    ElMessage.success(`${employee.name} 已成功设置为部门经理！`)
    loadOrgData()
  } catch (err) {
    ElMessage.error(err.response?.data?.message || '设置权限失败')
  }
}

const handleCloseEmpDialog = () => {
  showAddEmpDialog.value = false
  newEmployee.value = { employeeNo: '', name: '', position: '', phone: '', hireDate: '', departmentId: null, status: 1 }
  editingEmployee.value = null
}

const addEmployee = async () => {
  if (!newEmployee.value.name || !newEmployee.value.position) {
    ElMessage.warning('请填写姓名和职务')
    return
  }
  newEmployee.value.departmentId = selectedDept.value.id
  try {
    if (editingEmployee.value) {
      await updateEmployee(editingEmployee.value.id, newEmployee.value)
      ElMessage.success('员工信息更新成功')
    } else {
      const res = await createEmployee(newEmployee.value)
      if (res && res.data) {
        const emp = res.data
        await createAccount(emp.id, { role: 'EMPLOYEE' }).catch(() => null)
        ElMessage.success(`员工创建成功！工号：${emp.employeeNo}，初始账号密码：123456`)
      }
    }
    handleCloseEmpDialog()
    loadOrgData()
  } catch (err) {
    ElMessage.error('操作失败')
  }
}

onMounted(async () => {
  if (!localStorage.getItem('token')) return
  await loadOrgData()
  if (isRegularUser.value || isDeptAdmin.value) {
    const dept = depts.value.find(d => d.id === Number(userDepartmentId.value))
    if (dept) {
      selectedDept.value = dept
      currentView.value = 'employees'
    }
  }
  loading.value = false
})
</script>

<style scoped>
.org-container {
  max-width: 1400px;
}

.page-header {
  margin-bottom: 32px;
}

.header-top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
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

.dept-cards {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.dept-card {
  position: relative;
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-radius: 24px;
  padding: 32px 40px;
  border: 1px solid rgba(255, 255, 255, 0.5);
  box-shadow: 0 4px 30px rgba(0, 0, 0, 0.05);
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden;
}

.dept-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.1);
  border-color: rgba(102, 126, 234, 0.3);
}

.dept-card-bg {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.03) 0%, rgba(118, 75, 162, 0.02) 100%);
  opacity: 0;
  transition: opacity 0.3s ease;
}

.dept-card:hover .dept-card-bg {
  opacity: 1;
}

.dept-card-content {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  gap: 24px;
}

.dept-icon {
  width: 72px;
  height: 72px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32px;
  color: white;
  flex-shrink: 0;
}

.dept-info {
  flex: 1;
}

.dept-name {
  font-size: 24px;
  font-weight: 700;
  color: #1a1a2e;
  margin: 0 0 8px 0;
}

.dept-code,
.dept-employee-count {
  font-size: 14px;
  color: #64748b;
  margin: 0;
}

.dept-employee-count {
  margin-top: 4px;
}

.dept-meta {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-top: 12px;
}

.dept-status {
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
}

.dept-status.active {
  background: rgba(34, 197, 94, 0.1);
  color: #22c55e;
}

.dept-status.inactive {
  background: rgba(148, 163, 184, 0.1);
  color: #94a3b8;
}

.dept-leader {
  font-size: 14px;
  color: #64748b;
}

.dept-arrow {
  font-size: 20px;
  color: #94a3b8;
  transition: all 0.3s ease;
}

.dept-card:hover .dept-arrow {
  color: #667eea;
  transform: translateX(8px);
}

.employee-section {
  width: 100%;
}

.employee-card {
  width: 100%;
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-radius: 24px;
  border: 1px solid rgba(255, 255, 255, 0.5);
  box-shadow: 0 4px 30px rgba(0, 0, 0, 0.05);
  padding: 0;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24px 28px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);
}

.card-title {
  font-size: 18px;
  font-weight: 700;
  color: #1a1a2e;
}

.card-actions {
  display: flex;
  gap: 10px;
}

.table-wrapper {
  padding: 16px 28px 28px;
  overflow-x: auto;
}

.empty-state {
  text-align: center;
  padding: 60px 0;
  color: #94a3b8;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 16px;
}
</style>