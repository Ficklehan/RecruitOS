<script setup lang="ts">
import { ref } from 'vue'
import { RCard, RButton, RSwitch, RSelect, RInput, RSeparator } from '@/components/ui'
import { toast } from '@/lib/notify'
import { Brain, Zap, Shield, AlertTriangle, Save } from 'lucide-vue-next'

const loading = ref(false)

// AI 全局模式
const aiMode = ref('standard') // aggressive / standard / conservative
const modeOptions = [
  { label: '激进 — AI 更主动推送、自动执行低风险操作', value: 'aggressive' },
  { label: '标准 — 平衡推送频率和人工确认（推荐）', value: 'standard' },
  { label: '保守 — AI 仅在被请求时响应，不自动执行', value: 'conservative' },
]

// 触点开关
const touchpoints = ref([
  { key: 'intent', label: '候选人意向预测', enabled: true, desc: '在管道卡片上显示意向指示' },
  { key: 'pipeline', label: '管道健康诊断', enabled: true, desc: '每天早上自动巡检在招岗位' },
  { key: 'interview', label: '面试辅助', enabled: true, desc: '面试准备brief + 评估偏差提醒' },
  { key: 'calibration', label: '校准会主持', enabled: true, desc: 'AI主持校准会，统计评分分歧' },
  { key: 'offer', label: 'Offer 策略建议', enabled: true, desc: '基于市场数据和候选人信号生成策略' },
  { key: 'pattern', label: '模式自动发现', enabled: true, desc: '从历史数据中自动发现招聘规律' },
  { key: 'user_model', label: '用户画像分析', enabled: true, desc: '分析你的决策偏好和盲区' },
  { key: 'observation', label: '主动观察推送', enabled: true, desc: 'AI 主动推送需要关注的洞察' },
])

// 阈值
const thresholds = ref({
  pipelineStallDays: 7,
  intentLowThreshold: 40,
  interviewerBiasAlert: 0.3,
  offerAcceptLow: 60,
})

async function save() {
  loading.value = true
  await new Promise(r => setTimeout(r, 500))
  loading.value = false
  toast.success('AI 设置已保存')
}
</script>

<template>
  <div class="p-6 max-w-2xl mx-auto space-y-6">
    <div class="flex items-center gap-3 mb-2">
      <Brain class="h-5 w-5 text-blue-500" />
      <div>
        <h1 class="text-base font-semibold text-text-primary">AI 设置</h1>
        <p class="text-xs text-text-placeholder">配置 AI 的行为模式和能力开关</p>
      </div>
    </div>

    <!-- 全局模式 -->
    <RCard class="p-5">
      <div class="flex items-center gap-2 mb-4">
        <Zap class="h-4 w-4 text-amber-500" />
        <h2 class="text-sm font-semibold text-text-primary">AI 全局模式</h2>
      </div>
      <RSelect v-model="aiMode" :options="modeOptions" class="w-full" />
    </RCard>

    <!-- 触点开关 -->
    <RCard class="p-5">
      <div class="flex items-center gap-2 mb-4">
        <Brain class="h-4 w-4 text-blue-500" />
        <h2 class="text-sm font-semibold text-text-primary">AI 能力开关</h2>
      </div>
      <div class="space-y-3">
        <div v-for="tp in touchpoints" :key="tp.key" class="flex items-center justify-between py-2">
          <div class="flex-1">
            <div class="text-sm font-medium text-text-primary">{{ tp.label }}</div>
            <div class="text-xs text-text-placeholder">{{ tp.desc }}</div>
          </div>
          <RSwitch v-model="tp.enabled" />
        </div>
      </div>
    </RCard>

    <!-- 阈值配置 -->
    <RCard class="p-5">
      <div class="flex items-center gap-2 mb-4">
        <Shield class="h-4 w-4 text-green-500" />
        <h2 class="text-sm font-semibold text-text-primary">阈值配置</h2>
      </div>
      <div class="space-y-4">
        <div class="flex items-center justify-between">
          <div>
            <div class="text-sm text-text-primary">管道停滞告警</div>
            <div class="text-xs text-text-placeholder">连续N天无新增候选人时告警</div>
          </div>
          <RInput v-model.number="thresholds.pipelineStallDays" type="number" class="w-20 text-sm" />
          <span class="text-xs text-text-placeholder ml-1">天</span>
        </div>
        <div class="flex items-center justify-between">
          <div>
            <div class="text-sm text-text-primary">意向低分阈值</div>
            <div class="text-xs text-text-placeholder">意向分低于此值标记为LOW</div>
          </div>
          <RInput v-model.number="thresholds.intentLowThreshold" type="number" class="w-20 text-sm" />
        </div>
        <div class="flex items-center justify-between">
          <div>
            <div class="text-sm text-text-primary">面试官偏差告警</div>
            <div class="text-xs text-text-placeholder">宽松指数偏离1.0超过此值告警</div>
          </div>
          <RInput v-model.number="thresholds.interviewerBiasAlert" type="number" class="w-20 text-sm" step="0.05" />
        </div>
      </div>
    </RCard>

    <!-- 保存 -->
    <RButton class="w-full" size="lg" @click="save" :disabled="loading">
      <Save class="h-4 w-4 mr-2" />
      {{ loading ? '保存中...' : '保存设置' }}
    </RButton>
  </div>
</template>
