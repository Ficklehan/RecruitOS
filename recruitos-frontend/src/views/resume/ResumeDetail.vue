<template>
  <PageShell title="简历详情">
    <template #actions>
      <RButton variant="outline" @click="$router.back()">
        <ArrowLeft class="mr-2 h-4 w-4" />
        返回
      </RButton>
      <RButton @click="handleImportPool">
        <FolderPlus class="mr-2 h-4 w-4" />
        加入人才库
      </RButton>
    </template>

    <div class="detail-grid">
      <!-- 左栏：基本信息 + 经历 -->
      <div class="detail-main">
        <!-- 基本信息卡片 -->
        <div class="detail-card">
          <div class="card-header">
            <div class="candidate-avatar">{{ (resume.name || '?').charAt(0) }}</div>
            <div class="candidate-info">
              <h3 class="candidate-name">{{ resume.name || '未识别' }}</h3>
              <p class="candidate-title">{{ resume.position || '-' }} {{ resume.company ? '@ ' + resume.company : '' }}</p>
            </div>
          </div>
          <div class="info-grid">
            <div class="info-item">
              <span class="info-label">手机号</span>
              <span class="info-value">{{ resume.phone || '-' }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">邮箱</span>
              <span class="info-value">{{ resume.email || '-' }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">工作年限</span>
              <span class="info-value">{{ resume.workYears ? resume.workYears + '年' : '-' }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">学历</span>
              <span class="info-value">{{ resume.education || '-' }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">期望薪资</span>
              <span class="info-value">{{ resume.expectedSalary || '-' }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">来源</span>
              <span class="info-value">{{ sourceLabelMap[resume.source] || resume.source || '-' }}</span>
            </div>
          </div>
        </div>

        <!-- 技能标签 -->
        <div class="detail-card">
          <h4 class="card-title">技能标签</h4>
          <div class="skill-tags">
            <RBadge
              v-for="skill in resume.skills"
              :key="skill"
              variant="outline"
              class="skill-tag"
            >
              {{ skill }}
            </RBadge>
            <span v-if="!resume.skills?.length" class="empty-text">暂无技能标签</span>
          </div>
        </div>

        <!-- 工作经历 -->
        <div class="detail-card">
          <h4 class="card-title">工作经历</h4>
          <div class="timeline">
            <div v-for="(exp, index) in resume.workExperience" :key="index" class="timeline-item">
              <div class="timeline-dot" />
              <div class="timeline-content">
                <div class="timeline-header">
                  <span class="timeline-company">{{ exp.company }}</span>
                  <span class="timeline-period">{{ exp.startDate }} - {{ exp.endDate || '至今' }}</span>
                </div>
                <div class="timeline-position">{{ exp.position }}</div>
                <div class="timeline-desc">{{ exp.description }}</div>
              </div>
            </div>
            <span v-if="!resume.workExperience?.length" class="empty-text">暂无工作经历</span>
          </div>
        </div>

        <!-- 项目经历 -->
        <div class="detail-card">
          <h4 class="card-title">项目经历</h4>
          <div v-for="(proj, index) in resume.projectExperience" :key="index" class="project-item">
            <div class="project-header">
              <span class="project-name">{{ proj.name }}</span>
              <span class="project-period">{{ proj.startDate }} - {{ proj.endDate || '至今' }}</span>
            </div>
            <div class="project-role">{{ proj.role }}</div>
            <div class="project-desc">{{ proj.description }}</div>
          </div>
          <span v-if="!resume.projectExperience?.length" class="empty-text">暂无项目经历</span>
        </div>
      </div>

      <!-- 右栏：AI分析 + 操作 -->
      <div class="detail-side">
        <!-- AI分析卡片 -->
        <div class="detail-card">
          <h4 class="card-title">AI 分析</h4>
          <MatchVerdict
            :match-score="resume.matchScore"
            :match-detail="resume.matchDetail"
            mode="full"
          />
          <div v-if="resume.aiInsights" class="ai-insights">
            <div class="insight-item">
              <CircleCheck class="h-4 w-4 text-green-600 shrink-0" />
              <span>{{ resume.aiInsights.strength }}</span>
            </div>
            <div class="insight-item">
              <AlertTriangle class="h-4 w-4 text-amber-500 shrink-0" />
              <span>{{ resume.aiInsights.risk }}</span>
            </div>
          </div>
          <RButton class="w-full mt-3" @click="handleParse">
            <Wand2 class="mr-2 h-4 w-4" />
            重新解析
          </RButton>
        </div>

        <!-- 推荐在招职位 -->
        <div class="detail-card">
          <h4 class="card-title">推荐在招职位</h4>
          <div v-for="job in resume.recommendedJobs" :key="job.id" class="recommend-job">
            <div class="job-info">
              <div class="job-title">{{ job.title }}</div>
              <MatchVerdict
                :match-score="job.matchScore"
                :match-detail="job.matchDetail"
                mode="compact"
                :show-score="false"
              />
            </div>
            <RButton variant="link" size="sm">推荐</RButton>
          </div>
          <span v-if="!resume.recommendedJobs?.length" class="empty-text">暂无推荐</span>
        </div>

        <!-- 状态管理 -->
        <div class="detail-card">
          <h4 class="card-title">状态管理</h4>
          <RSelect
            v-model="resume.status"
            class="w-full"
            :options="statusOptions"
            @update:model-value="handleStatusChange"
          />
        </div>
      </div>
    </div>
</PageShell>
</template>

<script setup lang="ts">
import PageShell from '@/components/Layout/PageShell.vue'
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { toast } from '@/lib/notify'
import { ArrowLeft, FolderPlus, CircleCheck, AlertTriangle, Wand2 } from 'lucide-vue-next'
import { RButton, RBadge, RSelect } from '@/components/ui'
import MatchVerdict from '@/components/match/MatchVerdict.vue'
import { getResumeDetail, parseResume, importToTalentPool, updateResume } from '@/api/modules/resume'

const route = useRoute()
const router = useRouter()

const resume = ref<any>({
  name: '',
  phone: '',
  email: '',
  company: '',
  position: '',
  workYears: 0,
  education: '',
  expectedSalary: '',
  source: '',
  skills: [],
  workExperience: [],
  projectExperience: [],
  matchScore: 0,
  aiInsights: null,
  recommendedJobs: [],
  status: 'NEW',
})

const sourceLabelMap: Record<string, string> = {
  BOSS: 'Boss直聘', LAGOU: '拉勾', ZHILIAN: '智联招聘', LIEPIN: '猎聘',
  MANUAL: '手动上传', REFERRAL: '内推', HEADHUNTER: '猎头',
}

const statusOptions = [
  { label: '新简历', value: 'NEW' },
  { label: '待初筛', value: 'PENDING' },
  { label: '已通过', value: 'PASSED' },
  { label: '已淘汰', value: 'REJECTED' },
  { label: '已入职', value: 'ONBOARD' },
]

async function handleParse() {
  try {
    const res: any = await parseResume(route.params.id as string)
    const data = res.data || res
    Object.assign(resume.value, data)
    toast.success('解析完成')
  } catch {
    toast.error('解析失败')
  }
}

async function handleImportPool() {
  try {
    await importToTalentPool(route.params.id as string)
    toast.success('已导入人才库')
  } catch {
    toast.error('导入失败')
  }
}

async function handleStatusChange(val: string | number | undefined | null) {
  if (!val) return
  const status = String(val)
  try {
    await updateResume(route.params.id as string, { status })
    toast.success('状态已更新')
  } catch {
    toast.error('更新失败')
  }
}

async function loadData() {
  try {
    const res: any = await getResumeDetail(route.params.id as string)
    const data = res.data || res
    resume.value = { ...resume.value, ...data }
  } catch {
    toast.error('加载失败')
  }
}

onMounted(() => loadData())
</script>

<style lang="scss" scoped>
@import '@/assets/styles/variables.scss';

.header-actions {
  display: flex;
  gap: 8px;
}

.detail-grid {
  display: grid;
  grid-template-columns: 1fr 320px;
  gap: 20px;

  @media (max-width: 1200px) {
    grid-template-columns: 1fr;
  }
}

.detail-card {
  background: $bg-card;
  border-radius: $border-radius;
  padding: 24px;
  margin-bottom: 16px;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 20px;
}

.candidate-avatar {
  width: 52px;
  height: 52px;
  border-radius: 12px;
  background: linear-gradient(135deg, $primary-color, $primary-dark);
  color: #fff;
  font-size: 20px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.candidate-name {
  font-size: 18px;
  font-weight: 700;
  color: $text-primary;
  margin-bottom: 4px;
}

.candidate-title {
  font-size: 14px;
  color: $text-secondary;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;

  @media (max-width: 768px) {
    grid-template-columns: repeat(2, 1fr);
  }
}

.info-item {
  .info-label {
    display: block;
    font-size: 12px;
    color: $text-placeholder;
    margin-bottom: 4px;
  }

  .info-value {
    font-size: 14px;
    color: $text-primary;
    font-weight: 500;
  }
}

.card-title {
  font-size: 14px;
  font-weight: 600;
  color: $text-primary;
  margin-bottom: 16px;
}

.skill-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.skill-tag {
  border-radius: $border-radius-full;
}

.timeline {
  position: relative;
  padding-left: 20px;
}

.timeline-item {
  position: relative;
  padding-bottom: 20px;
  padding-left: 16px;
  border-left: 2px solid $bg-muted;

  &:last-child {
    border-left-color: transparent;
    padding-bottom: 0;
  }
}

.timeline-dot {
  position: absolute;
  left: -6px;
  top: 2px;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: $primary-color;
}

.timeline-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 4px;
}

.timeline-company {
  font-size: 14px;
  font-weight: 600;
  color: $text-primary;
}

.timeline-period {
  font-size: 12px;
  color: $text-placeholder;
}

.timeline-position {
  font-size: 13px;
  color: $text-secondary;
  margin-bottom: 6px;
}

.timeline-desc {
  font-size: 13px;
  color: $text-regular;
  line-height: 1.6;
}

.project-item {
  padding: 16px;
  background: $bg-muted;
  border-radius: $border-radius-sm;
  margin-bottom: 12px;

  &:last-child {
    margin-bottom: 0;
  }
}

.project-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 4px;
}

.project-name {
  font-size: 14px;
  font-weight: 600;
  color: $text-primary;
}

.project-period {
  font-size: 12px;
  color: $text-placeholder;
}

.project-role {
  font-size: 13px;
  color: $text-secondary;
  margin-bottom: 6px;
}

.project-desc {
  font-size: 13px;
  color: $text-regular;
  line-height: 1.6;
}

.ai-score {
  display: flex;
  justify-content: center;
  margin-bottom: 16px;
}

.score-circle {
  width: 100px;
  height: 100px;
  border-radius: 50%;
  border: 4px solid;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;

  .score-value {
    font-size: 28px;
    font-weight: 700;
    color: $text-primary;
    line-height: 1;
  }

  .score-label {
    font-size: 12px;
    color: $text-secondary;
    margin-top: 4px;
  }
}

.ai-insights {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.insight-item {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  font-size: 13px;
  color: $text-regular;
  line-height: 1.5;
}

.recommend-job {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 0;
  border-bottom: 1px solid $bg-muted;

  &:last-child {
    border-bottom: none;
  }

  .job-title {
    font-size: 14px;
    color: $text-primary;
    font-weight: 500;
  }

  .job-match {
    font-size: 12px;
    color: $text-secondary;
    margin-top: 2px;
  }
}

.empty-text {
  font-size: 13px;
  color: $text-placeholder;
}
</style>
