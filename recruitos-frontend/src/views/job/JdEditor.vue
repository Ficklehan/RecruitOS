<template>
  <div class="page-container jd-editor">
    <!-- 页面头部 -->
    <div class="page-header">
      <h2 class="page-title">JD 工作台</h2>
      <div class="header-actions">
        <el-button @click="goBack">
          <el-icon><ArrowLeft /></el-icon>
          返回列表
        </el-button>
      </div>
    </div>

    <!-- 主体区域：左右分栏 -->
    <div class="editor-layout">
      <!-- 左侧：JD 编辑区 -->
      <div class="editor-left">
        <div class="editor-panel">
          <div class="panel-header">
            <h3 class="panel-title">职位描述</h3>
            <el-tag v-if="jobForm.status" :type="getStatusType(jobForm.status)" size="small">
              {{ getStatusLabel(jobForm.status) }}
            </el-tag>
          </div>

          <div class="panel-body">
            <div class="form-section">
              <label class="form-label">岗位标题</label>
              <el-input
                v-model="jobForm.title"
                placeholder="请输入岗位标题，如：高级前端工程师"
                size="large"
              />
            </div>

            <div class="form-section">
              <label class="form-label">关联需求编号</label>
              <el-input
                v-model="jobForm.demandNo"
                placeholder="请输入关联的招聘需求编号"
              />
            </div>

            <div class="form-row">
              <div class="form-section flex-1">
                <label class="form-label">招聘人数</label>
                <el-input-number
                  v-model="jobForm.headcount"
                  :min="1"
                  :max="999"
                  style="width: 100%"
                />
              </div>
            </div>

            <div class="form-section">
              <label class="form-label">
                职位描述 (JD)
                <span class="label-hint">支持粘贴完整的职位描述文本</span>
              </label>
              <el-input
                v-model="jobForm.jdText"
                type="textarea"
                :rows="20"
                placeholder="请在此输入或粘贴职位描述内容...

示例：
岗位职责：
1. 负责公司核心产品的前端开发工作
2. 参与产品需求评审，给出技术方案建议
3. 负责前端工程化建设，提升开发效率

任职要求：
1. 本科及以上学历，计算机相关专业
2. 3年以上前端开发经验
3. 精通 Vue/React 等主流框架
4. 良好的沟通能力和团队协作精神"
                class="jd-textarea"
              />
            </div>

            <div class="form-actions">
              <el-button type="primary" size="large" @click="handleSave" :loading="saving">
                <el-icon><Check /></el-icon>
                保存岗位
              </el-button>
              <el-button size="large" @click="handleParseJd" :loading="parsing">
                <el-icon><MagicStick /></el-icon>
                解析 JD
              </el-button>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧：标签权重面板 -->
      <div class="editor-right">
        <div class="tag-panel">
          <div class="panel-header">
            <h3 class="panel-title">标签权重</h3>
            <el-button
              v-if="tags.length > 0"
              type="primary" link size="small"
              @click="handleSaveTags"
              :loading="savingTags"
            >
              保存标签
            </el-button>
          </div>

          <!-- 空状态 -->
          <div v-if="tags.length === 0" class="tag-empty">
            <div class="empty-icon">
              <el-icon :size="48"><PriceTag /></el-icon>
            </div>
            <p class="empty-title">暂无解析标签</p>
            <p class="empty-desc">请在左侧输入 JD 内容，然后点击"解析 JD"按钮</p>
          </div>

          <!-- 标签列表 -->
          <div v-else class="tag-list">
            <div
              v-for="(tag, index) in tags"
              :key="index"
              class="tag-card"
              :class="{ 'tag-locked': tag.locked }"
            >
              <div class="tag-header">
                <div class="tag-name-row">
                  <span class="tag-name">{{ tag.name }}</span>
                  <el-tag v-if="tag.category" size="small" type="info" class="tag-category">
                    {{ tag.category }}
                  </el-tag>
                </div>
                <el-tooltip :content="tag.locked ? '已锁定' : '未锁定'" placement="top">
                  <el-switch
                    v-model="tag.locked"
                    active-text="锁定"
                    inactive-text=""
                    size="small"
                  />
                </el-tooltip>
              </div>

              <div class="tag-weights">
                <div class="weight-item">
                  <div class="weight-label">
                    <span class="weight-name">匹配权重</span>
                    <span class="weight-value">{{ (tag.matchWeight * 100).toFixed(0) }}%</span>
                  </div>
                  <el-slider
                    v-model="tag.matchWeight"
                    :max="1"
                    :step="0.05"
                    :disabled="tag.locked"
                    :show-tooltip="false"
                    class="weight-slider"
                  />
                </div>

                <div class="weight-item">
                  <div class="weight-label">
                    <span class="weight-name">搜索权重</span>
                    <span class="weight-value">{{ (tag.searchWeight * 100).toFixed(0) }}%</span>
                  </div>
                  <el-slider
                    v-model="tag.searchWeight"
                    :max="1"
                    :step="0.05"
                    :disabled="tag.locked"
                    :show-tooltip="false"
                    class="weight-slider"
                  />
                </div>

                <div class="weight-item">
                  <div class="weight-label">
                    <span class="weight-name">决策权重</span>
                    <span class="weight-value">{{ (tag.decisionWeight * 100).toFixed(0) }}%</span>
                  </div>
                  <el-slider
                    v-model="tag.decisionWeight"
                    :max="1"
                    :step="0.05"
                    :disabled="tag.locked"
                    :show-tooltip="false"
                    class="weight-slider"
                  />
                </div>
              </div>
            </div>
          </div>

          <!-- 底部操作栏 -->
          <div v-if="tags.length > 0" class="tag-footer">
            <el-button @click="handleResetTags" :disabled="savingTags">
              <el-icon><RefreshLeft /></el-icon>
              重置标签
            </el-button>
            <el-button type="primary" @click="handleSaveTags" :loading="savingTags">
              <el-icon><Check /></el-icon>
              保存标签
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
import { ArrowLeft, Check, MagicStick, PriceTag, RefreshLeft } from '@element-plus/icons-vue'
import {
  getJobDetail,
  createJob,
  updateJob,
  parseJd as parseJdApi,
  getTags,
  updateTags,
} from '@/api/modules/job'

