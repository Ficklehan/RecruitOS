<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getLessons } from '@/api/modules/brain'
import { RCard, RBadge, REmpty, RSkeleton } from '@/components/ui'
import { BookOpen, AlertTriangle, Shield, Zap } from 'lucide-vue-next'

const loading = ref(true)
const lessons = ref<any[]>([])

const severityIcon: Record<string, any> = {
  CRITICAL: AlertTriangle,
  IMPORTANT: Shield,
  NOTABLE: Zap,
}
const severityColor: Record<string, string> = {
  CRITICAL: 'border-red-200 text-red-600',
  IMPORTANT: 'border-amber-200 text-amber-600',
  NOTABLE: 'border-blue-200 text-blue-600',
}

async function load() {
  loading.value = true
  try {
    const res = await getLessons()
    lessons.value = (res as any).data || []
  } finally { loading.value = false }
}

function parseEvidence(s: string): any {
  try { return typeof s === 'string' ? JSON.parse(s) : s }
  catch { return null }
}

onMounted(load)
</script>

<template>
  <div class="p-6 max-w-3xl mx-auto space-y-6">
    <div class="flex items-center gap-3 mb-6">
      <BookOpen class="h-6 w-6 text-amber-500" />
      <div>
        <h1 class="text-lg font-semibold text-text-primary">教训库</h1>
        <p class="text-sm text-text-placeholder">从负面结果中提炼的规律，避免重蹈覆辙</p>
      </div>
    </div>

    <RSkeleton v-if="loading" class="h-32" />
    <REmpty v-else-if="!lessons.length"
      description="暂无教训记录。当系统发现录用表现不佳的共性模式时，会自动沉淀到此。" />

    <RCard v-for="(l, i) in lessons" :key="i" class="p-4 border-l-4" :class="severityColor[l.severity] || 'border-gray-200'">
      <div class="flex items-start gap-3">
        <component :is="severityIcon[l.severity] || AlertTriangle"
          class="h-5 w-5 shrink-0 mt-0.5"
          :class="l.severity === 'CRITICAL' ? 'text-red-500' : l.severity === 'IMPORTANT' ? 'text-amber-500' : 'text-blue-500'" />
        <div class="flex-1 min-w-0">
          <div class="flex items-center gap-2 mb-1">
            <RBadge :variant="l.severity === 'CRITICAL' ? 'danger' : l.severity === 'IMPORTANT' ? 'warning' : 'default'" size="sm">
              {{ l.severity === 'CRITICAL' ? '严重' : l.severity === 'IMPORTANT' ? '重要' : '一般' }}
            </RBadge>
            <span class="text-xs text-text-placeholder">{{ l.lessonType === 'BAD_HIRE_PATTERN' ? '不良录用模式' : l.lessonType === 'MISSED_GOOD_CANDIDATE' ? '错失好候选人' : l.lessonType === 'INTERVIEW_BLIND_SPOT' ? '面试盲区' : '流程失误' }}</span>
          </div>
          <h3 class="text-sm font-semibold text-text-primary mb-1">{{ l.title }}</h3>
          <p class="text-sm text-text-secondary">{{ l.description }}</p>
          <div v-if="l.correctiveAction" class="mt-2 p-2 bg-green-50 rounded text-sm text-green-700">
            💡 {{ l.correctiveAction }}
          </div>
          <p class="text-xs text-text-placeholder mt-2">发现于 {{ l.learnedAt?.substring(0, 10) }}</p>
        </div>
      </div>
    </RCard>

    <RCard v-if="!loading" class="p-4 bg-amber-50 border-amber-200">
      <h3 class="text-sm font-semibold text-amber-800 mb-2">📖 教训如何产生？</h3>
      <p class="text-sm text-amber-700">
        当员工离职时，AI 回溯其面试评价和入职表现，寻找可预防的信号。
        多个类似负面案例被聚合后，系统自动提炼为一条教训，包含预防建议。
        这不是追责工具，而是团队持续改进的机制。
      </p>
    </RCard>
  </div>
</template>
