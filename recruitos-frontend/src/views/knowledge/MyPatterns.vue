<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getMyCognitiveModel } from '@/api/modules/brain'
import { RCard, RBadge, RProgress, REmpty, RSkeleton } from '@/components/ui'
import { UserCog, Eye, TrendingUp, Target } from 'lucide-vue-next'

const loading = ref(true)
const model = ref<any>(null)

async function load() {
  loading.value = true
  try {
    const res = await getMyCognitiveModel()
    model.value = (res as any).data
  } catch { model.value = null }
  finally { loading.value = false }
}

function parseJson(s: string): any {
  if (!s) return null
  try { return typeof s === 'string' ? JSON.parse(s) : s }
  catch { return null }
}

function formatBias(val: number): string {
  if (val > 0.2) return '偏高 ' + val.toFixed(1)
  if (val < -0.2) return '偏低 ' + Math.abs(val).toFixed(1)
  return '正常'
}

function biasColor(val: number): string {
  if (val > 0.3) return 'text-red-600'
  if (val < -0.3) return 'text-blue-600'
  return 'text-green-600'
}

onMounted(load)
</script>

<template>
  <div class="p-6 max-w-3xl mx-auto space-y-6">
    <div class="flex items-center gap-3 mb-6">
      <UserCog class="h-6 w-6 text-indigo-500" />
      <div>
        <h1 class="text-lg font-semibold text-text-primary">我的模式</h1>
        <p class="text-sm text-text-placeholder">AI 对你决策偏好的认知——来自你每一次录用和拒绝</p>
      </div>
    </div>

    <RSkeleton v-if="loading" class="h-32" />
    <REmpty v-else-if="!model || !model.totalDecisions"
      description="AI 尚未积累足够的决策数据来认识你。当你做出更多录用/拒绝决策后，AI 会逐步生成你的决策画像。" />

    <template v-else>
      <!-- 决策风格 -->
      <RCard class="p-4">
        <div class="flex items-center gap-2 mb-4">
          <TrendingUp class="h-4 w-4 text-text-secondary" />
          <h2 class="text-sm font-semibold text-text-primary">决策风格</h2>
          <RBadge size="sm" variant="default">基于 {{ model.totalDecisions }} 次决策</RBadge>
        </div>
        <div class="grid grid-cols-3 gap-4">
          <div class="text-center p-3 bg-bg-muted rounded-lg">
            <div class="text-2xl font-bold" :class="model.decisionSpeed > 0 ? 'text-green-600' : 'text-amber-600'">
              {{ model.decisionSpeed > 0 ? '⚡' : '🐢' }}
            </div>
            <div class="text-xs text-text-secondary mt-1">决策速度</div>
            <div class="text-xs" :class="model.decisionSpeed > 0 ? 'text-green-600' : 'text-amber-600'">
              {{ model.decisionSpeed > 0 ? '偏快' : '偏慢' }}
            </div>
          </div>
          <div class="text-center p-3 bg-bg-muted rounded-lg">
            <div class="text-2xl font-bold" :class="model.riskTolerance > 0 ? 'text-amber-600' : 'text-blue-600'">
              {{ model.riskTolerance > 0 ? '🎯' : '🛡️' }}
            </div>
            <div class="text-xs text-text-secondary mt-1">风险偏好</div>
            <div class="text-xs" :class="model.riskTolerance > 0 ? 'text-amber-600' : 'text-blue-600'">
              {{ model.riskTolerance > 0 ? '偏激进' : '偏保守' }}
            </div>
          </div>
          <div class="text-center p-3 bg-bg-muted rounded-lg">
            <div class="text-2xl font-bold" :class="model.standardRigidity > 0 ? 'text-green-600' : 'text-amber-600'">
              {{ model.standardRigidity > 0 ? '📏' : '↔️' }}
            </div>
            <div class="text-xs text-text-secondary mt-1">标准刚性</div>
            <div class="text-xs" :class="model.standardRigidity > 0 ? 'text-green-600' : 'text-amber-600'">
              {{ model.standardRigidity > 0 ? '不妥协' : '较灵活' }}
            </div>
          </div>
        </div>
      </RCard>

      <!-- 评分偏差 -->
      <RCard v-if="parseJson(model.scoringBiasJson)" class="p-4">
        <div class="flex items-center gap-2 mb-4">
          <Target class="h-4 w-4 text-text-secondary" />
          <h2 class="text-sm font-semibold text-text-primary">评分偏差</h2>
          <span class="text-xs text-text-placeholder">（与团队均值的差异）</span>
        </div>
        <div class="space-y-2">
          <div v-for="(val, key) in parseJson(model.scoringBiasJson)" :key="key" class="flex items-center justify-between">
            <span class="text-sm text-text-secondary">{{ key }}</span>
            <span class="text-sm font-medium" :class="biasColor(val as number)">
              {{ formatBias(val as number) }}
            </span>
          </div>
        </div>
        <div class="mt-3 pt-3 border-t border-divider text-xs text-text-placeholder">
          宽松指数: {{ model.leniencyIndex?.toFixed(2) }}
          {{ model.leniencyIndex > 1.05 ? '（偏宽松）' : model.leniencyIndex < 0.95 ? '（偏严格）' : '（正常）' }}
        </div>
      </RCard>

      <!-- 盲区 -->
      <RCard v-if="parseJson(model.blindSpotsJson)?.length" class="p-4 border-l-4 border-amber-200">
        <div class="flex items-center gap-2 mb-4">
          <Eye class="h-4 w-4 text-amber-500" />
          <h2 class="text-sm font-semibold text-text-primary">盲区</h2>
          <span class="text-xs text-text-placeholder">AI 发现你历史上反复忽视的信号</span>
        </div>
        <div class="space-y-3">
          <div v-for="(spot, i) in parseJson(model.blindSpotsJson)" :key="i" class="flex items-start gap-3 p-2 bg-amber-50 rounded-lg">
            <AlertTriangle v-if="(spot.missedCount || 0) >= 3" class="h-4 w-4 text-red-400 shrink-0 mt-0.5" />
            <div>
              <p class="text-sm font-medium text-text-primary">{{ spot.signal }}</p>
              <p class="text-xs text-text-secondary">{{ spot.note || '已错过 ' + spot.missedCount + ' 次' }}</p>
            </div>
          </div>
        </div>
      </RCard>

      <!-- 底线说明 -->
      <RCard class="p-4 bg-bg-muted border-divider">
        <h3 class="text-sm font-semibold text-text-secondary mb-2">🔒 只有你自己能看到</h3>
        <p class="text-xs text-text-placeholder">
          AI 对你的认知仅用于帮助你发现自己的决策模式，不会限制 AI 向你展示任何重要信号。
          这些数据不会分享给其他用户，包括你的上级。
        </p>
      </RCard>
    </template>
  </div>
</template>
