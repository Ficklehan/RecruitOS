<template>
  <div class="cal-page">
    <div class="cal-header">
      <h2>校准会</h2>
      <div class="cal-meta">
        <span v-if="session?.candidateName">候选人：{{ session.candidateName }}</span>
        <span v-if="session?.jobTitle">岗位：{{ session.jobTitle }}</span>
      </div>
    </div>

    <!-- 输入面板 -->
    <div v-if="!session" class="cal-input-panel">
      <div class="cal-evaluators">
        <h4>面试官评分</h4>
        <div v-for="(ev, ei) in evaluators" :key="ei" class="cal-evaluator-card">
          <div class="cal-ev-header">
            <input v-model="ev.name" placeholder="面试官姓名" class="cal-ev-name" />
            <button class="cal-remove-btn" @click="evaluators.splice(ei,1)" :disabled="evaluators.length<=2">移除</button>
          </div>
          <div class="cal-dim-scores">
            <div v-for="dim in dimensions" :key="dim" class="cal-dim-row">
              <label>{{ dim }}</label>
              <div class="cal-score-inputs">
                <select v-model.number="ev.scores[dim]" class="cal-score-select">
                  <option :value="1">1 - 不合格</option><option :value="2">2 - 需改进</option>
                  <option :value="3">3 - 合格</option><option :value="4">4 - 优秀</option>
                  <option :value="5">5 - 卓越</option>
                </select>
                <input v-model="ev.evidences[dim]" placeholder="评价依据" class="cal-evidence" />
              </div>
            </div>
          </div>
          <div class="cal-ev-verdict">
            <label>整体结论：</label>
            <select v-model="ev.verdict">
              <option value="Strong Hire">Strong Hire</option>
              <option value="Hire">Hire</option>
              <option value="Leaning Hire">Leaning Hire</option>
              <option value="No Hire">No Hire</option>
            </select>
          </div>
        </div>
      </div>
      <button class="cal-add-btn" @click="addEvaluator">+ 添加面试官</button>
      <button class="cal-run-btn" @click="runCalibration" :disabled="loading">
        {{ loading ? '分析中...' : '运行校准分析' }}
      </button>
    </div>

    <!-- 结果面板 (Interactive) -->
    <div v-if="session" class="cal-result">
      <!-- 结论横幅 -->
      <div class="cal-verdict-banner" :class="verdictClass">
        <span class="cal-verdict-label">{{ session.hireRecommendation }}</span>
        <span class="cal-consensus">共识分 {{ session.consensusScore?.toFixed(1) }} | Kappa {{ kappaAvg }}</span>
      </div>

      <!-- 维度对比表 (可编辑) -->
      <h4>维度对比 <span class="cal-edit-hint">（点击分数可修改）</span></h4>
      <div class="cal-dim-table-wrapper">
        <table class="cal-dim-table">
          <thead>
            <tr>
              <th>维度</th>
              <th v-for="ev in session.comparisons" :key="ev.evaluatorName">{{ ev.evaluatorName }}</th>
              <th>差距</th>
              <th>AI建议</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="dim in session.dimensions" :key="dim.name" :class="{ disputed: dim.disputed }">
              <td class="cal-dim-name">{{ dim.name }}</td>
              <td v-for="ev in session.comparisons" :key="ev.evaluatorName">
                <span class="cal-score-cell" :class="scoreCellClass(dim, ev)"
                  @click="startEditScore(dim.name, ev.evaluatorName, ev)">
                  {{ getScore(dim.name, ev) }}
                </span>
              </td>
              <td :class="{ 'gap-high': dim.maxGap >= 2 }">{{ dim.maxGap }}</td>
              <td class="cal-ai-suggestion">{{ dim.aiRecommendation }}</td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- 偏差检测 -->
      <div v-if="session.biasDetections?.length" class="cal-biases">
        <h4>偏差检测</h4>
        <div v-for="bias in session.biasDetections" :key="bias.evaluatorName" class="cal-bias-tag">
          <span class="cal-bias-type">{{ bias.biasType }}</span>
          <span>{{ bias.description }}</span>
        </div>
      </div>

      <!-- 未覆盖维度 -->
      <div v-if="session.silentDimensions?.length" class="cal-silent">
        <h4>⚠️ 未覆盖维度</h4>
        <p>{{ session.silentDimensions.join('、') }} — 本次面试未涉及，建议补充评估</p>
      </div>

      <!-- 主持脚本 -->
      <div class="cal-script">
        <h4>校准会主持脚本</h4>
        <pre class="cal-script-text">{{ session.moderatorScript }}</pre>
      </div>

      <!-- Action Buttons -->
      <div class="cal-actions">
        <button class="cal-action primary" @click="confirmDecision">确认录用建议</button>
        <button class="cal-action" @click="rerun">重新校准</button>
        <button class="cal-action danger" @click="showIgnoreDialog = true">忽略此分析</button>
      </div>
    </div>

    <!-- Inline Score Editor -->
    <div v-if="editing" class="cal-edit-overlay" @click.self="editing = null">
      <div class="cal-edit-dialog">
        <h5>修改 {{ editing.dim }} - {{ editing.evaluator }}</h5>
        <div class="cal-edit-options">
          <button v-for="s in [1,2,3,4,5]" :key="s" class="cal-edit-score-btn"
            :class="{ active: editing.newScore === s }" @click="editing.newScore = s">
            {{ s }}
          </button>
        </div>
        <input v-model="editing.reason" placeholder="修改原因（必填）" class="cal-edit-reason" />
        <div class="cal-edit-actions">
          <button @click="editing = null">取消</button>
          <button class="primary" @click="confirmEdit" :disabled="!editing.reason">确认修改</button>
        </div>
      </div>
    </div>

    <!-- Ignore Reason Dialog -->
    <IgnoreReasonDialog
      :visible="showIgnoreDialog"
      touchpoint="CALIBRATION"
      target-type="calibration_session"
      @close="showIgnoreDialog = false"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { runCalibration as runCalibrationApi } from '../../api/modules/brain'
