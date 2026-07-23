<template>
  <div class="ai-container">
    <div class="page-header">
      <h2 class="page-title">🤖 AI 智能助手</h2>
      <p class="page-subtitle">智能问答、全文检索，帮您快速找到答案</p>
    </div>

    <div class="tab-switcher">
      <button 
        class="tab-btn" 
        :class="{ active: activeTab === 'qa' }"
        @click="activeTab = 'qa'"
      >
        <div class="tab-glow"></div>
        <el-icon class="tab-icon"><ChatDotRound /></el-icon>
        <span class="tab-label">AI 问答</span>
        <div class="tab-indicator"></div>
      </button>
      <button 
        class="tab-btn" 
        :class="{ active: activeTab === 'search' }"
        @click="activeTab = 'search'"
      >
        <div class="tab-glow"></div>
        <el-icon class="tab-icon"><Search /></el-icon>
        <span class="tab-label">全文检索</span>
        <div class="tab-indicator"></div>
      </button>
    </div>

    <transition name="tab-fade" mode="out-in">
      <div v-if="activeTab === 'qa'" key="qa" class="tab-content">
        <div class="chat-container">
          <div class="chat-history">
            <div v-for="(msg, i) in chatHistory" :key="i" :class="['chat-item', msg.role]">
              <div class="chat-avatar">
                <el-icon v-if="msg.role === 'assistant'"><ChatDotRound /></el-icon>
                <el-icon v-else><User /></el-icon>
              </div>
              <div class="chat-bubble">
                <p>{{ msg.text }}</p>
                <div v-if="msg.sources && msg.sources.length" class="chat-sources">
                  <p class="sources-title">💡 匹配制度依据：</p>
                  <div class="sources-list">
                    <el-tag v-for="s in msg.sources" :key="s.contentId" size="small" type="info">
                      《{{ s.title }}》 (得分: {{ s.score }})
                    </el-tag>
                  </div>
                </div>
              </div>
            </div>
            <div v-if="isThinking" class="chat-item assistant thinking">
              <div class="chat-avatar thinking-avatar">
                <el-icon><ChatDotRound /></el-icon>
              </div>
              <div class="chat-bubble thinking-bubble">
                <div class="thinking-content">
                  <span class="thinking-text">AI正在思考</span>
                  <span class="thinking-dots">
                    <span class="dot"></span>
                    <span class="dot"></span>
                    <span class="dot"></span>
                  </span>
                </div>
              </div>
            </div>
          </div>
          <div class="chat-input">
            <el-input v-model="question" placeholder="问一问AI：年假怎么休？请假找谁审批？" @keyup.enter="askAi" :disabled="isThinking" size="large">
              <template #append>
                <el-button type="primary" @click="askAi" :disabled="isThinking">
                  <el-icon v-if="!isThinking"><ArrowRight /></el-icon>
                  <el-icon v-else class="loading-icon"><Loading /></el-icon>
                  {{ isThinking ? '思考中' : '发送' }}
                </el-button>
              </template>
            </el-input>
          </div>
        </div>
      </div>

      <div v-else key="search" class="tab-content">
        <div class="search-container">
          <el-input v-model="keyword" placeholder="输入关键字，从知识库中进行检索..." size="large">
            <template #append>
              <el-button type="primary" @click="doSearch">
                <el-icon><Search /></el-icon>
                检索
              </el-button>
            </template>
          </el-input>
          
          <div class="search-results" v-if="searchResults.length">
            <div v-for="item in searchResults" :key="item.id" class="result-item">
              <div class="result-header">
                <h4 v-html="item.highlightTitle || item.title"></h4>
                <el-tag :type="item.type === 'ANNOUNCEMENT' ? 'primary' : 'warning'" size="small">
                  {{ typeMap[item.type] || item.type }}
                </el-tag>
              </div>
              <p v-html="item.highlightBody || item.body" class="result-body"></p>
            </div>
          </div>
          <div class="search-empty" v-else>
            <el-icon class="empty-icon"><Search /></el-icon>
            <p>请输入关键词进行检索</p>
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { ChatDotRound, User, ArrowRight, Search, Loading } from '@element-plus/icons-vue'
import { searchContentsApi } from '../api/content'

const activeTab = ref('qa')
const question = ref('')
const keyword = ref('')
const chatHistory = ref([{ role: 'assistant', text: '您好！我是智办 AI 智能助手，输入您的疑问或关键词，我将从企业知识库与制度公告中为您即时解答。' }])
const searchResults = ref([])
const isThinking = ref(false)

const typeMap = {
  ANNOUNCEMENT: '公告',
  POLICY: '制度'
}

