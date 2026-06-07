import { defineStore } from 'pinia'
import { ref } from 'vue'

interface BreadcrumbItem {
  title: string
  path?: string
}

export const useAppStore = defineStore('app', () => {
  const sidebarCollapsed = ref<boolean>(false)
  const breadcrumbs = ref<BreadcrumbItem[]>([])

  function toggleSidebar() {
    sidebarCollapsed.value = !sidebarCollapsed.value
    // 持久化到 localStorage
    localStorage.setItem('sidebarCollapsed', String(sidebarCollapsed.value))
  }

  function setBreadcrumbs(items: BreadcrumbItem[]) {
    breadcrumbs.value = items
  }

  function initSidebar() {
    const saved = localStorage.getItem('sidebarCollapsed')
    if (saved !== null) {
      sidebarCollapsed.value = saved === 'true'
    }
  }

  // 初始化
  initSidebar()

  return {
    sidebarCollapsed,
    breadcrumbs,
    toggleSidebar,
    setBreadcrumbs,
  }
})
