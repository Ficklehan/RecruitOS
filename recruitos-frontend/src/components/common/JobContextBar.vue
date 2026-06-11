<script setup lang="ts">
import { onMounted, ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { cn } from '@/lib/utils'
import { RSelect, RButton } from '@/components/ui'
import { getJobList } from '@/api/modules/job'
import { ExternalLink } from 'lucide-vue-next'

interface Props {
  modelValue?: number | null
  showBar?: boolean
  class?: string
}

const props = withDefaults(defineProps<Props>(), {
  showBar: true,
})

const emit = defineEmits<{
  'update:modelValue': [number | null]
}>()

const router = useRouter()
const jobs = ref<any[]>([])

const jobOptions = computed(() =>
  jobs.value.map(j => ({ label: j.title, value: j.id }))
)

function onChange(id: number | string | null) {
  emit('update:modelValue', id as number | null)
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

<template>
  <div v-if="showBar" :class="cn('flex items-center gap-3 flex-wrap', props.class)">
    <span class="text-[13px] font-medium text-text-secondary shrink-0">当前在招职位</span>
    <RSelect
      :model-value="modelValue"
      :options="jobOptions"
      placeholder="选择在招职位"
      class="flex-1 min-w-[200px]"
      @update:model-value="onChange"
    />
    <RButton
      v-if="modelValue"
      variant="ghost"
      size="sm"
      class="text-primary"
      @click="openJobWorkspace"
    >
      进入职位工作台
      <ExternalLink class="ml-1 h-4 w-4" />
    </RButton>
    <slot />
  </div>
</template>
