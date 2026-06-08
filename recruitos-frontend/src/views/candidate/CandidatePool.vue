<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h2 class="page-title">人才库</h2>
        <p class="page-subtitle">搜索储备人才；选择在招职位后可查看匹配建议并推荐到职位</p>
      </div>
    </div>

    <!-- 岗位上下文：匹配结论必须先选岗位 -->
    <div class="pool-context-bar data-card">
      <div class="context-left">
        <span class="context-label">在招职位</span>
        <el-select
          v-model="contextJobId"
          placeholder="请先选择在招职位（必选，才能显示匹配建议）"
          filterable
          clearable
          style="width: 320px"
          @change="onJobContextChange"
        >
          <el-option v-for="job in jobOptions" :key="job.id" :label="job.title" :value="job.id" />
        </el-select>
      </div>
      <el-alert
        v-if="!contextJobId"
        type="info"
        :closable="false"
        show-icon
        title="默认展示当前不在招聘流程中的人才；选择职位后可查看匹配建议"
        class="context-alert"
      />
    </div>

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
        <el-tag v-if="selectedSkill" type="danger" effect="plain" class="skill-tag" @click="clearSkillFilter">
          清除筛选
        </el-tag>
      </div>
    </div>

    <div v-if="hasSearched" class="pool-results">
      <div class="results-header">
        <span class="results-count">
          共 <strong>{{ total }}</strong> 位人才
          <span v-if="!isSearchMode" class="results-hint">（不在流程中）</span>
        </span>
        <el-radio-group v-model="viewMode" size="small">
          <el-radio-button value="card">卡片</el-radio-button>
          <el-radio-button value="list">列表</el-radio-button>
        </el-radio-group>
      </div>

      <div v-if="viewMode === 'card'" class="talent-cards">
        <div v-for="talent in talentList" :key="talent.id" class="talent-card">
          <div class="talent-card-header">
            <div class="talent-avatar">{{ talent.name?.charAt(0) }}</div>
            <div class="talent-basic">
              <h3 class="talent-name">{{ talent.name }}</h3>
              <p class="talent-position">{{ talent.position || talent.currentTitle }}</p>
            </div>
            <div v-if="contextJobId && talent.matchDetail" class="talent-match-verdict">
              <MatchVerdict
                :match-score="talent.matchScore"
                :match-detail="talent.matchDetail"
                mode="compact"
                :show-score="false"
              />
            </div>
          </div>

          <div class="talent-card-body">
            <div class="talent-info-row">
              <el-icon><OfficeBuilding /></el-icon>
              <span>{{ talent.company || talent.currentCompany }}</span>
            </div>
            <div class="talent-info-row">
              <el-icon><Timer /></el-icon>
              <span>{{ talent.workYears }}年工作经验</span>
            </div>
            <div class="talent-skills">
              <el-tag v-for="skill in parseSkills(talent).slice(0, 4)" :key="skill" size="small" type="info" effect="plain">
                {{ skill }}
              </el-tag>
            </div>
            <div v-if="contextJobId && talent.matchDetail" class="match-preview">
              <MatchVerdict
                :match-score="talent.matchScore"
                :match-detail="talent.matchDetail"
                mode="card"
              />
            </div>
          </div>

          <div class="talent-card-footer">
            <el-button type="primary" link @click="handleViewDetail(talent)">查看详情</el-button>
            <el-button v-if="talent.resumeId" type="warning" link @click="goResume(talent)">查看简历</el-button>
            <el-button type="success" link @click="handleRecommend(talent)">推荐到职位</el-button>
          </div>
        </div>
      </div>

      <div v-else class="data-card">
        <el-table :data="talentList" stripe highlight-current-row>
          <el-table-column prop="name" label="姓名" width="100">
            <template #default="{ row }">
              <span class="title-link" @click="handleViewDetail(row)">{{ row.name }}</span>
            </template>
          </el-table-column>
          <el-table-column label="公司" min-width="140" show-overflow-tooltip>
            <template #default="{ row }">{{ row.company || row.currentCompany }}</template>
          </el-table-column>
          <el-table-column label="职位" min-width="140" show-overflow-tooltip>
            <template #default="{ row }">{{ row.position || row.currentTitle }}</template>
          </el-table-column>
          <el-table-column v-if="contextJobId" label="匹配建议" min-width="220">
            <template #default="{ row }">
              <MatchVerdict
                v-if="row.matchDetail"
                :match-score="row.matchScore"
                :match-detail="row.matchDetail"
                mode="compact"
                :show-score="false"
              />
              <span v-else class="text-muted">待评估</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="180" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" link size="small" @click="handleViewDetail(row)">详情</el-button>
              <el-button type="success" link size="small" @click="handleRecommend(row)">推荐</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <EmptyStateCta
        v-if="!talentList.length && !loading"
        title="未找到匹配人才"
        description="尝试更换关键词或技能标签；也可从简历收件导入新人才"
        :actions="[
          { label: '上传简历', type: 'primary', onClick: () => router.push('/talent/resumes/upload') },
          { label: '查看简历收件', type: 'default', onClick: () => router.push('/talent/resumes') },
        ]"
      />

      <el-pagination
        v-if="talentList.length"
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        :page-sizes="[12, 24, 48]"
        :total="total"
        layout="total, sizes, prev, pager, next"
        @size-change="handleSearch"
        @current-change="handleSearch"
      />
    </div>

    <div v-else-if="loading" class="pool-empty">
      <el-icon class="is-loading" :size="48"><Loading /></el-icon>
      <p class="empty-desc">加载人才库...</p>
    </div>

    <EmptyStateCta
      v-else
      title="开始搜索人才"
      description="输入姓名、技能或公司名开始搜索；也可从简历收件导入人才"
      :actions="[
        { label: '上传简历', type: 'primary', onClick: () => router.push('/talent/resumes/upload') },
        { label: '查看简历收件', type: 'default', onClick: () => router.push('/talent/resumes') },
      ]"
    />

    <el-dialog v-model="recommendVisible" title="推荐到在招职位" width="480px">
      <el-form label-width="96px">
        <el-form-item label="候选人">
          <el-input :model-value="currentTalent?.name" disabled />
        </el-form-item>
        <el-form-item label="在招职位">
          <el-select v-model="recommendJobId" placeholder="请选择推荐职位" style="width: 100%">
            <el-option v-for="job in jobOptions" :key="job.id" :label="job.title" :value="job.id" />
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
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search, OfficeBuilding, Timer, Loading } from '@element-plus/icons-vue'
import MatchVerdict from '@/components/match/MatchVerdict.vue'
import EmptyStateCta from '@/components/common/EmptyStateCta.vue'
import { getTalentPool, addToJob } from '@/api/modules/candidate'
import { getJobList } from '@/api/modules/job'

