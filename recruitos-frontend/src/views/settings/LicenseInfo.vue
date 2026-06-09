<template>
  <div class="page-container page-stack">
    <div class="page-header">
      <h2 class="page-title">许可信息</h2>
    </div>

    <el-card>
      <div class="license-info">
        <div class="license-header">
          <div class="license-icon">
            <el-icon :size="48" color="#3B82F6"><Stamp /></el-icon>
          </div>
          <div class="license-title">
            <h3>{{ licenseInfo.plan }} 版</h3>
            <el-tag :type="licenseInfo.status === 'active' ? 'success' : 'danger'" size="large">
              {{ licenseInfo.status === 'active' ? '有效' : '已过期' }}
            </el-tag>
          </div>
        </div>

        <el-descriptions :column="2" class="license-details">
          <el-descriptions-item label="许可证编号">{{ licenseInfo.licenseNo }}</el-descriptions-item>
          <el-descriptions-item label="企业名称">{{ licenseInfo.companyName }}</el-descriptions-item>
          <el-descriptions-item label="套餐类型">{{ licenseInfo.plan }}</el-descriptions-item>
          <el-descriptions-item label="用户上限">{{ licenseInfo.maxUsers }} 人</el-descriptions-item>
          <el-descriptions-item label="生效日期">{{ licenseInfo.startDate }}</el-descriptions-item>
          <el-descriptions-item label="到期日期">{{ licenseInfo.endDate }}</el-descriptions-item>
          <el-descriptions-item label="已用用户数">{{ licenseInfo.usedUsers }} 人</el-descriptions-item>
          <el-descriptions-item label="剩余天数">{{ licenseInfo.remainingDays }} 天</el-descriptions-item>
        </el-descriptions>

        <div class="license-actions">
          <el-button type="primary">续费</el-button>
          <el-button>升级套餐</el-button>
          <el-button>导出许可</el-button>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { reactive } from 'vue'

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

.license-title {
  h3 {
    font-size: 24px;
    font-weight: 700;
    color: $text-primary;
    margin-bottom: 8px;
  }
}

.license-details {
  margin-bottom: 32px;
}

.license-actions {
  display: flex;
  gap: 12px;
}
</style>
