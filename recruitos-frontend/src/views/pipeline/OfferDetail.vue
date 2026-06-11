<template>
  <PageShell>
<div v-if="loading" class="absolute inset-0 z-10 flex items-center justify-center bg-background/60">
      <Loader2 class="h-6 w-6 animate-spin text-primary" />
    </div>

    <div class="ws-breadcrumb">
      <RButton variant="ghost" @click="$router.back()">
        <ArrowLeft class="mr-2 h-4 w-4" />返回
      </RButton>
    </div>

    <ObjectHeader
      v-if="offer"
      :meta="`${offer.jobTitle} · ${offer.department} · ${offer.salary}`"
    >
      <template #title>
        {{ offer.candidateName }}
        <RBadge :variant="offerStatusBadge(offer.status)" class="ml-2">
          {{ statusLabel(offer.status) }}
        </RBadge>
      </template>
      <template #actions>
        <RButton v-if="offer.status === 'DRAFT'" @click="submitApproval">提交审批</RButton>
        <RButton v-if="offer.status === 'APPROVED'" variant="outline" @click="sendOffer">发送录用通知</RButton>
        <RButton v-if="offer.status === 'SENT'" variant="default" class="bg-green-600 hover:bg-green-700" @click="acceptOffer">
          候选人接受
        </RButton>
      </template>
    </ObjectHeader>

    <RTabs v-if="offer" default-value="basic" class="mt-4">
      <RTabsList>
        <RTabsTrigger value="basic">基本信息</RTabsTrigger>
        <RTabsTrigger value="salary">定薪</RTabsTrigger>
        <RTabsTrigger value="bg">背调</RTabsTrigger>
      <RTabsTrigger value="ai">AI 策略</RTabsTrigger>
      </RTabsList>
      <RTabsContent value="basic">
        <div class="rounded-xl bg-card text-card-foreground shadow-soft p-6">
          <p><b>候选人：</b>{{ offer.candidateName }}</p>
          <p><b>在招职位：</b>{{ offer.jobTitle }}</p>
          <p><b>部门：</b>{{ offer.department }}</p>
          <p><b>薪资：</b>{{ offer.salary }}</p>
          <p><b>职级：</b>{{ offer.level || '-' }}</p>
          <p><b>预计入职：</b>{{ offer.onboardDate || '-' }}</p>
          <p><b>备注：</b>{{ offer.remark || '-' }}</p>
        </div>
      </RTabsContent>
      <RTabsContent value="salary">
        <div class="rounded-xl bg-card text-card-foreground shadow-soft p-6 grid gap-4 max-w-md">
          <FormField label="月薪"><RInput v-model="salaryForm.salary" /></FormField>
          <FormField label="奖金"><RInput v-model="salaryForm.bonus" /></FormField>
          <FormField label="职级"><RInput v-model="salaryForm.level" /></FormField>
          <RButton @click="saveSalary">保存定薪</RButton>
        </div>
      </RTabsContent>
      <RTabsContent value="bg">
        <div class="rounded-xl bg-card text-card-foreground shadow-soft p-6">
          <p>当前状态：{{ bgLabel(offer.bgCheckStatus) }}</p>
          <div class="flex flex-wrap gap-2 mt-3">
            <RButton variant="outline" @click="setBg('PENDING')">待背调</RButton>
            <RButton class="bg-green-600 hover:bg-green-700" @click="setBg('PASSED')">背调通过</RButton>
            <RButton variant="destructive" @click="setBg('FAILED')">背调未通过</RButton>
          </div>
        </div>
      </RTabsContent>
      <RTabsContent value="ai">
        <div class="rounded-xl bg-card text-card-foreground shadow-soft p-6">
          <div v-if="strategyLoading" class="flex items-center justify-center py-8">
            <Loader2 class="h-5 w-5 animate-spin text-primary" />
          </div>
          <template v-else-if="aiStrategy">
            <div class="flex items-center gap-2 mb-5">
              <Sparkles class="h-4 w-4 text-primary" />
              <h4 class="font-semibold text-[15px] text-foreground">AI Offer 策略建议</h4>
              <span class="text-[11px] text-muted-foreground">置信度 {{ ((aiStrategy.confidence || 0) * 100).toFixed(0) }}%</span>
            </div>

            <div class="mb-6">
              <p class="text-[12px] text-muted-foreground mb-3">建议薪酬区间</p>
              <div class="flex items-end gap-6">
                <div class="text-center">
                  <p class="text-[11px] text-muted-foreground mb-1">下限</p>
                  <p class="text-[16px] font-bold text-muted-foreground">{{ (aiStrategy.suggestedRange.min / 10000).toFixed(0) }}万</p>
                </div>
                <div class="text-center bg-primary/5 rounded-lg px-6 py-3 border-2 border-primary/20">
                  <p class="text-[11px] text-primary mb-1 font-medium">建议</p>
                  <p class="text-[22px] font-bold text-primary">{{ (aiStrategy.suggestedRange.mid / 10000).toFixed(0) }}万</p>
                </div>
                <div class="text-center">
                  <p class="text-[11px] text-muted-foreground mb-1">上限</p>
                  <p class="text-[16px] font-bold text-muted-foreground">{{ (aiStrategy.suggestedRange.max / 10000).toFixed(0) }}万</p>
                </div>
              </div>
            </div>

            <div v-if="aiStrategy.components?.length" class="mb-6">
              <p class="text-[12px] text-muted-foreground mb-3">建议薪酬结构</p>
              <div class="space-y-2">
                <div v-for="c in aiStrategy.components" :key="c.type" class="flex justify-between items-center p-3 rounded-lg bg-muted">
                  <span class="font-medium text-[13px]">{{ { base: '基本工资', bonus: '目标奖金', options: '期权/股票' }[c.type] || c.type }}</span>
                  <span class="font-semibold text-[13px]">{{ c.amount > 0 ? (c.amount / 10000).toFixed(0) + '万' : '待定' }}</span>
                </div>
              </div>
            </div>

            <div v-if="aiStrategy.strategySummary" class="mb-6 p-4 bg-primary/5 border border-primary/10 rounded-lg">
              <p class="text-[13px] text-foreground">{{ aiStrategy.strategySummary }}</p>
            </div>

            <div v-if="aiStrategy.negotiationTips?.length" class="mb-4">
              <p class="text-[12px] text-muted-foreground mb-2">谈判建议</p>
              <ul class="space-y-1.5">
                <li v-for="(t, i) in aiStrategy.negotiationTips" :key="i" class="flex items-start gap-2 text-[13px] text-foreground">
                  <span class="text-primary mt-0.5">•</span>{{ t }}
                </li>
              </ul>
            </div>

            <div v-if="aiStrategy.risks?.length">
              <p class="text-[12px] text-muted-foreground mb-2">风险提示</p>
              <div v-for="(r, i) in aiStrategy.risks" :key="i" class="flex items-center gap-3 text-[13px] p-2 rounded-lg border border-warning/20 mb-1.5">
                <span class="text-[11px] font-medium px-1.5 py-0.5 rounded" :class="r.severity === 'HIGH' ? 'bg-danger/10 text-danger' : 'bg-warning/10 text-warning'">{{ r.severity }}</span>
                <span>{{ r.risk }}</span>
              </div>
            </div>
          </template>
          <div v-else class="text-center py-8">
            <p class="text-sm text-muted-foreground mb-3">尚未加载 AI 策略</p>
            <RButton variant="outline" size="sm" @click="loadStrategy">
              <Sparkles class="mr-1.5 h-3.5 w-3.5" />生成 AI 策略
            </RButton>
          </div>
        </div>
      </RTabsContent>
    </RTabs>
