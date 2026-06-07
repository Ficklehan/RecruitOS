<template>
  <div class="page-container">
    <!-- 页面头部 -->
    <div class="page-header">
      <h2 class="page-title">人才库</h2>
    </div>

    <!-- 全局搜索区 -->
    <div class="pool-search-section">
      <div class="search-box">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索人才库 - 输入姓名、技能或公司名称..."
          size="large"
          :prefix-icon="Search"
          clearable
          @keyup.enter="handleSearch"
        >
          <template #append>
            <el-button type="primary" @click="handleSearch">
              <el-icon><Search /></el-icon>
              搜索
            </el-button>
          </template>
        </el-input>
      </div>

      <!-- 热门技能标签 -->
      <div class="skill-tags">
        <span class="skill-tags-label">热门技能:</span>
        <el-tag
          v-for="tag in hotSkills"
          :key="tag"
          :type="selectedSkill === tag ? 'primary' : 'info'"
          :effect="selectedSkill === tag ? 'dark' : 'plain'"
          class="skill-tag"
          @click="handleSkillFilter(tag)"
        >
          {{ tag }}
        </el-tag>
        <el-tag
          v-if="selectedSkill"
          type="danger"
          effect="plain"
          class="skill-tag"
          @click="clearSkillFilter"
        >
          清除筛选
        </el-tag>
      </div>
    </div>

    <!-- 搜索结果 -->
    <div v-if="hasSearched" class="pool-results">
      <div class="results-header">
        <span class="results-count">共找到 <strong>{{ talentList.length }}</strong> 位人才</span>
        <el-radio-group v-model="viewMode" size="small">
          <el-radio-button value="card">卡片</el-radio-button>
          <el-radio-button value="list">列表</el-radio-button>
        </el-radio-group>
      </div>

      <!-- 卡片视图 -->
      <div v-if="viewMode === 'card'" class="talent-cards">
        <div
          v-for="talent in talentList"
          :key="talent.id"
          class="talent-card"
        >
          <div class="talent-card-header">
            <div class="talent-avatar">{{ talent.name.charAt(0) }}</div>
            <div class="talent-basic">
              <h3 class="talent-name">{{ talent.name }}</h3>
              <p class="talent-position">{{ talent.position }}</p>
            </div>
            <div v-if="talent.matchScore" class="talent-match">
              <div class="match-score">{{ talent.matchScore }}</div>
              <div class="match-label">匹配度</div>
            </div>
            <el-tag v-if="talent.source" :type="sourceTagMap[talent.source] || 'info'" size="small" class="talent-source-tag">
              {{ sourceLabelMap[talent.source] || talent.source }}
            </el-tag>
          </div>

          <div class="talent-card-body">
            <div class="talent-info-row">
              <el-icon><OfficeBuilding /></el-icon>
              <span>{{ talent.company }}</span>
            </div>
            <div class="talent-info-row">
              <el-icon><Timer /></el-icon>
              <span>{{ talent.workYears }}年工作经验</span>
            </div>
            <div class="talent-info-row">
              <el-icon><Reading /></el-icon>
              <span>{{ talent.education }}</span>
            </div>
            <div class="talent-skills">
              <el-tag
                v-for="skill in talent.skills?.slice(0, 4)"
                :key="skill"
                size="small"
                type="info"
                effect="plain"
              >
                {{ skill }}
              </el-tag>
              <el-tag
                v-if="talent.skills?.length > 4"
                size="small"
                type="info"
                effect="plain"
              >
                +{{ talent.skills.length - 4 }}
              </el-tag>
            </div>
          </div>

          <div class="talent-card-footer">
            <el-button type="primary" link @click="handleViewDetail(talent)">
              <el-icon><View /></el-icon>
              查看详情
            </el-button>
            <el-button v-if="talent.resumeId" type="warning" link @click="goResume(talent)">
              <el-icon><Document /></el-icon>
              查看简历
            </el-button>
            <el-button type="success" link @click="handleRecommend(talent)">
              <el-icon><Promotion /></el-icon>
              推荐到岗位
            </el-button>
          </div>
        </div>
      </div>

      <!-- 列表视图 -->
      <div v-else class="data-card">
        <el-table :data="talentList" stripe highlight-current-row>
          <el-table-column prop="name" label="姓名" width="100">
            <template #default="{ row }">
              <span class="title-link" @click="handleViewDetail(row)">{{ row.name }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="company" label="公司" min-width="140" show-overflow-tooltip />
          <el-table-column prop="position" label="职位" min-width="140" show-overflow-tooltip />
          <el-table-column prop="workYears" label="工作年限" width="90" align="center">
            <template #default="{ row }">{{ row.workYears }}年</template>
          </el-table-column>
          <el-table-column prop="education" label="学历" width="80" align="center" />
          <el-table-column prop="source" label="来源" width="100" align="center">
            <template #default="{ row }">
              <el-tag v-if="row.source" :type="sourceTagMap[row.source] || 'info'" size="small" disable-transitions>
                {{ sourceLabelMap[row.source] || row.source }}
              </el-tag>
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column prop="skills" label="技能" min-width="200">
            <template #default="{ row }">
              <el-tag
                v-for="skill in row.skills?.slice(0, 3)"
                :key="skill"
                size="small"
                type="info"
                effect="plain"
                style="margin-right: 4px"
              >
                {{ skill }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column v-if="talentList.some(t => t.matchScore)" prop="matchScore" label="匹配度" width="80" align="center">
            <template #default="{ row }">
              <span v-if="row.matchScore" class="match-score-text">{{ row.matchScore }}</span>
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="180" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" link size="small" @click="handleViewDetail(row)">查看详情</el-button>
              <el-button type="success" link size="small" @click="handleRecommend(row)">推荐到岗位</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 分页 -->
      <el-pagination
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        :page-sizes="[12, 24, 48]"
        :total="total"
        layout="total, sizes, prev, pager, next"
        @size-change="handleSearch"
        @current-change="handleSearch"
      />
    </div>

    <!-- 空状态 - 未搜索时 -->
    <div v-else class="pool-empty">
      <div class="empty-illustration">
        <el-icon :size="80" color="#94A3B8"><UserFilled /></el-icon>
      </div>
      <h3 class="empty-title">搜索企业人才库</h3>
      <p class="empty-desc">输入姓名、技能或公司名称，快速找到您需要的人才</p>
      <div class="empty-hot-skills">
        <span class="empty-hot-label">试试搜索:</span>
        <el-tag
          v-for="tag in suggestedSearches"
          :key="tag"
          effect="plain"
          class="suggested-tag"
          @click="searchKeyword = tag; handleSearch()"
        >
          {{ tag }}
        </el-tag>
      </div>
    </div>

    <!-- 推荐到岗位对话框 -->
    <el-dialog v-model="recommendVisible" title="推荐到岗位" width="480px">
      <el-form label-width="80px">
        <el-form-item label="候选人">
          <el-input :model-value="currentTalent?.name" disabled />
        </el-form-item>
        <el-form-item label="选择岗位">
          <el-select v-model="recommendJobId" placeholder="请选择推荐岗位" style="width: 100%">
            <el-option
              v-for="job in jobOptions"
              :key="job.id"
              :label="job.title"
              :value="job.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="recommendVisible = false">取消</el-button>
        <el-button type="primary" :loading="recommendLoading" @click="confirmRecommend">确定推荐</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search, UserFilled, OfficeBuilding, Timer, Reading, View, Promotion, Document } from '@element-plus/icons-vue'
import { getTalentPool, addToJob } from '@/api/modules/candidate'
import { getJobList } from '@/api/modules/job'

const router = useRouter()

const searchKeyword = ref('')
const selectedSkill = ref('')
const hasSearched = ref(false)
const viewMode = ref<'card' | 'list'>('card')
const pageNum = ref(1)
const pageSize = ref(12)
const total = ref(0)
const talentList = ref<any[]>([])

// 推荐对话框
const recommendVisible = ref(false)
const recommendLoading = ref(false)
const currentTalent = ref<any>(null)
const recommendJobId = ref<number | null>(null)
const jobOptions = ref<any[]>([])

// 热门技能
const hotSkills = [
  'Python', 'Java', 'React', 'Vue', 'Go', 'TypeScript',
  'Kubernetes', 'MySQL', 'Redis', '微服务', '机器学习', '大模型',
]

// 来源标签
const sourceTagMap: Record<string, string> = {
  BOSS: '', LAGOU: 'success', ZHILIAN: 'warning', LIEPIN: 'danger',
  MANUAL: 'info', REFERRAL: 'success', HEADHUNTER: 'warning',
  AI_SEARCH: 'primary',
}
const sourceLabelMap: Record<string, string> = {
  BOSS: 'Boss直聘', LAGOU: '拉勾', ZHILIAN: '智联招聘', LIEPIN: '猎聘',
  MANUAL: '手动上传', REFERRAL: '内推', HEADHUNTER: '猎头', AI_SEARCH: 'AI检索',
}

function goResume(talent: any) {
  router.push(`/talent/resume/detail/${talent.resumeId}`)
}

// 推荐搜索
const suggestedSearches = [
  '高级前端工程师', 'Python', '阿里巴巴', '算法工程师', '10年以上',
]

// 搜索
async function handleSearch() {
  if (!searchKeyword.value && !selectedSkill.value) return
  hasSearched.value = true
  pageNum.value = 1
  await loadData()
}

// 技能标签筛选
function handleSkillFilter(skill: string) {
  if (selectedSkill.value === skill) {
    selectedSkill.value = ''
  } else {
    selectedSkill.value = skill
  }
  searchKeyword.value = selectedSkill.value
  handleSearch()
}

function clearSkillFilter() {
  selectedSkill.value = ''
  searchKeyword.value = ''
  hasSearched.value = false
  talentList.value = []
}

// 加载数据
async function loadData() {
  try {
    const res: any = await getTalentPool({
      keyword: searchKeyword.value,
      skill: selectedSkill.value,
      pageNum: pageNum.value,
      pageSize: pageSize.value,
    })
    talentList.value = res.data?.list || res.data?.records || []
    total.value = res.data?.total || 0
  } catch {
    ElMessage.error('搜索人才库失败')
    talentList.value = []
    total.value = 0
  }
}

// 查看详情
function handleViewDetail(talent: any) {
  router.push(`/candidate/decision?candidateId=${talent.id}&jobId=1`)
}

// 推荐到岗位
async function handleRecommend(talent: any) {
  currentTalent.value = talent
  recommendJobId.value = null
  recommendVisible.value = true
  try {
    const res: any = await getJobList({ pageNum: 1, pageSize: 100 })
    jobOptions.value = res.data?.list || res.data?.records || []
  } catch {
    ElMessage.error('加载岗位列表失败')
    jobOptions.value = []
  }
}

async function confirmRecommend() {
  if (!recommendJobId.value || !currentTalent.value) return
  recommendLoading.value = true
  try {
    await addToJob(currentTalent.value.id, recommendJobId.value)
    ElMessage.success('推荐成功')
    recommendVisible.value = false
  } catch {
    // 操作失败
  } finally {
    recommendLoading.value = false
  }
}
</script>

<style lang="scss" scoped>
@import '@/assets/styles/variables.scss';
.pool-search-section {
  background: $bg-card;
  border-radius: 8px;
  padding: 32px 32px 24px;
  margin-bottom: 20px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);

  .search-box {
    max-width: 720px;
    margin: 0 auto 20px;
  }

  .skill-tags {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 8px;
    max-width: 720px;
    margin: 0 auto;

    .skill-tags-label {
      font-size: 13px;
      color: $text-secondary;
      margin-right: 4px;
    }

    .skill-tag {
      cursor: pointer;
      transition: all 0.2s;

      &:hover {
        transform: translateY(-1px);
      }
    }
  }
}

