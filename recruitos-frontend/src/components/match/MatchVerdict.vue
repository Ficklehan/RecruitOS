<template>
  <div class="match-verdict" :class="[`mode-${mode}`, `tier-${verdict.status}`]">
    <div class="verdict-head">
      <el-tag :type="tierTagType(verdict.status)" size="small" effect="dark" class="verdict-badge">
        {{ verdict.label }}
      </el-tag>
      <span v-if="verdict.summary && mode !== 'compact'" class="verdict-summary">{{ verdict.summary }}</span>
    </div>

    <div v-if="mode !== 'compact'" class="verdict-body">
      <div v-if="verdict.pros?.length" class="verdict-block pros">
        <div class="block-title">符合点</div>
        <ul>
          <li v-for="(p, i) in verdict.pros" :key="'p'+i">{{ p }}</li>
        </ul>
      </div>
      <div v-if="verdict.cons?.length" class="verdict-block cons">
        <div class="block-title">待确认</div>
        <ul>
          <li v-for="(c, i) in verdict.cons" :key="'c'+i">{{ c }}</li>
        </ul>
      </div>
      <div v-if="verdict.suggestedAction" class="verdict-hint">
        <el-icon><InfoFilled /></el-icon>
        建议下一步：{{ verdict.suggestedAction }}
        <span v-if="verdict.autoActionAllowed === false" class="hint-muted">（需您确认后执行）</span>
      </div>
    </div>

    <div v-if="showScore && verdict.modelScore != null" class="verdict-score">
      参考分 {{ verdict.modelScore }}
      <span v-if="verdict.confidence" class="confidence">· 置信度 {{ confidenceLabel }}</span>
      <span class="hint-muted"> · 仅供辅助判断</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { InfoFilled } from '@element-plus/icons-vue'
import { parseMatchDetail, tierTagType } from '@/utils/matchVerdict'

const props = withDefaults(defineProps<{
  matchScore?: number | null
  matchDetail?: string | null
  mode?: 'compact' | 'card' | 'full'
  showScore?: boolean
}>(), {
  mode: 'card',
  showScore: false,
})

const verdict = computed(() => parseMatchDetail(props.matchDetail, props.matchScore))

const confidenceLabel = computed(() => {
  const m: Record<string, string> = { HIGH: '高', MEDIUM: '中', LOW: '低' }
  return m[verdict.value.confidence || ''] || verdict.value.confidence
})
</script>

<style scoped lang="scss">
.match-verdict {
  font-size: 13px;
  line-height: 1.5;
}

.verdict-head {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  flex-wrap: wrap;
}

.verdict-badge {
  flex-shrink: 0;
}

.verdict-summary {
  color: #64748b;
  font-size: 12px;
}

.verdict-body {
  margin-top: 10px;
}

.verdict-block {
  margin-bottom: 8px;
  ul {
    margin: 4px 0 0;
    padding-left: 18px;
  }
  .block-title {
    font-weight: 600;
    font-size: 12px;
  }
}

.pros .block-title { color: #059669; }
.cons .block-title { color: #d97706; }

.verdict-jd-hit {
  font-size: 12px;
  color: #475569;
  margin-bottom: 6px;
}

.verdict-hint {
  display: flex;
  align-items: flex-start;
  gap: 4px;
  padding: 8px 10px;
  background: #f1f5f9;
  border-radius: 6px;
  font-size: 12px;
  color: #475569;
  margin-top: 8px;
}

.hint-muted { color: #94a3b8; }

.verdict-score {
  margin-top: 8px;
  font-size: 11px;
  color: #94a3b8;
}

.mode-compact .verdict-body,
.mode-compact .verdict-score {
  display: none;
}
</style>
