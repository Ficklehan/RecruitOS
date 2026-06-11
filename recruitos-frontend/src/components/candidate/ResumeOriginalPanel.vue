<template>
  <div class="resume-original-panel">
    <div class="toolbar">
      <div class="file-meta">
        <FileText class="h-4 w-4" />
        <span>{{ resume.fileName || '原始简历' }}</span>
        <Badge variant="secondary">{{ (resume.fileType || 'file').toUpperCase() }}</Badge>
      </div>
      <div class="toolbar-actions">
        <Button v-if="resume.fileUrl" size="sm" variant="outline" @click="openInNewTab">
          <Eye class="mr-2 h-4 w-4" />
          新窗口打开
        </Button>
        <Button v-if="resume.fileUrl" size="sm" variant="outline" @click="downloadFile">
          <Download class="mr-2 h-4 w-4" />
          下载
        </Button>
      </div>
    </div>

    <div v-if="showPdfFrame" class="pdf-frame-wrap">
      <iframe :src="pdfSrc" class="pdf-frame" title="简历 PDF 预览" @error="pdfLoadFailed = true" />
    </div>

    <Alert v-else-if="resume.fileUrl && pdfLoadFailed" variant="warning" class="mb-12">
      <AlertTitle>PDF 文件加载失败，请查看下方简历原文</AlertTitle>
    </Alert>

    <Alert v-else-if="resume.fileUrl && !showPdfFrame" variant="default" class="mb-12">
      <AlertTitle>当前环境暂不支持在线 PDF 预览，可查看下方简历原文或下载文件</AlertTitle>
    </Alert>

    <section v-if="resume.rawText" class="raw-section">
      <h4 class="section-title">简历原文</h4>
      <pre class="raw-text">{{ resume.rawText }}</pre>
    </section>

    <EmptyStateCta
      v-if="!resume.fileUrl && !resume.rawText"
      title="暂无原始简历"
      description="该候选人尚未关联可查看的简历文件"
      :image-size="64"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { FileText, Eye, Download } from 'lucide-vue-next'
import EmptyStateCta from '@/components/common/EmptyStateCta.vue'
import { Alert, AlertTitle, Badge, Button } from '@/components/ui'
import type { NormalizedResume } from '@/utils/resumeParser'
import { isPdfResume, resumeFileUrl } from '@/utils/resumeParser'

const props = defineProps<{
  resume: NormalizedResume
}>()

const pdfLoadFailed = ref(false)
const pdfSrc = computed(() => resumeFileUrl(props.resume.fileUrl))
const showPdfFrame = computed(() => isPdfResume(props.resume) && !!pdfSrc.value)

function openInNewTab() {
  if (!pdfSrc.value) return
  window.open(pdfSrc.value, '_blank', 'noopener')
}

function downloadFile() {
  if (!pdfSrc.value) return
  const a = document.createElement('a')
  a.href = pdfSrc.value
  a.download = props.resume.fileName || 'resume.pdf'
  a.target = '_blank'
  a.click()
}
</script>

<style scoped lang="scss">
.resume-original-panel {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  padding: 12px 16px;
  background: #f8fafc;
  border-radius: 12px;
}

.file-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: #334155;
}

.toolbar-actions {
  display: flex;
  gap: 8px;
}

.pdf-frame-wrap {
  border-radius: 12px;
  overflow: hidden;
  background: #fff;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

.pdf-frame {
  width: 100%;
  height: min(72vh, 720px);
  border: none;
}

.raw-section {
  background: #fff;
  border-radius: 12px;
  padding: 16px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

.section-title {
  margin: 0 0 12px;
  font-size: 15px;
  font-weight: 600;
}

.raw-text {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
  font-family: inherit;
  font-size: 14px;
  line-height: 1.7;
  color: #475569;
  background: #f8fafc;
  padding: 14px;
  border-radius: 8px;
}

.mb-12 {
  margin-bottom: 0;
}
</style>
