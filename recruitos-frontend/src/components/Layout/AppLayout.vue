<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import { cn } from '@/lib/utils'
import TopNav from './TopNav.vue'
import Sidebar from './Sidebar.vue'
import Breadcrumb from './Breadcrumb.vue'

const route = useRoute()
const sidebarCollapsed = ref(false)
const isMobile = ref(false)

function checkWidth() {
  isMobile.value = window.innerWidth < 768
  sidebarCollapsed.value = window.innerWidth >= 768 && window.innerWidth < 1024
}

onMounted(() => {
  checkWidth()
  window.addEventListener('resize', checkWidth)
})
onUnmounted(() => window.removeEventListener('resize', checkWidth))

const hasSidebar = ref(true)

function toggleSidebar() {
  sidebarCollapsed.value = !sidebarCollapsed.value
}

function closeMobileSidebar() {
  if (isMobile.value) sidebarCollapsed.value = true
}
</script>

<template>
  <div class="min-h-screen bg-bg-page">
    <TopNav :sidebar-collapsed="sidebarCollapsed" @toggle-sidebar="toggleSidebar" />
    <Sidebar
      v-if="!isMobile"
      :collapsed="sidebarCollapsed"
    />
    <main
      :class="cn(
        'pt-[var(--r-header-height)] min-h-screen transition-all duration-200',
        !isMobile && (sidebarCollapsed ? 'pl-[var(--r-sidebar-collapsed)]' : 'pl-[var(--r-sidebar-width)]'),
        isMobile && 'pl-0',
      )"
    >
      <div class="p-6 pb-8 min-h-[calc(100vh-var(--r-header-height))]">
        <Breadcrumb v-if="!route.path.startsWith('/workspace')" class="mb-4" />
        <router-view v-slot="{ Component }">
          <transition name="page-fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </div>
    </main>

    <!-- Mobile sidebar overlay -->
    <Transition
      enter-active-class="transition-opacity duration-200"
      enter-from-class="opacity-0"
      enter-to-class="opacity-100"
      leave-active-class="transition-opacity duration-150"
      leave-from-class="opacity-100"
      leave-to-class="opacity-0"
    >
      <div
        v-if="isMobile && !sidebarCollapsed"
        class="fixed inset-0 bg-black/40 z-[var(--r-z-sidebar)]"
        @click="closeMobileSidebar"
      />
    </Transition>
  </div>
</template>

<style scoped>
.page-fade-enter-active, .page-fade-leave-active { transition: opacity 0.12s ease; }
.page-fade-enter-from, .page-fade-leave-to { opacity: 0; }
</style>