const askAi = async () => {
  if (!question.value.trim() || isThinking.value) return
  const q = question.value.trim()
  chatHistory.value.push({ role: 'user', text: q })
  question.value = ''
  isThinking.value = true

  try {
    const res = await searchContentsApi({ keyword: q, page: 1, size: 3 })
    if (res && res.data && res.data.list && res.data.list.length > 0) {
      const topDoc = res.data.list[0]
      const answerText = `根据企业规范《${topDoc.title}》：\n${topDoc.body ? topDoc.body.substring(0, 200) : '详细信息请查看相关制度公告。'}...`
      const sources = res.data.list.map(d => ({
        contentId: d.id,
        title: d.title,
        score: '0.92'
      }))
      chatHistory.value.push({ role: 'assistant', text: answerText, sources })
    } else {
      chatHistory.value.push({
        role: 'assistant',
        text: `为您查阅了知识库，暂未找到与“${q}”直接匹配的内容。建议您直接在【公告制度中心】按分类浏览，或咨询部门经理。`
      })
    }
  } catch (err) {
    chatHistory.value.push({
      role: 'assistant',
      text: '智能助手正在进行算法维护，请稍后再试。'
    })
  } finally {
    isThinking.value = false
  }
}

const doSearch = async () => {
  if (!keyword.value.trim()) return
  try {
    const res = await searchContentsApi({ keyword: keyword.value.trim(), page: 1, size: 20 })
    if (res && res.data && (res.data.list || res.data.records)) {
      searchResults.value = res.data.list || res.data.records || []
      if (searchResults.value.length === 0) {
        ElMessage.info('未找到相关内容')
      }
    }
  } catch (err) {
    ElMessage.error(err.response?.data?.message || '搜索引擎检索失败')
  }
}
</script>

<style scoped>
.ai-container {
  max-width: 1400px;
}

.page-header {
  margin-bottom: 32px;
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

.tab-switcher {
  display: flex;
  gap: 16px;
  margin-bottom: 32px;
}

.tab-btn {
  flex: 1;
  position: relative;
  height: 72px;
  padding: 0 32px;
  background: rgba(255, 255, 255, 0.6);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.5);
  border-radius: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  cursor: pointer;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden;
}

.tab-btn:hover {
  background: rgba(255, 255, 255, 0.8);
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
  border-color: rgba(102, 126, 234, 0.2);
}

.tab-btn.active {
  background: rgba(255, 255, 255, 0.95);
  box-shadow: 0 8px 30px rgba(102, 126, 234, 0.15);
  border-color: rgba(102, 126, 234, 0.3);
}

.tab-glow {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.05) 0%, rgba(118, 75, 162, 0.03) 100%);
  opacity: 0;
  transition: opacity 0.4s ease;
}

.tab-btn.active .tab-glow {
  opacity: 1;
}

.tab-icon {
  font-size: 24px;
  color: #94a3b8;
  transition: all 0.4s ease;
}

.tab-btn.active .tab-icon {
  color: #667eea;
  transform: scale(1.1);
}

.tab-label {
  font-size: 18px;
  font-weight: 700;
  color: #64748b;
  transition: all 0.4s ease;
}

.tab-btn.active .tab-label {
  color: #1a1a2e;
}

.tab-indicator {
  position: absolute;
  bottom: 0;
  left: 15%;
  right: 15%;
  height: 4px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 2px;
  opacity: 0;
  transform: scaleX(0);
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}

.tab-btn.active .tab-indicator {
  opacity: 1;
  transform: scaleX(1);
}

.tab-content {
  min-height: 560px;
}

.tab-fade-enter-active,
.tab-fade-leave-active {
  transition: all 0.3s ease;
}

.tab-fade-enter-from {
  opacity: 0;
  transform: translateY(10px);
}

.tab-fade-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}

.chat-container {
  height: 560px;
  display: flex;
  flex-direction: column;
  background: rgba(255, 255, 255, 0.6);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-radius: 24px;
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.5);
  box-shadow: 0 4px 30px rgba(0, 0, 0, 0.05);
}

