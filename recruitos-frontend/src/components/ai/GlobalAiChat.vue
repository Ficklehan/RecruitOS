<script setup lang="ts">
import { ref, nextTick } from 'vue'
import { askAI } from '@/api/modules/brain'
import { RButton, RInput, RCard } from '@/components/ui'
import { Sparkles, X, Send, Loader2 } from 'lucide-vue-next'

const open = ref(false)
const input = ref('')
const sending = ref(false)
const messages = ref<{ role: string; text: string }[]>([])

function toggle() {
  open.value = !open.value
  if (open.value) {
    nextTick(() => {
      const el = document.getElementById('ai-chat-input')
      el?.focus()
    })
  }
}

async function send() {
  const q = input.value.trim()
  if (!q || sending.value) return
  sending.value = true
  messages.value.push({ role: 'user', text: q })
  input.value = ''

  try {
    const res = await askAI(q)
    const data = (res as any).data
    messages.value.push({ role: 'ai', text: data?.answer || '抱歉，我暂时无法回答这个问题。' })
  } catch {
    messages.value.push({ role: 'ai', text: '网络异常，请稍后再试。' })
  } finally {
    sending.value = false
  }
}

// Quick suggestions
const suggestions = [
  '支付架构师岗位进展怎么样？',
  '哪个渠道最近效果最好？',
  '有没有需要我关注的预警？',
]
</script>

<template>
  <!-- Floating button -->
  <button
    v-if="!open"
    class="fixed bottom-6 right-6 z-50 w-14 h-14 rounded-full bg-blue-600 text-white shadow-lg hover:bg-blue-700 transition-all flex items-center justify-center"
    @click="toggle"
    title="AI 助手"
  >
    <Sparkles class="h-6 w-6" />
  </button>

  <!-- Chat panel -->
  <div v-if="open"
    class="fixed bottom-6 right-6 z-50 w-[380px] max-h-[520px] bg-white rounded-xl shadow-2xl border border-gray-200 flex flex-col overflow-hidden">
    <!-- Header -->
    <div class="flex items-center justify-between px-4 py-3 border-b border-gray-100 bg-blue-50">
      <div class="flex items-center gap-2">
        <Sparkles class="h-4 w-4 text-blue-600" />
        <span class="text-sm font-semibold text-gray-900">AI 助手</span>
      </div>
      <button class="text-gray-400 hover:text-gray-600" @click="toggle">
        <X class="h-4 w-4" />
      </button>
    </div>

    <!-- Messages -->
    <div class="flex-1 overflow-y-auto p-4 space-y-3 min-h-[200px] max-h-[320px]">
      <div v-if="!messages.length" class="text-center py-8">
        <p class="text-sm text-gray-500 mb-3">我可以帮你：</p>
        <div class="space-y-1.5">
          <button
            v-for="s in suggestions" :key="s"
            class="block w-full text-left text-sm text-blue-600 hover:text-blue-800 px-3 py-1.5 rounded hover:bg-blue-50 transition-colors"
            @click="input = s; send()"
          >
            {{ s }}
          </button>
        </div>
      </div>

      <div v-for="(m, i) in messages" :key="i"
        :class="m.role === 'user'
          ? 'ml-auto bg-blue-600 text-white rounded-lg rounded-br-sm px-3 py-2 max-w-[80%] text-sm'
          : 'mr-auto bg-gray-100 text-gray-900 rounded-lg rounded-bl-sm px-3 py-2 max-w-[85%] text-sm'" >
        {{ m.text }}
      </div>

      <div v-if="sending" class="flex items-center gap-2 text-sm text-gray-400">
        <Loader2 class="h-3.5 w-3.5 animate-spin" /> AI 思考中...
      </div>
    </div>

    <!-- Input -->
    <div class="border-t border-gray-100 p-3 flex gap-2">
      <RInput id="ai-chat-input" v-model="input" placeholder="问任何招聘相关问题..."
        class="flex-1 text-sm" @keyup.enter="send" />
      <RButton size="sm" @click="send" :disabled="sending || !input.trim()">
        <Send class="h-4 w-4" />
      </RButton>
    </div>
  </div>
</template>
