<template>
  <PageShell title="许可信息">
<RCard>
      <CardContent class="pt-6">
        <div class="license-info">
          <div class="license-header">
            <div class="license-icon">
              <Stamp class="h-12 w-12 text-primary" />
            </div>
            <div class="license-title">
              <h3>{{ licenseInfo.plan }} 版</h3>
              <RBadge :variant="licenseInfo.status === 'active' ? 'default' : 'destructive'" class="mt-2">
                {{ licenseInfo.status === 'active' ? '有效' : '已过期' }}
              </RBadge>
            </div>
          </div>

          <dl class="license-details grid grid-cols-1 sm:grid-cols-2 gap-x-8 gap-y-4">
            <div v-for="item in detailItems" :key="item.label" class="flex flex-col gap-1">
              <dt class="text-sm text-muted-foreground">{{ item.label }}</dt>
              <dd class="text-sm font-medium">{{ item.value }}</dd>
            </div>
          </dl>

          <div class="license-actions">
            <RButton>续费</RButton>
            <RButton variant="outline">升级套餐</RButton>
            <RButton variant="outline">导出许可</RButton>
          </div>
        </div>
      </CardContent>
    </RCard>
</PageShell>
</template>

<script setup lang="ts">
import PageShell from '@/components/Layout/PageShell.vue'
import { reactive, computed } from 'vue'
import { Stamp } from 'lucide-vue-next'
import { RButton, RBadge, RCard, CardContent } from '@/components/ui'

const licenseInfo = reactive({
  licenseNo: 'RO-2024-XXXX-XXXX-XXXX',
  companyName: 'RecruitOS 演示企业',
  plan: '专业版',
  status: 'active',
  maxUsers: 100,
  usedUsers: 35,
  startDate: '2024-01-01',
  endDate: '2024-12-31',
  remainingDays: 291,
})

const detailItems = computed(() => [
  { label: '许可证编号', value: licenseInfo.licenseNo },
  { label: '企业名称', value: licenseInfo.companyName },
  { label: '套餐类型', value: licenseInfo.plan },
  { label: '用户上限', value: `${licenseInfo.maxUsers} 人` },
  { label: '生效日期', value: licenseInfo.startDate },
  { label: '到期日期', value: licenseInfo.endDate },
  { label: '已用用户数', value: `${licenseInfo.usedUsers} 人` },
  { label: '剩余天数', value: `${licenseInfo.remainingDays} 天` },
])
</script>

<style lang="scss" scoped>
@import '@/assets/styles/variables.scss';

.license-info {
  max-width: 800px;
}

.license-header {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-bottom: 32px;
  padding: 24px;
  background: linear-gradient(135deg, $primary-lighter 0%, $success-lighter 100%);
  border-radius: 8px;
}

.license-icon {
  width: 72px;
  height: 72px;
  background: $bg-card;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.license-title h3 {
  font-size: 24px;
  font-weight: 700;
  color: $text-primary;
  margin: 0;
}

.license-details {
  margin-bottom: 32px;
}

.license-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}
</style>
