<template>
  <div class="page-container page-stack">
    <div class="page-header">
      <div>
        <h2 class="page-title">上传简历</h2>
        <p class="page-subtitle">支持 PDF、Word、图片格式，上传后AI自动解析</p>
      </div>
      <el-button @click="$router.back()">
        <el-icon><ArrowLeft /></el-icon>
        返回
      </el-button>
    </div>

    <!-- 上传区域 -->
    <div class="upload-section">
      <el-upload
        ref="uploadRef"
        class="resume-upload"
        drag
        multiple
        :auto-upload="false"
        :on-change="handleFileChange"
        :on-remove="handleFileRemove"
        accept=".pdf,.doc,.docx,.jpg,.jpeg,.png"
      >
        <el-icon class="upload-icon"><UploadFilled /></el-icon>
        <div class="upload-text">将简历文件拖到此处，或<em>点击上传</em></div>
        <div class="upload-hint">支持 PDF、Word (.doc/.docx)、图片 (.jpg/.png)，单个文件不超过 10MB</div>
      </el-upload>
    </div>

    <!-- 文件列表 -->
    <div v-if="fileList.length" class="file-list-section">
      <div class="section-header">
        <span class="section-title">待上传文件 ({{ fileList.length }})</span>
        <div class="section-actions">
          <el-button type="primary" @click="handleUploadAll" :loading="uploading">
            <el-icon><Upload /></el-icon>
            {{ uploading ? '上传中...' : '开始上传' }}
          </el-button>
          <el-button @click="handleClearAll">清空</el-button>
        </div>
      </div>

      <div class="file-list">
        <div v-for="(file, index) in fileList" :key="index" class="file-item">
          <div class="file-icon">
            <el-icon :size="20" :color="getFileColor(file.name)">
              <Document />
            </el-icon>
          </div>
          <div class="file-info">
            <div class="file-name">{{ file.name }}</div>
            <div class="file-size">{{ formatSize(file.size) }}</div>
          </div>
          <div class="file-status">
            <el-tag v-if="file.status === 'ready'" type="info" size="small">待上传</el-tag>
            <el-tag v-else-if="file.status === 'uploading'" type="warning" size="small">上传中</el-tag>
            <el-tag v-else-if="file.status === 'success'" type="success" size="small">已上传</el-tag>
            <el-tag v-else-if="file.status === 'error'" type="danger" size="small">失败</el-tag>
          </div>
          <el-button type="danger" link size="small" @click="handleRemove(index)">
            <el-icon><Delete /></el-icon>
          </el-button>
        </div>
      </div>
    </div>

    <!-- 上传结果 -->
    <div v-if="uploadResults.length" class="results-section">
      <div class="section-header">
        <span class="section-title">上传结果</span>
        <el-button type="primary" @click="goToList">
          查看简历列表
          <el-icon><ArrowRight /></el-icon>
        </el-button>
      </div>

      <div class="results-grid">
        <div v-for="result in uploadResults" :key="result.id" class="result-card">
          <div class="result-header">
            <div class="result-avatar">{{ (result.name || '?').charAt(0) }}</div>
            <div class="result-info">
              <div class="result-name">{{ result.name || '待解析' }}</div>
              <div class="result-source">{{ result.fileName }}</div>
            </div>
            <el-tag :type="result.parseStatus === 'PARSED' ? 'success' : 'warning'" size="small">
              {{ result.parseStatus === 'PARSED' ? '已解析' : '待解析' }}
            </el-tag>
          </div>
          <div v-if="result.parseStatus === 'PARSED'" class="result-detail">
            <span>{{ result.company || '-' }}</span>
            <span>{{ result.position || '-' }}</span>
            <span>{{ result.workYears ? result.workYears + '年' : '-' }}</span>
          </div>
          <div class="result-actions">
            <el-button type="primary" link size="small" @click="goDetail(result)">查看详情</el-button>
            <el-button type="success" link size="small" @click="handleImport(result)">导入人才库</el-button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { UploadFilled, Upload, Document, Delete, ArrowLeft, ArrowRight } from '@element-plus/icons-vue'
import { uploadResume, importToTalentPool } from '@/api/modules/resume'

const router = useRouter()

const fileList = ref<any[]>([])
const uploading = ref(false)
const uploadResults = ref<any[]>([])

function handleFileChange(file: any) {
  file.status = 'ready'
  fileList.value.push(file)
}

function handleFileRemove(file: any) {
  fileList.value = fileList.value.filter(f => f !== file)
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
      results.push({
        ...data,
        fileName: file.name,
      })
    } catch {
      file.status = 'error'
    }
  }

  uploadResults.value = [...uploadResults.value, ...results]
  uploading.value = false

  const successCount = results.length
  const failCount = fileList.value.filter(f => f.status === 'error').length
  if (successCount > 0) ElMessage.success(`成功上传 ${successCount} 份简历`)
  if (failCount > 0) ElMessage.warning(`${failCount} 份简历上传失败`)
}

function formatSize(bytes: number) {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

function getFileColor(name: string) {
  if (name.endsWith('.pdf')) return '#DC2626'
  if (name.endsWith('.doc') || name.endsWith('.docx')) return '#3B82F6'
  return '#D97706'
}

function goDetail(result: any) {
  router.push(`/talent/resumes/${result.id}`)
}

async function handleImport(result: any) {
  try {
    await importToTalentPool(result.id)
    ElMessage.success('已导入人才库')
  } catch {
    ElMessage.error('导入失败')
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

  .resume-upload {
    width: 100%;

    :deep(.el-upload-dragger) {
      border: 2px dashed $border-color;
      border-radius: $border-radius;
      padding: 48px 24px;
      transition: border-color $transition-fast;

      &:hover {
        border-color: $primary-color;
      }
    }
  }

  .upload-icon {
    font-size: 48px;
    color: $text-placeholder;
    margin-bottom: 12px;
  }

  .upload-text {
    font-size: 15px;
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

  .file-icon {
    flex-shrink: 0;
  }

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

  .file-status {
    flex-shrink: 0;
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
  border: 1px solid $bg-muted;
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
    color: #fff;
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
