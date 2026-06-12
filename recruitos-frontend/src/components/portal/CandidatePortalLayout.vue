<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

const candidateName = computed(() => (route.query.name as string) || '候选人')
const tabs = [
  { key: 'overview', label: '我的进度', path: '/portal/overview' },
  { key: 'interviews', label: '面试', path: '/portal/interviews' },
  { key: 'offer', label: '录用', path: '/portal/offer' },
  { key: 'onboard', label: '入职', path: '/portal/onboard' },
]

const activeTab = computed(() => {
  const p = route.path
  if (p.includes('interviews')) return 'interviews'
  if (p.includes('offer')) return 'offer'
  if (p.includes('onboard')) return 'onboard'
  return 'overview'
})
</script>

<template>
  <div class="min-h-screen bg-gray-50">
    <!-- Header -->
    <header class="bg-white border-b border-gray-200 px-6 py-4">
      <div class="max-w-4xl mx-auto flex items-center justify-between">
        <div class="flex items-center gap-3">
          <div class="w-8 h-8 bg-blue-600 rounded-lg flex items-center justify-center">
            <span class="text-white text-sm font-bold">R</span>
          </div>
          <div>
            <h2 class="text-sm font-semibold text-gray-900">RecruitOS 候选人中心</h2>
            <p class="text-xs text-gray-500">你好，{{ candidateName }}</p>
          </div>
        </div>
        <div class="text-xs text-gray-400">安全通道 · 你的信息仅招聘团队可见</div>
      </div>
    </header>

    <!-- Nav tabs -->
    <div class="bg-white border-b border-gray-200">
      <div class="max-w-4xl mx-auto flex gap-0">
        <button
          v-for="tab in tabs"
          :key="tab.key"
          @click="$router.push(tab.path + ($route.query.token ? '?token=' + $route.query.token : ''))"
          class="px-5 py-3 text-sm font-medium border-b-2 transition-colors"
          :class="activeTab === tab.key
            ? 'border-blue-600 text-blue-600'
            : 'border-transparent text-gray-500 hover:text-gray-700'"
        >
          {{ tab.label }}
        </button>
      </div>
    </div>

    <!-- Content -->
    <main class="max-w-4xl mx-auto p-6">
      <slot />
    </main>

    <!-- Footer -->
    <footer class="max-w-4xl mx-auto px-6 py-8 text-center text-xs text-gray-400">
      RecruitOS · AI 驱动的招聘体验 · 如有问题请联系招聘团队
    </footer>
  </div>
</template>
