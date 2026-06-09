<template>
  <div class="page-container page-stack">
    <div class="page-header">
      <h2 class="page-title">租户设置</h2>
      <el-button type="primary" @click="handleSave">
        <el-icon><Check /></el-icon>
        保存设置
      </el-button>
    </div>

    <el-card>
      <el-form
        ref="formRef"
        :model="formData"
        :rules="rules"
        label-width="140px"
        size="large"
      >
        <!-- 基本信息 -->
        <h3 class="form-section-title">基本信息</h3>
        <el-form-item label="企业名称" prop="name">
          <el-input v-model="formData.name" placeholder="请输入企业名称" />
        </el-form-item>
        <el-form-item label="统一社会信用代码" prop="creditCode">
          <el-input v-model="formData.creditCode" placeholder="请输入统一社会信用代码" />
        </el-form-item>
        <el-form-item label="当前套餐" prop="plan">
          <el-select v-model="formData.plan" placeholder="请选择套餐" style="width: 100%">
            <el-option label="基础版" value="basic" />
            <el-option label="专业版" value="professional" />
            <el-option label="企业版" value="enterprise" />
          </el-select>
        </el-form-item>
        <el-form-item label="联系人" prop="contact">
          <el-input v-model="formData.contact" placeholder="请输入联系人姓名" />
        </el-form-item>
        <el-form-item label="联系电话" prop="phone">
          <el-input v-model="formData.phone" placeholder="请输入联系电话" />
        </el-form-item>

        <!-- 白标配置 -->
        <h3 class="form-section-title">白标配置</h3>
        <el-form-item label="企业 Logo">
          <el-upload
            class="logo-uploader"
            action="#"
            :show-file-list="false"
            :auto-upload="false"
          >
            <div class="logo-preview" v-if="formData.logo">
              <img :src="formData.logo" alt="Logo" />
            </div>
            <div class="logo-placeholder" v-else>
              <el-icon :size="24"><Plus /></el-icon>
              <span>上传 Logo</span>
            </div>
          </el-upload>
        </el-form-item>
        <el-form-item label="主题色">
          <el-color-picker v-model="formData.primaryColor" show-alpha />
        </el-form-item>
        <el-form-item label="自定义域名">
          <el-input v-model="formData.domain" placeholder="例如: hr.yourcompany.com">
            <template #prepend>https://</template>
          </el-input>
        </el-form-item>

        <!-- SSO 配置 -->
        <h3 class="form-section-title">SSO 配置</h3>
        <el-form-item label="启用 SSO">
          <el-switch v-model="formData.ssoEnabled" />
        </el-form-item>
        <template v-if="formData.ssoEnabled">
          <el-form-item label="SSO 类型">
            <el-select v-model="formData.ssoType" placeholder="请选择 SSO 类型" style="width: 100%">
              <el-option label="SAML 2.0" value="saml" />
              <el-option label="OAuth 2.0" value="oauth" />
              <el-option label="OIDC" value="oidc" />
            </el-select>
          </el-form-item>
          <el-form-item label="SSO URL">
            <el-input v-model="formData.ssoUrl" placeholder="请输入 SSO 登录 URL" />
          </el-form-item>
          <el-form-item label="Client ID">
            <el-input v-model="formData.ssoClientId" placeholder="请输入 Client ID" />
          </el-form-item>
          <el-form-item label="Client Secret">
            <el-input v-model="formData.ssoClientSecret" type="password" placeholder="请输入 Client Secret" show-password />
          </el-form-item>
        </template>

        <!-- 渠道招聘安全 -->
        <h3 class="form-section-title">渠道招聘安全</h3>
        <p class="section-hint">控制高风险自动化能力。默认仅半自动（发送前请你确认）。</p>
        <el-form-item label="允许全自动运行">
          <el-switch v-model="recruitSafety.allowFullAuto" />
          <span class="field-hint">开启后，启动平台招人任务时可选择全自动</span>
        </el-form-item>
        <el-form-item label="允许卡片即联系">
          <el-switch v-model="recruitSafety.allowCardGreet" />
          <span class="field-hint">不打开完整简历即发送联系，适用于批量初级岗</span>
        </el-form-item>
        <el-form-item label="默认运行方式">
          <el-select v-model="recruitSafety.defaultRunMode" style="width: 280px">
            <el-option label="半自动（发送前请你确认）" value="SEMI_AUTO" />
            <el-option label="仅发布与搜索" value="PUBLISH_SEARCH_ONLY" />
          </el-select>
        </el-form-item>

        <h3 class="form-section-title">招人方式建议</h3>
        <p class="section-hint">试用环境可调低信号阈值，更快看到优化建议（正式环境建议 ≥15）。</p>
        <el-form-item label="最少信号数">
          <el-input-number v-model="evolutionMinSignals" :min="3" :max="100" />
          <span class="field-hint">当前默认 {{ evolutionDefaultMinSignals }}</span>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import { loadTenantRecruitSafety, saveTenantRecruitSafety } from '@/utils/tenantRecruitSafety'
import { getEvolutionSettings, updateEvolutionMinSignals } from '@/api/modules/evolution'

const formRef = ref<FormInstance>()

const formData = reactive({
  name: 'RecruitOS 演示企业',
  creditCode: '91110108MA01XXXXXX',
  plan: 'professional',
  contact: '张三',
  phone: '13800138000',
  logo: '',
  primaryColor: '#3B82F6',
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

const rules: FormRules = {
  name: [{ required: true, message: '请输入企业名称', trigger: 'blur' }],
  creditCode: [{ required: true, message: '请输入统一社会信用代码', trigger: 'blur' }],
  plan: [{ required: true, message: '请选择套餐', trigger: 'change' }],
  contact: [{ required: true, message: '请输入联系人', trigger: 'blur' }],
  phone: [{ required: true, message: '请输入联系电话', trigger: 'blur' }],
}

async function handleSave() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (valid) {
      saveTenantRecruitSafety({ ...recruitSafety })
      try {
        await updateEvolutionMinSignals(evolutionMinSignals.value)
      } catch {
        ElMessage.warning('渠道安全已保存，进化信号阈值需 evolution 服务在线')
      }
      ElMessage.success('设置已保存')
    }
  })
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
  border-bottom: 1px solid $border-color-light;

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
  margin-left: 12px;
  font-size: 12px;
  color: $text-secondary;
}

.logo-uploader {
  :deep(.el-upload) {
    border: 1px dashed $border-color;
    border-radius: 8px;
    cursor: pointer;
    overflow: hidden;
    transition: border-color 0.3s;

    &:hover {
      border-color: $primary-color;
    }
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
