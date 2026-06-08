<template>
  <nav class="top-nav">
    <div class="nav-left">
      <div class="nav-logo" @click="goHome">
        <svg viewBox="0 0 32 32" fill="none" xmlns="http://www.w3.org/2000/svg">
          <rect width="32" height="32" rx="8" fill="#3B82F6"/>
          <path d="M9 11h14M9 16h10M9 21h6" stroke="white" stroke-width="2" stroke-linecap="round"/>
          <circle cx="23" cy="21" r="3" fill="white" opacity="0.9"/>
        </svg>
        <span class="logo-text">RecruitOS</span>
      </div>

      <div class="nav-items">
        <div
          v-for="item in visibleTopMenus"
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
      <el-popover placement="bottom-end" :width="320" trigger="click" @show="loadNotifications">
        <template #reference>
          <el-badge :value="unreadCount" :hidden="!unreadCount" class="nav-bell">
            <el-button text circle>
              <el-icon :size="18"><Bell /></el-icon>
            </el-button>
          </el-badge>
        </template>
        <div class="notify-panel">
          <div class="notify-title">通知</div>
          <el-empty v-if="!notifications.length" description="暂无通知" :image-size="60" />
          <div
            v-for="n in notifications"
            :key="n.id"
            class="notify-item"
            :class="{ unread: n.isRead !== 1 }"
            @click="openNotification(n)"
          >
            <div class="notify-item-title">{{ n.title }}</div>
            <div class="notify-item-time">{{ n.createdAt }}</div>
          </div>
          <el-button text type="primary" @click="router.push('/workspace/inbox')">查看收件箱</el-button>
        </div>
      </el-popover>

      <el-dropdown trigger="click" @command="handleCommand">
        <div class="nav-user">
          <div class="user-avatar">{{ userName.charAt(0) }}</div>
          <span class="user-name">{{ userName }}</span>
          <el-icon :size="12"><ArrowDown /></el-icon>
        </div>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="logout"><el-icon><SwitchButton /></el-icon>退出登录</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </nav>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessageBox } from 'element-plus'
import { topNavMenus, filterMenus, getDefaultRoute, type MenuItem } from '@/config/menus'
import { getMyNotifications, markNotificationRead } from '@/api/modules/notification'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const userName = computed(() => userStore.userInfo?.realName || '管理员')
const notifications = ref<any[]>([])

const unreadCount = computed(() => notifications.value.filter(n => n.isRead !== 1).length)

async function loadNotifications() {
  try {
    const res = await getMyNotifications(10)
    notifications.value = res.data || []
  } catch {
    notifications.value = []
  }
}

async function openNotification(n: any) {
  if (n.isRead !== 1) {
    await markNotificationRead(n.id)
    n.isRead = 1
  }
  router.push('/workspace/inbox')
}

const visibleTopMenus = computed(() =>
  filterMenus(topNavMenus, userStore.permissions)
)

function isActiveGroup(item: MenuItem) {
  const prefix = '/' + item.key
  return route.path.startsWith(prefix)
}

function goHome() {
  const roleCodes = userStore.roles.map((r: any) => (typeof r === 'string' ? r : r.roleCode)).filter(Boolean)
  router.push(getDefaultRoute(roleCodes))
}

function handleNavClick(item: MenuItem) {
  router.push(item.path)
}

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

  &:hover { color: $text-primary; background: $bg-muted; }
  &.active { color: $primary-color; background: $primary-lighter; }
}

.nav-right {
  display: flex;
  align-items: center;
  gap: 6px;
}

.nav-user {
  display: flex;
  align-items: center;
  gap: 7px;
  padding: 4px 8px 4px 4px;
  border-radius: $border-radius-sm;
  cursor: pointer;
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

.nav-bell { margin-right: 4px; }

.notify-panel {
  .notify-title { font-weight: 600; margin-bottom: 8px; }
  .notify-item {
    padding: 8px 0;
    border-bottom: 1px solid $border-color-light;
    cursor: pointer;
    &.unread .notify-item-title { font-weight: 600; color: $primary-color; }
  }
  .notify-item-title { font-size: 13px; }
  .notify-item-time { font-size: 11px; color: $text-secondary; margin-top: 2px; }
}
</style>
