<template>
  <div class="page-container jd-editor">
    <div class="page-header">
      <h2 class="page-title">任职要求</h2>
      <div class="header-actions">
        <el-button @click="goBack">
          <el-icon><ArrowLeft /></el-icon>
          返回在招职位
        </el-button>
      </div>
    </div>

    <div class="editor-layout">
      <div class="editor-left">
        <div class="editor-panel">
          <div class="panel-header">
            <h3 class="panel-title">{{ OBJECTS.jobDescription }}</h3>
            <el-tag v-if="jobForm.status" :type="getStatusType(jobForm.status)" size="small">
              {{ jobStatusLabel(jobForm.status) }}
            </el-tag>
          </div>

          <div class="panel-body">
            <div class="form-section">
              <label class="form-label">职位名称</label>
              <el-input
                v-model="jobForm.title"
                placeholder="如：高级前端工程师"
                size="large"
              />
            </div>

            <div class="form-section">
              <label class="form-label">关联招聘需求编号</label>
              <el-input v-model="jobForm.demandNo" placeholder="选填" />
            </div>

            <div class="form-row">
              <div class="form-section flex-1">
                <label class="form-label">招聘人数</label>
                <el-input-number v-model="jobForm.headcount" :min="1" :max="999" style="width: 100%" />
              </div>
            </div>

            <div class="form-section">
              <label class="form-label">
                {{ OBJECTS.jobDescription }}全文
                <span class="label-hint">粘贴完整 JD，系统将自动提取技能要求</span>
              </label>
              <el-input
                v-model="jobForm.jdText"
                type="textarea"
                :rows="20"
                placeholder="请粘贴或输入职位描述与任职要求…"
                class="jd-textarea"
              />
            </div>

            <div class="form-actions">
              <el-button type="primary" size="large" @click="handleSave" :loading="saving">
                <el-icon><Check /></el-icon>
                保存职位
              </el-button>
              <el-button size="large" @click="handleParseJd" :loading="parsing">
                <el-icon><MagicStick /></el-icon>
                提取任职要求
              </el-button>
            </div>
          </div>
        </div>
      </div>

      <div class="editor-right">
        <div class="tag-panel">
          <div class="panel-header">
            <h3 class="panel-title">{{ OBJECTS.jobRequirements }}</h3>
            <el-button v-if="requirements.length" type="primary" link size="small" @click="handleSaveTags" :loading="savingTags">
              保存
            </el-button>
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

            <el-table :data="requirements" size="small" class="req-table">
              <el-table-column prop="name" label="要求项" min-width="120">
                <template #default="{ row }">
                  <el-input v-if="row._editing" v-model="row.name" size="small" />
                  <span v-else>{{ row.name }}</span>
                </template>
              </el-table-column>
              <el-table-column label="类型" width="100">
                <template #default="{ row }">
                  <el-select
                    v-model="row.requirementType"
                    size="small"
                    :disabled="row.locked"
                    @change="onRequirementChange(row)"
                  >
                    <el-option label="必备" value="REQUIRED" />
                    <el-option label="加分" value="PREFERRED" />
                  </el-select>
                </template>
              </el-table-column>
              <el-table-column label="重要程度" width="100">
                <template #default="{ row }">
                  <el-select
                    v-model="row.importance"
                    size="small"
                    :disabled="row.locked"
                    @change="onRequirementChange(row)"
                  >
                    <el-option label="高" value="HIGH" />
                    <el-option label="中" value="MEDIUM" />
                    <el-option label="低" value="LOW" />
                  </el-select>
                </template>
              </el-table-column>
              <el-table-column label="锁定" width="70" align="center">
                <template #default="{ row }">
                  <el-switch v-model="row.locked" size="small" />
                </template>
              </el-table-column>
              <el-table-column label="" width="56" align="center">
                <template #default="{ $index }">
                  <el-button link type="danger" size="small" @click="removeRequirement($index)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>

            <el-button class="add-req-btn" @click="addRequirement">
              <el-icon><Plus /></el-icon>
              手动添加要求
            </el-button>
          </div>

          <div v-if="requirements.length > 0" class="tag-footer">
            <el-button @click="handleResetTags" :disabled="savingTags">
              <el-icon><RefreshLeft /></el-icon>
              恢复
            </el-button>
            <el-button type="primary" @click="handleSaveTags" :loading="savingTags">
              <el-icon><Check /></el-icon>
              保存任职要求
            </el-button>
          </div>
        </div>

        <div class="ops-pack-panel">
          <div class="panel-header">
            <h3 class="panel-title">{{ OBJECTS.sourcingMethod }}</h3>
          </div>
          <div class="ops-pack-body">
            <p class="req-hint">招人方式已迁移至在招职位工作台。请在工作台「规则 → 招人方式」中设置。</p>
            <el-button v-if="jobId" type="primary" @click="goSourcingMethod">
              前往设置招人方式
            </el-button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, Check, MagicStick, RefreshLeft, Plus } from '@element-plus/icons-vue'
