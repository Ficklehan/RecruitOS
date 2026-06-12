<template>
  <PageShell title="任职要求">
    <div class="editor-layout">
      <div class="editor-left">
        <div class="editor-panel">
          <div class="panel-header">
            <h3 class="panel-title">{{ OBJECTS.jobDescription }}</h3>
            <Badge v-if="jobForm.status" :variant="elTagTypeToBadge(getStatusType(jobForm.status))">
              {{ jobStatusLabel(jobForm.status) }}
            </Badge>
          </div>

          <div class="panel-body">
            <FormField label="职位名称" class="form-section">
              <Input v-model="jobForm.title" placeholder="如：高级前端工程师" class="text-base" />
            </FormField>

            <FormField label="关联招聘需求编号" class="form-section">
              <Input v-model="jobForm.demandNo" placeholder="选填" />
            </FormField>

            <FormField label="招聘人数" class="form-section">
              <NumberInput v-model="jobForm.headcount" :min="1" :max="999" class="w-full" />
            </FormField>

            <!-- AI 生成JD -->
            <div class="form-section">
              <div class="flex items-center gap-2 mb-2">
                <Sparkles class="h-4 w-4 text-primary" />
                <span class="text-sm font-medium text-foreground">AI 一句话生成 JD</span>
                <Badge variant="outline" class="text-[10px] px-1.5 py-0 h-4">实验性</Badge>
              </div>
              <div class="flex gap-2">
                <Textarea
                  v-model="aiPrompt"
                  :rows="2"
                  placeholder="例如：支付团队Q4要扛10倍交易量，现在4个人都没做过大规模分布式系统，需要找个能做分布式事务的"
                  class="flex-1"
                  :disabled="aiGenerating"
                />
                <Button
                  size="sm"
                  class="shrink-0 h-auto"
                  :disabled="!aiPrompt.trim() || aiGenerating"
                  @click="handleAiDraft"
                >
                  <Wand2 v-if="!aiGenerating" class="mr-1 h-3.5 w-3.5" />
                  <Loader2 v-else class="mr-1 h-3.5 w-3.5 animate-spin" />
                  {{ aiGenerating ? '生成中' : '生成' }}
                </Button>
              </div>
              <p v-if="aiResult" class="text-xs text-success mt-2">
                AI 已生成 JD 草稿，已填入下方编辑区，请审阅修改后保存
              </p>
            </div>

            <FormField label="职位描述全文" class="form-section">
              <p class="label-hint mb-2">粘贴完整 JD，系统将自动提取技能要求</p>
              <Textarea
                v-model="jobForm.jdText"
                :rows="20"
                placeholder="请粘贴或输入职位描述与任职要求…"
                class="jd-textarea"
              />
            </FormField>

            <div class="form-actions">
              <Button size="lg" :disabled="saving" @click="handleSave">
                <Check class="mr-2 h-4 w-4" />
                保存职位
              </Button>
              <Button size="lg" variant="outline" :disabled="parsing" @click="handleParseJd">
                <Wand2 class="mr-2 h-4 w-4" />
                提取任职要求
              </Button>
            </div>
          </div>
        </div>
      </div>

      <div class="editor-right">
        <div class="tag-panel">
          <div class="panel-header">
            <h3 class="panel-title">{{ OBJECTS.jobRequirements }}</h3>
            <Button v-if="requirements.length" variant="link" size="sm" :disabled="savingTags" @click="handleSaveTags">
              保存
            </Button>
          </div>

          <EmptyStateCta
            v-if="requirements.length === 0"
            title="尚未提取任职要求"
            description="在左侧填写职位描述后，点击「提取任职要求」自动识别技能与经验要求"
            :image-size="64"
            :actions="[
              { label: '提取任职要求', type: 'primary', onClick: handleParseJd },
            ]"
          />

          <div v-else class="req-panel-body">
            <p class="req-hint">标记「必备」或「加分」，并设置重要程度。系统据此评估候选人匹配度。</p>

            <Table class="req-table">
              <TableHeader>
                <TableRow>
                  <TableHead class="min-w-[120px]">要求项</TableHead>
                  <TableHead class="w-[100px]">类型</TableHead>
                  <TableHead class="w-[100px]">重要程度</TableHead>
                  <TableHead class="w-[70px] text-center">锁定</TableHead>
                  <TableHead class="w-[56px]" />
                </TableRow>
              </TableHeader>
              <TableBody>
                <TableRow v-for="(row, $index) in requirements" :key="$index">
                  <TableCell>
                    <Input v-if="row._editing" v-model="row.name" class="h-8" />
                    <span v-else>{{ row.name }}</span>
                  </TableCell>
                  <TableCell>
                    <Select
                      :model-value="row.requirementType"
                      :options="requirementTypeOptions"
                      :disabled="row.locked"
                      class="h-8"
                      @update:model-value="(v) => { row.requirementType = String(v) as RequirementType; onRequirementChange(row) }"
                    />
                  </TableCell>
                  <TableCell>
                    <Select
                      :model-value="row.importance"
                      :options="importanceOptions"
                      :disabled="row.locked"
                      class="h-8"
                      @update:model-value="(v) => { row.importance = String(v) as ImportanceLevel; onRequirementChange(row) }"
                    />
                  </TableCell>
                  <TableCell class="text-center">
                    <Switch v-model="row.locked" />
                  </TableCell>
                  <TableCell class="text-center">
                    <Button variant="link" size="sm" class="text-destructive" @click="removeRequirement($index)">删除</Button>
                  </TableCell>
                </TableRow>
              </TableBody>
            </Table>

            <Button variant="outline" class="add-req-btn w-full" @click="addRequirement">
              <Plus class="mr-2 h-4 w-4" />
              手动添加要求
            </Button>
          </div>

          <div v-if="requirements.length > 0" class="tag-footer">
            <Button variant="outline" :disabled="savingTags" @click="handleResetTags">
              <RefreshCw class="mr-2 h-4 w-4" />
              恢复
            </Button>
            <Button :disabled="savingTags" @click="handleSaveTags">
              <Check class="mr-2 h-4 w-4" />
              保存任职要求
            </Button>
          </div>
        </div>

        <div class="ops-pack-panel">
          <div class="panel-header">
            <h3 class="panel-title">{{ OBJECTS.sourcingMethod }}</h3>
          </div>
          <div class="ops-pack-body">
            <p class="req-hint">招人方式已迁移至在招职位工作台。请在工作台「规则 → 招人方式」中设置。</p>
            <Button v-if="jobId" @click="goSourcingMethod">
              前往设置招人方式
            </Button>
          </div>
        </div>
      </div>
    </div>