const router = useRouter()
const route = useRoute()

const jobId = ref<number | null>(null)
const saving = ref(false)
const parsing = ref(false)
const savingTags = ref(false)

// 岗位表单
const jobForm = reactive({
  title: '',
  demandNo: '',
  headcount: 1,
  jdText: '',
  status: '',
})

// 标签数据
interface TagItem {
  name: string
  category: string
  matchWeight: number
  searchWeight: number
  decisionWeight: number
  locked: boolean
}

const tags = ref<TagItem[]>([])
const originalTagsJson = ref('')

// 状态映射
function getStatusType(status: string) {
  const map: Record<string, string> = {
    DRAFT: 'info',
    ACTIVE: 'success',
    PAUSED: 'warning',
    CLOSED: 'info',
  }
  return map[status] || 'info'
}

function getStatusLabel(status: string) {
  const map: Record<string, string> = {
    DRAFT: '草稿',
    ACTIVE: '招聘中',
    PAUSED: '已暂停',
    CLOSED: '已关闭',
  }
  return map[status] || status
}

// 加载岗位详情
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

// 加载标签
async function loadTags() {
  if (!jobId.value) return
  const res: any = await getTags(jobId.value)
  tags.value = res.data || res || []
  originalTagsJson.value = JSON.stringify(tags.value)
}

// 保存岗位
async function handleSave() {
  if (!jobForm.title.trim()) {
    ElMessage.warning('请输入岗位标题')
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
      ElMessage.success('岗位已更新')
    } else {
      const res: any = await createJob(data)
      const newId = res.data?.id || res?.id
      if (newId) {
        jobId.value = newId
        router.replace({ query: { id: String(newId) } })
      }
      ElMessage.success('岗位已创建')
    }
  } catch {
    ElMessage.error('保存失败，请重试')
  } finally {
    saving.value = false
  }
}

// 解析 JD
async function handleParseJd() {
  if (!jobForm.jdText.trim()) {
    ElMessage.warning('请先输入 JD 内容')
    return
  }
  // 如果是新建岗位，先保存
  if (!jobId.value) {
    await handleSave()
    if (!jobId.value) return
  } else {
    // 先保存最新内容
    await handleSave()
  }

  parsing.value = true
  try {
    await parseJdApi(jobId.value!)
    ElMessage.success('JD 解析完成')
    await loadTags()
  } finally {
    parsing.value = false
  }
}

// 保存标签
async function handleSaveTags() {
  if (!jobId.value) return
  savingTags.value = true
  try {
    await updateTags(jobId.value, tags.value)
    originalTagsJson.value = JSON.stringify(tags.value)
    ElMessage.success('标签已保存')
  } catch {
    ElMessage.error('保存标签失败')
  } finally {
    savingTags.value = false
  }
}