.pool-results {
  .results-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;

    .results-count {
      font-size: 14px;
      color: $text-regular;

      strong {
        color: $primary-color;
        font-size: 16px;
      }
    }
  }

  .talent-cards {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
    gap: 16px;
    margin-bottom: 20px;
  }

  .talent-card {
    background: $bg-card;
    border-radius: 8px;
    padding: 20px;
    box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
    transition: all 0.25s;
    border: 1px solid $border-color-light;

    &:hover {
      box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
      transform: translateY(-2px);
    }

    .talent-card-header {
      display: flex;
      align-items: center;
      margin-bottom: 16px;

      .talent-avatar {
        width: 48px;
        height: 48px;
        border-radius: 50%;
        background: linear-gradient(135deg, $primary-color, $primary-light);
        color: #fff;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 18px;
        font-weight: 600;
        margin-right: 12px;
        flex-shrink: 0;
      }

      .talent-basic {
        flex: 1;
        min-width: 0;

        .talent-name {
          font-size: 16px;
          font-weight: 600;
          color: $text-primary;
          margin: 0 0 2px;
        }

        .talent-position {
          font-size: 13px;
          color: $text-secondary;
          margin: 0;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
        }
      }

      .talent-match {
        text-align: center;
        margin-left: 12px;

        .match-score {
          font-size: 24px;
          font-weight: 700;
          color: $success-color;
          line-height: 1;
        }

        .match-label {
          font-size: 11px;
          color: $text-secondary;
          margin-top: 2px;
        }
      }
    }

    .talent-card-body {
      .talent-info-row {
        display: flex;
        align-items: center;
        gap: 8px;
        font-size: 13px;
        color: $text-regular;
        margin-bottom: 8px;

        .el-icon {
          color: $text-placeholder;
          font-size: 14px;
        }
      }

      .talent-skills {
        display: flex;
        flex-wrap: wrap;
        gap: 6px;
        margin-top: 12px;
        padding-top: 12px;
        border-top: 1px solid #f5f5f5;
      }
    }

    .talent-card-footer {
      display: flex;
      justify-content: flex-end;
      gap: 8px;
      margin-top: 16px;
      padding-top: 12px;
      border-top: 1px solid #f5f5f5;
    }
  }
}

.pool-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
  text-align: center;

  .empty-illustration {
    margin-bottom: 24px;
    opacity: 0.5;
  }

  .empty-title {
    font-size: 20px;
    font-weight: 600;
    color: $text-primary;
    margin: 0 0 8px;
  }

  .empty-desc {
    font-size: 14px;
    color: $text-secondary;
    margin: 0 0 24px;
  }

  .empty-hot-skills {
    display: flex;
    align-items: center;
    gap: 8px;
    flex-wrap: wrap;

    .empty-hot-label {
      font-size: 13px;
      color: $text-secondary;
    }

    .suggested-tag {
      cursor: pointer;
      transition: all 0.2s;

      &:hover {
        transform: translateY(-1px);
        box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
      }
    }
  }
}

.title-link {
  color: $primary-color;
  cursor: pointer;
  font-weight: 500;

  &:hover {
    text-decoration: underline;
  }
}

.match-score-text {
  color: $success-color;
  font-weight: 600;
}

.talent-source-tag {
  position: absolute;
  top: 12px;
  right: 12px;
}
</style>
