<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../../stores/auth'
import { getMyProfile, type UserProfile } from '../../api/auth'
import { updateProfile, changePassword } from '../../api/employee'

const auth = useAuthStore()

// ---- 个人信息 ----
const loading = ref(true)
const profile = ref<UserProfile | null>(null)

/** 员工信息快捷引用 */
const emp = computed(() => profile.value?.employee)

/** 编辑模式切换 */
const editing = ref(false)

/** 编辑表单 */
const editForm = ref({ name: '', phone: '' })
const saving = ref(false)

/** 修改密码弹窗 */
const pwdDialogVisible = ref(false)
const pwdForm = ref({ oldPassword: '', newPassword: '', confirmPassword: '' })
const changingPwd = ref(false)

// ---- 加载数据 ----
async function loadProfile() {
  loading.value = true
  try {
    profile.value = await getMyProfile()
    if (emp.value) {
      editForm.value = { name: emp.value.name || '', phone: emp.value.phone || '' }
    }
  } catch { ElMessage.error('加载个人信息失败') }
  finally { loading.value = false }
}

onMounted(loadProfile)

// ---- 保存个人信息 ----
async function saveProfile() {
  saving.value = true
  try {
    const result = await updateProfile({
      name: editForm.value.name || undefined,
      phone: editForm.value.phone || undefined,
    })
    if (profile.value?.employee) {
      profile.value.employee.name = result.name
      profile.value.employee.phone = result.phone
    }
    editing.value = false
    ElMessage.success('修改成功')
  } catch (err: any) {
    ElMessage.error(err?.response?.data?.message || '保存失败')
  } finally { saving.value = false }
}

function cancelEdit() {
  if (emp.value) {
    editForm.value = { name: emp.value.name || '', phone: emp.value.phone || '' }
  }
  editing.value = false
}

// ---- 修改密码 ----
async function handleChangePassword() {
  if (pwdForm.value.newPassword !== pwdForm.value.confirmPassword) {
    ElMessage.error('两次输入的新密码不一致')
    return
  }
  if (pwdForm.value.newPassword.length < 8) {
    ElMessage.error('新密码至少8位')
    return
  }
  changingPwd.value = true
  try {
    await changePassword(pwdForm.value.oldPassword, pwdForm.value.newPassword)
    ElMessage.success('密码修改成功，请重新登录')
    pwdDialogVisible.value = false
    pwdForm.value = { oldPassword: '', newPassword: '', confirmPassword: '' }
    // 退出重新登录
    await auth.logout()
    window.location.href = '/login'
  } catch (err: any) {
    ElMessage.error(err?.response?.data?.message || '密码修改失败')
  } finally { changingPwd.value = false }
}
</script>

