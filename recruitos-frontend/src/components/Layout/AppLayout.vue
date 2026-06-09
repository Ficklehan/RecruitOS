<template>
  <div class="app-layout">
    <TopNav />
    <Sidebar />
    <div class="main-container" :class="{ 'has-sidebar': hasSidebar }">
      <div class="content-area">
        <Breadcrumb v-if="showBreadcrumb" />
        <router-view v-slot="{ Component }">
          <transition name="page-fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import TopNav from './TopNav.vue'
import Sidebar from './Sidebar.vue'
import Breadcrumb from './Breadcrumb.vue'

const route = useRoute()

const hasSidebar = computed(() => {
  const top = route.path.split('/')[1]
  return top && top !== 'login' && !route.path.startsWith('/platform')
})

const showBreadcrumb = computed(() => {
  return route.matched.some(item => item.meta?.title) && route.path !== '/workspace/dashboard'
})
</script>

<style lang="scss" scoped>
@import '@/assets/styles/variables.scss';

.app-layout {
  min-height: 100vh;
  background: $bg-page;
}

.main-container {
  padding-top: $topnav-height;
  min-height: 100vh;
  width: 100%;
  transition: padding-left $transition-slow;

  &.has-sidebar {
    padding-left: $sub-sidebar-width;
  }
}

.content-area {
  width: 100%;
  min-width: 0;
  padding: var(--spacing-page-y) var(--spacing-page-x) 28px;
  min-height: calc(100vh - #{$topnav-height});
}

.page-fade-enter-active,
.page-fade-leave-active {
  transition: opacity 0.12s ease;
}

.page-fade-enter-from,
.page-fade-leave-to {
  opacity: 0;
}
</style>
