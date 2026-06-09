<template>
  <el-drawer v-model="visible" title="提交面试反馈" size="520px" destroy-on-close @closed="onClosed">
    <div v-if="interview" v-loading="submitting">
      <p class="meta">{{ interview.candidateName }} · {{ interview.jobTitle }}</p>
      <p class="meta sub">{{ template.roundLabel }} · {{ interview.scheduledStartTime || '' }}</p>

      <div class="form-section">
        <label class="label">面试结论</label>
        <el-radio-group v-model="form.decision">
          <el-radio label="PASS">通过</el-radio>
          <el-radio label="HOLD">待定</el-radio>
          <el-radio label="REJECT">淘汰</el-radio>
        </el-radio-group>
      </div>

      <div v-for="dim in template.dimensions" :key="dim.key" class="form-section">
        <label class="label">{{ dim.label }}</label>
        <el-slider v-model="form.dimensionScores[dim.key]" :min="0" :max="100" show-input />
      </div>

      <div class="form-section">
        <label class="label">总分（加权）</label>
        <div class="total-score">{{ weightedTotal }}</div>
      </div>

      <div class="form-section">
        <label class="label">综合评语</label>
        <el-input v-model="form.remark" type="textarea" :rows="4" placeholder="请填写面试观察与结论" />
      </div>

      <el-button type="primary" class="submit-btn" :loading="submitting" @click="handleSubmit">
        提交反馈
      </el-button>
    </div>
  </el-drawer>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { submitEvaluation, triggerNextRound } from '@/api/modules/interview'
import {
  computeWeightedScore,
  evalTemplateForRound,
} from '@/utils/interviewEvalTemplates'

const props = defineProps<{
  modelValue: boolean
  interview: any | null
}>()

const emit = defineEmits<{ 'update:modelValue': [boolean]; submitted: [] }>()

const visible = ref(false)
const submitting = ref(false)
const form = reactive({
  decision: 'PASS',
  dimensionScores: {} as Record<string, number>,
  remark: '',
})

const template = computed(() => evalTemplateForRound(props.interview?.round))

const weightedTotal = computed(() =>
  computeWeightedScore(template.value.dimensions, form.dimensionScores),
)

watch(() => props.modelValue, (v) => { visible.value = v })
watch(visible, (v) => emit('update:modelValue', v))

watch(
  () => props.interview?.id,
  () => resetDimensionScores(),
  { immediate: true },
)

function resetDimensionScores() {
  form.dimensionScores = {}
  for (const d of template.value.dimensions) {
    form.dimensionScores[d.key] = 80
  }
}

function onClosed() {
  form.decision = 'PASS'
  form.remark = ''
  resetDimensionScores()
}

function buildDimensionsPayload() {
  return template.value.dimensions.map((d) => ({
    name: d.label,
    score: form.dimensionScores[d.key] ?? 0,
    weight: d.weight,
  }))
}

async function handleSubmit() {
  if (!props.interview?.id) return
  if (!form.decision) {
    ElMessage.warning('请选择面试结论')
    return
  }
  submitting.value = true
  try {
    await submitEvaluation({
      interviewId: props.interview.id,
      decision: form.decision,
      overallScore: weightedTotal.value,
      totalScore: weightedTotal.value,
      remark: form.remark,
      comment: form.remark,
      dimensions: buildDimensionsPayload(),
    })
    if (form.decision === 'PASS' && props.interview.round === 'INITIAL') {
      await triggerNextRound(props.interview.id).catch(() => null)
    }
    ElMessage.success('面试反馈已提交')
    visible.value = false
    emit('submitted')
  } catch (e: any) {
    ElMessage.error(e?.message || '提交失败')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.meta { margin: 0 0 4px; font-weight: 600; color: #0f172a; }
.meta.sub { font-weight: 400; color: #64748b; font-size: 13px; margin-bottom: 20px; }
.form-section { margin-bottom: 20px; }
.label { display: block; font-size: 13px; color: #475569; margin-bottom: 8px; }
.total-score { font-size: 28px; font-weight: 700; color: #3b82f6; }
.submit-btn { width: 100%; margin-top: 8px; }
</style>