</PageShell>
</template>

<script setup lang="ts">
import PageShell from '@/components/Layout/PageShell.vue'
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ArrowLeft, Check, Wand2, RefreshCw, Plus } from 'lucide-vue-next'
import request from '@/api/request'
import { toast } from '@/lib/notify'
import { elTagTypeToBadge } from '@/lib/badgeVariants'
import FormField from '@/components/app/FormField.vue'
import NumberInput from '@/components/app/NumberInput.vue'
import EmptyStateCta from '@/components/common/EmptyStateCta.vue'
import { OBJECTS, jobStatusLabel } from '@/constants/businessLabels'
import {
  type RequirementItem,
  type RequirementType,
  type ImportanceLevel,
  fromApiTag,
  toApiTag,
  applyRequirementChange,
} from '@/utils/jdRequirements'
import {
  Button, Badge, Input, Textarea, Switch,
  Table, TableHeader, TableBody, TableRow, TableHead, TableCell, Select,
} from '@/components/ui'
import {
  getJobDetail,
  createJob,
  updateJob,
  parseJd as parseJdApi,
  getTags,
  updateTags,
  generateOpsPack,
  getActiveOpsPack,
  confirmOpsPack,
} from '@/api/modules/job'

const router = useRouter()
const route = useRoute()

const jobId = ref<number | null>(null)
const saving = ref(false)
const parsing = ref(false)
const savingTags = ref(false)
const generatingOps = ref(false)
const confirmingOps = ref(false)
const activeOpsPack = ref<any>(null)
const draftOpsPack = ref<any>(null)
const opsPackPreview = ref('')

const requirementTypeOptions = [
  { label: '必备', value: 'REQUIRED' },
  { label: '加分', value: 'PREFERRED' },
]
const importanceOptions = [
  { label: '高', value: 'HIGH' },
  { label: '中', value: 'MEDIUM' },
  { label: '低', value: 'LOW' },
]

