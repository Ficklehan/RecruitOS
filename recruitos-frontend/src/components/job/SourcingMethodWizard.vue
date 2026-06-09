<template>
  <div class="sourcing-wizard" v-loading="loading">
    <div class="wizard-status data-card">
      <div class="status-left">
        <span v-if="activePack" class="status-chip active">已生效 v{{ activePack.version }}</span>
        <span v-if="draftPack" class="status-chip draft">草案 v{{ draftPack.version }}</span>
        <span v-if="!activePack && !draftPack" class="status-chip empty">待设置招人方式</span>
      </div>
      <div class="status-actions">
        <el-button type="primary" :loading="generating" :disabled="!jobId" @click="handleGenerate">
          {{ draftPack || activePack ? '重新生成' : '生成招人方式' }}
        </el-button>
        <el-button v-if="draftPack" :loading="saving" @click="handleSaveDraft">保存草案</el-button>
        <el-button v-if="draftPack" type="success" :loading="confirming" @click="handleConfirmPublish">
          确认并发布
        </el-button>
      </div>
    </div>

    <EmptyStateCta
      v-if="!draftPack && !activePack"
      title="尚未设置招人方式"
      description="基于职位要求自动生成：找什么人、怎么筛、怎么联系。确认后可用于开启平台招人任务。"
      :actions="[
        { label: '生成招人方式', type: 'primary', onClick: handleGenerate },
      ]"
    />

    <template v-else>
      <el-tabs v-model="wizardTab" class="wizard-tabs">
        <el-tab-pane label="找什么人" name="keywords">
          <p class="tab-hint">系统将根据这些关键词在招聘平台上搜寻候选人。</p>
          <div class="keyword-editor">
            <el-tag
              v-for="(kw, idx) in packForm.searchKeywords"
              :key="idx"
              closable
              class="kw-tag"
              @close="removeKeyword(idx)"
            >
              {{ kw }}
            </el-tag>
            <el-input
              v-if="keywordInputVisible"
              ref="keywordInputRef"
              v-model="keywordInput"
              size="small"
              class="kw-input"
              @keyup.enter="addKeyword"
              @blur="addKeyword"
            />
            <el-button v-else size="small" @click="showKeywordInput">+ 添加关键词</el-button>
          </div>
        </el-tab-pane>

        <el-tab-pane label="怎么筛" name="screening">
          <p class="tab-hint">两阶段筛选：先看平台卡片信息，通过后再看完整简历。</p>
          <el-form label-width="100px" size="default">
            <el-form-item label="通过线">
              <el-slider v-model="passThreshold" :min="40" :max="90" show-input />
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="对外怎么说" name="comm">
          <p class="tab-hint">首次联系与后续沟通的风格（可在「对外沟通风格」设置租户默认）。</p>
          <el-form label-width="100px">
            <el-form-item label="沟通人设">
              <el-input v-model="commPersona" type="textarea" :rows="2" placeholder="如：专业、简洁、尊重候选人" />
            </el-form-item>
            <el-form-item label="公司介绍">
              <el-input v-model="commBackground" type="textarea" :rows="3" placeholder="首次联系时可用的公司背景" />
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="跟进节奏" name="rechat">
          <el-form label-width="120px">
            <el-form-item label="最多跟进次数">
              <el-input-number v-model="rechatMax" :min="0" :max="5" />
            </el-form-item>
            <el-form-item label="间隔（小时）">
              <el-input-number v-model="rechatHours" :min="12" :max="168" :step="12" />
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="触达方式" name="touch">
          <el-form label-width="120px">
            <el-form-item :label="OBJECTS.sourcingMethod">
              <el-radio-group v-model="packForm.greetStrategy">
                <el-radio label="SCREEN_THEN_GREET">{{ greetStrategyLabel('SCREEN_THEN_GREET') }}</el-radio>
                <el-radio label="COLLECT_ONLY">{{ greetStrategyLabel('COLLECT_ONLY') }}</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="Boss 日配额">
              <el-input-number v-model="packForm.platformQuotas!.BOSS" :min="1" :max="200" />
            </el-form-item>
            <el-form-item label="猎聘 日配额">
              <el-input-number v-model="packForm.platformQuotas!.LIEPIN" :min="1" :max="200" />
            </el-form-item>
          </el-form>
          <el-collapse class="advanced-collapse">
            <el-collapse-item title="高级选项（实施顾问）" name="adv">
              <el-radio-group v-model="packForm.greetStrategy">
                <el-radio label="CARD_GREET">{{ greetStrategyLabel('CARD_GREET') }}</el-radio>
              </el-radio-group>
              <pre v-if="showJson" class="json-preview">{{ jsonPreview }}</pre>
              <el-button link type="primary" @click="showJson = !showJson">{{ showJson ? '隐藏' : '查看' }} JSON</el-button>
            </el-collapse-item>
          </el-collapse>
        </el-tab-pane>
      </el-tabs>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import EmptyStateCta from '@/components/common/EmptyStateCta.vue'
