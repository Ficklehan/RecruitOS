<script setup lang="ts">
import { ref, computed, onMounted, nextTick, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { cn } from '@/lib/utils'
import { RPageShell, RCard, RButton, RBadge, RTextarea } from '@/components/ui'
import { toast } from '@/lib/notify'
import {
  copilotChat, copilotDiagnose, copilotEvaluate, copilotSearchStrategy,
  clearCopilotSession, getQuickActions, getCopilotHistory,
  type CopilotMessage, type QuickAction, type ChatResponse,
} from '@/api/modules/copilot'
import {
  Sparkles, Send, RefreshCw, Trash2, User, Bot, ArrowRight,
  Loader2, Lightbulb, Target, Search, Users, FileText, MessageSquare,
  PanelRightClose, PanelRightOpen, ChevronDown,
} from 'lucide-vue-next'

const route = useRoute()
const router = useRouter()

// Session
const sessionId = ref<string>(crypto.randomUUID())
const messages = ref<CopilotMessage[]>([])
const inputMessage = ref('')
const loading = ref(false)
const quickActions = ref<QuickAction[]>([])
const historyPanelOpen = ref(false)

// Context page detection
const contextPage = computed(() => {
  const p = route.path
  if (p.includes('copilot')) return 'copilot'
  return p
})

// Welcome suggestions
const welcomeActions = [
  { icon: Target, label: '诊断团队缺口', prompt: '帮我分析一下，业务说「Q4 要扛 10 倍交易量」，现有团队 4 人，我应该招什么人？', color: 'bg-primary-light text-primary' },
  { icon: Search, label: '人才搜索策略', prompt: '我在 Boss 直聘上搜了一个月 AI 算法专家没找到合适的，帮我重新规划找人的策略', color: 'bg-info-light text-info' },
  { icon: Users, label: '候选人深度评估', prompt: '我有一份候选人的简历，用 Bar Raiser 的标准帮我评估一下', color: 'bg-success-light text-success' },
  { icon: FileText, label: '面试设计', prompt: '帮我设计一套结构化面试方案，岗位是高级后端工程师，重点考察分布式系统和团队协作', color: 'bg-warning-light text-warning' },
]

onMounted(async () => {
  await loadQuickActions()
  // 恢复之前的会话
  const saved = localStorage.getItem('copilot_session_id')
  if (saved) {
    sessionId.value = saved
    try {
      const res = await getCopilotHistory(sessionId.value)
      if (res.data && res.data.length > 0) {
        messages.value = res.data
      }
    } catch { /* no history */ }
  } else {
    localStorage.setItem('copilot_session_id', sessionId.value)
  }
})

async function loadQuickActions() {
  try {
    const res = await getQuickActions(contextPage.value)
    if (res.data) quickActions.value = res.data
  } catch { /* offline */ }
}

async function sendMessage() {
  const msg = inputMessage.value.trim()
  if (!msg || loading.value) return

  // Add user message
  messages.value.push({ role: 'user', content: msg, timestamp: Date.now() })
  inputMessage.value = ''
  loading.value = true

  await nextTick()
  scrollToBottom()

  try {
    const res = await copilotChat({
      sessionId: sessionId.value,
      message: msg,
      contextPage: contextPage.value,
    })

    if (res.data) {
      messages.value.push({
        role: 'assistant',
        content: res.data.message,
        timestamp: Date.now(),
      })
    }
  } catch (err: any) {
    toast.error(err?.message || 'AI 回复失败')
    messages.value.push({
      role: 'assistant',
      content: '抱歉，我暂时无法回复。请稍后再试，或者尝试换个方式描述你的问题。',
      timestamp: Date.now(),
    })
  } finally {
    loading.value = false
    await nextTick()
    scrollToBottom()
  }
}

async function handleQuickAction(prompt: string) {
  inputMessage.value = prompt
  await sendMessage()
}

async function handleWelcomeAction(prompt: string) {
  inputMessage.value = prompt
  await sendMessage()
}

async function handleClearSession() {
  try {
    await clearCopilotSession(sessionId.value)
  } catch { /* ok */ }
  messages.value = []
  sessionId.value = crypto.randomUUID()
  localStorage.setItem('copilot_session_id', sessionId.value)
  toast.info('会话已清空')
}

function scrollToBottom() {
  const el = document.getElementById('chat-messages')
  if (el) el.scrollTop = el.scrollHeight
}

// Keyboard shortcut
function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    sendMessage()
  }
}

