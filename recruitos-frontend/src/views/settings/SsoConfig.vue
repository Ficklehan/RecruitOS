<template>
  <PageShell title="SSO配置">
<RCard>
      <CardContent class="pt-6 space-y-4 max-w-xl">
        <FormField label="启用 SSO">
          <RSwitch v-model="formData.enabled" />
        </FormField>

        <template v-if="formData.enabled">
          <FormField label="SSO 类型">
            <RSelect v-model="formData.type" :options="ssoTypeOptions" placeholder="请选择 SSO 类型" />
          </FormField>
          <FormField label="SSO URL">
            <RInput v-model="formData.ssoUrl" placeholder="请输入 SSO 登录 URL" />
          </FormField>
          <FormField label="Client ID">
            <RInput v-model="formData.clientId" placeholder="请输入 Client ID" />
          </FormField>
          <FormField label="Client Secret">
            <RInput v-model="formData.clientSecret" type="password" placeholder="请输入 Client Secret" />
          </FormField>
          <FormField label="回调地址">
            <RInput v-model="formData.callbackUrl" placeholder="请输入回调地址" />
          </FormField>
          <FormField label="自动创建用户">
            <div class="flex items-center gap-3">
              <RSwitch v-model="formData.autoCreateUser" />
              <span class="form-tip">开启后，SSO 登录时自动创建不存在的用户</span>
            </div>
          </FormField>
        </template>
      </CardContent>
    </RCard>
</PageShell>
</template>

<script setup lang="ts">
import PageShell from '@/components/Layout/PageShell.vue'
import { reactive } from 'vue'
import { Check } from 'lucide-vue-next'
import { toast } from '@/lib/notify'
import FormField from '@/components/app/FormField.vue'
import { RButton, RInput, RSelect, RCard, CardContent, RSwitch } from '@/components/ui'

const formData = reactive({
  enabled: false,
  type: 'oidc',
  ssoUrl: '',
  clientId: '',
  clientSecret: '',
  callbackUrl: 'https://recruitos.example.com/auth/callback',
  autoCreateUser: true,
})

const ssoTypeOptions = [
  { label: 'SAML 2.0', value: 'saml' },
  { label: 'OAuth 2.0', value: 'oauth' },
  { label: 'OpenID Connect', value: 'oidc' },
]

function handleSave() {
  toast.success('SSO 配置保存成功')
}
</script>

<style lang="scss" scoped>
@import '@/assets/styles/variables.scss';

.form-tip {
  font-size: 13px;
  color: $text-secondary;
}
</style>