import { OBJECTS, greetStrategyLabel } from '@/constants/businessLabels'
import {
  type OpsPackBody,
  mergeOpsPackBody,
  opsPackHumanSummary,
} from '@/utils/opsPackSummary'
import {
  confirmOpsPack,
  generateOpsPack,
  getActiveOpsPack,
  getOpsPackById,
  listOpsPackVersions,
  updateOpsPack,
} from '@/api/modules/job'

const props = defineProps<{ jobId: number }>()
const emit = defineEmits<{ confirmed: [version: number] }>()

const loading = ref(false)
const generating = ref(false)
const saving = ref(false)
const confirming = ref(false)
const wizardTab = ref('keywords')
const showJson = ref(false)

const activePack = ref<any>(null)
const draftPack = ref<any>(null)
const packForm = reactive<OpsPackBody>(mergeOpsPackBody(null))

const keywordInput = ref('')
const keywordInputVisible = ref(false)
const keywordInputRef = ref<any>(null)

const passThreshold = computed({
  get: () => packForm.screeningProfile?.passThreshold ?? 60,
  set: (v: number) => {
    if (!packForm.screeningProfile) packForm.screeningProfile = {}
    packForm.screeningProfile.passThreshold = v
  },
})

const commPersona = computed({
  get: () => String((packForm.communicationProfile as any)?.persona || ''),
  set: (v: string) => {
    if (!packForm.communicationProfile) packForm.communicationProfile = {}
    ;(packForm.communicationProfile as any).persona = v
  },
})

const commBackground = computed({
  get: () => String((packForm.communicationProfile as any)?.companyBackground || ''),
  set: (v: string) => {
    if (!packForm.communicationProfile) packForm.communicationProfile = {}
    ;(packForm.communicationProfile as any).companyBackground = v
  },
})

const rechatMax = computed({
  get: () => packForm.rechatPolicy?.maxAttempts ?? 2,
  set: (v: number) => {
    if (!packForm.rechatPolicy) packForm.rechatPolicy = {}
    packForm.rechatPolicy.maxAttempts = v
  },
})

const rechatHours = computed({
  get: () => packForm.rechatPolicy?.intervalHours ?? 48,
  set: (v: number) => {
    if (!packForm.rechatPolicy) packForm.rechatPolicy = {}
    packForm.rechatPolicy.intervalHours = v
  },
})

const jsonPreview = computed(() => JSON.stringify(packForm, null, 2))

function applyPackToForm(pack: unknown) {
  const merged = mergeOpsPackBody(pack)
  Object.assign(packForm, merged)
  if (!packForm.searchKeywords) packForm.searchKeywords = []
  if (!packForm.platformQuotas) packForm.platformQuotas = { BOSS: 30, LIEPIN: 30 }
}

function buildPackPayload() {
  return { ...packForm }
}

