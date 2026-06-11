<script setup lang="ts">
import { ref, computed, watch, nextTick } from 'vue'
import { cn } from '@/lib/utils'
import { RButton, RTextarea, RBadge } from '@/components/ui'
import { copilotChat, type CopilotMessage } from '@/api/modules/copilot'
import { toast } from '@/lib/notify'
import {
  Sparkles, Send, X, ChevronRight, Loader2, Bot, User, Trash2, Minimize2, Maximize2,
} from 'lucide-vue-next'

const props = defineProps<{
  open: boolean
  contextPage?: string
  contextData?: string
}>()

const emit = defineEmits<{
  (e: 'close'): void
  (e: 'navigate', path: string): void
}>()

// Session
const sessionId = ref<string>(crypto.randomUUID())
const messages = ref<CopilotMessage[]>([])
const inputMessage = ref('')
const loading = ref(false)
const expanded = ref(false)

// Quick prompts based on context
const contextPrompts = computed(() => {
  const page = props.contextPage || ''
  const prompts: { label: string; text: string }[] = []

  if (page.includes('demand') || page.includes('job')) {
    prompts.push(
      { label: '分析缺口', text: '帮我分析这个岗位的核心能力要求' },
      { label: '搜索建议', text: '建议去哪里找到合适的候选人？' },
      { label: '面试维度', text: '为这个岗位设计面试维度' },
    )
  } else if (page.includes('candidate')) {
    prompts.push(
      { label: '深度评估', text: '用六维模型评估这个候选人' },
      { label: '面试问题', text: '为这个候选人设计面试问题' },
      { label: '风险分析', text: '这个候选人有什么风险信号？' },
    )
  } else if (page.includes('interview')) {
    prompts.push(
      { label: '追问建议', text: '基于当前回答，建议追问什么问题？' },
      { label: '偏差检测', text: '我的评分有没有潜在的偏差？' },
      { label: '校准参考', text: '这个评分在团队中是什么水平？' },
    )
  } else if (page.includes('offer')) {
    prompts.push(
      { label: '薪酬建议', text: '给这个候选人多少薪酬合适？' },
      { label: '谈判策略', text: '怎么跟这个候选人谈 Offer？' },
    )
  } else {
    prompts.push(
      { label: '快速诊断', text: '帮我看看当前招聘流程有什么问题' },
      { label: '找人建议', text: '我的岗位在哪里能找到合适的人？' },
    )
  }

  return prompts
})

async function sendMessage() {
  const msg = inputMessage.value.trim()
  if (!msg || loading.value) return

  messages.value.push({ role: 'user', content: msg, timestamp: Date.now() })
  inputMessage.value = ''
  loading.value = true

  await nextTick()
  scrollToBottom()

  try {
    const res = await copilotChat({
      sessionId: sessionId.value,
      message: props.contextData
        ? `[上下文数据]\n${props.contextData}\n---\n${msg}`
        : msg,
      contextPage: props.contextPage,
    })

    if (res.data) {
      messages.value.push({
        role: 'assistant',
        content: res.data.message,
        timestamp: Date.now(),
      })
    }
  } catch {
    toast.error('AI 回复失败')
  } finally {
    loading.value = false
    await nextTick()
    scrollToBottom()
  }
}

function handleQuickPrompt(text: string) {
  inputMessage.value = text
  sendMessage()
}

function handleClear() {
  messages.value = []
  sessionId.value = crypto.randomUUID()
}

function scrollToBottom() {
  const el = document.getElementById('drawer-chat-messages')
  if (el) el.scrollTop = el.scrollHeight
}

function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    sendMessage()
  }
}

