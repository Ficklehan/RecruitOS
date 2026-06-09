<template>
  <div class="page-container page-stack">
    <div class="page-header">
      <h2 class="page-title">沟通 Profile（六模块）</h2>
      <p class="page-desc">租户默认配置；岗位可覆盖。Campaign 发送招呼/复聊前会 merge 读取并走安全审查。</p>
    </div>

    <el-tabs v-model="activeTab">
      <el-tab-pane label="租户默认" name="tenant">
        <el-form v-loading="loading" label-width="120px" class="profile-form">
          <el-form-item label="人设 persona">
            <el-input v-model="form.persona" type="textarea" :rows="2" />
          </el-form-item>
          <el-form-item label="公司背景">
            <el-input v-model="form.companyBackground" type="textarea" :rows="2" />
          </el-form-item>
          <el-form-item label="沟通逻辑">
            <el-input v-model="form.communicationLogic" type="textarea" :rows="2" />
          </el-form-item>
          <el-form-item label="主动触发">
            <el-select v-model="form.proactiveTriggers" multiple style="width: 100%">
              <el-option label="有聊天回复" value="HAS_CHAT_REPLY" />
              <el-option label="沉默48小时" value="SILENCE_48H" />
            </el-select>
          </el-form-item>
          <el-form-item label="护栏 guardrails">
            <el-input v-model="form.guardrails" type="textarea" :rows="2" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="saving" @click="saveTenant">保存租户默认</el-button>
          </el-form-item>
        </el-form>
      </el-tab-pane>

      <el-tab-pane label="岗位覆盖" name="job">
        <div class="job-selector mb-12">
          <span>岗位：</span>
          <el-select v-model="selectedJobId" placeholder="选择岗位" style="width: 240px" @change="loadJobProfile">
            <el-option v-for="j in jobOptions" :key="j.id" :label="j.title" :value="j.id" />
          </el-select>
        </div>
        <el-form v-if="selectedJobId" v-loading="loadingJob" label-width="120px" class="profile-form">
          <el-alert type="info" :closable="false" class="mb-12"
            title="留空字段将继承租户默认；填写则覆盖该岗位。" />
          <el-form-item label="人设">
            <el-input v-model="jobForm.persona" type="textarea" :rows="2" />
          </el-form-item>
          <el-form-item label="公司背景">
            <el-input v-model="jobForm.companyBackground" type="textarea" :rows="2" />
          </el-form-item>
          <el-form-item label="沟通逻辑">
            <el-input v-model="jobForm.communicationLogic" type="textarea" :rows="2" />
          </el-form-item>
          <el-form-item label="护栏">
            <el-input v-model="jobForm.guardrails" type="textarea" :rows="2" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="savingJob" @click="saveJob">保存岗位覆盖</el-button>
          </el-form-item>
        </el-form>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
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
    ElMessage.success('已保存租户默认 Profile')
  } finally {
    saving.value = false
  }
}

async function loadJobs() {
  const res: any = await getJobList({ pageSize: 100, status: 'ACTIVE' })
  jobOptions.value = res.data?.records || res.data || []
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
    ElMessage.success('已保存岗位覆盖')
  } finally {
    savingJob.value = false
  }
}

onMounted(async () => {
  await loadTenant()
  await loadJobs()
})
</script>

<style scoped>
.page-desc { color: #64748b; font-size: 13px; margin-top: 4px; }
.profile-form { max-width: 720px; margin-top: 16px; }
.mb-12 { margin-bottom: 12px; }
.job-selector { display: flex; align-items: center; gap: 8px; }
</style>
