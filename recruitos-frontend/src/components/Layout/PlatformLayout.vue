<template>
  <div class="platform-layout">
    <aside class="sidebar">
      <div class="sidebar-brand">
        <div class="brand-icon">
          <svg viewBox="0 0 32 32" fill="none">
            <rect width="32" height="32" rx="8" fill="url(#g1)"/>
            <path d="M9 11h14M9 16h10M9 21h6" stroke="#fff" stroke-width="2" stroke-linecap="round"/>
            <defs><linearGradient id="g1" x1="0" y1="0" x2="32" y2="32"><stop stop-color="#6366f1"/><stop offset="1" stop-color="#8b5cf6"/></linearGradient></defs>
          </svg>
        </div>
        <div class="brand-text">
          <span class="brand-name">RecruitOS</span>
          <span class="brand-tag">Platform</span>
        </div>
      </div>

      <nav class="sidebar-nav">
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

<style scoped>
.platform-layout {
  display: flex;
  height: 100vh;
  background: #f8fafc;
}

/* Sidebar */
.sidebar {
  width: 240px;
  background: #0f172a;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
}

.sidebar-brand {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 24px 20px;
  border-bottom: 1px solid rgba(255,255,255,0.06);
}

.brand-icon {
  width: 36px;
  height: 36px;
  flex-shrink: 0;
}
.brand-icon svg { width: 100%; height: 100%; }

.brand-text {
  display: flex;
  flex-direction: column;
}

.brand-name {
  font-size: 16px;
  font-weight: 700;
  color: #f1f5f9;
  letter-spacing: 0.5px;
}

.brand-tag {
  font-size: 11px;
  color: #6366f1;
  font-weight: 600;
  letter-spacing: 1px;
  text-transform: uppercase;
}

.sidebar-nav {
  flex: 1;
  padding: 16px 12px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 14px;
  border-radius: 8px;
  color: #94a3b8;
  font-size: 14px;
  font-weight: 500;
  text-decoration: none;
  transition: all 0.2s;
}

.nav-item:hover {
  background: rgba(255,255,255,0.06);
  color: #e2e8f0;
}

.nav-item.active {
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  color: #fff;
  box-shadow: 0 2px 8px rgba(99,102,241,0.3);
}

.sidebar-footer {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 20px;
  border-top: 1px solid rgba(255,255,255,0.06);
}

.admin-avatar {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 700;
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
  font-weight: 600;
  color: #e2e8f0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.admin-role {
  font-size: 11px;
  color: #64748b;
}

.logout-btn {
  color: #64748b;
  cursor: pointer;
  padding: 6px;
  border-radius: 6px;
  transition: all 0.2s;
}
.logout-btn:hover {
  color: #f87171;
  background: rgba(248,113,113,0.1);
}

/* Main Content */
.main-content {
  flex: 1;
  overflow-y: auto;
  min-width: 0;
}
</style>
