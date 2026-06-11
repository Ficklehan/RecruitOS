<template>
  <PageShell title="上传简历" subtitle="支持 PDF、Word、图片格式，上传后AI自动解析">
    <template #actions>
      <RButton variant="outline" @click="$router.back()">
        <ArrowLeft class="mr-2 h-4 w-4" />
        返回
      </RButton>
    </template>

    <div class="upload-section">
      <label
        class="upload-dropzone"
        @dragover.prevent
        @drop.prevent="handleDrop"
      >
        <input
          ref="fileInputRef"
          type="file"
          multiple
          accept=".pdf,.doc,.docx,.jpg,.jpeg,.png"
          class="hidden"
          @change="handleFileInput"
        />
        <Upload class="upload-icon h-12 w-12 text-muted-foreground" />
        <div class="upload-text">将简历文件拖到此处，或<em @click.prevent="fileInputRef?.click()">点击上传</em></div>
        <div class="upload-hint">支持 PDF、Word (.doc/.docx)、图片 (.jpg/.png)，单个文件不超过 10MB</div>
      </label>
    </div>

    <div v-if="fileList.length" class="file-list-section">
      <div class="section-header">
        <span class="section-title">待上传文件 ({{ fileList.length }})</span>
        <div class="section-actions">
          <RButton @click="handleUploadAll" :disabled="uploading">
            <Upload class="mr-2 h-4 w-4" />
            {{ uploading ? '上传中...' : '开始上传' }}
          </RButton>
          <RButton variant="outline" @click="handleClearAll">清空</RButton>
        </div>
      </div>

      <div class="file-list">
        <div v-for="(file, index) in fileList" :key="index" class="file-item">
          <div class="file-icon">
            <FileText class="h-5 w-5" :style="{ color: getFileColor(file.name) }" />
          </div>
          <div class="file-info">
            <div class="file-name">{{ file.name }}</div>
            <div class="file-size">{{ formatSize(file.size) }}</div>
          </div>
          <div class="file-status">
            <RBadge v-if="file.status === 'ready'" variant="secondary">待上传</RBadge>
            <RBadge v-else-if="file.status === 'uploading'" variant="outline">上传中</RBadge>
            <RBadge v-else-if="file.status === 'success'" variant="default">已上传</RBadge>
            <RBadge v-else-if="file.status === 'error'" variant="destructive">失败</RBadge>
          </div>
          <RButton variant="ghost" size="icon" @click="handleRemove(index)">
            <Trash2 class="h-4 w-4 text-destructive" />
          </RButton>
        </div>
      </div>
    </div>

    <div v-if="uploadResults.length" class="results-section">
      <div class="section-header">
        <span class="section-title">上传结果</span>
        <RButton @click="goToList">
          查看简历列表
          <ArrowRight class="ml-2 h-4 w-4" />
        </RButton>
      </div>

      <div class="results-grid">
        <div v-for="result in uploadResults" :key="result.id" class="result-card">
          <div class="result-header">
            <div class="result-avatar">{{ (result.name || '?').charAt(0) }}</div>
            <div class="result-info">
              <div class="result-name">{{ result.name || '待解析' }}</div>
              <div class="result-source">{{ result.fileName }}</div>
            </div>
            <RBadge :variant="result.parseStatus === 'PARSED' ? 'default' : 'outline'">
              {{ result.parseStatus === 'PARSED' ? '已解析' : '待解析' }}
            </RBadge>
          </div>
          <div v-if="result.parseStatus === 'PARSED'" class="result-detail">
            <span>{{ result.company || '-' }}</span>
            <span>{{ result.position || '-' }}</span>
            <span>{{ result.workYears ? result.workYears + '年' : '-' }}</span>
          </div>
          <div class="result-actions">
            <RButton variant="link" size="sm" @click="goDetail(result)">查看详情</RButton>
            <RButton variant="link" size="sm" class="text-green-600" @click="handleImport(result)">导入人才库</RButton>
          </div>
        </div>
      </div>
    </div>
</PageShell>
</template>

<script setup lang="ts">
import PageShell from '@/components/Layout/PageShell.vue'
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { Upload, FileText, Trash2, ArrowLeft, ArrowRight } from 'lucide-vue-next'
import { toast } from '@/lib/notify'
import { RButton, RBadge } from '@/components/ui'
import { uploadResume, importToTalentPool } from '@/api/modules/resume'

const router = useRouter()
const fileInputRef = ref<HTMLInputElement | null>(null)
const fileList = ref<any[]>([])
const uploading = ref(false)
const uploadResults = ref<any[]>([])

