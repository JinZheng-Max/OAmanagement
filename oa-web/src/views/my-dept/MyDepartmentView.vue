<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getMyDepartment, type MyDepartmentResponse } from '../../api/department'

import { useAuthStore } from '../../stores/auth'
import { createAccount, type EmployeeInfo } from '../../api/employee'

const auth = useAuthStore()
const loading = ref(true)
const data = ref<MyDepartmentResponse | null>(null)

// 部门管理员开通账号
const accountDialogVisible = ref(false)
const currentEmp = ref<any | null>(null)
const accountForm = ref({ username: '', role: 'EMPLOYEE' })
const accountResult = ref('')
const formLoading = ref(false)

function openAccountModal(emp: any) {
  currentEmp.value = emp
  accountForm.value = { username: '', role: 'EMPLOYEE' }
  accountResult.value = ''
  accountDialogVisible.value = true
}

async function handleCreateAccount() {
  if (!currentEmp.value) return
  formLoading.value = true
  try {
    const password = await createAccount(currentEmp.value.id, {
      username: accountForm.value.username || undefined,
      role: accountForm.value.role || 'EMPLOYEE',
    })
    accountResult.value = password
    ElMessage.success('账号开通成功')
    // 重新拉取部门数据
    data.value = await getMyDepartment()
  } catch (err: any) {
    ElMessage.error(err?.response?.data?.message || '开通账号失败')
  } finally {
    formLoading.value = false
  }
}

onMounted(async () => {
  try {
    data.value = await getMyDepartment()
  } catch (err: any) {
    ElMessage.error(err?.response?.data?.message || '加载部门信息失败')
  } finally { loading.value = false }
})
</script>

<template>
  <div class="my-dept-page">
    <div v-if="loading" class="loading-wrapper">
      <el-skeleton :rows="5" animated />
    </div>

    <template v-else-if="data">
      <!-- ===== 部门概览卡片 ===== -->
      <div class="overview-card">
        <div class="overview-left">
          <div class="dept-icon">{{ data.department.name?.charAt(0) || '?' }}</div>
          <div class="dept-info">
            <h1>{{ data.department.name }}</h1>
            <span class="dept-code">{{ data.department.code }}</span>
          </div>
        </div>
        <div class="overview-right">
          <div class="stat-item">
            <span class="stat-num">{{ data.totalCount }}</span>
            <span class="stat-label">成员</span>
          </div>
          <div class="stat-divider" />
          <div class="stat-item">
            <span class="stat-num">{{ Object.keys(data.positionCounts).length }}</span>
            <span class="stat-label">岗位</span>
          </div>
        </div>
      </div>

      <!-- ===== 负责人信息 ===== -->
      <el-card v-if="data.leader" class="section-card" shadow="never">
        <template #header><span class="section-title">👤 部门负责人</span></template>
        <div class="leader-row">
          <div class="leader-avatar">{{ data.leader.name?.charAt(0) || '?' }}</div>
          <div class="leader-detail">
            <div class="leader-name">{{ data.leader.name }}</div>
            <div class="leader-meta">{{ data.leader.position || '-' }} · {{ data.leader.employeeNo }}</div>
          </div>
          <div class="leader-contact">
            <div class="contact-item">
              <el-icon><Phone /></el-icon>
              <span>{{ data.leader.phone || '未留电话' }}</span>
            </div>
          </div>
        </div>
      </el-card>

      <!-- ===== 岗位分布 ===== -->
      <el-card class="section-card" shadow="never">
        <template #header><span class="section-title">📊 岗位分布</span></template>
        <div class="position-grid">
          <div v-for="(count, pos) in data.positionCounts" :key="pos" class="pos-card">
            <div class="pos-name">{{ pos }}</div>
            <div class="pos-count">{{ count }}人</div>
          </div>
        </div>
      </el-card>

      <!-- ===== 全部成员 ===== -->
      <el-card class="section-card" shadow="never">
        <template #header><span class="section-title">👥 全部成员（{{ data.totalCount }}人）</span></template>
        <div class="member-list">
          <div v-for="emp in data.employees" :key="emp.id" class="member-item">
            <div class="m-avatar">{{ emp.name?.charAt(0) || '?' }}</div>
            <div class="m-info">
              <div class="m-name">
                {{ emp.name }}
                <el-tag v-if="emp.id === data.leader?.id" size="small" type="primary" class="m-tag">负责人</el-tag>
              </div>
              <div class="m-meta">{{ emp.position || '-' }} · 工号: {{ emp.employeeNo }}</div>
            </div>
            <div class="m-phone">{{ emp.phone || '-' }}</div>
            <div v-if="auth.isDeptManager" class="m-actions">
              <el-tag v-if="emp.hasAccount" type="success" size="small">已开通账号</el-tag>
              <el-button v-else type="primary" link size="small" @click="openAccountModal(emp)">开通账号</el-button>
            </div>
          </div>
        </div>
      </el-card>
    </template>

    <el-empty v-else description="暂无部门信息" />

    <!-- ===== 部门管理员开通账号弹窗 ===== -->
    <el-dialog v-model="accountDialogVisible" title="开通系统账号" width="440px">
      <div v-if="!accountResult">
        <p style="margin-bottom: 16px;">
          为本部门成员 <strong>{{ currentEmp?.name }}</strong> 开通系统账号
        </p>
        <el-form :model="accountForm" label-width="90px">
          <el-form-item label="用户名">
            <el-input v-model="accountForm.username" placeholder="不填则默认使用工号" />
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
.my-dept-page { padding: 24px 32px; max-width: 800px; margin: 0 auto; }
.loading-wrapper { padding: 40px; }