function renderMarkdown(text: string): string {
  if (!text) return ''
  return text
    .replace(/`([^`]+)`/g, '<code class="bg-bg-muted px-1 py-0.5 rounded text-[11px] font-mono text-primary">$1</code>')
    .replace(/\*\*(.+?)\*\*/g, '<strong class="font-semibold">$1</strong>')
    .replace(/^### (.+)$/gm, '<h4 class="text-[13px] font-semibold mt-3 mb-1">$1</h4>')
    .replace(/^## (.+)$/gm, '<h3 class="text-[14px] font-semibold mt-3 mb-1.5">$1</h3>')
    .replace(/^- (.+)$/gm, '<li class="ml-3 list-disc text-[12px]">$1</li>')
    .replace(/\n\n/g, '<br/><br/>')
}
</script>

<template>
  <Teleport to="body">
    <Transition name="drawer">
      <div v-if="open" class="fixed inset-0 z-[100]">
        <!-- Backdrop -->
        <div class="absolute inset-0 bg-black/20" @click="emit('close')" />

        <!-- Drawer -->
        <div
          :class="cn(
            'absolute right-0 top-0 h-full bg-white shadow-2xl border-l border-divider flex flex-col transition-all duration-300',
            expanded ? 'w-[520px]' : 'w-[400px]',
          )"
        >
          <!-- Header -->
          <div class="flex items-center justify-between px-4 py-3 border-b border-divider shrink-0">
            <div class="flex items-center gap-2">
              <div class="w-7 h-7 rounded-full bg-primary-light flex items-center justify-center">
                <Sparkles class="h-3.5 w-3.5 text-primary" />
              </div>
              <span class="text-[14px] font-semibold text-text-primary">AI Co-Pilot</span>
              <RBadge variant="outline" size="sm">Beta</RBadge>
            </div>
            <div class="flex items-center gap-1">
              <RButton variant="ghost" size="sm" @click="expanded = !expanded" :title="expanded ? '缩小' : '放大'">
                <Minimize2 v-if="expanded" class="h-3.5 w-3.5" />
                <Maximize2 v-else class="h-3.5 w-3.5" />
              </RButton>
              <RButton v-if="messages.length > 0" variant="ghost" size="sm" @click="handleClear" title="清空">
                <Trash2 class="h-3.5 w-3.5" />
              </RButton>
              <RButton variant="ghost" size="sm" @click="emit('close')">
                <X class="h-4 w-4" />
              </RButton>
            </div>
          </div>

          <!-- Messages -->
          <div id="drawer-chat-messages" class="flex-1 overflow-y-auto px-4 py-3 space-y-3 scroll-smooth">
            <!-- Welcome -->
            <div v-if="messages.length === 0" class="text-center py-8">
              <Sparkles class="h-8 w-8 text-primary/30 mx-auto mb-3" />
              <p class="text-[13px] font-medium text-text-primary mb-1">需要什么帮助？</p>
              <p class="text-[12px] text-text-secondary mb-4">我可以分析需求、评估候选人、设计面试方案</p>
              <div class="flex flex-wrap gap-2 justify-center">
                <button
                  v-for="cp in contextPrompts"
                  :key="cp.label"
                  class="px-3 py-1.5 rounded-full border border-divider text-[12px] text-text-secondary hover:bg-bg-hover hover:text-text-primary transition-colors"
                  @click="handleQuickPrompt(cp.text)"
                >
                  {{ cp.label }}
                </button>
              </div>
            </div>

            <!-- Chat -->
            <template v-for="(msg, idx) in messages" :key="idx">
              <div v-if="msg.role === 'user'" class="flex justify-end">
                <div class="max-w-[85%] bg-primary text-white rounded-xl rounded-br-sm px-3 py-2">
                  <p class="text-[13px] leading-relaxed whitespace-pre-wrap">{{ msg.content }}</p>
                </div>
              </div>
              <div v-else class="flex gap-2">
                <div class="w-6 h-6 rounded-full bg-primary-light flex items-center justify-center shrink-0 mt-0.5">
                  <Sparkles class="h-3 w-3 text-primary" />
                </div>
                <div class="max-w-[90%] bg-bg-muted rounded-xl rounded-bl-sm px-3 py-2">
                  <div class="text-[12px] leading-relaxed text-text-primary copilot-drawer-md" v-html="renderMarkdown(msg.content)" />
                </div>
              </div>
            </template>

            <!-- Loading -->
            <div v-if="loading" class="flex gap-2">
              <div class="w-6 h-6 rounded-full bg-primary-light flex items-center justify-center shrink-0 mt-0.5">
                <Sparkles class="h-3 w-3 text-primary" />
              </div>
              <div class="bg-bg-muted rounded-xl rounded-bl-sm px-3 py-2">
                <div class="flex gap-1">
                  <span class="w-1.5 h-1.5 bg-primary/40 rounded-full animate-bounce" style="animation-delay: 0ms"></span>
                  <span class="w-1.5 h-1.5 bg-primary/40 rounded-full animate-bounce" style="animation-delay: 100ms"></span>
                  <span class="w-1.5 h-1.5 bg-primary/40 rounded-full animate-bounce" style="animation-delay: 200ms"></span>
                </div>
              </div>
            </div>
          </div>

          <!-- Input -->
          <div class="shrink-0 px-4 py-3 border-t border-divider">
            <div class="flex items-end gap-2">
              <RTextarea
                v-model="inputMessage"
                :rows="1"
                placeholder="问点什么..."
                class="flex-1 resize-none text-[13px]"
                @keydown="handleKeydown"
                :disabled="loading"
              />
              <RButton size="sm" :disabled="!inputMessage.trim() || loading" @click="sendMessage" class="shrink-0">
                <Send v-if="!loading" class="h-3.5 w-3.5" />
                <Loader2 v-else class="h-3.5 w-3.5 animate-spin" />
              </RButton>
            </div>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.drawer-enter-active,
.drawer-leave-active {
  transition: opacity 0.2s ease;
}
.drawer-enter-from,
.drawer-leave-to {
  opacity: 0;
}
.copilot-drawer-md :deep(li) {
  line-height: 1.6;
}

@keyframes bounce {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-3px); }
}
.animate-bounce {
  animation: bounce 0.6s infinite;
}
</style>
