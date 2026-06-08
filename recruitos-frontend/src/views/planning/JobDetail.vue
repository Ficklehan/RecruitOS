<template>
  <div class="page-container" v-loading="loading">
    <div class="page-header">
      <el-button text @click="$router.push('/planning/jobs')">
        <el-icon><ArrowLeft /></el-icon>
        返回在招职位
      </el-button>
      <div class="header-main">
        <div class="title-row">
          <h2 class="page-title">{{ job?.title || '在招职位工作台' }}</h2>
          <el-tag v-if="job" :type="statusTagType(job.status)" size="small" disable-transitions>
            {{ jobStatusLabel(job.status) }}
          </el-tag>
        </div>
        <p v-if="job" class="page-subtitle">
          编号 {{ job.jobNo }}
          <span v-if="job.demandNo"> · 关联需求 {{ job.demandNo }}</span>
          · 编制 {{ filledCount }}/{{ headCount }}
        </p>
      </div>
      <div v-if="job" class="header-actions">
        <el-button
          v-if="job.status === 'DRAFT'"
          type="success"
          @click="handleActivate"
        >
          开始招聘
        </el-button>
        <el-button
          v-if="job.status === 'ACTIVE'"
          type="warning"
          @click="handlePause"
        >
          暂停招聘
        </el-button>
        <el-button
          v-if="job.status !== 'CLOSED'"
          type="danger"
          plain
          @click="openCloseDialog"
        >
          关闭职位
        </el-button>
      </div>
    </div>

    <JobContextBar
      v-if="job"
      :model-value="job.id"
      @update:model-value="switchJob"
    >
      <el-button type="primary" link @click="activeTab = 'sourcing'">渠道招聘</el-button>
    </JobContextBar>

    <el-row :gutter="16" v-if="job">
      <el-col :span="6">
        <div class="data-card sidebar-card">
          <h4>职位概览</h4>
          <div class="stat-grid">
            <div class="stat-item">
              <span class="stat-value">{{ boardItems.length }}</span>
              <span class="stat-label">流程中候选人</span>
            </div>
            <div class="stat-item">
              <span class="stat-value">{{ requiredCount }}</span>
              <span class="stat-label">必备要求</span>
            </div>
            <div class="stat-item">
              <span class="stat-value">{{ preferredCount }}</span>
              <span class="stat-label">加分要求</span>
            </div>
          </div>

          <div v-if="skillTagNames.length" class="tag-block">
            <span class="tag-label">关键技能</span>
            <el-tag
              v-for="name in skillTagNames"
              :key="name"
              size="small"
              class="tag-item"
            >
              {{ name }}
            </el-tag>
          </div>
          <p v-else class="hint">尚未提取任职要求，完善后可自动评估匹配度</p>

          <el-button type="primary" class="mt-12 block-btn" @click="goEditRequirements">
            编辑任职要求
          </el-button>
          <el-button class="mt-8 block-btn" @click="goPipeline">
            查看招聘进展
          </el-button>
        </div>

        <div class="data-card sidebar-card mt-12">
          <h4>匹配建议分布</h4>
          <p class="hint">基于本职位已关联候选人的最新评估</p>
          <div v-for="row in matchDistribution" :key="row.label" class="dist-row">
            <span>{{ row.label }}</span>
            <el-tag size="small" :type="row.type">{{ row.count }}</el-tag>
          </div>
          <EmptyStateCta
            v-if="!matchDistribution.length"
            :image-size="48"
            description="本职位尚无候选人，可先开始渠道招聘"
            :actions="[
              { label: '开始渠道招聘', type: 'primary', onClick: () => activeTab = 'sourcing' },
              { label: '查看招聘进展', onClick: goPipeline },
            ]"
          />
        </div>
      </el-col>

      <el-col :span="18">
        <el-tabs v-model="activeTab">
          <el-tab-pane label="职位描述" name="overview">
            <div class="data-card content-card">
              <h4>职位描述</h4>
              <p class="jd-snippet">{{ displayJdText }}</p>
            </div>
            <div class="quick-links data-card mt-12">
              <h4>快捷操作</h4>
              <el-button type="primary" @click="activeTab = 'sourcing'">开始渠道招聘</el-button>
              <el-button @click="goPipeline">查看招聘进展</el-button>
              <el-button @click="goCandidates">查看候选人</el-button>
              <el-button @click="goEditRequirements">编辑任职要求</el-button>
            </div>
          </el-tab-pane>

          <el-tab-pane label="任职要求" name="requirements">
            <div class="data-card content-card">
              <div class="section-head">
                <h4>任职要求清单</h4>
                <el-button type="primary" link @click="goEditRequirements">编辑</el-button>
              </div>
              <p class="hint">必备与加分项将用于候选人匹配评估，默认不向业务同事展示技术权重。</p>

              <el-table v-if="requirements.length" :data="requirements" size="small" stripe>
                <el-table-column prop="name" label="要求项" min-width="140" />
                <el-table-column label="类型" width="90">
                  <template #default="{ row }">
                    <el-tag size="small" :type="row.requirementType === 'REQUIRED' ? 'danger' : 'info'">
                      {{ REQUIREMENT_TYPE_LABEL[row.requirementType] }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="重要程度" width="90">
                  <template #default="{ row }">
                    {{ IMPORTANCE_LABEL[row.importance] }}
                  </template>
                </el-table-column>
              </el-table>

              <EmptyStateCta
                v-else
                title="尚未设置任职要求"
                description="粘贴职位描述后可自动提取，也可手动添加必备与加分项"
                :actions="[
                  { label: '去编辑任职要求', type: 'primary', onClick: goEditRequirements },
                  { label: '提取任职要求', onClick: goEditRequirements },
                ]"
              />
            </div>
          </el-tab-pane>

          <el-tab-pane label="渠道招聘" name="sourcing">
            <JobSourcing :job-id="job.id" :job-title="job.title" :job-status="job.status" />
          </el-tab-pane>

          <el-tab-pane label="候选人进展" name="pipeline">
            <div class="data-card content-card">
              <div class="section-head">
                <p>当前共有 <strong>{{ boardItems.length }}</strong> 位候选人在本职位流程中</p>
                <el-button type="primary" @click="goPipeline">打开招聘进展看板</el-button>
              </div>

              <div v-if="stageSummary.length" class="stage-chips">
                <div v-for="s in stageSummary" :key="s.stage" class="stage-chip">
                  <span class="stage-name">{{ s.label }}</span>
                  <span class="stage-count">{{ s.count }}</span>
                </div>
              </div>

              <div v-if="boardItems.length" class="candidate-preview-list">
                <div
                  v-for="item in boardItems.slice(0, 8)"
                  :key="item.id"
                  class="candidate-preview"
                  @click="openCandidate(item)"
                >
                  <div class="preview-main">
                    <span class="preview-name">{{ item.candidateName }}</span>
                    <el-tag size="small" type="info">{{ pipelineStageLabel(item.pipelineStage || item.stage) }}</el-tag>
                  </div>
                  <div class="preview-meta">{{ item.candidateCompany || item.candidateTitle || '—' }}</div>
                  <MatchVerdict
                    :match-score="item.matchScore"
                    :match-detail="item.matchDetail"
                    mode="compact"
                    :show-score="false"
                  />
                </div>
              </div>

              <EmptyStateCta
                v-else
                title="暂无候选人"
                description="开始渠道招聘后，新候选人将出现在待处理列"
                :actions="[
                  { label: '开始渠道招聘', type: 'primary', onClick: () => activeTab = 'sourcing' },
                ]"
              />
            </div>
          </el-tab-pane>
        </el-tabs>
      </el-col>
    </el-row>

    <el-dialog v-model="closeDialogVisible" title="关闭在招职位" width="420px">
      <el-form label-width="80px">
        <el-form-item label="关闭原因">
          <el-input v-model="closeReason" type="textarea" :rows="3" placeholder="请输入关闭原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="closeDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmClose">确定关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import MatchVerdict from '@/components/match/MatchVerdict.vue'
