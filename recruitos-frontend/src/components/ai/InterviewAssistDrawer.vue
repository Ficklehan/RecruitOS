<template>
  <Sheet :open="open" @update:open="$emit('update:open', $event)">
    <SheetContent side="right" class="!w-[420px] !max-w-[92vw] overflow-y-auto p-0">
      <div v-if="loading" class="flex items-center justify-center h-40"><Loader2 class="h-6 w-6 animate-spin text-muted-foreground" /></div>
      <template v-else-if="prep">
        <div class="p-5 border-b">
          <h3 class="font-semibold text-lg flex items-center gap-2"><Sparkles class="h-5 w-5 text-blue-500" />AI 面试辅助</h3>
          <p class="text-sm text-muted-foreground mt-1">{{ prep.candidateName }} · {{ prep.jobTitle }}</p>
          <Badge variant="outline" class="mt-2 text-xs">评估维度：{{ prep.evaluatorDimension }}（权重 {{ ((prep.dimensionWeight || 0) * 100).toFixed(0) }}%）</Badge>
        </div>

        <!-- 简历信号 -->
        <div v-if="prep.resumeSignals?.length" class="p-5 border-b">
          <p class="text-xs font-semibold text-muted-foreground mb-2">简历关键信号</p>
          <div class="space-y-2">
            <div v-for="sig in prep.resumeSignals" :key="sig.content" class="flex items-start gap-2 text-sm">
              <Badge :variant="sig.type === 'strong' ? 'default' : sig.type === 'concern' ? 'destructive' : 'outline'" class="text-xs shrink-0">
                {{ sig.type === 'strong' ? '强' : sig.type === 'concern' ? '⚠' : sig.type === 'moderate' ? '中' : '弱' }}
              </Badge>
              <span>{{ sig.content }}</span>
            </div>
          </div>
        </div>

        <!-- 建议追问 -->
        <div v-if="prep.suggestedQuestions?.length" class="p-5 border-b">
          <p class="text-xs font-semibold text-muted-foreground mb-2">AI 建议追问</p>
          <ul class="space-y-2">
            <li v-for="(q, i) in prep.suggestedQuestions" :key="i" class="text-sm flex gap-2">
              <span class="text-blue-500 shrink-0">{{ i + 1 }}.</span>
              <span>{{ q }}</span>
            </li>
          </ul>
        </div>

        <!-- 偏差提醒 -->
        <div v-if="prep.biasReminders?.length" class="p-5 border-b">
          <p class="text-xs font-semibold text-amber-600 mb-2">偏差提醒</p>
          <ul class="space-y-1">
            <li v-for="(b, i) in prep.biasReminders" :key="i" class="text-sm text-amber-700 flex gap-1">
              <span>•</span><span>{{ b }}</span>
            </li>
          </ul>
        </div>

        <!-- 注意事项 -->
        <div v-if="prep.cautions?.length" class="p-5 border-b">
          <p class="text-xs font-semibold text-muted-foreground mb-2">注意事项</p>
          <ul class="space-y-1">
            <li v-for="(c, i) in prep.cautions" :key="i" class="text-sm text-muted-foreground flex gap-1">
              <span>•</span><span>{{ c }}</span>
            </li>
          </ul>
        </div>

        <!-- 评分锚点 -->
        <div v-if="prep.scoreAnchors?.length" class="p-5 border-b">
          <p class="text-xs font-semibold text-muted-foreground mb-2">评分锚点（{{ prep.evaluatorDimension }}）</p>
          <div class="space-y-1.5">
            <div v-for="a in prep.scoreAnchors" :key="a.score" class="text-xs flex gap-2">
              <span :class="['shrink-0 w-5 h-5 rounded-full flex items-center justify-center text-white text-[10px] font-bold', scoreColor(a.score)]">{{ a.score }}</span>
              <span class="text-muted-foreground pt-0.5">{{ a.description }}</span>
            </div>
          </div>
        </div>

        <!-- 维度覆盖 -->
        <div v-if="prep.dimensionCoverage?.length" class="p-5">
          <p class="text-xs font-semibold text-muted-foreground mb-2">维度覆盖</p>
          <div class="space-y-2">
            <div v-for="d in prep.dimensionCoverage" :key="d.dimension" class="flex items-center justify-between text-sm">
              <span>{{ d.dimension }}</span>
              <span :class="d.covered ? 'text-green-600' : 'text-muted-foreground'">{{ d.covered ? `已覆盖 (${d.questionsAsked}题)` : '尚未覆盖' }}</span>
            </div>
          </div>
        </div>

        <div class="p-5 border-t">
          <Button class="w-full" @click="$emit('update:open', false)">关闭辅助面板</Button>
        </div>
      </template>
    </SheetContent>
  </Sheet>
</template>

<script setup lang="ts">
import { Sparkles, Loader2 } from 'lucide-vue-next'
import { Button, Badge, Sheet, SheetContent } from '@/components/ui'
import type { InterviewPrep } from '@/api/modules/brain'

defineProps<{ open: boolean; prep?: InterviewPrep | null; loading?: boolean }>()
defineEmits<{ 'update:open': [value: boolean] }>()

function scoreColor(s: number): string {
  if (s >= 5) return 'bg-green-600'
  if (s >= 4) return 'bg-blue-500'
  if (s >= 3) return 'bg-amber-500'
  return 'bg-red-500'
}
</script>
