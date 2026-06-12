<template>
  <PageShell title="评价管理">
<div class="info-card">
      <h3 class="section-title">面试信息</h3>
      <div class="info-grid">
        <div class="info-item">
          <span class="info-label">候选人</span>
          <span class="info-value">{{ interviewInfo.candidateName }}</span>
        </div>
        <div class="info-item">
          <span class="info-label">岗位</span>
          <span class="info-value">{{ interviewInfo.jobTitle }}</span>
        </div>
        <div class="info-item">
          <span class="info-label">面试轮次</span>
          <RBadge :variant="interviewInfo.round === 'INITIAL' ? 'default' : 'secondary'">
            {{ interviewInfo.round === 'INITIAL' ? '初面' : '复试' }}
          </RBadge>
        </div>
        <div class="info-item">
          <span class="info-label">面试官</span>
          <span class="info-value">{{ interviewInfo.interviewerName }}</span>
        </div>
        <div class="info-item">
          <span class="info-label">面试时间</span>
          <span class="info-value">{{ interviewInfo.scheduledTime }}</span>
        </div>
        <div class="info-item">
          <span class="info-label">面试形式</span>
          <span class="info-value">{{ interviewInfo.format === 'ONLINE' ? '线上' : '线下' }}</span>
        </div>
      </div>
    </div>

    <!-- AI 追问建议 -->
    <div class="info-card">
      <div class="flex items-center justify-between mb-3">
        <div class="flex items-center gap-2">
          <Brain class="h-4 w-4 text-primary" />
          <h3 class="section-title !mb-0">AI 追问建议</h3>
          <RBadge variant="outline" class="text-[10px] px-1.5 py-0 h-4">实验性</RBadge>
        </div>
        <RButton
          size="sm"
          variant="outline"
          :disabled="aiQuestionsLoading"
          @click="loadAiQuestions"
        >
          <Sparkles v-if="!aiQuestionsLoading" class="mr-1 h-3.5 w-3.5" />
          <Loader2 v-else class="mr-1 h-3.5 w-3.5 animate-spin" />
          {{ aiQuestionsLoading ? '生成中...' : aiQuestions.length ? '重新生成' : '生成追问' }}
        </RButton>
      </div>
      <div v-if="aiQuestions.length" class="space-y-3">
        <div
          v-for="(q, idx) in aiQuestions"
          :key="idx"
          class="flex items-start gap-3 p-3 rounded-lg bg-bg-muted"
        >
          <span class="text-xs font-bold text-primary shrink-0 mt-0.5">{{ idx + 1 }}</span>
          <div class="min-w-0 flex-1">
            <p class="text-sm text-foreground leading-relaxed">{{ q.question || q }}</p>
            <p v-if="q.dimension" class="text-xs text-muted-foreground mt-1">
              考察: {{ q.dimension }}
            </p>
          </div>
        </div>
      </div>
      <div v-else-if="aiQuestionsError" class="text-sm text-muted-foreground py-2">
        {{ aiQuestionsError }}
      </div>
      <div v-else class="text-sm text-muted-foreground py-2">
        点击「生成追问」获取 AI 根据候选人简历和岗位要求定制的面试追问
      </div>
    </div>

    <div class="eval-card">
      <h3 class="section-title">评价内容</h3>

      <div class="form-section">
        <label class="form-label">决策</label>
        <div class="decision-group">
          <div
            v-for="opt in decisionOptions"
            :key="opt.value"
            class="decision-btn"
            :class="{ active: form.decision === opt.value, [opt.value.toLowerCase()]: true }"
            @click="form.decision = opt.value"
          >
            <component :is="opt.icon" class="h-5 w-5" />
            <span>{{ opt.label }}</span>
          </div>
        </div>
      </div>

      <div class="form-section">
        <label class="form-label">总分</label>
        <div class="score-input-row">
          <NumberInput v-model="form.totalScore" :min="0" :max="100" :step="5" class="w-24" />
          <span class="score-hint">/ 100</span>
          <RangeSlider v-model="form.totalScore" :min="0" :max="100" :step="1" class="score-slider flex-1" />
        </div>
      </div>

      <div class="form-section">
        <label class="form-label">评价维度</label>
        <div class="dimensions-grid">
          <div v-for="dim in currentDimensions" :key="dim.key" class="dimension-card">
            <div class="dim-header">
              <span class="dim-name">{{ dim.label }}</span>
              <span class="dim-score">{{ form.dimensions[dim.key]?.score || 0 }}</span>
            </div>
            <RangeSlider
              v-model="form.dimensions[dim.key].score"
              :min="0"
              :max="100"
              :step="5"
            />
            <RTextarea
              v-model="form.dimensions[dim.key].comment"
              :rows="2"
              :placeholder="`请输入${dim.label}评语`"
              class="dim-comment mt-2"
            />
          </div>
        </div>
      </div>

      <div class="form-section">
        <label class="form-label">进化反馈 <span class="optional-tag">选填</span></label>
        <div class="evolution-section">
          <div class="evo-group">
            <span class="evo-label">确认标签</span>
            <div class="tag-list">
              <button
                v-for="tag in confirmTags"
                :key="tag"
                type="button"
                class="tag-toggle"
                :class="{ active: form.confirmTags.includes(tag) }"
                @click="toggleTag('confirmTags', tag)"
              >
                {{ tag }}
              </button>
            </div>
          </div>
          <div class="evo-group">
            <span class="evo-label">弱化标签</span>
            <div class="tag-list">
              <button
                v-for="tag in weakenTags"
                :key="tag"
                type="button"
                class="tag-toggle"
                :class="{ active: form.weakenTags.includes(tag) }"
                @click="toggleTag('weakenTags', tag)"
              >
                {{ tag }}
              </button>
            </div>
          </div>
          <div class="evo-group">
            <span class="evo-label">新增标签</span>
            <div class="new-tags-input">
              <RBadge v-for="tag in form.newTags" :key="tag" variant="secondary" class="gap-1">
                {{ tag }}
                <button type="button" class="ml-1 text-muted-foreground hover:text-foreground" @click="removeNewTag(tag)">×</button>
              </RBadge>
              <RInput
                v-if="newTagInputVisible"
                ref="newTagInputRef"
                v-model="newTagValue"
                class="w-32 h-8"
                @keyup.enter="addNewTag"
                @blur="addNewTag"
              />
              <RButton v-else size="sm" variant="outline" @click="showNewTagInput">
                + 添加标签
              </RButton>
            </div>
          </div>
        </div>
      </div>

      <div class="form-section">
        <label class="form-label">补充说明</label>
        <RTextarea
          v-model="form.remark"
          :rows="4"
          placeholder="请输入补充说明，如候选人亮点、风险点、建议等"
        />
      </div>

      <div class="form-footer">
        <RButton size="lg" variant="outline" @click="handleSaveDraft">
          <FileText class="mr-2 h-4 w-4" />
          保存草稿
        </RButton>
        <RButton size="lg" :disabled="submitting" @click="handleSubmit">
          <Check class="mr-2 h-4 w-4" />
          提交评价
        </RButton>
      </div>
    </div>