import JobContextBar from '@/components/common/JobContextBar.vue'
import EmptyStateCta from '@/components/common/EmptyStateCta.vue'
import JobSourcing from './JobSourcing.vue'
import {
  jobStatusLabel,
  pipelineStageLabel,
} from '@/constants/businessLabels'
import {
  type RequirementItem,
  fromApiTag,
  IMPORTANCE_LABEL,
  REQUIREMENT_TYPE_LABEL,
} from '@/utils/jdRequirements'
import { parseMatchDetail, tierTagType } from '@/utils/matchVerdict'
import {
  activateJob,
  closeJob,
  getJobDetail,
  getTags,
  pauseJob,
} from '@/api/modules/job'
import { getPipelineBoard } from '@/api/modules/pipeline'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const job = ref<any>(null)
const activeTab = ref('overview')
const boardColumns = ref<any[]>([])
const boardItems = ref<any[]>([])
const requirements = ref<RequirementItem[]>([])
const closeDialogVisible = ref(false)
const closeReason = ref('')

const headCount = computed(() => job.value?.headCount ?? job.value?.headcount ?? 0)
const filledCount = computed(() => job.value?.filledCount ?? job.value?.onboardCount ?? 0)

const displayJdText = computed(() => {
  const text = String(job.value?.jdText || '').trim()
  if (!text) return '暂无职位描述，请前往「任职要求」完善'
  if (text.startsWith('[') || text.startsWith('{')) {
    try {
      JSON.parse(text)
      return '职位描述已保存，请在「任职要求」标签查看结构化要求'
    } catch { /* plain text */ }
  }
  return text
})

