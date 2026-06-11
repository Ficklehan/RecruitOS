<template>
  <div class="referral-page">
    <RCard class="referral-card">
      <h1 class="title">员工内推</h1>
      <p v-if="linkInfo" class="subtitle">
        {{ linkInfo.referrerName }} 邀请您投递「{{ linkInfo.jobTitle }}」
      </p>
      <p v-else-if="loading" class="subtitle">加载中…</p>
      <p v-else class="subtitle text-destructive">链接无效或已过期</p>

      <form v-if="linkInfo" class="form" @submit.prevent="handleSubmit">
        <FormField label="姓名" required>
          <RInput v-model="form.candidateName" placeholder="候选人姓名" />
        </FormField>
        <FormField label="手机">
          <RInput v-model="form.phone" placeholder="手机号" />
        </FormField>
        <FormField label="邮箱">
          <RInput v-model="form.email" placeholder="邮箱" />
        </FormField>
        <FormField label="备注">
          <RTextarea v-model="form.remark" :rows="3" placeholder="补充说明（选填）" />
        </FormField>
        <RButton type="submit" class="w-full" :disabled="submitting">
          {{ submitting ? '提交中…' : '提交简历' }}
        </RButton>
      </form>

      <p v-if="submitted" class="success-msg">提交成功，HR 将尽快与您联系。</p>
    </RCard>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { toast } from '@/lib/notify'
import FormField from '@/components/app/FormField.vue'
import { RCard, RInput, RTextarea, RButton } from '@/components/ui'
import { getReferralLinkInfo, submitReferralPublic } from '@/api/modules/referral'

const route = useRoute()
const token = route.params.token as string
const loading = ref(true)
const submitting = ref(false)
const submitted = ref(false)
const linkInfo = ref<{ jobTitle: string; referrerName: string } | null>(null)
const form = reactive({
  candidateName: '',
  phone: '',
  email: '',
  remark: '',
})

onMounted(async () => {
  try {
    const res: any = await getReferralLinkInfo(token)
    linkInfo.value = res.data ?? res
  } catch {
    linkInfo.value = null
  } finally {
    loading.value = false
  }
})

async function handleSubmit() {
  if (!form.candidateName.trim()) {
    toast.error('请填写姓名')
    return
  }
  submitting.value = true
  try {
    await submitReferralPublic({
      token,
      candidateName: form.candidateName.trim(),
      phone: form.phone || undefined,
      email: form.email || undefined,
      remark: form.remark || undefined,
    })
    submitted.value = true
    toast.success('提交成功')
  } catch (e: any) {
    toast.error(e?.message || '提交失败')
  } finally {
    submitting.value = false
  }
}
</script>

<style lang="scss" scoped>
.referral-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: linear-gradient(160deg, #f0f4ff 0%, var(--r-bg-card) 50%);
}

.referral-card {
  width: 100%;
  max-width: 480px;
  padding: 32px;
}

.title {
  font-size: 22px;
  font-weight: 700;
  margin-bottom: 8px;
}

.subtitle {
  color: var(--muted-foreground);
  margin-bottom: 24px;
}

.form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.success-msg {
  margin-top: 16px;
  color: var(--r-success);
  text-align: center;
}
</style>
