<script setup lang="ts">
import { computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAppStore } from '@/stores/app'
import { Home, ChevronRight } from 'lucide-vue-next'

const router = useRouter()
const route = useRoute()
const appStore = useAppStore()

const breadcrumbs = computed(() => {
  const matched = route.matched.filter(item => item.meta && item.meta.title)
  return matched.map(item => ({
    title: item.meta.title as string,
    path: item.redirect ? undefined : item.path,
  }))
})

watch(breadcrumbs, (val) => {
  appStore.setBreadcrumbs(val)
}, { immediate: true })
</script>

<template>
  <nav class="flex items-center text-[13px] text-text-secondary">
    <ol class="flex items-center gap-1">
      <li>
        <button class="flex items-center hover:text-text-primary transition-colors" @click="router.push('/workspace/dashboard')">
          <Home class="h-3.5 w-3.5" />
        </button>
      </li>
      <template v-for="(item, index) in breadcrumbs" :key="item.path || index">
        <li class="flex items-center">
          <ChevronRight class="h-3 w-3 text-text-placeholder mx-1" />
        </li>
        <li>
          <button
            v-if="item.path && index < breadcrumbs.length - 1"
            class="hover:text-text-primary transition-colors"
            @click="router.push(item.path!)"
          >
            {{ item.title }}
          </button>
          <span v-else class="font-medium text-text-primary">{{ item.title }}</span>
        </li>
      </template>
    </ol>
  </nav>
</template>
