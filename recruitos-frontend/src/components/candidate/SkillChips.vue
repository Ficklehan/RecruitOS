<template>
  <div v-if="skills.length" class="skill-chips" :class="{ compact }">
    <span
      v-for="s in visibleSkills"
      :key="s"
      class="chip"
    >{{ s }}</span>
    <span v-if="hiddenCount > 0" class="chip chip-more">+{{ hiddenCount }}</span>
    <el-button
      v-if="!compact && hiddenCount > 0 && !expanded"
      link
      type="primary"
      size="small"
      class="expand-btn"
      @click="expanded = true"
    >
      展开
    </el-button>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'

const props = withDefaults(defineProps<{
  skills: string[]
  limit?: number
  compact?: boolean
}>(), {
  limit: 6,
  compact: true,
})

const expanded = ref(false)

const effectiveLimit = computed(() => (expanded.value ? props.skills.length : props.limit))

const visibleSkills = computed(() => props.skills.slice(0, effectiveLimit.value))
const hiddenCount = computed(() => Math.max(0, props.skills.length - effectiveLimit.value))
</script>

<style scoped lang="scss">
.skill-chips {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 4px;
  line-height: 1.4;

  &.compact {
    flex-wrap: nowrap;
    overflow: hidden;
    max-width: 100%;
  }
}

.chip {
  display: inline-block;
  padding: 0 6px;
  font-size: 11px;
  line-height: 18px;
  color: #475569;
  background: #f1f5f9;
  border-radius: 4px;
  white-space: nowrap;
  flex-shrink: 0;
}

.chip-more {
  color: #64748b;
  background: transparent;
  padding-left: 2px;
}

.expand-btn {
  margin-left: 2px;
  padding: 0 4px;
  height: 18px;
}
</style>