function addFiles(files: FileList | File[]) {
  for (const raw of Array.from(files)) {
    fileList.value.push({ name: raw.name, size: raw.size, raw, status: 'ready' })
  }
}

function handleFileInput(e: Event) {
  const input = e.target as HTMLInputElement
  if (input.files) addFiles(input.files)
  input.value = ''
}

function handleDrop(e: DragEvent) {
  if (e.dataTransfer?.files) addFiles(e.dataTransfer.files)
}

function handleRemove(index: number) {
  fileList.value.splice(index, 1)
}

function handleClearAll() {
  fileList.value = []
}

async function handleUploadAll() {
  uploading.value = true
  const results = []

  for (const file of fileList.value) {
    if (file.status === 'success') continue
    file.status = 'uploading'
    try {
      const res: any = await uploadResume(file.raw)
      const data = res.data || res
      file.status = 'success'
      results.push({ ...data, fileName: file.name })
    } catch {
      file.status = 'error'
    }
  }

  uploadResults.value = [...uploadResults.value, ...results]
  uploading.value = false

  const successCount = results.length
  const failCount = fileList.value.filter((f) => f.status === 'error').length
  if (successCount > 0) toast.success(`成功上传 ${successCount} 份简历`)
  if (failCount > 0) toast.error(`${failCount} 份简历上传失败`)
}

function formatSize(bytes: number) {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

function getFileColor(name: string) {
  if (name.endsWith('.pdf')) return '#DC2626'
  if (name.endsWith('.doc') || name.endsWith('.docx')) return '$primary-color'
  return '$warning-color'
}

function goDetail(result: any) {
  router.push(`/talent/resumes/${result.id}`)
}

async function handleImport(result: any) {
  try {
    await importToTalentPool(result.id)
    toast.success('已导入人才库')
  } catch {
    toast.error('导入失败')
  }
}

function goToList() {
  router.push('/talent/resume/list')
}
</script>

<style lang="scss" scoped>
@import '@/assets/styles/variables.scss';

.upload-section {
  background: $bg-card;
  border-radius: $border-radius;
  padding: 32px;
  margin-bottom: $spacing-lg;
}

.upload-dropzone {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  border: 2px dashed $border-color;
  border-radius: $border-radius;
  padding: 48px 24px;
  cursor: pointer;
  transition: border-color $transition-fast;

  &:hover {
    border-color: $primary-color;
  }
}

.hidden { display: none; }

.upload-icon { margin-bottom: 12px; }

.upload-text {
  font-size: 14px;
  color: $text-regular;
  margin-bottom: 8px;

  em {
    color: $primary-color;
    font-style: normal;
    cursor: pointer;
  }
}

.upload-hint {
  font-size: 13px;
  color: $text-placeholder;
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

  .section-actions {
    display: flex;
    gap: 8px;
  }
}

.file-list-section {
  background: $bg-card;
  border-radius: $border-radius;
  padding: 24px;
  margin-bottom: $spacing-lg;
}

.file-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.file-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border-radius: $border-radius-sm;
  background: $bg-muted;

  .file-info {
    flex: 1;
    min-width: 0;

    .file-name {
      font-size: 14px;
      color: $text-primary;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }

    .file-size {
      font-size: 12px;
      color: $text-placeholder;
      margin-top: 2px;
    }
  }
}

.results-section {
  background: $bg-card;
  border-radius: $border-radius;
  padding: 24px;
}

.results-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 16px;
}

.result-card {
  border: 2px dashed $border-color-light;
  border-radius: $border-radius;
  padding: 16px;

  .result-header {
    display: flex;
    align-items: center;
    gap: 10px;
    margin-bottom: 10px;
  }

  .result-avatar {
    width: 36px;
    height: 36px;
    border-radius: 8px;
    background: linear-gradient(135deg, $primary-color, $primary-dark);
    color: var(--r-bg-card);
    font-size: 14px;
    font-weight: 600;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
  }

  .result-info {
    flex: 1;
    min-width: 0;

    .result-name {
      font-size: 14px;
      font-weight: 600;
      color: $text-primary;
    }

    .result-source {
      font-size: 12px;
      color: $text-placeholder;
      margin-top: 2px;
    }
  }

  .result-detail {
    display: flex;
    gap: 12px;
    font-size: 13px;
    color: $text-secondary;
    margin-bottom: 10px;
  }

  .result-actions {
    display: flex;
    gap: 8px;
  }
}
</style>