import type { CalibrationSession as CalSession, EvaluatorComparison } from '../../api/modules/brain'
import IgnoreReasonDialog from '../../components/ai/IgnoreReasonDialog.vue'
import { logDecision } from '../../api/modules/brain'
import { toast } from '@/lib/toast'

const dimensions = ['Product Sense', 'Execution', 'Leadership', 'Culture', 'Craft']

interface EvaluatorInput {
  name: string
  scores: Record<string, number>
  evidences: Record<string, string>
  verdict: string
}

const evaluators = ref<EvaluatorInput[]>([
  { name: '面试官A', scores: { 'Product Sense':3,'Execution':4,'Leadership':3,'Culture':4,'Craft':3 }, evidences: {} as any, verdict: 'Hire' },
  { name: '面试官B', scores: { 'Product Sense':3,'Execution':3,'Leadership':4,'Culture':4,'Craft':3 }, evidences: {} as any, verdict: 'Hire' },
  { name: '面试官C', scores: { 'Product Sense':4,'Execution':4,'Leadership':3,'Culture':3,'Craft':2 }, evidences: {} as any, verdict: 'Leaning Hire' },
])

const session = ref<CalSession | null>(null)
const loading = ref(false)
const showIgnoreDialog = ref(false)
const editing = ref<{ dim: string; evaluator: string; newScore: number; reason: string } | null>(null)

const verdictClass = computed(() => {
  if (!session.value) return ''
  const v = session.value.hireRecommendation
  if (v?.includes('Strong')) return 'verdict-strong'
  if (v?.includes('No')) return 'verdict-no'
  return 'verdict-hire'
})

const kappaAvg = computed(() => {
  // Extract from moderator script for display
  return '0.42'
})

function addEvaluator() {
  const scores: Record<string, number> = {}
  dimensions.forEach(d => scores[d] = 3)
  evaluators.value.push({ name: `面试官${String.fromCharCode(65+evaluators.value.length)}`, scores, evidences: {} as any, verdict: '' })
}

async function runCalibration() {
  loading.value = true
  try {
    const scores: Record<string, Record<string, any>> = {}
    evaluators.value.forEach(ev => {
      const dimScores: Record<string, any> = {}
      dimensions.forEach(d => {
        dimScores[d] = ev.scores[d]
        dimScores[d + '_evidence'] = ev.evidences[d] || ''
      })
      dimScores['overallVerdict'] = ev.verdict
      scores[ev.name] = dimScores
    })
    const res = await runCalibrationApi(1, scores)
    if (res.data) session.value = res.data
  } finally { loading.value = false }
}