<template>
  <div class="profile-page">
    <!-- 加载中 -->
    <div v-if="loading" class="loading-wrapper">
      <el-skeleton :rows="6" animated />
    </div>

    <template v-else-if="profile && emp">
      <!-- ===== 头部：头像 + 姓名 + 身份 ===== -->
      <div class="profile-header">
        <div class="avatar-wrapper">
          <div class="avatar-circle">{{ emp.name?.charAt(0) || '?' }}</div>
        </div>
        <div class="header-info">
          <h1>{{ emp.name }}</h1>
          <div class="header-tags">
            <el-tag>{{ emp.employeeNo }}</el-tag>
            <el-tag :type="profile.role === 'SUPER_ADMIN' ? 'danger' : 'info'">
              {{ profile.role === 'SUPER_ADMIN' ? '总管理员' : profile.role === 'DEPT_MANAGER' ? '部门经理' : '普通员工' }}
            </el-tag>
            <el-tag :type="emp.status === 1 ? 'success' : 'danger'">
              {{ emp.status === 1 ? '在职' : '离职' }}
            </el-tag>
          </div>
        </div>
      </div>

      <!-- ===== 个人信息卡片 ===== -->
      <el-card class="info-card" shadow="never">
        <template #header>
          <div class="card-header">
            <span><el-icon><User /></el-icon> 个人信息</span>
            <div>
              <el-button v-if="!editing" size="small" type="primary" plain @click="editing = true">
                <el-icon><Edit /></el-icon> 编辑
              </el-button>
            </div>
          </div>
        </template>

        <!-- 查看模式 -->
        <el-descriptions v-if="!editing" :column="2" border>
          <el-descriptions-item label="工号" :span="1">{{ emp.employeeNo }}</el-descriptions-item>
          <el-descriptions-item label="姓名" :span="1">{{ emp.name }}</el-descriptions-item>
          <el-descriptions-item label="部门" :span="1">
            <el-tag size="small" type="info">{{ emp.departmentName || '-' }}</el-tag>
            <span class="field-lock">不可修改</span>
          </el-descriptions-item>
          <el-descriptions-item label="职位" :span="1">
            {{ emp.position || '-' }}
            <span class="field-lock">不可修改</span>
          </el-descriptions-item>
          <el-descriptions-item label="手机号" :span="1">{{ emp.phone || '-' }}</el-descriptions-item>
          <el-descriptions-item label="入职日期" :span="1">{{ emp.hireDate || '-' }}</el-descriptions-item>
        </el-descriptions>

        <!-- 编辑模式 -->
        <el-form v-else :model="editForm" label-width="100px" class="edit-form">
          <el-form-item label="工号">
            <el-input :model-value="emp.employeeNo" disabled />
          </el-form-item>
          <el-form-item label="姓名">
            <el-input v-model="editForm.name" placeholder="请输入姓名" />
          </el-form-item>
          <el-form-item label="部门">
            <el-input :model-value="emp.departmentName || '-'" disabled />
            <span class="field-lock">不可修改</span>
          </el-form-item>
          <el-form-item label="职位">
            <el-input :model-value="emp.position || '-'" disabled />
            <span class="field-lock">不可修改</span>
          </el-form-item>
          <el-form-item label="手机号">
            <el-input v-model="editForm.phone" placeholder="请输入11位手机号" maxlength="11" />
          </el-form-item>
          <el-form-item label="入职日期">
            <el-input :model-value="emp.hireDate || '-'" disabled />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="saving" @click="saveProfile">
              <el-icon><Check /></el-icon> 保存
            </el-button>
            <el-button @click="cancelEdit">取消</el-button>
          </el-form-item>
        </el-form>
      </el-card>

      <!-- ===== 修改密码卡片 ===== -->
      <el-card class="info-card" shadow="never">
        <template #header>
          <div class="card-header">
            <span><el-icon><Lock /></el-icon> 账号安全</span>
          </div>
        </template>
        <div class="security-row">
          <div class="security-info">
            <strong>登录密码</strong>
            <p>建议定期修改密码，密码需包含字母和数字，至少8位</p>
          </div>
          <el-button type="primary" plain @click="pwdDialogVisible = true">
            <el-icon><Edit /></el-icon> 修改密码
          </el-button>
        </div>
      </el-card>
    </template>

    <!-- 无员工信息 -->
    <el-empty v-else description="未关联员工信息" />

    <!-- ===== 修改密码弹窗 ===== -->
    <el-dialog v-model="pwdDialogVisible" title="修改密码" width="420px" :close-on-click-modal="false">
      <el-form :model="pwdForm" label-width="100px">
        <el-form-item label="当前密码">
          <el-input v-model="pwdForm.oldPassword" type="password" show-password placeholder="输入当前密码" />
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="pwdForm.newPassword" type="password" show-password placeholder="至少8位，字母+数字" />
        </el-form-item>
        <el-form-item label="确认新密码">
          <el-input v-model="pwdForm.confirmPassword" type="password" show-password placeholder="再次输入新密码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pwdDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="changingPwd" @click="handleChangePassword">确认修改</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.profile-page {
  padding: 24px 32px;
  max-width: 800px;
  margin: 0 auto;
}

.loading-wrapper {
  padding: 40px;
}

/* ---- 头部 ---- */
.profile-header {
  display: flex;
  align-items: center;
  gap: 24px;
  margin-bottom: 24px;
  padding: 28px 32px;
  background: linear-gradient(135deg, #409eff15, #337ecc08);
  border-radius: 12px;
  border: 1px solid #e8edf3;
}

.avatar-circle {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  background: linear-gradient(135deg, #409eff, #337ecc);
  color: #fff;
  font-size: 28px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.header-info h1 {
  margin: 0 0 8px;
  font-size: 22px;
  color: #1d2129;
}

.header-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

/* ---- 卡片 ---- */
.info-card {
  margin-bottom: 20px;
  border-radius: 10px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 15px;
  font-weight: 600;
}

.card-header span {
  display: flex;
  align-items: center;
  gap: 6px;
}

/* ---- 不可修改标记 ---- */
.field-lock {
  margin-left: 6px;
  font-size: 12px;
  color: #999;
}

/* ---- 编辑表单 ---- */
.edit-form {
  max-width: 500px;
}

/* ---- 安全设置 ---- */
.security-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.security-info strong {
  font-size: 14px;
  color: #1d2129;
}

.security-info p {
  margin: 4px 0 0;
  font-size: 13px;
  color: #86909c;
}
</style>
