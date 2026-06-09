<template>
  <ListPageLayout
    title="招聘进展"
    :subtitle="`按在招职位查看${OBJECTS.activeCandidates}当前进展`"
    plain
  >
    <template #toolbar>
      <JobContextBar v-model="selectedJobId" @update:model-value="onJobChange" />
    </template>

    <PipelineKanban
      v-if="selectedJobId"
      ref="kanbanRef"
      :job-id="selectedJobId"
      class="page-full-bleed"
    />
    <EmptyStateCta
      v-else
      title="还没有选定职位"
      description="请先在上方选择在招职位，再查看在招候选人"
      :actions="[
        { label: '查看在招职位', type: 'primary', onClick: () => router.push('/planning/jobs') },
      ]"
    />
  </ListPageLayout>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import ListPageLayout from '@/components/Layout/ListPageLayout.vue'
import JobContextBar from '@/components/common/JobContextBar.vue'
import EmptyStateCta from '@/components/common/EmptyStateCta.vue'
import PipelineKanban from '@/components/pipeline/PipelineKanban.vue'
import { OBJECTS } from '@/constants/businessLabels'
import { getJobList } from '@/api/modules/job'

const route = useRoute()
const router = useRouter()
const selectedJobId = ref<number | null>(null)
const kanbanRef = ref<InstanceType<typeof PipelineKanban> | null>(null)

function onJobChange() {
  kanbanRef.value?.reload()
}

onMounted(async () => {
  const qJob = Number(route.query.jobId)
  if (qJob) selectedJobId.value = qJob
  else {
    const { data } = await getJobList({ pageNum: 1, pageSize: 100, status: 'ACTIVE' })
    const jobs = data?.list || data?.records || []
    if (jobs.length) selectedJobId.value = jobs[0].id
  }
})
</script>
