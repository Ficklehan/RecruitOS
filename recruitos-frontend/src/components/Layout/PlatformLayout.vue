<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { cn } from '@/lib/utils'
import { platformLogoutApi } from '@/api/modules/platform'
import { removeToken } from '@/utils/auth'
import { RAvatar, RButton, RSeparator } from '@/components/ui'
import { Building2, LogOut } from 'lucide-vue-next'

const route = useRoute()
const router = useRouter()

const adminName = computed(() => localStorage.getItem('platformAdminName') || '管理员')

const menuItems = [
  { path: '/platform/tenants', label: '租户管理', icon: Building2 },
]

async function handleLogout() {
  try { await platformLogoutApi() } catch {}
  removeToken()
  localStorage.removeItem('isPlatformAdmin')
  localStorage.removeItem('platformAdminName')
  router.push('/login')
}
</script>

<template>
  <div class="flex h-screen bg-bg-page">
    <!-- Sidebar -->
    <aside class="w-[var(--r-sidebar-width)] bg-bg-card flex flex-col shrink-0 border-r border-divider">
      <!-- Brand -->
      <div class="flex items-center gap-3 p-5 border-b border-divider">
        <div class="w-8 h-8 rounded-[var(--r-radius)] bg-primary flex items-center justify-center shrink-0">
          <svg viewBox="0 0 32 32" fill="none" class="w-5 h-5">
            <path d="M9 11h14M9 16h10M9 21h6" stroke="#fff" stroke-width="2" stroke-linecap="round"/>
          </svg>
        </div>
        <div class="flex flex-col">
          <span class="text-[14px] font-semibold text-text-primary">RecruitOS</span>
          <span class="text-[12px] text-text-secondary">平台管理</span>
        </div>
      </div>

      <!-- Navigation -->
      <nav class="flex-1 p-2.5">
        <router-link
          v-for="item in menuItems"
          :key="item.path"
          :to="item.path"
          :class="cn(
            'flex items-center gap-2.5 px-3 py-2.5 rounded-[var(--r-radius-sm)] text-[14px] font-medium transition-all duration-200',
            route.path.startsWith(item.path)
              ? 'bg-primary-light text-primary'
              : 'text-text-secondary hover:bg-bg-hover hover:text-text-primary',
          )"
        >
          <component :is="item.icon" class="h-[18px] w-[18px]" />
          <span>{{ item.label }}</span>
        </router-link>
      </nav>

      <!-- User info -->
      <div class="p-3 border-t border-divider">
        <div class="flex items-center gap-2.5">
          <RAvatar :alt="adminName" size="sm" />
          <div class="flex-1 min-w-0">
            <p class="text-[13px] font-medium text-text-primary truncate">{{ adminName }}</p>
            <p class="text-[12px] text-text-secondary">平台管理员</p>
          </div>
          <RButton variant="ghost" size="sm" class="p-1.5 text-text-secondary hover:text-danger" @click="handleLogout">
            <LogOut class="h-4 w-4" />
          </RButton>
        </div>
      </div>
    </aside>

    <!-- Main content -->
    <main class="flex-1 overflow-y-auto p-6">
      <router-view />
    </main>
  </div>
</template>
