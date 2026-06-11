import { defineStore } from 'pinia'
import { ref } from 'vue'

interface BreadcrumbItem {
  title: string
  path?: string
}

export const useAppStore = defineStore('app', () => {
  const breadcrumbs = ref<BreadcrumbItem[]>([])

  function setBreadcrumbs(items: BreadcrumbItem[]) {
    breadcrumbs.value = items
  }

  return {
    breadcrumbs,
    setBreadcrumbs,
  }
})