const jobForm = reactive({
  title: '',
  demandNo: '',
  headcount: 1,
  jdText: '',
  status: '',
})

type EditableRequirement = RequirementItem & { _editing?: boolean }

const requirements = ref<EditableRequirement[]>([])
const originalRequirementsJson = ref('')

function getStatusType(status: string) {
  const map: Record<string, string> = {
    DRAFT: 'info', ACTIVE: 'success', PAUSED: 'warning', CLOSED: 'info',
  }
  return map[status] || 'info'
}

function onRequirementChange(row: EditableRequirement) {
  Object.assign(row, applyRequirementChange(row))
}

function addRequirement() {
  requirements.value.push({
    ...applyRequirementChange({
      name: '',
      requirementType: 'REQUIRED',
      importance: 'MEDIUM',
      locked: false,
      matchWeight: 0,
      searchWeight: 0,
      decisionWeight: 0,
    }),
    _editing: true,
  })
}

function removeRequirement(index: number) {
  requirements.value.splice(index, 1)
}

async function loadJobDetail() {
  if (!jobId.value) return
  const res: any = await getJobDetail(jobId.value)
  const data = res.data || res
  jobForm.title = data.title || ''
  jobForm.demandNo = data.demandNo || ''
  jobForm.headcount = data.headcount || 1
  jobForm.jdText = data.jdText || ''
  jobForm.status = data.status || ''
}

async function loadTags() {
  if (!jobId.value) return
  const res: any = await getTags(jobId.value)
  const raw = res.data || res || []
  requirements.value = (Array.isArray(raw) ? raw : []).map((t: Record<string, unknown>) => fromApiTag(t))
  originalRequirementsJson.value = JSON.stringify(requirements.value)
}

async function handleSave() {
  if (!jobForm.title.trim()) {
    toast.error('请输入职位名称')
    return
  }
  saving.value = true
  try {
    const data = {
      title: jobForm.title,
      demandNo: jobForm.demandNo,
      headcount: jobForm.headcount,
      jdText: jobForm.jdText,
    }
    if (jobId.value) {
      await updateJob(jobId.value, data)
      toast.success('职位已更新')
    } else {
      const res: any = await createJob(data)
      const newId = res.data?.id || res?.id
      if (newId) {
        jobId.value = newId
        router.replace({ query: { id: String(newId) } })
      }
      toast.success('职位已创建')
    }
  } catch {
    toast.error('保存失败，请重试')
  } finally {
    saving.value = false
  }
}

async function handleParseJd() {
  if (!jobForm.jdText.trim()) {
    toast.error('请先输入职位描述')
    return
  }
  if (!jobId.value) {
    await handleSave()
    if (!jobId.value) return
  } else {
    await handleSave()
  }

  parsing.value = true
  try {
    await parseJdApi(jobId.value!)
    toast.success('任职要求已提取')
    await loadTags()
  } finally {
    parsing.value = false
  }
}

async function handleSaveTags() {
  if (!jobId.value) return
  const invalid = requirements.value.find(r => !r.name.trim())
  if (invalid) {
    toast.error('请填写所有要求项名称')
    return
  }
  savingTags.value = true
  try {
    const payload = requirements.value.map(r => toApiTag(r))
    await updateTags(jobId.value, payload)
    originalRequirementsJson.value = JSON.stringify(requirements.value)
    toast.success('任职要求已保存')
  } catch {
    toast.error('保存失败')
  } finally {
    savingTags.value = false
  }
}

function handleResetTags() {
  if (originalRequirementsJson.value) {
    requirements.value = JSON.parse(originalRequirementsJson.value)
    toast.info('已恢复上次保存的内容')
  }
}

function goBack() {
  if (jobId.value) {
    router.push(`/planning/jobs/${jobId.value}`)
    return
  }
  router.push('/planning/jobs')
}

async function loadOpsPacks() {
  if (!jobId.value) return
  activeOpsPack.value = null
  draftOpsPack.value = null
  opsPackPreview.value = ''
  try {
    const activeRes: any = await getActiveOpsPack(jobId.value)
    const active = activeRes.data
    if (active) {
      activeOpsPack.value = active
      opsPackPreview.value = JSON.stringify(active.pack, null, 2)
    }
  } catch { /* no active */ }
}