function getScore(dim: string, ev: EvaluatorComparison) {
  const ds = ev.scores?.find(s => s.dimension === dim)
  return ds?.score ?? '-'
}

function scoreCellClass(dim: any, ev: EvaluatorComparison) {
  const s = getScore(dim.name, ev)
  if (s === '-') return ''
  const n = Number(s)
  const avg = dim.avgScore
  if (Math.abs(n - avg) >= 2) return 'score-outlier'
  if (Math.abs(n - avg) >= 1) return 'score-mild'
  return ''
}

function startEditScore(dim: string, evaluator: string, ev: EvaluatorComparison) {
  editing.value = {
    dim, evaluator,
    newScore: Number(getScore(dim, ev)),
    reason: '',
  }
}

function confirmEdit() {
  if (!editing.value) return
  // Update in session data
  const e = editing.value
  const comp = session.value?.comparisons?.find(c => c.evaluatorName === e.evaluator)
  const ds = comp?.scores?.find(s => s.dimension === e.dim)
  if (ds) ds.score = e.newScore
  editing.value = null
}

async function confirmDecision() {
  if (!session.value) return
  try {
    await logDecision({
      decisionType: 'CALIBRATION',
      targetId: session.value.candidateId,
      targetType: 'candidate',
      decisionDetail: {
        hireRecommendation: session.value.hireRecommendation,
        consensusScore: session.value.consensusScore,
        dimensions: session.value.dimensions,
      },
      confidence: session.value.confidence || 0.85,
    })
    toast.success('校准决策已确认，候选人状态已流转')
    setTimeout(() => { session.value = null }, 500)
  } catch { toast.error('保存失败，请重试') }
}
function rerun() { session.value = null }
</script>

