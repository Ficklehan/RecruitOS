<template>
  <div class="platform-layout">
    <aside class="sidebar">
      <div class="sidebar-brand">
        <div class="brand-icon">
          <svg viewBox="0 0 32 32" fill="none">
            <rect width="32" height="32" rx="8" fill="#3B82F6"/>
            <path d="M9 11h14M9 16h10M9 21h6" stroke="#fff" stroke-width="2" stroke-linecap="round"/>
          </svg>
        </div>
        <div class="brand-text">
          <span class="brand-name">RecruitOS</span>
          <span class="brand-tag">平台管理</span>
        </div>
      </div>

      <nav class="sidebar-nav" aria-label="平台导航">
        <router-link
          v-for="item in menuItems"
          :key="item.path"
          :to="item.path"
          class="nav-item"
          :class="{ active: route.path.startsWith(item.path) }"
        >
          <el-icon :size="18"><component :is="item.icon" /></el-icon>
          <span>{{ item.label }}</span>
        </router-link>
      </nav>

      <div class="sidebar-footer">
        <div class="admin-avatar">{{ (adminName[0] || 'A').toUpperCase() }}</div>
        <div class="admin-info">
          <span class="admin-name">{{ adminName }}</span>
          <span class="admin-role">平台管理员</span>
        </div>
        <el-icon class="logout-btn" @click="handleLogout" :size="18"><SwitchButton /></el-icon>
      </div>
    </aside>

    <main class="main-content">
      <router-view />
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { OfficeBuilding, SwitchButton } from '@element-plus/icons-vue'
import { platformLogoutApi } from '@/api/modules/platform'
import { removeToken } from '@/utils/auth'

const route = useRoute()
const router = useRouter()

const adminName = computed(() => localStorage.getItem('platformAdminName') || '管理员')

const menuItems = [
  { path: '/platform/tenants', label: '租户管理', icon: OfficeBuilding },
]

async function handleLogout() {
  try { await platformLogoutApi() } catch {}
  removeToken()
  localStorage.removeItem('isPlatformAdmin')
  localStorage.removeItem('platformAdminName')
  router.push('/login')
}
</script>

<style lang="scss" scoped>
@import '@/assets/styles/variables.scss';

.platform-layout {
  display: flex;
  height: 100vh;
  background: $bg-page;
}

.sidebar {
  width: $sidebar-width;
  background: $bg-card;
  border-right: 1px solid $border-color;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
}

.sidebar-brand {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 20px 18px;
  border-bottom: 1px solid $border-color-light;
}

.brand-icon {
  width: 32px;
  height: 32px;
  flex-shrink: 0;

  svg {
    width: 100%;
    height: 100%;
  }
}

.brand-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.brand-name {
  font-size: 15px;
  font-weight: 600;
  color: $text-primary;
}

.brand-tag {
  font-size: 11px;
  color: $text-secondary;
  font-weight: 500;
}

.sidebar-nav {
  flex: 1;
  padding: 12px 10px;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: $border-radius-sm;
  color: $text-secondary;
  font-size: 14px;
  font-weight: 500;
  text-decoration: none;
  transition: background-color $transition-fast, color $transition-fast;

  &:hover {
    background: $bg-muted;
    color: $text-primary;
  }

  &.active {
    background: $primary-lighter;
    color: $primary-color;
  }
}

.sidebar-footer {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 16px;
  border-top: 1px solid $border-color-light;
}

.admin-avatar {
  width: 32px;
  height: 32px;
  border-radius: $border-radius-sm;
  background: $primary-color;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 600;
  flex-shrink: 0;
}

.admin-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.admin-name {
  font-size: 13px;
  font-weight: 500;
  color: $text-primary;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.admin-role {
  font-size: 11px;
  color: $text-secondary;
}

.logout-btn {
  color: $text-secondary;
  cursor: pointer;
  padding: 6px;
  border-radius: $border-radius-sm;
  transition: color $transition-fast, background-color $transition-fast;

  &:hover {
    color: $danger-color;
    background: $danger-lighter;
  }
}

.main-content {
  flex: 1;
  overflow-y: auto;
  min-width: 0;
  width: 100%;
  padding: 16px 20px 28px;
}
</style>
