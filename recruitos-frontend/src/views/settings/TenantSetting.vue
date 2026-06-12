<template>
  <PageShell title="租户设置">
<RCard>
      <CardContent class="pt-6 space-y-2 max-w-2xl">
        <h3 class="form-section-title">基本信息</h3>
        <FormField label="企业名称" required :error="formErrors.name">
          <RInput v-model="formData.name" placeholder="请输入企业名称" />
        </FormField>
        <FormField label="统一社会信用代码" required :error="formErrors.creditCode">
          <RInput v-model="formData.creditCode" placeholder="请输入统一社会信用代码" />
        </FormField>
        <FormField label="当前套餐" required :error="formErrors.plan">
          <RSelect v-model="formData.plan" :options="planOptions" placeholder="请选择套餐" />
        </FormField>
        <FormField label="联系人" required :error="formErrors.contact">
          <RInput v-model="formData.contact" placeholder="请输入联系人姓名" />
        </FormField>
        <FormField label="联系电话" required :error="formErrors.phone">
          <RInput v-model="formData.phone" placeholder="请输入联系电话" />
        </FormField>

        <h3 class="form-section-title">白标配置</h3>
        <FormField label="企业 Logo">
          <label class="logo-uploader cursor-pointer block">
            <input type="file" accept="image/*" class="hidden" @change="onLogoChange" />
            <div v-if="formData.logo" class="logo-preview">
              <img :src="formData.logo" alt="Logo" />
            </div>
            <div v-else class="logo-placeholder">
              <Plus class="h-6 w-6" />
              <span>上传 Logo</span>
            </div>
          </label>
        </FormField>
        <FormField label="主题色">
          <input v-model="formData.primaryColor" type="color" class="h-10 w-20 cursor-pointer rounded-lg" />
        </FormField>
        <FormField label="自定义域名">
          <div class="flex">
            <span class="inline-flex items-center rounded-l-xl bg-bg-muted px-3 text-sm text-text-secondary">https://</span>
            <RInput v-model="formData.domain" placeholder="hr.yourcompany.com" class="rounded-l-none" />
          </div>
        </FormField>

        <h3 class="form-section-title">SSO 配置</h3>
        <FormField label="启用 SSO">
          <RSwitch v-model="formData.ssoEnabled" />
        </FormField>
        <template v-if="formData.ssoEnabled">
          <FormField label="SSO 类型">
            <RSelect v-model="formData.ssoType" :options="ssoTypeOptions" placeholder="请选择 SSO 类型" />
          </FormField>
          <FormField label="SSO URL">
            <RInput v-model="formData.ssoUrl" placeholder="请输入 SSO 登录 URL" />
          </FormField>
          <FormField label="Client ID">
            <RInput v-model="formData.ssoClientId" placeholder="请输入 Client ID" />
          </FormField>
          <FormField label="Client Secret">
            <RInput v-model="formData.ssoClientSecret" type="password" placeholder="请输入 Client Secret" />
          </FormField>
        </template>

        <h3 class="form-section-title">渠道招聘安全</h3>
        <p class="section-hint">控制高风险自动化能力。默认仅半自动（发送前请你确认）。</p>
        <FormField label="允许全自动运行">
          <div class="flex items-center gap-3">
            <RSwitch v-model="recruitSafety.allowFullAuto" />
            <span class="field-hint">开启后，启动平台招人任务时可选择全自动</span>
          </div>
        </FormField>
        <FormField label="允许卡片即联系">
          <div class="flex items-center gap-3">
            <RSwitch v-model="recruitSafety.allowCardGreet" />
            <span class="field-hint">不打开完整简历即发送联系，适用于批量初级岗</span>
          </div>
        </FormField>
        <FormField label="默认运行方式">
          <RSelect v-model="recruitSafety.defaultRunMode" :options="runModeOptions" class="w-full sm:w-72" />
        </FormField>

        <h3 class="form-section-title">招人方式建议</h3>
        <p class="section-hint">试用环境可调低信号阈值，更快看到优化建议（正式环境建议 ≥15）。</p>
        <FormField label="最少信号数">
          <div class="flex items-center gap-3">
            <RInput v-model.number="evolutionMinSignals" type="number" min="3" max="100" class="w-32" />
            <span class="field-hint">当前默认 {{ evolutionDefaultMinSignals }}</span>
          </div>
        </FormField>
      </CardContent>
    </RCard>
    <div class="mt-6 flex gap-3">
      <RButton @click="handleSave">保存设置</RButton>
    </div>
