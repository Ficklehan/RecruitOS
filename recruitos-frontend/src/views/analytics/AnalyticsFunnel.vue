<template>
  <div class="page-container">
    <!-- 页面头部 -->
    <div class="page-header">
      <div>
        <h2 class="page-title">招聘漏斗</h2>
        <p class="page-subtitle">追踪从需求提报到入职的全链路转化</p>
      </div>
    </div>

    <!-- 日期选择 -->
    <div class="filter-bar">
      <el-date-picker
        v-model="dateRange"
        type="daterange"
        range-separator="至"
        start-placeholder="开始日期"
        end-placeholder="结束日期"
        value-format="YYYY-MM-DD"
        style="width: 300px"
      />
      <el-button type="primary" @click="handleSearch">
        <el-icon><Search /></el-icon>
        查询
      </el-button>
    </div>

    <!-- 整体转化率 -->
    <div class="overall-card">
      <div class="overall-left">
        <div class="overall-label">整体转化率</div>
        <div class="overall-value">{{ overallRate }}<span class="overall-unit">%</span></div>
        <div class="overall-desc">从需求提报到入职的最终转化</div>
      </div>
      <div class="overall-right">
        <div class="overall-stat">
          <span class="overall-stat-value">{{ funnelData[0]?.count || 0 }}</span>
          <span class="overall-stat-label">需求提报</span>
        </div>
        <div class="overall-arrow">
          <svg width="32" height="16" viewBox="0 0 32 16" fill="none"><path d="M0 8h28M22 2l6 6-6 6" stroke="white" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" opacity="0.6"/></svg>
        </div>
        <div class="overall-stat">
          <span class="overall-stat-value">{{ funnelData[funnelData.length - 1]?.count || 0 }}</span>
          <span class="overall-stat-label">入职</span>
        </div>
      </div>
    </div>

    <!-- 漏斗图 -->
    <div class="data-card">
      <div class="funnel-container">
        <div
          v-for="(stage, index) in funnelData"
          :key="stage.name"
          class="funnel-stage"
        >
          <div class="funnel-label">{{ stage.name }}</div>
          <div class="funnel-bar-wrap">
            <div
              class="funnel-bar"
              :style="{ width: stage.percentage + '%', background: stage.color }"
            >
              <span class="funnel-count">{{ stage.count }}</span>
            </div>
          </div>
          <div class="funnel-meta">
            <span class="funnel-percentage">{{ stage.percentage }}%</span>
            <span v-if="index > 0" class="funnel-conversion">
              转化 {{ stage.conversionRate }}%
            </span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { Search } from '@element-plus/icons-vue'

const dateRange = ref<string[]>([])

const funnelData = ref([
  { name: '需求提报', count: 1000, percentage: 100, conversionRate: 100, color: '#3B82F6' },
  { name: '简历筛选', count: 650, percentage: 65, conversionRate: 65, color: '#60A5FA' },
  { name: '初面', count: 320, percentage: 32, conversionRate: 49.2, color: '#818CF8' },
  { name: '复试', count: 180, percentage: 18, conversionRate: 56.3, color: '#A78BFA' },
  { name: 'Offer', count: 95, percentage: 9.5, conversionRate: 52.8, color: '#C084FC' },
  { name: '入职', count: 78, percentage: 7.8, conversionRate: 82.1, color: '#059669' },
])

const overallRate = computed(() => {
  if (!funnelData.value.length) return 0
  const first = funnelData.value[0].count
  const last = funnelData.value[funnelData.value.length - 1].count
  return ((last / first) * 100).toFixed(1)
})

function handleSearch() {
  // Mock: 重新加载数据
}
</script>

<style lang="scss" scoped>
@import '@/assets/styles/variables.scss';

// ── 整体转化率卡片 ──────────────────────
.overall-card {
  background: linear-gradient(135deg, #2563EB 0%, #3B82F6 60%, #60A5FA 100%);
  border-radius: $border-radius-lg;
  padding: 28px 32px;
  margin-bottom: $spacing-xl;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.overall-left {
  .overall-label {
    font-size: 13px;
    opacity: 0.75;
    margin-bottom: 4px;
  }

  .overall-value {
    font-size: 42px;
    font-weight: 800;
    line-height: 1.1;
    letter-spacing: -0.03em;
  }

  .overall-unit {
    font-size: 22px;
    font-weight: 500;
    opacity: 0.8;
  }

  .overall-desc {
    font-size: 13px;
    opacity: 0.6;
    margin-top: 4px;
  }
}

.overall-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.overall-stat {
  text-align: center;

  .overall-stat-value {
    display: block;
    font-size: 24px;
    font-weight: 700;
    letter-spacing: -0.02em;
  }

  .overall-stat-label {
    display: block;
    font-size: 12px;
    opacity: 0.7;
    margin-top: 2px;
  }
}

.overall-arrow {
  opacity: 0.5;
}

// ── 漏斗 ────────────────────────────────
.funnel-container {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 12px 0;
}

.funnel-stage {
  display: grid;
  grid-template-columns: 80px 1fr 120px;
  align-items: center;
  gap: 16px;
}

.funnel-label {
  font-size: 13px;
  font-weight: 500;
  color: $text-regular;
  text-align: right;
}

.funnel-bar-wrap {
  height: 40px;
  background: $bg-muted;
  border-radius: 6px;
  overflow: hidden;
}

.funnel-bar {
  height: 100%;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  padding-right: 14px;
  transition: width 0.8s cubic-bezier(0.22, 1, 0.36, 1);
  min-width: 60px;
}

.funnel-count {
  font-size: 14px;
  font-weight: 700;
  color: #fff;
  letter-spacing: -0.01em;
}

.funnel-meta {
  display: flex;
  flex-direction: column;
  gap: 1px;
}

.funnel-percentage {
  font-size: 15px;
  font-weight: 600;
  color: $text-primary;
}

.funnel-conversion {
  font-size: 12px;
  color: $text-placeholder;
}
</style>
