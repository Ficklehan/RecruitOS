<template>
  <PageShell title="沟通 Profile（六模块）">
<RTabs v-model="activeTab">
      <RTabsList>
        <RTabsTrigger value="tenant">租户默认</RTabsTrigger>
        <RTabsTrigger value="job">岗位覆盖</RTabsTrigger>
      </RTabsList>

      <RTabsContent value="tenant">
        <div v-if="loading" class="py-8 text-sm text-muted-foreground">加载中…</div>
        <div v-else class="profile-form grid gap-4">
          <FormField label="人设 persona">
            <RTextarea v-model="form.persona" :rows="2" />
          </FormField>
          <FormField label="公司背景">
            <RTextarea v-model="form.companyBackground" :rows="2" />
          </FormField>
          <FormField label="沟通逻辑">
            <RTextarea v-model="form.communicationLogic" :rows="2" />
          </FormField>
          <FormField label="主动触发">
            <div class="flex flex-wrap gap-4">
              <label class="flex items-center gap-2 text-sm">
                <RCheckbox
                  :model-value="form.proactiveTriggers.includes('HAS_CHAT_REPLY')"
                  @update:model-value="toggleTrigger('HAS_CHAT_REPLY', $event)"
                />
                有聊天回复
              </label>
              <label class="flex items-center gap-2 text-sm">
                <RCheckbox
                  :model-value="form.proactiveTriggers.includes('SILENCE_48H')"
                  @update:model-value="toggleTrigger('SILENCE_48H', $event)"
                />
                沉默48小时
              </label>
            </div>
          </FormField>
          <FormField label="护栏 guardrails">
            <RTextarea v-model="form.guardrails" :rows="2" />
          </FormField>
          <div>
            <RButton :disabled="saving" @click="saveTenant">
              {{ saving ? '保存中…' : '保存租户默认' }}
            </RButton>
          </div>
        </div>
      </RTabsContent>

      <RTabsContent value="job">
        <div class="job-selector mb-12">
          <span>岗位：</span>
          <RSelect
            v-model="selectedJobId"
            :options="jobSelectOptions"
            placeholder="选择岗位"
            class="w-[240px]"
            @update:model-value="loadJobProfile"
          />
        </div>
        <div v-if="loadingJob" class="py-8 text-sm text-muted-foreground">加载中…</div>
        <div v-else-if="selectedJobId" class="profile-form grid gap-4">
          <RAlert variant="default">
            <RAlertDescription>留空字段将继承租户默认；填写则覆盖该岗位。</RAlertDescription>
          </RAlert>
          <FormField label="人设">
            <RTextarea v-model="jobForm.persona" :rows="2" />
          </FormField>
          <FormField label="公司背景">
            <RTextarea v-model="jobForm.companyBackground" :rows="2" />
          </FormField>
          <FormField label="沟通逻辑">
            <RTextarea v-model="jobForm.communicationLogic" :rows="2" />
          </FormField>
          <FormField label="护栏">
            <RTextarea v-model="jobForm.guardrails" :rows="2" />
          </FormField>
          <div>
            <RButton :disabled="savingJob" @click="saveJob">
              {{ savingJob ? '保存中…' : '保存岗位覆盖' }}
            </RButton>
          </div>
        </div>
      </RTabsContent>
    </RTabs>
</PageShell>
</template>

<script setup lang="ts">
import PageShell from '@/components/Layout/PageShell.vue'
import { onMounted, reactive, ref, computed } from 'vue'
import { toast } from '@/lib/notify'
import FormField from '@/components/app/FormField.vue'
import {
  RButton, RSelect, RTextarea, RCheckbox, RTabs, RTabsList, RTabsTrigger, RTabsContent, RAlert, RAlertDescription,
} from '@/components/ui'
import {
  getCommunicationProfileTenantDefault,
  saveCommunicationProfileTenantDefault,
  getCommunicationProfileJob,
  saveCommunicationProfileJob,
} from '@/api/modules/communication'
import { getJobList } from '@/api/modules/job'

const activeTab = ref('tenant')
const loading = ref(false)
const saving = ref(false)
const loadingJob = ref(false)
const savingJob = ref(false)
const jobOptions = ref<any[]>([])
const selectedJobId = ref<number | null>(null)

const jobSelectOptions = computed(() =>
  jobOptions.value.map((j) => ({ label: j.title, value: j.id }))
)

const form = reactive({
  persona: '',
  companyBackground: '',
  communicationLogic: '',
  proactiveTriggers: [] as string[],
  guardrails: '',
})

const jobForm = reactive({
  persona: '',
  companyBackground: '',
  communicationLogic: '',
  guardrails: '',
})

function toggleTrigger(value: string, checked: boolean) {
  if (checked && !form.proactiveTriggers.includes(value)) {
    form.proactiveTriggers.push(value)
  } else if (!checked) {
    form.proactiveTriggers = form.proactiveTriggers.filter((t) => t !== value)
  }
}

async function loadTenant() {
  loading.value = true
  try {
    const res: any = await getCommunicationProfileTenantDefault()
    const d = res.data || {}
    form.persona = d.persona || ''
    form.companyBackground = d.companyBackground || ''
    form.communicationLogic = d.communicationLogic || ''
    form.proactiveTriggers = d.proactiveTriggers || []
    form.guardrails = d.guardrails || ''
  } finally {
    loading.value = false
  }
}

async function saveTenant() {
  saving.value = true
  try {
    await saveCommunicationProfileTenantDefault({ ...form })
    toast.success('已保存租户默认 Profile')
  } finally {
    saving.value = false
  }
}

async function loadJobs() {
  const res: any = await getJobList({ pageSize: 100, status: 'ACTIVE' })
  jobOptions.value = res.data?.records || res.data?.list || res.data || []
}

async function loadJobProfile() {
  if (!selectedJobId.value) return
  loadingJob.value = true
  try {
    const res: any = await getCommunicationProfileJob(selectedJobId.value)
    const d = res.data || {}
    jobForm.persona = d.persona || ''
    jobForm.companyBackground = d.companyBackground || ''
    jobForm.communicationLogic = d.communicationLogic || ''
    jobForm.guardrails = d.guardrails || ''
  } finally {
    loadingJob.value = false
  }
}

async function saveJob() {
  if (!selectedJobId.value) return
  savingJob.value = true
  try {
    await saveCommunicationProfileJob(selectedJobId.value, { ...jobForm })
    toast.success('已保存岗位覆盖')
  } finally {
    savingJob.value = false
  }
}

onMounted(async () => {
  await loadTenant()
  await loadJobs()
})
</script>

<style scoped lang="scss">
@import '@/assets/styles/variables.scss';

.page-desc {
  color: $text-secondary;
  font-size: 13px;
  margin-top: 4px;
}

.profile-form {
  max-width: 720px;
  margin-top: 16px;
}

.mb-12 {
  margin-bottom: 12px;
}

.job-selector {
  display: flex;
  align-items: center;
  gap: 8px;
}
</style>