const skillTagNames = computed(() =>
  requirements.value
    .map(r => r.name)
    .filter(Boolean)
    .slice(0, 12),
)

const requiredCount = computed(() =>
  requirements.value.filter(r => r.requirementType === 'REQUIRED').length,
)

const preferredCount = computed(() =>
  requirements.value.filter(r => r.requirementType === 'PREFERRED').length,
)

const matchDistribution = computed(() => {
  const counts: Record<string, { count: number; tier: ReturnType<typeof parseMatchDetail>['status'] }> = {}
  for (const item of boardItems.value) {
    const v = parseMatchDetail(item.matchDetail, item.matchScore)
    if (!counts[v.label]) counts[v.label] = { count: 0, tier: v.status }
    counts[v.label].count++
  }
  return Object.entries(counts).map(([label, { count, tier }]) => ({
    label,
    count,
    type: tierTagType(tier),
  }))
})

const stageSummary = computed(() =>
  boardColumns.value.map(col => ({
    stage: col.stage,
    label: col.label || pipelineStageLabel(col.stage),
    count: col.items?.length || 0,
  })),
)

function statusTagType(status: string) {
  const map: Record<string, string> = {
    DRAFT: 'info', ACTIVE: 'success', PAUSED: 'warning', CLOSED: 'info',
  }
  return map[status] || 'info'
}

function goPipeline() {
  if (!job.value?.id) return
  router.push({ path: '/pipeline/board', query: { jobId: String(job.value.id) } })
}

function goCandidates() {
  if (!job.value?.id) return
  router.push({ path: '/pipeline/candidates', query: { jobId: String(job.value.id) } })
}

function goEditRequirements() {
  if (!job.value?.id) return
  router.push(`/planning/jobs/${job.value.id}/jd`)
}

function openCandidate(item: any) {
  const cid = item.candidateId
  if (!cid) return
  router.push({
    path: `/pipeline/candidates/${cid}`,
    query: { jobId: String(job.value?.id || '') },
  })
}

function switchJob(id: number | null) {
  if (!id || id === job.value?.id) return
  router.push(`/planning/jobs/${id}`)
}

function parseTagsFromJobField(raw: unknown): RequirementItem[] {
  if (!raw) return []
  if (Array.isArray(raw)) {
    return raw.map((t: Record<string, unknown>) => fromApiTag(t))
  }
  if (typeof raw === 'string') {
    try {
      const parsed = JSON.parse(raw)
      if (Array.isArray(parsed)) {
        return parsed.map((t: Record<string, unknown>) => fromApiTag(t))
      }
    } catch { /* ignore */ }
  }
  return []
}

