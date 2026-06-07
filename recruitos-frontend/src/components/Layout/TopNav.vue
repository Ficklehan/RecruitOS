<template>
  <nav class="top-nav">
    <div class="nav-left">
      <!-- Logo -->
      <div class="nav-logo" @click="$router.push('/dashboard')">
        <svg viewBox="0 0 32 32" fill="none" xmlns="http://www.w3.org/2000/svg">
          <rect width="32" height="32" rx="8" fill="#3B82F6"/>
          <path d="M9 11h14M9 16h10M9 21h6" stroke="white" stroke-width="2" stroke-linecap="round"/>
          <circle cx="23" cy="21" r="3" fill="white" opacity="0.9"/>
        </svg>
        <span class="logo-text">RecruitOS</span>
      </div>

      <!-- 主导航 -->
      <div class="nav-items">
        <div
          v-for="item in navGroups"
          :key="item.key"
          class="nav-item"
          :class="{ active: isActiveGroup(item) }"
          @click="handleNavClick(item)"
        >
          <span>{{ item.label }}</span>
        </div>
      </div>
    </div>

    <div class="nav-right">
      <!-- 搜索 -->
      <div class="nav-search" @click="handleSearch">
        <el-icon :size="15"><Search /></el-icon>
        <span>搜索</span>
        <kbd>⌘K</kbd>
      </div>

      <!-- 通知 -->
      <el-popover placement="bottom-end" :width="320" trigger="click">
        <template #reference>
          <div class="nav-icon-btn">
            <el-badge :value="3" :max="99" :offset="[-4, 4]">
              <el-icon :size="18"><Bell /></el-icon>
            </el-badge>
          </div>
        </template>
        <div class="notif-panel">
          <div class="notif-title">通知</div>
          <div class="notif-item unread">
            <div class="notif-dot"></div>
            <div><p>张三 通过了「高级前端工程师」面试</p><span>10 分钟前</span></div>
          </div>
          <div class="notif-item unread">
            <div class="notif-dot"></div>
            <div><p>新需求「产品经理」等待审批</p><span>30 分钟前</span></div>
          </div>
          <div class="notif-item">
            <div class="notif-dot-ph"></div>
            <div><p>李四 的 Offer 已发送</p><span>1 小时前</span></div>
          </div>
        </div>
      </el-popover>

      <!-- 用户 -->
      <el-dropdown trigger="click" @command="handleCommand">
        <div class="nav-user">
          <div class="user-avatar">{{ userName.charAt(0) }}</div>
          <span class="user-name">{{ userName }}</span>
          <el-icon :size="12"><ArrowDown /></el-icon>
        </div>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="profile"><el-icon><User /></el-icon>个人信息</el-dropdown-item>
            <el-dropdown-item command="password"><el-icon><Lock /></el-icon>修改密码</el-dropdown-item>
            <el-dropdown-item divided command="logout"><el-icon><SwitchButton /></el-icon>退出登录</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </nav>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessageBox } from 'element-plus'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const userName = computed(() => userStore.userInfo?.realName || '管理员')

// 导航分组：把 10 个模块整合成 5-6 个顶部项
const navGroups = [
  { key: 'dashboard', label: '仪表盘', path: '/dashboard', match: ['/dashboard'] },
  { key: 'recruit', label: '招聘', path: '/position/demand', match: ['/position', '/talent', '/screening'] },
  { key: 'process', label: '流程', path: '/interview/board', match: ['/interview', '/hiring', '/onboard'] },
  { key: 'ai', label: 'AI 工具', path: '/ai-tools/template', match: ['/ai-tools', '/agent', '/evolution', '/communication', '/referral', '/headhunter'] },
  { key: 'analytics', label: '分析', path: '/analytics/funnel', match: ['/analytics'] },
  { key: 'settings', label: '设置', path: '/settings/tenant', match: ['/settings'] },
]

