<template>
  <div class="hiring-summary relative">
    <div v-if="loading" class="absolute inset-0 z-10 flex items-center justify-center bg-background/60 rounded-lg min-h-[120px]">
      <Loader2 class="h-6 w-6 animate-spin text-primary" />
    </div>

    <div class="round-seg inline-flex rounded-md border p-1 mb-4">
      <RButton
        v-for="opt in roundOptions"
        :key="opt.value"
        size="sm"
        :variant="roundTab === opt.value ? 'default' : 'ghost'"
        @click="roundTab = opt.value as 'INITIAL' | 'SECOND'"
      >
        {{ opt.label }}
      </RButton>
    </div>

    <EmptyStateCta
      v-if="!filteredInterviews.length && !filteredOffers.length && !loading"
      title="暂无面试或录用进展"
      description="候选人进入面试或录用阶段后，会按初面/复试分 Tab 展示。可在看板安排面试或发起录用通知。"
      :image-size="56"
      :actions="[
        { label: '查看在招候选人', type: 'primary', onClick: () => emit('go-kanban') },
        { label: '打开面试日历', onClick: () => router.push({ path: '/pipeline/calendar', query: { jobId: String(jobId) } }) },
      ]"
    />

    <div v-else class="story-list">
      <div v-for="item in filteredInterviews" :key="item.id" class="story-item">
        <div class="story-main">
          <span class="story-name">{{ item.candidateName }}</span>
          <RBadge :variant="elTagTypeToBadge(item.state.tagType)">{{ item.state.label }}</RBadge>
          <span class="story-line">{{ item.state.phase }}</span>
        </div>
        <div class="story-actions">
          <RButton
            v-if="item.state.nextAction === '提交反馈'"
            size="sm"
            variant="outline"
            @click="openFeedback(item.raw)"
          >
            提交反馈
          </RButton>
          <RButton
            v-else-if="item.state.nextAction === '安排面试'"
            size="sm"
            @click="router.push({ path: '/pipeline/calendar', query: { jobId: String(jobId) } })"
          >
            安排面试
          </RButton>
          <RButton v-else size="sm" variant="link" @click="openCandidate(item.raw.candidateId)">
            查看候选人
          </RButton>
        </div>
      </div>

      <div v-for="o in filteredOffers" :key="`offer-${o.id}`" class="story-item offer-item">
        <div class="story-main">
          <span class="story-name">{{ o.candidateName }}</span>
          <span class="story-line">录用通知 · {{ offerStatusLabel(o.status) }}</span>
        </div>
        <RButton size="sm" variant="link" @click="router.push(`/pipeline/offers/${o.id}`)">
          查看录用通知
        </RButton>
      </div>
    </div>

    <InterviewEvalDrawer v-model="feedbackVisible" :interview="activeInterview" @submitted="load" />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { Loader2 } from 'lucide-vue-next'
import { RButton, RBadge } from '@/components/ui'
import { elTagTypeToBadge } from '@/lib/badgeVariants'
import EmptyStateCta from '@/components/common/EmptyStateCta.vue'
import InterviewEvalDrawer from '@/components/interview/InterviewEvalDrawer.vue'
import { offerStatusLabel } from '@/constants/businessLabels'
import { getInterviewList } from '@/api/modules/interview'
import { getOfferList } from '@/api/modules/offer'
import { describeInterviewState } from '@/utils/interviewRoundState'

const props = defineProps<{ jobId: number }>()
const emit = defineEmits<{ 'go-kanban': [] }>()

const router = useRouter()
const loading = ref(false)
const roundTab = ref<'INITIAL' | 'SECOND'>('INITIAL')
const interviews = ref<any[]>([])
const offers = ref<any[]>([])
const feedbackVisible = ref(false)
const activeInterview = ref<any | null>(null)

const roundOptions = [
  { label: '初面', value: 'INITIAL' },
  { label: '复试', value: 'SECOND' },
]

const filteredInterviews = computed(() =>
  interviews.value
    .filter((i) => {
      const r = (i.round || 'INITIAL').toUpperCase()
      if (roundTab.value === 'INITIAL') return r === 'INITIAL'
      return r === 'SECOND' || r === 'FINAL'
    })
    .map((raw) => ({
      id: raw.id,
      candidateName: raw.candidateName || '候选人',
      raw,
      state: describeInterviewState(raw),
    }))
)

const filteredOffers = computed(() => (roundTab.value === 'SECOND' ? offers.value : []))

async function load() {
  loading.value = true
  try {
    const [ivRes, offerRes]: any[] = await Promise.all([
      getInterviewList({ jobId: props.jobId, pageNum: 1, pageSize: 100 }),
      getOfferList({ pageNum: 1, pageSize: 50, jobId: props.jobId }).catch(() => ({ data: { list: [] } })),
    ])
    interviews.value = ivRes.data?.list || []
    offers.value = offerRes.data?.list || []
  } finally {
    loading.value = false
  }
}

function openCandidate(candidateId: number) {
  router.push({ path: `/pipeline/candidates/${candidateId}`, query: { jobId: String(props.jobId) } })
}

function openFeedback(interview: any) {
  activeInterview.value = interview
  feedbackVisible.value = true
}

watch(() => props.jobId, load, { immediate: true })
onMounted(load)
</script>

<style scoped lang="scss">
@import '@/assets/styles/variables.scss';
.story-list { display: flex; flex-direction: column; gap: 10px; }
.story-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 16px;
  border: none;
  border-radius: 10px;
  background: #fff;
}
.story-main { min-width: 0; display: flex; align-items: center; flex-wrap: wrap; gap: 8px; }
.story-name { font-weight: 600; color: $text-primary; }
.story-line { font-size: 13px; color: $text-secondary; }
.story-actions { flex-shrink: 0; }
.offer-item { border-style: dashed; }
</style>
