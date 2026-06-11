<template>
  <PageShell title="面试日历" subtitle="按周/月查看面试场次，点击可进入候选人详情">
    <template #actions>
      <RButton variant="outline" size="icon" @click="handleRefresh">
        <RefreshCw class="h-4 w-4" />
      </RButton>
      <RButton @click="handleArrange">
        <Plus class="mr-2 h-4 w-4" />
        安排面试
      </RButton>
    </template>

    <!-- 日历控制栏 -->
    <div class="calendar-controls">
      <div class="control-left">
        <div class="inline-flex rounded-md border">
          <RButton variant="ghost" size="sm" class="rounded-none border-r" @click="handlePrev">&lt;</RButton>
          <RButton variant="ghost" size="sm" class="rounded-none border-r" @click="handleToday">今天</RButton>
          <RButton variant="ghost" size="sm" class="rounded-none" @click="handleNext">&gt;</RButton>
        </div>
        <span class="current-range">{{ currentRange }}</span>
      </div>
      <div class="control-right">
        <div class="inline-flex rounded-md border p-1">
          <RButton size="sm" :variant="viewMode === 'week' ? 'default' : 'ghost'" @click="viewMode = 'week'; handleViewChange()">周</RButton>
          <RButton size="sm" :variant="viewMode === 'month' ? 'default' : 'ghost'" @click="viewMode = 'month'; handleViewChange()">月</RButton>
        </div>
        <div class="legend">
          <span class="legend-item">
            <span class="legend-dot legend-dot--primary"></span> 初面
          </span>
          <span class="legend-item">
            <span class="legend-dot legend-dot--success"></span> 复试
          </span>
          <span class="legend-item">
            <span class="legend-dot legend-dot--warning"></span> 待安排
          </span>
        </div>
      </div>
    </div>

    <EmptyStateCta
      v-if="!loading && interviewList.length === 0"
      title="暂无面试安排"
      description="可在招聘进展中为候选人安排面试，或从今日待办进入"
      :actions="[
        { label: '去招聘进展', type: 'primary', onClick: () => router.push('/pipeline/board') },
        { label: '查看今日待办', onClick: () => router.push('/workspace/today') },
      ]"
    />

    <!-- 周视图 -->
    <div v-else-if="viewMode === 'week'" class="week-view">
      <div class="week-header">
        <div class="time-gutter"></div>
        <div
          v-for="day in weekDays"
          :key="day.date"
          class="day-col-header"
          :class="{ 'is-today': day.isToday }"
        >
          <span class="day-name">{{ day.name }}</span>
          <span class="day-date" :class="{ 'today-badge': day.isToday }">{{ day.date }}</span>
        </div>
      </div>
      <div class="week-body">
        <div class="time-column">
          <div v-for="h in timeSlots" :key="h" class="time-label">{{ h }}</div>
        </div>
        <div v-for="day in weekDays" :key="day.date" class="day-column">
          <div v-for="h in timeSlots" :key="h" class="time-cell"></div>
          <div
            v-for="event in getDayEvents(day.fullDate)"
            :key="event.id"
            class="event-card"
            :style="{ top: getEventTop(event), height: getEventHeight(event), borderLeftColor: getEventColor(event) }"
            @click="handleEventClick(event)"
          >
            <div class="event-title">{{ event.candidateName }}</div>
            <div class="event-meta">{{ event.jobTitle }}</div>
          </div>
        </div>
      </div>
    </div>

    <!-- 月视图 -->
    <div v-else class="month-view">
      <div class="month-header">
        <div v-for="d in ['一', '二', '三', '四', '五', '六', '日']" :key="d" class="month-header-cell">
          {{ d }}
        </div>
      </div>
      <div class="month-grid">
        <div
          v-for="cell in monthCells"
          :key="cell.date"
          class="month-cell"
          :class="{ 'is-today': cell.isToday, 'is-other': !cell.isCurrentMonth }"
        >
          <span class="cell-date">{{ cell.day }}</span>
          <div class="cell-events">
            <div
              v-for="event in cell.events.slice(0, 3)"
              :key="event.id"
              class="cell-event-dot"
              :style="{ background: getEventColor(event) }"
              @click="handleEventClick(event)"
            >
              {{ event.candidateName }}
            </div>
            <span v-if="cell.events.length > 3" class="cell-more">+{{ cell.events.length - 3 }}</span>
          </div>
        </div>
      </div>
    </div>
