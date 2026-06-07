<template>
  <div class="page-container">
    <!-- 页面头部 -->
    <div class="page-header">
      <h2 class="page-title">评价管理</h2>
      <el-button @click="goBack">
        <el-icon><ArrowLeft /></el-icon>
        返回
      </el-button>
    </div>

    <!-- 面试信息 -->
    <div class="info-card">
      <h3 class="section-title">面试信息</h3>
      <el-descriptions :column="3">
        <el-descriptions-item label="候选人">{{ interviewInfo.candidateName }}</el-descriptions-item>
        <el-descriptions-item label="岗位">{{ interviewInfo.jobTitle }}</el-descriptions-item>
        <el-descriptions-item label="面试轮次">
          <el-tag
            :type="interviewInfo.round === 'FIRST' ? 'primary' : 'success'"
            size="small"
            disable-transitions
          >
            {{ interviewInfo.round === 'FIRST' ? '初面' : '复试' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="面试官">{{ interviewInfo.interviewerName }}</el-descriptions-item>
        <el-descriptions-item label="面试时间">{{ interviewInfo.scheduledTime }}</el-descriptions-item>
        <el-descriptions-item label="面试形式">
          {{ interviewInfo.format === 'ONLINE' ? '线上' : '线下' }}
        </el-descriptions-item>
      </el-descriptions>
    </div>

    <!-- 评价表单 -->
    <div class="eval-card">
      <h3 class="section-title">评价内容</h3>

      <!-- 决策选择 -->
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
            <el-icon :size="20"><component :is="opt.icon" /></el-icon>
            <span>{{ opt.label }}</span>
          </div>
        </div>
      </div>

      <!-- 总分 -->
      <div class="form-section">
        <label class="form-label">总分</label>
        <div class="score-input-row">
          <el-input-number
            v-model="form.totalScore"
            :min="0"
            :max="100"
            :step="5"
            size="large"
          />
          <span class="score-hint">/ 100</span>
          <el-slider
            v-model="form.totalScore"
            :min="0"
            :max="100"
            :step="1"
            class="score-slider"
          />
        </div>
      </div>

      <!-- 评价维度 -->
      <div class="form-section">
        <label class="form-label">评价维度</label>
        <div class="dimensions-grid">
          <div v-for="dim in currentDimensions" :key="dim.key" class="dimension-card">
            <div class="dim-header">
              <span class="dim-name">{{ dim.label }}</span>
              <span class="dim-score">{{ form.dimensions[dim.key]?.score || 0 }}</span>
            </div>
            <el-slider
              v-model="form.dimensions[dim.key].score"
              :min="0"
              :max="100"
              :step="5"
              :marks="{ 0: '0', 25: '25', 50: '50', 75: '75', 100: '100' }"
            />
            <el-input
              v-model="form.dimensions[dim.key].comment"
              type="textarea"
              :rows="2"
              :placeholder="`请输入${dim.label}评语`"
              class="dim-comment"
            />
          </div>
        </div>
      </div>

      <!-- 进化反馈 -->
      <div class="form-section">
        <label class="form-label">进化反馈 <span class="optional-tag">选填</span></label>
        <div class="evolution-section">
          <div class="evo-group">
            <span class="evo-label">确认标签</span>
            <div class="tag-list">
              <el-check-tag
                v-for="tag in confirmTags"
                :key="tag"
                :checked="form.confirmTags.includes(tag)"
                @change="toggleTag('confirmTags', tag)"
              >
                {{ tag }}
              </el-check-tag>
            </div>
          </div>
          <div class="evo-group">
            <span class="evo-label">弱化标签</span>
            <div class="tag-list">
              <el-check-tag
                v-for="tag in weakenTags"
                :key="tag"
                :checked="form.weakenTags.includes(tag)"
                @change="toggleTag('weakenTags', tag)"
              >
                {{ tag }}
              </el-check-tag>
            </div>
          </div>
          <div class="evo-group">
            <span class="evo-label">新增标签</span>
            <div class="new-tags-input">
              <el-tag
                v-for="tag in form.newTags"
                :key="tag"
                closable
                @close="removeNewTag(tag)"
              >
                {{ tag }}
              </el-tag>
              <el-input
                v-if="newTagInputVisible"
                ref="newTagInputRef"
                v-model="newTagValue"
                size="small"
                style="width: 120px"
                @keyup.enter="addNewTag"
                @blur="addNewTag"
              />
              <el-button v-else size="small" @click="showNewTagInput">
                + 添加标签
              </el-button>
            </div>
          </div>
        </div>
      </div>

      <!-- 补充说明 -->
      <div class="form-section">
        <label class="form-label">补充说明</label>
        <el-input
          v-model="form.remark"
          type="textarea"
          :rows="4"
          placeholder="请输入补充说明，如候选人亮点、风险点、建议等"
        />
      </div>

      <!-- 底部按钮 -->
      <div class="form-footer">
        <el-button size="large" @click="handleSaveDraft">
          <el-icon><Document /></el-icon>
          保存草稿
        </el-button>
        <el-button type="primary" size="large" @click="handleSubmit" :loading="submitting">
          <el-icon><Check /></el-icon>
          提交评价
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getInterviewDetail, submitEvaluation, getEvaluation } from '@/api/modules/interview'

const router = useRouter()
const route = useRoute()

// 面试信息
const interviewInfo = reactive({
  id: 0,
  candidateName: '',
  jobTitle: '',
  round: 'FIRST',
  interviewerName: '',
  scheduledTime: '',
  format: 'ONLINE',
})

// 决策选项
const decisionOptions = [
  { value: 'PASS', label: '通过', icon: 'CircleCheck' },
  { value: 'PENDING', label: '待定', icon: 'Clock' },
  { value: 'REJECT', label: '淘汰', icon: 'CircleClose' },
]

// 评价维度
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

// 标签
const confirmTags = ['技术扎实', '沟通良好', '逻辑清晰', '学习能力强', '经验丰富', '团队协作']
const weakenTags = ['技术一般', '沟通欠佳', '逻辑混乱', '经验不足', '缺乏热情']

// 表单
const form = reactive({
  decision: 'PASS',
  totalScore: 80,
  dimensions: {} as Record<string, { score: number; comment: string }>,
  confirmTags: [] as string[],
  weakenTags: [] as string[],
  newTags: [] as string[],
  remark: '',
})

// 新标签输入
const newTagInputVisible = ref(false)
const newTagValue = ref('')
const newTagInputRef = ref<any>(null)

const submitting = ref(false)

// 初始化维度
function initDimensions() {
  const dims = interviewInfo.round === 'SECOND' ? secondRoundDimensions : firstRoundDimensions
  dims.forEach((dim) => {
    if (!form.dimensions[dim.key]) {
      form.dimensions[dim.key] = { score: 60, comment: '' }
    }
  })
}

// 切换标签
function toggleTag(field: 'confirmTags' | 'weakenTags', tag: string) {
  const idx = form[field].indexOf(tag)
  if (idx >= 0) {
    form[field].splice(idx, 1)
  } else {
    form[field].push(tag)
  }
}

// 新标签
function showNewTagInput() {
  newTagInputVisible.value = true
  nextTick(() => {
    newTagInputRef.value?.focus()
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

// 返回
function goBack() {
  router.back()
}

// 加载面试信息
async function loadInterviewInfo() {
  const interviewId = Number(route.query.interviewId)
  if (!interviewId) {
    ElMessage.warning('缺少面试ID参数')
    return
  }

  try {
    const res: any = await getInterviewDetail(interviewId)
    const data = res.data || res
    Object.assign(interviewInfo, {
      id: data.id,
      candidateName: data.candidateName,
      jobTitle: data.jobTitle,
      round: data.round || 'FIRST',
      interviewerName: data.interviewerName,
      scheduledTime: data.scheduledTime,
      format: data.format,
    })
  } catch (err: any) {
    ElMessage.error(err?.message || '加载面试信息失败')
    return
  }

  initDimensions()

  // 加载已有评价
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
    // 无已有评价，正常情况
  }
}

// 保存草稿
function handleSaveDraft() {
  ElMessage.success('草稿已保存')
}

// 提交评价
async function handleSubmit() {
  if (!form.decision) {
    ElMessage.warning('请选择决策')
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
    ElMessage.success('评价提交成功')
    router.push('/interview/board')
  } catch (err: any) {
    ElMessage.error(err?.message || '提交失败')
  } finally {
    submitting.value = false
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

// 决策按钮组
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
  font-size: 15px;
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

// 总分
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
    flex: 1;
    max-width: 400px;
  }
}

// 维度网格
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

  .dim-comment {
    margin-top: 8px;
  }
}

// 进化反馈
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

.new-tags-input {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

// 底部按钮
.form-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding-top: 24px;
  border-top: 1px solid $border-color-light;
}
</style>
