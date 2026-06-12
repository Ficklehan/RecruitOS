<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getObservations, actionObservation, feedbackObservation, type ObservationItem } from '@/api/modules/brain'
import { RCard, RButton, RBadge, REmpty, RSkeleton } from '@/components/ui'
import { AlertTriangle, Check, ThumbsUp, ThumbsDown } from 'lucide-vue-next'

const props = defineProps<{ severity?: string }>()

const loading = ref(true)
const items = ref<ObservationItem[]>([])

const severityLabel: Record<string, string> = {
  CRITICAL: '需要处理', WARNING: '值得关注', INFO: '长期观察',
}
const severityColor: Record<string, string> = {
  CRITICAL: 'text-red-600 bg-red-50 border-red-200',
  WARNING: 'text-amber-600 bg-amber-50 border-amber-200',
  INFO: 'text-blue-600 bg-blue-50 border-blue-200',
}

async function load() {
  loading.value = true
  try {
    const res = await getObservations()
    const data = (res as any).data
    if (data) {
      if (props.severity === 'CRITICAL') items.value = data.critical || []
      else if (props.severity === 'WARNING') items.value = data.warnings || []
      else items.value = data.infos || []
    }
  } finally { loading.value = false }
}

async function handleAction(item: ObservationItem, action: string) {
  await actionObservation(item.id, action)
  load()
}

async function handleFeedback(item: ObservationItem, fb: string) {
  await feedbackObservation(item.id, fb)
}

onMounted(load)
</script>

<template>
  <div class="p-6 max-w-3xl mx-auto space-y-4">
    <div class="flex items-center gap-3 mb-6">
      <AlertTriangle :class="severity === 'CRITICAL' ? 'text-red-500' : severity === 'WARNING' ? 'text-amber-500' : 'text-blue-500'" class="h-6 w-6" />
      <h1 class="text-lg font-semibold text-text-primary">{{ severityLabel[severity || 'INFO'] || '洞察' }}</h1>
      <span class="text-sm text-text-placeholder">AI 主动推送</span>
    </div>

    <RSkeleton v-if="loading" class="h-32" />
    <REmpty v-else-if="!items.length" description="暂无待处理的洞察" />

    <RCard v-for="item in items" :key="item.id" class="p-4 border-l-4" :class="severityColor[item.severity] || ''">
      <div class="flex items-start justify-between gap-4">
        <div class="flex-1 min-w-0">
          <div class="flex items-center gap-2 mb-1">
            <RBadge :variant="item.severity === 'CRITICAL' ? 'danger' : item.severity === 'WARNING' ? 'warning' : 'default'" size="sm">
              {{ severityLabel[item.severity] }}
            </RBadge>
            <span class="text-xs text-text-placeholder">{{ item.createdAt?.substring(0, 16) }}</span>
          </div>
          <h3 class="text-sm font-semibold text-text-primary mb-1">{{ item.title }}</h3>
          <p class="text-sm text-text-secondary whitespace-pre-line">{{ item.body }}</p>
        </div>
      </div>

      <div v-if="item.suggestedAction" class="mt-3 pt-3 border-t border-divider flex items-center gap-2">
        <RButton size="sm" variant="default" @click="handleAction(item, 'EXECUTED')"><Check class="h-3.5 w-3.5 mr-1" /> 已处理</RButton>
        <RButton size="sm" variant="ghost" @click="handleAction(item, 'DISMISSED')">忽略</RButton>
        <RButton size="sm" variant="ghost" @click="handleAction(item, 'DEFERRED')">稍后</RButton>
        <div class="flex-1" />
        <RButton size="sm" variant="ghost" class="text-green-600" @click="handleFeedback(item, 'HELPFUL')"><ThumbsUp class="h-3.5 w-3.5" /></RButton>
        <RButton size="sm" variant="ghost" class="text-red-500" @click="handleFeedback(item, 'NOT_HELPFUL')"><ThumbsDown class="h-3.5 w-3.5" /></RButton>
      </div>
    </RCard>
  </div>
</template>
