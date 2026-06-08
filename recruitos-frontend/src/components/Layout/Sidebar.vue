<template>
  <aside class="sub-sidebar" v-if="sidebarMenu">
    <div class="sub-sidebar-header">
      <span class="sub-sidebar-title">{{ sidebarMenu.label }}</span>
    </div>
    <div class="sub-menu-list">
      <div
        v-for="child in sidebarMenu.children"
        :key="child.path"
        class="sub-menu-item"
        :class="{ active: isActive(child.path) }"
        @click="router.push(child.path)"
      >
        <el-icon v-if="child.icon" :size="16"><component :is="child.icon" /></el-icon>
        <span>{{ child.label }}</span>
      </div>
    </div>
  </aside>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { topNavMenus, filterMenus } from '@/config/menus'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const sidebarMenu = computed(() => {
  const menus = filterMenus(topNavMenus, userStore.permissions)
  const topKey = route.path.split('/')[1]
  return menus.find(m => m.key === topKey) || null
})

function isActive(path: string) {
  return route.path === path || route.path.startsWith(path + '/')
}
</script>

<style lang="scss" scoped>
@import '@/assets/styles/variables.scss';

.sub-sidebar {
  position: fixed;
  top: $topnav-height;
  left: 0;
  bottom: 0;
  width: $sub-sidebar-width;
  background: $bg-card;
  display: flex;
  flex-direction: column;
  overflow-y: auto;
  z-index: 100;
}

.sub-sidebar-header {
  padding: 20px 18px 12px;
}

.sub-sidebar-title {
  font-size: 12px;
  font-weight: 600;
  color: $text-placeholder;
  text-transform: uppercase;
  letter-spacing: 0.06em;
}

.sub-menu-list {
  padding: 0 10px;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.sub-menu-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 9px 12px;
  border-radius: $border-radius-sm;
  font-size: 14px;
  font-weight: 450;
  color: $text-secondary;
  cursor: pointer;
  transition: all $transition-fast;

  .el-icon { color: inherit; opacity: 0.6; }
  &:hover { background: $bg-muted; color: $text-primary; }
  &.active {
    background: $primary-lighter;
    color: $primary-color;
    font-weight: 500;
    .el-icon { opacity: 1; }
  }
}
</style>