</PageShell>
</template>

<script setup lang="ts">
import PageShell from '@/components/Layout/PageShell.vue'
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Plus, RefreshCw } from 'lucide-vue-next'
import { toast } from '@/lib/notify'
import { RButton } from '@/components/ui'
import EmptyStateCta from '@/components/common/EmptyStateCta.vue'
import { getInterviewList } from '@/api/modules/interview'

const router = useRouter()

const viewMode = ref<'week' | 'month'>('week')
const currentDate = ref(new Date())
const interviewList = ref<any[]>([])
const loading = ref(false)

function interviewTime(event: { scheduledStartTime?: string; scheduledTime?: string }) {
  return event.scheduledStartTime || event.scheduledTime || ''
}

const timeSlots = Array.from({ length: 12 }, (_, i) => `${String(i + 8).padStart(2, '0')}:00`)

const weekDays = computed(() => {
  const d = new Date(currentDate.value)
  const day = d.getDay()
  const diff = d.getDate() - day + (day === 0 ? -6 : 1)
  const monday = new Date(d.setDate(diff))
  const today = new Date()
  const names = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
  return Array.from({ length: 7 }, (_, i) => {
    const date = new Date(monday)
    date.setDate(monday.getDate() + i)
    return {
      name: names[i],
      date: date.getDate(),
      fullDate: date.toISOString().split('T')[0],
      isToday: date.toDateString() === today.toDateString(),
    }
  })
})

const monthCells = computed(() => {
  const d = new Date(currentDate.value)
  const year = d.getFullYear()
  const month = d.getMonth()
  const firstDay = new Date(year, month, 1)
  const lastDay = new Date(year, month + 1, 0)
  const startDay = firstDay.getDay() === 0 ? 6 : firstDay.getDay() - 1
  const today = new Date()
  const cells = []

  for (let i = startDay - 1; i >= 0; i--) {
    const date = new Date(year, month, -i)
    cells.push({ date: date.toISOString().split('T')[0], day: date.getDate(), isCurrentMonth: false, isToday: false, events: [] })
  }

  for (let i = 1; i <= lastDay.getDate(); i++) {
    const date = new Date(year, month, i)
    const dateStr = date.toISOString().split('T')[0]
    cells.push({
      date: dateStr,
      day: i,
      isCurrentMonth: true,
      isToday: date.toDateString() === today.toDateString(),
      events: interviewList.value.filter(e => interviewTime(e).startsWith(dateStr)),
    })
  }

  const remaining = 42 - cells.length
  for (let i = 1; i <= remaining; i++) {
    const date = new Date(year, month + 1, i)
    cells.push({ date: date.toISOString().split('T')[0], day: i, isCurrentMonth: false, isToday: false, events: [] })
  }

  return cells
})

const currentRange = computed(() => {
  const d = new Date(currentDate.value)
  if (viewMode.value === 'month') {
    return `${d.getFullYear()}年${d.getMonth() + 1}月`
  }
  const days = weekDays.value
  return `${days[0].fullDate} ~ ${days[6].fullDate}`
})

function getDayEvents(dateStr: string) {
  return interviewList.value.filter(e => interviewTime(e).startsWith(dateStr))
}

function getEventTop(event: any) {
  const time = interviewTime(event)
  if (!time) return '0px'
  const h = new Date(time).getHours()
  return `${(h - 8) * 52}px`
}

function getEventHeight(_event?: any) {
  return '48px'
}

function getEventColor(event: any) {
  if (event.round === 'SECOND') return '$success-color'
  if (event.status === 'PENDING_ARRANGE') return '$warning-color'
  return '$primary-color'
}

function handlePrev() {
  const d = new Date(currentDate.value)
  if (viewMode.value === 'week') d.setDate(d.getDate() - 7)
  else d.setMonth(d.getMonth() - 1)
  currentDate.value = d
}

function handleNext() {
  const d = new Date(currentDate.value)
  if (viewMode.value === 'week') d.setDate(d.getDate() + 7)
  else d.setMonth(d.getMonth() + 1)
  currentDate.value = d
}

function handleToday() {
  currentDate.value = new Date()
}

function handleViewChange() {}

function handleRefresh() {
  loadData()
  toast.success('刷新成功')
}

function handleArrange() {
  router.push('/pipeline/board')
}