</PageShell>
</template>

<script setup lang="ts">
import PageShell from '@/components/Layout/PageShell.vue'
import { ref, reactive, onMounted } from 'vue'
import { Check, Plus } from 'lucide-vue-next'
import { toast } from '@/lib/notify'
import FormField from '@/components/app/FormField.vue'
import { RButton, RInput, RSelect, RCard, CardContent, RSwitch } from '@/components/ui'
import { loadTenantRecruitSafety, saveTenantRecruitSafety } from '@/utils/tenantRecruitSafety'
import { getEvolutionSettings, updateEvolutionMinSignals } from '@/api/modules/evolution'

const formData = reactive({
  name: 'RecruitOS 演示企业',
  creditCode: '91110108MA01XXXXXX',
  plan: 'professional',
  contact: '张三',
  phone: '13800138000',
  logo: '',
  primaryColor: '#4F6BED',
  domain: '',
  ssoEnabled: false,
  ssoType: 'oidc',
  ssoUrl: '',
  ssoClientId: '',
  ssoClientSecret: '',
})

const recruitSafety = reactive(loadTenantRecruitSafety())
const evolutionMinSignals = ref(15)
const evolutionDefaultMinSignals = ref(15)

const formErrors = reactive({
  name: '',
  creditCode: '',
  plan: '',
  contact: '',
  phone: '',
})

const planOptions = [
  { label: '基础版', value: 'basic' },
  { label: '专业版', value: 'professional' },
  { label: '企业版', value: 'enterprise' },
]

const ssoTypeOptions = [
  { label: 'SAML 2.0', value: 'saml' },
  { label: 'OAuth 2.0', value: 'oauth' },
  { label: 'OIDC', value: 'oidc' },
]

const runModeOptions = [
  { label: '半自动（发送前请你确认）', value: 'SEMI_AUTO' },
  { label: '仅发布与搜索', value: 'PUBLISH_SEARCH_ONLY' },
]

function onLogoChange(e: Event) {
  const file = (e.target as HTMLInputElement).files?.[0]
  if (!file) return
  const reader = new FileReader()
  reader.onload = () => { formData.logo = reader.result as string }
  reader.readAsDataURL(file)
}

onMounted(async () => {
  Object.assign(recruitSafety, loadTenantRecruitSafety())
  try {
    const res: any = await getEvolutionSettings()
    const data = res.data || res
    evolutionMinSignals.value = data.minSignals ?? 15
    evolutionDefaultMinSignals.value = data.defaultMinSignals ?? 15
  } catch {
    // ignore
  }
})

function validateForm(): boolean {
  formErrors.name = formData.name ? '' : '请输入企业名称'
  formErrors.creditCode = formData.creditCode ? '' : '请输入统一社会信用代码'
  formErrors.plan = formData.plan ? '' : '请选择套餐'
  formErrors.contact = formData.contact ? '' : '请输入联系人'
  formErrors.phone = formData.phone ? '' : '请输入联系电话'
  return !Object.values(formErrors).some(Boolean)
}

async function handleSave() {
  if (!validateForm()) return
  saveTenantRecruitSafety({ ...recruitSafety })
  try {
    await updateEvolutionMinSignals(evolutionMinSignals.value)
  } catch {
    toast.error('渠道安全已保存，进化信号阈值需 evolution 服务在线')
  }
  toast.success('设置已保存')
}
</script>

<style lang="scss" scoped>
@import '@/assets/styles/variables.scss';

.form-section-title {
  font-size: 16px;
  font-weight: 600;
  color: $text-primary;
  margin: 24px 0 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--r-divider);

  &:first-child {
    margin-top: 0;
  }
}

.section-hint {
  font-size: 13px;
  color: $text-secondary;
  margin: -8px 0 16px;
}

.field-hint {
  font-size: 12px;
  color: $text-secondary;
}

.logo-uploader {
  border: 2px dashed $border-color-light;
  border-radius: 12px;
  overflow: hidden;
  transition: all 0.3s;
  width: fit-content;

  &:hover {
    border-color: $primary-color;
    background: $primary-50;
  }
}

.logo-preview {
  width: 120px;
  height: 120px;

  img {
    width: 100%;
    height: 100%;
    object-fit: contain;
  }
}

.logo-placeholder {
  width: 120px;
  height: 120px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: $text-secondary;

  span {
    font-size: 12px;
  }
}
</style>