// 重置标签
function handleResetTags() {
  if (originalTagsJson.value) {
    tags.value = JSON.parse(originalTagsJson.value)
    ElMessage.info('标签已重置')
  }
}

// 返回列表
function goBack() {
  router.push('/position/job')
}

onMounted(() => {
  const id = route.query.id
  if (id) {
    jobId.value = Number(id)
    loadJobDetail()
    loadTags()
  }
})
</script>

<style lang="scss" scoped>
@import '@/assets/styles/variables.scss';
.jd-editor {
  padding-bottom: 24px;
}

.editor-layout {
  display: flex;
  gap: 24px;
  min-height: calc(100vh - 200px);
}

// 左侧编辑区
.editor-left {
  flex: 0 0 60%;
  min-width: 0;
}

.editor-panel {
  background: $bg-card;
  border-radius: 8px;
  box-shadow: 0 1px 6px 0 rgba(0, 0, 0, 0.04);
  overflow: hidden;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 24px;
  border-bottom: 1px solid $border-color-light;
}

.panel-title {
  font-size: 16px;
  font-weight: 600;
  color: $text-primary;
}

.panel-body {
  padding: 24px;
}

.form-section {
  margin-bottom: 20px;
}

.form-label {
  display: block;
  font-size: 14px;
  font-weight: 500;
  color: $text-primary;
  margin-bottom: 8px;

  .label-hint {
    font-weight: 400;
    font-size: 12px;
    color: $text-secondary;
    margin-left: 8px;
  }
}

.form-row {
  display: flex;
  gap: 16px;
}

.flex-1 {
  flex: 1;
}

.jd-textarea {
  :deep(.el-textarea__inner) {
    font-family: 'SF Mono', 'Monaco', 'Menlo', 'Consolas', monospace;
    font-size: 13px;
    line-height: 1.8;
    padding: 16px;
    resize: vertical;
  }
}

.form-actions {
  display: flex;
  gap: 12px;
  padding-top: 8px;
}

// 右侧标签面板
.editor-right {
  flex: 0 0 40%;
  min-width: 0;
}

.tag-panel {
  background: $bg-card;
  border-radius: 8px;
  box-shadow: 0 1px 6px 0 rgba(0, 0, 0, 0.04);
  display: flex;
  flex-direction: column;
  max-height: calc(100vh - 200px);
  overflow: hidden;
}

.tag-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 40px;
  text-align: center;

  .empty-icon {
    color: $text-placeholder;
    margin-bottom: 16px;
  }

  .empty-title {
    font-size: 16px;
    font-weight: 500;
    color: $text-regular;
    margin-bottom: 8px;
  }

  .empty-desc {
    font-size: 13px;
    color: $text-secondary;
    line-height: 1.6;
  }
}

.tag-list {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
}

.tag-card {
  background: $bg-card;
  border: 1px solid $border-color-light;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 12px;
  transition: all 0.3s ease;

  &:hover {
    box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.08);
    border-color: #dcdfe6;
  }

  &.tag-locked {
    border-color: $warning-color;
    background: $warning-lighter;
  }
}

.tag-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.tag-name-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.tag-name {
  font-size: 15px;
  font-weight: 600;
  color: $text-primary;
}

.tag-category {
  font-size: 11px;
}

.tag-weights {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.weight-item {
  .weight-label {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 2px;
  }

  .weight-name {
    font-size: 12px;
    color: $text-regular;
  }

  .weight-value {
    font-size: 12px;
    font-weight: 600;
    color: $primary-color;
    min-width: 36px;
    text-align: right;
  }
}

.weight-slider {
  :deep(.el-slider__runway) {
    height: 4px;
  }

  :deep(.el-slider__bar) {
    height: 4px;
  }

  :deep(.el-slider__button-wrapper) {
    top: -16px;
  }

  :deep(.el-slider__button) {
    width: 12px;
    height: 12px;
    border: 2px solid $primary-color;
  }
}

.tag-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 16px;
  border-top: 1px solid $border-color-light;
}

// 响应式
@media (max-width: 1200px) {
  .editor-layout {
    flex-direction: column;
  }

  .editor-left,
  .editor-right {
    flex: none;
    width: 100%;
  }

  .tag-panel {
    max-height: 500px;
  }
}
</style>
