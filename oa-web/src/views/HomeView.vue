<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
const auth = useAuthStore(); const router = useRouter()
async function signOut() { await auth.logout(); await router.replace('/login') }
</script>
<template><main class="home"><header><div><strong>智办 AI OA</strong><span>模块化工作台</span></div><el-button @click="signOut">退出</el-button></header><section><h1>欢迎，{{ auth.user?.username }}</h1><p>认证基线已就绪，组织管理模块将在通过门禁后接入。</p></section></main></template>
