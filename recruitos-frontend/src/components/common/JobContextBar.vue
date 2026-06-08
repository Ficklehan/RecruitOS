<template>
  <div v-if="showBar" class="job-context-bar">
    <span class="ctx-label">当前在招职位</span>
    <el-select
      :model-value="modelValue"
      placeholder="选择在招职位"
      filterable
      clearable
      style="width: min(360px, 100%)"
      @update:model-value="onChange"
    >
      <el-option v-for="j in jobs" :key="j.id" :label="j.title" :value="j.id" />
    </el-select>
    <el-button v-if="modelValue" link type="primary" @click="openJobWorkspace">
      进入职位工作台
    </el-button>
    <slot />
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
.job-context-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  padding: 12px 16px;
  margin-bottom: 16px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
}
.ctx-label {
  font-size: 13px;
  color: #64748b;
  flex-shrink: 0;
}
</style>