// Simple markdown rendering (inline code, bold, headers, lists)
function renderMarkdown(text: string): string {
  if (!text) return ''
  return text
    .replace(/```(\w*)\n([\s\S]*?)```/g, '<pre class="bg-bg-muted rounded-[var(--r-radius)] p-3 my-2 overflow-x-auto text-[12px] font-mono leading-relaxed"><code>$2</code></pre>')
    .replace(/`([^`]+)`/g, '<code class="bg-bg-muted px-1 py-0.5 rounded text-[12px] font-mono text-primary">$1</code>')
    .replace(/^### (.+)$/gm, '<h4 class="text-[14px] font-semibold text-text-primary mt-4 mb-1">$1</h4>')
    .replace(/^## (.+)$/gm, '<h3 class="text-[15px] font-semibold text-text-primary mt-4 mb-2">$1</h3>')
    .replace(/^# (.+)$/gm, '<h2 class="text-[16px] font-bold text-text-primary mt-5 mb-2">$1</h2>')
    .replace(/\*\*(.+?)\*\*/g, '<strong class="font-semibold text-text-primary">$1</strong>')
    .replace(/\*(.+?)\*/g, '<em>$1</em>')
    .replace(/^- (.+)$/gm, '<li class="ml-4 list-disc text-[13px] text-text-primary leading-relaxed">$1</li>')
    .replace(/^> (.+)$/gm, '<blockquote class="border-l-2 border-primary/30 pl-3 my-2 text-[12px] text-text-secondary italic">$1</blockquote>')
    .replace(/\n\n/g, '<br/><br/>')
    .replace(/\|(.+)\|/g, (match) => {
      if (match.includes('---')) return ''
      return `<div class="text-[12px] font-mono text-text-secondary my-1">${match}</div>`
    })
}
</script>

<template>
  <RPageShell variant="plain">
    <div class="flex h-[calc(100vh-120px)] max-w-4xl mx-auto">
      <!-- Main Chat Area -->
      <div class="flex-1 flex flex-col min-w-0">
        <!-- Header -->
        <div class="flex items-center justify-between pb-4 border-b border-divider shrink-0">
          <div class="flex items-center gap-3">
            <div class="w-9 h-9 rounded-full bg-primary-light flex items-center justify-center">
              <Sparkles class="h-5 w-5 text-primary" />
            </div>
            <div>
              <h1 class="text-[16px] font-semibold text-text-primary">AI Co-Pilot</h1>
              <p class="text-[12px] text-text-secondary">世界级 PM + 招聘专家 · 随时在线</p>
            </div>
          </div>
          <div class="flex items-center gap-2">
            <RButton variant="ghost" size="sm" @click="handleClearSession" :disabled="messages.length === 0" title="清空会话">
              <Trash2 class="h-4 w-4" />
            </RButton>
            <RButton variant="ghost" size="sm" @click="historyPanelOpen = !historyPanelOpen" title="会话历史">
              <PanelRightOpen v-if="!historyPanelOpen" class="h-4 w-4" />
              <PanelRightClose v-else class="h-4 w-4" />
            </RButton>
          </div>
        </div>

        <!-- Messages -->
        <div id="chat-messages" class="flex-1 overflow-y-auto py-4 space-y-4 scroll-smooth">
          <!-- Welcome State -->
          <div v-if="messages.length === 0" class="flex flex-col items-center justify-center py-12 px-4">
            <div class="w-16 h-16 rounded-2xl bg-primary-light flex items-center justify-center mb-6">
              <Sparkles class="h-8 w-8 text-primary" />
            </div>
            <h2 class="text-[18px] font-semibold text-text-primary mb-2">你好，我是 RecruitOS AI Co-Pilot</h2>
            <p class="text-[14px] text-text-secondary text-center max-w-md mb-8">
              我融合了 Google、Meta、Amazon、Stripe 等顶级公司的产品管理与招聘实践。<br/>
              告诉我你的业务场景，我来帮你思考。
            </p>

            <!-- Quick Start Cards -->
            <div class="grid grid-cols-1 md:grid-cols-2 gap-3 w-full max-w-xl">
              <RCard
                v-for="action in welcomeActions"
                :key="action.label"
                variant="interactive"
                padding="md"
                class="cursor-pointer text-left"
                @click="handleWelcomeAction(action.prompt)"
              >
                <div class="flex items-start gap-3">
                  <div :class="cn('w-9 h-9 rounded-[var(--r-radius)] flex items-center justify-center shrink-0', action.color)">
                    <component :is="action.icon" class="h-4 w-4" />
                  </div>
                  <div>
                    <p class="text-[14px] font-medium text-text-primary">{{ action.label }}</p>
                    <p class="text-[12px] text-text-secondary mt-0.5 line-clamp-2">{{ action.prompt }}</p>
                  </div>
                </div>
              </RCard>
            </div>
          </div>

          <!-- Chat Messages -->
          <template v-for="(msg, idx) in messages" :key="idx">
            <!-- User Message -->
            <div v-if="msg.role === 'user'" class="flex justify-end px-4">
              <div class="max-w-[80%]">
                <div class="bg-primary text-white rounded-2xl rounded-br-md px-4 py-2.5">
                  <p class="text-[14px] leading-relaxed whitespace-pre-wrap">{{ msg.content }}</p>
                </div>
              </div>
            </div>

            <!-- AI Message -->
            <div v-else class="flex gap-3 px-4">
              <div class="w-8 h-8 rounded-full bg-primary-light flex items-center justify-center shrink-0 mt-1">
                <Sparkles class="h-4 w-4 text-primary" />
              </div>
              <div class="max-w-[85%] min-w-0">
                <div class="bg-bg-muted rounded-2xl rounded-bl-md px-4 py-3">
                  <div
                    class="text-[13px] leading-relaxed text-text-primary copilot-markdown"
                    v-html="renderMarkdown(msg.content)"
                  />
                </div>
              </div>
            </div>
          </template>

          <!-- Loading Indicator -->
          <div v-if="loading" class="flex gap-3 px-4">
            <div class="w-8 h-8 rounded-full bg-primary-light flex items-center justify-center shrink-0 mt-1">
              <Sparkles class="h-4 w-4 text-primary" />
            </div>
            <div class="bg-bg-muted rounded-2xl rounded-bl-md px-4 py-3">
              <div class="flex items-center gap-2">
                <div class="flex gap-1">
                  <span class="w-2 h-2 bg-primary/40 rounded-full animate-bounce" style="animation-delay: 0ms"></span>
                  <span class="w-2 h-2 bg-primary/40 rounded-full animate-bounce" style="animation-delay: 150ms"></span>
                  <span class="w-2 h-2 bg-primary/40 rounded-full animate-bounce" style="animation-delay: 300ms"></span>
                </div>
                <span class="text-[12px] text-text-placeholder">思考中...</span>
              </div>
            </div>
          </div>
        </div>

        <!-- Input Area -->
        <div class="shrink-0 pt-3 border-t border-divider">
          <!-- Quick Action Chips -->
          <div v-if="quickActions.length > 0 && messages.length === 0" class="flex flex-wrap gap-2 mb-3">
            <button
              v-for="qa in quickActions"
              :key="qa.action"
              class="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-full border border-divider text-[12px] text-text-secondary hover:bg-bg-hover hover:text-text-primary transition-colors"
              @click="handleQuickAction(qa.prompt)"
            >
              <Sparkles class="h-3 w-3 text-primary" />
              {{ qa.label }}
            </button>
          </div>

          <div class="flex items-end gap-2">
            <RTextarea
              v-model="inputMessage"
              :rows="1"
              placeholder="输入你的问题... (Enter 发送，Shift+Enter 换行)"
              class="flex-1 resize-none"
              @keydown="handleKeydown"
              :disabled="loading"
            />
            <RButton
              :disabled="!inputMessage.trim() || loading"
              @click="sendMessage"
              class="shrink-0"
            >
              <Send v-if="!loading" class="h-4 w-4" />
              <Loader2 v-else class="h-4 w-4 animate-spin" />
            </RButton>
          </div>
          <p class="text-[11px] text-text-placeholder mt-2 text-center">
            AI Co-Pilot 会认真思考但可能出错。关键决策请结合你的专业判断。
          </p>
        </div>
      </div>

      <!-- History Sidebar -->
      <div
        v-if="historyPanelOpen"
        class="w-64 shrink-0 border-l border-divider ml-4 pl-4 overflow-y-auto"
      >
        <h3 class="text-[14px] font-semibold text-text-primary mb-3">对话历史</h3>
        <div v-if="messages.length === 0" class="text-[12px] text-text-placeholder">
          暂无对话历史
        </div>
        <div v-else class="space-y-1">
          <div
            v-for="(msg, idx) in messages.filter(m => m.role === 'user')"
            :key="idx"
            class="text-[12px] text-text-secondary p-2 rounded hover:bg-bg-hover cursor-pointer truncate"
          >
            {{ msg.content.substring(0, 40) }}{{ msg.content.length > 40 ? '...' : '' }}
          </div>
        </div>
      </div>
    </div>
  </RPageShell>
</template>

<style scoped>
.copilot-markdown :deep(pre) {
  white-space: pre-wrap;
  word-break: break-word;
}
.copilot-markdown :deep(table) {
  width: 100%;
  border-collapse: collapse;
  font-size: 12px;
  margin: 8px 0;
}
.copilot-markdown :deep(th),
.copilot-markdown :deep(td) {
  border: 1px solid var(--color-divider);
  padding: 6px 8px;
  text-align: left;
}
.copilot-markdown :deep(th) {
  background: var(--color-bg-muted);
  font-weight: 600;
}
.copilot-markdown :deep(hr) {
  border: none;
  border-top: 1px solid var(--color-divider);
  margin: 12px 0;
}
@keyframes bounce {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-4px); }
}
.animate-bounce {
  animation: bounce 0.6s infinite;
}
</style>
