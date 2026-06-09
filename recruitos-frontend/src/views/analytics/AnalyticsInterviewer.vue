<template>
  <div class="page-container page-stack">
    <!-- 页面头部 -->
    <div class="page-header">
      <h2 class="page-title">面试官效能</h2>
    </div>

    <!-- Top 3 表现最佳面试官 -->
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

    <!-- 数据表格 -->
    <div class="data-card">
      <el-table :data="interviewerList" stripe highlight-current-row style="width: 100%">
        <el-table-column prop="name" label="面试官" width="120">
          <template #default="{ row }">
            <div class="interviewer-cell">
              <span class="interviewer-name">{{ row.name }}</span>
              <span class="interviewer-dept">{{ row.department }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="interviewCount" label="面试次数" width="100" align="center" />
        <el-table-column prop="passCount" label="通过次数" width="100" align="center" />
        <el-table-column label="通过率" width="180" align="center">
          <template #default="{ row }">
            <div class="rate-cell">
              <el-progress
                :percentage="row.passRate"
                :stroke-width="14"
                :text-inside="true"
                :color="getProgressColor(row.passRate)"
              />
            </div>
          </template>
        </el-table-column>
        <el-table-column label="平均评分" width="160" align="center">
          <template #default="{ row }">
            <div class="rating-cell">
              <el-rate
                v-model="row.avgScore"
                disabled
                :max="5"
                :colors="['#DC2626', '#D97706', '#059669']"
                :allow-half="true"
                size="small"
              />
              <span class="rating-value">{{ row.avgScore }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="avgDecisionDays" label="平均决策天数" width="130" align="center">
          <template #default="{ row }">
            <span :class="{ 'fast-decision': row.avgDecisionDays <= 2 }">
              {{ row.avgDecisionDays }} 天
            </span>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { getInterviewerData } from '@/api/modules/analytics'

const interviewerList = ref<any[]>([])

// Top 3 面试官
const topInterviewers = computed(() => {
  return [...interviewerList.value]
    .sort((a, b) => b.passRate - a.passRate)
    .slice(0, 3)
})

// 进度条颜色
function getProgressColor(rate: number): string {
  if (rate >= 60) return '#059669'
  if (rate >= 40) return '#3B82F6'
  if (rate >= 20) return '#D97706'
  return '#DC2626'
}

async function loadData() {
  const res = await getInterviewerData()
  interviewerList.value = res.data.records || res.data.list || res.data || []
}

onMounted(() => {
  loadData()
})
</script>

<style lang="scss" scoped>
@import '@/assets/styles/variables.scss';
.section-title {
  font-size: 15px;
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

  &.rank-1 {
    border-top: 3px solid #FFD700;
  }

  &.rank-2 {
    border-top: 3px solid #C0C0C0;
  }

  &.rank-3 {
    border-top: 3px solid #CD7F32;
  }
}

.rank-badge {
  position: absolute;
  top: 12px;
  right: 12px;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: $primary-color;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 700;

  .rank-1 & {
    background: #FFD700;
    color: $text-primary;
  }

  .rank-2 & {
    background: #C0C0C0;
    color: $text-primary;
  }

  .rank-3 & {
    background: #CD7F32;
  }
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
  width: 100%;
}

.rating-cell {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
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