async function handleGenerateOpsPack() {
  if (!jobId.value) return
  generatingOps.value = true
  try {
    const res: any = await generateOpsPack(jobId.value)
    draftOpsPack.value = res.data
    opsPackPreview.value = JSON.stringify(res.data?.pack, null, 2)
    toast.success('运营包草案已生成')
  } catch (e: any) {
    toast.error(e?.message || '生成失败，请先保存任职要求')
  } finally {
    generatingOps.value = false
  }
}

async function handleConfirmOpsPack() {
  if (!jobId.value || !draftOpsPack.value?.id) return
  confirmingOps.value = true
  try {
    const res: any = await confirmOpsPack(jobId.value, draftOpsPack.value.id)
    activeOpsPack.value = res.data
    draftOpsPack.value = null
    opsPackPreview.value = JSON.stringify(res.data?.pack, null, 2)
    toast.success('运营包已确认生效')
  } catch {
    toast.error('确认失败')
  } finally {
    confirmingOps.value = false
  }
}

function goSourcingMethod() {
  if (!jobId.value) return
  router.push({ path: `/planning/jobs/${jobId.value}`, query: { tab: 'rules', sub: 'method' } })
}

function goSourcing() {
  if (!jobId.value || !activeOpsPack.value?.id) return
  router.push({
    path: `/planning/jobs/${jobId.value}/sourcing`,
    query: { opsPackId: String(activeOpsPack.value.id), opsPackVersion: String(activeOpsPack.value.version) },
  })
}

// AI draft
const aiPrompt = ref('')
const aiGenerating = ref(false)
const aiResult = ref('')

async function handleAiDraft() {
  if (!aiPrompt.value.trim() || aiGenerating.value) return
  aiGenerating.value = true
  aiResult.value = ''
  try {
    const res = await request.post('/api/job/ai-draft', { description: aiPrompt.value })
    const data = res?.data || res
    if (data?.llmGenerated) {
      jobForm.jdText = data.llmGenerated
      aiResult.value = data.llmGenerated
    }
  } catch {
    // silent
  } finally {
    aiGenerating.value = false
  }
}

onMounted(() => {
  const id = route.params.id || route.query.id
  if (id) {
    jobId.value = Number(id)
    loadJobDetail()
    loadTags()
    loadOpsPacks()
  }
})
</script>

<style lang="scss" scoped>
@import '@/assets/styles/variables.scss';
.jd-editor { padding-bottom: 24px; }
.editor-layout { display: flex; gap: 24px; min-height: calc(100vh - 200px); }
.editor-left { flex: 0 0 58%; min-width: 0; }
.editor-right { flex: 0 0 42%; min-width: 0; display: flex; flex-direction: column; gap: 16px; }
.editor-panel, .tag-panel, .ops-pack-panel {
  background: $bg-card;
  border-radius: 8px;
  box-shadow: 0 1px 6px 0 rgba(0, 0, 0, 0.04);
  overflow: hidden;
}
.tag-panel { display: flex; flex-direction: column; max-height: calc(100vh - 200px); }
.panel-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 16px 24px; border-bottom: 1px solid $border-color-light;
}
.panel-title { font-size: 16px; font-weight: 600; color: $text-primary; }
.panel-body { padding: 24px; }
.form-section { margin-bottom: 20px; }
.label-hint { font-weight: 400; font-size: 12px; color: $text-secondary; }
.form-actions { display: flex; gap: 12px; padding-top: 8px; }
.req-panel-body { padding: 16px; flex: 1; overflow-y: auto; }
.req-hint { margin: 0 0 12px; font-size: 12px; color: $text-secondary; line-height: 1.5; }
.req-table { margin-bottom: 12px; }
.add-req-btn { margin-top: 8px; }
.tag-footer {
  display: flex; justify-content: flex-end; gap: 12px;
  padding: 16px; border-top: 1px solid $border-color-light;
}
.ops-pack-body { padding: 16px; display: flex; flex-wrap: wrap; gap: 8px; }
@media (max-width: 1200px) {
  .editor-layout { flex-direction: column; }
  .editor-left, .editor-right { flex: none; width: 100%; }
  .tag-panel { max-height: 500px; }
}
</style>
