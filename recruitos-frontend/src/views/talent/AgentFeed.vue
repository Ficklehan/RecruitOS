<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { RCard, RButton, RBadge, REmpty, RSkeleton } from '@/components/ui'
import { Sparkles, TrendingUp, AlertTriangle, ChevronRight, UserPlus, RefreshCw } from 'lucide-vue-next'

const router = useRouter()
const loading = ref(true)
const candidates = ref<any[]>([])
const error = ref('')

async function load() {
  loading.value = true
  error.value = ''
  try {
    const res = await fetch('/api/brain/agent-feed')
    const data = await res.json()
    if (data.code === 0 && data.data) {
      candidates.value = data.data
    } else {
      error.value = '数据加载失败'
    }
  } catch (e) {
    error.value = 'AI 推荐服务暂不可用'
  }
  loading.value = false
}

function goToCandidate(id: number) { router.push(`/pipeline/candidates/${id}`) }
function formatTime(d: string) {
  if (!d) return ''
  // Simple relative time
  try {
    const then = new Date(d).getTime()
    const now = Date.now()
    const hours = Math.floor((now - then) / 3600000)
    if (hours < 1) return '刚刚'
    if (hours < 24) return hours + '小时前'
    if (hours < 48) return '昨天'
    return Math.floor(hours / 24) + '天前'
  } catch { return d }
}
onMounted(load)
</script>

<template>
  <div class="p-6 max-w-2xl mx-auto space-y-4">
    <div class="flex items-center justify-between mb-4">
      <div class="flex items-center gap-3">
        <Sparkles class="h-5 w-5 text-blue-500" />
        <div>
          <h1 class="text-base font-semibold text-text-primary">AI 推荐</h1>
          <p class="text-xs text-text-placeholder">不是按匹配度排，是按 AI 觉得你该看看排</p>
        </div>
      </div>
      <RButton variant="ghost" size="sm" @click="load" :disabled="loading">
        <RefreshCw class="h-4 w-4" :class="loading ? 'animate-spin' : ''" />
      </RButton>
    </div>

    <RSkeleton v-if="loading" class="h-64" />
    <REmpty v-else-if="error" :description="error" />
    <REmpty v-else-if="!candidates.length" description="AI 正在扫描候选人池，稍后再来" />

    <RCard v-for="c in candidates" :key="c.id"
      class="p-4 hover:shadow-md transition-shadow cursor-pointer border-l-4"
      :class="c.intentLevel === 'HIGH' ? 'border-green-200' : c.intentLevel === 'LOW' ? 'border-red-200' : 'border-blue-200'"
      @click="goToCandidate(c.id)">
      <div class="flex items-start justify-between gap-3">
        <div class="flex-1 min-w-0">
          <div class="flex items-center gap-2 mb-1">
            <h3 class="text-sm font-semibold text-text-primary">{{ c.name }}</h3>
            <RBadge size="sm" variant="outline">{{ c.title }}</RBadge>
            <RBadge size="sm" variant="secondary">{{ c.matchScore }}%</RBadge>
          </div>
          <p class="text-xs text-text-placeholder mb-2">{{ c.company }} · {{ c.years }}年 · {{ c.source }}</p>
          <div class="flex items-start gap-1.5 mb-1">
            <TrendingUp class="h-3.5 w-3.5 text-blue-500 shrink-0 mt-0.5" />
            <p class="text-sm text-text-secondary">{{ c.aiRationale }}</p>
          </div>
          <div v-if="c.aiConcern" class="flex items-start gap-1.5">
            <AlertTriangle class="h-3.5 w-3.5 text-amber-500 shrink-0 mt-0.5" />
            <p class="text-sm text-amber-700">{{ c.aiConcern }}</p>
          </div>
        </div>
        <div class="text-center shrink-0">
          <div class="text-lg font-bold" :class="c.intentLevel === 'HIGH' ? 'text-green-500' : c.intentLevel === 'LOW' ? 'text-red-400' : 'text-amber-400'">{{ c.intentScore }}</div>
          <div class="text-[10px] text-text-placeholder">意向</div>
        </div>
        <ChevronRight class="h-4 w-4 text-text-placeholder shrink-0 self-center" />
      </div>
      <div class="mt-3 pt-3 border-t border-divider flex gap-2">
        <RButton size="sm" variant="default" @click.stop="goToCandidate(c.id)"><UserPlus class="h-3.5 w-3.5 mr-1" /> 查看</RButton>
        <RButton size="sm" variant="ghost" @click.stop>加入待联系</RButton>
        <RButton size="sm" variant="ghost" class="text-text-placeholder" @click.stop>跳过</RButton>
      </div>
    </RCard>

    <RCard class="p-4 bg-blue-50 border-blue-200 text-center">
      <p class="text-sm text-blue-700">AI 持续扫描人才库、内推、猎头和平台候选人，基于「团队缺什么」和「历史上什么人成功」来排序推荐。</p>
    </RCard>
  </div>
</template>
