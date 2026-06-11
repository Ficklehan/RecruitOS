<template>
  <PageShell title="AI 设置" subtitle="管理 AI 能力的行为模式与阈值">
    <div v-if="loading" class="flex justify-center py-12"><Loader2 class="h-6 w-6 animate-spin" /></div>
    <template v-if="settings">
      <!-- 全局模式 -->
      <Card class="p-6 mb-6">
        <h3 class="font-semibold mb-4">全局 AI 模式</h3>
        <div class="flex gap-3">
          <button v-for="m in modes" :key="m.value"
            class="mode-btn" :class="{ active: settings.globalMode === m.value }"
            @click="settings.globalMode = m.value">
            <span class="mode-label">{{ m.label }}</span>
            <span class="mode-desc">{{ m.desc }}</span>
          </button>
        </div>
      </Card>

      <!-- 触点开关 -->
      <Card class="p-6 mb-6">
        <h3 class="font-semibold mb-4">触点管理</h3>
        <div class="space-y-3">
          <div v-for="(cfg, key) in settings.touchpoints" :key="key" class="touchpoint-row">
            <div class="flex items-center justify-between">
              <div>
                <span class="font-medium text-sm">{{ touchpointLabels[key] || key }}</span>
                <p class="text-xs text-muted-foreground">{{ touchpointDescs[key] || '' }}</p>
              </div>
              <div class="flex items-center gap-3">
                <select v-model="cfg.aggressiveness" class="text-xs border rounded px-2 py-1">
                  <option value="AGGRESSIVE">激进</option>
                  <option value="STANDARD">标准</option>
                  <option value="CONSERVATIVE">保守</option>
                </select>
                <button :class="['toggle', cfg.enabled ? 'toggle-on' : 'toggle-off']" @click="cfg.enabled = !cfg.enabled">
                  <span class="toggle-dot" />
                </button>
              </div>
            </div>
          </div>
        </div>
      </Card>

      <!-- 阈值 -->
      <Card class="p-6 mb-6">
        <h3 class="font-semibold mb-4">阈值设置</h3>
        <div class="grid grid-cols-2 gap-4">
          <div>
            <label class="text-sm">意向评分 HIGH 阈值</label>
            <input v-model.number="settings.thresholds.intentHighThreshold" type="number" class="w-full border rounded px-3 py-2 mt-1" />
          </div>
          <div>
            <label class="text-sm">意向评分 MEDIUM 阈值</label>
            <input v-model.number="settings.thresholds.intentMediumThreshold" type="number" class="w-full border rounded px-3 py-2 mt-1" />
          </div>
          <div>
            <label class="text-sm">面试官偏差告警阈值</label>
            <input v-model.number="settings.thresholds.interviewerBiasThreshold" type="number" step="0.05" class="w-full border rounded px-3 py-2 mt-1" />
          </div>
          <div>
            <label class="text-sm">LLM 模型</label>
            <select v-model="settings.thresholds.llmModel" class="w-full border rounded px-3 py-2 mt-1"><option>gpt-4o</option><option>gpt-4o-mini</option></select>
          </div>
        </div>
      </Card>

      <div class="flex gap-3">
        <Button @click="handleSave" :disabled="saving">{{ saving ? '保存中...' : '保存设置' }}</Button>
        <Button variant="outline" @click="loadSettings">恢复默认</Button>
      </div>
    </template>
  </PageShell>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Loader2 } from 'lucide-vue-next'
import PageShell from '@/components/Layout/PageShell.vue'
import { Button, Card } from '@/components/ui'
import request from '@/api/request'
import { toast } from '@/lib/notify'

const loading = ref(true)
const saving = ref(false)
const settings = ref<any>(null)

const modes = [
  { value: 'AGGRESSIVE', label: '激进', desc: 'AI 主动推送建议，高置信度自动执行' },
  { value: 'STANDARD', label: '标准', desc: 'AI 主动推送建议，需人工确认' },
  { value: 'CONSERVATIVE', label: '保守', desc: 'AI 被动响应查询，仅提供参考' },
]

const touchpointLabels: Record<string, string> = {
  INTENT: '意向预测', INTERVIEW_ASSIST: '面试辅助', CALIBRATION: '校准会',
  OFFER_STRATEGY: 'Offer 策略', DEMAND_DIAGNOSIS: '需求诊断',
  CYCLE_PREDICTION: '周期预测', INTERVIEWER_QUALITY: '面试官质量', TALENT_DENSITY: '人才密度',
}

const touchpointDescs: Record<string, string> = {
  INTENT: '预测候选人 Offer 接受概率', INTERVIEW_ASSIST: '面试中实时辅助与偏差提醒',
  CALIBRATION: '多面试官评分校准分析', OFFER_STRATEGY: 'Offer 谈判策略建议',
  DEMAND_DIAGNOSIS: '业务需求→人才方案翻译',
}

async function loadSettings() {
  loading.value = true
  try {
    const { data } = await request.get('/api/brain/settings')
    settings.value = data
  } catch { toast.error('加载设置失败') }
  finally { loading.value = false }
}

async function handleSave() {
  saving.value = true
  try {
    await request.put('/api/brain/settings', settings.value)
    toast.success('设置已保存')
  } catch { toast.error('保存失败') }
  finally { saving.value = false }
}

onMounted(loadSettings)
</script>

<style scoped>
.mode-btn {
  flex: 1; padding: 16px; border: 2px solid #e2e8f0; border-radius: 8px;
  text-align: left; background: #fff; cursor: pointer; transition: all .2s;
}
.mode-btn:hover { border-color: #93c5fd; }
.mode-btn.active { border-color: #3b82f6; background: #eff6ff; }
.mode-label { display: block; font-weight: 600; font-size: 14px; }
.mode-desc { display: block; font-size: 12px; color: #64748b; margin-top: 4px; }
.touchpoint-row { padding: 12px; background: #f8fafc; border-radius: 8px; }
.toggle { width: 44px; height: 24px; border-radius: 12px; position: relative; cursor: pointer; transition: background .2s; border: none; }
.toggle-on { background: #16a34a; }
.toggle-off { background: #cbd5e1; }
.toggle-dot { position: absolute; top: 2px; width: 20px; height: 20px; border-radius: 50%; background: #fff; transition: left .2s; }
.toggle-on .toggle-dot { left: 22px; }
.toggle-off .toggle-dot { left: 2px; }
</style>