const router = useRouter()

const searchKeyword = ref('')
const selectedSkill = ref('')
const hasSearched = ref(false)
const loading = ref(false)
const viewMode = ref<'card' | 'list'>('card')
const pageNum = ref(1)
const pageSize = ref(12)
const total = ref(0)
const talentList = ref<any[]>([])
const contextJobId = ref<number | null>(null)
const jobOptions = ref<any[]>([])

const recommendVisible = ref(false)
const recommendLoading = ref(false)
const currentTalent = ref<any>(null)
const recommendJobId = ref<number | null>(null)

const hotSkills = ['Python', 'Java', 'React', 'Vue', 'Go', 'TypeScript', 'Kubernetes', 'MySQL']

const isSearchMode = computed(() =>
  Boolean(searchKeyword.value.trim() || selectedSkill.value)
)

function goResume(talent: any) {
  router.push(`/talent/resumes/${talent.resumeId}`)
}

function parseSkills(talent: any): string[] {
  if (Array.isArray(talent.skills)) return talent.skills
  if (talent.tags) return String(talent.tags).split(/[,，、]/).map((s: string) => s.trim()).filter(Boolean)
  return []
}

function mapTalent(row: any) {
  return {
    ...row,
    company: row.currentCompany,
    position: row.currentTitle,
    skills: parseSkills(row),
  }
}

async function loadJobs() {
  try {
    const res: any = await getJobList({ pageNum: 1, pageSize: 100, status: 'ACTIVE' })
    jobOptions.value = res.data?.list || res.data?.records || []
  } catch {
    jobOptions.value = []
  }
}

