<template>
  <RPageShell title="发起招聘需求" subtitle="描述业务目标，AI帮你诊断缺口、生成方案">
    <!-- Step 1 -->
    <div v-if="step === 1" class="max-w-2xl">
      <RCard padding="lg" class="mb-6">
        <h3 class="text-[16px] font-semibold text-text-primary mb-2">描述业务需求</h3>
        <p class="text-[13px] text-text-secondary mb-4">
          不需要想「该招什么人」，只需要说清楚业务目标。AI会分析团队现状，告诉你要招什么样的人。
        </p>
        <FormField label="业务目标" required>
          <RTextarea v-model="form.businessObjective" :rows="4" placeholder="例如：支付团队Q4要承载10倍交易量，现在4个人，都没做过大规模分布式系统..." />
        </FormField>
        <FormField label="所属部门" class="mt-4">
          <RSelect v-model="form.departmentId" :options="deptOptions" placeholder="选择部门（可选）" class="w-64" />
        </FormField>
        <div class="flex gap-3 mt-6">
          <RButton :disabled="!form.businessObjective.trim()" :loading="loading" @click="handleAnalyze">
            <Sparkles class="mr-2 h-4 w-4" />
            AI 分析缺口
          </RButton>
          <RButton variant="outline" @click="$router.back()">取消</RButton>
        </div>
      </RCard>

      <RCard variant="flat" padding="md" class="!bg-primary-light/30 !border-primary/10">
        <div class="flex items-start gap-3">
          <Lightbulb class="h-5 w-5 text-primary shrink-0 mt-0.5" />
          <div class="text-[13px] text-text-primary">
            <p class="font-medium mb-1">怎么写效果最好？</p>
            <ul class="list-disc pl-4 space-y-1 text-text-secondary">
              <li>描述业务目标，而不是岗位名称（说「Q4要扛10倍交易量」，不说「招个P7后端」）</li>
              <li>提到现有团队的情况（人数、级别、擅长什么）</li>
              <li>如果有时间和预算约束，也写出来</li>
            </ul>
          </div>
        </div>
      </RCard>
    </div>

    <!-- Step 2 -->
    <div v-if="step === 2 && analysis" class="max-w-3xl">
      <RCard padding="md" class="mb-6">
        <h3 class="text-[16px] font-semibold text-text-primary mb-4">团队能力现状</h3>
        <div class="overflow-x-auto">
          <table class="w-full text-[13px]">
            <thead>
              <tr class="border-b border-divider">
                <th class="text-left py-2 pr-4 text-text-secondary font-medium">成员</th>
                <th class="text-left py-2 px-4 text-text-secondary font-medium">级别</th>
                <th class="text-left py-2 px-4 text-text-secondary font-medium">技能</th>
                <th class="text-center py-2 pl-4 text-text-secondary font-medium w-[100px]">缺口相关</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="m in analysis.currentTeam" :key="m.name" class="border-b border-divider last:border-0">
                <td class="py-3 pr-4 font-medium text-text-primary">{{ m.name }}</td>
                <td class="py-3 px-4 text-text-primary">{{ m.level }}</td>
                <td class="py-3 px-4 text-text-secondary">{{ m.skills.join('、') }}</td>
                <td class="py-3 pl-4 text-center">
                  <RBadge :variant="m.hasGapSkill ? 'danger' : 'outline'">{{ m.hasGapSkill ? '缺' : '有' }}</RBadge>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </RCard>

      <RCard padding="md" class="mb-6">
        <h3 class="text-[16px] font-semibold text-text-primary mb-2">能力缺口</h3>
        <p class="text-[13px] text-text-secondary mb-4">业务目标要求 vs 团队当前覆盖</p>
        <div class="space-y-4">
          <div v-for="gap in analysis.capabilityGaps" :key="gap.skill" class="p-3 rounded-[var(--r-radius)] bg-bg-muted">
            <div class="flex justify-between mb-1">
              <span class="font-medium text-[13px] text-text-primary">{{ gap.skill }}</span>
              <RBadge :variant="gap.severity === 'critical' ? 'danger' : gap.severity === 'major' ? 'warning' : 'success'">
                {{ gap.severity === 'critical' ? '严重缺口' : gap.severity === 'major' ? '较大缺口' : '可接受' }}
              </RBadge>
            </div>
            <RProgress :value="gap.coverage" class="h-1.5" />
            <div class="flex justify-between text-[12px] text-text-secondary mt-1">
              <span>当前：{{ gap.current }}</span>
              <span>需要：{{ gap.required }}</span>
            </div>
          </div>
        </div>
      </RCard>

      <RCard padding="md" class="!bg-primary-light/50 !border-primary/20 mb-6">
        <div class="flex items-center gap-2 mb-4">
          <Sparkles class="h-5 w-5 text-primary" />
          <h3 class="text-[16px] font-semibold text-text-primary">AI 招聘建议</h3>
          <RBadge variant="outline">置信度 {{ (analysis.confidence * 100).toFixed(0) }}%</RBadge>
        </div>

        <div class="space-y-3 mb-4">
          <div class="flex gap-2">
            <span class="text-text-secondary w-20 shrink-0 text-[13px]">建议岗位</span>
            <span class="font-semibold text-[14px] text-text-primary">{{ analysis.recommendation.suggestedTitle }} · {{ analysis.recommendation.suggestedLevel }}</span>
          </div>
          <div class="flex gap-2">
            <span class="text-text-secondary w-20 shrink-0 text-[13px]">预算范围</span>
            <span class="text-[14px] text-text-primary">{{ analysis.recommendation.budgetRange }}</span>
          </div>
          <div class="flex gap-2">
            <span class="text-text-secondary w-20 shrink-0 text-[13px]">建议渠道</span>
            <span class="text-[14px] text-text-primary">{{ analysis.recommendation.suggestedChannels.join('、') }}</span>
          </div>
        </div>

        <div class="bg-bg-card rounded-[var(--r-radius)] p-4 mb-4">
          <p class="font-medium text-[14px] text-text-primary mb-2">为什么是这个岗位？</p>
          <p class="text-[13px] text-text-secondary">{{ analysis.recommendation.reasoning }}</p>
        </div>

        <div class="grid grid-cols-2 gap-4 mb-4">
          <div>
            <p class="font-medium text-[13px] text-text-primary mb-2">必备能力</p>
            <ul class="list-disc pl-4 text-[13px] space-y-1 text-text-secondary">
              <li v-for="s in analysis.recommendation.mustHaveSkills" :key="s">{{ s }}</li>
            </ul>
          </div>
          <div>
            <p class="font-medium text-[13px] text-text-primary mb-2">加分能力</p>
            <ul class="list-disc pl-4 text-[13px] space-y-1 text-text-secondary">
              <li v-for="s in analysis.recommendation.niceToHaveSkills" :key="s">{{ s }}</li>
            </ul>
          </div>
        </div>

        <div>
          <p class="font-medium text-[13px] text-text-primary mb-2">面试重点维度</p>
          <div class="space-y-2">
            <div v-for="dim in analysis.recommendation.interviewDimensions" :key="dim.name" class="flex items-center gap-2 text-[13px]">
              <span class="w-32 text-text-primary">{{ dim.name }}</span>
              <RProgress :value="dim.weight * 100" class="h-1.5 flex-1" />
              <span class="text-text-secondary w-10 text-right">{{ (dim.weight * 100).toFixed(0) }}%</span>
            </div>
          </div>
        </div>
      </RCard>

      <div class="flex gap-3">
        <RButton @click="handleConfirm">确认，生成招聘方案</RButton>
        <RButton variant="outline" @click="step = 1">调整需求描述</RButton>
        <RButton variant="outline" @click="$router.back()">取消</RButton>
      </div>
    </div>
  </RPageShell>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { Sparkles, Lightbulb } from 'lucide-vue-next'
import { RPageShell, RCard, RButton, RBadge, RTextarea, RSelect, RProgress } from '@/components/ui'
import FormField from '@/components/app/FormField.vue'
import { analyzeTeamGap, type TeamGapAnalysis } from '@/api/modules/brain'
import { toast } from '@/lib/notify'

const router = useRouter()
const step = ref(1)
const loading = ref(false)
const analysis = ref<TeamGapAnalysis | null>(null)

const form = reactive({
  businessObjective: '',
  departmentId: undefined as number | undefined,
})

const deptOptions = [{ label: '支付研发部', value: 1 }, { label: '增长产品部', value: 2 }, { label: '基础架构部', value: 3 }]

async function handleAnalyze() {
  loading.value = true
  try {
    const res = await analyzeTeamGap({
      businessObjective: form.businessObjective,
      departmentId: form.departmentId,
    })
    analysis.value = res.data
    step.value = 2
  } catch (e: any) {
    toast.error(e?.message || '分析失败')
  } finally {
    loading.value = false
  }
}

function handleConfirm() {
  toast.success('招聘方案已生成，正在创建需求...')
  router.push('/planning/demands')
}
</script>
