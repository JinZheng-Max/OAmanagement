<template>
  <router-view v-if="isLoginPage" />
  <div v-else class="main-layout">
    <div class="bg-decoration">
      <div class="bg-blob blob-1"></div>
      <div class="bg-blob blob-2"></div>
      <div class="bg-blob blob-3"></div>
      <div class="bg-blob blob-4"></div>
      <div class="bg-blob blob-5"></div>
      <div class="floating-particles">
        <div class="particle" v-for="n in 20" :key="n" :style="getParticleStyle(n)"></div>
      </div>
    </div>
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

const routeOrder = [
  '/dashboard',
  '/ai-assistant',
  '/attendance',
  '/leave',
  '/content',
  '/org'
]

watch(() => route.path, (newPath) => {
  const prevIndex = routeOrder.indexOf(prevPath.value)
  const currentIndex = routeOrder.indexOf(newPath)
  
  if (currentIndex > prevIndex) {
    transitionName.value = 'slide-up'
  } else {
    transitionName.value = 'slide-down'
  }
  
  prevPath.value = newPath
}, { immediate: false })

const getParticleStyle = (n) => {
  const left = (n * 17) % 100
  const top = (n * 23) % 100
  const size = 2 + (n % 6)
  const delay = n * 0.3
  const duration = 15 + (n % 10)
  return {
    left: `${left}%`,
    top: `${top}%`,
    width: `${size}px`,
    height: `${size}px`,
    animationDelay: `${delay}s`,
    animationDuration: `${duration}s`
  }
}
</script>

<style scoped>
.main-layout {
  display: flex;
  min-height: 100vh;
  width: 100%;
  position: relative;
  overflow: hidden;
}

.bg-decoration {
  position: fixed;
  top: 0;
  left: 250px;
  right: 0;
  bottom: 0;
  pointer-events: none;
  z-index: 0;
  overflow: hidden;
}

.bg-blob {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.3;
}

.blob-1 {
  width: 400px;
  height: 400px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  top: -100px;
  right: -50px;
  animation: blobFloat 20s ease-in-out infinite;
}

.blob-2 {
  width: 350px;
  height: 350px;
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
  bottom: -80px;
  left: 10%;
  animation: blobFloat 25s ease-in-out infinite reverse;
}

.blob-3 {
  width: 300px;
  height: 300px;
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
  top: 40%;
  right: 30%;
  animation: blobFloat 30s ease-in-out infinite;
}

.blob-4 {
  width: 250px;
  height: 250px;
  background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%);
  bottom: 20%;
  right: 10%;
  animation: blobFloat 22s ease-in-out infinite reverse;
}

.blob-5 {
  width: 200px;
  height: 200px;
  background: linear-gradient(135deg, #fc4a1a 0%, #f7b733 100%);
  top: 60%;
  left: 20%;
  animation: blobFloat 28s ease-in-out infinite;
}

@keyframes blobFloat {
  0%, 100% {
    transform: translate(0, 0) scale(1);
  }
  25% {
    transform: translate(30px, -30px) scale(1.1);
  }
  50% {
    transform: translate(-20px, 20px) scale(0.95);
  }
  75% {
    transform: translate(20px, 30px) scale(1.05);
  }
}

.floating-particles {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
}

.particle {
  position: absolute;
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.6), rgba(118, 75, 162, 0.6));
  border-radius: 50%;
  animation: particleFloat linear infinite;
}

@keyframes particleFloat {
  0% {
    transform: translateY(0) translateX(0) scale(1);
    opacity: 0;
  }
  10% {
    opacity: 1;
  }
  90% {
    opacity: 1;
  }
  100% {
    transform: translateY(-100vh) translateX(20px) scale(0.5);
    opacity: 0;
  }
}

.main-content {
  flex: 1;
  margin-left: 250px;
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  position: relative;
  z-index: 1;
}

.view-body {
  flex: 1;
  padding: 24px;
  overflow-y: auto;
  background: linear-gradient(135deg, rgba(248, 250, 252, 0.9), rgba(226, 232, 240, 0.9));
  position: relative;
}

.view-body > * {
  position: relative;
}

:deep(.slide-up-enter-active),
:deep(.slide-up-leave-active),
:deep(.slide-down-enter-active),
:deep(.slide-down-leave-active) {
  overflow: hidden;
}
</style>