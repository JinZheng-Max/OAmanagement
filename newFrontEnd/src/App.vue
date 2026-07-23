<template>
  <router-view v-if="isLoginPage" />
  <div v-else class="main-layout">
    <Sidebar />
    <div class="main-content">
      <Header />
      <main class="view-body">
        <router-view v-slot="{ Component }">
          <transition :name="transitionName" mode="out-in">
            <component :is="Component" :key="route.path" />
          </transition>
        </router-view>
      </main>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import Sidebar from './components/Sidebar.vue'
import Header from './components/Header.vue'

const route = useRoute()
const isLoginPage = computed(() => route.path === '/login')
const transitionName = ref('slide-down')
const prevPath = ref(route.path)

const routeOrder = ['/dashboard','/ai-assistant','/attendance','/leave','/content','/org']

watch(() => route.path, (newPath) => {
  const prevIndex = routeOrder.indexOf(prevPath.value)
  const currentIndex = routeOrder.indexOf(newPath)
  transitionName.value = currentIndex > prevIndex ? 'slide-up' : 'slide-down'
  prevPath.value = newPath
}, { immediate: false })
</script>

<style scoped>
.main-layout {
  display: flex;
  min-height: 100vh;
  width: 100%;
}

.main-content {
  flex: 1;
  margin-left: 250px;
  display: flex;
  flex-direction: column;
  min-height: 100vh;
}

.view-body {
  flex: 1;
  padding: 24px;
  overflow-y: auto;
  background: #f5f7fa;
}
</style>