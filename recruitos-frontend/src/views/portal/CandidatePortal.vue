<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import CandidatePortalLayout from '@/components/portal/CandidatePortalLayout.vue'
import { RCard, RButton, RBadge, RProgress, REmpty, RSkeleton } from '@/components/ui'
import {
  Check, Clock, MapPin, Calendar, FileText, Download, AlertCircle,
  TrendingUp, MessageSquare, Briefcase, Gift
} from 'lucide-vue-next'

const route = useRoute()
const loading = ref(true)
const stage = ref('INTERVIEWING') // 当前阶段

const application = ref<any>({})
const timeline = ref<any[]>([])
const upcomingInterviews = ref<any[]>([])
const offers = ref<any[]>([])
const onboardTasks = ref<any[]>([])
const portalError = ref('')

async function fetchApplication() {
  try {
    const candidateId = new URLSearchParams(window.location.search).get('id') || '1'
    const res = await fetch('/api/portal/application?candidateId=' + candidateId)
    if (res.ok) {
      const data = await res.json()
      if (data.data || data.code === 0) {
        const d = data.data || data
        application.value = {
          jobTitle: d.jobTitle || '待确认岗位',
          company: d.company || 'XX科技',
          appliedAt: d.appliedAt || '',
          currentStage: d.currentStage || 'PENDING',
          stageLabel: d.stageLabel || '待处理',
        }
        timeline.value = d.timeline || buildDefaultTimeline()
        upcomingInterviews.value = d.upcomingInterviews || []
        offers.value = d.offers || []
        onboardTasks.value = d.onboardTasks || []
      }
    }
  } catch {
    portalError.value = '无法加载申请信息'
  }
}

function buildDefaultTimeline() {
  return [
    { date: '—', title: '简历投递', status: 'done', desc: '' },
    { date: '—', title: '简历筛选', status: 'pending', desc: '' },
    { date: '—', title: '面试', status: 'pending', desc: '' },
    { date: '—', title: 'Offer', status: 'pending', desc: '' },
    { date: '—', title: '入职', status: 'pending', desc: '' },
  ]
}

const stageIndex = computed(() => timeline.value.findIndex(t => t.status === 'current'))

onMounted(async () => { await fetchApplication(); loading.value = false })
</script>

