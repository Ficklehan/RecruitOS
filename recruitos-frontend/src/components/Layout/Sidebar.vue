<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { cn } from '@/lib/utils'
import { useUserStore } from '@/stores/user'
import { topNavMenus, filterMenus } from '@/config/menus'
import { resolveActionIcon } from '@/lib/actionIcons'
import { RScrollArea } from '@/components/ui'

interface Props {
  collapsed?: boolean
}

const props = defineProps<Props>()

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const sidebarMenu = computed(() => {
  const menus = filterMenus(topNavMenus, userStore.permissions)
  const topKey = route.path.split('/')[1]
  const exact = menus.find(m => m.key === topKey)
  if (exact) return exact
  return menus.find(m => m.children?.some(c => route.path.startsWith(c.path))) || null
})

function isActive(path: string) {
  return route.path === path || route.path.startsWith(path + '/')
}
</script>

<template>
  <aside
    v-if="sidebarMenu"
    :class="cn(
      'fixed top-[var(--r-header-height)] left-0 bottom-0 bg-bg-card z-[var(--r-z-sidebar)] flex flex-col transition-all duration-200 border-r border-divider',
      collapsed ? 'w-[var(--r-sidebar-collapsed)]' : 'w-[var(--r-sidebar-width)]',
    )"
  >
    <div :class="cn('px-4 pt-5 pb-3', collapsed && 'px-2 pt-5 text-center')">
      <h3 v-if="!collapsed" class="text-[11px] font-semibold text-text-placeholder uppercase tracking-wider">
        {{ sidebarMenu.label }}
      </h3>
      <div v-else class="w-6 h-0.5 bg-bg-muted mx-auto rounded-full" />
    </div>

    <RScrollArea class="flex-1">
      <nav class="px-2 pb-4">
        <div class="space-y-0.5">
          <div
            v-for="child in sidebarMenu.children"
            :key="child.path"
            :class="cn(
              'relative flex items-center gap-3 rounded-[var(--r-radius-sm)] px-3 py-2.5 text-[13px] font-medium transition-all duration-200 cursor-pointer',
              isActive(child.path)
                ? 'bg-primary-light text-primary'
                : 'text-text-secondary hover:bg-bg-hover hover:text-text-primary',
              collapsed && 'justify-center px-2',
            )"
            :title="collapsed ? child.label : undefined"
            @click="router.push(child.path)"
          >
            <div
              v-if="isActive(child.path)"
              class="absolute left-0 top-1/2 -translate-y-1/2 w-[3px] h-5 bg-primary rounded-r-full"
            />
            <component
              :is="resolveActionIcon(child.icon)"
              :class="cn('h-4 w-4 shrink-0', !isActive(child.path) && 'opacity-50')"
            />
            <span v-if="!collapsed" class="truncate">{{ child.label }}</span>
            <!-- AI status badge -->
            <span
              v-if="!collapsed && child.aiStatus !== undefined && child.aiStatus > 0"
              class="ml-auto text-[11px] font-semibold px-1.5 py-0.5 rounded-full min-w-[20px] text-center"
              :class="child.aiStatus > 3 ? 'bg-red-100 text-red-700' : 'bg-amber-100 text-amber-700'"
            >
              {{ child.aiStatus }}
            </span>
          </div>
        </div>
      </nav>
    </RScrollArea>
  </aside>
</template>
