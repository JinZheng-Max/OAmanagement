<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage, ElCollapse, ElCollapseItem } from 'element-plus'
import { askAI, generateLearningPlan } from '../../api/ai'

const question = ref('')
const loading = ref(false)
const planMode = ref(false)
const planTopic = ref('')

interface SourceRef {
  id: string
  sourceId: number
  summary: string
}

interface ChatMessage {
  role: 'user' | 'ai'
  content: string
  sources?: SourceRef[]
}

const messages = ref<ChatMessage[]>([])

async function handleAsk() {
  if (!question.value.trim()) return
  const q = question.value.trim()
  messages.value.push({ role: 'user', content: q })
  question.value = ''
  loading.value = true
  try {
    const result = await askAI(q)
    messages.value.push({
      role: 'ai',
      content: result.answer || '暂无回答',
      sources: result.sources?.length ? result.sources : undefined
    })
  } catch (err: any) {
    ElMessage.error(err?.response?.data?.message || '查询失败')
    messages.value.push({ role: 'ai', content: '查询失败，请确认知识库已上传文档且AI服务可用。' })
  } finally {
    loading.value = false
  }
}

async function handlePlan() {
  if (!planTopic.value.trim()) return
  const topic = planTopic.value.trim()
  messages.value.push({ role: 'user', content: '📚 生成学习计划：' + topic })
  planTopic.value = ''
  planMode.value = false
  loading.value = true
  try {
    const result = await generateLearningPlan(topic)
    messages.value.push({
      role: 'ai',
      content: result.answer || '暂无内容',
      sources: result.sources?.length ? result.sources : undefined
    })
  } catch (err: any) {
    ElMessage.error(err?.response?.data?.message || '生成失败')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="chat-page">
    <div class="page-header">
      <h2>🤖 AI 智能问答</h2>
      <p class="subtitle">基于企业知识库的智能问答助手</p>
      <div class="header-actions">
        <el-button size="small" :type="planMode ? 'warning' : 'default'" @click="planMode = !planMode">
          {{ planMode ? '切换问答' : '生成学习计划' }}
        </el-button>
        <el-button size="small" @click="messages = []">清空对话</el-button>
      </div>
    </div>

    <div class="chat-container">
      <div class="message-list" ref="listRef">
        <div v-if="messages.length === 0" class="empty-state">
          <div class="empty-icon">🤖</div>
          <p style="font-size:16px;font-weight:500;color:#333;">开始提问</p>
          <p style="font-size:13px;color:#999;">输入问题，AI将基于企业知识库回答</p>
          <p style="font-size:12px;color:#c0c4cc;margin-top:8px;">💡 例如：公司的考勤制度是什么？</p>
          <p style="font-size:12px;color:#c0c4cc;">💡 例如：新员工入职流程有哪些步骤？</p>
        </div>

        <div v-for="(msg, i) in messages" :key="i" class="msg-group">
          <div class="msg-item" :class="msg.role">
            <div class="avatar">{{ msg.role === 'user' ? '👤' : '🤖' }}</div>
            <div class="msg-content">
              <div class="bubble">{{ msg.content }}</div>
              <!-- 显示来源引用 -->
              <div v-if="msg.sources && msg.sources.length" class="sources">
                <ElCollapse>
                  <ElCollapseItem :title="'📎 引用来源 (' + msg.sources.length + ')'">
                    <div v-for="(src, si) in msg.sources" :key="si" class="source-item">
                      <span class="source-tag">{{ src.id }}</span>
                      <span class="source-text">{{ src.summary }}</span>
                    </div>
                  </ElCollapseItem>
                </ElCollapse>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="input-bar">
        <template v-if="planMode">
          <el-input
            v-model="planTopic"
            placeholder="输入学习主题..."
            :disabled="loading"
            @keyup.enter="handlePlan"
          />
          <el-button type="warning" :loading="loading" @click="handlePlan" style="margin-left:10px;">
            生成计划
          </el-button>
        </template>
        <template v-else>
          <el-input
            v-model="question"
            placeholder="输入问题，按 Enter 提问..."
            :disabled="loading"
            @keyup.enter="handleAsk"
          />
          <el-button type="primary" :loading="loading" @click="handleAsk" style="margin-left:10px;">
            提问
          </el-button>
        </template>
      </div>
    </div>
  </div>
</template>

<style scoped>
.chat-page { padding: 24px; max-width: 860px; margin: 0 auto; }
.page-header { margin-bottom: 16px; display: flex; align-items: flex-start; justify-content: space-between; flex-wrap: wrap; }
.page-header h2 { margin: 0; font-size: 22px; color: #1d2129; }
.subtitle { font-size: 13px; color: #86909c; margin-top: 2px; width: 100%; }
.header-actions { display: flex; gap: 8px; margin-top: 8px; }

.chat-container {
  background: #fff; border-radius: 12px; border: 1px solid #ebeef5;
  overflow: hidden; display: flex; flex-direction: column; height: calc(100vh - 220px);
}
.message-list { flex: 1; overflow-y: auto; padding: 20px; scroll-behavior: smooth; }
.empty-state { text-align: center; padding: 80px 0; color: #86909c; }
.empty-icon { font-size: 48px; margin-bottom: 12px; }

.msg-group { margin-bottom: 20px; }
.msg-item { display: flex; gap: 12px; }
.msg-item.ai { flex-direction: row; }
.msg-item.user { flex-direction: row-reverse; }
.avatar {
  width: 36px; height: 36px; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  font-size: 18px; background: #f0f2f5; flex-shrink: 0;
}
.msg-content { max-width: 75%; }
.bubble {
  padding: 12px 16px; border-radius: 12px;
  font-size: 14px; line-height: 1.7; white-space: pre-wrap;
}
.msg-item.user .bubble { background: #409eff; color: #fff; }
.msg-item.ai .bubble { background: #f7f8fa; color: #1d2129; }

.sources { margin-top: 8px; }
.source-item { display: flex; gap: 8px; padding: 4px 0; font-size: 12px; align-items: flex-start; }
.source-tag { background: #e6f7ff; color: #1890ff; padding: 1px 6px; border-radius: 4px; font-weight: 600; white-space: nowrap; }
.source-text { color: #666; line-height: 1.5; }

.input-bar { display: flex; padding: 16px; border-top: 1px solid #ebeef5; background: #fafafa; }
</style>
