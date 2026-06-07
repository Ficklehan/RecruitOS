<template>
  <div class="page-container">
    <!-- 页面头部 -->
    <div class="page-header">
      <h2 class="page-title">权重面板</h2>
    </div>

    <!-- 岗位选择器 -->
    <div class="job-selector">
      <span class="selector-label">选择岗位：</span>
      <el-select v-model="selectedJob" placeholder="请选择岗位" style="width: 240px" @change="handleJobChange">
        <el-option
          v-for="job in jobOptions"
          :key="job.id"
          :label="job.title"
          :value="job.id"
        />
      </el-select>
    </div>

    <!-- 三列权重可视化 -->
    <div class="weight-columns">
      <!-- 匹配权重 -->
      <div class="weight-column">
        <div class="column-header">
          <el-icon color="#3B82F6"><Histogram /></el-icon>
          <span>匹配权重 (matchWeight)</span>
        </div>
        <div class="weight-bars">
          <div
            v-for="item in currentWeights"
            :key="item.tag"
            class="weight-bar-item"
            @click="openAdjustDialog(item)"
          >
            <div class="bar-label">{{ item.tag }}</div>
            <div class="bar-track">
              <div
                class="bar-fill match"
                :style="{ width: item.matchWeight * 100 + '%' }"
              ></div>
            </div>
            <div class="bar-value">{{ (item.matchWeight * 100).toFixed(0) }}%</div>
          </div>
        </div>
      </div>

      <!-- 搜索权重 -->
      <div class="weight-column">
        <div class="column-header">
          <el-icon color="#059669"><Search /></el-icon>
          <span>搜索权重 (searchWeight)</span>
        </div>
        <div class="weight-bars">
          <div
            v-for="item in currentWeights"
            :key="item.tag"
            class="weight-bar-item"
            @click="openAdjustDialog(item)"
          >
            <div class="bar-label">{{ item.tag }}</div>
            <div class="bar-track">
              <div
                class="bar-fill search"
                :style="{ width: item.searchWeight * 100 + '%' }"
              ></div>
            </div>
            <div class="bar-value">{{ (item.searchWeight * 100).toFixed(0) }}%</div>
          </div>
        </div>
      </div>

      <!-- 决策权重 -->
      <div class="weight-column">
        <div class="column-header">
          <el-icon color="#D97706"><DataAnalysis /></el-icon>
          <span>决策权重 (decisionWeight)</span>
        </div>
        <div class="weight-bars">
          <div
            v-for="item in currentWeights"
            :key="item.tag"
            class="weight-bar-item"
            @click="openAdjustDialog(item)"
          >
            <div class="bar-label">{{ item.tag }}</div>
            <div class="bar-track">
              <div
                class="bar-fill decision"
                :style="{ width: item.decisionWeight * 100 + '%' }"
              ></div>
            </div>
            <div class="bar-value">{{ (item.decisionWeight * 100).toFixed(0) }}%</div>
          </div>
        </div>
      </div>
    </div>

    <!-- 版本历史 -->
    <div class="data-card">
      <div class="section-header">
        <span class="section-title">版本历史</span>
      </div>
      <el-table :data="versionHistory" stripe style="width: 100%">
        <el-table-column prop="version" label="版本号" width="120" />
        <el-table-column prop="date" label="变更时间" width="180" />
        <el-table-column prop="operator" label="操作人" width="120" />
        <el-table-column prop="description" label="变更说明" min-width="200" show-overflow-tooltip />
        <el-table-column prop="changedTags" label="涉及标签" min-width="200">
          <template #default="{ row }">
            <el-tag
              v-for="tag in row.changedTags"
              :key="tag"
              size="small"
              style="margin-right: 4px; margin-bottom: 2px;"
            >
              {{ tag }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 权重调整对话框 -->
    <el-dialog v-model="adjustDialogVisible" title="调整权重" width="520px">
      <div v-if="adjustingItem" class="adjust-content">
        <div class="adjust-tag-name">
          <el-tag size="large">{{ adjustingItem.tag }}</el-tag>
        </div>
        <el-form label-width="100px">
          <el-form-item label="匹配权重">
            <el-slider
              v-model="adjustForm.matchWeight"
              :min="0"
              :max="100"
              :step="1"
              show-input
              input-size="small"
            />
          </el-form-item>
          <el-form-item label="搜索权重">
            <el-slider
              v-model="adjustForm.searchWeight"
              :min="0"
              :max="100"
              :step="1"
              show-input
              input-size="small"
            />
          </el-form-item>
          <el-form-item label="决策权重">
            <el-slider
              v-model="adjustForm.decisionWeight"
              :min="0"
              :max="100"
              :step="1"
              show-input
              input-size="small"
            />
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <el-button @click="adjustDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmAdjust">确认调整</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { Histogram, Search, DataAnalysis } from '@element-plus/icons-vue'

interface WeightItem {
  tag: string
  matchWeight: number
  searchWeight: number
  decisionWeight: number
}

interface VersionRecord {
  version: string
  date: string
  operator: string
  description: string
  changedTags: string[]
}

// 岗位选项
const jobOptions = ref([
  { id: 1, title: '高级前端工程师' },
  { id: 2, title: 'Java后端工程师' },
  { id: 3, title: '产品经理' },
  { id: 4, title: 'UI设计师' },
])

const selectedJob = ref(1)

// 权重数据
const weightDataMap: Record<number, WeightItem[]> = {
  1: [
    { tag: 'Vue.js', matchWeight: 0.85, searchWeight: 0.78, decisionWeight: 0.92 },
    { tag: 'React', matchWeight: 0.72, searchWeight: 0.65, decisionWeight: 0.70 },
    { tag: 'TypeScript', matchWeight: 0.80, searchWeight: 0.75, decisionWeight: 0.85 },
    { tag: 'Node.js', matchWeight: 0.55, searchWeight: 0.48, decisionWeight: 0.60 },
    { tag: 'CSS', matchWeight: 0.60, searchWeight: 0.52, decisionWeight: 0.45 },
    { tag: 'Webpack', matchWeight: 0.45, searchWeight: 0.38, decisionWeight: 0.40 },
  ],
  2: [
    { tag: 'Java', matchWeight: 0.90, searchWeight: 0.85, decisionWeight: 0.88 },
    { tag: 'Spring', matchWeight: 0.82, searchWeight: 0.78, decisionWeight: 0.80 },
    { tag: 'MySQL', matchWeight: 0.70, searchWeight: 0.65, decisionWeight: 0.72 },
    { tag: 'Redis', matchWeight: 0.60, searchWeight: 0.55, decisionWeight: 0.58 },
    { tag: 'Docker', matchWeight: 0.50, searchWeight: 0.45, decisionWeight: 0.52 },
    { tag: 'Kafka', matchWeight: 0.40, searchWeight: 0.35, decisionWeight: 0.38 },
  ],
  3: [
    { tag: '需求分析', matchWeight: 0.88, searchWeight: 0.80, decisionWeight: 0.85 },
    { tag: '数据分析', matchWeight: 0.75, searchWeight: 0.70, decisionWeight: 0.72 },
    { tag: 'Axure', matchWeight: 0.65, searchWeight: 0.60, decisionWeight: 0.55 },
    { tag: 'SQL', matchWeight: 0.55, searchWeight: 0.50, decisionWeight: 0.58 },
    { tag: '项目管理', matchWeight: 0.70, searchWeight: 0.68, decisionWeight: 0.75 },
    { tag: '用户研究', matchWeight: 0.60, searchWeight: 0.55, decisionWeight: 0.62 },
  ],
  4: [
    { tag: 'Figma', matchWeight: 0.90, searchWeight: 0.85, decisionWeight: 0.88 },
    { tag: 'Sketch', matchWeight: 0.78, searchWeight: 0.72, decisionWeight: 0.70 },
    { tag: 'PS', matchWeight: 0.65, searchWeight: 0.60, decisionWeight: 0.58 },
    { tag: 'AI', matchWeight: 0.55, searchWeight: 0.50, decisionWeight: 0.52 },
    { tag: '设计系统', matchWeight: 0.70, searchWeight: 0.65, decisionWeight: 0.72 },
    { tag: '交互设计', matchWeight: 0.80, searchWeight: 0.75, decisionWeight: 0.78 },
  ],
}

const currentWeights = computed(() => weightDataMap[selectedJob.value] || weightDataMap[1])

// 版本历史
const versionHistory = ref<VersionRecord[]>([
  {
    version: 'v2.3',
    date: '2026-06-05 14:30',
    operator: '张管理',
    description: '提升 Vue.js 和 TypeScript 决策权重，降低 CSS 决策权重',
    changedTags: ['Vue.js', 'TypeScript', 'CSS'],
  },
  {
    version: 'v2.2',
    date: '2026-06-01 10:15',
    operator: '李经理',
    description: '根据近两周匹配反馈微调搜索权重',
    changedTags: ['React', 'Node.js', 'Webpack'],
  },
  {
    version: 'v2.1',
    date: '2026-05-25 09:00',
    operator: '系统自动',
    description: '基于进化引擎自动调整，匹配质量提升 3.2%',
    changedTags: ['Vue.js', 'React', 'TypeScript', 'Node.js'],
  },
  {
    version: 'v2.0',
    date: '2026-05-18 16:45',
    operator: '张管理',
    description: '重大版本更新，全面调整前端岗位权重配置',
    changedTags: ['Vue.js', 'React', 'TypeScript', 'Node.js', 'CSS', 'Webpack'],
  },
  {
    version: 'v1.5',
    date: '2026-05-10 11:20',
    operator: '王HR',
    description: '增加 Webpack 搜索权重以匹配更多候选人',
    changedTags: ['Webpack'],
  },
])

// 调整对话框
const adjustDialogVisible = ref(false)
const adjustingItem = ref<WeightItem | null>(null)
const adjustForm = reactive({
  matchWeight: 50,
  searchWeight: 50,
  decisionWeight: 50,
})

function openAdjustDialog(item: WeightItem) {
  adjustingItem.value = item
  adjustForm.matchWeight = Math.round(item.matchWeight * 100)
  adjustForm.searchWeight = Math.round(item.searchWeight * 100)
  adjustForm.decisionWeight = Math.round(item.decisionWeight * 100)
  adjustDialogVisible.value = true
}

function confirmAdjust() {
  if (!adjustingItem.value) return
  const weights = weightDataMap[selectedJob.value]
  const target = weights.find(w => w.tag === adjustingItem.value!.tag)
  if (target) {
    target.matchWeight = adjustForm.matchWeight / 100
    target.searchWeight = adjustForm.searchWeight / 100
    target.decisionWeight = adjustForm.decisionWeight / 100
  }
  adjustDialogVisible.value = false
  ElMessage.success(`已更新「${adjustingItem.value.tag}」权重`)
}

function handleJobChange() {
  // 切换岗位时刷新数据
}
</script>

<style lang="scss" scoped>
@import '@/assets/styles/variables.scss';
.job-selector {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 24px;
  padding: 16px 20px;
  background: $bg-card;
  border-radius: 8px;
  box-shadow: 0 1px 6px 0 rgba(0, 0, 0, 0.04);

  .selector-label {
    font-size: 14px;
    color: $text-regular;
    font-weight: 500;
    white-space: nowrap;
  }
}

.weight-columns {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
  margin-bottom: 24px;

  @media (max-width: 1200px) {
    grid-template-columns: 1fr;
  }
}

.weight-column {
  background: $bg-card;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 1px 6px 0 rgba(0, 0, 0, 0.04);

  .column-header {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 15px;
    font-weight: 600;
    color: $text-primary;
    margin-bottom: 16px;
    padding-bottom: 12px;
    border-bottom: 1px solid $border-color-light;
  }
}

.weight-bars {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.weight-bar-item {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  padding: 6px 8px;
  border-radius: 6px;
  transition: background-color 0.2s;

  &:hover {
    background-color: $bg-muted;
  }

  .bar-label {
    width: 72px;
    font-size: 13px;
    color: $text-regular;
    text-align: right;
    flex-shrink: 0;
  }

  .bar-track {
    flex: 1;
    height: 20px;
    background: #f0f2f5;
    border-radius: 4px;
    overflow: hidden;
  }

  .bar-fill {
    height: 100%;
    border-radius: 4px;
    transition: width 0.6s ease;

    &.match {
      background: linear-gradient(90deg, $primary-color, $primary-light);
    }

    &.search {
      background: linear-gradient(90deg, $success-color, $success-color);
    }

    &.decision {
      background: linear-gradient(90deg, $warning-color, $warning-color);
    }
  }

  .bar-value {
    width: 40px;
    font-size: 13px;
    font-weight: 600;
    color: $text-primary;
    text-align: right;
    flex-shrink: 0;
  }
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;

  .section-title {
    font-size: 16px;
    font-weight: 600;
    color: $text-primary;
  }
}

.adjust-content {
  .adjust-tag-name {
    text-align: center;
    margin-bottom: 20px;
  }
}
</style>
