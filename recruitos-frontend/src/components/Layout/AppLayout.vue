<template>
  <div class="app-layout">
    <TopNav />
    <Sidebar />
    <div class="main-container" :class="{ 'has-sidebar': hasSidebar }">
      <div class="content-area">
        <Breadcrumb />
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

// dashboard 页面不需要侧栏
const hasSidebar = computed(() => {
  return route.path !== '/dashboard' && !route.path.startsWith('/dashboard')
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
  transition: padding-left $transition-slow;

  &.has-sidebar {
    padding-left: $sub-sidebar-width;
  }
}

.content-area {
  padding: 24px 28px;
}

.page-fade-enter-active,
.page-fade-leave-active {
  transition: opacity 0.16s ease, transform 0.16s ease;
}

.page-fade-enter-from {
  opacity: 0;
  transform: translateY(6px);
}

.page-fade-leave-to {
  opacity: 0;
}
</style>
