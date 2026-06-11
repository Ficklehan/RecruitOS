<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { cn } from '@/lib/utils'
import { useUserStore } from '@/stores/user'
import { topNavMenus, filterMenus, getDefaultRoute, type MenuItem } from '@/config/menus'
import { getMyNotifications, markNotificationRead } from '@/api/modules/notification'
import { RButton, RBadge, RSeparator, RDropdown, RAvatar, RScrollArea } from '@/components/ui'
import { Bell, ChevronDown, LogOut, Menu } from 'lucide-vue-next'

const props = defineProps<{ sidebarCollapsed?: boolean }>()
const emit = defineEmits<{ 'toggle-sidebar': [] }>()

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
  if (item.children?.some(c => route.path.startsWith(c.path))) return true
  return route.path.startsWith('/' + item.key)
}

function goHome() {
  const roleCodes = userStore.roles.map((r: any) => (typeof r === 'string' ? r : r.roleCode)).filter(Boolean)
  router.push(getDefaultRoute(roleCodes))
}

function handleNavClick(item: MenuItem) {
  router.push(item.path)
}

async function handleLogout() {
  await userStore.logout()
  router.push('/login')
}
</script>

<template>
  <nav class="fixed top-0 left-0 right-0 h-[var(--r-header-height)] bg-bg-card z-[var(--r-z-header)] flex items-center justify-between px-5 border-b border-divider">
    <div class="flex items-center gap-6">
      <!-- Mobile menu toggle -->
      <RButton variant="ghost" size="sm" class="md:hidden p-1.5" @click="emit('toggle-sidebar')">
        <Menu class="h-5 w-5" />
      </RButton>

      <!-- Logo -->
      <div class="flex items-center gap-2.5 cursor-pointer" @click="goHome">
        <svg viewBox="0 0 32 32" fill="none" class="w-7 h-7">
          <rect width="32" height="32" rx="8" fill="#4F6BED"/>
          <path d="M9 11h14M9 16h10M9 21h6" stroke="white" stroke-width="2" stroke-linecap="round"/>
          <circle cx="23" cy="21" r="3" fill="white" opacity="0.9"/>
        </svg>
        <span class="text-[15px] font-bold text-text-primary hidden sm:block">RecruitOS</span>
      </div>

      <!-- Nav items -->
      <div class="hidden md:flex items-center gap-0.5">
        <div
          v-for="item in visibleTopMenus"
          :key="item.key"
          :class="cn(
            'px-3.5 py-1.5 rounded-[var(--r-radius-sm)] text-[13px] font-medium cursor-pointer transition-all duration-200',
            isActiveGroup(item)
              ? 'bg-primary-light text-primary'
              : 'text-text-secondary hover:text-text-primary hover:bg-bg-hover',
          )"
          @click="handleNavClick(item)"
        >
          {{ item.label }}
        </div>
      </div>
    </div>

    <div class="flex items-center gap-1">
      <!-- Notifications -->
      <RDropdown @open-change="loadNotifications">
        <template #trigger>
          <RButton variant="ghost" size="sm" class="relative p-2">
            <Bell class="h-[18px] w-[18px] text-text-secondary" />
            <RBadge v-if="unreadCount > 0" variant="danger" size="sm" class="absolute -top-0.5 -right-0.5 min-w-[18px] h-[18px] px-1 flex items-center justify-center">
              {{ unreadCount }}
            </RBadge>
          </RButton>
        </template>
        <div class="w-[320px]">
          <div class="px-3 py-2.5 text-[13px] font-semibold text-text-primary border-b border-divider">通知</div>
          <RScrollArea class="h-[300px]">
            <div v-if="!notifications.length" class="py-8 text-center text-[13px] text-text-placeholder">暂无通知</div>
            <div
              v-for="n in notifications"
              :key="n.id"
              :class="cn('px-3 py-2.5 cursor-pointer hover:bg-bg-hover transition-colors', n.isRead !== 1 && 'bg-primary-light/50')"
              @click="openNotification(n)"
            >
              <p :class="cn('text-[13px] text-text-primary', n.isRead !== 1 && 'font-medium')">{{ n.title }}</p>
              <p class="text-[12px] text-text-placeholder mt-0.5">{{ n.createdAt }}</p>
            </div>
          </RScrollArea>
          <div class="px-3 py-2 border-t border-divider">
            <RButton variant="ghost" class="w-full justify-center text-primary text-[13px]" @click="router.push('/workspace/inbox')">
              查看收件箱
            </RButton>
          </div>
        </div>
      </RDropdown>

      <RSeparator direction="vertical" class="h-5 mx-1" />

      <!-- User menu -->
      <RDropdown>
        <template #trigger>
          <RButton variant="ghost" class="h-9 px-2 gap-2">
            <RAvatar :alt="userName" size="sm" />
            <span class="hidden sm:inline text-[13px] font-medium text-text-primary max-w-[72px] truncate">{{ userName }}</span>
            <ChevronDown class="h-3 w-3 text-text-placeholder" />
          </RButton>
        </template>
        <div class="w-[160px] py-1">
          <button class="w-full px-3 py-2 text-[13px] text-left flex items-center gap-2 hover:bg-bg-hover transition-colors text-danger" @click="handleLogout">
            <LogOut class="h-4 w-4" />
            退出登录
          </button>
        </div>
      </RDropdown>
    </div>
  </nav>
</template>
