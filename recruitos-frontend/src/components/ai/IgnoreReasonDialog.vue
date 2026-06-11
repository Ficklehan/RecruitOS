<template>
  <div v-if="visible" class="ird-overlay" @click.self="cancel">
    <div class="ird-dialog">
      <h4 class="ird-title">忽略此建议</h4>
      <p class="ird-subtitle">帮助我们改进 AI 建议质量</p>

      <div class="ird-options">
        <label v-for="opt in presetReasons" :key="opt.value" class="ird-option"
          :class="{ selected: selectedReason === opt.value }">
          <input type="radio" :value="opt.value" v-model="selectedReason" />
          <span>{{ opt.label }}</span>
        </label>
      </div>

      <textarea v-model="customNote" class="ird-textarea" placeholder="补充说明（可选）..."
        rows="2" maxlength="200"></textarea>

      <div class="ird-actions">
        <button class="ird-btn ird-btn-cancel" @click="cancel">取消</button>
        <button class="ird-btn ird-btn-submit" @click="submit" :disabled="!selectedReason">
          确认忽略
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { logIgnoreReason } from '../../api/modules/brain'

const props = defineProps<{
  visible: boolean
  touchpoint: string
  targetId?: number
  targetType?: string
}>()

const emit = defineEmits<{
  (e: 'close'): void
  (e: 'submitted', reason: string): void
}>()

const presetReasons = [
  { value: 'irrelevant', label: '建议与当前情况不匹配' },
  { value: 'disagree', label: '我不同意 AI 的判断' },
  { value: 'insufficient_context', label: 'AI 缺乏足够的上下文信息' },
  { value: 'too_aggressive', label: '建议过于激进' },
  { value: 'too_conservative', label: '建议过于保守' },
  { value: 'already_planned', label: '已有类似计划，不需要此建议' },
  { value: 'other', label: '其他原因' },
]

const selectedReason = ref('')
const customNote = ref('')

function cancel() {
  selectedReason.value = ''
  customNote.value = ''
  emit('close')
}

async function submit() {
  if (!selectedReason.value) return
  try {
    await logIgnoreReason({
      touchpoint: props.touchpoint,
      targetId: props.targetId,
      targetType: props.targetType || 'UNKNOWN',
      reason: selectedReason.value,
      note: customNote.value,
      userId: undefined,
    })
  } catch (e) { /* fire and forget */ }
  emit('submitted', selectedReason.value)
  selectedReason.value = ''
  customNote.value = ''
  emit('close')
}
</script>

<style scoped>
.ird-overlay {
  position: fixed; inset: 0; background: rgba(0,0,0,0.3);
  display: flex; align-items: center; justify-content: center; z-index: 1000;
}
.ird-dialog {
  background: var(--r-bg-primary, #fff);
  border-radius: 10px; padding: 24px; width: 400px; max-width: 90vw;
  box-shadow: 0 8px 30px rgba(0,0,0,0.12);
}
.ird-title { font-size: 16px; font-weight: 600; margin: 0 0 4px; }
.ird-subtitle { font-size: 13px; color: var(--r-text-secondary, #6b7280); margin: 0 0 16px; }

.ird-options { display: flex; flex-direction: column; gap: 6px; margin-bottom: 12px; }
.ird-option {
  display: flex; align-items: center; gap: 8px; padding: 8px 12px;
  border: 1px solid var(--r-border, #e5e7eb); border-radius: 6px;
  cursor: pointer; font-size: 13px; transition: border-color 0.15s;
}
.ird-option:hover { border-color: #6366f1; }
.ird-option.selected { border-color: #6366f1; background: #eef2ff; }
.ird-option input[type="radio"] { accent-color: #6366f1; }

.ird-textarea {
  width: 100%; padding: 8px 12px; border: 1px solid var(--r-border, #e5e7eb);
  border-radius: 6px; font-size: 13px; resize: vertical; font-family: inherit;
  box-sizing: border-box;
}
.ird-textarea:focus { outline: none; border-color: #6366f1; }

.ird-actions { display: flex; justify-content: flex-end; gap: 8px; margin-top: 16px; }
.ird-btn { padding: 8px 16px; border-radius: 6px; font-size: 13px; font-weight: 500; cursor: pointer; border: none; }
.ird-btn-cancel { background: var(--r-bg-secondary, #f3f4f6); color: var(--r-text-primary, #374151); }
.ird-btn-submit { background: #6366f1; color: #fff; }
.ird-btn-submit:disabled { opacity: 0.5; cursor: not-allowed; }
</style>