import EmptyStateCta from '@/components/common/EmptyStateCta.vue'
import { OBJECTS, jobStatusLabel } from '@/constants/businessLabels'
import {
  type RequirementItem,
  fromApiTag,
  toApiTag,
  applyRequirementChange,
} from '@/utils/jdRequirements'
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
    name: '',
    requirementType: 'REQUIRED',
    importance: 'MEDIUM',
    locked: false,
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
    ElMessage.warning('请输入职位名称')
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
      ElMessage.success('职位已更新')
    } else {
      const res: any = await createJob(data)
      const newId = res.data?.id || res?.id
      if (newId) {
        jobId.value = newId
        router.replace({ query: { id: String(newId) } })
      }
      ElMessage.success('职位已创建')
    }
  } catch {
    ElMessage.error('保存失败，请重试')
  } finally {
    saving.value = false
  }
}

async function handleParseJd() {
  if (!jobForm.jdText.trim()) {
    ElMessage.warning('请先输入职位描述')
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
    ElMessage.success('任职要求已提取')
    await loadTags()
  } finally {
    parsing.value = false
  }
}

async function handleSaveTags() {
  if (!jobId.value) return
  const invalid = requirements.value.find(r => !r.name.trim())
  if (invalid) {
    ElMessage.warning('请填写所有要求项名称')
    return
  }
  savingTags.value = true
  try {
    const payload = requirements.value.map(r => toApiTag(r))
    await updateTags(jobId.value, payload)
    originalRequirementsJson.value = JSON.stringify(requirements.value)
    ElMessage.success('任职要求已保存')
  } catch {
    ElMessage.error('保存失败')
  } finally {
    savingTags.value = false
  }
}

function handleResetTags() {
  if (originalRequirementsJson.value) {
    requirements.value = JSON.parse(originalRequirementsJson.value)
    ElMessage.info('已恢复上次保存的内容')
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
    ElMessage.success('运营包草案已生成')
  } catch (e: any) {
    ElMessage.error(e?.message || '生成失败，请先保存任职要求')
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
    ElMessage.success('运营包已确认生效')
  } catch {
    ElMessage.error('确认失败')
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
.form-label {
  display: block; font-size: 14px; font-weight: 500; color: $text-primary; margin-bottom: 8px;
  .label-hint { font-weight: 400; font-size: 12px; color: $text-secondary; margin-left: 8px; }
}
.form-row { display: flex; gap: 16px; }
.flex-1 { flex: 1; }
.jd-textarea :deep(.el-textarea__inner) {
  font-size: 13px; line-height: 1.8; padding: 16px; resize: vertical;
}
.form-actions { display: flex; gap: 12px; padding-top: 8px; }
.req-panel-body { padding: 16px; flex: 1; overflow-y: auto; }
.req-hint { margin: 0 0 12px; font-size: 12px; color: $text-secondary; line-height: 1.5; }
.req-table { margin-bottom: 12px; }
.add-req-btn { width: 100%; }
.tag-footer {
  display: flex; justify-content: flex-end; gap: 12px;
  padding: 16px; border-top: 1px solid $border-color-light;
}
.ops-pack-body { padding: 16px; display: flex; flex-wrap: wrap; gap: 8px; }
.ops-preview { width: 100%; margin-top: 8px; }
.ops-json {
  font-size: 11px; line-height: 1.5; max-height: 240px; overflow: auto;
  background: #f8fafc; padding: 8px; border-radius: 6px;
}
@media (max-width: 1200px) {
  .editor-layout { flex-direction: column; }
  .editor-left, .editor-right { flex: none; width: 100%; }
  .tag-panel { max-height: 500px; }
}
</style>
