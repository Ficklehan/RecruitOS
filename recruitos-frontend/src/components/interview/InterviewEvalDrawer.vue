<template>
  <Sheet v-model:open="visible" side="right">
    <SheetContent class="!w-[520px] !max-w-[92vw] overflow-y-auto">
      <div class="mb-4">
        <h3 class="text-lg font-semibold">提交面试反馈</h3>
      </div>

      <div v-if="interview" class="relative">
        <div v-if="submitting" class="absolute inset-0 z-10 flex items-center justify-center bg-background/60">
          <Loader2 class="h-6 w-6 animate-spin text-primary" />
        </div>

        <p class="meta">{{ interview.candidateName }} · {{ interview.jobTitle }}</p>
        <p class="meta sub">{{ template.roundLabel }} · {{ interview.scheduledStartTime || '' }}</p>

        <FormField label="面试结论" class="form-section">
          <RadioGroup v-model="form.decision" class="flex flex-wrap gap-4">
            <label class="flex items-center gap-2 text-sm cursor-pointer">
              <RadioGroupItem value="PASS" />通过
            </label>
            <label class="flex items-center gap-2 text-sm cursor-pointer">
              <RadioGroupItem value="HOLD" />待定
            </label>
            <label class="flex items-center gap-2 text-sm cursor-pointer">
              <RadioGroupItem value="REJECT" />淘汰
            </label>
          </RadioGroup>
        </FormField>

        <FormField v-for="dim in template.dimensions" :key="dim.key" :label="dim.label" class="form-section">
          <div class="flex items-center gap-3">
            <input
              v-model.number="form.dimensionScores[dim.key]"
              type="range"
              min="0"
              max="100"
              class="flex-1"
            />
            <NumberInput v-model="form.dimensionScores[dim.key]" :min="0" :max="100" class="w-20" />
          </div>
        </FormField>

        <FormField label="总分（加权）" class="form-section">
          <div class="total-score">{{ weightedTotal }}</div>
        </FormField>

        <FormField label="综合评语" class="form-section">
          <Textarea v-model="form.remark" :rows="4" placeholder="请填写面试观察与结论" />
        </FormField>

        <Button class="submit-btn w-full" :disabled="submitting" @click="handleSubmit">
          <Loader2 v-if="submitting" class="mr-2 h-4 w-4 animate-spin" />
          提交反馈
        </Button>
      </div>
    </SheetContent>
  </Sheet>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { Loader2 } from 'lucide-vue-next'
import { toast } from '@/lib/notify'
import FormField from '@/components/app/FormField.vue'
import NumberInput from '@/components/app/NumberInput.vue'
import { Button, Textarea, RadioGroup, RadioGroupItem, Sheet, SheetContent } from '@/components/ui'
import { submitEvaluation, triggerNextRound } from '@/api/modules/interview'
import { computeWeightedScore, evalTemplateForRound } from '@/utils/interviewEvalTemplates'

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
const weightedTotal = computed(() => computeWeightedScore(template.value.dimensions, form.dimensionScores))

watch(() => props.modelValue, (v) => { visible.value = v })
watch(visible, (v) => {
  emit('update:modelValue', v)
  if (!v) onClosed()
})

watch(() => props.interview?.id, () => resetDimensionScores(), { immediate: true })

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
    toast.error('请选择面试结论')
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
    toast.success('面试反馈已提交')
    visible.value = false
    emit('submitted')
  } catch (e: any) {
    toast.error(e?.message || '提交失败')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.meta { margin: 0 0 4px; font-weight: 600; color: #0f172a; }
.meta.sub { font-weight: 400; color: #64748b; font-size: 13px; margin-bottom: 20px; }
.form-section { margin-bottom: 20px; }
.total-score { font-size: 28px; font-weight: 700; color: #3b82f6; }
.submit-btn { margin-top: 8px; }
</style>
