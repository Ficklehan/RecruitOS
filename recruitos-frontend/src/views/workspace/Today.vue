<template>
  <div class="page-container page-stack">
    <div class="page-header">
      <div>
        <h2 class="page-title">今日安排</h2>
        <p class="page-subtitle">{{ todayStr }}</p>
      </div>
      <el-button type="primary" @click="$router.push('/pipeline/calendar')">面试日历</el-button>
    </div>

    <div v-if="feedbackItems.length" class="data-card feedback-card" v-loading="loadingFeedback">
      <div class="section-head">
        <h3>待提交反馈</h3>
        <span class="section-hint">已完成面试，请尽快提交评价</span>
      </div>
      <div class="today-list">
        <div v-for="item in feedbackItems" :key="item.id" class="today-item">
          <div class="today-time">{{ formatTime(item.scheduledStartTime) }}</div>
          <div class="today-body">
            <div class="today-title">{{ item.candidateName }} · {{ item.jobTitle }}</div>
            <div class="today-meta">{{ roundLabel(item.round) }}</div>
          </div>
          <el-button type="primary" size="small" @click="openFeedback(item)">提交反馈</el-button>
        </div>
      </div>
    </div>

    <div class="data-card" v-loading="loading">
      <div class="section-head">
        <h3>今日面试</h3>
      </div>
      <div v-if="interviews.length" class="today-list">
        <div v-for="item in interviews" :key="item.id" class="today-item" @click="goInterview(item)">
          <div class="today-time">{{ formatTime(item.scheduledStartTime) }}</div>
          <div class="today-body">
            <div class="today-title">{{ item.candidateName }} · {{ item.jobTitle }}</div>
            <div class="today-meta">
              {{ roundLabel(item.round) }}
              <span v-if="item.meetingPlatform || item.format"> · {{ item.meetingPlatform || item.format }}</span>
            </div>
          </div>
          <el-button link type="primary" size="small" @click.stop="goInterview(item)">查看候选人</el-button>
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
    </div>

    <InterviewEvalDrawer
      v-model="feedbackDrawerVisible"
      :interview="activeFeedbackInterview"
      @submitted="onFeedbackSubmitted"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
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

function formatTime(t?: string) {
  if (!t) return '--:--'
  return t.slice(11, 16)
}

function roundLabel(round?: string) {
  if (round === 'INITIAL') return '初试'
  if (round === 'SECOND') return '复试'
  if (round === 'FINAL') return '终面'
  return '面试'
}

function goInterview(item: any) {
  if (item.candidateId) {
    router.push(`/pipeline/candidates/${item.candidateId}`)
  } else {
    router.push('/pipeline/calendar')
  }
}

function openFeedback(item: any) {
  activeFeedbackInterview.value = item
  feedbackDrawerVisible.value = true
}

async function loadFeedback() {
  loadingFeedback.value = true
  try {
    feedbackItems.value = await loadPendingFeedbackInterviews()
  } finally {
    loadingFeedback.value = false
  }
}

async function onFeedbackSubmitted() {
  await loadFeedback()
}

onMounted(async () => {
  loading.value = true
  try {
    const [todayList] = await Promise.all([
      loadTodayInterviews(),
      loadFeedback(),
    ])
    interviews.value = todayList
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.section-head {
  display: flex;
  align-items: baseline;
  gap: 12px;
  margin-bottom: 12px;
}
.section-head h3 { margin: 0; font-size: 15px; font-weight: 600; }
.section-hint { font-size: 12px; color: #94a3b8; }
.feedback-card { margin-bottom: 16px; }
.today-list { display: flex; flex-direction: column; }
.today-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 14px 4px;
  border-bottom: 1px solid #f1f5f9;
  cursor: pointer;
}
.today-item:hover { background: #f8fafc; }
.today-time { font-weight: 600; width: 56px; color: #3b82f6; flex-shrink: 0; }
.today-body { flex: 1; min-width: 0; }
.today-title { font-weight: 500; }
.today-meta { font-size: 12px; color: #94a3b8; margin-top: 4px; }
</style>