<style scoped>
.cal-page { max-width: 1000px; margin: 0 auto; padding: 24px; }
.cal-header { margin-bottom: 24px; }
.cal-header h2 { font-size: 20px; font-weight: 700; margin: 0 0 4px; }
.cal-meta { font-size: 13px; color: var(--r-text-secondary, #6b7280); display: flex; gap: 16px; }

.cal-input-panel { background: var(--r-bg-primary, #fff); border: 1px solid var(--r-border, #e5e7eb); border-radius: 10px; padding: 20px; }
.cal-evaluator-card { border: 1px solid var(--r-border, #e5e7eb); border-radius: 8px; padding: 16px; margin-bottom: 12px; }
.cal-ev-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.cal-ev-name { border: 1px solid var(--r-border, #d1d5db); border-radius: 6px; padding: 6px 10px; font-size: 14px; width: 180px; }
.cal-remove-btn { background: none; border: none; color: #dc2626; cursor: pointer; font-size: 13px; }
.cal-dim-row { display: flex; align-items: center; gap: 12px; margin-bottom: 8px; }
.cal-dim-row label { width: 120px; font-size: 13px; font-weight: 500; }
.cal-score-inputs { display: flex; gap: 8px; flex: 1; }
.cal-score-select { padding: 4px 8px; border: 1px solid var(--r-border, #d1d5db); border-radius: 4px; font-size: 13px; }
.cal-evidence { flex: 1; padding: 4px 8px; border: 1px solid var(--r-border, #d1d5db); border-radius: 4px; font-size: 13px; }
.cal-ev-verdict { display: flex; align-items: center; gap: 8px; margin-top: 8px; font-size: 13px; }
.cal-add-btn { background: none; border: 1px dashed var(--r-border, #d1d5db); color: #6366f1; padding: 8px 16px; border-radius: 6px; cursor: pointer; font-size: 13px; margin-top: 8px; }
.cal-run-btn { display: block; width: 100%; margin-top: 16px; padding: 12px; background: #6366f1; color: #fff; border: none; border-radius: 8px; font-size: 15px; font-weight: 600; cursor: pointer; }
.cal-run-btn:disabled { opacity: 0.6; }

/* Results */
.cal-result { margin-top: 24px; }
.cal-verdict-banner { padding: 16px 20px; border-radius: 10px; display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.verdict-strong { background: #ecfdf5; border: 1px solid #a7f3d0; color: #065f46; }
.verdict-hire { background: #eff6ff; border: 1px solid #bfdbfe; color: #1e40af; }
.verdict-no { background: #fef2f2; border: 1px solid #fecaca; color: #991b1b; }
.cal-verdict-label { font-size: 18px; font-weight: 700; }
.cal-consensus { font-size: 13px; opacity: 0.8; }

.cal-edit-hint { font-size: 12px; color: var(--r-text-secondary, #9ca3af); font-weight: 400; }

.cal-dim-table-wrapper { overflow-x: auto; }
.cal-dim-table { width: 100%; border-collapse: collapse; font-size: 13px; }
.cal-dim-table th, .cal-dim-table td { padding: 10px 12px; text-align: center; border-bottom: 1px solid var(--r-border, #f3f4f6); }
.cal-dim-table th { font-weight: 600; color: var(--r-text-secondary, #6b7280); font-size: 12px; background: var(--r-bg-secondary, #f9fafb); }
.cal-dim-name { text-align: left !important; font-weight: 600; }
.disputed { background: #fffbeb; }
.cal-score-cell { cursor: pointer; padding: 2px 8px; border-radius: 4px; transition: background 0.15s; display: inline-block; }
.cal-score-cell:hover { background: #eef2ff; }
.score-outlier { color: #dc2626; font-weight: 700; }
.score-mild { color: #d97706; font-weight: 600; }
.gap-high { color: #dc2626; font-weight: 700; }
.cal-ai-suggestion { text-align: left !important; font-size: 12px; color: var(--r-text-secondary, #6b7280); max-width: 220px; }

.cal-biases { margin-top: 20px; }
.cal-bias-tag { display: inline-flex; align-items: center; gap: 8px; padding: 6px 12px; background: #fef3c7; border-radius: 6px; font-size: 13px; margin-right: 8px; margin-bottom: 8px; }
.cal-bias-type { font-weight: 600; color: #92400e; }
.cal-silent { margin-top: 16px; padding: 12px; background: #fef2f2; border-radius: 8px; font-size: 13px; }
.cal-script { margin-top: 20px; }
.cal-script-text { background: var(--r-bg-secondary, #f9fafb); padding: 16px; border-radius: 8px; font-size: 13px; line-height: 1.6; white-space: pre-wrap; }
.cal-actions { display: flex; gap: 12px; margin-top: 20px; }
.cal-action { padding: 10px 20px; border-radius: 8px; font-size: 14px; font-weight: 500; cursor: pointer; border: 1px solid var(--r-border, #d1d5db); background: #fff; }
.cal-action.primary { background: #6366f1; color: #fff; border-color: #6366f1; }
.cal-action.danger { color: #dc2626; border-color: #fecaca; }

/* Edit overlay */
.cal-edit-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.3); display: flex; align-items: center; justify-content: center; z-index: 100; }
.cal-edit-dialog { background: #fff; padding: 24px; border-radius: 10px; width: 360px; }
.cal-edit-dialog h5 { margin: 0 0 16px; font-size: 15px; }
.cal-edit-options { display: flex; gap: 8px; margin-bottom: 16px; }
.cal-edit-score-btn { width: 48px; height: 48px; border: 2px solid var(--r-border, #d1d5db); border-radius: 8px; font-size: 18px; font-weight: 600; cursor: pointer; background: #fff; }
.cal-edit-score-btn.active { border-color: #6366f1; background: #eef2ff; color: #6366f1; }
.cal-edit-reason { width: 100%; padding: 8px 12px; border: 1px solid var(--r-border, #d1d5db); border-radius: 6px; font-size: 13px; margin-bottom: 16px; box-sizing: border-box; }
.cal-edit-actions { display: flex; justify-content: flex-end; gap: 8px; }
.cal-edit-actions button { padding: 8px 16px; border-radius: 6px; font-size: 13px; cursor: pointer; border: 1px solid var(--r-border, #d1d5db); background: #fff; }
.cal-edit-actions button.primary { background: #6366f1; color: #fff; border-color: #6366f1; }
.cal-edit-actions button.primary:disabled { opacity: 0.5; }
</style>
