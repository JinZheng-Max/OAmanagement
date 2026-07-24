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

    <div class="org-tree-container" v-if="currentView === 'departments'">
      <div class="tree-header">
        <h3>🏢 组织架构图</h3>
        <p>点击部门查看成员详情</p>
      </div>
      
      <div class="tree-layout">
        <div class="super-admin-node">
          <div class="super-admin-box">
            <div class="super-admin-icon">
              <el-icon><Star /></el-icon>
            </div>
            <div class="super-admin-info">
              <div class="super-admin-title">总管理员</div>
              <div class="super-admin-name">superadmin</div>
            </div>
          </div>
        </div>

        <div class="tree-connections">
          <svg class="connection-lines" viewBox="0 0 1200 300" preserveAspectRatio="none">
            <line 
              v-for="(dept, index) in depts" 
              :key="'line-' + dept.id"
              :x1="600" 
              :y1="60" 
              :x2="100 + (index % 3) * 380" 
              :y2="200" 
              stroke="#cbd5e1" 
              stroke-width="2"
              stroke-dasharray="5,5"
              class="connection-line"
            />
          </svg>
        </div>

        <div class="departments-row">
          <div 
            class="dept-tree-node" 
            v-for="(dept, index) in depts" 
            :key="dept.id"
            :style="{ animationDelay: index * 0.1 + 's' }"
          >
            <div class="dept-tree-box">
              <div class="dept-tree-icon">
                <el-icon><OfficeBuilding /></el-icon>
              </div>
              <div class="dept-tree-info">
                <div class="dept-tree-name">{{ dept.name }}</div>
                <div class="dept-tree-count">{{ getDeptEmployeeCount(dept.id) }} 人</div>
              </div>
              <div class="dept-tree-status" :class="dept.status === 1 ? 'active' : 'inactive'">
                {{ dept.status === 1 ? '启用' : '停用' }}
              </div>
              <div class="dept-tree-actions">
                <button class="dept-action-btn edit-btn" @click.stop="goToEmployees(dept)">
                  <el-icon><Edit /></el-icon>
                </button>
                <button class="dept-action-btn delete-btn" @click.stop="deleteDept(dept)">
                  <el-icon><Delete /></el-icon>
                </button>
              </div>
            </div>
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
            <el-button type="primary" size="small" @click="handleAddEmployee">
              <el-icon><Plus /></el-icon>
              添加员工
            </el-button>
          </div>
        </div>
        <div class="table-wrapper">
          <el-table :data="paginatedEmployees" stripe style="width: 100%">
            <el-table-column prop="employeeNo" label="工号" width="140" />
            <el-table-column prop="name" label="姓名" width="120" />
            <el-table-column prop="departmentName" label="所属部门" width="180" />
            <el-table-column prop="position" label="职务" width="160" />
            <el-table-column label="身份证号" width="180" v-if="isAdmin || isDeptAdmin">
              <template #default="scope">
                {{ scope.row.idNumber || scope.row.idCard || '-' }}
              </template>
            </el-table-column>
            <el-table-column label="邮箱" width="200" v-if="isAdmin || isDeptAdmin">
              <template #default="scope">
                {{ scope.row.email || '-' }}
              </template>
            </el-table-column>
            <el-table-column prop="hireDate" label="入职日期" width="150" />
            <el-table-column label="入职年限" width="120">
              <template #default="scope">
                {{ calculateTenure(scope.row.hireDate) }}
              </template>
            </el-table-column>
            <el-table-column label="账号角色" width="140">
              <template #default="scope">
                <el-tag v-if="scope.row.role === 'SUPER_ADMIN' || scope.row.role === 'ADMIN'" type="danger" effect="dark" size="small">
                  <el-icon><Star /></el-icon> 超级管理员
                </el-tag>
                <el-tag v-else-if="scope.row.role === 'DEPT_MANAGER' || scope.row.role === 'DEPARTMENT_ADMIN'" type="warning" effect="light" size="small">
                  <el-icon><UserFilled /></el-icon> 部门管理员
                </el-tag>
                <el-tag v-else-if="scope.row.hasAccount" type="info" size="small">普通员工</el-tag>
                <span v-else style="color: #94a3b8; font-size: 12px;">未开通账号</span>
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="100">
              <template #default="scope">
                <el-tag :type="scope.row.status === 1 ? 'success' : 'info'" size="small">
                  {{ scope.row.status === 1 ? '在职' : '离职' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column v-if="isAdmin || isDeptAdmin" label="操作" width="340">
              <template #default="scope">
                <el-button v-if="canEditEmployee(scope.row)" size="small" type="primary" @click="action('edit', scope.row)">
                  <el-icon><Edit /></el-icon>
                  编辑
                </el-button>
                <el-button v-if="canDeleteEmployee(scope.row)" size="small" type="danger" @click="action('delete', scope.row.id)">
                  <el-icon><Delete /></el-icon>
                  删除
                </el-button>
                <template v-if="isAdmin && (scope.row.role !== 'SUPER_ADMIN' && scope.row.role !== 'ADMIN')">
                  <el-button 
                    v-if="scope.row.role === 'DEPT_MANAGER' || scope.row.role === 'DEPARTMENT_ADMIN'" 
                    size="small" 
                    type="warning" 
                    plain 
                    @click="cancelDeptAdmin(scope.row)"
                  >
                    <el-icon><Remove /></el-icon>
                    取消部门管理员
                  </el-button>
                  <el-button 
                    v-else 
                    size="small" 
                    type="warning" 
                    @click="setDeptAdmin(scope.row)"
                  >
                    <el-icon><User /></el-icon>
                    设置部门管理员
                  </el-button>
                </template>
              </template>
            </el-table-column>
          </el-table>
        </div>
        <div class="pagination-container" v-if="filteredEmployees.length > 0">
          <el-pagination
            v-model:current-page="empCurrentPage"
            v-model:page-size="empPageSize"
            :page-sizes="[5, 10]"
            :total="filteredEmployees.length"
            layout="total, sizes, prev, pager, next, jumper"
            style="float: right; margin-top: 16px;"
          />
        </div>
        <div v-if="filteredEmployees.length === 0" class="empty-state">
          <el-icon class="empty-icon"><User /></el-icon>
          <p>该部门暂无员工</p>
        </div>
      </div>
    </div>

    <el-dialog title="添加部门" v-model="showAddDeptDialog" width="500px" append-to-body align-center>
      <el-form :model="newDept" label-width="100px">
        <el-form-item required>
          <template #label>部门名称 <span style="color:red">*</span></template>
          <el-input v-model="newDept.name" placeholder="请输入部门名称" />
        </el-form-item>
        <el-form-item required>
          <template #label>部门编码 <span style="color:red">*</span></template>
          <el-input v-model="newDept.code" placeholder="请输入部门编码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddDeptDialog = false">取消</el-button>
        <el-button type="primary" @click="addDepartment">确认添加</el-button>
      </template>
    </el-dialog>

    <div class="custom-modal-overlay" v-if="showAddEmpDialog" @click="handleCloseEmpDialog">
      <div class="custom-modal" @click.stop>
        <button class="modal-close" @click="handleCloseEmpDialog">
          <el-icon><CircleClose /></el-icon>
        </button>
        <div class="modal-header">
          <div class="modal-title">
            <div class="title-icon">
              <el-icon><User /></el-icon>
            </div>
            <div>
              <h3>{{ editingEmployee ? '编辑员工信息' : '添加新员工' }}</h3>
              <p>{{ editingEmployee ? '修改员工个人资料' : '录入新员工基本信息' }}</p>
            </div>
          </div>
        </div>
        <div class="modal-body">
          <div class="form-grid">
            <div class="form-item">
              <label class="form-label">员工工号 <span class="required">*</span></label>
              <el-input v-model="newEmployee.employeeNo" placeholder="请输入员工工号" size="large" :disabled="!!editingEmployee" />
            </div>
            <div class="form-item">
              <label class="form-label">员工姓名 <span class="required">*</span></label>
              <el-input v-model="newEmployee.name" placeholder="请输入员工姓名" size="large" />
            </div>
            <div class="form-item">
              <label class="form-label">职务 <span class="required">*</span></label>
              <el-input v-model="newEmployee.position" placeholder="请输入职务" size="large" />
            </div>
            <div class="form-item">
              <label class="form-label">手机号 <span class="required">*</span></label>
              <el-input v-model="newEmployee.phone" placeholder="请输入手机号" size="large" />
            </div>
            <div class="form-item">
              <label class="form-label">身份证号 <span class="required">*</span></label>
              <el-input v-model="newEmployee.idNumber" placeholder="请输入身份证号" size="large" />
            </div>
            <div class="form-item">
              <label class="form-label">邮箱 <span class="required">*</span></label>
              <el-input v-model="newEmployee.email" placeholder="请输入邮箱" size="large" />
            </div>
            <div class="form-item">
              <label class="form-label">入职日期 <span class="required">*</span></label>
              <el-date-picker v-model="newEmployee.hireDate" type="date" value-format="YYYY-MM-DD" placeholder="选择入职日期" size="large" style="width: 100%" />
            </div>
            <div class="form-item">
              <label class="form-label">入职年限</label>
              <el-input :value="calculateTenure(newEmployee.hireDate)" placeholder="自动计算" size="large" disabled />
            </div>
            <div class="form-item" v-if="isAdmin">
              <label class="form-label">所属部门 <span class="required">*</span></label>
              <el-select v-model="newEmployee.departmentId" placeholder="请选择部门" size="large" style="width: 100%">
                <el-option v-for="dept in depts" :key="dept.id" :label="dept.name" :value="dept.id" />
              </el-select>
            </div>
            <div class="form-item" v-if="editingEmployee">
              <label class="form-label">状态 <span class="required">*</span></label>
              <el-select v-model="newEmployee.status" placeholder="请选择状态" size="large" style="width: 100%">
                <el-option label="在职" :value="1" />
                <el-option label="离职" :value="0" />
              </el-select>
            </div>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn-cancel" @click="handleCloseEmpDialog">取消</button>
          <button class="btn-primary" @click="addEmployee">
            {{ editingEmployee ? '保存修改' : '确认添加' }}
          </button>
        </div>
      </div>
    </div>

    <!-- ===== 批量导入员工 弹窗 ===== -->
    <el-dialog title="批量导入员工" v-model="showImportDialog" width="600px" append-to-body align-center :close-on-click-modal="false">
      <div class="import-dialog-body">
        <div class="template-section" style="margin-bottom: 20px; padding: 14px 18px; background: rgba(102, 126, 234, 0.05); border-radius: 12px; border: 1px dashed rgba(102, 126, 234, 0.3);">
          <p style="margin-bottom: 6px; font-weight: 600; color: #1a1a2e;">第 1 步：下载标准导入模版</p>
          <p style="font-size: 13px; color: #64748b; margin-bottom: 10px;">
            模版包含【工号、姓名、部门、职位、电话号码、身份证号、邮箱】列属性。工号可自定义填写，若留空系统将自动生成。
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
import { Plus, Edit, Delete, ArrowLeft, ArrowRight, OfficeBuilding, User, Upload, Download, Star, CircleClose, UserFilled, Remove } from '@element-plus/icons-vue'
import {
  getDepartmentPage,
  createDepartment,
  deleteDepartment,
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

const empCurrentPage = ref(1)
const empPageSize = ref(5)

const handleFileChange = (file) => {
  importFile.value = file.raw
}

const downloadTemplate = () => {
  const csvContent = "\uFEFF工号,姓名,部门,职位,电话号码,身份证号,邮箱\nEMP0001,张三,研发中心,Java开发工程师,13800000001,110101199001011234,zhangsan@company.com\nEMP0002,李四,财务部,会计,13900000002,110101199202022345,lisi@company.com\n"
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
  code: ''
})

const newEmployee = ref({
  employeeNo: '',
  name: '',
  position: '',
  phone: '',
  hireDate: '',
  departmentId: null,
  status: 1,
  idCard: '',
  email: ''
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

const calculateTenure = (hireDate) => {
  if (!hireDate) return ''
  const hire = new Date(hireDate)
  const now = new Date()
  let years = now.getFullYear() - hire.getFullYear()
  const months = now.getMonth() - hire.getMonth()
  if (months < 0 || (months === 0 && now.getDate() < hire.getDate())) {
    years--
  }
  return years >= 0 ? years + ' 年' : ''
}

const filteredEmployees = computed(() => {
  if (isAdmin.value) {
    if (!selectedDept.value) return []
    return employees.value.filter(e => e.departmentId === selectedDept.value.id)
  }
  return employees.value
})

const paginatedEmployees = computed(() => {
  const start = (empCurrentPage.value - 1) * empPageSize.value
  const end = start + empPageSize.value
  return filteredEmployees.value.slice(start, end)
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

const deleteDept = async (dept) => {
  try {
    await deleteDepartment(dept.id)
    ElMessage.success('部门删除成功')
    loadOrgData()
  } catch (err) {
    ElMessage.error(err.response?.data?.message || '删除部门失败')
  }
}

const action = async (type, data) => {
  try {
    if (type === 'delete') {
      ElMessage.info('出于审计安全考虑，员工档案暂不支持直接删除')
    } else if (type === 'edit') {
      editingEmployee.value = { ...data }
      newEmployee.value = { 
        employeeNo: data.employeeNo || '',
        name: data.name, 
        position: data.position, 
        phone: data.phone, 
        hireDate: data.hireDate || '', 
        departmentId: data.departmentId,
        status: data.status !== undefined ? data.status : 1,
        idNumber: data.idNumber || data.idCard || '',
        email: data.email || ''
      }
      showAddEmpDialog.value = true
    }
  } catch (err) {
    ElMessage.error('操作失败')
  }
}

const setDeptAdmin = async (employee) => {
  try {
    const res = await createAccount(employee.id, { role: 'DEPT_MANAGER' })
    if (res && res.data === 'SUCCESS_ROLE_UPDATED') {
      ElMessage.success(`已将员工【${employee.name}】的系统账号权限升为部门经理！`)
    } else {
      ElMessage.success(`${employee.name} 已成功设置为部门经理！初始密码：123456`)
    }
    loadOrgData()
  } catch (err) {
    ElMessage.error(err.response?.data?.message || '设置权限失败')
  }
}

const cancelDeptAdmin = async (employee) => {
  try {
    await createAccount(employee.id, { role: 'EMPLOYEE' })
    ElMessage.success(`已取消员工【${employee.name}】的部门管理员权限！`)
    loadOrgData()
  } catch (err) {
    ElMessage.error(err.response?.data?.message || '取消权限失败')
  }
}

const handleAddEmployee = () => {
  editingEmployee.value = null
  const autoEmpNo = 'EMP' + String(Date.now()).substring(7)
  newEmployee.value = { employeeNo: autoEmpNo, name: '', position: '', phone: '', hireDate: '', departmentId: null, status: 1, idNumber: '', email: '' }
  showAddEmpDialog.value = true
}

const handleCloseEmpDialog = () => {
  showAddEmpDialog.value = false
  newEmployee.value = { employeeNo: '', name: '', position: '', phone: '', hireDate: '', departmentId: null, status: 1, idNumber: '', email: '' }
  editingEmployee.value = null
}

const addEmployee = async () => {
  if (!newEmployee.value.employeeNo) {
    ElMessage.warning('请填写员工工号')
    return
  }
  if (!newEmployee.value.name) {
    ElMessage.warning('请填写员工姓名')
    return
  }
  if (!newEmployee.value.position) {
    ElMessage.warning('请填写职务')
    return
  }
  if (!newEmployee.value.phone) {
    ElMessage.warning('请填写手机号')
    return
  }
  if (!newEmployee.value.idNumber) {
    ElMessage.warning('请填写身份证号')
    return
  }
  if (!newEmployee.value.email) {
    ElMessage.warning('请填写邮箱')
    return
  }
  if (!newEmployee.value.hireDate) {
    ElMessage.warning('请选择入职日期')
    return
  }
  if (!isAdmin.value || !editingEmployee.value) {
    newEmployee.value.departmentId = selectedDept.value.id
  }
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
  width: 100%;
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

.org-tree-container {
  width: 100%;
}

.tree-header {
  text-align: center;
  margin-bottom: 40px;
}

.tree-header h3 {
  font-size: 28px;
  font-weight: 800;
  color: #1a1a2e;
  margin: 0 0 8px 0;
}

.tree-header p {
  font-size: 14px;
  color: #64748b;
  margin: 0;
}

.tree-layout {
  position: relative;
  min-height: 400px;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.super-admin-node {
  margin-bottom: 80px;
  animation: fadeInDown 0.6s ease-out;
}

@keyframes fadeInDown {
  from {
    opacity: 0;
    transform: translateY(-30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.super-admin-box {
  display: flex;
  align-items: center;
  gap: 16px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 24px 40px;
  border-radius: 20px;
  box-shadow: 0 10px 40px rgba(102, 126, 234, 0.4);
  transition: all 0.3s ease;
}

.super-admin-box:hover {
  transform: scale(1.02);
  box-shadow: 0 15px 50px rgba(102, 126, 234, 0.5);
}

.super-admin-icon {
  width: 56px;
  height: 56px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  color: #fff;
}

.super-admin-info {
  display: flex;
  flex-direction: column;
}

.super-admin-title {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.8);
  font-weight: 500;
}

.super-admin-name {
  font-size: 20px;
  color: #fff;
  font-weight: 700;
}

.tree-connections {
  position: absolute;
  top: 80px;
  left: 0;
  right: 0;
  height: 200px;
  pointer-events: none;
  z-index: 0;
}

.connection-lines {
  width: 100%;
  height: 200px;
}

.connection-line {
  animation: drawLine 0.8s ease-out forwards;
  stroke-dashoffset: 1000;
}

@keyframes drawLine {
  to {
    stroke-dashoffset: 0;
  }
}

.departments-row {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 32px;
  z-index: 1;
}

.dept-tree-node {
  animation: fadeInUp 0.6s ease-out forwards;
  opacity: 0;
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.dept-tree-box {
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(20px);
  border-radius: 16px;
  padding: 24px 32px;
  border: 2px solid rgba(102, 126, 234, 0.15);
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.06);
  cursor: pointer;
  transition: all 0.3s ease;
  min-width: 220px;
  text-align: center;
}

.dept-tree-box:hover {
  transform: translateY(-8px);
  border-color: rgba(102, 126, 234, 0.4);
  box-shadow: 0 12px 40px rgba(102, 126, 234, 0.2);
}

.dept-tree-icon {
  width: 48px;
  height: 48px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  color: white;
  margin: 0 auto 16px;
}

.dept-tree-info {
  margin-bottom: 12px;
}

.dept-tree-name {
  font-size: 18px;
  font-weight: 700;
  color: #1a1a2e;
  margin: 0 0 6px 0;
}

.dept-tree-count {
  font-size: 13px;
  color: #64748b;
  margin: 0;
}

.dept-tree-status {
  display: inline-block;
  padding: 4px 14px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
}

.dept-tree-status.active {
  background: rgba(34, 197, 94, 0.1);
  color: #22c55e;
}

.dept-tree-status.inactive {
  background: rgba(148, 163, 184, 0.1);
  color: #94a3b8;
}

.dept-tree-actions {
  display: flex;
  gap: 8px;
  justify-content: center;
  margin-top: 16px;
}

.dept-action-btn {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s ease;
}

.dept-action-btn.edit-btn {
  background: rgba(102, 126, 234, 0.1);
  color: #667eea;
}

.dept-action-btn.edit-btn:hover {
  background: rgba(102, 126, 234, 0.2);
  transform: scale(1.1);
}

.dept-action-btn.delete-btn {
  background: rgba(239, 68, 68, 0.1);
  color: #ef4444;
}

.dept-action-btn.delete-btn:hover {
  background: rgba(239, 68, 68, 0.2);
  transform: scale(1.1);
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
  width: 100%;
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

.custom-modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(8px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
  animation: fadeIn 0.3s ease;
}

@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

.custom-modal {
  width: 620px;
  background: #ffffff;
  border-radius: 24px;
  box-shadow: 0 25px 80px rgba(0, 0, 0, 0.18);
  overflow: hidden;
  animation: slideUp 0.35s ease;
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(30px) scale(0.95);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 28px 32px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.modal-title {
  display: flex;
  align-items: center;
  gap: 16px;
}

.title-icon {
  width: 48px;
  height: 48px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  color: #ffffff;
}

.modal-title h3 {
  font-size: 22px;
  font-weight: 700;
  color: #ffffff;
  margin: 0 0 4px 0;
}

.modal-title p {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.8);
  margin: 0;
}

.modal-close {
  width: 40px;
  height: 40px;
  background: #f1f5f9;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  color: #64748b;
  cursor: pointer;
  transition: all 0.3s ease;
  z-index: 10000;
  position: absolute;
  top: 16px;
  right: 16px;
}

.modal-close:hover {
  background: #e2e8f0;
  color: #334155;
  transform: scale(1.1);
}

.modal-body {
  padding: 32px;
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
}

.form-item {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.form-label {
  font-size: 14px;
  font-weight: 600;
  color: #334155;
}

.form-label .required {
  color: #ef4444;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 16px;
  padding: 20px 32px 28px;
  background: #f8fafc;
  border-top: 1px solid #f1f5f9;
}

.btn-cancel {
  padding: 12px 32px;
  background: #e2e8f0;
  border: none;
  border-radius: 12px;
  font-size: 15px;
  font-weight: 600;
  color: #64748b;
  cursor: pointer;
  transition: all 0.3s ease;
}

.btn-cancel:hover {
  background: #cbd5e1;
  transform: translateY(-2px);
}

.btn-primary {
  padding: 12px 32px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  border-radius: 12px;
  font-size: 15px;
  font-weight: 600;
  color: #ffffff;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 4px 15px rgba(102, 126, 234, 0.35);
}

.btn-primary:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(102, 126, 234, 0.45);
}

.btn-primary:active {
  transform: translateY(0);
}
</style>