<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h2 class="page-title">今日安排</h2>
        <p class="page-subtitle">{{ todayStr }}</p>
      </div>
      <el-button type="primary" @click="$router.push('/pipeline/calendar')">面试日历</el-button>
    </div>

    <div class="data-card" v-loading="loading">
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
          <el-button link type="primary" size="small">查看候选人</el-button>
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
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import EmptyStateCta from '@/components/common/EmptyStateCta.vue'
import { loadTodayInterviews } from '@/api/modules/workspace'

const router = useRouter()
const loading = ref(false)
const interviews = ref<any[]>([])

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

onMounted(async () => {
  loading.value = true
  try {
    interviews.value = await loadTodayInterviews()
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
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