/* 概览卡片 */
.overview-card {
  display: flex; justify-content: space-between; align-items: center;
  padding: 28px 32px; margin-bottom: 20px;
  background: linear-gradient(135deg, #409eff15, #337ecc08);
  border-radius: 12px; border: 1px solid #e8edf3;
}
.overview-left { display: flex; align-items: center; gap: 18px; }
.dept-icon {
  width: 56px; height: 56px; border-radius: 14px;
  background: linear-gradient(135deg, #409eff, #337ecc);
  color: #fff; font-size: 24px; font-weight: 700;
  display: flex; align-items: center; justify-content: center; flex-shrink: 0;
}
.dept-info h1 { margin: 0 0 4px; font-size: 22px; color: #1d2129; }
.dept-code { font-size: 13px; color: #86909c; }
.overview-right { display: flex; align-items: center; gap: 20px; }
.stat-item { text-align: center; }
.stat-num { display: block; font-size: 24px; font-weight: 700; color: #409eff; }
.stat-label { font-size: 12px; color: #86909c; }
.stat-divider { width: 1px; height: 40px; background: #e0e0e0; }

/* 卡片通用 */
.section-card { margin-bottom: 20px; border-radius: 10px; }
.section-title { font-size: 15px; font-weight: 600; }

/* 负责人 */
.leader-row { display: flex; align-items: center; gap: 18px; }
.leader-avatar {
  width: 48px; height: 48px; border-radius: 50%;
  background: linear-gradient(135deg, #409eff, #79bbff);
  color: #fff; font-size: 18px; font-weight: 600;
  display: flex; align-items: center; justify-content: center; flex-shrink: 0;
}
.leader-detail { flex: 1; }
.leader-name { font-size: 16px; font-weight: 600; color: #1d2129; }
.leader-meta { font-size: 13px; color: #86909c; margin-top: 2px; }
.leader-contact { text-align: right; }
.contact-item { display: flex; align-items: center; gap: 6px; color: #409eff; font-size: 14px; }

/* 岗位分布 */
.position-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(160px, 1fr)); gap: 10px; }
.pos-card {
  text-align: center; padding: 14px 12px;
  background: #f7f8fa; border-radius: 8px; border: 1px solid #ebeef5;
}
.pos-name { font-size: 13px; color: #4e5969; margin-bottom: 4px; }
.pos-count { font-size: 18px; font-weight: 700; color: #409eff; }

/* 全部成员 */
.member-list { display: grid; grid-template-columns: 1fr; gap: 6px; }
.member-item {
  display: flex; align-items: center; gap: 14px;
  padding: 10px 14px; border-radius: 8px;
  border: 1px solid #f0f2f5; transition: all 0.2s;
}
.member-item:hover { border-color: #409eff40; background: #f7f8fa; }
.m-avatar {
  width: 36px; height: 36px; border-radius: 50%;
  background: #e8edf3; color: #4e5969; font-size: 14px; font-weight: 600;
  display: flex; align-items: center; justify-content: center; flex-shrink: 0;
}
.m-info { flex: 1; min-width: 0; }
.m-name { font-size: 14px; font-weight: 500; color: #1d2129; display: flex; align-items: center; gap: 6px; }
.m-tag { font-size: 11px; }
.m-meta { font-size: 12px; color: #86909c; margin-top: 2px; }
.m-phone { font-size: 13px; color: #4e5969; white-space: nowrap; }
</style>