function handleEventClick(event: any) {
  if (event.candidateId) {
    const query: Record<string, string> = {}
    if (event.jobId) query.jobId = String(event.jobId)
    router.push({ path: `/pipeline/candidates/${event.candidateId}`, query })
    return
  }
  toast.info(`${event.candidateName} · ${event.jobTitle || '在招职位'}`)
}

async function loadData() {
  loading.value = true
  try {
    const res: any = await getInterviewList({})
    const data = res.data || res
    interviewList.value = Array.isArray(data) ? data : data.records || []
  } catch {
    interviewList.value = []
  } finally {
    loading.value = false
  }
}

onMounted(() => { loadData() })
</script>

<style lang="scss" scoped>
@import '@/assets/styles/variables.scss';

.header-actions {
  display: flex;
  gap: 8px;
}

// ── 控制栏 ──────────────────────────────
.calendar-controls {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: $spacing-lg;
  padding: $spacing-md 0;
}

.control-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.current-range {
  font-size: $text-heading-sm;
  font-weight: 600;
  color: $text-primary;
}

.control-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.legend {
  display: flex;
  align-items: center;
  gap: 14px;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 12px;
  color: $text-secondary;
}

.legend-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.legend-dot--primary { background: $primary-color; }
.legend-dot--success { background: $success-color; }
.legend-dot--warning { background: $warning-color; }

// ── 周视图 ──────────────────────────────
.week-view {
  background: $bg-card;
  border-radius: $border-radius;
  overflow: hidden;
}

.week-header {
  display: grid;
  grid-template-columns: 60px repeat(7, 1fr);
  border-bottom: 1px solid $border-color-light;
}

.time-gutter {
  padding: 10px 0;
}

.day-col-header {
  text-align: center;
  padding: 10px 0;
  border-left: 1px solid $border-color-light;

  &.is-today {
    background: $primary-lighter;
  }
}

.day-name {
  display: block;
  font-size: 12px;
  color: $text-secondary;
}

.day-date {
  display: inline-block;
  font-size: 16px;
  font-weight: 600;
  color: $text-primary;
  margin-top: 2px;

  &.today-badge {
    background: $primary-color;
    color: #fff;
    width: 28px;
    height: 28px;
    line-height: 28px;
    border-radius: 50%;
    text-align: center;
  }
}

.week-body {
  display: grid;
  grid-template-columns: 60px repeat(7, 1fr);
  max-height: calc(100vh - 320px);
  overflow-y: auto;
}

.time-column {
  border-right: 1px solid $border-color-light;
}

.time-label {
  height: 52px;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  font-size: 11px;
  color: $text-placeholder;
  padding-top: 2px;
}

.day-column {
  position: relative;
  border-left: 1px solid $border-color-light;
}

.time-cell {
  height: 52px;
  border-bottom: 1px solid $border-color-light;
}

.event-card {
  position: absolute;
  left: 2px;
  right: 2px;
  background: $primary-lighter;
  border-left: 3px solid $primary-color;
  border-radius: 4px;
  padding: 4px 8px;
  cursor: pointer;
  overflow: hidden;
  transition: opacity $transition-fast;
  z-index: 1;

  &:hover {
    opacity: 0.85;
  }
}

.event-title {
  font-size: 12px;
  font-weight: 600;
  color: $text-primary;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.event-meta {
  font-size: 11px;
  color: $text-secondary;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

// ── 月视图 ──────────────────────────────
.month-view {
  background: $bg-card;
  border-radius: $border-radius;
  overflow: hidden;
}

.month-header {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  border-bottom: 1px solid $border-color-light;
}

.month-header-cell {
  text-align: center;
  padding: 10px 0;
  font-size: 13px;
  font-weight: 500;
  color: $text-secondary;
}

.month-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
}

.month-cell {
  min-height: 100px;
  padding: 6px 8px;
  border-right: 1px solid $border-color-light;
  border-bottom: 1px solid $border-color-light;

  &:nth-child(7n) {
    border-right: none;
  }

  &.is-today {
    background: $primary-lighter;
  }

  &.is-other {
    opacity: 0.4;
  }
}

.cell-date {
  font-size: 13px;
  font-weight: 500;
  color: $text-primary;
}

.cell-events {
  margin-top: 4px;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.cell-event-dot {
  font-size: 11px;
  padding: 1px 6px;
  border-radius: 3px;
  color: #fff;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  cursor: pointer;
}

.cell-more {
  font-size: 11px;
  color: $text-secondary;
}
</style>