<template>
  <CandidatePortalLayout>
    <RSkeleton v-if="loading" class="h-64" />

    <template v-else>
      <!-- Overview Tab -->
      <div v-if="$route.path.includes('overview') || $route.path === '/portal'" class="space-y-6">
        <!-- Status card -->
        <RCard class="p-6 border-l-4 border-blue-400">
          <div class="flex items-start justify-between">
            <div>
              <div class="flex items-center gap-2 mb-1">
                <Briefcase class="h-4 w-4 text-blue-500" />
                <h3 class="text-lg font-semibold text-gray-900">{{ application.jobTitle }}</h3>
              </div>
              <p class="text-sm text-gray-500">{{ application.company }}</p>
            </div>
            <RBadge variant="default" size="sm" class="bg-blue-100 text-blue-700">
              {{ application.stageLabel }}
            </RBadge>
          </div>

          <!-- Progress bar -->
          <div class="mt-4">
            <div class="flex justify-between text-xs text-gray-500 mb-2">
              <span v-for="(t, i) in timeline" :key="i"
                :class="t.status === 'done' ? 'text-green-600' : t.status === 'current' ? 'text-blue-600 font-semibold' : 'text-gray-400'">
                {{ t.title }}
              </span>
            </div>
            <RProgress :value="Math.max(10, (stageIndex / timeline.length) * 100)" class="h-2" />
          </div>
        </RCard>

        <!-- Timeline -->
        <RCard class="p-6">
          <h3 class="text-sm font-semibold text-gray-900 mb-4 flex items-center gap-2">
            <Clock class="h-4 w-4 text-gray-400" /> 进度时间线
          </h3>
          <div class="space-y-0">
            <div v-for="(t, i) in timeline" :key="i" class="flex gap-4 pb-4 relative">
              <div class="flex flex-col items-center">
                <div class="w-3 h-3 rounded-full border-2 shrink-0"
                  :class="t.status === 'done' ? 'bg-green-500 border-green-500' : t.status === 'current' ? 'bg-blue-500 border-blue-500' : 'bg-white border-gray-300'" />
                <div v-if="i < timeline.length - 1" class="w-0.5 h-full mt-1"
                  :class="t.status === 'done' ? 'bg-green-200' : 'bg-gray-200'" />
              </div>
              <div class="flex-1 min-w-0 pb-2">
                <div class="flex items-center gap-2">
                  <span class="text-sm font-medium text-gray-900">{{ t.title }}</span>
                  <Check v-if="t.status === 'done'" class="h-3.5 w-3.5 text-green-500" />
                </div>
                <p class="text-xs text-gray-500">{{ t.date }} · {{ t.desc }}</p>
              </div>
            </div>
          </div>
        </RCard>
      </div>

      <!-- Interviews Tab -->
      <div v-if="$route.path.includes('interviews')" class="space-y-6">
        <RCard class="p-6">
          <h3 class="text-sm font-semibold text-gray-900 mb-4 flex items-center gap-2">
            <Calendar class="h-4 w-4 text-blue-500" /> 待确认面试
          </h3>

          <div v-if="!upcomingInterviews.length" class="text-center py-8 text-gray-400">
            暂无待确认的面试安排
          </div>

          <div v-for="iv in upcomingInterviews" :key="iv.id"
            class="p-4 bg-blue-50 rounded-lg border border-blue-100 mb-3">
            <div class="flex items-start justify-between">
              <div>
                <h4 class="text-sm font-semibold text-gray-900">{{ iv.type }}</h4>
                <div class="text-xs text-gray-500 space-y-1 mt-1">
                  <div class="flex items-center gap-1"><Clock class="h-3 w-3" /> {{ iv.duration }} · 线上面试</div>
                  <div class="flex items-center gap-1"><MapPin class="h-3 w-3" /> 面试官：{{ iv.interviewer }}</div>
                </div>
              </div>
              <RBadge variant="warning" size="sm">待确认时间</RBadge>
            </div>
            <div v-if="iv.note" class="mt-3 p-2 bg-white rounded text-xs text-gray-600 border border-gray-100">
              💡 {{ iv.note }}
            </div>
            <div class="mt-3 flex gap-2">
              <RButton size="sm">确认参加</RButton>
              <RButton size="sm" variant="outline">调整时间</RButton>
            </div>
          </div>
        </RCard>

        <RCard class="p-6">
          <h3 class="text-sm font-semibold text-gray-900 mb-3">面试准备小贴士</h3>
          <div class="text-sm text-gray-600 space-y-2">
            <div class="flex items-start gap-2">
              <Check class="h-4 w-4 text-green-500 shrink-0 mt-0.5" />
              <span>测试设备：确保摄像头和麦克风正常工作</span>
            </div>
            <div class="flex items-start gap-2">
              <Check class="h-4 w-4 text-green-500 shrink-0 mt-0.5" />
              <span>准备环境：选择安静、光线充足的空间</span>
            </div>
            <div class="flex items-start gap-2">
              <Check class="h-4 w-4 text-green-500 shrink-0 mt-0.5" />
              <span>了解公司：提前浏览公司官网和产品</span>
            </div>
          </div>
        </RCard>
      </div>

      <!-- Offer Tab -->
      <div v-if="$route.path.includes('offer')" class="space-y-6">
        <REmpty v-if="!offers.length" description="暂未收到录用通知。通过所有面试后，Offer 将在此展示。"
          image-description="Offer 阶段" />
      </div>

      <!-- Onboard Tab -->
      <div v-if="$route.path.includes('onboard')" class="space-y-6">
        <REmpty v-if="!onboardTasks.length" description="暂未进入入职阶段。接受 Offer 后，入职任务将在此展示。"
          image-description="入职阶段" />
      </div>
    </template>
  </CandidatePortalLayout>
</template>
