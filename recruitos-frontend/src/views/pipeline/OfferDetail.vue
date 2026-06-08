<template>
  <div class="page-container" v-loading="loading">
    <div class="page-header">
      <el-button text @click="$router.back()"><el-icon><ArrowLeft /></el-icon>返回</el-button>
      <h2 class="page-title">录用通知详情</h2>
      <el-tag v-if="offer">{{ statusLabel(offer.status) }}</el-tag>
    </div>

    <el-tabs v-if="offer">
      <el-tab-pane label="基本信息">
        <div class="data-card info-grid">
          <p><b>候选人：</b>{{ offer.candidateName }}</p>
          <p><b>在招职位：</b>{{ offer.jobTitle }}</p>
          <p><b>部门：</b>{{ offer.department }}</p>
          <p><b>薪资：</b>{{ offer.salary }}</p>
          <p><b>职级：</b>{{ offer.level || '-' }}</p>
          <p><b>预计入职：</b>{{ offer.onboardDate || '-' }}</p>
          <p><b>备注：</b>{{ offer.remark || '-' }}</p>
        </div>
        <div class="actions">
          <el-button v-if="offer.status === 'DRAFT'" type="primary" @click="submitApproval">提交审批</el-button>
          <el-button v-if="offer.status === 'APPROVED'" @click="sendOffer">发送录用通知</el-button>
          <el-button v-if="offer.status === 'SENT'" type="success" @click="acceptOffer">候选人接受</el-button>
        </div>
      </el-tab-pane>
      <el-tab-pane label="定薪">
        <div class="data-card">
          <el-form label-width="80px">
            <el-form-item label="月薪">
              <el-input v-model="salaryForm.salary" />
            </el-form-item>
            <el-form-item label="奖金">
              <el-input v-model="salaryForm.bonus" />
            </el-form-item>
            <el-form-item label="职级">
              <el-input v-model="salaryForm.level" />
            </el-form-item>
            <el-button type="primary" @click="saveSalary">保存定薪</el-button>
          </el-form>
        </div>
      </el-tab-pane>
      <el-tab-pane label="背调">
        <div class="data-card">
          <p>当前状态：{{ bgLabel(offer.bgCheckStatus) }}</p>
          <el-button-group style="margin-top: 12px">
            <el-button @click="setBg('PENDING')">待背调</el-button>
            <el-button type="success" @click="setBg('PASSED')">背调通过</el-button>
            <el-button type="danger" @click="setBg('FAILED')">背调未通过</el-button>
          </el-button-group>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  getOfferDetail,
  updateOffer,
  submitOfferApproval,
  sendOffer as sendOfferApi,
  acceptOffer as acceptOfferApi,
  updateBgCheckStatus,
} from '@/api/modules/offer'

const route = useRoute()
const loading = ref(false)
const offer = ref<any>(null)
const salaryForm = reactive({ salary: '', bonus: '', level: '' })

function statusLabel(s: string) {
  const map: Record<string, string> = {
    DRAFT: '草稿', PENDING: '待审批', APPROVED: '已通过', SENT: '已发送', ACCEPTED: '已接受', REJECTED: '已拒绝',
  }
  return map[s] || s
}

function bgLabel(s?: string) {
  const map: Record<string, string> = { PENDING: '待背调', PASSED: '已通过', FAILED: '未通过' }
  return map[s || ''] || '未启动'
}

async function load() {
  loading.value = true
  try {
    const { data } = await getOfferDetail(Number(route.params.id))
    offer.value = data
    salaryForm.salary = String(data.salary || '')
    salaryForm.bonus = String(data.bonus || '')
    salaryForm.level = data.level || ''
  } finally {
    loading.value = false
  }
}

async function saveSalary() {
  await updateOffer(offer.value.id, {
    salary: salaryForm.salary,
    remark: offer.value.remark,
    candidateName: offer.value.candidateName,
    jobTitle: offer.value.jobTitle,
    department: offer.value.department,
  })
  ElMessage.success('定薪已保存')
  load()
}

async function setBg(status: string) {
  await updateBgCheckStatus(offer.value.id, status)
  ElMessage.success('背调状态已更新')
  load()
}

async function submitApproval() {
  await submitOfferApproval(offer.value.id)
  ElMessage.success('已提交审批')
  load()
}

async function sendOffer() {
  await sendOfferApi(offer.value.id)
  ElMessage.success('Offer 已发送')
  load()
}

async function acceptOffer() {
  await acceptOfferApi(offer.value.id)
  ElMessage.success('候选人已接受 Offer')
  load()
}

onMounted(load)
</script>

<style scoped>
.info-grid p { margin: 8px 0; }
.actions { margin-top: 16px; display: flex; gap: 8px; }
</style>
