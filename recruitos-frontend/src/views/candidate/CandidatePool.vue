<template>
  <PageShell title="人才库" subtitle="搜索储备人才；选择在招职位后可查看匹配建议并推荐到职位">
    <RCard class="p-4">
      <div class="flex flex-wrap items-center gap-4">
        <div class="flex items-center gap-2">
          <span class="text-sm text-muted-foreground">在招职位</span>
          <RSelect
            v-model="contextJobId"
            :options="jobSelectOptions"
            placeholder="请先选择在招职位（必选，才能显示匹配建议）"
            clearable
            class="w-full sm:w-80"
            @update:model-value="onJobContextChange"
          />
        </div>
        <RAlert v-if="!contextJobId" class="flex-1 min-w-[240px]">
          <AlertTitle>提示</AlertTitle>
          <AlertDescription>默认展示当前不在招聘流程中的人才；选择职位后可查看匹配建议</AlertDescription>
        </RAlert>
      </div>
    </RCard>

    <RCard class="p-8">
      <div class="flex flex-col sm:flex-row gap-2 mb-5">
        <RInput
          v-model="searchKeyword"
          placeholder="搜索人才库 - 输入姓名、技能或公司名称..."
          class="flex-1"
          @keyup.enter="handleSearch"
        />
        <RButton @click="handleSearch">
          <Search class="mr-2 h-4 w-4" />
          搜索
        </RButton>
      </div>

      <div class="flex flex-wrap items-center gap-2">
        <span class="text-sm text-muted-foreground">热门技能:</span>
        <RBadge
          v-for="tag in hotSkills"
          :key="tag"
          :variant="selectedSkill === tag ? 'default' : 'secondary'"
          class="cursor-pointer"
          @click="handleSkillFilter(tag)"
        >
          {{ tag }}
        </RBadge>
        <RBadge
          v-if="selectedSkill"
          variant="destructive"
          class="cursor-pointer"
          @click="clearSkillFilter"
        >
          清除筛选
        </RBadge>
      </div>
    </RCard>

    <div v-if="hasSearched">
      <div class="flex items-center justify-between mb-4">
        <span class="text-sm text-muted-foreground">
          共 <strong class="text-foreground">{{ total }}</strong> 位人才
          <span v-if="!isSearchMode" class="ml-1.5">（不在流程中）</span>
        </span>
        <div class="flex gap-1">
          <RButton
            size="sm"
            :variant="viewMode === 'card' ? 'default' : 'outline'"
            @click="viewMode = 'card'"
          >
            卡片
          </RButton>
          <RButton
            size="sm"
            :variant="viewMode === 'list' ? 'default' : 'outline'"
            @click="viewMode = 'list'"
          >
            列表
          </RButton>
        </div>
      </div>

      <div v-if="viewMode === 'card'" class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4 mb-5">
        <RCard v-for="talent in talentList" :key="talent.id" class="p-5">
          <div class="flex items-start gap-3 mb-3">
            <div class="flex h-12 w-12 shrink-0 items-center justify-center rounded-full bg-primary text-primary-foreground font-semibold">
              {{ talent.name?.charAt(0) }}
            </div>
            <div class="flex-1 min-w-0">
              <h3 class="font-semibold truncate">{{ talent.name }}</h3>
              <p class="text-sm text-muted-foreground truncate">{{ talent.position || talent.currentTitle }}</p>
            </div>
            <div v-if="contextJobId && talent.matchDetail" class="shrink-0">
              <MatchVerdict
                :match-score="talent.matchScore"
                :match-detail="talent.matchDetail"
                mode="compact"
                :show-score="false"
              />
            </div>
          </div>

          <div class="space-y-1.5 text-sm">
            <div class="flex items-center gap-2 text-muted-foreground">
              <Building2 class="h-4 w-4 shrink-0" />
              <span>{{ talent.company || talent.currentCompany }}</span>
            </div>
            <div class="flex items-center gap-2 text-muted-foreground">
              <Clock class="h-4 w-4 shrink-0" />
              <span>{{ talent.workYears }}年工作经验</span>
            </div>
            <div class="flex flex-wrap gap-1.5 pt-1">
              <RBadge v-for="skill in parseSkills(talent).slice(0, 4)" :key="skill" variant="secondary">
                {{ skill }}
              </RBadge>
            </div>
            <div v-if="contextJobId && talent.matchDetail" class="pt-3 mt-3 border-t">
              <MatchVerdict
                :match-score="talent.matchScore"
                :match-detail="talent.matchDetail"
                mode="card"
              />
            </div>
          </div>

          <div class="flex justify-end gap-2 mt-4 pt-3 border-t">
            <RButton variant="link" class="h-auto p-0" @click="handleViewDetail(talent)">查看详情</RButton>
            <RButton v-if="talent.resumeId" variant="link" class="h-auto p-0 text-amber-600" @click="goResume(talent)">查看简历</RButton>
            <RButton variant="link" class="h-auto p-0 text-green-600" @click="handleRecommend(talent)">推荐到职位</RButton>
          </div>
        </RCard>
      </div>

      <RCard v-else class="overflow-hidden">
        <RTable v-if="talentList.length">
          <RTableHead>
            <RTableRow>
              <RTableTh class="w-[100px]">姓名</RTableTh>
              <RTableTh class="min-w-[140px]">公司</RTableTh>
              <RTableTh class="min-w-[140px]">职位</RTableTh>
              <RTableTh v-if="contextJobId" class="min-w-[220px]">匹配建议</RTableTh>
              <RTableTh class="w-[100px] text-center">操作</RTableTh>
            </RTableRow>
          </RTableHead>
          <RTableBody>
            <RTableRow v-for="row in talentList" :key="row.id">
              <RTableCell>
                <button type="button" class="font-medium text-primary hover:underline" @click="handleViewDetail(row)">
                  {{ row.name }}
                </button>
              </RTableCell>
              <RTableCell>{{ row.company || row.currentCompany }}</RTableCell>
              <RTableCell>{{ row.position || row.currentTitle }}</RTableCell>
              <RTableCell v-if="contextJobId">
                <MatchVerdict
                  v-if="row.matchDetail"
                  :match-score="row.matchScore"
                  :match-detail="row.matchDetail"
                  mode="compact"
                  :show-score="false"
                />
                <span v-else class="text-sm text-muted-foreground">待评估</span>
              </RTableCell>
              <RTableCell class="text-center">
                <RowActions :actions="getRowActions(row)" @action="(cmd) => handleRowCommand(cmd, row)" />
              </RTableCell>
            </RTableRow>
          </RTableBody>
        </RTable>
      </RCard>

      <EmptyStateCta
        v-if="!talentList.length && !loading"
        title="未找到匹配人才"
        description="尝试更换关键词或技能标签；也可从简历收件导入新人才"
        :actions="[
          { label: '上传简历', type: 'primary', onClick: () => router.push('/talent/resumes/upload') },
          { label: '查看简历收件', type: 'default', onClick: () => router.push('/talent/resumes') },
        ]"
      />

      <ListPagination
        v-if="talentList.length"
        v-model:page-num="pageNum"
        v-model:page-size="pageSize"
        :total="total"
        @change="loadData"
      />
    </div>

    <div v-else-if="loading" class="text-center py-20">
      <Loader2 class="h-12 w-12 animate-spin mx-auto text-muted-foreground" />
      <p class="text-sm text-muted-foreground mt-4">加载人才库...</p>
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

    <RDialog v-model:open="recommendVisible">
      <DialogContent class="max-w-md">
        <DialogHeader>
          <DialogTitle>推荐到在招职位</DialogTitle>
        </DialogHeader>
        <div class="grid gap-4 py-2">
          <FormField label="候选人">
            <RInput :model-value="currentTalent?.name" disabled />
          </FormField>
          <FormField label="在招职位">
            <RSelect
              v-model="recommendJobId"
              :options="jobSelectOptions"
              placeholder="请选择推荐职位"
              class="w-full"
            />
          </FormField>
        </div>
        <DialogFooter>
          <RButton variant="outline" @click="recommendVisible = false">取消</RButton>
          <RButton :disabled="recommendLoading" @click="confirmRecommend">确定推荐</RButton>
        </DialogFooter>
      </DialogContent>
    </RDialog>
  </PageShell>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Search, Building2, Clock, Loader2 } from 'lucide-vue-next'