async function loadJob(jobId: number) {
  loading.value = true
  try {
    const { data } = await getJobDetail(jobId)
    job.value = data

    try {
      const tagRes: any = await getTags(jobId)
      const raw = tagRes.data ?? tagRes
      const list = Array.isArray(raw) ? raw : []
      requirements.value = list.map((t: Record<string, unknown>) => fromApiTag(t))
    } catch {
      requirements.value = []
    }
    if (!requirements.value.length) {
      requirements.value = parseTagsFromJobField(data?.tags)
    }

    const boardRes: any = await getPipelineBoard(jobId)
    boardColumns.value = boardRes.data?.columns || []
    boardItems.value = boardColumns.value.flatMap((c: any) =>
      (c.items || []).map((item: any) => ({
        ...item,
        pipelineStage: item.pipelineStage || c.stage,
      })),
    )
  } finally {
    loading.value = false
  }
}

async function handleActivate() {
  if (!job.value?.id) return
  await activateJob(job.value.id)
  ElMessage.success('已开始招聘')
  await loadJob(job.value.id)
}

async function handlePause() {
  if (!job.value?.id) return
  await pauseJob(job.value.id)
  ElMessage.success('已暂停招聘')
  await loadJob(job.value.id)
}

function openCloseDialog() {
  closeReason.value = ''
  closeDialogVisible.value = true
}

async function confirmClose() {
  if (!job.value?.id || !closeReason.value.trim()) {
    ElMessage.warning('请填写关闭原因')
    return
  }
  await closeJob(job.value.id, closeReason.value.trim())
  ElMessage.success('职位已关闭')
  closeDialogVisible.value = false
  await loadJob(job.value.id)
}

watch(
  () => route.params.id,
  (id) => {
    if (id) loadJob(Number(id))
  },
)

onMounted(() => {
  if (route.query.tab && typeof route.query.tab === 'string') {
    activeTab.value = route.query.tab
  }
  loadJob(Number(route.params.id))
})
</script>

<style scoped lang="scss">
.header-main {
  flex: 1;
  min-width: 0;
}

.title-row {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.header-actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

.sidebar-card,
.content-card {
  padding: 16px;
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
  margin: 12px 0;
}

.stat-item {
  text-align: center;
  padding: 8px 4px;
  background: #f8fafc;
  border-radius: 8px;
}

.stat-value {
  display: block;
  font-size: 18px;
  font-weight: 700;
  color: #0f172a;
}

.stat-label {
  font-size: 11px;
  color: #64748b;
}

.meta-line {
  margin: 4px 0;
  font-size: 13px;
  color: #475569;
}

.tag-block {
  margin-top: 12px;
}

.tag-label {
  display: block;
  font-size: 12px;
  color: #64748b;
  margin-bottom: 6px;
}

.tag-item {
  margin: 0 4px 4px 0;
}

.dist-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 6px 0;
  font-size: 13px;
}

.jd-snippet {
  white-space: pre-wrap;
  line-height: 1.7;
  color: #334155;
  margin: 0;
}

.hint {
  color: #64748b;
  font-size: 12px;
  margin-bottom: 8px;
}

.mt-8 { margin-top: 8px; }
.mt-12 { margin-top: 12px; }

.block-btn {
  width: 100%;
  margin-left: 0 !important;
}

.quick-links {
  padding: 16px;
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}

.stage-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 16px;
}

.stage-chip {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 12px;
  background: #f1f5f9;
  border-radius: 20px;
  font-size: 13px;
}

.stage-count {
  font-weight: 700;
  color: #3b82f6;
}

.candidate-preview-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.candidate-preview {
  padding: 12px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  cursor: pointer;
  transition: border-color 0.15s, background 0.15s;

  &:hover {
    border-color: #93c5fd;
    background: #f8fafc;
  }
}

.preview-main {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.preview-name {
  font-weight: 600;
  color: #0f172a;
}

.preview-meta {
  font-size: 12px;
  color: #64748b;
  margin: 4px 0 8px;
}
</style>