</PageShell>
</template>

<script setup lang="ts">
import PageShell from '@/components/Layout/PageShell.vue'
import { ref, reactive, computed, onMounted, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ArrowLeft, Check, FileText, CircleCheck, Clock, CircleX, Brain, Sparkles, Loader2,
} from 'lucide-vue-next'
import { toast } from '@/lib/notify'
import RangeSlider from '@/components/app/RangeSlider.vue'
import NumberInput from '@/components/app/NumberInput.vue'
import { RButton, RBadge, RInput, RTextarea } from '@/components/ui'
import { getInterviewDetail, submitEvaluation, getEvaluation } from '@/api/modules/interview'
import request from '@/api/request'

const router = useRouter()
const route = useRoute()

const interviewInfo = reactive({
  id: 0,
  candidateName: '',
  jobTitle: '',
  round: 'INITIAL',
  interviewerName: '',
  scheduledTime: '',
  format: 'ONLINE',
})

const decisionOptions = [
  { value: 'PASS', label: '通过', icon: CircleCheck },
  { value: 'PENDING', label: '待定', icon: Clock },
  { value: 'REJECT', label: '淘汰', icon: CircleX },
]

const firstRoundDimensions = [
  { key: 'techDepth', label: '技术深度' },
  { key: 'systemDesign', label: '系统设计' },
  { key: 'coding', label: '编码能力' },
  { key: 'communication', label: '沟通表达' },
]

