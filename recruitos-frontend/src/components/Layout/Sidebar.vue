<template>
  <aside class="sub-sidebar" v-if="currentGroup">
    <div class="sub-sidebar-header">
      <span class="sub-sidebar-title">{{ currentGroup.title }}</span>
    </div>
    <div class="sub-menu-list">
      <div
        v-for="child in currentGroup.children"
        :key="child.path"
        class="sub-menu-item"
        :class="{ active: route.path === getFullPath(child) }"
        @click="router.push(getFullPath(child))"
      >
        <el-icon v-if="child.meta?.icon" :size="16"><component :is="child.meta.icon" /></el-icon>
        <span>{{ child.meta?.title }}</span>
      </div>
    </div>
  </aside>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

// 路由模块分组：同组的模块共享侧边栏
const routeGroups: Record<string, string[]> = {
  recruit: ['/position', '/talent', '/screening'],
  process: ['/interview', '/hiring', '/onboard'],
  ai: ['/ai-tools'],
}

function getCurrentGroupKey(): string | null {
  for (const [key, prefixes] of Object.entries(routeGroups)) {
    if (prefixes.some(p => route.path.startsWith(p))) return key
  }
  return null
}

// 从路由中找到当前匹配的顶层模块
const parentPath = computed(() => {
  const matched = route.matched
  for (let i = matched.length - 1; i >= 0; i--) {
    const m = matched[i]
    if (m.meta?.title && m.children && m.children.length > 1) {
      return m.path.replace(/^\//, '')
    }
  }
  return ''
})

function getFullPath(child: any): string {
  if (child.path.startsWith('/')) return child.path
  // 在 router routes 中找到该 child 所属的父路由
  for (const r of router.options.routes) {
    if (r.children?.some((c: any) => c.path === child.path || c.name === child.name)) {
      return `${r.path}/${child.path}`
    }
  }
  return `/${parentPath.value}/${child.path}`
}

const currentGroup = computed(() => {
  if (route.path === '/dashboard') return null

  const groupKey = getCurrentGroupKey()
  const matched = route.matched

  if (groupKey) {
    // 收集同组所有模块的子路由
    const allChildren: any[] = []
    for (let i = matched.length - 1; i >= 0; i--) {
      const m = matched[i]
      if (m.meta?.title && m.children) {
        allChildren.push(...m.children.filter((c: any) => !c.meta?.hidden))
      }
    }
    // 从 router.options.routes 补充同组其他模块的子路由
    const routerRoutes = router.options.routes
    const groupPrefixes = routeGroups[groupKey]
    for (const r of routerRoutes) {
      if (r.children && groupPrefixes.some(p => r.path === p)) {
        for (const child of r.children) {
          if (!child.meta?.hidden && !allChildren.some(c => c.path === child.path)) {
            allChildren.push(child)
          }
        }
      }
    }
    if (allChildren.length > 1) {
      // 找到第一个有 title 的模块名作为侧边栏标题
      const parentRoute = routerRoutes.find(r => groupPrefixes.some(p => r.path === p))
      return {
        title: parentRoute?.meta?.title || matched[1]?.meta?.title || '',
        children: allChildren,
      }
    }
  }

  // 默认逻辑：只显示当前模块的子路由
  for (let i = matched.length - 1; i >= 0; i--) {
    const m = matched[i]
    if (m.meta?.title && m.children && m.children.length > 1) {
      return {
        title: m.meta.title,
        children: m.children.filter((c: any) => !c.meta?.hidden),
      }
    }
  }
  return null
})
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

  .el-icon {
    color: inherit;
    opacity: 0.6;
  }

  &:hover {
    background: $bg-muted;
    color: $text-primary;

    .el-icon { opacity: 0.8; }
  }

  &.active {
    background: $primary-lighter;
    color: $primary-color;
    font-weight: 500;

    .el-icon { opacity: 1; }
  }
}
</style>