function isActiveGroup(item: typeof navGroups[0]) {
  return item.match.some(prefix => route.path.startsWith(prefix))
}

function handleNavClick(item: typeof navGroups[0]) {
  router.push(item.path)
}

function handleSearch() {}

async function handleCommand(cmd: string) {
  if (cmd === 'logout') {
    try {
      await ElMessageBox.confirm('确定要退出登录吗？', '退出确认', {
        confirmButtonText: '退出',
        cancelButtonText: '取消',
        type: 'warning',
      })
      await userStore.logout()
      router.push('/login')
    } catch {}
  }
}
</script>

<style lang="scss" scoped>
@import '@/assets/styles/variables.scss';

.top-nav {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  height: $topnav-height;
  background: $bg-card;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  z-index: 200;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

.nav-left {
  display: flex;
  align-items: center;
  gap: 32px;
}

.nav-logo {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  flex-shrink: 0;

  svg { width: 28px; height: 28px; }
}

.logo-text {
  font-size: 16px;
  font-weight: 700;
  color: $text-primary;
  letter-spacing: -0.02em;
}

.nav-items {
  display: flex;
  align-items: center;
  gap: 4px;
}

.nav-item {
  padding: 6px 14px;
  border-radius: $border-radius-sm;
  font-size: 14px;
  font-weight: 500;
  color: $text-secondary;
  cursor: pointer;
  transition: all $transition-fast;
  white-space: nowrap;

  &:hover {
    color: $text-primary;
    background: $bg-muted;
  }

  &.active {
    color: $primary-color;
    background: $primary-lighter;
  }
}

.nav-right {
  display: flex;
  align-items: center;
  gap: 6px;
}

.nav-search {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 5px 10px;
  border-radius: $border-radius-sm;
  border: 1px solid $border-color;
  color: $text-placeholder;
  font-size: 13px;
  cursor: pointer;
  transition: all $transition-fast;

  &:hover { border-color: $primary-light; }

  kbd {
    font-family: inherit;
    font-size: 10px;
    padding: 0px 4px;
    border-radius: 3px;
    border: 1px solid $border-color;
    background: $bg-muted;
    color: $text-placeholder;
  }
}

.nav-icon-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  border-radius: $border-radius-sm;
  cursor: pointer;
  color: $text-secondary;
  transition: all $transition-fast;

  &:hover { background: $bg-muted; color: $text-primary; }
}

.notif-panel {
  .notif-title {
    font-size: 14px;
    font-weight: 600;
    color: $text-primary;
    padding-bottom: 10px;
    border-bottom: 1px solid $border-color-light;
    margin-bottom: 6px;
  }
  .notif-item {
    display: flex;
    align-items: flex-start;
    gap: 10px;
    padding: 9px 6px;
    border-radius: $border-radius-sm;
    cursor: pointer;
    &:hover { background: $bg-muted; }
    &.unread p { font-weight: 500; color: $text-primary; }
    p { font-size: 13px; color: $text-regular; margin-bottom: 2px; line-height: 1.4; }
    span { font-size: 12px; color: $text-placeholder; }
  }
  .notif-dot { width: 7px; height: 7px; border-radius: 50%; background: $primary-color; flex-shrink: 0; margin-top: 6px; }
  .notif-dot-ph { width: 7px; height: 7px; flex-shrink: 0; margin-top: 6px; }
}

.nav-user {
  display: flex;
  align-items: center;
  gap: 7px;
  padding: 4px 8px 4px 4px;
  border-radius: $border-radius-sm;
  cursor: pointer;
  transition: background $transition-fast;

  &:hover { background: $bg-muted; }
}

.user-avatar {
  width: 28px;
  height: 28px;
  border-radius: 7px;
  background: linear-gradient(135deg, $primary-color, $primary-dark);
  color: #fff;
  font-size: 12px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
}

.user-name {
  font-size: 13px;
  color: $text-regular;
  font-weight: 500;
  max-width: 72px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