const secondRoundDimensions = [
  { key: 'overallAbility', label: '综合能力' },
  { key: 'cultureFit', label: '文化匹配' },
  { key: 'potential', label: '发展潜力' },
  { key: 'salaryExpectation', label: '薪酬预期' },
]

const currentDimensions = computed(() =>
  interviewInfo.round === 'SECOND' ? secondRoundDimensions : firstRoundDimensions
)

const confirmTags = ['技术扎实', '沟通良好', '逻辑清晰', '学习能力强', '经验丰富', '团队协作']
const weakenTags = ['技术一般', '沟通欠佳', '逻辑混乱', '经验不足', '缺乏热情']

const form = reactive({
  decision: 'PASS',
  totalScore: 80,
  dimensions: {} as Record<string, { score: number; comment: string }>,
  confirmTags: [] as string[],
  weakenTags: [] as string[],
  newTags: [] as string[],
  remark: '',
})

const newTagInputVisible = ref(false)
const newTagValue = ref('')
const newTagInputRef = ref<InstanceType<typeof RInput> | null>(null)

const submitting = ref(false)

function initDimensions() {
  const dims = interviewInfo.round === 'SECOND' ? secondRoundDimensions : firstRoundDimensions
  dims.forEach((dim) => {
    if (!form.dimensions[dim.key]) {
      form.dimensions[dim.key] = { score: 60, comment: '' }
    }
  })
}

function toggleTag(field: 'confirmTags' | 'weakenTags', tag: string) {
  const idx = form[field].indexOf(tag)
  if (idx >= 0) {
    form[field].splice(idx, 1)
  } else {
    form[field].push(tag)
  }
}

function showNewTagInput() {
  newTagInputVisible.value = true
  nextTick(() => {
    const el = newTagInputRef.value?.$el as HTMLInputElement | undefined
    el?.focus?.()
  })
}

function addNewTag() {
  if (newTagValue.value.trim() && !form.newTags.includes(newTagValue.value.trim())) {
    form.newTags.push(newTagValue.value.trim())
  }
  newTagInputVisible.value = false
  newTagValue.value = ''
}

function removeNewTag(tag: string) {
  form.newTags.splice(form.newTags.indexOf(tag), 1)
}

function goBack() {
  router.back()
}

async function loadInterviewInfo() {
  const interviewId = Number(route.query.interviewId)
  if (!interviewId) {
    toast.error('缺少面试ID参数')
    return
  }

  try {
    const res: any = await getInterviewDetail(interviewId)
    const data = res.data || res
    Object.assign(interviewInfo, {
      id: data.id,
      candidateName: data.candidateName,
      jobTitle: data.jobTitle,
      round: data.round || 'INITIAL',
      interviewerName: data.interviewerName,
      scheduledTime: data.scheduledTime,
      format: data.format,
    })
  } catch (err: any) {
    toast.error(err?.message || '加载面试信息失败')
    return
  }

  initDimensions()

  try {
    const evalRes: any = await getEvaluation(interviewId)
    const evalData = evalRes.data || evalRes
    if (evalData && evalData.decision) {
      form.decision = evalData.decision
      form.totalScore = evalData.totalScore ?? 80
      form.remark = evalData.remark || ''
      if (evalData.dimensions) {
        Object.assign(form.dimensions, evalData.dimensions)
      }
      if (evalData.confirmTags) form.confirmTags = evalData.confirmTags
      if (evalData.weakenTags) form.weakenTags = evalData.weakenTags
      if (evalData.newTags) form.newTags = evalData.newTags
    }
  } catch {
    // 无已有评价
  }
}

function handleSaveDraft() {
  toast.success('草稿已保存')
}

async function handleSubmit() {
  if (!form.decision) {
    toast.error('请选择决策')
    return
  }

  submitting.value = true
  try {
    await submitEvaluation({
      interviewId: interviewInfo.id || Number(route.query.interviewId),
      decision: form.decision,
      totalScore: form.totalScore,
      dimensions: form.dimensions,
      confirmTags: form.confirmTags,
      weakenTags: form.weakenTags,
      newTags: form.newTags,
      remark: form.remark,
    })
    toast.success('评价提交成功')
    router.push('/interview/board')
  } catch (err: any) {
    toast.error(err?.message || '提交失败')
  } finally {
    submitting.value = false
  }
}

