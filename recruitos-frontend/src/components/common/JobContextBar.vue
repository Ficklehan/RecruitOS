<template>
  <div v-if="showBar" class="context-bar job-context-bar">
    <span class="ctx-label">当前在招职位</span>
    <el-select
      :model-value="modelValue"
      placeholder="选择在招职位"
      filterable
      clearable
      class="ctx-select"
      @update:model-value="onChange"
    >
      <el-option v-for="j in jobs" :key="j.id" :label="j.title" :value="j.id" />
    </el-select>
    <el-button v-if="modelValue" link type="primary" @click="openJobWorkspace">
      进入职位工作台
    </el-button>
    <div v-if="$slots.default" class="context-bar-actions">
      <slot />
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getJobList } from '@/api/modules/job'

const props = withDefaults(defineProps<{
  modelValue?: number | null
  showBar?: boolean
}>(), {
  showBar: true,
})

const emit = defineEmits<{ 'update:modelValue': [number | null] }>()

const router = useRouter()
const jobs = ref<any[]>([])

function onChange(id: number | null) {
  emit('update:modelValue', id)
}

function openJobWorkspace() {
  if (!props.modelValue) return
  router.push(`/planning/jobs/${props.modelValue}`)
}

onMounted(async () => {
  try {
    const res: any = await getJobList({ pageNum: 1, pageSize: 100, status: 'ACTIVE' })
    jobs.value = res.data?.list || res.data?.records || []
  } catch {
    jobs.value = []
  }
})
</script>

<style scoped lang="scss">
@import '@/assets/styles/variables.scss';

.job-context-bar {
  margin-bottom: 0;
}

.ctx-label {
  font-size: 13px;
  font-weight: 500;
  color: $text-secondary;
  flex-shrink: 0;
}

.ctx-select {
  flex: 1 1 280px;
  width: auto;
  min-width: 200px;
  max-width: none;
}
</style>
