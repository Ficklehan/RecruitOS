<template>
  <PageShell title="面试官效能">
<div class="top-performers">
      <h3 class="section-title">最佳面试官</h3>
      <div class="top-cards">
        <div
          v-for="(item, index) in topInterviewers"
          :key="item.name"
          class="top-card"
          :class="'rank-' + (index + 1)"
        >
          <div class="rank-badge">{{ index + 1 }}</div>
          <div class="top-name">{{ item.name }}</div>
          <div class="top-dept">{{ item.department }}</div>
          <div class="top-stats">
            <span class="top-pass-rate">通过率 {{ item.passRate }}%</span>
            <span class="top-count">{{ item.interviewCount }} 次面试</span>
          </div>
        </div>
      </div>
    </div>

    <div class="rounded-xl bg-card text-card-foreground shadow-soft">
      <RTable v-if="interviewerList.length">
        <RTableHead>
          <RTableRow>
            <RTableTh class="w-[120px]">面试官</RTableTh>
            <RTableTh class="w-[100px] text-center">面试次数</RTableTh>
            <RTableTh class="w-[100px] text-center">通过次数</RTableTh>
            <RTableTh class="w-[180px] text-center">通过率</RTableTh>
            <RTableTh class="w-[160px] text-center">平均评分</RTableTh>
            <RTableTh class="w-[130px] text-center">平均决策天数</RTableTh>
          </RTableRow>
        </RTableHead>
        <RTableBody>
          <RTableRow v-for="row in interviewerList" :key="row.name">
            <RTableCell>
              <div class="interviewer-cell">
                <span class="interviewer-name">{{ row.name }}</span>
                <span class="interviewer-dept">{{ row.department }}</span>
              </div>
            </RTableCell>
            <RTableCell class="text-center">{{ row.interviewCount }}</RTableCell>
            <RTableCell class="text-center">{{ row.passCount }}</RTableCell>
            <RTableCell>
              <div class="rate-cell">
                <RProgress :value="row.passRate" class="h-3" />
                <span class="rate-text">{{ row.passRate }}%</span>
              </div>
            </RTableCell>
            <RTableCell class="text-center">
              <div class="rating-cell">
                <div class="star-row">
                  <Star
                    v-for="i in 5"
                    :key="i"
                    class="h-4 w-4"
                    :class="i <= Math.round(row.avgScore) ? 'text-amber-400 fill-amber-400' : 'text-muted-foreground'"
                  />
                </div>
                <span class="rating-value">{{ row.avgScore }}</span>
              </div>
            </RTableCell>
            <RTableCell class="text-center">
              <span :class="{ 'fast-decision': row.avgDecisionDays <= 2 }">
                {{ row.avgDecisionDays }} 天
              </span>
            </RTableCell>
          </RTableRow>
        </RTableBody>
      </RTable>
    </div>
</PageShell>
</template>

<script setup lang="ts">
import PageShell from '@/components/Layout/PageShell.vue'
import { ref, computed, onMounted } from 'vue'
import { Star } from 'lucide-vue-next'
import { RTable, RTableHead, RTableBody, RTableRow, RTableTh, RTableCell, RProgress } from '@/components/ui'
import { getInterviewerData } from '@/api/modules/analytics'

const interviewerList = ref<any[]>([])

const topInterviewers = computed(() =>
  [...interviewerList.value].sort((a, b) => b.passRate - a.passRate).slice(0, 3)
)

async function loadData() {
  const res = await getInterviewerData()
  interviewerList.value = res.data.records || res.data.list || res.data || []
}

onMounted(loadData)
</script>

<style lang="scss" scoped>
@import '@/assets/styles/variables.scss';
.section-title {
  font-size: 14px;
  font-weight: 600;
  color: $text-primary;
  margin: 0 0 16px 0;
}

.top-performers {
  margin-bottom: 20px;
}

.top-cards {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.top-card {
  background: $bg-card;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
  position: relative;
  overflow: hidden;

  &.rank-1 { border-top: 3px solid #FFD700; }
  &.rank-2 { border-top: 3px solid #C0C0C0; }
  &.rank-3 { border-top: 3px solid #CD7F32; }
}

.rank-badge {
  position: absolute;
  top: 12px;
  right: 12px;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: $primary-color;
  color: var(--r-bg-card);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 700;

  .rank-1 & { background: #FFD700; color: $text-primary; }
  .rank-2 & { background: #C0C0C0; color: $text-primary; }
  .rank-3 & { background: #CD7F32; }
}

.top-name {
  font-size: 18px;
  font-weight: 600;
  color: $text-primary;
  margin-bottom: 4px;
}

.top-dept {
  font-size: 13px;
  color: $text-secondary;
  margin-bottom: 12px;
}

.top-stats {
  display: flex;
  gap: 12px;
  font-size: 13px;
}

.top-pass-rate {
  color: $success-color;
  font-weight: 500;
}

.top-count {
  color: $text-secondary;
}

.interviewer-cell {
  display: flex;
  flex-direction: column;
}

.interviewer-name {
  font-weight: 500;
  color: $text-primary;
}

.interviewer-dept {
  font-size: 12px;
  color: $text-secondary;
}

.rate-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.rate-text {
  font-size: 12px;
  font-weight: 600;
  min-width: 36px;
}

.rating-cell {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.star-row {
  display: flex;
  gap: 2px;
}

.rating-value {
  font-size: 14px;
  font-weight: 600;
  color: $text-primary;
}

.fast-decision {
  color: $success-color;
  font-weight: 600;
}
</style>
