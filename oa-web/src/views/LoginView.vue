<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../stores/auth'

const form = reactive({ username: '', password: '' })
const loading = ref(false)
const auth = useAuthStore(); const router = useRouter(); const route = useRoute()
async function submit() {
  if (!form.username || !form.password) { ElMessage.warning('请输入用户名和密码'); return }
  loading.value = true
  try {
    await auth.login(form.username.trim(), form.password)
    await router.replace(typeof route.query.redirect === 'string' ? route.query.redirect : '/')
  } catch (error: any) { ElMessage.error(error.response?.data?.message ?? '登录失败，请稍后重试') }
  finally { loading.value = false }
}
</script>
<template>
  <main class="login-page">
    <section class="login-card">
      <p class="eyebrow">SMART WORKPLACE</p><h1>智办 AI OA</h1><p class="subtitle">统一、安全、可追踪的企业协作入口</p>
      <el-form @submit.prevent="submit">
        <el-form-item><el-input v-model="form.username" data-testid="username" autocomplete="username" placeholder="用户名" /></el-form-item>
        <el-form-item><el-input v-model="form.password" data-testid="password" type="password" autocomplete="current-password" show-password placeholder="密码" @keyup.enter="submit" /></el-form-item>
        <el-button data-testid="login" type="primary" native-type="submit" :loading="loading">登录</el-button>
      </el-form>
    </section>
  </main>
</template>