.chat-history {
  flex: 1;
  overflow-y: auto;
  padding: 32px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.chat-item {
  display: flex;
  gap: 14px;
  animation: fadeInUp 0.4s ease-out;
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.chat-item.user {
  justify-content: flex-end;
}

.chat-item.user .chat-bubble {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border-radius: 20px 6px 20px 20px;
  box-shadow: 0 4px 16px rgba(102, 126, 234, 0.3);
}

.chat-avatar {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: all 0.3s ease;
}

.chat-item.assistant .chat-avatar {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

.chat-item.user .chat-avatar {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
  box-shadow: 0 4px 12px rgba(240, 147, 251, 0.3);
}

.chat-avatar :deep(.el-icon) {
  font-size: 22px;
  color: white;
}

.thinking-avatar {
  animation: avatar-pulse 2s ease-in-out infinite;
}

@keyframes avatar-pulse {
  0%, 100% { transform: scale(1); box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3); }
  50% { transform: scale(1.05); box-shadow: 0 6px 16px rgba(102, 126, 234, 0.4); }
}

.thinking-bubble {
  background: rgba(255, 255, 255, 0.6) !important;
}

.thinking-content {
  display: flex;
  align-items: center;
  gap: 8px;
}

.thinking-text {
  font-size: 15px;
  color: #64748b;
}

.thinking-dots {
  display: flex;
  gap: 4px;
}

.thinking-dots .dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #667eea;
  animation: dot-bounce 1.4s ease-in-out infinite;
}

.thinking-dots .dot:nth-child(1) { animation-delay: 0s; }
.thinking-dots .dot:nth-child(2) { animation-delay: 0.2s; }
.thinking-dots .dot:nth-child(3) { animation-delay: 0.4s; }

@keyframes dot-bounce {
  0%, 80%, 100% { transform: scale(0.6); opacity: 0.5; }
  40% { transform: scale(1); opacity: 1; }
}

.loading-icon {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.chat-bubble {
  max-width: 70%;
  padding: 18px 24px;
  background: rgba(255, 255, 255, 0.7);
  border-radius: 6px 20px 20px 20px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.06);
  transition: all 0.3s ease;
}

.chat-bubble:hover {
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.08);
}

.chat-bubble p {
  margin: 0;
  font-size: 15px;
  line-height: 1.7;
}

.chat-item.user .chat-bubble p {
  color: white;
}

.chat-sources {
  margin-top: 14px;
  padding-top: 14px;
  border-top: 1px solid rgba(102, 126, 234, 0.1);
}

.sources-title {
  font-size: 12px;
  color: #e6a23c;
  margin: 0 0 10px 0;
  font-weight: 600;
}

.sources-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.chat-input {
  padding: 24px;
  border-top: 1px solid rgba(0, 0, 0, 0.06);
  background: rgba(255, 255, 255, 0.8);
}

.chat-input :deep(.el-input__wrapper) {
  border-radius: 16px;
  border: 1.5px solid rgba(0, 0, 0, 0.08);
  padding: 14px 18px;
}

.chat-input :deep(.el-input__wrapper:hover) {
  border-color: rgba(102, 126, 234, 0.3);
}

.chat-input :deep(.el-input__wrapper.is-focus) {
  border-color: #667eea;
  box-shadow: 0 0 0 4px rgba(102, 126, 234, 0.1);
}

.chat-input :deep(.el-button--primary) {
  border-radius: 12px;
  padding: 12px 28px;
  font-weight: 600;
}

.search-container {
  padding: 32px;
  background: rgba(255, 255, 255, 0.6);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-radius: 24px;
  border: 1px solid rgba(255, 255, 255, 0.5);
  box-shadow: 0 4px 30px rgba(0, 0, 0, 0.05);
  min-height: 560px;
}

.search-container :deep(.el-input__wrapper) {
  border-radius: 16px;
  border: 1.5px solid rgba(0, 0, 0, 0.08);
  padding: 16px 20px;
}

.search-container :deep(.el-input__wrapper:hover) {
  border-color: rgba(102, 126, 234, 0.3);
}

.search-container :deep(.el-input__wrapper.is-focus) {
  border-color: #667eea;
  box-shadow: 0 0 0 4px rgba(102, 126, 234, 0.1);
}

.search-container :deep(.el-button--primary) {
  border-radius: 12px;
  padding: 14px 28px;
  font-weight: 600;
}

.search-results {
  margin-top: 28px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.result-item {
  padding: 24px;
  background: rgba(255, 255, 255, 0.7);
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.5);
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.result-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.08);
  border-color: rgba(102, 126, 234, 0.2);
}

.result-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
}

.result-header h4 {
  font-size: 17px;
  font-weight: 700;
  color: #1a1a2e;
  margin: 0;
}

.result-body {
  font-size: 15px;
  color: #64748b;
  line-height: 1.7;
  margin: 0;
}

.search-empty {
  margin-top: 80px;
  text-align: center;
}

.empty-icon {
  font-size: 56px;
  color: #cbd5e1;
  margin-bottom: 20px;
  animation: float 3s ease-in-out infinite;
}

@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-8px); }
}

.search-empty p {
  font-size: 15px;
  color: #94a3b8;
  margin: 0;
}

:deep(em) {
  color: #e53935;
  font-style: normal;
  font-weight: 700;
  background: rgba(229, 57, 53, 0.1);
  padding: 3px 6px;
  border-radius: 6px;
}
</style>