</PageShell>
</template>

<script setup lang="ts">
import PageShell from '@/components/Layout/PageShell.vue'
import ObjectHeader from '@/components/Layout/ObjectHeader.vue'
import { onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ArrowLeft, Loader2, Sparkles } from 'lucide-vue-next'
import { toast } from '@/lib/notify'
import { offerStatusBadge } from '@/lib/badgeVariants'
import FormField from '@/components/app/FormField.vue'
import { RButton, RBadge, RInput, RTabs, RTabsList, RTabsTrigger, RTabsContent } from '@/components/ui'
import {
  getOfferDetail, updateOffer, submitOfferApproval,
  sendOffer as sendOfferApi, acceptOffer as acceptOfferApi, updateBgCheckStatus,
} from '@/api/modules/offer'
import { getOfferStrategy, type OfferStrategy } from '@/api/modules/brain'

const route = useRoute()
const loading = ref(false)
const offer = ref<any>(null)
const aiStrategy = ref<OfferStrategy | null>(null)
const strategyLoading = ref(false)
const salaryForm = reactive({ salary: '', bonus: '', level: '' })

function statusLabel(s: string) {
  return ({ DRAFT: '草稿', PENDING: '待审批', APPROVED: '已通过', SENT: '已发送', ACCEPTED: '已接受', REJECTED: '已拒绝' } as Record<string, string>)[s] || s
}
function bgLabel(s?: string) {
  return ({ PENDING: '待背调', PASSED: '已通过', FAILED: '未通过' } as Record<string, string>)[s || ''] || '未启动'
}

async function load() {
  loading.value = true
  try {
    const { data } = await getOfferDetail(Number(route.params.id))
    offer.value = data
    salaryForm.salary = String(data.salary || '')
    salaryForm.bonus = String(data.bonus || '')
    salaryForm.level = data.level || ''
  } finally { loading.value = false }
}

async function saveSalary() {
  await updateOffer(offer.value.id, {
    salary: salaryForm.salary,
    remark: offer.value.remark,
    candidateName: offer.value.candidateName,
    jobTitle: offer.value.jobTitle,
    department: offer.value.department,
  })
  toast.success('定薪已保存')
  load()
}
async function setBg(status: string) { await updateBgCheckStatus(offer.value.id, status); toast.success('背调状态已更新'); load() }

// AI Strategy
async function loadStrategy() {
  if (!offer.value) return
  strategyLoading.value = true
  try {
    const res = await getOfferStrategy(
      offer.value.candidateId || 0,
      offer.value.jobId,
      offer.value.candidateName,
      offer.value.jobTitle,
      offer.value.level,
    )
    aiStrategy.value = res.data
  } catch { aiStrategy.value = null } finally { strategyLoading.value = false }
}
async function submitApproval() { await submitOfferApproval(offer.value.id); toast.success('已提交审批'); load() }
async function sendOffer() { await sendOfferApi(offer.value.id); toast.success('Offer 已发送'); load() }
async function acceptOffer() { await acceptOfferApi(offer.value.id); toast.success('候选人已接受 Offer'); load() }

onMounted(() => { load().then(() => loadStrategy()) })
</script>

<style scoped lang="scss">
@import '@/assets/styles/variables.scss';
.ws-breadcrumb { margin-bottom: $spacing-sm; }
</style>
