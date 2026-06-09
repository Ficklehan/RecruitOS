<template>
  <div class="page-container page-stack">
    <div class="page-header">
      <h2 class="page-title">SSO配置</h2>
      <el-button type="primary" @click="handleSave">
        <el-icon><Check /></el-icon>
        保存配置
      </el-button>
    </div>

    <el-card>
      <el-form ref="formRef" :model="formData" label-width="120px" size="large">
        <el-form-item label="启用 SSO">
          <el-switch v-model="formData.enabled" />
        </el-form-item>

        <template v-if="formData.enabled">
          <el-form-item label="SSO 类型">
            <el-select v-model="formData.type" placeholder="请选择 SSO 类型" style="width: 100%">
              <el-option label="SAML 2.0" value="saml" />
              <el-option label="OAuth 2.0" value="oauth" />
              <el-option label="OpenID Connect" value="oidc" />
            </el-select>
          </el-form-item>

          <el-form-item label="SSO URL">
            <el-input v-model="formData.ssoUrl" placeholder="请输入 SSO 登录 URL" />
          </el-form-item>

          <el-form-item label="Client ID">
            <el-input v-model="formData.clientId" placeholder="请输入 Client ID" />
          </el-form-item>

          <el-form-item label="Client Secret">
            <el-input v-model="formData.clientSecret" type="password" placeholder="请输入 Client Secret" show-password />
          </el-form-item>

          <el-form-item label="回调地址">
            <el-input v-model="formData.callbackUrl" placeholder="请输入回调地址" />
          </el-form-item>

          <el-form-item label="自动创建用户">
            <el-switch v-model="formData.autoCreateUser" />
            <span class="form-tip">开启后，SSO 登录时自动创建不存在的用户</span>
          </el-form-item>
        </template>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'

const formRef = ref()

const formData = reactive({
  enabled: false,
  type: 'oidc',
  ssoUrl: '',
  clientId: '',
  clientSecret: '',
  callbackUrl: 'https://recruitos.example.com/auth/callback',
  autoCreateUser: true,
})

function handleSave() {
  ElMessage.success('SSO 配置保存成功')
}
</script>

<style lang="scss" scoped>
@import '@/assets/styles/variables.scss';
.form-tip {
  margin-left: 12px;
  font-size: 13px;
  color: $text-secondary;
}
</style>
