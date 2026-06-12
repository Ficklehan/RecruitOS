<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { RCard, RButton, RBadge, RInput, RSwitch, RSkeleton, RTable, RTableBody, RTableCell, RTableHead, RTableRow, RTableTh } from '@/components/ui'
import { MessageSquare, Send, Settings, Webhook, Bell, CheckCircle2, AlertTriangle } from 'lucide-vue-next'

const loading = ref(true)
const configs = ref<any[]>([])
const templates = ref<any[]>([])
const webhookUrl = ref('')
const testResult = ref<any>(null)
const showTestDialog = ref(false)

async function fetchConfigs() {
  try {
    const res = await fetch('/api/integration/configs')
    configs.value = await res.json()
    const wh = configs.value.find((c: any) => c.configKey === 'webhook_url')
    if (wh) webhookUrl.value = '••••••••' // 不展示完整 webhook
  } catch { /* ignore */ }
}

async function fetchTemplates() {
  try {
    const res = await fetch('/api/integration/templates?platform=FEISHU')
    templates.value = await res.json()
  } catch { /* ignore */ }
  loading.value = false
}

async function saveWebhook() {
  if (!webhookUrl.value || webhookUrl.value === '••••••••') return
  try {
    await fetch('/api/integration/configs', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ platform: 'FEISHU', configKey: 'webhook_url', configValue: webhookUrl.value }),
    })
    fetchConfigs()
  } catch { /* ignore */ }
}

async function testSend(templateCode: string) {
  testResult.value = null
  const endpoint = `/api/integration/send/${templateCode.toLowerCase().replace(/_/g, '-')}`
  try {
    const res = await fetch(endpoint, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: '{}' })
    testResult.value = await res.json()
  } catch (e) {
    testResult.value = { success: false, message: '请求失败' }
  }
}

onMounted(() => {
  fetchConfigs()
  fetchTemplates()
})
</script>

<template>
  <div class="space-y-6">
    <div class="flex items-center justify-between">
      <div>
        <h2 class="text-xl font-bold text-gray-900">飞书集成</h2>
        <p class="text-sm text-gray-500 mt-1">配置飞书机器人，实现面试邀请、Offer 审批、管道告警等卡片消息推送</p>
      </div>
      <RBadge variant="default" class="bg-green-50 text-green-600 border-green-200 text-xs">
        <Bell class="h-3 w-3 mr-1" /> 已连接
      </RBadge>
    </div>

    <!-- Webhook 配置 -->
    <RCard class="p-5">
      <h3 class="font-semibold text-gray-900 mb-4 flex items-center gap-2">
        <Webhook class="h-4 w-4 text-blue-500" /> Webhook 配置
      </h3>
      <div class="flex gap-3">
        <input v-model="webhookUrl"
               class="flex-1 px-4 py-2.5 border border-gray-200 rounded-lg text-sm font-mono focus:ring-2 focus:ring-blue-100 focus:border-blue-300"
               placeholder="https://open.feishu.cn/open-apis/bot/v2/hook/..." />
        <RButton @click="saveWebhook">
          <Settings class="h-4 w-4 mr-1" /> 保存
        </RButton>
      </div>
      <p class="text-xs text-gray-400 mt-2">从飞书群聊 → 设置 → 群机器人 → 添加机器人 → 复制 Webhook 地址</p>
    </RCard>

    <!-- 消息模板 -->
    <RCard class="p-5">
      <h3 class="font-semibold text-gray-900 mb-4 flex items-center gap-2">
        <MessageSquare class="h-4 w-4 text-blue-500" /> 消息模板
      </h3>
      <RSkeleton v-if="loading" class="h-32" />
      <RTable v-else>
        <RTableHead>
          <RTableRow>
            <RTableTh>模板名称</RTableTh>
            <RTableTh>模板编码</RTableTh>
            <RTableTh>状态</RTableTh>
            <RTableTh>操作</RTableTh>
          </RTableRow>
        </RTableHead>
        <RTableBody>
          <RTableRow v-for="t in templates" :key="t.id">
            <RTableCell class="font-medium">{{ t.templateName }}</RTableCell>
            <RTableCell class="text-xs text-gray-500 font-mono">{{ t.templateCode }}</RTableCell>
            <RTableCell>
              <RBadge :variant="t.enabled ? 'default' : 'secondary'" class="text-xs"
                      :class="t.enabled ? 'bg-green-50 text-green-600 border-green-200' : ''">
                {{ t.enabled ? '启用' : '禁用' }}
              </RBadge>
            </RTableCell>
            <RTableCell>
              <RButton size="xs" variant="outline" @click="testSend(t.templateCode)">
                <Send class="h-3 w-3 mr-1" /> 测试发送
              </RButton>
            </RTableCell>
          </RTableRow>
        </RTableBody>
      </RTable>
    </RCard>

    <!-- 测试结果 -->
    <RCard v-if="testResult" class="p-4" :class="testResult.success ? 'border-green-200 bg-green-50' : 'border-red-200 bg-red-50'">
      <div class="flex items-center gap-2">
        <CheckCircle2 v-if="testResult.success" class="h-4 w-4 text-green-600" />
        <AlertTriangle v-else class="h-4 w-4 text-red-600" />
        <span class="text-sm" :class="testResult.success ? 'text-green-700' : 'text-red-700'">
          {{ testResult.message }}
        </span>
      </div>
    </RCard>

    <!-- 使用说明 -->
    <RCard class="p-5 bg-blue-50 border-blue-200">
      <h3 class="font-semibold text-gray-900 mb-2">集成能力</h3>
      <div class="grid grid-cols-2 gap-3 text-sm text-gray-600">
        <div class="flex items-start gap-2">
          <span class="text-blue-500 mt-0.5">📋</span>
          <span><strong>面试邀请：</strong>自动向面试官和候选人发送面试安排卡片，支持一键确认/调整</span>
        </div>
        <div class="flex items-start gap-2">
          <span class="text-blue-500 mt-0.5">📝</span>
          <span><strong>Offer 审批：</strong>在飞书中完成 Offer 审批，含 AI 建议，批准/驳回即时同步</span>
        </div>
        <div class="flex items-start gap-2">
          <span class="text-blue-500 mt-0.5">🔥</span>
          <span><strong>管道告警：</strong>管道枯竭/停滞时主动推送告警卡片，可直接触达寻源</span>
        </div>
        <div class="flex items-start gap-2">
          <span class="text-blue-500 mt-0.5">⏰</span>
          <span><strong>决策提醒：</strong>候选人在某阶段停留过久时提醒 HR/HM，含 AI 观察</span>
        </div>
      </div>
    </RCard>
  </div>
</template>
