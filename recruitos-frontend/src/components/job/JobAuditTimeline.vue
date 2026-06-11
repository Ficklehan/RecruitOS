<template>
  <div class="audit-timeline relative">
    <div v-if="loading" class="absolute inset-0 z-10 flex items-center justify-center bg-background/60 rounded-lg">
      <Loader2 class="h-6 w-6 animate-spin text-primary" />
    </div>

    <div v-if="events.length" class="audit-toolbar">
      <Button size="sm" variant="outline" @click="exportCsv">
        <Download class="mr-2 h-4 w-4" />
        导出 CSV
      </Button>
    </div>

    <EmptyStateCta
      v-if="!events.length && !loading"
      title="暂无变更记录"
      description="确认或更新招人方式、采纳优化建议后，记录会显示在这里。"
    />

    <div v-else class="timeline-list">
      <div v-for="ev in events" :key="ev.id" class="timeline-entry">
        <div class="timeline-marker" :class="`marker-${ev.type || 'info'}`" />
        <div class="timeline-body">
          <div class="timeline-time">{{ formatTime(ev.time) }}</div>
          <div class="event-title">{{ ev.title }}</div>
          <div v-if="ev.detail" class="event-detail">{{ ev.detail }}</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { Download, Loader2 } from 'lucide-vue-next'
import { toast } from '@/lib/notify'
import { Button } from '@/components/ui'
import EmptyStateCta from '@/components/common/EmptyStateCta.vue'
import { OBJECTS } from '@/constants/businessLabels'
import { opsPackHumanSummary, proposalTypeLabel } from '@/utils/opsPackSummary'
import { listOpsPackVersions } from '@/api/modules/job'
import { getEvolutionProposalList } from '@/api/modules/evolution'

const props = defineProps<{ jobId: number }>()

interface AuditEvent {
  id: string
  time: string
  title: string
  detail?: string
  type?: 'primary' | 'success' | 'warning' | 'danger' | 'info'
}

const loading = ref(false)
const events = ref<AuditEvent[]>([])

function formatTime(t?: string) {
  if (!t) return '—'
  return t.replace('T', ' ').slice(0, 16)
}

async function load() {
  loading.value = true
  try {
    const list: AuditEvent[] = []
    const [packRes, propRes]: any[] = await Promise.all([
      listOpsPackVersions(props.jobId).catch(() => ({ data: [] })),
      getEvolutionProposalList({ jobId: props.jobId, pageSize: 30 }).catch(() => ({ data: { list: [] } })),
    ])
    const packs = packRes.data || []
    for (const p of packs) {
      if (p.status === 'ACTIVE' && p.confirmedAt) {
        list.push({
          id: `pack-active-${p.id}`,
          time: p.confirmedAt,
          title: `${OBJECTS.sourcingMethod} v${p.version} 已确认生效`,
          detail: opsPackHumanSummary(p.pack).join(' · '),
          type: 'success',
        })
      } else if (p.status === 'DRAFT') {
        list.push({
          id: `pack-draft-${p.id}`,
          time: p.createdAt || p.confirmedAt || '',
          title: `${OBJECTS.sourcingMethod} v${p.version} 草案`,
          type: 'info',
        })
      }
    }
    const proposals = propRes.data?.list || propRes.data || []
    for (const p of proposals) {
      const st = p.status
      if (st === 'CONFIRMED' || st === 'APPLIED') {
        list.push({
          id: `prop-ok-${p.id}`,
          time: p.updatedAt || p.createdAt,
          title: `已采纳${OBJECTS.evolutionSuggestion}`,
          detail: p.title,
          type: 'primary',
        })
      } else if (st === 'REJECTED') {
        list.push({
          id: `prop-no-${p.id}`,
          time: p.updatedAt || p.createdAt,
          title: `已暂不采纳${OBJECTS.evolutionSuggestion}`,
          detail: p.rejectReason || p.title,
          type: 'warning',
        })
      } else if (st === 'PENDING') {
        list.push({
          id: `prop-pend-${p.id}`,
          time: p.createdAt,
          title: `待处理：${proposalTypeLabel(p.proposalType)}`,
          detail: p.title,
          type: 'info',
        })
      }
    }
    events.value = list.sort((a, b) => (b.time || '').localeCompare(a.time || ''))
  } finally {
    loading.value = false
  }
}

function exportCsv() {
  if (!events.value.length) {
    toast.error('暂无记录可导出')
    return
  }
  const header = '时间,事件,详情\n'
  const rows = events.value.map((ev) => {
    const time = formatTime(ev.time)
    const title = (ev.title || '').replace(/"/g, '""')
    const detail = (ev.detail || '').replace(/"/g, '""')
    return `"${time}","${title}","${detail}"`
  }).join('\n')
  const blob = new Blob(['\ufeff' + header + rows], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `job-${props.jobId}-audit-${new Date().toISOString().slice(0, 10)}.csv`
  a.click()
  URL.revokeObjectURL(url)
  toast.success('变更记录已导出')
}

watch(() => props.jobId, load, { immediate: true })
onMounted(load)

defineExpose({ reload: load })
</script>

<style scoped lang="scss">
@import '@/assets/styles/variables.scss';

.audit-toolbar { display: flex; justify-content: flex-end; margin-bottom: 12px; }

.timeline-list {
  display: flex;
  flex-direction: column;
  gap: 0;
  padding-left: 20px;
  border-left: 2px solid $border-color;
}

.timeline-entry {
  display: flex;
  gap: 16px;
  padding: 12px 0 12px 8px;
  position: relative;
}

.timeline-marker {
  position: absolute;
  left: -27px;
  top: 18px;
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: $text-secondary;
  border: 2px solid #fff;
  box-shadow: 0 0 0 2px $border-color;

  &.marker-success { background: $success-color; }
  &.marker-primary { background: $primary-color; }
  &.marker-warning { background: $warning-color; }
  &.marker-danger { background: $danger-color; }
}

.timeline-time {
  font-size: 12px;
  color: $text-secondary;
  margin-bottom: 4px;
}

.event-title { font-weight: 600; color: #0f172a; font-size: 14px; }
.event-detail { font-size: 13px; color: #64748b; margin-top: 4px; line-height: 1.5; }
</style>