// AI questions
const aiQuestions = ref<any[]>([])
const aiQuestionsLoading = ref(false)
const aiQuestionsError = ref('')

async function loadAiQuestions() {
  if (!interviewInfo.id || aiQuestionsLoading.value) return
  aiQuestionsLoading.value = true
  aiQuestionsError.value = ''
  try {
    const res = await request.get(`/api/evaluation/interview/${interviewInfo.id}/ai-questions`)
    const data = res?.data || res
    const questions = data?.questions
    if (typeof questions === 'string') {
      // fallback: split by newlines
      aiQuestions.value = questions.split('\n').filter((l: string) => l.trim()).map((l: string) => ({ question: l.trim() }))
    } else if (Array.isArray(questions)) {
      aiQuestions.value = questions
    } else {
      aiQuestions.value = []
    }
    if (!aiQuestions.value.length) {
      aiQuestionsError.value = 'AI暂未生成追问，请稍后重试'
    }
  } catch {
    aiQuestionsError.value = 'AI追问生成失败，请检查LLM服务状态'
  } finally {
    aiQuestionsLoading.value = false
  }
}

onMounted(() => {
  loadInterviewInfo()
})
</script>

<style lang="scss" scoped>
@import '@/assets/styles/variables.scss';
.info-card,
.eval-card {
  background: $bg-card;
  border-radius: 8px;
  box-shadow: 0 1px 6px rgba(0, 0, 0, 0.04);
  padding: 24px;
  margin-bottom: 20px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: $text-primary;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid $border-color-light;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.info-label {
  font-size: 12px;
  color: $text-secondary;
}

.info-value {
  font-size: 14px;
  color: $text-primary;
  font-weight: 500;
}

.form-section {
  margin-bottom: 28px;
}

.form-label {
  display: block;
  font-size: 14px;
  font-weight: 600;
  color: $text-primary;
  margin-bottom: 12px;

  .optional-tag {
    font-weight: 400;
    font-size: 12px;
    color: $text-placeholder;
    margin-left: 4px;
  }
}

.decision-group {
  display: flex;
  gap: 16px;
}

.decision-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 28px;
  border: 2px solid $border-color;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  color: $text-regular;
  transition: all 0.2s;

  &:hover {
    border-color: $text-placeholder;
  }

  &.active.pass {
    border-color: $success-color;
    background: $success-lighter;
    color: $success-color;
  }

  &.active.pending {
    border-color: $warning-color;
    background: $warning-lighter;
    color: $warning-color;
  }

  &.active.reject {
    border-color: $danger-color;
    background: $danger-lighter;
    color: $danger-color;
  }
}

.score-input-row {
  display: flex;
  align-items: center;
  gap: 12px;

  .score-hint {
    font-size: 14px;
    color: $text-secondary;
    min-width: 40px;
  }

  .score-slider {
    max-width: 400px;
  }
}

.dimensions-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;

  @media (max-width: 900px) {
    grid-template-columns: 1fr;
  }
}

.dimension-card {
  background: $bg-muted;
  border-radius: 8px;
  padding: 16px;

  .dim-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 8px;

    .dim-name {
      font-size: 14px;
      font-weight: 600;
      color: $text-primary;
    }

    .dim-score {
      font-size: 20px;
      font-weight: 700;
      color: $primary-color;
    }
  }
}

.evolution-section {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.evo-group {
  .evo-label {
    display: block;
    font-size: 13px;
    color: $text-regular;
    margin-bottom: 8px;
    font-weight: 500;
  }
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tag-toggle {
  padding: 4px 12px;
  border-radius: 6px;
  border: none;
  background: $bg-card;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.15s;

  &.active {
    border-color: $primary-color;
    background: rgba($primary-color, 0.1);
    color: $primary-color;
  }
}

.new-tags-input {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.form-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding-top: 24px;
  border-top: 1px solid $border-color-light;
}
</style>
