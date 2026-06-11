<template>
  <PageShell variant="list"
    title="今日安排"
    :subtitle="todayStr"
    plain
  >
    <template #actions>
      <RButton @click="router.push('/pipeline/calendar')">面试日历</RButton>
    </template>

    <RCard v-if="feedbackItems.length" class="p-6 feedback-card">
      <div class="section-head">
        <h3>待提交反馈</h3>
        <span class="section-hint">已完成面试，请尽快提交评价</span>
      </div>
      <div class="today-list">
        <div v-for="item in feedbackItems" :key="item.id" class="today-item" tabindex="0">
          <div class="today-time">{{ formatTime(item.scheduledStartTime) }}</div>
          <div class="today-body">
            <div class="today-title">{{ item.candidateName }} · {{ item.jobTitle }}</div>
            <div class="today-meta">{{ roundLabel(item.round) }}</div>
          </div>
          <RButton size="sm" @click="openFeedback(item)">提交反馈</RButton>
        </div>
      </div>
    </RCard>

    <RCard class="p-6">
      <div class="section-head">
        <h3>今日面试</h3>
      </div>
      <div v-if="interviews.length" class="today-list">
        <div v-for="item in interviews" :key="item.id" class="today-item" tabindex="0" @click="goInterview(item)">
          <div class="today-time">{{ formatTime(item.scheduledStartTime) }}</div>
          <div class="today-body">
            <div class="today-title">{{ item.candidateName }} · {{ item.jobTitle }}</div>
            <div class="today-meta">
              {{ roundLabel(item.round) }}
              <span v-if="item.meetingPlatform || item.format"> · {{ item.meetingPlatform || item.format }}</span>
            </div>
          </div>
          <RButton variant="link" size="sm" class="shrink-0" @click.stop="goInterview(item)">查看候选人</RButton>
        </div>
      </div>

      <EmptyStateCta
        v-else
        title="今日暂无面试"
        description="可以在面试日历中安排候选人面试，或查看招聘进展跟进待处理候选人"
        :actions="[
          { label: '安排面试', type: 'primary', onClick: () => router.push('/pipeline/calendar') },
          { label: '查看招聘进展', type: 'default', onClick: () => router.push('/pipeline/board') },
        ]"
      />
    </RCard>

    <InterviewEvalDrawer
      v-model="feedbackDrawerVisible"
      :interview="activeFeedbackInterview"
      @submitted="onFeedbackSubmitted"
    />
  </PageShell>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { RButton, RCard } from '@/components/ui'
import PageShell from '@/components/Layout/PageShell.vue'
import EmptyStateCta from '@/components/common/EmptyStateCta.vue'
import InterviewEvalDrawer from '@/components/interview/InterviewEvalDrawer.vue'
import { loadPendingFeedbackInterviews, loadTodayInterviews } from '@/api/modules/workspace'

const router = useRouter()
const loading = ref(false)
const loadingFeedback = ref(false)
const interviews = ref<any[]>([])
const feedbackItems = ref<any[]>([])
const feedbackDrawerVisible = ref(false)
const activeFeedbackInterview = ref<any | null>(null)

const todayStr = computed(() => new Date().toLocaleDateString('zh-CN', {
  weekday: 'long', year: 'numeric', month: 'long', day: 'numeric',
}))

function formatTime(t?: string) { if (!t) return '--:--'; return t.slice(11, 16) }
function roundLabel(round?: string) { return ({ INITIAL: '初试', SECOND: '复试', FINAL: '终面' } as Record<string, string>)[round || ''] || '面试' }
function goInterview(item: any) { if (item.candidateId) router.push(`/pipeline/candidates/${item.candidateId}`); else router.push('/pipeline/calendar') }
function openFeedback(item: any) { activeFeedbackInterview.value = item; feedbackDrawerVisible.value = true }

async function loadFeedback() { loadingFeedback.value = true; try { feedbackItems.value = await loadPendingFeedbackInterviews() } finally { loadingFeedback.value = false } }
async function onFeedbackSubmitted() { await loadFeedback() }

onMounted(async () => {
  loading.value = true
  try { const [todayList] = await Promise.all([loadTodayInterviews(), loadFeedback()]); interviews.value = todayList } finally { loading.value = false }
})
</script>

<style scoped lang="scss">
@import '@/assets/styles/variables.scss';

.section-head {
  display: flex;
  align-items: baseline;
  gap: $spacing-md;
  margin-bottom: $spacing-md;
  h3 { margin: 0; font-size: $text-heading-sm; font-weight: $font-weight-semibold; color: $text-primary; }
}
.section-hint { font-size: $text-caption; color: $text-placeholder; }
.feedback-card { margin-bottom: $spacing-base; }
.today-list { display: flex; flex-direction: column; }
.today-item {
  display: flex;
  align-items: center;
  gap: $spacing-base;
  padding: 14px $spacing-xs;
  border-bottom: 1px solid var(--r-divider);
  cursor: pointer;
  transition: background-color $transition-fast;
  &:hover { background: $primary-lighter; }
  &:last-child { box-shadow: none; }
}
.today-time { font-weight: $font-weight-semibold; width: 56px; color: $primary-color; flex-shrink: 0; font-variant-numeric: tabular-nums; }
.today-body { flex: 1; min-width: 0; }
.today-title { font-weight: $font-weight-medium; color: $text-regular; }
.today-meta { font-size: $text-caption; color: $text-secondary; margin-top: $spacing-xs; }
</style>