function onJobContextChange() {
  if (hasSearched.value) loadData()
}

async function handleSearch() {
  hasSearched.value = true
  pageNum.value = 1
  await loadData()
}

function handleSkillFilter(skill: string) {
  selectedSkill.value = selectedSkill.value === skill ? '' : skill
  searchKeyword.value = selectedSkill.value
  handleSearch()
}

function clearSkillFilter() {
  selectedSkill.value = ''
  searchKeyword.value = ''
  pageNum.value = 1
  loadBrowsePool()
}

async function loadBrowsePool() {
  hasSearched.value = true
  pageNum.value = 1
  await loadData()
}

async function loadData() {
  loading.value = true
  try {
    const keyword = searchKeyword.value.trim()
    const res: any = await getTalentPool({
      keyword: keyword || undefined,
      tags: selectedSkill.value || undefined,
      jobId: contextJobId.value || undefined,
      pageNum: pageNum.value,
      pageSize: pageSize.value,
    })
    const rows = res.data?.list || res.data?.records || []
    talentList.value = rows.map(mapTalent)
    total.value = res.data?.total || 0
  } catch {
    ElMessage.error('加载人才库失败')
    talentList.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function handleViewDetail(talent: any) {
  const query: Record<string, string> = {}
  if (contextJobId.value) query.jobId = String(contextJobId.value)
  router.push({ path: `/pipeline/candidates/${talent.id}`, query })
}

async function handleRecommend(talent: any) {
  currentTalent.value = talent
  recommendJobId.value = contextJobId.value
  recommendVisible.value = true
  if (!jobOptions.value.length) await loadJobs()
}

async function confirmRecommend() {
  if (!recommendJobId.value || !currentTalent.value) return
  recommendLoading.value = true
  try {
    await addToJob(currentTalent.value.id, recommendJobId.value)
    ElMessage.success('已推荐到职位，可在招聘进展中查看')
    recommendVisible.value = false
    if (hasSearched.value) loadData()
  } finally {
    recommendLoading.value = false
  }
}

onMounted(async () => {
  await loadJobs()
  await loadBrowsePool()
})
</script>

<style lang="scss" scoped>
@import '@/assets/styles/variables.scss';
.pool-context-bar {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}
.context-label { font-size: 13px; color: $text-secondary; margin-right: 8px; }
.context-alert { flex: 1; min-width: 240px; }
.results-hint { margin-left: 6px; font-size: 13px; color: $text-secondary; font-weight: normal; }
.pool-search-section {
  background: $bg-card;
  border-radius: 8px;
  padding: 32px 32px 24px;
  margin-bottom: 20px;
  .search-box { max-width: 720px; margin: 0 auto 20px; }
  .skill-tags { display: flex; flex-wrap: wrap; gap: 8px; max-width: 720px; margin: 0 auto; }
}
.pool-results .talent-cards {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 16px;
  margin-bottom: 20px;
}
.talent-card {
  background: $bg-card;
  border-radius: 8px;
  padding: 20px;
  border: 1px solid $border-color-light;
}
.talent-card-header { display: flex; align-items: flex-start; gap: 12px; margin-bottom: 12px; }
.talent-avatar {
  width: 48px; height: 48px; border-radius: 50%;
  background: linear-gradient(135deg, $primary-color, $primary-light);
  color: #fff; display: flex; align-items: center; justify-content: center;
  font-weight: 600; flex-shrink: 0;
}
.talent-basic { flex: 1; min-width: 0; }
.talent-name { margin: 0; font-size: 16px; }
.talent-position { margin: 0; font-size: 13px; color: $text-secondary; }
.talent-info-row { display: flex; align-items: center; gap: 8px; font-size: 13px; margin-bottom: 6px; }
.talent-skills { display: flex; flex-wrap: wrap; gap: 6px; margin: 8px 0; }
.match-preview { margin-top: 12px; padding-top: 12px; border-top: 1px solid #f1f5f9; }
.talent-card-footer { display: flex; justify-content: flex-end; gap: 8px; margin-top: 12px; }
.pool-empty { text-align: center; padding: 80px 20px; }
.title-link { color: $primary-color; cursor: pointer; }
.text-muted { color: #94a3b8; font-size: 12px; }
</style>