function showKeywordInput() {
  keywordInputVisible.value = true
  nextTick(() => keywordInputRef.value?.focus?.())
}

function addKeyword() {
  const v = keywordInput.value.trim()
  if (v && !packForm.searchKeywords!.includes(v)) {
    packForm.searchKeywords!.push(v)
  }
  keywordInput.value = ''
  keywordInputVisible.value = false
}

function removeKeyword(idx: number) {
  packForm.searchKeywords!.splice(idx, 1)
}

async function loadPacks() {
  loading.value = true
  try {
    activePack.value = null
    draftPack.value = null
    const activeRes: any = await getActiveOpsPack(props.jobId).catch(() => null)
    if (activeRes?.data) activePack.value = activeRes.data

    const versionsRes: any = await listOpsPackVersions(props.jobId).catch(() => ({ data: [] }))
    const versions = versionsRes.data || []
    draftPack.value = versions.find((v: any) => v.status === 'DRAFT') || null

    const source = draftPack.value || activePack.value
    if (source?.pack) applyPackToForm(source.pack)
    else if (source?.id) {
      const detail: any = await getOpsPackById(props.jobId, source.id)
      applyPackToForm(detail.data?.pack)
    }
  } finally {
    loading.value = false
  }
}

async function handleGenerate() {
  generating.value = true
  try {
    const res: any = await generateOpsPack(props.jobId)
    draftPack.value = res.data
    applyPackToForm(res.data?.pack)
    ElMessage.success('已生成招人方式草案，请检查后确认发布')
  } finally {
    generating.value = false
  }
}

async function handleSaveDraft() {
  if (!draftPack.value?.id) return
  saving.value = true
  try {
    const res: any = await updateOpsPack(props.jobId, draftPack.value.id, buildPackPayload())
    draftPack.value = res.data
    ElMessage.success('草案已保存')
  } finally {
    saving.value = false
  }
}

async function handleConfirmPublish() {
  if (!draftPack.value?.id) return
  await handleSaveDraft()
  const summary = opsPackHumanSummary(buildPackPayload()).join('\n')
  try {
    await ElMessageBox.confirm(
      `将发布新的招人方式（v${(draftPack.value.version || 0) + 1} 或确认当前草案）。\n\n${summary}\n\n只影响此后新开启的平台招人任务；正在进行的任务不变。`,
      '确认并发布招人方式',
      { confirmButtonText: '确认发布', cancelButtonText: '取消', type: 'warning' },
    )
  } catch {
    return
  }
  confirming.value = true
  try {
    const res: any = await confirmOpsPack(props.jobId, draftPack.value.id)
    activePack.value = res.data
    draftPack.value = null
    ElMessage.success('新的招人方式已生效')
    emit('confirmed', res.data?.version || 0)
    await loadPacks()
  } finally {
    confirming.value = false
  }
}

watch(() => props.jobId, loadPacks, { immediate: true })

defineExpose({ reload: loadPacks, activeVersion: computed(() => activePack.value?.version) })
</script>

<style scoped lang="scss">
.wizard-status {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 16px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}
.status-left { display: flex; gap: 8px; flex-wrap: wrap; }
.status-chip {
  font-size: 12px;
  padding: 4px 10px;
  border-radius: 20px;
  &.active { background: #dcfce7; color: #166534; }
  &.draft { background: #fef9c3; color: #854d0e; }
  &.empty { background: #f1f5f9; color: #64748b; }
}
.status-actions { display: flex; gap: 8px; flex-wrap: wrap; }
.tab-hint { color: #64748b; font-size: 13px; margin-bottom: 12px; }
.keyword-editor { display: flex; flex-wrap: wrap; gap: 8px; align-items: center; }
.kw-tag { margin: 0; }
.kw-input { width: 140px; }
.advanced-collapse { margin-top: 16px; }
.json-preview {
  background: #f8fafc;
  padding: 12px;
  border-radius: 8px;
  font-size: 11px;
  max-height: 200px;
  overflow: auto;
}
</style>
