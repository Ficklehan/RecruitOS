<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { RCard, RButton, RInput, RBadge, REmpty } from '@/components/ui'
import { Search, Sparkles, UserPlus, Zap } from 'lucide-vue-next'

const router = useRouter()
const query = ref('')
const searching = ref(false)
const humanResults = ref<any[]>([])
const aiResults = ref<any[]>([])

async function doSearch() {
  if (!query.value.trim()) return
  searching.value = true
  try {
    const res = await fetch('/api/brain/search', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ query: query.value }),
    })
    const data = await res.json()
    if (data.code === 0 && data.data) {
      humanResults.value = data.data.humanResults || []
      aiResults.value = data.data.aiResults || []
    }
  } catch { /* ignore */ }
  searching.value = false
}

function goToCandidate(id: number) {
  router.push(`/pipeline/candidates/${id}`)
}
</script>

<template>
  <div class="p-6 max-w-3xl mx-auto space-y-6">
    <div class="flex items-center gap-3 mb-2">
      <Search class="h-5 w-5 text-blue-500" />
      <div>
        <h1 class="text-base font-semibold text-text-primary">主动搜索</h1>
        <p class="text-xs text-text-placeholder">你搜 + AI 同时搜，结果合并去重</p>
      </div>
    </div>

    <!-- Search bar -->
    <div class="flex gap-2">
      <RInput v-model="query" placeholder="搜索关键词、技能、公司..." class="flex-1"
        @keyup.enter="doSearch" />
      <RButton @click="doSearch" :disabled="searching">
        <Search class="h-4 w-4 mr-1" />
        {{ searching ? '搜索中...' : '搜索' }}
      </RButton>
    </div>

    <!-- Results -->
    <template v-if="humanResults.length || aiResults.length">
      <!-- Human results -->
      <div v-if="humanResults.length">
        <div class="flex items-center gap-2 mb-3">
          <Search class="h-4 w-4 text-gray-500" />
          <h2 class="text-sm font-semibold text-text-primary">精确匹配</h2>
          <RBadge size="sm" variant="secondary">{{ humanResults.length }}</RBadge>
        </div>
        <div class="space-y-2">
          <RCard v-for="r in humanResults" :key="'h'+r.id"
            class="p-3 flex items-center justify-between hover:shadow-sm cursor-pointer"
            @click="goToCandidate(r.id)">
            <div>
              <div class="flex items-center gap-2">
                <span class="text-sm font-semibold">{{ r.name }}</span>
                <span class="text-xs text-text-placeholder">{{ r.title }} · {{ r.company }}</span>
              </div>
              <span class="text-xs text-text-placeholder">{{ r.source }}</span>
            </div>
            <div class="flex items-center gap-2">
              <RBadge variant="default" size="sm">{{ r.match }}%</RBadge>
              <UserPlus class="h-4 w-4 text-text-placeholder" />
            </div>
          </RCard>
        </div>
      </div>

      <!-- AI results -->
      <div v-if="aiResults.length">
        <div class="flex items-center gap-2 mb-3 mt-6">
          <Sparkles class="h-4 w-4 text-blue-500" />
          <h2 class="text-sm font-semibold text-text-primary">AI 发现的候选人</h2>
          <RBadge size="sm" variant="outline" class="border-blue-200 text-blue-600">{{ aiResults.length }}</RBadge>
        </div>
        <p class="text-xs text-text-placeholder mb-3">AI 在关键词之外，基于语义和推理发现的候选人</p>
        <div class="space-y-2">
          <RCard v-for="r in aiResults" :key="'a'+r.id"
            class="p-3 hover:shadow-sm cursor-pointer border-l-4 border-blue-200"
            @click="goToCandidate(r.id)">
            <div class="flex items-center justify-between">
              <div>
                <div class="flex items-center gap-2">
                  <Sparkles class="h-3.5 w-3.5 text-blue-400" />
                  <span class="text-sm font-semibold">{{ r.name }}</span>
                  <span class="text-xs text-text-placeholder">{{ r.title }} · {{ r.company }}</span>
                </div>
                <p class="text-xs text-blue-600 mt-1 ml-6">💡 {{ r.aiNote }}</p>
              </div>
              <RBadge variant="default" size="sm">{{ r.match }}%</RBadge>
            </div>
          </RCard>
        </div>
      </div>
    </template>

    <REmpty v-else-if="!searching && query"
      description="未找到匹配的候选人。试试调整搜索关键词或放宽条件。" />

    <REmpty v-else-if="!searching"
      description="输入关键词开始搜索。AI 会同时进行语义搜索，发现你可能漏掉的候选人。" />
  </div>
</template>