import { toast } from '@/lib/notify'
import PageShell from '@/components/Layout/PageShell.vue'
import RowActions from '@/components/common/RowActions.vue'
import ListPagination from '@/components/common/ListPagination.vue'
import FormField from '@/components/app/FormField.vue'
import MatchVerdict from '@/components/match/MatchVerdict.vue'
import EmptyStateCta from '@/components/common/EmptyStateCta.vue'
import {
  RButton,
  RInput,
  RSelect,
  RBadge,
  RCard,
  RTable,
  RTableHead,
  RTableBody,
  RTableRow,
  RTableTh,
  RTableCell,
  RAlert,
  AlertTitle,
  AlertDescription,
  RDialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from '@/components/ui'
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
const jobSelectOptions = computed(() => jobOptions.value.map((j) => ({ label: j.title, value: j.id })))

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

function getRowActions(_row: any) {
  return [
    { command: 'view', label: '查看详情', icon: 'View', primary: true },
    { command: 'link', label: '关联职位', icon: 'Connection' },
  ]
}

function handleRowCommand(cmd: string, row: any) {
  if (cmd === 'view') handleViewDetail(row)
  else if (cmd === 'link') handleRecommend(row)
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
    toast.error('加载人才库失败')
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
    toast.success('已推荐到职位，可在招聘进展中查看')
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
